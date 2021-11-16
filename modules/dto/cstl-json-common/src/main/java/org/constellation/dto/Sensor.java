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

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class Sensor extends SensorReference implements Serializable {

    private String  name;
    private String  description;
    private String  type;
    private String  parent;
    private Integer owner;
    private Date    date;
    private Integer providerId;
    private String profile;
    protected String omType;

    public Sensor() {

    }
    
    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(String parent) {
        this.parent = parent;
    }

    /**
     * @return the owner
     */
    public Integer getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @param date the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return the providerId
     */
    public Integer getProviderId() {
        return providerId;
    }

    /**
     * @param providerId the providerId to set
     */
    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    /**
     * @return the profile
     */
    public String getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * @return the omType
     */
    public String getOmType() {
        return omType;
    }

    /**
     * @param omType the omType to set
     */
    public void setOmType(String omType) {
        this.omType = omType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(super.toString());
        sb.append("name").append(name).append('\n');
        sb.append("description").append(description).append('\n');
        sb.append("type").append(type).append('\n');
        sb.append("parent").append(parent).append('\n');
        sb.append("owner").append(owner).append('\n');
        sb.append("date").append(date).append('\n');
        sb.append("providerId").append(providerId).append('\n');
        sb.append("profile").append(profile).append('\n');
        sb.append("omType").append(omType).append('\n');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + super.hashCode();
        hash = 97 * hash + Objects.hashCode(this.name);
        hash = 97 * hash + Objects.hashCode(this.description);
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.parent);
        hash = 97 * hash + Objects.hashCode(this.owner);
        hash = 97 * hash + Objects.hashCode(this.date);
        hash = 97 * hash + Objects.hashCode(this.providerId);
        hash = 97 * hash + Objects.hashCode(this.profile);
        hash = 97 * hash + Objects.hashCode(this.omType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass() && super.equals(obj)) {
            final Sensor that = (Sensor) obj;
            return Objects.equals(this.date, that.date) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.description, that.description) &&
                   Objects.equals(this.type, that.type) &&
                   Objects.equals(this.parent, that.parent) &&
                   Objects.equals(this.owner, that.owner) &&
                   Objects.equals(this.profile, that.profile) &&
                   Objects.equals(this.omType, that.omType) &&
                   Objects.equals(this.providerId, that.providerId);
        }
        return false;
    }
}
