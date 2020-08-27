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
import static org.constellation.store.observation.db.OM2BaseReader.defaultCRS;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
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
import org.opengis.filter.expression.Literal;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TEquals;
import org.opengis.geometry.Geometry;
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
    public void setTimeFilter(final BinaryTemporalOperator tFilter) throws DataStoreException {
        // we get the property name (not used for now)
        // String propertyName = tFilter.getExpression1()
        Object time = tFilter.getExpression2();
            
        if (tFilter instanceof TEquals) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            if (time instanceof Period) {
                final Period tp    = (Period) time;
                final String begin = getTimeValue(tp.getBeginning().getDate());
                final String end   = getTimeValue(tp.getEnding().getDate());

                // we request directly a multiple observation or a period observation (one measure during a period)
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                sqlRequest.append(" \"time_begin\"='").append(begin).append("' AND ");
                sqlRequest.append(" \"time_end\"='").append(end).append("') ");
                obsJoin = true;
                firstFilter = false;
            // if the temporal object is a timeInstant
            } else if (time instanceof Instant) {
                final Instant ti      = (Instant) time;
                final String position = getTimeValue(ti.getDate());
                //sqlRequest.append("AND (\"time_begin\"='").append(position).append("' AND \"time_end\"='").append(position).append("') ");
                if (getLoc) {
                    if (firstFilter) {
                        sqlRequest.append(" ( ");
                    } else {
                        sqlRequest.append("AND ( ");
                    }
                    sqlRequest.append(" \"time\"='").append(position).append("') ");
                } else {
                    sqlMeasureRequest.append(" AND ( \"$time\"='").append(position).append("') ");
                    obsJoin = true;
                }
                firstFilter = false;
            } else {
                throw new ObservationStoreException("TM_Equals operation require timeInstant or TimePeriod!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
        } else if (tFilter instanceof Before) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            // for the operation before the temporal object must be an timeInstant
            if (time instanceof Instant) {
                final Instant ti      = (Instant) time;
                final String position = getTimeValue(ti.getDate());
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                if (getLoc) {
                    sqlRequest.append(" \"time\"<='").append(position).append("')");
                } else {
                    sqlRequest.append(" \"time_begin\"<='").append(position).append("')");
                    sqlMeasureRequest.append(" AND ( \"$time\"<='").append(position).append("')");
                    obsJoin = true;
                }
                firstFilter = false;
            } else {
                throw new ObservationStoreException("TM_Before operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
            
        } else if (tFilter instanceof After) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            // for the operation after the temporal object must be an timeInstant
            if (time instanceof Instant) {
                final Instant ti      = (Instant) time;
                final String position = getTimeValue(ti.getDate());
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }
                if (getLoc) {
                    sqlRequest.append(" \"time\">='").append(position).append("')");
                } else {
                    sqlRequest.append(" \"time_end\">='").append(position).append("')");
                    sqlMeasureRequest.append(" AND (\"$time\">='").append(position).append("')");
                    obsJoin = true;
                }
                firstFilter = false;
            } else {
                throw new ObservationStoreException("TM_After operation require timeInstant!",
                        INVALID_PARAMETER_VALUE, EVENT_TIME);
            }
            
        } else if (tFilter instanceof During) {
            if (time instanceof Literal && !(time instanceof TemporalGeometricPrimitive)) {
                time = ((Literal)time).getValue();
            }
            if (time instanceof Period) {
                final Period tp    = (Period) time;
                final String begin = getTimeValue(tp.getBeginning().getDate());
                final String end   = getTimeValue(tp.getEnding().getDate());
                if (firstFilter) {
                    sqlRequest.append(" ( ");
                } else {
                    sqlRequest.append("AND ( ");
                }

                if (getLoc) {
                    sqlRequest.append(" \"time\">='").append(begin).append("' AND \"time\"<='").append(end).append("')");
                } else {
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

                    sqlMeasureRequest.append(" AND ( \"$time\">='").append(begin).append("' AND \"$time\"<= '").append(end).append("')");

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
                    final Field mainField = getMainField(procedure);
                    if (mainField != null && "Time".equals(mainField.fieldType)) {
                        fields.remove(mainField);
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
                                        final Object result = buildMeasure(version, "measure-001", fields.get(i).fieldUom, 0d);
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
                        final Object result = buildMeasure(version, "measure-001", fields.get(0).fieldUom, 0d);
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
        boolean includeIDInDataBlock  = false;
        boolean includeTimeForProfile = false;
        boolean directResultArray     = false;
        String version = "2.0.0";
        if (hints != null) {
            if (hints.containsKey("version")) {
                version = hints.get("version");
            }
            if (hints.containsKey("includeIDInDataBlock")) {
                includeIDInDataBlock = Boolean.parseBoolean(hints.get("includeIDInDataBlock"));
            }
            if (hints.containsKey("includeTimeForProfile")) {
                includeTimeForProfile = Boolean.parseBoolean(hints.get("includeTimeForProfile"));
            }
            if (hints.containsKey("directResultArray")) {
                directResultArray = Boolean.parseBoolean(hints.get("directResultArray"));
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
                    ResultBuilder values = new ResultBuilder(directResultArray, encoding);
                    final String procedure = rs.getString("procedure");
                    final String featureID = rs.getString("foi");
                    final int oid = rs.getInt("id");
                    Observation observation = observations.get(procedure + '-' + featureID);
                    final int pid = getPIDFromProcedure(procedure, c);
                    final Field mainField = getMainField(procedure);
                    boolean isTimeField   = false;
                    if (mainField != null) {
                        isTimeField = "Time".equals(mainField.fieldType);
                    }
                    List<Field> fields = fieldMap.get(procedure);
                    if (fields == null) {
                        if (!currentFields.isEmpty()) {
                            fields = new ArrayList<>();
                            if (mainField != null) {
                                fields.add(mainField);
                            }

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

                        // add the result id in the dataBlock if requested
                        if (includeIDInDataBlock) {
                            fields.add(0, new Field("Text", "id", "measure identifier", null));
                        }
                        fieldMap.put(procedure, fields);
                    }

                    final String measureRequest;
                    if (isTimeField) {
                        measureRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                + "WHERE \"id_observation\" = ? " + sqlMeasureRequest.toString().replace("$time", mainField.fieldName)
                                + "ORDER BY m.\"id\"";
                    } else {
                        measureRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ? ORDER BY m.\"id\"";
                    }
                    int timeForProfileIndex = -1;
                    if (includeTimeForProfile && !isTimeField) {
                        timeForProfileIndex = includeIDInDataBlock ? 1 : 0;
                    }

                    Date firstTime = null;
                    if (observation == null) {
                        final String obsID = "obs-" + oid;
                        final String timeID = "time-" + oid;
                        final String name = rs.getString("identifier");
                        final String observedProperty = rs.getString("observed_property");
                        final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                        final FeatureProperty prop = buildFeatureProperty(version, feature);
                        final Phenomenon phen = getPhenomenon(version, observedProperty, c);
                        firstTime = dateFromTS(rs.getTimestamp("time_begin"));
                        Date lastTime = dateFromTS(rs.getTimestamp("time_end"));
                        boolean first = true;
                        final List<AnyScalar> scal = new ArrayList<>();
                        for (Field f : fields) {
                            scal.add(f.getScalar(version));
                        }

                        // add the time field in the dataBlock if requested (only if main field is not a time field)
                        if (timeForProfileIndex != -1) {
                            scal.add(timeForProfileIndex, new Field("Time", "time", "http://www.opengis.net/def/property/OGC/0/SamplingTime", null).getScalar(version));
                        }

                        /*
                         *  BUILD RESULT
                         */
                        try(final PreparedStatement stmt = c.prepareStatement(measureRequest)) {
                            stmt.setInt(1, oid);
                            try(final ResultSet rs2 = stmt.executeQuery()) {
                                while (rs2.next()) {
                                    values.newBlock();
                                    for (int i = 0; i < fields.size(); i++) {

                                        if (i == timeForProfileIndex) {
                                            values.appendTime(firstTime);
                                        }

                                        Field field = fields.get(i);
                                        switch (field.fieldType) {
                                            case "Time":
                                                Date t = dateFromTS(rs2.getTimestamp(field.fieldName));
                                                values.appendTime(t);
                                                if (first) {
                                                    firstTime = t;
                                                    first = false;
                                                }   lastTime = t;
                                                break;
                                            case "Quantity":
                                                String value = rs2.getString(field.fieldName); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                                Double d = Double.NaN;
                                                if (value != null && !value.isEmpty()) {
                                                    d = rs2.getDouble(field.fieldName);
                                                }
                                                values.appendDouble(d);
                                                break;
                                            default:
                                                values.appendString(rs2.getString(field.fieldName));
                                                break;
                                        }
                                    }
                                    nbValue = nbValue + values.endBlock();
                                }
                            }
                        }

                        final TemporalGeometricPrimitive time = buildTimePeriod(version, timeID, firstTime, lastTime);
                        final Object result = buildComplexResult(version, scal, nbValue, encoding, values, observations.size());
                        observation = OMXmlFactory.buildObservation(version, obsID, name, null, prop, phen, procedure, result, time);
                        observations.put(procedure + '-' + featureID, observation);
                    } else {
                        Date lastTime = null;
                        try(final PreparedStatement stmt = c.prepareStatement(measureRequest)) {
                            stmt.setInt(1, oid);
                            try(final ResultSet rs2 = stmt.executeQuery()) {
                                while (rs2.next()) {
                                    values.newBlock();
                                    for (int i = 0; i < fields.size(); i++) {

                                        if (i == timeForProfileIndex) {
                                            values.appendTime(firstTime);
                                        }

                                        Field field = fields.get(i);
                                        switch (field.fieldType) {
                                            case "Time":
                                                Date t = dateFromTS(rs2.getTimestamp(field.fieldName));
                                                values.appendTime(t);
                                                lastTime = t;
                                                break;
                                            case "Quantity":
                                                String value = rs2.getString(field.fieldName); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                                Double d = Double.NaN;
                                                if (value != null && !value.isEmpty()) {
                                                    d = rs2.getDouble(field.fieldName);
                                                }
                                                values.appendDouble(d);
                                                break;
                                            default:
                                                values.appendString(rs2.getString(field.fieldName));
                                                break;
                                        }
                                    }
                                    nbValue = nbValue + values.endBlock();
                                }
                            }
                        }

                        // UPDATE RESULTS
                        final DataArrayProperty result = (DataArrayProperty) (observation).getResult();
                        final DataArray array = result.getDataArray();
                        array.setElementCount(array.getElementCount().getCount().getValue() + nbValue);
                        if (directResultArray) {
                            array.getDataValues().getAny().addAll(values.getDataArray());
                        } else {
                            array.setValues(array.getValues() + values.getStringValues());
                        }
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

    private Date dateFromTS(Timestamp t) {
        if (t != null) {
            return new Date(t.getTime());
        }
        return null;
    }

    private DataArrayProperty buildComplexResult(final String version, final Collection<AnyScalar> fields, final int nbValue,
            final TextBlock encoding, final ResultBuilder values, final int cpt) {
        final String arrayID     = "dataArray-" + cpt;
        final String recordID    = "datarecord-" + cpt;
        final AbstractDataRecord record = buildSimpleDatarecord(version, null, recordID, null, false, new ArrayList<>(fields));
        String stringValues = null;
        List<Object> dataValues = null;
        if (values != null) {
            stringValues = values.getStringValues();
            dataValues   = values.getDataArray();
        }
        return buildDataArrayProperty(version, arrayID, nbValue, arrayID, record, encoding, stringValues, dataValues);
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
                    final Date startTime = dateFromTS(rs.getTimestamp("time_begin"));
                    final Date endTime = dateFromTS(rs.getTimestamp("time_end"));
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
                        synchronized (format2) {
                            start = format2.format(startTime);
                        }
                    }
                    String end = null;
                    if (endTime != null) {
                        synchronized (format2) {
                            end = format2.format(endTime);
                        }
                    }
                    TemporalGeometricPrimitive time = null;
                    if (start != null && end == null) {
                        time = buildTimeInstant(version, timeID, start);
                    } else if (start != null || end != null) {
                        time = buildTimePeriod(version, timeID, start, end);
                    }

                    /*
                     *  BUILD RESULT
                     */
                    final Field mainField = getMainField(procedure);
                    boolean isTimeField = false;
                    final String measureRequest;
                    if (mainField != null) {
                        isTimeField = "Time".equals(mainField.fieldType);
                        if (isTimeField) {
                            fields.remove(mainField);
                        }
                    }

                    if (isTimeField) {
                        measureRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m "
                                + "WHERE \"id_observation\" = ? " + sqlMeasureRequest.toString().replace("$time", mainField.fieldName)
                                + "ORDER BY m.\"id\"";
                    } else {
                        measureRequest = "SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m WHERE \"id_observation\" = ? ORDER BY m.\"id\"";
                    }

                    /**
                     * coherence verification
                     */
                    List<FieldPhenom> fieldPhen = getPhenomenonFields(phen, fields);

                    try(final PreparedStatement stmt = c.prepareStatement(measureRequest)) {
                        stmt.setInt(1, oid);
                        try(final ResultSet rs2 = stmt.executeQuery()) {
                            while (rs2.next()) {
                                final Integer rid = rs2.getInt("id");
                                if (measureIdFilters.isEmpty() || measureIdFilters.contains(rid)) {
                                    TemporalGeometricPrimitive measureTime;
                                    if (isTimeField) {
                                        final Date mt = dateFromTS(rs2.getTimestamp(mainField.fieldName));
                                        measureTime = buildTimeInstant(version, "time-" + oid + '-' + rid, mt);
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
                                            final Object result = buildMeasure(version, "measure-00" + rid, field.field.fieldUom, dValue);
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

    @Override
    public String getResults(final Map<String,String> hints) throws DataStoreException {
        Integer decimationSize = null;
        if (hints.containsKey("decimSize")) {
            decimationSize = Integer.parseInt(hints.get("decimSize"));
        }
        if (decimationSize != null) {
            return getDecimatedResults(decimationSize);
        }
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
            LOGGER.info(request);

            final StringBuilder values = new StringBuilder();
            try(final Connection c = source.getConnection();
                final Statement currentStatement = c.createStatement();
                final ResultSet rs = currentStatement.executeQuery(request)) {

                final List<Field> fields;
                if (!currentFields.isEmpty()) {
                    fields = new ArrayList<>();
                    final Field mainField = getMainField(currentProcedure, c);
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
                    List<Field> procedureFields = readFields(currentProcedure, c);
                    phenFields = reOrderFields(procedureFields, phenFields);
                    fields.addAll(phenFields);

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
                        switch (field.fieldType) {
                            case "Time":
                                Date t = dateFromTS(rs.getTimestamp(field.fieldName));
                                synchronized(format2) {
                                    value = format2.format(t);
                                }   line.append(value).append(encoding.getTokenSeparator());
                                break;
                            case "Quantity":
                                value = rs.getString(field.fieldName); // we need to kown if the value is null (rs.getDouble return 0 if so).
                                if (value != null && !value.isEmpty()) {
                                    value = Double.toString(rs.getDouble(field.fieldName));
                                    emptyLine = false;
                                    line.append(value);
                                }   line.append(encoding.getTokenSeparator());
                                break;
                            default:
                                value = rs.getString(field.fieldName);
                                if (value != null && !value.isEmpty()) {
                                    emptyLine = false;
                                    line.append(value);
                                }   line.append(encoding.getTokenSeparator());
                                break;
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
            return values.toString();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    private String getDecimatedResults(final int width) throws DataStoreException {
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
                            List<Field> phenFields = new ArrayList<>();
                            for (String f : currentFields) {
                                final Field field = getFieldForPhenomenon(currentProcedure, f, c);
                                if (field != null && !fields.contains(field)) {
                                    phenFields.add(field);
                                }
                            }
                             // add proper order to fields
                            List<Field> procedureFields = readFields(currentProcedure, c);
                            phenFields = reOrderFields(procedureFields, phenFields);
                            fields.addAll(phenFields);
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
                    final long min    = minT.longValue();
                    final long max    = maxT.longValue();
                    result[0] = min;
                    result[1] = (max - min) / width;

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
                    final org.locationtech.jts.geom.Geometry geom;
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

    @Override
    public Map<String, Map<Date, Geometry>> getSensorLocations(final Map<String,String> hints) throws DataStoreException {
        final String version = getVersionFromHints(hints);
        String request = sqlRequest.toString();
        if (firstFilter) {
            request = request.replaceFirst("WHERE", "");
        }

        request = request + " ORDER BY \"procedure\", \"time\"";
        request = appendPaginationToRequest(request, hints);
        try(final Connection c = source.getConnection()) {
            Map<String, Map<Date, Geometry>> locations = new LinkedHashMap<>();
            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs = currentStatement.executeQuery(request)) {
                while (rs.next()) {
                    try {
                        final String procedure = rs.getString("procedure");
                        final Date time = new Date(rs.getTimestamp("time").getTime());
                        final byte[] b = rs.getBytes(3);
                        final int srid = rs.getInt(4);
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

                        final String gmlVersion = getGMLVersion(version);
                        final AbstractGeometry gmlGeom = JTStoGeometry.toGML(gmlVersion, geom, crs);

                        final Map<Date, Geometry> procedureLocations;
                        if (locations.containsKey(procedure)) {
                            procedureLocations = locations.get(procedure);
                        } else {
                            procedureLocations = new LinkedHashMap<>();
                            locations.put(procedure, procedureLocations);

                        }
                        if (gmlGeom instanceof Geometry) {
                            procedureLocations.put(time, (Geometry) gmlGeom);
                        } else {
                            throw new DataStoreException("GML geometry cannot be casted as an Opengis one");
                        }
                    } catch (FactoryException | ParseException ex) {
                        throw new DataStoreException(ex);
                    }
                }
            }
            return locations;
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQLException while executing the query: {0}", request);
            throw new DataStoreException("the service has throw a SQL Exception:" + ex.getMessage(), ex);
        }
    }

    @Override
    public Map<String, List<Date>> getSensorTimes(final Map<String,String> hints) throws DataStoreException {
        String request = sqlRequest.toString();
        if (firstFilter) {
            request = request.replaceFirst("WHERE", "");
        }

        request = request + " ORDER BY \"procedure\", \"time\"";
        request = appendPaginationToRequest(request, hints);
        try(final Connection c = source.getConnection()) {
            Map<String, List<Date>> times = new LinkedHashMap<>();
            try(final Statement currentStatement = c.createStatement();
                final ResultSet rs = currentStatement.executeQuery(request)) {
                while (rs.next()) {
                    final String procedure = rs.getString("procedure");
                    final Date time = new Date(rs.getTimestamp("time").getTime());

                    final List<Date> procedureTimes;
                    if (times.containsKey(procedure)) {
                        procedureTimes = times.get(procedure);
                    } else {
                        procedureTimes = new ArrayList<>();
                        times.put(procedure, procedureTimes);
                    }
                    procedureTimes.add(time);
                }
            }
            return times;
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

    private static List<Field> reOrderFields(List<Field> procedureFields, List<Field> subset) {
        List<Field> result = new ArrayList();
        for (Field pField : procedureFields) {
            if (subset.contains(pField)) {
                result.add(pField);
            }
        }
        return result;
    }

    private static class ResultBuilder {
        private final boolean dra;
        private boolean emptyLine;

        private StringBuilder values;
        private StringBuilder currentLine;
        private final TextBlock encoding;

        private List<Object> dataArray;
        private List<Object> currentArrayLine;


        public ResultBuilder(boolean directResultArray, final TextBlock encoding) {
            this.dra = directResultArray;
            this.encoding = encoding;
            if (directResultArray) {
                dataArray = new ArrayList<>();
            } else {
                values = new StringBuilder();
            }
        }

        public void newBlock() {
            if (dra) {
                currentArrayLine = new ArrayList<>();
            } else {
                currentLine = new StringBuilder();
            }
            this.emptyLine = true;
        }

        public void appendTime(Date t) {
            if (dra) {
                currentArrayLine.add(t);
            } else {
                synchronized(format2) {
                    currentLine.append(format2.format(t)).append(encoding.getTokenSeparator());
                }
            }
        }

        public void appendDouble(Double d) {
            if (!d.isNaN()) emptyLine = false;
            if (dra) {
                currentArrayLine.add(d);
            } else {
                if (!d.isNaN()) {
                    currentLine.append(Double.toString(d));
                }
                currentLine.append(encoding.getTokenSeparator());
            }
        }

        public void appendString(String value) {
            if (value != null && !value.isEmpty()) emptyLine = false;
            if (dra) {
                currentArrayLine.add(value);
            } else {
                if (value != null && !value.isEmpty()) {
                    currentLine.append(value);
                }
                currentLine.append(encoding.getTokenSeparator());
            }
        }

        public int endBlock() {
            if (!emptyLine) {
                if (dra) {
                    dataArray.add(currentArrayLine);
                } else {
                    values.append(currentLine);
                    // remove last token separator
                    values.deleteCharAt(values.length() - 1);
                    values.append(encoding.getBlockSeparator());
                }
                return 1;
            }
            return 0;
        }

        public String getStringValues() {
            if (values != null) {
                return values.toString();
            }
            return null;
        }

        public List<Object> getDataArray() {
            return dataArray;
        }
    }
}
