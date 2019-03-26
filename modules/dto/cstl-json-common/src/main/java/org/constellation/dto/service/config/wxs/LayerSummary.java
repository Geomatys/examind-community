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
import org.constellation.dto.DataBrief;
import org.constellation.dto.StyleBrief;

/**
 * @author Cédric Briançon (Geomatys)
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class LayerSummary extends org.constellation.dto.Layer {

    private String type;
    private String subtype;
    private String owner;
    private String provider;
    private Integer providerId;
    private List<StyleBrief> targetStyle;

    public LayerSummary() {}

    public LayerSummary(final Layer layer, final DataBrief db, final List<StyleBrief> targetStyles) {
        super(layer.getId(),
              layer.getName().getLocalPart(),
              layer.getName().getNamespaceURI(),
              layer.getAlias(),
              null,
              db.getId(),
              layer.getDate(),
              null,
              db.getOwnerId(),
              layer.getTitle());
        this.type = db.getType();
        this.subtype = db.getSubtype();
        this.owner = db.getOwner();
        this.provider = db.getProvider();
        this.providerId = db.getProviderId();
        this.targetStyle = targetStyles;
    }

    public LayerSummary(final Integer id, final String name, final String namespace, final String alias, final Integer serviceID,
            final Integer dataID, final Date date, final String config, final Integer ownerId, final String title,
            final String dataType, final String dataSubType, final String dataOwner, final String dataProvider, final Integer dataProviderID,
            final List<StyleBrief> targetStyles) {
        super(id, name, namespace, alias, serviceID, dataID, date, config, ownerId,  title);
        this.type = dataType;
        this.subtype = dataSubType;
        this.owner = dataOwner;
        this.provider = dataProvider;
        this.providerId = dataProviderID;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getProviderId() {
        return providerId;
    }

    public void setProviderId(Integer providerId) {
        this.providerId = providerId;
    }

    public List<StyleBrief> getTargetStyle() {
        return targetStyle;
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
        if (provider != null ? !provider.equals(that.provider) : that.provider != null) return false;
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
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (targetStyle != null ? targetStyle.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LayerSummary{" +
                "id='" + getId() + '\'' +
                ", dataId='" + getDataId() + '\'' +
                ", name='" + getName() + '\'' +
                ", namespace='" + getNamespace() + '\'' +
                ", alias='" + getAlias() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", type='" + type + '\'' +
                ", subtype='" + subtype + '\'' +
                ", date=" + getDate() +
                ", owner='" + owner + '\'' +
                ", provider='" + provider + '\'' +
                ", targetStyle=" + targetStyle +
                '}';
    }
}
