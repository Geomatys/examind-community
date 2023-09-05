/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static com.examind.database.api.jooq.Tables.DATASOURCE_PATH_STORE;
import static com.examind.database.api.jooq.Tables.DATASOURCE_PATH;
import static com.examind.database.api.jooq.Tables.DATASOURCE;
import static com.examind.database.api.jooq.Tables.DATASOURCE_STORE;
import static com.examind.database.api.jooq.Tables.DATASOURCE_SELECTED_PATH;
import com.examind.database.api.jooq.tables.pojos.DatasourcePathStore;
import com.examind.database.api.jooq.tables.pojos.DatasourceSelectedPath;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourcePath;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.DataSourcePathComplete;
import com.examind.database.api.jooq.tables.pojos.Datasource;
import com.examind.database.api.jooq.tables.pojos.DatasourcePath;
import com.examind.database.api.jooq.tables.pojos.DatasourceStore;
import com.examind.database.api.jooq.tables.records.DatasourcePathRecord;
import com.examind.database.api.jooq.tables.records.DatasourceRecord;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IDatasourceBusiness.PathStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.constellation.repository.DatasourceRepository;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SelectConditionStep;
import org.springframework.context.annotation.DependsOn;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
@DependsOn("database-initer")
public class JooqDatasourceRepository extends AbstractJooqRespository<DatasourceRecord, Datasource> implements DatasourceRepository {

    public JooqDatasourceRepository() {
        super(Datasource.class, DATASOURCE);
    }

    @Override
    public DataSource findById(int id) {
        return convertToDto(dsl.select().from(DATASOURCE).where(DATASOURCE.ID.eq(id)).fetchOneInto(Datasource.class));
    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.selectCount().from(DATASOURCE)
                .where(DATASOURCE.ID.eq(id))
                .fetchOne(0, Integer.class) > 0;
    }

