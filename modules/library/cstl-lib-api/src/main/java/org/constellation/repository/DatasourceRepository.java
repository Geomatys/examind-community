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
package org.constellation.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourcePath;
import org.constellation.dto.DataSourcePathComplete;

/**
 *
 * @author Guilhem Legal
 */
public interface DatasourceRepository extends AbstractRepository {

    DataSource findById(int id);

    DataSource findByUrl(String url);

    int create(DataSource data);

    void deletePath(int id, String path);

    boolean hasSelectedPath(int id);

    List<DataSourceSelectedPath> getSelectedPath(int id, Integer limit);

    DataSourceSelectedPath getSelectedPath(int id, String path);

    void addDataSourceStore(int dsId, String storeId, String format);

    void addSelectedPath(int dsId, String subPath);

    boolean existSelectedPath(int dsId, String subPath);

    void updateAnalyzedPath(DataSourcePath dsPath, Map<String, String> types);

    void addAnalyzedPath(DataSourcePath dsPath, Map<String, String> types);

    void clearSelectedPath(int id);

    List<DataSource> findAll();

    void update(DataSource ds);

    DataSourcePathComplete getAnalyzedPath(int dsId, String path);

    String getAnalysisState(int id);

    void updateAnalysisState(int id, String state);

    Map<String, Set<String>> getDatasourceStores(int id);

    List<String> getPathByStore(int id, String storeId, Integer limit);

    List<String> getPathByStoreAndFormat(int id, String storeId, String format, Integer limit);

    void updatePathStatus(int id, String path, String newStatus);

    void updatePathProvider(int id, String path, int providerId);
}
