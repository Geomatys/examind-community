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

package org.constellation.wfs;

import org.constellation.dto.service.config.wxs.LayerContext;
import org.geotoolkit.feature.model.FeatureSetWrapper;
import org.constellation.wfs.ws.rs.WFSService;
import org.constellation.ws.rs.AbstractWebService;
import org.geotoolkit.nio.IOUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
public class WFSServiceTest extends AbstractWFSWorkerTest {

    private static WFSService service;

    @Before
    public void setUpClass() throws Exception {
        layerBusiness.removeAll();
        serviceBusiness.deleteAll();
        dataBusiness.deleteAll();
        providerBusiness.removeAll();

        List<DataImport> datas = testResources.createProvider(TestResource.OM2_DB, providerBusiness, null).datas;

        final LayerContext config = new LayerContext();
        config.getCustomParameters().put(TRANSACTION_SECURIZED, "false");
        config.getCustomParameters().put(TRANSACTIONAL, "true");

        Integer defId = serviceBusiness.create("wfs", "default", config, null, null);
        for (DataImport d : datas) {
            layerBusiness.add(d.id, null, d.namespace, d.name, null, defId, null);
        }

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
