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
 * Generated DAO object for table admin.style
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Style implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private Integer provider;
    private String type;
    private Long date;
    private String body;
    private Integer owner;
    private Boolean isShared;
    private String specification;

    public Style() {}

    public Style(Style value) {
        this.id = value.id;
        this.name = value.name;
        this.provider = value.provider;
        this.type = value.type;
        this.date = value.date;
        this.body = value.body;
        this.owner = value.owner;
        this.isShared = value.isShared;
        this.specification = value.specification;
    }

    public Style(
        Integer id,
        String name,
        Integer provider,
        String type,
        Long date,
        String body,
        Integer owner,
        Boolean isShared,
        String specification
    ) {
        this.id = id;
        this.name = name;
        this.provider = provider;
        this.type = type;
        this.date = date;
        this.body = body;
        this.owner = owner;
        this.isShared = isShared;
        this.specification = specification;
    }

    /**
     * Getter for <code>admin.style.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.style.id</code>.
     */
    public Style setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.style.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.style.name</code>.
     */
    public Style setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.style.provider</code>.
     */
    @NotNull
    public Integer getProvider() {
        return this.provider;
    }

    /**
     * Setter for <code>admin.style.provider</code>.
     */
    public Style setProvider(Integer provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Getter for <code>admin.style.type</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.style.type</code>.
     */
    public Style setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.style.date</code>.
     */
    @NotNull
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.style.date</code>.
     */
    public Style setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.style.body</code>.
     */
    @NotNull
    public String getBody() {
        return this.body;
    }

    /**
     * Setter for <code>admin.style.body</code>.
     */
    public Style setBody(String body) {
        this.body = body;
        return this;
    }

    /**
     * Getter for <code>admin.style.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.style.owner</code>.
     */
    public Style setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.style.is_shared</code>.
     */
    public Boolean getIsShared() {
        return this.isShared;
    }

    /**
     * Setter for <code>admin.style.is_shared</code>.
     */
    public Style setIsShared(Boolean isShared) {
        this.isShared = isShared;
        return this;
    }

    /**
     * Getter for <code>admin.style.specification</code>.
     */
    @Size(max = 100)
    public String getSpecification() {
        return this.specification;
    }

    /**
     * Setter for <code>admin.style.specification</code>.
     */
    public Style setSpecification(String specification) {
        this.specification = specification;
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
        final Style other = (Style) obj;
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
        if (this.provider == null) {
            if (other.provider != null)
                return false;
        }
        else if (!this.provider.equals(other.provider))
            return false;
        if (this.type == null) {
            if (other.type != null)
                return false;
        }
        else if (!this.type.equals(other.type))
            return false;
        if (this.date == null) {
            if (other.date != null)
                return false;
        }
        else if (!this.date.equals(other.date))
            return false;
        if (this.body == null) {
            if (other.body != null)
                return false;
        }
        else if (!this.body.equals(other.body))
            return false;
        if (this.owner == null) {
            if (other.owner != null)
                return false;
        }
        else if (!this.owner.equals(other.owner))
            return false;
        if (this.isShared == null) {
            if (other.isShared != null)
                return false;
        }
        else if (!this.isShared.equals(other.isShared))
            return false;
        if (this.specification == null) {
            if (other.specification != null)
                return false;
        }
        else if (!this.specification.equals(other.specification))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.provider == null) ? 0 : this.provider.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        result = prime * result + ((this.date == null) ? 0 : this.date.hashCode());
        result = prime * result + ((this.body == null) ? 0 : this.body.hashCode());
        result = prime * result + ((this.owner == null) ? 0 : this.owner.hashCode());
        result = prime * result + ((this.isShared == null) ? 0 : this.isShared.hashCode());
        result = prime * result + ((this.specification == null) ? 0 : this.specification.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Style (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(provider);
        sb.append(", ").append(type);
        sb.append(", ").append(date);
        sb.append(", ").append(body);
        sb.append(", ").append(owner);
        sb.append(", ").append(isShared);
        sb.append(", ").append(specification);

        sb.append(")");
        return sb.toString();
    }
}
