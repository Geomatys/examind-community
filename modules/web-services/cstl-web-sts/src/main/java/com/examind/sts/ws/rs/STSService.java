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

import static com.examind.sts.core.STSConstants.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import static org.constellation.api.QueryConstants.REQUEST_PARAMETER;
import static org.constellation.api.QueryConstants.VERSION_PARAMETER;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.UnauthorizedException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.ows.xml.ExceptionResponse;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.ows.xml.RequestBase;
import org.opengis.filter.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.constellation.ws.rs.OGCWebService;
import com.examind.sts.core.STSWorker;
import java.util.LinkedHashMap;
import org.geotoolkit.sts.AbstractSTSRequest;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetLocations;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.GetThings;
import org.springframework.http.MediaType;


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

            if (request instanceof GetFeatureOfInterests) {
                final GetFeatureOfInterests model = (GetFeatureOfInterests) request;
                return new ResponseObject(worker.getFeatureOfInterests(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetThings) {
                final GetThings model = (GetThings) request;
                return new ResponseObject(worker.getThings(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetObservations) {
                final GetObservations model = (GetObservations) request;
                return new ResponseObject(worker.getObservations(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetDatastreams) {
                final GetDatastreams model = (GetDatastreams) request;
                return new ResponseObject(worker.getDatastreams(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetObservedProperties) {
                final GetObservedProperties model = (GetObservedProperties) request;
                return new ResponseObject(worker.getObservedProperties(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetLocations) {
                final GetLocations model = (GetLocations) request;
                return new ResponseObject(worker.getLocations(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetSensors) {
                final GetSensors model = (GetSensors) request;
                return new ResponseObject(worker.getSensors(model), MediaType.APPLICATION_JSON_UTF8);
            }

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
        final String version           = serviceDef.exceptionVersion.toString();
        final String exceptionCode     = getOWSExceptionCodeRepresentation(ex.getExceptionCode());
        final ExceptionResponse report = new org.geotoolkit.ows.xml.v200.ExceptionReport(ex.getMessage(), exceptionCode, ex.getLocator(), version);
        final int port = getHttpCodeFromErrorCode(exceptionCode);
        return new ResponseObject(report,  MediaType.APPLICATION_JSON_UTF8, port);
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


    private AbstractSTSRequest adaptQuery(final String requestName, final Worker worker) throws CstlServiceException {

        AbstractSTSRequest request = null;
        if (STR_GETFEATUREOFINTEREST.equalsIgnoreCase(requestName)) {
           request = new GetFeatureOfInterests();
        } else if (STR_GETTHINGS.equalsIgnoreCase(requestName)) {
           request = new GetThings();
        } else if (STR_GETOBSERVATION.equalsIgnoreCase(requestName)) {
           request = new GetObservations();
        } else if (STR_GETDATASTREAMS.equalsIgnoreCase(requestName)) {
           request = new GetDatastreams();
        } else if (STR_GETOBSERVEDPROPERTIES.equalsIgnoreCase(requestName)) {
           request = new GetObservedProperties();
        } else if (STR_GETLOCATIONS.equalsIgnoreCase(requestName)) {
           request = new GetLocations();
        } else if (STR_GETSENSORS.equalsIgnoreCase(requestName)) {
           request = new GetSensors();
        }
        if (request != null) {
            request.setCount(getBooleanParameter(COUNT, false));
            request.setFilter(getParameter(FILTER, false));
            request.setSkip(parseOptionalIntegerParam(SKIP));
            request.setTop(parseOptionalIntegerParam(TOP));
            request.setExpand(parseCommaSeparatedParameter(EXPAND));
            request.setSelect(parseCommaSeparatedParameter(SELECT));
            request.setOrderby(parseSortByParameter());
            return request;
        } else {
            throw new CstlServiceException("The operation " + requestName + " is not supported by the service",
                        INVALID_PARAMETER_VALUE, "request");
        }
    }

    private Map<String, SortOrder> parseSortByParameter() throws CstlServiceException {
        Map<String, SortOrder> results = new LinkedHashMap<>();
        List<String> sortByParams = parseCommaSeparatedParameter(ORDERBY);
        for (String sortByParam : sortByParams) {
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
            results.put(sortByParam, order);
        }
        return results;
    }

    @RequestMapping(path = "FeatureOfInterests", method = RequestMethod.GET)
    public ResponseEntity getFeatureOfInterests(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETFEATUREOFINTEREST, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Things", method = RequestMethod.GET)
    public ResponseEntity getThings(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETTHINGS, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Observations", method = RequestMethod.GET)
    public ResponseEntity getObservations(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETOBSERVATION, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Datastreams", method = RequestMethod.GET)
    public ResponseEntity getDatastreams(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETDATASTREAMS, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "ObservedProperties", method = RequestMethod.GET)
    public ResponseEntity getObservedProperties(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETOBSERVEDPROPERTIES, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Locations", method = RequestMethod.GET)
    public ResponseEntity getLocations(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETLOCATIONS, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Sensors", method = RequestMethod.GET)
    public ResponseEntity getSensors(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETSENSORS, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "HistoricalLocations", method = RequestMethod.GET)
    public ResponseEntity getHistoricalLocations(@PathVariable("serviceId") String serviceId) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = adaptQuery(STR_GETHISTORICALLOCATIONS, worker);
                return treatIncomingRequest(request).getResponseEntity();
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity();
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }
}
