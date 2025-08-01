/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.store.observation.db;

import org.constellation.store.observation.db.model.DbField;
import org.constellation.util.SQLResult;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.constellation.api.CommonConstants.COMPLEX_OBSERVATION;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.geotoolkit.observation.OMUtils;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.Field;
import static org.geotoolkit.observation.model.FieldDataType.BOOLEAN;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.temp.ObservationType;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResultMode;
import static org.geotoolkit.observation.model.ResultMode.CSV;
import static org.geotoolkit.observation.model.ResultMode.DATA_ARRAY;
import org.geotoolkit.observation.model.SamplingFeature;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;
import org.opengis.temporal.TemporalPrimitive;

/**
 *
 * @author guilhem
 */
public class FieldParser {

    protected final SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");
    
    protected Date firstTime = null;
    protected Date lastTime  = null;
    protected int nbParsed  = 0;
    
    protected final List<Field> fields;
    protected final boolean profileWithTime;
    protected final boolean includeID;
    protected final boolean includeQuality;
    protected final boolean includeParameter;
    protected final ResultBuilder values;
    protected String obsName;
    protected final int fieldOffset;
    
    protected boolean onlyMain;

    protected boolean first = true;

    /**
     * Build a new parser to transform SQL result in An exploitable STA result.
     * 
     * @param fields List of available fields in the SQL result.
     * @param values Result builder that will be append.
     * @param profileWithTime A flag indicating if we are building profile measure and if we must add time.
     * @param includeID A flag indicating if we must include measure identifier.
     * @param includeQuality A flag indicating if we must include quality field for measure.
     * @param includeParameter A flag indicating if we must include parameter field for measure.
     * @param obsName Main observation identifier (used to build measure identifier).
     * @param fieldOffset The index of the first measure field, in the field list.
     */
    public FieldParser(int mainFieldIndex, List<Field> fields, ResultBuilder values, boolean profileWithTime, boolean includeID, boolean includeQuality, boolean includeParameter, String obsName, int fieldOffset) {
        this.onlyMain = true;
        for (Field f : fields) {
            if (f.type == FieldType.MEASURE)  {
                this.onlyMain = false;
                break;
            }
        }
        this.profileWithTime = profileWithTime;
        this.fields = fields;
        this.includeID = includeID;
        this.includeQuality = includeQuality;
        this.includeParameter = includeParameter;
        this.obsName = obsName;
        this.values = values;
        this.fieldOffset = fieldOffset;
    }
    
    /**
     * Build a new parser to transform SQL result in An exploitable STA result.
     * 
     * @param fields List of available fields in the SQL result.
     * @param resultMode Result mode in order to build a ResultBuilder.
     * @param profileWithTime A flag indicating if we are building profile measure and if we must add time.
     * @param includeID A flag indicating if we must include measure identifier.
     * @param includeQuality A flag indicating if we must include quality field for measure.
     * @param includeParameter A flag indicating if we must include parameter field for measure.
     * @param obsName Main observation identifier (used to build measure identifier).
     * @param fieldOffset The index of the first measure field, in the field list.
     */
    public FieldParser(int mainFieldIndex, List<Field> fields, ResultMode resultMode, boolean profileWithTime, boolean includeID, boolean includeQuality, boolean includeParameter, String obsName, int fieldOffset) {
        this(mainFieldIndex, fields, new ResultBuilder(resultMode, DEFAULT_ENCODING, false), profileWithTime, includeID, includeQuality, includeParameter, obsName, fieldOffset);
    }

    public void setName(String name) {
        this.obsName = name;
    }
    
    public void setFirstTime(Date firstTime) {
        this.firstTime = firstTime;
    }

    /**
     * Parse a measure line from an sql result.
     * 
     * @param rs A SQL result set.
     * 
     * @throws SQLException 
     */
    public void parseLine(SQLResult rs) throws SQLException {
        values.newBlock();
        for (int i = 0; i < fields.size(); i++) {

            DbField field = (DbField) fields.get(i);
            parseField(field, rs, i, fieldOffset, null, null);

            if (includeQuality && field.qualityFields != null) {
                for (Field qField : field.qualityFields) {
                    parseField((DbField) qField, rs, -1, -1, field, "quality");
                }
            }
            if (includeParameter && field.parameterFields != null) {
                for (Field pField : field.parameterFields) {
                    parseField((DbField) pField, rs, -1, -1, field, "parameter");
                }
            }
        }
        nbParsed = nbParsed + values.endBlock();
    }

