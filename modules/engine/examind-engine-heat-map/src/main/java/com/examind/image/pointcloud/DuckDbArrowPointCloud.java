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

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.FieldVector;
import static org.apache.arrow.vector.Float8Vector.TYPE_WIDTH;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowReader;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.collection.BackingStoreException;
import org.opengis.geometry.Envelope;

import org.duckdb.DuckDBResultSet;

public class DuckDbArrowPointCloud extends AbstractSQLPointCloud {
    
    public DuckDbArrowPointCloud(final Path parquetPath, final String longitudeColumn, final String latitudeColumn) {
        super(parquetPath, longitudeColumn, latitudeColumn);
    }
    
    public DuckDbArrowPointCloud(final DataSource datasource, final String table, final String query, final String longitudeColumn, final String latitudeColumn) {
        super(datasource, table, query, longitudeColumn, latitudeColumn);
    }

    @Override
    public Stream<double[]> batchPoints(Envelope envelope, boolean parallel, int batchSize) throws DataStoreException {

        final Envelope env = envelope == null ? null : uncheck(() -> Envelopes.transform(envelope, CRS_84));
        return Stream.of(env)
                .flatMap(bbox -> {
                    Connection c = null;
                    PreparedStatement s = null;
                    DuckDBResultSet rs = null;
                    RootAllocator a = null;
                    ArrowReader re = null;
                    try {

                        c  = datasource.getConnection();
                        s  = preparedStatement(c, env);
                        rs = (DuckDBResultSet) s.executeQuery();
                        a  = new RootAllocator();
                        re = (ArrowReader) rs.arrowExportStream(a, batchSize);

                        final ResultSet finalRs        = rs;
                        final Connection finalC        = c;
                        final PreparedStatement finalS = s;
                        final RootAllocator finalA     = a;
                        final ArrowReader finalRe      = re;
                        
                        Spliterator<double[]> ptsSplit = new Spliterator<>() {

                            private boolean close() {
                                try (AutoCloseable cc  = finalC  != null ? finalC::close  : null;
                                     AutoCloseable sc  = finalS  != null ? finalS::close  : null;
                                     AutoCloseable rsc = finalRs != null ? finalRs::close : null;
                                     AutoCloseable r   = finalRe != null ? finalRe::close : null;
                                     AutoCloseable a   = finalA  != null ? finalA::close  : null;) {
                                    // Nothing, we just want to safely close all resources
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                return false;
                            }

                            @Override
                            public boolean tryAdvance(Consumer<? super double[]> action) {
                                try {
                                    boolean dataRemains = uncheck(finalRe::loadNextBatch);
                                    
                                    double[] chunk = new double[batchSize * 2];
                                    
                                    VectorSchemaRoot vectorSchemaRoot = finalRe.getVectorSchemaRoot();
                                    FieldVector latVector = vectorSchemaRoot.getVector("latitude");
                                    FieldVector lonVector = vectorSchemaRoot.getVector("longitude");
                                    
                                    if (dataRemains) {
                                        int j = 0;
                                        int nbValue = vectorSchemaRoot.getRowCount();
                                        for (int i = 0; i < nbValue; i++) {
                                            chunk[j++] = lonVector.getDataBuffer().getDouble((long) i * TYPE_WIDTH);
                                            chunk[j++] = latVector.getDataBuffer().getDouble((long) i * TYPE_WIDTH);
                                        }
                                        if (nbValue < batchSize) {
                                            chunk = Arrays.copyOfRange(chunk, 0, j);
                                        }
                                        action.accept(chunk);
                                        return true;
                                
                                    }
                                    return false;
                                } catch (IOException e) {
                                    try {
                                        close();
                                    } catch (RuntimeException bis) {
                                        e.addSuppressed(bis);
                                    }
                                    throw new BackingStoreException(e);
                                }
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

                        return StreamSupport.stream(ptsSplit, false)
                                .onClose(uncheckClose(rs))
                                .onClose(uncheckClose(s))
                                .onClose(uncheckClose(c))
                                .onClose(uncheckClose(re))
                                .onClose(uncheckClose(a));

                    } catch (Exception e) {
                        uncheckClose(rs, e);
                        uncheckClose(s, e);
                        uncheckClose(c, e);
                        uncheckClose(re, e);
                        uncheckClose(a, e);
                        throw e instanceof RuntimeException rex ? rex : new RuntimeException(e);
                    }
                });

    }
}
