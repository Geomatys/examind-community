/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Date;
import java.util.Objects;
import javax.xml.namespace.QName;

/**
 *
 * Abstract class for a MapContext layer.
 * 
 * @author Guilhem Legal (Geomatys)
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "layerType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = InternalServiceMCLayerDTO.class, name = "internal-service"),
    @JsonSubTypes.Type(value = ExternalServiceMCLayerDTO.class, name = "external-service"),
    @JsonSubTypes.Type(value = DataMCLayerDTO.class, name = "internal-data")})
public abstract class AbstractMCLayerDTO extends Identifiable implements Comparable<AbstractMCLayerDTO> {

    private QName name;
    private int order;
    private int opacity;
    private boolean visible;

    private Date date;
    private String type;
    private String owner;

    public AbstractMCLayerDTO() {}

    public AbstractMCLayerDTO(QName name, int order, int opacity, boolean visible, Date date, String type, String owner) {
        this.name = name;
        this.order = order;
        this.opacity = opacity;
        this.visible = visible;
        this.date = date;
        this.type = type;
        this.owner = owner;
    }

    public QName getName() {
        return name;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOpacity() {
        return opacity;
    }

    public void setOpacity(int opacity) {
        this.opacity = opacity;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public int compareTo(AbstractMCLayerDTO o) {
        return getOrder() - o.getOrder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("name:").append(name).append('\n');
        sb.append("order:").append(order).append('\n');
        sb.append("opacity:").append(opacity).append('\n');
        sb.append("visible:").append(visible).append('\n');

        sb.append("date:").append(date).append('\n');
        sb.append("type:").append(type).append('\n');
        sb.append("owner:").append(owner).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass() && super.equals(obj)) {
            AbstractMCLayerDTO that = (AbstractMCLayerDTO) obj;
            return Objects.equals(this.opacity, that.opacity) &&
                   Objects.equals(this.name, that.name) &&
                   Objects.equals(this.order, that.order) &&
                   Objects.equals(this.date, that.date) &&
                   Objects.equals(this.type, that.type) &&
                   Objects.equals(this.owner, that.owner) &&
                   Objects.equals(this.visible, that.visible);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 79 * hash + this.name.hashCode();
        hash = 79 * hash + this.date.hashCode();
        hash = 79 * hash + this.type.hashCode();
        hash = 79 * hash + this.owner.hashCode();
        hash = 79 * hash + this.order;
        hash = 79 * hash + this.opacity;
        hash = 79 * hash + (this.visible ? 1 : 0);
        return hash;
    }
}
