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

import org.apache.sis.util.logging.Logging;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.ScriptRunner;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotoolkit.nio.IOUtilities;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @since 0.9
 */
public class OM2DatabaseCreator {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.om2");

    private final static String LAST_VERSION = "1.0.9";

    /**
     * Fill a new PostgreSQL database with the O&amp;M model.
     *
     * @param dataSource A postgreSQL dataSource.
     *
     * @throws SQLException if an error occurs while filling the database.
     * @throws IllegalArgumentException if the dataSource is null.
     */
    public static void createObservationDatabase(final DataSource dataSource, final boolean isPostgres, final File postgisInstall, String schemaPrefix) throws SQLException, IOException {
        if (dataSource == null) {
            throw new IllegalArgumentException("The DataSource is null");
        }

        if (schemaPrefix == null) {
            schemaPrefix = "";
        }

        try(final Connection con  = dataSource.getConnection()) {

            final ScriptRunner sr = new ScriptRunner(con);
            if (isPostgres) {
                execute("org/constellation/om2/structure_observations_pg.sql", sr, schemaPrefix);
            } else {
                execute("org/constellation/om2/structure_observations.sql", sr, schemaPrefix);
            }
            LOGGER.info("O&M 2 database created");

            sr.close(false);
        }
    }

    public static boolean structurePresent(final DataSource source, final String schemaPrefix) {
        if (source != null) {
            if (Util.containsForbiddenCharacter(schemaPrefix)) {
                throw new IllegalArgumentException("Invalid schema prefix value");
            }
            try (final Connection con = source.getConnection();
                 final Statement stmt = con.createStatement()) {

                try (final ResultSet resultObs = stmt.executeQuery("SELECT * FROM \"" + schemaPrefix + "om\".\"observed_properties\"")) {//NOSONAR
                    resultObs.next();
                }

                // version table was before in public schema, now each schema has its version table.
                updateTableVersion(con, schemaPrefix);

                return true;
            } catch (SQLException ex) {
                LOGGER.log(Level.FINER, "missing table in OM database", ex);
            }
        }
        return false;
    }

    public static boolean isPostgisInstalled(final DataSource source, boolean isPostgres) {
        if (!isPostgres) return true;
        if (source != null) {
            try (final Connection con = source.getConnection();
                 final Statement stmt = con.createStatement()) {
                try (final ResultSet result = stmt.executeQuery("SELECT postgis_full_version()")){
                    result.next();
                } catch(SQLException ex) {
                    // try to install it
                    stmt.execute("CREATE EXTENSION postgis");
                }
                return true;
            } catch(SQLException ex) {
                return false;
            }
        }
        return false;
    }

    private static boolean newVersionTableMissing(final Connection con, final String schemaPrefix) {
        if (con != null) {
            if (Util.containsForbiddenCharacter(schemaPrefix)) {
                throw new IllegalArgumentException("Invalid schema prefix value");
            }
            try (final Statement stmt = con.createStatement();
                 final ResultSet result = stmt.executeQuery("SELECT * FROM \"" + schemaPrefix + "om\".\"version\"")) {//NOSONAR
                result.next();
                return false;
            } catch(SQLException ex) {
                return true;
            }
        }
        return false;
    }

    private static void updateTableVersion(final Connection con, final String schemaPrefix) throws SQLException {
        if (Util.containsForbiddenCharacter(schemaPrefix)) {
            throw new IllegalArgumentException("Invalid schema prefix value");
        }
        if (newVersionTableMissing(con, schemaPrefix)) {
            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate("CREATE TABLE \"" + schemaPrefix + "om\".\"version\" (\"number\"   character varying(10) NOT NULL);");//NOSONAR
                stmt.executeUpdate("INSERT INTO \"" + schemaPrefix + "om\".\"version\" VALUES ('1.0.5');");//NOSONAR
                stmt.executeUpdate("ALTER TABLE \"" + schemaPrefix + "om\".\"version\" ADD CONSTRAINT version_pk PRIMARY KEY (\"number\");");//NOSONAR
            }
        }
    }

    public static boolean updateStructure(final DataSource source, final String schemaPrefix, final boolean isPostgres) {
        if (source != null) {
            try (final Connection con = source.getConnection()) {

                String version = getVersion(con, schemaPrefix);
                if (version != null && !LAST_VERSION.equals(version)) {
                    final ScriptRunner sr = new ScriptRunner(con);
                    switch (version) {
                        case "1.0.0": execute("org/constellation/om2/update/update101.sql", sr, schemaPrefix);
                        case "1.0.1": execute("org/constellation/om2/update/update102.sql", sr, schemaPrefix);
                        case "1.0.2": execute("org/constellation/om2/update/update103.sql", sr, schemaPrefix);
                        case "1.0.3": if (isPostgres) {
                                        execute("org/constellation/om2/update/update104_pg.sql", sr, schemaPrefix);
                                      } else {
                                        execute("org/constellation/om2/update/update104.sql", sr, schemaPrefix);
                                      }
                        case "1.0.4": execute("org/constellation/om2/update/update105.sql", sr, schemaPrefix);
                        case "1.0.5": execute("org/constellation/om2/update/update106.sql", sr, schemaPrefix);
                        case "1.0.6": execute("org/constellation/om2/update/update107.sql", sr, schemaPrefix);
                        case "1.0.7": execute("org/constellation/om2/update/update108.sql", sr, schemaPrefix);
                        case "1.0.8": execute("org/constellation/om2/update/update109.sql", sr, schemaPrefix);
                    }
                    return true;
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.FINER, "Error during database update.", ex);
            }
        }
        return false;
    }

    private static String getVersion(final Connection con, final String schemaPrefix) throws SQLException {
        if (Util.containsForbiddenCharacter(schemaPrefix)) {
            throw new IllegalArgumentException("Invalid schema prefix value");
        }
        try (final Statement stmt = con.createStatement();
             final ResultSet result = stmt.executeQuery("SELECT * FROM \"" + schemaPrefix + "om\".\"version\"")) {//NOSONAR
            if (result.next()) {
                return result.getString(1);
            }
            return null;
        }
    }

    public static boolean validConnection(final DataSource source) {
        try (final Connection con = source.getConnection()) {
            return true;
        } catch (SQLException ex) {
            LOGGER.log(Level.FINER, "unable to connect", ex);
        }
        return false;
    }

    /**
     * Execute the SQL script pointed by the specified path.
     *
     * @param path A path in the resource files to a SQL script.
     * @param runner A SQL script runner connected to a database.
     */
    private static void execute(final String path, final ScriptRunner runner, final String schemaPrefix) {
        try {
            String sql = IOUtilities.toString(Util.getResourceAsStream(path));
            sql = sql.replace("$SCHEMA", schemaPrefix);
            runner.run(sql);
         } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "IO exception while executing SQL script", ex);
        } catch (SQLException ex) {
            LOGGER.severe("SQLException creating statement: " + runner.getCurrentPosition() + " in " + path + " file.\n" + ex.getMessage());
        }
    }
}
