package com.examind.stac.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import javax.xml.namespace.QName;
import org.constellation.api.ServiceDef;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.LayerCache;
import org.constellation.ws.LayerWorker;
import org.geotoolkit.ogcapi.dto.common.ConfClasses;
import org.geotoolkit.ogcapi.dto.common.Extent;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.ogcapi.dto.common.SpatialExtent;
import org.geotoolkit.ogcapi.dto.common.TemporalExtent;
import org.geotoolkit.stac.dto.Collection;
import org.geotoolkit.stac.dto.Collections;
import org.geotoolkit.stac.dto.Item;
import org.geotoolkit.stac.dto.ItemCollection;
import org.opengis.metadata.extent.GeographicBoundingBox;

/**
 * Default {@link STACWorker} implementation, backed by the layers configured
 * on the STAC service instance (reusing the same layer/provider selection as
 * WMS/WFS/WCS/DGGS).
 *
 * <p>Per the STAC best practices for vector data, a layer (vector or coverage)
 * is exposed as a single STAC item representing the whole dataset, with its
 * bounding box as geometry — not one item per feature. {@code /search} and
 * per-item {@code assets} (a download link to the underlying file) can be
 * added later if needed.</p>
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public class DefaultSTACWorker extends LayerWorker implements STACWorker {

    private static final String STAC_VERSION = "1.0.0";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * @param id identifier of the STAC service instance
     */
    public DefaultSTACWorker(final String id) {
        super(id, ServiceDef.Specification.STAC);
        started();
    }

    /**
     * {@link #getServiceUrl()} appends a trailing {@code ?}, meant for KVP-style OGC
     * services (e.g. {@code WMS?SERVICE=WMS&...}). STAC is a plain REST API, so that
     * marker is stripped before building links.
     */
    private String getBaseUrl() {
        return getServiceUrl().replace("?", "");
    }

    /**
     * Builds the STAC landing page, with links to the {@code /conformance} and
     * {@code /collections} endpoints.
     */
    @Override
    public LandingPage getLandingPage() {
        final LandingPage landingPage = new LandingPage();
        landingPage.setTitle(getId());
        landingPage.setDescription("STAC catalog " + getId());
        landingPage.setLinks(List.of(
                selfLink(getBaseUrl(), "self"),
                selfLink(getBaseUrl(), "root"),
                selfLink(getBaseUrl() + "/conformance", "conformance"),
                selfLink(getBaseUrl() + "/collections", "data")));
        return landingPage;
    }

    /**
     * Declares conformance to STAC core, the STAC/OGC API - Features conformance class,
     * and OGC API - Common core.
     */
    @Override
    public ConfClasses getConformance() {
        final ConfClasses confClasses = new ConfClasses();
        confClasses.setConformsTo(List.of(
                "https://api.stacspec.org/v1.0.0/core",
                "https://api.stacspec.org/v1.0.0/ogcapi-features",
                "http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core"));
        return confClasses;
    }

    /**
     * Lists one STAC collection per layer configured on this service instance,
     * skipping (and logging) any layer that fails to build.
     */
    @Override
    public Collections getCollections() throws CstlServiceException {
        final List<Collection> collections = new ArrayList<>();
        for (LayerCache layer : getLayerCaches(getUserLogin())) {
            try {
                collections.add(buildCollection(layer));
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Failed to build STAC collection for layer " + layer.getName(), ex);
            }
        }
        return new Collections(collections, List.of(
                selfLink(getBaseUrl() + "/collections", "self"),
                selfLink(getBaseUrl(), "root")));
    }

    /**
     * @return the STAC collection for the given layer, or {@code null} if no layer with
     *         that identifier is configured on this service instance
     */
    @Override
    public Collection getCollection(String collectionId) throws CstlServiceException {
        final LayerCache layer = findLayer(collectionId);
        if (layer == null) {
            return null;
        }
        try {
            return buildCollection(layer);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    /**
     * @return the single-item collection representing the whole given layer, or {@code null}
     *         if no layer with that identifier is configured on this service instance
     */
    @Override
    public ItemCollection getItems(String collectionId) throws CstlServiceException {
        final LayerCache layer = findLayer(collectionId);
        if (layer == null) {
            return null;
        }
        try {
            final ItemCollection itemCollection = new ItemCollection();
            itemCollection.setFeatures(List.of(buildItem(layer)));
            itemCollection.setLinks(List.of(
                    selfLink(getBaseUrl() + "/collections/" + collectionId + "/items", "self"),
                    selfLink(getBaseUrl(), "root"),
                    selfLink(getBaseUrl() + "/collections/" + collectionId, "parent")));
            return itemCollection;
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    /**
     * @return the single item representing the given collection if {@code itemId} matches its
     *         identifier, or {@code null} if no such collection/item exists
     */
    @Override
    public Item getItem(String collectionId, String itemId) throws CstlServiceException {
        final LayerCache layer = findLayer(collectionId);
        if (layer == null || !layer.getName().getLocalPart().equals(itemId)) {
            return null;
        }
        try {
            return buildItem(layer);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException(ex);
        }
    }

    /**
     * Looks up the configured layer backing the given collection identifier.
     *
     * @return the matching layer, or {@code null} if none matches
     */
    private LayerCache findLayer(String collectionId) throws CstlServiceException {
        for (LayerCache layer : getLayerCaches(getUserLogin())) {
            if (layer.getName().getLocalPart().equals(collectionId)) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Builds the STAC collection representing the given layer.
     */
    private Collection buildCollection(LayerCache layer) throws ConstellationStoreException {
        final QName name = layer.getName();
        final String id = name.getLocalPart();
        final Collection collection = new Collection();
        collection.setId(id);
        collection.setTitle(id);
        collection.setDescription(id);
        collection.setItemType("feature");
        collection.setStacVersion(STAC_VERSION);
        collection.setExtent(buildExtent(layer));
        collection.setLinks(List.of(
                selfLink(getBaseUrl() + "/collections/" + id, "self"),
                selfLink(getBaseUrl() + "/collections/" + id + "/items", "items"),
                selfLink(getBaseUrl(), "root"),
                selfLink(getBaseUrl(), "parent")));
        return collection;
    }

    /**
     * Builds the single STAC item representing the whole extent of the given layer, per the
     * STAC best practices for vector data (whole-dataset item, bbox as geometry).
     */
    private Item buildItem(LayerCache layer) throws ConstellationStoreException {
        final QName name = layer.getName();
        final String id = name.getLocalPart();
        final List<Double> box = bbox(layer);
        final Item item = new Item();
        item.setId(id);
        item.setCollection(id);
        item.setStacVersion(STAC_VERSION);
        item.setBbox(box);
        item.setGeometry(bboxGeometry(box));
        item.setProperties(itemProperties(layer));
        item.setLinks(List.of(
                selfLink(getBaseUrl() + "/collections/" + id + "/items/" + id, "self"),
                selfLink(getBaseUrl() + "/collections/" + id, "collection"),
                selfLink(getBaseUrl() + "/collections/" + id, "parent"),
                selfLink(getBaseUrl(), "root")));
        return item;
    }

    /**
     * Builds a GeoJSON Polygon geometry tracing the given {@code [west, south, east, north]}
     * bbox, since the item's geometry is the whole-dataset envelope (no per-feature geometry).
     */
    private static JsonNode bboxGeometry(List<Double> box) {
        final double west = box.get(0), south = box.get(1), east = box.get(2), north = box.get(3);
        final ArrayNode ring = MAPPER.createArrayNode();
        ring.add(point(west, south)).add(point(east, south)).add(point(east, north)).add(point(west, north)).add(point(west, south));
        final ObjectNode geometry = MAPPER.createObjectNode();
        geometry.put("type", "Polygon");
        geometry.set("coordinates", MAPPER.createArrayNode().add(ring));
        return geometry;
    }

    private static ArrayNode point(double x, double y) {
        return MAPPER.createArrayNode().add(x).add(y);
    }

    /**
     * Builds the STAC {@code properties} of the whole-dataset item, in particular the required
     * {@code datetime} field. Falls back to the layer's date range end when available; when a
     * layer exposes no temporal information at all, {@code datetime} is left {@code null} —
     * ponytail: this technically leaves such items short of the "datetime or start/end" STAC
     * validation rule, upgrade if a data source without any date metadata needs to validate.
     */
    private static Map<String, Object> itemProperties(LayerCache layer) throws ConstellationStoreException {
        final Map<String, Object> properties = new HashMap<>();
        final SortedSet<Date> dateRange = layer.getDateRange();
        properties.put("datetime", dateRange != null && !dateRange.isEmpty()
                ? dateRange.last().toInstant().atOffset(java.time.ZoneOffset.UTC).toString()
                : null);
        return properties;
    }

    /**
     * Builds the spatial (and, if available, temporal) extent of the given layer.
     */
    private Extent buildExtent(LayerCache layer) throws ConstellationStoreException {
        final Extent extent = new Extent();
        final SpatialExtent spatial = new SpatialExtent();
        final List<Double> box = bbox(layer);
        spatial.setBbox(new double[][]{{box.get(0), box.get(1), box.get(2), box.get(3)}});
        spatial.setCrs(SpatialExtent.CRS84);
        extent.setSpatial(spatial);

        final SortedSet<Date> dateRange = layer.getDateRange();
        if (dateRange != null && !dateRange.isEmpty()) {
            final TemporalExtent temporal = new TemporalExtent();
            temporal.setInterval(new java.time.OffsetDateTime[][]{{
                    dateRange.first().toInstant().atOffset(java.time.ZoneOffset.UTC),
                    dateRange.last().toInstant().atOffset(java.time.ZoneOffset.UTC)}});
            extent.setTemporal(temporal);
        }
        return extent;
    }

    /**
     * Returns the layer's geographic bounding box as {@code [west, south, east, north]},
     * or the whole globe if the layer exposes no bounding box.
     */
    private List<Double> bbox(LayerCache layer) throws ConstellationStoreException {
        final GeographicBoundingBox bbox = layer.getGeographicBoundingBox();
        if (bbox == null) {
            return List.of(-180.0, -90.0, 180.0, 90.0);
        }
        return List.of(bbox.getWestBoundLongitude(), bbox.getSouthBoundLatitude(),
                        bbox.getEastBoundLongitude(), bbox.getNorthBoundLatitude());
    }

    /**
     * Builds a JSON {@link Link} with the given href/rel, typed as {@code application/json}.
     */
    private static Link selfLink(String href, String rel) {
        final Link link = new Link();
        link.setHref(href);
        link.setRel(rel);
        link.setType("application/json");
        return link;
    }
}
