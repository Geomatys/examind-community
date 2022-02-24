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
import com.examind.database.api.jooq.tables.Data;
import com.examind.database.api.jooq.tables.pojos.Dataset;
import com.examind.database.api.jooq.tables.pojos.Metadata;
import com.examind.database.api.jooq.tables.records.DatasetRecord;
import org.constellation.repository.DatasetRepository;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.UpdateConditionStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.examind.database.api.jooq.Tables.CSTL_USER;
import static com.examind.database.api.jooq.Tables.DATA;
import static com.examind.database.api.jooq.Tables.DATASET;
import static com.examind.database.api.jooq.Tables.LAYER;
import static com.examind.database.api.jooq.Tables.METADATA;
import static com.examind.database.api.jooq.Tables.METADATA_X_CSW;
import static com.examind.database.api.jooq.Tables.SENSORED_DATA;
import org.constellation.dto.DataSet;
import org.constellation.exception.ConstellationPersistenceException;
import org.jooq.Select;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
import org.springframework.context.annotation.DependsOn;

/**
 *
 * @author Guilhem Legal
 */
@Component
@DependsOn("database-initer")
public class JooqDatasetRepository extends AbstractJooqRespository<DatasetRecord, Dataset> implements
        DatasetRepository {

    public JooqDatasetRepository() {
        super(Dataset.class, DATASET);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Integer create(DataSet dataset) {
        DatasetRecord newRecord = dsl.newRecord(DATASET);
        newRecord.setIdentifier(dataset.getIdentifier());
        newRecord.setOwner(dataset.getOwnerId());
        newRecord.setDate(dataset.getDate());
        newRecord.setType(dataset.getType());
        newRecord.store();
        return newRecord.getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int update(DataSet dataset) {
        DatasetRecord datasetRecord = new DatasetRecord();
        datasetRecord.from(dataset);
        UpdateConditionStep<DatasetRecord> set = dsl.update(DATASET)
                .set(DATASET.IDENTIFIER, dataset.getIdentifier())
                .set(DATASET.OWNER, dataset.getOwnerId())
                .set(DATASET.DATE, dataset.getDate())
                .set(DATASET.TYPE, dataset.getType())
                .where(DATASET.ID.eq(dataset.getId()));

        return set.execute();

    }

    @Override
    public DataSet findByMetadataId(String metadataId) {
        return convertDatasetIntoDto(dsl.select(DATASET.fields())
                                        .from(DATASET)
                                        .join(METADATA).onKey(METADATA.DATASET_ID)
                                        .where(METADATA.METADATA_ID.eq(metadataId))
                                        .fetchOneInto(Dataset.class));
    }

    @Override
    public DataSet findByIdentifier(String identifier) {
        return convertDatasetIntoDto(dsl.select()
                                        .from(DATASET)
                                        .where(DATASET.IDENTIFIER.eq(identifier))
                                        .fetchOneInto(Dataset.class));
    }

    @Override
    public Integer findIdForIdentifier(String identifier) {
        return dsl.select(DATASET.ID)
                  .from(DATASET)
                  .where(DATASET.IDENTIFIER.eq(identifier))
                  .fetchOneInto(Integer.class);
    }

    @Override
    public DataSet findByIdentifierWithEmptyMetadata(String identifier) {
        List<Dataset> datas = dsl.select().from(DATASET).where(DATASET.IDENTIFIER.eq(identifier)).fetchInto(Dataset.class);
        for (Dataset dataset : datas) {
            Metadata m = dsl.select().from(METADATA).where(METADATA.DATASET_ID.eq(dataset.getId())).fetchOneInto(Metadata.class);
            if (m == null) {
                return convertDatasetIntoDto(dataset);
            }
        }
        return null;
    }

    @Override
    public DataSet findById(int id) {
        return convertDatasetIntoDto(dsl.select().from(DATASET).where(DATASET.ID.eq(id)).fetchOneInto(Dataset.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        return dsl.delete(DATASET).where(DATASET.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        return dsl.delete(DATASET).execute();
    }

    @Override
    public List<DataSet> getCswLinkedDataset(final int cswId) {
        return convertDatasetListToDto(
             dsl.select(DATASET.fields()).from(DATASET, METADATA, METADATA_X_CSW)
                .where(METADATA.ID.eq(METADATA_X_CSW.METADATA_ID))
                .and(DATASET.ID.eq(METADATA.DATASET_ID))
                .and(METADATA_X_CSW.CSW_ID.eq(cswId)).and(METADATA.DATASET_ID.isNotNull())
                .fetchInto(Dataset.class));
    }

    @Override
    public boolean existsById(Integer datasetId) {
        return dsl.selectCount().from(DATASET)
                .where(DATASET.ID.eq(datasetId))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public boolean existsByName(String datasetName) {
        return dsl.selectCount().from(DATASET)
                .where(DATASET.IDENTIFIER.eq(datasetName))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public Map.Entry<Integer, List<DataSet>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {

        // build SQL query
        SelectJoinStep baseQuery = dsl.select(DATASET.fields()).from(DATASET).leftOuterJoin(CSTL_USER).on(CSTL_USER.ID.eq(DATASET.OWNER)); // dataset -> cstl_user
        SelectConnectByStep fquery = buildQuery(baseQuery, filterMap);

        // add sort
        Select query;
        if (sortEntry != null) {
            final SortField f;
            if ("title".equals(sortEntry.getKey()) || "name".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? DSL.lower(DATASET.IDENTIFIER).asc() : DSL.lower(DATASET.IDENTIFIER).desc();
            } else if ("owner_login".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? CSTL_USER.LOGIN.asc() : CSTL_USER.LOGIN.desc();
            } else { //default sorting on date stamp
                f = "ASC".equals(sortEntry.getValue()) ? DATASET.DATE.asc() : DATASET.DATE.desc();
            }
            query = fquery.orderBy(f);
        } else {
            query = fquery;
        }

        final int count = dsl.fetchCount(query);
        final Map.Entry<Integer,List<DataSet>> result = new AbstractMap.SimpleImmutableEntry<>(count,
                convertDatasetListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage)
                                         .fetchInto(Dataset.class)));
        return result;
    }

    @Override
    protected Condition buildSpecificCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return DATASET.OWNER.equal(castOrThrow(key, value, Integer.class));
        } else if ("id".equals(key)) {
            return DATASET.ID.equal(castOrThrow(key, value, Integer.class));
        } else if ("term".equals(key)) {
            String likeExpr = '%' + castOrThrow(key, value, String.class) + '%';
            Field<Integer> countNamedData = countNamedData(DATASET.ID, likeExpr);
            return DATASET.IDENTIFIER.likeIgnoreCase(likeExpr)
               .or(CSTL_USER.LOGIN.likeIgnoreCase(likeExpr))
               .or(countNamedData.greaterThan(0));
        } else if ("period".equals(key)) {
            return DATASET.DATE.greaterOrEqual(castOrThrow(key, value, Long.class));
        } else if ("type".equals(key)) {
            return DATASET.TYPE.eq(castOrThrow(key, value, String.class));
        } else if ("excludeEmpty".equals(key)) {
            Boolean is = castOrThrow(key, value, Boolean.class);
            if (is) {
                return countData(DATASET.ID).greaterThan(0);
            } else {
                return DSL.trueCondition();
            }
        } else if ("hasVectorData".equals(key)) {
            Boolean hvd = castOrThrow(key, value, Boolean.class);
            Field<Integer> countVectorData = countDataOfType(DATASET.ID, "VECTOR");
            return hvd ? countVectorData.greaterThan(0) : countVectorData.eq(0);

        } else if ("hasCoverageData".equals(key)) {
            Boolean hcd = castOrThrow(key, value, Boolean.class);
            Field<Integer> countCoverageData = countDataOfType(DATASET.ID, "COVERAGE");
            return hcd ? countCoverageData.greaterThan(0) : countCoverageData.eq(0);

        } else if ("hasLayerData".equals(key)) {
            Boolean hld = castOrThrow(key, value, Boolean.class);
            Field<Integer> countLayerData = countLayerData(DATASET.ID);
            return hld ? countLayerData.greaterThan(0) : countLayerData.eq(0);

        } else if ("hasSensorData".equals(key)) {
            Boolean hsd = castOrThrow(key, value, Boolean.class);
            Field<Integer> countSensorData = countSensorData(DATASET.ID);
            return hsd ? countSensorData.greaterThan(0) : countSensorData.eq(0);

        }
        throw new ConstellationPersistenceException(key + " parameter is not supported.");
    }

    @Override
    public List<Integer> getAllIds() {
        return dsl.select(DATASET.ID).from(DATASET).fetchInto(Integer.class);
    }

    // -------------------------------------------------------------------------
    //  Private utility methods
    // -------------------------------------------------------------------------

    private static Field<Integer> countData(Field<Integer> datasetId) {
        return DSL.selectCount().from(DATA)
                .where(DATA.DATASET_ID.eq(datasetId))
                .and(isIncludedAndNotHiddenData(DATA))
                .asField();
    }

    private static Field<Integer> countDataOfType(Field<Integer> datasetId, String type) {
        return DSL.selectCount().from(DATA)
                .where(DATA.DATASET_ID.eq(datasetId))
                .and(isIncludedAndNotHiddenData(DATA))
                .and(DATA.TYPE.eq(type))
                .asField();
    }

    private static Field<Integer> countNamedData(Field<Integer> datasetId, String term) {
        return DSL.selectCount().from(DATA)
                .where(DATA.DATASET_ID.eq(datasetId))
                .and(isIncludedAndNotHiddenData(DATA))
                .and(DATA.NAME.likeIgnoreCase(term))
                .asField();
    }

    private static Field<Integer> countLayerData(Field<Integer> datasetId) {
        return DSL.selectCount().from(LAYER)
                .join(DATA).on(LAYER.DATA.eq(DATA.ID)) // layer -> data
                .where(DATA.DATASET_ID.eq(datasetId))
                .and(isIncludedAndNotHiddenData(DATA))
                .asField();
    }

    private static Field<Integer> countSensorData(Field<Integer> datasetId) {
        return DSL.selectCount().from(SENSORED_DATA)
                .join(DATA).on(SENSORED_DATA.DATA.eq(DATA.ID)) // sensored_data -> data
                .where(DATA.DATASET_ID.eq(datasetId))
                .and(isIncludedAndNotHiddenData(DATA))
                .asField();
    }

    private static Condition isIncludedAndNotHiddenData(Data dataTable) {
        return dataTable.INCLUDED.eq(true).and(dataTable.HIDDEN.eq(false));
    }

    @Override
    public Integer getDataCount(int datasetId) {
        return dsl.selectCount().from(DATA)
                .where(DATA.DATASET_ID.eq(datasetId))
                .and(isIncludedAndNotHiddenData(DATA)).fetchOne(0, Integer.class);
    }

    @Override
    public List<DataSet> findAll() {
        return convertDatasetListToDto(dsl.select().from(DATASET).fetchInto(Dataset.class));
    }

    public static List<DataSet> convertDatasetListToDto(List<Dataset> daos) {
        List<DataSet> results = new ArrayList<>();
        for (Dataset dao : daos) {
            results.add(convertDatasetIntoDto(dao));
        }
        return results;
    }

    public static DataSet convertDatasetIntoDto(final Dataset dao) {
        if (dao != null) {
            final DataSet dto = new DataSet();
            dto.setId(dao.getId());
            dto.setIdentifier(dao.getIdentifier());
            dto.setOwnerId(dao.getOwner());
            dto.setDate(dao.getDate());
            dto.setType(dao.getType());
            return dto;
        }
        return null;
    }

}
