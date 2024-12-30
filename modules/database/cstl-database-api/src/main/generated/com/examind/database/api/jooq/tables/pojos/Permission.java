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
 * Generated DAO object for table admin.permission
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private String description;

    public Permission() {}

    public Permission(Permission value) {
        this.id = value.id;
        this.name = value.name;
        this.description = value.description;
    }

    public Permission(
        Integer id,
        String name,
        String description
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * Getter for <code>admin.permission.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.permission.id</code>.
     */
    public Permission setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.permission.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.permission.name</code>.
     */
    public Permission setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.permission.description</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getDescription() {
        return this.description;
    }

    /**
     * Setter for <code>admin.permission.description</code>.
     */
    public Permission setDescription(String description) {
        this.description = description;
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
        final Permission other = (Permission) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.description == null) {
            if (other.description != null)
                return false;
        }
        else if (!this.description.equals(other.description))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Permission (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(description);

        sb.append(")");
        return sb.toString();
    }
}
