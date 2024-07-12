/*
 *    Examind Community An open source and standard compliant SDI
 *    https://community.examind.com
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

import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.decimation.SensorLocationDecimatorV2;
import org.constellation.store.observation.db.decimation.SensorLocationDecimator;
import org.constellation.store.observation.db.decimation.DefaultResultDecimator;
import org.constellation.store.observation.db.decimation.BucketTimeScaleResultDecimator;
import org.constellation.store.observation.db.decimation.ASMTimeScaleResultDecimator;
import org.constellation.util.FilterSQLRequest;
import org.locationtech.jts.io.ParseException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import org.apache.sis.util.Version;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.result.ResultBuilder;
import org.constellation.util.FilterSQLRequest.TableJoin;
import org.constellation.util.MultiFilterSQLRequest;
import org.constellation.util.SQLResult;
import org.geotoolkit.geometry.GeometricUtilities;
import org.geotoolkit.geometry.GeometricUtilities.WrapResolution;
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
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.ResultMode;
import org.locationtech.jts.geom.Geometry;
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
    
    protected static final Version MIN_TIMESCALE_VERSION_SMOOTH = new Version("1.11.0");

    public OM2ObservationFilterReader(final OM2ObservationFilter omFilter) {
        super(omFilter);
    }

    public OM2ObservationFilterReader(final DataSource source, final Map<String, Object> properties) throws DataStoreException {
        super(source, properties);
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
        List<FilterSQLRequest.TableJoin> joins = new ArrayList<>();
        if (phenPropJoin) {
            /*
             * PROBLEM HERE: if we look for a property on a simple phenomenon :
             *
             * - we will lost the templates with composite phenomenon containing a component matching the property.
             * - we will include those with multiple observation with a least one with the single phenomenon.
             *
             * I dont know what response is the more correct.
             * - return a full observation template as long as one of its components match the property.
             * - exclude the template if not all components match the property.
             * - build a trunctated template with only the matching components.
             *
             * Anyway i let this piece of sql here so i don't forget it, for solution number one:
             * replace the join by :
             (o."observed_property" in (
                    SELECT cmp.phenomenon
                    FROM  "om"."components" cmp,"om"."observed_properties_properties" opp
                    where  cmp."component" = opp.id_phenomenon
                    AND opp."property_name"= 'propName'  AND opp."value" = ' propValue')
             ) OR (
               o."observed_property" in (
                    select op.id
                    FROM  "om".observed_properties op ,"om"."observed_properties_properties" opp
                    where  op."id" = opp.id_phenomenon
                    AND opp."property_name"= 'propName'  AND opp."value" = ' propValue')
            )
            
            UPDATE there was kind of a change here, the request is now almost like the suggestion above. TODO verify that the problem is still here
             */
            sqlRequest.replaceAll("${phen-prop-join}", "o.\"observed_property\"");
        }
        if (procPropJoin) {
            sqlRequest.replaceAll("${proc-prop-join}", "o.\"procedure\"");
        }
        if (foiPropJoin) {
            sqlRequest.replaceAll("${foi-prop-join}", "o.\"foi\"");
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest.append(" ORDER BY o.\"procedure\" ");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        final List<org.opengis.observation.Observation> observations = new ArrayList<>();
        final Map<String, Procedure> processMap = new HashMap<>();

        LOGGER.fine(sqlRequest.toString());
        try (final Connection c = source.getConnection();
             final SQLResult rs = sqlRequest.execute(c)) {

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

                Map<String, Object> properties = new HashMap<>();
                properties.put("type", getProcedureOMType(procedure, c));
                
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


        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw an Exception.", ex);
        }
        return observations;
    }

    private List<org.opengis.observation.Observation> getMesurementTemplates() throws DataStoreException {
        List<FilterSQLRequest.TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            String obsFrom = ",\"" + schemaPrefix + "om\".\"observations\" o  LEFT JOIN \"" + schemaPrefix + "om\".\"components\" c ON o.\"observed_property\" = c.\"phenomenon\"";
            sqlRequest.replaceAll("${obs-join-from}", obsFrom);
            if (!firstFilter) {
                sqlRequest.append(" AND ");
            }
            sqlRequest.append(" (CASE WHEN c.\"component\" IS NULL THEN o.\"observed_property\" ELSE \"component\" END) = pd.\"field_name\" ");
            sqlRequest.append(" AND pd.\"procedure\" = o.\"procedure\" ");
        } else {
            sqlRequest.replaceAll("${obs-join-from}", "");
            // we must add a field filter to remove the "time" field of the timeseries
            if (!firstFilter) {
                sqlRequest.append(" AND ");
            }
            // we must add a field filter to remove the "time" field of the timeseries
            sqlRequest.append(" NOT ( pd.\"field_type\" = 'Time' AND pd.\"order\" = 1 ) ");
            // we must remove the quality fields
            sqlRequest.append(" AND (pd.\"parent\" IS NULL) ");
        }
        firstFilter = false;
        if (phenPropJoin) {
            sqlRequest.replaceAll("${phen-prop-join}", "pd.\"field_name\"");
        }
        if (procPropJoin) {
            sqlRequest.replaceAll("${proc-prop-join}", "pd.\"procedure\"");
        }
        if (foiPropJoin) {
            sqlRequest.replaceAll("${foi-prop-join}", "o.\"foi\"");
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest.append(" ORDER BY pd.\"procedure\", pd.\"order\" ");

        if (!hasMeasureFilter) {
            sqlRequest = appendPaginationToRequest(sqlRequest);
        }

        final List<org.opengis.observation.Observation> observations = new ArrayList<>();

        // various cache map to avoid reading multiple time the same data
        final Map<String, Procedure> processMap = new HashMap<>();
        final Map<String, Phenomenon> phenMap = new HashMap<>();
        final Map<String, Map<String, Object>> obsPropMap  = new HashMap<>();

        LOGGER.fine(sqlRequest.toString());
        try (final Connection c = source.getConnection();
             final SQLResult rs = sqlRequest.execute(c)) {

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
                final int fieldIndex  = rs.getInt("order");
                final DbField field   = getFieldByIndex(procedure, fieldIndex, true, c);

                if (hasMeasureFilter) {
                    ProcedureInfo pti = getPIDFromProcedure(procedure, c).orElseThrow(IllegalStateException::new); // we know that the procedure exist
                    final FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(0, List.of(field), pti);
                    MultiFilterSQLRequest measureRequests = buildMesureRequests(pti, List.of(field), measureFilter, null, false, false, true, true);
                    try (final SQLResult rs2 = measureRequests.execute(c)) {
                        if (rs2.next()) {
                            int count = rs2.getInt(1, field.tableNumber);
                            // TODO pagination broken
                            if (count == 0) continue;
                        }
                    }
                }

                Map<String, Object> properties = obsPropMap.computeIfAbsent(procedure, pr -> {
                    try {
                        Map<String, Object> res = new HashMap<>();
                        res.put("type", getProcedureOMType(procedure, c));
                        return res;
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                

                final Phenomenon phen = phenMap.computeIfAbsent(observedProperty,op -> {return getPhenomenonSafe(op, c);});
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
                final List<Element> resultQuality = buildResultQuality(field, null);
                Observation observation = new Observation(obsID + '-' + fieldIndex,
                                                          name + '-' + fieldIndex,
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
        if (hasMeasureFilter) {
            return applyPostPagination(observations);
        } else {
            return observations;
        }
    }

    private List<org.opengis.observation.Observation> getComplexObservations() throws DataStoreException {
        final Map<String, Observation> observations = new LinkedHashMap<>();
        final Map<String, Procedure> processMap     = new LinkedHashMap<>();
        final Map<String, List<Field>> fieldMap     = new LinkedHashMap<>();
        if (resultMode == null) {
            resultMode = ResultMode.CSV;
        }
        final ResultBuilder values = new ResultBuilder(resultMode, DEFAULT_ENCODING, false);
        sqlRequest.append(" ORDER BY o.\"time_begin\"");
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {

            while (rs.next()) {
                int nbValue = 0;
                final String procedure   = rs.getString("procedure");
                final String featureID   = rs.getString("foi");
                final int oid            = rs.getInt("id");
                Observation observation  = observations.get(procedure + '-' + featureID);
                ProcedureInfo pti        = getPIDFromProcedure(procedure, c).orElseThrow(); // we know that the procedure exist
                boolean profile          = "profile".equals(pti.type);
                final boolean profileWithTime = profile && includeTimeForProfile;

                Map<String, Object> properties = new HashMap<>();
                properties.put("type", pti.type);
                
               /*
                * Compute procedure fields
                */
                List<Field> fields = fieldMap.get(procedure);
                if (fields == null) {
                    if (!fieldIdFilters.isEmpty()) {
                        fields = new ArrayList<>();
                        fields.add(pti.mainField);

                        List<Field> phenFields = new ArrayList<>();
                        for (String f : fieldIdFilters) {
                            final Field field = getProcedureField(procedure, f, c);
                            if (field != null && !fields.contains(field)) {
                                phenFields.add(field);
                            }
                        }

                        // add proper order to fields
                        Collections.sort(phenFields, Comparator.comparing(field -> field.index));
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
                        fields.add(0, new DbField(0, FieldType.TIME, "time_begin", "time", "time", null, -1));
                    }
                    // add the result id in the dataBlock if requested
                    if (includeIDInDataBlock) {
                        fields.add(0, new DbField(0, FieldType.TEXT, "id", "measure identifier", "measure identifier", null, -1));
                    }
                    fieldMap.put(procedure, fields);
                }

               /*
                * Compute procedure measure request
                */
                int fieldOffset = getFieldsOffset(profile, profileWithTime, includeIDInDataBlock);
                final FilterSQLRequest measureFilter        = applyFilterOnMeasureRequest(fieldOffset, fields, pti);
                final MultiFilterSQLRequest measureRequests = buildMesureRequests(pti, fields, measureFilter, oid, false, true, false, false);
                LOGGER.fine(measureRequests.toString());
                
                final String name = rs.getString("identifier");
                final FieldParser parser = new FieldParser(fields, values, profileWithTime, includeIDInDataBlock, includeQualityFields, name);

                // profile oservation are instant
                if (profile) {
                    parser.firstTime = dateFromTS(rs.getTimestamp("time_begin"));
                }

                if (observation == null) {
                    final String obsID            = "obs-" + oid;
                    final SamplingFeature feature = getFeatureOfInterest(featureID,  c);
                    final Phenomenon phen         = getGlobalCompositePhenomenon(c, procedure);

                    if (profile) {
                        // profile observation are instant
                        parser.firstTime = dateFromTS(rs.getTimestamp("time_begin"));
                    }

                    /*
                     *  BUILD RESULT
                     */
                    try (final SQLResult rs2 = measureRequests.execute(c)) {
                        while (rs2.nextOnField(pti.mainField.name)) {
                            parser.parseLine(rs2, fieldOffset);
                            nbValue = nbValue + parser.nbValue;

                            /**
                             * In "separated observation" mode we create an observation for each measure and don't merge it into a single obervation by procedure/foi.
                             */
                            if (separatedMeasure) {
                                final TemporalGeometricPrimitive time = buildTime(obsID, parser.lastTime != null ? parser.lastTime : parser.firstTime, null);
                                final ComplexResult result = buildComplexResult(fields, nbValue, values);
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
                        LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", measureRequests.toString());
                        throw new DataStoreException("the service has throw a SQL Exception.", ex);
                    }

                    /**
                    * we create an observation with all the measures and keep it so we can extend it if another observation for the same procedure/foi appears.
                    */
                    if (!separatedMeasure) {
                        final TemporalGeometricPrimitive time = buildTime(obsID, parser.firstTime, parser.lastTime);
                        final ComplexResult result = buildComplexResult(fields, nbValue, values);
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
                        if (separatedProfileObs && profile) {
                            synchronized (format2) {
                                observations.put(procedure + '-' + featureID + '-' + format2.format(parser.firstTime), observation);
                            }
                        } else {
                            observations.put(procedure + '-' + featureID, observation);
                        }
                    }
                } else {
                   /**
                    * complete the previous observation with new measures.
                    */
                    try (final SQLResult rs2 = measureRequests.execute(c)) {
                        while (rs2.next()) {
                            parser.parseLine(rs2, fieldOffset);
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
                    // observation can be instant
                    observation.extendSamplingTime(parser.firstTime);
                    observation.extendSamplingTime(parser.lastTime);
                }
                values.clear();
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
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
        final Map<String, Map<Field, Phenomenon>> phenMap = new HashMap<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final Date startTime = dateFromTS(rs.getTimestamp("time_begin"));
                final Date endTime = dateFromTS(rs.getTimestamp("time_end"));
                final int oid = rs.getInt("id");
                final String name = rs.getString("identifier");
                final String obsID = "obs-" + oid;
                final String featureID = rs.getString("foi");
                final SamplingFeature feature = getFeatureOfInterest(featureID, c);
                final ProcedureInfo pti = getPIDFromProcedure(procedure, c).orElseThrow();// we know that the procedure exist
                final Map<Field, Phenomenon> fieldPhen = phenMap.computeIfAbsent(procedure,  p -> getPhenomenonFields(p, c));
                final Procedure proc = processMap.computeIfAbsent(procedure, p -> getProcessSafe(p, c));
                final TemporalGeometricPrimitive time = buildTime(obsID, startTime, endTime);

                /*
                 *  BUILD RESULT
                 */
                boolean profile       = "profile".equals(pti.type);

                Map<String, Object> properties = new HashMap<>();
                properties.put("type", pti.type);
                List<Field> fields = new ArrayList<>(fieldPhen.keySet());
                final FilterSQLRequest measureFilter       = applyFilterOnMeasureRequest(0, fields, pti);
                final MultiFilterSQLRequest measureRequest =  buildMesureRequests(pti, fields, measureFilter, oid, false, true, false, false);

                /**
                 * coherence verification
                 */
                LOGGER.fine(measureRequest.toString());
                try (final SQLResult rs2 = measureRequest.execute(c)) {
                    // get the first for now
                    int tableNum = rs2.getFirstTableNumber();
            
                    while (rs2.nextOnField(pti.mainField.name)) {
                        final Integer rid = rs2.getInt("id", tableNum);
                        if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                            TemporalGeometricPrimitive measureTime;
                            if (profile) {
                                measureTime = time;
                            } else {
                                final Date mt = dateFromTS(rs2.getTimestamp(pti.mainField.name, tableNum));
                                measureTime = buildTime(oid + "-" + rid, mt, null);
                            }

                            for (Entry<Field, Phenomenon> entry : fieldPhen.entrySet()) {
                                Phenomenon fphen = entry.getValue();
                                DbField field    = (DbField) entry.getKey();
                                FieldType fType  = field.type;
                                String fName     = field.name;
                                int rsIndex      = field.tableNumber;
                                final String observationType = getOmTypeFromFieldType(fType);
                                final String value = rs2.getString(fName, rsIndex);
                                if (value != null) {
                                    Object resultValue;
                                    switch (fType) {
                                        case QUANTITY: resultValue = rs2.getDouble(fName, rsIndex); break;
                                        case BOOLEAN:  resultValue = rs2.getBoolean(fName, rsIndex); break;
                                        case TIME:     resultValue = new Date(rs2.getTimestamp(fName, rsIndex).getTime()); break;
                                        case TEXT:
                                        default: resultValue = value; break;
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
        int fieldOffset = profile ? 0 : 1; // for profile, the first phenomenon field is the main field
        if (profileWithTime) {
            fieldOffset++;
        }
        if (includeIDInDataBlock) {
            fieldOffset++;
        }
        return fieldOffset;
    }

    @Override
    public Result getResults() throws DataStoreException {
        if (ResponseMode.OUT_OF_BAND.equals(responseMode)) {
            throw new ObservationStoreException("Out of band response mode has not been implemented yet", NO_APPLICABLE_CODE, RESPONSE_MODE);
        }
        final boolean countRequest          = "count".equals(responseFormat);
        final boolean includeTimeForProfile = !countRequest && this.includeTimeForProfile;
        final boolean decimate              = !countRequest && decimationSize != null;
        final boolean profile               = "profile".equals(currentProcedure.type);
        final boolean profileWithTime       = profile && includeTimeForProfile;
       
        FilterSQLRequest measureRequest    = null;
        try (final Connection c = source.getConnection()) {
            /**
             *  1) build field list.
             */
            final List<Field> fields;
            if (!fieldIdFilters.isEmpty()) {
                fields = new ArrayList<>();
                fields.add(currentProcedure.mainField);
                
                List<Field> phenFields = new ArrayList<>();
                for (String f : fieldIdFilters) {
                    final Field field = getProcedureField(currentProcedure.procedureId, f, c);
                    if (field != null && !fields.contains(field)) {
                        phenFields.add(field);
                    }
                }
                // add proper order to fields
                Collections.sort(phenFields, Comparator.comparing(field -> field.index));
                fields.addAll(phenFields);

            } else {
                fields = readFields(currentProcedure.procedureId, false, c, fieldIndexFilters, fieldIdFilters);
            }
            // in a measurement context, the last field is the one we look want to use for identifier construction.
            String idSuffix = "";
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                idSuffix = "-" + fields.get(fields.size() - 1).index;
            }

            // add the time for profile in the dataBlock if requested
            if (profileWithTime) {
                fields.add(0, new DbField(0, FieldType.TIME, "time_begin", "time", "time", null, -1));
            }
            // add the result id in the dataBlock if requested
            if (includeIDInDataBlock) {
                fields.add(0, new DbField(0, FieldType.TEXT, "id", "measure identifier", "measure identifier", null, -1));
            }

            /**
             *  2) complete SQL request.
             */
            int fieldOffset = getFieldsOffset(profile, profileWithTime, includeIDInDataBlock);
            FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(fieldOffset, fields, currentProcedure);

            measureRequest = buildMesureRequests(currentProcedure, fields, measureFilter, null, obsJoin, false, false, false);
            measureRequest.append(sqlRequest);
            
            ResultProcessor processor = chooseResultProcessor(decimate, fields, fieldOffset, idSuffix);
            processor.computeRequest(measureRequest, fieldOffset, firstFilter, c);
            LOGGER.fine(measureRequest.toString());

            /**
             * 3) Extract results.
             */
            ResultBuilder values = processor.initResultBuilder(responseFormat, countRequest);
            try (final SQLResult rs = measureRequest.execute(c)) {
                processor.processResults(rs, fieldOffset);
            }
            switch (values.getMode()) {
                case DATA_ARRAY:  return new ComplexResult(fields, values.getDataArray(), null);
                case CSV:         return new ComplexResult(fields, values.getEncoding(), values.getStringValues(), null);
                case COUNT:       return new ComplexResult(values.getCount());
                default: throw new IllegalArgumentException("Unexpected result mode");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", measureRequest != null ? measureRequest.toString() : "NO SQL QUERY");
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
    }
    
    protected ResultProcessor chooseResultProcessor(boolean decimate, final List<Field> fields, int fieldOffset, String idSuffix) {
        ResultProcessor processor;
        if (decimate) {
            if (timescaleDB) {
                boolean singleField = (fields.size() - fieldOffset) == 1;
                boolean smoothAvailable = timescaleDBVersion.compareTo(MIN_TIMESCALE_VERSION_SMOOTH) >= 0;
                
                /**
                * for a single field we use specific timescale function
                * asap_smooth (lttb todo)
                */
               if (singleField && smoothAvailable && !decimationAlgorithm.equals("time_bucket")) {
                   processor = new ASMTimeScaleResultDecimator(fields, includeIDInDataBlock, decimationSize, fieldIndexFilters, includeTimeForProfile, currentProcedure);

               /**
                * otherwise we use time bucket method.
                * This methods seems not to be so fast with very large group of data.
                */
               } else {
                   processor = new BucketTimeScaleResultDecimator(fields, includeIDInDataBlock, decimationSize, fieldIndexFilters, includeTimeForProfile, currentProcedure);
               }
            } else {
                /**
                 * default java bucket decimation.
                 * no algorithm change possible yet
                 */
                processor = new DefaultResultDecimator(fields, includeIDInDataBlock, decimationSize, fieldIndexFilters, includeTimeForProfile, currentProcedure);
            }
        } else {
            processor = new ResultProcessor(fields, includeIDInDataBlock, includeQualityFields, includeTimeForProfile, currentProcedure, idSuffix);
        }
        return processor;
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
            sqlRequest.replaceAll("${foi-prop-join}", "sf.\"id\"");
        }
        if (procPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offerings\" offe", "off.\"id_offering\" = offe.\"identifier\""));
            sqlRequest.replaceAll("${proc-prop-join}", "offe.\"procedure\"");
        }
        if (phenPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" offop", "offop.\"id_offering\" = off.\"id_offering\""));
            //joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = offop.\"phenomenon\""));
            sqlRequest.replaceAll("${phen-prop-join}",  "offop.\"phenomenon\"");
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        final List<org.opengis.observation.sampling.SamplingFeature> features = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                final String id = rs.getString("id");
                final String name = rs.getString("name");
                final String desc = rs.getString("description");
                final String sf = rs.getString("sampledfeature");
                final int srid = rs.getInt("crs");
                final org.locationtech.jts.geom.Geometry geom = readGeom(rs, "shape");
                if (geom != null) {
                    final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                    JTS.setCRS(geom, crs);
                }
                final Map<String, Object> properties = readProperties("sampling_features_properties", "id_sampling_feature", id, c);
                features.add(new SamplingFeature(id, name, desc, properties, sf, geom));
            }
        } catch (SQLException | FactoryException | ParseException ex) {
            LOGGER.log(Level.WARNING, "Exception while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("Error while reading feature of interests.", ex);
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
            // if there is only one procedure involved, we want to order them by "order", if there is more than one, the order is irrelevant.
            // that why we use the "max(order)" and "group by max(order)"
            noCompositePhenomenon = false;
            if (!obsJoin) {
                sqlRequest.replaceFirst("DISTINCT(op.\"id\") as \"id\"", "op.\"id\" as \"id\", MAX(\"order\") as \"mo\"");
            }
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedure_descriptions\" pd", "pd.\"field_name\" = op.\"id\""));
        }
        if (phenPropJoin) {
            sqlRequest.replaceAll("${phen-prop-join}",  "op.\"id\"");
        }
        if (procPropJoin) {
            sqlRequest.replaceAll("${proc-prop-join}", "o.\"procedure\"");
        }
        if (foiPropJoin) {
            sqlRequest.replaceAll("${foi-prop-join}", "o.\"foi\"");
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
            
            // -- TODO REALLY find a cleaner way to do this
            secondRequest.replaceAll("op.\"id\" =", "c.\"component\" =");
            secondRequest.replaceAll("op.\"id\" IN", "c.\"component\" IN");
            // -- end TODO
            
            secondRequest.replaceFirst("op.\"id\" NOT IN (SELECT DISTINCT(\"phenomenon\")", "op.\"id\" IN (SELECT DISTINCT(\"phenomenon\")");
            
            sqlRequest.join(joins, firstFilter);

            sqlRequest.append(" UNION ").append(secondRequest);
        } else {
            sqlRequest.join(joins, firstFilter);
        }

        if (procDescJoin && !obsJoin) {
            sqlRequest.append("group by \"id\"");
            sqlRequest.append(" ORDER BY \"mo\" ");
        } else {
            sqlRequest.append(" ORDER BY \"id\"");
        }
        sqlRequest = appendPaginationToRequest(sqlRequest);
        final List<org.opengis.observation.Phenomenon> phenomenons = new ArrayList<>();
        LOGGER.fine(sqlRequest.toString());
        try (final Connection c = source.getConnection();
            final SQLResult rs  = sqlRequest.execute(c)) {
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
            sqlRequest.replaceAll("${proc-prop-join}", "pr.\"id\"");
        }
        if (phenPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" offop", "offop.\"id_offering\" = off.\"identifier\""));
            sqlRequest.replaceAll("${phen-prop-join}",  "offop.\"phenomenon\"");
            
        }
        if (foiPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_foi\" offf", "offf.\"id_offering\" = off.\"identifier\""));
            sqlRequest.replaceAll("${foi-prop-join}", "offf.\"foi\"");
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        final List<Process> results = new ArrayList<>();
        try(final Connection c = source.getConnection();
            final SQLResult rs =  sqlRequest.execute(c)) {
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
        try (final Connection c = source.getConnection();
             final SQLResult rs = sqlRequest.execute(c)) {
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
        Geometry spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = GeometricUtilities.toJTSGeometry(envelopeFilter, WrapResolution.NONE);
        }
        sqlRequest.append(" ORDER BY pr.\"id\"");

        boolean applyPostPagination = true;
        if (spaFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest);
        }

        Map<String, Geometry> locations = new LinkedHashMap<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                try {
                    final String procedure = rs.getString("id");
                    final org.locationtech.jts.geom.Geometry geom = readGeom(rs, "location");
                    if (geom != null) {
                        final int srid = rs.getInt(3);
                        final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
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
        sqlRequest.append(" ORDER BY hl.\"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {

            SensorLocationProcessor processor = new SensorLocationProcessor(envelopeFilter, dialect);
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
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        FilterSQLRequest stepRequest = sqlRequest.clone();
        sqlRequest.append(" ORDER BY hl.\"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        int nbCell = decimationSize;

        try (final Connection c = source.getConnection()) {

            // calculate the first date and the time step for each procedure.
            final Map<Object, long[]> times = OM2Utils.getMainFieldStep(stepRequest, null, c, nbCell, OMEntity.HISTORICAL_LOCATION, null);

            LOGGER.fine(sqlRequest.toString());
            try(final SQLResult rs = sqlRequest.execute(c)) {
                SensorLocationProcessor processor = new SensorLocationDecimator(envelopeFilter, nbCell, times, dialect);
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
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        FilterSQLRequest stepRequest = sqlRequest.clone();
        sqlRequest.append(" ORDER BY hl.\"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);

        int nbCell = decimationSize;
        try (final Connection c = source.getConnection()) {

            // calculate the first date and the time step for each procedure.
            final Map<Object, long[]> times = OM2Utils.getMainFieldStep(stepRequest, null, c, nbCell, OMEntity.HISTORICAL_LOCATION, null);
            LOGGER.fine(sqlRequest.toString());
            try(final SQLResult rs = sqlRequest.execute(c)) {
                final SensorLocationProcessor processor = new SensorLocationDecimatorV2(envelopeFilter, nbCell, times, dialect);
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
        sqlRequest.append(" ORDER BY hl.\"procedure\", \"time\"");
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        Map<String, Set<Date>> times = new LinkedHashMap<>();
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
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
