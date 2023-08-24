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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.geometry.GeneralEnvelope;
import static org.constellation.api.CommonConstants.EVENT_TIME;
import org.geotoolkit.observation.model.Field;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import org.constellation.util.FilterSQLRequest.TableJoin;
import org.constellation.util.MultiFilterSQLRequest;
import org.constellation.util.SQLResult;
import org.constellation.util.SingleFilterSQLRequest;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.model.OMEntity.*;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.Phenomenon;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.query.AbstractObservationQuery;
import org.geotoolkit.observation.query.HistoricalLocationQuery;
import org.geotoolkit.observation.query.ObservationQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ResultQuery;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.filter.BinaryComparisonOperator;
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
    protected FilterSQLRequest sqlMeasureRequest = new SingleFilterSQLRequest();

    protected final DataSource source;

    protected boolean template = false;

    protected boolean firstFilter = true;

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
    protected String currentProcedure = null;
    protected String currentOMType = null;

    protected List<String> currentFields = new ArrayList<>();

    protected List<Integer> fieldFilters = new ArrayList<>();

    protected List<Integer> measureIdFilters = new ArrayList<>();

    protected GeneralEnvelope envelopeFilter = null;

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

    public OM2ObservationFilter(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties, final boolean timescaleDB) throws DataStoreException {
        super(properties, schemaPrefix, true, isPostgres, timescaleDB);
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
            sqlRequest = new SingleFilterSQLRequest("SELECT distinct o.\"procedure\"");
            if (singleObservedPropertyInTemplate) {
                sqlRequest.append(", (CASE WHEN \"component\" IS NULL THEN o.\"observed_property\" ELSE \"component\" END) AS \"obsprop\", pd.\"order\", pd.\"uom\"");
            } 
            if (includeFoiInTemplate) {
                sqlRequest.append(", \"foi\"");
            }
            if (singleObservedPropertyInTemplate) {
                sqlRequest.append(" FROM \"").append(schemaPrefix).append("om\".\"observations\" o");
                sqlRequest.append(" LEFT JOIN \"").append(schemaPrefix).append("om\".\"components\" c ON o.\"observed_property\" = c.\"phenomenon\" , \"").append(schemaPrefix).append("om\".\"procedure_descriptions\" pd");
                sqlRequest.append(" WHERE (CASE WHEN c.\"component\" IS NULL THEN o.\"observed_property\" ELSE \"component\" END) = pd.\"field_name\" ");
                sqlRequest.append(" AND pd.\"procedure\" = o.\"procedure\"");
                firstFilter = false;
            } else {
                sqlRequest.append(" FROM \"").append(schemaPrefix).append("om\".\"observations\" o");
                sqlRequest.append(" WHERE ");
                firstFilter = true;
            }

            template = true;
        } else {
            sqlRequest = new SingleFilterSQLRequest("SELECT o.\"id\", o.\"identifier\", \"observed_property\", \"procedure\", \"foi\", \"time_begin\", \"time_end\" FROM \"");
            sqlRequest.append(schemaPrefix).append("om\".\"observations\" o WHERE \"identifier\" NOT LIKE ").appendValue(observationTemplateIdBase + '%').append(" ");
            firstFilter = false;
        }
    }

    private void initFilterGetResult(ResultQuery query) throws DataStoreException {
        this.includeTimeForProfile = query.isIncludeTimeForProfile();
        this.responseMode          = query.getResponseMode();
        this.currentProcedure      = query.getProcedure();
        this.includeIDInDataBlock  = query.isIncludeIdInDataBlock();
        this.includeQualityFields  = query.isIncludeQualityFields();
        this.responseFormat        = query.getResponseFormat();
        this.decimationSize        = query.getDecimationSize();

        this.firstFilter = false;
        try(final Connection c = source.getConnection()) {
            final ProcedureInfo pi = getPIDFromProcedure(currentProcedure, c).orElse(null);
            if (pi == null) throw new DataStoreException("Unexisting procedure:" + currentProcedure);

            currentOMType = getProcedureOMType(currentProcedure, c);
            Field mainField = getMainField(currentProcedure, c);

            sqlRequest = buildMesureRequests(pi, mainField, null, "profile".equals(currentOMType), null, true, false, false);
            sqlRequest.append(" AND \"procedure\"=").appendValue(currentProcedure).append(" ");
        } catch (SQLException ex) {
            throw new DataStoreException("Error while initailizing getResultFilter", ex);
        }
    }

    private void initFilterGetFeatureOfInterest() {
        firstFilter = true;
        String geomColum;
        if (isPostgres) {
            geomColum = "st_asBinary(\"shape\") as \"shape\"";
        } else {
            geomColum = "\"shape\"";
        }
        sqlRequest = new SingleFilterSQLRequest("SELECT distinct sf.\"id\", sf.\"name\", sf.\"description\", sf.\"sampledfeature\", sf.\"crs\", ").append(geomColum).append(" FROM \"")
                    .append(schemaPrefix).append("om\".\"sampling_features\" sf WHERE ");
        obsJoin = false;
    }

    private void initFilterGetPhenomenon(ObservedPropertyQuery query) {
        this.noCompositePhenomenon = query.isNoCompositePhenomenon();
        sqlRequest = new SingleFilterSQLRequest("SELECT DISTINCT(op.\"id\") as \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" op ");
        if (noCompositePhenomenon) {
            sqlRequest.append(" WHERE op.\"id\" NOT IN (SELECT \"phenomenon\" FROM \"").append(schemaPrefix).append("om\".\"components\") ");
            firstFilter = false;
        } else {
            sqlRequest.append(" WHERE ");
            firstFilter = true;
        }
    }

    private void initFilterGetSensor() {
        sqlRequest = new SingleFilterSQLRequest("SELECT distinct(pr.\"id\") FROM \"" + schemaPrefix + "om\".\"procedures\" pr WHERE ");
        firstFilter = true;
    }

    private void initFilterOffering() throws DataStoreException {
        sqlRequest = new SingleFilterSQLRequest("SELECT off.\"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" off WHERE ");
        firstFilter = true;
    }

    private void initFilterGetLocations() throws DataStoreException {
        String geomColum;
        if (isPostgres) {
            geomColum = "st_asBinary(\"shape\") as \"location\"";
        } else {
            geomColum = "\"shape\"";
        }
        sqlRequest = new SingleFilterSQLRequest("SELECT pr.\"id\", ")
                .append(geomColum).append(", pr.\"crs\" FROM \"")
                .append(schemaPrefix).append("om\".\"procedures\" pr WHERE ");
        firstFilter = true;
    }

    private void initFilterGetHistoricalLocations(HistoricalLocationQuery query) throws DataStoreException {
        this.decimationSize = query.getDecimationSize();
        String geomColum;
        if (isPostgres) {
            geomColum = "st_asBinary(\"location\") as \"location\"";
        } else {
            geomColum = "\"location\"";
        }
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
                final FilterSQLRequest procSb  = new SingleFilterSQLRequest();
                final FilterSQLRequest fieldSb = new SingleFilterSQLRequest();
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
            } else {
                final String columnName;
                if (HISTORICAL_LOCATION.equals(objectType)) {
                    columnName = "hl.\"procedure\"";
                   // obsJoin = true;  TODO verify
                } else if (OFFERING.equals(objectType)) {
                    columnName = "off.\"procedure\"";
                } else if (PROCEDURE.equals(objectType) || LOCATION.equals(objectType)) {
                    columnName = "pr.\"id\"";
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
                    sbPheno.append(" op.\"id\"=").appendValue(p).append(" OR ");
                    // try to be flexible and allow to call this ommiting phenomenon id base
                    if (!p.startsWith(phenomenonIdBase)) {
                        sbPheno.append(" op.\"id\"=").appendValue(phenomenonIdBase + p).append(" OR ");
                    }
                    fields.addAll(getFieldsForPhenomenon(p));
                }
                sbPheno.deleteLastChar(3);
                sb = sbPheno;
            } else {
                if (singleObservedPropertyInTemplate) {
                    final FilterSQLRequest sbPheno = new SingleFilterSQLRequest();
                    for (String p : phenomenon) {
                        sbPheno.append(" (CASE WHEN \"component\" IS NULL THEN o.\"observed_property\" ELSE \"component\" END) = ").appendValue(p).append(" OR ");
                        fields.addAll(getFieldsForPhenomenon(p));
                    }
                    sbPheno.deleteLastChar(3);
                    sb = sbPheno;
                } else {
                    final FilterSQLRequest sbPheno = new SingleFilterSQLRequest();
                    final FilterSQLRequest sbCompo = new SingleFilterSQLRequest(" OR \"observed_property\" IN (SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE ");
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
                    currentFields.add(field);
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
            boolean getPhen = OMEntity.OBSERVED_PROPERTY.equals(objectType);
            if (getPhen) {
                final FilterSQLRequest procSb  = new SingleFilterSQLRequest();
                final FilterSQLRequest fieldSb = new SingleFilterSQLRequest();
               /*
                * in phenomenon mode 2 possibility :
                *   1) look for for observation for a template:
                *       - <template base> - <proc id>
                *       - <template base> - <proc id> - <field id>
                *   2) look for observation by id:
                *       - <observation id>
                *       - <observation id> - <measure id>
                *       - <observation id> - <field id> - <measure id>
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
                        

                    // we need to join with the proc!
                    } else if (oid.startsWith(observationIdBase)) {
                        String[] component = oid.split("-");
                        if (component.length == 3) {
                            oid = component[0];
                            fieldFilters.add(Integer.valueOf(component[1]));
                            int fieldId = Integer.parseInt(component[2]);
                            fieldSb.append("(pd.\"order\"=").appendValue(fieldId).append(") OR");
                        } else if (component.length == 2) {
                            oid = component[0];
                            int fieldId = Integer.parseInt(component[2]);
                            fieldSb.append("(pd.\"order\"=").appendValue(fieldId).append(") OR");
                        }
                        // ?? what to do with oid ?
                    } else {
                        // ?? what to do with oid ?
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
                final FilterSQLRequest procSb  = new SingleFilterSQLRequest();
                final FilterSQLRequest fieldSb = new SingleFilterSQLRequest();
               /*
                * in template mode 2 possibility :
                *   1) look for for a template by id:
                *       - <template base> - <proc id>
                *       - <template base> - <proc id> - <field id>
                *   2) look for a template for an observation id:
                *       - <observation id>
                *       - <observation id> - <field id>
                *       - <observation id> - <field id> - <measure id>
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
                                        fieldFilters.add(fieldIdentifier);
                                        fieldSb.append("(pd.\"order\"=").appendValue(fieldIdentifier).append(") OR");
                                    }
                                } catch (NumberFormatException ex) {}
                            }
                            if (existProcedure(sensorIdBase + procedureID)) {
                                procSb.append("(o.\"procedure\"=").appendValue(sensorIdBase + procedureID).append(") OR");
                            } else {
                                procSb.append("(o.\"procedure\"=").appendValue(procedureID).append(") OR");
                            }
                        } else if (oid.startsWith(observationIdBase)) {
                            String[] component = oid.split("-");
                            if (component.length == 3) {
                                oid = component[0];
                                int fieldId = Integer.parseInt(component[1]);
                                fieldFilters.add(fieldId);
                                fieldSb.append("(pd.\"order\"=").appendValue(fieldId).append(") OR");
                                measureIdFilters.add(Integer.valueOf(component[2]));
                            } else if (component.length == 2) {
                                oid = component[0];
                                int fieldId = Integer.parseInt(component[1]);
                                fieldFilters.add(fieldId);
                                fieldSb.append("(pd.\"order\"=").appendValue(fieldId).append(") OR");
                            }
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        } else {
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        }
                    }
               /*
                * in observations mode 2 possibility :
                *   1) look for for observation for a template:
                *       - <template base> - <proc id>
                *       - <template base> - <proc id> - <field id>
                *   2) look for observation by id:
                *       - <observation id>
                *       - <observation id> - <measure id>
                *       - <observation id> - <field id> - <measure id>
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
                                        fieldFilters.add(fieldIdentifier);
                                    }
                                } catch (NumberFormatException ex) {}
                            }
                            if (existProcedure(sensorIdBase + procedureID)) {
                                procSb.append("(o.\"procedure\"=").appendValue(sensorIdBase + procedureID).append(") OR");
                            } else {
                                procSb.append("(o.\"procedure\"=").appendValue(procedureID).append(") OR");
                            }
                        } else if (oid.startsWith(observationIdBase)) {
                            String[] component = oid.split("-");
                            if (component.length == 3) {
                                oid = component[0];
                                fieldFilters.add(Integer.valueOf(component[1]));
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
                }
                procSb.deleteLastChar(3);
                if (!fieldSb.isEmpty()) {
                    fieldSb.deleteLastChar(3);
                }
                if (!firstFilter) {
                    sqlRequest.append(" AND( ").append(procSb).append(") ");
                } else {
                    sqlRequest.append(procSb);
                    firstFilter = false;
                }
                if (!fieldSb.isEmpty()) {
                    sqlRequest.append(" AND( ").append(fieldSb).append(") ");
                }
                obsJoin = true;
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
        TemporalOperatorName type = tFilter.getOperatorType();

        final String tableAlias;
        if (objectType == OMEntity.OFFERING) {
            tableAlias = "off";
        } else if (objectType == OMEntity.HISTORICAL_LOCATION) {
            tableAlias = "hl";
        } else {
            tableAlias = "o";
            obsJoin = true;
        }

        if (firstFilter) {
            sqlRequest.append(" ( ");
            firstFilter = false;
        } else {
            sqlRequest.append("AND ( ");
        }

        if (type == TemporalOperatorName.EQUALS) {

            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            if (time instanceof Period tp) {
                final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
                final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());

                // we request directly a multiple observation or a period observation (one measure during a period)
                sqlRequest.append(" ").append(tableAlias).append(".\"time_begin\"=").appendValue(begin).append(" AND ");
                sqlRequest.append(" ").append(tableAlias).append(".\"time_end\"=").appendValue(end).append(") ");
            // if the temporal object is a timeInstant
            } else if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    sqlRequest.append(" ").append(tableAlias).append(".\"time\"=").appendValue(position).append(") ");
                } else {
                    OM2Utils.addtimeDuringSQLFilter(sqlRequest, ti, tableAlias);
                    sqlRequest.append(" ) ");
                    
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
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            // for the operation before the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    sqlRequest.append(" \"time\"<=").appendValue(position).append(")");
                } else {
                    sqlRequest.append(" ").append(tableAlias).append(".\"time_begin\"<=").appendValue(position).append(")");
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
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            // for the operation after the temporal object must be an timeInstant
            if (time instanceof Instant ti) {
                final Timestamp position = new Timestamp(ti.getDate().getTime());
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    sqlRequest.append(" \"time\">=").appendValue(position).append(")");
                } else {
                    sqlRequest.append("(").append(tableAlias).append(".\"time_end\">=").appendValue(position).append(") OR (").append(tableAlias).append(".\"time_end\" IS NULL AND ").append(tableAlias).append(".\"time_begin\" >=").appendValue(position).append("))");
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
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            if (time instanceof Period tp) {
                final Timestamp begin = new Timestamp(tp.getBeginning().getDate().getTime());
                final Timestamp end   = new Timestamp(tp.getEnding().getDate().getTime());
                if (objectType == OMEntity.HISTORICAL_LOCATION) {
                    sqlRequest.append(" \"time\">=").appendValue(begin).append(" AND \"time\"<=").appendValue(end).append(")");
                } else {
                    OM2Utils.addtimeDuringSQLFilter(sqlRequest, tp, tableAlias);
                    sqlRequest.append(" ) ");

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
                suffix = "_quality_" + suffix.substring(1);
            }
            // if the field is not a number this will fail.
            sqlMeasureRequest.append(" AND (\"$phen").append(index).append(suffix).append("\" ").append(operator).appendNamedObjectValue("phen" + index, value.getValue()).append(")");

        // apply only on all phenonemenon
        } else {
            sqlMeasureRequest.append(" ${allphen").append(operator).appendNamedObjectValue("allphen", value.getValue()).append("} ");
        }
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
            }
        }

        String propertyName = XPath.substring(pos + 11);
        if (firstFilter) {
            sqlRequest.append(" ( ");
        } else {
            sqlRequest.append("AND ( ");
        }

        String propColumnName;
        String valueColumnName;

        switch (targetEntity) {
            case OBSERVED_PROPERTY -> {
                propColumnName  = "opp.\"property_name\"";
                valueColumnName = "opp.\"value\"";
                phenPropJoin = true;
            }
            case PROCEDURE -> {
                propColumnName  = "prp.\"property_name\"";
                valueColumnName = "prp.\"value\"";
                procPropJoin = true;
            }
            case FEATURE_OF_INTEREST -> {
                propColumnName  = "sfp.\"property_name\"";
                valueColumnName = "sfp.\"value\"";
                foiPropJoin = true;
            }
            default -> {
                throw new ObservationStoreException("Unsuported property filter on entity:" + objectType);
            }
        }
        sqlRequest.append(propColumnName).append("=").appendValue(propertyName).append(" AND ");
        sqlRequest.append(valueColumnName).append(operator).appendObjectValue(value.getValue());
        sqlRequest.append(" ) ");
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
     * @param offset The index where starts the measure fields.
     * @param mainField Main field of the current observation.
     * @param fields fields list.
     *
     * @return a filtered measure request.
     */
    protected FilterSQLRequest applyFilterOnMeasureRequest(int offset, Field mainField, List<Field> fields, ProcedureInfo pti) {
        // some time filter may have already been set in the measure request
        MultiFilterSQLRequest result = new MultiFilterSQLRequest();
        for (int k = 1; k < pti.nbTable + 1; k++) {
            FilterSQLRequest single = sqlMeasureRequest.clone();
            single.replaceAll("$time", mainField.name);

           /**
            * there is an issue here in a measurement context.
            * The filter should be applied on each field separately
            * Actually the filter is apply on each field with a "AND"
            */
            final String allPhenKeyword = "${allphen";
            while (single.contains(allPhenKeyword)) {
                String measureFilter = single.getRequest();
                int opos = measureFilter.indexOf(allPhenKeyword);
                int cpos = measureFilter.indexOf("}", opos + allPhenKeyword.length());
                String block = measureFilter.substring(opos, cpos + 1);
                StringBuilder sb = new StringBuilder();
                int extraFilter = -1;
                for (int i = offset; i < fields.size(); i++) {
                    DbField field = (DbField) fields.get(i);
                    if (field.tableNumber == k) {
                        extraFilter++;
                        sb.append(" AND (").append(block.replace("${allphen", "\"" + field.name + "\"").replace('}', ' ')).append(") ");
                    }
                }
                single.replaceFirst(block, sb.toString());
                if (extraFilter == -1) {
                    // the filter has been removed, we need to remove the param
                    single.removeNamedParam("allphen");
                } else {
                    single.duplicateNamedParam("allphen", extraFilter);
                }
            }


            for (int i = offset; i < fields.size(); i++) {
                DbField field = (DbField) fields.get(i);
                if (field.tableNumber == k) {
                    single.replaceAll("$phen" + (i - offset), field.name);
                } else {
                    // we need to remove the filter fom the request, as it does not apply to this table
                    final String phenKeyword = " AND (\"$phen" + (i - offset);
                    while (single.contains(phenKeyword)) {
                        String measureFilter = single.getRequest();
                        int opos = measureFilter.indexOf(phenKeyword);
                        int cpos = measureFilter.indexOf(")", opos + phenKeyword.length());
                        String block = measureFilter.substring(opos, cpos + 1);
                        single.replaceFirst(block, "");
                        single.removeNamedParam("phen" + (i - offset));
                    }
                }
            }
            result.addRequest(single);
        }
        
        return result;
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

    private List<String> filterObservation() throws DataStoreException {
        List<TableJoin> joins = new ArrayList<>();
        if (phenPropJoin) {
            String joinColumn;
            if (MEASUREMENT_QNAME.equals(resultModel)) {
                joinColumn = "pd.\"field_name\"";
            } else {
                // there is a problem here. see comment in OM2ObservationFilterReader#getObservationTemplates()
                joinColumn = "o.\"observed_property\"";
            }
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "opp.\"id_phenomenon\" = " + joinColumn));
        }
        if (procPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedures_properties\" prp", "prp.\"id_procedure\" = o.\"procedure\""));
        }
        if (foiPropJoin) {
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"sampling_features_properties\" sfp", " sfp.\"id_sampling_feature\" = o.\"foi\""));
        }
        sqlRequest.join(joins, firstFilter);
        sqlRequest.append(" ORDER BY \"procedure\"");
        if (firstFilter) {
            sqlRequest = sqlRequest.replaceFirst("WHERE", "");
        }
        LOGGER.fine(sqlRequest.toString());
        try (final Connection c = source.getConnection();
             final SQLResult rs = sqlRequest.execute(c)) {
            final List<String> results       = new ArrayList<>();
            while (rs.next()) {
                final String procedure       = rs.getString("procedure");
                final String procedureID;
                if (procedure.startsWith(sensorIdBase)) {
                    procedureID      = procedure.substring(sensorIdBase.length());
                } else {
                    procedureID      = procedure;
                }
                if (template) {
                    if (MEASUREMENT_QNAME.equals(resultModel)) {
                        final String index = rs.getString("order");
                        results.add(observationTemplateIdBase + procedureID + '-' + index);
                    } else {
                        final String name = observationTemplateIdBase + procedureID;
                        results.add(name);
                    }
                } else {
                    final int oid                 = rs.getInt("id");
                    final String name             = rs.getString("identifier");
                    final String observedProperty = rs.getString("observed_property");
                    final List<Field> fields      = readFields(procedure, true, c);
                    final Field mainField         = getMainField(procedure, c);
                    final ProcedureInfo pti       = getPIDFromProcedure(procedure, c).orElseThrow(); // we know that the procedure exist
                    boolean profile               = !FieldType.TIME.equals(mainField.type);

                    final boolean idOnly = !MEASUREMENT_QNAME.equals(resultModel);
                    final FilterSQLRequest measureFilter = applyFilterOnMeasureRequest(0, mainField, fields, pti);
                    final FilterSQLRequest mesureRequest = buildMesureRequests(pti, mainField, measureFilter, profile, oid, false, true, idOnly);
                    LOGGER.fine(mesureRequest.toString());

                    if (MEASUREMENT_QNAME.equals(resultModel)) {
                        final Phenomenon phen = getPhenomenon(observedProperty, c);
                        List<Field> fieldPhen = getFieldsForPhenomenon(phen, fields, c);
                        
                        try (final SQLResult rs2 = mesureRequest.execute(c)) {
                            while (rs2.next()) {
                                final Integer rid = rs2.getInt("id", 0);
                                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                    for (int i = 0; i < fieldPhen.size(); i++) {
                                        DbField field = (DbField) fieldPhen.get(i);
                                        // in measurement mode we only want the non empty measure
                                        final String value = rs2.getString(field.name, field.tableNumber -1);
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
                            while (rs2.next()) {
                                final Integer rid = rs2.getInt("id", 0);
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
            return applyPostPagination(results);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString() + '\n' + ex.getMessage());
            throw new DataStoreException("the service has throw a SQL Exception.");
        }
    }

    private List<String> filterSensorLocations() throws DataStoreException {
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

        LOGGER.fine(sqlRequest.toString());
        List<String> locations            = new ArrayList<>();
        try(final Connection c = source.getConnection();
            final SQLResult rs = sqlRequest.execute(c)) {
            while (rs.next()) {
                try {
                    final String procedure = rs.getString("id");
                    
                     // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (spaFilter != null) {
                        final byte[] b = rs.getBytes(2);
                        final int srid = rs.getInt(3);
                        final CoordinateReferenceSystem crs= OM2Utils.parsePostgisCRS(srid);
                        final org.locationtech.jts.geom.Geometry geom;
                        if (b != null) {
                            WKBReader reader = new WKBReader();
                            geom             = reader.read(b);
                            JTS.setCRS(geom, crs);
                        } else {
                            continue;
                        }
                   
                        if (!spaFilter.intersects(geom)) {
                            continue;
                        }
                    }
                    // can it happen? if so the pagination will be broken
                    if (!locations.contains(procedure)) {
                        locations.add(procedure);
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
            locations = applyPostPagination(locations);
        }
        return locations;
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

        // will be removed when postgis filter will be set in request
        Polygon spaFilter = null;
        if (envelopeFilter != null) {
            spaFilter = JTS.toGeometry(envelopeFilter);
        }
        
        boolean applyPostPagination = true;
        if (spaFilter == null) {
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

                    if (envelopeFilter != null) {
                        final byte[] b = rs.getBytes(3);
                        final int srid = rs.getInt(4);
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
                    }

                    // can it happen? if so the pagination will be broken
                    String hlid = procedure + "-" + time;
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

    private String getFeatureOfInterestRequest() {
        sqlRequest = appendPaginationToRequest(sqlRequest);
        List<TableJoin> joins = new ArrayList<>();
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
        return sqlRequest.toString();
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
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"observed_properties_properties\" opp", "op.\"id\" = opp.\"id_phenomenon\""));
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
            joins.add(new TableJoin("\"" + schemaPrefix +"om\".\"procedures_properties\" prp", "pr.\"id\" = prp.\"id_procedure\""));
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
            case FEATURE_OF_INTEREST: request = getFeatureOfInterestRequest(); break;
            case OBSERVED_PROPERTY:   request = getPhenomenonRequest(); break;
            case PROCEDURE:           request = getProcedureRequest(); break;
            case OFFERING:            request = getOfferingRequest(); break;
            case OBSERVATION:         return new LinkedHashSet(filterObservation());
            case LOCATION:            return new LinkedHashSet(filterSensorLocations());
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
            case FEATURE_OF_INTEREST: request = getFeatureOfInterestRequest(); break;
            case OBSERVED_PROPERTY:   request = getPhenomenonRequest(); break;
            case PROCEDURE:           request = getProcedureRequest(); break;
            case OFFERING:            request = getOfferingRequest(); break;
            case HISTORICAL_LOCATION: request = getHistoricalLocationRequest();break;
            case OBSERVATION:         return filterObservation().size();
            case RESULT:              return filterResult().size();
            case LOCATION:            return filterSensorLocations().size();
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
    public void setBoundingBox(final Envelope e) throws DataStoreException {
        if (LOCATION.equals(objectType) || HISTORICAL_LOCATION.equals(objectType)) {
            envelopeFilter = new GeneralEnvelope(e);
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
        if (isPostgres) {
            if (limit != null && limit > 0) {
                request.append(" LIMIT ").append(Long.toString(limit));
            }
            if (offset != null && offset > 0) {
                request.append(" OFFSET ").append(Long.toString(offset));
            }
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

    protected Field getMainField(final String procedure) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getMainField(procedure, c);
        } catch (SQLException ex) {
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }

    protected Field getTimeField(final String procedure) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getTimeField(procedure, c);
        } catch (SQLException ex) {
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
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

    protected Map<Field, Phenomenon> getPhenomenonFields(final Phenomenon fullPhen, List<Field> fields, Connection c) throws DataStoreException {
        final Map<Field, Phenomenon> results = new LinkedHashMap<>();
        for (Field f : fields) {
            if (isIncludedField(f.name, f.description, f.index) && isFieldInPhenomenon(f.name, fullPhen)) {
                Phenomenon phen = getPhenomenon(f.name, c);
                results.put(f, phen);
            }
        }
        return results;
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
        return (currentFields.isEmpty() || currentFields.contains(id) || (desc != null && currentFields.contains(desc))) &&
               (fieldFilters.isEmpty()  || fieldFilters.contains(index));
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
