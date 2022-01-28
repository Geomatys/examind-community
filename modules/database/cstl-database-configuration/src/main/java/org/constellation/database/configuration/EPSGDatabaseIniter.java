/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IClusterBusiness;
import org.geotoolkit.lang.Setup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Install EPSG database on given datasource (mostly same datasource of Constellation database)
 *
 * @author Quentin Boileau (Geomatys)
 */
public class EPSGDatabaseIniter {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.database.configuration");

    @Autowired
    private IClusterBusiness clusterBusiness;

    @Autowired
    @Qualifier("epsgDataSource")
    private DataSource epsgDatasource;

    /**
     * Some EPSG database table used to check if database already installed.
     */
    private static final String[] SAMPLES = {
            "Coordinate Reference System",
            "coordinatereferencesystem",
            "epsg_coordinatereferencesystem"
    };

    @PostConstruct
    public void init() {
        Lock lock = clusterBusiness.acquireLock("epsg-database-creation");
        lock.lock();
        LOGGER.finer("LOCK Acquired on cluster: epsg-database-creation");
        try {
            //set datasource used by geotoolkit EPSG database
            Setup.setEPSG(epsgDatasource);

            if (exists(epsgDatasource)) {
                LOGGER.info("EPSG database already installed.");
            } else {
                //force loading or creating the epsg schema now that the datasource is available
                CRS.forCode("EPSG:2154");
                CRS.forCode("EPSG:3395");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to connect to database " + e.getMessage(), e);
        } finally {
            LOGGER.finer("UNLOCK on cluster: epsg-database-creation");
            lock.unlock();
        }

        LOGGER.info(org.apache.sis.setup.About.configuration().toString());
    }

    @PreDestroy
    public void destroy() {
        Setup.setEPSG(null);
    }

    /**
     * Check if EPSG database is already installed.
     *
     * @param dataSource
     * @returnq
     * @throws IOException
     */
    private synchronized boolean exists(DataSource dataSource) throws IOException {
        try (Connection conn = dataSource.getConnection()) {
            final DatabaseMetaData md = conn.getMetaData();
            LOGGER.info("Check EPSG database installation on " + md.getURL());

            final ResultSet result = md.getTables(null, "EPSG", null, new String[] {"TABLE"});
            while (result.next()) {
                final String table = result.getString("TABLE_NAME");
                for (final String candidate : SAMPLES) {
                    if (candidate.equalsIgnoreCase(table)) {
                        return true;
                    }
                }
            }
            return false;

        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
