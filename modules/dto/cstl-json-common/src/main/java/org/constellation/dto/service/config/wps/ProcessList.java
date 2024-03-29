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
package org.constellation.dto.service.config.wps;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.9
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessList {

    @XmlElement(name="Process")
    private List<Process> process;

    public ProcessList() {

    }

    public ProcessList(final List<Process> process) {
        this.process = process;
    }

    /**
     * @return the process
     */
    public List<Process> getProcess() {
        if (process == null) {
            process = new ArrayList<>();
        }
        return process;
    }

    /**
     * @param process the process to set
     */
    public void setProcess(final List<Process> process) {
        this.process = process;
    }

    public void add(String id) {
        if (process == null) {
            process = new ArrayList<>();
        }
        this.process.add(new Process(id));
    }

    public void add(Process p) {
        if (p != null) {
            if (process == null) {
                process = new ArrayList<>();
            }
            this.process.add(p);
        }
    }

    public void add(org.constellation.dto.process.Process p) {
        if (p != null) {
            if (process == null) {
                process = new ArrayList<>();
            }
            this.process.add(new Process(p.getId(), p.getUsePrefix(), p.getJobControlOptions(), p.getOutputTransmission(), p.getUserMap()));
        }
    }

    public void remove(String id) {
        if (process != null) {
            for (Process p : process) {
                if (p.getId().equals(id)) {
                    process.remove(p);
                    return;
                }
            }
        }
    }

    public boolean contains(final String id) {
        for (Process p : getProcess()) {
            if (p.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
