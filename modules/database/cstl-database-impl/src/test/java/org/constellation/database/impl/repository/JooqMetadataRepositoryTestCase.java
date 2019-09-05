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
import org.constellation.dto.metadata.Metadata;
import org.constellation.repository.MetadataRepository;
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Transactional
public class JooqMetadataRepositoryTestCase extends AbstractJooqTestTestCase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MetadataRepository metadataRepository;

    @Test
    @Transactional()
    public void crude() {

        metadataRepository.deleteAll();
        List<Metadata> all = metadataRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        int mid = metadataRepository.create(TestSamples.newMetadata(owner.getId(), "meta-1"));
        Assert.assertNotNull(mid);

        Metadata s = metadataRepository.findById(mid);
        Assert.assertNotNull(s);

        metadataRepository.delete(mid);

        s = metadataRepository.findById(mid);
        Assert.assertNull(s);
    }
}
