/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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

package org.constellation.wfs;

import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.SpringContextTest;
import org.geotoolkit.feature.model.FeatureSetWrapper;
import org.constellation.wfs.ws.rs.WFSService;
import org.constellation.ws.rs.AbstractWebService;
import org.geotoolkit.nio.IOUtilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import javax.inject.Inject;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.storage.FeatureSet;
import static org.constellation.api.CommonConstants.TRANSACTIONAL;
import static org.constellation.api.CommonConstants.TRANSACTION_SECURIZED;

import org.constellation.test.utils.TestEnvironment.DataImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.geotoolkit.storage.feature.FeatureStoreUtilities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class WFSServiceTest extends SpringContextTest {

    @Inject
    private IServiceBusiness serviceBusiness;
    @Inject
    protected ILayerBusiness layerBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected IDataBusiness dataBusiness;

    private static WFSService service;

    private static final Logger LOGGER = Logger.getLogger("org.constellation.wfs");

    @Before
    public void setUpClass() throws Exception {
        layerBusiness.removeAll();
        serviceBusiness.deleteAll();
        dataBusiness.deleteAll();
        providerBusiness.removeAll();

        DataImport d = testResources.createProvider(TestResource.OM2_FEATURE_DB, providerBusiness, null).datas.get(0);

        final LayerContext config = new LayerContext();
        config.getCustomParameters().put(TRANSACTION_SECURIZED, "false");
        config.getCustomParameters().put(TRANSACTIONAL, "true");

        Integer defId = serviceBusiness.create("wfs", "default", config, null, null);
        layerBusiness.add(d.id, null, d.namespace, d.name, null, defId, null);

        serviceBusiness.start(defId);

        // let the worker start
        Thread.sleep(2000);
        service = new WFSService();

        Field privateStringField = AbstractWebService.class.getDeclaredField("postKvpParameters");
        privateStringField.setAccessible(true);
        ThreadLocal<Map<String, String[]>> postKvpParameters = (ThreadLocal<Map<String, String[]>>) privateStringField.get(service);

        final Map<String, String[]> kvpMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        kvpMap.put("serviceId", new String[]{"default"});
        postKvpParameters.set(kvpMap);

    }

    @After
    public void disposeService() {
        service.destroy();
    }

    @AfterClass
    public static void tearDownClass() {
        try {
            File derbyLog = new File("derby.log");
            if (derbyLog.exists()) {
                derbyLog.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
    }

    @Test
    public void transactionInsertTest() throws Exception {

        // wait for service to start
        Thread.sleep(2000);
        /*
         * we verify that the number of features before insert
         */
        InputStream is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.GetFeature.xml"));
        ResponseEntity result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        assertTrue(result.getBody() instanceof FeatureSetWrapper);
        FeatureSet collection = ((FeatureSetWrapper) result.getBody()).getFeatureSet().get(0);
        assertEquals(6, FeatureStoreUtilities.getCount(collection).intValue());

        /*
         * we insert the feature
         */
        is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.InsertFeature.xml"));
        result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        /*
         * we verify that the features has been inserted
         */
        is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.GetFeature.xml"));
        result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        assertTrue(result.getBody() instanceof FeatureSetWrapper);
        collection = ((FeatureSetWrapper) result.getBody()).getFeatureSet().get(0);
        assertEquals(8, FeatureStoreUtilities.getCount(collection).intValue());

        /*
         * we delete the features
         */
        is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.DeleteFeature.xml"));
        result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        /*
         * we verify that the features has been deleted
         */
        is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.GetFeature.xml"));
        result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        assertTrue(result.getBody() instanceof FeatureSetWrapper);
        collection = ((FeatureSetWrapper) result.getBody()).getFeatureSet().get(0);
        assertEquals(6, FeatureStoreUtilities.getCount(collection).intValue());

        /*
         * we insert the feature with another request
         */
        is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.InsertFeature2.xml"));
        result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        /*
         * we verify that the features has been inserted
         */
        is = Files.newInputStream(IOUtilities.getResourceAsPath("org.constellation.wfs.request.xml.GetFeature.xml"));
        result = service.doPOSTXml("default", is);

        assertEquals(HttpStatus.OK.value(), result.getStatusCodeValue());

        assertTrue(result.getBody() instanceof FeatureSetWrapper);
        collection = ((FeatureSetWrapper) result.getBody()).getFeatureSet().get(0);
        assertEquals(8, FeatureStoreUtilities.getCount(collection).intValue());

    }
}
