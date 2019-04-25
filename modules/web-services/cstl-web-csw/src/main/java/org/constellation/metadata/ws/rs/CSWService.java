/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.metadata.ws.rs;

import org.apache.sis.internal.xml.LegacyNamespaces;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.jaxb.CstlXMLSerializer;
import org.constellation.metadata.core.CSWworker;
import org.constellation.metadata.utils.CSWUtils;
import org.constellation.metadata.utils.SerializerResponse;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.constellation.ws.UnauthorizedException;
import org.constellation.ws.WebServiceUtilities;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;
import org.geotoolkit.csw.xml.CSWResponse;
import org.geotoolkit.csw.xml.CswXmlFactory;
import org.geotoolkit.csw.xml.DescribeRecord;
import org.geotoolkit.csw.xml.DistributedSearch;
import org.geotoolkit.csw.xml.ElementSetName;
import org.geotoolkit.csw.xml.ElementSetType;
import org.geotoolkit.csw.xml.GetCapabilities;
import org.geotoolkit.csw.xml.GetDomain;
import org.geotoolkit.csw.xml.GetRecordById;
import org.geotoolkit.csw.xml.GetRecordsRequest;
import org.geotoolkit.csw.xml.Harvest;
import org.geotoolkit.csw.xml.Query;
import org.geotoolkit.csw.xml.QueryConstraint;
import org.geotoolkit.csw.xml.ResultType;
import org.geotoolkit.csw.xml.Transaction;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.ows.xml.AcceptFormats;
import org.geotoolkit.ows.xml.AcceptVersions;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.ows.xml.Sections;
import org.geotoolkit.ows.xml.v100.SectionsType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;

import static org.constellation.api.QueryConstants.ACCEPT_FORMATS_PARAMETER;
import static org.constellation.api.QueryConstants.ACCEPT_VERSIONS_PARAMETER;
import static org.constellation.api.QueryConstants.SECTIONS_PARAMETER;
import static org.constellation.api.QueryConstants.SERVICE_PARAMETER;
import static org.constellation.api.QueryConstants.UPDATESEQUENCE_PARAMETER;
import static org.constellation.api.QueryConstants.VERSION_PARAMETER;
import org.constellation.metadata.core.CSWConstants;
import static org.constellation.metadata.core.CSWConstants.CONSTRAINT;
import static org.constellation.metadata.core.CSWConstants.CONSTRAINT_LANGUAGE;
import static org.constellation.metadata.core.CSWConstants.CONSTRAINT_LANGUAGE_VERSION;
import static org.constellation.metadata.core.CSWConstants.MALFORMED;
import static org.constellation.metadata.core.CSWConstants.MAX_RECORDS;
import static org.constellation.metadata.core.CSWConstants.NAMESPACE;
import static org.constellation.metadata.core.CSWConstants.NOT_EXIST;
import static org.constellation.metadata.core.CSWConstants.OUTPUT_FORMAT;
import static org.constellation.metadata.core.CSWConstants.OUTPUT_SCHEMA;
import static org.constellation.metadata.core.CSWConstants.REQUEST_ID;
import static org.constellation.metadata.core.CSWConstants.RESULT_TYPE;
import static org.constellation.metadata.core.CSWConstants.START_POSITION;
import static org.constellation.metadata.core.CSWConstants.TYPENAMES;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.gml.xml.Point;
import org.geotoolkit.ogc.xml.FilterXmlFactory;
import org.geotoolkit.ogc.xml.SortBy;
import org.geotoolkit.ops.xml.v110.OpenSearchDescription;
import org.geotoolkit.ows.xml.ExceptionResponse;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.sort.SortOrder;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import org.w3._2005.atom.FeedType;


/**
 * RestFul CSW service.
 *
 * @author Guilhem Legal
 * @author Benjamin Garcia (Geomatys)
 *
 * @version 0.9
 */
@Controller
@RequestMapping("csw/{serviceId:.+}")
public class CSWService extends OGCWebService<CSWworker> {

    /**
     * Build a new Restful CSW service.
     */
    public CSWService() {
        super(Specification.CSW);
        setXMLContext(EBRIMMarshallerPool.getInstance());
        LOGGER.log(Level.INFO, "CSW REST service running");
    }

