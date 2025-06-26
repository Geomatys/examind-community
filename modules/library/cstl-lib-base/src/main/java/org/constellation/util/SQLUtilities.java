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
package org.constellation.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.sql.DataSource;
import org.constellation.exception.ConfigurationRuntimeException;

/**
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class SQLUtilities {

    /**
     * Build a SQL datasource (simple version often used for derby datasource).
     *
     * @param connectURL JDBC datasource url.
     *
     * @return A SQL Datasource.
     */
    public static DataSource getDataSource(String connectURL) {
        return getDataSource(connectURL, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Build a SQL datasource.
     * 
     * @param className A JDBC driver class name or {@code null}
     * @param connectURL JDBC datasource url.
     * @param user user name.
     * @param password user pwd.
     *
     * @return A SQL Datasource.
     */
    public static DataSource getDataSource(String connectURL, String className, String user, String password) {
        return getDataSource(connectURL, className, null, user, password, null, null, null, null, null, null);
    }
    
    /**
     * Build an Hikari datasource.
     *
     * @param databaseURL A database URL in Hiroku like format with included username/password.
     * @param poolName Name assigned to the connection pool.
     * @param maxPoolSize Maximum pool size. If null use Hikari default value.
     * @param leakDetectionThreshold This property controls the amount of time that a connection can be out of the pool before a message is
     * logged indicating a possible connection leak. can be {@code null}.
     * @param minIdle The minimum number of idle connections that HikariCP tries to maintain in the pool,
     * including both idle and in-use connections.
     * @param idleTimeout The maximum amount of time (in milliseconds) that a connection is allowed to sit
     * idle in the pool. A value of 0 means that idle connections are never removed from the pool.
     * @param dsPropertie Datasource properties. Can be {@code null}.
     * @param readOnly Set read only on the datasource. Can be {@code null}.
     * 
     * @return An Hikari datasource.
     */
    public static DataSource getDataSource(String databaseURL, String poolName, Integer maxPoolSize, Long leakDetectionThreshold, Integer minIdle, Long idleTimeout, Properties dsPropertie, Boolean readOnly) {
        var userInfos = extractUserPasswordUrl(databaseURL);
        return getDataSource(userInfos[0], null, poolName, userInfos[1], userInfos[2], maxPoolSize, leakDetectionThreshold, minIdle, idleTimeout, dsPropertie, readOnly);
    }

    /**
     * Build an Hikari datasource.
     *
     * @param databaseURL A database URL in Hiroku like format NOT including username/password.
     * @param className A JDBC driver class name or {@code null}
     * @param poolName Name assigned to the connection pool.
     * @param userName user name.
     * @param password user pwd.
     * @param maxPoolSize Maximum pool size. If null use Hikari default value.
     * @param leakDetectionThreshold This property controls the amount of time that a connection can be out of the pool before a message is.
     * @param minIdle The minimum number of idle connections that HikariCP tries to maintain in the pool,
     * including both idle and in-use connections.
     * @param idleTimeout The maximum amount of time (in milliseconds) that a connection is allowed to sit
     * idle in the pool. A value of 0 means that idle connections are never removed from the pool.
     * @param dsPropertie Datasource properties. Can be {@code null}.
     * @param readOnly Set read only on the datasource. Can be {@code null}.
     * @return
     */
    public static DataSource getDataSource(String databaseURL, String className, String poolName, String userName, String password, Integer maxPoolSize, 
            Long leakDetectionThreshold, Integer minIdle, Long idleTimeout, Properties dsPropertie, Boolean readOnly) {
        HikariConfig config = createHikariConfig(poolName, className, maxPoolSize, databaseURL, userName, password, leakDetectionThreshold, minIdle, idleTimeout, dsPropertie, readOnly);
        return new HikariDataSource(config);
    }
    
    
    public static HikariConfig createHikariConfig(String connectURL, String className, String user, String password) {
        return createHikariConfig(null, className, null, connectURL, user, password, null, null, null, null, null);
    }

    /**
     * Build an Hikaru configuration.
     *
     * @param poolName Name assigned to the connection pool.
     * @param className A JDBC driver class name or {@code null}
     * @param maxPoolSize Maximum pool size. If null use Hikari default value.
     * @param jdbcUrl  An JDBC database URL. accept hiroku form.
     * @param userName User name.
     * @param password User password.
     * @param leakDetectionThreshold This property controls the amount of time that a connection can be out of the pool before a message is
     * logged indicating a possible connection leak. can be {@code null}.
     * @param minIdle The minimum number of idle connections that HikariCP tries to maintain in the pool,
     * including both idle and in-use connections.
     * @param idleTimeout The maximum amount of time (in milliseconds) that a connection is allowed to sit
     * idle in the pool. A value of 0 means that idle connections are never removed from the pool.
     * @param dsProperties Datasource properties. Can be {@code null}.
     * @param readOnly Set read only on the datasource. Can be {@code null}.
     *
     * @return An Hikari configuration.
     */
    public static HikariConfig createHikariConfig(String poolName, String className, Integer maxPoolSize, String jdbcUrl, String userName,
            String password, Long leakDetectionThreshold, Integer minIdle, Long idleTimeout, Properties dsProperties, Boolean readOnly) {
        HikariConfig config = new HikariConfig();
        jdbcUrl = convertToJDBCUrl(jdbcUrl);
        config.setJdbcUrl(jdbcUrl);

        config.setUsername(userName);
        config.setPassword(password);

        if (className == null) {
            if (jdbcUrl.startsWith("jdbc:hsqldb")) {
                className = "org.hsqldb.jdbc.JDBCDriver";
            } else if (jdbcUrl.startsWith("jdbc:duckdb")) {
                className = "org.duckdb.DuckDBDriver";
            } else if (jdbcUrl.startsWith("jdbc:postgres")) {
                className = "org.postgresql.Driver";
            } else if (jdbcUrl.startsWith("jdbc:derby")) {
                className = "org.apache.derby.jdbc.EmbeddedDriver";
            } else {
                throw new IllegalArgumentException("Unable to find a driver for jdbc url: " + jdbcUrl);
            }
        }
        config.setDriverClassName(className);
        if (poolName != null) {
            config.setPoolName(poolName);
        }
        if (maxPoolSize != null) {
            config.setMaximumPoolSize(maxPoolSize);
        }
        if (leakDetectionThreshold != null) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }
        if (minIdle != null) {
            config.setMinimumIdle(minIdle);
        }
        if (idleTimeout != null) {
            config.setIdleTimeout(idleTimeout);
        }
        if (readOnly != null) {
            config.setReadOnly(readOnly);
            
            // special case for duckbd, the read only property must be set on properties
            if ("org.duckdb.DuckDBDriver".equals(className)) {
                if (dsProperties == null) dsProperties = new Properties();
                dsProperties.setProperty("duckdb.read_only", readOnly.toString());   
            }
        }
        if (dsProperties != null) {
            config.setDataSourceProperties(dsProperties);
        }
        return config;
    }

    /**
     * Parse and convert database URL in Hiroku form into JDBC compatible URL.
     * Only support postgres, derby and hsql JDBC url.
     *
     * @param databaseURL
     * @return JDBC url String
     * @throws ConfigurationRuntimeException
     */
    public static String convertToJDBCUrl(String databaseURL) throws ConfigurationRuntimeException {
        if (databaseURL == null || (databaseURL = databaseURL.trim()).isEmpty()) {
            throw new ConfigurationRuntimeException("Input database url is blank");
        }
        // Consider user already gave a standard jdbc URL
        if (databaseURL.toLowerCase().startsWith("jdbc:")) {
            return databaseURL;
        }
        URI dbUri;
        try {
            dbUri = new URI(databaseURL);
        } catch (URISyntaxException e) {
            throw new ConfigurationRuntimeException("", e);
        }

        String scheme = dbUri.getScheme();
        if (scheme.equals("derby")) {
            if (!databaseURL.contains("create=true")) {
                databaseURL = databaseURL.concat(";create=true");
            }
            return "jdbc:"+databaseURL;
        } else if (scheme.equals("hsqldb")) {
            return "jdbc:"+databaseURL;
        }

        if (scheme.equals("postgres")) {
            scheme = "postgresql";
        }

        return  "jdbc:"+ scheme +"://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
    }

    /**
     * Parse and extract user infos (login, password) from a database URL in Hiroku form.
     * Remove then the user infos from the url and return it.
     *
     *
     * @param databaseURL A database URL in Hiroku like format.
     *
     * @return A String array with url / user name / password.
     * @throws ConfigurationRuntimeException
     */
    public static String[] extractUserPasswordUrl(String databaseURL) throws ConfigurationRuntimeException {
        URI dbUri;
        try {
            dbUri = new URI(databaseURL);
        } catch (URISyntaxException e) {
            throw new ConfigurationRuntimeException("", e);
        }
        if (dbUri.getUserInfo() != null) {
            final String username = dbUri.getUserInfo().split(":")[0];
            final String password = dbUri.getUserInfo().split(":")[1];
            databaseURL = databaseURL.replace(username + ':' + password + '@', "");
            return new String[]{databaseURL, username, password};
        }
        return new String[]{databaseURL, null, null};
    }

    public static String getSGBDType(String databaseURL) {
        if (databaseURL.startsWith("jdbc:")) {
            databaseURL = databaseURL.substring(5);
        }
        int i = databaseURL.indexOf(':');
        return databaseURL.substring(0, i);
    }
}
