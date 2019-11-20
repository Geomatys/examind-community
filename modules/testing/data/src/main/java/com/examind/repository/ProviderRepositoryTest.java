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
import org.constellation.repository.ProviderRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.service.Service;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ServiceRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProviderRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Test
    public void all() {
        dump(providerRepository.findAll());
    }

    @Test
    public void crude() {

        List<Service> serv = serviceRepository.findAll();
        for (Service p : serv) {
            serviceRepository.delete(p.getId());
        }

        // no removeAll method
        List<ProviderBrief> all = providerRepository.findAll();
        for (ProviderBrief p : all) {
            providerRepository.delete(p.getId());
        }
        all = providerRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());


        /**
         * provider insertion
         */
        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        ProviderBrief pr = providerRepository.findOne(pid);
        Assert.assertNotNull(pr);

        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, null));
        Assert.assertNotNull(did1);

        /**
         * provider search
         */
        Assert.assertEquals(pr, providerRepository.findByIdentifier("provider-test"));
        Assert.assertEquals(pr.getId(), providerRepository.findIdForIdentifier("provider-test"));

        Assert.assertTrue(providerRepository.findByImpl("immmmp").contains(pr));

        Assert.assertTrue(providerRepository.findForData(did1).equals(pr));
        /**
         * provider deletion
         */
        int res = providerRepository.delete(pid);
        Assert.assertEquals(1, res);

        res = providerRepository.delete(-1);
        Assert.assertEquals(0, res);

        pr = providerRepository.findOne(pid);
        Assert.assertNull(pr);

        //cleanup
        dataRepository.delete(did1);
    }
}
