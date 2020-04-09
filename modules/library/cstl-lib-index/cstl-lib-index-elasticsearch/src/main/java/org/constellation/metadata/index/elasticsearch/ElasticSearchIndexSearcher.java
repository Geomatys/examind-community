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

package org.constellation.metadata.index.elasticsearch;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.constellation.metadata.index.IndexSearcher;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.geotoolkit.index.SpatialQuery;
import org.geotoolkit.index.SearchingException;
import org.geotoolkit.index.IndexingException;

/**
 *
 * @author Guilhem Legal (Gematys)
 */
public class ElasticSearchIndexSearcher implements IndexSearcher {

    private final String indexName;

    protected final ElasticSearchClient client;

    private Level logLevel = Level.INFO;

    private final boolean remoteES;

    private final String hostName;

    private final String clusterName;

    public ElasticSearchIndexSearcher(final String host, final String clusterName, final String indexName, final boolean remoteES) throws IndexingException {
        this.indexName   = indexName.toLowerCase();
        this.remoteES    = remoteES;
        this.clusterName = clusterName;
        this.hostName    = host;
        if (remoteES) {
            try {
                this.client = ElasticSearchClient.getClientInstance(host, clusterName);
            } catch (UnknownHostException | ElasticsearchException ex) {
                throw new IndexingException("error while connecting ELasticSearch cluster", ex);
            }
        } else {
            this.client = ElasticSearchClient.getServerInstance(clusterName);
        }

    }

    @Override
    public Set<String> doSearch(SpatialQuery spatialQuery) throws SearchingException {
        final Set<String> results = new LinkedHashSet<>();
        try {
            SearchHit[] resultHits = client.search(indexName, spatialQuery.getTextQuery(), (XContentBuilder) spatialQuery.getQuery(), (Sort)spatialQuery.getSort(), Integer.MAX_VALUE);
            for (int i = 0; i < resultHits.length; i++) {
                results.add(resultHits[i].getId());
            }
        } catch (IOException ex) {
            throw new SearchingException("Error while searching in elasticSearch", ex);
        }
        return results;
    }

    @Override
    public Map<String, Character> getNumericFields() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String identifierQuery(String id) throws SearchingException {
        String result = null;
        try {
            SearchHit[] resultHits = client.StringSearch(indexName, "identifier_sort", id, 1);
            for (int i = 0; i < resultHits.length; i++) {
                 result = resultHits[i].getId();
            }
        } catch (IOException ex) {
            throw new SearchingException("Error while searching in elasticSearch", ex);
        }
        return result;
    }

    @Override
    public void refresh() throws IndexingException {
        // do nothing for now
    }

    @Override
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    @Override
    public void destroy() {
        if (remoteES) {
            ElasticSearchClient.releaseClientInstance(hostName, clusterName);
        } else {
            ElasticSearchClient.releaseServerInstance(clusterName);
        }
    }

}
