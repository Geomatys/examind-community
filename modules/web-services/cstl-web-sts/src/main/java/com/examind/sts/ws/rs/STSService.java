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

package com.examind.sts.ws.rs;

// J2SE dependencies

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import static org.constellation.api.QueryConstants.REQUEST_PARAMETER;
import static org.constellation.api.QueryConstants.SECTIONS_PARAMETER;
import static org.constellation.api.QueryConstants.VERSION_PARAMETER;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.UnauthorizedException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ogc.xml.SortBy;
import org.geotoolkit.ows.xml.AcceptVersions;
import org.geotoolkit.ows.xml.ExceptionResponse;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.ows.xml.Sections;
import org.geotoolkit.ows.xml.v100.SectionsType;
import org.opengis.filter.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.constellation.ws.rs.OGCWebService;
import com.examind.sts.core.STSWorker;
import org.geotoolkit.ogc.xml.FilterXmlFactory;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Controller
@RequestMapping("sts/{serviceId:.+}")
public class STSService extends OGCWebService<STSWorker> {

    /**
     * Build a new Restful WFS service.
     */
    public STSService() {
        super(Specification.STS);
        LOGGER.log(Level.INFO, "STS REST service running");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseObject treatIncomingRequest(final Object objectRequest, final STSWorker worker) {

        ServiceDef version    = null;

        try {

            // if the request is not an xml request we fill the request parameter.
            final RequestBase request;
            if (objectRequest == null) {
                version = worker.getVersionFromNumber(getParameter(VERSION_PARAMETER, false)); // needed if exception is launch before request build
                request = adaptQuery(getParameter(REQUEST_PARAMETER, true), worker);
            } else if (objectRequest instanceof RequestBase) {
                request = (RequestBase) objectRequest;
            } else {
                throw new CstlServiceException("The operation " + objectRequest.getClass().getName() + " is not supported by the service",
                        INVALID_PARAMETER_VALUE, "request");
            }
            version = worker.getVersionFromNumber(request.getVersion());

            /*if (request instanceof GetCapabilities) {
                final GetCapabilities model = (GetCapabilities) request;
                String outputFormat = model.getFirstAcceptFormat();
                if (outputFormat == null) {
                    outputFormat = "application/xml";
                }
                return new ResponseObject(worker.getCapabilities(model), outputFormat);

            } else if (request instanceof DescribeFeatureType) {
                final DescribeFeatureType model = (DescribeFeatureType) request;
                String requestOutputFormat = model.getOutputFormat();
                final String outputFormat;
                if (requestOutputFormat == null || requestOutputFormat.equals("text/xml; subtype=\"gml/3.1.1\"")) {
                    outputFormat = GML_3_1_1_MIME;
                } else if (requestOutputFormat.equals("text/xml; subtype=\"gml/3.2.1\"") || requestOutputFormat.equals("text/xml; subtype=\"gml/3.2\"")||
                           requestOutputFormat.equals("application/gml+xml; version=3.2")) {
                    outputFormat = GML_3_2_1_MIME;
                } else {
                    outputFormat = requestOutputFormat;
                }
                LOGGER.log(Level.INFO, "outputFormat asked:{0}", requestOutputFormat);

                return new ResponseObject(worker.describeFeatureType(model), outputFormat);

            }*/

            throw new CstlServiceException("The operation " + request.getClass().getName() + " is not supported by the service",
                                          INVALID_PARAMETER_VALUE, "request");

        } catch (CstlServiceException ex) {
            return processExceptionResponse(ex, version, worker);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject processExceptionResponse(final CstlServiceException ex, ServiceDef serviceDef, final Worker worker) {
         // asking for authentication
        if (ex instanceof UnauthorizedException) {
            Map<String, String> headers = new HashMap<>();
            headers.put("WWW-Authenticate", " Basic");
            return new ResponseObject(HttpStatus.UNAUTHORIZED, headers);
        }
        logException(ex);

        if (serviceDef == null) {
            serviceDef = worker.getBestVersion(null);
        }
        final String version         = serviceDef.exceptionVersion.toString();
        final String exceptionCode   = getOWSExceptionCodeRepresentation(ex.getExceptionCode());
        final ExceptionResponse report;
        if (serviceDef.exceptionVersion.toString().equals("1.0.0")) {
            report = new org.geotoolkit.ows.xml.v100.ExceptionReport(ex.getMessage(), exceptionCode, ex.getLocator(), version);
            return new ResponseObject(report, "text/xml");
        } else {
            report = new org.geotoolkit.ows.xml.v110.ExceptionReport(ex.getMessage(), exceptionCode, ex.getLocator(), version);
            final int port = getHttpCodeFromErrorCode(exceptionCode);
            return new ResponseObject(report, "text/xml", port);
        }
    }

    private int getHttpCodeFromErrorCode(final String exceptionCode) {
        if (null ==exceptionCode) {
            return 200;
        } else switch (exceptionCode) {
            case "CannotLockAllFeatures":
            case "FeaturesNotLocked":
            case "InvalidLockId":
            case "InvalidValue":
            case "OperationParsingFailed":
            case "OperationNotSupported":
            case "MissingParameterValue":
            case "InvalidParameterValue":
            case "VersionNegotiationFailed":
            case "InvalidUpdateSequence":
            case "OptionNotSupported":
            case "NoApplicableCode":
                return 400;
            case "DuplicateStoredQueryIdValue":
            case "DuplicateStoredQueryParameterName":
                return 409;
            case "LockHasExpired":
            case "OperationProcessingFailed":
                return 403;
            default:
                return 200;
        }
    }


    private RequestBase adaptQuery(final String request, final STSWorker worker) throws CstlServiceException {
        /*if (STR_GETCAPABILITIES.equalsIgnoreCase(request)) {
            return createNewGetCapabilitiesRequest(worker);
        } else if (STR_DESCRIBEFEATURETYPE.equalsIgnoreCase(request)) {
            return createNewDescribeFeatureTypeRequest(worker);
        } */
        throw new CstlServiceException("The operation " + request + " is not supported by the service",
                        INVALID_PARAMETER_VALUE, "request");
    }

    private SortBy parseSortByParameter(String version) {
        // TODO handle multiple properties and handle prefixed properties
        String sortByParam = getSafeParameter("sortBy");
        final SortBy sortBy;
        if (sortByParam != null) {
            if (sortByParam.indexOf(':') != -1) {
                sortByParam = sortByParam.substring(sortByParam.indexOf(':') + 1);
            }
            //we get the order
            final SortOrder order;
            if (sortByParam.indexOf(' ') != -1) {
                final char cOrder = sortByParam.charAt(sortByParam.length() -1);
                sortByParam = sortByParam.substring(0, sortByParam.indexOf(' '));
                if (cOrder == 'D') {
                    order = SortOrder.DESCENDING;
                } else {
                    order = SortOrder.ASCENDING;
                }
            } else {
                order = SortOrder.ASCENDING;
            }
            sortBy =  (SortBy) FilterXmlFactory.buildSortProperty(version, sortByParam, order);
        } else {
            sortBy = null;
        }
        return sortBy;
    }

    private Integer parseOptionalIntegerParam(String paramName) throws CstlServiceException {
        Integer result = null;
        final String max = getParameter(paramName, false);
        if (max != null) {
            try {
                result = Integer.parseInt(max);
            } catch (NumberFormatException ex) {
                throw new CstlServiceException("Unable to parse the integer " + paramName + " parameter" + max,
                                                  INVALID_PARAMETER_VALUE, paramName);
            }

        }
        return result;
    }

    private List<String> parseCommaSeparatedParameter(String paramName) throws CstlServiceException {
        final String propertyNameParam = getParameter(paramName, false);
        final List<String> results = new ArrayList<>();
        if (propertyNameParam != null) {
            final StringTokenizer tokens = new StringTokenizer(propertyNameParam, ",;");
            while (tokens.hasMoreTokens()) {
                final String token = tokens.nextToken().trim();
                results.add(token);
            }
        }
        return results;
    }




    @RequestMapping(path = "{version:.+}", method = RequestMethod.GET)
    public ResponseEntity processGetCapabilitiesRestful(@PathVariable("serviceId") String serviceId, @PathVariable("version") final String version) throws CstlServiceException {
       /* putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                worker.checkVersionSupported(version, true);
                final Sections sections;
                final String section = getSafeParameter(SECTIONS_PARAMETER);
                if (section != null && !section.equalsIgnoreCase("All")) {
                    final List<String> requestedSections = new ArrayList<>();
                    final StringTokenizer tokens = new StringTokenizer(section, ",;");
                    while (tokens.hasMoreTokens()) {
                        final String token = tokens.nextToken().trim();
                        if (SectionsType.getExistingSections().contains(token)){
                            requestedSections.add(token);
                        } else {
                            throw new CstlServiceException("The section " + token + " does not exist",
                                                          INVALID_PARAMETER_VALUE, "Sections");
                        }
                    }
                    sections = buildSections(version, requestedSections);
                } else {
                    sections = null;

                }
                final List<String> versions = new ArrayList<>();
                versions.add(version);
                final AcceptVersions acceptVersions = buildAcceptVersion(version, versions);
                final GetCapabilities gc = WFSXmlFactory.buildGetCapabilities(version, acceptVersions, sections, null, null, "WFS");
                return treatIncomingRequest(gc).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }*/
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }



}
