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

import org.constellation.dto.service.config.Languages;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.6
 */
@XmlRootElement(name="LayerContext")
@XmlAccessorType(XmlAccessType.FIELD)
public class LayerContext extends AbstractConfigurationObject {

    private Layer mainLayer;

    private String security;

    private Languages supportedLanguages;

    private final Map<String, String> customParameters = new HashMap<>();

    @XmlElementWrapper(name="featureInfos")
    @XmlElement(name="FeatureInfo")
    private List<GetFeatureInfoCfg> getFeatureInfoCfgs;

    public LayerContext() {

    }

    /**
     * @return the layers
     */
    public Layer getMainLayer() {
        return mainLayer;
    }

    public void setMainLayer(final Layer mainlayer) {
        this.mainLayer = mainlayer;
    }

    /**
     * @return the security constraint, or {@code null} if none.
     */
    public String getSecurity() {
        return security;
    }

    /**
     * Sets the security value.
     *
     * @param security the security value.
     */
    public void setSecurity(String security) {
        this.security = security;
    }

    /**
     * @return the supportedLanguages
     */
    public Languages getSupportedLanguages() {
        return supportedLanguages;
    }

    /**
     * @param supportedLanguages the supportedLanguages to set
     */
    public void setSupportedLanguages(Languages supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    /**
     * @return the customParameters
     */
    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    /**
     * Return custom getFeatureInfos
     * @return a list with GetFeatureInfoCfg, can be null.
     */
    public List<GetFeatureInfoCfg> getGetFeatureInfoCfgs() {
        if (getFeatureInfoCfgs == null) {
            getFeatureInfoCfgs = new ArrayList<>();
        }
        return getFeatureInfoCfgs;
    }

    public void setGetFeatureInfoCfgs(final List<GetFeatureInfoCfg> getFeatureInfoCfgs) {
        this.getFeatureInfoCfgs = getFeatureInfoCfgs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (security != null && !security.isEmpty()) {
            sb.append("Security=").append(security);
        }
        if (supportedLanguages != null) {
            sb.append("Supported languages:\n").append(supportedLanguages);
        }
        if (customParameters != null && !customParameters.isEmpty()) {
            sb.append("Custom parameters:\n");
            for (Entry<String, String> entry : customParameters.entrySet()) {
                sb.append("key:").append(entry.getKey()).append(" value:").append(entry.getValue()).append('\n');
            }
        }
        if (getFeatureInfoCfgs != null && !getFeatureInfoCfgs.isEmpty()) {
            sb.append("featureInfos:\n").append(getFeatureInfoCfgs).append('\n');
            for (final GetFeatureInfoCfg getFeatureInfoCfg : getFeatureInfoCfgs) {
                sb.append("featureInfos:\n").append(getFeatureInfoCfg).append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof LayerContext) {
            final LayerContext that = (LayerContext) obj;
            return Objects.equals(this.mainLayer, that.mainLayer) &&
                   Objects.equals(this.security, that.security) &&
                   Objects.equals(this.customParameters, that.customParameters) &&
                   Objects.equals(this.supportedLanguages, that.supportedLanguages)&&
                   Objects.equals(this.getFeatureInfoCfgs, that.getFeatureInfoCfgs);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.mainLayer != null ? this.mainLayer.hashCode() : 0);
        hash = 97 * hash + (this.security != null ? this.security.hashCode() : 0);
        hash = 97 * hash +  this.customParameters.hashCode();
        hash = 97 * hash + (this.supportedLanguages != null ? this.supportedLanguages.hashCode() : 0);
        hash = 97 * hash + (this.getFeatureInfoCfgs != null ? this.getFeatureInfoCfgs.hashCode() : 0);
        return hash;
    }
}
