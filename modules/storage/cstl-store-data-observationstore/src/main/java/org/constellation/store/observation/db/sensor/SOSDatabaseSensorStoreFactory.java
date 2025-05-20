/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
package org.constellation.store.observation.db.sensor;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.DATABASE;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.DATABASE_READONLY;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.DERBY_URL;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.HOST;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.MAX_FIELD_BY_TABLE;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.MODE;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.PASSWD;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.PORT;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.SCHEMA_PREFIX;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.SGBDTYPE;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.TIMESCALEDB;
import static org.constellation.store.observation.db.SOSDatabaseObservationStoreFactory.USER;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.NAMESPACE;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_ID_BASE;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_TEMPLATE_ID_BASE;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.PHENOMENON_ID_BASE;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.SENSOR_ID_BASE;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.createFixedIdentifier;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
@StoreMetadata(
        formatName = SOSDatabaseSensorStoreFactory.NAME,
        capabilities = {Capability.READ, Capability.WRITE},
        resourceTypes = {})
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class SOSDatabaseSensorStoreFactory extends DataStoreProvider {

     /** factory identification **/
    public static final String NAME = "om2sensor";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new ParameterBuilder().addName(NAME)
                                  .addName("OM2SensorParameters")
                                  .createGroup(IDENTIFIER,HOST,PORT,DATABASE,USER,PASSWD,NAMESPACE, SGBDTYPE, DERBY_URL, PHENOMENON_ID_BASE, OBSERVATION_TEMPLATE_ID_BASE,
                         OBSERVATION_ID_BASE, SENSOR_ID_BASE, SCHEMA_PREFIX, TIMESCALEDB, MAX_FIELD_BY_TABLE, DATABASE_READONLY, MODE);

    @Override
    public String getShortName() {
        return NAME;
    }

    public CharSequence getDescription() {
        return "OM2 database sensor store";
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public SOSDatabaseSensorStore open(ParameterValueGroup params) throws DataStoreException {
        return new SOSDatabaseSensorStore(Parameters.castOrWrap(params));
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("StorageConnector not supported.");
    }
}
