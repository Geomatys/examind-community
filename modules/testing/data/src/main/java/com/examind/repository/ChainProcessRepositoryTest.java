/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
import org.constellation.dto.process.ChainProcess;
import org.constellation.repository.ChainProcessRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ChainProcessRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ChainProcessRepository chainProcessRepository;

    @Test
    @Transactional()
    public void crude() {
        chainProcessRepository.deleteAll();
        List<ChainProcess> all = chainProcessRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        int cip = chainProcessRepository.create(TestSamples.newChainProcess());
        Assert.assertNotNull(cip);

        int i = chainProcessRepository.findId("test", "001");
        Assert.assertEquals(cip, i);

        ChainProcess cp1 = chainProcessRepository.findOne("test", "001");
        Assert.assertNotNull(cp1);

        int cip2 = chainProcessRepository.create(TestSamples.newChainProcessQuote());
        Assert.assertNotNull(cip2);

        ChainProcess cp2 = chainProcessRepository.findOne("tes't", "'001'");
        Assert.assertNotNull(cp2);


        chainProcessRepository.delete(cip);
        cp1 = chainProcessRepository.findOne("test", "001");
        Assert.assertNull(cp1);

        chainProcessRepository.delete("tes't", "'001'");
        cp2 = chainProcessRepository.findOne("tes't", "'001'");
        Assert.assertNull(cp2);

        
    }

}
