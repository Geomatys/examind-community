/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
import static org.constellation.database.api.jooq.Tables.METADATA;
import static org.constellation.database.api.jooq.Tables.METADATA_BBOX;
import static org.constellation.database.api.jooq.Tables.METADATA_X_CSW;
import static org.constellation.database.api.jooq.Tables.INTERNAL_METADATA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static org.constellation.database.api.jooq.Tables.SERVICE;

import org.constellation.dto.metadata.MetadataComplete;
import org.constellation.dto.metadata.Metadata;
import org.constellation.dto.metadata.MetadataBbox;
import org.constellation.database.api.jooq.tables.pojos.MetadataXCsw;
import org.constellation.database.api.jooq.tables.records.MetadataBboxRecord;
import org.constellation.database.api.jooq.tables.records.MetadataRecord;
import org.constellation.database.api.jooq.tables.records.MetadataXCswRecord;
import org.constellation.repository.MetadataRepository;
import org.jooq.AggregateFunction;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitStep;
import org.jooq.SortField;
import org.jooq.UpdateSetFirstStep;
import org.jooq.impl.DSL;
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
public class JooqMetadataRepository extends AbstractJooqRespository<MetadataRecord, org.constellation.database.api.jooq.tables.pojos.Metadata> implements MetadataRepository {

    public JooqMetadataRepository() {
        super(org.constellation.database.api.jooq.tables.pojos.Metadata.class, METADATA);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Metadata update(MetadataComplete metadata) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.METADATA_ID, metadata.getMetadataId());
        if (metadata.getDatasetId() != null) update.set(METADATA.DATASET_ID, metadata.getDatasetId());
        if (metadata.getDataId() != null) update.set(METADATA.DATA_ID, metadata.getDataId());
        if (metadata.getServiceId() != null) update.set(METADATA.SERVICE_ID, metadata.getServiceId());
        if (metadata.getMapContextId()!= null) update.set(METADATA.MAP_CONTEXT_ID, metadata.getMapContextId());
        if (metadata.getMdCompletion() != null) update.set(METADATA.MD_COMPLETION, metadata.getMdCompletion());
        if (metadata.getParentIdentifier() != null) update.set(METADATA.PARENT_IDENTIFIER, metadata.getParentIdentifier());
        if (metadata.getProviderId() != null) update.set(METADATA.PROVIDER_ID, metadata.getProviderId());
        if (metadata.getProfile()!= null) update.set(METADATA.TYPE, metadata.getType());

        update.set(METADATA.OWNER, metadata.getOwner());
        update.set(METADATA.DATESTAMP, metadata.getDatestamp());
        update.set(METADATA.DATE_CREATION, metadata.getDateCreation());
        update.set(METADATA.LEVEL, metadata.getLevel());

        if (metadata.getIsPublished() != null) update.set(METADATA.IS_PUBLISHED, metadata.getIsPublished());
        else update.set(METADATA.IS_PUBLISHED, false);

        if (metadata.getIsValidated() != null) update.set(METADATA.IS_VALIDATED, metadata.getIsValidated());
        else update.set(METADATA.IS_VALIDATED, false);

        if (metadata.getIsHidden()!= null) update.set(METADATA.IS_HIDDEN, metadata.getIsHidden());
        else update.set(METADATA.IS_HIDDEN, false);

        if (metadata.getValidationRequired() != null) update.set(METADATA.VALIDATION_REQUIRED, metadata.getValidationRequired());
        update.set(METADATA.COMMENT, metadata.getComment());
        update.set(METADATA.VALIDATED_STATE, metadata.getValidatedState());
        update.set(METADATA.PROFILE, metadata.getProfile());
        update.set(METADATA.TITLE, metadata.getTitle());
        update.set(METADATA.IS_SHARED, metadata.getIsShared());
        update.set(METADATA.RESUME, metadata.getResume()).where(METADATA.ID.eq(metadata.getId())).execute();

        updateBboxes(metadata.getId(), metadata.getBboxes());

