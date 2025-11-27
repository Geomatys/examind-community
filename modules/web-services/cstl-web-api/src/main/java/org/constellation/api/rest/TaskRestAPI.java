/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2017 Geomatys.
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
package org.constellation.api.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.process.TaskParameterWithOwnerName;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.CstlUser;
import org.constellation.dto.process.Task;
import org.constellation.dto.process.TaskParameter;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.StringList;
import org.constellation.dto.process.TaskStatus;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.MapContextProcessReference;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.dto.process.UserProcessReference;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.client.CapabilitiesException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.wps.client.WebProcessingClient;
import org.geotoolkit.wps.client.process.WPSProcessingRegistry;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * RestFull API for task management/operations.
 *
 * @author Johann Sorel (Geomatys)
 */
@RestController
public class TaskRestAPI extends AbstractRestAPI {
    private static final DateFormat TASK_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    @Autowired
    private IProcessBusiness processBusiness;

    /**
     * DatasetBusiness used for provider GUI editors data
     */
    @Autowired
    private IDatasetBusiness datasetBusiness;

    /**
     * DatasetBusiness used for provider GUI editors data
     */
    @Autowired
    private IDataBusiness dataBusiness;

    /**
     * ServiceBusiness used for provider GUI editors data
     */
    @Autowired
    private IServiceBusiness serviceBusiness;
    
    /**
     * MapContextBusiness used for provider GUI editors data
     */
    @Autowired
    private IMapContextBusiness mapcontextBusiness;

    /**
     * StyleBusiness used for provider GUI editors data
     */
    @Autowired
    private IStyleBusiness styleBusiness;

