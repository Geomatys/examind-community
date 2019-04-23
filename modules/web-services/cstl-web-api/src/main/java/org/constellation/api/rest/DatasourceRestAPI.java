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
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.constellation.process.data.ImportDataDescriptor;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterValueGroup;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

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
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class DatasourceRestAPI extends AbstractRestAPI {

    @Inject
    private IDatasourceBusiness datasourceBusiness;

    @Inject
    private IProcessBusiness processBusiness;

    @RequestMapping(value = "/datasources/{id}", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasource(@PathVariable("id") int id) {
        try {
            return new ResponseEntity(datasourceBusiness.getDatasource(id), OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity create(@RequestBody final DataSource ds) {
        try {
            // 1. create new upload directory if not set for local_files datasource
            if ("local_files".equals(ds.getType()) && (ds.getUrl() == null) || ds.getUrl().isEmpty()) {
                final Path uploadDirectory = getUploadDirectory();
                final Path dsDirectory = uploadDirectory.resolve(UUID.randomUUID().toString());
                Files.createDirectory(dsDirectory);
                ds.setUrl(dsDirectory.toUri().toString());
            }
            return new ResponseEntity(datasourceBusiness.create(ds), OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources", method = PUT, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity update(@RequestBody final DataSource ds) {
        try {
            datasourceBusiness.update(ds);
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}", method = DELETE)
    public ResponseEntity deleteDatasource(@PathVariable("id") int id) {
        try {
            datasourceBusiness.delete(id);
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/test", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity testDatasource(@RequestBody final DataSource ds,  HttpServletResponse response) {
        try {
            IOUtils.write(datasourceBusiness.testDatasource(ds), response.getOutputStream());
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/upload", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity uploadDatasourceFile(@PathVariable("id") int id, @RequestParam("file") MultipartFile uploadedFile) {
        try {
            // 1. retrieve the upload directory
            final DataSource ds = datasourceBusiness.getDatasource(id);
            final Path dsDirectory = Paths.get(new URI(ds.getUrl()));

            // 2. save file to upload directory
            final Path newFile = dsDirectory.resolve(uploadedFile.getOriginalFilename());
            try (InputStream in = uploadedFile.getInputStream()) {
                Files.copy(in, newFile, StandardCopyOption.REPLACE_EXISTING);
                // 2.1 unzip if needed
                String ext = IOUtilities.extension(newFile);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(newFile, dsDirectory, new CRC32());
                    Files.deleteIfExists(newFile);
                }
            }

            // 3. reset the analyse state
            datasourceBusiness.updateDatasourceAnalysisState(id, "NOT_STARTED");

            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/upload/distant", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity uploadDatasourceDistantFile(@RequestParam("url") String distantFile,
            @RequestParam(name = "user", required = false) String userName, @RequestParam(name = "pwd", required = false) String pwd) {
        try {
            // 1. create new upload directory
            final Path uploadDirectory = getUploadDirectory();
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
            DataSource ds = new DataSource(null, "local_files", dsDirectory.toUri().toString(), userName, pwd, null, false, System.currentTimeMillis(), "NOT_STARTED", null, false);
            return new ResponseEntity(datasourceBusiness.create(ds), OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/upload/distant", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity uploadDatasourceDistantFile(@PathVariable("id") int id, @RequestParam("url") String distantFile) {
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
            try (InputStream in = url.openStream()) {
                Files.copy(in, newFile, StandardCopyOption.REPLACE_EXISTING);

                // 2.1 unzip if needed
                String ext = IOUtilities.extension(newFile);
                if ("zip".equals(ext.toLowerCase())) {
                    ZipUtilities.unzip(newFile, dsDirectory, new CRC32());
                    Files.deleteIfExists(newFile);
                }
            }
            // 3. reset the analyse state
            datasourceBusiness.updateDatasourceAnalysisState(id, "NOT_STARTED");

            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/explore", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity explore(@PathVariable("id") int id, @RequestParam(name = "path", required = false, defaultValue = "/") String path) {
        try {
            return new ResponseEntity(datasourceBusiness.exploreDatasource(id, path), OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/stores", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceStores(@PathVariable("id") int id, @RequestParam(name = "async", required = false, defaultValue = "false") Boolean async) {
        try {
            Map<String, Set<String>> storeFormats = datasourceBusiness.computeDatasourceStores(id, async);
            final List<StoreFormat> results = new ArrayList<>();
            for (Entry<String, Set<String>> entry : storeFormats.entrySet()) {
                for (String format : entry.getValue()) {
                    results.add(new StoreFormat(entry.getKey(), format));
                }
            }
            return new ResponseEntity(results, OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/analysis/state", method = GET, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisState(@PathVariable("id") int id,  HttpServletResponse response) {
        try {
            IOUtils.write(datasourceBusiness.getDatasourceAnalysisState(id), response.getOutputStream());
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/analysisV3", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisV3(@PathVariable("id") int id, @RequestBody final DataCustomConfiguration.Type storeParams) {
        try {
            final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
            storeParams.cleanupEmptyProperty();
            storeParams.propertyToMap(provConfig.getParameters());
            return new ResponseEntity(datasourceBusiness.analyseDatasourceV3(id, provConfig), OK);

        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/analysis/sample", method = POST, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity getDatasourceAnalysisSample(@PathVariable("id") int id, @RequestBody final DataCustomConfiguration.Type storeParams) {
        try {
            final ProviderConfiguration provConfig = new ProviderConfiguration(storeParams.getCategory(), storeParams.getId());
            storeParams.cleanupEmptyProperty();
            storeParams.propertyToMap(provConfig.getParameters());

            final DataSource ds = datasourceBusiness.getDatasource(id);
            datasourceBusiness.recordSelectedPath(ds);
             List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(ds, 5);
             DatasourceAnalysisV3 outputDatas = new DatasourceAnalysisV3();
             for (DataSourceSelectedPath p : paths) {
                 ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, ds, provConfig, true, null, null);
                 outputDatas.getStores().add(store);
             }

            return new ResponseEntity(outputDatas, OK);

        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

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
            taskParameter.setProcessAuthority(desc.getIdentifier().getAuthority().toString());
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

        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value = "/datasources/{id}/selectedPath", method = POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity setSelectedPaths(@PathVariable("id") int id, @RequestBody List<FileBean> paths) {
        try {
            datasourceBusiness.clearSelectedPaths(id);
            for (FileBean fb : paths) {
                datasourceBusiness.addSelectedPath(id, fb.getPath());
            }
            return new ResponseEntity(OK);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }
}
