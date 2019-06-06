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

import static com.examind.process.sos.SosHarvesterProcessTest.writeDataFile;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IProcessBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.process.ChainProcessRetriever;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.process.dynamic.ExamindDynamicProcessFactory;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.SpringTestRunner;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-context.xml"})
@RunWith(SpringTestRunner.class)
public class HarvesterPreProcessTest {

    private static final Logger LOGGER = Logging.getLogger("com.examind.process.sos");

    private static boolean initialized = false;

    private static File argoDirectory;
    private static File fmlwDirectory;
    private static File mooDirectory;

    @Inject
    protected IProcessBusiness processBusiness;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final File configDir = ConfigDirectory.setupTestEnvironement("HarvesterPreProcessTest").toFile();
        File dataDirectory  = new File(configDir, "data");
        argoDirectory       = new File(dataDirectory, "argo-profile");
        argoDirectory.mkdirs();
        fmlwDirectory       = new File(dataDirectory, "fmlw-traj");
        fmlwDirectory.mkdirs();
        mooDirectory       = new File(dataDirectory, "moo-ts");
        mooDirectory.mkdirs();

        writeDataFile(argoDirectory, "argo-profiles-2902402-1.csv");
        writeDataFile(argoDirectory, "argo-profiles-2902402-2.csv");

        writeDataFile(fmlwDirectory, "tsg-FMLW-1.csv");
        writeDataFile(fmlwDirectory, "tsg-FMLW-2.csv");
        writeDataFile(fmlwDirectory, "tsg-FMLW-3.csv");

        writeDataFile(mooDirectory, "mooring-buoys-time-series-62069.csv");

    }

    @PostConstruct
    public void setUp() {
        try {

            if (!initialized) {

                for (ProcessDescriptor chainDesc : ChainProcessRetriever.getChainDescriptors()) {
                    processBusiness.deleteChainProcess(ExamindDynamicProcessFactory.NAME, chainDesc.getIdentifier().getCode());
                }
                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
        }
        File mappingFile = new File("mapping.properties");
        if (mappingFile.exists()) {
            mappingFile.delete();
        }
        ConfigDirectory.shutdownTestEnvironement("HarvesterPreProcessTest");
    }

    @Test
    @Order(order = 1)
    public void harvestProfileTest() throws Exception {
        final String processId = "generated-1";

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, HarvesterPreProcessDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(HarvesterPreProcessDescriptor.TASK_NAME_NAME).setValue(processId);
        in.parameter(HarvesterPreProcessDescriptor.DATA_FOLDER_NAME).setValue(argoDirectory.toURI().toString());

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
        in.parameter(HarvesterPreProcessDescriptor.DATA_FOLDER_NAME).setValue(fmlwDirectory.toURI().toString());

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
        in.parameter(HarvesterPreProcessDescriptor.DATA_FOLDER_NAME).setValue(mooDirectory.toURI().toString());

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

        Assert.assertEquals(16, mainParamExt.getValidValues().size());

    }

}
