/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
package org.constellation.data.sensor;

import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.observation.Bundle;
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
        formatName = InternalSensorStoreFactory.NAME,
        capabilities = {Capability.READ, Capability.WRITE},
        resourceTypes = {})
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class InternalSensorStoreFactory extends DataStoreProvider {

     /** factory identification **/
    public static final String NAME = "cstlsensor";

    public static final ParameterDescriptor<String> IDENTIFIER = new ParameterBuilder()
                    .addName("identifier")
                    .addName(Bundle.formatInternational(Bundle.Keys.paramIdentifierAlias))
                    .setRemarks(Bundle.formatInternational(Bundle.Keys.paramIdentifierRemarks))
                    .setRequired(true)
                    .createEnumerated(String.class, new String[]{NAME}, NAME);


    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new ParameterBuilder().addName(NAME).addName("CstlSensorParameters").createGroup(IDENTIFIER);

    @Override
    public String getShortName() {
        return NAME;
    }

    public CharSequence getDescription() {
        return "Constellation internal sensor store";
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public InternalSensorStore open(ParameterValueGroup params) throws DataStoreException {
        return new InternalSensorStore(params);
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
