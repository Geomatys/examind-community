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
package com.examind.wps;

import com.examind.wps.api.IOParameterException;
import com.examind.wps.api.ProcessPreConsumer;
import com.examind.wps.api.UnknowJobException;
import com.examind.wps.api.UnknowQuotationException;
import com.examind.wps.api.WPSException;
import com.examind.wps.api.WPSProcess;
import com.examind.wps.api.WPSWorker;
import com.examind.wps.component.GeotkProcess;
import com.examind.wps.util.SimpleJobExecutor;
import static com.examind.wps.util.WPSConstants.*;
import com.examind.wps.util.WPSUtils;
import com.examind.wps.util.WPSConfigurationUtils;
import org.geotoolkit.atom.xml.Link;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.sis.util.Version;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import static org.constellation.api.QueryConstants.SERVICE_PARAMETER;
import org.constellation.api.ServiceDef;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.config.wps.Process;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.ws.AbstractWorker;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.IWSEngine;
import org.geotoolkit.ows.xml.AbstractCapabilitiesCore;
import org.geotoolkit.ows.xml.AcceptVersions;
import org.geotoolkit.ows.xml.ExceptionResponse;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STORAGE_NOT_SUPPORTED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;
import org.geotoolkit.ows.xml.RequestBase;
import org.geotoolkit.ows.xml.Sections;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.ows.xml.v200.ExceptionReport;
import org.geotoolkit.parameter.Parameters;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;
import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.util.Exceptions;
import org.geotoolkit.util.StringUtilities;
import org.geotoolkit.wps.client.WPSVersion;
import org.geotoolkit.wps.converters.WPSConvertersUtils;
import org.geotoolkit.wps.xml.WPSMarshallerPool;
import org.geotoolkit.wps.xml.v200.Capabilities;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.DataOutput;
import org.geotoolkit.wps.xml.v200.DescribeProcess;
import org.geotoolkit.wps.xml.v200.Dismiss;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.GetCapabilities;
import org.geotoolkit.wps.xml.v200.GetResult;
import org.geotoolkit.wps.xml.v200.GetStatus;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.process.Registry;
import org.constellation.dto.process.RegistryList;
import org.constellation.exception.ConstellationException;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.process.dynamic.ExamindDynamicProcessFactory;
import org.constellation.process.dynamic.cwl.RunCWLDescriptor;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.ws.UnauthorizedException;
import org.geotoolkit.client.CapabilitiesException;
import org.geotoolkit.gml.xml.Envelope;
import static org.geotoolkit.ows.xml.OWSExceptionCode.OPERATION_NOT_SUPPORTED;
import org.geotoolkit.ows.xml.v200.AdditionalParameter;
import org.geotoolkit.ows.xml.v200.AdditionalParametersType;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Constant;
import static org.geotoolkit.processing.chain.model.Element.BEGIN;
import static org.geotoolkit.processing.chain.model.Element.END;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.geotoolkit.processing.chain.model.ParameterFormat;
import org.geotoolkit.wps.client.WebProcessingClient;
import org.geotoolkit.wps.client.process.WPSProcessingRegistry;
import org.geotoolkit.wps.xml.v200.Bill;
import org.geotoolkit.wps.xml.v200.BillList;
import org.geotoolkit.wps.xml.v200.BoundingBoxData;
import org.geotoolkit.wps.xml.v200.ComplexData;
import org.geotoolkit.wps.xml.v200.DataDescription;
import org.geotoolkit.wps.xml.v200.DataTransmissionMode;
import org.geotoolkit.wps.xml.v200.Deploy;
import org.geotoolkit.wps.xml.v200.DeployResult;
import org.geotoolkit.wps.xml.v200.Format;
import org.geotoolkit.wps.xml.v200.InputDescription;
import org.geotoolkit.wps.xml.v200.JobControlOptions;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.ProcessOffering;
import org.geotoolkit.wps.xml.v200.ProcessOfferings;
import org.geotoolkit.wps.xml.v200.ProcessSummary;
import org.geotoolkit.wps.xml.v200.Result;
import org.geotoolkit.wps.xml.v200.Status;
import org.geotoolkit.wps.xml.v200.StatusInfo;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.CodeList;
import org.opengis.util.NoSuchIdentifierException;
import org.geotoolkit.wps.xml.v200.LiteralData;
import org.geotoolkit.wps.xml.v200.OutputDescription;
import org.geotoolkit.wps.xml.v200.ProcessDescription;
import org.geotoolkit.wps.xml.v200.ProcessDescriptionChoiceType;
import org.geotoolkit.wps.xml.v200.Quotation;
import org.geotoolkit.wps.xml.v200.QuotationList;
import org.geotoolkit.wps.xml.v200.Undeploy;
import org.geotoolkit.wps.xml.v200.UndeployResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;


/**
 * WPS worker.Compute response of getCapabilities, DescribeProcess and Execute requests.
 *
 * TODO: change how processes are handled, to allow a better abstraction. To do
 * so, we should continue the work started in package com.examind.wps.api, whose
 * aim is to provide an interoperability layer between the worker and the
 * processing sources (geotk process, wps proxy, etc.).
 *
 * @author Quentin Boileau
 */
