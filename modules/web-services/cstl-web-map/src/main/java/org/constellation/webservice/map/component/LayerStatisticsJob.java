package org.constellation.webservice.map.component;

import org.apache.sis.storage.FeatureSet;
import org.constellation.admin.SpringHelper;
import org.constellation.api.DataType;
import org.constellation.business.ILayerStatisticsJob;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.*;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.map.layerstats.LayerStatisticsUtils;
import org.constellation.provider.DataProviders;
import org.constellation.provider.FeatureData;
import org.constellation.repository.*;
import org.opengis.style.Style;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.constellation.api.StatisticState.*;
import static org.constellation.api.StatisticState.STATE_ERROR;

/**
 *
 * @author Estelle Idée (Geomatys)
 */
@Component
@Primary
public class LayerStatisticsJob implements ILayerStatisticsJob {

    /**
     * Used for debugging purposes.
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.webservice.map.component");

    @Autowired
    private StyledLayerRepository styledLayerRepository;
    @Autowired
    private LayerRepository layerRepository;
    @Autowired
    private IServiceBusiness serviceBusiness;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private ProviderRepository providerRepository;
    @Autowired
    private IStyleBusiness styleBusiness;

    /**
     * {@inheritDoc}
     */
    @Scheduled(cron = "1 * * * * *")
    @Override
    public void updateStyledLayerStatistics() {
        computeEmptyStyledLayerStatistics(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void computeEmptyStyledLayerStatistics(boolean isInit) {
        final List<StyledLayer> layersList = styledLayerRepository.findStatisticLess();

        Map<Integer, Integer> styledLayerWithoutStats = new HashMap<>();
        for (final StyledLayer styledLayer : layersList) {
            String state = styledLayer.getStatsState();
            boolean activateStats = styledLayer.getActivateStats();
            final Integer styleId = styledLayer.getStyle();
            final Integer layerId = styledLayer.getLayer();
            if (isInit) {
                //rerun statistic for error and pending states
                if (STATE_PENDING.equalsIgnoreCase(state) || STATE_ERROR.equalsIgnoreCase(state)) {
                    SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                            styledLayerRepository.updateStatistics(styleId, layerId, null, null);
                        }
                    });
                }
            }

            if (activateStats && (state == null || state.isEmpty())) {
                styledLayerWithoutStats.put(styleId, layerId);
            }
        }

        for (Map.Entry<Integer, Integer> styleLayerIds : styledLayerWithoutStats.entrySet()) {
            final Integer styleId = styleLayerIds.getKey();
            final Integer layerId = styleLayerIds.getValue();
            asyncUpdateStyledLayerStatistics(styleId, layerId);
        }
    }

    @Override
    @Async
    public void asyncUpdateStyledLayerStatistics(int styleId, int layerId) {
        updateStyledLayerStatistics(styleId, layerId);
    }
    @Override
    public void syncUpdateStyledLayerStatistics(int styleId, int layerId) { updateStyledLayerStatistics(styleId, layerId); }

    private void updateStyledLayerStatistics(final int styleId, final int layerId) {
        StyledLayer styledLayer = styledLayerRepository.findByStyleAndLayer(styleId, layerId);
        if (styledLayer == null) {
            LOGGER.log(Level.WARNING, "Can't compute statistics for style id " + styleId +
                    " and layer id " + layerId + " because a corresponding StyledLayer is not found in database.");
            return;
        }

        try {
            final Layer layer = layerRepository.findById(layerId);
            // Check service type : compute statistics only for WMS and WMTS layers.
            final Integer serviceId = layer.getService();
            final ServiceComplete service = serviceBusiness.getServiceById(serviceId, null);
            if (service == null) {
                throw new TargetNotFoundException("Service not found by id : " + serviceId);
            }
            final String type = service.getType();
            if ("wms".equalsIgnoreCase(type) &&
                    styledLayer.getActivateStats() &&
                    styledLayer.getExtraInfo() == null &&
                    styledLayer.getStatsState() == null) {

                LOGGER.log(Level.INFO, "Start computing statistics for styleId " + styleId + " and layerId " + layerId + ".");
                styledLayer.setStatsState(STATE_PENDING);

                final Integer dataId = layer.getDataId();
                final Data data = dataRepository.findById(dataId);

                // TODO add COVERAGE once added to LayerStatisticsUtils.
                if (DataType.VECTOR.name().equals(data.getType())) {
                    final org.constellation.provider.Data dataP = DataProviders.getProviderData(dataId);

                    if (dataP instanceof FeatureData fd) {
                        final FeatureSet featureSet = fd.getOrigin();
                        final Style style = styleBusiness.getStyle(styleId);
                        final String stats = LayerStatisticsUtils.computeStatisticsForLayerWithStyle(featureSet, style);
                        styledLayer.setStatsState(STATE_COMPLETED);
                        styledLayer.setExtraInfo(stats);
                        updateLayer(styledLayer);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during statistic update for style id " + styleId + " and layer id " + layerId + " " + " : " + e.getMessage(), e);

            //update styledLayer
            StyledLayer lastStyledLayer = styledLayerRepository.findByStyleAndLayer(styleId, layerId);
            if (lastStyledLayer != null && !lastStyledLayer.getStatsState().equals(STATE_ERROR)) {
                styledLayer.setStatsState(STATE_ERROR);
                updateLayer(styledLayer);
            }
        }
    }

    private void updateLayer(final StyledLayer styledLayer) {
        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                styledLayerRepository.updateStatistics(styledLayer.getStyle(), styledLayer.getLayer(), styledLayer.getExtraInfo(), styledLayer.getStatsState());
            }
        });
    }


}
