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

import java.util.LinkedHashSet;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.9
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceProtocol {
    private String name;

    private Set<String> availableVersions;

    private Set<String> protocol;

    public ServiceProtocol() {

    }

    public ServiceProtocol(final String name, final Set<String> protocol, final Set<String> availableVersions) {
        this.name = name;
        this.protocol = protocol;
        this.availableVersions = availableVersions;
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
     * @return the protocol
     */
    public Set<String> getProtocol() {
        if (protocol == null) {
            this.protocol = new LinkedHashSet<>();
        }
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(Set<String> protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the availableVersions
     */
    public Set<String> getAvailableVersions() {
        if (availableVersions == null) {
            this.availableVersions = new LinkedHashSet<>();
        }
        return availableVersions;
    }

    /**
     * @param availableVersions the availableVersions to set
     */
    public void setAvailableVersions(Set<String> availableVersions) {
        this.availableVersions = availableVersions;
    }

    public ServiceProtocol merge(ServiceProtocol protocol) {
        if (protocol != null) {
            this.getAvailableVersions().addAll(protocol.getAvailableVersions());
            this.getAvailableVersions().addAll(protocol.getProtocol());
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceProtocol that = (ServiceProtocol) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (protocol != null ? !protocol.equals(that.protocol) : that.protocol != null) return false;
        if (availableVersions != null ? !availableVersions.equals(that.availableVersions) : that.availableVersions != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (protocol != null ? protocol.hashCode() : 0);
        result = 31 * result + (availableVersions != null ? availableVersions.hashCode() : 0);
        return result;
    }
}
