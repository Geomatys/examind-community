/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dggs.ws.rs;

import com.examind.dggs.core.DGGSWorker;
import org.geotoolkit.ogcapi.request.common.GetCollection;
import org.geotoolkit.ogcapi.request.common.GetCollectionList;
import org.geotoolkit.ogcapi.request.common.GetCollectionQueryables;
import org.geotoolkit.ogcapi.request.common.GetCollectionSchema;
import org.geotoolkit.ogcapi.request.common.GetConformance;
import org.geotoolkit.ogcapi.request.dggs.GetDggrs;
import org.geotoolkit.ogcapi.request.dggs.GetDggrsDefinition;
import org.geotoolkit.ogcapi.request.dggs.GetDggrsList;
import org.geotoolkit.ogcapi.request.dggs.GetZoneData;
import org.geotoolkit.ogcapi.request.dggs.GetZone;
import org.geotoolkit.ogcapi.request.dggs.GetZoneList;
import org.geotoolkit.ogcapi.request.feature.GetFunctions;
import org.geotoolkit.ogcapi.request.common.GetLandingPage;
import org.geotoolkit.ogcapi.request.common.GetCollectionMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.api.ServiceDef;
import org.constellation.dto.ExceptionReport;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.ExceptionCode;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogcapi.dto.Conformance;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.Collections;
import org.geotoolkit.ogcapi.dto.common.ConfClasses;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.dggs.Dggrs;
import org.geotoolkit.ogcapi.dto.dggs.DggrsData;
import org.geotoolkit.ogcapi.dto.dggs.DggrsDefinition;
import org.geotoolkit.ogcapi.dto.dggs.DggrsListResponse;
import org.geotoolkit.ogcapi.dto.dggs.DggrsZonesResponse;
import org.geotoolkit.ogcapi.dto.dggs.MediaTypes;
import org.geotoolkit.ogcapi.dto.dggs.ZoneInfo;
import org.geotoolkit.ogcapi.dto.feature.Functions;
import org.geotoolkit.ogcapi.dto.jsonschema.JSONSchema;
import org.geotoolkit.ogcapi.request.common.GetApi;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
@RequestMapping("dggs/{serviceId:.+}")
public final class DGGSService extends OGCWebService<DGGSWorker> {

    private static final String OPENAPI_MEDIATYPE = "application/vnd.oai.openapi+json;version=3.0";

    //use it to store the template in a temporary directory editable in live with the service
    private static boolean DEBUG = false;

    private static final Map<String,URL> TEMPLATES = new LinkedHashMap();
    private static final String TEMPLATE_LANDINGPAGE = "landingpage.html";
    private static final String TEMPLATE_CONFORMANCE = "conformance.html";
    private static final String TEMPLATE_COLLECTIONLIST = "collectionlist.html";
    private static final String TEMPLATE_COLLECTION = "collection.html";
    private static final String TEMPLATE_FUNCTIONS = "functions.html";
    private static final String TEMPLATE_SCHEMA = "schema.html";
    private static final String TEMPLATE_QUERYABLES = "queryables.html";
    private static final String TEMPLATE_DGGRSLIST = "dggrslist.html";
    private static final String TEMPLATE_DGGRS = "dggrs.html";
    private static final String TEMPLATE_DGGRSZONELIST = "dggrszonelist.html";
    private static final String TEMPLATE_DGGRSZONEINFO = "dggrszoneinfo.html";
    private static final String TEMPLATE_DGGRSCOLLECTIONZONEINFO = "dggrscollectionzoneinfo.html";
    private static final String TEMPLATE_DGGRSCOLLECTIONZONELIST = "dggrscollectionzonelist.html";
    private static final String TEMPLATE_SWAGGER = "swagger.html";

    private static final ConfClasses CONFCLASSES = new ConfClasses();

