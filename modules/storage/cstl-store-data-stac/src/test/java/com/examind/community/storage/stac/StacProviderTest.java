package com.examind.community.storage.stac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.geotoolkit.client.openapi.OpenApiConfiguration;
import org.geotoolkit.stac.client.StacApi;
import org.geotoolkit.storage.DataStores;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class StacProviderTest {

    private static HttpServer server;
    private static String base;
    private static byte[] martiniqueBytes;
    /** Requests with a query string on an asset: only service clients (e.g. WMS GetCapabilities) send those. */
    private static final AtomicInteger assetQueryRequests = new AtomicInteger();
    /** Plain GET requests (no {@code Range} header) on counted assets: each one is a full download. */
    private static final AtomicInteger fullDownloads = new AtomicInteger();
    /** GET requests received by the flaky asset, which fails the first one. */
    private static final AtomicInteger flakyGets = new AtomicInteger();

    @BeforeClass
    public static void startServer() throws Exception {
        ImageIO.scanForPlugins();
        org.geotoolkit.lang.Setup.initialize(null);

        try (InputStream in = StacProviderTest.class.getResourceAsStream("/org/constellation/data/image/tif/martinique.tif")) {
            assertNotNull("martinique.tif test fixture not found on classpath", in);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            martiniqueBytes = out.toByteArray();
        }

        StacProvider.cacheRootOverride = java.nio.file.Files.createTempDirectory("stac-cache-test");

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        base = "http://localhost:" + server.getAddress().getPort();

        server.createContext("/asset/martinique.tif", StacProviderTest::serveAsset);
        server.createContext("/asset/missing.tif", StacProviderTest::serve404);
        server.createContext("/asset/typed", StacProviderTest::serveAsset);
        server.createContext("/asset/cached.tif", StacProviderTest::serveCountedAsset);
        server.createContext("/asset/remote.tif", StacProviderTest::serveCountedAsset);
        server.createContext("/asset/flaky.tif", StacProviderTest::serveFailingFirstGet);
        server.createContext("/item-cached", ex -> serveJson(ex, itemJson("item-cached", base + "/asset/cached.tif")));
        server.createContext("/item-remote", ex -> serveJson(ex, itemJson("item-remote", base + "/asset/remote.tif")));
        server.createContext("/item-flaky", ex -> serveJson(ex, itemJson("item-flaky", base + "/asset/flaky.tif")));
        server.createContext("/item-typed", ex -> serveJson(ex, typedItemJson("item-typed", base + "/asset/typed", "image/tiff; application=geotiff")));
        server.createContext("/item", ex -> serveJson(ex, itemJson("item1", base + "/asset/martinique.tif")));
        server.createContext("/item-alt", ex -> serveJson(ex, itemWithAlternateJson("item-alt", base + "/asset/martinique.tif")));
        server.createContext("/collection", ex -> serveJson(ex, collectionJson()));
        server.createContext("/notstac", ex -> serveJson(ex, "{\"foo\":\"bar\"}"));
        server.createContext("/search", StacProviderTest::serveSearch);
        server.start();
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) server.stop(0);
    }

    private final StacProvider provider = new StacProvider();

    @Test
    public void probeContent_item_supported() throws Exception {
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(new StorageConnector(new URL(base + "/item"))));
    }

    @Test
    public void probeContent_collection_supported() throws Exception {
        assertEquals(ProbeResult.SUPPORTED, provider.probeContent(new StorageConnector(new URL(base + "/collection"))));
    }

    @Test
    public void probeContent_nonStac_unsupported() throws Exception {
        assertEquals(ProbeResult.UNSUPPORTED_STORAGE, provider.probeContent(new StorageConnector(new URL(base + "/notstac"))));
    }

    @Test
    public void open_item_yieldsGridCoverage() throws Exception {
        try (DataStore store = provider.open(paramsFor(base + "/item", null))) {
            final var coverages = DataStores.flatten(store, true, GridCoverageResource.class);
            assertEquals(1, coverages.size());
            assertNotNull(coverages.iterator().next().getGridGeometry());
        }
    }

    @Test
    public void withBasicAuth_embedsCredentialsAsUserInfo() {
        final StacProvider.StacCredentials creds = new StacProvider.StacCredentials("bob", "s3:cr@t", null, null, null);
        assertEquals("https://bob:s3:cr%40t@example.org/collections/awc",
                StacProvider.withBasicAuth("https://example.org/collections/awc", creds));
    }

    @Test
    public void withBasicAuth_noCredentials_returnsUrlUnchanged() {
        assertEquals(base, StacProvider.withBasicAuth(base, null));
        assertEquals(base, StacProvider.withBasicAuth(base, new StacProvider.StacCredentials(null, null, null, null, null)));
    }

    @Test
    public void openAsset_directURI_yieldsGridCoverage() throws Exception {
        final StacApi api = new StacApi(new OpenApiConfiguration.Builder().updateBaseUri(base).build());
        try (DataStore store = StacProvider.openAsset(api, URI.create(base + "/asset/martinique.tif"), null, null, true)) {
            final var coverages = DataStores.flatten(store, true, GridCoverageResource.class);
            assertEquals(1, coverages.size());
            assertNotNull(coverages.iterator().next().getGridGeometry());
        }
    }

    @Test
    public void open_item_withMediaType_doesNotProbeServiceClients() throws Exception {
        assetQueryRequests.set(0);
        try (DataStore store = provider.open(paramsFor(base + "/item-typed", null))) {
            assertEquals(1, DataStores.flatten(store, true, GridCoverageResource.class).size());
        }
        assertEquals("no service-client probe (e.g. WMS GetCapabilities) must hit the asset", 0, assetQueryRequests.get());
    }

    @Test
    public void open_item_default_downloadsOnceThenReusesCache() throws Exception {
        fullDownloads.set(0);
        for (int i = 0; i < 2; i++) {
            try (DataStore store = provider.open(paramsFor(base + "/item-cached", null))) {
                assertEquals(1, DataStores.flatten(store, true, GridCoverageResource.class).size());
            }
        }
        assertEquals("asset must be downloaded once, then read from the cache", 1, fullDownloads.get());
    }

    @Test
    public void open_item_remoteReading_doesNotDownload() throws Exception {
        final long before = cacheEntries();
        try (DataStore store = provider.open(paramsFor(base + "/item-remote", null, null, true))) {
            assertEquals(1, DataStores.flatten(store, true, GridCoverageResource.class).size());
        }
        assertEquals("remote reading must not write the asset to the download cache", before, cacheEntries());
    }

    private static long cacheEntries() throws IOException {
        try (var entries = java.nio.file.Files.list(StacProvider.cacheRootOverride)) {
            return entries.count();
        }
    }

    @Test
    public void open_item_remoteReading_fallsBackToDownloadWhenRemoteFails() throws Exception {
        flakyGets.set(0);
        final long before = cacheEntries();
        try (DataStore store = provider.open(paramsFor(base + "/item-flaky", null, null, true))) {
            assertEquals(1, DataStores.flatten(store, true, GridCoverageResource.class).size());
        }
        assertEquals("the remote attempt must fail, then the asset must be downloaded", 2, flakyGets.get());
        assertEquals("the fallback download must go to the cache", before + 1, cacheEntries());
    }

    @Test
    public void preferredFormat_fromMediaTypeThenExtension() {
        assertEquals("NetCDF", StacProvider.preferredFormat("application/x-netcdf", URI.create("https://h/a")));
        assertEquals("NetCDF", StacProvider.preferredFormat("application/netcdf", URI.create("https://h/a")));
        assertEquals("GeoTIFF", StacProvider.preferredFormat("image/tiff; application=geotiff; profile=cloud-optimized", URI.create("https://h/a")));
        assertEquals("geojson", StacProvider.preferredFormat("application/geo+json", URI.create("https://h/a")));
        assertEquals("NetCDF", StacProvider.preferredFormat(null, URI.create("https://h/dir/file.NC?token=x")));
        assertEquals("GeoTIFF", StacProvider.preferredFormat("application/octet-stream", URI.create("s3://bucket/file.tiff")));
        assertEquals("CSV", StacProvider.preferredFormat("text/csv; charset=utf-8", URI.create("https://h/a")));
        assertEquals("CSV", StacProvider.preferredFormat(null, URI.create("https://h/stations.CSV")));
        assertEquals(null, StacProvider.preferredFormat(null, URI.create("https://h/download")));
    }

    @Test
    public void mediaTypeOf_primaryAndAlternate() throws Exception {
        final org.geotoolkit.stac.dto.Item item = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                "{\"id\":\"i\",\"assets\":{\"data-file\":{\"href\":\"/local/a.nc\",\"type\":\"application/x-netcdf\"," +
                "\"alternate\":{\"HTTPS_MIRROR\":{\"href\":\"https://m/a.nc\"},\"S3_MIRROR\":{\"href\":\"s3://b/a.tif\",\"type\":\"image/tiff\"}}}}}",
                org.geotoolkit.stac.dto.Item.class);
        assertEquals("application/x-netcdf", StacProvider.mediaTypeOf(item, URI.create("/local/a.nc")));
        assertEquals("application/x-netcdf", StacProvider.mediaTypeOf(item, URI.create("https://m/a.nc")));
        assertEquals("image/tiff", StacProvider.mediaTypeOf(item, URI.create("s3://b/a.tif")));
        assertEquals(null, StacProvider.mediaTypeOf(item, URI.create("https://other/x")));
    }

    @Test
    public void open_collection_skipsUnresolvableItems() throws Exception {
        try (DataStore store = provider.open(paramsFor(base + "/collection", null))) {
            assertTrue(store instanceof Aggregate);
            assertEquals(2, ((Aggregate) store).components().size());
        }
    }

    /**
     * Two items whose assets open to datasets with the same identifier (here the same file) must still yield
     * distinct, resolvable names: Examind indexes data by identifier and drops duplicates.
     */
    @Test
    public void open_collection_scopesDatasetNamesByItemId() throws Exception {
        try (DataStore store = provider.open(paramsFor(base + "/collection", null))) {
            final java.util.List<String> ids = new java.util.ArrayList<>();
            for (org.apache.sis.storage.Resource r : ((Aggregate) store).components()) {
                ids.add(r.getIdentifier().orElseThrow().toString());
            }
            assertEquals(2, ids.size());
            assertEquals("dataset names must be unique: " + ids, 2, new java.util.HashSet<>(ids).size());
            assertTrue(ids.toString(), ids.get(0).startsWith("item-ok-1:"));
            assertTrue(ids.toString(), ids.get(1).startsWith("item-ok-2:"));
            for (String id : ids) {
                assertEquals(id, store.findResource(id).getIdentifier().orElseThrow().toString());
            }
        }
    }

    @Test
    public void open_collection_respectsMaxItems() throws Exception {
        try (DataStore store = provider.open(paramsFor(base + "/collection", 1))) {
            assertEquals(1, ((Aggregate) store).components().size());
        }
    }

    @Test
    public void open_item_withAssetSelector_usesAlternate() throws Exception {
        try (DataStore store = provider.open(paramsFor(base + "/item-alt", null, "{\"role\":\"data\",\"href\":\"HTTPS_MIRROR\"}"))) {
            assertEquals(1, DataStores.flatten(store, true, GridCoverageResource.class).size());
        }
    }

    @Test(expected = DataStoreException.class)
    public void open_item_withAssetSelector_noMatch_throws() throws Exception {
        // the default extractor would find HTTPS_MIRROR by probing; the rule must be authoritative
        provider.open(paramsFor(base + "/item-alt", null, "{\"href\":\"FTP_MIRROR\"}")).close();
    }

    @Test(expected = DataStoreException.class)
    public void open_invalidAssetSelector_throws() throws Exception {
        provider.open(paramsFor(base + "/item", null, "{not json")).close();
    }

    /** Data asset whose primary href is a non-downloadable local path; the real file is behind the HTTPS_MIRROR alternate. */
    private static String itemWithAlternateJson(String id, String alternateHref) {
        return "{\"type\":\"Feature\",\"id\":\"" + id + "\",\"collection\":\"col1\"," +
                "\"assets\":{\"data-file\":{\"href\":\"/archive/local/" + id + ".tif\",\"roles\":[\"data\"]," +
                "\"alternate\":{\"HTTPS_MIRROR\":{\"href\":\"" + alternateHref + "\"}}}}}";
    }

    private static Parameters paramsFor(String url, Integer maxItems) throws Exception {
        return paramsFor(url, maxItems, null);
    }

    private static Parameters paramsFor(String url, Integer maxItems, String assetSelector) throws Exception {
        return paramsFor(url, maxItems, assetSelector, null);
    }

    private static Parameters paramsFor(String url, Integer maxItems, String assetSelector, Boolean remoteReading) throws Exception {
        final Parameters params = Parameters.castOrWrap(StacProvider.PARAMETERS.createValue());
        params.getOrCreate(StacProvider.URL).setValue(new URL(url));
        if (maxItems != null) params.getOrCreate(StacProvider.MAX_ITEMS).setValue(maxItems);
        if (assetSelector != null) params.getOrCreate(StacProvider.ASSET_SELECTOR).setValue(assetSelector);
        if (remoteReading != null) params.getOrCreate(StacProvider.REMOTE_READING).setValue(remoteReading);
        return params;
    }

    private static void serveAsset(HttpExchange ex) throws IOException {
        if (ex.getRequestURI().getRawQuery() != null) assetQueryRequests.incrementAndGet();
        if ("HEAD".equals(ex.getRequestMethod())) {
            ex.getResponseHeaders().add("Content-Length", String.valueOf(martiniqueBytes.length));
            ex.sendResponseHeaders(200, -1);
            ex.close();
            return;
        }
        ex.sendResponseHeaders(200, martiniqueBytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(martiniqueBytes);
        }
    }

    /** Serves martinique.tif and counts plain GET requests (each one is a full download). */
    private static void serveCountedAsset(HttpExchange ex) throws IOException {
        if ("GET".equals(ex.getRequestMethod()) && !ex.getRequestHeaders().containsKey("Range")) fullDownloads.incrementAndGet();
        serveAsset(ex);
    }

    /**
     * Answers 500 to the first GET (the remote-reading attempt) and serves martinique.tif afterwards
     * (the fallback download). HEAD requests from the URI extractor always succeed.
     */
    private static void serveFailingFirstGet(HttpExchange ex) throws IOException {
        if ("GET".equals(ex.getRequestMethod()) && flakyGets.incrementAndGet() == 1) {
            ex.sendResponseHeaders(500, -1);
            ex.close();
            return;
        }
        serveAsset(ex);
    }

    private static void serve404(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(404, -1);
        ex.close();
    }

    private static void serveJson(HttpExchange ex, String json) throws IOException {
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void serveSearch(HttpExchange ex) throws IOException {
        // Three items: two resolvable via the local asset endpoint, one pointing at a 404 (unresolvable, must be skipped).
        final String json = "{\"type\":\"FeatureCollection\",\"features\":[" +
                itemJson("item-ok-1", base + "/asset/martinique.tif") + "," +
                itemJson("item-ok-2", base + "/asset/martinique.tif") + "," +
                itemJson("item-bad", base + "/asset/missing.tif") +
                "]}";
        serveJson(ex, json);
    }

    private static String itemJson(String id, String assetHref) {
        return "{\"type\":\"Feature\",\"id\":\"" + id + "\",\"collection\":\"col1\"," +
                "\"assets\":{\"data\":{\"href\":\"" + assetHref + "\",\"roles\":[\"data\"]}}}";
    }

    private static String typedItemJson(String id, String assetHref, String mediaType) {
        return "{\"type\":\"Feature\",\"id\":\"" + id + "\",\"collection\":\"col1\"," +
                "\"assets\":{\"data\":{\"href\":\"" + assetHref + "\",\"type\":\"" + mediaType + "\",\"roles\":[\"data\"]}}}";
    }

    private static String collectionJson() {
        return "{\"type\":\"Collection\",\"id\":\"col1\",\"links\":[{\"rel\":\"root\",\"href\":\"" + base + "\"}]}";
    }

    @Test
    public void preview_item_listsCandidates() throws Exception {
        final StacPreview.Result result = StacPreview.preview(base + "/item-alt", null, null);
        assertEquals("ITEM", result.type());
        assertEquals("item-alt", result.itemId());
        assertEquals(2, result.candidates().size());
        assertEquals("HTTPS_MIRROR", result.candidates().get(1).href());
        assertEquals("http", result.candidates().get(1).scheme());
    }

    @Test
    public void preview_collection_usesFirstItem() throws Exception {
        final StacPreview.Result result = StacPreview.preview(base + "/collection", null, null);
        assertEquals("COLLECTION", result.type());
        assertEquals("item-ok-1", result.itemId());
    }

    @Test(expected = DataStoreException.class)
    public void preview_nonStac_throws() throws Exception {
        StacPreview.preview(base + "/notstac", null, null);
    }
}
