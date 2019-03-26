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
package org.constellation.dto;

import java.io.Serializable;
import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlType(name = "LayerDTO")
@XmlRootElement
public class Layer implements Serializable {

    protected Integer id;
    private String name;
    private String namespace;
    private String alias;
    private Integer service;
    protected Integer dataId;
    private Date date;
    private String config;
    private Integer ownerId;
    private String title;

    public Layer() {
    }

    public Layer(
            Integer id,
            String name,
            String namespace,
            String alias,
            Integer service,
            Integer dataId,
            Date date,
            String config,
            Integer ownerId,
            String title
    ) {
        this.id = id;
        this.name = name;
        this.namespace = namespace;
        this.alias = alias;
        this.service = service;
        this.dataId = dataId;
        this.date = date;
        this.config = config;
        this.ownerId = ownerId;
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * @return the service
     */
    public Integer getService() {
        return service;
    }

    /**
     * @param service the service to set
     */
    public void setService(Integer service) {
        this.service = service;
    }

    /**
     * @return the config
     */
    public String getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(String config) {
        this.config = config;
    }

    /**
     * @return the ownerId
     */
    public Integer getOwnerId() {
        return ownerId;
    }

    /**
     * @param ownerId the ownerId to set
     */
    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Layer that = (Layer) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (dataId != null ? !dataId.equals(that.dataId) : that.dataId != null) return false;
        if (alias != null ? !alias.equals(that.alias) : that.alias != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (namespace != null ? !namespace.equals(that.namespace) : that.namespace != null) return false;
        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (dataId != null ? dataId.hashCode() : 0);
        result = 31 * result + (namespace != null ? namespace.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Layer{" +
                "id='" + id + '\'' +
                ", dataId='" + dataId + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", alias='" + alias + '\'' +
                ", title='" + title + '\'' +
                ", date=" + date +
                ", owner='" + ownerId + '\'' +
                '}';
    }

}
