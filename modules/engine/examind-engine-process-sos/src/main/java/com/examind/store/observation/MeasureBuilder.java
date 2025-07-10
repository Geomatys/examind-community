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
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.geotoolkit.observation.result.ResultBuilder;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MeasureBuilder {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");
            
    private final Map<Number, LinkedHashMap<String, Measure>> mmb = new LinkedHashMap<>();
     
    private final boolean isProfile;
     
    private final Map<String, MeasureField> measureColumns = new LinkedHashMap<>();

    private final List<String> mainColumns;

    private static class Measure {
        public final Object value;
        public final Object[] qualityValues;
        public final Object[] parameterValues;

        public Measure(Object value, Object[] qualityValues, Object[] parameterValues) {
            this.value = value;
            this.qualityValues = qualityValues;
            this.parameterValues = parameterValues;
        }

        public boolean isNaN() {
            if (value instanceof Double d) {
                return Double.isNaN(d);
            } else if (value instanceof String s ) {
                return s.isBlank();
            } else {
                return value == null;
            }
        }
    }

    public MeasureBuilder(FileParsingObservationStore.MeasureColumns measColumns) {
        if (measColumns.mainColumns == null   || measColumns.mainColumns.isEmpty())   throw new IllegalArgumentException("mains columns should not be null or empty");
        if (measColumns.measureFields == null || measColumns.measureFields.isEmpty()) throw new IllegalArgumentException("measures columns should not be null or empty");
        this.isProfile = measColumns.isProfile;
        for (MeasureField mf : measColumns.measureFields) {
            this.measureColumns.put(mf.name, mf);
        }
        this.mainColumns = measColumns.mainColumns;
    }

    public MeasureBuilder(MeasureBuilder cmb, boolean isProfile) {
        this.isProfile = isProfile;
        this.measureColumns.putAll(cmb.measureColumns);
        this.mainColumns =  new ArrayList<>(cmb.mainColumns);
    }
     
    public void appendValue(Number mainValue, String measureCode, Object measureValue, int lineNumber, Object[] qualityValues, Object[] parameterValues) {
        if (!mmb.containsKey(mainValue)) {
            mmb.put(mainValue, new LinkedHashMap<>());
        }
        // add measure code
        if (measureCode != null && !measureCode.isEmpty() && measureColumns.keySet().contains(measureCode)) {
            LinkedHashMap<String, Measure> row = mmb.get(mainValue);
            if (row.containsKey(measureCode) && !row.get(measureCode).isNaN()) {
                LOGGER.log(Level.FINE, "Duplicated value at line {0} and for main value {1} (value=''{2}'')", new Object[]{lineNumber, mainValue, measureValue});
            }
            row.put(measureCode, new Measure(measureValue, qualityValues, parameterValues));
            mmb.put(mainValue, row);
        }
    }
     
     private Set<String> getMeasureFromMap() {
        Set<String> result = new HashSet<>();
        for (Map.Entry<Number, LinkedHashMap<String, Measure>> entry1: mmb.entrySet()) {
            for (Map.Entry<String, Measure> entry2: entry1.getValue().entrySet()) {
                final String measureName = entry2.getKey();
                final Measure measureValue = entry2.getValue();

                if (!measureValue.isNaN()) result.add(measureName);
            }
        }
        return result;
    }

    public Set<MeasureField> getUsedMeasureColumns() {
        final Set<String> measureColumnFound = getMeasureFromMap();

        //we complete the measure field only with those found in the data
        Set<MeasureField> filteredMeasure = new LinkedHashSet<>();
        if (isProfile) {
            if (mainColumns.size() > 1) {
                throw new IllegalArgumentException("Multiple main columns is not yet supported for Profile");
            }
            filteredMeasure.add(new MeasureField(-1, mainColumns.get(0), FieldDataType.QUANTITY, List.of(), List.of()));
        }
        for (Entry<String, MeasureField> m : measureColumns.entrySet()) {
            if (measureColumnFound.contains(m.getKey())) {
                filteredMeasure.add(m.getValue());
            }
        }
        return filteredMeasure;
    }

    public void updateObservedProperty(ObservedProperty observedProperty) {
        MeasureField field = measureColumns.get(observedProperty.id);
        if (field != null) {
            field.label       = observedProperty.name;
            field.uom         = observedProperty.uom;
            field.description = observedProperty.description;
            field.properties  = observedProperty.properties;
        }
    }

     public ResultBuilder buildMeasureStringBuilderFromMap(ResultMode resultMode) {
       final Set<String> measureColumnFound = getMeasureFromMap();
        ResultBuilder result = new ResultBuilder(resultMode, TextEncoderProperties.DEFAULT_ENCODING, false);
        boolean noneValue = true;

        List<Number> keys = new ArrayList<>(mmb.keySet());
        Collections.sort(keys, new MainColumnComparator());
        for (Number mainValue: keys) {
            // verify that the line is not all NAN
            boolean emptyLine = mmb.get(mainValue).isEmpty();
            if (emptyLine) {
                continue;
            }
            
            // write the data line
            result.newBlock();
            if (isProfile) {
                result.appendDouble((Double)mainValue, false, null);
            } else {
                result.appendTime((long)mainValue, false, null);
            }
            Map<String, Measure> measures = mmb.get(mainValue);
            for (Entry<String,MeasureField> measureField: measureColumns.entrySet()) {
                if (measureColumnFound.contains(measureField.getKey())) {
                    final Measure measure = measures.get(measureField.getKey());
                    
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
                        MeasureField f = measureField.getValue();
                        result.appendValue(null, true, null);
                        for (MeasureField qf : f.qualityFields) {
                            result.appendString(null, false, null);
                        }
                        for (MeasureField pf : f.parameterFields) {
                            result.appendString(null, false, null);
                        }
                    }
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
        return mmb.size();
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
