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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public class FeatureDataDescription extends SimpleDataDescription {

    private PropertyDescription geometryProperty;
    private List<PropertyDescription> properties;

    public FeatureDataDescription() {
        this.properties = new ArrayList<>(0);
    }

    public PropertyDescription getGeometryProperty() {
        return geometryProperty;
    }

    public void setGeometryProperty(final PropertyDescription geometryProperty) {
        this.geometryProperty = geometryProperty;
    }

    public List<PropertyDescription> getProperties() {
        return properties;
    }

    public void setProperties(final List<PropertyDescription> properties) {
        this.properties = properties;
    }
}
