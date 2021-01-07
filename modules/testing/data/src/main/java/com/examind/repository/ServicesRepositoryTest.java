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
package com.examind.repository;

import java.util.List;
import java.util.Map;

import org.constellation.dto.service.Service;
import org.constellation.repository.ServiceRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.ServiceReference;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ServicesRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private LayerRepository layerRepository;

    @Test
    public void all() {
        dump(serviceRepository.findAll());
    }

    @Test
    public void crud() {

        serviceRepository.deleteAll();
        List<Service> all = serviceRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer pid1 = providerRepository.create(TestSamples.newProviderQuote(owner.getId()));
        Assert.assertNotNull(pid1);

        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid1, null));
        Assert.assertNotNull(did1);

        Integer pid2 = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid2);

        Integer pid3 = providerRepository.create(TestSamples.newProvider2(owner.getId()));
        Assert.assertNotNull(pid3);

        /**
         * service insertion
         */
        Integer sid1 = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid1);

        Service s1 = serviceRepository.findById(sid1);
        Assert.assertNotNull(s1);

        Integer lid1 = layerRepository.create(TestSamples.newLayer(owner.getId(), did1, sid1));
        Assert.assertNotNull(lid1);

        String file1 =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<details/>";
        serviceRepository.updateExtraFile(sid1, "file1.xml", file1);

        String file2 =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<detai'; DROP TABLE admin.services;'ls/>";
        serviceRepository.updateExtraFile(sid1, "file2.sh", file2);
        

        Integer sid2 = serviceRepository.create(TestSamples.newService2(owner.getId()));
        Assert.assertNotNull(sid2);

        Service s2 = serviceRepository.findById(sid2);
        Assert.assertNotNull(s2);

        serviceRepository.linkMetadataProvider(sid2, pid2, true);
        serviceRepository.linkSensorProvider(sid2, pid3, true);

        Integer sid3 = serviceRepository.create(TestSamples.newServiceQuote(owner.getId()));
        Assert.assertNotNull(sid3);

        Service s3 = serviceRepository.findById(sid3);
        Assert.assertNotNull(s3);

        String xmlEN =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ns2:details xmlns:ns2=\"http://www.constellation.org/config\">\n" +
                "  <ns2:description>JAXA CSW Catalog service</ns2:description>\n" +
                "  <ns2:identifier>default</ns2:identifier>\n" +
                "  <ns2:lang>en</ns2:lang>\n" +
                "  <ns2:name>default</ns2:name>\n" +
                "  <ns2:transactional>false</ns2:transactional>\n" +
                "  <ns2:versions>3.0.0</ns2:versions>\n" +
                "  <ns2:versions>2.0.2</ns2:versions>\n" +
                "</ns2:details>";
        serviceRepository.createOrUpdateServiceDetails(sid3, "en", xmlEN, true);

        String xmlFR =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ns2:details xmlns:ns2=\"http://www.constellation.org/config\">\n" +
                "  <ns2:description>JAXA '; delete * from services;'CSW Catalog service</ns2:description>\n" +
                "  <ns2:identifier>default</ns2:identifier>\n" +
                "  <ns2:lang>fr</ns2:lang>\n" +
                "  <ns2:name>default</ns2:name>\n" +
                "  <ns2:transactional>false</ns2:transactional>\n" +
                "  <ns2:versions>3.0.0</ns2:versions>\n" +
                "  <ns2:versions>2.0.2</ns2:versions>\n" +
                "</ns2:details>";
        serviceRepository.createOrUpdateServiceDetails(sid3, "fr", xmlFR, false);

        

        /**
         * service search.
         */
        List<Service> services = serviceRepository.findByType("wms");
        Assert.assertTrue(services.contains(s1));
        Assert.assertTrue(services.contains(s2));
        Assert.assertTrue(services.contains(s3));

        services = serviceRepository.findByType("wm'; s");
        Assert.assertTrue(services.isEmpty());

        Assert.assertEquals(s1,         serviceRepository.findByIdentifierAndType("default", "wms"));
        Assert.assertEquals(s1.getId(), serviceRepository.findIdByIdentifierAndType("default", "wms"));

        Assert.assertEquals(s3,         serviceRepository.findByIdentifierAndType("te'; delete * from st", "wms"));
        Assert.assertEquals(s3.getId(), serviceRepository.findIdByIdentifierAndType("te'; delete * from st", "wms"));

        List<ServiceReference> refs = serviceRepository.fetchByDataId(did1);
        Assert.assertEquals(1, refs.size());
        Assert.assertEquals(new ServiceReference(s1), refs.get(0));


        services = serviceRepository.findByDataId(did1);
        Assert.assertEquals(1, services.size());
        Assert.assertEquals(s1, services.get(0));

        Assert.assertEquals("impl1",   serviceRepository.getImplementation(sid1));
        Assert.assertEquals("impl''3", serviceRepository.getImplementation(sid3));

        /**
         * metadata provider link.
         */
        Assert.assertTrue(serviceRepository.isLinkedMetadataProviderAndService(sid2, pid2));
        Assert.assertFalse(serviceRepository.isLinkedMetadataProviderAndService(sid2, pid3));

        Assert.assertEquals(pid2, serviceRepository.getLinkedMetadataProvider(sid2));
        Assert.assertEquals(s2, serviceRepository.getLinkedMetadataService(pid2));

        serviceRepository.removelinkedMetadataProvider(sid2);
        Assert.assertFalse(serviceRepository.isLinkedMetadataProviderAndService(sid2, pid2));
        Assert.assertNull(serviceRepository.getLinkedMetadataProvider(sid2));
        Assert.assertNull(serviceRepository.getLinkedMetadataService(pid2));

        /**
         * sensor provider link.
         */
        Assert.assertTrue(serviceRepository.getLinkedSensorProviders(sid2).contains(pid3));
        Assert.assertTrue(serviceRepository.getLinkedSOSServices(pid3).contains(s2));

        serviceRepository.removelinkedSensorProviders(sid2);

        Assert.assertFalse(serviceRepository.getLinkedSensorProviders(sid2).contains(pid3));
        Assert.assertFalse(serviceRepository.getLinkedSOSServices(pid3).contains(s2));

        /**
         * service details.
         */
        String details = serviceRepository.getServiceDetails(sid3, "en");
        Assert.assertEquals(xmlEN, details);

        details = serviceRepository.getServiceDetails(sid3, "fr");
        Assert.assertEquals(xmlFR, details);

        details = serviceRepository.getServiceDetails(sid3, "f''r");
        Assert.assertNull(details);

        details = serviceRepository.getServiceDetailsForDefaultLang(sid3);
        Assert.assertEquals(xmlEN, details);

        /**
         * extra config
         */
        Map<String, String> extraConfigs = serviceRepository.getExtraConfig(sid1);
        Assert.assertTrue(extraConfigs.containsKey("file1.xml"));
        Assert.assertTrue(extraConfigs.containsKey("file2.sh"));

        Assert.assertEquals(file1, extraConfigs.get("file1.xml"));
        Assert.assertEquals(file2, extraConfigs.get("file2.sh"));

        Assert.assertEquals(file1, serviceRepository.getExtraConfig(sid1, "file1.xml"));
        Assert.assertNull(serviceRepository.getExtraConfig(sid1, "fil'; 'e1.xml"));


        /**
         * Update
         */
        serviceRepository.updateStatus(sid1, "STOPPED");

        Service s = serviceRepository.findById(sid1);
        Assert.assertEquals("STOPPED", s.getStatus());

        s.setIdentifier("gg'fg");
        s.setImpl("hhh'jj");
        s.setStatus("ddddd'; drop");
        s.setType("s'; delete from admin.services;");
        s.setVersions("sksksksksk");

        serviceRepository.update(s);

        Assert.assertEquals(s, serviceRepository.findById(sid1));

        /**
         * service deletion.
         */
        serviceRepository.delete(sid3);

        s3 = serviceRepository.findById(sid3);
        Assert.assertNull(s3);

        serviceRepository.deleteAll();
    }

}
