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

import org.constellation.dto.service.Instance;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Create a report about a service with the instances informations.
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement(name ="InstanceReport")
@XmlAccessorType(XmlAccessType.FIELD)
public class InstanceReport {

    @XmlElement(name="instance")
    private Set<Instance> instances;

    public InstanceReport() {

    }

    public InstanceReport(final Set<Instance> instances) {
        this.instances = instances;
    }

    /**
     * @return the instances
     */
    public Set<Instance> getInstances() {
        if (instances == null) {
            instances = new LinkedHashSet<>();
        }
        return instances;
    }

    public Instance getInstance(final String identifier) {
        if (instances != null) {
            for (Instance instance : instances) {
                if (instance.getIdentifier().equals(identifier)) {
                    return instance;
                }
            }
        }
        return null;
    }

    public Set<Instance> getInstances(final String type) {
        final Set<Instance> results = new HashSet<>();
        if (instances != null) {
            for (Instance instance : instances) {
                if (instance.getType().equals(type)) {
                    results.add(instance);
                }
            }
        }
        return results;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final InstanceReport that = (InstanceReport) obj;
            return Objects.equals(this.instances, that.instances);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.instances != null ? this.instances.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[InstanceReport]\n");
        sb.append("Instances:\n");
        for (Instance instance : instances) {
            sb.append(instance);
        }
        return sb.toString();
    }
}
