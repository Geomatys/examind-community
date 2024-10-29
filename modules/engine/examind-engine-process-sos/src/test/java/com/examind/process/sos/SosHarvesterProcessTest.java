/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2019 Geomatys.
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

import com.examind.sts.core.STSWorker;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.constellation.dto.Sensor;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.sos.core.SOSworker;
import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import static com.examind.process.sos.SosHarvesterTestUtils.*;
import java.util.Set;
import org.constellation.process.ProcessUtils;
import org.geotoolkit.gml.xml.GMLInstant;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.v200.GetObservationResponseType;
import org.geotoolkit.sos.xml.v200.GetObservationType;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.MultiDatastream;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.UnitOfMeasure;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.swe.xml.v200.DataArrayPropertyType;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SosHarvesterProcessTest extends AbstractSosHarvesterTest {

    @Test
    public void harvestCSVProfileTest() throws Exception {
        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(argoDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE (YYYY-MM-DDTHH:MI:SSZ)");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("PRES (decibar)");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
        in.parameter(SosHarvesterProcessDescriptor.FOI_COLUMN_NAME).setValue("CONFIG_MISSION_NUMBER");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMP (degree_Celsius)");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("PSAL (psu)");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Profile");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        
        ParameterValue scval1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval1.setValue(new ServiceProcessReference(sc));
        in.values().add(scval1);
        ParameterValue scval2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval2.setValue(new ServiceProcessReference(sc2));
        in.values().add(scval2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(1, insertedFiles.size());
        Assert.assertEquals("/argo-profiles-2902402-1.csv", insertedFiles.get(0));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        Thing t = getThing(stsWorker, sensorId);
        Assert.assertNotNull(t);
        Assert.assertEquals(sensorId, t.getName());
        Assert.assertEquals("", t.getDescription());

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-13T03:55:49.000", time.getEndPosition().getValue());

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        // composite + components
        Assert.assertEquals(4, offp.getObservedProperties().size());

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(4, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-11-02T07:10:52Z", -6.81, 44.06);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1609, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP (degree_Celsius)", "PSAL (psu)", "PRES (decibar)"));

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        verifySamplingFeature(fois,  "251",  -6.81,  44.06);
        verifySamplingFeature(fois,  "252",  -6.581, 44.01);
        verifySamplingFeature(fois,  "253",  -6.256, 43.959);
        verifySamplingFeature(fois,  "254",  -6.035, 44.031);

        /*
         * add a new file to integrate and call again the process
         */
        writeResourceDataFile(argoDirectory, "com/examind/process/sos/argo-profiles-2902402-2.csv", "argo-profiles-2902402-2.csv");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        proc = desc.createProcess(in);
        results = proc.call();

        nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        List<String> alreadyInsertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_NAME);
        int nbAlreadyInserted= (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbAlreadyInserted);
        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(1, insertedFiles.size());
        Assert.assertEquals("/argo-profiles-2902402-2.csv", insertedFiles.get(0));
        Assert.assertEquals(1, alreadyInsertedFiles.size());
        Assert.assertEquals("/argo-profiles-2902402-1.csv", alreadyInsertedFiles.get(0));

        offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);
        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-27T15:09:17.000", time.getEndPosition().getValue());

        Assert.assertEquals(8, offp.getFeatureOfInterestIds().size());
        Assert.assertEquals(4, offp.getObservedProperties().size());

        ObservedProperty obsProp1 = getObservedPropertyById(stsWorker, "PSAL (psu)");
        Assert.assertNotNull(obsProp1);
        Assert.assertEquals("PSAL (psu)", obsProp1.getIotId());
        Assert.assertEquals("PSAL (psu)", obsProp1.getName());
        Assert.assertEquals("", obsProp1.getDescription());

        ObservedProperty obsProp2 = getObservedPropertyById(stsWorker, "TEMP (degree_Celsius)");
        Assert.assertNotNull(obsProp2);
        Assert.assertEquals("TEMP (degree_Celsius)", obsProp2.getIotId());
        Assert.assertEquals("TEMP (degree_Celsius)", obsProp2.getName());
        Assert.assertEquals("", obsProp2.getDescription());

        String observedProperty = "PSAL (psu)";
        String foi = "251";

        /*
        * Verify an inserted profile
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/argo-datablock-values.txt").replace("\n", "") + "\n";;
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(8, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-11-02T07:10:52Z", -6.81, 44.06);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2018-11-05T22:03:31Z", -6.581, 44.01);

        HistoricalLocation loc8 = response.getValue().get(7);
        verifyHistoricalLocation(loc8, "2018-11-27T15:09:17Z", -5.04, 44.154);

        nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(3209, nbMeasure);

        /*
         * remove the new file and reinsert with REMOVE PREVIOUS and uom/obsprop regex extraction
         */
        Path p = argoDirectory.resolve("argo-profiles-2902402-2.csv");
        Files.delete(p);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.UOM_REGEX_NAME).setValue("\\(([^\\)]+)\\)?");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_REGEX_NAME).setValue("([\\w\\s]+)");

        proc = desc.createProcess(in);
        results = proc.call();

        insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        
        Assert.assertEquals(1, nbInserted);
        Assert.assertEquals(1, insertedFiles.size());
        Assert.assertEquals("/argo-profiles-2902402-1.csv", insertedFiles.get(0));

        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-13T03:55:49.000", time.getEndPosition().getValue());

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(4, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-11-02T07:10:52Z", -6.81, 44.06);

        nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1609, nbMeasure);

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        verifySamplingFeature(fois,  "251",  -6.81,  44.06);
        verifySamplingFeature(fois,  "252",  -6.581, 44.01);
        verifySamplingFeature(fois,  "253",  -6.256, 43.959);
        verifySamplingFeature(fois,  "254",  -6.035, 44.031);

        obsProp1 = getObservedPropertyById(stsWorker, "PSAL");
        Assert.assertNotNull(obsProp1);
        Assert.assertEquals("PSAL", obsProp1.getIotId());
        Assert.assertEquals("PSAL", obsProp1.getName());
        Assert.assertEquals("", obsProp1.getDescription());

        obsProp2 = getObservedPropertyById(stsWorker, "TEMP");
        Assert.assertNotNull(obsProp2);
        Assert.assertEquals("TEMP", obsProp2.getIotId());
        Assert.assertEquals("TEMP", obsProp2.getName());
        Assert.assertEquals("", obsProp2.getDescription());

        MultiDatastream mds = stsWorker.getMultiDatastreamById(new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:" + sensorId));
        Assert.assertNotNull(mds);

        assertTrue(mds.getUnitOfMeasurement() instanceof List);

        List listUom = (List) mds.getUnitOfMeasurement();

        Assert.assertEquals(3, listUom.size());

        List<String> expectedUoms = Arrays.asList("decibar", "psu", "degree_Celsius");
        for (Object uomObj : listUom) {
            assertTrue(uomObj instanceof UnitOfMeasure);
            UnitOfMeasure uom = (UnitOfMeasure) uomObj;
            String uomName = uom.getName();
            Assert.assertNotNull(uomName);
            Assert.assertTrue("unexpected uom:" + uomName, expectedUoms.contains(uomName));
        }
    }

    @Test
    public void harvestCSVProfileNoFOITest() throws Exception {
        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(argoDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE (YYYY-MM-DDTHH:MI:SSZ)");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("PRES (decibar)");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
        in.parameter(SosHarvesterProcessDescriptor.FOI_COLUMN_NAME).setValue("CONFIG_MISSION_NUMBER");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMP (degree_Celsius)");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("PSAL (psu)");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Profile");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);

        in.parameter(SosHarvesterProcessDescriptor.GENERATE_FOI_NAME).setValue(false);

        ParameterValue scval1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval1.setValue(new ServiceProcessReference(sc));
        in.values().add(scval1);
        ParameterValue scval2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval2.setValue(new ServiceProcessReference(sc2));
        in.values().add(scval2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        Thing t = getThing(stsWorker, sensorId);
        Assert.assertNotNull(t);
        Assert.assertEquals(sensorId, t.getName());
        Assert.assertEquals("", t.getDescription());

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-13T03:55:49.000", time.getEndPosition().getValue());

        Assert.assertTrue(offp.getFeatureOfInterestIds().isEmpty());

        // composite + components
        Assert.assertEquals(4, offp.getObservedProperties().size());

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(4, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-11-02T07:10:52Z", -6.81, 44.06);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1609, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP (degree_Celsius)", "PSAL (psu)", "PRES (decibar)"));

        /*
         * add a new file to integrate and call again the process
         */
        writeResourceDataFile(argoDirectory, "com/examind/process/sos/argo-profiles-2902402-2.csv", "argo-profiles-2902402-2.csv");
        proc.call();

        offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);
        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-27T15:09:17.000", time.getEndPosition().getValue());

        Assert.assertTrue(offp.getFeatureOfInterestIds().isEmpty());
        Assert.assertEquals(4, offp.getObservedProperties().size());

        ObservedProperty obsProp1 = getObservedPropertyById(stsWorker, "PSAL (psu)");
        Assert.assertNotNull(obsProp1);
        Assert.assertEquals("PSAL (psu)", obsProp1.getIotId());
        Assert.assertEquals("PSAL (psu)", obsProp1.getName());
        Assert.assertEquals("", obsProp1.getDescription());

        ObservedProperty obsProp2 = getObservedPropertyById(stsWorker, "TEMP (degree_Celsius)");
        Assert.assertNotNull(obsProp2);
        Assert.assertEquals("TEMP (degree_Celsius)", obsProp2.getIotId());
        Assert.assertEquals("TEMP (degree_Celsius)", obsProp2.getName());
        Assert.assertEquals("", obsProp2.getDescription());

        String observedProperty = "PSAL (psu)";

        /*
        * Verify inserted profiles (all at once)
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        String expectedResult = getResourceAsString("com/examind/process/sos/argo-datablock-values-no-foi.txt").replace("\n", "") + "\n";
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(8, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-11-02T07:10:52Z", -6.81, 44.06);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2018-11-05T22:03:31Z", -6.581, 44.01);

        HistoricalLocation loc8 = response.getValue().get(7);
        verifyHistoricalLocation(loc8, "2018-11-27T15:09:17Z", -5.04, 44.154);

        nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(3209, nbMeasure);

        /*
         * remove the new file and reinsert with REMOVE PREVIOUS and uom/obsprop regex extraction
         */
        Path p = argoDirectory.resolve("argo-profiles-2902402-2.csv");
        Files.delete(p);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.UOM_REGEX_NAME).setValue("\\(([^\\)]+)\\)?");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_REGEX_NAME).setValue("([\\w\\s]+)");

        proc = desc.createProcess(in);
        proc.call();

        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-13T03:55:49.000", time.getEndPosition().getValue());

        Assert.assertTrue(offp.getFeatureOfInterestIds().isEmpty());

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(4, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-11-02T07:10:52Z", -6.81, 44.06);

        nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1609, nbMeasure);

        obsProp1 = getObservedPropertyById(stsWorker, "PSAL");
        Assert.assertNotNull(obsProp1);
        Assert.assertEquals("PSAL", obsProp1.getIotId());
        Assert.assertEquals("PSAL", obsProp1.getName());
        Assert.assertEquals("", obsProp1.getDescription());

        obsProp2 = getObservedPropertyById(stsWorker, "TEMP");
        Assert.assertNotNull(obsProp2);
        Assert.assertEquals("TEMP", obsProp2.getIotId());
        Assert.assertEquals("TEMP", obsProp2.getName());
        Assert.assertEquals("", obsProp2.getDescription());

        MultiDatastream mds = stsWorker.getMultiDatastreamById(new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:" + sensorId));
        Assert.assertNotNull(mds);

        assertTrue(mds.getUnitOfMeasurement() instanceof List);

        List listUom = (List) mds.getUnitOfMeasurement();

        Assert.assertEquals(3, listUom.size());

        List<String> expectedUoms = Arrays.asList("decibar", "psu", "degree_Celsius");
        for (Object uomObj : listUom) {
            assertTrue(uomObj instanceof UnitOfMeasure);
            UnitOfMeasure uom = (UnitOfMeasure) uomObj;
            String uomName = uom.getName();
            Assert.assertNotNull(uomName);
            Assert.assertTrue("unexpected uom:" + uomName, expectedUoms.contains(uomName));
        }
    }

    @Test
    public void harvestCSVTrajTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:2";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(fmlwDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("PSAL LEVEL1 (psu)");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Trajectory");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        val1.setValue(new ServiceProcessReference(sc));
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        val2.setValue(new ServiceProcessReference(sc2));
        in.values().add(val2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(3, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/tsg-FMLW-1.csv"));
        Assert.assertTrue(insertedFiles.contains("/tsg-FMLW-2.csv"));
        Assert.assertTrue(insertedFiles.contains("/tsg-FMLW-3.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-10-30", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-10-31T06:42:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(3, offp.getFeatureOfInterestIds().size());

        String observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("PSAL LEVEL1 (psu)", observedProperty);

        /*
        * Verify inserted results
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        String expectedResult =  getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);


        String resultForFoi1 = getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values-1.txt");
        String resultForFoi2 = getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values-2.txt");
        String resultForFoi3 = getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values-3.txt");

        String foi = offp.getFeatureOfInterestIds().get(0);
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        Assert.assertTrue(result.equals(resultForFoi1) ||  result.equals(resultForFoi2) || result.equals(resultForFoi3));

        foi = offp.getFeatureOfInterestIds().get(1);
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        Assert.assertTrue(result.equals(resultForFoi1) ||  result.equals(resultForFoi2) || result.equals(resultForFoi3));

        foi = offp.getFeatureOfInterestIds().get(2);
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        Assert.assertTrue(result.equals(resultForFoi1) ||  result.equals(resultForFoi2) || result.equals(resultForFoi3));


        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        hl.setCount(true);
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1236, response.getIotCount().intValue());

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1236, nbMeasure);
    }

    @Test
    public void harvestCSVTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:3";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(mooDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMP LEVEL0 (degree_Celsius)");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("VEPK LEVEL0 (meter2 second)");
        in.values().add(val2);


        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.THING_DESC_COLUMN_NAME).setValue("PLATFORM_DESC");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sc2));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/mooring-buoys-time-series-62069.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorId);
        Assert.assertNotNull(sensor);
        Assert.assertEquals(sensorId, sensor.getIdentifier());
        Assert.assertEquals(sensorId, sensor.getName());
        Assert.assertEquals("some description", sensor.getDescription());

        Thing t = getThing(stsWorker, sensorId);
        Assert.assertNotNull(t);
        Assert.assertEquals(sensorId, t.getName());
        Assert.assertEquals("some description", t.getDescription());

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-10-30T00:29:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-30T11:59:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        verifySamplingFeature(fois,  -4.9683, 48.2903);

        Assert.assertEquals(3, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);
        String foi = offp.getFeatureOfInterestIds().get(0);


        /*
        * Verify an inserted profile
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());
        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2018-10-30T00:29:00Z", -4.9683,  48.2903);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1509, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP LEVEL0 (degree_Celsius)", "VEPK LEVEL0 (meter2 second)"));
    }

    @Test
    public void harvestTS2Test() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:5";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(mooDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMP LEVEL0 (degree_Celsius)");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("VZMX LEVEL0 (meter)");
        in.values().add(val2);


        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/mooring-buoys-time-series-62069.csv"));


        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-10-30", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-30T12:30:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeature(fois,  -4.9683, 48.2903);

        Assert.assertEquals(3, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);
        
        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP LEVEL0 (degree_Celsius)", "VZMX LEVEL0 (meter)"));

        /*
        * Verify an inserted timeSeries
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, result);

        /*
        * Verify an inserted timeSeries
        */
        observedProperty = "VZMX LEVEL0 (meter)";
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values-3.txt");
        Assert.assertEquals(expectedResult, result);

        Object obj = sosWorker.getObservation(new GetObservationType("2.0.0", offp.getId(), null, Arrays.asList(sensorId), Arrays.asList(observedProperty), new ArrayList<>(), null));

        Assert.assertTrue(obj instanceof GetObservationResponseType);

        GetObservationResponseType goResponse = (GetObservationResponseType) obj;

        Assert.assertTrue(goResponse.getObservationData().get(0).getOMObservation().getResult() instanceof DataArrayPropertyType);

        DataArrayPropertyType daResult = (DataArrayPropertyType) goResponse.getObservationData().get(0).getOMObservation().getResult();

        String value = daResult.getDataArray().getValues();
        Assert.assertEquals(expectedResult, value + '\n');

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(3020, nbMeasure);
    }

    @Test
    public void harvestDBFTSTest() throws Exception {
        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:dbf:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(ltDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationDbfFile");
        in.parameter(SosHarvesterProcessDescriptor.FORMAT_NAME).setValue("application/dbase; subtype=\"om\"");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME_STR");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME_STR");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd' 'HH:mm:ss");

        in.parameter(SosHarvesterProcessDescriptor.FOI_COLUMN_NAME).setValue("PRIOR_ID");

        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("HEIGHT");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sc2));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(2, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/LakeTile_001.dbf"));
        Assert.assertTrue(insertedFiles.contains("/LakeTile_002.dbf"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2022-08-20T01:55:11.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2022-08-28T10:58:39.000", time.getEndPosition().getValue());

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());
        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());

        Assert.assertEquals(1, offp.getObservedProperties().size());
        String observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("HEIGHT", observedProperty);

        /*
         * Verify an inserted time serie
         */
        String foi = verifySamplingFeature(fois, "54008001708");
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/LakeTile_foi-1.txt");
        Assert.assertEquals(expectedResult, result);

        foi = verifySamplingFeature(fois,        "54008001586");
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/LakeTile_foi-2.txt");
        Assert.assertEquals(expectedResult, result);

        verifySamplingFeature(fois,        "54008001453");
        verifySamplingFeature(fois,        "54008001446");

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9, nbMeasure);
    }

    @Test
    public void harvestDBFTS2Test() throws Exception {
         SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:dbf:2";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(rtDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationDbfFile");
        in.parameter(SosHarvesterProcessDescriptor.FORMAT_NAME).setValue("application/dbase; subtype=\"om\"");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("latitude");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("longitude");

        in.parameter(SosHarvesterProcessDescriptor.FOI_COLUMN_NAME).setValue("node_id");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("height");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("width");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(2, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/rivertile_001.dbf"));
        Assert.assertTrue(insertedFiles.contains("/rivertile_002.dbf"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2022-06-06T00:58:50.921", time.getBeginPosition().getValue());
        Assert.assertEquals("2022-06-15T23:21:00.641", time.getEndPosition().getValue());

        Assert.assertEquals(64, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());

        // composite + components
        Assert.assertEquals(3, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("height", "width"));

        /*
        * Verify an inserted time serie
        */
        String foi = verifySamplingFeature(fois, "8403780.0", 2.074899266154643, 45.22470466446091);
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/rivertile_foi-1.txt");
        Assert.assertEquals(expectedResult, result);

        foi = verifySamplingFeature(fois, "8403781.0", 2.07361239284379, 45.224199842811814);
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/rivertile_foi-2.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(85, nbMeasure);

    }

    @Test
    public void harvestCSVTSMultiPlatformTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(multiPlatDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("DATE");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMP (degree_Celsius)");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("VEPK (meter2 second)");
        in.values().add(val2);


        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("PLATFORM");
        in.parameter(SosHarvesterProcessDescriptor.THING_REGEX_NAME).setValue("(^[^/]*)");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        
        ParameterValue scval1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval1.setValue(new ServiceProcessReference(sc));
        in.values().add(scval1);
        ParameterValue scval2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval2.setValue(new ServiceProcessReference(sc2));
        in.values().add(scval2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(2, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/multiplatform-1.csv"));
        Assert.assertTrue(insertedFiles.contains("/multiplatform-2.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("p001"));
        Assert.assertNotNull(sensorBusiness.getSensor("p002"));
        Assert.assertNotNull(sensorBusiness.getSensor("p003"));

         // verify that the sensor is linked to the services
        Assert.assertTrue(sensorBusiness.isLinkedSensor(sc.getId(), "p001"));
        Assert.assertTrue(sensorBusiness.isLinkedSensor(sc2.getId(), "p001"));

        verifyAllObservedProperties(stsWorker, "p001", Arrays.asList("TEMP (degree_Celsius)", "VEPK (meter2 second)"));
        verifyAllObservedProperties(stsWorker, "p002", Arrays.asList("TEMP (degree_Celsius)", "VEPK (meter2 second)"));
        verifyAllObservedProperties(stsWorker, "p003", Arrays.asList("TEMP (degree_Celsius)", "VEPK (meter2 second)"));

        /*
        * first extracted procedure
        */

        ObservationOffering offp = getOffering(sosWorker, "p001");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2000-07-28T00:30:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2000-07-29T23:30:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeatureLine(fois, 2);

        Assert.assertEquals(3, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        /*
        * Verify an inserted data
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/multi-platform-values-p001.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "p001");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(2, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2000-07-28T00:30:00Z", -6.9, 49.4);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2000-07-29T23:00:00Z", -6.8, 49.5);

        int nbMeasure = getNbMeasure(stsWorker, "p001");
        Assert.assertEquals(95, nbMeasure);

        /*
        * second extracted procedure
        */

        offp = getOffering(sosWorker, "p002");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2000-07-30", time.getBeginPosition().getValue());
        Assert.assertEquals("2000-07-31T10:30:00.000", time.getEndPosition().getValue());

        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        foi = verifySamplingFeature(fois, -6.9, 49.4);

        Assert.assertEquals(3, offp.getObservedProperties().size());
        observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        /*
        * Verify an inserted data
        */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/multi-platform-values-p002.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "p002");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(2, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2000-07-30T00:00:00Z", -6.9, 49.4);

        loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2000-07-30T02:30:00Z", -6.9, 49.4);

        nbMeasure = getNbMeasure(stsWorker, "p002");
        Assert.assertEquals(70, nbMeasure);

       /*
        * third extracted procedure
        */

        offp = getOffering(sosWorker, "p003");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2000-07-31T11:00:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2000-08-01T04:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeature(fois, -5.3, 51.2);

        Assert.assertEquals(3, offp.getObservedProperties().size());
        observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        /*
        * Verify an inserted data
        */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/multi-platform-values-p003.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "p003");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2000-07-31T11:00:00Z", -5.3, 51.2);

        nbMeasure = getNbMeasure(stsWorker, "p003");
        Assert.assertEquals(35, nbMeasure);

    }

    @Test
    public void harvesterCSVFlatProfileSingleTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        // ???
        String sensorId = "urn:sensor:bgdata";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(bigdataDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("station_date");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("z_value");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("latitude");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("longitude");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val1.setValue("30");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val2.setValue("35");
        in.values().add(val2);
        ParameterValue val3 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val3.setValue("66");
        in.values().add(val3);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("parameter_value");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("parameter_code");
        in.parameter(SosHarvesterProcessDescriptor.TYPE_COLUMN_NAME).setValue("file_type");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Profile");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        
        ParameterValue scval1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval1.setValue(new ServiceProcessReference(sc));
        in.values().add(scval1);
        ParameterValue scval2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval2.setValue(new ServiceProcessReference(sc2));
        in.values().add(scval2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/bigdata-1.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24T00:25:47.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T08:48:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(11, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        verifySamplingFeatureNotSame(fois);
        String foi = verifySamplingFeature(fois, 68.2395, -61.4234);

        Assert.assertNotNull(foi);
        
        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("30", "35", "66", "z_value"));
        
        Object o = sosWorker.getObservation(new GetObservationType("2.0.0", "SOS",Arrays.asList(offp.getId()), null, Arrays.asList(sensorId), null, null, null,null));
        Assert.assertTrue(o instanceof ObservationCollection);

        ObservationCollection oc = (ObservationCollection)o;

        String observedProperty = null;
        for (Observation obs : oc.getMember()) {
            if (obs.getFeatureOfInterest() instanceof SamplingFeature sf) {
                if (sf.getId().equals(foi)) {
                    observedProperty = ((Phenomenon)obs.getObservedProperty()).getName().getCode();
                }
            }
        }
        Assert.assertNotNull(observedProperty);

        /*
         * Verify an inserted profile
         */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(11, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:25:47Z", -35.27835, -3.61021);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9566, nbMeasure);
    }

    @Test
    public void harvesterCSVFlatProfileTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(bigdataDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("station_date");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("z_value");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("latitude");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("longitude");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val1.setValue("30");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val2.setValue("35");
        in.values().add(val2);
        ParameterValue val3 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val3.setValue("66");
        in.values().add(val3);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("parameter_value");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("parameter_code");
        in.parameter(SosHarvesterProcessDescriptor.TYPE_COLUMN_NAME).setValue("file_type");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Profile");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue("urn:template:");
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("platform_code");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sc2));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/bigdata-1.csv"));


        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensors has been created
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1901290"));
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1901689"));
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1901710"));


        Assert.assertNull(sensorBusiness.getSensor("urn:template:666999"));

        Assert.assertEquals(11, getNbOffering(sosWorker, prev));

        ObservationOffering offp = getOffering(sosWorker, "urn:template:1901290");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof GMLInstant);
        GMLInstant time = (GMLInstant) offp.getTime();

        // ???
        Assert.assertEquals("2020-03-24T05:07:54Z", sdf.format(time.getTimePosition().getDate()));

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeature(fois, 68.2395, -61.4234);

        Assert.assertNotNull(foi);

        Assert.assertEquals(5, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        verifyAllObservedProperties(stsWorker, "urn:template:1901290", Arrays.asList("z_value", "30", "35", "66"));

        /*
         * Verify an inserted profile
         */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1901290");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T05:07:54Z", 68.2395, -61.4234);

        int nbMeasure = getNbMeasure(stsWorker, "urn:template:1901290");
        Assert.assertEquals(68, nbMeasure);

       /*
        * second
        */

        offp = getOffering(sosWorker, "urn:template:1901689");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof GMLInstant);
        time = (GMLInstant) offp.getTime();

        Assert.assertEquals("2020-03-24T08:48:00Z", sdf.format(time.getTimePosition().getDate()));

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeature(fois, -25.92446, 5.92986);

        Assert.assertNotNull(foi);

        Assert.assertEquals(4, offp.getObservedProperties().size());
        observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        verifyAllObservedProperties(stsWorker, "urn:template:1901689", Arrays.asList("z_value", "30", "35"));

        /*
         * Verify an inserted profile
         */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-4.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1901689");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T08:48:00Z", -25.92446, 5.92986);

        nbMeasure = getNbMeasure(stsWorker, "urn:template:1901689");
        Assert.assertEquals(503, nbMeasure);

        // verify that all the sensors have at least one of the three observed properties
        List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(sc.getId(), null);
        for (String sid : sensorIds) {
            if (sid.startsWith("urn:template:")) {
                verifyObservedProperties(stsWorker, sid, Arrays.asList("30", "35", "66"));
            }
        }
    }

    @Test
    public void harvesterCSVFlatTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_2";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(bigdataDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("station_date");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("station_date");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("latitude");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("longitude");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val1.setValue("30");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val2.setValue("35");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.QUALITY_COLUMN_NAME).setValue("parameter_qc");
        in.parameter(SosHarvesterProcessDescriptor.QUALITY_COLUMN_ID_NAME).setValue("parameter_qc_mod");
        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("parameter_value");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("parameter_code");
        in.parameter(SosHarvesterProcessDescriptor.TYPE_COLUMN_NAME).setValue("file_type");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("platform_code");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue("urn:template:");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);

        ParameterValue scval1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval1.setValue(new ServiceProcessReference(sc));
        in.values().add(scval1);
        ParameterValue scval2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval2.setValue(new ServiceProcessReference(sc2));
        in.values().add(scval2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/bigdata-1.csv"));


        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1501563"));
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1501564"));

        // not matching the measure
        Assert.assertNull(sensorBusiness.getSensor("urn:template:1301603"));

        Assert.assertEquals(301, getNbOffering(sosWorker, prev));

        /*
        * first extracted procedure
        */

        ObservationOffering offp = getOffering(sosWorker, "urn:template:1501563");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T10:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeatureLine(fois,21);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        String observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("35", observedProperty);

        Set<String> qualityFields = getQualityFieldNames(stsWorker, "urn:template:1501563");
        Assert.assertEquals(1, qualityFields.size());
        assertTrue(qualityFields.contains("parameter_qc_mod"));

        /*
        * Verify an inserted data (time filter is here juste to test the query)
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi, "1950-01-01T00:00:00Z", "2500-01-01T00:00:00Z", true);
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-2-quality.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1501563");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:00:00Z", -20.539, -29.9916);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2020-03-24T00:30:00Z", -20.5456, -29.995);

        int nbMeasure = getNbMeasure(stsWorker, "urn:template:1501563");
        Assert.assertEquals(21, nbMeasure);

        /*
        * second extracted procedure
        */

        offp = getOffering(sosWorker, "urn:template:1501564");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T10:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeatureLine(fois, 21);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        observedProperty = offp.getObservedProperties().get(0);

        Assert.assertEquals("35", observedProperty);
        /*
        * Verify an inserted data
        */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi, "1950-01-01T00:00:00Z", "2500-01-01T00:00:00Z", true);
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-3-quality.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1501564");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:00:00Z", -23.209, -30.3464);

        loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2020-03-24T00:30:00Z", -23.2064, -30.3484);

        nbMeasure = getNbMeasure(stsWorker, "urn:template:1501564");
        Assert.assertEquals(21, nbMeasure);

        /*
        * third extracted procedure with a lot of mesure code
        */

        offp = getOffering(sosWorker, "urn:template:1801573");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T09:59:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeatureLine(fois, 600);

        // composite + components
        Assert.assertEquals(3, offp.getObservedProperties().size());

        List<String> observedProperties = getObservedProperties(stsWorker, "urn:template:1801573");

        Assert.assertEquals(2, observedProperties.size());
        Assert.assertEquals("30", observedProperties.get(0));
        Assert.assertEquals("35", observedProperties.get(1));

        nbMeasure = getNbMeasure(stsWorker, "urn:template:1801573");
        Assert.assertEquals(600, nbMeasure);

        /*
        * fourth extracted procedure with only measure 1
        */

        offp = getOffering(sosWorker, "urn:template:2100914");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T08:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeature(fois, 137.91875, 16.01637);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        observedProperty = offp.getObservedProperties().get(0);

        Assert.assertEquals("35", observedProperty);

        nbMeasure = getNbMeasure(stsWorker, "urn:template:2100914");
        Assert.assertEquals(4, nbMeasure);

        // verify that all the sensors have at least one of the two observed properties
        List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(sc.getId(), null);
        for (String sid : sensorIds) {
            if (sid.startsWith("urn:template:")) {
                verifyObservedProperties(stsWorker, sid, Arrays.asList("30", "35"));
            }
        }

    }

    @Test
    public void harvesterCSVFlatMultiTypeTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_3";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(bigdataDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("station_date");
        in.parameter(SosHarvesterProcessDescriptor.Z_COLUMN_NAME).setValue("z_value");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("latitude");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("longitude");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val1.setValue("30");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val2.setValue("35");
        in.values().add(val2);

        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");
        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("parameter_value");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("parameter_code");
        in.parameter(SosHarvesterProcessDescriptor.TYPE_COLUMN_NAME).setValue("file_type");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue(null);
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("platform_code");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(null);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);

        ParameterValue scval1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval1.setValue(new ServiceProcessReference(sc));
        in.values().add(scval1);
        ParameterValue scval2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        scval2.setValue(new ServiceProcessReference(sc2));
        in.values().add(scval2);


        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/bigdata-1.csv"));


        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("1501563"));
        Assert.assertNotNull(sensorBusiness.getSensor("1501564"));

        // not matching the measure
        Assert.assertNull(sensorBusiness.getSensor("urn:template:1301603"));

        Assert.assertEquals(312, getNbOffering(sosWorker, prev));

        /*
        * first extracted procedure (TIMESERIE)
        */

        Thing t = getThing(stsWorker, "1501563");
        Assert.assertNotNull(t);
        Assert.assertEquals("1501563", t.getName());
        Assert.assertEquals("", t.getDescription());


        ObservationOffering offp = getOffering(sosWorker, "1501563");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T10:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeatureLine(fois,21);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        String observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("35", observedProperty);

        /*
        * Verify an inserted data
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1501563");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:00:00Z", -20.539, -29.9916);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2020-03-24T00:30:00Z", -20.5456, -29.995);

        int nbMeasure = getNbMeasure(stsWorker, "1501563");
        Assert.assertEquals(21, nbMeasure);

        /*
        * second extracted procedure (TIMESERIE)
        */

        offp = getOffering(sosWorker, "1501564");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T10:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi  = verifySamplingFeatureLine(fois,21);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("35", observedProperty);

        /*
        * Verify an inserted data
        */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-3.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1501564");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:00:00Z", -23.209, -30.3464);

        loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, "2020-03-24T00:30:00Z", -23.2064, -30.3484);

        nbMeasure = getNbMeasure(stsWorker, "1501564");
        Assert.assertEquals(21, nbMeasure);

        /*
        * third extracted procedure with a lot of mesure code (TIMESERIE)
        */

        offp = getOffering(sosWorker, "1801573");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T09:59:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi  = verifySamplingFeatureLine(fois,600);

        // composite + components
        Assert.assertEquals(3, offp.getObservedProperties().size());
        observedProperty = null;
        for (String op : offp.getObservedProperties()) {
            if (op.startsWith("composite")) observedProperty = op;
        }
        assertNotNull(observedProperty);
        
        List<String> observedProperties = getObservedProperties(stsWorker, "1801573");

        Assert.assertEquals(2, observedProperties.size());
        Assert.assertEquals("30", observedProperties.get(0));
        Assert.assertEquals("35", observedProperties.get(1));


        nbMeasure = getNbMeasure(stsWorker, "1801573");
        Assert.assertEquals(600, nbMeasure);

        /*
        * fourth extracted procedure with only measure 1 (TIMESERIE)
        */

        offp = getOffering(sosWorker, "2100914");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T08:00:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeature(fois, 137.91875, 16.01637);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("35", observedProperty);

        nbMeasure = getNbMeasure(stsWorker, "2100914");
        Assert.assertEquals(4, nbMeasure);

        // verify that those profiles sensors has been created
        Assert.assertNotNull(sensorBusiness.getSensor("1901290"));
        Assert.assertNotNull(sensorBusiness.getSensor("1901689"));
        Assert.assertNotNull(sensorBusiness.getSensor("1901710"));


       /*
        * fifth extracted procedure with only measure 1 (PROFILE)
        */
        offp = getOffering(sosWorker, "1901290");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof GMLInstant);
        GMLInstant timeI = (GMLInstant) offp.getTime();


        // ???
        Assert.assertEquals("2020-03-24T05:07:54Z", sdf.format(timeI.getTimePosition().getDate()));

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi  = verifySamplingFeature(fois, 68.2395, -61.4234);

        Assert.assertEquals(4, offp.getObservedProperties().size());
        observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        verifyAllObservedProperties(stsWorker, "1901290", Arrays.asList("z_value", "30", "35"));

        /*
         * Verify an inserted profile
         */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-5.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1901290");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T05:07:54Z", 68.2395, -61.4234);

        nbMeasure = getNbMeasure(stsWorker, "1901290");
        Assert.assertEquals(68, nbMeasure);

       /*
        * sixth (PROFILE)
        */

        offp = getOffering(sosWorker, "1901689");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof GMLInstant);
        timeI = (GMLInstant) offp.getTime();

        // ???
        Assert.assertEquals("2020-03-24T08:48:00Z", sdf.format(timeI.getTimePosition().getDate()));

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi  = verifySamplingFeature(fois, -25.92446, 5.92986);

        Assert.assertEquals(4, offp.getObservedProperties().size());
        observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        /*
         * Verify an inserted profile
         */
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-6.txt");
        Assert.assertEquals(expectedResult, result);

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1901689");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T08:48:00Z", -25.92446, 5.92986);

        nbMeasure = getNbMeasure(stsWorker, "1901689");
        Assert.assertEquals(503, nbMeasure);

    }

    @Test
    public void harvesterCSVSurvalTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_3";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(survalDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");
        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("ANALYSE_DATE");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("ANALYSE_DATE");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("dd/MM/yy");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val1.setValue("7-FLORTOT");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val2.setValue("18-FLORTOT");
        in.values().add(val2);
        ParameterValue val3 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMNS_FILTER_NAME).createValue();
        val3.setValue("18-SALI");
        in.values().add(val3);

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("VALUE");

        ParameterValue cc1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        cc1.setValue("SUPPORT");
        in.values().add(cc1);
        ParameterValue cc2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        cc2.setValue("PARAMETER");
        in.values().add(cc2);

        ParameterValue od1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_NAME_COLUMN_NAME).createValue();
        od1.setValue("SUPPORT_NAME");
        in.values().add(od1);
        ParameterValue od2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_NAME_COLUMN_NAME).createValue();
        od2.setValue("PARAMETER_NAME");
        in.values().add(od2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("PLATFORM_ID");
        in.parameter(SosHarvesterProcessDescriptor.THING_NAME_COLUMN_NAME).setValue("PLATFORM_NAME");
        in.parameter(SosHarvesterProcessDescriptor.THING_DESC_COLUMN_NAME).setValue("PLATFORM_DESC");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue("urn:surval:");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));
        in.parameter(SosHarvesterProcessDescriptor.UOM_COLUMN_NAME).setValue("PARAMETER_UNIT");

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        in.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        in.values().add(s2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/surval-small.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor("urn:surval:25049001");
        Assert.assertNotNull(sensor);
        Assert.assertEquals("urn:surval:25049001", sensor.getIdentifier());
        Assert.assertEquals("055-P-001 - Men er Roue", sensor.getName());
        Assert.assertEquals("description de la platf", sensor.getDescription());

        Thing t = getThing(stsWorker, "urn:surval:25049001");
        Assert.assertNotNull(t);
        Assert.assertEquals("055-P-001 - Men er Roue", t.getName());
        Assert.assertEquals("description de la platf", t.getDescription());


        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * first extracted procedure
        */

        ObservationOffering offp = getOffering(sosWorker, "urn:surval:25049001");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1987-06-01", time.getBeginPosition().getValue());
        Assert.assertEquals("2019-12-17", time.getEndPosition().getValue());

        // something is wrong here
        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeature(fois,-3.093748, 47.534765);

        // comosite + components
        Assert.assertEquals(4, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        verifyAllObservedProperties(stsWorker, "urn:surval:25049001", Arrays.asList("7-FLORTOT", "18-FLORTOT", "18-SALI"));

        List<ObservedProperty> obsProperties = getFullObservedProperties(stsWorker, "urn:surval:25049001");
        for(ObservedProperty op : obsProperties) {
            if ("7-FLORTOT".equals(op.getIotId())) {
                Assert.assertEquals("Support : Masse d'eau, eau brute - Niveau : Surface (0-1m)-Flore Totale - abondance de cellules", op.getName());
            } else if ("18-FLORTOT".equals(op.getIotId())) {
                Assert.assertEquals("Support : Masse d'eau, eau brute - Niveau : Mi-profondeur-Flore Totale - abondance de cellules", op.getName());
            } else if ("18-SALI".equals(op.getIotId())) {
                Assert.assertEquals("Support : Masse d'eau, eau brute - Niveau : Mi-profondeur-Salinité", op.getName());
            } else {
                Assert.fail("Unexpected observed properties:" + op.getIotId());
            }
        }
        MultiDatastream mds = stsWorker.getMultiDatastreamById(new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:urn:surval:25049001"));
        Assert.assertNotNull(mds);

        assertTrue(mds.getUnitOfMeasurement() instanceof List);

        List listUom = (List) mds.getUnitOfMeasurement();

        Assert.assertEquals(3, listUom.size());

        for (Object uomObj : listUom) {
            assertTrue(uomObj instanceof UnitOfMeasure);
            UnitOfMeasure uom = (UnitOfMeasure) uomObj;
            String uomName = uom.getName();
            Assert.assertNotNull(uomName);
            Assert.assertTrue("sans unité".equals(uomName) || "l-1".equals(uomName));
        }
        /*
        * Verify an inserted data
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/surval-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:surval:25049001");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2007-12-18T00:00:00Z", -3.093748, 47.534765);
        int nbMeasure = getNbMeasure(stsWorker, "urn:surval:25049001");
        Assert.assertEquals(791, nbMeasure);

    }

    @Test
    public void harvesterXLSTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_4";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(xDataDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("date_mesure");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("date_mesure");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("dd/MM/yyyy HH:mm:ss");

         in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("ph");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("capteur");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue("urn:xdata:");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        in.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        in.values().add(s2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/xdata.xlsx"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor("urn:xdata:cap-90");
        Assert.assertNotNull(sensor);
        Assert.assertEquals("urn:xdata:cap-90", sensor.getIdentifier());

        Thing t = getThing(stsWorker, "urn:xdata:cap-90");
        Assert.assertNotNull(t);

        Assert.assertEquals(2, getNbOffering(sosWorker, prev));

        /*
        * first extracted procedure
        */

        ObservationOffering offp = getOffering(sosWorker, "urn:xdata:cap-90");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2021-01-29T06:39:29.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2021-01-29T06:39:31.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        
        String observedProperty = offp.getObservedProperties().get(0);

        Assert.assertEquals("ph" , observedProperty);

        /*
        * Verify an inserted data
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        String expectedResult = getResourceAsString("com/examind/process/sos/xdata-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, "urn:xdata:cap-90");
        Assert.assertEquals(3, nbMeasure);
    }

    @Test
    public void harvesterXLSFlatTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_4";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(xDataFlatDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("dd/MM/yyyy'T'HH:mm:ss");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("dd/MM/yy");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE");

        in.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("RES");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("PROPERTY");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue("urn:flat:xlsx");
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        in.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        in.values().add(s2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/test-flat.xlsx"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor("urn:flat:xlsx");
        Assert.assertNotNull(sensor);
        Assert.assertEquals("urn:flat:xlsx", sensor.getIdentifier());

        Thing t = getThing(stsWorker, "urn:flat:xlsx");
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * first extracted procedure
        */

        ObservationOffering offp = getOffering(sosWorker, "urn:flat:xlsx");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        Assert.assertEquals(3, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        /*
        * Verify an inserted data
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        String expectedResult = getResourceAsString("com/examind/process/sos/test-flat-X-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, "urn:flat:xlsx");
        Assert.assertEquals(2, nbMeasure);
    }

    @Test
    public void harvesterCSVNoHeaderTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_5";
        String sensorID = "NHSensor";
        String observedProperty = "Conductivité";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(noHeadDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFile");
        in.parameter(SosHarvesterProcessDescriptor.DIRECT_COLUMN_INDEX_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.NO_HEADER_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(";");

        ParameterValue main1 = (ParameterValue) SosHarvesterProcessDescriptor.MAIN_COLUMN.createValue();
        main1.setValue("0");
        in.values().add(main1);
        ParameterValue main2 = (ParameterValue) SosHarvesterProcessDescriptor.MAIN_COLUMN.createValue();
        main2.setValue("1");
        in.values().add(main2);

        ParameterValue date1 = (ParameterValue) SosHarvesterProcessDescriptor.DATE_COLUMN.createValue();
        date1.setValue("0");
        in.values().add(date1);
        ParameterValue date2 = (ParameterValue) SosHarvesterProcessDescriptor.DATE_COLUMN.createValue();
        date2.setValue("1");
        in.values().add(date2);

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-ddHH:mm:ss");
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("2");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorID);
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_ID_NAME).setValue(observedProperty);
        
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        in.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        in.values().add(s2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/nohead.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorID);
        Assert.assertNotNull(sensor);

        Thing t = getThing(stsWorker, sensorID);
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * first extracted procedure
        */

        ObservationOffering offp = getOffering(sosWorker, sensorID);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2002-01-01T01:24:22.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2002-01-03T13:47:59.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        Assert.assertEquals(observedProperty, offp.getObservedProperties().get(0));

        /*
        * Verify an inserted data
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        String expectedResult = getResourceAsString("com/examind/process/sos/nohead-datablock-values.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(3, nbMeasure);
    }

    @Test
    public void harvesterCSVDisjointTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_6";
        String sensorID = "urn:ogc:object:sensor:GEOM:disjoint_sensor";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup baseParam = desc.getInputDescriptor().createValue();
        baseParam.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        baseParam.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(disjointDirectory.toUri().toString());

        baseParam.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFile");
        baseParam.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue(",");
        baseParam.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");
        baseParam.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        baseParam.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");
        baseParam.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        baseParam.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("lat");
        baseParam.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("lon");
        baseParam.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorID);
        baseParam.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        baseParam.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        baseParam.values().add(s2);

        /*
        * first insertion
        */
        ParameterValueGroup in = baseParam.clone();
        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("salinity-DIS");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("temperature-DIS");
        in.values().add(val2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/disjoint-1.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorID);
        Assert.assertNotNull(sensor);

        Thing t = getThing(stsWorker, sensorID);
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * first extraction
        */
        ObservationOffering offp = getOffering(sosWorker, sensorID);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        // composite
        String observedProperty = offp.getObservedProperties().get(0);

        /*
        * Verify an inserted data
        */
        String expectedResult = getResourceAsString("com/examind/process/sos/disjoint-1.txt");
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(2, nbMeasure);

        /*
        * second insertion
        */
        writeResourceDataFile(disjointDirectory, "com/examind/process/sos/disjoint-2.csv", "disjoint-2.csv");

        in = baseParam.clone();
        val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("depth-DIS");
        in.values().add(val1);
        val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("pressure-DIS");
        in.values().add(val2);

        proc = desc.createProcess(in);
        results = proc.call();

        nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        List<String> alreadyInsertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_NAME);
        int nbAlreadyInserted= (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbAlreadyInserted);
        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/disjoint-2.csv"));
        Assert.assertTrue(alreadyInsertedFiles.contains("/disjoint-1.csv"));

        t = getThing(stsWorker, sensorID);
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * second extraction
        */
        offp = getOffering(sosWorker, sensorID);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-04T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());


        /*
        * Verify an inserted data
        */
        result = getMeasure(stsWorker, sensorID);
        expectedResult = getResourceAsString("com/examind/process/sos/disjoint-2.txt");
        Assert.assertEquals(expectedResult, result);

        nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(4, nbMeasure);

        /*
        * third insertion
        */
        writeResourceDataFile(disjointDirectory, "com/examind/process/sos/disjoint-3.csv", "disjoint-3.csv");

        in = baseParam.clone();
        val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("hotness-DIS");
        in.values().add(val1);
        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_TYPE_NAME).setValue("TEXT");

        proc = desc.createProcess(in);
        results = proc.call();

        nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();
        insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        alreadyInsertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_NAME);
        nbAlreadyInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_ALREADY_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(2, nbAlreadyInserted);
        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/disjoint-3.csv"));
        Assert.assertTrue(alreadyInsertedFiles.contains("/disjoint-2.csv"));
        Assert.assertTrue(alreadyInsertedFiles.contains("/disjoint-1.csv"));

        t = getThing(stsWorker, sensorID);
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * third extraction
        */
        offp = getOffering(sosWorker, sensorID);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-06T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());


        /*
        * Verify an inserted data
        */
        result = getMeasure(stsWorker, sensorID);
        expectedResult = getResourceAsString("com/examind/process/sos/disjoint-3.txt");
        Assert.assertEquals(expectedResult, result);

        nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(6, nbMeasure);

    }

    @Test
    public void harvesterTSVTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_6";
        String sensorID = "urn:ogc:object:sensor:GEOM:tsv_sensor";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup baseParam = desc.getInputDescriptor().createValue();
        baseParam.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        baseParam.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(tsvDirectory.toUri().toString());

        baseParam.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFile");
        baseParam.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue("\t");
        baseParam.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");
        baseParam.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        baseParam.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");
        baseParam.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        baseParam.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        baseParam.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");
        baseParam.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorID);
        baseParam.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        baseParam.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        baseParam.values().add(s2);

        /*
        * first insertion
        */
        ParameterValueGroup in = baseParam.clone();
        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMPERATURE");
        in.values().add(val1);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/tabulation.tsv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorID);
        Assert.assertNotNull(sensor);

        Thing t = getThing(stsWorker, sensorID);
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * first extraction
        */
        ObservationOffering offp = getOffering(sosWorker, sensorID);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        // composite
        String observedProperty = offp.getObservedProperties().get(0);

        /*
        * Verify an inserted data
        */
        String expectedResult = getResourceAsString("com/examind/process/sos/tabulation.txt");
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(2, nbMeasure);
    }

    @Test
    public void harvesterTSVFlatTSTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA_6";
        String sensorID = "urn:ogc:object:sensor:GEOM:tsv_flat_sensor";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup baseParam = desc.getInputDescriptor().createValue();
        baseParam.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        baseParam.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(tsvFlatDirectory.toUri().toString());

        baseParam.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationCsvFlatFile");
        baseParam.parameter(SosHarvesterProcessDescriptor.SEPARATOR_NAME).setValue("\t");
        baseParam.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("TIME");
        baseParam.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("TIME");
        baseParam.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");
        baseParam.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        baseParam.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LAT");
        baseParam.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LON");
        baseParam.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorID);
        baseParam.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        baseParam.parameter(SosHarvesterProcessDescriptor.RESULT_COLUMN_NAME).setValue("RESULT");

        ParameterValue s1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s1.setValue(new ServiceProcessReference(sc));
        baseParam.values().add(s1);
        ParameterValue s2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        s2.setValue(new ServiceProcessReference(sc2));
        baseParam.values().add(s2);
        
        /*
        * first insertion
        */
        ParameterValueGroup in = baseParam.clone();
        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("PROPERTY");
        in.values().add(val1);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/tabulation-flat.tsv"));
        
        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorID);
        Assert.assertNotNull(sensor);

        Thing t = getThing(stsWorker, sensorID);
        Assert.assertNotNull(t);

        Assert.assertEquals(1, getNbOffering(sosWorker, prev));

        /*
        * first extraction
        */
        ObservationOffering offp = getOffering(sosWorker, sensorID);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        // composite
        String observedProperty = offp.getObservedProperties().get(0);

        /*
        * Verify an inserted data
        */
        String expectedResult = getResourceAsString("com/examind/process/sos/tabulation.txt");
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, null);
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(2, nbMeasure);
    }

    /**
     * the point of this test is to set a file instead of a directory
     * @throws Exception
     */
    @Test
    public void harvestSingleFileTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:sf";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(mooFile.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("DATE (yyyy-mm-ddThh:mi:ssZ)");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");

        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val1.setValue("TEMP LEVEL0 (degree_Celsius)");
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        val2.setValue("VZMX LEVEL0 (meter)");
        in.values().add(val2);


        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(true);
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sc));

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/mooring-buoys-time-series-62069.csv"));


        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-10-30", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-30T12:30:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeature(fois,  -4.9683, 48.2903);

        Assert.assertEquals(3, offp.getObservedProperties().size());
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP LEVEL0 (degree_Celsius)", "VZMX LEVEL0 (meter)"));

        /*
        * Verify an inserted timeSeries
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, result);

        /*
        * Verify an inserted timeSeries
        */
        observedProperty = "VZMX LEVEL0 (meter)";
        result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values-3.txt");
        Assert.assertEquals(expectedResult, result);

        Object obj = sosWorker.getObservation(new GetObservationType("2.0.0", offp.getId(), null, Arrays.asList(sensorId), Arrays.asList(observedProperty), new ArrayList<>(), null));

        Assert.assertTrue(obj instanceof GetObservationResponseType);

        GetObservationResponseType goResponse = (GetObservationResponseType) obj;

        Assert.assertTrue(goResponse.getObservationData().get(0).getOMObservation().getResult() instanceof DataArrayPropertyType);

        DataArrayPropertyType daResult = (DataArrayPropertyType) goResponse.getObservationData().get(0).getOMObservation().getResult();

        String value = daResult.getDataArray().getValues();
        Assert.assertEquals(expectedResult, value + '\n');

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(3020, nbMeasure);
    }
    
    @Test
    public void harvestCSVMultiFixedTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:mf1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(multiFixedDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("date");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("date");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        ParameterValue valOPC1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        valOPC1.setValue("col1");
        in.values().add(valOPC1);
        ParameterValue valOPC2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        valOPC2.setValue("col2");
        in.values().add(valOPC2);
        
        ParameterValue valOPI1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_ID_NAME).createValue();
        valOPI1.setValue("temperature");
        in.values().add(valOPI1);
        ParameterValue valOPI2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_ID_NAME).createValue();
        valOPI2.setValue("salinity");
        in.values().add(valOPI2);

        ParameterValue valUOM1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.UOM_ID_NAME).createValue();
        valUOM1.setValue("°C");
        in.values().add(valUOM1);
        ParameterValue valUOM2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.UOM_ID_NAME).createValue();
        valUOM2.setValue("msu");
        in.values().add(valUOM2);


        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_COLUMN_NAME).setValue("proc");
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sc2));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/multi-fixed-1.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorId);
        Assert.assertNotNull(sensor);
        Assert.assertEquals(sensorId, sensor.getIdentifier());
        Assert.assertEquals(sensorId, sensor.getName());

        Thing t = getThing(stsWorker, sensorId);
        Assert.assertNotNull(t);
        Assert.assertEquals(sensorId, t.getName());

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:53:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        Assert.assertEquals(3, offp.getObservedProperties().size());
        assertTrue(offp.getObservedProperties().contains("temperature"));
        assertTrue(offp.getObservedProperties().contains("salinity"));
        
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);
        String foi = offp.getFeatureOfInterestIds().get(0);


        /*
        * Verify an inserted profile
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/multi-fixed-values-1.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(2, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("temperature", "salinity"));
        
        MultiDatastream mds = stsWorker.getMultiDatastreamById(new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:" + sensorId));
        Assert.assertNotNull(mds);

        assertTrue(mds.getUnitOfMeasurement() instanceof List);

        List listUom = (List) mds.getUnitOfMeasurement();

        Assert.assertEquals(2, listUom.size());

        List<String> expectedUoms = Arrays.asList("°C", "msu");
        for (Object uomObj : listUom) {
            assertTrue(uomObj instanceof UnitOfMeasure);
            UnitOfMeasure uom = (UnitOfMeasure) uomObj;
            String uomName = uom.getName();
            Assert.assertNotNull(uomName);
            Assert.assertTrue("unexpected uom:" + uomName, expectedUoms.contains(uomName));
        }
    }
    
    @Test
    public void harvestCSVSingleQualityTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:sqcsv:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(qualityCSVDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        ParameterValue valOPC1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        valOPC1.setValue("salinity-SQ");
        in.values().add(valOPC1);
        ParameterValue valOPC2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        valOPC2.setValue("temperature-SQ");
        in.values().add(valOPC2);
        
        ParameterValue valQ1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_NAME).createValue();
        valQ1.setValue("sal-qual");
        in.values().add(valQ1);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sc2));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/single-csv-qual.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorId);
        Assert.assertNotNull(sensor);
        Assert.assertEquals(sensorId, sensor.getIdentifier());
        Assert.assertEquals(sensorId, sensor.getName());

        Thing t = getThing(stsWorker, sensorId);
        Assert.assertNotNull(t);
        Assert.assertEquals(sensorId, t.getName());

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        Assert.assertEquals(3, offp.getObservedProperties().size());
        assertTrue(offp.getObservedProperties().contains("temperature-SQ"));
        assertTrue(offp.getObservedProperties().contains("salinity-SQ"));
        
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);
        String foi = offp.getFeatureOfInterestIds().get(0);
        
        Set<String> qualityFields = getQualityFieldNames(stsWorker, sensorId);
        Assert.assertEquals(1, qualityFields.size());
        assertTrue(qualityFields.contains("sal-qual"));


        /*
        * Verify an inserted profile
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/single-csv-qual-values.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(2, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("temperature-SQ", "salinity-SQ"));
        
        MultiDatastream mds = stsWorker.getMultiDatastreamById(new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:" + sensorId));
        Assert.assertNotNull(mds);
    }
    
    @Test
    public void harvestCSVMultiQualityTest() throws Exception {

        SOSworker sosWorker = (SOSworker) wsEngine.buildWorker("sos", "default");
        sosWorker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(sosWorker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:mqcsv:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(multiQualityCSVDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss.S");

        ParameterValue valOPC1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        valOPC1.setValue("salinity-SQ");
        in.values().add(valOPC1);
        ParameterValue valOPC2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).createValue();
        valOPC2.setValue("temperature-SQ");
        in.values().add(valOPC2);
        
        ParameterValue valQ1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_NAME).createValue();
        valQ1.setValue("sal-qual");
        in.values().add(valQ1);
        
        ParameterValue valQ2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_NAME).createValue();
        valQ2.setValue("temp-qual");
        in.values().add(valQ2);
        
        ParameterValue valQID1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_ID_NAME).createValue();
        valQID1.setValue("sal-quality");
        in.values().add(valQID1);
        
        ParameterValue valQID2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_ID_NAME).createValue();
        valQID2.setValue("temp-quality");
        in.values().add(valQID2);
        
        ParameterValue valQT1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_TYPE_NAME).createValue();
        valQT1.setValue("TEXT");
        in.values().add(valQT1);
        
        ParameterValue valQT2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.QUALITY_COLUMN_TYPE_NAME).createValue();
        valQT2.setValue("QUANTITY");
        in.values().add(valQT2);

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sc));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sc2));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        ParameterValueGroup results = proc.call();

        List<String> insertedFiles = ProcessUtils.getMultipleValues(results, SosHarvesterProcessDescriptor.FILE_INSERTED_NAME);
        int nbInserted = (Integer) results.parameter(SosHarvesterProcessDescriptor.FILE_INSERTED_COUNT_NAME).getValue();

        Assert.assertEquals(1, nbInserted);
        Assert.assertTrue(insertedFiles.contains("/multi-csv-qual.csv"));

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Sensor sensor = sensorBusiness.getSensor(sensorId);
        Assert.assertNotNull(sensor);
        Assert.assertEquals(sensorId, sensor.getIdentifier());
        Assert.assertEquals(sensorId, sensor.getName());

        Thing t = getThing(stsWorker, sensorId);
        Assert.assertNotNull(t);
        Assert.assertEquals(sensorId, t.getName());

        ObservationOffering offp = getOffering(sosWorker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("1980-03-01T21:52:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("1980-03-02T21:52:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        Assert.assertEquals(3, offp.getObservedProperties().size());
        assertTrue(offp.getObservedProperties().contains("temperature-SQ"));
        assertTrue(offp.getObservedProperties().contains("salinity-SQ"));
        
        String observedProperty = getCompositePhenomenon(offp);
        assertNotNull(observedProperty);
        String foi = offp.getFeatureOfInterestIds().get(0);

        Set<String> qualityFields = getQualityFieldNames(stsWorker, sensorId);
        Assert.assertEquals(2, qualityFields.size());
        assertTrue(qualityFields.contains("sal-quality"));
        assertTrue(qualityFields.contains("temp-quality"));
        

        /*
        * Verify an inserted profile
        */
        String result = getMeasure(sosWorker, offp.getId(), observedProperty, foi);
        String expectedResult = getResourceAsString("com/examind/process/sos/multi-csv-qual-values.txt");
        Assert.assertEquals(expectedResult, result);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(2, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("temperature-SQ", "salinity-SQ"));
        
        MultiDatastream mds = stsWorker.getMultiDatastreamById(new GetMultiDatastreamById("urn:ogc:object:observation:template:GEOM:" + sensorId));
        Assert.assertNotNull(mds);
    }
}