        return metadata;
    }

    private void updateBboxes(int metadataID, List<MetadataBbox> bboxes) {
        dsl.delete(METADATA_BBOX).where(METADATA_BBOX.METADATA_ID.eq(metadataID)).execute();
        for (MetadataBbox bbox : bboxes) {
            MetadataBboxRecord record = dsl.newRecord(METADATA_BBOX);
            record.setMetadataId(metadataID);
            record.setEast(bbox.getEast());
            record.setWest(bbox.getWest());
            record.setNorth(bbox.getNorth());
            record.setSouth(bbox.getSouth());
            record.store();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(MetadataComplete metadata) {
        MetadataRecord metadataRecord = dsl.newRecord(METADATA);
        metadataRecord.setMetadataId(metadata.getMetadataId());
        if (metadata.getDataId() != null) metadataRecord.setDataId(metadata.getDataId());
        if (metadata.getDatasetId() != null) metadataRecord.setDatasetId(metadata.getDatasetId());
        if (metadata.getServiceId() != null) metadataRecord.setServiceId(metadata.getServiceId());
        if (metadata.getMapContextId() != null) metadataRecord.setMapContextId(metadata.getMapContextId());
        if (metadata.getMdCompletion() != null) metadataRecord.setMdCompletion(metadata.getMdCompletion());
        if (metadata.getParentIdentifier() != null) metadataRecord.setParentIdentifier(metadata.getParentIdentifier());
        if (metadata.getProviderId() != null) metadataRecord.setProviderId(metadata.getProviderId());
        metadataRecord.setType(metadata.getType());
        metadataRecord.setDateCreation(metadata.getDateCreation());
        metadataRecord.setDatestamp(metadata.getDatestamp());
        metadataRecord.setLevel(metadata.getLevel());
        metadataRecord.setOwner(metadata.getOwner());
        metadataRecord.setProfile(metadata.getProfile());
        metadataRecord.setTitle(metadata.getTitle());
        metadataRecord.setResume(metadata.getResume());
        metadataRecord.setComment(metadata.getComment());
        metadataRecord.setValidatedState(metadata.getValidatedState());
        if (metadata.getValidationRequired() != null) metadataRecord.setValidationRequired(metadata.getValidationRequired());

        if (metadata.getIsPublished() != null) metadataRecord.setIsPublished(metadata.getIsPublished());
        else metadataRecord.setIsPublished(false); //default

        if (metadata.getIsValidated() != null) metadataRecord.setIsValidated(metadata.getIsValidated());
        else metadataRecord.setIsValidated(false); //default

        if (metadata.getIsHidden()!= null) metadataRecord.setIsHidden(metadata.getIsHidden());
        else metadataRecord.setIsHidden(false); //default

        metadataRecord.setIsShared(metadata.getIsShared());
        metadataRecord.store();

        updateBboxes(metadataRecord.getId(), metadata.getBboxes());

        return metadataRecord.getId();
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(METADATA)
                .where(METADATA.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<Metadata> findAll() {
        return convertListToDto(dsl.select().from(METADATA).fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public Metadata findByMetadataId(String metadataId) {
        return convertToDto(
                dsl.select().from(METADATA).where(METADATA.METADATA_ID.eq(metadataId)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public Metadata findById(int id) {
        return convertToDto(
                dsl.select().from(METADATA).where(METADATA.ID.eq(id)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public List<Metadata> findByDataId(int dataId) {
        return convertListToDto(dsl.select().from(METADATA).where(METADATA.DATA_ID.eq(dataId)).fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public Metadata findByDatasetId(int datasetId) {
        return convertToDto(
            dsl.select().from(METADATA).where(METADATA.DATASET_ID.eq(datasetId)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public Metadata findByServiceId(int serviceId) {
        return convertToDto(
            dsl.select().from(METADATA).where(METADATA.SERVICE_ID.eq(serviceId)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public Metadata findByMapContextId(int mapContextId) {
        return convertToDto(
            dsl.select().from(METADATA).where(METADATA.MAP_CONTEXT_ID.eq(mapContextId)).fetchOneInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public void addMetadataToCSW(final String metadataID, final int serviceID) {
        final Integer metadata =
                dsl.select(METADATA.ID).from(METADATA).where(METADATA.METADATA_ID.eq(metadataID)).fetchOneInto(Integer.class);
        if (metadata != null) {
            final MetadataXCsw dxc = dsl.select().from(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).and(METADATA_X_CSW.METADATA_ID.eq(metadata)).fetchOneInto(MetadataXCsw.class);
            if (dxc == null) {
                MetadataXCswRecord newRecord = dsl.newRecord(METADATA_X_CSW);
                newRecord.setCswId(serviceID);
                newRecord.setMetadataId(metadata);
                newRecord.store();
            }
        }
    }

    @Override
    public void removeDataFromCSW(final String metadataID, final int serviceID) {
        final Integer metadata = dsl.select(METADATA.ID).from(METADATA).where(METADATA.METADATA_ID.eq(metadataID)).fetchOneInto(Integer.class);
        if (metadata != null) {
            dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceID)).and(METADATA_X_CSW.METADATA_ID.eq(metadata)).execute();
        }
    }

    @Override
    public List<Metadata> findByCswId(Integer id) {
        return convertListToDto(dsl.select(METADATA.fields()).from(METADATA, METADATA_X_CSW)
                  .where(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                  .and(METADATA_X_CSW.CSW_ID.eq(id))
                  .fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public List<String> findMetadataIDByCswId(final Integer id, final boolean includeService, final boolean onlyPublished,
            final String type, final Boolean hidden) {
        SelectConditionStep<Record1<String>> query =
               dsl.select(METADATA.METADATA_ID).from(METADATA, METADATA_X_CSW)
                  .where(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                  .and(METADATA_X_CSW.CSW_ID.eq(id));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }
        if (type != null) {
            query = query.and(METADATA.TYPE.eq(type));
        }
        if (hidden != null) {
            query = query.and(METADATA.IS_HIDDEN.eq(hidden));
        }
        return query.fetchInto(String.class);
    }

    @Override
    public List<String> findMetadataIDByProviderId(final Integer providerId, final boolean includeService, final boolean onlyPublished,
            final String type, final Boolean hidden) {
        SelectConditionStep<Record1<String>> query =
               dsl.select(METADATA.METADATA_ID).from(METADATA)
                  .where(METADATA.PROVIDER_ID.eq(providerId));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }
        if (type != null) {
            query = query.and(METADATA.TYPE.eq(type));
        }
        if (hidden != null) {
            query = query.and(METADATA.IS_HIDDEN.eq(hidden));
        }
        return query.fetchInto(String.class);
    }

    @Override
    public int countMetadataByCswId(final Integer id, final boolean includeService, final boolean onlyPublished, final String type,
            final Boolean hidden) {
        SelectConditionStep<Record1<String>> query =
               dsl.select(METADATA.METADATA_ID).from(METADATA, METADATA_X_CSW)
                  .where(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                  .and(METADATA_X_CSW.CSW_ID.eq(id));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }
        if (type != null) {
            query = query.and(METADATA.TYPE.eq(type));
        }
        if (hidden != null) {
            query = query.and(METADATA.IS_HIDDEN.eq(hidden));
        }
        return query.fetchCount();
    }

    @Override
    public int countMetadataByProviderId(final Integer id, final boolean includeService, final boolean onlyPublished, final String type,
            final Boolean hidden) {
        SelectConditionStep<Record1<String>> query =
               dsl.select(METADATA.METADATA_ID).from(METADATA)
                  .where(METADATA.PROVIDER_ID.eq(id));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }
        if (type != null) {
            query = query.and(METADATA.TYPE.eq(type));
        }
        if (hidden != null) {
            query = query.and(METADATA.IS_HIDDEN.eq(hidden));
        }
        return query.fetchCount();
    }

    @Override
    public List<String> findMetadataID(final boolean includeService, final boolean onlyPublished, final Integer providerId, final String type) {
        SelectJoinStep<Record1<String>> query =  dsl.select(METADATA.METADATA_ID).from(METADATA);
        if (includeService && !onlyPublished && providerId == null && type == null) {
            return query.fetchInto(String.class);
        } else {
            SelectConditionStep<Record1<String>> filterQuery = null;
            if (!includeService) {
                filterQuery = query.where(METADATA.SERVICE_ID.isNull());
            }
            if (onlyPublished) {
                if (filterQuery == null) {
                    filterQuery = query.where(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
                } else {
                    filterQuery = filterQuery.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
                }
            }
            if (providerId != null) {
                if (filterQuery == null) {
                    filterQuery = query.where(METADATA.PROVIDER_ID.eq(providerId));
                } else {
                    filterQuery = filterQuery.and(METADATA.PROVIDER_ID.eq(providerId));
                }
            }
            if (type != null) {
                if (filterQuery == null) {
                    filterQuery = query.where(METADATA.TYPE.eq(type));
                } else {
                    filterQuery = filterQuery.and(METADATA.TYPE.eq(type));
                }
            }
            return filterQuery.fetchInto(String.class);
        }
    }

    @Override
    public int countMetadata(final boolean includeService, final boolean onlyPublished, final Integer providerId, final String type) {
        SelectJoinStep<Record1<String>> query =  dsl.select(METADATA.METADATA_ID).from(METADATA);
        if (includeService && !onlyPublished && providerId == null && type == null) {
            return query.fetchCount();
        } else {
            SelectConditionStep<Record1<String>> filterQuery = null;
            if (!includeService) {
                filterQuery = query.where(METADATA.SERVICE_ID.isNull());
            }
            if (onlyPublished) {
                if (filterQuery == null) {
                    filterQuery = query.where(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
                } else {
                    filterQuery = filterQuery.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
                }
            }
            if (providerId != null) {
                if (filterQuery == null) {
                    filterQuery = query.where(METADATA.PROVIDER_ID.eq(providerId));
                } else {
                    filterQuery = filterQuery.and(METADATA.PROVIDER_ID.eq(providerId));
                }
            }
            if (type != null) {
                if (filterQuery == null) {
                    filterQuery = query.where(METADATA.TYPE.eq(type));
                } else {
                    filterQuery = filterQuery.and(METADATA.TYPE.eq(type));
                }
            }
            return filterQuery.fetchCount();
        }
    }

    @Override
    public List<Metadata> findAll(final boolean includeService, final boolean onlyPublished) {
        SelectJoinStep<Record> query =  dsl.select(METADATA.fields()).from(METADATA);
        if (includeService && !onlyPublished) {
            return convertListToDto(query.fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
        }
        SelectConditionStep<Record> filterQuery = null;
        if (!includeService) {
            filterQuery = query.where(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            if (filterQuery == null) {
                filterQuery = query.where(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
            } else {
                filterQuery = filterQuery.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
            }
        }
        return convertListToDto(filterQuery.fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public List<Metadata> findByProviderId(final Integer providerId, final String type) {
        SelectConditionStep<Record> filterQuery = dsl.select(METADATA.fields()).from(METADATA).where(METADATA.PROVIDER_ID.eq(providerId));
        if (type != null) {
            filterQuery = filterQuery.and(METADATA.TYPE.eq(type));
        }
        return convertListToDto(filterQuery.fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class));
    }

    @Override
    public boolean isLinkedMetadata(Integer metadataID, Integer cswID) {
        return dsl.select(METADATA.fields()).from(METADATA, METADATA_X_CSW)
                  .where(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                  .and(METADATA_X_CSW.CSW_ID.eq(cswID))
                  .and(METADATA_X_CSW.METADATA_ID.eq(metadataID)).fetchOneInto(Metadata.class) != null;
    }

    @Override
    public boolean isLinkedMetadata(String metadataID, String cswID) {
        return dsl.select(METADATA.fields()).from(METADATA, METADATA_X_CSW, SERVICE)
                  .where(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                  .and(METADATA_X_CSW.CSW_ID.eq(SERVICE.ID))
                  .and(SERVICE.IDENTIFIER.eq(cswID))
                  .and(SERVICE.TYPE.eq("csw"))
                  .and(METADATA.METADATA_ID.eq(metadataID)).fetchOneInto(Metadata.class) != null;
    }

    @Override
    public boolean isLinkedMetadata(String metadataID, String cswID, final boolean includeService, final boolean onlyPublished) {
        SelectConditionStep query = dsl.select(METADATA.ID).from(METADATA, METADATA_X_CSW, SERVICE)
                  .where(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                  .and(METADATA_X_CSW.CSW_ID.eq(SERVICE.ID))
                  .and(SERVICE.IDENTIFIER.eq(cswID))
                  .and(SERVICE.TYPE.eq("csw"))
                  .and(METADATA.IS_HIDDEN.eq(false))
                  .and(METADATA.METADATA_ID.eq(metadataID));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }

        return query.fetchOne() != null;
    }

    @Override
    public boolean isLinkedMetadata(String metadataID, int providerID, final boolean includeService, final boolean onlyPublished) {
        SelectConditionStep query = dsl.select(METADATA.ID).from(METADATA)
                  .where(METADATA.METADATA_ID.eq(metadataID))
                  .and(METADATA.IS_HIDDEN.eq(false))
                  .and(METADATA.PROVIDER_ID.eq(providerID));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }

        return query.fetchOne() != null;
    }

    /**
     * Returns a map that contains id of metadata as key and the title of metadata as value.
     * the filterMap passed in arguments is optional and can contains one or multiple filter on each field.
     * This method is used for selection of rows to check the state when using server pagination,
     * the pagination should not be included in this result to keep a list of all existing ids.
     * @param filterMap
     * @return
     */
    @Override
    public List<Map<String, Object>> filterAndGetWithoutPagination(final Map<String,Object> filterMap) {
        Select query = null;
        Collection<Field<?>> fields = new ArrayList<>();
        Collections.addAll(fields,METADATA.ID,METADATA.TITLE,METADATA.PROFILE);
        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition cond = buidCondition(entry.getKey(), entry.getValue());
                if (cond != null) {
                    if (query == null) {
                        query = dsl.select(fields).from(METADATA).where(cond);
                    } else {
                        query = ((SelectConditionStep) query).and(cond);
                    }
                }
            }
        }
        if(query == null) {
            return dsl.select(fields).from(METADATA).fetchMaps();
        }else {
            return query.fetchMaps();
        }
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
    public Map.Entry<Integer, List<Metadata>> filterAndGet(Map<String,Object> filterMap,
                                       final Map.Entry<String,String> sortEntry,
                                       final int pageNumber,
                                       final int rowsPerPage) {
        Collection<Field<?>> fields = new ArrayList<>();
        Collections.addAll(fields,METADATA.ID,METADATA.METADATA_ID,
                METADATA.TITLE,METADATA.PROFILE,METADATA.OWNER,METADATA.DATESTAMP,
                METADATA.DATE_CREATION,METADATA.MD_COMPLETION,METADATA.LEVEL,
                METADATA.IS_VALIDATED,METADATA.IS_PUBLISHED,METADATA.RESUME,
                METADATA.VALIDATION_REQUIRED,METADATA.COMMENT,METADATA.DATA_ID,
                METADATA.DATASET_ID, METADATA.TYPE, METADATA.IS_SHARED);

        //add default filter
        if (filterMap == null) {
           filterMap = new HashMap<>();
        }
        if (!filterMap.containsKey("hidden")) {
            filterMap.put("hidden", false);
        }

        Select query = null;
        for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
            final Condition cond = buidCondition(entry.getKey(), entry.getValue());
            if (cond != null) {
                if (query == null) {
                    query = dsl.select(fields).from(METADATA).where(cond);
                } else {
                    query = ((SelectConditionStep) query).and(cond);
                }
            }
        }

        if(sortEntry != null) {
            final SortField f;
            if("title".equals(sortEntry.getKey())){
                f = "ASC".equals(sortEntry.getValue()) ? METADATA.TITLE.lower().asc() : METADATA.TITLE.lower().desc();
            }else if("date_creation".equals(sortEntry.getKey())){
                f = "ASC".equals(sortEntry.getValue()) ? METADATA.DATE_CREATION.asc() : METADATA.DATE_CREATION.desc();
            }else { //default sorting on date stamp
                f = "ASC".equals(sortEntry.getValue()) ? METADATA.DATESTAMP.asc() : METADATA.DATESTAMP.desc();
            }
            if(query == null) {
                query = dsl.select(fields).from(METADATA).orderBy(f);
            }else {
                query = ((SelectConditionStep)query).orderBy(f);
            }
        }

        final Map.Entry<Integer,List<Metadata>> result;
        if(query == null) { //means there are no sorting and no filters
            final int count = dsl.selectCount().from(METADATA).fetchOne(0,int.class);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertListToDto(dsl.select(fields)
                                        .from(METADATA)
                                        .limit(rowsPerPage).offset((pageNumber - 1) * rowsPerPage)
                                        .fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class)));
        }else {
            final int count = dsl.fetchCount(query);
            result = new AbstractMap.SimpleImmutableEntry<>(count,
                    convertListToDto(((SelectLimitStep) query)
                                       .limit(rowsPerPage)
                                       .offset((pageNumber - 1) * rowsPerPage)
                                       .fetchInto(org.constellation.database.api.jooq.tables.pojos.Metadata.class)));
        }
        return result;
    }

    public Condition buidCondition(String key, Object value) {
        if ("owner".equals(key)) {
            return METADATA.OWNER.equal((Integer) value);
        } else if ("data".equals(key)) {
            return METADATA.DATA_ID.equal((Integer) value);
        } else if ("dataset".equals(key)) {
            return METADATA.DATASET_ID.equal((Integer) value);
        } else if ("profile".equals(key)) {
            return METADATA.PROFILE.equal((String) value);
        } else if ("validated".equals(key)) {
            return METADATA.IS_VALIDATED.equal((Boolean) value);
        } else if ("validation_required".equals(key)) {
            return METADATA.VALIDATION_REQUIRED.equal((String) value);
        } else if ("isShared".equals(key)) {
            return METADATA.IS_SHARED.equal((Boolean) value);
        } else if ("hidden".equals(key)) {
            return METADATA.IS_HIDDEN.equal((Boolean) value);
        } else if ("id".equals(key)) {
            return METADATA.ID.equal((Integer) value);
        } else if ("identifier".equals(key)) {
            return METADATA.METADATA_ID.equal((String) value);
        } else if ("parent".equals(key)) {
            return METADATA.PARENT_IDENTIFIER.equal((Integer) value).or(METADATA.ID.equal((Integer) value));
        } else if ("published".equals(key)) {
            return METADATA.IS_PUBLISHED.equal((Boolean) value);
        } else if ("level".equals(key)) {
            return METADATA.LEVEL.equal((String) value);
        } else if ("term".equals(key)) {
            return METADATA.TITLE.likeIgnoreCase("%" + value + "%").or(METADATA.RESUME.likeIgnoreCase("%" + value + "%"));
        } else if ("period".equals(key)) {
            return METADATA.DATESTAMP.greaterOrEqual((Long) value);
        } else if ("type".equals(key)) {
            return METADATA.TYPE.eq((String) value);
        } else if ("name".equals(key)) {
            Condition namesCond = null;
            List<String> names = (List<String>) value;
            for (String name : names) {
                if (namesCond == null) {
                    namesCond = METADATA.PROFILE.eq(name);
                } else {
                    namesCond = namesCond.or(METADATA.PROFILE.eq(name));
                }
            }
            return namesCond;
        } else if ("OR".equals(key)) {
            List<Entry<String, Object>> values =  (List<Entry<String, Object>>) value;
            Condition c = null;
            for (Entry<String, Object> e: values) {
                Condition c2 = buidCondition(e.getKey(), e.getValue());
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
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(final Integer id) {
        dsl.delete(INTERNAL_METADATA).where(INTERNAL_METADATA.ID.eq(id)).execute();
        dsl.delete(METADATA_BBOX).where(METADATA_BBOX.METADATA_ID.eq(id)).execute();
        return dsl.delete(METADATA).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        dsl.delete(INTERNAL_METADATA).execute();
        dsl.delete(METADATA_BBOX).execute();
        return dsl.delete(METADATA).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeOwner(final int id, final int owner) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.OWNER, owner).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeValidation(int id, boolean validated) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.IS_VALIDATED, validated).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changePublication(int id, boolean published) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.IS_PUBLISHED, published).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeHidden(int id, boolean published) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.IS_HIDDEN, published).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeProfile(int id, String newProfile) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.PROFILE, newProfile).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void changeSharedProperty(int id, boolean shared) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.IS_SHARED, shared).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    public List<MetadataBbox> getBboxes(int id) {
        return convertListToDtoBbox(dsl.select()
                                       .from(METADATA_BBOX)
                                       .where(METADATA_BBOX.METADATA_ID.eq(id))
                                       .fetchInto(org.constellation.database.api.jooq.tables.pojos.MetadataBbox.class));
    }

    @Override
    public Map<String,Integer> getProfilesCount(final Map<String,Object> filterMap) {

        AggregateFunction<Integer> count = DSL.count(METADATA.PROFILE);

        SelectJoinStep select = dsl.select(METADATA.PROFILE, count ).from(METADATA);
        SelectConditionStep cond = null;

        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition condFilter = buidCondition(entry.getKey(), entry.getValue());
                if (condFilter != null) {
                    if (cond != null) {
                        cond = cond.and(condFilter);
                    } else if (select != null){
                        cond = select.where(condFilter);
                        select = null;
                    }
                }
            }
        }

        if (select != null) {
            return select.groupBy(METADATA.PROFILE).orderBy(count.desc()).fetchMap(METADATA.PROFILE, count);
        } else if (cond != null){
            return cond.groupBy(METADATA.PROFILE).orderBy(count.desc()).fetchMap(METADATA.PROFILE, count);
        // should never happen
        } else {
            throw new IllegalStateException("SQL error");
        }
    }

    @Override
    public int countInCompletionRange(final Map<String,Object> filterMap, final int minCompletion, final int maxCompletion) {
        SelectConditionStep cond = dsl.select().from(METADATA).where(METADATA.MD_COMPLETION.between(minCompletion, maxCompletion));
        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition condFilter = buidCondition(entry.getKey(), entry.getValue());
                if (condFilter != null) {
                    cond = cond.and(condFilter);
                }
            }
        }
        return dsl.fetchCount(cond);
    }

    @Override
    public int countTotalMetadata(final Map<String,Object> filterMap) {
        SelectJoinStep select = dsl.select().from(METADATA);
        SelectConditionStep cond = null;

        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition condFilter = buidCondition(entry.getKey(), entry.getValue());
                if (condFilter != null) {
                    if (cond != null) {
                        cond = cond.and(condFilter);
                    } else if (select != null){
                        cond = select.where(condFilter);
                        select = null;
                    }
                }
            }
        }

        if (select != null) {
            return dsl.fetchCount(select);
        } else if (cond != null) {
            return dsl.fetchCount(cond);
        // should never happen
        } else {
            throw new IllegalStateException("SQL error");
        }
    }

    @Override
    public int countValidated(boolean status, final Map<String,Object> filterMap) {
        SelectConditionStep cond = dsl.select().from(METADATA).where(METADATA.IS_VALIDATED.equal(status));
        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition condFilter = buidCondition(entry.getKey(), entry.getValue());
                if (condFilter != null) {
                    cond = cond.and(condFilter);
                }
            }
        }
        return dsl.fetchCount(cond);
    }

    @Override
    public int countPublished(boolean status, final Map<String,Object> filterMap) {
        SelectConditionStep cond = dsl.select().from(METADATA).where(METADATA.IS_PUBLISHED.equal(status));
        if (filterMap != null) {
            for (final Map.Entry<String, Object> entry : filterMap.entrySet()) {
                final Condition condFilter = buidCondition(entry.getKey(), entry.getValue());
                if (condFilter != null) {
                    cond = cond.and(condFilter);
                }
            }
        }
        return dsl.fetchCount(cond);
    }

    @Override
    @Transactional
    public void setValidationRequired(int id, String state, String validationState) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.VALIDATION_REQUIRED, state)
              .set(METADATA.VALIDATED_STATE, validationState).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void denyValidation(int id, String comment) {
        UpdateSetFirstStep<MetadataRecord> update = dsl.update(METADATA);
        update.set(METADATA.VALIDATION_REQUIRED, "REJECTED")
              .set(METADATA.COMMENT, comment).where(METADATA.ID.eq(id)).execute();
    }

    @Override
    public boolean existInternalMetadata(String metadataID, boolean includeService, boolean onlyPublished, final Integer providerID) {
        SelectConditionStep query = dsl.select(METADATA.ID).from(METADATA)
                                       .where(METADATA.METADATA_ID.eq(metadataID));

        if (!includeService) {
            query = query.and(METADATA.SERVICE_ID.isNull());
        }
        if (onlyPublished) {
            query = query.and(METADATA.IS_PUBLISHED.eq(Boolean.TRUE));
        }
        if (providerID != null) {
            query = query.and(METADATA.PROVIDER_ID.eq(providerID));
        }

        return query.fetchOne() != null;
    }

    @Override
    public boolean existMetadataTitle(final String title) {
        SelectConditionStep query = dsl.select(METADATA.ID).from(METADATA)
                                       .where(METADATA.TITLE.eq(title));


        return dsl.fetchCount(query) != 0;
    }

    @Override
    public List<Integer> findAllIds() {
        return dsl.select(METADATA.ID).from(METADATA).fetchInto(Integer.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkMetadataData(int metadataID, int dataId) {
        dsl.update(METADATA).set(METADATA.DATA_ID, dataId).where(METADATA.ID.eq(metadataID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkMetadataData(int metadataID) {
        Integer nullInt = null;
        dsl.update(METADATA).set(METADATA.DATA_ID, nullInt).where(METADATA.ID.eq(metadataID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkMetadataMapContext(int metadataID, int contextId) {
        dsl.update(METADATA).set(METADATA.MAP_CONTEXT_ID, contextId).where(METADATA.ID.eq(metadataID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkMetadataMapContext(int metadataID) {
        Integer nullInt = null;
        dsl.update(METADATA).set(METADATA.MAP_CONTEXT_ID, nullInt).where(METADATA.ID.eq(metadataID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkMetadataDataset(int metadataID, int datasetId) {
        dsl.update(METADATA).set(METADATA.DATASET_ID, datasetId).where(METADATA.ID.eq(metadataID)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void unlinkMetadataDataset(int metadataID) {
        Integer nullInt = null;
        dsl.update(METADATA).set(METADATA.DATASET_ID, nullInt).where(METADATA.ID.eq(metadataID)).execute();
    }

    private Metadata convertToDto(org.constellation.database.api.jooq.tables.pojos.Metadata dao) {
        if (dao != null) {
            return new Metadata(dao.getId(),
                    dao.getMetadataId(),
                    dao.getDataId(),
                    dao.getDatasetId(),
                    dao.getServiceId(),
                    dao.getMdCompletion(),
                    dao.getOwner(),
                    dao.getDatestamp(),
                    dao.getDateCreation(),
                    dao.getTitle(),
                    dao.getProfile(),
                    dao.getParentIdentifier(),
                    dao.getIsValidated(),
                    dao.getIsPublished(),
                    dao.getLevel(),
                    dao.getResume(),
                    dao.getValidationRequired(),
                    dao.getValidatedState(),
                    dao.getComment(),
                    dao.getProviderId(),
                    dao.getMapContextId(),
                    dao.getType(),
                    dao.getIsShared(),
                    dao.getIsHidden());
        }
        return null;
    }

    private List<Metadata> convertListToDto(List<org.constellation.database.api.jooq.tables.pojos.Metadata> daos) {
        List<Metadata> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.Metadata dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    private MetadataBbox convertToDto(org.constellation.database.api.jooq.tables.pojos.MetadataBbox dao) {
        if (dao != null) {
            return new MetadataBbox(dao.getMetadataId(),
                    dao.getEast(),
                    dao.getWest(),
                    dao.getNorth(),
                    dao.getSouth());
        }
        return null;
    }

    private List<MetadataBbox> convertListToDtoBbox(List<org.constellation.database.api.jooq.tables.pojos.MetadataBbox> daos) {
        List<MetadataBbox> results = new ArrayList<>();
        for (org.constellation.database.api.jooq.tables.pojos.MetadataBbox dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }
}
