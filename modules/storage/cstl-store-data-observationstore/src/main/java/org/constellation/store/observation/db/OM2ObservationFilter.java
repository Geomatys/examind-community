/*
 *    Examind - An open source and standard compliant SDI
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
import org.constellation.util.FilterSQLRequest;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStoreException;
import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import static org.constellation.api.CommonConstants.EVENT_TIME;
import org.geotoolkit.observation.model.Field;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import static org.constellation.store.observation.db.model.OMSQLDialect.*;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest.TableJoin;
import org.constellation.util.MultiFilterSQLRequest;
import org.constellation.util.SQLResult;
import org.constellation.util.SingleFilterSQLRequest;
import org.constellation.util.SingleFilterSQLRequest.Param;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.model.OMEntity.*;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.Phenomenon;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ResultQuery;
import org.locationtech.jts.io.ParseException;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.ComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Literal;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class OM2ObservationFilter extends OM2BaseReader implements ObservationFilterReader {

    protected FilterSQLRequest sqlRequest;
    protected SingleFilterSQLRequest sqlMeasureRequest = new SingleFilterSQLRequest();

    protected final DataSource source;

    protected boolean template = false;

    protected boolean firstFilter = true;

    // flag set to true if there is measure filter
    // (but time filter is not counted has one, because time filter can be used on observatio,n)
    protected boolean hasMeasureFilter = false;

    protected QName resultModel;

    protected boolean offJoin      = false;
    protected boolean obsJoin      = false;
    protected boolean procJoin     = false;
    protected boolean procDescJoin = false;
    protected boolean phenPropJoin = false;
    protected boolean procPropJoin = false;
    protected boolean foiPropJoin  = false;

    protected boolean separatedMeasure       = false;
    protected boolean separatedProfileObs   = true;
    protected boolean includeIDInDataBlock  = false;
    protected boolean includeTimeForProfile = false;
    protected boolean includeTimeInTemplate = false;
    protected boolean includeFoiInTemplate  = true;
    protected boolean includeQualityFields  = true;
    protected boolean singleObservedPropertyInTemplate = false;
    protected boolean noCompositePhenomenon = false;

    protected String responseFormat         = null;
    protected ResultMode resultMode         = null;
    protected Integer decimationSize        = null;
    protected String version;

    protected OMEntity objectType = null;

    protected ResponseMode responseMode;
    protected ProcedureInfo currentProcedure = null;

    protected List<String> fieldIdFilters = new ArrayList<>();

    protected List<Integer> fieldIndexFilters = new ArrayList<>();

    protected List<Integer> measureIdFilters = new ArrayList<>();

    protected Envelope envelopeFilter = null;

    protected Long limit     = null;
    protected Long offset    = null;

    /**
     * Clone a new Observation Filter.
     *
     * @param omFilter
     */
    public OM2ObservationFilter(final OM2ObservationFilter omFilter) {
        super(omFilter);
        this.source                    = omFilter.source;
        this.template                  = false;
        resultModel                    = null;

    }

    public OM2ObservationFilter(final DataSource source, final Map<String, Object> properties) throws DataStoreException {
        super(properties, true);
        this.source     = source;
        resultModel     = null;
        try {
            // try if the connection is valid
            try (final Connection c = this.source.getConnection()) {}
        } catch (SQLException ex) {
            throw new DataStoreException("SQLException while initializing the observation filter:" +'\n'+
                                           "cause:" + ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(AbstractObservationQuery query) throws DataStoreException {
        this.objectType = query.getEntityType();
        this.limit                 = query.getLimit().isPresent() ? query.getLimit().getAsLong() : null;
        this.offset                = query.getOffset();

        switch (objectType) {
            case FEATURE_OF_INTEREST -> initFilterGetFeatureOfInterest();
            case OBSERVED_PROPERTY   -> initFilterGetPhenomenon((ObservedPropertyQuery) query);
            case PROCEDURE           -> initFilterGetSensor();
            case OFFERING            -> initFilterOffering();
            case LOCATION            -> initFilterGetLocations();
            case HISTORICAL_LOCATION -> initFilterGetHistoricalLocations((HistoricalLocationQuery) query);
            case OBSERVATION         -> initFilterObservation((ObservationQuery) query);
            case RESULT              -> initFilterGetResult((ResultQuery) query);
            default -> throw new DataStoreException("unexpected object type:" + objectType);
        }
    }

    private void initFilterObservation(ObservationQuery query) {
        this.includeTimeInTemplate = query.isIncludeTimeInTemplate();
        this.responseMode          = query.getResponseMode();
        this.resultModel           = query.getResultModel();
        this.includeFoiInTemplate  = query.isIncludeFoiInTemplate();
        this.includeTimeForProfile = query.isIncludeTimeForProfile();
        this.includeIDInDataBlock  = query.isIncludeIdInDataBlock();
        this.includeQualityFields  = query.isIncludeQualityFields();
        this.separatedMeasure      = query.isSeparatedMeasure();
        this.resultMode            = query.getResultMode();
        this.responseFormat        = query.getResponseFormat();
        this.decimationSize        = query.getDecimationSize();
        this.separatedProfileObs   = query.isSeparatedProfileObservation();
        
        this.singleObservedPropertyInTemplate = MEASUREMENT_QNAME.equals(resultModel) && ResponseMode.RESULT_TEMPLATE.equals(responseMode);
        if (ResponseMode.RESULT_TEMPLATE.equals(responseMode)) {
            sqlRequest = new SingleFilterSQLRequest("SELECT distinct ");
            if (singleObservedPropertyInTemplate) {
                sqlRequest.append("pd.\"procedure\", pd.\"field_name\" AS \"obsprop\", pd.\"order\", pd.\"uom\"");
            } else {
                sqlRequest.append("o.\"procedure\"");
            }
            if (includeFoiInTemplate) {
                sqlRequest.append(", o.\"foi\"");
                obsJoin = true;
            }
            if (singleObservedPropertyInTemplate) {
                sqlRequest.append(" FROM \"").append(schemaPrefix).append("om\".\"procedure_descriptions\" pd ${obs-join-from} ");
                sqlRequest.append(" WHERE ");
                firstFilter = true;
            } else {
                sqlRequest.append(" FROM \"").append(schemaPrefix).append("om\".\"observations\" o");
                sqlRequest.append(" WHERE ");
                firstFilter = true;
            }

            template = true;
        } else {
            sqlRequest = new SingleFilterSQLRequest("SELECT o.\"id\", o.\"identifier\", \"observed_property\", o.\"procedure\", \"foi\", \"time_begin\", \"time_end\" FROM \"");
            sqlRequest.append(schemaPrefix).append("om\".\"observations\" o WHERE \"identifier\" NOT LIKE ").appendValue(observationTemplateIdBase + '%').append(" ");
            firstFilter = false;
        }
    }

    private void initFilterGetResult(ResultQuery query) throws DataStoreException {
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

            final boolean decimate = decimationSize != null && !"count".equals(responseFormat);
            /*
             * As an optimization, we try to avoid to join the observation table if possible.
             * mandatory case to join the table:
             *
             *  - profile procedure (because of time)
             *  - include id in un-decimated extraction (debatable, but like this for now)
             *  - some filter has been set on observation table (obsJoin flag will be set as true later in this case)
             */
            obsJoin = "profile".equals(currentProcedure.type) || !decimate && includeIDInDataBlock;

            // sqlRequest = buildMesureRequests(currentProcedure, null, null, true, false, false, false);
           // sqlRequest.append(" AND \"procedure\"=").appendValue(currentProcedure.procedureId).append(" ");
           sqlRequest = new SingleFilterSQLRequest();


        } catch (SQLException ex) {
            throw new DataStoreException("Error while initailizing getResultFilter", ex);
        }
    }

    private void initFilterGetFeatureOfInterest() {
        firstFilter = true;
        String geomColum = switch(dialect) {
            case POSTGRES     -> "st_asBinary(\"shape\") as \"shape\"";
            case DERBY,DUCKDB -> "\"shape\"";
        };
        sqlRequest = new SingleFilterSQLRequest("SELECT DISTINCT sf.\"id\", sf.\"name\", sf.\"description\", sf.\"sampledfeature\", sf.\"crs\", ").append(geomColum).append(" FROM \"")
                    .append(schemaPrefix).append("om\".\"sampling_features\" sf WHERE ");
        obsJoin = false;
    }

    private void initFilterGetPhenomenon(ObservedPropertyQuery query) {
        this.noCompositePhenomenon = query.isNoCompositePhenomenon();
        sqlRequest = new SingleFilterSQLRequest("SELECT DISTINCT(op.\"id\") as \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" op ");
        if (noCompositePhenomenon) {
            sqlRequest.append(" WHERE op.\"id\" NOT IN (SELECT DISTINCT(\"phenomenon\") FROM \"").append(schemaPrefix).append("om\".\"components\") ");
            firstFilter = false;
        } else {
            sqlRequest.append(" WHERE ");
            firstFilter = true;
        }
    }

    private void initFilterGetSensor() {
        sqlRequest = new SingleFilterSQLRequest("SELECT DISTINCT(pr.\"id\") FROM \"" + schemaPrefix + "om\".\"procedures\" pr WHERE ");
        firstFilter = true;
    }

    private void initFilterOffering() throws DataStoreException {
        sqlRequest = new SingleFilterSQLRequest("SELECT off.\"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" off WHERE ");
        firstFilter = true;
    }

    private void initFilterGetLocations() throws DataStoreException {
        String geomColum = switch(dialect) {
            case POSTGRES -> "st_asBinary(\"shape\") as \"location\"";
            case DUCKDB   -> "ST_AsText(\"shape\") as \"location\"";
            default       -> "\"shape\" as \"location\"";
        };
        sqlRequest = new SingleFilterSQLRequest("SELECT pr.\"id\", ")
                .append(geomColum).append(", pr.\"crs\" FROM \"")
                .append(schemaPrefix).append("om\".\"procedures\" pr WHERE ");
        firstFilter = true;
    }

    private void initFilterGetHistoricalLocations(HistoricalLocationQuery query) throws DataStoreException {
        this.decimationSize = query.getDecimationSize();
        String geomColum = switch(dialect) {
            case POSTGRES -> "st_asBinary(\"location\") as \"location\"";
            case DUCKDB   -> "ST_AsText(\"location\") as \"location\"";
            default       -> "\"location\"";
        };
        sqlRequest = new SingleFilterSQLRequest("SELECT hl.\"procedure\", hl.\"time\", ")
                .append(geomColum).append(", hl.\"crs\" FROM \"")
                .append(schemaPrefix).append("om\".\"historical_locations\" hl WHERE ");
        firstFilter = true;
    }

    private void initFilterGetProcedureTimes() throws DataStoreException {
        sqlRequest = new SingleFilterSQLRequest("SELECT hl.\"procedure\", hl.\"time\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" hl WHERE ");
        firstFilter = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcedure(final List<String> procedures) throws DataStoreException {
        if (procedures != null && !procedures.isEmpty()) {
            if (OMEntity.OBSERVED_PROPERTY.equals(objectType)) {
                final SingleFilterSQLRequest procSb  = new SingleFilterSQLRequest();
                final SingleFilterSQLRequest fieldSb = new SingleFilterSQLRequest();
                for (String procedureID : procedures) {
                    if (!noCompositePhenomenon) {
                        Phenomenon phen;
                        try {
                            phen = getGlobalCompositePhenomenon(procedureID);
                            if (phen == null) {
                                LOGGER.warning("Global phenomenon not found for procedure :" + procedureID);
                            } else {
                                fieldSb.append(" (op.\"id\" = '").append(phen.getId()).append("') OR");
                            }
                        } catch (DataStoreException ex) {
                            throw new DataStoreException("Error while getting global phenomenon for procedure:" + procedureID, ex);
                        }
                    } else {
                        procSb.append("(pd.\"procedure\"=").appendValue(procedureID).append(") OR");
                        procDescJoin = true;
                    }
                }
                if (!procSb.isEmpty()) {
                    procSb.deleteLastChar(3);
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(procSb).append(") ");
                    } else {
                        sqlRequest.append(procSb);
                        firstFilter = false;
                    }
                }
                if (!fieldSb.isEmpty()) {
                    fieldSb.deleteLastChar(3);
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(fieldSb).append(") ");
                    } else {
                        sqlRequest.append(fieldSb);
                        firstFilter = false;
                    }
                }
            } else if (RESULT.equals(objectType) ) {
                // procedure is already known
                // the filter will eventually be set at results extraction
            } else {
                final String columnName;
                if (HISTORICAL_LOCATION.equals(objectType)) {
                    columnName = "hl.\"procedure\"";
                   // obsJoin = true;  TODO verify
                } else if (OFFERING.equals(objectType)) {
                    columnName = "off.\"procedure\"";
                } else if (PROCEDURE.equals(objectType) || LOCATION.equals(objectType)) {
                    columnName = "pr.\"id\"";
                } else if (singleObservedPropertyInTemplate) {
                    columnName = "pd.\"procedure\"";
                } else {
                    columnName = "o.\"procedure\"";
                    obsJoin = true;
                }
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                for (String s : procedures) {
                    if (s != null) {
                        sqlRequest.append(" ").append(columnName).append("=").appendValue(s).append(" OR ");
                    }
                }
                sqlRequest.deleteLastChar(3);
                sqlRequest.append(") ");
                firstFilter = false;
            }
        }
    }

    @Override
    public void setProcedureType(String type) throws DataStoreException {
        if (type != null) {
            if (!firstFilter) {
                sqlRequest.append(" AND ");
            }
            sqlRequest.append(" pr.\"type\"=").appendValue(type).append(" ");
            firstFilter = false;
            procJoin = true;
        }
    }

    private boolean allPhenonenon(final List<String> phenomenons) {
        return phenomenons.size() == 1 && phenomenons.get(0).equals(phenomenonIdBase + "ALL");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setObservedProperties(final List<String> phenomenon) {
        if (phenomenon != null && !phenomenon.isEmpty() && !allPhenonenon(phenomenon)) {
            final FilterSQLRequest sb;
            final Set<String> fields    = new HashSet<>();
            boolean getPhen = OMEntity.OBSERVED_PROPERTY.equals(objectType);
            boolean getFOI  = OMEntity.FEATURE_OF_INTEREST.equals(objectType);
            if (getPhen) {
                final FilterSQLRequest sbPheno = new SingleFilterSQLRequest();
                for (String p : phenomenon) {
                    sbPheno.append(" op.\"id\" =").appendValue(p).append(" OR ");
                    // try to be flexible and allow to call this ommiting phenomenon id base
                    if (!p.startsWith(phenomenonIdBase)) {
                        sbPheno.append(" op.\"id\" =").appendValue(phenomenonIdBase + p).append(" OR ");
                    }
                    fields.addAll(getFieldsForPhenomenon(p));
                }
                sbPheno.deleteLastChar(3);
                sb = sbPheno;
            } else {
                if (singleObservedPropertyInTemplate) {
                    final FilterSQLRequest sbPheno = new SingleFilterSQLRequest();
                    for (String p : phenomenon) {
                        sbPheno.append(" pd.\"field_name\" = ").appendValue(p).append(" OR ");
                        fields.addAll(getFieldsForPhenomenon(p));
                    }
                    sbPheno.deleteLastChar(3);
                    sb = sbPheno;
                } else {
                    final FilterSQLRequest sbPheno = new SingleFilterSQLRequest();
                    final FilterSQLRequest sbCompo = new SingleFilterSQLRequest(" OR \"observed_property\" IN (SELECT DISTINCT(\"phenomenon\") FROM \"" + schemaPrefix + "om\".\"components\" WHERE ");
                    for (String p : phenomenon) {
                        sbPheno.append(" \"observed_property\"=").appendValue(p).append(" OR ");
                        sbCompo.append(" \"component\"=").appendValue(p).append(" OR ");
                        fields.addAll(getFieldsForPhenomenon(p));
                    }
                    sbPheno.deleteLastChar(3);
                    sbCompo.deleteLastChar(3);
                    sbCompo.append(")");
                    sb = sbPheno.append(sbCompo);
                    obsJoin = true;
                }
               
            }
            if (!getFOI) {
                for (String field : fields) {
                    fieldIdFilters.add(field);
                }
            }
            if (!firstFilter) {
                sqlRequest.append(" AND( ").append(sb).append(") ");
            } else {
                sqlRequest.append(" ( ").append(sb).append(") ");
                firstFilter = false;
            }
        }
    }

    private Set<String> getFieldsForPhenomenon(final String phenomenon) {
        final Set<String> results = new HashSet<>();
        try(final Connection c = source.getConnection()) {
            final Phenomenon phen = getPhenomenon(phenomenon, c);
            if (phen instanceof CompositePhenomenon compo) {
                results.addAll(compo.getComponent().stream().map(child -> ((Phenomenon)child).getId()).toList());
            } else {
                results.add(phenomenon);
            }
        } catch (SQLException | DataStoreException ex) {
            LOGGER.log(Level.WARNING, "Exception while reading phenomenon", ex);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFeatureOfInterest(final List<String> fois) {
        if (fois != null && !fois.isEmpty()) {
            String columnName;
            if (OMEntity.FEATURE_OF_INTEREST.equals(objectType)) {
                columnName = "sf.\"id\"";
            } else {
                columnName = "\"foi\"";
                obsJoin = true;
            }
            final FilterSQLRequest sb = new SingleFilterSQLRequest();
            for (String foi : fois) {
                sb.append("(").append(columnName).append("=").appendValue(foi).append(") OR");
            }
            sb.deleteLastChar(3);

            if (!firstFilter) {
                sqlRequest.append(" AND( ").append(sb).append(") ");
            } else {
                sqlRequest.append(" (").append(sb).append(") ");
                firstFilter = false;
            }
        }
    }

    @Override
    public void setObservationIds(List<String> ids) {
        if (!ids.isEmpty()) {
            if (OMEntity.OBSERVED_PROPERTY.equals(objectType)) {
                final SingleFilterSQLRequest procSb  = new SingleFilterSQLRequest();
                final SingleFilterSQLRequest fieldSb = new SingleFilterSQLRequest();
               /*
                * in phenomenon mode 2 possibility :
                *   1) look properties for observation for a template:
                *       - <template base> <proc id>
                *       - <template base> <proc id> - <field id>
                *
                *   2) look properties for observation by id:
                *       - <observation base> <observation id>
                *       - <observation base> <observation id> - <measure id>
                *       - <observation base> <observation id> - <field id> - <measure id>
                *
                *   3) look for a template for an observation id:
                *       - <observation id>
                */
                for (String oid : ids) {
                    if (oid.contains(observationTemplateIdBase)) {
                        String procedureID = oid.substring(observationTemplateIdBase.length());
                        // look for a field separator
                        boolean hasFieldId = false;
                        int pos = procedureID.lastIndexOf("-");
                        if (pos != -1) {
                            try {
                                int fieldIdentifier = Integer.parseInt(procedureID.substring(pos + 1));
                                String tmpProcedureID = procedureID.substring(0, pos);
                                if (existProcedure(sensorIdBase + tmpProcedureID) || existProcedure(tmpProcedureID)) {
                                    procedureID = tmpProcedureID;
                                    fieldSb.append("(pd.\"order\"=").appendValue(fieldIdentifier).append(") OR");
                                    if (existProcedure(sensorIdBase + procedureID)) {
                                        procedureID = sensorIdBase + procedureID;
                                    }
                                    procSb.append("(pd.\"procedure\"=").appendValue(procedureID).append(") OR");
                                    procDescJoin = true;
                                    hasFieldId = true;
                                }
                            } catch (NumberFormatException ex) {}
                        }
                        if (!hasFieldId) {
                             if (existProcedure(sensorIdBase + procedureID)) {
                                procedureID = sensorIdBase + procedureID;
                            }
                            if (!noCompositePhenomenon) {
                                Phenomenon phen;
                                try {
                                    phen = getGlobalCompositePhenomenon(procedureID);
                                    if (phen == null) {
                                        LOGGER.warning("Global phenomenon not found for procedure :" + procedureID);
                                    } else {
                                        fieldSb.append(" (op.\"id\" = '").append(phen.getId()).append("') OR");
                                    }
                                } catch (DataStoreException ex) {
                                    LOGGER.log(Level.SEVERE, "Error while getting global phenomenon for procedure:" + procedureID, ex);
                                }
                            } else {
                                procSb.append("(pd.\"procedure\"=").appendValue(procedureID).append(") OR");
                                procDescJoin = true;
                            }
                        }

                    } else if (oid.startsWith(observationIdBase)) {
                        String[] component = oid.split("-");
                        if (component.length == 3) {
                            oid = component[0];
                            int fieldId = Integer.parseInt(component[1]);
                            fieldIndexFilters.add(fieldId);
                            fieldSb.append("(pd.\"order\"=").appendValue(fieldId).append(") OR");
                            procDescJoin = true;

                            // mid is not really used in that case we should look for existence and none emptiness but it will be costly
                            int mid = Integer.parseInt(component[2]);
                            measureIdFilters.add(mid);
                            

                        } else if (component.length == 2) {
                            oid = component[0];
                           
                            // mid is not really used in that case we should look for existence and none emptiness but it will be costly
                            int mid = Integer.parseInt(component[1]);
                            measureIdFilters.add(mid);
                        }
                        procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        obsJoin = true;
                    } else {
                        procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        obsJoin = true;
                    }
                    
                }
                if (!procSb.isEmpty()) {
                    procSb.deleteLastChar(3);
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(procSb).append(") ");
                    } else {
                        sqlRequest.append(procSb);
                        firstFilter = false;
                    }
                }
                if (!fieldSb.isEmpty()) {
                    fieldSb.deleteLastChar(3);
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(fieldSb).append(") ");
                    } else {
                        sqlRequest.append(fieldSb);
                        firstFilter = false;
                    }
                }

            } else {
                final SingleFilterSQLRequest procSb  = new SingleFilterSQLRequest();
                final SingleFilterSQLRequest fieldSb = new SingleFilterSQLRequest();
               /*
                * in template mode 2 possibility :
                *   1) look for for a template by id:
                *       - <template base> <proc id>
                *       - <template base> <proc id> - <field id>
                *
                *   2) look for a template for an observation id:
                *       - <observation base> <observation id>
                *       - <observation base> <observation id> - <measure id>
                *       - <observation base> <observation id> - <field id> - <measure id>
                *
                *   3) look for a template for an observation id:
                *       - <observation id>
                */
                if (template) {
                    for (String oid : ids) {
                        if (oid.startsWith(observationTemplateIdBase)) {
                            String procedureID = oid.substring(observationTemplateIdBase.length());
                            // look for a field separator
                            int pos = procedureID.lastIndexOf("-");
                            if (pos != -1) {
                                try {
                                    int fieldIdentifier = Integer.parseInt(procedureID.substring(pos + 1));
                                    String tmpProcedureID = procedureID.substring(0, pos);
                                    if (existProcedure(sensorIdBase + tmpProcedureID) ||
                                        existProcedure(tmpProcedureID)) {
                                        procedureID = tmpProcedureID;
                                        fieldIndexFilters.add(fieldIdentifier);
                                        fieldSb.append("(pd.\"order\"=").appendValue(fieldIdentifier).append(") OR");
                                        procDescJoin = true;
                                    }
                                } catch (NumberFormatException ex) {}
                            }
                            String tablePrefix;
                            if (singleObservedPropertyInTemplate) {
                                tablePrefix = "pd";
                            } else {
                                tablePrefix = "o";
                            }
                            if (existProcedure(sensorIdBase + procedureID)) {
                                procSb.append("(").append(tablePrefix).append(".\"procedure\"=").appendValue(sensorIdBase + procedureID).append(") OR");
                            } else {
                                procSb.append("(").append(tablePrefix).append(".\"procedure\"=").appendValue(procedureID).append(") OR");
                            }
                        } else if (oid.startsWith(observationIdBase)) {
                            String[] component = oid.split("-");
                            if (component.length == 3) {
                                oid = component[0];
                                int fieldId = Integer.parseInt(component[1]);
                                fieldIndexFilters.add(fieldId);
                                fieldSb.append("(pd.\"order\"=").appendValue(fieldId).append(") OR");
                                procDescJoin = true;
                                measureIdFilters.add(Integer.valueOf(component[2]));
                            } else if (component.length == 2) {
                                oid = component[0];
                                measureIdFilters.add(Integer.valueOf(component[1]));
                            }
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        } else {
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        }
                    }
                    obsJoin = true;

               /*
                * in observations mode 2 possibility :
                *   1) look for for observation for a template:
                *       - <template base> - <proc id>
                *       - <template base> - <proc id> - <field id>
                *   2) look for observation by id:
                *       - <observation base> <observation id>
                *       - <observation base> <observation id> - <measure id>
                *       - <observation base> <observation id> - <field id> - <measure id>
                */
                } else {
                    for (String oid : ids) {
                         if (oid.contains(observationTemplateIdBase)) {
                            String procedureID = oid.substring(observationTemplateIdBase.length());
                            // look for a field separator
                            int pos = procedureID.lastIndexOf("-");
                            if (pos != -1) {
                                try {
                                    int fieldIdentifier = Integer.parseInt(procedureID.substring(pos + 1));
                                    String tmpProcedureID = procedureID.substring(0, pos);
                                    if (existProcedure(sensorIdBase + tmpProcedureID) ||
                                        existProcedure(tmpProcedureID)) {
                                        procedureID = tmpProcedureID;
                                        fieldIndexFilters.add(fieldIdentifier);
                                    }
                                } catch (NumberFormatException ex) {}
                            }
                            if (existProcedure(sensorIdBase + procedureID)) {
                                procedureID = sensorIdBase + procedureID;
                            }
                            
                            // avoid to join with observation in a getResult context
                            if (currentProcedure == null || !currentProcedure.procedureId.equals(procedureID)) {
                                procSb.append("(o.\"procedure\"=").appendValue(procedureID).append(") OR");
                                obsJoin = true;
                            }
                            
                        } else if (oid.startsWith(observationIdBase)) {
                            String[] component = oid.split("-");
                            if (component.length == 3) {
                                oid = component[0];
                                fieldIndexFilters.add(Integer.valueOf(component[1]));
                                measureIdFilters.add(Integer.valueOf(component[2]));
                            } else if (component.length == 2) {
                                oid = component[0];
                                measureIdFilters.add(Integer.valueOf(component[1]));
                            }
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                            obsJoin = true;
                        } else {
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                            obsJoin = true;
                        }
                    }
                }
                if (!procSb.isEmpty()) procSb.deleteLastChar(3);
                if (!fieldSb.isEmpty()) fieldSb.deleteLastChar(3);

                if (!procSb.isEmpty()) {
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(procSb).append(") ");
                    } else {
                        sqlRequest.append(procSb);
                        firstFilter = false;
                    }
                }
                if (!fieldSb.isEmpty()) {
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(fieldSb).append(") ");
                    } else {
                        sqlRequest.append(fieldSb);
                        firstFilter = false;
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeFilter(final TemporalOperator tFilter) throws DataStoreException {
        // we get the property name (not used for now)
        // String XPath = tFilter.getExpression1()
        Object time = tFilter.getExpressions().get(1);
        if (time instanceof Literal lit && !(time instanceof TemporalGeometricPrimitive)) {
            time = lit.getValue();
        }

        TemporalOperatorName type = tFilter.getOperatorType();
        String currentOMType = currentProcedure != null ? currentProcedure.type : null;
        boolean skipforResult = false;
        final String tableAlias;
        if (objectType == OMEntity.OFFERING) {
            tableAlias = "off";
        } else if (objectType == OMEntity.HISTORICAL_LOCATION) {
            tableAlias = "hl";
        } else if (objectType == OMEntity.RESULT) {
            tableAlias = "o";
            skipforResult = !obsJoin;
        } else {
            tableAlias = "o";
            obsJoin = true;
        }
        FilterSQLRequest timeRequest = new SingleFilterSQLRequest();

        if (type == TemporalOperatorName.EQUALS) {

            if (time instanceof Period tp) {
                final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
                final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());

                // historical can't have a period as time value
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                     throw new ObservationStoreException("TM_Equals operation require timeInstant for historical location!", INVALID_PARAMETER_VALUE, EVENT_TIME);
                } else {
                    // force the join with observation table.
                    if (objectType == OMEntity.RESULT) obsJoin = true;
                    
                    // we request directly a multiple observation or a period observation (one measure during a period)
                    timeRequest.append(" ").append(tableAlias).append(".\"time_begin\"=").appendValue(begin).append(" AND ");
                    timeRequest.append(" ").append(tableAlias).append(".\"time_end\"=").appendValue(end).append(" ");
                }
            // if the temporal object is a timeInstant
            } else if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    timeRequest.append(" ").append(tableAlias).append(".\"time\"=").appendValue(position);
                } else {
                    if (!skipforResult) {
                        OM2Utils.addtimeDuringSQLFilter(timeRequest, ti, tableAlias);
                    }
                    
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND ( \"$time\"=", conditional).appendValue(position, conditional).append(") ", conditional);
                    }
                }
                
            } else {
                throw new ObservationStoreException("TM_Equals operation require timeInstant or TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else if (type == TemporalOperatorName.BEFORE) {
            
            // for the operation before the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    timeRequest.append(" \"time\"<=").appendValue(position);
                } else {
                    if (!skipforResult) {
                        timeRequest.append(" ").append(tableAlias).append(".\"time_begin\"<=").appendValue(position);
                    }
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND ( \"$time\"<=", conditional).appendValue(position, conditional).append(")", conditional);
                    }
                }
            } else {
                throw new ObservationStoreException("TM_Before operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }

        } else if (type == TemporalOperatorName.AFTER) {
            
            // for the operation after the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    timeRequest.append(" \"time\">=").appendValue(position);
                } else {
                    if (!skipforResult) {
                        timeRequest.append("(").append(tableAlias).append(".\"time_end\">=").appendValue(position);
                        timeRequest.append(") OR (");
                        timeRequest.append(tableAlias).append(".\"time_end\" IS NULL AND ").append(tableAlias).append(".\"time_begin\" >=").appendValue(position).append(")");
                    }
                    
                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND (\"$time\">=", conditional).appendValue(position, conditional).append(")", conditional);
                    }
                }
            } else {
                throw new ObservationStoreException("TM_After operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }

        } else if (type == TemporalOperatorName.DURING) {
            
            if (time instanceof Period tp) {
                final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
                final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    timeRequest.append(" \"time\">=").appendValue(begin).append(" AND \"time\"<=").appendValue(end);
                } else {
                    if (!skipforResult) {
                        OM2Utils.addtimeDuringSQLFilter(timeRequest, tp, tableAlias);
                    }

                    if (!"profile".equals(currentOMType)) {
                        boolean conditional = (currentOMType == null);
                        sqlMeasureRequest.append(" AND ( \"$time\">=", conditional).appendValue(begin, conditional)
                                         .append(" AND \"$time\"<= ", conditional).appendValue(end, conditional).append(")", conditional);
                    }
                }
            } else {
                throw new ObservationStoreException("TM_During operation require TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else {
            throw new ObservationStoreException("This operation is not take in charge by the Web Service, supported one are: TM_Equals, TM_After, TM_Before, TM_During");
        }

        if (!timeRequest.isEmpty()) {
            if (firstFilter) {
                sqlRequest.append(" ( ").append(timeRequest).append(" ) ");
                firstFilter = false;
            } else {
                sqlRequest.append("AND ( ").append(timeRequest).append(" ) ");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResultFilter(final BinaryComparisonOperator filter) throws DataStoreException {
        if (!(filter.getOperand1() instanceof ValueReference)) {
            throw new ObservationStoreException("Expression is null or not a propertyName on result filter");
        }
        if (!(filter.getOperand2() instanceof Literal)) {
            throw new ObservationStoreException("Expression is null or not a Literal on result filter");
        }
        // we expectthe property name to be "result" or "result[fieldIndex]" eventually followed by .quality_field_name
        final String propertyName = ((ValueReference)filter.getOperand1()).getXPath();
        final Literal value = (Literal) filter.getOperand2();
        final String operator = getSQLOperator(filter);

        // apply only on one phenonemenon
        if (propertyName.contains("[")) {
            int opos = propertyName.indexOf('[');
            int cpos = propertyName.indexOf(']');
            if (cpos <= opos) {
                throw new ObservationStoreException("Malformed propertyName in result filter:" + propertyName);
            }

            // we look for a quality field on the form ".field_name"
            String index = propertyName.substring(opos + 1, cpos);
            String suffix = propertyName.substring(cpos + 1);
            if (!suffix.isEmpty()) {
                suffix = "_quality_" + suffix.substring(1); // remove the '.'
            }
            String paramName = "phen" + index + suffix;
            sqlMeasureRequest.append(" AND (\"$").append(paramName).append("\" ").append(operator).appendNamedObjectValue(paramName, value.getValue()).append(")");

        // apply only on all phenonemenon
        } else if (propertyName.startsWith("result")){
            // we look for a quality field on the form ".field_name"
            String suffix = propertyName.substring(6);
            if (!suffix.isEmpty()) {
                suffix = "_quality_" + suffix.substring(1); // remove the '.'
            }
            String paramName = "allphen" + suffix;
            sqlMeasureRequest.append(" ${").append(paramName).append(operator).appendNamedObjectValue(paramName, value.getValue()).append("} ");
        } else {
            throw new ObservationStoreException("Unpexpected propertyName in result filter:" + propertyName);
        }
        hasMeasureFilter = true;
    }

    @Override
    public void setPropertiesFilter(BinaryComparisonOperator filter) throws DataStoreException {
        if (!(filter.getOperand1() instanceof ValueReference)) {
            throw new ObservationStoreException("Expression is null or not a propertyName on property filter");
        }
        if (!(filter.getOperand2() instanceof Literal)) {
            throw new ObservationStoreException("Expression is null or not a Literal on property filter");
        }
        final Literal value = (Literal) filter.getOperand2();
        final String operator = getSQLOperator(filter);
        String XPath = ((ValueReference)filter.getOperand1()).getXPath();
        // always contains "properties/"
        int pos = XPath.indexOf("properties/");
        if (pos == -1) throw new IllegalArgumentException("malformed propertyName. must follow the pattern (*/)properties/* ");

        OMEntity targetEntity = objectType;

        // requesting a property filter on another entity that the current object Type
        if (pos != 0) {
            String trgEntiName = XPath.substring(0, pos -1);
            targetEntity = OMEntity.fromName(trgEntiName);

            // add joins
            if (objectType == PROCEDURE || objectType == FEATURE_OF_INTEREST) {
                // we joins with the offerings
                offJoin = true;
            } else if (objectType == OBSERVED_PROPERTY) {
                // we joins with the observations
                obsJoin = true;
                
            // TODO verify here if we must join without the target entity test
            // like this
            // } else if (objectType == OBSERVATION) {
            } else if (objectType == OBSERVATION && targetEntity == FEATURE_OF_INTEREST) {
                // we joins with the observations
                obsJoin = true;
            }
        }

        String propertyName = XPath.substring(pos + 11);
        if (firstFilter) {
            sqlRequest.append(" ( ");
        } else {
            sqlRequest.append("AND ( ");
        }

        String select;

        switch (targetEntity) {
            case OBSERVED_PROPERTY -> {
                select = " ${phen-prop-join} IN (select DISTINCT(\"id_phenomenon\") FROM \"" + schemaPrefix + "om\".\"observed_properties_properties\" WHERE ";
                phenPropJoin = true;
            }
            case PROCEDURE -> {
                select = " ${proc-prop-join} IN (select DISTINCT(\"id_procedure\") FROM \"" + schemaPrefix + "om\".\"procedures_properties\" WHERE ";
                procPropJoin = true;
            }
            case FEATURE_OF_INTEREST -> {
                select = " ${foi-prop-join} IN (select DISTINCT(\"id_sampling_feature\") FROM \"" + schemaPrefix + "om\".\"sampling_features_properties\" WHERE ";
                foiPropJoin = true;
            }
            default -> {
                throw new ObservationStoreException("Unsuported property filter on entity:" + objectType);
            }
        }
        sqlRequest.append(select).append("\"property_name\" = ").appendValue(propertyName).append(" AND ");
        sqlRequest.append("\"value\" ").append(operator).appendObjectValue(value.getValue());
        sqlRequest.append(" )) ");
        firstFilter = false;
    }

    @Override
    public void setOfferings(final List<String> offerings) throws DataStoreException {
        if (offerings != null && !offerings.isEmpty()) {
            if (firstFilter) {
                sqlRequest.append(" ( ");
            } else {
                sqlRequest.append("AND ( ");
            }
            String columnName;
            if (OMEntity.OBSERVED_PROPERTY.equals(objectType) || OMEntity.FEATURE_OF_INTEREST.equals(objectType)) {
                columnName = " off.\"id_offering\"=";
            } else {
                columnName = " off.\"identifier\"=";
            }
            for (String s : offerings) {
                if (s != null) {
                    sqlRequest.append(columnName).appendValue(s).append(" OR ");
                }
            }
            sqlRequest.deleteLastChar(3);
            sqlRequest.append(") ");
            firstFilter = false;
            offJoin = true;
        }
    }

    private String getSQLOperator(final ComparisonOperator filter) throws ObservationStoreException {
        ComparisonOperatorName type = filter.getOperatorType();
        if (type == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO) {
            return " = ";
        } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN) {
            return " > ";
        } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO) {
            return " >= ";
        } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN) {
            return " < ";
        } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO) {
            return " <= ";
        } else {
            throw new ObservationStoreException("Unsuported binary comparison filter");
        }
    }

    /**
     * Apply the filters on the measure tables to the SQL measure request and return a cloned version.
     * Those filters can be on one or all the phenomenon fields, or on the main time field in a Timeseries context.
     *
     * @param offset The fieldIndex where starts the measure fields.
     * @param pti Procedure informations.
     * @param fields fields list.
     *
     * @return a filtered measure request.
     */
    protected FilterSQLRequest applyFilterOnMeasureRequest(int offset, List<Field> fields, ProcedureInfo pti) {
        // some time filter may have already been set in the measure request
        MultiFilterSQLRequest result = new MultiFilterSQLRequest();
        for (int tableNum = 1; tableNum < pti.nbTable + 1; tableNum++) {
            SingleFilterSQLRequest single = sqlMeasureRequest.clone();

            // $time will be present only for timeseries
            if ("timeseries".equals(pti.type) || "timeserie".equals(pti.type)) {
                single.replaceAll("$time", pti.mainField.name);
            }

           /**
            * 1) Look for measure filter applying on all result fields.
            * For each filter we replace it by a filter on each field.
            *
            * NB: There is an issue here in a measurement observations context.
            * The filter should be applied on each field separately
            * Actually the filter is apply on each field with a "AND"
            */
            final String allPhenKeyword = "${allphen ";
            List<Param> allPhenParams = single.getParamsByName("allphen");
            for (Param param : allPhenParams) {
                // it must be one ${allphen ...} for each "allPhen" param
                if (!single.contains(allPhenKeyword)) throw new IllegalStateException("Result filter is malformed");
                String block = extractAllPhenBlock(single, allPhenKeyword);
                StringBuilder sb = new StringBuilder();
                int extraFilter = -1;
                for (int i = offset; i < fields.size(); i++) {
                    DbField field = (DbField) fields.get(i);
                    if (field.tableNumber == tableNum) {
                        if (matchType(param, field)) {
                            sb.append(" AND (").append(block.replace(allPhenKeyword, "\"" + field.name + "\" ").replace('}', ' ')).append(") ");
                            extraFilter++;
                        } else {
                            LOGGER.fine("Param type is not matching the field type: " + param.type.getName() + " => " + field.type);
                            sb.append(" AND FALSE ");
                        }
                    }
                }
                single.replaceFirst(block, sb.toString());
                if (extraFilter == -1) {
                    // the filter has been removed, we need to remove the param
                    single.removeNamedParam(param);
                } else {
                    single.duplicateNamedParam(param, extraFilter);
                }
            }

            /**
            * 2) Look for measure filter applying on all result quality  fields.
            * 
            */
            Map<Param, AtomicInteger> extraFilter = new LinkedHashMap<>();
            for (int i = offset; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                if (field.tableNumber == tableNum) {
                    for (Field qField : field.qualityFields) {
                        final String allQPhenKeyword = "${allphen_quality_" + qField.name;
                        List<Param> allQPhenParams = single.getParamsByName("allphen_quality_" + qField.name);
                        for (Param qparam : allQPhenParams) {
                            extraFilter.put(qparam, new AtomicInteger(-1));
                            // it must be one ${allphen _quality_ ...} for each "allPhen _quality_" param
                            if (!single.contains(allQPhenKeyword)) throw new IllegalStateException("Result filter is malformed");

                            String block = extractAllPhenBlock(single, allQPhenKeyword);
                            StringBuilder sb = new StringBuilder();
                            if (matchType(qparam, qField)) {
                                sb.append(" AND (").append(block.replace(allQPhenKeyword, "\"" + field.name + "_quality_" + qField.name + "\" ").replace('}', ' ')).append(") ");
                                extraFilter.get(qparam).incrementAndGet();
                            } else {
                                LOGGER.fine("Param type is not matching the field type: " + qparam.type.getName() + " => " + field.type);
                                sb.append(" AND FALSE ");
                            }
                            single.replaceFirst(block, sb.toString());
                        }
                    }
                }
            }
            for (Entry<Param, AtomicInteger> entry : extraFilter.entrySet()) {
                if (entry.getValue().intValue() == -1) {
                    // the filter has been removed, we need to remove the param
                    single.removeNamedParam(entry.getKey());
                } else {
                    single.duplicateNamedParam(entry.getKey(), entry.getValue().intValue());
                }
            }


            /**
             * 3) Look for left over unexisting quality field filter.
             *
             *  Invalidate query if there are filter on unexisting fields.
             *  Handle also filter on quality fields.
             */
            final String allQPhenKeyword = "${allphen_quality_";
            while (single.contains(allQPhenKeyword)) {
                String measureFilter = single.getRequest();
                int opos = measureFilter.indexOf(allQPhenKeyword);
                // look for the phen Index
                int spos = measureFilter.indexOf(" ", opos + allQPhenKeyword.length());
                String suffix = measureFilter.substring(opos + allQPhenKeyword.length(), spos);

                // will remove eventually multiple params, but its not bad as we want to remove them all
                single.removeNamedParams("allphen_quality_" + suffix);
                int cpos = measureFilter.indexOf("}", opos + allQPhenKeyword.length());
                String block = measureFilter.substring(opos, cpos + 1);
                single.replaceFirst(block, " AND FALSE  ");

            }

            /**
             * 2) Look for measure filter applying on specific fields.
             *
             *  Replace phenomenon index filter by the real field name.
             *  Handle also filter on quality fields.
             */
            for (int i = offset; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                int pIndex = (i - offset);
                treatPhenFilterForField(field, pIndex, single, tableNum, null);
            }

            /**
             * 3) Look for left over out of index result field filter.
             *
             *  Invalidate query if there are filter on unexisting fields.
             *  Handle also filter on quality fields.
             */
            final String phenKeyword = " AND (\"$phen";
            while (single.contains(phenKeyword)) {
                String measureFilter = single.getRequest();
                int opos = measureFilter.indexOf(phenKeyword);
                // look for the phen Index
                int spos = measureFilter.indexOf("\"", opos + phenKeyword.length());
                String pSuffix = measureFilter.substring(opos + phenKeyword.length(), spos);

                // will remove eventually multiple params, but its not bad as we want to remove them all
                single.removeNamedParams("phen" + pSuffix);
                int cpos = measureFilter.indexOf(")", opos + phenKeyword.length());
                String block = measureFilter.substring(opos, cpos + 1);
                single.replaceFirst(block, " AND FALSE  ");

            }
            result.addRequest(tableNum, single);
        }
        
        return result;
    }

    private String extractAllPhenBlock(SingleFilterSQLRequest request, String keyword) {
        String measureFilter = request.getRequest();
        int opos = measureFilter.indexOf(keyword);
        int cpos = measureFilter.indexOf("}", opos + keyword.length());
        return measureFilter.substring(opos, cpos + 1);
    }
    
    private static boolean matchType(Param param, Field field) {
        //we want to let pass different numbers type
        if (Number.class.isAssignableFrom(param.type) &&
            Number.class.isAssignableFrom(field.type.getJavaType()))  {
            return true;
        }
        return param.type.isAssignableFrom(field.type.getJavaType());
    }

    private void treatPhenFilterForField(DbField field, int pIndex, SingleFilterSQLRequest single, int curTable, Field parent) {
        String columnName;
        String qualitySuffix;
        if (parent != null) {
            qualitySuffix = "_quality_" + field.name;
            columnName = parent.name + qualitySuffix;
        } else {
            qualitySuffix = "";
            columnName = field.name;
        }
        final String phenKeyword = " AND (\"$phen" + pIndex + qualitySuffix;
        List<Param> phenParams = single.getParamsByName("phen" + pIndex + qualitySuffix);
        for (Param phenParam : phenParams) {
            boolean typeMatch = matchType(phenParam, field);
            if (field.tableNumber == curTable && typeMatch) {
                single.replaceFirst("$phen" + pIndex + qualitySuffix, columnName);
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
        // treat the quality fields
        for (Field qField : field.qualityFields) {
            treatPhenFilterForField((DbField)qField, pIndex, single, curTable, field);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationResult> filterResult() throws DataStoreException {
        LOGGER.log(Level.FINER, "request:{0}", sqlRequest.toString());
        sqlRequest = appendPaginationToRequest(sqlRequest);
        LOGGER.fine(sqlRequest.toString());
        try (final Connection c    = source.getConnection();
            final SQLResult result = sqlRequest.execute(c)) {
            final List<ObservationResult> results = new ArrayList<>();
            while (result.next()) {
                results.add(new ObservationResult(result.getString(1),
                                                  result.getTimestamp(2),
                                                  result.getTimestamp(3)));
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }

    }

    /**
     * Used for either entity identifier listing or enytity count.
     * Just reduce the memory usage by not storing a list of identifiers.
     */
    private class CountOrIdentifiers {
        private List<String> identifiers = new ArrayList<>();
        private long count = 0;

        private final boolean forCount;

        public CountOrIdentifiers(boolean forCount) {
            this.forCount = forCount;
        }
        
        public void add(String identifier) {
            if (forCount) {
                count++;
            } else {
                identifiers.add(identifier);
            }
        }

        private void applyPostPagination(Function<List, List> postPaginationFct) {
            // post pagination make no sense for a count request
            if (!forCount) {
                identifiers = postPaginationFct.apply(identifiers);
            }
        }
    }

    private CountOrIdentifiers filterObservation(boolean forCount) throws DataStoreException {
        final CountOrIdentifiers results  = new CountOrIdentifiers(forCount);
        List<TableJoin> joins = new ArrayList<>();
        if (singleObservedPropertyInTemplate) {
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
                if (!firstFilter) {
                    sqlRequest.append(" AND ");
                }
                // we must add a field filter to remove the "time" field of the timeseries
                sqlRequest.append(" NOT ( pd.\"field_type\" = 'Time' AND pd.\"order\" = 1 ) ");
                // we must remove the quality fields
                sqlRequest.append(" AND (pd.\"parent\" IS NULL) ");
            }
            firstFilter = false;
        }
        if (phenPropJoin) {
            String joinColumn;
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                joinColumn = "pd.\"field_name\"";
            } else {
                // there is a problem here. see comment in OM2ObservationFilterReader#getObservationTemplates()
                // is the probleme still here ? since we don't join anymore
                joinColumn = "o.\"observed_property\"";
            }
            sqlRequest.replaceAll("${phen-prop-join}", joinColumn);
        }
        if (procPropJoin) {
            String tablePrefix;
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                tablePrefix = "pd";
            } else {
                tablePrefix = "o";
            }
            sqlRequest.replaceAll("${proc-prop-join}", tablePrefix + ".\"procedure\"");
        }
        if (foiPropJoin) {
            sqlRequest.replaceAll("${foi-prop-join}", "o.\"foi\"");
        }
        sqlRequest.join(joins, firstFilter);
        if (!forCount) {
            if (singleObservedPropertyInTemplate) {
                sqlRequest.append(" ORDER BY pd.\"procedure\", pd.\"order\" "); 
            } else {
                sqlRequest.append(" ORDER BY o.\"procedure\"");
            }
        }
        if (firstFilter) {
            sqlRequest = sqlRequest.replaceFirst("WHERE", "");
        }
        LOGGER.fine(sqlRequest.toString());
        try (final Connection c = source.getConnection();
             final SQLResult rs = sqlRequest.execute(c)) {
            final Map<String, ProcedureInfo> ptiMap = new HashMap<>();
            final Map<String, List<Field>> fieldMap = new HashMap<>();
            while (rs.next()) {
                final String procedure = rs.getString("procedure");
                final String procedureID;
                if (procedure.startsWith(sensorIdBase)) {
                    procedureID = procedure.substring(sensorIdBase.length());
                } else {
                    procedureID = procedure;
                }
                if (template) {
                    if (MEASUREMENT_QNAME.equals(resultModel)) {
                        final int fieldIndex = rs.getInt("order");
                        if (hasMeasureFilter) {
                            final DbField field = getFieldByIndex(procedure, fieldIndex, true, c);
                            final ProcedureInfo pti = ptiMap.computeIfAbsent(procedure, p -> getPIDFromProcedureSafe(procedure, c).orElseThrow()); // we know that the procedure exist
                            final FilterSQLRequest measureFilter   = applyFilterOnMeasureRequest(0, List.of(field), pti);
                            final FilterSQLRequest measureRequests = buildMesureRequests(pti, List.of(field), measureFilter, null, false, false, true, true);
                            try (final SQLResult rs2 = measureRequests.execute(c)) {
                                if (rs2.next()) {
                                    int count = rs2.getInt(1, field.tableNumber);
                                    // WARNING if we set a proper SQL pagination, this will broke it;
                                    if (count == 0) continue;
                                }
                            }
                        }
                        results.add(observationTemplateIdBase + procedureID + '-' + fieldIndex);
                    } else {
                        final String name = observationTemplateIdBase + procedureID;
                        results.add(name);
                    }
                } else {
                    final int oid            = rs.getInt("id");
                    final String name        = rs.getString("identifier");
                    final List<Field> fields = fieldMap.computeIfAbsent(procedure,  p -> readFields(procedure, true, c, fieldIndexFilters, fieldIdFilters));
                    final ProcedureInfo pti  = ptiMap.computeIfAbsent(procedure, p -> getPIDFromProcedureSafe(procedure, c).orElseThrow()); // we know that the procedure exist

                    final boolean idOnly = !MEASUREMENT_QNAME.equals(resultModel);
                    final FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(0, fields, pti);
                    final FilterSQLRequest mesureRequest = buildMesureRequests(pti, fields, measureFilter, oid, false, true, idOnly, false);
                    LOGGER.fine(mesureRequest.toString());

                    if (MEASUREMENT_QNAME.equals(resultModel)) {
                        
                        try (final SQLResult rs2 = mesureRequest.execute(c)) {
                            int tNum = rs2.getFirstTableNumber();
                            while (rs2.nextOnField("id")) {
                                final Integer rid = rs2.getInt("id", tNum);
                                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                    for (int i = 0; i < fields.size(); i++) {
                                        DbField field = (DbField) fields.get(i);
                                        // in measurement mode we only want the non empty measure
                                        final String value = rs2.getString(field.name, field.tableNumber);
                                        if (value != null) {
                                            results.add(name + '-' + field.index + '-' + rid);
                                        }
                                    }
                                }
                            }
                        } catch (SQLException ex) {
                            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", mesureRequest.toString() + '\n' + ex.getMessage());
                            throw new DataStoreException("the service has throw a SQL Exception.");
                        }
                        

                    } else {
                        try (final SQLResult rs2 = mesureRequest.execute(c)) {
                            while (rs2.nextOnField("id")) {
                                int tNum = rs2.getFirstTableNumber();
                                final Integer rid = rs2.getInt("id", tNum);
                                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                    results.add(name + '-' + rid);
                                }
                            }
                        } catch (SQLException ex) {
                            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", mesureRequest.toString() + '\n' + ex.getMessage());
                            throw new DataStoreException("the service has throw a SQL Exception.");
                        }
                    }
                }
            }
            // TODO make a real pagination
            results.applyPostPagination(this::applyPostPagination);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString() + '\n' + ex.getMessage());
            throw new DataStoreException("the service has throw a SQL Exception.");
        } catch (RuntimeException ex) {
            throw new DataStoreException("the service has throw a Runtime Exception:" + ex.getMessage(), ex);
        }
        return results;
    }
    
    private long getSensorLocationCount(boolean forCount) throws DataStoreException {
        // optimization with a count request if there is no bbox filter.
        if (forCount && envelopeFilter == null) {
            List<TableJoin> joins = new ArrayList<>();
            if (obsJoin) {
                joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = pr.\"id\""));
            }
            sqlRequest.join(joins, firstFilter);
        
            String request = "SELECT COUNT(*) FROM (" + sqlRequest.toString() + ") AS sub";

            LOGGER.fine(request);
            try(final Connection c               = source.getConnection();
                final Statement currentStatement = c.createStatement();
                final ResultSet result           = currentStatement.executeQuery(request)) {
                if (result.next()) {
                    return result.getLong(1);
                }
                throw new DataStoreException("the count request does not return anything!");
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
                throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
            }
        } else {
            return filterSensorLocations(forCount).count;
        }
    }
    
    protected boolean entityMatchEnvelope(final SQLResult rs, String geomColumn, String crsColumn) throws SQLException, DataStoreException, ParseException, FactoryException {
        if (envelopeFilter != null) {
            final org.locationtech.jts.geom.Geometry geom = readGeom(rs, geomColumn);
            if (geom == null) return false;
            final int srid = rs.getInt(crsColumn);
            final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
            JTS.setCRS(geom, crs);

            return geometryMatchEnvelope(geom, envelopeFilter);
        }
        return true;
    }

    private CountOrIdentifiers filterSensorLocations(boolean forCount) throws DataStoreException {
        CountOrIdentifiers results = new CountOrIdentifiers(forCount);
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = pr.\"id\""));
        }
        sqlRequest.join(joins, firstFilter);
        
        if (!forCount) {
            sqlRequest.append(" ORDER BY \"id\"");
        }
        boolean applyPostPagination = true;
        if (envelopeFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest);
        }

        LOGGER.fine(sqlRequest.toString());
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                try {
                    final String procedure = rs.getString("id");
                    
                     // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (!entityMatchEnvelope(rs, "location", "crs")) {
                        continue;
                    }
                    results.add(procedure);
                } catch (FactoryException | ParseException ex) {
                    throw new DataStoreException(ex);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        if (applyPostPagination) {
            results.applyPostPagination(this::applyPostPagination);
        }
        return results;
    }

    private List<String> filterHistoricalSensorLocations() throws DataStoreException {
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

        boolean applyPostPagination = true;
        if (envelopeFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest);
        }

        LOGGER.fine(sqlRequest.toString());
        List<String> results            = new ArrayList<>();
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                try {
                    final String procedure = rs.getString("procedure");
                    final long time = rs.getTimestamp("time").getTime();

                    // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (!entityMatchEnvelope(rs, "location", "crs")) {
                        continue;
                    }
                    final String hlid = procedure + "-" + time;

                    // can it happen? if so the pagination will be broken
                    // we temporarly throw an exception to see if this happen. if not remove this "IF"
                    if (results.contains(hlid)) {
                        throw new IllegalArgumentException("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
                    }
                    results.add(hlid);

                } catch (FactoryException | ParseException ex) {
                    throw new DataStoreException(ex);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        if (applyPostPagination) {
            results = applyPostPagination(results);
        }
        return results;
    }

    private List<String> filterFeatureOfInterest() throws DataStoreException {
        sqlRequest = appendPaginationToRequest(sqlRequest);
        List<TableJoin> joins = new ArrayList<>();
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
            
            // joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = offop.\"phenomenon\""));
            sqlRequest.replaceAll("${phen-prop-join}", "offop.\"phenomenon\"");
        }
        sqlRequest.join(joins, firstFilter);
        
        boolean applyPostPagination = true;
        if (envelopeFilter == null) {
            applyPostPagination = false;
            sqlRequest = appendPaginationToRequest(sqlRequest);
        }

        LOGGER.fine(sqlRequest.toString());
        List<String> results            = new ArrayList<>();
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                try {
                    final String id = rs.getString("id");
                    
                    // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (!entityMatchEnvelope(rs, "shape", "crs")) {
                        continue;
                    }
                    results.add(id);

                } catch (FactoryException | ParseException ex) {
                    throw new DataStoreException(ex);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception.", ex);
        }
        if (applyPostPagination) {
            results = applyPostPagination(results);
        }
        return results;
    }

    private String getPhenomenonRequest() {
        sqlRequest = appendPaginationToRequest(sqlRequest);
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"observed_property\" = op.\"id\""));
        }
        if (offJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" off", "off.\"phenomenon\" = op.\"id\""));
        }
        if (procDescJoin) {
            // in this case no composite will appears in the results. so no need for an SQL union later
            noCompositePhenomenon = false;
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedure_descriptions\" pd", "pd.\"field_name\" = op.\"id\""));
        }
        if (phenPropJoin) {
            sqlRequest.replaceAll("${phen-prop-join}", "op.\"id\"");
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
        return sqlRequest.toString();
    }

    private String getProcedureRequest() {
        sqlRequest = appendPaginationToRequest(sqlRequest);
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = pr.\"id\""));
        }
        if (offJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offerings\" off", "off.\"procedure\" = pr.\"id\""));
        }
        if (procPropJoin) {
            //joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedures_properties\" prp", "pr.\"id\" = prp.\"id_procedure\""));
            sqlRequest.replaceAll("${proc-prop-join}", "pr.\"id\"");
        }
        if (phenPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_observed_properties\" offop", "offop.\"id_offering\" = off.\"identifier\""));
            //joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = offop.\"phenomenon\""));
            sqlRequest.replaceAll("${phen-prop-join}", "offop.\"phenomenon\"");
        }
        if (foiPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"offering_foi\" offf", "offf.\"id_offering\" = off.\"identifier\""));
            sqlRequest.replaceAll("${foi-prop-join}", "offf.\"foi\"");
        }
        sqlRequest.join(joins, firstFilter);
        return sqlRequest.toString();
    }

    private String getHistoricalLocationRequest() {
        sqlRequest = appendPaginationToRequest(sqlRequest);
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
        return sqlRequest.toString();
    }

    private String getOfferingRequest() {
        sqlRequest = appendPaginationToRequest(sqlRequest);
        List<TableJoin> joins = new ArrayList<>();
        if (obsJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"observations\" o", "o.\"procedure\" = off.\"procedure\""));
        }
        if (procJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix + "om\".\"procedures\" pr", "off.\"procedure\" = pr.\"id\""));
        }
        sqlRequest.join(joins, firstFilter);
        return sqlRequest.toString();
    }

    @Override
    public Set<String> getIdentifiers() throws DataStoreException {
        if (objectType == null) {
            throw new DataStoreException("initialisation of the filter missing.");
        }
        String request;
        switch (objectType) {
            case OBSERVED_PROPERTY:   request = getPhenomenonRequest(); break;
            case PROCEDURE:           request = getProcedureRequest(); break;
            case OFFERING:            request = getOfferingRequest(); break;
            case FEATURE_OF_INTEREST: return new LinkedHashSet<>(filterFeatureOfInterest());
            case OBSERVATION:         return new LinkedHashSet(filterObservation(false).identifiers);
            case LOCATION:            return new LinkedHashSet(filterSensorLocations(false).identifiers);
            case HISTORICAL_LOCATION: return new LinkedHashSet(filterHistoricalSensorLocations());
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected object type:" + objectType);
        }

        final Set<String> results = new LinkedHashSet<>();

        LOGGER.fine(request);
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery(request)) {
            while (result.next()) {
                results.add(result.getString(1));
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }


    @Override
    public long getCount() throws DataStoreException {
        if (objectType == null) {
            throw new DataStoreException("initialisation of the filter missing.");
        }
        String request;
        switch (objectType) {
            case OBSERVED_PROPERTY:   request = getPhenomenonRequest(); break;
            case PROCEDURE:           request = getProcedureRequest(); break;
            case OFFERING:            request = getOfferingRequest(); break;
            case HISTORICAL_LOCATION: request = getHistoricalLocationRequest();break;
            case FEATURE_OF_INTEREST: return filterFeatureOfInterest().size();
            case OBSERVATION:         return filterObservation(true).count;
            case RESULT:              return filterResult().size();
            case LOCATION:            return getSensorLocationCount(true);
            default: throw new DataStoreException("unexpected object type:" + objectType);
        }

        request = "SELECT COUNT(*) FROM (" + request + ") AS sub";

        LOGGER.fine(request);
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery(request)) {
            if (result.next()) {
                return result.getLong(1);
            }
            throw new DataStoreException("the count request does not return anything!");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBoundingBox(final BinarySpatialOperator box) throws DataStoreException {
        if (LOCATION.equals(objectType) || HISTORICAL_LOCATION.equals(objectType) || FEATURE_OF_INTEREST.equals(objectType)) {
            Envelope e = OMUtils.getEnvelopeFromBBOXFilter(box);
            envelopeFilter = e;
        } else {
            throw new DataStoreException("SetBoundingBox is not supported by this ObservationFilter implementation.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        //do nothing
    }

    @Override
    public void destroy() {
        //do nothing
    }

    protected FilterSQLRequest appendPaginationToRequest(FilterSQLRequest request) {
        if (dialect.equals(POSTGRES)) {
            if (limit != null && limit > 0) {
                request.append(" LIMIT ").append(Long.toString(limit));
            }
            if (offset != null && offset > 0) {
                request.append(" OFFSET ").append(Long.toString(offset));
            }
        // TODO verify if this is suported by duckdb
        } else {
            if (offset != null && offset > 0) {
                request.append(" OFFSET ").append(Long.toString(offset)).append(" ROWS ");
            }
            if (limit != null && limit > 0) {
                request.append(" fetch next ").append(Long.toString(limit)).append(" rows only");
            }
        }
        return request;
    }

    public boolean existProcedure(String procedure) {
        try(final Connection c   = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, procedure);

            try (final ResultSet rs   = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while looking for procedure existance.", ex);
        }
        return false;
    }

    protected Map<Field, Phenomenon> getPhenomenonFields(String procedure, Connection c) {
        try {
            List<Field> fields = readFields(procedure, true, c, fieldIndexFilters, fieldIdFilters);
            final Map<Field, Phenomenon> results = new LinkedHashMap<>();
            for (Field f : fields) {
                results.put(f, getSinglePhenomenon(f.name, c));
            }
            return results;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected List<Field> getFieldsForPhenomenon(final Phenomenon fullPhen, List<Field> fields, Connection c) throws DataStoreException {
        final List<Field> results = new ArrayList<>();
        for (Field f : fields) {
            if (isIncludedField(f.name, f.description, f.index) && isFieldInPhenomenon(f.name, fullPhen)) {
                results.add(f);
            }
        }
        return results;
    }

    private boolean isFieldInPhenomenon(String phenId, Phenomenon phen) {
        if (phen instanceof CompositePhenomenon composite) {
            return hasComponent(phenId, composite);
        } else {
            return phenId.equals(phen.getId());
        }
    }

    protected boolean isIncludedField(String id, String desc, int index) {
        return (fieldIdFilters.isEmpty() || fieldIdFilters.contains(id) || (desc != null && fieldIdFilters.contains(desc))) &&
               (fieldIndexFilters.isEmpty()  || fieldIndexFilters.contains(index));
    }

    /**
     * Return the global phenomenon for a procedure.
     * We need this method because some procedure got multiple observation with only a phenomon component,
     * and not the full composite.
     * some other are registered with composite that are a subset of the global procedure phenomenon.
     *
     * @return
     */
    protected Phenomenon getGlobalCompositePhenomenon(String procedure) throws DataStoreException {
        try(final Connection c   = source.getConnection()) {
            return getGlobalCompositePhenomenon(c, procedure);
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }
    
    protected List applyPostPagination(List full) {
        if (offset == null && limit == null) return full;
        
        int from = 0;
        if (offset != null) {
            from = offset.intValue();
        }
        int to = full.size();
        if (limit != null) {
            to = from + limit.intValue();
            if (to >= full.size()) {
                to = full.size();
            }
        }
        if (from > to) {
            full.clear();
            return full;
        }
        return full.subList(from, to);
    }

    protected Map applyPostPagination(Map<?, ?> full) {
        int from = 0;
        if (offset != null) {
            from = offset.intValue();
        }
        int to = full.size();
        if (limit != null) {
            to = from + limit.intValue();
            if (to >= full.size()) {
                to = full.size();
            }
        }
        if (from > to) {
            full.clear();
            return full;
        }
        Map result = new LinkedHashMap();
        Iterator it = full.entrySet().iterator();
        int i = 0;
        while (it.hasNext() && i < to) {
             Entry e = (Entry) it.next();
            if (i >= from) {
                result.put(e.getKey(), e.getValue());
            }
            i++;
        }
        return result;
    }
}
