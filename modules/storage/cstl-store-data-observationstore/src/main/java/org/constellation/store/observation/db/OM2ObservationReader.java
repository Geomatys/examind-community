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

// J2SE dependencies

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.gml.JTStoGeometry;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.xml.OMXmlFactory;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.ResponseModeType;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.DataArrayProperty;
import org.geotoolkit.swe.xml.PhenomenonProperty;
import org.geotoolkit.swe.xml.TextBlock;
import org.geotoolkit.swe.xml.v101.PhenomenonPropertyType;
import org.geotoolkit.util.StringUtilities;
import org.opengis.observation.Observation;
import org.opengis.observation.Phenomenon;
import org.opengis.observation.sampling.SamplingFeature;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.constellation.api.CommonConstants.MEASUREMENT_QNAME;
import static org.constellation.api.CommonConstants.OBSERVATION_MODEL;
import static org.constellation.api.CommonConstants.OBSERVATION_QNAME;
import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_100_FORMAT_V200;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V100;
import static org.constellation.api.CommonConstants.SENSORML_101_FORMAT_V200;
import org.geotoolkit.gml.xml.TimeIndeterminateValueType;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildDataArrayProperty;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildFeatureProperty;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildMeasure;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildOffering;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSimpleDatarecord;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeInstant;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimePeriod;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getDefaultTextEncoding;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getGMLVersion;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.opengis.observation.Process;


/**
 * Default Observation reader for Postgrid O&amp;M database.
 *
 * @author Guilhem Legal
 */
public class OM2ObservationReader extends OM2BaseReader implements ObservationReader {

    protected final DataSource source;

