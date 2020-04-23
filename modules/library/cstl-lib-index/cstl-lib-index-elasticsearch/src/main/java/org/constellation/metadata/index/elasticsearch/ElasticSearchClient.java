package org.constellation.metadata.index.elasticsearch;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.constellation.exception.ConfigurationException;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import static org.constellation.metadata.index.elasticsearch.SpatialFilterBuilder.*;
import org.elasticsearch.ElasticsearchException;
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
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.opengis.geometry.Envelope;

/**
 *
 * @author rmarechal
 */
public class ElasticSearchClient {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.index.elasticsearch");

    private final Settings _configuration;

    private RestHighLevelClient client;

    private final String id;

    // Constructors ----------------------------------------------------------------
    private ElasticSearchClient(final String id, final Properties configuration) {
        this.id = id;
        Builder builder = Settings.builder();
        for (Entry entry : configuration.entrySet()) {
            builder.put((String)entry.getKey(), (String)entry.getValue());
        }
        this._configuration = builder.build();
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

    public void startClient(String host) throws ElasticsearchException, UnknownHostException {
        // on startup
        this.client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(host, 9200, "http")));
    }

    public RestHighLevelClient getClient() {
        if (client != null) {
            return client;
        }
        return null;
    }

    public boolean setLogLevel() throws IOException {
        Map map = new HashMap();
        map.put("logger._root", "TRACE");
        ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();
        request.transientSettings(map);
        ClusterUpdateSettingsResponse response = client.cluster().putSettings(request, RequestOptions.DEFAULT);
        return response.isAcknowledged();
    }

    public boolean prepareType(String indexName, Map<String, Class> fields) throws IOException {
        final boolean exist = indexExist(indexName);
        if (!exist) {
            LOGGER.log(Level.INFO, "creating type for index: " + indexName);
            final XContentBuilder obj1 = getMappingsByJson(fields);
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
        return response.getVersion() > 1;
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
        return response.getVersion() > 1;
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

    public Map generateMapEnvelopes(final String SpatialType, final String CRSNameCode, final List<Double> minx, final List<Double> maxx, final List<Double> miny, final List<Double> maxy) {

        final int nbEnv = minx.size();
        final Map map   = new HashMap();

        final List<Map> envelopeList = new ArrayList<>();

        for (int e = 0; e < nbEnv; e++) {
            if (!Double.isNaN(minx.get(e)) && !Double.isNaN(maxx.get(e)) && !Double.isNaN(miny.get(e)) && !Double.isNaN(maxy.get(e))) {
                Map boxMap = new HashMap();
                boxMap.put("minx", ""+ minx.get(e));
                boxMap.put("maxx", ""+ maxx.get(e));
                boxMap.put("miny", ""+ miny.get(e));
                boxMap.put("maxy", ""+ maxy.get(e));
                boxMap.put("crs",  CRSNameCode);
                envelopeList.add(boxMap);
            } else {
                LOGGER.warning("Nan coordinates in bbox");
            }
        }

        map.put(SpatialType, envelopeList);

        return map;
    }

    public SearchHit[] spatialSearch(final String index, final String spatialType, final String crsName,
                                  final double minx, final double miny,
                                  final double maxx, final double maxy,
                                  final int limit, final String filterType) throws IOException {
        return spatialSearch(index, spatialType, crsName, minx, miny, maxx, maxy, limit, filterType, null, null);
    }

    public SearchHit[] spatialSearch(final String index, final String spatialType, final String crsName,
                                  final double minx, final double miny,
                                  final double maxx, final double maxy,
                                  final int limit, final String filterType, final Double distance, final String unit) throws IOException {

        final QueryBuilder filter = QueryBuilders.wrapperQuery(addSpatialFilter(filterType, spatialType, crsName, minx, maxx, miny, maxy, distance, unit).toString());
        return search(index, null, null, filter, -1, limit, null, null);
    }

    public SearchHit[] spatialSearch(final String index, final String spatialType, final String filterType, final String crsName,
                                  final int limit, final Double ...coord) throws IOException {
        return spatialSearch(index, spatialType, filterType, crsName, limit, null, null, coord);
    }

    public SearchHit[] spatialSearch(final String index, final String spatialType, final String filterType, final String crsName,
                                  final int limit, final Double distance, final String unit, final Double ...coord) throws IOException {

        final QueryBuilder filter = QueryBuilders.wrapperQuery(addSpatialFilter(filterType, spatialType, crsName, distance, unit, coord).toString());
        return search(index, null, null, filter, -1, limit, null, null);
    }

    public SearchHit[] spatialSearch(final String index, final String spatialType, final String crsName,
                                  final double x, final double y,
                                  final int limit, final String filterType) throws IOException {
        return spatialSearch(index, spatialType, crsName, x, y, limit, filterType, null, null);
    }

    public SearchHit[] spatialSearch(final String index, final String spatialType, final String crsName,
                                  final double x, final double y,
                                  final int limit, final String filterType, final Double distance, final String unit) throws IOException {

        final QueryBuilder filter = QueryBuilders.wrapperQuery(addSpatialFilter(filterType, spatialType, x, y, crsName, distance, unit).toString());
        return search(index, null, null, filter, -1, limit, null, null);
    }

    public SearchHit[] search(final String index, final String queryJson, final QueryBuilder query, final QueryBuilder filter, final int start, final int limit, final String type,final Sort sort) throws IOException {
       /* SearchRequestBuilder builder = client.prepareSearch(index)
                                             .setSearchType(SearchType.DEFAULT);*/

        SearchSourceBuilder builder = new SearchSourceBuilder();

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
        if (type != null) {
           // builder = builder.setTypes(type); type will be removed from ES
        }
        if (sort != null) {
            builder = builder.sort(sort.getField(), SortOrder.valueOf(sort.getOrder()));
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

    public SearchHit[] StringSearch(final String index, final String term, final String value, final int limit) throws IOException {
        return search(index, null, QueryBuilders.termQuery(term, value), null, -1, limit, null, null);
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
        return search(index, null, queryBuilder, filterBuilder, -1, limit, null, sort);
    }

    public SearchHit[] searchAll(final String index, final int limit) throws IOException {
        return search(index, null, QueryBuilders.matchAllQuery(), null, -1, limit, null, null);
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
    private static XContentBuilder getMappingsByJson(Map<String, Class> fields) throws IOException {
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
                LOGGER.warning("Envelope are no longer supported (no plugin)");
                /*builder.startObject(entry.getKey())
                       .field("type", "bbox")
                       .endObject();*/
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

    public static synchronized ElasticSearchClient getClientInstance(String host, String clusterName) throws UnknownHostException, ElasticsearchException {
        final String key = host + clusterName;
        ElasticSearchClient instance = CLIENT_INSTANCE.get(key);
        if (instance == null) {
            Properties properties = new Properties();
            properties.put("cluster.name", clusterName);
             properties.put("client.transport.sniff", "true");
            instance = new ElasticSearchClient(key, properties);
            instance.startClient(host);
            CLIENT_INSTANCE.put(key, instance);
            CLIENT_COUNTER.put(key, new AtomicInteger());
        }
        CLIENT_COUNTER.get(key).incrementAndGet();
        return instance;
    }

    public static synchronized void releaseClientInstance(String host, String clusterName) {
        final String key = host + clusterName;
        if (CLIENT_INSTANCE.containsKey(key)) {
            ElasticSearchClient client = CLIENT_INSTANCE.get(key);
            if (CLIENT_COUNTER.get(key).decrementAndGet() <= 0) {
                CLIENT_INSTANCE.remove(key);
                client.close();
            }
        }
    }

    public static Map<String, Object> getServerInfo(String sourceURL) throws ConfigurationException {

        try {
            final URL source          = new URL(sourceURL);
            final URLConnection conec = source.openConnection();
            conec.setReadTimeout(20000);
            InputStream in = conec.getInputStream();
            InputStreamReader conv = new InputStreamReader(in, "UTF8");


            final StringWriter out = new StringWriter();
            char[] buffer          = new char[1024];
            int size;

            while ((size = conv.read(buffer, 0, 1024)) > 0) {
                out.write(new String(buffer, 0, size));
            }

            //we convert the brut String value into UTF-8 encoding
            String response = out.toString();
            return new ObjectMapper().readValue(response, HashMap.class);
        } catch (IOException ex) {
            throw new ConfigurationException(ex);
        }
    }
}
