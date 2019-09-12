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
package org.constellation.scheduler;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.ProcessBusiness;
import org.constellation.api.CstlJobListener;
import org.constellation.api.CstlScheduler;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationSchedulerException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.quartz.ProcessJobDetail;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.impl.matchers.EverythingMatcher.allJobs;
import org.springframework.stereotype.Component;

/**
 *
  * @author Guilhem Legal Geomatys
 */
@Component
public class QuartzScheduler implements CstlScheduler {

    private Scheduler quartzScheduler;

    private static final Logger LOGGER = Logging.getLogger("org.constellation.scheduler");

    private QuartzScheduler() throws ConstellationSchedulerException {
        Properties properties;
        try {
            properties = new Properties();
            properties.load(ProcessBusiness.class.getResourceAsStream("/org/constellation/scheduler/tasks-quartz.properties")); // TODO remove dependency to processBusiness
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load quartz properties", e);
            //use default quartz configuration
            properties = null;
        }

        if (properties != null) {
            try {
                final StdSchedulerFactory schedFact = new StdSchedulerFactory();
                schedFact.initialize(properties);

                quartzScheduler = schedFact.getScheduler();
            } catch (SchedulerException ex) {
                throw new ConstellationSchedulerException(ex);
            }
        }
    }

    @Override
    public void start() throws ConstellationSchedulerException {
        try {
            quartzScheduler.start();
        } catch (SchedulerException ex) {
           throw new ConstellationSchedulerException(ex);
        }
    }

    @Override
    public void addJobListener(CstlJobListener listener) throws ConstellationSchedulerException {
        final JobListener listenerImpl;
        if (listener instanceof JobListener) {
            listenerImpl = (JobListener) listener;
        } else {
            // TODO : create a bridge between Quartz and Examind API. Not done yet, because, CstlJobListener is empty.
            // Furthermore, we should rather refactor the entire job system for something more efficient in term of
            // functionnalities and ease of use.
            throw new UnsupportedOperationException("Cannot attach non quartz listeners. Bridge API missing");
        }
        try {
            quartzScheduler.getListenerManager().addJobListener(listenerImpl, allJobs());
        } catch (SchedulerException ex) {
            throw new ConstellationSchedulerException(ex);
        }
    }

    @Override
    public void scheduleJobNow(String title, Integer taskParameterId, Integer userId, Callable<ParameterValueGroup> process) throws ConstellationSchedulerException {
        final TriggerBuilder tb = TriggerBuilder.newTrigger();
        final Trigger trigger = tb.startNow().build();

        final ProcessJobDetail detail = new QuartzProcessJobDetails((org.geotoolkit.process.Process) process);

        final QuartzTask quartzTask = new QuartzTask(UUID.randomUUID().toString());
        quartzTask.setDetail(detail);
        quartzTask.setTitle(title);
        quartzTask.setTrigger(trigger);
        quartzTask.setTaskParameterId(taskParameterId);
        quartzTask.setUserId(userId);

        //ensure the job detail contain the task in the datamap, this is used in the
        //job listener to track back the task
        quartzTask.getDetail().getJobDataMap().put(QuartzJobListener.PROPERTY_TASK, quartzTask);

        try {
            quartzScheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new ConstellationSchedulerException(e);
        }

        LOGGER.info("Scheduler task added : "+quartzTask.getId()+", "+quartzTask.getTitle()
                        +"   type : "+quartzTask.getDetail().getFactoryIdentifier()+"."+quartzTask.getDetail().getProcessIdentifier());
    }

    @Override
    public void scheduleJobNow(String title, Integer taskParameterId, Integer userId, TaskParameter taskParameter) throws ConstellationSchedulerException {
        final TriggerBuilder tb = TriggerBuilder.newTrigger();
        final Trigger trigger = tb.startNow().build();

        final ProcessJobDetail detail = createJobDetailFromTaskParameter(taskParameter, true);

        final QuartzTask quartzTask = new QuartzTask(UUID.randomUUID().toString());
        quartzTask.setDetail(detail);
        quartzTask.setTitle(title);
        quartzTask.setTrigger(trigger);
        quartzTask.setTaskParameterId(taskParameterId);
        quartzTask.setUserId(userId);

        //ensure the job detail contain the task in the datamap, this is used in the
        //job listener to track back the task
        quartzTask.getDetail().getJobDataMap().put(QuartzJobListener.PROPERTY_TASK, quartzTask);

        try {
            quartzScheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new ConstellationSchedulerException(e);
        }

        LOGGER.info("Scheduler task added : "+quartzTask.getId()+", "+quartzTask.getTitle()
                        +"   type : "+quartzTask.getDetail().getFactoryIdentifier()+"."+quartzTask.getDetail().getProcessIdentifier());
    }

