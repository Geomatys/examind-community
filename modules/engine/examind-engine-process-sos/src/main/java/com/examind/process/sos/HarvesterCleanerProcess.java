/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package com.examind.process.sos;

import static com.examind.process.sos.HarvesterCleanerDescriptor.DATA_FOLDER;
import static com.examind.process.sos.HarvesterCleanerDescriptor.STORE_ID;
import static com.examind.process.sos.HarvesterCleanerDescriptor.OBS_TYPE;
import com.examind.sensor.component.SensorServiceBusiness;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.SensorReference;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;
import static com.examind.store.observation.csvflat.CsvFlatUtils.csvFlatProviderForPath;

/**
 *
 * @author guilhem
 */
public class HarvesterCleanerProcess extends AbstractCstlProcess {
    
    @Autowired
    private IDatasourceBusiness datasourceBusiness;
    
    @Autowired
    private IProviderBusiness providerBusiness;
    
    @Autowired
    private ISensorBusiness sensorBusiness;
    
    @Autowired
    private SensorServiceBusiness sensorServBusiness;
    
    public HarvesterCleanerProcess(final ProcessDescriptor desc, final ParameterValueGroup input) {
        super(desc,input);
    }

    @Override
    protected void execute() throws ProcessException {
        LOGGER.info("executing sensor havest cleaner process");
        
        final String sourceFolderStr = inputParameters.getValue(DATA_FOLDER);
        final String storeId = inputParameters.getValue(STORE_ID);
        final String observationType = inputParameters.getValue(OBS_TYPE);
        
        
        boolean csvFlatMulti = storeId.equals("observationCsvFlatFile") && observationType == null;
        
        final int dsId;
        DataSource ds = datasourceBusiness.getByUrl(sourceFolderStr);
        if (ds == null) {
            LOGGER.log(Level.INFO, "No datasource existing for path: {0}", sourceFolderStr);
            return;
        } else {
            dsId = ds.getId();
        }

        // remove previous integration
        try {
            Set<Integer> providers = new HashSet<>();
            List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(dsId, Integer.MAX_VALUE);
            for (DataSourceSelectedPath path : paths) {
                if (csvFlatMulti) {
                    // hack to remove the multiple providers created in flat multi mode.
                    if (path.getProviderId() != null && path.getProviderId() != -1) {
                        ProviderBrief pr = providerBusiness.getProvider(path.getProviderId());
                        if (pr != null) {
                            providers.addAll(csvFlatProviderForPath(pr.getConfig(), providerBusiness));
                        } else {
                            throw new ProcessException("Inconsistency in database, a datasource path point to an unexisting provider", this);
                        }
                    }
                } else {
                    if (path.getProviderId() != null && path.getProviderId() != -1) {
                        providers.add(path.getProviderId());
                    }
                }
            }

            // remove data
            Set<SensorReference> sensors = new HashSet<>();
            for (Integer pid : providers) {
                for (Integer dataId : providerBusiness.getDataIdsFromProviderId(pid)) {
                    sensors.addAll(sensorBusiness.getByDataId(dataId));
                }
                providerBusiness.removeProvider(pid);
            }

            // remove sensors
            for (SensorReference sid : sensors) {

                // unlink from SOS
                for (Integer service : sensorBusiness.getLinkedServiceIds(sid.getId())) {
                    sensorServBusiness.removeSensor(service, sid.getIdentifier());
                }

                // remove sensor
                sensorBusiness.delete(sid.getId());
            }

            datasourceBusiness.clearSelectedPaths(dsId);

            // remove datasource
            datasourceBusiness.delete(dsId);

        } catch (ConstellationException ex) {
            throw new ProcessException("Error while removing previous insertion.", this, ex);
        }
    
    }
    
    
}
