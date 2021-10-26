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

package org.constellation.metadata.index.elasticsearch;

import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConfigurationException;
import org.constellation.filter.ElasticSearchFilterParser;
import org.constellation.filter.FilterParser;
import org.constellation.filter.SQLFilterParser;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.metadata.index.IndexProvider;
import org.constellation.metadata.index.IndexSearcher;
import org.constellation.metadata.index.Indexer;
import org.constellation.store.metadata.AbstractCstlMetadataStore;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataStore;
import org.springframework.stereotype.Component;

/**
 * @author Quentin Boileau (Geomatys)
 */
@Component(value = "es-node")
public class ElasticSearchIndexProvider implements IndexProvider {

    private static final Logger LOGGER = Logging.getLogger("ElasticSearchIndexProvider");

    public static final String INDEX_TYPE = "es-node";

    public static final String ES_URL_PARAM = "es-url";
    public static final String ES_PORT_PARAM = "es-port";
    public static final String ES_USER_PARAM = "es-user";
    public static final String ES_PWD_PARAM = "es-pwd";
    public static final String ES_SCHEME_PARAM = "es-scheme";

    @Override
    public String indexType() {
        return INDEX_TYPE;
    }

    @Override
    public Indexer getIndexer(Automatic configuration, MetadataStore mdStore, String serviceID) throws ConfigurationException {
        String host = configuration.getParameter(ES_URL_PARAM);
        String scheme = configuration.getParameter(ES_SCHEME_PARAM);
        String portStr = configuration.getParameter(ES_PORT_PARAM);
        int port = 9200;
        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Unable to parse elasticsearch port value:" + portStr);
            }
        }
        String user = configuration.getParameter(ES_USER_PARAM);
        String pwd = configuration.getParameter(ES_PWD_PARAM);
        try {
            return new ElasticSearchNodeIndexer(mdStore, host, port, scheme, user, pwd, serviceID, ((AbstractCstlMetadataStore)mdStore).getAdditionalQueryable(), true);
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        }
    }

    @Override
    public IndexSearcher getIndexSearcher(Automatic configuration, String serviceID) throws ConfigurationException {
        String host = configuration.getParameter(ES_URL_PARAM);
        String scheme = configuration.getParameter(ES_SCHEME_PARAM);
        String portStr = configuration.getParameter(ES_PORT_PARAM);
        int port = 9200;
        if (portStr != null) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                throw new ConfigurationException("Unable to parse elasticsearch port value:" + portStr);
            }
        }
        String user = configuration.getParameter(ES_USER_PARAM);
        String pwd = configuration.getParameter(ES_PWD_PARAM);
        try {
            return new ElasticSearchIndexSearcher(host, port, scheme, user, pwd, serviceID);
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        }
    }

    @Override
    public FilterParser getFilterParser(Automatic configuration) throws ConfigurationException {
        return new ElasticSearchFilterParser(false);
    }

    @Override
    public FilterParser getSQLFilterParser(Automatic configuration) throws ConfigurationException {
        return new SQLFilterParser();
    }

    @Override
    public boolean refreshIndex(Automatic configuration, String serviceID, Indexer indexer, boolean asynchrone) throws ConfigurationException {
        try {
            // TODO asynchrone refresh can't be performed for now
            if (asynchrone) {
                LOGGER.warning("asynchrone refresh can't be performed for now. performing in synchroneous way");
            }
            if (indexer.destroyIndex()) {
                indexer.createIndex();
                return true;
            } else {
                throw new ConfigurationException("Unable to destroy the ES index");
            }
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        }
    }
}
