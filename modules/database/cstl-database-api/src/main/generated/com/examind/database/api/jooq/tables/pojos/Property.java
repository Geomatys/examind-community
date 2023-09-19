/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.database.api.jooq.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.property
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Property implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String value;

    public Property() {}

    public Property(Property value) {
        this.name = value.name;
        this.value = value.value;
    }

    public Property(
        String name,
        String value
    ) {
        this.name = name;
        this.value = value;
    }

    /**
     * Getter for <code>admin.property.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.property.name</code>.
     */
    public Property setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.property.value</code>.
     */
    @NotNull
    @Size(max = 1000)
    public String getValue() {
        return this.value;
    }

    /**
     * Setter for <code>admin.property.value</code>.
     */
    public Property setValue(String value) {
        this.value = value;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Property other = (Property) obj;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.value == null) {
            if (other.value != null)
                return false;
        }
        else if (!this.value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.value == null) ? 0 : this.value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Property (");

        sb.append(name);
        sb.append(", ").append(value);

        sb.append(")");
        return sb.toString();
    }
}
