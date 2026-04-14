/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2025 Geomatys.
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
package com.examind.setup;

import jakarta.annotation.PostConstruct;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.ObjectConverters;
import org.constellation.api.ProviderType;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IFileSystemSetupBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.ISensorServiceBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataSource;
import com.examind.dto.fs.Collection;
import com.examind.dto.fs.CollectionItem;
import com.examind.dto.fs.Datasource;
import com.examind.dto.fs.DimensionItem;
import com.examind.dto.fs.Provider;
import com.examind.dto.fs.Service;
import static com.examind.setup.FileSystemUtilities.*;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.constellation.dto.Data;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wps.Processes;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.constellation.repository.DataRepository;
import org.geotoolkit.style.MutableStyle;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.context.annotation.Profile;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@Profile("fsconfig")
public class FileSystemSetupBusiness implements IFileSystemSetupBusiness {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    @Autowired
    private IServiceBusiness serviceBusiness;
    
    @Autowired
    private IConfigurationBusiness configBusiness;
    
    @Autowired
    private IProviderBusiness providerBusiness;
    
    @Autowired
    private IDatasetBusiness datasetBusiness;
    
    @Autowired
    private IDataBusiness dataBusiness;
    
    @Autowired
    private DataRepository dataRepository;
    
    @Autowired
    private IDatasourceBusiness datasourceBusiness;
    
    @Autowired
    private ILayerBusiness layerBusiness;
    
    @Autowired
    private IStyleBusiness styleBusiness;
    
    @Autowired
    private ISensorBusiness sensorBusiness;
    
    @Autowired
    private IMetadataBusiness metadataBusiness;
    
    @Autowired
    private ISensorServiceBusiness sensorServiceBusiness;
    
    private static final List<String> CSW_SERVICE_CONFIGURATION_PARAMETERS = List.of("collection", "onlyPublished", "partial", "es-url");
    
    @PostConstruct
    public void initFsConfiguration() {
        installDatas();
    }

