/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.admin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.sis.util.logging.Logging;
import org.constellation.api.DataType;
import org.constellation.dto.Data;
import org.constellation.exception.ConfigurationException;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.metadata.ImageStatistics;
import org.geotoolkit.storage.DataStores;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataCoverageUtilities {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.provider");

    /**
     * Get and parse data statistics.
     *
     * @param d
     *
     * @return ImageStatistics object or null if data is not a coverage or if Statistics were not computed.
     * @throws org.constellation.exception.ConfigurationException
     */
    public static ImageStatistics getDataStatistics(Data d) throws ConfigurationException {
        final String type = d.getType();
        final Boolean rendered = d.getRendered();
        final String state = d.getStatsState();
        final String result = d.getStatsResult();
        if (DataType.COVERAGE.name().equals(type) && (rendered == null || !rendered)) {
            try {
                if (state != null) {
                    switch (state) {
                        case "PARTIAL" : //fall through
                        case "COMPLETED" :
                            return deserializeImageStatistics(result);
                        case "PENDING" : return null;
                        case "ERROR" :
                            //can have partial statistics even if an error occurs.
                            if (result != null && result.startsWith("{")) {
                                return deserializeImageStatistics(result);
                            } else {
                                return null;
                            }
                    }
                }

            } catch (IOException e) {
                throw new ConfigurationException("Invalid statistic JSON format.", e);
            }
        }
        return null;
    }

    private static ImageStatistics deserializeImageStatistics(String state) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(ImageStatistics.class, new ImageStatisticDeserializer()); //custom deserializer
        mapper.registerModule(module);
        return mapper.readValue(state, ImageStatistics.class);
    }

    /**
     * Return an extension list managed by geotoolkit.
     *
     * @return a {@link Map} which contain file extension.
     */
    public static Map<String, String> getAvailableFileExtension() {
        final Map<String, String> extensions = new HashMap<>(0);

        //get coverage file extension and add on list
        final String[] coverageExtension = ImageIO.getReaderFileSuffixes();
        for (String extension : coverageExtension) {
            extensions.put(extension.toLowerCase(), "raster");
        }

        //access to features file factories
        final Iterator<FileFeatureStoreFactory> ite = DataStores.getProviders(FileFeatureStoreFactory.class).iterator();
        while (ite.hasNext()) {

            final FileFeatureStoreFactory factory = ite.next();
            //display general informations about this factory
            String[] tempExtensions = factory.getSuffix().toArray(new String[0]);
            for (int i = 0; i < tempExtensions.length; i++) {
                String extension = tempExtensions[i];

                //remove point before extension
                extensions.put(extension.toLowerCase(), "vector");
            }
        }

        // for O&M files
        extensions.put("xml", "observation");
        return extensions;
    }
}
