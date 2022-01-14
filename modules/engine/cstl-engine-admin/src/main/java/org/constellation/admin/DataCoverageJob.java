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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

import org.opengis.metadata.Metadata;
import org.opengis.metadata.content.CoverageDescription;

import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.collection.BackingStoreException;

import org.geotoolkit.storage.coverage.CoverageDescriptionAdapter;
import org.geotoolkit.storage.coverage.ImageStatistics;

import org.constellation.api.DataType;
import org.constellation.business.IDataCoverageJob;
import org.constellation.business.IMetadataBusiness;
import org.constellation.dto.Data;
import org.constellation.exception.ConfigurationException;
import org.constellation.metadata.utils.MetadataFeeder;
import org.constellation.provider.DataProviders;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ProviderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static org.constellation.api.StatisticState.STATE_ERROR;
import static org.constellation.api.StatisticState.STATE_PARTIAL;
import static org.constellation.api.StatisticState.STATE_PENDING;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.CoverageData;
import org.constellation.provider.PyramidData;
import org.springframework.scheduling.annotation.Scheduled;

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
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

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

    @Inject
    private IMetadataBusiness metadataService;

    /**
     * {@inheritDoc}
     */
    @Scheduled(cron = "1 * * * * *")
    @Override
    public void updateDataStatistics() {
         boolean doAnalysis = Application.getBooleanProperty(AppProperty.DATA_AUTO_ANALYSE, Boolean.TRUE);
        if (doAnalysis) {
            computeEmptyDataStatistics(false);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void computeEmptyDataStatistics(boolean isInit) {
        final List<Data> dataList = dataRepository.findStatisticLess();

        List<Integer> dataWithoutStats = new ArrayList<>();
        for (final Data data : dataList) {
            String state = data.getStatsState();
            if (isInit) {
                //rerun statistic for error and pending states
                if ("PENDING".equalsIgnoreCase(state) || "ERROR".equalsIgnoreCase(state)) {
                    SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            dataRepository.updateStatistics(data.getId(), null, null);
                        }
                    });
                    dataWithoutStats.add(data.getId());
                }
            }

            if (state == null || state.isEmpty()) {
                dataWithoutStats.add(data.getId());
            }
        }

        for (Integer dataId : dataWithoutStats) {
            asyncUpdateDataStatistics(dataId);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Async
    public void asyncUpdateDataStatistics(final int dataId) {
        updateDataStatistics(dataId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncUpdateDataStatistics(final int dataId) {
        updateDataStatistics(dataId);
    }

    private void updateDataStatistics(final int dataId) {
        Data data = dataRepository.findById(dataId);
        if (data == null) {
            LOGGER.log(Level.WARNING, "Can't compute coverage statistics on data id " + dataId +
                    " because data is not found in database.");
            return;
        }
        try {
            if (DataType.COVERAGE.name().equals(data.getType())    &&
               (data.getRendered() == null || !data.getRendered()) &&
               (!"pyramid".equalsIgnoreCase(data.getSubtype()))    &&
               (data.getStatsState() == null)) {

                LOGGER.log(Level.INFO, "Start computing data " + dataId + " "+data.getName()+" coverage statistics.");

                data.setStatsState(STATE_PENDING);
                updateData(data);

                if (providerRepository.existsById(data.getProviderId())) {
                    final org.constellation.provider.Data dataP  = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());

                    if (dataP instanceof PyramidData) {
                        //pyramid, too large to compute statistics
                        data.setStatsState(STATE_PARTIAL);
                        updateData(data);
                        return;
                    }

                    if (dataP instanceof CoverageData) {
                        final Object result = dataP.computeStatistic(dataId, dataRepository);
                        if (result instanceof ImageStatistics) {
                            updateMetadata((ImageStatistics) result, data);
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

    private void updateMetadata(final ImageStatistics stats, final Data target) {
        SpringHelper.executeInTransaction(status -> {
            final Object md;
            final Integer dataId = target.getId();
            try {
                md = metadataService.getIsoMetadataForData(dataId);
                if (md == null) {
                    LOGGER.log(Level.WARNING, "Data {0}: cannot update metadata with statistics : metadata not found.", dataId);
                    return null;
                }
                if (!(md instanceof Metadata))
                    throw new RuntimeException("Only GeoAPI metadata accepted for statistics update");
                final DefaultMetadata updatedMd = new DefaultMetadata((Metadata) md);
                final MetadataFeeder feeder = new MetadataFeeder(updatedMd);
                final CoverageDescription adapter = new CoverageDescriptionAdapter(stats);
                feeder.setCoverageDescription(adapter, MetadataFeeder.WriteOption.REPLACE_EXISTING);
                metadataService.updateMetadata(((Metadata) md).getFileIdentifier(), updatedMd, dataId, null, null, null, null, null);
                return null;
            } catch (ConstellationException e) {
                throw new BackingStoreException(String.format("Cannot update metadata for data %d (%s)", dataId, target.getName()), e);
            }
        });
    }
}
