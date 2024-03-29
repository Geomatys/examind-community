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

package org.constellation.dto.service;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Create a report about the available services.
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.7
 */
@XmlRootElement(name ="ServiceReport")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceReport {

    private List<ServiceProtocol> availableServices = new ArrayList<>();

    public ServiceReport() {

    }

    public ServiceReport(final Map<String, ServiceProtocol> availableServices) {
        setAvailableServices(availableServices);
    }

    /**
     * @return the availableServices
     */
    public Map<String, ServiceProtocol> getAvailableServices() {
        final Map<String, ServiceProtocol> response = new HashMap<>();
        for (ServiceProtocol sp : availableServices) {
            response.put(sp.getName().toLowerCase(), sp);
        }
        return response;
    }

    /**
     * @param availableServices the availableServices to set
     */
    public final void setAvailableServices(final Map<String, ServiceProtocol> availableServices) {
        if (availableServices != null) {
            for (Entry<String, ServiceProtocol> entry : availableServices.entrySet()) {
                this.availableServices.add(entry.getValue());
            }
        }
    }
}
