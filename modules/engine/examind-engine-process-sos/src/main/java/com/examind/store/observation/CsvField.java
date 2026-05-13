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
public class CsvField extends Field {

    /**
     * A csv field can be a concatenation of multiple csv column.
     */
    private final List<Integer> columnIndexes;
    
    public CsvField(int columnIndex, String name, FieldDataType dataType, FieldType type) {
        this(columnIndex, name, dataType, null, type, List.of(), List.of());
    }
    
    public CsvField(int columnIndex, String name, FieldDataType dataType, String uom, FieldType type, List<Field> qualityFields, List<Field> parameterFields) {
        super(-1, dataType, name, null, null, uom, type, qualityFields, parameterFields, new HashMap<>());
        this.columnIndexes = List.of(columnIndex);
    }
    
    public CsvField(List<Integer> columnIndexes, String name, FieldDataType dataType, String uom, FieldType type, List<Field> qualityFields, List<Field> parameterFields) {
        super(-1, dataType, name, null, null, uom, type, qualityFields, parameterFields, new HashMap<>());
        this.columnIndexes = columnIndexes;
    }
    
    public CsvField(int columnIndex, Field f) {
        super(f);
        this.columnIndexes = List.of(columnIndex);
        
        this.qualityFields.clear();
        if (f.getQualityFields() != null) {
            for (Field qf : f.getQualityFields()) {
                this.qualityFields.add(new CsvField(-1, qf));
            }
        }
        
        this.parameterFields.clear();
        if (f.getParameterFields() != null) {
            for (Field pf : f.getParameterFields()) {
                this.parameterFields.add(new CsvField(-1, pf));
            }
        }
    }
    
    public List<Integer> getColumnIndexes() {
        return columnIndexes;
    }
    
    public String getColumnIndexeRepresentation() {
        final String repres;
        if (columnIndexes.isEmpty()) {
            repres = "no column";
        } else if (columnIndexes.size() == 1) {
            repres   = columnIndexes.get(0).toString();
        } else {
            String value = columnIndexes.get(0).toString();
            for (int i = 1; i < columnIndexes.size(); i++) {
                value += "-" + columnIndexes.get(i).toString();
            }
            repres = value;
        }
        return repres;
    }

    /**
     * Mutable for flat-csv case.
     * 
     * @param label 
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Mutable for flat-csv case.
     * 
     * @param uom 
     */
    public void setUom(String uom) {
        this.uom = uom;
    }

    /**
     * Mutable for flat-csv case.
     * 
     * @param description 
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Mutable for flat-csv case.
     * @param properties 
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }
    
}
