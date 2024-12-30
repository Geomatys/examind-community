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
 * Generated DAO object for table admin.service_extra_config
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class ServiceExtraConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String filename;
    private String content;

    public ServiceExtraConfig() {}

    public ServiceExtraConfig(ServiceExtraConfig value) {
        this.id = value.id;
        this.filename = value.filename;
        this.content = value.content;
    }

    public ServiceExtraConfig(
        Integer id,
        String filename,
        String content
    ) {
        this.id = id;
        this.filename = filename;
        this.content = content;
    }

    /**
     * Getter for <code>admin.service_extra_config.id</code>.
     */
    @NotNull
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.service_extra_config.id</code>.
     */
    public ServiceExtraConfig setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.service_extra_config.filename</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getFilename() {
        return this.filename;
    }

    /**
     * Setter for <code>admin.service_extra_config.filename</code>.
     */
    public ServiceExtraConfig setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    /**
     * Getter for <code>admin.service_extra_config.content</code>.
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Setter for <code>admin.service_extra_config.content</code>.
     */
    public ServiceExtraConfig setContent(String content) {
        this.content = content;
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
        final ServiceExtraConfig other = (ServiceExtraConfig) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.filename == null) {
            if (other.filename != null)
                return false;
        }
        else if (!this.filename.equals(other.filename))
            return false;
        if (this.content == null) {
            if (other.content != null)
                return false;
        }
        else if (!this.content.equals(other.content))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.filename == null) ? 0 : this.filename.hashCode());
        result = prime * result + ((this.content == null) ? 0 : this.content.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ServiceExtraConfig (");

        sb.append(id);
        sb.append(", ").append(filename);
        sb.append(", ").append(content);

        sb.append(")");
        return sb.toString();
    }
}
