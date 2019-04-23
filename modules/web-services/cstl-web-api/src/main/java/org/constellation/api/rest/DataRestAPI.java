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

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.CRC32;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.WritableAggregate;
import static org.constellation.api.rest.AbstractRestAPI.LOGGER;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.DataSetBrief;
import org.constellation.dto.Filter;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.ProviderData;
import org.constellation.dto.Sort;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.metadata.utils.Utils;
import static org.constellation.metadata.utils.Utils.UNKNOW_IDENTIFIER;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.GeoData;
import org.constellation.util.MetadataMerger;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.coverage.xmlstore.XMLCoverageResource;
import org.geotoolkit.data.multires.DefiningPyramid;
import org.geotoolkit.data.multires.Pyramids;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.storage.coverage.CoverageStore;
import org.geotoolkit.storage.coverage.DefiningCoverageResource;
import org.geotoolkit.storage.coverage.PyramidalCoverageResource;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.util.NamesExt;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.multipart.MultipartFile;

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

    @Inject
    private IStyleBusiness styleBusiness;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IDatasetBusiness datasetBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private ISensorBusiness sensorBusiness;

    @Inject
    private IMapContextBusiness mapContextBusiness;

    @Inject
    private IMetadataBusiness metadataBusiness;

    @Inject
    private IProcessBusiness processBusiness;


    /**
     * Receive a {@link MultipartFile} which contain a data file to save on server,
     * or a path to a file already on the server.
     *
     * - Move uploaded data to integrated folder.
     * - Create the provider.
     * - Verify the CRS validity.
     * - Launch pyramidage for raster data
     * - Initialize extracted metadata for dataset / datas
     * - Finally return the data briefs created.
     *
     * @param data An uploaded data file.
     * @param serverPath A path to a data file already on the server.
     * @param dataType The type of the data (example : raster, vector, ...).
     * @param fileExtension The file extension.
     * @param req
     *
     * @return A list of Databrief extracted from the uploaded file.
     */
    @RequestMapping(value="/datas/upload",method=POST, consumes=MULTIPART_FORM_DATA_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadData(@RequestParam(name = "data", required = false) MultipartFile data,
                                     @RequestParam(name = "serverPath", required = false) String serverPath,
                                     @RequestParam("dataType") String dataType,
                                     @RequestParam("extension") String fileExtension,
                                     HttpServletRequest req) {

        try {
            final Path uploadDirectory = getUploadDirectory();
            final int userId           = assertAuthentificated(req);
            final String fileName;
            if (data != null) {
                fileName = data.getOriginalFilename();
            } else if (serverPath != null) {
                final int lastSeparator = serverPath.lastIndexOf(File.separator);
                fileName = serverPath.substring(lastSeparator + 1);
            } else {
                return new ResponseEntity("Neither local nor server file has been provided.", HttpStatus.BAD_REQUEST);
            }
            final String datasetName   = FilenameUtils.removeExtension(fileName);
            final String providerId    = fileName + '-' + UUID.randomUUID().toString();
            String dataFile;

            // Server mode
            if (serverPath !=null && !serverPath.isEmpty()){
                dataFile = serverPath;
            }

            // Upload mode
            else {

                // 1. Move the file to upload directory
                Path newFileData = uploadDirectory.resolve(fileName);
                if (!data.isEmpty()) {
                    try(InputStream in = data.getInputStream()){
                        Files.copy(in, newFileData, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                // 2. init provider directory
                final Path integratedDir = ConfigDirectory.getDataIntegratedDirectory();
                final Path providerDir   = integratedDir.resolve(providerId);
                final Path dataDir       = providerDir.resolve(fileName);
                if (Files.exists(dataDir)) {
                    IOUtilities.deleteRecursively(dataDir);
                }
                Files.createDirectories(dataDir);

                // 3. unzip if needed
                String ext = IOUtilities.extension(newFileData);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(newFileData, dataDir, new CRC32());
                    newFileData = dataDir.toAbsolutePath();
                }

                // 4. move to integrated
                if (newFileData.startsWith(uploadDirectory.toAbsolutePath())) {
                    final Path destFile = dataDir.toAbsolutePath().resolve(newFileData.getFileName().toString());
                    Files.move(newFileData, destFile, StandardCopyOption.REPLACE_EXISTING);
                    newFileData = destFile;
                }

                dataFile = newFileData.toAbsolutePath().toUri().toString();
            }


            final String uploadType = RestApiUtil.findDataType(dataFile, fileExtension, dataType);

            // 5. determine the provider type
            String subType;
            if ("vector".equalsIgnoreCase(uploadType)) {
                String[] extracted = DataProviders.findFeatureFactoryForFiles(dataFile, FACTORY_COMPARATOR);
                dataFile           = extracted[0];
                subType            = extracted[1];

            } else if("raster".equalsIgnoreCase(uploadType)) {
                subType = "coverage-file";

            } else if ("observation".equalsIgnoreCase(uploadType)) {
                subType = "observationFile";
                if ("xml".equalsIgnoreCase(fileExtension)) {
                    subType = "observationXmlFile";
                }
            } else {
                // TODO remove data if throw an exception?
                throw new UnsupportedOperationException("The uploaded file is not recognized or not supported by the application. file:" + uploadType);
            }

            // 6. create dataset with unique name
            String freeDatasetName = datasetName;
            int i = 1;
            while (datasetBusiness.existsByName(freeDatasetName)) {
                freeDatasetName = datasetName + "_" + i;
                i++;
            }
            final Integer dsId = datasetBusiness.createDataset(freeDatasetName, userId, null);

            // 7. create provider and all its datas with an hidden state
            final ProviderConfiguration config = new ProviderConfiguration("data-store", subType, dataFile);
            final Integer prId = providerBusiness.create(providerId, config);
            providerBusiness.createOrUpdateData(prId, dsId, true, true, userId);

            // 8. verify CRS for vector and raster
            if ("vector".equalsIgnoreCase(uploadType) || "raster".equalsIgnoreCase(uploadType)) {
                verifyCRS(prId); // TODO remove data if throw an exception?
            }

            // 9. retrieve all the newly create datas
            final List<DataBrief> briefs = dataBusiness.getDataBriefsFromDatasetId(dsId, true, true, null, null);

            return new ResponseEntity(briefs, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/{dataId}/accept",method=POST, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity acceptData(@PathVariable Integer dataId, @RequestParam(name="hidden", required = false, defaultValue = "false") Boolean hidden, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            return new ResponseEntity(dataBusiness.acceptData(dataId, userId, hidden), OK);

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/datas/accept",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity acceptDatas(@RequestBody List<Integer> dataIds, @RequestParam(name="hidden", required = false, defaultValue = "false") Boolean hidden, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            return new ResponseEntity(dataBusiness.acceptDatas(dataIds, userId, hidden), OK);

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
                DataBrief brief = dataBusiness.getDataBrief(dataId);
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
     * Receive a {@link MultipartFile} which contain a metadata file to save on server,
     * or a path to a file already on the server.
     *
     * The metadata object will be extracted from the file, saved in the database
     * and then bounded to the specified data.
     *
     *
     * @param metadata An uploaded metadata file.
     * @param dataId The data identifier.
     * @param serverMetadataPath A path to a metadata file already on the server.
     *
     * @return A {@link ResponseEntity} with 200 code if upload work, 500 if not work.
     */
    @RequestMapping(value="/datas/{dataId}/metadata/upload",method=POST, consumes=MULTIPART_FORM_DATA_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadDataMetadata(
            @RequestParam(name = "metadata", required = false) MultipartFile metadata,
            @RequestParam(name = "serverMetadataPath", required = false) String serverMetadataPath,
            @PathVariable("dataId") Integer dataId) {

        try {
            Path metadataFile = null;

            // Server mode
            if (serverMetadataPath !=null && !serverMetadataPath.isEmpty()){
                metadataFile = IOUtilities.toPath(serverMetadataPath);

            // Upload mode
            } else  if (!metadata.getOriginalFilename().isEmpty()) {

                final Path uploadDirectory = getUploadDirectory();
                final Path newFileMetaData = uploadDirectory.resolve(metadata.getOriginalFilename());
                if (!metadata.isEmpty()) {
                    try (InputStream in = metadata.getInputStream()) {
                        Files.copy(in, newFileMetaData, StandardCopyOption.REPLACE_EXISTING);
                        metadataFile = newFileMetaData;
                    }
                }
            }

            if (metadataFile != null) {

                // 1. Extract metadata Object
                Object obj = metadataBusiness.getMetadataFromFile(metadataFile);
                if (obj == null) {
                    throw new ConstellationException("metadata file is incorrect");
                }
                final String metaIdentifier = Utils.findIdentifier(obj);
                if (UNKNOW_IDENTIFIER.equals(metaIdentifier)) {
                    throw new ConstellationException("metadata does not contains any identifier," +
                            " please check the fileIdentifier in your metadata.");
                }

                // 2. Update metadata and bound it to the data
                dataBusiness.updateMetadata(dataId, obj, false);
            }
        } catch (ConstellationException | IOException ex) {
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Verify if a valid Coordinate Reference System has been found on the specified provider.
     *
     * @param providerId The provider identifier.
     *
     * @throws ConfigurationException if CRS can not be accessed, if it is null or invalid
     */
    private void verifyCRS(int providerId) throws ConfigurationException {
        try {
            final Map<GenericName, CoordinateReferenceSystem> nameCoordinateReferenceSystemHashMap = DataProviders.getCRS(providerId);
            for(final CoordinateReferenceSystem crs : nameCoordinateReferenceSystemHashMap.values()){
                if (crs == null || crs instanceof ImageCRS) {
                    throw new DataStoreException("CRS is null or is instance of ImageCRS");
                }
            }
        } catch (DataStoreException e) {
            throw new ConfigurationException("Cannot get CRS for provider " + providerId);
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
    public ResponseEntity getDataMetadata(final @PathVariable("dataId") int dataId, final @RequestParam(name="prune", defaultValue = "false") String prune) {

        try {
            final String buffer = metadataBusiness.getJsonDataMetadata(dataId, Boolean.parseBoolean(prune), false);
            return new ResponseEntity(buffer, OK);
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
            final Map.Entry<Integer,List<MetadataBrief>> entry = metadataBusiness.filterAndGetBrief(
                    Collections.singletonMap("term", term),null,1,Integer.MAX_VALUE);

            for (final MetadataBrief md : entry.getValue()) {
                if (md.getDatasetId() != null) {
                    final DataSetBrief dsb = buildDatsetBrief(md.getDatasetId(), null);
                    briefs.addAll(dsb.getData());
                } else if (md.getDataId() != null) {
                    final DataBrief db = dataBusiness.getDataBrief(md.getDataId());
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
                return new AbstractMap.SimpleEntry<>("parent", parentId);
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
    private DataSetBrief buildDatsetBrief(final int dataSetId, List<DataBrief> children){
        if (children == null) {
            children = dataBusiness.getDataBriefsFromDatasetId(dataSetId);
        }
        final DataSetBrief dsb = datasetBusiness.getDatasetBrief(dataSetId, children);
        return dsb;
    }

    /**
     * Generates a pyramid on a data in the given provider, create and return this new provider.
     *
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param dataId the given data id.
     * @param req
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/pyramid/",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity pyramidData(@PathVariable("dataId") final int dataId,
            HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            final DataBrief db = providerBusiness.createPyramidConform(dataId, userId);
            // link original data with the tiled data.
            if (db != null) {
                dataBusiness.linkDataToData(dataId, db.getId());
            }
            final ProviderData ref = new ProviderData(db.getProvider(), db.getName());
            return new ResponseEntity(ref, OK);
        }catch(ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Generates a pyramid on a list of data and create and return this new provider.
     * Creates btw a mapcontext that contains internal data.
     * N.B : It creates a styled pyramid, which can be used for display purposes, but not for analysis.
     *
     * @param crs
     * @param layerName
     * @param dataIds
     * @param req
     * @return
     */
    @RequestMapping(value="/datas/pyramid",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity pyramidDatas(@RequestParam("crs") final String crs, @RequestParam("layerName") final String layerName,
            @RequestBody final List<Integer> dataIds, HttpServletRequest req) {

        final int userId;
        try {
            userId = assertAuthentificated(req);
        } catch (ConstellationException ex) {
            return new ErrorMessage().message(ex.getMessage()).build();
        }
        final CoordinateReferenceSystem coordsys;
        if(crs != null) {
            try {
                coordsys = AbstractCRS.castOrCopy(CRS.forCode(crs)).forConvention(AxesConvention.RIGHT_HANDED);
            } catch (FactoryException ex) {
                LOGGER.log(Level.WARNING, "Invalid CRS code : "+crs, ex);
                return new ErrorMessage().message("Invalid CRS code : " + crs).build();
            }
        } else {
            coordsys = null;
        }

        final List<DataBrief> briefs = new ArrayList<>();
        for (Integer dataId : dataIds) {
            try {
               briefs.add(dataBusiness.getDataBrief(dataId));
            } catch (ConstellationException ex) {
                return new ErrorMessage().message(ex.getMessage()).build();
            }
        }

        if(!briefs.isEmpty()){
            /**
             * 1) calculate best scales array.
             *    loop on each data and determine the largest scales
             *    that wrap all data.
             */
            final List<Double> mergedScales = DataProviders.getBestScales(briefs, crs);

            /**
             * 2) creates the styled pyramid that contains all selected layers
             *    we need to loop on data and creates a mapcontext
             *    to send to pyramid process
             */
            double[] scales = new double[mergedScales.size()];
            for (int i = 0; i < scales.length; i++) {
                scales[i] = mergedScales.get(i);
            }
            final String tileFormat = "PNG";
            String firstDataProv=null;
            String firstDataName=null;
            GeneralEnvelope globalEnv = null;
            final MapContext context = MapBuilder.createContext();
            for(final DataBrief db : briefs){
                final String providerIdentifier = db.getProvider();
                final String dataName = db.getName();
                if(firstDataProv==null){
                    firstDataProv = providerIdentifier;
                    firstDataName = dataName;
                }
                //get data
                final DataProvider inProvider;
                try {
                    inProvider = DataProviders.getProvider(db.getProviderId());
                } catch (ConfigurationException ex) {
                    LOGGER.log(Level.WARNING,"Provider "+providerIdentifier+" does not exist");
                    continue;
                }

                final Data inD = inProvider.get(NamesExt.create(dataName));
                if (!(inD  instanceof GeoData)) {
                    LOGGER.log(Level.WARNING,"Data "+dataName+" does not exist in provider "+providerIdentifier + " (or is not a GeoData)");
                    continue;
                }
                final GeoData inData = (GeoData) inD;

                Envelope dataEnv;
                try {
                    dataEnv = inData.getEnvelope();
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.WARNING,"Failed to extract envelope for data "+dataName);
                    continue;
                }
                MutableStyle style = null;
                try {
                    final List<StyleBrief> styles = db.getTargetStyle();
                    if(styles != null && !styles.isEmpty()){
                        final String styleName = styles.get(0).getName();
                        style = (MutableStyle) styleBusiness.getStyle("sld",styleName);
                    }
                }catch(Exception ex){
                    LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
                }

                try {
                    //if style is null, a default style will be used in maplayer.
                    context.items().add(inData.getMapLayer(style, null));
                } catch (ConstellationStoreException ex) {
                    LOGGER.log(Level.WARNING, "Failed to create map context layer for data " + ex.getMessage(), ex);
                    continue;
                }
                if(coordsys != null) {
                    try {
                        //reproject data envelope
                        dataEnv = Envelopes.transform(dataEnv, coordsys);
                    } catch (TransformException ex) {
                        LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                        return new ErrorMessage().message("Could not transform data envelope to crs "+crs).build();
                    }
                }
                if(globalEnv == null) {
                    globalEnv = (GeneralEnvelope) dataEnv;
                }else {
                    globalEnv.add(dataEnv);
                }
            }

            globalEnv.intersect(CRS.getDomainOfValidity(coordsys));

            final String uuid = UUID.randomUUID().toString();
            final String providerId = briefs.size()==1?firstDataProv:uuid;
            final String dataName = layerName;
            context.setName("Styled pyramid " + crs + " for " + providerId + ":" + dataName);

            PyramidalCoverageResource outRef;
            String pyramidProviderId = RENDERED_PREFIX + uuid;

            //create the output provider
            final int pojoProviderID;
            final DataProvider outProvider;
            try {
                pojoProviderID = providerBusiness.createPyramidProvider(providerId, pyramidProviderId);
                outProvider = DataProviders.getProvider(pojoProviderID);

                // Update the parent attribute of the created provider
                if(briefs.size()==1){
                    providerBusiness.updateParent(outProvider.getId(), providerId);
                }
            } catch (Exception ex) {
                DataProviders.LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ErrorMessage().message("Failed to create pyramid provider " + ex.getMessage()).build();
            }

            try {
                //create the output pyramid coverage reference
                CoverageStore pyramidStore = (CoverageStore) outProvider.getMainStore();
                outRef = (XMLCoverageResource) ((WritableAggregate)pyramidStore).add(new DefiningCoverageResource(NamesExt.create(dataName)));
                outRef.setPackMode(ViewType.RENDERED);
                ((XMLCoverageResource) outRef).setPreferredFormat(tileFormat);
                //this produces an update event which will create the DataRecord
                outProvider.reload();

                pyramidStore = (CoverageStore) outProvider.getMainStore();
                outRef = (XMLCoverageResource) pyramidStore.findResource(outRef.getIdentifier().toString());
                //create database data object
                providerBusiness.createOrUpdateData(pojoProviderID, null, false);

                // Get the new data created
                final QName outDataQName = new QName(NamesExt.getNamespace(outRef.getIdentifier()), outRef.getIdentifier().tip().toString());
                final Integer dataId = dataBusiness.getDataId(outDataQName, pojoProviderID);

                //set data as RENDERED
                dataBusiness.updateDataRendered(dataId, true);

                //set hidden value to true for the pyramid styled map
                dataBusiness.updateDataHidden(dataId,true);

                //link pyramid data to original data
                for(final DataBrief db : briefs){
                    dataBusiness.linkDataToData(db.getId(), dataId);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ErrorMessage().message("Failed to create pyramid layer " + ex.getMessage()).build();
            }

            try {
                //insert a mapcontext for this pyramid of data
                final MapContextLayersDTO mapContext = new MapContextLayersDTO();
                mapContext.setOwner(userId);
                mapContext.setCrs(crs);
                mapContext.setKeywords("");
                mapContext.setWest(globalEnv.getLower(0));
                mapContext.setSouth(globalEnv.getLower(1));
                mapContext.setEast(globalEnv.getUpper(0));
                mapContext.setNorth(globalEnv.getUpper(1));
                mapContext.setName(dataName+" (wmts context)");
                final Integer mapcontextId = mapContextBusiness.create(mapContext);
                final List<MapContextStyledLayerDTO> mapcontextlayers = new ArrayList<>();
                for(final DataBrief db : briefs) {
                    final MapContextStyledLayerDTO mcStyledLayer = new MapContextStyledLayerDTO();
                    mcStyledLayer.setDataId(db.getId());
                    final List<StyleBrief> styles = db.getTargetStyle();
                    if(styles != null && !styles.isEmpty()){
                        final String styleName = styles.get(0).getName();
                        mcStyledLayer.setExternalStyle(styleName);
                        mcStyledLayer.setStyleId(styles.get(0).getId());
                    }
                    mcStyledLayer.setIswms(false);
                    mcStyledLayer.setLayerId(null);
                    mcStyledLayer.setOpacity(100);
                    mcStyledLayer.setOrder(briefs.indexOf(db));
                    mcStyledLayer.setVisible(true);
                    mcStyledLayer.setMapcontextId(mapcontextId);
                    mapcontextlayers.add(mcStyledLayer);
                }
                mapContextBusiness.setMapItems(mapcontextId, mapcontextlayers);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Can not create mapcontext for WMTS layer", ex);
            }

            //prepare the pyramid and mosaics
            try {
                final DefiningPyramid template = Pyramids.createTemplate(globalEnv, new Dimension(256, 256), scales);
                outRef.createModel(template);

                final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("administration", "gen-pyramid");
                final ParameterValueGroup input = desc.getInputDescriptor().createValue();
                input.parameter("mapcontext").setValue(context);
                input.parameter("resource").setValue(outRef);
                input.parameter("mode").setValue("rgb");
                final org.geotoolkit.process.Process p = desc.createProcess(input);

                //add task in scheduler
                TaskParameter taskParameter = new TaskParameter();
                taskParameter.setProcessAuthority(desc.getIdentifier().getAuthority().toString());
                taskParameter.setProcessCode(desc.getIdentifier().getCode());
                taskParameter.setDate(System.currentTimeMillis());
                taskParameter.setInputs(ParamUtilities.writeParameter(input));
                taskParameter.setOwner(userId);
                taskParameter.setName(context.getName() + " | " + System.currentTimeMillis());
                taskParameter.setType("INTERNAL");
                Integer newID = processBusiness.addTaskParameter(taskParameter);
                //add task in scheduler
                processBusiness.runProcess("Create " + context.getName(), p, newID, userId);

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                return new ErrorMessage().message("Data cannot be tiled. " + ex.getMessage()).build();
            }

            final ProviderData ref = new ProviderData(pyramidProviderId, dataName);
            return new ResponseEntity(ref, OK);
        }
        return new ResponseEntity("The given list of data to pyramid is empty.",OK);
    }

    @RequestMapping(value="/datas/{dataId}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getData(@PathVariable("dataId") int dataId) {

        final DataBrief db;
        try {
            db = dataBusiness.getDataBrief(dataId);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(db,OK);
    }

    @RequestMapping(value="/layers/{layerId}/data",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getLayerData(@PathVariable("layerId") int layerId) {
        try {
            final DataBrief db = dataBusiness.getDataLayer(layerId);
            return new ResponseEntity(db, OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            return new ErrorMessage(ex).build();
        }

    }

    @RequestMapping(value="/datas",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataList(@RequestParam(value="type",       required=false) String type,
                                      @RequestParam(value="published",  required=false) Boolean published,
                                      @RequestParam(value="sensorable", required=false) Boolean sensorable,
                                      @RequestParam(value="fetchDataDescription", required=false) Boolean fetchDataDescription) {
        try {
            final List<DataBrief> results = new ArrayList<>();
            // only providers that have a no parent
            final List<Integer> providerIds = providerBusiness.getProviderIdsAsInt(true);
            for (final Integer providerId : providerIds) {
                final List<DataBrief> briefs = dataBusiness.getDataBriefsFromProviderId(providerId, type, true, false, sensorable, published, fetchDataDescription);
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
            final Path zip = Paths.get(System.getProperty("java.io.tmpdir"), "exported_data.zip");
            Files.deleteIfExists(zip);
            ZipUtilities.zipNIO(zip, filesToSend);

            final FileSystemResource r = new FileSystemResource(zip.toFile());

            final HttpHeaders header = new HttpHeaders();
            header.set("Content-Disposition", "attachment; filename=" + zip.getFileName().toString());

            return new ResponseEntity(r, header, OK);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING,"Error while zipping data",ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Get the properties (columns) names for a vector data.
     *
     * @param dataId Vector data identifier
     * @return
     */
    @RequestMapping(value="/datas/{dataId}/vectorcolumns",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getVectorDataColumns(final @PathVariable("dataId") int dataId) {
        try {
            return new ResponseEntity(dataBusiness.getVectorDataColumns(dataId),OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
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
            DataBrief db = dataBusiness.getDataBrief(dataId);
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
            DataBrief db = dataBusiness.getDataBrief(dataId);
            if (db != null) {
                return new ResponseEntity(db.getDataDescription() ,OK);
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
                final DataBrief db = dataBusiness.getDataBrief(dataId);
                final DataDescription ddesc = db.getDataDescription();
                final double[] bbox = ddesc.getBoundingBox();
                final GeneralEnvelope dataEnv = new GeneralEnvelope(CRS.forCode("CRS:84"));
                dataEnv.setRange(0,bbox[0],bbox[2]);
                dataEnv.setRange(1,bbox[1],bbox[3]);
                if(globalEnv == null) {
                    globalEnv = dataEnv;
                }else {
                    globalEnv.add(dataEnv);
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
}