    @Override
    public String scheduleCronJob(String title, Integer taskParameterId, Integer userId, String cronExp, Date endDate, TaskParameter taskParameter) throws ConstellationSchedulerException {
        final ProcessJobDetail jobDetail = createJobDetailFromTaskParameter(taskParameter, false);
        final JobKey key = jobDetail.getKey();

        final TriggerBuilder tb = TriggerBuilder.newTrigger();
        final CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(cronExp);
        final Trigger cronTrigger;
        if (endDate != null) {
            cronTrigger = tb.withSchedule(cronSchedule).forJob(key).endAt(endDate).build();
        } else {
            cronTrigger = tb.withSchedule(cronSchedule).forJob(key).build();
        }
        registerJobInScheduler(title, taskParameterId, userId, cronTrigger, jobDetail);

        return key.getName();
    }

    private void registerJobInScheduler(String title, Integer taskParameterId, Integer userId,
            Trigger trigger, ProcessJobDetail detail) throws ConstellationSchedulerException {
        final QuartzTask quartzTask = new QuartzTask(UUID.randomUUID().toString());
        quartzTask.setDetail(detail);
        quartzTask.setTitle(title);
        quartzTask.setTrigger(trigger);
        quartzTask.setTaskParameterId(taskParameterId);
        quartzTask.setUserId(userId);


        //ensure the job detail contain the task in the datamap, this is used in the
        //job listener to track back the task
        quartzTask.getDetail().getJobDataMap().put(QuartzJobListener.PROPERTY_TASK, quartzTask);
        try {
            quartzScheduler.scheduleJob(quartzTask.getDetail(), quartzTask.getTrigger());
        } catch (SchedulerException ex) {
            throw new ConstellationSchedulerException(ex);
        }
        LOGGER.info("Scheduler task added : "+quartzTask.getId()+", "+quartzTask.getTitle()
                        +"   type : "+quartzTask.getDetail().getFactoryIdentifier()+"."+quartzTask.getDetail().getProcessIdentifier());
    }

    /**
     * Read TaskParameter process description and inputs to create a ProcessJobDetail for quartz scheduler.
     *
     * @param task TaskParameter
     * @param createProcess flag that specified if the process is instantiated in ProcessJobDetails or
     *                      ProcessJobDetails create it-self a new instance each time is executed.
     * @return ProcessJobDetails
     */
    private ProcessJobDetail createJobDetailFromTaskParameter(final TaskParameter task, final boolean createProcess)
            throws ConstellationSchedulerException {

        try {
            final ProcessDescriptor processDesc = Util.getDescriptor(task.getProcessAuthority(), task.getProcessCode());
            final ParameterValueGroup params = Util.readTaskParametersFromJSON(task, processDesc);

            if (createProcess) {
                final ParameterDescriptorGroup originalDesc = processDesc.getInputDescriptor();
                final ParameterValueGroup orig = originalDesc.createValue();
                Parameters.copy(params, orig);
                final org.geotoolkit.process.Process process = processDesc.createProcess(orig);
                return new QuartzProcessJobDetails(process);
            } else {
                return new QuartzProcessJobDetails(task.getProcessAuthority(), task.getProcessCode(), params);
            }
        } catch (ConstellationException ex) {
            throw new ConstellationSchedulerException(ex);
        }
    }

    @Override
    public void interrupt(String keyS) throws ConstellationSchedulerException {
        try {
            // Strip the group / name
            String group = null;
            int pos = keyS.indexOf('.');
            if (keyS.indexOf('.') != -1) {
                group = keyS.substring(0, pos);
                keyS  = keyS.substring(pos + 1, keyS.length());
            }

            // strip the last task part unique uuid
            pos = keyS.lastIndexOf('_');
            if (pos != -1) {
                keyS = keyS.substring(0, pos);
            }

            JobKey key = JobKey.jobKey(keyS, group);
            quartzScheduler.interrupt(key);
        } catch (UnableToInterruptJobException ex) {
             throw new ConstellationSchedulerException(ex);
        }
    }

    @Override
    public boolean deleteJob(String keyS) throws ConstellationSchedulerException {
        try {
            JobKey key = JobKey.jobKey(keyS);
            return quartzScheduler.deleteJob( key);
        } catch (SchedulerException ex) {
            throw new ConstellationSchedulerException(ex);
        }
    }

    @Override
    public void shutdown(boolean b) throws ConstellationSchedulerException {
        try {
            quartzScheduler.shutdown(b);
            quartzScheduler = null;
        } catch (SchedulerException ex) {
            throw new ConstellationSchedulerException(ex);
        }
    }

}
