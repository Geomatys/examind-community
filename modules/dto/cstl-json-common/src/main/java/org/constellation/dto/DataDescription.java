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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CoverageDataDescription.class, name = "coverage"),
    @JsonSubTypes.Type(value = FeatureDataDescription.class, name = "feature")
})
public class DataDescription implements Serializable {

    protected double[] boundingBox;

    public DataDescription() {
        this.boundingBox = new double[]{-180,-90,180,90};
    }

    public DataDescription(double[] boundingBox) {
        this.boundingBox = boundingBox;
    }

    public double[] getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(final double[] boundingBox) {
        this.boundingBox = boundingBox;
    }

}
