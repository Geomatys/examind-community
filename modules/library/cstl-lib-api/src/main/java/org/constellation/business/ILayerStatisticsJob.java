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
package org.constellation.business;


/**
 * Gather all asynchronous method on StyledLayers.
 *
 * @author Estelle Idée (Geomatys)
 */
public interface ILayerStatisticsJob {

    /*
     * Run {@link org.constellation.business.ILayerStatisticsJob#asyncUpdateDataStatistics(int)}
     * on each StyledLayer without computed statistics.
     * @param isInit flag that define if it's a startup call.
     *               If true, statistic of all StyledLayer in ERROR and PENDING will be also computed
     */
    void computeEmptyStyledLayerStatistics(boolean isInit);

    /**
     * Asynchronous method that compute statistics for a given couple style/layer and store result into database.
     * This method doesn't affect non feature data.
     * @param styleId The style identifier.
     * @param layerId The layer identifier.
     */
    void asyncUpdateStyledLayerStatistics(final int styleId, final int layerId);

    /**
     * Synchronous method that compute statistics for a given couple style/layer and store result into database.
     * This method doesn't affect non feature data.
     * @param styleId The style identifier.
     * @param layerId The layer identifier.
     */
    void syncUpdateStyledLayerStatistics(final int styleId, final int layerId);

    /**
     * Search for StyledLayers without statistics and launch analysis on them.
     */
    void updateStyledLayerStatistics();
}
