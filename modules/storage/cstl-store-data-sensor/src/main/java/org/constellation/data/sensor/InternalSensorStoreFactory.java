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
 * @author Guilhem Legal (Geomatys)
 */
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR,canWrite = true)
public class InternalSensorStoreFactory extends DataStoreFactory {

     /** factory identification **/
    public static final String NAME = "cstlsensor";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);


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
        ensureCanProcess(params);
        return new InternalSensorStore(params);
    }

    @Override
    public InternalSensorStore create(ParameterValueGroup params) throws DataStoreException {
        throw new DataStoreException("Not supported.");
    }
}
