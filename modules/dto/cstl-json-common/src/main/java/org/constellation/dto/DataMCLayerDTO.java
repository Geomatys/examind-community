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
import java.util.Objects;
import javax.xml.namespace.QName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataMCLayerDTO extends AbstractMCLayerDTO {

    private Integer dataId;
    private Integer styleId;

    /**
     * Those attribute are redundant, but here for the UI
     */
    private String styleName;

    public DataMCLayerDTO() {}

    public DataMCLayerDTO(QName name, int order, int opacity, boolean visible, Date date, String type, String owner, int dataId, Integer styleId, String styleName) {
        super(name, order, opacity, visible, date, type, owner);
        this.dataId = dataId;
        this.styleId = styleId;
        this.styleName = styleName;
    }

    public Integer getDataId() {
        return dataId;
    }

    public void setDataId(Integer dataId) {
        this.dataId = dataId;
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("styleId:").append(styleId).append('\n');
        sb.append("styleName:").append(styleName).append('\n');
        sb.append("dataId:").append(dataId).append('\n');
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
            DataMCLayerDTO that = (DataMCLayerDTO) obj;
            return Objects.equals(this.styleId, that.styleId) &&
                   Objects.equals(this.styleName, that.styleName) &&
                   Objects.equals(this.dataId, that.dataId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 79 * hash + Objects.hashCode(this.styleId);
        hash = 79 * hash + Objects.hashCode(this.styleName);
        hash = 79 * hash + Objects.hashCode(this.dataId);
        return hash;
    }
}
