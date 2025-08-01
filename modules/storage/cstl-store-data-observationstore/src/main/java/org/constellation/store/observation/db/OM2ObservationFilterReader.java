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
import org.constellation.api.CommonConstants;
import static org.constellation.api.CommonConstants.COMPLEX_OBSERVATION;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.result.ResultBuilder;
import org.constellation.util.FilterSQLRequest.TableJoin;
import org.constellation.util.MultiFilterSQLRequest;
import org.constellation.util.SQLResult;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.temp.ObservationType;
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
import org.opengis.temporal.TemporalPrimitive;
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
    
    public OM2ObservationFilterReader(final DataSource source, final Map<String, Object> properties, boolean includeTimeInprofileMeasureRequest) throws DataStoreException {
        super(source, properties, includeTimeInprofileMeasureRequest);
    }

    public OM2ObservationFilterReader(final DataSource source, final Map<String, Object> properties) throws DataStoreException {
        super(source, properties, false);
    }

    @Override
    public List<Observation> getObservations() throws DataStoreException {
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

    private List<Observation> getObservationTemplates() throws DataStoreException {
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

        final List<Observation> observations = new ArrayList<>();
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
                TemporalPrimitive tempTime = null;
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
                                                          COMPLEX_OBSERVATION,
                                                          proc,
                                                          tempTime,
                                                          feature,
                                                          phen,
                                                          null,
                                                          result,
                                                          properties,
                                                          null);
                observations.add(observation);
            }


        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw an Exception.", ex);
        }
        return observations;
    }

    private List<Observation> getMesurementTemplates() throws DataStoreException {
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

        final List<Observation> observations = new ArrayList<>();

        // various cache map to avoid reading multiple time the same data
        final Map<String, Procedure> processMap               = new HashMap<>();
        final Map<String, Phenomenon> phenMap                 = new HashMap<>();
        final Map<String, Map<String, Object>> obsPropMap     = new HashMap<>();
        final Map<String, ProcedureInfo> ptiMap               = new HashMap<>();
        final Map<String, TemporalPrimitive> timeMap          = new HashMap<>();

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
                    ProcedureInfo pti = ptiMap.computeIfAbsent(procedure, p -> getPIDFromProcedureSafe(procedure, c).orElseThrow()); // we know that the procedure exist
                    final MultiFilterSQLRequest measureFilter = applyFilterOnMeasureRequest(0, List.of(field), pti);
                    final FilterSQLRequest measureRequests    = buildMesureRequests(pti, List.of(field), measureFilter, null, false, false, true, true, false);
                    try (final SQLResult rs2 = measureRequests.execute(c)) {
                        if (rs2.next()) {
                            int count = rs2.getInt(1, field.tableNumber);
                            // TODO pagination broken
                            if (count == 0) continue;
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.SEVERE, "Exception while executing the query: {0}", measureRequests.toString());
                        throw new DataStoreException("the service has throw a Exception.", ex);
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


                final Phenomenon phen = phenMap.computeIfAbsent(observedProperty, op -> {return getPhenomenonSafe(op, c);});
                final String featureID;
                final SamplingFeature feature;
                if (includeFoiInTemplate) {
                    featureID = rs.getString("foi");
                    feature   = getFeatureOfInterest(featureID, c);
                } else {
                    featureID = null;
                    feature   = null;
                }

                /*
                 *  BUILD RESULT
                 */
                final Procedure proc = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});

                TemporalPrimitive tempTime = null;
                if (includeTimeInTemplate) {
                    String timeKey =  procedure + "-" + observedProperty + "-" + featureID;
                    tempTime = timeMap.computeIfAbsent(timeKey, k -> getTimeForTemplate(c, procedure, observedProperty, featureID));
                }
                final String observationType         = getOmTypeFromFieldType(field.dataType);
                MeasureResult result                 = new MeasureResult(field, null);
                final List<Element> resultQuality    = buildResultQuality(field, null);
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
                                                          properties,
                                                          new HashMap<>());
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

    private List<Observation> getComplexObservations() throws DataStoreException {
        final Map<String, Observation> observations = new LinkedHashMap<>();
        final Map<String, Procedure> processMap     = new LinkedHashMap<>();
        final Map<String, List<Field>> fieldMap     = new LinkedHashMap<>();
        final Map<String, ProcedureInfo> ptiMap     = new HashMap<>();
        if (resultMode == null) {
            resultMode = ResultMode.CSV;
        }
        sqlRequest.append(" ORDER BY o.\"time_begin\"");
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {

            while (rs.next()) {
                final String procedure   = rs.getString("procedure");
                final String featureID   = rs.getString("foi");
                final long oid           = rs.getLong("id");
                Observation observation  = observations.get(procedure + '-' + featureID);
                ProcedureInfo pti        = ptiMap.computeIfAbsent(procedure, p -> getPIDFromProcedureSafe(procedure, c).orElseThrow()); // we know that the procedure exist
                boolean timeseries       = pti.type == ObservationType.TIMESERIES;
                final boolean includeObservationTime = (!timeseries) && includeTimeForProfile;

               /*
                * Compute procedure fields
                */
                List<Field> fields = fieldMap.get(procedure);
                if (fields == null) {
                    
                    fields = readFields(procedure, false, c, fieldIndexFilters, fieldIdFilters);

                    // add the time for profile in the dataBlock if requested
                    if (includeObservationTime) {
                        fields.add(0, new DbField(0, FieldDataType.TIME, "time_begin", "time", "time", null, FieldType.METADATA, -1, List.of(), List.of()));
                    }
                    // add the result id in the dataBlock if requested
                    if (includeIDInDataBlock) {
                        fields.add(0, new DbField(0, FieldDataType.TEXT, "id", "measure identifier", "measure identifier", null, FieldType.METADATA, -1, List.of(), List.of()));
                    }
                    fieldMap.put(procedure, fields);
                }

               /*
                * Compute procedure measure request
                */
                final int fieldOffset = getFieldsOffset(!timeseries, includeObservationTime, includeIDInDataBlock);
                final MultiFilterSQLRequest measureFilter = applyFilterOnMeasureRequest(fieldOffset, fields, pti);
                final FilterSQLRequest measureRequests    = buildMesureRequests(pti, fields, measureFilter, oid, false, true, false, false, false);
                LOGGER.fine(measureRequests.toString());
                
                final String obsName = rs.getString("identifier");
                final FieldParser parser = buildFieldParser(pti.mainField.index, fields, includeObservationTime, obsName, fieldOffset);

                // profile oservation are instant
                if (!timeseries) {
                    parser.setFirstTime(dateFromTS(rs.getTimestamp("time_begin")));
                }

                if (observation == null) {
                    final SamplingFeature feature = getFeatureOfInterest(featureID,  c);
                    final Phenomenon phen         = getGlobalCompositePhenomenon(c, procedure);
                    final Procedure proc          = processMap.computeIfAbsent(procedure, f -> {return getProcessSafe(procedure, c);});
                    
                    try (final SQLResult rs2 = measureRequests.execute(c)) {
                       /**
                        * In "separated observation" mode we create an observation for each measure and don't merge it into a single obervation by procedure/foi.
                        */
                        if (separatedMeasure) {
                            observations.putAll(parser.parseSingleMeasureObservation(rs2, oid, pti, proc, feature, phen));
                        /**
                         * we create an observation with all the measures and keep it so we can extend it if another observation for the same procedure/foi appears.
                         */
                        } else {
                            observations.putAll(parser.parseComplexObservation(rs2, oid, pti, proc, feature, phen, separatedProfileObs));
                        }
                    } catch (SQLException ex) {
                        LOGGER.log(Level.SEVERE, "SQLException while executing the measure query: {0}", measureRequests.toString());
                        throw new DataStoreException("the service has throw a SQL Exception.", ex);
                    }
                } else {
                   /**
                    * complete the previous observation with new measures.
                    */
                    try (final SQLResult rs2 = measureRequests.execute(c)) {
                        parser.completeObservation(rs2, pti, observation);
                    }
                }
                parser.clear();
            }

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }

        // TODO make a real pagination
        return applyPostPagination(new ArrayList<>(observations.values()));
    }
    
    /**
     * Allow to override the field parser for different observations.
     * 
     * @param fields Procedure fields.
     * @param profileWithTime Flag indicating if the time must be included for profile observations.
     * @param obsName Observation base identifier.
     * @param fieldOffset index of the first measure field in the field list.
     * 
     * @return A field parser. 
     */
    protected FieldParser buildFieldParser(int mainFieldIndex, List<Field> fields, boolean profileWithTime, String obsName, int fieldOffset) {
        return new FieldParser(mainFieldIndex,fields, resultMode, profileWithTime, includeIDInDataBlock, includeQualityFields, includeParameterFields, obsName, fieldOffset);
    }

    protected List<Observation> getMesurements() throws DataStoreException {
        if (phenPropJoin) {
            sqlRequest.replaceAll("${phen-prop-join}", "o.\"observed_property\"");
        }
        if (procPropJoin) {
            sqlRequest.replaceAll("${proc-prop-join}", "o.\"procedure\"");
        }
        if (foiPropJoin) {
            sqlRequest.replaceAll("${foi-prop-join}", "o.\"foi\"");
        }
        // add orderby to the query
        sqlRequest.append(" ORDER BY o.\"time_begin\"");
        if (firstFilter) {
            sqlRequest.replaceFirst("WHERE", "");
        }

        final List<Observation> observations = new ArrayList<>();
        final Map<String, Procedure> processMap = new HashMap<>();
        final Map<String, Map<Field, Phenomenon>> phenMap = new HashMap<>();
        final Map<String, ProcedureInfo> ptiMap = new HashMap<>();
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final Date startTime   = dateFromTS(rs.getTimestamp("time_begin"));
                final Date endTime     = dateFromTS(rs.getTimestamp("time_end"));
                final long oid         = rs.getLong("id");
                final String name      = rs.getString("identifier");
                final String obsID     = "obs-" + oid;
                final String featureID = rs.getString("foi");
                final SamplingFeature feature = getFeatureOfInterest(featureID, c);
                final ProcedureInfo pti = ptiMap.computeIfAbsent(procedure, p -> getPIDFromProcedureSafe(procedure, c).orElseThrow());// we know that the procedure exist
                final Map<Field, Phenomenon> fieldPhen = phenMap.computeIfAbsent(procedure,  p -> getPhenomenonFields(pti, c));
                final Procedure proc = processMap.computeIfAbsent(procedure, p -> getProcessSafe(p, c));
                final TemporalPrimitive time = buildTime(obsID, startTime, endTime);

                /*
                 *  BUILD RESULT
                 */
                boolean timeseries = (pti.type == ObservationType.TIMESERIES);

                Map<String, Object> properties = new HashMap<>();
                properties.put("type", pti.type.name().toLowerCase());
                List<Field> fields = new ArrayList<>(fieldPhen.keySet());
                final MultiFilterSQLRequest measureFilter = applyFilterOnMeasureRequest(0, fields, pti);
                final FilterSQLRequest measureRequest     =  buildMesureRequests(pti, fields, measureFilter, oid, false, true, false, false, false);

                /**
                 * coherence verification
                 */
                LOGGER.fine(measureRequest.toString());
                try (final SQLResult rs2 = measureRequest.execute(c)) {
                    // get the first for now
                    int tableNum = rs2.getFirstTableNumber();
            
                    while (rs2.nextOnField(pti.mainField.name)) {
                        final Long rid = rs2.getLong("id", tableNum);
                        if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                            TemporalPrimitive measureTime;
                            Date mt = null;
                            if (timeseries) {
                                mt = dateFromTS(rs2.getTimestamp(pti.mainField.name, tableNum));
                            }

                            for (Entry<Field, Phenomenon> entry : fieldPhen.entrySet()) {
                                Phenomenon fphen    = entry.getValue();
                                DbField field       = (DbField) entry.getKey();
                                FieldDataType fType = field.dataType;
                                String fName        = field.name;
                                int rsIndex         = field.tableNumber;
                                final String observationType = getOmTypeFromFieldType(fType);
                                final String value = rs2.getString(fName, rsIndex);
                                if (value != null) {
                                    Object resultValue;
                                    switch (fType) {
                                        case QUANTITY: resultValue = rs2.getDouble(fName, rsIndex); break;
                                        case BOOLEAN:  resultValue = rs2.getBoolean(fName, rsIndex); break;
                                        case TIME:     resultValue = new Date(rs2.getTimestamp(fName, rsIndex).getTime()); break;
                                        case JSON:     resultValue = readJsonMap(value); break;
                                        case TEXT:
                                        default: resultValue = value; break;
                                    }
                                    MeasureResult result = new MeasureResult(field, resultValue);
                                    final String measId =  obsID + '-' + field.index + '-' + rid;
                                    final String measName = name + '-' + field.index + '-' + rid;
                                    if (!timeseries) {
                                        measureTime = time;
                                    } else {
                                        measureTime = buildTime(measId, mt, null);
                                    }
                                    List<Element> resultQuality = buildResultQuality(field, rs2);
                                    Map<String, Object> parameters = buildParameters(field, rs2);
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
                                                                              properties,
                                                                              parameters);
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
        } catch (DataStoreException | RuntimeException ex) {
            throw new DataStoreException("the service has throw an Exception:" + ex.getMessage(), ex);
        }
        // TODO make a real pagination
        return applyPostPagination(observations);
    }
    
    /**
     * Return the index of the first measure fields.
     *
     * @param nonTimeseries Set to {@code true} if the current observation is a Profile.
     * @param observationIncludedTime Set to {@code true} if the time has to be included in the result for a non timeseries.
     * @param includeIDInDataBlock Set to {@code true} if the measure identifier has to be included in the result.
     *
     * @return The index where starts the measure fields.
     */
    protected int getFieldsOffset(boolean nonTimeseries, boolean observationIncludedTime, boolean includeIDInDataBlock) {
        int fieldOffset = nonTimeseries ? 0 : 1; // for non Timeseries, the first phenomenon field is the main field
        if (observationIncludedTime) {
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
        final boolean includeTimeForNTS     = !countRequest && this.includeTimeForProfile;
        final boolean decimate              = !countRequest && decimationSize != null;
        final boolean nonTimeseries         = currentProcedure.type != ObservationType.TIMESERIES;
        final boolean ntsWithTime           = nonTimeseries && includeTimeForNTS;

        MultiFilterSQLRequest measureRequest = null;
        try (final Connection c = source.getConnection()) {
            /**
             *  1) build field list.
             */
            final List<Field> fields;
                
            boolean removeMainField  = false; // currentProcedure.mainField.dataType != FieldDataType.TIME;
            fields = readFields(currentProcedure.id, removeMainField, c, fieldIndexFilters, fieldIdFilters);
            //}
            if (fields.isEmpty()) {
                throw new DataStoreException("The sensor: " + currentProcedure + " has no fields.");
            }
            // in a measurement context, the last field is the one we look want to use for identifier construction.
            String idSuffix = "";
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                idSuffix = "-" + fields.get(fields.size() - 1).index;
            }

            // add the time for profile in the dataBlock if requested
            if (ntsWithTime) {
                fields.add(0, new DbField(0, FieldDataType.TIME, "time_begin", "time", "time", null, FieldType.METADATA, -1, List.of(), List.of()));
            }
            // add the result id in the dataBlock if requested
            if (includeIDInDataBlock) {
                fields.add(0, new DbField(0, FieldDataType.TEXT, "id", "measure identifier", "measure identifier", null, FieldType.METADATA, -1, List.of(), List.of()));
            }

            /**
             *  2) complete SQL request.
             */
            int fieldOffset = getFieldsOffset(nonTimeseries, ntsWithTime, includeIDInDataBlock);
            FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(fieldOffset, fields, currentProcedure);

            measureRequest = buildMesureRequests(currentProcedure, fields, measureFilter, null, obsJoin, false, false, false, decimate);
            // TODO not fan of this
            // this will append the time filter but ....
            if (!sqlRequest.isEmpty()) {
                // this one is probably useless since we already selected a procedure.
                // but i can't invalidate the result to return 0 measures.
                if (procPropJoin) {
                    sqlRequest.replaceAll("${proc-prop-join}", "o.\"procedure\"");
                }
                if (phenPropJoin) {
                    sqlRequest.replaceAll("${phen-prop-join}", "o.\"observed_property\"");
                }
                if (foiPropJoin) {
                    sqlRequest.replaceAll("${foi-prop-join}", "o.\"foi\"");
                }
                measureRequest.append(" AND ").append(sqlRequest);
            }
            
            ResultProcessor processor = chooseResultProcessor(decimate, fields, fieldOffset, idSuffix, c);
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
    
    protected ResultProcessor chooseResultProcessor(boolean decimate, final List<Field> fields, int fieldOffset, String idSuffix, Connection c) throws SQLException {
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
            processor = new ResultProcessor(fields, includeIDInDataBlock, includeQualityFields, includeParameterFields, includeTimeForProfile, currentProcedure, idSuffix);
        }
        if (CommonConstants.CSV_FLAT.equals(responseFormat)) {
            processor.setPhenomenons(getPhenomenonFields(fields, c));
            processor.setProcedureProperties(readProperties("procedures_properties", "id_procedure", currentProcedure.id, c));
        }
        return processor;
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests() throws DataStoreException {
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
        
        boolean applyPostPagination = true;
        if (envelopeFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest);
        }
        
        LOGGER.fine(sqlRequest.toString());
        List<SamplingFeature> results = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                final String id   = rs.getString("id");
                final String name = rs.getString("name");
                final String desc = rs.getString("description");
                final String sf   = rs.getString("sampledfeature");
                final int srid    = rs.getInt("crs");
                final org.locationtech.jts.geom.Geometry geom = readGeom(rs, "shape");
                if (geom != null) {
                    final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                    JTS.setCRS(geom, crs);
                    
                    // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (envelopeFilter != null && !geometryMatchEnvelope(geom, envelopeFilter)) {
                        continue;
                    }
                } else if (envelopeFilter != null) {
                    continue;
                }
                final Map<String, Object> properties = readProperties("sampling_features_properties", "id_sampling_feature", id, c);
                results.add(new SamplingFeature(id, name, desc, properties, sf, geom));
            }
        } catch (SQLException | FactoryException | ParseException ex) {
            LOGGER.log(Level.WARNING, "Exception while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("Error while reading feature of interests.", ex);
        }
        if (applyPostPagination) {
            results = applyPostPagination(results);
        }
        return results;
    }

    @Override
    public List<Phenomenon> getPhenomenons() throws DataStoreException {
        // TODO find better way
        if (sqlRequest.toString().endsWith(" AND ")){ 
            sqlRequest.replaceAll(" AND ", "");
        }
        
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
        final List<Phenomenon> phenomenons = new ArrayList<>();
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
    public List<Procedure> getProcesses() throws DataStoreException {
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
        final List<Procedure> results = new ArrayList<>();
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
        
        sqlRequest.append(" ORDER BY pr.\"id\"");

        boolean applyPostPagination = true;
        if (envelopeFilter == null) {
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

                    // this must not happen. it will break the pagination
                    if (geom == null) continue;
                    final int srid = rs.getInt(3);
                    final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                    JTS.setCRS(geom, crs);
                    
                    // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (envelopeFilter != null && !geometryMatchEnvelope(geom, envelopeFilter)) {
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