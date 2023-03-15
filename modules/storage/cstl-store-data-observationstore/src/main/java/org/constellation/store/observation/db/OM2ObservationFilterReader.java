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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.result.ResultBuilder;
import org.constellation.util.FilterSQLRequest.TableJoin;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.ResultMode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.metadata.quality.Element;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.observation.Process;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2ObservationFilterReader extends OM2ObservationFilter {

    public OM2ObservationFilterReader(final OM2ObservationFilter omFilter) {
        super(omFilter);
    }

    public OM2ObservationFilterReader(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties, final boolean timescaleDB) throws DataStoreException {
        super(source, isPostgres, schemaPrefix, properties, timescaleDB);
    }

    @Override
    public List<org.opengis.observation.Observation> getObservations() throws DataStoreException {
        if (ResponseMode.RESULT_TEMPLATE.equals(responseMode)) {
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                return getMesurementTemplates();
            } else {
                return getObservationTemplates();
            }
        } else if (ResponseMode.INLINE.equals(responseMode)) {
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                return getMesurements();
            } else {
                return getComplexObservations();
            }
        } else {
            throw new DataStoreException("Unsupported response mode:" + responseMode);
        }
    }

    private List<org.opengis.observation.Observation> getObservationTemplates() throws DataStoreException {
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\" ");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        final List<org.opengis.observation.Observation> observations = new ArrayList<>();
        final Map<String, Procedure> processMap = new HashMap<>();

        LOGGER.fine(sqlRequest.getRequest());
        try (final Connection c            = source.getConnection();
             final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
             final ResultSet rs            = pstmt.executeQuery()) {

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
                final Phenomenon phen = getGlobalCompositePhenomenon(c, procedure);
                String featureID = null;
                SamplingFeature feature = null;
                if (includeFoiInTemplate) {
                    featureID = rs.getString("foi");
                    feature = getFeatureOfInterest(featureID, c);
                }
                TemporalGeometricPrimitive tempTime = null;
                if (includeTimeInTemplate) {
                    tempTime = getTimeForTemplate(c, procedure, null, featureID);
                }
                List<Field> fields = readFields(procedure, c);
                Field mainField = getMainField(procedure, c);

                Map<String, Object> properties = new HashMap<>();
                if (mainField.type == FieldType.TIME) {
                    properties.put("type", "timeseries");
                } else {
                    properties.put("type", "profile");
                }
                final Procedure proc = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});
                /*
                 *  BUILD RESULT
                 */
                
                final ComplexResult result = new ComplexResult(fields, DEFAULT_ENCODING, null, null);
                Observation observation = new Observation(obsID,
                                                          name,
                                                          null, null,
                                                          "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation",
                                                          proc,
                                                          tempTime,
                                                          feature,
                                                          phen,
                                                          null,
                                                          result,
                                                          properties);
                observations.add(observation);
            }


        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw a Runtime Exception:" + ex.getMessage(), ex);
        }
        return observations;
    }

    private List<org.opengis.observation.Observation> getMesurementTemplates() throws DataStoreException {
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\", pd.\"order\" ");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        final List<org.opengis.observation.Observation> observations = new ArrayList<>();
        final Map<String, Procedure> processMap = new HashMap<>();
        final Map<String, Map<String, Object>> obsPropMap  = new HashMap<>();

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
                if (!obsPropMap.containsKey(procedure)) {
                    final Field mainField = getMainField(procedure, c);
                    Map<String, Object> properties = new HashMap<>();
                    if (mainField.type == FieldType.TIME) {
                        properties.put("type", "timeseries");
                    } else {
                        properties.put("type", "profile");
                    }
                    obsPropMap.put(procedure, properties);
                }
                
                final String obsID = "obs-" + procedureID;
                final String name = observationTemplateIdBase + procedureID;
                final String observedProperty = rs.getString("obsprop");
                final int phenIndex   = rs.getInt("order");
                final Field field     = getFieldByIndex(procedure, phenIndex, true, c);

                // skip the main field for timeseries
                if (phenIndex == 1 && field.type == FieldType.TIME) {
                    continue;
                }
                
                final Phenomenon phen = getPhenomenon(observedProperty, c);
                String featureID = null;
                SamplingFeature feature = null;
                if (includeFoiInTemplate) {
                    featureID = rs.getString("foi");
                    feature = getFeatureOfInterest(featureID, c);
                }

                /*
                 *  BUILD RESULT
                 */
                final Procedure proc = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});
                
                TemporalGeometricPrimitive tempTime = null;
                if (includeTimeInTemplate) {
                    tempTime = getTimeForTemplate(c, procedure, observedProperty, featureID);
                }
                final String observationType      = getOmTypeFromFieldType(field.type);
                MeasureResult result              = new MeasureResult(field, null);
                Map<String, Object> properties    = obsPropMap.get(procedure);
                final List<Element> resultQuality = buildResultQuality(field, null);
                Observation observation = new Observation(obsID + '-' + phenIndex,
                                                          name + '-' + phenIndex,
                                                          null, null,
                                                          observationType,
                                                          proc,
                                                          tempTime,
                                                          feature,
                                                          phen,
                                                          resultQuality,
                                                          result,
                                                          properties);
                observations.add(observation);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw a Runtime Exception:" + ex.getMessage(), ex);
        }
        return observations;
    }

    private List<org.opengis.observation.Observation> getComplexObservations() throws DataStoreException {
        final Map<String, Observation> observations = new LinkedHashMap<>();
        final Map<String, Procedure> processMap       = new LinkedHashMap<>();
        final Map<String, List<Field>> fieldMap     = new LinkedHashMap<>();
        if (resultMode == null) {
            resultMode = ResultMode.CSV;
        }
        final ResultBuilder values          = new ResultBuilder(resultMode, DEFAULT_ENCODING, false);
        sqlRequest.append(" ORDER BY o.\"time_begin\"");
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        LOGGER.fine(sqlRequest.getRequest());
        try(final Connection c              = source.getConnection();
            final PreparedStatement pstmt   = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs              = pstmt.executeQuery()) {

            while (rs.next()) {
                int nbValue = 0;
                final String procedure   = rs.getString("procedure");
                final String featureID   = rs.getString("foi");
                final int oid            = rs.getInt("id");
                Observation observation  = observations.get(procedure + '-' + featureID);
                final String measureJoin = getMeasureTableJoin(getPIDFromProcedure(procedure, c));
                final Field mainField    = getMainField(procedure, c);
                boolean profile          = !FieldType.TIME.equals(mainField.type);
                final boolean profileWithTime = profile && includeTimeForProfile;

                Map<String, Object> properties = new HashMap<>();
                if (!profile) {
                    properties.put("type", "timeseries");
                } else {
                    properties.put("type", "profile");
                }
                
               /*
                * Compute procedure fields
                */
                List<Field> fields = fieldMap.get(procedure);
                if (fields == null) {
                    if (!currentFields.isEmpty()) {
                        fields = new ArrayList<>();
                        fields.add(mainField);

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

                    // add the time for profile in the dataBlock if requested
                    if (profileWithTime) {
                        fields.add(0, new Field(0, FieldType.TIME, "time_begin", "time", "time", null));
                    }
                    // add the result id in the dataBlock if requested
                    if (includeIDInDataBlock) {
                        fields.add(0, new Field(0, FieldType.TEXT, "id", "measure identifier", "measure identifier", null));
                    }
                    fieldMap.put(procedure, fields);
                }

               /*
                * Compute procedure measure request
                */
                int offset = getFieldsOffset(profile, profileWithTime, includeIDInDataBlock);
                FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(offset, mainField, fields);
                
                final FilterSQLRequest measureRequest = new FilterSQLRequest("SELECT * FROM " + measureJoin + " WHERE m.\"id_observation\" = ");
                measureRequest.appendValue(-1).append(measureFilter, !profile).append("ORDER BY ").append("m.\"" + mainField.name + "\"");

                final String name = rs.getString("identifier");
                final FieldParser parser = new FieldParser(fields, values, profileWithTime, includeIDInDataBlock, includeQualityFields, name);

                if (observation == null) {
                    final String obsID            = "obs-" + oid;
                    final SamplingFeature feature = getFeatureOfInterest(featureID,  c);
                    final Phenomenon phen         = getGlobalCompositePhenomenon(c, procedure);

                    parser.firstTime = dateFromTS(rs.getTimestamp("time_begin"));
                    parser.lastTime = dateFromTS(rs.getTimestamp("time_end"));

                    /*
                     *  BUILD RESULT
                     */
                    measureRequest.setParamValue(0, oid);
                    LOGGER.fine(measureRequest.getRequest());
                    try(final PreparedStatement stmt = measureRequest.fillParams(c.prepareStatement(measureRequest.getRequest()));
                        final ResultSet rs2 = stmt.executeQuery()) {
                        while (rs2.next()) {
                            parser.parseLine(rs2, offset);
                            nbValue = nbValue + parser.nbValue;

                            /**
                             * In "separated observation" mode we create an observation for each measure and don't merge it into a single obervation by procedure/foi.
                             */
                            if (separatedObs) {
                                final TemporalGeometricPrimitive time = buildTime(obsID, parser.lastTime != null ? parser.lastTime : parser.firstTime, null);
                                final ComplexResult result = buildComplexResult(fields, nbValue, DEFAULT_ENCODING, values);
                                final Procedure proc = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});
                                final String measureID = rs2.getString("id");
                                final String singleObsID = "obs-" + oid + '-' + measureID;
                                final String singleName  = name + '-' + measureID;
                                observation = new Observation(singleObsID,
                                                              singleName,
                                                              null, null,
                                                              "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation",
                                                              proc,
                                                              time,
                                                              feature,
                                                              phen,
                                                              null,
                                                              result,
                                                              properties);
                                observations.put(procedure + '-' + name + '-' + measureID, observation);
                                values.clear();
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
                        final TemporalGeometricPrimitive time = buildTime(obsID, parser.firstTime, parser.lastTime);
                        final ComplexResult result = buildComplexResult(fields, nbValue, DEFAULT_ENCODING, values);
                        final Procedure proc = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});
                        observation = new Observation(obsID,
                                                      name,
                                                      null, null,
                                                      "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation",
                                                      proc,
                                                      time,
                                                      feature,
                                                      phen,
                                                      null,
                                                      result,
                                                      properties);
                        observations.put(procedure + '-' + featureID, observation);
                    }
                } else {
                    /**
                     * complete the previous observation with new measures.
                     */
                    measureRequest.setParamValue(0, oid);
                    LOGGER.fine(measureRequest.toString());
                    try(final PreparedStatement stmt = measureRequest.fillParams(c.prepareStatement(measureRequest.getRequest()));
                        final ResultSet rs2 = stmt.executeQuery()) {
                        while (rs2.next()) {
                            parser.parseLine(rs2, offset);
                            nbValue = nbValue + parser.nbValue;
                        }
                    }

                    // update observation result and sampling time
                    ComplexResult cr = (ComplexResult) observation.getResult();
                    cr.setNbValues(cr.getNbValues() + nbValue);
                    switch (resultMode) {
                        case DATA_ARRAY: cr.getDataArray().addAll(values.getDataArray()); break;
                        case CSV:        cr.setValues(cr.getValues() + values.getStringValues()); break;
                    }
                   observation.extendSamplingTime(parser.lastTime);
                }
                values.clear();
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw a Runtime Exception:" + ex.getMessage(), ex);
        }
        
        // TODO make a real pagination
        return applyPostPagination(new ArrayList<>(observations.values()));
    }

    private List<org.opengis.observation.Observation> getMesurements() throws DataStoreException {
        // add orderby to the query
        sqlRequest.append(" ORDER BY o.\"time_begin\"");
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }

        final List<Observation> observations = new ArrayList<>();
        final Map<String, Procedure> processMap = new HashMap<>();
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
                final String featureID = rs.getString("foi");
                final String observedProperty = rs.getString("observed_property");
                final SamplingFeature feature = getFeatureOfInterest(featureID, c);
                final Phenomenon phen = getPhenomenon(observedProperty, c);
                final String measureJoin   = getMeasureTableJoin(getPIDFromProcedure(procedure, c));
                final List<Field> fields = readFields(procedure, true, c);
                final Map<Field, Phenomenon> fieldPhen = getPhenomenonFields(phen, fields, c);
                final Procedure proc = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});
                final TemporalGeometricPrimitive time = buildTime(obsID, startTime, endTime);

                /*
                 *  BUILD RESULT
                 */
                final Field mainField = getMainField(procedure, c);
                boolean notProfile    = FieldType.TIME.equals(mainField.type);
                Map<String, Object> properties = new HashMap<>();
                if (notProfile) {
                    properties.put("type", "timeseries");
                } else {
                    properties.put("type", "profile");
                }

                final FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(0, mainField, new ArrayList<>(fieldPhen.keySet()));

                final FilterSQLRequest measureRequest = new FilterSQLRequest("SELECT * FROM " + measureJoin + " WHERE m.\"id_observation\" = ");
                measureRequest.appendValue(oid).append(" ").append(measureFilter, notProfile);
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
                                measureTime = buildTime(oid + "-" + rid, mt, null);
                            } else {
                                measureTime = time;
                            }

                            for (Entry<Field, Phenomenon> entry : fieldPhen.entrySet()) {
                                Phenomenon fphen = entry.getValue();
                                Field field      = entry.getKey();
                                FieldType fType  = field.type;
                                String fName     = field.name;
                                final String observationType = getOmTypeFromFieldType(fType);
                                final String value = rs2.getString(fName);
                                if (value != null) {
                                    Object resultValue;
                                    if (fType == FieldType.QUANTITY) {
                                        resultValue = rs2.getDouble(fName);
                                    } else if (fType == FieldType.BOOLEAN) {
                                        resultValue = rs2.getBoolean(fName);
                                    } else if (fType == FieldType.TIME) {
                                        Timestamp ts = rs2.getTimestamp(fName);
                                        resultValue = new Date(ts.getTime());
                                    } else {
                                        resultValue = value;
                                    }
                                    MeasureResult result = new MeasureResult(field, resultValue);
                                    final String measId =  obsID + '-' + field.index + '-' + rid;
                                    final String measName = name + '-' + field.index + '-' + rid;
                                    List<Element> resultQuality = buildResultQuality(field, rs2);
                                    Observation observation = new Observation(measId,
                                                                              measName,
                                                                              null, null,
                                                                              observationType,
                                                                              proc,
                                                                              measureTime,
                                                                              feature,
                                                                              fphen,
                                                                              resultQuality,
                                                                              result,
                                                                              properties);
                                    observations.add(observation);
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
            throw new DataStoreException("the service has throw an Exception:" + ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw a Runtime Exception:" + ex.getMessage(), ex);
        }
        // TODO make a real pagination
        return applyPostPagination(observations);
    }

    /**
     * Return the index of the first measure fields.
     *
     * @param profile Set to {@code true} if the current observation is a Profile.
     * @param profileWithTime Set to {@code true} if the time has to be included in the result for a profile.
     * @param includeIDInDataBlock Set to {@code true} if the measure identifier has to be included in the result.
     * 
     * @return The index where starts the measure fields.
     */
    private  int getFieldsOffset(boolean profile, boolean profileWithTime, boolean includeIDInDataBlock) {
        int offset = profile ? 0 : 1; // for profile, the first phenomenon field is the main field
        if (profileWithTime) {
            offset++;
        }
        if (includeIDInDataBlock) {
            offset++;
        }
        return offset;
    }

    @Override
    public Object getResults() throws DataStoreException {
        if (ResponseMode.OUT_OF_BAND.equals(responseMode)) {
            throw new ObservationStoreException("Out of band response mode has not been implemented yet", NO_APPLICABLE_CODE, RESPONSE_MODE);
        }
        final boolean countRequest         = "count".equals(responseFormat);
        boolean includeTimeForProfile      = !countRequest && this.includeTimeForProfile;
        final boolean profile              = "profile".equals(currentOMType);
        final boolean profileWithTime      = profile && includeTimeForProfile;
        if (decimationSize != null && !countRequest) {
            if (timescaleDB) {
                return getDecimatedResultsTimeScale();
            } else {
                return getDecimatedResults();
            }
        }
        try (final Connection c = source.getConnection()) {
            /**
             *  1) build field list.
             */
            final Field mainField = getMainField(currentProcedure);
            
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
                List<Field> allfields = readFields(currentProcedure, c);
                phenFields = reOrderFields(allfields, phenFields);
                fields.addAll(phenFields);

            } else {
                fields = readFields(currentProcedure, c);
            }
            // add the time for profile in the dataBlock if requested
            if (profileWithTime) {
                fields.add(0, new Field(0, FieldType.TIME, "time_begin", "time", "time", null));
            }
            // add the result id in the dataBlock if requested
            if (includeIDInDataBlock) {
                fields.add(0, new Field(0, FieldType.TEXT, "id", "measure identifier", "measure identifier", null));
            }

            /**
             *  2) complete SQL request.
             */
            int offset = getFieldsOffset(profile, profileWithTime, includeIDInDataBlock);
            FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(offset, mainField, fields);
            sqlRequest.append(measureFilter);
            
            final String fieldOrdering;
            if (mainField != null) {
                fieldOrdering = "m.\"" + mainField.name + "\"";
            } else {
                // we keep this fallback where no main field is found.
                // i'm not sure it will be possible to handle an observation with no main field (meaning its not a timeseries or a profile).
                fieldOrdering = "m.\"id\"";
            }
            StringBuilder select  = new StringBuilder("m.*");
            if (profile) {
                select.append(", o.\"id\" as oid ");
            }
            if (profileWithTime) {
                select.append(", o.\"time_begin\" ");
            }
            if (includeIDInDataBlock) {
                select.append(", o.\"identifier\" ");
            }
            sqlRequest.replaceFirst("m.*", select.toString());
            sqlRequest.append(" ORDER BY  o.\"time_begin\", ").append(fieldOrdering);

            if (firstFilter) {
                return sqlRequest.replaceFirst("WHERE", "");
            }
            LOGGER.fine(sqlRequest.toString());

            /**
             * 3) Extract results.
             */
            ResultProcessor processor = new ResultProcessor(fields, profile, includeIDInDataBlock, includeQualityFields);
            ResultBuilder values = processor.initResultBuilder(responseFormat, countRequest);
            try (final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                 final ResultSet rs = pstmt.executeQuery()) {
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

    private Object getDecimatedResults() throws DataStoreException {
        final boolean profile = "profile".equals(currentOMType);
        final boolean profileWithTime = profile && includeTimeForProfile;
        try (final Connection c = source.getConnection()) {

            /**
             *  1) build field list.
             */
            final List<Field> fields = new ArrayList<>();
            final List<Field> allfields = readFields(currentProcedure, c);
            fields.add(allfields.get(0));
            for (int i = 1; i < allfields.size(); i++) {
                Field f = allfields.get(i);
                 if (isIncludedField(f.name, f.description, f.index)) {
                     fields.add(f);
                 }
            }

            // add the time for profile in the dataBlock if requested
            if (profileWithTime) {
                fields.add(0, new Field(0, FieldType.TIME, "time_begin", "time", "time", null));
            }
            // add the result id in the dataBlock if requested
            if (includeIDInDataBlock) {
                fields.add(0, new Field(0, FieldType.TEXT, "id", "measure identifier", "measure identifier", null));
            }
            final Field mainField = getMainField(currentProcedure, c);

            /**
             *  2) complete SQL request.
             */
            int offset = getFieldsOffset(profile, profileWithTime, includeIDInDataBlock);
            FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(offset, mainField, fields);
            sqlRequest.append(measureFilter);
            
            final FilterSQLRequest fieldRequest = sqlRequest.clone();
            final String fieldOrdering;
            if (mainField != null) {
                fieldOrdering = "m.\"" + mainField.name + "\"";
            } else {
                // we keep this fallback where no main field is found.
                // i'm not sure it will be possible to handle an observation with no main field (meaning its not a timeseries or a profile).
                fieldOrdering = "m.\"id\"";
            }
            sqlRequest.append(" ORDER BY  o.\"time_begin\", ").append(fieldOrdering);
            StringBuilder select  = new StringBuilder("m.*");
            if (profile) {
                select.append(", o.\"id\" as oid ");
            }
            if (profileWithTime) {
                select.append(", o.\"time_begin\" ");
            }
            if (includeIDInDataBlock) {
                select.append(", o.\"identifier\" ");
            }
            sqlRequest.replaceFirst("m.*", select.toString());
            LOGGER.fine(sqlRequest.toString());
            
            /**
             * 3) Extract results.
             */
            final Map<Object, long[]> times = getMainFieldStep(fieldRequest, mainField, c, decimationSize);
            final int mainFieldIndex = fields.indexOf(mainField);
            ResultProcessor processor = new DefaultResultDecimator(fields, profile, includeIDInDataBlock, decimationSize, fieldFilters, mainFieldIndex, currentProcedure, times);
            ResultBuilder values = processor.initResultBuilder(responseFormat, false);

            try (final PreparedStatement pstmt    = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                 final ResultSet rs = pstmt.executeQuery()) {
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

    private Object getDecimatedResultsTimeScale() throws DataStoreException {
        final boolean profile = "profile".equals(currentOMType);
        final boolean profileWithTime = profile && includeTimeForProfile;
        try(final Connection c = source.getConnection()) {

            /**
             *  1) build field list.
             */
            final List<Field> fields = new ArrayList<>();
            final List<Field> allfields = readFields(currentProcedure, c);
            fields.add(allfields.get(0));
            for (int i = 1; i < allfields.size(); i++) {
                Field f = allfields.get(i);
                 if (isIncludedField(f.name, f.description, f.index)) {
                     fields.add(f);
                 }
            }
            final Field mainField = getMainField(currentProcedure, c);
            // add the time for profile in the dataBlock if requested
            if (profileWithTime) {
                fields.add(0, new Field(0, FieldType.TIME, "time_begin", "time", "time", null));
            }
            // add the result id in the dataBlock if requested
            if (includeIDInDataBlock) {
                fields.add(0, new Field(0, FieldType.TEXT, "id", "measure identifier", "measure identifier", null));
            }
            
            /**
             *  2) complete SQL request.
             */
            int offset = getFieldsOffset(profile, profileWithTime, includeIDInDataBlock);
            FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(offset, mainField, fields);
            // TODO the measure filter is not used ? need testing

            // calculate step
            final Map<Object, long[]> times = getMainFieldStep(sqlRequest.clone(), fields.get(0), c, decimationSize);
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
            select.append(mainField.name).append("\") AS step");
            for (int i = offset; i < fields.size(); i++) {
                 select.append(", avg(\"").append(fields.get(i).name).append("\") AS \"").append(fields.get(i).name).append("\"");
            }
            if (profile) {
                select.append(", o.\"id\" as \"oid\" ");
            }
            if (profileWithTime) {
                select.append(", o.\"time_begin\" ");
            }
            if (includeIDInDataBlock) {
                select.append(", o.\"identifier\" ");
            }
            sqlRequest.replaceFirst("m.*", select.toString());
            if (profile) {
                sqlRequest.append(" GROUP BY step, \"oid\" ORDER BY \"oid\", step");
            } else {
                sqlRequest.append(" GROUP BY step ORDER BY step");
            }
            LOGGER.fine(sqlRequest.toString());

            /**
             * 3) Extract results.
             */
            final int mainFieldIndex = fields.indexOf(mainField);
            ResultProcessor processor = new TimeScaleResultDecimator(fields, profile, includeIDInDataBlock, decimationSize, fieldFilters, mainFieldIndex, currentProcedure);
            ResultBuilder values = processor.initResultBuilder(responseFormat, false);
            try (final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                 final ResultSet rs            = pstmt.executeQuery()) {
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
                                 "SELECT MIN(\"" + mainField.name + "\") as tmin, MAX(\"" + mainField.name + "\") as tmax, hl.\"procedure\" ");
            request.append(" group by hl.\"procedure\" order by hl.\"procedure\"");

        } else {
            if (profile) {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\"), o.\"id\" ");
                request.append(" group by o.\"id\" order by o.\"id\"");
            } else {
                request.replaceSelect(" MIN(\"" + mainField.name + "\"), MAX(\"" + mainField.name + "\") ");
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
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0}", request.toString());
            throw ex;
        }
    }

    @Override
    public List<org.opengis.observation.sampling.SamplingFeature> getFeatureOfInterests() throws DataStoreException {
        List<FilterSQLRequest.TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"foi\" = sf.\"id\""));
        }
        if (offJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_foi\" off", "off.\"foi\" = sf.\"id\""));
        }
        if (foiPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"sampling_features_properties\" sfp", "sf.\"id\" = sfp.\"id_sampling_feature\""));
        }
        if (procPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offerings\" offe", "off.\"id_offering\" = offe.\"identifier\""));
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedures_properties\" prp", "prp.\"id_procedure\" = offe.\"procedure\""));
        }
        if (phenPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" offop", "offop.\"id_offering\" = off.\"id_offering\""));
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = offop.\"phenomenon\""));
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        final List<org.opengis.observation.sampling.SamplingFeature> features = new ArrayList<>();
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
                final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                final org.locationtech.jts.geom.Geometry geom;
                if (b != null) {
                    WKBReader reader = new WKBReader();
                    geom = reader.read(b);
                    JTS.setCRS(geom, crs);
                } else {
                    geom = null;
                }
                final Map<String, Object> properties = readProperties("sampling_features_properties", "id_sampling_feature", id, c);
                features.add(new SamplingFeature(id, name, desc, properties, sf, geom));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (FactoryException ex) {
            LOGGER.log(Level.WARNING, "FactoryException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a Factory Exception:" + ex.getMessage(), ex);
        } catch (ParseException ex) {
            LOGGER.log(Level.WARNING, "ParseException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a Parse Exception:" + ex.getMessage(), ex);
        }
        return features;
    }

    @Override
    public List<org.opengis.observation.Phenomenon> getPhenomenons() throws DataStoreException {
        List<FilterSQLRequest.TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"observed_property\" = op.\"id\""));
        }
        if (offJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" off", "off.\"phenomenon\" = op.\"id\""));
        }
        if (procDescJoin) {
            // in this case no composite will appears in the results. so no need for an SQL union later
            noCompositePhenomenon = false;
            sqlRequest.replaceFirst("DISTINCT(op.\"id\")", "op.\"id\"");
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedure_descriptions\" pd", "pd.\"field_name\" = op.\"id\""));
        }
        if (phenPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = op.\"id\""));
        }
        if (procPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedures_properties\" prp", "prp.\"id_procedure\" = o.\"procedure\""));
        }
        if (foiPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"sampling_features_properties\" sfp", "sfp.\"id_sampling_feature\" = o.\"foi\""));
        }
        
        /*
         * build UNION request
         *
         * simple phenomenon directly in observation table
         * +
         * decomposed composite phenomenon
         */
        if (noCompositePhenomenon) {
            FilterSQLRequest secondRequest = sqlRequest.clone();
            secondRequest.replaceFirst("op.\"id\"", "c.\"component\"");
            List<FilterSQLRequest.TableJoin> joins2 = new ArrayList<>(joins);
            joins2.add(new TableJoin("\"" + schemaPrefix +"om\".\"components\" c", "c.\"phenomenon\" = op.\"id\""));
            secondRequest.join(joins2, firstFilter);
            secondRequest.replaceFirst("op.\"id\" NOT IN (SELECT \"phenomenon\"", "op.\"id\" IN (SELECT \"phenomenon\"");

            sqlRequest.join(joins, firstFilter);

            sqlRequest.append(" UNION ").append(secondRequest);
        } else {
            sqlRequest.join(joins, firstFilter);
        }

        if (procDescJoin) {
            sqlRequest.append(" ORDER BY \"order\"");
        } else {
            sqlRequest.append(" ORDER BY \"id\"");
        }
        sqlRequest = appendPaginationToRequest(sqlRequest);
        final List<org.opengis.observation.Phenomenon> phenomenons = new ArrayList<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                phenomenons.add(getPhenomenon(rs.getString(1), c));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        return phenomenons;
    }

    @Override
    public List<Process> getProcesses() throws DataStoreException {
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = pr.\"id\""));
        }
        if (offJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offerings\" off", "off.\"procedure\" = pr.\"id\""));
        }
        if (procPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedures_properties\" prp", "prp.\"id_procedure\" = pr.\"id\""));
        }
        if (phenPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" offop", "offop.\"id_offering\" = off.\"identifier\""));
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = offop.\"phenomenon\""));
        }
        if (foiPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_foi\" offf", "offf.\"id_offering\" = off.\"identifier\""));
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"sampling_features_properties\" sfp", "offf.\"foi\" = sfp.\"id_sampling_feature\""));
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        final List<Process> results = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt =  sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                results.add(getProcess(rs.getString(1), c));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        return results;
    }

    @Override
    public List<Offering> getOfferings() throws DataStoreException {
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = off.\"procedure\""));
        }
        if (procJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"procedures\" pr", "off.\"procedure\" = pr.\"id\""));
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        final List<Offering> results = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt =  sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                results.add(readObservationOffering(rs.getString(1), c));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        return results;
    }

    @Override
    public Map<String, Geometry> getSensorLocations() throws DataStoreException {
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = pr.\"id\""));
        }
        sqlRequest.join(joins, firstFilter);
        
         // will be removed when postgis filter will be set in request
        Polygon spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = JTS.toGeometry(envelopeFilter);
        }
        sqlRequest.append(" ORDER BY \"id\"");

        boolean applyPostPagination = true;
        if (spaFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest);
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
                    final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                    final org.locationtech.jts.geom.Geometry geom;
                    if (b != null) {
                        WKBReader reader = new WKBReader();
                        geom             = reader.read(b);
                        JTS.setCRS(geom, crs);
                    } else {
                        continue;
                    }
                    // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (spaFilter != null && !spaFilter.intersects(geom)) {
                        continue;
                    }
                    locations.put(procedure, geom);
                    
                } catch (FactoryException | ParseException ex) {
                    throw new DataStoreException(ex);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        if (applyPostPagination) {
            locations = applyPostPagination(locations);
        }
        return locations;
    }

    @Override
    public Map<String, Map<Date, Geometry>> getSensorHistoricalLocations() throws DataStoreException {
        if (decimationSize != null) {
            return getDecimatedSensorLocationsV2(decimationSize);
        }
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {

            // basic join statement
            String obsStmt = "o.\"procedure\" = hl.\"procedure\"";
            
            // TODO, i can't remember what is the purpose of this piece of sql request
            obsStmt = obsStmt + "AND (";
            // profile / single date ts
            obsStmt = obsStmt + "(hl.\"time\" = o.\"time_begin\" AND o.\"time_end\" IS NULL)  OR ";
            // period observation
            obsStmt = obsStmt + "( o.\"time_end\" IS NOT NULL AND hl.\"time\" >= o.\"time_begin\" AND hl.\"time\" <= o.\"time_end\")";
            obsStmt = obsStmt + ")";

            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", obsStmt));
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        LOGGER.fine(sqlRequest.toString());
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {

            SensorLocationProcessor processor = new SensorLocationProcessor(envelopeFilter);
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
     * @return
     * @throws DataStoreException
     */
    private Map<String, Map<Date, Geometry>> getDecimatedSensorLocations() throws DataStoreException {
        FilterSQLRequest stepRequest = sqlRequest.clone();
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        int nbCell = decimationSize;

        try (final Connection c = source.getConnection()) {

            // calculate the first date and the time step for each procedure.
            final Map<Object, long[]> times = getMainFieldStep(stepRequest, DEFAULT_TIME_FIELD, c, nbCell);

            LOGGER.fine(sqlRequest.toString());
            try(final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                final ResultSet rs = pstmt.executeQuery()) {
                SensorLocationProcessor processor = new SensorLocationDecimator(envelopeFilter, nbCell, times);
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
    private Map<String, Map<Date, Geometry>> getDecimatedSensorLocationsV2(int decimSize) throws DataStoreException {
        FilterSQLRequest stepRequest = sqlRequest.clone();
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        int nbCell = decimationSize;
        try (final Connection c = source.getConnection()) {

            // calculate the first date and the time step for each procedure.
            final Map<Object, long[]> times = getMainFieldStep(stepRequest, DEFAULT_TIME_FIELD, c, nbCell);
            LOGGER.fine(sqlRequest.toString());
            try(final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                final ResultSet rs = pstmt.executeQuery()) {
                final SensorLocationProcessor processor = new SensorLocationDecimatorV2(envelopeFilter, nbCell, times);
                return processor.processLocations(rs);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
    }

    @Override
    public Map<String, Set<Date>> getSensorHistoricalTimes() throws DataStoreException {
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {

            // basic join statement
            String obsStmt = "o.\"procedure\" = hl.\"procedure\"";

            // TODO, i can't remember what is the purpose of this piece of sql request
            obsStmt = obsStmt + "AND (";
            // profile / single date ts
            obsStmt = obsStmt + "(hl.\"time\" = o.\"time_begin\" AND o.\"time_end\" IS NULL)  OR ";
            // period observation
            obsStmt = obsStmt + "( o.\"time_end\" IS NOT NULL AND hl.\"time\" >= o.\"time_begin\" AND hl.\"time\" <= o.\"time_end\")";
            obsStmt = obsStmt + ")";

            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", obsStmt));
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest.append(" ORDER BY \"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        Map<String, Set<Date>> times = new LinkedHashMap<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final Date time = new Date(rs.getTimestamp("time").getTime());

                final Set<Date> procedureTimes = times.computeIfAbsent(procedure, f -> {return new LinkedHashSet<>();});
                procedureTimes.add(time);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw a Runtime Exception:" + ex.getMessage(), ex);
        }
        return times;
    }

    @Override
    public org.geotoolkit.gml.xml.Envelope getCollectionBoundingShape() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
