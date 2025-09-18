/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.examind.fr
 *
 * Copyright 2025 Geomatys.
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
package org.constellation.store.observation.db.model;

import java.sql.SQLException;
import java.util.List;
import org.constellation.util.SQLResult;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal
 */
public class CompositeTextField extends SelectionField {

    public final String separator;
    
    public final List<SelectionField> components;

    // TODO isSelected should be coputed (if one of the component is selected so the composite)
    public CompositeTextField(Integer index, FieldDataType dataType, String name, String label, String description,FieldType type, boolean isSelected, String separator, List<SelectionField> components) {
        super(index, dataType, name, label, description, null, type, -1, isSelected);
        this.components = components != null ? components : List.of();
        this.separator = separator != null ? separator : "";
    }

    @Override
    public Object getValueFromResult(SQLResult rs) throws SQLException {
        return getValueFromResult(rs, tableNumber);
    }
    
    @Override
    public Object getValueFromResult(SQLResult rs, int tableNumber) throws SQLException {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (DbField component : components) {
            Object obj = component.getValueFromResult(rs);
            if (!first) {
                sb.append(separator);
            }
            sb.append(obj.toString());
            first = false;
        }
        return sb.toString();
    }

    @Override
    public String getSelection() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (SelectionField sf : components) {
            if (sf.isSelected) {
                if (!first) {
                    sb.append(" , ");
                }
                sb.append("\"" + sf.name + "\"");
            }
             first = false;
        }
        return sb.toString();
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("separator:").append(separator);
        sb.append("components:\n");
        for (DbField component : components) {
            sb.append(component).append("\n");
        }
        return sb.toString();
    }
}