    private void parseField(DbField field, SQLResult rs, int fieldIndex, int offset, Field parent, String subFileType) throws SQLException {
        boolean isMeasureField;
        String fieldName;
        if (parent != null) {
           fieldName = parent.name + "_" + subFileType + "_" + field.name;
           isMeasureField = true;
        } else {
           fieldName = field.name;
           isMeasureField = (field.type == FieldType.MEASURE) || (onlyMain && field.type == FieldType.MAIN );
        }
        int rsIndex = field.tableNumber;
        switch (field.dataType) {
            case TIME:
                // profile with time field whth value set externally
                if (profileWithTime && fieldIndex < offset) {
                    values.appendTime(firstTime, isMeasureField, field);
                } else {
                    Date t;
                    // main timeseries field or joined profile time field 
                    if (field.type == FieldType.MAIN || fieldIndex < offset) {
                        t = dateFromTS(rs.getTimestamp(fieldName)); // main field is present in every table request
                        if (first) {
                            firstTime = t;
                            first = false;
                        }
                        lastTime = t;
                    } else {
                        t = dateFromTS(rs.getTimestamp(fieldName, rsIndex));
                    }
                    values.appendTime(t, isMeasureField, field);
                }
                break;
            case QUANTITY:
                Double d;
                // profile main field
                if (field.type == FieldType.MAIN) {
                    d =  rs.getDouble(fieldName); // main field is present in every table request
                    if (rs.wasNull()) {
                        d = Double.NaN;
                    }
                } else {
                    d =  rs.getDouble(fieldName, rsIndex);
                    if (rs.wasNull(rsIndex)) {
                        d = Double.NaN;
                    }
                }
                
                values.appendDouble(d, isMeasureField, field);
                break;
            case BOOLEAN:
                boolean bvalue = rs.getBoolean(fieldName, rsIndex);
                values.appendBoolean(bvalue, isMeasureField, field);
                break;
            case JSON:
                String sValue = rs.getString(fieldName, rsIndex);
                Map m = OMUtils.readJsonMap(sValue);
                values.appendMap(m, isMeasureField, field);
                break;
            default:
                String svalue;
                // id field is present in all th resultSets
                if (includeID && fieldName.equals("id")) {
                    svalue =  obsName + '-' + rs.getString(fieldName);
                } else {
                    svalue = rs.getString(fieldName, rsIndex);
                }
                values.appendString(svalue, isMeasureField, field);
                break;
        }
    }
    
    public ComplexResult buildComplexResult() {
        return OMUtils.buildComplexResult(fields, nbParsed, values);
    }
    
    public void clear() {
        nbParsed = 0;
        values.clear();
    }

    public int getNbValueParsed() {
        return nbParsed;
    }
    
    public Map<String, Observation> parseSingleMeasureObservation(SQLResult rs2, long oid, final ProcedureInfo pti, final Procedure proc, final SamplingFeature feature, final Phenomenon phen) throws SQLException {
        final Map<String, Observation> observations = new LinkedHashMap<>();
        Map<String, Object> properties = new HashMap<>();
        properties.put("type", pti.type.name().toLowerCase());
                
        while (rs2.nextOnField(pti.mainField.name)) {
            parseLine(rs2);

            /**
             * In "separated observation" mode we create an observation for each measure and don't merge it into a single obervation by procedure/foi.
             */
            final String measureID                = rs2.getString("id");
            final String singleObsID              = "obs-" + oid + '-' + measureID;
            final TemporalPrimitive time = buildTime(singleObsID, lastTime != null ? lastTime : firstTime, null);
            final ComplexResult result            = buildComplexResult();
            final String singleName               = obsName + '-' + measureID;
            final Observation observation = new Observation(singleObsID,
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
            observations.put(pti.id + '-' + obsName + '-' + measureID, observation);
            clear();
        }
        return observations;
    }
    
    public Map<String, Observation> parseComplexObservation(SQLResult rs2, long oid, final ProcedureInfo pti, final Procedure proc, final SamplingFeature feature, final Phenomenon phen, boolean separatedProfileObs) throws SQLException {
        final String obsID             = "obs-" + oid;
        boolean profile                = pti.type == ObservationType.PROFILE;
        Map<String, Object> properties = new HashMap<>();
        properties.put("type", pti.type.name().toLowerCase());
        
        while (rs2.nextOnField(pti.mainField.name)) {
            parseLine(rs2);
        }
        
        final TemporalPrimitive time = buildTime(obsID, firstTime, lastTime);
        final ComplexResult result = buildComplexResult();
        final Observation observation = new Observation(obsID,
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
        String observationKey;
        String fid = feature != null ? feature.getId() : "null";
        if (separatedProfileObs && profile) {
            synchronized (format2) {
                observationKey = pti.id + '-' + fid + '-' + format2.format(firstTime);
            }
        } else {
            observationKey = pti.id + '-' + fid;
        }
        return Map.of(observationKey, observation);
    }
    
    public void completeObservation(final SQLResult rs2, final ProcedureInfo pti, Observation observation) throws SQLException {
        while (rs2.nextOnField(pti.mainField.name)) {
            parseLine(rs2);
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