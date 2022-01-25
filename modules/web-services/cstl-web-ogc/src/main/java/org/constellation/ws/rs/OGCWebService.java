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
package org.constellation.ws.rs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.validation.Schema;
import org.apache.sis.util.iso.Types;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.IWSEngine;
import org.constellation.ws.UnauthorizedException;
import org.constellation.ws.Worker;
import org.geotoolkit.ows.xml.ExceptionResponse;
import org.geotoolkit.ows.xml.OWSExceptionCode;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_CRS;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_DIMENSION_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_FORMAT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_POINT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_REQUEST;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.CURRENT_UPDATE_SEQUENCE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.TILE_OUT_OF_RANGE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;
import org.geotoolkit.ows.xml.OWSXmlFactory;
import org.geotoolkit.util.StringUtilities;
import org.opengis.util.CodeList;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;


/**
 * Abstract parent REST facade for all OGC web services in Constellation.
 * <p>
 * The Open Geospatial Consortium (OGC) has defined a number of web services for
 * geospatial data such as:
 * </p>
 * <ul>
 *   <li><b>CSW</b> -- Catalog Service for the Web</li>
 *   <li><b>WMS</b> -- Web Map Service</li>
 *   <li><b>WCS</b> -- Web Coverage Service</li>
 *   <li><b>SOS</b> -- Sensor Observation Service</li>
 * </ul>
 * <p>
 * Many of these Web Services have been defined to work with REST based HTTP
 * message exchange; this class provides base functionality for those services.
 * </p>
 *
 * @version $Id$
 *
 * @author Guilhem Legal (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @author Garcia Benjamin (Geomatys)
 *
 * @version 0.9
 * @since 0.3
 */
public abstract class OGCWebService<W extends Worker> extends AbstractWebService {

    private final String serviceName;

    @Inject
    protected IWSEngine wsengine;

