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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * @author Legal Guilhem (Geomatys).
 * @version 0.9
 * @since 0.9
 */
@XmlRootElement
public class ParameterValues {

    private HashMap<String,String> values = new HashMap<>();

    public ParameterValues() {

    }

    public ParameterValues(final Map<String,String> values) {
        this.values = new HashMap<>(values);
    }

    public String get(final String key) {
        if (values != null) {
            return values.get(key);
        }
        return null;
    }
    /**
     * @return the values
     */
    public HashMap<String,String> getValues() {
        return values;
    }

    public Boolean getAsBoolean(final String paramName) {
        for (String key : values.keySet()) {
            if (key.equalsIgnoreCase(paramName)) {
                return Boolean.parseBoolean(values.get(key));
            }
        }
        return false;
    }

    /**
     * @param values the values to set
     */
    public void setValues(HashMap<String,String> values) {
        this.values = values;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("[ParameterValues]\n");
        if (values != null) {
            for (Entry<String, String> entry : values.entrySet()) {
                sb.append("key:").append(entry.getKey()).append(" => ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            ParameterValues that = (ParameterValues) obj;
            return Objects.equals(this.values, that.values);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.values);
        return hash;
    }
}
