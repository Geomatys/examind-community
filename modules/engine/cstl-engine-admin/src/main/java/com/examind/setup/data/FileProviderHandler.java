/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2026 Geomatys.
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
package com.examind.setup.data;

import com.examind.dto.fs.Service;
import static com.examind.setup.DatasourceUtilities.getOrCreateDatasourceForProviderFiles;
import com.examind.setup.FileSystemAnalysis;
import com.examind.setup.FileSystemSetupBusiness;
import static com.examind.setup.ProviderUtilities.createFileProvider;
import static com.examind.setup.ProviderUtilities.getProviderFileFilter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.constellation.api.PathStatus;
import static org.constellation.api.PathStatus.ERROR;
import static org.constellation.api.PathStatus.INTEGRATED;
import static org.constellation.api.PathStatus.MODIFIED;
import static org.constellation.api.PathStatus.NO_DATA;
import static org.constellation.api.PathStatus.PENDING;
import static org.constellation.api.PathStatus.REMOVED;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author glegal
 */
public class FileProviderHandler extends FSProviderHandler {
    
    public FileProviderHandler(FileSystemAnalysis.ProviderWithPath pwp, Map<String, List<Service>> asyncInfos, FileSystemSetupBusiness parent) {
         super(pwp, asyncInfos, parent);
    }

    @Override
    public Integer createProviders(boolean diffMode) {
        try {
            int dsrcId = getOrCreateDatasourceForProviderFiles(parent.datasourceBusiness, pwp, diffMode);
            List<DataSourceSelectedPath> files = parent.datasourceBusiness.getSelectedPath(dsrcId, Integer.MAX_VALUE);
            
            Integer dsId = parent.datasetBusiness.getOrCreateDataset(dataset, null);
            for (DataSourceSelectedPath dsp : files) {
                PathStatus newStatus;
                Integer pid = null;
                try {
                    Path p = parent.datasourceBusiness.getDatasourcePath(dsp.getDatasourceId(), dsp.getPath());

                    // Create provider
                    pid = createFileProvider(pwp.provider, parent.providerBusiness, p.toUri());

                    // Generate data.
                    generateDatas(pid, dsId, asyncInfos, true);
                    
                    newStatus = INTEGRATED;
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error while creating provider for file: " + pwp.ymlFile.getFileName().toString() + " data file: " + dsp.getPath(), ex);
                    newStatus = ERROR;
                }
                parent.datasourceBusiness.updatePathStatusAndProvider(dsp.getDatasourceId(), dsp.getPath(), newStatus, pid);
            }
            return dsrcId;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while creating provider for file: " + pwp.ymlFile.getFileName().toString(), ex);
        }
        return null;
    }
    
    
    @Override
    public void handleProviderFileChanges(Integer datasourceFileID) throws ConstellationException {
        
        Predicate<Path> fileFilter = getProviderFileFilter(pwp.provider);
        parent.datasourceBusiness.scanForModification(datasourceFileID, fileFilter);
        parent.datasourceBusiness.recordSelectedPath(datasourceFileID, true);
        
        int datasetId = parent.datasetBusiness.getOrCreateDataset(dataset, null);
        
        List<DataSourceSelectedPath> paths = parent.datasourceBusiness.getSelectedPath(datasourceFileID, Integer.MAX_VALUE);
        for (DataSourceSelectedPath path : paths) {
            
            Path p = parent.datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
            switch (PathStatus.valueOf(path.getStatus())) {
                case PENDING -> {
                    PathStatus newStatus;
                    Integer pid = null;
                    try {

                        // Create provider
                        pid = createFileProvider(pwp.provider, parent.providerBusiness, p.toUri());

                        // Generate data.
                        generateDatas(pid, datasetId, asyncInfos, true);

                        newStatus = INTEGRATED;
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while create new provider for file : " + p.toString(), ex);
                        newStatus = ERROR;
                    }
                    parent.datasourceBusiness.updatePathStatusAndProvider(path.getDatasourceId(), path.getPath(), newStatus, pid);
                }
                case MODIFIED -> {
                    System.out.println("TODO modify in provider");
                }

                case REMOVED -> {
                    parent.providerBusiness.removeProvider(path.getProviderId());
                    parent.datasourceBusiness.removePath(datasourceFileID, path.getPath());
                }
                case ERROR -> {
                    // idk
                }
                case INTEGRATED -> {
                    // nothing to do
                }
                case NO_DATA -> {
                    // idk
                }
            }
        }
    }
}
