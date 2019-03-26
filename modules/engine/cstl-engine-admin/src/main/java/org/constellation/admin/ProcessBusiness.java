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
package org.constellation.admin;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sis.parameter.ParameterBuilder;
import org.constellation.exception.ConstellationException;
import org.constellation.api.TaskState;
import org.constellation.business.IProcessBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.process.ChainProcess;
import org.constellation.dto.process.Task;
import org.constellation.dto.process.TaskParameter;
import org.constellation.repository.ChainProcessRepository;
import org.constellation.repository.TaskParameterRepository;
import org.constellation.repository.TaskRepository;
import org.constellation.scheduler.QuartzJobListener;
import org.geotoolkit.nio.DirectoryWatcher;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.PathChangeListener;
import org.geotoolkit.nio.PathChangedEvent;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.process.ProcessingRegistry;
import org.geotoolkit.wps.client.WebProcessingClient;
import org.geotoolkit.xml.parameter.ParameterValueReader;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import org.geotoolkit.wps.client.process.WPSProcessingRegistry;
import java.util.concurrent.Callable;
import org.constellation.api.CstlJobListener;
import org.constellation.api.CstlScheduler;
import org.constellation.dto.process.TaskParameterWithOwnerName;
import org.constellation.dto.CstlUser;
import org.constellation.exception.ConstellationSchedulerException;
import org.constellation.repository.UserRepository;
import org.constellation.scheduler.Util;
import org.geotoolkit.client.CapabilitiesException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Cédric Briançon (Geomatys)
 * @author Christophe Mourette (Geomatys)
 * @author Quentin Boileau (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
@Component(ProcessBusiness.BEAN_NAME)
@Primary
//@DependsOn("database-initer")
public class ProcessBusiness implements IProcessBusiness {

    public static final String BEAN_NAME = "processBusiness";

    private static final DateFormat TASK_DATE = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private static final Logger LOGGER = LoggerFactory.getLogger("org.constellation.admin");

    @Inject
    private TaskParameterRepository taskParameterRepository;

    @Inject
    private TaskRepository taskRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private ChainProcessRepository chainRepository;

    @Inject
    private CstlScheduler quartzScheduler;

    @Inject
    private PlatformTransactionManager transactionManager;

    private DirectoryWatcher directoryWatcher;

    private final Map<Integer, Object> scheduledTasks = new HashMap<>();

    @PostConstruct
    public void init(){

        //transaction needed for clean tasks in database
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus arg0) {
                cleanTasksStates();
            }
        });

        LOGGER.info("Starting Constellation Scheduler");
        /*
            Quartz scheduler
         */
        try {
            quartzScheduler.start();

            //listen and attach a process on all geotk process tasks
            quartzScheduler.addJobListener(new QuartzJobListener());

        } catch (ConstellationSchedulerException ex) {
            LOGGER.error("Failed to start quartz scheduler\n"+ex.getLocalizedMessage(), ex);
            return;
        }
        LOGGER.info("Constellation Scheduler successfully started");

        /*
            DirectoryWatcher
         */
        LOGGER.info("Starting directory watcher");
        try {
            directoryWatcher = new DirectoryWatcher(true);

            final PathChangeListener pathListener = new PathChangeListener() {
                @Override
                public void pathChanged(PathChangedEvent event) {
                    if (event.kind.equals(ENTRY_MODIFY) || event.kind.equals(ENTRY_CREATE)) {

                        final Path target = event.target;
                        for (Map.Entry<Integer, Object> sTask : scheduledTasks.entrySet()) {
                            if (sTask.getValue() instanceof Path && target.startsWith((Path) sTask.getValue())) {
                                final Integer taskId = sTask.getKey();
                                final TaskParameter taskParameter = getTaskParameterById(taskId);
                                try {
                                    executeTaskParameter(taskParameter, null, taskParameter.getOwner());
                                } catch (ConstellationException ex) {
                                    LOGGER.warn(ex.getMessage(), ex);
                                }
                            }
                        }
                    }
                }
            };
            directoryWatcher.addPathChangeListener(pathListener);
            directoryWatcher.start();

        } catch (IOException ex) {
            LOGGER.error("Failed to start directory watcher\n"+ex.getLocalizedMessage(), ex);
            return;
        }
        LOGGER.info("Directory watcher successfully started");

        /*
          Re-programme taskParameters with trigger in scheduler.
         */
        List<? extends TaskParameter> programmedTasks = taskParameterRepository.findProgrammedTasks();
        for (TaskParameter taskParameter : programmedTasks) {
            try {
                scheduleTaskParameter(taskParameter, taskParameter.getName(), taskParameter.getOwner(), false);
            } catch (ConstellationException ex) {
                LOGGER.warn(ex.getMessage(), ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Set<String>> listProcess(){
        Map<String, Set<String>> processes = new TreeMap<>();

        final Iterator<ProcessingRegistry> ite = ProcessFinder.getProcessFactories();
        while(ite.hasNext()){
            final ProcessingRegistry factory = ite.next();
            final String authorityCode = factory.getIdentification().getCitation()
                    .getIdentifiers().iterator().next().getCode();
            Set<String> codes = new TreeSet<>(factory.getNames());
            processes.put(authorityCode, codes);
        }

        return processes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listProcessForFactory(final String authorityCode){
        final List<String> names = new ArrayList<>();

        final Iterator<ProcessingRegistry> ite = ProcessFinder.getProcessFactories();
        while(ite.hasNext()){
            final ProcessingRegistry factory = ite.next();
            final String currentAuthorityCode = factory.getIdentification().getCitation()
                    .getIdentifiers().iterator().next().getCode();
            if (currentAuthorityCode.equals(authorityCode)) {
                for(String processCode : factory.getNames()){
                    names.add(processCode);
                }
            }
        }
        return names;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listProcessFactory(){
        final List<String> names = new ArrayList<>();

        final Iterator<ProcessingRegistry> ite = ProcessFinder.getProcessFactories();
        while(ite.hasNext()){
            final ProcessingRegistry factory = ite.next();
            names.add(factory.getIdentification().getCitation()
                    .getIdentifiers().iterator().next().getCode());
        }
        return names;
    }

    @Override
    public TaskParameter getTaskParameterById(Integer id) {
        return taskParameterRepository.get(id);
    }

    @Override
    @Transactional
    public Integer addTaskParameter(TaskParameter taskParameter) {
        return taskParameterRepository.create(taskParameter);
    }

    @Override
    @Transactional
    public void updateTaskParameter(TaskParameter taskParameter) {
        taskParameterRepository.update(taskParameter);
    }

    @Override
    @Transactional
    public void deleteTaskParameter(Integer taskParameterID) {
        taskParameterRepository.delete(taskParameterID);
    }

    @Override
    public List<TaskParameter> findTaskParameterByNameAndProcess(String name, String authority, String code) {
        return (List<TaskParameter>) taskParameterRepository.findAllByNameAndProcess(name, authority, code);
    }

    @Override
    public void registerQuartzListener(CstlJobListener jobListener) throws ConstellationException {
        quartzScheduler.addJobListener(jobListener);
    }

    private ProcessDescriptor getDescriptor(final String authority, final String code) throws ConstellationException {
        final ProcessDescriptor desc;
        try {
            if (authority.startsWith("http")) {
                final WebProcessingClient client = new WebProcessingClient(new URL(authority));
                desc = new WPSProcessingRegistry(client).getDescriptor(code);
            } else {
                desc = ProcessFinder.getProcessDescriptor(authority, code);
            }
        } catch (NoSuchIdentifierException ex) {
            throw new ConstellationException("No Process for id: {" + authority + "}" + code + " has been found");
        } catch (InvalidParameterValueException | MalformedURLException | CapabilitiesException ex) {
            throw new ConstellationException(ex);
        }
        if (desc == null) {
            throw new ConstellationException("No Process for id: {" + authority + "}" + code + " has been found");
        }
        return desc;
    }

    private ParameterValueGroup readTaskParametersFromXML(final TaskParameter taskParameter,
            final ProcessDescriptor processDesc) throws ConstellationException{

        //change the description, always encapsulate in the same namespace and name
        //jaxb object factory can not reconize changing names without a namespace
        final ParameterDescriptorGroup idesc = processDesc.getInputDescriptor();
        final GeneralParameterDescriptor retypedDesc = new ParameterBuilder().addName("input").setRequired(true)
                .createGroup(idesc.descriptors().toArray(new GeneralParameterDescriptor[0]));

        final ParameterValueGroup params;
        final ParameterValueReader reader = new ParameterValueReader(retypedDesc);
        try {
            reader.setInput(taskParameter.getInputs());
            params = (ParameterValueGroup) reader.read();
            reader.dispose();
        } catch (XMLStreamException | IOException ex) {
            throw new ConstellationException(ex);
        }
        return params;
    }

    /**
     * unregister the given task in the scheduler.
     */
    private void unregisterTaskInScheduler(final String key) throws ConstellationSchedulerException {
        quartzScheduler.interrupt(key);
        final boolean removed = quartzScheduler.deleteJob(key);

        if(removed){
            LOGGER.info("Scheduler task removed : "+key);
        }else{
            LOGGER.warn("Scheduler failed to remove task : "+key);
        }

    }

    /**
     * Get specific task from task journal (running or finished)
     * @param uuid task id
     * @return task object
     */
    @Override
    public Task getTask(String uuid) {
        return taskRepository.get(uuid);
    }

    @Override
    @Transactional
    public String addTask(Task task) throws ConstellationException {
        return taskRepository.create(task);
    }

    @Override
    @Transactional
    public void updateTask(Task task) throws ConstellationException {
        taskRepository.update(task);
    }

    @Override
    public List<Task> listRunningTasks() {
        return taskRepository.findRunningTasks();
    }

    @Override
    public List<Task> listRunningTasks(Integer id, Integer offset, Integer limit) {
        return taskRepository.findRunningTasks(id, offset, limit);
    }

    @Override
    public List<Task> listTaskHistory(Integer id, Integer offset, Integer limit) {
        return taskRepository.taskHistory(id, offset, limit);
    }

    @Override
    public List<ChainProcess> getChainModels() throws ConstellationException {
        return chainRepository.findAll();
    }

    @Override
    @Transactional
    public void createChainProcess(final ChainProcess chain) throws ConstellationException {
        chainRepository.create(chain);
    }

    @Override
    @Transactional
    public boolean deleteChainProcess(final String auth, final String code) {
        final Integer chainId = chainRepository.findId(auth, code);
        if (chainId != null) {
            chainRepository.delete(chainId);
            return true;
        }
        return false;
    }

    @Override
    public ChainProcess getChainProcess(final String auth, final String code) {
        return chainRepository.findOne(auth, code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runProcess(final String title, final Callable<ParameterValueGroup> process, final Integer taskParameterId, final Integer userId)
            throws ConstellationException {
        quartzScheduler.scheduleJobNow(title, taskParameterId, userId, process);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeTaskParameter (final TaskParameter taskParameter, String title, final Integer userId)
            throws ConstellationException, ConfigurationException {

        if (title == null) {
            title = taskParameter.getName()+TASK_DATE.format(new Date());
        }
        quartzScheduler.scheduleJobNow(title, taskParameter.getId(), userId, taskParameter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testTaskParameter(TaskParameter taskParameter) throws ConstellationException {
        if (taskParameter.getInputs() != null && !taskParameter.getInputs().isEmpty()) {
            final ProcessDescriptor processDesc = Util.getDescriptor(taskParameter.getProcessAuthority(), taskParameter.getProcessCode());
            Util.readTaskParametersFromJSON(taskParameter, processDesc);
        } else {
            throw new ConfigurationException("No input for task : " + taskParameter.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleTaskParameter (final TaskParameter task, final String title, final Integer userId, boolean checkEndDate)
            throws ConstellationException {

        // Stop previous scheduling first.
        if (scheduledTasks.containsKey(task.getId())) {
            try {
                stopScheduleTaskParameter(task, userId);
            } catch (ConfigurationException e) {
                throw new ConstellationException("Unable to re-schedule task.", e);
            }
        }

        String trigger = task.getTrigger();
        if (task.getTriggerType() != null && trigger != null && !trigger.isEmpty()) {

            if ("CRON".equalsIgnoreCase(task.getTriggerType())) {

                try {
                    String cronExp = null;
                    Date endDate = null;
                    if (trigger.contains("{")) {
                        ObjectMapper jsonMapper = new ObjectMapper();
                        jsonMapper.configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true);

                        Map map = jsonMapper.readValue(trigger, Map.class);

                        cronExp = (String) map.get("cron");
                        long endDateMs =  ((BigInteger)map.get("endDate")).longValue();
                        if (endDateMs > 0) {
                            endDate = new Date(endDateMs);
                        }
                    } else {
                        cronExp = trigger;
                    }

                    if (cronExp == null) {
                        throw new ConstellationException("Invalid cron expression. Can't be empty.");
                    }

                    if (endDate != null && endDate.before(new Date())) {
                        String message = "Task " + task.getName() + " can't be scheduled : end date in the past.";
                        if (checkEndDate) {
                            throw new ConstellationException(message);
                        } else {
                            LOGGER.info(message);
                            return;
                        }
                    }

                    // HACK for Quartz to prevent ParseException :
                    // "Support for specifying both a day-of-week AND a day-of-month parameter is not implemented."
                    // in this case replace the last '*' by '?'
                    if (cronExp.matches("([0-9]\\d{0,1}|\\*) ([0-9]\\d{0,1}|\\*) ([0-9]\\d{0,1}|\\*) \\* ([0-9]\\d{0,1}|\\*) \\*")) {
                        cronExp = cronExp.substring(0, cronExp.length()-1)+ "?";
                    }

                    String key = quartzScheduler.scheduleCronJob(title, task.getId(), userId, cronExp, endDate, task);
                    scheduledTasks.put(task.getId(), key);

                } catch (ConfigurationException | IOException e) {
                    throw new ConstellationException(e.getMessage(), e);
                }

            } else if ("FOLDER".equalsIgnoreCase(task.getTriggerType())) {

                try {
                    final Path folder = IOUtilities.toPath(trigger);
                    if (Files.isDirectory(folder)) {
                        scheduledTasks.put(task.getId(), folder);
                        directoryWatcher.register(folder);
                    } else {
                        throw new ConstellationException("Invalid folder trigger : " + trigger);
                    }
                } catch (IOException e) {
                    // remove task from scheduled list
                    if (scheduledTasks.containsKey(task.getId())) {
                        scheduledTasks.remove(task.getId());
                    }
                    throw new ConstellationException(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void stopScheduleTaskParameter(final TaskParameter task, final Integer userId)
            throws ConstellationException, ConfigurationException {

        if (!scheduledTasks.containsKey(task.getId())) {
            throw new ConstellationException("Task "+task.getName()+" wasn't scheduled.");
        }

        final Object obj = scheduledTasks.get(task.getId());

        //scheduled task
        if (obj instanceof String) {
            unregisterTaskInScheduler((String) obj);
            scheduledTasks.remove(task.getId());

        } else if (obj instanceof Path) {
            //directory watched task
            directoryWatcher.unregister((Path) obj);
            scheduledTasks.remove(task.getId());
        } else {
            throw new ConstellationException("Unable to stop scheduled task " + task.getName());
        }
    }

    @PreDestroy
    @Transactional
    public void stop() {
        LOGGER.info("=== Stopping Scheduler ===");
        try {
            LOGGER.info("=== Wait for job to stop ===");
            quartzScheduler.shutdown(false);
            quartzScheduler = null;
        } catch (ConstellationSchedulerException ex) {
            LOGGER.error("=== Failed to stop quartz scheduler ===", ex);
        }
        LOGGER.info("=== Scheduler successfully stopped ===");

        LOGGER.info("=== Stopping directory watcher ===");
        try {
            directoryWatcher.close();
        } catch (IOException ex) {
            LOGGER.error("=== Failed to stop directory watcher ===", ex);
        }
        LOGGER.info("=== Directory watcher successfully stopped ===");
        cleanTasksStates();
    }

    /**
     * Clear remaining running tasks before server shutdown or after server startup
     */
    private void cleanTasksStates() {
        List<Task> runningTasks = taskRepository.findRunningTasks();

        if (!runningTasks.isEmpty()) {
            LOGGER.info("=== Clear remaining running tasks ===");
        }

        long now = System.currentTimeMillis();
        String msg = "Stopped by server shutdown";
        for (Task runningTask : runningTasks) {
            if (runningTask.getDateEnd() == null) {
                runningTask.setDateEnd(now);
                runningTask.setState(TaskState.CANCELLED.name());
                runningTask.setMessage(msg);
                taskRepository.update(runningTask);
            }
        }
    }

    @Override
    public List<TaskParameterWithOwnerName> getAllTaskParameters() {
        final List<? extends TaskParameter> all = taskParameterRepository.findAll();
        final List<TaskParameterWithOwnerName> ltpwon = new ArrayList<>();
        for (TaskParameter tp : all){
            String owerName = null;
            final Optional<CstlUser> byId = userRepository.findById(tp.getOwner());
            if (byId.isPresent()) {
                owerName = byId.get().getFirstname()+" "+byId.get().getLastname();
            }
            ltpwon.add(new TaskParameterWithOwnerName(tp, owerName));
        }
        return ltpwon;
    }
}
