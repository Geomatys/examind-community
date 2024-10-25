/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class NameInProvider {

    public final Integer layerId;
    public final QName layerName;
    public final Integer providerID;
    public final String alias;
    public final Date dataVersion;
    public final GenericName dataName;
    public final Integer dataId;

    public NameInProvider(final Integer layerId, final QName name, final Integer providerID, final Date dataVersion, final String alias, final GenericName dataName, final Integer dataId) {
        this.layerId = layerId;
        this.layerName = name;
        this.providerID = providerID;
        this.dataVersion= dataVersion;
        this.alias = alias;
        this.dataName = dataName;
        this.dataId = dataId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.layerId);
        hash = 83 * hash + Objects.hashCode(this.layerName);
        hash = 83 * hash + Objects.hashCode(this.providerID);
        hash = 83 * hash + Objects.hashCode(this.alias);
        hash = 83 * hash + Objects.hashCode(this.dataVersion);
        hash = 83 * hash + Objects.hashCode(this.dataName);
        hash = 83 * hash + Objects.hashCode(this.dataId);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (this.getClass() == o.getClass()) {
            NameInProvider that = (NameInProvider) o;
            return Objects.equals(this.layerId,     that.layerId) &&
                   Objects.equals(this.alias,       that.alias) &&
                   Objects.equals(this.dataVersion, that.dataVersion) &&
                   Objects.equals(this.layerName,   that.layerName) &&
                   Objects.equals(this.providerID,  that.providerID) &&
                   Objects.equals(this.dataName,    that.dataName) &&
                   Objects.equals(this.dataId,      that.dataId);
        }
        return false;
    }
}
