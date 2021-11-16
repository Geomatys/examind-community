/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.dto.process;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataProcessReference implements Serializable {
    
    private int id;
    private String name;
    private String namespace;
    private String type;
    private int provider;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getProvider() {
        return provider;
    }

    public void setProvider(int provider) {
        this.provider = provider;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            final DataProcessReference other = (DataProcessReference) obj;
            return Objects.equals(this.id, other.id) &&
                   Objects.equals(this.name, other.name) &&
                   Objects.equals(this.namespace, other.namespace) &&
                   Objects.equals(this.type, other.type) &&
                   Objects.equals(this.provider, other.provider);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + this.id;
        hash = 79 * hash + Objects.hashCode(this.name);
        hash = 79 * hash + Objects.hashCode(this.namespace);
        hash = 79 * hash + Objects.hashCode(this.type);
        hash = 79 * hash + this.provider;
        return hash;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataProcessReference{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", namespace='").append(namespace).append('\'');
        sb.append(", provider='").append(provider).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
