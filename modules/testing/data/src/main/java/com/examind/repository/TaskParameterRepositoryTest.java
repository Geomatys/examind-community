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
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class TaskParameterRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskParameterRepository taskParamRepository;

    @Test
    @Transactional()
    public void crude() {

        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        // no removeAll method
        List<? extends TaskParameter> all = taskParamRepository.findAll();
        for (TaskParameter p : all) {
            taskParamRepository.delete(p.getId());
        }
        all = taskParamRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        int sid = taskParamRepository.create(TestSamples.newTaskParameter(owner.getId(), "auth", "code"));
        Assert.assertNotNull(sid);

        TaskParameter s = taskParamRepository.get(sid);
        Assert.assertNotNull(s);

        taskParamRepository.delete(s.getId());

        s = taskParamRepository.get(s.getId());
        Assert.assertNull(s);
    }

}

