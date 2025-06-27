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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
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
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataSource;
import org.constellation.dto.fs.Collection;
import org.constellation.dto.fs.CollectionItem;
import org.constellation.dto.fs.Datasource;
import org.constellation.dto.fs.DimensionItem;
import org.constellation.dto.fs.Provider;
import org.constellation.dto.fs.Service;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.geotoolkit.style.MutableStyle;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
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
    
    private static final Map<String, List<String>> ALLOWED_MULTI_PROVIDER = new HashMap<>();
    static {
        ALLOWED_MULTI_PROVIDER.put("shapefile", List.of("shp"));
        ALLOWED_MULTI_PROVIDER.put("geotk_csv", List.of("csv"));
        ALLOWED_MULTI_PROVIDER.put("geojson",   List.of("json", "geojson"));
        ALLOWED_MULTI_PROVIDER.put("GeoTIFF",   List.of("tif", "tiff", "geotiff", "geotif"));
    }
    
    @PostConstruct
    public void initFsConfiguration() {
        installDatas();
    }

    @Override
    public void installDatas() {
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    -- STARTING FILESYSTEM CONFIG INSTALLATION               --
                    -----------------------------------------------------------
                    """);
        try {
            // install new data
            Path dataDir = configBusiness.getProvidersDirectory();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path) && "yaml".equals(FileNameUtils.getExtension(path))) {
                        createProviderFromFile(path);
                    }
                }
            }
            
            // install new styles
            Path styleDir = configBusiness.getStylesDirectory();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(styleDir)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path)) {
                        createStyleFromFile(path);
                    }
                }
            }
            
            // install new services
            Path servDir = configBusiness.getServicesDirectory();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(servDir)) {
                for (Path path : stream) {
                    if (!Files.isDirectory(path)) {
                        createServiceFromFile(path);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error a filesystem configuration startup", ex);
        }
    }
    
    private void createServiceFromFile(Path path) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Service instance = mapper.readValue(path.toFile(), Service.class);
            if (serviceBusiness.getServiceIdentifiers(instance.getType()).contains(instance.getIdentifier())) {
                throw new ConfigurationException("Service identifier: " + instance.getIdentifier() + "(" +  instance.getType() + ") already used");
            }
            int sid = serviceBusiness.create(instance.getType(), instance.getIdentifier(), null, null, null);

            // special case
            if ("STS".equalsIgnoreCase(instance.getType())) {
                boolean directProvider = Boolean.parseBoolean(instance.getAdvancedParameters().getOrDefault("direct-provider", "false"));
                if (directProvider) {
                    SOSConfiguration conf = (SOSConfiguration) serviceBusiness.getConfiguration(sid);
                    conf.getParameters().put("directProvider", "true");
                    serviceBusiness.setConfiguration(sid, conf);
                }
                
                Integer datasourceId;
                Datasource source = instance.getSource();
                if (source != null) {
                    datasourceId = createDatasource(source);
                } else {
                    throw new ConstellationException("Source missing for STS service.");
                }
                
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
                
                
                boolean generate = Boolean.parseBoolean(instance.getAdvancedParameters().getOrDefault("generate-from-existing", "false"));
                if (generate && !directProvider) {
                    sensorServiceBusiness.generateSensorFromOMProvider(sid);
                }

            } else if ("CSW".equalsIgnoreCase(instance.getType())) {
                boolean partial = false;
                int spid = createMetadataDatabaseProvider(instance.getIdentifier(), instance.getAdvancedParameters());
                if (!instance.getAdvancedParameters().isEmpty()) {
                    Automatic conf = (Automatic) serviceBusiness.getConfiguration(sid);
                    for (Entry<String, String> entry : instance.getAdvancedParameters().entrySet()) {
                        if (CSW_SERVICE_CONFIGURATION_PARAMETERS.contains(entry.getKey())) {
                            conf.getCustomparameters().put(entry.getKey(), entry.getValue());
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
                        conf.getCustomparameters().put("partial", "true");
                        partial = false;
                    }
                    serviceBusiness.setConfiguration(sid, conf);
                }
                serviceBusiness.linkCSWAndProvider(sid, spid, !partial);
            } else if ("WPS".equalsIgnoreCase(instance.getType())) {
                if (!instance.getProcessFactories().isEmpty()) {
                    ProcessContext conf = (ProcessContext) serviceBusiness.getConfiguration(sid);
                    List<ProcessFactory> factories = new ArrayList<>();
                    for (org.constellation.dto.fs.ProcessFactory factory : instance.getProcessFactories()) {
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
                    conf.setProcesses(factories);
                    serviceBusiness.setConfiguration(sid, conf);
                }
            }

            serviceBusiness.start(sid);

            for (Collection col : instance.getCollections()) {
                if (col.getDataSet() != null) {
                    Integer styleId = null;
                    if (col.getDatasetStyle() != null) {
                        styleId = styleBusiness.getStyleId("sld", col.getDatasetStyle());
                    }
                    Integer dsId = datasetBusiness.getDatasetId(col.getDataSet());
                    if (dsId == null) {
                        LOGGER.warning("Unable to find a dataset: " + col.getDataSet() + " for service: " + instance.getIdentifier() + " (" + instance.getType() + ")");
                        continue;
                    }
                    List<DataBrief> datas = dataBusiness.getDataBriefsFromDatasetId(dsId, true, false, null, null, false, false);
                    for (DataBrief data : datas) {
                        
                        if (!isAllowedDataTypeForService(instance.getType(), data.getType(), data.getSubtype())) {
                            LOGGER.finer("Data type: " + data.getType() + " not allowed for service: " + instance.getType());
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
                                final DimensionDefinition dimensionDef = new DimensionDefinition();
                                dimensionDef.setCrs(di.getName());
                                if (di.getColumn() != null) {
                                    dimensionDef.setLower(di.getColumn());
                                    dimensionDef.setUpper(di.getColumn());
                                } else if (di.getColumnLower() != null && di.getColumnUpper() != null) {
                                    dimensionDef.setLower(di.getColumnLower());
                                    dimensionDef.setUpper(di.getColumnUpper());
                                }

                                newLayer.getDimensions().add(dimensionDef);
                            }
                        } else {
                            title = data.getTitle();
                        }
                        int layerId = layerBusiness.add(data.getId(), alias, data.getNamespace(), data.getName(), title, sid, newLayer);
                        if (styleId != null) {
                            styleBusiness.linkToLayer(styleId, layerId);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while importing service file: " + path.getFileName().toString(), ex);
        }
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
    
    private Integer createOM2DatabaseProvider(String serviceId, Map<String, String> parameters, Integer datasourceId) {
        try {
            final String providerIdentifier = "omSrc-" + serviceId;
            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("observationSOSDatabase");
            
            config.parameter("datasource-id").setValue(datasourceId);
            setParameter(config, parameters, "max-field-by-table", false);
            setParameter(config, parameters, "database-readonly", false);
            setParameter(config, parameters, "mode", false);
            setParameter(config, parameters, "schema-prefix", false);
            
            // fixed for now
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            
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
            final ParameterValueGroup config = choice.addGroup("om2sensor");
            
            config.parameter("datasource-id").setValue(datasourceId);
            
            setParameter(config, parameters, "max-field-by-table", false);
            setParameter(config, parameters, "database-readonly", false);
            setParameter(config, parameters, "mode", false);
            setParameter(config, parameters, "schema-prefix", false);
            
            // fixed for now
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            
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
    
    private void setParameter(final ParameterValueGroup config, Map<String, String> parameters, String paramName, boolean mandatory) throws ConstellationException {
        
        String value = parameters.get(paramName);
        if (value == null) {
            if (mandatory) throw new ConstellationException("Missing advanced parameter: " + paramName);
            return;
        }
        ParameterValue<?> parameter = config.parameter(paramName);
        parameter.setValue(ObjectConverters.convert(value, parameter.getDescriptor().getValueClass()));
    }
    
    private Integer createDatasource(Datasource source) throws ConstellationException {
        String location = source.getLocation();
        String userName = source.getUserName();
        String pwd = source.getPassword();
        DataSource ds = new DataSource(null, "database", location, userName, pwd, null, false, System.currentTimeMillis(), "COMPLETED", null, true, source.getAdvancedParameters());
        return datasourceBusiness.create(ds);
    }
    
    private void createProviderFromFile(final Path path) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Provider providerConf = mapper.readValue(path.toFile(), Provider.class);

            String dataType = providerConf.getDataType();
            String impl = providerConf.getProviderType();
            String dataStr = providerConf.getLocation() != null ? providerConf.getLocation() : null;
            String dataset = providerConf.getDataset();
            String pathParamName = "location";
            String providerIdentifier = providerConf.getIdentifier();
            Integer datasourceId = null;
            
            // special case
            if ("coverage-xml-pyramid".equals(impl)) {
                pathParamName = "path";
            } else if ("SQL".equals(impl)) {
                pathParamName = "datasourceId";
                Datasource source = providerConf.getSource();
                if (source != null) {
                    datasourceId = createDatasource(source);
                } else {
                    throw new ConstellationException("Provider source missing for SQL provider.");
                }
            }
            
            // special case for folder
            List<Object> files = new ArrayList<>();
            
            if (datasourceId != null) {
                files.add(datasourceId);
            } else {
                try {
                    URI dataUri = URI.create(dataStr);
                    Path dataDir = Paths.get(dataUri);
                    if (ALLOWED_MULTI_PROVIDER.containsKey(impl) && Files.isDirectory(dataDir)) {
                        List<String> exts = ALLOWED_MULTI_PROVIDER.get(impl);
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir)) {
                            for (Path file : stream) {
                                if (!Files.isDirectory(file) && exts.contains(FileNameUtils.getExtension(file).toLowerCase())) {
                                    files.add(file.toUri());
                                }
                            }
                        }
                    } else {
                        files.add(dataUri);
                    }
                } catch (FileSystemNotFoundException ex) {
                    LOGGER.log(Level.FINER, ex.getMessage(), ex);
                    files = List.of(dataStr);
                }
            }

            Integer dsId = dataset != null ? datasetBusiness.getDatasetId(dataset) : null;
            if (dsId == null && dataset != null) {
                dsId = datasetBusiness.createDataset(dataset, null, null);
            }
            
            // Acquire provider service instance.
            DataProviderFactory storeService = null;
            for (final DataProviderFactory service : DataProviders.getFactories()) {
                if (service.getName().equals(dataType)) {
                    storeService = service;
                    break;
                }
            }
            if (storeService == null) {
                throw new ConstellationException("Provider service not found.");
            }
            
            for (Object fileUri : files) {
                try {
                    String currentProviderId;
                    if (providerIdentifier == null) {
                        currentProviderId = impl + '-' + UUID.randomUUID();
                    } else {
                        currentProviderId = providerIdentifier;
                    }

                    Integer provider = providerBusiness.getIDFromIdentifier(currentProviderId);
                    if (provider != null) {
                        throw new ConstellationException("Duplicated provider:" + currentProviderId);
                    }

                    final ParameterValueGroup source = Parameters.castOrWrap(storeService.getProviderDescriptor().createValue());
                    source.parameter("id").setValue(currentProviderId);
                    source.parameter("providerType").setValue(dataType);

                    final List<ParameterValueGroup> choices = source.groups("choice");
                    final ParameterValueGroup choice;
                    if (choices.isEmpty()) {
                        choice = source.addGroup("choice");
                    } else {
                        choice = choices.get(0);
                    }

                    final ParameterValueGroup config = choice.addGroup(impl);
                    config.parameter(pathParamName).setValue(fileUri);

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

                    // Create provider and generate data.
                    final Integer pid = providerBusiness.storeProvider(currentProviderId, ProviderType.LAYER, "data-store", source);
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
            String styleName = FilenameUtils.removeExtension(fileName);
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
    
    private static final List<String> VECTOR_ALLOWED = List.of("wfs", "wms");
    private static final List<String> COVERAGE_ALLOWED = List.of("wcs", "wms");
    
    private boolean isAllowedDataTypeForService(String serviceType, String dataType, String subDataType) {
        return switch (dataType.toLowerCase()) {
            case "vector"   -> VECTOR_ALLOWED.contains(serviceType.toLowerCase());
            case "coverage" -> ("pyramid".equals(subDataType.toLowerCase()) && "wmts".equals(serviceType.toLowerCase())) ||
                               (!"pyramid".equals(subDataType.toLowerCase()) && COVERAGE_ALLOWED.contains(serviceType.toLowerCase()));
            
            default -> false;
        };
    }
}
