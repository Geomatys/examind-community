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
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.opengis.observation.Phenomenon;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import static org.constellation.api.CommonConstants.EVENT_TIME;

import org.geotoolkit.observation.model.Field;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import static org.constellation.store.observation.db.OM2BaseReader.defaultCRS;
import org.geotoolkit.geometry.jts.JTS;
import static org.geotoolkit.observation.ObservationFilterFlags.*;
import org.geotoolkit.observation.model.FieldPhenomenon;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.model.OMEntity.HISTORICAL_LOCATION;
import static org.geotoolkit.observation.model.OMEntity.LOCATION;
import static org.geotoolkit.observation.OMUtils.*;
import org.geotoolkit.observation.model.FieldType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildCompositePhenomenon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Literal;
import org.opengis.filter.TemporalOperator;
import org.opengis.filter.TemporalOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.observation.CompositePhenomenon;
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
    protected FilterSQLRequest sqlMeasureRequest = new FilterSQLRequest();

    protected final DataSource source;

    protected boolean template = false;

    protected boolean firstFilter = true;

    protected QName resultModel;

    protected boolean offJoin      = false;
    protected boolean obsJoin      = false;
    protected boolean procJoin     = false;
    protected boolean procDescJoin = false;

    protected boolean includeFoiInTemplate = true;
    protected boolean singleObservedPropertyInTemplate = false;
    protected boolean noCompositePhenomenon = false;

    protected OMEntity objectType = null;

    protected ResponseModeType responseMode;
    protected String currentProcedure = null;
    protected String currentOMType = null;

    protected List<String> currentFields = new ArrayList<>();

    protected List<Integer> fieldFilters = new ArrayList<>();

    protected List<Integer> measureIdFilters = new ArrayList<>();

    protected GeneralEnvelope envelopeFilter = null;

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
    public void init(OMEntity objectType, Map<String, Object> hints) throws DataStoreException {
        this.objectType = objectType;
        switch (objectType) {
            case FEATURE_OF_INTEREST: initFilterGetFeatureOfInterest(); break;
            case OBSERVED_PROPERTY:   initFilterGetPhenomenon(hints); break;
            case PROCEDURE:           initFilterGetSensor(); break;
            case OFFERING:            initFilterOffering(); break;
            case LOCATION:            initFilterGetLocations(); break;
            case HISTORICAL_LOCATION: initFilterGetHistoricalLocations(); break;
            case OBSERVATION:         initFilterObservation(hints); break;
            case RESULT:              initFilterGetResult(hints); break;
            default: throw new DataStoreException("unexpected object type:" + objectType);
        }
    }

    private void initFilterObservation(final Map<String, Object> hints) {
        this.responseMode    = (ResponseModeType) hints.get("responseMode");
        this.resultModel     = (QName) hints.get("resultModel");
        includeFoiInTemplate = getBooleanHint(hints, INCLUDE_FOI_IN_TEMPLATE, true);
        singleObservedPropertyInTemplate = MEASUREMENT_QNAME.equals(resultModel) && ResponseModeType.RESULT_TEMPLATE.equals(responseMode);
        if (ResponseModeType.RESULT_TEMPLATE.equals(responseMode)) {
            sqlRequest = new FilterSQLRequest("SELECT distinct  o.\"procedure\"");
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
            sqlRequest = new FilterSQLRequest("SELECT o.\"id\", o.\"identifier\", \"observed_property\", \"procedure\", \"foi\", \"time_begin\", \"time_end\" FROM \"");
            sqlRequest.append(schemaPrefix).append("om\".\"observations\" o WHERE \"identifier\" NOT LIKE ").appendValue(observationTemplateIdBase + '%').append(" ");
            firstFilter = false;
        }
    }

    private void initFilterGetResult(final Map<String, Object> hints) {
        firstFilter = false;
        this.responseMode = (ResponseModeType) hints.get("responseMode");
        currentProcedure  = (String) hints.get("procedure");
        try(final Connection c = source.getConnection()) {
            final int pid = getPIDFromProcedure(currentProcedure, c);
            currentOMType = getProcedureOMType(currentProcedure, c);
            sqlRequest = new FilterSQLRequest("SELECT m.* "
                                            + "FROM \"" + schemaPrefix + "om\".\"observations\" o, \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                            + "WHERE o.\"id\" = m.\"id_observation\"");

            //we add to the request the property of the template
            sqlRequest.append(" AND \"procedure\"=").appendValue(currentProcedure).append(" ");
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while initailizing getResultFilter", ex);
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
        sqlRequest = new FilterSQLRequest("SELECT distinct sf.\"id\", sf.\"name\", sf.\"description\", sf.\"sampledfeature\", sf.\"crs\", ").append(geomColum).append(" FROM \"")
                    .append(schemaPrefix).append("om\".\"sampling_features\" sf WHERE ");
        obsJoin = false;
    }

    private void initFilterGetPhenomenon(final Map<String, Object> hints) {
        sqlRequest = new FilterSQLRequest("SELECT op.\"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" op ");
        noCompositePhenomenon = getBooleanHint(hints, NO_COMPOSITE_PHENOMENON, false);
        if (noCompositePhenomenon) {
            sqlRequest.append(" WHERE op.\"id\" NOT IN (SELECT \"phenomenon\" FROM \"").append(schemaPrefix).append("om\".\"components\") ");
            firstFilter = false;
        } else {
            sqlRequest.append(" WHERE ");
            firstFilter = true;
        }
    }

    private void initFilterGetSensor() {
        sqlRequest = new FilterSQLRequest("SELECT distinct(pr.\"id\") FROM \"" + schemaPrefix + "om\".\"procedures\" pr WHERE ");
        firstFilter = true;
    }

    private void initFilterOffering() throws DataStoreException {
        sqlRequest = new FilterSQLRequest("SELECT off.\"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" off WHERE ");
        firstFilter = true;
    }

    private void initFilterGetLocations() throws DataStoreException {
        String geomColum;
        if (isPostgres) {
            geomColum = "st_asBinary(\"shape\") as \"location\"";
        } else {
            geomColum = "\"shape\"";
        }
        sqlRequest = new FilterSQLRequest("SELECT pr.\"id\", ")
                .append(geomColum).append(", pr.\"crs\" FROM \"")
                .append(schemaPrefix).append("om\".\"procedures\" pr WHERE ");
        firstFilter = true;
    }

    private void initFilterGetHistoricalLocations() throws DataStoreException {
        String geomColum;
        if (isPostgres) {
            geomColum = "st_asBinary(\"location\") as \"location\"";
        } else {
            geomColum = "\"location\"";
        }
        sqlRequest = new FilterSQLRequest("SELECT hl.\"procedure\", hl.\"time\", ")
                .append(geomColum).append(", hl.\"crs\" FROM \"")
                .append(schemaPrefix).append("om\".\"historical_locations\" hl WHERE ");
        firstFilter = true;
    }

    private void initFilterGetProcedureTimes() throws DataStoreException {
        sqlRequest = new FilterSQLRequest("SELECT hl.\"procedure\", hl.\"time\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" hl WHERE ");
        firstFilter = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcedure(final List<String> procedures) {
        if (procedures != null && !procedures.isEmpty()) {
            final String tableName;
            if (HISTORICAL_LOCATION.equals(objectType)) {
                tableName = "hl";
            } else {
                tableName = "o";
            }
            if (firstFilter) {
                sqlRequest.append(" ( ");
            } else {
                sqlRequest.append("AND ( ");
            }
            for (String s : procedures) {
                if (s != null) {
                    sqlRequest.append(" ").append(tableName).append(".\"procedure\"=").appendValue(s).append(" OR ");
                }
            }
            sqlRequest.delete(sqlRequest.length() - 3, sqlRequest.length());
            sqlRequest.append(") ");
            firstFilter = false;
            obsJoin = true;
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
                final FilterSQLRequest sbPheno = new FilterSQLRequest();
                for (String p : phenomenon) {
                    sbPheno.append(" \"id\"=").appendValue(p).append(" OR ");
                    // try to be flexible and allow to call this ommiting phenomenon id base
                    if (!p.startsWith(phenomenonIdBase)) {
                        sbPheno.append(" \"id\"=").appendValue(phenomenonIdBase + p).append(" OR ");
                    }
                    fields.addAll(getFieldsForPhenomenon(p));
                }
                sbPheno.delete(sbPheno.length() - 3, sbPheno.length());
                sb = sbPheno;
            } else {
                if (singleObservedPropertyInTemplate) {
                    final FilterSQLRequest sbPheno = new FilterSQLRequest();
                    for (String p : phenomenon) {
                        sbPheno.append(" (CASE WHEN \"component\" IS NULL THEN o.\"observed_property\" ELSE \"component\" END) = ").appendValue(p).append(" OR ");
                        fields.addAll(getFieldsForPhenomenon(p));
                    }
                    sbPheno.delete(sbPheno.length() - 3, sbPheno.length());
                    sb = sbPheno;
                } else {
                    final FilterSQLRequest sbPheno = new FilterSQLRequest();
                    final FilterSQLRequest sbCompo = new FilterSQLRequest(" OR \"observed_property\" IN (SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE ");
                    for (String p : phenomenon) {
                        sbPheno.append(" \"observed_property\"=").appendValue(p).append(" OR ");
                        sbCompo.append(" \"component\"=").appendValue(p).append(" OR ");
                        fields.addAll(getFieldsForPhenomenon(p));
                    }
                    sbPheno.delete(sbPheno.length() - 3, sbPheno.length());
                    sbCompo.delete(sbCompo.length() - 3, sbCompo.length());
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
            final Phenomenon phen = getPhenomenon("1.0.0", phenomenon, c);
            if (phen instanceof CompositePhenomenon) {
                final CompositePhenomenon compo = (CompositePhenomenon) phen;
                for (Phenomenon child : compo.getComponent()) {
                    results.add(((org.geotoolkit.swe.xml.Phenomenon)child).getId());
                }
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
            final FilterSQLRequest sb = new FilterSQLRequest();
            for (String foi : fois) {
                sb.append("(\"foi\"=").appendValue(foi).append(") OR");
            }
            sb.delete(sb.length() - 3, sb.length());

            if (!firstFilter) {
                sqlRequest.append(" AND( ").append(sb).append(") ");
            } else {
                sqlRequest.append(" (").append(sb).append(") ");
                firstFilter = false;
            }
            obsJoin = true;
        }
    }

    @Override
    public void setObservationIds(List<String> ids) {
        if (!ids.isEmpty()) {
            boolean getPhen = OMEntity.OBSERVED_PROPERTY.equals(objectType);
            if (getPhen) {
                final FilterSQLRequest procSb  = new FilterSQLRequest();
                final FilterSQLRequest fieldSb = new FilterSQLRequest();
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
                                    phen = getGlobalCompositePhenomenon("2.0.0", procedureID);
                                    if (phen == null) {
                                        LOGGER.warning("Global phenomenon not found for procedure :" + procedureID);
                                    } else {
                                        fieldSb.append(" (op.\"id\" = '").append(getId(phen)).append("') OR");
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
                            fieldFilters.add(Integer.parseInt(component[1]));
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
                if (procSb.length() > 0) {
                    procSb.delete(procSb.length() - 3, procSb.length());
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(procSb).append(") ");
                    } else {
                        sqlRequest.append(procSb);
                        firstFilter = false;
                    }
                }
                if (fieldSb.length() > 0) {
                    fieldSb.delete(fieldSb.length() - 3, fieldSb.length());
                    if (!firstFilter) {
                        sqlRequest.append(" AND( ").append(fieldSb).append(") ");
                    } else {
                        sqlRequest.append(fieldSb);
                        firstFilter = false;
                    }
                }

            } else {
                final FilterSQLRequest procSb  = new FilterSQLRequest();
                final FilterSQLRequest fieldSb = new FilterSQLRequest();
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
                                measureIdFilters.add(Integer.parseInt(component[2]));
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
                                fieldFilters.add(Integer.parseInt(component[1]));
                                measureIdFilters.add(Integer.parseInt(component[2]));
                            } else if (component.length == 2) {
                                oid = component[0];
                                measureIdFilters.add(Integer.parseInt(component[1]));
                            }
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        } else {
                            procSb.append("(o.\"identifier\"=").appendValue(oid).append(") OR");
                        }
                    }
                }
                procSb.delete(procSb.length() - 3, procSb.length());
                if (fieldSb.length() > 0) {
                    fieldSb.delete(fieldSb.length() - 3, fieldSb.length());
                }
                if (!firstFilter) {
                    sqlRequest.append(" AND( ").append(procSb).append(") ");
                } else {
                    sqlRequest.append(procSb);
                    firstFilter = false;
                }
                if (fieldSb.length() > 0) {
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
            String index = propertyName.substring(opos + 1, cpos);

            // if the field is not a number this will fail.
            sqlMeasureRequest.append(" AND ($phen").append(index).append(operator).appendObjectValue(value.getValue()).append(")");

        // apply only on all phenonemenon
        } else {
            sqlMeasureRequest.append(" ${allphen").append(operator).appendObjectValue(value.getValue()).append("} ");
        }
    }

    private String getSQLOperator(final BinaryComparisonOperator filter) throws ObservationStoreException {
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
     * {@inheritDoc}
     */
    @Override
    public List<String> supportedQueryableResultProperties() {
        // will work mostly for STS
        return Arrays.asList("result");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationResult> filterResult(Map<String, Object> hints) throws DataStoreException {
        LOGGER.log(Level.FINER, "request:{0}", sqlRequest.toString());
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        LOGGER.fine(sqlRequest.toString());
        try (final Connection c                   = source.getConnection();
            final PreparedStatement pstmt         = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet result                = pstmt.executeQuery()) {
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

    private List<String> filterObservation(Map<String, Object> hints) throws DataStoreException {
        sqlRequest.append(" ORDER BY \"procedure\"");
        if (firstFilter) {
            sqlRequest = sqlRequest.replaceFirst("WHERE", "");
        }
        LOGGER.fine(sqlRequest.toString());
        try(final Connection c               = source.getConnection();
            final PreparedStatement pstmt    = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs               = pstmt.executeQuery()) {
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
                    final int pid                 = getPIDFromProcedure(procedure, c);
                    final List<Field> fields      = readFields(procedure, true, c);
                    final Field mainField         = getMainField(procedure, c);
                    boolean isTimeField           = false;
                    if (mainField != null) {
                        isTimeField = FieldType.TIME.equals(mainField.type);
                    }

                    final String select;
                    if (MEASUREMENT_QNAME.equals(resultModel)) {
                        select = "m.*";
                    } else {
                        select = "\"id\"";
                    }
                    final String sqlRequest;
                    if (isTimeField) {
                        sqlRequest = "SELECT " + select + " FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                + "WHERE \"id_observation\" = ? " + sqlMeasureRequest.replaceAll("$time", mainField.name)
                                + "ORDER BY m.\"id\"";
                    } else {
                        sqlRequest = "SELECT " + select + " FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ? ORDER BY m.\"id\"";
                    }

                    if (MEASUREMENT_QNAME.equals(resultModel)) {
                        final Phenomenon phen = getPhenomenon("1.0.0", observedProperty, c);
                        List<FieldPhenomenon> fieldPhen = getPhenomenonFields(phen, fields, c);
                        LOGGER.fine(sqlRequest);
                        try (final PreparedStatement stmt = c.prepareStatement(sqlRequest)) {
                            stmt.setInt(1, oid);
                            try (final ResultSet rs2 = stmt.executeQuery()) {
                                while (rs2.next()) {
                                    final Integer rid = rs2.getInt("id");
                                    if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                        for (int i = 0; i < fieldPhen.size(); i++) {
                                            FieldPhenomenon field = fieldPhen.get(i);
                                            final String value = rs2.getString(field.getField().name);
                                            if (value != null) {
                                                results.add(name + '-' + field.getField().index + '-' + rid);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else {
                        LOGGER.fine(sqlRequest);
                        try (final PreparedStatement stmt = c.prepareStatement(sqlRequest)) {
                            stmt.setInt(1, oid);
                            try (final ResultSet rs2 = stmt.executeQuery()) {
                                while (rs2.next()) {
                                    final Integer rid = rs2.getInt("id");
                                    if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                        results.add(name + '-' + rid);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // TODO make a real pagination
            return applyPostPagination(hints, results);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString() + '\n' + ex.getMessage());
            throw new DataStoreException("the service has throw a SQL Exception.");
        }
    }

    private List<String> filterSensorLocations(Map<String, Object> hints) throws DataStoreException {
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

        LOGGER.fine(sqlRequest.toString());
        List<String> locations            = new ArrayList<>();
        try(final Connection c            = source.getConnection();
            final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
            final ResultSet rs            = pstmt.executeQuery()) {
            while (rs.next()) {
                try {
                    final String procedure = rs.getString("id");
                    
                     // exclude from spatial filter (will be removed when postgis filter will be set in request)
                    if (spaFilter != null) {
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
            locations = applyPostPagination(hints, locations);
        }
        return locations;
    }

    private String getFeatureOfInterestRequest(Map<String, Object> hints) {
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            final String obsJoin = "\"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"foi\" = sf.\"id\" ";
            request = request.replace("WHERE", obsJoin);
        } else {
            request = request.replace("\"foi\"='", "sf.\"id\"='");
            if (firstFilter) {
                request = request.replace("WHERE", "");
            }
        }
        return request;
    }

    private String getPhenomenonRequest(Map<String, Object> hints) {
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"observed_property\" = op.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        return request;
    }

    private String getProcedureRequest(Map<String, Object> hints) {
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = pr.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        return request;
    }

    private String getHistoricalLocationRequest(Map<String, Object> hints) {
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = hl.\"procedure\" AND (";
            // profile / single date ts
            obsJoin = obsJoin + "(hl.\"time\" = o.\"time_begin\" AND o.\"time_end\" IS NULL)  OR ";
            // period observation
             obsJoin = obsJoin + "( o.\"time_end\" IS NOT NULL AND hl.\"time\" >= o.\"time_begin\" AND hl.\"time\" <= o.\"time_end\")) ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        return request;
    }

    private String getOfferingRequest(Map<String, Object> hints) {
        sqlRequest = appendPaginationToRequest(sqlRequest, hints);
        String request = sqlRequest.toString();
        if (obsJoin && procJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o, \"" + schemaPrefix + "om\".\"procedures\" pr WHERE o.\"procedure\" = off.\"procedure\" AND pr.\"id\" = off.\"procedure\"";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"procedures\" o WHERE o.\"procedure\" = off.\"procedure\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (procJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"procedures\" pr WHERE pr.\"id\" = off.\"procedure\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        return request;
    }

    @Override
    public Set<String> getIdentifiers(Map<String, Object> hints) throws DataStoreException {
        if (objectType == null) {
            throw new DataStoreException("initialisation of the filter missing.");
        }
        String request;
        switch (objectType) {
            case FEATURE_OF_INTEREST: request = getFeatureOfInterestRequest(hints); break;
            case OBSERVED_PROPERTY:   request = getPhenomenonRequest(hints); break;
            case PROCEDURE:           request = getProcedureRequest(hints); break;
            case OFFERING:            request = getOfferingRequest(hints); break;
            case OBSERVATION:         return new LinkedHashSet(filterObservation(hints));
            case LOCATION:            return new LinkedHashSet(filterSensorLocations(hints));
            case HISTORICAL_LOCATION: 
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected object type:" + objectType);
        }

        final Set<String> results = new LinkedHashSet<>();

        LOGGER.fine(request.toString());
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
        Map<String, Object> hints = Collections.EMPTY_MAP;
        String request;
        switch (objectType) {
            case FEATURE_OF_INTEREST: request = getFeatureOfInterestRequest(hints); break;
            case OBSERVED_PROPERTY:   request = getPhenomenonRequest(hints); break;
            case PROCEDURE:           request = getProcedureRequest(hints); break;
            case OFFERING:            request = getOfferingRequest(hints); break;
            case HISTORICAL_LOCATION: request = getHistoricalLocationRequest(hints);break;
            case OBSERVATION:         return filterObservation(hints).size();
            case RESULT:              return filterResult(hints).size();
            case LOCATION:            return filterSensorLocations(hints).size();
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
    public String getInfos() {
        return "Constellation O&M 2 Filter 1.2-EE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBoundedObservation() {
        return false;
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
    public void setOfferings(final List<String> offerings) throws DataStoreException {
        if (offerings != null && !offerings.isEmpty()) {
            if (firstFilter) {
                sqlRequest.append(" ( ");
            } else {
                sqlRequest.append("AND ( ");
            }
            String block = " off.\"identifier\"=";
            if (OMEntity.OBSERVED_PROPERTY.equals(objectType) || OMEntity.FEATURE_OF_INTEREST.equals(objectType)) {
                block = " off.\"id_offering\"=";
            }
            for (String s : offerings) {
                if (s != null) {
                    sqlRequest.append(block).appendValue(s).append(" OR ");
                }
            }
            sqlRequest.delete(sqlRequest.length() - 3, sqlRequest.length());
            sqlRequest.append(") ");
            firstFilter = false;
            offJoin = true;
        }
    }

    @Override
    public boolean isDefaultTemplateTime() {
        return true;
    }

    @Override
    public void destroy() {
        //do nothing
    }

    protected FilterSQLRequest appendPaginationToRequest(FilterSQLRequest request, Map<String, Object> hints) {
        Long limit     = getLongHint(hints, PAGE_LIMIT);
        Long offset    = getLongHint(hints, PAGE_OFFSET);
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

    protected List<FieldPhenomenon> getPhenomenonFields(final Phenomenon fullPhen, List<Field> fields, Connection c) throws DataStoreException {
        final List<FieldPhenomenon> results = new ArrayList<>();
        for (Field f : fields) {
            if (isIncludedField(f.name, f.description, f.index) && isFieldInPhenomenon(f.name, fullPhen)) {
                Phenomenon phen = getPhenomenon("1.0.0", f.name, c);
                results.add(new FieldPhenomenon(phen, f));
            }
        }
        return results;
    }

    private boolean isFieldInPhenomenon(String phenId, Phenomenon phen) {
        if (phen instanceof CompositePhenomenon) {
                CompositePhenomenon composite = (CompositePhenomenon) phen;
            return hasComponent(phenId, composite);
        } else {
            return phenId.equals(getId(phen));
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
    protected Phenomenon getGlobalCompositePhenomenon(String version, String procedure) throws DataStoreException {
        try(final Connection c   = source.getConnection()) {
            return getGlobalCompositePhenomenon(version, c, procedure);
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * Return the global phenomenon for a procedure.
     * We need this method because some procedure got multiple observation with only a phenomon component,
     * and not the full composite.
     * some other are registered with composite that are a subset of the global procedure phenomenon.
     *
     * @return
     */
    protected Phenomenon getGlobalCompositePhenomenon(String version, Connection c, String procedure) throws DataStoreException {
       String request = "SELECT DISTINCT(\"observed_property\") FROM \"" + schemaPrefix + "om\".\"observations\" o, \"" + schemaPrefix + "om\".\"components\" c "
                      + "WHERE \"procedure\"=? ";
       LOGGER.fine(request);
       try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, procedure);
            try (final ResultSet rs   = stmt.executeQuery()) {
                List<CompositePhenomenon> composites = new ArrayList<>();
                List<Phenomenon> singles = new ArrayList<>();
                while (rs.next()) {
                    Phenomenon phen = getPhenomenon(version, rs.getString("observed_property"), c);
                    if (phen instanceof CompositePhenomenon) {
                        composites.add((CompositePhenomenon) phen);
                    } else {
                        singles.add(phen);
                    }
                }
                if (composites.isEmpty()) {
                    if (singles.isEmpty()) {
                        // i don't think this will ever happen
                        return null;
                    } else if (singles.size() == 1) {
                        return singles.get(0);
                    } else  {
                        // multiple phenomenons are present, but no composite... TODO
                        return getVirtualCompositePhenomenon(version, c, procedure);
                    }
                } else if (composites.size() == 1) {
                    return composites.get(0);
                } else  {
                    // multiple composite phenomenons are present, we must choose the global one
                    return getOverlappingComposite(composites);
                }
            }
       } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while looking for global phenomenon.", ex);
            throw new DataStoreException("Error while looking for global phenomenon.");
       }
    }

    protected Phenomenon getVirtualCompositePhenomenon(String version, Connection c, String procedure) throws DataStoreException {
       String request = "SELECT \"field_name\" FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" "
                      + "WHERE \"procedure\"=? "
                      + "AND NOT (\"order\"=1 AND \"field_type\"='Time') "
                      + "order by \"order\"";
       LOGGER.fine(request);
       try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, procedure);
            try (final ResultSet rs   = stmt.executeQuery()) {
                List<Phenomenon> components = new ArrayList<>();
                int i = 0;
                while (rs.next()) {
                    final String fieldName = rs.getString("field_name");
                    Phenomenon phen = getPhenomenon(version, fieldName, c);
                    if (phen == null) {
                        throw new DataStoreException("Unable to link a procedure field to a phenomenon:" + fieldName);
                    }
                    components.add(phen);
                }
                if (components.size() == 1) {
                    return components.get(0);
                } else {
                    final String name = "computed-phen-" + procedure;
                    return buildCompositePhenomenon(version, name, name, name,(String)null, components);
                }
            }
       } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while building virtual composite phenomenon.", ex);
            throw new DataStoreException("Error while building virtual composite phenomenon.");
       }
    }

    @SuppressWarnings("squid:S2695")
    protected TemporalGeometricPrimitive getTimeForTemplate(Connection c, String procedure, String observedProperty, String foi, String version) {
        String request = "SELECT min(\"time_begin\"), max(\"time_begin\"), max(\"time_end\") FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?";
        if (observedProperty != null) {
             request = request + " AND (\"observed_property\"=? OR \"observed_property\" IN (SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"component\"=?))";
        }
        if (foi != null) {
            request = request + " AND \"foi\"=?";
        }
        LOGGER.fine(request);
        try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, procedure);
            int cpt = 2;
            if (observedProperty != null) {
                stmt.setString(cpt, observedProperty);
                cpt++;
                stmt.setString(cpt, observedProperty);
                cpt++;
            }
            if (foi != null) {
                stmt.setString(cpt, foi);
            }
            try (final ResultSet rs   = stmt.executeQuery()) {
                if (rs.next()) {
                    Date minBegin = rs.getTimestamp(1);
                    Date maxBegin = rs.getTimestamp(2);
                    Date maxEnd   = rs.getTimestamp(3);
                    if (minBegin != null && maxEnd != null && maxEnd.after(maxBegin)) {
                        return SOSXmlFactory.buildTimePeriod(version, minBegin, maxEnd);
                    } else if (minBegin != null && !minBegin.equals(maxBegin)) {
                        return SOSXmlFactory.buildTimePeriod(version, minBegin, maxBegin);
                    } else if (minBegin != null) {
                        return SOSXmlFactory.buildTimeInstant(version, minBegin);
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while looking for template time.", ex);
        }
        return null;
    }

    protected List applyPostPagination(Map<String, Object> hints, List full) {
        Long limit     = getLongHint(hints, PAGE_LIMIT);
        Long offset    = getLongHint(hints, PAGE_OFFSET);
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

    protected Map applyPostPagination(Map<String, Object> hints, Map<?, ?> full) {
        Long limit     = getLongHint(hints, PAGE_LIMIT);
        Long offset    = getLongHint(hints, PAGE_OFFSET);
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
