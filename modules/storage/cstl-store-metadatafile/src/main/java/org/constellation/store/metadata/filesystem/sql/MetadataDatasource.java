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
package org.constellation.store.metadata.filesystem.sql;

import java.io.ByteArrayInputStream;
import org.constellation.util.Util;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.nio.IOUtilities;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataDatasource {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.metadata.io.filesystem.sql");

    private final DataSource source;

    private final String storeID;

    public MetadataDatasource(final DataSource source, final String storeID) throws MetadataIoException {
        this.source = source;
        if (Util.containsForbiddenCharacter(storeID)) {
            throw new MetadataIoException("Invalid store id value");
        }
        this.storeID = storeID;
    }

    /**
     * Obtains a csw database {@link org.constellation.store.metadata.filesystem.sql.Session} instance.
     *
     * @return a {@link org.constellation.store.metadata.filesystem.sql.Session} instance
     *
     * @throws  SQLException if a database access error occurs
     * @throws org.geotoolkit.metadata.MetadataIoException If the schema prefix contains an invalid character.
     */
    public Session createSession() throws SQLException, MetadataIoException {
        final Connection c = source.getConnection();
        setup(c);
        return new Session(c, storeID);
    }

    public void destroySchema() throws SQLException {
        try (final Connection c = source.getConnection();
             Statement stmt = c.createStatement()) {
            stmt.executeUpdate("DROP SCHEMA \"" + storeID + "\" CASCADE");//NOSONAR
        }
    }

    /**
     * Sets static connection variables and check if the csw schema named
     * {@code "csw"} exists on the current {@link Connection}.
     * <p />
     * If the schema is missing create it executing the {@code create-csw-db.sql} resource file.
     *
     * @throws SQLException if an error occurred while connecting to database or executing a SQL statement
     */
    private void setup(final Connection con) throws SQLException {

        // Establish connection and create schema if does not exist.
        try {
            if (!schemaExists(con, storeID)) {
                // Load database schema SQL stream.
                String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/csw/filesystem/sql/v1/create-csw-db.sql"));
                sql = sql.replace("$schema", storeID);
                final InputStream stream = new ByteArrayInputStream(sql.getBytes());

                // Create schema.
                final ScriptRunner runner = new ScriptRunner(con);
                runner.run(stream);
                runner.close(false);
            }
        } catch (IOException unexpected) {
            throw new IllegalStateException("Unexpected error occurred while trying to create csw database schema.", unexpected);
        }
    }

    private static boolean schemaExists(final Connection connect, final String schemaName) throws SQLException {
        ensureNonNull("schemaName", schemaName);
        final ResultSet schemas = connect.getMetaData().getSchemas();
        while (schemas.next()) {
            if (schemaName.equals(schemas.getString(1))) {
                return true;
            }
        }
        return false;
    }
}
