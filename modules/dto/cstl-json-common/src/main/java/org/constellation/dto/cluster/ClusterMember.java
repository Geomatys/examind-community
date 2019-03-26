/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.dto.cluster;

import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.service.ServiceComplete;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ClusterMember {

    private String socketAddress;

    private List<ServiceComplete> services;

    /**
     *
     * @return cluster member socket address
     */
    public String getSocketAddress() {
        return socketAddress;
    }

    /**
     *
     * @param socketAddress set member socket address
     */
    public void setSocketAddress(String socketAddress) {
        this.socketAddress = socketAddress;
    }

    /**
     * @return the member services
     */
    public List<ServiceComplete> getServices() {
        if (services == null) {
            services = new ArrayList<>();
        }
        return services;
    }

    /**
     * @param services
     */
    public void setServices(List<ServiceComplete> services) {
        this.services = services;
    }

}