    static {
        TEMPLATES.put(TEMPLATE_LANDINGPAGE, DGGSService.class.getResource("/com/examind/dggs/landingpage.html"));
        TEMPLATES.put(TEMPLATE_COLLECTIONLIST, DGGSService.class.getResource("/com/examind/dggs/collectionlist.html"));
        TEMPLATES.put(TEMPLATE_COLLECTION, DGGSService.class.getResource("/com/examind/dggs/collection.html"));
        TEMPLATES.put(TEMPLATE_FUNCTIONS, DGGSService.class.getResource("/com/examind/dggs/functions.html"));
        TEMPLATES.put(TEMPLATE_SCHEMA, DGGSService.class.getResource("/com/examind/dggs/schema.html"));
        TEMPLATES.put(TEMPLATE_QUERYABLES, DGGSService.class.getResource("/com/examind/dggs/queryables.html"));
        TEMPLATES.put(TEMPLATE_DGGRSLIST, DGGSService.class.getResource("/com/examind/dggs/dggrslist.html"));
        TEMPLATES.put(TEMPLATE_DGGRS, DGGSService.class.getResource("/com/examind/dggs/dggrs.html"));
        TEMPLATES.put(TEMPLATE_CONFORMANCE, DGGSService.class.getResource("/com/examind/dggs/conformance.html"));
        TEMPLATES.put(TEMPLATE_DGGRSZONELIST, DGGSService.class.getResource("/com/examind/dggs/dggrszonelist.html"));
        TEMPLATES.put(TEMPLATE_DGGRSZONEINFO, DGGSService.class.getResource("/com/examind/dggs/dggrszoneinfo.html"));
        TEMPLATES.put(TEMPLATE_DGGRSCOLLECTIONZONEINFO, DGGSService.class.getResource("/com/examind/dggs/dggrscollectionzoneinfo.html"));
        TEMPLATES.put(TEMPLATE_DGGRSCOLLECTIONZONELIST, DGGSService.class.getResource("/com/examind/dggs/dggrscollectionzonelist.html"));
        TEMPLATES.put(TEMPLATE_SWAGGER, DGGSService.class.getResource("/com/examind/dggs/swagger.html"));

        if (DEBUG) {
            try {
                final Path tempDir = Files.createTempDirectory("dggs");
                LOGGER.warning(">>>>>>> DGGS TEMPLATE DIR >>>>>>>>>>>> " + tempDir.toUri().toString());

                for (Entry<String,URL> entry : TEMPLATES.entrySet()) {
                    Path p = tempDir.resolve(entry.getValue().toString().substring(entry.getValue().toString().lastIndexOf("/")+1));
                    String template;
                    try (InputStream in = TEMPLATES.get(entry.getKey()).openStream()) {
                        template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    Files.writeString(p, template);
                    entry.setValue(p.toUri().toURL());
                }

            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);;
            }
        }

        //conformance configuration
        CONFCLASSES.addConformsToItem(Conformance.CORE);
        CONFCLASSES.addConformsToItem(Conformance.CORE_LANDINGPAGE);
        CONFCLASSES.addConformsToItem(Conformance.CORE_HTML);
        CONFCLASSES.addConformsToItem(Conformance.CORE_JSON);
        CONFCLASSES.addConformsToItem(Conformance.CORE_OAS);
        CONFCLASSES.addConformsToItem(Conformance.COLLECTIONS_v1);

        CONFCLASSES.addConformsToItem(Conformance.FEATURE_QUERYABLES);
        CONFCLASSES.addConformsToItem(Conformance.FEATURE_SCHEMAS);

        CONFCLASSES.addConformsToItem(Conformance.DGGS_CORE);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_RETRIEVAL);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_SUBSETTING);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_CUSTOM_DEPTHS);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_FILTERING_ZONE_DATA_WITH_CQL2);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ZONE_QUERY);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_FILTERING_ZONE_QUERY_WITH_CQL2);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ROOT_DGGS);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_COLLECTION);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_JSON);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_UBJSON );
        //dto.addConformsToItem(Conformance.DGGS_DATA_JSONFG);
        //dto.addConformsToItem(Conformance.DGGS_DATA_UBJSONFG);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_GEOTIFF);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_GEOJSON);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_CBOR);
        //dto.addConformsToItem(Conformance.DGGS_DATA_NETCDF);
        //dto.addConformsToItem(Conformance.DGGS_DATA_ZARR);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_COVERAGEJSON);
        //dto.addConformsToItem(Conformance.DGGS_DATA_JPEGXL);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_DATA_PNG);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ZONELIST_HTML);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ZONELIST_UINT64);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ZONELIST_GEOJSON);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ZONELIST_GEOTIFF);
        CONFCLASSES.addConformsToItem(Conformance.DGGS_ZONELIST_OPERATIONIDS);
    }

    private static final ObjectMapper MAPPER_JSON =JsonMapper.builder()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .addModule(new JavaTimeModule())
        .build();

    private static final ObjectMapper MAPPER_YAML = YAMLMapper.builder()
                    .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                    .build();

    private static final XmlMapper MAPPER_XML = XmlMapper.builder()
                    .defaultUseWrapper(false)
                    .build();

    public DGGSService() {
        super(ServiceDef.Specification.DGGS);
    }

    private static ResponseObject fromTemplate(Object candidate, String mediaType, String templateKey, String serviceUrl) {

        try {
            if (MediaType.APPLICATION_JSON_VALUE.equals(mediaType)) {
                return new ResponseObject(MAPPER_JSON.writeValueAsString(candidate), MediaType.APPLICATION_JSON);

            } else if (MediaType.TEXT_HTML_VALUE.equals(mediaType)) {

                try {
                    final String str = MAPPER_JSON.writeValueAsString(candidate);
                    String template;
                    try (InputStream in = TEMPLATES.get(templateKey).openStream()) {
                        template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    final String baseUrl = serviceUrl.substring(0, serviceUrl.indexOf("/WS")) +"/";
                    template = template.replace("_baseurl_", baseUrl);
                    return new ResponseObject(template.replace("_json_", '\"' + str.replace("\"", "\\x22").replace("\\n", "") + '\"'), MediaType.TEXT_HTML);
                } catch (IOException ex) {
                    return new ResponseObject(500, ex.getMessage());
                }
            } else if (MediaType.APPLICATION_YAML_VALUE.equals(mediaType)) {
                // Spring media type for yaml is not handle by browsers
                return new ResponseObject(MAPPER_YAML.writeValueAsString(candidate), "text/x-yaml");

            } else if (MediaType.APPLICATION_XML_VALUE.equals(mediaType)) {
                // we must use getBytes otherwise spring interceptor reencode the value
                return new ResponseObject(MAPPER_XML.writeValueAsString(candidate).getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML);
            } else {
                return new ResponseObject(406);
            }
        } catch (JsonProcessingException ex) {
            return new ResponseObject(500, ex.getMessage());
        }
    }

    @Override
    protected ResponseObject treatIncomingRequest(Object request, DGGSWorker worker) {

        if (request == null) {
            request = new GetLandingPage().format("application/json");
        }

        try {
            //core api
            if (request instanceof GetLandingPage r) {
                final String format = r.getFormat();
                final LandingPage dto = worker.getLandingPage();
                return fromTemplate(dto, format, TEMPLATE_LANDINGPAGE, getServiceURL());

            } else if (request instanceof GetConformance r) {
                final String format = r.getFormat();

                return fromTemplate(CONFCLASSES, format, TEMPLATE_CONFORMANCE, getServiceURL());
            } else if (request instanceof GetApi r) {
                final String format = r.getFormat();

                if (OPENAPI_MEDIATYPE.equals(format) ||
                    MediaType.APPLICATION_JSON_VALUE.equals(format)) {
                    final String api = worker.getAPI();
                    return new ResponseObject(api, MediaType.APPLICATION_JSON);
                } else if (MediaType.TEXT_HTML_VALUE.equals(format)) {
                    String html;
                    try (InputStream in = TEMPLATES.get(TEMPLATE_SWAGGER).openStream()) {
                        html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        throw new CstlServiceException(ex.getMessage(), ex);
                    }
                    final String serviceUrl = getServiceURL();
                    final String baseUrl = serviceUrl.substring(0, serviceUrl.indexOf("/WS")) +"/";
                    html = html.replace("_baseurl_", baseUrl);

                    html = html.replace("$${url}", worker.getServiceUrl().replace("?", "/api?f=json"));
                    return new ResponseObject(html, MediaType.TEXT_HTML);
                }

                return new ResponseObject("Format not supported.", MediaType.TEXT_PLAIN, HttpStatus.BAD_REQUEST);
            }
            //collection api
            else if (request instanceof GetCollectionList r) {
                final String format = r.getFormat();
                final Collections dto = worker.getCollectionList(r);
                return fromTemplate(dto, format, TEMPLATE_COLLECTIONLIST, getServiceURL());

            } else if (request instanceof GetCollection r) {
                final String format = r.getFormat();
                final CollectionDescription dto = worker.getCollection(r);
                return fromTemplate(dto, format, TEMPLATE_COLLECTION, getServiceURL());
            } else if (request instanceof GetCollectionMetadata r) {
                final String dto = worker.getCollectionMetadata(r);
                return new ResponseObject(dto.getBytes(StandardCharsets.UTF_8), MediaType.APPLICATION_XML);
            }
            //feature query api
            else if (request instanceof GetFunctions r) {
                final Functions dto = worker.getFunctions();
                final String format = r.getFormat();
                if (MediaType.TEXT_HTML_VALUE.equals(format)) {
                    return fromTemplate(dto, format, TEMPLATE_FUNCTIONS, getServiceURL());
                } else {
                    return new ResponseObject(dto, MediaType.APPLICATION_JSON);
                }
            } else if (request instanceof GetCollectionSchema r) {
                final JSONSchema dto = worker.getCollectionSchema(r);
                final String format = r.getFormat();
                if (MediaType.TEXT_HTML_VALUE.equals(format)) {
                    return fromTemplate(dto, format, TEMPLATE_FUNCTIONS, getServiceURL());
                } else {
                    return new ResponseObject(dto, "application/schema+json");
                }
            } else if (request instanceof GetCollectionQueryables r) {
                final JSONSchema dto = worker.getCollectionQueryables(r);
                final String format = r.getFormat();
                if (MediaType.TEXT_HTML_VALUE.equals(format)) {
                    return fromTemplate(dto, format, TEMPLATE_FUNCTIONS, getServiceURL());
                } else {
                    return new ResponseObject(dto, "application/schema+json");
                }
            }
            //dggrs api
            else if (request instanceof GetDggrsList r) {

                final String format = r.getFormat();
                final DggrsListResponse dto = worker.getDggrsList(r);
                return fromTemplate(dto, format, TEMPLATE_DGGRSLIST, getServiceURL());

            } else if (request instanceof GetDggrs r) {

                final String format = r.getFormat();
                final Dggrs dto = worker.getDggrs(r);
                return fromTemplate(dto, format, TEMPLATE_DGGRS, getServiceURL());

            } else if (request instanceof GetDggrsDefinition r) {
                final DggrsDefinition dto = worker.getDggrsDefinition(r);
                return new ResponseObject(dto, MediaType.APPLICATION_JSON);

            } else if (request instanceof GetZoneList r) {
                final String format = r.getFormat();
                ResponseObject response = worker.getDggrsZoneList(r);

                final Object body = response.getResponseEntity().getBody();
                if (body instanceof DggrsZonesResponse) {
                    return fromTemplate(body, format, r.getCollectionId() == null ? TEMPLATE_DGGRSZONELIST: TEMPLATE_DGGRSCOLLECTIONZONELIST, getServiceURL());
                } else {
                    return response;
                }

            } else if (request instanceof GetZone r) {
                final String format = r.getFormat();
                final ZoneInfo dto = worker.getDggrsZone(r);

                if (r.getCollectionId() == null) {
                    return fromTemplate(dto, format, TEMPLATE_DGGRSZONEINFO, getServiceURL());
                } else {
                    return fromTemplate(dto, format, TEMPLATE_DGGRSCOLLECTIONZONEINFO, getServiceURL());
                }

            } else if (request instanceof GetZoneData r) {
                if (r.getZoneId() != null) {
                    return worker.getDggrsZoneData(r);
                } else {
                    return worker.getDggrsData(r);
                }
            } else {
                return new ResponseObject(404);
            }
        } catch (IllegalArgumentException ex) {
            return processExceptionResponse(new CstlServiceException(ex, ExceptionCode.INVALID_PARAMETER_VALUE), ServiceDef.DGGS_1_0_0, worker);
        } catch (BadParameterException ex) {
            return ex.toResponse();
        } catch (CstlServiceException ex) {
            return processExceptionResponse(ex, ServiceDef.DGGS_1_0_0, worker);
        }

    }

    /**
     * Specification says to extract the format from the header if 'f' parameter is not defined.
     * @return format as MediaType value.
     */
    private String defaultFormat(String f, HttpHeaders headers) throws BadParameterException {
        if (f != null && !f.isBlank()) {
            f = f.toLowerCase();
            switch (f) {
                case "json" : return MediaTypes.DATA_JSON;
                case "yaml" : return MediaType.APPLICATION_YAML_VALUE;
                case "html" : return MediaType.TEXT_HTML_VALUE;
                case "xml" : return MediaType.APPLICATION_XML_VALUE;
                case "png" : return MediaTypes.DATA_PNG;
                case "geotiff" : return MediaTypes.DATA_GEOTIFF;
                case "zarr" : return MediaTypes.DATA_ZARR;
                case "geojson" : return MediaTypes.DATA_GEOJSON;
                case "ubjson" : return MediaTypes.DATA_UBJSON;
                case "cbor" : return MediaTypes.DATA_CBOR;
                default: throw new BadParameterException("format ", f);
            }
        }
        for (MediaType type : headers.getAccept()) {
            if (MediaType.ALL.equals(type)) {
                return MediaType.APPLICATION_JSON_VALUE;
            } else if (MediaType.APPLICATION_JSON.equals(type)) {
                return MediaType.APPLICATION_JSON_VALUE;
            } else if (MediaType.TEXT_HTML.equals(type)) {
                return MediaType.TEXT_HTML_VALUE;
            } else if (MediaType.APPLICATION_YAML.equals(type)) {
                return MediaType.APPLICATION_YAML_VALUE;
            } else if (MediaType.APPLICATION_XML.equals(type)) {
                return MediaType.APPLICATION_XML_VALUE;
            }

            final String t = type.toString();
            if ( MediaTypes.ZONELIST_GEOJSON.equals(t)
               || MediaTypes.ZONELIST_GEOTIFF.equals(t)
               || MediaTypes.ZONELIST_HTML.equals(t)
               || MediaTypes.ZONELIST_JSON.equals(t)
               || MediaTypes.ZONELIST_UINT64.equals(t)
               || MediaTypes.DATA_PNG.equals(t)
               || MediaTypes.DATA_GEOTIFF.equals(t)
               || MediaTypes.DATA_GEOJSON.equals(t)
               || MediaTypes.DATA_UBJSON.equals(t)
               || MediaTypes.DATA_JPEGXL.equals(t)
               || MediaTypes.DATA_COVERAGEJSON.equals(t)
               || MediaTypes.DATA_CBOR.equals(t)
               || OPENAPI_MEDIATYPE.equals(t)
                    ) {
                return t;
            }
        }

        if (!headers.getAccept().isEmpty()) {
            throw new BadParameterException("No matching accept format found");
        }

        //return json by default
        return MediaType.APPLICATION_JSON_VALUE;
    }

    private boolean askingGzip(HttpHeaders headers) {
        List<String> acc = headers.get("Accept-Encoding");
        return acc != null && acc.contains("gzip");
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Core API ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * GET / : Retrieve the OGC API landing page for this service.
     *
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return The landing page provides links to the API definition (link relation &#x60;service-desc&#x60;, in this
     * case path &#x60;/api&#x60;), to the Conformance declaration (path &#x60;/conformance&#x60;, link relation
     * &#x60;conformance&#x60;), and to the Collections of geospatial data (path &#x60;/collections&#x60;, link relation
     * &#x60;data&#x60;). (status code 200) or Content negotiation failed. For example, the &#x60;Accept&#x60; header
     * submitted in the request did not support any of the media types supported by the server for the requested
     * resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Object> getLandingPage(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetLandingPage().format(f)).getResponseEntity();
    }

    /**
     * GET /conformance : Retrieve the set of OGC API conformance classes that are supported by this service.
     *
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return The URIs of all conformance classes supported by the server (status code 200) or Content negotiation
     * failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media
     * types supported by the server for the requested resource. (status code 406) or A server error occurred. (status
     * code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/conformance",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<ConfClasses> getConformance(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetConformance().format(f)).getResponseEntity();
    }

    /**
     * GET /api : Retrieve this API definition.
     *
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return The OpenAPI definition of the API. (status code 200) or Content negotiation failed. For example, the
     * &#x60;Accept&#x60; header submitted in the request did not support any of the media types supported by the server
     * for the requested resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/api",
            produces = {OPENAPI_MEDIATYPE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Object> getAPI(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetApi().format(f)).getResponseEntity();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Feature query API ///////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * GET /functions : Retrieve the set of supported CQL2 functions.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/functions",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<ConfClasses> getFunctions(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetFunctions().format(f)).getResponseEntity();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Collection API //////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * GET /collections : Retrieve the list of geospatial data collections available from this service.
     *
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @param bbox Only resources that have a geometry that intersects the bounding box are selected. The bounding box
     * is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis
     * (elevation or depth): * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum
     * value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis
     * 2 * Maximum value, coordinate axis 3 (optional) If the value consists of four numbers, the coordinate reference
     * system is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate
     * reference system is specified in the parameter &#x60;bbox-crs&#x60;. If the value consists of six numbers, the
     * coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height
     * (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in a
     * parameter &#x60;bbox-crs&#x60;. For WGS84 longitude/latitude the values are in most cases the sequence of minimum
     * longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the
     * antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge). If the
     * vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional
     * bounding box. If a resource has multiple spatial geometry properties, it is the decision of the server whether
     * only a single spatial geometry property is used to determine the extent or all relevant geometries. (optional)
     * @param limit The optional limit parameter limits the number of collections that are presented in the response
     * document. Only items are counted that are on the first level of the collection in the response document. Nested
     * objects contained within the explicitly requested items shall not be counted. * Minimum &#x3D; 1 * Maximum &#x3D;
     * 10000 * Default &#x3D; 10 (optional, default to 10)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return The collections of (mostly geospatial) data available from this API. The dataset contains one or more
     * collections. This resource provides information about and access to the collections. The response contains the
     * list of collections. Each collection is accessible via one or more OGC API set of specifications, for which a
     * link to relevant accessible resources, e.g. /collections/{collectionId}/(items, coverage, map, tiles...) is
     * provided, with the corresponding relation type, as well as key information about the collection. This information
     * includes: * a local identifier for the collection that is unique for the dataset; * a list of coordinate
     * reference systems (CRS) in which data may be returned by the server. The first CRS is the default coordinate
     * reference system (the default is always WGS 84 with axis order longitude/latitude); * an optional title and
     * description for the collection; * an optional extent that can be used to provide an indication of the spatial and
     * temporal extent of the collection - typically derived from the data; * for collections accessible via the
     * Features or Records API, an optional indicator about the type of the items in the collection (the default value,
     * if the indicator is not provided, is &#39;feature&#39;). (status code 200)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Collections> getCollectionList(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "bbox", required = false) List<Double> bbox,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        GeneralEnvelope env = null;
        if (bbox != null && !bbox.isEmpty()) {
            if (bbox.size() == 4) {
                env = new GeneralEnvelope(CommonCRS.WGS84.geographic());
                env.setRange(0, bbox.get(0), bbox.get(2));
                env.setRange(1, bbox.get(1), bbox.get(3));
            } else if (bbox.size() == 6) {
                env = new GeneralEnvelope(CommonCRS.WGS84.geographic3D());
                env.setRange(0, bbox.get(0), bbox.get(3));
                env.setRange(1, bbox.get(1), bbox.get(4));
                env.setRange(2, bbox.get(2), bbox.get(5));
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        return treatIncomingRequest(new GetCollectionList().datetime(datetime).bbox(env).limit(limit).format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId} : Retrieve the description of a collection available from this service.
     *
     * @param collectionId Local identifier of a collection (required)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return Information about a particular collection of (mostly geospatial) data available from this API. The
     * collection is accessible via one or more OGC API set of specifications, for which a link to relevant accessible
     * resources, e.g. /collections/{collectionId}/(items, coverage, map, tiles...) is contained in the response, with
     * the corresponding relation type, as well as key information about the collection. This information includes: * a
     * local identifier for the collection that is unique for the dataset; * a list of coordinate reference systems
     * (CRS) in which data may be returned by the server. The first CRS is the default coordinate reference system (the
     * default is always WGS 84 with axis order longitude/latitude); * an optional title and description for the
     * collection; * an optional extent that can be used to provide an indication of the spatial and temporal extent of
     * the collection - typically derived from the data; * for collections accessible via the Features or Records API,
     * an optional indicator about the type of the items in the collection (the default value, if the indicator is not
     * provided, is &#39;feature&#39;). (status code 200)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<CollectionDescription> getCollection(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetCollection().collectionId(collectionId).format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/schema : Retrieve the description of a collection
     *
     * @param collectionId Local identifier of a collection (required)
     * @return Descriptive information of the collection
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/schema",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<CollectionDescription> getCollectionSchema(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetCollectionSchema().collectionId(collectionId).format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/queryables
     *
     * @param collectionId Local identifier of a collection (required)
     * @return Descriptive information of queryables
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/queryables",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ResponseEntity<CollectionDescription> getCollectionQueryables(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetCollectionQueryables().collectionId(collectionId).format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/metadata
     *
     * @param collectionId Local identifier of a collection (required)
     * @return Descriptive information of queryables
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/metadata",
            produces = {MediaType.APPLICATION_XML_VALUE}
    )
    public ResponseEntity<String> getCollectionMetadata(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetCollectionMetadata().collectionId(collectionId).format(f)).getResponseEntity();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Collection DGGRS API/////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * GET /collections/{collectionId}/dggs : Retrieve the list of available DGGRS for the specified collection
     *
     * @param collectionId Local identifier of a collection (required)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return List of available Discrete Global Grid Reference Systems. (status code 200) or The requested resource
     * does not exist on the server. For example, a path parameter had an incorrect value. (status code 404) or Content
     * negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of
     * the media types supported by the server for the requested resource. (status code 406) or A server error occurred.
     * (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<DggrsListResponse> getCollectionDggsList(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetDggrsList().collectionId(collectionId).format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/dggs/{dggrsId} : Retrieve the description of the specified Discrete Global Grid
     * Reference System in the context of a specified collection
     *
     * @param collectionId Local identifier of a collection (required)
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return Description for a specific Discrete Global Grid Reference System. (status code 200) or The requested
     * resource does not exist on the server. For example, a path parameter had an incorrect value. (status code 404) or
     * Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support
     * any of the media types supported by the server for the requested resource. (status code 406) or A server error
     * occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs/{dggrsId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Dggrs> getCollectionDggrs(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetDggrs().collectionId(collectionId).dggrsId(dggrsId).format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/dggs/{dggrsId}/definition : Retrieve the full definition of the specified Discrete Global Grid Reference System
     *
     * @param collectionId Local identifier of a collection (required)
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return Description for a specific Discrete Global Grid Reference System. (status code 200) or The requested DGGS
     * id was not found (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60; header
     * submitted in the request did not support any of the media types supported by the server for the requested
     * resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs/{dggrsId}/definition",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Dggrs> getCollectionDggrsDefinition(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers) {
        putServiceIdParam(serviceId);
        return treatIncomingRequest(new GetDggrsDefinition().collectionId(collectionId).dggrsId(dggrsId)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/dggs/{dggrsId}/zones : Retrieve the list of zones with data for a specific
     * collection, or for a particular query
     *
     * @param collectionId Local identifier of a collection (required)
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param collections The collections that should be included in the response. The parameter value is a
     * comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be
     * included. This parameter may be useful for dataset-wide DGGS resources, but it is not defined by OGC API - DGGS -
     * Part 1. (optional)
     * @param bbox Only resources that have a geometry that intersects the bounding box are selected. The bounding box
     * is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis
     * (elevation or depth): * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum
     * value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis
     * 2 * Maximum value, coordinate axis 3 (optional) If the value consists of four numbers, the coordinate reference
     * system is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate
     * reference system is specified in the parameter &#x60;bbox-crs&#x60;. If the value consists of six numbers, the
     * coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height
     * (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in a
     * parameter &#x60;bbox-crs&#x60;. For WGS84 longitude/latitude the values are in most cases the sequence of minimum
     * longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the
     * antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge). If the
     * vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional
     * bounding box. If a resource has multiple spatial geometry properties, it is the decision of the server whether
     * only a single spatial geometry property is used to determine the extent or all relevant geometries. (optional)
     * @param bboxCrs crs for the specified bbox (optional)
     * @param compactZones If set to true (default), when the list of DGGS zones to be returned at the requested
     * resolution (zone-level) includes all children of a parent zone, the parent zone will be returned as a shorthand
     * for that list of children zone. If set to false, all zones returned will be of the requested zone level.
     * (optional, default to true)
     * @param limit The optional limit parameter limits the number of zones that are presented in the response document.
     * * Minimum &#x3D; 1 * Maximum &#x3D; 10000 * Default &#x3D; 1000 (optional, default to 1000)
     * @param parentZone The optional parent zone parameter restricts a zone query to only return zones within that
     * parent zone. Used together with &#x60;zone-level&#x60;, it allows to explore the response for a large zone query
     * in a hierarchical manner. (optional)
     * @param offset The optional offset parameter indicates the offset within the result set from which the server
     * shall begin presenting results in the response document. The first element has an offset of 0 (default).
     * (optional, default to 0)
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @param subset Retrieve only part of the data by slicing or trimming along one or more axis For trimming:
     * {axisAbbrev}({low}:{high}) (preserves dimensionality) For slicing: {axisAbbrev}({value}) (reduces dimensionality)
     * An asterisk (&#x60;*&#x60;) can be used instead of {low} or {high} to indicate the minimum/maximum value. For a
     * temporal dimension, a single asterisk can be used to indicate the high value. Support for &#x60;*&#x60; is
     * required for time, but optional for spatial and other dimensions. (optional)
     * @param subsetCrs crs for the specified subset (optional)
     * @param outputCrs reproject the output to the given crs (optional)
     * @param geometry For vector output formats, specify how to return the geometry and/or what the features of the
     * response should represent. &#x60;vectorized&#x60;: return features with regular non-rasterized, non-quantized
     * geometry &#x60;zone-centroid&#x60;: rasterize to zone features and use a Point geometry representing that zone
     * centroid &#x60;zone-region&#x60;: rasterize to zone features and use a (Multi)Polygon/Polyhedron geometry
     * representing that zone&#39;s region -- not supported for DGGS-JSON-FG profiles
     * (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) &#x60;none&#x60;: (for zone listing) omit zone geometry -- not supported
     * for DGGS-JSON-FG profiles (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) (optional)
     * @param profile Allows negotiating a particular profile of an output format, such as OGC Feature &amp; Geometry
     * JSON (JSON-FG) or DGGS-JSON-FG output when requesting an &#x60;application/geo+json&#x60; media type for zone
     * data or zone list requests. For both zone data and zone lists in GeoJSON (&#x60;application/geo+json&#x60;):
     * &#x60;rfc7946&#x60;: return standard GeoJSON without using any extension &#x60;jsonfg&#x60;: return JSON-FG
     * representation &#x60;jsonfg-plus&#x60;: return JSON-FG representation with GeoJSON compatibility For zone data in
     * GeoJSON (&#x60;application/geo+json&#x60;): &#x60;jsonfg-dggs&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points quantized to sub-zone, represented as local indices from 1 to the
     * number of sub-zones corresponding to the DGGRS deterministic sub-zone order, with a special value of 0
     * representing an artificial node &#x60;jsonfg-dggs-plus&#x60;: return DGGS-JSON-FG representation, with GeoJSON
     * compatibility &#x60;geometry&#x60; &#x60;jsonfg-dggs-zoneids&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points as textual global zone identifiers, with a special value of
     * _null_ representing an artificial node &#x60;jsonfg-dggs-zoneids-plus&#x60;: return DGGS-JSON-FG representation,
     * encoding geometry points as global zone IDs, with GeoJSON compatibility &#x60;geometry&#x60; For zone data in
     * netCDF (&#x60;application/x-netcdf&#x60;): &#x60;netcdf3&#x60;: return NetCDF classic and 64-bit offset format
     * (not quantized to DGGH) &#x60;netcdf3-dggs&#x60;: return NetCDF classic and 64-bit offset format where one axis
     * corresponds to local sub-zone indices &#x60;netcdf3-dggs-zoneids&#x60;: return NetCDF classic and 64-bit offset
     * format where one axis corresponds to the global identifiers of sub-zones (textual or 64-bit integer)
     * &#x60;netcdf4&#x60;: return HFG5-based NetCDF 4 format (not quantized to DGGH) &#x60;netcdf4-dggs&#x60;: return
     * HDF5-based NetCDF 4 format where one axis corresponds to local sub-zone indices &#x60;netcdf4-dggs-zoneids&#x60;:
     * return HDF5-based NetCDF 4 format where one axis corresponds to the global identifiers of sub-zones (textual or
     * 64-bit integer) For zone data in zipped Zarr 2.0 (&#x60;application/zarr+zip&#x60;): &#x60;zarr2&#x60;: return
     * zipped Zarr 2.0 (not quantized to DGGH) &#x60;zarr2-dggs&#x60;: return zipped Zarr 2.0 where one axis corresponds
     * to local sub-zone indices &#x60;zarr2-dggs-zoneids&#x60;: return zipped Zarr 2.0 where one axis corresponds to
     * the global identifiers of sub-zones (textual or 64-bit integer) For zone data in CoverageJSON
     * (&#x60;application/prs.coverage+json&#x60;): &#x60;covjson&#x60;: return CoverageJSON (not quantized to DGGH)
     * &#x60;covjson-dggs&#x60;: return CoverageJSON where one axis corresponds to local sub-zone indices
     * &#x60;covjson-dggs-zoneids&#x60;: return CoverageJSON where one axis corresponds to the global identifiers of
     * sub-zones (textual or 64-bit integer) (optional)
     * @param filter The filter parameter specifies an expression in a query language (e.g. CQL2) for which an entire
     * feature will be returned if the filter predicate is matched. The language of the filter is specified by the
     * &#x60;filter-lang&#x60; query parameter. (optional)
     * @param filterLang The &#x60;filter-lang&#x60; parameter specifies the query language for the &#x60;filter&#x60;
     * query parameter. (optional)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39;, &#39;html&#39;, &#39;geojson&#39;, &#39;geotiff&#39; or &#39;uint64&#39;.
     * (optional)
     * @return List of DGGRS Zones. In addition to a compact JSON response intended for fast DGGS client/server exchange
     * (which should support compression), visual representations such as GeoTIFF and/or GeoJSON may also be supported.
     * (status code 200) or Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the
     * request did not support any of the media types supported by the server for the requested resource. (status code
     * 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs/{dggrsId}/zones",
            produces = {MediaTypes.ZONELIST_GEOJSON, MediaTypes.ZONELIST_GEOTIFF, MediaTypes.ZONELIST_HTML, MediaTypes.ZONELIST_JSON, MediaTypes.ZONELIST_UINT64}
    )
    public ResponseEntity<DggrsZonesResponse> getCollectionDggsZoneList(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "bbox", required = false) List<Double> bbox,
            @RequestParam(value = "bbox-crs", required = false) String bboxCrs,
            @RequestParam(value = "zone-level", required = false) Integer zoneLevel,
            @RequestParam(value = "compact-zones", required = false, defaultValue = "true") Boolean compactZones, //todo
            @RequestParam(value = "limit", required = false, defaultValue = "1000") Integer limit,
            @RequestParam(value = "parent-zone", required = false) String parentZone,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "subset", required = false) List<String> subset,
            @RequestParam(value = "subset-crs", required = false) String subsetCrs,
            @RequestParam(value = "crs", required = false) String outputCrs,
            @RequestParam(value = "geometry", required = false) String geometry,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "filter-lang", required = false) String filterLang,
            @RequestParam(value = "f", required = false) String f) throws FactoryException {
        putServiceIdParam(serviceId);
        final GeneralEnvelope env;
        try {
            f = defaultFormat(f, headers);
            env = parseBbox(bbox, bboxCrs);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }

        final List<String> col = new ArrayList();
        col.add(collectionId);
        if (collections != null && !collections.isEmpty() ){
            for (String s : collections) {
                if (!col.contains(s)) {
                    col.add(s);
                }
            }
        }

        return treatIncomingRequest(new GetZoneList()
                .collectionId(col)
                .dggrsId(dggrsId)
                .bbox(env)
                .zoneLevel(zoneLevel)
                .compactZones(compactZones)
                .limit(limit)
                .parentZone(parentZone)
                .offset(offset)
                .subset(subset)
                .datetime(datetime)
                .subsetCrs(subsetCrs)
                .crs(outputCrs)
                .geometry(geometry)
                .profile(profile)
                .filter(filter)
                .filterLang(filterLang)
                .format(f)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/dggs/{dggrsId}/zones/{zoneId} : Retrieve information about a DGGRS Zone, such as
     * geometry and data availability, in the context of a specific collection.
     *
     * @param collectionId Local identifier of a collection (required)
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param zoneId Identifier for a specific zone of a Discrete Global Grid Systems. This identifier usually includes
     * a component corresponding to a hierarchy level / scale / resolution, components identifying a spatial region, and
     * a optionally a temporal component. (required)
     * @param collections The collections that should be included in the response. The parameter value is a
     * comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be
     * included. This parameter may be useful for dataset-wide DGGS resources, but it is not defined by OGC API - DGGS -
     * Part 1. (optional)
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @return (status code 200) or The requested resource does not exist on the server. For example, a path parameter
     * had an incorrect value. (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60;
     * header submitted in the request did not support any of the media types supported by the server for the requested
     * resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs/{dggrsId}/zones/{zoneId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Void> getCollectionDggrsZone(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @PathVariable("dggrsId") String dggrsId,
            @PathVariable("zoneId") String zoneId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetZone().collectionId(collectionId).dggrsId(dggrsId).zoneId(zoneId).format(f).collections(collections).datetime(datetime)).getResponseEntity();
    }

    /**
     * GET /collections/{collectionId}/dggs/{dggrsId}/zones/{zoneId}/data : Retrieve data from a DGGRS Zone for a
     * specific collection. For a DGGRS defining a sub-zone order, optimized zone data packets such as DGGS-(UB)JSON for
     * raster data, or DGGS-(UB)JSON-FG for vector data can be used.
     *
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param zoneId Identifier for a specific zone of a Discrete Global Grid Systems. This identifier usually includes
     * a component corresponding to a hierarchy level / scale / resolution, components identifying a spatial region, and
     * a optionally a temporal component. (required)
     * @param collectionId Local identifier of a collection (required)
     * @param f The format of the zone data response (e.g. GeoJSON, GeoTIFF). (optional)
     * @param properties Select specific data record fields (measured/observed properties) to be returned using a
     * comma-separated list of field names. The field name must be one of the fields defined in the associated data
     * resource&#39;s logical schema. Extensions may enable the use of complex expressions to support defining derived
     * fields, potentially also including the possibility to use aggregation functons. (optional)
     * @param excludeProperties Exclude specific data record fields (measured/observed properties) from being returned
     * using a comma-separated list of field names. The field name must be one of the fields defined in the associated
     * data resource&#39;s logical schema. (optional)
     * @param collections
     * @param subset Retrieve only part of the data by slicing or trimming along one or more axis For trimming:
     * {axisAbbrev}({low}:{high}) (preserves dimensionality) For slicing: {axisAbbrev}({value}) (reduces dimensionality)
     * An asterisk (&#x60;*&#x60;) can be used instead of {low} or {high} to indicate the minimum/maximum value. For a
     * temporal dimension, a single asterisk can be used to indicate the high value. Support for &#x60;*&#x60; is
     * required for time, but optional for spatial and other dimensions. (optional)
     * @param filter The filter parameter specifies an expression in a query language (e.g. CQL2) for which an entire
     * feature will be returned if the filter predicate is matched. The language of the filter is specified by the
     * &#x60;filter-lang&#x60; query parameter. (optional)
     * @param crs reproject the output to the given crs (optional)
     * @param geometry For vector output formats, specify how to return the geometry and/or what the features of the
     * response should represent. &#x60;vectorized&#x60;: return features with regular non-rasterized, non-quantized
     * geometry &#x60;zone-centroid&#x60;: rasterize to zone features and use a Point geometry representing that zone
     * centroid &#x60;zone-region&#x60;: rasterize to zone features and use a (Multi)Polygon/Polyhedron geometry
     * representing that zone&#39;s region -- not supported for DGGS-JSON-FG profiles
     * (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) &#x60;none&#x60;: (for zone listing) omit zone geometry -- not supported
     * for DGGS-JSON-FG profiles (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) (optional)
     * @param profile Allows negotiating a particular profile of an output format, such as OGC Feature &amp; Geometry
     * JSON (JSON-FG) or DGGS-JSON-FG output when requesting an &#x60;application/geo+json&#x60; media type for zone
     * data or zone list requests. For both zone data and zone lists in GeoJSON (&#x60;application/geo+json&#x60;):
     * &#x60;rfc7946&#x60;: return standard GeoJSON without using any extension &#x60;jsonfg&#x60;: return JSON-FG
     * representation &#x60;jsonfg-plus&#x60;: return JSON-FG representation with GeoJSON compatibility For zone data in
     * GeoJSON (&#x60;application/geo+json&#x60;): &#x60;jsonfg-dggs&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points quantized to sub-zone, represented as local indices from 1 to the
     * number of sub-zones corresponding to the DGGRS deterministic sub-zone order, with a special value of 0
     * representing an artificial node &#x60;jsonfg-dggs-plus&#x60;: return DGGS-JSON-FG representation, with GeoJSON
     * compatibility &#x60;geometry&#x60; &#x60;jsonfg-dggs-zoneids&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points as textual global zone identifiers, with a special value of
     * _null_ representing an artificial node &#x60;jsonfg-dggs-zoneids-plus&#x60;: return DGGS-JSON-FG representation,
     * encoding geometry points as global zone IDs, with GeoJSON compatibility &#x60;geometry&#x60; For zone data in
     * netCDF (&#x60;application/x-netcdf&#x60;): &#x60;netcdf3&#x60;: return NetCDF classic and 64-bit offset format
     * (not quantized to DGGH) &#x60;netcdf3-dggs&#x60;: return NetCDF classic and 64-bit offset format where one axis
     * corresponds to local sub-zone indices &#x60;netcdf3-dggs-zoneids&#x60;: return NetCDF classic and 64-bit offset
     * format where one axis corresponds to the global identifiers of sub-zones (textual or 64-bit integer)
     * &#x60;netcdf4&#x60;: return HFG5-based NetCDF 4 format (not quantized to DGGH) &#x60;netcdf4-dggs&#x60;: return
     * HDF5-based NetCDF 4 format where one axis corresponds to local sub-zone indices &#x60;netcdf4-dggs-zoneids&#x60;:
     * return HDF5-based NetCDF 4 format where one axis corresponds to the global identifiers of sub-zones (textual or
     * 64-bit integer) For zone data in zipped Zarr 2.0 (&#x60;application/zarr+zip&#x60;): &#x60;zarr2&#x60;: return
     * zipped Zarr 2.0 (not quantized to DGGH) &#x60;zarr2-dggs&#x60;: return zipped Zarr 2.0 where one axis corresponds
     * to local sub-zone indices &#x60;zarr2-dggs-zoneids&#x60;: return zipped Zarr 2.0 where one axis corresponds to
     * the global identifiers of sub-zones (textual or 64-bit integer) For zone data in CoverageJSON
     * (&#x60;application/prs.coverage+json&#x60;): &#x60;covjson&#x60;: return CoverageJSON (not quantized to DGGH)
     * &#x60;covjson-dggs&#x60;: return CoverageJSON where one axis corresponds to local sub-zone indices
     * &#x60;covjson-dggs-zoneids&#x60;: return CoverageJSON where one axis corresponds to the global identifiers of
     * sub-zones (textual or 64-bit integer) (optional)
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @param zoneDepth The DGGS resolution levels beyond the requested DGGS zone’s hierarchy level to include in the
     * response, when retrieving data for that zone. This can be either: • A single positive integer value —
     * representing a specific zone depth to return (e.g., &#x60;zone-depth&#x3D;5&#x60;); • A range of positive integer
     * values in the form “{low}-{high}” — representing a continuous range of zone depths to return (e.g.,
     * &#x60;zone-depth&#x3D;1-8&#x60;); or, • A comma separated list of at least two (2) positive integer values —
     * representing a set of specific zone depths to return (e.g., &#x60;zone-depth&#x3D;1,3,7&#x60;). Some or all of
     * these forms of the zone-depth parameter may not be supported with particular data packet encodings (the data
     * encoding may support a fixed depth, a range of depths, and/or an arbitrary selection of depths). When this
     * parameter is omitted, the default value specified in the &#x60;defaultDepth&#x60; property of the
     * &#x60;.../dggs/{dggrsId}&#x60; DGGRS description is used. (optional)
     * @param valuesOffset Specify the offset for a zone data output format such as PNG not supporting floating-point,
     * to be applied after multiplying by the scale factor and resulting in the encoded integer values (e.g., 8-bit or
     * 16-bit unsigned for PNG). (optional)
     * @param valuesScale Specify the scale factor for a zone data output format such as PNG not supporting
     * floating-point, to be applied before adding an offset and resulting in the encoded integer values (e.g., 8-bit or
     * 16-bit unsigned for PNG). (optional)
     * @return DGGRS zone data returned as a response. (status code 200) or No data available for this zone. (status
     * code 204) or The requested resource does not exist on the server. For example, a path parameter had an incorrect
     * value. (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in
     * the request did not support any of the media types supported by the server for the requested resource. (status
     * code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs/{dggrsId}/zones/{zoneId}/data",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE, MediaTypes.DATA_PNG, MediaTypes.DATA_GEOTIFF, MediaTypes.DATA_GEOJSON, MediaTypes.DATA_COVERAGEJSON, MediaTypes.DATA_UBJSON}
    )
    public ResponseEntity<DggrsData> getCollectionDggrsZoneData(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @PathVariable("dggrsId") String dggrsId,
            @PathVariable("zoneId") String zoneId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "properties", required = false) String properties,
            @RequestParam(value = "exclude-properties", required = false) String excludeProperties,
            @RequestParam(value = "subset", required = false) List<String> subset,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "crs", required = false) String crs,
            @RequestParam(value = "geometry", required = false) String geometry,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "zone-depth", required = false) String zoneDepth,
            @RequestParam(value = "values-offset", required = false) Double valuesOffset,
            @RequestParam(value = "values-scale", required = false) Double valuesScale) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        final List<String> col = new ArrayList();
        col.add(collectionId);
        if (collections != null && !collections.isEmpty() ){
            for (String s : collections) {
                if (!col.contains(s)) {
                    col.add(s);
                }
            }
        }
        return treatIncomingRequest(new GetZoneData()
                .collectionId(col)
                .dggrsId(dggrsId)
                .zoneId(zoneId)
                .format(f)
                .properties(properties)
                .excludeProperties(excludeProperties)
                .subset(subset)
                .datetime(datetime)
                .filter(filter)
                .crs(crs)
                .geometry(geometry)
                .profile(profile)
                .zoneDepth(zoneDepth)
                .valuesOffset(valuesOffset)
                .valuesScale(valuesScale)
                .gzip(askingGzip(headers))).getResponseEntity();
    }

    /**
     * EXTENSION EXPERIMENTAL.
     * GET /collections/{collectionId}/dggs/{dggrsId}/data : Retrieve data from a DGGRS for a
     * specific collection.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/collections/{collectionId}/dggs/{dggrsId}/data",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE, MediaTypes.DATA_PNG, MediaTypes.DATA_GEOTIFF, MediaTypes.DATA_GEOJSON, MediaTypes.DATA_COVERAGEJSON, MediaTypes.DATA_UBJSON}
    )
    public ResponseEntity<DggrsData> getCollectionDggrsData(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("collectionId") String collectionId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "domain", required = true) String area,
            @RequestParam(value = "domainCrs", required = true) String areaCrs,
            @RequestParam(value = "properties", required = false) String properties,
            @RequestParam(value = "exclude-properties", required = false) String excludeProperties,
            @RequestParam(value = "subset", required = false) List<String> subset,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "crs", required = false) String crs,
            @RequestParam(value = "geometry", required = false) String geometry,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "zone-depth", required = false) String zoneDepth,
            @RequestParam(value = "zone-accuracy", required = false) String zoneAccuracy,
            @RequestParam(value = "values-offset", required = false) Double valuesOffset,
            @RequestParam(value = "values-scale", required = false) Double valuesScale) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }

        final List<String> col = new ArrayList();
        if (collectionId != null) col.add(collectionId);
        if (collections != null && !collections.isEmpty() ){
            for (String s : collections) {
                if (!col.contains(s)) {
                    col.add(s);
                }
            }
        }
        return treatIncomingRequest(new GetZoneData()
                .collectionId(col)
                .dggrsId(dggrsId)
                .area(area)
                .areaCrs(areaCrs)
                .format(f)
                .properties(properties)
                .excludeProperties(excludeProperties)
                .subset(subset)
                .datetime(datetime)
                .filter(filter)
                .crs(crs)
                .geometry(geometry)
                .profile(profile)
                .zoneDepth(zoneDepth)
                .zoneAccuracy(zoneAccuracy)
                .valuesOffset(valuesOffset)
                .valuesScale(valuesScale)
                .gzip(askingGzip(headers))).getResponseEntity();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Dataset API /////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * GET /dggs : Retrieve the list of available DGGRSs
     *
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return List of available Discrete Global Grid Reference Systems. (status code 200) or Content negotiation
     * failed. For example, the &#x60;Accept&#x60; header submitted in the request did not support any of the media
     * types supported by the server for the requested resource. (status code 406) or A server error occurred. (status
     * code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<DggrsListResponse> getDggrsList(
            @PathVariable("serviceId") String serviceId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetDggrsList().format(f)).getResponseEntity();
    }

    /**
     * GET /dggs/{dggrsId} : Retrieve the description of the specified Discrete Global Grid Reference System
     *
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return Description for a specific Discrete Global Grid Reference System. (status code 200) or The requested DGGS
     * id was not found (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60; header
     * submitted in the request did not support any of the media types supported by the server for the requested
     * resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs/{dggrsId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Dggrs> getDggrs(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetDggrs().dggrsId(dggrsId).format(f)).getResponseEntity();
    }

    /**
     * GET /dggs/{dggrsId}/definition : Retrieve the full definition of the specified Discrete Global Grid Reference System
     *
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39; or &#39;html&#39;. (optional)
     * @return Description for a specific Discrete Global Grid Reference System. (status code 200) or The requested DGGS
     * id was not found (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60; header
     * submitted in the request did not support any of the media types supported by the server for the requested
     * resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs/{dggrsId}/definition",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
    )
    public ResponseEntity<Dggrs> getDggrsDefinition(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers) {
        putServiceIdParam(serviceId);
        return treatIncomingRequest(new GetDggrsDefinition().dggrsId(dggrsId)).getResponseEntity();
    }


    /**
     * EXTENSION EXPERIMENTAL.
     * GET /dggs/{dggrsId}/data : Retrieve data from a DGGRS.
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs/{dggrsId}/data",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE, MediaTypes.DATA_PNG, MediaTypes.DATA_GEOTIFF, MediaTypes.DATA_GEOJSON, MediaTypes.DATA_COVERAGEJSON, MediaTypes.DATA_UBJSON}
    )
    public ResponseEntity<DggrsData> getDggrsData(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "f", required = false) String f,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "domain", required = true) String area,
            @RequestParam(value = "domainCrs", required = true) String areaCrs,
            @RequestParam(value = "properties", required = false) String properties,
            @RequestParam(value = "exclude-properties", required = false) String excludeProperties,
            @RequestParam(value = "subset", required = false) List<String> subset,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "crs", required = false) String crs,
            @RequestParam(value = "geometry", required = false) String geometry,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "zone-depth", required = false) String zoneDepth,
            @RequestParam(value = "zone-accuracy", required = false) String zoneAccuracy,
            @RequestParam(value = "values-offset", required = false) Double valuesOffset,
            @RequestParam(value = "values-scale", required = false) Double valuesScale) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }

        final List<String> col = new ArrayList();
        if (collections != null && !collections.isEmpty() ){
            for (String s : collections) {
                if (!col.contains(s)) {
                    col.add(s);
                }
            }
        }
        return treatIncomingRequest(new GetZoneData()
                .collectionId(col)
                .dggrsId(dggrsId)
                .area(area)
                .areaCrs(areaCrs)
                .format(f)
                .properties(properties)
                .excludeProperties(excludeProperties)
                .subset(subset)
                .datetime(datetime)
                .filter(filter)
                .crs(crs)
                .geometry(geometry)
                .profile(profile)
                .zoneDepth(zoneDepth)
                .zoneAccuracy(zoneAccuracy)
                .valuesOffset(valuesOffset)
                .valuesScale(valuesScale)
                .gzip(askingGzip(headers))).getResponseEntity();
    }

    /**
     * GET /dggs/{dggrsId}/zones : Retrieve the list of zones with data for this dataset, or for a particular query
     *
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param collections The collections that should be included in the response. The parameter value is a
     * comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be
     * included. This parameter may be useful for dataset-wide DGGS resources, but it is not defined by OGC API - DGGS -
     * Part 1. (optional)
     * @param bbox Only resources that have a geometry that intersects the bounding box are selected. The bounding box
     * is provided as four or six numbers, depending on whether the coordinate reference system includes a vertical axis
     * (elevation or depth): * Lower left corner, coordinate axis 1 * Lower left corner, coordinate axis 2 * Minimum
     * value, coordinate axis 3 (optional) * Upper right corner, coordinate axis 1 * Upper right corner, coordinate axis
     * 2 * Maximum value, coordinate axis 3 (optional) If the value consists of four numbers, the coordinate reference
     * system is WGS84 longitude/latitude (http://www.opengis.net/def/crs/OGC/1.3/CRS84) unless a different coordinate
     * reference system is specified in the parameter &#x60;bbox-crs&#x60;. If the value consists of six numbers, the
     * coordinate reference system is WGS 84 longitude/latitude/ellipsoidal height
     * (http://www.opengis.net/def/crs/OGC/0/CRS84h) unless a different coordinate reference system is specified in a
     * parameter &#x60;bbox-crs&#x60;. For WGS84 longitude/latitude the values are in most cases the sequence of minimum
     * longitude, minimum latitude, maximum longitude and maximum latitude. However, in cases where the box spans the
     * antimeridian the first value (west-most box edge) is larger than the third value (east-most box edge). If the
     * vertical axis is included, the third and the sixth number are the bottom and the top of the 3-dimensional
     * bounding box. If a resource has multiple spatial geometry properties, it is the decision of the server whether
     * only a single spatial geometry property is used to determine the extent or all relevant geometries. (optional)
     * @param bboxCrs crs for the specified bbox (optional)
     * @param zoneLevel The DGGS hierarchy level at which to return the list of zones. The precision of the calculation
     * to return the results depends on this parameter. Returned zones will have a level equal or smaller to this
     * specified level. If &#x60;compact-zones&#x60; is set to true, all returned zones will be of this zone level. If
     * not specified, this defaults to the most detailed zone that the system is able to return for the specific
     * request. (optional)
     * @param compactZones If set to true (default), when the list of DGGS zones to be returned at the requested
     * resolution (zone-level) includes all children of a parent zone, the parent zone will be returned as a shorthand
     * for that list of children zone. If set to false, all zones returned will be of the requested zone level.
     * (optional, default to true)
     * @param limit The optional limit parameter limits the number of zones that are presented in the response document.
     * * Minimum &#x3D; 1 * Maximum &#x3D; 10000 * Default &#x3D; 1000 (optional, default to 1000)
     * @param parentZone The optional parent zone parameter restricts a zone query to only return zones within that
     * parent zone. Used together with &#x60;zone-level&#x60;, it allows to explore the response for a large zone query
     * in a hierarchical manner. (optional)
     * @param offset The optional offset parameter indicates the offset within the result set from which the server
     * shall begin presenting results in the response document. The first element has an offset of 0 (default).
     * (optional, default to 0)
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @param subset Retrieve only part of the data by slicing or trimming along one or more axis For trimming:
     * {axisAbbrev}({low}:{high}) (preserves dimensionality) For slicing: {axisAbbrev}({value}) (reduces dimensionality)
     * An asterisk (&#x60;*&#x60;) can be used instead of {low} or {high} to indicate the minimum/maximum value. For a
     * temporal dimension, a single asterisk can be used to indicate the high value. Support for &#x60;*&#x60; is
     * required for time, but optional for spatial and other dimensions. (optional)
     * @param subsetCrs crs for the specified subset (optional)
     * @param crs reproject the output to the given crs (optional)
     * @param geometry For vector output formats, specify how to return the geometry and/or what the features of the
     * response should represent. &#x60;vectorized&#x60;: return features with regular non-rasterized, non-quantized
     * geometry &#x60;zone-centroid&#x60;: rasterize to zone features and use a Point geometry representing that zone
     * centroid &#x60;zone-region&#x60;: rasterize to zone features and use a (Multi)Polygon/Polyhedron geometry
     * representing that zone&#39;s region -- not supported for DGGS-JSON-FG profiles
     * (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) &#x60;none&#x60;: (for zone listing) omit zone geometry -- not supported
     * for DGGS-JSON-FG profiles (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) (optional)
     * @param profile Allows negotiating a particular profile of an output format, such as OGC Feature &amp; Geometry
     * JSON (JSON-FG) or DGGS-JSON-FG output when requesting an &#x60;application/geo+json&#x60; media type for zone
     * data or zone list requests. For both zone data and zone lists in GeoJSON (&#x60;application/geo+json&#x60;):
     * &#x60;rfc7946&#x60;: return standard GeoJSON without using any extension &#x60;jsonfg&#x60;: return JSON-FG
     * representation &#x60;jsonfg-plus&#x60;: return JSON-FG representation with GeoJSON compatibility For zone data in
     * GeoJSON (&#x60;application/geo+json&#x60;): &#x60;jsonfg-dggs&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points quantized to sub-zone, represented as local indices from 1 to the
     * number of sub-zones corresponding to the DGGRS deterministic sub-zone order, with a special value of 0
     * representing an artificial node &#x60;jsonfg-dggs-plus&#x60;: return DGGS-JSON-FG representation, with GeoJSON
     * compatibility &#x60;geometry&#x60; &#x60;jsonfg-dggs-zoneids&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points as textual global zone identifiers, with a special value of
     * _null_ representing an artificial node &#x60;jsonfg-dggs-zoneids-plus&#x60;: return DGGS-JSON-FG representation,
     * encoding geometry points as global zone IDs, with GeoJSON compatibility &#x60;geometry&#x60; For zone data in
     * netCDF (&#x60;application/x-netcdf&#x60;): &#x60;netcdf3&#x60;: return NetCDF classic and 64-bit offset format
     * (not quantized to DGGH) &#x60;netcdf3-dggs&#x60;: return NetCDF classic and 64-bit offset format where one axis
     * corresponds to local sub-zone indices &#x60;netcdf3-dggs-zoneids&#x60;: return NetCDF classic and 64-bit offset
     * format where one axis corresponds to the global identifiers of sub-zones (textual or 64-bit integer)
     * &#x60;netcdf4&#x60;: return HFG5-based NetCDF 4 format (not quantized to DGGH) &#x60;netcdf4-dggs&#x60;: return
     * HDF5-based NetCDF 4 format where one axis corresponds to local sub-zone indices &#x60;netcdf4-dggs-zoneids&#x60;:
     * return HDF5-based NetCDF 4 format where one axis corresponds to the global identifiers of sub-zones (textual or
     * 64-bit integer) For zone data in zipped Zarr 2.0 (&#x60;application/zarr+zip&#x60;): &#x60;zarr2&#x60;: return
     * zipped Zarr 2.0 (not quantized to DGGH) &#x60;zarr2-dggs&#x60;: return zipped Zarr 2.0 where one axis corresponds
     * to local sub-zone indices &#x60;zarr2-dggs-zoneids&#x60;: return zipped Zarr 2.0 where one axis corresponds to
     * the global identifiers of sub-zones (textual or 64-bit integer) For zone data in CoverageJSON
     * (&#x60;application/prs.coverage+json&#x60;): &#x60;covjson&#x60;: return CoverageJSON (not quantized to DGGH)
     * &#x60;covjson-dggs&#x60;: return CoverageJSON where one axis corresponds to local sub-zone indices
     * &#x60;covjson-dggs-zoneids&#x60;: return CoverageJSON where one axis corresponds to the global identifiers of
     * sub-zones (textual or 64-bit integer) (optional)
     * @param filter The filter parameter specifies an expression in a query language (e.g. CQL2) for which an entire
     * feature will be returned if the filter predicate is matched. The language of the filter is specified by the
     * &#x60;filter-lang&#x60; query parameter. (optional)
     * @param filterLang The &#x60;filter-lang&#x60; parameter specifies the query language for the &#x60;filter&#x60;
     * query parameter. (optional)
     * @param f The format of the response. If no value is provided, the accept header is used to determine the format.
     * Accepted values are &#39;json&#39;, &#39;html&#39;, &#39;geojson&#39;, &#39;geotiff&#39; or &#39;uint64&#39;.
     * (optional)
     * @return List of DGGRS Zones. In addition to a compact JSON response intended for fast DGGS client/server exchange
     * (which should support compression), visual representations such as GeoTIFF and/or GeoJSON may also be supported.
     * (status code 200) or Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in the
     * request did not support any of the media types supported by the server for the requested resource. (status code
     * 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs/{dggrsId}/zones",
            produces = {MediaTypes.ZONELIST_GEOJSON, MediaTypes.ZONELIST_GEOTIFF, MediaTypes.ZONELIST_HTML, MediaTypes.ZONELIST_JSON, MediaTypes.ZONELIST_UINT64}
    )
    public ResponseEntity<DggrsZonesResponse> getDggrsZoneList(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("dggrsId") String dggrsId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "bbox", required = false) List<Double> bbox,
            @RequestParam(value = "bbox-crs", required = false) String bboxCrs,
            @RequestParam(value = "zone-level", required = false) Integer zoneLevel,
            @RequestParam(value = "compact-zones", required = false, defaultValue = "true") Boolean compactZones,
            @RequestParam(value = "limit", required = false, defaultValue = "1000") Integer limit,
            @RequestParam(value = "parent-zone", required = false) String parentZone,
            @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "subset", required = false) List<String> subset,
            @RequestParam(value = "subset-crs", required = false) String subsetCrs,
            @RequestParam(value = "crs", required = false) String crs,
            @RequestParam(value = "geometry", required = false) String geometry,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "filter-lang", required = false) String filterLang,
            @RequestParam(value = "f", required = false) String f) throws FactoryException {
        putServiceIdParam(serviceId);
        final GeneralEnvelope env;
        try {
            f = defaultFormat(f, headers);
            env = parseBbox(bbox, bboxCrs);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }

        return treatIncomingRequest(new GetZoneList()
                .dggrsId(dggrsId)
                .collectionId(collections)
                .bbox(env)
                .zoneLevel(zoneLevel)
                .compactZones(compactZones)
                .limit(limit)
                .parentZone(parentZone)
                .offset(offset)
                .subset(subset)
                .datetime(datetime)
                .subsetCrs(subsetCrs)
                .crs(crs)
                .geometry(geometry)
                .profile(profile)
                .filter(filter)
                .filterLang(filterLang)
                .format(f)).getResponseEntity();
    }

    /**
     * GET /dggs/{dggrsId}/zones/{zoneId} : Retrieve information about a DGGRS Zone, such as geometry and data
     * availability.
     *
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param zoneId Identifier for a specific zone of a Discrete Global Grid Systems. This identifier usually includes
     * a component corresponding to a hierarchy level / scale / resolution, components identifying a spatial region, and
     * a optionally a temporal component. (required)
     * @param collections The collections that should be included in the response. The parameter value is a
     * comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be
     * included. This parameter may be useful for dataset-wide DGGS resources, but it is not defined by OGC API - DGGS -
     * Part 1. (optional)
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @return DGGRS zone information returned as a response, potentially including id, geometry, links to DGGRS (rel:
     * dggrs), dataset, (rel: dataset), collection (rel: geodata), data (rel: dggrs-zone-data) (if available for this
     * zone) (status code 200) or The requested resource does not exist on the server. For example, a path parameter had
     * an incorrect value. (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60; header
     * submitted in the request did not support any of the media types supported by the server for the requested
     * resource. (status code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs/{dggrsId}/zones/{zoneId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE, "application/geo+json"}
    )
    public ResponseEntity<ZoneInfo> getDggrsZone(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("dggrsId") String dggrsId,
            @PathVariable("zoneId") String zoneId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "f", required = false) String f) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }
        return treatIncomingRequest(new GetZone().dggrsId(dggrsId).zoneId(zoneId).format(f).collections(collections).datetime(datetime)).getResponseEntity();
    }

    /**
     * GET /dggs/{dggrsId}/zones/{zoneId}/data : Retrieve data from a DGGRS Zone. For a DGGRS defining a sub-zone order,
     * optimized zone data packets such as DGGS-(UB)JSON for raster data, or DGGS-(UB)JSON-FG for vector data can be
     * used.
     *
     * @param dggrsId Identifier for a supported Discrete Global Grid System (required)
     * @param zoneId Identifier for a specific zone of a Discrete Global Grid Systems. This identifier usually includes
     * a component corresponding to a hierarchy level / scale / resolution, components identifying a spatial region, and
     * a optionally a temporal component. (required)
     * @param collections The collections that should be included in the response. The parameter value is a
     * comma-separated list of collection identifiers. If the parameters is missing, some or all collections will be
     * included. This parameter may be useful for dataset-wide DGGS resources, but it is not defined by OGC API - DGGS -
     * Part 1. (optional)
     * @param f The format of the zone data response (e.g. GeoJSON, GeoTIFF). (optional)
     * @param properties Select specific data record fields (measured/observed properties) to be returned using a
     * comma-separated list of field names. The field name must be one of the fields defined in the associated data
     * resource&#39;s logical schema. Extensions may enable the use of complex expressions to support defining derived
     * fields, potentially also including the possibility to use aggregation functons. (optional)
     * @param excludeProperties Exclude specific data record fields (measured/observed properties) from being returned
     * using a comma-separated list of field names. The field name must be one of the fields defined in the associated
     * data resource&#39;s logical schema. (optional)
     * @param subset Retrieve only part of the data by slicing or trimming along one or more axis For trimming:
     * {axisAbbrev}({low}:{high}) (preserves dimensionality) For slicing: {axisAbbrev}({value}) (reduces dimensionality)
     * An asterisk (&#x60;*&#x60;) can be used instead of {low} or {high} to indicate the minimum/maximum value. For a
     * temporal dimension, a single asterisk can be used to indicate the high value. Support for &#x60;*&#x60; is
     * required for time, but optional for spatial and other dimensions. (optional)
     * @param filter The filter parameter specifies an expression in a query language (e.g. CQL2) for which an entire
     * feature will be returned if the filter predicate is matched. The language of the filter is specified by the
     * &#x60;filter-lang&#x60; query parameter. (optional)
     * @param crs reproject the output to the given crs (optional)
     * @param geometry For vector output formats, specify how to return the geometry and/or what the features of the
     * response should represent. &#x60;vectorized&#x60;: return features with regular non-rasterized, non-quantized
     * geometry &#x60;zone-centroid&#x60;: rasterize to zone features and use a Point geometry representing that zone
     * centroid &#x60;zone-region&#x60;: rasterize to zone features and use a (Multi)Polygon/Polyhedron geometry
     * representing that zone&#39;s region -- not supported for DGGS-JSON-FG profiles
     * (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) &#x60;none&#x60;: (for zone listing) omit zone geometry -- not supported
     * for DGGS-JSON-FG profiles (&#x60;profile&#x3D;jsonfg-dggs*&#x60;) (optional)
     * @param profile Allows negotiating a particular profile of an output format, such as OGC Feature &amp; Geometry
     * JSON (JSON-FG) or DGGS-JSON-FG output when requesting an &#x60;application/geo+json&#x60; media type for zone
     * data or zone list requests. For both zone data and zone lists in GeoJSON (&#x60;application/geo+json&#x60;):
     * &#x60;rfc7946&#x60;: return standard GeoJSON without using any extension &#x60;jsonfg&#x60;: return JSON-FG
     * representation &#x60;jsonfg-plus&#x60;: return JSON-FG representation with GeoJSON compatibility For zone data in
     * GeoJSON (&#x60;application/geo+json&#x60;): &#x60;jsonfg-dggs&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points quantized to sub-zone, represented as local indices from 1 to the
     * number of sub-zones corresponding to the DGGRS deterministic sub-zone order, with a special value of 0
     * representing an artificial node &#x60;jsonfg-dggs-plus&#x60;: return DGGS-JSON-FG representation, with GeoJSON
     * compatibility &#x60;geometry&#x60; &#x60;jsonfg-dggs-zoneids&#x60;: return DGGS-JSON-FG representation, using
     * &#x60;dggsPlace&#x60; to encode geometry points as textual global zone identifiers, with a special value of
     * _null_ representing an artificial node &#x60;jsonfg-dggs-zoneids-plus&#x60;: return DGGS-JSON-FG representation,
     * encoding geometry points as global zone IDs, with GeoJSON compatibility &#x60;geometry&#x60; For zone data in
     * netCDF (&#x60;application/x-netcdf&#x60;): &#x60;netcdf3&#x60;: return NetCDF classic and 64-bit offset format
     * (not quantized to DGGH) &#x60;netcdf3-dggs&#x60;: return NetCDF classic and 64-bit offset format where one axis
     * corresponds to local sub-zone indices &#x60;netcdf3-dggs-zoneids&#x60;: return NetCDF classic and 64-bit offset
     * format where one axis corresponds to the global identifiers of sub-zones (textual or 64-bit integer)
     * &#x60;netcdf4&#x60;: return HFG5-based NetCDF 4 format (not quantized to DGGH) &#x60;netcdf4-dggs&#x60;: return
     * HDF5-based NetCDF 4 format where one axis corresponds to local sub-zone indices &#x60;netcdf4-dggs-zoneids&#x60;:
     * return HDF5-based NetCDF 4 format where one axis corresponds to the global identifiers of sub-zones (textual or
     * 64-bit integer) For zone data in zipped Zarr 2.0 (&#x60;application/zarr+zip&#x60;): &#x60;zarr2&#x60;: return
     * zipped Zarr 2.0 (not quantized to DGGH) &#x60;zarr2-dggs&#x60;: return zipped Zarr 2.0 where one axis corresponds
     * to local sub-zone indices &#x60;zarr2-dggs-zoneids&#x60;: return zipped Zarr 2.0 where one axis corresponds to
     * the global identifiers of sub-zones (textual or 64-bit integer) For zone data in CoverageJSON
     * (&#x60;application/prs.coverage+json&#x60;): &#x60;covjson&#x60;: return CoverageJSON (not quantized to DGGH)
     * &#x60;covjson-dggs&#x60;: return CoverageJSON where one axis corresponds to local sub-zone indices
     * &#x60;covjson-dggs-zoneids&#x60;: return CoverageJSON where one axis corresponds to the global identifiers of
     * sub-zones (textual or 64-bit integer) (optional)
     * @param datetime Either a date-time or an interval. Date and time expressions adhere to RFC 3339, section 5.6.
     * Intervals may be bounded or half-bounded (double-dots at start or end). Server implementations may or may not
     * support times expressed using time offsets from UTC, but need to support UTC time with the notation ending with a
     * Z. Examples: * A date-time: \&quot;2018-02-12T23:20:50Z\&quot; * A bounded interval:
     * \&quot;2018-02-12T00:00:00Z/2018-03-18T12:31:12Z\&quot; * Half-bounded intervals:
     * \&quot;2018-02-12T00:00:00Z/..\&quot; or \&quot;../2018-03-18T12:31:12Z\&quot; Only resources that have a
     * temporal property that intersects the value of &#x60;datetime&#x60; are selected. If a feature has multiple
     * temporal properties, it is the decision of the server whether only a single temporal property is used to
     * determine the extent or all relevant temporal properties. (optional)
     * @param zoneDepth The DGGS resolution levels beyond the requested DGGS zone’s hierarchy level to include in the
     * response, when retrieving data for that zone. This can be either: • A single positive integer value —
     * representing a specific zone depth to return (e.g., &#x60;zone-depth&#x3D;5&#x60;); • A range of positive integer
     * values in the form “{low}-{high}” — representing a continuous range of zone depths to return (e.g.,
     * &#x60;zone-depth&#x3D;1-8&#x60;); or, • A comma separated list of at least two (2) positive integer values —
     * representing a set of specific zone depths to return (e.g., &#x60;zone-depth&#x3D;1,3,7&#x60;). Some or all of
     * these forms of the zone-depth parameter may not be supported with particular data packet encodings (the data
     * encoding may support a fixed depth, a range of depths, and/or an arbitrary selection of depths). When this
     * parameter is omitted, the default value specified in the &#x60;defaultDepth&#x60; property of the
     * &#x60;.../dggs/{dggrsId}&#x60; DGGRS description is used. (optional)
     * @param valuesOffset Specify the offset for a zone data output format such as PNG not supporting floating-point,
     * to be applied after multiplying by the scale factor and resulting in the encoded integer values (e.g., 8-bit or
     * 16-bit unsigned for PNG). (optional)
     * @param valuesScale Specify the scale factor for a zone data output format such as PNG not supporting
     * floating-point, to be applied before adding an offset and resulting in the encoded integer values (e.g., 8-bit or
     * 16-bit unsigned for PNG). (optional)
     * @return DGGRS zone data returned as a response. (status code 200) or No data available for this zone. (status
     * code 204) or The requested resource does not exist on the server. For example, a path parameter had an incorrect
     * value. (status code 404) or Content negotiation failed. For example, the &#x60;Accept&#x60; header submitted in
     * the request did not support any of the media types supported by the server for the requested resource. (status
     * code 406) or A server error occurred. (status code 500)
     */
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/dggs/{dggrsId}/zones/{zoneId}/data",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE, "image/png", "image/tiff; application=geotiff", "application/geo+json", MediaTypes.DATA_UBJSON}
    )
    public ResponseEntity<DggrsData> getDggrsZoneData(
            @PathVariable("serviceId") String serviceId,
            @PathVariable("dggrsId") String dggrsId,
            @PathVariable("zoneId") String zoneId,
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "collections", required = false) List<String> collections,
            @RequestParam(value = "f", required = false) String f,
            @RequestParam(value = "properties", required = false) String properties,
            @RequestParam(value = "exclude-properties", required = false) String excludeProperties,
            @RequestParam(value = "subset", required = false) List<String> subset,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "crs", required = false) String crs,
            @RequestParam(value = "geometry", required = false) String geometry,
            @RequestParam(value = "profile", required = false) String profile,
            @RequestParam(value = "datetime", required = false) String datetime,
            @RequestParam(value = "zone-depth", required = false) String zoneDepth,
            @RequestParam(value = "values-offset", required = false) Double valuesOffset,
            @RequestParam(value = "values-scale", required = false) Double valuesScale) {
        putServiceIdParam(serviceId);
        try {
            f = defaultFormat(f, headers);
        } catch (BadParameterException ex) {
            return ex.toResponse().getResponseEntity();
        }

        return treatIncomingRequest(new GetZoneData()
                .collectionId(collections)
                .dggrsId(dggrsId)
                .zoneId(zoneId)
                .format(f)
                .properties(properties)
                .excludeProperties(excludeProperties)
                .subset(subset)
                .datetime(datetime)
                .filter(filter)
                .crs(crs)
                .geometry(geometry)
                .profile(profile)
                .zoneDepth(zoneDepth)
                .valuesOffset(valuesOffset)
                .valuesScale(valuesScale)
                .gzip(askingGzip(headers))).getResponseEntity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        LOGGER.log(Level.WARNING, exc.getMessage(), exc);
        final CstlServiceException ex = CstlServiceException.castOrWrap(exc);
        return new ResponseObject(new ExceptionReport(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode());
    }

    private static GeneralEnvelope parseBbox(List<Double> coordinates, String bboxCrs) throws BadParameterException {
        if(coordinates == null || coordinates.isEmpty()) return null;

        final CoordinateReferenceSystem crs;
        if (bboxCrs != null) {
            crs = parseCrs(bboxCrs);
        } else {
            if (coordinates.size() == 4) {
                crs = CommonCRS.WGS84.normalizedGeographic();
            } else if (coordinates.size() == 6) {
                try {
                    crs = CRS.compound(CommonCRS.WGS84.normalizedGeographic(), CommonCRS.Vertical.ELLIPSOIDAL.crs());
                } catch (FactoryException ex) {
                    throw new BadParameterException("Failed to create CRS:84+H");
                }
            } else {
                throw new BadParameterException("bbox");
            }
        }

        GeneralEnvelope env = null;
        if (coordinates.size() == 4) {
            env = new GeneralEnvelope(crs);
            env.setRange(0, coordinates.get(0), coordinates.get(2));
            env.setRange(1, coordinates.get(1), coordinates.get(3));
        } else if (coordinates.size() == 6) {
            env = new GeneralEnvelope(crs);
            env.setRange(0, coordinates.get(0), coordinates.get(3));
            env.setRange(1, coordinates.get(1), coordinates.get(4));
            env.setRange(2, coordinates.get(2), coordinates.get(5));
        } else {
            throw new BadParameterException("bbox", bboxCrs, coordinates);
        }
        return env;
    }

    public static CoordinateReferenceSystem parseCrs(String crs) throws BadParameterException {
        if (crs == null || crs.isBlank()) return null;
        try {
            return CRS.forCode(crs);
        } catch (FactoryException ex) {
            throw new BadParameterException("crs", crs);
        }
    }

    public static final class BadParameterException extends CstlServiceException {

        public final String parameterName;
        public final Object[] parameterValues;

        public BadParameterException(String parameterName, Object ... parameterValues) {
            super("Bad request parameter : " + parameterName + " reason/value(s) : " + Arrays.toString(parameterValues));
            this.parameterName = parameterName;
            this.parameterValues = parameterValues;
        }

        public ResponseObject toResponse() {
            return new ResponseObject(getMessage(), MediaType.TEXT_PLAIN, HttpStatus.BAD_REQUEST);
        }

    }
}
