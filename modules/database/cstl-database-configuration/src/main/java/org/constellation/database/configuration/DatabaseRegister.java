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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import org.apache.sis.util.logging.Logging;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConfigurationRuntimeException;
import org.constellation.util.SQLUtilities;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.RenderQuotedNames;

/**
 * Search for Database configuration and register beans like "jooq-setting', "dataSource", "epsgDataSource", "dialect".
 *
 * @author Quentin Boileau (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
@Configuration
public class DatabaseRegister {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.database.configuration");
    private static final String DEFAULT_TEST_DATABASE_URL = "jdbc:hsqldb:mem:admin";

    private DataSource exaDatasource;
    private DataSource epsgDatasource;
    private Settings settings;
    private SQLDialect dialect;

    private boolean test = false;

    @PostConstruct
    public void init() {
        // Force loading driver because some containers like tomcat 7.0.21+ disable drivers at startup.
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }

        final String exaDbUrl = test ?
            Application.getProperty(AppProperty.TEST_DATABASE_URL, DEFAULT_TEST_DATABASE_URL) :
            Application.getProperty(AppProperty.CSTL_DATABASE_URL);
        final String epsgDbUrl = Application.getProperty(AppProperty.EPSG_DATABASE_URL, exaDbUrl);


        if (exaDbUrl == null) {
            throw new ConfigurationRuntimeException("Property \"" + AppProperty.CSTL_DATABASE_URL.name() + "\" not defined.");
        }
        if (epsgDbUrl == null) {
            throw new ConfigurationRuntimeException("Property \"" + AppProperty.EPSG_DATABASE_URL.name() + "\" not defined.");
        }
        Long leakDetectionThreshold = null;
        if (test) {
            leakDetectionThreshold = 10000L;
        }
        try {
            final Integer exaMaxPoolSize = Application.getIntegerProperty(AppProperty.CSTL_DATABASE_MAX_POOL_SIZE);
            exaDatasource = SQLUtilities.createDataSource(exaDbUrl, "examind", exaMaxPoolSize, leakDetectionThreshold);

            final Integer epsgMaxPoolSize = Application.getIntegerProperty(AppProperty.EPSG_DATABASE_MAX_POOL_SIZE);
            epsgDatasource = SQLUtilities.createDataSource(exaDbUrl, "epsg", epsgMaxPoolSize, leakDetectionThreshold);
        
            if (exaDbUrl.contains("postgres")) {
                settings = new Settings().withRenderNameCase(RenderNameCase.AS_IS);
                dialect = SQLDialect.POSTGRES;
            } else {
                settings = new Settings().withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_QUOTED);
                dialect = SQLDialect.HSQLDB;
            }
        } catch (Exception e) {
            throw new ConfigurationRuntimeException("Error while initializing the examind datasources", e);
        }
    }

    @Bean(name = "jooq-setting")
    public Settings getJOOQSettings() {
        return settings;
    }

    @Bean(name = "dialect")
    public SQLDialect getDialect() {
        return dialect;
    }

    @Bean(name = "dataSource")
    public DataSource createDatasource() {
        return exaDatasource;
    }

    @Bean(name = "epsgDataSource")
    public DataSource createEPSGDatasource() {
        return epsgDatasource;
    }

    public boolean getTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }
}