    /**
     * This method has to be overridden by child classes.
     */
    protected CstlXMLSerializer getXMLSerializer() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject treatIncomingRequest(final Object objectRequest, final CSWworker worker) {
        ServiceDef serviceDef = null;

        try {

            // if the request is not an xml request we fill the request parameter.
            final RequestBase request;
            if (objectRequest == null) {
                request = adaptQuery(getParameter("REQUEST", true), worker);
            } else if (objectRequest instanceof RequestBase) {
                request = (RequestBase) objectRequest;
            } else {
                throw new CstlServiceException("The operation " +  objectRequest.getClass().getName() + " is not supported by the service",
                    INVALID_PARAMETER_VALUE, "request");
            }

            serviceDef = worker.getVersionFromNumber(request.getVersion());

            if (request instanceof GetCapabilities) {
                final GetCapabilities gc = (GetCapabilities) request;
                final MediaType outputFormat  = MediaType.APPLICATION_XML; // TODO
                return new ResponseObject(worker.getCapabilities(gc), outputFormat);
            }

            if (request instanceof GetRecordsRequest) {

                final GetRecordsRequest gr = (GetRecordsRequest)request;
                final String outputFormat  = CSWUtils.getOutputFormat(gr);
                // we pass the serializer to the messageBodyWriter
                final SerializerResponse response = new SerializerResponse((CSWResponse) worker.getRecords(gr), getXMLSerializer());
                return new ResponseObject(response, outputFormat);
            }

            if (request instanceof GetRecordById) {

                final GetRecordById grbi = (GetRecordById)request;
                final String outputFormat  = CSWUtils.getOutputFormat(grbi);
                // we pass the serializer to the messageBodyWriter
                final SerializerResponse response = new SerializerResponse((CSWResponse) worker.getRecordById(grbi), getXMLSerializer());
                return new ResponseObject(response, outputFormat);
            }

            if (request instanceof DescribeRecord) {

                final DescribeRecord dr = (DescribeRecord)request;
                final String outputFormat  = CSWUtils.getOutputFormat(dr);
                return new ResponseObject(worker.describeRecord(dr), outputFormat);
            }

            if (request instanceof GetDomain) {
                final GetDomain gd = (GetDomain)request;
                final String outputFormat  = CSWUtils.getOutputFormat(gd);
                return new ResponseObject(worker.getDomain(gd), outputFormat);
            }

            if (request instanceof Transaction) {
                final Transaction tr = (Transaction)request;
                final String outputFormat  = CSWUtils.getOutputFormat(tr);
                return new ResponseObject(worker.transaction(tr), outputFormat);
            }

            if (request instanceof Harvest) {
                final Harvest hv = (Harvest)request;
                final String outputFormat  = CSWUtils.getOutputFormat(hv);
                return new ResponseObject(worker.harvest(hv), outputFormat);
            }

            throw new CstlServiceException("The operation " +  request.getClass().getName() + " is not supported by the service",
                    INVALID_PARAMETER_VALUE, "request");

        } catch (CstlServiceException ex) {
            return processExceptionResponse(ex, serviceDef, worker);

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject processExceptionResponse(final CstlServiceException ex, ServiceDef serviceDef, final Worker w) {
        // asking for authentication
        if (ex instanceof UnauthorizedException) {
            Map<String, String> headers = new HashMap<>();
            headers.put("WWW-Authenticate", " Basic");
            return new ResponseObject(HttpStatus.UNAUTHORIZED, headers);
        }
        logException(ex);
        if (serviceDef == null) {
            serviceDef = w.getBestVersion(null);
        }
        final String version           = serviceDef.exceptionVersion.toString();
        final String code              = getOWSExceptionCodeRepresentation(ex.getExceptionCode());
        final ExceptionResponse report = CswXmlFactory.buildExceptionReport(serviceDef.version.toString(), ex.getMessage(), code, ex.getLocator(), version);
        return new ResponseObject(report, MediaType.TEXT_XML);
    }


    /**
     * Build request object from KVP parameters.
     */
    private RequestBase adaptQuery(final String request, final Worker w) throws CstlServiceException {

        if ("GetCapabilities".equalsIgnoreCase(request)) {
            return createNewGetCapabilitiesRequest(w);
        } else if ("GetRecords".equalsIgnoreCase(request)) {
            return createNewGetRecordsRequest();
        } else if ("GetRecordById".equalsIgnoreCase(request)) {
            return createNewGetRecordByIdRequest();
        } else if ("DescribeRecord".equalsIgnoreCase(request)) {
            return createNewDescribeRecordRequest();
        } else if ("GetDomain".equalsIgnoreCase(request)) {
            return createNewGetDomainRequest();
        } else if ("Transaction".equalsIgnoreCase(request)) {
            throw new CstlServiceException("The Operation transaction is not available in KVP", OPERATION_NOT_SUPPORTED, "transaction");
        } else if ("Harvest".equalsIgnoreCase(request)) {
            return createNewHarvestRequest();
        }
        throw new CstlServiceException("The operation " + request + " is not supported by the service",
                        INVALID_PARAMETER_VALUE, "request");
    }

    /**
     * Build a new GetCapabilities request object with the url parameters
     */
    private GetCapabilities createNewGetCapabilitiesRequest(final Worker w) throws CstlServiceException {

        final String service = getParameter(SERVICE_PARAMETER, true);

        String acceptVersion = getParameter(ACCEPT_VERSIONS_PARAMETER, false);
        String currentVersion = getParameter(VERSION_PARAMETER, false);
        if (currentVersion == null) {
            currentVersion = w.getBestVersion(null).version.toString();
        }
        w.checkVersionSupported(currentVersion, true);

        final AcceptVersions versions;
        if (acceptVersion != null) {
            if (acceptVersion.indexOf(',') != -1) {
                acceptVersion = acceptVersion.substring(0, acceptVersion.indexOf(','));
            }
            currentVersion = acceptVersion;
            List<String> v = new ArrayList<>();
            v.add(acceptVersion);
            versions = CswXmlFactory.buildAcceptVersion(currentVersion, v);
        } else {
            List<String> v = new ArrayList<>();
            v.add(currentVersion);
            versions = CswXmlFactory.buildAcceptVersion(currentVersion, v);
        }

        final AcceptFormats formats = CswXmlFactory.buildAcceptFormat(currentVersion, Arrays.asList(getParameter(ACCEPT_FORMATS_PARAMETER, false)));
        final String updateSequence = getParameter(UPDATESEQUENCE_PARAMETER, false);

        //We transform the String of sections in a list.
        //In the same time we verify that the requested sections are valid.
        final String section = getParameter(SECTIONS_PARAMETER, false);
        List<String> requestedSections = new ArrayList<>();
        if (section != null && !section.equalsIgnoreCase("All")) {
            final StringTokenizer tokens = new StringTokenizer(section, ",;");
            while (tokens.hasMoreTokens()) {
                final String token = tokens.nextToken().trim();
                if (SectionsType.getExistingSections().contains(token)){
                    requestedSections.add(token);
                } else {
                    throw new CstlServiceException("The section " + token + NOT_EXIST,
                                                  INVALID_PARAMETER_VALUE, "Sections");
                }
            }
        } else {
            //if there is no requested Sections we add all the sections
            requestedSections = SectionsType.getExistingSections();
        }
        final Sections sections = CswXmlFactory.buildSections(currentVersion, requestedSections);
        return CswXmlFactory.createGetCapabilities(currentVersion, versions, sections, formats, updateSequence, service);
    }


    /**
     * Build a new GetRecords request object with the url parameters
     */
    private GetRecordsRequest createNewGetRecordsRequest() throws CstlServiceException {

        final String version    = getParameter(VERSION_PARAMETER, true);
        final String service    = getParameter(SERVICE_PARAMETER, true);

        //we get the value of result type, if not set we put default value "HITS"
        final String resultTypeName = getParameter(RESULT_TYPE, false);
        ResultType resultType = ResultType.HITS;
        if (resultTypeName != null) {
            try {
                resultType = ResultType.fromValue(resultTypeName);
            } catch (IllegalArgumentException e){
               throw new CstlServiceException("The resultType " + resultTypeName + NOT_EXIST,
                                             INVALID_PARAMETER_VALUE, "ResultType");
            }
        }

        final String requestID    = getParameter(REQUEST_ID, false);

        String outputFormat = getParameter(OUTPUT_FORMAT, false);
        if (outputFormat == null) {
            outputFormat = MimeType.APPLICATION_XML;
        }

        String outputSchema = getParameter(OUTPUT_SCHEMA, false);
        if (outputSchema == null) {
            outputSchema = LegacyNamespaces.CSW;
        }

        //we get the value of start position, if not set we put default value "1"
        final String startPos = getParameter(START_POSITION, false);
        Integer startPosition = Integer.valueOf("1");
        if (startPos != null) {
            try {
                startPosition = Integer.valueOf(startPos);
            } catch (NumberFormatException e){
               throw new CstlServiceException("The positif integer " + startPos + MALFORMED,
                                             INVALID_PARAMETER_VALUE, "startPosition");
            }
        }

        //we get the value of max record, if not set we put default value "10"
        final String maxRec = getParameter(MAX_RECORDS, false);
        Integer maxRecords= Integer.valueOf("10");
        if (maxRec != null) {
            try {
                maxRecords = Integer.valueOf(maxRec);
            } catch (NumberFormatException e){
               throw new CstlServiceException("The positif integer " + maxRec + MALFORMED,
                                             INVALID_PARAMETER_VALUE, "maxRecords");
            }
        }

        /*
         * here we build the "Query" object
         */

        // we get the namespaces.
        final String namespace               = getParameter("NAMESPACE", false);
        final Map<String, String> namespaces = WebServiceUtilities.extractNamespace(namespace);

        //if there is not namespace specified, using the default namespace
        if (namespaces.isEmpty()) {
            namespaces.put("csw", LegacyNamespaces.CSW);
            namespaces.put("gmd", LegacyNamespaces.GMD);
        }

        final String names          = getParameter(TYPENAMES, true);
        final List<QName> typeNames = new ArrayList<>();
        StringTokenizer tokens = new StringTokenizer(names, ",;");
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken().trim();

            if (token.indexOf(':') != -1) {
                final String prefix = token.substring(0, token.indexOf(':'));
                final String localPart = token.substring(token.indexOf(':') + 1);
                typeNames.add(new QName(namespaces.get(prefix), localPart, prefix));
            } else {
                throw new CstlServiceException("The QName " + token + MALFORMED,
                        INVALID_PARAMETER_VALUE, NAMESPACE);
            }
        }

        final String eSetName     = getParameter("ELEMENTSETNAME", false);
        ElementSetType elementSet = ElementSetType.SUMMARY;
        if (eSetName != null) {
            try {
                elementSet = ElementSetType.fromValue(eSetName);

            } catch (IllegalArgumentException e){
               throw new CstlServiceException("The ElementSet Name " + eSetName + NOT_EXIST,
                                                INVALID_PARAMETER_VALUE, "ElementSetName");
            }
        }
        final ElementSetName setName = CswXmlFactory.createElementSetName(version, elementSet);

        //we get the list of sort by object
        SortBy sortBy = getSortFromKvp(version);

        /*
         * here we build the constraint object
         */
        final String constLanguage = getParameter(CONSTRAINT_LANGUAGE, false);
        QueryConstraint constraint = null;
        if (constLanguage != null) {
            final String languageVersion  = getParameter(CONSTRAINT_LANGUAGE_VERSION, true);

            if (constLanguage.equalsIgnoreCase("CQL_TEXT")) {

                String constraintObject = getParameter(CONSTRAINT, false);
                if (constraintObject == null) {
                    constraintObject = "AnyText LIKE '%%'";
                }
                constraint = CswXmlFactory.createQueryConstraint(version, constraintObject, languageVersion);

            } else if (constLanguage.equalsIgnoreCase("FILTER")) {
                final Object constraintObject = getComplexParameter(CONSTRAINT, false);
                if (constraintObject == null) {
                    // do nothing
                } else if (constraintObject instanceof Filter){
                    constraint = CswXmlFactory.createQueryConstraint(version, (Filter)constraintObject, languageVersion);
                } else {
                    throw new CstlServiceException("The filter type is not supported:" + constraintObject.getClass().getName(),
                                                 INVALID_PARAMETER_VALUE, "Constraint");
                }

            } else {
                throw new CstlServiceException("The constraint language " + constLanguage + " is not supported",
                                                 INVALID_PARAMETER_VALUE, "ConstraintLanguage");
            }
        } else {
            // kvp opensearch like query
            constraint = getConstraintFromKvp(version);
        }


        final Query query = CswXmlFactory.createQuery(version, typeNames, setName, sortBy, constraint);

        /*
         * here we build a optionnal ditributed search object
         */
        final String distrib = getParameter("DISTRIBUTEDSEARCH", false);
        DistributedSearch distribSearch = null;
        if (distrib != null && distrib.equalsIgnoreCase("true")) {
            final String count = getParameter("HOPCOUNT", false);
            Integer hopCount   = 2;
            if (count != null) {
                try {
                    hopCount = Integer.parseInt(count);
                } catch (NumberFormatException e){
                    throw new CstlServiceException("The positif integer " + count + MALFORMED,
                                                  INVALID_PARAMETER_VALUE, "HopCount");
                }
            }
            distribSearch = CswXmlFactory.createDistributedSearch(version, hopCount);
        }

        // TODO not implemented yet
        // String handler = getParameter("RESPONSEHANDLER", false);

        return CswXmlFactory.createGetRecord(version, service, resultType, requestID, outputFormat, outputSchema, startPosition, maxRecords, query, distribSearch);
    }

    private SortBy getSortFromKvp(String version) throws CstlServiceException {
        final String filterVersion = CswXmlFactory.getFilterVersion(version);
        final String sort          = getParameter("SORTBY", false);

        if (sort != null) {
            final List<org.opengis.filter.sort.SortBy> sorts = new ArrayList<>();
            StringTokenizer tokens = new StringTokenizer(sort, ",;");
            while (tokens.hasMoreTokens()) {
                final String token = tokens.nextToken().trim();

                if (token.indexOf(':') != -1) {
                    final String propName    = token.substring(0, token.indexOf(':'));
                    final String order       = token.substring(token.indexOf(':') + 1);
                    SortOrder orderType;
                    try {
                        orderType = SortOrder.valueOf(order);
                    } catch (IllegalArgumentException e){
                        throw new CstlServiceException("The SortOrder Name " + order + NOT_EXIST,
                                                      INVALID_PARAMETER_VALUE, "SortBy");
                    }
                    sorts.add(FilterXmlFactory.buildSortProperty(filterVersion, propName, orderType));
                } else {
                     throw new CstlServiceException("The expression " + token + MALFORMED,
                                                      INVALID_PARAMETER_VALUE, "SortBy");
                }
            }
            return FilterXmlFactory.buildSortBy(filterVersion, sorts);
        }
        return null;
    }

    private GetRecordsRequest createNewOpenSearchGetRecordsRequest() throws CstlServiceException {

        final String version    = getParameter(VERSION_PARAMETER, true);
        final String service    = getParameter(SERVICE_PARAMETER, true);

        String outputFormat = getParameter(OUTPUT_FORMAT, false);
        if (outputFormat == null) {
            outputFormat = MimeType.APPLICATION_XML;
        }

        String outputSchema = getParameter(OUTPUT_SCHEMA, false);
        if (outputSchema == null) {
            outputSchema = LegacyNamespaces.CSW;
        }

        //we get the value of start position, if not set we put default value "1"
        final String startPos = getParameter(START_POSITION, false);
        Integer startPosition = Integer.valueOf("1");
        if (startPos != null) {
            try {
                startPosition = Integer.valueOf(startPos);
            } catch (NumberFormatException e){
               throw new CstlServiceException("The positif integer " + startPos + MALFORMED,
                                             INVALID_PARAMETER_VALUE, "startPosition");
            }
        }

        //we get the value of max record, if not set we put default value "10"
        final String maxRec = getParameter(MAX_RECORDS, false);
        Integer maxRecords= Integer.valueOf("10");
        if (maxRec != null) {
            try {
                maxRecords = Integer.valueOf(maxRec);
            } catch (NumberFormatException e){
               throw new CstlServiceException("The positif integer " + maxRec + MALFORMED,
                                             INVALID_PARAMETER_VALUE, "maxRecords");
            }
        }

        /**
         * add sort parameter even if its not on the spec, for test purpose
         */
        SortBy sortBy = getSortFromKvp(version);

        /*
         * here we build the "Query" object
         */
        final List<QName> typeNames = Arrays.asList(new QName(LegacyNamespaces.CSW, "Record"));
        final ElementSetName setName = CswXmlFactory.createElementSetName(version, ElementSetType.FULL);

        /*
         * here we build the constraint object FROM kvp opensearch like query
         */
        final QueryConstraint constraint = getConstraintFromKvp(version);
        final Query query = CswXmlFactory.createQuery(version, typeNames, setName, sortBy, constraint);
        return CswXmlFactory.createGetRecord(version, service, ResultType.RESULTS, null, outputFormat, outputSchema, startPosition, maxRecords, query, null);
    }

    private QueryConstraint getConstraintFromKvp(String version) throws CstlServiceException {
        final String filterVersion = CswXmlFactory.getFilterVersion(version);
        final String gmlVersion    = CswXmlFactory.getGmlVersion(version);
        final List<Filter> filters = new ArrayList<>();

        final String searchTerms = getParameter("q", false);
        final String bbox        = getParameter("bbox", false);

        final String ids         = getParameter("recordIds", false);

        final String geometry    = getParameter("geometry", false);
        final String geometryCRS = getParameter("geometry_crs", false);
        String spRelation        = getParameter("relation", false);
        final String distance    = getParameter("distance", false);
        String distanceUOM       = getParameter("distance_uom", false);
        final String geoName     = getParameter("name", false);

        final String lat         = getParameter("lat", false);
        final String lon         = getParameter("lon", false);
        final String radius      = getParameter("radius", false);

        final String time        = getParameter("time", false);
        String trelation         = getParameter("trelation", false);

        if (searchTerms != null && !searchTerms.isEmpty()) {
            filters.add(CSWUtils.BuildSearchTermsFilter(filterVersion, searchTerms));
        }

        if (ids != null && !ids.isEmpty()) {
            String[] identifiers = ids.split(",");
            final List<Filter> idFilters = new ArrayList<>();
            for (String identifier : identifiers) {
                Literal lit = FilterXmlFactory.buildLiteral(filterVersion, identifier);
                idFilters.add(FilterXmlFactory.buildPropertyIsEquals(filterVersion, "dc:identifier", lit, true));
            }
            if (idFilters.size() == 1) {
                filters.add(idFilters.get(0));
            } else {
                filters.add(FilterXmlFactory.buildOr(filterVersion, idFilters.toArray(new Object[idFilters.size()])));
            }
        }

        if (geoName != null) {
            Literal lit = FilterXmlFactory.buildLiteral(filterVersion, geoName);
            filters.add(FilterXmlFactory.buildPropertyIsEquals(filterVersion, "GeographicDescriptionCode", lit, true));
        }

        if (bbox != null) {
            String[] parts = bbox.split(",");
            if (parts.length == 5) {
                try {
                    double minx = Double.parseDouble(parts[0]);
                    double miny = Double.parseDouble(parts[1]);
                    double maxx = Double.parseDouble(parts[2]);
                    double maxy = Double.parseDouble(parts[3]);
                    String crs =  parts[4];
                    filters.add(FilterXmlFactory.buildBBOX(filterVersion, "ows:BoundingBox",minx, miny, maxx, maxy, crs));
                } catch (NumberFormatException ex) {
                    throw new CstlServiceException("Bbox parameter must be of the form minx<Number>,miny<Number>,maxx<Number>,maxy<Number>,crs<String>:" + ex.getMessage(), INVALID_PARAMETER_VALUE, "bbox");
                }
            } else {
                throw new CstlServiceException("Bbox parameter must be of the form minx,miny,maxx,maxy,crs.", INVALID_PARAMETER_VALUE, "bbox");
            }
        }
        if (geometry != null) {
            if (spRelation == null) {
                spRelation = "Intersects";
            }
            WKTReader reader = new WKTReader();
            try {
                Geometry jtsGeom = reader.read(geometry);
                CoordinateReferenceSystem crs;
                if (geometryCRS != null) {
                    crs = CRS.forCode(geometryCRS);
                } else {
                    crs = CommonCRS.WGS84.geographic();
                }
                AbstractGeometry geom = JTStoGeometry.toGML(gmlVersion, jtsGeom, crs);
                if (distance == null) {
                    filters.add(FilterXmlFactory.buildBinarySpatial(filterVersion, spRelation, "ows:BoundingBox", geom));
                } else {
                    double dist = Double.parseDouble(distance);
                    if (distanceUOM == null) {
                        distanceUOM = "m";
                    }
                    filters.add(FilterXmlFactory.buildDistanceSpatialFilter(filterVersion, spRelation, "ows:BoundingBox", geom, dist, distanceUOM));
                }

            } catch (ParseException | FactoryException ex) {
                throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, "geometry");
            }
        }

        if (lat != null && lon != null && radius != null) {
            try {
                String relation = "DWithin";
                double latD     = Double.parseDouble(lat);
                double lonD     = Double.parseDouble(lon);
                double radiusD  = Double.parseDouble(radius);
                DirectPosition pos = GMLXmlFactory.buildDirectPosition(gmlVersion, "urn:x-ogc:def:crs:EPSG:6.11:4326", 2, Arrays.asList(latD,lonD));
                Point pt = GMLXmlFactory.buildPoint(gmlVersion, "pt-1",  "urn:x-ogc:def:crs:EPSG:6.11:4326", pos);
                filters.add(FilterXmlFactory.buildDistanceSpatialFilter(filterVersion, relation, "ows:BoundingBox", pt, radiusD, "meters"));
            } catch (NumberFormatException ex) {
                throw new CstlServiceException("Proximity parameters must be Numbers (lat,lon,radius):" + ex.getMessage(), INVALID_PARAMETER_VALUE, "bbox");
            }

        } else if (lat != null || lon != null || radius != null) {
            throw new CstlServiceException("The 3 parameters must be definied lat, lon, radius for a distance filter.", INVALID_PARAMETER_VALUE);
        }

        if (time != null) {
            int separator = time.indexOf("/");
            if (separator != -1) {
                if (trelation == null) {
                    trelation = "AnyInteracts";
                }
                String start = time.substring(0, separator - 1);
                String end   = time.substring(separator + 1);
                //timeObj = GMLXmlFactory.createTimePeriod(gmlVersion, "p-1", start, end);

                Literal timeStart = FilterXmlFactory.buildLiteral(filterVersion, GMLXmlFactory.createTimeInstant(gmlVersion, "start-1", start));
                Literal timeEnd   = FilterXmlFactory.buildLiteral(filterVersion, GMLXmlFactory.createTimeInstant(gmlVersion, "end-1", end));

                filters.add(CSWUtils.BuildTemporalFilter(filterVersion, trelation, timeStart, timeEnd));

            } else {
                if (trelation == null) {
                    trelation = "TEquals";
                }
                Literal timeStart = FilterXmlFactory.buildLiteral(filterVersion, GMLXmlFactory.createTimeInstant(gmlVersion, "start-1", time));
                Literal timeEnd   = FilterXmlFactory.buildLiteral(filterVersion, GMLXmlFactory.createTimeInstant(gmlVersion, "end-1", time));

                filters.add(CSWUtils.BuildTemporalFilter(filterVersion, trelation, timeStart, timeEnd));
            }
        }

        if (!filters.isEmpty()) {
            Filter constraintObject;
            if (filters.size() == 1) {
                constraintObject = (Filter) FilterXmlFactory.buildFilter(filterVersion, filters.get(0));
            } else {
                constraintObject = (Filter) FilterXmlFactory.buildAnd(filterVersion, filters.toArray(new Object[filters.size()]));
            }
            return CswXmlFactory.createQueryConstraint(version, constraintObject, "1.1.0");
        }
        return null;
    }


