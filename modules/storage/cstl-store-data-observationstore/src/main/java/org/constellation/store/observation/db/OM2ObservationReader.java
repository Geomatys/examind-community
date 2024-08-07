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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.ObservationReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;

import javax.sql.DataSource;
import javax.xml.namespace.QName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.store.observation.db.model.OMSQLDialect.DUCKDB;
import static org.constellation.store.observation.db.model.OMSQLDialect.POSTGRES;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import org.geotoolkit.geometry.jts.JTS;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.getOmTypeFromFieldType;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.SamplingFeature;
import static org.geotoolkit.observation.model.TextEncoderProperties.DEFAULT_ENCODING;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.geotoolkit.temporal.object.DefaultInstant;
import org.geotoolkit.temporal.object.DefaultPeriod;
import org.geotoolkit.temporal.object.DefaultTemporalPosition;
import org.opengis.metadata.quality.Element;
import static org.opengis.referencing.IdentifiedObject.NAME_KEY;
import org.opengis.temporal.IndeterminateValue;
import org.opengis.temporal.Instant;

/**
 * Default Observation reader for Postgrid O&amp;M database.
 *
 * @author Guilhem Legal
 */
public class OM2ObservationReader extends OM2BaseReader implements ObservationReader {

    protected final DataSource source;

    public OM2ObservationReader(final DataSource source, final Map<String, Object> properties) throws DataStoreException {
        super(properties, false);
        this.source = source;
        try {
            // try if the connection is valid
            try(final Connection c = this.source.getConnection()) {}
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getEntityNames(OMEntity entityType) throws DataStoreException {
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames();
            case OBSERVED_PROPERTY:   return getPhenomenonNames();
            case PROCEDURE:           return getProcedureNames();
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames();
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    private List<String> getOfferingNames() throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement()) {
            final List<String> results = new ArrayList<>();
            final String query = "SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\"";

            try (final ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving offering names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existEntity(final IdentifierQuery query) throws DataStoreException {
        OMEntity entityType = query.getEntityType();
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String identifier   = query.getIdentifier();
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames().contains(identifier);
            case OBSERVED_PROPERTY:   return existPhenomenon(identifier);
            case PROCEDURE:           return existProcedure(identifier);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames().contains(identifier);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Offering getObservationOffering(String identifier) throws DataStoreException {
        try (final Connection c   = source.getConnection()) {
            return readObservationOffering(identifier, c);
        } catch (SQLException ex) {
             throw new DataStoreException("Error while retrieving offering: " + identifier, ex);
        }
    }

    private List<String> getProcedureNames() throws DataStoreException {
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\"")) {//NOSONAR

            final List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure names.", ex);
        }
    }

    private List<String> getPhenomenonNames() throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\"")) {//NOSONAR
            final List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon names.", ex);
        }
    }

