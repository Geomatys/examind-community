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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.constellation.dto.service.config.sos.OM2ResultEventDTO;
import static org.constellation.store.observation.db.OM2BaseReader.LOGGER;
import static org.constellation.store.observation.db.OM2Utils.*;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import org.constellation.util.SingleFilterSQLRequest;
import org.constellation.util.Util;
import org.geotoolkit.observation.OMUtils;
import static org.geotoolkit.observation.OMUtils.OBSERVATION_QNAME;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.MeasureResult;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.Offering;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.Result;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.model.TextEncoderProperties;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalGeometricPrimitive;



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
        public final String foiId;
        public final Timestamp startTime;
        public final Timestamp endTime;
        public final Timestamp extendStartTime;
        public final Timestamp extendEndTime;

        public ObservationRef(int id, String name, String phenomenonId, String foiId, Timestamp startTime, Timestamp endTime, Timestamp extendStartTime, Timestamp extendEndTime) {
            this.id = id;
            this.name = name;
            this.phenomenonId = phenomenonId;
            this.foiId  = foiId;
            this.endTime = endTime;
            this.startTime = startTime;
            this.extendStartTime = extendStartTime;
            this.extendEndTime = extendEndTime;
        }
    }

    private List<ObservationRef> isConflicted(final Connection c, final String procedureID, final TemporalObject samplingTime, final String foiID) throws DataStoreException {
        List<ObservationRef> obs = new ArrayList<>();
        if (samplingTime != null) {
            FilterSQLRequest sqlRequest = new SingleFilterSQLRequest("SELECT \"id\", \"identifier\", \"observed_property\", \"time_begin\", \"time_end\", \"foi\" FROM \"" + schemaPrefix + "om\".\"observations\" o WHERE ");
            sqlRequest.append(" \"procedure\"=").appendValue(procedureID);

            // i'm suspicious about this
            if (foiID != null) {
                sqlRequest.append(" AND \"foi\"=").appendValue(foiID);
            }

            FilterSQLRequest sqlConflictRequest = sqlRequest.clone();
            sqlConflictRequest.append(" AND ( ");
            addtimeDuringSQLFilter(sqlConflictRequest, samplingTime, "o");
            sqlConflictRequest.append(" ) ");
            
            try (final SQLResult rs = sqlConflictRequest.execute(c)) {
                while (rs.next()) {
                    // look for observation time extension
                    Timestamp extBegin = null;
                    Timestamp extEnd   = null;
                    final Timestamp obsBegin = rs.getTimestamp("time_begin");
                    final Timestamp obsEnd   = rs.getTimestamp("time_end");
                    if (samplingTime instanceof Period p) {
                        final Timestamp newBegin = getInstantTimestamp(p.getBeginning());
                        final Timestamp newEnd   = getInstantTimestamp(p.getEnding());
                        if (newBegin.before(obsBegin)) {
                            extBegin = newBegin;
                        }
                        if (obsEnd == null || newEnd.after(obsEnd)) {
                            extEnd = newEnd;
                        }
                    }
                    obs.add(new ObservationRef(rs.getInt("id"), rs.getString("identifier"), rs.getString("observed_property"), rs.getString("foi"),
                            obsBegin, obsEnd, extBegin, extEnd));
                    
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
                        return obs;
                    // this means there are already conflicted observation this should not happen in a sane database.
                    } else {
                        throw new DataStoreException("The observation is in temporal conflict with other (already conflicting observation present)");
                    }

                } else if (samplingTime instanceof Period p) {
                     return obs;

                // unexpected case
                } else {
                    throw new DataStoreException("Unpected sampling time implementation." + samplingTime.getClass().getName());
                }
            }
        }
        return obs;
    }

    private String writeObservation(final Observation observation, final Connection c, final int generatedID) throws DataStoreException {
        // look for an conflicted observation
        final Procedure procedure  = observation.getProcedure();
        final String procedureID   = procedure.getId();
        final String procedureName = procedure.getName();
        final String procedureDesc = procedure.getDescription();
        final String procedureOMType = (String) procedure.getProperties().getOrDefault("type", "timeseries");
        
        final TemporalObject samplingTime = observation.getSamplingTime();

        final SamplingFeature foi = observation.getFeatureOfInterest();
        String foiID = null;
        if (foi != null) {
            foiID = foi.getId();
        }

        final Phenomenon phenomenon = observation.getObservedProperty();

        List<ObservationRef> conflictedObservations = isConflicted(c, procedureID, samplingTime, foiID);
        
        final String phenRef;
        final String observationName;

        try (final PreparedStatement insertObs   = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"observations\" VALUES(?,?,?,?,?,?,?)");                  //NOSONAR
             final PreparedStatement updatePhen  = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"observed_property\" = ? WHERE \"id\" = ?");//NOSONAR
             final PreparedStatement updateBegin = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"time_begin\" = ? WHERE \"id\" = ?");//NOSONAR
             final PreparedStatement updateEnd   = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"time_end\" = ? WHERE \"id\" = ?")) { //NOSONAR

            /*
             * insert a new observation
             */
            if (conflictedObservations.isEmpty()) {
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

                // TODO use new procedureDataset constructor when geotk is updated
                final ProcedureInfo pi = writeProcedure(new ProcedureDataset(procedureID, procedureName, procedureDesc, null, procedureOMType, new ArrayList<>(), procedure.getProperties()), null, c);
                insertObs.setString(6, procedureID);
                if (foiID != null) {
                    insertObs.setString(7, foiID);
                    writeFeatureOfInterest(foi, c);
                } else {
                    insertObs.setNull(7, java.sql.Types.VARCHAR);
                }
                insertObs.executeUpdate();
                writeResult(oid, pi, observation.getResult(), c, false);
                emitResultOnBus(procedureID, observation.getResult());

            } else {
                final ProcedureInfo pi = getPIDFromProcedure(procedureID, c).get(); //we know that the procedure exist
                Timestamp begin, end;
                ObservationRef replacingObs = conflictedObservations.get(0);
                int modOid                 = replacingObs.id;
                observationName            = replacingObs.name;
                boolean replacePhen        = false;
                
                // write the new phenomenon even if its not actually used in the observation
                // for a composite, we need to write at least the components
                String newPhen = writePhenomenon(phenomenon, c, false);

                /*
                * update an existing observation
                */
                if (conflictedObservations.size() == 1) {
                    
                    writeResult(modOid, pi, observation.getResult(), c, true);

                    replacePhen = !replacingObs.phenomenonId.equals(newPhen);
                    begin       = replacingObs.extendStartTime;
                    end         = replacingObs.extendEndTime;

                /*
                 * update multiple existing observation
                 */
                } else {

                    //we cannot merge observations with different foi.
                    for (int i = 1; i < conflictedObservations.size(); i++) {
                        ObservationRef obs = conflictedObservations.get(i);
                        if (!Objects.equals(obs.foiId, replacingObs.foiId)) {
                            throw new DataStoreException("The observation is in temporal conflict with multiple observations. Multiple feature of interest are involved. Unable to perform the insertion");
                        }
                    }

                    if (samplingTime instanceof Period p) {
                        begin = getInstantTimestamp(p.getBeginning());
                        end   = getInstantTimestamp(p.getEnding());
                    } else {
                        throw new IllegalStateException("An observation can not be in conflict with multiple others and have a instant or null sampling time.");
                    }

                    // we need to merge all the conflicted observation in one
                    for (int i = 0; i < conflictedObservations.size(); i++) {
                        ObservationRef obs = conflictedObservations.get(i);
                        if (begin == null || obs.startTime.before(begin)) {
                            begin = obs.startTime;
                        }
                        if (obs.endTime != null && (end == null || obs.endTime.after(end))) {
                            end = obs.endTime;
                        }
                        boolean phenChange = !obs.phenomenonId.equals(newPhen);
                        if (phenChange) replacePhen = true;
                        if (i >= 1) {
                            Result result = getResult(obs.id, OBSERVATION_QNAME, null, null, c);
                            writeResult(modOid, pi, result, c, true);

                            removeObservation(obs.id, pi, c);
                        }

                    }
                    // write the new observation into the merged one
                    writeResult(modOid, pi, observation.getResult(), c, true);
                }

                // if a new phenomenon has been added we must create a composite and change the observation reference
                // for now we write the full procedure phenomenon. we should build a more precise phenomenon
                if (replacePhen) {
                    List<Field> readFields = readFields(procedureID, true, c);
                    Phenomenon replacingPhen = OMUtils.getPhenomenonModels(null, readFields, phenomenonIdBase, getAllPhenomenon(c));

                    phenRef = writePhenomenon(replacingPhen, c, false);
                    updatePhen.setString(1, phenRef);
                    updatePhen.setInt(2, modOid);
                    updatePhen.executeUpdate();
                } else {
                    phenRef = newPhen;
                }

                //update observation bounds
                if (begin != null) {
                    updateBegin.setTimestamp(1, begin);
                    updateBegin.setInt(2, modOid);
                    updateBegin.executeUpdate();
                }
                if (end != null) {
                    updateEnd.setTimestamp(1, end);
                    updateEnd.setInt(2, modOid);
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
                stmt.setString(3,  Objects.toString(entry.getValue()));
                stmt.addBatch();
            }
            stmt.executeBatch();
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
        if (phenomenon ==null || phenomenon.getId() == null) return null;

        final String phenomenonId = phenomenon.getId();
        try (final PreparedStatement stmtExist = c.prepareStatement("SELECT \"id\", \"partial\" FROM  \"" + schemaPrefix + "om\".\"observed_properties\" WHERE \"id\"=?")) {//NOSONAR
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
            } else {
                // not a complete update, only properties
                deleteProperties("observed_properties_properties", "id_phenomenon", phenomenonId, c);
                writeProperties("observed_properties_properties", phenomenonId, phenomenon.getProperties(), c);
            }
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

    private ProcedureInfo writeProcedure(final ProcedureDataset procedure, final String parent, final Connection c) throws SQLException, FactoryException, DataStoreException {
        int pid;
        int nbTable;
        try(final PreparedStatement stmtExist = c.prepareStatement("SELECT \"pid\", \"nb_table\" FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\"=?")) {//NOSONAR
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
                    nbTable = procedure.fields.size() / maxFieldByTable;
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
                    nbTable = rs.getInt(2);
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
        return new ProcedureInfo(pid, nbTable, procedure.getId(), procedure.omType);
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

    /**
     * Write  an observation result.
     *
     * @param oid Observation identifier.
     * @param pi Informations about the procedure owning the observation.
     * @param result
     * @param c
     * @param update
     * @throws SQLException
     * @throws DataStoreException
     */
    private void writeResult(final int oid, final ProcedureInfo pi, final Result result, final Connection c, boolean update) throws SQLException, DataStoreException {
        if (result instanceof MeasureResult measRes) {
            
            buildMeasureTable(pi, Arrays.asList(MEASURE_SINGLE_FIELD),  c);
            if (update) {
                try (final PreparedStatement stmt = c.prepareStatement("UPDATE \"" + schemaPrefix + "mesures\".\"mesure" + pi.pid + "\" SET \"value\" = ? WHERE \"id_observation\"= ? AND id = 1")) {//NOSONAR
                    setResultField(stmt, 1, measRes);
                    stmt.setInt(2, oid);
                    stmt.executeUpdate();
                }
            } else {
                try (final PreparedStatement stmt = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "mesures\".\"mesure" + pi.pid + "\" VALUES(?,?,?)")) {//NOSONAR
                    stmt.setInt(1, oid);
                    stmt.setInt(2, 1);
                    setResultField(stmt, 3, measRes);
                    stmt.executeUpdate();
                }
            }
        } else if (result instanceof ComplexResult cr) {
            final List<Field> fields = cr.getFields();
            buildMeasureTable(pi, fields, c);

            final String values          = cr.getValues();
            if (values != null && !values.isEmpty()) {
                final List<InsertDbField> dbFields = completeDbField(pi.procedureId, fields, c);
                OM2MeasureSQLInserter msi    = new OM2MeasureSQLInserter(pi, schemaPrefix, isPostgres, dbFields);
                msi.fillMesureTable(c, oid, cr, update);
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
        final ProcedureInfo pi = getPIDFromProcedure(procedureID, c).orElse(null);
        if (pi != null) {
            // remove from measures tables
            for (int i = 0; i < pi.nbTable; i++) {
                String suffix = Integer.toString(pi.pid);
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized List<String> removeDataSet(ObservationDataset dataset) throws DataStoreException {
        List<String> sensorRemoved = new ArrayList<>();
        try (final Connection c = source.getConnection()) {

            for (Observation obs : dataset.observations) {
                // 1. look for intersecting observation based on time and phenomenon
                Set<ObservationInfos> candidates = getIntersectingObservation(obs, c);

                // 2. look for each observation if we should remove it, totally or partially
                for (ObservationInfos cdt : candidates) {
                    final RelativePosition pos = obs.getSamplingTime().relativePosition(cdt.time);

                    LOGGER.fine("Observation candidate id:" + cdt.id + " phen:" + cdt.phenomenon.getId() + " time related:" + pos.name());

                    boolean timeEquals = pos.equals(RelativePosition.EQUALS);
                    boolean fullRemoval  = timeEquals && (getMeasureCount(obs) == cdt.nbMeasure);

                    /*
                     * case 1: the time span is equals and the measure count is equals.
                     * This is not perfect and can lead to bad removal.
                     * But in most of the case this will do the job with minimum time/resource comsumption.
                     */
                    if (fullRemoval) {

                        // case 1.1: simple case. observation has the same phenomenon or the phenomenon is a subset.
                        if (OM2Utils.isEqualsOrSubset(cdt.phenomenon, obs.getObservedProperty())) {
                            removeObservation(cdt.id, cdt.pi, c);
                            
                            // remove procedure if needed
                            boolean removed = removeProcedureIfEmpty(cdt.pi, c);
                            if (removed) sensorRemoved.add(cdt.pi.procedureId);

                        // case 1.2 phenomenon to remove is part of the original phenomenon we need to remove only a part of the observation.
                        } else if (isPartOf(cdt.phenomenon, obs.getObservedProperty())) {

                            final List<Field> fields                 = getMeasureFields(obs);
                            final List<InsertDbField> fieldsToRemove = completeDbField(cdt.pi.procedureId, fields, c);
                            OM2MeasureFieldRemover remover            = new OM2MeasureFieldRemover(cdt, schemaPrefix, fieldsToRemove);
                            remover.removeMeasures(c);
                            
                            int nbRemoved = removeEmptyMeasures(cdt, c);
                            if (nbRemoved > 0) {
                                final Field mainField = getMainField(cdt.pi.procedureId, c);
                                updateObservationTemporalBounds(cdt, mainField, c);
                            }
                            updateObservationPhenomenon(cdt, fieldsToRemove, c);
                        } else {
                            LOGGER.fine("Full removal mode: no intersecting phenomenon. no deletion");
                        }

                    // case 2: the time span is intersecting
                    } else if (timeEquals || isTimeIntersect(pos)) {

                        final Field mainField = getMainField(cdt.pi.procedureId, c);

                        // case 2.1: simple case. observation has the same phenomenon or the phenomenon is a subset.
                        // so we remove all the measure line that match the main field
                        if (OM2Utils.isEqualsOrSubset(cdt.phenomenon, obs.getObservedProperty())) {
                            OM2MeasureRemover remover = new OM2MeasureRemover(cdt, mainField, schemaPrefix);
                            remover.removeMeasures(c, obs);
                            boolean rmo = removeObservationIfEmpty(cdt, c);
                            if (!rmo) {
                                updateObservationTemporalBounds(cdt, mainField, c);
                                List<Field> emptyFields = getEmptyFieldsForObservation(cdt, c);
                                if (!emptyFields.isEmpty()) {
                                    updateObservationPhenomenon(cdt, emptyFields, c);
                                }
                            } else {
                                boolean removed = removeProcedureIfEmpty(cdt.pi, c);
                                if (removed) sensorRemoved.add(cdt.pi.procedureId);
                            }

                        // case 2.2 phenomenon to remove is part of the original phenomenon we need to remove only a part of the observation.
                        } else if (isPartOf(cdt.phenomenon, obs.getObservedProperty())) {
                            final List<Field> fields                 = getMeasureFields(obs);
                            final List<InsertDbField> fieldsToRemove = completeDbField(cdt.pi.procedureId, fields, c);
                            OM2MeasureFieldFilteredRemover remover    = new OM2MeasureFieldFilteredRemover(cdt, mainField, schemaPrefix, fieldsToRemove);
                            remover.removeMeasures(c, obs);

                            removeEmptyMeasures(cdt, c);
                            updateObservationTemporalBounds(cdt, mainField, c);

                            // the phenomenon may be updated if some fields are now empty
                            List<Field> emptyFields = getEmptyFieldsForObservation(cdt, c);
                            if (!emptyFields.isEmpty()) {
                                updateObservationPhenomenon(cdt, emptyFields, c);
                            }
                           
                        } else {
                            LOGGER.fine("Fine removal mode: no intersecting phenomenon. no deletion");
                        }
                    } else {
                        LOGGER.fine("No intersecting time: " + pos);
                    }
                }
            }
            
        } catch (SQLException ex) {
            throw new DataStoreException("Error while removing observation Dataset.", ex);
        }
        return sensorRemoved;
    }

    public static class ObservationInfos {
        public final int id;
        public final int nbMeasure;
        public final String identifier;
        public final TemporalGeometricPrimitive time;
        public final Phenomenon phenomenon;
        public final ProcedureInfo pi;

        public ObservationInfos(int id, String identifier, TemporalGeometricPrimitive time, Phenomenon phenomenon, int nbMeasure, ProcedureInfo pi) {
            this.id         = id;
            this.identifier = identifier;
            this.time       = time;
            this.phenomenon = phenomenon;
            this.nbMeasure  = nbMeasure;
            this.pi         = pi;
        }
    }

    private Set<ObservationInfos> getIntersectingObservation(Observation obs, Connection c) throws SQLException, DataStoreException {
        FilterSQLRequest sql = new SingleFilterSQLRequest("SELECT \"id\", \"identifier\", \"time_begin\", \"time_end\", \"observed_property\" FROM  \"" + schemaPrefix + "om\".\"observations\" o WHERE ");
        // procedure match
        sql.append("o.\"procedure\" = ").appendValue(obs.getProcedure().getId()).append(" AND ");
        // phenomenon match
        Set<String> intersectingPhen = getIntersectingPhenomenon(obs.getObservedProperty(), c);
        if (intersectingPhen.isEmpty()) {
            return new HashSet<>();
        }
        sql.append("( o.\"observed_property\" IN ( ").appendValues(intersectingPhen).append(") )");
        // time matching
        sql.append(" AND (");
        OM2Utils.addtimeDuringSQLFilter(sql, obs.getSamplingTime(), "o");
        sql.append(" ) ");

        Set<ObservationInfos> results = new HashSet<>();
        try (final SQLResult result = sql.execute(c)) {
            while (result.next()) {
                TemporalGeometricPrimitive time = OMUtils.buildTime("id", result.getTimestamp("time_begin"), result.getTimestamp("time_end"));
                Phenomenon phenomenon = getPhenomenon(result.getString("observed_property"), c);
                final int obsId = result.getInt("id");
                final String identifier = result.getString("identifier");
                final ProcedureInfo pi = getPIDFromOID(obsId, c).orElseThrow(IllegalStateException::new);
                int nbMeasure = getNbMeasureForObservation(pi.pid, obsId, c);
                results.add(new ObservationInfos(obsId, identifier, time, phenomenon, nbMeasure, pi));
            }
        }
        return results;
    }

    private Set<String> getIntersectingPhenomenon(Phenomenon phen, Connection c) throws SQLException {
        if (phen == null) return new HashSet<>();
        FilterSQLRequest sql = new SingleFilterSQLRequest("SELECT DISTINCT(\"id\") FROM  \"" + schemaPrefix + "om\".\"observed_properties\" op WHERE ");

        // look for a direct phenomenon use, or a single component use
        sql.append(" \"id\" = ").appendValue(phen.getId());
        if (phen instanceof CompositePhenomenon cPhen) {
            for (Phenomenon compo : cPhen.getComponent()) {
                sql.append( "OR \"id\" = ").appendValue(compo.getId());
            }
        }
        sql.append(" OR  \"id\" IN ( SELECT \"phenomenon\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE ");

        if (phen instanceof CompositePhenomenon cPhen && !cPhen.getComponent().isEmpty()) {
            // look for other composite containing a component of the searched one
            sql.append("( \"component\" = ").appendValue(cPhen.getComponent().get(0).getId());
            for (int i = 1; i < cPhen.getComponent().size(); i++) {
                sql.append(" OR \"component\" = ").appendValue(cPhen.getComponent().get(i).getId());
            }
            sql.append("))");
            // add the components of the composite to the result
            sql.append(" OR  \"id\" IN ( SELECT \"component\" FROM \"" + schemaPrefix + "om\".\"components\" WHERE \"phenomenon\" = ").appendValue(phen.getId()).append(")");

        } else {
            sql.append(" \"component\" = ").appendValue(phen.getId()).append(")");
        }

        Set<String> results = new HashSet<>();
        try (final SQLResult result = sql.execute(c)) {
            while (result.next()) {
                results.add(result.getString("id"));
            }
        }
        return results;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeProcedure(final String procedureID) throws DataStoreException {
        try (final Connection c = source.getConnection()) {
            final ProcedureInfo pi = getPIDFromProcedure(procedureID, c).orElse(null);
            removeProcedure(pi, c);
        } catch (SQLException ex) {
            throw new DataStoreException("Error while removing procedure.", ex);
        }
    }


    private void removeProcedure(final ProcedureInfo pi, final Connection c) throws SQLException, DataStoreException {
        if (pi == null) return;
        
        deleteProperties("procedures_properties", "id_procedure", pi.procedureId, c);
        removeObservationForProcedure(pi.procedureId, c);

        // remove measure tables
        try (final Statement stmtDrop = c.createStatement()) {

            for (int i = 0; i < pi.nbTable; i++) {
                String suffix = Integer.toString(pi.pid);
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

            stmtObsP.setString(1, pi.procedureId);
            stmtObsP.executeUpdate();

            stmtFoi.setString(1, pi.procedureId);
            stmtFoi.executeUpdate();

            stmtHl.setString(1, pi.procedureId);
            stmtHl.executeUpdate();

            stmtMes.setString(1, pi.procedureId);
            stmtMes.executeUpdate();

            stmtProcDesc.setString(1, pi.procedureId);
            stmtProcDesc.executeUpdate();

            stmtObs.setString(1, pi.procedureId);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeObservation(final String observationID) throws DataStoreException {
        try (final Connection c = source.getConnection()) {
             final ProcedureInfo pi = getPIDFromObservation(observationID, c).orElse(null);
            // observation does not exist
            if (pi == null) return;

            removeObservation(observationID, pi, c);
            removeProcedureIfEmpty(pi, c);
        } catch (SQLException ex) {
            throw new DataStoreException("Error while inserting observation.", ex);
        }
    }

    /**
     * Remove an observation with the specified identifier.
     * this will remove all its measures.
     *
     * @param observationID Observation identifier.
     * @param c A SQL connection.
     */
    private synchronized void removeObservation(final String observationID, final ProcedureInfo pi, Connection c) throws SQLException, DataStoreException {
        
        // remove from measure tables
        for (int i = 0; i < pi.nbTable; i++) {
            String suffix = pi.pid + "";
            if (i > 0) {
                suffix = suffix + "_" + (i + 1);
            }
            try (final PreparedStatement stmtMes = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "mesures\".\"mesure" + suffix + "\" WHERE \"id_observation\" IN (SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?)")){//NOSONAR
                stmtMes.setString(1, observationID);
                stmtMes.executeUpdate();
            }
        }

        // remove from observation table
        try(final PreparedStatement stmtObs = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"identifier\"=?")) {//NOSONAR
            stmtObs.setString(1, observationID);
            stmtObs.executeUpdate();
        }
    }

    /**
     * Remove an observation with the specified identifier.
     * this will remove all its measures.
     *
     * @param oid Observation identifier.
     * @param c A SQL connection.
     */
    private synchronized void removeObservation(final int oid, final ProcedureInfo pi, Connection c) throws SQLException, DataStoreException {

        // remove from measure tables
        for (int i = 0; i < pi.nbTable; i++) {
            String suffix = pi.pid + "";
            if (i > 0) {
                suffix = suffix + "_" + (i + 1);
            }
            try (final PreparedStatement stmtMes = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "mesures\".\"mesure" + suffix + "\" WHERE \"id_observation\" = ?")){//NOSONAR
                stmtMes.setInt(1, oid);
                stmtMes.executeUpdate();
            }
        }

        // remove from observation table
        try(final PreparedStatement stmtObs = c.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"observations\" WHERE \"id\"=?")) {//NOSONAR
            stmtObs.setInt(1, oid);
            stmtObs.executeUpdate();
        }
    }

    /**
     * Remove a procedure if it has no more observation linked to it.
     *
     * @param procedureId procedure identifier.
     * @param c A SQL connection.
     *
     * @return {@code true} id the procedure has been removed
     */
    private boolean removeProcedureIfEmpty(ProcedureInfo pi, Connection c) throws SQLException, DataStoreException {
        boolean removeProc = (getNbMeasureForProcedure(pi.pid, c) == 0);
        if (removeProc) {
            removeProcedure(pi, c);
        }
        return removeProc;
    }

    /**
     * Remove an observation if it has no more measure.
     *
     * @param obsInfo Informations about the observation that may need removal.
     * @param c A SQL connection.
     *
     * @return {@code true} if the observation has been removed.
     */
    private boolean removeObservationIfEmpty(ObservationInfos obsInfo, Connection c) throws SQLException, DataStoreException {
        boolean removeObs = getNbMeasureForObservation(obsInfo.pi.pid, obsInfo.id, c) == 0;
        if (removeObs) {
            removeObservation(obsInfo.id, obsInfo.pi, c);
        }
        return removeObs;
    }

    /**
     * Update the temporal bound of a timeseries observation by looking for the min/max value of the main (Time) field.
     *
     * @param obsInfo Informations about the observation that need to be updated.
     * @param mainField Main field for the observation.
     * @param c A SQL connection.
     */
    private void updateObservationTemporalBounds(ObservationInfos obsInfo, Field mainField, Connection c) throws SQLException, DataStoreException {
        // only for timeseries
        if (mainField.type.equals(FieldType.TIME)) {
            String boundSQL  = "SELECT min(\"" + mainField.name + "\"), max(\"" + mainField.name + "\") FROM \"" + schemaPrefix + "mesures\".\"mesure" + obsInfo.pi.pid + "\" WHERE \"id_observation\" = ?" ;
            String updateObs = "UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"time_begin\"=?, \"time_end\"=? WHERE \"id\"=?";
            try (PreparedStatement bStmt  = c.prepareStatement(boundSQL);
                 PreparedStatement upStmt = c.prepareStatement(updateObs)) {
                bStmt.setInt(1, obsInfo.id);
                try (ResultSet rs = bStmt.executeQuery()) {
                    if (rs.next()) {
                        Timestamp begin = rs.getTimestamp(1);
                        Timestamp end   = rs.getTimestamp(2);
                        upStmt.setTimestamp(1, begin);
                        if (begin.equals(end)) {
                            upStmt.setNull(2, java.sql.Types.TIMESTAMP);
                        } else {
                            upStmt.setTimestamp(2, end);
                        }
                        upStmt.setInt(3, obsInfo.id);
                        upStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Remove the empty measures in an observation.
     *
     * @param obsInfo Informations about the observation that need to be updated.
     * @param c A SQL connection.
     *
     * @return the number of measure removed.
     */
    private int removeEmptyMeasures(final ObservationInfos obsInfo, Connection c) throws SQLException {
        final ProcedureInfo pi = obsInfo.pi;
        List<Field> fields = readFields(pi.procedureId, true, c);

        List<String> rmSQLs = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT m.\"id\" FROM \"" + schemaPrefix + "mesures\".\"mesure" + pi.pid + "\" m");
        StringBuilder where = new StringBuilder(" WHERE ");
        rmSQLs.add("DELETE FROM \"" + schemaPrefix + "mesures\".\"mesure" + pi.pid + "\" WHERE \"id\" = ? AND \"id_observation\" = ?");
        for (int i = 1; i < pi.nbTable; i++) {
            String tableName = "mesure" + pi.pid + "_" + (i + 1);
            sb.append(", \"" + schemaPrefix + "mesures\".\"" + tableName + "\" m" + i + " ");
            where.append(" m" + i + ".\"id\" = m.\"id\" AND ");
            rmSQLs.add("DELETE FROM \"" + schemaPrefix + "mesures\".\"" + tableName + "\" WHERE \"id\" = ? AND \"id_observation\" = ?");
        }
        sb.append(where);
        sb.append(" m.\"id_observation\" = ? ");
        for (Field f : fields) {
            sb.append(" AND \"").append(f.name).append("\" IS NULL");
        }
        List<PreparedStatement> rmStmts = new ArrayList<>();
        for (String sql : rmSQLs) {
            rmStmts.add(c.prepareStatement(sql));
        }
        int rmCount = 0;
        try (PreparedStatement stmt = c.prepareStatement(sb.toString())) {
            stmt.setInt(1, obsInfo.id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    for (int i = 0; i < rmStmts.size(); i++) {
                        PreparedStatement rmStmt = rmStmts.get(i);
                        rmStmt.setInt(1, rs.getInt(1));
                        rmStmt.setInt(2, obsInfo.id);
                        int removed = rmStmt.executeUpdate();
                        // if one table match all the other table should match also
                        if (i == 0) {
                            rmCount += removed;
                        }
                    }
                }
            }
            LOGGER.fine(rmCount + " empty measures deleted");
        } finally {
            for (PreparedStatement stmt : rmStmts) {
                try {stmt.close();} catch (SQLException ex) {LOGGER.log(Level.FINER, "Error while closing reove measure statement", ex);}
            }
        }
        return rmCount;
    }

    private List<Field> getEmptyFieldsForObservation(final ObservationInfos obsInfo, Connection c) throws SQLException {
        final ProcedureInfo pi = obsInfo.pi;
        List<Field> fields = readFields(pi.procedureId, true, c);

        StringBuilder sql = new StringBuilder("SELECT ");

        for (Field field : fields) {
            DbField db = (DbField) field;
            sql.append(" COUNT(m" + db.tableNumber + ".\"" + db.name + "\") as \"" + db.name + "\",");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(" FROM \"" + schemaPrefix + "mesures\".\"mesure" + pi.pid + "\" m1");

        StringBuilder where = new StringBuilder(" WHERE ");
        for (int i = 1; i < pi.nbTable; i++) {
            int tableNum = i + 1;
            String tableName = "mesure" + pi.pid + "_" + tableNum;
            sql.append(", \"" + schemaPrefix + "mesures\".\"" + tableName + "\" m" + tableNum + " ");
            where.append(" m" + tableNum + ".\"id\" = m1.\"id\" AND ");
           
        }
        sql.append(where);
        sql.append(" m1.\"id_observation\" = " + obsInfo.id);

        List<Field> results = new ArrayList<>();
        try (PreparedStatement stmt = c.prepareStatement(sql.toString());
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                for (int i = 0; i < fields.size(); i++) {
                    Field field = fields.get(i);
                    if (rs.getInt(i + 1) == 0) {
                        results.add(field);
                    }
                }
            }
        }
        return results;
    }

    /**
     * Update the observation identifier by removing the specified fields from the current phenomenon fields.
     * Try to find an existing composite phenomenon if it exist. If not a new composite will be created.
     * 
     * @param obsInfo Informations about the observation that need to be updated.
     * @param fieldsToRemove Phenomenon fields that need to be removed fom the current phenomenon.
     * @param c An SQL connection.
     *
     */
    private synchronized void updateObservationPhenomenon(final ObservationInfos obsInfo, List<? extends Field> fieldsToRemove, Connection c) throws SQLException, DataStoreException {

        /*
        * Update the new phenomenon of the observation
        */
        List<Field> remainingFields = new ArrayList<>();
        List<Field> initialFields = getPhenomenonsFields(obsInfo.phenomenon);
        ini:for (Field initialField : initialFields) {
            for (Field rmField : fieldsToRemove) {
                if (Objects.equals(initialField.name, rmField.name)) {
                    continue ini;
                }
            }
            remainingFields.add(initialField);
        }

        Phenomenon phen;
        if (remainingFields.size() == 1) {
            // should never return null
            phen = getPhenomenon(remainingFields.get(0).name, c);
        } else {
            phen = getPhenomenonForFields(remainingFields, c);
            // no existing phenomenon for remaning fields
            // we muste creat a new one
            if (phen == null) {
                phen = createCompositePhenomenonFromField(c, remainingFields);
                writePhenomenon(phen, c, false);
            }
        }

        if (phen != null) {
            try (final PreparedStatement stmtMes = c.prepareStatement("UPDATE \"" + schemaPrefix + "om\".\"observations\" SET \"observed_property\" = ? WHERE \"id\" = ?")){//NOSONAR
                stmtMes.setString(1, phen.getId());
                stmtMes.setInt(2, obsInfo.id);
                stmtMes.executeUpdate();
            }
        } else {
            // should never happen
            throw new DataStoreException("Unable to update observation phenomenon after field removal");
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

    private abstract static class TableStatement {
        public final String baseTableName;
        public final String tableName;

        public TableStatement(int tableNum, String baseTableName) {
            this.baseTableName = baseTableName;
            String suffix = "";
            if (tableNum > 1) {
                suffix = "_" + tableNum;
            }
            this.tableName = baseTableName + suffix;
        }

        abstract void appendField(Field field, boolean isMain, String parentName) throws SQLException, DataStoreException;
    }

    private class TableUpdate extends TableStatement {
        public final List<String> sqls = new ArrayList<>();

        public TableUpdate(int tableNum, String baseTableName) {
            super(tableNum, baseTableName);
        }

        @Override
        public void appendField(Field field, boolean isMain, String parentName) throws SQLException, DataStoreException {
            if (Util.containsForbiddenCharacter(field.name)) {
                throw new DataStoreException("Invalid field name");
            }
            String columnName;
            if (parentName != null) {
                columnName = parentName + "_quality_" + field.name;
            } else {
                columnName = field.name;
            }
            StringBuilder sb = new StringBuilder("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD \"" + columnName + "\" ");
            sb.append(field.getSQLType(isPostgres, false));
            sqls.add(sb.toString());
        }
    }

    private class TableCreation extends TableStatement {
        public final StringBuilder sql;
        public Field mainField;
        public final boolean fillObservations;

        public TableCreation(int tableNum, String baseTableName, boolean fillObs) {
            super(tableNum, baseTableName);
            this.sql = new StringBuilder("CREATE TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\"("
                                                 + "\"id_observation\" integer NOT NULL,"
                                                 + "\"id\"             integer NOT NULL,");
            this.fillObservations = fillObs;
        }

        @Override
        public void appendField(Field field, boolean isMain, String parentName) throws SQLException, DataStoreException {
            if (Util.containsForbiddenCharacter(field.name)) {
                throw new DataStoreException("Invalid field name");
            }
            String columnName;
            if (parentName != null) {
                columnName = parentName + "_quality_" + field.name;
            } else {
                columnName = field.name;
            }

            sql.append('"').append(columnName).append("\" ").append(field.getSQLType(isPostgres, isMain && timescaleDB));
            // main field should not be null (timescaledb compatibility)
            if (isMain) {
                mainField = field;
                sql.append(" NOT NULL");
            }
            sql.append(",");
        }
    }

    private void buildMeasureTable(final ProcedureInfo pi, final List<Field> fields, final Connection c) throws SQLException, DataStoreException {
        if (fields == null || fields.isEmpty()) {
            throw new DataStoreException("measure fields can not be empty");
        }
        final String baseTableName = "mesure" + pi.pid;

        Map<Integer, TableStatement> tablesStmt = new LinkedHashMap<>();
        final List<Field> oldfields;
        final List<DbField> newFields = new ArrayList<>();
        final int fieldOffset;
        int nbTable;
        int nbTabField;
        boolean firstField;
        
        /**
         * Look for table existence.
         */
        final boolean exist = measureTableExist(pi.pid);
        if (!exist) {
            oldfields   = new ArrayList<>();
            firstField  = true;
            nbTable     = 0;
            nbTabField  = 0;
            fieldOffset = 1;
            
        } else if (allowSensorStructureUpdate) {
            oldfields               = readFields(pi.procedureId, false, c);
            firstField              = false;
            nbTable                 = pi.nbTable;
            // number of field in the last measure table
            nbTabField              = getNbFieldInTable(pi.procedureId, nbTable, c);
            fieldOffset             = oldfields.size() + 1;
            
        } else {
            throw new DataStoreException("Observation writer does not support sensor structure update");
        }

        /**
         * Prepare measure table creation / update SQL script.
         */
        for (Field field : fields) {
            if (!containsField(oldfields, field)) {
                // create new table
                if (nbTabField == 0 || nbTabField > maxFieldByTable) {
                    nbTable++;
                    tablesStmt.put(nbTable, new TableCreation(nbTable, baseTableName, !firstField));
                    nbTabField = 0;

                // update existing table
                } else if (!tablesStmt.containsKey(nbTable)) {
                    tablesStmt.put(nbTable, new TableUpdate(nbTable, baseTableName));
                }
                TableStatement tc = tablesStmt.get(nbTable);

                tc.appendField(field, firstField, null);
                firstField = false;

                if (field.qualityFields != null && !field.qualityFields.isEmpty()) {
                    for (Field qField : field.qualityFields) {
                        tc.appendField(qField, false, field.name);
                        nbTabField++;
                    }
                }
                newFields.add(new DbField(field, nbTable));
                nbTabField++;
            }
        }

        /**
         * build/update measures tables.
         */
        try (final Statement stmt = c.createStatement()) {

            for (TableStatement ts : tablesStmt.values()) {
                
                if (ts instanceof TableCreation tc) {
                    String tableName = tc.tableName;
                    StringBuilder tableStsmt = tc.sql;
                    // close statement
                    tableStsmt.setCharAt(tableStsmt.length() - 1, ' ');
                    tableStsmt.append(")");

                    stmt.executeUpdate(tableStsmt.toString());

                    String mainFieldPk = "";
                    if (tc.mainField != null) {
                        mainFieldPk = ", \"" + tc.mainField.name + "\"";
                    }
                    // main field should not be in the primary phenId (timescaledb compatibility)
                    stmt.executeUpdate("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD CONSTRAINT " + tableName + "_pk PRIMARY KEY (\"id_observation\", \"id\"" + mainFieldPk + ")");//NOSONAR
                    stmt.executeUpdate("ALTER TABLE \"" + schemaPrefix + "mesures\".\"" + tableName + "\" ADD CONSTRAINT " + tableName + "_obs_fk FOREIGN KEY (\"id_observation\") REFERENCES \"" + schemaPrefix + "om\".\"observations\"(\"id\")");//NOSONAR

                    //only for timeseries for now
                    if (timescaleDB && tc.mainField != null && FieldType.TIME.equals(tc.mainField.type)) {
                        stmt.execute("SELECT create_hypertable('" + schemaPrefix + "mesures." + baseTableName + "', '" + tc.mainField.name + "')");//NOSONAR
                    }

                    // for extra table, we prefill the observation measures
                    if (tc.fillObservations) {
                        ResultSet rs = stmt.executeQuery("SELECT \"id_observation\", \"id\" FROM \"" + schemaPrefix + "mesures\".\"" + tc.baseTableName + "\"");

                        try (final PreparedStatement stmtBatch = c.prepareStatement("INSERT INTO \"" + schemaPrefix + "mesures\".\"" + tc.tableName + "\" (\"id_observation\", \"id\") VALUES(?,?)")) {
                            while (rs.next()) {
                                stmtBatch.setInt(1, rs.getInt(1));
                                stmtBatch.setInt(2, rs.getInt(2));
                                stmtBatch.addBatch();
                            }
                            stmtBatch.executeBatch();
                        }
                    }

                } else if (ts instanceof TableUpdate tu) {
                    for (String sql : tu.sqls) {
                        stmt.executeUpdate(sql);
                    }
                }
            }
            // update table count
            stmt.executeUpdate("UPDATE \"" + schemaPrefix + "om\".\"procedures\" SET \"nb_table\" = " + nbTable + " WHERE \"id\"='" + pi.procedureId + "'");//NOSONAR
        }

        /**
         * fill procedure_descriptions table
         */
        insertFields(pi.procedureId, newFields, fieldOffset, c);
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
