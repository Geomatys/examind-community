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

import javax.validation.constraints.Size;


/**
 * Generated DAO object for table admin.chain_process
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ChainProcess implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  auth;
    private String  code;
    private String  config;

    public ChainProcess() {}

    public ChainProcess(ChainProcess value) {
        this.id = value.id;
        this.auth = value.auth;
        this.code = value.code;
        this.config = value.config;
    }

    public ChainProcess(
        Integer id,
        String  auth,
        String  code,
        String  config
    ) {
        this.id = id;
        this.auth = auth;
        this.code = code;
        this.config = config;
    }

    /**
     * Getter for <code>admin.chain_process.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.chain_process.id</code>.
     */
    public ChainProcess setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.auth</code>.
     */
    @Size(max = 512)
    public String getAuth() {
        return this.auth;
    }

    /**
     * Setter for <code>admin.chain_process.auth</code>.
     */
    public ChainProcess setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.code</code>.
     */
    @Size(max = 512)
    public String getCode() {
        return this.code;
    }

    /**
     * Setter for <code>admin.chain_process.code</code>.
     */
    public ChainProcess setCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * Getter for <code>admin.chain_process.config</code>.
     */
    public String getConfig() {
        return this.config;
    }

    /**
     * Setter for <code>admin.chain_process.config</code>.
     */
    public ChainProcess setConfig(String config) {
        this.config = config;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ChainProcess (");

        sb.append(id);
        sb.append(", ").append(auth);
        sb.append(", ").append(code);
        sb.append(", ").append(config);

        sb.append(")");
        return sb.toString();
    }
}
