/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.util.ImageStatisticSerializer;
import org.constellation.api.DataType;
import org.constellation.business.IDataCoverageJob;
import org.constellation.dto.Data;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.geotoolkit.data.multires.MultiResolutionResource;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.util.StoreUtilities;
import org.geotoolkit.metadata.ImageStatistics;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.processing.ProcessListenerAdapter;
import org.geotoolkit.processing.coverage.statistics.Statistics;
import static org.geotoolkit.processing.coverage.statistics.StatisticsDescriptor.OUTCOVERAGE;
import org.geotoolkit.util.NamesExt;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 *
 * @author Quentin Boileau (Geomatys)
 */
@Component
@Primary
public class DataCoverageJob implements IDataCoverageJob {

    /**
     * Used for debugging purposes.
     */
    protected static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    public static final String STATE_PENDING    = "PENDING";
    public static final String STATE_ERROR      = "ERROR";
    public static final String STATE_COMPLETED  = "COMPLETED";
    public static final String STATE_PARTIAL    = "PARTIAL";

    /**
     * Injected data repository.
     */
    @Inject
    private DataRepository dataRepository;

    /**
     * Injected data repository.
     */
    @Inject
    private ProviderRepository providerRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public void asyncUpdateDataStatistics(final int dataId) {

        Data data = dataRepository.findById(dataId);
        if (data == null) {
            LOGGER.log(Level.WARNING, "Can't compute coverage statistics on data id " + dataId +
                    " because data is not found in database.");
            return;
        }
        try {
            if (DataType.COVERAGE.name().equals(data.getType())
                    && (data.getRendered() == null || !data.getRendered())
                    && data.getStatsState() == null) {

                LOGGER.log(Level.INFO, "Start computing data " + dataId + " "+data.getName()+" coverage statistics.");

                data.setStatsState(STATE_PENDING);
                updateData(data);

                if (providerRepository.existById(data.getProviderId())) {
                    final DataProvider dataProvider = DataProviders.getProvider(data.getProviderId());
                    final DataStore store = dataProvider.getMainStore();

                    GenericName name = NamesExt.create(data.getNamespace(), data.getName());
                    if (data.getNamespace() == null || data.getNamespace().isEmpty()) {
                        name = NamesExt.create(data.getName());
                    }
                    final Resource res = StoreUtilities.findResource(store, name.toString());

                    if (res instanceof MultiResolutionResource) {
                        //pyramid, too large to compute statistics
                        data.setStatsState(STATE_PARTIAL);
                        updateData(data);
                        return;
                    }

                    if (res instanceof GridCoverageResource) {
                        final GridCoverageResource covRef = (GridCoverageResource) res;

                        // Hack compute statistic from 5% of the first slice
                        try {
                            GridGeometry gg = covRef.getGridGeometry();
                            GridExtent extent = gg.getExtent();
                            int[] subSample = new int[extent.getDimension()];
                            subSample[0] = Math.round(extent.getSize(0) * 0.05f);
                            subSample[1] = Math.round(extent.getSize(1) * 0.05f);
                            for (int i = 2; i < extent.getDimension(); i++) {
                                subSample[i] = Math.toIntExact(extent.getSize(i));
                            }
                            gg = gg.derive().subsample(subSample).build();

                            final GridCoverage cov = covRef.read(gg);
                            final org.geotoolkit.process.Process process = new Statistics(cov, false);
                            process.addListener(new DataStatisticsListener(dataId));
                            process.call();

                        } catch(Throwable ex) {
                            //we tryed
                        }
                    }

                } else {
                    throw new ConfigurationException("Provider has been removed before the end of statistic computation");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during coverage statistic update for data " + dataId + " "+data.getName() + " : " + e.getMessage(), e);

            //update data
            Data lastData = dataRepository.findById(dataId);
            if (lastData != null && !lastData.getStatsState().equals(STATE_ERROR)) {
                data.setStatsState(STATE_ERROR);
                //data.setStatsResult(Exceptions.formatStackTrace(e));
                updateData(data);
            }
        }
    }

    private void updateData(final Data data) {
        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                dataRepository.updateStatistics(data.getId(), data.getStatsResult(), data.getStatsState());

                // forward original data statistic result and state to pyramid conform child.
                final List<Data> dataChildren = dataRepository.getDataLinkedData(data.getId());
                for (Data dataChild : dataChildren) {
                    if (dataChild.getSubtype().equalsIgnoreCase("pyramid") && !dataChild.getRendered()) {
                        dataRepository.updateStatistics(dataChild.getId(), data.getStatsResult(), data.getStatsState());
                    }
                }
            }
        });
    }

    /**
     * ProcessListener that will update data record in database.
     */
    private class DataStatisticsListener extends ProcessListenerAdapter {

        private int dataId;

        public DataStatisticsListener(int dataId) {
            this.dataId = dataId;
        }

        @Override
        public void progressing(ProcessEvent event) {
            if (event.getOutput() != null) {
                final Data data = getData();
                if (data != null) {
                    try {
                        data.setStatsState(STATE_PARTIAL);
                        data.setStatsResult(statisticsAsString(event));
                        updateData(data);
                    } catch (JsonProcessingException e) {
                        data.setStatsState(STATE_ERROR);
                        data.setStatsResult("Error during statistic serializing.");
                        updateData(data);
                    }
                }
            }
        }

        @Override
        public void completed(ProcessEvent event) {
            final Data data = getData();
            if (data != null) {
                try {
                    data.setStatsState(STATE_COMPLETED);
                    data.setStatsResult(statisticsAsString(event));
                    updateData(data);
                    LOGGER.log(Level.INFO, "Data " + dataId + " " + data.getName() + " coverage statistics completed.");
                } catch (JsonProcessingException e) {
                    data.setStatsState(STATE_ERROR);
                    data.setStatsResult("Error during statistic serializing.");
                    updateData(data);
                }
            }
        }

        @Override
        public void failed(ProcessEvent event) {
            final Data data = getData();
            if (data != null) {
                data.setStatsState(STATE_ERROR);
                //data.setStatsResult(Exceptions.formatStackTrace(event.getException()));
                updateData(data);
                Exception exception = event.getException();
                LOGGER.log(Level.WARNING, "Error during coverage statistic update for data " + dataId +
                        " " + data.getName() + " : " + exception.getMessage(), exception);
            }

        }

        private Data getData() {
            return dataRepository.findById(dataId);
        }

        /**
         * Serialize Statistic in JSON
         * @param event
         * @return JSON String or null if event output is null.
         * @throws JsonProcessingException
         */
        private String statisticsAsString(ProcessEvent event) throws JsonProcessingException {
            final ParameterValueGroup out = event.getOutput();
            if (out != null) {
                final ImageStatistics statistics = Parameters.castOrWrap(out).getMandatoryValue(OUTCOVERAGE);

                final ObjectMapper mapper = new ObjectMapper();
                final SimpleModule module = new SimpleModule();
                module.addSerializer(ImageStatistics.class, new ImageStatisticSerializer()); //custom serializer
                mapper.registerModule(module);
                //mapper.enable(SerializationFeature.INDENT_OUTPUT); //json pretty print
                return mapper.writeValueAsString(statistics);
            }
            return null;
        }
    }
}
