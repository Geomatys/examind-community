/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2023 Geomatys.
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
package com.examind.process.sos;

import static com.examind.process.sos.AbstractSosHarvesterTest.ORIGIN_NB_SENSOR;
import static com.examind.process.sos.AbstractSosHarvesterTest.errorHeaderDirectory;
import static com.examind.process.sos.AbstractSosHarvesterTest.errorHeaderDirectory2;
import static com.examind.process.sos.AbstractSosHarvesterTest.errorHeaderDirectory_1;
import static com.examind.process.sos.SosHarvesterTestUtils.getNbOffering;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.logging.Level;
import org.constellation.business.IDataBusiness;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.sos.core.SOSworker;
import org.constellation.test.utils.TestEnvironment;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author guilhem
 */
public class SOSharvesterProcessCheckTest extends AbstractSosHarvesterTest {

    private static boolean initialized = false;

    private static Integer FEATURE_DATA_ID = null;

    @PostConstruct
    public void setUpClass2() {
        if (!initialized) {
            try {
                final List<TestEnvironment.DataImport> datas = testResources.createProviders(TestEnvironment.TestResource.WMS111_SHAPEFILES, providerBusiness, null).datas();
                for (TestEnvironment.DataImport data : datas) {
                    if (data.name.equals("Bridges")) {
                        FEATURE_DATA_ID = data.id;
                    }
                }
                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "error while initializing test", ex);
            }
        }
    }

    @Test
    public void harvestCSVSingleErrorHeaderTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:er";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory_1.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.SSS");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("lat");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("lon");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("temperature");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("salinity");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);

        // bad separator
        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
       
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ProcessException error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        // i don't know why the message is prefixed with the type of the exception
        Assert.assertEquals("""
                            /error-header.csv:
                            Unable to find main column(s): [time]
                            KO.
                            
                            """, error.getMessage());

    }

    @Test
    public void harvestCSVMultiErrorHeaderTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:er";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.SSS");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("lat");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("lon");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("temperature");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("salinity");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);

        // bad separator
        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);

        /*
         * two files (with same name)
        */
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory.toUri().toString());

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        Exception error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        Assert.assertEquals("""
                            All the files insertion failed:
                            /error-head-dir1/error-header.csv:
                            Unable to find main column(s): [time]
                            KO.

                            /error-head-dir2/error-header.csv:
                            Unable to find main column(s): [time]
                            KO.

                            """, error.getMessage());

        /*
        * two files but one pass
        */
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorHeaderDirectory2.toUri().toString());
        proc = desc.createProcess(in);

        ParameterValueGroup results = proc.call();
        String insertedFile = results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_NAME).stringValue();
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        String errorFile = results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_NAME).stringValue();
        int nbError = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_COUNT_NAME).getValue();
        String checkReport = results.parameter(SosHarvesterProcessDescriptor.CHECK_REPORT_NAME).stringValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(1, nbError);
        Assert.assertEquals("/error-header.csv", errorFile);
        Assert.assertEquals("/error-header-2.csv", insertedFile);
        Assert.assertEquals("""
                           /error-header-2.csv:
                           OK.

                           /error-header.csv:
                           Unable to find main column(s): [time]
                           KO.
                            
                            """, checkReport);

    }

    @Test
    public void harvestCSVbadUOMConvertTest() throws Exception {
        
        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:bad-uom-convert";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorUnitConvertFile1.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);
        
        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");
        in.parameter(SosHarvesterProcessDescriptor.UOM_COLUMN.getName().getCode()).setValue("UNIT");

        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);

        // first file insertion will set the field unit to '°C'
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        ParameterValueGroup results = proc.call();

        String insertedFile = results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_NAME).stringValue();
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals("/unit-convert-error-1.csv", insertedFile);

         // second file insertion will set try to insert in the field with unit 'm'
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorUnitConvertFile2.toUri().toString());

        proc = desc.createProcess(in);
        Exception error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        Assert.assertEquals("""
                            /unit-convert-error-2.csv:
                            [ERROR] unconvertible Unit Of Measure:
                             - Sensor urn:sensor:bad-uom-convert
                            	 - m => °C for property: TEMPERATURE
                            KO.
                            
                            """,
                           error.getMessage());
    }

    @Test
    public void harvestCSVbadUOMConvertTest2() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:bad-uom-convert2";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorUnitConvertDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");
        in.parameter(SosHarvesterProcessDescriptor.UOM_COLUMN.getName().getCode()).setValue("UNIT");

        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);

        // first file insertion will set the field unit to '°C'
        // the second will fail because of the field with is 'm'
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        ParameterValueGroup results = proc.call();

        String insertedFile = results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_NAME).stringValue();
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals("/unit-convert-error-1.csv", insertedFile);

        insertedFile = results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_NAME).stringValue();
        nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        String errorFile = results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_NAME).stringValue();
        int nbError = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_COUNT_NAME).getValue();
        String checkReport = results.parameter(SosHarvesterProcessDescriptor.CHECK_REPORT_NAME).stringValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(1, nbError);
        Assert.assertEquals("/unit-convert-error-2.csv", errorFile);
        Assert.assertEquals("/unit-convert-error-1.csv", insertedFile);

        Assert.assertEquals("""
                            /unit-convert-error-1.csv:
                            OK.

                            /unit-convert-error-2.csv:
                            [ERROR] unconvertible Unit Of Measure:
                             - Sensor urn:sensor:bad-uom-convert2
                            	 - m => °C for property: TEMPERATURE
                            KO.

                            """, checkReport);

    }

    @Test
    public void harvestCSVbadObservedPropertiesColumnTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:bad-obs-prop-column";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(errorUnitConvertFile1.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN.getName().getCode()).setValue("BADCOL");
        in.parameter(SosHarvesterProcessDescriptor.UOM_COLUMN.getName().getCode()).setValue("UNIT");

        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);

        // fail because of the missing column 'BADCOL'

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        Exception error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        Assert.assertEquals("""
                           /unit-convert-error-1.csv:
                           File headers is missing observed properties columns: BADCOL
                           KO.
                           
                           """,
                           error.getMessage());
    }

    @Test
    public void harvestCSVFlatWarningTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:warn:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(warningUomDirectory.toUri().toString());

         in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");
        in.parameter(SosHarvesterProcessDescriptor.UOM_COLUMN.getName().getCode()).setValue("UNIT");

        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);
      
        org.geotoolkit.process.Process proc = desc.createProcess(in);

        ParameterValueGroup results = proc.call();
        String insertedFile = results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_NAME).stringValue();
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        int nbError = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ERROR_COUNT_NAME).getValue();
        String checkReport = results.parameter(SosHarvesterProcessDescriptor.CHECK_REPORT_NAME).stringValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(0, nbError);
        Assert.assertEquals("/warning-uom.csv", insertedFile);
        Assert.assertEquals("""
                            /warning-uom.csv:
                            [WARNING] unparseable Unit Of Measure:
                             - baduom
                            OK.
                            
                            """, checkReport);

    }

    @Test
    public void harvestCSVFlatNoLineTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:no-line:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(noLineDirectory.toUri().toString());

         in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.CHECK_FILE_NAME).setValue(true);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN.getName().getCode()).setValue("RESULT");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN.getName().getCode()).setValue("PROPERTY");
        in.parameter(SosHarvesterProcessDescriptor.UOM_COLUMN.getName().getCode()).setValue("UNIT");

        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.REMOTE_READ_NAME).setValue(true);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        Exception error = null;
        try {
            proc.call();
        } catch (ProcessException ex) {
            error = ex;
        }
        Assert.assertNotNull(error);
        Assert.assertEquals("""
                           /no-valid-lines.csv:
                           The data provider did not produce any observations.
                           KO.
                           
                           """,
                           error.getMessage());
    }

    @Test
    public void directCheckerTest() throws Exception {

        SosHarvestFileChecker checker = new SosHarvestFileChecker();

        // unexisting data
        checker.checkFile("unexisting-file.txt", -1);
        Assert.assertNotNull(checker.error);
        Assert.assertEquals("""
                           unexisting-file.txt:
                           No Data for id -1
                           KO.
                            
                           """,
                            checker.report.toString());

        // feature data
        checker = new SosHarvestFileChecker();
        checker.checkFile("Bridges.shp", FEATURE_DATA_ID);
        Assert.assertNotNull(checker.error);
        Assert.assertEquals("""
                           Bridges.shp:
                           The file is not supported by csv observation store
                           KO.

                           """,
                            checker.report.toString());

    }
}
