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
package com.examind.wps;

import com.examind.wps.api.UnknowJobException;
import com.examind.wps.api.WPSException;
import com.examind.wps.api.WPSProcess;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.geotoolkit.processing.AbstractProcess;
import org.geotoolkit.wps.xml.v200.StatusInfo;

/**
 *
 * Object containing all the informations about current Executions in WPS.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExecutionInfo {

    private final Map<String, StatusInfo> statusMap = new HashMap<>();
    private final Map<String, Object> resultMap = new HashMap<>();
    private final Map<String, Set<String>> jobMap = new HashMap<>();
    private final Map<String, ProcessAndCallable> processMap = new HashMap<>();
    private final Set<String> dismissedJobs = new HashSet<>();

    public StatusInfo getStatus(String jobId) throws UnknowJobException {
        if (!statusMap.containsKey(jobId)) {
            throw new UnknowJobException("There is no job registrered in the service with the id:" + jobId);
        }
        return statusMap.get(jobId);
    }

    public Object getResult(String jobId) throws UnknowJobException {
        // verify the the job exist by looking for a status (he result can not be set yet)
       if (!statusMap.containsKey(jobId)) {
            throw new UnknowJobException("There is no job registrered in the service with the id:" + jobId);
        }
        return resultMap.get(jobId);
    }

    public Set<String> getJobs(String processId) {
        return jobMap.get(processId);
    }

    public Set<String> getAllJobs() {
        return jobMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public String processIdAssociated(String jobId) {
        for (Map.Entry<String, Set<String>> entry : jobMap.entrySet()) {
            if (entry.getValue().contains(jobId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void addJob(String processId, String jobId, StatusInfo status, WPSProcess process, Callable callable) {
        if (jobMap.containsKey(processId)) {
            jobMap.get(processId).add(jobId);
        } else {
            Set<String> jobs = new HashSet<>();
            jobs.add(jobId);
            jobMap.put(processId, jobs);
        }
        this.statusMap.put(jobId, status);
        this.processMap.put(jobId, new ProcessAndCallable(process, callable));
    }

    public void setStatus(String jobId, StatusInfo status) {
        // if the job is dismissed we don't record status anymore
        if (!dismissedJobs.contains(jobId)) {
            this.statusMap.put(jobId, status);
        }
    }

    public void setResult(String jobId, Object result) {
        // if the job is dismissed we don't record result anymore
        if (!dismissedJobs.contains(jobId)) {
            this.resultMap.put(jobId, result);
        }
    }

    public void dismissJob(String jobId) throws WPSException {
        // verify the the job exist by requesting status
        if (!processMap.containsKey(jobId) || dismissedJobs.contains(jobId)) {
            throw new UnknowJobException("There is no job registrered in the service with the id:" + jobId);
        }
        Callable process = processMap.get(jobId).callable;
        if (process instanceof AbstractProcess) {
            ((AbstractProcess) process).dismissProcess();
            statusMap.remove(jobId);
            processMap.remove(jobId);
            resultMap.remove(jobId);
            for (Set<String> jobList : jobMap.values()) {
                jobList.remove(jobId);
            }
            dismissedJobs.add(jobId);
        } else {
            throw new WPSException("The job :" + jobId + " is not dismissable");
        }
    }

    public WPSProcess getJobProcess(String jobId) throws UnknowJobException {
        if(!processMap.containsKey(jobId)) {
            throw new UnknowJobException("There is no job registrered in the service with the id:" + jobId);
        }
        return processMap.get(jobId).process;
    }

    public Callable getJobCallable(String jobId) throws UnknowJobException {
        if(!processMap.containsKey(jobId)) {
            throw new UnknowJobException("There is no job registrered in the service with the id:" + jobId);
        }
        return processMap.get(jobId).callable;
    }

    private static class ProcessAndCallable {
        public WPSProcess process;
        public Callable callable;

        public ProcessAndCallable(WPSProcess process, Callable callable) {
            this.process = process;
            this.callable = callable;
        }
    }
}
