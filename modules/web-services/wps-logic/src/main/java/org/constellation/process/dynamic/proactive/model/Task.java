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

import java.util.Map;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Task {

    private String description;
    private String tag;
    private Integer maxNumberOfExecution;
    private Map<String, String> genericInformation; // not sure about the type

    private Integer maxNumberOfExecutionOnFailure;
    private Integer iterationIndex;
    private Integer replicationIndex;
    private Integer numberOfNodesNeeded;
    private String name;
    private TaskInfo taskInfo;
    private ParallelEnvironment parallelEnvironment;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * @return the maxNumberOfExecution
     */
    public Integer getMaxNumberOfExecution() {
        return maxNumberOfExecution;
    }

    /**
     * @param maxNumberOfExecution the maxNumberOfExecution to set
     */
    public void setMaxNumberOfExecution(Integer maxNumberOfExecution) {
        this.maxNumberOfExecution = maxNumberOfExecution;
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
     * @return the maxNumberOfExecutionOnFailure
     */
    public Integer getMaxNumberOfExecutionOnFailure() {
        return maxNumberOfExecutionOnFailure;
    }

    /**
     * @param maxNumberOfExecutionOnFailure the maxNumberOfExecutionOnFailure to set
     */
    public void setMaxNumberOfExecutionOnFailure(Integer maxNumberOfExecutionOnFailure) {
        this.maxNumberOfExecutionOnFailure = maxNumberOfExecutionOnFailure;
    }

    /**
     * @return the iterationIndex
     */
    public Integer getIterationIndex() {
        return iterationIndex;
    }

    /**
     * @param iterationIndex the iterationIndex to set
     */
    public void setIterationIndex(Integer iterationIndex) {
        this.iterationIndex = iterationIndex;
    }

    /**
     * @return the replicationIndex
     */
    public Integer getReplicationIndex() {
        return replicationIndex;
    }

    /**
     * @param replicationIndex the replicationIndex to set
     */
    public void setReplicationIndex(Integer replicationIndex) {
        this.replicationIndex = replicationIndex;
    }

    /**
     * @return the numberOfNodesNeeded
     */
    public Integer getNumberOfNodesNeeded() {
        return numberOfNodesNeeded;
    }

    /**
     * @param numberOfNodesNeeded the numberOfNodesNeeded to set
     */
    public void setNumberOfNodesNeeded(Integer numberOfNodesNeeded) {
        this.numberOfNodesNeeded = numberOfNodesNeeded;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the taskInfo
     */
    public TaskInfo getTaskInfo() {
        return taskInfo;
    }

    /**
     * @param taskInfo the taskInfo to set
     */
    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    /**
     * @return the parallelEnvironment
     */
    public ParallelEnvironment getParallelEnvironment() {
        return parallelEnvironment;
    }

    /**
     * @param parallelEnvironment the parallelEnvironment to set
     */
    public void setParallelEnvironment(ParallelEnvironment parallelEnvironment) {
        this.parallelEnvironment = parallelEnvironment;
    }

}
