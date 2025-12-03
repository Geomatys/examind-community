package com.examind.openeo.api.rest.process;

import org.geotoolkit.openeo.dto.CheckMessage;
import org.geotoolkit.openeo.dto.ResponseMessage;
import org.geotoolkit.openeo.process.dto.BoundingBox;
import org.geotoolkit.openeo.process.dto.DataTypeSchema;
import org.geotoolkit.openeo.process.dto.Job;
import org.geotoolkit.openeo.process.dto.Jobs;
import org.geotoolkit.openeo.process.dto.ProcessDescription;
import org.geotoolkit.openeo.process.dto.ProcessDescriptionArgument;
import org.geotoolkit.openeo.process.dto.ProcessHolder;
import org.geotoolkit.openeo.process.dto.ProcessParameter;
import org.geotoolkit.openeo.process.dto.ProcessReturn;
import org.geotoolkit.openeo.process.dto.Processes;
import org.geotoolkit.openeo.process.dto.Process;
import com.examind.wps.api.WPSWorker;
import com.examind.wps.util.WPSUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.referencing.CRS;
import org.constellation.api.ServiceDef;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.ITokenBusiness;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.process.Registry;
import org.constellation.exception.ConstellationException;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.constellation.ws.rs.OGCWebService;
import org.constellation.ws.rs.ResponseObject;
import org.geotoolkit.atom.xml.Link;
import org.geotoolkit.openeo.process.dto.Status;
import org.geotoolkit.ows.xml.v200.CodeType;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.processing.chain.model.Constant;
import org.geotoolkit.processing.chain.model.Element;
import org.geotoolkit.processing.chain.model.ElementProcess;
import org.geotoolkit.processing.chain.model.Parameter;
import org.geotoolkit.wps.xml.v200.DataInput;
import org.geotoolkit.wps.xml.v200.Dismiss;
import org.geotoolkit.wps.xml.v200.Execute;
import org.geotoolkit.wps.xml.v200.GetResult;
import org.geotoolkit.wps.xml.v200.GetStatus;
import org.geotoolkit.wps.xml.v200.OutputDefinition;
import org.geotoolkit.wps.xml.v200.StatusInfo;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.geotoolkit.openeo.process.OpenEOUtils.buildDataTypeSchema;
import static org.geotoolkit.openeo.process.OpenEOUtils.examindProcessIdToOpenEOProcessId;
import static org.geotoolkit.openeo.process.OpenEOUtils.openEOProcessIdToExamindProcessId;
import static org.geotoolkit.processing.chain.model.Element.BEGIN;
import org.opengis.util.InternationalString;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FAILED_DEPENDENCY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * OpenEO Process Service REST API.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
@RestController
public class OpenEOProcessService extends OGCWebService<WPSWorker> {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger("com.examind.openeo.api.rest");

    /**
     * Prefix for user-defined processes.
     */
    private final String USER_DEFINED_PROCESS_PREFIX = "openeo-";

    /**
     * Prefix for temporary user-defined processes.
     */
    private final String TEMPORARY_USER_DEFINED_PROCESS_PREFIX = "temp-openeo-";

    /**
     * Process business service.
     */
    @Autowired
    public IProcessBusiness processBusiness;

    /**
     * Token business service.
     */
    @Autowired
    private ITokenBusiness tokenBusiness;

    /**
     * Map of job IDs to their corresponding processes.
     */
    private final Map<String, Process> jobIdToProcessMap;

    private final ExternalStacManager externalStac;

    /**
     * Default Constructor.
     */
    public OpenEOProcessService() {
        super(ServiceDef.Specification.WPS);
        this.jobIdToProcessMap = new HashMap<>();
        this.externalStac = new ExternalStacManager();
    }

    /**
     * {@inheritDoc}
     * @param objectRequest if the server receive a POST request in XML,
     *        this object contain the request. Else for a GET or a POST kvp
     *        request this parameter is {@code null}
     *
     * @param worker the selected worker on which apply the request.
     *
     * @return {@code null} as this method is not used in this service.
     */
    @Override
    protected ResponseObject treatIncomingRequest(Object objectRequest, WPSWorker worker) {
        return null;
    }

    /**
     * Connect via basic authentication and retrieve user credentials by generating a token.
     *
     * @param serviceId   the ID of the service
     * @param httpRequest the HTTP servlet request
     * @return a response entity containing the user's token if authenticated, or an unauthorized status otherwise
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/credentials/basic", "openeo/{serviceId:.+}/credentials/basic/"}, method = {GET, OPTIONS}, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getCredentials(@PathVariable("serviceId") String serviceId, HttpServletRequest httpRequest) {

        if (HttpMethod.OPTIONS.matches(httpRequest.getMethod())) {
            return ResponseEntity.ok().build();
        }

        if (SecurityManagerHolder.getInstance().isAuthenticated()) {
            String userLogin = SecurityManagerHolder.getInstance().getCurrentUserLogin();
            String token = tokenBusiness.createToken(userLogin);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("user_id", SecurityManagerHolder.getInstance().getCurrentUserLogin());
            response.put("access_token", token);
            response.put("token_type", "Bearer");

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Retrieves all processes.
     *
     * @return a response entity containing a list of processes
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/processes", "openeo/{serviceId:.+}/processes/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getAllProcesses() {

        final Processes processes = new Processes();

        for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext(); ) {
            final ProcessingRegistry processingRegistry = it.next();
            Iterator<? extends Identifier> iterator = processingRegistry
                    .getIdentification().getCitation().getIdentifiers()
                    .iterator();
            final Registry registry = new Registry(iterator.next().getCode());

            for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                if (WPSUtils.isValidOpenEOProcess(descriptor)) {
                    final Process process = buildProcess(registry, descriptor);

                    processes.addProcessesItem(process);
                }
            }
        }

        return new ResponseEntity(processes, OK);
    }

    /**
     * Check if a process with the same ID already exist
     * @param processID id of the process
     * @return true if already exist, false otherwise
     */
    private boolean processAlreadyExist(String processID, boolean isTemp) {
        if (isTemp) {
            processID = TEMPORARY_USER_DEFINED_PROCESS_PREFIX + processID;
        } else {
            processID = USER_DEFINED_PROCESS_PREFIX + processID;
        }

        for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext(); ) {
            final ProcessingRegistry processingRegistry = it.next();
            for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                if (WPSUtils.isValidOpenEOProcess(descriptor)) {
                    if (descriptor.getIdentifier().getCode().equals(processID)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Retrieves all user-defined processes.
     *
     * @return a response entity containing a list of user-defined processes
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/process_graphs", "openeo/{serviceId:.+}/process_graphs/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getUserDefinedProcesses() {

        final Processes processes = new Processes();
        ProcessingRegistry processingRegistry = ProcessFinder.getProcessFactory("examind-dynamic");

        if (processingRegistry != null) {
            Iterator<? extends Identifier> iterator = processingRegistry
                    .getIdentification().getCitation().getIdentifiers()
                    .iterator();
            final Registry registry = new Registry(iterator.next().getCode());

            for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                if (descriptor.getIdentifier().getCode().startsWith(USER_DEFINED_PROCESS_PREFIX) && WPSUtils.isValidOpenEOProcess(descriptor)) {
                    final Process process = buildProcess(registry, descriptor);

                    processes.addProcessesItem(process);
                }
            }

        } else {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "RegistryNotFound",
                            "The ProcessingRegistry examind-dynamic, containing all the processes is not found", List.of()),
                    BAD_REQUEST);
        }

        return new ResponseEntity(processes, OK);
    }

