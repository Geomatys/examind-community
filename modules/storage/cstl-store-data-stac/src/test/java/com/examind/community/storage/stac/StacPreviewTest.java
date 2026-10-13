package com.examind.community.storage.stac;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geotoolkit.stac.dto.Asset;
import org.geotoolkit.stac.dto.Item;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class StacPreviewTest {

    @Test
    public void candidates_listsPrimaryThenAlternates_skipsBlankHrefs() {
        final Asset s3 = new Asset();
        s3.setHref("s3://bucket/sample.nc");
        final Asset empty = new Asset();
        empty.setHref("");
        final Map<String, Asset> alternates = new LinkedHashMap<>();
        alternates.put("S3_MIRROR", s3);
        alternates.put("BROKEN", empty);

        final Asset data = new Asset();
        data.setHref("/archive/local/sample.nc");
        data.setType("application/x-netcdf");
        data.setRoles(List.of("data"));
        data.setAlternate(alternates);

        final Map<String, Asset> assets = new LinkedHashMap<>();
        assets.put("data-file", data);
        final Item item = new Item();
        item.setAssets(assets);

        assertEquals(List.of(
                new StacPreview.Candidate("data-file", List.of("data"), "application/x-netcdf", null, "/archive/local/sample.nc", "file"),
                new StacPreview.Candidate("data-file", List.of("data"), "application/x-netcdf", "S3_MIRROR", "s3://bucket/sample.nc", "s3")),
                StacPreview.candidates(item));
    }

    @Test
    public void candidates_noAssets_empty() {
        assertEquals(List.of(), StacPreview.candidates(new Item()));
    }

    @Test
    public void scheme_normalizesCase_defaultsToFile() {
        assertEquals("https", StacPreview.scheme("HTTPS://example.org/a.nc"));
        assertEquals("file", StacPreview.scheme("/local/a.nc"));
        assertEquals("file", StacPreview.scheme("not a uri with spaces"));
    }
}
