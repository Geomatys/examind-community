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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import org.constellation.store.observation.db.FieldParser;
import org.constellation.store.observation.db.OM2ObservationFilter;
import org.constellation.store.observation.db.OM2ObservationFilterReader;
import org.constellation.store.observation.db.ResultProcessor;
import org.constellation.store.observation.db.model.DbField;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.MultiFilterSQLRequest;
import org.constellation.util.SQLResult;
import org.constellation.util.SingleFilterSQLRequest;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.dateFromTS;
import static org.geotoolkit.observation.OMUtils.getOmTypeFromFieldType;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import static org.geotoolkit.observation.model.FieldType.BOOLEAN;
import static org.geotoolkit.observation.model.FieldType.QUANTITY;
import static org.geotoolkit.observation.model.FieldType.TEXT;
import static org.geotoolkit.observation.model.FieldType.TIME;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.ResultQuery;
import org.opengis.metadata.quality.Element;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 * Observation Filter reader specialization for database with mesure in a single flat table.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class MixedObservationFilterReader extends OM2ObservationFilterReader {
    
    public static String TABLE_NAME = "flat_csv_data";
    
    public MixedObservationFilterReader(OM2ObservationFilter omFilter) {
        super(omFilter);
    }

    public MixedObservationFilterReader(final DataSource source, final Map<String, Object> properties) throws DataStoreException {
        super(source, properties, true);
    }
    
    @Override
    protected void initFilterGetResult(ResultQuery query) throws DataStoreException {
        this.includeTimeForProfile = query.isIncludeTimeForProfile();
        this.responseMode          = query.getResponseMode();
        this.includeIDInDataBlock  = query.isIncludeIdInDataBlock();
        this.includeQualityFields  = query.isIncludeQualityFields();
        this.responseFormat        = query.getResponseFormat();
        this.decimationSize        = query.getDecimationSize();
        this.resultModel           = query.getResultModel();

        this.firstFilter = false;
        try (final Connection c = source.getConnection()) {
            currentProcedure = getPIDFromProcedure(query.getProcedure(), c).orElseThrow(() -> new DataStoreException("Unexisting procedure:" + query.getProcedure()));

            /*
             * in this implementation, we do not need to join observation for get result.
             * some filter may set on observation table (obsJoin flag will be set as true later in this case)
             */
            obsJoin = false;
            sqlRequest = new SingleFilterSQLRequest();
            
        } catch (SQLException ex) {
            throw new DataStoreException("Error while initailizing getResultFilter", ex);
        }
    }
    
    @Override
    protected ResultProcessor chooseResultProcessor(boolean decimate, final List<Field> fields, int fieldOffset, String idSuffix) {
        // for now we don't handle timescaledb case, has we assume we are in a duckdb context
        ResultProcessor processor;
        if (decimate) {
            /**
             * default java bucket decimation.
             * no algorithm change possible yet
             */
            processor = new MixedResultDecimator(fields, includeIDInDataBlock, decimationSize, fieldIndexFilters, includeTimeForProfile, currentProcedure);
            
        } else {
            processor = new MixedResultProcessor(fields, includeIDInDataBlock, includeQualityFields, includeTimeForProfile, currentProcedure, idSuffix);
        }
        return processor;
    }
    
    @Override
    protected void extractObservationIds(SQLResult rs2, List<Field> fields, final CountOrIdentifiers results, String name) throws SQLException {
        int tNum = rs2.getFirstTableNumber();
        if (MEASUREMENT_QNAME.equals(resultModel)) {
            Map<String, Integer> fieldIndex = new HashMap<>();
            fields.forEach(f -> fieldIndex.put(f.name, f.index));
            while (rs2.nextOnField("id")) {
                final Long rid = rs2.getLong("id", tNum);
                final Integer index = fieldIndex.get(rs2.getString("obsprop_id"));
                // in this implementation we don't have nan values
                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                    results.add(name + '-' + index + '-' + rid);
                }
            }
        } else {
            // in this implementation wehave multiple line for the same observation. so we trim the results using a set (TODO, change the query ?)
            Set<String> uniqueResults = new LinkedHashSet<>();
            while (rs2.nextOnField("id")) {
                final Long rid = rs2.getLong("id", tNum);
                
                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                    uniqueResults.add(name + '-' + rid);
                }
            }
            results.addAll(uniqueResults);
        }
    }
    
    /**
     * Build one or more SQL request on the measure(s) tables.
     * The point of this mecanism is to bypass the issue of selecting more than 1664 columns in a select.
     * The measure table columns are already multiple to avoid the probleme of limited columns in a table,
     * so we build a select request for eache measure table, join with the main field of the primary table.
     *
     * @param pti Informations abut the measure tables.
     * @param queryFields List of measure fields we want to read (id or main fields will be excluded).
     * @param measureFilter Piece of SQL to apply to all the measure query. (can be null)
     * @param oid An Observation id used to filter the measure. (can be null)
     * @param obsJoin If true, a join with the observation table will be applied.
     * @param addOrderBy If true, an order by main filed will be applied.
     * @param idOnly If true, only the measure identifier will be selected.
     * @param count
     * 
     * @return A Multi filter request on measure tables.
     */
    @Override
    protected FilterSQLRequest buildMesureRequests(ProcedureInfo pti, List<Field> queryFields, FilterSQLRequest measureFilter, Long oid, boolean obsJoin, boolean addOrderBy, boolean idOnly, boolean count, boolean decimate) {
        final boolean profile = "profile".equals(pti.type);
        final String mainFieldName = pti.mainField.name;
        final MultiFilterSQLRequest measureRequests = new MultiFilterSQLRequest();
        
        // OID is no longer relevant in this implementation as there is one observation by procedure
        boolean onlyMain = false;
        if (profile) {
            int mainFieldIndex = queryFields.indexOf(pti.mainField);
            onlyMain = true;
            for (Field field : queryFields) {
                if (field.index > mainFieldIndex + 1) {
                    onlyMain = false;
                    break;
                }
            }
        }
        
        String select;
        String where  = "WHERE \"thing_id\" = '" + pti.procedureId + "'";
        if (idOnly) {
            if (profile) {
                select = "getmesureidpr(\"z_value\", \"time\") as \"id\"";
            } else {
                select = "getmesureidts(\"time\") as \"id\"";
            }
        } else {
            if (profile) {
                select = "m.\"" + pti.mainField.name + "\", m.\"obsprop_id\", m.\"result\", m.\"time\" ";
            } else {
                select = "m.\"" + pti.mainField.name + "\", m.\"obsprop_id\", m.\"result\" ";
            }
            if (!decimate) {
                if (profile) {
                    select = select + ", getmesureidpr(m.\"z_value\", m.\"time\") as \"id\" ";
                } else {
                    select = select + ", getmesureidts(m.\"time\") as \"id\" ";
                }
            }
            // we skip special case for mainfield only request (profile case)
            if (!onlyMain) {
                where = where + " AND ( ";
                boolean first = true;
                for (Field df : queryFields) {
                    if (!df.name.equals(mainFieldName)) {
                        if (!first) where = where + " OR ";
                        where = where + "m.\"obsprop_id\" = '" + df.name + "' ";
                        first = false;
                    }
                }
                where = where + " ) ";
            }
        }
        final SingleFilterSQLRequest measureRequest = new SingleFilterSQLRequest("SELECT ");
        if (count) {
            // the "as id" throw an error when present
            select = select.replace("as \"id\"", "");
            measureRequest.append("COUNT(").append(select).append(")");
        } else {
            measureRequest.append(select);
        }
        measureRequest.append(" FROM \"" + schemaPrefix + "main\".\"" + TABLE_NAME + "\" m ");
        if (obsJoin) {
            measureRequest.append(",\"" + schemaPrefix + "om\".\"observations\" o ");
            where = where + " AND o.id = getpid(m.\"thing_id\") ";
        }
        measureRequest.append(where);
        
        measureRequests.addRequest(1, measureRequest);
        
        if (measureFilter != null && !measureFilter.isEmpty()) {
            FilterSQLRequest clone = measureFilter.clone();
            if (clone instanceof MultiFilterSQLRequest mf) {
                measureRequest.append(mf.getRequest(1), !profile);
            } else {
                measureRequest.append(clone, !profile);
            }
        }
        if (addOrderBy) {
            measureRequests.append(" ORDER BY ");
            if (profile) {
                measureRequests.append("m.\"time\",");
            }
            measureRequests.append("m.\"" + pti.mainField.name + "\"");
        }
        return measureRequests;
    }
    
    @Override
    protected List<org.opengis.observation.Observation> getMesurements() throws DataStoreException {
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
                final FilterSQLRequest measureFilter  = applyFilterOnMeasureRequest(0, fields, pti);
                final FilterSQLRequest measureRequest =  buildMesureRequests(pti, fields, measureFilter, oid, false, true, false, false, false);

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
                            TemporalGeometricPrimitive measureTime;
                            if (profile) {
                                measureTime = time;
                            } else {
                                final Date mt = dateFromTS(rs2.getTimestamp(pti.mainField.name, tableNum));
                                measureTime = buildTime(oid + "-" + rid, mt, null);
                            }
                            String currentFname = rs2.getString("obsprop_id");
                            
                            Entry<Field, Phenomenon> entry = getPhenomenonFromFieldName(currentFname, fieldPhen);
                            Phenomenon fphen = entry.getValue();
                            DbField field    = (DbField) entry.getKey();
                            FieldType fType  = field.type;
                            int rsIndex      = field.tableNumber;
                            final String observationType = getOmTypeFromFieldType(fType);
                            final String value = rs2.getString("result", rsIndex);
                            if (value != null) {
                                Object resultValue;
                                switch (fType) {
                                    case QUANTITY: resultValue = rs2.getDouble("result", rsIndex); break;
                                    case BOOLEAN:  resultValue = rs2.getBoolean("result", rsIndex); break;
                                    case TIME:     resultValue = new Date(rs2.getTimestamp("result", rsIndex).getTime()); break;
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
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "SQLException while executing the measure query: {0}", measureRequest.toString());
                    throw new DataStoreException("the service has throw a SQL Exception.", ex);
                }
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the observation query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw an Exception:" + ex.getMessage(), ex);
        }
        // TODO make a real pagination
        return applyPostPagination(observations);
    }
    
    private static Entry<Field, Phenomenon> getPhenomenonFromFieldName(String name, Map<Field, Phenomenon> fieldPhen) {
        for (Entry<Field, Phenomenon> entry : fieldPhen.entrySet()) {
            if (entry.getKey().name.equals(name)) {
                return entry;
            }
        }
        return null;
    }
    
    @Override
    protected void handleAllPhenParam(SingleFilterSQLRequest single, int tableNum, List<Field> fields, int offset, ProcedureInfo pti) {
        
        final String allPhenKeyword = "${allphen ";
        List<SingleFilterSQLRequest.Param> allPhenParams = single.getParamsByName("allphen");
        for (SingleFilterSQLRequest.Param param : allPhenParams) {
            // it must be one ${allphen ...} for each "allPhen" param
            if (!single.contains(allPhenKeyword)) throw new IllegalStateException("Result filter is malformed");
            String block = extractAllPhenBlock(single, allPhenKeyword);
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            int extraFilter = -1;
            for (int i = offset; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                if (matchType(param, field)) {
                    String mFilter = " m.\"obsprop_id\" = '" + field.name + "' AND m.\"result\" ";
                    if (i != offset) sb.append(" OR"); // not for the first
                    sb.append(" (").append(block.replace(allPhenKeyword, mFilter).replace('}', ' ')).append(") ");
                    extraFilter++;
                } else {
                    LOGGER.fine("Param type is not matching the field type: " + param.type.getName() + " => " + field.type);
                    if (i != offset) sb.append(" AND"); // not for the first
                    sb.append(" (FALSE) ");  // TODO is this invalidating anything?
                }
                
            }
            String filter = sb.toString();
            if (!filter.isEmpty()) {
                filter = " AND (" + filter + ") ";
            }
            single.replaceFirst(block, filter);
            if (extraFilter == -1) {
                // the filter has been removed, we need to remove the param
                single.removeNamedParam(param);
            } else {
                single.duplicateNamedParam(param, extraFilter);
            }
        }
    }
    
    /**
     * TODO NOT WORKING
     * 
     * @param field
     * @param pIndex
     * @param single
     * @param curTable
     * @param parent 
     */
    @Override
    protected void treatPhenFilterForField(DbField field, int pIndex, SingleFilterSQLRequest single, int curTable, Field parent) {
        String replacement = "obsprop_id\" = '" + field.name + "' AND m.\"result";
       
        final String phenKeyword = " AND (\"$phen" + pIndex;
        List<SingleFilterSQLRequest.Param> phenParams = single.getParamsByName("phen" + pIndex);
        for (SingleFilterSQLRequest.Param phenParam : phenParams) {
            boolean typeMatch = matchType(phenParam, field);
            if (field.tableNumber == curTable && typeMatch) {
                single.replaceFirst("$phen" + pIndex, replacement);
            } else {
                 // it must be one phenKeyword for each "phen$i" param
                if (!single.contains(phenKeyword)) throw new IllegalStateException("Result filter is malformed");
                String measureFilter = single.getRequest();
                int opos = measureFilter.indexOf(phenKeyword);
                int cpos = measureFilter.indexOf(")", opos + phenKeyword.length());
                String block = measureFilter.substring(opos, cpos + 1);

                // the parameter type does not match, we must invalidate the results
                if (!typeMatch) {
                    LOGGER.fine("Param type is not matching the field type: " + phenParam.type.getName() + " => " + field.type);
                    single.replaceFirst(block, " AND FALSE ");

                // we need to remove the filter fom the request, as it does not apply to this table
                } else {
                    single.replaceFirst(block, "");
                }
                single.removeNamedParam(phenParam);
            }
        }
        // no quality fields and this implementation
    }

    @Override
    protected FieldParser buildFieldParser(List<Field> fields, boolean profileWithTime, String obsName, int fieldOffset) {
        return new MixedFieldParser(fields, resultMode, profileWithTime, includeIDInDataBlock, includeQualityFields, obsName, fieldOffset);
    }
    
    @Override
    protected int getFieldsOffset(boolean profile, boolean profileWithTime, boolean includeIDInDataBlock) {
        int fieldOffset = 1; // int this implementation the first field is always a non-measure field
        if (profileWithTime) {
            fieldOffset++;
        }
        if (includeIDInDataBlock) {
            fieldOffset++;
        }
        return fieldOffset;
    }

    @Override
    protected void handleTimeMeasureFilterInRequest(SingleFilterSQLRequest single, ProcedureInfo pti) {
        single.replaceAll("$time", "time");
    }
    
    /**
     * the point of this method override is to avoid to join with observation in case of a result request.
     * 
     * @param phenomenon A list of phenomenon filter.
     */
    @Override
    public void setObservedProperties(final List<String> phenomenon) {
        if (objectType.equals(OMEntity.RESULT)) {
            if (phenomenon != null && !phenomenon.isEmpty() && !allPhenonenon(phenomenon)) {
                final FilterSQLRequest sb;
                final Set<String> fields    = new HashSet<>();
                final FilterSQLRequest sbPheno = new SingleFilterSQLRequest();
                final FilterSQLRequest sbCompo = new SingleFilterSQLRequest(" OR \"obsprop_id\" IN (SELECT DISTINCT(\"phenomenon\") FROM \"" + schemaPrefix + "om\".\"components\" WHERE ");
                for (String p : phenomenon) {
                    sbPheno.append(" \"obsprop_id\"=").appendValue(p).append(" OR ");
                    sbCompo.append(" \"component\"=").appendValue(p).append(" OR ");
                    fields.addAll(getFieldsForPhenomenon(p));
                }
                sbPheno.deleteLastChar(3);
                sbCompo.deleteLastChar(3);
                sbCompo.append(")");
                sb = sbPheno.append(sbCompo);
                obsJoin = false;
            
                for (String field : fields) {
                    fieldIdFilters.add(field);
                }
            
                if (!firstFilter) {
                    sqlRequest.append(" AND( ").append(sb).append(") ");
                } else {
                    sqlRequest.append(" ( ").append(sb).append(") ");
                    firstFilter = false;
                }
            }
        } else {
            super.setObservedProperties(phenomenon);
        }
    }
}
