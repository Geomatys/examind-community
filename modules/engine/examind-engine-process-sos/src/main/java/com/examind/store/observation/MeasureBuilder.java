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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.sos.MeasureStringBuilder;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MeasureBuilder {
    
    private static final Logger LOGGER = Logging.getLogger("com.examind.store.observation");
            
    private final LinkedHashMap<Number, LinkedHashMap<String, Double>> mmb = new LinkedHashMap<>();
     
    private final boolean isProfile;
     
    private final Map<String, String> measureColumns = new LinkedHashMap<>();

    private final String mainColumn;

    public MeasureBuilder(boolean isProfile, List<String> measureColumns, String mainColumn) {
        this.isProfile = isProfile;
        // initialize description
        for (String mc : measureColumns) {
            this.measureColumns.put(mc, mc);
        }
        this.mainColumn = mainColumn;
    }

    public MeasureBuilder(MeasureBuilder cmb, boolean isProfile) {
        this.isProfile = isProfile;
        this.measureColumns.putAll(cmb.measureColumns);
        this.mainColumn =  cmb.mainColumn;
    }
     
     public void appendValue(Number mainValue, String measureCode, Double measureValue, int lineNumber) {
         if (!mmb.containsKey(mainValue)) {
            LinkedHashMap<String, Double> row = new LinkedHashMap<>();
            for (String measure: measureColumns.keySet()) {
                row.put(measure, Double.NaN);
            }
            mmb.put(mainValue, row);
        }
        // add measure code
        if (measureCode != null && !measureCode.isEmpty() && measureColumns.keySet().contains(measureCode)) {
            LinkedHashMap<String, Double> row = mmb.get(mainValue);
            if (row.containsKey(measureCode) && !row.get(measureCode).isNaN()) {
                LOGGER.log(Level.FINE, "Duplicated value at line {0} and for main value {1} (value=''{2}'')", new Object[]{lineNumber, mainValue, measureValue});
            }
            row.put(measureCode, measureValue);
            mmb.put(mainValue, row);
        }
     }
     
     private Set<String> getMeasureFromMap() {
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

    public Map<String, String> getUsedMeasureColumns() {
        final Set<String> measureColumnFound = getMeasureFromMap();

        // On complète les champs de mesures seulement avec celles trouvées dans la donnée
        Map<String, String> filteredMeasure = new LinkedHashMap<>();
        if (isProfile) {
            filteredMeasure.put(mainColumn, mainColumn);
        }
        for (Entry<String, String> m : measureColumns.entrySet()) {
            if (measureColumnFound.contains(m.getKey())) {
                filteredMeasure.put(m.getKey(), m.getValue());
            }
        }
        return filteredMeasure;
    }

    public void updateObservedPropertyName(String observedProperty, String observedPropertyName) {
        if (measureColumns.containsKey(observedProperty)) {
            measureColumns.put(observedProperty, observedPropertyName);
        }
    }
     
     public MeasureStringBuilder buildMeasureStringBuilderFromMap() {
       final Set<String> measureColumnFound = getMeasureFromMap();
        MeasureStringBuilder result = new MeasureStringBuilder();
        boolean noneValue = true;

        List<Number> keys = new ArrayList<>(mmb.keySet());
        Collections.sort(keys, new MainColumnComparator());
        for (Number mainValue: keys) {
            // verify that the line is not all NAN
            boolean emptyLine = true;
            for (Map.Entry<String, Double> entry2: mmb.get(mainValue).entrySet()) {
                final String measureName = entry2.getKey();
                if (measureColumnFound.contains(measureName) && !entry2.getValue().isNaN()) {
                    emptyLine = false;
                    break;
                }
            }
            if (emptyLine) {
                continue;
            }
            
            // write the data line
            if (isProfile) {
                result.appendValue((Double)mainValue);
            } else {
                result.appendDate((long)mainValue);
            }
            for (Map.Entry<String, Double> entry2: mmb.get(mainValue).entrySet()) {
                final String measureName = entry2.getKey();
                if (measureColumnFound.contains(measureName)) {
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
