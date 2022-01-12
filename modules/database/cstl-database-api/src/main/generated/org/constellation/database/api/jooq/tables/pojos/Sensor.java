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
 * Generated DAO object for table admin.sensor
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Sensor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  identifier;
    private String  type;
    private String  parent;
    private Integer owner;
    private Long    date;
    private Integer providerId;
    private String  profile;
    private String  omType;
    private String  name;
    private String  description;

    public Sensor() {}

    public Sensor(Sensor value) {
        this.id = value.id;
        this.identifier = value.identifier;
        this.type = value.type;
        this.parent = value.parent;
        this.owner = value.owner;
        this.date = value.date;
        this.providerId = value.providerId;
        this.profile = value.profile;
        this.omType = value.omType;
        this.name = value.name;
        this.description = value.description;
    }

    public Sensor(
        Integer id,
        String  identifier,
        String  type,
        String  parent,
        Integer owner,
        Long    date,
        Integer providerId,
        String  profile,
        String  omType,
        String  name,
        String  description
    ) {
        this.id = id;
        this.identifier = identifier;
        this.type = type;
        this.parent = parent;
        this.owner = owner;
        this.date = date;
        this.providerId = providerId;
        this.profile = profile;
        this.omType = omType;
        this.name = name;
        this.description = description;
    }

    /**
     * Getter for <code>admin.sensor.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.sensor.id</code>.
     */
    public Sensor setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.identifier</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Setter for <code>admin.sensor.identifier</code>.
     */
    public Sensor setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.type</code>.
     */
    @NotNull
    @Size(max = 64)
    public String getType() {
        return this.type;
    }

    /**
     * Setter for <code>admin.sensor.type</code>.
     */
    public Sensor setType(String type) {
        this.type = type;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.parent</code>.
     */
    @Size(max = 512)
    public String getParent() {
        return this.parent;
    }

    /**
     * Setter for <code>admin.sensor.parent</code>.
     */
    public Sensor setParent(String parent) {
        this.parent = parent;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.sensor.owner</code>.
     */
    public Sensor setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.date</code>.
     */
    public Long getDate() {
        return this.date;
    }

    /**
     * Setter for <code>admin.sensor.date</code>.
     */
    public Sensor setDate(Long date) {
        this.date = date;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.provider_id</code>.
     */
    public Integer getProviderId() {
        return this.providerId;
    }

    /**
     * Setter for <code>admin.sensor.provider_id</code>.
     */
    public Sensor setProviderId(Integer providerId) {
        this.providerId = providerId;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.profile</code>.
     */
    @Size(max = 255)
    public String getProfile() {
        return this.profile;
    }

    /**
     * Setter for <code>admin.sensor.profile</code>.
     */
    public Sensor setProfile(String profile) {
        this.profile = profile;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.om_type</code>.
     */
    @Size(max = 100)
    public String getOmType() {
        return this.omType;
    }

    /**
     * Setter for <code>admin.sensor.om_type</code>.
     */
    public Sensor setOmType(String omType) {
        this.omType = omType;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.name</code>.
     */
    @Size(max = 1000)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.sensor.name</code>.
     */
    public Sensor setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.sensor.description</code>.
     */
    @Size(max = 5000)
    public String getDescription() {
        return this.description;
    }

    /**
     * Setter for <code>admin.sensor.description</code>.
     */
    public Sensor setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Sensor (");

        sb.append(id);
        sb.append(", ").append(identifier);
        sb.append(", ").append(type);
        sb.append(", ").append(parent);
        sb.append(", ").append(owner);
        sb.append(", ").append(date);
        sb.append(", ").append(providerId);
        sb.append(", ").append(profile);
        sb.append(", ").append(omType);
        sb.append(", ").append(name);
        sb.append(", ").append(description);

        sb.append(")");
        return sb.toString();
    }
}
