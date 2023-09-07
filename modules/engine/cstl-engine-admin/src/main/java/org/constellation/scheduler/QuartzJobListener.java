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

package org.constellation.scheduler;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.constellation.admin.SpringHelper;
import org.constellation.dto.process.TaskStatus;
import org.constellation.api.TaskState;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.process.Task;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.process.ProcessListener;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.UUID;
import org.constellation.api.CstlJobListener;
import org.constellation.exception.ConstellationException;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.processing.quartz.ProcessJob;
import org.geotoolkit.processing.quartz.ProcessJobDetail;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * Quartz Job listener attaching a listener on geotoolkit processes to track their state.
 *
 * @author Johann Sorel (Geomatys)
 * @author Christophe Mourette (Geomatys)
 */
public class QuartzJobListener implements JobListener, CstlJobListener {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.scheduler");
    private static final int ROUND_SCALE = 2;

    public static final String PROPERTY_TASK = "task";
    private IProcessBusiness processBusiness;

    public QuartzJobListener() {
    }

    ////////////////////////////////////////////////////////////////////////////
    // Quartz listener /////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return "ConstellationJobTracker";
    }

    @Override
    public synchronized void jobToBeExecuted(JobExecutionContext jec) {
        if (processBusiness == null) {
            this.processBusiness = SpringHelper.getBean(IProcessBusiness.class).orElseThrow(IllegalStateException::new);
        }

        final Job job = jec.getJobInstance();
        if(!(job instanceof ProcessJob)) return;
        final ProcessJob pj = (ProcessJob) job;
        final ProcessJobDetail detail = (ProcessJobDetail) jec.getJobDetail();
        final QuartzTask quartzTask = (QuartzTask) detail.getJobDataMap().get(QuartzJobListener.PROPERTY_TASK);
        String jobId = detail.getKey().toString() + '_' + UUID.randomUUID().toString();
        pj.setJobId(jobId);
        SpringHelper.executeInTransaction(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                final Task taskEntity = new Task();
                taskEntity.setIdentifier(jobId);
                taskEntity.setState(TaskState.PENDING.name());
                taskEntity.setTaskParameterId(quartzTask.getTaskParameterId());
                taskEntity.setOwner(quartzTask.getUserId());
                taskEntity.setType(""); // TODO
                try {
                    processBusiness.addTask(taskEntity);
                } catch (ConstellationException ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                }

                final ProcessListener listener = new StateListener(taskEntity.getIdentifier(), quartzTask.getTitle() );
                pj.addListener(listener);
                LOGGER.log(Level.INFO, "Run task "+taskEntity.getIdentifier());
                return null;
            }
        });
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jec) {
        // do nothing
    }

    @Override
    public void jobWasExecuted(JobExecutionContext jec, JobExecutionException jee) {
       if (jee != null) {
            LOGGER.log(Level.WARNING, "Error after job execution.", jee);
            final ProcessJobDetail detail = (ProcessJobDetail) jec.getJobDetail();
            final ProcessJob pj = (ProcessJob) jec.getJobInstance();
            final QuartzTask quartzTask = (QuartzTask) detail.getJobDataMap().get(QuartzJobListener.PROPERTY_TASK);

            // I don't remember why we need this part.
            // normally it should have been covered by the StateListener#failed()
            // i guess it is neccessary when a process throw a runtime exception
            // sdomeone should investigate
            SpringHelper.executeInTransaction(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction(TransactionStatus status) {
                    final Task taskEntity = processBusiness.getTask(pj.getJobId());

                    taskEntity.setState(TaskState.FAILED.name());
                    taskEntity.setDateEnd(System.currentTimeMillis());
                    String message;
                    if (jee.getCause() instanceof ProcessException pe) {
                        message = printException(pe);
                    } else {
                        message = printException(jee);
                    }
                    taskEntity.setMessage(message);
                    try {
                        //update in database
                        processBusiness.updateTask(taskEntity);
                    } catch (ConstellationException ex) {
                        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                    }

                    //send event
                    final TaskStatus taskStatus = new TaskStatus(taskEntity, quartzTask.getTitle());
                    SpringHelper.sendEvent(taskStatus);
                    return null;
                }
            });

        } else {
           final ProcessJob pj = (ProcessJob) jec.getJobInstance();
           LOGGER.info(pj.getJobId() + " Process execution finished");
       }
    }

    /**
     * Print an exception. Format : "message stacktrace"
     *
     * @param exception
     * @return
     */
    private static String printException(Exception exception) {
        StringWriter errors = new StringWriter();
        if (exception != null) {
            errors.append(exception.getMessage()).append('\n');
            exception.printStackTrace(new PrintWriter(errors));
        }
        return errors.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Geotk process listener //////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Catch process events and set them in the TaskState.
     */
    private static class StateListener implements ProcessListener {

        private final String title;
        private final Task taskEntity;
        private IProcessBusiness processBusiness;

        /** Used to store eventual warnings process could send us. */
        private final ArrayList<ProcessEvent> warnings = new ArrayList<>();

        public StateListener(String taskId, String title) {
            if (processBusiness == null) {
                this.processBusiness = SpringHelper.getBean(IProcessBusiness.class).orElseThrow(IllegalStateException::new);
            }
            this.taskEntity = processBusiness.getTask(taskId);
            this.title = title;
        }

        @Override
        public void started(ProcessEvent event) {
            final String newState = TaskState.RUNNING.name();
            final Double progress = roundProgression(event);
            final String msg      = toString(event.getTask());
            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setState(newState);
                    taskEntity.setDateStart(System.currentTimeMillis());
                    taskEntity.setMessage(msg);
                    if (progress != null) taskEntity.setProgress(progress);
                    updateTask(taskEntity);
                }
            }
        }

        @Override
        public void progressing(ProcessEvent event) {
            final String newState = TaskState.RUNNING.name();
            final Double progress = roundProgression(event);
            final String msg      = toString(event.getTask());
            final String output   = getTaskOutput(event.getOutput());
            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setState(newState);
                    taskEntity.setMessage(msg);
                    taskEntity.setTaskOutput(output);
                    if (progress != null) taskEntity.setProgress(progress);
                    if (event.getException() != null) warnings.add(event);
                    updateTask(taskEntity);
                }
            }
        }

        @Override
        public void paused(ProcessEvent event) {
            final String newState = TaskState.PAUSED.name();
            final Double progress = roundProgression(event);
            final String msg      = toString(event.getTask());
            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setState(newState);
                    taskEntity.setMessage(msg);
                    if (progress != null) taskEntity.setProgress(progress);
                    updateTask(taskEntity);
                }
            }
        }

        @Override
        public void resumed(ProcessEvent event) {
            final String newState = TaskState.RUNNING.name();
            final Double progress = roundProgression(event);
            final String msg      = toString(event.getTask());
            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setState(TaskState.RUNNING.name());
                    taskEntity.setMessage(msg);
                    if (progress != null) taskEntity.setProgress(progress);
                    updateTask(taskEntity);
                }
            }
        }

        @Override
        public void completed(ProcessEvent event) {
            final Double progress = roundProgression(event);
            final String output   = getTaskOutput(event.getOutput());
            final String newState;
            final String msg;
            // If a warning occurred, send exception to the user.
            if (!warnings.isEmpty()) {
                newState = TaskState.WARNING.name();
                msg = processWarningMessage();
            } else {
                newState = TaskState.SUCCEED.name();
                msg = toString(event.getTask());
            }
            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setDateEnd(System.currentTimeMillis());
                    taskEntity.setMessage(msg);
                    taskEntity.setState(newState);
                    taskEntity.setTaskOutput(output);
                    if (progress != null) taskEntity.setProgress(progress);
                    updateTask(taskEntity);
                }
            }
        }


        @Override
        public void failed(ProcessEvent event) {
            final String newState = TaskState.FAILED.name();
            final Exception exception = event.getException();
            final String exceptionStr = printException(exception);
            final String msg = toString(event.getTask()) + " cause : " + exceptionStr;

            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setState(newState);
                    taskEntity.setDateEnd(System.currentTimeMillis());
                    taskEntity.setMessage(msg);
                    updateTask(taskEntity);
                }
            }
        }

        @Override
        public void dismissed(ProcessEvent event) {
            final Exception exception = event.getException();
            final String exceptionStr = printException(exception);
            final String newState = TaskState.CANCELLED.name();
            final String msg = toString(event.getTask()) + " cause : " + exceptionStr;
            synchronized (taskEntity) {
                if (!isAlreadyDone(taskEntity.getState(), newState)) {
                    taskEntity.setState(newState);
                    taskEntity.setDateEnd(System.currentTimeMillis());
                    taskEntity.setMessage(msg);
                    updateTask(taskEntity);
                }
            }
        }

        private void updateTask(Task taskEntity) {
            if (processBusiness == null) {
                this.processBusiness = SpringHelper.getBean(IProcessBusiness.class).orElseThrow(IllegalStateException::new);
            }
            try {
                //update in database
                processBusiness.updateTask(taskEntity);
            } catch (ConstellationException ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }

            //send event
            final TaskStatus taskStatus = new TaskStatus(taskEntity, title);
            SpringHelper.sendEvent(taskStatus);
        }

        /**
         * Format :
         * "
         * Task1 description : exceptionMessage
         * stacktrace
         *
         * Task2 description : exceptionMessage
         * stacktrace
         * "
         * @return
         */
        private String processWarningMessage() {
            final StringBuilder warningStr = new StringBuilder();
            for (ProcessEvent warning : warnings) {
                warningStr.append(warning.getTask().toString()).append(" : ");
                warningStr.append(printException(warning.getException()));
                warningStr.append('\n');
            }
            return warningStr.toString();
        }

        /**
         * Transform a parameter value group in a JSON String.
         * If the serialisation fail, it will only log the error and return {@code null}.
         * 
         * @param output A proccess output result.
         *
         * @return A JSON String or {@code null}.
         */
        private String getTaskOutput(ParameterValueGroup output) {
            if (output != null) {
                try {
                    return ParamUtilities.writeParameterJSON(output);
                } catch (JsonProcessingException e) {
                    LOGGER.log(Level.WARNING, "Process output serialization failed", e);
                }
            }
            return null;
        }

        /**
         * Round event progression value to {@link #ROUND_SCALE} before
         * set to taskEntity object.
         *
         * @param event ProcessEvent
         */
        private Double roundProgression(ProcessEvent event) {
            if (!Float.isNaN(event.getProgress()) && !Float.isInfinite(event.getProgress())) {
                BigDecimal progress = BigDecimal.valueOf(event.getProgress());
                progress = progress.setScale(ROUND_SCALE, BigDecimal.ROUND_HALF_UP);
                return progress.doubleValue();
            }
            return null;
        }

        private String toString(InternationalString str){
            return str != null ? str.toString() : "";
        }

        private boolean isAlreadyDone(String currentState, String newState) {
            boolean alreadyDone =
                    TaskState.FAILED.name().equals(currentState)    ||
                    TaskState.CANCELLED.name().equals(currentState) ||
                    TaskState.SUCCEED.name().equals(currentState);
            if (alreadyDone) {
                LOGGER.warning("A task try to change the state of a process to: " + newState + ", after the task was reported as " + currentState);
            }
            return alreadyDone;
        }
    }
}
