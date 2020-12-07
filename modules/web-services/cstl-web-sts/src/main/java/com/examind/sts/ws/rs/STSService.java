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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geotoolkit.sts.AbstractSTSRequest;
import org.geotoolkit.sts.AbstractSTSRequestById;
import org.geotoolkit.sts.GetCapabilities;
import org.geotoolkit.sts.GetDatastreamById;
import org.geotoolkit.sts.GetDatastreams;
import org.geotoolkit.sts.GetFeatureOfInterestById;
import org.geotoolkit.sts.GetFeatureOfInterests;
import org.geotoolkit.sts.GetHistoricalLocationById;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetLocationById;
import org.geotoolkit.sts.GetLocations;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.GetMultiDatastreams;
import org.geotoolkit.sts.GetObservationById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetSensorById;
import org.geotoolkit.sts.GetSensors;
import org.geotoolkit.sts.GetThingById;
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

             // Handle an empty request by sending a basic web page.
            if ((null == objectRequest) && isGetCapaRequest()) {
                return getCapabilities(worker);
            }

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

            if (request instanceof GetCapabilities) {
                final GetCapabilities model = (GetCapabilities) request;
                return new ResponseObject(worker.getCapabilities(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetFeatureOfInterests) {
                final GetFeatureOfInterests model = (GetFeatureOfInterests) request;
                return new ResponseObject(worker.getFeatureOfInterests(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetFeatureOfInterestById) {
                final GetFeatureOfInterestById model = (GetFeatureOfInterestById) request;
                return new ResponseObject(worker.getFeatureOfInterestById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetThings) {
                final GetThings model = (GetThings) request;
                return new ResponseObject(worker.getThings(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetThingById) {
                final GetThingById model = (GetThingById) request;
                return new ResponseObject(worker.getThingById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetObservations) {
                final GetObservations model = (GetObservations) request;
                return new ResponseObject(worker.getObservations(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetObservationById) {
                final GetObservationById model = (GetObservationById) request;
                return new ResponseObject(worker.getObservationById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetDatastreams) {
                final GetDatastreams model = (GetDatastreams) request;
                return new ResponseObject(worker.getDatastreams(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetDatastreamById) {
                final GetDatastreamById model = (GetDatastreamById) request;
                return new ResponseObject(worker.getDatastreamById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetMultiDatastreams) {
                final GetMultiDatastreams model = (GetMultiDatastreams) request;
                return new ResponseObject(worker.getMultiDatastreams(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetMultiDatastreamById) {
                final GetMultiDatastreamById model = (GetMultiDatastreamById) request;
                return new ResponseObject(worker.getMultiDatastreamById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetObservedProperties) {
                final GetObservedProperties model = (GetObservedProperties) request;
                return new ResponseObject(worker.getObservedProperties(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetObservedPropertyById) {
                final GetObservedPropertyById model = (GetObservedPropertyById) request;
                return new ResponseObject(worker.getObservedPropertyById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetLocations) {
                final GetLocations model = (GetLocations) request;
                return new ResponseObject(worker.getLocations(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetLocationById) {
                final GetLocationById model = (GetLocationById) request;
                return new ResponseObject(worker.getLocationById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetHistoricalLocations) {
                final GetHistoricalLocations model = (GetHistoricalLocations) request;
                return new ResponseObject(worker.getHistoricalLocations(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetHistoricalLocationById) {
                final GetHistoricalLocationById model = (GetHistoricalLocationById) request;
                return new ResponseObject(worker.getHistoricalLocationById(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetSensors) {
                final GetSensors model = (GetSensors) request;
                return new ResponseObject(worker.getSensors(model), MediaType.APPLICATION_JSON_UTF8);

            } else if (request instanceof GetSensorById) {
                final GetSensorById model = (GetSensorById) request;
                return new ResponseObject(worker.getSensorById(model), MediaType.APPLICATION_JSON_UTF8);
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


    private RequestBase adaptQuery(final String requestName, final Worker worker) throws CstlServiceException {

        RequestBase request = null;
        if (STR_GETFEATUREOFINTEREST.equalsIgnoreCase(requestName)) {
           request = new GetFeatureOfInterests();
        } else if (STR_GETTHINGS.equalsIgnoreCase(requestName)) {
           request = new GetThings();
        } else if (STR_GETTHING_BYID.equalsIgnoreCase(requestName)) {
           request = new GetThingById();
        } else if (STR_GETFEATUREOFINTEREST_BYID.equalsIgnoreCase(requestName)) {
           request = new GetFeatureOfInterestById();
        } else if (STR_GETOBSERVATION_BYID.equalsIgnoreCase(requestName)) {
           request = new GetObservationById();
        } else if (STR_GETOBSERVATION.equalsIgnoreCase(requestName)) {
           request = new GetObservations();
        } else if (STR_GETDATASTREAMS.equalsIgnoreCase(requestName)) {
           request = new GetDatastreams();
        } else if (STR_GETDATASTREAM_BYID.equalsIgnoreCase(requestName)) {
           request = new GetDatastreamById();
        } else if (STR_GETMULTIDATASTREAMS.equalsIgnoreCase(requestName)) {
           request = new GetMultiDatastreams();
        } else if (STR_GETMULTIDATASTREAM_BYID.equalsIgnoreCase(requestName)) {
           request = new GetMultiDatastreamById();
        } else if (STR_GETOBSERVEDPROPERTIES.equalsIgnoreCase(requestName)) {
           request = new GetObservedProperties();
        } else if (STR_GETOBSERVEDPROPERTY_BYID.equalsIgnoreCase(requestName)) {
           request = new GetObservedPropertyById();
        } else if (STR_GETLOCATIONS.equalsIgnoreCase(requestName)) {
           request = new GetLocations();
        } else if (STR_GETLOCATION_BYID.equalsIgnoreCase(requestName)) {
           request = new GetLocationById();
        } else if (STR_GETSENSORS.equalsIgnoreCase(requestName)) {
           request = new GetSensors();
        } else if (STR_GETSENSOR_BYID.equalsIgnoreCase(requestName)) {
           request = new GetSensorById();
        } else if (STR_GETCAPABILITIES.equalsIgnoreCase(requestName)) {
           request = new GetCapabilities();
        } else if (STR_GETHISTORICALLOCATIONS.equalsIgnoreCase(requestName)) {
           request = new GetHistoricalLocations();
        } else if (STR_GETHISTORICALLOCATION_BYID.equalsIgnoreCase(requestName)) {
           request = new GetHistoricalLocationById();
        }
        if (request instanceof AbstractSTSRequest) {
            AbstractSTSRequest sRequest = (AbstractSTSRequest) request;
            sRequest.setCount(getBooleanParameter(COUNT, false));
            sRequest.setFilter(getParameter(FILTER, false));
            sRequest.setResultFormat(getParameter(RESULT_FORMAT, false));
            sRequest.setSkip(parseOptionalIntegerParam(SKIP));
            sRequest.setTop(parseOptionalIntegerParam(TOP));
            sRequest.setExpand(parseCommaSeparatedParameter(EXPAND));
            sRequest.setSelect(parseCommaSeparatedParameter(SELECT));
            sRequest.setOrderby(parseSortByParameter());
            // extended param decimation
            String deci = getParameter(DECIMATION, false);
            if (deci != null) {
                sRequest.getExtraFlag().put("decimation", deci);
            }
            return request;
        } else if (request instanceof AbstractSTSRequestById) {
            AbstractSTSRequestById sRequest = (AbstractSTSRequestById) request;
            sRequest.setExpand(parseCommaSeparatedParameter(EXPAND));
            sRequest.setSelect(parseCommaSeparatedParameter(SELECT));
            sRequest.setResultFormat(getParameter(RESULT_FORMAT, false));
            sRequest.setId(getParameter("id", true));
            return request;
        } else if (request instanceof RequestBase) {
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

    private ResponseObject getCapabilities(STSWorker worker) throws CstlServiceException {
        if (worker != null) {
            return new ResponseObject(worker.getCapabilities(new GetCapabilities()), MediaType.APPLICATION_JSON_UTF8);
        }
        return new ResponseObject(HttpStatus.NOT_FOUND);
    }

    private boolean isGetCapaRequest() {
        Map<String, String[]> params = new HashMap<>(getParameters());
        params.remove("serviceId");
        return params.isEmpty();
    }

    @RequestMapping(path = "FeaturesOfInterest", method = RequestMethod.GET)
    public ResponseEntity getFeatureOfInterests(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETFEATUREOFINTEREST, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Observations({id:[^\\)]+})/FeaturesOfInterest", method = RequestMethod.GET)
    public ResponseEntity getFeatureOfInterestForObservation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETFEATUREOFINTEREST, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "FeaturesOfInterest({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getFeatureOfInterestById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETFEATUREOFINTEREST_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Things", method = RequestMethod.GET)
    public ResponseEntity getThings(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETTHINGS, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Things({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getThingsById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETTHING_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Observations", method = RequestMethod.GET)
    public ResponseEntity getObservations(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVATION, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "FeaturesOfInterest({id:[^\\)]+})/Observations", method = RequestMethod.GET)
    public ResponseEntity getObservationForFoi(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVATION, worker);
                request.getExtraFilter().put("featureOfInterest", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Observations({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getObservationsById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETOBSERVATION_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Datastreams", method = RequestMethod.GET)
    public ResponseEntity getDatastreams(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETDATASTREAMS, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Datastreams({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getDatastreamById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETDATASTREAM_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Observations({id:[^\\)]+})/Datastreams", method = RequestMethod.GET)
    public ResponseEntity getDatastreamsForObservation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETDATASTREAMS, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "ObservedProperties({id:[^\\)]+})/Datastreams", method = RequestMethod.GET)
    public ResponseEntity getDatastreamsForObservedProperty(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETDATASTREAMS, worker);
                request.getExtraFilter().put("observedProperty", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "MultiDatastreams", method = RequestMethod.GET)
    public ResponseEntity getMultiDatastreams(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETMULTIDATASTREAMS, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "MultiDatastreams({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getMultiDatastreamById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETMULTIDATASTREAM_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Observations({id:[^\\)]+})/MultiDatastreams", method = RequestMethod.GET)
    public ResponseEntity getMultiDatastreamsForObservation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETMULTIDATASTREAMS, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "ObservedProperties({id:[^\\)]+})/MultiDatastreams", method = RequestMethod.GET)
    public ResponseEntity getMultiDatastreamsForObservedProperty(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETMULTIDATASTREAMS, worker);
                request.getExtraFilter().put("observedProperty", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }


    @RequestMapping(path = "ObservedProperties", method = RequestMethod.GET)
    public ResponseEntity getObservedProperties(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVEDPROPERTIES, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "ObservedProperties({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getObservedPropertyById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETOBSERVEDPROPERTY_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Datastreams({id:[^\\)]+})/ObservedProperties", method = RequestMethod.GET)
    public ResponseEntity getObservedPropertyForDataStream(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVEDPROPERTIES, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Datastreams({id:[^\\)]+})/Observations", method = RequestMethod.GET)
    public ResponseEntity getObservationForDataStream(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVATION, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Datastreams({id:[^\\)]+})/Sensors", method = RequestMethod.GET)
    public ResponseEntity getSensorsForDataStream(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETSENSORS, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "MultiDatastreams({id:[^\\)]+})/ObservedProperties", method = RequestMethod.GET)
    public ResponseEntity getObservedPropertyForMultiDataStream(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVEDPROPERTIES, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("forMDS", "true");
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "MultiDatastreams({id:[^\\)]+})/Observations", method = RequestMethod.GET)
    public ResponseEntity getObservationForMultiDataStream(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETOBSERVATION, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("forMDS", "true");
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "MultiDatastreams({id:[^\\)]+})/Sensors", method = RequestMethod.GET)
    public ResponseEntity getSensorsForMultiDataStream(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETSENSORS, worker);
                request.getExtraFilter().put("observationId", id);
                request.getExtraFlag().put("forMDS", "true");
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }


    @RequestMapping(path = "Locations", method = RequestMethod.GET)
    public ResponseEntity getLocations(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETLOCATIONS, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Locations({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getLocationById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETLOCATION_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Locations({id:[^\\)]+})/Things", method = RequestMethod.GET)
    public ResponseEntity getThingsForLocation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETTHINGS, worker);
                request.getExtraFilter().put("procedure", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Sensors", method = RequestMethod.GET)
    public ResponseEntity getSensors(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETSENSORS, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Sensors({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getSensorById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETSENSOR_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Sensors({id:[^\\)]+})/Datastreams", method = RequestMethod.GET)
    public ResponseEntity getDatastreamsForSensor(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETDATASTREAMS, worker);
                request.getExtraFilter().put("procedure", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Things({id:[^\\)]+})/Datastreams", method = RequestMethod.GET)
    public ResponseEntity getDatastreamsForThing(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETDATASTREAMS, worker);
                request.getExtraFilter().put("procedure", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Sensors({id:[^\\)]+})/MultiDatastreams", method = RequestMethod.GET)
    public ResponseEntity getMultiDatastreamsForSensor(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETMULTIDATASTREAMS, worker);
                request.getExtraFilter().put("procedure", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Things({id:[^\\)]+})/MultiDatastreams", method = RequestMethod.GET)
    public ResponseEntity getMultiDatastreamsForThing(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETMULTIDATASTREAMS, worker);
                request.getExtraFilter().put("procedure", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "HistoricalLocations", method = RequestMethod.GET)
    public ResponseEntity getHistoricalLocations(@PathVariable("serviceId") String serviceId, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETHISTORICALLOCATIONS, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "HistoricalLocations({id:[^\\)]+})", method = RequestMethod.GET)
    public ResponseEntity getHistoricalLocationById(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
        putServiceIdParam(serviceId);
        id = removeQuote(id);
        putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequestById request = (AbstractSTSRequestById) adaptQuery(STR_GETHISTORICALLOCATION_BYID, worker);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }


    @RequestMapping(path = "HistoricalLocations({id:[^\\)]+})/Things", method = RequestMethod.GET)
    public ResponseEntity getThingsForHistoricalLocation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETTHINGS, worker);
                int pos = id.lastIndexOf('-');
                if (pos != -1) {
                    String sensorId = id.substring(0, pos);
                    String timeStr = id.substring(pos + 1); // not neccesary for finding thing
                    request.getExtraFilter().put("procedure", sensorId);
                    request.getExtraFlag().put("orig-path", req.getPathInfo());
                    return treatIncomingRequest(request).getResponseEntity(response);
                }
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Locations({id:[^\\)]+})/HistoricalLocations", method = RequestMethod.GET)
    public ResponseEntity getHistoricalLocationForLocation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETHISTORICALLOCATIONS, worker);
                int pos = id.lastIndexOf('-');

                // single historical location
                if (pos != -1) {
                    String sensorId = id.substring(0, pos);
                    String timeStr = id.substring(pos + 1);
                    request.getExtraFilter().put("procedure", sensorId);
                    request.getExtraFlag().put("orig-path", req.getPathInfo());
                    request.getExtraFlag().put("hloc-time", timeStr);
                    return treatIncomingRequest(request).getResponseEntity(response);

                // sensor location
                } else {
                    request.getExtraFilter().put("procedure", id);
                    request.getExtraFlag().put("orig-path", req.getPathInfo());
                    request.getExtraFlag().put("hloc-time", "no-time");
                    return treatIncomingRequest(request).getResponseEntity(response);
                }
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    @RequestMapping(path = "Things({id:[^\\)]+})/HistoricalLocations", method = RequestMethod.GET)
    public ResponseEntity getHistoricalLocationForThing(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETHISTORICALLOCATIONS, worker);
                request.getExtraFilter().put("procedure", id);
                request.getExtraFlag().put("orig-path", req.getPathInfo());
                return treatIncomingRequest(request).getResponseEntity(response);

            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }



    @RequestMapping(path = "HistoricalLocations({id:[^\\)]+})/Locations", method = RequestMethod.GET)
    public ResponseEntity getLocationsForHistoricalLocation(@PathVariable("serviceId") String serviceId, @PathVariable("id") String id, HttpServletRequest req, HttpServletResponse response) throws CstlServiceException {
       putServiceIdParam(serviceId);
       id = removeQuote(id);
       putParam("id", id);
        final Worker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                AbstractSTSRequest request = (AbstractSTSRequest) adaptQuery(STR_GETLOCATIONS, worker);
                int pos = id.lastIndexOf('-');
                if (pos != -1) {
                    String sensorId = id.substring(0, pos);
                    String timeStr = id.substring(pos + 1);
                    request.getExtraFilter().put("procedure", sensorId);
                    request.getExtraFlag().put("hloc-time", timeStr);
                    return treatIncomingRequest(request).getResponseEntity(response);
                }
            } catch (IllegalArgumentException ex) {
                return processExceptionResponse(new CstlServiceException(ex), null, worker).getResponseEntity(response);
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker).getResponseEntity(response);
            } finally {
                clearKvpMap();
            }
        }
        return new ResponseEntity(HttpStatus.NOT_FOUND);
    }

    private String removeQuote(String s) {
        if (s != null) {
            if (s.charAt(0) == '\'' && s.charAt(s.length() -1) == '\'') {
                s = s.substring(1, s.length() -1);
            }
        }
        return s;
    }
}
