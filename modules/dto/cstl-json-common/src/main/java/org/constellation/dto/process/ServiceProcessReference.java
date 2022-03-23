/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
import org.constellation.dto.Identifiable;
import org.constellation.dto.service.ServiceComplete;

/**
 * Lite view of a Constellation Service object.
 * Used to refer to an existing Service.
 * Used to define Constellation compatible process input/output.
 *
 * @author Quentin Boileau (Geomatys)
 */
public class ServiceProcessReference extends Identifiable implements Serializable {

    private String type;
    private String name;

    public ServiceProcessReference() {
    }

    public ServiceProcessReference(int id, String type, String name) {
        super(id);
        this.name = name;
        this.type = type;
    }

    public ServiceProcessReference(ServiceComplete service) {
        super(service);
        if (service != null) {
            this.type = service.getType();
            this.name  = service.getIdentifier();
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ServiceProcessReference spr = (ServiceProcessReference) o;
        return Objects.equals(id , spr.id) &&
               Objects.equals(name, spr.name) &&
               Objects.equals(type, spr.type);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + type.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("name:").append(name).append('\n');
        sb.append("type:").append(type).append('\n');
        return sb.toString();
    }
}
