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
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.metadata.ModifiableMetadata;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.geotoolkit.sensor.AbstractSensorStore;
import org.constellation.sos.io.filesystem.FileSensorReader;
import org.constellation.sos.io.filesystem.FileSensorWriter;
import org.opengis.parameter.ParameterValueGroup;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemSensorStore extends AbstractSensorStore implements Resource {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store");

    public FileSystemSensorStore(ParameterValueGroup source) {
        super(source);
        try {
            final Path dataDirectory = (Path) source.parameter(FileSystemSensorStoreFactory.DATA_DIRECTORY_DESCRIPTOR.getName().getCode()).getValue();
            this.reader = new FileSensorReader(dataDirectory, new HashMap<>());
            this.writer = new FileSensorWriter(dataDirectory, new HashMap<>());
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, "Unable to initalize the filesystem sensor reader", ex);
        }
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(FileSystemSensorStoreFactory.NAME);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        final String name = "file-observation";
        final DefaultMetadata metadata = new DefaultMetadata();
        final DefaultDataIdentification identification = new DefaultDataIdentification();
        final NamedIdentifier identifier = new NamedIdentifier(new DefaultIdentifier(name));
        final DefaultCitation citation = new DefaultCitation(name);
        citation.setIdentifiers(Collections.singleton(identifier));
        identification.setCitation(citation);
        metadata.setIdentificationInfo(Collections.singleton(identification));
        metadata.transitionTo(ModifiableMetadata.State.FINAL);
        return metadata;
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.empty();
    }
}
