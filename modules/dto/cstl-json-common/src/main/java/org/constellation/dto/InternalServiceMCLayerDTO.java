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

import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

/**
 * MapContext layer pointing to an internal examind layer.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class InternalServiceMCLayerDTO extends AbstractMCLayerDTO {

    private Integer layerId;
    private Integer styleId;

    /**
     * Those attribute are redundant, but here for the UI
     */
    private Integer dataId;
    private String styleName;
    private String serviceIdentifier;
    private List<String> serviceVersions;

    public InternalServiceMCLayerDTO(){}

    public InternalServiceMCLayerDTO(QName name, int order, int opacity, boolean visible, Integer layerId, 
            Integer styleId, String styleName, Date date, String type, String owner, Integer dataId, String serviceIdentifier, List<String> serviceVersions) {
        super(name, order, opacity, visible, date, type, owner);
        this.layerId = layerId;
        this.styleId = styleId;
        this.dataId = dataId;
        this.styleName = styleName;
        this.serviceIdentifier = serviceIdentifier;
        this.serviceVersions = serviceVersions;
    }

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }

    public Integer getStyleId() {
        return styleId;
    }

    public void setStyleId(Integer styleId) {
        this.styleId = styleId;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }

    public List<String> getServiceVersions() {
        return serviceVersions;
    }

    public void setServiceVersions(List<String> serviceVersions) {
        this.serviceVersions = serviceVersions;
    }

    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(Integer dataId) {
        this.dataId = dataId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("layerId:").append(layerId).append('\n');
        sb.append("styleId:").append(styleId).append('\n');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof InternalServiceMCLayerDTO && super.equals(obj)) {
            InternalServiceMCLayerDTO that = (InternalServiceMCLayerDTO) obj;
            return Objects.equals(this.layerId, that.layerId) &&
                   Objects.equals(this.styleId, that.styleId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 79 * hash + Objects.hashCode(this.layerId);
        hash = 79 * hash + Objects.hashCode(this.styleId);
        return hash;
    }

}
