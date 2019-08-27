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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConfigurationRuntimeException;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.apache.sis.util.logging.Logging;

/**
 * @author Quentin Boileau (Geomatys)
 */
public class TestingDBConfiguration implements IDatabaseConfiguration {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.database.configuration");
    private static final String DEFAULT_TEST_DATABASE_URL = "postgres://cstl:admin@localhost:5432/cstl-test";

    private DataSource testDatasource;
    private Settings testSettings;
    private SQLDialect testDialect;

    @PostConstruct
    public void init() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }

        String testDBURL = Application.getProperty(AppProperty.TEST_DATABASE_URL);
        if (testDBURL == null) {
            testDBURL = DEFAULT_TEST_DATABASE_URL;
        }

        HikariConfig config = DatabaseConfigurationUtils.createHikariConfig(testDBURL, "testing", 5);
        try {
            testDatasource = new HikariDataSource(config);
            if (testDBURL.contains("postgres")) {
                testSettings = new Settings().withRenderNameStyle(RenderNameStyle.AS_IS);
                testDialect = SQLDialect.POSTGRES;
            } else {
                testSettings = new Settings().withRenderNameStyle(RenderNameStyle.QUOTED);
                testDialect = SQLDialect.HSQLDB;
            }
        } catch (Exception e) {
            throw new ConfigurationRuntimeException("No testing database found with matching URL : "+testDBURL, e);
        }

    }

    @Override
    public Settings getJOOQSettings() {
        return testSettings;
    }

    @Override
    public SQLDialect getDialect() {
        return testDialect;
    }

    @Override
    public DataSource createCstlDatasource() {
        return testDatasource;
    }

    @Override
    public DataSource createEPSGDatasource() {
        return testDatasource;
    }
}
