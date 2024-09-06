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
import java.util.logging.Level;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.process.DatasetProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.data.DataInfoCacheProcessDescriptor.DATASET;
import static org.constellation.process.data.DataInfoCacheProcessDescriptor.DATA_ERRORS;
import static org.constellation.process.data.DataInfoCacheProcessDescriptor.DATA_SUCCESSES;
import static org.constellation.process.data.DataInfoCacheProcessDescriptor.REFRESH;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DataInfoCacheProcess extends AbstractCstlProcess {

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    public DataInfoCacheProcess(ProcessDescriptor desc, ParameterValueGroup parameter) {
        super(desc, parameter);
    }

    @Override
    protected void execute() throws ProcessException {

        final Boolean refresh   = inputParameters.getValue(REFRESH);
        final DatasetProcessReference dataset = inputParameters.getValue(DATASET);
        long dataErrors = 0, successes = 0;

        List<Integer> idsToProcess;
        if (dataset != null) {
            idsToProcess = dataBusiness.getDataIdsFromDatasetId(dataset.getId(), true, false);
        } else {
            idsToProcess = new ArrayList<>();
            for (Integer pid : providerBusiness.getProviderIdsAsInt()) {
                idsToProcess.addAll(providerBusiness.getDataIdsFromProviderId(pid, null, true, false));
            }
        }
        int total = idsToProcess.size();
        float part = 100f / total;
        int i = 1;
        for (Integer dataId : idsToProcess) {
            try {
                fireProgressing(i + " / " + total, i * part, false);
                dataBusiness.cacheDataInformation(dataId, refresh);
                successes++;
            } catch (ConstellationException ex) {
                String msg = "Error while caching informations for data: " + dataId;
                LOGGER.log(Level.FINE, msg, ex);
                fireWarningOccurred(msg, i * part, ex);
                dataErrors++;
            }
            i++;
        }

        outputParameters.getOrCreate(DATA_ERRORS).setValue(dataErrors);
        outputParameters.getOrCreate(DATA_SUCCESSES).setValue(successes);
    }

}
