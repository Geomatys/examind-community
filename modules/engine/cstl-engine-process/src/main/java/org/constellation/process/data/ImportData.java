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
package org.constellation.process.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IDatasourceBusiness.PathStatus;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.importdata.ResourceAnalysisV3;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.data.ImportDataDescriptor.DATASOURCE_ID;
import static org.constellation.process.data.ImportDataDescriptor.DATASET_ID;
import static org.constellation.process.data.ImportDataDescriptor.METADATA_MODEL_ID;
import static org.constellation.process.data.ImportDataDescriptor.PROVIDER_CONFIGURATION;
import static org.constellation.process.data.ImportDataDescriptor.STYLE_ID;
import static org.constellation.process.data.ImportDataDescriptor.USER_ID;
import static org.constellation.process.data.ImportDataDescriptor.OUT_CONFIGURATION;
import org.constellation.util.MetadataMerger;
import org.geotoolkit.process.DismissProcessException;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ImportData extends AbstractCstlProcess {

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private IStyleBusiness styleBusiness;

    public ImportData(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }


    @Override
    protected void execute() throws ProcessException {

        final Integer userId         = inputParameters.getValue(USER_ID);
        final Integer datasourceId   = inputParameters.getValue(DATASOURCE_ID);
        final Integer datasetId      = inputParameters.getValue(DATASET_ID);
        final Integer styleId        = inputParameters.getValue(STYLE_ID);
        final Integer modelId        = inputParameters.getValue(METADATA_MODEL_ID);
        final ProviderConfiguration provConfig = inputParameters.getValue(PROVIDER_CONFIGURATION);


        final DataSource ds = datasourceBusiness.getDatasource(datasourceId);
        try {
            fireProgressing("Waiting for Datasource analysis to complete...", 0, false);
            // waiting for analysis to be complete
            String analysisState = datasourceBusiness.getDatasourceAnalysisState(datasourceId);
            if (analysisState == null || IDatasourceBusiness.AnalysisState.NOT_STARTED.name().equals(analysisState)) {
                throw new ProcessException("Datasource analysis has not been launched. We cannot import data", this);
            }

            final String pendingState = IDatasourceBusiness.AnalysisState.PENDING.name();
            while (pendingState.equals(datasourceBusiness.getDatasourceAnalysisState(datasourceId))) {
                synchronized (this) {
                    checkDismissed();
                    try {
                        wait(200);
                    } catch (InterruptedException ex) {
                        throw new DismissProcessException("Process interrupted while waiting for analysis", this, ex);
                    }
                }
            }

            fireProgressing("Datasource analysis completed.", 10, false);

             datasourceBusiness.recordSelectedPath(datasourceId, false);
             List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(datasourceId, Integer.MAX_VALUE);

             LOGGER.log(Level.INFO, "{0} files to integrate.", paths.size());
             List<Integer> outputDatas = new ArrayList<>();
             int i = 1;
             float part = 90 / (float) paths.size();
             for (DataSourceSelectedPath p : paths) {
                 checkDismissed();
                 List<Integer> storeDatas = new ArrayList<>();
                 if ("INTEGRATED".equals(p.getStatus())) {
                    fireProgressing("Integrating sampled data file:" + p.getPath() + " (" + i + "/" + paths.size() + ")", 10 + (i*part), false);
                    List<Integer> datas = providerBusiness.getDataIdsFromProviderId(p.getProviderId());
                    for (Integer dataId : datas) {
                         storeDatas.add(dataId);
                         dataBusiness.updateDataDataSetId(dataId, datasetId);
                     }
                 } else if ("NO_DATA".equals(p.getStatus()) ||"ERROR".equals(p.getStatus())) {
                     fireProgressing("No data / Error in file: " + p.getPath() + " (" + i + "/" + paths.size() + ")", 10 + (i*part), false);
                 } else {
                     fireProgressing("Integrating data file: " + p.getPath() + " (" + i + "/" + paths.size() + ")", 10 + (i*part), false);
                     ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, ds.getId(), provConfig, true, datasetId, userId);
                     for (ResourceAnalysisV3 rs : store.getResources()) {
                         storeDatas.add(rs.getId());
                     }
                 }
                 LOGGER.log(Level.FINE, "{0} datas to integrate.", storeDatas.size());
                 final List<Integer> failedDatas = new ArrayList<>();
                 for (Integer dataId : storeDatas) {
                     if (styleId != null) {
                         styleBusiness.linkToData(styleId, dataId);
                     }
                     try {
                        dataBusiness.acceptData(dataId, userId, false);

                        if (modelId != null) {
                           DataBrief brief = dataBusiness.getDataBrief(dataId, false, true);
                           List<MetadataLightBrief> metadatas = brief.getMetadatas();
                           for (MetadataLightBrief metadata : metadatas) {
                               final Object dataObj  = metadataBusiness.getMetadata(metadata.getId());
                               if (dataObj instanceof DefaultMetadata) {

                                   final DefaultMetadata modelMeta = (DefaultMetadata) metadataBusiness.getMetadata(modelId);
                                   final DefaultMetadata dataMeta = (DefaultMetadata) dataObj;
                                   modelMeta.setFileIdentifier(null);
                                   modelMeta.prune();
                                   dataMeta.prune();

                                   final MetadataMerger merger = new MetadataMerger(Locale.FRENCH);
                                   merger.copy(modelMeta, dataMeta);
                                   metadataBusiness.updateMetadata(dataMeta.getFileIdentifier(), dataMeta, dataId, null, null, userId, null, "DOC");
                               }
                           }
                        }
                    } catch (Exception ex) {
                         LOGGER.log(Level.WARNING, "Error while accepting data:" + dataId, ex);
                         dataBusiness.removeData(dataId, false);
                         failedDatas.add(dataId);
                     }
                 }
                 LOGGER.log(Level.INFO, "Data file: " + p.getPath() + " integrated (" + i + "/" + paths.size() + ")");
                 storeDatas.removeAll(failedDatas);
                 outputDatas.addAll(storeDatas);
                 outputParameters.getOrCreate(OUT_CONFIGURATION).setValue(outputDatas);
                 datasourceBusiness.updatePathStatus(ds.getId(), p.getPath(), PathStatus.COMPLETED.name());
                 i++;
             }

             LOGGER.info("Data import completed");

         } catch (ConstellationException ex) {
             throw new ProcessException("Error while importing data", this, ex);
         } finally {
             // Remove datasource
            if (datasourceId != null) {
                try {datasourceBusiness.delete(datasourceId);} catch (ConstellationException ex) {}
            }

            // Delete metadata model
            if (modelId != null) {
                try {metadataBusiness.deleteMetadata(modelId);} catch (ConstellationException ex) {}
            }
         }

    }
}
