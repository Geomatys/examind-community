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
package org.constellation.map.ws.rs;

import org.apache.sis.util.Version;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.api.QueryConstants;
import org.constellation.map.core.QueryContext;
import org.constellation.map.core.WMSConstant;
import org.constellation.map.core.WMSWorker;
import org.constellation.portrayal.PortrayalResponse;
import org.constellation.util.Util;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.GridWebService;
import org.constellation.ws.rs.provider.SchemaLocatedExceptionResponse;
import org.geotoolkit.client.RequestsUtilities;
import org.geotoolkit.display2d.service.DefaultPortrayalService;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ogc.xml.exception.ServiceExceptionReport;
import org.geotoolkit.ogc.xml.exception.ServiceExceptionType;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.sld.xml.GetLegendGraphic;
import org.geotoolkit.sld.xml.Specification.StyledLayerDescriptor;
import org.geotoolkit.sld.xml.v110.DescribeLayerResponseType;
import org.geotoolkit.util.StringUtilities;
import org.geotoolkit.temporal.util.TimeParser;
import org.geotoolkit.wms.xml.AbstractWMSCapabilities;
import org.geotoolkit.wms.xml.DescribeLayer;
import org.geotoolkit.wms.xml.GetCapabilities;
import org.geotoolkit.wms.xml.GetFeatureInfo;
import org.geotoolkit.wms.xml.GetMap;
import org.geotoolkit.wms.xml.WMSMarshallerPool;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.util.ArgumentChecks;
import static org.constellation.api.QueryConstants.REQUEST_PARAMETER;
import static org.constellation.api.QueryConstants.SERVICE_PARAMETER;
import static org.constellation.api.QueryConstants.UPDATESEQUENCE_PARAMETER;
import static org.constellation.api.QueryConstants.VERSION_PARAMETER;
import static org.constellation.api.ServiceConstants.GET_CAPABILITIES;
import org.constellation.business.IStyleBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConstellationException;
import static org.constellation.map.core.WMSConstant.*;
import static org.constellation.util.Util.parseLayerNameList;
import org.constellation.ws.rs.ResponseObject;
import static org.geotoolkit.client.RequestsUtilities.toDouble;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_CRS;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_DIMENSION_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_FORMAT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_POINT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import org.geotoolkit.resources.Errors;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


/**
 * The REST facade to an OGC Web Map Service, implementing versions 1.1.1 and
 * 1.3.0.
 *
 * @version 0.9
 *
 * @author Guilhem Legal (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @author Benjain Garcia (Geomatys)
 * @since 0.1
 */
@Controller
@RequestMapping("wms/{serviceId:.+}")
public class WMSService extends GridWebService<WMSWorker> {

    public static boolean writeDTD = true;

    @Autowired
    private IStyleBusiness styleBusiness;

