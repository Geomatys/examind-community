package com.examind.community.storage.stac;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.stac.client.DownloadURIExtractor;
import org.geotoolkit.stac.dto.Asset;
import org.geotoolkit.stac.dto.Item;

/**
 * {@link DownloadURIExtractor} driven by a user-defined {@link AssetSelector}: returns the href
 * (primary or a given alternate) of the first asset matching every non-null filter. No HTTP probing.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public class RuleBasedDownloadURIExtractor implements DownloadURIExtractor {

    /**
     * Rule describing which href to open.
     *
     * @param asset asset key in {@code item.assets}, or {@code null} for any
     * @param role  role the asset must have, or {@code null} for any
     * @param type  media type the asset must have, or {@code null} for any
     * @param href  alternate key (STAC alternate-assets extension) to use, or {@code null} for the primary href
     */
    public record AssetSelector(String asset, String role, String type, String href) {

        private static final ObjectMapper MAPPER = new ObjectMapper();

        /**
         * Parses the JSON form of a selector, as stored in the {@code assetSelector} provider parameter.
         * Missing keys are read as {@code null}.
         *
         * @param json e.g. {@code {"asset":"data-file","role":"data","type":null,"href":"HTTPS_MIRROR"}}
         * @return the parsed selector
         * @throws DataStoreException if the JSON is malformed or contains unknown keys
         */
        public static AssetSelector parse(String json) throws DataStoreException {
            try {
                return MAPPER.readValue(json, AssetSelector.class);
            } catch (JsonProcessingException e) {
                throw new DataStoreException("Invalid STAC assetSelector '" + json + "': " + e.getOriginalMessage(), e);
            }
        }
    }

    private final AssetSelector selector;

    /**
     * @param selector the rule applied to every item
     */
    public RuleBasedDownloadURIExtractor(AssetSelector selector) {
        this.selector = selector;
    }

    @Override
    public URI extract(Item item) {
        final Map<String, Asset> assets = item.getAssets();
        if (assets == null) return null;
        for (Map.Entry<String, Asset> entry : assets.entrySet()) {
            final Asset asset = entry.getValue();
            if (!matches(entry.getKey(), asset)) continue;
            final String href = hrefOf(asset);
            if (href != null && !href.isBlank()) return URI.create(href);
        }
        return null;
    }

    private boolean matches(String key, Asset asset) {
        return (selector.asset() == null || selector.asset().equals(key))
            && (selector.role() == null || (asset.getRoles() != null && asset.getRoles().contains(selector.role())))
            && (selector.type() == null || selector.type().equals(asset.getType()));
    }

    private String hrefOf(Asset asset) {
        if (selector.href() == null) return asset.getHref();
        final Map<String, Asset> alternates = asset.getAlternate();
        final Asset alternate = alternates == null ? null : alternates.get(selector.href());
        return alternate == null ? null : alternate.getHref();
    }
}
