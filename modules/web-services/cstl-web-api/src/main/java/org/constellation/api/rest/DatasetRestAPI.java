/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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

import java.nio.charset.StandardCharsets;
import org.constellation.dto.DataSetBrief;
import org.constellation.dto.DataBrief;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.api.ProviderType;

import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.Data;
import org.constellation.dto.Filter;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.Sort;
import org.constellation.dto.metadata.RootObj;
import org.constellation.exception.ConstellationException;
import org.constellation.json.metadata.Template;
import org.constellation.json.metadata.bean.TemplateResolver;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author guilhem
 */
@RestController
public class DatasetRestAPI extends AbstractRestAPI {

    @Autowired
    private IDatasetBusiness datasetBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IStyleBusiness styleBusiness;

    @Autowired
    private TemplateResolver templateResolver;

    private static final String AGG_COV_DATASET_NAME = "Aggregated-Coverage-Dataset";

    @RequestMapping(value="/datasets",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasetList(@RequestParam(value="includeData", required=true, defaultValue = "false") boolean includeData,
                                         @RequestParam(value="published",   required=false) Boolean published,
                                         @RequestParam(value="sensorable",  required=false) Boolean sensorable) {

        try {
            final List<DataSetBrief> datasetBriefs = new ArrayList<>();
            final List<Integer> datasets = datasetBusiness.getAllDatasetIds();
            for (final Integer datasetId : datasets) {
                final List<DataBrief> briefs;
                if (includeData) {
                    briefs = dataBusiness.getDataBriefsFromDatasetId(datasetId, true, false, sensorable, published, true, true);
                    if (briefs.isEmpty()) {
                        continue;
                    }
                } else {
                    briefs = new ArrayList<>();
                }
                datasetBriefs.add(buildDatsetBrief(datasetId, briefs, true, true));
            }
            return new ResponseEntity(datasetBriefs, OK);
        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasets", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity create(@RequestParam(name = "identifier") final String identifier,
            @RequestParam(name = "hidden", required = false, defaultValue = "false") boolean hidden,
            @RequestBody final RootObj metadataValues, HttpServletRequest req) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final int userId           = assertAuthentificated(req);
            if (!identifier.isEmpty()) {
                if (datasetBusiness.existsByName(identifier)) {
                    LOGGER.log(Level.WARNING, "Dataset with identifier {0} already exist", identifier);
                    return new ResponseEntity("failed", HttpStatus.CONFLICT);
                }
                Integer dsId = datasetBusiness.createDataset(identifier, userId, null);

                // save the new metadata
                final Template template = templateResolver.getByName("profile_import");
                final Object metadata = template.emptyMetadata();
                template.read(metadataValues, metadata, true);
                String mdIdentifier = template.getMetadataIdentifier(metadata);
                if (mdIdentifier == null) {
                    mdIdentifier = UUID.randomUUID().toString();
                    template.setMetadataIdentifier(mdIdentifier, metadata);
                }
                if (metadata instanceof DefaultMetadata) {
                    ((DefaultMetadata)metadata).setDateStamp(new Date());
                }
                datasetBusiness.updateMetadata(dsId, metadata, hidden);

                // get the brief to return
                DataSetBrief brief = buildDatsetBrief(dsId, new ArrayList<>(), true, true);

                return new ResponseEntity(brief, OK);
            } else {
                LOGGER.log(Level.WARNING, "Cannot create dataset with empty identifier");
                return new ResponseEntity("failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Update the dataset metadata.
     *
     * @param datasetId Identifier of the dataset.
     * @param metadataValues the values of metadata editor.
     * @return
     */
    @RequestMapping(value="/datasets/{datasetId}/metadata",method=POST,consumes=APPLICATION_JSON_VALUE)
    public ResponseEntity updateDatasetMetadata(@PathVariable("datasetId") final int datasetId,
            @RequestBody final RootObj metadataValues) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            metadataBusiness.mergeDatasetMetadata(datasetId, metadataValues);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).message("Error while saving dataset metadata").build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Used to open metadata editor form.
     * the metadata.prune() should never be called in this method.
     * Returns json result of template writer to apply a given template to metadata object.
     * The path of each fields/blocks will be numerated.
     * the returned json object will be used directly in html metadata editor.
     *
     * @param datasetId dataset identifier.
     * @param prune flag that indicates if template result will clean empty children/block.
     * @return {@code Response}
     */
    @RequestMapping(value="/datasets/{datasetId}/metadata",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> getDatasetMetadata(final @PathVariable("datasetId") int datasetId,
                                                     final @RequestParam(name="prune") boolean prune) {
        try {
            final String buffer = metadataBusiness.getJsonDatasetMetadata(datasetId, prune, false);
            return ResponseEntity.ok(new ByteArrayResource(buffer.getBytes(StandardCharsets.UTF_8)));
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "error while writing metadata json.", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Deletes the dataset with the specified {@literal datasetId}.
     *
     * @param datasetId the dataset id.
     * @return a {@link ResponseEntity} with the appropriate HTTP status (and entity).
     */
    @RequestMapping(value = "/datasets/{datasetId}", method = DELETE)
    public ResponseEntity deleteDataset(@PathVariable(value = "datasetId") int datasetId) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            if (datasetBusiness.existsById(datasetId)) {
                datasetBusiness.removeDataset(datasetId);
                return new ResponseEntity(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).message("Failed to remaove dataset").build();
        }
    }

    /**
     * return true if the dataset name already exist
     *
     * @param datasetName the dataset name.
     *
     * @return {@code true} if the dataset name is already used.
     */
    @RequestMapping(value = "/datasets/name/{datasetName}/exist", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity existDatasetName(@PathVariable(value = "datasetName") String datasetName) {
        return new ResponseEntity(datasetBusiness.existsByName(datasetName), OK);
    }

    /**
     * Lists the data of the dataset with the specified {@literal datasetId}.
     *
     * @param datasetId the dataset id.
     * @return the {@link List} of {@link DataBrief}s.
     */
    @RequestMapping(value = "/datasets/{datasetId}/datas", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasetData(@PathVariable(value = "datasetId") int datasetId) {
        try {
            if (datasetBusiness.existsById(datasetId)) {
                return new ResponseEntity(dataBusiness.getDataBriefsFromDatasetId(datasetId, true, false, null, null, true, false), OK);
            }
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value="/datasets/{datasetId}/datas",method=POST, consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity updateDataDataset(@RequestBody List<Integer> dataIds, @PathVariable(value = "datasetId") int datasetId) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            Integer dsId = datasetId;
            if (datasetId == -1) {
                dsId = null;
            }
            for (Integer dataId : dataIds) {
                dataBusiness.updateDataDataSetId(dataId, dsId);
            }
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
    
    @RequestMapping(value="/datasets/{datasetId}/identifier/{newIdentifier}",method=POST)
    public ResponseEntity updateDatasetIdentifier(@PathVariable(value = "datasetId") int datasetId, @PathVariable(value = "newIdentifier") String newIdentifier) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            if (!datasetBusiness.existsById(datasetId)) {
                return new ResponseEntity(NOT_FOUND);
            }
            if (datasetBusiness.existsByName(newIdentifier)) {
                return new ResponseEntity("Dataset identifier already in use", HttpStatus.BAD_REQUEST);
            }
            datasetBusiness.updateDatasetIdentifier(datasetId, newIdentifier);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }


    /**
     * Returns a page of datasets matching the specified search criteria.
     *
     * @param pagedSearch the search information.
     * @return the {@link Page} of {@link DataSetBrief}s.
     */
    @RequestMapping(value="/datasets/search",method=POST, consumes = APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity searchDatasets(@RequestBody PagedSearch pagedSearch, HttpServletRequest req) {
        try {
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

            // Perform search.
            final Map.Entry<Integer,List<DataSetBrief>> entry = datasetBusiness.filterAndGetBrief(filterMap,sortEntry,pageNumber,rowsPerPage);
            final int total = entry.getKey();
            List<DataSetBrief> results = entry.getValue();


//            // Extract ids of datasets that contain only one data ("singleton").
//            Collection<Integer> singletonIds = Lists.transform(
//                    results,
//                    (DataSetBrief dataset) -> (dataset.getDataCount() == 1) ? dataset.getId() : null
//            );
//
//            // no "singleton" datasets -> Build and return the content list of page.
//            if (!singletonIds.isEmpty()) {
//
//                // Query the single data of these datasets.
//                final Map<Integer, DataBrief> indexedData = new HashMap<>();
//                for (Integer dsid : singletonIds) {
//                    if (dsid != null) {
//                        List<DataBrief> datas = dataBusiness.getDataBriefsFromDatasetId(dsid);
//                        indexedData.put(dsid, datas.get(0));
//                    }
//                }
//
//
//                // Map single data of each "singleton" dataset in response.
//                results = Lists.transform(results, new Function<DataSetBrief, DataSetBrief>() {
//                    @Override
//                    public DataSetBrief apply(DataSetBrief dataset) {
//                        if (dataset.getDataCount() == 1 && indexedData.containsKey(dataset.getId())) {
//                            return datasetBusiness.getSingletonDatasetBrief(dataset, Arrays.asList(indexedData.get(dataset.getId())));
//                        }
//                        return dataset;
//                    }
//                });
//            }

            return new ResponseEntity(new Page<DataSetBrief>()
                    .setNumber(pageNumber)
                    .setSize(rowsPerPage)
                    .setContent(results)
                    .setTotal(total), OK);

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, "Error while searching datasets", ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map.Entry<String, Object> transformFilter(Filter f, final HttpServletRequest req) {
        Map.Entry<String, Object> result = super.transformFilter(f, req);
        if (result != null) {
            return result;
        }
        String value = f.getValue();
        if (value == null || "_all".equals(value)) {
            return null;
        }
        if ("hasVectorData".equals(f.getField()) || "hasCoverageData".equals(f.getField()) || "hasLayerData".equals(f.getField()) ||
            "hasSensorData".equals(f.getField()) || "excludeEmpty".equals(f.getField())) {

            return new AbstractMap.SimpleEntry<>(f.getField(), Boolean.parseBoolean(value));

        } else if ("id".equals(f.getField())) {
            try {
                final int parentId = Integer.valueOf(value);
                return new AbstractMap.SimpleEntry<>("id", parentId);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Filter by " + f.getField() + " value should be an integer: " + ex.getLocalizedMessage(), ex);
                return null;
            }

        // just here to list the existing filter
        } else if ("term".equals(f.getField())) {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        } else {
            return new AbstractMap.SimpleEntry<>(f.getField(), value);
        }
    }

    /**
     * Build {@link DataSetBrief} instance.
     * @param dataSetId given dataset object.
     * @return {@link DataSetBrief} built from the given dataset.
     */
    private DataSetBrief buildDatsetBrief(final int dataSetId, List<DataBrief> children, boolean fetchDataDescription, boolean fetchDataAssociations) throws ConstellationException {
        if (children == null) {
            children = dataBusiness.getDataBriefsFromDatasetId(dataSetId, true, false, null, null, fetchDataDescription, fetchDataAssociations);
        }
        final DataSetBrief dsb = datasetBusiness.getDatasetBrief(dataSetId, children);
        return dsb;
    }

    /**
     * Return {@code true} if the specified dataset contains at least 2 coverages.
     *
     * @param datasetId The dataset identifier.
     * @param req
     * @return
     */
    @RequestMapping(value = "/datasets/coverage-aggregation/{datasetId}", method = GET)
    public ResponseEntity isCoverageAggregationDatasetCandidate(@PathVariable("datasetId") int datasetId, HttpServletRequest req) {
        try {
            int cpt = 0;
            List<DataBrief> data = dataBusiness.getDataBriefsFromDatasetId(datasetId, true, false, null, null, false, false);
            for (DataBrief db : data) {
                if ("COVERAGE".equalsIgnoreCase(db.getType())) {
                    cpt++;
                }
                if (cpt > 1) {
                    return new ResponseEntity(true, OK);
                }
            }
            return new ResponseEntity(false, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Build a new coevrage aggregation from a list of coverage data in the specified dataset.
     *
     * @param datasetId The dataset identifier.
     * @param dataName The assigned data name of the aggregated data.
     * @param req
     * @return
     */
    @RequestMapping(value = "/datasets/coverage-aggregation/{datasetId}", method = PUT)
    public ResponseEntity createCoverageAggregation(@PathVariable("datasetId") int datasetId, @RequestParam("dataName") String dataName, HttpServletRequest req) {
        if (readOnlyAPI) return readOnlyModeActivated();
        try {
            final int userId       = assertAuthentificated(req);

            // 1. extract the S57 data and associatd styles in this dataset.
            final DataAndStyle dns = getDataIdsToCombine(datasetId);

            // 2. build an Ecdis provider
            String providerIdentifier = "aggCovSrc" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
            final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("AggregatedCoverageProvider");

            final GeneralParameterDescriptor dataIdsDesc = config.getDescriptor().descriptor("data_ids");
            for (Integer dataId : dns.dataIds) {
                ParameterValue p = (ParameterValue) dataIdsDesc.createValue();
                p.setValue(dataId);
                config.values().add(p);
            }
            config.parameter("DataName").setValue(dataName);

            // 3. store the provider
            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "computed-resource", source);

            // 4. get or create a static dataset for the ecdis products.
            Integer dsid = datasetBusiness.getDatasetId(AGG_COV_DATASET_NAME);
            if (dsid == null) {
                dsid = datasetBusiness.createDataset(AGG_COV_DATASET_NAME, userId, null);
            }

            // 5. generate the data in the static dataset and link them with the available styles
            providerBusiness.createOrUpdateData(pid, dsid, false, false, userId);
            List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(pid);
            for (Integer did : dataIds) {
                for (Integer sid : dns.styleIds) {
                    styleBusiness.linkToData(sid, did);
                }
            }
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private static class DataAndStyle {
        Set<Integer> dataIds  = new HashSet<>();
        Set<Integer> styleIds = new HashSet<>();
    }

    /**
     * Extract all the S57 data in this dataset along with the styles they are associated to.
     *
     * @param datasetId The dataset identifier.
     *
     * @return A list of data ids and styles ids.
     * @throws ConstellationException
     */
    private DataAndStyle getDataIdsToCombine(int datasetId) throws ConstellationException  {
        DataAndStyle results = new DataAndStyle();
        List<DataBrief> data = dataBusiness.getDataBriefsFromDatasetId(datasetId, true, false, null, null, false, true);
        for (DataBrief db : data) {
            if ("COVERAGE".equalsIgnoreCase(db.getType())) {
                results.dataIds.add(db.getId());
                results.styleIds.addAll(db.getTargetStyle().stream().map(s -> s.getId()).collect(Collectors.toList()));
            }
        }
        return results;
    }
    
    @RequestMapping(value="/datasets/{datasetId}/export",method=POST,produces=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity exportDataset(@PathVariable("datasetId") int datasetId) {
        try {

            final Path archive = Files.createTempDirectory("export-datas");
            final List<Path> toSend = new ArrayList<>();
            final List<Integer> dataIds = dataBusiness.getDataIdsFromDatasetId(datasetId, true, false);

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
}
