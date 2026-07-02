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

import com.examind.community.storage.sql.CoverageSQLProvider.CoverageSQLStore;
import jakarta.annotation.PostConstruct;
import java.nio.file.FileSystemNotFoundException;
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
import com.examind.dto.fs.ProcessFactory;
import com.examind.dto.fs.Provider;
import com.examind.dto.fs.Service;
import com.examind.setup.FileSystemAnalysis.ProviderWithPath;
import static com.examind.setup.FileSystemUtilities.*;
import static com.examind.setup.ProviderUtilities.COMPUTED_PROVIDER;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.apache.sis.storage.DataStoreException;
import org.constellation.dto.Data;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.provider.DataProvider;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@Profile("fsconfig")
public class FileSystemSetupBusiness implements IFileSystemSetupBusiness {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    private static final String NO_FILES = "NO_FILES";
    
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
    
    private static final List<String> CSW_SERVICE_CONFIGURATION_PARAMETERS = List.of("collection", "onlyPublished", "partial", "es-url", "transactional");

    // advancedParameters key: when set on an OPENEO service, skip local WCS creation (STAC catalog is external)
    private static final String OPENEO_EXTERNAL_STAC_PARAM = "externalStacUrl";
    
    /**
     * Executor to perform task asynchroneously.
     */
    @Autowired
    @Qualifier("cstlExecutor")
    private TaskExecutor taskExecutor;
    
    @PostConstruct
    public void initFsConfiguration() {
        if (Application.getBooleanProperty(AppProperty.EXA_FS_STARTUP, Boolean.TRUE)) {
            if (Application.getBooleanProperty(AppProperty.EXA_FS_ASYNC, Boolean.FALSE)) {
                taskExecutor.execute(() -> installDatas(true));
            } else {
                installDatas(false);
            }
        }
    }
    
