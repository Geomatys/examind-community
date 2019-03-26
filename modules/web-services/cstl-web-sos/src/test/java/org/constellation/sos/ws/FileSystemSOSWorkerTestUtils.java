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
package org.constellation.sos.ws;

import org.constellation.util.Util;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.Charset;
 
/**
 * @author Quentin Boileau (Geomatys)
 */
public abstract class FileSystemSOSWorkerTestUtils extends SOSWorkerTest {


    public static void writeCommonDataFile(File dataDirectory, String resourceName, String identifier) throws IOException {

        File dataFile = new File(dataDirectory, identifier + ".xml");
        try (FileWriter fw = new FileWriter(dataFile)) {
            try (InputStream in = Util.getResourceAsStream("org/constellation/xml/sml/" + resourceName)) {

                byte[] buffer = new byte[1024];
                int size;

                while ((size = in.read(buffer, 0, 1024)) > 0) {
                    fw.write(new String(buffer, 0, size));
                }
            }
        }
    }

    /**
     * Copy and fix EPSG version
     * @param dataDirectory
     * @param resourceName
     * @param identifier
     * @throws IOException
     */
    public static void writeDataFile(File dataDirectory, String resourceName, String identifier, String epsgVersion) throws IOException {

        File dataFile = new File(dataDirectory, identifier + ".xml");
        try(FileWriter fw = new FileWriter(dataFile)) {
            try (InputStream in = Util.getResourceAsStream("org/constellation/sos/" + resourceName)) {
                final Charset charset = Charset.forName("UTF-8");
                String content = StreamUtils.copyToString(in, charset);
                content = content.replace("EPSG_VERSION", epsgVersion);
                fw.write(content);
            }
        }
    }
}