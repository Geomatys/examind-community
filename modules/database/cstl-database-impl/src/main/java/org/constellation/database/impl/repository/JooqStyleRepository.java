package org.constellation.database.impl.repository;

import org.constellation.database.api.jooq.tables.records.StyleRecord;
import org.constellation.database.api.jooq.tables.records.StyledDataRecord;
import org.constellation.database.api.jooq.tables.records.StyledLayerRecord;
import org.constellation.dto.Style;
import org.constellation.dto.StyleReference;
import org.constellation.repository.StyleRepository;
import org.jooq.*;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.Map.Entry;

import static org.constellation.database.api.jooq.Tables.*;

@Component
@DependsOn("database-initer")
public class JooqStyleRepository extends AbstractJooqRespository<StyleRecord, org.constellation.database.api.jooq.tables.pojos.Style> implements StyleRepository {

    private static final Field[] REFERENCE_FIELDS = new Field[]{
            STYLE.ID.as("id"),
            STYLE.NAME.as("name"),
            STYLE.PROVIDER.as("provider_id")};


    public JooqStyleRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.Style.class, STYLE);
    }

    @Override
    public List<Style> findByData(Integer dataId) {
        return convertStyleListToDto(dsl.select(STYLE.fields()).from(STYLE).join(STYLED_DATA).onKey()
                .where(STYLED_DATA.DATA.eq(dataId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByType(String type) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.TYPE.eq(type)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByTypeAndProvider(final int providerId, String type) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.TYPE.eq(type)).and(STYLE.PROVIDER.eq(providerId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByProvider(final int providerId) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.PROVIDER.eq(providerId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByLayer(Integer layerId) {
        return convertStyleListToDto(
                dsl.select(STYLE.fields())
                        .from(STYLE)
                        .join(STYLED_LAYER).onKey()
                        .where(STYLED_LAYER.LAYER.eq(layerId))
                        .orderBy(STYLED_LAYER.IS_DEFAULT)
                        .fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class)
        );
    }

    @Override
    public Style findById(int id) {
        return convertToDto(dsl.select().from(STYLE).where(STYLE.ID.eq(id)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public List<Style> findByName(String name) {
        return convertStyleListToDto(dsl.select().from(STYLE).where(STYLE.NAME.eq(name)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    @Override
    public Style findByNameAndProvider(int providerId, String name) {
        return convertToDto(dsl.select().from(STYLE).where(STYLE.NAME.eq(name)).and(STYLE.PROVIDER.eq(providerId)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
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
    public void delete(int styleId) {
        dsl.delete(STYLE).where(STYLE.ID.eq(styleId)).execute();

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
                .where(STYLE.ID.eq(s.getId())).execute();
    }

    @Override
    public boolean existsById(int styleId) {
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
            switch(sr.getProviderId()){
                case 1 : sr.setProviderIdentifier("sld"); break;
                case 2 : sr.setProviderIdentifier("sld_temp"); break;
                default : throw new IllegalArgumentException("Style provider with identifier \"" + sr.getProviderId() + "\" does not exist.");
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

        for(StyleReference sr : refs){
            switch(sr.getProviderId()){
                case 1 : sr.setProviderIdentifier("sld"); break;
                case 2 : sr.setProviderIdentifier("sld_temp"); break;
                default : throw new IllegalArgumentException("Style provider with identifier \"" + sr.getProviderId() + "\" does not exist.");
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
        Select query = null;
        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition cond = buildCondition(entry.getKey(), entry.getValue());
                if (cond != null) {
                    if (query == null) {
                        query = dsl.select(fields).from(STYLE).leftOuterJoin(CSTL_USER).on(STYLE.OWNER.eq(CSTL_USER.ID)).where(cond);
                    } else {
                        query = ((SelectConditionStep) query).and(cond);
                    }
                }
            }
        }
        if(sortEntry != null) {
            SortField f = null;
            if ("title".equals(sortEntry.getKey()) || "name".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? STYLE.NAME.asc() : STYLE.NAME.desc();
            } else if ("date".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? STYLE.DATE.asc() : STYLE.DATE.desc();
            } else if ("owner".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? CSTL_USER.LOGIN.asc() : CSTL_USER.LOGIN.desc();
            }
            if (f != null) {
                if (query == null) {
                    query = dsl.select(fields).from(STYLE).leftOuterJoin(CSTL_USER).on(STYLE.OWNER.eq(CSTL_USER.ID)).orderBy(f);
                } else {
                    query = ((SelectConditionStep) query).orderBy(f);
                }
            }
        }

        final Map.Entry<Integer,List<Style>> result;
        if(query == null) { //means there are no sorting and no filters
            final int count = dsl.selectCount().from(STYLE).fetchOne(0,int.class);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertStyleListToDto(dsl.select(fields).from(STYLE).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class)));
        }else {
            final int count = dsl.fetchCount(query);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertStyleListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class)));
        }
        return result;
    }

    private Condition buildCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return STYLE.OWNER.equal((Integer) value);
        } else if ("term".equals(key)) {
            return STYLE.NAME.likeIgnoreCase("%" + value + "%");
        } else if ("isShared".equals(key)) {
            return STYLE.IS_SHARED.eq((Boolean)value);
        } else if ("period".equals(key)) {
            return STYLE.DATE.greaterOrEqual((Long) value);
        } else if ("type".equals(key)) {
            return STYLE.TYPE.eq((String) value);
        } else if ("provider".equalsIgnoreCase(key)) {
            return STYLE.PROVIDER.equal((Integer) value);
        } else if ("OR".equals(key)) {
            List<Entry<String, Object>> values =  (List<Entry<String, Object>>) value;
            Condition c = null;
            for (Entry<String, Object> e: values) {
                Condition c2 = buildCondition(e.getKey(), e.getValue());
                if (c == null) {
                    c = c2;
                } else {
                    c = c.or(c2);
                }
            }
            return c;
        } else if ("AND".equals(key)) {
            List<Entry<String, Object>> values =  (List<Entry<String, Object>>) value;
            Condition c = null;
            for (Entry<String, Object> e: values) {
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

    @Override
    public List<Style> findAll() {
        return convertStyleListToDto(dsl.select().from(STYLE).fetchInto(org.constellation.database.api.jooq.tables.pojos.Style.class));
    }

    public static List<Style> convertStyleListToDto(List<org.constellation.database.api.jooq.tables.pojos.Style> daos) {
        List<Style> dtos = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Style dao : daos) {
            dtos.add(convertToDto(dao));
        }
        return dtos;
    }

    public static Style convertToDto(org.constellation.database.api.jooq.tables.pojos.Style dao) {
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
            return p;
        }
        return null;
    }
}
