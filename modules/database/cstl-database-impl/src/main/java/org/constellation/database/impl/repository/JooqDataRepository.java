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
import org.constellation.database.api.jooq.Tables;
import org.constellation.dto.Data;
import org.constellation.database.api.jooq.tables.pojos.DataXData;
import org.constellation.database.api.jooq.tables.pojos.Metadata;
import org.constellation.database.api.jooq.tables.pojos.MetadataXCsw;
import org.constellation.database.api.jooq.tables.records.DataRecord;
import org.constellation.database.api.jooq.tables.records.DataXDataRecord;
import org.constellation.database.api.jooq.tables.records.MetadataXCswRecord;
import org.constellation.repository.DataRepository;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.constellation.database.api.jooq.Tables.DATA;
import static org.constellation.database.api.jooq.Tables.DATA_X_DATA;
import static org.constellation.database.api.jooq.Tables.LAYER;
import static org.constellation.database.api.jooq.Tables.METADATA;
import static org.constellation.database.api.jooq.Tables.METADATA_X_CSW;
import static org.constellation.database.api.jooq.Tables.PROVIDER;
import static org.constellation.database.api.jooq.Tables.SENSORED_DATA;
import static org.constellation.database.api.jooq.Tables.SERVICE;
import static org.constellation.database.api.jooq.Tables.STYLED_DATA;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqDataRepository extends AbstractJooqRespository<DataRecord, org.constellation.database.api.jooq.tables.pojos.Data> implements DataRepository {

    /**
     * Field list use to return a lighten reference to Data object
     */
    public static final Field[] REF_FIELDS = new Field[]{
            DATA.ID,
            DATA.NAMESPACE,
            DATA.NAME,
            DATA.TYPE,
            DATA.SUBTYPE,
            DATA.PROVIDER};

    public JooqDataRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.Data.class, DATA);
    }

    @Override
    public Data findById(int id) {
        return convertDataIntoDto(dsl.select().from(DATA).where(DATA.ID.eq(id)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(Data data) {
        DataRecord newRecord = dsl.newRecord(DATA);
        newRecord.from(convertIntoDao(data));
        newRecord.store();
        return newRecord.getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(int id) {
        return dsl.delete(DATA).where(DATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(String namespaceURI, String localPart, int providerId) {
        Condition whereClause = buildWhereClause(namespaceURI, localPart, providerId);
        return dsl.delete(DATA).where(whereClause).execute();

    }

    private Condition buildWhereClause(String namespaceURI, String localPart, int providerId) {
        Condition whereClause = DATA.NAME.eq(localPart).and(DATA.PROVIDER.eq(providerId));
        if (namespaceURI != null) {
            return whereClause.and(DATA.NAMESPACE.eq(namespaceURI));
        }
        return whereClause;
    }

    private Condition buildWhereClause(String namespaceURI, String localPart, String providerId) {
        Condition whereClause = Tables.PROVIDER.IDENTIFIER.eq(providerId).and(DATA.NAME.eq(localPart));
        if (namespaceURI != null && ! namespaceURI.isEmpty()) {
            return whereClause.and(DATA.NAMESPACE.eq(namespaceURI));
        }
        return whereClause;
    }

    @Override
    public Data findDataFromProvider(String namespaceURI, String localPart, String providerId) {
        final Condition whereClause = buildWhereClause(namespaceURI, localPart, providerId);
        return convertDataIntoDto(dsl.select(DATA.fields())
                                 .from(DATA).join(Tables.PROVIDER).onKey()
                                 .where(whereClause)
                                 .fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public Integer findIdFromProvider(String namespaceURI, String localPart, String providerId) {
        final Condition whereClause = buildWhereClause(namespaceURI, localPart, providerId);
        return dsl.select(DATA.ID).from(DATA).join(Tables.PROVIDER).onKey().where(whereClause).fetchOneInto(Integer.class);
    }

    @Override
    public Data findByMetadataId(String metadataId) {
        return convertDataIntoDto(dsl.select(DATA.fields())
                                             .from(DATA).join(METADATA).onKey(METADATA.DATA_ID)
                                             .where(METADATA.METADATA_ID.eq(metadataId))
                                             .fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findByProviderId(Integer id) {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.PROVIDER.eq(id))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }


    @Override
    public List<Data> findByProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        SelectConditionStep c = dsl.select(DATA.fields()).from(DATA)
                .where(DATA.PROVIDER.eq(id)).and(DATA.INCLUDED.eq(included)).and(DATA.HIDDEN.eq(hidden));

        if (dataType != null) {
            c = c.and(DATA.TYPE.eq(dataType));
        }
        return convertDataListToDto(c.fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Integer> findIdsByProviderId(Integer id) {
        return dsl.select(DATA.ID).from(DATA)
                .where(DATA.PROVIDER.eq(id)).fetchInto(Integer.class);
    }

    @Override
    public List<Integer> findIdsByProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        SelectConditionStep c = dsl.select(DATA.ID).from(DATA)
                .where(DATA.PROVIDER.eq(id)).and(DATA.INCLUDED.eq(included)).and(DATA.HIDDEN.eq(hidden));

        if (dataType != null) {
            c = c.and(DATA.TYPE.eq(dataType));
        }
        return c.fetchInto(Integer.class);
    }

    @Override
    public List<Data> findByDatasetId(Integer id) {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.DATASET_ID.eq(id))
                                   .and(DATA.INCLUDED.eq(Boolean.TRUE))
                                   .and(DATA.HIDDEN.isNull().or(DATA.HIDDEN.isFalse()))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findByDatasetId(Integer id, boolean included, boolean hidden) {
        return convertDataListToDto(dsl.select().from(DATA)
                                   .where(DATA.DATASET_ID.eq(id))
                                   .and(DATA.INCLUDED.eq(included))
                                   .and(DATA.HIDDEN.eq(hidden))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findAllByDatasetId(Integer id) {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.DATASET_ID.eq(id))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public Data findByNameAndNamespaceAndProviderId(String localPart, String namespaceURI, Integer providerId) {
        return convertDataIntoDto(dsl.select()
                                 .from(DATA)
                                 .where(DATA.PROVIDER.eq(providerId))
                                 .and(DATA.NAME.eq(localPart))
                                 .and(DATA.NAMESPACE.eq(namespaceURI))
                                 .fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findByServiceId(Integer id) {
        return convertDataListToDto(dsl.select().from(DATA).join(LAYER).on(LAYER.DATA.eq(DATA.ID)).join(SERVICE).on(LAYER.SERVICE.eq(SERVICE.ID)).where(SERVICE.ID.eq(id))
                .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateStatistics(int dataId, String statsResult, String statsState) {

        dsl.update(DATA)
                .set(DATA.STATS_RESULT, statsResult)
                .set(DATA.STATS_STATE, statsState)
                .where(DATA.ID.eq(dataId))
                .execute();

    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(Data data) {

        dsl.update(DATA)
                .set(DATA.DATE, data.getDate() != null ? data.getDate().getTime() : null)
                .set(DATA.METADATA, data.getMetadata())
                .set(DATA.NAME, data.getName())
                .set(DATA.NAMESPACE, data.getNamespace())
                .set(DATA.OWNER, data.getOwnerId())
                .set(DATA.PROVIDER, data.getProviderId())
                .set(DATA.SENSORABLE, data.getSensorable())
                .set(DATA.SUBTYPE, data.getSubtype())
                .set(DATA.TYPE, data.getType())
                .set(DATA.INCLUDED, data.getIncluded())
                .set(DATA.DATASET_ID, data.getDatasetId())
                .set(DATA.STATS_RESULT, data.getStatsResult())
                .set(DATA.STATS_STATE, data.getStatsState())
                .set(DATA.RENDERED, data.getRendered())
                .set(DATA.HIDDEN, data.getHidden())
                .where(DATA.ID.eq(data.getId()))
                .execute();

    }


    @Override
    public Data findByIdentifierWithEmptyMetadata(String localPart) {
        List<org.constellation.database.api.jooq.tables.pojos.Data> datas = dsl.select()
                                                                               .from(DATA)
                                                                               .where(DATA.NAME.eq(localPart))
                                                                               .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class);
        for (org.constellation.database.api.jooq.tables.pojos.Data data : datas) {
            List<Metadata> m = dsl.select().from(METADATA).where(METADATA.DATA_ID.eq(data.getId())).fetchInto(Metadata.class);
            if (m.isEmpty()) {
                return convertDataIntoDto(data);
            }
        }
        return null;
    }

    @Override
    public List<Data> getCswLinkedData(final int cswId) {
        return convertDataListToDto(dsl.select(DATA.fields())
                                   .from(DATA, METADATA, METADATA_X_CSW)
                                   .where(METADATA.DATA_ID.eq(DATA.ID))
                                   .and(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                                   .and(METADATA_X_CSW.CSW_ID.eq(cswId))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addDataToCSW(final int serviceID, final int dataID) {
        final List<Metadata> metadatas = dsl.select().from(METADATA).where(METADATA.DATA_ID.eq(dataID)).fetchInto(Metadata.class);
        for (Metadata metadata : metadatas) {
            final MetadataXCsw dxc = dsl.select().from(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).and(METADATA_X_CSW.METADATA_ID.eq(metadata.getId())).fetchOneInto(MetadataXCsw.class);
            if (dxc == null) {
                MetadataXCswRecord newRecord = dsl.newRecord(METADATA_X_CSW);
                newRecord.setCswId(serviceID);
                newRecord.setMetadataId(metadata.getId());
                newRecord.store();
                newRecord.into(MetadataXCsw.class);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeDataFromCSW(int serviceID, int dataID) {
        final List<Metadata> metadatas = dsl.select().from(METADATA).where(METADATA.DATA_ID.eq(dataID)).fetchInto(Metadata.class);
        for (Metadata metadata : metadatas) {
            dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).and(METADATA_X_CSW.METADATA_ID.eq(metadata.getId())).execute();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeDataFromAllCSW(int dataID) {
        final List<Metadata> metadatas = dsl.select().from(METADATA).where(METADATA.DATA_ID.eq(dataID)).fetchInto(Metadata.class);
        for (Metadata metadata : metadatas) {
            dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.METADATA_ID.eq(metadata.getId())).execute();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeAllDataFromCSW(int serviceID) {
        dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).execute();
    }

    @Override
    public void linkDataToData(final int dataId, final int childId) {
        final DataXData dxd = dsl.select().from(DATA_X_DATA).where(DATA_X_DATA.DATA_ID.eq(dataId)).and(DATA_X_DATA.CHILD_ID.eq(childId)).fetchOneInto(DataXData.class);
        if (dxd == null) {
            DataXDataRecord newRecord = dsl.newRecord(DATA_X_DATA);
            newRecord.setDataId(dataId);
            newRecord.setChildId(childId);
            newRecord.store();
        }
    }

    @Override
    public List<Data> getDataLinkedData(final int dataId) {
        return convertDataListToDto(dsl.select(DATA.fields())
                                   .from(DATA)
                                   .join(DATA_X_DATA).onKey(DATA_X_DATA.CHILD_ID)
                                   .where(DATA_X_DATA.DATA_ID.eq(dataId))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLinkedData(int dataId) {
        dsl.delete(DATA_X_DATA).where(DATA_X_DATA.DATA_ID.eq(dataId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Data> getFullDataByLinkedStyle(final int styleId) {
        return convertDataListToDto(dsl.select(DATA.fields())
                                   .from(DATA)
                                   .join(STYLED_DATA).onKey(STYLED_DATA.DATA)
                                   .where(STYLED_DATA.STYLE.eq(styleId))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Data> getRefDataByLinkedStyle(final int styleId) {
        return convertDataListToDto(dsl.select()//dsl.select(REF_FIELDS)
                                   .from(DATA)
                                   .join(STYLED_DATA).onKey(STYLED_DATA.DATA)
                                   .where(STYLED_DATA.STYLE.eq(styleId))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findStatisticLess() {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.TYPE.eq("COVERAGE"))
                                   .and(DATA.RENDERED.isNull().or(DATA.RENDERED.isFalse()))
                                   .fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public boolean existsById(int dataId) {
        return dsl.selectCount().from(DATA)
                .where(DATA.ID.eq(dataId))
                .fetchOne(0, Integer.class) > 0;
    }

    /**
     * Select count of data and return the result.
     * @param includeInvisibleData flag that indicates if the count will includes hidden data.
     * @return count of rows in table
     */
    @Override
    public Integer countAll(boolean includeInvisibleData) {
        if(includeInvisibleData) {
            return dsl.selectCount().from(DATA).fetchOne(0,int.class);
        } else {
            return dsl.selectCount().from(DATA).where(DATA.HIDDEN.eq(false).and(DATA.INCLUDED.eq(true))).fetchOne(0, int.class);
        }
    }

    @Override
    public Integer getDatasetId(int dataId) {
        return dsl.select(DATA.DATASET_ID).from(DATA).where(DATA.ID.eq(dataId)).fetchOneInto(Integer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateOwner(int dataId, int newOwner) {
        dsl.update(DATA)
                .set(DATA.OWNER, newOwner)
                .where(DATA.ID.eq(dataId))
                .execute();
    }

    // -------------------------------------------------------------------------
    //  Private utility methods
    // -------------------------------------------------------------------------

    private static Field<Integer> countStyles(Field<Integer> dataId) {
        return DSL.selectCount().from(STYLED_DATA)
                .where(STYLED_DATA.DATA.eq(dataId))
                .asField();
    }

    private static Field<Integer> countLayers(Field<Integer> dataId) {
        return DSL.selectCount().from(LAYER)
                .leftOuterJoin(DATA_X_DATA).on(DATA_X_DATA.CHILD_ID.eq(LAYER.DATA)) // layer -> data_x_data (child_id)
                .where(LAYER.DATA.eq(dataId).or(DATA_X_DATA.DATA_ID.eq(dataId)))
                .asField();
    }

    private static Field<Integer> countServices(Field<Integer> dataId) {
        // "Layer" services.
        Field<Integer> layerServices = DSL.select(DSL.countDistinct(LAYER.SERVICE)).from(LAYER)
                .leftOuterJoin(DATA_X_DATA).on(DATA_X_DATA.CHILD_ID.eq(LAYER.DATA)) // layer -> data_x_data (child_id)
                .where(LAYER.DATA.eq(dataId).or(DATA_X_DATA.DATA_ID.eq(dataId)))
                .asField();

        // "Metadata" services.
        org.constellation.database.api.jooq.tables.Data dataAlias = DATA.as("target_data");
        Field<Integer> metadataServices = DSL.select(DSL.countDistinct(METADATA_X_CSW.CSW_ID)).from(dataAlias)
                .join(METADATA).on(METADATA.DATASET_ID.eq(dataAlias.DATASET_ID)) // data -> metadata
                .join(METADATA_X_CSW).on(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID)) // metadata -> metadata_x_csw
                .where(dataAlias.ID.eq(dataId))
                .asField();

        // Sum.
        return layerServices.add(metadataServices);
    }

    private static Field<Integer> countSensors(Field<Integer> dataId) {
        return DSL.selectCount().from(SENSORED_DATA)
                .where(SENSORED_DATA.DATA.eq(dataId))
                .asField();
    }

    private static Field<String> selectConformPyramidProviderIdentifier(Field<Integer> dataId) {
        org.constellation.database.api.jooq.tables.Data dataAlias = DATA.as("child_data");
        org.constellation.database.api.jooq.tables.Provider providerAlias = PROVIDER.as("child_provider");
        return DSL.select(providerAlias.IDENTIFIER).from(DATA_X_DATA)
                .join(dataAlias).on(dataAlias.ID.eq(DATA_X_DATA.CHILD_ID)) // data_x_data (child_id) -> data
                .join(providerAlias).on(providerAlias.ID.eq(dataAlias.PROVIDER)) // data -> provider
                .where(DATA_X_DATA.DATA_ID.eq(dataId)).and(dataAlias.SUBTYPE.eq("pyramid").and(dataAlias.RENDERED.eq(false)))
                .asField();
    }

    @Override
    public Map.Entry<Integer, List<Data>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {

        //add default filter
        if (filterMap == null) {
           filterMap = new HashMap<>();
        }
        if (!filterMap.containsKey("hidden")) {
            filterMap.put("hidden", false);
        }
        if (!filterMap.containsKey("included")) {
            filterMap.put("included", true);
        }

        // build SQL query
        Select query = null;
        for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
            final Condition cond = buidCondition(entry.getKey(), entry.getValue());
            if (cond != null) {
                if (query == null) {
                    query = dsl.select(DATA.fields()).from(DATA).where(cond);
                } else {
                    query = ((SelectConditionStep) query).and(cond);
                }
            }
        }

        // add sort
        if(sortEntry != null) {
            final SortField f;
            if("title".equals(sortEntry.getKey())){
                f = "ASC".equals(sortEntry.getValue()) ? DATA.NAME.lower().asc() : DATA.NAME.lower().desc();
            }else {
                f = "ASC".equals(sortEntry.getValue()) ? DATA.DATE.asc() : DATA.DATE.desc();
            }
            if (query == null) {
                query = dsl.select(DATA.fields()).from(DATA).orderBy(f);
            } else {
                query = ((SelectConditionStep)query).orderBy(f);
            }
        }

        final Map.Entry<Integer,List<Data>> result;
        if (query == null) {
            final int count = dsl.selectCount().from(DATA).fetchOne(0,int.class);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertDataListToDto(dsl.select(DATA.fields()).from(DATA).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class)));
        } else {
            final int count = dsl.fetchCount(query);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertDataListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class)));
        }
        return result;
    }

    public Condition buidCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return DATA.OWNER.equal((Integer) value);
        } else if ("dataset".equals(key)) {
            return DATA.DATASET_ID.equal((Integer) value);
        } else if ("provider_id".equals(key)) {
            return DATA.PROVIDER.equal((Integer) value);
        } else if ("rendered".equals(key)) {
            return DATA.RENDERED.equal((Boolean) value);
        } else if ("included".equals(key)) {
            return DATA.INCLUDED.equal((Boolean) value);
        } else if ("id".equals(key)) {
            return DATA.ID.equal((Integer) value);
        } else if ("sub_type".equals(key)) {
            return DATA.SUBTYPE.equal((String) value);
        } else if ("hidden".equals(key)) {
            return DATA.HIDDEN.equal((Boolean) value);
        } else if ("sensorable".equals(key)) {
            return DATA.SENSORABLE.equal((Boolean) value);
        } else if ("term".equals(key)) {
            return DATA.NAME.likeIgnoreCase("%" + value + "%");
        } else if ("period".equals(key)) {
            return DATA.DATE.greaterOrEqual((Long) value);
        } else if ("type".equals(key)) {
            return DATA.TYPE.eq((String) value);
        } else if ("OR".equals(key)) {
            List<Map.Entry<String, Object>> values =  (List<Map.Entry<String, Object>>) value;
            Condition c = null;
            for (Map.Entry<String, Object> e: values) {
                Condition c2 = buidCondition(e.getKey(), e.getValue());
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
                Condition c2 = buidCondition(e.getKey(), e.getValue());
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
    public List<Data> findAllByVisibility(boolean hidden) {
        return convertDataListToDto(dsl.select().from(DATA).where(DATA.HIDDEN.eq(hidden)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findAll() {
        return convertDataListToDto(dsl.select().from(DATA).fetchInto(org.constellation.database.api.jooq.tables.pojos.Data.class));
    }

    public static List<Data> convertDataListToDto(List<org.constellation.database.api.jooq.tables.pojos.Data> daos) {
        List<Data> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Data dao : daos) {
            results.add(convertDataIntoDto(dao));
        }
        return results;
    }

    public static Data convertDataIntoDto(final org.constellation.database.api.jooq.tables.pojos.Data dao) {
        if (dao != null) {
            final org.constellation.dto.Data dto = new org.constellation.dto.Data();
            dto.setDatasetId(dao.getDatasetId());
            dto.setDate(new Date(dao.getDate()));
            dto.setHidden(dao.getHidden());
            dto.setId(dao.getId());
            dto.setIncluded(dao.getIncluded());
            dto.setMetadata(dao.getMetadata());
            dto.setName(dao.getName());
            dto.setNamespace(dao.getNamespace());
            dto.setOwnerId(dao.getOwner());
            dto.setProviderId(dao.getProvider());
            dto.setRendered(dao.getRendered());
            dto.setSensorable(dao.getSensorable());
            dto.setStatsResult(dao.getStatsResult());
            dto.setStatsState(dao.getStatsState());
            dto.setSubtype(dao.getSubtype());
            dto.setType(dao.getType());
            return dto;
        }
        return null;
    }

    private org.constellation.database.api.jooq.tables.pojos.Data convertIntoDao(final Data dto) {
        if (dto != null) {
            final org.constellation.database.api.jooq.tables.pojos.Data dao = new org.constellation.database.api.jooq.tables.pojos.Data();
            dao.setDatasetId(dto.getDatasetId());
            dao.setDate(dto.getDate() != null ? dto.getDate().getTime() : null);
            dao.setHidden(dto.getHidden());
            dao.setId(dto.getId());
            dao.setIncluded(dto.getIncluded());
            dao.setMetadata(dto.getMetadata());
            dao.setName(dto.getName());
            dao.setNamespace(dto.getNamespace());
            dao.setOwner(dto.getOwnerId());
            dao.setProvider(dto.getProviderId());
            dao.setRendered(dto.getRendered());
            dao.setSensorable(dto.getSensorable());
            dao.setStatsResult(dto.getStatsResult());
            dao.setStatsState(dto.getStatsState());
            dao.setSubtype(dto.getSubtype());
            dao.setType(dto.getType());
            return dao;
        }
        return null;
    }
}
