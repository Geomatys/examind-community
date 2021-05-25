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

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.sis.util.UnconvertibleObjectException;
import org.geotoolkit.feature.util.converter.SimpleConverter;
import org.geotoolkit.internal.sql.DefaultDataSource;

/**
 *
 * @author guilhem
 */
public class DataSourceToStringConverter extends SimpleConverter<DataSource, String> {

    @Override
    public Class<DataSource> getSourceClass() {
        return DataSource.class;
    }

    @Override
    public Class<String> getTargetClass() {
        return String.class;
    }

    @Override
    public String apply(DataSource s) throws UnconvertibleObjectException {
        if (s == null) return null;
        if (s instanceof DefaultDataSource) {
            DefaultDataSource ds = (DefaultDataSource) s;
            return ds.url;
        } else if (s instanceof HikariDataSource) {
            final HikariDataSource ds = (HikariDataSource) s;
            String url = ds.getJdbcUrl();
            if (url == null) {
                // TODO: Try forging back jdbc url ? (Note: hide login/pw for security purpose)
                throw new UnconvertibleObjectException("Cannot extract JDBC URL from input datasource");
            }
        }
        throw new UnconvertibleObjectException("Unknown datasource type. For now, only Geotk/Hikari datasources are supported");
    }
}
