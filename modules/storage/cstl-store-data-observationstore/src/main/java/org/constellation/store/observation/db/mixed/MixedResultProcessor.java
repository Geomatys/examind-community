/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.store.observation.db.mixed;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.ResultProcessor;
import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.SQLResult;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MixedResultProcessor extends ResultProcessor {
    
    // used for nonTimeseries case like "only-main" or "no-main"
    private final Map<String, Field> includedFields; 
    private final boolean onlyMain;
    private final boolean mainIncluded;
    
    public MixedResultProcessor(List<? extends DbField> fields, boolean includeQuality, boolean includeParameter, ProcedureInfo procedure) {
        super(fields, includeQuality, includeParameter, procedure);
        includedFields = new HashMap<>();
        fields.forEach(f -> includedFields.put(f.name, f));
        mainIncluded = fields.stream().anyMatch(f -> f.type.equals(FieldType.MAIN));
        if (nonTimeseries) {
            if (mainIncluded) {
                onlyMain = fields.stream().noneMatch(f -> f.type.equals(FieldType.MEASURE));
            } else {
                onlyMain = false;
            }
        } else {
            onlyMain = false;
        }
    }
    
    @Override
    public void processResults(SQLResult rs) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        
        Map<Field, Object> blocValues = createNewBlocValues();
        Object previousKey             = null;
        boolean hasData                = false;
        DbField mainField;
        if (mainIncluded) {
            mainField  = fields.stream().filter(f -> f.type.equals(FieldType.MAIN)).findFirst().orElse(null);
        } else {
            mainField = procedure.mainField;
        }
        
        while (rs.nextOnField(mainField.name)) {
            // in some case like aggregation, the column time, will not be available int the resultset
            // this is not important if we only have one observation to extract
            Object mainValue;
            try {
                mainValue = mainField.getValueFromResult(rs);
            } catch (SQLException ex) {
                if (!mainIncluded) {
                    LOGGER.log(Level.FINE, "No main field available in mixed mode resultset", ex);
                    mainValue = "null";
                } else {
                    throw ex;
                }
            }
            
            final String fieldName = rs.getString("obsprop_id");
            final Double value     = rs.getDouble("result");
            
            // observations for nonTimeseries are a combination of the time and the z_value
            Object mainKey;
            if (nonTimeseries) {
                Object time;
                // in some case like aggregation, the column time, will not be available int the resultset
                // this is not important if we only have one observation to extract
                try {
                    time = rs.getTimestamp("time").getTime();
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "No time field available in mixed mode resultset", ex);
                    time = "null";
                }
                mainKey = time.toString() + '-' + mainValue.toString();
            } else {
                mainKey = mainValue;
            }
            
            // start new line
            if (!Objects.equals(mainKey, previousKey)) {
                
                // close previous block
                if (previousKey != null) {
                    endBlock(blocValues);
                    blocValues = createNewBlocValues();
                }
                
                values.newBlock();
                hasData = true;
                // handle non measure fields
                for (Field f : fields) {
                    if (f.type.equals(FieldType.METADATA) && f instanceof DbField df) {
                        Object t = df.getValueFromResult(rs);
                        values.appendValue(t, false, f);
                    }
                }
                // handle main field
                if (mainIncluded) {
                    values.appendValue(mainValue, onlyMain, procedure.mainField);
                }
                
                // handle current measure field
                if (includedFields.containsKey(fieldName)) {
                    blocValues.put(includedFields.get(fieldName), value);
                }
                
            // continue line
            } else {
                // handle current measure field
                if (includedFields.containsKey(fieldName)) {
                    blocValues.put(includedFields.get(fieldName), value);
                }
            }
            previousKey    = mainKey;
        }
        // close last block if any
        if (hasData) {
            endBlock(blocValues);
        }
    }
    
    private void endBlock(Map<Field, Object> blocValues) {
        for (Entry<Field, Object> entry : blocValues.entrySet()) {
            values.appendValue(entry.getValue(), true, entry.getKey()); // null field is an issue?
        }
        values.endBlock();
    }
    
    private Map<Field, Object> createNewBlocValues() {
        Map<Field, Object> results = new LinkedHashMap<>();
        // exclude non measure fields
        for (Field f : fields) {
            if (f.type.equals(FieldType.MEASURE)) {
                results.put(f, null);
            }
        }
        return results;
    }
}
