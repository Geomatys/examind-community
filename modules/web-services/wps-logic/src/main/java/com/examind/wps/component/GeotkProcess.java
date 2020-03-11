/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package com.examind.wps.component;

import com.examind.wps.ExecutionInfo;
import com.examind.wps.QuotationInfo;
import com.examind.wps.WPSProcessListener;
import com.examind.wps.WPSProcessRawListener;
import com.examind.wps.api.IOParameterException;
import com.examind.wps.api.ProcessPreConsumer;
import com.examind.wps.api.WPSException;
import com.examind.wps.api.WPSProcess;
import static com.examind.wps.util.WPSConstants.GML_VERSION;
import static com.examind.wps.util.WPSConstants.WMS_SUPPORTED;
import static com.examind.wps.util.WPSConstants.WPS_SUPPORTED_CRS;
import com.examind.wps.util.WPSURLUtils;
import com.examind.wps.util.WPSUtils;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.xml.bind.JAXBException;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.service.config.wps.Process;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.coverage.grid.GridCoverage;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.ows.xml.BoundingBox;
import org.geotoolkit.ows.xml.v200.AdditionalParametersType;
import org.geotoolkit.ows.xml.v200.AllowedValues;
import org.geotoolkit.ows.xml.v200.AnyValue;
import org.geotoolkit.ows.xml.v200.BoundingBoxType;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.ows.xml.v200.DomainMetadataType;
import org.geotoolkit.ows.xml.v200.LanguageStringType;
import org.geotoolkit.ows.xml.v200.OwsContextDescriptionType;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.io.WPSIO;
import org.geotoolkit.wps.io.WPSMimeType;
import org.geotoolkit.wps.xml.v200.Data;
import org.geotoolkit.wps.xml.v200.DataDescription;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.DataOutput;
import org.geotoolkit.wps.xml.v200.DataTransmissionMode;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.Format;
import org.geotoolkit.wps.xml.v200.InputDescription;
import org.geotoolkit.wps.xml.v200.JobControlOptions;
import org.geotoolkit.wps.xml.v200.LiteralData;
import org.geotoolkit.wps.xml.v200.LiteralValue;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.OutputDescription;
import org.geotoolkit.wps.xml.v200.ProcessDescription;
import org.geotoolkit.wps.xml.v200.ProcessOffering;
import org.geotoolkit.wps.xml.v200.ProcessSummary;
import org.geotoolkit.wps.xml.v200.Quotation;
import org.geotoolkit.wps.xml.v200.Reference;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class GeotkProcess implements WPSProcess {

    private final static Logger LOGGER = Logging.getLogger("com.examind.wps.component");
    private final ProcessDescriptor descriptor;
    private final URI schemaFolder;
    private String schemaURL;
    private final List<JobControlOptions> controlOptions;
    private final List<DataTransmissionMode> outTransmissions;
    private final Map<String, Object> userMap;

    private static final NumberFormat NUMFORM = NumberFormat.getNumberInstance(Locale.ENGLISH);
    static {
        NUMFORM.setMinimumFractionDigits(2);
        NUMFORM.setMaximumFractionDigits(2);

    }

    /**
     * A hint telling if process identifier will be prefixed with urn:.. If true, we add the prefix. if false, we don't.
     * Default value is true.
     */
    private final boolean withPrefix;

    Collection<ProcessPreConsumer> preConsumers;

    public GeotkProcess(ProcessDescriptor descriptor, URI schemaFolder, String schemaURL, Process controlOptions) {
        this(descriptor, schemaFolder, schemaURL, controlOptions, true);
    }

    public GeotkProcess(ProcessDescriptor descriptor, URI schemaFolder, String schemaURL, Process configuration, boolean addPrefix) {
        this.descriptor = descriptor;
        this.schemaFolder = schemaFolder;
        this.schemaURL = schemaURL;
        if (configuration == null || configuration.getJobControlOptions() == null || configuration.getJobControlOptions().isEmpty()) {
            this.controlOptions = Arrays.asList(JobControlOptions.SYNC_EXECUTE,
                                                JobControlOptions.ASYNC_EXECUTE,
                                                JobControlOptions.DISMISS);
        } else {
            this.controlOptions = new ArrayList<>();
            for (String s: configuration.getJobControlOptions()) {
                this.controlOptions.add(JobControlOptions.valueOf(s));
            }
        }
        if (configuration == null || configuration.getOutputTransmission()== null || configuration.getOutputTransmission().isEmpty()) {
            this.outTransmissions = Arrays.asList(DataTransmissionMode.REFERENCE,
                                                DataTransmissionMode.VALUE);
        } else {
            this.outTransmissions = new ArrayList<>();
            for (String s: configuration.getOutputTransmission()) {
                this.outTransmissions.add(DataTransmissionMode.valueOf(s));
            }
        }
        if (configuration == null || configuration.getUsePrefix() == null) {
            withPrefix = addPrefix;
        } else {
            withPrefix = configuration.getUsePrefix();
        }
        if (configuration == null || configuration.getUserMap() == null) {
            userMap = new HashMap<>();
        } else {
            userMap = configuration.getUserMap();
        }
    }

    public void setSchemaURL(final String schemaURL) {
        this.schemaURL = schemaURL;
    }

    public void setPreConsumers(final Collection<ProcessPreConsumer> preConsumers) {
        this.preConsumers = preConsumers;
    }

    private void applyPreConsumers(final org.geotoolkit.process.Process p) {
        if (preConsumers != null) {
            preConsumers.stream()
                    .forEach(preconsumer -> preconsumer.accept(p));
        }
    }

    @Override
    public void checkForSchemasToStore() throws IOException {
        /*
         * Check each input and output. If we get a parameterDescriptorGroup,
         * we must store a schema which describe its structure.
         */
        for (GeneralParameterDescriptor desc : descriptor.getInputDescriptor().descriptors()) {
            checkSchemaForParamDesc(desc, schemaFolder);
        }

        for (GeneralParameterDescriptor desc : descriptor.getOutputDescriptor().descriptors()) {
            checkSchemaForParamDesc(desc, schemaFolder);
        }
    }

    private void checkSchemaForParamDesc(GeneralParameterDescriptor desc, URI schemaFolder) throws IOException {
        if (desc instanceof ParameterDescriptorGroup) {
            FeatureType ft = WPSConvertersUtils.descriptorGroupToFeatureType((ParameterDescriptorGroup) desc);
            Path xsdStore = Paths.get(schemaFolder).resolve(ft.getName().tip().toString() + ".xsd");
            try {
                WPSUtils.storeFeatureSchema(ft, xsdStore);
            } catch (JAXBException ex) {
                throw new IOException(ex);
            }
        }
    }

    @Override
    public CodeType getIdentifier() {
        return new CodeType(withPrefix? WPSUtils.buildProcessIdentifier(descriptor) : descriptor.getIdentifier().getCode());
    }

    @Override
    public ProcessSummary getProcessSummary(Locale loc) {
        ProcessSummary sum = WPSUtils.generateProcessBrief(descriptor, loc, withPrefix);
        sum.getJobControlOptions().addAll(controlOptions);
        sum.getOutputTransmission().addAll(outTransmissions);
        return sum;
    }

    @Override
    public boolean isSupportedProcess() {
        return WPSUtils.isSupportedProcess(descriptor);
    }

    @Override
    public ProcessOffering getProcessOffering(Locale lang) throws WPSException {

        final CodeType identifier = getIdentifier();
        final LanguageStringType title = WPSUtils.buildProcessTitle(descriptor, lang);
        final List<LanguageStringType> _abstract = WPSUtils.buildProcessDescription(descriptor, lang).collect(Collectors.toList());

        //TODO WSDL
        final boolean statusSupported = true;

        // Get process input and output descriptors
        final ParameterDescriptorGroup input = descriptor.getInputDescriptor();
        final ParameterDescriptorGroup output = descriptor.getOutputDescriptor();

        ///////////////////////////////
        //  Process Input parameters
        ///////////////////////////////
        final List<InputDescription> dataInputs = new ArrayList<>();
        for (final GeneralParameterDescriptor param : input.descriptors()) {

            /*
             * Whatever the parameter type is, we prepare the name, title, abstract and multiplicity parts.
             */
            final CodeType inId = new CodeType(withPrefix?
                    WPSUtils.buildProcessIOIdentifiers(descriptor, param, WPSIO.IOType.INPUT) :
                    param.getName().getCode());
            final LanguageStringType inTitle = WPSUtils.buildProcessIOTitle(param, lang);
            final LanguageStringType inAbstract = WPSUtils.buildProcessIODescription(param, lang);
            final List<AdditionalParametersType> inAddParams = WPSUtils.buildAdditionalParams(param);

            //set occurs
            String maxOccurs = Integer.toString(param.getMaximumOccurs());
            Integer minOccurs = param.getMinimumOccurs();

            DataDescription dataDescription;

            // If the Parameter Descriptor isn't a ParameterDescriptorGroup
            if (param instanceof ParameterDescriptor) {
                final ParameterDescriptor paramDesc = (ParameterDescriptor) param;

                // Input class
                final Class clazz = paramDesc.getValueClass();

                // extra parameter description
                Map<String, Object> userData = null;
                if (paramDesc instanceof ExtendedParameterDescriptor) {
                    userData = ((ExtendedParameterDescriptor) paramDesc).getUserObject();
                }

                // BoundingBox type
                if (WPSIO.isSupportedBBoxInputClass(clazz)) {
                    dataDescription = WPS_SUPPORTED_CRS;

                //Complex type (XML, ...)
                } else if (WPSIO.isSupportedComplexInputClass(clazz)) {

                    dataDescription = WPSUtils.describeComplex(clazz, WPSIO.IOType.INPUT, WPSIO.FormChoice.COMPLEX, userData);

                //Literal type
                } else if (WPSIO.isSupportedLiteralInputClass(clazz)) {

                    String defaultValue = null;
                    if (paramDesc.getDefaultValue() != null) {
                        defaultValue = paramDesc.getDefaultValue().toString(); //default value if enable
                    }

                    Object uom = null;
                    if (paramDesc.getUnit() != null) {
                        uom = WPSUtils.generateUOMs(paramDesc);
                    }
                    //AllowedValues setted
                    AllowedValues allowedValues = null;
                    AnyValue anyvalue = null;
                    if (paramDesc.getValidValues() != null && !paramDesc.getValidValues().isEmpty()) {
                        allowedValues = new AllowedValues(paramDesc.getValidValues());
                    } else {
                        anyvalue = new AnyValue();
                    }

                    DomainMetadataType dataType = WPSConvertersUtils.createDataType(clazz);

                    // for literal data add default format text/plain if no custom format is supplied
                    List<Format> formats = WPSUtils.getWPSCustomIOFormats(userData, WPSIO.IOType.INPUT);
                    if (formats == null) {
                       formats =  Arrays.asList(new Format(WPSMimeType.TEXT_PLAIN.val(), true));
                    }
                    dataDescription = new LiteralData(formats, allowedValues, anyvalue, null, dataType, (DomainMetadataType) uom, defaultValue, null);

                //Reference type (XML, ...)
                } else if (WPSIO.isSupportedReferenceInputClass(clazz)) {

                    dataDescription = WPSUtils.describeComplex(clazz, WPSIO.IOType.INPUT, WPSIO.FormChoice.REFERENCE, userData);

                    //Simple object (Integer, double, ...) and Object which need a conversion from String like affineTransform or WKT Geometry
                } else {
                    throw new WPSException("Process input parameter" + inId + " not supported.");
                }

            } else if (param instanceof ParameterDescriptorGroup) {
                /*
                     * If we get a parameterDescriptorGroup, we must expose the
                     * parameters contained in it as one single input. To do so,
                     * we'll expose a feature type input.
                 */
                FeatureType ft = WPSConvertersUtils.descriptorGroupToFeatureType((ParameterDescriptorGroup) param);

                // Build the schema xsd, and store it into temporary folder.
                String xsdName = ft.getName().tip().toString() + ".xsd";
                Path xsdStore = Paths.get(schemaFolder).resolve(xsdName);
                try {
                    WPSUtils.storeFeatureSchema(ft, xsdStore);
                    final Class clazz = ft.getClass();

                    String publicAddress = schemaURL + "/" + xsdName;
                    HashMap<String, Object> userData = new HashMap<>(1);
                    userData.put(WPSIO.SCHEMA_KEY, publicAddress);
                    dataDescription = WPSUtils.describeComplex(clazz, WPSIO.IOType.INPUT, WPSIO.FormChoice.COMPLEX, userData);
                } catch (IOException | JAXBException ex) {
                    throw new WPSException("The schema for parameter " + param.getName().getCode() + "can't be build.");
                }

            } else {
                throw new WPSException("Process input parameter " + inId + " invalid.");
            }
            dataInputs.add(new InputDescription(inId, inTitle, inAbstract, null, inAddParams, minOccurs, maxOccurs, dataDescription));
        }

        ///////////////////////////////
        //  Process Output parameters
        ///////////////////////////////
        final List<OutputDescription> dataOutputs = new ArrayList<>();
        for (GeneralParameterDescriptor param : output.descriptors()) {

            //parameter information
            final CodeType outId = new CodeType(withPrefix?
                    WPSUtils.buildProcessIOIdentifiers(descriptor, param, WPSIO.IOType.OUTPUT) :
                    param.getName().getCode());
            final LanguageStringType outTitle = WPSUtils.buildProcessIOTitle(param, lang);
            final LanguageStringType outAbstract = WPSUtils.buildProcessIODescription(param, lang);
            final List<AdditionalParametersType> outAddParams = WPSUtils.buildAdditionalParams(param);

            DataDescription dataDescription;
            //simple parameter
            if (param instanceof ParameterDescriptor) {
                final ParameterDescriptor paramDesc = (ParameterDescriptor) param;

                //input class
                final Class clazz = paramDesc.getValueClass();

                //BoundingBox type
                if (WPSIO.isSupportedBBoxOutputClass(clazz)) {
                    dataDescription = WPS_SUPPORTED_CRS;

                //Simple object (Integer, double) and Object which need a conversion from String like affineTransform or Geometry
                //Complex type (XML, raster, ...)
                } else if (WPSIO.isSupportedComplexOutputClass(clazz)) {
                    Map<String, Object> userData = null;
                    if (paramDesc instanceof ExtendedParameterDescriptor) {
                        userData = ((ExtendedParameterDescriptor) paramDesc).getUserObject();
                    }
                    dataDescription = WPSUtils.describeComplex(clazz, WPSIO.IOType.OUTPUT, WPSIO.FormChoice.COMPLEX, userData);

                // Litteral type
                } else if (WPSIO.isSupportedLiteralOutputClass(clazz)) {

                    DomainMetadataType dataType = WPSConvertersUtils.createDataType(clazz);
                    DomainMetadataType uom = null;
                    if (paramDesc.getUnit() != null) {
                        uom = WPSUtils.generateUOMs(paramDesc);
                    }
                    // for literal data add default format text/plain
                    Format plain = new Format(WPSMimeType.TEXT_PLAIN.val(), true);
                    dataDescription = new LiteralData(Arrays.asList(plain), dataType, uom, new AnyValue());
                    ((LiteralData) dataDescription).setIsParentOutput(true);

                //Reference type (XML, ...)
                } else if (WPSIO.isSupportedReferenceOutputClass(clazz)) {
                    Map<String, Object> userData = null;
                    if (paramDesc instanceof ExtendedParameterDescriptor) {
                        userData = ((ExtendedParameterDescriptor) paramDesc).getUserObject();
                    }
                    dataDescription = WPSUtils.describeComplex(clazz, WPSIO.IOType.OUTPUT, WPSIO.FormChoice.REFERENCE, userData);

                } else {
                    throw new WPSException("Process output parameter " + outId + " not supported.");
                }

            } else if (param instanceof ParameterDescriptorGroup) {
                /*
                 * If we get a parameterDescriptorGroup, we must expose the
                 * parameters contained in it as one single input. To do so,
                 * we'll expose a feature type input.
                 */
                FeatureType ft = WPSConvertersUtils.descriptorGroupToFeatureType((ParameterDescriptorGroup) param);

                // Input class
                final Class clazz = ft.getClass();
                String xsdName = ft.getName().tip().toString() + ".xsd";
                Path xsdStore = Paths.get(schemaFolder).resolve(xsdName);
                try {
                    WPSUtils.storeFeatureSchema(ft, xsdStore);

                    String publicAddress = schemaURL + "/" + xsdName;
                    HashMap<String, Object> userData = new HashMap<>(1);
                    userData.put(WPSIO.SCHEMA_KEY, publicAddress);
                    dataDescription = WPSUtils.describeComplex(clazz, WPSIO.IOType.OUTPUT, WPSIO.FormChoice.COMPLEX, userData);
                } catch (IOException | JAXBException ex) {
                    throw new WPSException("The schema for parameter " + param.getName().getCode() + "can't be build.");
                }

            } else {
                throw new WPSException("Process output parameter " + outId + " invalid");
            }
            dataOutputs.add(new OutputDescription(outId, outTitle, outAbstract, null, outAddParams, dataDescription));
        }

        OwsContextDescriptionType owsContext = null;
        if (userMap.containsKey("offering.code") || userMap.containsKey("offering.content")) {
            owsContext = new OwsContextDescriptionType((String)userMap.get("offering.code"), (String)userMap.get("offering.content"));
        }

        // que faire de processVersion / supportStorage / statusSupported V1 ???
        String processVersion = "1.0.0";
        ProcessOffering offering = new ProcessOffering(new ProcessDescription(identifier, title, _abstract, null, dataInputs, dataOutputs, processVersion, owsContext));
        offering.getJobControlOptions().addAll(controlOptions);
        offering.getOutputTransmission().addAll(outTransmissions);
        return offering;
    }

    @Override
    public void checkValidInputOuputRequest(Execute request) throws IOParameterException {
        WPSUtils.checkValidInputOuputRequest(descriptor, request, withPrefix);

        // check output transmission
        for (OutputDefinition out : request.getOutput()) {
            if (out.getTransmission() != null && !outTransmissions.contains(out.getTransmission())) {
                throw new IOParameterException(out.getTransmission() + " transmission mode is not allowed for this output", out.getIdentifier());
            }
        }
    }

    @Override
    public List<OutputDefinition> getOutputDefinitions() {
        List<OutputDefinition> outputs = new ArrayList<>();
        for (GeneralParameterDescriptor gpd : descriptor.getOutputDescriptor().descriptors()) {
            if (gpd instanceof ParameterDescriptor) {
                String id = withPrefix? WPSUtils.buildProcessIOIdentifiers(descriptor, gpd, WPSIO.IOType.OUTPUT) : gpd.getName().getCode();
                final OutputDefinition docOutDef = new OutputDefinition(id, false);
                outputs.add(docOutDef);
            }
            //TODO handle sub levels of ParameterDescriptors
        }
        return outputs;
    }

    @Override
    public Callable createRawProcess(boolean async, String version, List<Path> tempFiles, ExecutionInfo execInfo, QuotationInfo quoteInfo, Execute request, String jobId, String quoteId) throws IOParameterException {
        final ParameterValueGroup in = descriptor.getInputDescriptor().createValue();

        List<DataInput> requestInputData = request.getInput();
        if (requestInputData == null) {
            requestInputData = new ArrayList<>();
        }
        final List<GeneralParameterDescriptor> processInputDesc = descriptor.getInputDescriptor().descriptors();
        //Fill input process with there default values
        for (final GeneralParameterDescriptor inputGeneDesc : processInputDesc) {

            if (inputGeneDesc instanceof ParameterDescriptor) {
                final ParameterDescriptor inputDesc = (ParameterDescriptor) inputGeneDesc;

                if (inputDesc.getDefaultValue() != null) {
                    in.parameter(inputDesc.getName().getCode()).setValue(inputDesc.getDefaultValue());
                }
            }
        }

        //Fill process input with data from execute request.
        fillProcessInputFromRequest(version, in, requestInputData, processInputDesc, tempFiles);

        //Give input parameter to the process
        final org.geotoolkit.process.Process process = descriptor.createProcess(in);
        if (process instanceof AbstractProcess) {
            ((AbstractProcess)process).setJobId(jobId);
        }

        if (async) {
            process.addListener(new WPSProcessRawListener(version, execInfo, quoteInfo, request, jobId, quoteId, this));
        }

        applyPreConsumers(process);

        return process;
    }

    @Override
    public Object createRawOutput(String version, String outputId, Object resultI) throws WPSException {
        final ParameterValueGroup result = (ParameterValueGroup) resultI;
        final List<GeneralParameterDescriptor> processOutputDesc = descriptor.getOutputDescriptor().descriptors();
        String outputIdCode = WPSUtils.extractProcessIOCode(descriptor, outputId);

        //Check if it's a valid input identifier and hold it if found.
        ParameterDescriptor outputDescriptor = null;
        for (final GeneralParameterDescriptor processInput : processOutputDesc) {
            if ((processInput.getName().getCode().equals(outputIdCode) || processInput.getName().getCode().equals(outputId)) &&
                processInput instanceof ParameterDescriptor) {
                outputDescriptor = (ParameterDescriptor) processInput;
                outputIdCode = processInput.getName().getCode();
                break;
            }
        }
        if (outputDescriptor == null) {
            throw new IOParameterException("Invalid or unknown output identifier " + outputId + ".", outputId);
        }

        //output value object.
        final List<ParameterValue> values = getValues(result, outputIdCode);
        final List<Object> outputValues = new ArrayList<>();
        for (ParameterValue value : values ) {
            Object outputValue = value.getValue();
            if (outputValue instanceof Geometry) {
                try {

                    final Geometry jtsGeom = (Geometry) outputValue;
                    outputValue = JTStoGeometry.toGML(GML_VERSION.get(version), jtsGeom);

                } catch (FactoryException ex) {
                    throw new WPSException("Error while converting output to GML", ex);
                }
            }

            if (outputValue instanceof Envelope) {
                outputValue = new BoundingBoxType((Envelope) outputValue);
            }
            outputValues.add(outputValue);
        }
        if (outputValues.size() == 1) {
            return outputValues.get(0);
        }
        return outputValues;
    }

    private static List<ParameterValue> getValues(final ParameterValueGroup param, final String descCode) {
        List<ParameterValue> results = new ArrayList<>();
        for (GeneralParameterValue value : param.values()) {
            if (value.getDescriptor().getName().getCode().equals(descCode)) {
                results.add((ParameterValue) value);
            }
        }
        return results;
    }

    @Override
    public Callable createDocProcess(boolean async, String version, List<Path> tempFiles, ExecutionInfo execInfo, QuotationInfo quoteInfo,
            Execute request, String serviceInstance, ProcessSummary procSum, List<DataInput> inputsResponse, List<OutputDefinition> outputsResponse,
            String jobId, String quoteId, Map<String, Object> parameters) throws IOParameterException {

        final ParameterValueGroup in = descriptor.getInputDescriptor().createValue();

        List<DataInput> requestInputData = request.getInput();
        if (requestInputData == null) {
            requestInputData = new ArrayList<>();
        }
        final List<GeneralParameterDescriptor> processInputDesc = descriptor.getInputDescriptor().descriptors();
        //Fill input process with there default values
        for (final GeneralParameterDescriptor inputGeneDesc : processInputDesc) {

            if (inputGeneDesc instanceof ParameterDescriptor) {
                final ParameterDescriptor inputDesc = (ParameterDescriptor) inputGeneDesc;

                if (inputDesc.getDefaultValue() != null) {
                    in.parameter(inputDesc.getName().getCode()).setValue(inputDesc.getDefaultValue());
                }
            }
        }

        //Fill process input with data from execute request.
        fillProcessInputFromRequest(version, in, requestInputData, processInputDesc, tempFiles);

        //Give input parameter to the process
        final org.geotoolkit.process.Process process = descriptor.createProcess(in);
        if (process instanceof AbstractProcess) {
            ((AbstractProcess)process).setJobId(jobId);
        }

        if (async) {
            process.addListener(new WPSProcessListener(version, execInfo, quoteInfo, request, serviceInstance, procSum, inputsResponse, outputsResponse, jobId, quoteId, parameters, this));
        }

        applyPreConsumers(process);

        return process;
    }


    /**
     * For each inputs in Execute request, this method will find corresponding {@link ParameterDescriptor ParameterDescriptor} input in the
     * process and fill the {@link ParameterValueGroup ParameterValueGroup} with the data.
     *
     * @param in
     * @param requestInputData
     * @param processInputDesc
     * @param files
     * @throws CstlServiceException
     */
    private void fillProcessInputFromRequest(final String version, final ParameterValueGroup in, final List<DataInput> requestInputData,
            final List<GeneralParameterDescriptor> processInputDesc, List<Path> files) throws IOParameterException {

        ArgumentChecks.ensureNonNull("in", in);
        ArgumentChecks.ensureNonNull("requestInputData", requestInputData);
        Set<String> alreadySet = new HashSet();
        for (final DataInput inputRequest : requestInputData) {

            if (inputRequest.getId() == null || inputRequest.getId().isEmpty()) {
                throw new IOParameterException("Empty input Identifier.", null);
            }

            final String inputId = inputRequest.getId();
            String inputIdCode = WPSUtils.extractProcessIOCode(descriptor, inputId);

            //Check if it's a valid input identifier and hold it if found.
            GeneralParameterDescriptor inputDescriptor = null;
            for (final GeneralParameterDescriptor processInput : processInputDesc) {
                if (processInput.getName().getCode().equals(inputIdCode) ||
                    processInput.getName().getCode().equals(inputId)) {
                    inputDescriptor = processInput;
                    inputIdCode = processInput.getName().getCode();
                    break;
                }
            }
            if (inputDescriptor == null) {
                throw new IOParameterException("Invalid or unknown input identifier " + inputId + ".", inputId);
            }

            boolean isReference = false;
            boolean isBBox = false;
            boolean isComplex = false;
            boolean isLiteral = false;

            if (inputRequest.getReference() != null) {
                isReference = true;
            } else {
                if (inputRequest.getData() != null) {

                    final Data dataType = inputRequest.getData();
                    if (dataType.getBoundingBoxData() != null) {
                        isBBox = true;

                    // issue here : we don't need to have a literal value. the value can be directly in content
                    // TODO see dirty patch added below
                    } else if (dataType.getLiteralData() != null) {
                        isLiteral = true;
                    } else {
                        isComplex = true;
                    }
                } else {
                    throw new IOParameterException("Input doesn't have data or reference.", inputId);
                }
            }

            /*
             * Get expected input Class from the process input
             */
            Class expectedClass;
            if(inputDescriptor instanceof ParameterDescriptor) {
                expectedClass = ((ParameterDescriptor)inputDescriptor).getValueClass();
            } else {
                expectedClass = Feature.class;
            }

            // quick dirty patch for literal values without LiteralData
            if (WPSIO.isSupportedLiteralInputClass(expectedClass) && inputRequest.getData().getContent().size() == 1 &&
                inputRequest.getData().getContent().get(0) instanceof String) {
                isLiteral = true;
                isComplex = false;
            }

            Object dataValue = null;

            /**
             * Handle referenced input data.
             */
            if (isReference) {

                //Check if the expected class is supported for reference using
                if (!WPSIO.isSupportedReferenceInputClass(expectedClass)) {
                    throw new IOParameterException("The input" + inputId + " can't handle reference.", inputId);
                }
                final Reference requestedRef = inputRequest.getReference();
                if (requestedRef.getHref() == null) {
                    throw new IOParameterException("Invalid reference input : href can't be null.", inputId);
                }
                try {
                    dataValue = WPSConvertersUtils.convertFromReference(requestedRef, expectedClass);
                } catch (UnconvertibleObjectException ex) {
                    throw new IOParameterException("Error during conversion of reference input : "  + inputId + " : " + ex.getMessage(), ex, inputId);
                }

                if (dataValue instanceof File && files != null) {
                    files.add(((File) dataValue).toPath());
                }

                if (dataValue instanceof Path && files != null) {
                    files.add((Path) dataValue);
                }
            }

            /**
             * Handle Bbox input data.
             */
            if (isBBox) {
                final BoundingBox bBox = inputRequest.getData().getBoundingBoxData();
                List<Double> tmpLc = bBox.getLowerCorner();
                List<Double> tmpUc = bBox.getUpperCorner();
                if (tmpLc.isEmpty()) {
                    throw new IOParameterException("Invalid bbox: no lower corner given.", false, true, inputId);
                } else if (tmpUc.isEmpty()) {
                    throw new IOParameterException("Invalid bbox: no upper corner given.", false, true, inputId);
                } else if (tmpLc.size() != tmpUc.size()) {
                    throw new IOParameterException(String.format(
                            "Invalid bbox: Lower corner and upper corner dimension mismatch.%nLower=%d, Upper=%d",
                            tmpLc.size(), tmpUc.size()
                    ), inputId);
                }

                final GeneralEnvelope env;
                try {
                    env = new GeneralEnvelope(
                            tmpLc.stream().mapToDouble(d -> d).toArray(),
                            tmpUc.stream().mapToDouble(d -> d).toArray()
                    );
                } catch (NullPointerException e) {
                    throw new IOParameterException("A null value has been found in bbox ordinates", inputId);
                }

                String crs = bBox.getCrs();
                /* WPS 2 specification states that CRS is mandatory. Text can be found in section 7.3.3.2 (BoundingBox Values):
                 * "Values for bounding boxes are specified in the BoundingBox data type from OWS Common [OGC 06-121r9].
                 * For consistency with the BoundingBoxData description, the specification of a CRS is mandatory."
                 * Link: http://docs.opengeospatial.org/is/14-065/14-065.html#30
                 *
                 */
                if (crs == null || (crs =crs.trim()).isEmpty()) {
                    throw new IOParameterException("A CRS must be specified for input bounding box (see WPS Spec 2.0, section 7.3.3.2)", false, true, inputId);
                }

                try {
                    env.setCoordinateReferenceSystem(CRS.forCode(crs));
                } catch (FactoryException ex) {
                    throw new IOParameterException("Invalid data input : CRS not supported: " + crs, ex, true, inputId);
                }

                dataValue = env;
            }

            /**
             * Handle Complex input data.
             */
            if (isComplex) {
                //Check if the expected class is supported for complex using
                if (!WPSIO.isSupportedComplexInputClass(expectedClass)) {
                    throw new IOParameterException("Complex value expected", inputId);
                }

                if (inputRequest.getData().getContent() == null || inputRequest.getData().getContent().size() <= 0) {
                    throw new IOParameterException("Missing data input value.", inputId);

                } else {

                    try {
                        dataValue = WPSConvertersUtils.convertFromComplex(version, inputRequest.getData(), expectedClass);
                    } catch (IllegalArgumentException ex) {
                        throw new IOParameterException("Error during conversion of complex input " + inputId + " : " + ex.getMessage(), ex, inputId);
                    }
                }
            }

            /**
             * Handle Literal input data.
             */
            if (isLiteral) {
                //Check if the expected class is supported for literal using
                if (!WPSIO.isSupportedLiteralInputClass(expectedClass)) {
                    throw new IOParameterException("Literal value expected", inputId);
                }

                if(!(inputDescriptor instanceof ParameterDescriptor)) {
                    throw new IOParameterException("Invalid parameter type.", inputId);
                }

                final LiteralValue literal = inputRequest.getData().getLiteralData();
                if (literal != null) {
                    final String data = literal.getValue();

                    final Unit paramUnit = ((ParameterDescriptor)inputDescriptor).getUnit();
                    if (paramUnit != null) {
                        final Unit requestedUnit = Units.valueOf(literal.getUom());
                        final UnitConverter converter = requestedUnit.getConverterTo(paramUnit);
                        dataValue = converter.convert(Double.valueOf(data));

                    } else {
                        try {
                            dataValue = WPSConvertersUtils.convertFromString(data, expectedClass);
                        } catch (UnconvertibleObjectException ex) {
                            throw new IOParameterException("Error during conversion of literal input : " +  inputId + " : " + ex.getMessage(), ex, inputId);
                        }
                    }

                // dirty patch, la suite
                } else {
                    try {
                        final String data = (String) inputRequest.getData().getContent().get(0);
                        dataValue = WPSConvertersUtils.convertFromString(data, expectedClass);
                    } catch (UnconvertibleObjectException ex) {
                        throw new IOParameterException("Error during conversion of literal input : " + inputId + " : " + ex.getMessage(), ex, inputId);
                    }
                }
            }

            try {
                if(inputDescriptor instanceof ParameterDescriptor) {
                    if (alreadySet.contains(inputIdCode)) {
                        ParameterValue newOccurence = ((ParameterDescriptor)inputDescriptor).createValue();
                        newOccurence.setValue(dataValue);
                        in.values().add(newOccurence);
                    } else {
                        in.parameter(inputIdCode).setValue(dataValue);
                        alreadySet.add(inputIdCode);
                    }

                } else if(inputDescriptor instanceof ParameterDescriptorGroup && dataValue instanceof Feature) {
                    WPSConvertersUtils.featureToParameterGroup(version,
                            (Feature)dataValue,
                            in.addGroup(inputIdCode));
                } else {
                    throw new Exception();
                }
            } catch (Exception ex) {
                throw new IOParameterException("Invalid data input value.", ex, inputId);
            }
        }
    }

    @Override
    public List<DataOutput> createDocOutput(String version, List<? extends OutputDefinition> wantedOutputs, Object resultI, Map<String, Object> parameters, boolean progressing) throws WPSException {
        if (resultI == null) {
            throw new WPSException("Empty process result.");
        }
        final ParameterValueGroup result = (ParameterValueGroup) resultI;

        List<DataOutput> outputs = new ArrayList<>();
        for (final OutputDefinition outputsRequest : wantedOutputs) {

            if (outputsRequest.getIdentifier() == null || outputsRequest.getIdentifier().isEmpty()) {
                throw new IOParameterException("Empty output Identifier.", null);
            }

            final String outputId = outputsRequest.getIdentifier();
            String outputIdCode = WPSUtils.extractProcessIOCode(descriptor, outputId);

            final List<GeneralParameterDescriptor> processOutputDesc = descriptor.getOutputDescriptor().descriptors();
            //Check if it's a valid input identifier and hold it if found.
            GeneralParameterDescriptor outputDescriptor = null;
            for (final GeneralParameterDescriptor processInput : processOutputDesc) {
                if (processInput.getName().getCode().equals(outputIdCode) ||
                    processInput.getName().getCode().equals(outputId)) {
                    outputDescriptor = (ParameterDescriptor) processInput;
                    outputIdCode = processInput.getName().getCode();
                    break;
                }
            }
            if (outputDescriptor == null) {
                throw new IOParameterException("Invalid or unknown output identifier " + outputId + ".", outputId);
            }

            if (outputDescriptor instanceof ParameterDescriptor) {
                //output value object.
                final List<ParameterValue> values = getValues(result, outputIdCode);
                for (ParameterValue value : values ) {
                    final Object outputValue = value.getValue();
                    if (!progressing || (progressing && outputValue != null)) {

                        outputs.add(createDocumentResponseOutput(version, descriptor, (ParameterDescriptor) outputDescriptor, outputsRequest, outputValue, parameters));
                    }
                }
            } else if (outputDescriptor instanceof ParameterDescriptorGroup) {
                /**
                 * TODO: Treat ParameterValueGroup for outputs.
                 */
                throw new IOParameterException("Invalid or unknown output identifier " + outputId + ".", outputId);
            } else {
                throw new IOParameterException("Invalid or unknown output identifier " + outputId + ".", outputId);
            }

        }//end foreach wanted outputs
        return outputs;
    }

    /**
     * Create {@link DataOutput output} object for one requested output.
     *
     * @param version
     * @param outputDescriptor
     * @param requestedOutput
     * @param outputValue
     * @param parameters
     * @return
     * @throws CstlServiceException
     */
    private static DataOutput createDocumentResponseOutput(final String version, final ProcessDescriptor processDesc, final ParameterDescriptor outputDescriptor, final OutputDefinition requestedOutput,
            final Object outputValue, final Map<String, Object> parameters) throws WPSException {

        final String outputIdentifier = requestedOutput.getIdentifier();
        final String outputIdentifierCode = WPSUtils.extractProcessIOCode(processDesc, outputIdentifier);

        //support custom title/abstract.
        final String titleOut = requestedOutput.getTitle() != null ? requestedOutput.getTitle().getValue() : WPSUtils.capitalizeFirstLetterStr(outputIdentifierCode);
        String abstractOut = requestedOutput.getAbstract()!= null ? requestedOutput.getAbstract().getValue() : null;
        if (abstractOut == null) {
            if (outputDescriptor.getRemarks() != null) {
                abstractOut = WPSUtils.capitalizeFirstLetterStr(outputDescriptor.getRemarks().toString());
            } else {
                abstractOut = WPSUtils.capitalizeFirstLetterStr("No description available");
            }
        }

        if (DataTransmissionMode.REFERENCE.equals(requestedOutput.getTransmission())) {
            final Reference ref = createReferenceOutput(version, requestedOutput, outputValue, parameters);
            return new DataOutput(outputIdentifier, titleOut, abstractOut, ref);

        } else {
            final Class outClass = outputDescriptor.getValueClass(); // output class

            Object outputData = null;

            if (WPSIO.isSupportedBBoxOutputClass(outClass)) {

                outputData = new BoundingBoxType((org.opengis.geometry.Envelope) outputValue);

            } else if (WPSIO.isSupportedComplexOutputClass(outClass)) {

                try {
                    Object complex = null;
                    if (outputValue instanceof GridCoverage && requestedOutput.getMimeType().equals(WPSMimeType.OGC_WMS.val())) {
                        if (parameters.get(WMS_SUPPORTED).equals(Boolean.TRUE)) {
                            //add output identifier to layerName
                            parameters.put(WPSConvertersUtils.WMS_LAYER_NAME, parameters.get(WPSConvertersUtils.WMS_LAYER_NAME)+"_"+outputIdentifierCode);
                            complex = WPSConvertersUtils.convertToWMSComplex(
                                    version,
                                    outputValue,
                                    requestedOutput.getMimeType(),
                                    requestedOutput.getEncoding(),
                                    requestedOutput.getSchema(),
                                    parameters);
                            WPSUtils.restartWMS(parameters);
                        } else {
                            LOGGER.log(Level.WARNING, "Can\'t publish {0} value in a WMS.", outputIdentifier);
                        }

                    } else {
                        complex = WPSConvertersUtils.convertToComplex(
                            version,
                            outputValue,
                            requestedOutput.getMimeType(),
                            requestedOutput.getEncoding(),
                            requestedOutput.getSchema(),
                            parameters);
                    }

                    outputData = complex;

                } catch (UnconvertibleObjectException ex) {
                    LOGGER.log(Level.WARNING, "Error during conversion of complex output {0}.", outputIdentifier);
                    throw new WPSException(ex.getMessage(), ex);
                }

            } else if (WPSIO.isSupportedLiteralOutputClass(outClass)) {

                String dataType = WPSConvertersUtils.getDataTypeString(version, outClass);
                String value = WPSConvertersUtils.convertToString(outputValue);
                String uom = null;
                if (outputDescriptor.getUnit() != null) {
                    uom = outputDescriptor.getUnit().toString();
                }
                outputData = new LiteralValue(value, dataType, uom);


            } else {
                throw new IOParameterException("Process output parameter invalid", outputIdentifier);
            }
            final Data data;
            if (outputData instanceof Data) {
                data = (Data) outputData;
            } else {
                data = new Data(outputData);
            }
            return new DataOutput(outputIdentifier, titleOut, abstractOut, data);
        }
    }

    /**
     * Create reference output.
     *
     * @param clazz
     * @param requestedOutput
     * @param outputValue
     * @param parameters
     * @return
     * @throws CstlServiceException
     */
    private static Reference createReferenceOutput(final String version, final OutputDefinition requestedOutput,
            final Object outputValue, final Map<String, Object> parameters) throws WPSException {

        try {
           return (Reference) WPSConvertersUtils.convertToReference(
                    version,
                    outputValue,
                    requestedOutput.getMimeType(),
                    requestedOutput.getEncoding(),
                    requestedOutput.getSchema(),
                    parameters,
                    WPSIO.IOType.OUTPUT);

        } catch (UnconvertibleObjectException ex) {
            throw new WPSException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getLayerName() {
        return  descriptor.getIdentifier().getAuthority().getTitle().toString() + "." + descriptor.getIdentifier().getCode();
    }

    @Override
    public List<JobControlOptions> getJobControlOptions() {
        return controlOptions;
    }

    @Override
    public Quotation quote(Execute request) throws WPSException {
        final Quotation result = new Quotation();

        final List<DataInput> requestInputData = request.getInput();
        final List<GeneralParameterDescriptor> processInputDesc = descriptor.getInputDescriptor().descriptors();

        long estimatedTime = 60000; // initialized at 1 minute.
        double price = 10.0;
        double megaBytesPrice = 0.001;
        double megaBytesTime = 10; // 10 ms by mo

        StringBuilder details = new StringBuilder("Basic price 10 euros.");
        for (final DataInput inputRequest : requestInputData) {

            if (inputRequest.getId() == null || inputRequest.getId().isEmpty()) {
                throw new IOParameterException("Empty input Identifier.", null);
            }

            final String inputId = inputRequest.getId();
            String inputIdCode = WPSUtils.extractProcessIOCode(descriptor, inputId);

            //Check if it's a valid input identifier and hold it if found.
            GeneralParameterDescriptor inputDescriptor = null;
            for (final GeneralParameterDescriptor processInput : processInputDesc) {
                if (processInput.getName().getCode().equals(inputIdCode) ||
                    processInput.getName().getCode().equals(inputId)) {
                    inputDescriptor = processInput;
                    break;
                }
            }
            if (inputDescriptor == null) {
                throw new IOParameterException("Invalid or unknown input identifier " + inputId + ".", inputId);
            }

            boolean isReference = false;
            boolean isBBox = false;
            boolean isComplex = false;
            boolean isLiteral = false;

            if (inputRequest.getReference() != null) {
                isReference = true;
            } else {
                if (inputRequest.getData() != null) {

                    final Data dataType = inputRequest.getData();
                    if (dataType.getBoundingBoxData() != null) {
                        isBBox = true;

                    // issue here : we don't need to have a literal value. the value can be directly in content
                    // TODO see dirty patch added below
                    } else if (dataType.getLiteralData() != null) {
                        isLiteral = true;
                    } else {
                        isComplex = true;
                    }
                } else {
                    throw new IOParameterException("Input doesn't have data or reference.", inputId);
                }
            }

            /*
             * Get expected input Class from the process input
             */
            Class expectedClass;
            if(inputDescriptor instanceof ParameterDescriptor) {
                expectedClass = ((ParameterDescriptor)inputDescriptor).getValueClass();
            } else {
                expectedClass = Feature.class;
            }

            // quick dirty patch for literal values without LiteralData
            if (WPSIO.isSupportedLiteralInputClass(expectedClass) && inputRequest.getData().getContent().size() == 1 &&
                inputRequest.getData().getContent().get(0) instanceof String) {
                isLiteral = true;
                isComplex = false;
            }

            Object dataValue = null;

            /**
             * Handle referenced input data.
             */
            if (isReference) {

                //Check if the expected class is supported for reference using
                if (!WPSIO.isSupportedReferenceInputClass(expectedClass)) {
                    throw new IOParameterException("The input" + inputId + " can't handle reference.", inputId);
                }
                final Reference requestedRef = inputRequest.getReference();
                if (requestedRef.getHref() == null) {
                    throw new IOParameterException("Invalid reference input : href can't be null.", inputId);
                }
                try {
                    dataValue = WPSConvertersUtils.convertFromReference(requestedRef, expectedClass);
                } catch (UnconvertibleObjectException ex) {
                    throw new IOParameterException("Error during conversion of reference input : "  + inputId + " : " + ex.getMessage(), ex, inputId);
                }

                if (dataValue instanceof File) {
                    System.out.println(((File) dataValue).toPath()); // TODO ?
                }

                if (dataValue instanceof Path) {
                    System.out.println((Path) dataValue);  // TODO ?
                }
            }

            /**
             * Handle Bbox input data.
             */
            if (isBBox) {
                throw new IOParameterException("Quoting for BBOX input is not yet imlplemented.", true, false, inputId);
            }

            /**
             * Handle Complex input data.
             */
            if (isComplex) {
                //Check if the expected class is supported for complex using
                if (!WPSIO.isSupportedComplexInputClass(expectedClass)) {
                    throw new IOParameterException("Complex value expected", inputId);
                }

                if (inputRequest.getData().getContent() == null || inputRequest.getData().getContent().size() <= 0) {
                    throw new IOParameterException("Missing data input value.", inputId);

                } else {

                    try {
                        dataValue = WPSConvertersUtils.convertFromComplex("2.0", inputRequest.getData(), expectedClass);
                    } catch (IllegalArgumentException ex) {
                        throw new IOParameterException("Error during conversion of complex input " + inputId + " : " + ex.getMessage(), ex, inputId);
                    }
                }
            }

            /**
             * Handle Literal input data.
             */
            if (isLiteral) {
               //Check if the expected class is supported for literal using
                if (!WPSIO.isSupportedLiteralInputClass(expectedClass)) {
                    throw new IOParameterException("Literal value expected", inputId);
                }

                if(!(inputDescriptor instanceof ParameterDescriptor)) {
                    throw new IOParameterException("Invalid parameter type.", inputId);
                }

                final LiteralValue literal = inputRequest.getData().getLiteralData();
                if (literal != null) {
                    final String data = literal.getValue();

                    final Unit paramUnit = ((ParameterDescriptor)inputDescriptor).getUnit();
                    if (paramUnit != null) {
                        final Unit requestedUnit = Units.valueOf(literal.getUom());
                        final UnitConverter converter = requestedUnit.getConverterTo(paramUnit);
                        dataValue = converter.convert(Double.valueOf(data));

                    } else {
                        try {
                            dataValue = WPSConvertersUtils.convertFromString(data, expectedClass);
                        } catch (UnconvertibleObjectException ex) {
                            throw new IOParameterException("Error during conversion of literal input : " +  inputId + " : " + ex.getMessage(), ex, inputId);
                        }
                    }

                // dirty patch, la suite
                } else {
                    try {
                        final String data = (String) inputRequest.getData().getContent().get(0);
                        dataValue = WPSConvertersUtils.convertFromString(data, expectedClass);
                    } catch (UnconvertibleObjectException ex) {
                        throw new IOParameterException("Error during conversion of literal input : " + inputId + " : " + ex.getMessage(), ex, inputId);
                    }
                }
            }

            try {
                long size;
                if (dataValue instanceof File) {
                    size = ((File) dataValue).length();
                } else if (dataValue instanceof Path) {
                    size = Files.size((Path) dataValue);
                } else if (dataValue instanceof URI) {
                    size = getFileSize(((URI) dataValue).toURL());
                } else {
                    size = -1;
                }

                if (size != -1) {
                    Integer megabytes = Math.round((size / 1024) / 1024);
                    double inputPrice = (megaBytesPrice * megabytes);
                    price = price + inputPrice;
                    details.append(" - input file ").append(megabytes).append("mo : ").append(NUMFORM.format(inputPrice)).append("euros.");

                    estimatedTime = estimatedTime + (long)(megaBytesTime * megabytes);
                }

            } catch(IOException ex) {
                LOGGER.log(Level.WARNING, "Error while estimating input size", ex);
            }

        }

        details.append(" Total: ").append(NUMFORM.format(price)).append(" euros.");

        result.setId(UUID.randomUUID().toString());
        result.setTitle(descriptor.getDisplayName().toString());
        result.setDescription(descriptor.getProcedureDescription().toString());
        result.setEstimatedTime(Duration.ofMillis(estimatedTime).toString());
        result.setPrice(Double.valueOf(NUMFORM.format(price)));
        result.setCurrency("EUR");
        result.setDetails(details.toString());
        result.setProcessId(request.getIdentifier().getValue());
        result.setProcessParameters(request);
        return result;
    }

    public long getFileSize(URL url) {
        try {
            WPSURLUtils.authenticate(url.toURI());
        } catch (URISyntaxException ex) {}

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            return conn.getContentLengthLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
