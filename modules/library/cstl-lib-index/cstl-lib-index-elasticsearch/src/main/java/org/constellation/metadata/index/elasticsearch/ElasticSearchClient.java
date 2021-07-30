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

import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse.Result;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.opengis.geometry.Envelope;

/**
 *
 * @author rmarechal
 */
public class ElasticSearchClient {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.index.elasticsearch");

    private RestHighLevelClient client;

    private final String id;

    // Constructors ----------------------------------------------------------------
    private ElasticSearchClient(final String id) {
        this.id = id;
    }

    protected ClusterHealthStatus getHealthStatus() throws IOException {
        ClusterHealthRequest request = new ClusterHealthRequest();
        return client.cluster().health(request, RequestOptions.DEFAULT).getStatus();
    }

    protected void checkServerStatus() throws IOException {
        ClusterHealthStatus status = getHealthStatus();

        // Check the current status of the ES cluster.
        if (ClusterHealthStatus.RED.equals(status)) {
            LOGGER.log(Level.INFO, "ES cluster status is {0}. Waiting for ES recovery.", status);

            // Waits at most 30 seconds to make sure the cluster health is at least yellow.
            ClusterHealthRequest request = new ClusterHealthRequest();
            request.waitForYellowStatus();
            request.timeout("60s");
            client.cluster().health(request, RequestOptions.DEFAULT);
        }

        // Check the cluster health for a final time.
        status = getHealthStatus();
        LOGGER.log(Level.INFO, "ES cluster status is {0}", status);

        // If we are still in red status, then we cannot proceed.
        if (ClusterHealthStatus.RED.equals(status)) {
            throw new RuntimeException("ES cluster health status is RED. Server is not able to start.");
        }
    }

