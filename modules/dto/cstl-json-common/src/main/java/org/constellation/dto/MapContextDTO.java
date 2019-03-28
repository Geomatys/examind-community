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
		String  keywords) {
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
}
