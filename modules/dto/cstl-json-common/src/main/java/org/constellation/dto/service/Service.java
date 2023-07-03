/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.dto.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.constellation.dto.ServiceReference;

@XmlRootElement
public class Service extends ServiceReference implements Serializable {

    private Date date;
    private String config;
    private Integer owner;
    private String status;
    private String versions;
    private String impl;

    public Service() {
    }

    public Service(Integer id, String identifier, String type, Date date,
            String config, Integer owner, String status, String versions, String impl) {
        super(id, identifier, type);
        this.date = date;
        this.config = config;
        this.owner = owner;
        this.status = status;
        this.versions = versions;
        this.impl = impl;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public Integer getOwner() {
        return owner;
    }

    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersions() {
        return versions;
    }

    public void setVersions(String versions) {
        this.versions = versions;
    }

    /**
     * @return the impl
     */
    public String getImpl() {
        if (impl == null) {
            impl = "default";
        }
        return impl;
    }

    /**
     * @param impl the impl to set
     */
    public void setImpl(String impl) {
        this.impl = impl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.config != null) {
            sb.append("config: ").append(config).append('\n');
        }
        if (this.date != null) {
            sb.append("date: ").append(date).append('\n');
        }
        if (this.status != null) {
            sb.append("status: ").append(status).append('\n');
        }
        if (this.owner != null) {
            sb.append("owner: ").append(owner).append('\n');
        }
        if (this.versions != null) {
            sb.append("versions: ").append(versions).append('\n');
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass() && super.equals(obj)) {
            Service that = (Service) obj;
            return     Objects.equals(this.config, that.config)
                    && Objects.equals(this.status, that.status)
                    && Objects.equals(this.owner, that.owner)
                    && Objects.equals(this.versions, that.versions)
                    && Objects.equals(this.date, that.date);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + super.hashCode();
        hash = 67 * hash + Objects.hashCode(this.date);
        hash = 67 * hash + Objects.hashCode(this.config);
        hash = 67 * hash + Objects.hashCode(this.owner);
        hash = 67 * hash + Objects.hashCode(this.status);
        hash = 67 * hash + Objects.hashCode(this.versions);
        return hash;
    }

}
