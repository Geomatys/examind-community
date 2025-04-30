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
import java.io.IOException;
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
    protected static Path mooFile;
    protected static Path multiPlatDirectory;
    protected static Path bigdataDirectory;
    protected static Path survalDirectory;
    protected static Path noHeadDirectory;
    protected static Path disjointDirectory;
    protected static Path tsvDirectory;
    protected static Path tsvFlatDirectory;
    protected static Path propDirectory;
    protected static Path propFlatDirectory;

    // XLS dir
    protected static Path xDataDirectory;
    protected static Path xDataFlatDirectory;

    // DBF dir
    protected static Path ltDirectory;
    protected static Path rtDirectory;

    // error files
    protected static Path errorHeaderDirectory;
    protected static Path errorHeaderDirectory_1;
    protected static Path errorHeaderDirectory_2;

    protected static Path errorHeaderDirectory2;
    protected static Path errorUnitConvertDirectory;
    protected static Path errorUnitConvertFile1;
    protected static Path errorUnitConvertFile2;
    
    protected static Path warningUomDirectory;
    protected static Path noLineDirectory;
    protected static Path multiFixedDirectory;
    
    protected static Path qualityCSVDirectory;
    protected static Path multiQualityCSVDirectory;
    protected static Path multiParameterCSVDirectory;


    protected static final int ORIGIN_NB_SENSOR = 19;

    @BeforeClass
    public static void setUpClass() throws Exception {

        final Path configDir = Paths.get("target");
        DATA_DIRECTORY            = configDir.resolve("data" + UUID.randomUUID());
        errorHeaderDirectory      = DATA_DIRECTORY.resolve("error-dir");
        
        
        argoDirectory             = writeResourceFileInDir("argo-profile", "argo-profiles-2902402-1.csv");
        fmlwDirectory             = writeResourceFileInDir("fmlw-traj", "tsg-FMLW-1.csv", "tsg-FMLW-2.csv", "tsg-FMLW-3.csv");
        mooDirectory              = writeResourceFileInDir("moo-ts", "mooring-buoys-time-series-62069.csv");
        ltDirectory               = writeResourceFileInDir("lt-ts", "LakeTile_001.dbf", "LakeTile_002.dbf");
        rtDirectory               = writeResourceFileInDir("rt-ts",  "rivertile_001.dbf", "rivertile_002.dbf");
        multiPlatDirectory        = writeResourceFileInDir("multi-plat", "multiplatform-1.csv", "multiplatform-2.csv");
        bigdataDirectory          = writeResourceFileInDir("bigdata-profile", "bigdata-1.csv");
        survalDirectory           = writeResourceFileInDir("surval", "surval-small.csv");
        xDataDirectory            = writeResourceFileInDir("xdata", "xdata.xlsx");
        noHeadDirectory           = writeResourceFileInDir("noHead", "nohead.csv");
        disjointDirectory         = writeResourceFileInDir("disjoint", "disjoint-1.csv");
        tsvDirectory              = writeResourceFileInDir("tsv", "tabulation.tsv");
        tsvFlatDirectory          = writeResourceFileInDir("tsv-flat", "tabulation-flat.tsv");
        xDataFlatDirectory        = writeResourceFileInDir("xlsx-flat", "test-flat.xlsx");
        errorHeaderDirectory_1    = writeResourceFileInDir("error-dir/error-head-dir1", "error-header.csv");
        errorHeaderDirectory_2    = writeResourceFileInDir("error-dir/error-head-dir2", "error-header.csv");
        errorHeaderDirectory2     = writeResourceFileInDir("error-dir-2", "error-header.csv", "error-header-2.csv");
        errorUnitConvertDirectory = writeResourceFileInDir("error-unit-convert", "unit-convert-error-1.csv", "unit-convert-error-2.csv");
        warningUomDirectory       = writeResourceFileInDir("warning-uom", "warning-uom.csv");
        noLineDirectory           = writeResourceFileInDir("no-valid-line", "no-valid-lines.csv");
        multiFixedDirectory       = writeResourceFileInDir("multi-fixed", "multi-fixed-1.csv");
        qualityCSVDirectory       = writeResourceFileInDir("single-quality-csv", "single-csv-qual.csv");
        multiQualityCSVDirectory  = writeResourceFileInDir("multi-quality-csv", "multi-csv-qual.csv");
        multiParameterCSVDirectory  = writeResourceFileInDir("multi-parameter-csv", "multi-csv-param.csv");
        propDirectory             = writeResourceFileInDir("prop", "properties.csv");
        propFlatDirectory         = writeResourceFileInDir("prop-flat", "properties-flat.csv");
        
        mooFile               = mooDirectory.resolve("mooring-buoys-time-series-62069.csv");
        errorUnitConvertFile1 = errorUnitConvertDirectory.resolve("unit-convert-error-1.csv");
        errorUnitConvertFile2 = errorUnitConvertDirectory.resolve("unit-convert-error-2.csv");
    }
    
    private static Path writeResourceFileInDir(String dirName, String... fileNames) throws IOException {
        Path dir = Files.createDirectories(DATA_DIRECTORY.resolve(dirName));
        for (String fileName : fileNames) {
            writeResourceDataFile(dir, "com/examind/process/sos/" + fileName, fileName);
        }
        return dir;
    }

    protected ServiceComplete sc;
    protected ServiceComplete sc2;
    protected ServiceComplete badService;

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
            serviceBusiness.linkServiceAndSensorProvider(sid, pid, true);
            serviceBusiness.start(sid);

            sid = serviceBusiness.create("sts", "default", configuration, null, null);
            serviceBusiness.linkServiceAndSensorProvider(sid, pid, true);
            serviceBusiness.start(sid);

            sid = serviceBusiness.create("sts", "bad", configuration, null, null);
            // no provider linked
            serviceBusiness.start(sid);

            initialized = true;
        }

        sc = serviceBusiness.getServiceByIdentifierAndType("sos", "default");
        Assert.assertNotNull(sc);
        sensorServBusiness.removeAllSensors(sc.getId());

        sc2 = serviceBusiness.getServiceByIdentifierAndType("sts", "default");
        Assert.assertNotNull(sc2);
        sensorServBusiness.removeAllSensors(sc2.getId());

        badService = serviceBusiness.getServiceByIdentifierAndType("sts", "bad");
        Assert.assertNotNull(sc2);
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