@Named("WPSWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DefaultWPSWorker extends AbstractWorker implements WPSWorker {

    public static final String PATH_PRODUCTS_NAME = "products";
    public static final String PATH_SCHEMA_NAME = "schemas";

    /**
     * Timeout in seconds use to kill long process execution in synchronous mode.
     */
    private static final int TIMEOUT = 120;

    /**
     * Try to create temporary directory.
     */
    private boolean supportStorage;

    /**
     * Path where output file will be saved.
     */
    private URI productFolderPath;

    /**
     * Query URL to retrieve datas.
     */
    private String productURL;

    private URI schemaFolder;
    private String schemaURL;

    /**
     * WMS link attributes
     */
    private boolean wmsSupported = false;
    private String wmsInstanceName;
    private String wmsInstanceURL;
    private String wmsProviderId;
    private String fileCoverageStorePath;

    /**
     * List of process descriptors available.
     */
    private final Map<CodeType, WPSProcess> processList = new LinkedHashMap<>();

    private final ExecutionInfo execInfo = new ExecutionInfo();

    private final QuotationInfo quoteInfo = new QuotationInfo();

    @Inject SimpleJobExecutor jobExecutor;

    @Autowired(required=false)
    private Collection<ProcessPreConsumer> preConsumers;

    @Autowired
    private IProcessBusiness processBusiness;

    /**
     * Constructor.
     *
     * @param id
     */
    public DefaultWPSWorker(final String id) {
        super(id, ServiceDef.Specification.WPS);
        ProcessContext context = null;
        try {
            final Object obj = serviceBusiness.getConfiguration("WPS", id);
            if (obj instanceof ProcessContext) {
                context = (ProcessContext) obj;
                applySupportedVersion();
                isStarted = true;
            } else {
                startError = "The process context File does not contain a ProcessContext object";
                isStarted = false;
                LOGGER.log(Level.WARNING, "The worker ({0}) is not working!\nCause: " + startError, id);
            }
        } catch (ConfigurationException ex) {
            startError = ex.getMessage();
            isStarted = false;
            LOGGER.log(Level.WARNING, "The worker ({0}) is not working!\nCause: " + startError, id);
        } catch (CstlServiceException ex) {
            startError = "Error applying supported versions : " + ex.getMessage();
            isStarted = false;
            LOGGER.log(Level.WARNING, "The worker ({0}) is not working!\nCause: " + startError, id);
        }

        this.supportStorage = false;
        this.productURL = null; //initialize on WPS execute request.
        this.productFolderPath = null;

        // default output directory
        Path configPath = ConfigDirectory.getInstanceDirectory("wps", id);

        // custom override of the output directory
        if (context != null && context.getOutputDirectory() != null && !context.getOutputDirectory().isEmpty()) {
            try {
                configPath = Paths.get(new URI(context.getOutputDirectory()));
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.WARNING,  "Error while reading custom output directory", ex);
            }
        }

        //prepare folder where job products will be stored
        try {
            Files.createDirectories(configPath.resolve(PATH_PRODUCTS_NAME));
            productFolderPath = configPath.resolve(PATH_PRODUCTS_NAME).toUri();
            final Path schemaLoc = configPath.resolve(PATH_PRODUCTS_NAME).resolve(PATH_SCHEMA_NAME);
            schemaFolder = Files.createDirectories(schemaLoc).toUri();
            supportStorage = true;
        } catch (Exception e) {
            this.supportStorage = false;
            LOGGER.log(Level.WARNING, "The worker ({0}) does not support storage!\n" +
                    "Cause: Error during WPS WebDav service creation : {1}", new Object[]{id, e.getMessage()});
            e.printStackTrace();
        }

        //WMS link
        if (context != null) {
            if (context.getWmsInstanceName() != null) {
                this.wmsInstanceName = context.getWmsInstanceName();

                if (context.getFileCoverageProviderId() != null) {
                    this.wmsProviderId = context.getFileCoverageProviderId();

                    DataProvider provider = null;
                    try {
                        provider = DataProviders.getProvider(this.wmsProviderId);
                    } catch (ConfigurationException ex) {
                        LOGGER.log(Level.WARNING, "error while retrieving WMS provider", ex);
                    }

                    final IWSEngine wsengine = SpringHelper.getBean(IWSEngine.class);
                    if(wsengine.serviceInstanceExist("WMS", wmsInstanceName) || provider == null) {
                        startError = "Linked WMS instance is not found or FileCoverageStore not defined.";
                    } else {
                        try {
                            final ParameterValue pathParam = (ParameterValue) Parameters.search(provider.getSource(), "path", 3).get(0);
                            this.fileCoverageStorePath = ((URL) pathParam.getValue()).getPath();
                            final Path dir = Paths.get(fileCoverageStorePath);
                            if (!Files.exists(dir)) {
                                Files.createDirectories(dir);
                            }
                            this.wmsSupported = true;
                        } catch (IOException ex) {
                            startError = "Linked WMS storage folder failed to initialize.";
                        }
                    }

                } else {
                    startError = "Linked provider identifier name is not defined.";
                }
            } else {
                startError = "Linked WMS instance name is not defined.";
            }
        }

        if (!wmsSupported) {
            LOGGER.log(Level.WARNING, "The WPS worker ({0}) don\'t support WMS outputs : \n " + startError, id);
        }

        fillProcessList(context);

        if (isStarted) {
            LOGGER.log(Level.INFO, "WPS worker {0} running", id);
        }
    }

    @PostConstruct
    private void appyPreConsumers() {
        if (preConsumers != null) {
            Stream<GeotkProcess> stream = (Stream) processList.values().stream()
                .filter(GeotkProcess.class::isInstance);

            stream.forEach(p -> p.setPreConsumers(preConsumers));
        }
    }

    /**
     * Create process list from context file.
     */
    private void fillProcessList(ProcessContext context) {
        if (context == null || context.getProcesses() == null) {
            return;
        }
        final Map<ProcessDescriptor, Process> overridenProperties = new HashMap<>();
        final List<ProcessDescriptor> linkedDescriptors = new ArrayList<>();
        // Load all processes from all factory
        if (Boolean.TRUE.equals(context.getProcesses().getLoadAll())) {
            LOGGER.info("Loading all process");
            final Iterator<ProcessingRegistry> factoryIte = ProcessFinder.getProcessFactories();
            while (factoryIte.hasNext()) {
                final ProcessingRegistry factory = factoryIte.next();
                linkedDescriptors.addAll(factory.getDescriptors());
            }
        } else {
            for (final ProcessFactory processFactory : context.getProcessFactories()) {
                String authorityCode = processFactory.getAutorityCode();
                ProcessingRegistry factory = null;
                if (authorityCode.startsWith("http")) {
                    try {
                        final WebProcessingClient client = new WebProcessingClient(new URL(authorityCode));
                        factory = new WPSProcessingRegistry(client);

                    } catch (MalformedURLException | CapabilitiesException | RuntimeException ex) {
                        LOGGER.log(Level.WARNING, "Unable to open distant WPS:" + authorityCode, ex);
                    }
                } else {
                    factory = ProcessFinder.getProcessFactory(processFactory.getAutorityCode());
                }
                if (factory != null) {
                    if (Boolean.TRUE.equals(processFactory.getLoadAll())) {
                        linkedDescriptors.addAll(factory.getDescriptors());
                    } else {
                        for (final Process process : processFactory.getInclude().getProcess()) {
                            try {
                                final ProcessDescriptor descriptor = factory.getDescriptor(process.getId());
                                if (descriptor != null) {
                                    linkedDescriptors.add(descriptor);
                                    overridenProperties.put(descriptor, process);
                                }
                            } catch (NoSuchIdentifierException | RuntimeException  ex) {
                                LOGGER.log(Level.WARNING, "Unable to find a process named:" + process.getId() + " in factory " + processFactory.getAutorityCode(), ex);
                            }
                        }
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No process factory found for authorityCode:{0}", processFactory.getAutorityCode());
                }
            }
        }

        processList.clear();
        boolean activatePrefix = true;
        try {
            final String tmpPrefixOpt = getProperty(ProcessContext.PREFIX_IDS);
            if (tmpPrefixOpt != null && tmpPrefixOpt.trim().toLowerCase().startsWith("f")) {
                activatePrefix = false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot detect prfix option", e);
        }

        for (ProcessDescriptor descriptor : linkedDescriptors) {
            if (WPSUtils.isSupportedProcess(descriptor)) {
                WPSProcess proc = new GeotkProcess(descriptor, schemaFolder, schemaURL, overridenProperties.get(descriptor), activatePrefix);
                try {
                    proc.checkForSchemasToStore();
                    processList.put(proc.getIdentifier(), proc);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Process " + proc.getIdentifier() + " can't be added. Needed schemas can't be build.", ex);
                }
            } else {
                final Identifier pid = descriptor.getIdentifier();
                final Citation authy = pid.getAuthority();
                LOGGER.log(Level.WARNING, "Process {0}:{1} not supported.",
                        new Object[]{authy == null || authy.getTitle() == null? "unknown authority" : authy.getTitle().toString(), pid.getCode()});
            }
        }
        LOGGER.log(Level.INFO, "{0} processes loaded.", processList.size());
    }

    @Override
    protected MarshallerPool getMarshallerPool() {
        return WPSMarshallerPool.getInstance();
    }

    /**
     * Update the current WPS URL based on the current service URL.
     */
    private void updateWPSURL() {
        String webappURL = getServiceUrl();
        if (webappURL != null) {
            if (webappURL.contains("/wps/"+getId())) {
                this.wmsInstanceURL = webappURL.replace("/wps/"+getId(), "/wms/"+this.wmsInstanceName);
            } else {
                LOGGER.log(Level.WARNING, "Wrong service URL.");
            }
        } else {
            LOGGER.log(Level.WARNING, "Service URL undefined.");
        }
    }

    /**
     * Update the current products URL based on the current service URL.
     * TODO find a better way to build URL
     */
    private void updateProductsURL() {
        String webappURL = getServiceUrl();
        if (webappURL != null) {
            final String name = "/wps/" + getId();
            final int index = webappURL.indexOf(name);
            if (index != -1) {
                webappURL = webappURL.substring(0, index+name.length());
                this.productURL = webappURL + "/"+PATH_PRODUCTS_NAME;
                this.schemaURL = webappURL + "/"+PATH_PRODUCTS_NAME+"/"+PATH_SCHEMA_NAME;
            } else {
                LOGGER.log(Level.WARNING, "Wrong service URL.");
            }
        } else {
            LOGGER.log(Level.WARNING, "Service URL undefined.");
        }
    }

    //////////////////////////////////////////////////////////////////////
    //                      GetCapabilities
    //////////////////////////////////////////////////////////////////////
    /**
     * GetCapabilities request
     *
     * @param request request
     * @return WPSCapabilitiesType
     * @throws CstlServiceException
     */
    @Override
    public Capabilities getCapabilities(final GetCapabilities request) throws CstlServiceException {
        isWorking();

        //check LANGUAGE=en-EN
        final Locale lang;
        if (!request.getLanguages().isEmpty() && (!StringUtilities.containsIgnoreCase(request.getLanguages(), WPS_LANG_EN) || !StringUtilities.containsIgnoreCase(request.getLanguages(), WPS_LANG_FR))) {
            throw new CstlServiceException("The specified " + LANGUAGE_PARAMETER + " is not handled by the service. ",
                    INVALID_PARAMETER_VALUE, LANGUAGE_PARAMETER.toLowerCase());
        } else if (!request.getLanguages().isEmpty()) {
            lang = Locale.forLanguageTag(request.getLanguages().get(0));
        } else {
            lang = WPS_EN_LOC;
        }

        //we verify the base request attribute
        verifyBaseRequest(request, false, true);

        final String currentVersion = request.getVersion().toString();

        //set the current updateSequence parameter
        final boolean returnUS = returnUpdateSequenceDocument(request.getUpdateSequence());
        if (returnUS) {
            return new Capabilities(currentVersion, getCurrentUpdateSequence());
        }

        Sections sections = request.getSections();
        // todo verify sections
        final AbstractCapabilitiesCore cachedCapabilities = getCapabilitiesFromCache(currentVersion, null);
        if (cachedCapabilities != null) {
            return (Capabilities) cachedCapabilities.applySections(sections);
        }

        // We unmarshall the static capabilities document.
        final Details skeleton = getStaticCapabilitiesObject("WPS", null);

        final Stream<ProcessSummary> processSummaries = processList.values().stream()
                                .map(pd -> pd.getProcessSummary(lang));

        final Capabilities response = new GetCapabilitiesBuilder()
                .setVersion(WPSVersion.getVersion(currentVersion))
                .setServiceDetails(skeleton)
                .setServiceUrl(getServiceUrl())
                .setUpdateSequence(getCurrentUpdateSequence())
                .setDefaultLanguage(WPS_SUPPORTED_LANG.get(0))
                .setSupportedLanguages(WPS_SUPPORTED_LANG.toArray(new String[WPS_SUPPORTED_LANG.size()]))
                .setProcesses(processSummaries)
                .build();

        putCapabilitiesInCache(currentVersion, null, response);
        return response;

    }

    //////////////////////////////////////////////////////////////////////
    //                      DescribeProcess
    //////////////////////////////////////////////////////////////////////
    /**
     * Describe process request.
     *
     * @param request request
     * @return ProcessDescriptions
     * @throws CstlServiceException
     *
     */
    @Override
    public ProcessOfferings describeProcess(final DescribeProcess request) throws CstlServiceException {
        isWorking();

        verifyBaseRequest(request, true, false);
        final String currentVersion = request.getVersion().toString();

        //check LANGUAGE is supported
        if (request.getLanguage() != null && !StringUtilities.containsIgnoreCase(WPS_SUPPORTED_LANG, request.getLanguage()) ) {
            throw new CstlServiceException("The specified " + LANGUAGE_PARAMETER + " is not handled by the service. ",
                    INVALID_PARAMETER_VALUE, LANGUAGE_PARAMETER.toLowerCase());
        }

        //needed to get the public adress of generated schemas (for feature parameters).
        updateProductsURL();

        //check mandatory IDENTIFIER is not missing.
        if (request.getIdentifier() == null || request.getIdentifier().isEmpty()) {
            throw new CstlServiceException("The parameter " + IDENTIFIER_PARAMETER + " must be specified.",
                    MISSING_PARAMETER_VALUE, IDENTIFIER_PARAMETER.toLowerCase());
        }

        Locale lang = WPS_EN_LOC;
        if (request.getLanguage() != null) {
            lang = Locale.forLanguageTag(request.getLanguage());
        }

        final List<CodeType> identifiers;
        // if no identifier is supplied or the special value "all" we return all the process description
        if (request.getIdentifier().size() == 1 && request.getIdentifier().get(0) != null && "all".equalsIgnoreCase(request.getIdentifier().get(0).getValue())) {
            identifiers = new ArrayList<>(processList.keySet());
        } else {
            identifiers = request.getIdentifier();
        }
        final List<ProcessOffering> descriptions = new ArrayList<>();
        for (final CodeType identifier : identifiers) {

            // Find the process and verify if the descriptor is linked to the WPS instance
            final WPSProcess process = getWPSProcess(identifier.getValue());
            try {
                descriptions.add(process.getProcessOffering(lang));
            } catch (WPSException ex) {
                throw new CstlServiceException(ex);
            }
        }
        ProcessOfferings off = new ProcessOfferings(descriptions);
        if ("1.0.0".equals(currentVersion)) {
            off.setService("WPS");
            off.setVersion("1.0.0");
            off.setLanguage(lang.toLanguageTag());
        }
        return off;
    }


    //////////////////////////////////////////////////////////////////////
    //                      Get Job List
    //////////////////////////////////////////////////////////////////////

    @Override
    public Set<String> getJobList(String processId) throws CstlServiceException {
        // Verify if the descriptor exist and is linked to the WPS instance
        getWPSProcess(processId);
        Set<String> jobs = execInfo.getJobs(processId);
        if (jobs == null) {
            return new HashSet<>();
        }
        return jobs;
    }

    //////////////////////////////////////////////////////////////////////
    //                      Get Status
    //////////////////////////////////////////////////////////////////////

    @Override
    public StatusInfo getStatus(GetStatus request) throws CstlServiceException {
        verifyBaseRequest(request, true, false);
        try {
            return execInfo.getStatus(request.getJobID());
        } catch (UnknowJobException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "jobId", 404);
        }
    }

    //////////////////////////////////////////////////////////////////////
    //                      Get Result
    //////////////////////////////////////////////////////////////////////

    @Override
    public Object getResult(GetResult request) throws CstlServiceException {
        verifyBaseRequest(request, true, false);
        try {
            Object result = execInfo.getResult(request.getJobID());
            Bill bill = quoteInfo.getBillForJob(request.getJobID(), true);
            if ((result instanceof Result) && bill != null) {
                Result r = (Result) result;
                String serviceUrl = getServiceUrl();
                r.setLinks(Arrays.asList(new Link(serviceUrl.substring(0, serviceUrl.length() - 1) + "/bills/" + bill.getId(), "bill", "application/json", "Associated bill")));
            }
            return result;
        } catch (UnknowJobException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "jobId", 404);
        }
    }

    //////////////////////////////////////////////////////////////////////
    //                      Dismiss
    //////////////////////////////////////////////////////////////////////

    @Override
    public StatusInfo dismiss(Dismiss request) throws CstlServiceException {
        if (isTransactionSecurized()) {
            if (!SecurityManagerHolder.getInstance().isAuthenticated()) {
                throw new UnauthorizedException("You must be authentified to perform an dismiss request.");
            }
            if (!SecurityManagerHolder.getInstance().isAllowed("execute")) {
               throw new UnauthorizedException("You are not allowed to perform an dismiss request.");
            }
        }
        verifyBaseRequest(request, true, false);
        try {
            execInfo.dismissJob(request.getJobID());

        } catch (UnknowJobException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "jobId", 404);
        } catch (WPSException ex) {
            throw new CstlServiceException(ex.getMessage());
        }
        return new StatusInfo(Status.DISMISS, request.getJobID());
    }

    //////////////////////////////////////////////////////////////////////
    //                      Execute
    //////////////////////////////////////////////////////////////////////
    /**
     * Redirect execute requests from the WPS version requested.
     *
     * @param request request
     * @return execute response (Raw data or Document response) depends of the ResponseFormType in execute request
     * @throws CstlServiceException
     */
    @Override
    public Object execute(final Execute request) throws CstlServiceException {
        return execute(request, null);
    }

    public Object execute(final Execute request, String quotationId) throws CstlServiceException {
        if (isTransactionSecurized()) {
            if (Application.getBooleanProperty(AppProperty.EXA_WPS_EXECUTE_SECURE, false)) {
                if (!SecurityManagerHolder.getInstance().isAuthenticated()) {
                    throw new UnauthorizedException("You must be authentified to perform an execute request.");
                }
                if (!SecurityManagerHolder.getInstance().isAllowed("execute")) {
                   throw new UnauthorizedException("You are not allowed to perform an execute request.");
                }
            }
        }
        verifyBaseRequest(request, true, false);

        final String version = request.getVersion().toString();

        updateProductsURL();
        updateWPSURL();

        //check LANGUAGE is supported
        if (request.getLanguage() != null && !StringUtilities.containsIgnoreCase(WPS_SUPPORTED_LANG, request.getLanguage()) ) {
            throw new CstlServiceException("The specified " + LANGUAGE_PARAMETER + " is not handled by the service. ",
                    INVALID_PARAMETER_VALUE, LANGUAGE_PARAMETER.toLowerCase());
        }
        final Locale lang;
        if (request.getLanguage() != null) {
            lang = Locale.forLanguageTag(request.getLanguage());
        } else {
            lang = WPS_EN_LOC;
        }


        //////////////////////////////
        //    REQUEST VALIDATION
        //////////////////////////////

        //check mandatory IDENTIFIER is not missing.
        if (request.getIdentifier() == null || request.getIdentifier().getValue() == null || request.getIdentifier().getValue().isEmpty()) {
            throw new CstlServiceException("The parameter " + IDENTIFIER_PARAMETER + " must be specified.",
                    MISSING_PARAMETER_VALUE, IDENTIFIER_PARAMETER.toLowerCase());
        }

        // Find the process and verify if the descriptor is linked to the WPS instance
        final WPSProcess processDesc = getWPSProcess(request.getIdentifier().getValue());

        if (!processDesc.isSupportedProcess()) {
            throw new CstlServiceException("Process " + request.getIdentifier().getValue() + " not supported by the service.",
                    INVALID_PARAMETER_VALUE, IDENTIFIER_PARAMETER.toLowerCase());
        }

        //check requested INPUT/OUTPUT. Throw a CstlException otherwise.
        try {
            processDesc.checkValidInputOuputRequest(request);
        } catch (IOParameterException ex) {
            throw new CstlServiceException(ex, getCodeFromIOParameterException(ex), ex.getParamId());
        }

        boolean isOutputRaw      = request.isRawOutput();
        boolean isOutputRespDoc  = request.isDocumentOutput();
        boolean isStatus         = request.isStatus();
        boolean isAsync;
        if (request.getMode() != null && Execute.Mode.auto.equals(request.getMode())) {
            isAsync = true;
        } else {
            isAsync          = Execute.Mode.async.equals(request.getMode());
        }

        if (isAsync && !processDesc.getJobControlOptions().contains(JobControlOptions.ASYNC_EXECUTE)) {
            throw new CstlServiceException("The process does not support asynchrone mode.", INVALID_PARAMETER_VALUE, "mode");
        } else if (!isAsync && !processDesc.getJobControlOptions().contains(JobControlOptions.SYNC_EXECUTE)) {
            throw new CstlServiceException("The process does not support synchrone mode.", INVALID_PARAMETER_VALUE, "mode");
        }

        //List all outputs if not define in request
        final List<OutputDefinition> outputs;
        if (request.getOutput() == null || request.isLineage()) {
            outputs = processDesc.getOutputDefinitions();
        } else {
            outputs = request.getOutput();
        }

        if (!isOutputRaw && !isOutputRespDoc) {
            throw new CstlServiceException("The response form should be defined.", MISSING_PARAMETER_VALUE, "responseForm");
        }

        //status="true" && storeExecuteResponse="false" -> exception (see WPS-1.0.0 spec page 43).
        if(isOutputRespDoc && isStatus && !isAsync){
             throw new CstlServiceException("Set the storeExecuteResponse to true if you want to see status in response documents.", INVALID_PARAMETER_VALUE, "storeExecuteResponse");
        }

        //////////////////////////////
        // END OF REQUEST VALIDATION
        //////////////////////////////
        LOGGER.log(Level.INFO, "Process Execute : {0}", request.getIdentifier().getValue());

        /*
         * ResponseDocument attributes
         */
        boolean useStorage = isOutputRespDoc && isAsync;
        final List<? extends OutputDefinition> wantedOutputs = request.getOutput();

        //Input temporary files used by the process. In order to delete them at the end of the process.
        final ArrayList<Path> tempFiles = new ArrayList<>();
        final String jobId = UUID.randomUUID().toString();
        final XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();


        ///////////////////////
        //   RUN Process
        //////////////////////

        if (isOutputRaw) {

            final Callable process;
            try {
                process = processDesc.createRawProcess(isAsync, version, tempFiles, execInfo, quoteInfo, request, jobId, quotationId);
            } catch (IOParameterException ex) {
                throw new CstlServiceException(ex.getMessage(), getCodeFromIOParameterException(ex), ex.getParamId());
            }

            ////////
            // RAW Async (only WPS 2.0.0)
            ////////
            if (isAsync) {

                // should not be possible with a valid request
                if (version.equals("1.0.0")) {
                    throw new CstlServiceException("Raw output is not available with StoreExecuteResponse=true", INVALID_PARAMETER_VALUE);
                }

                //run process in asynchronous
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Prepare and launch process in a separate thread.
                            jobExecutor.submit(process);
                        } catch (Exception e) {
                            // If we've got an exception, input parsing must have failed.
                            XMLGregorianCalendar creationTime = WPSUtils.getCurrentXMLGregorianCalendar();
                            ExceptionResponse exceptionReport = new ExceptionReport(Exceptions.formatStackTrace(e), null, null, ServiceDef.WPS_1_0_0.exceptionVersion.toString());

                            StatusInfo status = new StatusInfo(Status.FAILED, creationTime, exceptionReport.toString(), jobId);
                            WPSUtils.storeResponse(status, productFolderPath, jobId);
                            execInfo.setStatus(jobId, status);
                        }
                    }
                }).start();

                return new StatusInfo(Status.ACCEPTED, creationTime, "Process " + request.getIdentifier().getValue() + " accepted.", jobId);

            ////////
            // RAW Sync no timeout
            ////////
            } else {
                ParameterValueGroup result;
                final Future<ParameterValueGroup> future = jobExecutor.submit(process);
                try {
                    result = future.get();

                } catch (InterruptedException ex) {
                    throw new CstlServiceException("Process interrupted.", ex, NO_APPLICABLE_CODE);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof ProcessException) {
                        throw new CstlServiceException("Process execution failed." + ex.getCause().getMessage(), ex, NO_APPLICABLE_CODE);
                    } else if (cause != null) {
                        throw new CstlServiceException("Process execution failed." + ex.getCause().getMessage(), ex, NO_APPLICABLE_CODE);
                    } else {
                        throw new CstlServiceException("Process execution failed.", ex, NO_APPLICABLE_CODE);
                    }
                }

                try {
                    //only one output for raw data
                    final String outputIdentifier = outputs.get(0).getIdentifier();
                    return processDesc.createRawOutput(version, outputIdentifier, result);
                } catch (IOParameterException ex) {
                    throw new CstlServiceException(ex.getMessage(), getCodeFromIOParameterException(ex), ex.getParamId());
                } catch (WPSException ex) {
                    throw new CstlServiceException(ex);
                }
            }

        } else {

            //Lineage option.
            final List<DataInput> inputsResponse;
            final List<OutputDefinition> outputsResponse;
            if (request.isLineage()) {
		inputsResponse = request.getInput();
                outputsResponse = new ArrayList<>();
                outputsResponse.addAll(outputs);
            } else {
                inputsResponse  = null;
                outputsResponse = null;
            }

            final Map<String, Object> parameters =  buildParametersMap("WPS_" + getId() + "_" + processDesc.getLayerName(), jobId);
            //Give a brief process description into the execute response
            final ProcessSummary procSum = processDesc.getProcessSummary(lang);
            final String serviceInstance = getServiceUrl() + "SERVICE=WPS&REQUEST=GetCapabilities";

            final Callable process;
            try {
                process = processDesc.createDocProcess(useStorage, version, tempFiles, execInfo, quoteInfo, request, serviceInstance, procSum, inputsResponse, outputsResponse, jobId, quotationId, parameters);
            } catch (IOParameterException ex) {
                throw new CstlServiceException(ex.getMessage(), getCodeFromIOParameterException(ex), ex.getParamId());
            }

            ////////////////////////
            //   DOC Asynchrone   //
            ////////////////////////
            if (useStorage) {
                /*
                 * If we are in asynchronous execution, we create the status document and return, to specify user we accepted its
                 * request. We create the process in a thread, so if an input is a reference and we've got a timeout, client has
                 * already got the status location, and he will receive a proper exception report instead of a timeout error due
                 * to the input.
                 */
                if (!supportStorage) {
                    throw new CstlServiceException("Storage not supported.", STORAGE_NOT_SUPPORTED, "storeExecuteResponse");
                }

                //run process in asynchronous
                jobExecutor.submit(() -> {
                    try {
                        // Prepare and launch process in a separate thread.
                        process.call();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error while executing synchronous process", e);
                        // If we've got an exception, input parsing must have failed.
                        XMLGregorianCalendar creationTime1 = WPSUtils.getCurrentXMLGregorianCalendar();
                        ExceptionResponse exceptionReport = new ExceptionReport(Exceptions.formatStackTrace(e), null, null, ServiceDef.WPS_1_0_0.exceptionVersion.toString());
                        StatusInfo status1 = new StatusInfo(Status.FAILED, creationTime1, exceptionReport.toString(), jobId);
                        final Result response1 = new Result(WPS_SERVICE, version, lang.toLanguageTag(), serviceInstance, procSum, inputsResponse, outputsResponse, null, status1, jobId);
                        WPSUtils.storeResponse(response1, productFolderPath, jobId);
                    }
                });

                StatusInfo status = new StatusInfo(Status.ACCEPTED, creationTime, "Process " + request.getIdentifier().getValue() + " accepted.", jobId);
                final Result response = new Result(WPS_SERVICE, version, lang.toLanguageTag(), serviceInstance, procSum, inputsResponse, outputsResponse, null, status, jobId);
                response.setStatusLocation(productURL + "/" + jobId); //Output data URL

                //store response document
                WPSUtils.storeResponse(response, productFolderPath, jobId);

                // for WPS 2.0 return status instead of response document
                if ("2.0.0".equals(version)) {
                    return status;
                } else {
                    return response;
                }

            ////////////////////////
            //   DOC Synchrone    //
            ////////////////////////
            } else {

                final Future<ParameterValueGroup> future = jobExecutor.submit(process);

                ParameterValueGroup result = null;
                ExceptionResponse report = null;
                // timeout
                try {
                    //run process
                    result = future.get(TIMEOUT, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.WARNING, "Process " + processDesc.getIdentifier() + " interrupted.", ex);
                    report = new ExceptionReport(ex.getLocalizedMessage(), null, null, null);
                } catch (ExecutionException ex) {
                    //process exception
                    LOGGER.log(Level.WARNING, "Process " + processDesc.getIdentifier() + " has failed.", ex);
                    report = new ExceptionReport("Process error : " + ex.getLocalizedMessage(), null, null, null);
                } catch (TimeoutException ex) {
                    ((AbstractProcess) process).cancelProcess();
                    future.cancel(true);

                    report = new ExceptionReport("Process execution timeout. This process is too long and had been canceled,"
                            + " re-run request with status set to true.", null, null, null);

                }

                List<DataOutput> dataOutput;
                StatusInfo status;

                // error build failed status
                if (report != null) {

                    // for WPS 2.0 return report directly
                    if ("2.0.0".equals(version)) {
                        return report;
                    }

                    dataOutput = null;
                    status = new StatusInfo(Status.FAILED, creationTime, report.toString(), jobId);

                //no error - fill response outputs.
                } else {
                    try {
                        dataOutput = processDesc.createDocOutput(version, wantedOutputs, result, parameters, false);
                    } catch (IOParameterException ex) {
                        throw new CstlServiceException(ex.getMessage(), getCodeFromIOParameterException(ex), ex.getParamId());
                    } catch (WPSException ex) {
                        throw new CstlServiceException(ex);
                    }
                    status = new StatusInfo(Status.SUCCEEDED, creationTime,"Process completed.", jobId);
                }

                // add status and results to the response
                Result response = new Result(WPS_SERVICE, version, lang.toLanguageTag(), serviceInstance, procSum, inputsResponse, outputsResponse, dataOutput, status, jobId);
                if (version.equals("2.0.0")) {
                    response.setLanguage(null);
                    response.setService(null);
                    response.setVersion(null);
                }
                return response;
            }
        }
    }

    private Map<String, Object> buildParametersMap(final String layerName, final String jobId) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put(WPSConvertersUtils.OUT_STORAGE_DIR, productFolderPath);
        parameters.put(WPSConvertersUtils.OUT_STORAGE_URL, productURL);
        parameters.put(WPSConvertersUtils.WMS_INSTANCE_NAME, wmsInstanceName);
        parameters.put(WPSConvertersUtils.WMS_INSTANCE_URL, wmsInstanceURL);
        parameters.put(WPSConvertersUtils.WMS_STORAGE_DIR, fileCoverageStorePath);
        parameters.put(WPSConvertersUtils.WMS_STORAGE_ID, wmsProviderId);
        parameters.put(WPSConvertersUtils.WMS_LAYER_NAME, layerName);
        parameters.put(WPSConvertersUtils.CURRENT_JOB_ID, jobId);
        parameters.put(WMS_SUPPORTED, wmsSupported);
        return parameters;
    }

    /**
     * Search for an existing process descriptor and then verify if the descriptor is linked to the current WPS instance.
     *
     * @param identifier Process Identifier.
     * @return
     * @throws CstlServiceException
     */
    private WPSProcess getWPSProcess(String identifier) throws CstlServiceException {
        final CodeType id = new CodeType(identifier);

        // verify if the descriptor is linked to the WPS instance
        if (!processList.containsKey(id)) {
            throw new CstlServiceException("The process " + IDENTIFIER_PARAMETER.toLowerCase() + " : " + identifier + " does not exist.", null,
                INVALID_PARAMETER_VALUE, IDENTIFIER_PARAMETER.toLowerCase(), 404);
        }
        return processList.get(id);
    }


    @Override
    protected String getProperty(final String key) {
        try {
            final Object obj = serviceBusiness.getConfiguration("WPS", getId());
            if (obj instanceof ProcessContext) {
                ProcessContext context = (ProcessContext) obj;
                if (context.getCustomParameters() != null) {
                    return context.getCustomParameters().get(key);
                }
            }
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        return null;
    }

    /**
     *  Verify that the bases request attributes are correct.
     */
    private void verifyBaseRequest(final RequestBase request, final boolean versionMandatory, final boolean getCapabilities) throws CstlServiceException {
        isWorking();
        if (request != null) {
            if (request.getService() != null && !request.getService().isEmpty()) {
                if (!request.getService().equals(WPS_SERVICE))  {
                    throw new CstlServiceException("service must be \"WPS\"!", INVALID_PARAMETER_VALUE, SERVICE_PARAMETER);
                }
            } else {
                throw new CstlServiceException("service must be specified!", MISSING_PARAMETER_VALUE, SERVICE_PARAMETER);
            }

            if(getCapabilities) {
                final GetCapabilities getcapa = (GetCapabilities) request;
                final AcceptVersions acceptVersions = getcapa.getAcceptVersions();
                final String[] array = acceptVersions.getVersion().toArray(new String[0]);

                //find compatible and upmost version
                Version bestVersion = null;
                for(String str : array){
                    for(ServiceDef def : supportedVersions){
                        if(def.version.toString().equals(str) && (bestVersion==null || bestVersion.compareTo(def.version)<0)){
                            bestVersion = def.version;
                        }
                    }
                }

                if (bestVersion!=null) {
                    request.setVersion(bestVersion.toString());
                    return;
                } else {
                    final CodeList code;
                    final String locator;
                    if (getCapabilities) {
                        code = VERSION_NEGOTIATION_FAILED;
                        locator = "acceptVersion";
                    } else {
                        code = INVALID_PARAMETER_VALUE;
                        locator = "version";
                    }
                    final StringBuilder sb = new StringBuilder();
                    for (ServiceDef v : supportedVersions) {
                        sb.append("\"").append(v.version.toString()).append("\"");
                    }
                    throw new CstlServiceException("version must be " + sb.toString() + "!", code, locator);
                }

            } else if (request.getVersion()!= null && !request.getVersion().toString().isEmpty()) {

                if (isSupportedVersion(request.getVersion().toString())) {
                    request.setVersion(request.getVersion().toString());
                } else {
                    final CodeList code;
                    final String locator;
                    if (getCapabilities) {
                        code = VERSION_NEGOTIATION_FAILED;
                        locator = "acceptVersion";
                    } else {
                        code = INVALID_PARAMETER_VALUE;
                        locator = "version";
                    }
                    final StringBuilder sb = new StringBuilder();
                    for (ServiceDef v : supportedVersions) {
                        sb.append("\"").append(v.version.toString()).append("\"");
                    }
                    throw new CstlServiceException("version must be " + sb.toString() + "!", code, locator);
                }
            } else {
                if (versionMandatory) {
                    throw new CstlServiceException("version must be specified!", MISSING_PARAMETER_VALUE, "version");
                } else {
                    request.setVersion(getBestVersion(null).version.toString());
                }
            }
         } else {
            throw new CstlServiceException("The request is null!", NO_APPLICABLE_CODE);
         }
    }

    private static CodeList getCodeFromIOParameterException(IOParameterException ex) {
        if (ex.isMissing()) {
            return MISSING_PARAMETER_VALUE;
        } else if (ex.isUnsupported()) {
            return OPERATION_NOT_SUPPORTED;
        }
        return INVALID_PARAMETER_VALUE;
    }

    @Override
    public DeployResult deploy(Deploy request) throws CstlServiceException {
        if (isTransactionSecurized()) {
            if (!SecurityManagerHolder.getInstance().isAuthenticated()) {
                throw new UnauthorizedException("You must be authentified to perform a deploy request.");
            }
            if (!SecurityManagerHolder.getInstance().isAllowed("deploy")) {
               throw new UnauthorizedException("You are not allowed to perform a deploy request.");
            }
        }
        if (request.getProcessDescription()== null) {
            throw new CstlServiceException("Process description must be specified", MISSING_PARAMETER_VALUE);
        }
        if (request.getProcessDescription().getProcess() == null) {
            throw new CstlServiceException("Process description is not complete (Process part missing. reference not supported for now)", INVALID_PARAMETER_VALUE);
        }
        if (request.getProcessDescription().getProcess().getOwsContext() == null || request.getProcessDescription().getProcess().getOwsContext().getOffering() == null) {
            throw new CstlServiceException("Process part is not complete (OWS context part missing/incomplete)", INVALID_PARAMETER_VALUE);
        }
        if (request.getExecutionUnit() == null || request.getExecutionUnit().isEmpty()) {
            throw new CstlServiceException("Execution unit must be specified", MISSING_PARAMETER_VALUE);
        }

        ProcessDescriptionChoiceType off = request.getProcessDescription();
        final ProcessDescription processDescription = off.getProcess();

        if (processDescription.getIdentifier() != null) {
            int id  = 1;
            String processId      = processDescription.getIdentifier().getValue();
            String title          = processDescription.getFirstTitle();
            String description    = processDescription.getFirstAbstract();
            List<String> controls = new ArrayList<>();
            for (JobControlOptions control : off.getJobControlOptions()) {
                controls.add(control.name());
            }
            List<String> outTransmission = new ArrayList<>();
            for (DataTransmissionMode trans : off.getOutputTransmission()) {
                outTransmission.add(trans.name());
            }

            if (processBusiness.getChainProcess(ExamindDynamicProcessFactory.NAME, processId) != null) {
                throw new CstlServiceException("Process " + processId + " is already deployed", INVALID_PARAMETER_VALUE);
            }

            final Chain chain = new Chain(processId);
            chain.setTitle(title);
            chain.setAbstract(description);

            if (request.getExecutionUnit().get(0) == null || request.getExecutionUnit().get(0).getReference() == null || request.getExecutionUnit().get(0).getReference().getHref() == null) {
                throw new CstlServiceException("Process offering content must be specified", MISSING_PARAMETER_VALUE);
            }

            /*
             What to do with the execution unit
             String cwlFile = request.getExecutionUnit().get(0).getReference().getHref();
            */
            String offeringCode = processDescription.getOwsContext().getOffering().getCode();
            String offeringCnt  = processDescription.getOwsContext().getOffering().getContentRef(); // cwl File

            Map<String, Object> chainUserMap = new HashMap();
            chainUserMap.put("offering.code", offeringCode);
            chainUserMap.put("offering.content", offeringCnt);
            if (request.getDeploymentProfileName() != null) {
                chainUserMap.put("profile", request.getDeploymentProfileName());
            }

            String runDocName = RunCWLDescriptor.NAME + '-' + UUID.randomUUID().toString();

            //input/out/constants parameters

            final Constant c = chain.addConstant(id++, String.class, offeringCnt);

            final List<Parameter> inputs = new ArrayList<>();
            final List<Parameter> outputs = new ArrayList<>();

            chain.setTitle(processDescription.getFirstTitle());
            chain.setAbstract(processDescription.getFirstAbstract());
            for (InputDescription in : processDescription.getInputs()) {
                // TODO type
                Class type;
                if (in.getDataDescription() instanceof LiteralData) {
                    type = String.class;
                } else if (in.getDataDescription() instanceof ComplexData) {
                    type = URI.class;
                } else if (in.getDataDescription() instanceof BoundingBoxData) {
                    type = Envelope.class;
                } else {
                    type = String.class;
                }
                final Parameter param = new Parameter(in.getIdentifier().getValue(), type, in.getFirstTitle(), in.getFirstAbstract(), in.getMinOccurs(), in.getMaxOccurs());
                if (in.getAdditionalParameters() != null) {
                    final Map<String, Object> userMap = new HashMap<>();
                    for (AdditionalParametersType addParams : in.getAdditionalParameters()) {
                        // WARNING role work for only one
                        if (addParams.getRole() != null) {
                            userMap.put("role", addParams.getRole());
                        }
                        for (AdditionalParameter addParam : addParams.getAdditionalParameter()) {
                            Object values = addParam.getValue();
                            if (addParam.getValue() != null && addParam.getValue().size() == 1) {
                                values = addParam.getValue().get(0);
                            }
                            userMap.put(addParam.getName().getValue(), values);
                        }
                    }
                    param.setUserMap(userMap);
                }
                // extracting format
                if (in.getDataDescription() != null) {
                    DataDescription desc  = in.getDataDescription();
                    if (!desc.getFormat().isEmpty()) {
                        List<ParameterFormat> formats = new ArrayList<>();
                        for (Format format : desc.getFormat()) {
                            ParameterFormat m = new ParameterFormat();
                            if (format.getEncoding() != null)         m.setEncoding(format.getEncoding());
                            if (format.getMimeType() != null)         m.setMimeType(format.getMimeType());
                            if (format.getSchema() != null)           m.setSchema(format.getSchema());
                            formats.add(m);
                        }
                        param.setFormats(formats);
                    }
                }
                inputs.add(param);
            }

            chain.setInputs(inputs);

            for (OutputDescription out : processDescription.getOutputs()) {
                Class type;
                if (out.getDataDescription() instanceof LiteralData) {
                    type = String.class;
                } else if (out.getDataDescription() instanceof ComplexData) {
                    type = File.class;
                } else if (out.getDataDescription() instanceof BoundingBoxData) {
                    type = Envelope.class;
                } else {
                    type = String.class;
                }
                final Parameter param = new Parameter(out.getIdentifier().getValue(), type, out.getFirstTitle(), out.getFirstAbstract(), 0, Integer.MAX_VALUE);
                if (out.getAdditionalParameters() != null) {
                    final Map<String, Object> userMap = new HashMap<>();
                    for (AdditionalParametersType addParams : out.getAdditionalParameters()) {
                        // WARNING role work for only one
                        if (addParams.getRole() != null) {
                            userMap.put("role", addParams.getRole());
                        }
                        for (AdditionalParameter addParam : addParams.getAdditionalParameter()) {
                            Object values = addParam.getValue();
                            if (addParam.getValue() != null && addParam.getValue().size() == 1) {
                                values = addParam.getValue().get(0);
                            }
                            userMap.put(addParam.getName().getValue(), values);
                        }
                    }
                    param.setUserMap(userMap);
                }
                // extracting format
                if (out.getDataDescription() != null) {
                    DataDescription desc  = out.getDataDescription();
                    if (!desc.getFormat().isEmpty()) {
                        List<ParameterFormat> formats = new ArrayList<>();
                        for (Format format : desc.getFormat()) {
                            ParameterFormat m = new ParameterFormat();
                            if (format.getEncoding() != null)         m.setEncoding(format.getEncoding());
                            if (format.getMimeType() != null)         m.setMimeType(format.getMimeType());
                            if (format.getSchema() != null)           m.setSchema(format.getSchema());
                            formats.add(m);
                        }
                        param.setFormats(formats);
                    }
                }
                outputs.add(param);
            }
            chain.setOutputs(outputs);


            //chain blocks
            final ElementProcess dock = chain.addProcessElement(id++, ExamindDynamicProcessFactory.NAME, runDocName);

            chain.addFlowLink(BEGIN.getId(), dock.getId());
            chain.addFlowLink(dock.getId(), END.getId());


            //data flow links
            chain.addDataLink(c.getId(), "", dock.getId(), RunCWLDescriptor.CWL_FILE_NAME);

            for (Parameter in : inputs) {
                chain.addDataLink(BEGIN.getId(), in.getCode(),  dock.getId(), in.getCode());
            }

            for (Parameter out : outputs) {
                chain.addDataLink(dock.getId(), out.getCode(),  END.getId(), out.getCode());
            }

            try {

                processBusiness.createChainProcess(ChainProcessRetriever.convertToDto(chain));
                LOGGER.log(Level.INFO, "=== Deploying process " + processId + " ===");

                Registry registry = new Registry(ExamindDynamicProcessFactory.NAME);
                registry.setProcesses(Arrays.asList(new org.constellation.dto.process.Process(processId, title, description, false, controls, outTransmission, chainUserMap)));

                ProcessContext context = (ProcessContext) serviceBusiness.getConfiguration("WPS", "default");
                context = WPSConfigurationUtils.addProcessToContext(context, new RegistryList(registry));

                // save modified context
                serviceBusiness.configure("WPS", "default", null, context);

                //reload process list
                fillProcessList(context);
                clearCapabilitiesCache();

            } catch (ConstellationException ex) {
                throw new CstlServiceException(ex);
            }

        } else {
            throw new CstlServiceException("Process identifier is missing.", MISSING_PARAMETER_VALUE);
        }

        ProcessSummary sum = new ProcessSummary(off.getProcess(), off.getProcessVersion(), off.getJobControlOptions(), off.getOutputTransmission());
        return new DeployResult(sum);
    }

    @Override
    public UndeployResult undeploy(Undeploy request) throws CstlServiceException {
        if (isTransactionSecurized()) {
            if (!SecurityManagerHolder.getInstance().isAuthenticated()) {
                throw new UnauthorizedException("You must be authentified to perform an undeploy request.");
            }
            if (!SecurityManagerHolder.getInstance().isAllowed("deploy")) {
               throw new UnauthorizedException("You are not allowed to perform an undeploy request.");
            }
        }
        try {
            if (request.getIdentifier() == null) {
                throw new CstlServiceException("Process identifier must be specified", MISSING_PARAMETER_VALUE);
            }
            String processId = request.getIdentifier().getValue();
            if (processBusiness.getChainProcess(ExamindDynamicProcessFactory.NAME, processId) == null) {
                throw new CstlServiceException("Process " + processId + " is not deployed on the server", null, INVALID_PARAMETER_VALUE, "processId", 404);
            }

            // remove process link
            ProcessContext context = (ProcessContext) serviceBusiness.getConfiguration("WPS", "default");
            context = WPSConfigurationUtils.removeProcessFromContext(context, ExamindDynamicProcessFactory.NAME, processId);

            // save modified context
            serviceBusiness.configure("WPS", "default", null, context);

            // remove chain
            processBusiness.deleteChainProcess(ExamindDynamicProcessFactory.NAME, processId);

            //reload process list
            fillProcessList(context);
            clearCapabilitiesCache();

            return new UndeployResult(processId, "OK");

        } catch (ConstellationException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public QuotationList getQuotationList(String processId) throws CstlServiceException {
        // Verify if the descriptor exist and is linked to the WPS instance
        getWPSProcess(processId);
        return new QuotationList(quoteInfo.getQuotations(processId));
    }

    @Override
    public QuotationList getQuotationList() throws CstlServiceException {
        return new QuotationList(quoteInfo.getAllQuotationIds());
    }

    @Override
    public Quotation getQuotation(String quotationId) throws CstlServiceException {
        try {
            return quoteInfo.getQuotation(quotationId);
        } catch (UnknowQuotationException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "quotationId", 404);
        }
    }

    @Override
    public Quotation quote(Execute request) throws CstlServiceException {
        // Find the process and verify if the descriptor is linked to the WPS instance
        final WPSProcess process = getWPSProcess(request.getIdentifier().getValue());

        //check requested INPUT/OUTPUT. Throw a CstlException otherwise.
        try {
            process.checkValidInputOuputRequest(request);
        } catch (IOParameterException ex) {
            throw new CstlServiceException(ex, getCodeFromIOParameterException(ex), ex.getParamId());
        }
        try {
            Quotation quote = process.quote(request);
            quoteInfo.addQuotation(quote);
            return quote;
        } catch (WPSException ex) {
            throw new CstlServiceException(ex);
        }
    }

    @Override
    public Object executeQuotation(String quotationId) throws CstlServiceException {
        try {
            Quotation quote = quoteInfo.getQuotation(quotationId);
            Execute request = quote.getProcessParameters();
            return execute(request, quotationId);
        } catch (UnknowQuotationException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "quotationId", 404);
        }
    }

    @Override
    public BillList getBillList() throws CstlServiceException {
        return new BillList(quoteInfo.getAllBillIds());
    }

    @Override
    public Bill getBill(String billId) throws CstlServiceException {
        try {
            return quoteInfo.getBill(billId);
        } catch (UnknowQuotationException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "billId", 404);
        }
    }

    @Override
    public Bill getBillForJob(String jobID) throws CstlServiceException {
        try {
            return quoteInfo.getBillForJob(jobID, false);
        } catch (UnknowJobException ex) {
            throw new CstlServiceException(ex.getMessage(), ex, INVALID_PARAMETER_VALUE, "jobID", 404);
        }
    }
}

