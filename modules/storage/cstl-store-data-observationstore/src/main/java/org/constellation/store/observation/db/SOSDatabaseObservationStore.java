/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.store.observation.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.apache.sis.storage.AbstractFeatureSet;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V100_XML;
import static org.constellation.api.CommonConstants.RESPONSE_FORMAT_V200_XML;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.storage.event.FeatureStoreContentEvent;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.observation.AbstractObservationStore;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationStore;
import org.geotoolkit.observation.ObservationStoreCapabilities;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.feature.OMFeatureTypes;
import static org.geotoolkit.observation.feature.OMFeatureTypes.*;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.Phenomenon;
import org.geotoolkit.observation.model.Procedure;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.model.ResponseMode;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.geotoolkit.storage.DataStores;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.ResourceId;
import org.opengis.observation.Process;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseObservationStore extends AbstractObservationStore implements Aggregate {

    private final String SQL_WRITE_SAMPLING_POINT;
    private final String SQL_GET_LAST_ID;

    static final Map<String, List<String>> RESPONSE_FORMAT = new HashMap<>();
    static {
        RESPONSE_FORMAT.put("1.0.0", Arrays.asList(RESPONSE_FORMAT_V100_XML));
        RESPONSE_FORMAT.put("2.0.0", Arrays.asList(RESPONSE_FORMAT_V200_XML));
    }

    protected ObservationReader reader;
    protected ObservationWriter writer;
    protected ObservationFilterReader filter;
    protected final DataSource source;
    protected final String schemaPrefix;
    protected final boolean timescaleDB;
    protected final int maxFieldByTable;

    protected final boolean isPostgres;
    private final List<Resource> components = new ArrayList<>();


    public SOSDatabaseObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(Parameters.castOrWrap(params));
        try {

            // driver
            final String driver = SOSDatabaseParamsUtils.getDriverClassName(params);

            // url
            final String jdbcUrl = SOSDatabaseParamsUtils.getJDBCUrl(params);

            // username
            final String user = (String) params.parameter(SOSDatabaseObservationStoreFactory.USER.getName().toString()).getValue();

            // password
            final String passwd = (String) params.parameter(SOSDatabaseObservationStoreFactory.PASSWD.getName().toString()).getValue();

            source =  SQLUtilities.getDataSource(driver, jdbcUrl, user, passwd);

            isPostgres = driver.startsWith("org.postgresql");
            timescaleDB = (Boolean) params.parameter(SOSDatabaseObservationStoreFactory.TIMESCALEDB.getName().toString()).getValue();

            String sp =  (String) params.parameter(SOSDatabaseObservationStoreFactory.SCHEMA_PREFIX.getName().toString()).getValue();
            if (sp == null) {
                this.schemaPrefix = "";
            } else {
                if (Util.containsForbiddenCharacter(sp)) {
                    throw new DataStoreException("Invalid schema prefix value");
                }
                this.schemaPrefix = sp;
            }
            this.maxFieldByTable = (int) params.parameter(SOSDatabaseObservationStoreFactory.MAX_FIELD_BY_TABLE.getName().toString()).getValue();

            // build database structure if needed
            buildDatasource();

            // Test if the connection is valid
            try(final Connection c = this.source.getConnection()) {
                // TODO: add a validation test here (query db metadata ?)
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        } catch(IOException ex) {
            throw new DataStoreException(ex);
        }

        SQL_WRITE_SAMPLING_POINT = "INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)";
        SQL_GET_LAST_ID = "SELECT COUNT(*) FROM \"" + schemaPrefix + "om\".\"sampling_features\"";
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(SOSDatabaseObservationStoreFactory.NAME);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected String getStoreIdentifier() {
        return "om2-observation";
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized Collection<? extends Resource> components() throws DataStoreException {
        if (featureSets == null) {
            featureSets = new ArrayList<>();
            featureSets.add(new FeatureView(this, OMFeatureTypes.buildSamplingFeatureFeatureType()));
        }
        return featureSets;
    }

    private Connection getConnection() throws SQLException{
        return source.getConnection();
    }

    public ResourceId getNewFeatureId() {
        try (final Connection cnx = getConnection();
             final PreparedStatement stmtLastId = cnx.prepareStatement(SQL_GET_LAST_ID);
             final ResultSet result = stmtLastId.executeQuery()){
            if (result.next()) {
                final int nb = result.getInt(1) + 1;
                return FilterUtilities.FF.resourceId("sampling-point-" + nb);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // OBSERVATION STORE ///////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc }
     */
    @Override
    protected Map<Date, Geometry> getSensorLocations(String sensorID) throws DataStoreException {
        return getReader().getSensorLocations(sensorID);
    }

    @Override
    public List<ProcedureDataset> getProcedures() throws DataStoreException {
        final List<ProcedureDataset> results = new ArrayList<>();

        final ObservationFilterReader procFilter = getFilter();
        procFilter.init(new ProcedureQuery());

        for (Process p : procFilter.getProcesses()) {
            
            final Procedure proc  =  (Procedure) p;
            final ProcedureDataset procedure = new ProcedureDataset(proc.getId(), proc.getName(), proc.getDescription(), "Component", "timeseries", new ArrayList<>(), null);

            Observation template = (Observation) getReader().getTemplateForProcedure(proc.getId());

            if (template != null) {
                final Phenomenon phenProp = template.getObservedProperty();
                if (phenProp != null) {
                    final List<String> fields = OMUtils.getPhenomenonsFieldIdentifiers(phenProp);
                    for (String field : fields) {
                        if (!procedure.fields.contains(field)) {
                            procedure.fields.add(field);
                        }
                    }
                }
                SamplingFeature foim = template.getFeatureOfInterest();
                procedure.spatialBound.appendLocation(template.getSamplingTime(), foim);
                procedure.spatialBound.getHistoricalLocations().putAll(getSensorLocations(proc.getId()));
            }
            results.add(procedure);
        }
        return results;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() throws DataStoreException {
        if (reader != null) reader.destroy();
        if (writer != null) writer.destroy();
        if (filter != null) filter.destroy();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TemporalGeometricPrimitive getTemporalBounds() throws DataStoreException {
        final ObservationDataset result = new ObservationDataset();
        result.spatialBound.addTime(getReader().getEventTime());
        return result.spatialBound.getTimeObject();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationReader getReader() throws DataStoreException {
        if (reader == null) {
            final Map<String,Object> properties = getBasicProperties();
            reader = new OM2ObservationReader(source, isPostgres, schemaPrefix, properties, timescaleDB);
        }
        return reader;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationWriter getWriter() throws DataStoreException {
        if (writer == null) {
            final Map<String,Object> properties = getBasicProperties();
            writer = new OM2ObservationWriter(source, isPostgres, schemaPrefix, properties, timescaleDB, maxFieldByTable);
        }
        return writer;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationFilterReader getFilter() throws DataStoreException {
        if (filter == null) {
            final Map<String,Object> properties = getBasicProperties();
            filter = new OM2ObservationFilterReader(source, isPostgres, schemaPrefix, properties, timescaleDB);
        }
        return new OM2ObservationFilterReader((OM2ObservationFilter) filter);
    }

    @Override
    public ObservationStoreCapabilities getCapabilities() {
        final List<ResponseMode> responseMode = Arrays.asList(ResponseMode.INLINE, ResponseMode.RESULT_TEMPLATE);
        return new ObservationStoreCapabilities(true, false, false, Arrays.asList("result"), RESPONSE_FORMAT, responseMode, true);
    }

    private boolean buildDatasource() throws DataStoreException {
        try {
            if (OM2DatabaseCreator.validConnection(source)) {
                if (OM2DatabaseCreator.isPostgisInstalled(source, true)) {
                    if (!OM2DatabaseCreator.structurePresent(source, schemaPrefix)) {
                        OM2DatabaseCreator.createObservationDatabase(source, true, null, schemaPrefix);
                        return true;
                    } else {
                        boolean updated = OM2DatabaseCreator.updateStructure(source, schemaPrefix, true);
                        if (updated) {
                            LOGGER.info("OM2 structure already present (updated)");
                        } else {
                            LOGGER.info("OM2 structure already present");
                        }
                    }
                    return true;
                } else {
                    LOGGER.warning("Missing Postgis extension.");
                }
            } else {
                LOGGER.warning("unable to connect OM datasource");
            }
            return false;
        } catch (SQLException | IOException ex) {
            throw new DataStoreException("Erro while building OM2 datasource", ex);
        }
    }

    private final class FeatureView extends AbstractFeatureSet implements StoreResource, WritableFeatureSet {

        private FeatureType type;
        private final ObservationStore store;

        FeatureView(ObservationStore originator, FeatureType type) {
            super(null, false);
            this.type = type;
            this.store = originator;
        }

        @Override
        public synchronized FeatureType getType() throws DataStoreException {
            if (FeatureExt.getCRS(type) == null) {
                //read a first feature to find the crs
                try (final SOSDatabaseFeatureReader r = new SOSDatabaseFeatureReader(getConnection(), isPostgres, type, schemaPrefix)){
                    if (r.hasNext()) {
                        type = r.next().getType();
                    }
                } catch (SQLException ex) {
                    throw new DataStoreException("Error while building feature type from first record", ex);
                }
            }

            return type;
        }

        @Override
        public DataStore getOriginator() {
            return (DataStore) store;
        }

        @Override
        public Stream<Feature> features(boolean parallel) throws DataStoreException {
            final FeatureType sft = getType();
            try {
                final SOSDatabaseFeatureReader reader = new SOSDatabaseFeatureReader(getConnection(), isPostgres, sft, schemaPrefix);
                final Spliterator<Feature> spliterator = Spliterators.spliteratorUnknownSize(reader, Spliterator.ORDERED);
                final Stream<Feature> stream = StreamSupport.stream(spliterator, false);
                return stream.onClose(reader::close);
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        }

        @Override
        public void add(Iterator<? extends Feature> features) throws DataStoreException {
            final Set<ResourceId> result = new LinkedHashSet<>();

            try (Connection cnx = getConnection();
                 PreparedStatement stmtWrite = cnx.prepareStatement(SQL_WRITE_SAMPLING_POINT)) {

                while (features.hasNext()) {
                    final Feature feature = features.next();
                    ResourceId identifier = FeatureExt.getId(feature);
                    if (identifier == null || identifier.getIdentifier().isEmpty()) {
                        identifier = getNewFeatureId();
                    }

                    stmtWrite.setString(1, identifier.getIdentifier());
                    Collection<String> names = ((Collection)feature.getPropertyValue(SF_ATT_NAME.toString()));
                    Collection<String> sampleds = ((Collection)feature.getPropertyValue(SF_ATT_SAMPLED.toString()));
                    stmtWrite.setString(2, (String) names.iterator().next());
                    stmtWrite.setString(3, (String)feature.getPropertyValue(SF_ATT_DESC.toString()));
                    stmtWrite.setString(4, sampleds.isEmpty() ? null : sampleds.iterator().next());
                    final Optional<org.locationtech.jts.geom.Geometry> geometry = FeatureExt.getDefaultGeometryValue(feature)
                            .filter(org.locationtech.jts.geom.Geometry.class::isInstance)
                            .map(org.locationtech.jts.geom.Geometry.class::cast);
                    if (geometry.isPresent()) {
                        final org.locationtech.jts.geom.Geometry geom = geometry.get();
                        WKBWriter writer = new WKBWriter();
                        final int SRID = geom.getSRID();
                        stmtWrite.setBytes(5, writer.write(geom));
                        stmtWrite.setInt(6, SRID);
                    } else {
                        stmtWrite.setNull(5, java.sql.Types.VARBINARY);
                        stmtWrite.setNull(6, java.sql.Types.INTEGER);
                    }
                    stmtWrite.executeUpdate();
                    result.add(identifier);
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, SQL_WRITE_SAMPLING_POINT, ex);
            }
            //todo return created ids
            //return result;
            listeners.fire( FeatureStoreContentEvent.class, new FeatureStoreContentEvent(this, FeatureStoreContentEvent.Type.ADD, getType().getName(), result));
        }

        @Override
        public boolean removeIf(Predicate<? super Feature> filter) throws DataStoreException {
            boolean match = false;
            try (SOSDatabaseFeatureWriter writer = new SOSDatabaseFeatureWriter(getConnection(),isPostgres,getType(), schemaPrefix)) {
                while (writer.hasNext()) {
                    Feature feature = writer.next();
                    if (filter.test(feature)) {
                        writer.remove();
                        match = true;
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(OM2FeatureStore.class.getName()).log(Level.SEVERE, null, ex);
            }
            return match;
        }

        @Override
        public void updateType(FeatureType newType) throws DataStoreException {
            throw new DataStoreException("Not supported.");
        }

        @Override
        public void replaceIf(Predicate<? super Feature> filter, UnaryOperator<Feature> updater) throws DataStoreException {
            throw new DataStoreException("Not supported.");
        }

        @Override
        public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
            listeners.addListener(eventType, listener);
        }

        @Override
        public synchronized <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
            listeners.removeListener(eventType, listener);
        }
    }
}
