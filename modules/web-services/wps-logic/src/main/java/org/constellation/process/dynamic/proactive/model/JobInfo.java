/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools Templates
 * and open the template in the editor.
 */
package org.constellation.process.dynamic.proactive.model;
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
import java.util.Map;

/**
 *
* @author Guilhem Legal (Geomatys)
 */
public class JobInfo {
    private JobStatus status;
    private Long startime;
    private Map<String, String> variables;
    private Identifier jobId;
    
    private Integer numberOfFinishedTasks;
    private Long finishedTime;
    private Integer numberOfPendingTasks;
    private Integer numberOfRunningTasks;
    private Integer numberOfFailedTasks;
    private Integer numberOfFaultyTasks;
    private Integer numberOfInErrorTasks;
    private Long inErrorTime;
    private Integer totalNumberOfTasks;
    private Long removedTime;
    private Long submittedTime;
    private Map<String, String> genericInformation; // not sure about the type
    private Boolean toBeRemoved;

    private String jobOwner;
    private JobPriority priority;

    /**
     * @return the status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * @return the startime
     */
    public Long getStartime() {
        return startime;
    }

    /**
     * @param startime the startime to set
     */
    public void setStartime(Long startime) {
        this.startime = startime;
    }

    /**
     * @return the variables
     */
    public Map<String, String> getVariables() {
        return variables;
    }

    /**
     * @param variables the variables to set
     */
    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
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
     * @return the numberOfFinishedTasks
     */
    public Integer getNumberOfFinishedTasks() {
        return numberOfFinishedTasks;
    }

    /**
     * @param numberOfFinishedTasks the numberOfFinishedTasks to set
     */
    public void setNumberOfFinishedTasks(Integer numberOfFinishedTasks) {
        this.numberOfFinishedTasks = numberOfFinishedTasks;
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
     * @return the numberOfPendingTasks
     */
    public Integer getNumberOfPendingTasks() {
        return numberOfPendingTasks;
    }

    /**
     * @param numberOfPendingTasks the numberOfPendingTasks to set
     */
    public void setNumberOfPendingTasks(Integer numberOfPendingTasks) {
        this.numberOfPendingTasks = numberOfPendingTasks;
    }

    /**
     * @return the numberOfRunningTasks
     */
    public Integer getNumberOfRunningTasks() {
        return numberOfRunningTasks;
    }

    /**
     * @param numberOfRunningTasks the numberOfRunningTasks to set
     */
    public void setNumberOfRunningTasks(Integer numberOfRunningTasks) {
        this.numberOfRunningTasks = numberOfRunningTasks;
    }

    /**
     * @return the numberOfFailedTasks
     */
    public Integer getNumberOfFailedTasks() {
        return numberOfFailedTasks;
    }

    /**
     * @param numberOfFailedTasks the numberOfFailedTasks to set
     */
    public void setNumberOfFailedTasks(Integer numberOfFailedTasks) {
        this.numberOfFailedTasks = numberOfFailedTasks;
    }

    /**
     * @return the numberOfFaultyTasks
     */
    public Integer getNumberOfFaultyTasks() {
        return numberOfFaultyTasks;
    }

    /**
     * @param numberOfFaultyTasks the numberOfFaultyTasks to set
     */
    public void setNumberOfFaultyTasks(Integer numberOfFaultyTasks) {
        this.numberOfFaultyTasks = numberOfFaultyTasks;
    }

    /**
     * @return the numberOfInErrorTasks
     */
    public Integer getNumberOfInErrorTasks() {
        return numberOfInErrorTasks;
    }

    /**
     * @param numberOfInErrorTasks the numberOfInErrorTasks to set
     */
    public void setNumberOfInErrorTasks(Integer numberOfInErrorTasks) {
        this.numberOfInErrorTasks = numberOfInErrorTasks;
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
     * @return the totalNumberOfTasks
     */
    public Integer getTotalNumberOfTasks() {
        return totalNumberOfTasks;
    }

    /**
     * @param totalNumberOfTasks the totalNumberOfTasks to set
     */
    public void setTotalNumberOfTasks(Integer totalNumberOfTasks) {
        this.totalNumberOfTasks = totalNumberOfTasks;
    }

    /**
     * @return the removedTime
     */
    public Long getRemovedTime() {
        return removedTime;
    }

    /**
     * @param removedTime the removedTime to set
     */
    public void setRemovedTime(Long removedTime) {
        this.removedTime = removedTime;
    }

    /**
     * @return the submittedTime
     */
    public Long getSubmittedTime() {
        return submittedTime;
    }

    /**
     * @param submittedTime the submittedTime to set
     */
    public void setSubmittedTime(Long submittedTime) {
        this.submittedTime = submittedTime;
    }

    /**
     * @return the genericInformation
     */
    public Map<String, String> getGenericInformation() {
        return genericInformation;
    }

    /**
     * @param genericInformation the genericInformation to set
     */
    public void setGenericInformation(Map<String, String> genericInformation) {
        this.genericInformation = genericInformation;
    }

    /**
     * @return the toBeRemoved
     */
    public Boolean getToBeRemoved() {
        return toBeRemoved;
    }

    /**
     * @param toBeRemoved the toBeRemoved to set
     */
    public void setToBeRemoved(Boolean toBeRemoved) {
        this.toBeRemoved = toBeRemoved;
    }

    /**
     * @return the jobOwner
     */
    public String getJobOwner() {
        return jobOwner;
    }

    /**
     * @param jobOwner the jobOwner to set
     */
    public void setJobOwner(String jobOwner) {
        this.jobOwner = jobOwner;
    }

    /**
     * @return the priority
     */
    public JobPriority getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(JobPriority priority) {
        this.priority = priority;
    }

}
