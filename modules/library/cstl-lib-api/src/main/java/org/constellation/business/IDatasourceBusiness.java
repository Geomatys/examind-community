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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    public static enum PathStatus {
        PENDING,
        NO_DATA,
        ERROR,
        INTEGRATED,
        COMPLETED,
        REMOVED
    }

    /**
     * Store a new Datasource.
     *
     * @param ds the new datasource to store.
     * @return the assigned datasource id.
     * 
     * @throws org.constellation.exception.ConstellationException
     */
    Integer create(DataSource ds) throws ConstellationException;

    /**
     * Update a Datasource.
     *
     * @param ds the new datasource to update, id must not be null.
     * @throws org.constellation.exception.ConstellationException
     */
    void update(DataSource ds) throws ConstellationException;

    /**
     * Close a datasource, identified by its id.
     *  - Close the current analysis, if its still going on.
     *  - if the datasource is of type local_files , the files are removed.
     *  - close the fileSystem.
     *
     * @param id the datasource identifier.
     */
    void close(int id);

    /**
     * Remove a datasource, identified by its id.
     * Call {@link IDatasourceBusiness#close()} before removing it.
     *
     * @param id the datasource identifier.
     * @throws org.constellation.exception.ConstellationException
     */
    void delete(int id) throws ConstellationException;

    /**
     * Remove all the datasources.
     * Call {@link IDatasourceBusiness#close()}
     *
     * @throws org.constellation.exception.ConstellationException
     */
    void deleteAll() throws ConstellationException;

    /**
     * Find a datasource by its id.
     *
     * @param id the searched datasource identifier.
     *
     * @return A Datasource or {@code null}
     */
    DataSource getDatasource(int id);

    /**
     * Find a datasource by its utl.
     *
     * @param url the searched datasource url.
     *
     * @return A Datasource or {@code null}
     */
    DataSource getByUrl(String url);

    /**
     * Test if the url pointed by the datasource is reachable.
     * The datasource object, does not have to be recorded in the system to test it.
     *
     * @param ds A datasource.
     *
     * @return Return the keyword "OK" if the datasource is reachable, else, return the exception message explaining the problem.
     */
    String testDatasource(DataSource ds);

    /**
     * Automaticaly select all the paths correspounding
     * to the datasource selected store and format, If no path have been already selected.
     * If the flag forceAutoselection is set, the selection will be completed even if there is already some selected paths.
     *
     *
     * @param id A datasource identifier.
     * @param forceAutoSelection if set to {@code true} the selection will be completed even if there is already some selected paths.
     * @throws org.constellation.exception.ConstellationException
     */
    void recordSelectedPath(Integer id, boolean forceAutoSelection) throws ConstellationException;

    /**
     * Remove a recorded datasource path from the system.
     *
     * @param id A datasource identifier.
     * @param path The path of a file.
     */
    void removePath(Integer id, String path);

    /**
     * Return the list of datasource path that have been selected to import by the user.
     *
     * @param id A datasource identifier.
     * @param limit maximum number of path returned.
     *
     * @return A list of selected datasource path.
     * @throws ConstellationException
     */
    List<DataSourceSelectedPath> getSelectedPath(Integer id, Integer limit) throws ConstellationException;

    /**
     * Return a datasource path that have been selected to import by the user with the specified path.
     *
     * @param id A datasource identifier.
     * @param path the searched path.
     *
     * @return A list of selected datasource path.
     */
    DataSourceSelectedPath getSelectedPath(Integer id, String path) throws ConstellationException;

    /**
     * Return {@code true} if the specified path exist and has been selected for import by the user.
     *
     * @param dsId the datasource identifier.
     * @param path the searched path.
     *
     * @return
     */
    boolean existSelectedPath(final int dsId, String path);

    /**
     * Return a List of path informations for the specified sub path (not recursive).
     *
     * @param dsId the datasource identifier.
     * @param subPath the datasource sub path.
     *
     * @return A list of file informations.
     */
    List<FileBean> exploreDatasource(final Integer dsId, final String subPath) throws ConstellationException;

    /**
     * Return an already analyzed path in the datasource if present.
     *
     * @param dsId Datasource identifier.
     * @param path path of the searched file.
     *
     * @return An analyzed path if already present.
     * @throws ConstellationException
     */
    Optional<FileBean> getAnalyzedPath(final Integer dsId, final String path) throws ConstellationException;

    /**
     * Treat all the selected datasource path, using the specified provider configuration.
     * Instanciate provider, data and metadata for each path, with an hidden flag.
     *
     * @param dsId The datasource identifier.
     * @param provConfig The provider configuration, containing various custom parameters for the datastore.
     *
     * @return A list of instancied store containing data.
     * @throws ConstellationException
     */
    DatasourceAnalysisV3 analyseDatasourceV3(final Integer dsId, ProviderConfiguration provConfig) throws ConstellationException;

    /**
     * Add a new datasource path in the selection for import.
     *
     * @param dsId The datasource identifier.
     * @param subPath The path to add at the selection.
     */
    void addSelectedPath(final int dsId,  String subPath);

    /**
     * Clear the datasource selection of path to import.
     *
     * @param id The datasource identifier.
     */
    void clearSelectedPaths(int id);

    /**
     * Clear All the datasource path recorded.
     *
     * @param id The datasource identifier.
     */
    void clearPaths(int id);

    /**
     * Remove all the datasource not permanent and older t han 24 hours.
     *
     * @throws ConstellationException
     */
    void removeOldDatasource() throws ConstellationException;

    /**
     * Perform an analysis on each file of the datasource (if deep is set to false, perform it only in the first level).
     * Then return a map of store / formats detected in the datasource.
     *
     * @param id The datasource identifier.
     * @param async if true, and if the datasource is not yet analyzed, it will return an empty result and perform the analysis on a new Thread.
     * @param deep if false, it will only analyse the first level of tha datasource.
     *
     * @return A map of store / formats detected in the datasource.
     * @throws ConstellationException
     */
    Map<String, Set<String>> computeDatasourceStores(int id, boolean async, boolean deep) throws ConstellationException;

    /**
     * Perform an analysis on each file of the datasource (if deep is set to false, perform it only in the first level).
     * if storeId is not null, try to analyse the files only with the specified store.
     * Then return a map of store / formats detected in the datasource.
     *
     * @param id The datasource identifier.
     * @param async if true, and if the datasource is not yet analyzed, it will return an empty result and perform the analysis on a new Thread.
     * @param storeId Allow to analyse the file only against one store.
     * @param deep if false, it will only analyse the first level of tha datasource.
     *
     * @return A map of store / formats detected in the datasource.
     * @throws ConstellationException
     */
    Map<String, Set<String>> computeDatasourceStores(int id, boolean async, String storeId, boolean deep) throws ConstellationException;

    /**
     * Return the current state of the datasource analysis going on (or already finished).
     * for the different possible values see {@link AnalysisState}
     *
     * @param id The datasource identifier.
     * @return the current state of the datasource analysis.
     */
    String getDatasourceAnalysisState(int id);

    /**
     * Update the current state of the datasource analysis.
     * for the different possible vlues see {@link AnalysisState}
     *
     * @param dsId The datasource identifier.
     * @param state the new state of the datasource analysis.
     */
    void updateDatasourceAnalysisState(int dsId, String state);

    /**
     * Analyse and treat the specified datasource select path.
     *
     * Instanciate provider, data and metadata for the path, with the specified hidden flag.
     *
     * @param p The datasource path.
     * @param dsId The datasource identifier.
     * @param provConfig The provider configuration, containing various custom parameters for the datastore.
     * @param hidden A flag applied to the data and metadata created.
     * @param datasetId The dataset identifier in which the data will be inserted.
     * @param owner the owner of the created provider, data and metadata.
     *
     * @return informations about the generated objects.
     * @throws ConstellationException
     */
    ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath p, Integer dsId, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner) throws ConstellationException;
    
    /**
     * Analyse and treat the specified datasource select path.Instanciate provider, data and metadata for the path, with the specified hidden flag.
     *
     * @param p The datasource path.
     * @param dsId The datasource identifier.
     * @param provConfig The provider configuration, containing various custom parameters for the datastore.
     * @param hidden A flag applied to the data and metadata created.
     * @param datasetId The dataset identifier in which the data will be inserted.
     * @param owner the owner of the created provider, data and metadata.
     * @param assignedId Assign the provider identifier of the created provider.
     *
     * @return informations about the generated objects.
     * @throws ConstellationException
     */
    ResourceStoreAnalysisV3 treatDataPath(DataSourceSelectedPath p, Integer dsId, ProviderConfiguration provConfig, boolean hidden, Integer datasetId, Integer owner, String assignedId) throws ConstellationException;

    /**
     * Update the status of the specified datasource path.
     * for the different possible vlues see {@link PathStatus}
     *
     * @param id The datasource identifier.
     * @param path the designed datasource path.
     * @param newStatus the new status of the path.
     */
    void updatePathStatus(int id, String path, String newStatus);

    /**
     * Some filesystem needs an initialization before being usable.
     * for S3 for example we need to create a filesystem with the credentials in order that the path created by
     * geotk or SIS to work.
     */
    void initializeFilesystems();

    /**
     * Instanciate a sub path for the specified datasource.
     *
     * @param id datasource identifier.
     * @param subPath the subpath.
     *
     * @return  A Path.
     */
    Path getDatasourcePath(int id, final String subPath) throws ConstellationException;
}
