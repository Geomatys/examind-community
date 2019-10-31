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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.CRC32;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.ImportedData;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.SelectedExtension;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.metadata.RootObj;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.metadata.utils.Utils;
import static org.constellation.metadata.utils.Utils.UNKNOW_IDENTIFIER;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ISO19110Builder;
import org.constellation.util.Util;
import org.geotoolkit.client.AbstractClientProvider;
import org.geotoolkit.coverage.xmlstore.XMLCoverageStore;
import org.geotoolkit.db.AbstractJDBCFeatureStoreFactory;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.apache.sis.storage.DataStore;
import static org.apache.sis.util.ArraysExt.contains;
import org.constellation.admin.util.DataCoverageUtilities;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.ResourceType;
import static org.geotoolkit.storage.ResourceType.*;
import org.opengis.feature.catalog.FeatureCatalogue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ImageCRS;
import org.opengis.util.GenericName;
import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class InternalDataRestAPI extends AbstractRestAPI {

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IMetadataBusiness metadataBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private IDatasetBusiness datasetBusiness;

    /**
     * Give subfolder list of data from a server file path
     *
     * @param path server file path
     * @param filtered {@code True} if we want to keep only known files.
     * @return a file list
     */
    @RequestMapping(value="/internal/datas/datapath/{filtered}",method=POST,consumes=APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity listDataFolderContent(@PathVariable("filtered") final Boolean filtered,
            @RequestBody final String path) {

        try {
            final List<FileBean> listBean = dataBusiness.getFilesFromPath(path, filtered, false);
            return new ResponseEntity(listBean,OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Give subfolder list of metadata xml from a server file path
     *
     * @param path server file path
     * @param filtered {@code True} if we want to keep only known files.
     * @return a {@link ResponseEntity} which contain file list
     */
    @RequestMapping(value="/internal/datas/metadatapath/{filtered}",method=POST,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity ListMetaDataFolderContent(@PathVariable("filtered") final Boolean filtered,
            @RequestBody final String path) {

        try {
            final List<FileBean> listBean = dataBusiness.getFilesFromPath(path, filtered, true);
            return new ResponseEntity(listBean,OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/internal/datas/testextension/{ext}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity testExtension(@PathVariable("ext") final String extension) {

        final Map<String, String> extensions = DataCoverageUtilities.getAvailableFileExtension();
        String type = extensions.get(extension.toLowerCase());
        if (type == null) {
            type = "";
        }
        return new ResponseEntity(new SelectedExtension(type, extension), OK);
    }

    @RequestMapping(value="/internal/datas/pyramid/folder/{id}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity deletePyramidFolder(@PathVariable("id") final int providerId) {

        final Map<String,Object> map = new HashMap<>();
        final DataStore ds;
        try {
            ds = DataProviders.getProvider(providerId).getMainStore();
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Unable to access provider "+ providerId, ex);
            return new ErrorMessage(ex).build();
        }
        // todo find another way to determine if its a pyramid provider
        if (!(ds instanceof XMLCoverageStore)) {
            map.put("isPyramid",false);
            return new ResponseEntity(map, OK);
        }
        map.put("isPyramid",true);
        final ResourceOnFileSystem xmlCoverageStore = (ResourceOnFileSystem) ds;
        try {
            for (Path p : xmlCoverageStore.getComponentFiles()) {
                IOUtilities.deleteRecursively(p);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Unable to delete folder for provider:" + providerId, ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(map,OK);
    }

    /**
     * Receive a {@link MultipartFile} which contain a file need to be save on server to create data on provider
     * @param data
     * @return A {@link ResponseEntity} with 200 code if upload work, 500 if not work.
     */
    @RequestMapping(value="/internal/datas/upload/data",method=POST, consumes=MULTIPART_FORM_DATA_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadData(@RequestParam("data") MultipartFile data) {

        try {
            final Path uploadDirectory = getUploadDirectory();
            final HashMap<String,String> hashMap = new HashMap<>();
            final String dataName = data.getOriginalFilename();
            final Path newFileData = uploadDirectory.resolve(dataName);
            if (!data.isEmpty()) {
                try(InputStream in = data.getInputStream()){
                    Files.copy(in, newFileData, StandardCopyOption.REPLACE_EXISTING);
                }
                hashMap.put("dataPath", newFileData.toUri().toString());
            }
            return new ResponseEntity(hashMap,OK);
        }catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Receive a {@link MultipartFile} which contain a file need to be save on server to create data on provider
     *
     * @param metadata
     * @param identifier
     * @param serverMetadataPath
     * @return A {@link ResponseEntity} with 200 code if upload work, 500 if not work.
     */
    @RequestMapping(value="/internal/datas/upload/metadata",method=POST, consumes=MULTIPART_FORM_DATA_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity uploadMetadata(
            @RequestParam(name = "metadata", required = false) MultipartFile metadata,
            @RequestParam(name = "identifier", required = false) String identifier,
            @RequestParam(name = "serverMetadataPath", required = false) String serverMetadataPath) {

        final Map<String,String> hashMap = new HashMap<>();
        if (identifier != null && ! identifier.isEmpty()){
            hashMap.put("dataName", identifier);
        } else {
            try {
                Path metadataFile = null;
                if (serverMetadataPath !=null && !serverMetadataPath.isEmpty()){

                    metadataFile = IOUtilities.toPath(serverMetadataPath);

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
                    Object obj = metadataBusiness.getMetadataFromFile(metadataFile);
                    if (obj == null) {
                        throw new ConstellationException("metadata file is incorrect");
                    }

                    final String metaIdentifier = Utils.findIdentifier(obj);
                    if (!UNKNOW_IDENTIFIER.equals(metaIdentifier)) {
                        hashMap.put("dataName", metaIdentifier);
                    }else {
                        throw new ConstellationException("metadata does not contains any identifier," +
                                " please check the fileIdentifier in your metadata.");
                    }
                    hashMap.put("metadataPath", metadataFile.toAbsolutePath().toUri().toString());
                    hashMap.put("metatitle", Utils.findTitle(obj));
                    hashMap.put("metaIdentifier", metaIdentifier);
                }
            } catch (ConstellationException | IOException ex) {
                return new ErrorMessage(ex).build();
            }
        }
        //verify uniqueness of data identifier
        final Integer prId = providerBusiness.getIDFromIdentifier(hashMap.get("dataName"));
        if (prId!=null){
            return new ErrorMessage().message("dataName or identifier of metadata is already used").build();
        }
        return new ResponseEntity(hashMap, OK);
    }

    /**
     * Init metadata for imported data.
     * It is the first save called after import phase.
     * if user send its own metadata he can decide if its
     * metadata will be merged with reader metadata by passing parameter flag mergeWithUploadedMD.
     *
     * @param providerId Provider identifier.
     * @param dataType Data type.
     * @param mergeWithUploadedMD Flag to indicate if we merge xith the uploaded metadata.
     *
     * @return {@link ResponseEntity}
     * @throws ConfigurationException
     */
    @RequestMapping(value = "/internal/datas/init/metadata",consumes=APPLICATION_JSON_VALUE, method = POST)
    public ResponseEntity initMetadataFromReader(@RequestParam("providerId")final String providerId,
                                                 @RequestParam("dataType")  final String dataType,
                                                 @RequestParam("mergeWithUploadedMD") final String mergeWithUploadedMD) throws ConfigurationException {
        Object uploadedMetadata;
        try{
            uploadedMetadata = datasetBusiness.getMetadata(providerId);
        }catch(Exception ex){
            uploadedMetadata = null;
        }
        if(uploadedMetadata!=null && (mergeWithUploadedMD == null || mergeWithUploadedMD.equalsIgnoreCase("false"))){
            //skip if there is uploaded metadata and user want to keep this original metadata.
            return new ResponseEntity(OK);
        }
        try {
            Integer dsId = datasetBusiness.getDatasetId(providerId);
            datasetBusiness.initDatasetMetadata(dsId, providerId, dataType, false);
        } catch (ConstellationException ex) {
            throw ex.toRuntimeException();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Import data from upload Directory to integrated directory
     * - change file location from upload to integrated
     * this method do all chain: init provider and metadata.
     *
     * @param values {@link org.constellation.dto.ParameterValues} containing file path &amp; data type
     * @return a {@link ResponseEntity}
     */
    @RequestMapping(value="/internal/datas/import/full",method=POST,consumes=APPLICATION_JSON_VALUE , produces=APPLICATION_JSON_VALUE)
    public ResponseEntity proceedToImport(@RequestBody final ParameterValues values) {

        final String filePathStr      = values.getValues().get("dataPath");
        final String metadataFilePath = values.getValues().get("metadataFilePath");
        final String dataType         = values.getValues().get("dataType");
        final String dataName         = values.getValues().get("dataName");
        final String fileExtension    = values.getValues().get("extension");
        final String fsServer         = values.getValues().get("fsServer");

        final ImportedData importedDataReport = new ImportedData();
        try {
            final Path dataIntegratedDirectory = ConfigDirectory.getDataIntegratedDirectory();
            final Path uploadFolder = getUploadDirectory();

            if (metadataFilePath != null) {
                Path metadataPath = IOUtilities.toPath(metadataFilePath);
                if (metadataPath.startsWith(uploadFolder.toAbsolutePath())) {
                    final Path destMd = dataIntegratedDirectory.toAbsolutePath().resolve(metadataPath.getFileName().toString());
                    Files.move(metadataPath, destMd, StandardCopyOption.REPLACE_EXISTING);
                    importedDataReport.setMetadataFile(destMd.toUri().toString());
                } else {
                    importedDataReport.setMetadataFile(metadataFilePath);
                }
            }

            // For server file mode, we let the data in its current location
            if (fsServer != null && fsServer.equalsIgnoreCase("true")) {

                Path filePath = IOUtilities.toPath(filePathStr);
                String ext = IOUtilities.extension(filePath);

                // if the server file is a zip we unzip it, in the integrated folder
                if ("zip".equals(ext.toLowerCase())) {

                     //init provider directory
                    final Path intDirPath  = dataIntegratedDirectory;
                    final Path providerDir = intDirPath.resolve(dataName);
                    final Path dataDir     = providerDir.resolve(dataName);
                    if (Files.exists(dataDir)) {
                        IOUtilities.deleteRecursively(dataDir);
                    }
                    Files.createDirectories(dataDir);

                    //unzip
                    ZipUtilities.unzip(filePath, dataDir, new CRC32());
                    filePath = dataDir.toAbsolutePath();
                }

                importedDataReport.setDataFile(filePath.toAbsolutePath().toUri().toString());


            // For upload mode, we move the data to the "integrated" folder of examind.
            } else if (filePathStr != null) {
                Path filePath = IOUtilities.toPath(filePathStr);
                filePath = Util.renameFile(dataName, filePath);

                //init provider directory
                final Path intDirPath  = dataIntegratedDirectory;
                final Path providerDir = intDirPath.resolve(dataName);
                final Path dataDir     = providerDir.resolve(dataName);
                if (Files.exists(dataDir)) {
                    IOUtilities.deleteRecursively(dataDir);
                }
                Files.createDirectories(dataDir);

                //unzip
                String ext = IOUtilities.extension(filePath);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(filePath, dataDir, new CRC32());
                    filePath = dataDir.toAbsolutePath();
                }

                //move to integrated
                if (filePath.startsWith(uploadFolder.toAbsolutePath())) {
                    final Path destFile = dataDir.toAbsolutePath().resolve(filePath.getFileName().toString());
                    Files.move(filePath, destFile, StandardCopyOption.REPLACE_EXISTING);
                    importedDataReport.setDataFile(destFile.toAbsolutePath().toUri().toString());
                } else {
                    importedDataReport.setDataFile(filePath.toAbsolutePath().toUri().toString());
                }
            }

            String dataFile = importedDataReport.getDataFile();
            final String metadataPath = importedDataReport.getMetadataFile();
            final String uploadType = RestApiUtil.findDataType(dataFile,fileExtension,dataType);
            importedDataReport.setDataType(uploadType);
            final String providerIdentifier = dataName;

            Integer datasetId;

            if ("vector".equalsIgnoreCase(uploadType)) {

                String[] extracted = DataProviders.findFeatureFactoryForFiles(dataFile, FACTORY_COMPARATOR);
                dataFile             = extracted[0];
                final String subType = extracted[1];

                //create provider
                final ProviderConfiguration config = new ProviderConfiguration("data-store", subType, dataFile);
                final Integer prId = providerBusiness.create(providerIdentifier, config);
                datasetId = providerBusiness.createOrUpdateData(prId, null, true);

                //verify CRS
                if (verifyCRS(prId)) {
                    importedDataReport.setVerifyCRS("success");
                } else {
                    importedDataReport.setVerifyCRS("error");
                    //get a list of EPSG codes
                    importedDataReport.setCodes(DataProviders.getAllEpsgCodes());
                }

            } else if("raster".equalsIgnoreCase(uploadType)) {
                //create provider
                final ProviderConfiguration config = new ProviderConfiguration("data-store", "coverage-file", dataFile);
                final Integer prId = providerBusiness.create(providerIdentifier, config);
                datasetId = providerBusiness.createOrUpdateData(prId, null, true);

                //verify CRS
                if (verifyCRS(prId)) {
                    importedDataReport.setVerifyCRS("success");
                } else {
                    importedDataReport.setVerifyCRS("error");
                    //get a list of EPSG codes
                    importedDataReport.setCodes(DataProviders.getAllEpsgCodes());
                }

                /**
                 * For each data created in provider, we need to pyramid conform each raster.
                 */
                providerBusiness.createAllPyramidConformForProvider(prId);

            } else if ("observation".equalsIgnoreCase(uploadType)) {
                String subType = "observationFile";
                if ("xml".equalsIgnoreCase(fileExtension)) {
                    subType = "observationXmlFile";
                }
                //create provider
                final ProviderConfiguration config = new ProviderConfiguration("data-store", subType, dataFile);
                final Integer prId = providerBusiness.create(providerIdentifier, config);
                datasetId = providerBusiness.createOrUpdateData(prId, null, true);

            } else {
                //not supported
                throw new UnsupportedOperationException("The uploaded file is not recognized or not supported by the application. file:"+uploadType);
            }

            //set up user metadata
            if (metadataPath != null && !metadataPath.isEmpty()) {
                try {
                    Path f = IOUtilities.toPath(metadataPath);
                    Object metadata = metadataBusiness.getMetadataFromFile(f);
                    if (metadata == null) {
                        throw new ConstellationException("Cannot save uploaded metadata because it is not recognized as a valid file.");
                    }
                    datasetBusiness.updateMetadata(datasetId, metadata, false);
                } catch (ConfigurationException | IOException ex) {
                    throw new ConstellationException("Error while saving dataset metadata, " + ex.getMessage());
                }
            }

            return new ResponseEntity(importedDataReport,OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private boolean verifyCRS(int providerId) {
        try {
            final Map<GenericName, CoordinateReferenceSystem> nameCoordinateReferenceSystemHashMap = DataProviders.getCRS(providerId);
            for(final CoordinateReferenceSystem crs : nameCoordinateReferenceSystemHashMap.values()){
                if (crs == null || crs instanceof ImageCRS) {
                    throw new DataStoreException("CRS is null or is instance of ImageCRS");
                }
            }
            return true;
        } catch (ConfigurationException | DataStoreException e) {
            LOGGER.log(Level.INFO, "Cannot get CRS for provider " + providerId);
        }
        return false;
    }

    @RequestMapping(value="/internal/datas/store/{storeId}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataStoreConfiguration(@PathVariable(name="storeId") String storeId){
        DataStoreProvider factory = DataStores.getProviderById(storeId);
        if (factory != null) {
            final DataCustomConfiguration.Type type = DataProviders.buildDatastoreConfiguration(factory, "data-store", null);
            type.setSelected(true);
            return new ResponseEntity(type, OK);
        }
        return new ResponseEntity(NOT_FOUND);
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
        for (DataStoreProvider p : org.apache.sis.storage.DataStores.providers()) {
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
                        if (!(p instanceof AbstractJDBCFeatureStoreFactory)) continue;
                        break;
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
            final List<DataBrief> briefs = providerBusiness.getDataBriefsFromProviderId(prId, null, true, hidden);
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

    /**
     * Create a dataset with an optional metadata file.
     *
     * @param values
     * @param req
     * @return
     */
    @RequestMapping(value="/internal/datasets",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity createEmptyDataset(@RequestBody final ParameterValues values, HttpServletRequest req) {

        final String metaPath = values.getValues().get("metadataFilePath");

        final String datasetIdentifier = values.getValues().get("datasetIdentifier");
        if (datasetIdentifier != null && !datasetIdentifier.isEmpty()) {
            try {
                if (datasetBusiness.existsByName(datasetIdentifier)) {
                    LOGGER.log(Level.WARNING, "Dataset with identifier " + datasetIdentifier + " already exist");
                    return new ResponseEntity("failed", HttpStatus.CONFLICT);
                }

                Object metadata = null;
                if (metaPath != null) {
                    final java.nio.file.Path f = IOUtilities.toPath(metaPath);
                    if (metadataBusiness.isSpecialMetadataFormat(f)){
                        metadata = metadataBusiness.getMetadataFromSpecialFormat(f);
                    } else {
                        metadata = (DefaultMetadata) metadataBusiness.unmarshallMetadata(f);
                    }
                }

                Integer userId = assertAuthentificated(req);
                Integer dsId = datasetBusiness.createDataset(datasetIdentifier, userId, null);
                datasetBusiness.updateMetadata(dsId, metadata, false);

                return new ResponseEntity(dsId, HttpStatus.CREATED);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to create dataset with identifier " + datasetIdentifier, ex);
                return new ResponseEntity("failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            LOGGER.log(Level.WARNING, "Can't create dataset with empty identifier");
            return new ResponseEntity("failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value="/internal/datas/provider/{providerId}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getDataListsForProviders(@PathVariable("providerId") final String providerId) {
        try {
            final Integer prId = providerBusiness.getIDFromIdentifier(providerId);
            final List<DataBrief> briefs = new ArrayList<>();
            final List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(prId, null, true, false);
            for (final Integer dataId : dataIds) {
                briefs.add(dataBusiness.getDataBrief(dataId));
            }
            return new ResponseEntity(briefs, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/internal/datas/saveUploadedMetadata",method=POST,consumes=APPLICATION_JSON_VALUE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity saveUploadedMetadata(final @RequestBody ParameterValues values) {
        final String providerId = values.getValues().get("providerId");
        final String mdPath = values.getValues().get("mdPath");
        try {
            if (mdPath != null && !mdPath.isEmpty()) {
                try {
                    java.nio.file.Path f = IOUtilities.toPath(mdPath);
                    final Object metadata;
                    if (metadataBusiness.isSpecialMetadataFormat(f)) {
                        metadata = metadataBusiness.getMetadataFromSpecialFormat(f);
                    } else {
                        metadata = metadataBusiness.unmarshallMetadata(f);
                    }
                    if (metadata == null) {
                        throw new ConstellationException("Cannot save uploaded metadata because it is not recognized as a valid file!");
                    }
                    // for now we assume datasetID == providerID
                    datasetBusiness.updateMetadata(providerId, metadata);
                } catch (ConfigurationException | IOException ex) {
                    throw new ConstellationException("Error while saving dataset metadata, " + ex.getMessage());
                }
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
