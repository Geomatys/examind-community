/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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

import com.examind.process.admin.AdminProcessRegistry;
import com.examind.process.admin.yamlReader.ProcessFromYamlProcessDescriptor;
import com.examind.sts.core.STSWorker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.exception.ConstellationException;
import org.constellation.sos.core.SOSworker;
import org.constellation.test.utils.Order;
import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import static com.examind.process.sos.SosHarvesterTestUtils.*;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.v200.GetObservationType;
import org.geotoolkit.sos.xml.v200.GetResultResponseType;
import org.geotoolkit.sos.xml.v200.GetResultType;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.swe.xml.Phenomenon;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Instant;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SosHarvesterProcessYamlTest extends AbstractSosHarvesterTest {

    @Test
    @Order(order = 9)
    public void harvesterCSVFlatProfileSingleFromYamlNoFIlterTest() throws ConstellationException, NoSuchIdentifierException, ProcessException, IOException, ParseException {
        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        sensorServBusiness.removeAllSensors(sc.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:bgdata";

        String datasetId = "SOS_DATA";

        // Create a temporary yaml file.
        Path tempFile = Files.createTempFile(null, null);
        List<String> listYamlParameter = Arrays.asList(
                "process_name: sosHarvester",
                "data_folder: "+bigdataDirectory.toUri().toString(),
                "sensor_service:",
                "   service:",
                "      identifier: default",
                "      type: sos",
                "   service2:",
                "      identifier: default",
                "      type: sts",
                "dataset_identifier: "+datasetId,
                "thing_id: "+sensorId,
                "#thing_column: test string",
                "observation_type: Profile",
                "separator: ','",
                "main_column: z_value",
                "date_column: station_date",
                "date_format: yyyy-MM-dd'T'HH:mm:ss'Z'",
                "longitude_column: longitude",
                "latitude_column: latitude",
                "remove_previous_integration: true",
                "store_id: observationCsvFlatFile",
                "format: 'text/csv; subtype=\"om\"'",
                "result_column: parameter_value",
                "observed_properties_columns:",
                "- parameter_code",
                "type_column : file_type"
        );
        Files.write(tempFile, listYamlParameter, StandardOpenOption.APPEND);

        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(AdminProcessRegistry.NAME, "yamlReader");
        ParameterValueGroup in = desc.getInputDescriptor().createValue();

        in.parameter(ProcessFromYamlProcessDescriptor.DATA_FOLDER_NAME).setValue(tempFile);
        org.geotoolkit.process.Process process = desc.createProcess(in); // Create the process

        process.call();// Call the process.

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24T00:25:47.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T08:48:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(11, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        verifySamplingFeatureNotSame(fois);
        String foi = verifySamplingFeature(fois, 68.2395, -61.4234);

        Assert.assertNotNull(foi);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("30", "35", "66", "z_value", "68", "70" ,"99"));


        Object o = worker.getObservation(new GetObservationType("2.0.0", "SOS",Arrays.asList(offp.getId()), null, Arrays.asList(sensorId), null, null, null,null));
        Assert.assertTrue(o instanceof ObservationCollection);

        ObservationCollection oc = (ObservationCollection)o;

        String observedProperty = null;
        for (Observation obs : oc.getMember()) {
            if (obs.getFeatureOfInterest() instanceof SamplingFeature sf) {
                if (sf.getId().equals(foi)) {
                    observedProperty = ((Phenomenon)obs.getObservedProperty()).getName().getCode();
                }
            }
            Assert.assertNotNull(obs.getSamplingTime());
            Assert.assertTrue(obs.getSamplingTime() instanceof Instant);
            Instant instant = (Instant) obs.getSamplingTime();
            Assert.assertNotNull(instant.getDate());
        }
        Assert.assertNotNull(observedProperty);

        GetObservations stsGetObs = new GetObservations();
        stsGetObs.getExtraFilter().put("procedure", sensorId);
        stsGetObs.getExtraFlag().put("forMDS", "true");
        ObservationsResponse obss = (ObservationsResponse) stsWorker.getObservations(stsGetObs);
        for (org.geotoolkit.sts.json.Observation obs : obss.getValue()) {
            Assert.assertNotNull(obs.getPhenomenonTime());
        }
        /*
         * Verify an inserted profile
         */
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-7.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Assert.assertEquals(11, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:25:47Z", -35.27835, -3.61021);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9566, nbMeasure);
    }

    /**
     * Same test as harvesterCSVFlatProfileSingleTest but the process SosHarvester is called from the ProcessFromYamlProcess.
     */
    @Test
    @Order(order = 9)
    public void harvesterCSVSurvalProfileSingleFromYamlTest() throws Exception {
        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        sensorServBusiness.removeAllSensors(sc.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String datasetId = "SOS_DATA";

        // Create a temporary yaml file.
        Path tempFile = Files.createTempFile(null, null);
        List<String> listYamlParameter = Arrays.asList(
                "process_name: sosHarvester",
                "data_folder: "+survalDirectory.toUri().toString(),
                "sensor_service:",
                "   service:",
                "      identifier: default",
                "      type: sos",
                "   service2:",
                "      identifier: default",
                "      type: sts",
                "dataset_identifier: "+datasetId,
                "thing_id: 'urn:sensor:surval:'",
                "thing_column: PLATFORM_ID",
                "thing_name_column: PLATFORM_NAME",
                "thing_desc_column: PLATFORM_DESC",
                "observation_type: Timeserie",
                "separator: ';'",
                "main_column: ANALYSE_DATE",
                "date_column: ANALYSE_DATE",
                "date_format: dd/MM/yy",
                "longitude_column: LONGITUDE",
                "latitude_column: LATITUDE",
                "observed_properties_columns_filters:",
                "- 11-SALI",
                "- 12-NO2",
                "- 2-NB_DECHETS",
                "remove_previous_integration: true",
                "store_id: observationCsvFlatFile",
                "format: 'text/csv; subtype=\"om\"'",
                "result_column: VALUE",
                "observed_properties_columns:",
                "- SUPPORT",
                "- PARAMETER",
                "observed_properties_name_columns:",
                "- SUPPORT_NAME",
                "- PARAMETER_NAME"
        );
        Files.write(tempFile, listYamlParameter, StandardOpenOption.APPEND);

        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(AdminProcessRegistry.NAME, "yamlReader");
        ParameterValueGroup in = desc.getInputDescriptor().createValue();

        in.parameter(ProcessFromYamlProcessDescriptor.DATA_FOLDER_NAME).setValue(tempFile);
        org.geotoolkit.process.Process process = desc.createProcess(in); // Create the process

        process.call();// Call the process.

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("urn:sensor:surval:60007755"));

        Sensor sensor = sensorBusiness.getSensor("urn:sensor:surval:25049001");
        Assert.assertNotNull(sensor);
        Assert.assertEquals("urn:sensor:surval:25049001", sensor.getIdentifier());
        Assert.assertEquals("055-P-001 - Men er Roue", sensor.getName());
        Assert.assertEquals("description de la platf", sensor.getDescription());

        Thing t = getThing(stsWorker, "urn:sensor:surval:25049001");
        Assert.assertNotNull(t);
        Assert.assertEquals("055-P-001 - Men er Roue", t.getName());
        Assert.assertEquals("description de la platf", t.getDescription());

        ObservationOffering offp = getOffering(worker, "urn:sensor:surval:60007755");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2013-10-31", time.getBeginPosition().getValue());
        Assert.assertEquals("2015-09-26", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        verifySamplingFeatureNotSame(fois);
        String foi = verifySamplingFeature(fois, 15.3275447596057, 37.7923499263524);

        Assert.assertNotNull(foi);

        verifyAllObservedProperties(stsWorker, "urn:sensor:surval:60007755", Arrays.asList("2-NB_DECHETS"));

        List<ObservedProperty> obsProperties = getFullObservedProperties(stsWorker, "urn:sensor:surval:60007755");
        Assert.assertEquals(1, obsProperties.size());
        for(ObservedProperty op : obsProperties) {
            if ("2-NB_DECHETS".equals(op.getIotId())) {
                Assert.assertEquals("Niveau : Surface (0-20cm)-Nombre de déchets", op.getName());
            } else {
                Assert.fail("Unexpected observed properties:" + op.getIotId());
            }
        }

        verifyAllObservedProperties(stsWorker, "urn:sensor:surval:25049001", Arrays.asList("11-SALI", "12-NO2"));


        obsProperties = getFullObservedProperties(stsWorker, "urn:sensor:surval:25049001");
        Assert.assertEquals(2, obsProperties.size());
        for(ObservedProperty op : obsProperties) {
            if ("11-SALI".equals(op.getIotId())) {
                Assert.assertEquals("Niveau : Mi-profondeur-Salinité", op.getName());
            } else if ("12-NO2".equals(op.getIotId())) {
                Assert.assertEquals("Support : Eau filtrée - Niveau : Mi-profondeur-Azote nitreux (nitrite)", op.getName());
            } else {
                Assert.fail("Unexpected observed properties:" + op.getIotId());
            }
        }


        Object o = worker.getObservation(new GetObservationType("2.0.0", "SOS",Arrays.asList(offp.getId()), null, Arrays.asList("urn:sensor:surval:60007755"), null, null, null,null));
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
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/surval-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:sensor:surval:60007755");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Assert.assertEquals(1, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2015-09-26T00:00:00Z", 15.3275447596057, 37.7923499263524);

        int nbMeasure = getNbMeasure(stsWorker, "urn:sensor:surval:60007755");
        Assert.assertEquals(4, nbMeasure);
    }

    /**
     * Same test as harvesterCSVFlatProfileSingleTest but the process SosHarvester is called from the ProcessFromYamlProcess.
     */
    @Test
    @Order(order = 9)
    public void harvesterCSVFlatProfileSingleFromYamlTest() throws Exception {
        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        sensorServBusiness.removeAllSensors(sc.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:bgdata";

        String datasetId = "SOS_DATA";

        // Create a temporary yaml file.
        Path tempFile = Files.createTempFile(null, null);
        List<String> listYamlParameter = Arrays.asList(
                "process_name: sosHarvester",
                "data_folder: "+bigdataDirectory.toUri().toString(),
                "sensor_service:",
                "   service:",
                "      identifier: default",
                "      type: sos",
                "   service2:",
                "      identifier: default",
                "      type: sts",
                "dataset_identifier: "+datasetId,
                "thing_id: "+sensorId,
                "#thing_column: test string",
                "observation_type: Profile",
                "separator: ','",
                "main_column: z_value",
                "date_column: station_date",
                "date_format: yyyy-MM-dd'T'HH:mm:ss'Z'",
                "longitude_column: longitude",
                "latitude_column: latitude",
                "observed_properties_columns_filters:",
                "- '30'",
                "- '35'",
                "- '66'",
                "remove_previous_integration: true",
                "store_id: observationCsvFlatFile",
                "format: 'text/csv; subtype=\"om\"'",
                "result_column: parameter_value",
                "observed_properties_columns:",
                "- parameter_code",
                "type_column : file_type"
        );
        Files.write(tempFile, listYamlParameter, StandardOpenOption.APPEND);

        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(AdminProcessRegistry.NAME, "yamlReader");
        ParameterValueGroup in = desc.getInputDescriptor().createValue();

        in.parameter(ProcessFromYamlProcessDescriptor.DATA_FOLDER_NAME).setValue(tempFile);
        org.geotoolkit.process.Process process = desc.createProcess(in); // Create the process

        process.call();// Call the process.

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24T00:25:47.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2020-03-24T08:48:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(11, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        verifySamplingFeatureNotSame(fois);
        String foi = verifySamplingFeature(fois, 68.2395, -61.4234);

        Assert.assertNotNull(foi);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("30", "35", "66", "z_value"));


        Object o = worker.getObservation(new GetObservationType("2.0.0", "SOS",Arrays.asList(offp.getId()), null, Arrays.asList(sensorId), null, null, null,null));
        Assert.assertTrue(o instanceof ObservationCollection);

        ObservationCollection oc = (ObservationCollection)o;

        String observedProperty = null;
        for (Observation obs : oc.getMember()) {
            if (obs.getFeatureOfInterest() instanceof SamplingFeature) {
                SamplingFeature sf = (SamplingFeature) obs.getFeatureOfInterest();
                if (sf.getId().equals(foi)) {
                    observedProperty = ((Phenomenon)obs.getObservedProperty()).getName().getCode();
                }
            }
        }
        Assert.assertNotNull(observedProperty);

        /*
         * Verify an inserted profile
         */
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Assert.assertEquals(11, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, "2020-03-24T00:25:47Z", -35.27835, -3.61021);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9566, nbMeasure);
    }


}
