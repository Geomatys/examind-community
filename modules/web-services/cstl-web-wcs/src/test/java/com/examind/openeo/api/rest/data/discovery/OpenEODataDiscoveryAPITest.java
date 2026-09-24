package com.examind.openeo.api.rest.data.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OpenEODataDiscoveryAPITest {

    private static final String REMOTE = "https://stac.example.org";
    private static final String PROXY = "https://examind.example.org/openeo/svc1";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void rewritesLinkAndAssetHrefsAnywhereInTheDocument() throws Exception {
        String json = "{"
                + "\"id\": \"collection-1\","
                + "\"description\": \"mentions " + REMOTE + " in prose\","
                + "\"links\": [{\"rel\": \"self\", \"href\": \"" + REMOTE + "/collections/collection-1\"}],"
                + "\"assets\": {\"thumbnail\": {\"href\": \"" + REMOTE + "/thumb.png\"}},"
                + "\"features\": [{"
                + "  \"links\": [{\"rel\": \"self\", \"href\": \"" + REMOTE + "/collections/collection-1/items/item-1\"}],"
                + "  \"assets\": {\"data\": {\"href\": \"" + REMOTE + "/data/item-1.tif\"}}"
                + "}]"
                + "}";

        JsonNode root = MAPPER.readTree(json);
        OpenEODataDiscoveryAPI.rewriteHrefs(root, REMOTE, PROXY);

        assertEquals(PROXY + "/collections/collection-1", root.at("/links/0/href").asText());
        assertEquals(PROXY + "/thumb.png", root.at("/assets/thumbnail/href").asText());
        assertEquals(PROXY + "/collections/collection-1/items/item-1", root.at("/features/0/links/0/href").asText());
        assertEquals(PROXY + "/data/item-1.tif", root.at("/features/0/assets/data/href").asText());

        // Non-href occurrences of the remote URL substring (e.g. in prose) must be left untouched,
        // unlike the whole-body string-replace this test guards against regressing to.
        assertEquals("mentions " + REMOTE + " in prose", root.get("description").asText());
    }

    @Test
    public void leavesUnrelatedHrefsUntouched() throws Exception {
        String json = "{\"links\": [{\"rel\": \"self\", \"href\": \"https://other-host.org/collections/foo\"}]}";

        JsonNode root = MAPPER.readTree(json);
        OpenEODataDiscoveryAPI.rewriteHrefs(root, REMOTE, PROXY);

        assertEquals("https://other-host.org/collections/foo", root.at("/links/0/href").asText());
    }
}
