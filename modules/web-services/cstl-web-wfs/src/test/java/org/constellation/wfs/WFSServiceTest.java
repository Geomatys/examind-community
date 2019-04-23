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
import org.constellation.configuration.ConfigDirectory;
import org.constellation.api.ProviderType;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.util.Util;
import org.constellation.wfs.ws.rs.FeatureCollectionWrapper;
import org.constellation.wfs.ws.rs.WFSService;
import org.constellation.ws.rs.AbstractWebService;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import org.apache.sis.util.logging.Logging;
import org.constellation.provider.ProviderParameters;
import org.constellation.provider.datastore.DataStoreProviderService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringTestRunner.class)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,DirtiesContextTestExecutionListener.class})
@DirtiesContext(hierarchyMode = DirtiesContext.HierarchyMode.EXHAUSTIVE,classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(inheritInitializers = false, locations={"classpath:/cstl/spring/test-context.xml"})
public class WFSServiceTest {

    @Inject
    private IServiceBusiness serviceBusiness;
    @Inject
    protected ILayerBusiness layerBusiness;
    @Inject
    protected IProviderBusiness providerBusiness;
    @Inject
    protected IDataBusiness dataBusiness;

    private static WFSService service;

    private static boolean initialized = false;

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("WFSServiceTest");
    }

    @PostConstruct
    public void setUpClass() {
        if (!initialized) {
            try {
                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                final DataProviderFactory featfactory = DataProviders.getFactory("data-store");

                final String url = "jdbc:derby:memory:TestWFSServiceOM";
                final DefaultDataSource ds = new DefaultDataSource(url + ";create=true");
                Connection con = ds.getConnection();
                DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
                String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
                sql = sql.replace("$SCHEMA", "");
                sr.run(sql);
                sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));
                con.close();
                ds.shutdown();

                final ParameterValueGroup sourceOM = featfactory.getProviderDescriptor().createValue();
                sourceOM.parameter("id").setValue("omSrc");

                final ParameterValueGroup choiceOM = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourceOM);
                final ParameterValueGroup omconfig = choiceOM.addGroup("SOSDBParameters");
                omconfig.parameter("sgbdtype").setValue("derby");
                omconfig.parameter("derbyurl").setValue(url);

                providerBusiness.storeProvider("omSrc", null, ProviderType.LAYER, "data-store", sourceOM);
                dataBusiness.create(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"), "omSrc", "VECTOR", false, true, null, null);

                final LayerContext config = new LayerContext();
                config.getCustomParameters().put("transactionSecurized", "false");
                config.getCustomParameters().put("transactional", "true");

                serviceBusiness.create("wfs", "default", config, null, null);
                layerBusiness.add("SamplingPoint",       "http://www.opengis.net/sampling/1.0",  "omSrc",      null, "default", "wfs", null);

                serviceBusiness.start("wfs", "default");

                // let the worker start
                Thread.sleep(2000);
                service = new WFSService();

                Field privateStringField = AbstractWebService.class.getDeclaredField("postKvpParameters");
                privateStringField.setAccessible(true);
                ThreadLocal<Map<String, String[]>> postKvpParameters = (ThreadLocal<Map<String, String[]>>) privateStringField.get(service);

                final Map<String, String[]> kvpMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                kvpMap.put("serviceId", new String[]{"default"});
                postKvpParameters.set(kvpMap);

                initialized = true;
            } catch (Exception ex) {
                Logging.getLogger("org.constellation.wfs").log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ConfigDirectory.shutdownTestEnvironement("WFSServiceTest");

        if (service != null) {
            service.destroy();
        }
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
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

        assertTrue(result.getBody() instanceof FeatureCollectionWrapper);
        FeatureCollection collection = ((FeatureCollectionWrapper) result.getBody()).getFeatureCollection();
        assertEquals(6, collection.size());

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

        assertTrue(result.getBody() instanceof FeatureCollectionWrapper);
        collection = ((FeatureCollectionWrapper) result.getBody()).getFeatureCollection();
        assertEquals(8, collection.size());

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

        assertTrue(result.getBody() instanceof FeatureCollectionWrapper);
        collection = ((FeatureCollectionWrapper) result.getBody()).getFeatureCollection();
        assertEquals(6, collection.size());

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

        assertTrue(result.getBody() instanceof FeatureCollectionWrapper);
        collection = ((FeatureCollectionWrapper) result.getBody()).getFeatureCollection();
        assertEquals(8, collection.size());

    }
}
