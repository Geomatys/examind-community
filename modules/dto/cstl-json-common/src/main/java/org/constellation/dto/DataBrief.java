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
import jakarta.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.metadata.MetadataLightBrief;

/**
 * @author Bernard Fabien (Geomatys)
 * @author Garcia Benjamin (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @version 0.9
 * @since 0.9
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataBrief extends Data implements Serializable {

    @XmlElement(name="provider")
    private String provider;

    @XmlElement(name="parent")
    private String parent;

    @XmlElement(name="title")
    private String title;

    @XmlElement(name="owner")
    private String owner;

    @XmlElement(name="targetStyle")
    private List<StyleBrief> targetStyle = new ArrayList<>(0);

    @XmlElement(name="targetService")
    private List<ServiceReference> targetService = new ArrayList<>(0);

    @XmlElement(name="targetSensor")
    private List<String> targetSensor = new ArrayList<>(0);

    @XmlElement(name="metadatas")
    private List<MetadataLightBrief> metadatas = new ArrayList<>(0);

    @XmlElement(name="linkedDatas")
    private List<DataBrief> linkedDatas = new ArrayList<>(0);

    @XmlElement(name="pyramidConformProviderId")
    private String pyramidConformProviderId;

    @XmlTransient
    private DataDescription dataDescription;

    @XmlElement(name="dimension")
    private List<Dimension> dimensions = new ArrayList<>(0);

    public DataBrief() {

    }

    public DataBrief(Data data) {
        super(data);
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(final String parent) {
        this.parent = parent;
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

    public StyleBrief getFirstStyle() {
        if (targetStyle != null && !targetStyle.isEmpty()) {
            return targetStyle.get(0);
        }
        return null;
    }

    public List<StyleBrief> getTargetStyle() {
        return targetStyle;
    }

    public void setTargetStyle(final List<StyleBrief> targetStyle) {
        this.targetStyle = targetStyle;
    }

    public List<ServiceReference> getTargetService() {
        return targetService;
    }

    public void setTargetService(final List<ServiceReference> targetService) {
        this.targetService = targetService;
    }

    public List<String> getTargetSensor() {
        return targetSensor;
    }

    public void setTargetSensor(final List<String> targetSensor) {
        this.targetSensor = targetSensor;
    }

    public String getPyramidConformProviderId() {
        return pyramidConformProviderId;
    }

    public void setPyramidConformProviderId(String pyramidConformProviderId) {
        this.pyramidConformProviderId = pyramidConformProviderId;
    }

    public List<MetadataLightBrief> getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(List<MetadataLightBrief> metadatas) {
        this.metadatas = metadatas;
    }

    public DataDescription getDataDescription() {
        return dataDescription;
    }

    public void setDataDescription(DataDescription dataDescription) {
        this.dataDescription = dataDescription;
    }

    public List<DataBrief> getLinkedDatas() {
        return linkedDatas;
    }

    public void setLinkedDatas(List<DataBrief> linkedDatas) {
        this.linkedDatas = linkedDatas;
    }

    public List<Dimension> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<Dimension> dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (this.title != null) {
            sb.append("title: ").append(title).append('\n');
        }
        if (this.owner != null) {
            sb.append("owner: ").append(owner).append('\n');
        }
        if (this.provider != null) {
            sb.append("provider: ").append(provider).append('\n');
        }
        if (this.pyramidConformProviderId != null) {
            sb.append("pyramidConformProviderId: ").append(pyramidConformProviderId).append('\n');
        }
        if (this.parent != null) {
            sb.append("parent: ").append(parent).append('\n');
        }
        sb.append("dimensions:\n");
        for (Dimension s : dimensions) {
            sb.append(s).append('\n');
        }
        sb.append("targetStyle:\n");
        for (StyleBrief s : targetStyle) {
            sb.append(s).append('\n');
        }
        sb.append("targetService:\n");
        for (ServiceReference s : targetService) {
            sb.append(s).append('\n');
        }
        sb.append("targetSensor:\n");
        for (String s : targetSensor) {
            sb.append(s).append('\n');
        }
        sb.append("linkedDatas:\n");
        for (DataBrief s : linkedDatas) {
            sb.append(s).append('\n');
        }
        sb.append("matadataDatas:\n");
        for (MetadataLightBrief s : metadatas) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }
}
