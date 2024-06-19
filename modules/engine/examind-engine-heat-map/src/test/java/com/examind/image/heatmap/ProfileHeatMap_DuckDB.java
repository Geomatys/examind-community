package com.examind.image.heatmap;

import com.examind.image.pointcloud.DuckDbPointCloud;
import com.examind.image.pointcloud.PointCloudResource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.sis.geometry.Envelope2D;

import static com.examind.image.heatmap.ProfileHeatMap.CRS_84;
import static com.examind.image.heatmap.ProfileHeatMap.profilePointCloud;

public class ProfileHeatMap_DuckDB {

    public static void simpleReadParquet(final String parquet_path) throws SQLException, ClassNotFoundException {

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            final String sql = "SELECT longitude, latitude from read_parquet('" + parquet_path + "/*.parquet')";
            final ResultSet resultSet = stmt.executeQuery(sql);
            ResultSetMetaData loRsmd = resultSet.getMetaData();
            int nbColonnes = loRsmd.getColumnCount();
            int count = 0;
            long start = System.currentTimeMillis();
            while (resultSet.next()) {
                for (int i = 1; i <= nbColonnes; i++) {
//                    if (i > 1) System.out.print(",  ");
//                    String valeur = resultSet.getString(i);
                    Double valeur = resultSet.getDouble(i);
//                    System.out.print(laValeur);
                }
                count++;
//                System.out.println();
            }
            System.out.println("read " + count + " in " + (System.currentTimeMillis() - start) / 1000d + "s");
        }

    }


    /**
     * @param args : args[0] -> path to the directory containing the .parquets files,
     *             args[1], args[2] -> names of the longitude and latitude columns respectively
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Class.forName("org.duckdb.DuckDBDriver");
        
/*        final String root_parquet_path = "~/Documents/doc_ifremer/geoviz.snappy.parq/";
        String root_parquet_path = args[0];
        ArgumentChecks.ensureNonNull("parquets directory's path", root_parquet_path);
        if (root_parquet_path.endsWith("/"))
            root_parquet_path = root_parquet_path.substring(0, root_parquet_path.length() - 1);
        final String parquet_path_query = root_parquet_path;
        System.out.println("simple read:\n==================");
        simpleReadParquet(parquet_path_query);

        System.out.println("heatMap read:\n==================");*/
        String dbPath = "/home/glegal/Sources/workspace_qualinov/qualinov-app/submodules/examind-community/docker/mount/examind/data/ddb/test.db";
        final PointCloudResource points = new DuckDbPointCloud(dbPath, "argo", null, "longitude", "latitude");
        final var env = new Envelope2D(CRS_84, -180, -90, 360, 180);
        /*========================
        * Base HeatMap Computation
        //========================*/
////        final float distanceX = 0.25f, distanceY = 0.25f;
//        final float distanceX = 0.05f, distanceY = 0.05f;
//        final double max = 271.45937;
        /*==============================
        * Simplified HeatMap Computation
        //==============================*/
        final float distanceX = 0f, distanceY = 0f;
//        final double max = 728.424;
        final double max = 175;

        var colorRatio = 65535d / max;
//        var colorRatio = 10000;
//        profilePointCloud(points, env, distanceX, distanceY, v -> v * colorRatio );
        profilePointCloud(points, env, distanceX, distanceY, v -> v != 0 ? v * colorRatio + 65535 / 5 : 0);
    }


}
