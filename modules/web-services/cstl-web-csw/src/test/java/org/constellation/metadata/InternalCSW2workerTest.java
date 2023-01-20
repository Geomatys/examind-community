/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2014 Geomatys.
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


package org.constellation.metadata;


import org.constellation.metadata.core.CSWworker;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.test.utils.Order;
import org.constellation.util.Util;
import org.geotoolkit.ebrim.xml.EBRIMMarshallerPool;
import org.geotoolkit.xml.AnchoredMarshallerPool;
import org.constellation.util.NodeUtilities;
import org.junit.Test;
import org.w3c.dom.Node;

/**
 * Test of the Examind internal Metadata provider.
 *
 * @author Guilhem Legal (Geomatys)
 */
public class InternalCSW2workerTest extends CSW2workerTest {

    private static boolean initialized = false;

    @PostConstruct
    public void setUpClass() {
        try {
            if (!initialized) {
                serviceBusiness.deleteAll();
                providerBusiness.removeAll();
                metadataBusiness.deleteAllMetadata();

                int internalPID = metadataBusiness.getDefaultInternalProviderID();
                pool = EBRIMMarshallerPool.getInstance();
                fillPoolAnchor((AnchoredMarshallerPool) pool);

                //we write the data files
                writeMetadata("meta1.xml",         "42292_5p_19900609195600", internalPID);
                writeMetadata("meta2.xml",         "42292_9s_19900610041000", internalPID);
                writeMetadata("meta3.xml",         "39727_22_19750113062500", internalPID);
                writeMetadata("meta4.xml",         "11325_158_19640418141800", internalPID);
                writeMetadata("meta5.xml",         "40510_145_19930221211500", internalPID);
                writeMetadata("meta-19119.xml",    "mdweb_2_catalog_CSW Data Catalog_profile_inspire_core_service_4", internalPID);
                writeMetadata("imageMetadata.xml", "gov.noaa.nodc.ncddc. MODXXYYYYJJJ.L3_Mosaic_NOAA_GMX or MODXXYYYYJJJHHMMSS.L3_NOAA_GMX", internalPID);
                writeMetadata("ebrim1.xml",        "000068C3-3B49-C671-89CF-10A39BB1B652", internalPID);
                writeMetadata("ebrim2.xml",        "urn:uuid:3e195454-42e8-11dd-8329-00e08157d076", internalPID);
                writeMetadata("ebrim3.xml",        "urn:motiive:csw-ebrim", internalPID);
                writeMetadata("meta13.xml",        "urn:uuid:1ef30a8b-876d-4828-9246-dcbbyyiioo", internalPID);

                writeMetadata("meta7.xml",         "MDWeb_FR_SY_couche_vecteur_258", internalPID, true);

                //we write the configuration file
                Automatic configuration = new Automatic();
                configuration.putParameter(TRANSACTION_SECURIZED, "false");
                configuration.putParameter(TRANSACTIONAL, "true");

                Integer sid = serviceBusiness.create("csw", "default", configuration, null, null);
                serviceBusiness.linkCSWAndProvider(sid, internalPID, true);

                worker = new CSWworker("default");
                initialized = true;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=1)
    public void getCapabilitiesTest() throws Exception {
        super.getCapabilitiesTest();
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=2)
    public void getRecordByIdTest() throws Exception {
        super.getRecordByIdTest();
    }

    /**
     * Tests the getcapabilities method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=3)
    public void getRecordByIdErrorTest() throws Exception {
        super.getRecordByIdErrorTest();
    }

    /**
     * Tests the getRecords method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=4)
    public void getRecordsTest() throws Exception {
        super.getRecordsTest();
    }

    @Test
    @Override
    @Order(order=5)
    public void getRecordsSpatialTest() throws Exception {
        super.getRecordsSpatialTest();
    }

    @Test
    @Override
    @Order(order=6)
    public void getRecords191152Test() throws Exception {
        super.getRecords191152Test();
    }


    /**
     * Tests the getRecords method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=7)
    public void getRecordsErrorTest() throws Exception {
        super.getRecordsErrorTest();
    }

    /**
     * Tests the getDomain method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=8)
    public void getDomainTest() throws Exception {
        super.getDomainTest();
    }

    /**
     * Tests the describeRecord method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=9)
    public void DescribeRecordTest() throws Exception {
        super.DescribeRecordTest();
    }

    /**
     * Tests the transaction method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=10)
    public void transactionDeleteInsertTest() throws Exception {
        super.transactionDeleteInsertTest();
    }

    /**
     * Tests the transaction method
     *
     * @throws java.lang.Exception
     */
    @Test
    @Override
    @Order(order=11)
    public void transactionUpdateTest() throws Exception {
        typeCheckUpdate = false;
        super.transactionUpdateTest();

    }

    public void writeMetadata(String resourceName, String identifier, Integer providerID) throws Exception {
        writeMetadata(resourceName, identifier, providerID, false);
    }

    public void writeMetadata(String resourceName, String identifier, Integer providerID, boolean hidden) throws Exception {
       Node node  = NodeUtilities.getNodeFromStream(Util.getResourceAsStream("org/constellation/xml/metadata/" + resourceName));
       metadataBusiness.updateMetadata(identifier, node, null, null, null, null, providerID, "DOC", null, hidden);
    }
}