    /**
     * Build a new GetRecordById request object with the url parameters
     */
    private GetRecordById createNewGetRecordByIdRequest() throws CstlServiceException {

        final String version    = getParameter(VERSION_PARAMETER, true);
        final String service    = getParameter(SERVICE_PARAMETER, true);

        String eSetName         = getParameter("ELEMENTSETNAME", false);
        ElementSetType elementSet = ElementSetType.SUMMARY;
        if (eSetName != null) {
            try {
                eSetName = eSetName.toLowerCase();
                elementSet = ElementSetType.fromValue(eSetName);

            } catch (IllegalArgumentException e){
               throw new CstlServiceException("The ElementSet Name " + eSetName + NOT_EXIST,
                                             INVALID_PARAMETER_VALUE, "ElementSetName");
            }
        }
        final ElementSetName setName = CswXmlFactory.createElementSetName(version, elementSet);

        String outputFormat = getParameter("OUTPUTFORMAT", false);
        if (outputFormat == null) {
            outputFormat = MimeType.APPLICATION_XML;
        }

        String outputSchema = getParameter("OUTPUTSCHEMA", false);
        if (outputSchema == null) {
            outputSchema = LegacyNamespaces.CSW;
        }

        final String ids             = getParameter("ID", true);
        final List<String> id        = new ArrayList<>();
        final StringTokenizer tokens = new StringTokenizer(ids, ",;");
        while (tokens.hasMoreTokens()) {
            final String token = tokens.nextToken().trim();
            id.add(token);
        }

        return CswXmlFactory.createGetRecordById(version, service, setName, outputFormat, outputSchema, id);
    }