    private static final Map<String, List<String>> RESPONSE_FORMAT = new HashMap<>();
    static {
        RESPONSE_FORMAT.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        RESPONSE_FORMAT.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));
    }

    private final Map<String, List<String>> acceptedSensorMLFormats = new HashMap<>();

    public OM2ObservationReader(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties) throws DataStoreException {
        super(properties, schemaPrefix);
        this.isPostgres = isPostgres;
        this.source = source;
        try {
            // try if the connection is valid
            try(final Connection c = this.source.getConnection()) {}
        } catch (SQLException ex) {
            throw new DataStoreException(ex);
        }
        final String smlFormats100 = (String) properties.get("smlFormats100");
        if (smlFormats100 != null) {
            acceptedSensorMLFormats.put("1.0.0", StringUtilities.toStringList(smlFormats100));
        } else {
            acceptedSensorMLFormats.put("1.0.0", Arrays.asList(SENSORML_100_FORMAT_V100,
                                                               SENSORML_101_FORMAT_V100));
        }

        final String smlFormats200 = (String) properties.get("smlFormats200");
        if (smlFormats200 != null) {
            acceptedSensorMLFormats.put("2.0.0", StringUtilities.toStringList(smlFormats200));
        } else {
            acceptedSensorMLFormats.put("2.0.0", Arrays.asList(SENSORML_100_FORMAT_V200,
                                                               SENSORML_101_FORMAT_V200));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getOfferingNames(final String version) throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement()) {
            final List<String> results = new ArrayList<>();
            try(final ResultSet rs         = stmt.executeQuery("SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\"")) {
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving offering names.", ex);
        }
    }

    @Override
    public List<String> getOfferingNames(final String version, final String sensorType) throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement()) {
            final List<String> results = new ArrayList<>();
            String query;
            if (sensorType != null) {
                query = "SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" o, \"" + schemaPrefix + "om\".\"procedures\" p WHERE  o.\"procedure\" = p.\"id\" AND p.\"type\" = '" + sensorType + "'";
            } else {
                query = "SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\"";
            }

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
    public List<ObservationOffering> getObservationOfferings(final List<String> offeringNames, final String version) throws DataStoreException {
        final List<ObservationOffering> offerings = new ArrayList<>();
        for (String offeringName : offeringNames) {
            offerings.add(getObservationOffering(offeringName, version));
        }
        return offerings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObservationOffering getObservationOffering(final String offeringName, final String version) throws DataStoreException {
        final String id;
        final String name;
        final String description;
        final TemporalGeometricPrimitive time;
        final String procedure;
        final List<String> phen200             = new ArrayList<>();
        final List<PhenomenonProperty> phen100 = new ArrayList<>();
        final List<String> foi                 = new ArrayList<>();

        try(final Connection c = source.getConnection()) {
            try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"offerings\" WHERE \"identifier\"=?")) {
                stmt.setString(1, offeringName);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        id                 = rs.getString(1);
                        description        = rs.getString(2);
                        name               = rs.getString(3);
                        final Timestamp b  = rs.getTimestamp(4);
                        final Timestamp e  = rs.getTimestamp(5);
                        procedure          = rs.getString(6);
                        if (b != null && e == null) {
                            time = buildTimePeriod(version, b, (TimeIndeterminateValueType)null);
                        } else if (b != null && e != null) {
                            time = buildTimePeriod(version, b, e);
                        } else {
                            time = null;
                        }
                    } else {
                        return null;
                    }
                }

                try(final PreparedStatement stmt2 = c.prepareStatement("SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"offering_observed_properties\" WHERE \"id_offering\"=?")) {
                    stmt2.setString(1, offeringName);
                    try(final ResultSet rs2 = stmt2.executeQuery()) {
                        while (rs2.next()) {
                            final String href = rs2.getString(1);
                            phen200.add(href);
                            phen100.add(new PhenomenonPropertyType(href));
                        }
                    }
                }

                try(final PreparedStatement stmt3 = c.prepareStatement("SELECT \"foi\" FROM \"" + schemaPrefix + "om\".\"offering_foi\" WHERE \"id_offering\"=?")) {
                    stmt3.setString(1, offeringName);
                    try(final ResultSet rs3 = stmt3.executeQuery()) {
                        while (rs3.next()) {
                            foi.add(rs3.getString(1));
                        }
                    }
                }
            }

            final List<String> responseFormat         = RESPONSE_FORMAT.get(version);
            final List<QName> resultModel             = Arrays.asList(OBSERVATION_QNAME, MEASUREMENT_QNAME);
            final List<String> resultModelV200        = Arrays.asList(OBSERVATION_MODEL);
            final List<ResponseModeType> responseMode = Arrays.asList(ResponseModeType.INLINE, ResponseModeType.RESULT_TEMPLATE);
            final List<String> procedureDescription   = acceptedSensorMLFormats.get("2.0.0");
            return buildOffering(version,
                                 id,
                                 name,
                                 description,
                                 null,
                                 time,
                                 Arrays.asList(procedure),
                                 phen100,
                                 phen200,
                                 foi,
                                 responseFormat,
                                 resultModel,
                                 resultModelV200,
                                 responseMode,
                                 procedureDescription);

        } catch (SQLException e) {
            throw new DataStoreException("Error while retrieving offering names.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationOffering> getObservationOfferings(final String version) throws DataStoreException {
        final List<String> offeringNames    = getOfferingNames(version);
        final List<ObservationOffering> loo = new ArrayList<>();
        for (String offeringName : offeringNames) {
            loo.add(getObservationOffering(offeringName, version));
        }
        return loo;
    }

    @Override
    public List<ObservationOffering> getObservationOfferings(final String version, final String sensorType) throws DataStoreException {
        final List<String> offeringNames    = getOfferingNames(version, sensorType);
        final List<ObservationOffering> loo = new ArrayList<>();
        for (String offeringName : offeringNames) {
            loo.add(getObservationOffering(offeringName, version));
        }
        return loo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getProcedureNames() throws DataStoreException {
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\"")) {

            final List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getProcedureNames(final String sensorType) throws DataStoreException {
        String filter = "";
        if (sensorType != null) {
            filter = " WHERE \"type\"='" + sensorType + "'";
        }
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\"" + filter)) {

            final List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString(1));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getPhenomenonNames() throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\"")) {
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
    public Collection<Phenomenon> getPhenomenons(final String version) throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\"")) {
            final List<Phenomenon> results = new ArrayList<>();
            while (rs.next()) {
                results.add(getPhenomenon(version, rs.getString(1), c));
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
    public Phenomenon getPhenomenon(String identifier, String version) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getPhenomenon(version, identifier, c);
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Process getProcess(String identifier, String version) throws DataStoreException {
        if (existProcedure(identifier)) {
            return SOSXmlFactory.buildProcess(version, identifier);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getProceduresForPhenomenon(final String observedProperty) throws DataStoreException {
        try(final Connection c           = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement("SELECT DISTINCT \"procedure\" "
                                                            + "FROM \"" + schemaPrefix + "om\".\"offerings\", \"" + schemaPrefix + "om\".\"offering_observed_properties\""
                                                            + "WHERE \"identifier\"=\"id_offering\""
                                                            + "AND \"phenomenon\"=?")) {
            final List<String> results   = new ArrayList<>();
            stmt.setString(1, observedProperty);
            try(final ResultSet rs =  stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getPhenomenonsForProcedure(final String sensorID) throws DataStoreException {
        try(final Connection c          = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement("SELECT \"phenomenon\" "
                                                            + "FROM \"" + schemaPrefix + "om\".\"offerings\", \"" + schemaPrefix + "om\".\"offering_observed_properties\""
                                                            + "WHERE \"identifier\"=\"id_offering\""
                                                            + "AND \"procedure\"=?")) {
            final Set<String> results   = new HashSet<>();
            stmt.setString(1, sensorID);
            try(final ResultSet rs =  stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getString(1));
                }
            }

            //look for composite phenomenons
            try(final PreparedStatement stmtC = c.prepareStatement("SELECT \"component\" "
                                                             + "FROM \"" + schemaPrefix + "om\".\"components\""
                                                             + "WHERE \"phenomenon\"=?")) {
                final Set<String> toAdd    = new HashSet<>();
                final Set<String> toRemove = new HashSet<>();
                for (Iterator<String> it = results.iterator(); it.hasNext();) {
                    String pheno = it.next();
                    stmtC.setString(1, pheno);
                    try(final ResultSet rsC = stmtC.executeQuery()) {
                        boolean composite = false;
                        while (rsC.next()) {
                            composite = true;
                            toAdd.add(rsC.getString(1));
                        }
                        if (composite) {
                            toRemove.add(pheno);
                        }
                    }
                }
                results.removeAll(toRemove);
                results.addAll(toAdd);
            }

            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existPhenomenon(final String phenomenonName) throws DataStoreException {
        return phenomenonName.equals(phenomenonIdBase + "ALL") || getPhenomenonNames().contains(phenomenonName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getFeatureOfInterestNames() throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"sampling_features\"")) {
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
    public Collection<SamplingFeature> getFeatureOfInterestForProcedure(String sensorID, String version) throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT sf.\"id\" FROM \"" + schemaPrefix + "om\".\"sampling_features\" sf, \"" + schemaPrefix + "om\".\"observations\" ob "
                                                         + " WHERE sf.\"id\"=ob.\"foi\" AND ob.\"procedure\"='" + sensorID +"'")) {
            final List<SamplingFeature> results = new ArrayList<>();
            while (rs.next()) {
                results.add(getFeatureOfInterest(rs.getString(1), version, c));
            }
            return results;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving existingFeature for procedure.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SamplingFeature getFeatureOfInterest(final String samplingFeatureName, final String version) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getFeatureOfInterest(samplingFeatureName, version, c);
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    @Override
    public AbstractGeometry getSensorLocation(final String sensorID, final String version) throws DataStoreException {
        try(final Connection c = source.getConnection()) {

            final byte[] b;
            final int srid;
            try(final PreparedStatement stmt = (isPostgres) ?
                    c.prepareStatement("SELECT st_asBinary(\"shape\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?") :
                    c.prepareStatement("SELECT \"shape\", \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {
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
            } else {
                return null;
            }

            final String gmlVersion = getGMLVersion(version);
            return JTStoGeometry.toGML(gmlVersion, geom, crs);
        } catch (SQLException | FactoryException  | ParseException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    @Override
    public Map<Date, AbstractGeometry> getSensorLocations(final String sensorID, final String version) throws DataStoreException {
        final Map<Date, AbstractGeometry> results = new HashMap<>();
        try(final Connection c = source.getConnection()) {

            try(final PreparedStatement stmt = (isPostgres) ?
                    c.prepareStatement("SELECT \"time\", st_asBinary(\"location\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?") :
                    c.prepareStatement("SELECT \"time\", \"location\", \"crs\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?")) {
                stmt.setString(1, sensorID);
                try(final ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        final Date d = rs.getTimestamp(1);
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
                        } else {
                            return null;
                        }

                        final String gmlVersion = getGMLVersion(version);
                        results.put(d, JTStoGeometry.toGML(gmlVersion, geom, crs));
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
    public TemporalGeometricPrimitive getTimeForProcedure(final String version, final String sensorID) throws DataStoreException {
        TemporalGeometricPrimitive result = null;
        try(final Connection c          = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement("SELECT \"time_begin\", \"time_end\" "
                                                            + "FROM \"" + schemaPrefix + "om\".\"offerings\" "
                                                            + "WHERE \"procedure\"=?")) {
            stmt.setString(1, sensorID);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    final Timestamp begin = rs.getTimestamp(1);
                    final Timestamp end = rs.getTimestamp(2);
                    if (begin != null && end == null) {
                        result = SOSXmlFactory.buildTimeInstant(version, begin);
                    } else if (begin == null && end != null) {
                        result = SOSXmlFactory.buildTimeInstant(version, begin);
                    } else if (begin != null && end != null) {
                        result = SOSXmlFactory.buildTimePeriod(version, begin, end);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving procedure time.", ex);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getObservation(String identifier, final QName resultModel, final ResponseModeType mode, final String version) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            final String observationID;
            if (identifier.startsWith(observationIdBase)) {
                observationID = identifier.substring(observationIdBase.length());
            } else if (identifier.startsWith(observationTemplateIdBase)) {
                final String procedureID     = sensorIdBase + identifier.substring(observationTemplateIdBase.length());
                try(final PreparedStatement stmt = c.prepareStatement("SELECT \"id\", \"identifier\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?")) {
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
            } else {
                observationID = identifier;
            }

            final String obsID = "obs-" + observationID;
            final String timeID = "time-" + observationID;
            final String observedProperty;
            final String procedure;
            final String foi;
            final TemporalGeometricPrimitive time;

            try(final PreparedStatement stmt  = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {
                stmt.setString(1, identifier);
                try(final ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        final String b = rs.getString(3);
                        final String e = rs.getString(4);
                        if (b != null && e == null) {
                            time = buildTimeInstant(version, timeID, b.replace(' ', 'T'));
                        } else if (b != null && e != null) {
                            time = buildTimePeriod(version, timeID, b.replace(' ', 'T'), e.replace(' ', 'T'));
                        } else {
                            time = null;
                        }
                        observedProperty = rs.getString(5);
                        procedure = rs.getString(6);
                        foi = rs.getString(7);
                    } else {
                        return null;
                    }
                }
            }

            final SamplingFeature feature = getFeatureOfInterest(foi, version, c);
            final FeatureProperty prop    = buildFeatureProperty(version, feature);
            final Phenomenon phen         = getPhenomenon(version, observedProperty, c);

            final String name;
            if (ResponseModeType.RESULT_TEMPLATE.equals(mode)) {
                final String procedureID = procedure.substring(sensorIdBase.length());
                name = observationTemplateIdBase + procedureID;
            } else {
                name = identifier;
            }

            if (resultModel.equals(MEASUREMENT_QNAME)) {
                final Object result = getResult(identifier, resultModel, version);
                return OMXmlFactory.buildMeasurement(version, obsID, name, null, prop, phen, procedure, result, time);
            } else {
                final Object result = getResult(identifier, resultModel, version);
                return OMXmlFactory.buildObservation(version, obsID, name, null, prop, phen, procedure, result, time);
            }

        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getResult(final String identifier, final QName resultModel, final String version) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            return getResult(identifier, resultModel, version, c);
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private Object getResult(final String identifier, final QName resultModel, final String version, final Connection c) throws DataStoreException, SQLException {
        if (resultModel.equals(MEASUREMENT_QNAME)) {
            return buildMeasureResult(identifier, version, c);
        } else {
            return buildComplexResult2(identifier, version, c);
        }
    }

    private DataArrayProperty buildComplexResult2(final String identifier, final String version, final Connection c) throws DataStoreException, SQLException {

        final int pid              = getPIDFromObservation(identifier, c);
        final String procedure     = getProcedureFromObservation(identifier, c);
        final List<Field> fields   = readFields(procedure, c);
        final String arrayID       = "dataArray-1"; // TODO
        final String recordID      = "datarecord-0"; // TODO
        final TextBlock encoding   = getDefaultTextEncoding(version);
        final List<AnyScalar> scal = new ArrayList<>();
        for (Field f : fields) {
            scal.add(f.getScalar(version));
        }

        int nbValue                = 0;
        final StringBuilder values = new StringBuilder();


        try(final PreparedStatement stmt  = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m, \"" + schemaPrefix + "om\".\"observations\" o "
                                                         + "WHERE \"id_observation\" = o.\"id\" "
                                                         + "AND o.\"identifier\"=?"
                                                         + "ORDER BY m.\"id\"")) {
            stmt.setString(1, identifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        String value;
                        if (field.fieldType.equals("Time")) {
                            Timestamp t = rs.getTimestamp(i + 3);
                            synchronized(format2) {
                                value = format2.format(t);
                            }
                        } else {
                            value = rs.getString(i + 3);
                        }
                        values.append(value).append(encoding.getTokenSeparator());
                    }
                    values.deleteCharAt(values.length() - 1);
                    values.append(encoding.getBlockSeparator());
                    nbValue++;
                }
            }
        }

        final AbstractDataRecord record = buildSimpleDatarecord(version, null, recordID, null, false, scal);

        return buildDataArrayProperty(version, arrayID, nbValue, arrayID, record, encoding, values.toString());
    }

    private Object buildMeasureResult(final String identifier, final String version, final Connection c) throws DataStoreException, SQLException {
        final int pid              = getPIDFromObservation(identifier, c);
        final String procedure     = getProcedureFromObservation(identifier, c);
        final List<Field> fields   = readFields(procedure, c);
        final String uom           = fields.get(0).fieldUom;
        final double value;
        final String name;
        try(final PreparedStatement stmt  = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" m, \"" + schemaPrefix + "om\".\"observations\" o "
                                                             + "WHERE \"id_observation\" = o.\"id\" "
                                                             + "AND o.\"identifier\"=?"
                                                             + "ORDER BY m.\"id\"")) {
            stmt.setString(1, identifier);
            try(final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    name = "measure-00" + rs.getString("id");
                    value = Double.parseDouble(rs.getString(3));
                } else {
                    return null;
                }
            }
        } catch (NumberFormatException ex) {
            throw new DataStoreException("Unable ta parse the result value as a double");
        }
        return buildMeasure(version, name, uom, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observation getTemplateForProcedure(final String procedure, final String version) throws DataStoreException {
        // TODO generate template from procedue description
        Observation template = null;
        try(final Connection c = source.getConnection()) {
            try(final Statement stmt = c.createStatement();
                final ResultSet rs   = stmt.executeQuery("SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"='" + procedure + "'")) {
                String identifier = null;
                if (rs.next()) {
                    identifier = rs.getString(1);
                }
                if (identifier != null) {
                    template = getObservation(identifier, OBSERVATION_QNAME, ResponseModeType.RESULT_TEMPLATE, version);
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
        return template;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existProcedure(final String href) throws DataStoreException {
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
    public String getNewObservationId() throws DataStoreException {
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT max(\"id\") FROM \"" + schemaPrefix + "om\".\"observations\"")) {
            int resultNum;
            if (rs.next()) {
                resultNum = rs.getInt(1) + 1;
            } else {
                resultNum = 1;
            }
            return observationIdBase + resultNum;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while looking for available observation id.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TemporalPrimitive getFeatureOfInterestTime(final String samplingFeatureName, final String version) throws DataStoreException {
        try(final Connection c           = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement("SELECT min(\"time_begin\") as mib, max(\"time_begin\") as mab, max(\"time_end\") as mae "
                                                            + "FROM \"" + schemaPrefix + "om\".\"observations\" "
                                                            + "WHERE \"foi\"=?")) {
            stmt.setString(1, samplingFeatureName);
            try (final ResultSet rs = stmt.executeQuery()) {
                final TemporalGeometricPrimitive time;
                if (rs.next()) {
                    final Timestamp mib = rs.getTimestamp(1);
                    final Timestamp mab = rs.getTimestamp(2);
                    final Timestamp mae = rs.getTimestamp(3);
                    if (mib != null && mae == null) {
                        if (mab != null && !mib.equals(mab)) {
                            time = buildTimePeriod(version, mib, mab);
                        } else {
                            time = buildTimeInstant(version, mib);
                        }
                    } else if (mib != null && mae != null) {
                        time = buildTimePeriod(version, mib, mae);
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
    public TemporalPrimitive getEventTime(String version) throws DataStoreException {
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT max(\"time_begin\"), min(\"time_end\") FROM \"" + schemaPrefix + "om\".\"offerings\"")) {
            String start         = "undefined";
            String end           = "now";
            if (rs.next()) {
                String s = rs.getString(1);
                if (s != null) {
                    start = s;
                }
                s = rs.getString(2);
                if (s != null) {
                    end = s;
                }
            }
            if ("2.0.0".equals(version)) {
                return GMLXmlFactory.createTimePeriod("3.2.1", "evt-1", start, end);
            } else {
                return GMLXmlFactory.createTimePeriod("3.1.1", "evt-1", start, end);
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while retrieving phenomenon names.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInfos() {
        return "Constellation O&M 2 Reader 1.2-EE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResponseModeType> getResponseModes() throws DataStoreException {
        return Arrays.asList(ResponseModeType.INLINE, ResponseModeType.RESULT_TEMPLATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getResponseFormats() throws DataStoreException {
        return RESPONSE_FORMAT;
    }
}
