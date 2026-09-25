/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.openeo.api.rest.service;

import java.util.Map;

/**
 * Examind-internal bookkeeping for an openEO secondary service (WMS/WCS), persisted as JSON
 * in {@link org.constellation.dto.process.Task#getTaskOutput()}. This is NOT the openEO wire
 * DTO (see {@link org.geotoolkit.openeo.dto.service.Service}) - it carries the extra ids needed
 * to manage the underlying Examind service/layer/provider/data on update and delete.
 *
 * @author Quentin BIALOTA (Geomatys)
 */
public class SecondaryServiceRecord {

    private String title;
    private String description;
    private String type;
    private boolean enabled;
    private Object process;
    private Map<String, Object> configuration;
    private String url;
    private Integer examindServiceId;
    private Integer examindLayerId;
    private Integer examindProviderId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Object getProcess() { return process; }
    public void setProcess(Object process) { this.process = process; }

    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Integer getExamindServiceId() { return examindServiceId; }
    public void setExamindServiceId(Integer examindServiceId) { this.examindServiceId = examindServiceId; }

    public Integer getExamindLayerId() { return examindLayerId; }
    public void setExamindLayerId(Integer examindLayerId) { this.examindLayerId = examindLayerId; }

    public Integer getExamindProviderId() { return examindProviderId; }
    public void setExamindProviderId(Integer examindProviderId) { this.examindProviderId = examindProviderId; }
}
