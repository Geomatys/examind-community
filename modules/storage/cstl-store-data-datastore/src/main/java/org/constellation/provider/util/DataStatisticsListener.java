/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2019, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.provider.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.parameter.Parameters;
import org.constellation.admin.SpringHelper;
import org.constellation.dto.Data;
import org.constellation.repository.DataRepository;
import org.geotoolkit.storage.coverage.ImageStatistics;
import org.geotoolkit.process.ProcessEvent;
import org.geotoolkit.processing.ProcessListenerAdapter;
import static org.geotoolkit.processing.coverage.statistics.StatisticsDescriptor.OUTCOVERAGE;
import org.opengis.parameter.ParameterValueGroup;
import static org.constellation.api.StatisticState.*;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

/**
 * ProcessListener that will update data record in database.
 *
 * @author guilhem
 */
public class DataStatisticsListener extends ProcessListenerAdapter {

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    private final int dataId;

    private final DataRepository dataRepository;

    public DataStatisticsListener(int dataId, DataRepository dataRepository) {
        this.dataId = dataId;
        this.dataRepository = dataRepository;
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
            LOGGER.log(Level.WARNING, "Error during coverage statistic update for data " + dataId
                    + " " + data.getName() + " : " + exception.getMessage(), exception);
        }

    }

    private Data getData() {
        return dataRepository.findById(dataId);
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
     * Serialize Statistic in JSON
     *
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
