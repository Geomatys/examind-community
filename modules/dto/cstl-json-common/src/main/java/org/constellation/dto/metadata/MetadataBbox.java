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
package org.constellation.dto.metadata;

/**
 *
 * @author Guilhem Legal
 */
public class MetadataBbox {

    private Integer metadataId;
    private Double east;
    private Double west;
    private Double north;
    private Double south;

    public MetadataBbox() {
    }

    public MetadataBbox(Integer metadataId, Double east, Double west, Double north,
            Double south) {
        this.metadataId = metadataId;
        this.east = east;
        this.west = west;
        this.north = north;
        this.south = south;
    }

    /**
     * @return the metadataId
     */
    public Integer getMetadataId() {
        return metadataId;
    }

    /**
     * @param metadataId the metadataId to set
     */
    public void setMetadataId(Integer metadataId) {
        this.metadataId = metadataId;
    }

    /**
     * @return the east
     */
    public Double getEast() {
        return east;
    }

    /**
     * @param east the east to set
     */
    public void setEast(Double east) {
        this.east = east;
    }

    /**
     * @return the west
     */
    public Double getWest() {
        return west;
    }

    /**
     * @param west the west to set
     */
    public void setWest(Double west) {
        this.west = west;
    }

    /**
     * @return the north
     */
    public Double getNorth() {
        return north;
    }

    /**
     * @param north the north to set
     */
    public void setNorth(Double north) {
        this.north = north;
    }

    /**
     * @return the south
     */
    public Double getSouth() {
        return south;
    }

    /**
     * @param south the south to set
     */
    public void setSouth(Double south) {
        this.south = south;
    }
}
