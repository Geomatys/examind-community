/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

import org.constellation.util.FilterSQLRequest;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.EVENT_TIME;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.ResultBuilder;
import org.geotoolkit.observation.model.ResultMode;
import static org.constellation.store.observation.db.OM2BaseReader.defaultCRS;
import static org.constellation.store.observation.db.OM2Utils.buildComplexResult;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.FeatureProperty;
import static org.geotoolkit.observation.ObservationFilterFlags.*;
import org.geotoolkit.observation.model.FieldPhenomenon;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.observation.xml.OMXmlFactory;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.geotoolkit.sos.xml.ResponseModeType;
import static org.geotoolkit.sos.xml.ResponseModeType.INLINE;
import static org.geotoolkit.sos.xml.ResponseModeType.RESULT_TEMPLATE;

import static org.geotoolkit.sos.xml.SOSXmlFactory.*;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.TextBlock;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.Literal;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.observation.Process;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2ObservationFilterReader extends OM2ObservationFilter {

    private String responseFormat;

    public OM2ObservationFilterReader(final OM2ObservationFilter omFilter) {
        super(omFilter);
    }

    public OM2ObservationFilterReader(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties, final boolean timescaleDB) throws DataStoreException {
        super(source, isPostgres, schemaPrefix, properties, timescaleDB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeFilter(final TemporalOperator tFilter) throws DataStoreException {
        // we get the property name (not used for now)
        // String propertyName = tFilter.getExpression1()
        Object time = tFilter.getExpressions().get(1);
        boolean getLoc = OMEntity.HISTORICAL_LOCATION.equals(objectType);
        TemporalOperatorName type = tFilter.getOperatorType();
        if (type == TemporalOperatorName.EQUALS) {

            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            if (time instanceof Period tp) {
                final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
                final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());

                // we request directly a multiple observation or a period observation (one measure during a period)
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                sqlRequest.append(" \"time_begin\"=").appendValue(begin).append(" AND ");
                sqlRequest.append(" \"time_end\"=").appendValue(end).append(") ");
                obsJoin = true;
                firstFilter = false;
            // if the temporal object is a timeInstant
            } else if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                if (getLoc) {
                    if (firstFilter) {
                        sqlRequest.append(" ( ");
                    } else {
                        sqlRequest.append("AND ( ");
                    }
                    sqlRequest.append(" \"time\"=").appendValue(position).append(") ");
                    firstFilter = false;
                } else {
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND ( \"$time\"=", conditional).appendValue(position, conditional).append(") ", conditional);
                    }
                    obsJoin = true;
                }
            } else {
                throw new ObservationStoreException("TM_Equals operation require timeInstant or TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else if (type == TemporalOperatorName.BEFORE) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            // for the operation before the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                if (getLoc) {
                    sqlRequest.append(" \"time\"<=").appendValue(position).append(")");
                } else {
                    sqlRequest.append(" \"time_begin\"<=").appendValue(position).append(")");
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND ( \"$time\"<=", conditional).appendValue(position, conditional).append(")", conditional);
                    }
                    obsJoin = true;
                }
                firstFilter = false;
            } else {
                throw new ObservationStoreException("TM_Before operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }

        } else if (type == TemporalOperatorName.AFTER) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            // for the operation after the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                if (getLoc) {
                    sqlRequest.append(" \"time\">=").appendValue(position).append(")");
                } else {
                    sqlRequest.append("( \"time_end\">=").appendValue(position).append(") OR (\"time_end\" IS NULL AND \"time_begin\" >=").appendValue(position).append("))");
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND (\"$time\">=", conditional).appendValue(position, conditional).append(")", conditional);
                    }
                    obsJoin = true;
                }
                firstFilter = false;
            } else {
                throw new ObservationStoreException("TM_After operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }

        } else if (type == TemporalOperatorName.DURING) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            if (time instanceof Period tp) {
                final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
                final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }

                if (getLoc) {
                    sqlRequest.append(" \"time\">=").appendValue(begin).append(" AND \"time\"<=").appendValue(end).append(")");
                } else {
                    OM2Utils.addtimeDuringSQLFilter(sqlRequest, tp);
                    sqlRequest.append(" ) ");
                    
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND ( \"$time\">=", conditional).appendValue(begin, conditional)
                                         .append(" AND \"$time\"<= ", conditional).appendValue(end, conditional).append(")", conditional);
                    }
                    obsJoin = true;
                }
                firstFilter = false;
            } else {
                throw new ObservationStoreException("TM_During operation require TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else {
            throw new ObservationStoreException("This operation is not take in charge by the Web Service, supported one are: TM_Equals, TM_After, TM_Before, TM_During");
        }
    }

    @Override
    public List<Observation> getObservations(Map<String, Object> hints) throws DataStoreException {
        if (RESULT_TEMPLATE.equals(responseMode)) {
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                return getMesurementTemplates(hints);
            } else {
                return getObservationTemplates(hints);
            }
        } else if (INLINE.equals(responseMode)) {
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                return getMesurements(hints);
            } else {
                return getComplexObservations(hints);
            }
        } else {
            throw new DataStoreException("Unsupported response mode:" + responseMode);
        }
    }

    private List<Observation> getObservationTemplates(final Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\" ");
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        boolean includeTimeInTemplate = getBooleanHint(hints, INCLUDE_TIME_IN_TEMPLATE, false);

        final List<Observation> observations = new ArrayList<>();
        final Map<String, Process> processMap = new HashMap<>();

        LOGGER.fine(sqlRequest.getRequest());
        try (final Connection c            = source.getConnection();
             final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
             final ResultSet rs            = pstmt.executeQuery()) {
             final TextBlock encoding = getDefaultTextEncoding(version);

            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final String procedureID;
                if (procedure.startsWith(sensorIdBase)) {
                    procedureID = procedure.substring(sensorIdBase.length());
                } else {
                    procedureID = procedure;
                }
                final String obsID = "obs-" + procedureID;
                final String name = observationTemplateIdBase + procedureID;
                final Phenomenon phen = getGlobalCompositePhenomenon(version, c, procedure);
                String featureID = null;
                FeatureProperty foi = null;
                if (includeFoiInTemplate) {
                    featureID = rs.getString("foi");
                    final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                    foi = buildFeatureProperty(version, feature);
                }
                TemporalGeometricPrimitive tempTime = null;
                if (includeTimeInTemplate) {
                    tempTime = getTimeForTemplate(c, procedure, null, featureID, version);
                }
                List<Field> fields = readFields(procedure, c);
                /*
                 *  BUILD RESULT
                 */
                final List<AnyScalar> scal = new ArrayList<>();
                for (Field f : fields) {
                    scal.add(f.getScalar(version));
                }
                final Process proc;
                if (processMap.containsKey(procedure)) {
                    proc = processMap.get(procedure);
                } else {
                    proc = getProcess(version, procedure, c);
                    processMap.put(procedure, proc);
                }
                final Object result = buildComplexResult(version, scal, 0, encoding, null, observations.size());
                Observation observation = OMXmlFactory.buildObservation(version, obsID, name, null, foi, phen, proc, result, tempTime, null);
                observations.add(observation);
            }


        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
        return observations;
    }

    private List<Observation> getMesurementTemplates(Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\", pd.\"order\" ");
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);

        boolean includeTimeInTemplate = getBooleanHint(hints, INCLUDE_TIME_IN_TEMPLATE, false);
        final List<Observation> observations = new ArrayList<>();
        final Map<String, Process> processMap = new HashMap<>();

        LOGGER.fine(sqlRequest.getRequest());
        try (final Connection c              = source.getConnection();
             final PreparedStatement pstmt   = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
             final ResultSet rs              = pstmt.executeQuery()) {

            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final String procedureID;
                if (procedure.startsWith(sensorIdBase)) {
                    procedureID = procedure.substring(sensorIdBase.length());
                } else {
                    procedureID = procedure;
                }
                final String obsID = "obs-" + procedureID;
                final String name = observationTemplateIdBase + procedureID;
                final String observedProperty = rs.getString("obsprop");
                final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                final int phenIndex = rs.getInt("order");
                final String phenUom = rs.getString("uom");
                
                String featureID = null;
                FeatureProperty foi = null;
                if (includeFoiInTemplate) {
                    featureID = rs.getString("foi");
                    final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                    foi = buildFeatureProperty(version, feature);
                }

                /*
                 *  BUILD RESULT
                 */
                final Process proc;
                if (processMap.containsKey(procedure)) {
                    proc = processMap.get(procedure);
                } else {
                    proc = getProcess(version, procedure, c);
                    processMap.put(procedure, proc);
                }
                
                TemporalGeometricPrimitive tempTime = null;
                if (includeTimeInTemplate) {
                    tempTime = getTimeForTemplate(c, procedure, observedProperty, featureID, version);
                }
                final Object result = buildMeasure(version, "measure-001", phenUom, 0d);
                observations.add(OMXmlFactory.buildMeasurement(version, obsID + '-' + phenIndex, name + '-' + phenIndex, null, foi, phen, proc, result, tempTime, null));
                
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
        return observations;
    }

    private List<Observation> getComplexObservations(final Map<String,Object> hints) throws DataStoreException {
        String version                = getVersionFromHints(hints);
        boolean includeIDInDataBlock  = getBooleanHint(hints, INCLUDE_ID_IN_DATABLOCK,  false);
        boolean includeTimeForProfile = getBooleanHint(hints, "includeTimeForProfile", false);
        ResultMode resultMode         = (ResultMode) hints.get(RESULT_MODE);
        boolean separatedObs          = getBooleanHint(hints, SEPARATED_OBSERVATION,  false);
        if (resultMode == null) {
            resultMode = ResultMode.CSV;
        }
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        final Map<String, Observation> observations = new LinkedHashMap<>();
        final Map<String, Process> processMap = new LinkedHashMap<>();

        LOGGER.fine(sqlRequest.getRequest());
        try(final Connection c              = source.getConnection();
            final PreparedStatement pstmt   = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs              = pstmt.executeQuery()) {
            // add orderby to the query

            final TextBlock encoding = getDefaultTextEncoding(version);
            final Map<String, List<Field>> fieldMap = new LinkedHashMap<>();

            while (rs.next()) {
                int nbValue = 0;
                ResultBuilder values = new ResultBuilder(resultMode, encoding, false);
                final String procedure = rs.getString("procedure");
                final String featureID = rs.getString("foi");
                final int oid = rs.getInt("id");
                Observation observation = observations.get(procedure + '-' + featureID);
                final int pid = getPIDFromProcedure(procedure, c);
                final Field mainField = getMainField(procedure, c);
                boolean isTimeField   = false;
                int offset            = 0;
                if (mainField != null) {
                    isTimeField = FieldType.TIME.equals(mainField.type);
                }
                List<Field> fields = fieldMap.get(procedure);
                if (fields == null) {
                    if (!currentFields.isEmpty()) {
                        fields = new ArrayList<>();
                        if (mainField != null) {
                            fields.add(mainField);
                        }

                        List<Field> phenFields = new ArrayList<>();
                        for (String f : currentFields) {
                            final Field field = getFieldForPhenomenon(procedure, f, c);
                            if (field != null && !fields.contains(field)) {
                                phenFields.add(field);
                            }
                        }

                        // add proper order to fields
                        List<Field> procedureFields = readFields(procedure, c);
                        phenFields = reOrderFields(procedureFields, phenFields);
                        fields.addAll(phenFields);

                        // special case for trajectory observation
                        // if lat/lon are available, include it anyway if they are not part of the phenomenon.
                        final List<Field> llFields = getPosFields(procedure, c);
                        for (Field llField : llFields) {
                            if (!fields.contains(llField)) {
                                fields.add(1, llField);
                            }
                        }

                    } else {
                        fields = readFields(procedure, c);
                    }

                    // add the result id in the dataBlock if requested
                    if (includeIDInDataBlock) {
                        fields.add(0, new Field(0, FieldType.TEXT, "id", null, "measure identifier", null));
                    }
                    fieldMap.put(procedure, fields);
                }

                if (isTimeField) {
                    sqlMeasureRequest.replaceAll("$time", mainField.name);
                    offset++;
                }
                if (includeIDInDataBlock) {
                    offset++;
                }
                while (sqlMeasureRequest.contains("${allphen")) {
                    String measureFilter = sqlMeasureRequest.getRequest();
                    int opos = measureFilter.indexOf("${allphen");
                    int cpos = measureFilter.indexOf("}", opos + 9);
                    String block = measureFilter.substring(opos, cpos + 1);
                    StringBuilder sb = new StringBuilder();
                    for (Field field : fields) {
                        sb.append(" AND (").append(block.replace("${allphen", "\"" + field.name + "\"").replace('}', ' ')).append(") ");
                    }
                    sqlMeasureRequest.replaceFirst(block, sb.toString());
                }

                for (int i = offset, j=0; i < fields.size(); i++, j++) {
                    sqlMeasureRequest.replaceAll("$phen" + j, "\"" + fields.get(i).name + "\"");
                }
                final FilterSQLRequest measureRequest = new FilterSQLRequest("SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ");
                final String fieldOrdering;
                if (mainField != null) {
                    fieldOrdering = "m.\"" + mainField.name + "\"";
                } else {
                    // we keep this fallback where no main field is found.
                    // i'm not sure it will be possible to handle an observation with no main field (meaning its not a timeseries or a profile).
                    fieldOrdering = "m.\"id\"";
                }
                measureRequest.appendValue(-1).append(sqlMeasureRequest, isTimeField).append("ORDER BY ").append(fieldOrdering);

                int timeForProfileIndex = -1;
                if (includeTimeForProfile && !isTimeField) {
                    timeForProfileIndex = includeIDInDataBlock ? 1 : 0;
                }

                Date firstTime = null;
                final String name = rs.getString("identifier");
                if (observation == null) {
                    final String obsID = "obs-" + oid;
                    final String timeID = "time-" + oid;
                    final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                    final FeatureProperty prop = buildFeatureProperty(version, feature);

                    //final String observedProperty = rs.getString("observed_property");
                    //final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                    final Phenomenon phen = getGlobalCompositePhenomenon(version, c, procedure);


                    firstTime = dateFromTS(rs.getTimestamp("time_begin"));
                    Date lastTime = dateFromTS(rs.getTimestamp("time_end"));
                    boolean first = true;
                    final List<AnyScalar> scal = new ArrayList<>();
                    for (Field f : fields) {
                        scal.add(f.getScalar(version));
                    }

                    // add the time field in the dataBlock if requested (only if main field is not a time field)
                    if (timeForProfileIndex != -1) {
                        scal.add(timeForProfileIndex, DEFAULT_TIME_FIELD.getScalar(version));
                    }

                    /*
                     *  BUILD RESULT
                     */
                    measureRequest.setParamValue(0, oid);
                    LOGGER.fine(measureRequest.getRequest());
                    try(final PreparedStatement stmt = measureRequest.fillParams(c.prepareStatement(measureRequest.getRequest()));
                        final ResultSet rs2 = stmt.executeQuery()) {
                        while (rs2.next()) {
                            values.newBlock();
                            for (int i = 0; i < fields.size(); i++) {

                                if (i == timeForProfileIndex) {
                                    values.appendTime(firstTime);
                                }

                                Field field = fields.get(i);
                                switch (field.type) {
                                    case TIME:
                                        Date t = dateFromTS(rs2.getTimestamp(field.name));
                                        values.appendTime(t);
                                        if (first) {
                                            firstTime = t;
                                            first = false;
                                        }
                                        lastTime = t;
                                        break;
                                    case QUANTITY:
                                        String value = rs2.getString(field.name); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                        Double d = Double.NaN;
                                        if (value != null && !value.isEmpty()) {
                                            d = rs2.getDouble(field.name);
                                        }
                                        values.appendDouble(d);
                                        break;
                                    case BOOLEAN:
                                        boolean bvalue = rs2.getBoolean(field.name);
                                        values.appendBoolean(bvalue);
                                        break;
                                    default:
                                        String svalue = rs2.getString(field.name);
                                        if (includeIDInDataBlock && field.name.equals("id")) {
                                            svalue = name + '-' + svalue;
                                        }
                                        values.appendString(svalue);
                                        break;
                                }
                            }
                            nbValue = nbValue + values.endBlock();

                            /**
                             * In "separated observation" mode we create an observation for each measure and don't merge it into a single obervation by procedure/foi.
                             */
                            if (separatedObs) {
                                final TemporalGeometricPrimitive time = buildTimeInstant(version, timeID, lastTime != null ? lastTime : firstTime);
                                final Object result = buildComplexResult(version, scal, nbValue, encoding, values, observations.size());
                                final Process proc;
                                if (processMap.containsKey(procedure)) {
                                    proc = processMap.get(procedure);
                                } else {
                                    proc = getProcess(version, procedure, c);
                                    processMap.put(procedure, proc);
                                }
                                String measureID         = rs2.getString("id");
                                final String singleObsID = "obs-" + oid + '-' + measureID;
                                final String singleName  = name + '-' + measureID;
                                observation = OMXmlFactory.buildObservation(version, singleObsID, singleName, null, prop, phen, proc, result, time, null);
                                observations.put(procedure + '-' + name + '-' + measureID, observation);
                                values = new ResultBuilder(resultMode, encoding, false);
                                nbValue = 0;
                            }
                        }
                    } catch (SQLException ex) {
                        LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", measureRequest.getRequest());
                        throw new DataStoreException("the service has throw a SQL Exception.", ex);
                    }

                   /**
                    * we create an observation with all the measures and keep it so we can extend it if another observation for the same procedure/foi appears.
                    */
                    if (!separatedObs) {
                        final TemporalGeometricPrimitive time = buildTimePeriod(version, timeID, firstTime, lastTime);
                        final Object result = buildComplexResult(version, scal, nbValue, encoding, values, observations.size());
                        final Process proc;
                        if (processMap.containsKey(procedure)) {
                            proc = processMap.get(procedure);
                        } else {
                            proc = getProcess(version, procedure, c);
                            processMap.put(procedure, proc);
                        }
                        observation = OMXmlFactory.buildObservation(version, obsID, name, null, prop, phen, proc, result, time, null);
                        observations.put(procedure + '-' + featureID, observation);
                    }
                } else {
                    /**
                     * complete the previous observation with new measures.
                     */
                    Date lastTime = null;
                    measureRequest.setParamValue(0, oid);
                    LOGGER.fine(measureRequest.toString());
                    try(final PreparedStatement stmt = measureRequest.fillParams(c.prepareStatement(measureRequest.getRequest()));
                        final ResultSet rs2 = stmt.executeQuery()) {
                        while (rs2.next()) {
                            values.newBlock();
                            for (int i = 0; i < fields.size(); i++) {

                                if (i == timeForProfileIndex) {
                                    values.appendTime(firstTime);
                                }

                                Field field = fields.get(i);
                                switch (field.type) {
                                    case TIME:
                                        Date t = dateFromTS(rs2.getTimestamp(field.name));
                                        values.appendTime(t);
                                        lastTime = t;
                                        break;
                                    case QUANTITY:
                                        String value = rs2.getString(field.name); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                        Double d = Double.NaN;
                                        if (value != null && !value.isEmpty()) {
                                            d = rs2.getDouble(field.name);
                                        }
                                        values.appendDouble(d);
                                        break;
                                    case BOOLEAN:
                                        boolean bvalue = rs2.getBoolean(field.name);
                                        values.appendBoolean(bvalue);
                                        break;
                                    default:
                                        String svalue = rs2.getString(field.name);
                                        if (includeIDInDataBlock && field.name.equals("id")) {
                                            svalue = name + '-' + svalue;
                                        }
                                        values.appendString(svalue);
                                        break;
                                }
                            }
                            nbValue = nbValue + values.endBlock();
                        }
                    }

                    // update observation result and sampling time
                    final DataArrayProperty result = (DataArrayProperty) (observation).getResult();
                    final DataArray array = result.getDataArray();
                    array.setElementCount(array.getElementCount().getCount().getValue() + nbValue);
                    switch (resultMode) {
                        case DATA_ARRAY: array.getDataValues().getAny().addAll(values.getDataArray()); break;
                        case CSV:     array.setValues(array.getValues() + values.getStringValues()); break;
                    }
                    ((AbstractObservation) observation).extendSamplingTime(lastTime);
                }
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
        List<Observation> results = new ArrayList<>(observations.values());
        
        // TODO make a real pagination
        return applyPostPagination(hints, results);
    }

    private List<Observation> getMesurements(final Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        // add orderby to the query
        sqlRequest.append(" ORDER BY o.\"id\"");
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }

        final List<Observation> observations = new ArrayList<>();
        final Map<String, Process> processMap = new HashMap<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final Date startTime = dateFromTS(rs.getTimestamp("time_begin"));
                final Date endTime = dateFromTS(rs.getTimestamp("time_end"));
                final int oid = rs.getInt("id");
                final String name = rs.getString("identifier");
                final String obsID = "obs-" + oid;
                final String timeID = "time-" + oid;
                final String featureID = rs.getString("foi");
                final String observedProperty = rs.getString("observed_property");
                final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                final int pid = getPIDFromProcedure(procedure, c);
                final List<Field> fields = readFields(procedure, true, c);

                final Process proc;
                if (processMap.containsKey(procedure)) {
                    proc = processMap.get(procedure);
                } else {
                    proc = getProcess(version, procedure, c);
                    processMap.put(procedure, proc);
                }

                String start = null;
                if (startTime != null) {
                    synchronized (format2) {
                        start = format2.format(startTime);
                    }
                }
                String end = null;
                if (endTime != null) {
                    synchronized (format2) {
                        end = format2.format(endTime);
                    }
                }
                TemporalGeometricPrimitive time = null;
                if (start != null && end == null) {
                    time = buildTimeInstant(version, timeID, start);
                } else if (start != null || end != null) {
                    if (Objects.equals(start, end)) {
                        time = buildTimeInstant(version, timeID, start);
                    } else {
                        time = buildTimePeriod(version, timeID, start, end);
                    }
                }

                /*
                 *  BUILD RESULT
                 */
                final Field mainField = getMainField(procedure, c);
                boolean notProfile   = FieldType.TIME.equals(mainField.type);

                List<FieldPhenomenon> fieldPhen = getPhenomenonFields(phen, fields, c);
                if (notProfile) {
                    sqlMeasureRequest.replaceAll("$time", mainField.name);
                }
                while (sqlMeasureRequest.contains("${allphen")) {
                    String measureFilter = sqlMeasureRequest.getRequest();
                    int opos = measureFilter.indexOf("${allphen");
                    int cpos = measureFilter.indexOf("}", opos + 9);
                    String block = measureFilter.substring(opos, cpos + 1);
                    StringBuilder sb = new StringBuilder();
                    for (FieldPhenomenon field : fieldPhen) {
                        sb.append(" AND (").append(block.replace("${allphen", "\"" + field.getField().name + "\"").replace('}', ' ')).append(") ");
                    }
                    sqlMeasureRequest.replaceFirst(block, sb.toString());
                }

                for (FieldPhenomenon field : fieldPhen) {
                    sqlMeasureRequest.replaceAll("$phen" + field.getField().index, "\"" + field.getField().name + "\"");
                }

                final FilterSQLRequest measureRequest = new FilterSQLRequest("SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ");
                measureRequest.appendValue(oid).append(" ").append(sqlMeasureRequest, notProfile);
                measureRequest.append(" ORDER BY ").append("m.\"" + mainField.name + "\"");

                /**
                 * coherence verification
                 */
                LOGGER.fine(measureRequest.toString());
                try(final PreparedStatement stmt = measureRequest.fillParams(c.prepareStatement(measureRequest.getRequest()));
                    final ResultSet rs2 = stmt.executeQuery()) {
                    while (rs2.next()) {
                        final Integer rid = rs2.getInt("id");
                        if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                            TemporalGeometricPrimitive measureTime;
                            if (notProfile) {
                                final Date mt = dateFromTS(rs2.getTimestamp(mainField.name));
                                measureTime = buildTimeInstant(version, "time-" + oid + '-' + rid, mt);
                            } else {
                                measureTime = time;
                            }

                            for (int i = 0; i < fieldPhen.size(); i++) {
                                FieldPhenomenon field = fieldPhen.get(i);
                                Double dValue = null;
                                final String value = rs2.getString(field.getField().name);
                                if (value != null) {
                                    try {
                                        dValue = Double.parseDouble(value);
                                    } catch (NumberFormatException ex) {
                                        throw new DataStoreException("Unable ta parse the result value as a double (value=" + value + ")");
                                    }
                                    final FeatureProperty foi = buildFeatureProperty(version, feature); // do not share the same object
                                    final Object result = buildMeasure(version, "measure-00" + rid, field.getField().uom, dValue);
                                    observations.add(OMXmlFactory.buildMeasurement(version, obsID + '-' + field.getField().index + '-' + rid, name + '-' + field.getField().index + '-' + rid, null, foi, field.getPhenomenon(), proc, result, measureTime, null));
                                }
                            }
                        }
                    }
                }
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
        // TODO make a real pagination
        return applyPostPagination(hints, observations);
    }

    @Override
    public Object getResults(final Map<String, Object> hints) throws DataStoreException {
        if (ResponseModeType.OUT_OF_BAND.equals(responseMode)) {
            throw new ObservationStoreException("Out of band response mode has not been implemented yet", NO_APPLICABLE_CODE, RESPONSE_MODE);
        }
        final Integer decimationSize   = getIntegerHint(hints, DECIMATION_SIZE, null);
        final boolean countRequest     = "count".equals(responseFormat);
        boolean includeTimeForProfile  = !countRequest && getBooleanHint(hints, "includeTimeForProfile", false);
        final boolean profile          = "profile".equals(currentOMType);
        final boolean profileWithTime  = profile && includeTimeForProfile;
        if (decimationSize != null && !countRequest) {
            if (timescaleDB) {
                return getDecimatedResultsTimeScale(decimationSize, includeTimeForProfile);
            } else {
                return getDecimatedResults(decimationSize, includeTimeForProfile);
            }
        }
        try {
            // add orderby to the query
            final Field timeField = getTimeField(currentProcedure);
            if (timeField != null) {
                sqlRequest.append(sqlMeasureRequest.replaceAll("$time", timeField.name));
            }
            final Field mainField = getMainField(currentProcedure);
            final String fieldOrdering;
            if (mainField != null) {
                fieldOrdering = "m.\"" + mainField.name + "\"";
            } else {
                // we keep this fallback where no main field is found.
                // i'm not sure it will be possible to handle an observation with no main field (meaning its not a timeseries or a profile).
                fieldOrdering = "m.\"id\"";
            }
            sqlRequest.append(" ORDER BY  o.\"id\", ").append(fieldOrdering);

            if (firstFilter) {
                return sqlRequest.replaceFirst("WHERE", "");
            }
            LOGGER.fine(sqlRequest.toString());
            ResultBuilder values;
            try (final Connection c = source.getConnection();
                final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                final ResultSet rs = pstmt.executeQuery()) {

                final List<Field> fields;
                if (!currentFields.isEmpty()) {
                    fields = new ArrayList<>();
                    if (mainField != null) {
                        fields.add(mainField);
                    }
                    List<Field> phenFields = new ArrayList<>();
                    for (String f : currentFields) {
                        final Field field = getFieldForPhenomenon(currentProcedure, f, c);
                        if (field != null && !fields.contains(field)) {
                            phenFields.add(field);
                        }
                    }
                    // add proper order to fields
                    List<Field> procedureFields = readFields(currentProcedure, c);
                    phenFields = reOrderFields(procedureFields, phenFields);
                    fields.addAll(phenFields);

                } else {
                    fields = readFields(currentProcedure, c);
                }
                if ("resultArray".equals(responseFormat)) {
                    values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
                } else if ("text/csv".equals(responseFormat)) {
                    values = new ResultBuilder(ResultMode.CSV, getCsvTextEncoding("2.0.0"), true);
                    // Add the header
                    values.appendHeaders(fields);
                } else if (countRequest) {
                    values = new ResultBuilder(ResultMode.COUNT, null, false);
                } else {
                    values = new ResultBuilder(ResultMode.CSV, getDefaultTextEncoding("2.0.0"), false);
                }
                ResultProcessor processor = new ResultProcessor(values, fields, profileWithTime, profile);
                processor.processResults(rs);
            }
            switch (values.getMode()) {
                case DATA_ARRAY:  return values.getDataArray();
                case CSV:         return values.getStringValues();
                case COUNT:       return values.getCount();
                default: throw new IllegalArgumentException("Unexpected result mode");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
    }

    private Object getDecimatedResults(final int width, boolean includeTimeInProfile) throws DataStoreException {
        final boolean profile = "profile".equals(currentOMType);
        final boolean profileWithTime = profile && includeTimeInProfile;
        try (final Connection c = source.getConnection()) {

            final List<Field> fields = new ArrayList<>();
            final List<Field> allfields = readFields(currentProcedure, c);
            fields.add(allfields.get(0));
            for (int i = 1; i < allfields.size(); i++) {
                Field f = allfields.get(i);
                 if (isIncludedField(f.name, f.description, f.index)) {
                     fields.add(f);
                 }
            }

            final Field timeField = getTimeField(currentProcedure, c);
            if (timeField != null) {
                sqlMeasureRequest.replaceAll("$time", timeField.name);
            }
            while (sqlMeasureRequest.contains("${allphen")) {
                String measureFilter = sqlMeasureRequest.getRequest();
                int opos = measureFilter.indexOf("${allphen");
                int cpos = measureFilter.indexOf("}", opos + 9);
                String block = measureFilter.substring(opos, cpos + 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < fields.size(); i++) {
                    Field field = fields.get(i);
                    sb.append(" AND (").append(block.replace("${allphen", "\"" + field.name + "\"").replace('}', ' ')).append(") ");
                }
                sqlMeasureRequest.replaceFirst(block, sb.toString());
            }
            int offset = profile ? 0 : 1; // for profile, the first phenomenon field is the main field
            for (int i = offset; i < allfields.size(); i++) {
                Field f = allfields.get(i);
                sqlMeasureRequest.replaceAll("$phen" + (i - offset), "\"" + f.name + "\"");
            }
            sqlRequest.append(sqlMeasureRequest);
            final FilterSQLRequest fieldRequest = sqlRequest.clone();
            final Field mainField = getMainField(currentProcedure);
            final String fieldOrdering;
            if (mainField != null) {
                fieldOrdering = "m.\"" + mainField.name + "\"";
            } else {
                // we keep this fallback where no main field is found.
                // i'm not sure it will be possible to handle an observation with no main field (meaning its not a timeseries or a profile).
                fieldOrdering = "m.\"id\"";
            }
            sqlRequest.append(" ORDER BY  o.\"id\", ").append(fieldOrdering);
            if (profileWithTime) {
                sqlRequest.replaceFirst("m.*", "m.*, o.\"id\" as oid, o.\"time_begin\" ");
            } else if (profile) {
                sqlRequest.replaceFirst("m.*", "m.*, o.\"id\" as oid ");
            }
            LOGGER.fine(sqlRequest.toString());
            ResultBuilder values;
            try (final PreparedStatement pstmt    = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                 final ResultSet rs = pstmt.executeQuery()) {

                if ("resultArray".equals(responseFormat)) {
                    values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
                } else if ("text/csv".equals(responseFormat)) {
                    values = new ResultBuilder(ResultMode.CSV, getCsvTextEncoding("2.0.0"), true);
                    // Add the header
                    values.appendHeaders(fields);
                } else {
                    values = new ResultBuilder(ResultMode.CSV, getDefaultTextEncoding("2.0.0"), false);
                }
                final Map<Object, long[]> times = getMainFieldStep(fieldRequest, fields.get(0), c, width);

                ResultProcessor processor = new DefaultResultDecimator(values, fields, profileWithTime, profile, width, fieldFilters, times);
                processor.processResults(rs);
            }

            switch (values.getMode()) {
                case DATA_ARRAY:  return values.getDataArray();
                case CSV:         return values.getStringValues();
                default: throw new IllegalArgumentException("Unexpected result mode");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    private Object getDecimatedResultsTimeScale(final int width, boolean includeTimeInProfile) throws DataStoreException {
        final boolean profile = "profile".equals(currentOMType);
        final boolean profileWithTime = profile && includeTimeInProfile;
        try(final Connection c = source.getConnection()) {

            final List<Field> fields = new ArrayList<>();
            final List<Field> allfields = readFields(currentProcedure, c);
            fields.add(allfields.get(0));
            for (int i = 1; i < allfields.size(); i++) {
                Field f = allfields.get(i);
                 if (isIncludedField(f.name, f.description, f.index)) {
                     fields.add(f);
                 }
            }
            // add measure filter
            final Field timeField = getTimeField(currentProcedure, c);
            if (timeField != null) {
                sqlMeasureRequest.replaceAll("$time", timeField.name);
            }
            while (sqlMeasureRequest.contains("${allphen")) {
                String measureFilter = sqlMeasureRequest.getRequest();
                int opos = measureFilter.indexOf("${allphen");
                int cpos = measureFilter.indexOf("}", opos + 9);
                String block = measureFilter.substring(opos, cpos + 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < fields.size(); i++) {
                    Field field = fields.get(i);
                    sb.append(" AND (").append(block.replace("${allphen", "\"" + field.name + "\"").replace('}', ' ')).append(") ");
                }
                sqlMeasureRequest.replaceFirst(block, sb.toString());
            }
            int offset = profile ? 0 : 1; // for profile, the first phenomenon field is the main field
            for (int i = 0; i < allfields.size(); i++) {
                Field f = allfields.get(i);
                sqlMeasureRequest.replaceAll("$phen" + (i - offset), "\"" + f.name + "\"");
            }
            sqlRequest.append(sqlMeasureRequest);

            // calculate step
            final Map<Object, long[]> times = getMainFieldStep(sqlRequest.clone(), fields.get(0), c, width);
            final long step;
            if (profile) {
                // choose the first step
                // (may be replaced by one request by observation, maybe by looking if the step is uniform)
                step = times.values().iterator().next()[1];
            } else {
                step = times.get(1)[1];
            }

            StringBuilder select  = new StringBuilder();
            select.append("time_bucket('").append(step);
            if (profile) {
                select.append("', \"");
            } else {
                select.append(" ms', \"");
            }
            select.append(fields.get(0).name).append("\") AS step");
            for (int i = 1; i < fields.size(); i++) {
                 select.append(", avg(\"").append(fields.get(i).name).append("\") AS \"").append(fields.get(i).name).append("\"");
            }
            if (profileWithTime) {
                select.append(", o.\"id\" AS \"oid\", o.\"time_begin\"");
            } else if (profile) {
                select.append(", o.\"id\" AS \"oid\"");
            }
            sqlRequest.replaceFirst("m.*", select.toString());
            if (profile) {
                sqlRequest.append(" GROUP BY step, \"oid\" ORDER BY \"oid\", step");
            } else {
                sqlRequest.append(" GROUP BY step ORDER BY step");
            }
            LOGGER.fine(sqlRequest.toString());
            ResultBuilder values;
            try (final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                 final ResultSet rs            = pstmt.executeQuery()) {

                if ("resultArray".equals(responseFormat)) {
                    values = new ResultBuilder(ResultMode.DATA_ARRAY, null, false);
                } else if ("text/csv".equals(responseFormat)) {
                    values = new ResultBuilder(ResultMode.CSV, getCsvTextEncoding("2.0.0"), true);
                    // Add the header
                    values.appendHeaders(fields);
                } else {
                    values = new ResultBuilder(ResultMode.CSV, getDefaultTextEncoding("2.0.0"), false);
                }

                ResultProcessor processor = new TimeScaleResultDecimator(values, fields, profileWithTime, profile, width, fieldFilters);
                processor.processResults(rs);
            }
            switch (values.getMode()) {
                case DATA_ARRAY:  return values.getDataArray();
                case CSV:         return values.getStringValues();
                default: throw new IllegalArgumentException("Unexpected result mode");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception", ex);
        }
    }

    /**
     * extract the main field (time or other for profile observation) span and determine a step regarding the width parameter.
     *
     * return a Map with in the keys :
     *  - the procedure id for location retrieval
     *  - the observation id for profiles sensor.
     *  - a fixed value "1" for non profiles sensor (as in time series all observation are merged).
     *
     * and in the values, an array of a fixed size of 2 containing :
     *  - the mnimal value
     *  - the step value
     */
    private Map<Object, long[]> getMainFieldStep(FilterSQLRequest request, final Field mainField, final Connection c, final int width) throws SQLException {
        boolean profile = "profile".equals(currentOMType);
        boolean getLoc = OMEntity.HISTORICAL_LOCATION.equals(objectType);
        if (getLoc) {
            request.replaceFirst("SELECT hl.\"procedure\", hl.\"time\", st_asBinary(\"location\") as \"location\", hl.\"crs\" ",
                                 "SELECT MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\"), hl.\"procedure\" ");
            request.append(" group by hl.\"procedure\" order by hl.\"procedure\"");

        } else {
            if (profile) {
                request.replaceFirst("SELECT m.*", "SELECT MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\"), o.\"id\" ");
                request.append(" group by o.\"id\" order by o.\"id\"");
            } else {
                request.replaceFirst("SELECT m.*", "SELECT MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\") ");
            }
        }
        LOGGER.fine(request.toString());
        try (final PreparedStatement pstmt = request.fillParams(c.prepareStatement(request.getRequest()));
             final ResultSet rs = pstmt.executeQuery()) {
            Map<Object, long[]> results = new LinkedHashMap<>();
            while (rs.next()) {
                final long[] result = {-1L, -1L};
                if (FieldType.TIME.equals(mainField.type)) {
                    final Timestamp minT = rs.getTimestamp(1);
                    final Timestamp maxT = rs.getTimestamp(2);
                    if (minT != null && maxT != null) {
                        final long min = minT.getTime();
                        final long max = maxT.getTime();
                        result[0] = min;
                        long step = (max - min) / width;
                        /* step should always be positive
                        if (step <= 0) {
                            step = 1;
                        }*/
                        result[1] = step;
                    }
                } else if (FieldType.QUANTITY.equals(mainField.type)) {
                    final Double minT = rs.getDouble(1);
                    final Double maxT = rs.getDouble(2);
                    final long min    = minT.longValue();
                    final long max    = maxT.longValue();
                    result[0] = min;
                    long step = (max - min) / width;
                    /* step should always be positive
                    if (step <= 0) {
                        step = 1;
                    }*/
                    result[1] = step;

                } else {
                    throw new SQLException("unable to extract bound from a " + mainField.type + " main field.");
                }
                final Object key;
                if (getLoc) {
                    key = rs.getString(3);
                } else {
                    if (profile) {
                        key = rs.getInt(3);
                    } else {
                        key = 1; // single in time series
                    }
                }
                results.put(key, result);
            }
            return results;
        }
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests(final Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"foi\" = sf.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", obsJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (offJoin) {
            final String offJoin = ", \"" + schemaPrefix + "om\".\"offering_foi\" off WHERE off.\"foi\" = sf.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", offJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", offJoin + "AND ");
            }
        } else {
            sqlRequest.replaceFirst("\"foi\"='", "sf.\"id\"='");
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", "");
            }
        }
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        LOGGER.fine(sqlRequest.toString());
        final List<SamplingFeature> features = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                final String id = rs.getString("id");
                final String name = rs.getString("name");
                final String desc = rs.getString("description");
                final String sf = rs.getString("sampledfeature");
                final int srid = rs.getInt("crs");
                final byte[] b = rs.getBytes("shape");
                final CoordinateReferenceSystem crs;
                if (srid != 0) {
                    crs = CRS.forCode(SRIDGenerator.toSRS(srid, SRIDGenerator.Version.V1));
                } else {
                    crs = defaultCRS;
                }
                final org.locationtech.jts.geom.Geometry geom;
                if (b != null) {
                    WKBReader reader = new WKBReader();
                    geom = reader.read(b);
                } else {
                    geom = null;
                }
                features.add(buildFoi(version, id, name, desc, sf, geom, crs));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (FactoryException ex) {
            LOGGER.log(Level.SEVERE, "FactoryException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a Factory Exception:" + ex.getMessage(), ex);
        } catch (ParseException ex) {
            LOGGER.log(Level.SEVERE, "ParseException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a Parse Exception:" + ex.getMessage(), ex);
        }
        return features;
    }

    @Override
    public List<Phenomenon> getPhenomenons(final Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"observed_property\" = op.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", obsJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (procDescJoin) {
            final String procDescJoin = ", \"" + schemaPrefix + "om\".\"procedure_descriptions\" pd WHERE pd.\"field_name\" = op.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", procDescJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", procDescJoin + "AND ");
            }
        } else if (offJoin) {
            final String offJoin = ", \"" + schemaPrefix + "om\".\"offering_observed_properties\" off WHERE off.\"phenomenon\" = op.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", offJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", offJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", "");
            }
        }
        if (procDescJoin) {
            sqlRequest.append(" ORDER BY \"order\"");
        } else {
            sqlRequest.append(" ORDER BY \"id\"");
        }
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        final List<Phenomenon> phenomenons = new ArrayList<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                Phenomenon phen = getPhenomenon(version, rs.getString(1), c);
                phenomenons.add(phen);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        return phenomenons;
    }

    @Override
    public List<Process> getProcesses(final Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = pr.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", obsJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (offJoin) {
            final String offJoin = ", \"" + schemaPrefix + "om\".\"offerings\" off WHERE off.\"procedure\" = pr.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", offJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", offJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", "");
            }
        }
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        LOGGER.fine(sqlRequest.toString());
        final List<Process> processes = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt =  sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                processes.add(getProcess(version, rs.getString(1), c));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        return processes;
    }

    @Override
    public Map<String, Geometry> getSensorLocations(Map<String, Object> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        final String gmlVersion = getGMLVersion(version);
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = pr.\"id\" ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", obsJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", "");
            }
        }
         // will be removed when postgis filter will be set in request
        Polygon spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = JTS.toGeometry(envelopeFilter);
        }
        sqlRequest.append(" ORDER BY \"id\"");

        boolean applyPostPagination = true;
        if (spaFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        }

        Map<String, Geometry> locations = new LinkedHashMap<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                try {
                    final String procedure = rs.getString("id");
                    final byte[] b = rs.getBytes(2);
                    final int srid = rs.getInt(3);
                    final CoordinateReferenceSystem crs;
                    if (srid != 0) {
                        crs = CRS.forCode("urn:ogc:def:crs:EPSG::" + srid);
                    } else {
                        crs = defaultCRS;
                    }
                    final org.locationtech.jts.geom.Geometry geom;
                    if (b != null) {
                        WKBReader reader = new WKBReader();
                        geom             = reader.read(b);
                    } else {
                        continue;
                    }
                    // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (spaFilter != null && !spaFilter.intersects(geom)) {
                        continue;
                    }
                    final AbstractGeometry gmlGeom = JTStoGeometry.toGML(gmlVersion, geom, crs);
                    if (gmlGeom instanceof Geometry g) {
                        locations.put(procedure, g);
                    } else {
                        throw new DataStoreException("GML geometry cannot be casted as an Opengis one");
                    }
                    
                } catch (FactoryException | ParseException ex) {
                    throw new DataStoreException(ex);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        if (applyPostPagination) {
            locations = applyPostPagination(hints, locations);
        }
        return locations;
    }

    @Override
    public Map<String, Map<Date, Geometry>> getSensorHistoricalLocations(final Map<String, Object> hints) throws DataStoreException {
        Integer decimSize = getIntegerHint(hints, DECIMATION_SIZE, null);
        if (decimSize != null) {
            return getDecimatedSensorLocationsV2(hints, decimSize);
        }
        final String version = getVersionFromHints(hints);
        final String gmlVersion = getGMLVersion(version);
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        if (obsJoin) {
            String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = hl.\"procedure\" AND (";
            // profile / single date ts
            obsJoin = obsJoin + "(hl.\"time\" = o.\"time_begin\" AND o.\"time_end\" IS NULL)  OR ";
            // period observation
             obsJoin = obsJoin + "( o.\"time_end\" IS NOT NULL AND hl.\"time\" >= o.\"time_begin\" AND hl.\"time\" <= o.\"time_end\")) ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", obsJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", "");
            }
        }
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);

        LOGGER.fine(sqlRequest.toString());
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {

            SensorLocationProcessor processor = new SensorLocationProcessor(envelopeFilter, gmlVersion);
            return processor.processLocations(rs);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
    }

    /**
     * First version of decimation for Sensor Historical Location.
     *
     * We build a cube of decimSize X decimSize X decimSize  with the following dimension : Time, Latitude, longitude.
     * Each results maching the query will be put in the correspounding cell of the cube.
     *
     * Once the cube is complete, we build a entity for each non-empty cell.
     * if multiple entries are in the same cell, we build en entry by getting the centroid of the geometries union.
     *
     * This method is more efficient used with a bbox filter. if not, the whole world will be used for the cube LAT/LON boundaries.
     *
     * this method can return results for multiple procedure so, it will build one cube by procedure.
     *
     * @param hints
     * @param decimSize
     * @return
     * @throws DataStoreException
     */
    private Map<String, Map<Date, Geometry>> getDecimatedSensorLocations(final Map<String, Object> hints, int decimSize) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        final String gmlVersion = getGMLVersion(version);
        FilterSQLRequest stepRequest = sqlRequest.clone();
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);

        int nbCell = decimSize;

        GeneralEnvelope env;
        if (envelopeFilter != null) {
            env = envelopeFilter;
        } else {
            env = new GeneralEnvelope(CRS.getDomainOfValidity(defaultCRS));
        }

        try (final Connection c = source.getConnection()) {

            // calculate the first date and the time step for each procedure.
            final Map<Object, long[]> times = getMainFieldStep(stepRequest, DEFAULT_TIME_FIELD, c, nbCell);

            

            LOGGER.fine(sqlRequest.toString());
            try(final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                final ResultSet rs = pstmt.executeQuery()) {
                SensorLocationProcessor processor = new SensorLocationDecimator(envelopeFilter, gmlVersion, nbCell, times);
                return processor.processLocations(rs);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
    }

    /**
     * Second version of decimation for Sensor Historical Location.
     *
     * Much more simple version of  the decimation.
     * we split all the entries by time frame,
     * and we build an entry using centroid if multiple entries are in the same time slice.
     *
     * @param hints
     * @param decimSize
     * @return
     * @throws DataStoreException
     */
    private Map<String, Map<Date, Geometry>> getDecimatedSensorLocationsV2(final Map<String, Object> hints, int decimSize) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        final String gmlVersion = getGMLVersion(version);
        FilterSQLRequest stepRequest = sqlRequest.clone();
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);

        // will be removed when postgis filter will be set in request
        Polygon spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = JTS.toGeometry(envelopeFilter);
        }

        int nbCell = decimSize;

        try (final Connection c = source.getConnection()) {

            // calculate the first date and the time step for each procedure.
            final Map<Object, long[]> times = getMainFieldStep(stepRequest, DEFAULT_TIME_FIELD, c, nbCell);
            LOGGER.fine(sqlRequest.toString());
            try(final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                final ResultSet rs = pstmt.executeQuery()) {
                final SensorLocationProcessor processor = new SensorLocationDecimatorV2(envelopeFilter, gmlVersion, nbCell, times);
                return processor.processLocations(rs);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
    }

    @Override
    public Map<String, List<Date>> getSensorTimes(final Map<String, Object> hints) throws DataStoreException {
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        if (obsJoin) {
            String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = hl.\"procedure\" AND (";
            // profile / single date ts
            obsJoin = obsJoin + "(hl.\"time\" = o.\"time_begin\" AND o.\"time_end\" IS NULL)  OR ";
            // period observation
             obsJoin = obsJoin + "( o.\"time_end\" IS NOT NULL AND hl.\"time\" >= o.\"time_begin\" AND hl.\"time\" <= o.\"time_end\")) ";
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", obsJoin);
            } else {
                sqlRequest.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                sqlRequest.replaceFirst("WHERE", "");
            }
        }

        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        LOGGER.fine(sqlRequest.toString());
        Map<String, List<Date>> times = new LinkedHashMap<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final Date time = new Date(rs.getTimestamp("time").getTime());

                final List<Date> procedureTimes;
                if (times.containsKey(procedure)) {
                    procedureTimes = times.get(procedure);
                } else {
                    procedureTimes = new ArrayList<>();
                    times.put(procedure, procedureTimes);
                }
                procedureTimes.add(time);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        return times;
    }

    @Override
    public void setResponseFormat(final String responseFormat) {
        this.responseFormat = responseFormat;
    }

    @Override
    public boolean computeCollectionBound() {
        return false;
    }

    @Override
    public org.geotoolkit.gml.xml.Envelope getCollectionBoundingShape() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
