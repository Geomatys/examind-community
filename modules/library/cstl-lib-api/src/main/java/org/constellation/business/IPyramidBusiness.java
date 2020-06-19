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
import org.constellation.dto.DataBrief;
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

    TilingResult pyramidMapContextRendered(Integer userId, String pyramidDataName, final String crs, final MapContextLayersDTO mc) throws ConstellationException;

    TilingResult pyramidDatasRendered(Integer userId, String pyramidDataName, List<Integer> dataIds, final String crs) throws ConstellationException;

    /**
     * Generates a pyramid conform for data.
     * N.B : Generated pyramid contains coverage real values, it's not styled for rendering.
     *
     * @param dataId The given data identifier.
     * @param userId The pyramids owner.
     * @return {@link DataBrief}
     */
    TilingResult pyramidDataConform(final int dataId, final int userId) throws ConstellationException;

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
