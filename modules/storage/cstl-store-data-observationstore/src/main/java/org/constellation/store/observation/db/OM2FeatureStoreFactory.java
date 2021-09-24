/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.store.observation.db;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.ResourceInternationalString;
import org.geotoolkit.jdbc.DBCPDataSource;
import org.geotoolkit.jdbc.ManageableDataSource;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.constellation.provider.DataProviders;
import org.geotoolkit.observation.Bundle;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterNotFoundException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
@StoreMetadata(
        formatName = OM2FeatureStoreFactory.NAME,
        capabilities = {Capability.READ},
        resourceTypes = {FeatureSet.class})
@StoreMetadataExt(
        resourceTypes = ResourceType.VECTOR,
        geometryTypes ={Geometry.class,
                        Point.class,
                        LineString.class,
                        Polygon.class,
                        MultiPoint.class,
                        MultiLineString.class,
                        MultiPolygon.class})
public class OM2FeatureStoreFactory extends DataStoreProvider {

    /** factory identification **/
    public static final String NAME = "om2";

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final ParameterDescriptor<String> IDENTIFIER = new ParameterBuilder()
                    .addName("identifier")
                    .addName(Bundle.formatInternational(Bundle.Keys.paramIdentifierAlias))
                    .setRemarks(Bundle.formatInternational(Bundle.Keys.paramIdentifierRemarks))
                    .setRequired(true)
                    .createEnumerated(String.class, new String[]{NAME}, NAME);

    /**
     * Parameter for database port
     */
    public static final ParameterDescriptor<Integer> PORT = BUILDER
            .addName("port")
            .setRemarks("Port")
            .setRequired(false)
            .create(Integer.class, 5432);

    /**
     * Parameter identifying the OM datastore
     */
    public static final ParameterDescriptor<String> DBTYPE = BUILDER
            .addName("dbtype")
            .setRemarks("DbType")
            .setRequired(true)
            .create(String.class, "OM2");

    /**
     * Parameter for database type (postgres, derby, ...)
     */
    public static final ParameterDescriptor<String> SGBDTYPE = BUILDER
            .addName("sgbdtype")
            .setRemarks("sgbdtype")
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"derby", "postgres"}, "derby");

    /**
     * Parameter for database url for derby database
     */
    public static final ParameterDescriptor<String> DERBYURL = BUILDER
            .addName("derbyurl")
            .setRemarks("DerbyURL")
            .setRequired(false)
            .create(String.class, null);

    /**
     * Parameter for database host
     */
    public static final ParameterDescriptor<String> HOST = BUILDER
            .addName("host")
            .setRemarks("Host")
            .setRequired(false)
            .create(String.class, "localhost");

    /**
     * Parameter for database name
     */
    public static final ParameterDescriptor<String> DATABASE = BUILDER
            .addName("database")
            .setRemarks("Database")
            .setRequired(false)
            .create(String.class, null);

    /**
     * Parameter for database user name
     */
    public static final ParameterDescriptor<String> USER = BUILDER
            .addName("user")
            .setRemarks("User")
            .setRequired(false)
            .create(String.class, null);

    /**
     * Parameter for database user password
     */
    public static final ParameterDescriptor<String> PASSWD = BUILDER
            .addName("password")
            .setRemarks("Password")
            .setRequired(false)
            .create(String.class, null);

     public static final ParameterDescriptor<String> SCHEMA_PREFIX =
             BUILDER.addName("schema-prefix").setRemarks("schema-prefix").setRequired(false).create(String.class, null);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = BUILDER.addName(NAME).addName("OM2Parameters").setRequired(true)
            .createGroup(IDENTIFIER, DBTYPE, HOST, PORT, DATABASE, USER, PASSWD, SGBDTYPE, DERBYURL, SCHEMA_PREFIX);

    @Override
    public String getShortName() {
        return NAME;
    }

    /**
     * {@inheritDoc }
     */
    public CharSequence getDescription() {
        return new ResourceInternationalString("org/constellation/data/om2/bundle", "datastoreDescription");
    }

    public CharSequence getDisplayName() {
        return new ResourceInternationalString("org/constellation/data/om2/bundle", "datastoreTitle");
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    public boolean canProcess(final ParameterValueGroup params) {
        try {
            boolean valid = true; //super.canProcess(params);
            if (valid) {
                Object value = params.parameter(DBTYPE.getName().toString()).getValue();
                if ("OM2".equals(value)) {
                    Object sgbdtype = params.parameter(SGBDTYPE.getName().getCode()).getValue();

                    if ("derby".equals(sgbdtype)) {
                        //check the url is set
                        Object derbyurl = params.parameter(DERBYURL.getName().getCode()).getValue();
                        return derbyurl != null;
                    }
                }
            }
        } catch (ParameterNotFoundException e) {
            DataProviders.LOGGER.log(Level.FINE, "Invalid parameters for OM2 feature store.", e);
        }
        return false;
    }

    @Override
    public OM2FeatureStore open(final ParameterValueGroup params) throws DataStoreException {
        if (!canProcess(params)) {
            throw new DataStoreException("Parameter values not supported by this factory.");
        }
        try{
            //create a datasource
            final BasicDataSource dataSource = new BasicDataSource();

            // some default data source behaviour
            dataSource.setPoolPreparedStatements(true);

            // driver
            dataSource.setDriverClassName(getDriverClassName(params));

            // url
            dataSource.setUrl(getJDBCUrl(params));

            // username
            final String user = (String) params.parameter(USER.getName().toString()).getValue();
            dataSource.setUsername(user);

            // password
            final String passwd = (String) params.parameter(PASSWD.getName().toString()).getValue();
            if (passwd != null) {
                dataSource.setPassword(passwd);
            }

            // some datastores might need this
            dataSource.setAccessToUnderlyingConnectionAllowed(true);

            final ManageableDataSource source = new DBCPDataSource(dataSource);
            return new OM2FeatureStore(params,source);
        } catch (IOException ex) {
            throw new DataStoreException(ex);
        }
    }

    private String getDriverClassName(final ParameterValueGroup params){
        final String type  = (String) params.parameter(SGBDTYPE.getName().toString()).getValue();
        if (type.equals("derby")) {
            return "org.apache.derby.jdbc.EmbeddedDriver";
        } else {
            return "org.postgresql.Driver";
        }
    }

    private String getJDBCUrl(final ParameterValueGroup params) throws IOException {
        final String type  = (String) params.parameter(SGBDTYPE.getName().toString()).getValue();
        if (type.equals("derby")) {
            final String derbyURL = (String) params.parameter(DERBYURL.getName().toString()).getValue();
            return derbyURL;
        } else {
            final String host  = (String) params.parameter(HOST.getName().toString()).getValue();
            final Integer port = (Integer) params.parameter(PORT.getName().toString()).getValue();
            final String db    = (String) params.parameter(DATABASE.getName().toString()).getValue();
            return "jdbc:postgresql" + "://" + host + ":" + port + "/" + db;
        }
    }

    @Override
    public ProbeResult probeContent(StorageConnector sc) throws DataStoreException {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        throw new DataStoreException("StorageConnector not supported.");
    }
}
