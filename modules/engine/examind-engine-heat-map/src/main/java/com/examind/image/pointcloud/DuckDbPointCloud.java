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
package com.examind.image.pointcloud;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.geometry.Envelope;
import org.opengis.util.GenericName;

import org.geotoolkit.util.NamesExt;

public class DuckDbPointCloud extends AbstractSQLPointCloud {
    
    private final boolean noNullValue = true; // TODO parameters

    public DuckDbPointCloud(final Path parquetPath, final String longitudeColumn, final String latitudeColumn) {
        super(parquetPath, longitudeColumn, latitudeColumn);
    }
    
    public DuckDbPointCloud(final DataSource datasource, final String table, final String query, final String longitudeColumn, final String latitudeColumn) {
        super(datasource, table, query, longitudeColumn, latitudeColumn);
    }
    
    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.of(NamesExt.create(name));
    }

   @Override
    public Stream<double[]> batchPoints(Envelope envelope, boolean parallel, int batchSize) throws DataStoreException {

        final Envelope env = envelope == null ? null : uncheck(() -> Envelopes.transform(envelope, CRS_84));
        return Stream.of(env)
                .flatMap(bbox -> {
                    Connection c = null;
                    PreparedStatement s = null;
                    ResultSet r = null;
                    try {

                        c = datasource.getConnection();
                        s = preparedStatement(c, env);
                        r = s.executeQuery();

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
                                        if (!noNullValue && rs.wasNull()) continue;
                                        var lat = rs.getDouble(2);
                                        if (!noNullValue && rs.wasNull()) continue;
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
                                .onClose(uncheckClose(r))
                                .onClose(uncheckClose(s))
                                .onClose(uncheckClose(c));
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
                        uncheckClose(r, e);
                        uncheckClose(s, e);
                        uncheckClose(c, e);
                        throw e instanceof RuntimeException re ? re : new RuntimeException(e);
                    }
                });

    }
}
