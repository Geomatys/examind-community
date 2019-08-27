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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConfigurationRuntimeException;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class HSQLDatabaseConfiguration implements IDatabaseConfiguration {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.database.configuration");

    @Override
    public Settings getJOOQSettings() {
        return new Settings().withRenderNameStyle(RenderNameStyle.QUOTED);
    }

    @Override
    public SQLDialect getDialect() {
        return SQLDialect.HSQLDB;
    }

    @Override
    public DataSource createCstlDatasource() {
        // Force loading driver because some containers like tomcat 7.0.21+ disable drivers at startup.
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }

        final String databaseURL = Application.getProperty(AppProperty.CSTL_DATABASE_URL);
        if (databaseURL == null) {
            throw new ConfigurationRuntimeException("Property \""+AppProperty.CSTL_DATABASE_URL.name()+"\" not defined.");
        }
        final Integer maxPoolSize = Application.getIntegerProperty(AppProperty.CSTL_DATABASE_MAX_POOL_SIZE);
        final HikariConfig config = DatabaseConfigurationUtils.createHikariConfig(databaseURL, "constellation", maxPoolSize);
        return new HikariDataSource(config);
    }

    @Override
    public DataSource createEPSGDatasource() {
        // Force loading driver because some containers like tomcat 7.0.21+ disable drivers at startup.
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }

        final String databaseURL = Application.getProperty(AppProperty.EPSG_DATABASE_URL);
        if (databaseURL == null) {
            throw new ConfigurationRuntimeException("Property \""+AppProperty.EPSG_DATABASE_URL.name()+"\" not defined.");
        }

        final int maxPoolSize = Application.getIntegerProperty(AppProperty.EPSG_DATABASE_MAX_POOL_SIZE, 5);
        final HikariConfig config = DatabaseConfigurationUtils.createHikariConfig(databaseURL, "epsg", maxPoolSize);
        return new HikariDataSource(config);
    }

}
