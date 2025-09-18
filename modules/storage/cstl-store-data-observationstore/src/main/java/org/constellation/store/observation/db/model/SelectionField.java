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

import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SelectionField extends DbField {

    public boolean isSelected;

    public SelectionField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber, boolean isSelected) {
        super(index, dataType, name, label, description, uom, type, tableNumber);
        this.isSelected = isSelected;
    }
    
    public SelectionField(DbField original, boolean isSelected) {
        super(original);
        this.isSelected = isSelected;
         // overide quality Fields type
        this.qualityFields.clear();
        for (Field qf : original.qualityFields) {
            if (qf instanceof DbField dqf) {
                SelectionField sqf = new SelectionField(dqf, isSelected);
                this.qualityFields.add(sqf);
                sqf.parent = this;
            }
        }
        // overide parameter Fields type
        this.parameterFields.clear();
        for (Field pf : original.parameterFields) {
            if (pf instanceof DbField dpf) {
                SelectionField spf = new SelectionField(dpf, isSelected);
                this.parameterFields.add(spf);
                spf.parent = this;
            }
        }
    }

    public String getSelection() {
        if (!isSelected) return "";
        StringBuilder selection = new StringBuilder();
        // only measure/quality/parameter field have aliases
        final String tableAlias = (type.equals(FieldType.MEASURE) || type.equals(FieldType.PARAMETER) || type.equals(FieldType.QUALITY)) ? (tableNumber == 1 ? "m." : "m" + tableNumber + ".") : "";
        final String columnName;
        if (parent != null) {
            columnName = parent.name + "_"  + type.name().toLowerCase()  + "_" + name;
        } else {
            columnName = name;
        }
        selection.append(tableAlias).append("\"").append(columnName).append("\"");
        
        for (Field qf : qualityFields) {
            if (qf instanceof SelectionField qsf) {
                selection.append(",");
                selection.append(qsf.getSelection());
            }
        }
        for (Field qf : parameterFields) {
            if (qf instanceof SelectionField qsf) {
                selection.append(",");
                selection.append(qsf.getSelection());
            }
        }
        return selection.toString(); 
    }
    
}
