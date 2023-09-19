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
 * Generated DAO object for table admin.crs
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Crs implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataid;
    private String crscode;

    public Crs() {}

    public Crs(Crs value) {
        this.dataid = value.dataid;
        this.crscode = value.crscode;
    }

    public Crs(
        Integer dataid,
        String crscode
    ) {
        this.dataid = dataid;
        this.crscode = crscode;
    }

    /**
     * Getter for <code>admin.crs.dataid</code>.
     */
    @NotNull
    public Integer getDataid() {
        return this.dataid;
    }

    /**
     * Setter for <code>admin.crs.dataid</code>.
     */
    public Crs setDataid(Integer dataid) {
        this.dataid = dataid;
        return this;
    }

    /**
     * Getter for <code>admin.crs.crscode</code>.
     */
    @NotNull
    @Size(max = 64)
    public String getCrscode() {
        return this.crscode;
    }

    /**
     * Setter for <code>admin.crs.crscode</code>.
     */
    public Crs setCrscode(String crscode) {
        this.crscode = crscode;
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
        final Crs other = (Crs) obj;
        if (this.dataid == null) {
            if (other.dataid != null)
                return false;
        }
        else if (!this.dataid.equals(other.dataid))
            return false;
        if (this.crscode == null) {
            if (other.crscode != null)
                return false;
        }
        else if (!this.crscode.equals(other.crscode))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.dataid == null) ? 0 : this.dataid.hashCode());
        result = prime * result + ((this.crscode == null) ? 0 : this.crscode.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Crs (");

        sb.append(dataid);
        sb.append(", ").append(crscode);

        sb.append(")");
        return sb.toString();
    }
}
