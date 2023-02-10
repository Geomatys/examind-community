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

package org.constellation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.constellation.dto.service.config.wxs.LayerSummary;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * @author Bernard Fabien (Geomatys)
 * @version 0.9
 * @since 0.9
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class StyleBrief extends Style implements Serializable {

    private String provider;
    private String title;
    private String owner;
    private List<DataBrief> dataList;
    private List<LayerSummary> layersList;


    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public List<DataBrief> getDataList() {
        return dataList;
    }

    public void setDataList(List<DataBrief> dataList) {
        this.dataList = dataList;
    }

    public List<LayerSummary> getLayersList() {
        return layersList;
    }

    public void setLayersList(List<LayerSummary> layersList) {
        this.layersList = layersList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StyleBrief that = (StyleBrief) o;

        if (!super.equals(o)) return false;
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (provider != null ? !provider.equals(that.provider) : that.provider != null) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (provider != null ? provider.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", provider='" + provider + '\'' +
                ", title='" + title + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
