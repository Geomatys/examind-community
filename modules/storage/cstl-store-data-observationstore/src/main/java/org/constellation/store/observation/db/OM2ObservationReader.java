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
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildMeasure;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildOffering;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildSimpleDatarecord;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimeInstant;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildTimePeriod;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getDefaultTextEncoding;
import static org.geotoolkit.sos.xml.SOSXmlFactory.getGMLVersion;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import static org.constellation.store.observation.db.OM2Utils.buildComplexResult;
import static org.constellation.store.observation.db.SOSDatabaseObservationStore.RESPONSE_FORMAT;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.observation.model.OMEntity;
import static org.geotoolkit.observation.ObservationReader.ENTITY_TYPE;
import static org.geotoolkit.observation.ObservationReader.SENSOR_TYPE;
import static org.geotoolkit.observation.ObservationReader.SOS_VERSION;
import org.geotoolkit.observation.result.ResultBuilder;
import org.geotoolkit.observation.model.ResultMode;
import static org.geotoolkit.sos.xml.SOSXmlFactory.buildFeatureProperty;
import org.opengis.metadata.quality.Element;
import org.opengis.observation.Process;


/**
 * Default Observation reader for Postgrid O&amp;M database.
 *
 * @author Guilhem Legal
 */
public class OM2ObservationReader extends OM2BaseReader implements ObservationReader {

    protected final DataSource source;

    private final Map<String, List<String>> acceptedSensorMLFormats = new HashMap<>();

