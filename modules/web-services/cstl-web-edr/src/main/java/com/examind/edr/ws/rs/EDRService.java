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
package com.examind.edr.ws.rs;

import com.examind.edr.core.EDRWorker;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.logging.Level;
import org.apache.sis.geometry.GeneralEnvelope;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.api.rest.I18nCodes;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogcapi.dto.Conformance;
import org.geotoolkit.ogcapi.dto.common.ConfClasses;
import org.geotoolkit.ogcapi.request.common.GetCollection;
import org.geotoolkit.ogcapi.request.common.GetCollectionList;
import org.geotoolkit.ogcapi.request.edr.GetArea;
import org.geotoolkit.ogcapi.request.edr.GetCorridor;
import org.geotoolkit.ogcapi.request.edr.GetCube;
import org.geotoolkit.ogcapi.request.edr.GetEdrInstances;
import org.geotoolkit.ogcapi.request.edr.GetPosition;
import org.geotoolkit.ogcapi.request.edr.GetRadius;
import org.geotoolkit.ogcapi.request.edr.GetTrajectory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.util.CodeList;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import static org.geotoolkit.ows.xml.OWSExceptionCode.FILE_SIZE_EXCEEDED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * REST facade for the {@link EDRWorker}. Mirrors {@code com.examind.dggs.ws.rs.DGGSService}: a thin
 * routing layer that builds a geotoolkit {@code org.geotoolkit.ogcapi.request.edr.*} request DTO per
 * endpoint and dispatches it to the worker via {@link #treatIncomingRequest(Object, EDRWorker)}.
 *
 * @author Quentin Bialota (Geomatys)
 */
@RestController
@RequestMapping("edr/{serviceId:.+}")
public final class EDRService extends OGCWebService<EDRWorker> {

    /**
     * HTTP 413, resolved by code because Spring renamed the constant ({@code PAYLOAD_TOO_LARGE} / {@code CONTENT_TOO_LARGE}).
     */
    private static final HttpStatus PAYLOAD_TOO_LARGE = HttpStatus.valueOf(413);

    public static final String SPECIFICATION_URL = "https://docs.ogc.org/is/19-086r6/19-086r6.html";

    private static final ConfClasses CONFCLASSES = new ConfClasses();

    static {
        CONFCLASSES.addConformsToItem(Conformance.CORE);
        CONFCLASSES.addConformsToItem(Conformance.CORE_LANDINGPAGE);
        CONFCLASSES.addConformsToItem(Conformance.CORE_JSON);
        CONFCLASSES.addConformsToItem(Conformance.COLLECTIONS_v1);
        CONFCLASSES.addConformsToItem(Conformance.EDR_CORE);
        CONFCLASSES.addConformsToItem(Conformance.EDR_COLLECTIONS);
        CONFCLASSES.addConformsToItem(Conformance.EDR_QUERIES);
        CONFCLASSES.addConformsToItem(Conformance.EDR_JSON);
        CONFCLASSES.addConformsToItem(Conformance.EDR_COVERAGEJSON);
    }

    public EDRService() {
        super(ServiceDef.Specification.EDR);
    }

    /**
     * Dispatches an already-built request DTO to the matching {@link EDRWorker} operation.
     * <p>
     * {@code objectRequest} is {@code null} only for the landing page (see {@link #getLandingPage}); every
     * other endpoint builds one of the {@code org.geotoolkit.ogcapi.request.*} DTOs below and passes it in.
     *
     * @param objectRequest the request DTO (or {@code null} for the landing page).
     * @param worker        the worker to dispatch to.
     * @return the response, or an error response built by {@link #handleException} if the worker throws.
     */
    @Override
    protected ResponseObject treatIncomingRequest(final Object objectRequest, final EDRWorker worker) {
        try {
            if (objectRequest instanceof GetCollectionList r) {
                return new ResponseObject(worker.getCollectionList(r), MediaType.APPLICATION_JSON);
            } else if (objectRequest instanceof GetCollection r) {
                return new ResponseObject(worker.getCollection(r), MediaType.APPLICATION_JSON);
            } else if (objectRequest instanceof GetEdrInstances r) {
                return new ResponseObject(worker.getInstances(r), MediaType.APPLICATION_JSON);
            } else if (objectRequest instanceof GetPosition r) {
                return worker.getPosition(r);
            } else if (objectRequest instanceof GetRadius r) {
                return worker.getRadius(r);
            } else if (objectRequest instanceof GetArea r) {
                return worker.getArea(r);
            } else if (objectRequest instanceof GetCube r) {
                return worker.getCube(r);
            } else if (objectRequest instanceof GetTrajectory r) {
                return worker.getTrajectory(r);
            } else if (objectRequest instanceof GetCorridor r) {
                return worker.getCorridor(r);
            } else {
                return new ResponseObject(worker.getLandingPage(), MediaType.APPLICATION_JSON);
            }
        } catch (CstlServiceException ex) {
            return handleException(ex);
        }
    }

    /**
     * Catch-all handler for exceptions that escape {@link #treatIncomingRequest} (i.e. anything other
     * than {@link CstlServiceException}, which is already turned into a proper error response there).
     *
     * @return a generic 500-ish {@link ErrorMessage} response wrapping the exception.
     */
    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getLocalizedMessage(), exc);
        return new ResponseObject(new ErrorMessage(exc), MediaType.APPLICATION_JSON, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Maps a {@link CstlServiceException} thrown by the worker to an HTTP error response.
     * <ul>
     *   <li>{@code LAYER_NOT_DEFINED} (unknown or non-coverage collection) : 404 with the i18n-ed
     *       {@code Collection.NOT_FOUND} message;</li>
     *   <li>{@code INVALID_PARAMETER_VALUE}, {@code MISSING_PARAMETER_VALUE}, {@code OPERATION_NOT_SUPPORTED} : 400;</li>
     *   <li>{@code FILE_SIZE_EXCEEDED} (query above the maximum cell count) : 413;</li>
     *   <li>any other code : 500.</li>
     * </ul>
     *
     * @param ex the exception thrown by the worker.
     * @return the mapped error response.
     */
    private ResponseObject handleException(CstlServiceException ex) {
        final CodeList<?> code = ex.getExceptionCode();
        if (LAYER_NOT_DEFINED.equals(code)) {
            LOGGER.log(Level.FINE, ex.getLocalizedMessage(), ex);
            return new ResponseObject(new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND), MediaType.APPLICATION_JSON, HttpStatus.NOT_FOUND);
        }
        final HttpStatus status;
        if (INVALID_PARAMETER_VALUE.equals(code) || MISSING_PARAMETER_VALUE.equals(code) || OPERATION_NOT_SUPPORTED.equals(code)) {
            status = HttpStatus.BAD_REQUEST;
        } else if (FILE_SIZE_EXCEEDED.equals(code)) {
            status = PAYLOAD_TOO_LARGE;
        } else {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ResponseObject(new ErrorMessage(ex), MediaType.APPLICATION_JSON, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // client error: not worth a stack trace in the server logs.
        LOGGER.log(Level.FINE, ex.getLocalizedMessage(), ex);
        return new ResponseObject(new ErrorMessage(status, ex.getLocalizedMessage()), MediaType.APPLICATION_JSON, status);
    }

    /**
     * {@code GET /WS/edr/{serviceId}/} — the OGC API landing page. No query parameters.
     */
    @RequestMapping(method = GET, value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getLandingPage(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        return treatIncomingRequest(null).getResponseEntity();
    }

    /**
     * {@code GET /WS/edr/{serviceId}/api} — redirects to the static OGC API - EDR specification document.
     */
    @RequestMapping(method = GET, value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public RedirectView getApi() {
        // no generated OpenAPI document yet — point clients at the spec itself.
        return new RedirectView(SPECIFICATION_URL);
    }

    /**
     * {@code GET /WS/edr/{serviceId}/conformance} — the static list of conformance classes this
     * service claims (see {@link #CONFCLASSES}). No query parameters.
     */
    @RequestMapping(method = GET, value = "/conformance", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getConformance(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        return new ResponseObject(CONFCLASSES, MediaType.APPLICATION_JSON).getResponseEntity();
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections} — lists every collection. No query parameters.
     */
    @RequestMapping(method = GET, value = "/collections", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getCollectionList(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        return treatIncomingRequest(new GetCollectionList()).getResponseEntity();
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}} — describes one collection.
     * Falls through to the worker, which raises {@code LAYER_NOT_DEFINED} (mapped to 404 by
     * {@link #handleException}) if {@code collectionId} is unknown.
     */
    @RequestMapping(method = GET, value = "/collections/{collectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getCollection(@PathVariable("serviceId") String serviceId,
                                         @PathVariable("collectionId") String collectionId) {
        putServiceIdParam(serviceId);
        return treatIncomingRequest(new GetCollection().collectionId(collectionId)).getResponseEntity();
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/instances} — lists the collection's
     * instances (always a single synthetic instance, see {@code DefaultEDRWorker.getInstances}).
     * Same 404-on-unknown-collection behavior as {@link #getCollection}.
     */
    @RequestMapping(method = GET, value = "/collections/{collectionId}/instances", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getInstances(@PathVariable("serviceId") String serviceId,
                                        @PathVariable("collectionId") String collectionId) {
        putServiceIdParam(serviceId);
        return treatIncomingRequest(new GetEdrInstances().collectionId(collectionId)).getResponseEntity();
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/position} — position query.
     * Required: {@code coords} (WKT {@code POINT}). Optional: {@code z}, {@code datetime},
     * {@code parameter-name}, {@code crs}, {@code limit}.
     * <p>
     * Missing {@code coords} or invalid WKT both yield a 400 (via {@link #missingParam}/{@link #badCoords});
     * anything else falls through to the worker.
     * <p>
     * Also reachable as {@code GET /collections/{collectionId}/instances/{instanceId}/position}, which
     * 404s (via {@link #unknownInstance}) unless {@code instanceId} matches the collection's own id
     * (its single synthetic instance, see {@code DefaultEDRWorker.getInstances}).
     */
    @RequestMapping(method = GET, value = {"/collections/{collectionId}/position", "/collections/{collectionId}/instances/{instanceId}/position"})
    public ResponseEntity position(@PathVariable("serviceId") String serviceId,
                                    @PathVariable("collectionId") String collectionId,
                                    @PathVariable(name = "instanceId", required = false) String instanceId,
                                    @RequestParam(name = "coords", required = false) String coords,
                                    @RequestParam(name = "z", required = false) String z,
                                    @RequestParam(name = "datetime", required = false) String datetime,
                                    @RequestParam(name = "parameter-name", required = false) List<String> parameterName,
                                    @RequestParam(name = "crs", required = false) String crs,
                                    @RequestParam(name = "limit", required = false) Integer limit) {
        putServiceIdParam(serviceId);
        if (instanceId != null && !instanceId.equals(collectionId)) {
            return unknownInstance(collectionId, instanceId);
        }
        if (coords == null) {
            return missingParam("coords", "POINT(2.35 48.85)");
        }
        try {
            final Geometry g = parseCoords(coords, Point.class, MultiPoint.class);
            return treatIncomingRequest(new GetPosition().collectionId(collectionId).coords(g)
                    .z(z).datetime(datetime).parameterName(parameterName).crs(crs).limit(limit)).getResponseEntity();
        } catch (ParseException ex) {
            return badCoords(ex);
        }
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/radius} — radius query.
     * Required: {@code coords} (WKT {@code POINT}) and {@code within}. Optional: {@code within-units} (metres by default),
     * {@code z}, {@code datetime}, {@code parameter-name}, {@code crs}, {@code limit}.
     * <p>
     * Missing {@code coords} or invalid WKT both yield a 400; anything else falls through to the worker.
     * <p>
     * Also reachable as {@code GET /collections/{collectionId}/instances/{instanceId}/radius} (see
     * {@link #position} for the instance-scoped 404 behavior).
     */
    @RequestMapping(method = GET, value = {"/collections/{collectionId}/radius", "/collections/{collectionId}/instances/{instanceId}/radius"})
    public ResponseEntity radius(@PathVariable("serviceId") String serviceId,
                                  @PathVariable("collectionId") String collectionId,
                                  @PathVariable(name = "instanceId", required = false) String instanceId,
                                  @RequestParam(name = "coords", required = false) String coords,
                                  @RequestParam(name = "within", required = false) Double within,
                                  @RequestParam(name = "within-units", required = false) String withinUnits,
                                  @RequestParam(name = "z", required = false) String z,
                                  @RequestParam(name = "datetime", required = false) String datetime,
                                  @RequestParam(name = "parameter-name", required = false) List<String> parameterName,
                                  @RequestParam(name = "crs", required = false) String crs,
                                  @RequestParam(name = "limit", required = false) Integer limit) {
        putServiceIdParam(serviceId);
        if (instanceId != null && !instanceId.equals(collectionId)) {
            return unknownInstance(collectionId, instanceId);
        }
        if (coords == null) {
            return missingParam("coords", "POINT(2.35 48.85)");
        }
        if (within == null) {
            return missingParam("within", "10&within-units=km");
        }
        try {
            final Geometry g = parseCoords(coords, Point.class);
            return treatIncomingRequest(new GetRadius().collectionId(collectionId).coords(g)
                    .within(within).withinUnits(withinUnits).z(z).datetime(datetime).parameterName(parameterName).crs(crs).limit(limit)).getResponseEntity();
        } catch (ParseException ex) {
            return badCoords(ex);
        }
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/area} — area query.
     * Required: {@code coords} (WKT {@code POLYGON}). Optional: {@code z}, {@code datetime},
     * {@code parameter-name}, {@code crs}, {@code resolution-x}, {@code resolution-y}, {@code limit}.
     * <p>
     * Missing {@code coords} or invalid WKT both yield a 400; anything else falls through to the worker.
     * <p>
     * Also reachable as {@code GET /collections/{collectionId}/instances/{instanceId}/area} (see
     * {@link #position} for the instance-scoped 404 behavior).
     */
    @RequestMapping(method = GET, value = {"/collections/{collectionId}/area", "/collections/{collectionId}/instances/{instanceId}/area"})
    public ResponseEntity area(@PathVariable("serviceId") String serviceId,
                                @PathVariable("collectionId") String collectionId,
                                @PathVariable(name = "instanceId", required = false) String instanceId,
                                @RequestParam(name = "coords", required = false) String coords,
                                @RequestParam(name = "z", required = false) String z,
                                @RequestParam(name = "datetime", required = false) String datetime,
                                @RequestParam(name = "parameter-name", required = false) List<String> parameterName,
                                @RequestParam(name = "crs", required = false) String crs,
                                @RequestParam(name = "resolution-x", required = false) Double resolutionX,
                                @RequestParam(name = "resolution-y", required = false) Double resolutionY,
                                @RequestParam(name = "limit", required = false) Integer limit) {
        putServiceIdParam(serviceId);
        if (instanceId != null && !instanceId.equals(collectionId)) {
            return unknownInstance(collectionId, instanceId);
        }
        if (coords == null) {
            return missingParam("coords", "POLYGON((2.2 48.8,2.5 48.8,2.5 49.0,2.2 49.0,2.2 48.8))");
        }
        try {
            final Geometry g = parseCoords(coords, Polygon.class, MultiPolygon.class);
            return treatIncomingRequest(new GetArea().collectionId(collectionId).coords(g)
                    .z(z).datetime(datetime).parameterName(parameterName).crs(crs)
                    .resolutionX(resolutionX).resolutionY(resolutionY).limit(limit)).getResponseEntity();
        } catch (ParseException ex) {
            return badCoords(ex);
        }
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/cube} — cube query.
     * Required: {@code bbox} (4 comma-separated values: minx,miny,maxx,maxy). Optional: {@code z},
     * {@code datetime}, {@code parameter-name}, {@code crs}, {@code resolution-x/y/z}.
     * <p>
     * A missing {@code bbox}, or one that doesn't have exactly 4 values, yields a 400; anything else
     * falls through to the worker (unlike the other five queries, {@code cube} takes no WKT, so there
     * is no {@link #badCoords} path here).
     * <p>
     * Also reachable as {@code GET /collections/{collectionId}/instances/{instanceId}/cube} (see
     * {@link #position} for the instance-scoped 404 behavior).
     */
    @RequestMapping(method = GET, value = {"/collections/{collectionId}/cube", "/collections/{collectionId}/instances/{instanceId}/cube"})
    public ResponseEntity cube(@PathVariable("serviceId") String serviceId,
                                @PathVariable("collectionId") String collectionId,
                                @PathVariable(name = "instanceId", required = false) String instanceId,
                                @RequestParam(name = "bbox", required = false) List<Double> bbox,
                                @RequestParam(name = "z", required = false) String z,
                                @RequestParam(name = "datetime", required = false) String datetime,
                                @RequestParam(name = "parameter-name", required = false) List<String> parameterName,
                                @RequestParam(name = "crs", required = false) String crs,
                                @RequestParam(name = "resolution-x", required = false) Double resolutionX,
                                @RequestParam(name = "resolution-y", required = false) Double resolutionY,
                                @RequestParam(name = "resolution-z", required = false) Double resolutionZ) {
        putServiceIdParam(serviceId);
        if (instanceId != null && !instanceId.equals(collectionId)) {
            return unknownInstance(collectionId, instanceId);
        }
        if (bbox == null) {
            return missingParam("bbox", "2.2,48.8,2.5,49.0");
        }
        if (bbox.size() != 4) {
            return new ResponseObject(new ErrorMessage(HttpStatus.BAD_REQUEST, "The 'bbox' parameter must have 4 values"), MediaType.APPLICATION_JSON, HttpStatus.BAD_REQUEST).getResponseEntity();
        }
        final GeneralEnvelope env = new GeneralEnvelope(2);
        env.setRange(0, bbox.get(0), bbox.get(2));
        env.setRange(1, bbox.get(1), bbox.get(3));
        return treatIncomingRequest(new GetCube().collectionId(collectionId).bbox(env)
                .z(z).datetime(datetime).parameterName(parameterName).crs(crs)
                .resolutionX(resolutionX).resolutionY(resolutionY).resolutionZ(resolutionZ)).getResponseEntity();
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/trajectory} — trajectory query.
     * Required: {@code coords} (WKT {@code LINESTRING}). Optional: {@code z}, {@code datetime},
     * {@code parameter-name}, {@code crs}.
     * <p>
     * Missing {@code coords} or invalid WKT both yield a 400; anything else falls through to the worker.
     * <p>
     * Also reachable as {@code GET /collections/{collectionId}/instances/{instanceId}/trajectory} (see
     * {@link #position} for the instance-scoped 404 behavior).
     */
    @RequestMapping(method = GET, value = {"/collections/{collectionId}/trajectory", "/collections/{collectionId}/instances/{instanceId}/trajectory"})
    public ResponseEntity trajectory(@PathVariable("serviceId") String serviceId,
                                      @PathVariable("collectionId") String collectionId,
                                      @PathVariable(name = "instanceId", required = false) String instanceId,
                                      @RequestParam(name = "coords", required = false) String coords,
                                      @RequestParam(name = "z", required = false) String z,
                                      @RequestParam(name = "datetime", required = false) String datetime,
                                      @RequestParam(name = "parameter-name", required = false) List<String> parameterName,
                                      @RequestParam(name = "crs", required = false) String crs) {
        putServiceIdParam(serviceId);
        if (instanceId != null && !instanceId.equals(collectionId)) {
            return unknownInstance(collectionId, instanceId);
        }
        if (coords == null) {
            return missingParam("coords", "LINESTRING(2.2 48.8,2.5 49.0)");
        }
        try {
            final Geometry g = parseCoords(coords, LineString.class, MultiLineString.class);
            return treatIncomingRequest(new GetTrajectory().collectionId(collectionId).coords(g)
                    .z(z).datetime(datetime).parameterName(parameterName).crs(crs)).getResponseEntity();
        } catch (ParseException ex) {
            return badCoords(ex);
        }
    }

    /**
     * {@code GET /WS/edr/{serviceId}/collections/{collectionId}/corridor} — corridor query.
     * Required: {@code coords} (WKT {@code LINESTRING}) and {@code corridor-width}. Optional:
     * {@code width-units}, {@code corridor-height}, {@code height-units}, {@code z}, {@code datetime},
     * {@code parameter-name}, {@code crs}, {@code resolution-x/y}.
     * <p>
     * Missing {@code coords}, missing {@code corridor-width}, or invalid WKT each yield a 400; anything
     * else falls through to the worker.
     * <p>
     * Also reachable as {@code GET /collections/{collectionId}/instances/{instanceId}/corridor} (see
     * {@link #position} for the instance-scoped 404 behavior).
     */
    @RequestMapping(method = GET, value = {"/collections/{collectionId}/corridor", "/collections/{collectionId}/instances/{instanceId}/corridor"})
    public ResponseEntity corridor(@PathVariable("serviceId") String serviceId,
                                    @PathVariable("collectionId") String collectionId,
                                    @PathVariable(name = "instanceId", required = false) String instanceId,
                                    @RequestParam(name = "coords", required = false) String coords,
                                    @RequestParam(name = "corridor-width", required = false) Double corridorWidth,
                                    @RequestParam(name = "width-units", required = false) String widthUnits,
                                    @RequestParam(name = "corridor-height", required = false) Double corridorHeight,
                                    @RequestParam(name = "height-units", required = false) String heightUnits,
                                    @RequestParam(name = "z", required = false) String z,
                                    @RequestParam(name = "datetime", required = false) String datetime,
                                    @RequestParam(name = "parameter-name", required = false) List<String> parameterName,
                                    @RequestParam(name = "crs", required = false) String crs,
                                    @RequestParam(name = "resolution-x", required = false) Double resolutionX,
                                    @RequestParam(name = "resolution-y", required = false) Double resolutionY) {
        putServiceIdParam(serviceId);
        if (instanceId != null && !instanceId.equals(collectionId)) {
            return unknownInstance(collectionId, instanceId);
        }
        if (coords == null) {
            return missingParam("coords", "LINESTRING(2.2 48.8,2.5 49.0)");
        }
        if (corridorWidth == null) {
            return missingParam("corridor-width", "10");
        }
        try {
            final Geometry g = parseCoords(coords, LineString.class, MultiLineString.class);
            return treatIncomingRequest(new GetCorridor().collectionId(collectionId).coords(g)
                    .corridorWidth(corridorWidth).widthUnits(widthUnits).corridorHeight(corridorHeight).heightUnits(heightUnits)
                    .z(z).datetime(datetime).parameterName(parameterName).crs(crs)
                    .resolutionX(resolutionX).resolutionY(resolutionY)).getResponseEntity();
        } catch (ParseException ex) {
            return badCoords(ex);
        }
    }

    /**
     * Parses a {@code coords} (or {@code trajectory}/{@code corridor} centerline) query parameter as WKT.
     *
     * @param wkt     the raw WKT string.
     * @param allowed the geometry types accepted by the query (e.g. {@code POINT} for position).
     * @return the parsed geometry.
     * @throws ParseException if {@code wkt} is not valid WKT, or not one of the {@code allowed} types — the
     *                        caller turns this into a 400 via {@link #badCoords}.
     */
    private static Geometry parseCoords(String wkt, Class<?>... allowed) throws ParseException {
        final Geometry g = new WKTReader().read(wkt);
        for (Class<?> type : allowed) {
            if (type.isInstance(g)) return g;
        }
        throw new ParseException("Unsupported geometry type " + g.getGeometryType() + " for this query, expected "
                + Arrays.stream(allowed).map(Class::getSimpleName).map(String::toUpperCase).collect(Collectors.joining(" or ")));
    }

    /**
     * Builds a 400 response for a {@code coords}/geometry parameter that failed WKT parsing.
     */
    private static ResponseEntity badCoords(ParseException ex) {
        return new ResponseObject(new ErrorMessage(HttpStatus.BAD_REQUEST, ex.getMessage()), MediaType.APPLICATION_JSON, HttpStatus.BAD_REQUEST).getResponseEntity();
    }

    /**
     * Builds a 400 response for a missing required query parameter, with a usage example in the message.
     *
     * @param name    the parameter name.
     * @param example an example value, appended to the message as {@code ?name=example}.
     */
    private static ResponseEntity missingParam(String name, String example) {
        final String message = "Missing required parameter '" + name + "'. Example: ?" + name + "=" + example;
        LOGGER.log(Level.WARNING, message);
        return new ResponseObject(new ErrorMessage(HttpStatus.BAD_REQUEST, message), MediaType.APPLICATION_JSON, HttpStatus.BAD_REQUEST).getResponseEntity();
    }

    /**
     * Builds a 404 response for an instance-scoped query whose {@code instanceId} path segment does
     * not match the collection's single synthetic instance (whose id is the collection's own id).
     */
    private static ResponseEntity unknownInstance(String collectionId, String instanceId) {
        final String message = "Unknown instance '" + instanceId + "' for collection '" + collectionId + "'.";
        LOGGER.log(Level.WARNING, message);
        return new ResponseObject(new ErrorMessage(HttpStatus.NOT_FOUND, message), MediaType.APPLICATION_JSON, HttpStatus.NOT_FOUND).getResponseEntity();
    }
}
