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

package com.examind.process.sos.csvcoriolis;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.sos.MeasureStringBuilder;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoriolisMeasureBuilder {
    
    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos.csvcoriolis");
            
     private final LinkedHashMap<Number, LinkedHashMap<String, Double>> mmb = new LinkedHashMap<>();
     
     private final String observationType;
     
     private final DateFormat sdf;
     
     private final List<String> sortedMeasureColumns;
     
     private final static Map<String, String> codesMeasure;
     
     static {
        codesMeasure = new HashMap<>();
        codesMeasure.put("30", "measure1");
        codesMeasure.put("35", "measure2");
        codesMeasure.put("66", "measure3");
        codesMeasure.put("70", "measure4");
        codesMeasure.put("64", "measure5");
        codesMeasure.put("65", "measure6");
        codesMeasure.put("169", "measure7");
        codesMeasure.put("193", "measure8");
        codesMeasure.put("577", "measure9");
        codesMeasure.put("584", "measure10");
    }
     
     public CoriolisMeasureBuilder(String observationType, DateFormat sdf, List<String> sortedMeasureColumns) {
         this.observationType = observationType;
         this.sdf = sdf;
         this.sortedMeasureColumns = sortedMeasureColumns;
     }
     
     public void parseLine(String value, Long millis, String measureCode, String mesureValue, int count, int valueColumnIndex) throws NumberFormatException, ParseException {
         Number mainValue;
         // assume that for profile main field is a double
        if ("Profile".equals(observationType)) {
            mainValue = Double.parseDouble(value);
            if (!mmb.containsKey(mainValue)) {
                LinkedHashMap<String, Double> row = new LinkedHashMap<>();
                for (String measure: sortedMeasureColumns) {
                    row.put(measure, Double.NaN);
                }
                mmb.put(mainValue, row);
            }
            
        // assume that is a date otherwise
        } else {
            // little optimization if date column == main column
            if (millis != null) {
                mainValue = millis;
            } else {
                mainValue = sdf.parse(value).getTime();
            }
            if (!mmb.containsKey(mainValue)) {
                LinkedHashMap<String, Double> row = new LinkedHashMap<>();
                for (String measure: sortedMeasureColumns) {
                    row.put(measure, Double.NaN);
                }
                mmb.put(mainValue, row);
            }
        }
        
        // add measure code
        try {
            String currentMeasureCodeLabel = codesMeasure.get(measureCode);
            if (currentMeasureCodeLabel != null && !currentMeasureCodeLabel.isEmpty() && sortedMeasureColumns.contains(currentMeasureCodeLabel)) {
                LinkedHashMap<String, Double> row = mmb.get(mainValue);

                row.put(currentMeasureCodeLabel, Double.parseDouble(mesureValue));
                mmb.put(mainValue, row);
            }
        } catch (NumberFormatException ex) {
            if (!mesureValue.isEmpty()) {
                LOGGER.warning(String.format("Problem parsing double value at line %d and column %d (value='%s')", count, valueColumnIndex, mesureValue));
            }
        }
     }
     
     public Set<String> getMeasureFromMap() {
        Set<String> result = new HashSet<>();
        for (Map.Entry<Number, LinkedHashMap<String, Double>> entry1: mmb.entrySet()) {
            for (Map.Entry<String, Double> entry2: entry1.getValue().entrySet()) {
                final String measureName = entry2.getKey();
                final Double measureValue = entry2.getValue();

                if (!measureValue.isNaN()) result.add(measureName);
            }
        }
        return result;
    }
     
     public MeasureStringBuilder buildMeasureStringBuilderFromMap(final Set<String> measureFound, final boolean isProfile) {
        MeasureStringBuilder result = new MeasureStringBuilder();
        boolean noneValue = true;

        List<Number> keys = new ArrayList<>(mmb.keySet());
        Collections.sort(keys, new MainColumnComparator());
        for (Number mainValue: keys) {
            if (isProfile) {
                result.appendValue((Double)mainValue);
            } else {
                result.appendDate((long)mainValue);
            }
            for (Map.Entry<String, Double> entry2: mmb.get(mainValue).entrySet()) {
                final String measureName = entry2.getKey();
                if (measureFound.contains(measureName)) {
                    final Double measureValue = entry2.getValue();
                    result.appendValue(measureValue);
                    noneValue = false;
                }
            }
            result.closeBlock();
        }
        if (noneValue) {
            return new MeasureStringBuilder();
        } else {
            return result;
        }
    }
     
     public void clear() {
         mmb.clear();
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
