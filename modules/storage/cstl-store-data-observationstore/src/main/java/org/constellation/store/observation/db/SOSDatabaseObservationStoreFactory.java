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

import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.createFixedIdentifier;
import org.apache.sis.parameter.ParameterBuilder;
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
    public static final ParameterDescriptor<Integer> PORT = BUILDER.addName("port").setRemarks("Port").setRequired(false).create(Integer.class, 5432);

    /**
     * Parameter identifying the OM datastore
     */
    public static final ParameterDescriptor<String> DBTYPE =
             BUILDER.addName("dbtype").setRemarks("DbType").setRequired(true).create(String.class, "OM");

    /**
     * Parameter for database type (postgres, derby, ...)
     */
    public static final ParameterDescriptor<String> SGBDTYPE =
             BUILDER.addName("sgbdtype").setRequired(false).createEnumerated(String.class, new String[]{"derby","postgres"}, "derby");

    /**
     * Parameter for database url for derby database
     */
    public static final ParameterDescriptor<String> DERBYURL =
             BUILDER.addName("derbyurl").setRemarks("DerbyURL").setRequired(false).create(String.class, null);

    /**
     * Parameter for database host
     */
    public static final ParameterDescriptor<String> HOST =
             BUILDER.addName("host").setRemarks("Host").setRequired(false).create(String.class, "localhost");

    /**
     * Parameter for database name
     */
    public static final ParameterDescriptor<String> DATABASE =
             BUILDER.addName("database").setRemarks("Database").setRequired(false).create(String.class, null);

    /**
     * Parameter for database user name
     */
    public static final ParameterDescriptor<String> USER =
             BUILDER.addName("user").setRemarks("User").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> PHENOMENON_ID_BASE =
             BUILDER.addName("phenomenon-id-base").setRemarks("phenomenon-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_TEMPLATE_ID_BASE =
             BUILDER.addName("observation-template-id-base").setRemarks("observation-template-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_ID_BASE =
             BUILDER.addName("observation-id-base").setRemarks("observation-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> SENSOR_ID_BASE =
             BUILDER.addName("sensor-id-base").setRemarks("sensor-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> SCHEMA_PREFIX =
             BUILDER.addName("schema-prefix").setRemarks("schema-prefix").setRequired(false).create(String.class, null);
    
    public static final ParameterDescriptor<Boolean> TIMESCALEDB =
             BUILDER.addName("timescaledb").setRemarks("timescale db").setRequired(false).create(Boolean.class, false);

    /**
     * Parameter for database user password
     */
    public static final ParameterDescriptor<String> PASSWD =
             BUILDER.addName("password").setRemarks("Password").setRequired(false).create(String.class, null);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = BUILDER.addName(NAME).addName("SOSDBParameters").setRequired(true)
            .createGroup(IDENTIFIER,DBTYPE,HOST,PORT,DATABASE,USER,PASSWD,NAMESPACE, SGBDTYPE, DERBYURL, PHENOMENON_ID_BASE, OBSERVATION_TEMPLATE_ID_BASE, OBSERVATION_ID_BASE, SENSOR_ID_BASE, SCHEMA_PREFIX, TIMESCALEDB);

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
        return new SOSDatabaseObservationStore(params);
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
