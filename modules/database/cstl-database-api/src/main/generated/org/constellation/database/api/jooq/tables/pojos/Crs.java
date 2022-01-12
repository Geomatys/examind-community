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
package org.constellation.database.api.jooq.tables.pojos;


import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.crs
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Crs implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer dataid;
    private String  crscode;

    public Crs() {}

    public Crs(Crs value) {
        this.dataid = value.dataid;
        this.crscode = value.crscode;
    }

    public Crs(
        Integer dataid,
        String  crscode
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
    public String toString() {
        StringBuilder sb = new StringBuilder("Crs (");

        sb.append(dataid);
        sb.append(", ").append(crscode);

        sb.append(")");
        return sb.toString();
    }
}
