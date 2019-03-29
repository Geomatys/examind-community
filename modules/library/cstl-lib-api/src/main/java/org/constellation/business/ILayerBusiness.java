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
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.DataBrief;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.dto.service.config.wxs.AddLayer;
import org.constellation.dto.service.config.wxs.FilterAndDimension;
import org.constellation.dto.NameInProvider;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface ILayerBusiness {
    void removeAll() throws ConfigurationException;

    void add(AddLayer layer) throws ConfigurationException;

    void add(String name, String namespace, String providerId, String alias,
             String serviceId, String serviceType, org.constellation.dto.service.config.wxs.Layer config) throws ConfigurationException;

    void add(int dataId, String alias,
             int serviceId, org.constellation.dto.service.config.wxs.Layer config) throws ConfigurationException;

    /**
     * Update the title of a lyer
     *
     * @param layerID Identifier of the layer.
     * @param newTitle The new title to set.
     *
     * @throws ConfigurationException
     */
    void updateLayerTitle(int layerID, String newTitle) throws ConfigurationException;

    void removeForService(final String serviceType, final String serviceId) throws ConfigurationException;

    List<Layer> getLayers(Integer serviceId, String userLogin) throws ConfigurationException;

    List<NameInProvider> getLayerNames(String serviceType, String serviceName, String userLogin) throws ConfigurationException;

    List<Integer> getLayerIds(Integer layerId, String userLogin) throws ConfigurationException;

    Layer getLayer(String spec, String identifier, String name, String namespace, String login) throws ConfigurationException;

    FilterAndDimension getLayerFilterDimension(String spec, String identifier, String name, String namespace, String login) throws ConfigurationException;

    NameInProvider getFullLayerName(Integer serviceId, String nameOrAlias, String namespace, String login) throws ConfigurationException;

    NameInProvider getFullLayerName(Integer serviceId, Integer layerId, String login) throws ConfigurationException;

    void remove(String spec, String serviceId, String layerId, String namespace) throws ConfigurationException;

    /**
     * Return all layer mapped in {@link LayerSummary} using given style.
     * Returned {@link LayerSummary} will not have {@code targetStyle} field filled.
     *
     * @param styleId
     * @return list of {@link LayerSummary} without {@code targetStyle} field
     */
    List<LayerSummary> getLayerSummaryFromStyleId(final Integer styleId) throws ConstellationException;

    /**
     * Returns a list of light {@link LayerSummary} for given style id.
     * Output LayerSummary contain only :
     * <ul>
     *     <li>id</li>
     *     <li>name</li>
     *     <li>namespace</li>
     *     <li>provider</li>
     *     <li>type</li>
     *     <li>subtype</li>
     * </ul>
     *
     * @param styleId the given style id.
     * @return the list of light {@link DataBrief}.
     */
    List<LayerSummary> getLayerRefFromStyleId(final Integer styleId);

}
