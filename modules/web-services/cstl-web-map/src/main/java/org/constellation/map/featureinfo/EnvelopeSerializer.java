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

import org.opengis.geometry.Envelope;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Simplistic serialization of bounding boxes
 * <ul>
 *     <li>For now, do not specify coordinate reference system</li>
 *     <li>
 *         Create a JSON object with two attributes:
 *         <ul>
 *             <li><em>lowerCorner</em>: Double array, representing {@code Envelope.getLowerCorner().getCoordinates()}</li>
 *             <li><em>upperCorner</em>: Double array, representing {@code Envelope.getUpperCorner().getCoordinates()}</li>
 *         </ul>
 *     </li>
 * </ul>
 * @author Alexis Manin (Geomatys)
 */
public class EnvelopeSerializer extends StdSerializer<Envelope> {
    protected EnvelopeSerializer() {
        super(Envelope.class);
    }

    @Override
    public void serialize(Envelope value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        final int dimension = value.getDimension();
        gen.writeFieldName("lowerCorner");
        gen.writeArray(value.getLowerCorner().getCoordinates(), 0, dimension);
        gen.writeFieldName("upperCorner");
        gen.writeArray(value.getUpperCorner().getCoordinates(), 0, dimension);
        gen.writeEndObject();
    }
}