    public void startClient(String host, int port, String scheme, String user, String pwd) throws ElasticsearchException, UnknownHostException {
        // on startup
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
        if (user != null && pwd != null) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, pwd));
            builder = builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(
                        HttpAsyncClientBuilder httpClientBuilder) {
                    return httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider);
                }
            });
        }
        this.client = new RestHighLevelClient(builder);
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public boolean setLogLevel() throws IOException {
        Map map = new HashMap();
        map.put("logger._root", "TRACE");
        ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();
        request.transientSettings(map);
        ClusterUpdateSettingsResponse response = client.cluster().putSettings(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    public boolean prepareType(String indexName, Map<String, Class> fields, boolean withPlugin) throws IOException {
        final boolean exist = indexExist(indexName);
        if (!exist) {
            LOGGER.log(Level.INFO, "creating type for index: " + indexName);
            final XContentBuilder obj1 = getMappingsByJson(fields, withPlugin);
            CreateIndexRequest request = new CreateIndexRequest(indexName).settings(getAnalyzer()).mapping(obj1);
            client.indices().create(request, RequestOptions.DEFAULT);
            return true;
        }
        return false;
    }

    public boolean indexExist(final String indexName) throws IOException {
        final GetIndexRequest request = new GetIndexRequest(indexName);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    public boolean deleteIndex(final String indexName) throws IOException {
        final boolean exist = indexExist(indexName);
        if (exist) {
            final DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            final AcknowledgedResponse delResponse = client.indices().delete(request, RequestOptions.DEFAULT);
            return delResponse.isAcknowledged();
        }
        return true;
    }

    public boolean indexDoc(final String indexName, final String id, final Map values) throws IOException {
        IndexRequest request = new IndexRequest(indexName).id(id).source(values).setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
        return Result.CREATED.equals(response.getResult()) || Result.UPDATED.equals(response.getResult());
    }

    public boolean indexDoc(final String indexName,   final String id,
                            final String SpatialType, final String CRSNameCode,
                            final int spaceDim,       final double ...coordinates) throws IOException {
        final Map values = generateMapEnvelopes(SpatialType, CRSNameCode, spaceDim, coordinates);
        return indexDoc(indexName, id, values);
    }

    public boolean removeDoc(final String indexName, final String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id).setRefreshPolicy(RefreshPolicy.IMMEDIATE);
        final DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        return Result.DELETED.equals(response.getResult());
    }

    public void removeDocAll(final String indexName) throws IOException {
        final SearchHit[] hits = searchAll(indexName, Integer.MAX_VALUE);
        for (SearchHit hit : hits) {
            removeDoc(indexName, hit.getId());
        }
    }

    public Map generateMapEnvelopes(final String SpatialType, final String CRSNameCode,
                                    final int spaceDim, final double ...coordinates) {
        assert coordinates.length % spaceDim == 0;

        if (spaceDim != 2) {
            throw new IllegalStateException("not implemented yet");
        }

        final int nbEnv = coordinates.length / (spaceDim << 1);

        Map map = new HashMap();

        final List<Map> envelopeList = new ArrayList<>();

        for (int e = 0; e < nbEnv; e++) {
            Map boxMap = new HashMap();
            boxMap.put("minx", ""+coordinates[e * (spaceDim << 1)]);
            boxMap.put("maxx", ""+coordinates[e * (spaceDim << 1) + spaceDim]);
            boxMap.put("miny", ""+coordinates[e * (spaceDim << 1) + 1]);
            boxMap.put("maxy", ""+coordinates[e * (spaceDim << 1) + spaceDim + 1]);
            boxMap.put("crs",  CRSNameCode);

            envelopeList.add(boxMap);
        }

        map.put(SpatialType, envelopeList);

        return map;
    }

   public SearchHit[] search(final String index, final String queryJson, final QueryBuilder query,
           final QueryBuilder filter, final int start, final int limit, final Sort sort,
           final boolean fetchSource, final List<String> fields) throws IOException {

        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder = builder.fetchSource(fetchSource);

        if (queryJson != null) {
            builder = builder.query(QueryBuilders.wrapperQuery(queryJson));
        } else if (query != null) {
            builder = builder.query(query);
        }
        if (filter != null) {
            builder = builder.postFilter(filter);
        }
        if (start != -1) {
            builder = builder.from(start);
        }
        if (sort != null) {
            builder = builder.sort(sort.getField(), SortOrder.valueOf(sort.getOrder()));
        }
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields) {
                builder = builder.docValueField(field);
            }
        }
        if (limit < 10000) {
            builder = builder.size(limit);
            SearchRequest sRequest = new SearchRequest(index);
            sRequest.source(builder);
            final SearchResponse response = client.search(sRequest, RequestOptions.DEFAULT);
            return response.getHits().getHits();
        } else {
            List<SearchHit> results = new ArrayList<>();

            //Scroll until no hits are returned
            builder = builder.size(10000);

            SearchRequest sRequest = new SearchRequest(index);
            sRequest.scroll(new TimeValue(60000));
            sRequest.source(builder);

            SearchResponse response = client.search(sRequest, RequestOptions.DEFAULT);

            boolean more;
            do {
                for (SearchHit hit : response.getHits().getHits()) {
                    results.add(hit);
                }
                SearchScrollRequest request = new SearchScrollRequest(response.getScrollId());
                request.scroll(new Scroll(new TimeValue(60000)));

                response = client.scroll(request, RequestOptions.DEFAULT);
                //Break condition: No hits are returned
                more = response.getHits().getHits().length != 0;

            } while (more);

            return results.toArray(new SearchHit[results.size()]);
        }
    }

    public SearchHit[] search(final String index, final String queryJson, final QueryBuilder query, final QueryBuilder filter, final int start, final int limit, final Sort sort) throws IOException {
        return search(index, queryJson, query, filter, start, limit, sort, true, null);
    }

    public SearchHit[] StringSearch(final String index, final String term, final String value, final int limit) throws IOException {
        return search(index, null, QueryBuilders.termQuery(term, value), null, -1, limit, null);
    }

    public SearchHit[] search(final String index, final String query, final XContentBuilder filter, final Sort sort, final int limit) throws IOException {
        QueryBuilder queryBuilder = null;
        if (query != null) {
            queryBuilder = QueryBuilders.queryStringQuery(query);
        }
        QueryBuilder filterBuilder = null;
        if (filter != null) {
            queryBuilder = QueryBuilders.wrapperQuery(Strings.toString(filter));
        }
        return search(index, null, queryBuilder, filterBuilder, -1, limit, sort);
    }

    public SearchHit[] searchAll(final String index, final int limit) throws IOException {
        return search(index, null, QueryBuilders.matchAllQuery(), null, -1, limit,  null);
    }

    public long count(final String index, final String queryJson, QueryBuilder query, final QueryBuilder filter) throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder();

        if (queryJson != null) {
            builder = builder.query(QueryBuilders.wrapperQuery(queryJson));
        } else if (query != null) {
            builder = builder.query(query);
        }
        if (filter != null) {
            builder = builder.postFilter(filter);
        }

        builder = builder.fetchSource(false);
        CountRequest sRequest = new CountRequest(index);
        sRequest.query(builder.query());
        final CountResponse response = client.count(sRequest, RequestOptions.DEFAULT);
        return response.getCount();
    }

    public List<String> getFieldValues(final String index, final String queryJson, QueryBuilder query, final QueryBuilder filter, String field) throws IOException {
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.aggregation(AggregationBuilders.terms("agg1").field(field).size(10000));
        builder.fetchSource(false);
        builder.size(0);

        if (queryJson != null) {
            builder = builder.query(QueryBuilders.wrapperQuery(queryJson));
        } else if (query != null) {
            builder = builder.query(query);
        }
        if (filter != null) {
            builder = builder.postFilter(filter);
        }

        SearchRequest sRequest = new SearchRequest(index);
        sRequest.source(builder);
        final SearchResponse response = client.search(sRequest, RequestOptions.DEFAULT);
        Terms agg1 = response.getAggregations().get("agg1");
        List<String> results = new ArrayList<>();
        for (Terms.Bucket b : agg1.getBuckets()) {
            results.add(b.getKeyAsString());
        }
        return results;
    }


    private static XContentBuilder getAnalyzer() throws IOException {
        return XContentFactory.jsonBuilder()
                .startObject()
                    .startObject("analysis")
                        .startObject("analyzer")
                            .startObject("lowerCaseAnalyzer")
                                .field("type", "custom")
                                .field("tokenizer", "keyword")
                                .field("filter", new String[]{"lowercase", "asciifolding"})
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private static XContentBuilder getMappingsByJson(Map<String, Class> fields, boolean withPlugin) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject().startObject("properties");
        for (Entry<String, Class> entry : fields.entrySet()) {
            if (entry.getValue() == Date.class) {
                builder.startObject(entry.getKey())
                       .field("type", "date")
                       .field("format", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                       .field("store", "true")
                       .field("doc_values", "false")
                       .endObject();

                builder.startObject(entry.getKey() + "_sort")
                       .field("type", "keyword")
                       .field("store", "true")
                       .field("doc_values", "true")
                       .endObject();
            } else if (entry.getValue() == Double.class) {
                builder.startObject(entry.getKey())
                       .field("type", "double")
                       .field("store", "true")
                       .field("doc_values", "false")
                       .endObject();

                builder.startObject(entry.getKey() + "_sort")
                       .field("type", "double")
                       .field("store", "true")
                       .field("doc_values", "true")
                       .endObject();
            } else if (entry.getValue() == Envelope.class) {
                if (withPlugin) {
                    builder.startObject(entry.getKey())
                           .field("type", "bbox")
                           .endObject();
                } else {
                    builder.startObject(entry.getKey())
                           .field("type", "geo_shape")
                           .endObject();
                }
            } else {
                builder.startObject(entry.getKey())
                       .field("type", "text")
                       .field("store", "true")
                       .field("analyzer", "lowerCaseAnalyzer") //, "geotk-classic") need plugin
                       .field("doc_values", "false")
                       .endObject();

                builder.startObject(entry.getKey() + "_sort")
                       .field("type", "keyword")
                       .field("store", "true")
                       .field("doc_values", "true")
                       .endObject();
            }
        }
// TODO
//
//
//        builder.startObject("lac")
//               .field("type", "bbox")
//               .endObject();

        builder.endObject()
               .endObject();

        LOGGER.log(Level.FINER, "MAPPING:{0}", builder.toString());
        return builder;
    }

    private static final Map<String, ElasticSearchClient> CLIENT_INSTANCE = new HashMap<>();
    private static final Map<String, AtomicInteger> CLIENT_COUNTER = new HashMap<>();

    private void close() {
        LOGGER.info("--- CLOSING ES CLIENT " + id + " ---");
        if (client != null) {
            try {
                client.close();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Error while closing elasticsearch client", ex);
            }
            client = null;
        }
    }

    public static synchronized ElasticSearchClient getClientInstance(String host, int port, String scheme, String user, String pwd) throws UnknownHostException, ElasticsearchException {
        if (scheme == null) {
            scheme = "http";
        }
        final String key = scheme + host + port;
        ElasticSearchClient instance = CLIENT_INSTANCE.get(key);
        if (instance == null) {
            instance = new ElasticSearchClient(key);
            instance.startClient(host, port, scheme, user, pwd);
            CLIENT_INSTANCE.put(key, instance);
            CLIENT_COUNTER.put(key, new AtomicInteger());
        }
        CLIENT_COUNTER.get(key).incrementAndGet();
        return instance;
    }

    public static synchronized void releaseClientInstance(String host, int port, String scheme) {
        if (scheme == null) {
            scheme = "http";
        }
        final String key = scheme + host + port;
        if (CLIENT_INSTANCE.containsKey(key)) {
            ElasticSearchClient client = CLIENT_INSTANCE.get(key);
            if (CLIENT_COUNTER.get(key).decrementAndGet() <= 0) {
                CLIENT_INSTANCE.remove(key);
                client.close();
            }
        }
    }
}