    /**
     * Build a new DescribeRecord request object with the url parameters
     */
    private DescribeRecord createNewDescribeRecordRequest() throws CstlServiceException {

        final String version    = getParameter(VERSION_PARAMETER, true);
        final String service    = getParameter(SERVICE_PARAMETER, true);

        String outputFormat = getParameter("OUTPUTFORMAT", false);
        if (outputFormat == null) {
            outputFormat = MimeType.APPLICATION_XML;
        }

        String schemaLanguage = getParameter("SCHEMALANGUAGE", false);
        if (schemaLanguage == null) {
            schemaLanguage = "XMLSCHEMA";
        }

         // we get the namespaces.
        final String namespace               = getParameter("NAMESPACE", false);
        final Map<String, String> namespaces = WebServiceUtilities.extractNamespace(namespace);

        //if there is not namespace specified, using the default namespace
        // TODO add gmd...
        if (namespaces.isEmpty()) {
            namespaces.put("csw", LegacyNamespaces.CSW);
            namespaces.put("gmd", LegacyNamespaces.GMD);
        }

        final List<QName> typeNames  = new ArrayList<>();
        final String names           = getParameter("TYPENAME", false);
        if (names != null) {
            final StringTokenizer tokens = new StringTokenizer(names, ",;");
                while (tokens.hasMoreTokens()) {
                    final String token = tokens.nextToken().trim();

                    if (token.indexOf(':') != -1) {
                        final String prefix    = token.substring(0, token.indexOf(':'));
                        final String localPart = token.substring(token.indexOf(':') + 1);
                        typeNames.add(new QName(namespaces.get(prefix), localPart));
                    } else {
                         throw new CstlServiceException("The QName " + token + MALFORMED,
                                                       INVALID_PARAMETER_VALUE, NAMESPACE);
                    }
            }
        }

        return CswXmlFactory.createDescribeRecord(version, service, typeNames, outputFormat, schemaLanguage);
    }

