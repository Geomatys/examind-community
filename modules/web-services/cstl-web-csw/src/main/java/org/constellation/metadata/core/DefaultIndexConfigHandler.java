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

import org.apache.sis.xml.MarshallerPool;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.DataSourceType;
import org.constellation.filter.FilterParser;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.metadata.harvest.ByIDHarvester;
import org.constellation.metadata.harvest.CatalogueHarvester;
import org.constellation.metadata.harvest.DefaultCatalogueHarvester;
import org.constellation.metadata.harvest.FileSystemHarvester;
import org.constellation.metadata.index.IndexProvider;
import org.constellation.metadata.index.IndexSearcher;
import org.constellation.metadata.index.Indexer;
import org.constellation.metadata.security.MetadataSecurityFilter;
import org.constellation.metadata.security.NoMetadataSecurityFilter;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.constellation.dto.service.config.generic.Automatic.*;

/**
 * Sub project can override this bean by adding a new Bean implementing IndexConfigHandler
 * and named "indexConfigHandler"
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("indexConfigHandler")
public class DefaultIndexConfigHandler implements IndexConfigHandler{

    @Autowired
    private Map<String, IndexProvider> providers;

    @Override
    public CatalogueHarvester getCatalogueHarvester(final Automatic configuration, final MetadataStore store) throws MetadataIoException {
        int type = -1;
        if (configuration != null) {
            type = configuration.getHarvestType();
        }
        switch (type) {
            case DEFAULT:
                return new DefaultCatalogueHarvester(store);
            case FILESYSTEM:
                return new FileSystemHarvester(store);
            case BYID:
                return new ByIDHarvester(store, configuration.getIdentifierDirectory());
            default:
                throw new IllegalArgumentException("Unknow harvester type: " + configuration.getHarvestType() + ".");
        }
    }

    @Override
    public Indexer getIndexer(final Automatic configuration, final MetadataStore mdStore, final String serviceID) throws IndexingException, ConfigurationException {

        String indexType = configuration.getIndexType();
        IndexProvider indexProvider = providers.get(indexType);
        if (indexProvider != null) {
            return indexProvider.getIndexer(configuration, mdStore, serviceID);
        } else {
            throw new ConfigurationException("unexpected Datasource type: can't find proper indexer \""+ indexType +"\"");
        }
    }

    @Override
    public IndexSearcher getIndexSearcher(final Automatic configuration, final String serviceID) throws IndexingException, ConfigurationException {

        String indexType = configuration.getIndexType();
        IndexProvider indexProvider = providers.get(indexType);
        if (indexProvider != null) {
            return indexProvider.getIndexSearcher(configuration, serviceID);
        } else {
            throw new ConfigurationException("unexpected Datasource type: can't find proper index searcher \""+ indexType +"\"");
        }
    }
    
    @Override
    public void refreshIndex(final Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws IndexingException, ConfigurationException {

        String indexType = configuration.getIndexType();
        IndexProvider indexProvider = providers.get(indexType);
        if (indexProvider != null) {
            indexProvider.refreshIndex(configuration, serviceID, indexer, asynchrone);
        } else {
            throw new ConfigurationException("unexpected Datasource type: can't find proper index provider \""+ indexType +"\"");
        }
    }

    @Override
    public FilterParser getFilterParser(final Automatic configuration) throws ConfigurationException {

        String indexType = configuration.getIndexType();
        IndexProvider indexProvider = providers.get(indexType);
        if (indexProvider != null) {
            return indexProvider.getFilterParser(configuration);
        } else {
                throw new ConfigurationException("unexpected Datasource type: can't find proper filter parser \""+ indexType +"\"");
        }
    }

    @Override
    public FilterParser getSQLFilterParser(final Automatic configuration) throws ConfigurationException {

        String indexType = configuration.getIndexType();
        IndexProvider indexProvider = providers.get(indexType);
        if (indexProvider != null) {
            return indexProvider.getSQLFilterParser(configuration);
        } else {
            throw new ConfigurationException("unexpected Datasource type: can't find proper filter parser \""+ indexType +"\"");
        }
    }

    @Override
    public MetadataSecurityFilter getSecurityFilter() {
        return new NoMetadataSecurityFilter();
    }
    
    @Override
    public Map<String, List<String>> getBriefFieldMap(final Automatic configuration) {
        return CSWConstants.ISO_BRIEF_FIELDS;
    }
    
    @Override
    public MarshallerPool getMarshallerPool() {
        return EBRIMMarshallerPool.getInstance();
    }
    
    @Override
    public String getTemplateName(String metaID, String type) {
        final String templateName;
        if("vector".equalsIgnoreCase(type)){
            //vector template
            templateName="profile_default_vector";
        }else if ("raster".equalsIgnoreCase(type)){
            //raster template
            templateName="profile_default_raster";
        } else {
            //default template is import
            templateName="profile_import";
        }
        return templateName;
    }

    @Override
    public List<DataSourceType> getAvailableDatastourceType() {
        return Arrays.asList(DataSourceType.FILESYSTEM, DataSourceType.INTERNAL, DataSourceType.NETCDF);
    }
}
