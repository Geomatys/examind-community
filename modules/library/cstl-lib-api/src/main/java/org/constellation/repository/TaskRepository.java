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
package org.constellation.repository;

import java.util.List;

import org.constellation.dto.process.Task;

 public interface TaskRepository {

    /**
     * Return all the registered tasks.
     * @return
     */
     List<? extends Task> findAll();

    /**
     * Insert a new task into the datasource.
     *
     * @param task the task to insert
     * @return the task identifier.
     */
     String create(Task task);

    /**
     * Return a task identifier by the specified uuid.
     *
     * @param uuid An identifier.
     * @return
     */
     Task get(String uuid);

    /**
     * Update a task.
     *
     * @param task
     */
     void update(Task task);

     void delete(String uuid);
     
    /**
     * List all the tasks with the state "RUNNING".
     *
     * @return
     */
     List<Task> findRunningTasks();

    /**
     * List all the tasks with the state "RUNNING" for the specified task parameter.
     *
     * @param id Task parameter id.
     * @param offset
     * @param limit maximum number of results returned.
     * @return
     */
     List<Task> findRunningTasks(Integer id, Integer offset, Integer limit);

    /**
     * List all the tasks for the specified task parameter.
     *
     * @param id Task parameter id.
     * @param offset
     * @param limit maximum number of results returned.
     * @return
    */
     List<Task> taskHistory(Integer id, Integer offset, Integer limit);

 }