    /**
     * Retrieves a user-defined process by its ID.
     *
     * @param processGraphId the ID of the process to retrieve
     * @return a response entity containing the user-defined process
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/process_graphs/{process_graph_id}", "openeo/{serviceId:.+}/process_graphs/{process_graph_id}/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getUserDefinedProcess(@PathVariable("process_graph_id") String processGraphId) {

        Process process = null;

        ProcessingRegistry processingRegistry = ProcessFinder.getProcessFactory("examind-dynamic");
        if (processingRegistry != null) {

            String processId = processGraphId.split("\\.")[1];

            Iterator<? extends Identifier> iterator = processingRegistry
                    .getIdentification().getCitation().getIdentifiers()
                    .iterator();
            final Registry registry = new Registry(iterator.next().getCode());

            ProcessDescriptor desc = processingRegistry.getDescriptors().stream()
                    .filter(descriptor -> descriptor.getIdentifier().getCode().equals(processId))
                    .findFirst()
                    .orElse(null);

            if (desc != null && WPSUtils.isValidOpenEOProcess(desc)) {
                process = buildProcess(registry, desc);
            } else {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ProcessNotFound",
                                "The process with id " + processGraphId + " is not found", List.of()),
                        BAD_REQUEST);
            }
        } else {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "RegistryNotFound",
                            "The ProcessingRegistry examind-dynamic, containing all the processes is not found", List.of()),
                    BAD_REQUEST);
        }

        return new ResponseEntity(process, OK);
    }

    /**
     * Build an OpenEO Process Object from a Geotoolkit ProcessDescriptor (Examind process)
     *
     * @param registry Registry of the source process
     * @param descriptor Descriptor of the source process
     * @return OpenEO Process Object
     */
    private Process buildProcess(Registry registry, ProcessDescriptor descriptor) {
        final Process process = new Process();

        String exaId = registry.getName() + "." + descriptor.getIdentifier().getCode();
        String openEOId = examindProcessIdToOpenEOProcessId(exaId);
        process.setId(openEOId);
        process.setCategories(List.of(registry.getName()));
        process.setDeprecated(false);
        process.setExperimental(false);

        if (descriptor.getProcedureDescription() != null) {
            process.setDescription(descriptor.getProcedureDescription().toString());
            process.setSummary(descriptor.getProcedureDescription().toString());
        }

        //PROCESS INPUTS
        if (!descriptor.getInputDescriptor().descriptors().isEmpty()) {
            List<ProcessParameter> parameters = new ArrayList<>();

            for (GeneralParameterDescriptor inputDescriptor : descriptor.getInputDescriptor().descriptors()) {
                // HACK : for the load_collection process, we don't want to show the serviceId parameter
                // openEO will use the same id for the wps and wcs. So the serviceId parameter will reuse the id of the current wps service
                if (openEOId.equalsIgnoreCase("load_collection") && inputDescriptor.getName().getCode().equalsIgnoreCase("serviceId")) {
                    continue;
                }

                String description = null;
                InternationalString desc = inputDescriptor.getDescription().orElse(null);
                InternationalString rema = inputDescriptor.getRemarks().orElse(null);
                if (desc != null && !desc.toString().isEmpty()) {
                    description = desc.toString();
                } else if (rema != null && !rema.toString().isEmpty()) {
                    description = rema.toString();
                }

                String type = null;
                Class<?> clazz = null;
                boolean isArray = false;
                if (inputDescriptor instanceof DefaultParameterDescriptor<?> inputDefaultParameterDescriptor) {
                    try {
                        if (inputDefaultParameterDescriptor.getValueType() != null) {
                            type = inputDefaultParameterDescriptor.getValueType().toString();
                        }
                        if (inputDefaultParameterDescriptor.getValueClass() != null) {
                            clazz = inputDefaultParameterDescriptor.getValueClass();
                            isArray = inputDefaultParameterDescriptor.getValueClass().isArray();
                        }
                    } catch (NullPointerException ex) {
                        LOGGER.log(Level.INFO, "Error impossible to get the type of the input", ex);
                    }
                }

                parameters.add(new ProcessParameter(inputDescriptor.getName().getCode(), description,
                        buildDataTypeSchema(descriptor, inputDescriptor.getName().getCode(), type, clazz, isArray, (inputDescriptor.getMinimumOccurs() > 0)),
                        (inputDescriptor.getMinimumOccurs() == 0), null));
            }

            process.setParameters(parameters);
        } else {
            process.setParameters(List.of());
        }

        //PROCESS OUTPUT(s)
        if (!descriptor.getOutputDescriptor().descriptors().isEmpty()) {
            //In openEO API we only can return one thing, so here we only work with the first output
            GeneralParameterDescriptor outputDescriptor = descriptor.getOutputDescriptor().descriptors().get(0);

            String description;
            InternationalString desc = outputDescriptor.getDescription().orElse(null);
            InternationalString rema = outputDescriptor.getRemarks().orElse(null);
            if (desc != null && !desc.toString().isEmpty()) {
                description = desc.toString();
            } else if (rema != null && !rema.toString().isEmpty()) {
                description = rema.toString();
            } else {
                description = outputDescriptor.getName().getCode();
            }

            String type = null;
            boolean isArray = false;
            if (descriptor.getOutputDescriptor().descriptors().get(0) instanceof DefaultParameterDescriptor<?> outputDefaultParameterDescriptor) {
                try {
                    if (outputDefaultParameterDescriptor.getValueType() != null) {
                        type = outputDefaultParameterDescriptor.getValueType().toString();
                    }
                    if (outputDefaultParameterDescriptor.getValueClass() != null) {
                        isArray = outputDefaultParameterDescriptor.getValueClass().isArray();
                    }
                } catch (NullPointerException ex) {
                    LOGGER.log(Level.WARNING, "Error impossible to get the type of the ouput", ex);
                }
            }

            process.setReturns(new ProcessReturn(description, new DataTypeSchema(type == null ? List.of() : List.of(DataTypeSchema.Type.fromValue(type, isArray)), null)));
        } else {
            process.setReturns(new ProcessReturn("No data returned", null));
        }

        return process;
    }

