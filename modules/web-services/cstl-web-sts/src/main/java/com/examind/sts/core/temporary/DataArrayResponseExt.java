/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2022 Geomatys.
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
package com.examind.sts.core.temporary;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.geotoolkit.sts.json.DataArray;
import org.geotoolkit.sts.json.DataArrayResponse;
import org.geotoolkit.sts.json.STSPagedResponse;

/**
 * Temporary extension of the geotk class {@linkplain DataArrayResponse}.
 * TODO This class must be removed when geotk will be upgraded.
 *
 * @author guilhem
 */
public class DataArrayResponseExt extends DataArrayResponse implements STSPagedResponse {

    @JsonProperty("@iot.count")
    private BigDecimal iotCount = null;

    @JsonProperty("@iot.nextLink")
    private String iotNextLink = null;

    public DataArrayResponseExt() {
        
    }

    public DataArrayResponseExt(List<DataArray> value, BigDecimal iotCount, String iotNextLink) {
        super(value);
        this.iotCount = iotCount;
        this.iotNextLink = iotNextLink;
    }

    public DataArrayResponse iotCount(BigDecimal iotCount) {
        this.iotCount = iotCount;
        return this;
    }

    /**
     * Get iotCount
     *
     * @return iotCount
  *
     */
    @Override
    public BigDecimal getIotCount() {
        return iotCount;
    }

    @Override
    public void setIotCount(BigDecimal iotCount) {
        this.iotCount = iotCount;
    }

    public DataArrayResponse iotNextLink(String iotNextLink) {
        this.iotNextLink = iotNextLink;
        return this;
    }

    /**
     * Get iotNextLink
     *
     * @return iotNextLink
  *
     */
    @Override
    public String getIotNextLink() {
        return iotNextLink;
    }

    @Override
    public void setIotNextLink(String iotNextLink) {
        this.iotNextLink = iotNextLink;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataArrayResponseExt da = (DataArrayResponseExt) o;
        return Objects.equals(this.iotCount, da.iotCount) &&
               Objects.equals(this.getValue(), da.getValue()) &&
               Objects.equals(this.iotNextLink, da.iotNextLink);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(iotCount, getValue(), iotNextLink);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class DataArrayResponseExt {\n");
        sb.append("    iotCount: ").append(toIndentedString(iotCount)).append("\n");
        sb.append("    value: ").append(toIndentedString(getValue())).append("\n");
        sb.append("    iotNextLink: ").append(toIndentedString(iotNextLink)).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
