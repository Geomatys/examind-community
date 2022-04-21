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
 * MapContext layer pointing to an external WMS layer.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ExternalServiceMCLayerDTO extends AbstractMCLayerDTO {

    private QName externalLayer;
    private String externalStyle;
    private String externalServiceUrl;
    private String externalServiceVersion;
    private String externalLayerExtent;

    public ExternalServiceMCLayerDTO(){}
    
    public ExternalServiceMCLayerDTO(Integer id, QName name, Integer order, Integer opacity, Boolean visible, Date date, String type, String owner,
            QName externalLayer, String externalStyle, String externalServiceUrl, String externalServiceVersion,  String externalLayerExtent) {
        super(id, name, order, opacity, visible, date, type, owner);
        this.externalLayer = externalLayer;
        this.externalStyle = externalStyle;
        this.externalServiceUrl = externalServiceUrl;
        this.externalServiceVersion = externalServiceVersion;
        this.externalLayerExtent = externalLayerExtent;
    }

    public String getExternalStyle() {
        return externalStyle;
    }

    public void setExternalStyle(String externalStyle) {
        this.externalStyle = externalStyle;
    }

    public String getExternalServiceUrl() {
        return externalServiceUrl;
    }

    public void setExternalServiceUrl(String externalServiceUrl) {
        this.externalServiceUrl = externalServiceUrl;
    }

    public String getExternalServiceVersion() {
        return externalServiceVersion;
    }

    public void setExternalServiceVersion(String externalServiceVersion) {
        this.externalServiceVersion = externalServiceVersion;
    }

    public QName getExternalLayer() {
        return externalLayer;
    }

    public void setExternalLayer(QName externalLayer) {
        this.externalLayer = externalLayer;
    }

    public String getExternalLayerExtent() {
        return externalLayerExtent;
    }

    public void setExternalLayerExtent(String externalLayerExtent) {
        this.externalLayerExtent = externalLayerExtent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("externalLayer:").append(externalLayer).append('\n');
        sb.append("externalStyle:").append(externalStyle).append('\n');
        sb.append("externalServiceUrl:").append(externalServiceUrl).append('\n');
        sb.append("externalServiceVersion:").append(externalServiceVersion).append('\n');
        sb.append("externalLayerExtent:").append(externalLayerExtent).append('\n');
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
            ExternalServiceMCLayerDTO that = (ExternalServiceMCLayerDTO) obj;
            return Objects.equals(this.externalLayer, that.externalLayer) &&
                   Objects.equals(this.externalLayerExtent, that.externalLayerExtent) &&
                   Objects.equals(this.externalServiceUrl, that.externalServiceUrl) &&
                   Objects.equals(this.externalServiceVersion, that.externalServiceVersion) &&
                   Objects.equals(this.externalStyle, that.externalStyle);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 79 * hash + Objects.hashCode(this.externalStyle);
        hash = 79 * hash + Objects.hashCode(this.externalServiceUrl);
        hash = 79 * hash + Objects.hashCode(this.externalServiceVersion);
        hash = 79 * hash + Objects.hashCode(this.externalLayer);
        hash = 79 * hash + Objects.hashCode(this.externalLayerExtent);
        return hash;
    }
}