    @Override
    public Phenomenon getPhenomenon(String identifier) throws DataStoreException {
        String where = "WHERE \"id\" = '" + identifier + "'";
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" " + where)) {//NOSONAR
            Phenomenon result          = null;
            while (rs.next()) {
                result = getPhenomenon(rs.getString(1), c);
            }
            return result;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Procedure getProcess(String identifier) throws DataStoreException {
        try (final Connection c = source.getConnection()) {
            return getProcess(identifier, c);
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon.", ex);
        }
    }

    private boolean existPhenomenon(final String phenomenonName) throws DataStoreException {
        return phenomenonName.equals(phenomenonIdBase + "ALL") || getPhenomenonNames().contains(phenomenonName);
    }

    private List<String> getFeatureOfInterestNames() throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"sampling_features\"")) {//NOSONAR
            final List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SamplingFeature getFeatureOfInterest(final String identifier) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getFeatureOfInterest(identifier, c);
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    @Override
    public Geometry getSensorLocation(final String sensorID) throws DataStoreException {
        try(final Connection c = source.getConnection()) {

            final String query = switch(dialect) {
                case POSTGRES -> "SELECT st_asBinary(\"shape\") as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?";
                case DUCKDB   -> "SELECT ST_AsText(\"shape\")  as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?";
                default       -> "SELECT \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?";
            };
            final Geometry geom;
            final int srid;
            try(final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
                stmt.setString(1, sensorID);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        geom = readGeom(rs, "shape");
                        srid = rs.getInt(2);
                    } else {
                        return null;
                    }
                }
            }
            if (geom != null) {
                final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                JTS.setCRS(geom, crs);
            } else {
                return null;
            }
            return geom;
        } catch (SQLException | FactoryException | ParseException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    @Override
    public Map<Date, Geometry> getSensorLocations(final String sensorID) throws DataStoreException {
        final Map<Date, Geometry> results = new HashMap<>();
        final String query = switch(dialect) {
                case POSTGRES -> "SELECT \"time\", st_asBinary(\"location\") as \"location\", \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?";
                case DUCKDB   -> "SELECT \"time\", ST_AsText(\"location\") as \"location\", \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?";
                default       -> "SELECT \"time\", \"location\", \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?";
            };
        try (final Connection c = source.getConnection();
             final PreparedStatement stmt = c.prepareStatement(query)) { //NOSONAR

            stmt.setString(1, sensorID);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    final Date d = new Date(rs.getTimestamp(1).getTime());
                    final Geometry geom = readGeom(rs, "location");
                    if (geom != null) {
                        final int srid = rs.getInt("crs");
                        final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                        JTS.setCRS(geom, crs);
                    } else {
                        return null;
                    }
                    results.put(d, geom);
                }
            }
        } catch (SQLException | FactoryException | ParseException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalGeometricPrimitive getProcedureTime(final String sensorID) throws DataStoreException {
        final String query = "SELECT \"time_begin\", \"time_end\" "
                           + "FROM \"" + schemaPrefix + "om\".\"offerings\" "
                           + "WHERE \"procedure\"=?";
        try(final Connection c          = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, sensorID);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    final Timestamp begin = rs.getTimestamp(1);
                    final Timestamp end = rs.getTimestamp(2);
                    return buildTime(sensorID, begin, end);
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure time.", ex);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getObservation(String identifier, final QName resultModel, final ResponseMode mode) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            String observationID;
            Integer fieldIndex   = null;
            Integer measureId    = null;
            /*
             * observation id, 3 possiblity :
             *       - <observation id>                              ( resultModel = OBSERVATION, multiple measure )
             *       - <observation id> - <measure id>               ( resultModel = OBSERVATION, single measure   )
             *       - <observation id> - <field id> - <measure id>  ( resultModel = MEASUREMENT, single measure   )
             */
            if (identifier.startsWith(observationIdBase)) {
                observationID = identifier.substring(observationIdBase.length());

                String[] component = observationID.split("-");
                if (component.length == 3) {
                    identifier    = observationIdBase + component[0];
                    fieldIndex    = Integer.valueOf(component[1]);
                    measureId     = Integer.valueOf(component[2]);
                } else if (component.length == 2) {
                    identifier    = observationIdBase + component[0];
                    measureId     = Integer.valueOf(component[1]);
                } else if (component.length != 1) {
                    LOGGER.fine("Malformed ID received: " + identifier + ". We expected between 1 and 3 parts, but got " + component.length + ". It might lead to unspecified behaviour");
                }
            /*
             *  observation template id, 2 possiblity :
             *       - <template base> - <proc id>              ( resultModel = OBSERVATION )
             *       - <template base> - <proc id> - <field id> ( resultModel = MEASUREMENT )
             */
            } else if (identifier.startsWith(observationTemplateIdBase)) {
                String procedureID = identifier.substring(observationTemplateIdBase.length());
                // look for a field separator
                int pos = procedureID.lastIndexOf("-");
                if (pos != -1) {
                    try {
                        int fieldIdentifier = Integer.parseInt(procedureID.substring(pos + 1));
                        String tmpProcedureID = procedureID.substring(0, pos);
                        if (existProcedure(sensorIdBase + tmpProcedureID) ||
                            existProcedure(tmpProcedureID)) {
                            procedureID = tmpProcedureID;
                            fieldIndex = fieldIdentifier;
                        }
                    } catch (NumberFormatException ex) {}
                }
                if (existProcedure(sensorIdBase + procedureID)) {
                    procedureID = sensorIdBase + procedureID;
                }
                try (final PreparedStatement stmt = c.prepareStatement("SELECT \"id\", \"identifier\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?")) {//NOSONAR
                    stmt.setString(1, procedureID);
                    try(final ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            observationID = rs.getString(1);
                            identifier = rs.getString(2);
                        } else {
                            return null;
                        }
                    }
                }

            // i don't think this case should be accepted anymore
            } else {
                observationID = identifier;
            }

            final String obsID = "obs-" + observationID;
            final String observedProperty;
            final String procedure;
            final String foi;
            final int oid;
            TemporalGeometricPrimitive time = null;

            try(final PreparedStatement stmt  = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {//NOSONAR
                stmt.setString(1, identifier);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        oid = rs.getInt(2);
                        final Date b = rs.getTimestamp(3);
                        final Date e = rs.getTimestamp(4);
                        if (b != null && e == null) {
                            time = buildTime(observationID, b, null);
                        } else if (b != null && e != null) {
                            time = buildTime(observationID, b, e);
                        }
                        observedProperty = rs.getString(5);
                        procedure = rs.getString(6);
                        foi = rs.getString(7);
                    } else {
                        return null;
                    }
                }
            }

            final SamplingFeature feature = getFeatureOfInterest(foi, c);
            final Phenomenon phen         = getPhenomenon(observedProperty, c);

            final String name;
            if (ResponseMode.RESULT_TEMPLATE.equals(mode)) {
                final String procedureID = procedure.substring(sensorIdBase.length());
                name = observationTemplateIdBase + procedureID;
            } else {
                name = observationIdBase + observationID;
            }

            Map<String, Object> properties = new HashMap<>();
            final ProcedureInfo pi = getPIDFromProcedure(procedure, c).orElseThrow(IllegalArgumentException::new);
            properties.put("type", pi.type);
            final Procedure proc = getProcess(procedure, c);
            final Phenomenon resultPhen;
            final Result result;
            List<Element> resultQuality;
            final String omType;
            if (resultModel.equals(MEASUREMENT_QNAME)) {
                if (fieldIndex == null) {
                    throw new DataStoreException("Measurement extraction need a field index specified");
                }
                Field selectedField = getFieldByIndex(procedure, fieldIndex, true, c);
                if (phen instanceof CompositePhenomenon) {
                    resultPhen = getPhenomenon(selectedField.name, c);
                } else {
                    resultPhen = phen;
                }
                if (ResponseMode.RESULT_TEMPLATE.equals(mode)) {
                    resultQuality = new ArrayList<>();
                    result = new MeasureResult(selectedField, null);
                } else {
                    resultQuality = buildResultQuality(pi, identifier, measureId, selectedField, c);
                    result = getResult(pi, oid, resultModel, measureId, selectedField, c);
                    if ("timeseries".equals(pi.type)) {
                        time = getMeasureTimeForTimeSeries(pi, identifier, c, measureId);
                    }
                }
                omType = getOmTypeFromFieldType(selectedField.type);
                
            } else {
                omType        = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation";
                resultQuality = new ArrayList<>();
                resultPhen    = phen;
                if (ResponseMode.RESULT_TEMPLATE.equals(mode)) {
                    final List<Field> fields = readFields(procedure, false, c, new ArrayList<>(), new ArrayList<>());
                    result = new ComplexResult(fields, DEFAULT_ENCODING, null, null);
                } else {
                    result = getResult(pi, oid, resultModel, measureId, null, c);
                }
            }
            return new Observation(obsID,
                                   name,
                                   null,
                                   null,
                                   omType,
                                   proc,
                                   time,
                                   feature,
                                   resultPhen,
                                   resultQuality,
                                   result,
                                   properties);

        } catch (Exception ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * Return the time value (main field) for the specified measure.
     * Work only for timeseries.
     * 
     * @param identifier Observation identifier.
     * @param c A SQL connection.
     * @param measureId identifier od the measure.
     *
     * @return the time value (main field) for the specified measure.
     * @throws SQLException
     */
    private TemporalGeometricPrimitive getMeasureTimeForTimeSeries(ProcedureInfo pti, String identifier, final Connection c, int measureId) throws SQLException {
        FilterSQLRequest query  = buildMesureRequests(pti, List.of(pti.mainField), null, null, true, false, false, false);
        query.append(" AND o.\"identifier\"=").appendValue(identifier);
        query.append(" AND m.\"id\" = ").appendValue(measureId);
        try (final SQLResult rs  = query.execute(c)) {
            int tableNum = rs.getFirstTableNumber();
            if (rs.next()) {
                final Timestamp t = rs.getTimestamp(pti.mainField.name, tableNum);
                return buildTime(identifier, t, null);
            }
        }
        return null;
    }

    /*
    * Not optimal at all. should be merged with buildResult
    */
    private List<Element> buildResultQuality(ProcedureInfo pti, final String identifier, final Integer measureId, final Field selectedField, final Connection c) throws SQLException, DataStoreException {
        if (selectedField == null) {
            throw new DataStoreException("Measurement extraction need a field index specified");
        }
        FilterSQLRequest query = buildMesureRequests(pti, List.of(selectedField), null, null, true, false, false, false);
        query.append("AND o.\"identifier\"=").appendValue(identifier);
        if (measureId != null) {
            query.append(" AND m.\"id\" = " + measureId + " ");
        }
        query.append(" ORDER BY m.\"id\"");

        try (final SQLResult rs  = query.execute(c)) {
            if (rs.next()) {
                return buildResultQuality(selectedField, rs);
            }
        } catch (NumberFormatException ex) {
            throw new DataStoreException("Unable ta parse the result value as a double");
        }
        return new ArrayList<>();
    }

    protected Set<String> getFoiIdsForProcedure(final String procedure, final Connection c) throws SQLException {
        try (final PreparedStatement stmt = c.prepareStatement("SELECT \"foi\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?")) {//NOSONAR
            stmt.setString(1, procedure);
            try (final ResultSet rs = stmt.executeQuery()) {
                final Set<String> results = new HashSet<>();
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
                return results;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getTemplateForProcedure(final String procedure) throws DataStoreException {
        try (final Connection c = source.getConnection()) {

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
            Set<String> fois = getFoiIdsForProcedure(procedure, c);
            if (fois.size() == 1) {
                featureID = fois.iterator().next();
                feature = getFeatureOfInterest(featureID, c);
            }
            TemporalGeometricPrimitive tempTime = getTimeForTemplate(c, procedure, null, featureID);
            List<Field> fields = readFields(procedure, c);
            Map<String, Object> properties = new HashMap<>();
            properties.put("type", getProcedureOMType(procedure, c));
            final Procedure proc = (Procedure) getProcess(procedure, c);
            /*
             *  BUILD RESULT
             */
            final ComplexResult result = new ComplexResult(fields, DEFAULT_ENCODING, null, null);
            return new Observation(obsID,
                                    name,
                                    null,
                                    null,
                                    "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation",
                                    proc,
                                    tempTime,
                                    feature,
                                    phen,
                                    null,
                                    result,
                                    properties);
        } catch (Exception ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * 
     * @param sensorId
     * @return
     * @throws DataStoreException
     */
    private boolean existProcedure(final String sensorId) throws DataStoreException {
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\" = '" + sensorId + "' fetch first 1 rows only")) {//NOSONAR
            return rs.next();
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalGeometricPrimitive getFeatureOfInterestTime(final String identifier) throws DataStoreException {
        final String query = "SELECT min(\"time_begin\") as mib, max(\"time_begin\") as mab, max(\"time_end\") as mae "
                           + "FROM \"" + schemaPrefix + "om\".\"observations\" "
                           + "WHERE \"foi\"=?";
        try(final Connection c           = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, identifier);
            try (final ResultSet rs = stmt.executeQuery()) {
                final TemporalGeometricPrimitive time;
                if (rs.next()) {
                    final Timestamp mib = rs.getTimestamp(1);
                    final Timestamp mab = rs.getTimestamp(2);
                    final Timestamp mae = rs.getTimestamp(3);
                    Map<String, ?> props = Collections.singletonMap(NAME_KEY, identifier + "-time");
                    if (mib != null && mae == null) {
                        if (mab != null && !mib.equals(mab)) {
                            time = new DefaultPeriod(props,
                                                     new DefaultInstant(Collections.singletonMap(NAME_KEY, identifier + "-time-st"), mib),
                                                     new DefaultInstant(Collections.singletonMap(NAME_KEY, identifier + "-time-en"), mab));
                        } else {
                            time = new DefaultInstant(props, mib);
                        }
                    } else if (mib != null && mae != null) {
                        if (mab != null && mab.after(mae)) {
                            time = new DefaultPeriod(props, 
                                                     new DefaultInstant(Collections.singletonMap(NAME_KEY, identifier + "-time-st"), mib),
                                                     new DefaultInstant(Collections.singletonMap(NAME_KEY, identifier + "-time-en"), mab));
                        } else {
                            time = new DefaultPeriod(props, 
                                                    new DefaultInstant(Collections.singletonMap(NAME_KEY, identifier + "-time-st"), mib),
                                                    new DefaultInstant(Collections.singletonMap(NAME_KEY, identifier + "-time-en"), mae));
                        }
                    } else {
                        time = null;
                    }
                } else {
                    time = null;
                }
                return time;
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalGeometricPrimitive getEventTime() throws DataStoreException {
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT min(\"time_begin\"), max(\"time_end\") FROM \"" + schemaPrefix + "om\".\"offerings\"")) {//NOSONAR
            Instant start        = new DefaultInstant(Collections.singletonMap(NAME_KEY, "event-time-st"), new DefaultTemporalPosition(IndeterminateValue.UNKNOWN));
            Instant end          = new DefaultInstant(Collections.singletonMap(NAME_KEY, "event-time-en"), new DefaultTemporalPosition(IndeterminateValue.NOW));
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp(1);
                if (ts != null) {
                    start = new DefaultInstant(Collections.singletonMap(NAME_KEY, "event-time-st"), ts);
                }
                ts = rs.getTimestamp(2);
                if (ts != null) {
                    end = new DefaultInstant(Collections.singletonMap(NAME_KEY, "event-time-en"), ts);
                }
            }
            return new DefaultPeriod(Collections.singletonMap(NAME_KEY, "event-time"), start, end);
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon names.", ex);
        }
    }
}
