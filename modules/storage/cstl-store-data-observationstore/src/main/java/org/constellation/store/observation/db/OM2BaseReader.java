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

import com.google.common.base.Objects;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.CommonConstants;
import org.constellation.util.Util;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.Envelope;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.xml.Process;
import org.geotoolkit.sos.xml.SOSXmlFactory;

import static org.geotoolkit.sos.xml.SOSXmlFactory.*;
import org.geotoolkit.swe.xml.AbstractBoolean;
import org.geotoolkit.swe.xml.AbstractDataComponent;
import org.geotoolkit.swe.xml.AbstractText;
import org.geotoolkit.swe.xml.AbstractTime;

import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.Quantity;
import org.geotoolkit.swe.xml.UomProperty;
import org.opengis.metadata.Identifier;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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
     * The is forged by version + feature ID.
     * This map is only populated if {@link OM2BaseReader#cacheEnabled} is set to true.
     */
    private final Map<String, SamplingFeature> cachedFoi = new HashMap<>();

    /**
     * A map of already read Phenomenon.
     * The is forged by version + pehnomenon ID.
     * This map is only populated if {@link OM2BaseReader#cacheEnabled} is set to true.
     */
    private final Map<String, Phenomenon> cachedPhenomenon = new HashMap<>();

    protected final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected final SimpleDateFormat format2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S");

    public OM2BaseReader(final Map<String, Object> properties, final String schemaPrefix, final boolean cacheEnabled, final boolean isPostgres, final boolean timescaleDB) throws DataStoreException {
        this.isPostgres = isPostgres;
        this.timescaleDB = timescaleDB;
        final String phenID = (String) properties.get(CommonConstants.PHENOMENON_ID_BASE);
        if (phenID == null) {
            this.phenomenonIdBase      = "";
        } else {
            this.phenomenonIdBase      = phenID;
        }
        final String sidBase = (String) properties.get(CommonConstants.SENSOR_ID_BASE);
        if (sidBase == null) {
            this.sensorIdBase = "";
        } else {
            this.sensorIdBase = sidBase;
        }
        this.observationTemplateIdBase = (String) properties.get(CommonConstants.OBSERVATION_TEMPLATE_ID_BASE);
        final String oidBase           = (String) properties.get(CommonConstants.OBSERVATION_ID_BASE);
        if (oidBase == null) {
            this.observationIdBase = "";
        } else {
            this.observationIdBase = oidBase;
        }
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
    protected static final Logger LOGGER = Logging.getLogger("org.constellation.sos");

    protected static final CoordinateReferenceSystem defaultCRS;
    static {
        CoordinateReferenceSystem candidate = null;
        try {
            candidate = CRS.forCode("EPSG:4326");
        } catch (FactoryException ex) {
            LOGGER.log(Level.SEVERE, "Error while retrieving default CRS 4326", ex);
        }
        defaultCRS = candidate;
    }

    protected SamplingFeature getFeatureOfInterest(final String id, final String version, final Connection c) throws SQLException, DataStoreException {
        final String key = version + id;
        if (cacheEnabled && cachedFoi.containsKey(key)) {
            return cachedFoi.get(key);
        }
        try {
            final String name;
            final String description;
            final String sampledFeature;
            final byte[] b;
            final int srid;
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
                } else {
                    geom = null;
                }
                final SamplingFeature sf = buildFoi(version, id, name, description, sampledFeature, geom, crs);
                if (cacheEnabled) {
                    cachedFoi.put(key, sf);
                }
                return sf;
            }

        } catch (ParseException | FactoryException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    protected SamplingFeature buildFoi(final String version, final String id, final String name, final String description, final String sampledFeature,
            final Geometry geom, final CoordinateReferenceSystem crs) throws FactoryException {

        final String gmlVersion = getGMLVersion(version);
        // sampled feature is mandatory (even if its null, we build a property)
        final FeatureProperty prop = buildFeatureProperty(version, sampledFeature);
        if (geom instanceof Point) {
            final org.geotoolkit.gml.xml.Point point = JTStoGeometry.toGML(gmlVersion, (Point)geom, crs);
            // little hack fo unit test
            //point.setSrsName(null);
            point.setId("pt-" + id);
            return buildSamplingPoint(version, id, name, description, prop, point);
        } else if (geom instanceof LineString) {
            final org.geotoolkit.gml.xml.LineString line = JTStoGeometry.toGML(gmlVersion, (LineString)geom, crs);
            line.emptySrsNameOnChild();
            line.setId("line-" + id);
            final Envelope bound = line.getBounds();
            return buildSamplingCurve(version, id, name, description, prop, line, null, null, bound);
        } else if (geom instanceof Polygon) {
            final org.geotoolkit.gml.xml.Polygon poly = JTStoGeometry.toGML(gmlVersion, (Polygon)geom, crs);
            poly.setId("polygon-" + id);
            return buildSamplingPolygon(version, id, name, description, prop, poly, null, null, null);
        } else if (geom != null) {
            LOGGER.log(Level.WARNING, "Unexpected geometry type:{0}", geom.getClass());
        }
        return buildSamplingFeature(version, id, name, description, prop);
    }

    protected Phenomenon getPhenomenon(final String version, final String observedProperty, final Connection c) throws DataStoreException {
        final String id;
        // cleanup phenomenon id because of its XML ype (NCName)
        if (observedProperty.startsWith(phenomenonIdBase)) {
            id = observedProperty.substring(phenomenonIdBase.length()).replace(':', '-');
        } else {
            id = observedProperty.replace(':', '-');
        }
        final String key = version + id;
        if (cacheEnabled && cachedPhenomenon.containsKey(key)) {
            return cachedPhenomenon.get(key);
        }
        try {
            // look for composite phenomenon
            try (final PreparedStatement stmt = c.prepareStatement("SELECT \"component\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"phenomenon\"=? ORDER BY \"order\" ASC")) {//NOSONAR
                stmt.setString(1, observedProperty);
                try(final ResultSet rs = stmt.executeQuery()) {
                    final List<Phenomenon> phenomenons = new ArrayList<>();
                    while (rs.next()) {
                        final String phenID = rs.getString(1);
                        phenomenons.add(getSinglePhenomenon(version, phenID, c));
                    }
                    org.geotoolkit.swe.xml.Phenomenon base = getSinglePhenomenon(version, observedProperty, c);
                    Phenomenon result = null;
                    if (base != null) {
                        if (phenomenons.isEmpty()) {
                            result = base;
                        } else {
                            Identifier identifier = base.getName();
                            String name = identifier.getCode();
                            String definition = identifier.getCode();
                            if (identifier.getDescription() != null) {
                                name = identifier.getDescription().toString();
                            }
                            result = buildCompositePhenomenon(version, id, name, definition, base.getDescription(), phenomenons);
                        }
                        if (cacheEnabled) {
                            cachedPhenomenon.put(key, result);
                        }
                    }
                    return result;
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private org.geotoolkit.swe.xml.Phenomenon getSinglePhenomenon(final String version, final String observedProperty, final Connection c) throws DataStoreException {
        try {
            // look for composite phenomenon
            try (final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"observed_properties\" WHERE \"id\"=?")) {//NOSONAR
                stmt.setString(1, observedProperty);
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
                            } else if (phenID.startsWith(phenomenonIdBase)) {
                                name = phenID.substring(phenomenonIdBase.length());
                            }
                        }
                        return buildPhenomenon(version, phenID, name, definition, description);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return null;
    }

    protected List<Field> readFields(final String procedureID, final Connection c) throws SQLException {
        final List<Field> results = new ArrayList<>();
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? ORDER BY \"order\"")) {//NOSONAR
            stmt.setString(1, procedureID);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Field(rs.getString("field_type"),
                            rs.getString("field_name"),
                            rs.getString("field_definition"),
                            rs.getString("uom")));
                }
                return results;
            }
        }
    }

    protected Field getTimeField(final String procedureID, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"field_type\"='Time' ORDER BY \"order\"")) {//NOSONAR
            stmt.setString(1, procedureID);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Field(rs.getString("field_type"),
                            rs.getString("field_name"),
                            rs.getString("field_definition"),
                            rs.getString("uom"));
                }
                return null;
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
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND \"order\"=1")) {//NOSONAR
            stmt.setString(1, procedureID);
            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Field(rs.getString("field_type"),
                            rs.getString("field_name"),
                            rs.getString("field_definition"),
                            rs.getString("uom"));
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
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND (\"field_name\"='lat' OR \"field_name\"='lon') ORDER BY \"order\" DESC")) {//NOSONAR
            stmt.setString(1, procedureID);
            try (final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Field(
                            rs.getString("field_type"),
                            rs.getString("field_name"),
                            rs.getString("field_definition"),
                            rs.getString("uom")));
                }
            }
        }
        return results;
    }

    protected Field getFieldForPhenomenon(final String procedureID, final String phenomenon, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=? AND (\"field_name\"= ?)")) {//NOSONAR
            stmt.setString(1, procedureID);
            stmt.setString(2, phenomenon);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Field(rs.getString("field_type"),
                            rs.getString("field_name"),
                            rs.getString("field_definition"),
                            rs.getString("uom"));
                }
                return null;
            }
        }
    }

    protected int getPIDFromObservation(final String obsIdentifier, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"pid\" FROM \"" + schemaPrefix + "om\".\"observations\", \"" + schemaPrefix + "om\".\"procedures\" p WHERE \"identifier\"=? AND \"procedure\"=p.\"id\"")) {//NOSONAR
            stmt.setString(1, obsIdentifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                int pid = -1;
                if (rs.next()) {
                    pid = rs.getInt(1);
                }
                return pid;
            }
        }
    }

    protected int getPIDFromProcedure(final String procedure, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT \"pid\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, procedure);
            try(final ResultSet rs = stmt.executeQuery()) {
                int pid = -1;
                if (rs.next()) {
                    pid = rs.getInt(1);
                }
                return pid;
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

    public org.opengis.observation.Process getProcess(String version, String identifier, final Connection c) throws SQLException {
        try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmt.setString(1, identifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                String pid = null;
                if (rs.next()) {
                    return SOSXmlFactory.buildProcess(version, rs.getString("id"), rs.getString("name"), rs.getString("description"));
                }
            }
        }
        return null;
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

    protected static class Field {

        public String fieldType;
        public String fieldName;
        public String fieldDesc;
        public String fieldUom;

        public Field(final String fieldType, final String fieldName, final String fieldDesc, final String fieldUom) {
            this.fieldDesc = fieldDesc;
            this.fieldName = fieldName;
            this.fieldType = fieldType;
            this.fieldUom  = fieldUom;
        }

        public Field(final String fieldName, final AbstractDataComponent value) throws SQLException {
            this.fieldName = fieldName;
            if (value instanceof Quantity) {
                final Quantity q = (Quantity) value;
                if (q.getUom() != null) {
                    this.fieldUom = q.getUom().getCode();
                }
                this.fieldDesc = q.getDefinition();
                this.fieldType = "Quantity";
            } else if (value instanceof AbstractText) {
                final AbstractText q = (AbstractText) value;
                this.fieldDesc = q.getDefinition();
                this.fieldType = "Text";
            } else if (value instanceof AbstractBoolean) {
                final AbstractBoolean q = (AbstractBoolean) value;
                this.fieldDesc =  q.getDefinition();
                this.fieldType = "Boolean";
            } else if (value instanceof AbstractTime) {
                final AbstractTime q = (AbstractTime) value;
                this.fieldDesc =  q.getDefinition();
                this.fieldType = "Time";
            } else {
                throw new SQLException("Only Quantity, Text AND Time is supported for now");
            }

        }


        public AnyScalar getScalar(final String version) {
            final AbstractDataComponent compo;
            if ("Quantity".equals(fieldType)) {
                final UomProperty uomCode = buildUomProperty(version, fieldUom, null);
                compo = buildQuantity(version, fieldDesc, uomCode, null);
            } else if ("Text".equals(fieldType)) {
                compo = buildText(version, fieldDesc, null);
            } else if ("Time".equals(fieldType)) {
                compo = buildTime(version, fieldDesc, null);
            } else {
                throw new IllegalArgumentException("Unexpected field Type:" + fieldType);
            }
            return buildAnyScalar(version, null, fieldName, compo);
        }

        public String getSQLType(boolean isPostgres, boolean timescaledbMain) throws SQLException {
            if (fieldType.equals("Quantity")) {
                if (timescaledbMain) {
                    return "integer";
                } else if (!isPostgres) {
                    return "double";
                } else {
                    return "double precision";
                }
            } else if (fieldType.equals("Text")) {
                return "character varying(1000)";
            } else if (fieldType.equals("Boolean")) {
                if (isPostgres) {
                    return "boolean";
                } else {
                    return "integer";
                }
            } else if (fieldType.equals("Time")) {
                return "timestamp";
            } else {
                throw new SQLException("Only Quantity, Text AND Time is supported for now");
            }
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 89 * hash + java.util.Objects.hashCode(this.fieldType);
            hash = 89 * hash + java.util.Objects.hashCode(this.fieldName);
            hash = 89 * hash + java.util.Objects.hashCode(this.fieldDesc);
            hash = 89 * hash + java.util.Objects.hashCode(this.fieldUom);
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Field) {
                final Field that = (Field) obj;
                return Objects.equal(this.fieldDesc, that.fieldDesc) &&
                       Objects.equal(this.fieldName, that.fieldName) &&
                       Objects.equal(this.fieldType, that.fieldType) &&
                       Objects.equal(this.fieldUom,  that.fieldUom);
            }
            return false;
        }

        @Override
        public String toString() {
            return fieldName + ": " + fieldType;
        }
    }
}
