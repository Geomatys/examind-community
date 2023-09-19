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


import jakarta.validation.constraints.Size;

import java.io.Serializable;


/**
 * Generated DAO object for table admin.chain_process
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ChainProcess implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String auth;
    private String code;
    private String config;

    public ChainProcess() {}

    public ChainProcess(ChainProcess value) {
        this.id = value.id;
        this.auth = value.auth;
        this.code = value.code;
        this.config = value.config;
    }

    public ChainProcess(
        Integer id,
        String auth,
        String code,
        String config
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ChainProcess other = (ChainProcess) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.auth == null) {
            if (other.auth != null)
                return false;
        }
        else if (!this.auth.equals(other.auth))
            return false;
        if (this.code == null) {
            if (other.code != null)
                return false;
        }
        else if (!this.code.equals(other.code))
            return false;
        if (this.config == null) {
            if (other.config != null)
                return false;
        }
        else if (!this.config.equals(other.config))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.auth == null) ? 0 : this.auth.hashCode());
        result = prime * result + ((this.code == null) ? 0 : this.code.hashCode());
        result = prime * result + ((this.config == null) ? 0 : this.config.hashCode());
        return result;
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
