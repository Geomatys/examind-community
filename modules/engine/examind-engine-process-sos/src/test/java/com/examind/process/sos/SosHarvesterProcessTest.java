/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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

import com.examind.process.admin.AdminProcessRegistry;
import com.examind.process.admin.yamlReader.ProcessFromYamlProcessDescriptor;
import com.examind.sensor.component.SensorServiceBusiness;
import com.examind.sts.core.STSWorker;
import com.examind.sts.core.temporary.DataArrayResponseExt;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.constellation.admin.SpringHelper;
import org.constellation.admin.WSEngine;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.exception.ConstellationException;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.sos.core.SOSworker;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.TestResource;
import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.gml.xml.AbstractFeature;
import org.geotoolkit.gml.xml.FeatureCollection;
import org.geotoolkit.gml.xml.FeatureProperty;
import org.geotoolkit.gml.xml.LineString;
import org.geotoolkit.gml.xml.v311.TimePeriodType;
import org.geotoolkit.gml.xml.v321.PointType;
import org.geotoolkit.internal.geojson.binding.GeoJSONFeature;
import org.geotoolkit.internal.geojson.binding.GeoJSONGeometry.GeoJSONPoint;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.sos.xml.Capabilities;
import org.geotoolkit.sos.xml.Contents;
import org.geotoolkit.sos.xml.ObservationOffering;
import org.geotoolkit.sos.xml.v200.GetCapabilitiesType;
import org.geotoolkit.sos.xml.v200.GetFeatureOfInterestType;
import org.geotoolkit.sos.xml.v200.GetObservationResponseType;
import org.geotoolkit.sos.xml.v200.GetObservationType;
import org.geotoolkit.sos.xml.v200.GetResultResponseType;
import org.geotoolkit.sos.xml.v200.GetResultType;
import org.geotoolkit.sts.GetHistoricalLocations;
import org.geotoolkit.sts.GetMultiDatastreamById;
import org.geotoolkit.sts.GetObservations;
import org.geotoolkit.sts.GetObservedProperties;
import org.geotoolkit.sts.GetObservedPropertyById;
import org.geotoolkit.sts.GetThingById;
import org.geotoolkit.sts.json.DataArrayResponse;
import org.geotoolkit.sts.json.HistoricalLocation;
import org.geotoolkit.sts.json.HistoricalLocationsResponse;
import org.geotoolkit.sts.json.MultiDatastream;
import org.geotoolkit.sts.json.ObservationsResponse;
import org.geotoolkit.sts.json.ObservedPropertiesResponse;
import org.geotoolkit.sts.json.ObservedProperty;
import org.geotoolkit.sts.json.Thing;
import org.geotoolkit.sts.json.UnitOfMeasure;
import org.geotoolkit.swe.xml.Phenomenon;
import org.geotoolkit.swe.xml.v200.DataArrayPropertyType;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.observation.Observation;
import org.opengis.observation.ObservationCollection;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.temporal.Period;
import org.opengis.util.NoSuchIdentifierException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SosHarvesterProcessTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    private static boolean initialized = false;

    private static Path DATA_DIRECTORY;

    // CSV dir
    private static Path argoDirectory;
    private static Path fmlwDirectory;
    private static Path mooDirectory;
    private static Path multiPlatDirectory;
    private static Path bigdataDirectory;
    private static Path survalDirectory;
    private static Path noHeadDirectory;

    // XLS dir
    private static Path xDataDirectory;

    // DBF dir
    private static Path ltDirectory;
    private static Path rtDirectory;

    @Inject
    protected IServiceBusiness serviceBusiness;
    @Inject
    protected IDatasourceBusiness datasourceBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected ISensorBusiness sensorBusiness;
    @Inject
    protected SensorServiceBusiness sensorServBusiness;
    @Inject
    protected IDatasetBusiness datasetBusiness;

    @Inject
    protected WSEngine wsEngine;


    private static final int ORIGIN_NB_SENSOR = 15;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final Path configDir = Paths.get("target");
        DATA_DIRECTORY      = configDir.resolve("data" + UUID.randomUUID());
        argoDirectory       = DATA_DIRECTORY.resolve("argo-profile");
        Files.createDirectories(argoDirectory);
        fmlwDirectory       = DATA_DIRECTORY.resolve("fmlw-traj");
        Files.createDirectories(fmlwDirectory);
        mooDirectory       = DATA_DIRECTORY.resolve("moo-ts");
        Files.createDirectories(mooDirectory);
        ltDirectory       = DATA_DIRECTORY.resolve("lt-ts");
        Files.createDirectories(ltDirectory);
        rtDirectory       = DATA_DIRECTORY.resolve("rt-ts");
        Files.createDirectories(rtDirectory);
        multiPlatDirectory = DATA_DIRECTORY.resolve("multi-plat");
        Files.createDirectories(multiPlatDirectory);
        bigdataDirectory = DATA_DIRECTORY.resolve("bigdata-profile");
        Files.createDirectories(bigdataDirectory);
        survalDirectory = DATA_DIRECTORY.resolve("surval");
        Files.createDirectories(survalDirectory);
        xDataDirectory = DATA_DIRECTORY.resolve("xdata");
        Files.createDirectories(xDataDirectory);
        noHeadDirectory = DATA_DIRECTORY.resolve("noHead");
        Files.createDirectories(noHeadDirectory);

        writeResourceDataFile(argoDirectory, "com/examind/process/sos/argo-profiles-2902402-1.csv", "argo-profiles-2902402-1.csv");
        writeResourceDataFile(fmlwDirectory, "com/examind/process/sos/tsg-FMLW-1.csv", "tsg-FMLW-1.csv");
        writeResourceDataFile(fmlwDirectory, "com/examind/process/sos/tsg-FMLW-2.csv", "tsg-FMLW-2.csv");
        writeResourceDataFile(fmlwDirectory, "com/examind/process/sos/tsg-FMLW-3.csv", "tsg-FMLW-3.csv");
        writeResourceDataFile(mooDirectory,  "com/examind/process/sos/mooring-buoys-time-series-62069.csv", "mooring-buoys-time-series-62069.csv");
        writeResourceDataFile(ltDirectory,   "com/examind/process/sos/LakeTile_001.dbf", "LakeTile_001.dbf");
        writeResourceDataFile(ltDirectory,   "com/examind/process/sos/LakeTile_002.dbf", "LakeTile_002.dbf");
        writeResourceDataFile(rtDirectory,   "com/examind/process/sos/rivertile_001.dbf", "rivertile_001.dbf");
        writeResourceDataFile(rtDirectory,   "com/examind/process/sos/rivertile_002.dbf", "rivertile_002.dbf");
        writeResourceDataFile(multiPlatDirectory,   "com/examind/process/sos/multiplatform-1.csv", "multiplatform-1.csv");
        writeResourceDataFile(multiPlatDirectory,   "com/examind/process/sos/multiplatform-2.csv", "multiplatform-2.csv");
        writeResourceDataFile(bigdataDirectory, "com/examind/process/sos/bigdata-1.csv", "bigdata-1.csv");
        writeResourceDataFile(survalDirectory, "com/examind/process/sos/surval-small.csv", "surval-small.csv");
        writeResourceDataFile(xDataDirectory, "com/examind/process/sos/xdata.xlsx", "xdata.xlsx");
        writeResourceDataFile(noHeadDirectory, "com/examind/process/sos/nohead.csv", "nohead.csv");

    }

    @PostConstruct
    public void setUp() throws Exception {
        if (!initialized) {
            // clean up
            serviceBusiness.deleteAll();
            providerBusiness.removeAll();
            datasourceBusiness.deleteAll();

            Integer pid = testResources.createProvider(TestResource.OM2_DB, providerBusiness, null).id;

            //we write the configuration file
            final SOSConfiguration configuration = new SOSConfiguration();
            configuration.getParameters().put(TRANSACTION_SECURIZED, "false");

            Integer sid = serviceBusiness.create("sos", "default", configuration, null, null);
            serviceBusiness.linkServiceAndProvider(sid, pid);
            serviceBusiness.start(sid);

            sid = serviceBusiness.create("sts", "default", configuration, null, null);
            serviceBusiness.linkServiceAndProvider(sid, pid);
            serviceBusiness.start(sid);

            initialized = true;
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            IServiceBusiness sb = SpringHelper.getBean(IServiceBusiness.class).orElse(null);
            if (sb != null) {
                sb.deleteAll();
            }
            IProviderBusiness pb = SpringHelper.getBean(IProviderBusiness.class).orElse(null);
            if (pb != null) {
                pb.removeAll();
            }
            IDatasourceBusiness dsb = SpringHelper.getBean(IDatasourceBusiness.class).orElse(null);
            if (dsb != null) {
                dsb.deleteAll();
            }
            File derbyLog = new File("derby.log");
            if (derbyLog.exists()) {
                derbyLog.delete();
            }
            File mappingFile = new File("mapping.properties");
            if (mappingFile.exists()) {
                mappingFile.delete();
            }
            IOUtilities.deleteSilently(DATA_DIRECTORY);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Test
    @Order(order = 1)
    public void harvestCSVProfileTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        sensorServBusiness.removeAllSensors(sc.getId());

        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);
        sensorServBusiness.removeAllSensors(sc2.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        
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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-13T03:55:49.000", time.getEndPosition().getValue());

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(4, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2018-11-02T07:10:52Z", -6.81, 44.06);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1609, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP (degree_Celsius)", "PSAL (psu)", "PRES (decibar)"));

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        verifySamplingFeature(fois,  "251",  -6.81,  44.06);
        verifySamplingFeature(fois,  "252",  -6.581, 44.01);
        verifySamplingFeature(fois,  "253",  -6.256, 43.959);
        verifySamplingFeature(fois,  "254",  -6.035, 44.031);

        /*
         * add a new file to integrate and call again the process
         */
        writeResourceDataFile(argoDirectory, "com/examind/process/sos/argo-profiles-2902402-2.csv", "argo-profiles-2902402-2.csv");
        proc.call();

        offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);
        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-11-02T07:10:52.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-27T15:09:17.000", time.getEndPosition().getValue());

        Assert.assertEquals(8, offp.getFeatureOfInterestIds().size());
        Assert.assertEquals(1, offp.getObservedProperties().size());

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
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/argo-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(8, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2018-11-02T07:10:52Z", -6.81, 44.06);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2018-11-05T22:03:31Z", -6.581, 44.01);

        HistoricalLocation loc8 = response.getValue().get(7);
        verifyHistoricalLocation(loc8, sdf, "2018-11-27T15:09:17Z", -5.04, 44.154);

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

        offp = getOffering(worker, sensorId);
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
        verifyHistoricalLocation(loc1, sdf, "2018-11-02T07:10:52Z", -6.81, 44.06);

        nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1609, nbMeasure);

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());

        fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
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
    @Order(order = 2)
    public void harvestCSVTrajTest() throws Exception {

        ServiceComplete sos = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sos);
        sensorServBusiness.removeAllSensors(sos.getId());

        ServiceComplete sts = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sts);
        sensorServBusiness.removeAllSensors(sts.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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
        in.parameter(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).setValue(new ServiceProcessReference(sos));

        ParameterValue val1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        val1.setValue(new ServiceProcessReference(sos));
        in.values().add(val1);
        ParameterValue val2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        val2.setValue(new ServiceProcessReference(sts));
        in.values().add(val2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(worker, sensorId);
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

        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, null));
        String expectedResult =  getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');


        String resultForFoi1 = getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values-1.txt");
        String resultForFoi2 = getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values-2.txt");
        String resultForFoi3 = getResourceAsString("com/examind/process/sos/tsg-FMLW-datablock-values-3.txt");


        String foi = offp.getFeatureOfInterestIds().get(0);
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String result = gr.getResultValues().toString() + '\n';
        Assert.assertTrue(result.equals(resultForFoi1) ||  result.equals(resultForFoi2) || result.equals(resultForFoi3));

        foi = offp.getFeatureOfInterestIds().get(1);
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        result = gr.getResultValues().toString() + '\n';
        Assert.assertTrue(result.equals(resultForFoi1) ||  result.equals(resultForFoi2) || result.equals(resultForFoi3));

        foi = offp.getFeatureOfInterestIds().get(2);
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        result = gr.getResultValues().toString() + '\n';
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
    @Order(order = 3)
    public void harvestCSVTSTest() throws Exception {

        ServiceComplete sos = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sos);
        sensorServBusiness.removeAllSensors(sos.getId());

        ServiceComplete sts = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sts);
        sensorServBusiness.removeAllSensors(sts.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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
        serv1.setValue(new ServiceProcessReference(sos));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sts));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

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

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-10-30T00:29:00.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-30T11:59:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        verifySamplingFeature(fois, -4.9683,  48.2903);

        String observedProperty = offp.getObservedProperties().get(0);
        String foi = offp.getFeatureOfInterestIds().get(0);


        /*
        * Verify an inserted profile
        */
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", sensorId);
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(1509, nbMeasure);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP LEVEL0 (degree_Celsius)", "VEPK LEVEL0 (meter2 second)"));
    }

    @Test
    @Order(order = 4)
    public void harvestTS2Test() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        sensorServBusiness.removeAllSensors(sc.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2018-10-30", time.getBeginPosition().getValue());
        Assert.assertEquals("2018-11-30T12:30:00.000", time.getEndPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeature(fois, -4.9683,  48.2903);

        Assert.assertEquals(1, offp.getObservedProperties().size());
        String observedProperty = offp.getObservedProperties().get(0);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("TEMP LEVEL0 (degree_Celsius)", "VZMX LEVEL0 (meter)"));

        /*
        * Verify an inserted timeSeries
        */
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        /*
        * Verify an inserted timeSeries
        */
        observedProperty = "VZMX LEVEL0 (meter)";
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/mooring-datablock-values-3.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        Object obj = worker.getObservation(new GetObservationType("2.0.0", offp.getId(), null, Arrays.asList(sensorId), Arrays.asList(observedProperty), new ArrayList<>(), null));

        Assert.assertTrue(obj instanceof GetObservationResponseType);

        GetObservationResponseType result = (GetObservationResponseType) obj;

        Assert.assertTrue(result.getObservationData().get(0).getOMObservation().getResult() instanceof DataArrayPropertyType);

        DataArrayPropertyType daResult = (DataArrayPropertyType) result.getObservationData().get(0).getOMObservation().getResult();

        String value = daResult.getDataArray().getValues();
        Assert.assertEquals(expectedResult, value + '\n');

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(3020, nbMeasure);
    }

    @Test
    @Order(order = 4)
    public void harvestDBFTSTest() throws Exception {
        ServiceComplete sos = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sos);
        sensorServBusiness.removeAllSensors(sos.getId());

        ServiceComplete sts = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sts);
        sensorServBusiness.removeAllSensors(sts.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

        Assert.assertEquals(ORIGIN_NB_SENSOR, prev);

        String sensorId = "urn:sensor:dbf:1";

        String datasetId = "SOS_DATA";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, SosHarvesterProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(SosHarvesterProcessDescriptor.DATASET_IDENTIFIER_NAME).setValue(datasetId);
        in.parameter(SosHarvesterProcessDescriptor.DATA_FOLDER_NAME).setValue(ltDirectory.toUri().toString());

        in.parameter(SosHarvesterProcessDescriptor.STORE_ID_NAME).setValue("observationDbfFile");
        in.parameter(SosHarvesterProcessDescriptor.FORMAT_NAME).setValue("application/dbase; subtype=\"om\"");

        in.parameter(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME).setValue("time_str");
        in.parameter(SosHarvesterProcessDescriptor.MAIN_COLUMN_NAME).setValue("time_str");

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd' 'HH:mm:ss");
        in.parameter(SosHarvesterProcessDescriptor.LATITUDE_COLUMN_NAME).setValue("LATITUDE (degree_north)");
        in.parameter(SosHarvesterProcessDescriptor.LONGITUDE_COLUMN_NAME).setValue("LONGITUDE (degree_east)");

        in.parameter(SosHarvesterProcessDescriptor.FOI_COLUMN_NAME).setValue("prior_id");

        in.parameter(SosHarvesterProcessDescriptor.OBS_PROP_COLUMN_NAME).setValue("height");

        in.parameter(SosHarvesterProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");
        in.parameter(SosHarvesterProcessDescriptor.THING_ID_NAME).setValue(sensorId);
        in.parameter(SosHarvesterProcessDescriptor.REMOVE_PREVIOUS_NAME).setValue(false);
        ParameterValue serv1 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv1.setValue(new ServiceProcessReference(sos));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sts));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2022-08-20T01:55:11.000", time.getBeginPosition().getValue());
        Assert.assertEquals("2022-08-28T10:58:39.000", time.getEndPosition().getValue());

        Assert.assertEquals(4, offp.getFeatureOfInterestIds().size());
        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());

        Assert.assertEquals(1, offp.getObservedProperties().size());
        String observedProperty = offp.getObservedProperties().get(0);
        Assert.assertEquals("height", observedProperty);

        /*
         * Verify an inserted time serie
         */
        String foi = verifySamplingFeature(fois, "54008001708");
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/LakeTile_foi-1.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        foi = verifySamplingFeature(fois,        "54008001586");
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/LakeTile_foi-2.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        verifySamplingFeature(fois,        ""); // TODO verify why we have a empty id here
        verifySamplingFeature(fois,        "54008001446;54008001453");

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9, nbMeasure);
    }

    @Test
    @Order(order = 5)
    public void harvestDBFTS2Test() throws Exception {
        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        sensorServBusiness.removeAllSensors(sc.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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

        in.parameter(SosHarvesterProcessDescriptor.DATE_FORMAT_NAME).setValue("yyyy-MM-dd'T'HH:mm:ss'Z'");
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
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor(sensorId));

        ObservationOffering offp = getOffering(worker, sensorId);
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2022-06-06T00:58:50.921", time.getBeginPosition().getValue());
        Assert.assertEquals("2022-06-15T23:21:00.641", time.getEndPosition().getValue());

        Assert.assertEquals(64, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());

        Assert.assertEquals(1, offp.getObservedProperties().size());

        String observedProperty = offp.getObservedProperties().get(0);

        verifyAllObservedProperties(stsWorker, sensorId, Arrays.asList("height", "width"));

        /*
        * Verify an inserted time serie
        */
        String foi = verifySamplingFeature(fois, "8403780.0", 2.074899266154643, 45.22470466446091);
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/rivertile_foi-1.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        foi = verifySamplingFeature(fois, "8403781.0", 2.07361239284379, 45.224199842811814);
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/rivertile_foi-2.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(85, nbMeasure);

    }

    @Test
    @Order(order = 6)
    public void harvestCSVTSMultiPlatformTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);
        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);

        sensorServBusiness.removeAllSensors(sc.getId());
        sensorServBusiness.removeAllSensors(sc2.getId());

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
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("p001"));
        Assert.assertNotNull(sensorBusiness.getSensor("p002"));
        Assert.assertNotNull(sensorBusiness.getSensor("p003"));

         // verify that the sensor is linked to the services
        Assert.assertTrue(sensorBusiness.isLinkedSensor(sc.getId(), "p001"));
        Assert.assertTrue(sensorBusiness.isLinkedSensor(sc2.getId(), "p001"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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

        String observedProperty = offp.getObservedProperties().get(0);

        /*
        * Verify an inserted data
        */
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/multi-platform-values-p001.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "p001");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(2, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2000-07-28T00:30:00Z", -6.9, 49.4);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2000-07-29T23:00:00Z", -6.8, 49.5);

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

        observedProperty = offp.getObservedProperties().get(0);

        /*
        * Verify an inserted data
        */
        gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/multi-platform-values-p002.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "p002");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(2, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2000-07-30T00:00:00Z", -6.9, 49.4);

        loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2000-07-30T02:30:00Z", -6.9, 49.4);

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

        observedProperty = offp.getObservedProperties().get(0);

        /*
        * Verify an inserted data
        */
        gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/multi-platform-values-p003.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "p003");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2000-07-31T11:00:00Z", -5.3, 51.2);

        nbMeasure = getNbMeasure(stsWorker, "p003");
        Assert.assertEquals(35, nbMeasure);

    }

    @Test
    @Order(order = 6)
    public void harvesterCSVFlatProfileSingleTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);
        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);

        sensorServBusiness.removeAllSensors(sc.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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
        proc.call();

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
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:25:47Z", -35.27835, -3.61021);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9566, nbMeasure);
    }

    @Test
    @Order(order = 6)
    public void harvesterCSVFlatProfileTest() throws Exception {

        ServiceComplete sos = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sos);
        sensorServBusiness.removeAllSensors(sos.getId());

        ServiceComplete sts = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sts);
        sensorServBusiness.removeAllSensors(sts.getId());

        SOSworker worker = (SOSworker) wsEngine.buildWorker("sos", "default");
        worker.setServiceUrl("http://localhost/examind/");

        STSWorker stsWorker = (STSWorker) wsEngine.buildWorker("sts", "default");
        stsWorker.setServiceUrl("http://localhost/examind/");

        int prev = getNbOffering(worker, 0);

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
        serv1.setValue(new ServiceProcessReference(sos));
        in.values().add(serv1);
        ParameterValue serv2 = (ParameterValue) desc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.SERVICE_ID_NAME).createValue();
        serv2.setValue(new ServiceProcessReference(sts));
        in.values().add(serv2);

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensors has been created
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1901290"));
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1901689"));
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1901710"));


        Assert.assertNull(sensorBusiness.getSensor("urn:template:666999"));

        Assert.assertEquals(11, getNbOffering(worker, prev));

        ObservationOffering offp = getOffering(worker, "urn:template:1901290");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        TimePeriodType time = (TimePeriodType) offp.getTime();


        // ???
        Assert.assertEquals("2020-03-24T05:07:54.000", time.getBeginPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        List<SamplingFeature> fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        String foi = verifySamplingFeature(fois, 68.2395, -61.4234);

        Assert.assertNotNull(foi);

        //-61.4234,68.2395
        String observedProperty = offp.getObservedProperties().get(0);

        /*
         * Verify an inserted profile
         */
        GetResultResponseType gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1901290");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Assert.assertEquals(1, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T05:07:54Z", 68.2395, -61.4234);

        int nbMeasure = getNbMeasure(stsWorker, "urn:template:1901290");
        Assert.assertEquals(68, nbMeasure);

       /*
        * second
        */

        offp = getOffering(worker, "urn:template:1901689");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();

        Assert.assertEquals("2020-03-24T08:48:00.000", time.getBeginPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());

        fois  = getFeatureOfInterest(worker, offp.getFeatureOfInterestIds());
        foi = verifySamplingFeature(fois, -25.92446, 5.92986);

        Assert.assertNotNull(foi);

        observedProperty = offp.getObservedProperties().get(0);

        /*
         * Verify an inserted profile
         */
        gr = (GetResultResponseType) worker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-4.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1901689");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T08:48:00Z", -25.92446, 5.92986);

        nbMeasure = getNbMeasure(stsWorker, "urn:template:1901689");
        Assert.assertEquals(503, nbMeasure);

        // verify that all the sensors have at least one of the three observed properties
        List<String> sensorIds = sensorBusiness.getLinkedSensorIdentifiers(sos.getId(), null);
        for (String sid : sensorIds) {
            if (sid.startsWith("urn:template:")) {
                verifyObservedProperties(stsWorker, sid, Arrays.asList("30", "35", "66"));
            }
        }
    }

    @Test
    @Order(order = 7)
    public void harvesterCSVFlatTSTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);
        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);

        sensorServBusiness.removeAllSensors(sc.getId());
        sensorServBusiness.removeAllSensors(sc2.getId());

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
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1501563"));
        Assert.assertNotNull(sensorBusiness.getSensor("urn:template:1501564"));

        // not matching the measure
        Assert.assertNull(sensorBusiness.getSensor("urn:template:1301603"));

        Assert.assertEquals(301, getNbOffering(sosWorker, prev));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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

        /*
        * Verify an inserted data
        */
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-2-quality.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString().replace("@@", "@@\n"));

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1501563");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:00:00Z", -20.539, -29.9916);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2020-03-24T00:30:00Z", -20.5456, -29.995);

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
        gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-3-quality.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString().replace("@@", "@@\n"));

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:template:1501564");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:00:00Z", -23.209, -30.3464);

        loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2020-03-24T00:30:00Z", -23.2064, -30.3484);

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

        Assert.assertEquals(1, offp.getObservedProperties().size());
        observedProperty = offp.getObservedProperties().get(0); // composite

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
    @Order(order = 7)
    public void harvesterCSVFlatMultiTypeTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);
        sensorServBusiness.removeAllSensors(sc.getId());

        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);
        sensorServBusiness.removeAllSensors(sc2.getId());

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
        proc.call();

        // verify that the dataset has been created
        Assert.assertNotNull(datasetBusiness.getDatasetId(datasetId));

        // verify that the sensor has been created
        Assert.assertNotNull(sensorBusiness.getSensor("1501563"));
        Assert.assertNotNull(sensorBusiness.getSensor("1501564"));

        // not matching the measure
        Assert.assertNull(sensorBusiness.getSensor("urn:template:1301603"));

        Assert.assertEquals(312, getNbOffering(sosWorker, prev));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1501563");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:00:00Z", -20.539, -29.9916);

        HistoricalLocation loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2020-03-24T00:30:00Z", -20.5456, -29.995);

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
        gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-3.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1501564");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(21, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:00:00Z", -23.209, -30.3464);

        loc2 = response.getValue().get(1);
        verifyHistoricalLocation(loc2, sdf, "2020-03-24T00:30:00Z", -23.2064, -30.3484);

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

        Assert.assertEquals(1, offp.getObservedProperties().size());
        observedProperty = offp.getObservedProperties().get(0); // composite
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

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();


        // ???
        Assert.assertEquals("2020-03-24T05:07:54.000", time.getBeginPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi  = verifySamplingFeature(fois, 68.2395, -61.4234);

        observedProperty = offp.getObservedProperties().get(0);

        /*
         * Verify an inserted profile
         */
        gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-5.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1901290");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T05:07:54Z", 68.2395, -61.4234);

        nbMeasure = getNbMeasure(stsWorker, "1901290");
        Assert.assertEquals(68, nbMeasure);

       /*
        * sixth (PROFILE)
        */

        offp = getOffering(sosWorker, "1901689");
        Assert.assertNotNull(offp);

        Assert.assertTrue(offp.getTime() instanceof TimePeriodType);
        time = (TimePeriodType) offp.getTime();


        // ???
        Assert.assertEquals("2020-03-24T08:48:00.000", time.getBeginPosition().getValue());

        Assert.assertEquals(1, offp.getFeatureOfInterestIds().size());
        fois = getFeatureOfInterest(sosWorker, offp.getFeatureOfInterestIds());
        foi  = verifySamplingFeature(fois, -25.92446, 5.92986);

        //-61.4234,68.2395
        observedProperty = offp.getObservedProperties().get(0);

        /*
         * Verify an inserted profile
         */
        gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        expectedResult = getResourceAsString("com/examind/process/sos/bigdata-datablock-values-6.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "1901689");
        hl.getExpand().add("Locations");
        response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T08:48:00Z", -25.92446, 5.92986);

        nbMeasure = getNbMeasure(stsWorker, "1901689");
        Assert.assertEquals(503, nbMeasure);

    }

    @Test
    @Order(order = 8)
    public void harvesterCSVSurvalTSTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);

        sensorServBusiness.removeAllSensors(sc.getId());
        sensorServBusiness.removeAllSensors(sc2.getId());

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
        proc.call();

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

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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

        Assert.assertEquals(1, offp.getObservedProperties().size());
        String observedProperty = offp.getObservedProperties().get(0);

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
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, Arrays.asList(foi)));
        String expectedResult = getResourceAsString("com/examind/process/sos/surval-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:surval:25049001");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        Assert.assertEquals(1, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2007-12-18T00:00:00Z", -3.093748, 47.534765);
        int nbMeasure = getNbMeasure(stsWorker, "urn:surval:25049001");
        Assert.assertEquals(791, nbMeasure);

    }

    @Test
    @Order(order = 8)
    public void harvesterXLSTSTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);

        sensorServBusiness.removeAllSensors(sc.getId());
        sensorServBusiness.removeAllSensors(sc2.getId());

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
        proc.call();

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
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, null));
        String expectedResult = getResourceAsString("com/examind/process/sos/xdata-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        int nbMeasure = getNbMeasure(stsWorker, "urn:xdata:cap-90");
        Assert.assertEquals(3, nbMeasure);

    }

    @Test
    @Order(order = 8)
    public void harvesterCSVNoHeaderTSTest() throws Exception {

        ServiceComplete sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);

        ServiceComplete sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);

        sensorServBusiness.removeAllSensors(sc.getId());
        sensorServBusiness.removeAllSensors(sc2.getId());

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
        proc.call();

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
        GetResultResponseType gr = (GetResultResponseType) sosWorker.getResult(new GetResultType("2.0.0", "SOS", offp.getId(), observedProperty, null, null, null));
        String expectedResult = getResourceAsString("com/examind/process/sos/nohead-datablock-values.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        int nbMeasure = getNbMeasure(stsWorker, sensorID);
        Assert.assertEquals(3, nbMeasure);

    }

    /**
     * Same test as harvesterCSVFlatProfileSingleTest but the process SosHarvester is called from the ProcessFromYamlProcess.
     */
    @Test
    @Order(order = 9)
    public void harvesterCSVFlatProfileSingleFromYamlTest() throws ConstellationException, NoSuchIdentifierException, ProcessException, IOException, ParseException {
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
        Process process = desc.createProcess(in); // Create the process

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
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:25:47Z", -35.27835, -3.61021);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9566, nbMeasure);
    }

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
        Process process = desc.createProcess(in); // Create the process

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
            if (obs.getFeatureOfInterest() instanceof SamplingFeature) {
                SamplingFeature sf = (SamplingFeature) obs.getFeatureOfInterest();
                if (sf.getId().equals(foi)) {
                    observedProperty = ((Phenomenon)obs.getObservedProperty()).getName().getCode();
                }
            }
            Assert.assertNotNull(obs.getSamplingTime());
            Assert.assertTrue(obs.getSamplingTime() instanceof Period);
            Period period = (Period) obs.getSamplingTime();
            Assert.assertNotNull(period.getBeginning());
            Assert.assertNotNull(period.getBeginning().getDate());
            Assert.assertNotNull(period.getEnding());
            Assert.assertNull(period.getEnding().getDate());
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
        verifyHistoricalLocation(loc1, sdf, "2020-03-24T00:25:47Z", -35.27835, -3.61021);

        int nbMeasure = getNbMeasure(stsWorker, sensorId);
        Assert.assertEquals(9566, nbMeasure);
    }

    /**
     * Same test as harvesterCSVFlatProfileSingleTest but the process SosHarvester is called from the ProcessFromYamlProcess.
     */
    @Test
    @Order(order = 9)
    public void harvesterCSVSurvalProfileSingleFromYamlTest() throws ConstellationException, NoSuchIdentifierException, ProcessException, IOException, ParseException {
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
        Process process = desc.createProcess(in); // Create the process

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
        String expectedResult = getResourceAsString("com/examind/process/sos/surval-datablock-values-2.txt");
        Assert.assertEquals(expectedResult, gr.getResultValues().toString() + '\n');

        GetHistoricalLocations hl = new GetHistoricalLocations();
        hl.getExtraFilter().put("procedure", "urn:sensor:surval:60007755");
        hl.getExpand().add("Locations");
        HistoricalLocationsResponse response = stsWorker.getHistoricalLocations(hl);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        Assert.assertEquals(1, response.getValue().size());

        HistoricalLocation loc1 = response.getValue().get(0);
        verifyHistoricalLocation(loc1, sdf, "2015-09-26T00:00:00Z", 15.3275447596057, 37.7923499263524);

        int nbMeasure = getNbMeasure(stsWorker, "urn:sensor:surval:60007755");
        Assert.assertEquals(4, nbMeasure);
    }

    private static List<String> getObservedProperties(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        List<String> results = new ArrayList<>();
        GetObservedProperties request = new GetObservedProperties();
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        ObservedPropertiesResponse resp = stsWorker.getObservedProperties(request);
        for (ObservedProperty op : resp.getValue()) {
            results.add(op.getIotId());
        }
        return results;
    }

    private static Thing getThing(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetThingById request = new GetThingById();
        request.setId(sensorId);
        return stsWorker.getThingById(request);
    }

    private static List<ObservedProperty> getFullObservedProperties(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        List<ObservedProperty> results = new ArrayList<>();
        GetObservedProperties request = new GetObservedProperties();
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        ObservedPropertiesResponse resp = stsWorker.getObservedProperties(request);
        for (ObservedProperty op : resp.getValue()) {
            results.add(op);
        }
        return results;
    }

    private static ObservedProperty getObservedPropertyById(STSWorker stsWorker, String obsId) throws CstlServiceException {
        GetObservedPropertyById request = new GetObservedPropertyById();
        request.setId(obsId);
        return stsWorker.getObservedPropertyById(request);
    }

    private static void verifyAllObservedProperties(STSWorker stsWorker, String sensorId, List<String> expectedObsProp) throws CstlServiceException {
        List<String> obsProp = getObservedProperties(stsWorker, sensorId);
        boolean ok = obsProp.containsAll(expectedObsProp);
        String msg = "";
        if (!ok) {
            msg = sensorId + " observed properties missing:\n";
            for (String o : expectedObsProp) {
                if (!obsProp.contains(o)) {
                    msg = msg + o + '\n';
                }
            }
        }
        Assert.assertTrue(msg, ok);

        ok = expectedObsProp.containsAll(obsProp);
        msg = "";
        if (!ok) {
            msg = sensorId + " observed properties supplementary:\n";
            for (String o : obsProp) {
                if (!expectedObsProp.contains(o)) {
                    msg = msg + o + '\n';
                }
            }
        }
        Assert.assertTrue(msg, ok);
    }

    private static void verifyObservedProperties(STSWorker stsWorker, String sensorId, List<String> expectedObsProp) throws CstlServiceException {
        List<String> obsProp = getObservedProperties(stsWorker, sensorId);
        boolean ok = false;
        for (String expO : expectedObsProp) {
            if (obsProp.contains(expO)) {
                ok = true;
                break;
            }
        }
        String msg = "";
        if (!ok) {
            msg = sensorId + " observed properties missing:\n";
            for (String o : obsProp) {
                msg = msg + o + '\n';
            }
        }
        Assert.assertTrue(msg, ok);
    }

    private static Integer getNbMeasure(STSWorker stsWorker, String sensorId) throws CstlServiceException {
        GetObservations request = new GetObservations();
        request.setResultFormat("dataArray");
        request.getExtraFlag().put("forMDS", "true");
        request.setCount(true);
        request.getExtraFilter().put("observationId", "urn:ogc:object:observation:template:GEOM:" + sensorId);
        DataArrayResponseExt resp = (DataArrayResponseExt) stsWorker.getObservations(request);
        return resp.getIotCount().toBigInteger().intValue();
    }

    private static ObservationOffering getOffering(SOSworker worker, String sensorId) throws CstlServiceException {
        Capabilities capa        = worker.getCapabilities(new GetCapabilitiesType());
        Contents ct              = capa.getContents();
        ObservationOffering offp = null;
        for (ObservationOffering off : ct.getOfferings()) {
            if (off.getId().equals("offering-" + sensorId.replace(':', '-'))) {
                offp = off;
            }
        }
        return offp;
    }

    private static int getNbOffering(SOSworker worker, int offset) throws CstlServiceException {
        Capabilities capa        = worker.getCapabilities(new GetCapabilitiesType());
        Contents ct              = capa.getContents();
        return ct.getOfferings().size() - offset;
    }

    private static List<SamplingFeature> getFeatureOfInterest(SOSworker worker, List<String> foids) throws CstlServiceException {
        List<SamplingFeature> results = new ArrayList<>();
        AbstractFeature o = worker.getFeatureOfInterest(new GetFeatureOfInterestType("2.0.0", "SOS", foids));
        if (o instanceof FeatureCollection ) {
            for (FeatureProperty fp :  ((FeatureCollection)o).getFeatureMember()) {
                if (fp.getAbstractFeature() instanceof SamplingFeature) {
                    results.add((SamplingFeature)fp.getAbstractFeature());
                }
            }
        } else if (o instanceof SamplingFeature) {
            results.add((SamplingFeature)o);
        }
        return results;
    }

    private String verifySamplingFeatureLine(List<SamplingFeature> fois, int nbPoint) {
       String foi = null;
       for (SamplingFeature sp : fois) {
            if (sp.getGeometry() instanceof LineString) {
                LineString ln = (LineString) sp.getGeometry();
                if (ln.getPosList().getValue().size() == nbPoint*2) {
                    foi = sp.getId();
                }
            }
        }
        Assert.assertNotNull(foi);
        return foi;
    }

    private String verifySamplingFeature(List<SamplingFeature> fois,  double x, double y) {
        return verifySamplingFeature(fois, null, x, y);
    }

    private String verifySamplingFeature(List<SamplingFeature> fois, String id, double x, double y) {
       String foi = null;
       for (SamplingFeature sp : fois) {
            if ((id != null && sp.getId().equals(id)) || id == null)
            if (sp.getGeometry() instanceof PointType) {
                PointType pt = (PointType) sp.getGeometry();
                if (pt.getDirectPosition().getOrdinate(0) == x &&
                    pt.getDirectPosition().getOrdinate(1) == y) {

                    foi = sp.getId();
                }
            }
        }
        Assert.assertNotNull(foi);
        return foi;
    }

    private String verifySamplingFeature(List<SamplingFeature> fois, String id) {
       String foi = null;
       for (SamplingFeature sp : fois) {
            if (sp.getId().equals(id)) {
                foi = sp.getId();
            }
        }
        Assert.assertNotNull(foi);
        return foi;
    }

    private void verifySamplingFeatureNotSame(List<SamplingFeature> fois) {
       Set<String> alreadyFound = new HashSet<>();
       for (SamplingFeature sp : fois) {
            if (sp.getGeometry() instanceof PointType) {
                PointType pt = (PointType) sp.getGeometry();
                String key = pt.getDirectPosition().getOrdinate(0) + "-" + pt.getDirectPosition().getOrdinate(1);
                if (alreadyFound.contains(key)) {
                    throw new IllegalStateException("duplicated feature of interest for coord:" + key);
                }
                alreadyFound.add(pt.getDirectPosition().getOrdinate(0) + "-" + pt.getDirectPosition().getOrdinate(1));
            }
        }
    }

    private static void verifyHistoricalLocation(HistoricalLocation loc1, SimpleDateFormat sdf, String date, double x, double y) throws ParseException {
        Assert.assertEquals(loc1.getTime().getTime(), sdf.parse(date).getTime());
        Assert.assertEquals(1, loc1.getLocations().size());
        Assert.assertTrue(loc1.getLocations().get(0).getLocation() instanceof GeoJSONFeature);
        GeoJSONFeature feat1 = (GeoJSONFeature) loc1.getLocations().get(0).getLocation();
        Assert.assertTrue(feat1.getGeometry() instanceof GeoJSONPoint);
        GeoJSONPoint pt1 = (GeoJSONPoint) feat1.getGeometry();
        Assert.assertEquals(x, pt1.getCoordinates()[0], 0.001);
        Assert.assertEquals(y, pt1.getCoordinates()[1], 0.001);
    }
}
