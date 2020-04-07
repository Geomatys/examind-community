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

package org.constellation.sos.io.generic;

import org.apache.sis.storage.DataStoreException;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.generic.From;
import org.constellation.dto.service.config.generic.Query;
import org.constellation.dto.service.config.generic.Select;
import org.constellation.dto.service.config.generic.Where;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.observation.ObservationResult;
import org.geotoolkit.observation.ObservationStoreException;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;

import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static org.constellation.api.CommonConstants.EVENT_TIME;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.PROCEDURE;
import org.constellation.sos.ws.DatablockParser;
import static org.constellation.sos.ws.DatablockParser.getResultValues;
import org.geotoolkit.observation.ObservationReader;
import static org.geotoolkit.observation.Utils.getTimeValue;
import org.geotoolkit.ogc.xml.v200.TimeAfterType;
import org.geotoolkit.ogc.xml.v200.TimeBeforeType;
import org.geotoolkit.ogc.xml.v200.TimeDuringType;
import org.geotoolkit.ogc.xml.v200.TimeEqualsType;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.sos.xml.ResponseModeType.INLINE;
import static org.geotoolkit.sos.xml.ResponseModeType.RESULT_TEMPLATE;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.opengis.filter.Filter;
import org.opengis.geometry.Geometry;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.Process;
import org.opengis.observation.sampling.SamplingFeature;

/**
 *
 * @author Guilhem Legal
 */
public class GenericObservationFilter extends AbstractGenericObservationFilter {

    private ObservationReader reader;

    private String responseFormat;

    protected QName resultModel;

    protected ResponseModeType requestMode;

    protected final List<Filter> eventTimes = new ArrayList<>();

    /**
     * Clone a  Generic Observation Filter for CSTL O&amp;M datasource.
     * @param omFilter
     */
    public GenericObservationFilter(final GenericObservationFilter omFilter) {
        super(omFilter);
        this.reader= omFilter.reader;
    }

    /**
     * Build a new Generic Observation Filter for CSTL O&amp;M datasource.
     *
     * @param configuration
     * @param properties
     *
     * @throws DataStoreException
     */
    public GenericObservationFilter(final Automatic configuration, final Map<String, Object> properties, ObservationReader reader) throws DataStoreException {
        super(configuration, properties);
        this.reader = reader;
    }

