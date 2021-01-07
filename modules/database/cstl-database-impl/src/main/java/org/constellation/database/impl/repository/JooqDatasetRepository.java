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
import java.util.HashMap;
import org.constellation.database.api.jooq.tables.Data;
import org.constellation.database.api.jooq.tables.pojos.Dataset;
import org.constellation.database.api.jooq.tables.pojos.Metadata;
import org.constellation.database.api.jooq.tables.pojos.MetadataXCsw;
import org.constellation.database.api.jooq.tables.records.DatasetRecord;
import org.constellation.database.api.jooq.tables.records.MetadataXCswRecord;
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

import static org.constellation.database.api.jooq.Tables.CSTL_USER;
import static org.constellation.database.api.jooq.Tables.DATA;
import static org.constellation.database.api.jooq.Tables.DATASET;
import static org.constellation.database.api.jooq.Tables.LAYER;
import static org.constellation.database.api.jooq.Tables.METADATA;
import static org.constellation.database.api.jooq.Tables.METADATA_X_CSW;
import static org.constellation.database.api.jooq.Tables.SENSORED_DATA;
import org.constellation.dto.DataSet;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
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
    @Transactional(propagation = Propagation.MANDATORY)
    public void addDatasetToCSW(final int serviceID, final int datasetID) {
        final Metadata metadata = dsl.select().from(METADATA).where(METADATA.DATASET_ID.eq(datasetID)).fetchOneInto(Metadata.class);
        if (metadata != null) {
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
    public void removeDatasetFromCSW(int serviceID, int datasetID) {
        final Metadata metadata = dsl.select().from(METADATA).where(METADATA.DATASET_ID.eq(datasetID)).fetchOneInto(Metadata.class);
        if (metadata != null) {
            dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).and(METADATA_X_CSW.METADATA_ID.eq(metadata.getId())).execute();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeAllDatasetFromCSW(int serviceID) {
        dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).execute();
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

        //add default filter
        if (filterMap == null) {
           filterMap = new HashMap<>();
        }

        // build SQL query
        Select query = null;
        for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
            final Condition cond = buidCondition(entry.getKey(), entry.getValue());
            if (cond != null) {
                if (query == null) {
                    query = dsl.select(DATASET.fields()).from(DATASET)
                               .leftOuterJoin(CSTL_USER).on(CSTL_USER.ID.eq(DATASET.OWNER)) // dataset -> cstl_user
                               .where(cond);
                } else {
                    query = ((SelectConditionStep) query).and(cond);
                }
            }
        }

        // add sort
        if(sortEntry != null) {
            final SortField f;
            if ("title".equals(sortEntry.getKey()) || "name".equals(sortEntry.getKey())){
                f = "ASC".equals(sortEntry.getValue()) ? DATASET.IDENTIFIER.lower().asc() : DATASET.IDENTIFIER.lower().desc();
            } else if ("owner_login".equals(sortEntry.getKey())) {
                f = "ASC".equals(sortEntry.getValue()) ? CSTL_USER.LOGIN.asc() : CSTL_USER.LOGIN.desc();
            } else {
                f = "ASC".equals(sortEntry.getValue()) ? DATASET.DATE.asc() : DATASET.DATE.desc();
            }
            if (query == null) {
                query = dsl.select(DATASET.fields()).from(DATASET)
                           .leftOuterJoin(CSTL_USER).on(CSTL_USER.ID.eq(DATASET.OWNER)) // dataset -> cstl_user
                           .orderBy(f);
            } else {
                query = ((SelectConditionStep)query).orderBy(f);
            }
        }

        final Map.Entry<Integer,List<DataSet>> result;
        if (query == null) {
            final int count = dsl.selectCount().from(DATASET).fetchOne(0,int.class);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertDatasetListToDto(dsl.select(DATASET.fields())
                                               .from(DATASET)
                                               .limit(rowsPerPage)
                                               .offset((pageNumber - 1) * rowsPerPage)
                                               .fetchInto(Dataset.class)));
        } else {
            final int count = dsl.fetchCount(query);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertDatasetListToDto(((SelectLimitStep) query).limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage)
                                             .fetchInto(Dataset.class)));
        }
        return result;
    }

    public Condition buidCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return DATASET.OWNER.equal((Integer) value);
        } else if ("id".equals(key)) {
            return DATASET.ID.equal((Integer) value);
        } else if ("term".equals(key)) {
            String likeExpr = '%' + (String)value + '%';
             Field<Integer> countNamedData = countNamedData(DATASET.ID, likeExpr);
            return DATASET.IDENTIFIER.likeIgnoreCase(likeExpr).or(CSTL_USER.LOGIN.likeIgnoreCase(likeExpr)).or(countNamedData.greaterThan(0));
        } else if ("period".equals(key)) {
            return DATASET.DATE.greaterOrEqual((Long) value);

        } else if ("type".equals(key)) {
            return DATASET.TYPE.eq((String) value);

        } else if ("excludeEmpty".equals(key)) {
            if ((boolean)value == true) {
                Field<Integer> countData = countData(DATASET.ID);
                return countData.greaterThan(0);
            } else {
                return DSL.trueCondition();
            }

        } else if ("hasVectorData".equals(key)) {
            Field<Integer> countVectorData = countDataOfType(DATASET.ID, "VECTOR");
            return (boolean)value ? countVectorData.greaterThan(0) : countVectorData.eq(0);

        } else if ("hasCoverageData".equals(key)) {
            Field<Integer> countCoverageData = countDataOfType(DATASET.ID, "COVERAGE");
            return (boolean)value ? countCoverageData.greaterThan(0) : countCoverageData.eq(0);

        } else if ("hasLayerData".equals(key)) {
            Field<Integer> countLayerData = countLayerData(DATASET.ID);
            return (boolean)value ? countLayerData.greaterThan(0) : countLayerData.eq(0);

        } else if ("hasSensorData".equals(key)) {
            Field<Integer> countSensorData = countSensorData(DATASET.ID);
            return (boolean)value ? countSensorData.greaterThan(0) : countSensorData.eq(0);

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
