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
package org.constellation.database.impl.repository;

import java.util.List;
import org.constellation.database.impl.AbstractJooqTestTestCase;
import org.constellation.database.impl.TestSamples;
import org.constellation.dto.CstlUser;
import org.constellation.dto.thesaurus.Thesaurus;
import org.constellation.repository.ThesaurusRepository;
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JooqThesaurusRepositoryTestCase extends AbstractJooqTestTestCase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThesaurusRepository thesaurusRepository;

    @Test
    @Transactional()
    public void crude() {

        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        // no removeAll method
        List<Thesaurus> all = thesaurusRepository.getAll();
        for (Thesaurus p : all) {
            thesaurusRepository.delete(p.getId());
        }
        all = thesaurusRepository.getAll();
        Assert.assertTrue(all.isEmpty());

        int sid = thesaurusRepository.create(TestSamples.newThesaurus());
        Assert.assertNotNull(sid);

        Thesaurus s = thesaurusRepository.get(sid);
        Assert.assertNotNull(s);

        thesaurusRepository.delete(s.getId());

        s = thesaurusRepository.get(s.getId());
        Assert.assertNull(s);
    }

}
