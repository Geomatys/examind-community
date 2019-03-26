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
package org.constellation.metadata.core;

import java.util.List;
import java.util.Map;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.DataSourceType;
import org.constellation.filter.FilterParser;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.metadata.harvest.CatalogueHarvester;
import org.constellation.metadata.index.IndexSearcher;
import org.constellation.metadata.index.Indexer;
import org.geotoolkit.metadata.MetadataIoException;
import org.constellation.metadata.security.MetadataSecurityFilter;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.index.IndexingException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IndexConfigHandler {
    
    CatalogueHarvester getCatalogueHarvester(final Automatic configuration, final MetadataStore store) throws MetadataIoException;
    
    Indexer getIndexer(final Automatic configuration, final MetadataStore mdStore, final String serviceID) throws IndexingException, ConfigurationException ;
    
    IndexSearcher getIndexSearcher(final Automatic configuration, final String serviceID) throws IndexingException, ConfigurationException;
    
    FilterParser getFilterParser(final Automatic configuration) throws ConfigurationException ;
    
    FilterParser getSQLFilterParser(final Automatic configuration) throws ConfigurationException ;
    
    MetadataSecurityFilter getSecurityFilter();
    
    Map<String, List<String>> getBriefFieldMap(final Automatic configuration);
    
    MarshallerPool getMarshallerPool();
    
    String getTemplateName(final String metaID, final String type);
    
    List<DataSourceType> getAvailableDatastourceType();
    
    void refreshIndex(final Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws IndexingException, ConfigurationException ;
}
