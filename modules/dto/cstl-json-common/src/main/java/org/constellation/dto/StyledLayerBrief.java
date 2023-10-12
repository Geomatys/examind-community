/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * @author Estelle Idée (Geomatys)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StyledLayerBrief extends StyleBrief {

    private Boolean activateStats;

    public boolean getActivateStats() {
        return activateStats;
    }

    public void setActivateStats(boolean activateStats) {
        this.activateStats = activateStats;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StyledLayerBrief that = (StyledLayerBrief) o;

        if (activateStats != that.activateStats) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (activateStats != null ? activateStats.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                ", activateStats='" + activateStats + '\'' +
                '}';
    }
}
