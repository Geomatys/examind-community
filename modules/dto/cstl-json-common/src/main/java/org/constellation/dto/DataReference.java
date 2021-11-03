/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataReference extends Identifiable {

    private String name;
    private String namespace;
    private Integer providerId;

    public DataReference() {

    }
    
    public DataReference(Integer id, String name, String namespace, Integer providerId) {
        super(id);
        this.name = name;
        this.namespace = namespace;
        this.providerId = providerId;
    }

    public DataReference(DataReference data) {
        super(data);
        if (data != null) {
            this.name = data.name;
            this.namespace = data.namespace;
            this.providerId = data.providerId;
        }
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
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the provider
     */
    public Integer getProviderId() {
        return providerId;
    }

    /**
     * @param providerId the provider identifier to set
     */
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.name != null) {
            sb.append("name: ").append(name).append('\n');
        }
        if (this.namespace != null) {
            sb.append("namespace: ").append(namespace).append('\n');
        }
        if (this.providerId != null) {
            sb.append("providerId: ").append(providerId).append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DataReference && super.equals(obj)) {
            DataReference that = (DataReference) obj;
            return     Objects.equals(this.name, that.name)
                    && Objects.equals(this.namespace, that.namespace)
                    && Objects.equals(this.providerId, that.providerId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + super.hashCode();
        hash = 71 * hash + Objects.hashCode(this.name);
        hash = 71 * hash + Objects.hashCode(this.namespace);
        hash = 71 * hash + Objects.hashCode(this.providerId);
        return hash;
    }
}
