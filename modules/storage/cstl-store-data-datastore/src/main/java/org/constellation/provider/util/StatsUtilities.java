/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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
package org.constellation.provider.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.dto.StatInfo;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.storage.coverage.ImageStatistics;

import static org.constellation.api.StatisticState.*;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StatsUtilities {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.provider");

    /**
     * Get and parse data statistics.
     *
     * @param s specified statistics information.
     *
     * @return ImageStatistics object or Empty if data is not a coverage or if Statistics were not computed.
     * @throws ConstellationStoreException
     */
    public static Optional<ImageStatistics> getDataStatistics(StatInfo s) throws ConstellationStoreException {
        if (s != null) {
            final String state = s.getState();
            try {
                if (state != null) {
                    switch (state) {
                        case STATE_PARTIAL : //fall through
                        case STATE_COMPLETED :
                        case STATE_ERROR : //can have partial statistics even if an error occurs.
                            final String result = s.getResult();
                            if (result != null && result.startsWith("{")) {
                                return deserializeImageStatistics(result);
                            } else {
                                LOGGER.log(Level.WARNING, "Unreadable statistics flagged as {0}", state);
                            }
                    }
                }

            } catch (IOException e) {
                throw new ConstellationStoreException("Invalid statistic JSON format for data. ", e);
            }
        }
        return Optional.empty();
    }

    private static Optional<ImageStatistics> deserializeImageStatistics(String result) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(ImageStatistics.class, new ImageStatisticDeserializer()); //custom deserializer
        mapper.registerModule(module);
        return Optional.of(mapper.readValue(result, ImageStatistics.class));
    }
}
