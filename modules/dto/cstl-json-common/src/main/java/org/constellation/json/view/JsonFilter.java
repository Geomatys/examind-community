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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static org.constellation.json.view.JsonViewWriter.getFieldName;

/**
 * Hold the list of fields and sub-fields to serialize.
 * 
 * @author Johann Sorel (Geomatys)
 */
public class JsonFilter {
    
    private static final JsonFilter EMPTY = new JsonFilter();
    private final Map<String,JsonFilter> fields = new HashMap<>();
    
    public JsonFilter(){
    }
    
    /**
     * Add field names to include in serialization.
     * 
     * @param includes 
     */
    public void put(String ... includes) {
        
        for (String name : includes) {
            String[] parts = name.split("\\.");
            Map<String,JsonFilter> m = fields;
            for (String p : parts) {
                JsonFilter sub = m.get(p);
                if (sub==null){
                    sub = new JsonFilter();
                    m.put(p, sub);
                }
                m = sub.fields;
            }
        }
    }
    
    public JsonFilter subFilter(String propertyName){
        final JsonFilter filter = fields.get(propertyName);
        return filter==null ? EMPTY : filter;
    }
    
    /**
     * Returns true if field should be serialized.
     * 
     * @param field
     * @param sp
     * @return true if field should be serialized
     */
    public boolean include(Field field, SerializerProvider sp) {
        if (isIgnored(field)){
           return false; 
        }
        
        String name = getFieldName(field);
        if (Modifier.isStatic(field.getModifiers())) {
            return false;
        } else if(isIgnored(field)){
            return false;
        }

        return fields.isEmpty() || fields.keySet().contains(name);
    }
    
    /**
     * Check if field is marked as ignored by annotations.
     */
    private boolean isIgnored(Field f) {
        final JsonIgnore ignore = f.getAnnotation(JsonIgnore.class);
        if (ignore!=null && ignore.value()){
            return true;
        }
        
        final String fieldName = getFieldName(f);
        final JsonIgnoreProperties classIgnoreProperties = f.getDeclaringClass().getAnnotation(JsonIgnoreProperties.class);
        if (classIgnoreProperties!=null && Arrays.asList(classIgnoreProperties.value()).contains(fieldName) ){
            return true;
        }
        
        return false;
    }
        
}
