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
public class Job {
    
    private String owner;
    private String projectName;
    private String name;
    private String priority;
    private Integer id;
    private JobInfo jobInfo;
    private Map<String, Task> tasks;
    private Map<String, String> genericInformation; // not sure about the type

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
     * @return the priority
     */
    public String getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the tasks
     */
    public Map<String, Task> getTasks() {
        return tasks;
    }

    /**
     * @param tasks the tasks to set
     */
    public void setTasks(Map<String, Task> tasks) {
        this.tasks = tasks;
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
     * @return the jobInfo
     */
    public JobInfo getJobInfo() {
        return jobInfo;
    }

    /**
     * @param jobInfo the jobInfo to set
     */
    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }
    

}
