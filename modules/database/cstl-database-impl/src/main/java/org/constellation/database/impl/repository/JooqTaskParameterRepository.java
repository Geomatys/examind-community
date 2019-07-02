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
import java.util.Date;
import java.util.List;

import org.constellation.database.api.jooq.Tables;
import org.constellation.dto.process.TaskParameter;
import org.constellation.database.api.jooq.tables.records.TaskParameterRecord;
import org.constellation.repository.TaskParameterRepository;
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
public class JooqTaskParameterRepository extends AbstractJooqRespository<TaskParameterRecord, org.constellation.database.api.jooq.tables.pojos.TaskParameter> implements TaskParameterRepository {

    public JooqTaskParameterRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.TaskParameter.class, Tables.TASK_PARAMETER);
    }

    @Override
    public List<? extends TaskParameter> findAllByType(String type) {
        return convertTaskParamListToDto(dsl.select()
                                            .from(Tables.TASK_PARAMETER)
                                            .where(Tables.TASK_PARAMETER.TYPE.eq(type))
                                            .fetchInto( org.constellation.database.api.jooq.tables.pojos.TaskParameter.class));
    }

    @Override
    public List<? extends TaskParameter> findAllByNameAndProcess(String name, String authority, String code) {
        return convertTaskParamListToDto(dsl.select()
                                            .from(Tables.TASK_PARAMETER)
                                            .where(Tables.TASK_PARAMETER.NAME.eq(name)
                                            .and(Tables.TASK_PARAMETER.PROCESS_AUTHORITY.eq(authority))
                                            .and(Tables.TASK_PARAMETER.PROCESS_CODE.eq(code)))
                                            .fetchInto(org.constellation.database.api.jooq.tables.pojos.TaskParameter.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(TaskParameter task ) {
        TaskParameterRecord newRecord = dsl.newRecord(Tables.TASK_PARAMETER);
        newRecord.from(convertToDao(task));
        newRecord.store();
        return newRecord.into(TaskParameter.class).getId();
    }

    @Override
    public TaskParameter get(Integer uuid) {
        return convertToDto(dsl.select()
                               .from(Tables.TASK_PARAMETER)
                               .where(Tables.TASK_PARAMETER.ID.eq(uuid))
                               .fetchOneInto(org.constellation.database.api.jooq.tables.pojos.TaskParameter.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(Integer taskId) {
        dsl.delete(Tables.TASK_PARAMETER).where(Tables.TASK_PARAMETER.ID.eq(taskId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteAll() {
        dsl.delete(Tables.TASK_PARAMETER).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(TaskParameter task) {
        dsl.update(Tables.TASK_PARAMETER)
                .set(Tables.TASK_PARAMETER.NAME, task.getName())
                .set(Tables.TASK_PARAMETER.DATE, new Date().getTime())
                .set(Tables.TASK_PARAMETER.PROCESS_AUTHORITY, task.getProcessAuthority())
                .set(Tables.TASK_PARAMETER.PROCESS_CODE, task.getProcessCode())
                .set(Tables.TASK_PARAMETER.INPUTS, task.getInputs())
                .set(Tables.TASK_PARAMETER.TRIGGER_TYPE, task.getTriggerType())
                .set(Tables.TASK_PARAMETER.TYPE, task.getType())
                .set(Tables.TASK_PARAMETER.TRIGGER, task.getTrigger())
                .where(Tables.TASK_PARAMETER.ID.eq(task.getId()))
                .execute();
    }

    @Override
    public List<? extends TaskParameter> findProgrammedTasks() {
        return convertTaskParamListToDto(dsl.select()
                                            .from(Tables.TASK_PARAMETER)
                                            .where(Tables.TASK_PARAMETER.TRIGGER.isNotNull())
                                            .fetchInto(org.constellation.database.api.jooq.tables.pojos.TaskParameter.class));
    }

    @Override
    public List<TaskParameter> findAll() {
        return convertTaskParamListToDto(dsl.select()
                                            .from(Tables.TASK_PARAMETER)
                                            .fetchInto(org.constellation.database.api.jooq.tables.pojos.TaskParameter.class));
    }

    private TaskParameter convertToDto(org.constellation.database.api.jooq.tables.pojos.TaskParameter dao) {
        if (dao != null) {
            TaskParameter dto = new TaskParameter();
            dto.setDate(dao.getDate());
            dto.setId(dao.getId());
            dto.setInputs(dao.getInputs());
            dto.setName(dao.getName());
            dto.setOwner(dao.getOwner());
            dto.setProcessAuthority(dao.getProcessAuthority());
            dto.setProcessCode(dao.getProcessCode());
            dto.setTrigger(dao.getTrigger());
            dto.setTriggerType(dao.getTriggerType());
            dto.setType(dao.getType());
            return dto;
        }
        return null;
    }

    private org.constellation.database.api.jooq.tables.pojos.TaskParameter convertToDao(TaskParameter dto) {
        if (dto != null) {
            org.constellation.database.api.jooq.tables.pojos.TaskParameter dao = new org.constellation.database.api.jooq.tables.pojos.TaskParameter();
            dao.setDate(dto.getDate());
            dao.setId(dto.getId());
            dao.setInputs(dto.getInputs());
            dao.setName(dto.getName());
            dao.setOwner(dto.getOwner());
            dao.setProcessAuthority(dto.getProcessAuthority());
            dao.setProcessCode(dto.getProcessCode());
            dao.setTrigger(dto.getTrigger());
            dao.setTriggerType(dto.getTriggerType());
            dao.setType(dto.getType());
            return dao;
        }
        return null;
    }

    private List<TaskParameter> convertTaskParamListToDto(List<? extends org.constellation.database.api.jooq.tables.pojos.TaskParameter> daos) {
        List<TaskParameter> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.TaskParameter dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

}
