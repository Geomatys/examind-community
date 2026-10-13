package com.examind.community.storage.stac;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.util.FileSystemUtilities;
import org.geotoolkit.client.AbstractClientProvider;
import org.geotoolkit.client.openapi.OpenApiConfiguration;
import org.geotoolkit.stac.client.DefaultDownloadURIExtractor;
import org.geotoolkit.stac.client.DownloadURIExtractor;
import org.geotoolkit.stac.client.StacApi;
import org.geotoolkit.stac.client.StacResourceType;
import org.geotoolkit.stac.dto.Item;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Exposes a remote STAC (SpatioTemporal Asset Catalog) Item or Collection as Examind data.
 * An Item resolves to the datastore of its downloaded asset; a Collection resolves to a
 * {@link StacCollectionStore} aggregating each of its items (up to {@link #MAX_ITEMS}).
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@StoreMetadata(
        formatName = StacProvider.NAME,
        capabilities = {Capability.READ},
        resourceTypes = {GridCoverageResource.class, FeatureSet.class, Aggregate.class})
public class StacProvider extends AbstractClientProvider {

    private static final Logger LOGGER = Logger.getLogger(StacProvider.class.getName());

    /** Provider short name, also used as provider type in Examind configurations. */
    public static final String NAME = "stac";

    /** Fully qualified class name of the {@link DownloadURIExtractor} to use, Optional. */
    public static final ParameterDescriptor<String> DOWNLOAD_URI_EXTRACTOR;

    /** JSON {@link RuleBasedDownloadURIExtractor.AssetSelector} choosing the asset href to open; overrides {@link #DOWNLOAD_URI_EXTRACTOR}, Optional. */
    public static final ParameterDescriptor<String> ASSET_SELECTOR;

    /** Safety cap on the number of items eagerly resolved when the URL points to a Collection, Optional. */
    public static final ParameterDescriptor<Integer> MAX_ITEMS;

    /** When {@code true}, {@link #MAX_ITEMS} is ignored and every item of the Collection is resolved, Optional. */
    public static final ParameterDescriptor<Boolean> UNLIMITED_ITEMS;

    /**
     * When {@code true}, assets are read directly from their remote location (HTTP range requests, S3…) and
     * downloaded only if their format cannot be read remotely. When {@code false} (default), each asset is
     * downloaded once to a persistent cache and read locally. Optional.
     */
    public static final ParameterDescriptor<Boolean> REMOTE_READING;

    /** HTTP Basic Auth login used for the STAC catalog and its assets, Optional. */
    public static final ParameterDescriptor<String> HTTP_LOGIN;
    /** HTTP Basic Auth password used for the STAC catalog and its assets, Optional. */
    public static final ParameterDescriptor<String> HTTP_PASSWORD;

    /** Access key used to read assets hosted on an {@code s3://} bucket, Optional. */
    public static final ParameterDescriptor<String> S3_ACCESS_KEY;
    /** Secret key used to read assets hosted on an {@code s3://} bucket, Optional. */
    public static final ParameterDescriptor<String> S3_SECRET_KEY;
    /** Region of the {@code s3://} bucket hosting the assets, Optional. */
    public static final ParameterDescriptor<String> S3_REGION;

    /** All parameters accepted by {@link #open(ParameterValueGroup)}. */
    public static final ParameterDescriptorGroup PARAMETERS;

    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(false);
        DOWNLOAD_URI_EXTRACTOR = builder
                .addName("downloadURIExtractor")
                .setDescription("Fully qualified class name of the DownloadURIExtractor implementation to use.")
                .createEnumerated(String.class,
                        new String[]{DefaultDownloadURIExtractor.class.getName()},
                        DefaultDownloadURIExtractor.class.getName());
        ASSET_SELECTOR = builder
                .addName("assetSelector")
                .setDescription("JSON rule {asset, role, type, href} selecting the asset href to open. Overrides downloadURIExtractor.")
                .create(String.class, null);
        MAX_ITEMS = builder
                .addName("maxItems")
                .setDescription("Maximum number of items to import when the URL points to a STAC Collection.")
                .create(Integer.class, 20);
        UNLIMITED_ITEMS = builder
                .addName("unlimitedItems")
                .setDescription("Ignore maxItems and import every item of the STAC Collection.")
                .create(Boolean.class, false);
        REMOTE_READING = builder
                .addName("remoteReading")
                .setDescription("Read assets remotely instead of downloading them to a local cache first.")
                .create(Boolean.class, false);
        HTTP_LOGIN = builder
                .addName("httpLogin")
                .setDescription("Login used for HTTP Basic authentication against the STAC catalog and its assets.")
                .create(String.class, null);
        HTTP_PASSWORD = builder
                .addName("httpPassword")
                .setDescription("Password used for HTTP Basic authentication against the STAC catalog and its assets.")
                .create(String.class, null);
        S3_ACCESS_KEY = builder
                .addName("s3AccessKey")
                .setDescription("Access key used to read STAC assets hosted on an s3:// bucket.")
                .create(String.class, null);
        S3_SECRET_KEY = builder
                .addName("s3SecretKey")
                .setDescription("Secret key used to read STAC assets hosted on an s3:// bucket.")
                .create(String.class, null);
        S3_REGION = builder
                .addName("s3Region")
                .setDescription("Region of the s3:// bucket hosting STAC assets.")
                .create(String.class, null);
        PARAMETERS = builder.addName(NAME).addName("StacParameters").createGroup(
                URL, DOWNLOAD_URI_EXTRACTOR, ASSET_SELECTOR, MAX_ITEMS, UNLIMITED_ITEMS, REMOTE_READING, HTTP_LOGIN, HTTP_PASSWORD, S3_ACCESS_KEY, S3_SECRET_KEY, S3_REGION);
    }

    /**
     * Credentials optionally used to access the STAC catalog itself (HTTP Basic Auth) and/or its
     * assets ({@code s3://} bucket credentials). Any field may be {@code null} when not configured.
     */
    record StacCredentials(String httpLogin, String httpPassword, String s3AccessKey, String s3SecretKey, String s3Region) {}

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS;
    }

    /**
     * Accepts a URL when it resolves to a STAC Item or Collection document. Any failure (unreachable host,
     * non-JSON answer, unknown STAC type) is reported as {@link ProbeResult#UNSUPPORTED_STORAGE}.
     */
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        try {
            final URL url = connector.getStorageAs(java.net.URL.class);
            if (url == null) return ProbeResult.UNSUPPORTED_STORAGE;
            final StacApi api = new StacApi(new OpenApiConfiguration.Builder().updateBaseUri(url.toString()).build());
            final StacResourceType type = api.detectStacType(url.toString());
            return type == StacResourceType.UNKNOWN ? ProbeResult.UNSUPPORTED_STORAGE : ProbeResult.SUPPORTED;
        } catch (Exception e) {
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    /**
     * Opens the STAC resource designated by the {@code url} parameter:
     * <ul>
     *   <li>an Item is opened as the datastore of its selected asset (see {@link #openItem});</li>
     *   <li>a Collection is opened as a {@link StacCollectionStore} aggregating up to {@link #MAX_ITEMS}
     *       of its items (all of them when {@link #UNLIMITED_ITEMS} is set).</li>
     * </ul>
     * The asset of each item is chosen by {@link #ASSET_SELECTOR} when present, otherwise by the
     * {@link #DOWNLOAD_URI_EXTRACTOR} class (default: HTTP reachability probing). Assets are read remotely
     * or from a local download depending on {@link #REMOTE_READING}.
     *
     * @throws DataStoreException if the URL is not a STAC Item/Collection, the asset selector is invalid,
     *         or the STAC endpoint cannot be queried
     */
    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        ensureCanProcess(parameters);
        final Parameters params = Parameters.castOrWrap(parameters);
        final URL url = params.getMandatoryValue(URL);
        final String extractorClassName = params.getValue(DOWNLOAD_URI_EXTRACTOR);
        final String selectorJson = params.getValue(ASSET_SELECTOR);
        final DownloadURIExtractor extractor = (selectorJson == null || selectorJson.isBlank())
                ? null : new RuleBasedDownloadURIExtractor(RuleBasedDownloadURIExtractor.AssetSelector.parse(selectorJson));
        final Integer maxItemsParam = params.getValue(MAX_ITEMS);
        final int maxItems = maxItemsParam == null ? 20 : maxItemsParam;
        final Boolean unlimitedParam = params.getValue(UNLIMITED_ITEMS);
        final Integer maxTotal = Boolean.TRUE.equals(unlimitedParam) ? null : maxItems;
        final boolean remoteReading = Boolean.TRUE.equals(params.getValue(REMOTE_READING));
        final StacCredentials credentials = new StacCredentials(
                params.getValue(HTTP_LOGIN), params.getValue(HTTP_PASSWORD),
                params.getValue(S3_ACCESS_KEY), params.getValue(S3_SECRET_KEY), params.getValue(S3_REGION));
        final String urlString = url.toString();
        final StacApi api = buildStacApi(urlString, extractorClassName, extractor, credentials);
        try {
            final StacResourceType type = api.detectStacType(urlString);
            return switch (type) {
                case ITEM -> openItem(api, urlString, credentials, remoteReading);
                case COLLECTION -> {
                    final CollectionRoot root = resolveCollectionRoot(urlString, credentials);
                    final StacApi collectionApi = buildStacApi(root.rootUrl(), extractorClassName, extractor, credentials);
                    yield new StacCollectionStore(collectionApi, root.collectionId(), maxTotal, credentials, remoteReading);
                }
                case UNKNOWN -> throw new DataStoreException("URL does not point to a STAC Item or Collection: " + urlString);
            };
        } catch (IOException e) {
            throw new DataStoreException("Error while querying STAC endpoint: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataStoreException("Interrupted while querying STAC endpoint: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the Item at {@code url}, resolves its download URI with the API's extractor and opens that asset.
     *
     * @throws DataStoreException if the document is not an Item or no asset matches the extractor
     */
    private DataStore openItem(StacApi api, String url, StacCredentials credentials, boolean remoteReading) throws DataStoreException, IOException, InterruptedException {
        final Item item = api.loadItem(url);
        if (item == null) throw new DataStoreException("Not a STAC Item: " + url);
        final URI downloadURI = api.getDownloadURI(item);
        if (downloadURI == null) throw new DataStoreException("No downloadable asset found for STAC item: " + url);
        return openAsset(api, downloadURI, mediaTypeOf(item, downloadURI), credentials, remoteReading);
    }

    /**
     * Opens a STAC asset.
     * <ul>
     *   <li>{@code remoteReading = false}: the asset is downloaded once to the local cache (see
     *       {@link #cachedDownload}) and opened from there; later opens reuse the cached file.</li>
     *   <li>{@code remoteReading = true}: the asset is opened directly from its remote location (HTTP range
     *       requests, S3…). If its format cannot be read remotely (format needing local random access, host
     *       without HTTP Range support…), a warning is logged and the asset is downloaded as above.</li>
     * </ul>
     *
     * @param mediaType     the asset media type if known, used to pick the datastore format to try first
     * @param remoteReading whether to read the asset remotely instead of downloading it
     */
    static DataStore openAsset(StacApi api, URI downloadURI, String mediaType, StacCredentials credentials, boolean remoteReading) throws DataStoreException {
        registerS3FileSystemIfNeeded(downloadURI, credentials);
        final String format = preferredFormat(mediaType, downloadURI);
        if (remoteReading) {
            LOGGER.info(() -> "STAC asset " + downloadURI + " (type=" + mediaType + "): opening remotely, preferred format=" + format);
            final long start = System.nanoTime();
            try {
                final DataStore store = DataStores.open(downloadURI, format);
                LOGGER.info(() -> "STAC asset " + downloadURI + ": opened remotely as '" + store.getProvider().getShortName() + "'" + elapsed(start));
                return store;
            } catch (DataStoreException e) {
                LOGGER.log(Level.WARNING, "STAC asset {0}: cannot be read remotely ({1}){2}, downloading it instead",
                        new Object[] {downloadURI, e.getMessage(), elapsed(start)});
            }
        }
        final Path file = cachedDownload(api, downloadURI);
        final long openStart = System.nanoTime();
        final DataStore store = DataStores.open(file, format);
        LOGGER.info(() -> "STAC asset " + downloadURI + ": opened local file " + file + " as '" + store.getProvider().getShortName() + "'" + elapsed(openStart));
        return store;
    }

    /**
     * Root directory of the STAC asset download cache. Set only by tests; {@code null} means
     * {@code <Examind integrated data directory>/stac-cache}.
     */
    static volatile Path cacheRootOverride;

    /**
     * Returns the local copy of an asset, downloading it on first use.
     *
     * <p>Each asset gets its own directory, named after the SHA-256 of its URI, under the cache root
     * ({@code <Examind integrated data directory>/stac-cache}, or {@code <java.io.tmpdir>/examind-stac-cache}
     * when Examind configuration is not initialised). The cache survives restarts: an asset already present
     * is reused without any network access. The download is done in a temporary sibling directory moved
     * into place once complete, so an interrupted download is never mistaken for a cached file, and two
     * concurrent downloads of the same asset keep a single copy.</p>
     *
     * <p>Cached files are not removed when the provider is deleted.</p>
     *
     * @return the downloaded file
     */
    static Path cachedDownload(StacApi api, URI uri) throws DataStoreException {
        try {
            final Path root = cacheRoot();
            final Path target = root.resolve(sha256(uri.toString()));
            final Path cached = singleFile(target);
            if (cached != null) {
                LOGGER.info(() -> "STAC asset " + uri + ": reusing cached download " + cached);
                return cached;
            }
            final Path partial = Files.createTempDirectory(root, target.getFileName() + ".part-");
            final long start = System.nanoTime();
            try {
                api.downloadFile(uri, partial);
            } catch (IOException | InterruptedException | RuntimeException e) {
                deleteRecursively(partial);
                throw e;
            }
            try {
                Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // another download of the same asset completed first: keep its copy, drop ours
                if (singleFile(target) == null) throw e;
                deleteRecursively(partial);
            }
            final Path file = singleFile(target);
            if (file == null) throw new IOException("Downloaded asset not found in " + target);
            LOGGER.info(() -> "STAC asset " + uri + ": downloaded to " + file + elapsed(start));
            return file;
        } catch (IOException e) {
            throw new DataStoreException("Cannot download STAC item asset " + uri + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataStoreException("Interrupted while downloading STAC item asset " + uri, e);
        }
    }

    private static Path cacheRoot() throws IOException {
        Path root = cacheRootOverride;
        if (root == null) {
            final Path integrated = ConfigDirectory.getDataIntegratedDirectory("stac-cache", true);
            root = integrated != null ? integrated : Path.of(System.getProperty("java.io.tmpdir"), "examind-stac-cache");
        }
        return Files.createDirectories(root);
    }

    /** Best-effort removal of a directory tree; failures are logged, not thrown. */
    private static void deleteRecursively(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Cannot delete " + dir, e);
        }
    }

    /** The only regular file in {@code dir}, or {@code null} if {@code dir} does not exist or holds no file. */
    private static Path singleFile(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return null;
        try (var files = Files.list(dir)) {
            return files.filter(Files::isRegularFile).findFirst().orElse(null);
        }
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // SHA-256 is mandatory on every JVM
        }
    }

    /** Elapsed time since {@code start} (from {@link System#nanoTime()}), formatted for log messages. */
    static String elapsed(long start) {
        return " in " + (System.nanoTime() - start) / 1_000_000 + " ms";
    }

    /**
     * Name of the datastore format to try first when opening an asset, passed to
     * {@link DataStores#open(Object, String)}.
     *
     * <p>Without a preferred format, SIS probes every registered {@link org.apache.sis.storage.DataStoreProvider}
     * in registration order. Service clients (WMS, WMS-C, OGC API…) come first in that order and each of them
     * sends a request against the asset URL (e.g. a WMS {@code GetCapabilities}) before any file format is
     * tried: slow, noisy in the logs, and it may download the whole asset for nothing. Naming the format
     * makes SIS try the matching provider first; if that provider rejects the asset, SIS falls back to
     * probing every provider as before.</p>
     *
     * <p>The format is derived from the STAC asset media type first, then from the file extension of the URI
     * path. Media types are matched by prefix so that parameters ({@code image/tiff; profile=cloud-optimized})
     * are accepted. Returned values are {@link org.apache.sis.storage.DataStoreProvider#getShortName()} names:</p>
     *
     * <table>
     *   <caption>Supported formats</caption>
     *   <tr><th>Media type</th><th>Extension</th><th>Format</th></tr>
     *   <tr><td>{@code application/x-netcdf}, {@code application/netcdf}</td><td>{@code .nc}, {@code .nc4}, {@code .cdf}</td><td>{@code NetCDF}</td></tr>
     *   <tr><td>{@code image/tiff} (including COG)</td><td>{@code .tif}, {@code .tiff}</td><td>{@code GeoTIFF}</td></tr>
     *   <tr><td>{@code application/geo+json}</td><td>{@code .geojson}</td><td>{@code geojson}</td></tr>
     *   <tr><td>{@code text/csv}</td><td>{@code .csv}</td><td>{@code CSV}</td></tr>
     * </table>
     *
     * <p>To support a new asset format, add its media type and extension here with the short name of the
     * provider that reads it.</p>
     *
     * @param mediaType the asset media type ({@code type} property of the STAC asset), or {@code null}
     * @param uri       the asset URI, whose path extension is used when the media type is unknown
     * @return the preferred format short name, or {@code null} to let SIS probe every provider
     */
    static String preferredFormat(String mediaType, URI uri) {
        if (mediaType != null) {
            final String type = mediaType.toLowerCase(java.util.Locale.ROOT);
            if (type.startsWith("application/x-netcdf") || type.startsWith("application/netcdf")) return "NetCDF";
            if (type.startsWith("image/tiff")) return "GeoTIFF";
            if (type.startsWith("application/geo+json")) return "geojson";
            if (type.startsWith("text/csv")) return "CSV";
        }
        final String path = uri.getPath();
        if (path == null) return null;
        final String name = path.toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".nc") || name.endsWith(".nc4") || name.endsWith(".cdf")) return "NetCDF";
        if (name.endsWith(".tif") || name.endsWith(".tiff")) return "GeoTIFF";
        if (name.endsWith(".geojson")) return "geojson";
        if (name.endsWith(".csv")) return "CSV";
        return null;
    }

    /**
     * Media type of the item asset whose primary or alternate href is {@code uri}: the alternate's own
     * type when it declares one, else the asset's type. {@code null} when no asset matches.
     */
    static String mediaTypeOf(Item item, URI uri) {
        if (item.getAssets() == null) return null;
        final String href = uri.toString();
        for (org.geotoolkit.stac.dto.Asset asset : item.getAssets().values()) {
            if (href.equals(asset.getHref())) return asset.getType();
            if (asset.getAlternate() == null) continue;
            for (org.geotoolkit.stac.dto.Asset alt : asset.getAlternate().values()) {
                if (href.equals(alt.getHref())) return alt.getType() != null ? alt.getType() : asset.getType();
            }
        }
        return null;
    }

    /**
     * Registers a credentialed NIO {@code FileSystem} for the asset's {@code s3://} bucket, so that
     * the subsequent {@code Paths.get}/{@code DataStores.open} calls on it can authenticate.
     * Registration is cached process-wide by {@link FileSystemUtilities}, keyed by URL, so this is
     * safe to call on every asset open.
     */
    private static void registerS3FileSystemIfNeeded(URI downloadURI, StacCredentials credentials) throws DataStoreException {
        if (credentials == null || credentials.s3AccessKey() == null || credentials.s3AccessKey().isBlank()) return;
        if (!"s3".equalsIgnoreCase(downloadURI.getScheme())) return;
        try {
            final Map<String, String> properties = (credentials.s3Region() == null || credentials.s3Region().isBlank())
                    ? Map.of() : Map.of("aws.region", credentials.s3Region());
            FileSystemUtilities.getFileSystem("s3", downloadURI.toString(), credentials.s3AccessKey(), credentials.s3SecretKey(), null, true, properties);
        } catch (URISyntaxException | IOException e) {
            throw new DataStoreException("Cannot open S3 filesystem for STAC asset '" + downloadURI + "': " + e.getMessage(), e);
        } catch (java.nio.file.ProviderNotFoundException e) {
            throw new DataStoreException("No S3 filesystem provider available on the classpath to open STAC asset '" + downloadURI + "'", e);
        }
    }

    /**
     * Mirrors {@code org.geotoolkit.stac.process.StacApiProcessingUtils.buildStacApi}, which is
     * package-private in geotk-stac and cannot be called directly from here.
     * A non-null {@code extractor} takes precedence over {@code extractorClassName}.
     */
    static StacApi buildStacApi(String baseUrl, String extractorClassName, DownloadURIExtractor extractor, StacCredentials credentials) throws DataStoreException {
        final OpenApiConfiguration config = new OpenApiConfiguration.Builder().updateBaseUri(withBasicAuth(baseUrl, credentials)).build();
        if (extractor != null) {
            return new StacApi(config, extractor);
        }
        if (extractorClassName == null || extractorClassName.isBlank()) {
            return new StacApi(config);
        }
        try {
            final Class<?> extractorClass = Class.forName(extractorClassName);
            return new StacApi(config, (DownloadURIExtractor) extractorClass.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new DataStoreException("Cannot instantiate DownloadURIExtractor '" + extractorClassName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Root STAC API URL and collection id resolved from a Collection resource URL — a Collection is not
     * directly searchable, only {@code {root}/search} is, so the API client must be bound to the root.
     */
    record CollectionRoot(String rootUrl, String collectionId) {}

    /** Embeds HTTP Basic Auth credentials, if any, as userinfo in {@code url} (consumed by {@link OpenApiConfiguration}). */
    static String withBasicAuth(String url, StacCredentials credentials) {
        if (credentials == null || credentials.httpLogin() == null || credentials.httpLogin().isBlank()) return url;
        try {
            final URI u = URI.create(url);
            final String userInfo = credentials.httpPassword() == null
                    ? credentials.httpLogin() : credentials.httpLogin() + ':' + credentials.httpPassword();
            return new URI(u.getScheme(), userInfo, u.getHost(), u.getPort(), u.getPath(), u.getQuery(), u.getFragment()).toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }

    /**
     * Mirrors {@code org.geotoolkit.stac.process.StacApiProcessingUtils.resolveCollectionRoot}, which is
     * package-private in geotk-stac and cannot be called directly from here.
     */
    static CollectionRoot resolveCollectionRoot(String collectionUrl, StacCredentials credentials) throws IOException, InterruptedException {
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(collectionUrl))
                .timeout(Duration.ofSeconds(60));
        if (credentials != null && credentials.httpLogin() != null && !credentials.httpLogin().isBlank()) {
            final String basic = credentials.httpLogin() + ':' + (credentials.httpPassword() == null ? "" : credentials.httpPassword());
            requestBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
        }
        final HttpResponse<String> response = HttpClient.newHttpClient()
                .send(requestBuilder.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " from GET " + collectionUrl);
        }
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode collNode = mapper.readTree(response.body());
        final String collectionId = collNode.path("id").asText(null);

        final JsonNode links = collNode.get("links");
        if (links != null && links.isArray()) {
            for (JsonNode link : links) {
                if ("root".equals(link.path("rel").asText()) && link.hasNonNull("href")) {
                    return new CollectionRoot(link.get("href").asText(), collectionId);
                }
            }
        }

        // Heuristic fallback if no explicit root link is present
        String rootUrl = collectionUrl;
        int idx = rootUrl.lastIndexOf("/collections/");
        if (idx != -1) {
            rootUrl = rootUrl.substring(0, idx);
        } else if (rootUrl.endsWith("/collections")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - "/collections".length());
        }
        return new CollectionRoot(rootUrl, collectionId);
    }
}