    /**
     * Returns a list of all process available in the current factories.
     *
     * @return A list of formated string "authority:code"
     */
    @RequestMapping(value="/task/listProcesses",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listProcess(){

        final Map<String, Set<String>> registryMap = processBusiness.listProcess();

        final List<String> result = new ArrayList<>();

        for (Map.Entry<String, Set<String>> registry : registryMap.entrySet()) {
            final Set<String> processes = registry.getValue();
            for (String process : processes) {
                result.add(registry.getKey() +":"+ process);
            }
        }

        final Map<String, List<String>> wrapper = Collections.singletonMap("list", result);
        return new ResponseEntity(wrapper, OK);
    }

    /**
     * Returns a list of all process factories available.
     *
     * @return A list of authority names.
     */
    @RequestMapping(value="/task/listProcessFactories",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listProcessFactories(){
        final List<String> names = processBusiness.listProcessFactory();
        return new ResponseEntity(new StringList(names), OK);
    }

    /**
     * Returns a list of all process available for the specified factory.
     *
     * @param authorityCode the process factory authority code.
     * @return A list of process codes.
     */
    @RequestMapping(value="/task/process/factory/{authorityCode}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listProcessForFactory(final @PathVariable("authorityCode") String authorityCode) {
        return new ResponseEntity(new StringList(processBusiness.listProcessForFactory(authorityCode)), OK);
    }

    /**
     * Returns the count of all process available in the current factories.
     *
     * @return The number of process available.
     */
    @RequestMapping(value="/task/countProcesses",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity countAvailableProcess(){
        final Map<String, Set<String>> registryMap = processBusiness.listProcess();
        int count = 0;
        for (Set<String> strings : registryMap.values()) {
            count += strings.size();
        }

        return new ResponseEntity(Collections.singletonMap("count", count), OK);
    }

    /**
     * Returns a description of the process parameters.
     *
     * @param params a map containing two parameter with key "authority" and "code".
     * @return A description of the process inputs parameters.
     */
    @RequestMapping(value="/task/process/descriptor",method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> getProcessDescriptor(@RequestBody final ParameterValues params) {
        final ParameterDescriptorGroup idesc;
        try {
            idesc = getDescriptor(params.get("authority"), params.get("code"));
        } catch (ConfigurationException e) {
            return new ErrorMessage(NOT_FOUND).message("Failure,Could not find process description for given authority/code.").build();
        }
        try {
            final String jsonString = ParamUtilities.writeParameterDescriptorJSON(idesc);
            return ResponseEntity.ok(new ByteArrayResource(jsonString.getBytes(StandardCharsets.UTF_8)));
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).message("Failure, Could not find chain for given authority/code.").build();
        }
    }

    /**
     * Return a the parameter input descriptor for a process (local or distant on a WPS).
     *
     * @param authority identifier of the process factory.
     * @param code identifier of the process.
     *
     * @return The input descriptor of the specified process.
     */
    private ParameterDescriptorGroup getDescriptor(final String authority, final String code) throws ConfigurationException {
        final ProcessDescriptor desc;
        try {
            if(authority.startsWith("http")) {
                final WebProcessingClient client = new WebProcessingClient(new URL(authority));
                desc = new WPSProcessingRegistry(client).getDescriptor(code);
            } else {
                desc = ProcessFinder.getProcessDescriptor(authority,code);
            }
        } catch (NoSuchIdentifierException ex) {
            throw new ConfigurationException("No Process for id : {" + authority + "}"+code+" has been found");
        } catch (InvalidParameterValueException | MalformedURLException | CapabilitiesException ex) {
            throw new ConfigurationException(ex);
        }
        if(desc == null){
            throw new ConfigurationException("No Process for id : {" + authority + "}"+code+" has been found");
        }

        //change the description, always encapsulate in the same namespace and name
        //jaxb object factory can not reconize changing names without a namespace
        ParameterDescriptorGroup idesc = desc.getInputDescriptor();
        idesc = new ParameterBuilder().addName("input").setRequired(true)
                .createGroup(idesc.descriptors().toArray(new GeneralParameterDescriptor[0]));
        return idesc;
    }

    /**
     * Create a process chain.
     *
     * @param chain The process chain to save.
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/chain",method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createChain(final Chain chain) {
        try {
            ChainProcess cp = ChainProcessRetriever.convertToDto(chain);
            processBusiness.createChainProcess(cp);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).message("Failure, Could not find chain for given authority/code.").build();
        }
    }

    /**
     * Delete a process chain.
     *
     * @param authority The process chain authority.
     * @param code The process chain code.
     *
     * @return HTTP code 200 if the operation suceed.
     */
    @RequestMapping(value="/task/chain/{authority}/{code}",method=DELETE,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteChain(final @PathVariable("authority") String authority, final @PathVariable("code") String code) {
        if (processBusiness.deleteChainProcess(authority, code)) {
            return new ResponseEntity(new AcknowlegementType("Success", "The chain has been deleted"),OK);
        }
        return new ErrorMessage().message("Failure, Could not find chain for given authority/code.").build();
    }

    /**
     * Create a new task with the specified parameters
     *
     * @param taskParameter the task to save.
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/create",method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createParamsTask(@RequestBody final TaskParameter taskParameter, HttpServletRequest req) {
        try {
            int userId = assertAuthentificated(req);
            processBusiness.testTaskParameter(taskParameter);
            taskParameter.setOwner(userId);
            taskParameter.setDate(System.currentTimeMillis());
            Integer id = processBusiness.addTaskParameter(taskParameter);
            return new ResponseEntity(id, OK);

        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Update a task with the specified parameters
     *
     * @param taskParameter the task to update.
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/update",method=POST,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity updateParamsTask(@RequestBody final TaskParameter taskParameter) {
        try {
            processBusiness.testTaskParameter(taskParameter);
            processBusiness.updateTaskParameter(taskParameter);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return the input parameters for the specified task.
     *
     * @param id Task identifier.
     * @return The input parameters for the specified task.
     */
    @RequestMapping(value="/task/params/get/{id}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getParamsTask(final @PathVariable("id") Integer id) {
        final TaskParameter taskParameter = processBusiness.getTaskParameterById(id);
        if (taskParameter != null) {
            return new ResponseEntity(taskParameter, OK);
        }
        return new ErrorMessage(BAD_REQUEST).build();
    }

    /**
     * Remove the specified task.
     *
     * @param id Task identifier.
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/delete/{id}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteParamsTask(final @PathVariable("id") Integer id) {
        try {
            processBusiness.deleteTaskParameter(id);
            return new ResponseEntity(OK);
        } catch (ConstellationException ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Duplicate a task and create a new one with the name "duplicatedName" + "(COPY)".
     *
     * @param id task to duplicate identifier
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/duplicate/{id}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity duplicateParamsTask(final @PathVariable("id") Integer id, HttpServletRequest req) {
        try {
            int userId = assertAuthentificated(req);
            final TaskParameter taskParameter = processBusiness.getTaskParameterById(id);
            taskParameter.setId(null);
            taskParameter.setName(taskParameter.getName()+"(COPY)");
            taskParameter.setOwner(userId);
            final Integer duplicatedID = processBusiness.addTaskParameter(taskParameter);
            final TaskParameter duplicated = processBusiness.getTaskParameterById(duplicatedID);
            return new ResponseEntity(duplicated, OK);
        } catch (Exception ex) {
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Execute a task.
     *
     * @param id task to execute identifier.
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/execute/{id}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity executeParamsTask(final @PathVariable("id") Integer id, HttpServletRequest req) {
        final TaskParameter taskParameter = processBusiness.getTaskParameterById(id);
        final String title = taskParameter.getName()+" "+TASK_DATE.format(new Date());
        try {
            int userId = assertAuthentificated(req);
            processBusiness.executeTaskParameter(taskParameter, title, userId);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }

        return new ResponseEntity(OK);
    }

    /**
     * Schedule the execution of a task.
     *
     * @param id task to schedule identifier.
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/schedule/start/{id}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity startScheduleParamsTask(final @PathVariable("id") Integer id, HttpServletRequest req) {
        final Date now = new Date();
        final TaskParameter taskParameter = processBusiness.getTaskParameterById(id);
        final String title = taskParameter.getName()+" "+TASK_DATE.format(now);

        try {
            int userId = assertAuthentificated(req);
            processBusiness.scheduleTaskParameter(taskParameter, title, userId, true);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
        return new ResponseEntity(OK);
    }

    /**
     * Un-schedule the execution of a task.
     *
     * @param id task to un-schedule identifier.
     *
     * @return HTTP code 200 if the operation succeed.
     */
    @RequestMapping(value="/task/params/schedule/stop/{id}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity stopScheduleParamsTask(final @PathVariable("id") Integer id, HttpServletRequest req) {
        try {
            assertAuthentificated(req);
            processBusiness.stopScheduleTaskParameter(id);
        } catch (ConstellationException ex) {
            return new ErrorMessage(INTERNAL_SERVER_ERROR).error(ex).message("Failure to un-schedule  task: "+ex.getMessage()).build();
        }
        return new ResponseEntity("Success, The task has been un-schedule",OK);
    }

    /**
     * Return the list of all the tasks (with input parameters and owner name).
     *
     * @return a list of task.
     */
    @RequestMapping(value="/task/params/list",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listParamsTask() {
        return new ResponseEntity(processBusiness.getAllTaskParameters(),OK);
    }

    /**
     * List all datasets as DatasetProcessReference to GUI editors.
     *
     * @return list of DatasetProcessReference
     */
    @RequestMapping(value="/task/list/datasets",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasetProcessReferenceList() {
        final List<DatasetProcessReference> datasetPRef = datasetBusiness.getAllDatasetReference();
        return new ResponseEntity(datasetPRef, OK);
    }

    /**
     * List all data as DataProcessReference to GUI editors.
     *
     * @param type filter on data type. can be  {@code null}
     * @return list of DataProcessReference
     */
    @RequestMapping(value="/task/list/datas",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasProcessReferenceList(@RequestParam(name = "type", required = false) String type) {
        final List<DataProcessReference> dataPRef = dataBusiness.findDataProcessReference(type);
        return new ResponseEntity(dataPRef, OK);
    }

    /**
     * List all Services as ServiceProcessReference to GUI editors.
     *
     * @return list of ServiceProcessReference
     */
    @RequestMapping(value="/task/list/services",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getServiceProcessReferenceList() {
        try {
            final List<ServiceProcessReference> servicePRef = new ArrayList<>();
            final List<ServiceComplete> services = serviceBusiness.getAllServices(null);
            if (services != null) {
                for (final ServiceComplete service : services) {
                    final ServiceProcessReference ref = new ServiceProcessReference(service);
                    servicePRef.add(ref);
                }
            }
            return new ResponseEntity(servicePRef, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
    
    /**
     * List all Services as ServiceProcessReference to GUI editors.
     *
     * @return list of ServiceProcessReference
     */
    @RequestMapping(value="/task/list/mapcontexts",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getMapContextProcessReferenceList() {
        try {
            final List<MapContextProcessReference> mpRef = new ArrayList<>();
            final List<MapContextDTO> mps = mapcontextBusiness.getAllContexts(false);
            if (mps != null) {
                for (final MapContextDTO mp : mps) {
                    final MapContextProcessReference ref = new MapContextProcessReference(mp);
                    mpRef.add(ref);
                }
            }
            return new ResponseEntity(mpRef, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * List all Style as StyleProcessReference to GUI editors.
     *
     * @return list of ServiceProcessReference
     */
    @RequestMapping(value="/task/list/styles",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getStyleProcessReferenceList() {
        try {
            final List<StyleProcessReference> servicePRef = styleBusiness.getAllStyleReferences("sld");
            return new ResponseEntity(servicePRef, OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * List all User as UserProcessReference to GUI editors.
     *
     * @return list of UserProcessReference
     */
    @RequestMapping(value="/task/list/users",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getUserProcessReferenceList() {
        final List<UserProcessReference> userPRef = new ArrayList<>();
        final List<CstlUser> users = userBusiness.findAll();
        if (users != null) {
            for (final CstlUser user : users) {
                final UserProcessReference ref = new UserProcessReference(user);
                userPRef.add(ref);
            }
        }
        return new ResponseEntity(userPRef, OK);
    }

    /**
     * List all the saved tasks.
     *
     * @return A list of tasks.
     */
    @RequestMapping(value="/task/listTasks",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listTasks() {
        final List<Task> tasks = processBusiness.listRunningTasks();
        Map<Integer, List<TaskStatus>> map = new HashMap<>();

        for (Task task : tasks) {
            Integer taskParameterId = task.getTaskParameterId();

            if (!map.containsKey(taskParameterId)) {
                map.put(taskParameterId, new ArrayList<>());
            }
            map.get(taskParameterId).add(toTaskStatus(task));
        }

        return new ResponseEntity(map, OK);
    }

    /**
     * List execution history for the specified task.
     *
     * @param id Task identifier.
     * @param limit Maximum numer of returned results.
     *
     * @return A list of task status
     */
    @RequestMapping(value="/task/taskHistory/{id}/{limit}",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listHistoryForTaskParameter(
            final @PathVariable("id") Integer id,
            final @PathVariable("limit") Integer limit) {
        final List<Task> tasks = processBusiness.listTaskHistory(id, 0, limit);

        List<TaskStatus> lst = new ArrayList<>();
        for(Task task : tasks) {
            lst.add(toTaskStatus(task));
        }
        return new ResponseEntity(lst, OK);
    }

    /**
     * List last execution history for the all the task.
     *
     * @return A list of task status
     */
    @RequestMapping(value="/task/taskHistory",method=GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity listHistoryForTaskParameters() {
        List<TaskStatus> results = new ArrayList<>();
        final List<TaskParameterWithOwnerName> taskONs = processBusiness.getAllTaskParameters();
        for (TaskParameterWithOwnerName taskON : taskONs) {
            final List<Task> tasks = processBusiness.listTaskHistory(taskON.getId(), 0, 1);
            for (Task task : tasks) {
                results.add(toTaskStatus(task));
            }
        }
        return new ResponseEntity(results, OK);
    }

    /**
     * List last execution history for the all the task.
     *
     * @return A list of task status
     */
    @RequestMapping(value="/task/params/delete/{id}",method=DELETE)
    public ResponseEntity cancelTaskForTaskParameter(final @PathVariable("id") Integer id) {
        try {
            processBusiness.cancelTaskForTaskParameter(id);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * List last execution history for the all the task.
     *
     * @return A list of task status
     */
    @RequestMapping(value="/task/{taskId:.+}",method=DELETE)
    public ResponseEntity cancelTask(final @PathVariable("taskId") String taskId) {
        try {
            processBusiness.cancelTask(taskId);
            return new ResponseEntity(OK);
        } catch(Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    private TaskStatus toTaskStatus(Task task) {
        final TaskStatus status = new TaskStatus();
        status.setId(task.getIdentifier());
        status.setTaskId(task.getTaskParameterId());
        status.setMessage(task.getMessage());
        status.setPercent(task.getProgress() != null ? task.getProgress().floatValue() : 0f);
        status.setStatus(task.getState());
        status.setStart(task.getDateStart());
        status.setEnd(task.getDateEnd());
        status.setOutput(task.getTaskOutput());

        final TaskParameter taskParameter = processBusiness.getTaskParameterById(task.getTaskParameterId());
        status.setTitle(taskParameter.getName());
        return status;
    }
}
