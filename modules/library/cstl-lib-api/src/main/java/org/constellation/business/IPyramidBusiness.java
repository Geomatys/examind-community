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
package org.constellation.business;

import java.util.List;
import org.constellation.api.TilingMode;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.TilingResult;
import org.constellation.dto.ProviderPyramidChoiceList;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IPyramidBusiness {

    /**
     * Generates a pyramid for a map context. if mode = RENDERED generated pyramid will be styled for rendering.
     * if mode == CONFORM generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param pyramidDataName The given pyramid name.
     * @param mc The input map context.
     * @param crs The given pyramid coordinate reference system.
     * @param userId The pyramids owner.
     * @param mode The tiling mode, RENDERED or CONFORM.
     * 
     * @return {@link TilingResult}
     * @throws org.constellation.exception.ConstellationException
     */
    TilingResult pyramidMapContext(Integer userId, String pyramidDataName, final String crs, final MapContextLayersDTO mc, final TilingMode mode) throws ConstellationException;

    /**
     * Generates a pyramid for a map context. if mode = RENDERED generated pyramid will be styled for rendering.
     * if mode == CONFORM generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param pyramidDataName The given pyramid name.
     * @param dataIds The input map data identifiers.
     * @param crs The given pyramid coordinate reference system.
     * @param userId The pyramids owner.
     * @param mode The tiling mode, RENDERED or CONFORM.
     * 
     * @return {@link TilingResult}
     * @throws org.constellation.exception.ConstellationException
     */
    TilingResult pyramidDatas(Integer userId, String pyramidDataName, List<Integer> dataIds, final String crs, final TilingMode mode) throws ConstellationException;

    /**
     * Generates a pyramid conform for each data of the provider.
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param providerId Provider identifier of the data to tile.
     *
     */
    void createAllPyramidConformForProvider(final int providerId) throws ConstellationException;

    ProviderPyramidChoiceList listPyramids(final String id, final String dataName) throws ConfigurationException;
}
