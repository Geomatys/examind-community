/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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
package org.constellation.store.metadata.internal;

import java.util.Map;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import static org.constellation.store.metadata.CstlMetadataStoreDescriptors.EXTRA_QUERYABLE;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.storage.DataStoreFactory;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Johann Sorel (Geomatys)
 */

@StoreMetadataExt(resourceTypes = ResourceType.METADATA, canWrite = true)
public class InternalMetadataStoreFactory extends DataStoreProvider {

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

     /** factory identification **/
    public static final String NAME = "InternalCstlmetadata";

    public static final ParameterDescriptor<String> IDENTIFIER = DataStoreFactory.createFixedIdentifier(NAME);

    public static final ParameterDescriptor<Map> CONFIG_PARAMS = BUILDER
            .addName("config-params")
            .setRemarks("Configuration parameters")
            .setRequired(false)
            .create(Map.class, null);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            BUILDER.addName(NAME).addName("CstlMetadataParameters").createGroup(IDENTIFIER, CONFIG_PARAMS, EXTRA_QUERYABLE);

    @Override
    public String getShortName() {
        return NAME;
    }

    public CharSequence getDescription() {
        return "Constellation internal metadata store";
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public MetadataStore open(ParameterValueGroup params) throws DataStoreException {
        //ensureCanProcess(params);
        try {
            return new InternalMetadataStore(params);
        } catch (MetadataIoException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
