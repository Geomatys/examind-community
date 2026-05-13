/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.geotoolkit.observation.result.ResultBuilder;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MeasureBuilder {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");
            
    private final Map<Number, LinkedHashMap<String, MeasureValue>> measureMemoryMap = new LinkedHashMap<>();
     
    private final FieldInfos fieldInfos;

    public MeasureBuilder(FieldInfos fieldInfos) {
        if (fieldInfos.measureFields == null || fieldInfos.measureFields.isEmpty()) throw new IllegalArgumentException("measures columns should not be null or empty");
        
        this.fieldInfos = fieldInfos; 
    }
    
    public void appendProfileTime(Number mainValue, long millis) {
        if (!measureMemoryMap.containsKey(mainValue)) {
            measureMemoryMap.put(mainValue, new LinkedHashMap<>());
        }
        // add measure code
        if (fieldInfos.containsMeasureField("time")) {
            LinkedHashMap<String, MeasureValue> row = measureMemoryMap.get(mainValue);
            row.put("time", new MeasureValue(millis, new Object[0], new Object[0]));
            measureMemoryMap.put(mainValue, row);
        }
    }

    public void appendValue(Number mainValue, String measureCode, MeasureValue measureValue, int lineNumber) {
        if (measureCode == null || measureCode.isEmpty()) return;
        
        measureMemoryMap.computeIfAbsent(mainValue, k -> new LinkedHashMap<>());
        
        // add measure code
        if (fieldInfos.containsMeasureField(measureCode)) {
            LinkedHashMap<String, MeasureValue> row = measureMemoryMap.get(mainValue);
            if (row.containsKey(measureCode) && !row.get(measureCode).isNaN) {
                LOGGER.log(Level.FINE, "Duplicated value at line {0} and for main value {1} (value=''{2}'')", new Object[]{lineNumber, mainValue, measureValue});
            }
            row.put(measureCode, measureValue);
            measureMemoryMap.put(mainValue, row);
        }
    }
     
     private Set<String> getMeasureFieldInMap() {
        Set<String> result = new HashSet<>();
        for (Map.Entry<Number, LinkedHashMap<String, MeasureValue>> entry1: measureMemoryMap.entrySet()) {
            for (Map.Entry<String, MeasureValue> entry2: entry1.getValue().entrySet()) {
                final String measureName = entry2.getKey();
                final MeasureValue measureValue = entry2.getValue();

                if (!measureValue.isNaN) result.add(measureName);
            }
        }
        return result;
    }

    public Set<Field> getUsedFields() {
        final Set<String> measureColumnFound = getMeasureFieldInMap();

        //we complete the measure field only with those found in the data
        Set<Field> filteredMeasure = new LinkedHashSet<>();
        
        for (Field field : fieldInfos.measureFields) {
           if (FieldType.MAIN.equals(field.getType())     ||
               FieldType.METADATA.equals(field.getType()) || 
               measureColumnFound.contains(field.getName())) {
                filteredMeasure.add(field);
            }
        }
        return filteredMeasure;
    }
    
    public void updateObservedProperty(ObservedProperty observedProperty) {
        Field field = fieldInfos.getFieldByName(observedProperty.id);
        if (field instanceof CsvField cField) {
            cField.setLabel(observedProperty.name);
            cField.setUom(observedProperty.uom);
            cField.setDescription(observedProperty.description);
            cField.setProperties(observedProperty.properties);
        } else {
            throw new IllegalStateException("we are expecting a CSV field here");
        }
    }
    
    public ResultBuilder buildMeasureStringBuilderFromMap(ResultMode resultMode) {
        ResultBuilder result = new ResultBuilder(resultMode, TextEncoderProperties.DEFAULT_ENCODING, false);
        return buildMeasureStringBuilderFromMap(result, false);
    }

    public ResultBuilder buildMeasureStringBuilderFromMap(ResultBuilder result, boolean fillEmptyFields) {
       final Set<String> measureColumnFound = getMeasureFieldInMap();
        boolean noneValue = true;

        List<Number> keys = new ArrayList<>(measureMemoryMap.keySet());
        Collections.sort(keys, new MainColumnComparator());
        for (Number mainValue: keys) {
            // verify that the line is not all NAN
            boolean emptyLine = measureMemoryMap.get(mainValue).isEmpty();
            if (emptyLine) {
                continue;
            }
            
            // write the data line
            result.newBlock();
            
            Map<String, MeasureValue> measures = measureMemoryMap.get(mainValue);
            for (Field field : fieldInfos.measureFields) {
                 // write main field
                if (FieldType.MAIN.equals(field.getType())) {
                    if (fieldInfos.isProfile) {
                        result.appendDouble((Double)mainValue, false, null);
                    } else {
                        result.appendTime((long)mainValue, false, null);
                    }
                    
                 // write metadata fields
                 // identifier TODO
                } else if (FieldType.METADATA.equals(field.getType()) && FieldDataType.TEXT.equals(field.getDataType())) {
                    result.appendString("todo", false, null);
                    
                // profile time    
                } else if (FieldType.METADATA.equals(field.getType()) && FieldDataType.TIME.equals(field.getDataType())) {
                    final MeasureValue measure = measures.get(field.getName());
                    result.appendTime((long)measure.value, false, null);
                 
                // write measure field
                } else if (measureColumnFound.contains(field.getName())) {
                    final MeasureValue measure = measures.get(field.getName());
                    
                    if (measure != null) {
                        result.appendValue(measure.value, true, null);
                        for (Object qValue : measure.qualityValues) {
                            result.appendValue(qValue, false, null);
                        }
                        for (Object pValue : measure.parameterValues) {
                            result.appendValue(pValue, false, null);
                        }
                        noneValue = false;
                    } else {
                        result.appendDouble(Double.NaN, true, null);
                        for (Field qf : field.getQualityFields()) {
                            result.appendString(null, false, null);
                        }
                        for (Field pf : field.getParameterFields()) {
                            result.appendString(null, false, null);
                        }
                    }
                } else if (fillEmptyFields) {
                    result.appendDouble(Double.NaN, true, null);
                }
            }
            result.endBlock();
        }
        if (noneValue) {
            result.clear();
        }
        return result;
    }

    public int getMeasureCount() {
        return measureMemoryMap.size();
    }

    private static class MainColumnComparator implements Comparator<Number> {

       @Override
       public int compare(Number o1, Number o2) {
           if (o1 instanceof Double && o2 instanceof Double) {
               return ((Double)o1).compareTo((Double) o2);
           }
           if (o1 instanceof Long && o2 instanceof Long) {
               return ((Long)o1).compareTo((Long) o2);
           }
           throw new IllegalArgumentException("Unexpected Main value type");
       }

    }
    
}
