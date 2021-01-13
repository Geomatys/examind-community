/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.json.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
final class JsonViewWriter {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.json.view");

    private final JsonFilter filter;
    private final JsonGenerator jg;
    private final SerializerProvider sp;

    JsonViewWriter(JsonFilter filter, JsonGenerator jg, SerializerProvider sp) {
        this.filter = filter;
        this.jg = jg;
        this.sp = sp;
    }

    void serialize(Object obj) throws IOException {

        if (obj instanceof Boolean)         jg.writeBoolean((Boolean) obj);
        else if (obj instanceof Byte)       jg.writeNumber((Byte) obj);
        else if (obj instanceof Short)      jg.writeNumber((Short) obj);
        else if (obj instanceof Integer)    jg.writeNumber((Integer) obj);
        else if (obj instanceof Long)       jg.writeNumber((Long) obj);
        else if (obj instanceof Float)      jg.writeNumber((Float) obj);
        else if (obj instanceof Double)     jg.writeNumber((Double) obj);
        else if (obj instanceof BigInteger) jg.writeNumber((BigInteger) obj);
        else if (obj instanceof BigDecimal) jg.writeNumber((BigDecimal) obj);
        else if (obj instanceof Character)  jg.writeNumber((Character) obj);
        else if (obj instanceof String)     jg.writeString((String) obj);
        else if (obj instanceof Date)       sp.defaultSerializeDateValue((Date) obj, jg);
        else if (obj instanceof Temporal)   sp.defaultSerializeValue(obj, jg);
        else if (obj instanceof URL)        jg.writeString(obj.toString());
        else if (obj instanceof URI)        jg.writeString(obj.toString());
        else if (obj instanceof Class)      jg.writeString(((Class) obj).getCanonicalName());         
        else if (obj == null)               jg.writeNull(); 
        else if (obj.getClass().isEnum())   jg.writeString(((Enum) obj).name());
        else if (obj instanceof byte[])     jg.writeBinary((byte[]) obj);
        else if(obj instanceof Collection){
            jg.writeStartArray();
            for (Object cdt : (Collection)obj) {
                final JsonViewWriter writer = new JsonViewWriter(filter, jg, sp);
                writer.serialize(cdt);
            }
            jg.writeEndArray();
        } else  if(obj.getClass().isArray()){
            jg.writeStartArray();
            for (int n=Array.getLength(obj),i=0;i<n;i++) {
                final JsonViewWriter writer = new JsonViewWriter(filter, jg, sp);
                writer.serialize(Array.get(obj, i));
            }
            jg.writeEndArray();
        } else  if(obj instanceof Map){
            final Map<Object, Object> map = (Map<Object, Object>) obj;
            jg.writeStartObject();
            for (Object key : map.keySet()) {
                jg.writeFieldName(key.toString());
                final JsonViewWriter writer = new JsonViewWriter(filter, jg, sp);
                writer.serialize(map.get(key));
            }
            jg.writeEndObject();
        } else {
            jg.writeStartObject();
            Class clazz = obj.getClass();
            while (!clazz.equals(Object.class)) {
                for (Field field : clazz.getDeclaredFields()) {
                    try {
                        field.setAccessible(true);
                        if (filter.include(field, sp)) {
                            final String name = getFieldName(field);
                            jg.writeFieldName(name);
                            final JsonViewWriter writer = new JsonViewWriter(filter.subFilter(name), jg, sp);
                            writer.serialize(field.get(obj));
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }
                }
                clazz = clazz.getSuperclass();
            }

            jg.writeEndObject();
        }
    }


    static String getFieldName(Field field) {
        final JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isEmpty()) {
            return jsonProperty.value();
        } else {
            return field.getName();
        }
    }

}
