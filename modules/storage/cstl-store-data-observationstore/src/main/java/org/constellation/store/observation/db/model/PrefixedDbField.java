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

import java.sql.ResultSet;
import java.sql.SQLException;
import org.constellation.util.SQLResult;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class PrefixedDbField extends SelectionField {

    public String prefix;

    public PrefixedDbField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber, boolean isSelected, String prefix) {
        super(index, dataType, name, label, description, uom, type, tableNumber, isSelected);
        this.prefix = prefix;
    }

    public void updatePrefix(String newPrefix) {
        this.prefix = newPrefix;
    }

    @Override
    public Object getValueFromResult(ResultSet rs) throws SQLException {
        Object obj = super.getValueFromResult(rs);
        if (obj instanceof String s) {
            return prefix + s;
        }
        throw new IllegalStateException("prefixed field should only be created on TEXT field");
    }
    
    @Override
    public Object getValueFromResult(SQLResult rs) throws SQLException {
        return getValueFromResult(rs, tableNumber);
    }
    
    @Override
    public Object getValueFromResult(SQLResult rs, int tableNumber) throws SQLException {
        Object obj = super.getValueFromResult(rs, tableNumber);
        if (obj instanceof String s) {
            return prefix + s;
        }
        throw new IllegalStateException("prefixed field should only be created on TEXT field");
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("prefix:").append(prefix);
        return sb.toString();
    }
    
}