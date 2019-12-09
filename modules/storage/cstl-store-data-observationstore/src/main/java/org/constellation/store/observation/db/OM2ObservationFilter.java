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

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationFilter;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.swe.xml.CompositePhenomenon;
import org.opengis.observation.Phenomenon;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static org.constellation.api.CommonConstants.EVENT_TIME;
import static org.geotoolkit.observation.Utils.getTimeValue;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2ObservationFilter extends OM2BaseReader implements ObservationFilter {


    protected StringBuilder sqlRequest;
    protected StringBuilder sqlMeasureRequest = new StringBuilder();

    protected final DataSource source;

    protected boolean template = false;

    protected boolean firstFilter = true;

    protected QName resultModel;

    protected boolean obsJoin = false;
    protected boolean includeFoiInTemplate = true;

    protected boolean getFOI = false;
    protected boolean getPhen = false;
    protected boolean getProc = false;

    protected String currentProcedure = null;

    protected List<String> currentFields = new ArrayList<>();

    protected List<Integer> fieldFilters = new ArrayList<>();

    protected List<Integer> measureIdFilters = new ArrayList<>();

    /**
     * Clone a new Observation Filter.
     *
     * @param omFilter
     */
    public OM2ObservationFilter(final OM2ObservationFilter omFilter) {
        super(omFilter);
        this.source                    = omFilter.source;
        this.template                  = false;
        this.getFOI                    = false;
        resultModel                    = null;

    }

    public OM2ObservationFilter(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties) throws DataStoreException {
        super(properties, schemaPrefix);
        this.source     = source;
        this.isPostgres = isPostgres;
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
    public void initFilterObservation(final ResponseModeType requestMode, final QName resultModel, final Map<String,String> hints) {
        if (hints != null && hints.containsKey("includeFoiInTemplate")) {
            includeFoiInTemplate = Boolean.parseBoolean(hints.get("includeFoiInTemplate"));
        }
        if (ResponseModeType.RESULT_TEMPLATE.equals(requestMode)) {
            sqlRequest = new StringBuilder("SELECT distinct \"observed_property\", \"procedure\"");
            if (includeFoiInTemplate) {
                sqlRequest.append(", \"foi\"");
            }
            sqlRequest.append(" FROM \"").append(schemaPrefix).append("om\".\"observations\" o WHERE");
            template = true;
            firstFilter = true;
        } else {
            sqlRequest = new StringBuilder("SELECT o.\"id\", o.\"identifier\", \"observed_property\", \"procedure\", \"foi\", \"time_begin\", \"time_end\" FROM \"");
            sqlRequest.append(schemaPrefix).append("om\".\"observations\" o WHERE \"identifier\" NOT LIKE '").append(observationTemplateIdBase).append("%' ");
            firstFilter = false;
        }
        this.resultModel = resultModel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetResult(final String procedure, final QName resultModel, final Map<String,String> hints) {
        firstFilter = false;
        currentProcedure = procedure;
        try(final Connection c = source.getConnection()) {
            final int pid = getPIDFromProcedure(procedure, c);
            sqlRequest = new StringBuilder("SELECT m.* "
                                         + "FROM \"" + schemaPrefix + "om\".\"observations\" o, \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                         + "WHERE o.\"id\" = m.\"id_observation\"");

            //we add to the request the property of the template
            sqlRequest.append(" AND \"procedure\"='").append(procedure).append("'");
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while initailizing getResultFilter", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetFeatureOfInterest() {
        firstFilter = true;
        String geomColum;
        if (isPostgres) {
            geomColum = "st_asBinary(\"shape\") as \"shape\"";
        } else {
            geomColum = "\"shape\"";
        }
        sqlRequest = new StringBuilder("SELECT distinct sf.\"id\", sf.\"name\", sf.\"description\", sf.\"sampledfeature\", sf.\"crs\", ").append(geomColum).append(" FROM \"")
                    .append(schemaPrefix).append("om\".\"sampling_features\" sf WHERE ");
        obsJoin = false;

        getFOI = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetPhenomenon() {
        sqlRequest = new StringBuilder("SELECT op.\"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" op WHERE ");
        firstFilter = true;
        getPhen = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetSensor() {
        sqlRequest = new StringBuilder("SELECT pr.\"id\" FROM \"" + schemaPrefix + "om\".\"procedures\" pr WHERE ");
        firstFilter = true;
        getProc = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcedure(final List<String> procedures, final List<ObservationOffering> offerings) {
        if (procedures != null && !procedures.isEmpty()) {
            if (firstFilter) {
                sqlRequest.append(" ( ");
            } else {
                sqlRequest.append("AND ( ");
            }
            for (String s : procedures) {
                if (s != null) {
                    sqlRequest.append(" \"procedure\"='").append(s).append("' OR ");
                }
            }
            sqlRequest.delete(sqlRequest.length() - 3, sqlRequest.length());
            sqlRequest.append(") ");
            firstFilter = false;
            obsJoin = true;
        } else if (offerings != null && !offerings.isEmpty()) {

            if (firstFilter) {
                sqlRequest.append(" ( ");
            } else {
                sqlRequest.append("AND ( ");
            }
            //if is not specified we use all the process of the offering
            for (ObservationOffering off : offerings) {
                for (String proc : off.getProcedures()) {
                    sqlRequest.append(" \"procedure\"='").append(proc).append("' OR ");
                }
            }
            sqlRequest.delete(sqlRequest.length() - 3, sqlRequest.length());
            sqlRequest.append(") ");
            firstFilter = false;
            obsJoin = true;
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
        if (!phenomenon.isEmpty() && !allPhenonenon(phenomenon)) {
            final String sb;
            final Set<String> fields    = new HashSet<>();
            if (getPhen) {
                final StringBuilder sbPheno = new StringBuilder();
                for (String p : phenomenon) {
                    sbPheno.append(" \"id\"='").append(p).append("' OR ");
                    fields.addAll(getFieldsForPhenomenon(p));
                }
                sbPheno.delete(sbPheno.length() - 3, sbPheno.length());
                sb = sbPheno.toString();
            } else {
                final StringBuilder sbPheno = new StringBuilder();
                final StringBuilder sbCompo = new StringBuilder(" OR \"observed_property\" IN (SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE ");
                for (String p : phenomenon) {
                    sbPheno.append(" \"observed_property\"='").append(p).append("' OR ");
                    sbCompo.append(" \"component\"='").append(p).append("' OR ");
                    fields.addAll(getFieldsForPhenomenon(p));
                }
                sbPheno.delete(sbPheno.length() - 3, sbPheno.length());
                sbCompo.delete(sbCompo.length() - 3, sbCompo.length());
                sbCompo.append(')');
                sb = sbPheno.append(sbCompo).toString();
                obsJoin = true;
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
                    results.add(((org.geotoolkit.swe.xml.Phenomenon)child).getName().getCode());
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
        if (!fois.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (String foi : fois) {
                sb.append("(\"foi\"='").append(foi).append("') OR");
            }
            sb.delete(sb.length() - 3, sb.length());

            if (!firstFilter) {
                sqlRequest.append(" AND( ").append(sb).append(") ");
            } else {
                sqlRequest.append(sb);
                firstFilter = false;
            }
        }
    }

    @Override
    public void setObservationIds(List<String> ids) {
        if (!ids.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
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
                        int pos = procedureID.indexOf("-");
                        if (pos != -1) {
                            fieldFilters.add(Integer.parseInt(procedureID.substring(pos + 1)));
                            procedureID = procedureID.substring(0, pos);
                        }
                        sb.append("(o.\"procedure\"='").append(sensorIdBase).append(procedureID).append("') OR");
                    } else if (oid.startsWith(observationIdBase)) {
                        String[] component = oid.split("-");
                        if (component.length == 3) {
                            oid = component[0];
                            fieldFilters.add(Integer.parseInt(component[1]));
                            measureIdFilters.add(Integer.parseInt(component[2]));
                        } else if (component.length == 2) {
                            oid = component[0];
                            fieldFilters.add(Integer.parseInt(component[1]));
                        }
                        sb.append("(o.\"identifier\"='").append(oid).append("') OR");
                    } else {
                        sb.append("(o.\"identifier\"='").append(oid).append("') OR");
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
                        int pos = procedureID.indexOf("-");
                        if (pos != -1) {
                            fieldFilters.add(Integer.parseInt(procedureID.substring(pos + 1)));
                            procedureID = procedureID.substring(0, pos);
                        }
                        sb.append("(o.\"procedure\"='").append(sensorIdBase).append(procedureID).append("') OR");
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
                        sb.append("(o.\"identifier\"='").append(oid).append("') OR");
                    } else {
                        sb.append("(o.\"identifier\"='").append(oid).append("') OR");
                    }
                }
            }
            sb.delete(sb.length() - 3, sb.length());
            if (!firstFilter) {
                sqlRequest.append(" AND( ").append(sb).append(") ");
            } else {
                sqlRequest.append(sb);
                firstFilter = false;
            }
            obsJoin = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeEquals(final Object time) throws DataStoreException {
        if (time instanceof Period) {
            final Period tp    = (Period) time;
            final String begin = getTimeValue(tp.getBeginning().getDate());
            final String end   = getTimeValue(tp.getEnding().getDate());

            // we request directly a multiple observation or a period observation (one measure during a period)
            sqlRequest.append("AND (");
            sqlRequest.append(" \"time_begin\"='").append(begin).append("' AND ");
            sqlRequest.append(" \"time_end\"='").append(end).append("') ");

            obsJoin = true;
        // if the temporal object is a timeInstant
        } else if (time instanceof Instant) {
            final Instant ti      = (Instant) time;
            final String position = getTimeValue(ti.getDate());
            sqlRequest.append("AND (");

            // case 1 a single observation
            sqlRequest.append("(\"time_begin\"='").append(position).append("' AND \"time_end\" IS NULL)");
            sqlRequest.append(" OR ");

            //case 2 multiple observations containing a matching value
            sqlRequest.append("(\"time_begin\"<='").append(position).append("' AND \"time_end\">='").append(position).append("'))");

            obsJoin = true;
        } else {
            throw new ObservationStoreException("TM_Equals operation require timeInstant or TimePeriod!",
                    INVALID_PARAMETER_VALUE, EVENT_TIME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeBefore(final Object time) throws DataStoreException  {
        // for the operation before the temporal object must be an timeInstant
        if (time instanceof Instant) {
            final Instant ti      = (Instant) time;
            final String position = getTimeValue(ti.getDate());
            sqlRequest.append("AND (");

            // the single and multpile observations which begin after the bound
            sqlRequest.append("(\"time_begin\"<='").append(position).append("'))");

            obsJoin = true;
        } else {
            throw new ObservationStoreException("TM_Before operation require timeInstant!",
                    INVALID_PARAMETER_VALUE, EVENT_TIME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeAfter(final Object time) throws DataStoreException {
        // for the operation after the temporal object must be an timeInstant
        if (time instanceof Instant) {
            final Instant ti      = (Instant) time;
            final String position = getTimeValue(ti.getDate());
            sqlRequest.append("AND (");

            // the single and multpile observations which begin after the bound
            sqlRequest.append("(\"time_begin\">='").append(position).append("')");
            sqlRequest.append(" OR ");
            // the multiple observations overlapping the bound
            sqlRequest.append("(\"time_begin\"<='").append(position).append("' AND \"time_end\">='").append(position).append("'))");

            obsJoin = true;
        } else {
            throw new ObservationStoreException("TM_After operation require timeInstant!",
                    INVALID_PARAMETER_VALUE, EVENT_TIME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeDuring(final Object time) throws DataStoreException {
        if (time instanceof Period) {
            final Period tp    = (Period) time;
            final String begin = getTimeValue(tp.getBeginning().getDate());
            final String end   = getTimeValue(tp.getEnding().getDate());
            sqlRequest.append("AND (");

            // the multiple observations included in the period
            sqlRequest.append(" (\"time_begin\">='").append(begin).append("' AND \"time_end\"<= '").append(end).append("')");
            sqlRequest.append(" OR ");
            // the single observations included in the period
            sqlRequest.append(" (\"time_begin\">='").append(begin).append("' AND \"time_begin\"<='").append(end).append("' AND \"time_end\" IS NULL)");
            sqlRequest.append(" OR ");
            // the multiple observations which overlaps the first bound
            sqlRequest.append(" (\"time_begin\"<='").append(begin).append("' AND \"time_end\"<= '").append(end).append("' AND \"time_end\">='").append(begin).append("')");
            sqlRequest.append(" OR ");
            // the multiple observations which overlaps the second bound
            sqlRequest.append(" (\"time_begin\">='").append(begin).append("' AND \"time_end\">= '").append(end).append("' AND \"time_begin\"<='").append(end).append("')");
            sqlRequest.append(" OR ");
            // the multiple observations which overlaps the whole period
            sqlRequest.append(" (\"time_begin\"<='").append(begin).append("' AND \"time_end\">= '").append(end).append("'))");

            obsJoin = true;
        } else {
            throw new ObservationStoreException("TM_During operation require TimePeriod!",
                    INVALID_PARAMETER_VALUE, EVENT_TIME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setResultEquals(final String propertyName, final String value) throws DataStoreException{
        throw new DataStoreException("setResultEquals is not supported by this ObservationFilter implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> supportedQueryableResultProperties() {
        return new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationResult> filterResult() throws DataStoreException {
        LOGGER.log(Level.FINER, "request:{0}", sqlRequest.toString());
        try(final Connection c                    = source.getConnection();
            final Statement currentStatement      = c.createStatement();
            final ResultSet result                = currentStatement.executeQuery(sqlRequest.toString())) {
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
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterObservation() throws DataStoreException {
        LOGGER.log(Level.FINER, "request:{0}", sqlRequest.toString());
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery(sqlRequest.toString())) {
            final Set<String> results        = new LinkedHashSet<>();
            final List<String> procedures    = new ArrayList<>();
            while (result.next()) {
                final String procedure = result.getString("procedure");
                if (!template || !procedures.contains(procedure)) {
                    results.add(result.getString("id"));
                    procedures.add(procedure);
                }
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterFeatureOfInterest() throws DataStoreException {

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

        LOGGER.log(Level.FINER, "request:{0}", request);
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery(request)) {
            final Set<String> results        = new LinkedHashSet<>();
            while (result.next()) {
                results.add(result.getString("id"));
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterPhenomenon() throws DataStoreException {

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

        LOGGER.log(Level.FINER, "request:{0}", request);
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery(request)) {
            final Set<String> results        = new LinkedHashSet<>();
            while (result.next()) {
                results.add(result.getString("id"));
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterProcedure() throws DataStoreException {
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

        LOGGER.log(Level.FINER, "request:{0}", request);
        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement();
            final ResultSet result           = currentStatement.executeQuery(request)) {
            final Set<String> results        = new LinkedHashSet<>();
            while (result.next()) {
                results.add(result.getString("id"));
            }
            return results;
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
        throw new DataStoreException("SetBoundingBox is not supported by this ObservationFilter implementation.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        //do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLoglevel(final Level logLevel) {
         //do nothing
    }

    @Override
    public void setTimeLatest() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTimeFirst() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOfferings(final List<ObservationOffering> offerings) throws DataStoreException {
        // not used in this implementations
    }

    @Override
    public boolean isDefaultTemplateTime() {
        return true;
    }

    @Override
    public void destroy() {
        //do nothing
    }

    public Field getMainField(final String procedure) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getMainField(procedure, c);
        } catch (SQLException ex) {
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }

    public Field getTimeField(final String procedure) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getTimeField(procedure, c);
        } catch (SQLException ex) {
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        }
    }
}