    /**
     * Install all the styles, services and providers from the filesystem configuration.
     * 
     * @param async asynchroneous mode.
     */
    @Override
    public void installDatas(boolean async) {
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    --        STARTING FILESYSTEM CONFIG INSTALLATION        --
                    -----------------------------------------------------------
                    
                    """);
        try {
            Path styleDir = configBusiness.getStylesDirectory();
            Path servDir  = configBusiness.getServicesDirectory();
            Path dataDir  = configBusiness.getProvidersDirectory();
            
            FileSystemAnalysis analysis = new FileSystemAnalysis(styleDir, servDir, dataDir, this::parseStyle, async);
                    
            // 1. install styles
            for (MutableStyle style : analysis.styles.values()) {
                createStyleFromFile(style);
            }
            
            // 2. install services with data
            for (Service service : analysis.servicesWithData.values()) {
                createServiceFromFile(service);
            }
            
            // 3. (Async) install services
            if (async) {
                for (Service service : analysis.services.values()) {
                    createServiceFromFile(service);
                }
            }
            
            // 4. install regular data
            for (ProviderWithPath provider : analysis.providers.values()) {
                createProviderFromFile(provider, analysis.asyncInfos, async);
            }
            
            // 5. install computed data that use data created in the previous pass
            for (ProviderWithPath provider : analysis.computedProviders.values()) {
                createProviderFromFile(provider, analysis.asyncInfos, async);
            }
            
            // 6. (Sync) install services
            if (!async) {
                for (Service service : analysis.services.values()) {
                    createServiceFromFile(service);
                }
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error a filesystem configuration startup", ex);
        }
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    --        FILESYSTEM CONFIG INSTALLATION COMPLETE        --
                    -----------------------------------------------------------
                    
                    """);
    }
    
    /**
     * Instanciate a service from its configuration.
     * 
     * @param instance Service configuration.
     */
    private void createServiceFromFile(Service instance) {
        if ("OPENEO".equalsIgnoreCase(instance.getType())) {
            createOpenEOServicesFromFile(instance);
            return;
        }
        try {
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
                
                Integer datasourceId = createDatasource(instance.getType() + "-" + instance.getIdentifier(), instance.getSource());
                
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
                    conf.setProcesses(toWPSConfig(instance));
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
                    publishLayersOnService(col, sid, instance.getType());
                } else {
                    LOGGER.warning("No dataset specified in collection");
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while importing service: " + instance.getType() + " " + instance.getIdentifier(), ex);
        }
    }
    
    /**
     * OpenEO runs on a WPS (process part) and a WCS (STAC/data part) sharing the same identifier.
     * Expand a single "OPENEO" filesystem entry into that WPS + WCS pair instead of requiring
     * both services to be declared and wired manually in the UI.
     *
     * @param instance OpenEO service configuration.
     */
    private void createOpenEOServicesFromFile(Service instance) {
        Service wps = new Service();
        wps.setIdentifier(instance.getIdentifier());
        wps.setType("WPS");
        wps.setMetadata(instance.getMetadata());

        wps.setProcessFactories(instance.getProcessFactories());
        createServiceFromFile(wps);

        // external STAC catalog: the WCS below just proxies it, the actual data lives outside Examind.
        // Store the url on the WPS configuration: it takes priority over the app property
        // EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE, which stays as the base/fallback value.
        Map<String, String> wcsParameters = instance.getAdvancedParameters();
        String externalStacUrl = wcsParameters.get(OPENEO_EXTERNAL_STAC_PARAM);
        if (externalStacUrl != null) {
            Integer wpsId = serviceBusiness.getServiceIdByIdentifierAndType("wps", instance.getIdentifier());
            try {
                AbstractConfigurationObject wpsConf = serviceBusiness.getConfiguration(wpsId);
                wpsConf.setProperty(OPENEO_EXTERNAL_STAC_PARAM, externalStacUrl);
                serviceBusiness.setConfiguration(wpsId, wpsConf);
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "Error while storing external STAC url on WPS service: " + instance.getIdentifier(), ex);
            }
            wcsParameters = new HashMap<>(wcsParameters);
            wcsParameters.remove(OPENEO_EXTERNAL_STAC_PARAM);
        }

        Service wcs = new Service();
        wcs.setIdentifier(instance.getIdentifier());
        wcs.setType("WCS");
        wcs.setMetadata(instance.getMetadata());
        wcs.setAdvancedParameters(wcsParameters);
        wcs.setCollections(instance.getCollections());
        createServiceFromFile(wcs);
    }

    /**
     * Publish data on a service.
     * 
     * @param col Collection configuration.
     * @param serviceId Service identifier.
     * @param serviceType Service Type.
     * @throws ConstellationException 
     */
    private void publishLayersOnService(Collection col, int serviceId, String serviceType) throws ConstellationException {
        Integer styleId = (col.getDatasetStyle() != null) ? styleBusiness.getStyleId("sld", col.getDatasetStyle()) : null;
        List<Data> datas = getDataFromCollection(col);

        for (Data data : datas) {

            if (!isAllowedDataTypeForService(serviceType, data.getType(), data.getSubtype())) {
                LOGGER.log(Level.FINER, "Data type: {0} not allowed for service: {1}", new Object[]{data.getType(), serviceType});
                continue;
            }

            //create future new layer
            QName layerQName     = new QName(data.getName(), data.getNamespace());
            LayerConfig newLayer = new LayerConfig(layerQName);

            CollectionItem custom = col.getItemByName(data.getName(), data.getNamespace());
            String alias = null;
            String aliasNmsp;
            String name;
            String title ;
            if (custom != null) {
                alias = custom.getAlias();
                aliasNmsp = custom.getAliasNamespace() ;
                // special case for custom namespace, we use the alias as name
                if (aliasNmsp != null) {
                    name = alias;
                    alias = null;
                } else {
                    name = data.getName();
                }
                title = custom.getTitle();
                if (custom.getStyle() != null) {
                    try {
                        styleId = styleBusiness.getStyleId("sld", custom.getStyle());
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while importing style : " + custom.getStyle() + " for data: " + data.getName(), ex);
                    }
                }
                for (DimensionItem di : custom.getDimensions()) {
                    newLayer.addDimension(new DimensionDefinition(di));
                }
            } else {
                title = data.getName();
                name = data.getName();
                aliasNmsp = data.getNamespace();
            }
            if (!layerBusiness.exists(serviceId, alias, name, aliasNmsp)) {
                int layerId = layerBusiness.add(data.getId(), alias, aliasNmsp, name, title, serviceId, newLayer);
                if (styleId != null) {
                    styleBusiness.linkToLayer(styleId, layerId);
                }
            }
        }
    }
    
    /**
     * Extract data from a configuration collection.
     * 
     * @param col
     * @return
     * @throws ConstellationException 
     */
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
    
    private Integer createCoverageSQLProvider(Provider providerConf, Integer datasetId, Integer datasourceId, List<Object> files) throws Exception {
        if (datasourceId == null) {
            throw new ConstellationException("Provider source missing for SQL provider.");
        }
        if (files.size() == 1 && files.get(0) instanceof String s && NO_FILES.equals(s)) {
            throw new ConstellationException("No file found for coverage sql provider");
        }
        
        final String providerIdentifier = "csql-" + datasourceId;
        Integer prId = providerBusiness.getIDFromIdentifier(providerIdentifier);
        
        // we keep only one provider by datasource
        if (prId == null) {
            final DataProviderFactory dsFactory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source    = dsFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dsFactory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("exa-coverage-sql");
            config.parameter("datasourceId").setValue(datasourceId);
            config.parameter("rootDirectory").setValue(Path.of("/"));
            prId = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
        }
        
        DataProvider provider = DataProviders.getProvider(prId);
        CoverageSQLStore store = (CoverageSQLStore) provider.getMainStore();
        
        String productName = providerConf.getAdvancedParameters().get("productName");
        String subDataType = providerConf.getAdvancedParameters().get("subDataType");
        boolean asChild    = Boolean.parseBoolean(providerConf.getAdvancedParameters().getOrDefault("asChild", "false"));
        boolean worldGG    = Boolean.parseBoolean(providerConf.getAdvancedParameters().getOrDefault("worldGG", "false"));
        String wgrStr      = providerConf.getAdvancedParameters().get("worldGGResolution");
        Double worldGGRes  = wgrStr != null ? Double.valueOf(wgrStr) : null;
        
        List<Path> dataPaths = files.stream().map(uri -> Paths.get((URI)uri)).toList();
        try {
            store.createProduct(productName, worldGG, worldGGRes, asChild, subDataType, dataPaths);
        } catch (DataStoreException ex) {
            throw new ConstellationException("Error while adding raster into coverage sql", ex);
        }
        
        provider.reload();
        providerBusiness.createOrUpdateData(prId, null, false, false, null);

        List<Integer> productIds = new ArrayList<>();
        List<Data> datas = dataRepository.findByProviderId(prId);
        for (Data data : datas) {
            if (data.getName().equals(productName)     ||  // single product
                data.getNamespace().equals(productName)) { // aggregated product
                productIds.add(data.getId());
                dataBusiness.updateDataDataSetId(data.getId(), datasetId);
            }
        }
        dataBusiness.acceptDatas(productIds, null, false);
        return prId;
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
    
    private Integer createDatasource(String identifier, Datasource source) throws ConstellationException {
        if (source == null) return null;
        String location = source.getLocation();
        String userName = source.getUserName();
        String pwd = source.getPassword();
        DataSource ds = new DataSource(null, identifier, "database", location, userName, pwd, null, false, System.currentTimeMillis(), "COMPLETED", null, true, source.getAdvancedParameters());
        return datasourceBusiness.getOrcreate(ds);
    }
    
    private void createProviderFromFile(final ProviderWithPath provider, Map<String, List<Service>> providerServiceLink, boolean async) {
        try {
            Provider providerConf = provider.provider;

            String dataType = providerConf.getDataType();
            String impl = providerConf.getProviderType();
            String dataStr = providerConf.getLocation();
            String dataset = providerConf.getDataset();
            String providerIdentifier = providerConf.getIdentifier();
            String dirFilter = providerConf.getDirectoryFilter();
            Integer datasourceId = null;
            
            final Pattern dirPattern = (dirFilter != null) ? Pattern.compile(dirFilter) : null;
            
            if (impl == null) {
                throw new ConstellationException("Provider type is missing for:" + providerConf.getIdentifier());
            }
            
            // special case
            String pathParamName = null;
            if (providerConf.getSource() != null) {
                datasourceId  = createDatasource(providerIdentifier, providerConf.getSource());
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
                    files.addAll(listFiles(provider.ymlFile, dataStr, dirPattern));
                } catch (FileSystemNotFoundException ex) {
                    LOGGER.log(Level.FINER, ex.getMessage(), ex);
                    files = List.of(dataStr);
                }
            } 
            if (files.isEmpty()) {
                files = List.of(NO_FILES);
            }
            
            Integer dsId = dataset != null ? datasetBusiness.getOrCreateDataset(dataset, null) : null;
            
            if ("coverage-sql".equals(impl)) {
                createCoverageSQLProvider(providerConf, dsId, datasourceId, files);
                return;
            }
            
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
                    
                    // SYNC MODE Add layer and reload needed service
                    if (async && dataset != null) {
                        List<Service> services = providerServiceLink.getOrDefault(dataset, new ArrayList<>());
                        for (Service service : services) {
                            Integer sid = serviceBusiness.getServiceIdByIdentifierAndType(service.getType(), service.getIdentifier());
                            Collection collection = service.getCollection(dataset);
                            if (collection != null) {
                                publishLayersOnService(collection, sid, service.getType());
                                serviceBusiness.restart(sid);
                            } else {
                                LOGGER.log(Level.WARNING, "unable to find a collection with dataset {0} in service ({1}) {2}", new Object[]{dataset, service.getType(), service.getIdentifier()});
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error while importing provider file: " + provider.ymlFile.getFileName().toString() + " data file: " + fileUri, ex);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider file: " + provider.ymlFile.getFileName().toString(), ex);
        }
    }
    
    private MutableStyle parseStyle(Path path) {
        String fileName = path.getFileName().toString();
        String styleName = IOUtilities.filenameWithoutExtension(fileName);


        MutableStyle style = (MutableStyle) styleBusiness.parseStyle(styleName, path, fileName);

        if (style == null) {
            LOGGER.log(Level.WARNING, "Failed to import style from file: {0}", fileName);
        }
        return style;
    }

    private void createStyleFromFile(MutableStyle style) {
        try {
            String type = "sld";
            final boolean exists = styleBusiness.existsStyle(type, style.getName());
            if (!exists) {
                styleBusiness.createStyle(type, style);
            } else {
                LOGGER.log(Level.WARNING, "Duplicated style:{0}", style.getName());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing style: " + style.getName(), ex);
        }
    }
}
