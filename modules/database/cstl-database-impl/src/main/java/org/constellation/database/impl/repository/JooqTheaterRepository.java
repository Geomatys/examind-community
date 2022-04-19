/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2022 Geomatys.
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

import com.examind.database.api.jooq.Tables;
import com.examind.database.api.jooq.tables.records.TheaterRecord;
import com.examind.database.api.jooq.tables.records.TheaterSceneRecord;
import java.util.ArrayList;
import java.util.List;
import org.constellation.dto.Theater;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.constellation.repository.TheaterRepository;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqTheaterRepository extends AbstractJooqRespository<TheaterRecord, com.examind.database.api.jooq.tables.pojos.Theater> implements TheaterRepository {

    public JooqTheaterRepository() {
        super(com.examind.database.api.jooq.tables.pojos.Theater.class, Tables.THEATER);
    }
    
    @Override
    public List<Theater> findAll() {
        return convertListToDto(dsl.select().from(Tables.THEATER).fetchInto(com.examind.database.api.jooq.tables.pojos.Theater.class));
    }

    @Override
    public List<Theater> findForScene(Integer sceneId) {
        return convertListToDto(dsl.select(Tables.THEATER.fields())
                                   .from(Tables.THEATER, Tables.THEATER_SCENE)
                                   .where(Tables.THEATER_SCENE.SCENE_ID.eq(sceneId))
                                   .and(Tables.THEATER_SCENE.THEATER_ID.eq(Tables.THEATER.ID))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Theater.class));
    }

    @Override
    public Theater findById(Integer id) {
        return convertToDto(dsl.select().from(Tables.THEATER).where(Tables.THEATER.ID.eq(id)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Theater.class));
    }

    @Override
    public Theater findByName(String name) {
        return convertToDto(dsl.select().from(Tables.THEATER).where(Tables.THEATER.NAME.eq(name)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Theater.class));
    }

    @Override
    public int delete(Integer id) {
        return dsl.delete(Tables.THEATER).where(Tables.THEATER.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addScene(Integer id, Integer sceneId) {
        TheaterSceneRecord newRecord = dsl.newRecord(Tables.THEATER_SCENE);
        newRecord.setSceneId(sceneId);
        newRecord.setTheaterId(id);
        newRecord.store();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeAllScene(Integer id) {
        dsl.deleteFrom(Tables.THEATER_SCENE)
           .where(Tables.THEATER_SCENE.THEATER_ID.eq(id))
           .execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeScene(Integer id, Integer sceneId) {
        dsl.deleteFrom(Tables.THEATER_SCENE)
           .where(Tables.THEATER_SCENE.THEATER_ID.eq(id))
           .and(Tables.THEATER_SCENE.SCENE_ID.eq(sceneId))
           .execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(Theater theater) {
        TheaterRecord newRecord = dsl.newRecord(Tables.THEATER);
        newRecord.from(convertToDao(theater));
        newRecord.store();
        return newRecord.getId();
    }

    private List<Theater> convertListToDto(List<com.examind.database.api.jooq.tables.pojos.Theater> daos) {
        List<org.constellation.dto.Theater> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.Theater dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    private Theater convertToDto(com.examind.database.api.jooq.tables.pojos.Theater dao) {
        if (dao != null) {
            Theater dto = new Theater();
            dto.setId(dao.getId());
            dto.setDataId(dao.getDataId());
            dto.setLayerId(dao.getLayerId());
            dto.setName(dao.getName());
            dto.setType(dao.getType());
            return dto;
        }
        return null;
    }

    private com.examind.database.api.jooq.tables.pojos.Theater convertToDao(Theater dto) {
        if (dto != null) {
            com.examind.database.api.jooq.tables.pojos.Theater dao = new com.examind.database.api.jooq.tables.pojos.Theater();
            dao.setDataId(dto.getDataId());
            dao.setLayerId(dto.getLayerId());
            dao.setType(dto.getType());
            dao.setName(dto.getName());
            dao.setId(dto.getId());
            return dao;
        }
        return null;
    }
}
