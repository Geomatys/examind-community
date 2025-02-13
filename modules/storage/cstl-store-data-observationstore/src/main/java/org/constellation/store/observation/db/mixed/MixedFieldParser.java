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
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static org.constellation.api.CommonConstants.COMPLEX_OBSERVATION;
import org.constellation.store.observation.db.FieldParser;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import static org.geotoolkit.observation.model.FieldType.QUANTITY;
import static org.geotoolkit.observation.model.FieldType.TIME;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResultMode;
import static org.geotoolkit.observation.model.ResultMode.CSV;
import static org.geotoolkit.observation.model.ResultMode.DATA_ARRAY;
import org.geotoolkit.observation.model.SamplingFeature;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;
import org.geotoolkit.observation.result.ResultBuilder;
import org.opengis.temporal.TemporalPrimitive;

/**
 *
 * @author glegal
 */
public class MixedFieldParser extends FieldParser {
    
    private final Set<String> includedFields; 
    
    public MixedFieldParser(int mainFieldIndex, List<Field> fields, ResultMode resultMode, boolean profileWithTime, boolean includeID, boolean includeQuality, boolean includeParameter, String obsName, int fieldOffset) {
        super(mainFieldIndex, fields, new ResultBuilder(resultMode, DEFAULT_ENCODING, false), profileWithTime, includeID, includeQuality, includeParameter, obsName, fieldOffset);
        includedFields = fields.stream().map(f -> f.name).collect(Collectors.toSet());
    }
    
    @Override
    public Map<String, Observation> parseSingleMeasureObservation(SQLResult rs2, long oid, final ProcedureInfo pti, final Procedure proc, final SamplingFeature feature, final Phenomenon phen) throws SQLException {
        final Map<String, Observation> observations = new LinkedHashMap<>();
        final Map<String, Object> properties = new HashMap<>();
        properties.put("type", pti.type);
        final boolean profile = "profile".equals(pti.type);
        int mainFieldIndex = fields.indexOf(pti.mainField);
        Object previousKey    = null;
        Long previousMeasureId = null;
        Map<String, Object> blocValues = createNewBlocValues(profile, mainFieldIndex);
        
        while (rs2.nextOnField(pti.mainField.name)) {
            
            final Object mainValue = switch (pti.mainField.type) {
                case TIME     -> rs2.getTimestamp(pti.mainField.name);
                case QUANTITY -> rs2.getDouble(pti.mainField.name);
                default       -> throw new SQLException("Unexpected main field type");
            };
            
            final String fieldName = rs2.getString("obsprop_id");
            final Double value     = rs2.getDouble("result");
            final Long measureId   = rs2.getLong("id");
            final Timestamp time   = rs2.getTimestamp("time");
            
            // observations for profile are a combination of the time and the z_value
            Object mainKey;
            if (profile) {
                mainKey = time.getTime() + '-' + mainValue.toString();
            } else {
                mainKey = mainValue;
            }
            
            // start new line
            if (!Objects.equals(mainKey, previousKey)) {
                
                // close previous block
                if (previousKey != null) {
                    Observation obs =  endBlock(blocValues, oid, previousMeasureId, proc, feature, phen, properties);
                    observations.put(pti.procedureId + '-' + obsName + '-' + previousMeasureId, obs);
                    blocValues = createNewBlocValues(profile, mainFieldIndex);
                }
                
                values.newBlock();
                // handle non measure fields
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if (includeID && f.name.equals("id")) {
                        values.appendString(obsName + '-' + measureId, false, f);
                    } else if (f.type.equals(FieldType.TIME) && profileWithTime) {
                        values.appendTime(dateFromTS(time), false, f);
                    }
                }
                // handle main field
                //if (mainIncluded) {
                    values.appendValue(mainValue, false, pti.mainField);
                //}
                
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
            previousMeasureId = measureId;
        }
        // close last block
        Observation obs = endBlock(blocValues, oid, previousMeasureId, proc, feature, phen, properties);
        observations.put(pti.procedureId + '-' + obsName + '-' + previousMeasureId, obs);
        
        return observations;
    }
    
