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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IFileSystemStartupCleanerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.metadata.Metadata;
import org.constellation.exception.ConstellationException;
import org.constellation.repository.MetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@Profile("fsconfig")
public class FileSystemStartupCleanerBusiness implements IFileSystemStartupCleanerBusiness {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    @Autowired
    private IServiceBusiness serviceBusiness;
    
    @Autowired
    private IProviderBusiness providerBusiness;
    
    @Autowired
    private IDatasetBusiness datasetBusiness;
    
    @Autowired
    private IStyleBusiness styleBusiness;
    
    @Autowired
    private MetadataRepository metadataRepository;
    
    @Autowired
    private IDatasourceBusiness datasourceBusiness;
    
    @PostConstruct
    public void initFsConfiguration() {
        cleanupDatas();
    }
    
    @Override
    public void cleanupDatas() {
        LOGGER.info("""
                    
                    -----------------------------------------------------------
                    -- STARTING FILESYSTEM DATA REMOVAL                      --
                    -----------------------------------------------------------
                    """);
        try {
            // clear previous configuration
            serviceBusiness.deleteAll();
            
            // we need to put on a special case for fs metadata provider in order to not delete the metatadata files
            for (ProviderBrief pb : providerBusiness.getProviders()) {
                if ("metadata-store".equals(pb.getImpl()) && pb.getConfig().contains("FilesystemMetadata")) {
                    for (Metadata m : metadataRepository.findByProviderId(pb.getId(), null)) {
                        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                                metadataRepository.delete(m.getId());
                            }
                        });
                    }
                }
            }
            
            providerBusiness.removeAll();
            datasetBusiness.removeAllDatasets();
            styleBusiness.deleteAll();
            datasourceBusiness.deleteAll();
        
        } catch (ConstellationException ex) {
            LOGGER.log(Level.SEVERE, "Error a filesystem configuration startup", ex);
        }
    }
}
