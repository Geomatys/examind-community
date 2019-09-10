/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package org.constellation.database.configuration;

import org.constellation.exception.ConfigurationRuntimeException;
import org.constellation.database.model.FlywayUtils;
import org.constellation.business.IClusterBusiness;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;

/**
 * Bean used to initialize/migrate database in Spring context.
 *
 * @author Quentin Boileau (Geomatys)
 */
@Configuration
public class FlywaySpring {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.database.configuration");

    @Autowired
    @Qualifier(value = "dataSource")
    private DataSource dataSource;

    @Autowired
    private IClusterBusiness clusterBusiness;


    @PostConstruct
    public void migrate() throws ConfigurationRuntimeException {

        LOGGER.info("Start database migration check");
        boolean liquibaseInstalled = false;
        boolean liquibaseUpToDate = false;

        Lock lock = clusterBusiness.acquireLock("database-creation");
        lock.lock();
        LOGGER.finer("LOCK Acquired on cluster: database-creation");
        try {
            //search for previous installation using liquibase
            try (Connection conn = dataSource.getConnection()) {
                final DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet liquibaseTable = metaData.getTables(null, "public", "databasechangelog", null)) {
                    if (liquibaseTable.next()) {
                        liquibaseInstalled = true;

                        final String lastLBVersion = "SELECT 1 FROM public.databasechangelog WHERE id='version_1.45';";
                        try (Statement stmt = conn.createStatement();
                             ResultSet upToDate = stmt.executeQuery(lastLBVersion)) {

                            if (upToDate.next()) {
                                liquibaseUpToDate = true;
                            }
                        }
                    }
                }
            } catch (SQLException ex) {
                throw new ConfigurationRuntimeException("An error occurs during database analysis searching for " +
                        "previous installations.", ex);
            }

            try {
                final Flyway flyway = FlywayUtils.createFlywayConfig(dataSource);

                //create schema_version table if not exist, even if database is not empty
                flyway.setBaselineOnMigrate(true);

                //previous liquibase installation found but not up to date
                if (liquibaseInstalled) {
                    if (liquibaseUpToDate) {
                        //start after 1.1.0.0
                        flyway.setBaselineVersion(MigrationVersion.fromVersion("1.1.0.0"));
                        LOGGER.info("Previous installation with Liquibase detected and up to date, start migration from 1.1.0.0 patch");
                    } else {
                        throw new ConfigurationRuntimeException("Previous database installation found but not up to date, " +
                                "please update to 1.0.13 before applying this update.");
                    }
                }
                flyway.migrate();
            } catch (SQLException ex) {
                throw new ConfigurationRuntimeException(ex.getMessage(), ex);
            }

            //clean old liquibase changelogs
            if (liquibaseInstalled) {
                LOGGER.info("Drop old liquibase changelogs tables");
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS public.databasechangelog CASCADE;");
                    stmt.execute("DROP TABLE IF EXISTS public.databasechangeloglock CASCADE;");
                } catch (SQLException ex) {
                    throw new ConfigurationRuntimeException("Unable to delete old liquibase changelog tables", ex);
                }
            }
        } finally {
            LOGGER.finer("UNLOCK on cluster: database-creation");
            lock.unlock();
        }
    }
}
