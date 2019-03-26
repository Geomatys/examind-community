/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.api.rest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import org.apache.sis.util.logging.Logging;
import org.constellation.util.Util;
import org.geotoolkit.sos.netcdf.NetCDFExtractor;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class RestApiUtil {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.api.rest");

    public static String findDataType(String filePath, String extension, String selectedType) {
         // look for observation netcdf
        if ("nc".equals(extension) && NetCDFExtractor.isObservationFile(filePath)) {
            selectedType = "observation";
        }
        // look for SML file (available for data import ?)
        if ("xml".equals(extension)) {
            try {
                String rootMark = Util.getXmlDocumentRoot(filePath);
                if (rootMark.equals("SensorML")) {
                    selectedType = "observation";
                }
            } catch (IOException | XMLStreamException ex) {
                LOGGER.log(Level.WARNING, "error while reading xml file", ex);
            }
        }
        return selectedType;
    }
}
