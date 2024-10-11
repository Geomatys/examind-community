/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.constellation.api.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.api.TilingMode;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDataCoverageJob;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IPyramidBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.DataSetBrief;
import org.constellation.dto.Filter;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.TilingResult;
import org.constellation.dto.Sort;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConstellationException;
import org.constellation.metadata.utils.Utils;
import org.constellation.provider.DataProviders;
import org.constellation.provider.PyramidData;
import org.constellation.util.MetadataMerger;
import org.constellation.util.ParamUtilities;
import org.constellation.util.Util;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manage data sending
 *
 * @author Benjamin Garcia (Geomatys)
 * @author Christophe Mourette (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class DataRestAPI extends AbstractRestAPI{

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IDatasetBusiness datasetBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IProcessBusiness processBusiness;

    @Autowired
    private ISensorBusiness sensorBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private IPyramidBusiness pyramidBusiness;

    @Autowired
    private IDataCoverageJob dataCoverageJob;

    @RequestMapping(value="/datas/{dataId}/accept",method=POST, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity acceptData(@PathVariable Integer dataId, @RequestParam(name="hidden", required = false, defaultValue = "false") Boolean hidden, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            dataBusiness.acceptData(dataId, userId, hidden);
            DataBrief brief = dataBusiness.getDataBrief(dataId, true, true);
            return new ResponseEntity(brief, OK);

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/accept",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity acceptDatas(@RequestBody List<Integer> dataIds, @RequestParam(name="hidden", required = false, defaultValue = "false") Boolean hidden, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            Map<String, List> results = new HashMap<>();
            results.put("accepted", new ArrayList<>());
            results.put("refused", new ArrayList<>());

            for (Integer id : dataIds) {
                try {
                    dataBusiness.acceptData(id, userId, hidden);
                    DataBrief brief = dataBusiness.getDataBrief(id, true, true);
                    results.get("accepted").add(brief);
                } catch (Exception ex) {
                    results.get("refused").add(id);
                    LOGGER.log(Level.INFO, ex.getMessage(), ex);
                }
            }
            return new ResponseEntity(results, OK);

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/metadata/model/{modelId}",method=POST, consumes=APPLICATION_JSON_VALUE)
    public ResponseEntity mergeModelDataMetadata(@RequestBody List<Integer> dataIds, @PathVariable Integer modelId, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            for (Integer dataId : dataIds) {
                DataBrief brief = dataBusiness.getDataBrief(dataId, false, true);
                List<MetadataLightBrief> metadatas = brief.getMetadatas();
                for (MetadataLightBrief metadata : metadatas) {
                    final Object dataObj = metadataBusiness.getMetadata(metadata.getId());
                    if (dataObj instanceof DefaultMetadata) {

                        final DefaultMetadata modelMeta = (DefaultMetadata) metadataBusiness.getMetadata(modelId);
                        final DefaultMetadata dataMeta = (DefaultMetadata) dataObj;
                        modelMeta.setFileIdentifier(null);
                        modelMeta.prune();
                        dataMeta.prune();

                        final MetadataMerger merger = new MetadataMerger(Locale.FRENCH);
                        merger.copy(modelMeta, dataMeta);
                        metadataBusiness.updateMetadata(dataMeta.getFileIdentifier(), dataMeta, brief.getId(), null, null, userId, null, "DOC");
                    }
                }
            }
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/hide/{flag}",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity changeHiddenFlag(@RequestBody List<Integer> dataIds, @PathVariable(name = "flag") boolean flag, HttpServletRequest req) {
        try {
            for (Integer dataId : dataIds) {
                dataBusiness.updateDataHidden(dataId, flag);
            }
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
    
    /**
     * Used to open metadata editor form.
     * the metadata.prune() should never be called in this method.
     * Returns json result of template writer to apply a given template to metadata object.
     *
     * @param dataId The data identifier.
     * @param prune flag that indicates if template result will clean empty children/block.
     * @return {@code Response}
     */
    @RequestMapping(value="/datas/{dataId}/metadata",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataMetadata(final @PathVariable("dataId") int dataId, final @RequestParam(name="prune", defaultValue = "false") String prune, HttpServletResponse response) {

        try {
            final String buffer = metadataBusiness.getJsonDataMetadata(dataId, Boolean.parseBoolean(prune), false);
            IOUtils.write(buffer, response.getOutputStream());
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Proceed to merge saved metadata with given values from metadata editor.
     *
     * @param dataId the data identifier.
     * @param metadataValues the values of metadata editor.
     * @return {@code Response}
     */
    @RequestMapping(value="/datas/{dataId}/metadata",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity updateDataMetadata(@PathVariable("dataId") final int dataId,
            @RequestBody final RootObj metadataValues) {
        try {
            metadataBusiness.mergeDataMetadata(dataId, metadataValues);
        } catch (ConstellationException ex) {
            LOGGER.warning("Error while saving data metadata");
            throw ex.toRuntimeException();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Proceed to search data for query (looking in metadata).
     *
     * @param term Search term.
     * @return A list that contains all datas matching the query.
     */
    @RequestMapping(value="/datas/search",method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity searchDatas(@RequestParam("term") final String term) {

        final List<DataBrief> briefs = new ArrayList<>();
        try {
            Map filterMap = new HashMap<>();
            filterMap.put("term", term);
            final Map.Entry<Integer,List<MetadataBrief>> entry = metadataBusiness.filterAndGetBrief(
                    filterMap,null,1,Integer.MAX_VALUE);

            for (final MetadataBrief md : entry.getValue()) {
                if (md.getDatasetId() != null) {
                    final DataSetBrief dsb = buildDatsetBrief(md.getDatasetId(), null, true, true);
                    briefs.addAll(dsb.getData());
                } else if (md.getDataId() != null) {
                    final DataBrief db = dataBusiness.getDataBrief(md.getDataId(), true, true);
                    briefs.add(db);
                }
            }

        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).message("Failed to parse query : "+ex.getMessage()).build();
        }

        return new ResponseEntity(briefs,OK);
    }

    /**
     * Proceed to get list of records {@link DataBrief} in Page object for dashboard.
     * the list can be filtered, sorted and use the pagination.
     *
     * @param pagedSearch given params of filters, sorting and pagination served by a pojo {link PagedSearch}
     * @param req the http request needed to get the current user.
     * @return {link Page} of {@link DataBrief}
     */
    @RequestMapping(value="/datas/search",method=POST,produces=APPLICATION_JSON_VALUE)
    public Page<DataBrief> search(@RequestBody final PagedSearch pagedSearch, final HttpServletRequest req) {

        //filters
        final Map<String,Object> filterMap = prepareFilters(pagedSearch,req);

        //sorting
        final Sort sort = pagedSearch.getSort();
        Map.Entry<String,String> sortEntry = null;
        if (sort != null) {
            sortEntry = new AbstractMap.SimpleEntry<>(sort.getField(),sort.getOrder().toString());
        }

        //pagination
        final int pageNumber = pagedSearch.getPage();
        final int rowsPerPage = pagedSearch.getSize();

        final Map.Entry<Integer,List<DataBrief>> entry = dataBusiness.filterAndGetBrief(filterMap,sortEntry,pageNumber,rowsPerPage);
        final int total = entry.getKey();
        final List<DataBrief> results = entry.getValue();

        // Build and return the content list of page.
        return new Page<DataBrief>()
                .setNumber(pageNumber)
                .setSize(rowsPerPage)
                .setContent(results)
                .setTotal(total);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Entry<String, Object> transformFilter(Filter f, final HttpServletRequest req) {
        Entry<String, Object> result = super.transformFilter(f, req);
        if (result != null) {
            return result;
        }
        String value = f.getValue();
        if (value == null || "_all".equals(value)) {
            return null;
        }
        if ("rendered".equals(f.getField()) || "included".equals(f.getField()) || "hidden".equals(f.getField()) || "sensorable".equals(f.getField())) {

            return new AbstractMap.SimpleEntry<>(f.getField(), Boolean.parseBoolean(value));

        } else if ("dataset".equals(f.getField()) || "provider_id".equals(f.getField()) || "id".equals(f.getField())) {
            try {
                final int parentId = Integer.valueOf(value);
                return new AbstractMap.SimpleEntry<>(f.getField(), parentId);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Filter by " + f.getField() + " value should be an integer: " + ex.getLocalizedMessage(), ex);
                return null;
            }

        // just here to list the existing filter
        } else if ("sub_type".equals(f.getField()) || "term".equals(f.getField()) || "type".equals(f.getField())) {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        } else {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        }
    }

    /**
     * Build {@link DataSetBrief} instance from dataset id and data children.
     *
     * @param dataSetId given dataset id.
     * @return {@link DataSetBrief} built from the given dataset.
     */
    private DataSetBrief buildDatsetBrief(final int dataSetId, List<DataBrief> children, boolean fetchDataDescription, boolean fetchDataAssociations) throws ConstellationException{
        if (children == null) {
            children = dataBusiness.getDataBriefsFromDatasetId(dataSetId, true, false, null, null, fetchDataDescription, fetchDataAssociations);
        }
        final DataSetBrief dsb = datasetBusiness.getDatasetBrief(dataSetId, children);
        return dsb;
    }

    /**
     * Generates a pyramid on a data in the given provider, create and return this new provider.
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     *
     * @param dataId the given data id.
     * @param nbLevel Number of level to compute (used only for non-coverage data). default to 8.
     * @param req
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/pyramid/",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity pyramidData(@PathVariable("dataId") final int dataId,
            @RequestParam(name = "nblevel", defaultValue = "8") final int nbLevel,
            HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            final TilingResult ref =  pyramidBusiness.pyramidDatas(userId, null, Arrays.asList(dataId), null, TilingMode.CONFORM, nbLevel);
            return new ResponseEntity(ref, OK);
        }catch(ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Generates a pyramid on a list of data and create and return this new provider.Creates btw a mapcontext that contains internal data.
     * N.B : It creates a styled pyramid, which can be used for display purposes, but not for analysis.
     *
     * @param crs The selected CRS for the generated pyramid.
     * @param layerName The given pyramid name.
     * @param dataIds The list of data identifier to integrate in the generated pyramid.
     * @param mode Tiling mode, default to RENDERED.
     * @param nbLevel Number of level to compute (used only if a non-coverage data is present in the list). default to 8.
     * @param req
     *
     * @return Informations about tiling process.
     */
    @RequestMapping(value="/datas/pyramid",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity pyramidDatas(
            @RequestParam("crs") final String crs, 
            @RequestParam("layerName") final String layerName,
            @RequestBody final List<Integer> dataIds,
            @RequestParam(name = "mode", defaultValue = "RENDERED") final String mode,
            @RequestParam(name = "nblevel", defaultValue = "8") final int nbLevel,
            HttpServletRequest req) {
        try {

            int userId = assertAuthentificated(req);
            final TilingResult ref = pyramidBusiness.pyramidDatas(userId, layerName, dataIds, crs, TilingMode.valueOf(mode), nbLevel);
            return new ResponseEntity(ref, OK);
        } catch (ConstellationException ex) {
            return new ErrorMessage().message(ex.getMessage()).build();
        }
    }

    /**
     * Indicate if given data is a pyramid.
     * If data could not be found or is not a pyramid return an empty list.
     *
     * @param dataId
     * @param req
     * @return list of Coordinate Reference System is the pyramid.
     */
    @RequestMapping(value="/datas/{dataId}/describePyramid",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity describePyramid(@PathVariable("dataId") final int dataId, HttpServletRequest req) {

        final int userId;
        try {
            userId = assertAuthentificated(req);
        } catch (ConstellationException ex) {
            return new ErrorMessage().message(ex.getMessage()).build();
        }

        try {
            final org.constellation.dto.Data brief = dataBusiness.getData(dataId);

            final Map<String,List<String>> result = new HashMap<>();
            final List<String> crss = new ArrayList<>();
            result.put("crs", crss);

            //get data
            final org.constellation.provider.Data data = DataProviders.getProviderData(brief.getProviderId(), brief.getNamespace(), brief.getName());
            if (!(data instanceof PyramidData)) return new ResponseEntity(result, OK);

            crss.addAll( ((PyramidData)data).listPyramidCRSCode());

            return new ResponseEntity(result, OK);
        } catch (ConstellationException ex) {
            return new ErrorMessage().message(ex.getMessage()).build();
        }
    }

    @RequestMapping(value="/datas/{dataId}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getData(@PathVariable("dataId") int dataId) {
        try {
            return new ResponseEntity(dataBusiness.getDataBrief(dataId, true, true),OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataList(@RequestParam(value="type",       required=false) String type,
                                      @RequestParam(value="published",  required=false) Boolean published,
                                      @RequestParam(value="sensorable", required=false) Boolean sensorable,
                                      @RequestParam(value="fetchDataDescription", required=false, defaultValue = "true") Boolean fetchDataDescription,
                                      @RequestParam(value="fetchDataAssociation", required=false, defaultValue = "true") Boolean fetchDataAssociation) {
        try {
            final List<DataBrief> results = new ArrayList<>();
            // only providers that have a no parent
            final List<Integer> providerIds = providerBusiness.getProviderIdsAsInt();
            for (final Integer providerId : providerIds) {
                final List<DataBrief> briefs = dataBusiness.getDataBriefsFromProviderId(providerId, type, true, false, sensorable, published, fetchDataDescription, fetchDataAssociation);
                results.addAll(briefs);
            }
            return new ResponseEntity(results, OK);
        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/count",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataCount() {
        final int count = dataBusiness.getCountAll(false);
        return new ResponseEntity(Collections.singletonMap("count", count), OK);
    }

    /**
     * Change the inclusion flag of a data (set to true).
     *
     * @param dataId Data identifier.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/include",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity includeData(@PathVariable("dataId") final int dataId) {
        try {
            dataBusiness.updateDataIncluded(dataId, true, false);
            return new ResponseEntity("Data included successfully.",OK);
        }catch(Exception ex){
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).message("Failed to include data").build();
        }
    }

    /**
     * Remove a data from the database.
     *
     * @param dataId Data identifier.
     * @param removeFiles Flag to indicate if you want to remove the datas from the data source (Files, database tables, ...).
     * @return
     */
    @RequestMapping(value="/datas/{dataId}",method=DELETE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity removeData(@PathVariable("dataId") final int dataId,
            @RequestParam(name = "removeFiles", defaultValue = "false", required = false) boolean removeFiles) {
        try {
            dataBusiness.removeData(dataId, removeFiles);
            return new ResponseEntity(OK);
        }catch(Exception ex){
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).message("Failed to remove data").build();
        }
    }

    /**
     * Remove multiple data from the database.
     *
     * @param dataIds List of data identifiers.
     * @param removeFiles Flag to indicate if you want to remove the datas from the data source (Files, database tables, ...).
     * @return
     */
    @RequestMapping(value="/datas/remove",method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity removeDatas(@RequestBody final List<Integer> dataIds,
            @RequestParam(name = "removeFiles", defaultValue = "false", required = false) boolean removeFiles) {
        try {
            for (int dataId : dataIds) {
                dataBusiness.removeData(dataId, removeFiles);
            }
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).message("Failed to remove datas").build();
        }
    }

    /**
     * Change the inclusion flag of a data (set to true).
     *
     * @param dataId Data identifier.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/stats",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity computeDataStats(@PathVariable("dataId") final int dataId) {
        try {
            dataCoverageJob.asyncUpdateDataStatistics(dataId);
            return new ResponseEntity("Data statistics compute requested.",OK);
        }catch(Exception ex){
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).message("Failed to compute data statistics").build();
        }
    }

    /**
     * Return as an attachment file the metadata of data in xml format.
     *
     * @param dataId Data identifier.
     * @return the xml file
     */
    @RequestMapping(value="/datas/{dataId}/metadata",method=GET,produces=APPLICATION_XML_VALUE)
    public ResponseEntity downloadMetadataForData(@PathVariable("dataId") final int dataId) {

        final HttpHeaders header = new HttpHeaders();

        try {
            List<Object> metadatas = metadataBusiness.getIsoMetadatasForData(dataId);
            if (metadatas.isEmpty()) {
                //try to get dataset metadata.
                final Integer datasetId = dataBusiness.getDataDataset(dataId);
                if (datasetId != null) {
                    Object metadata = metadataBusiness.getIsoMetadataForDataset(datasetId);
                    if (metadata != null) {
                        metadatas.add(metadata);
                    }
                }
            }
            if (!metadatas.isEmpty()) {
                for (Object metadata : metadatas) {
                    if (metadata instanceof DefaultMetadata) {
                        ((DefaultMetadata)metadata).prune();
                    }
                }
                if (metadatas.size() == 1) {
                    header.set("Content-Disposition", "attachment; filename=metadata.xml");
                    header.setContentType(MediaType.APPLICATION_XML);
                    final String xmlStr = metadataBusiness.marshallMetadata(metadatas.get(0));
                    return new ResponseEntity(xmlStr, header,OK);

                } else {
                    //create ZIP
                    try {
                        final Path directory = Files.createTempDirectory(null);
                        final List<Path> filesToSend = new ArrayList<>();
                        for (Object metadata : metadatas) {
                            final String metadataId = Utils.findIdentifier(metadata);
                            final Path file = directory.resolve(metadataId + ".xml");
                            String xml = metadataBusiness.marshallMetadata(metadata);
                            IOUtilities.writeString(xml, file);
                            filesToSend.add(file);
                        }
                        final Path zip = Paths.get(System.getProperty("java.io.tmpdir"), "exported_metadata.zip");
                        Files.deleteIfExists(zip);
                        ZipUtilities.zipNIO(zip, filesToSend.toArray(new Path[filesToSend.size()]));

                        final FileSystemResource r = new FileSystemResource(zip.toFile());
                        header.set("Content-Disposition", "attachment; filename=" + zip.getFileName().toString());

                        return new ResponseEntity(r, header, OK);
                    } catch (IOException ex) {
                        LOGGER.log(Level.WARNING, "Error while zipping data", ex);
                        return new ErrorMessage(ex).build();
                    }
                }
            }
        }catch(Exception ex){
            LOGGER.log(Level.WARNING, "Failed to get xml metadata for data id = "+dataId,ex);
        }

        // todo return error ?
        header.set("Content-Disposition", "attachment; filename=empty.xml");
        return new ResponseEntity("<empty></empty>",header,OK);
    }

    @RequestMapping(value="/datas/{dataId}/export",method=GET,produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity exportData(@PathVariable("dataId") final int dataId) {

        final Path[] filesToSend;
        try {
            filesToSend = dataBusiness.exportData(dataId);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }

        if (filesToSend.length == 0) {
            LOGGER.info("No files for this data to export.");
            return new ErrorMessage(NO_CONTENT).message("No files for this data to export.").build();
        }

        if (filesToSend.length == 1 && !Files.isDirectory(filesToSend[0])) {
            final Path f = filesToSend[0];
            final FileSystemResource r = new FileSystemResource(f.toFile());

            final HttpHeaders header = new HttpHeaders();
            header.set("Content-Disposition", "attachment; filename=" + f.getFileName().toString());

            return new ResponseEntity(r, header,OK);
        }

        //create ZIP
        try {
            final Path zip = Files.createTempFile("exported_data" , ".zip");
            Files.deleteIfExists(zip);
            ZipUtilities.zipNIO(zip, filesToSend);

            final FileSystemResource r = new FileSystemResource(zip.toFile());

            final HttpHeaders header = new HttpHeaders();
            header.set("Content-Disposition", "attachment; filename=exported_data.zip");

            return new ResponseEntity(r, header, OK);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,"Error while zipping data",ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/export",method=POST,produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity exportDatas(@RequestBody final List<Integer> dataIds) {
        try {

            final Path archive = Files.createTempDirectory("export-datas");
            final List<Path> toSend = new ArrayList<>();

            // has for now exporting a data will export in fact all the data in the provider
            // we export only one data by provider to avoid having doublon.
            // TODO remove this hack when the data will be properly exported
            Set<Integer> alreadyVisitedProvider = new HashSet<>();
            for (Integer dataId : dataIds) {
                try {
                    Data d = dataBusiness.getData(dataId);
                    Integer pid = d.getProviderId();
                    if (!alreadyVisitedProvider.contains(pid)) {
                        final Path[] dataFiles = dataBusiness.exportData(dataId);
                        alreadyVisitedProvider.add(pid);
                        if (dataFiles.length == 0) {
                            LOGGER.warning("No files for the data " + d.getId() + " to export.");
                        } else {
                            Path dataDir = archive.resolve(pid + "_" + d.getName());
                            Files.createDirectory(dataDir);
                            for (Path f : dataFiles) {
                                Path dst = dataDir.resolve(f.getFileName());
                                IOUtilities.copy(f, dst);
                            }
                            toSend.add(dataDir);
                        }
                    }

                } catch (ConstellationException | IOException ex) {
                    LOGGER.log(Level.WARNING, "Error while trying to export data: " + dataId, ex);
                    return new ErrorMessage(ex).build();
                }
            }
        
            //create ZIP
            final Path zip = Files.createTempFile("exported_data" , ".zip");
            Files.deleteIfExists(zip);
            ZipUtilities.zipNIO(zip, toSend.toArray(Path[]::new));

            final FileSystemResource r = new FileSystemResource(zip.toFile());

            final HttpHeaders header = new HttpHeaders();
            header.set("Content-Disposition", "attachment; filename=exported_data.zip");

            return new ResponseEntity(r, header, OK);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,"Error while zipping data",ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return a map of the associations of the data (styles, services, sensors).
     *
     * @param dataId identifier of the data.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/associations",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDataAssociations(@PathVariable("dataId") int dataId) {

        if (dataBusiness.existsById(dataId)) {
            Map<String, Object> entity = dataBusiness.getDataAssociations(dataId);
            return new ResponseEntity(entity, OK);
        }
        return new ErrorMessage(NOT_FOUND).build();
    }

    /**
     *  Remove the association between a data and a style.
     *
     * @param dataId Identifier of the data.
     * @param styleId Identifier of the Style.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/styles/{styleId}",method=DELETE,produces=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity deleteStyleAssociation(@PathVariable("dataId") int dataId, @PathVariable("styleId") int styleId) {

        if (dataBusiness.existsById(dataId) && styleBusiness.existsStyle(styleId)) {
            try {
                styleBusiness.unlinkFromData(styleId, dataId);
                return new ResponseEntity(NO_CONTENT);
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ErrorMessage(NOT_FOUND).build();
    }

    /**
     *  Remove the association between a data and all the linked styles.
     *
     * @param dataId Identifier of the data.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/styles",method=DELETE, produces=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity deleteStyleAssociation(@PathVariable("dataId") int dataId) {

        if (dataBusiness.existsById(dataId)) {
            try {
                styleBusiness.unlinkAllFromData(dataId);
                return new ResponseEntity(NO_CONTENT);
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ErrorMessage(NOT_FOUND).build();
    }

    /**
     *  Remove the association between multiple data and all its linked styles.
     *
     * @param dataIds Identifier of the data.
     * @return
     */
    @RequestMapping(value="/datas/styles/unlink",method=DELETE, consumes=APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity deleteStyleAssociation(@RequestBody List<Integer> dataIds) {
        try {
            for (Integer dataId : dataIds) {
                if (dataBusiness.existsById(dataId)) {
                    styleBusiness.unlinkAllFromData(dataId);
                    return new ResponseEntity(NO_CONTENT);
                }
            }
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *  Add an association between a data and a sensor.
     *
     * @param dataId Identifier of the data.
     * @param sensorId Identifier of the sensor.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/sensors/{sensorId}",method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addSensorAssociation(@PathVariable("dataId") final int dataId, @PathVariable("sensorId") final int sensorId) {
        sensorBusiness.linkDataToSensor(dataId, sensorId);
        return new ResponseEntity(OK);
    }

    /**
     *  Remove an association between a data and a sensor.
     *
     * @param dataId Identifier of the data.
     * @param sensorId Identifier of the sensor.
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/sensors/{sensorId}",method=DELETE,produces=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity deleteSensorAssociation(@PathVariable("dataId") int dataId,  @PathVariable("sensorId") int sensorId) {
        sensorBusiness.unlinkDataToSensor(dataId, sensorId);
        return new ResponseEntity(NO_CONTENT);
    }

    /**
     * Return a description of the data.
     *
     * @param dataId Data Identifier
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/description",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity dataDescription(@PathVariable("dataId") final int dataId) {
        try {
            DataBrief db = dataBusiness.getDataBrief(dataId, true, false);
            if (db != null) {
                 return new ResponseEntity(db.getDataDescription() ,OK);
            }
            return new ResponseEntity(NOT_FOUND);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return The geographic extent of the data.
     *
     * @param dataId Data Identifier
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/geographicExtent",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity dataGeographicExtent(@PathVariable("dataId") final int dataId) {
        try {
            DataBrief db = dataBusiness.getDataBrief(dataId, true, false);
            if (db != null) {
                return new ResponseEntity(db.getDataDescription() ,OK);
            }
            return new ResponseEntity(NOT_FOUND);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return a description tree of given resource.
     *
     * @param dataId Data Identifier
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/rawModel",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity dataRawModel(@PathVariable("dataId") final int dataId) {
        try {
            Map<String, Object> db = dataBusiness.getDataRawModel(dataId);
            if (db != null) {
                return new ResponseEntity(db ,OK);
            }
            return new ResponseEntity(NOT_FOUND);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/geographicExtent/merge",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity mergedDataGeographicExtent(@RequestBody final List<Integer> dataIds) {

        final Map<String,double[]> result = new HashMap<>();
        if(dataIds == null || dataIds.isEmpty()){
            return new ResponseEntity(result,OK);
        }
        GeneralEnvelope globalEnv = null;
        for (final Integer dataId : dataIds) {
            try {
                final DataBrief db = dataBusiness.getDataBrief(dataId, true, false);
                final DataDescription ddesc = db.getDataDescription();
                if (ddesc != null) {
                    final double[] bbox = ddesc.getBoundingBox();
                    final GeneralEnvelope dataEnv = new GeneralEnvelope(CommonCRS.defaultGeographic());
                    dataEnv.setRange(0,bbox[0],bbox[2]);
                    dataEnv.setRange(1,bbox[1],bbox[3]);
                    if(globalEnv == null) {
                        globalEnv = dataEnv;
                    }else {
                        globalEnv.add(dataEnv);
                    }
                } else {
                    LOGGER.warning("Null dataDescription for data:" + db.getName());
                }
            }catch(Exception ex){
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }
        }
        if(globalEnv != null){
            double[] bbox = new double[4];
            bbox[0]=globalEnv.getMinimum(0);
            bbox[1]=globalEnv.getMinimum(1);
            bbox[2]=globalEnv.getMaximum(0);
            bbox[3]=globalEnv.getMaximum(1);
            result.put("boundingBox",bbox);
        }
        return new ResponseEntity(result,OK);
    }

    /**
     * Compute and store data informations into the datasource.
     *
     * @param dataId Data identifier.
     * @param refresh if set to {@code false} the informations will not be updated if already recorded.
     * @return
     */
    @RequestMapping(value = "/datas/{dataId}/compute/info", method = GET)
    public ResponseEntity computeDataInfo(@PathVariable("dataId") final int dataId, @RequestParam(name = "refresh", required = false, defaultValue = "false") final Boolean refresh) {
        try {
            dataBusiness.cacheDataInformation(dataId, refresh);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Compute and store all the data informations into the datasource.
     *
     * @param refresh if set to {@code false} the informations will not be updated if already recorded.
     * @param dataset if set only the dataset datas will be computed.
     * @return
     */
    @RequestMapping(value = "/datas/compute/info", method = GET)
    public ResponseEntity computeDatasInfo(@RequestParam(name = "refresh", required = false) final Boolean refresh, @RequestParam(name = "dataset", required = false) final Integer dataset, HttpServletRequest req) {
        try {
            Integer usedId = assertAuthentificated(req);
            TaskContext tc = buildcomputeInfoProcess(usedId, refresh, dataset);

            processBusiness.runProcess("Compute data informations.", tc.p, tc.taskId, usedId);

            return new ResponseEntity(tc.taskId, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private static final class TaskContext {
        public org.geotoolkit.process.Process p;
        public Integer taskId;
        public TaskContext(org.geotoolkit.process.Process p,Integer taskId) {
            this.p = p;
            this.taskId = taskId;
        }
    }

    private TaskContext buildcomputeInfoProcess(Integer userId, Boolean refresh, Integer dataset) throws ConstellationException {
        try {
            final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("examind", "data.cache.info");
            final ParameterValueGroup input = desc.getInputDescriptor().createValue();
            input.parameter("refresh").setValue(refresh);
            if (dataset != null) {
                input.parameter("dataset").setValue(new DatasetProcessReference(dataset, null));
            }
            final org.geotoolkit.process.Process p = desc.createProcess(input);

            //add task in scheduler
            final String taskName = "compute data info - " + System.currentTimeMillis();
            TaskParameter taskParameter = new TaskParameter();
            taskParameter.setProcessAuthority(Util.getProcessAuthorityCode(desc));
            taskParameter.setProcessCode(desc.getIdentifier().getCode());
            taskParameter.setDate(System.currentTimeMillis());
            taskParameter.setInputs(ParamUtilities.writeParameterJSON(input));
            taskParameter.setOwner(userId);
            taskParameter.setName(taskName);
            taskParameter.setType("INTERNAL");
            Integer taskId = processBusiness.addTaskParameter(taskParameter);

            return new TaskContext(p, taskId);
        } catch (NoSuchIdentifierException | JsonProcessingException ex) {
            throw new ConstellationException("Error while tiling data", ex);
        }
    }
}
