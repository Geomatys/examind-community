/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package com.examind.store.observation.csv;

import com.examind.store.observation.csvflat.CsvFlatObservationStore;
import com.examind.store.observation.csvflat.CsvFlatObservationStoreFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.constellation.test.utils.Order;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.ObservationReader;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalGeometricPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvFlatObservationStoreTest {

    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation.csv");
    private static Path DATA_DIRECTORY;
    
    private static Path survalFile;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final Path configDir = Paths.get("target");
        DATA_DIRECTORY      = configDir.resolve("data" + UUID.randomUUID());
        Path survalDirectory = DATA_DIRECTORY.resolve("surval");
        Files.createDirectories(survalDirectory);

        writeResourceDataFile(survalDirectory, "com/examind/process/sos/surval-small.csv", "surval-small.csv");
        survalFile = survalDirectory.resolve("surval-small.csv");

    }
    @AfterClass
    public static void tearDownClass() throws Exception {
        IOUtilities.deleteSilently(DATA_DIRECTORY);
    }

    @Test
    @Order(order = 1)
    public void csvFlatStoreSurvalTest() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(survalFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("ANALYSE_DATE");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("ANALYSE_DATE");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("dd/MM/yy");
       
        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LATITUDE");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LONGITUDE");

        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_FILTER_COLUMN.getName().getCode()).setValue("7-FLORTOT,18-FLORTOT,18-SALI");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("VALUE");

        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("SUPPORT,PARAMETER");
         params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_NAME_COLUMN.getName().getCode()).setValue("SUPPORT_NAME,PARAMETER_NAME");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_NAME_COLUMN.getName().getCode()).setValue("PLATFORM_NAME");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_DESC_COLUMN.getName().getCode()).setValue("PLATFORM_DESC");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_COLUMN.getName().getCode()).setValue("PLATFORM_ID");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue("urn:surval:");
        params.parameter(CsvFlatObservationStoreFactory.UOM_COLUMN.getName().getCode()).setValue("PARAMETER_UNIT:");


        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(9, procedureNames.size());

        String sensorId = "urn:surval:25049001";
        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("7-FLORTOT"));
        Assert.assertTrue(phenomenonNames.contains("18-FLORTOT"));
        Assert.assertTrue(phenomenonNames.contains("18-SALI"));

        ObservationReader reader = store.getReader();

        TemporalGeometricPrimitive time = reader.getTimeForProcedure(sensorId);

        Assert.assertTrue(time instanceof Period);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Period tp = (Period) time;
        Assert.assertEquals("1987-06-01" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("2019-12-17" , sdf.format(tp.getEnding().getDate()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        final StringBuilder sb = new StringBuilder("result procedures:\n");
        results.procedures.stream().forEach(p -> sb.append(p.getId()).append("\n"));
        LOGGER.info(sb.toString());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals("urn:surval:25049001", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        List<ProcedureDataset> procedures = store.getProcedures();

        final StringBuilder sb2 = new StringBuilder("procedures:\n");
        procedures.stream().forEach(p -> sb2.append(p.getId()).append("\n"));
        LOGGER.info(sb2.toString());
        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:surval:25049001", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1987-06-01" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("2019-12-17" , sdf.format(tp.getEnding().getDate()));
    }


}
