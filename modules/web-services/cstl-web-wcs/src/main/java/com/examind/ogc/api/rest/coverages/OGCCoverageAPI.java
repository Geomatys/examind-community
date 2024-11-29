/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.ogc.api.rest.coverages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.examind.ogc.api.rest.common.ConformanceProvider;
import com.examind.ogc.api.rest.common.dto.Collection;
import com.examind.ogc.api.rest.common.dto.Conformance;
import com.examind.ogc.api.rest.common.dto.LandingPage;
import com.examind.ogc.api.rest.coverages.dto.DataRecord;
import com.examind.ogc.api.rest.coverages.dto.DomainSet;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ServiceDef;
import org.constellation.api.rest.ErrorMessage;
import org.constellation.api.rest.I18nCodes;
import org.constellation.coverage.core.WCSWorker;
import org.constellation.exception.ConstellationException;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.GridWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.atom.xml.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import static org.constellation.coverage.core.AtomLinkBuilder.buildDocumentLinks;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * <em>WARNING:</em> Beware of transaction management. For now, rather use {@link SpringHelper#executeInTransaction(TransactionCallback) manual transactions}
 * than automated annotation-based transactions, because:
 * <ul>
 *     <li>Transaction annotation might not be processed, because this component is setup by a servlet context, not
 *     by the main application context.</li>
 *     <li>Default transaction annotation configuration only triggers rollback upon runtime exception or fatal error.</li>
 * </ul>
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@RestController
//@RequestMapping("/ogc/collections/{collectionId}/coverage")
@RequestMapping("coverage/{serviceId:.+}")
public class OGCCoverageAPI extends GridWebService<WCSWorker> implements ConformanceProvider {

    private static final List<Link> CONFORMS = Collections.unmodifiableList(Arrays.asList( //TODO : Conformity with Part 1 html, Part 2 simple-query & html
            new Link("https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/core", null, null, null),
            new Link("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/landing-page", null, null, null),
            new Link("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/json", null, null, null),
            //new Link("https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html", null, null, null), Doesn't conform
            new Link("http://www.opengis.net/spec/ogcapi-common-1/1.0/conf/oas30", null, null, null),

            new Link("http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/collections", null, null, null),
            //new Link("http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/simple-query", null, null, null), Doesn't conform
            new Link("http://www.opengis.net/spec/ogcapi-common-2/1.0/conf/json", null, null, null),
            //new Link("https://www.opengis.net/spec/ogcapi-common-1/1.0/conf/html", null, null, null), Doesn't conform

            new Link("http://www.opengis.net/spec/ogcapi-coverages-1/1.0/conf/core", null, null, null)
    ));

    public static final String SPECIFICATION_URL = "https://docs.ogc.org/DRAFTS/19-087.html";

    public OGCCoverageAPI() {
        // here we use wcs for worker retrieval purpose
        super(ServiceDef.Specification.WCS);
    }

    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WCSWorker worker) {
        try {
            String format = getParameter("f", false);
            if (format == null) {
                format = "application/json";
            }
            final boolean asJson = format.contains(MimeType.APP_JSON);
            MediaType media;
            if (asJson) {
                media = MediaType.APPLICATION_JSON;
            } else {
                media = MediaType.APPLICATION_XML;
            }

            LandingPage landingPage = buildLandingPage(format, worker.getId());
            return new ResponseObject(landingPage, media, HttpStatus.OK);
        } catch (CstlServiceException ex) {
            return processExceptionResponse(ex, ServiceDef.WCS_2_0_0, worker);
        }
    }

    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getLocalizedMessage(), exc);
        return new ResponseObject(new ErrorMessage(exc));
    }

    @RequestMapping(value = "/", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getLandingPage(@PathVariable("serviceId") String serviceId, @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        try {
            final boolean asJson = format.contains(MimeType.APP_JSON);
            MediaType media;
            if (asJson) {
                media = MediaType.APPLICATION_JSON;
            } else {
                media = MediaType.APPLICATION_XML;
            }
            LandingPage landingPage = buildLandingPage(format, serviceId);
            return new ResponseObject(landingPage, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private LandingPage buildLandingPage(String format, String serviceId) {
        final boolean asJson = format.contains(MimeType.APP_JSON);
        String url    = getServiceURL() + "/coverage/" + serviceId;
        String wcsUrl = getServiceURL() + "/wcs/" + serviceId + "?";

        List<Link> links = new ArrayList<>();
        links.add(new Link(url + "/api", "service-desc", null, "the API definition"));
        buildDocumentLinks(url, asJson, links, false);
        links.add(new Link(url + "/conformance", "conformance", MimeType.APP_JSON, "OGC API conformance classes implemented by this server as JSON"));
        links.add(new Link(url + "/conformance?f=application/xml", "conformance", MimeType.APP_XML, "OGC API conformance classes implemented by this server as XML"));
        links.add(new Link(url + "/collections", "data", MimeType.APP_JSON, "Information about the feature collections as JSON"));
        links.add(new Link(url + "/collections?f=application/xml", "data", MimeType.APP_XML, "Information about the feature collections as XML"));
        return new LandingPage(links);
    }

    @RequestMapping(value = "/api", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RedirectView api() {
        return new RedirectView(SPECIFICATION_URL);
    }

    @RequestMapping(value = "/conformance", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getConformance(@RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        try {
            final boolean asJson = format.contains(MimeType.APP_JSON);
            MediaType media;
            if (asJson) {
                media = MediaType.APPLICATION_JSON;
            } else {
                media = MediaType.APPLICATION_XML;
            }
            Conformance conformance = new Conformance(CONFORMS);
            return new ResponseObject(conformance, media, HttpStatus.OK).getResponseEntity();
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/collections", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getCollections(@PathVariable("serviceId") String serviceId,
                                         @RequestParam(value = "bbox", required = false, defaultValue = "") String bbox,
                                         @RequestParam(value = "time", required = false, defaultValue = "") String time,
                                         @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                List<com.examind.ogc.api.rest.common.dto.Collection> layers = worker.getCollections(new ArrayList<>(), false);
                com.examind.ogc.api.rest.common.dto.Collections response = new com.examind.ogc.api.rest.common.dto.Collections();

                response.setCollections(layers);

                final boolean asJson = format.contains(MimeType.APP_JSON);
                MediaType media;
                String url = getServiceURL() + "/coverage/" + serviceId + "/collections";

                List<Link> links = new ArrayList<>();
                buildDocumentLinks(url, asJson, links, false);

                response.setLinks(links);

                if (asJson) {
                    media = MediaType.APPLICATION_JSON;
                } else {
                    media = MediaType.APPLICATION_XML;
                    //collections.setXMLBBoxMode();
                }

                return new ResponseObject(response, media, HttpStatus.OK).getResponseEntity();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(value = "/collections/{collectionId}", method = GET, produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity getCollection(@PathVariable("serviceId") String serviceId,
                                        @PathVariable(value = "collectionId") String collectionId,
                                        @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) {
        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                // if the layer does not exist an exception will be thrown
                final List<Collection> layers = worker.getCollections(Collections.singletonList(collectionId), false);
                final boolean asJson = format.contains(MimeType.APP_JSON);
                Object result;
                MediaType media;
                if (asJson) {
                    media = MediaType.APPLICATION_JSON;
                    result = layers.get(0);
                } else {
                    media = MediaType.APPLICATION_XML;
                    com.examind.ogc.api.rest.common.dto.Collections collections = new com.examind.ogc.api.rest.common.dto.Collections();
                    collections.setLinks(new ArrayList<>());
                    collections.setCollections(layers);
                    //collections.setXMLBBoxMode();
                    result = collections;
                }
                return new ResponseObject(result, media, HttpStatus.OK).getResponseEntity();

            } catch (CstlServiceException ex) {
                if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                    return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                }
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    /**
     * GET /collections/{collectionId}/coverage
     * <p>
     * reference : https://developer.ogc.org/api/coverages/index.html#tag/Coverage/operation/getCoverage
     *
     * @return coverage object (tiff format)
     */
    @RequestMapping(value = "/collections/{collectionId}/coverage", method = RequestMethod.GET)
    public ResponseEntity coverage(@PathVariable("serviceId") String serviceId,
                                   @PathVariable(value = "collectionId") String collectionId,
                                   @RequestParam(name = "f", required = false, defaultValue = MimeType.IMAGE_TIFF) String format,
                                   @RequestParam(name = "properties", required = false) String properties,
                                   @RequestParam(name = "bbox", required = false) List<Double> bbox,
                                   @RequestParam(name = "scaleFactor", required = false) Double scaleFactor,
                                   @RequestParam(name = "scaleAxes", required = false) String scaleAxesQuery,
                                   @RequestParam(name = "scaleSize", required = false) String scaleSizeQuery,
                                   @RequestParam(name = "subset", required = false) String subset) throws ConstellationException {

        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                int numParamsSet = 0;
                String scaleData = null;
                if (scaleFactor != null) {
                    List<String> dimensionsNames = worker.getDimensionsNames(collectionId);
                    scaleData = "";
                    for(var dimension : dimensionsNames) {
                        scaleData += dimension + "(" + scaleFactor + ")";
                    }
                    numParamsSet++;
                }
                if (scaleAxesQuery != null) {
                    numParamsSet++;
                    scaleData=scaleAxesQuery;
                }
                if (scaleSizeQuery != null) {
                    //For the moment, not supported
                    //TODO : Support ScaleSize argument in request
                    numParamsSet++;
                }
                if (numParamsSet > 1) {
                    throw new IllegalArgumentException("Only one scaling parameter can be set at a time");
                }

                List<String> subsetData = new ArrayList<>();
                if (subset != null && !subset.isEmpty()) {
                    subsetData = Arrays.stream(subset.split(",")).toList();
                }

                List<String> propertiesData = new ArrayList<>();
                if (properties != null && !properties.isEmpty()) {
                    propertiesData = Arrays.stream(properties.split(",")).toList();
                }

                // if the layer does not exist an exception will be thrown
                Object response = worker.getCoverage(collectionId, format, bbox, scaleData, subsetData, propertiesData);

                return new ResponseObject(response, format).getResponseEntity();

            } catch (CstlServiceException ex) {
                if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                    return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                }
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    /**
     * GET /collections/{collectionId}/coverage/domainset
     * <p>
     * reference : https://developer.ogc.org/api/coverages/index.html#tag/Coverage/operation/getCoverageDomainSet
     *
     * @return coverage domain set information
     */
    @RequestMapping(value = "/collections/{collectionId}/coverage/domainset", method = RequestMethod.GET)
    public ResponseEntity coverageDomainSet(@PathVariable("serviceId") String serviceId,
                                   @PathVariable(value = "collectionId") String collectionId,
                                   @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format,
                                   @RequestParam(name = "bbox", required = false) List<Double> bbox) throws ConstellationException {

        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                // if the layer does not exist an exception will be thrown
                DomainSet response = worker.getDomainSet(collectionId, bbox);

                return new ResponseObject(response, format).getResponseEntity();

            } catch (CstlServiceException ex) {
                if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                    return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                }
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    /**
     * GET /collections/{collectionId}/coverage/rangetype
     * <p>
     * reference : https://developer.ogc.org/api/coverages/index.html#tag/Coverage/operation/getCoverageRangeType
     *
     * @return coverage domain set information
     */
    @RequestMapping(value = "/collections/{collectionId}/coverage/rangetype", method = RequestMethod.GET)
    public ResponseEntity coverageRangeType(@PathVariable("serviceId") String serviceId,
                                            @PathVariable(value = "collectionId") String collectionId,
                                            @RequestParam(name = "f", required = false, defaultValue = MimeType.APP_JSON) String format) throws ConstellationException {

        putServiceIdParam(serviceId);
        final WCSWorker worker = getWorker(serviceId);

        if (worker != null) {
            try {
                // if the layer does not exist an exception will be thrown
                DataRecord response = worker.getDataRecord(collectionId);

                return new ResponseObject(response, format).getResponseEntity();

            } catch (CstlServiceException ex) {
                if (ex.getExceptionCode().equals(LAYER_NOT_DEFINED)) {
                    return new ErrorMessage(HttpStatus.NOT_FOUND).i18N(I18nCodes.Collection.NOT_FOUND).build();
                }
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                return new ErrorMessage(ex).build();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @Override
    public List<Link> getConformances() {
        return CONFORMS;
    }
}
