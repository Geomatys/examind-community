/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.examind.edr.core;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverageProcessor;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.coverage.grid.GridRoundingMode;
import org.apache.sis.coverage.grid.IllegalGridGeometryException;
import org.apache.sis.coverage.grid.IncompleteGridGeometryException;
import javax.measure.format.MeasurementParseException;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.MemoryGridCoverageResource;
import org.apache.sis.storage.coveragejson.CoverageJsonStore;
import org.apache.sis.util.iso.Names;
import org.constellation.api.ServiceDef;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.CoverageData;
import org.constellation.provider.Data;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.LayerCache;
import org.constellation.ws.LayerWorker;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogcapi.dto.LinkRelations;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.Collections;
import org.geotoolkit.ogcapi.dto.common.Extent;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.ogcapi.dto.common.SpatialExtent;
import org.geotoolkit.ogcapi.dto.common.TemporalExtent;
import org.geotoolkit.ogcapi.dto.edr.DataQueries;
import org.geotoolkit.ogcapi.dto.edr.DataQueryLink;
import org.geotoolkit.ogcapi.dto.edr.EdrCollection;
import org.geotoolkit.ogcapi.dto.edr.EdrInstances;
import org.geotoolkit.ogcapi.request.common.GetCollection;
import org.geotoolkit.ogcapi.request.common.GetCollectionList;
import org.geotoolkit.ogcapi.request.edr.AbstractEdrDataQuery;
import org.geotoolkit.ogcapi.request.edr.GetArea;
import org.geotoolkit.ogcapi.request.edr.GetCorridor;
import org.geotoolkit.ogcapi.request.edr.GetCube;
import org.geotoolkit.ogcapi.request.edr.GetEdrInstances;
import org.geotoolkit.ogcapi.request.edr.GetPosition;
import org.geotoolkit.ogcapi.request.edr.GetRadius;
import org.geotoolkit.ogcapi.request.edr.GetTrajectory;
import org.geotoolkit.ows.xml.OWSExceptionCode;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Default {@link EDRWorker} implementation.
 * <p>
 * Like {@code DefaultDGGSWorker}, this bridges examind's own data-provider layer
 * ({@link LayerWorker#getLayerCache(String, String) LayerCache} / {@link Data}) to the OGC API - EDR
 * DTOs, instead of delegating to another service's worker (WCS). Every EDR {@code data_queries} shape
 * (position/radius/area/cube/trajectory/corridor) is reduced to a spatial (and, best-effort, temporal)
 * {@link Envelope}, extracted from the underlying {@link CoverageData} via
 * {@link CoverageData#getCoverage(Envelope, java.awt.Dimension)}, then encoded as CoverageJSON.
 *
 * @author Quentin Bialota (Geomatys)
 */
@Component("EDRWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultEDRWorker extends LayerWorker implements EDRWorker {

    private static final GeographicCRS CRS84 = CommonCRS.WGS84.normalizedGeographic();

    /** Length of one degree of latitude (and of longitude at the equator) on a sphere, in metres. */
    static final double METRES_PER_DEGREE = 111_320d;

    static final String COVERAGEJSON_MIME = "application/prs.coverage+json";

    /**
     * Default maximum number of grid cells a single data query may read (all dimensions multiplied),
     * overridden by {@link AppProperty#EXA_EDR_MAX_CELLS} (read on every query so it can be tuned at runtime).
     * Queries are extracted at native resolution, so without this guard an area/cube over a large
     * raster would load the whole coverage in memory.
     */
    static final long DEFAULT_MAX_CELLS = 10_000_000L;

    public DefaultEDRWorker(String id) {
        super(id, ServiceDef.Specification.EDR);
        started();
    }

    @Override
    public synchronized void refreshUpdateSequence() {
        // no landing-page/collections cache yet (unlike DGGS), nothing to invalidate.
    }

    @Override
    public synchronized void clearCapabilitiesCache() {
        // no landing-page/collections cache yet (unlike DGGS), nothing to invalidate.
    }

    /**
     * @return this service instance's base URL, with the trailing {@code ?} of {@link #getServiceUrl()} stripped.
     */
    private String getServicePath() {
        return getServiceUrl().replace("?", "");
    }

    /**
     * Encodes a collection id (a raw layer name, which may contain a namespace separated by {@code :}
     * or {@code /}) into a value safe to use as a single URL path segment.
     * <p>
     * Only {@code /} needs the round-trip below: a literal slash cannot appear inside one path segment, so
     * it is turned into the percent-encoded caret {@code %5E}. The servlet container decodes that to a literal
     * {@code ^} before the controller ever sees it, and {@link #decodeCollectionId(String)} then restores the
     * {@code /}. A literal {@code :} needs no such trick — standard percent-decoding of {@code %3A} back to
     * {@code :} happens automatically, so only the encode side handles it.
     *
     * @param name the raw layer name.
     * @return the path-segment-safe collection id, or {@code null} if {@code name} is {@code null}.
     */
    private static String encodeCollectionId(String name) {
        if (name == null) return null;
        return name.replace("/", "%5E").replace(":", "%3A");
    }

    /**
     * Reverses {@link #encodeCollectionId(String)}'s caret substitution back to the original {@code /}.
     * See that method for why {@code :} needs no equivalent handling here.
     *
     * @param name the collection id as received from the path.
     * @return the raw layer name, or {@code null} if {@code name} is {@code null}.
     */
    private static String decodeCollectionId(String name) {
        if (name == null) return null;
        return name.replace("^", "/");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Static links only (self, api, conformance, collections) — no per-instance state to expose.
     */
    @Override
    public LandingPage getLandingPage() throws CstlServiceException {
        final LandingPage dto = new LandingPage();
        dto.setTitle("OGC-API EDR by Examind");
        final String base = getServicePath();
        dto.addLinksItem(new Link(base + "/", LinkRelations.SELF, MediaType.APPLICATION_JSON_VALUE, null, "This document", null));
        dto.addLinksItem(new Link(base + "/api", LinkRelations.SERVICE_DESC, null, null, "API definition", null));
        dto.addLinksItem(new Link(base + "/conformance", LinkRelations.OGC_CORE_CONFORMANCE, MediaType.APPLICATION_JSON_VALUE, null, "Conformance classes", null));
        dto.addLinksItem(new Link(base + "/collections", LinkRelations.OGC_DATA, MediaType.APPLICATION_JSON_VALUE, null, "Collections", null));
        return dto;
    }

    /**
     * {@inheritDoc}
     * <p>
     * One {@link CollectionDescription} per accessible {@link LayerCache}; {@code parameters} is
     * unused (no filtering/paging on the collections list yet).
     */
    @Override
    public Collections getCollectionList(GetCollectionList parameters) throws CstlServiceException {
        final String userLogin = getUserLogin();
        final List<LayerCache> layers = getLayerCaches(userLogin);

        final List<CollectionDescription> items = new ArrayList<>();
        for (LayerCache layer : layers) {
            // EDR queries only make sense on coverages: other layers of the service are not exposed.
            if (layer.getData() instanceof CoverageData) {
                items.add(describe(layer, false));
            }
        }

        final Collections dto = new Collections();
        dto.setCollections(items);
        dto.setNumberMatched(items.size());
        dto.setNumberReturned(items.size());
        return dto;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EdrCollection getCollection(GetCollection parameters) throws CstlServiceException {
        return describe(getCoverageLayer(parameters.getCollectionId()), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EdrInstances getInstances(GetEdrInstances parameters) throws CstlServiceException {
        // no multi-instance data providers yet (a la STAC item search) — one synthetic
        // instance per collection, matching the collection itself. Revisit once EDR needs to expose
        // several spatiotemporal-extent-scoped sub-collections per dataset.
        final EdrCollection instance = describe(getCoverageLayer(parameters.getCollectionId()), true);
        return new EdrInstances().instances(List.of(instance));
    }

    /**
     * Resolves a collection id to its layer, treating any non-coverage layer as unknown so that
     * {@code /collections/{id}} and the data queries stay consistent with the collections list.
     *
     * @param collectionId the collection id as received from the path.
     * @return the layer, whose data is guaranteed to be a {@link CoverageData}.
     * @throws CstlServiceException {@code LAYER_NOT_DEFINED} if the layer is unknown or not a coverage.
     */
    private LayerCache getCoverageLayer(String collectionId) throws CstlServiceException {
        final String name = decodeCollectionId(collectionId);
        final LayerCache layer = getLayerCache(getUserLogin(), name);
        if (!(layer.getData() instanceof CoverageData)) {
            throw new CstlServiceException("Collection " + name + " is not a coverage", OWSExceptionCode.LAYER_NOT_DEFINED);
        }
        return layer;
    }

    /**
     * Builds an {@link EdrCollection} description for a layer: id, title, supported CRS/output format,
     * extent, and the six data-query links (position/radius/area/cube/trajectory/corridor).
     *
     * @param layer the coverage layer to describe.
     * @param full   whether to also add the {@code self}/{@code instances} links — {@code true} for the
     *               single-collection {@code /collections/{id}} endpoint, {@code false} when listing all
     *               collections (where those per-collection links would just be noise).
     * @return the collection description.
     */
    private EdrCollection describe(LayerCache layer, boolean full) {
        final Data<?> data = layer.getData();

        final EdrCollection dto = new EdrCollection();
        final String id = encodeCollectionId(layer.getName().getLocalPart());
        dto.id(id);
        dto.title(layer.getName().getLocalPart());
        dto.setCrs(List.of(SpatialExtent.CRS84));
        dto.setOutputFormats(List.of(COVERAGEJSON_MIME));
        dto.setExtent(buildExtent(data));

        final String base = getServicePath() + "/collections/" + id;
        // no 'locations'/'items' link — that query type targets predefined point/station
        // locations, which examind's raster coverage collections don't have. Add it if a point/station
        // dataset needs EDR exposure.
        dto.setDataQueries(new DataQueries()
                .position(dataQueryLink(base + "/position", "position query"))
                .radius(dataQueryLink(base + "/radius", "radius query"))
                .area(dataQueryLink(base + "/area", "area query"))
                .cube(dataQueryLink(base + "/cube", "cube query"))
                .trajectory(dataQueryLink(base + "/trajectory", "trajectory query"))
                .corridor(dataQueryLink(base + "/corridor", "corridor query")));

        if (full) {
            dto.addLinksItem(new Link(base, LinkRelations.SELF, MediaType.APPLICATION_JSON_VALUE, null, "This collection", null));
            dto.addLinksItem(new Link(base + "/instances", LinkRelations.OGC_DATA, MediaType.APPLICATION_JSON_VALUE, null, "Instances", null));
        }
        return dto;
    }

    /**
     * Builds one {@code data_queries} link entry, always advertising the single CoverageJSON output
     * format this worker produces.
     *
     * @param href  the absolute URL of the query endpoint.
     * @param title a human-readable label for the query.
     * @return the link.
     */
    private static DataQueryLink dataQueryLink(String href, String title) {
        final DataQueryLink link = new DataQueryLink();
        link.setHref(href);
        link.setRel("data");
        link.setType(COVERAGEJSON_MIME);
        link.setTitle(title);
        return link;
    }

    /**
     * Derives a collection's {@link Extent} from its underlying data: the spatial bbox reprojected to
     * {@link #CRS84}, and, if the provider exposes one, the temporal min/max of its date range.
     * <p>
     * Either half is silently omitted (logged at {@code FINE}) rather than failing the whole collection
     * description if the provider cannot supply it — an extent-less collection is still usable, just
     * without discoverability metadata.
     *
     * @param data the coverage data to inspect.
     * @return the extent, with {@code spatial} and/or {@code temporal} left unset where unavailable.
     */
    private Extent buildExtent(Data<?> data) {
        final Extent extent = new Extent();
        try {
            final Envelope env = data.getEnvelope(CRS84);
            if (env != null) {
                extent.setSpatial(new SpatialExtent().bbox(new double[][]{{
                        env.getMinimum(0), env.getMinimum(1), env.getMaximum(0), env.getMaximum(1)
                }}));
            }
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }
        try {
            final SortedSet<Date> dates = data.getDateRange();
            if (dates != null && !dates.isEmpty()) {
                extent.setTemporal(new TemporalExtent().interval(new java.time.OffsetDateTime[][]{{
                        dates.first().toInstant().atOffset(java.time.ZoneOffset.UTC),
                        dates.last().toInstant().atOffset(java.time.ZoneOffset.UTC)
                }}));
            }
        } catch (ConstellationStoreException ex) {
            LOGGER.log(Level.FINE, ex.getMessage(), ex);
        }
        return extent;
    }

    /**
     * {@inheritDoc}
     * <p>
     * No buffer around the point: the coverage is queried exactly at its envelope, i.e. effectively at
     * native resolution around that single coordinate.
     */
    @Override
    public ResponseObject getPosition(GetPosition parameters) throws CstlServiceException {
        unsupported("z", parameters.getZ());
        unsupported("limit", parameters.getLimit());
        return queryCoverage(parameters, envelopeOf(parameters, requireGeometry(parameters.getCoords(), "coords"), 0), null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The point is buffered by {@code within}/{@code within-units} (see {@link #toMetres}) to build the
     * query envelope: the result is the square bounding the requested circle, not the circle itself.
     */
    @Override
    public ResponseObject getRadius(GetRadius parameters) throws CstlServiceException {
        unsupported("z", parameters.getZ());
        unsupported("limit", parameters.getLimit());
        final double metres = toMetres(parameters.getWithin(), parameters.getWithinUnits(), "within-units");
        return queryCoverage(parameters, envelopeOf(parameters, requireGeometry(parameters.getCoords(), "coords"), metres), null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The polygon's own bounding box is used as the query envelope — no clipping to the polygon shape.
     */
    @Override
    public ResponseObject getArea(GetArea parameters) throws CstlServiceException {
        unsupported("z", parameters.getZ());
        unsupported("limit", parameters.getLimit());
        return queryCoverage(parameters, envelopeOf(parameters, requireGeometry(parameters.getCoords(), "coords"), 0),
                outputSize(parameters.getResolutionX(), parameters.getResolutionY()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Unlike the other five queries, {@code bbox} is already an {@link Envelope} (not WKT), so no
     * {@link #envelopeOf} conversion is needed — its own CRS is used if set, else {@code crs} is resolved.
     */
    @Override
    public ResponseObject getCube(GetCube parameters) throws CstlServiceException {
        final Envelope bbox = parameters.getBbox();
        if (bbox == null) {
            throw new CstlServiceException("The 'bbox' parameter is required for the cube query", OWSExceptionCode.MISSING_PARAMETER_VALUE, "bbox");
        }
        unsupported("z", parameters.getZ());
        unsupported("resolution-z", parameters.getResolutionZ());
        final CoordinateReferenceSystem crs = (bbox.getCoordinateReferenceSystem() != null)
                ? bbox.getCoordinateReferenceSystem() : resolveCrs(parameters.getCrs());
        final GeneralEnvelope env = new GeneralEnvelope(bbox);
        if (bbox.getCoordinateReferenceSystem() == null) env.setCoordinateReferenceSystem(crs);
        return queryCoverage(parameters, env, outputSize(parameters.getResolutionX(), parameters.getResolutionY()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * The line's bounding box is used as the query envelope: the whole grid under it is returned,
     * not only the cells crossed by the line.
     */
    @Override
    public ResponseObject getTrajectory(GetTrajectory parameters) throws CstlServiceException {
        // Simplification: bbox of the line; real sampling would evaluate the coverage per vertex (and honor Z/M coordinates).
        return queryCoverage(parameters, envelopeOf(parameters, requireGeometry(parameters.getCoords(), "coords"), 0), null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Buffer is halved because {@code corridor-width} is a full width straddling the centerline, while
     * {@link #envelopeOf} expects a one-sided buffer distance. As for trajectory, the whole grid under the
     * buffered bounding box is returned.
     */
    @Override
    public ResponseObject getCorridor(GetCorridor parameters) throws CstlServiceException {
        unsupported("z", parameters.getZ());
        unsupported("corridor-height", parameters.getCorridorHeight());
        unsupported("height-units", parameters.getHeightUnits());
        final double metres = toMetres(parameters.getCorridorWidth(), parameters.getWidthUnits(), "width-units") / 2;
        return queryCoverage(parameters, envelopeOf(parameters, requireGeometry(parameters.getCoords(), "coords"), metres),
                outputSize(parameters.getResolutionX(), parameters.getResolutionY()));
    }

    /**
     * Guards against a missing {@code coords} geometry, since {@code AbstractEdrDataQuery} models it as
     * an optional field even though every query but cube requires it.
     *
     * @param coords    the parsed geometry (already validated as valid WKT by the REST layer), or {@code null}.
     * @param paramName the query-parameter name to report, for the exception message.
     * @return {@code coords}, never {@code null}.
     * @throws CstlServiceException with {@code MISSING_PARAMETER_VALUE} if {@code coords} is {@code null}.
     */
    private static Geometry requireGeometry(Geometry coords, String paramName) throws CstlServiceException {
        if (coords == null) {
            throw new CstlServiceException("The '" + paramName + "' parameter is required", OWSExceptionCode.MISSING_PARAMETER_VALUE, paramName);
        }
        return coords;
    }

    /**
     * Rejects a query parameter this implementation does not honor yet, rather than silently ignoring it
     * and returning data the client did not ask for.
     *
     * @throws CstlServiceException {@code OPERATION_NOT_SUPPORTED} (HTTP 400) if {@code value} is set.
     */
    private static void unsupported(String name, Object value) throws CstlServiceException {
        if (value != null && !(value instanceof String str && str.isBlank())) {
            throw new CstlServiceException("The '" + name + "' parameter is not supported by this service yet.",
                    OWSExceptionCode.OPERATION_NOT_SUPPORTED, name);
        }
    }

    /**
     * Converts a distance expressed in {@code unit} (any linear UCUM-like symbol understood by
     * {@link Units#valueOf(String)}: {@code m}, {@code km}, {@code mi}, {@code ft}...) to metres.
     *
     * @param distance  the distance, or {@code null} for none (returns 0).
     * @param unit      the unit symbol, or {@code null}/blank for metres.
     * @param paramName the unit parameter name, for the error message.
     * @throws CstlServiceException {@code INVALID_PARAMETER_VALUE} for a negative distance or a unit that is
     *                               unknown or not a length.
     */
    static double toMetres(Double distance, String unit, String paramName) throws CstlServiceException {
        if (distance == null) return 0;
        if (!(distance >= 0)) {
            throw new CstlServiceException("The distance must be a positive number: " + distance, OWSExceptionCode.INVALID_PARAMETER_VALUE, paramName);
        }
        if (unit == null || unit.isBlank()) return distance;
        try {
            return Units.ensureLinear(Units.valueOf(unit.trim())).getConverterTo(Units.METRE).convert(distance.doubleValue());
        } catch (MeasurementParseException | IllegalArgumentException ex) {
            throw new CstlServiceException("Invalid '" + paramName + "' parameter: " + unit + " is not a known length unit.",
                    ex, OWSExceptionCode.INVALID_PARAMETER_VALUE, paramName);
        }
    }

    /**
     * Converts a buffer distance in metres to a buffer along each of the two axes of {@code crs}.
     * <p>
     * Geographic CRS (axes normalized to longitude, latitude by {@link #resolveCrs}): metres are converted
     * to degrees on a sphere, the longitude buffer being widened by {@code 1/cos(latitude)} at the
     * highest latitude of the envelope, so the buffer is never smaller than requested.
     * Projected CRS: metres are converted to the axis unit, ignoring the projection scale factor.
     *
     * @param crs    the (2D, horizontal) query CRS.
     * @param maxAbsLatitude the highest absolute latitude of the query geometry, used for geographic CRS only.
     * @param metres the buffer distance in metres.
     * @return {@code {bufferAxis0, bufferAxis1}} in the CRS axis units.
     * @throws CstlServiceException if an axis unit is neither angular nor linear.
     */
    static double[] bufferInCrsUnits(CoordinateReferenceSystem crs, double maxAbsLatitude, double metres) throws CstlServiceException {
        if (metres == 0) return new double[] {0, 0};
        if (crs instanceof GeographicCRS) {
            // Simplification: spherical approximation (1 degree of latitude = 111.32 km), use a geodesic buffer if precision matters.
            final double lat = metres / METRES_PER_DEGREE;
            final double cos = Math.cos(Math.toRadians(Math.min(maxAbsLatitude + lat, 89)));
            return new double[] {lat / cos, lat};
        }
        final double[] buffer = new double[2];
        for (int i = 0; i < 2; i++) {
            try {
                buffer[i] = Units.METRE.getConverterTo(Units.ensureLinear(crs.getCoordinateSystem().getAxis(i).getUnit())).convert(metres);
            } catch (IllegalArgumentException ex) {
                throw new CstlServiceException("Distances are not supported in the '" + crs.getName() + "' reference system.",
                        ex, OWSExceptionCode.INVALID_PARAMETER_VALUE, "crs");
            }
        }
        return buffer;
    }

    /**
     * Converts the EDR {@code resolution-x}/{@code resolution-y} parameters (number of sample points along
     * each axis) to the output grid size given to {@link CoverageData#getCoverage(Envelope, Dimension)}.
     *
     * @return the output size, or {@code null} (native resolution) if neither parameter is given.
     * @throws CstlServiceException {@code INVALID_PARAMETER_VALUE} if only one is given, or if a value is not
     *                               a strictly positive integer.
     */
    static Dimension outputSize(Double resolutionX, Double resolutionY) throws CstlServiceException {
        if (resolutionX == null && resolutionY == null) return null;
        return new Dimension(sampleCount(resolutionX, "resolution-x"), sampleCount(resolutionY, "resolution-y"));
    }

    private static int sampleCount(Double value, String name) throws CstlServiceException {
        if (value == null) {
            throw new CstlServiceException("'resolution-x' and 'resolution-y' must be given together.", OWSExceptionCode.MISSING_PARAMETER_VALUE, name);
        }
        if (!(value >= 1) || value > Integer.MAX_VALUE || value != Math.rint(value)) {
            throw new CstlServiceException("The '" + name + "' parameter must be a positive integer (number of sample points): " + value,
                    OWSExceptionCode.INVALID_PARAMETER_VALUE, name);
        }
        return value.intValue();
    }

    /**
     * Resolves the {@code crs} query parameter to a 2D horizontal CRS, defaulting to {@link #CRS84} when absent.
     * <p>
     * Axes are normalized to the (x, y) / (longitude, latitude) order, which is the order of WKT coordinates
     * and bbox values: {@code crs=EPSG:4326} is thus read as lon/lat, like {@code CRS84}.
     *
     * @param crsParam the {@code crs} parameter value, or {@code null}/blank for the default.
     * @return the resolved CRS.
     * @throws CstlServiceException with {@code INVALID_PARAMETER_VALUE} if {@code crsParam} does not resolve
     *                               to a known authority code, or has no horizontal component.
     */
    private static CoordinateReferenceSystem resolveCrs(String crsParam) throws CstlServiceException {
        if (crsParam == null || crsParam.isBlank()) return CRS84;
        final CoordinateReferenceSystem crs;
        try {
            crs = CRS.getHorizontalComponent(CRS.forCode(crsParam.trim()));
        } catch (FactoryException ex) {
            throw new CstlServiceException("Invalid 'crs' parameter : " + crsParam, ex, OWSExceptionCode.INVALID_PARAMETER_VALUE, "crs");
        }
        if (crs == null) {
            throw new CstlServiceException("The 'crs' parameter must have a horizontal component: " + crsParam, OWSExceptionCode.INVALID_PARAMETER_VALUE, "crs");
        }
        return AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
    }

    /**
     * Reduces a query geometry (point/polygon/line) plus an optional buffer to the {@link GeneralEnvelope}
     * used to extract a coverage — the common building block behind position/radius/area/trajectory/corridor.
     *
     * @param parameters the query, used only to resolve its {@code crs} parameter.
     * @param coords     the query geometry, already required non-null by the caller.
     * @param metres     a buffer distance in metres added on every side of the geometry's bounding box
     *                   (see {@link #bufferInCrsUnits}); {@code 0} for queries with no buffer notion.
     * @return the resulting envelope, in the resolved CRS.
     * @throws CstlServiceException if {@code crs} cannot be resolved or does not support distances.
     */
    private static GeneralEnvelope envelopeOf(AbstractEdrDataQuery parameters, Geometry coords, double metres) throws CstlServiceException {
        final CoordinateReferenceSystem crs = resolveCrs(parameters.getCrs());
        final org.locationtech.jts.geom.Envelope env = coords.getEnvelopeInternal();
        final double[] buffer = bufferInCrsUnits(crs, Math.max(Math.abs(env.getMinY()), Math.abs(env.getMaxY())), metres);
        final GeneralEnvelope ge = new GeneralEnvelope(crs);
        ge.setRange(0, env.getMinX() - buffer[0], env.getMaxX() + buffer[0]);
        ge.setRange(1, env.getMinY() - buffer[1], env.getMaxY() + buffer[1]);
        return ge;
    }

    /**
     * Central execution path shared by all six EDR data queries: resolves the target layer, requires it
     * to be a {@link CoverageData} (EDR has nothing meaningful to return for vector/other data types),
     * extracts a {@link GridCoverage} over {@code spatialEnvelope}, either at native resolution or resampled to
     * {@code outputSize} ({@code resolution-x/y}), and encodes the result as CoverageJSON.
     *
     * @param parameters       the originating query, used only for its {@code collectionId}.
     * @param spatialEnvelope  the envelope to extract, as built by {@link #envelopeOf} or (for cube) directly
     *                         from {@code bbox}.
     * @param outputSize       the output grid size, or {@code null} for native resolution.
     * @return a {@link ResponseObject} wrapping the CoverageJSON bytes with the {@code application/prs.coverage+json} mime type.
     * @throws CstlServiceException if the collection is unknown, is not backed by coverage data, or the
     *                               coverage extraction/encoding fails.
     */
    private ResponseObject queryCoverage(AbstractEdrDataQuery parameters, Envelope spatialEnvelope, Dimension outputSize) throws CstlServiceException {
        final String collectionId = decodeCollectionId(parameters.getCollectionId());
        final CoverageData covData = (CoverageData) getCoverageLayer(parameters.getCollectionId()).getData();

        final Envelope queryEnvelope = applyDatetime(spatialEnvelope, covData, parameters.getDatetime());
        checkCellCount(covData, queryEnvelope, outputSize);

        GridCoverage coverage;
        try {
            coverage = covData.getCoverage(queryEnvelope, outputSize);
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException("Failed to extract coverage for collection " + collectionId, ex);
        }
        coverage = (outputSize != null) ? resample(coverage, queryEnvelope, outputSize) : crop(coverage, queryEnvelope);
        coverage = applyParameterNames(coverage, parameters.getParameterName());

        try {
            final GenericName name = Names.createLocalName(null, null, collectionId);
            return new ResponseObject(toCoverageJson(name, coverage), COVERAGEJSON_MIME);
        } catch (IOException | DataStoreException ex) {
            throw new CstlServiceException("Failed to encode coverage to CoverageJSON", ex);
        }
    }

    /**
     * Crops {@code coverage} to the cells touched by {@code envelope}. Stores may return more than requested
     * (e.g. a GeoTIFF read is aligned on its tiles/strips), so without this a position query on a
     * 4-row-strip TIFF returns 4 values instead of 1.
     */
    private static GridCoverage crop(GridCoverage coverage, Envelope envelope) throws CstlServiceException {
        final GridGeometry grid = coverage.getGridGeometry();
        final GridGeometry target;
        try {
            target = grid.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(envelope).build();
        } catch (IllegalGridGeometryException | IncompleteGridGeometryException ex) {
            LOGGER.log(Level.FINE, "Unable to crop the coverage to the query envelope, returning it as read", ex);
            return coverage;
        }
        if (target.getExtent().equals(grid.getExtent())) {
            return coverage;
        }
        try {
            return new GridCoverageProcessor().resample(coverage, target);
        } catch (TransformException ex) {
            throw new CstlServiceException("Failed to crop the coverage to the query envelope", ex);
        }
    }

    /**
     * Resamples {@code coverage} to exactly {@code outputSize} points over {@code envelope}: the store read only
     * uses the requested size as a hint (integer subsampling), while EDR {@code resolution-x/y} ask for an exact
     * number of points. The grid is built like {@code DefaultCoverageData.getCoverage} does.
     */
    private static GridCoverage resample(GridCoverage coverage, Envelope envelope, Dimension outputSize) throws CstlServiceException {
        final long[] high = new long[envelope.getDimension()];
        high[0] = outputSize.width - 1;
        high[1] = outputSize.height - 1;
        final GridGeometry target = new GridGeometry(new GridExtent(null, null, high, true), envelope, GridOrientation.REFLECTION_Y);
        try {
            return new GridCoverageProcessor().resample(coverage, target);
        } catch (TransformException ex) {
            throw new CstlServiceException("Failed to resample the coverage to the requested resolution", ex);
        }
    }

    /**
     * Rejects a query whose native-resolution extraction would exceed the maximum cell count
     * (see {@link AppProperty#EXA_EDR_MAX_CELLS}), before any pixel is read. When {@code outputSize} is given, the
     * output grid size is checked instead.
     * <p>
     * The estimate multiplies every dimension of the sub-grid intersecting {@code queryEnvelope}; dimensions
     * the envelope does not constrain (e.g. time when no {@code datetime} is given) count with their full
     * extent, so the estimate errs on the safe side. If the sub-grid cannot be computed (disjoint envelope,
     * incomplete grid geometry), the check is skipped and the extraction itself reports the problem.
     *
     * @throws CstlServiceException {@code FILE_SIZE_EXCEEDED} (mapped to HTTP 413) if the limit is exceeded.
     */
    private static void checkCellCount(CoverageData data, Envelope queryEnvelope, Dimension outputSize) throws CstlServiceException {
        final long maxCells = Application.getLongProperty(AppProperty.EXA_EDR_MAX_CELLS, DEFAULT_MAX_CELLS);
        if (outputSize != null) {
            if ((long) outputSize.width * outputSize.height > maxCells) {
                throw new CstlServiceException("The requested resolution exceeds " + maxCells + " sample points.", OWSExceptionCode.FILE_SIZE_EXCEEDED);
            }
            return;
        }
        final GridExtent extent;
        try {
            final GridGeometry grid = data.getGeometry();
            if (grid == null || !grid.isDefined(GridGeometry.EXTENT)) return;
            // ENCLOSING, like the store read: every cell touched by the envelope counts (default NEAREST can drop edge cells).
            extent = grid.derive().rounding(GridRoundingMode.ENCLOSING).subgrid(queryEnvelope).getIntersection();
        } catch (ConstellationStoreException | IllegalGridGeometryException | IncompleteGridGeometryException ex) {
            LOGGER.log(Level.FINE, "Unable to estimate the EDR query size, skipping the check", ex);
            return;
        }
        long cells = 1;
        for (int i = 0; i < extent.getDimension(); i++) {
            final long size = extent.getSize(i);
            if (size > maxCells / cells) {
                throw new CstlServiceException("The query would read more than " + maxCells + " grid cells. "
                        + "Reduce the requested area or time range.", OWSExceptionCode.FILE_SIZE_EXCEEDED);
            }
            cells *= size;
        }
    }

    /**
     * Clips {@code spatialEnvelope} on its temporal axis when the query carries a {@code datetime}
     * parameter (a single RFC3339 instant, or a {@code start/end} interval using {@code ".."} for an
     * open bound). Returns {@code spatialEnvelope} unchanged when {@code datetime} is {@code null}.
     *
     * @throws CstlServiceException MISSING/INVALID_PARAMETER_VALUE if the collection has no temporal
     *                               axis, or {@code datetime} isn't a valid RFC3339 instant/interval.
     */
    private static Envelope applyDatetime(Envelope spatialEnvelope, CoverageData data, String datetime) throws CstlServiceException {
        if (datetime == null) {
            return spatialEnvelope;
        }
        final Envelope nativeEnvelope;
        try {
            nativeEnvelope = data.getEnvelope();
        } catch (ConstellationStoreException ex) {
            throw new CstlServiceException("Failed to determine the collection's temporal extent", ex);
        }
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(nativeEnvelope.getCoordinateReferenceSystem());
        if (temporalCRS == null) {
            throw new CstlServiceException("The 'datetime' parameter is not supported: this collection has no temporal axis.",
                    OWSExceptionCode.INVALID_PARAMETER_VALUE, "datetime");
        }
        final DefaultTemporalCRS timeAxis = DefaultTemporalCRS.castOrCopy(temporalCRS);
        final double[] range = parseDatetimeRange(datetime, timeAxis);

        final CoordinateReferenceSystem compoundCrs;
        try {
            compoundCrs = CRS.compound(spatialEnvelope.getCoordinateReferenceSystem(), temporalCRS);
        } catch (FactoryException ex) {
            throw new CstlServiceException("Failed to combine the spatial and temporal reference systems", ex);
        }
        final GeneralEnvelope combined = new GeneralEnvelope(compoundCrs);
        final int spatialDim = spatialEnvelope.getDimension();
        for (int i = 0; i < spatialDim; i++) {
            combined.setRange(i, spatialEnvelope.getMinimum(i), spatialEnvelope.getMaximum(i));
        }
        combined.setRange(spatialDim, range[0], range[1]);
        return combined;
    }

    /**
     * Parses an EDR {@code datetime} value into {@code [min, max]} numeric coordinates on {@code timeAxis}.
     * Accepts a single instant ({@code min == max}) or a {@code start/end} interval, either bound of which
     * may be {@code ".."} for an open (infinite) end.
     */
    private static double[] parseDatetimeRange(String datetime, DefaultTemporalCRS timeAxis) throws CstlServiceException {
        try {
            final int sep = datetime.indexOf('/');
            if (sep < 0) {
                final double value = timeAxis.toValue(Instant.parse(datetime));
                return new double[]{value, value};
            }
            final String startStr = datetime.substring(0, sep);
            final String endStr = datetime.substring(sep + 1);
            final double start = "..".equals(startStr) ? Double.NEGATIVE_INFINITY : timeAxis.toValue(Instant.parse(startStr));
            final double end = "..".equals(endStr) ? Double.POSITIVE_INFINITY : timeAxis.toValue(Instant.parse(endStr));
            return new double[]{start, end};
        } catch (DateTimeParseException ex) {
            throw new CstlServiceException("Invalid 'datetime' parameter: " + datetime + ". Expected a RFC3339 instant "
                    + "(e.g. 2021-05-17T12:00:00Z) or interval (e.g. 2021-05-17T00:00:00Z/2021-05-18T00:00:00Z, "
                    + "using '..' for an open bound).", ex, OWSExceptionCode.INVALID_PARAMETER_VALUE, "datetime");
        }
    }

    /**
     * Reduces {@code coverage} to only the bands named in {@code parameterNames} (EDR's {@code parameter-name}
     * query parameter), matched against each band's {@link SampleDimension#getName()}. Returns {@code coverage}
     * unchanged when {@code parameterNames} is {@code null}/empty.
     *
     * @throws CstlServiceException INVALID_PARAMETER_VALUE if a requested name matches no band.
     */
    private static GridCoverage applyParameterNames(GridCoverage coverage, List<String> parameterNames) throws CstlServiceException {
        if (parameterNames == null || parameterNames.isEmpty()) {
            return coverage;
        }
        final List<SampleDimension> bands = coverage.getSampleDimensions();
        final int[] indices = new int[parameterNames.size()];
        for (int i = 0; i < parameterNames.size(); i++) {
            final String requested = parameterNames.get(i);
            final int found = indexOfBand(bands, requested);
            if (found < 0) {
                throw new CstlServiceException("Unknown 'parameter-name' value: " + requested,
                        OWSExceptionCode.INVALID_PARAMETER_VALUE, "parameter-name");
            }
            indices[i] = found;
        }
        return new GridCoverageProcessor().selectSampleDimensions(coverage, indices);
    }

    /**
     * @return index of the band whose name matches {@code name} (case-insensitive), or -1 if none.
     */
    private static int indexOfBand(List<SampleDimension> bands, String name) {
        for (int b = 0; b < bands.size(); b++) {
            final GenericName bandName = bands.get(b).getName();
            if (bandName != null && bandName.toString().equalsIgnoreCase(name)) {
                return b;
            }
        }
        return -1;
    }

    /**
     * Encode a raw grid coverage to CoverageJSON bytes.
     * <p>
     * Mirrors {@code DefaultDGGSWorker.toCoverageJson}, reusing the same Apache SIS
     * {@code sis-coveragejson} store round-trip.
     */
    private static byte[] toCoverageJson(GenericName name, GridCoverage gridCoverage) throws IOException, DataStoreException {
        final Path temp = Files.createTempFile("edr", ".covjson");
        Files.delete(temp);
        try (CoverageJsonStore store = (CoverageJsonStore) DataStores.openWritable(temp, "CoverageJSON")) {
            store.add(new MemoryGridCoverageResource(null, name, gridCoverage, null));
        }
        try {
            return Files.readAllBytes(temp);
        } finally {
            Files.delete(temp);
        }
    }
}
