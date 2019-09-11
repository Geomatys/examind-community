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
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author guilhem
 */
@XmlRootElement
public class MapContextDTO implements Serializable{

    private Integer id;
    private String  name;
    private Integer owner;
    private String  description;
    private String  crs;
    private Double  west;
    private Double  north;
    private Double  east;
    private Double  south;
    private String  keywords;
    private String userOwner;

    public MapContextDTO() {
    }

    public MapContextDTO(Integer id,
		String  name,
		Integer owner,
		String  description,
		String  crs,
		Double  west,
		Double  north,
		Double  east,
		Double  south,
		String  keywords,
                String userOwner) {
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
        this.userOwner = userOwner;
    }

    public String getUserOwner() {
        return userOwner;
    }

    public void setUserOwner(String userOwner) {
        this.userOwner = userOwner;
    }

    /**
     * @return the id
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id) {
        this.id = id;
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

    /**
     * @return the crs
     */
    public String getCrs() {
        return crs;
    }

    /**
     * @param crs the crs to set
     */
    public void setCrs(String crs) {
        this.crs = crs;
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

    /**
     * @return the keywords
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public boolean hasEnvelope() {
        return west != null &&  north != null && east != null && south != null && crs != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[").append(this.getClass().getSimpleName()).append("]\n");
        sb.append("id=").append(this.id).append('\n');
        sb.append("name=").append(this.name).append('\n');
        sb.append("owner=").append(this.owner).append('\n');
        sb.append("description=").append(this.description).append('\n');
        sb.append("crs=").append(this.crs).append('\n');
        sb.append("west=").append(this.west).append('\n');
        sb.append("north=").append(this.north).append('\n');
        sb.append("east=").append(this.east).append('\n');
        sb.append("south=").append(this.south).append('\n');
        sb.append("keywords=").append(this.keywords).append('\n');
        sb.append("userOwner=").append(this.userOwner).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MapContextDTO) {
            MapContextDTO that = (MapContextDTO) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.crs, that.crs)
                    && Objects.equals(this.description, that.description)
                    && Objects.equals(this.owner, that.owner)
                    && Objects.equals(this.east, that.east)
                    && Objects.equals(this.id, that.id)
                    && Objects.equals(this.keywords, that.keywords)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.north, that.north)
                    && Objects.equals(this.south, that.south)
                    && Objects.equals(this.userOwner, that.userOwner)
                    && Objects.equals(this.west, that.west);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + Objects.hashCode(this.id);
        hash = 11 * hash + Objects.hashCode(this.name);
        hash = 11 * hash + Objects.hashCode(this.owner);
        hash = 11 * hash + Objects.hashCode(this.description);
        hash = 11 * hash + Objects.hashCode(this.crs);
        hash = 11 * hash + Objects.hashCode(this.west);
        hash = 11 * hash + Objects.hashCode(this.north);
        hash = 11 * hash + Objects.hashCode(this.east);
        hash = 11 * hash + Objects.hashCode(this.south);
        hash = 11 * hash + Objects.hashCode(this.keywords);
        hash = 11 * hash + Objects.hashCode(this.userOwner);
        return hash;
    }
}
