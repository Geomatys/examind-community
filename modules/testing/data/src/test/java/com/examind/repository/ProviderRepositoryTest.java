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
import org.constellation.repository.DataRepository;
import org.constellation.repository.ServiceRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

public class ProviderRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ProviderRepository providerRepository;

    public void all() {
        dump(providerRepository.findAll());
    }

    public void crude() {

        dataRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();
        
        List<ProviderBrief> all = providerRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        /**
         * provider insertion
         */
        Integer pid1 = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid1);

        Integer pid2 = providerRepository.create(TestSamples.newProvider2(owner.getId()));
        Assert.assertNotNull(pid2);

        Integer pid3 = providerRepository.create(TestSamples.newProviderQuote(owner.getId()));
        Assert.assertNotNull(pid3);

        ProviderBrief pr1 = providerRepository.findOne(pid1);
        Assert.assertNotNull(pr1);

        ProviderBrief pr2 = providerRepository.findOne(pid2);
        Assert.assertNotNull(pr2);

        ProviderBrief pr3 = providerRepository.findOne(pid3);
        Assert.assertNotNull(pr3);

        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid1, null));
        Assert.assertNotNull(did1);

        /**
         * provider search
         */
        Assert.assertEquals(pr1, providerRepository.findByIdentifier("provider-test"));
        Assert.assertEquals(pr1.getId(), providerRepository.findIdForIdentifier("provider-test"));

        Assert.assertEquals(pr3, providerRepository.findByIdentifier("provider-'; drop table admin.provider; 'test3"));
        Assert.assertEquals(pr3.getId(), providerRepository.findIdForIdentifier("provider-'; drop table admin.provider; 'test3"));

        Assert.assertTrue(providerRepository.findByImpl("immmmp").contains(pr1));

        Assert.assertTrue(providerRepository.findByImpl("i'mmmmp").contains(pr3));

        Assert.assertTrue(providerRepository.findForData(did1).equals(pr1));

        pr2.setImpl("ol'");
        providerRepository.update(pr2);

        Assert.assertTrue(providerRepository.findByImpl("ol'").contains(pr2));

        /**
         * provider deletion
         */
        int res = providerRepository.delete(pid1);
        Assert.assertEquals(1, res);

        res = providerRepository.delete(-1);
        Assert.assertEquals(0, res);

        pr1 = providerRepository.findOne(pid1);
        Assert.assertNull(pr1);

        res = providerRepository.deleteByIdentifier("provider-'; drop table admin.provider; 'test3");
        Assert.assertEquals(1, res);

        pr3 = providerRepository.findOne(pid3);
        Assert.assertNull(pr3);

        //cleanup
        dataRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();
    }
}
