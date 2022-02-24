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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.process.dynamic.ExamindDynamicProcessFactory;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.Order;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.processing.chain.model.Chain;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class HarvesterPreProcessTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    private static final String confDirName = "HarvesterPreProcessTest" + UUID.randomUUID();

    private static boolean initialized = false;

    private static Path argoDirectory;
    private static Path fmlwDirectory;
    private static Path mooDirectory;

    @Inject
    protected IProcessBusiness processBusiness;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final Path configDir = ConfigDirectory.setupTestEnvironement(confDirName);
        Path dataDirectory  = configDir.resolve("data");
        argoDirectory       = dataDirectory.resolve("argo-profile");
        Files.createDirectories(argoDirectory);
        fmlwDirectory       = dataDirectory.resolve("fmlw-traj");
        Files.createDirectories(fmlwDirectory);
        mooDirectory       = dataDirectory.resolve("moo-ts");
        Files.createDirectories(mooDirectory);

        writeResourceDataFile(argoDirectory, "com/examind/process/sos/argo-profiles-2902402-1.csv", "argo-profiles-2902402-1.csv");
        writeResourceDataFile(argoDirectory, "com/examind/process/sos/argo-profiles-2902402-2.csv", "argo-profiles-2902402-2.csv");

        writeResourceDataFile(fmlwDirectory, "com/examind/process/sos/tsg-FMLW-1.csv", "tsg-FMLW-1.csv");
        writeResourceDataFile(fmlwDirectory, "com/examind/process/sos/tsg-FMLW-2.csv", "tsg-FMLW-1.csv");
        writeResourceDataFile(fmlwDirectory, "com/examind/process/sos/tsg-FMLW-3.csv", "tsg-FMLW-1.csv");

        writeResourceDataFile(mooDirectory,  "com/examind/process/sos/mooring-buoys-time-series-62069.csv", "mooring-buoys-time-series-62069.csv");
    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {

                for (Chain chainDesc : ChainProcessRetriever.getChainModels()) {
                    processBusiness.deleteChainProcess(ExamindDynamicProcessFactory.NAME, chainDesc.getName());
                }
                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            File derbyLog = new File("derby.log");
            if (derbyLog.exists()) {
                derbyLog.delete();
            }
            File mappingFile = new File("mapping.properties");
            if (mappingFile.exists()) {
                mappingFile.delete();
            }
            ConfigDirectory.shutdownTestEnvironement(confDirName);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Test
    @Order(order = 1)
    public void harvestProfileTest() throws Exception {
        final String processId = "generated-1";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, HarvesterPreProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(HarvesterPreProcessDescriptor.TASK_NAME_NAME).setValue(processId);
        in.parameter(HarvesterPreProcessDescriptor.DATA_FOLDER_NAME).setValue(argoDirectory.toUri().toString());

        in.parameter(HarvesterPreProcessDescriptor.OBS_TYPE_NAME).setValue("Profile");

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the task has been created
        final ProcessDescriptor generatedDesc = ProcessFinder.getProcessDescriptor(ExamindDynamicProcessFactory.NAME, processId);
        Assert.assertNotNull(generatedDesc);

        // verify that the sensor has been created
        GeneralParameterDescriptor mainParamDesc = generatedDesc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME);
        Assert.assertNotNull(mainParamDesc);

        Assert.assertTrue(mainParamDesc instanceof ExtendedParameterDescriptor);

        ExtendedParameterDescriptor mainParamExt = (ExtendedParameterDescriptor) mainParamDesc;

        Assert.assertEquals("DATE (YYYY-MM-DDTHH:MI:SSZ)", mainParamExt.getDefaultValue());

        Assert.assertEquals(22, mainParamExt.getValidValues().size());

    }

    @Test
    @Order(order = 1)
    public void harvestTrajTest() throws Exception {
        final String processId = "generated-2";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, HarvesterPreProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(HarvesterPreProcessDescriptor.TASK_NAME_NAME).setValue(processId);
        in.parameter(HarvesterPreProcessDescriptor.DATA_FOLDER_NAME).setValue(fmlwDirectory.toUri().toString());

        in.parameter(HarvesterPreProcessDescriptor.OBS_TYPE_NAME).setValue("Trajectory");

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the task has been created
        final ProcessDescriptor generatedDesc = ProcessFinder.getProcessDescriptor(ExamindDynamicProcessFactory.NAME, processId);
        Assert.assertNotNull(generatedDesc);

        // verify that the sensor has been created
        GeneralParameterDescriptor mainParamDesc = generatedDesc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME);
        Assert.assertNotNull(mainParamDesc);

        Assert.assertTrue(mainParamDesc instanceof ExtendedParameterDescriptor);

        ExtendedParameterDescriptor mainParamExt = (ExtendedParameterDescriptor) mainParamDesc;

        Assert.assertEquals("DATE (yyyy-mm-ddThh:mi:ssZ)", mainParamExt.getDefaultValue());

        Assert.assertEquals(10, mainParamExt.getValidValues().size());

    }

    @Test
    @Order(order = 1)
    public void harvestTSTest() throws Exception {
        final String processId = "generated-3";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, HarvesterPreProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(HarvesterPreProcessDescriptor.TASK_NAME_NAME).setValue(processId);
        in.parameter(HarvesterPreProcessDescriptor.DATA_FOLDER_NAME).setValue(mooDirectory.toUri().toString());

        in.parameter(HarvesterPreProcessDescriptor.OBS_TYPE_NAME).setValue("Timeserie");

        org.geotoolkit.process.Process proc = desc.createProcess(in);
        proc.call();

        // verify that the task has been created
        final ProcessDescriptor generatedDesc = ProcessFinder.getProcessDescriptor(ExamindDynamicProcessFactory.NAME, processId);
        Assert.assertNotNull(generatedDesc);

        // verify that the sensor has been created
        GeneralParameterDescriptor mainParamDesc = generatedDesc.getInputDescriptor().descriptor(SosHarvesterProcessDescriptor.DATE_COLUMN_NAME);
        Assert.assertNotNull(mainParamDesc);

        Assert.assertTrue(mainParamDesc instanceof ExtendedParameterDescriptor);

        ExtendedParameterDescriptor mainParamExt = (ExtendedParameterDescriptor) mainParamDesc;

        Assert.assertEquals("DATE (yyyy-mm-ddThh:mi:ssZ)", mainParamExt.getDefaultValue());

        // column count
        Assert.assertEquals(17, mainParamExt.getValidValues().size());
    }
}
