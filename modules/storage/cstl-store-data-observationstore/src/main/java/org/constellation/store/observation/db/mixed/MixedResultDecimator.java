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
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.store.observation.db.OM2Utils.DEFAULT_TIME_FIELD;
import org.constellation.store.observation.db.decimation.DefaultResultDecimator;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.Field;
import static org.geotoolkit.observation.model.FieldDataType.QUANTITY;
import static org.geotoolkit.observation.model.FieldDataType.TIME;
import org.geotoolkit.observation.model.OMEntity;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MixedResultDecimator extends DefaultResultDecimator {
    
    // used for case like "only-main"
    private final Set<String> includedFields; 
    
    public MixedResultDecimator(List<Field> fields, boolean includeId, int width, List<Integer> fieldFilters, boolean includeTimeInProfile, ProcedureInfo procedure) {
        super(fields, includeId, width, fieldFilters, includeTimeInProfile, procedure);
        includedFields = fields.stream().map(f -> f.name).collect(Collectors.toSet());
    }
    
    @Override
    public void computeRequest(FilterSQLRequest sqlRequest, int fieldOffset, boolean firstFilter, Connection c) throws SQLException {

        final FilterSQLRequest fieldRequest = sqlRequest.clone();
        times = getMainFieldStep(fieldRequest, fields, c, width, OMEntity.RESULT, procedure);

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
    
    public static Map<Object, long[]> getMainFieldStep(FilterSQLRequest request, List<Field> measureFields, final Connection c, final int width, OMEntity objectType, ProcedureInfo proc) throws SQLException {
        final boolean getLoc  = OMEntity.HISTORICAL_LOCATION.equals(objectType);
        final Field mainField = getLoc ? DEFAULT_TIME_FIELD :  proc.mainField;
        final Boolean profile = getLoc ? null : "profile".equals(proc.type);
        if (getLoc) {
            request.replaceSelect("MIN(\"" + mainField.name + "\") as tmin, MAX(\"" + mainField.name + "\") as tmax, hl.\"procedure\" ");
            request.append(" GROUP BY hl.\"procedure\" order by hl.\"procedure\"");

        } else {
            if (profile) {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\"), m.\"time\" ");
                request.append(" GROUP BY m.\"time\"");
            } else {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\") ");
            }
        }
        LOGGER.fine(request.toString());
        try (final SQLResult rs = request.execute(c)) {
            // get the first for now
            int tableNum = rs.getFirstTableNumber();
            
            Map<Object, long[]> results = new LinkedHashMap<>();
            while (rs.next()) {
                final long[] result = {-1L, -1L};
                switch (mainField.dataType) {
                    case TIME -> {
                        final Timestamp minT = rs.getTimestamp(1, tableNum);
                        final Timestamp maxT = rs.getTimestamp(2, tableNum);
                        if (minT != null && maxT != null) {
                            final long min = minT.getTime();
                            final long max = maxT.getTime();
                            result[0] = min;
                            long step = (max - min) / width;
                            result[1] = step;
                        }
                    }
                    case QUANTITY -> {
                        final Double minT = rs.getDouble(1, tableNum);
                        final Double maxT = rs.getDouble(2, tableNum);
                        final long min    = minT.longValue();
                        final long max    = maxT.longValue();
                        result[0] = min;
                        long step = (max - min) / width;
                        result[1] = step;
                    }
                    default -> throw new SQLException("unable to extract bound from a " + mainField.dataType + " main field.");
                }
                final Object key;
                if (getLoc) {
                    key = rs.getString(3);
                } else {
                    if (profile) {
                        key = rs.getTimestamp(3, tableNum);
                    } else {
                        key = 1; // single in time series
                    }
                }
                results.put(key, result);
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0}", request.toString());
            throw ex;
        }
    }
    
    @Override
    public void processResults(SQLResult rs, int fieldOffset) throws SQLException, DataStoreException {
        if (values == null) {
            throw new DataStoreException("initResultBuilder(...) must be called before processing the results");
        }
        if (times == null) {
            throw new DataStoreException("computeRequest(...) must be called before processing the results");
        }
        StepValues mapValues = null;
        long start = -1;
        long step  = -1;
        Object prevObs = null;
        Date time = null;
        Date prevTime = null;
        AtomicInteger cpt = new AtomicInteger();
        AtomicBoolean first = new AtomicBoolean(true);
        
        Long previousMainValue = null;
        
        while (rs.nextOnField(procedure.mainField.name)) {
            final Object currentObs;
            if (profile) {
                currentObs = rs.getTimestamp("time");
            } else {
                currentObs = 1;
            }
            final Long mainValue   = extractMainValue(procedure.mainField, rs);
            final String fieldName = rs.getString("obsprop_id");
            final Double value     = rs.getDouble("result");
            time                   = dateFromTS(rs.getTimestamp("time"));
            
            if (!currentObs.equals(prevObs)) {
                if (prevObs != null) {
                    // append the last values
                    appendValue(prevTime, cpt, mapValues, first, true, fieldOffset);
                }

                first.set(true);
                step = times.get(currentObs)[1];
                start = times.get(currentObs)[0];
                mapValues = new StepValues(start, step, fields, profile);
            }
            prevObs = currentObs;
            
            // continue current block
            if (previousMainValue == null || mainValue.equals(previousMainValue)) {
                
                // handle current measure field
                if (includedFields.contains(fieldName)) {
                    mapValues.addToMapVal(mainValue, fieldName, value);
                }
            
            // end block
            } else {
                
                if (mainValue > (start + step)) {
                    appendValue(time, cpt, mapValues, first, false, fieldOffset);

                    // move to the next closest step
                    if (step > 0) {
                        while (mainValue >= start + step) {
                            start = start + step;
                        }
                    }
                    mapValues = new StepValues(start, step, fields, profile);
                }
                if (includedFields.contains(fieldName)) {
                    mapValues.addToMapVal(mainValue, fieldName, value);
                }
                
            }
            previousMainValue = mainValue;
            prevTime             = time;
        }

        // append the last values
        appendValue(time, cpt, mapValues, first, true, fieldOffset);
    }
    
    
   
    
    
}
