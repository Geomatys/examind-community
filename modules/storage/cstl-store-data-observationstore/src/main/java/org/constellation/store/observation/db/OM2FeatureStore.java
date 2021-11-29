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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.feature.builder.AttributeRole;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.AbstractFeatureSet;
import org.apache.sis.internal.storage.StoreResource;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.IllegalNameException;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.WritableFeatureSet;
import org.apache.sis.util.logging.Logging;
import static org.constellation.store.observation.db.OM2FeatureStoreFactory.SCHEMA_PREFIX;
import static org.constellation.store.observation.db.OM2FeatureStoreFactory.SGBDTYPE;
import org.constellation.util.Util;
import org.geotoolkit.storage.feature.FeatureStoreRuntimeException;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.storage.feature.GenericNameIndex;
import org.geotoolkit.jdbc.ManageableDataSource;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.util.collection.CloseableIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureType;
import org.opengis.filter.ResourceId;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

/**
 * Feature store on the Examind OM2 database.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Johann Sorel (Geomatys)
 *
 */
public class OM2FeatureStore extends DataStore implements Aggregate {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.store.observation");

    private static final String CSTL_NAMESPACE = "http://constellation.org/om2";
    private static final GenericName CSTL_TN_SENSOR = NamesExt.create(CSTL_NAMESPACE, "Sensor");
    protected static final GenericName ATT_ID = NamesExt.create(CSTL_NAMESPACE, "id");
    protected static final GenericName ATT_POSITION = NamesExt.create(CSTL_NAMESPACE, "position");

    private final Parameters parameters;
    private final GenericNameIndex<FeatureType> types = new GenericNameIndex<>();

    private final ManageableDataSource source;

    private final String sensorIdBase = "urn:ogc:object:sensor:GEOM:"; // TODO

    private final boolean isPostgres;

    protected final String schemaPrefix;

    private final List<Resource> components = new ArrayList<>();

    public OM2FeatureStore(final ParameterValueGroup params, final ManageableDataSource source) throws DataStoreException {
        this.parameters = Parameters.castOrWrap(params);
        this.source = source;
        Object sgbdtype = parameters.getMandatoryValue(SGBDTYPE);
        isPostgres = !("derby".equals(sgbdtype));
        String sc = parameters.getValue(SCHEMA_PREFIX);
        if (sc != null) {
            if (Util.containsForbiddenCharacter(sc)) {
                throw new DataStoreException("Invalid schema prefix value");
            }
            schemaPrefix = sc;
        } else {
            schemaPrefix = "";
        }
        initTypes();
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(OM2FeatureStoreFactory.NAME);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return new DefaultMetadata();
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.of(parameters);
    }

    @Override
    public Collection<? extends Resource> components() throws DataStoreException {
        return components;
    }

