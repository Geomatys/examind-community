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
import org.constellation.dto.DataSet;
import org.constellation.dto.Identifiable;

/**
 * Lite view of a Constellation Dataset object.
 * Used to refer to an existing Dataset.
 * Used to define Constellation compatible process input/output.
 *
 * @author Quentin Boileau (Geomatys)
 */
public class DatasetProcessReference extends Identifiable implements Serializable {

    private String identifier;

    public DatasetProcessReference() {

    }
    
    public DatasetProcessReference(DataSet ds) {
        super(ds);
        if (ds != null) {
            this.identifier = ds.getIdentifier();
        }
    }

    public DatasetProcessReference(Integer id, String identifier) {
        super(id);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DatasetProcessReference that = (DatasetProcessReference) o;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + identifier.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("identifier:").append(identifier).append('\n');
        return sb.toString();
    }
}
