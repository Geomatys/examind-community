/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.metadata.legacy;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.exception.ConfigurationException;
import static org.constellation.dto.service.config.DataSourceType.FILESYSTEM;
import static org.constellation.dto.service.config.DataSourceType.INTERNAL;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.geotoolkit.storage.DataStores;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataConfigurationUpgrade {

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private IClusterBusiness clusterBusiness;

    private static final Logger LOGGER = Logging.getLogger("org.constellation.metadata.legacy");

    public MetadataConfigurationUpgrade() {
        SpringHelper.injectDependencies(this);
    }

    public void upgradeConfiguration(final String id) throws ConfigurationException {

        Lock lock = clusterBusiness.acquireLock("upgrade-csw-configuration");
        lock.lock();
        LOGGER.fine("LOCK Acquired on cluster: upgrade-csw-configuration");

        try {
            final Object object = serviceBusiness.getConfiguration("csw", id);
            if (object instanceof Automatic) {
                final Automatic config = (Automatic) object;

                if (config.getFormat() != null) {

                    LOGGER.info("-- UPGRADING CSW CONFIGURATION -- ");
                    final String providerID;

                    if (FILESYSTEM.getName().equals(config.getFormat())) {

                        String candidatePID = null;
                        // Look for an already existing provider
                        for (ProviderBrief sp : providerBusiness.getProviders()) {
                            try {
                                final DataProvider sdp = DataProviders.getProvider(sp.getId());
                                if (sdp != null) {
                                    if (sdp.getMainStore() instanceof FileSystemMetadataStore) {
                                        final ParameterValueGroup fconfig = sdp.getSource().groups("choice").stream()
                                                .flatMap(choice -> choice.groups("FSMetadataParameters").stream())
                                                .findFirst()
                                                .orElse(null);
                                        if (fconfig != null) {
                                            final Object dataDir = fconfig.parameter("folder").getValue();
                                            if (dataDir != null && dataDir.equals(config.getDataDirectory())) {
                                                candidatePID = sp.getIdentifier();
                                                LOGGER.info("Found a previous FS Metadata provider matching");
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    LOGGER.warning("Unable to find a DataProvider for ID:" + sp.getId());
                                }
                            } catch (Throwable ex) {
                                LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
                            }
                        }

                        if (candidatePID == null) {
                            providerID = UUID.randomUUID().toString();
                            final DataStoreProvider factory = DataStores.getProviderById("FilesystemMetadata");
                            final ParameterValueGroup params = factory.getOpenParameters().createValue();
                            params.parameter("folder").setValue(config.getDataDirectory());
                            params.parameter("store-id").setValue(providerID);

                            Integer pr = providerBusiness.create(providerID, IProviderBusiness.SPI_NAMES.METADATA_SPI_NAME, params);
                            try {
                                providerBusiness.createOrUpdateData(pr, null, false);
                            } catch (IOException | ConstellationException ex) {
                                throw new ConfigurationException(ex);
                            }
                        } else {
                            providerID = candidatePID;
                        }
                    } else if (INTERNAL.getName().equals(config.getFormat())) {
                        providerID = "default-internal-metadata";
                    } else {
                        return;
                    }
                    config.setFormat(null);
                    SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            try {
                                serviceBusiness.setConfiguration("csw", id, config);
                                serviceBusiness.linkCSWAndProvider(id, providerID);
                            } catch (ConfigurationException ex) {
                                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                            }
                        }
                    });
                    LOGGER.info("-- CSW CONFIGURATION UPGRADED -- ");
                }
            }
        } finally {
            LOGGER.fine("UNLOCK on cluster: upgrade-csw-configuration");
            lock.unlock();
        }
    }
}
