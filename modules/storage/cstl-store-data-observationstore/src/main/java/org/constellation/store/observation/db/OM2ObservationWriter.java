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
import org.apache.sis.storage.DataStoreException;
import org.constellation.admin.SpringHelper;
import org.geotoolkit.observation.ObservationWriter;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalObject;
import org.opengis.util.FactoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.constellation.dto.service.config.sos.OM2ResultEventDTO;
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import static org.constellation.store.observation.db.OM2Utils.*;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.Util;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.model.TextEncoderProperties;



/**
 * Default Observation reader for Postgis O&amp;M2 database.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class OM2ObservationWriter extends OM2BaseReader implements ObservationWriter {

    protected final DataSource source;

    private final boolean allowSensorStructureUpdate = true;

    private final int maxFieldByTable;

    /**
     * Build a new Observation writer for the given data source.
     *
     * @param source Data source on the database.
     * @param isPostgres {@code True} if the database is a postgresql db, {@code false} otherwise.
     * @param schemaPrefix Prefix for the database schemas.
     * @param properties
     * @param timescaleDB {@code True} if the database has the TimescaleDB extension available.
     * @param maxFieldByTable Maximum number of field allowed for a measure table.
     *
     * @throws org.apache.sis.storage.DataStoreException
     */
    public OM2ObservationWriter(final DataSource source, final boolean isPostgres, final String schemaPrefix, final Map<String, Object> properties, final boolean timescaleDB, final int maxFieldByTable) throws DataStoreException {
        super(properties, schemaPrefix, false, isPostgres, timescaleDB);
        if (source == null) {
            throw new DataStoreException("The source object is null");
        }
        this.source = source;
        this.maxFieldByTable = maxFieldByTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String writeObservation(final Observation observation) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            final int generatedID   = getNewObservationId(c);
            final String oid        = writeObservation(observation, c, generatedID);
            return oid;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while inserting observation.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<String> writeObservations(final List<Observation> observations) throws DataStoreException {
        final List<String> results = new ArrayList<>();
        try(final Connection c = source.getConnection()) {
            int generatedID = getNewObservationId(c);
            for (Observation observation : observations) {
                final String oid = writeObservation(observation, c, generatedID);
                results.add(oid);
                generatedID++;
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while inserting observations.", ex);
        }
        return results;
    }

    private static final class ObservationRef {
        public final int id;
        public final String name;
        public final String phenomenonId;
        public final Timestamp extendStartTime;
        public final Timestamp extendEndTime;

        public ObservationRef(int id, String name, String phenomenonId, Timestamp extendStartTime, Timestamp extendEndTime) {
            this.id = id;
            this.name = name;
            this.phenomenonId = phenomenonId;
            this.extendStartTime = extendStartTime;
            this.extendEndTime = extendEndTime;
        }
    }

    private ObservationRef isConflicted(final Connection c, final String procedureID, final TemporalObject samplingTime, final String foiID) throws DataStoreException {
        if (samplingTime != null) {
            FilterSQLRequest sqlRequest = new FilterSQLRequest("SELECT \"id\", \"identifier\", \"observed_property\", \"time_begin\", \"time_end\" FROM \"" + schemaPrefix + "om\".\"observations\" o WHERE ");
            sqlRequest.append(" \"procedure\"=").appendValue(procedureID);
            if (foiID != null) {
                sqlRequest.append(" AND \"foi\"=").appendValue(foiID);
            }

            FilterSQLRequest sqlConflictRequest = sqlRequest.clone();
            sqlConflictRequest.append(" AND ( ");
            addtimeDuringSQLFilter(sqlConflictRequest, samplingTime, "o");
            sqlConflictRequest.append(" ) ");
            
            List<ObservationRef> obs = new ArrayList<>();
            try (final PreparedStatement pstmt = sqlConflictRequest.fillParams(c.prepareStatement(sqlConflictRequest.getRequest()));
                 final ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // look for observation time extension
                    Timestamp extBegin = null;
                    Timestamp extEnd   = null;
                    if (samplingTime instanceof Period p) {
                        final Timestamp newBegin = getInstantTimestamp(p.getBeginning());
                        final Timestamp newEnd   = getInstantTimestamp(p.getEnding());
                        final Timestamp obsBegin = rs.getTimestamp("time_begin");
                        final Timestamp obsEnd   = rs.getTimestamp("time_end");
                        if (newBegin.before(obsBegin)) {
                            extBegin = newBegin;
                        }
                        if (newEnd.after(obsEnd)) {
                            extEnd = newEnd;
                        }
                    }
                    obs.add(new ObservationRef(rs.getInt("id"), rs.getString("identifier"), rs.getString("observed_property"), extBegin, extEnd));
                    
                }
            } catch (SQLException ex) {
                throw new DataStoreException("Error while looking for conflicting observation:" + ex.getMessage(), ex);
            }

            // there a conflict with another observations
            if (!obs.isEmpty()) {
                LOGGER.info("Found a potentially conflict in observation insertion");
                if (samplingTime instanceof Instant i) {
                    // the observation can be inserted into another one
                    if (obs.size() == 1) {
                        return obs.get(0);
                    // this means there are already conflicted observation
                    } else {
                        throw new DataStoreException("The observation is in temporal conflict with other (already conflicting observation present)");
                    }

                } else if (samplingTime instanceof Period p) {
                    /*sqlRequest.append(" AND ( ");
                    addTimeContainsSQLFilter(sqlRequest, p);
                    sqlRequest.append(" ) ");

                    // restrict to already found observations
                    if (obs.size() == 1) {
                        sqlRequest.append(" AND \"id\" = " + obs.get(0).id);
                    } else {
                        sqlRequest.append(" AND \"id\" IN ( ");
                        for (ObservationRef o : obs) {
                            sqlRequest.append(o.id + ", ");
                        }
                        sqlRequest.delete(sqlRequest.length() -2, sqlRequest.length());
                        sqlRequest.append(")");
                    }
                    LOGGER.fine("conflict request:" + sqlRequest.toString());

                    List<ObservationRef> matchingObs = new ArrayList<>();
                    try (final PreparedStatement pstmt = sqlRequest.fillParams(c.prepareStatement(sqlRequest.getRequest()));
                         final ResultSet rs = pstmt.executeQuery()) {
                       while (rs.next()) {
                           matchingObs.add(new ObservationRef(rs.getInt("id"), rs.getString("identifier"), rs.getString("observed_property")));
                       }
                    } catch (SQLException ex) {
                       throw new DataStoreException("Error while looking for conflicting observation:" + ex.getMessage(), ex);
                    }

                    // if there is no match, this means that the current observation period is overlapping each conflicted observation
                    // TODO: there is probably a way to handle this case by updating the observation
                    if (matchingObs.isEmpty()) {
                        throw new DataStoreException("Unable to find a conflicted observation to update.");

                    // the current observation can be inserted inside another one
                    } else if (matchingObs.size() == 1) {
                        return matchingObs.get(0);

                    // this means there are already conflicted observations (the current observation period is included in multiple other observations).
                    } else {
                        throw new DataStoreException("Unable to find a conflicted observation to update (multiple match was found).");
                    }*/
                    
                    // the observation can be inserted into another one
                    if (obs.size() == 1) {
                        return obs.get(0);
                        
                    // not handled for now
                    } else {
                        throw new DataStoreException("The observation is in temporal conflict with multiple observations. Not handled yet");
                    }

                // unexpected case
                } else {
                    throw new DataStoreException("Unpected sampling time implementation." + samplingTime.getClass().getName());
                }
            }
        }
        return null;
    }

    private String writeObservation(final Observation observation, final Connection c, final int generatedID) throws DataStoreException {
        // look for an conflicted observation
        final Procedure procedure  = observation.getProcedure();
        final String procedureID   = procedure.getId();
        final String procedureName = procedure.getName();
        final String procedureDesc = procedure.getDescription();
        
        final TemporalObject samplingTime = observation.getSamplingTime();

        final SamplingFeature foi = observation.getFeatureOfInterest();
        String foiID = null;
        if (foi != null) {
            foiID = foi.getId();
        }

        final Phenomenon phenomenon = observation.getObservedProperty();

        ObservationRef replacingObs = isConflicted(c, procedureID, samplingTime, foiID);
        
        final String phenRef;
        final String observationName;

        // insert observation
        try (final PreparedStatement insertObs   = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"observations\" VALUES(?,?,?,?,?,?,?)");                  //NOSONAR
             final PreparedStatement updatePhen  = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"observed_property\" = ? WHERE \"id\" = ?");//NOSONAR
             final PreparedStatement updateBegin = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"time_begin\" = ? WHERE \"id\" = ?");//NOSONAR
             final PreparedStatement updateEnd   = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"time_end\" = ? WHERE \"id\" = ?")) { //NOSONAR

            // insert a new observation
            if (replacingObs == null) {
                int oid;
                if (observation.getName() == null) {
                    oid = generatedID;
                    observationName = observationIdBase + oid;
                } else {
                    observationName = observation.getName().getCode();
                    if (observationName.startsWith(observationIdBase)) {
                        try {
                            oid = Integer.parseInt(observationName.substring(observationIdBase.length()));
                        } catch (NumberFormatException ex) {
                            oid = generatedID;
                        }
                    } else {
                        oid = generatedID;
                    }
                }

                insertObs.setString(1, observationName);
                insertObs.setInt(2, oid);

                if (samplingTime instanceof Period p) {
                    final Timestamp beginDate = getInstantTimestamp(p.getBeginning());
                    final Timestamp endDate   = getInstantTimestamp(p.getEnding());
                    if (beginDate != null) {
                        insertObs.setTimestamp(3, beginDate);
                    } else {
                        insertObs.setNull(3, java.sql.Types.TIMESTAMP);
                    }
                    if (endDate != null) {
                        insertObs.setTimestamp(4, endDate);
                    } else {
                        insertObs.setNull(4, java.sql.Types.TIMESTAMP);
                    }
                } else if (samplingTime instanceof Instant inst) {
                    final Timestamp date = getInstantTimestamp(inst);
                    if (date != null) {
                        insertObs.setTimestamp(3, date);
                    } else {
                        insertObs.setNull(3, java.sql.Types.TIMESTAMP);
                    }
                    insertObs.setNull(4, java.sql.Types.TIMESTAMP);
                } else {
                    insertObs.setNull(3, java.sql.Types.TIMESTAMP);
                    insertObs.setNull(4, java.sql.Types.TIMESTAMP);
                }
                phenRef = writePhenomenon(phenomenon, c, false);
                insertObs.setString(5, phenRef);


                final int pid = writeProcedure(new ProcedureDataset(procedureID, procedureName, procedureDesc, null, null, new ArrayList<>(), null), null, c);
                insertObs.setString(6, procedureID);
                if (foiID != null) {
                    insertObs.setString(7, foiID);
                    writeFeatureOfInterest(foi, c);
                } else {
                    insertObs.setNull(7, java.sql.Types.VARCHAR);
                }
                insertObs.executeUpdate();
                writeResult(oid, pid, procedureID, observation.getResult(), c, false);
                emitResultOnBus(procedureID, observation.getResult());

            // update an existing observation
            } else {
                String newPhen = writePhenomenon(phenomenon, c, false);
                
                observationName = replacingObs.name;
                final int[] pidNumber = getPIDFromProcedure(procedureID, c);
                writeResult(replacingObs.id, pidNumber[0], procedureID, observation.getResult(), c, true);
                // if a new phenomenon has been added we must create a composite and change the observation reference
                if (!replacingObs.phenomenonId.equals(newPhen)) {
                    List<Field> readFields = readFields(procedureID, true, c);
                   // Set<Phenomenon> temporaries = getAllPhenomenon(c).stream().map(phen -> TemporaryUtils.toXML(phen, "1.0.0")).collect(Collectors.toSet());
                    Phenomenon replacingPhen = OMUtils.getPhenomenonModels("1.0.O", readFields, phenomenonIdBase, getAllPhenomenon(c));

                    //PhenomenonProperty replacingPhenP = SOSXmlFactory.buildPhenomenonProperty("1.0.0", (org.geotoolkit.swe.xml.Phenomenon) replacingPhen);
                    phenRef = writePhenomenon(replacingPhen, c, false);
                    updatePhen.setString(1, phenRef);
                    updatePhen.setInt(2, replacingObs.id);
                    updatePhen.executeUpdate();
                } else {
                    phenRef = newPhen;
                }
                if (replacingObs.extendStartTime != null) {
                    updateBegin.setTimestamp(1, replacingObs.extendStartTime);
                    updateBegin.setInt(2, replacingObs.id);
                    updateBegin.executeUpdate();
                }
                if (replacingObs.extendEndTime != null) {
                    updateEnd.setTimestamp(1, replacingObs.extendEndTime);
                    updateEnd.setInt(2, replacingObs.id);
                    updateEnd.executeUpdate();
                }
            }
            
            String parent = getProcedureParent(procedureID, c);
            if (parent != null) {
                updateOrCreateOffering(parent,samplingTime, phenRef, foiID, c);
            }
            updateOrCreateOffering(procedureID,samplingTime, phenRef, foiID, c);

            return observationName;
        } catch (SQLException | FactoryException ex) {
            throw new DataStoreException("Error while inserting observation:" + ex.getMessage(), ex);
        }
    }

    private void emitResultOnBus(String procedureID, Object result) {
        if (result instanceof ComplexResult cr){
            OM2ResultEventDTO resultEvent = new OM2ResultEventDTO();
            final TextEncoderProperties encoding = cr.getTextEncodingProperties();
            resultEvent.setBlockSeparator(encoding.getBlockSeparator());
            resultEvent.setDecimalSeparator(encoding.getDecimalSeparator());
            resultEvent.setTokenSeparator(encoding.getTokenSeparator());
            resultEvent.setValues(cr.getValues());
            List<String> headers = new ArrayList<>();
            for (Field field : cr.getFields()) {
                headers.add(field.name);
            }
            resultEvent.setHeaders(headers);
            resultEvent.setProcedureID(procedureID);
            SpringHelper.sendEvent(resultEvent);
        }
    }

    private void deleteProperties(String tableName, String columnName, String id, Connection c) throws SQLException {
        String request = "DELETE FROM \"" + schemaPrefix + "om\".\"" + tableName + "\" WHERE \"" + columnName + "\"=?";
        LOGGER.fine(request);
        try (final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            stmt.setString(1, id);
            stmt.executeUpdate();
        }
    }

    private void writeProperties(String tableName, String id,  Map<String, Object> properties, Connection c) throws SQLException {
        if (properties == null || properties.isEmpty()) return;
        final String request = "INSERT into \"" + schemaPrefix + "om\".\"" + tableName + "\" VALUES (?,?,?)";
        LOGGER.fine(request);
        try (final PreparedStatement stmt = c.prepareStatement(request)) {//NOSONAR
            for (Entry<String, Object> entry : properties.entrySet()) {
                stmt.setString(1, id);
                stmt.setString(2, entry.getKey());
                stmt.setString(3, entry.getValue().toString());
                stmt.executeUpdate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void writePhenomenons(final List<Phenomenon> phenomenons) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            for (Phenomenon phenomenon : phenomenons) {
                writePhenomenon(phenomenon, c, false);
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while inserting phenomenons.", ex);
        }
    }

    private String writePhenomenon(final Phenomenon phenomenon, final Connection c, final boolean partial) throws SQLException {
        final String phenomenonId   = phenomenon != null ? phenomenon.getId() : null;
        if (phenomenonId == null) return null;

        try(final PreparedStatement stmtExist = c.prepareStatement("SELECT \"id\", \"partial\" FROM  \"" + schemaPrefix + "om\".\"observed_properties\" WHERE \"id\"=?")) {//NOSONAR
            stmtExist.setString(1, phenomenonId);
            boolean exist = false;
            boolean isPartial = false;
            try(final ResultSet rs = stmtExist.executeQuery()) {
                if (rs.next()) {
                    isPartial = rs.getBoolean("partial");
                    exist = true;
                }
            }

            // if not exist with id, try without phenonmenon base (because of v2 href / v1 difference)
            if (!exist && phenomenonId.startsWith(phenomenonIdBase)) {
                String phenomenonId2 = phenomenonId.substring(phenomenonIdBase.length());
                stmtExist.setString(1, phenomenonId2);
                try(final ResultSet rs = stmtExist.executeQuery()) {
                    if (rs.next()) {
                        isPartial = rs.getBoolean("partial");
                        exist = true;
                    }
                }
            }

            if (!exist) {
                try(final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"observed_properties\" VALUES(?,?,?,?,?)")) {//NOSONAR
                    stmtInsert.setString(1, phenomenonId);
                    stmtInsert.setBoolean(2, partial);
                    if (phenomenon.getName() != null) {
                        stmtInsert.setString(3, phenomenon.getName());
                    } else {
                        stmtInsert.setNull(3, java.sql.Types.VARCHAR);
                    }
                    stmtInsert.setString(4, phenomenon.getDefinition());
                    if (phenomenon.getDescription() != null) {
                        stmtInsert.setString(5, phenomenon.getDescription());
                    } else {
                        stmtInsert.setNull(5, java.sql.Types.VARCHAR);
                    }
                    stmtInsert.executeUpdate();
                    writeProperties("observed_properties_properties", phenomenonId, phenomenon.getProperties(), c);
                }
                if (phenomenon instanceof CompositePhenomenon composite) {
                    
                    try(final PreparedStatement stmtInsertCompo = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"components\" VALUES(?,?,?)")) {//NOSONAR
                        int cpt = 0;
                        for (org.opengis.observation.Phenomenon childA : composite.getComponent()) {
                            Phenomenon child = (Phenomenon) childA;
                            writePhenomenon(child, c, false);
                            stmtInsertCompo.setString(1, phenomenonId);
                            stmtInsertCompo.setString(2, child.getId());
                            stmtInsertCompo.setInt(3, cpt++);
                            stmtInsertCompo.executeUpdate();
                        }
                    }
                }
            } else if (exist && isPartial) {
                try(final PreparedStatement stmtUpdate = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observed_properties\" SET \"partial\" = ? WHERE \"id\"= ?")) {//NOSONAR
                    stmtUpdate.setBoolean(1, false);
                    stmtUpdate.setString(2, phenomenonId);
                    stmtUpdate.executeUpdate();
                }
                if (phenomenon instanceof CompositePhenomenon composite) {
                    try(final PreparedStatement stmtInsertCompo = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"components\" VALUES(?,?,?)")) {//NOSONAR
                        int cpt = 0;
                        for (org.opengis.observation.Phenomenon childA : composite.getComponent()) {
                            Phenomenon child = (Phenomenon) childA;
                            writePhenomenon(child, c, false);
                            stmtInsertCompo.setString(1, phenomenonId);
                            stmtInsertCompo.setString(2, child.getId());
                            stmtInsertCompo.setInt(3, cpt++);
                            stmtInsertCompo.executeUpdate();
                        }
                    }
                }
            }
            // no real update of a phenomenon is available for now
        }
        return phenomenonId;
    }

    @Override
    public void writeProcedure(final ProcedureDataset procedure) throws DataStoreException {
        try(final Connection c = source.getConnection()) {
            writeProcedure(procedure, null, c);
        } catch (SQLException | FactoryException ex) {
            throw new DataStoreException("Error while inserting procedure.", ex);
        }
    }

    private int writeProcedure(final ProcedureDataset procedure, final String parent, final Connection c) throws SQLException, FactoryException, DataStoreException {
        int pid;
        try(final PreparedStatement stmtExist = c.prepareStatement("SELECT \"pid\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
            stmtExist.setString(1, procedure.getId());
            try(final ResultSet rs = stmtExist.executeQuery()) {
                if (!rs.next()) {
                    try(final Statement stmt = c.createStatement();
                        final ResultSet rs2 = stmt.executeQuery("SELECT max(\"pid\") FROM \"" + schemaPrefix + "om\".\"procedures\"")) {//NOSONAR
                        pid = 0;
                        if (rs2.next()) {
                            pid = rs2.getInt(1) + 1;
                        }
                    }

                    // compute the number of measure table needed
                    int nbTable = procedure.fields.size() / maxFieldByTable;
                    if (procedure.fields.size() % maxFieldByTable > 1) {
                        nbTable++;
                    }

                    try(final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"procedures\" VALUES(?,?,?,?,?,?,?,?,?,?)")) {//NOSONAR
                        stmtInsert.setString(1, procedure.getId());
                        Geometry position = procedure.spatialBound.getLastGeometry();
                        if (position != null) {
                            int srid = position.getSRID();
                            if (srid == 0) {
                                srid = 4326;
                            }
                            stmtInsert.setBytes(2, getGeometryBytes(position));
                            stmtInsert.setInt(3, srid);
                        } else {
                            stmtInsert.setNull(2, java.sql.Types.BINARY);
                            stmtInsert.setNull(3, java.sql.Types.INTEGER);
                        }
                        stmtInsert.setInt(4, pid);
                        if (parent != null) {
                            stmtInsert.setString(5, parent);
                        } else {
                            stmtInsert.setNull(5, java.sql.Types.VARCHAR);
                        }
                        if (procedure.type != null) {
                            stmtInsert.setString(6, procedure.type);
                        } else {
                            stmtInsert.setNull(6, java.sql.Types.VARCHAR);
                        }
                        if (procedure.omType != null) {
                            stmtInsert.setString(7, procedure.omType);
                        } else {
                            stmtInsert.setNull(7, java.sql.Types.VARCHAR);
                        }
                        if (procedure.getName() != null) {
                            stmtInsert.setString(8, procedure.getName());
                        } else {
                            stmtInsert.setNull(8, java.sql.Types.VARCHAR);
                        }
                        if (procedure.getDescription() != null) {
                            stmtInsert.setString(9, procedure.getDescription());
                        } else {
                            stmtInsert.setNull(9, java.sql.Types.VARCHAR);
                        }
                        stmtInsert.setInt(10, nbTable);
                        stmtInsert.executeUpdate();
                    }

                    // write properties
                    writeProperties("procedures_properties", procedure.getId(), procedure.getProperties(), c);

                    // write locations
                    try(final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"historical_locations\" VALUES(?,?,?,?)")) {//NOSONAR
                        for (Entry<Date, Geometry> entry : procedure.spatialBound.getHistoricalLocations().entrySet()) {
                            insertHistoricalLocation(stmtInsert, procedure.getId(), entry);
                        }
                    }
                } else {
                    pid = rs.getInt(1);

                    try(final PreparedStatement stmtHlExist  = c.prepareStatement("SELECT \"procedure\" FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=? AND \"time\"=?");//NOSONAR
                        final PreparedStatement stmtHlInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"historical_locations\" VALUES(?,?,?,?)")) {//NOSONAR
                        stmtHlExist.setString(1, procedure.getId());
                        // write new locations
                        for (Entry<Date, Geometry> entry : procedure.spatialBound.getHistoricalLocations().entrySet()) {
                            final Timestamp ts = new Timestamp(entry.getKey().getTime());
                            stmtHlExist.setTimestamp(2, ts);
                            try (final ResultSet rshl = stmtHlExist.executeQuery()) {
                                if (!rshl.next()) {
                                    insertHistoricalLocation(stmtHlInsert, procedure.getId(), entry);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (ProcedureDataset child : procedure.children) {
            writeProcedure(child, procedure.getId(), c);
        }
        return pid;
    }

    private void insertHistoricalLocation(PreparedStatement stmtInsert, String procedureId, Entry<Date, Geometry> entry) throws SQLException, DataStoreException, FactoryException {
        stmtInsert.setString(1, procedureId);
        stmtInsert.setTimestamp(2, new Timestamp(entry.getKey().getTime()));
        if (entry.getValue() != null) {
            Geometry pt = entry.getValue();
            int srid = pt.getSRID();
            if (srid == 0) {
                srid = 4326;
            }
            stmtInsert.setBytes(3, getGeometryBytes(pt));
            stmtInsert.setInt(4, srid);
        } else {
            stmtInsert.setNull(3, java.sql.Types.BINARY);
            stmtInsert.setNull(4, java.sql.Types.INTEGER);
        }
        stmtInsert.executeUpdate();
    }

    private void writeFeatureOfInterest(final SamplingFeature foi, final Connection c) throws SQLException, DataStoreException {
        if (foi == null) return;
        try(final PreparedStatement stmtExist = c.prepareStatement("SELECT \"id\" FROM  \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?")) {//NOSONAR
            stmtExist.setString(1, foi.getId());
            try (final ResultSet rs = stmtExist.executeQuery()) {
                if (!rs.next()) {
                    try (final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)")) {//NOSONAR
                        stmtInsert.setString(1, foi.getId());
                        stmtInsert.setString(2, foi.getName());
                        stmtInsert.setString(3, foi.getDescription());
                        if (foi.getSampledFeatureId() != null) {
                            stmtInsert.setString(4, foi.getSampledFeatureId());
                        } else {
                            stmtInsert.setNull(4, java.sql.Types.VARCHAR);
                        }
                        if (foi.getGeometry() != null) {
                            final Geometry geom = foi.getGeometry();
                            final int SRID = geom.getSRID();
                            stmtInsert.setBytes(5, getGeometryBytes(foi.getGeometry()));
                            stmtInsert.setInt(6, SRID);
                        } else {
                            stmtInsert.setNull(5, java.sql.Types.VARBINARY);
                            stmtInsert.setNull(6, java.sql.Types.INTEGER);
                        }
                        stmtInsert.executeUpdate();
                    }
                    // write properties
                    writeProperties("sampling_features_properties", foi.getId(), foi.getProperties(), c);
                }
            }
        }
    }

    private static final DbField MEASURE_SINGLE_FIELD = new DbField(1, FieldType.QUANTITY, "value", null, null, null, 1);

    private void writeResult(final int oid, final int pid, final String procedureID, final Result result, final Connection c, boolean update) throws SQLException, DataStoreException {
        if (result instanceof MeasureResult measRes) {
            
            buildMeasureTable(procedureID, pid, Arrays.asList(MEASURE_SINGLE_FIELD),  c);
            if (update) {
                try (final PreparedStatement stmt = c.prepareStatement("UPDATE \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" SET \"value\" = ? WHERE \"id_observation\"= ? AND id = 1")) {//NOSONAR
                    setResultField(stmt, 1, measRes);
                    stmt.setInt(2, oid);
                    stmt.executeUpdate();
                }
            } else {
                try (final PreparedStatement stmt = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "mesures\".\"mesure" + pid + "\" VALUES(?,?,?)")) {//NOSONAR
                    stmt.setInt(1, oid);
                    stmt.setInt(2, 1);
                    setResultField(stmt, 3, measRes);
                    stmt.executeUpdate();
                }
            }
        } else if (result instanceof ComplexResult cr) {
            final TextEncoderProperties encoding = cr.getTextEncodingProperties();
            final List<Field> fields = cr.getFields();
            buildMeasureTable(procedureID, pid, fields, c);

            final String values          = cr.getValues();
            if (values != null && !values.isEmpty()) {
                final List<DbField> dbFields = completeDbField(procedureID, fields.stream().map(f -> f.name).toList(), c);
                OM2MeasureSQLInserter msi    = new OM2MeasureSQLInserter(encoding, pid, schemaPrefix, isPostgres, dbFields);
                msi.fillMesureTable(c, oid, values, update);
            }
        } else if (result != null) {
            throw new DataStoreException("This type of resultat is not supported :" + result.getClass().getName());
        }
    }

    private void setResultField(PreparedStatement stmt, int index, MeasureResult result) throws SQLException {
        switch (result.getField().type) {
            case BOOLEAN  -> stmt.setBoolean(index,   (boolean) result.getValue());
            case QUANTITY -> stmt.setDouble(index,    (double) result.getValue());
            case TEXT     -> stmt.setString(index,    (String) result.getValue());
            case TIME     -> stmt.setTimestamp(index, (Timestamp) result.getValue());
        }
    }

    private void updateOrCreateOffering(final String procedureID, final TemporalObject samplingTime, final String phenoID, final String foiID, final Connection c) throws SQLException {
        final String offeringID;
        try(final PreparedStatement stmtExist = c.prepareStatement("SELECT * FROM  \"" + schemaPrefix + "om\".\"offerings\" WHERE \"procedure\"=?")) {//NOSONAR
            stmtExist.setString(1, procedureID);
            try(final ResultSet rs = stmtExist.executeQuery()) {
                // INSERT
                if (!rs.next()) {
                    // cleanup offering id because of its XML ype (NCName)
                    if (procedureID.startsWith(sensorIdBase)) {
                        offeringID  = "offering-" + procedureID.substring(sensorIdBase.length()).replace(':', '-');
                    } else {
                        offeringID  = "offering-" + procedureID.replace(':', '-');
                    }
                    try(final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offerings\" VALUES(?,?,?,?,?,?)")) {//NOSONAR
                        stmtInsert.setString(1, offeringID);
                        stmtInsert.setString(2, "Offering for procedure:" + procedureID);
                        stmtInsert.setString(3, offeringID);
                        if (samplingTime instanceof Period period) {
                            final Date beginDate = period.getBeginning().getDate();
                            final Date endDate = period.getEnding().getDate();
                            if (beginDate != null) {
                                stmtInsert.setTimestamp(4, new Timestamp(beginDate.getTime()));
                            } else {
                                stmtInsert.setNull(4, java.sql.Types.TIMESTAMP);
                            }
                            if (endDate != null) {
                                stmtInsert.setTimestamp(5, new Timestamp(endDate.getTime()));
                            } else {
                                stmtInsert.setNull(5, java.sql.Types.TIMESTAMP);
                            }
                        } else if (samplingTime instanceof Instant instant) {
                            final Date date = instant.getDate();
                            if (date != null) {
                                stmtInsert.setTimestamp(4, new Timestamp(date.getTime()));
                            } else {
                                stmtInsert.setNull(4, java.sql.Types.TIMESTAMP);
                            }
                            stmtInsert.setNull(5, java.sql.Types.TIMESTAMP);
                        } else {
                            stmtInsert.setNull(4, java.sql.Types.TIMESTAMP);
                            stmtInsert.setNull(5, java.sql.Types.TIMESTAMP);
                        }
                        stmtInsert.setString(6, procedureID);
                        stmtInsert.executeUpdate();
                    }

                    if (phenoID != null) {
                        try(final PreparedStatement stmtInsertOP = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offering_observed_properties\" VALUES(?,?)")) {//NOSONAR
                            stmtInsertOP.setString(1, offeringID);
                            stmtInsertOP.setString(2, phenoID);
                            stmtInsertOP.executeUpdate();
                        }
                    }

                    if (foiID != null) {
                        try(final PreparedStatement stmtInsertFOI = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offering_foi\" VALUES(?,?)")) {//NOSONAR
                            stmtInsertFOI.setString(1, offeringID);
                            stmtInsertFOI.setString(2, foiID);
                            stmtInsertFOI.executeUpdate();
                        }
                    }

                    // UPDATE
                } else {
                    offeringID = rs.getString(1);
                    /*
                     * update time bound
                     */
                    final Timestamp timeBegin = rs.getTimestamp(4);
                    final long offBegin;
                    if (timeBegin != null) {
                        offBegin = timeBegin.getTime();
                    } else {
                        offBegin = Long.MAX_VALUE;
                    }
                    final Timestamp timeEnd = rs.getTimestamp(5);
                    final long offEnd;
                    if (timeEnd != null) {
                        offEnd = timeEnd.getTime();
                    } else {
                        offEnd = -Long.MAX_VALUE;
                    }

                    if (samplingTime instanceof Period period) {
                        final Date beginDate = period.getBeginning().getDate();
                        final Date endDate = period.getEnding().getDate();
                        if (beginDate != null) {
                            final long obsBeginTime = beginDate.getTime();
                            if (obsBeginTime < offBegin) {
                                try(final PreparedStatement beginStmt = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"offerings\" SET \"time_begin\"=? WHERE \"identifier\"=?")) {//NOSONAR
                                    beginStmt.setTimestamp(1, new Timestamp(obsBeginTime));
                                    beginStmt.setString(2, offeringID);
                                    beginStmt.executeUpdate();
                                }
                            }
                        }
                        if (endDate != null) {
                            final long obsEndTime = endDate.getTime();
                            if (obsEndTime > offEnd) {
                                try(final PreparedStatement endStmt = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"offerings\" SET \"time_end\"=? WHERE \"identifier\"=?")) {//NOSONAR
                                    endStmt.setTimestamp(1, new Timestamp(obsEndTime));
                                    endStmt.setString(2, offeringID);
                                    endStmt.executeUpdate();
                                }
                            }
                        }
                    } else if (samplingTime instanceof Instant instant) {
                        final Date date = instant.getDate();
                        if (date != null) {
                            final long obsTime = date.getTime();
                            if (obsTime < offBegin) {
                                try(final PreparedStatement beginStmt = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"offerings\" SET \"time_begin\"=?  WHERE \"identifier\"=?")) {//NOSONAR
                                    beginStmt.setTimestamp(1, new Timestamp(obsTime));
                                    beginStmt.setString(2, offeringID);
                                    beginStmt.executeUpdate();
                                }
                            }
                            if (obsTime > offEnd) {
                                try(final PreparedStatement endStmt = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"offerings\" SET \"time_end\"=?  WHERE \"identifier\"=?")) {//NOSONAR
                                    endStmt.setTimestamp(1, new Timestamp(obsTime));
                                    endStmt.setString(2, offeringID);
                                    endStmt.executeUpdate();
                                }
                            }
                        }
                    }

                    /*
                     * Phenomenon
                     */
                    if (phenoID != null) {
                        try(final PreparedStatement phenoStmt = c.prepareStatement("SELECT \"phenomenon\" FROM  \"" + schemaPrefix + "om\".\"offering_observed_properties\" WHERE \"id_offering\"=? AND \"phenomenon\"=?")) {//NOSONAR
                            phenoStmt.setString(1, offeringID);
                            phenoStmt.setString(2, phenoID);
                            try(final ResultSet rsp = phenoStmt.executeQuery()) {
                                if (!rsp.next()) {
                                    try(final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offering_observed_properties\" VALUES(?,?)")) {//NOSONAR
                                        stmtInsert.setString(1, offeringID);
                                        stmtInsert.setString(2, phenoID);
                                        stmtInsert.executeUpdate();
                                    }
                                }
                            }
                        }
                    }
                }
                /*
                 * Feature Of interest
                 */
                if (foiID != null) {
                    try(final PreparedStatement foiStmt = c.prepareStatement("SELECT \"foi\" FROM  \"" + schemaPrefix + "om\".\"offering_foi\" WHERE \"id_offering\"=? AND \"foi\"=?")) {//NOSONAR
                        foiStmt.setString(1, offeringID);
                        foiStmt.setString(2, foiID);
                        try(final ResultSet rsf = foiStmt.executeQuery()) {
                            if (!rsf.next()) {
                                try(final PreparedStatement stmtInsert = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offering_foi\" VALUES(?,?)")) {//NOSONAR
                                    stmtInsert.setString(1, offeringID);
                                    stmtInsert.setString(2, foiID);
                                    stmtInsert.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void writeOffering(final Offering offering) throws DataStoreException {
        if (offering == null) return;
        try (final Connection c = source.getConnection()) {

            boolean exist;
            try (final PreparedStatement stmtExist = c.prepareStatement("SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" where \"identifier\" = ?")) {//NOSONAR
                stmtExist.setString(1, offering.getId());
                try (final ResultSet rs = stmtExist.executeQuery()) {
                    exist = rs.next();
                }
            }

            String currentStmt;
            if (exist) {
                currentStmt = "UPDATE \"" + schemaPrefix + "om\".\"offerings\" SET VALUES(?,?,?,?,?,?) WHERE \"identifier\"= ?";
            } else {
                currentStmt = "INSERT INTO \"" + schemaPrefix + "om\".\"offerings\" VALUES(?,?,?,?,?,?)";
            }

            try (final PreparedStatement stmt = c.prepareStatement(currentStmt)) {//NOSONAR
                stmt.setString(1, offering.getId());
                stmt.setString(2, offering.getDescription());
                stmt.setString(3, (offering.getName() != null) ? offering.getName() : null);
                if (offering.getTime() instanceof Period period) {
                    if (period.getBeginning() != null && period.getBeginning().getDate() != null) {
                        stmt.setTimestamp(4, new Timestamp(period.getBeginning().getDate().getTime()));
                    } else {
                        stmt.setNull(4, java.sql.Types.TIMESTAMP);
                    }
                    if (period.getEnding() != null && period.getEnding().getDate() != null) {
                        stmt.setTimestamp(5, new Timestamp(period.getEnding().getDate().getTime()));
                    } else {
                        stmt.setNull(5, java.sql.Types.TIMESTAMP);
                    }
                } else if (offering.getTime() instanceof Instant instant) {
                    if (instant != null && instant.getDate() != null) {
                        stmt.setTimestamp(4, new Timestamp(instant.getDate().getTime()));
                    } else {
                        stmt.setNull(4, java.sql.Types.TIMESTAMP);
                    }
                    stmt.setNull(5, java.sql.Types.TIMESTAMP);
                } else {
                    stmt.setNull(4, java.sql.Types.TIMESTAMP);
                    stmt.setNull(5, java.sql.Types.TIMESTAMP);
                }
                stmt.setString(6, offering.getProcedure());
                if (exist) {
                    stmt.setString(7, offering.getId());
                }
                stmt.executeUpdate();

                if (exist) {
                    try (final PreparedStatement opstmt = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"offering_observed_properties\" WHERE \"id_offering\"=?")) {//NOSONAR
                        opstmt.setString(1, offering.getId());
                        opstmt.executeUpdate();
                    }
                    try (final PreparedStatement opstmt = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"offering_foi\" WHERE \"id_offering\"=?")) {//NOSONAR
                        opstmt.setString(1, offering.getId());
                        opstmt.executeUpdate();
                    }
                }
                
                try (final PreparedStatement opstmt = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offering_observed_properties\" VALUES(?,?)")) {//NOSONAR
                    for (String op : offering.getObservedProperties()) {
                        if (op != null) {
                            opstmt.setString(1, offering.getId());
                            opstmt.setString(2, op);
                            opstmt.executeUpdate();
                        }
                    }
                }

                try(final PreparedStatement foistmt = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"offering_foi\" VALUES(?,?)")) {//NOSONAR
                    for (String foi : offering.getFeatureOfInterestIds()) {
                        if (foi != null) {
                            foistmt.setString(1, offering.getId());
                            foistmt.setString(2, foi);
                            foistmt.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while inserting offering.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void recordProcedureLocation(final String physicalID, final Geometry position) throws DataStoreException {
        if (position != null) {
            try(final Connection c     = source.getConnection();
                PreparedStatement ps   = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"procedures\" SET \"shape\"=?, \"crs\"=? WHERE \"id\"=?")) {//NOSONAR
                ps.setString(3, physicalID);
                int srid = position.getSRID();
                if (srid == 0) {
                    srid = 4326;
                }
                ps.setBytes(1, getGeometryBytes(position));
                ps.setInt(2, srid);
                ps.execute();
            } catch (SQLException e) {
                throw new DataStoreException(e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    private int getNewObservationId(Connection c) throws DataStoreException {
        try(final Statement stmt       = c.createStatement();
            final ResultSet rs         = stmt.executeQuery("SELECT max(\"id\") FROM \"" + schemaPrefix + "om\".\"observations\"")) {//NOSONAR
            int resultNum;
            if (rs.next()) {
                resultNum = rs.getInt(1) + 1;
            } else {
                resultNum = 1;
            }
            return resultNum;
        } catch (SQLException ex) {
            throw new DataStoreException("Error while looking for available observation id.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeObservationForProcedure(final String procedureID) throws DataStoreException {
        try (final Connection c = source.getConnection()) {
            removeObservationForProcedure(procedureID, c);
        } catch (SQLException ex) {
            throw new DataStoreException("Error while removing observation for procedure.", ex);
        }
    }

    private synchronized void removeObservationForProcedure(final String procedureID, Connection c) throws DataStoreException, SQLException {
        final int[] pidNumber = getPIDFromProcedure(procedureID, c);

        // remove from measures tables
        for (int i = 0; i < pidNumber[1]; i++) {
            String suffix = pidNumber[0] + "";
            if (i > 0) {
                suffix = suffix + "_" + (i + 1);
            }

            boolean mesureTableExist = true;
            try(final PreparedStatement stmtExist  = c.prepareStatement("SELECT COUNT(\"id\") FROM \"" + schemaPrefix + "mesures\".\"mesure" + suffix + "\"")) {//NOSONAR
                stmtExist.executeQuery();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "no measure table mesure{0} exist.", suffix);
                mesureTableExist = false;
            }

            if (mesureTableExist) {
                try (final PreparedStatement stmtMes  = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "mesures\".\"mesure" + suffix + "\" WHERE \"id_observation\" IN (SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?)")) {//NOSONAR

                    stmtMes.setString(1, procedureID);
                    stmtMes.executeUpdate();
                }
            }
        }

        // remove from observation table
        try(final PreparedStatement stmtObs  = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"procedure\"=?")) {//NOSONAR

            stmtObs.setString(1, procedureID);
            stmtObs.executeUpdate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeProcedure(final String procedureID) throws DataStoreException {
        try {
            try (final Connection c = source.getConnection()) {

                deleteProperties("procedures_properties", "id_procedure", procedureID, c);
                removeObservationForProcedure(procedureID, c);
                
                // remove measure tables
                try (final Statement stmtDrop = c.createStatement()) {
                    final int[] pidNumber = getPIDFromProcedure(procedureID, c);

                    for (int i = 0; i < pidNumber[1]; i++) {
                        String suffix = pidNumber[0] + "";
                        if (i > 0) {
                            suffix = suffix + "_" + (i + 1);
                        }
                        stmtDrop.executeUpdate("DROP TABLE \"" + schemaPrefix + "mesures\".\"mesure" + suffix + "\"");//NOSONAR
                    }
                }  catch (SQLException ex) {
                    // it happen that the table does not exist
                    LOGGER.log(Level.WARNING, "Unable to remove measure table.{0}", ex.getMessage());
                }
                
                try (final PreparedStatement stmtObsP = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"offering_observed_properties\" WHERE \"id_offering\" IN(SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" WHERE \"procedure\"=?)");//NOSONAR
                     final PreparedStatement stmtFoi = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"offering_foi\" WHERE \"id_offering\" IN(SELECT \"identifier\" FROM \"" + schemaPrefix + "om\".\"offerings\" WHERE \"procedure\"=?)");//NOSONAR
                     final PreparedStatement stmtHl  = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"historical_locations\" WHERE \"procedure\"=?");//NOSONAR
                     final PreparedStatement stmtMes = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"offerings\" WHERE \"procedure\"=?");//NOSONAR
                     final PreparedStatement stmtObs = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?");//NOSONAR
                     final PreparedStatement stmtProcDesc = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"procedure_descriptions\" WHERE \"procedure\"=?")) {//NOSONAR

                    stmtObsP.setString(1, procedureID);
                    stmtObsP.executeUpdate();

                    stmtFoi.setString(1, procedureID);
                    stmtFoi.executeUpdate();

                    stmtHl.setString(1, procedureID);
                    stmtHl.executeUpdate();

                    stmtMes.setString(1, procedureID);
                    stmtMes.executeUpdate();

                    stmtProcDesc.setString(1, procedureID);
                    stmtProcDesc.executeUpdate();

                    stmtObs.setString(1, procedureID);
                    stmtObs.executeUpdate();
                }

                final String cleanOPQUery = " SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observed_properties\""
                                          + " WHERE  \"id\" NOT IN (SELECT DISTINCT \"observed_property\" FROM \"" + schemaPrefix + "om\".\"observations\") "
                                          + " AND    \"id\" NOT IN (SELECT DISTINCT \"phenomenon\"        FROM \"" + schemaPrefix + "om\".\"offering_observed_properties\")"
                                          + " AND    \"id\" NOT IN (SELECT DISTINCT \"component\"         FROM \"" + schemaPrefix + "om\".\"components\")";

                final String cleanFOIQUery = " SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"sampling_features\""
                                           + " WHERE \"id\" NOT IN (SELECT DISTINCT \"foi\" FROM \"" + schemaPrefix + "om\".\"observations\") "
                                           + " AND \"id\" NOT IN (SELECT DISTINCT \"foi\" FROM \"" + schemaPrefix + "om\".\"offering_foi\")";

                //look for unused observed properties (execute the statement 2 times for remaining components)
                try (final Statement stmtOP = c.createStatement()) {
                    for (int i = 0; i < 2; i++) {
                        try (final ResultSet rs = stmtOP.executeQuery(cleanOPQUery)) {//NOSONAR

                            try (final PreparedStatement stmtdeleteCompo = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"phenomenon\"=?");//NOSONAR
                                 final PreparedStatement stmtdeleteobsPr = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"observed_properties\" WHERE \"id\"=?")) {//NOSONAR
                                while (rs.next()) {
                                    final String phenId = rs.getString(1);
                                    deleteProperties("observed_properties_properties", "id_phenomenon", phenId, c);
                                    stmtdeleteCompo.setString(1, phenId);
                                    stmtdeleteCompo.execute();
                                    stmtdeleteobsPr.setString(1, phenId);
                                    stmtdeleteobsPr.execute();
                                }
                            }
                        }
                    }

                    //look for unused foi
                    try (final Statement stmtFOI = c.createStatement();
                         final ResultSet rs2 = stmtFOI.executeQuery(cleanFOIQUery)) {//NOSONAR
                        try (final PreparedStatement stmtdeletefoi = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"sampling_features\" WHERE \"id\"=?")) {//NOSONAR
                            while (rs2.next()) {
                                String foiId = rs2.getString(1);
                                deleteProperties("sampling_features_properties", "id_sampling_feature", foiId, c);
                                stmtdeletefoi.setString(1, foiId);
                                stmtdeletefoi.execute();
                            }
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Error while removing procedure.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeObservation(final String observationID) throws DataStoreException {
        try(final Connection c     = source.getConnection()) {
            final int[] pidNumber  = getPIDFromObservation(observationID, c);
            final int pid          = pidNumber[0];
            final int nbTable      = pidNumber[1];

            // remove from measure tables
            for (int i = 0; i < nbTable; i++) {
                String suffix = pid + "";
                if (i > 0) {
                    suffix = suffix + "_" + (i + 1);
                }
                try (final PreparedStatement stmtMes = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "mesures\".\"mesure" + suffix + "\" WHERE id_observation IN (SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE identifier=?)")){//NOSONAR
                    stmtMes.setString(1, observationID);
                    stmtMes.executeUpdate();
                }
            }
            
            // remove from observation table
            try(final PreparedStatement stmtObs = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"observations\" WHERE identifier=?")) {//NOSONAR
                stmtObs.setString(1, observationID);
                stmtObs.executeUpdate();
            }
            
        } catch (SQLException ex) {
            throw new DataStoreException("Error while inserting observation.", ex);
        }
    }

    private boolean measureTableExist(final int pid) throws SQLException {
        final String tableName = "mesure" + pid;
        try (final Connection c = source.getConnection();
             final Statement stmt = c.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT \"id\" FROM \"" + schemaPrefix + "mesures\".\"" + tableName + "\"")) {//NOSONAR
            // if no exception this mean that the table exist
            return true;
        } catch (SQLException ex) {
            LOGGER.log(Level.FINER, "Error while looking for measure table existence (normal error if table does not exist).", ex);
        }
        return false;
    }

    private Entry<String,StringBuilder> createTableBaseInsert(int i, String baseTableName) {
        String suffix = "";
        if (i > 1) {
            suffix = "_" + i;
        }
        final String tableName = baseTableName + suffix;
        final StringBuilder sb = new StringBuilder("CREATE TABLE \"" + schemaPrefix + "mesures\".\"" + baseTableName + suffix + "\"("
                                                 + "\"id_observation\" integer NOT NULL,"
                                                 + "\"id\"             integer NOT NULL,");
        return new AbstractMap.SimpleEntry<>(tableName, sb);
    }

    private void buildMeasureTable(final String procedureID, final int pid, final List<Field> fields, final Connection c) throws SQLException, DataStoreException {
        if (fields == null || fields.isEmpty()) {
            throw new DataStoreException("measure fields can not be empty");
        }
        final String baseTableName = "mesure" + pid;
        

        //look for existence
        final boolean exist = measureTableExist(pid);
        if (!exist) {
            final List<DbField> dbFields = new ArrayList<>();
            Map<Integer, Entry<String, StringBuilder>> creationTablesStmt = new LinkedHashMap<>();
            boolean firstField = true;
            Field mainField    = null;
            int fieldCpt       = 0;
            int nbTable        = 0;
            int nbTabField     = -1;

            // Build measures tables
            while (fieldCpt < fields.size()) {
                
                 // create new table
                if (nbTabField == -1 || nbTabField > maxFieldByTable) {
                    nbTable++;
                    creationTablesStmt.put(nbTable, createTableBaseInsert(nbTable, baseTableName));
                    nbTabField = 0;
                }
                StringBuilder sb = creationTablesStmt.get(nbTable).getValue();

                final Field field = fields.get(fieldCpt);
                dbFields.add(new DbField(field, nbTable));

                if (Util.containsForbiddenCharacter(field.name)) {
                    throw new DataStoreException("Invalid field name");
                }
                sb.append('"').append(field.name).append("\" ").append(field.getSQLType(isPostgres, firstField && timescaleDB));
                // main field should not be null (timescaledb compatibility)
                if (firstField) {
                    mainField = field;
                    sb.append(" NOT NULL");
                    firstField = false;
                }
                sb.append(",");
                if (field.qualityFields != null && !field.qualityFields.isEmpty()) {
                    for (Field qField : field.qualityFields) {
                        String columnName = field.name + "_quality_" + qField.name;
                        sb.append('"').append(columnName).append("\" ").append(qField.getSQLType(isPostgres, false));
                        sb.append(",");
                        nbTabField++;
                    }
                }
                fieldCpt++;
                nbTabField++;
            }

            if (mainField == null) {
                throw new DataStoreException("No main field was found");
            }
            
            try (final Statement stmt = c.createStatement()) {

                // build measures tables
                boolean first = false;
                for (Entry<String, StringBuilder> entry : creationTablesStmt.values()) {
                    String tableName = entry.getKey();
                    StringBuilder tableStsmt = entry.getValue();
                    // close statement
                    tableStsmt.setCharAt(tableStsmt.length() - 1, ' ');
                    tableStsmt.append(")");
                    
                    stmt.executeUpdate(tableStsmt.toString());

                    String mainFieldPk = "";
                    if (first) {
                        mainFieldPk = ", \"" + mainField.name + "\"";
                        first = false;
                    }
                    // main field should not be in the primary phenId (timescaledb compatibility)
                    stmt.executeUpdate("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD CONSTRAINT " + tableName + "_pk PRIMARY KEY (\"id_observation\", \"id\"" + mainFieldPk + ")");//NOSONAR
                    stmt.executeUpdate("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD CONSTRAINT " + tableName + "_obs_fk FOREIGN KEY (\"id_observation\") REFERENCES \"" + schemaPrefix + "om\".\"observations\"(\"id\")");//NOSONAR

                    //only for timeseries for now
                    if (timescaleDB && FieldType.TIME.equals(mainField.type)) {
                        stmt.execute("SELECT create_hypertable('" + schemaPrefix + "mesures." + baseTableName + "', '" + mainField.name + "')");//NOSONAR
                    }
                }

                // update table count
                stmt.executeUpdate("UPDATE \"" + schemaPrefix + "om\".\"procedures\" SET \"nb_table\" = " + nbTable + " WHERE \"id\"='" + procedureID + "'");//NOSONAR
            }

            //fill procedure_descriptions table
            insertFields(procedureID, dbFields, 1, c);


        } else if (allowSensorStructureUpdate) {
            final int [] pidNumber       = getPIDFromProcedure(procedureID, c);
            final int nbTable            = pidNumber[1];
            final List<Field> oldfields  = readFields(procedureID, false, c);
            final List<Field> newfields  = new ArrayList<>();
            final List<DbField> dbFields = new ArrayList<>();
            for (Field field : fields) {
                if (!oldfields.contains(field)) {
                    newfields.add(field);
                    dbFields.add(new DbField(field, nbTable)); // TODO. see comment under
                }
            }

            /**
             * Update measure table by adding new fields.
             *
             * TODO: this update mode actually do not handle the multi measure table mecanism.
             * It will add all the new fields to the last measure table.
             * we need to handle when the next fields reach the max comlumn limit and then, create a new table.
             */
            try (Statement addColumnStmt = c.createStatement()) {
                String tableName = baseTableName;
                if (nbTable > 1) {
                    tableName = baseTableName + "_" + nbTable;
                }
                for (Field newField : newfields) {
                    StringBuilder sb = new StringBuilder("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD \"" + newField.name + "\" ");
                    sb.append(newField.getSQLType(isPostgres, false));
                    addColumnStmt.execute(sb.toString());

                    if (newField.qualityFields != null && !newField.qualityFields.isEmpty()) {
                        for (Field qField : newField.qualityFields) {
                            String columnName = newField.name + "_quality_" + qField.name;
                            StringBuilder qsb = new StringBuilder("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD \"" + columnName + "\" ");
                            qsb.append(qField.getSQLType(isPostgres, false));
                            addColumnStmt.execute(qsb.toString());
                        }
                    }
                }
            }

            //fill procedure_descriptions table
            insertFields(procedureID, dbFields, oldfields.size() + 1, c);
        }
    }

    private void insertFields(String procedureID, List<DbField> fields, int offset, final Connection c) throws SQLException {
        try (final PreparedStatement insertFieldStmt = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"procedure_descriptions\" VALUES (?,?,?,?,?,?,?,?)")) {//NOSONAR
            for (DbField field : fields) {
                insertField(insertFieldStmt, procedureID, field, null, offset);
                if (field.qualityFields != null) {
                    int qOffset = 1;
                    for (Field qfield : field.qualityFields) {
                        insertField(insertFieldStmt, procedureID, (DbField)qfield, field.name, qOffset);
                        qOffset++;
                    }
                }
                offset++;
            }
        }
    }

    private void insertField(PreparedStatement insertFieldStmt, String procedureID, DbField field, String parent, int offset) throws SQLException {
        insertFieldStmt.setString(1, procedureID);
        insertFieldStmt.setInt(2, offset);
        insertFieldStmt.setString(3, field.name);
        insertFieldStmt.setString(4, field.type.label);
        if (field.description != null) {
            insertFieldStmt.setString(5, field.description);
        } else {
            insertFieldStmt.setNull(5, java.sql.Types.VARCHAR);
        }
        if (field.uom != null) {
            insertFieldStmt.setString(6, field.uom);
        } else {
            insertFieldStmt.setNull(6, java.sql.Types.VARCHAR);
        }
        if (parent != null) {
            insertFieldStmt.setString(7, parent);
        } else {
            insertFieldStmt.setNull(7, java.sql.Types.VARCHAR);
        }
        insertFieldStmt.setInt(8, field.tableNumber);
        insertFieldStmt.executeUpdate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {

    }
}