    public void endBlock(Map<String, Object> blocValues) {
        for (Object value : blocValues.values()) {
            values.appendValue(value, true, null); // null field is an issue?
        }
        values.endBlock();
    }
    
    public Observation endBlock(Map<String, Object> blocValues, long oid, long measureID, final Procedure proc, final SamplingFeature feature, final Phenomenon phen, final Map<String, Object> properties) {
       endBlock(blocValues);
        
       final String singleObsID              = "obs-" + oid + '-' + measureID;
       final TemporalPrimitive time = buildTime(singleObsID, lastTime != null ? lastTime : firstTime, null);
       final ComplexResult result            = buildComplexResult();
       final String singleName               = obsName + '-' + measureID;
       clear();
       return new Observation(singleObsID,
                                     singleName,
                                     null, null,
                                     COMPLEX_OBSERVATION,
                                     proc,
                                     time,
                                     feature,
                                     phen,
                                     null,
                                     result,
                                     properties,
                                     null);
       
    }
    
    private Map<String, Object> createNewBlocValues(boolean profile, int mainFieldIndex) {
        Map<String, Object> results = new LinkedHashMap<>();
        // exclude non measure fields
        for (int i = 0; i < fields.size(); i++) {
            Field f = fields.get(i);
            if (!((includeID && f.name.equals("id"))          || // id field
                  (f.type.equals(FieldType.TIME) && profile)  || // time field fr profile
                  (mainFieldIndex == i))) {                        // main field
                results.put(fields.get(i).name, null);
            } 
        }
        return results;
    }
    
    @Override
    public Map<String, Observation> parseComplexObservation(SQLResult rs2, long oid, final ProcedureInfo pti, final Procedure proc, final SamplingFeature feature, final Phenomenon phen, boolean separatedProfileObs) throws SQLException {
        final String obsID               = "obs-" + oid;
        boolean profile                  = "profile".equals(pti.type);
        int mainFieldIndex               = fields.indexOf(pti.mainField);
        Object prevLineKey               = null;
        Object prevObsKey                = null;
        boolean hasData                  = false;
        Map<String, Object> blocValues   = createNewBlocValues(profile, mainFieldIndex);
        Map<String, Object> properties   = Map.of("type", pti.type);
        Map<String, Observation> results = new HashMap<>();
        boolean separated                = (separatedProfileObs && profile);
        
        while (rs2.nextOnField(pti.mainField.name)) {
            
            final Object mainValue = switch (pti.mainField.type) {
                case TIME     -> rs2.getTimestamp(pti.mainField.name);
                case QUANTITY -> rs2.getDouble(pti.mainField.name);
                default       -> throw new SQLException("Unexpected main field type");
            };
            
            final String fieldName = rs2.getString("obsprop_id");
            final Double value     = rs2.getDouble("result");
            final Long measureId   = rs2.getLong("id");
            final Timestamp time   = rs2.getTimestamp("time");
            
            if (firstTime == null) {
                firstTime = dateFromTS(time);
            }
            lastTime = dateFromTS(time);
            
            // observations for profile are a combination of the time and the z_value
            Object lineKey;
            Object obsKey;
            if (profile) {
                lineKey = time.getTime() + '-' + mainValue.toString();
                obsKey  = time.getTime();
            } else {
                lineKey = mainValue;
                obsKey  = null;
            }
            
            // start new line
            if (!Objects.equals(lineKey, prevLineKey)) {
                
                // close previous block
                if (prevLineKey != null) {
                    endBlock(blocValues);
                    blocValues = createNewBlocValues(profile, mainFieldIndex);
                    
                    // close profile observation
                    if (separated && !Objects.equals(obsKey, prevObsKey)) {
                        final TemporalPrimitive timeObs = buildTime(obsID, firstTime, null);
                        Entry<String, Observation> entry = buildObservation(obsID, proc, feature, phen, timeObs, properties, separated);
                        results.put(entry.getKey(), entry.getValue());
                    }
                }
                
                values.newBlock();
                hasData = true;
                // handle non measure fields
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if (includeID && f.name.equals("id")) {
                        values.appendString(obsName + '-' + measureId, false, f);
                    } else if (f.type.equals(FieldType.TIME) && profileWithTime) {
                        values.appendTime(dateFromTS(time), false, f);
                    }
                }
                // handle main field
                //if (mainIncluded) {
                    values.appendValue(mainValue, false, pti.mainField);
                //}
                
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
            prevLineKey    = lineKey;
            prevObsKey     = obsKey;
        }
        // close last block if any
        if (hasData) {
            endBlock(blocValues);
        }
        final TemporalPrimitive timeObs = buildTime(obsID, firstTime, lastTime);
        Entry<String, Observation> entry = buildObservation(obsID, proc, feature, phen, timeObs, properties, separated);
        results.put(entry.getKey(), entry.getValue());
        return results;
    }
    