    /**
     * Build a new GetDomain request object with the url parameters
     */
    private GetDomain createNewGetDomainRequest() throws CstlServiceException {

        final String version    = getParameter(VERSION_PARAMETER, true);
        final String service    = getParameter(SERVICE_PARAMETER, true);

        //not supported by the ISO profile
        final String parameterName = getParameter("PARAMETERNAME", false);

        final String propertyName = getParameter("PROPERTYNAME", false);
        if (propertyName != null && parameterName != null) {
            throw new CstlServiceException("One of propertyName or parameterName must be null",
                                          INVALID_PARAMETER_VALUE, "parameterName");
        }
        return CswXmlFactory.createGetDomain(version, service, propertyName, parameterName);
    }

    /**
     * Build a new GetDomain request object with the url parameters
     */
    private Harvest createNewHarvestRequest() throws CstlServiceException {

        final String version      = getParameter(VERSION_PARAMETER, true);
        final String service      = getParameter(SERVICE_PARAMETER, true);
        final String source       = getParameter("SOURCE", true);
        final String resourceType = getParameter("RESOURCETYPE", true);
        String resourceFormat     = getParameter("RESOURCEFORMAT", false);
        if (resourceFormat == null) {
            resourceFormat = MimeType.APPLICATION_XML;
        }
        final String handler      = getParameter("RESPONSEHANDLER", false);
        final String interval     = getParameter("HARVESTINTERVAL", false);
        Duration harvestInterval  = null;
        if (interval != null) {
            try {
                final DatatypeFactory factory = DatatypeFactory.newInstance();
                harvestInterval               = factory.newDuration(interval) ;
            } catch (DatatypeConfigurationException ex) {
                throw new CstlServiceException("The Duration " + interval + MALFORMED,
                                              INVALID_PARAMETER_VALUE, "HarvestInsterval");
            }
        }
        return CswXmlFactory.createHarvest(version, service, source, resourceType, resourceFormat, handler, harvestInterval);
    }

