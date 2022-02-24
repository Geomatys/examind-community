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
import static com.examind.database.api.jooq.Tables.LAYER;
import static com.examind.database.api.jooq.Tables.STYLED_LAYER;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.constellation.dto.Layer;
import com.examind.database.api.jooq.tables.records.LayerRecord;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.LayerRepository;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
import org.jooq.UpdateConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqLayerRepository extends AbstractJooqRespository<LayerRecord, com.examind.database.api.jooq.tables.pojos.Layer> implements LayerRepository {

    /**
     * Fields use to select a lighten Layer reference objects
     */
    private static final Field[] REF_FIELDS = new Field[]{
            LAYER.ID,
            LAYER.NAME};

    public JooqLayerRepository() {

        super(com.examind.database.api.jooq.tables.pojos.Layer.class, LAYER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteServiceLayer(Integer serviceId) {
        return dsl.delete(LAYER).where(LAYER.SERVICE.eq(serviceId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(Layer layer) {
        LayerRecord newRecord = dsl.newRecord(LAYER);
        newRecord.setOwner(layer.getOwnerId());
        newRecord.setAlias(layer.getAlias());
        newRecord.setConfig(layer.getConfig());
        newRecord.setData(layer.getDataId());
        newRecord.setName(layer.getName().getLocalPart());
        // QNAME namespace is never null
        if (!layer.getName().getNamespaceURI().isEmpty()) {
            newRecord.setNamespace(layer.getName().getNamespaceURI());
        }
        newRecord.setService(layer.getService());
        newRecord.setTitle(layer.getTitle());
        newRecord.setDate(layer.getDate() != null ? layer.getDate().getTime() : null);
        if (newRecord.store() > 0)
            return newRecord.getId();

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Layer layer) {
        LayerRecord layerRecord = new LayerRecord();
        layerRecord.from(layer);
        UpdateConditionStep<LayerRecord> set = dsl.update(LAYER).set(LAYER.NAME, layer.getName().getLocalPart())
                .set(LAYER.NAMESPACE, layer.getName().getNamespaceURI()).set(LAYER.ALIAS, layer.getAlias()).set(LAYER.DATA, layer.getDataId())
                .set(LAYER.CONFIG, layer.getConfig()).set(LAYER.TITLE, layer.getTitle()).where(LAYER.ID.eq(layer.getId()));
        set.execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer layerId) {
        return dsl.delete(LAYER).where(LAYER.ID.eq(layerId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(LAYER).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Layer findById(Integer layerId) {
        return convertIntoDto(dsl.select().from(LAYER).where(LAYER.ID.eq(layerId)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(LAYER)
                .where(LAYER.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Layer> findByServiceId(int serviceId) {
        return convertListToDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countByServiceId(Integer serviceId) {
        if (serviceId == null) return 0;
        return dsl.selectCount().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchOne(0, Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> findIdByServiceId(int serviceId) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchInto(Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<QName> findNameByServiceId(int serviceId) {
        List<Map<String, Object>> test = dsl.select(LAYER.NAME, LAYER.NAMESPACE).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).fetchMaps();
        List<QName> result = new ArrayList<>();
        for (Map<String, Object> t : test) {
            result.add(new QName((String) t.get("namespace"), (String) t.get("name")));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> findByDataId(int dataId) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.DATA.eq(dataId)).fetchInto(Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer findIdByServiceIdAndLayerName(int serviceId, String layerName, boolean noNamespace) {
        SelectConditionStep<Record1<Integer>> sel = dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.ALIAS.isNull());
        if (noNamespace) {
            sel = sel.and(LAYER.NAMESPACE.isNull());
        }
        return sel.fetchOneInto(Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Layer findByServiceIdAndLayerName(int serviceId, String layerName, boolean noNamespace) {
        SelectConditionStep<Record> sel = dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.ALIAS.isNull());
        if (noNamespace) {
            sel = sel.and(LAYER.NAMESPACE.isNull());
        }
        return convertIntoDto(sel.fetchOneInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer findIdByServiceIdAndLayerName(int serviceId, String layerName, String namespace) {
        if (namespace != null) {
            return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.NAMESPACE.eq(namespace)).fetchOneInto(Integer.class);
        } else {
            return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).fetchOneInto(Integer.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Layer findByServiceIdAndLayerName(int serviceId, String layerName, String namespace) {
        if (namespace != null) {
            return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName))
                    .and(LAYER.NAMESPACE.eq(namespace)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
        } else {
            return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.NAME.eq(layerName)).and(LAYER.NAMESPACE.isNull())
                    .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Layer findByServiceIdAndAlias(int serviceId, String alias) {
        return convertIntoDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.ALIAS.eq(alias))
                    .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer findIdByServiceIdAndAlias(int serviceId, String alias) {
        return dsl.select(LAYER.ID).from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.ALIAS.eq(alias)).fetchOneInto(Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Layer> findByServiceIdAndDataId(int serviceId, int dataId) {
        return convertListToDto(dsl.select().from(LAYER).where(LAYER.SERVICE.eq(serviceId)).and(LAYER.DATA.eq(dataId))
                .and(LAYER.ALIAS.isNull()).fetchInto(com.examind.database.api.jooq.tables.pojos.Layer.class));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Layer> getLayersByLinkedStyle(final int styleId) {
        return convertListToDto(dsl.select(LAYER.fields()).from(LAYER).join(STYLED_LAYER).onKey(STYLED_LAYER.LAYER).where(STYLED_LAYER.STYLE.eq(styleId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Layer> findAll() {
        return convertListToDto(dsl.select().from(LAYER).fetchInto(com.examind.database.api.jooq.tables.pojos.Layer.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map.Entry<Integer, List<Layer>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        
        // build SQL query
        SelectJoinStep baseQuery = dsl.select(LAYER.fields()).from(LAYER);
        SelectConnectByStep fquery = buildQuery(baseQuery, filterMap);

        // add sort
        Select query;
        if (sortEntry != null) {
            final SortField f;
            if("title".equals(sortEntry.getKey())){
                f = "ASC".equals(sortEntry.getValue()) ? DSL.lower(LAYER.TITLE).asc() : DSL.lower(LAYER.TITLE).desc();
            } else {
                f = "ASC".equals(sortEntry.getValue()) ? LAYER.DATE.asc() : LAYER.DATE.desc();
            }
            query = fquery.orderBy(f);
        } else {
            query = fquery;
        }
        final int count = dsl.fetchCount(query);
        final Map.Entry<Integer,List<Layer>> result = new AbstractMap.SimpleImmutableEntry<>(count,
                convertListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(com.examind.database.api.jooq.tables.pojos.Layer.class)));
        return result;
    }

    @Override
    protected Condition buildSpecificCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return LAYER.OWNER.equal(castOrThrow(key, value, Integer.class));
        } else if ("alias".equals(key)) {
            return LAYER.ALIAS.equal(castOrThrow(key, value, String.class));
        } else if ("data".equals(key)) {
            return LAYER.DATA.equal(castOrThrow(key, value, Integer.class));
        } else if ("title".equals(key)) {
            return LAYER.TITLE.likeIgnoreCase("%" + castOrThrow(key, value, String.class) + "%");
        } else if ("service".equals(key)) {
            return LAYER.SERVICE.equal(castOrThrow(key, value, Integer.class));
        } else if ("id".equals(key)) {
            return LAYER.ID.equal(castOrThrow(key, value, Integer.class));
        } else if ("term".equals(key)) {
            return LAYER.NAME.likeIgnoreCase("%" + castOrThrow(key, value, String.class) + "%");
        } else if ("period".equals(key)) {
            return LAYER.DATE.greaterOrEqual(castOrThrow(key, value, Long.class));
        }
        throw new ConstellationPersistenceException(key + " parameter is not supported.");
    }

    private static List<Layer> convertListToDto(List<com.examind.database.api.jooq.tables.pojos.Layer> daos) {
        List<Layer> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.Layer dao : daos) {
            results.add(convertIntoDto(dao));
        }
        return results;
    }

    private static Layer convertIntoDto(final com.examind.database.api.jooq.tables.pojos.Layer dao) {
        if (dao != null) {
            final Layer dto = new Layer();
            dto.setAlias(dao.getAlias());
            dto.setConfig(dao.getConfig());
            dto.setDataId(dao.getData());
            dto.setDate(new Date(dao.getDate()));
            dto.setId(dao.getId());
            dto.setName(new QName(dao.getNamespace(), dao.getName()));
            dto.setOwnerId(dao.getOwner());
            dto.setService(dao.getService());
            dto.setTitle(dao.getTitle());
            return dto;
        }
        return null;
    }
}
