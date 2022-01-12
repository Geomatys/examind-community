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
 * Generated DAO object for table admin.role
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    public Role() {}

    public Role(Role value) {
        this.name = value.name;
    }

    public Role(
        String name
    ) {
        this.name = name;
    }

    /**
     * Getter for <code>admin.role.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.role.name</code>.
     */
    public Role setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Role (");

        sb.append(name);

        sb.append(")");
        return sb.toString();
    }
}
