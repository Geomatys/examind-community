/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.metadata.index.generic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.exception.ConfigurationException;
import org.constellation.filter.FilterParser;
import org.constellation.filter.LuceneFilterParser;
import org.constellation.metadata.index.IndexProvider;
import org.constellation.metadata.index.IndexSearcher;
import org.constellation.metadata.index.Indexer;
import org.geotoolkit.index.IndexingException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractLuceneIndexProvider implements IndexProvider {

    @Autowired
    protected IConfigurationBusiness configBusiness;

    @Override
    public IndexSearcher getIndexSearcher(Automatic configuration, String serviceID) throws ConfigurationException {
        try {
            final Path instanceDirectory = configBusiness.getInstanceDirectory("csw", serviceID);
            return new LuceneIndexSearcher(instanceDirectory, "", null, true);
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        }
    }

    @Override
    public FilterParser getFilterParser(Automatic configuration) throws ConfigurationException {
        return new LuceneFilterParser();
    }

    @Override
    public boolean refreshIndex(Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws ConfigurationException {
        final Path instanceDir = configBusiness.getInstanceDirectory("csw", serviceID);
        if (Files.exists(instanceDir)) {
            if (asynchrone) {
                final Path nexIndexDir = instanceDir.resolve("index-" + System.currentTimeMillis());
                try {
                    if (indexer != null) {
                        try {
                            Files.createDirectories(nexIndexDir);
                        } catch (IOException e) {
                            throw new ConfigurationException("Unable to create a directory nextIndex for  the id:" + serviceID);
                        }
                        indexer.setFileDirectory(nexIndexDir);
                        indexer.createIndex();
                    } else {
                        throw new ConfigurationException("Unable to create an indexer for the id:" + serviceID);
                    }
                } catch (IllegalArgumentException | IndexingException ex) {
                    throw new ConfigurationException("An exception occurs while creating the index!\ncause:" + ex.getMessage());
                }
                return true;
            } else {
                try {
                    return indexer.destroyIndex();
                } catch (IndexingException ex) {
                    throw new ConfigurationException(ex);
                }
            }
        }
        return false;
    }
}
