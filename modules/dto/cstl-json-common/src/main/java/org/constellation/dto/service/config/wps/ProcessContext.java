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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.dto.service.config.Languages;

/**
 * Configuration for a WPS service.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Quentin Boileau (Geomatys)
 * @since 0.9
 */
@XmlRootElement(name="ProcessContext")
@XmlAccessorType(XmlAccessType.FIELD)
public class ProcessContext extends AbstractConfigurationObject {

    public static final String PREFIX_IDS = "prefix-identifiers";

    private Processes processes;

    private String security;

    private Languages supportedLanguages;

    /**
     * Path where output wps data will be saved.
     */
    private String outputDirectory;

    /**
     * Identifier of FileCoverageStore provider used by WPS to publish
     * coverages in WMS.
     */
    private String fileCoverageProviderId;

    /**
     * Instance name of the WMS service linked to current WPS.
     */
    private String wmsInstanceName;

    private final Map<String, String> customParameters = new HashMap<>();

    public ProcessContext() {

    }

    public ProcessContext(Processes processes) {
        this.processes = processes;
    }

    public ProcessContext(Processes processes, String security) {
        this.processes = processes;
        this.security = security;
    }

    public Processes getProcesses() {
        return processes;
    }

    /**
     * @return the layers
     */
    public List<ProcessFactory> getProcessFactories() {
        if (processes == null) {
            processes = new Processes();
            return processes.getFactory();
        } else {
            return processes.getFactory();
        }
    }

    public ProcessFactory getProcessFactory(String authorityCode) {
        if (processes != null) {
            for (ProcessFactory factory : processes.getFactory()) {
                if (factory.getAutorityCode().equals(authorityCode)) {
                    return factory;
                }
            }
        }
        return null;
    }

    public void removeProcessFactory(String authorityCode) {
        if (processes != null) {
            for (ProcessFactory factory : processes.getFactory()) {
                if (factory.getAutorityCode().equals(authorityCode)) {
                    processes.getFactory().remove(factory);
                    return;
                }
            }
        }
    }

    /**
     * @param processes the layers to set
     */
    public void setProcesses(List<ProcessFactory> processes) {
        this.processes = new Processes(processes);
    }

    /**
     * @return the security constraint, or {@code null} if none.
     */
    public String getSecurity() {
        return security;
    }

    /**
     * Sets the security value.
     *
     * @param security the security value.
     */
    public void setSecurity(String security) {
        this.security = security;
    }

    /**
     * @return the supportedLanguages
     */
    public Languages getSupportedLanguages() {
        return supportedLanguages;
    }

    /**
     * @param supportedLanguages the supportedLanguages to set
     */
    public void setSupportedLanguages(Languages supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    /**
     * @return the outputDirectory
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * @param outputDirectory the outputDirectory to set
     */
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return the wmsInstanceName
     */
    public String getWmsInstanceName() {
        return wmsInstanceName;
    }

    /**
     * @param wmsInstanceName the wmsInstanceName to set
     */
    public void setWmsInstanceName(String wmsInstanceName) {
        this.wmsInstanceName = wmsInstanceName;
    }

    /**
     * @return the fileCoverageProviderId
     */
    public String getFileCoverageProviderId() {
        return fileCoverageProviderId;
    }


    /**
     * @param fileCoverageProviderId the fileCoverageProviderId to set
     */
    public void setFileCoverageProviderId(String fileCoverageProviderId) {
        this.fileCoverageProviderId = fileCoverageProviderId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProperty(String key) {
        if (customParameters != null) {
            return customParameters.get(key);
        }
        return null;
    }

    /**
     * @return the customParameters
     */
    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    @Override
    public String toString() {
        final String factoryList = getProcessFactories().stream()
                .map(Object::toString)
                .collect(Collectors.joining(System.lineSeparator(), String.format("Process context:%n"), System.lineSeparator()));

        final StringBuilder sb = new StringBuilder(factoryList);
        if (security != null && !security.isEmpty()) {
            sb.append("Security=").append(security);
        }
        if (supportedLanguages != null) {
            sb.append("Supported languages:\n").append(supportedLanguages);
        }
        if (outputDirectory != null) {
            sb.append("Output directory :\n").append(outputDirectory);
        }
        if (wmsInstanceName != null) {
            sb.append("WMS instance name :\n").append(wmsInstanceName);
        }
        if (fileCoverageProviderId != null) {
            sb.append("FileCoverageStore id :\n").append(fileCoverageProviderId);
        }
        if (customParameters != null && !customParameters.isEmpty()) {
            sb.append("Custom parameters:\n");
            for (Map.Entry<String, String> entry : customParameters.entrySet()) {
                sb.append("key:").append(entry.getKey()).append(" value:").append(entry.getValue()).append('\n');
            }
        }
        return sb.toString();
    }

}
