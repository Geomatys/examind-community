/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.examind.fr
 *
 * Copyright 2022 Geomatys.
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
import java.util.List;
import org.constellation.util.SQLResult;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import static org.geotoolkit.observation.model.FieldDataType.BOOLEAN;
import static org.geotoolkit.observation.model.FieldDataType.QUANTITY;
import static org.geotoolkit.observation.model.FieldDataType.TEXT;
import static org.geotoolkit.observation.model.FieldDataType.TIME;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DbField extends Field {

    public final int tableNumber;

    public DbField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber) {
        this(index, dataType, name, label, description, uom, type, tableNumber, List.of(), List.of());
    }
    
    public DbField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber,
            List<Field> qualityFields, List<Field> parameterFields) {
        super(index, dataType, name, label, description, uom, type, qualityFields, parameterFields);
        this.tableNumber = tableNumber;
    }
    
    public DbField(DbField original) {
        this(original, original.tableNumber);
    }

    public DbField(Field original, int tableNumber) {
        super(original);
        this.tableNumber = tableNumber;
        // overide quality Fields type
        this.qualityFields.clear();
        for (Field qField : original.qualityFields) {
            if (qField instanceof DbField dqField) {
                tableNumber = dqField.tableNumber;
            }
            DbField qf = new DbField(qField, tableNumber);
            this.qualityFields.add(qf);
            qf.parent = this;
        }
        // overide parameter Fields type
        this.parameterFields.clear();
        for (Field pField : original.parameterFields) {
            if (pField instanceof DbField dqField) {
                tableNumber = dqField.tableNumber;
            }
            DbField pf = new DbField(pField, tableNumber);
            this.parameterFields.add(pf);
            pf.parent = this;
        }
    }
    
    public Object getValueFromResult(ResultSet rs) throws SQLException {
        String fieldName;
        if (parent != null) {
            fieldName = parent.name + "_" + type.name().toLowerCase() + "_" + name;
        } else {
            fieldName = name;
        }
        return switch(this.dataType) {
            case BOOLEAN  -> rs.getBoolean(fieldName);
            case QUANTITY -> rs.getDouble(fieldName);
            case TIME     -> rs.getTimestamp(fieldName);
            case TEXT     -> rs.getString(fieldName);
            case JSON     -> OMUtils.readJsonMap(rs.getString(fieldName));
            default       -> rs.getString(fieldName);
        };
    }
    
    public Object getValueFromResult(SQLResult rs) throws SQLException {
        return getValueFromResult(rs, tableNumber);
    }
    
    public Object getValueFromResult(SQLResult rs, int tableNumber) throws SQLException {
        String fieldName;
        if (parent != null) {
            fieldName = parent.name + "_" + type.name().toLowerCase() + "_" + name;
        } else {
            fieldName = name;
        }
        return switch(this.dataType) {
            case BOOLEAN  -> rs.getBoolean(fieldName, tableNumber);
            case QUANTITY -> notNullDouble(rs, fieldName, tableNumber);
            case TIME     -> rs.getTimestamp(fieldName, tableNumber);
            case TEXT     -> rs.getString(fieldName, tableNumber);
            case JSON     -> OMUtils.readJsonMap(rs.getString(fieldName, tableNumber));
            default       -> rs.getString(fieldName, tableNumber);
        };
    }
    
    private Double notNullDouble(SQLResult rs, String fieldName, int tableNumber) throws SQLException {
        double d = rs.getDouble(fieldName, tableNumber);
        if (rs.wasNull(tableNumber)) return Double.NaN;
        return d;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("tableNumber:").append(tableNumber);
        return sb.toString();
    }
}