    /**
     * Build a new instance of the webService and initialize the JAXB context.
     */
    public WMSService() {
        super(Specification.WMS);
        //we build the JAXB marshaller and unmarshaller to bind java/xml
        setXMLContext(WMSMarshallerPool.getInstance());
        LOGGER.log(Level.INFO, "WMS REST service running");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseObject treatIncomingRequest(final Object objectRequest, final WMSWorker worker) {
        ArgumentChecks.ensureNonNull("worker", worker);
        final QueryContext queryContext = new QueryContext();

        ServiceDef version = null;
        try {

            final RequestBase request;
            if (objectRequest == null) {
                version = worker.getVersionFromNumber(getParameter(VERSION_PARAMETER, false)); // needed if exception is launch before request build
                request = adaptQuery(getParameter(REQUEST_PARAMETER, true), worker, queryContext);
            } else if (objectRequest instanceof RequestBase) {
                request = (RequestBase) objectRequest;
            } else {
                throw new CstlServiceException("The operation " + objectRequest.getClass().getName() + " is not supported by the service",
                        INVALID_PARAMETER_VALUE, "request");
            }
            version = worker.getVersionFromNumber(request.getVersion());

            //Handle user's requests.
            if (request instanceof GetFeatureInfo requestFeatureInfo) {
                final Map.Entry<String, Object> result  = worker.getFeatureInfo(requestFeatureInfo);

                if (result != null) {
                    final String infoFormat = result.getKey();
                    return new ResponseObject(result.getValue(), infoFormat);
                }

                //throw an exception if result of GetFeatureInfo visitor is null
                throw new CstlServiceException("An error occurred during GetFeatureInfo response building.");
            }
            if (request instanceof GetMap requestMap) {
                final PortrayalResponse map = worker.getMap(requestMap);
                return new ResponseObject(map, requestMap.getFormat());
            }
            if (request instanceof GetCapabilities requestCapab) {
                final AbstractWMSCapabilities capabilities = worker.getCapabilities(requestCapab);
                return new ResponseObject(capabilities, requestCapab.getFormat());
            }
            if (request instanceof GetLegendGraphic requestLegend) {
                final Object legend = worker.getLegendGraphic(requestLegend);
                return new ResponseObject(legend, requestLegend.getFormat());
            }
            if (request instanceof DescribeLayer describeLayer)  {
                final DescribeLayerResponseType response = worker.describeLayer(describeLayer);
                return new ResponseObject(response, MediaType.TEXT_XML);
            }
            throw new CstlServiceException("The operation " + request + " is not supported by the service",
                                           OPERATION_NOT_SUPPORTED, QueryConstants.REQUEST_PARAMETER.toLowerCase());
        } catch (CstlServiceException ex) {
            return processExceptionResponse(queryContext, ex, version, worker);
        }
    }

    /**
     * Build request object from KVP parameters.
     *
     * @param request
     * @return
     * @throws CstlServiceException
     */
    private RequestBase adaptQuery(final String request, final Worker worker, final QueryContext queryContext) throws CstlServiceException {
         if (GETMAP.equalsIgnoreCase(request) || MAP.equalsIgnoreCase(request)) {
             return  adaptGetMap(true, queryContext, worker);

         } else if (GETFEATUREINFO.equalsIgnoreCase(request)) {
             return adaptGetFeatureInfo(queryContext, worker);

         // For backward compatibility between WMS 1.1.1 and WMS 1.0.0, we handle the "Capabilities" request
         // as "GetCapabilities" request in version 1.1.1.
         } else if (GET_CAPABILITIES.equalsIgnoreCase(request) || CAPABILITIES.equalsIgnoreCase(request)) {
             return adaptGetCapabilities(request, worker);

         } else  if (GETLEGENDGRAPHIC.equalsIgnoreCase(request)) {
             return adaptGetLegendGraphic();
         } else if (DESCRIBELAYER.equalsIgnoreCase(request)) {

             return adaptDescribeLayer(worker);
         }
         throw new CstlServiceException("The operation " + request + " is not supported by the service", INVALID_PARAMETER_VALUE, "request");
    }

    /**
     * Generate an error response in image if query asks it.
     * Otherwise this call will fallback on normal xml error.
     */
    private ResponseObject processExceptionResponse(final QueryContext queryContext, final CstlServiceException ex, ServiceDef serviceDef, final Worker w) {
        logException(ex);

        // Now handle in image response or exception report.
        if (queryContext.isErrorInimage()) {
            final BufferedImage image = DefaultPortrayalService.writeException(ex, new Dimension(600, 400), queryContext.isOpaque());
            return new ResponseObject(image, queryContext.getExceptionImageFormat());
        } else {
            return processExceptionResponse(ex, serviceDef, w);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject processExceptionResponse(final Exception exc, ServiceDef serviceDef, final Worker w) {
        final CstlServiceException ex = CstlServiceException.castOrWrap(exc);

        if (serviceDef == null) {
            serviceDef = w.getBestVersion(null);
        }
        final Version version = serviceDef.exceptionVersion;
        final String locator = ex.getLocator();
        final ServiceExceptionReport report = new ServiceExceptionReport(version,
                (locator == null) ? new ServiceExceptionType(ex.getMessage(), ex.getExceptionCode()) :
                                    new ServiceExceptionType(ex.getMessage(), ex.getExceptionCode(), locator));


        final String schemaLocation;
        if (serviceDef.equals(ServiceDef.WMS_1_1_1_SLD) || serviceDef.equals(ServiceDef.WMS_1_1_1)) {
            schemaLocation = "http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd";
        } else {
            schemaLocation = "http://www.opengis.net/ogc http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd";
        }
        final SchemaLocatedExceptionResponse response = new SchemaLocatedExceptionResponse(report, schemaLocation);
        final String mimeException = (serviceDef.version.equals(ServiceDef.WMS_1_1_1_SLD.version)) ? MimeType.APP_SE_XML : MimeType.TEXT_XML;
        return new ResponseObject(response, mimeException);
    }

    /**
     * Converts a DescribeLayer request composed of string values, to a container
     * of real java objects.
     *
     * @return The DescribeLayer request.
     * @throws CstlServiceException
     */
    private DescribeLayer adaptDescribeLayer(final Worker worker) throws CstlServiceException {
        String version = getParameter(VERSION_PARAMETER, false);
        if (version == null) {
            version = getParameter(KEY_WMTVER, false);
        }
        if (version == null) {
            throw new CstlServiceException("The parameter version must be specified",
                MISSING_PARAMETER_VALUE, "version");
        }
        ServiceDef serviceDef = worker.getVersionFromNumber(version);
        if (serviceDef == null) {
            serviceDef = worker.getBestVersion(null);
        }
        worker.checkVersionSupported(version, false);
        final String strLayer  = getParameter(KEY_LAYERS,  true);
        final List<String> layers = StringUtilities.toStringList(strLayer);
        return new DescribeLayer(layers, serviceDef.version);
    }

    /**
     * Converts a GetCapabilities request composed of string values, to a container
     * of real java objects.
     *
     * @return A GetCapabilities request.
     * @throws CstlServiceException
     */
    private GetCapabilities adaptGetCapabilities(final String request, final Worker worker) throws CstlServiceException {
        String version;
        if (CAPABILITIES.equalsIgnoreCase(request)) {
            version =  ServiceDef.WMS_1_1_1_SLD.version.toString();
        } else {
            version = getParameter(VERSION_PARAMETER, false);
            if (version == null) {
                // For backward compatibility with WMS 1.0.0, we try to find the version number
                // from the WMTVER parameter too.
                version = getParameter(KEY_WMTVER, false);
            }
        }
        final String service = getParameter(SERVICE_PARAMETER, true);
        if (!ServiceDef.Specification.WMS.toString().equalsIgnoreCase(service)) {
            throw new CstlServiceException("Invalid service specified. Should be WMS.",
                    INVALID_PARAMETER_VALUE, SERVICE_PARAMETER.toLowerCase());
        }
        final String language = getParameter(KEY_LANGUAGE, false);
        if (version == null) {
            final ServiceDef capsService = worker.getBestVersion(null);
            String format = getParameter(KEY_FORMAT, false);
            // Verify that the format is not null, and is not something totally different from the known
            // output formats. If it is the case, choose the default output format according to the version.
            if (format == null || format.isEmpty() ||
                    (!format.equalsIgnoreCase(MimeType.APP_XML) && !format.equalsIgnoreCase(MimeType.APPLICATION_XML)
                  && !format.equalsIgnoreCase(MimeType.TEXT_XML) && !format.equalsIgnoreCase(MimeType.APP_WMS_XML)))
            {
                format = (ServiceDef.WMS_1_1_1_SLD.version.equals(capsService.version)) ?
                    MimeType.APP_WMS_XML : MimeType.TEXT_XML;
            }
            return new GetCapabilities(capsService.version, format, language);
        }
        final ServiceDef bestVersion = worker.getBestVersion(version);
        String format = getParameter(KEY_FORMAT, false);
        // Verify that the format is not null, and is not something totally different from the known
        // output formats. If it is the case, choose the default output format according to the version.
        if (format == null || format.isEmpty() ||
                (!format.equalsIgnoreCase(MimeType.APP_XML) && !format.equalsIgnoreCase(MimeType.APPLICATION_XML)
              && !format.equalsIgnoreCase(MimeType.TEXT_XML) && !format.equalsIgnoreCase(MimeType.APP_WMS_XML)))
        {
            format = (ServiceDef.WMS_1_1_1_SLD.version.equals(bestVersion.version)) ?
                     MimeType.APP_WMS_XML : MimeType.TEXT_XML;
        }
        final String updateSequence = getParameter(UPDATESEQUENCE_PARAMETER, false);

        return new GetCapabilities(bestVersion.version, format, language, updateSequence);
    }

    /**
     * Converts a GetFeatureInfo request composed of string values, to a container
     * of real java objects.
     *
     * @return A GetFeatureInfo request.
     * @throws CstlServiceException
     */
    private GetFeatureInfo adaptGetFeatureInfo(final QueryContext queryContext, final Worker worker) throws CstlServiceException, NumberFormatException {
        final GetMap getMap  = adaptGetMap(false, queryContext, worker);

        String version = getParameter(VERSION_PARAMETER, false);
        if (version == null) {
            version = getParameter(KEY_WMTVER, false);
        }
        if (version == null) {
            throw new CstlServiceException("The parameter version must be specified",
                MISSING_PARAMETER_VALUE, "version");
        }

        final boolean strict = Application.getBooleanProperty(AppProperty.EXA_WMS_NO_MS, true);

        final String xKey = version.equals(ServiceDef.WMS_1_1_1_SLD.version.toString()) ? KEY_I_V111 : KEY_I_V130;
        final List<String> xKeys = strict ? Collections.EMPTY_LIST : X_KEYS;

        final String yKey = version.equals(ServiceDef.WMS_1_1_1_SLD.version.toString()) ? KEY_J_V111 : KEY_J_V130;
        final List<String> yKeys = strict ? Collections.EMPTY_LIST : Y_KEYS;

        final String strX    = getParameter(xKey, xKeys, true);
        final String strY    = getParameter(yKey, yKeys, true);
        final String strQueryLayers = getParameter(KEY_QUERY_LAYERS, true);
              String infoFormat  = getParameter(KEY_INFO_FORMAT, false);
        final String strFeatureCount = getParameter(KEY_FEATURE_COUNT, false);
        final List<String> namedQueryableLayers = StringUtilities.toStringList(strQueryLayers);
        if (infoFormat == null) {
            infoFormat = MimeType.TEXT_XML;
        }
        final String pNameStr = getParameter(KEY_PROPERTYNAME, false);
        final List<List<String>> propertyNames = pNameStr != null ? Util.parseMultipleList(pNameStr) : Collections.EMPTY_LIST;
        getMap.getParameters().put(KEY_PROPERTYNAME, propertyNames);

        final int x, y;
        try {
            x = RequestsUtilities.toInt(strX);
        } catch (NumberFormatException ex) {
            throw new CstlServiceException("Integer value waited. " + ex.getMessage(), ex, INVALID_POINT, xKey);
        }
        try {
            y = RequestsUtilities.toInt(strY);
        } catch (NumberFormatException ex) {
            throw new CstlServiceException("Integer value waited. " + ex.getMessage(), ex, INVALID_POINT, yKey);
        }
        final Integer featureCount;
        if (strFeatureCount == null || strFeatureCount.isEmpty()) {
            featureCount = 1;
        } else {
            featureCount = RequestsUtilities.toInt(strFeatureCount);
        }
        return new GetFeatureInfo(getMap, x, y, namedQueryableLayers, infoFormat, featureCount);
    }

    /**
     * Converts a GetLegendGraphic request composed of string values, to a container
     * of real java objects.
     *
     * @return The GetLegendGraphic request.
     * @throws CstlServiceException
     */
    private GetLegendGraphic adaptGetLegendGraphic() throws CstlServiceException {
        final String strLayer  = getParameter(KEY_LAYER,  true);
        final String strFormat = getParameter(KEY_FORMAT, true);
        // Verify that the format is known, otherwise returns an exception.
        final String format;
        try {
            // special examind extra format
            if (MimeType.APP_JSON.equals(strFormat)) {
                format = strFormat;
            } else {
                format = RequestsUtilities.toFormat(strFormat);
            }
        } catch (IllegalArgumentException i) {
            throw new CstlServiceException(i, INVALID_FORMAT);
        }

        final String strWidth  = getParameter(KEY_WIDTH,  false);
        final String strHeight = getParameter(KEY_HEIGHT, false);
        final Integer width;
        final Integer height;
        if (strWidth == null || strHeight == null) {
            width  = null;
            height = null;
        } else {
            try {
                width  = RequestsUtilities.toInt(strWidth);
            } catch (NumberFormatException n) {
                throw new CstlServiceException(n, INVALID_PARAMETER_VALUE, KEY_WIDTH.toLowerCase());
            }
            try {
                height = RequestsUtilities.toInt(strHeight);
            } catch (NumberFormatException n) {
                throw new CstlServiceException(n, INVALID_PARAMETER_VALUE, KEY_HEIGHT.toLowerCase());
            }
        }

        final String strStyle   = getParameter(KEY_STYLE,       false);
        final String strSld     = getParameter(KEY_SLD,         false);
        final String strSldVers = getParameter(KEY_SLD_VERSION, (strSld != null));
        final String strRule    = getParameter(KEY_RULE,        false);
        final StyledLayerDescriptor sldVersion;
        if (strSldVers == null) {
            sldVersion = null;
        } else if (strSldVers.equalsIgnoreCase("1.0.0")) {
            sldVersion = StyledLayerDescriptor.V_1_0_0;
        } else if (strSldVers.equalsIgnoreCase("1.1.0")) {
            sldVersion = StyledLayerDescriptor.V_1_1_0;
        } else {
            throw new CstlServiceException("The given sld version number "+ strSldVers +" is not known.",
                    INVALID_PARAMETER_VALUE, KEY_SLD_VERSION.toLowerCase());
        }
        final String strScale   = getParameter(KEY_SCALE,       false);
        final Double scale = RequestsUtilities.toDouble(strScale);
        return new GetLegendGraphic(strLayer, format, width, height, strStyle, strSld, sldVersion, strRule, scale, ServiceDef.WMS_1_1_1_SLD.version);
    }

    private boolean isV111orUnder(String version) {
        return version.equals(ServiceDef.WMS_1_0_0.version.toString())     ||
               version.equals(ServiceDef.WMS_1_0_0_SLD.version.toString()) ||
               version.equals(ServiceDef.WMS_1_1_1.version.toString())     ||
               version.equals(ServiceDef.WMS_1_1_1_SLD.version.toString());
    }

    /**
     * Converts a GetMap request composed of string values, to a container of real
     * java objects.
     *
     * @param fromGetMap {@code true} if the request is done for a GetMap, {@code false}
     *                   otherwise (in the case of a GetFeatureInfo for example).
     * @return The GetMap request.
     * @throws CstlServiceException
     */
    private GetMap adaptGetMap(final boolean fromGetMap, final QueryContext queryContext, final Worker w) throws CstlServiceException {
        String version = getParameter(VERSION_PARAMETER, false);
        if (version == null) {
            version = getParameter(KEY_WMTVER, false);
        }
        if (version == null) {
            throw new CstlServiceException("The parameter version must be specified", MISSING_PARAMETER_VALUE, "version");
        }
        w.checkVersionSupported(version, false);

        final String strExceptions   = getParameter(WMSConstant.KEY_EXCEPTIONS,     false);
        /*
         * we verify that the exception format is an allowed value
         */
        if (ServiceDef.WMS_1_3_0_SLD.version.toString().equals(version)) {
            if (strExceptions != null && !WMSConstant.EXCEPTION_130.contains(strExceptions)) {
                throw new CstlServiceException("exception format:" + strExceptions + " is not allowed. Use XML, INIMAGE or BLANK", INVALID_PARAMETER_VALUE);
            }
        } else {
            if (strExceptions != null && !WMSConstant.EXCEPTION_111.contains(strExceptions)) {
                throw new CstlServiceException("exception format:" + strExceptions + " is not allowed. Use application/vnd.ogc.se_xml, application/vnd.ogc.se_inimage or application/vnd.ogc.se_blank", INVALID_PARAMETER_VALUE);
            }
        }
        if (strExceptions != null && (strExceptions.equalsIgnoreCase(MimeType.APP_INIMAGE) || strExceptions.equalsIgnoreCase("INIMAGE"))) {
            queryContext.setErrorInimage(true);
        }
        final String strFormat       = getParameter(KEY_FORMAT,    fromGetMap);
        if (strFormat != null && !strFormat.isEmpty()) {
            // Ensures that the format specified is known, to use it as the format of the
            // image which will contain the exception.
            if (DefaultPortrayalService.isImageFormat(strFormat) ||
                DefaultPortrayalService.isPresentationFormat(strFormat))
            {
                queryContext.setExceptionImageFormat(strFormat);
            }
        }

        final boolean strict = Application.getBooleanProperty(AppProperty.EXA_WMS_STRICT, true);

        final String crsKey = version.equals(ServiceDef.WMS_1_1_1_SLD.version.toString()) ? KEY_CRS_V111 : KEY_CRS_V130;
        final List<String> altCrsKeys = strict ? Collections.EMPTY_LIST : CRS_KEYS;
        
        final String strCRS          = getParameter(crsKey, altCrsKeys,  true);
        final String strBBox         = getParameter(KEY_BBOX,            true);
        final String strLayers       = getParameter(KEY_LAYERS,          true);
        final String strWidth        = getParameter(KEY_WIDTH,           true);
        final String strHeight       = getParameter(KEY_HEIGHT,          true);
        final String strElevation    = getParameter(KEY_ELEVATION,      false);
        final String strTime         = getParameter(KEY_TIME,           false);
        final String strBGColor      = getParameter(KEY_BGCOLOR,        false);
        final String strTransparent  = getParameter(KEY_TRANSPARENT,    false);
        //final String strRemoteOwsType = getParameter(KEY_REMOTE_OWS_TYPE, false);
        final String strRemoteOwsUrl = getParameter(KEY_REMOTE_OWS_URL, false);
        final String urlSLD          = getParameter(KEY_SLD,            false);
        final String bodySLD         = getParameter(KEY_SLD_BODY,       false);
        final String strSldVersion   = getParameter(KEY_SLD_VERSION, (urlSLD != null));
        final String strAzimuth      = getParameter(KEY_AZIMUTH,        false);
        final String strStyles       = getParameter(KEY_STYLES, ((urlSLD != null)) ? false : fromGetMap);

        CoordinateReferenceSystem crs;
        boolean forceLongitudeFirst = false;
        try {
            if (isV111orUnder(version)) {
                /*
                 * If we are in version older than WMS 1.3.0, then the bounding box is
                 * expressed with the longitude in first, even if the CRS has the latitude as
                 * first axis. Consequently we have to force the longitude in first for the
                 * CRS decoding.
                 */
                forceLongitudeFirst = true;
            }
            crs = CRS.forCode(strCRS);
            if (forceLongitudeFirst) {
                crs = AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);
            }
        } catch (FactoryException ex) {
            if (isV111orUnder(version)) {
                throw new CstlServiceException(ex, org.constellation.ws.ExceptionCode.INVALID_SRS);
            } else {
                throw new CstlServiceException(ex, INVALID_CRS);
            }
        }
        final Envelope env;
        try {
            env = toEnvelope(strBBox, crs);
        } catch (IllegalArgumentException i) {
            throw new CstlServiceException(i, INVALID_PARAMETER_VALUE);
        }
        final String format = strFormat;
        if (fromGetMap && format == null) {
            throw new CstlServiceException("Invalid format specified.", INVALID_FORMAT, KEY_FORMAT.toLowerCase());
        }
        final List<String> layers  = StringUtilities.toStringList(strLayers);
        final List<String> styles = StringUtilities.toStringList(strStyles);
        final Double elevation;
        try {
            elevation = (strElevation != null) ? RequestsUtilities.toDouble(strElevation) : null;
        } catch (NumberFormatException n) {
            throw new CstlServiceException(n, INVALID_PARAMETER_VALUE, KEY_ELEVATION.toLowerCase());
        }
        final ArrayList<Date> dates = new ArrayList<>();
        try {
            TimeParser.parse(strTime, 0l, dates, true);
        } catch (ParseException ex) {
            throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, KEY_TIME.toLowerCase());
        }
        final int width;
        final int height;
        try {
            width  = RequestsUtilities.toInt(strWidth);
            height = RequestsUtilities.toInt(strHeight);
        } catch (NumberFormatException n) {
            throw new CstlServiceException(n, INVALID_DIMENSION_VALUE);
        }
        if (width < 1) {
            throw new CstlServiceException("Width must be a positive number", INVALID_DIMENSION_VALUE);
        }
        if (height < 1) {
            throw new CstlServiceException("Height must be a positive number", INVALID_DIMENSION_VALUE);
        }
        final Dimension size = new Dimension(width, height);
        final Color background = RequestsUtilities.toColor(strBGColor);
        final boolean transparent = RequestsUtilities.toBoolean(strTransparent);
        queryContext.setOpaque(!transparent);
        org.geotoolkit.sld.StyledLayerDescriptor sld = null;
        if (strRemoteOwsUrl != null) {
            try {
                Path strRemoteOwsPath = IOUtilities.toPath(strRemoteOwsUrl);
                sld = styleBusiness.readSLD(strRemoteOwsPath, true);

            } catch (IOException | ConstellationException ex) {
                throw new CstlServiceException(ex, STYLE_NOT_DEFINED);
            }
        } else if (bodySLD != null || urlSLD != null){
            try {
                // used to verify that the version is supported
                StyledLayerDescriptor.version(strSldVersion);
                Object src = urlSLD != null ? new URL(urlSLD) : bodySLD;
                sld = styleBusiness.readSLD(src, strSldVersion);
            } catch (IllegalArgumentException ex) {
                throw new CstlServiceException("The given sld version " + strSldVersion + " is not known.", INVALID_PARAMETER_VALUE, KEY_SLD_VERSION.toLowerCase());
            } catch (ConstellationException | MalformedURLException ex) {
                throw new CstlServiceException(ex, STYLE_NOT_DEFINED);
            }
        }
        Map<String, Object> extraParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        extraParameters.putAll(getParameters());

        boolean hasCQL = false;
        final String cqlStr = getParameter(KEY_CQL_FILTER, false);
        if (cqlStr != null) {
            extraParameters.put(KEY_CQL_FILTER, StringUtilities.toStringList(cqlStr, ';'));
            hasCQL = true;
        }

        boolean hasFilter = false;
        final List<Object> xmlFiltersObj  = getComplexParameterList(KEY_FILTER, false);
        if (!xmlFiltersObj.isEmpty()) {
            final List<Filter> xmlFilters = new ArrayList<>();
            for (Object xmlFilter : xmlFiltersObj) {
                if (xmlFilter instanceof Filter f) {
                    xmlFilters.add(f);
                } else {
                    throw new CstlServiceException("FILTER parameter must contains OGC filter.", INVALID_PARAMETER_VALUE, KEY_FILTER);
                }
            }
            extraParameters.put(KEY_FILTER, xmlFilters);
            hasFilter = true;
        }

        if (hasCQL && hasFilter) {
            throw new CstlServiceException("You can't specify both CQL_FILTER and FILTER.",INVALID_PARAMETER_VALUE);
        }

        final double azimuth;
        try {
            azimuth = (strAzimuth == null) ? 0.0 : RequestsUtilities.toDouble(strAzimuth);
        } catch(NumberFormatException ex) {
            throw new CstlServiceException(ex, INVALID_PARAMETER_VALUE, KEY_AZIMUTH.toLowerCase());
        }

        // Builds the request.
        return new GetMap(env, new Version(version), format, layers, styles, sld, elevation,
                    dates, size, background, transparent, azimuth, strExceptions, extraParameters);
    }

    /**
     * This copy contains a <em>workaround</em> to handle wrap-adjustment in the specific case of <em>Mercator projection</em>.
     * Note: the code produced for this fix is <em>not</em> generic, and might cause errors if applied on projections other than mercator.
     *
     * Converts a string representing the bbox coordinates into a {@link GeneralEnvelope}.
     *
     * @param bbox Coordinates of the bounding box, seperated by comas.
     * @param crs  The {@linkplain CoordinateReferenceSystem coordinate reference system} in
     *             which the envelope is expressed. Must not be {@code null}.
     * @return The enveloppe for the bounding box specified, or an
     *         {@linkplain GeneralEnvelope#setToInfinite infinite envelope}
     *         if the bbox is {@code null}.
     * @throws IllegalArgumentException if the given CRS is {@code null}, or if the bbox string
     *                                  contains too much parameters to fill the CRS ranges.
     */
    @Deprecated
    public static Envelope toEnvelope(final String bbox, CoordinateReferenceSystem crs)
                                                              throws IllegalArgumentException
    {
        if (crs == null) {
            throw new IllegalArgumentException("The CRS must not be null");
        }
        if (bbox == null) {
            final GeneralEnvelope infinite = new GeneralEnvelope(crs);
            infinite.setToInfinite();
            return infinite;
        }

        final GeneralEnvelope envelope = new GeneralEnvelope(crs);
        final int dimension = envelope.getDimension();
        final StringTokenizer tokens = new StringTokenizer(bbox, ",;");
        final double[] coordinates = new double[dimension * 2];

        int index = 0;
        while (tokens.hasMoreTokens()) {
            final double value = toDouble(tokens.nextToken());
            if (index >= coordinates.length) {
                throw new IllegalArgumentException(
                        Errors.format(Errors.Keys.IllegalCsDimension_1, coordinates.length));
            }
            coordinates[index++] = value;
        }
        if ((index & 1) != 0) {
            throw new IllegalArgumentException(
                    Errors.format(Errors.Keys.OddArrayLength_1, index));
        }

        boolean inverted = coordinates[0] > coordinates[2];

        if (inverted && (crs instanceof ProjectedCRS || crs instanceof GeographicCRS)) {
            try {
                CoordinateReferenceSystem target = crs;
                if (crs instanceof ProjectedCRS pcrs) {
                    target = pcrs.getBaseCRS();
                }
                target = AbstractCRS.castOrCopy(target).forConvention(AxesConvention.NORMALIZED);
                MathTransform tr = CRS.findOperation(crs, target,null).getMathTransform();
                tr.transform(coordinates, 0, coordinates, 0, 2);
                crs = target;
                envelope.setCoordinateReferenceSystem(crs);
            } catch (TransformException | FactoryException ex) {
                throw new IllegalArgumentException(ex);
            }
            double c = 360;
            //c *= Math.PI * 2;
            if (Math.abs(coordinates[0]) < Math.abs(coordinates[2])) {
                coordinates[0] -= c;
            } else {
                coordinates[2] += c;
            }
        }

        // Fallthrough in every cases.
        switch (index) {
            default: {
                while (index >= 6) {
                    final double maximum = coordinates[--index];
                    final double minimum = coordinates[--index];
                    envelope.setRange(index >> 1, minimum, maximum);
                }
            }
            case 4: envelope.setRange(1, coordinates[1], coordinates[3]);
            case 3:
            case 2: envelope.setRange(0, coordinates[0], coordinates[2]);
            case 1:
            case 0: break;
        }
        /*
         * Checks the envelope validity. Given that the parameter order in the bounding box
         * is a little-bit counter-intuitive, it is worth to perform this check in order to
         * avoid a NonInvertibleTransformException at some later stage.

        for (index = 0; index < dimension; index++) {
            final double minimum = envelope.getMinimum(index);
            final double maximum = envelope.getMaximum(index);
            if (!(minimum < maximum)) {
                throw new IllegalArgumentException(
                        Errors.format(Errors.Keys.IllegalRange_2, minimum, maximum));
            }
        }*/
        return envelope;
    }
}
