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

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.constellation.api.ProviderType;
import org.constellation.business.IMetadataBusiness;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.ProviderParameters;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataConfigurationUpgrade {

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private IClusterBusiness clusterBusiness;

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.legacy");

    public MetadataConfigurationUpgrade() {
        SpringHelper.injectDependencies(this);
    }

    public void upgradeConfiguration(final Integer id, final String identifier) throws ConfigurationException {

        Lock lock = clusterBusiness.acquireLock("upgrade-csw-configuration");
        lock.lock();
        LOGGER.fine("LOCK Acquired on cluster: upgrade-csw-configuration");

        try {
            final Object object = serviceBusiness.getConfiguration(id);
            if (object instanceof Automatic) {
                final Automatic config = (Automatic) object;

                if (config.getFormat() != null) {

                    LOGGER.info("-- UPGRADING CSW CONFIGURATION -- ");

                    /**
                     * A little different behavior between FS/internal provider (partial case).
                     *  - for FS provider, we want to start with all the provider metadata linked.
                     *  - for internal provider, we want to start with none.
                     */
                    final boolean linkAllProviderMeta;
                    final Integer providerID;
                    if (FILESYSTEM.getName().equals(config.getFormat())) {
                        linkAllProviderMeta = true;
                        Integer candidatePID = null;
                        // Look for an already existing provider
                        for (ProviderBrief sp : providerBusiness.getProviders()) {
                            try {
                                final DataProvider sdp = DataProviders.getProvider(sp.getId());
                                if (sdp != null) {
                                    if (sdp.getMainStore() instanceof FileSystemMetadataStore) {
                                        final ParameterValueGroup fconfig = sdp.getSource().groups("choice").stream()
                                                .flatMap(choice -> choice.groups("FilesystemMetadata").stream())
                                                .findFirst()
                                                .orElse(null);
                                        if (fconfig != null) {
                                            final Object dataDir = fconfig.parameter("folder").getValue();
                                            if (dataDir != null && dataDir.equals(config.getDataDirectory())) {
                                                candidatePID = sp.getId();
                                                LOGGER.info("Found a previous FS Metadata provider matching");
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    LOGGER.warning("Unable to find a DataProvider for ID:" + sp.getId());
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
                            }
                        }

                        if (candidatePID == null) {
                            final String providerIdentifier = "csw-" + identifier + "-" + UUID.randomUUID().toString();
                            final DataProviderFactory factory = DataProviders.getFactory("metadata-store");
                            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
                            sourcef.parameter("id").setValue(providerIdentifier);

                            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
                            final ParameterValueGroup fsConfig = choice.addGroup("FilesystemMetadata");
                            fsConfig.parameter("folder").setValue(config.getDataDirectory());
                            fsConfig.parameter("store-id").setValue(providerIdentifier);

                            providerID = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "metadata-store", sourcef);
                            try {
                                providerBusiness.createOrUpdateData(providerID, null, false, false, null);
                            } catch (ConstellationException ex) {
                                throw new ConfigurationException(ex);
                            }
                        } else {
                            providerID = candidatePID;
                        }
                    } else if (INTERNAL.getName().equals(config.getFormat())) {
                        linkAllProviderMeta = false;
                        providerID = metadataBusiness.getDefaultInternalProviderID();
                    } else {
                        return;
                    }
                    config.setFormat(null);
                    SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            try {
                                serviceBusiness.setConfiguration(id, config);
                                serviceBusiness.linkCSWAndProvider(id, providerID, linkAllProviderMeta);
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
