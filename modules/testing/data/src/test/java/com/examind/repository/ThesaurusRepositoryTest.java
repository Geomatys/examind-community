/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
import org.constellation.dto.CstlUser;
import org.constellation.dto.thesaurus.Thesaurus;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.ThesaurusRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ThesaurusRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ThesaurusRepository thesaurusRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    public void crude() {

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        thesaurusRepository.deleteAll();
        serviceRepository.deleteAll();

        List<Thesaurus> all = thesaurusRepository.getAll();
        Assert.assertTrue(all.isEmpty());

        Integer sid1 = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid1);

        Integer sid2 = serviceRepository.create(TestSamples.newServiceQuote(owner.getId()));
        Assert.assertNotNull(sid2);

        Integer tid1 = thesaurusRepository.create(TestSamples.newThesaurus());
        Assert.assertNotNull(tid1);

        Thesaurus th1 = thesaurusRepository.get(tid1);
        Assert.assertNotNull(th1);

        Integer tid2 = thesaurusRepository.create(TestSamples.newThesaurusQuote());
        Assert.assertNotNull(tid2);

        Thesaurus th2 = thesaurusRepository.get(tid2);
        Assert.assertNotNull(th2);
        
        /**
         * search
         */
        Thesaurus th = thesaurusRepository.getByName("thesau'; drop table admin.thesaurus; '1");
        Assert.assertEquals(th2, th);

        th = thesaurusRepository.getByUri("ur''n:th");
        Assert.assertEquals(th2, th);

        /**
         * Link
         */
        thesaurusRepository.linkThesaurusAndService(tid1, sid1);
        thesaurusRepository.linkThesaurusAndService(tid2, sid2);

        List<String> linkedUris = thesaurusRepository.getLinkedThesaurusUri(sid1);
        Assert.assertEquals(1, linkedUris.size());
        Assert.assertEquals("urn:th", linkedUris.get(0));

        List<Thesaurus> linkedthws = thesaurusRepository.getLinkedThesaurus(sid2);
        Assert.assertEquals(1, linkedthws.size());
        Assert.assertEquals(th2, linkedthws.get(0));

        /**
         * Update
         */
        th = thesaurusRepository.get(tid1);
        th.setDescription("sjsjsj',");
        thesaurusRepository.update(th);

        th = thesaurusRepository.get(tid1);
        Assert.assertEquals("sjsjsj',", th.getDescription());

        /**
         * delete
         */
        thesaurusRepository.delete(tid1);

        th1 = thesaurusRepository.get(tid1);
        Assert.assertNull(th1);

        thesaurusRepository.deleteAll();
        serviceRepository.deleteAll();
    }

}
