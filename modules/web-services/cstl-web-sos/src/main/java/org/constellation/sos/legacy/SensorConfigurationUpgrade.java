/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.sos.legacy;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ProviderType;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.ProviderBrief;
import static org.constellation.dto.service.config.DataSourceType.FILESYSTEM;
import static org.constellation.dto.service.config.DataSourceType.OM2;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.ProviderParameters;
import org.constellation.provider.SensorProvider;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SensorConfigurationUpgrade {

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IClusterBusiness clusterBusiness;

    private static final Logger LOGGER = Logger.getLogger("org.constellation.sos.legacy");

    public SensorConfigurationUpgrade() {
        SpringHelper.injectDependencies(this);
    }

    public void upgradeConfiguration(final Integer id) throws ConfigurationException {

        Lock lock = clusterBusiness.acquireLock("upgrade-sos-confguration");
        lock.lock();
        LOGGER.fine("LOCK Acquired on cluster: upgrade-sos-confguration");

        try {

            final Object object = serviceBusiness.getConfiguration(id);
            if (object instanceof SOSConfiguration) {
                final SOSConfiguration config = (SOSConfiguration) object;

                if (FILESYSTEM.equals(config.getSMLType())) {
                    final Automatic smlConf               = config.getSMLConfiguration();
                    if (smlConf != null) {
                        LOGGER.info("-- UPGRADING SOS SML CONFIGURATION -- ");
                        Integer providerID = null;

                        // Look for an already existing provider
                        for (ProviderBrief sp : providerBusiness.getProviders()) {
                            DataProvider sdp = DataProviders.getProvider(sp.getId());
                            if (sdp instanceof SensorProvider) {
                                final ParameterValueGroup source  = sdp.getSource();
                                final List<ParameterValueGroup> choices = source.groups("choice");
                                if (!choices.isEmpty()) {
                                    final List<ParameterValueGroup> fconfig = choices.get(0).groups("FileSensorParameters");
                                    if (!fconfig.isEmpty()) {
                                        final Object dataDirObj = fconfig.get(0).parameter("data_directory").getValue();
                                        Path dataDir = null;
                                        if (dataDirObj instanceof File) {
                                            dataDir = ((File)dataDirObj).toPath();
                                        } else if (dataDirObj instanceof Path) {
                                            dataDir = (Path)dataDirObj;
                                        } else if (dataDirObj != null) {
                                            // Not known type
                                            throw new ConfigurationException("Unknown value for data_directory configuration");
                                        }
                                        if (dataDir != null && dataDir.equals(smlConf.getDataDirectory())) {
                                            providerID = sp.getId();
                                            LOGGER.info("Found a previous SML provider matching");
                                        }
                                    }
                                }
                            }
                        }

                        if (providerID == null) {
                            final String providerIdentifier = UUID.randomUUID().toString();
                            final DataProviderFactory factory = DataProviders.getFactory("sensor-store");
                            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
                            sourcef.parameter("id").setValue(providerIdentifier);

                            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
                            final ParameterValueGroup fsConfig = choice.addGroup("filesensor");
                            fsConfig.parameter("data_directory").setValue(smlConf.getDataDirectory());

                            providerID = providerBusiness.storeProvider(providerIdentifier, ProviderType.SENSOR, "sensor-store", fsConfig);
                            try {
                                providerBusiness.createOrUpdateData(providerID, null, false, false, null);
                            } catch (ConstellationException ex) {
                                throw new ConfigurationException(ex);
                            }
                        }

                        serviceBusiness.linkServiceAndProvider(id, providerID);
                        config.clearSMLDeprecatedAttibute();
                        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                                try {
                                    serviceBusiness.setConfiguration(id, config);
                                } catch (ConfigurationException ex) {
                                    LOGGER.log(Level.SEVERE, null, ex);
                                }
                            }
                        });
                        LOGGER.info("-- SOS SML CONFIGURATION UPGRADED -- ");
                    }
                }
                if (OM2.equals(config.getObservationReaderType())) {
                    final Automatic omConf = config.getOMConfiguration();
                    if (omConf != null && omConf.getBdd() != null) {
                        LOGGER.info("-- UPGRADING SOS O&M CONFIGURATION -- ");
                        String currentDBName = omConf.getBdd().getDatabaseName();
                        String currentHost   = omConf.getBdd().getHostName();
                        Integer providerID    = null;

                        // Look for an already existing provider
                        for (ProviderBrief sp : providerBusiness.getProviders()) {
                            DataProvider sdp = DataProviders.getProvider(sp.getId());
                            if (sdp instanceof ObservationProvider) {
                                final ParameterValueGroup source  = sdp.getSource();
                                providerID = source.groups("choice").stream()
                                        .flatMap(choice -> choice.groups("SOSDBParameters").stream())
                                        .findFirst()
                                        .map(fconfig -> {
                                            final Object host = fconfig.parameter("host").getValue();
                                            final Object database = fconfig.parameter("database").getValue();
                                            if (host != null && database != null && host.equals(currentHost) && database.equals(currentDBName)) {
                                                LOGGER.info("Found a previous O&M provider matching");
                                                return sp.getId();
                                            }

                                            return null;
                                        })
                                        .orElse(null);
                                if (providerID != null)
                                    break;
                            }
                        }

                        if (providerID == null) {
                            String providerIdentifier = UUID.randomUUID().toString();
                            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
                            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
                            source.parameter("id").setValue(providerIdentifier);
                            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

                            final ParameterValueGroup dbConfig = choice.addGroup("observationSOSDatabase");
                            dbConfig.parameter("sgbdtype").setValue("postgres");
                            dbConfig.parameter("host").setValue(omConf.getBdd().getHostName());
                            dbConfig.parameter("database").setValue(omConf.getBdd().getDatabaseName());
                            dbConfig.parameter("user").setValue(omConf.getBdd().getUser());
                            dbConfig.parameter("password").setValue(omConf.getBdd().getPassword());
                            dbConfig.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
                            dbConfig.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
                            dbConfig.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
                            dbConfig.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");

                            providerID = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
                        }

                        serviceBusiness.linkServiceAndProvider(id, providerID);
                        config.clearOMDeprecatedAttibute();
                        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                                try {
                                    serviceBusiness.setConfiguration(id, config);
                                } catch (ConfigurationException ex) {
                                    LOGGER.log(Level.SEVERE, null, ex);
                                }
                            }
                        });
                        LOGGER.info("-- SOS O&M CONFIGURATION UPGRADED -- ");
                    }
                }
            }
        } finally {
            LOGGER.fine("UNLOCK on cluster: upgrade-sos-confguration");
            lock.unlock();
        }
    }
}
