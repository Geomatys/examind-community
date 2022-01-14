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
import javax.sql.DataSource;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SQLUtilities {

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
            config.setDataSourceClassName(className);
        }
        config.setJdbcUrl(connectURL);
        config.setUsername(user);
        config.setPassword(password);
        return new HikariDataSource(config);
    }
}
