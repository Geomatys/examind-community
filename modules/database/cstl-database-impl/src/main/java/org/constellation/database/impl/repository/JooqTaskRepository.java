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
package org.constellation.database.impl.repository;

import java.util.ArrayList;

import java.util.List;

import org.constellation.database.api.jooq.Tables;
import org.constellation.dto.process.Task;
import org.constellation.database.api.jooq.tables.records.TaskRecord;
import org.constellation.repository.TaskRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Rouby (Geomatys)
 * @author Christophe Mourette (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqTaskRepository extends AbstractJooqRespository<TaskRecord, org.constellation.database.api.jooq.tables.pojos.Task> implements TaskRepository {

    public JooqTaskRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.Task.class, Tables.TASK);
    }


    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String create(Task task) {
        TaskRecord newRecord = dsl.newRecord(Tables.TASK);
        if (task.getDateStart() == null) {
            task.setDateStart(System.currentTimeMillis());
        }
        newRecord.from(convertToDao(task));
        newRecord.store();
        return newRecord.into(Task.class).getIdentifier();
    }

    @Override
    public Task get(String uuid) {
        return convertToDto(dsl.select().from(Tables.TASK).where(Tables.TASK.IDENTIFIER.eq(uuid)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Task.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Task task) {
        dsl.update(Tables.TASK)
                .set(Tables.TASK.TASK_PARAMETER_ID, task.getTaskParameterId())
                .set(Tables.TASK.TYPE, task.getType())
                .set(Tables.TASK.STATE, task.getState())
                .set(Tables.TASK.DATE_START, task.getDateStart())
                .set(Tables.TASK.DATE_END, task.getDateEnd())
                .set(Tables.TASK.MESSAGE, task.getMessage())
                .set(Tables.TASK.OWNER, task.getOwner())
                .set(Tables.TASK.PROGRESS, task.getProgress())
                .set(Tables.TASK.TASK_OUTPUT, task.getTaskOutput())
                .where(Tables.TASK.IDENTIFIER.eq(task.getIdentifier())).execute();
    }

    @Override
    public List<Task> findRunningTasks() {
        return convertTaskListToDto(dsl.select().from(Tables.TASK)
                .where(Tables.TASK.DATE_END.isNull())
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Task.class));
    }

    @Override
    public List<Task> findRunningTasks(Integer id, Integer offset, Integer limit) {
        return convertTaskListToDto(dsl.select().from(Tables.TASK)
                .where(Tables.TASK.DATE_END.isNull().and(Tables.TASK.TASK_PARAMETER_ID.eq(id)))
                .orderBy(Tables.TASK.DATE_END.desc())
                .limit(limit).offset(offset)
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Task.class));
    }

    @Override
    public List<Task> taskHistory(Integer id, Integer offset, Integer limit) {
        return convertTaskListToDto(dsl.select().from(Tables.TASK)
                .where(Tables.TASK.TASK_PARAMETER_ID.eq(id))
                .andNot(Tables.TASK.DATE_END.isNull())
                .orderBy(Tables.TASK.DATE_END.desc())
                .limit(limit).offset(offset)
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Task.class));
    }


    @Override
    public List<Task> findAll() {
        return convertTaskListToDto(dsl.select().from(Tables.TASK).fetchInto(org.constellation.database.api.jooq.tables.pojos.Task.class));
    }

    @Override
    public int delete(String uuid) {
        return dsl.delete(Tables.TASK).where(Tables.TASK.IDENTIFIER.eq(uuid)).execute();
    }

    @Override
    public int deleteAll() {
        return dsl.delete(Tables.TASK).execute();
    }

    private List<Task> convertTaskListToDto(List<org.constellation.database.api.jooq.tables.pojos.Task> daos) {
        List<org.constellation.dto.process.Task> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Task dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    private Task convertToDto(org.constellation.database.api.jooq.tables.pojos.Task dao) {
        if (dao != null) {
            Task dto = new Task();
            dto.setDateEnd(dao.getDateEnd());
            dto.setDateStart(dao.getDateStart());
            dto.setIdentifier(dao.getIdentifier());
            dto.setMessage(dao.getMessage());
            dto.setOwner(dao.getOwner());
            dto.setProgress(dao.getProgress());
            dto.setState(dao.getState());
            dto.setTaskOutput(dao.getTaskOutput());
            dto.setTaskParameterId(dao.getTaskParameterId());
            dto.setType(dao.getType());
            return dto;
        }
        return null;
    }

    private org.constellation.database.api.jooq.tables.pojos.Task convertToDao(Task dto) {
        if (dto != null) {
            org.constellation.database.api.jooq.tables.pojos.Task dao = new org.constellation.database.api.jooq.tables.pojos.Task();
            dao.setDateEnd(dto.getDateEnd());
            dao.setDateStart(dto.getDateStart());
            dao.setIdentifier(dto.getIdentifier());
            dao.setMessage(dto.getMessage());
            dao.setOwner(dto.getOwner());
            dao.setProgress(dto.getProgress());
            dao.setState(dto.getState());
            dao.setTaskOutput(dto.getTaskOutput());
            dao.setTaskParameterId(dto.getTaskParameterId());
            dao.setType(dto.getType());
            return dao;
        }
        return null;
    }
}
