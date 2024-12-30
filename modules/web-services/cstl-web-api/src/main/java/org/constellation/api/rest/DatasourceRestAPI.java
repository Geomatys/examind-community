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
package org.constellation.api.rest;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.zip.CRC32;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

import static org.constellation.api.rest.AbstractRestAPI.LOGGER;

import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProcessBusiness;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.process.TaskParameter;
import org.constellation.dto.importdata.BatchAnalysis;
import org.constellation.dto.DataCustomConfiguration;
import org.constellation.dto.importdata.DatasourceAnalysisV3;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.dto.importdata.StoreFormat;
import org.constellation.exception.ConstellationException;
import org.constellation.process.data.ImportDataDescriptor;
import org.constellation.util.ParamUtilities;
import org.constellation.util.Util;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterValueGroup;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Various API related to datasource management.
 * 
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class DatasourceRestAPI extends AbstractRestAPI {

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    @Autowired
    private IProcessBusiness processBusiness;

    /**
     * Return the {@link DataSource} with the specified id.
     *
     * @param id The {@link DataSource} id.
     * @return The {@link DataSource} or {@code null} if the datasource does not exist.
     */
    @RequestMapping(value = "/datasources/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasource(@PathVariable("id") int id) {
        try {
            return new ResponseEntity(datasourceBusiness.getDatasource(id), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Create a new {@link DataSource}.
     *
     * @param ds The {@link DataSource} to store in the system.
     * @return The assigned datasource id.
     */
    @RequestMapping(value = "/datasources", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity create(@RequestBody final DataSource ds, HttpServletRequest req) {
        try {
            assertAuthentificated(req);
            // 1. create new upload directory if not set for local_files datasource
            if ("local_files".equals(ds.getType()) && (ds.getUrl() == null) || ds.getUrl().isEmpty()) {
                final Path uploadDirectory = getUploadDirectory(req);
                final Path dsDirectory = uploadDirectory.resolve(UUID.randomUUID().toString());
                Files.createDirectory(dsDirectory);
                ds.setUrl(dsDirectory.toUri().toString());
            }
            return new ResponseEntity(datasourceBusiness.create(ds), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Update the {@link DataSource} identifier by {@link DataSource#getId()}
     * 
     * @param ds The {@link DataSource} to update.
     * @return
     */
    @RequestMapping(value = "/datasources", method = PUT, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity update(@RequestBody final DataSource ds) {
        try {
            datasourceBusiness.update(ds);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Remove the {@link DataSource} with the specified id.
     *
     * @param id The {@link DataSource} id.
     * @return 
     */
    @RequestMapping(value = "/datasources/{id}", method = DELETE)
    public ResponseEntity deleteDatasource(@PathVariable("id") int id) {
        try {
            datasourceBusiness.delete(id);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Test if the url pointed by the specified {@link DataSource} is reachable.
     * The datasource does not have to be recorded in the system to test it.
     *
     * @param ds The {@link DataSource} to test.
     * @param response
     * @return
     */
    @RequestMapping(value = "/datasources/test", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity testDatasource(@RequestBody final DataSource ds,  HttpServletResponse response) {
        try {
            IOUtils.write(datasourceBusiness.testDatasource(ds), response.getOutputStream());
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Upload a file into the specified {@link DataSource}.
     * 
     * @param id The {@link DataSource} id.
     * @param uploadedFile a file to upload on the service.
     * @return
     */
    @RequestMapping(value = "/datasources/{id}/upload", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> uploadDatasourceFile(@PathVariable("id") int id, @RequestParam("file") MultipartFile uploadedFile) {
        try {
            // 1. retrieve the upload directory
            final DataSource ds = datasourceBusiness.getDatasource(id);
            final Path dsDirectory = Paths.get(new URI(ds.getUrl()));

            // 2. save file to upload directory
            final Path newFile = dsDirectory.resolve(uploadedFile.getOriginalFilename());
            String url;
            try (InputStream in = uploadedFile.getInputStream()) {
                Files.copy(in, newFile, StandardCopyOption.REPLACE_EXISTING);
                // 2.1 unzip if needed
                String ext = IOUtilities.extension(newFile);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(newFile, dsDirectory, new CRC32());
                    Files.deleteIfExists(newFile);
                    url = dsDirectory.toUri().toString();
                } else {
                    url = newFile.toUri().toString();
                }
            }

            // 3. reset the analyse state
            datasourceBusiness.updateDatasourceAnalysisState(id, IDatasourceBusiness.AnalysisState.NOT_STARTED.name());

            return new ResponseEntity(url, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Remove the specified file in the specified {@link DataSource}.
     *
     * @param id The {@link DataSource} id.
     * @param file The file path to remove from the datasource.
     * @return
     */
    @RequestMapping(value = "/datasources/{id}/remove", method = DELETE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity removeDatasourceFile(@PathVariable("id") int id, @RequestParam("file") String file) {
        try {
            // 1. retrieve the upload directory
            final DataSource ds = datasourceBusiness.getDatasource(id);
            final Path dsDirectory = Paths.get(new URI(ds.getUrl()));

            // 2. remove file to upload directory
            final Path newFile = dsDirectory.resolve(file);
            if (Files.exists(newFile)) {
                IOUtilities.deleteRecursively(newFile);
            } else {
                throw new ConstellationException("The specified file does not exist in the datasource.");
            }

            // 3. reset the analyse state
            datasourceBusiness.updateDatasourceAnalysisState(id, IDatasourceBusiness.AnalysisState.NOT_STARTED.name());

            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Upload the specified distant file (pointed by an url) and build a new {@link DataSource}.
     * Support Basic authentication when username/pwd are supplied.
     * 
     * @param distantFile An url pointing to an online file.
     * @param userName Optional Basic authentication username.
     * @param pwd Optional Basic authentication password.
     * 
     * @return The assigned datasource id.
     */
    @RequestMapping(value = "/datasources/upload/distant", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity uploadDatasourceDistantFile(@RequestParam("url") String distantFile,
            @RequestParam(name = "user", required = false) String userName, @RequestParam(name = "pwd", required = false) String pwd, HttpServletRequest req) {
        try {
            assertAuthentificated(req);
            // 1. create new upload directory
            final Path uploadDirectory = getUploadDirectory(req);
            final Path dsDirectory = uploadDirectory.resolve(UUID.randomUUID().toString());
            Files.createDirectory(dsDirectory);

            // 2. download and save file to upload directory
            String fileName = distantFile.substring(distantFile.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) {
                fileName = "tmp-file";
            }
            final Path newFile = dsDirectory.resolve(fileName);
            final URL url = new URL(distantFile);
            URLConnection conn = url.openConnection();
            if (userName != null && !userName.isEmpty() && pwd != null && !pwd.isEmpty()) {
                String user_pass = userName + ":" + pwd;
                String encoded = Base64.getEncoder().encodeToString(user_pass.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, newFile, StandardCopyOption.REPLACE_EXISTING);

                // 2.1 unzip if needed
                String ext = IOUtilities.extension(newFile);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(newFile, dsDirectory, new CRC32());
                    Files.deleteIfExists(newFile);
                }
            }

            // 3. record the new datasource
            DataSource ds = new DataSource(null, "local_files", dsDirectory.toUri().toString(), userName, pwd, null, false, System.currentTimeMillis(), "NOT_STARTED", null, false, Map.of());
            return new ResponseEntity(datasourceBusiness.create(ds), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Upload the specified distant file (pointed by an url) into a {@link DataSource}.
     * Support Basic authentication when username/pwd are supplied.
     *
     * @param id The {@link DataSource} id.
     * @param distantFile An url pointing to an online file.
     * @param userName Optional Basic authentication username.
     * @param pwd Optional Basic authentication password.
     *
     * @return 
     */
    @RequestMapping(value = "/datasources/{id}/upload/distant", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity uploadDatasourceDistantFile(@PathVariable("id") int id, @RequestParam("url") String distantFile,
            @RequestParam(name = "user", required = false) String userName, @RequestParam(name = "pwd", required = false) String pwd) {
        try {
            // 1. retrieve the upload directory
            final DataSource ds = datasourceBusiness.getDatasource(id);
            final Path dsDirectory = Paths.get(new URI(ds.getUrl()));

            // 2. save file to upload directory
            String fileName = distantFile.substring(distantFile.lastIndexOf('/') + 1);
            if (fileName.isEmpty()) {
                fileName = "tmp-file";
            }

            int i = 1;
            Path newFile = dsDirectory.resolve(fileName);
            while (Files.exists(newFile)) {
                newFile = dsDirectory.resolve(fileName + '(' + i + ')');
                i++;
            }

            final URL url = new URL(distantFile);
            URLConnection conn = url.openConnection();
            if (userName != null && !userName.isEmpty() && pwd != null && !pwd.isEmpty()) {
                String user_pass = userName + ":" + pwd;
                String encoded = Base64.getEncoder().encodeToString(user_pass.getBytes());
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }

            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, newFile, StandardCopyOption.REPLACE_EXISTING);

                // 2.1 unzip if needed
                String ext = IOUtilities.extension(newFile);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(newFile, dsDirectory, new CRC32());
                    Files.deleteIfExists(newFile);
                }
            }
            // 3. reset the analyse state
            datasourceBusiness.updateDatasourceAnalysisState(id, IDatasourceBusiness.AnalysisState.NOT_STARTED.name());

            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Explore a {@link DataSource} from the specified sub path (default "/").
     * return a list of {@link FileBean} for each file in the pointed directory.
     *
     * @param id The {@link DataSource} id.
     * @param path Sub path to explore (default "/"). Must point to a directory.
     * @return
     */
    @RequestMapping(value = "/datasources/{id}/explore", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity explore(@PathVariable("id") int id, @RequestParam(name = "path", required = false, defaultValue = "/") String path) {
        try {
            return new ResponseEntity(datasourceBusiness.exploreDatasource(id, path), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Perform an analysis on each file a {@link DataSource}.
     * If deep is set to false, perform it only in the first level.
     * If async is set to true, this method wil return an empty results, and perform the analysis operation in background.
     *
     * Then return a list of {@link StoreFormat} detected in the datasource.
     *
     * @param id The {@link DataSource} id.
     * @param async Asynchrous mode, default to {@code false}
     * @param deep Deep mode, default to {@code false}
     * @param s63 If true, the analysis will seach for S63 dataset. set default to true for retro-compatibility
     * 
     * @return A list of {@link StoreFormat} detected in the datasource, or an empty result if set to asynchrous.
     */
    @RequestMapping(value = "/datasources/{id}/stores", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceStores(@PathVariable("id") int id,
                                            @RequestParam(name = "async", required = false, defaultValue = "false") Boolean async,
                                            @RequestParam(name = "deep", required = false, defaultValue = "false") Boolean deep,
                                            @RequestParam(name = "s63", required = false, defaultValue = "true") Boolean s63) {
        try {
            Map<String, Set<String>> storeFormats = datasourceBusiness.computeDatasourceStores(id, async, deep, s63);
            final List<StoreFormat> results = new ArrayList<>();
            for (Entry<String, Set<String>> entry : storeFormats.entrySet()) {
                for (String format : entry.getValue()) {
                    results.add(new StoreFormat(entry.getKey(), format));
                }
            }
            return new ResponseEntity(results, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return the {@link AnalysisState} of the specified {@link DataSource}.
     *
     * return values are :
     *  - PENDING
     *  - COMPLETED
     *  - NOT_STARTED
     *  - ERROR
     * 
     * @param id The {@link DataSource} id.
     * @param response
     * @return return the current state of analysis of the datasource.
     */
    @RequestMapping(value = "/datasources/{id}/analysis/state", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisState(@PathVariable("id") int id,  HttpServletResponse response) {
        try {
            IOUtils.write(datasourceBusiness.getDatasourceAnalysisState(id), response.getOutputStream());
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return the result of the {@link DataSource} analysis.
     * Combined with the specified store params it will return a list of stores and resources,
     * for each selected file, that can be integrated as data by Examind.
     *
     * @param id The {@link DataSource} id.
     * @param storeParams Datastore parameters to apply to the matching files candidates of the datasource.
     * 
     * @return a List of store and resources.
     */
    @RequestMapping(value = "/datasources/{id}/analysisV3", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisV3(@PathVariable("id") int id, @RequestBody final DataCustomConfiguration.Type storeParams) {
        try {
            final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
            storeParams.cleanupEmptyProperty();
            storeParams.propertyToMap(provConfig.getParameters());
            return new ResponseEntity(datasourceBusiness.analyseDatasourceV3(id, provConfig), OK);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Return a sampled result of the {@link DataSource} analysis.
     * Combined with the specified store params it will return a list of stores and resources,
     * for (some) selected file, that can be integrated as data by Examind.
     *
     * The result will be limited to the first 5 file encountered.
     *
     * @param id The {@link DataSource} id.
     * @param storeParams Datastore parameters to apply to the matching files candidates of the datasource.
     *
     * @return a limited (5 at most) List of store and resources.
     */
    @RequestMapping(value = "/datasources/{id}/analysis/sample", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisSample(@PathVariable("id") int id, @RequestBody final DataCustomConfiguration.Type storeParams) {
        try {
            final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
            storeParams.cleanupEmptyProperty();
            storeParams.propertyToMap(provConfig.getParameters());

            datasourceBusiness.recordSelectedPath(id, false);
             List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(id, 5);
             DatasourceAnalysisV3 outputDatas = new DatasourceAnalysisV3();
             for (DataSourceSelectedPath p : paths) {
                 ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, id, provConfig, true, null, null);
                 outputDatas.getStores().add(store);
             }

            return new ResponseEntity(outputDatas, OK);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Launch the complete "Import Data" process in batch mode.
     *
     * @param id The {@link DataSource} id.
     * @param params All the parameters required for an import data.
     * @param req
     * @return
     */
    @RequestMapping(value = "/datasources/{id}/analysis/batch", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisBatch(@PathVariable("id") int id, @RequestBody final BatchAnalysis params, HttpServletRequest req) {
        try {
            final int userId = assertAuthentificated(req);
            final DataSource ds = datasourceBusiness.getDatasource(id);

            final ProviderConfiguration provConfig = new ProviderConfiguration(params.getStoreParams().getCategory(), params.getStoreParams().getId());
            params.getStoreParams().cleanupEmptyProperty();
            params.getStoreParams().propertyToMap(provConfig.getParameters());

            final ProcessDescriptor desc = ImportDataDescriptor.INSTANCE;
            final ParameterValueGroup inputs = desc.getInputDescriptor().createValue();
            inputs.parameter(ImportDataDescriptor.DATASET_ID_NAME).setValue(params.getDatasetId());
            inputs.parameter(ImportDataDescriptor.DATASOURCE_ID_NAME).setValue(id);
            inputs.parameter(ImportDataDescriptor.METADATA_MODEL_ID_NAME).setValue(params.getModelId());
            inputs.parameter(ImportDataDescriptor.PROVIDER_CONFIGURATION_NAME).setValue(provConfig);
            inputs.parameter(ImportDataDescriptor.STYLE_ID_NAME).setValue(params.getStyleId());
            inputs.parameter(ImportDataDescriptor.USER_ID_NAME).setValue(userId);

            final org.geotoolkit.process.Process p = desc.createProcess(inputs);

            String taskName = "import_data_" + ds.getStoreId() + ":" + ds.getId() + "_" + System.currentTimeMillis();
            TaskParameter taskParameter = new TaskParameter();
            taskParameter.setProcessAuthority(Util.getProcessAuthorityCode(desc));
            taskParameter.setProcessCode(desc.getIdentifier().getCode());
            taskParameter.setDate(System.currentTimeMillis());
            taskParameter.setInputs(ParamUtilities.writeParameterJSON(inputs));
            taskParameter.setOwner(userId);
            taskParameter.setName(taskName);
            taskParameter.setType("INTERNAL");
            Integer taskId = processBusiness.addTaskParameter(taskParameter);

            //run pyramid process in Quartz
            processBusiness.runProcess(taskName, p, taskId, userId);

            return new ResponseEntity(OK);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    /**
     * Record the files selection within the {@link DataSource} that we want to integrated
     *
     * @param id The {@link DataSource} id.
     * @param paths A list of {@link FileBean} we want to be integrated.
     * @return
     */
    @RequestMapping(value = "/datasources/{id}/selectedPath", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity setSelectedPaths(@PathVariable("id") int id, @RequestBody List<FileBean> paths) {
        try {
            datasourceBusiness.clearSelectedPaths(id);
            for (FileBean fb : paths) {
                datasourceBusiness.addSelectedPath(id, fb.getPath());
            }
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
