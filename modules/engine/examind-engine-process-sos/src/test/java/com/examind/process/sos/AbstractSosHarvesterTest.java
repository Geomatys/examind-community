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
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import org.constellation.admin.SpringHelper;
import org.constellation.admin.WSEngine;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.business.ISensorServiceBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import static org.constellation.test.utils.TestResourceUtils.writeResourceDataFile;
import org.geotoolkit.nio.IOUtilities;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractSosHarvesterTest extends SpringContextTest {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected IDatasourceBusiness datasourceBusiness;
    @Autowired
    protected IProviderBusiness providerBusiness;
    @Autowired
    protected ISensorBusiness sensorBusiness;
    @Autowired
    protected ISensorServiceBusiness sensorServBusiness;
    @Autowired
    protected IDatasetBusiness datasetBusiness;

    @Autowired
    protected WSEngine wsEngine;

    private static boolean initialized = false;

    private static Path DATA_DIRECTORY;

    // CSV dir
    protected static Path argoDirectory;
    protected static Path fmlwDirectory;
    protected static Path mooDirectory;
    protected static Path multiPlatDirectory;
    protected static Path bigdataDirectory;
    protected static Path survalDirectory;
    protected static Path noHeadDirectory;
    protected static Path disjointDirectory;
    protected static Path tsvDirectory;
    protected static Path tsvFlatDirectory;

    // XLS dir
    protected static Path xDataDirectory;
    protected static Path xDataFlatDirectory;

    // DBF dir
    protected static Path ltDirectory;
    protected static Path rtDirectory;


    protected static final int ORIGIN_NB_SENSOR = 17;

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
        disjointDirectory = DATA_DIRECTORY.resolve("disjoint");
        Files.createDirectories(disjointDirectory);
        tsvDirectory = DATA_DIRECTORY.resolve("tsv");
        Files.createDirectories(tsvDirectory);
        tsvFlatDirectory = DATA_DIRECTORY.resolve("tsv-flat");
        Files.createDirectories(tsvFlatDirectory);
        xDataFlatDirectory = DATA_DIRECTORY.resolve("xlsx-flat");
        Files.createDirectories(xDataFlatDirectory);

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
        writeResourceDataFile(disjointDirectory, "com/examind/process/sos/disjoint-1.csv", "disjoint-1.csv");
        writeResourceDataFile(tsvDirectory, "com/examind/process/sos/tabulation.tsv", "tabulation.tsv");
        writeResourceDataFile(tsvFlatDirectory, "com/examind/process/sos/tabulation-flat.tsv", "tabulation-flat.tsv");
        writeResourceDataFile(xDataFlatDirectory, "com/examind/process/sos/test-flat.xlsx", "test-flat.xlsx");
    }

    protected ServiceComplete sc;
    protected ServiceComplete sc2;

    @PostConstruct
    public void setUp() throws Exception {
        if (!initialized) {
            // clean up
            serviceBusiness.deleteAll();
            providerBusiness.removeAll();
            datasourceBusiness.deleteAll();

            Integer pid = testResources.createProvider(TestEnvironment.TestResource.OM2_DB, providerBusiness, null).id;

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

        sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);
        sensorServBusiness.removeAllSensors(sc.getId());

        sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);
        sensorServBusiness.removeAllSensors(sc2.getId());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        initialized = false;
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

}
