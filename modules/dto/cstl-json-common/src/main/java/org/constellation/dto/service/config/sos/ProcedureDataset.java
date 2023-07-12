/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.dto.service.config.sos;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.locationtech.jts.geom.Geometry;

/**
 * This pojo is here to replace the class {@linkplain org.geotoolkit.observation.model.ProcedureDataset} to avoid adding the dependency to the geotk module.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class ProcedureDataset {

    private String id;

    private String name;

    private String description;

    private String type;

    private String omType;

    private List<ProcedureDataset> children = new ArrayList<>();

    private List<String> fields = new ArrayList<>();

    private Date dateStart;
    private Date dateEnd;

    private Double minx;
    private Double maxx;

    private Double miny;
    private Double maxy;

    private Geometry geom;

    private Map<Date, Geometry> historicalLocations = new HashMap<>();

    private Map<String, Object> properties = new HashMap<>();

    public ProcedureDataset() {

    }

    public ProcedureDataset(String id, String name, String description, String type, String omType, Date dateStart, Date dateEnd, 
            Double minx, Double maxx, Double miny, Double maxy, List<String> fields, Geometry geom, Map<String, Object> properties) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.omType = omType;
        this.fields = fields;

        this.dateStart = dateStart;
        this.dateEnd = dateEnd;

        this.minx = minx;
        this.maxx = maxx;
        this.miny = miny;
        this.maxy = maxy;
        this.geom = geom;
        this.properties = properties;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
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
     * @return the children
     */
    public List<ProcedureDataset> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<ProcedureDataset> children) {
        this.children = children;
    }

    /**
     * @return the fields
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * @param fields the fields to set
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    /**
     * @return the dateStart
     */
    public Date getDateStart() {
        return dateStart;
    }

    /**
     * @param dateStart the dateStart to set
     */
    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * @return the dateEnd
     */
    public Date getDateEnd() {
        return dateEnd;
    }

    /**
     * @param dateEnd the dateEnd to set
     */
    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * @return the minx
     */
    public Double getMinx() {
        return minx;
    }

    /**
     * @param minx the minx to set
     */
    public void setMinx(Double minx) {
        this.minx = minx;
    }

    /**
     * @return the maxx
     */
    public Double getMaxx() {
        return maxx;
    }

    /**
     * @param maxx the maxx to set
     */
    public void setMaxx(Double maxx) {
        this.maxx = maxx;
    }

    /**
     * @return the miny
     */
    public Double getMiny() {
        return miny;
    }

    /**
     * @param miny the miny to set
     */
    public void setMiny(Double miny) {
        this.miny = miny;
    }

    /**
     * @return the maxy
     */
    public Double getMaxy() {
        return maxy;
    }

    /**
     * @param maxy the maxy to set
     */
    public void setMaxy(Double maxy) {
        this.maxy = maxy;
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
     * @return the geom
     */
    public Geometry getGeom() {
        return geom;
    }

    /**
     * @param geom the geom to set
     */
    public void setGeom(Geometry geom) {
        this.geom = geom;
    }

    /**
     * @return the historicalLocations
     */
    public Map<Date, Geometry> getHistoricalLocations() {
        return historicalLocations;
    }

    /**
     * @param historicalLocations the historicalLocations to set
     */
    public void setHistoricalLocations(Map<Date, Geometry> historicalLocations) {
        this.historicalLocations = historicalLocations;
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

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[ProcedureTree]\n");
        sb.append("id=").append(id).append('\n');
        if (name != null) {
            sb.append("name=").append(name).append("\n");
        }
        if (description != null) {
            sb.append("description=").append(description).append("\n");
        }
        if (type != null) {
            sb.append("type=").append(type).append("\n");
        }
        if (omType != null) {
            sb.append("OM type=").append(omType).append("\n");
        }
        if (dateStart != null) {
            sb.append("dateStart=").append(dateStart).append("\n");
        }
        if (dateEnd != null) {
            sb.append("dateEnd=").append(dateEnd).append("\n");
        }
        if (minx != null) {
            sb.append("minx=").append(minx).append("\n");
        }
        if (maxx != null) {
            sb.append("maxx=").append(maxx).append("\n");
        }
        if (miny != null) {
            sb.append("miny=").append(miny).append("\n");
        }
        if (maxy != null) {
            sb.append("maxy=").append(maxy).append("\n");
        }
        if (geom != null) {
            sb.append("geom=").append(geom).append("\n");
        }
        if (fields != null) {
            sb.append("fields:\n");
            for (String field : fields) {
                sb.append(field).append("\n");
            }
        }
        if (historicalLocations != null) {
            sb.append("historical locations:\n");
            for (Entry<Date, Geometry> hl : historicalLocations.entrySet()) {
                sb.append(hl.getKey()).append(" => ").append(hl.getValue()).append("\n");
            }
        }
        if (properties != null) {
            sb.append("properties:\n");
            for (Entry<String, Object> p : properties.entrySet()) {
                sb.append(p.getKey()).append(" => ").append(p.getValue()).append("\n");
            }
        }
        if (children != null) {
            sb.append("children:\n");
            for (ProcedureDataset child : children) {
                sb.append(child.id).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (this.getClass() == object.getClass()) {
            final ProcedureDataset that = (ProcedureDataset) object;
            return Objects.equals(this.id,           that.id)   &&
                   Objects.equals(this.dateEnd,      that.dateEnd)   &&
                   Objects.equals(this.name,         that.name)   &&
                   Objects.equals(this.description,  that.description)   &&
                   Objects.equals(this.dateStart,    that.dateStart)   &&
                   Objects.equals(this.minx,         that.minx)   &&
                   Objects.equals(this.maxx,         that.maxx)   &&
                   Objects.equals(this.miny,         that.miny)   &&
                   Objects.equals(this.maxy,         that.maxy)   &&
                   Objects.equals(this.fields,       that.fields)   &&
                   Objects.equals(this.children,     that.children)   &&
                   Objects.equals(this.omType,       that.omType)   &&
                   Objects.equals(this.geom,         that.geom)   &&
                   Objects.equals(this.historicalLocations, that.historicalLocations)   &&
                   Objects.equals(this.properties,   that.properties)   &&
                   Objects.equals(this.type,         that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.name, this.description, this.type, this.omType, this.children, this.fields,
                this.dateStart, this.dateEnd, this.minx, this.maxx, this.miny, this.maxy, this.geom, this.historicalLocations, this.properties);
    }
}
