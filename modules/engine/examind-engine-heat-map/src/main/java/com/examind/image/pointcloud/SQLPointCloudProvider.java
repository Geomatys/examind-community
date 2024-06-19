/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
package com.examind.image.pointcloud;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.CanNotProbeException;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SQLPointCloudProvider extends DataStoreProvider {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.image.pointcloud");

    public static final String NAME = "SQL-POINT-CLOUD";

    public static final ParameterDescriptor<String> LOCATION;
    
    public static final ParameterDescriptor<String> USER;
    public static final ParameterDescriptor<String> PASSWORD;
    public static final ParameterDescriptor<Boolean> USE_ARROW;
    

    public static final ParameterDescriptor<String> LATITUDE_COLUMN;
    public static final ParameterDescriptor<String> LONGITUDE_COLUMN;
    
    public static final ParameterDescriptor<String> QUERY;
    public static final ParameterDescriptor<String> TABLE;
    public static final ParameterDescriptor<Path> PARQUET_FILE;

    public static final ParameterDescriptorGroup INPUT;

    static {
        final ParameterBuilder builder = new ParameterBuilder();
        builder.setRequired(true);

        LOCATION = builder.addName(DataStoreProvider.LOCATION)
                .setDescription("JDBC URL to use to connect to the database. User and password must be provided separately")
                .create(String.class, null);
        
        LATITUDE_COLUMN = builder.addName("latitude-column")
                .setDescription("Latitude column in the table.")
                .create(String.class, null);
        
        LONGITUDE_COLUMN = builder.addName("longitude-column")
                .setDescription("Longitude column in the table.")
                .create(String.class, null);
        
        USE_ARROW = builder.addName("use-arrow")
                .setDescription("If set to true, apache arrow will be used for duckdb extraction.")
                .create(Boolean.class, false);

        builder.setRequired(false);
        
        TABLE = builder.addName("tables")
                .setDescription("A table to consider as a point cloud.")
                .create(String.class, null);
        
        QUERY = builder.addName("query")
                .setDescription("A sql query to consider as a point cloud.")
                .create(String.class, null);
        
        PARQUET_FILE = builder.addName("parquet_file")
                .setDescription("A parquetfile or directory containing parquet files.")
                .create(Path.class, null);

        USER = builder.addName("user")
                .setDescription("User name to use when connecting to the database")
                .create(String.class, null);
        PASSWORD = builder.addName("password")
                .setDescription("Password to use for connection")
                .create(String.class, null);

        INPUT = builder.addName(NAME).createGroup(
                LOCATION, USER, PASSWORD,
                TABLE, QUERY, PARQUET_FILE, LATITUDE_COLUMN, LONGITUDE_COLUMN, USE_ARROW
        );
    }



    public SQLPointCloudProvider() {
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return INPUT;
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        try {
            final DataSource ds = connector.getStorageAs(DataSource.class);
            if (ds != null) {
                try (Connection c = ds.getConnection()) {
                    return ProbeResult.SUPPORTED;
                } catch (SQLException e) {
                    final String state = e.getSQLState();
                    if (!"08001".equals(state) || !"3D000".equals(state)) {
                        throw new CanNotProbeException(this, connector, e);
                    }
                }
            }
            return ProbeResult.UNSUPPORTED_STORAGE;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Content probing failed using SQL point cloud provider", e);
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        // TODO ?
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        final Parameters p = Parameters.castOrWrap(parameters);
        return new SQLStore(p);
    }

    class SQLStore extends DataStore implements Aggregate {
        private final Parameters parameters;

        private final PointCloudResource pointCloud;

        public SQLStore(Parameters parameters) throws DataStoreException {
            this.parameters = parameters;

            String location = parameters.getValue(LOCATION);
            String table = parameters.getValue(TABLE);
            String query = parameters.getValue(QUERY);
            Path parquetFiles = parameters.getValue(PARQUET_FILE);
            String latitudeColumn = parameters.getValue(LATITUDE_COLUMN);
            String longitudeColumn = parameters.getValue(LONGITUDE_COLUMN);
            boolean useArrow = parameters.getValue(USE_ARROW);
            
            if (parquetFiles != null) {
                if (useArrow) {
                    pointCloud = new DuckDbArrowPointCloud(parquetFiles, longitudeColumn, latitudeColumn);
                } else {
                    pointCloud = new DuckDbPointCloud(parquetFiles, longitudeColumn, latitudeColumn);
                }
            } else {
                if (useArrow) {
                    pointCloud = new DuckDbArrowPointCloud(location, table, query, longitudeColumn, latitudeColumn);
                } else {
                    pointCloud = new DuckDbPointCloud(location, table, query, longitudeColumn, latitudeColumn);
                }
            }
        }

        @Override
        public Collection<? extends Resource> components() throws DataStoreException {
            return List.of(pointCloud);
        }

        @Override
        public Optional<ParameterValueGroup> getOpenParameters() {
            return Optional.of(parameters);
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return new DefaultMetadata();
        }

        @Override
        public void close() throws DataStoreException {
        }
    }
}