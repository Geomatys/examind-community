/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package com.examind.image.heatmap;

import java.awt.geom.Point2D;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.GenericName;

import static com.examind.image.heatmap.ProfileHeatMap.CRS_84;

class DuckDbPointCloud implements PointCloudResource {

    private final String longitudeColumn;
    private final String latitudeColumn;
    private final String query;
    private final String dbUrl;

    public DuckDbPointCloud(final String rootParquetPath, final String longitudeColumn, final String latitudeColumn) {
        ArgumentChecks.ensureNonNull("longitude column", longitudeColumn);
        ArgumentChecks.ensureNonNull("latitude column", latitudeColumn);
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        this.query = "SELECT " + longitudeColumn + "," + latitudeColumn + " from read_parquet('" + rootParquetPath + "/*.parquet')";
        this.dbUrl = "jdbc:duckdb:";
    }
    
    public DuckDbPointCloud(final String dbPath, final String table, Integer limit, final String longitudeColumn, final String latitudeColumn) {
        ArgumentChecks.ensureNonNull("longitude columne", longitudeColumn);
        ArgumentChecks.ensureNonNull("latitude columne", latitudeColumn);
        this.longitudeColumn = longitudeColumn;
        this.latitudeColumn = latitudeColumn;
        this.query = "SELECT " + longitudeColumn + "," + latitudeColumn + " from " + table;
        this.dbUrl = "jdbc:duckdb:" + dbPath;
    }

    private static Runnable uncheck(AutoCloseable resource) {
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

    private static <T> T uncheck(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new BackingStoreException(e);
        }
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.empty();
    }

    @Override
    public Metadata getMetadata() {
        final MetadataBuilder builder = new MetadataBuilder();
        return builder.buildAndFreeze();
    }

    @Override
    public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {
    }

    @Override
    public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {
    }

    @Override
    public Optional<Envelope> getEnvelope() {
        return Optional.of(new Envelope2D(CRS_84, -180, -90, 360, 180));
    }


