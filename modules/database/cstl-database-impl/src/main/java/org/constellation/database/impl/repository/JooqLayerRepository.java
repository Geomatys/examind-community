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
import static org.constellation.database.api.jooq.Tables.LAYER;
import static org.constellation.database.api.jooq.Tables.STYLED_LAYER;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.constellation.dto.Layer;
import org.constellation.database.api.jooq.tables.records.LayerRecord;
import org.constellation.repository.LayerRepository;
import org.jooq.Field;
import org.jooq.UpdateConditionStep;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqLayerRepository extends AbstractJooqRespository<LayerRecord, org.constellation.database.api.jooq.tables.pojos.Layer> implements LayerRepository {

    /**
     * Fields use to select a lighten Layer reference objects
     */
    private static final Field[] REF_FIELDS = new Field[]{
            LAYER.ID,
            LAYER.NAME};

    public JooqLayerRepository() {

        super(org.constellation.database.api.jooq.tables.pojos.Layer.class, LAYER);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteServiceLayer(Integer serviceId) {
        return dsl.delete(LAYER).where(LAYER.SERVICE.eq(serviceId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(Layer layer) {
        LayerRecord newRecord = dsl.newRecord(LAYER);
        newRecord.setOwner(layer.getOwnerId());
        newRecord.setAlias(layer.getAlias());
        newRecord.setConfig(layer.getConfig());
        newRecord.setData(layer.getDataId());
        newRecord.setName(layer.getName());
        newRecord.setNamespace(layer.getNamespace());
        newRecord.setService(layer.getService());
        newRecord.setDate(layer.getDate() != null ? layer.getDate().getTime() : null);
        if (newRecord.store() > 0)
            return newRecord.into(Layer.class).getId();

        return null;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Layer layer) {
        LayerRecord layerRecord = new LayerRecord();
        layerRecord.from(layer);
        UpdateConditionStep<LayerRecord> set = dsl.update(LAYER).set(LAYER.NAME, layer.getName())
                .set(LAYER.NAMESPACE, layer.getNamespace()).set(LAYER.ALIAS, layer.getAlias()).set(LAYER.DATA, layer.getDataId())
                .set(LAYER.CONFIG, layer.getConfig()).set(LAYER.TITLE, layer.getTitle()).where(LAYER.ID.eq(layer.getId()));
        set.execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateLayerTitle(int layerID, String newTitle) {
        dsl.update(LAYER).set(LAYER.TITLE, newTitle).where(LAYER.ID.eq(layerID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer layerId) {
        return dsl.delete(LAYER).where(LAYER.ID.eq(layerId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(LAYER).execute();
    }

    @Override
    public Layer findById(Integer layerId) {
        return convertIntoDto(dsl.select().from(LAYER).where(LAYER.ID.eq(layerId)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(LAYER)
                .where(LAYER.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<Layer> findByServiceId(int serviceId) {
        return convertListToDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    @Override
    public List<Integer> findIdByServiceId(int serviceId) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchInto(Integer.class);
    }

    @Override
    public List<QName> findNameByServiceId(int serviceId) {
        List<Map<String, Object>> test = dsl.select(LAYER.NAME, LAYER.NAMESPACE).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchMaps();
        List<QName> result = new ArrayList<>();
        for (Map<String, Object> t : test) {
            result.add(new QName((String) t.get("namespace"), (String) t.get("name")));
        }
        return result;
    }

    @Override
    public List<Integer> findByDataId(int dataId) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.DATA.eq(dataId)).fetchInto(Integer.class);
    }

    @Override
    public Integer findIdByServiceIdAndLayerName(int serviceId, String layerName) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).fetchOneInto(Integer.class);
    }

    @Override
    public Layer findByServiceIdAndLayerName(int serviceId, String layerName) {
        return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.ALIAS.isNull()).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    @Override
    public Integer findIdByServiceIdAndLayerName(int serviceId, String layerName, String namespace) {
        if (namespace != null) {
            return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.NAMESPACE.eq(namespace)).fetchOneInto(Integer.class);
        } else {
            return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).fetchOneInto(Integer.class);
        }
    }

    @Override
    public Layer findByServiceIdAndLayerName(int serviceId, String layerName, String namespace) {
        if (namespace != null) {
            return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName))
                    .and(LAYER.NAMESPACE.eq(namespace)).and(LAYER.ALIAS.isNull()).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
        } else {
            return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.NAMESPACE.isNull())
                    .and(LAYER.ALIAS.isNull()).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
        }
    }

    @Override
    public Layer findByServiceIdAndAlias(int serviceId, String alias) {
        return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.ALIAS.eq(alias))
                    .fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    @Override
    public Integer findIdByServiceIdAndAlias(int serviceId, String alias) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.ALIAS.eq(alias)).fetchOneInto(Integer.class);
    }

    @Override
    public Layer findByServiceIdAndDataId(int serviceId, int dataId) {
        return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.DATA.eq(dataId))
                .and(LAYER.ALIAS.isNull()).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));

    }

    @Override
    public List<Layer> getLayersByLinkedStyle(final int styleId) {
        return convertListToDto(dsl.select(LAYER.fields()).from(LAYER).join(STYLED_LAYER).onKey(STYLED_LAYER.LAYER).where(STYLED_LAYER.STYLE.eq(styleId))
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    @Override
    public List<Layer> getLayersRefsByLinkedStyle(final int styleId) {
        return convertListToDto(dsl.select(REF_FIELDS).from(LAYER).join(STYLED_LAYER).onKey(STYLED_LAYER.LAYER).where(STYLED_LAYER.STYLE.eq(styleId))
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    @Override
    public List<Layer> findAll() {
        return convertListToDto(dsl.select().from(LAYER).fetchInto(org.constellation.database.api.jooq.tables.pojos.Layer.class));
    }

    private static List<Layer> convertListToDto(List<org.constellation.database.api.jooq.tables.pojos.Layer> daos) {
        List<Layer> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Layer dao : daos) {
            results.add(convertIntoDto(dao));
        }
        return results;
    }

    private static Layer convertIntoDto(final org.constellation.database.api.jooq.tables.pojos.Layer dao) {
        if (dao != null) {
            final Layer dto = new Layer();
            dto.setAlias(dao.getAlias());
            dto.setConfig(dao.getConfig());
            dto.setDataId(dao.getData());
            dto.setDate(new Date(dao.getDate()));
            dto.setId(dao.getId());
            dto.setName(dao.getName());
            dto.setNamespace(dao.getNamespace());
            dto.setOwnerId(dao.getOwner());
            dto.setService(dao.getService());
            dto.setTitle(dao.getTitle());
            return dto;
        }
        return null;
    }
}
