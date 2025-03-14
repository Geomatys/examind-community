/*
 *    Examind Community - An open source and standard compliant SDI
 *    https://www.examind.com/en/examind-community-2/about/
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.map.featureinfo;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyType;

import org.apache.sis.feature.Features;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * @author Alexis Manin (Geomatys)
 */
public class GeoJSONFeatureSerializer extends StdSerializer<Feature> {
    public GeoJSONFeatureSerializer() {
        super(Feature.class);
    }

    @Override
    public void serialize(Feature feature, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        final FeatureType type = feature.getType();
        /*
         * TODO: find a better way to filter. We're mapping by name to remove link operations, because we do not have
         * consistent way to detect link Operations. You can find code searching for an @ prefix, but this is not
         * sufficient, because an operator could be "@myProperty + 2". The choice here is to keep the name of the
         * resulting attribute if any, as link operation results in its dependency.
         */
        final Map<String, ? extends PropertyType> uniqueProperties = type.getProperties(true).stream()
                .collect(Collectors.toMap(GeoJSONFeatureSerializer::getName, Function.identity(), (o1, o2) -> o1));
        for (Map.Entry<String, ? extends PropertyType> entry : uniqueProperties.entrySet()) {
            PropertyType p = entry.getValue();
            if (!("sis:envelope".equals(p.getName().toString()) ||  "sis:geometry".equals(p.getName().toString()))) {
                final Object value = feature.getPropertyValue(p.getName().toString());
                if (value != null) {
                    gen.writeObjectField(entry.getKey(), value);
                }
            }
        }
        /* TODO: attributes could have a unit characteristic. For now, we ignore it because:
         * 1. It could be costly to fetch.
         * 2. We have to properly define where and how to serialize it.
         */
        gen.writeEndObject();
    }

    private static String getName(final PropertyType p) {
        return Features.toAttribute(p)
                .map(attr -> attr.getName())
                .orElseGet(p::getName)
                .tip()
                .toString();
    }
}
