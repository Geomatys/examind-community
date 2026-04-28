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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.observation.model.ComplexResult;
import org.geotoolkit.observation.model.CompositePhenomenon;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.OMEntity;
import org.geotoolkit.observation.model.Observation;
import org.geotoolkit.observation.model.ObservationDataset;
import org.geotoolkit.observation.model.ProcedureDataset;
import org.geotoolkit.observation.query.DatasetQuery;
import org.geotoolkit.observation.query.IdentifierQuery;
import org.geotoolkit.observation.query.ObservedPropertyQuery;
import org.geotoolkit.observation.query.ProcedureQuery;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.geotoolkit.observation.model.Phenomenon;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CsvFlatObservationStoreTest extends AbstractCsvStoreTest {

    private static Path survalFile;
    private static Path tsvFile;
    private static Path tsvTimeSepFile;
    private static Path bigdataFile;
    private static Path xDataFile;
    private static Path qualSpaceFile;
    private static Path propertiesFile;
    private static Path diffLengthFile;
    private static Path diffLengthFile2;
    private static Path incompLineFile;
    private static Path regexMatchingObsPropFile;

    @BeforeClass
    public static void setUpClass() throws Exception {
        AbstractCsvStoreTest.setUpClass();
        
        survalFile      = writeResourceFileInDir("surval", "surval-small.csv");
        tsvFile         = writeResourceFileInDir("tsv-flat", "tabulation-flat.tsv");
        tsvTimeSepFile  = writeResourceFileInDir("tsv-flat", "tabulation-timesep-flat.tsv");
        bigdataFile     = writeResourceFileInDir("bigdata-profile", "bigdata-1.csv");
        xDataFile       = writeResourceFileInDir("xlsx-flat", "test-flat.xlsx");
        qualSpaceFile   = writeResourceFileInDir("qual-space-flat", "quality-space-flat.csv");
        propertiesFile  = writeResourceFileInDir("properties-flat", "properties-flat.csv");
        diffLengthFile  = writeResourceFileInDir("diff-length", "flat-diff-length.csv");
        diffLengthFile2 = writeResourceFileInDir("diff-length", "flat-diff-length-2.csv");
        incompLineFile  = writeResourceFileInDir("incomp-line", "incomplete-line-flat.csv");
        regexMatchingObsPropFile = writeResourceFileInDir("reg-match-op", "regex-match-op-flat.csv"); 
    }
    
    @Test
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
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_DESC_COLUMN.getName().getCode()).setValue("PARAMETER_GROUP");

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
        
        List<Phenomenon> phenomenons = store.getPhenomenons(new ObservedPropertyQuery(true));
        Assert.assertEquals(1, phenomenons.size());
        Assert.assertTrue(phenomenons.get(0) instanceof CompositePhenomenon);
        CompositePhenomenon composite = (CompositePhenomenon) phenomenons.get(0);
        Assert.assertEquals(3, composite.getComponent().size());
        Assert.assertEquals("18-FLORTOT", composite.getComponent().get(0).getId());
        Assert.assertEquals("Support : Masse d'eau, eau brute - Niveau : Mi-profondeur-Flore Totale - abondance de cellules", composite.getComponent().get(0).getName());
        Assert.assertEquals("Biologie/Phytoplancton", composite.getComponent().get(0).getDescription());

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1987-06-01T00:00:00" , format(tp.getBeginning()));
        Assert.assertEquals("2019-12-17T00:00:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals("urn:surval:25049001", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());
        
        Assert.assertEquals(1, results.observations.size());
        
        Observation obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        ComplexResult cRes = (ComplexResult) obsResult.getResult();
        
        verifyTSFields(cRes, 4);
        
        String expectedValues = getResourceAsString("com/examind/process/sos/surval-datablock-values.txt");
        Assert.assertEquals(expectedValues, cRes.getValues() + '\n');
        
        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:surval:25049001", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1987-06-01T00:00:00" , format(tp.getBeginning()));
        Assert.assertEquals("2019-12-17T00:00:00" , format(tp.getEnding()));
      
        verifyTSFields(proc, 4);
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
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        Assert.assertEquals(1, results.procedures.get(0).spatialBound.getHistoricalLocations().size());
        
        Assert.assertEquals(1, results.observations.size());
        
        Observation obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        ComplexResult cRes = (ComplexResult) obsResult.getResult();
        
        verifyTSFields(cRes, 2);
        
        String expectedValues = getResourceAsString("com/examind/process/sos/tabulation.txt");
        Assert.assertEquals(expectedValues, cRes.getValues() + '\n');

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset proc = procedures.get(0);
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 2);
    }
    
    @Test
    public void csvStoreTSVTimeSeparatedTest() throws Exception {

        String sensorId = "urn:sensor:tsv-ts-1";
        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(tsvTimeSepFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("DATE,TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("DATE,TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-ddHH:mm:ss.S");

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
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        Assert.assertEquals(1, results.procedures.get(0).spatialBound.getHistoricalLocations().size());
        
        Assert.assertEquals(1, results.observations.size());
        
        Observation obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        ComplexResult cRes = (ComplexResult) obsResult.getResult();
        
        verifyTSFields(cRes, 2);
        
        String expectedValues = getResourceAsString("com/examind/process/sos/tabulation.txt");
        Assert.assertEquals(expectedValues, cRes.getValues() + '\n');

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset proc = procedures.get(0);
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 2);
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
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("2020-03-24T00:00:00" , format(tp.getBeginning()));
        Assert.assertEquals("2020-03-24T10:00:00" , format(tp.getEnding()));

        // full dataset
        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(301, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);

        Assert.assertEquals("urn:template:1300131", proc.getId());

        // full procedures
        Assert.assertEquals(3, proc.spatialBound.getHistoricalLocations().size());
        
        Assert.assertEquals(301, results.observations.size());
        
        Observation obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        ComplexResult cRes = (ComplexResult) obsResult.getResult();
        
        verifyTSFields(cRes, 3);
        
        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(301, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:template:1300131", proc.getId());
        Assert.assertEquals(3, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("2020-03-24T00:00:00" , format(tp.getBeginning()));
        Assert.assertEquals("2020-03-24T03:00:00" , format(tp.getEnding()));

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
        
        verifyTSFields(proc, 3);
    }
    
    @Test
    public void harvesterCSVFlatPRTest() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();

        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(bigdataFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("station_date");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("z_value");

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

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Profile");
        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");
        params.parameter(CsvFlatObservationStoreFactory.SEPARATOR.getName().getCode()).setValue(Character.valueOf(','));
        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(11, procedureNames.size());

        // verify that the sensor has been created
        Assert.assertTrue(procedureNames.contains("urn:template:1901880"));
        Assert.assertTrue(procedureNames.contains("urn:template:1901710"));

        // not matching the parameters
        Assert.assertFalse(procedureNames.contains("urn:template:1501563"));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertEquals(2, phenomenonNames.size());
        Assert.assertTrue(phenomenonNames.contains("30"));
        Assert.assertTrue(phenomenonNames.contains("35"));

        String sensorId = "urn:template:1901880";
        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Instant);

        Instant tp = (Instant) time;
        Assert.assertEquals("2020-03-24T02:57:05" , format(tp.getPosition()));

        // full dataset
        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(11, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);

        Assert.assertEquals("urn:template:1901290", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());
        
        Assert.assertEquals(11, results.observations.size());
        
        Observation obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        ComplexResult cRes = (ComplexResult) obsResult.getResult();
        
        verifyPRFields(cRes, 3);
        
        Assert.assertEquals(
                  "4.4,33.708,1,1.162,1@@"
                + "9.1,33.708,1,1.161,1@@"
                + "14.2,33.71,1,1.156,1@@"
                + "19.9,33.702,1,1.164,1@@"
                + "25.8,33.696,1,1.162,1@@"
                + "31.9,33.676,1,1.164,1@@"
                + "37.6,33.645,1,1.164,1@@"
                + "42.9,33.588,4,1.163,4@@"
                + "48.2,33.445,4,1.149,4@@"
                + "54.6,32.949,4,0.994,4@@"
                + "58.7,33.403,1,-0.062,1@@"
                + "62.9,33.805,1,-0.78,1@@"
                + "67.3,33.956,1,-1.001,1@@"
                + "71.7,33.942,1,-1.002,1@@"
                + "79.6,34.014,1,-1.042,1@@"
                + "84.1,34.037,1,-1.063,1@@"
                + "88.6,34.143,1,-1.1,1@@"
                + "93.1,34.258,1,-1.044,1@@"
                + "97.7,34.294,1,-0.851,1@@"
                + "108.2,34.362,1,-0.249,1@@"
                + "118.6,34.443,1,0.213,1@@"
                + "128.7,34.461,1,0.737,1@@"
                + "138.8,34.546,1,1.085,1@@"
                + "149.1,34.551,1,1.546,1@@"
                + "157.6,34.498,1,1.754,1@@"
                + "168.4,34.512,1,1.877,1@@"
                + "178.9,34.514,1,1.862,1@@"
                + "189.6,34.52,1,1.908,1@@"
                + "197.4,34.534,1,1.914,1@@"
                + "208.0,34.548,1,1.939,1@@"
                + "218.9,34.572,1,1.965,1@@"
                + "226.8,34.566,1,2.003,1@@"
                + "238.3,34.579,1,2.016,1@@"
                + "249.3,34.584,1,2.043,1@@"
                + "257.3,34.595,1,2.044,1@@"
                + "268.5,34.591,1,2.052,1@@"
                + "279.5,34.601,1,2.03,1@@"
                + "287.5,34.611,1,2.028,1@@"
                + "298.7,34.626,1,2.039,1@@"
                + "306.9,34.625,1,2.06,1@@"
                + "319.2,34.626,1,2.073,1@@"
                + "327.2,34.631,1,2.069,1@@"
                + "338.4,34.635,1,2.072,1@@"
                + "346.5,34.636,1,2.074,1@@"
                + "357.8,34.64,1,2.069,1@@"
                + "378.2,34.648,1,2.075,1@@"
                + "398.8,34.657,1,2.068,1@@"
                + "418.2,34.663,1,2.058,1@@"
                + "436.5,34.673,1,2.059,1@@"
                + "458.1,34.676,1,2.054,1@@"
                + "476.7,34.682,1,2.042,1@@"
                + "496.3,34.68,1,2.051,1@@"
                + "548.1,34.7,1,2.009,1@@"
                + "598.1,34.705,1,2.02,1@@"
                + "648.6,34.707,1,1.951,1@@"
                + "699.0,34.718,1,1.887,1@@"
                + "797.3,34.724,1,1.807,1@@"
                + "899.1,34.728,1,1.748,1@@"
                + "995.9,34.73,1,1.645,1@@"
                + "1094.3,34.732,1,1.565,1@@"
                + "1197.8,34.734,1,1.469,1@@"
                + "1297.2,34.728,1,1.388,1@@"
                + "1391.8,34.727,1,1.288,1@@"
                + "1498.9,34.719,1,1.189,1@@"
                + "1592.9,34.714,1,1.097,1@@"
                + "1699.7,34.708,1,0.996,1@@"
                + "1799.3,34.705,1,0.924,1@@"
                + "1899.6,34.699,1,0.845,1@@", cRes.getValues());
        
        // full dataset with time included
        DatasetQuery query = new DatasetQuery();
        query.setIncludeTimeForProfile(true);
        results = store.getDataset(query);
        Assert.assertEquals(11, results.procedures.size());
        proc = results.procedures.get(0);

        Assert.assertEquals("urn:template:1901290", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());
        
        Assert.assertEquals(11, results.observations.size());
        
        obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        cRes = (ComplexResult) obsResult.getResult();
        
        verifyPRFields(cRes, 4, true);
        
        Assert.assertEquals(
                  "2020-03-24T05:07:54.0,4.4,33.708,1,1.162,1@@"
                + "2020-03-24T05:07:54.0,9.1,33.708,1,1.161,1@@"
                + "2020-03-24T05:07:54.0,14.2,33.71,1,1.156,1@@"
                + "2020-03-24T05:07:54.0,19.9,33.702,1,1.164,1@@"
                + "2020-03-24T05:07:54.0,25.8,33.696,1,1.162,1@@"
                + "2020-03-24T05:07:54.0,31.9,33.676,1,1.164,1@@"
                + "2020-03-24T05:07:54.0,37.6,33.645,1,1.164,1@@"
                + "2020-03-24T05:07:54.0,42.9,33.588,4,1.163,4@@"
                + "2020-03-24T05:07:54.0,48.2,33.445,4,1.149,4@@"
                + "2020-03-24T05:07:54.0,54.6,32.949,4,0.994,4@@"
                + "2020-03-24T05:07:54.0,58.7,33.403,1,-0.062,1@@"
                + "2020-03-24T05:07:54.0,62.9,33.805,1,-0.78,1@@"
                + "2020-03-24T05:07:54.0,67.3,33.956,1,-1.001,1@@"
                + "2020-03-24T05:07:54.0,71.7,33.942,1,-1.002,1@@"
                + "2020-03-24T05:07:54.0,79.6,34.014,1,-1.042,1@@"
                + "2020-03-24T05:07:54.0,84.1,34.037,1,-1.063,1@@"
                + "2020-03-24T05:07:54.0,88.6,34.143,1,-1.1,1@@"
                + "2020-03-24T05:07:54.0,93.1,34.258,1,-1.044,1@@"
                + "2020-03-24T05:07:54.0,97.7,34.294,1,-0.851,1@@"
                + "2020-03-24T05:07:54.0,108.2,34.362,1,-0.249,1@@"
                + "2020-03-24T05:07:54.0,118.6,34.443,1,0.213,1@@"
                + "2020-03-24T05:07:54.0,128.7,34.461,1,0.737,1@@"
                + "2020-03-24T05:07:54.0,138.8,34.546,1,1.085,1@@"
                + "2020-03-24T05:07:54.0,149.1,34.551,1,1.546,1@@"
                + "2020-03-24T05:07:54.0,157.6,34.498,1,1.754,1@@"
                + "2020-03-24T05:07:54.0,168.4,34.512,1,1.877,1@@"
                + "2020-03-24T05:07:54.0,178.9,34.514,1,1.862,1@@"
                + "2020-03-24T05:07:54.0,189.6,34.52,1,1.908,1@@"
                + "2020-03-24T05:07:54.0,197.4,34.534,1,1.914,1@@"
                + "2020-03-24T05:07:54.0,208.0,34.548,1,1.939,1@@"
                + "2020-03-24T05:07:54.0,218.9,34.572,1,1.965,1@@"
                + "2020-03-24T05:07:54.0,226.8,34.566,1,2.003,1@@"
                + "2020-03-24T05:07:54.0,238.3,34.579,1,2.016,1@@"
                + "2020-03-24T05:07:54.0,249.3,34.584,1,2.043,1@@"
                + "2020-03-24T05:07:54.0,257.3,34.595,1,2.044,1@@"
                + "2020-03-24T05:07:54.0,268.5,34.591,1,2.052,1@@"
                + "2020-03-24T05:07:54.0,279.5,34.601,1,2.03,1@@"
                + "2020-03-24T05:07:54.0,287.5,34.611,1,2.028,1@@"
                + "2020-03-24T05:07:54.0,298.7,34.626,1,2.039,1@@"
                + "2020-03-24T05:07:54.0,306.9,34.625,1,2.06,1@@"
                + "2020-03-24T05:07:54.0,319.2,34.626,1,2.073,1@@"
                + "2020-03-24T05:07:54.0,327.2,34.631,1,2.069,1@@"
                + "2020-03-24T05:07:54.0,338.4,34.635,1,2.072,1@@"
                + "2020-03-24T05:07:54.0,346.5,34.636,1,2.074,1@@"
                + "2020-03-24T05:07:54.0,357.8,34.64,1,2.069,1@@"
                + "2020-03-24T05:07:54.0,378.2,34.648,1,2.075,1@@"
                + "2020-03-24T05:07:54.0,398.8,34.657,1,2.068,1@@"
                + "2020-03-24T05:07:54.0,418.2,34.663,1,2.058,1@@"
                + "2020-03-24T05:07:54.0,436.5,34.673,1,2.059,1@@"
                + "2020-03-24T05:07:54.0,458.1,34.676,1,2.054,1@@"
                + "2020-03-24T05:07:54.0,476.7,34.682,1,2.042,1@@"
                + "2020-03-24T05:07:54.0,496.3,34.68,1,2.051,1@@"
                + "2020-03-24T05:07:54.0,548.1,34.7,1,2.009,1@@"
                + "2020-03-24T05:07:54.0,598.1,34.705,1,2.02,1@@"
                + "2020-03-24T05:07:54.0,648.6,34.707,1,1.951,1@@"
                + "2020-03-24T05:07:54.0,699.0,34.718,1,1.887,1@@"
                + "2020-03-24T05:07:54.0,797.3,34.724,1,1.807,1@@"
                + "2020-03-24T05:07:54.0,899.1,34.728,1,1.748,1@@"
                + "2020-03-24T05:07:54.0,995.9,34.73,1,1.645,1@@"
                + "2020-03-24T05:07:54.0,1094.3,34.732,1,1.565,1@@"
                + "2020-03-24T05:07:54.0,1197.8,34.734,1,1.469,1@@"
                + "2020-03-24T05:07:54.0,1297.2,34.728,1,1.388,1@@"
                + "2020-03-24T05:07:54.0,1391.8,34.727,1,1.288,1@@"
                + "2020-03-24T05:07:54.0,1498.9,34.719,1,1.189,1@@"
                + "2020-03-24T05:07:54.0,1592.9,34.714,1,1.097,1@@"
                + "2020-03-24T05:07:54.0,1699.7,34.708,1,0.996,1@@"
                + "2020-03-24T05:07:54.0,1799.3,34.705,1,0.924,1@@"
                + "2020-03-24T05:07:54.0,1899.6,34.699,1,0.845,1@@", cRes.getValues());
        
        // full procedures
        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(11, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:template:1901290", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Instant);

        tp = (Instant) time;
        Assert.assertEquals("2020-03-24T05:07:54" , format(tp.getPosition()));
        
        verifyPRFields(proc, 3);

        // filtered dataset
        results = store.getDataset(new DatasetQuery(Arrays.asList("urn:template:1901290")));
        Assert.assertEquals(1, results.procedures.size());
        proc = results.procedures.get(0);

        Assert.assertEquals("urn:template:1901290", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());
        
        obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        cRes = (ComplexResult) obsResult.getResult();
        
        verifyPRFields(cRes, 3);


        // filtered procedure
        procedures = store.getProcedureDatasets(new DatasetQuery(Arrays.asList("urn:template:1901290")));

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("urn:template:1901290", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Instant);
        
        verifyPRFields(proc, 3);
    }

    @Test
    public void xlsxStoreTSTest() throws Exception {

        String sensorId = "urn:sensor:x";
        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(xDataFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RES");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_NAME_COLUMN.getName().getCode()).setValue("PROPNAME");

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

        List<Phenomenon> phenomenons = store.getPhenomenons(new ObservedPropertyQuery(true));
        for (Phenomenon phen : phenomenons) {
            if (phen.getId().endsWith("TEMP")) {
                Assert.assertEquals("12", phen.getName());
            } else if (phen.getId().endsWith("SALINITY")) {
                Assert.assertEquals("13", phen.getName());
            }
        }

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());
        
        Observation obsResult = results.observations.get(0);
        Assert.assertTrue(obsResult.getResult() instanceof ComplexResult);
        
        ComplexResult cRes = (ComplexResult) obsResult.getResult();
        
        verifyTSFields(cRes, 3);

        ProcedureDataset proc = procedures.get(0);
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 3);
    }

    @Test
    public void csvFlatStoreQualitySpaceTest() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(qualSpaceFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue("urn:space-qual:1");
        params.parameter(CsvFlatObservationStoreFactory.QUALITY_COLUMN.getName().getCode()).setValue("QUA LITY FI");
        params.parameter(CsvFlatObservationStoreFactory.QUALITY_COLUMN_TYPE.getName().getCode()).setValue("QUANTITY");


        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String sensorId = "urn:space-qual:1";
        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        Assert.assertEquals(1, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 2);

        Field f = cr.getFields().get(1);
        Assert.assertEquals(1, f.getQualityFields().size());

        Field qualityField = f.getQualityFields().get(0);
        Assert.assertEquals("qua_lity_fi", qualityField.getName());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 2);
    }
    
    @Test
    public void csvFlatStorePropertiesTest() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(propertiesFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue("urn:space-qual:1");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_PROPERTIES_MAP_COLUMN.getName().getCode()).setValue("PROC_METADATA");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_PROPERTIES_COLUMN.getName().getCode()).setValue("PROC_PROP3");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_PROPERTIES_MAP_COLUMN.getName().getCode()).setValue("OP_METADATA");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_PROPERTIES_COLUMN.getName().getCode()).setValue("OP_PROP3");


        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String sensorId = "urn:space-qual:1";
        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());
        Assert.assertEquals(3, proc.getProperties().size());
        Assert.assertTrue(proc.getProperties().containsKey("PROC_PROP1"));
        Assert.assertEquals("p1", proc.getProperties().get("PROC_PROP1"));
        Assert.assertTrue(proc.getProperties().containsKey("PROC_PROP2"));
        Assert.assertTrue(proc.getProperties().get("PROC_PROP2") instanceof List);
        List prop2 = (List) proc.getProperties().get("PROC_PROP2");
        Assert.assertEquals(2, prop2.size());
        Assert.assertEquals("p2_1", prop2.get(0));
        Assert.assertEquals("p2_2", prop2.get(1));
        Assert.assertTrue(proc.getProperties().containsKey("PROC_PROP3"));
        Assert.assertEquals("p3", proc.getProperties().get("PROC_PROP3"));

        Assert.assertEquals(1, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 2);
        
        Assert.assertEquals(1, results.phenomenons.size());
        Phenomenon phen = results.phenomenons.get(0);
        Assert.assertEquals(3, phen.getProperties().size());
        Assert.assertTrue(phen.getProperties().containsKey("OP_PROP1"));
        Assert.assertEquals("p1", phen.getProperties().get("OP_PROP1"));
        Assert.assertTrue(phen.getProperties().containsKey("OP_PROP2"));
        Assert.assertTrue(phen.getProperties().get("OP_PROP2") instanceof List);
        prop2 = (List) phen.getProperties().get("OP_PROP2");
        Assert.assertEquals(2, prop2.size());
        Assert.assertEquals("p2_1", prop2.get(0));
        Assert.assertEquals("p2_2", prop2.get(1));
        Assert.assertTrue(phen.getProperties().containsKey("OP_PROP3"));
        Assert.assertEquals("p3", phen.getProperties().get("OP_PROP3"));
                
                
        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());
        Assert.assertEquals(3, proc.getProperties().size());
        Assert.assertTrue(proc.getProperties().containsKey("PROC_PROP1"));
        Assert.assertEquals("p1", proc.getProperties().get("PROC_PROP1"));
        Assert.assertTrue(proc.getProperties().containsKey("PROC_PROP2"));
        Assert.assertTrue(proc.getProperties().get("PROC_PROP2") instanceof List);
        prop2 = (List) proc.getProperties().get("PROC_PROP2");
        Assert.assertEquals(2, prop2.size());
        Assert.assertEquals("p2_1", prop2.get(0));
        Assert.assertEquals("p2_2", prop2.get(1));
        Assert.assertTrue(proc.getProperties().containsKey("PROC_PROP3"));
        Assert.assertEquals("p3", proc.getProperties().get("PROC_PROP3"));

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 2);
    }

    @Test
    public void csvFlatStoreRenameQualityTest() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(qualSpaceFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue("urn:space-qual:1");
        params.parameter(CsvFlatObservationStoreFactory.QUALITY_COLUMN.getName().getCode()).setValue("QUA LITY FI");
        params.parameter(CsvObservationStoreFactory.QUALITY_COLUMN_ID.getName().getCode()).setValue("new_quality_name");


        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());

        String sensorId = "urn:space-qual:1";
        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        Assert.assertEquals(1, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 2);

        Field f = cr.getFields().get(1);
        Assert.assertEquals(1, f.getQualityFields().size());

        Field qualityField = f.getQualityFields().get(0);
        Assert.assertEquals("new_quality_name", qualityField.getName());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 2);
    }

    @Test
    public void csvFlatStoreDiffLengthTest() throws Exception {

        String sensorId = "urn:diff-length:1";

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(diffLengthFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue(sensorId);

        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());


        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));
        Assert.assertTrue(phenomenonNames.contains("SALINITY"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        Assert.assertEquals(1, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 3);

        String expectedValues = "1980-03-01T21:52:00.0,122.6,13.4@@" +
                               "1980-03-02T21:52:00.0,,14.1@@" +
                               "1980-03-03T21:52:00.0,,13.1@@";
        Assert.assertEquals(expectedValues, cr.getValues());


        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 3);
    }

    @Test
    public void csvFlatStoreDiffLengthTest2() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(diffLengthFile2.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_COLUMN.getName().getCode()).setValue("PROC");

        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(2, procedureNames.size());

        Assert.assertTrue(procedureNames.contains("pdl1"));
        Assert.assertTrue(procedureNames.contains("pdl2"));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));
        Assert.assertTrue(phenomenonNames.contains("SALINITY"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, "pdl1");
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));

        timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, "pdl2");
        time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-04T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(2, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals("pdl1", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        Assert.assertEquals(2, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 3);

        String expectedValues = "1980-03-01T21:52:00.0,122.6,13.4@@" +
                               "1980-03-02T21:52:00.0,,14.1@@" +
                               "1980-03-03T21:52:00.0,,13.1@@";
        Assert.assertEquals(expectedValues, cr.getValues());

        obs = results.observations.get(1);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        cr = (ComplexResult) obs.getResult();

        Assert.assertEquals(3, cr.getFields().size());

        expectedValues = "1980-03-01T21:52:00.0,126.2,12.1@@" +
                         "1980-03-02T21:52:00.0,,11.2@@" +
                         "1980-03-03T21:52:00.0,,15.1@@" +
                         "1980-03-04T21:52:00.0,123.2,@@";
        Assert.assertEquals(expectedValues, cr.getValues());


        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(2, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("pdl1", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 3);

        proc = procedures.get(1);
        Assert.assertEquals("pdl2", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-04T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 3);
    }


    @Test
    public void csvFlatStoreIncompleteLine() throws Exception {

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(incompLineFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_COLUMN.getName().getCode()).setValue("PLAT");

        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(2, procedureNames.size());

        Assert.assertTrue(procedureNames.contains("P1"));
        Assert.assertTrue(procedureNames.contains("P2"));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));
        Assert.assertTrue(phenomenonNames.contains("SALINITY"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, "P1");
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));

        timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, "P2");
        time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(2, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals("P1", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        Assert.assertEquals(2, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 2);

        String expectedValues = "1980-03-01T21:52:00.0,13.4@@" +
                                "1980-03-02T21:52:00.0,14.1@@";
        Assert.assertEquals(expectedValues, cr.getValues());

        obs = results.observations.get(1);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        cr = (ComplexResult) obs.getResult();

        Assert.assertEquals(3, cr.getFields().size());

        expectedValues = "1980-03-01T21:52:00.0,122.6,@@" +
                         "1980-03-03T21:52:00.0,,13.1@@";
        Assert.assertEquals(expectedValues, cr.getValues());


        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(2, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals("P1", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 2);

        proc = procedures.get(1);
        Assert.assertEquals("P2", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 3);
    }
    
    @Test
    public void csvFlatStoreRegexMatchOPTest() throws Exception {

        String sensorId = "urn:reg-match-op:1";

        CsvFlatObservationStoreFactory factory = new CsvFlatObservationStoreFactory();
        ParameterValueGroup params = factory.getOpenParameters().createValue();
        params.parameter(CsvFlatObservationStoreFactory.LOCATION).setValue(regexMatchingObsPropFile.toUri().toString());

        params.parameter(CsvFlatObservationStoreFactory.DATE_COLUMN.getName().getCode()).setValue("TIME");
        params.parameter(CsvFlatObservationStoreFactory.MAIN_COLUMN.getName().getCode()).setValue("TIME");

        params.parameter(CsvFlatObservationStoreFactory.DATE_FORMAT.getName().getCode()).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        params.parameter(CsvFlatObservationStoreFactory.LATITUDE_COLUMN.getName().getCode()).setValue("LAT");
        params.parameter(CsvFlatObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode()).setValue("LON");

        params.parameter(CsvFlatObservationStoreFactory.FILE_MIME_TYPE.getName().getCode()).setValue("csv");

        params.parameter(CsvFlatObservationStoreFactory.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");
        params.parameter(CsvFlatObservationStoreFactory.OBS_PROP_REGEX.getName().getCode()).setValue("([\\w\\s]+)");
        
        params.parameter(CsvFlatObservationStoreFactory.UOM_COLUMN.getName().getCode()).setValue("UOM");
        params.parameter(CsvFlatObservationStoreFactory.UOM_REGEX.getName().getCode()).setValue("\\(([^\\)]+)\\)?");

        params.parameter(CsvFlatObservationStoreFactory.OBSERVATION_TYPE.getName().getCode()).setValue("Timeserie");
        params.parameter(CsvFlatObservationStoreFactory.PROCEDURE_ID.getName().getCode()).setValue(sensorId);

        params.parameter(CSVProvider.SEPARATOR.getName().getCode()).setValue(Character.valueOf(';'));

        CsvFlatObservationStore store = factory.open(params);

        Set<String> procedureNames = store.getEntityNames(new ProcedureQuery());
        Assert.assertEquals(1, procedureNames.size());


        Assert.assertTrue(procedureNames.contains(sensorId));

        Set<String> phenomenonNames = store.getEntityNames(new ObservedPropertyQuery());
        Assert.assertTrue(phenomenonNames.contains("TEMPERATURE"));
        Assert.assertTrue(phenomenonNames.contains("SALINITY"));

        IdentifierQuery timeQuery = new IdentifierQuery(OMEntity.PROCEDURE, sensorId);
        TemporalPrimitive time = store.getEntityTemporalBounds(timeQuery);

        Assert.assertTrue(time instanceof Period);

        Period tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));

        ObservationDataset results = store.getDataset(new DatasetQuery());
        Assert.assertEquals(1, results.procedures.size());
        ProcedureDataset proc = results.procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        Assert.assertEquals(1, results.observations.size());
        Observation obs = results.observations.get(0);
        Assert.assertTrue(obs.getResult() instanceof ComplexResult);
        ComplexResult cr = (ComplexResult) obs.getResult();

        verifyTSFields(cr, 3);
        
        Field f = cr.getFields().get(1);
        Assert.assertEquals("psu", f.getUom());

        f = cr.getFields().get(2);
        Assert.assertEquals("°C", f.getUom());
        

        String expectedValues = "1980-03-01T21:52:00.0,122.6,13.4@@" +
                                "1980-03-02T21:52:00.0,,14.1@@" +
                                "1980-03-03T21:52:00.0,,13.1@@";
        Assert.assertEquals(expectedValues, cr.getValues());

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());

        Assert.assertEquals(1, procedures.size());
        proc = procedures.get(0);
        Assert.assertEquals(sensorId, proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));
        
        verifyTSFields(proc, 3);
    }
}
