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
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.observation.model.ComplexResult;
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

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(1, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
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

        List<ProcedureDataset> procedures = store.getProcedureDatasets(new DatasetQuery());
        Assert.assertEquals(1, procedures.size());

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(1, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
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

        ProcedureDataset pt = procedures.get(0);
        Assert.assertEquals(1, pt.spatialBound.getHistoricalLocations().size());

        time = pt.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-02T21:52:00" , format(tp.getEnding()));
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

        Assert.assertEquals(2, cr.getFields().size());

        Field f = cr.getFields().get(1);
        Assert.assertEquals(1, f.qualityFields.size());

        Field qualityField = f.qualityFields.get(0);
        Assert.assertEquals("qua_lity_fi", qualityField.name);

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

        Assert.assertEquals(2, cr.getFields().size());
        
        
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

        Assert.assertEquals(2, cr.getFields().size());

        Field f = cr.getFields().get(1);
        Assert.assertEquals(1, f.qualityFields.size());

        Field qualityField = f.qualityFields.get(0);
        Assert.assertEquals("new_quality_name", qualityField.name);

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

        Assert.assertEquals(3, cr.getFields().size());

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

        Assert.assertEquals(3, cr.getFields().size());

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

        proc = procedures.get(1);
        Assert.assertEquals("pdl2", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-04T21:52:00" , format(tp.getEnding()));
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

        Assert.assertEquals(2, cr.getFields().size());

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

        proc = procedures.get(1);
        Assert.assertEquals("P2", proc.getId());
        Assert.assertEquals(1, proc.spatialBound.getHistoricalLocations().size());

        time = proc.spatialBound.getTimeObject();
        Assert.assertTrue(time instanceof Period);

        tp = (Period) time;
        Assert.assertEquals("1980-03-01T21:52:00" , format(tp.getBeginning()));
        Assert.assertEquals("1980-03-03T21:52:00" , format(tp.getEnding()));
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

        Assert.assertEquals(3, cr.getFields().size());
        
        Field f = cr.getFields().get(1);
        Assert.assertEquals("psu", f.uom);

        f = cr.getFields().get(2);
        Assert.assertEquals("°C", f.uom);
        

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
    }
}
