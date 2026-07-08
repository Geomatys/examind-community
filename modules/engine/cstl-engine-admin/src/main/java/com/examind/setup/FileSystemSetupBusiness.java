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

import com.examind.setup.data.ComputedProviderHandler;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.examind.dto.fs.Service;
import com.examind.setup.FileSystemAnalysis.ProviderWithPath;
import java.util.HashMap;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.api.PathStatus;
import static org.constellation.api.PathStatus.*;
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
import org.constellation.repository.DataRepository;
import org.geotoolkit.style.MutableStyle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;

import static com.examind.setup.FileSystemUtilities.*;
import static com.examind.setup.DatasourceUtilities.*;
import static com.examind.setup.ProviderUtilities.*;
import static com.examind.setup.ProviderUtilities.ProviderSourceType.*;
import com.examind.setup.data.*;
import java.util.Objects;
import org.constellation.exception.TargetNotFoundException;

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
    public IProviderBusiness providerBusiness;
    
    @Autowired
    public IDatasetBusiness datasetBusiness;
    
    @Autowired
    public IDataBusiness dataBusiness;
    
    @Autowired
    public DataRepository dataRepository;
    
    @Autowired
    public IDatasourceBusiness datasourceBusiness;
    
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
    
    
    private FileSystemAnalysis analyze(boolean async) {
        Path styleDir = configBusiness.getStylesDirectory();
        Path servDir  = configBusiness.getServicesDirectory();
        Path provDir  = configBusiness.getProvidersDirectory();
        return new FileSystemAnalysis(styleDir, servDir, provDir, this::parseStyle, async);
    }
    
    @PostConstruct
    public void initFsConfiguration() {
        boolean execAtStartup = Application.getBooleanProperty(AppProperty.EXA_FS_STARTUP, Boolean.TRUE);
        if (execAtStartup) {
            LOGGER.info("""
                    
                    -----------------------------------------------------------
                    --        STARTING FILESYSTEM CONFIG INSTALLATION        --
                    -----------------------------------------------------------
                    
                    """);
            boolean async = Application.getBooleanProperty(AppProperty.EXA_FS_ASYNC, Boolean.FALSE);
            boolean diffMode = Application.getBooleanProperty(AppProperty.EXA_FS_DIFF, Boolean.FALSE);
            FileSystemAnalysis analysis = analyze(async);
            
            if (diffMode) {
                if (async) {
                    taskExecutor.execute(() -> performDiff(analysis));
                } else {
                    performDiff(analysis);
                }
            } else {
                if (async) {
                    taskExecutor.execute(() -> installDatas(analysis));
                } else {
                    installDatas(analysis);
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
        FileSystemAnalysis analysis = analyze(async);
        performDiff(analysis);
    }
    
    @Override
    public void installDatas(boolean async) {
        FileSystemAnalysis analysis = analyze(async);
        installDatas(analysis);
    }
    
    private void performDiff(FileSystemAnalysis analysis) {
        try {
            
            // 1. install styles
            int dsId = getOrCreateDatasourceForConfigFiles(datasourceBusiness, "stylesFS", analysis.styleDir, FileSystemUtilities::sldFileFilter);
            List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleStyleYamlFile(path, analysis);
            }
            
            // 2. install services with data
            dsId = getOrCreateDatasourceForConfigFiles(datasourceBusiness, "serviceWithDataFS", analysis.serviceDir, FileSystemUtilities::serviceWithDataFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleServiceYamlFile(path, analysis, true);
            }
            
            // 3. install services with data
            dsId = getOrCreateDatasourceForConfigFiles(datasourceBusiness, "serviceFS", analysis.serviceDir, FileSystemUtilities::serviceNoDataFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleServiceYamlFile(path, analysis, false);
            }
            
            // 4. install providers
            dsId = getOrCreateDatasourceForConfigFiles(datasourceBusiness, "providerFS", analysis.providerDir, FileSystemUtilities::regularProviderFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleProviderYamlFile(path, analysis, false);
            }
            
            // 5. install computed providers
            dsId = getOrCreateDatasourceForConfigFiles(datasourceBusiness, "providerComputedFS", analysis.providerDir, FileSystemUtilities::computedProviderFileFilter);
            paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                handleProviderYamlFile(path, analysis, true);
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
    
    
    private void handleStyleYamlFile(DataSourceSelectedPath path, FileSystemAnalysis analysis) throws ConstellationException {
        Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
        MutableStyle style = analysis.styles.get(p.toString());
        
        switch (PathStatus.valueOf(path.getStatus())) {
            case PENDING -> {
                PathStatus newStatus;
                int styleId = -1;
                if (style != null) {
                    styleId = importStyle(style);
                    newStatus = styleId != -1 ?  PathStatus.INTEGRATED : PathStatus.ERROR; // NO DATA?
                } else {
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatusAndProvider(path.getDatasourceId(), path.getPath(), newStatus, styleId);
            }
            case MODIFIED -> {
                PathStatus newStatus;
                if (style != null) {
                    styleBusiness.updateStyle(path.getProviderId(), style.getName(), style);
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
            case INTEGRATED -> {} // do nothing
            case ERROR, NO_DATA -> {} // ???
        }
    }
    
    private void handleServiceYamlFile(DataSourceSelectedPath path, FileSystemAnalysis analysis, boolean withData) throws ConstellationException {
        Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
        Service serv = withData ? analysis.servicesWithData.get(p.toString()) : analysis.services.get(p.toString());
        
        switch (PathStatus.valueOf(path.getStatus())) {
            case PENDING -> {
                PathStatus newStatus;
                int sid = -1;
                if (serv != null) {
                    sid = createService(serv, analysis.async);
                    newStatus = sid != -1 ?  PathStatus.INTEGRATED : PathStatus.ERROR; // NO DATA?
                } else {
                    newStatus = PathStatus.ERROR;
                }
                datasourceBusiness.updatePathStatusAndProvider(path.getDatasourceId(), path.getPath(), newStatus, sid);
            }
            case MODIFIED -> {
                PathStatus newStatus;
                if (serv != null) {
                    int sid = updateService(path.getProviderId(), serv, analysis.async);
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
    
    private void handleProviderYamlFile(DataSourceSelectedPath path, FileSystemAnalysis analysis, boolean computed) {
        try {
            PathStatus status = PathStatus.valueOf(path.getStatus());
            if (status == REMOVED) {
                removeProviders(path.getProviderId());
                datasourceBusiness.removePath(path.getDatasourceId(), path.getPath());
                return;
            }

            Path p = datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
            ProviderWithPath pwp = computed ? analysis.computedProviders.get(p.toString()) : analysis.providers.get(p.toString());

            // file is here but is not valid
            // what to do with the old provider ?
            if (pwp == null) {
                datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), ERROR);
                return;
            }

            FSProviderHandler handler = getHandler(pwp, analysis);
            switch (status) {
                case PENDING -> {
                    Integer dsFileId = handler.createProviders(true);
                    PathStatus newStatus = dsFileId != null ? PathStatus.INTEGRATED : PathStatus.ERROR; // NO DATA?
                    datasourceBusiness.updatePathStatusAndProvider(path.getDatasourceId(), path.getPath(), newStatus, dsFileId);
                }
                case MODIFIED -> {
                    PathStatus newStatus;
                    Integer dsFileId = handler.updateProviders(path.getProviderId(), true);

                    if (dsFileId != null) {
                        datasourceBusiness.updatePathProvider(path.getDatasourceId(), path.getPath(), dsFileId);
                        newStatus = PathStatus.INTEGRATED;
                    } else {
                        // what to do with the old provider(s)? remove it?
                        newStatus = PathStatus.ERROR;
                    }
                    datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
                }
                case INTEGRATED -> {
                    Integer datasourceFileId = path.getProviderId();
                    if (datasourceFileId != null && datasourceFileId != -1) {
                        handler.handleProviderFileChanges(path.getProviderId());
                    }
                }

                case ERROR, NO_DATA -> {} // ???
            }
        }  catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider: {0}\n{1}\n", new Object[]{path.getPath(), ex.getMessage()});
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider: " + path.getPath(), ex);
        }
    }
    
    private void installDatas(FileSystemAnalysis analysis) {
        try {
                    
            // 1. install styles
            for (MutableStyle style : analysis.styles.values()) {
                importStyle(style);
            }
            
            // 2. install services with data
            for (Service service : analysis.servicesWithData.values()) {
                createService(service, analysis.async);
            }
            
            // 3. (Async) install services
            if (analysis.async) {
                for (Service service : analysis.services.values()) {
                    createService(service, analysis.async);
                }
            }
            
            // 4. install regular data
            for (ProviderWithPath provider : analysis.providers.values()) {
                FSProviderHandler handler = getHandler(provider, analysis);
                handler.createProviders(false);
            }
            
            // 5. install computed data that use data created in the previous pass
            for (ProviderWithPath provider : analysis.computedProviders.values()) {
                FSProviderHandler handler = getHandler(provider, analysis);
                handler.createProviders(false);
            }
            
            // 6. (Sync) install services
            if (!analysis.async) {
                for (Service service : analysis.services.values()) {
                    createService(service, analysis.async);
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
    private int createService(Service instance, boolean async) {
        try {
            if (serviceBusiness.getServiceIdentifiers(instance.getType()).contains(instance.getIdentifier())) {
                throw new ConfigurationException("Service identifier: " + instance.getIdentifier() + "(" +  instance.getType() + ") already used");
            }
            
            if ("OPENEO".equalsIgnoreCase(instance.getType())) {
                return createOpenEOServicesFromFile(instance, async);
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
                
                Integer datasourceId = getOrCreateSQLDatasource(datasourceBusiness, instance.getType() + "-" + instance.getIdentifier(), instance.getSource());
                
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
                    publishLayersOnService(col, sid, instance.getType(), async);
                } else {
                    LOGGER.warning("No dataset specified in collection");
                }
            }
            return sid;
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while importing service: {0} {1}\n{2}\n", new Object[]{instance.getType(), instance.getIdentifier(), ex.getMessage()});
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing service: " + instance.getType() + " " + instance.getIdentifier(), ex);
        }
        return -1;
    }
    
    private Integer updateService(Integer serviceId, Service instance, boolean async) throws ConstellationException {
        // for now we do an simple remove/create 
        // TODO update metadata
        // TODO linked files?
        serviceBusiness.delete(serviceId);
        return createService(instance, async);
    }
    
    private void removeProviders(Integer dsFileId) throws ConstellationException {
        if (dsFileId == null || dsFileId == -1) return;
        List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsFileId, Integer.MAX_VALUE);
        for (DataSourceSelectedPath path : paths) {
            Integer pid = path.getProviderId();
            if (pid != null && pid != -1) {
                providerBusiness.removeProvider(pid);
            }
        }
        datasourceBusiness.delete(dsFileId);
    }

    /**
     * OpenEO runs on a WPS (process part) and a WCS (STAC/data part) sharing the same identifier.
     * Expand a single "OPENEO" filesystem entry into that WPS + WCS pair instead of requiring
     * both services to be declared and wired manually in the UI.
     *
     * @param instance OpenEO service configuration.
     */
    private Integer createOpenEOServicesFromFile(Service instance, boolean async) {
        Service wps = new Service(instance.getIdentifier(), "WPS", instance.getMetadata());
        wps.setProcessFactories(instance.getProcessFactories());
        
        int wpsId = createService(wps, async);

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
        return createService(wcs, async);
    }

    /**
     * Publish data on a service.
     * 
     * @param col Collection configuration.
     * @param serviceId Service identifier.
     * @param serviceType Service Type.
     * @throws ConstellationException 
     */
    private void publishLayersOnService(Collection col, int serviceId, String serviceType, boolean async) throws ConstellationException {
        Integer styleId = (col.getDatasetStyle() != null) ? styleBusiness.getStyleId("sld", col.getDatasetStyle()) : null;
        List<Data> datas = getDataFromCollection(col, async);

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
                    } catch (TargetNotFoundException ex) {
                        LOGGER.log(Level.WARNING, "Error while linking style : " + custom.getStyle() + " for data: " + data.getName(), ex);
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
    public List<Data> getDataFromCollection(Collection col, boolean async) throws ConstellationException {
        Integer dsId  = col.getDataSet() != null ? datasetBusiness.getDatasetId(col.getDataSet()) : null;
        
        if (col.getDataSet() != null && dsId == null) {
            if (!async) {
                LOGGER.log(Level.WARNING, "Unable to find a dataset: {0}", new Object[]{col.getDataSet()});
            }
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
                    if (!async) {
                        LOGGER.log(Level.WARNING, "No data found for:\ndataset: {0}\nname: {1}\nnamespace:{2}", new Object[]{col.getDataSet(), it.getName(), it.getNamespace()});
                    }
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
    
    public void asyncServiceReload(String dataset, Map<String, List<Service>> asyncInfos) throws ConstellationException {
        List<Service> services = asyncInfos.getOrDefault(dataset, new ArrayList<>());
        for (Service service : services) {
            Integer sid = serviceBusiness.getServiceIdByIdentifierAndType(service.getType(), service.getIdentifier());
            if (sid != null) {
                Collection collection = service.getCollection(dataset);
                if (collection != null) {
                    publishLayersOnService(collection, sid, service.getType(), true);
                    serviceBusiness.restart(sid);
                } else {
                    LOGGER.log(Level.WARNING, "unable to find a collection with dataset {0} in service ({1}) {2}", new Object[]{dataset, service.getType(), service.getIdentifier()});
                }
            } else {
                LOGGER.log(Level.WARNING, "unable to find a service ({0}) {1}", new Object[]{service.getType(), service.getIdentifier()});
            }
        }
    }
    
    private FSProviderHandler getHandler(ProviderWithPath pwp, FileSystemAnalysis analysis) {
        return switch (pwp.sourceType) {
            case CSQL      -> new CSQLProviderhandler(pwp,     analysis.asyncInfos, this);
            case COMPUTED  -> new ComputedProviderHandler(pwp, analysis.asyncInfos, this);
            case FILE      -> new FileProviderHandler(pwp,     analysis.asyncInfos, this);
            case OTHER     -> new OtherProviderHandler(pwp,    analysis.asyncInfos, this);
        }; 
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
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while importing style: " + style.getName(), ex);
        }
        return null;
    }
}
