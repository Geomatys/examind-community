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
import javax.xml.bind.annotation.XmlRootElement;
import org.constellation.dto.Data;

@XmlRootElement
public class Service implements Serializable {

    private int id;
    private String identifier;
    private String type;
    private Date date;
    private String config;
    private Integer owner;
    private String status;
    private String versions;

    public Service() {
    }

    public Service(Integer id, String identifier, String type, Date date,
            String config, Integer owner, String status, String versions) {
        this.id = id;
        this.identifier = identifier;
        this.type = type;
        this.date = date;
        this.config = config;
        this.owner = owner;
        this.status = status;
        this.versions = versions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "ServiceDTO [id=" + id + ", identifier=" + identifier
                + ", type=" + type + ", date=" + date
                + ", config=" + config
                + ", owner=" + owner + ", status=" + status + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Service) {
            Service that = (Service) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.config, that.config)
                    && Objects.equals(this.identifier, that.identifier)
                    && Objects.equals(this.status, that.status)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.owner, that.owner)
                    && Objects.equals(this.versions, that.versions)
                    && Objects.equals(this.date, that.date);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + this.id;
        hash = 67 * hash + Objects.hashCode(this.identifier);
        hash = 67 * hash + Objects.hashCode(this.type);
        hash = 67 * hash + Objects.hashCode(this.date);
        hash = 67 * hash + Objects.hashCode(this.config);
        hash = 67 * hash + Objects.hashCode(this.owner);
        hash = 67 * hash + Objects.hashCode(this.status);
        hash = 67 * hash + Objects.hashCode(this.versions);
        return hash;
    }

}
