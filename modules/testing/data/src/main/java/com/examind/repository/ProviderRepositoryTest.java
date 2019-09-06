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
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class ProviderRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void all() {
        dump(providerRepository.findAll());
    }

    @Test
    @Transactional()
    public void crude() {

        // no removeAll method
        List<ProviderBrief> all = providerRepository.findAll();
        for (ProviderBrief p : all) {
            providerRepository.delete(p.getId());
        }
        all = providerRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        ProviderBrief pr = providerRepository.findOne(pid);
        Assert.assertNotNull(pr);

        int res = providerRepository.delete(pid);
        Assert.assertEquals(1, res);

        res = providerRepository.delete(-1);
        Assert.assertEquals(0, res);

        pr = providerRepository.findOne(pid);
        Assert.assertNull(pr);

    }
}
