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
import com.examind.dto.fs.Collection;
import com.examind.dto.fs.CollectionItem;
import com.examind.dto.fs.DimensionItem;
import com.examind.dto.fs.Provider;
import com.examind.dto.fs.Service;
import com.examind.setup.FileSystemAnalysis.ProviderWithPath;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.apache.sis.storage.DataStoreException;
import org.constellation.api.PathStatus;
import static org.constellation.api.PathStatus.MODIFIED;
import static org.constellation.api.PathStatus.PENDING;
import org.constellation.dto.Data;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.repository.DataRepository;
import org.geotoolkit.style.MutableStyle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;

import static com.examind.setup.FileSystemUtilities.*;
import static com.examind.setup.DatasourceUtilities.*;
import static com.examind.setup.ProviderUtilities.*;

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
        boolean execAtStartup = Application.getBooleanProperty(AppProperty.EXA_FS_STARTUP, Boolean.TRUE);
        if (execAtStartup) {
            boolean async = Application.getBooleanProperty(AppProperty.EXA_FS_ASYNC, Boolean.FALSE);
            boolean diffMode = Application.getBooleanProperty(AppProperty.EXA_FS_DIFF, Boolean.FALSE);
            if (diffMode) {
                if (async) {
                    taskExecutor.execute(() -> performDiff(async));
                } else {
                    performDiff(async);
                }
            } else {
                if (async) {
                    taskExecutor.execute(() -> installDatas(async));
                } else {
                    installDatas(async);
                }
            }
        }
    }
    
    /**
     * Install all the styles, services and providers from the filesystem configuration.
     * 
     * @param async asynchroneous mode.
     */
    @Override
    public void performDiff(boolean async) {
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    --        STARTING FILESYSTEM CONFIG DIFF               --
                    -----------------------------------------------------------
                    
                    """);
        
        try {
            Path styleDir = configBusiness.getStylesDirectory();
            Path servDir  = configBusiness.getServicesDirectory();
            Path provDir  = configBusiness.getProvidersDirectory();
            
            FileSystemAnalysis analysis = new FileSystemAnalysis(styleDir, servDir, provDir, this::parseStyle, async);
            
            // 1. install styles
            int dsId = createDatasourceForConfigFiles(datasourceBusiness, "stylesFS", styleDir, FileSystemUtilities::sldFileFilter);
            List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleStylePath(path);
            }
            
            // 2. install services with data
            dsId = createDatasourceForConfigFiles(datasourceBusiness, "serviceWithDataFS", servDir, FileSystemUtilities::serviceWithDataFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleServicePath(path);
            }
            
            // 3. install services with data
            dsId = createDatasourceForConfigFiles(datasourceBusiness, "serviceFS", servDir, FileSystemUtilities::serviceNoDataFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleProviderPath(path, analysis.asyncInfos);
            }
            
            // 4. install providers
            dsId = createDatasourceForConfigFiles(datasourceBusiness, "providerFS", provDir, FileSystemUtilities::regularProviderFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleProviderPath(path, analysis.asyncInfos);
            }
            
            // 5. install computed providers
            dsId = createDatasourceForConfigFiles(datasourceBusiness, "providerConputedFS", provDir, FileSystemUtilities::computedProviderFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleProviderPath(path, analysis.asyncInfos);
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error a filesystem configuration startup", ex);
        }
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    --        FILESYSTEM CONFIG DIFF COMPLETE        --
                    -----------------------------------------------------------
                    
                    """);
    }
    
    
    private void handleStylePath(DataSourceSelectedPath path) throws ConstellationException {
        Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
        switch (PathStatus.valueOf(path.getStatus())) {
            case PENDING -> {
                MutableStyle s = parseStyle(p);
                PathStatus newStatus;
                if (s != null) {
                    Integer styleId = importStyle(s);
                    if (styleId != null) {
                        datasourceBusiness.updatePathProvider(path.getDatasourceId(), path.getPath(), styleId);
                        newStatus = PathStatus.INTEGRATED;
                    } else {
                        newStatus = PathStatus.ERROR; // NO DATA?
                    }
                } else {
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
            }
            case MODIFIED -> {
                MutableStyle s = parseStyle(p);
                PathStatus newStatus;
                if (s != null) {
                    styleBusiness.updateStyle(path.getProviderId(), s.getName(), s);
                    newStatus = PathStatus.INTEGRATED;
                } else {
                    // what to do with the old style? remove it?
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
            }

            case REMOVED -> {
                styleBusiness.deleteStyle(path.getProviderId());
                datasourceBusiness.removePath(path.getDatasourceId(), path.getPath());
            }
        }
    }
    
    private void handleServicePath(DataSourceSelectedPath path) throws ConstellationException {
        Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
        switch (PathStatus.valueOf(path.getStatus())) {
            case PENDING -> {
                Service s = parseYaml(p, Service.class);
                PathStatus newStatus;
                if (s != null) {
                    Integer sid = createService(s);
                    if (sid != null) {
                        datasourceBusiness.updatePathProvider(path.getDatasourceId(), path.getPath(), sid);
                        newStatus = PathStatus.INTEGRATED;
                    } else {
                        newStatus = PathStatus.ERROR; // NO DATA?
                    }
                } else {
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
            }
            case MODIFIED -> {
                Service s = parseYaml(p, Service.class);
                PathStatus newStatus;
                if (s != null) {
                    int sid = updateService(path.getProviderId(), s);
                    datasourceBusiness.updatePathProvider(path.getDatasourceId(), path.getPath(), sid);
                    newStatus = PathStatus.INTEGRATED;
                } else {
                    // what to do with the old service? remove it?
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
            }

            case REMOVED -> {
                serviceBusiness.delete(path.getProviderId());
                datasourceBusiness.removePath(path.getDatasourceId(), path.getPath());
            }
        }
    }
    
    private void handleProviderPath(DataSourceSelectedPath path, Map<String, List<Service>> providerServiceLink) throws ConstellationException {
        Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
        switch (PathStatus.valueOf(path.getStatus())) {
            case PENDING -> {
                Provider pr = parseYaml(p, Provider.class);
                PathStatus newStatus;
                if (pr != null) {
                    ProviderWithPath pwp = new ProviderWithPath(pr, p);
                    List<Integer> pids = createProvider(pwp, providerServiceLink, true);
                    
                    // TODO handle link between yaml file and multiple providers
                    if (!pids.isEmpty()) {
                        if (pids.size() == 1) {
                            datasourceBusiness.updatePathProvider(path.getDatasourceId(), path.getPath(), pids.get(0));
                        }
                        newStatus = PathStatus.INTEGRATED;
                    } else {
                        newStatus = PathStatus.ERROR; // NO DATA?
                    }
                } else {
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
            }
            case MODIFIED -> {
                Provider pr = parseYaml(p, Provider.class);
                PathStatus newStatus;
                if (pr != null) {
                    ProviderWithPath pwp = new ProviderWithPath(pr, p);
                    List<Integer> pids = updateProvider(path.getProviderId(), pwp, providerServiceLink);
                    
                    // TODO handle link between yaml file and multiple providers
                    if (!pids.isEmpty()) {
                        if (pids.size() == 1) {
                            datasourceBusiness.updatePathProvider(path.getDatasourceId(), path.getPath(), pids.get(0));
                        }
                        newStatus = PathStatus.INTEGRATED;
                    } else {
                        // what to do with the old provider(s)? remove it?
                        newStatus = PathStatus.ERROR;
                    }
                } else {
                    // what to do with the old provider(s)? remove it?
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
            }

            case REMOVED -> {
                // TODO handle link between yaml file and multiple providers
                if (path.getProviderId() != null && path.getProviderId() != -1) {
                    providerBusiness.removeProvider(path.getProviderId());
                }
                datasourceBusiness.removePath(path.getDatasourceId(), path.getPath());
            }
        }
    }
    
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
                importStyle(style);
            }
            
            // 2. install services with data
            for (Service service : analysis.servicesWithData.values()) {
                createService(service);
            }
            
            // 3. (Async) install services
            if (async) {
                for (Service service : analysis.services.values()) {
                    createService(service);
                }
            }
            
            // 4. install regular data
            for (ProviderWithPath provider : analysis.providers.values()) {
                createProvider(provider, analysis.asyncInfos, false);
            }
            
            // 5. install computed data that use data created in the previous pass
            for (ProviderWithPath provider : analysis.computedProviders.values()) {
                createProvider(provider, analysis.asyncInfos, false);
            }
            
            // 6. (Sync) install services
            if (!async) {
                for (Service service : analysis.services.values()) {
                    createService(service);
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
    private Integer createService(Service instance) {
        try {
            if (serviceBusiness.getServiceIdentifiers(instance.getType()).contains(instance.getIdentifier())) {
                throw new ConfigurationException("Service identifier: " + instance.getIdentifier() + "(" +  instance.getType() + ") already used");
            }
            
            if ("OPENEO".equalsIgnoreCase(instance.getType())) {
                return createOpenEOServicesFromFile(instance);
            }
            
            Details metadata = instance.getMetadata();
            metadata.setIdentifier(instance.getIdentifier());
            int sid = serviceBusiness.create(instance.getType(), instance.getIdentifier(), null, metadata, null);
            
            // special case
            if ("STS".equalsIgnoreCase(instance.getType())) {
                boolean directProvider = instance.getAdvancedParameter("direct-provider", false);
                if (directProvider) {
                    AbstractConfigurationObject conf = serviceBusiness.getConfiguration(sid);
                    conf.setProperty("directProvider", "true");
                    serviceBusiness.setConfiguration(sid, conf);
                }
                
                Integer datasourceId = createSQLDatasource(datasourceBusiness, instance.getType() + "-" + instance.getIdentifier(), instance.getSource());
                
                int pid = createOM2DatabaseProvider(providerBusiness, instance.getIdentifier(), instance.getAdvancedParameters(), datasourceId);
                serviceBusiness.linkServiceAndSensorProvider(sid, pid, true);
                
                boolean fullLink;
                int spid;
                if (directProvider) {
                    spid = createSensorDatabaseProvider(providerBusiness, instance.getIdentifier(), instance.getAdvancedParameters(), datasourceId);
                    fullLink = true;
                } else {
                    String sensorFolder = instance.getAdvancedParameter("sensor-metadata-path", null);
                    if (sensorFolder == null) {
                        spid = sensorBusiness.getDefaultInternalProviderID();
                        fullLink = false;
                    } else {
                        spid = createSensorFSProvider(providerBusiness, instance.getIdentifier(), sensorFolder);
                        providerBusiness.createOrUpdateData(spid, null, false, false, null);
                        fullLink = true;
                    }
                }
                serviceBusiness.linkServiceAndSensorProvider(sid, spid, fullLink);
                
                boolean generateSensor = instance.getAdvancedParameter("generate-from-existing", false);
                if (generateSensor && !directProvider) {
                    sensorServiceBusiness.generateSensorFromOMProvider(sid);
                }
                
                boolean generateData = instance.getAdvancedParameter("create-data", false);
                if (generateData) {
                    providerBusiness.createOrUpdateData(pid, null, true, false, null);
                }

            } else if ("CSW".equalsIgnoreCase(instance.getType())) {
                boolean partial = false;
                
                String dataDirectory = instance.getAdvancedParameter("dataDirectory", null);
                int spid;
                if (dataDirectory == null) {
                    spid = metadataBusiness.getDefaultInternalProviderID();
                } else {
                    spid = createMetadataFSProvider(providerBusiness, instance.getIdentifier(), dataDirectory);
                }
                
                if (!instance.getAdvancedParameters().isEmpty()) {
                    partial = instance.getAdvancedParameter("partial", false);
                    
                    Automatic conf = (Automatic) serviceBusiness.getConfiguration(sid);
                    for (Entry<String, String> entry : instance.getAdvancedParameters().entrySet()) {
                        if (CSW_SERVICE_CONFIGURATION_PARAMETERS.contains(entry.getKey())) {
                            conf.setProperty(entry.getKey(), entry.getValue());
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
            return sid;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while importing service: " + instance.getType() + " " + instance.getIdentifier(), ex);
        }
        return null;
    }
    
     private Integer updateService(Integer serviceId, Service instance) throws ConstellationException {
        // for now we do an simple remove/create 
        // TODO update metadata
        // TODO linked files?
        serviceBusiness.delete(serviceId);
        return createService(instance);
    }
    
    private List<Integer> updateProvider(Integer providerId, ProviderWithPath provider, Map<String, List<Service>> providerServiceLink) throws ConstellationException {
        // for now we do an simple remove/create 
        // TODO update metadata
        // TODO linked files?
        providerBusiness.removeProvider(providerId);
        return createProvider(provider, providerServiceLink, true);
    }

    /**
     * OpenEO runs on a WPS (process part) and a WCS (STAC/data part) sharing the same identifier.
     * Expand a single "OPENEO" filesystem entry into that WPS + WCS pair instead of requiring
     * both services to be declared and wired manually in the UI.
     *
     * @param instance OpenEO service configuration.
     */
    private Integer createOpenEOServicesFromFile(Service instance) {
        Service wps = new Service(instance.getIdentifier(), "WPS", instance.getMetadata());
        wps.setProcessFactories(instance.getProcessFactories());
        
        Integer wpsId = createService(wps);

        // external STAC catalog: the WCS below just proxies it, the actual data lives outside Examind.
        // Store the url on the WPS configuration: it takes priority over the app property
        // EXA_OPENEO_EXTERNAL_STAC_PER_WPS_SERVICE, which stays as the base/fallback value.
        Map<String, String> wcsParameters = instance.getAdvancedParameters();
        String externalStacUrl = wcsParameters.get(OPENEO_EXTERNAL_STAC_PARAM);
        if (externalStacUrl != null) {
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

        Service wcs = new Service(instance.getIdentifier(), "WCS", instance.getMetadata());
        wcs.setAdvancedParameters(wcsParameters);
        wcs.setCollections(instance.getCollections());
        
        
        // for now i return only the wcs id.
        // i dont know if i should return the wps one, or both
        return createService(wcs);
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
            prId = createCSQLProvider(providerBusiness, providerIdentifier, datasourceId);
        }
        
        DataProvider provider = DataProviders.getProvider(prId);
        CoverageSQLStore store = (CoverageSQLStore) provider.getMainStore();
        
        String productName = providerConf.getAdvancedParameter("productName", (String) null);
        String subDataType = providerConf.getAdvancedParameter("subDataType", (String) null);
        boolean asChild    = providerConf.getAdvancedParameter("asChild", false);
        boolean worldGG    = providerConf.getAdvancedParameter("worldGG", false);
        Double worldGGRes  = providerConf.getAdvancedParameter("worldGGResolution", (Double) null);
        
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
    
    private List<Integer> createProvider(final ProviderWithPath provider, Map<String, List<Service>> providerServiceLink, boolean diffMode) {
        List<Integer> results = new ArrayList<>();
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
                datasourceId  = createSQLDatasource(datasourceBusiness, providerIdentifier, providerConf.getSource());
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
                    if (diffMode) {
                        Predicate<Path> filter = dirPattern != null ? p -> regexFileFilter(p, dirPattern) : null;
                        Path dataPath = getDataPathPath(provider.ymlFile.getParent(), dataStr);
                        int dsId = createDatasourceForProviderFiles(datasourceBusiness, providerIdentifier, dataPath, filter, impl);
                        List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
                        for (DataSourceSelectedPath path : paths) {
                            Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
                            files.add(p.toUri());
                            // TODO
                        }
                    } else {
                        files.addAll(listFiles(provider.ymlFile, dataStr, dirPattern));
                    }
                } catch (FileSystemNotFoundException ex) {
                    LOGGER.log(Level.FINER, ex.getMessage(), ex);
                    // not sure if i have to keep this case
                    files = List.of(dataStr);
                }
            }
            
            Integer dsId = dataset != null ? datasetBusiness.getOrCreateDataset(dataset, null) : null;
            
            if ("coverage-sql".equals(impl)) {
                final Integer pid = createCoverageSQLProvider(providerConf, dsId, datasourceId, files);
                
                // data are already generated
                
                results.add(pid);
            } else if (COMPUTED_PROVIDER.equals(dataType)) {
                List<Data> datas = new ArrayList<>();
                for (Collection col : providerConf.getComputedData()) {
                    datas.addAll(getDataFromCollection(col));
                }
                // Create provider
                final Integer pid = createComputedProvider(dataType, providerBusiness, providerIdentifier, impl, datas, providerConf.getAdvancedParameters());
                
                // Generate data.
                generateDatas(pid, dsId, dataset, providerServiceLink);
                
                results.add(pid);
            } else {
            
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

                        // Create provider
                        final Integer pid = createFileProvider(dataType, providerBusiness, currentProviderId, impl, datasourceId, fileUri, pathParamName, providerConf.getAdvancedParameters());

                        // Generate data.
                        generateDatas(pid, dsId, dataset, providerServiceLink);
                        
                        results.add(pid);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while importing provider file: " + provider.ymlFile.getFileName().toString() + " data file: " + fileUri, ex);
                    }
                }
            }
            
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider file: " + provider.ymlFile.getFileName().toString(), ex);
        }
        return results;
    }
    
    private void generateDatas(int pid, int dsId, String dataset, Map<String, List<Service>> providerServiceLink) throws ConstellationException {
        providerBusiness.createOrUpdateData(pid, dsId, true, false, null);

        List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(pid);
        dataBusiness.acceptDatas(dataIds, null, false);
        
        // ASYNC MODE Add layer and reload needed service
        if (providerServiceLink != null && dataset != null) {
            asyncServiceReload(dataset, providerServiceLink);
        }
    }
    
    private void asyncServiceReload(String dataset, Map<String, List<Service>> providerServiceLink) throws ConstellationException {
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
    
    private MutableStyle parseStyle(Path path) {
        String fileName = path.getFileName().toString();
        String styleName = IOUtilities.filenameWithoutExtension(fileName);


        MutableStyle style = (MutableStyle) styleBusiness.parseStyle(styleName, path, fileName);

        if (style == null) {
            LOGGER.log(Level.WARNING, "Failed to import style from file: {0}", fileName);
        }
        return style;
    }

    private Integer importStyle(MutableStyle style) {
        try {
            String type = "sld";
            final boolean exists = styleBusiness.existsStyle(type, style.getName());
            if (!exists) {
                return styleBusiness.createStyle(type, style);
            } else {
                LOGGER.log(Level.WARNING, "Duplicated style:{0}", style.getName());
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing style: " + style.getName(), ex);
        }
        return null;
    }
}
