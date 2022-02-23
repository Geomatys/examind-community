/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.util.converter;

import org.geotoolkit.internal.sql.DefaultDataSource;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataSourceToStringConverterTest {

    @Test
    public void convertTest() {
        DataSourceToStringConverter converter = new DataSourceToStringConverter();

        DefaultDataSource geotkDs = new DefaultDataSource("jdbc:derby:memory:test");
        String result = converter.apply(geotkDs);
        Assert.assertEquals("jdbc:derby:memory:test", result);

        /* require a working datasource

        DataSource hikDs =  SQLUtilities.createDataSource("postgres://cstl:cstl@examind-db:5432/exa-db", "examind", 1, -1L);
        result = converter.apply(geotkDs);
        Assert.assertEquals("", result);*/
        
    }
}
