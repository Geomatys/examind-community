package com.examind.image.heatmap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.sql.DataSource;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.coverage.grid.GridOrientation;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.math.Statistics;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.sql.ResourceDefinition;
import org.apache.sis.storage.sql.SQLStore;
import org.apache.sis.storage.sql.SQLStoreProvider;
import org.geotoolkit.math.XMath;

public class ProfileHeatMap {

    public static FeatureSet loadDataset() throws DataStoreException {
        final HikariConfig dbConf = new HikariConfig();
        dbConf.setJdbcUrl("jdbc:postgresql://localhost:5432/examind");
        dbConf.setUsername("examind");
        dbConf.setPassword("examind");

        DataSource sqlSource = new HikariDataSource(dbConf);

        SQLStore store = new SQLStore(new SQLStoreProvider(), new StorageConnector(sqlSource), ResourceDefinition.query("elephantsdemer", "SELECT location FROM elephantsdemer"));
        return store.findResource("elephantsdemer");
    }

    public static void main(String[] args) throws Exception {
        final PointCloudResource points = new FeatureSetAsPointsCloud(loadDataset());
        var heat = new HeatMapResource(points, new Dimension(256, 256), 50, 50, HeatMapImage.Algorithm.GAUSSIAN);
        var targetGrid = new GridGeometry(new GridExtent(2048, 1024), new Envelope2D(CommonCRS.defaultGeographic(), -160, 30, 60, 30), GridOrientation.DISPLAY);

        for (int i = 0 ; i < 5 ; i++) {
            var start = System.nanoTime();
            var data = heat.read(targetGrid);
            var rendering = data.render(null);
            var buffer = new BufferedImage(rendering.getWidth(), rendering.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
            var stats = new Statistics("tile-compute-time");
            var valueStats = new Statistics("tile-non-zero-values");
            for (int x = rendering.getMinTileX() ; x < rendering.getNumXTiles() + rendering.getMinTileX() ; x++) {
                for (int y = rendering.getMinTileY() ; y < rendering.getNumYTiles() + rendering.getMinTileY() ; y++) {
                    var tileStart = System.nanoTime();
                    var tile = rendering.getTile(x, y);
                    var samples = tile.getPixels(tile.getMinX(), tile.getMinY(), tile.getWidth(), tile.getHeight(), (double[]) null);
                    for (var sample : samples) if (Math.abs(sample) > 1e-9) valueStats.accept(sample);
                    final int[] integerSamples = Arrays.stream(samples).mapToInt(value -> (int) XMath.clamp(value * 500, 0, 65535)).toArray();
                    buffer.getRaster().setPixels(tile.getMinX(), tile.getMinY(), tile.getWidth(), tile.getHeight(), integerSamples);
                    stats.accept(System.nanoTime() - tileStart);
                }
            }

            System.out.println("Rendered in " + (System.nanoTime() - start) / 1e6 + " ms");
            System.out.printf("Statistics per tile:%n->%s%n->%s%n", stats, valueStats);
        }
    }
}
