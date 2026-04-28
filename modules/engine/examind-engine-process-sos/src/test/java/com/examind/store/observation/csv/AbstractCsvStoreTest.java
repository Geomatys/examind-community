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
import java.util.List;
import java.util.UUID;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.temporal.object.TemporalUtilities;
import org.junit.AfterClass;
import org.junit.Assert;
import org.opengis.temporal.Instant;

/**
 *
 * @author Guilhem Legal (geomatys)
 */
public class AbstractCsvStoreTest {
    
    protected static Path DATA_DIRECTORY;
    
    public static void setUpClass() throws Exception {
        final Path configDir = Paths.get("target");
        DATA_DIRECTORY       = configDir.resolve("data"  + UUID.randomUUID());
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        IOUtilities.deleteSilently(DATA_DIRECTORY);
    }
    
    protected static Path writeResourceFileInDir(String dirName, String srcName, String fileName) throws IOException {
        Path dir = Files.createDirectories(DATA_DIRECTORY.resolve(dirName));
        writeResourceDataFile(dir, "com/examind/process/sos/" + srcName, fileName);
        return dir.resolve(fileName);
        
    }
    protected static Path writeResourceFileInDir(String dirName, String fileName) throws IOException {
        return writeResourceFileInDir(dirName, fileName, fileName);
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
    
    protected void verifyTSFields(ProcedureDataset proc, int nbField) {
        verifyFields(proc.fields, FieldDataType.TIME, nbField, false);
    }
    
    protected void verifyTSFields(ComplexResult res, int nbField) {
        verifyFields(res.getFields(), FieldDataType.TIME, nbField, false);
    }
    
    protected void verifyPRFields(ComplexResult res, int nbField) {
        verifyFields(res.getFields(), FieldDataType.QUANTITY, nbField, false);
    }
    
    protected void verifyPRFields(ComplexResult res, int nbField, boolean includeTime) {
        verifyFields(res.getFields(), FieldDataType.QUANTITY, nbField, includeTime);
    }
    
    protected void verifyPRFields(ProcedureDataset proc, int nbField) {
       verifyFields(proc.fields, FieldDataType.QUANTITY, nbField, false);
    }
    
    protected void verifyPRFields(ProcedureDataset proc, int nbField, boolean includeTime) {
       verifyFields(proc.fields, FieldDataType.QUANTITY, nbField, includeTime);
    }
    
    private void verifyFields(List<Field> fields, FieldDataType mainType, int nbField, boolean includeTime) {
        int i = 0;
        if (includeTime) {
            Assert.assertEquals(FieldDataType.TIME, fields.get(i).getDataType());
            Assert.assertEquals(FieldType.METADATA, fields.get(i).getType());
            i++;
        }
        Assert.assertEquals(nbField, fields.size());
        Assert.assertEquals(mainType, fields.get(i).getDataType());
        Assert.assertEquals(FieldType.MAIN, fields.get(i).getType());
    }
}
