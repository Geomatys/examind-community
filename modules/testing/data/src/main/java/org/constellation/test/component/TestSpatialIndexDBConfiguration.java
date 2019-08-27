/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.test.component;

import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.geotoolkit.index.tree.manager.postgres.PGDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author guilhem
 */
@Configuration
public class TestSpatialIndexDBConfiguration {

    @Autowired
    @Qualifier(value = "dataSource")
    private DataSource datasource;

    @PostConstruct
    public void init() {
        boolean isPostgres = true;
        if (datasource instanceof HikariDataSource) {
            isPostgres = ((HikariDataSource)datasource).getJdbcUrl().contains("postgres");
        }
        PGDataSource.setDataSource(datasource, isPostgres);
    }
}
