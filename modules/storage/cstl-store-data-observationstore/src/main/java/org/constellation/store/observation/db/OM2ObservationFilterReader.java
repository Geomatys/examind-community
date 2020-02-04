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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.sql.DataSource;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import static org.constellation.api.CommonConstants.EVENT_TIME;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.RESPONSE_MODE;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.ObservationStoreException;
import static org.geotoolkit.observation.Utils.getTimeValue;
import org.geotoolkit.observation.xml.AbstractObservation;
import org.geotoolkit.observation.xml.OMXmlFactory;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.geotoolkit.sos.xml.SOSXmlFactory;

import static org.geotoolkit.sos.xml.SOSXmlFactory.*;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.DataArray;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.TextBlock;
import org.opengis.observation.CompositePhenomenon;
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

    public OM2ObservationFilterReader(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties) throws DataStoreException {
        super(source, isPostgres, schemaPrefix, properties);
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
            //sqlRequest.append("AND (\"time_begin\"='").append(position).append("' AND \"time_end\"='").append(position).append("') ");
            sqlMeasureRequest.append("AND (\"$time\"='").append(position).append("') ");
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
            sqlRequest.append("AND (\"time_begin\"<='").append(position).append("')");
            sqlMeasureRequest.append("AND (\"$time\"<='").append(position).append("')");
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
            sqlRequest.append("AND (\"time_end\">='").append(position).append("')");
            sqlMeasureRequest.append("AND (\"$time\">='").append(position).append("')");
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
            sqlRequest.append(" (\"time_begin\">='").append(begin).append("' AND \"time_end\"<='").append(end).append("')");
            sqlRequest.append("OR");
            // the single observations included in the period
            sqlRequest.append(" (\"time_begin\">='").append(begin).append("' AND \"time_begin\"<='").append(end).append("' AND \"time_end\" IS NULL)");
            sqlRequest.append("OR");
            // the multiple observations which overlaps the first bound
            sqlRequest.append(" (\"time_begin\"<='").append(begin).append("' AND \"time_end\"<='").append(end).append("' AND \"time_end\">='").append(begin).append("')");
            sqlRequest.append("OR");
            // the multiple observations which overlaps the second bound
            sqlRequest.append(" (\"time_begin\">='").append(begin).append("' AND \"time_end\">='").append(end).append("' AND \"time_begin\"<='").append(end).append("')");
            sqlRequest.append("OR");
            // the multiple observations which overlaps the whole period
            sqlRequest.append(" (\"time_begin\"<='").append(begin).append("' AND \"time_end\">='").append(end).append("'))");

            sqlMeasureRequest.append("AND (\"$time\">='").append(begin).append("' AND \"$time\"<= '").append(end).append("')");
            obsJoin = true;
        } else {
            throw new ObservationStoreException("TM_During operation require TimePeriod!",
                    INVALID_PARAMETER_VALUE, EVENT_TIME);
        }
    }

    @Override
    public List<Observation> getObservationTemplates(final Map<String,String> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        if (MEASUREMENT_QNAME.equals(resultModel)) {
            return getMesurementTemplates(version, hints);
        }
        String request = sqlRequest.toString();
        if (firstFilter) {
            request = request.replaceFirst("WHERE", "");
        }
        request = request + " ORDER BY \"procedure\" ";
        request = appendPaginationToRequest(request, hints);
        boolean includeTimeInTemplate = false;
        if (hints != null && hints.containsKey("includeTimeInTemplate")) {
            includeTimeInTemplate = Boolean.parseBoolean(hints.get("includeTimeInTemplate"));
        }

        try(final Connection c = source.getConnection()) {
            final List<Observation> observations = new ArrayList<>();
            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs               = currentStatement.executeQuery(request)) {
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
                    final String observedProperty = rs.getString("observed_property");
                    String featureID = null;
                    FeatureProperty foi = null;
                    if (includeFoiInTemplate) {
                        featureID = rs.getString("foi");
                        final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                        foi = buildFeatureProperty(version, feature);
                    }
                    TemporalGeometricPrimitive tempTime = null;
                    if (includeTimeInTemplate) {
                        tempTime = getTimeForTemplate(c, procedure, observedProperty, featureID, version);
                    }
                    final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                    List<Field> fields = readFields(procedure, c);
                    /*
                     *  BUILD RESULT
                     */
                    final List<AnyScalar> scal = new ArrayList<>();
                    for (Field f : fields) {
                        scal.add(f.getScalar(version));
                    }
                    final Object result = buildComplexResult(version, scal, 0, encoding, null, observations.size());
                    Observation observation = OMXmlFactory.buildObservation(version, obsID, name, null, foi, phen, procedure, result, tempTime);
                    observations.add(observation);
                }
            }
            return observations;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
    }

    private List<Observation> getMesurementTemplates(final String version, Map<String, String> hints) throws DataStoreException {
        String request = sqlRequest.toString();
        if (firstFilter) {
            request = request.replaceFirst("WHERE", "");
        }
        request = request + " ORDER BY \"procedure\" ";
        request = appendPaginationToRequest(request, hints);

        boolean includeTimeInTemplate = false;
        if (hints != null && hints.containsKey("includeTimeInTemplate")) {
            includeTimeInTemplate = Boolean.parseBoolean(hints.get("includeTimeInTemplate"));
        }

        try(final Connection c = source.getConnection()) {
            final List<Observation> observations = new ArrayList<>();

            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs               = currentStatement.executeQuery(request)) {

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
                    final String observedProperty = rs.getString("observed_property");
                    String featureID = null;
                    FeatureProperty foi = null;
                    if (includeFoiInTemplate) {
                        featureID = rs.getString("foi");
                        final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                        foi = buildFeatureProperty(version, feature);
                    }
                    TemporalGeometricPrimitive tempTime = null;
                    if (includeTimeInTemplate) {
                        tempTime = getTimeForTemplate(c, procedure, observedProperty, featureID, version);
                    }
                    final org.geotoolkit.swe.xml.Phenomenon phen = (org.geotoolkit.swe.xml.Phenomenon) getPhenomenon(version, observedProperty, c);
                    /*
                     *  BUILD RESULT
                     */
                    final List<Field> fields = readFields(procedure, c);
                    final Field timeField = getTimeField(procedure);
                    if (timeField != null) {
                        fields.remove(timeField);
                    }

                    // aggregate phenomenon mode
                    if (fields.size() > 1) {
                        if (phen instanceof CompositePhenomenon) {
                            CompositePhenomenon compoPhen = (CompositePhenomenon) phen;
                            if (compoPhen.getComponent().size() == fields.size()) {
                                for (int i = 0; i < fields.size(); i++) {
                                    org.geotoolkit.swe.xml.Phenomenon compPhen = (org.geotoolkit.swe.xml.Phenomenon) compoPhen.getComponent().get(i);
                                    if ((currentFields.isEmpty() || currentFields.contains(compPhen.getName().getCode())) &&
                                        (fieldFilters.isEmpty() || fieldFilters.contains(i))) {
                                        //final String cphenID = compPhen.getId();
                                        final Object result = buildMeasureResult(version, 0, fields.get(i).fieldUom, "1");
                                        observations.add(OMXmlFactory.buildMeasurement(version, obsID + '-' + i, name + '-' + i, null, foi, compPhen, procedure, result, tempTime));
                                    }
                                }
                            } else {
                                throw new DataStoreException("incoherence between multiple fields size and composite phenomenon components size");
                            }
                        } else {
                            throw new DataStoreException("incoherence between multiple fields and non-composite phenomenon");
                        }

                    // simple phenomenon mode
                    } else {
                        if (phen instanceof CompositePhenomenon) {
                            throw new DataStoreException("incoherence between single fields and composite phenomenon");
                        }
                        final Object result = buildMeasureResult(version, 0, fields.get(0).fieldUom, "1");
                        observations.add(OMXmlFactory.buildMeasurement(version, obsID + "-0", name + "-0", null, foi, phen, procedure, result, tempTime));
                    }
                }
            }
            return observations;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage());
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage());
        }
    }

    @Override
    public List<Observation> getObservations(final Map<String,String> hints) throws DataStoreException {
        boolean includeIDInDataBlock = false;
        String version = "2.0.0";
        if (hints != null) {
            if (hints.containsKey("version")) {
                version = hints.get("version");
            }
            if (hints.containsKey("includeIDInDataBlock")) {
                includeIDInDataBlock = Boolean.parseBoolean(hints.get("includeIDInDataBlock"));
            }
        }
        if (MEASUREMENT_QNAME.equals(resultModel)) {
            return getMesurements(version);
        }
        String request = sqlRequest.toString();
        if (firstFilter) {
            request = request.replaceFirst("WHERE", "");
        }

        try(final Connection c               = source.getConnection();
            final Statement currentStatement = c.createStatement()) {
            try(final ResultSet rs           = currentStatement.executeQuery(request)) {
                // add orderby to the query
                final Map<String, Observation> observations = new HashMap<>();
                final TextBlock encoding = getDefaultTextEncoding(version);
                final Map<String, List<Field>> fieldMap = new LinkedHashMap<>();

                while (rs.next()) {
                    int nbValue = 0;
                    StringBuilder values = new StringBuilder();
                    final String procedure = rs.getString("procedure");
                    final String featureID = rs.getString("foi");
                    final int oid = rs.getInt("id");
                    Observation observation = observations.get(procedure + '-' + featureID);
                    final int pid = getPIDFromProcedure(procedure, c);
                    final Field mainField = getMainField(procedure);

                    List<Field> fields = fieldMap.get(procedure);
                    if (fields == null) {
                        if (!currentFields.isEmpty()) {
                            fields = new ArrayList<>();
                            if (mainField != null) {
                                fields.add(mainField);
                            }
                            for (String f : currentFields) {
                                final Field field = getFieldForPhenomenon(procedure, f, c);
                                if (field != null && !fields.contains(field)) {
                                    fields.add(field);
                                }
                            }
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
                            fields.add(0, new Field("Text", "id", "measure identifier", null));
                        }
                        fieldMap.put(procedure, fields);
                    }

                    final String sqlRequest;
                    if (mainField != null) {
                        sqlRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                + "WHERE \"id_observation\" = ? " + sqlMeasureRequest.toString().replace("$time", mainField.fieldName)
                                + "ORDER BY m.\"id\"";
                    } else {
                        sqlRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ? ORDER BY m.\"id\"";
                    }

                    if (observation == null) {
                        final String obsID = "obs-" + oid;
                        final String timeID = "time-" + oid;
                        final String name = rs.getString("identifier");
                        final String observedProperty = rs.getString("observed_property");
                        final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                        final FeatureProperty prop = buildFeatureProperty(version, feature);
                        final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                        String firstTime = formatTime(rs.getString("time_begin"));
                        String lastTime = formatTime(rs.getString("time_end"));
                        boolean first = true;
                        final List<AnyScalar> scal = new ArrayList<>();
                        for (Field f : fields) {
                            scal.add(f.getScalar(version));
                        }

                        /*
                         *  BUILD RESULT
                         */
                        try(final PreparedStatement stmt = c.prepareStatement(sqlRequest)) {
                            stmt.setInt(1, oid);
                            try(final ResultSet rs2 = stmt.executeQuery()) {
                                while (rs2.next()) {
                                    StringBuilder line = new StringBuilder();
                                    boolean emptyLine = true;
                                    for (int i = 0; i < fields.size(); i++) {
                                        Field field = fields.get(i);
                                        String value;
                                        if (field.fieldType.equals("Time")) {
                                            Timestamp t = rs2.getTimestamp(field.fieldName);
                                            synchronized(format2) {
                                                value = format2.format(t);
                                            }
                                            line.append(value).append(encoding.getTokenSeparator());
                                            if (first) {
                                                firstTime = value;
                                                first = false;
                                            }
                                            lastTime = value;
                                        } else if (field.fieldType.equals("Quantity")){
                                            value = rs2.getString(field.fieldName); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                            if (value != null && !value.isEmpty()) {
                                                value = Double.toString(rs2.getDouble(field.fieldName));
                                                emptyLine = false;
                                                line.append(value);
                                            }
                                            line.append(encoding.getTokenSeparator());
                                        } else {
                                            value = rs2.getString(field.fieldName);
                                            if (value != null && !value.isEmpty()) {
                                                emptyLine = false;
                                                line.append(value);
                                            }
                                            line.append(encoding.getTokenSeparator());
                                        }
                                    }
                                    if (!emptyLine) {
                                        values.append(line);
                                        // remove last token separator
                                        values.deleteCharAt(values.length() - 1);
                                        values.append(encoding.getBlockSeparator());
                                        nbValue++;
                                    }
                                }
                            }
                        }

                        final TemporalGeometricPrimitive time = buildTimePeriod(version, timeID, firstTime, lastTime);
                        final Object result = buildComplexResult(version, scal, nbValue, encoding, values.toString(), observations.size());
                        observation = OMXmlFactory.buildObservation(version, obsID, name, null, prop, phen, procedure, result, time);
                        observations.put(procedure + '-' + featureID, observation);
                    } else {
                        String lastTime = null;
                        try(final PreparedStatement stmt = c.prepareStatement(sqlRequest)) {
                            stmt.setInt(1, oid);
                            try(final ResultSet rs2 = stmt.executeQuery()) {
                                while (rs2.next()) {
                                    StringBuilder line = new StringBuilder();
                                    boolean emptyLine = true;
                                    for (int i = 0; i < fields.size(); i++) {
                                        Field field = fields.get(i);
                                        String value;
                                        if (field.fieldType.equals("Time")) {
                                            Timestamp t = rs2.getTimestamp(field.fieldName);
                                            synchronized(format2) {
                                                value = format2.format(t);
                                            }
                                            lastTime = value;
                                            line.append(value).append(encoding.getTokenSeparator());
                                        } else if (field.fieldType.equals("Quantity")){
                                            value = rs2.getString(field.fieldName); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                            if (value != null && !value.isEmpty()) {
                                                value = Double.toString(rs2.getDouble(field.fieldName));
                                                emptyLine = false;
                                                line.append(value);
                                            }
                                            line.append(encoding.getTokenSeparator());
                                        } else {
                                            value = rs2.getString(field.fieldName);
                                            if (value != null && !value.isEmpty()) {
                                                emptyLine = false;
                                                line.append(value);
                                            }
                                            line.append(encoding.getTokenSeparator());
                                        }
                                    }
                                    if (!emptyLine) {
                                        values.append(line);
                                        // remove last token separator
                                        values.deleteCharAt(values.length() - 1);
                                        values.append(encoding.getBlockSeparator());
                                        nbValue++;
                                    }
                                }
                            }
                        }

                        // UPDATE RESULTS
                        final DataArrayProperty result = (DataArrayProperty) (observation).getResult();
                        final DataArray array = result.getDataArray();
                        array.setElementCount(array.getElementCount().getCount().getValue() + nbValue);
                        array.setValues(array.getValues() + values.toString());
                        ((AbstractObservation) observation).extendSamplingTime(lastTime);
                    }
                }
                return new ArrayList<>(observations.values());
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
    }

    private String formatTime(String s) {
        if (s != null) {
            s = s.replace(' ', 'T');
        }
        return s;
    }

    private DataArrayProperty buildComplexResult(final String version, final Collection<AnyScalar> fields, final int nbValue,
            final TextBlock encoding, final String values, final int cpt) {
        final String arrayID     = "dataArray-" + cpt;
        final String recordID    = "datarecord-" + cpt;
        final AbstractDataRecord record = buildSimpleDatarecord(version, null, recordID, null, false, new ArrayList<>(fields));
        return buildDataArrayProperty(version, arrayID, nbValue, arrayID, record, encoding, values);
    }

    public List<Observation> getMesurements(final String version) throws DataStoreException {
        // add orderby to the query
        String request = sqlRequest.append(" ORDER BY o.\"id\"").toString();
        if (firstFilter) {
            request = request.replaceFirst("WHERE", "");
        }

        try(final Connection c = source.getConnection()) {
            final List<Observation> observations = new ArrayList<>();

            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs               = currentStatement.executeQuery(request)) {
                while (rs.next()) {
                    final String procedure = rs.getString("procedure");
                    final Timestamp startTime = rs.getTimestamp("time_begin");
                    final Timestamp endTime = rs.getTimestamp("time_end");
                    final int oid = rs.getInt("id");
                    final String name = rs.getString("identifier");
                    final String obsID = "obs-" + oid;
                    final String timeID = "time-" + oid;
                    final String featureID = rs.getString("foi");
                    final String observedProperty = rs.getString("observed_property");
                    final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                    final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                    final int pid = getPIDFromProcedure(procedure, c);
                    final List<Field> fields = readFields(procedure, c);

                    String start = null;
                    if (startTime != null) {
                        start = format2.format(startTime);
                    }
                    String end = null;
                    if (endTime != null) {
                        end = format2.format(endTime);
                    }
                    TemporalGeometricPrimitive time = null;
                    if (start != null || end != null) {
                        time = buildTimePeriod(version, timeID, start, end);
                    } else if (start != null) {
                        time = buildTimeInstant(version, timeID, start);
                    }

                    /*
                     *  BUILD RESULT
                     */
                    final Field timeField = getTimeField(procedure);
                    final String sqlRequest;
                    if (timeField != null) {
                        sqlRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                + "WHERE \"id_observation\" = ? " + sqlMeasureRequest.toString().replace("$time", timeField.fieldName)
                                + "ORDER BY m.\"id\"";
                        fields.remove(timeField);
                    } else {
                        sqlRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ? ORDER BY m.\"id\"";
                    }

                    /**
                     * coherence verification
                     */
                    List<FieldPhenom> fieldPhen = getPhenomenonFields(phen, fields);

                    try(final PreparedStatement stmt = c.prepareStatement(sqlRequest)) {
                        stmt.setInt(1, oid);
                        try(final ResultSet rs2 = stmt.executeQuery()) {
                            while (rs2.next()) {
                                final Integer rid = rs2.getInt("id");
                                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                    TemporalGeometricPrimitive measureTime;
                                    if (timeField != null) {
                                        final Timestamp mt = rs2.getTimestamp(timeField.fieldName);
                                        String t = null;
                                        if (mt != null) {
                                            t = format2.format(mt);
                                        }
                                        measureTime = buildTimeInstant(version, "time-" + oid + '-' + rid, t);
                                    } else {
                                        measureTime = time;
                                    }

                                    for (int i = 0; i < fieldPhen.size(); i++) {
                                        FieldPhenom field = fieldPhen.get(i);
                                        Double dValue = null;
                                        final String value = rs2.getString(field.field.fieldName);
                                        if (value != null) {
                                            try {
                                                dValue = Double.parseDouble(value);
                                            } catch (NumberFormatException ex) {
                                                throw new DataStoreException("Unable ta parse the result value as a double (value=" + value + ")");
                                            }
                                            final FeatureProperty foi = buildFeatureProperty(version, feature); // do not share the same object
                                            final Object result = buildMeasureResult(version, dValue, field.field.fieldUom, Integer.toString(rid));
                                            observations.add(OMXmlFactory.buildMeasurement(version, obsID + '-' + field.i + '-' + rid, name + '-' + field.i + '-' + rid, null, foi, field.phenomenon, procedure, result, measureTime));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return observations;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request.toString());
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        } catch (DataStoreException ex) {
            throw new DataStoreException("the service has throw a Datastore Exception:" + ex.getMessage(), ex);
        }
    }

    private Object buildMeasureResult(final String version, final double value, final String uom, final String resultId) {
        final String name   = "measure-00" + resultId;
        return buildMeasure(version, name, uom, value);
    }

    @Override
    public String getResults() throws DataStoreException {
        String request = null;
        try {
            // add orderby to the query
            final Field timeField = getTimeField(currentProcedure);
            if (timeField != null) {
                sqlRequest.append(sqlMeasureRequest.toString().replace("$time", timeField.fieldName));
            }
            request = sqlRequest.append(" ORDER BY  o.\"id\", m.\"id\"").toString();

            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }

            final StringBuilder values = new StringBuilder();
            try(final Connection c = source.getConnection()) {
                try(final Statement currentStatement = c.createStatement()) {
                    LOGGER.info(request);
                    final ResultSet rs = currentStatement.executeQuery(request);
                    final List<Field> fields;
                    if (!currentFields.isEmpty()) {
                        fields = new ArrayList<>();
                        final Field mainField = getMainField(currentProcedure, c);
                        if (mainField != null) {
                            fields.add(mainField);
                        }
                        for (String f : currentFields) {
                            final Field field = getFieldForPhenomenon(currentProcedure, f, c);
                            if (field != null && !fields.contains(field)) {
                                fields.add(field);
                            }
                        }
                    } else {
                        fields = readFields(currentProcedure, c);
                    }
                    final TextBlock encoding;
                    if ("text/csv".equals(responseFormat)) {
                        encoding = getCsvTextEncoding("2.0.0");
                        // Add the header
                        for (Field pheno : fields) {
                            values.append(pheno.fieldDesc).append(',');
                        }
                        values.setCharAt(values.length() - 1, '\n');
                    } else {
                        encoding = getDefaultTextEncoding("2.0.0");
                    }

                    while (rs.next()) {
                        StringBuilder line = new StringBuilder();
                        boolean emptyLine = true;
                        for (int i = 0; i < fields.size(); i++) {
                            Field field = fields.get(i);
                            String value;
                            if (field.fieldType.equals("Time")) {
                                Timestamp t = rs.getTimestamp(field.fieldName);
                                synchronized(format2) {
                                    value = format2.format(t);
                                }
                                line.append(value).append(encoding.getTokenSeparator());
                            } else if (field.fieldType.equals("Quantity")){
                                value = rs.getString(field.fieldName); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                if (value != null && !value.isEmpty()) {
                                    value = Double.toString(rs.getDouble(field.fieldName));
                                    emptyLine = false;
                                    line.append(value);
                                }
                                line.append(encoding.getTokenSeparator());
                            } else {
                                value = rs.getString(field.fieldName);
                                if (value != null && !value.isEmpty()) {
                                    emptyLine = false;
                                    line.append(value);
                                }
                                line.append(encoding.getTokenSeparator());
                            }
                        }
                        if (!emptyLine) {
                            values.append(line);
                            // remove last token separator
                            values.deleteCharAt(values.length() - 1);
                            values.append(encoding.getBlockSeparator());
                        }
                    }
                }
            }
            return values.toString();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    @Override
    public String getDecimatedResults(final int width) throws DataStoreException {
        try {
            // add orderby to the query
            final String fieldRequest = sqlRequest.toString();
            sqlRequest.append(" ORDER BY  o.\"id\", m.\"id\"");
            final StringBuilder values = new StringBuilder();
            try(final Connection c = source.getConnection()) {
                try (final Statement currentStatement = c.createStatement()) {
                    LOGGER.info(sqlRequest.toString());
                    try (final ResultSet rs = currentStatement.executeQuery(sqlRequest.toString())) {
                        final TextBlock encoding;
                        final List<Field> fields;
                        if (!currentFields.isEmpty()) {
                            fields = new ArrayList<>();
                            final Field mainField = getMainField(currentProcedure, c);
                            if (mainField != null) {
                                fields.add(mainField);
                            }
                            for (String f : currentFields) {
                                final Field field = getFieldForPhenomenon(currentProcedure, f, c);
                                if (field != null && !fields.contains(field)) {
                                    fields.add(field);
                                }
                            }
                        } else {
                            fields = readFields(currentProcedure, c);
                        }
                        if ("text/csv".equals(responseFormat)) {
                            encoding = getCsvTextEncoding("2.0.0");
                            // Add the header
                            for (Field pheno : fields) {
                                // hack for the current graph in cstl you only work when the main field is named "time"
                                if ("Time".equals(pheno.fieldType)) {
                                    values.append("time").append(',');
                                } else {
                                    values.append(pheno.fieldDesc).append(',');
                                }
                            }
                            values.setCharAt(values.length() - 1, '\n');
                        } else {
                            encoding = getDefaultTextEncoding("2.0.0");
                        }
                        Map<String, Double> minVal = initMapVal(fields, false);
                        Map<String, Double> maxVal = initMapVal(fields, true);
                        final long[] times = getMainFieldStepForGetResult(fieldRequest, fields.get(0), c, width);
                        final long step = times[1];
                        long start = times[0];

                        while (rs.next()) {

                            long currentMainValue = -1;
                            for (int i = 0; i < fields.size(); i++) {
                                Field field = fields.get(i);
                                String value = rs.getString(field.fieldName);

                                if (i == 0) {
                                    if (field.fieldType.equals("Time")) {
                                        final Timestamp currentTime = Timestamp.valueOf(value);
                                        currentMainValue = currentTime.getTime();
                                    } else if (field.fieldType.equals("Quantity")) {
                                        if (value != null && !value.isEmpty()) {
                                            final Double d = Double.parseDouble(value);
                                            currentMainValue = d.longValue();
                                        }
                                    }
                                }
                                addToMapVal(minVal, maxVal, field.fieldName, value);
                            }


                            if (currentMainValue != -1 && currentMainValue > (start + step)) {
                                //min
                                if (fields.get(0).fieldType.equals("Time")) {
                                    values.append(format.format(new Date(start)));
                                } else if (fields.get(0).fieldType.equals("Quantity")) {
                                    values.append(start);
                                } else {
                                    throw new DataStoreException("main field other than Time or Quantity are not yet allowed");
                                }
                                for (Field field : fields) {
                                    if (!field.equals(fields.get(0))) {
                                        values.append(encoding.getTokenSeparator());
                                        final double minValue = minVal.get(field.fieldName);
                                        if (minValue != Double.MAX_VALUE) {
                                            values.append(minValue);
                                        }
                                    }
                                }
                                values.append(encoding.getBlockSeparator());
                                //max
                                if (fields.get(0).fieldType.equals("Time")) {
                                    long maxTime = start + step;
                                    values.append(format.format(new Date(maxTime)));
                                } else if (fields.get(0).fieldType.equals("Quantity")) {
                                    values.append(start + step);
                                } else {
                                    throw new DataStoreException("main field other than Time or Quantity are not yet allowed");
                                }
                                for (Field field : fields) {
                                    if (!field.equals(fields.get(0))) {
                                        values.append(encoding.getTokenSeparator());
                                        final double maxValue = maxVal.get(field.fieldName);
                                        if (maxValue != -Double.MAX_VALUE) {
                                            values.append(maxValue);
                                        }
                                    }
                                }
                                values.append(encoding.getBlockSeparator());
                                start = currentMainValue;
                                minVal = initMapVal(fields, false);
                                maxVal = initMapVal(fields, true);
                            }
                        }
                    }
                }
            }
            return values.toString();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", sqlRequest.toString());
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    private Map<String, Double> initMapVal(final List<Field> fields, final boolean max) {
        final Map<String, Double> result = new HashMap<>();
        final double value;
        if (max) {
            value = -Double.MAX_VALUE;
        } else {
            value = Double.MAX_VALUE;
        }
        for (Field field : fields) {
            result.put(field.fieldName, value);
        }
        return result;
    }

    private void addToMapVal(final Map<String, Double> minMap, final Map<String, Double> maxMap, final String field, final String value) {
        if (value == null || value.isEmpty()) return;

        final Double minPrevious = minMap.get(field);
        final Double maxPrevious = maxMap.get(field);
        try {
            final Double current = Double.parseDouble(value);
            if (current > maxPrevious) {
                maxMap.put(field, current);
            }
            if (current < minPrevious) {
                minMap.put(field, current);
            }
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.FINER, "unable to parse value:{0}", value);
        }
    }

    private long[] getMainFieldStepForGetResult(String request, final Field mainField, final Connection c, final int width) throws SQLException {
        request = request.replace("SELECT m.*", "SELECT MIN(\"" + mainField.fieldName + "\"), MAX(\"" + mainField.fieldName + "\") ");
        try(final Statement stmt = c.createStatement();
            final ResultSet rs = stmt.executeQuery(request)) {
            final long[] result = {-1L, -1L};
            if (rs.next()) {
                if (mainField.fieldType.equals("Time")) {
                    final Timestamp minT = rs.getTimestamp(1);
                    final Timestamp maxT = rs.getTimestamp(2);
                    if (minT != null && maxT != null) {
                        final long min = minT.getTime();
                        final long max = maxT.getTime();
                        result[0] = min;
                        result[1] = (max - min) / width;
                    }
                } else if (mainField.fieldType.equals("Quantity")) {
                    final Double minT = rs.getDouble(1);
                    final Double maxT = rs.getDouble(2);
                    if (minT != null && maxT != null) {
                        final long min = minT.longValue();
                        final long max = maxT.longValue();
                        result[0] = min;
                        result[1] = (max - min) / width;
                    }
                } else {
                    throw new SQLException("unable to extract bound from a " + mainField.fieldType + " main field.");
                }
            }
            return result;
        }
    }

    @Override
    public List<SamplingFeature> getFeatureOfInterests(final Map<String,String> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"foi\" = sf.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (offJoin) {
            final String offJoin = ", \"" + schemaPrefix + "om\".\"offering_foi\" off WHERE off.\"foi\" = sf.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", offJoin);
            } else {
                request = request.replaceFirst("WHERE", offJoin + "AND ");
            }
        } else {
            request = request.replace("\"foi\"='", "sf.\"id\"='");
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        request = appendPaginationToRequest(request, hints);

        try(final Connection c = source.getConnection()) {
            final List<SamplingFeature> features = new ArrayList<>();
            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs = currentStatement.executeQuery(request)) {
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
                    final Geometry geom;
                    if (b != null) {
                        WKBReader reader = new WKBReader();
                        geom = reader.read(b);
                    } else {
                        geom = null;
                    }
                    features.add(buildFoi(version, id, name, desc, sf, geom, crs));
                }
            }
            return features;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }catch (FactoryException ex) {
            LOGGER.log(Level.SEVERE, "FactoryException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a Factory Exception:" + ex.getMessage(), ex);
        }catch (ParseException ex) {
            LOGGER.log(Level.SEVERE, "ParseException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a Parse Exception:" + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Phenomenon> getPhenomenons(final Map<String,String> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"observed_property\" = op.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (offJoin) {
            final String offJoin = ", \"" + schemaPrefix + "om\".\"offering_observed_properties\" off WHERE off.\"phenomenon\" = op.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", offJoin);
            } else {
                request = request.replaceFirst("WHERE", offJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        request = appendPaginationToRequest(request, hints);

        try(final Connection c = source.getConnection()) {
            final List<Phenomenon> phenomenons = new ArrayList<>();
            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs = currentStatement.executeQuery(request)) {
                while (rs.next()) {
                    Phenomenon phen = getPhenomenon(version, rs.getString(1), c);
                    if (phen instanceof CompositePhenomenon && !fieldFilters.isEmpty()) {
                        CompositePhenomenon compos = (CompositePhenomenon) phen;
                        for (int i = 0; i< compos.getComponent().size(); i++) {
                            if (fieldFilters.contains(i)) {
                                if (!phenomenons.contains(phen)) {
                                    phenomenons.add(compos.getComponent().get(i));
                                }
                            }
                        }
                    } else {
                        if (!phenomenons.contains(phen)) {
                            phenomenons.add(phen);
                        }
                    }
                }
            }
            return phenomenons;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Process> getProcesses(final Map<String,String> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        String request = sqlRequest.toString();
        if (obsJoin) {
            final String obsJoin = ", \"" + schemaPrefix + "om\".\"observations\" o WHERE o.\"procedure\" = pr.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", obsJoin);
            } else {
                request = request.replaceFirst("WHERE", obsJoin + "AND ");
            }
        } else if (offJoin) {
            final String offJoin = ", \"" + schemaPrefix + "om\".\"offerings\" off WHERE off.\"procedure\" = pr.\"id\" ";
            if (firstFilter) {
                request = request.replaceFirst("WHERE", offJoin);
            } else {
                request = request.replaceFirst("WHERE", offJoin + "AND ");
            }
        } else {
            if (firstFilter) {
                request = request.replaceFirst("WHERE", "");
            }
        }
        request = appendPaginationToRequest(request, hints);
        try(final Connection c = source.getConnection()) {
            final List<Process> processes = new ArrayList<>();
            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs = currentStatement.executeQuery(request)) {
                while (rs.next()) {
                    processes.add(SOSXmlFactory.buildProcess(version, rs.getString(1)));
                }
            }
            return processes;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    private String appendPaginationToRequest(String request, Map<String, String> hints) {
        Long limit     = null;
        Long offset    = null;
        if (hints != null) {
            if (hints.containsKey("limit")) {
                limit = Long.parseLong(hints.get("limit"));
            }
            if (hints.containsKey("offset")) {
                offset = Long.parseLong(hints.get("offset"));
            }
        }
        if (isPostgres) {
            if (limit != null) {
                request = request + " LIMIT " + limit;
            }
            if (offset != null) {
                request = request + " OFFSET " + offset;
            }
        } else {
            if (offset != null) {
                request = request + " OFFSET " + offset + " ROWS ";
            }
            if (limit != null) {
                request = request + " fetch next " + limit +" rows only";
            }
        }
        return request;
    }

    @Override
    public String getOutOfBandResults() throws DataStoreException {
        throw new ObservationStoreException("Out of band response mode has not been implemented yet", NO_APPLICABLE_CODE, RESPONSE_MODE);
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
}
