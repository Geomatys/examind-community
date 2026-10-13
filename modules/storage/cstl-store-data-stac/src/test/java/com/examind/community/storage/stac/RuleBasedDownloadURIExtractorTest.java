package com.examind.community.storage.stac;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.stac.dto.Asset;
import org.geotoolkit.stac.dto.Item;
import org.junit.Test;

import com.examind.community.storage.stac.RuleBasedDownloadURIExtractor.AssetSelector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class RuleBasedDownloadURIExtractorTest {

    private static final String PRIMARY = "/archive/local/sample.nc";
    private static final String FTP = "ftp://mirror.example.org/sample.nc";
    private static final String HTTPS = "https://mirror.example.org/sample.nc";
    private static final String THUMB = "https://mirror.example.org/sample.png";

    /** Synthetic item: a data asset with a non-downloadable primary href and two alternates, plus a thumbnail. */
    private static Item item() {
        final Asset ftp = new Asset();
        ftp.setHref(FTP);
        final Asset https = new Asset();
        https.setHref(HTTPS);
        final Map<String, Asset> alternates = new LinkedHashMap<>();
        alternates.put("FTP_MIRROR", ftp);
        alternates.put("HTTPS_MIRROR", https);

        final Asset data = new Asset();
        data.setHref(PRIMARY);
        data.setType("application/x-netcdf");
        data.setRoles(List.of("data", "metadata"));
        data.setAlternate(alternates);

        final Asset thumbnail = new Asset();
        thumbnail.setHref(THUMB);
        thumbnail.setType("image/png");
        thumbnail.setRoles(List.of("thumbnail"));

        final Map<String, Asset> assets = new LinkedHashMap<>();
        assets.put("thumbnail", thumbnail);
        assets.put("data-file", data);
        final Item item = new Item();
        item.setAssets(assets);
        return item;
    }

    private static URI extract(String asset, String role, String type, String href) {
        return new RuleBasedDownloadURIExtractor(new AssetSelector(asset, role, type, href)).extract(item());
    }

    @Test
    public void alternateOfMatchingAsset() {
        assertEquals(URI.create(HTTPS), extract("data-file", "data", null, "HTTPS_MIRROR"));
    }

    @Test
    public void primaryHrefWhenHrefIsNull() {
        assertEquals(URI.create(PRIMARY), extract("data-file", null, null, null));
    }

    @Test
    public void roleFilterSkipsNonMatchingAssets() {
        // thumbnail comes first in iteration order but has no "data" role
        assertEquals(URI.create(FTP), extract(null, "data", null, "FTP_MIRROR"));
    }

    @Test
    public void typeFilter() {
        assertEquals(URI.create(THUMB), extract(null, null, "image/png", null));
    }

    @Test
    public void allFiltersNull_firstAssetPrimaryHref() {
        assertEquals(URI.create(THUMB), extract(null, null, null, null));
    }

    @Test
    public void unknownAsset_returnsNull() {
        assertNull(extract("missing", null, null, null));
    }

    @Test
    public void unknownAlternate_returnsNull() {
        assertNull(extract("data-file", null, null, "S3_MIRROR"));
    }

    @Test
    public void noAssets_returnsNull() {
        assertNull(new RuleBasedDownloadURIExtractor(new AssetSelector(null, null, null, null)).extract(new Item()));
    }

    @Test
    public void parse_readsAllFields() throws DataStoreException {
        assertEquals(new AssetSelector("data-file", "data", null, "HTTPS_MIRROR"),
                AssetSelector.parse("{\"asset\":\"data-file\",\"role\":\"data\",\"type\":null,\"href\":\"HTTPS_MIRROR\"}"));
    }

    @Test(expected = DataStoreException.class)
    public void parse_invalidJson_throws() throws DataStoreException {
        AssetSelector.parse("{not json");
    }
}
