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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.inject.Inject;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStores;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.SimpleValue;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.memory.ExtendedFeatureStore;
import org.geotoolkit.io.wkt.PrjFiles;
import org.apache.sis.storage.DataStore;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.constellation.dto.ProviderBrief;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.FeatureData;
import org.geotoolkit.util.NamesExt;

/**
 * RestFull API for provider management/operations.
 *
 * @author Fabien Bernard (Geomatys)
 * @author Johann Sorel (Geomatys)
 * @version 0.9
 * @since 0.9
 */
@RestController
@RequestMapping("/providers")
public class ProviderRestAPI extends AbstractRestAPI {

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private IDatasetBusiness datasetBusiness;

    /**
     * List all providers.
     *
     * @return list of providers
     */
    @RequestMapping(method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getProviders() {
        final List<ProviderBrief> providers = providerBusiness.getProviders();
        return new ResponseEntity(providers,OK);
    }

    /**
     * List all providers services.
     *
     * @return list of providers services
     */
    @RequestMapping(value="/services",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getServices() {
        final List<String> lst = new ArrayList<>();
        for (DataStoreProvider dp : DataStores.providers()) {
            lst.add(dp.getClass().getName() + " ("+dp.getShortName()+")");
        }
        return new ResponseEntity(lst,OK);
    }

    /**
     * Reload a provider.
     *
     * @param providerId the provider ID as integer
     * @return
     * @deprecated duplicate with CRSRest API
     */
    @RequestMapping(value="/{id}/reload",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity reloadProvider(@PathVariable("id") final int providerId) {
        try {
            providerBusiness.reload(providerId);
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/{id}/test",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity test(
            @PathVariable("id") final String providerIdentifier,
            @RequestBody final ProviderConfiguration configuration) {

        try {
            final Set<GenericName> names = providerBusiness.test(providerIdentifier, configuration);
            if (names.isEmpty()){
                LOGGER.log(Level.WARNING, "non data found for provider: {0}", providerIdentifier);
                return new ErrorMessage().message("Unable to find any data, please check the database parameters and make sure that the database is properly configured.").build();
            }
        } catch (ConfigurationException e) {
            LOGGER.log(Level.WARNING, "Cannot open provider "+providerIdentifier, e);
            return new ErrorMessage(e).build();
        }
        return new ResponseEntity(OK);
    }

    @RequestMapping(value="/{id}",method=PUT,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity update(
            @PathVariable("id") final String id,
            @RequestBody        final ProviderConfiguration config) {

        try {
            providerBusiness.update(id, config);
        }catch(ConfigurationException ex){
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Create a new provider from the given configuration.
     *
     * @param id
     * @param createdata
     * @param config
     * @return
     */
    @RequestMapping(value="/{id}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity create(
            @PathVariable("id") final String id,
            @RequestParam("createdata") boolean createdata,
            @RequestBody final ProviderConfiguration config) {

        try {
            Integer prId = providerBusiness.create(id, config);
            if (createdata) {
                providerBusiness.createOrUpdateData(prId, null, true);
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }

        return new ResponseEntity(OK);
    }

    /**
     *
     * @param providerIdentifier
     * @return
     * @deprecated duplicate with CRSRest API
     */
    @RequestMapping(value="/{id}/epsgCode",method=GET,produces=APPLICATION_JSON_VALUE)
    @Deprecated
    public ResponseEntity getAllEpsgCode(
            @PathVariable("id") final String providerIdentifier) {

        try {
            final CRSAuthorityFactory factory = org.apache.sis.referencing.CRS.getAuthorityFactory("EPSG");
            final Set<String> authorityCodes = factory.getAuthorityCodes(CoordinateReferenceSystem.class);
            final List<String> codes = new ArrayList<>();
            for (String code : authorityCodes){
                code += " - " + factory.getDescriptionText(code).toString();
                codes.add(code);
            }
            return new ResponseEntity(codes,OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }


    @RequestMapping(value="/{id}/createprj",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity createPrj(
            @PathVariable("id")       final String providerIdentifier,
            @RequestBody              final Map<String,String> epsgCode) {

        final DataProvider provider;
        try {
            provider = DataProviders.getProvider(providerIdentifier);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            return new ErrorMessage(ex).build();
        }

        try {
            final DataStore datastore = provider.getMainStore();
            if (datastore instanceof ResourceOnFileSystem){
                proceedToCreatePrj(provider,(ResourceOnFileSystem)datastore,epsgCode);
                return new ResponseEntity(OK);
            }else if(datastore instanceof ExtendedFeatureStore) {
                final ExtendedFeatureStore efs = (ExtendedFeatureStore) datastore;
                final FeatureStore fstore = efs.getWrapped();
                if (fstore instanceof ResourceOnFileSystem) {
                    proceedToCreatePrj(provider,(ResourceOnFileSystem)fstore,epsgCode);
                    return new ResponseEntity(OK);
                }
            }
            return new ErrorMessage().message("Cannot creates the prj file for the data. the operation is not implemented yet for this format.").build();

        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private void proceedToCreatePrj(final DataProvider provider,
                                    final ResourceOnFileSystem dataFileStore,
                                    final Map<String,String> epsgCode) throws DataStoreException,FactoryException,IOException {
        java.nio.file.Path[] dataFiles = dataFileStore.getComponentFiles();
        if(dataFiles == null) return;
        if (dataFiles.length == 1 && Files.isDirectory(dataFiles[0])) {
            List<java.nio.file.Path> dirPaths = new ArrayList<>();
            try (DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(dataFiles[0])) {
                for (java.nio.file.Path candidate : stream) {
                    dirPaths.add(candidate);
                }
            }
            dataFiles = dirPaths.toArray(new java.nio.file.Path[dirPaths.size()]);
        }
        if(dataFiles.length==0) return;
        final String firstFileName = dataFiles[0].getFileName().toString();
        final String fileNameWithoutExtention;
        if(firstFileName.indexOf('.')!=-1){
            fileNameWithoutExtention = firstFileName.substring(0, firstFileName.indexOf('.'));
        }else {
            fileNameWithoutExtention = firstFileName;
        }
        final java.nio.file.Path parentPath = dataFiles[0].getParent();
        final CoordinateReferenceSystem coordinateReferenceSystem = CRS.forCode(epsgCode.get("codeEpsg"));
        PrjFiles.write(coordinateReferenceSystem, parentPath.resolve(fileNameWithoutExtention+".prj"));
        provider.reload();
    }

    /**
     * Delete a provider with the given id.
     *
     * @param id provider identifier.
     * @return
     */
    @RequestMapping(value="/{id}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity delete(@PathVariable("id") final Integer id) {

        try {
            providerBusiness.removeProvider(id);
            return new ResponseEntity(OK);
        } catch(Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * @param dataName
     * @param id
     * @param property
     * @return
     */
    @RequestMapping(value="/{id}/{dataName}/{property}/propertyValues",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity propertyValues(
            @PathVariable("id")         final String id,
            @PathVariable("dataName")   final String dataName,
            @PathVariable("property")   final String property) {

        try {
            final Data data = getProviderData(id, dataName);
            if (data instanceof FeatureData) {
                return new ResponseEntity(((FeatureData)data).getPropertyValues(property),OK);
            } else {
                throw new ConstellationStoreException("No data " + dataName + " found in provider:" + id + " (or is not a faure data)");
            }

        } catch (ConfigurationException | ConstellationStoreException  ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve information for data " + dataName, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Indicate if given provider contains a geophysic data.
     *
     * @param id
     * @param dataName
     * @return
     */
    @RequestMapping(value="/{id}/{dataName}/isGeophysic",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity isGeophysic(
            @PathVariable("id")        final String id,
            @PathVariable("dataName") final String dataName) {

        boolean isGeophysic = false;
        try {
            final Data data = getProviderData(id, dataName);

            if(data!=null && data.getOrigin() instanceof CoverageResource){
                final CoverageResource ref = (CoverageResource) data.getOrigin();
                final GridCoverageReader reader = (GridCoverageReader) ref.acquireReader();
                final List<GridSampleDimension> dims = reader.getSampleDimensions(ref.getImageIndex());
                if(dims!=null && !dims.isEmpty()){
                    isGeophysic = true;
                }
                ref.recycle(reader);
            }
        } catch (CoverageStoreException | ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve information for dataName " + dataName, ex);
            return new ErrorMessage(ex).build();
        }

       return new ResponseEntity(new SimpleValue(isGeophysic),OK);
    }

    /**
     * List the available pyramids for this layer
     *
     * @param id
     * @param dataName
     * @return
     */
    @RequestMapping(value="/{id}/{dataName}/listPyramidChoice",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity listPyramids(
            @PathVariable("id")        final String id,
            @PathVariable("dataName") final String dataName) {

        try {
            return new ResponseEntity(providerBusiness.listPyramids(id, dataName),OK);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve information for data "+dataName, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * Is this method still used ??
     *
     * No longer metadata for provider but for dataset
     *
     * @param providerId
     * @return
     */
    @RequestMapping(value="/metadata/{providerId}",method=GET,produces=APPLICATION_XML_VALUE)
    public ResponseEntity getMetadata(
            @PathVariable("providerId") final String providerId) {

        // for now assume that providerID == datasetID
        try {
            return new ResponseEntity(datasetBusiness.getMetadata(providerId),OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, "Cannot retrieve metadata for provider "+providerId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     *
     * Is this method still used ??
     *
     * No longer metadata for provider but for dataset
     *
     * @param providerId
     * @param metadata
     * @return
     */
    @RequestMapping(value="/metadata/{providerId}",method=POST,consumes=APPLICATION_XML_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity setMetadata(
            @PathVariable("providerId") final String providerId,
            @RequestBody                final DefaultMetadata metadata) {

        // for now assume that providerID == datasetID
        try {
            datasetBusiness.updateMetadata(providerId, metadata);
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, "Cannot update metadata for provider "+providerId, ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * List the data names in a provider
     *
     * @param providerId Identifier of the provider.
     *
     * @return a Map of index / data name
     */
    @RequestMapping(value="/{id}/datas/name",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataNamesList(@PathVariable("id") int providerId) {

        final DataProvider provider;
        try {
            provider = DataProviders.getProvider(providerId);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING,"Error while accessing provider "+ providerId,ex);
            return new ErrorMessage(ex).build();
        }
        final Set<GenericName> nameSet = provider.getKeys();
        final List<String> names = new ArrayList<>();
        for (GenericName n : nameSet) {
            names.add(n.tip().toString());
        }
        Collections.sort(names);

        //Search on Metadata to found description
        final Map<String, String> dataDescriptions = new HashMap<>(0);
        for (int i = 0; i < names.size(); i++) {
            dataDescriptions.put(String.valueOf(i), names.get(i));
        }

        //Send String Map via REST
        return new ResponseEntity(new ParameterValues(dataDescriptions), OK);
    }

    /**
     * List the datas in a provider
     *
     * @param providerId Identifier of the provider.
     *
     * @return A list of {@code DataBrief}.
     */
    @RequestMapping(value="/{id}/datas",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataListsForProvider(@PathVariable("id") final int providerId) {
        final List<DataBrief> briefs;
        try {
            briefs = providerBusiness.getDataBriefsFromProviderId(providerId, null, true, false);
        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(briefs, OK);
    }

    private Data getProviderData(final String providerId, final String dataName) throws ConfigurationException {
        Integer prId = providerBusiness.getIDFromIdentifier(providerId);
        final DataProvider provider = DataProviders.getProvider(prId);
        final GenericName name = NamesExt.valueOf(dataName);
        final Data layer = provider.get(name);
        if (layer == null) {
            throw new ConfigurationException("No data named \"" + dataName + "\" in provider with id \"" + provider.getId() + "\".");
        }
        return layer;
    }
}