    @RequestMapping(path = "/descriptionDocument.xml", method = GET)
    public ResponseEntity getOpenSearchDescriptionDocument(@PathVariable("serviceId") String serviceId) {
        OpenSearchDescription description = CSWConstants.OS_DESCRIPTION;
        String cswUrl = getServiceURL() + "/csw/" + serviceId;
        CSWUtils.updateCswURL(description, cswUrl);
        return new ResponseObject(description, "application/opensearchdescription+xml").getResponseEntity();
    }

    @RequestMapping(path = "/opensearch", method = GET)
    public ResponseEntity openSearchRequest(@PathVariable("serviceId") String serviceId) {

        CSWworker worker = getWorker(serviceId);
        if (worker != null) {
            ServiceDef serviceDef = null;
            try {
                GetRecordsRequest request = createNewOpenSearchGetRecordsRequest();
                serviceDef = worker.getVersionFromNumber(request.getVersion());

                Object response = worker.getRecords(request);
                final String outputFormat  = CSWUtils.getOutputFormat(request);

                if (response instanceof FeedType) {
                    FeedType feed = (FeedType) response;
                    String selfRequest = getLogParameters();
                    CSWUtils.addNavigationRequest(request, feed, selfRequest);
                    // TODO add next / previous
                }

                return new ResponseObject(response, outputFormat).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, serviceDef, worker).getResponseEntity();
            }
        } else {
            LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
            return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
        }
    }
}
