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

import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal()
 */
public class SOSDatabaseParamsUtils {
    
    public static String getDriverClassName(final ParameterValueGroup params){
        final String type  = (String) params.parameter(SOSDatabaseObservationStoreFactory.SGBDTYPE.getName().toString()).getValue();
        if (type.equals("derby")) {
            return "org.apache.derby.jdbc.EmbeddedDriver";
        } else {
            return "org.postgresql.Driver";
        }
    }

    public static String getJDBCUrl(final ParameterValueGroup params) throws IOException {
        final String type  = (String) params.parameter(SOSDatabaseObservationStoreFactory.SGBDTYPE.getName().toString()).getValue();
        if (type.equals("derby")) {
            final String derbyURL = (String) params.parameter(SOSDatabaseObservationStoreFactory.DERBYURL.getName().toString()).getValue();
            return derbyURL;
        } else {
            final String host  = (String) params.parameter(SOSDatabaseObservationStoreFactory.HOST.getName().toString()).getValue();
            final Integer port = (Integer) params.parameter(SOSDatabaseObservationStoreFactory.PORT.getName().toString()).getValue();
            final String db    = (String) params.parameter(SOSDatabaseObservationStoreFactory.DATABASE.getName().toString()).getValue();
            return "jdbc:postgresql" + "://" + host + ":" + port + "/" + db;
        }
    }
}
