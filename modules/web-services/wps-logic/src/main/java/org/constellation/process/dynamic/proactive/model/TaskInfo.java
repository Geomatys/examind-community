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
package org.constellation.process.dynamic.proactive.model;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TaskInfo {
private Long startTime;
private Float progress; 
private Identifier jobId;
private Identifier taskId;
private Long finishedTime;
private Long inErrorTime;
private Long scheduledTime;
private TaskStatus taskStatus;
private String executionHostName;
private Integer numberOfExecutionLeft;
private Integer numberOfExecutionOnFailureLeft;
private Long executionDuration;

    /**
     * @return the startTime
     */
    public Long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the progress
     */
    public Float getProgress() {
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(Float progress) {
        this.progress = progress;
    }

    /**
     * @return the jobId
     */
    public Identifier getJobId() {
        return jobId;
    }

    /**
     * @param jobId the jobId to set
     */
    public void setJobId(Identifier jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the taskId
     */
    public Identifier getTaskId() {
        return taskId;
    }

    /**
     * @param taskId the taskId to set
     */
    public void setTaskId(Identifier taskId) {
        this.taskId = taskId;
    }

    /**
     * @return the finishedTime
     */
    public Long getFinishedTime() {
        return finishedTime;
    }

    /**
     * @param finishedTime the finishedTime to set
     */
    public void setFinishedTime(Long finishedTime) {
        this.finishedTime = finishedTime;
    }

    /**
     * @return the inErrorTime
     */
    public Long getInErrorTime() {
        return inErrorTime;
    }

    /**
     * @param inErrorTime the inErrorTime to set
     */
    public void setInErrorTime(Long inErrorTime) {
        this.inErrorTime = inErrorTime;
    }

    /**
     * @return the scheduledTime
     */
    public Long getScheduledTime() {
        return scheduledTime;
    }

    /**
     * @param scheduledTime the scheduledTime to set
     */
    public void setScheduledTime(Long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    /**
     * @return the taskStatus
     */
    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    /**
     * @param taskStatus the taskStatus to set
     */
    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    /**
     * @return the executionHostName
     */
    public String getExecutionHostName() {
        return executionHostName;
    }

    /**
     * @param executionHostName the executionHostName to set
     */
    public void setExecutionHostName(String executionHostName) {
        this.executionHostName = executionHostName;
    }

    /**
     * @return the numberOfExecutionLeft
     */
    public Integer getNumberOfExecutionLeft() {
        return numberOfExecutionLeft;
    }

    /**
     * @param numberOfExecutionLeft the numberOfExecutionLeft to set
     */
    public void setNumberOfExecutionLeft(Integer numberOfExecutionLeft) {
        this.numberOfExecutionLeft = numberOfExecutionLeft;
    }

    /**
     * @return the numberOfExecutionOnFailureLeft
     */
    public Integer getNumberOfExecutionOnFailureLeft() {
        return numberOfExecutionOnFailureLeft;
    }

    /**
     * @param numberOfExecutionOnFailureLeft the numberOfExecutionOnFailureLeft to set
     */
    public void setNumberOfExecutionOnFailureLeft(Integer numberOfExecutionOnFailureLeft) {
        this.numberOfExecutionOnFailureLeft = numberOfExecutionOnFailureLeft;
    }

    /**
     * @return the executionDuration
     */
    public Long getExecutionDuration() {
        return executionDuration;
    }

    /**
     * @param executionDuration the executionDuration to set
     */
    public void setExecutionDuration(Long executionDuration) {
        this.executionDuration = executionDuration;
    }

}
