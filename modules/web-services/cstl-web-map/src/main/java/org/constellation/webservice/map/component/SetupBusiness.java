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
package org.constellation.webservice.map.component;

import com.examind.provider.component.ExaDataCreator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.parameter.Parameters;

import org.constellation.admin.SpringHelper;
import static org.constellation.api.ProviderConstants.GENERIC_SHAPE_PROVIDER;
import static org.constellation.api.ProviderConstants.GENERIC_TIF_PROVIDER;
import org.constellation.api.ProviderType;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataCoverageJob;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.DataSource;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConfigurationRuntimeException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.constellation.util.SQLUtilities;
import org.constellation.ws.IWSEngine;
import org.constellation.ws.Worker;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.style.DefaultExternalGraphic;
import org.geotoolkit.style.DefaultGraphic;
import org.geotoolkit.style.DefaultOnlineResource;
import org.geotoolkit.style.DefaultPointSymbolizer;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.style.GraphicalSymbol;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static org.geotoolkit.style.StyleConstants.DEFAULT_LINE_SYMBOLIZER;
import static org.geotoolkit.style.StyleConstants.DEFAULT_POINT_SYMBOLIZER;
import static org.geotoolkit.style.StyleConstants.DEFAULT_POLYGON_SYMBOLIZER;
import static org.geotoolkit.style.StyleConstants.DEFAULT_RASTER_SYMBOLIZER;
import org.opengis.style.StyleFactory;

/**
 * Specific setup for map service
 *
 * @author Guilhem Legal (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @author Cédric Briançon (Geomatys)
 */
@Named
//@DependsOn("database-initer")
public class SetupBusiness {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.webservice.map.component");

    private static final String DEFAULT_RESOURCES = "org/constellation/map/setup.zip";

    @Inject
    private IStyleBusiness styleBusiness;

    @Inject
    private IClusterBusiness clusterBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

     @Inject
    private IMapContextBusiness mapContextBusiness;

    @Inject
    private IConfigurationBusiness configurationBusiness;

    @Inject
    private IDatasourceBusiness datasourceBusiness;
    
    @Inject
    private IDataCoverageJob dataCoverageJob;

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private IWSEngine wsEngine;

    @Inject
    private ExaDataCreator dataCreator;

    @PostConstruct
    public void contextInitialized() {
        LOGGER.log(Level.INFO, "=== Initialize Application ===");

        try {
            // Try to load postgresql driver for further use
            Class.forName("org.postgresql.ds.PGSimpleDataSource");
            LOGGER.log(Level.INFO, "postgresql loading success!");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
        }

        Lock lock = clusterBusiness.acquireLock("setup-default-resources");
        lock.lock();
        LOGGER.fine("LOCK Acquired on cluster: setup-default-resources");
        final Path DataDirectory = configurationBusiness.getDataDirectory();
        try {
            SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {

                @Override
                protected void doInTransactionWithoutResult(TransactionStatus arg0) {

                    WithDefaultResources defaultResourcesDeployed = deployDefaultResources(DataDirectory);

                    LOGGER.log(Level.INFO, "initializing default styles ...");
                    defaultResourcesDeployed.initializeDefaultStyles();
                    LOGGER.log(Level.INFO, "initializing vector data ...");
                    defaultResourcesDeployed.initializeDefaultVectorData();
                    LOGGER.log(Level.INFO, "initializing raster data ...");
                    defaultResourcesDeployed.initializeDefaultRasterData();
                    LOGGER.log(Level.INFO, "initializing properties ...");
                    defaultResourcesDeployed.initializeDefaultProperties();

                }
            });
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error while deploying default resources", ex);
        } finally {
            LOGGER.fine("UNLOCK on cluster: setup-default-resources");
            lock.unlock();
        }

