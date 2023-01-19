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
import java.util.AbstractMap;
import java.util.Map;
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
        return getDataSource(null, connectURL, null, null);
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
    public static DataSource getDataSource(String className, String connectURL, String user, String password) {
        HikariConfig config = new HikariConfig();
        if (className != null) {
            config.setDriverClassName(className);
        }
        config.setJdbcUrl(connectURL);
        config.setUsername(user);
        config.setPassword(password);
        return new HikariDataSource(config);
    }

    /**
     * Parse and convert database URL in Hiroku form into JDBC compatible URL.
     * Only support postgres, derby and hsql JDBC url.
     *
     * @param databaseURL
     * @return JDBC url String
     * @throws ConfigurationRuntimeException
     */
    static String convertToJDBCUrl(String databaseURL) throws ConfigurationRuntimeException {
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
     *
     * @param databaseURL A database URL in Hiroku like format.
     *
     * @return A map entry username => password
     * @throws ConfigurationRuntimeException
     */
    static Map.Entry<String, String> extractUserPassword(String databaseURL) throws ConfigurationRuntimeException {
        URI dbUri;
        try {
            dbUri = new URI(databaseURL);
        } catch (URISyntaxException e) {
            throw new ConfigurationRuntimeException("", e);
        }
        if (dbUri.getUserInfo() != null) {
            final String username = dbUri.getUserInfo().split(":")[0];
            final String password = dbUri.getUserInfo().split(":")[1];
            return new AbstractMap.SimpleImmutableEntry<>(username, password);
        }
        return null;
    }

    /**
     * Build an Hikari configuration.
     * 
     * @param databaseURL A database URL in Hiroku like format.
     * @param poolName pool name optional
     * @param maxPoolSize maximum pool size. If null use Hikari default value
     *
     * @return An Hikari configuration.
     */
    private static HikariConfig createHikariConfig(String databaseURL, String poolName, Integer maxPoolSize, Long leakDetectionThreshold) {
        final String dbUrl = convertToJDBCUrl(databaseURL);
        final Map.Entry<String, String> userInfo = extractUserPassword(databaseURL);

        String user = null;
        String password = null;

        if (userInfo != null) {
            user = userInfo.getKey();
            password = userInfo.getValue();
        }
        return createHikariConfig(poolName, maxPoolSize, dbUrl, user, password, leakDetectionThreshold);
    }

    /**
     * Build an Hikaru configuration.
     *
     * @param poolName Name assigned to the connection pool.
     * @param maxPoolSize Maximum pool size. If null use Hikari default value.
     * @param jdbcUrl  An JDBC database URL.
     * @param userName User name.
     * @param password User password.
     * @param leakDetectionThreshold This property controls the amount of time that a connection can be out of the pool before a message is
    * logged indicating a possible connection leak. can be {@code null}.
    *
     * @return An Hikari configuration.
     */
    private static HikariConfig createHikariConfig(String poolName, Integer maxPoolSize, String jdbcUrl, String userName, String password, Long leakDetectionThreshold) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);

        config.setUsername(userName);
        config.setPassword(password);

        if (poolName != null) {
            config.setPoolName(poolName);
        }
        if (maxPoolSize != null) {
            config.setMaximumPoolSize(maxPoolSize);
        }
        if (leakDetectionThreshold != null) {
            config.setLeakDetectionThreshold(leakDetectionThreshold);
        }
        return config;
    }

    /**
     * Build an Hikari datasource.
     * 
     * @param databaseURL A database URL in Hiroku like format.
     * @param poolName Name assigned to the connection pool.
     * @param maxPoolSize Maximum pool size. If null use Hikari default value.
     * @param leakDetectionThreshold This property controls the amount of time that a connection can be out of the pool before a message is
    * logged indicating a possible connection leak. can be {@code null}.
    * 
     * @return An Hikari datasource.
     */
    public static DataSource createDataSource(String databaseURL, String poolName, Integer maxPoolSize, Long leakDetectionThreshold) {
        HikariConfig config = createHikariConfig(databaseURL, poolName, maxPoolSize, leakDetectionThreshold);
        return new HikariDataSource(config);
    }
}
