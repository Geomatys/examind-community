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

    public static final String DATASOURCE_ID_NAME = "datasource-id";
    public static final ParameterDescriptor<Integer> DATASOURCE_ID =  
            BUILDER.addName(DATASOURCE_ID_NAME).setDescription("Examind datasource identifier").create(Integer.class, null);
    
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
    
    public static final String MODE_NAME = "mode";
    public static final ParameterDescriptor<String> MODE =
             BUILDER.addName(MODE_NAME).setRemarks(MODE_NAME).setRequired(false).create(String.class, "default");

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = BUILDER.addName(NAME).addName("SOSDBParameters").setRequired(true)
            .createGroup(IDENTIFIER, DATASOURCE_ID, NAMESPACE, PHENOMENON_ID_BASE, OBSERVATION_TEMPLATE_ID_BASE,
                         OBSERVATION_ID_BASE, SENSOR_ID_BASE, SCHEMA_PREFIX, TIMESCALEDB, MAX_FIELD_BY_TABLE, MODE);

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
