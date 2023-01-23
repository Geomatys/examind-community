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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.constellation.util.Util;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.PHENOMENON_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.SENSOR_ID_BASE_NAME;
import org.geotoolkit.observation.OMUtils;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.getOverlappingComposite;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.SamplingFeature;

import org.opengis.metadata.quality.Element;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2BaseReader {

    protected final boolean isPostgres;
    
    protected final boolean timescaleDB;

    /**
     * The base for observation id.
     */
    protected final String observationIdBase;

    protected final String phenomenonIdBase;

    protected final String sensorIdBase;

    protected final String observationTemplateIdBase;

    protected final String schemaPrefix;

    /**
     * Some sub-classes of the base reader are used in a single session (Observation filters).
     * So they can activate the cache to avoid reading the same object in the same session.
     */
    protected boolean cacheEnabled;

    /**
     * A map of already read sampling feature.
     *
     * This map is only populated if {@link OM2BaseReader#cacheEnabled} is set to true.
     */
    private final Map<String, SamplingFeature> cachedFoi = new HashMap<>();

    /**
     * A map of already read Phenomenon.
     *
     * This map is only populated if {@link OM2BaseReader#cacheEnabled} is set to true.
     */
    private final Map<String, Phenomenon> cachedPhenomenon = new HashMap<>();

    protected final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected final SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

    protected Field DEFAULT_TIME_FIELD = new Field(-1, FieldType.TIME, "time", null, "http://www.opengis.net/def/property/OGC/0/SamplingTime", null);

    public OM2BaseReader(final Map<String, Object> properties, final String schemaPrefix, final boolean cacheEnabled, final boolean isPostgres, final boolean timescaleDB) throws DataStoreException {
        this.isPostgres = isPostgres;
        this.timescaleDB = timescaleDB;
        this.phenomenonIdBase = (String) properties.getOrDefault(PHENOMENON_ID_BASE_NAME, "");
        this.sensorIdBase = (String) properties.getOrDefault(SENSOR_ID_BASE_NAME, "");
        this.observationTemplateIdBase = (String) properties.getOrDefault(OBSERVATION_TEMPLATE_ID_BASE_NAME, "urn:observation:template:");
        this.observationIdBase         = (String) properties.getOrDefault(OBSERVATION_ID_BASE_NAME, "");
        if (schemaPrefix == null) {
            this.schemaPrefix = "";
        } else {
            if (Util.containsForbiddenCharacter(schemaPrefix)) {
                throw new DataStoreException("Invalid schema prefix value");
            }
            this.schemaPrefix = schemaPrefix;
        }
        this.cacheEnabled = cacheEnabled;
    }

    public OM2BaseReader(final OM2BaseReader that) {
        this.phenomenonIdBase          = that.phenomenonIdBase;
        this.observationTemplateIdBase = that.observationTemplateIdBase;
        this.sensorIdBase              = that.sensorIdBase;
        this.isPostgres                = that.isPostgres;
        this.observationIdBase         = that.observationIdBase;
        this.schemaPrefix              = that.schemaPrefix;
        this.cacheEnabled              = that.cacheEnabled;
        this.timescaleDB               = that.timescaleDB;
    }

    /**
     * use for debugging purpose
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");

    protected static final CoordinateReferenceSystem defaultCRS = CommonCRS.WGS84.geographic();

    protected SamplingFeature getFeatureOfInterest(final String id, final Connection c) throws SQLException, DataStoreException {
        if (cacheEnabled && cachedFoi.containsKey(id)) {
            return cachedFoi.get(id);
        }
        try {
            final String name;
            final String description;
            final String sampledFeature;
            final byte[] b;
            final int srid;
            final Map<String, Object> properties = readProperties("sampling_features_properties", "id_sampling_feature", id, c);
            try (final PreparedStatement stmt = (isPostgres) ?
                c.prepareStatement("SELECT \"id\", \"name\", \"description\", \"sampledfeature\", st_asBinary(\"shape\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?") ://NOSONAR
                c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?")) {//NOSONAR
                stmt.setString(1, id);
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        name = rs.getString(2);
                        description = rs.getString(3);
                        sampledFeature = rs.getString(4);
                        b = rs.getBytes(5);
                        srid = rs.getInt(6);
                    } else {
                        return null;
                    }
                }
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
                    JTS.setCRS(geom, crs);
                } else {
                    geom = null;
                }
                final SamplingFeature sf = new SamplingFeature(id, name, description, properties, sampledFeature, geom);
                if (cacheEnabled) {
                    cachedFoi.put(id, sf);
                }
                return sf;
            }

        } catch (ParseException | FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    protected Map<String, Object> readProperties(String tableName, String columnName, String id, Connection c) throws SQLException {
        String request = "SELECT \"property_name\", \"value\" FROM \"" + schemaPrefix + "om\".\"" + tableName + "\" WHERE \"" + columnName + "\"=?";
        LOGGER.fine(request);
        Map<String, Object> results = new HashMap<>();
        try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, id);
            try (final ResultSet rs   = stmt.executeQuery()) {
                while (rs.next()) {
                    String pName = rs.getString("property_name");
                    String pValue = rs.getString("value");
                    results.put(pName, pValue);
                }
            }
        }
        return results;
    }

    @SuppressWarnings("squid:S2695")
    protected TemporalGeometricPrimitive getTimeForTemplate(Connection c, String procedure, String observedProperty, String foi) {
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
                        return buildTime(procedure, minBegin, maxEnd);
                    } else if (minBegin != null && !minBegin.equals(maxBegin)) {
                        return buildTime(procedure, minBegin, maxBegin);
                    } else if (minBegin != null) {
                        return buildTime(procedure, minBegin, null);
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while looking for template time.", ex);
        }
        return null;
    }

    protected Set<Phenomenon> getAllPhenomenon(final Connection c) throws DataStoreException {
        try(final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" ")) {//NOSONAR
            final Set<Phenomenon> results = new HashSet<>();
            while (rs.next()) {
                results.add(getPhenomenon(rs.getString(1), c));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving all phenomenons.", ex);
        }
    }

    protected Phenomenon getPhenomenon(final String observedProperty, final Connection c) throws DataStoreException {
        final String id;
        // cleanup phenomenon id because of its XML ype (NCName)
        if (observedProperty.startsWith(phenomenonIdBase)) {
            id = observedProperty.substring(phenomenonIdBase.length()).replace(':', '-');
        } else {
            id = observedProperty.replace(':', '-');
        }
        if (cacheEnabled && cachedPhenomenon.containsKey(id)) {
            return cachedPhenomenon.get(id);
        }
        try {
            // look for composite phenomenon
            try (final PreparedStatement stmt = c.prepareStatement("SELECT \"component\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"phenomenon\"=? ORDER BY \"order\" ASC")) {//NOSONAR
                stmt.setString(1, observedProperty);
                try(final ResultSet rs = stmt.executeQuery()) {
                    final List<Phenomenon> phenomenons = new ArrayList<>();
                    while (rs.next()) {
                        final String phenID = rs.getString(1);
                        phenomenons.add(getSinglePhenomenon(phenID, c));
                    }
                    Phenomenon base = getSinglePhenomenon(observedProperty, c);
                    Phenomenon result = null;
                    if (base != null) {
                        if (phenomenons.isEmpty()) {
                            result = base;
                        } else {
                            result = new CompositePhenomenon(id, base.getName(), base.getDefinition(), base.getDescription(), base.getProperties(), phenomenons);
                        }
                        if (cacheEnabled) {
                            cachedPhenomenon.put(id, result);
                        }
                    }
                    return result;
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private Phenomenon getSinglePhenomenon(final String id, final Connection c) throws DataStoreException {
        try {
            final Map<String, Object> properties = readProperties("observed_properties_properties", "id_phenomenon", id, c);
            try (final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"observed_properties\" WHERE \"id\"=?")) {//NOSONAR
                stmt.setString(1, id);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String phenID = rs.getString(1);
                        String name = rs.getString(3);
                        String definition = rs.getString(4);
                        final String description = rs.getString(5);

                        // hack for valid phenomenon ID in 1.0.0 static fields
                        if (phenID != null) {
                            if (phenID.equals("http://mmisw.org/ont/cf/parameter/latitude")) {
                                name = "latitude";
                            } else if (phenID.equals("http://mmisw.org/ont/cf/parameter/longitude")) {
                                name = "longitude";
                            } else if (phenID.equals("http://www.opengis.net/def/property/OGC/0/SamplingTime")) {
                                name = "samplingTime";
                            }
                            if (name == null) {
                                name = phenID.startsWith(phenomenonIdBase) ? phenID.substring(phenomenonIdBase.length()) : phenID;
                            }
                        }
                        return new Phenomenon(phenID, name, definition, description, properties);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return null;
    }

     /**
     * Return the global phenomenon for a procedure.
     * We need this method because some procedure got multiple observation with only a phenomon component,
     * and not the full composite.
     * some other are registered with composite that are a subset of the global procedure phenomenon.
     *
     * @return
     */
    protected Phenomenon getGlobalCompositePhenomenon(Connection c, String procedure) throws DataStoreException {
       String request = "SELECT DISTINCT(\"observed_property\") FROM \"" + schemaPrefix + "om\".\"observations\" o "
                      + "WHERE \"procedure\"=? ";
       LOGGER.fine(request);
       try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, procedure);
            try (final ResultSet rs   = stmt.executeQuery()) {
                List<CompositePhenomenon> composites = new ArrayList<>();
                List<Phenomenon> singles = new ArrayList<>();
                while (rs.next()) {
                    Phenomenon phen = getPhenomenon(rs.getString("observed_property"), c);
                    if (phen instanceof CompositePhenomenon compo) {
                        composites.add(compo);
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
                        return getVirtualCompositePhenomenon(c, procedure);
                    }
                } else if (composites.size() == 1) {
                    return composites.get(0);
                } else  {
                    // multiple composite phenomenons are present, we must choose the global one
                    return (Phenomenon) getOverlappingComposite(composites);
                }
            }
       } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while looking for global phenomenon.", ex);
            throw new DataStoreException("Error while looking for global phenomenon.");
       }
    }

    protected Phenomenon getVirtualCompositePhenomenon(Connection c, String procedure) throws DataStoreException {
       String request = "SELECT \"field_name\" FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" "
                      + "WHERE \"procedure\"=? "
                      + "AND NOT (\"order\"=1 AND \"field_type\"='Time') "
                      + "AND \"parent\" IS NULL "
                      + "order by \"order\"";
       LOGGER.fine(request);
       try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, procedure);
            try (final ResultSet rs   = stmt.executeQuery()) {
                List<Phenomenon> components = new ArrayList<>();
                int i = 0;
                while (rs.next()) {
                    final String fieldName = rs.getString("field_name");
                    Phenomenon phen = getPhenomenon(fieldName, c);
                    if (phen == null) {
                        throw new DataStoreException("Unable to link a procedure field to a phenomenon:" + fieldName);
                    }
                    components.add(phen);
                }
                if (components.size() == 1) {
                    return components.get(0);
                } else {
                    final String name = "computed-phen-" + procedure;
                    return new CompositePhenomenon(name, name, name, null, null, components);
                }
            }
       } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error while building virtual composite phenomenon.", ex);
            throw new DataStoreException("Error while building virtual composite phenomenon.");
       }
    }

    protected Offering readObservationOffering(final String offeringId, final Connection c) throws DataStoreException {
        final String id;
        final String name;
        final String description;
        final TemporalGeometricPrimitive time;
        final String procedure;
        final List<String> phens = new ArrayList<>();
        final List<String> foi       = new ArrayList<>();

        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"offerings\" WHERE \"identifier\"=?")) {//NOSONAR
            stmt.setString(1, offeringId);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    id                 = rs.getString(1);
                    description        = rs.getString(2);
                    name               = rs.getString(3);
                    final Timestamp b  = rs.getTimestamp(4);
                    final Timestamp e  = rs.getTimestamp(5);
                    procedure          = rs.getString(6);
                    time               = OMUtils.buildTime(id, b, e);
                } else {
                    return null;
                }
            }

            try(final PreparedStatement stmt2 = c.prepareStatement("SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"offering_observed_properties\" WHERE \"id_offering\"=?")) {//NOSONAR
                stmt2.setString(1, offeringId);
                try(final ResultSet rs2 = stmt2.executeQuery()) {
                    while (rs2.next()) {
                        phens.add(rs2.getString(1));
                    }
                }
            }

            try(final PreparedStatement stmt3 = c.prepareStatement("SELECT \"foi\" FROM \"" + schemaPrefix + "om\".\"offering_foi\" WHERE \"id_offering\"=?")) {//NOSONAR
                stmt3.setString(1, offeringId);
                try(final ResultSet rs3 = stmt3.executeQuery()) {
                    while (rs3.next()) {
                        foi.add(rs3.getString(1));
                    }
                }
            }

            return new Offering( id,
                                 name,
                                 description,
                                 null,
                                 null, // bounds
                                 new ArrayList<>(),
                                 time,
                                 procedure,
                                 phens,
                                 foi);

        } catch (SQLException e) {
            throw new DataStoreException("Error while retrieving offering: " + offeringId, e);
        }
    }

    protected List<Field> readFields(final String procedureID, final Connection c) throws SQLException {
        return readFields(procedureID, false, c);
    }
    
    protected List<Field> readFields(final String procedureID, final boolean removeMainTimeField, final Connection c) throws SQLException {
        final List<Field> results = new ArrayList<>();
        String query = "SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"parent\" IS NULL";
        if (removeMainTimeField) {
            query = query + " AND NOT(\"order\"= 1 AND \"field_type\"= 'Time')";
        }
        query = query + " ORDER BY \"order\"";
        try(final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, procedureID);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(getFieldFromDb(rs, procedureID, c, true));
                }
                return results;
            }
        }
    }

    protected List<DbField> completeDbField(final String procedureID, final List<String> fieldNames, final Connection c) throws SQLException {
        List<DbField> results = new ArrayList<>();
        for (String fieldName : fieldNames) {
            results.add(completeDbField(procedureID, fieldName, c));
        }
        return results;
    }

    protected DbField completeDbField(final String procedureID, final String fieldName, final Connection c) throws SQLException {
        final String query = "SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"parent\" IS NULL AND \"field_name\" = ?";
        try(final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setString(2, fieldName);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getFieldFromDb(rs, procedureID, c, true);
                }
            }
        }
        throw new SQLException("No field " + fieldName + " found for procedure:" + procedureID);
    }

    protected Field getTimeField(final String procedureID, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"field_type\"='Time' AND \"parent\" IS NULL ORDER BY \"order\"")) {//NOSONAR
            stmt.setString(1, procedureID);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getFieldFromDb(rs, procedureID, c, false);
                }
                return null;
            }
        }
    }

    protected boolean isMainTimeField(final String procedureID, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"field_type\" FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"field_type\"='Time'  AND \"parent\" IS NULL AND \"order\"=1")) {//NOSONAR
            stmt.setString(1, procedureID);
            try (final ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Return the main field of the timeseries/trajectory (TIME) or profile (DEPTH, PRESSION, ...).
     * This method assume that the main field is !ALWAYS! set a the order 1.
     *
     * @param procedureID
     * @param c
     * @return
     * @throws SQLException
     */
    protected Field getMainField(final String procedureID, final Connection c) throws SQLException {
        return getFieldByIndex(procedureID, 1, false, c);
    }

    protected Field getFieldByIndex(final String procedureID, final int index, final boolean fetchQualityFields, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"order\"=?  AND \"parent\" IS NULL")) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setInt(2, index);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getFieldFromDb(rs, procedureID, c, fetchQualityFields);
                }
                return null;
            }
        }
    }

    /**
     * Return the positions field for trajectory.
     * This method assume that the fields are names 'lat' or 'lon'
     *
     * @param procedureID
     * @param c
     * @return
     * @throws SQLException
     */
    protected List<Field> getPosFields(final String procedureID, final Connection c) throws SQLException {
        final List<Field> results = new ArrayList<>();
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND (\"field_name\"='lat' OR \"field_name\"='lon') AND \"parent\" IS NULL ORDER BY \"order\" DESC")) {//NOSONAR
            stmt.setString(1, procedureID);
            try (final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(getFieldFromDb(rs, procedureID, c, false));
                }
            }
        }
        return results;
    }

    protected Field getFieldForPhenomenon(final String procedureID, final String phenomenon, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND (\"field_name\"= ?) AND \"parent\" IS NULL")) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setString(2, phenomenon);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getFieldFromDb(rs, procedureID, c, true);
                }
                return null;
            }
        }
    }

    private DbField getFieldFromDb(final ResultSet rs, String procedureID, Connection c, boolean fetchQualityFields) throws SQLException {
        final String fieldName = rs.getString("field_name");
        final DbField f = new DbField(
                         rs.getInt("order"),
                         FieldType.fromLabel(rs.getString("field_type")),
                         fieldName,
                         fieldName,
                         rs.getString("field_definition"),
                         rs.getString("uom"),
                         rs.getInt("table_number"));

        if (fetchQualityFields) {
            try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"parent\"=? ORDER BY \"order\"")) {//NOSONAR
                stmt.setString(1, procedureID);
                stmt.setString(2, fieldName);
                try(final ResultSet rss = stmt.executeQuery()) {
                    if (rss.next()) {
                        f.qualityFields.add(getFieldFromDb(rss, procedureID, c, false));
                    }
                }
            }
        }
        return f;
    }

    protected String getMeasureTableJoin(int[] pidNumber) {
        int pid = pidNumber[0];
        int nbTable = pidNumber[1];
        StringBuilder result = new StringBuilder("\"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m");
        for (int i = 1; i < nbTable; i++) {
            String alias = "m" + (i+1);
            result.append(" LEFT JOIN \"" + schemaPrefix + "mesures\".\"mesure" + pid + "_" + (i+1) + "\" " + alias + " ON (m.\"id\" = " + alias + ".\"id\" and  m.\"id_observation\" = " + alias + ".\"id_observation\") ");
        }
        return result.toString();
    }

    /**
     * Return the PID (internal int procedure identifier) and the number of measure table associated for the specified observation.
     * If there is no procedure for the specified procedure id, thsi method will return {-1, 0}
     * 
     * @param obsIdentifier Observation identifier.
     * @param c A SQL connection.
     *
     * @return A int array with PID and number of measure table.
     * @throws SQLException id The sql query fails.
     */
    protected int[] getPIDFromObservation(final String obsIdentifier, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"pid\", \"nb_table\" FROM \"" + schemaPrefix + "om\".\"observations\", \"" + schemaPrefix + "om\".\"procedures\" p WHERE \"identifier\"=? AND \"procedure\"=p.\"id\"")) {//NOSONAR
            stmt.setString(1, obsIdentifier);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new int[] {rs.getInt(1), rs.getInt(2)};
                }
                return new int[] {-1, 0};
            }
        }
    }

    /**
     * Return the PID (internal int procedure identifier) and the number of measure table associated for the specified procedure.
     *
     * z
     *
     * @param procedure Procedure identifier.
     * @param c A SQL connection.
     *
     * @return A int array with PID and number of measure table.
     */
    protected int[] getPIDFromProcedure(final String procedure, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"pid\", \"nb_table\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, procedure);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new int[] {rs.getInt(1), rs.getInt(2)};
                }
                return new int[] {-1, 0};
            }
        }
    }
    
    protected String getProcedureOMType(final String procedure, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"om_type\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, procedure);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        }
    }

    protected String getProcedureFromObservation(final String obsIdentifier, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"procedure\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {//NOSONAR
            stmt.setString(1, obsIdentifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                String pid = null;
                if (rs.next()) {
                    pid = rs.getString(1);
                }
                return pid;
            }
        }
    }

    public Procedure getProcess(String id, final Connection c) throws SQLException {
        final Map<String, Object> properties = readProperties("procedures_properties", "id_procedure", id, c);
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, id);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Procedure(rs.getString("id"), rs.getString("name"), rs.getString("description"), properties);
                }
            }
        }
        return null;
    }

    public Procedure getProcessSafe(String identifier, final Connection c) throws RuntimeException {
        try {
            return getProcess(identifier, c);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected String getProcedureParent(final String procedureId, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"parent\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, procedureId);
            try(final ResultSet rs = stmt.executeQuery()) {
                String parent = null;
                if (rs.next()) {
                    parent = rs.getString(1);
                }
                return parent;
            }
        }
    }

    protected List<Element> buildResultQuality(Field parent, ResultSet rs) throws SQLException {
        List<Element> results = new ArrayList<>();
        if (parent.qualityFields != null) {
            for (Field field : parent.qualityFields) {
                String fieldName = parent.name + "_quality_" + field.name;
                Object value = null;
                if (rs != null) {
                    switch(field.type) {
                        case BOOLEAN: value = rs.getBoolean(fieldName);break;
                        case QUANTITY: value = rs.getDouble(fieldName);break;
                        case TIME: value = rs.getTimestamp(fieldName);break;
                        case TEXT:
                        default: value = rs.getString(fieldName);
                    }
                    
                }
                try {
                    results.add(OMUtils.createQualityElement2(field, value));
                } catch (ReflectiveOperationException ex) {
                    throw new SQLException(ex);
                }
            }
        }
        return results;
    }
}
