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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.sos.MeasureStringBuilder;

import static com.examind.process.sos.csvcoriolis.CsvCoriolisObservationStoreUtils.parseDouble;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CoriolisMeasureBuilder {
    
    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos.csvcoriolis");
            
    private final LinkedHashMap<Number, LinkedHashMap<String, Double>> mmb = new LinkedHashMap<>();
     
    private final boolean isProfile;
     
    private final DateFormat sdf;
     
    private final List<String> sortedMeasureColumns;

    private final String mainColumn;

    public CoriolisMeasureBuilder(boolean isProfile, DateFormat sdf, List<String> sortedMeasureColumns, String mainColumn) {
        this.isProfile = isProfile;
        this.sdf = sdf;
        this.sortedMeasureColumns = sortedMeasureColumns;
        this.mainColumn = mainColumn;
    }

    public CoriolisMeasureBuilder(CoriolisMeasureBuilder cmb) {
        this.isProfile = cmb.isProfile;
        this.sdf =  cmb.sdf;
        this.sortedMeasureColumns =  cmb.sortedMeasureColumns;
        this.mainColumn =  cmb.mainColumn;
    }
     
     public void parseLine(String value, Long millis, String measureCode, String mesureValue, int lineNumber, int valueColumnIndex) throws NumberFormatException, ParseException {
         Number mainValue;
         // assume that for profile main field is a double
        if (isProfile) {
            mainValue = parseDouble(value);
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
            if (measureCode != null && !measureCode.isEmpty() && sortedMeasureColumns.contains(measureCode)) {
                LinkedHashMap<String, Double> row = mmb.get(mainValue);
                if (row.containsKey(measureCode) && !row.get(measureCode).isNaN()) {
                    LOGGER.warning(String.format("Duplicated value at line %d and for main value %s (value='%s')", lineNumber, value, mesureValue));
                }
                row.put(measureCode, parseDouble(mesureValue));
                mmb.put(mainValue, row);
            }
        } catch (NumberFormatException ex) {
            if (!mesureValue.isEmpty()) {
                LOGGER.warning(String.format("Problem parsing double value at line %d and column %d (value='%s')", lineNumber, valueColumnIndex, mesureValue));
            }
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

     public List<String> getFilteredMeasure() {
         final Set<String> measureColumnFound = getMeasureFromMap();

        // On complète les champs de mesures seulement avec celles trouvées dans la donnée
        List<String> filteredMeasure = new ArrayList<>();
        if (isProfile)  filteredMeasure.add(mainColumn);
        for (String m: sortedMeasureColumns) {
            if (measureColumnFound.contains(m)) filteredMeasure.add(m);
        }
        return filteredMeasure;
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
     
    @Override
    public CoriolisMeasureBuilder clone() {
        return new CoriolisMeasureBuilder(this);
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
