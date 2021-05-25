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
package org.constellation.util.converter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.sis.util.UnconvertibleObjectException;
import org.geotoolkit.feature.util.converter.SimpleConverter;

/**
 *
 * @author guilhem
 */
public class StringToDataSourceConverter extends SimpleConverter<String, DataSource> {

    @Override
    public Class<String> getSourceClass() {
        return String.class;
    }

    @Override
    public Class<DataSource> getTargetClass() {
        return DataSource.class;
    }

    @Override
    public DataSource apply(String s) throws UnconvertibleObjectException {
        if (s == null || (s = s.trim()).isEmpty()) return null;
        // TODO: how to properly validate input ? Should we open a connection ? Problem: it is very costly.
        if (!s.startsWith("jdbc")) {
            throw new UnconvertibleObjectException("Only standard JDBC url are accepted (Ex: jdbc:hsqldb:mem:myDb or jdbc:postgresql://localhost:5432/db)");
        }
        final HikariConfig dbConf = new HikariConfig();
        dbConf.setJdbcUrl(s);
        // TODO: search input URI for datasource advanced configuration ?
        // TODO: Use a datasource registry/cache ?
        return new HikariDataSource(dbConf);
    }

}
