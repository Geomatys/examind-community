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
import org.constellation.store.observation.db.model.OMSQLDialect;
import org.constellation.store.observation.db.model.InsertDbField;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.Version;
import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.store.observation.db.SOSDatabaseObservationStore.SQL_DIALECT;
import static org.constellation.store.observation.db.SOSDatabaseObservationStore.TIMESCALEDB_VERSION;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.*;
import static org.constellation.store.observation.db.model.OMSQLDialect.DUCKDB;
import org.constellation.store.observation.db.model.ProcedureInfo;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.MultiFilterSQLRequest;
import org.constellation.util.SQLResult;
import org.constellation.util.SingleFilterSQLRequest;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.OMUtils;
import static org.geotoolkit.observation.OMUtils.buildTime;
import static org.geotoolkit.observation.OMUtils.getOverlappingComposite;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import static org.geotoolkit.observation.model.FieldDataType.BOOLEAN;
import static org.geotoolkit.observation.model.FieldDataType.QUANTITY;
import static org.geotoolkit.observation.model.FieldDataType.TEXT;
import static org.geotoolkit.observation.model.FieldDataType.TIME;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.ResultMode;
import org.geotoolkit.observation.model.SamplingFeature;
import org.locationtech.jts.io.WKTReader;

import org.opengis.metadata.quality.Element;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.temporal.TemporalPrimitive;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2BaseReader {

    protected final OMSQLDialect dialect;
    
    protected final DataSource source;

    protected final boolean timescaleDB;

    protected final Version timescaleDBVersion;

    protected final String decimationAlgorithm;

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
    
    protected final boolean spatialOperatorsEnable;

    public OM2BaseReader(final Map<String, Object> properties, final DataSource source, final boolean cacheEnabled) throws DataStoreException {
        this.dialect                   = (OMSQLDialect) properties.getOrDefault(SQL_DIALECT, null);
        this.timescaleDBVersion        = (Version) properties.getOrDefault(TIMESCALEDB_VERSION, null);
        this.timescaleDB               = timescaleDBVersion != null;
        this.decimationAlgorithm       = (String) properties.getOrDefault(DECIMATION_ALGORITHM_NAME, "");
        this.phenomenonIdBase          = (String) properties.getOrDefault(PHENOMENON_ID_BASE_NAME, "");
        this.sensorIdBase              = (String) properties.getOrDefault(SENSOR_ID_BASE_NAME, "");
        this.observationTemplateIdBase = (String) properties.getOrDefault(OBSERVATION_TEMPLATE_ID_BASE_NAME, "urn:observation:template:");
        this.observationIdBase         = (String) properties.getOrDefault(OBSERVATION_ID_BASE_NAME, "");
        this.schemaPrefix              = (String)  properties.getOrDefault(SCHEMA_PREFIX_NAME, "");
        this.cacheEnabled              = cacheEnabled;
        this.spatialOperatorsEnable    = dialect.equals(OMSQLDialect.DUCKDB) || dialect.equals(OMSQLDialect.POSTGRES);
        this.source                    = source;
        try {
            // try if the connection is valid
            try(final Connection c = this.source.getConnection()) {}
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
    }

    public OM2BaseReader(final OM2BaseReader that) {
        this.phenomenonIdBase          = that.phenomenonIdBase;
        this.observationTemplateIdBase = that.observationTemplateIdBase;
        this.sensorIdBase              = that.sensorIdBase;
        this.dialect                   = that.dialect;
        this.observationIdBase         = that.observationIdBase;
        this.schemaPrefix              = that.schemaPrefix;
        this.cacheEnabled              = that.cacheEnabled;
        this.timescaleDB               = that.timescaleDB;
        this.timescaleDBVersion        = that.timescaleDBVersion;
        this.decimationAlgorithm       = that.decimationAlgorithm;
        this.spatialOperatorsEnable    = that.spatialOperatorsEnable;
        this.source                    = that.source;
    }

    /**
     * use for debugging purpose
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");

    /**
     * Extract a Feature of interest from its identifier.
     * If cache is enabled, it can be returned from it if already read.
     *
     * @param id FOI identifier.
     * @param c A SQL connection.
     *
     * @return
     * @throws SQLException If an error occurs during query execution.
     * @throws DataStoreException If an error occurs during geometry instanciation.
     */
    protected SamplingFeature getFeatureOfInterest(final String id, final Connection c) throws SQLException, DataStoreException {
        if (cacheEnabled && cachedFoi.containsKey(id)) {
            return cachedFoi.get(id);
        }
        try {
            final String name;
            final String description;
            final String sampledFeature;
            final org.locationtech.jts.geom.Geometry geom;
            final int srid;
            final Map<String, Object> properties = readProperties("sampling_features_properties", "id_sampling_feature", id, c);
            final String query = switch(dialect) {
                case POSTGRES -> "SELECT \"id\", \"name\", \"description\", \"sampledfeature\", st_asBinary(\"shape\") as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?";
                case DUCKDB   -> "SELECT \"id\", \"name\", \"description\", \"sampledfeature\", ST_AsText(\"shape\") as \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?";
                case DERBY    -> "SELECT * FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?";
            };
            try (final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
                stmt.setString(1, id);
                try (final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        name = rs.getString("name");
                        description = rs.getString("description");
                        sampledFeature = rs.getString("sampledfeature");
                        geom = readGeom(rs, "shape");
                        srid = rs.getInt("crs");
                    } else {
                        return null;
                    }
                }
                if (geom != null) {
                    final CoordinateReferenceSystem crs = OM2Utils.parsePostgisCRS(srid);
                    JTS.setCRS(geom, crs);
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

    protected org.locationtech.jts.geom.Geometry readGeom(SQLResult rs, String colName) throws SQLException, ParseException {
        org.locationtech.jts.geom.Geometry geom = null;
        // duck db driver does not support a lot of bytes handling. TODO, look for future driver improvement
        if (dialect.equals(OMSQLDialect.DUCKDB)) {
           String s = rs.getString(colName);
            if (s != null) {
                WKTReader reader = new WKTReader();
                geom = reader.read(s);
            }
        } else {
            final byte[] b = rs.getBytes(colName);
            if (b != null) {
                WKBReader reader = new WKBReader();
                geom = reader.read(b);
            }
        }
        return geom;
    }


     protected org.locationtech.jts.geom.Geometry readGeom(ResultSet rs, String colName) throws SQLException, ParseException {
        org.locationtech.jts.geom.Geometry geom = null;
        // duck db driver does not support a lot of bytes handling. TODO, look for future driver improvement
        if (dialect.equals(OMSQLDialect.DUCKDB)) {
           String s = rs.getString(colName);
            if (s != null) {
                WKTReader reader = new WKTReader();
                geom = reader.read(s);
            }
        } else {
            final byte[] b = rs.getBytes(colName);
            if (b != null) {
                WKBReader reader = new WKBReader();
                geom = reader.read(b);
            }
        }
        return geom;
    }

    /**
     * Read specified entity properties.
     *
     * @param tableName Properties table name, depending on the type of the entity.
     * @param columnName Column name of the entity identifier depending on the type of the entity.
     * @param id Entity identifier.
     * @param c A SQL connection.
     *
     * @return A Map of properties.
     * @throws SQLException If an error occurs during query execution.
     */
    protected Map<String, Object> readProperties(String tableName, String columnName, String id, Connection c) throws SQLException {
        String request = "SELECT \"property_name\", \"value\" FROM \"" + schemaPrefix + "om\".\"" + tableName + "\" WHERE \"" + columnName + "\"=? ORDER BY \"property_name\", \"value\"";
        LOGGER.fine(request);
        Map<String, Object> results = new LinkedHashMap<>();
        try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, id);
            try (final ResultSet rs   = stmt.executeQuery()) {
                while (rs.next()) {
                    String pName = rs.getString("property_name");
                    String pValue = rs.getString("value");
                    Object prev = results.get(pName);
                    if (prev instanceof List ls) {
                        ls.add(pValue);
                    // transform single value into a list
                    } else if (prev != null) {
                        List ls = new ArrayList<>();
                        ls.add(prev);
                        ls.add(pValue);
                        results.put(pName, ls);
                    } else {
                        results.put(pName, pValue);
                    }
                }
            }
        }
        return results;
    }
    
    protected Object readProperty(String tableName, String columnName, String propertyName, String id, Connection c) throws SQLException {
        String request = "SELECT \"value\" FROM \"" + schemaPrefix + "om\".\"" + tableName + "\" WHERE \"" + columnName + "\" = ? AND \"property_name\" = ? ORDER BY \"value\"";
        LOGGER.fine(request);
        Object result = null;
        try(final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, id);
            stmt.setString(2, propertyName);
            try (final ResultSet rs   = stmt.executeQuery()) {
                while (rs.next()) {
                    String pValue = rs.getString("value");
                    if (result instanceof List ls) {
                        ls.add(pValue);
                    // transform single value into a list
                    } else if (result != null) {
                        List ls = new ArrayList<>();
                        ls.add(result);
                        ls.add(pValue);
                        result = ls;
                    } else {
                        result = pValue;
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("squid:S2695")
    protected TemporalPrimitive getTimeForTemplate(Connection c, String procedure, String observedProperty, String foi) {
        String request = "SELECT min(\"time_begin\"), max(\"time_begin\"), max(\"time_end\") FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?";
        if (observedProperty != null) {
             request = request + " AND (\"observed_property\"=? OR \"observed_property\" IN (SELECT DISTINCT(\"phenomenon\") FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"component\"=?))";
        }
        if (foi != null) {
            request = request + " AND \"foi\"=?";
        }
        LOGGER.fine(request);
        String procedureId = procedure;
        if (procedureId.startsWith(sensorIdBase)) {
            procedureId = procedureId.substring(sensorIdBase.length());
        }
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
                        return buildTime(procedureId, minBegin, maxEnd);
                    } else if (minBegin != null && !minBegin.equals(maxBegin)) {
                        return buildTime(procedureId, minBegin, maxBegin);
                    } else if (minBegin != null) {
                        return buildTime(procedureId, minBegin, null);
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

    protected Phenomenon getPhenomenonSafe(String identifier, final Connection c) throws RuntimeException {
        try {
            return getPhenomenon(identifier, c);
        } catch (DataStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Read a phenomenon in the datasource.
     *
     * @param identifier Phenomenon identifier.
     * @param c SQL connection.
     *
     * @return A phenomenon or {@code null} if it does not exist.
     */
    protected Phenomenon getPhenomenon(final String identifier, final Connection c) throws DataStoreException {
        final String id;
        // cleanup phenomenon id because of its XML ype (NCName)
        if (identifier.startsWith(phenomenonIdBase)) {
            id = identifier.substring(phenomenonIdBase.length()).replace(':', '-');
        } else {
            id = identifier.replace(':', '-');
        }
        if (cacheEnabled && cachedPhenomenon.containsKey(id)) {
            return cachedPhenomenon.get(id);
        }
        try {
            // look for composite phenomenon
            try (final PreparedStatement stmt = c.prepareStatement("SELECT \"component\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"phenomenon\"=? ORDER BY \"order\" ASC")) {//NOSONAR
                stmt.setString(1, identifier);
                try(final ResultSet rs = stmt.executeQuery()) {
                    final List<Phenomenon> components = new ArrayList<>();
                    while (rs.next()) {
                        final String phenID = rs.getString(1);
                        components.add(getSinglePhenomenon(phenID, c));
                    }
                    Phenomenon base = getSinglePhenomenon(id, c);
                    Phenomenon result = null;
                    if (base != null) {
                        if (components.isEmpty()) {
                            result = base;
                        } else {
                            result = new CompositePhenomenon(id, base.getName(), base.getDefinition(), base.getDescription(), base.getProperties(), components);
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

     protected Phenomenon getPhenomenonForFields(final List<Field> fields, final Connection c) throws DataStoreException {
         FilterSQLRequest request = new SingleFilterSQLRequest("SELECT \"phenomenon\", COUNT(\"component\") FROM \"" + schemaPrefix + "om\".\"components\" ");
         request.append(" WHERE \"component\" IN (");
         request.appendValues(fields.stream().map(f -> f.name).toList());
         request.append(" ) AND \"phenomenon\" NOT IN (");
         request.append("SELECT DISTINCT(cc.\"phenomenon\") FROM \"" + schemaPrefix + "om\".\"components\" cc WHERE ");
         for (Field field : fields) {
             request.append(" \"component\" <> ").appendValue(field.name).append(" AND ");
         }
         request.deleteLastChar(4);
         request.append(" ) ");
         request.append(" GROUP BY \"phenomenon\" HAVING COUNT(\"component\") = ").append(Integer.toString(fields.size()));

         String result = null;
         try (final SQLResult rs = request.execute(c)) {
             if (rs.next()) {
                 result = rs.getString(1);
             }
         } catch (SQLException ex) {
             throw new DataStoreException(ex.getMessage(), ex);
         }
         return (result != null) ? getPhenomenon(result, c) : null;
     }

    protected Phenomenon getSinglePhenomenon(final String id, final Connection c) throws DataStoreException {
        if (cacheEnabled && cachedPhenomenon.containsKey(id)) {
            return cachedPhenomenon.get(id);
        }
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
                        // multiple components are present, but no composite... TODO
                        return getVirtualCompositePhenomenon(c, procedure);
                    }
                } else if (composites.size() == 1) {
                    return composites.get(0);
                } else  {
                    // multiple composite components are present, we must choose the global one or create a virtual
                    Optional<CompositePhenomenon> phen = getOverlappingComposite(composites);
                    if (phen.isPresent()) {
                        return phen.get();
                    }
                    return getVirtualCompositePhenomenon(c, procedure);
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

    protected Phenomenon createCompositePhenomenonFromField(Connection c, List<Field> fields) throws DataStoreException {
        if (fields == null || fields.size() < 2) {
            throw new DataStoreException("at least two fields are required for building composite phenomenon");
        }
        final List<Phenomenon> components = new ArrayList<>();
        for (Field field : fields) {
            final Phenomenon phen = getPhenomenon(field.name, c);
            if (phen == null) {
                throw new DataStoreException("Unable to link a field to a phenomenon: " + field.name);
            }
            components.add(phen);
        }
        final String name = "computed-phen-" + UUID.randomUUID().toString();
        return new CompositePhenomenon(name, name, name, null, null, components);
    }

    protected Offering readObservationOffering(final String offeringId, final Connection c) throws DataStoreException {
        final String id;
        final String name;
        final String description;
        final TemporalPrimitive time;
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

    protected List<Field> readFields(final String procedureID, final Connection c) {
        return readFields(procedureID, false, c, new ArrayList<>(), new ArrayList<>());
    }

    protected List<Field> readFields(final String procedureID, final boolean removeMainTimeField, final Connection c, List<Integer> fieldIndexFilters, List<String> fieldIdFilters) {
        final List<Field> results = new ArrayList<>();
        StringBuilder query = new StringBuilder("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"parent\" IS NULL");
        if (removeMainTimeField) {
            query.append(" AND NOT(\"order\"= 1 AND \"field_type\"= 'Time')");
        }
        if (fieldIndexFilters != null && !fieldIndexFilters.isEmpty()) {
            query.append(" AND (");
            for (Integer fieldFilter : fieldIndexFilters) {
                query.append("\"order\"= ").append(fieldFilter).append(" OR ");
            }
            if (!removeMainTimeField) {
                query.append("(\"order\"= 1 AND \"field_type\"= 'Time'))");
            } else {
                query.delete(query.length() - 4, query.length());
                query.append(")");
            }
        }
        if (fieldIdFilters != null && !fieldIdFilters.isEmpty()) {
            query.append(" AND (");
            for (String fieldFilter : fieldIdFilters) {
                query.append("\"field_name\"= '").append(fieldFilter).append("' OR ");
            }
            // main field name may vary
            if (!removeMainTimeField) {
                query.append("\"order\"= 1)");
            } else {
                query.delete(query.length() - 4, query.length());
                query.append(")");
            }
        }
        query.append(" ORDER BY \"order\"");
        try(final PreparedStatement stmt = c.prepareStatement(query.toString())) {//NOSONAR
            stmt.setString(1, procedureID);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(getFieldFromDb(rs, procedureID, c, true));
                }
                return results;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected List<InsertDbField> completeDbField(final String procedureID, final List<Field> inputFields, final Connection c) throws SQLException {
        List<InsertDbField> results = new ArrayList<>();
        for (Field inputField : inputFields) {
            results.add(completeDbField(procedureID, inputField, c));
        }
        return results;
    }

    protected InsertDbField completeDbField(final String procedureID, final Field inputField, final Connection c) throws SQLException {
        final String query = "SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"parent\" IS NULL AND \"field_name\" = ?";
        try(final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setString(2, inputField.name);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    DbField dbField = getFieldFromDb(rs, procedureID, c, true);
                    InsertDbField result = new InsertDbField(dbField);
                    result.setInputUom(inputField.uom);
                    return result;
                }
            }
        }
        throw new SQLException("No field " + inputField.name + " found for procedure:" + procedureID);
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

    /**
     * Return the field identified by its index for the specified procedure.
     * 
     * @param procedureID procedure identifier.
     * @param index Field index
     * @param fetchExtraFields if set to {@code true} the quality and parameter fields will be fetched.
     * @param c SQL connection.
     * @return A field or {@code null} if the field or procedure can not be found.
     * @throws SQLException 
     */
    protected DbField getFieldByIndex(final String procedureID, final int index, final boolean fetchExtraFields, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"order\"=?  AND \"parent\" IS NULL")) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setInt(2, index);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getFieldFromDb(rs, procedureID, c, fetchExtraFields);
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

    protected Field getProcedureField(final String procedureID, final String fieldName, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND (\"field_name\"= ?) AND \"parent\" IS NULL")) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setString(2, fieldName);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return getFieldFromDb(rs, procedureID, c, true);
                }
                return null;
            }
        }
    }

    protected DbField getFieldFromDb(final ResultSet rs, String procedureID, Connection c, boolean fetchExtraFields) throws SQLException {
        final String fieldName = rs.getString("field_name");
        List<Field> parameterFields = new ArrayList<>();
        List<Field> qualityFields = new ArrayList<>();
        if (fetchExtraFields) {
            try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"parent\"=? ORDER BY \"order\"")) {//NOSONAR
                stmt.setString(1, procedureID);
                stmt.setString(2, fieldName);
                try(final ResultSet rss = stmt.executeQuery()) {
                    while (rss.next()) {
                        String subFieldType = rss.getString("sub_field_type");
                        if ("PARAMETER".equals(subFieldType)) {
                            parameterFields.add(getFieldFromDb(rss, procedureID, c, false));
                            
                        // fall back to quality for retro-compatibility
                        } else {
                            qualityFields.add(getFieldFromDb(rss, procedureID, c, false));
                        }
                    }
                }
            }
        }
        
        int order = rs.getInt("order");
        String fieldType = rs.getString("sub_field_type");
        FieldType ft;
         if (fieldType != null) {
            ft = FieldType.valueOf(fieldType);
        } else if (order == 1) {
            ft = FieldType.MAIN;
        } else{
            ft = FieldType.MEASURE;
        }
        final DbField f = new DbField(
                         order,
                         FieldDataType.fromLabel(rs.getString("field_type")),
                         fieldName,
                         rs.getString("label"),
                         rs.getString("field_definition"),
                         rs.getString("uom"),
                         ft,
                         rs.getInt("table_number"),
                         qualityFields,
                         parameterFields);
        return f;
    }

    /**
     * Return the information about the procedure:  PID (internal int procedure identifier) , the number of measure table, ... associated for the specified observation.
     * If there is no procedure for the specified procedure id, this method will return PID = -1, nb table = 0 (for backward compatibility).
     *
     * @param obsIdentifier Observation identifier.
     * @param c A SQL connection.
     *
     * @return Information about the procedure such as PID and number of measure table.
     * @throws SQLException id The sql query fails.
     */
    protected Optional<ProcedureInfo> getPIDFromObservation(final String obsIdentifier, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT p.\"pid\", p.\"nb_table\", p.\"id\", p.\"om_type\", p.\"name\" FROM \"" + schemaPrefix + "om\".\"observations\", \"" + schemaPrefix + "om\".\"procedures\" p WHERE \"identifier\"=? AND \"procedure\"=p.\"id\"")) {//NOSONAR
            stmt.setString(1, obsIdentifier);
            return extractPID(stmt, c);
        }
    }

    /**
     * Return  the information about the procedure: PID (internal int procedure identifier) and the number of measure table associated for the specified observation.
     * If there is no procedure for the specified procedure id, thsi method will return {-1, 0}
     *
     * @param oid Observation id.
     * @param c A SQL connection.
     *
     * @return A int array with PID and number of measure table.
     * @throws SQLException id The sql query fails.
     */
    protected Optional<ProcedureInfo> getPIDFromOID(final int oid, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT p.\"pid\", p.\"nb_table\", p.\"id\", p.\"om_type\", p.\"name\", p.\"description\" FROM \"" + schemaPrefix + "om\".\"observations\" o, \"" + schemaPrefix + "om\".\"procedures\" p WHERE o.\"id\"=? AND \"procedure\"=p.\"id\"")) {//NOSONAR
            stmt.setInt(1, oid);
            return extractPID(stmt, c);
        }
    }
    
    private Optional<ProcedureInfo> extractPID(final PreparedStatement stmt, Connection c) throws SQLException {
        try (final ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
               final String procedureId = rs.getString(3);
               final Field mainField = getMainField(procedureId, c);
               return Optional.of(new ProcedureInfo(rs.getInt(1), rs.getInt(2), procedureId, rs.getString(5), rs.getString(6), rs.getString(4), mainField));
            }
            return Optional.empty();
        }
    }

    /**
     * Return  the information about the id: PID (internal int id identifier) and the number of measure table associated for the specified id.
     *
     * @param procedureId Procedure identifier.
     * @param c A SQL connection.
     *
     * @return Information about the id such as PID and number of measure table.
     */
    protected Optional<ProcedureInfo> getPIDFromProcedure(final String procedureId, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"pid\", \"nb_table\", \"id\", \"om_type\", \"name\", \"description\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, procedureId);
            return extractPID(stmt, c);
        }
    }
    
    protected Optional<ProcedureInfo> getPIDFromProcedureSafe(final String procedureId, final Connection c) throws RuntimeException {
        try {
            return getPIDFromProcedure(procedureId, c);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to fetch procedure attributes from ID: " + procedureId, ex);
        }
    }

    /**
     * Return the observation internal id for the specified observation identifier.
     *
     * @param identifier  observation identifier.
     * @param c An SQL connection.
     *
     * @return The observation internal id or -1 if the observation does not exist.
     * @throws SQLException
     */
    protected int getOIDFromIdentifier(final String identifier, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {//NOSONAR
            stmt.setString(1, identifier);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return -1;
            }
        }
    }

    /**
     * Return the number of registred fields in the measure table identified by its procedure and table number.
     *
     * @param procedure Procedure identifier.
     * @param tableNum measure table number.
     * @param c A SQL connection.
     *
     * @return The number of fields (meaning SQL column) of the specified measure table for the specified procedure.
     * If the procedure or the the table number does not exist it will return 0.
     */
    protected int getNbFieldInTable(final String procedure, final int tableNum, final Connection c) throws SQLException {
        try (final PreparedStatement stmt = c.prepareStatement("SELECT COUNT(*) FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"table_number\"=?")) {//NOSONAR
            stmt.setString(1, procedure);
            stmt.setInt(2, tableNum);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new IllegalStateException("Unexpected no results");
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

    public Procedure getProcess(String id, final Connection c) throws SQLException {
        final Map<String, Object> properties = readProperties("procedures_properties", "id_procedure", id, c);
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, id);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String omType = rs.getString("om_type");
                    if (omType != null) {
                        properties.putIfAbsent("type", omType);
                    }
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
    
    protected List<Element> buildResultQuality(Field parent, SQLResult rs) throws SQLException {
        List<Element> results = new ArrayList<>();
        if (parent.qualityFields != null) {
            for (Field field : parent.qualityFields) {
                int rsIndex = ((DbField)field).tableNumber;
                String fieldName = parent.name + "_quality_" + field.name;
                Object value = null;
                if (rs != null) {
                    switch(field.dataType) {
                        case BOOLEAN: value = rs.getBoolean(fieldName, rsIndex);break;
                        case QUANTITY: value = rs.getDouble(fieldName, rsIndex);break;
                        case TIME: value = rs.getTimestamp(fieldName, rsIndex);break;
                        case TEXT:
                        default: value = rs.getString(fieldName, rsIndex);
                    }

                }
                results.add(OMUtils.createQualityElement(field, value));
            }
        }
        return results;
    }
    
    protected Map<String, Object> buildParameters(Field parent, SQLResult rs) throws SQLException {
        Map<String, Object> results = new HashMap<>();
        if (parent.parameterFields != null) {
            for (Field field : parent.parameterFields) {
                int rsIndex = ((DbField)field).tableNumber;
                String fieldName = parent.name + "_parameter_" + field.name;
                Object value = null;
                if (rs != null) {
                    switch(field.dataType) {
                        case BOOLEAN: value = rs.getBoolean(fieldName, rsIndex);break;
                        case QUANTITY: value = rs.getDouble(fieldName, rsIndex);break;
                        case TIME: value = rs.getTimestamp(fieldName, rsIndex);break;
                        case JSON: value = OMUtils.readJsonMap(rs.getString(fieldName, rsIndex));break;
                        case TEXT:
                        default: value = rs.getString(fieldName, rsIndex);
                    }

                }
                results.put(field.name, value);
            }
        }
        return results;
    }

    protected int getNbMeasureForProcedure(int pid, Connection c) {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT COUNT(\"id\") FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\"");
            final ResultSet rs = stmt.executeQuery()) {//NOSONAR
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            // we catch it because the table can not exist.
            LOGGER.log(Level.FINE, "Error while looking for measure count", ex);
        }
        return 0;
    }

    protected int getNbMeasureForObservation(int pid, int oid, Connection c) {
        try (final PreparedStatement stmt = c.prepareStatement("SELECT COUNT(\"id\") FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" WHERE \"id_observation\" = ?")) {
            stmt.setInt(1, oid);
            try (final ResultSet rs = stmt.executeQuery()) {//NOSONAR
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            // we catch it because the table can not exist.
            LOGGER.log(Level.FINE, "Error while looking for measure count", ex);
        }
        return -1;
    }

    protected Result getResult(final ProcedureInfo ti, int oid , final QName resultModel, final Integer measureId, final Field selectedField, final Connection c) throws DataStoreException, SQLException {
        if (resultModel.equals(MEASUREMENT_QNAME)) {
            return buildMeasureResult(ti, oid, measureId, selectedField, c);
        } else {
            return buildComplexResult(ti, oid, measureId, c);
        }
    }

    private ComplexResult buildComplexResult(final ProcedureInfo ti, final long oid, final Integer measureId, final Connection c) throws DataStoreException, SQLException {

        final List<Field> fields    = readFields(ti.id, false, c, new ArrayList<>(), new ArrayList<>());

        FilterSQLRequest measureFilter = null;
        if (measureId != null) {
            measureFilter = new SingleFilterSQLRequest(" AND m.\"id\" = ").appendValue(measureId);
        }
        final MultiFilterSQLRequest queries = buildMesureRequests(ti, fields, measureFilter,  oid, false, true, false, false, false);
        final FieldParser parser            = new FieldParser(ti.mainField.index, fields, ResultMode.CSV, false, false, true, true, null, 0);
        try (SQLResult rs = queries.execute(c)) {
            while (rs.next()) {
                parser.parseLine(rs);
            }
            return parser.buildComplexResult();
        }
    }

    private MeasureResult buildMeasureResult(final ProcedureInfo ti, final int oid, final Integer measureId, final Field selectedField, final Connection c) throws DataStoreException, SQLException {
        if (selectedField == null) {
            throw new DataStoreException("Measurement extraction need a field index specified");
        }
        if (measureId == null) {
            throw new DataStoreException("Measurement extraction need a measure id specified");
        }

        final FieldDataType fType  = selectedField.dataType;
        String tableName = "mesure" + ti.pid;
        int tn = ((DbField) selectedField).tableNumber;
        if (tn > 1) {
            tableName = tableName + "_" + tn;
        }

        String query = "SELECT \"" + selectedField.name + "\" FROM  \"" + schemaPrefix + "mesures\".\"" + tableName + "\" m " +
                       "WHERE \"id_observation\" = ? " +
                       "AND m.\"id\" = ? ";

        try(final PreparedStatement stmt  = c.prepareStatement(query)) {//NOSONAR
            stmt.setInt(1, oid);
            stmt.setInt(2, measureId);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Object value;
                    switch (fType) {
                        case QUANTITY: value = rs.getDouble(selectedField.name);break;
                        case BOOLEAN: value = rs.getBoolean(selectedField.name);break;
                        case TIME: value = new Date(rs.getTimestamp(selectedField.name).getTime());break;
                        case TEXT:
                        default: value = rs.getString(selectedField.name);break;
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
    
    private static Map<Integer, List<DbField>> extractTableFields(String mainFieldName, List<Field> queryFields) {
        final Map<Integer, List<DbField>> results = new HashMap<>();
        
        // add a special where the only field requested is the main.
        if (queryFields.size() == 1 && queryFields.get(0).name.equals(mainFieldName)) {
            if (queryFields.get(0) instanceof DbField df) {
                return Map.of(df.tableNumber, List.of(df));
            }  else {
                throw new IllegalStateException("Unexpected field implementation: " + queryFields.get(0).getClass().getName());
            } 
        }
        
        for (Field f : queryFields) {
            if (f instanceof DbField df) {
                // index 0 are non measure fields
                if (df.index != 0 && !df.name.equals(mainFieldName)) {
                    List<DbField> fields = results.computeIfAbsent(df.tableNumber, tn -> new ArrayList<>());
                    fields.add(df);
                }
            } else {
                throw new IllegalStateException("Unexpected field implementation: " + f.getClass().getName());
            }
        }
        return results;
    }

    /**
     * Build one or more SQL request on the measure(s) tables.
     * The point of this mecanism is to bypass the issue of selecting more than 1664 columns in a select.
     * The measure table columns are already multiple to avoid the probleme of limited columns in a table,
     * so we build a select request for eache measure table, join with the main field of the primary table.
     *
     * @param pti Informations abut the measure tables.
     * @param queryFields List of measure fields we want to read (id or main fields will be excluded).
     * @param measureFilter Piece of SQL to apply to all the measure query. (can be null)
     * @param oid An Observation id used to filter the measure. (can be null)
     * @param obsJoin If true, a join with the observation table will be applied.
     * @param addOrderBy If true, an order by main filed will be applied.
     * @param idOnly If true, only the measure identifier will be selected.
     * @param count
     * @param decimate Indicate if we are in a decimation context.
     * 
     * @return A Multi filter request on measure tables.
     */
    protected MultiFilterSQLRequest buildMesureRequests(ProcedureInfo pti, List<Field> queryFields, FilterSQLRequest measureFilter, Long oid, boolean obsJoin, boolean addOrderBy, boolean idOnly, boolean count, boolean decimate) {
        final boolean profile = "profile".equals(pti.type);
        final String mainFieldName = pti.mainField.name;
        final MultiFilterSQLRequest measureRequests = new MultiFilterSQLRequest();
        final Map<Integer, List<DbField>> queryTableFields = extractTableFields(mainFieldName, queryFields);
        
        final boolean multiTable = pti.nbTable > 1;
        
        
        for (int tableNum = 1; tableNum < pti.nbTable + 1; tableNum++) {
            
            // skip the table if none query fields are requested (multi table procedure only) (what about filters???)
            if (!queryTableFields.containsKey(tableNum) && pti.nbTable > 1) continue;
            
            // this can be null
            List<DbField> tableFields = queryTableFields.get(tableNum);
            
            String baseTableName = "mesure" + pti.pid;
            final SingleFilterSQLRequest measureRequest;
            AtomicBoolean whereSet = new AtomicBoolean(false);
            
            // first main table
            if (tableNum == 1) {
                String select;
                if (idOnly) {
                    select = "m.\"id\"";
                } else {
                    // add always id and main field
                    select = "m.\"id\", m.\"" + mainFieldName + "\"";
                    if (tableFields != null) {
                        for (DbField df : tableFields) {
                            if (!df.name.equals(mainFieldName)) {
                                select = select + ", m.\"" + df.name + "\"";

                                // add also quality fields (TODO disable for decimation ?)
                                for (Field qf : df.qualityFields) {
                                    select = select + ", m.\"" + df.name + "_quality_" + qf.name + "\"";
                                }
                                for (Field pf : df.parameterFields) {
                                    select = select + ", m.\"" + df.name + "_parameter_" + pf.name + "\"";
                                }
                            }
                        }
                    }
                }
                measureRequest = new SingleFilterSQLRequest("SELECT ");
                if (count) {
                    measureRequest.append("COUNT(").append(select).append(")");
                } else {
                    measureRequest.append(select);
                }
                measureRequest.append(" FROM \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" m");
                if (obsJoin) {
                    measureRequest.append(",\"" + schemaPrefix + "om\".\"observations\" o ");
                }
                if (oid != null) {
                    measureRequest.append(" WHERE m.\"id_observation\" = ").appendValue(oid);
                    whereSet.set(true);
                }
                if (obsJoin) {
                    String where = whereSet.get() ? " AND " : " WHERE ";
                    measureRequest.append(where).append(" o.\"id\" = m.\"id_observation\" ");
                    whereSet.set(true);
                }
                
                /*
                 * append filter on null values
                 *  
                 * Deactivated on multi- table for now.
                 * TODO maybe add the main field on each extra table to solve this problem.
                 */
                if (!multiTable && tableFields != null) {
                    appendNotNullFilter(tableFields, mainFieldName, whereSet, measureRequest);
                }
                        
            // other tables
            } else {
                String tableName = baseTableName + "_" + tableNum;
                String select;
                if (idOnly) {
                    select = "m2.\"id\"";
                } else {
                    // add always id and main field
                    select = "m.\"id\", m.\"" + pti.mainField.name + "\"";
                    for (DbField df : queryTableFields.get(tableNum)) {
                        select = select + ", m2.\"" + df.name + "\"";

                        // add also quality fields (TODO disable for decimation ?)
                        for (Field qf : df.qualityFields) {
                            select = select + ", m2.\"" + df.name + "_quality_" + qf.name + "\"";
                        }
                        for (Field pf : df.parameterFields) {
                            select = select + ", m2.\"" + df.name + "_parameter_" + pf.name + "\"";
                        }
                    }
                }
                measureRequest = new SingleFilterSQLRequest("SELECT ");
                if (count) {
                    measureRequest.append("COUNT(").append(select).append(")");
                } else {
                    measureRequest.append(select);
                }
                measureRequest.append(" FROM \"" + schemaPrefix + "mesures\".\"" + tableName + "\" m2, \"" + schemaPrefix + "mesures\".\"" + baseTableName + "\" m");
                if (obsJoin) {
                    measureRequest.append(",\"" + schemaPrefix + "om\".\"observations\" o ");
                }
                measureRequest.append(" WHERE (m.\"id\" = m2.\"id\" AND  m.\"id_observation\" = m2.\"id_observation\") ");
                whereSet.set(true);
                if (oid != null) {
                    measureRequest.append(" AND m2.\"id_observation\" = ").appendValue(oid);
                }
                if (obsJoin) {
                    measureRequest.append(" AND o.\"id\" = m2.\"id_observation\" ");
                }
                
               /*
                 * append filter on null values
                 *  
                 * Deactivated on multi- table for now.
                 * TODO maybe add the main field on each extra table to solve this problem.
                 */
                if (!multiTable && tableFields != null) {
                    appendNotNullFilter(tableFields, mainFieldName, whereSet, measureRequest);
                }
            }
            
            /*
            * Append measure filter on each measure request
            */
            boolean includeConditional = !profile;
            boolean isEmpty = measureFilter == null || 
                             ((measureFilter instanceof MultiFilterSQLRequest mf) && mf.isEmpty(tableNum, includeConditional)) ||
                             measureFilter.isEmpty(includeConditional);
                    
            if (!isEmpty) {
                if (whereSet.get()) {
                    measureRequest.append(" AND ");
                } else {
                    measureRequest.append(" WHERE ");
                    whereSet.set(true);
                }
                
                FilterSQLRequest clone = measureFilter.clone();
                if (clone instanceof MultiFilterSQLRequest mf) {
                    measureRequest.append(mf.getRequest(tableNum), includeConditional);
                } else {
                    measureRequest.append(clone, includeConditional);
                }
            }
            
            /*
            * Append order by on main field
            */
            if (addOrderBy) {
                measureRequest.append(" ORDER BY ").append("m.\"" + pti.mainField.name + "\"");
            }
            
            measureRequests.addRequest(tableNum, measureRequest);
        }
        return measureRequests;
    }
    
    /**
     * Append NOT NULL filter for the table fields in the measure request .
     * 
     * @param tableFields Field list for a specific measure table.
     * @param mainFieldName Name of the main field (the filter will not apply to this field).
     * @param whereSet True if a WHERE has already be append to the sql query.
     * @param measureRequest Appendable measure query.
     */
    private void appendNotNullFilter(List<DbField> tableFields, String mainFieldName, AtomicBoolean whereSet, SingleFilterSQLRequest measureRequest) {
         boolean nullFilterApplied = false;

        // 1. we sort the field identified as measure field along their table number
        StringBuilder s = new StringBuilder("(");
        for (DbField df : tableFields) {
            // index 0 are non measure fields
            if (df.index != 0 && !df.name.equals(mainFieldName)) {
                s.append(" \"").append(df.name).append("\" IS NOT NULL OR ");
                nullFilterApplied = true;
            }
        }
        if (nullFilterApplied) {
            s.delete(s.length() - 3, s.length());
            s.append(")");
            String where = whereSet.get() ? " AND " : " WHERE ";
            measureRequest.append(where).append(s.toString());
            whereSet.set(true);
        }
    }
}
