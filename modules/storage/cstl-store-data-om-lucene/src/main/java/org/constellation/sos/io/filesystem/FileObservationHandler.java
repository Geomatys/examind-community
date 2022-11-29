/*
 *    Examind - An open source and standard compliant SDI
 *    https://www.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.sos.io.filesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.xml.MarshallerPool;
import org.geotoolkit.observation.json.ObservationJsonUtils;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.OBSERVATION_ID_BASE_NAME;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.PHENOMENON_ID_BASE_NAME;
import org.geotoolkit.sos.xml.SOSMarshallerPool;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public abstract class FileObservationHandler {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.sos.io.filesystem");

    protected ObjectMapper mapper;

    protected static final String FILE_EXTENSION_XML = "xml";
    protected static final String FILE_EXTENSION_JS = "json";

    protected final String observationIdBase;

    protected final String phenomenonIdBase;

    protected Path offeringDirectory;

    protected Path phenomenonDirectory;

    protected Path observationDirectory;

    protected Path observationTemplateDirectory;

    protected Path sensorDirectory;

    protected Path foiDirectory;

    protected static final MarshallerPool MARSHALLER_POOL;
    static {
        MARSHALLER_POOL = SOSMarshallerPool.getInstance();
    }

    public FileObservationHandler(final Path dataDirectory, final Map<String, Object> properties) throws DataStoreException {
        this.observationIdBase = (String) properties.get(OBSERVATION_ID_BASE_NAME);
        this.phenomenonIdBase  = (String) properties.get(PHENOMENON_ID_BASE_NAME);
        if (Files.isDirectory(dataDirectory)) {
            offeringDirectory            = dataDirectory.resolve("offerings");
            phenomenonDirectory          = dataDirectory.resolve("phenomenons");
            observationDirectory         = dataDirectory.resolve("observations");
            observationTemplateDirectory = dataDirectory.resolve("observationTemplates");
            sensorDirectory              = dataDirectory.resolve("sensors");
            foiDirectory                 = dataDirectory.resolve("features");
        } else {
            throw new DataStoreException("There is no data Directory");
        }
        if (MARSHALLER_POOL == null) {
            throw new DataStoreException("JAXB exception while initializing the file observation reader");
        }
        mapper = ObservationJsonUtils.getMapper();
    }
}
