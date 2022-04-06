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
import com.examind.database.api.jooq.Tables;
import org.constellation.dto.Data;
import org.constellation.dto.DimensionRange;
import com.examind.database.api.jooq.tables.pojos.DataXData;
import com.examind.database.api.jooq.tables.pojos.Metadata;
import com.examind.database.api.jooq.tables.records.DataRecord;
import com.examind.database.api.jooq.tables.records.DataXDataRecord;
import com.examind.database.api.jooq.tables.records.DataDimRangeRecord;
import com.examind.database.api.jooq.tables.records.DataElevationsRecord;
import com.examind.database.api.jooq.tables.records.DataEnvelopeRecord;
import com.examind.database.api.jooq.tables.records.DataTimesRecord;
import org.constellation.repository.DataRepository;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.Record2;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.examind.database.api.jooq.Tables.DATA;
import static com.examind.database.api.jooq.Tables.DATA_DIM_RANGE;
import static com.examind.database.api.jooq.Tables.DATA_TIMES;
import static com.examind.database.api.jooq.Tables.DATA_ELEVATIONS;
import static com.examind.database.api.jooq.Tables.DATA_ENVELOPE;
import static com.examind.database.api.jooq.Tables.DATA_X_DATA;
import static com.examind.database.api.jooq.Tables.LAYER;
import static com.examind.database.api.jooq.Tables.METADATA;
import static com.examind.database.api.jooq.Tables.METADATA_X_CSW;
import static com.examind.database.api.jooq.Tables.PROVIDER;
import static com.examind.database.api.jooq.Tables.SENSORED_DATA;
import static com.examind.database.api.jooq.Tables.SERVICE;
import static com.examind.database.api.jooq.Tables.STYLED_DATA;
import org.constellation.exception.ConstellationPersistenceException;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqDataRepository extends AbstractJooqRespository<DataRecord, com.examind.database.api.jooq.tables.pojos.Data> implements DataRepository {

    public JooqDataRepository() {
        super(com.examind.database.api.jooq.tables.pojos.Data.class, DATA);
    }

    @Override
    public Data findById(int id) {
        return convertDataIntoDto(dsl.select().from(DATA).where(DATA.ID.eq(id)).fetchOneInto(com.examind.database.api.jooq.tables.pojos.Data.class));
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
    public int delete(Integer id) {
        return dsl.delete(DATA).where(DATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(DATA).execute();
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
                                 .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Data.class));
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
                                             .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findByProviderId(Integer id) {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.PROVIDER.eq(id))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }


    @Override
    public List<Data> findByProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        SelectConditionStep c = dsl.select(DATA.fields()).from(DATA)
                .where(DATA.PROVIDER.eq(id)).and(DATA.INCLUDED.eq(included)).and(DATA.HIDDEN.eq(hidden));

        if (dataType != null) {
            c = c.and(DATA.TYPE.eq(dataType));
        }
        return convertDataListToDto(c.fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
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
    public List<Integer> findIdsByDatasetId(Integer id, boolean included, boolean hidden) {
        SelectConditionStep c = dsl.select(DATA.ID).from(DATA)
                .where(DATA.DATASET_ID.eq(id)).and(DATA.INCLUDED.eq(included)).and(DATA.HIDDEN.eq(hidden));
        return c.fetchInto(Integer.class);
    }

    @Override
    public List<Data> findByDatasetId(Integer id) {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.DATASET_ID.eq(id))
                                   .and(DATA.INCLUDED.eq(Boolean.TRUE))
                                   .and(DATA.HIDDEN.isNull().or(DATA.HIDDEN.isFalse()))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findByDatasetId(Integer id, boolean included, boolean hidden) {
        return convertDataListToDto(dsl.select().from(DATA)
                                   .where(DATA.DATASET_ID.eq(id))
                                   .and(DATA.INCLUDED.eq(included))
                                   .and(DATA.HIDDEN.eq(hidden))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findAllByDatasetId(Integer id) {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.DATASET_ID.eq(id))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public Data findByNameAndNamespaceAndProviderId(String localPart, String namespaceURI, Integer providerId) {
        return convertDataIntoDto(dsl.select()
                                 .from(DATA)
                                 .where(DATA.PROVIDER.eq(providerId))
                                 .and(DATA.NAME.eq(localPart))
                                 .and(DATA.NAMESPACE.eq(namespaceURI))
                                 .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findByServiceId(Integer id) {
        return convertDataListToDto(dsl.select().from(DATA).join(LAYER).on(LAYER.DATA.eq(DATA.ID)).join(SERVICE).on(LAYER.SERVICE.eq(SERVICE.ID)).where(SERVICE.ID.eq(id))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
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
        List<com.examind.database.api.jooq.tables.pojos.Data> datas = dsl.select()
                                                                               .from(DATA)
                                                                               .where(DATA.NAME.eq(localPart))
                                                                               .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class);
        for (com.examind.database.api.jooq.tables.pojos.Data data : datas) {
            List<Metadata> m = dsl.select().from(METADATA).where(METADATA.DATA_ID.eq(data.getId())).fetchInto(Metadata.class);
            if (m.isEmpty()) {
                return convertDataIntoDto(data);
            }
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
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
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Integer> getParents(Integer id) {
        return dsl.select(DATA_X_DATA.DATA_ID)
                .from(DATA_X_DATA)
                .where(DATA_X_DATA.CHILD_ID.eq(id))
                .fetchInto(Integer.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeLinkedData(int dataId) {
        dsl.delete(DATA_X_DATA).where(DATA_X_DATA.DATA_ID.eq(dataId)).execute();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Data> getDataByLinkedStyle(final int styleId) {
        return convertDataListToDto(dsl.select(DATA.fields())
                                   .from(DATA)
                                   .join(STYLED_DATA).onKey(STYLED_DATA.DATA)
                                   .where(STYLED_DATA.STYLE.eq(styleId))
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findStatisticLess() {
        return convertDataListToDto(dsl.select()
                                   .from(DATA)
                                   .where(DATA.TYPE.eq("COVERAGE"))
                                   .and(DATA.RENDERED.isNull().or(DATA.RENDERED.isFalse()))
                                   .and(DATA.SUBTYPE.isNull().orNot(DATA.SUBTYPE.equalIgnoreCase("pyramid")))
                                   .and(DATA.HIDDEN.isFalse())
                                   .fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public boolean existsById(Integer dataId) {
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
    public Integer getProviderId(int dataId) {
        return dsl.select(DATA.PROVIDER).from(DATA).where(DATA.ID.eq(dataId)).fetchOneInto(Integer.class);
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
        com.examind.database.api.jooq.tables.Data dataAlias = DATA.as("target_data");
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
        com.examind.database.api.jooq.tables.Data dataAlias = DATA.as("child_data");
        com.examind.database.api.jooq.tables.Provider providerAlias = PROVIDER.as("child_provider");
        return DSL.select(providerAlias.IDENTIFIER).from(DATA_X_DATA)
                .join(dataAlias).on(dataAlias.ID.eq(DATA_X_DATA.CHILD_ID)) // data_x_data (child_id) -> data
                .join(providerAlias).on(providerAlias.ID.eq(dataAlias.PROVIDER)) // data -> provider
                .where(DATA_X_DATA.DATA_ID.eq(dataId)).and(dataAlias.SUBTYPE.eq("pyramid").and(dataAlias.RENDERED.eq(false)))
                .asField();
    }

    @Override
    public Map.Entry<Integer, List<Data>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {

        if (filterMap == null) {
           filterMap = new HashMap<>();
        }
        //add default filter
        filterMap.putIfAbsent("hidden",  false); // problem if there is a "hidden" filter in a sub clause
        filterMap.putIfAbsent("included", true); // same problem

        // build filtered SQL query
        SelectJoinStep baseQuery = dsl.select(DATA.fields()).from(DATA);
        SelectConnectByStep fquery = buildQuery(baseQuery, filterMap);

        // add sort
        Select query;
        if (sortEntry != null) {
            final SortField f;
            if ("title".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? DSL.lower(DATA.NAME).asc() : DSL.lower(DATA.NAME).desc();
            } else { //default sorting on date stamp
                f = "ASC".equals(sortEntry.getValue()) ? DATA.DATE.asc() : DATA.DATE.desc();
            }
            query = fquery.orderBy(f);
        } else {
            query = fquery;
        }
        
        final int count = dsl.fetchCount(query);
        final Map.Entry<Integer,List<Data>> result = new AbstractMap.SimpleImmutableEntry<>(count,
                convertDataListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage).fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class)));
        return result;
    }

    @Override
    protected Condition buildSpecificCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return DATA.OWNER.equal(castOrThrow(key, value, Integer.class));
        } else if ("dataset".equals(key)) {
            return DATA.DATASET_ID.equal(castOrThrow(key, value, Integer.class));
        } else if ("provider_id".equals(key)) {
            return DATA.PROVIDER.equal(castOrThrow(key, value, Integer.class));
        } else if ("rendered".equals(key)) {
            return DATA.RENDERED.equal(castOrThrow(key, value, Boolean.class));
        } else if ("included".equals(key)) {
            return DATA.INCLUDED.equal(castOrThrow(key, value, Boolean.class));
        } else if ("id".equals(key)) {
            return DATA.ID.equal(castOrThrow(key, value, Integer.class));
        } else if ("sub_type".equals(key)) {
            return DATA.SUBTYPE.equal(castOrThrow(key, value, String.class));
        } else if ("hidden".equals(key)) {
            return DATA.HIDDEN.equal(castOrThrow(key, value, Boolean.class));
        } else if ("sensorable".equals(key)) {
            return DATA.SENSORABLE.equal(castOrThrow(key, value, Boolean.class));
        } else if ("term".equals(key)) {
            return DATA.NAME.likeIgnoreCase("%" + castOrThrow(key, value, String.class) + "%");
        } else if ("period".equals(key)) {
            return DATA.DATE.greaterOrEqual(castOrThrow(key, value, Long.class));
        } else if ("type".equals(key)) {
            return DATA.TYPE.eq(castOrThrow(key, value, String.class));
        }
        throw new ConstellationPersistenceException(key + " parameter is not supported.");
    }

    @Override
    public List<Data> findAllByVisibility(boolean hidden) {
        return convertDataListToDto(dsl.select().from(DATA).where(DATA.HIDDEN.eq(hidden)).fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    @Override
    public List<Data> findAll() {
        return convertDataListToDto(dsl.select().from(DATA).fetchInto(com.examind.database.api.jooq.tables.pojos.Data.class));
    }

    public static List<Data> convertDataListToDto(List<com.examind.database.api.jooq.tables.pojos.Data> daos) {
        List<Data> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.Data dao : daos) {
            results.add(convertDataIntoDto(dao));
        }
        return results;
    }

    public static Data convertDataIntoDto(final com.examind.database.api.jooq.tables.pojos.Data dao) {
        if (dao != null) {
            final org.constellation.dto.Data dto = new org.constellation.dto.Data();
            dto.setDatasetId(dao.getDatasetId());
            dto.setDate(new Date(dao.getDate()));
            dto.setHidden(dao.getHidden());
            dto.setId(dao.getId());
            dto.setIncluded(dao.getIncluded());
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
            dto.setCrs(dao.getCrs());
            dto.setHasDim(dao.getHasDim());
            dto.setHasElevation(dao.getHasElevation());
            dto.setHasTime(dao.getHasTime());
            dto.setCachedInfo(dao.getCachedInfo());
            return dto;
        }
        return null;
    }

    private com.examind.database.api.jooq.tables.pojos.Data convertIntoDao(final Data dto) {
        if (dto != null) {
            final com.examind.database.api.jooq.tables.pojos.Data dao = new com.examind.database.api.jooq.tables.pojos.Data();
            dao.setDatasetId(dto.getDatasetId());
            dao.setDate(dto.getDate() != null ? dto.getDate().getTime() : null);
            dao.setHidden(dto.getHidden());
            dao.setId(dto.getId());
            dao.setIncluded(dto.getIncluded());
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
            dao.setCrs(dto.getCrs());
            dao.setHasDim(dto.getHasDim());
            dao.setHasElevation(dto.getHasElevation());
            dao.setHasTime(dto.getHasTime());
            dao.setCachedInfo(dto.getCachedInfo());
            return dao;
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateDataBBox(int dataId, String crs, List<Double[]> coordinates) {
        dsl.update(DATA)
                .set(DATA.CRS, crs)
                .set(DATA.CACHED_INFO, true)
                .where(DATA.ID.eq(dataId))
                .execute();
        dsl.delete(DATA_ENVELOPE).where(DATA_ENVELOPE.DATA_ID.eq(dataId)).execute();
        for (int i = 0; i < coordinates.size(); i++) {
            DataEnvelopeRecord newRecord = dsl.newRecord(DATA_ENVELOPE);
            newRecord.setDataId(dataId);
            newRecord.setDimension(i);
            newRecord.setMin(coordinates.get(i)[0]);
            newRecord.setMax(coordinates.get(i)[1]);
            newRecord.store();
        }
    }
    
    @Override
    public List<Double[]> getDataBBox(int dataId) {
        List<Double[]> results = new ArrayList<>();
        List<DataEnvelopeRecord> deas = dsl.select().from(DATA_ENVELOPE).where(DATA_ENVELOPE.DATA_ID.eq(dataId)).orderBy(DATA_ENVELOPE.DIMENSION).fetchInto(DataEnvelopeRecord.class);
        for (DataEnvelopeRecord dea : deas) {
           results.add(new Double[]{dea.getMin(), dea.getMax()});
        }
        return results;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateDataTimes(int dataId, Set<Date> dates) {
        dsl.delete(DATA_TIMES).where(DATA_TIMES.DATA_ID.eq(dataId)).execute();
        for (Date d : dates) {
            DataTimesRecord newRecord = dsl.newRecord(DATA_TIMES);
            newRecord.setDataId(dataId);
            newRecord.setDate(d.getTime());
            newRecord.store();
        }
        dsl.update(DATA)
                .set(DATA.HAS_TIME, !dates.isEmpty())
                .where(DATA.ID.eq(dataId))
                .execute();

    }
    
    @Override
    public SortedSet<Date> getDataTimes(int dataId, boolean range) {
        final SortedSet<Date> results = new TreeSet<>();
        if (range) {
            Record2<Long, Long> rec = dsl.select(DATA_TIMES.DATE.min().as("min"), DATA_TIMES.DATE.max().as("max")).from(DATA_TIMES).where(DATA_TIMES.DATA_ID.eq(dataId)).fetchOne();
            if (rec.value1() != null && rec.value2() != null) {
                results.add(new Date(rec.value1()));
                results.add(new Date(rec.value2()));
            }
        } else {
            List<DataTimesRecord> deas = dsl.select().from(DATA_TIMES).where(DATA_TIMES.DATA_ID.eq(dataId)).fetchInto(DataTimesRecord.class);
            for (DataTimesRecord dea : deas) {
               results.add(new Date(dea.getDate()));
            }
        }
        return results;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateDataElevations(int dataId, Set<Number> elevations) {
        dsl.delete(DATA_ELEVATIONS).where(DATA_ELEVATIONS.DATA_ID.eq(dataId)).execute();
        for (Number d : elevations) {
            DataElevationsRecord newRecord = dsl.newRecord(DATA_ELEVATIONS);
            newRecord.setDataId(dataId);
            newRecord.setElevation(d.doubleValue());
            newRecord.store();
        }
        dsl.update(DATA)
                .set(DATA.HAS_ELEVATION, !elevations.isEmpty())
                .where(DATA.ID.eq(dataId))
                .execute();
    }
    
    @Override
    public SortedSet<Number> getDataElevations(int dataId) {
        final SortedSet<Number> results = new TreeSet<>();
        List<DataElevationsRecord> deas = dsl.select().from(DATA_ELEVATIONS).where(DATA_ELEVATIONS.DATA_ID.eq(dataId)).fetchInto(DataElevationsRecord.class);
        for (DataElevationsRecord dea : deas) {
           results.add(dea.getElevation());
        }
        return results;
    }

    @Override
    public boolean isCachedDataInfo(int dataId) {
        return dsl.select(DATA.CACHED_INFO).from(DATA).where(DATA.ID.eq(dataId)).fetchOneInto(Boolean.class);
    }

    @Override
    public SortedSet<DimensionRange> getDataDimensionRange(int dataId) {
        final SortedSet<DimensionRange> results = new TreeSet<>();
        List<DataDimRangeRecord> deas = dsl.select().from(DATA_DIM_RANGE).where(DATA_DIM_RANGE.DATA_ID.eq(dataId)).orderBy(DATA_DIM_RANGE.DIMENSION).fetchInto(DataDimRangeRecord.class);
        for (DataDimRangeRecord dea : deas) {
           results.add(new DimensionRange(dea.getMin(), dea.getMax(), dea.getUnit(), dea.getUnitSymbol()));
        }
        return results;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateDimensionRange(int dataId, Set<DimensionRange> dimensions) {
        dsl.delete(DATA_DIM_RANGE).where(DATA_DIM_RANGE.DATA_ID.eq(dataId)).execute();
        for (DimensionRange d : dimensions) {
            DataDimRangeRecord newRecord = dsl.newRecord(DATA_DIM_RANGE);
            newRecord.setDataId(dataId);
            newRecord.setMin(d.getMin());
            newRecord.setMax(d.getMin());
            newRecord.setUnit(d.getUnit());
            newRecord.setUnitSymbol(d.getUnitsymbol());
            newRecord.store();
        }
        dsl.update(DATA)
                .set(DATA.HAS_DIM, !dimensions.isEmpty())
                .where(DATA.ID.eq(dataId))
                .execute();
    }
}
