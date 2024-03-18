/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
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

import com.examind.database.api.jooq.tables.records.StyleRecord;
import com.examind.database.api.jooq.tables.records.StyledDataRecord;
import com.examind.database.api.jooq.tables.records.StyledLayerRecord;
import org.constellation.dto.Style;
import org.constellation.dto.StyleReference;
import org.constellation.dto.StyledLayer;
import org.constellation.repository.StyleRepository;
import org.jooq.*;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.examind.database.api.jooq.Tables.*;
import org.constellation.exception.ConstellationPersistenceException;

@Component
@DependsOn("database-initer")
public class JooqStyleRepository extends AbstractJooqRespository<StyleRecord, com.examind.database.api.jooq.tables.pojos.Style> implements StyleRepository {

    private static final Field[] REFERENCE_FIELDS = new Field[]{
            STYLE.ID.as("id"),
            STYLE.NAME.as("name"),
            STYLE.PROVIDER.as("provider_id")};


    public JooqStyleRepository() {
        super(com.examind.database.api.jooq.tables.pojos.Style.class, STYLE);
    }

    @Override
    public List<Style> findByData(Integer dataId) {
        return convertStyleListToDto(dsl.select(STYLE.fields()).from(STYLE).join(STYLED_DATA).onKey()
                .where(STYLED_DATA.DATA.eq(dataId)).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByType(String type) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.TYPE.eq(type)).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByTypeAndProvider(final int providerId, String type) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.TYPE.eq(type)).and(STYLE.PROVIDER.eq(providerId)).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByProvider(final int providerId) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.PROVIDER.eq(providerId)).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByLayer(Integer layerId) {
        return convertStyleListToDto(
                dsl.select(STYLE.fields())
                        .from(STYLE)
                        .join(STYLED_LAYER).onKey()
                        .where(STYLED_LAYER.LAYER.eq(layerId))
                        .orderBy(STYLED_LAYER.IS_DEFAULT)
                        .fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class)
        );
    }

    @Override
    public Style findById(int id) {
        return convertToDto(dsl.select().from(STYLE).where(STYLE.ID.eq(id)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByName(String name) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.NAME.eq(name)).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public Style findByNameAndProvider(int providerId, String name) {
        return convertToDto(dsl.select().from(STYLE).where(STYLE.NAME.eq(name)).and(STYLE.PROVIDER.eq(providerId)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public Integer findIdByNameAndProvider(int providerId, String name) {
        return dsl.select(STYLE.ID).from(STYLE).where(STYLE.NAME.eq(name)).and(STYLE.PROVIDER.eq(providerId)).fetchOneInto(Integer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkStyleToData(int styleId, int dataid) {
        StyledDataRecord link = dsl.select().from(STYLED_DATA)
                .where(STYLED_DATA.DATA.eq(dataid).and(STYLED_DATA.STYLE.eq(styleId)))
                .fetchOneInto(StyledDataRecord.class);
        if (link == null) {
            dsl.insertInto(STYLED_DATA).set(STYLED_DATA.DATA, dataid).set(STYLED_DATA.STYLE, styleId).execute();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkStyleToData(int styleId, int dataid) {
        dsl.delete(STYLED_DATA).where(STYLED_DATA.DATA.eq(dataid).and(STYLED_DATA.STYLE.eq(styleId))).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkAllStylesFromData(int dataId) {
        dsl.delete(STYLED_DATA).where(STYLED_DATA.DATA.eq(dataId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkStyleToLayer(int styleId, int layerId) {
        StyledLayerRecord styledLayerRecord = dsl.select().from(STYLED_LAYER).where(STYLED_LAYER.LAYER.eq(layerId)).and(STYLED_LAYER.STYLE.eq(styleId))
                .fetchOneInto(StyledLayerRecord.class);
        if (styledLayerRecord == null) {

            InsertSetMoreStep<StyledLayerRecord> insert = dsl.insertInto(STYLED_LAYER).set(STYLED_LAYER.LAYER, layerId).set(STYLED_LAYER.STYLE, styleId);
            insert.execute();
            setDefaultStyleToLayer(styleId, layerId);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void setDefaultStyleToLayer(int styleId, int layerId) {
        StyledLayerRecord styledLayerRecord = dsl.select().from(STYLED_LAYER).where(STYLED_LAYER.LAYER.eq(layerId)).and(STYLED_LAYER.IS_DEFAULT.eq(true)).fetchOneInto(StyledLayerRecord.class);
        if (styledLayerRecord != null) {
            dsl.update(STYLED_LAYER).set(STYLED_LAYER.IS_DEFAULT, false).where(STYLED_LAYER.LAYER.eq(layerId)).execute();
        }
        UpdateConditionStep<StyledLayerRecord> update = dsl.update(STYLED_LAYER).set(STYLED_LAYER.IS_DEFAULT, true).where(STYLED_LAYER.LAYER.eq(layerId)).and(STYLED_LAYER.STYLE.eq(styleId));
        update.execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkStyleToLayer(int styleId, int layerid) {
        dsl.delete(STYLED_LAYER).where(STYLED_LAYER.LAYER.eq(layerid).and(STYLED_LAYER.STYLE.eq(styleId))).execute();

    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkAllStylesFromLayer(int layerId) {
        dsl.delete(STYLED_LAYER).where(STYLED_LAYER.LAYER.eq(layerId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer styleId) {
        return dsl.delete(STYLE).where(STYLE.ID.eq(styleId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(STYLE).execute();
    }

    @Override
    public List<Integer> getStyleIdsForData(int id) {
        return dsl.select(STYLE.ID).from(STYLE).join(STYLED_DATA).on(STYLED_DATA.STYLE.eq(STYLE.ID)).where(STYLED_DATA.DATA.eq(id)).fetch(STYLE.ID);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(Style style) {
        StyleRecord styleRecord = dsl.newRecord(STYLE);
        styleRecord.setBody(style.getBody());
        styleRecord.setDate(style.getDate() != null ? style.getDate().getTime() : null);
        styleRecord.setName(style.getName());
        styleRecord.setOwner(style.getOwnerId());
        styleRecord.setProvider(style.getProviderId());
        styleRecord.setType(style.getType());
        styleRecord.setSpecification(style.getSpecification());
        // default value
        if (style.getIsShared() == null) {
            style.setIsShared(false);
        }
        styleRecord.setIsShared(style.getIsShared());
        styleRecord.store();
        return styleRecord.getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Style s) {
        dsl.update(STYLE)
                .set(STYLE.DATE, s.getDate() != null ? s.getDate().getTime() : null)
                .set(STYLE.BODY, s.getBody())
                .set(STYLE.NAME, s.getName())
                .set(STYLE.PROVIDER, s.getProviderId())
                .set(STYLE.OWNER, s.getOwnerId())
                .set(STYLE.TYPE, s.getType())
                .set(STYLE.SPECIFICATION, s.getSpecification())
                .where(STYLE.ID.eq(s.getId())).execute();
    }

    @Override
    public boolean existsById(Integer styleId) {
        return dsl.selectCount().from(STYLE)
                .where(STYLE.ID.eq(styleId))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<StyleReference> fetchByDataId(int dataId) {
        final List<StyleReference> refs = dsl.select(REFERENCE_FIELDS).from(STYLE)
                .join(STYLED_DATA).on(STYLED_DATA.STYLE.eq(STYLE.ID)) // style -> styled_data
                .where(STYLED_DATA.DATA.eq(dataId))
                .fetchInto(StyleReference.class);

        for(StyleReference sr : refs){
            switch (sr.getProviderId()) {
                case 1 -> sr.setProviderIdentifier("sld");
                case 2 -> sr.setProviderIdentifier("sld_temp");
                default -> throw new IllegalArgumentException("Style provider with identifier \"" + sr.getProviderId() + "\" does not exist.");
            }
        }

        return refs;
    }

    @Override
    public List<StyleReference> fetchByLayerId(int layerId) {
        final List<StyleReference> refs =
                dsl.select(REFERENCE_FIELDS)
                        .from(STYLE)
                        .join(STYLED_LAYER).onKey()
                        .where(STYLED_LAYER.LAYER.eq(layerId))
                        .orderBy(STYLED_LAYER.IS_DEFAULT)
                        .fetchInto(StyleReference.class);

        for (StyleReference sr : refs) {
            switch (sr.getProviderId()) {
                case 1 -> sr.setProviderIdentifier("sld");
                case 2 -> sr.setProviderIdentifier("sld_temp");
                default -> throw new IllegalArgumentException("Style provider with identifier \"" + sr.getProviderId() + "\" does not exist.");
            }
        }
        return refs;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeSharedProperty(int id, boolean shared) {
        UpdateSetFirstStep<StyleRecord> update = dsl.update(STYLE);
        update.set(STYLE.IS_SHARED, shared).where(STYLE.ID.eq(id)).execute();
    }

    @Override
    public StyledLayer getStyledLayer(int styleId, int layerId) {
       return dsl.select(STYLED_LAYER).from(STYLED_LAYER).where(STYLED_LAYER.LAYER.eq(layerId)).and(STYLED_LAYER.STYLE.eq(styleId))
                .fetchOneInto(StyledLayer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addExtraInfo(int styleId, int layerId, String extraInfo) {
        UpdateConditionStep<StyledLayerRecord> update = dsl.update(STYLED_LAYER).set(STYLED_LAYER.EXTRA_INFO, extraInfo).where(STYLED_LAYER.LAYER.eq(layerId)).and(STYLED_LAYER.STYLE.eq(styleId));
        update.execute();
    }

    /**
     * Returns a singleton map that contains the total count of records as key,
     * and the list of records as value.
     * the list is resulted by filters, it use pagination and sorting.

     * @param filterMap given filters
     * @param sortEntry given sort, can be null
     * @param pageNumber pagination page
     * @param rowsPerPage count of rows per page
     * @return Map
     */
    @Override
    public Map.Entry<Integer, List<Style>> filterAndGet(final Map<String,Object> filterMap,
                                       final Map.Entry<String,String> sortEntry,
                                       final int pageNumber,
                                       final int rowsPerPage) {
        Collection<Field<?>> fields = new ArrayList<>();
        Collections.addAll(fields,STYLE.fields());

        SelectJoinStep baseQuery   = dsl.select(fields).from(STYLE).leftOuterJoin(CSTL_USER).on(STYLE.OWNER.eq(CSTL_USER.ID));
        SelectConnectByStep fquery = buildQuery(baseQuery, filterMap);

        Select query;
        if (sortEntry != null) {
            SortField f;
            if ("title".equals(sortEntry.getKey()) || "name".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? STYLE.NAME.asc() : STYLE.NAME.desc();
            } else if ("owner".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? CSTL_USER.LOGIN.asc() : CSTL_USER.LOGIN.desc();
            } else {  //default sorting on date stamp
                f = "ASC".equals(sortEntry.getValue()) ? STYLE.DATE.asc() : STYLE.DATE.desc();
            }
            query = fquery.orderBy(f);
        } else {
            query = fquery;
        }

        final int count = dsl.fetchCount(query);
        final Map.Entry<Integer,List<Style>> result = new AbstractMap.SimpleImmutableEntry<>(count,
                convertStyleListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class)));
        return result;
    }

    @Override
    protected Condition buildSpecificCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return STYLE.OWNER.equal(castOrThrow(key, value, Integer.class));
        } else if ("term".equals(key)) {
            return STYLE.NAME.likeIgnoreCase("%" +  castOrThrow(key, value, String.class) + "%");
        } else if ("isShared".equals(key)) {
            return STYLE.IS_SHARED.eq(castOrThrow(key, value, Boolean.class));
        } else if ("period".equals(key)) {
            return STYLE.DATE.greaterOrEqual(castOrThrow(key, value, Long.class));
        } else if ("type".equals(key)) {
            return STYLE.TYPE.eq( castOrThrow(key, value, String.class));
        } else if ("provider".equalsIgnoreCase(key)) {
            return STYLE.PROVIDER.equal(castOrThrow(key, value, Integer.class));
        }
        throw new ConstellationPersistenceException(key + " parameter is not supported.");
    }

    @Override
    public List<Style> findAll() {
        return convertStyleListToDto(dsl.select().from(STYLE).fetchInto(com.examind.database.api.jooq.tables.pojos.Style.class));
    }

    public static List<Style> convertStyleListToDto(List<com.examind.database.api.jooq.tables.pojos.Style> daos) {
        List<Style> dtos = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.Style dao : daos) {
            dtos.add(convertToDto(dao));
        }
        return dtos;
    }

    public static Style convertToDto(com.examind.database.api.jooq.tables.pojos.Style dao) {
        if (dao != null) {
            Style p = new Style();
            p.setBody(dao.getBody());
            p.setId(dao.getId());
            p.setDate(new Date(dao.getDate()));
            p.setIsShared(dao.getIsShared());
            p.setOwnerId(dao.getOwner());
            p.setName(dao.getName());
            p.setType(dao.getType());
            p.setProviderId(dao.getProvider());
            p.setSpecification(dao.getSpecification());
            return p;
        }
        return null;
    }
}
