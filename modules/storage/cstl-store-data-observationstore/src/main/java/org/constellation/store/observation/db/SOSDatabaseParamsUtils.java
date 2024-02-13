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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.sis.parameter.Parameters;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.store.observation.db.model.OMSQLDialect;
import org.constellation.util.SQLUtilities;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSDatabaseParamsUtils {
    
    private static final Logger LOGGER = Logger.getLogger("org.constellation.store.observation.db");
    
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
    
    public static DataSource extractOM2Datasource(final Parameters params) throws IOException {
        // driver
            final String driver = SOSDatabaseParamsUtils.getDriverClassName(params);

            // jdbc url
            final String jdbcUrl = SOSDatabaseParamsUtils.getJDBCUrl(params);

            // hiroku form url
            final String hirokuUrl = SOSDatabaseParamsUtils.getHirokuUrl(params);

            // username
            final String user = params.getValue(SOSDatabaseObservationStoreFactory.USER);

            // password
            final String passwd = params.getValue(SOSDatabaseObservationStoreFactory.PASSWD);
            
            OMSQLDialect dialect = OMSQLDialect.valueOf((params.getValue(SOSDatabaseObservationStoreFactory.SGBDTYPE)).toUpperCase());
             
            // examind special for sharing datasource (disabled for derby)
            DataSource candidate = null;
            if (hirokuUrl != null) {
                try {
                    IDatasourceBusiness dsBusiness = SpringHelper.getBean(IDatasourceBusiness.class).orElse(null);
                    candidate = dsBusiness.getSQLDatasource(hirokuUrl, user, passwd).orElse(null);
                    if (candidate == null) {
                        LOGGER.info("No existing examind datasource found.");
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Unable to get an existing examind datasource.", ex);
                }
            }
            // fall back on direct datasource instanciation.
            if (candidate == null) {
                Boolean readOnly = params.getValue(SOSDatabaseObservationStoreFactory.DATABASE_READONLY);
                Properties ro_prop = null;
                if (dialect.equals(OMSQLDialect.DUCKDB) && Boolean.TRUE.equals(readOnly)) {
                    ro_prop = new Properties();
                    ro_prop.setProperty("duckdb.read_only", "true");   
                }     
                candidate = SQLUtilities.getDataSource(jdbcUrl, driver, user, passwd, ro_prop, readOnly);
            }
            return candidate;
    }
}
