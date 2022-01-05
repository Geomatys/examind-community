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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import static java.nio.file.Files.isDirectory;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.sis.util.ArgumentChecks;
import org.constellation.business.ISensorBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.util.Util;
import org.geotoolkit.nio.IOUtilities;

/**
 * Various Utility method to handle test resource.
 *
 * @author Guilhem Legal (Geomatrys)
 */
public class TestResourceUtils {

    /**
     * Unmarshall a XML resource file containing a Sensor description (like sensorML).
     * 
     * @param resourceName resource path.
     * @param sensorBusiness SensorBusiness bean used to unmarshall the XML.
     * 
     * @return A java pojo.
     * @throws ConstellationException if an error occurs during the process.
     */
    public static Object unmarshallSensorResource(String resourceName, ISensorBusiness sensorBusiness) throws ConstellationException {
        StringWriter fw = new StringWriter();
        try (InputStream in = Util.getResourceAsStream(resourceName)) {
            byte[] buffer = new byte[1024];
            int size;

            while ((size = in.read(buffer, 0, 1024)) > 0) {
                fw.write(new String(buffer, 0, size));
            }
            return sensorBusiness.unmarshallSensor(fw.toString());
        } catch (IOException ex) {
            throw new ConstellationException(ex);
        }
    }

    /**
     * Copy a resource file into the specified target directory.
     * The character ':' will be replaced by 'µ' on the new file name for windows compatibility.
     *
     * @param targetDirectory The directory where to create a new file.
     * @param resourceName Resource file path.
     * @param newFileName Name of the new file created.
     * 
     * @throws IOException if an error occurs during the copy.
     */
    public static void writeResourceDataFile(Path targetDirectory, String resourceName, String newFileName) throws IOException {
        writeResourceDataFile(targetDirectory, resourceName, newFileName, 'µ');
    }

    /**
     * Copy a resource file into the specified target directory.
     * The character ':' will be replaced by the specified relacement character on the new file name for windows compatibility.
     *
     * @param targetDirectory The directory where to create a new file.
     * @param resourceName Resource file path.
     * @param newFileName Name of the new file created.
     * @param replacement Replacement character for the ':' in created file name.
     * 
     * @throws IOException If an error occurs during the copy.
     */
    public static void writeResourceDataFile(Path targetDirectory, String resourceName, String newFileName, char replacement) throws IOException {
        newFileName = newFileName.replace(':', replacement);
        Path dataFile = targetDirectory.resolve(newFileName);
        try (InputStream in = Util.getResourceAsStream(resourceName)) {
            Files.copy(in, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Copy a resource file into the specified target directory. The file must be a text file.
     * The character ':' will be replaced by 'µ' on the new file name for windows compatibility.
     * If the text file contain the constant "EPSG_VERSION", it will be replaced with the current EPSG database version.
     * 
     * @param targetDirectory The directory where to create a new file.
     * @param resourceName Resource file path.
     * @param newFileName Name of the new file created.
     * @param epsgVersion The current EPSG database version.
     *
     * @throws IOException If an error occurs during the copy.
     */
    public static void writeDataFileEPSG(Path targetDirectory, String resourceName, String newFileName, String epsgVersion) throws IOException {
        newFileName = newFileName.replace(':', 'µ');
        Path dataFile = targetDirectory.resolve(newFileName);
        try (OutputStream out = Files.newOutputStream(dataFile);
             OutputStreamWriter fw = new OutputStreamWriter(out)) {
            String content = getResourceAsString(resourceName).replace("EPSG_VERSION", epsgVersion);
            fw.write(content);
        }
    }

    /**
     * Return the string content of a resource text file.
     * 
     * @param resourceName Resource file path.
     * @return  Resource file path.
     * 
     * @throws IOException If an error occurs during the resource file reading.
     */
    public static String getResourceAsString(String resourceName) throws IOException {
        InputStream in = Util.getResourceAsStream(resourceName);
        return IOUtilities.toString(in);
    }

    /**
     * Recursively copy a file/directory into the target one.
     * If an XML file is copied, the constant "EPSG_VERSION", will be replaced with the current EPSG database version.
     *
     * @param sourcePath File to copy.
     * @param targetPath Target path.
     * @param copyOption
     *
     * @throws IOException If an error occurs during the copy.
     */
    public static void copyEPSG(Path sourcePath, Path targetPath, CopyOption... copyOption) throws IOException {
        ArgumentChecks.ensureNonNull("sourcePath", sourcePath);
        ArgumentChecks.ensureNonNull("targetPath", targetPath);

        if (isDirectory(sourcePath)) {
            Files.walkFileTree(sourcePath, new EPSGCopyFileVisitor(targetPath, copyOption));
        } else {
            Files.copy(sourcePath, targetPath, copyOption);
        }
    }
}
