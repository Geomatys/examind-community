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

/**
 * Lite view of a Constellation Style object.
 * Used to refer to an existing Style.
 * Used to define Constellation compatible process input/output.
 *
 * @author Quentin Boileau (Geomatys)
 */
public class StyleProcessReference extends Identifiable implements Serializable {

    private int id;
    private String name;
    private String type;
    private int provider;

    public StyleProcessReference() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getProvider() {
        return provider;
    }

    public void setProvider(int provider) {
        this.provider = provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final StyleProcessReference other = (StyleProcessReference) o;

        return Objects.equals(this.id, other.id) &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.type, other.type) &&
               Objects.equals(this.provider, other.provider);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + provider;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("name:").append(name).append('\n');
        sb.append("provider:").append(provider).append('\n');
        sb.append("type:").append(type).append('\n');
        return sb.toString();
    }
}
