/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.provider.computed;

import com.examind.image.heatmap.FeatureSetAsPointsCloud;
import com.examind.image.heatmap.HeatMapImage;
import com.examind.image.heatmap.HeatMapResource;
import com.examind.image.heatmap.PointCloudResource;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.util.iso.Names;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DefaultCoverageData;
import org.constellation.provider.FeatureData;
import org.constellation.repository.DataRepository;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;

import java.awt.*;
import java.util.logging.Level;

import static org.constellation.provider.computed.HeatMapCoverageProviderDescriptor.*;


/**
 * Based on {@link AggregatedCoverageProvider}
 */
public class HeatMapCoverageProvider extends ComputedResourceProvider {

    private final int[] dataIds;
    private final Dimension tilingDimension;
    private final float distanceX, distanceY;
    private final HeatMapImage.Algorithm algorithm;

    public HeatMapCoverageProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws FactoryException {
        super(providerId, service, param);
        final Parameters input = Parameters.castOrWrap(param);
        dataIds = input.values().stream()
                .filter(it -> DATA_IDS.equals(it.getDescriptor()))
                .map(it -> (ParameterValue<Integer>) it)
                .filter(it -> it.getValue() != null)
                .mapToInt(ParameterValue::getValue)
                .toArray();

        final Integer tilingDimX = input.getValue(TILING_DIMENSION_X);
        final Integer tilingDimY = input.getValue(TILING_DIMENSION_Y);
        if (tilingDimX == null || tilingDimY == null) {
            tilingDimension = null;
        } else {
            tilingDimension = new Dimension(tilingDimX, tilingDimY);
        }

        distanceX = input.getValue(DISTANCE_X);
        distanceY = input.getValue(DISTANCE_Y);

        algorithm = HeatMapImage.Algorithm.valueOf(input.getValue(ALGORITHM));


    }

    @Override
    protected synchronized Data getComputedData() {
        if (cachedData == null) {
            try {
                // TODO: add algorithm as provider parameter
                final HeatMapResource res = new HeatMapResource(dataToPointCloud(), tilingDimension, distanceX, distanceY, algorithm);
                final String resultDataName = getDataName().orElse("HeatMap");
                cachedData = new DefaultCoverageData(Names.createLocalName(null, ":", resultDataName), res, null);
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, id, ex);
            }
        }
        return cachedData;
    }

    private PointCloudResource dataToPointCloud() throws ConstellationException, DataStoreException {

        if (dataIds.length == 0) {
            throw new IllegalArgumentException("No data given for HeatMapComputation. At least 1 resource required.");
        } else if (dataIds.length > 1) {
            throw new UnsupportedOperationException("Not supported yet. Currently only features from a single FeatureSet can be used to compute an HeatMap.");
        } else {
            final DataRepository repo = SpringHelper.getBean(DataRepository.class)
                    .orElseThrow(() -> new ConstellationException("No spring context available"));
            final Data<?> data = getData(repo, dataIds[0]);
            if (data instanceof FeatureData featureData) {
                return new FeatureSetAsPointsCloud(featureData.getOrigin());
            } else {
                throw new ConfigurationException("A FeatureSet data was expected for HeatMap computation.");
            }
        }
    }
}
