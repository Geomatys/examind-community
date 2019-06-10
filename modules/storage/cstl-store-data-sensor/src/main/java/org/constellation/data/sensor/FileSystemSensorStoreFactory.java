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
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 *  @author Guilhem Legal (Geomatys)
 */
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR,canWrite = true)
public class FileSystemSensorStoreFactory extends DataStoreFactory {

     /** factory identification **/
    public static final String NAME = "filesensor";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

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
        ensureCanProcess(params);
        return new FileSystemSensorStore(params);
    }

}
