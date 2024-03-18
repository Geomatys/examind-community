/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.Date;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Style extends Identifiable implements Serializable {

    private String name;
    private Integer providerId;
    private String type;
    private Date date;
    private String body;
    private Integer ownerId;
    private Boolean isShared;
    private String specification;

    public Style() {
    }

    public Style(
            Integer id,
            String name,
            Integer providerId,
            String type,
            Date date,
            String body,
            Integer ownerId,
            Boolean isShared,
            String specification
    ) {
        this.id = id;
        this.name = name;
        this.providerId = providerId;
        this.type = type;
        this.date = date;
        this.body = body;
        this.ownerId = ownerId;
        this.isShared = isShared;
        this.specification = specification;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(final Integer providerId) {
        this.providerId = providerId;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final Integer ownerId) {
        this.ownerId = ownerId;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Style that = (Style) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        if (providerId != null ? !providerId.equals(that.providerId) : that.providerId != null) return false;
        if (body != null ? !body.equals(that.body) : that.body != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (specification != null ? !specification.equals(that.specification) : that.specification != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (body != null ? body.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (specification != null ? specification.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", providerId='" + providerId + '\'' +
                ", body='" + body + '\'' +
                ", date=" + date +
                ", type='" + type + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", specification='" + specification + '\'' +
                '}';
    }
}