    @Override
    public List<DataSource> search(String url, String storeId, String format, String userName, String pwd) {
        SelectConditionStep<Record> query = dsl.select().from(DATASOURCE)
                                               .where(DATASOURCE.URL.eq(url))
                                               .and(DATASOURCE.PERMANENT.isTrue());
        if (storeId != null) {
             if (storeId.equals("NULL")){
                query = query.and(DATASOURCE.STORE_ID.isNull());
            } else {
                query = query.and(DATASOURCE.STORE_ID.eq(storeId));
             }
        }
        if (format != null) {
            if (format.equals("NULL")){
                query = query.and(DATASOURCE.FORMAT.isNull());
            } else {
                query = query.and(DATASOURCE.FORMAT.eq(format));
            }
        }
        if (userName != null) {
            query = query.and(DATASOURCE.USERNAME.eq(userName));
        }
        if (pwd != null) {
            query = query.and(DATASOURCE.PWD.eq(pwd));
        }
        return convertListToDto(query.fetchInto(Datasource.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(DataSource datasource) {
        DatasourceRecord newRecord = dsl.newRecord(DATASOURCE);
        newRecord.from(datasource);
        newRecord.store();
        return newRecord.into(Datasource.class).getId();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        dsl.delete(DATASOURCE_SELECTED_PATH).where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id)).execute();
        dsl.delete(DATASOURCE_PATH_STORE).where(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(id)).execute();
        dsl.delete(DATASOURCE_PATH).where(DATASOURCE_PATH.DATASOURCE_ID.eq(id)).execute();
        dsl.delete(DATASOURCE_STORE).where(DATASOURCE_STORE.DATASOURCE_ID.eq(id)).execute();
        return dsl.delete(DATASOURCE).where(DATASOURCE.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        List<DataSource> all = findAll();
        int cpt = 0;
        for (DataSource ds : all) {
            cpt = cpt + delete(ds.getId());
        }
        return cpt;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void deletePath(int id, String path) {
        dsl.delete(DATASOURCE_SELECTED_PATH).where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id)).and(DATASOURCE_SELECTED_PATH.PATH.eq(path)).execute();
        dsl.delete(DATASOURCE_PATH_STORE).where(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(id)).and(DATASOURCE_PATH_STORE.PATH.eq(path)).execute();
        dsl.delete(DATASOURCE_PATH).where(DATASOURCE_PATH.DATASOURCE_ID.eq(id)).and(DATASOURCE_PATH.PATH.eq(path)).execute();
    }

    @Override
    public boolean hasSelectedPath(int id) {
        int exist = dsl.selectCount()
                       .from(DATASOURCE_SELECTED_PATH)
                       .where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id))
                       .fetchOneInto(Integer.class);
        return exist > 0;
    }

    @Override
    public List<DataSourceSelectedPath> getSelectedPath(int id, Integer limit) {
            var query = dsl.select()
                  .from(DATASOURCE_SELECTED_PATH)
                  .where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id))
                  .orderBy(DATASOURCE_SELECTED_PATH.PATH);
            Select fullQuery;
            if (limit != null) {
                fullQuery = query.limit(limit);
            } else {
                fullQuery = query;
            }
            return convertListToDtoPath(fullQuery.fetchInto(DatasourceSelectedPath.class));
    }

    @Override
    public DataSourceSelectedPath getSelectedPath(int id, String path) {
        return convertToDto(dsl.select()
                               .from(DATASOURCE_SELECTED_PATH)
                               .where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id))
                               .and(DATASOURCE_SELECTED_PATH.PATH.eq(path))
                               .fetchOneInto(DatasourceSelectedPath.class));
    }

    @Override
    public boolean existSelectedPath(int id, String path) {
        return dsl.selectCount()
                  .from(DATASOURCE_SELECTED_PATH)
                  .where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id))
                  .and(DATASOURCE_SELECTED_PATH.PATH.eq(path))
                  .fetchOneInto(Integer.class) > 0;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addSelectedPath(int dsId, String subPath) {
        dsl.insertInto(DATASOURCE_SELECTED_PATH).set(DATASOURCE_SELECTED_PATH.DATASOURCE_ID, dsId).set(DATASOURCE_SELECTED_PATH.PATH, subPath).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addAnalyzedPath(DataSourcePath dsPath, Map<String, String> types) {
        DatasourcePathRecord newRecord = dsl.newRecord(DATASOURCE_PATH);
        newRecord.from(dsPath);
        newRecord.store();
        for (Entry<String, String> type : types.entrySet()) {
            dsl.insertInto(DATASOURCE_PATH_STORE).set(DATASOURCE_PATH_STORE.DATASOURCE_ID, dsPath.getDatasourceId())
                                                 .set(DATASOURCE_PATH_STORE.PATH, dsPath.getPath())
                                                 .set(DATASOURCE_PATH_STORE.STORE, type.getKey())
                                                 .set(DATASOURCE_PATH_STORE.TYPE,  type.getValue()).execute();
            int exist = dsl.selectCount().from(DATASOURCE_STORE)
                             .where(DATASOURCE_STORE.DATASOURCE_ID.eq(dsPath.getDatasourceId()))
                             .and(DATASOURCE_STORE.STORE.eq(type.getKey()))
                             .and(DATASOURCE_STORE.TYPE.eq(type.getValue()))
                             .fetchOneInto(Integer.class);
            if (exist == 0) {
                dsl.insertInto(DATASOURCE_STORE)
                        .set(DATASOURCE_STORE.DATASOURCE_ID, dsPath.getDatasourceId())
                        .set(DATASOURCE_STORE.STORE, type.getKey())
                        .set(DATASOURCE_STORE.TYPE, type.getValue())
                        .execute();
            }
        }

    }

    @Override
    public DataSourcePathComplete getAnalyzedPath(int dsId, String path) {
        DatasourcePath dsP = dsl.select().from(DATASOURCE_PATH)
                .where(DATASOURCE_PATH.DATASOURCE_ID.eq(dsId))
                .and(DATASOURCE_PATH.PATH.eq(path))
                .fetchOneInto(DatasourcePath.class);
        if (dsP != null) {
            List<DatasourcePathStore> pathTypes = dsl.select()
                                               .from(DATASOURCE_PATH_STORE)
                                               .where(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(dsId))
                                               .and(DATASOURCE_PATH_STORE.PATH.eq(path))
                                               .fetchInto(DatasourcePathStore.class);
            Map<String, String> types = new HashMap<>();
            for (DatasourcePathStore pathType : pathTypes) {
                types.put(pathType.getStore(), pathType.getType());
            }
            return new DataSourcePathComplete(convertToDto(dsP), types);
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearSelectedPath(int id) {
        dsl.delete(DATASOURCE_SELECTED_PATH).where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void clearAllPath(int id) {
        dsl.delete(DATASOURCE_SELECTED_PATH).where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id)).execute();
        dsl.delete(DATASOURCE_PATH_STORE).where(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(id)).execute();
        dsl.delete(DATASOURCE_PATH).where(DATASOURCE_PATH.DATASOURCE_ID.eq(id)).execute();
        dsl.delete(DATASOURCE_STORE).where(DATASOURCE_STORE.DATASOURCE_ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void update(DataSource ds) {
        dsl.update(DATASOURCE)
                //.set(DATASOURCE.DATE_CREATION, ds.getDateCreation()) do not update date creation
                .set(DATASOURCE.PWD, ds.getPwd())
                .set(DATASOURCE.READ_FROM_REMOTE, ds.getReadFromRemote())
                .set(DATASOURCE.STORE_ID, ds.getStoreId())
                .set(DATASOURCE.TYPE, ds.getType())
                .set(DATASOURCE.URL, ds.getUrl())
                .set(DATASOURCE.USERNAME, ds.getUsername())
                .set(DATASOURCE.FORMAT, ds.getFormat())
                .set(DATASOURCE.PERMANENT, ds.getPermanent())
                .where(DATASOURCE.ID.eq(ds.getId()))
                .execute();

    }

    @Override
    public String getAnalysisState(int id) {
        return dsl.select(DATASOURCE.ANALYSIS_STATE).from(DATASOURCE).where(DATASOURCE.ID.eq(id)).fetchOneInto(String.class);
    }

    @Override
    public void updateAnalyzedPath(DataSourcePath dsPath, Map<String, String> types) {
        dsl.update(DATASOURCE_PATH)
                .set(DATASOURCE_PATH.FOLDER, dsPath.getFolder())
                .set(DATASOURCE_PATH.NAME, dsPath.getName())
                .set(DATASOURCE_PATH.PARENT_PATH, dsPath.getParentPath())
                .set(DATASOURCE_PATH.SIZE, dsPath.getSize())
                .where(DATASOURCE_PATH.DATASOURCE_ID.eq(dsPath.getDatasourceId())
                .and(DATASOURCE_PATH.PATH.eq(dsPath.getPath())));

        dsl.delete(DATASOURCE_PATH_STORE)
               .where(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(dsPath.getDatasourceId()))
               .and(DATASOURCE_PATH_STORE.PATH.eq(dsPath.getPath()))
               .execute();

        for (Entry<String, String> type : types.entrySet()) {
            dsl.insertInto(DATASOURCE_PATH_STORE)
                .set(DATASOURCE_PATH_STORE.DATASOURCE_ID, dsPath.getDatasourceId())
                .set(DATASOURCE_PATH_STORE.PATH, dsPath.getPath())
                .set(DATASOURCE_PATH_STORE.STORE, type.getKey())
                .set(DATASOURCE_PATH_STORE.TYPE,  type.getValue())
                .execute();
            int exist = dsl.selectCount()
                           .from(DATASOURCE_STORE)
                           .where(DATASOURCE_STORE.DATASOURCE_ID.eq(dsPath.getDatasourceId()))
                           .and(DATASOURCE_STORE.STORE.eq(type.getKey()))
                           .and(DATASOURCE_STORE.TYPE.eq(type.getValue()))
                           .fetchOneInto(Integer.class);
            if (exist == 0) {
                dsl.insertInto(DATASOURCE_STORE)
                   .set(DATASOURCE_STORE.DATASOURCE_ID, dsPath.getDatasourceId())
                   .set(DATASOURCE_STORE.STORE, type.getKey())
                   .set(DATASOURCE_STORE.TYPE, type.getValue())
                   .execute();
            }
        }
    }

    @Override
    public void updateAnalysisState(int id, String state) {
        dsl.update(DATASOURCE)
           .set(DATASOURCE.ANALYSIS_STATE, state)
           .where(DATASOURCE.ID.eq(id))
           .execute();
    }

    @Override
    public Map<String, Set<String>> getDatasourceStores(int id) {
        List<DatasourceStore> storeTypes = dsl
                           .select()
                           .from(DATASOURCE_STORE)
                           .where(DATASOURCE_STORE.DATASOURCE_ID.eq(id))
                           .fetchInto(DatasourceStore.class);
        final Map<String, Set<String>> results = new HashMap<>();
        for (DatasourceStore storeType : storeTypes) {
            if (results.containsKey(storeType.getStore())) {
                results.get(storeType.getStore()).add(storeType.getType());
            } else {
                Set<String> s = new HashSet<>();
                s.add(storeType.getType());
                results.put(storeType.getStore(), s);
            }
        }
        return results;
    }

    @Override
    public List<String> getPathByStore(int id, String storeId, Integer limit) {
        SelectConditionStep query = dsl.select(DATASOURCE_PATH_STORE.PATH)
                  .from(DATASOURCE_PATH_STORE)
                  .where(DATASOURCE_PATH_STORE.STORE.eq(storeId))
                  .and(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(id));
        if (limit != null) {
            return query.limit(limit).fetchInto(String.class);
        }
        return query.fetchInto(String.class);
    }

    @Override
    public List<String> getPathByStoreAndFormat(int id, String storeId, String format, Integer limit) {
        SelectConditionStep query = dsl.select(DATASOURCE_PATH_STORE.PATH)
                  .from(DATASOURCE_PATH_STORE)
                  .where(DATASOURCE_PATH_STORE.STORE.eq(storeId));
        if (format != null) {
            query = query.and(DATASOURCE_PATH_STORE.TYPE.eq(format));
        }
        if (storeId != null) {
            query = query.and(DATASOURCE_PATH_STORE.DATASOURCE_ID.eq(id));
        }
        if (limit != null) {
            return query.limit(limit).fetchInto(String.class);
        }
        return query.fetchInto(String.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updatePathStatus(int id, String path, String newStatus) {
        dsl.update(DATASOURCE_SELECTED_PATH)
           .set(DATASOURCE_SELECTED_PATH.STATUS, newStatus)
           .where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id))
           .and(DATASOURCE_SELECTED_PATH.PATH.eq(path))
           .execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updatePathProvider(int id, String path, int providerId) {
        dsl.update(DATASOURCE_SELECTED_PATH)
           .set(DATASOURCE_SELECTED_PATH.PROVIDER_ID, providerId)
           .where(DATASOURCE_SELECTED_PATH.DATASOURCE_ID.eq(id))
           .and(DATASOURCE_SELECTED_PATH.PATH.eq(path))
           .execute();
    }

    @Override
    public void addDataSourceStore(int dsId, String storeId, String format) {
        dsl.insertInto(DATASOURCE_STORE)
                   .set(DATASOURCE_STORE.DATASOURCE_ID, dsId)
                   .set(DATASOURCE_STORE.STORE, storeId)
                   .set(DATASOURCE_STORE.TYPE, format)
                   .execute();
    }

    @Override
    public List<DataSource> findAll() {
        return convertListToDto(dsl.select().from(DATASOURCE).fetchInto(Datasource.class));
    }

    @Override
    public List<DataSourceSelectedPath> getSelectedPathForProvider(int providerId) {
        SelectConditionStep query = dsl.select()
                  .from(DATASOURCE_SELECTED_PATH)
                  .where(DATASOURCE_SELECTED_PATH.PROVIDER_ID.eq(providerId));
            return convertListToDtoPath(query.fetchInto(DatasourceSelectedPath.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateAfterProviderRemoval(int providerId, String newStatus) {
        dsl.update(DATASOURCE_SELECTED_PATH)
           .set(DATASOURCE_SELECTED_PATH.PROVIDER_ID, -1)
           .set(DATASOURCE_SELECTED_PATH.STATUS, newStatus)
           .where(DATASOURCE_SELECTED_PATH.PROVIDER_ID.eq(providerId))
           .execute();
    }

    private DataSource convertToDto(Datasource ds) {
        if (ds != null) {
            DataSource dto = new DataSource();
            dto.setAnalysisState(ds.getAnalysisState());
            dto.setDateCreation(ds.getDateCreation());
            dto.setFormat(ds.getFormat());
            dto.setId(ds.getId());
            dto.setPwd(ds.getPwd());
            dto.setReadFromRemote(ds.getReadFromRemote());
            dto.setStoreId(ds.getStoreId());
            dto.setType(ds.getType());
            dto.setUrl(ds.getUrl());
            dto.setUsername(ds.getUsername());
            dto.setPermanent(ds.getPermanent());
            return dto;
        }
        return null;
    }

    private List<DataSource> convertListToDto(List<Datasource> daos) {
        List<DataSource> results = new ArrayList<>();
        for (Datasource dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }

    private DataSourceSelectedPath convertToDto(DatasourceSelectedPath ds) {
        if (ds != null) {
            DataSourceSelectedPath dto = new DataSourceSelectedPath();
            dto.setDatasourceId(ds.getDatasourceId());
            dto.setPath(ds.getPath());
            dto.setProviderId(ds.getProviderId());
            dto.setStatus(ds.getStatus());
            return dto;
        }
        return null;
    }

    private DataSourcePath convertToDto(DatasourcePath dao) {
        if (dao != null) {
            DataSourcePath dto = new DataSourcePath();
            dto.setDatasourceId(dao.getDatasourceId());
            dto.setPath(dao.getPath());
            dto.setFolder(dao.getFolder());
            dto.setName(dao.getName());
            dto.setSize(dao.getSize());
            dto.setParentPath(dao.getParentPath());
            return dto;
        }
        return null;
    }

    private List<DataSourceSelectedPath> convertListToDtoPath(List<DatasourceSelectedPath> daos) {
        List<DataSourceSelectedPath> results = new ArrayList<>();
        for (DatasourceSelectedPath dao : daos) {
            results.add(convertToDto(dao));
        }
        return results;
    }
}
