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
import org.constellation.repository.DataRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class DataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Test
    public void findAll() {
        dump(dataRepository.findAll());
    }

    @Test
    @Transactional()
    public void crud() {

        // no removeAll method
        List<Data> all = dataRepository.findAll();
        for (Data p : all) {
            dataRepository.delete(p.getId());
        }
        all = dataRepository.findAll();
        Assert.assertTrue(all.isEmpty());


        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        Data data = dataRepository.create(TestSamples.newData(owner.getId(), pid));
        Assert.assertNotNull(data);
        Assert.assertNotNull(data.getId());

        int res = dataRepository.delete(data.getId());

        Assert.assertEquals(1, res);

        res = dataRepository.delete(-1);
        Assert.assertEquals(0, res);

        data = dataRepository.findById(data.getId());
        Assert.assertNull(data);
    }

}