        LOGGER.log(Level.INFO, "initializing map context data ...");
        try {
            mapContextBusiness.initializeDefaultMapContextData();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "An error occurred when creating default map context provider.", ex);
        }

        //check if data analysis is required
        boolean doAnalysis = Application.getBooleanProperty(AppProperty.DATA_AUTO_ANALYSE, Boolean.TRUE);
        if (doAnalysis) {
            LOGGER.log(Level.FINE, "Start data analysis");
            dataCoverageJob.computeEmptyDataStatistics(true);
        }

        LOGGER.log(Level.INFO, "initializing filesystems ...");
        try {
            datasourceBusiness.initializeFilesystems();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "An error occurred when initializing filesystems.", ex);
        }

        LOGGER.log(Level.INFO, "initializing default datasource ...");
        try {
            createInternalDatasource();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "An error occurred when initializing default datasource.", ex);
        }

        boolean serviceWarmup = Application.getBooleanProperty(AppProperty.EXA_SERVICE_WARMUP, Boolean.FALSE);
        if (serviceWarmup) {
            long start = System.currentTimeMillis();
            LOGGER.log(Level.INFO, "=== Start Services Warmup ===");
            try {
                for (ServiceComplete sc : serviceBusiness.getAllServices(null)) {
                    try {
                        Worker w = wsEngine.getInstance(sc.getType(), sc.getIdentifier());
                        for (String version : sc.getVersions().split("µ")) {
                            LOGGER.info("Get capabilities on " + sc.getType() + " " + sc.getIdentifier() + " version " + version);
                            w.getCapabilities(version);
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while warming up service:" + sc.getType() + " " + sc.getIdentifier(), ex);
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Error while warming up services", ex);
            }
            LOGGER.log(Level.INFO, "=== Services Warmup Finished in " + (System.currentTimeMillis() - start) + "ms ===");
        }

    }

    /**
     * Invoked when the module needs to be shutdown.
     */
    @PreDestroy
    public void contextDestroyed() {
        DataProviders.dispose();
    }

    /**
     * Record the current sql datasource ine order to use it for further usage.
     * 
     * @throws ConstellationException
     */
    private void createInternalDatasource() throws ConstellationException {
        String fullDbUrl = Application.getProperty(AppProperty.CSTL_DATABASE_URL);
        String[] infos = SQLUtilities.extractUserPasswordUrl(fullDbUrl);
        String dbUrl = infos[0];
        String user  = infos[1];
        String pwd   = infos[2];
        List<DataSource> datasources = datasourceBusiness.search(dbUrl, "NULL", "NULL", user, pwd);
        if (datasources.isEmpty()) {
            DataSource ds = new DataSource(null, "database", dbUrl, user, pwd, null, false, System.currentTimeMillis(), "COMPLETED", null, true);
            datasourceBusiness.create(ds);
        }
    }

    private WithDefaultResources deployDefaultResources(final Path dataDirectory) {
        try {

            Path zipPath = IOUtilities.getResourceAsPath(DEFAULT_RESOURCES);

            Path tempDir = Files.createTempDirectory("");
            Path tempZip = tempDir.resolve(zipPath.getFileName().toString());
            Files.copy(zipPath, tempZip);

            ZipUtilities.unzipNIO(tempZip, dataDirectory, false);

            IOUtilities.deleteRecursively(tempDir);

            return new WithDefaultResources(dataDirectory);
        } catch (IOException | URISyntaxException e) {
            throw new ConfigurationRuntimeException("Error while deploying default ressources", e);
        }
    }

    private class WithDefaultResources {
        
        private final Path dataDirectory;
        
        WithDefaultResources(final Path dataDirectory) {
            this.dataDirectory = dataDirectory;
        }
        /**
         * Initialize default styles for generic data.
         */
        private void initializeDefaultStyles() {

            final Path dstImages = dataDirectory.resolve("images");
            final Path markerNormal = dstImages.resolve("marker_normal.png");
            final Path markerSelected = dstImages.resolve("marker_selected.png");
            try {
                if (!Files.exists(markerNormal)) {
                    IOUtilities.copyResource("org/constellation/map/setup/images", null, dataDirectory, false);
                }
            } catch (URISyntaxException | IOException ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            }

            // Fill default SLD provider.
            final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);


            try {
                //create sld and sld_temp providers

                if (!styleBusiness.existsStyle("sld","default-point")) {
                    final MutableStyle style = SF.style(DEFAULT_POINT_SYMBOLIZER);
                    style.setName("default-point");
                    style.featureTypeStyles().get(0).rules().get(0).setName("default-point");
                    styleBusiness.createStyle("sld", style);
                }

                createPointMarkerStyle(markerNormal, SF, "default-point-sensor");
                createPointMarkerStyle(markerSelected, SF, "default-point-sensor-selected");

                if (!styleBusiness.existsStyle("sld","default-line")) {
                    final MutableStyle style = SF.style(DEFAULT_LINE_SYMBOLIZER);
                    style.setName("default-line");
                    style.featureTypeStyles().get(0).rules().get(0).setName("default-line");
                    styleBusiness.createStyle("sld", style);
                }
                if (!styleBusiness.existsStyle("sld","default-polygon")) {
                    final MutableStyle style = SF.style(DEFAULT_POLYGON_SYMBOLIZER);
                    style.setName("default-polygon");
                    style.featureTypeStyles().get(0).rules().get(0).setName("default-polygon");
                    styleBusiness.createStyle("sld", style);
                }
                if (!styleBusiness.existsStyle("sld","default-raster")) {
                    final MutableStyle style = SF.style(DEFAULT_RASTER_SYMBOLIZER);
                    style.setName("default-raster");
                    style.featureTypeStyles().get(0).rules().get(0).setName("default-raster");
                    styleBusiness.createStyle("sld", style);
                }
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "An error occurred when creating default styles for default SLD provider.", ex);
            }
        }

        private void createPointMarkerStyle(Path markerNormal, MutableStyleFactory SF, String pointNormalStyleName) throws ConfigurationException {
            if (!styleBusiness.existsStyle("sld", pointNormalStyleName)) {
                final MutableStyle style = SF.style(DEFAULT_POINT_SYMBOLIZER);
                style.setName(pointNormalStyleName);
                style.featureTypeStyles().get(0).rules().get(0).setName(pointNormalStyleName);

                // Marker
                String fileName = markerNormal.getFileName().toString();
                final DefaultOnlineResource onlineResource = new DefaultOnlineResource(markerNormal.toUri(), "", "", fileName,
                        null, null);
                final DefaultExternalGraphic graphSymb = (DefaultExternalGraphic) SF.externalGraphic(onlineResource, "png", null);
                final List<GraphicalSymbol> symbs = new ArrayList<>();
                symbs.add(graphSymb);
                final DefaultGraphic graphic = (DefaultGraphic) SF.graphic(symbs, null, null, null, null, null);
                final DefaultPointSymbolizer pointSymbolizer = (DefaultPointSymbolizer) SF.pointSymbolizer(pointNormalStyleName, "",
                        null, null, graphic);
                style.featureTypeStyles().get(0).rules().get(0).symbolizers().clear();
                style.featureTypeStyles().get(0).rules().get(0).symbolizers().add(pointSymbolizer);
                styleBusiness.createStyle("sld", style);
            }
        }

        /**
         * Initialize default vector data for displaying generic features in
         * data editors.
         */
        private void initializeDefaultVectorData() {
            // remove old legacy provider
            Integer pid = providerBusiness.getIDFromIdentifier(GENERIC_SHAPE_PROVIDER);
            if (pid != null) {
                try {
                    providerBusiness.removeProvider(pid);
                } catch (ConstellationException ex) {
                    LOGGER.log(Level.WARNING, "Error while removing old default vector data provider", ex);
                }
            }

            final Path dst = dataDirectory.resolve("shapes");
            createProvider(dst.resolve("CNTR_BN_60M_2006.shp"), GENERIC_SHAPE_PROVIDER + "-linestring", "vector", "shapefile", "path");
            createProvider(dst.resolve("CNTR_LB_2006.shp"),     GENERIC_SHAPE_PROVIDER + "-point",      "vector", "shapefile", "path");
            createProvider(dst.resolve("CNTR_RG_60M_2006.shp"), GENERIC_SHAPE_PROVIDER + "-polygon",    "vector", "shapefile", "path");
        }

        /**
         * Initialize default raster data for displaying generic features in
         * data editors.
         */
        private void initializeDefaultRasterData() {
            final Path dst = dataDirectory.resolve("raster");
            createProvider(dst.resolve("cloudsgrey.tiff"), GENERIC_TIF_PROVIDER, "raster", "GeoTIFF", "location");
        }


        private void createProvider(final Path shapefile, String providerIdentifier, String dataType, String impl, String pathParamName) {
            Integer provider = providerBusiness.getIDFromIdentifier(providerIdentifier);
            if (provider == null) {
                // Acquire provider service instance.
                DataProviderFactory storeService = null;
                for (final DataProviderFactory service : DataProviders.getFactories()) {
                    if (service.getName().equals("data-store")) {
                        storeService = service;
                        break;
                    }
                }
                if (storeService == null) {
                    LOGGER.log(Level.WARNING, "Provider service not found.");
                    return;
                }

                final ParameterValueGroup source = Parameters.castOrWrap(storeService.getProviderDescriptor().createValue());
                source.parameter("id").setValue(providerIdentifier);
                source.parameter("providerType").setValue(dataType);

                final List<ParameterValueGroup> choices = source.groups("choice");
                final ParameterValueGroup choice;
                if (choices.isEmpty()) {
                    choice = source.addGroup("choice");
                } else {
                    choice = choices.get(0);
                }

                final ParameterValueGroup config = choice.addGroup(impl);
                config.parameter(pathParamName).setValue(shapefile.toUri());

                // Create provider and generate data.
                try {
                    final Integer pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
                    providerBusiness.createOrUpdateData(pid, null, false, true, null);
                } catch (ConstellationException ex) {
                    LOGGER.log(Level.WARNING, "An error occurred when creating default provider.", ex);
                }
            }
        }

        /**
         * Initialize default properties values if not exist.
         */
        private void initializeDefaultProperties() {
            //nothing to do for now
        }
    }

}
