/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 * 
 *  Copyright 2022 Geomatys.
 * 
 *  Licensed under the Apache License, Version 2.0 (    the "License");
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


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


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
    @Size(max = 64)
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
    public String toString() {
        StringBuilder sb = new StringBuilder("Property (");

        sb.append(name);
        sb.append(", ").append(value);

        sb.append(")");
        return sb.toString();
    }
}
