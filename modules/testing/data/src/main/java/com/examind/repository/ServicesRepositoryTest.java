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

import org.constellation.dto.service.Service;
import org.constellation.repository.ServiceRepository;
import org.constellation.dto.CstlUser;
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ServicesRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;


    @Test
    public void all() {
        dump(serviceRepository.findAll());
    }

    @Test
    @Transactional()
    public void crud() {

        // no removeAll method
        List<Service> all = serviceRepository.findAll();
        for (Service p : all) {
            serviceRepository.delete(p.getId());
        }
        all = serviceRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        /**
         * service insertion
         */
        Integer sid = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid);

        Service s = serviceRepository.findById(sid);
        Assert.assertNotNull(s);

        /**
         * service search
         */
        Assert.assertEquals(s, serviceRepository.findByIdentifierAndType("default", "wms"));
        Assert.assertTrue(serviceRepository.findByType("wms").contains(s));
        Assert.assertEquals(new Integer(s.getId()), serviceRepository.findIdByIdentifierAndType("default", "wms"));

        /**
         * service deletion
         */
        serviceRepository.delete(sid);

        s = serviceRepository.findById(sid);
        Assert.assertNull(s);
    }

}