    @Override
    public Stream<? extends Point2D> points(Envelope envelope, boolean parallel) {
        final Envelope env = envelope == null ? null : uncheck(() -> Envelopes.transform(envelope, CRS_84));
        //  todo try using Connection c = ((DuckDBConnection) conn).duplicate() ?
        return Stream.of(env)
                .flatMap(bbox -> {

                    Connection c = null;
                    PreparedStatement s = null;
                    ResultSet r = null;
                    try {

                        c = DriverManager.getConnection(dbUrl);
                        if (env != null) {
                            s = c.prepareStatement(query + " WHERE " + longitudeColumn + " between ? and ? and " + latitudeColumn + " between ? and ?");
                            s.setDouble(1, env.getMinimum(0));
                            s.setDouble(2, env.getMaximum(0));
                            s.setDouble(3, env.getMinimum(1));
                            s.setDouble(4, env.getMaximum(1));

//                            // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
//                            s.setFetchSize(1_000_000);
                            r = s.executeQuery();
                        } else {
                            Statement stmt = c.createStatement();
//                            // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
//                            stmt.setFetchSize(1_000_000);
                            r = stmt.executeQuery(query);
                        }


                        final ResultSet rs = r;
                        final Connection finalC = c;
                        final PreparedStatement finalS = s;
                        Spliterator<DirectPosition2D> ptsSplit = new Spliterator<>() {

                            private boolean close() {
                                try (AutoCloseable cc = finalC != null? finalC::close : null;
                                     AutoCloseable sc = finalS != null? finalS::close : null;
                                     AutoCloseable rsc = rs != null? rs::close : null) {
                                    // Nothing, we just want to safely close all resources
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return false;
                            }

                            @Override
                            public boolean tryAdvance(Consumer<? super DirectPosition2D> action) {
                                if (uncheck(rs::isClosed)) return false;

                                while (uncheck(rs::next)) {
                                    try {
                                        var lon = rs.getDouble(1);
                                        if (rs.wasNull()) continue;
                                        var lat = rs.getDouble(2);
                                        if (rs.wasNull()) continue;
                                        action.accept(new DirectPosition2D(CRS_84, lon, lat));
                                        return true;
                                    } catch (SQLException e) {
                                        try {
                                            close();
                                        } catch (RuntimeException bis) {
                                            e.addSuppressed(bis);
                                        }
                                        throw new BackingStoreException(e);
                                    }
                                }
                                return close();
                            }

                            @Override
                            public Spliterator<DirectPosition2D> trySplit() {
                                return null;
                            }

                            @Override
                            public long estimateSize() {
                                return Long.MAX_VALUE;
                            }

                            @Override
                            public int characteristics() {
                                return Spliterator.NONNULL;
                            }
                        };

                        return StreamSupport.stream(ptsSplit, false)
                                .onClose(uncheck(r))
                                .onClose(uncheck(s))
                                .onClose(uncheck(c))
                                ;

                    } catch (Exception e) {
                        if (r != null) {
                            try {
                                r.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                        }
                        if (s != null) {
                            try {
                                s.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                        }
                        if (c != null) {
                            try {
                                c.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                        }
                        throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                    }
                });
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return CRS_84;
    }

    @Override
    public Stream<double[]> batch(Envelope envelope, boolean parallel, int batchSize) throws DataStoreException {

        final Envelope env = envelope == null ? null : uncheck(() -> Envelopes.transform(envelope, CRS_84));
        return Stream.of(env)
                .flatMap(bbox -> {
                    Connection c = null;
                    PreparedStatement s = null;
                    ResultSet r = null;
                    try {

                        c = DriverManager.getConnection(dbUrl);
                        if (env != null) {
                            s = c.prepareStatement(this.query + " WHERE longitude between ? and ? and latitude between ? and ?");
                            s.setDouble(1, env.getMinimum(0));
                            s.setDouble(2, env.getMaximum(0));
                            s.setDouble(3, env.getMinimum(1));
                            s.setDouble(4, env.getMaximum(1));

                            // System.out.println("QUERY: "+s);
                            // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
                            s.setFetchSize(1_000_000);
                            r = s.executeQuery();
                        } else {
                            Statement stmt = c.createStatement();
                            // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
                            stmt.setFetchSize(1_000_000);
                            r = stmt.executeQuery(query);

                        }

                        final ResultSet rs = r;
                        final Connection finalC = c;
                        final PreparedStatement finalS = s;
                        Spliterator<double[]> ptsSplit = new Spliterator<>() {

                            private boolean close() {
                                try (AutoCloseable cc = finalC != null ? finalC::close : null;
                                     AutoCloseable sc = finalS != null ? finalS::close : null;
                                     AutoCloseable rsc = rs != null ? rs::close : null) {
                                    // Nothing, we just want to safely close all resources
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return false;
                            }

                            @Override
                            public boolean tryAdvance(Consumer<? super double[]> action) {
                                if (uncheck(rs::isClosed)) return false;

                                while (uncheck(rs::next)) {
                                    try {
                                        var lon = rs.getDouble(1);
                                        if (rs.wasNull()) continue;
                                        var lat = rs.getDouble(2);
                                        if (rs.wasNull()) continue;
                                        action.accept(new double[]{lon, lat});
                                        return true;
                                    } catch (SQLException e) {
                                        try {
                                            close();
                                        } catch (RuntimeException bis) {
                                            e.addSuppressed(bis);
                                        }
                                        throw new BackingStoreException(e);
                                    }
                                }
                                return close();
                            }

                            @Override
                            public Spliterator<double[]> trySplit() {
                                return null;
                            }

                            @Override
                            public long estimateSize() {
                                return Long.MAX_VALUE;
                            }

                            @Override
                            public int characteristics() {
                                return Spliterator.NONNULL;
                            }
                        };

                        final Stream<double[]> pointStream = StreamSupport.stream(ptsSplit, false)
                                .onClose(uncheck(r))
                                .onClose(uncheck(s))
                                .onClose(uncheck(c));
                        var iterator = pointStream.iterator();
                        final Spliterator<double[]> chunkSpliterator = new Spliterator<>() {

                            @Override
                            public boolean tryAdvance(Consumer<? super double[]> sink) {
                                if (!iterator.hasNext()) return false;

                                double[] chunk = new double[batchSize * 2];
                                for (int i = 0, j = 0; i < batchSize; i++) {
                                    if (!iterator.hasNext()) {
                                        chunk = Arrays.copyOfRange(chunk, 0, j);
                                        break;
                                    }
                                    var pos = iterator.next();
                                    chunk[j++] = pos[0];
                                    chunk[j++] = pos[1];
                                }
                                sink.accept(chunk);
                                return true;
                            }

                            @Override
                            public Spliterator<double[]> trySplit() {
                                return null;
                            }

                            @Override
                            public long estimateSize() {
                                return Long.MAX_VALUE;
                            }

                            @Override
                            public int characteristics() {
                                return NONNULL;
                            }

                        };

                        return StreamSupport.stream(chunkSpliterator, false)
                                .onClose(() -> pointStream.close());

                    } catch (Exception e) {
                        if (r != null) {
                            try {
                                r.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                        }
                        if (s != null) {
                            try {
                                s.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                        }
                        if (c != null) {
                            try {
                                c.close();
                            } catch (SQLException ex) {
                                e.addSuppressed(ex);
                            }
                        }
                        throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                    }
                });

    }
}
