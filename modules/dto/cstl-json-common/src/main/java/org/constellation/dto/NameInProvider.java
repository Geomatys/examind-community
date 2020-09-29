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
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class NameInProvider {

    public Integer layerId;
    public GenericName name;
    public Integer providerID;
    public String alias;
    public Date dataVersion;

    public NameInProvider(final Integer layerId, final GenericName name, final Integer providerID, final Date dataVersion, final String alias) {
        this.layerId = layerId;
        this.name = name;
        this.providerID = providerID;
        this.dataVersion= dataVersion;
        this.alias = alias;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.layerId);
        hash = 83 * hash + Objects.hashCode(this.name);
        hash = 83 * hash + Objects.hashCode(this.providerID);
        hash = 83 * hash + Objects.hashCode(this.alias);
        hash = 83 * hash + Objects.hashCode(this.dataVersion);
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof NameInProvider) {
            NameInProvider that = (NameInProvider) o;
            return Objects.equals(this.layerId,     that.layerId) &&
                   Objects.equals(this.alias,       that.alias) &&
                   Objects.equals(this.dataVersion, that.dataVersion) &&
                   Objects.equals(this.name,        that.name) &&
                   Objects.equals(this.providerID,  that.providerID);
        }
        return false;
    }
}