    @Override
    public void installDatas() {
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    --        STARTING FILESYSTEM CONFIG INSTALLATION        --
                    -----------------------------------------------------------
                    """);
        try {
            
            // 1. install styles
            Path styleDir = configBusiness.getStylesDirectory();
            try (Stream<Path> stream = Files.walk(styleDir)) {
                 stream.filter(FileSystemUtilities::sldFileFilter).forEach(path -> {
                    createStyleFromFile(path);
                });
            }
            
            // 2. install regular data
            Path dataDir = configBusiness.getProvidersDirectory();
            try (Stream<Path> stream = Files.walk(dataDir)) {
                stream.filter(p -> providerFileFilter(p, false)).forEach(path -> {
                    createProviderFromFile(path);
                });
            }
            
            // 3. install services potentially creating data
            Path servDir = configBusiness.getServicesDirectory();
            try (Stream<Path> stream = Files.walk(servDir)) {
                 stream.filter(p -> serviceFileFilter(p, true)).forEach(path -> {
                    createServiceFromFile(path);
                });
            }
            
            // 4. install computed data that use data created in the previous pass
            try (Stream<Path> stream = Files.walk(dataDir)) {
                stream.filter(p -> providerFileFilter(p, true)).forEach(path -> {
                    createProviderFromFile(path);
                });
            }
            
            // 5. install regular services
            try (Stream<Path> stream = Files.walk(servDir)) {
                 stream.filter(p -> serviceFileFilter(p, false)).forEach(path -> {
                    createServiceFromFile(path);
                });
            }
            
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error a filesystem configuration startup", ex);
        }
    }
    
    private void createServiceFromFile(Path path) {
        try {
            Service instance = FS_MAPPER.readValue(path.toFile(), Service.class);
            if (serviceBusiness.getServiceIdentifiers(instance.getType()).contains(instance.getIdentifier())) {
                throw new ConfigurationException("Service identifier: " + instance.getIdentifier() + "(" +  instance.getType() + ") already used");
            }
            
            Details metadata = instance.getMetadata();
            metadata.setIdentifier(instance.getIdentifier());
            int sid = serviceBusiness.create(instance.getType(), instance.getIdentifier(), null, metadata, null);
            
            // special case
            if ("STS".equalsIgnoreCase(instance.getType())) {
                boolean directProvider = Boolean.parseBoolean(instance.getAdvancedParameters().getOrDefault("direct-provider", "false"));
                if (directProvider) {
                    AbstractConfigurationObject conf = serviceBusiness.getConfiguration(sid);
                    conf.setProperty("directProvider", "true");
                    serviceBusiness.setConfiguration(sid, conf);
                }
                
                Integer datasourceId = createDatasource(instance.getSource());
                
                int pid = createOM2DatabaseProvider(instance.getIdentifier(), instance.getAdvancedParameters(), datasourceId);
                serviceBusiness.linkServiceAndSensorProvider(sid, pid, true);
                
                boolean fullLink;
                int spid;
                if (directProvider) {
                    spid = createSensorDatabaseProvider(instance.getIdentifier(), instance.getAdvancedParameters(), datasourceId);
                    fullLink = true;
                } else {
                    String sensorFolder = instance.getAdvancedParameters().getOrDefault("sensor-metadata-path", null);
                    if (sensorFolder == null) {
                        spid = sensorBusiness.getDefaultInternalProviderID();
                        fullLink = false;
                    } else {
                        spid = createSensorFSProvider(instance.getIdentifier(), sensorFolder);
                        providerBusiness.createOrUpdateData(spid, null, false, false, null);
                        fullLink = true;
                    }
                }
                serviceBusiness.linkServiceAndSensorProvider(sid, spid, fullLink);
                
                
                boolean generateSensor = Boolean.parseBoolean(instance.getAdvancedParameters().getOrDefault("generate-from-existing", "false"));
                if (generateSensor && !directProvider) {
                    sensorServiceBusiness.generateSensorFromOMProvider(sid);
                }
                
                boolean generateData = Boolean.parseBoolean(instance.getAdvancedParameters().getOrDefault("create-data", "false"));
                if (generateData) {
                    providerBusiness.createOrUpdateData(pid, null, true, false, null);
                }

            } else if ("CSW".equalsIgnoreCase(instance.getType())) {
                boolean partial = false;
                int spid = createMetadataDatabaseProvider(instance.getIdentifier(), instance.getAdvancedParameters());
                if (!instance.getAdvancedParameters().isEmpty()) {
                    Automatic conf = (Automatic) serviceBusiness.getConfiguration(sid);
                    for (Entry<String, String> entry : instance.getAdvancedParameters().entrySet()) {
                        if (CSW_SERVICE_CONFIGURATION_PARAMETERS.contains(entry.getKey())) {
                            conf.setProperty(entry.getKey(), entry.getValue());
                            if (entry.getKey().equals("partial")) {
                                partial = Boolean.parseBoolean(entry.getValue());
                            }
                        }
                    }
                    String indexType = instance.getAdvancedParameters().get("indexType");
                    if (indexType != null) conf.setIndexType(indexType);
                    String profile = instance.getAdvancedParameters().get("profile");
                    if (profile != null) conf.setProfile(profile);

                    // force partial for filesystem CSW
                    if (instance.getAdvancedParameters().containsKey("dataDirectory")) {
                        conf.setProperty("partial", "true");
                        partial = false;
                    }
                    serviceBusiness.setConfiguration(sid, conf);
                }
                serviceBusiness.linkCSWAndProvider(sid, spid, !partial);
            } else if ("WPS".equalsIgnoreCase(instance.getType())) {
                if (!instance.getProcessFactories().isEmpty()) {
                    ProcessContext conf = (ProcessContext) serviceBusiness.getConfiguration(sid);
                    List<ProcessFactory> factories = new ArrayList<>();
                    for (com.examind.dto.fs.ProcessFactory factory : instance.getProcessFactories()) {
                        ProcessFactory processFactory;
                        if (factory.getProcess().isEmpty()) {
                            processFactory = new ProcessFactory(factory.getAuthority(), Boolean.TRUE);
                        } else {
                            processFactory = new ProcessFactory(factory.getAuthority(), Boolean.FALSE);
                            for (String pr : factory.getProcess()) {
                                processFactory.getInclude().add(new org.constellation.dto.service.config.wps.Process(pr));
                            }
                        }
                        factories.add(processFactory);
                    }
                    conf.setProcesses(new Processes(false, factories));
                    serviceBusiness.setConfiguration(sid, conf);
                }
            
            } else {
                AbstractConfigurationObject conf = serviceBusiness.getConfiguration(sid);
                for (Entry<String, String> entry : instance.getAdvancedParameters().entrySet()) {
                    conf.setProperty(entry.getKey(), entry.getValue());
                }
                serviceBusiness.setConfiguration(sid, conf);
            }

            serviceBusiness.start(sid);

            for (Collection col : instance.getCollections()) {
                if (col.getDataSet() != null) {
                    
                    Integer styleId = (col.getDatasetStyle() != null) ? styleBusiness.getStyleId("sld", col.getDatasetStyle()) : null;
                    List<Data> datas = getDataFromCollection(col);
                    
                    for (Data data : datas) {
                        
                        if (!isAllowedDataTypeForService(instance.getType(), data.getType(), data.getSubtype())) {
                            LOGGER.log(Level.FINER, "Data type: {0} not allowed for service: {1}", new Object[]{data.getType(), instance.getType()});
                            continue;
                        }

                        //create future new layer
                        QName layerQName     = new QName(data.getName(), data.getNamespace());
                        LayerConfig newLayer = new LayerConfig(layerQName);

                        CollectionItem custom = col.getItemByName(data.getName(), data.getNamespace());
                        String alias = null;
                        String title ;
                        if (custom != null) {
                            alias = custom.getAlias();
                            title = custom.getTitle();
                            if (custom.getStyle() != null) {
                                styleId = styleBusiness.getStyleId("sld", custom.getStyle());
                            }
                            for (DimensionItem di : custom.getDimensions()) {
                                newLayer.addDimension(new DimensionDefinition(di));
                            }
                        } else {
                            title = data.getName();
                        }
                        int layerId = layerBusiness.add(data.getId(), alias, data.getNamespace(), data.getName(), title, sid, newLayer);
                        if (styleId != null) {
                            styleBusiness.linkToLayer(styleId, layerId);
                        }
                    }
                } else {
                    LOGGER.warning("No dataset specified in collection");
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while importing service file: " + path.getFileName().toString(), ex);
        }
    }
    
    private List<Data> getDataFromCollection(Collection col) throws ConstellationException {
        Integer dsId  = col.getDataSet() != null ? datasetBusiness.getDatasetId(col.getDataSet()) : null;
        
        if (col.getDataSet() != null && dsId == null) {
            LOGGER.log(Level.WARNING, "Unable to find a dataset: {0}", new Object[]{col.getDataSet()});
            return List.of();
        }
                    
        List<Data> datas = new ArrayList<>();
        if (col.isIncludeAll()) {
            if (dsId == null) {
                LOGGER.log(Level.WARNING, "Include All collection require a dataset declaration");
                return List.of();
            }
            datas.addAll(dataRepository.findByDatasetId(dsId, true, false));
        } else {
            for (CollectionItem it : col.getData()) {
                Map filter = new HashMap();
                filter.put("name", it.getName());
                if (dsId              != null) filter.put("dataset",     dsId);
                if (it.getNamespace() != null) filter.put("namespace",   it.getNamespace());
                if (it.getProvider()  != null) filter.put("provider_id", it.getProvider());

                Entry<Integer, List<Data>> candidates = dataRepository.filterAndGet(filter, null, 1, 2);
                if (candidates.getKey() == 0) {
                    LOGGER.log(Level.WARNING, "No data found for:\ndataset: {0}\nname: {1}\nnamespace:{2}", new Object[]{col.getDataSet(), it.getName(), it.getNamespace()});
                } else if (candidates.getKey() > 1) {
                    StringBuilder errorMsg = new StringBuilder("Multiple data found for input:\ndataset: ").append(col.getDataSet())
                                                       .append("\nname: ").append(it.getName())
                                                       .append("\nnamespace: ").append(it.getNamespace())
                                                       .append("\nAvailable candidates:");
                    for (Data db : candidates.getValue()) {
                        errorMsg.append("\n - name: ").append(db.getName());
                        if (db.getNamespace() != null) errorMsg.append(" namespace: ").append(db.getNamespace());
                        errorMsg.append("provider_id: ").append(db.getProviderId());
                    }
                    LOGGER.warning(errorMsg.toString());
                } else {
                    datas.add(candidates.getValue().get(0));
                }
            }
        }
        return datas;
    }
    
    private Integer createMetadataDatabaseProvider(String serviceId, Map<String, String> parameters) throws ConstellationException {
        if (parameters.isEmpty()) return metadataBusiness.getDefaultInternalProviderID();
        String dataDirectory = parameters.get("dataDirectory");
        if (dataDirectory != null) {
            final String providerIdentifier = "csw-" + serviceId + "-" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("metadata-store");
            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
            sourcef.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
            final ParameterValueGroup config = choice.addGroup("FilesystemMetadata");
            config.parameter("folder").setValue(dataDirectory);
            config.parameter("store-id").setValue(providerIdentifier);

            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "metadata-store", sourcef);
            providerBusiness.createOrUpdateData(pid, null, false, false, null);
            return pid;
        }
        return metadataBusiness.getDefaultInternalProviderID();
    }
    
    private final List<String> skippedForOMProvider = List.of("om-implementation", "sn-implementation", "direct-provider", "create-data");
    
    private Integer createOM2DatabaseProvider(String serviceId, Map<String, String> parameters, Integer datasourceId) {
        try {
            final String providerIdentifier = "om-src-" + serviceId;
            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);
            
            String impl = parameters.getOrDefault("om-implementation", "observationSOSDatabase");
            final ParameterValueGroup config = choice.addGroup(impl);
            
            if (datasourceId != null) {
                config.parameter("datasource-id").setValue(datasourceId);
            }
            for (Entry<String, String> param : parameters.entrySet()) {
                // skip some reserved or know parameter
                String key = param.getKey();
                if (skippedForOMProvider.contains(key)) continue;
                try {
                    ParameterValue<?> paramValue = config.parameter(param.getKey());
                    paramValue.setValue(ObjectConverters.convert(param.getValue(), paramValue.getDescriptor().getValueClass()));
                } catch (ParameterNotFoundException ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }
            
            // fixed for now TODO remove ? 
            if (impl.equals("observationSOSDatabase")) {
                config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            }
            
            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    
    private Integer createSensorDatabaseProvider(String serviceId, Map<String, String> parameters, Integer datasourceId) {
        try {
            final String providerIdentifier = "sensorSrc-" + serviceId;
            final DataProviderFactory omFactory = DataProviders.getFactory("sensor-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);
            
            String impl = parameters.getOrDefault("sn-implementation", "om2sensor");
            final ParameterValueGroup config = choice.addGroup(impl);
            
            if (datasourceId != null) {
                config.parameter("datasource-id").setValue(datasourceId);
            }
            
            for (Entry<String, String> param : parameters.entrySet()) {
                // skip some reserved or know parameter
                String key = param.getKey();
                if (skippedForOMProvider.contains(key)) continue;
                try {
                    ParameterValue<?> paramValue = config.parameter(param.getKey());
                    paramValue.setValue(ObjectConverters.convert(param.getValue(), paramValue.getDescriptor().getValueClass()));
                } catch (ParameterNotFoundException ex) {
                    LOGGER.warning(ex.getMessage());
                }
            }
            
            // fixed for now TODO remove ? 
            if (impl.equals("om2sensor")) {
                config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            }
            
            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "sensor-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    private Integer createSensorFSProvider(String serviceId, String path) {
        try {
            final String providerIdentifier = "sensorSrc-" + serviceId;
            final DataProviderFactory omFactory = DataProviders.getFactory("sensor-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("filesensor");
            
            config.parameter("data_directory").setValue(path);
            
            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "sensor-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    private Integer createDatasource(Datasource source) throws ConstellationException {
        if (source == null) return null;
        String location = source.getLocation();
        String userName = source.getUserName();
        String pwd = source.getPassword();
        DataSource ds = new DataSource(null, "database", location, userName, pwd, null, false, System.currentTimeMillis(), "COMPLETED", null, true, source.getAdvancedParameters());
        return datasourceBusiness.getOrcreate(ds);
    }
    
    private void createProviderFromFile(final Path path) {
        try {
            Provider providerConf = FS_MAPPER.readValue(path.toFile(), Provider.class);

            String dataType = providerConf.getDataType();
            String impl = providerConf.getProviderType();
            String dataStr = providerConf.getLocation();
            String dataset = providerConf.getDataset();
            String providerIdentifier = providerConf.getIdentifier();
            String dirFilter = providerConf.getDirectoryFilter();
            Integer datasourceId = null;
            
            final Pattern dirPattern = (dirFilter != null) ? Pattern.compile(dirFilter) : null;
            
            if (impl == null) {
                throw new ConstellationException("Provider type is missing for:" + path.getFileName().toString());
            }
            
            // special case
            String pathParamName = null;
            if (providerConf.getSource() != null) {
                datasourceId  = createDatasource(providerConf.getSource());
                if (datasourceId == null) throw new ConstellationException("Provider source missing for SQL provider.");
            } else if ("coverage-xml-pyramid".equals(impl)) {
                pathParamName = "path";
            // default case for file provider    
            } else if (dataStr != null) {
                pathParamName = "location";
            }
            
            List<Object> files = new ArrayList<>();
            if (dataStr != null) {
                try {
                    files.addAll(listFiles(path, dataStr, dirPattern));
                } catch (FileSystemNotFoundException ex) {
                    LOGGER.log(Level.FINER, ex.getMessage(), ex);
                    files = List.of(dataStr);
                }
            } 
            if (files.isEmpty()) {
                files = List.of("NO_FILES");
            }
            
            Integer dsId = dataset != null ? datasetBusiness.getOrCreateDataset(dataset, null) : null;
            
            // Acquire provider service instance.
            DataProviderFactory storeService = DataProviders.getFactory(dataType);
            if (storeService == null) {
                throw new ConstellationException("Provider service not found: " + dataType);
            }
            
            for (Object fileUri : files) {
                try {
                    String currentProviderId;
                    if (providerIdentifier == null) {
                        currentProviderId = impl + '-' + UUID.randomUUID();
                    } else {
                        currentProviderId = providerIdentifier;
                    }

                    if (providerBusiness.existIdentifier(currentProviderId)) {
                        throw new ConstellationException("Duplicated provider:" + currentProviderId);
                    }

                    final Parameters source = Parameters.castOrWrap(storeService.getProviderDescriptor().createValue());
                    source.parameter("id").setValue(currentProviderId);
                    source.parameter("providerType").setValue(dataType);

                    final List<ParameterValueGroup> choices = source.groups("choice");
                    final ParameterValueGroup choice;
                    if (choices.isEmpty()) {
                        choice = source.addGroup("choice");
                    } else {
                        choice = choices.get(0);
                    }
                    
                    final ParameterValueGroup config;
                    try {
                        config = choice.addGroup(impl);
                    } catch(ParameterNotFoundException ex) {
                        throw new ConstellationException("Unknow provider type: " + impl);
                    }
                    
                    if (pathParamName != null) {
                        config.parameter(pathParamName).setValue(fileUri);
                    }
                    
                    if (datasourceId != null) {
                        config.parameter("datasourceId").setValue(datasourceId);
                    }

                    ParameterDescriptorGroup configDescriptor = config.getDescriptor();
                    for (Entry<String, String> entry : providerConf.getAdvancedParameters().entrySet()) {
                        try {
                            GeneralParameterDescriptor genParamDesc = configDescriptor.descriptor(entry.getKey());
                            if (genParamDesc instanceof ParameterDescriptor paramDesc) {
                                Object converted = ObjectConverters.convert(entry.getValue(), paramDesc.getValueClass());
                                config.parameter(entry.getKey()).setValue(converted);
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Erreur while setting advanced parameter " + entry.getKey() + " on provider: " + providerConf.getIdentifier(), ex);
                        }
                    }
                    
                    /*
                     * special case for computed resource
                     */
                    if (COMPUTED_PROVIDER.equals(dataType)) {
                        List<Data> datas = new ArrayList<>();
                        for (Collection col : providerConf.getComputedData()) {
                            datas.addAll(getDataFromCollection(col));
                        }
                        GeneralParameterDescriptor genParamDesc = configDescriptor.descriptor("data_ids");
                        if (genParamDesc instanceof ParameterDescriptor paramDesc) {
                            for (Data brief : datas) {
                                ParameterValue value = paramDesc.createValue();
                                value.setValue(brief.getId());
                                config.values().add(value);
                            }
                        }
                    }

                    // Create provider and generate data.
                    final Integer pid = providerBusiness.storeProvider(currentProviderId, ProviderType.LAYER, dataType, source);
                    providerBusiness.createOrUpdateData(pid, dsId, true, false, null);

                    List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(pid);
                    dataBusiness.acceptDatas(dataIds, null, false);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error while importing provider file: " + path.getFileName().toString() + " data file: " + fileUri, ex);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while importing provider file: " + path.getFileName().toString(), ex);
        }
    }
    
    private void createStyleFromFile(Path path) {
        try {
            String fileName = path.getFileName().toString();
            String styleName = IOUtilities.filenameWithoutExtension(fileName);
            String type = "sld";

            //try to parse a style from various form and version
            MutableStyle style = (MutableStyle) styleBusiness.parseStyle(styleName, path, fileName);

            if (style == null) {
                throw new ConstellationException("Failed to import style from file, no UserStyle element defined, in file: " + fileName);
            }
            final boolean exists = styleBusiness.existsStyle(type, style.getName());
            if (!exists) {
                styleBusiness.createStyle(type, style);
            } else {
                throw new ConstellationException("Duplicated style:" + fileName);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while importing style file: " + path.getFileName().toString(), ex);
        }
    }
}