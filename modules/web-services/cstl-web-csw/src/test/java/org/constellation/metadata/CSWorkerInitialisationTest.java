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

package org.constellation.metadata;

import org.constellation.metadata.core.CSWworker;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Date;
import javax.annotation.PostConstruct;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.sis.xml.MarshallerPool;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.Service;
import org.constellation.repository.ServiceRepository;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.test.SpringContextTest;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.v202.GetCapabilitiesType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;


/**
 * Test some erroned initialisation of CSW Worker
 *
 * @author Guilhem Legal (Geomatys)
 */
@TestExecutionListeners({ TransactionalTestExecutionListener.class })
public class CSWorkerInitialisationTest extends SpringContextTest {

    private static MarshallerPool pool;

    @Inject
    private IServiceBusiness serviceBusiness;

    @Inject
    private ServiceRepository serviceRepository;

    @BeforeClass
    public static void setUpClass() throws Exception {
        pool = CSWMarshallerPool.getInstance();
    }

    @PostConstruct
    public void setUp() throws ConstellationException {
        serviceBusiness.deleteAll();
    }

    /**
     * Tests the initialisation of the CSW worker with different configuration
     * mistake
     *
     * @throws java.lang.Exception
     */
    @Test
    @Transactional
    public void initialisationTest() throws Exception {

        /**
         * Test 1: No configuration file.
         */
        Service service = new Service();
        service.setIdentifier("default");
        service.setDate(new Date(System.currentTimeMillis()));
        service.setType("csw");
        service.setStatus("NOT_STARTED");
        service.setVersions("2.0.0");

        int id = serviceRepository.create(service);
        assertTrue(id > 0);

        CSWworker worker = new CSWworker("default");

        boolean exceptionLaunched = false;
        GetCapabilitiesType request = new GetCapabilitiesType("CSW");
        try {

            worker.getCapabilities(request);

        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertEquals(
                    ex.getMessage(),
                    "The service is not running.\nCause:The configuration object is null.");
            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);

        /**
         * Test 2: An empty configuration file.
         */
        service = serviceRepository.findByIdentifierAndType("default", "csw");
        service.setConfig("");
        serviceRepository.update(service);

        worker = new CSWworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch (CstlServiceException ex) {
            assertEquals(NO_APPLICABLE_CODE, ex.getExceptionCode());
            assertEquals("The service is not running.\nCause:The configuration object is malformed.", ex.getMessage());
            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);

        /**
         * Test 3: A malformed configuration file (bad recognized type).
         */
        StringWriter sw = new StringWriter();
        final Marshaller m = pool.acquireMarshaller();
        m.marshal(request, sw);
        pool.recycle(m);

        service = serviceRepository.findByIdentifierAndType("default", "csw");
        service.setConfig(sw.toString());
        serviceRepository.update(service);

        worker = new CSWworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertTrue(ex.getMessage()
                    .startsWith("The service is not running."));
            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);

        /**
         * Test 4: A malformed configuration file (bad not recognized type).
         */
        sw = new StringWriter();
        Marshaller tempMarshaller = JAXBContext.newInstance(UnknowObject.class,
                Automatic.class).createMarshaller();
        tempMarshaller.marshal(new UnknowObject(), sw);

        service = serviceRepository.findByIdentifierAndType("default", "csw");
        service.setConfig(sw.toString());
        serviceRepository.update(service);

        worker = new CSWworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertTrue(ex.getMessage()
                    .startsWith("The service is not running."));
            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);

        /**
         * Test 5: A configuration file with missing part.
         */
        sw = new StringWriter();
        Automatic configuration = new Automatic();
        tempMarshaller.marshal(configuration, sw);

        service = serviceRepository.findByIdentifierAndType("default", "csw");
        service.setConfig(sw.toString());
        serviceRepository.update(service);

        worker = new CSWworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch (CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertEquals(ex.getMessage(),
                    "The service is not running.\nCause:No linked metadata Providers");
            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);



    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "Unknow")
    private static class UnknowObject {

        private final String field1 = "something";

        private final String field2 = "other thing";

    }

}
