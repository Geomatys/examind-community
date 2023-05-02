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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.constellation.test.utils.Order;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.IdentifierQuery;
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

    private static Path DATA_DIRECTORY;
    
    private static Path survalFile;
    private static Path tsvFile;
    protected static Path bigdataFile;
     protected static Path XdataFile;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final Path configDir = Paths.get("target");
        DATA_DIRECTORY      = configDir.resolve("data" + UUID.randomUUID());
        Path survalDirectory = DATA_DIRECTORY.resolve("surval");
        Files.createDirectories(survalDirectory);
        Path tsvDirectory = DATA_DIRECTORY.resolve("tsv-flat");
        Files.createDirectories(tsvDirectory);
        Path bgDirectory = DATA_DIRECTORY.resolve("bigdata-profile");
        Files.createDirectories(bgDirectory);
        Path xDataFlatDirectory = DATA_DIRECTORY.resolve("xlsx-flat");
        Files.createDirectories(xDataFlatDirectory);


        writeResourceDataFile(survalDirectory, "com/examind/process/sos/surval-small.csv", "surval-small.csv");
        survalFile = survalDirectory.resolve("surval-small.csv");

        writeResourceDataFile(survalDirectory, "com/examind/process/sos/tabulation-flat.tsv", "tabulation-flat.tsv");
        tsvFile = survalDirectory.resolve("tabulation-flat.tsv");

        writeResourceDataFile(bgDirectory, "com/examind/process/sos/bigdata-1.csv", "bigdata-1.csv");
        bigdataFile = bgDirectory.resolve("bigdata-1.csv");

        writeResourceDataFile(xDataFlatDirectory, "com/examind/process/sos/test-flat.xlsx", "test-flat.xlsx");
        XdataFile = xDataFlatDirectory.resolve("test-flat.xlsx");
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
        Assert.assertEquals(1, procedureNames.size());

        String sensorId = "urn:surval:25049001";
        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("7-FLORTOT"));
        Assert.assertTrue(phenomenonNames.contains("18-FLORTOT"));
        Assert.assertTrue(phenomenonNames.contains("18-SALI"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalGeometricPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Period tp = (Period) time;
        Assert.assertEquals("1987-06-01" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("2019-12-17" , sdf.format(tp.getEnding().getDate()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals("urn:surval:25049001", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

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


    @Test
    public void csvStoreTSVTest() throws Exception {

        String sensorId = "urn:sensor:1";
        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(tsvFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue(sensorId);
        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("tsv");

        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf('\t'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String sid = procedureNames.iterator().next();
        Assert.assertEquals(sensorId, sid);

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalGeometricPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00.000" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("1980-03-02T21:52:00.000" , sdf.format(tp.getEnding().getDate()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        Assert.assertEquals(1, results.procedures.get(0).spatialBound.getHistoricalLocations().size());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(1, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00.000" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("1980-03-02T21:52:00.000" , sdf.format(tp.getEnding().getDate()));
    }

    @Test
    public void harvesterCSVFlatTSTest() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();

        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(bigdataFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("station_date");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("station_date");

        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_COLUMN.getName().getCode()).setValue("platform_code");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue("urn:template:");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("latitude");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("longitude");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("parameter_value");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("parameter_code");
        params.parameter(CsvFlatObservationStoreFactory.QUALITY_COLUMN.getName().getCode()).setValue("parameter_qc");
        params.parameter(CsvFlatObservationStoreFactory.TYPE_COLUMN.getName().getCode()).setValue("file_type");

        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_FILTER_COLUMN.getName().getCode()).setValue("30,35");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");
        params.parameter(CsvFlatObservationStoreFactory.SEPARATOR.getName().getCode()).setValue(Character.valueOf(','));
        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(301, procedureNames.size());

        // verify that the sensor has been created
        Assert.assertTrue(procedureNames.contains("urn:template:1501563"));
        Assert.assertTrue(procedureNames.contains("urn:template:1501564"));

        // not matching the parameters
        Assert.assertFalse(procedureNames.contains("urn:template:1301603"));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertEquals(2, phenomenonNames.size());
        Assert.assertTrue(phenomenonNames.contains("30"));
        Assert.assertTrue(phenomenonNames.contains("35"));

        String sensorId = "urn:template:1501563";
        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalGeometricPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        Period tp = (Period) time;
        Assert.assertEquals("2020-03-24T00:00:00.000" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("2020-03-24T10:00:00.000" , sdf.format(tp.getEnding().getDate()));

        // full dataset
        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(301, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        
        Assert.assertEquals("urn:template:1300131", proc.getId());

        // full procedures
        Assert.assertEquals(3, proc.spatialBound.getHistoricalLocations().size());
        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(301, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:template:1300131", proc.getId());
        Assert.assertEquals(3, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("2020-03-24T00:00:00.000" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("2020-03-24T03:00:00.000" , sdf.format(tp.getEnding().getDate()));

        // filtered dataset
        results = store.getDataset(new DatasetQuery(Arrays.asList("urn:template:1300131")));
        Assert.assertEquals(1, results.procedures.size());
        proc = results.procedures.get(0);

        Assert.assertEquals("urn:template:1300131", proc.getId());
        Assert.assertEquals(3, proc.spatialBound.getHistoricalLocations().size());

        // filtered procedure
        procedures = store.getProcedureDatasets(new DatasetQuery(Arrays.asList("urn:template:1300131")));

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:template:1300131", proc.getId());
        Assert.assertEquals(3, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);
    }

    @Test
    public void xlsxStoreTSTest() throws Exception {

        String sensorId = "urn:sensor:x";
        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(XdataFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RES");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue(sensorId);
        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("xlsx");

        CsvFlatObservationStore store = factory.open(params);

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        Assert.assertEquals(1, results.procedures.get(0).spatialBound.getHistoricalLocations().size());

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String sid = procedureNames.iterator().next();
        Assert.assertEquals(sensorId, sid);

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMP"));
        Assert.assertTrue(phenomenonNames.contains("SALINITY"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalGeometricPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00.000" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("1980-03-02T21:52:00.000" , sdf.format(tp.getEnding().getDate()));

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(1, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00.000" , sdf.format(tp.getBeginning().getDate()));
        Assert.assertEquals("1980-03-02T21:52:00.000" , sdf.format(tp.getEnding().getDate()));
    }
}
