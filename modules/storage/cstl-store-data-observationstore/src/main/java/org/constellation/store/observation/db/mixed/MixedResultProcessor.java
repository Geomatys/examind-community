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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.sis.storage.DataStoreException;
import org.constellation.store.observation.db.ResultProcessor;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MixedResultProcessor extends ResultProcessor {
    
    // used for profile case like "only-main" or "no-main"
    private final Set<String> includedFields; 
    private boolean onlyMain;
    private boolean mainIncluded;
    
    public MixedResultProcessor(List<Field> fields, boolean includeId, boolean includeQuality, boolean includeParameter, boolean includeTimeInProfile, ProcedureInfo procedure, String idSuffix) {
        super(fields, includeId, includeQuality, includeParameter, includeTimeInProfile, procedure, idSuffix);
        includedFields = fields.stream().map(f -> f.name).collect(Collectors.toSet());
        if (profile) {
            mainIncluded = mainFieldIndex != -1;
            onlyMain = true;
            for (Field field : fields) {
                if (field.index > mainFieldIndex + 1) {
                    onlyMain = false;
                    break;
                }
            }
        } else {
            onlyMain = false;
            mainIncluded = true;
        }
    }
    
    @Override
    public void computeRequest(FilterSQLRequest sqlRequest, int fieldOffset, boolean firstFilter, Connection c) throws SQLException {
        String mainFieldSelect = "m.\"" + procedure.mainField.name + "\"";
        StringBuilder select  = new StringBuilder(mainFieldSelect);
        StringBuilder orderBy = new StringBuilder(" ORDER BY ");
        if (profile) {
            if (includeTimeInProfile) {
                select.append(", m.\"time\" ");
            }
            orderBy.append(" m.\"time\", ");
        }
        // always order by main field
        orderBy.append("\"").append(procedure.mainField.name).append("\"");
        
        sqlRequest.replaceFirst(mainFieldSelect, select.toString());
        sqlRequest.append(orderBy.toString());

        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
    }
    
    @Override
    public void processResults(SQLResult rs, int fieldOffset) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        
        Map<String, Object> blocValues = createNewBlocValues();
        Object previousKey             = null;
        boolean hasData                = false;
        while (rs.nextOnField(procedure.mainField.name)) {
            final Object mainValue = switch (procedure.mainField.dataType) {
                case TIME     -> rs.getTimestamp(procedure.mainField.name);
                case QUANTITY -> rs.getDouble(procedure.mainField.name);
                default       -> throw new DataStoreException("Unexpected main field type");
            };
            
            final String fieldName = rs.getString("obsprop_id");
            final Double value     = rs.getDouble("result");
            
            // observations for profile are a combination of the time and the z_value
            Object mainKey;
            if (profile) {
                long time = rs.getTimestamp("time").getTime();
                mainKey = time + '-' + mainValue.toString();
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
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if (includeId && f.name.equals("id")) {
                        values.appendString("urn:ogc:object:observation:GEOM:" + procedure.pid + idSuffix + '-' + rs.getLong("id"), false, f);
                    } else if (f.dataType.equals(FieldDataType.TIME) && profile) {
                        values.appendTime(dateFromTS(rs.getTimestamp("time")), false, f);
                    }
                }
                // handle main field
                if (mainIncluded) {
                    values.appendValue(mainValue, onlyMain, procedure.mainField);
                }
                
                // handle current measure field
                if (includedFields.contains(fieldName)) {
                    blocValues.put(fieldName, value);
                }
                
            // continue line
            } else {
                // handle current measure field
                if (includedFields.contains(fieldName)) {
                    blocValues.put(fieldName, value);
                }
            }
            previousKey    = mainKey;
        }
        // close last block if any
        if (hasData) {
            endBlock(blocValues);
        }
    }
    
    private void endBlock(Map<String, Object> blocValues) {
        for (Object value : blocValues.values()) {
            values.appendValue(value, true, null); // null field is an issue?
        }
        values.endBlock();
    }
    
    private Map<String, Object> createNewBlocValues() {
        Map<String, Object> results = new LinkedHashMap<>();
        // exclude non measure fields
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            if (!((includeId && f.name.equals("id"))          || // id field
                  (f.dataType.equals(FieldDataType.TIME) && profile)  || // time field fr profile
                  (mainFieldIndex == i))) {                        // main field
                results.put(fields.get(i).name, null);
            } 
        }
        return results;
    }
}
