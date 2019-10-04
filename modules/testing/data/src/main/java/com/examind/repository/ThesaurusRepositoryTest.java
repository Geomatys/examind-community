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
import org.junit.Test;
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

    @Test
    public void crude() {

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        // no removeAll method
        List<Thesaurus> all = thesaurusRepository.getAll();
        for (Thesaurus p : all) {
            thesaurusRepository.delete(p.getId());
        }
        all = thesaurusRepository.getAll();
        Assert.assertTrue(all.isEmpty());

        Integer sid = thesaurusRepository.create(TestSamples.newThesaurus());
        Assert.assertNotNull(sid);

        Thesaurus s = thesaurusRepository.get(sid);
        Assert.assertNotNull(s);

        thesaurusRepository.delete(s.getId());

        s = thesaurusRepository.get(s.getId());
        Assert.assertNull(s);
    }

    @Test
    public void thesaurusLinkTest() {
        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer sid = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid);

        Integer tid = thesaurusRepository.create(TestSamples.newThesaurus());
        Assert.assertNotNull(tid);

        thesaurusRepository.linkThesaurusAndService(tid, sid);

        List<String> linkedUris = thesaurusRepository.getLinkedThesaurusUri(sid);
        Assert.assertEquals(1, linkedUris.size());

        Assert.assertEquals(TestSamples.newThesaurus().getUri(), linkedUris.get(0));

        List<Thesaurus> linkedthws = thesaurusRepository.getLinkedThesaurus(sid);
        Assert.assertEquals(1, linkedthws.size());

        Assert.assertEquals(TestSamples.newThesaurus().getUri(), linkedthws.get(0).getUri());

        thesaurusRepository.delete(tid);
        serviceRepository.delete(tid);
    }

}
