package com.examind.community.storage.stac;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.stac.client.StacApi;
import org.geotoolkit.stac.client.StacResourceType;
import org.geotoolkit.stac.dto.Asset;
import org.geotoolkit.stac.dto.Item;
import org.geotoolkit.stac.request.SearchItems;

/**
 * Lists every href (primary and alternates of each asset) of a STAC Item — or of the first item of a
 * Collection, assuming all its items share the same asset structure — so the user can build an
 * {@link RuleBasedDownloadURIExtractor.AssetSelector} from one of them.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public final class StacPreview {

    /**
     * @param href alternate key, or {@code null} for the asset's primary href (same meaning as {@code AssetSelector.href})
     * @param uri  the actual href value
     */
    public record Candidate(String asset, List<String> roles, String type, String href, String uri, String scheme) {}

    /** @param type {@code "ITEM"} or {@code "COLLECTION"}; @param itemId id of the item actually analysed */
    public record Result(String type, String itemId, List<Candidate> candidates) {}

    private StacPreview() {}

    /**
     * Loads the STAC resource at {@code url} and lists the hrefs of one of its items.
     * For a Collection, only its first item is fetched and analysed.
     *
     * @param url          URL of a STAC Item or Collection
     * @param httpLogin    HTTP Basic Auth login for the catalog, or {@code null}
     * @param httpPassword HTTP Basic Auth password for the catalog, or {@code null}
     * @return the resource type, the analysed item id and its candidate hrefs
     * @throws DataStoreException if the URL is missing, is not a STAC Item/Collection, the Collection is empty,
     *         or the endpoint cannot be queried
     */
    public static Result preview(String url, String httpLogin, String httpPassword) throws DataStoreException {
        if (url == null || url.isBlank()) throw new DataStoreException("Missing STAC url");
        final StacProvider.StacCredentials credentials = new StacProvider.StacCredentials(httpLogin, httpPassword, null, null, null);
        try {
            final StacApi api = StacProvider.buildStacApi(url, null, null, credentials);
            final StacResourceType type = api.detectStacType(url);
            final Item item = switch (type) {
                case ITEM -> api.loadItem(url);
                case COLLECTION -> {
                    final StacProvider.CollectionRoot root = StacProvider.resolveCollectionRoot(url, credentials);
                    final List<Item> items = StacProvider.buildStacApi(root.rootUrl(), null, null, credentials)
                            .searchItems(new SearchItems().collections(List.of(root.collectionId())).limit(1), 1);
                    yield items.isEmpty() ? null : items.get(0);
                }
                case UNKNOWN -> throw new DataStoreException("URL does not point to a STAC Item or Collection: " + url);
            };
            if (item == null) throw new DataStoreException("No STAC item found at: " + url);
            return new Result(type.name(), item.getId(), candidates(item));
        } catch (DataStoreException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataStoreException("Interrupted while querying STAC endpoint: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DataStoreException("Error while querying STAC endpoint: " + e.getMessage(), e);
        }
    }

    /**
     * Lists, for each asset of the item, its primary href then each alternate href. Blank hrefs are skipped.
     */
    static List<Candidate> candidates(Item item) {
        final List<Candidate> result = new ArrayList<>();
        if (item.getAssets() == null) return result;
        item.getAssets().forEach((key, asset) -> {
            add(result, key, asset, null, asset.getHref());
            if (asset.getAlternate() != null) {
                asset.getAlternate().forEach((altKey, alt) -> add(result, key, asset, altKey, alt.getHref()));
            }
        });
        return result;
    }

    private static void add(List<Candidate> result, String key, Asset asset, String href, String uri) {
        if (uri == null || uri.isBlank()) return;
        result.add(new Candidate(key, asset.getRoles(), asset.getType(), href, uri, scheme(uri)));
    }

    /**
     * @return the lower-case URI scheme ({@code https}, {@code s3}…), or {@code "file"} for a path without scheme
     *         or an unparsable URI
     */
    static String scheme(String uri) {
        try {
            final String scheme = URI.create(uri).getScheme();
            return scheme == null ? "file" : scheme.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return "file";
        }
    }
}
