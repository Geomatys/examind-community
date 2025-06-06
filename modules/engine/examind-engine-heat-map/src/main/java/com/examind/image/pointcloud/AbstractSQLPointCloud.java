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

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.util.NamesExt;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractSQLPointCloud implements PointCloudResource {
    
    protected static final int FETCH_SIZE = 1_000_000;
    
    protected static final GeographicCRS CRS_84 = CommonCRS.defaultGeographic();
    
    protected final String longitudeColumn;
    protected final String latitudeColumn;
    
    protected final String query;
    protected final DataSource datasource;
    protected final String name;
    
    /**
     * Build an SQL query on a parquet file, or a directory containing multiple files.
     * The data will not be stored in a Duckdb database.
     * 
     * @param parquetPath Path to the parquet file or directory.
     * @param longitudeColumn Name of the longitude column.
     * @param latitudeColumn Name of the latitude column.
     */
    public AbstractSQLPointCloud(final Path parquetPath, final String longitudeColumn, final String latitudeColumn) {
        ArgumentChecks.ensureNonNull("longitude column", longitudeColumn);
        ArgumentChecks.ensureNonNull("latitude column", latitudeColumn);
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        
        this.name = parquetPath.getFileName().toString();
        if (Files.isDirectory(parquetPath)) {
            this.query = "SELECT " + longitudeColumn + "," + latitudeColumn + " from read_parquet('" + parquetPath.toString() + "/*.parquet')";
        } else {
            this.query = "SELECT " + longitudeColumn + "," + latitudeColumn + " from read_parquet('" + parquetPath.toString() + "')";
        }
        this.datasource = new DefaultDataSource("jdbc:duckdb:"); // TODO test
    }
    
    /**
     * Build an SQL query on a database table, or on a SQL query.
     * 
     * @param datasource SQL datasource.
     * @param table Table name, can be {@code null} if query is specified.
     * @param query SQL query, can be {@code null} if table is specified.
     * @param longitudeColumn Name of the longitude column.
     * @param latitudeColumn Name of the latitude column.
     */
    public AbstractSQLPointCloud(final DataSource datasource, final String table, final String query, final String longitudeColumn, final String latitudeColumn) {
        ArgumentChecks.ensureNonNull("longitude column", longitudeColumn);
        ArgumentChecks.ensureNonNull("latitude column", latitudeColumn);
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        
        if (query != null && table != null) {
            throw new IllegalArgumentException("Query and Table parameter can not be both not null.");
        } else if (query == null && table == null) {
            throw new IllegalArgumentException("Query and Table parameter can not be both null.");
        }
        
        if (table != null) {
            this.query = "SELECT " + longitudeColumn + "," + latitudeColumn + " from " + table;
            this.name = table;
        } else {
            this.query = "SELECT " + longitudeColumn + "," + latitudeColumn + " from (" + query + ")";
            this.name = "Query";
        }
        this.datasource = datasource;
    }
    
    protected PreparedStatement preparedStatement(Connection c, Envelope env) throws SQLException {
        PreparedStatement result;
        if (env != null) {
            result = c.prepareStatement(query + " WHERE " + longitudeColumn + " between ? and ? and " + latitudeColumn + " between ? and ?");
            result.setDouble(1, env.getMinimum(0));
            result.setDouble(2, env.getMaximum(0));
            result.setDouble(3, env.getMinimum(1));
            result.setDouble(4, env.getMaximum(1));
        } else {
            result = c.prepareStatement(query);
        }
        // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
        result.setFetchSize(FETCH_SIZE);
        return result;

    }
    
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(NamesExt.create(name));
    }
    
    @Override
    public Metadata getMetadata() {
        // TODO fill with some informations ?
        final MetadataBuilder builder = new MetadataBuilder();
        return builder.buildAndFreeze();
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        // TODO dynamic ?
        return Optional.of(new Envelope2D(CRS_84, -180, -90, 360, 180));
    }
    
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return CRS_84;
    }
    
    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
        // do nothing
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
        // do nothing
    }
    
    protected static Runnable uncheckClose(AutoCloseable resource) {
        return () -> {
            try {
                if (resource != null)
                    resource.close();
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new BackingStoreException(e);
            }
        };
    }
    
    protected static Runnable uncheckClose(AutoCloseable resource, Exception e) {
        return () -> {
            try {
                if (resource != null)
                    resource.close();
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
        };
    }

    protected static <T> T uncheck(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }
}
