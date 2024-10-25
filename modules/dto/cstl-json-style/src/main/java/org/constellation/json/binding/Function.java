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
package org.constellation.json.binding;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 *
 * ColorMap function. Interval and NanColor getter/setter are here to ensure
 * implementation have methods.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@function")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Interpolate.class, name = "interpolate"),
    @JsonSubTypes.Type(value = Categorize.class, name = "categorize")
})
public interface Function extends StyleElement<org.opengis.filter.Expression> {

    public Double getInterval();

    public void setInterval(Double interval);

    public String getNanColor();

    public void setNanColor(String nanColor);
}
