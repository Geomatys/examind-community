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
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ProcedureTree {

    private String id;

    private String type;

    protected String omType;

    private List<ProcedureTree> children = new ArrayList<>();

    private List<String> fields = new ArrayList<>();

    private Date dateStart;
    private Date dateEnd;

    private Double minx;
    private Double maxx;

    private Double miny;
    private Double maxy;

    public ProcedureTree() {

    }

    public ProcedureTree(String id, String type, String omType, Date dateStart, Date dateEnd, Double minx, Double maxx, Double miny, Double maxy, List<String> fields) {
        this.id = id;
        this.type = type;
        this.omType = omType;
        this.fields = fields;

        this.dateStart = dateStart;
        this.dateEnd = dateEnd;

        this.minx = minx;
        this.maxx = maxx;
        this.miny = miny;
        this.maxy = maxy;
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
    public List<ProcedureTree> getChildren() {
        return children;
    }

    /**
     * @param children the children to set
     */
    public void setChildren(List<ProcedureTree> children) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[ProcedureTree]\n");
        sb.append("id=").append(id).append('\n');
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
        if (fields != null) {
            sb.append("fields:");
            for (String field : fields) {
                sb.append(field).append("\n");
            }
        }
        if (children != null) {
            sb.append("children:");
            for (ProcedureTree child : children) {
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

        if (object instanceof ProcedureTree) {
            final ProcedureTree that = (ProcedureTree) object;
            return Objects.equals(this.id,           that.id)   &&
                   Objects.equals(this.dateEnd,      that.dateEnd)   &&
                   Objects.equals(this.dateStart,    that.dateStart)   &&
                   Objects.equals(this.minx,         that.minx)   &&
                   Objects.equals(this.maxx,         that.maxx)   &&
                   Objects.equals(this.miny,         that.miny)   &&
                   Objects.equals(this.maxy,         that.maxy)   &&
                   Objects.equals(this.fields,       that.fields)   &&
                   Objects.equals(this.children,     that.children)   &&
                   Objects.equals(this.omType,       that.omType)   &&
                   Objects.equals(this.type,         that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + Objects.hashCode(this.id);
        hash = 11 * hash + Objects.hashCode(this.type);
        hash = 11 * hash + Objects.hashCode(this.omType);
        hash = 11 * hash + Objects.hashCode(this.children);
        hash = 11 * hash + Objects.hashCode(this.fields);
        hash = 11 * hash + Objects.hashCode(this.dateStart);
        hash = 11 * hash + Objects.hashCode(this.dateEnd);
        hash = 11 * hash + Objects.hashCode(this.minx);
        hash = 11 * hash + Objects.hashCode(this.maxx);
        hash = 11 * hash + Objects.hashCode(this.miny);
        hash = 11 * hash + Objects.hashCode(this.maxy);
        return hash;
    }
}
