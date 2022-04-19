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
import com.examind.database.api.jooq.tables.records.SceneRecord;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.constellation.dto.Scene;
import org.constellation.repository.SceneRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqSceneRepository extends AbstractJooqRespository<SceneRecord, com.examind.database.api.jooq.tables.pojos.Scene> implements SceneRepository {

    public JooqSceneRepository() {
        super(com.examind.database.api.jooq.tables.pojos.Scene.class, Tables.SCENE);
    }

    @Override
    public List<Scene> findAll() {
        return convertListToDto(dsl.select().from(Tables.SCENE).fetchInto(com.examind.database.api.jooq.tables.pojos.Scene.class));
    }

    @Override
    public Scene findById(Integer id) {
        return convertToDto(dsl.select().from(Tables.SCENE).where(Tables.SCENE.ID.eq(id)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Scene.class));
    }

    @Override
    public Scene findByName(String name) {
        return convertToDto(dsl.select().from(Tables.SCENE).where(Tables.SCENE.NAME.eq(name)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Scene.class));
    }

    @Override
    public boolean isUsedName(String name) {
        return dsl.fetchCount(dsl.select().from(Tables.SCENE).where(Tables.SCENE.NAME.eq(name))) > 0;
    }

    @Override
    public int delete(Integer id) {
        return dsl.delete(Tables.SCENE).where(Tables.SCENE.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void create(Scene scene) {
        SceneRecord newRecord = dsl.newRecord(Tables.SCENE);
        newRecord.from(convertToDao(scene));
        newRecord.store();
    }

    @Override
    public List<Scene> getTheatherScene(Integer theaterId) {
        return convertListToDto(dsl.select(Tables.SCENE.fields())
                                   .from(Tables.SCENE, Tables.THEATER_SCENE)
                                   .where(Tables.THEATER_SCENE.THEATER_ID.eq(theaterId))
                                   .and(Tables.THEATER_SCENE.SCENE_ID.eq(Tables.SCENE.ID))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Scene.class));

    }


    private List<Scene> convertListToDto(List<com.examind.database.api.jooq.tables.pojos.Scene> daos) {
        List<org.constellation.dto.Scene> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.Scene dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    private Scene convertToDto(com.examind.database.api.jooq.tables.pojos.Scene dao) {
        if (dao != null) {
            Scene dto = new Scene();
            dto.setPublicationId(dao.getId());
            dto.setDataId(dao.getDataId());
            dto.setLayerId(dao.getLayerId());
            dto.setTitle(dao.getName());
            dto.setCreationDate(new Date(dao.getCreationDate()));
            dto.setExtras(dao.getExtras());
            dto.setMapContext(dao.getMapContextId());
            dto.setMaxLod(dao.getMaxLod());
            dto.setMinLod(dao.getMinLod());
            dto.setStatus(dao.getStatus());
            dto.setSurface(dao.getSurface());
            dto.setSurfaceFactor(dao.getSurfaceFactor());
            dto.setSurfaceParameters(dao.getSurfaceParameters());
            dto.setTime(dao.getTime());
            dto.setVectorSimplifyFactor(dao.getVectorSimplifyFactor());
            dto.setFormat(dao.getType());
            dto.setBboxMinX(dao.getBboxMinx());
            dto.setBboxMaxX(dao.getBboxMaxx());
            dto.setBboxMinY(dao.getBboxMiny());
            dto.setBboxMaxY(dao.getBboxMaxy());
            return dto;
        }
        return null;
    }

    private com.examind.database.api.jooq.tables.pojos.Scene convertToDao(Scene dto) {
        if (dto != null) {
            com.examind.database.api.jooq.tables.pojos.Scene dao = new com.examind.database.api.jooq.tables.pojos.Scene();
            dao.setId(dto.getPublicationId());
            dao.setDataId(dto.getDataId());
            dao.setLayerId(dto.getLayerId());
            dao.setName(dto.getTitle());
            dao.setCreationDate(dto.getCreationDate().getTime());
            dao.setExtras(dto.getExtras());
            dao.setMapContextId(dto.getMapContext());
            dao.setMaxLod(dto.getMaxLod());
            dao.setMinLod(dto.getMinLod());
            dao.setStatus(dto.getStatus());
            dao.setSurface(dto.getSurface());
            dao.setSurfaceFactor(dto.getSurfaceFactor());
            dao.setSurfaceParameters(dto.getSurfaceParameters());
            dao.setTime(dto.getTime());
            dao.setVectorSimplifyFactor(dto.getVectorSimplifyFactor());
            dao.setType(dto.getFormat());
            dao.setBboxMinx(dto.getBboxMinX());
            dao.setBboxMaxx(dto.getBboxMaxX());
            dao.setBboxMiny(dto.getBboxMinY());
            dao.setBboxMaxy(dto.getBboxMaxY());
            return dao;
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateStatus(int id, String status) {
        dsl.update(Tables.SCENE)
           .set(Tables.SCENE.STATUS, status)
           .where(Tables.SCENE.ID.eq(id))
           .execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateBBox(int id, Double minx, Double maxx, Double miny, Double maxy) {
         dsl.update(Tables.SCENE)
           .set(Tables.SCENE.BBOX_MAXX, maxx)
           .set(Tables.SCENE.BBOX_MAXY, maxy)
           .set(Tables.SCENE.BBOX_MINX, minx)
           .set(Tables.SCENE.BBOX_MINY, miny)
           .where(Tables.SCENE.ID.eq(id))
           .execute();
    }

    @Override
    public void update(Scene scene) {
         dsl.update(Tables.SCENE)
                .set(Tables.SCENE.NAME, scene.getTitle())
                .set(Tables.SCENE.BBOX_MAXX, scene.getBboxMaxX())
                .set(Tables.SCENE.BBOX_MAXY, scene.getBboxMaxY())
                .set(Tables.SCENE.BBOX_MINX, scene.getBboxMinX())
                .set(Tables.SCENE.BBOX_MINY, scene.getBboxMinY())
                .set(Tables.SCENE.CREATION_DATE, scene.getCreationDate().getTime())
                .set(Tables.SCENE.DATA_ID, scene.getDataId())
                .set(Tables.SCENE.TYPE, scene.getFormat())
                .set(Tables.SCENE.EXTRAS, scene.getExtras())
                .set(Tables.SCENE.LAYER_ID, scene.getLayerId())
                .set(Tables.SCENE.MAP_CONTEXT_ID, scene.getMapContext())
                .set(Tables.SCENE.MAX_LOD, scene.getMaxLod())
                .set(Tables.SCENE.MIN_LOD, scene.getMinLod())
                .set(Tables.SCENE.STATUS, scene.getStatus())
                .set(Tables.SCENE.SURFACE, scene.getSurface())
                .set(Tables.SCENE.SURFACE_FACTOR, scene.getSurfaceFactor())
                .set(Tables.SCENE.SURFACE_PARAMETERS, scene.getSurfaceParameters())
                .set(Tables.SCENE.TIME, scene.getTime())
                .set(Tables.SCENE.VECTOR_SIMPLIFY_FACTOR, scene.getVectorSimplifyFactor())
                .where(Tables.SCENE.ID.eq(scene.getPublicationId()))
                .execute();
    }
}
