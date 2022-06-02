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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.apache.sis.storage.event.StoreListeners;
import org.constellation.util.Util;
import org.geotoolkit.storage.event.FeatureStoreContentEvent;
import org.geotoolkit.data.om.OMFeatureTypes;
import static org.geotoolkit.data.om.OMFeatureTypes.*;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.storage.feature.GenericNameIndex;
import org.geotoolkit.observation.AbstractObservationStore;
import org.geotoolkit.observation.ObservationFilterReader;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.ObservationWriter;
import org.geotoolkit.observation.model.ExtractionResult;
import org.geotoolkit.storage.DataStores;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.ResourceId;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseObservationStore extends AbstractObservationStore implements Aggregate {

    private final String SQL_WRITE_SAMPLING_POINT;
    private final String SQL_GET_LAST_ID;

    private ObservationReader reader;
    private ObservationWriter writer;
    private ObservationFilterReader filter;
    private DataSource source;
    protected final String schemaPrefix;

    private final boolean isPostgres;
    protected final GenericNameIndex<FeatureType> types;
    private final List<Resource> components = new ArrayList<>();


    public SOSDatabaseObservationStore(final ParameterValueGroup params) throws DataStoreException {
        super(Parameters.castOrWrap(params));

        try {
            //create a datasource
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(SOSDatabaseParamsUtils.getJDBCUrl(params));


            // driver
            final String driver = SOSDatabaseParamsUtils.getDriverClassName(params);
            config.setDriverClassName(driver);
            isPostgres = driver.startsWith("org.postgresql");
            types = OMFeatureTypes.getFeatureTypes("SamplingPoint");
            Boolean timescaleDB = (Boolean) params.parameter(SOSDatabaseObservationStoreFactory.TIMESCALEDB.getName().toString()).getValue();

            // url
            config.setJdbcUrl(SOSDatabaseParamsUtils.getJDBCUrl(params));

            // username
            final String user = (String) params.parameter(SOSDatabaseObservationStoreFactory.USER.getName().toString()).getValue();
            config.setUsername(user);

            // password
            final String passwd = (String) params.parameter(SOSDatabaseObservationStoreFactory.PASSWD.getName().toString()).getValue();
            if (passwd != null) {
                config.setPassword(passwd);
            }

            source =  new HikariDataSource(config);
            final Map<String,Object> properties = getBasicProperties();

            String sp =  (String) params.parameter(SOSDatabaseObservationStoreFactory.SCHEMA_PREFIX.getName().toString()).getValue();
            if (sp == null) {
                this.schemaPrefix = "";
            } else {
                if (Util.containsForbiddenCharacter(sp)) {
                    throw new DataStoreException("Invalid schema prefix value");
                }
                this.schemaPrefix = sp;
            }

            // build database structure if needed
            buildDatasource();

            reader = new OM2ObservationReader(source, isPostgres, schemaPrefix, properties, timescaleDB);
            writer = new OM2ObservationWriter(source, isPostgres, schemaPrefix, properties, timescaleDB);
            filter = new OM2ObservationFilterReader(source, isPostgres, schemaPrefix, properties, timescaleDB);
        } catch(IOException ex) {
            throw new DataStoreException(ex);
        }

        SQL_WRITE_SAMPLING_POINT = "INSERT INTO \"" + schemaPrefix + "om\".\"sampling_features\" VALUES(?,?,?,?,?,?)";
        SQL_GET_LAST_ID = "SELECT COUNT(*) FROM \"" + schemaPrefix + "om\".\"sampling_features\"";

        for (GenericName name : types.getNames()) {
            components.add(new FeatureView(name));
        }

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
    public Metadata getMetadata() throws DataStoreException {
        return buildMetadata("om2-observation");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Collection<? extends Resource> components() throws DataStoreException {
        return components;
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
    protected Map<Date, AbstractGeometry> getSensorLocations(String sensorID, String version) throws DataStoreException {
        return reader.getSensorLocations(sensorID, "2.0.0");
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
        final ExtractionResult result = new ExtractionResult();
        result.spatialBound.addTime(reader.getEventTime("2.0.0"));
        return result.spatialBound.getTimeObject("2.0.0");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationReader getReader() {
        return reader;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public synchronized ObservationWriter getWriter() {
        return writer;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObservationFilterReader getFilter() {
        return new OM2ObservationFilterReader((OM2ObservationFilter) filter);
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

        private final StoreListeners listeners;
        private final GenericName name;

        FeatureView(GenericName name) {
            super(null, false);
            listeners = new StoreListeners(null, this);
            this.name = name;
        }

        @Override
        public synchronized FeatureType getType() throws DataStoreException {
            FeatureType type = types.get(name.toString());
            if (FeatureExt.getCRS(type) == null) {
                //read a first feature to find the crs
                try (final SOSDatabaseFeatureReader r = new SOSDatabaseFeatureReader(getConnection(), isPostgres, type, schemaPrefix)){
                    if (r.hasNext()) {
                        type = r.next().getType();
                        types.remove(type.getName());
                        types.add(type.getName(), type);
                    }
                } catch (SQLException ex) {
                    throw new DataStoreException("Error while building feature type from first record", ex);
                }
            }

            return types.get(name.toString());
        }

        @Override
        public DataStore getOriginator() {
            return SOSDatabaseObservationStore.this;
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
                    Collection<String> names = ((Collection)feature.getPropertyValue(ATT_NAME.toString()));
                    Collection<String> sampleds = ((Collection)feature.getPropertyValue(ATT_SAMPLED.toString()));
                    stmtWrite.setString(2, (String) names.iterator().next());
                    stmtWrite.setString(3, (String)feature.getPropertyValue(ATT_DESC.toString()));
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
            listeners.fire(new FeatureStoreContentEvent(this, FeatureStoreContentEvent.Type.ADD, getType().getName(), result), FeatureStoreContentEvent.class);
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
