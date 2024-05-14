/*
 *    Examind community - An open source and standard compliant SDI
 *    https://www.examind.com/examind-community/
 *
 * Copyright 2024 Geomatys.
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
package com.examind.store.observation.csv;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.UUID;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.junit.AfterClass;
import org.opengis.temporal.Instant;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class AbstractCsvStoreTest {
    
    private static Path DATA_DIRECTORY;
    
    public static void setUpClass() throws Exception {
        final Path configDir = Paths.get("target");
        DATA_DIRECTORY       = configDir.resolve("data"  + UUID.randomUUID());
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        IOUtilities.deleteSilently(DATA_DIRECTORY);
    }
    
    protected static Path writeResourceFileInDir(String dirName, String fileName) throws IOException {
        Path dir = Files.createDirectories(DATA_DIRECTORY.resolve(dirName));
        writeResourceDataFile(dir, "com/examind/process/sos/" + fileName, fileName);
        return dir.resolve(fileName);
    }
    
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    protected static String format(Temporal t) {
        return SDF.format(TemporalUtilities.toDate(t));
    }
    
    protected static String format(Instant t) {
        return SDF.format(Date.from(TemporalUtilities.toInstant(t)));
    }
    
    protected static String format(SimpleDateFormat sdf,Instant t) {
        return sdf.format(Date.from(TemporalUtilities.toInstant(t)));
    }
}
