/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2023 Geomatys.
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IDatasourceBusiness.PathStatus;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.importdata.ResourceAnalysisV3;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.dto.process.StyleProcessReference;
import org.constellation.dto.process.UserProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.data.ImportDataAutoDescriptor.DATA_PATH;
import static org.constellation.process.data.ImportDataAutoDescriptor.DATASET_ID;
import static org.constellation.process.data.ImportDataAutoDescriptor.STYLE_ID;
import static org.constellation.process.data.ImportDataAutoDescriptor.USER_ID;
import static org.constellation.process.data.ImportDataAutoDescriptor.OUT_CONFIGURATION;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ImportDataAuto extends AbstractCstlProcess {

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    @Autowired
    private IConfigurationBusiness configBusiness;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IStyleBusiness styleBusiness;

    public ImportDataAuto(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }


    @Override
    protected void execute() throws ProcessException {

        Integer datasourceId = null;
        Integer userId = 1;
        UserProcessReference user  = inputParameters.getValue(USER_ID);
        if (user != null) {
            userId = user.getId();
        }
        final Path dataPath                   = inputParameters.getValue(DATA_PATH);
        final DatasetProcessReference dataset = inputParameters.getValue(DATASET_ID);
        final StyleProcessReference style     = inputParameters.getValue(STYLE_ID);

        try {

            /**
             * 1 -  Build a datasource for file analyze.
             * - if the file is in the config directory (we assume in the upload dir), the file will be copied. This is the upload mode.
             * - if the path is elsewhere on the system, we use directly the file where it is. This is the server file mode.
             */
            final Path configDir       = configBusiness.getConfigurationDirectory();
            final boolean remoteFile   = !dataPath.startsWith(configDir);
            final boolean permanent    = false; // we don't keep the datasource in this process.
            final String datasourceURL = dataPath.toUri().toString();
            final String scheme        = dataPath.toUri().getScheme();
            final DataSource ds        = new DataSource(null, scheme, datasourceURL, null, null, null, remoteFile, System.currentTimeMillis(), IDatasourceBusiness.AnalysisState.NOT_STARTED.name(), null, permanent);
            datasourceId               = datasourceBusiness.create(ds);

            /**
             * 2 - We launch the analyze on the datasource files, to look for matching providers.
             * As we are in "single" and "magic" import, we need to choose the matching store that we will use to import one (or more) file.
             *
             * We made a partial hard coded method to choose the "best" provider.
             */
            Map<String, Set<String>> storeFormats = datasourceBusiness.computeDatasourceStores(datasourceId, false, true, false);
            if (storeFormats.isEmpty()) {
                throw new ProcessException("No store found to read this data file", this);
            }
            final String storeId = magicProviderChoose(storeFormats.keySet());

            // select all the files that match the choosen provider
            datasourceBusiness.recordSelectedPath(datasourceId, storeId, false);
            fireProgressing("Datasource analysis completed.", 10, false);

            /**
             * 3 - Iterate on each selected files, and integrate them
             */
            List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(datasourceId, Integer.MAX_VALUE);
            LOGGER.log(Level.INFO, "{0} files to integrate.", paths.size());
            List<Integer> outputDatas = new ArrayList<>();
            int i = 1;
            float part = 90 / (float) paths.size();
            for (DataSourceSelectedPath p : paths) {
                checkDismissed();
                List<Integer> storeDatas = new ArrayList<>();
                /**
                 * 3.1 - Build provider/data for each selected file.
                 * 
                 * The status "NO_DATA,"ERROR", "INTEGRATED", "COMPLETED", "REMOVED" should never happen in this case as we are in a fresh datasource.
                 * All the files should be in a "PENDING" state
                 */
                if ("PENDING".equals(p.getStatus())) {

                    fireProgressing("Integrating data file: " + p.getPath() + " (" + i + "/" + paths.size() + ")", 10 + (i*part), false);

                    final ProviderConfiguration provConfig = new ProviderConfiguration("data-store", storeId);
                    provConfig.getParameters().put("location", null);

                    ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, datasourceId, provConfig, true, dataset.getId(), userId);
                    for (ResourceAnalysisV3 rs : store.getResources()) {
                        storeDatas.add(rs.getId());
                    }
                } else {
                    fireProgressing("Unexpected data file status: " + p.getStatus() + " for file "+ p.getPath() + " (" + i + "/" + paths.size() + ")", 10 + (i*part), false);
                }

                /**
                 * 3.2 - Integrate each data provided by the current file(s), link the style.
                 */
                LOGGER.log(Level.FINE, "{0} datas to integrate.", storeDatas.size());
                final List<Integer> failedDatas = new ArrayList<>();
                for (Integer dataId : storeDatas) {
                    if (style != null) {
                        styleBusiness.linkToData(style.getId(), dataId);
                    }
                    try {
                        dataBusiness.acceptData(dataId, userId, false);

                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error while accepting data:" + dataId, ex);
                        dataBusiness.removeData(dataId, false);
                        failedDatas.add(dataId);
                    }
                }
                LOGGER.log(Level.INFO, "Data file: " + p.getPath() + " integrated (" + i + "/" + paths.size() + ")");
                storeDatas.removeAll(failedDatas);
                outputDatas.addAll(storeDatas);
                datasourceBusiness.updatePathStatus(datasourceId, p.getPath(), PathStatus.COMPLETED.name());
                i++;
            }

            outputParameters.getOrCreate(OUT_CONFIGURATION).setValue(outputDatas);
            LOGGER.info("Data import auto completed");

        } catch (ConstellationException ex) {
            throw new ProcessException("Error while importing data", this, ex);
        } finally {
            // Remove datasource
            if (datasourceId != null) {
                try {datasourceBusiness.delete(datasourceId);} catch (ConstellationException ex) {}
            }
        }
    }

    /**
     * Arbitrary choose the provider. Only geotiff and shapefile are hardcoded for now, this method may should be completed.
     * @param providerIds available providers (not empty).
     * @return A choosen provider id.
     */
    private String magicProviderChoose(Set<String> providerIds) {
        if (providerIds.contains("GeoTIFF")) {
            return "GeoTIFF";
        } else if (providerIds.contains("shapefile")) {
            return "shapefile";
        }
        // we rely on luck and use the first provider found
        return providerIds.iterator().next();
    }
}
