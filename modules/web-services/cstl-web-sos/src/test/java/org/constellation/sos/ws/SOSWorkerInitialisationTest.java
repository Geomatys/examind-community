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

package org.constellation.sos.ws;

import org.constellation.sos.core.SOSworker;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.util.Date;

import jakarta.xml.bind.Marshaller;

import org.apache.sis.xml.MarshallerPool;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IServiceBusiness;

import org.constellation.dto.service.Service;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.repository.ServiceRepository;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.test.SpringContextTest;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.sos.xml.SOSMarshallerPool;
import org.geotoolkit.sos.xml.v100.GetCapabilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;


/**
 * Test some erroned initialisation of SOS Worker
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SOSWorkerInitialisationTest extends SpringContextTest {

    @Autowired
    private IServiceBusiness serviceBusiness;
    @Autowired
    private ServiceRepository serviceRepository;

    private static MarshallerPool pool;

    @BeforeClass
    public static void setUpClass() throws Exception {
        pool = SOSMarshallerPool.getInstance();
    }

    /**
     * Tests the initialisation of the SOS worker with different configuration mistake
     *
     * @throws java.lang.Exception
     */
    @Test
    public void initialisationTest() throws Exception {

        // clear
        serviceBusiness.deleteAll();

        /**
         * Test 1: No configuration file.
         */

        Service service = new Service();
        service.setIdentifier("default");
        service.setDate(new Date(System.currentTimeMillis()));
        service.setType("sos");
        service.setStatus("NOT_STARTED");
        service.setVersions("1.0.0");

        final Service s1 = service;
        int id = SpringHelper.executeInTransaction(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction(TransactionStatus ts) {
                return serviceRepository.create(s1);
            }
        });

        assertTrue(id > 0);

        SOSworker worker = new SOSworker("default");

        boolean exceptionLaunched = false;
        GetCapabilities request = new GetCapabilities();
        try {

            worker.getCapabilities(request);

        } catch(CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertEquals(ex.getMessage(), "The service is not running.\nCause:The configuration object is null.");
            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);

        /**
         * Test 2: An empty configuration file.
         */
        service = serviceRepository.findByIdentifierAndType("default", "sos");
        service.setConfig("");
        final Service s2 = service;
        SpringHelper.executeInTransaction(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus ts) {
                return serviceRepository.update(s2);
            }
        });


        worker = new SOSworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch(CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertEquals("The service is not running.\nCause:The configuration object is malformed.", ex.getMessage());

            exceptionLaunched = true;
        }

        assertTrue(exceptionLaunched);

        /**
         * Test 3: A malformed configuration file (bad unrecognized type).
         */
        StringWriter sw = new StringWriter();
        final Marshaller m = pool.acquireMarshaller();
        m.marshal(request, sw);
        pool.recycle(m);

        service = serviceRepository.findByIdentifierAndType("default", "sos");
        service.setConfig(sw.toString());
        final Service s3 = service;
        SpringHelper.executeInTransaction(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus ts) {
                return serviceRepository.update(s3);
            }
        });

        worker = new SOSworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch(CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertTrue(ex.getMessage().startsWith("The service is not running."));
            exceptionLaunched = true;
        }
        assertTrue(exceptionLaunched);

        Marshaller marshaller = GenericDatabaseMarshallerPool.getInstance().acquireMarshaller();
        /**
         * Test 4: A malformed configuration file (bad unrecognized type).
         */
        sw = new StringWriter();
        marshaller.marshal(new Automatic(), sw);
        service = serviceRepository.findByIdentifierAndType("default", "sos");
        service.setConfig(sw.toString());
        final Service s4 = service;
        SpringHelper.executeInTransaction(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus ts) {
                return serviceRepository.update(s4);
            }
        });

        worker = new SOSworker("default");

        exceptionLaunched = false;
        try {

            worker.getCapabilities(request);

        } catch(CstlServiceException ex) {
            assertEquals(ex.getExceptionCode(), NO_APPLICABLE_CODE);
            assertTrue(ex.getMessage().startsWith("The service is not running."));
            exceptionLaunched = true;
        }
        assertTrue(exceptionLaunched);

        GenericDatabaseMarshallerPool.getInstance().recycle(marshaller);
    }

}
