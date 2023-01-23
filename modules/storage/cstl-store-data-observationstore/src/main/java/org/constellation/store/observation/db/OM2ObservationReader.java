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
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.ObservationReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.temporal.TemporalPrimitive;
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
import org.geotoolkit.geometry.jts.JTS;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.getOmTypeFromFieldType;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.ResultMode;
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

    public OM2ObservationReader(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties, final boolean timescaleDB) throws DataStoreException {
        super(properties, schemaPrefix, false, isPostgres, timescaleDB);
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

            final byte[] b;
            final int srid;
            try(final PreparedStatement stmt = (isPostgres) ?
                    c.prepareStatement("SELECT st_asBinary(\"shape\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?") ://NOSONAR
                    c.prepareStatement("SELECT \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
                stmt.setString(1, sensorID);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        b = rs.getBytes(1);
                        srid = rs.getInt(2);
                    } else {
                        return null;
                    }
                }
            }
            final CoordinateReferenceSystem crs;
            if (srid != 0) {
                crs = CRS.forCode("urn:ogc:def:crs:EPSG::" + srid);
            } else {
                crs = defaultCRS;
            }
            final Geometry geom;
            if (b != null) {
                WKBReader reader = new WKBReader();
                geom             = reader.read(b);
                JTS.setCRS(geom, crs);
            } else {
                return null;
            }
            return geom;
        } catch (SQLException | FactoryException  | ParseException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    @Override
    public Map<Date, Geometry> getSensorLocations(final String sensorID) throws DataStoreException {
        final Map<Date, Geometry> results = new HashMap<>();
        try(final Connection c = source.getConnection()) {

            try(final PreparedStatement stmt = (isPostgres) ?
                    c.prepareStatement("SELECT \"time\", st_asBinary(\"location\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?") ://NOSONAR
                    c.prepareStatement("SELECT \"time\", \"location\", \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?")) {//NOSONAR
                stmt.setString(1, sensorID);
                try(final ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        final Date d = new Date(rs.getTimestamp(1).getTime());
                        final byte[] b = rs.getBytes(2);
                        final int srid = rs.getInt(3);
                        final CoordinateReferenceSystem crs;
                        if (srid != 0) {
                            crs = CRS.forCode("urn:ogc:def:crs:EPSG::" + srid);
                        } else {
                            crs = defaultCRS;
                        }
                        final Geometry geom;
                        if (b != null) {
                            WKBReader reader = new WKBReader();
                            geom             = reader.read(b);
                            JTS.setCRS(geom, crs);
                        } else {
                            return null;
                        }
                        results.put(d, geom);
                    }
                }
            }

        } catch (SQLException | FactoryException  | ParseException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalGeometricPrimitive getTimeForProcedure(final String sensorID) throws DataStoreException {
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
            String observationID = null;
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
                    LOGGER.fine("Malformed ID received: " + observationID + ". We expected between 1 and 3 parts, but got " + component.length + ". It might lead to unspecified behaviour");
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
            TemporalGeometricPrimitive time = null;

            try(final PreparedStatement stmt  = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {//NOSONAR
                stmt.setString(1, identifier);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
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

            final Field mainField = getMainField(procedure, c);
            Map<String, Object> properties = new HashMap<>();
            if (mainField.type == FieldType.TIME) {
                properties.put("type", "timeseries");
            } else {
                properties.put("type", "profile");
            }
            final Procedure proc = getProcess(procedure, c);
            final Phenomenon resultPhen;
            final Result result;
            List<Element> resultQuality;
            final String omType;
            if (resultModel.equals(MEASUREMENT_QNAME)) {
                if (fieldIndex == null) {
                    throw new DataStoreException("Measurement extraction need a field index specified");
                }
                Field selectedField         = getFieldByIndex(procedure, fieldIndex, true, c);
                resultQuality = buildResultQuality(identifier, procedure, measureId, selectedField, c);
                result = getResult(identifier, resultModel, measureId, selectedField, c);
                omType = getOmTypeFromFieldType(selectedField.type);
                if (phen instanceof CompositePhenomenon) {
                    resultPhen = getPhenomenon(selectedField.name, c);
                } else {
                    resultPhen = phen;
                }
                if (FieldType.TIME.equals(mainField.type)) {
                    time = getMeasureTimeForProfile(identifier, mainField, c, measureId);
                }
            } else {
                resultQuality = new ArrayList<>();
                resultPhen = phen;
                result = getResult(identifier, resultModel, measureId, null, c);
                omType = "http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_ComplexObservation";
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

        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result getResult(final String identifier, final QName resultModel) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getResult(identifier, resultModel, null, null, c);
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private Result getResult(final String identifier, final QName resultModel, final Integer measureId, final Field selectedField, final Connection c) throws DataStoreException, SQLException {
        if (resultModel.equals(MEASUREMENT_QNAME)) {
            return buildMeasureResult(identifier, measureId, selectedField, c);
        } else {
            return buildComplexResult2(identifier, measureId, c);
        }
    }

    private ComplexResult buildComplexResult2(final String identifier, final Integer measureId, final Connection c) throws DataStoreException, SQLException {

        final String procedure     = getProcedureFromObservation(identifier, c);
        final String measureJoin   = getMeasureTableJoin(getPIDFromProcedure(procedure, c));
        final List<Field> fields   = readFields(procedure, false, c);

        int nbValue                 = 0;
        final ResultBuilder values  = new ResultBuilder(ResultMode.CSV, DEFAULT_ENCODING, false);
        final FieldParser parser    = new FieldParser(fields, values, false, false, true, null);
        String query                = "SELECT * FROM " + measureJoin + ", \"" + schemaPrefix + "om\".\"observations\" o "
                                    + "WHERE \"id_observation\" = o.\"id\" "
                                    + "AND o.\"identifier\"=?";
        if (measureId != null) {
            query = query + " AND m.\"id\" = " + measureId + " ";
        }
        query = query + " ORDER BY m.\"id\"";
        try(final PreparedStatement stmt  = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, identifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parser.parseLine(rs, 0);
                    nbValue = nbValue + parser.nbValue;
                }
            }
        }
        return new ComplexResult(fields, DEFAULT_ENCODING, values.getStringValues(), nbValue);
    }

    private MeasureResult buildMeasureResult(final String identifier, final Integer measureId, final Field selectedField, final Connection c) throws DataStoreException, SQLException {
        if (selectedField == null) {
            throw new DataStoreException("Measurement extraction need a field index specified");
        }
        final String measureJoin   = getMeasureTableJoin(getPIDFromObservation(identifier, c));
        final String uom           = selectedField.uom;
        final FieldType fType      = selectedField.type;
        String query       = "SELECT * FROM " + measureJoin + ", \"" + schemaPrefix + "om\".\"observations\" o "
                           + "WHERE \"id_observation\" = o.\"id\" "
                           + "AND o.\"identifier\"=?";
        if (measureId != null) {
            query = query + " AND m.\"id\" = " + measureId + " ";
        }
        query = query + " ORDER BY m.\"id\"";

        try(final PreparedStatement stmt  = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, identifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object value;
                    if (fType == FieldType.QUANTITY) {
                        value = rs.getDouble(selectedField.name);
                    } else if (fType == FieldType.BOOLEAN) {
                        value = rs.getBoolean(selectedField.name);
                    } else if (fType == FieldType.TIME) {
                        Timestamp ts = rs.getTimestamp(selectedField.name);
                        /*
                        GregorianCalendar cal = new GregorianCalendar();
                        cal.setTimeInMillis(ts.getTime());
                        value = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal) ;*/
                        value = new Date(ts.getTime());
                    } else {
                        value = rs.getString(selectedField.name);
                    }
                    return new MeasureResult(selectedField, value);
                } else {
                    return null;
                }
            }
        } catch (NumberFormatException ex) {
            throw new DataStoreException("Unable ta parse the result value as a double");
        }
    }

    private TemporalGeometricPrimitive getMeasureTimeForProfile(String identifier, Field mainField, final Connection c, int measureId) throws SQLException {
        final String measureJoin   = getMeasureTableJoin(getPIDFromObservation(identifier, c));
        String query       = "SELECT * FROM " + measureJoin + ", \"" + schemaPrefix + "om\".\"observations\" o "
                           + "WHERE \"id_observation\" = o.\"id\" "
                           + "AND o.\"identifier\" = ? "
                           + "AND m.\"id\" = ?";
        try(final PreparedStatement stmt  = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, identifier);
            stmt.setInt(2, measureId);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    final Timestamp t = rs.getTimestamp(mainField.name);
                    return buildTime(identifier, t, null);
                    }
                }
            }
        return null;
    }

    /*
    * Not optimal at all. should be merged with buildResult
    */
    private List<Element> buildResultQuality(final String identifier, final String procedure, final Integer measureId, final Field selectedField, final Connection c) throws SQLException, DataStoreException {
        if (selectedField == null) {
            throw new DataStoreException("Measurement extraction need a field index specified");
        }
        final String measureJoin   = getMeasureTableJoin(getPIDFromProcedure(procedure, c));
        String query       = "SELECT * FROM " + measureJoin + ", \"" + schemaPrefix + "om\".\"observations\" o "
                           + "WHERE \"id_observation\" = o.\"id\" "
                           + "AND o.\"identifier\"=?";
        if (measureId != null) {
            query = query + " AND m.\"id\" = " + measureId + " ";
        }
        query = query + " ORDER BY m.\"id\"";

        try(final PreparedStatement stmt  = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, identifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return buildResultQuality(selectedField, rs);
                }
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
            final Field mainField = getMainField(procedure, c);
            Map<String, Object> properties = new HashMap<>();
            if (mainField != null && mainField.type == FieldType.TIME) {
                properties.put("type", "timeseries");
            } else {
                properties.put("type", "profile");
            }
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
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * TODO optimize
     * 
     * @param href
     * @return
     * @throws DataStoreException
     */
    private boolean existProcedure(final String href) throws DataStoreException {
        return getProcedureNames().contains(href);
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
    public TemporalPrimitive getFeatureOfInterestTime(final String identifier) throws DataStoreException {
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
    public TemporalPrimitive getEventTime() throws DataStoreException {
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
