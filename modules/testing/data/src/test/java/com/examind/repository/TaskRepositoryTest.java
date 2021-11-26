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
import org.constellation.repository.TaskRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.process.Task;
import org.constellation.repository.TaskParameterRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskParameterRepository taskParamRepository;
    
    public void crude() {
        taskRepository.deleteAll();
        taskParamRepository.deleteAll();

        List<? extends Task> all = taskRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer tpid1 = taskParamRepository.create(TestSamples.newTaskParameter(owner.getId(), "auth", "code"));
        Assert.assertNotNull(tpid1);

        Integer tpid2 = taskParamRepository.create(TestSamples.newTaskParameterQuote(owner.getId(), "aut';h", "co';de"));
        Assert.assertNotNull(tpid2);

        String uuid1 = taskRepository.create(TestSamples.newTask(owner.getId(), "999-666", tpid1));
        Assert.assertNotNull(uuid1);

        Task t1 = taskRepository.get(uuid1);
        Assert.assertNotNull(t1);

        String uuid2 = taskRepository.create(TestSamples.newTaskQuote(owner.getId(), "999-'; delete * from *;'666", tpid2));
        Assert.assertNotNull(uuid2);

        Task t2 = taskRepository.get(uuid2);
        Assert.assertNotNull(t2);
        Assert.assertEquals(tpid2, t2.getTaskParameterId());

        List<? extends Task> tasks = taskRepository.findRunningTasks();
        Assert.assertEquals(1, tasks.size());
        Assert.assertEquals(t2, tasks.get(0));

        tasks = taskRepository.findRunningTasks(tpid2, 0, 10);
        Assert.assertEquals(1, tasks.size());
        Assert.assertEquals(t2, tasks.get(0));

        tasks = taskRepository.taskHistory(tpid1, 0, 10);
        Assert.assertEquals(1, tasks.size());
        Assert.assertEquals(t1, tasks.get(0));

        taskRepository.delete(uuid1);

        t1 = taskRepository.get(uuid1);
        Assert.assertNull(t1);

        taskRepository.deleteAll();
        taskParamRepository.deleteAll();

    }
}
