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
package org.constellation.wmts.ws;

import org.constellation.test.SpringContextTest;
import org.constellation.wmts.core.DefaultWMTSWorker;
import org.constellation.wmts.core.WMTSWorker;
import org.geotoolkit.test.xml.DocumentComparator;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.business.IServiceBusiness;
import org.constellation.admin.SpringHelper;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ows.xml.v110.AcceptVersionsType;
import org.geotoolkit.ows.xml.v110.SectionsType;
import org.geotoolkit.wmts.xml.WMTSMarshallerPool;
import org.geotoolkit.wmts.xml.v100.Capabilities;
import org.geotoolkit.wmts.xml.v100.GetCapabilities;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.annotation.PostConstruct;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConstellationException;
import org.constellation.test.utils.TestEnvironment;
import org.springframework.beans.factory.annotation.Autowired;

import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.MISSING_PARAMETER_VALUE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.VERSION_NEGOTIATION_FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class WMTSWorkerTest extends SpringContextTest {

    @Autowired
    private IServiceBusiness serviceBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    private static MarshallerPool pool;
    private static WMTSWorker worker ;

    @PostConstruct
    public void setUpClass() {
        try {

            try {
                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
            } catch (ConstellationException ex) {}

            pool = WMTSMarshallerPool.getInstance();

            Integer sid = serviceBusiness.create("wmts", "default", new LayerContext(), null, null);

            TestEnvironment.DataImport did  = testResources.createProvider(TestEnvironment.TestResource.XML_PYRAMID, providerBusiness, null).datas.get(0);

            // one layer with alias
            layerBusiness.add(did.id, "haiti", did.namespace, did.name, null, sid, null);

            // same data, but with namespace
            layerBusiness.add(did.id, null, "nmsp", did.name, null, sid, null);

            worker = new DefaultWMTSWorker("default");
            worker.setServiceUrl("http://localhost:9090/WS/");
        } catch (Exception ex) {
            Logger.getLogger("org.constellation.wmts.ws").log(Level.SEVERE, null, ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);;
        if (service != null) {
            service.deleteAll();
        }
    }

    /**
     * test the feature marshall
     *
     */
    @Test
    public void getCapabilitiesTest() throws Exception {
        final Marshaller marshaller = pool.acquireMarshaller();

        AcceptVersionsType acceptVersion = new AcceptVersionsType("1.0.0");
        SectionsType sections   = new SectionsType("Contents");
        GetCapabilities request = new GetCapabilities(acceptVersion, sections, null, null, "WMTS");

        Capabilities result = worker.getCapabilities(request);

        StringWriter sw = new StringWriter();
        marshaller.marshal(result, sw);
        DocumentComparator comparator = new DocumentComparator(IOUtilities.toString(
                IOUtilities.getResourceAsPath("org.constellation.wmts.xml.WMTSCapabilities1-0-0-cont.xml")), sw.toString());
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();


        request = new GetCapabilities("WMTS");
        result = worker.getCapabilities(request);

        sw = new StringWriter();
        marshaller.marshal(result, sw);
        comparator = new DocumentComparator(IOUtilities.toString(
                IOUtilities.getResourceAsPath("org.constellation.wmts.xml.WMTSCapabilities1-0-0.xml")), sw.toString());
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();

        acceptVersion = new AcceptVersionsType("2.3.0");
        request = new GetCapabilities(acceptVersion, null, null, null, "WMTS");

        try {
            worker.getCapabilities(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), VERSION_NEGOTIATION_FAILED);
            assertEquals(ex.getLocator(), "acceptVersion");
        }

         acceptVersion = new AcceptVersionsType("1.0.0");
        request = new GetCapabilities(acceptVersion, null, null, null, "WPS");

        try {
            worker.getCapabilities(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), INVALID_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "service");
        }

        request = new GetCapabilities(null);

        try {
            worker.getCapabilities(request);
            fail("Should have raised an error.");
        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), MISSING_PARAMETER_VALUE);
            assertEquals(ex.getLocator(), "service");
        }

        acceptVersion = new AcceptVersionsType("1.0.0");
        sections      = new SectionsType("operationsMetadata");
        request       = new GetCapabilities(acceptVersion, sections, null, null, "WMTS");

        result = worker.getCapabilities(request);

        sw = new StringWriter();
        marshaller.marshal(result, sw);
        comparator = new DocumentComparator(IOUtilities.toString(
                IOUtilities.getResourceAsPath("org.constellation.wmts.xml.WMTSCapabilities1-0-0-om.xml")), sw.toString());
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();

        acceptVersion = new AcceptVersionsType("1.0.0");
        sections      = new SectionsType("serviceIdentification");
        request       = new GetCapabilities(acceptVersion, sections, null, null, "WMTS");

        result = worker.getCapabilities(request);

        sw = new StringWriter();
        marshaller.marshal(result, sw);
        comparator = new DocumentComparator(IOUtilities.toString(
                IOUtilities.getResourceAsPath("org.constellation.wmts.xml.WMTSCapabilities1-0-0-si.xml")), sw.toString());
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();

        acceptVersion = new AcceptVersionsType("1.0.0");
        sections      = new SectionsType("serviceProvider");
        request       = new GetCapabilities(acceptVersion, sections, null, null, "WMTS");

        result = worker.getCapabilities(request);

        sw = new StringWriter();
        marshaller.marshal(result, sw);
        comparator = new DocumentComparator(IOUtilities.toString(
                IOUtilities.getResourceAsPath("org.constellation.wmts.xml.WMTSCapabilities1-0-0-sp.xml")), sw.toString());
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();

        pool.recycle(marshaller);
    }

    @Test
    @Ignore("TODO: implement")
    public void getTileTest() throws Exception {

    }

}
