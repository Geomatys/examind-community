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

import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DbField extends Field {

    public final int tableNumber;

    public DbField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber) {
        super(index, dataType, name, label, description, uom, type);
        this.tableNumber = tableNumber;
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
            this.qualityFields.add(new DbField(qField, tableNumber));
        }
        // overide parameter Fields type
        this.parameterFields.clear();
        for (Field pField : original.parameterFields) {
            if (pField instanceof DbField dqField) {
                tableNumber = dqField.tableNumber;
            }
            this.parameterFields.add(new DbField(pField, tableNumber));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("tableNumber:").append(tableNumber);
        return sb.toString();
    }
}
