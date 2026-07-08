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

import com.examind.community.storage.sql.CoverageSQLProvider.CoverageSQLStore;
import com.examind.dto.fs.Provider;
import com.examind.dto.fs.Service;
import static com.examind.setup.DatasourceUtilities.getOrCreateDatasourceForProviderFiles;
import static com.examind.setup.DatasourceUtilities.getOrCreateSQLDatasource;
import com.examind.setup.FileSystemAnalysis.ProviderWithPath;
import com.examind.setup.FileSystemSetupBusiness;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.constellation.dto.DataSourceSelectedPath;
import static com.examind.setup.ProviderUtilities.createCSQLProvider;
import static com.examind.setup.ProviderUtilities.getProviderFileFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Predicate;
import org.apache.sis.storage.DataStoreException;
import org.constellation.api.PathStatus;
import static org.constellation.api.PathStatus.ERROR;
import static org.constellation.api.PathStatus.INTEGRATED;
import static org.constellation.api.PathStatus.MODIFIED;
import static org.constellation.api.PathStatus.NO_DATA;
import static org.constellation.api.PathStatus.PENDING;
import static org.constellation.api.PathStatus.REMOVED;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.Data;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;

/**
 *
 * @author glegal
 */
public class CSQLProviderhandler extends FSProviderHandler {
    
    private final String productName;
    private final String subDataType;
    private final boolean asChild;
    private final boolean worldGG;
    private final Double worldGGRes;
    
    public CSQLProviderhandler(ProviderWithPath pwp, Map<String, List<Service>> asyncInfos, FileSystemSetupBusiness parent) {
        super(pwp, asyncInfos, parent);
         
        productName = pwp.provider.getAdvancedParameter("productName", (String) null);
        subDataType = pwp.provider.getAdvancedParameter("subDataType", (String) null);
        asChild     = pwp.provider.getAdvancedParameter("asChild", false);
        worldGG     = pwp.provider.getAdvancedParameter("worldGG", false);
        worldGGRes  = pwp.provider.getAdvancedParameter("worldGGResolution", (Double) null);
         
    }
    
    @Override
    public Integer createProviders(boolean diffMode) {
        try {

            Integer datasourceId = getOrCreateSQLDatasource(parent.datasourceBusiness, pwp.provider);

            int dsrcId = getOrCreateDatasourceForProviderFiles(parent.datasourceBusiness, pwp, diffMode);
            List<DataSourceSelectedPath> files = parent.datasourceBusiness.getSelectedPath(dsrcId, Integer.MAX_VALUE);

            Integer datasetId = parent.datasetBusiness.getOrCreateDataset(dataset, null);
        
            final String providerIdentifier = "csql-" + datasourceId;
            Integer prId = parent.providerBusiness.getIDFromIdentifier(providerIdentifier);
        
            // we keep only one provider by datasource
            if (prId == null) {
                prId = createCSQLProvider(parent.providerBusiness, providerIdentifier, datasourceId);
            }
        
            DataProvider provider = DataProviders.getProvider(prId);
            CoverageSQLStore store = (CoverageSQLStore) provider.getMainStore();
        
            for (DataSourceSelectedPath dsp : files) {
                // as we are in creation mode, we assume that all the files are in pending status
                try {
                    Path p = parent.datasourceBusiness.getDatasourcePath(dsp.getDatasourceId(), dsp.getPath());
                    store.createOrAddToProduct(productName, worldGG, worldGGRes, asChild, subDataType, p);

                    parent.datasourceBusiness.updatePathStatusAndProvider(dsp.getDatasourceId(), dsp.getPath(), INTEGRATED, prId);
                } catch (DataStoreException ex) {
                    LOGGER.log(Level.WARNING, "Error while integrating file into coverage sql: " + dsp.getPath() + " provider: " + pwp.provider.getIdentifier(), ex);
                    parent.datasourceBusiness.updatePathStatusAndProvider(dsp.getDatasourceId(), dsp.getPath(), ERROR, prId);
                }
            }
            provider.reload();
            generateDatas(prId, datasetId, asyncInfos, true);
            return dsrcId;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider file: " + pwp.ymlFile.getFileName().toString(), ex);
        }
        return null;
    }

