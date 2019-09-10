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
import java.io.PrintWriter;
import java.sql.*;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IClusterBusiness;
import org.geotoolkit.lang.Setup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Install EPSG database on given datasource (mostly same datasource of Constellation database)
 *
 * @author Quentin Boileau (Geomatys)
 */
public class EPSGDatabaseIniter {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.database.configuration");

    @Autowired
    private IClusterBusiness clusterBusiness;

    /**
     * Some EPSG database table used to check if database already installed.
     */
    private static final String[] SAMPLES = {
            "Coordinate Reference System",
            "coordinatereferencesystem",
            "epsg_coordinatereferencesystem"
    };

    private static final AtomicReference<DataSource> DATASOURCE = new AtomicReference<>(null);

    @PostConstruct
    public void init() {
        Lock lock = clusterBusiness.acquireLock("epsg-database-creation");
        lock.lock();
        LOGGER.finer("LOCK Acquired on cluster: epsg-database-creation");
        try {
            //set datasource used by geotoolkit EPSG database
            final DataSource dataSource = DATASOURCE.get();
            Setup.setEPSG(dataSource);

            if (exists(dataSource)) {
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
        DATASOURCE.set(null);
        Setup.setEPSG(null);
    }

    public static DataSource getDataSource() {
        return DATASOURCE.get();
    }

    public void setDataSource(DataSource dataSource) {
        if(DATASOURCE.getAndSet(dataSource)!=null){
            throw new IllegalStateException("Datasource already configured, close previous spring context before starting a new one.");
        }
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

            final ResultSet result = md.getTables(null, "epsg", null, new String[] {"TABLE"});
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

    /**
     * Factory used to create epsg datasource.
     * The returned datasource is backed by given spring hikary datasource.
     */
    public static class Factory implements ObjectFactory{

        @Override
        public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
            return new DelayedDataSource();
        }

    }

    private static class DelayedDataSource implements DataSource{

        @Override
        public Connection getConnection() throws SQLException {
            return getDataSource().getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getDataSource().getConnection(username, password);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return getDataSource().getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            getDataSource().setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            getDataSource().setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return getDataSource().getLoginTimeout();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return getDataSource().getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return getDataSource().unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return getDataSource().isWrapperFor(iface);
        }

    }

}
