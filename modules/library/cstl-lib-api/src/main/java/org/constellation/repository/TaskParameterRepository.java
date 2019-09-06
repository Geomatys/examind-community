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

import org.constellation.dto.process.TaskParameter;

public interface TaskParameterRepository {

    /**
     * Return all the registered task parameters.
     * @return
     */
     List<? extends TaskParameter> findAll();

    /**
     * Return all the registered task parameters wih the specified type.
     * @return
     */
     List<? extends TaskParameter> findAllByType(String type);

    /**
     * Return all the registered task parameters wih the specified name, authority and code.
     * @return
     */
     List<? extends TaskParameter> findAllByNameAndProcess(String name, String authority, String code);

    /**
     * Insert a new Task parameter into the datasource.
     *
     * @param task the task parameter to insert
     * @return
     */
     Integer create(TaskParameter task);

    /**
     * Return a task identifier by the specified id.
     *
     * @param id An identifier.
     * @return
     */
    TaskParameter get(Integer id);

    /**
     * Remove a task parameter.
     *
     * @param taskId
     */
     void delete(Integer taskId);

     /**
     * Remove all task parameters.
     */
     void deleteAll();

    /**
     * Update a task parameter.
     *
     * @param task
     */
     void update(TaskParameter task);

    /**
     * Return all the registered task parameters with a trigger.
     *
     * @return
     */
     List<? extends TaskParameter> findProgrammedTasks();
 }