    /**
     * Validates a user-defined process.
     *
     * @param serviceId the ID of the service
     * @param process the process to validate
     * @return a response entity containing a map of validation errors, if any
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/validation", "openeo/{serviceId:.+}/validation/"}, method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity validateUserProcess(@PathVariable("serviceId") String serviceId, @RequestBody final Process process) {

        List<ProcessDescriptor> descriptorList = getDescriptorList();

        Map<String, List<ResponseMessage>> responseErrorMap = new HashMap<>();
        List<ResponseMessage> listError = new ArrayList<>();

        if (processAlreadyExist(process.getId(), false)) {
            listError.add(new ResponseMessage(UUID.randomUUID().toString(), "ProcessIDAlreadyExist", "The process id specified already exist, you cannot use the same to add this process.", List.of()));
        }

        CheckMessage checkMessage = process.isProcessGraphValid(descriptorList);
        if (!checkMessage.isValid()) {
            listError.add(new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + checkMessage.getMessage(), List.of()));
        }

        // Check if the collection exists in the external STAC endpoint if configured
        externalStac.checkProcessLoadCollectionsWithExternalStac(process, serviceId, listError);

        responseErrorMap.put("errors", listError);

        return new ResponseEntity(responseErrorMap, OK);
    }

    /**
     * Stores a user-defined process with the given ID.
     *
     * @param serviceId      the ID of the service
     * @param processGraphId the ID of the process to be stored
     * @param process        the process to be stored, with the same ID as `processGraphId`
     * @return a response with an HTTP status code of 200 (OK) if the storage is successful,
     * or a response with an HTTP status code of 400 (Bad Request) and a JSON payload
     * containing an error message if the storage fails or the input is invalid
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/process_graphs/{process_graph_id}" ,
            "openeo/{serviceId:.+}/process_graphs/{process_graph_id}/"}, method = PUT, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity storeUserDefinedProcess(@PathVariable("serviceId") String serviceId,
                                                  @PathVariable("process_graph_id") String processGraphId,
                                                  @RequestBody final Process process) {

        if (!process.getId().equalsIgnoreCase(processGraphId)) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "The process id in the URL and in the Request Body are not equals.", List.of()),
                    BAD_REQUEST);
        }

        if (processAlreadyExist(process.getId(), false)) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "ProcessIDAlreadyExist", "The process id specified already exist, you cannot use the same to add this process.", List.of()),
                    BAD_REQUEST);
        }

        List<ProcessDescriptor> descriptorList = getDescriptorList();

        CheckMessage checkMessage = process.isProcessGraphValid(descriptorList);
        if (!checkMessage.isValid()) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + checkMessage.getMessage() + " --- The process specified is not valid, check the elements of the process graphs." +
                            "Arguments are maybe incorrect, check ids of the processes,...", List.of()),
                    BAD_REQUEST);
        }

        // Check if the collection exists in the external STAC endpoint if configured
        List<ResponseMessage> listError = new ArrayList<>();
        externalStac.checkProcessLoadCollectionsWithExternalStac(process, serviceId, listError);
        if (!listError.isEmpty()) {
            return new ResponseEntity(listError.getFirst(), BAD_REQUEST);
        }

        try {
            deployUserDefinedProcess(process, false, true, serviceId);
        } catch (UnsupportedOperationException | IllegalArgumentException | ProcessException ex) {
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + ex.getMessage() + " --- The process specified is not valid, check the elements of the process graphs." +
                            "Arguments are maybe incorrect, check ids of the processes,...", List.of()),
                    BAD_REQUEST);
        }

        return new ResponseEntity(OK);
    }

    /**
     * Deletes a user-defined process with the given ID.
     *
     * @param processGraphId the ID of the process to be deleted
     * @return a response with an HTTP status code of 200 (OK) if the deletion is successful,
     * or a response with an HTTP status code of 500 (Internal Server Error) and a JSON payload
     * containing an error message if the deletion fails
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/process_graphs/{process_graph_id}","openeo/{serviceId:.+}/process_graphs/{process_graph_id}/" }, method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity deleteUserDefinedProcess(@PathVariable("process_graph_id") String processGraphId) {

        boolean result = processBusiness.deleteChainProcess("examind-dynamic", USER_DEFINED_PROCESS_PREFIX + processGraphId);
        if (!result) {
            new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "CannotDeleteProcess", "Impossible do delete the process " + processGraphId, List.of()),
                    INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity(OK);
    }

    /**
     * Runs a process synchronously, and get the result directly
     *
     * @param serviceId the ID of the service to use for running the process
     * @param processHolder containing the process to run
     * @return a response entity containing the result of the process execution with HTTP status code 200 (OK) or
     * if the process can't be started, an error message
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/result", "openeo/{serviceId:.+}/result/"}, method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity runProcessSynchronously(@PathVariable("serviceId") String serviceId,
                                                  @RequestBody final ProcessHolder processHolder) {
        String processId = null;
        final Process process = processHolder.process();
        try {
            putServiceIdParam(serviceId);
            final WPSWorker worker = getWorker(serviceId);

            if (process.getId() == null) {
                process.setId(UUID.randomUUID().toString());
            } else {
                process.setId(process.getId() + "-" + UUID.randomUUID());
            }

            if (worker != null) {
                List<ProcessDescriptor> descriptorList = getDescriptorList();

                CheckMessage checkMessage = process.isProcessGraphValid(descriptorList);
                if (!checkMessage.isValid()) {
                    return new ResponseEntity(
                            new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + checkMessage.getMessage() + " --- The process specified is not valid, check the elements of the process graphs." +
                                    "Arguments are maybe incorrect, check ids of the processes,...", List.of()),
                            BAD_REQUEST);
                }

                // Check if the collection exists in the external STAC endpoint if configured
                List<ResponseMessage> listError = new ArrayList<>();
                externalStac.checkProcessLoadCollectionsWithExternalStac(process, serviceId, listError);
                if (!listError.isEmpty()) {
                    return new ResponseEntity(listError.getFirst() ,BAD_REQUEST);
                }

                try {
                    processId = deployUserDefinedProcess(process, true, false, serviceId);
                } catch (UnsupportedOperationException | IllegalArgumentException | ProcessException ex) {
                    return new ResponseEntity(
                            new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + ex.getMessage() + " --- The process specified is not valid, check the elements of the process graphs." +
                                    "Arguments are maybe incorrect, check ids of the processes,...", List.of()),
                            BAD_REQUEST);
                }

                if (processId != null) {
                    try {
                        worker.updateProcess();
                    } catch (CstlServiceException e) {
                        throw new RuntimeException(e);
                    }

                    //Create Job and Execute it (because isAsync = false)
                    Object response = createProcessJob(processId, worker, process, false);

                    deleteProcess(processId);

                    HttpHeaders headers = new HttpHeaders();

                    if(response != null && response instanceof Path f) {

                        String[] nameSplitted = f.getFileName().toString().split("\\.");
                        String ext = nameSplitted[nameSplitted.length - 1];

                        if(ext.equalsIgnoreCase("tiff") || ext.equalsIgnoreCase("tif")) {
                            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.tiff");
                            headers.add(HttpHeaders.CONTENT_TYPE, MimeType.IMAGE_TIFF);
                        }
                        else if(ext.equalsIgnoreCase("netcdf") || ext.equalsIgnoreCase("nc")) {
                            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.nc");
                            headers.add(HttpHeaders.CONTENT_TYPE, MimeType.NETCDF);
                        }

                        Resource resource = new FileSystemResource(String.valueOf(response));

                        try {
                            return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).body(resource);
                        } catch (IOException e) {
                            return new ResponseEntity(
                                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : Error with the output file - " + e.getMessage(), List.of()),
                                    INTERNAL_SERVER_ERROR);
                        }
                    }

                    return new ResponseEntity(response, OK);
                }
            }
        } catch (Exception ex) {
            deleteProcess(processId);
            return new ResponseEntity(
                    new ResponseMessage(UUID.randomUUID().toString(), "InternalServerError", "Info : " + ex.getMessage(), List.of()),
                    INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Create a process asynchronously (call /jobs/{job_id}/results to run the process / job)
     *
     * @param serviceId the ID of the service to use for running the process
     * @param processHolder containing the process to run
     * @return a response entity containing the jobId with HTTP status code 201 (CREATED) or
     * if the process can't be created, an error message
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs", "openeo/{serviceId:.+}/jobs/"}, method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity createProcessJobAsynchronously(@PathVariable("serviceId") String serviceId,
                                                  @RequestBody final ProcessHolder processHolder) {
        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        Process process = processHolder.process();

        if (worker != null) {
            List<ProcessDescriptor> descriptorList = getDescriptorList();

            if (process.getId() == null) {
                process.setId(UUID.randomUUID().toString());
            } else {
                process.setId(process.getId() + "-" + UUID.randomUUID());
            }

            if (processAlreadyExist(process.getId(), true)) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ProcessIDAlreadyExist", "The process id specified already exist, you cannot use the same to add this process (change id).", List.of()),
                        BAD_REQUEST);
            }

            CheckMessage checkMessage = process.isProcessGraphValid(descriptorList);
            if (!checkMessage.isValid()) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + checkMessage.getMessage() + " --- The process specified is not valid, check the elements of the process graphs." +
                                "Arguments are maybe incorrect, check ids of the processes,...", List.of()),
                        BAD_REQUEST);
            }

            // Check if the collection exists in the external STAC endpoint if configured
            List<ResponseMessage> listError = new ArrayList<>();
            externalStac.checkProcessLoadCollectionsWithExternalStac(process, serviceId, listError);
            if (!listError.isEmpty()) {
                return new ResponseEntity(listError.getFirst() ,BAD_REQUEST);
            }

            String processId;
            try {
                processId = deployUserDefinedProcess(process, true, false, serviceId);
            } catch (UnsupportedOperationException | IllegalArgumentException | ProcessException ex) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "InvalidArgument", "Info : " + ex.getMessage() + " --- The process specified is not valid, check the elements of the process graphs." +
                                "Arguments are maybe incorrect, check ids of the processes,...", List.of()),
                        BAD_REQUEST);
            }

            if (processId != null) {
                try {
                    worker.updateProcess();
                } catch (CstlServiceException e) {
                    throw new RuntimeException(e);
                }

                //Create Job (not started yet) (because isAsync = true)
                Object result = createProcessJob(processId, worker, process, true);

                if (result instanceof StatusInfo statusInfo) {
                    Map<String, String> jobMap = new HashMap<>();
                    jobMap.put("job_id", statusInfo.getJobID());

                    // Keep track of the process associated to the jobId
                    jobIdToProcessMap.put(statusInfo.getJobID(), process);

                    return new ResponseEntity(jobMap, CREATED);
                } else {
                    return new ResponseEntity(CREATED);
                }
            }

        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Get All Jobs
     *
     * @param serviceId the ID of the service to use for running the process
     * @return a response entity containing the list of jobs
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs", "openeo/{serviceId:.+}/jobs/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getAllProcessJobs(@PathVariable("serviceId") String serviceId) {
        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        if (worker != null) {
            Jobs jobs = new Jobs();

            try {
                Set<String> jobIds = worker.getJobList(null);
                for (String jobId : jobIds) {
                    StatusInfo statusInfo = worker.getStatus(new GetStatus("WPS", "2.0.0", jobId));
                    Job job = new Job();
                    job.setId(jobId);
                    job.setStatus(Status.wpsStatusEquivalentTo(statusInfo.getStatus()));
                    job.setCreated(statusInfo.getCreationTime());
                    job.setProgress(statusInfo.getPercentCompleted());

                    Process assiociatedProcess = jobIdToProcessMap.get(jobId);
                    if (assiociatedProcess != null) {
                        job.setProcess(assiociatedProcess);
                    }

                    jobs.addJobsItem(job);
                }

            } catch (CstlServiceException ex) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServerError", "Info : " + ex.getMessage(), List.of()),
                        INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity(jobs, OK);
        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Get a Job
     *
     * @param serviceId the ID of the service to use for running the process
     * @param jobId the ID of the job
     * @return a response entity containing the job
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs/{jobId}", "openeo/{serviceId:.+}/jobs/{jobId}/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getProcessJob(@PathVariable("serviceId") String serviceId, @PathVariable("jobId") String jobId) {
        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        if (worker != null) {
            Job job = new Job();

            try {
                StatusInfo statusInfo = worker.getStatus(new GetStatus("WPS", "2.0.0", jobId));
                if (statusInfo == null) {
                    return new ResponseEntity(
                            new ResponseMessage(UUID.randomUUID().toString(), "JobNotFound", "Info : The job with the id : " + jobId + " was not found.", List.of()),
                            NOT_FOUND);
                }

                job.setId(jobId);
                job.setStatus(Status.wpsStatusEquivalentTo(statusInfo.getStatus()));
                job.setCreated(statusInfo.getCreationTime());
                job.setUpdated(statusInfo.getCreationTime());
                job.setDescription(statusInfo.getMessage());

                Process assiociatedProcess = jobIdToProcessMap.get(jobId);
                if (assiociatedProcess != null) {
                    job.setProcess(assiociatedProcess);
                }

                job.setProgress(statusInfo.getPercentCompleted());

            } catch (CstlServiceException ex) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServerError", "Info : " + ex.getMessage(), List.of()),
                        INTERNAL_SERVER_ERROR);
            }

            return new ResponseEntity(job, OK);
        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Run a job
     *
     * @param serviceId the ID of the service to use for running the process
     * @param jobId the ID of the job
     * @return a response entity containing the status of the job, with an HTTP status code 202 (ACCEPTED)
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs/{job_id}/results", "openeo/{serviceId:.+}/jobs/{job_id}/results/"}, method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity runProcessJobAsynchronously(@PathVariable("serviceId") String serviceId, @PathVariable("job_id") String jobId) {
        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                Object result = worker.runProcess(jobId);
                if (result instanceof StatusInfo statusInfo) {
                    Status status = Status.wpsStatusEquivalentTo(statusInfo.getStatus());
                    return new ResponseEntity(status.toString().split("\\.", 2)[1], ACCEPTED);
                } else {
                    return new ResponseEntity(ACCEPTED);
                }
            } catch (CstlServiceException ex) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServerError", "Info : " + ex.getMessage(), List.of()),
                        INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Get Job Result
     *
     * @param serviceId the ID of the service to use for running the process
     * @param jobId the ID of the job
     * @return a response entity containing the job result as a STAC response structure
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs/{job_id}/results", "openeo/{serviceId:.+}/jobs/{job_id}/results/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getProcessAsyncJobResult(@PathVariable("serviceId") String serviceId, @PathVariable("job_id") String jobId) {
        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        if (worker == null)
            return new ResponseEntity<>("Service ID: " + serviceId + " not found", NOT_FOUND);

        try {
            Object result = worker.getResult(new GetResult("WPS", "2.0.0", jobId));

            if (result == null)
                return new ResponseEntity<>(
                        new ResponseMessage(UUID.randomUUID().toString(), "JobNotFinished", "Info : job is not finished, you can not get the result", List.of()),
                        CONFLICT);

            if (result instanceof Path filePath) {
                // Determine MIME
                String fileName = filePath.getFileName().toString();
                String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                String mimeType = switch (ext) {
                    case "tif", "tiff" -> "image/tiff";
                    case "nc", "netcdf" -> "application/x-netcdf";
                    default -> "application/octet-stream";
                };

                String url = getServiceURL() + "/openeo/" + serviceId;
                final Link link = new Link(url + "/jobs/" + jobId + "/results/download",   "canonical",  mimeType, fileName);

                // Return STAC response structure
                Map<String, Object> stacResponse = Map.of(
                        "stac_version", "1.0.0",
                        "type", "Feature",
                        "id", jobId,
                        "assets", Map.of(
                                "result", link
                        ),
                        "links", List.of(link)
                );

                return ResponseEntity.ok(stacResponse);
            }

            return new ResponseEntity<>(
                    new ResponseMessage(UUID.randomUUID().toString(), "JobResultError", "Info : Job result could not be retrieved", List.of()),
                    FAILED_DEPENDENCY);

        } catch (CstlServiceException ex) {
            return new ResponseEntity<>(
                    new ResponseMessage(UUID.randomUUID().toString(), "ServerError", ex.getMessage(), List.of()),
                    INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Download Job Result File
     *
     * @param serviceId the ID of the service to use for running the process
     * @param jobId the ID of the job
     * @return a response entity containing the job result file as a downloadable resource
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs/{job_id}/results/download", "openeo/{serviceId:.+}/jobs/{job_id}/results/download/"}, method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> downloadJobResultFile(@PathVariable("serviceId") String serviceId, @PathVariable("job_id") String jobId) {

        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                Object result = worker.getResult(new GetResult("WPS", "2.0.0", jobId));

                HttpHeaders headers = new HttpHeaders();
                if(result != null && result instanceof Path f) {

                    String[] nameSplitted = f.getFileName().toString().split("\\.");
                    String ext = nameSplitted[nameSplitted.length - 1];

                    if(ext.equalsIgnoreCase("tiff") || ext.equalsIgnoreCase("tif")) {
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.tiff");
                        headers.add(HttpHeaders.CONTENT_TYPE, MimeType.IMAGE_TIFF);
                    }
                    else if(ext.equalsIgnoreCase("netcdf") || ext.equalsIgnoreCase("nc")) {
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=file.nc");
                        headers.add(HttpHeaders.CONTENT_TYPE, MimeType.NETCDF);
                    }

                    Resource resource = new FileSystemResource(f.toString());
                    try {
                        return ResponseEntity.ok().headers(headers).contentLength(resource.contentLength()).body(resource);
                    } catch (IOException e) {
                        return new ResponseEntity("Error with the output file " + e.getMessage(), INTERNAL_SERVER_ERROR);
                    }
                }

                return new ResponseEntity(result, OK);
            } catch (CstlServiceException ex) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServerError", "Info : " + ex.getMessage(), List.of()),
                        INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Delete a Job
     *
     * @param serviceId the ID of the service to use for running the process
     * @param jobId the ID of the job
     * @return a response entity containing the status of the deletion with HTTP status code 200 (OK)
     */
    @RequestMapping(value = {"openeo/{serviceId:.+}/jobs/{job_id}", "openeo/{serviceId:.+}/jobs/{job_id}/"}, method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity deleteJob(@PathVariable("serviceId") String serviceId, @PathVariable("job_id") String jobId) {
        putServiceIdParam(serviceId);
        final WPSWorker worker = getWorker(serviceId);
        if (worker != null) {
            try {
                String processId = worker.getProcessAssociated(jobId);
                worker.dismiss(new Dismiss("WPS", "2.0.0", jobId));

                // Remove the jobId to process mapping
                jobIdToProcessMap.remove(jobId);

                if (processId != null) {
                    //Process ID returned is like : "urn:exa:wps:examind-dynamic::temp-openeo-evi-execution"
                    //But we only need part after '::'
                    String[] parts = processId.split("::");
                    String lastPart = parts[parts.length - 1];
                    deleteProcess(lastPart);
                }
            } catch (CstlServiceException ex) {
                return new ResponseEntity(
                        new ResponseMessage(UUID.randomUUID().toString(), "ServerError", "Info : " + ex.getMessage(), List.of()),
                        INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity(OK);
        }
        return new ResponseEntity("Service ID : " + serviceId + "not found", NOT_FOUND);
    }

    /**
     * Returns a list of process descriptors for all supported processes.
     *
     * @return a list of process descriptors
     */
    private List<ProcessDescriptor> getDescriptorList() {
        List<ProcessDescriptor> descriptorList = new ArrayList<>();
        for (Iterator<ProcessingRegistry> it = ProcessFinder.getProcessFactories(); it.hasNext(); ) {
            final ProcessingRegistry processingRegistry = it.next();
            for (ProcessDescriptor descriptor : processingRegistry.getDescriptors()) {
                if (WPSUtils.isValidOpenEOProcess(descriptor)) {
                    descriptorList.add(descriptor);
                }
            }
        }
        return descriptorList;
    }

    /**
     * Deploys a user-defined process.
     *
     * @param process     The process to be deployed.
     * @param isTemporary If true, the process will be deleted after its execution.
     * @param acceptParameters If true, the process will accept parameters; otherwise, parameters must be set directly in the process graph.
     * @param serviceId   The ID of the service where the process will be deployed.
     * @return The ID of the deployed process.
     * @throws UnsupportedOperationException If deploying the process is not supported.
     * @throws IllegalArgumentException      If the specified process is invalid.
     * @throws ProcessException              If an error occurs during the deployment of the process.
     */
    private String deployUserDefinedProcess(Process process, boolean isTemporary, boolean acceptParameters, String serviceId)
            throws UnsupportedOperationException, IllegalArgumentException, ProcessException {
        String processId;

        // Sort the process graph to ensure the correct order of execution
        process.sortProcessGraph();

        if (isTemporary) processId = TEMPORARY_USER_DEFINED_PROCESS_PREFIX + process.getId();
        else processId = USER_DEFINED_PROCESS_PREFIX + process.getId();
        final Chain chain = new Chain(processId);
        int id = 1;

        chain.setTitle(process.getSummary());
        chain.setAbstract(process.getDescription());

        if (!acceptParameters && !process.getParameters().isEmpty()) {
            throw new IllegalArgumentException("Parameters are not accepted in this process. Set values directly in the process graph.");
        }

        //Inputs
        final Map<String, Parameter> inputs = new HashMap<>();
        for (ProcessParameter in : process.getParameters()) {
            Class<?> inputParameterClass;
            if (in.getSchema()[0].getType().isEmpty()) {
                inputParameterClass = Object.class;
            } else {
                //TODO : Find a way to put all "types" in input
                inputParameterClass = in.getSchema()[0].getType().getFirst().getClassAssociated(in.getSchema()[0].getSubType());
            }
            final Parameter param = new Parameter(in.getName(), inputParameterClass, in.getName(), in.getDescription(), 1, 1);
            inputs.put(in.getName(), param);
        }
        chain.setInputs(inputs.values().stream().toList());

        //Outputs
        final List<Parameter> outputs = new ArrayList<>();
        //OpenEO accepts only One output
        Class<?> outputParameterClass;
        ProcessReturn returnParam = process.getReturns();
        String description = null;
        if (returnParam != null) {
            description = returnParam.getDescription();
            DataTypeSchema processReturnSchema = process.getReturns().getSchema();
            if (processReturnSchema.getType().isEmpty()) {
                outputParameterClass = Object.class;
            } else {
                //TODO : Find a way to put all "types" in output
                DataTypeSchema.Type type = processReturnSchema.getType().stream().findFirst().orElse(DataTypeSchema.Type.OBJECT);
                outputParameterClass = type.getClassAssociated(processReturnSchema.getSubType());
            }
        } else {
            outputParameterClass = Object.class;
        }

        outputs.add(new Parameter("output", outputParameterClass, "output", description, 1, 1));
        chain.setOutputs(outputs);

        Integer previousElement = BEGIN.getId();
        int elementIndex = 0;
        boolean resultAlreadySet = false;
        Map<String, ElementProcess> elementProcessMap = new HashMap<>();

        for (var entry : process.getProcessGraph().entrySet()) {
            String nodeName = entry.getKey();
            ProcessDescription processDescription = entry.getValue();

            String processIdParsed = openEOProcessIdToExamindProcessId(processDescription.getProcessId(), true);
            String[] splitPoint = processIdParsed.split("\\.", 2);

            final ElementProcess elementProcess = chain.addProcessElement(id++, splitPoint[0], splitPoint[1]);
            elementProcessMap.put(nodeName, elementProcess);

            //Flow links
            chain.addFlowLink(previousElement, elementProcess.getId());
            previousElement = elementProcess.getId();
            if ((elementIndex == process.getProcessGraph().size() - 1 || processDescription.getResult()) && !resultAlreadySet) {
                chain.addFlowLink(elementProcess.getId(), Element.END.getId());
            }

            // HACK : for the load_collection process, we need to force the use of the current serviceId
            // We will use the service id of the current wps service
            if (processDescription.getProcessId().equalsIgnoreCase("load_collection")) {
                final Constant constant = chain.addConstant(id++, String.class, serviceId);
                chain.addDataLink(constant.getId(), "", elementProcess.getId(), "serviceId");
            }

            //Data flow links
            for (var arg : processDescription.getArguments().entrySet()) {
                String argName = arg.getKey();
                ProcessDescriptionArgument argContent = arg.getValue();

                Object value;
                switch (argContent.getType()) {
                    case VALUE, ARRAY:
                        try {
                            value = parseArgumentValue(argContent.getValue(), argContent.getType());
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException("Arg " + argName + " " + ex.getMessage());
                        }

                        final Constant constant = chain.addConstant(id++, (value != null ? value.getClass() : null), value);
                        chain.addDataLink(constant.getId(), "", elementProcess.getId(), argName);
                        break;

                    case FROM_NODE:
                        ElementProcess referencedProcess = elementProcessMap.get((String) argContent.getValue());

                        ProcessingRegistry registry = ProcessFinder.getProcessFactory(referencedProcess.getAuthority());
                        if (registry == null) {
                            throw new IllegalArgumentException("Arg " + argName + " with From_node " + argContent.getValue() + ", cannot find the registry "
                                    + referencedProcess.getAuthority() + " for the process id " + referencedProcess.getCode());
                        }

                        String outputName;
                        try {
                            outputName = registry.getDescriptor(referencedProcess.getCode()).getOutputDescriptor().descriptors().getFirst().getName().getCode();
                        } catch (NoSuchIdentifierException | IndexOutOfBoundsException e) {
                            throw new IllegalArgumentException("Arg " + argName + " with From_node " + argContent.getValue() + ", cannot find the process id " + referencedProcess.getCode());
                        }

                        if (referencedProcess != null) {
                            chain.addDataLink(referencedProcess.getId(), outputName, elementProcess.getId(), argName);
                        } else {
                            throw new IllegalArgumentException("Arg " + argName + " with From_node " + argContent.getValue() + " doesn't exists in other processes referenced in process_graph");
                        }
                        break;

                    case FROM_PARAMETER:
                        Parameter inParam = inputs.get((String) argContent.getValue());
                        if (inParam != null) {
                            //Search id for the corresponding process
                            chain.addDataLink(BEGIN.getId(), inParam.getCode(), elementProcess.getId(), argName);
                        } else {
                            throw new IllegalArgumentException("Arg " + argName + " with From_Parameter " + argContent.getValue() + " doesn't exists in parameter inputs");
                        }
                        break;

                    default:
                        throw new UnsupportedOperationException("Arg content type not supported : " + argContent.getType() + " (for " + argName + ")");
                }
            }

            if ((elementIndex == process.getProcessGraph().size() - 1 || processDescription.getResult()) && !resultAlreadySet) {

                //splitPoint[0] => Authority (ex : examind)
                //splitPoint[1] => Code (ex : math:sum)
                ProcessingRegistry registry = ProcessFinder.getProcessFactory(splitPoint[0]);
                if (registry == null) {
                    throw new IllegalArgumentException("For final output, cannot find the registry " + splitPoint[0] + " for the process id " + splitPoint[1]);
                }

                String outputName;
                try {
                    outputName = registry.getDescriptor(splitPoint[1]).getOutputDescriptor().descriptors().get(0).getName().getCode();
                } catch (NoSuchIdentifierException | IndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("For final output, cannot find the process id " + splitPoint[1]);
                }

                chain.addDataLink(elementProcess.getId(), outputName, Element.END.getId(), "output");
                resultAlreadySet = true;
            }

            elementIndex++;
        }

        try {
            ChainProcess cp = ChainProcessRetriever.convertToDto(chain);
            processBusiness.createChainProcess(cp);

            return processId;
        } catch (ConstellationException ex) {
            throw new ProcessException("Error while creating chain", null);
        }
    }

    private Object parseArgumentValue(Object sourceValue, ProcessDescriptionArgument.ArgumentType type) throws IllegalArgumentException {
        if (type == ProcessDescriptionArgument.ArgumentType.VALUE) {

            if (sourceValue instanceof BoundingBox sourceValueBbox) {
                CoordinateReferenceSystem crs = null;
                try {
                    crs = CRS.forCode(sourceValueBbox.getCrs());
                } catch (FactoryException | NullPointerException ex) {
                    try {
                        crs = CRS.forCode("urn:ogc:def:crs:OGC:2:84");
                    } catch (FactoryException e) {
                        LOGGER.log(Level.WARNING, "Error, cannot find CRS for code \"urn:ogc:def:crs:OGC:2:84\".", e);
                    } //Should never be called
                }

                GeneralEnvelope envelope = new GeneralEnvelope(crs);
                envelope.setEnvelope(sourceValueBbox.getWest(), sourceValueBbox.getSouth(), sourceValueBbox.getEast(), sourceValueBbox.getNorth());

                return envelope;
            }
            return sourceValue;

        } else if (type == ProcessDescriptionArgument.ArgumentType.ARRAY) {

            ArrayList<?> listProcessDescriptionArgument = (ArrayList<?>) sourceValue;
            if (listProcessDescriptionArgument.isEmpty()) {
                return null;
                //throw new IllegalArgumentException("Array value " + sourceValue + " is not valid (0 elements in the array)");
            }

            ArrayList<?> list = (ArrayList<?>) listProcessDescriptionArgument.stream().map(e -> ((ProcessDescriptionArgument)e).getValue()).collect(Collectors.toList());

            Class<?> valueClass = list.getFirst().getClass();

            Class<?> primitiveClass = null;
            if (!valueClass.isPrimitive() && !valueClass.equals(String.class)) {
                if (valueClass == Boolean.class) {
                    primitiveClass = boolean.class;
                } else if (valueClass == Byte.class) {
                    primitiveClass = byte.class;
                } else if (valueClass == Character.class) {
                    primitiveClass = char.class;
                } else if (valueClass == Double.class) {
                    primitiveClass = double.class;
                } else if (valueClass == Float.class) {
                    primitiveClass = float.class;
                } else if (valueClass == Integer.class) {
                    primitiveClass = int.class;
                } else if (valueClass == Long.class) {
                    primitiveClass = long.class;
                } else if (valueClass == Short.class) {
                    primitiveClass = short.class;
                }
            } else {
                primitiveClass = valueClass;
            }

            if (primitiveClass == null) {
                throw new IllegalArgumentException("Array value " + sourceValue + " is not valid (array currently only support primitive type and not objects)");
            }

            Object array = Array.newInstance(primitiveClass, list.size());
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == null && primitiveClass == String.class) {
                    Array.set(array, i, "null");
                } else {
                    Array.set(array, i, list.get(i));
                }
            }

            return array;
        }

        return sourceValue;
    }

    /**
     * Create a process job and execute it directly if it's a synchronous execution (isAsync = false)
     *
     * @param processId Id of the process to be run
     * @param worker WPSWorker instance / service (where the process will be run)
     * @param process Process object description
     * @param isAsync Boolean to define if the process is synchronous or asynchronous (true = asynchronous)
     * @return Object corresponding to the result of the process if synchronous process, or a StatusInfo object with the status of the created job (not started yet)
     */
    private Object createProcessJob(String processId, WPSWorker worker, Process process, boolean isAsync) {
        ProcessingRegistry processingRegistry = ProcessFinder.getProcessFactory("examind-dynamic");
        ProcessDescriptor descriptor;

        if (processingRegistry != null) {
            try {
                descriptor = processingRegistry.getDescriptor(processId);
            } catch (NoSuchIdentifierException e) {
                return false;
            }
        } else {
            return false;
        }

        List<DataInput> dataInputs = new ArrayList<>();
        List<OutputDefinition> outputDefinitions = new ArrayList<>();

        //TODO : In case of parameter, find how they are passed in openEO Request (I didn't find ANYTHING in the doc) https://api.openeo.org/#tag/Data-Processing/operation/compute-result
        //Technically, a runnable process have no parameters (all are set in the process graph, designed by the user)
        /*for(ProcessParameter param : process.getParameters()) {
            Object value;
            if (param.isOptional()) {
                value = param.getDefaultObject();
            }
        }*/

        outputDefinitions.add(new OutputDefinition("urn:exa:wps:examind-dynamic::" + processId + ":output:" +
                descriptor.getOutputDescriptor().descriptors().get(0).getName().getCode(), null));

//        Execute request = new Execute(worker.getId(), "2.0.0", "en", new CodeType("urn:exa:wps:examind-dynamic::" + processId),
//                dataInputs, outputDefinitions, Execute.Response.raw);
        Execute request = new Execute("WPS", "2.0.0", "en", new CodeType("urn:exa:wps:examind-dynamic::" + processId),
                dataInputs, outputDefinitions, Execute.Response.raw);

        if (isAsync) {
            request.setMode(Execute.Mode.async);
        } else {
            request.setMode(Execute.Mode.sync);
        }

        try {
            return worker.execute(request, !isAsync);
        } catch (CstlServiceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a user defined process from process list
     *
     * @param processId Id of the process to delete
     * @return boolean (true if deleted, false if not deleted)
     */
    private boolean deleteProcess(String processId) {
        if (processId != null && !processId.isEmpty()) {
            return processBusiness.deleteChainProcess("examind-dynamic", processId);
        }
        return false;
    }
}
