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
 * Generated DAO object for table admin.mapcontext
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class Mapcontext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String name;
    private Integer owner;
    private String description;
    private String crs;
    private Double west;
    private Double north;
    private Double east;
    private Double south;
    private String keywords;

    public Mapcontext() {}

    public Mapcontext(Mapcontext value) {
        this.id = value.id;
        this.name = value.name;
        this.owner = value.owner;
        this.description = value.description;
        this.crs = value.crs;
        this.west = value.west;
        this.north = value.north;
        this.east = value.east;
        this.south = value.south;
        this.keywords = value.keywords;
    }

    public Mapcontext(
        Integer id,
        String name,
        Integer owner,
        String description,
        String crs,
        Double west,
        Double north,
        Double east,
        Double south,
        String keywords
    ) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.description = description;
        this.crs = crs;
        this.west = west;
        this.north = north;
        this.east = east;
        this.south = south;
        this.keywords = keywords;
    }

    /**
     * Getter for <code>admin.mapcontext.id</code>.
     */
    public Integer getId() {
        return this.id;
    }

    /**
     * Setter for <code>admin.mapcontext.id</code>.
     */
    public Mapcontext setId(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.name</code>.
     */
    @NotNull
    @Size(max = 512)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>admin.mapcontext.name</code>.
     */
    public Mapcontext setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.owner</code>.
     */
    public Integer getOwner() {
        return this.owner;
    }

    /**
     * Setter for <code>admin.mapcontext.owner</code>.
     */
    public Mapcontext setOwner(Integer owner) {
        this.owner = owner;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.description</code>.
     */
    @Size(max = 512)
    public String getDescription() {
        return this.description;
    }

    /**
     * Setter for <code>admin.mapcontext.description</code>.
     */
    public Mapcontext setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.crs</code>.
     */
    @Size(max = 32)
    public String getCrs() {
        return this.crs;
    }

    /**
     * Setter for <code>admin.mapcontext.crs</code>.
     */
    public Mapcontext setCrs(String crs) {
        this.crs = crs;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.west</code>.
     */
    public Double getWest() {
        return this.west;
    }

    /**
     * Setter for <code>admin.mapcontext.west</code>.
     */
    public Mapcontext setWest(Double west) {
        this.west = west;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.north</code>.
     */
    public Double getNorth() {
        return this.north;
    }

    /**
     * Setter for <code>admin.mapcontext.north</code>.
     */
    public Mapcontext setNorth(Double north) {
        this.north = north;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.east</code>.
     */
    public Double getEast() {
        return this.east;
    }

    /**
     * Setter for <code>admin.mapcontext.east</code>.
     */
    public Mapcontext setEast(Double east) {
        this.east = east;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.south</code>.
     */
    public Double getSouth() {
        return this.south;
    }

    /**
     * Setter for <code>admin.mapcontext.south</code>.
     */
    public Mapcontext setSouth(Double south) {
        this.south = south;
        return this;
    }

    /**
     * Getter for <code>admin.mapcontext.keywords</code>.
     */
    @Size(max = 256)
    public String getKeywords() {
        return this.keywords;
    }

    /**
     * Setter for <code>admin.mapcontext.keywords</code>.
     */
    public Mapcontext setKeywords(String keywords) {
        this.keywords = keywords;
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
        final Mapcontext other = (Mapcontext) obj;
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
        if (this.owner == null) {
            if (other.owner != null)
                return false;
        }
        else if (!this.owner.equals(other.owner))
            return false;
        if (this.description == null) {
            if (other.description != null)
                return false;
        }
        else if (!this.description.equals(other.description))
            return false;
        if (this.crs == null) {
            if (other.crs != null)
                return false;
        }
        else if (!this.crs.equals(other.crs))
            return false;
        if (this.west == null) {
            if (other.west != null)
                return false;
        }
        else if (!this.west.equals(other.west))
            return false;
        if (this.north == null) {
            if (other.north != null)
                return false;
        }
        else if (!this.north.equals(other.north))
            return false;
        if (this.east == null) {
            if (other.east != null)
                return false;
        }
        else if (!this.east.equals(other.east))
            return false;
        if (this.south == null) {
            if (other.south != null)
                return false;
        }
        else if (!this.south.equals(other.south))
            return false;
        if (this.keywords == null) {
            if (other.keywords != null)
                return false;
        }
        else if (!this.keywords.equals(other.keywords))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.owner == null) ? 0 : this.owner.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.crs == null) ? 0 : this.crs.hashCode());
        result = prime * result + ((this.west == null) ? 0 : this.west.hashCode());
        result = prime * result + ((this.north == null) ? 0 : this.north.hashCode());
        result = prime * result + ((this.east == null) ? 0 : this.east.hashCode());
        result = prime * result + ((this.south == null) ? 0 : this.south.hashCode());
        result = prime * result + ((this.keywords == null) ? 0 : this.keywords.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Mapcontext (");

        sb.append(id);
        sb.append(", ").append(name);
        sb.append(", ").append(owner);
        sb.append(", ").append(description);
        sb.append(", ").append(crs);
        sb.append(", ").append(west);
        sb.append(", ").append(north);
        sb.append(", ").append(east);
        sb.append(", ").append(south);
        sb.append(", ").append(keywords);

        sb.append(")");
        return sb.toString();
    }
}
