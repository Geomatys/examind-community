/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.constellation.business.ISensorBusiness;
import org.constellation.util.Util;
import org.geotoolkit.nio.IOUtilities;

/**
 *
 * @author Guilhem Legal (Geomatrys)
 */
public class TestResourceUtils {

     public static Object unmarshallSensorResource(String resourceName, ISensorBusiness sensorBusiness) throws Exception {
        StringWriter fw = new StringWriter();
        InputStream in = Util.getResourceAsStream(resourceName);

        byte[] buffer = new byte[1024];
        int size;

        while ((size = in.read(buffer, 0, 1024)) > 0) {
            fw.write(new String(buffer, 0, size));
        }
        in.close();
        return sensorBusiness.unmarshallSensor(fw.toString());
    }

    public static void writeResourceDataFile(Path dataDirectory, String resourceName, String identifier) throws IOException {
        writeResourceDataFile(dataDirectory, resourceName, identifier, 'µ');
    }

    public static void writeResourceDataFile(Path dataDirectory, String resourceName, String identifier, char replacement) throws IOException {
        identifier = identifier.replace(':', replacement);
        Path dataFile = dataDirectory.resolve(identifier);
        try (InputStream in = Util.getResourceAsStream(resourceName)) {
            Files.copy(in, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void writeDataFileEPSG(Path dataDirectory, String resourceName, String identifier, String epsgVersion) throws IOException {
        identifier = identifier.replace(':', 'µ');
        Path dataFile = dataDirectory.resolve(identifier);
        try (OutputStream out = Files.newOutputStream(dataFile);
             OutputStreamWriter fw = new OutputStreamWriter(out);
             InputStream in = Util.getResourceAsStream(resourceName)) {
            String content = IOUtilities.toString(in).replace("EPSG_VERSION", epsgVersion);
            fw.write(content);
        }
    }

     public static String getResourceAsString(String resourceName) throws IOException {
        InputStream in = Util.getResourceAsStream(resourceName);
        return IOUtilities.toString(in);
    }
}
