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

import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.Resource;
import org.geotoolkit.sensor.AbstractSensorStore;
import org.constellation.sos.io.internal.InternalSensorReader;
import org.constellation.sos.io.internal.InternalSensorWriter;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.storage.DataStores;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class InternalSensorStore extends AbstractSensorStore implements Resource {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.store");

    public InternalSensorStore(ParameterValueGroup source) {
        super(source);
        try {
            this.reader = new InternalSensorReader(new HashMap<>());
            this.writer = new InternalSensorWriter(new HashMap<>());
        } catch (DataStoreException ex) {
            LOGGER.log(Level.WARNING, "Unable to initalize the internal sensor reader", ex);
        }
    }

    @Override
    public DataStoreProvider getProvider() {
        return DataStores.getProviderById(InternalSensorStoreFactory.NAME);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return OMUtils.buildMetadata(InternalSensorStoreFactory.NAME);
    }

    @Override
    public Optional<GenericName> getIdentifier() {
        return Optional.empty();
    }
}