    private Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    private void initTypes() {
        final FeatureTypeBuilder featureTypeBuilder = new FeatureTypeBuilder();
        featureTypeBuilder.setName(CSTL_TN_SENSOR);
        featureTypeBuilder.addAttribute(String.class).setName(ATT_ID).addRole(AttributeRole.IDENTIFIER_COMPONENT);
        featureTypeBuilder.addAttribute(Geometry.class).setName(ATT_POSITION).addRole(AttributeRole.DEFAULT_GEOMETRY);
        try {
            final FeatureType type = featureTypeBuilder.build();
            types.add(CSTL_TN_SENSOR, type);

            components.add(new FeatureView(type.getName()));
        } catch (IllegalNameException ex) {
            //won't happen
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void close() {
        try {
            source.close();
        } catch (SQLException ex) {
            LOGGER.info("SQL Exception while closing O&M2 datastore");
        }
    }

    public ResourceId getNewFeatureId() {
        Connection cnx = null;
        PreparedStatement stmtLastId = null;
        try {
            cnx = getConnection();
            stmtLastId = cnx.prepareStatement("SELECT \"id\" FROM \"" + schemaPrefix + "om\".\"procedures\" ORDER BY \"id\" ASC");//NOSONAR
            try (final ResultSet result = stmtLastId.executeQuery()) {
                // keep the last
                String id = null;
                while (result.next()) {
                    id = result.getString(1);
                }
                if (id != null) {
                    try {
                        final int i = Integer.parseInt(id.substring(sensorIdBase.length()));
                        return FilterUtilities.FF.resourceId(sensorIdBase + i);
                    } catch (NumberFormatException ex) {
                        LOGGER.warning("a snesor ID is malformed in procedures tables");
                    }
                } else {
                    return FilterUtilities.FF.resourceId(sensorIdBase + 1);
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        } finally {
            if (stmtLastId != null) {
                try {
                    stmtLastId.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }

            if (cnx != null) {
                try {
                    cnx.close();
                } catch (SQLException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Feature Reader //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private class OMReader implements CloseableIterator<Feature> {

        protected final Connection cnx;
        private boolean firstCRS = true;
        protected FeatureType type;
        private final ResultSet result;
        protected Feature current = null;

        @SuppressWarnings("squid:S2095")
        private OMReader(final FeatureType type) throws SQLException {
            this.type = type;
            cnx = getConnection();
            final PreparedStatement stmtAll;
            if (isPostgres) {
                stmtAll = cnx.prepareStatement("SELECT  \"id\", \"postgis\".st_asBinary(\"shape\"), \"crs\" FROM \"" + schemaPrefix + "om\".\"procedures\"");//NOSONAR
            } else {
                stmtAll = cnx.prepareStatement("SELECT * FROM \"" + schemaPrefix + "om\".\"procedures\"");//NOSONAR
            }
            result = stmtAll.executeQuery();
        }

        public FeatureType getFeatureType() {
            return type;
        }

        @Override
        public Feature next() throws FeatureStoreRuntimeException {
            try {
                read();
            } catch (Exception ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
            Feature candidate = current;
            current = null;
            return candidate;
        }

        @Override
        public boolean hasNext() throws FeatureStoreRuntimeException {
            try {
                read();
            } catch (Exception ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
            return current != null;
        }

        protected void read() throws Exception {
            if (current != null) {
                return;
            }

            if (!result.next()) {
                return;
            }

            final String crsStr = result.getString(3);
            Geometry geom = null;

            if (crsStr != null && !crsStr.isEmpty()) {
                if (firstCRS) {
                    try {
                        CoordinateReferenceSystem crs = CRS.forCode("EPSG:" + crsStr);
                        final FeatureTypeBuilder ftb = new FeatureTypeBuilder(type);
                        ((AttributeTypeBuilder) ftb.getProperty("position")).setCRS(crs);
                        type = ftb.build();
                        firstCRS = false;
                    } catch (NoSuchAuthorityCodeException ex) {
                        throw new IOException(ex);
                    } catch (FactoryException ex) {
                        throw new IOException(ex);
                    }
                }

                final byte[] b = result.getBytes(2);
                if (b != null) {
                    WKBReader reader = new WKBReader();
                    geom = reader.read(b);
                }
            }

            final String id = result.getString(1);
            current = type.newInstance();
            current.setPropertyValue(AttributeConvention.IDENTIFIER_PROPERTY.toString(), id);
            current.setPropertyValue(ATT_ID.toString(), id);
            current.setPropertyValue(ATT_POSITION.toString(), geom);
            //props.add(FF.createAttribute(result.getString("description"), (AttributeDescriptor) type.getDescriptor(ATT_DESC), null));
        }

        @Override
        public void close() {
            try {
                result.close();
                cnx.close();
            } catch (SQLException ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
        }

        @Override
        public void remove() throws FeatureStoreRuntimeException {
            throw new FeatureStoreRuntimeException("Not supported.");
        }
    }

    private class OMWriter extends OMReader {

        protected Feature candidate = null;

        private OMWriter(final FeatureType type) throws SQLException {
            super(type);
        }

        @Override
        public Feature next() throws FeatureStoreRuntimeException {
            try {
                read();
            } catch (Exception ex) {
                throw new FeatureStoreRuntimeException(ex);
            }
            candidate = current;
            current = null;
            return candidate;
        }

        @Override
        public void remove() throws FeatureStoreRuntimeException {

            if (candidate == null) {
                return;
            }

            try (PreparedStatement stmtDelete = cnx.prepareStatement("DELETE FROM \"" + schemaPrefix + "om\".\"procedures\" WHERE \"id\" = ?")) {//NOSONAR

                stmtDelete.setString(1, FeatureExt.getId(candidate).getIdentifier());
                stmtDelete.executeUpdate();

            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Error while deleting procedure features", ex);
            }
        }

    }

    private final class FeatureView extends AbstractFeatureSet implements StoreResource, WritableFeatureSet {

        private final GenericName name;

        FeatureView(GenericName name) {
            super(null);
            this.name = name;
        }

        @Override
        public FeatureType getType() throws DataStoreException {
            return types.get(name.toString());
        }

        @Override
        public DataStore getOriginator() {
            return OM2FeatureStore.this;
        }

        @Override
        public Stream<Feature> features(boolean parallel) throws DataStoreException {
            final FeatureType sft = getType();
            try {
                final OMReader reader = new OMReader(sft);
                final Spliterator<Feature> spliterator = Spliterators.spliteratorUnknownSize(reader, Spliterator.ORDERED);
                final Stream<Feature> stream = StreamSupport.stream(spliterator, false);
                return stream.onClose(reader::close);
            } catch (SQLException ex) {
                throw new DataStoreException(ex);
            }
        }

        @Override
        public void add(Iterator<? extends Feature> features) throws DataStoreException {
            final List<ResourceId> result = new ArrayList<>();

            Connection cnx = null;
            PreparedStatement stmtWrite = null;
            try {
                cnx = getConnection();
                stmtWrite = cnx.prepareStatement("INSERT INTO \"" + schemaPrefix + "om\".\"procedures\" VALUES(?,?,?)");//NOSONAR

                while (features.hasNext()) {
                    final Feature feature = features.next();
                    ResourceId identifier = FeatureExt.getId(feature);
                    if (identifier == null || identifier.getIdentifier().isEmpty()) {
                        identifier = getNewFeatureId();
                    }

                    stmtWrite.setString(1, identifier.getIdentifier());
                    final Optional<Geometry> geometry = FeatureExt.getDefaultGeometryValue(feature)
                            .filter(Geometry.class::isInstance)
                            .map(Geometry.class::cast);
                    if (geometry.isPresent()) {
                        final Geometry geom = geometry.get();
                        final WKBWriter writer = new WKBWriter();
                        final int SRID = geom.getSRID();
                        stmtWrite.setBytes(2, writer.write(geom));
                        stmtWrite.setInt(3, SRID);

                    } else {
                        stmtWrite.setNull(2, Types.VARCHAR);
                        stmtWrite.setNull(3, Types.INTEGER);

                    }
                    stmtWrite.executeUpdate();
                    result.add(identifier);
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Error while writing procedure feature", ex);
            } finally {
                if (stmtWrite != null) {
                    try {
                        stmtWrite.close();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.WARNING, null, ex);
                    }
                }

                if (cnx != null) {
                    try {
                        cnx.close();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.WARNING, null, ex);
                    }
                }
            }
            //todo find a way to return created feature ids
            //return result;
        }

        @Override
        public boolean removeIf(Predicate<? super Feature> filter) throws DataStoreException {
            boolean match = false;
            try (OMWriter writer = new OMWriter(getType())) {
                while (writer.hasNext()) {
                    Feature feature = writer.next();
                    if (filter.test(feature)) {
                        writer.remove();
                        match = true;
                    }
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
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
    }
}
