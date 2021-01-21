/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.dto.service.config.sos;

import java.util.List;
import java.util.Map;

/**
 *
 * @author guilhem
 */
public class SOSProviderCapabilities {

    public final Map<String, List<String>> responseFormats;
    public final List<String> responseModes;
    public final List<String> queryableResultProperties;
    public final boolean isBoundedObservation;
    public final boolean computeCollectionBound;
    public final boolean isDefaultTemplateTime;
    public final boolean hasFilter;

    public SOSProviderCapabilities(Map<String, List<String>> responseFormats , List<String> responseModes,
            List<String> queryableResultProperties , boolean isBoundedObservation, boolean computeCollectionBound,
            boolean isDefaultTemplateTime, boolean hasFilter) {
        this.responseFormats = responseFormats;
        this.responseModes = responseModes;
        this.queryableResultProperties = queryableResultProperties;
        this.isBoundedObservation = isBoundedObservation;
        this.computeCollectionBound = computeCollectionBound;
        this.isDefaultTemplateTime = isDefaultTemplateTime;
        this.hasFilter = hasFilter;
    }
}