    public OM2ObservationReader(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties, final boolean timescaleDB) throws DataStoreException {
        super(properties, schemaPrefix, false, isPostgres, timescaleDB);
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
    public Collection<String> getEntityNames(final Map<String, Object> hints) throws DataStoreException {
        OMEntity entityType = (OMEntity) hints.get(ENTITY_TYPE);
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames();
            case OBSERVED_PROPERTY:   return getPhenomenonNames();
            case PROCEDURE:           return getProcedureNames(sensorType);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames(sensorType);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    private List<String> getOfferingNames(final String sensorType) throws DataStoreException {
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
    public boolean existEntity(final Map<String, Object> hints) throws DataStoreException {
        OMEntity entityType = (OMEntity) hints.get(ENTITY_TYPE);
        if (entityType == null) {
            throw new DataStoreException("Missing entity type parameter");
        }
        String identifier   = (String) hints.get(IDENTIFIER);
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        switch (entityType) {
            case FEATURE_OF_INTEREST: return getFeatureOfInterestNames().contains(identifier);
            case OBSERVED_PROPERTY:   return existPhenomenon(identifier);
            case PROCEDURE:           return existProcedure(identifier);
            case LOCATION:            throw new DataStoreException("not implemented yet.");
            case HISTORICAL_LOCATION: throw new DataStoreException("not implemented yet.");
            case OFFERING:            return getOfferingNames(sensorType).contains(identifier);
            case OBSERVATION:         throw new DataStoreException("not implemented yet.");
            case RESULT:              throw new DataStoreException("not implemented yet.");
            default: throw new DataStoreException("unexpected entity type:" + entityType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ObservationOffering> getObservationOfferings(final Map<String, Object> hints) throws DataStoreException {
        String sensorType   = (String) hints.get(SENSOR_TYPE);
        String version      = (String) hints.get(SOS_VERSION);
        Object identifierVal = hints.get(IDENTIFIER);
        List<String> identifiers = new ArrayList<>();
        if (identifierVal instanceof Collection) {
            identifiers.addAll((Collection<? extends String>) identifierVal);
        } else if (identifierVal instanceof String) {
            identifiers.add((String) identifierVal);
        } else if (identifierVal == null) {
            identifiers.addAll(getOfferingNames(sensorType));
        }
        final List<ObservationOffering> offerings = new ArrayList<>();
        for (String offeringName : identifiers) {
            ObservationOffering off = getObservationOffering(offeringName, version);
            if (off != null) {
                offerings.add(off);
            }
        }
        return offerings;
    }

    private ObservationOffering getObservationOffering(final String offeringName, final String version) throws DataStoreException {
        final String id;
        final String name;
        final String description;
        final TemporalGeometricPrimitive time;
        final String procedure;
        final List<String> phen200             = new ArrayList<>();
        final List<PhenomenonProperty> phen100 = new ArrayList<>();
        final List<String> foi                 = new ArrayList<>();

        try(final Connection c = source.getConnection()) {
            try(final PreparedStatement stmt = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"offerings\" WHERE \"identifier\"=?")) {//NOSONAR
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

                try(final PreparedStatement stmt2 = c.prepareStatement("SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"offering_observed_properties\" WHERE \"id_offering\"=?")) {//NOSONAR
                    stmt2.setString(1, offeringName);
                    try(final ResultSet rs2 = stmt2.executeQuery()) {
                        while (rs2.next()) {
                            final String href = rs2.getString(1);
                            phen200.add(href);
                            phen100.add(new PhenomenonPropertyType(href));
                        }
                    }
                }

                try(final PreparedStatement stmt3 = c.prepareStatement("SELECT \"foi\" FROM \"" + schemaPrefix + "om\".\"offering_foi\" WHERE \"id_offering\"=?")) {//NOSONAR
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
                                 procedure,
                                 phen100,
                                 phen200,
                                 foi,
                                 responseFormat,
                                 resultModel,
                                 resultModelV200,
                                 responseMode,
                                 procedureDescription);

        } catch (SQLException e) {
            throw new DataStoreException("Error while retrieving offering: " + offeringName, e);
        }
    }

    private List<String> getProcedureNames(final String sensorType) throws DataStoreException {
        String filter = "";
        if (sensorType != null) {
            filter = " WHERE \"type\"='" + sensorType + "'";
        }
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\"" + filter)) {//NOSONAR

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
    public Collection<Phenomenon> getPhenomenons(final Map<String, Object> hints) throws DataStoreException {
        String version      = (String) hints.get(SOS_VERSION);
        Object identifierVal = hints.get(IDENTIFIER);
        String where = "";
        if (identifierVal instanceof Collection && !((Collection)identifierVal).isEmpty()) {
            where = "WHERE \"id\" IN (";
            for (String id : (Collection<? extends String>) identifierVal) {
                where = where + "'" + id + "',";
            }
            where = where.substring(0, where.length() - 1) + ")";
        } else if (identifierVal instanceof String) {
            where = "WHERE \"id\" = '" + identifierVal + "'";
        }
        try(final Connection c         = source.getConnection();
            final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\" " + where)) {//NOSONAR
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
    public Process getProcess(String identifier, String version) throws DataStoreException {
        try (final Connection c = source.getConnection()) {
            return getProcess(version, identifier, c);
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
                    if (begin != null && end == null) {
                        result = SOSXmlFactory.buildTimeInstant(version, begin);
                    } else if (begin == null && end != null) {
                        result = SOSXmlFactory.buildTimeInstant(version, end);
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
                    identifier = component[0];
                    fieldIndex    = Integer.parseInt(component[1]);
                    measureId     = Integer.parseInt(component[2]);
                } else if (component.length == 2) {
                    identifier = component[0];
                    measureId     = Integer.parseInt(component[1]);
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
            final String timeID = "time-" + observationID;
            final String observedProperty;
            final String procedure;
            final String foi;
            final TemporalGeometricPrimitive time;

            try(final PreparedStatement stmt  = c.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {//NOSONAR
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

            final Process proc = getProcess(version, procedure, c);
            if (resultModel.equals(MEASUREMENT_QNAME)) {
                if (fieldIndex == null) {
                    throw new DataStoreException("Measurement extraction need a field index specified");
                }
                Field selectedField         = getFieldByIndex(procedure, fieldIndex, true, c);
                List<Element> resultQuality = buildResultQuality(identifier, procedure, measureId, selectedField, c);
                final Object result = getResult(identifier, resultModel, measureId, selectedField, version, c);
                return OMXmlFactory.buildMeasurement(version, obsID, name, null, prop, phen, proc, result, time, null, resultQuality);
            } else {
                final Object result = getResult(identifier, resultModel, measureId, null, version, c);
                return OMXmlFactory.buildObservation(version, obsID, name, null, prop, phen, proc, result, time, null);
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
            return getResult(identifier, resultModel, null, null, version, c);
        } catch (SQLException ex) {
            throw new DataStoreException(ex.getMessage(), ex);
        }
    }

    private Object getResult(final String identifier, final QName resultModel, final Integer measureId, final Field selectedField, final String version, final Connection c) throws DataStoreException, SQLException {
        if (resultModel.equals(MEASUREMENT_QNAME)) {
            return buildMeasureResult(identifier, measureId, selectedField, version, c);
        } else {
            return buildComplexResult2(identifier, measureId, version, c);
        }
    }

    private DataArrayProperty buildComplexResult2(final String identifier, final Integer measureId, final String version, final Connection c) throws DataStoreException, SQLException {

        final String procedure     = getProcedureFromObservation(identifier, c);
        final String measureJoin   = getMeasureTableJoin(getPIDFromProcedure(procedure, c));
        final List<Field> fields   = readFields(procedure, false, c);
        final String arrayID       = "dataArray-0"; // TODO
        final String recordID      = "datarecord-0"; // TODO
        final TextBlock encoding   = getDefaultTextEncoding(version);
        final List<AnyScalar> scal = new ArrayList<>();
        for (Field f : fields) {
            scal.add(f.getScalar(version));
        }

        int nbValue                 = 0;
        final ResultBuilder values  = new ResultBuilder(ResultMode.CSV, encoding, false);
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

        final AbstractDataRecord record = buildSimpleDatarecord(version, null, recordID, null, null, scal);

        return buildDataArrayProperty(version, arrayID, nbValue, arrayID, record, encoding, values.getStringValues(), null);
    }

    private Object buildMeasureResult(final String identifier, final Integer measureId, final Field selectedField, final String version, final Connection c) throws DataStoreException, SQLException {
        if (selectedField == null) {
            throw new DataStoreException("Measurement extraction need a field index specified");
        }
        final String measureJoin   = getMeasureTableJoin(getPIDFromObservation(identifier, c));
        final double value;
        final String name;
        final String uom   = selectedField.uom;
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
                    name = "measure-00" + rs.getString("id");
                    value = Double.parseDouble(rs.getString(selectedField.name));
                } else {
                    return null;
                }
            }
        } catch (NumberFormatException ex) {
            throw new DataStoreException("Unable ta parse the result value as a double");
        }
        return buildMeasure(version, uom, value);
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
    public Observation getTemplateForProcedure(final String procedure, final String version) throws DataStoreException {
        try (final Connection c = source.getConnection()) {

            final String procedureID;
            if (procedure.startsWith(sensorIdBase)) {
                procedureID = procedure.substring(sensorIdBase.length());
            } else {
                procedureID = procedure;
            }
            final String obsID = "obs-" + procedureID;
            final String name = observationTemplateIdBase + procedureID;
            final Phenomenon phen = getGlobalCompositePhenomenon(version, c, procedure);
            String featureID = null;
            FeatureProperty foi = null;
            Set<String> fois = getFoiIdsForProcedure(procedure, c);
            if (fois.size() == 1) {
                featureID = fois.iterator().next();
                final SamplingFeature feature = getFeatureOfInterest(featureID, version, c);
                foi = buildFeatureProperty(version, feature);
            }
            TemporalGeometricPrimitive tempTime = getTimeForTemplate(c, procedure, null, featureID, version);
            List<Field> fields = readFields(procedure, c);
            /*
             *  BUILD RESULT
             */
            final List<AnyScalar> scal = new ArrayList<>();
            for (Field f : fields) {
                scal.add(f.getScalar(version));
            }
            final Process proc = getProcess(version, procedure, c);
            final TextBlock encoding = getDefaultTextEncoding(version);
            
            final Object result = buildComplexResult(version, scal, 0, encoding, null, 0);
            return OMXmlFactory.buildObservation(version, obsID, name, null, foi, phen, proc, result, tempTime, null);
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
        return getProcedureNames(null).contains(href);
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
    public TemporalPrimitive getFeatureOfInterestTime(final String samplingFeatureName, final String version) throws DataStoreException {
        final String query = "SELECT min(\"time_begin\") as mib, max(\"time_begin\") as mab, max(\"time_end\") as mae "
                           + "FROM \"" + schemaPrefix + "om\".\"observations\" "
                           + "WHERE \"foi\"=?";
        try(final Connection c           = source.getConnection();
            final PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
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
                        if (mab != null && mab.after(mae)) {
                            time = buildTimePeriod(version, mib, mab);
                        } else {
                            time = buildTimePeriod(version, mib, mae);
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
    public TemporalPrimitive getEventTime(String version) throws DataStoreException {
        try(final Connection c   = source.getConnection();
            final Statement stmt = c.createStatement();
            final ResultSet rs   = stmt.executeQuery("SELECT max(\"time_begin\"), min(\"time_end\") FROM \"" + schemaPrefix + "om\".\"offerings\"")) {//NOSONAR
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
}
