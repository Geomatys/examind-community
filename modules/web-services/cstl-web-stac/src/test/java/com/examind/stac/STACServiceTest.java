package com.examind.stac;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse;
import org.geotoolkit.ogcapi.dto.common.ConfClasses;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.stac.dto.Collection;
import org.geotoolkit.stac.dto.Collections;
import org.geotoolkit.stac.dto.Item;
import org.geotoolkit.stac.dto.ItemCollection;
import org.junit.Test;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end tests of the STAC REST endpoints, against a service instance
 * configured with one coverage layer ("coveragepng") and vector layers
 * (shapefiles), to exercise the generic-over-any-provider design.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public class STACServiceTest extends STACAbstractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Calls the STAC landing page ({@code GET /WS/stac/default}) and checks that:
     * <ul>
     *   <li>the request succeeds (HTTP 200) and the body parses as a {@link LandingPage};</li>
     *   <li>every link has a non-blank {@code href} and, per the trailing-{@code ?} bug fix
     *       in {@code DefaultSTACWorker#getBaseUrl()}, none of them contain the KVP marker
     *       {@code ?};</li>
     *   <li>a link to {@code /collections} is present, as required by the STAC/OGC API - Features spec.</li>
     * </ul>
     */
    @Test
    public void landingPageTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final LandingPage dto = MAPPER.readValue(response.body(), LandingPage.class);
        assertNotNull(dto.getLinks());
        boolean collectionsLinkFound = false;
        for (Link link : dto.getLinks()) {
            assertTrue(link.getHref() != null && !link.getHref().isBlank());
            assertTrue("STAC links must not contain a KVP marker", !link.getHref().contains("?"));
            if (link.getHref().endsWith("/collections")) {
                collectionsLinkFound = true;
            }
        }
        assertTrue(collectionsLinkFound);
    }

    /**
     * Calls {@code GET /WS/stac/default/conformance} and checks that the response
     * declares conformance to the STAC core conformance class, confirming the
     * service correctly advertises itself as a v1.0.0 STAC API.
     */
    @Test
    public void conformanceTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/conformance");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final ConfClasses dto = MAPPER.readValue(response.body(), ConfClasses.class);
        assertNotNull(dto.getConformsTo());
        assertTrue(dto.getConformsTo().contains("https://api.stacspec.org/v1.0.0/core"));
    }

    /**
     * Calls {@code GET /WS/stac/default/collections} and checks that the coverage
     * layer configured by {@link STACAbstractTest#initLayerList()} ("coveragepng")
     * is exposed as one of the STAC collections, proving that a coverage-backed
     * provider is correctly translated into a {@link Collection}.
     */
    @Test
    public void getCollectionsTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final Collections dto = MAPPER.readValue(response.body(), Collections.class);
        assertNotNull(dto.getCollections());
        boolean coveragepngFound = false;
        for (Collection collection : dto.getCollections()) {
            if ("coveragepng".equals(collection.getId())) {
                coveragepngFound = true;
            }
        }
        assertTrue(coveragepngFound);
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/coveragepng} and checks that a
     * single existing collection is returned with a populated spatial extent,
     * proving the generic-over-any-provider extent building (envelope derived
     * from the underlying coverage) works for a real request.
     */
    @Test
    public void getCollectionTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/coveragepng");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final Collection dto = MAPPER.readValue(response.body(), Collection.class);
        assertEquals("coveragepng", dto.getId());
        assertNotNull(dto.getExtent());
        assertNotNull(dto.getExtent().getSpatial());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/unknown} and checks that
     * requesting a collection id that has no matching layer correctly yields
     * HTTP 404, rather than an error or an empty 200.
     */
    @Test
    public void getCollectionNotFoundTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/unknown");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/coveragepng/items} and checks
     * that the coverage collection is exposed as exactly one synthetic
     * whole-coverage {@link Item} (the v1 simplification for coverage data,
     * as opposed to per-feature items for vector data).
     */
    @Test
    public void getItemsTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/coveragepng/items");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final ItemCollection dto = MAPPER.readValue(response.body(), ItemCollection.class);
        assertNotNull(dto.getFeatures());
        assertEquals(1, dto.getFeatures().size());
        assertEquals("coveragepng", dto.getFeatures().get(0).getId());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/city/items} against the "city" vector
     * layer and checks that, per the STAC best practices for vector data, it returns a
     * single whole-dataset item (not one item per feature) with a bounding-box geometry.
     */
    @Test
    public void getFeatureItemsTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/city/items");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final ItemCollection dto = MAPPER.readValue(response.body(), ItemCollection.class);
        assertNotNull(dto.getFeatures());
        assertEquals(1, dto.getFeatures().size());
        final Item item = dto.getFeatures().get(0);
        assertEquals("city", item.getCollection());
        assertNotNull(item.getBbox());
        assertEquals(4, item.getBbox().size());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/city/items/city} and checks that the
     * single whole-dataset item for the "city" vector layer can be fetched directly by id.
     */
    @Test
    public void getFeatureItemTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/city/items/city");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final Item dto = MAPPER.readValue(response.body(), Item.class);
        assertEquals("city", dto.getId());
        assertEquals("city", dto.getCollection());
        assertNotNull(dto.getBbox());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/unknown/items} and checks
     * that listing items of a non-existent collection yields HTTP 404.
     */
    @Test
    public void getItemsNotFoundTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/unknown/items");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/coveragepng/items/coveragepng}
     * and checks that the single synthetic item can be fetched directly by id,
     * with its collection reference and bounding box correctly populated.
     */
    @Test
    public void getItemTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/coveragepng/items/coveragepng");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());

        final Item dto = MAPPER.readValue(response.body(), Item.class);
        assertEquals("coveragepng", dto.getId());
        assertEquals("coveragepng", dto.getCollection());
        assertNotNull(dto.getBbox());
    }

    /**
     * Calls {@code GET /WS/stac/default/collections/coveragepng/items/unknown}
     * and checks that fetching a non-existent item id under an existing
     * collection yields HTTP 404.
     */
    @Test
    public void getItemNotFoundTest() throws Exception {
        initLayerList();

        final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/stac/default/collections/coveragepng/items/unknown");
        final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
        assertEquals(HttpURLConnection.HTTP_NOT_FOUND, response.statusCode());
    }
}