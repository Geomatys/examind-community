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

import java.io.Serializable;


/**
 * Generated DAO object for table admin.metadata_bbox
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class MetadataBbox implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer metadataId;
    private Double east;
    private Double west;
    private Double north;
    private Double south;

    public MetadataBbox() {}

    public MetadataBbox(MetadataBbox value) {
        this.metadataId = value.metadataId;
        this.east = value.east;
        this.west = value.west;
        this.north = value.north;
        this.south = value.south;
    }

    public MetadataBbox(
        Integer metadataId,
        Double east,
        Double west,
        Double north,
        Double south
    ) {
        this.metadataId = metadataId;
        this.east = east;
        this.west = west;
        this.north = north;
        this.south = south;
    }

    /**
     * Getter for <code>admin.metadata_bbox.metadata_id</code>.
     */
    @NotNull
    public Integer getMetadataId() {
        return this.metadataId;
    }

    /**
     * Setter for <code>admin.metadata_bbox.metadata_id</code>.
     */
    public MetadataBbox setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.east</code>.
     */
    @NotNull
    public Double getEast() {
        return this.east;
    }

    /**
     * Setter for <code>admin.metadata_bbox.east</code>.
     */
    public MetadataBbox setEast(Double east) {
        this.east = east;
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.west</code>.
     */
    @NotNull
    public Double getWest() {
        return this.west;
    }

    /**
     * Setter for <code>admin.metadata_bbox.west</code>.
     */
    public MetadataBbox setWest(Double west) {
        this.west = west;
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.north</code>.
     */
    @NotNull
    public Double getNorth() {
        return this.north;
    }

    /**
     * Setter for <code>admin.metadata_bbox.north</code>.
     */
    public MetadataBbox setNorth(Double north) {
        this.north = north;
        return this;
    }

    /**
     * Getter for <code>admin.metadata_bbox.south</code>.
     */
    @NotNull
    public Double getSouth() {
        return this.south;
    }

    /**
     * Setter for <code>admin.metadata_bbox.south</code>.
     */
    public MetadataBbox setSouth(Double south) {
        this.south = south;
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
        final MetadataBbox other = (MetadataBbox) obj;
        if (this.metadataId == null) {
            if (other.metadataId != null)
                return false;
        }
        else if (!this.metadataId.equals(other.metadataId))
            return false;
        if (this.east == null) {
            if (other.east != null)
                return false;
        }
        else if (!this.east.equals(other.east))
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
        if (this.south == null) {
            if (other.south != null)
                return false;
        }
        else if (!this.south.equals(other.south))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.metadataId == null) ? 0 : this.metadataId.hashCode());
        result = prime * result + ((this.east == null) ? 0 : this.east.hashCode());
        result = prime * result + ((this.west == null) ? 0 : this.west.hashCode());
        result = prime * result + ((this.north == null) ? 0 : this.north.hashCode());
        result = prime * result + ((this.south == null) ? 0 : this.south.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MetadataBbox (");

        sb.append(metadataId);
        sb.append(", ").append(east);
        sb.append(", ").append(west);
        sb.append(", ").append(north);
        sb.append(", ").append(south);

        sb.append(")");
        return sb.toString();
    }
}
