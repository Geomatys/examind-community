/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2015, Geomatys
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

import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.createFixedIdentifier;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@StoreMetadata(
        formatName = SOSDatabaseObservationStoreFactory.NAME,
        capabilities = {Capability.READ, Capability.CREATE, Capability.WRITE},
        resourceTypes = {})
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class SOSDatabaseObservationStoreFactory extends AbstractObservationStoreFactory {

    /** factory identification **/
    public static final String NAME = "observationSOSDatabase";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    /**
     * Parameter for database port
     */
    public static final String PORT_NAME = "port";
    public static final ParameterDescriptor<Integer> PORT = BUILDER.addName(PORT_NAME).setRemarks("Port").setRequired(false).create(Integer.class, 5432);

    /**
     * Parameter for database type (postgres, derby, ...)
     */
    public static final String SGBDTYPE_NAME = "sgbdtype";
    public static final ParameterDescriptor<String> SGBDTYPE =
             BUILDER.addName(SGBDTYPE_NAME).setRequired(false).createEnumerated(String.class, new String[]{"derby","duckdb","postgres"}, "derby");

    /**
     * Parameter for database url for derby database
     */
    public static final String DERBY_URL_NAME = "derbyurl";
    public static final ParameterDescriptor<String> DERBY_URL =
             BUILDER.addName(DERBY_URL_NAME).setRemarks("DerbyURL").setRequired(false).create(String.class, null);

    /**
     * Parameter for database host
     */
    public static final String HOST_NAME = "host";
    public static final ParameterDescriptor<String> HOST =
             BUILDER.addName(HOST_NAME).setRemarks("Host").setRequired(false).create(String.class, "localhost");

    /**
     * Parameter for database name
     */
    public static final String DATABASE_NAME = "database";
    public static final ParameterDescriptor<String> DATABASE =
             BUILDER.addName(DATABASE_NAME).setRemarks("Database").setRequired(false).create(String.class, null);

    /**
     * Parameter for database user name
     */
    public static final String USER_NAME = "user";
    public static final ParameterDescriptor<String> USER =
             BUILDER.addName(USER_NAME).setRemarks("User").setRequired(false).create(String.class, null);
    
    public static final String DATABASE_READONLY_NAME = "database-readonly";
    public static final ParameterDescriptor<Boolean> DATABASE_READONLY =
             BUILDER.addName(DATABASE_READONLY_NAME).setRemarks("database readonly").setRequired(false).create(Boolean.class, null);

    public static final String SCHEMA_PREFIX_NAME = "schema-prefix";
    public static final ParameterDescriptor<String> SCHEMA_PREFIX =
             BUILDER.addName(SCHEMA_PREFIX_NAME).setRemarks(SCHEMA_PREFIX_NAME).setRequired(false).create(String.class, null);
    
    public static final String DECIMATION_ALGORITHM_NAME = "decimation-algorithm";
    public static final ParameterDescriptor<String> DECIMATION_ALGORITHM =
             BUILDER.addName(DECIMATION_ALGORITHM_NAME).setRemarks(DECIMATION_ALGORITHM_NAME).setRequired(false).create(String.class, "");
    
    public static final String TIMESCALEDB_NAME = "timescaledb";
    public static final ParameterDescriptor<Boolean> TIMESCALEDB =
             BUILDER.addName(TIMESCALEDB_NAME).setRemarks("timescale db").setRequired(false).create(Boolean.class, false);
    
    /**
     * Max field by table, Optional.
     * Maximum number of field by measure table.
     * If the number of fields exceed this value, another measure table will be created.
     */
    public static final String MAX_FIELD_BY_TABLE_NAME = "max-field-by-table";
    public static final ParameterDescriptor<Integer> MAX_FIELD_BY_TABLE = BUILDER
            .addName(MAX_FIELD_BY_TABLE_NAME)
            .setRemarks(MAX_FIELD_BY_TABLE_NAME)
            .setRequired(false)
            .create(Integer.class, 1000);
    
    /**
     * Parameter for database user password
     */
    public static final String PASSWD_NAME = "password";
    public static final ParameterDescriptor<String> PASSWD =
             BUILDER.addName(PASSWD_NAME).setRemarks("Password").setRequired(false).create(String.class, null);
    
    public static final String MODE_NAME = "mode";
    public static final ParameterDescriptor<String> MODE =
             BUILDER.addName(MODE_NAME).setRemarks(MODE_NAME).setRequired(false).create(String.class, "default");

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = BUILDER.addName(NAME).addName("SOSDBParameters").setRequired(true)
            .createGroup(IDENTIFIER,HOST,PORT,DATABASE,USER,PASSWD,NAMESPACE, SGBDTYPE, DERBY_URL, PHENOMENON_ID_BASE, OBSERVATION_TEMPLATE_ID_BASE,
                         OBSERVATION_ID_BASE, SENSOR_ID_BASE, SCHEMA_PREFIX, TIMESCALEDB, MAX_FIELD_BY_TABLE, DATABASE_READONLY, MODE);

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public SOSDatabaseObservationStore open(ParameterValueGroup params) throws DataStoreException {
        return new SOSDatabaseObservationStore(Parameters.castOrWrap(params));
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
