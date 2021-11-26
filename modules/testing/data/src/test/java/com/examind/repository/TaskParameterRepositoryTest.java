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
import org.constellation.dto.process.TaskParameter;
import org.constellation.repository.TaskParameterRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TaskParameterRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TaskParameterRepository taskParamRepository;

    public void crude() {
        taskParamRepository.deleteAll();
        List<? extends TaskParameter> all = taskParamRepository.findAll();
        Assert.assertTrue(all.isEmpty());


        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        /**
         * Insertion
         */
        Integer tpid1 = taskParamRepository.create(TestSamples.newTaskParameter(owner.getId(), "auth", "code"));
        Assert.assertNotNull(tpid1);

        TaskParameter tp1 = taskParamRepository.get(tpid1);
        Assert.assertNotNull(tp1);

        Integer tpid2 = taskParamRepository.create(TestSamples.newTaskParameterQuote(owner.getId(), "aut';h", "co';de"));
        Assert.assertNotNull(tpid2);

        TaskParameter tp2 = taskParamRepository.get(tpid2);
        Assert.assertNotNull(tp2);

        /**
         * Search
         */
        List<? extends TaskParameter> tps = taskParamRepository.findAllByNameAndProcess("na'med", "aut';h", "co';de");
        Assert.assertEquals(1, tps.size());
        Assert.assertEquals(tp2, tps.get(0));

        tps = taskParamRepository.findAllByType("t'ype");
        Assert.assertEquals(1, tps.size());
        Assert.assertEquals(tp2, tps.get(0));

        tps = taskParamRepository.findProgrammedTasks();
        Assert.assertEquals(1, tps.size());
        Assert.assertEquals(tp2, tps.get(0));

        /**
         * Update
         */
        tp1.setInputs("inn'p; puts");
        taskParamRepository.update(tp1);
        TaskParameter tp = taskParamRepository.get(tpid1);
        Assert.assertEquals("inn'p; puts", tp.getInputs());
        // date has changed
        tp1.setDate(null);
        tp.setDate(null);
        Assert.assertEquals(tp1, tp);

        /**
         * deletetion
         */
        taskParamRepository.delete(tp1.getId());

        tp1 = taskParamRepository.get(tp1.getId());
        Assert.assertNull(tp1);

        taskParamRepository.deleteAll();
    }

}

