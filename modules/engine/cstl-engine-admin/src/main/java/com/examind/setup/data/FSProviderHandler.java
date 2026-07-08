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

import com.examind.dto.fs.Provider;
import com.examind.dto.fs.Service;
import com.examind.setup.FileSystemAnalysis.ProviderWithPath;
import com.examind.setup.FileSystemSetupBusiness;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author glegal
 */
public abstract class FSProviderHandler {
    
    protected static final Logger LOGGER = Logger.getLogger("com.examind.setup.data");
    protected ProviderWithPath pwp;
    protected Map<String, List<Service>> asyncInfos;
    protected final String dataset;
    
    // for access to other business
    protected final FileSystemSetupBusiness parent;
    
    public FSProviderHandler(ProviderWithPath pwp, Map<String, List<Service>> asyncInfos, FileSystemSetupBusiness parent) {
        this.pwp = pwp;
        this.asyncInfos = asyncInfos;
        this.dataset = pwp.provider.getDataset();
        this.parent = parent;
    }
    
    /**
     * Create and store the provider(s) for this handler.
     * It may also create a new examind datasource for the files involved in this provider(s), if so, the datasource id will be returned.
     * 
     * It can return:
     *  - The examind datasource id for the files.
     *  - The value -1 if no datasource has been created (normal for computed provider handler for example)
     *  - {@code null} if an error occurs.
     * 
     * @param diffMode
     * @return An examind datasource id or {@code null}
     */
    public abstract Integer createProviders(boolean diffMode);
    
    public void handleProviderFileChanges(Integer datasourceFileID) throws ConstellationException {
        
    }
    
    public Integer updateProviders(Integer dsFileId, boolean diffMode) throws ConstellationException {
        // for now we do an simple remove/create 
        // TODO update metadata
        // TODO linked files?
        removeProviders(dsFileId);
        return createProviders(diffMode);
    }
    
    private void removeProviders(Integer dsFileId) throws ConstellationException {
        List<DataSourceSelectedPath> paths = parent.datasourceBusiness.getSelectedPath(dsFileId, Integer.MAX_VALUE);
        for (DataSourceSelectedPath path : paths) {
            Integer pid = path.getProviderId();
            if (pid != null && pid != -1) {
                parent.providerBusiness.removeProvider(pid);
            }
        }
        parent.datasourceBusiness.delete(dsFileId);
    }
    
    protected void generateDatas(int pid, Provider conf, Map<String, List<Service>> asyncInfos, boolean create) throws ConstellationException {
        int datasetId = parent.datasetBusiness.getOrCreateDataset(dataset, null);
        generateDatas(pid, datasetId, asyncInfos, create);
    }
    
    protected void generateDatas(int pid, int dsId, Map<String, List<Service>> asyncInfos, boolean create) throws ConstellationException {
        parent.providerBusiness.createOrUpdateData(pid, dsId, true, false, null);

        if (create) {
            List<Integer> dataIds = parent.providerBusiness.getDataIdsFromProviderId(pid);
            parent.dataBusiness.acceptDatas(dataIds, null, false);
        }
        
        // ASYNC MODE Add layer and reload needed service
        if (asyncInfos != null && dataset != null) {
            parent.asyncServiceReload(dataset, asyncInfos);
        }
    }
    
}
