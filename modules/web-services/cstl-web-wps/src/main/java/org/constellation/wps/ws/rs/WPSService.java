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
package org.constellation.wps.ws.rs;

import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.api.ServiceDef;
import org.constellation.api.ServiceDef.Specification;
import com.examind.wps.api.WPSWorker;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.ows.xml.BoundingBox;
import org.geotoolkit.ows.xml.ExceptionResponse;
import org.geotoolkit.ows.xml.v200.AcceptVersionsType;
import org.geotoolkit.wps.json.ProcessCollection;
import org.geotoolkit.wps.xml.v200.Format;
import org.geotoolkit.wps.xml.v200.ProcessOfferings;
import org.geotoolkit.wps.xml.v200.StatusInfo;
import org.geotoolkit.wps.xml.v200.Capabilities;
import org.geotoolkit.wps.xml.v200.Result;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.ows.xml.v200.AcceptFormatsType;
import org.geotoolkit.ows.xml.v200.GetCapabilitiesType;
import org.geotoolkit.wps.xml.v200.Contents;
import org.geotoolkit.wps.xml.v200.DataTransmissionMode;
import org.geotoolkit.wps.xml.v200.Reference;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.DescribeProcess;
import org.geotoolkit.wps.xml.v200.GetResult;
import org.geotoolkit.wps.xml.v200.GetStatus;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.GetCapabilities;
import org.geotoolkit.atom.xml.Link;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import static org.constellation.api.QueryConstants.ACCEPT_FORMATS_PARAMETER;

import static org.constellation.api.QueryConstants.ACCEPT_VERSIONS_PARAMETER;
import static org.constellation.api.QueryConstants.REQUEST_PARAMETER;
import static org.constellation.api.QueryConstants.SERVICE_PARAMETER;
import static org.constellation.api.QueryConstants.UPDATESEQUENCE_PARAMETER;
import static org.constellation.api.QueryConstants.VERSION_PARAMETER;
import static org.constellation.api.QueryConstants.SECTIONS_PARAMETER;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_FORMAT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import com.examind.wps.util.WPSUtils;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.constellation.ws.rs.ResponseObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import static com.examind.wps.util.WPSConstants.*;
import java.io.IOException;
import java.util.Set;
import org.constellation.util.Util;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ows.xml.OWSXmlFactory;
import org.geotoolkit.ows.xml.v200.BoundingBoxType;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.ows.xml.v200.SectionsType;
import org.geotoolkit.wps.json.ExceptionReportType;
import org.geotoolkit.wps.json.JobCollection;
import org.geotoolkit.wps.json.JsonLink;
import org.geotoolkit.wps.json.LandingPage;
import org.geotoolkit.wps.json.OutputInfo;
import org.geotoolkit.wps.json.ReqClasses;
import org.geotoolkit.wps.xml.v200.Bill;
import org.geotoolkit.wps.xml.v200.BillList;
import org.geotoolkit.wps.xml.v200.Data;
import org.geotoolkit.wps.xml.v200.DataOutput;
import org.geotoolkit.wps.xml.v200.Deploy;
import org.geotoolkit.wps.xml.v200.DeployResult;
import org.geotoolkit.wps.xml.v200.Dismiss;
import org.geotoolkit.wps.xml.v200.Execute.Response;
import org.geotoolkit.wps.xml.v200.LiteralValue;
import org.geotoolkit.wps.xml.v200.Quotation;
import org.geotoolkit.wps.xml.v200.QuotationList;
import org.geotoolkit.wps.xml.v200.Undeploy;
import org.geotoolkit.wps.xml.v200.UndeployResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * WPS web service class.
 *
 * @author Quentin Boileau (Geomatys).
 * @author Benjamin Garcia (Geomatys).
 *
 * @version 0.9
 */
@Controller
@RequestMapping("wps/{serviceId}")
public class WPSService extends OGCWebService<WPSWorker> {

    /**
     * The default CRS to apply on a bounding box when no CRS are provided with
     * a GET request using the execute method
     */
    private static final CoordinateReferenceSystem DEFAULT_CRS = CommonCRS.WGS84.normalizedGeographic();


    /**
     * Build a new instance of the webService and initialize the JAXB context.
     */
    public WPSService() {
        super(Specification.WPS);
        setXMLContext(WPSMarshallerPool.getInstance());
        LOGGER.log(Level.INFO, "WPS REST service running");
    }