    @Override
    protected Connection acquireConnection() throws SQLException {
        final Connection c = super.acquireConnection();
        c.setAutoCommit(true);
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterObservation(final ResponseModeType requestMode, final QName resultModel, Map<String, String> hints) {
        currentQuery              = new Query();
        final Select select       = new Select(configurationQuery.getSelect("filterObservation"));
        final From from;
        if (resultModel.equals(OBSERVATION_QNAME)) {
            from = new From(configurationQuery.getFrom("observations"));
        } else {
            from = new From(configurationQuery.getFrom("measurements"));
        }
        final Where where         = new Where(configurationQuery.getWhere("observationType"));

        if (requestMode == INLINE) {
            where.replaceVariable("observationIdBase", observationIdBase, false);
        } else if (requestMode == RESULT_TEMPLATE) {
            where.replaceVariable("observationIdBase", observationTemplateIdBase, false);
        }
        currentQuery.addSelect(select);
        currentQuery.addFrom(from);
        currentQuery.addWhere(where);
        eventTimes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetResult(final String procedure, final QName resultModel, Map<String, String> hints) {
        currentQuery              = new Query();
        final Select select       = new Select(configurationQuery.getSelect("filterResult"));
        final From from           = new From(configurationQuery.getFrom("observations"));
        final Where where         = new Where(configurationQuery.getWhere(PROCEDURE));

        where.replaceVariable(PROCEDURE, procedure, true);
        currentQuery.addSelect(select);
        currentQuery.addFrom(from);
        currentQuery.addWhere(where);
        eventTimes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetPhenomenon() throws DataStoreException {
        currentQuery              = new Query();
        final Select select       = new Select(configurationQuery.getSelect("filterPhenomenon"));
        final From from           = new From(configurationQuery.getFrom("observed_properties"));

        currentQuery.addSelect(select);
        currentQuery.addFrom(from);
        eventTimes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetSensor() throws DataStoreException {
        currentQuery              = new Query();
        final Select select       = new Select(configurationQuery.getSelect("filterSensor"));
        final From from           = new From(configurationQuery.getFrom("sensor"));

        currentQuery.addSelect(select);
        currentQuery.addFrom(from);
        eventTimes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initFilterGetFeatureOfInterest() throws DataStoreException {
        // do nothing no implemented
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcedure(final List<String> procedures) {
        if (!procedures.isEmpty()) {
            for (String s : procedures) {
                if (s != null) {
                    final Where where = new Where(configurationQuery.getWhere(PROCEDURE));
                    where.replaceVariable(PROCEDURE, s, true);
                    currentQuery.addWhere(where);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setObservedProperties(final List<String> phenomenon) {
        if (phenomenon != null) {
            for (String p : phenomenon) {
                if (p.contains(phenomenonIdBase)) {
                    p = p.replace(phenomenonIdBase, "");
                }
                final Where where = new Where(configurationQuery.getWhere("phenomenon"));
                where.replaceVariable("phenomenon", p, true);
                currentQuery.addWhere(where);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFeatureOfInterest(final List<String> fois) {
        if (fois != null) {
            for (String foi : fois) {
                final Where where = new Where(configurationQuery.getWhere("foi"));
                where.replaceVariable("foi", foi, true);
                currentQuery.addWhere(where);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setObservationIds(List<String> ids) {
        for (String oid : ids) {
            final Where where = new Where(configurationQuery.getWhere("oid"));
            where.replaceVariable("oid", oid, true);
            currentQuery.addWhere(where);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTimeEquals(final Object time) throws DataStoreException {
        eventTimes.add(new TimeEqualsType("result_time", time));
        if (time instanceof Period) {
            final Period tp    = (Period) time;
            final String begin = getTimeValue(tp.getBeginning().getDate());
            final String end   = getTimeValue(tp.getEnding().getDate());

            final Where where       = new Where(configurationQuery.getWhere("tequalsTP"));
            where.replaceVariable("begin", begin, true);
            where.replaceVariable("end", end, true);
            currentQuery.addWhere(where);

        // if the temporal object is a timeInstant
        } else if (time instanceof Instant) {
            final Instant ti = (Instant) time;
            final String position = getTimeValue(ti.getDate());

            final Where where = new Where(configurationQuery.getWhere("tequalsTI"));
            where.replaceVariable("position", position, true);
            currentQuery.addWhere(where);

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
        eventTimes.add(new TimeBeforeType("result_time", time));
        // for the operation before the temporal object must be an timeInstant
        if (time instanceof Instant) {
            final Instant ti = (Instant) time;
            final String position = getTimeValue(ti.getDate());

            final Where where = new Where(configurationQuery.getWhere("tbefore"));
            where.replaceVariable("time", position, true);
            currentQuery.addWhere(where);

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
        eventTimes.add(new TimeAfterType("result_time", time));
        // for the operation after the temporal object must be an timeInstant
        if (time instanceof Instant) {
            final Instant ti = (Instant) time;
            final String position    = getTimeValue(ti.getDate());

            final Where where        = new Where(configurationQuery.getWhere("tafter"));
            where.replaceVariable("time", position, true);
            currentQuery.addWhere(where);

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
        eventTimes.add(new TimeDuringType("result_time", time));
        if (time instanceof Period) {
            final Period tp    = (Period) time;
            final String begin = getTimeValue(tp.getBeginning().getDate());
            final String end   = getTimeValue(tp.getEnding().getDate());

            final Where where = new Where(configurationQuery.getWhere("tduring"));
            where.replaceVariable("begin", begin, true);
            where.replaceVariable("end", end, true);
            currentQuery.addWhere(where);

        } else {
            throw new ObservationStoreException("TM_During operation require TimePeriod!",
                    INVALID_PARAMETER_VALUE, EVENT_TIME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOfferings(final List<String> offerings) throws DataStoreException {
        // not used in this implementations
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationResult> filterResult() throws DataStoreException {
        final String request = currentQuery.buildSQLQuery();
        LOGGER.log(Level.INFO, "request:{0}", request);
        try {
            final List<ObservationResult> results     = new ArrayList<>();
            try (Connection connection                = acquireConnection();
                final Statement currentStatement      = connection.createStatement();
                final ResultSet result                = currentStatement.executeQuery(request)) {
                while (result.next()) {
                    results.add(new ObservationResult(result.getString(1),
                            result.getTimestamp(2),
                            result.getTimestamp(3)));
                }
            }
            return results;

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage() + '\n' + "while executing the request:" + request);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterObservation() throws DataStoreException {
        return extractIdQuery();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterPhenomenon() throws DataStoreException {
        return extractIdQuery();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> filterProcedure() throws DataStoreException {
        return extractIdQuery();
    }

    private Set<String> extractIdQuery() throws DataStoreException {
        final String request = currentQuery.buildSQLQuery();
        LOGGER.log(Level.INFO, "request:{0}", request);
        try {
            final Set<String> results            = new LinkedHashSet<>();
            try (Connection connection           = acquireConnection();
                final Statement currentStatement = connection.createStatement();
                final ResultSet result           = currentStatement.executeQuery(request)) {
                while (result.next()) {
                    results.add(result.getString(1));
                }
            }
            return results;
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQLException while executing the query: {0} \nmsg:{1}", new Object[]{request, ex.getMessage()});
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInfos() {
        return "Constellation Generic O&M Filter 1.2-EE";
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

    @Override
    public void setTimeLatest() throws DataStoreException {
        throw new DataStoreException("setTimeLatest is not supported by this ObservationFilter implementation.");
    }

    @Override
    public void setTimeFirst() throws DataStoreException {
        throw new DataStoreException("setTimeFirst is not supported by this ObservationFilter implementation.");
    }

    @Override
    public Set<String> filterFeatureOfInterest() throws DataStoreException {
        throw new DataStoreException("filterFeatureOfInterest is not supported by this ObservationFilter implementation.");
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void setProcedureType(String type) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initFilterOffering() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initFilterGetLocations() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void initFilterGetProcedureTimes() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<String> filterOffering() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * TODO Not sure if it is working.
     *
     * @param hints
     * @return
     * @throws DataStoreException
     */
    @Override
    public List<Observation> getObservationTemplates(Map<String, String> hints) throws DataStoreException {
        final String version                 = getVersionFromHints(hints);
        final List<Observation> observations = new ArrayList<>();
        final Set<String> oid = filterObservation();
        for (String foid : oid) {
            final Observation obs = reader.getObservation(foid, resultModel, requestMode, version);
            observations.add(obs);
        }
        return observations;
    }

    @Override
    public List<Observation> getObservations(Map<String, String> hints) throws DataStoreException {
        final String version                 = getVersionFromHints(hints);
        final List<Observation> observations = new ArrayList<>();
        final Set<String> oid = filterObservation();
        for (String foid : oid) {
            final Observation obs = reader.getObservation(foid, resultModel, requestMode, version);
            // parse result values to eliminate wrong results
            if (obs.getSamplingTime() instanceof Period) {
                final Timestamp tbegin;
                final Timestamp tend;
                final Period p = (Period)obs.getSamplingTime();
                if (p.getBeginning() != null && p.getBeginning().getDate() != null) {
                    tbegin = new Timestamp(p.getBeginning().getDate().getTime());
                } else {
                    tbegin = null;
                }
                if (p.getEnding() != null && p.getEnding().getDate() != null) {
                    tend = new Timestamp(p.getEnding().getDate().getTime());
                } else {
                    tend = null;
                }
                if (obs.getResult() instanceof DataArrayProperty) {
                    final DataArray array = ((DataArrayProperty)obs.getResult()).getDataArray();
                    final DatablockParser.Values result   = getResultValues(tbegin, tend, array, eventTimes);
                    array.setValues(result.values.toString());
                    array.setElementCount(result.nbBlock);
                }
            }
            observations.add(obs);
        }
        return observations;
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests(Map<String, String> hints) throws DataStoreException {
        final String version                 = getVersionFromHints(hints);
        final List<SamplingFeature> features = new ArrayList<>();
        final Set<String> fid = filterFeatureOfInterest();
        for (String foid : fid) {
            final SamplingFeature feature = reader.getFeatureOfInterest(foid, version);
            features.add(feature);
        }
        return features;
    }

    @Override
    public List<Phenomenon> getPhenomenons(Map<String, String> hints) throws DataStoreException {
        final String version               = getVersionFromHints(hints);
        final List<Phenomenon> phenomenons = new ArrayList<>();
        final Set<String> fid = filterPhenomenon();
        for (String foid : fid) {
            final Phenomenon phen = reader.getPhenomenon(foid, version);
            phenomenons.add(phen);
        }
        return phenomenons;
    }

    @Override
    public List<Process> getProcesses(Map<String, String> hints) throws DataStoreException {
        final String version          = getVersionFromHints(hints);
        final List<Process> processes = new ArrayList<>();
        final Set<String> pids = filterProcedure();
        for (String pid : pids) {
            final Process pr = reader.getProcess(pid, version);
            processes.add(pr);
        }
        return processes;
    }

    @Override
    public String getResults() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getDecimatedResults(int width) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getOutOfBandResults() throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    @Override
    public boolean computeCollectionBound() {
        return false;
    }

    @Override
    public Envelope getCollectionBoundingShape() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getVersionFromHints(Map<String, String> hints) {
        String version = "2.0.0";
        if (hints != null) {
            if (hints.containsKey("version")) {
                version = hints.get("version");
            }
        }
        return version;
    }

    @Override
    public Map<String, Map<Date, Geometry>> getSensorLocations(Map<String, String> hints) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map<String, List<Date>> getSensorTimes(Map<String, String> hints) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
