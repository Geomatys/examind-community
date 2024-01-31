/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.store.observation.db;

import java.io.IOException;
import org.apache.sis.parameter.Parameters;

/**
 *
 * @author Guilhem Legal()
 */
public class SOSDatabaseParamsUtils {
    
    public static String getDriverClassName(final Parameters params){
        final String type  = params.getValue(SOSDatabaseObservationStoreFactory.SGBDTYPE);
        return switch (type) {
            case "derby"    ->  "org.apache.derby.jdbc.EmbeddedDriver";
            case "duckdb"   ->  "org.duckdb.DuckDBDriver";
            case "postgres" ->  "org.postgresql.Driver";
            default -> throw new IllegalArgumentException("Unexpected sgbd type:" + type);
        };
    }

    public static String getJDBCUrl(final Parameters params) throws IOException {
        final String type  = params.getMandatoryValue(SOSDatabaseObservationStoreFactory.SGBDTYPE);
        if (type.equals("derby") || type.equals("duckdb")) {
            final String derbyURL = params.getValue(SOSDatabaseObservationStoreFactory.DERBY_URL);
            return derbyURL;
        } else {
            final String host  = params.getValue(SOSDatabaseObservationStoreFactory.HOST);
            final Integer port = params.getValue(SOSDatabaseObservationStoreFactory.PORT);
            final String db    = params.getValue(SOSDatabaseObservationStoreFactory.DATABASE);
            return "jdbc:postgresql" + "://" + host + ":" + port + "/" + db;
        }
    }

    public static String getHirokuUrl(final Parameters params) throws IOException {
        final String type = params.getMandatoryValue(SOSDatabaseObservationStoreFactory.SGBDTYPE);
        if (type.equals("derby") || type.equals("duckdb")) {
            // i don't know if its possible to build an hiroku url for derby
            return null;
        } else {
            final String host  = params.getValue(SOSDatabaseObservationStoreFactory.HOST);
            final Integer port = params.getValue(SOSDatabaseObservationStoreFactory.PORT);
            final String db    = params.getValue(SOSDatabaseObservationStoreFactory.DATABASE);
            return "postgres" + "://" + host + ":" + port + "/" + db;
        }
    }
}