    @Override
    protected ResponseObject treatIncomingRequest(final Object objectRequest, final WPSWorker worker) {
        ServiceDef version = null;
        String requestName = null;
        try {
            // Handle an empty request by sending a basic web page.
            if ((null == objectRequest) && isIndexPageRequest()) {
                return getIndexPage(worker.getId());
            }

            // if the request is not an xml request we fill the request parameter.
            final RequestBase request;
            if (objectRequest == null) {
                //build objectRequest from parameters
                version = worker.getVersionFromNumber(getParameter(VERSION_PARAMETER, false)); // needed if exception is launch before request build
                requestName = getParameter(REQUEST_PARAMETER, true);
                request = adaptQuery(requestName, worker);
            } else if (objectRequest instanceof RequestBase) {
                request = (RequestBase) objectRequest;
            } else {
                throw new CstlServiceException("The operation " + objectRequest.getClass().getName() + " is not supported by the service",
                        OPERATION_NOT_SUPPORTED, objectRequest.getClass().getName());
            }

            version = worker.getVersionFromNumber(request.getVersion());

            /*
             * GetCapabilities request
             */
            if (request instanceof GetCapabilities) {
                final GetCapabilities getcaps = (GetCapabilities) request;
                String outputFormat = getcaps.getFirstAcceptFormat();
                if (outputFormat == null) {
                    outputFormat = MimeType.TEXT_XML;
                }
                return new ResponseObject(worker.getCapabilities(getcaps), outputFormat);
            }

            /*
             * DescribeProcess request
             */
            if (request instanceof DescribeProcess) {
                final DescribeProcess descProc = (DescribeProcess) request;
                return new ResponseObject(worker.describeProcess(descProc), MediaType.TEXT_XML);
            }

            /*
             * GetStatus request
             */
            if (request instanceof GetStatus) {
                final GetStatus gs = (GetStatus) request;
                return new ResponseObject(worker.getStatus(gs), MediaType.TEXT_XML);
            }

            /*
             * GetResult request
             */
            if (request instanceof GetResult) {
                final GetResult gs = (GetResult) request;
                final Object grResponse = worker.getResult(gs);

                boolean isTextPlain = false;
                boolean isImage = false;
                //if response is a literal
                if (grResponse instanceof String  || grResponse instanceof Double
                 || grResponse instanceof Float   || grResponse instanceof Integer
                 || grResponse instanceof Boolean || grResponse instanceof Long) {
                    isTextPlain = true;
                }
                if (grResponse instanceof RenderedImage || grResponse instanceof BufferedImage
                        || grResponse instanceof GridCoverage2D) {
                    isImage = true;
                }
                if (isTextPlain)  {
                    return new ResponseObject(grResponse.toString(), MediaType.TEXT_PLAIN);
                } else if (isImage) {
                    return new ResponseObject(grResponse.toString(), MediaType.IMAGE_PNG);
                } else {
                    // TODO : how to determine the good mime type???
                    String mimeType = MimeType.TEXT_XML;
                    return new ResponseObject(grResponse, mimeType);
                }
            }

            /*
             * Dismiss request
             */
            if (request instanceof Dismiss) {
                final Dismiss gs = (Dismiss) request;
                return new ResponseObject(worker.dismiss(gs), MediaType.TEXT_XML);
            }

            /*
             * Execute request
             */
            if (request instanceof Execute) {
                final Execute exec = (Execute) request;
                final Object executeResponse = worker.execute(exec);

                boolean isTextPlain = false;
                boolean isImage = false;
                //if response is a literal
                if (executeResponse instanceof String  || executeResponse instanceof Double
                 || executeResponse instanceof Float   || executeResponse instanceof Integer
                 || executeResponse instanceof Boolean || executeResponse instanceof Long) {
                    isTextPlain = true;
                }
                if (executeResponse instanceof RenderedImage || executeResponse instanceof BufferedImage
                        || executeResponse instanceof GridCoverage2D) {
                    isImage = true;
                }
                if (isTextPlain)  {
                    return new ResponseObject(executeResponse.toString(), MediaType.TEXT_PLAIN);
                } else if (isImage) {
                    return new ResponseObject(executeResponse.toString(), MediaType.IMAGE_PNG);
                } else {
                    String mimeType;
                    // extract raw mimeType
                    if (exec.isRawOutput() && !exec.getOutput().isEmpty() && exec.getOutput().get(0).getMimeType() != null) {
                        mimeType = exec.getOutput().get(0).getMimeType();
                    } else {
                        mimeType = MimeType.TEXT_XML;
                    }
                    return new ResponseObject(executeResponse, mimeType);
                }

            }

            throw new CstlServiceException("This service can not handle the requested operation: " + request.getClass().getName() + ".",
                    OPERATION_NOT_SUPPORTED, requestName);

        } catch (CstlServiceException ex) {
            /*
             * This block handles all the exceptions which have been generated anywhere in the service and transforms them to a response
             * message for the protocol stream which JAXB, in this case, will then marshall and serialize into an XML message HTTP response.
             */
            return processExceptionResponse(ex, version, worker);

        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject processExceptionResponse(final CstlServiceException ex, ServiceDef serviceDef, final Worker worker) {
        logException(ex);

        // SEND THE HTTP RESPONSE
        if (serviceDef == null) {
            serviceDef = ServiceDef.WPS_1_0_0;
        }
        final String exceptionCode   = getOWSExceptionCodeRepresentation(ex.getExceptionCode());
        final ExceptionResponse report = OWSXmlFactory.buildExceptionReport(serviceDef.exceptionVersion.toString(), ex.getMessage(), exceptionCode, ex.getLocator(),
                                                     serviceDef.exceptionVersion.toString());
        return new ResponseObject(report, MediaType.TEXT_XML);
    }

    /**
     * Handle GET request in KVP.
     *
     * @param request
     * @return GetCapabilities or DescribeProcess or Execute object.
     * @throws CstlServiceException if request is unknow.
     */
    public RequestBase adaptQuery(final String request, final Worker w) throws CstlServiceException {

        if (GETCAPABILITIES.equalsIgnoreCase(request)) {
            return adaptKvpGetCapabilitiesRequest(w);
        } else if (DESCRIBEPROCESS.equalsIgnoreCase(request)) {
            return adaptKvpDescribeProcessRequest(w);
        } else if (EXECUTE.equalsIgnoreCase(request)) {
            return adaptKvpExecuteRequest();
        } else if (GETSTATUS.equalsIgnoreCase(request)) {
            return adaptKvpGetStatusRequest();
        } else if (GETRESULT.equalsIgnoreCase(request)) {
            return adaptKvpGetResultRequest();
        } else if (DISMISS.equalsIgnoreCase(request)) {
            return adaptKvpDismissRequest();
        }
        throw new CstlServiceException("The operation " + request + " is not supported by the service",
                OPERATION_NOT_SUPPORTED, request);
    }

    /**
     * Create GetCapabilities object from kvp parameters.
     *
     * @return GetCapabilities object.
     * @throws CstlServiceException
     */
    private GetCapabilities adaptKvpGetCapabilitiesRequest(Worker worker) throws CstlServiceException {

        String service        = getParameter(SERVICE_PARAMETER, true);
        String language       = getParameter(LANGUAGE_PARAMETER, false);
        String updateSequence = getParameter(UPDATESEQUENCE_PARAMETER, false);
        String version        = getParameter(VERSION_PARAMETER, false);
        if (version == null) {
            version = worker.getBestVersion(null).version.toString();
        }
        worker.checkVersionSupported(version, true);


        final AcceptVersionsType versions;
        final String acceptVersionsParam = getParameter(ACCEPT_VERSIONS_PARAMETER, false);
        if(acceptVersionsParam!= null){
            final String[] acceptVersions = acceptVersionsParam.split(",");
            versions = new AcceptVersionsType(acceptVersions);
        } else {
            versions = new AcceptVersionsType(new String[]{version});
        }
        AcceptFormatsType formats = null;
        final String acceptFormatsParam = getParameter(ACCEPT_FORMATS_PARAMETER, false);
        if(acceptFormatsParam!= null){
            final String[] acceptFormats = acceptFormatsParam.split(",");
            formats = new AcceptFormatsType(acceptFormats);
        }
        SectionsType sections = null;
        final String sectionsParam = getParameter(SECTIONS_PARAMETER, false);
        if (sectionsParam != null) {
            final String[] acceptSections = sectionsParam.split(",");
            sections = new SectionsType(Arrays.asList(acceptSections));
        }
        final GetCapabilitiesType.AcceptLanguages languages = new GetCapabilitiesType.AcceptLanguages(language);
        return new GetCapabilities(versions, sections, formats, updateSequence, service, languages);
    }

    /**
     * Create DescribeProcess object from kvp parameters.
     *
     * @return DescribeProcess object.
     * @throws CstlServiceException if mandatory parameters are missing.
     */
    private DescribeProcess adaptKvpDescribeProcessRequest(final Worker w) throws CstlServiceException {

        final String version = getParameter(VERSION_PARAMETER, true);
        w.checkVersionSupported(version, false);

        String service = getParameter(SERVICE_PARAMETER, true);
        // WPS 2 specify Language parameter should be lang. See table 51: "DescribeProcess request KVP encoding"
        String language = getParameter("lang", false);
        if (language == null) {
            // wps 1 retro-compatibility
            language = getParameter(LANGUAGE_PARAMETER, false);
        }

        final String allIdentifiers = getParameter(IDENTIFIER_PARAMETER, true);
        if (allIdentifiers != null) {
            final String[] splitStr = allIdentifiers.split(",");
            final List<CodeType> identifiers = new ArrayList<>();
            for (String id : splitStr) {
                identifiers.add(new CodeType(id));
            }
            return new DescribeProcess(service, version, language, identifiers);
        } else {
            throw new CstlServiceException("The parameter " + IDENTIFIER_PARAMETER + " must be specified.",
                    MISSING_PARAMETER_VALUE, IDENTIFIER_PARAMETER.toLowerCase());
        }
    }

    /**
     * Create Execute object from kvp parameters.
     *
     * @return Execute object.
     * @throws CstlServiceException
     */
    private Execute adaptKvpExecuteRequest() throws CstlServiceException {
        final String version = getParameter(VERSION_PARAMETER, true);
        final String service = getParameter(SERVICE_PARAMETER, true);
        final String identifier =  getParameter(IDENTIFIER_PARAMETER, true);
        final String language = getParameter(LANGUAGE_PARAMETER, true);
        final String dataInputs = getParameter(DATA_INPUTS_PARAMETER, false);
        final String respDoc = getParameter(RESPONSE_DOCUMENT_PARAMETER, false);
        final String respRawData = getParameter(RAW_DATA_OUTPUT_PARAMETER, false);
        final String lineage = getParameter(LINEAGE_PARAMETER, false);
        final String status = getParameter(STATUS_PARAMETER, false);
        final String storeExecuteResponse = getParameter(STORE_EXECUTE_RESPONSE_PARAMETER, false);

        if (ServiceDef.getServiceDefinition(service, version).equals(ServiceDef.WPS_1_0_0)) {

            // Check dataInputs nullity
            List<DataInput> inputs = null;
            if (dataInputs != null && !dataInputs.isEmpty()) {
                inputs = extractInput(version, identifier, dataInputs);
            }

            boolean statusB           = extractOutputParameter(status);
            boolean lineageB          = extractOutputParameter(lineage);
            boolean storeExecuteRespB = extractOutputParameter(storeExecuteResponse);

            Response response;
            boolean isRaw;
            List<OutputDefinition> outputs;
            if (respDoc != null && !respDoc.isEmpty()) {
                response = Response.document;
                Map<String, Map> inputMap = extractDataFromKvpString(respDoc);
                outputs = extractDocumentResponseForm(version, inputMap);

            } else if (respRawData != null && !respRawData.isEmpty()) {
                if (lineage != null || status != null || storeExecuteResponse != null) {
                    throw new CstlServiceException("lineage, status and storeExecuteResponse can not be set alongside a RawDataOutput");
                }
                response = Response.raw;
                Map<String, Map> inputMap = extractDataFromKvpString(respRawData);
                outputs = new ArrayList<>();
                outputs.add(extractRawResponseForm(version, inputMap));
            } else {
                outputs = new ArrayList<>();
                response = Response.document;
            }

            return new Execute(service, version, language, new CodeType(identifier), inputs, outputs, response, storeExecuteRespB, lineageB, statusB);

        } else if (ServiceDef.getServiceDefinition(service, version).equals(ServiceDef.WPS_2_0_0)) {
            throw new CstlServiceException("The execute request KVP is not yet available in WPS 2.0.0.");
        } else {
            throw new CstlServiceException("The version number specified for this request is not handled.");
        }
    }

    private Dismiss adaptKvpDismissRequest() throws CstlServiceException {
        final String version = getParameter(VERSION_PARAMETER, true);
        final String service = getParameter(SERVICE_PARAMETER, true);
        final String jobId =  getParameter(JOBID_PARAMETER, true);
        return new Dismiss(service, version, jobId);
    }

    private GetResult adaptKvpGetResultRequest() throws CstlServiceException {
        final String version = getParameter(VERSION_PARAMETER, true);
        final String service = getParameter(SERVICE_PARAMETER, true);
        final String jobId =  getParameter(JOBID_PARAMETER, true);
        return new GetResult(service, version, jobId);
    }

    private GetStatus adaptKvpGetStatusRequest() throws CstlServiceException {
        final String version = getParameter(VERSION_PARAMETER, true);
        final String service = getParameter(SERVICE_PARAMETER, true);
        final String jobId =  getParameter(JOBID_PARAMETER, true);
        return new GetStatus(service, version, jobId);
    }

    /**
     * Helper method that extracts a boolean from one of the following WPS GET
     * argument : lineage, status, storeExecuteResponse
     * @param parameter should be a string extracted using getParameter with one
     * of the following arguments : STATUS_PARAMETER, LINEAGE_PARAMETER, STATUS_PARAMETER
     * @return the value of the extracted boolean
     * @throws CstlServiceException if the extraced value is not a boolean
     */
    static boolean extractOutputParameter(String parameter) throws CstlServiceException {
        if (parameter == null)
            return false;

        Map<String, Map> inputMap = extractDataFromKvpString(parameter);

        // Since this method is used with three different parameters
        // which are expected to just contain a boolean the map must have exactly
        // one element.
        assert inputMap.keySet().size() == 1;
        String value = inputMap.keySet().iterator().next();

        if ("true".equalsIgnoreCase(value))
            return true;
        else if ("false".equalsIgnoreCase(value))
            return false;

        throw new CstlServiceException("Expected values for lineage, status and storeExecuteResponse are true or false, the current value is " + value);
    }

    /**
     * Helper method to detect wether a given input is a reference input or not
     *
     * Since a reference has a mandatory 'href' attribute the test consist in checking
     * if this value exists in the attributes map.
     *
     * @param attributesMap attributes map for a given input
     * @return true if a 'href' attribute is detected
     */
    static boolean detectReference(final Map<String, String> attributesMap) {
        return attributesMap.keySet().contains(HREF_PARAMETER.toLowerCase());
    }

    /**
     * Helper method to detect wether a given input is a bounding box or not.
     *
     * Detecting a bounding box is a little tricky in some cases.
     *
     * When the literal has no declared attribute and bounding box has no CRS
     * they can not be distinguished.
     *
     * eg :
     *  literal -> array=42,26,30,102
     *  bounding box -> bbox=104,16,27,83
     *
     * So the solution is to get the input's class type by using its
     * ParameterDescriptor through the WPSUtils.getClassFromIOIdentifier method.
     *
     * But BoundingBox has no attributes, so if there's an attributes map with more
     * than one key (because it contains always at least one key) it means that
     * we are not reading a bounding box
     *
     * @param processIdentifier identifier of the current process
     * @param inputIdentifier input's identifier which may be a bounding box
     * @param attributesMap attributes map for a given input
     * @return true if a bounding box is detected
     */
    static boolean detectBoundingBox(final String processIdentifier, final String inputIdentifier, final Map<String, String> attributesMap) throws CstlServiceException {
        if (attributesMap.keySet().size() > 1)
            return false;

        Class inputType;
        try {
            inputType = WPSUtils.getIOClassFromIdentifier(processIdentifier, inputIdentifier);
        }
        catch (ParameterNotFoundException ex) {
            throw new CstlServiceException("Can not found the input " + inputIdentifier + " in the process " + processIdentifier + "\n" + ex.getLocalizedMessage());
        }

        return inputType == org.opengis.geometry.Envelope.class;
    }

    /**
     * Parse the decoded arguments of a GET request
     * @param processIdentifier process identifier, useful to give hints to the detect bounding box method
     * @param dataInputs the decoded arguments
     * @return a DataInputsType containing all the inputs read from the GET request
     * and translated into WPS Object
     * @throws CstlServiceException when an unknown attribute read
     */
    static List<DataInput> extractInput(final String version, final String processIdentifier, final String dataInputs) throws CstlServiceException {
        ArgumentChecks.ensureNonEmpty("processIdentifier", processIdentifier);
        ArgumentChecks.ensureNonEmpty("dataInputs", dataInputs);

        List<DataInput> inputTypeList = new ArrayList<>();
        Map<String, Map> inputMap = extractDataFromKvpString(dataInputs);

        for (String inputIdentifier : inputMap.keySet()) {
            Map<String, String> attributesMap = inputMap.get(inputIdentifier);

            if (detectReference(attributesMap))
                inputTypeList.add(readReference(version, inputIdentifier, attributesMap));
            else if (detectBoundingBox(processIdentifier, inputIdentifier, attributesMap))
                inputTypeList.add(readBoundingBoxData(version, inputIdentifier, attributesMap));
            else
                inputTypeList.add(readLiteralData(version, inputIdentifier, attributesMap));
        }
        return inputTypeList;
    }

    /**
     * Read an input assuming it's a reference input and encapsulate it into an InputType
     * @param inputIdentifier input identifier of the current input being processed
     * @param attributesMap attributes map associated with the current input
     * @return an InputReferenceType encapsulated into an InputType
     * @throws CstlServiceException when an unknown attributes is read
     */
    static DataInput readReference(final String version, final String inputIdentifier, final Map<String, String> attributesMap) throws CstlServiceException {

        final Reference inputRef = new Reference(null, null, null);
        for (String key : attributesMap.keySet()) {
            String value = attributesMap.get(key);

            if (key.equalsIgnoreCase(MIME_TYPE_PARAMETER) || key.equalsIgnoreCase(FORMAT_PARAMETER))
                inputRef.setMimeType(value);
            else if (key.equalsIgnoreCase(ENCODING_PARAMETER))
                inputRef.setEncoding(value);
            else if (key.equalsIgnoreCase(SCHEMA_PARAMETER))
                inputRef.setSchema(value);
            else if (key.equalsIgnoreCase(HREF_PARAMETER))
                inputRef.setHref(value);
            else if (key.equalsIgnoreCase(METHOD_PARAMETER)         ||
                     key.equalsIgnoreCase(BODY_PARAMETER)           ||
                     key.equalsIgnoreCase(BODY_REFERENCE_PARAMETER) ||
                     key.equalsIgnoreCase(HEADER_PARAMETER))
                throw new CstlServiceException("The " + key + " attribute is not supported in a GET request");

            else if (!(key.equals(inputIdentifier) && value == null))
                throw new CstlServiceException("Trying to set an InputReference with the unknown attribute " + key + " (value : " + value + ")");
        }
        return new DataInput(inputIdentifier, inputRef);
    }

    /**
     * Read an input assuming it's a literal data and encapsulate it into an InputType
     * @param inputIdentifier input identifier of the current input being processed
     * @param attributesMap attributes of the current input
     * @return a LiteralDataType encapsulated into an InputType
     * @throws CstlServiceException when an unknown attribute is read
     */
    static DataInput readLiteralData(final String version, final String inputIdentifier, final Map<String, String> attributesMap) throws CstlServiceException {

        LiteralValue literalData = new LiteralValue();
        for (String key : attributesMap.keySet()) {
            String value = attributesMap.get(key);
            if (inputIdentifier.equals(key)) {
                literalData.setValue(value);
            }
            if (key.equalsIgnoreCase(DATA_TYPE_PARAMETER))
                literalData.setDataType(value);
            else if (key.equalsIgnoreCase(UOM_PARAMETER))
                literalData.setUom(value);
            else if (inputIdentifier.equals(key))
                literalData.setValue(value);
            else
                throw new CstlServiceException("Trying to set a LiteralData with the unknown attribute " + key + " (value : " + value + ")");
        }

        // Ensure the literal has a value
        if (literalData.getValue() == null || literalData.getValue().isEmpty()) {
            throw new CstlServiceException("No value given to " + inputIdentifier);
        }

        Data dataType = new Data(literalData);
        return new DataInput(inputIdentifier, dataType);
    }

    /**
     * Read an input assuming it's a bounding box
     * @param inputIdentifier identifier of the current input being processed
     * @param attributesMap attributes of the current input
     * @return a BoundingBoxType encapsulated into an InputType
     */
    static DataInput readBoundingBoxData(final String version, final String inputIdentifier, final Map<String, String> attributesMap) throws CstlServiceException {

        // A bounding box input has no attributes
        // So the only key in the attributesMap is equals to inputIdentifier
        // and its value is the bounding box string to parse
        assert attributesMap.size() == 1;

        String bboxString = attributesMap.values().iterator().next();
        String comaSeparatedStrings[] = bboxString.split(",");

        /*
         * These variables indicate if there is a crs code in the coma-separated string
         * and how many dimension there is in the crs
         */
        CoordinateReferenceSystem crs = null;


        //-- reading coordinate list.
        //-- in case where length is odd the last interger should be equals to coordinates numbers
        final List<Double> coords = new ArrayList<>();

        // Pre analysis
        for (String value : comaSeparatedStrings) {
            if (NumberUtils.isNumber(value)) {
                coords.add(Double.valueOf(value));
            } else {
                // If a CRS has been already read...abort
                if (crs != null)
                    throw new CstlServiceException("Two CRS found while reading the " + inputIdentifier + " BoundingBox");
                try {
                    // If when reading the crs you already read an odd number of
                    // bounding box coordinates there is a problem
                    if (coords.size() % 2 != 0)
                        throw new CstlServiceException("An odd number of bounding box coordinates has been read.");

                    crs = CRS.forCode(value);
                } catch (FactoryException ex) {
                    throw new CstlServiceException(ex);
                }
            }
        }

        final int coordsListLength = coords.size();

        // If no coordinate has been read...abort
        if (coords.isEmpty())
            throw new CstlServiceException("Could not read any coordinate from the BoundingBox");

        // Extract BoundingBox dimension
        final int bboxDimension = coordsListLength >> 1;

        //-- if coordinates numbers is odd
        if (coordsListLength % 2 != 0) {//-- coordinateElement & 1 == 0
           /*
            * In the the following strings the number N tell us how many dimension
            * there is in the bounding box :
            * 46,102,... 47,103,... crs code,N
            * 46,102,... 47,103, ...N,crs code
            *
            * But this number is not mandatory.
            */
            final int evenListLength = coordsListLength & ~1;//-- = -1 on odd number
            final int dimensionHint = (int) StrictMath.round(coords.get(evenListLength));

            //-- check that cast double to integer has no problem
            if (StrictMath.abs(coords.get(evenListLength) - dimensionHint) > 1E-12)
                throw new CstlServiceException("The dimension parameter is not an integer : " + coords.get(evenListLength));

            ArgumentChecks.ensureStrictlyPositive("dimensionHint", dimensionHint);

            assert dimensionHint >= 2 : "Expected dimension hint equal or greater than 4, adapted for Geographical coordinates. Found : " + dimensionHint;

            if (evenListLength != dimensionHint * 2)
                throw new CstlServiceException("Expected " + evenListLength + " coordinates whereas " + dimensionHint + " was expected.");
        }

        if (crs != null && bboxDimension != crs.getCoordinateSystem().getDimension())
            throw new CstlServiceException("Reading coordinates number does not match with CRS dimension number.\n"
                    + " CRS dimension : "+crs.getCoordinateSystem().getDimension()+". Coordinates number : "+bboxDimension);

        //-- bind list -> array
        final double[] coordsArrayLower = new double[bboxDimension];
        final double[] coordsArrayUpper = new double[bboxDimension];

        for (int i = 0; i < bboxDimension; i++) {
            coordsArrayLower[i] = coords.get(i);
            coordsArrayUpper[i] = coords.get(i + bboxDimension);
        }

        final GeneralEnvelope generalEnvelope = new GeneralEnvelope(coordsArrayLower, coordsArrayUpper);

        if (crs == null) {
            // If no CRS are provided we set a default one which is the WGS84.
            // But this CRS can not be applied on every bounding box, so we have to
            // check the dimensions and raise an error when they are different
            if (bboxDimension > DEFAULT_CRS.getCoordinateSystem().getDimension())
                throw new CstlServiceException("No CRS provided and the default 2D CRS"
                                             + " can not be applied because the bounding box has " + bboxDimension + " dimensions.");

            generalEnvelope.setCoordinateReferenceSystem(DEFAULT_CRS);
        } else {
            generalEnvelope.setCoordinateReferenceSystem(crs);
        }

        BoundingBox bb = new BoundingBoxType(generalEnvelope);
        Data dataType = new Data(bb);
        return new DataInput(inputIdentifier, dataType);
    }

    /**
     * Parse the decoded arguments of a GET request in order to extract the response
     * form.
     *
     * This method assumes that responseString contains only the response field of
     * the GET request and that it is URL decoded
     *
     * @param responseString the string containing document response attributes
     * @param isRawData set to true if responseString contains raw data
     * @return a ResponseDocumentType encapsulated into a ResponseFormType
     * @throws CstlServiceException when an unknown attribute is read
     */
    static OutputDefinition extractRawResponseForm(final String version, final Map<String, Map> inputMap) throws CstlServiceException {
        for (String inputIdentifier : inputMap.keySet()) {

            Map<String, String> attributesMap = inputMap.get(inputIdentifier);
            OutputDefinition docOutput = new OutputDefinition(inputIdentifier, false);

            for (String key : attributesMap.keySet()) {
                String value = attributesMap.get(key);

                if (key.equalsIgnoreCase(MIME_TYPE_PARAMETER) || key.equalsIgnoreCase(FORMAT_PARAMETER)) {
                    docOutput.setMimeType(value);
                } else if (key.equalsIgnoreCase(ENCODING_PARAMETER)) {
                    docOutput.setEncoding(value);
                } else if (key.equalsIgnoreCase(SCHEMA_PARAMETER)) {
                    docOutput.setSchema(value);
                } else if (key.equalsIgnoreCase(UOM_PARAMETER)) {
                    docOutput.setUom(value);
                } else if (key.equalsIgnoreCase(AS_REFERENCE_PARAMETER)) {
                    throw new CstlServiceException("Trying to set RawDataOutput with unknown attribute " + key + " (value : " + value + ")");
                } else if (!key.equals(inputIdentifier)) {
                    throw new CstlServiceException("Trying to set RawDataOutput with unknown attribute " + key + " (value : " + value + ")");
                }
            }

            // We can have more than one DocumentOutputDefinition
            // but we can have just one RawDataOutput. Since the code to read
            // rawdata attribute is almost the same as the one to read
            // DocumentOutputDefinition (see the above condition against
            // AS_REFERENCE_PARAMETER), we kept everything in the same method
            // and just break the loop when a RawData has been read
            return docOutput;
        }
        return null;
    }

    static List<OutputDefinition> extractDocumentResponseForm(final String version, Map<String, Map> inputMap) throws CstlServiceException {

        final List<OutputDefinition> outputs = new ArrayList<>();
        for (String inputIdentifier : inputMap.keySet()) {
            Map<String, String> attributesMap = inputMap.get(inputIdentifier);


           /*
            * FIX ME- remove this block when GEOTK-548 is integrated
            */
            boolean asReference = false;
            for (String key : attributesMap.keySet()) {
                String value = attributesMap.get(key);
                if (key.equalsIgnoreCase(AS_REFERENCE_PARAMETER)) {
                    asReference = Boolean.parseBoolean(value);
                }
            }

            OutputDefinition docOutput = new OutputDefinition(inputIdentifier, asReference);

            for (String key : attributesMap.keySet()) {
                String value = attributesMap.get(key);

                if (key.equalsIgnoreCase(MIME_TYPE_PARAMETER) || key.equalsIgnoreCase(FORMAT_PARAMETER)) {
                    docOutput.setMimeType(value);
                } else if (key.equalsIgnoreCase(ENCODING_PARAMETER)) {
                    docOutput.setEncoding(value);
                } else if (key.equalsIgnoreCase(SCHEMA_PARAMETER)) {
                    docOutput.setSchema(value);
                } else if (key.equalsIgnoreCase(UOM_PARAMETER)) {
                    docOutput.setUom(value);
                } else if (key.equalsIgnoreCase(AS_REFERENCE_PARAMETER)) {
                    //docOutput.setAsReference(Boolean.parseBoolean(value));FIX ME- uncomment when GEOTK-548 is integrated

                } else if (!key.equals(inputIdentifier)) {
                    throw new CstlServiceException("Trying to set DocumentOutputDefinition with unknown attribute " + key + " (value : " + value + ")");
                }
            }
            outputs.add(docOutput);
        }
        return outputs;
    }

    static Map extractDataFromKvpString(final String inputString) throws CstlServiceException {
        ArgumentChecks.ensureNonEmpty("inputString", inputString);
        final String[] allInputs = inputString.split(";");
        Map<String, Map> inputMap = new HashMap<>();
        for (String input : allInputs) {
            final String[] attribs = input.split("@");
            final String inputIdent = attribs[0].split("=")[0];

            final Map<String, String> attributsMap = new HashMap<>();
            for (String attribut : attribs) {
                String[] splitAttribute = attribut.split("=");

                if (splitAttribute.length == 2) {
                    attributsMap.put(splitAttribute[0], splitAttribute[1]);
                } else if (splitAttribute.length == 1) {
                    attributsMap.put(splitAttribute[0], null);
                } else {
                    throw new CstlServiceException("Invalid DataInputs format", INVALID_FORMAT, VERSION_PARAMETER.toLowerCase());
                }
            }
            inputMap.put(inputIdent, attributsMap);
        }
        return inputMap;
    }

    /**
     * Get an html page for the root resource.
     */
    private ResponseObject getIndexPage(String serviceId) throws CstlServiceException {
        String format = getParameter("f", false);
        if ("json".equals(format) || "application/json".equals(format)) {
            LandingPage lp = new LandingPage();
            lp.addLinksItem(new JsonLink(getServiceURL() + "/wps/" + serviceId,                  "self",        MediaType.APPLICATION_JSON_VALUE, null, null));
            lp.addLinksItem(new JsonLink(getServiceURL() + "/wps/" + serviceId + "/api",         "service",     MediaType.APPLICATION_JSON_VALUE, null, null));
            lp.addLinksItem(new JsonLink(getServiceURL() + "/wps/" + serviceId + "/conformance", "conformance", MediaType.APPLICATION_JSON_VALUE, null, null));
            lp.addLinksItem(new JsonLink(getServiceURL() + "/wps/" + serviceId + "/processes",   "processes",   MediaType.APPLICATION_JSON_VALUE, null, null));
            return new ResponseObject(lp, MediaType.APPLICATION_JSON);
        } else {

            return new ResponseObject("<html>\n"
                    + "  <title>Examind WPS</title>\n"
                    + "  <body>\n"
                    + "    <h1><i>Examind:</i></h1>\n"
                    + "    <h1>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; Web Processing Service</h1>\n"
                    + "    <p>\n"
                    + "      In order to access this service, you must form a valid request.\n"
                    + "    </p\n"
                    + "    <p>\n"
                    + "      Try using a <a href=\"" + getServiceURL() + "/wps/" + serviceId
                    + "?service=WPS&request=GetCapabilities\""
                    + ">Get Capabilities</a> request to obtain the 'Capabilities'<br>\n"
                    + "      document which describes the resources available on this server.\n"
                    + "    </p>\n"
                    + "  </body>\n"
                    + "</html>\n", MediaType.TEXT_HTML);
        }
    }

    private boolean isIndexPageRequest() {
        Map<String, String[]> params = new HashMap<>(getParameters());
        params.remove("serviceId");
        params.remove("f");
        return params.isEmpty();
    }

    @RequestMapping(path = "processes", method = RequestMethod.GET)
    public ResponseEntity listProcessRestful(@PathVariable("serviceId") String serviceId) {
        try {
            final AcceptVersionsType versions = new AcceptVersionsType("2.0.0");
            final AcceptFormatsType formats   = new AcceptFormatsType("application/json");
            final GetCapabilities gc = new GetCapabilities(versions, null, formats, null, "WPS", null);

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Capabilities capa = worker.getCapabilities(gc);
                Contents offering = capa.getContents();
                ProcessCollection collec = new ProcessCollection(offering.getProcessSummary().stream());
                //update process description URL
                for (org.geotoolkit.wps.json.ProcessSummary sum : collec.getProcesses()) {
                    sum.setProcessDescriptionURL(getServiceURL() + "/wps/" + serviceId + "/processes/" + sum.getId());
                }

                return new ResponseObject(collec, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}", method = RequestMethod.GET)
    public ResponseEntity describeProcessRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId) {
        try {
            final DescribeProcess dp = new DescribeProcess("WPS", "2.0.0", null, Arrays.asList(new CodeType(processId)));

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                ProcessOfferings offering = worker.describeProcess(dp);

                org.geotoolkit.wps.json.ProcessOffering jsOffering = new org.geotoolkit.wps.json.ProcessOffering(offering.getProcessOffering().get(0));

                // update executEndpoint
                if (jsOffering.getProcess() != null) {
                    jsOffering.getProcess().setExecuteEndpoint(getServiceURL() + "/wps/" + serviceId + "/processes/" + jsOffering.getProcess().getId() + "/jobs");
                }

                return new ResponseObject(jsOffering, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}/jobs/{jobID}", method = RequestMethod.GET)
    public ResponseEntity getStatusInfoRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId
            , @PathVariable("jobID") String jobId) {
        try {
            final GetStatus gs = new GetStatus("WPS", "2.0.0", jobId);

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                StatusInfo si = worker.getStatus(gs);

                return new ResponseObject(new org.geotoolkit.wps.json.StatusInfo(si), MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}/jobs/{jobID}", method = RequestMethod.DELETE)
    public ResponseEntity dismissRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId
            , @PathVariable("jobID") String jobId) {
        try {
            final Dismiss gs = new Dismiss("WPS", "2.0.0", jobId);

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                StatusInfo si = worker.dismiss(gs);

                return new ResponseObject(new org.geotoolkit.wps.json.StatusInfo(si), MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}/jobs", method = RequestMethod.GET)
    public ResponseEntity getJobsListRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Set<String> jobs = worker.getJobList(processId);
                return new ResponseObject(new JobCollection().jobs(new ArrayList<>(jobs)), MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}/jobs", method = RequestMethod.POST)
    public ResponseEntity executeRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId,
            @RequestBody org.geotoolkit.wps.json.Execute request) {

        try {
            final Execute exec = convertExecuteRequestToXML(processId, request);

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Object execResp = worker.execute(exec);
                Map<String, String> headers = new HashMap<>();
                if (execResp instanceof Result) {
                    String statusLocation = getServiceURL() + "/wps/" + serviceId + "/processes/" + processId + "/jobs/" + ((Result) execResp).getJobID();
                    headers.put("Location", statusLocation);
                } else if (execResp instanceof StatusInfo) {
                    String statusLocation = getServiceURL() + "/wps/" + serviceId + "/processes/" + processId + "/jobs/" + ((StatusInfo) execResp).getJobID();
                    headers.put("Location", statusLocation);
                }

                return new ResponseObject(HttpStatus.CREATED, headers).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}/jobs/{jobID}/result", method = RequestMethod.GET)
    public ResponseEntity getResultRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId
            , @PathVariable("jobID") String jobId) {
        try {
            final GetResult gs = new GetResult("WPS", "2.0.0", jobId);

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Object re = worker.getResult(gs);
                Object response;
                // Document
                if (re instanceof Result) {
                    org.geotoolkit.wps.json.Result r = new org.geotoolkit.wps.json.Result();
                    Result result = (Result) re;
                    for (DataOutput out : result.getOutput()) {
                        OutputInfo oi = new OutputInfo();
                        oi.setId(out.getId());

                        // TODO complex
                        if (out.getData() != null && out.getData().getContent().size() > 0) {
                            oi.setData(out.getData().getContent().get(0).toString());
                        } else if (out.getReference() != null && out.getReference().getHref() != null) {
                            oi.setData(out.getReference().getHref());
                        }
                        r.addOutputsItem(oi);
                    }
                    if (result.getLinks() != null) {
                        List<org.geotoolkit.wps.json.JsonLink> links = new ArrayList<>();
                        for (Link link : result.getLinks()) {
                            links.add(new org.geotoolkit.wps.json.JsonLink(link));
                        }
                        r.setLinks(links);
                    }
                    response = r;

                // RAW
                } else  {
                    response = re;
                }

                return new ResponseObject(response, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes", method = RequestMethod.POST)
    public ResponseEntity deployRestful(@PathVariable("serviceId") String serviceId, @RequestBody org.geotoolkit.wps.json.Deploy request) {
        try {
            final Deploy dp = new Deploy("WPS", "2.0.0", null, request);

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                DeployResult result = worker.deploy(dp);
                org.geotoolkit.wps.json.DeploymentResult jsResult = new org.geotoolkit.wps.json.DeploymentResult(result);

                // update description URL
                if (jsResult.getProcessSummary() != null) {
                    jsResult.getProcessSummary().setProcessDescriptionURL(getServiceURL() + "/wps/" + serviceId + "/processes/" + jsResult.getProcessSummary().getId());
                }

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "processes/{id}", method = RequestMethod.DELETE)
    public ResponseEntity undeployProcessRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId) {
        try {
            final Undeploy dp = new Undeploy("WPS", "2.0.0", null, new CodeType(processId));

            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                UndeployResult result = worker.undeploy(dp);

                org.geotoolkit.wps.json.UndeployementResult jsResult = new org.geotoolkit.wps.json.UndeployementResult(result);

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/processes/{id}/quotations", method = RequestMethod.GET)
    public ResponseEntity getQuotationsForProcessListRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                QuotationList result = worker.getQuotationList(processId);

                org.geotoolkit.wps.json.QuotationList jsResult = new org.geotoolkit.wps.json.QuotationList(result.getQuotations());

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/quotations", method = RequestMethod.GET)
    public ResponseEntity getQuotationListRestful(@PathVariable("serviceId") String serviceId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                QuotationList result = worker.getQuotationList();

                org.geotoolkit.wps.json.QuotationList jsResult = new org.geotoolkit.wps.json.QuotationList(result.getQuotations());

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }


    @RequestMapping(path = "/processes/{id}/quotations", method = RequestMethod.POST)
    public ResponseEntity quoteRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId,
            @RequestBody org.geotoolkit.wps.json.Execute request) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                final Execute exec = convertExecuteRequestToXML(processId, request);

                Quotation result = worker.quote(exec);

                org.geotoolkit.wps.json.Quotation jsResult = new org.geotoolkit.wps.json.Quotation(result);
                jsResult.setProcessParameters(request);

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/quotations/{quotationId}", method = RequestMethod.GET)
    public ResponseEntity getQuotationRestful(@PathVariable("serviceId") String serviceId, @PathVariable("quotationId") String quotationId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Quotation result = worker.getQuotation(quotationId);

                org.geotoolkit.wps.json.Quotation jsResult = new org.geotoolkit.wps.json.Quotation(result);
                jsResult.setProcessParameters(convertExecuteRequestToJson(result.getProcessParameters()));

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    /**
     * useless at my opinion, as the quotation is unique we can use getQuotationRestful()
     */
    @RequestMapping(path = "/processes/{id}/quotations/{quotationId}", method = RequestMethod.GET)
    public ResponseEntity getQuotationRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId, @PathVariable("quotationId") String quotationId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Quotation result = worker.getQuotation(quotationId);

                org.geotoolkit.wps.json.Quotation jsResult = new org.geotoolkit.wps.json.Quotation(result);

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/quotations/{quotationId}", method = RequestMethod.POST)
    public ResponseEntity executeQuotationRestful(@PathVariable("serviceId") String serviceId, @PathVariable("quotationId") String quotationId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Quotation quot = worker.getQuotation(quotationId);

                Object execResp = worker.executeQuotation(quotationId);
                Map<String, String> headers = new HashMap<>();
                if (execResp instanceof Result) {
                    String statusLocation = getServiceURL() + "/wps/" + serviceId + "/processes/" + quot.getProcessId() + "/jobs/" + ((Result) execResp).getJobID();
                    headers.put("Location", statusLocation);
                } else if (execResp instanceof StatusInfo) {
                    String statusLocation = getServiceURL() + "/wps/" + serviceId + "/processes/" + quot.getProcessId() + "/jobs/" + ((StatusInfo) execResp).getJobID();
                    headers.put("Location", statusLocation);
                }

                return new ResponseObject(HttpStatus.CREATED, headers).getResponseEntity();

            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    /**
     * useless at my opinion, as the quotation is unique we can use getQuotationRestful()
     */
    @RequestMapping(path = "/processes/{id}/quotations/{quotationId}", method = RequestMethod.POST)
    public ResponseEntity executeQuotationForProcessRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId, @PathVariable("quotationId") String quotationId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Object execResp = worker.executeQuotation(quotationId);
                Map<String, String> headers = new HashMap<>();
                if (execResp instanceof Result) {
                    String statusLocation = getServiceURL() + "/wps/" + serviceId + "/processes/" + processId + "/jobs/" + ((Result) execResp).getJobID();
                    headers.put("Location", statusLocation);
                } else if (execResp instanceof StatusInfo) {
                    String statusLocation = getServiceURL() + "/wps/" + serviceId + "/processes/" + processId + "/jobs/" + ((StatusInfo) execResp).getJobID();
                    headers.put("Location", statusLocation);
                }

                return new ResponseObject(HttpStatus.CREATED, headers).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/bills", method = RequestMethod.GET)
    public ResponseEntity getBillListRestful(@PathVariable("serviceId") String serviceId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                BillList result = worker.getBillList();

                org.geotoolkit.wps.json.BillList jsResult = new org.geotoolkit.wps.json.BillList(result.getBills());

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/bills/{billId}", method = RequestMethod.GET)
    public ResponseEntity getBillRestful(@PathVariable("serviceId") String serviceId, @PathVariable("billId") String billId) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Bill result = worker.getBill(billId);

                org.geotoolkit.wps.json.Bill jsResult = new org.geotoolkit.wps.json.Bill(result);

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }

    @RequestMapping(path = "/processes/{id}/jobs/{jobID}/bill", method = RequestMethod.GET)
    public ResponseEntity getBillForJobRestful(@PathVariable("serviceId") String serviceId, @PathVariable("id") String processId, @PathVariable("jobID") String jobID) {
        try {
            putServiceIdParam(serviceId);
            WPSWorker worker = getWorker(serviceId);

            if (worker != null) {
                Bill result = worker.getBillForJob(jobID);

                org.geotoolkit.wps.json.Bill jsResult = new org.geotoolkit.wps.json.Bill(result);

                return new ResponseObject(jsResult, MediaType.APPLICATION_JSON).getResponseEntity();
            } else {
                LOGGER.log(Level.WARNING, "Received request on undefined instance identifier:{0}", serviceId);
                return new ResponseObject(HttpStatus.NOT_FOUND).getResponseEntity();
            }
        } catch (CstlServiceException ex) {
            return new ResponseObject(new ExceptionReportType(ex.getExceptionCode().name(), ex.getMessage()), MediaType.APPLICATION_JSON, ex.getHttpCode()).getResponseEntity();
        }
    }


    @RequestMapping(path = "/conformance", method = RequestMethod.GET)
    public ResponseEntity conformanceRestful(@PathVariable("serviceId") String serviceId) {
        ReqClasses result = new ReqClasses();
        result.addConformsToItem("http://www.opengis.net/spec/WPS/2.0/conf/service/profile/basic-wps");
        result.addConformsToItem("http://www.opengis.net/spec/WPS/2.0/conf/service/synchronous-wps");
        result.addConformsToItem("http://www.opengis.net/spec/WPS/2.0/conf/service/asynchronous-wps");
        result.addConformsToItem("http://www.opengis.net/spec/WPS/2.0/conf/service/transactional-wps");
        result.addConformsToItem("http://www.opengis.net/spec/WPS/2.0/conf/process-model-encoding");
        return new ResponseObject(result, MediaType.APPLICATION_JSON).getResponseEntity();
    }

    @RequestMapping(path = "/api", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity api(@PathVariable("serviceId") String serviceId) throws IOException {
        String api = IOUtilities.toString(Util.getResourceAsStream("org/constellation/json/wps-t-api.json"));
        return new ResponseEntity(api, HttpStatus.OK);
    }


    private static Execute convertExecuteRequestToXML(String processId, org.geotoolkit.wps.json.Execute request) throws CstlServiceException {
        final List<DataInput> inputs = new ArrayList<>();
        final List<OutputDefinition> outputs = new ArrayList<>();
        for (org.geotoolkit.wps.json.Input input : request.getInputs()) {
            Format format = null;
            Object value;
            if (input.getFormat() != null) {
                format = new Format(input.getFormat().getEncoding(), input.getFormat().getMimeType(), input.getFormat().getSchema(), null);
            }

            // we assume that is a literal or a reference
            if (input.getData()!= null) {
                value = input.getData();
            } else if (input.getHref()!= null) {
                value = input.getHref();
            } else {
                throw new CstlServiceException("Missing input valueReference/inlineValue for parameter:" + input.getId(), INVALID_PARAMETER_VALUE);
            }

            Data inputData = new Data(format, value);
            inputs.add(new DataInput(input.getId(), inputData));
        }

        for (org.geotoolkit.wps.json.Output output : request.getOutputs()) {
            boolean asReference = output.getTransmissionMode().equals(DataTransmissionMode.REFERENCE);
            if (output.getFormat() != null) {
                outputs.add(new OutputDefinition(output.getId(), output.getFormat().getEncoding(), output.getFormat().getMimeType(), output.getFormat().getSchema(), asReference));
            } else {
                outputs.add(new OutputDefinition(output.getId(), asReference));
            }
        }

        Execute exec = new Execute("WPS", "2.0.0", null, new CodeType(processId), inputs, outputs, request.getResponse());
        exec.setMode(request.getMode());
        return exec;
    }

    private static org.geotoolkit.wps.json.Execute convertExecuteRequestToJson(Execute request) throws CstlServiceException {
        final List<org.geotoolkit.wps.json.Input> inputs = new ArrayList<>();
        final List<org.geotoolkit.wps.json.Output> outputs = new ArrayList<>();
        for (DataInput input : request.getInput()) {
            org.geotoolkit.wps.json.Format format = null;

            if (input.getData() != null &&
                (input.getData().getMimeType()!= null ||  input.getData().getSchema()!= null || input.getData().getEncoding()!= null)) {
                format = new org.geotoolkit.wps.json.Format(input.getData().getMimeType(), input.getData().getSchema(), input.getData().getEncoding());
            }

            // we assume that is a literal or a reference
            String data = null;
            String href = null;
            if (input.getData() != null && input.getData().getContent() != null && !input.getData().getContent().isEmpty()) {
                data = (String) input.getData().getContent().get(0);
            } else if (input.getReference() != null) {
                href = input.getReference().getHref();
            } else {
                throw new CstlServiceException("Missing input Reference/Data for parameter:" + input.getId(), INVALID_PARAMETER_VALUE);
            }

            inputs.add(new org.geotoolkit.wps.json.Input(input.getId(), format, data, href));
        }

        for (OutputDefinition output : request.getOutput()) {

            org.geotoolkit.wps.json.Format format = null;

            if (output.getMimeType()!= null ||  output.getSchema()!= null || output.getEncoding()!= null) {
                format = new org.geotoolkit.wps.json.Format(output.getMimeType(), output.getSchema(), output.getEncoding());
            }
            outputs.add(new org.geotoolkit.wps.json.Output(output.getIdentifier(), format, output.getTransmission()));
        }

        return new org.geotoolkit.wps.json.Execute(inputs, outputs);
    }

}