    /**
     * Initialize the basic attributes of a web serviceType.
     *
     * @param specification The OGC specification.
     */
    public OGCWebService(final Specification specification) {
        super();
        if (specification == null){
            throw new IllegalArgumentException("It is compulsory for a web service to have a specification.");
        }
        if(SpringHelper.get()!=null){
            SpringHelper.injectDependencies(this);
        }
        this.serviceName = specification.name();

        LOGGER.log(Level.INFO, "Starting the REST {0} service facade.", serviceName);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isRequestValidationActivated(final String serviceID) {
        if (serviceID != null && wsengine.serviceInstanceExist(serviceName, serviceID)) {
            final W worker = (W) wsengine.getInstance(serviceName, serviceID);
            return worker.isRequestValidationActivated();
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Schema> getRequestValidationSchema(final String serviceID) {
        if (serviceID != null && wsengine.serviceInstanceExist(serviceName, serviceID)) {
            final W worker = (W) wsengine.getInstance(serviceName, serviceID);
            return worker.getRequestValidationSchema();
        }
        return new ArrayList<>();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseObject treatIncomingRequest(final Object request) {
        final Object objectRequest;
        if (request instanceof JAXBElement) {
            objectRequest = ((JAXBElement) request).getValue();
            LOGGER.log(Level.FINER, "request type:{0}", request.getClass().getName());
        } else {
            objectRequest = request;
        }

        final String serviceID = getSafeParameter("serviceId");
        final W worker = getWorker(serviceID);

        // request is send to the specified worker
        if (worker != null) {
            if (worker.isSecured()) {
                final String ip = getHttpServletRequest().getRemoteAddr();
                final String referer = getHeaderValue("referer");
                if (!worker.isAuthorized(ip, referer)) {
                    LOGGER.log(Level.INFO, "Received a request from unauthorized ip:{0} or referer:{1}",
                            new String[]{ip, referer});
                    return new ResponseObject(HttpStatus.UNAUTHORIZED);
                }
            }
            if (worker.isPostRequestLog()) {
                logPostParameters(request);
            }
            if (worker.isPrintRequestParameter()) {
                logParameters();
            }
            return treatIncomingRequest(objectRequest, worker);

        // unbounded URL
        } else {
            LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceID);
            return new ResponseObject(HttpStatus.NOT_FOUND);
        }
    }

    protected W getWorker(String serviceID) {
        if (serviceID != null && wsengine.serviceInstanceExist(serviceName, serviceID)) {
            W worker = (W) wsengine.getInstance(serviceName, serviceID);
            worker.setServiceUrl(getServiceURL());
            return worker;
        }
        return null;
    }

    /**
     * Treat the incoming request and call the right function.
     *
     * @param objectRequest if the server receive a POST request in XML,
     *        this object contain the request. Else for a GET or a POST kvp
     *        request this parameter is {@code null}
     *
     * @param worker the selected worker on which apply the request.
     *
     * @return an xml response.
     */
    protected abstract ResponseObject treatIncomingRequest(final Object objectRequest,final  W worker);

    /**
     * Handle all exceptions returned by a web service operation in two ways:
     * <ul>
     *   <li>if the exception code indicates a mistake done by the user, just display a single
     *       line message in logs.</li>
     *   <li>otherwise logs the full stack trace in logs, because it is something interesting for
     *       a developer</li>
     * </ul>
     * In both ways, the exception is then marshalled and returned to the client.
     *
     * @param exc        The exception that has been generated during the web-service operation requested.
     * @param serviceDef The service definition, from which the version number of exception report will
     *                   be extracted. if {@code null} the default version of the worker will be used.
     * @param w the selected worker on which apply the request.
     *
     * @return A HTTP response oject representing the exception.
     */
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w) {
        return processExceptionResponse(exc, serviceDef, w, getExceptionMimeType());
    }
    
    /**
     * Handle all exceptions returned by a web service operation in two ways:
     * <ul>
     *   <li>if the exception code indicates a mistake done by the user, just display a single
     *       line message in logs.</li>
     *   <li>otherwise logs the full stack trace in logs, because it is something interesting for
     *       a developer</li>
     * </ul>
     * In both ways, the exception is then marshalled and returned to the client.
     *
     * @param exc        The exception that has been generated during the web-service operation requested.
     * @param serviceDef The service definition, from which the version number of exception report will
     *                   be extracted. if {@code null} the default version of the worker will be used.
     * @param w the selected worker on which apply the request.
     * @param mimeType The mime type to use to return the http response.
     *
     * @return A HTTP response oject representing the exception.
     */
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w, MediaType mimeType) {
        final CstlServiceException ex = CstlServiceException.castOrWrap(exc);
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
        final String version           = serviceDef.version.toString();
        final String owsVersion        = serviceDef.owsVersion.toString();
        final String exVersion         = serviceDef.exceptionVersion.toString();
        final String exceptionCode     = getOWSExceptionCodeRepresentation(ex.getExceptionCode());
        final ExceptionResponse report = OWSXmlFactory.buildExceptionReport(owsVersion, ex.getMessage(), exceptionCode, ex.getLocator(), exVersion);
        final Integer status           = getHttpCodeFromErrorCode(exceptionCode, version);
        return new ResponseObject(report, mimeType, status);
    }

    /**
     * return a specific http code correspounding to the exception code.
     * Default implementation return always 200 (OK). Sub-classes may override this method to return specific code.
     * The acting version of the service is specified because in some standard, a version send some related version http code.
     * 
     * @param exceptionCode An ows error code.
     * @param version Acting version of the service.
     * 
     * @return An HTTP status code.
     */
    protected int getHttpCodeFromErrorCode(final String exceptionCode, String version) {
        return 200;
    }

    /**
     * Return the default mime type to use, to return Exception report.
     * Default value is XML, sub-classes may override this method to return specific value.
     *
     * @return A mime type.
     */
    protected MediaType getExceptionMimeType() {
        return MediaType.TEXT_XML;
    }

    /**
     * The shared method to build a service ExceptionReport.
     *
     * @param message A message dscribing the error.
     * @param codeName An error code.
     * @return
     */
    @Override
    protected ResponseObject launchException(final String message, String codeName, final String locator) {
        final String serviceID = getSafeParameter("serviceId");
        final W worker = (W) wsengine.getInstance(serviceName, serviceID);
        ServiceDef mainVersion = null;
        if (worker != null) {
            mainVersion = worker.getBestVersion(null);
            if (mainVersion.owsCompliant) {
                codeName = StringUtilities.transformCodeName(codeName);
            }
        }
        final OWSExceptionCode code   = Types.forCodeName(OWSExceptionCode.class, codeName, true);
        final CstlServiceException ex = new CstlServiceException(message, code, locator);
        return processExceptionResponse(ex, mainVersion, worker);
    }

    /**
     * Return the correct representation of an OWS exceptionCode
     *
     * @param exceptionCode
     * @return
     */
    protected String getOWSExceptionCodeRepresentation(final CodeList exceptionCode) {
        final String codeRepresentation;
        if (exceptionCode instanceof org.constellation.ws.ExceptionCode) {
            codeRepresentation = StringUtilities.transformCodeName(exceptionCode.name());
        } else {
            codeRepresentation = exceptionCode.name();
        }
        return codeRepresentation;
    }

    /**
     * We don't print the stack trace:
     * - if the user have forget a mandatory parameter.
     * - if the version number is wrong.
     * - if the user have send a wrong request parameter
     *
     *  @param ex The exception to log
     */
    protected void logException(final CstlServiceException ex) {
        if (!ex.getExceptionCode().equals(MISSING_PARAMETER_VALUE)    && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.MISSING_PARAMETER_VALUE) &&
            !ex.getExceptionCode().equals(VERSION_NEGOTIATION_FAILED) && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.VERSION_NEGOTIATION_FAILED) &&
            !ex.getExceptionCode().equals(INVALID_PARAMETER_VALUE)    && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_PARAMETER_VALUE) &&
            !ex.getExceptionCode().equals(OPERATION_NOT_SUPPORTED)    && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.OPERATION_NOT_SUPPORTED) &&
            !ex.getExceptionCode().equals(STYLE_NOT_DEFINED)          && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.STYLE_NOT_DEFINED) &&
            !ex.getExceptionCode().equals(INVALID_POINT)              && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_POINT) &&
            !ex.getExceptionCode().equals(INVALID_FORMAT)             && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_FORMAT) &&
            !ex.getExceptionCode().equals(INVALID_CRS)                && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_CRS) &&
            !ex.getExceptionCode().equals(LAYER_NOT_DEFINED)          && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.LAYER_NOT_DEFINED) &&
            !ex.getExceptionCode().equals(INVALID_REQUEST)            && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_REQUEST) &&
            !ex.getExceptionCode().equals(INVALID_UPDATE_SEQUENCE)    && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_UPDATE_SEQUENCE) &&
            !ex.getExceptionCode().equals(CURRENT_UPDATE_SEQUENCE)    && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.CURRENT_UPDATE_SEQUENCE) &&
            !ex.getExceptionCode().equals(INVALID_VALUE)              && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.AXIS_LABEL_INVALID) &&
            !ex.getExceptionCode().equals(INVALID_DIMENSION_VALUE)    && !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_DIMENSION_VALUE) &&
            !ex.getExceptionCode().equals(TILE_OUT_OF_RANGE)          &&
            !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_SRS) &&
            !ex.getExceptionCode().equals(org.constellation.ws.ExceptionCode.INVALID_SUBSETTING)) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        } else {
            LOGGER.log(Level.INFO, "SENDING EXCEPTION: {0} {1}", new Object[]{ex.getExceptionCode().name(), ex.getMessage()});
        }
    }

    /**
     * Return the number of instance if the web-service
     */
    protected int getWorkerMapSize() {
        return wsengine.getInstanceSize(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @PreDestroy
    @Override
    public void destroy() {
        super.destroy();
        LOGGER.log(Level.INFO, "Shutting down the REST {0} service facade.", serviceName);
        wsengine.destroyInstances(serviceName);
    }
}
