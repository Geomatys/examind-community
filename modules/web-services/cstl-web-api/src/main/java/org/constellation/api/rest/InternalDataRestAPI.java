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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.RootObj;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ISO19110Builder;
import org.geotoolkit.client.AbstractClientProvider;
import static org.apache.sis.util.ArraysExt.contains;
import org.constellation.business.IProviderBusiness.SPI_NAMES;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceType;
import static org.geotoolkit.storage.ResourceType.*;
import org.opengis.feature.catalog.FeatureCatalogue;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class InternalDataRestAPI extends AbstractRestAPI {

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IDatasetBusiness datasetBusiness;

    @RequestMapping(value="/internal/datas/store/{storeId}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataStoreConfiguration(@PathVariable(name="storeId") String storeId){
        DataStoreProvider factory = DataStores.getProviderById(storeId);
        if (factory != null) {
            final DataCustomConfiguration.Type type = DataProviders.buildDatastoreConfiguration(factory, getCategory(factory), null);
            type.setSelected(true);
            return new ResponseEntity(type, OK);
        }
        return new ResponseEntity(NOT_FOUND);
    }

    /**
     * Special case to enable observation provider.
     * TODO we must fnd a generic way to determine the type od the provider.
     */
    private String getCategory(DataStoreProvider provider) {
        final String id = provider.getOpenParameters().getName().getCode();
        switch (id) {
            case "observationSOSDatabase":
            case "observationFile" :
            case "observationXmlFile": return SPI_NAMES.OBSERVATION_SPI_NAME.name;
            default : return SPI_NAMES.DATA_SPI_NAME.name;
        }
    }

    /**
     * List all FeatureStore and CoverageStore factories and there parameters.
     *
     * @param dataType filter on datastore type (raster, vector, jdbc, service, data or all).
     * @return Response {@link DataCustomConfiguration}
     */
    @RequestMapping(value="/internal/datas/store/list",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getAllDataStoreConfigurations(@RequestParam(name="type", defaultValue = "all", required = false) String dataType){

        try {
            final DataCustomConfiguration all = getStoreConfigurations(dataType, null);
            return new ResponseEntity(all, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private DataCustomConfiguration getStoreConfigurations(String dataType, List<String> storeIds) {
        switch (dataType) {
            case "raster"   :
            case "vector"   :
            case "jdbc"     :
            case "service"  :
            case "data"     : break;
            case "all"      : dataType = null; break;
            default: throw new IllegalArgumentException("Unsupported data type filter:" + dataType);
        }

        final DataCustomConfiguration all = new DataCustomConfiguration();

        //list feature store factories
        for (DataStoreProvider p : DataProviders.listAcceptedProviders(true)) {
            // if there is a list of provider identifier specified
            final String identifier = p.getOpenParameters().getName().getCode();
            if (storeIds != null && !storeIds.contains(identifier)) {
                continue;
            }

            final ResourceType[] resourceTypes = DataStores.getResourceTypes(p);
            if (dataType != null) {
                switch (dataType) {
                    case "raster" :
                        if (!(contains(resourceTypes, COVERAGE) || contains(resourceTypes, GRID) || contains(resourceTypes, PYRAMID))) continue;
                        break;
                    case "vector" :
                        if (!contains(resourceTypes, VECTOR)) continue;
                        break;
                    case "jdbc"   :
                        // Workaround: avoid forcing dependency over sis-sql. Check it by name.
                        if ("sql".equalsIgnoreCase(identifier)) break;
                        else continue;
                    case "service":
                        if (!(p instanceof AbstractClientProvider)) continue;
                        break;
                    case "data"   :
                        if (!(contains(resourceTypes, COVERAGE) || contains(resourceTypes, GRID) || contains(resourceTypes, PYRAMID) || contains(resourceTypes, VECTOR))) continue;
                        break;
                }
                // exclude raster SQL factory in filtered search for now
                if ("coverage-sql".equals(identifier) || "pgraster".equals(identifier)) {
                    continue;
                }
            }

            // add a tag vector/raster on the pojo for ui purpose
            final String tag = contains(resourceTypes, VECTOR) ? "vector" : "raster";
            final DataCustomConfiguration.Type type = DataProviders.buildDatastoreConfiguration(p, "data-store", tag);
            if(all.getTypes().isEmpty()){
                //select the first type found
                type.setSelected(true);
            }
            all.getTypes().add(type);
        }

        return all;
    }

    /**
     * Proceed to create new provider for given type.
     *
     * @param selected given selected type
     * @return {@code ResponseEntity}
     */
    @RequestMapping(value="/internal/datas/store",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity putDataStoreConfiguration(@RequestBody final DataCustomConfiguration.Type selected,
            @RequestParam(name = "hidden", required = false, defaultValue = "false") boolean hidden,
            @RequestParam(name = "providerId", required = false) String providerId, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);

            // 1. Create provider
            final ProviderConfiguration provConfig = new ProviderConfiguration(selected.getCategory(), selected.getId());
            selected.cleanupEmptyProperty();
            selected.propertyToMap(provConfig.getParameters());
            if (providerId == null) {
                providerId = selected.getId() + UUID.randomUUID().toString();
            }
            final Integer prId = providerBusiness.create(providerId, provConfig);

            // 2. Create the data, hidden for now and not bounded to any dataset
            providerBusiness.createOrUpdateData(prId, null, false, hidden, userId);

            // 3. For each created data
            final List<DataBrief> briefs = providerBusiness.getDataBriefsFromProviderId(prId, null, true, hidden, false, false);
            for (DataBrief brief : briefs) {

                // 3.1 we init the metadata
                MetadataLightBrief meta = dataBusiness.initDataMetadata(brief.getId(), hidden);
                brief.getMetadatas().add(meta);

                // 3.2 if enable and for vector data, we generate feature catalog metadata
                boolean generateFeatCat = Application.getBooleanProperty(AppProperty.GENERATE_FEATURE_CATALOG, true);
                if  ("vector".equalsIgnoreCase(brief.getType()) && generateFeatCat) {
                    FeatureCatalogue fc = ISO19110Builder.createCatalogueForData(brief.getProviderId(), new QName(brief.getNamespace(), brief.getName()));
                    if (fc != null) {
                        Integer intProviderID = metadataBusiness.getDefaultInternalProviderID();
                        if (intProviderID != null) {
                            meta = metadataBusiness.updateMetadata(fc.getId(), fc, brief.getId(), null, null, userId, intProviderID, "DOC", null, hidden);
                            brief.getMetadatas().add(new MetadataLightBrief(meta.getId(), meta.getTitle(), meta.getProfile()));
                        } else {
                            LOGGER.warning("No metadata provider available");
                        }
                    }
                }
            }

            return new ResponseEntity(briefs,OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/internal/datas/provider/{providerId}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataListsForProviders(@PathVariable("providerId") final String providerId) {
        try {
            final Integer prId = providerBusiness.getIDFromIdentifier(providerId);
            final List<DataBrief> briefs = new ArrayList<>();
            final List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(prId, null, true, false);
            for (final Integer dataId : dataIds) {
                briefs.add(dataBusiness.getDataBrief(dataId, true, true));
            }
            return new ResponseEntity(briefs, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Used to open metadata editor form.
     * the metadata.prune() should never be called in this method.
     * Returns json result of template writer to apply a given template to metadata object.
     * The path of each fields/blocks will be numerated.
     * the returned json object will be used directly in html metadata editor.
     *
     * @param identifier given dataset identifier.
     * @return {@code ResponseEntity}
     */
    @RequestMapping(value="/internal/datasets/metadata/{identifier}",method=GET, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasetMetadata(final @PathVariable String identifier) {
        try {
            Integer dsId = datasetBusiness.getDatasetId(identifier);
            if (dsId != null) {
                final String buffer = metadataBusiness.getJsonDatasetMetadata(dsId, false, false);
                return new ResponseEntity(buffer, OK);
            }
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "error cannot get dataset Metadata.", ex);
            return new ErrorMessage(ex).build();
        }

    }


    @RequestMapping(value="/internal/datasets/metadata/{identifier}",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity mergeMetadataDS(@PathVariable("identifier") final String identifier, final @RequestBody RootObj metadataValues) {
        try {
            Integer dsId = datasetBusiness.getDatasetId(identifier);
            if (dsId != null) {
                metadataBusiness.mergeDatasetMetadata(dsId, metadataValues);
            }
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.warning("Error while saving dataset metadata");
            return new ErrorMessage(ex).build();
        }
    }

}
