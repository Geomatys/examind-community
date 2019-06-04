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
package org.constellation.business;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.importdata.DatasourceAnalysisV3;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IDatasourceBusiness {

    public static enum AnalysisState {
        PENDING,
        COMPLETED,
        NOT_STARTED,
        ERROR
    }

    Integer create(DataSource ds);

    void update(DataSource ds) throws ConstellationException;

    void close(int id);

    void delete(int id) throws ConstellationException;

    DataSource getDatasource(int id);

    DataSource getByUrl(String url);

    String testDatasource(DataSource ds);

    void recordSelectedPath(DataSource ds);

    void removePath(DataSource ds, String path);

    List<DataSourceSelectedPath> getSelectedPath(DataSource ds, Integer limit) throws ConstellationException;

    DataSourceSelectedPath getSelectedPath(DataSource ds, String path);

    boolean existSelectedPath(final int dsId, String path);

    List<FileBean> exploreDatasource(final Integer dsId, final String subPath) throws ConstellationException;

    DatasourceAnalysisV3 analyseDatasourceV3(final Integer dsId, ProviderConfiguration provConfig) throws ConstellationException;

    void addSelectedPath(final int dsId,  String subPath);

    void clearSelectedPaths(int id);

    void removeOldDatasource() throws ConstellationException;

    Map<String, Set<String>> computeDatasourceStores(int id, boolean async) throws ConstellationException;

    Map<String, Set<String>> computeDatasourceStores(int id, boolean async, String storeId) throws ConstellationException;

    String getDatasourceAnalysisState(int id);

    void updateDatasourceAnalysisState(int dsId, String state);

    ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath p, DataSource ds, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner) throws ConstellationException;

    void updatePathStatus(int id, String path, String newStatus);
}
