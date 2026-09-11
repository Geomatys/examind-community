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
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasetBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IFileSystemSetupBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.exception.ConstellationException;
import org.constellation.repository.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class to handle providers updates.
 * 
 * @author glegal
 */
public abstract class FSProviderHandler {
    
    protected static final Logger LOGGER = Logger.getLogger("com.examind.setup.data");
    protected ProviderWithPath pwp;
    protected Map<String, List<Service>> asyncInfos;
    protected final String dataset;
    
    @Autowired
    protected IDatasourceBusiness datasourceBusiness;
    
    @Autowired
    protected IDataBusiness dataBusiness;
    
    @Autowired
    protected IProviderBusiness providerBusiness;
    
    @Autowired
    protected IDatasetBusiness datasetBusiness;
    
    @Autowired
    protected DataRepository dataRepository;
    
    @Autowired
    protected IFileSystemSetupBusiness fsSetupBusiness;
    
    public FSProviderHandler(ProviderWithPath pwp, Map<String, List<Service>> asyncInfos) {
        SpringHelper.injectDependencies(this);
        this.pwp = pwp;
        this.asyncInfos = asyncInfos;
        this.dataset = pwp.provider.getDataset();
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
    
    /**
     * Handle changes in files involved in this provider handler.
     * 
     * @param datasourceFileID The datasource identifier of the providers files.
     * @throws ConstellationException 
     */
    public void handleProviderFileChanges(Integer datasourceFileID) throws ConstellationException {
        // does nothing by default (for non files provider for example).
    }
    
    /**
     * Handle changes provider handler configuration.
     * 
     * @param dsFileId The datasource identifier of the providers files.
     * @param diffMode Flag for diff mode.
     * @return
     * @throws ConstellationException 
     */
    public Integer updateProviders(Integer dsFileId, boolean diffMode) throws ConstellationException {
        // for now we do an simple remove/create 
        // TODO update metadata
        // TODO linked files?
        removeProviders(dsFileId);
        return createProviders(diffMode);
    }
    
    /**
     * Remove the providers for this handler.
     * 
     * @param dsFileId The datasource identifier of the providers files.
     * @throws ConstellationException 
     */
    protected void removeProviders(Integer dsFileId) throws ConstellationException {
        List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsFileId, Integer.MAX_VALUE);
        for (DataSourceSelectedPath path : paths) {
            Integer pid = path.getProviderId();
            if (pid != null && pid != -1) {
                providerBusiness.removeProvider(pid);
            }
        }
        datasourceBusiness.delete(dsFileId);
    }
    
    /**
     * Geneerates the datas for the specified provider
     * 
     * @param pid Provider id.
     * @param conf Provider configuration (From the yaml file).
     * @param asyncInfos Provider/service relations in case of asynchroneous mode.
     * @param create true if the datas does not already exist.
     * @throws ConstellationException 
     */
    protected void generateDatas(int pid, Provider conf, Map<String, List<Service>> asyncInfos, boolean create) throws ConstellationException {
        int datasetId = datasetBusiness.getOrCreateDataset(dataset, null);
        generateDatas(pid, datasetId, asyncInfos, create);
    }
    
    protected void generateDatas(int pid, int dsId, Map<String, List<Service>> asyncInfos, boolean create) throws ConstellationException {
        providerBusiness.createOrUpdateData(pid, dsId, true, false, null);

        if (create) {
            List<Integer> dataIds = providerBusiness.getDataIdsFromProviderId(pid);
            dataBusiness.acceptDatas(dataIds, null, false);
        }
        
        // ASYNC MODE Add layer and reload needed service
        if (asyncInfos != null && dataset != null) {
            fsSetupBusiness.asyncServiceReload(dataset, asyncInfos);
        }
    }
    
}
