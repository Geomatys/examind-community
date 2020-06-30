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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import static org.constellation.database.api.jooq.Tables.LAYER;
import static org.constellation.database.api.jooq.Tables.STYLED_LAYER;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.constellation.dto.Layer;
import org.constellation.database.api.jooq.tables.records.LayerRecord;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.LayerRepository;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
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
        newRecord.setTitle(layer.getTitle());
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

    @Override
    public Map.Entry<Integer, List<Layer>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {

        //add default filter
        if (filterMap == null) {
           filterMap = new HashMap<>();
        }
        // build SQL query
        Select query = null;
        for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
            final Condition cond = buildCondition(entry.getKey(), entry.getValue());
            if (cond != null) {
                if (query == null) {
                    query = dsl.select(LAYER.fields()).from(LAYER).where(cond);
                } else {
                    query = ((SelectConditionStep) query).and(cond);
                }
            }
        }

        // add sort
        if(sortEntry != null) {
            final SortField f;
            if("title".equals(sortEntry.getKey())){
                f = "ASC".equals(sortEntry.getValue()) ? LAYER.TITLE.lower().asc() : LAYER.TITLE.lower().desc();
            } else {
                f = "ASC".equals(sortEntry.getValue()) ? LAYER.DATE.asc() : LAYER.DATE.desc();
            }
            if (query == null) {
                query = dsl.select(LAYER.fields()).from(LAYER).orderBy(f);
            } else {
                query = ((SelectConditionStep)query).orderBy(f);
            }
        }

        final Map.Entry<Integer,List<Layer>> result;
        if (query == null) {
            final int count = dsl.selectCount().from(LAYER).fetchOne(0,int.class);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertListToDto(dsl.select(LAYER.fields()).from(LAYER).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(org.constellation.database.api.jooq.tables.pojos.Layer.class)));
        } else {
            final int count = dsl.fetchCount(query);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(org.constellation.database.api.jooq.tables.pojos.Layer.class)));
        }
        return result;
    }

    private Condition buildCondition(String key, Object value) {
        if ("owner".equals(key)) {
            if (value instanceof Integer) {
                return LAYER.OWNER.equal((Integer) value);
            }
            throw new ConstellationPersistenceException("Owner parameter must be an Integer");
        } else if ("alias".equals(key)) {
            if (value instanceof String) {
                return LAYER.ALIAS.equal((String) value);
            }
            throw new ConstellationPersistenceException("Alias parameter must be an String");
        } else if ("data".equals(key)) {
            if (value instanceof Integer) {
                return LAYER.DATA.equal((Integer) value);
            }
            throw new ConstellationPersistenceException("Data parameter must be an Integer");
        } else if ("title".equals(key)) {
            return LAYER.TITLE.likeIgnoreCase("%" + value + "%");
        } else if ("service".equals(key)) {
            if (value instanceof Integer) {
                return LAYER.SERVICE.equal((Integer) value);
            }
            throw new ConstellationPersistenceException("Service parameter must be an Integer");
        } else if ("id".equals(key)) {
            if (value instanceof Integer) {
                return LAYER.ID.equal((Integer) value);
            }
            throw new ConstellationPersistenceException("Id parameter must be an Integer");
        } else if ("term".equals(key)) {
            return LAYER.NAME.likeIgnoreCase("%" + value + "%");
        } else if ("period".equals(key)) {
            if (value instanceof Long) {
                return LAYER.DATE.greaterOrEqual((Long) value);
            }
            throw new ConstellationPersistenceException("period parameter must be a Long");
        } else if ("OR".equals(key)) {
            List<Map.Entry<String, Object>> values =  (List<Map.Entry<String, Object>>) value;
            Condition c = null;
            for (Map.Entry<String, Object> e: values) {
                Condition c2 = buildCondition(e.getKey(), e.getValue());
                if (c == null) {
                    c = c2;
                } else {
                    c = c.or(c2);
                }
            }
            return c;
        } else if ("AND".equals(key)) {
            List<Map.Entry<String, Object>> values =  (List<Map.Entry<String, Object>>) value;
            Condition c = null;
            for (Map.Entry<String, Object> e: values) {
                Condition c2 = buildCondition(e.getKey(), e.getValue());
                if (c == null) {
                    c = c2;
                } else {
                    c = c.and(c2);
                }
            }
        }
        return null;
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
