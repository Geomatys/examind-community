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
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    public void crude() {
        // no removeAll method
        List<? extends Task> all = taskRepository.findAll();
        for (Task p : all) {
            taskRepository.delete(p.getIdentifier());
        }
        all = taskRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());


        String uuid = taskRepository.create(TestSamples.newTask(owner.getId(), "999-666"));
        Assert.assertNotNull(uuid);

        Task t = taskRepository.get(uuid);
        Assert.assertNotNull(t);

        taskRepository.delete(uuid);

        t = taskRepository.get(uuid);
        Assert.assertNull(t);

    }
}
