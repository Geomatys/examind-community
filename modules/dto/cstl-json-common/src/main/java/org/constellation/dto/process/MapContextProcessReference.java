/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
import org.constellation.dto.MapContextDTO;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextProcessReference extends Identifiable implements Serializable {
    
    private String name;

    public MapContextProcessReference() {
    }

    public MapContextProcessReference(int id, String name) {
        super(id);
        this.name = name;
    }

    public MapContextProcessReference(MapContextDTO mp) {
        super(mp);
        if (mp != null) {
            this.name  = mp.getName();
        }
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

        final MapContextProcessReference that = (MapContextProcessReference) o;

        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("name:").append(name).append('\n');
        return sb.toString();
    }
    
}