    private Entry<String, Observation> buildObservation(String obsID, final Procedure proc, final SamplingFeature feature, final Phenomenon phen, final TemporalPrimitive time, Map<String, Object> properties, boolean separatedObs) {
        String observationKey;
        if (separatedObs) {
            synchronized (format2) {
                observationKey = proc.getId() + '-' + feature.getId() + '-' + format2.format(firstTime);
            }
        } else {
            observationKey = proc.getId() + '-' + feature.getId();
        }
        final ComplexResult result = buildComplexResult();
        
        // reset
        firstTime = null;
        lastTime  = null;
        values.clear();
        
        Observation observation = new Observation(obsID,
                                                  obsName,
                                                  null, null,
                                                  COMPLEX_OBSERVATION,
                                                  proc,
                                                  time,
                                                  feature,
                                                  phen,
                                                  null,
                                                  result,
                                                  properties,
                                                  null);
        
        return new AbstractMap.SimpleEntry<>(observationKey, observation);
    }
    
    @Override
    public void completeObservation(final SQLResult rs2, final ProcedureInfo pti, Observation observation) throws SQLException {
        boolean profile                = "profile".equals(pti.type);
        int mainFieldIndex             = fields.indexOf(pti.mainField);
        boolean hasData                = false;
        Object previousKey             = null;
        Map<String, Object> blocValues = createNewBlocValues(profile, mainFieldIndex);
        
        while (rs2.nextOnField(pti.mainField.name)) {
            
            final Object mainValue = switch (pti.mainField.type) {
                case TIME     -> rs2.getTimestamp(pti.mainField.name);
                case QUANTITY -> rs2.getDouble(pti.mainField.name);
                default       -> throw new SQLException("Unexpected main field type");
            };
            
            
            final String fieldName = rs2.getString("obsprop_id");
            final Double value     = rs2.getDouble("result");
            final Long measureId   = rs2.getLong("id");
            final Timestamp time   = rs2.getTimestamp("time");
            
            if (firstTime == null) {
                firstTime = dateFromTS(time);
            }
            lastTime = dateFromTS(time);
            
            // observations for profile are a combination of the time and the z_value
            Object mainKey;
            if (profile) {
                mainKey = time.getTime() + '-' + mainValue.toString();
            } else {
                mainKey = mainValue;
            }
            
            // start new line
            if (!Objects.equals(mainKey, previousKey)) {
                
                // close previous block
                if (previousKey != null) {
                    endBlock(blocValues);
                    blocValues = createNewBlocValues(profile, mainFieldIndex);
                }
                
                values.newBlock();
                hasData = true;
                // handle non measure fields
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    if (includeID && f.name.equals("id")) {
                        values.appendString(obsName + '-' + measureId, false, f);
                    } else if (f.type.equals(FieldType.TIME) && profileWithTime) {
                        values.appendTime(dateFromTS(time), false, f);
                    }
                }
                // handle main field
                //if (mainIncluded) {
                    values.appendValue(mainValue, false, pti.mainField);
                //}
                
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

        // update observation result and sampling time
        ComplexResult cr = (ComplexResult) observation.getResult();
        cr.setNbValues(cr.getNbValues() + getNbValueParsed());
        switch (values.getMode()) {
            case DATA_ARRAY -> cr.getDataArray().addAll(values.getDataArray());
            case CSV        -> cr.setValues(cr.getValues() + values.getStringValues());
        }
        // observation can be instant
        observation.extendSamplingTime(firstTime);
        observation.extendSamplingTime(lastTime);
    }
}
