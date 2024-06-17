package com.examind.image.heatmap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.storage.base.MetadataBuilder;
import org.apache.sis.math.Statistics;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.event.StoreEvent;
import org.apache.sis.storage.event.StoreListener;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.apache.sis.util.collection.BackingStoreException;
import org.geotoolkit.math.XMath;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.util.GenericName;

import javax.sql.DataSource;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ProfileHeatMap {

    public static final GeographicCRS CRS_84 = CommonCRS.defaultGeographic();

    private static final String START_DATE = "2019-03-01T00:00:00Z";
    private static final String END_DATE = "2019-03-02T00:00:00Z";

    private static Runnable uncheck(AutoCloseable resource) {
        return () -> {
            try {
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
    public static PointCloudResource loadAIS(String password, String table, Instant start, Instant end) {

        final HikariConfig dbConf = new HikariConfig();
        dbConf.setJdbcUrl("jdbc:postgresql://192.168.20.13:32345/movingdata_ng");
        dbConf.setUsername("geouser");
        dbConf.setPassword(password);
        // WARNING: AUTO-COMMIT MUST BE DEACTIVATED TO ALLOW RESULT STREAMING !!!
        dbConf.setAutoCommit(false);
        dbConf.setReadOnly(true);
        dbConf.setConnectionTimeout(2000);
        dbConf.setRegisterMbeans(false);
        dbConf.setIdleTimeout(2000);
        dbConf.setLeakDetectionThreshold(20000);
        dbConf.setMaximumPoolSize(5);
        dbConf.setMinimumIdle(2);

        DataSource sqlSource = new HikariDataSource(dbConf);

        return new PointCloudResource() {

            @Override
            public Optional<GenericName> getIdentifier() { return Optional.empty(); }

            @Override
            public Metadata getMetadata() {
                final MetadataBuilder builder = new MetadataBuilder();
                return builder.buildAndFreeze();
            }

            @Override
            public <T extends StoreEvent> void addListener(Class<T> eventType, StoreListener<? super T> listener) {}

            @Override
            public <T extends StoreEvent> void removeListener(Class<T> eventType, StoreListener<? super T> listener) {}

            @Override
            public Optional<Envelope> getEnvelope() {
                return Optional.of(new Envelope2D(CRS_84, -180, -90, 360, 180));
            }

            @Override
            public Stream<? extends Point2D> points(Envelope envelope, boolean parallel) {
                final Envelope env = envelope == null ? null : uncheck(() -> Envelopes.transform(envelope, CRS_84));
                return Stream.of(env)
                        .flatMap(bbox -> {
                            Connection c = null;
                            PreparedStatement s = null;
                            ResultSet r = null;
                            try {
                                c = sqlSource.getConnection();
                                if (env == null) {
                                    s = c.prepareStatement("SELECT longitude, latitude FROM \"" + table + "\" WHERE \"timestamp\" between '"+START_DATE+"' and '"+END_DATE+"'");
                                    s.setTimestamp(1, Timestamp.from(start));
                                    s.setTimestamp(2, Timestamp.from(end));
                                } else {
                                    s = c.prepareStatement("SELECT longitude, latitude FROM \"" + table + "\" WHERE \"timestamp\" between ? and ? AND longitude between ? and ? and latitude between ? and ?");
                                    s.setTimestamp(1, Timestamp.from(start));
                                    s.setTimestamp(2, Timestamp.from(end));
                                    s.setDouble(3, env.getMinimum(0));
                                    s.setDouble(4, env.getMaximum(0));
                                    s.setDouble(5, env.getMinimum(1));
                                    s.setDouble(6, env.getMaximum(1));
                                }

                                // System.out.println("QUERY: "+s);
                                // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
                                s.setFetchSize(1_000_000);
                                r = s.executeQuery();
                                final ResultSet rs = r;
                                final Connection finalC = c;
                                final PreparedStatement finalS = s;
                                Spliterator<DirectPosition2D> ptsSplit = new Spliterator<>() {

                                    private boolean close() {
                                        try (AutoCloseable cc = finalC::close; AutoCloseable sc = finalS::close; AutoCloseable rsc = rs::close) {
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
                                c = sqlSource.getConnection();
                                if (env == null) {
                                    s = c.prepareStatement("SELECT longitude, latitude FROM \"" + table + "\" WHERE \"timestamp\" between '"+START_DATE+"' and '"+END_DATE+"'");
                                    s.setTimestamp(1, Timestamp.from(start));
                                    s.setTimestamp(2, Timestamp.from(end));
                                } else {
                                    s = c.prepareStatement("SELECT longitude, latitude FROM \"" + table + "\" WHERE \"timestamp\" between ? and ? AND longitude between ? and ? and latitude between ? and ?");
                                    s.setTimestamp(1, Timestamp.from(start));
                                    s.setTimestamp(2, Timestamp.from(end));
                                    s.setDouble(3, env.getMinimum(0));
                                    s.setDouble(4, env.getMaximum(0));
                                    s.setDouble(5, env.getMinimum(1));
                                    s.setDouble(6, env.getMaximum(1));
                                }

                                // System.out.println("QUERY: "+s);
                                // WARNING: this is VITAL. Otherwise, all results are buffered in memory, causing high latency and memory footprint...
                                s.setFetchSize(1_000_000);
                                r = s.executeQuery();
                                final ResultSet rs = r;
                                final Connection finalC = c;
                                final PreparedStatement finalS = s;
                                Spliterator<double[]> ptsSplit = new Spliterator<>() {

                                    private boolean close() {
                                        try (AutoCloseable cc = finalC::close; AutoCloseable sc = finalS::close; AutoCloseable rsc = rs::close) {
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
        };
    }

    public static PointCloudResource loadElephants() throws DataStoreException {
        final HikariConfig dbConf = new HikariConfig();
        dbConf.setJdbcUrl("jdbc:postgresql://localhost:5432/examind");
        dbConf.setUsername("examind");
        dbConf.setPassword("examind");

        DataSource sqlSource = new HikariDataSource(dbConf);

        SQLStore store = new SQLStore(new SQLStoreProvider(), new StorageConnector(sqlSource), ResourceDefinition.query("elephantsdemer", "SELECT location FROM elephantsdemer"));
        var dataset = store.findResource("elephantsdemer");
        return new FeatureSetAsPointsCloud(dataset, false);
    }

    public static void main(String[] args) throws Exception {

        final var envelope = new Envelope2D(CRS_84, -180, -80, 360, 160);
        System.out.println("Envelope :" + envelope);
        final float distanceX = 0.25f, distanceY = 0.25f;
        System.out.println("Distances :" + distanceX);
        final Dimension tileSize = new Dimension(2048, 1024);
        final double[] line = IntStream.range(0, tileSize.width).mapToDouble(it -> 32767).toArray();
        final double[] column = IntStream.range(0, tileSize.height).mapToDouble(it -> 32767).toArray();

//        //-------------------------
//        // For batching comparison; imply to make HeatMapImage.BATCH_SIZE accessible and modifiable
//        for (int batchSize : List.of(100, 1_000, 10_000, 100_000, 1_000_000)) {
//            System.out.println("==========================\n==========================\n Test with batchSize : " + batchSize + "\n-------------\n-------------");
//            HeatMapImage.BATCH_SIZE = batchSize;


//        //-------------------------
//        // For algo comparison :
//            for (HeatMapImage.Algorithm algo : HeatMapImage.Algorithm.values()) {

        final var algo = HeatMapImage.Algorithm.GAUSSIAN_MASK;
                System.out.println("\n------------- Test with Algo : " + algo + "\n-------------");


        final PointCloudResource points = loadAIS(args[0], args[1], Instant.parse(args[2]), Instant.parse(args[3]));
        System.out.println("Statement : SELECT longitude, latitude FROM \"" + args[1] + "\"\nWHERE \"timestamp\" between '" + args[2] + "' and '" + args[3] + "'");
//        final PointCloudResource points = loadElephants();
//        System.out.println("Données éléphants de mer: ");

        {
            var startCount = System.nanoTime();
            System.out.println("Nombre de données : " + points.points(envelope, false).count());
            System.out.println("Count in " + (System.nanoTime() - startCount) / 1e9 + " s");
        }
//        var targetGrid = new GridGeometry(new GridExtent(2048, 1024), envelope, GridOrientation.DISPLAY);
        var targetGrid = new GridGeometry(new GridExtent(tileSize.width*4, tileSize.height*4), envelope, GridOrientation.DISPLAY);
        //TODO debug following commented code with wraparound problem with elephantdemer data
//        var targetGrid = new GridGeometry(new GridExtent(tileSize.width, tileSize.height), envelope, GridOrientation.DISPLAY);

        var colorRatio = 10_000d;

        // WARNING: Tiled images fail with AIS database, I do not know why...
        var heat = new HeatMapResource(points, tileSize, distanceX, distanceY, algo);

        for (int i = 0; i < 5; i++) {
            var start = System.nanoTime();
            var data = heat.read(targetGrid);
            var rendering = data.render(null);
            var buffer = new BufferedImage(rendering.getWidth(), rendering.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
            var stats = new Statistics("tile-compute-time");
            var valueStats = new Statistics("tile-non-zero-values");
            for (int x = rendering.getMinTileX(); x < rendering.getNumXTiles() + rendering.getMinTileX(); x++) {
                for (int y = rendering.getMinTileY(); y < rendering.getNumYTiles() + rendering.getMinTileY(); y++) {
                    var tileStart = System.nanoTime();
                    var tile = rendering.getTile(x, y);
                    var samples = tile.getPixels(tile.getMinX(), tile.getMinY(), tile.getWidth(), tile.getHeight(), (double[]) null);
                    for (var sample : samples) if (Math.abs(sample) > 1e-9) valueStats.accept(sample);
                    final int[] integerSamples = Arrays.stream(samples).mapToInt(value -> (int) XMath.clamp(value * colorRatio, 0, 65535)).toArray();
                    buffer.getRaster().setPixels(tile.getMinX(), tile.getMinY(), tile.getWidth(), tile.getHeight(), integerSamples);

                    // draw tile border
                    buffer.getRaster().setSamples(tile.getMinX(), tile.getMinY() + tile.getHeight() - 1, tile.getWidth(), 1, 0, line);
                    buffer.getRaster().setSamples(tile.getMinX() + tile.getWidth() - 1, tile.getMinY(), 1, tile.getHeight(), 0, column);

                    stats.accept(System.nanoTime() - tileStart);
                }
            }

            System.out.println("Rendered in " + (System.nanoTime() - start) / 1e9 + " s");
            System.out.printf("Statistics per tile:%n->%s%n->%s%n", stats, valueStats);
        }
//            }
//        }
    }
}
