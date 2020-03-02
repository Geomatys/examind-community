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
package org.constellation.store.metadata.filesystem;

import java.nio.file.Path;
import java.util.Map;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import static org.constellation.store.metadata.CstlMetadataStoreDescriptors.EXTRA_QUERYABLE;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.storage.Bundle;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@StoreMetadata(
        formatName = FileSystemMetadataStoreFactory.NAME,
        capabilities = {Capability.READ, Capability.WRITE},
        resourceTypes = {})
@StoreMetadataExt(resourceTypes = ResourceType.METADATA)
public class FileSystemMetadataStoreFactory extends DataStoreProvider {

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

     /** factory identification **/
    public static final String NAME = "FilesystemMetadata";

    public static final ParameterDescriptor<String> IDENTIFIER = new ParameterBuilder()
                    .addName("identifier")
                    .addName(Bundle.formatInternational(Bundle.Keys.paramIdentifierAlias))
                    .setRemarks(Bundle.formatInternational(Bundle.Keys.paramIdentifierRemarks))
                    .setRequired(true)
                    .createEnumerated(String.class, new String[]{NAME}, NAME);

    public static final ParameterDescriptor<Map> CONFIG_PARAMS = BUILDER
            .addName("config-params")
            .setRemarks("Configuration parameters")
            .setRequired(false)
            .create(Map.class, null);

    public static final ParameterDescriptor<Path> FOLDER = BUILDER
            .addName("folder")
            .setRemarks("Folder containing metadata files")
            .setRequired(true)
            .create(Path.class, null);

    public static final ParameterDescriptor<String> STORE_ID = BUILDER
            .addName("store-id")
            .setRemarks("Store unique identifier")
            .setRequired(true)
            .create(String.class, null);


    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR =
            BUILDER.addName(NAME).addName("FSMetadataParameters").createGroup(IDENTIFIER, FOLDER, STORE_ID, CONFIG_PARAMS, EXTRA_QUERYABLE);

    @Override
    public String getShortName() {
        return NAME;
    }

    public CharSequence getDescription() {
        return "Constellation filesystem metadata store";
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public MetadataStore open(ParameterValueGroup params) throws DataStoreException {
        //ensureCanProcess(params);
        try {
            return new FileSystemMetadataStore(params);
        } catch (MetadataIoException ex) {
            throw new DataStoreException(ex);
        }
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector connector) throws DataStoreException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
