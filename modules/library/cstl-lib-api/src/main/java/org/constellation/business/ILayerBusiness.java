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

import java.util.List;
import java.util.Map;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.dto.service.config.wxs.FilterAndDimension;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.StyleReference;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface ILayerBusiness {
    void removeAll() throws ConstellationException;

    Integer add(int dataId, String alias, String namespace, String name, int serviceId, LayerConfig config) throws ConfigurationException;

    /**
     * Update a layer.
     *
     * for now it only update the title and alias
     *
     * @param layerID Identifier of the layer.
     * @param summary AN object containing all the field to update
     *
     * @throws ConfigurationException
     */
    void update(int layerID, LayerSummary summary) throws ConfigurationException;

    void removeForService(Integer serviceId) throws ConstellationException;

    List<LayerConfig> getLayers(Integer serviceId, String userLogin) throws ConfigurationException;

    List<NameInProvider> getLayerNames(Integer ServiceId, String userLogin) throws ConfigurationException;

    List<Integer> getLayerIds(Integer layerId, String userLogin) throws ConfigurationException;

    /**
     * Get a single layer from service spec and identifier and layer name and namespace.
     *
     * @param layerId layer identifier
     * @param login login for security check
     *
     * @return Layer
     * @throws ConfigurationException
     */
    LayerConfig getLayer(Integer layerId, String login) throws ConfigurationException;

    FilterAndDimension getLayerFilterDimension(Integer layerId) throws ConfigurationException;

    NameInProvider getFullLayerName(Integer serviceId, String nameOrAlias, String namespace, String login) throws ConfigurationException;

    NameInProvider getFullLayerName(Integer serviceId, Integer layerId, String login) throws ConfigurationException;

    void remove(Integer layerId) throws ConstellationException;

    /**
     * Return all layer mapped in {@link LayerSummary} using given style.
     * Returned {@link LayerSummary} will not have {@code targetStyle} field filled.
     *
     * @param styleId
     * @return list of {@link LayerSummary} without {@code targetStyle} field
     */
    List<LayerSummary> getLayerSummaryFromStyleId(final Integer styleId) throws ConstellationException;

    List<StyleReference> getLayerStyles(Integer layerId) throws ConstellationException;

    Map.Entry<Integer, List<LayerConfig>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) throws ConstellationException;

}
