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

import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregationField extends SelectionField {

    public final Aggregation aggregation;

    public AggregationField(Integer index, FieldDataType dataType, String name, String label, String description, String uom, FieldType type, int tableNumber, Aggregation aggregation) {
        super(index, dataType, name, label, description, uom, type, tableNumber, true);
        this.aggregation = aggregation;
    }
    
    public AggregationField(DbField original, Aggregation aggregation) {
        super(original, true);
        this.aggregation = aggregation;
        // empty quality/parameter Fields type
        this.qualityFields.clear();
        this.parameterFields.clear();
    }

    @Override
    public String getSelection() {
        StringBuilder sb = new StringBuilder();
        final String tableAlias    = tableNumber == 1 ? "m" : "m" + tableNumber;
        sb.append(" " + aggregation.name() + "(" + tableAlias + ".\"" + name + "\") as " + name);
        return sb.toString();
    }
    
    

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("Aggregation:").append(aggregation);
        return sb.toString();
    }
    
}
