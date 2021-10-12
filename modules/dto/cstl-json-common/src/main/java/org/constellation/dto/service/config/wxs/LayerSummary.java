/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.dto.service.config.wxs;


import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.List;
import javax.xml.namespace.QName;
import org.constellation.dto.Data;
import org.constellation.dto.Layer;
import org.constellation.dto.StyleBrief;

/**
 * @author Cédric Briançon (Geomatys)
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class LayerSummary extends org.constellation.dto.Layer {

    private String type;
    private String subtype;
    private String owner;
    private List<StyleBrief> targetStyle;

    public LayerSummary() {}

    public LayerSummary(final Layer layer, final Data db, final String owner, final List<StyleBrief> targetStyles) {
        super(layer);
        this.type = db.getType();
        this.subtype = db.getSubtype();
        this.owner = owner;
        this.targetStyle = targetStyles;
    }

    public LayerSummary(final Integer id, final QName name, final String alias, final Integer serviceID,
            final Integer dataID, final Date date, final String config, final Integer ownerId, final String title,
            final String dataType, final String dataSubType, final String dataOwner, final List<StyleBrief> targetStyles) {
        super(id, name, alias, serviceID, dataID, date, config, ownerId,  title);
        this.type = dataType;
        this.subtype = dataSubType;
        this.owner = dataOwner;
        this.targetStyle = targetStyles;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<StyleBrief> getTargetStyle() {
        return targetStyle;
    }

    public StyleBrief getFirstStyle() {
        if (targetStyle != null && !targetStyle.isEmpty()) {
            return targetStyle.get(0);
        }
        return null;
    }

    public void setTargetStyle(List<StyleBrief> targetStyle) {
        this.targetStyle = targetStyle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LayerSummary that = (LayerSummary) o;

        if (!super.equals(o)) return false;
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (subtype != null ? !subtype.equals(that.subtype) : that.subtype != null) return false;
        if (targetStyle != null ? !targetStyle.equals(that.targetStyle) : that.targetStyle != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (subtype != null ? subtype.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (targetStyle != null ? targetStyle.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LayerSummary{" +
                "id='" + getId() + '\'' +
                ", dataId='" + getDataId() + '\'' +
                ", name='" + getName() + '\'' +
                ", alias='" + getAlias() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", type='" + type + '\'' +
                ", subtype='" + subtype + '\'' +
                ", date=" + getDate() +
                ", owner='" + owner + '\'' +
                ", targetStyle=" + targetStyle +
                '}';
    }
}
