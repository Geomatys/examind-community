/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.store.observation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MeasureField {

    public final int columnIndex;
    public final String name;
    public final FieldDataType dataType;
    public final FieldType type;
    public final List<MeasureField> qualityFields;
    public final List<MeasureField> parameterFields;
    
    /*
    * these attribute will be updated after the creation.
    */
    public String label;
    public String uom;
    public String description;
    public Map<String, Object> properties;

    public MeasureField(int columnIndex, String name, FieldDataType dataType, FieldType type) {
        this(columnIndex, name, dataType, List.of(), List.of(), type);
    }
    
    public MeasureField(int columnIndex, String name, FieldDataType dataType, List<MeasureField> qualityFields, List<MeasureField> parameterFields, FieldType type) {
        this.columnIndex = columnIndex;
        this.name = name;
        this.dataType = dataType;
        this.qualityFields = qualityFields;
        this.parameterFields = parameterFields;
        this.type = type;
    }
    
    public MeasureField(int columnIndex, Field f) {
        this.columnIndex = columnIndex;
        this.name = f.name;
        this.dataType = f.dataType;
        this.type = f.type;
        
        this.qualityFields = new ArrayList<>();
        if (f.qualityFields != null) {
            for (Field qf : f.qualityFields) {
                this.qualityFields.add(new MeasureField(-1, qf));
            }
        }
        
        this.parameterFields = new ArrayList<>();
        if (f.parameterFields != null) {
            for (Field pf : f.parameterFields) {
                this.parameterFields.add(new MeasureField(-1, pf));
            }
        }
        this.label = f.label;
        this.uom = f.uom;
        this.description = f.description;
        this.properties = new HashMap<>();
    }
}
