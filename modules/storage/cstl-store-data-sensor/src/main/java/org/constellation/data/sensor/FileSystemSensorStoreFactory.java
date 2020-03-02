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

import java.nio.file.Path;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
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
 *  @author Guilhem Legal (Geomatys)
 */
@StoreMetadata(
        formatName = FileSystemSensorStoreFactory.NAME,
        capabilities = {Capability.READ, Capability.WRITE},
        resourceTypes = {})
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class FileSystemSensorStoreFactory extends DataStoreProvider {

     /** factory identification **/
    public static final String NAME = "filesensor";

    public static final ParameterDescriptor<String> IDENTIFIER  = new ParameterBuilder()
                    .addName("identifier")
                    .addName(Bundle.formatInternational(Bundle.Keys.paramIdentifierAlias))
                    .setRemarks(Bundle.formatInternational(Bundle.Keys.paramIdentifierRemarks))
                    .setRequired(true)
                    .createEnumerated(String.class, new String[]{NAME}, NAME);

    public static final ParameterDescriptor<Path> DATA_DIRECTORY_DESCRIPTOR = new ParameterBuilder().addName("data_directory")
            .setRemarks("Directory where are stored the sensorML files").setRequired(true).create(Path.class, null);


    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            new ParameterBuilder().addName(NAME).addName("FileSensorParameters").createGroup(IDENTIFIER,DATA_DIRECTORY_DESCRIPTOR);

    @Override
    public String getShortName() {
        return NAME;
    }

    public CharSequence getDescription() {
        return "File system sensor store";
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public FileSystemSensorStore open(ParameterValueGroup params) throws DataStoreException {
        return new FileSystemSensorStore(params);
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
