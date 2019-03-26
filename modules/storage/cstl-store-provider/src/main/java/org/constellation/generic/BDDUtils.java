/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.generic;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.constellation.dto.service.config.generic.BDD;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.jdbc.DBCPDataSource;
import org.geotoolkit.jdbc.WrappedDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.ds.common.BaseDataSource;

/**
 *
 * @author guilhem
 */
public class BDDUtils {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.generic.database");


    /**
     * Return a new connection to the database.
     *
     * @return
     * @throws java.sql.SQLException
     */
    public static DataSource getDataSource(String className, String connectURL, String user, String password) throws SQLException {
        final DataSource source;
        // by Default  we use the postgres driver.
        if (className == null) {
            className = BDD.POSTGRES_DRIVER_CLASS;
        }
        switch (className) {
            case BDD.POSTGRES_DRIVER_CLASS:
                if (connectURL != null && connectURL.startsWith("jdbc:postgresql://")) {
                    final PGSimpleDataSource pgSource = new PGSimpleDataSource();
                    fillSourceFromURL(pgSource, connectURL, user, password);
                    source = pgSource;
                } else {
                    return null;
                }
                break;
            case BDD.ORACLE_DRIVER_CLASS:
                source = (DataSource) createDataSourceByReflection("oracle.jdbc.pool.OracleDataSource", connectURL, user, password);
                break;
            default:
                source = new DefaultDataSource(connectURL);
                break;
        }
        return source;
    }

    /**
     * Return a new connection to the database.
     *
     * @return
     */
    public static DataSource getPooledDataSource(String className, String connectURL, String user, String password) {
        final DataSource source;
        // by Default  we use the postgres driver.
        if (className == null) {
            className = BDD.POSTGRES_DRIVER_CLASS;
        }
        switch (className) {
            case BDD.POSTGRES_DRIVER_CLASS:
                if (connectURL != null && connectURL.startsWith("jdbc:postgresql://")) {
                    //final PGConnectionPoolDataSource pgSource = new PGConnectionPoolDataSource();
                    final BasicDataSource dataSource = new BasicDataSource();

                    // some default data source behaviour
                    dataSource.setPoolPreparedStatements(true);

                    // driver
                    dataSource.setDriverClassName(BDD.POSTGRES_DRIVER_CLASS);

                    // url
                    dataSource.setUrl(connectURL);

                    // username
                    dataSource.setUsername(user);

                    // password
                    if (password != null) {
                        dataSource.setPassword(password);
                    }

                    /* max wait
                    final Integer maxWait = (Integer) params.parameter(MAXWAIT.getName().toString()).getValue();
                    if (maxWait != null && maxWait != -1) {
                        dataSource.setMaxWait(maxWait * 1000);
                    }

                    // connection pooling options
                    final Integer minConn = (Integer) params.parameter(MINCONN.getName().toString()).getValue();
                    if ( minConn != null ) {
                        dataSource.setMinIdle(minConn);
                    }

                    final Integer maxConn = (Integer) params.parameter(MAXCONN.getName().toString()).getValue();
                    if ( maxConn != null ) {
                        dataSource.setMaxActive(maxConn);
                    }

                    final Boolean validate = (Boolean) params.parameter(VALIDATECONN.getName().toString()).getValue();
                    if(validate != null && validate && getValidationQuery() != null) {
                        dataSource.setTestOnBorrow(true);
                        dataSource.setValidationQuery(getValidationQuery());
                    }*/

                    // some datastores might need this
                    dataSource.setAccessToUnderlyingConnectionAllowed(true);

                    return new DBCPDataSource(dataSource);
                } else {
                    return null;
                }
            case BDD.ORACLE_DRIVER_CLASS:
                try {
                    source = new WrappedDataSource((ConnectionPoolDataSource)
                            createDataSourceByReflection("oracle.jdbc.pool.OracleConnectionPoolDataSource", connectURL, user, password));
                } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "SQLException while creating oracle datasource", ex);
                    return null;
                }
                break;
            default:
                source = new DefaultDataSource(connectURL);
                break;
        }
        return source;
    }

    /**
     * Fill the dataSource supplied with the informations extracted from the database URL.
     * @param pgSource
     */
    private static  void fillSourceFromURL(final BaseDataSource pgSource, String connectURL, String user, String password) {
         // exemple : jdbc:postgresql://localhost:5432/mydb-SML
         String url = connectURL.substring(18);
        final String host;
        final int port;
        if (url.indexOf(':') != -1) {
            host = url.substring(0, url.indexOf(':'));
            url = url.substring(url.indexOf(':') + 1);
            final String sPort = url.substring(0, url.indexOf('/'));
            port = Integer.parseInt(sPort);
        } else {
            host = url.substring(0, url.indexOf('/'));
            port = 5432;
            LOGGER.finer("Using default postgres post 5432");
        }

        final String dbName = url.substring(url.indexOf('/') + 1);

        pgSource.setServerName(host);
        pgSource.setPortNumber(port);
        pgSource.setDatabaseName(dbName);
        pgSource.setUser(user);
        pgSource.setPassword(password);
    }

    /**
     * Creates a data source for the given classname using the reflection API.
     * This avoid direct dependency to a driver that we can not redistribute.
     */
    private static Object createDataSourceByReflection(final String classname, String connectURL, String user, String password) throws SQLException {
        try {
            final Class<?> c = Class.forName(classname);
            final Object source = c.newInstance();
            c.getMethod("setURL",      String.class).invoke(source, connectURL);
            c.getMethod("setUser",     String.class).invoke(source, user);
            c.getMethod("setPassword", String.class).invoke(source, password);
            return source;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                throw (SQLException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new UndeclaredThrowableException(e);
        }
    }
}