    @Override
    public void handleProviderFileChanges(Integer datasourceFileID) throws ConstellationException {
        // look for CSQL provider
        Integer datasourceId = getOrCreateSQLDatasource(parent.datasourceBusiness, pwp.provider);
        final String providerIdentifier = "csql-" + datasourceId;
        Integer prId = parent.providerBusiness.getIDFromIdentifier(providerIdentifier);
        DataProvider provider = DataProviders.getProvider(prId);
        CoverageSQLStore store = (CoverageSQLStore) provider.getMainStore();
        
        Predicate<Path> fileFilter = getProviderFileFilter(pwp.provider);
        parent.datasourceBusiness.scanForModification(datasourceFileID, fileFilter);
        parent.datasourceBusiness.recordSelectedPath(datasourceFileID, true);
        
        int datasetId = parent.datasetBusiness.getOrCreateDataset(dataset, null);
        
        boolean hasChanges = false;
        
        List<DataSourceSelectedPath> paths = parent.datasourceBusiness.getSelectedPath(datasourceFileID, Integer.MAX_VALUE);
        for (DataSourceSelectedPath path : paths) {
            
            Path p = parent.datasourceBusiness.getDatasourcePath(path.getDatasourceId(), path.getPath());
            switch (PathStatus.valueOf(path.getStatus())) {
                case PENDING -> {
                    PathStatus newStatus;
                    try {
                        store.createOrAddToProduct(productName, worldGG, worldGGRes, asChild, subDataType, p);
                        newStatus = INTEGRATED;
                        hasChanges = true;
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while inserting  new file in coverage-sql provider : " + p.toString(), ex);
                        newStatus = ERROR;
                    }
                    parent.datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
                }
                case MODIFIED -> {
                    PathStatus newStatus;
                    try {
                        store.removeFromProduct(productName, p);
                        store.createOrAddToProduct(productName, worldGG, worldGGRes, asChild, subDataType, p);
                        newStatus = INTEGRATED;
                        hasChanges = true;
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while inserting  modified file in coverage-sql provider : " + p.toString(), ex);
                        newStatus = ERROR;
                    }
                    parent.datasourceBusiness.updatePathStatus(path.getDatasourceId(), path.getPath(), newStatus);
                }

                case REMOVED -> {
                    try {
                        store.removeFromProduct(productName, p);
                        hasChanges = true;
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while removing file from coverage-sql provider : " + p.toString(), ex);
                    }
                    parent.datasourceBusiness.removePath(datasourceFileID, path.getPath());
                }
                case ERROR -> {
                    // idk, do we need tro try to re-insert it?
                    LOGGER.log(Level.WARNING, "File still in insertion error for coverage-sql provider : " + p.toString());
                }
                case INTEGRATED -> {
                    // nothing to do
                }
                case NO_DATA -> {
                    // idk
                    LOGGER.log(Level.WARNING, "File still produce no data for coverage-sql provider : " + p.toString());
                }
            }
        }
        
        if (hasChanges) {
            // Generate data.
            provider.reload();
            generateDatas(prId, datasetId, asyncInfos, false);

        }
    }

    @Override
    protected void generateDatas(int pid, int dsId, Map<String, List<Service>> asyncInfos, boolean create) throws ConstellationException {
        parent.providerBusiness.createOrUpdateData(pid, null, false, false, null);

        // with cache activated, the createOrUpdateData may not update the cache if the data was already present
        boolean cached = Application.getBooleanProperty(AppProperty.EXA_CACHE_DATA_INFO, false);
        
        if (create | cached) {
            List<Integer> productIds = new ArrayList<>();
            List<Data> datas = parent.dataRepository.findByProviderId(pid);
            for (Data data : datas) {
                if (data.getName().equals(productName)     ||  // single product
                    data.getNamespace().equals(productName)) { // aggregated product
                    productIds.add(data.getId());
                    
                    if (create) parent.dataBusiness.updateDataDataSetId(data.getId(), dsId);
                    if (cached) parent.dataBusiness.cacheDataInformation(data.getId(), true);
                }
            }
            parent.dataBusiness.acceptDatas(productIds, null, false);
        }
        
        

        // ASYNC MODE Add layer and reload needed service
        if (asyncInfos != null && dataset != null) {
            parent.asyncServiceReload(dataset, asyncInfos);
        }
    }
    
}
