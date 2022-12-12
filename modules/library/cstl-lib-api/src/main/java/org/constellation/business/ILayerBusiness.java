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
import org.constellation.dto.NameInProvider;
import org.constellation.dto.StyleReference;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface ILayerBusiness {

    /**
     * Remove All the layers in the system.
     * 
     * @throws ConstellationException If a problem occurs during a layer removal.
     */
    void removeAll() throws ConstellationException;


    /**
     * Add a new Layer to the specified service.
     *
     * @param dataId Referenced data id.
     * @param alias Layer alias.
     * @param namespace Layer namespace.
     * @param name Layer name.
     * @param serviceId Service identifier.
     * @param config Layer configuration, can be {@code null}
     * 
     * @return The assigned layer identifier.
     * @throws ConfigurationException If the service does not exist.
     */
    Integer add(int dataId, String alias, String namespace, String name, String title, int serviceId, LayerConfig config) throws ConfigurationException;

    /**
     * Update a layer.
     *
     * for now it only update the title and alias
     *
     * @param layerID Identifier of the layer.
     * @param summary An object containing all the field to update
     *
     * @throws ConfigurationException If the layer does not exist.
     */
    void update(int layerID, LayerSummary summary) throws ConfigurationException;

    /**
     * Remove all the layer of the specified service.
     * 
     * @param serviceId Service identifier.
     *
     * @throws ConstellationException If the service does not exist.
     */
    void removeForService(Integer serviceId) throws ConstellationException;

    /**
     * Return all the layers for the specifed service.
     * This list is filtered on the user security rights.
     *
     * @param serviceId Service identifier.
     * @param userLogin login of the user asking for layers.
     *
     */
    List<LayerConfig> getLayers(Integer serviceId, String userLogin) throws ConfigurationException;

    /**
     * Return the layer number for the specifed service.
     * This list is not filtered on the user security rights.
     *
     * @param serviceId Service identifier.
     *
     */
    int getLayerCount(Integer serviceId);

    /**
     * Return all the layer's names for the specifed service.
     * This list is filtered on the user security rights.
     *
     * @param serviceId Service identifier.
     * @param userLogin login of the user asking for layers.
     *
     */
    List<NameInProvider> getLayerNames(Integer serviceId, String userLogin) throws ConfigurationException;

    /**
     * Return all the layer's ids for the specifed service.
     * This list is filtered on the user security rights.
     *
     * @param serviceId Service identifier.
     * @param userLogin login of the user asking for layers.
     *
     */
    List<Integer> getLayerIds(Integer serviceId, String userLogin) throws ConfigurationException;

    /**
     * Get a single layer from its id.
     *
     * @param layerId layer identifier
     * @param login login for security check
     *
     */
    /**
     * Get a single layer from its identifier.
     *
     * @param layerId  layer identifier.
     * @param login User login for security check.
     *
     * @return The layer description.
     *
     * @throws ConfigurationException If the layer does not exist, or if its not allowed for the user to access it.
     */
    LayerConfig getLayer(Integer layerId, String login) throws ConfigurationException;

    /**
     * Return the full layer name, if a layer is matching the parameters.
     * it search for a layer in this order
     *  - 1. search by name and namespace
     *  - 2. search by alias
     *  - 3. search by single name with no namespace
     *  - 4. search by single name (with ommited namespace)
     *
     * @param serviceId The service identifier.
     * @param nameOrAlias The name or alias of the searched layer.
     * @param namespace  The namespace of the searched layer.
     * @param login User login for security check.
     * 
     * @throws ConfigurationException If the layer can not be found.
     *                                If the service does not exist.
     *                                If the user is not allowed to see the layer.
     */
    NameInProvider getFullLayerName(Integer serviceId, String nameOrAlias, String namespace, String login) throws ConfigurationException;

    /**
     * Return the full name of a layer.
     *
     * @param serviceId The service identifier.
     * @param layerId The layer identifier.
     * @param login User login for security check.
     * 
     * @throws ConfigurationException If the layer can not be found.
     *                                If the user is not allowed to see the layer.
     */
    NameInProvider getFullLayerName(Integer serviceId, Integer layerId, String login) throws ConfigurationException;

    /**
     * Remove the specified layer.
     *
     * @param layerId The layer identifier.
     *
     * @throws ConstellationException If the layer does not exist, or if a problem occurs during removal.
     */
    void remove(Integer layerId) throws ConstellationException;

    /**
     * Return all layer mapped in {@link LayerSummary} using given style.
     * Returned {@link LayerSummary} will not have {@code targetStyle} field filled.
     *
     * @param styleId The style identifier.
     * @return list of {@link LayerSummary} without {@code targetStyle} field
     */
    List<LayerSummary> getLayerSummaryFromStyleId(final Integer styleId) throws ConstellationException;

    /**
     * Return all the styles linked to the specified layer.
     *
     * @param layerId The layer identifier.
     * @return A list of {@link StyleReference}
     */
    List<StyleReference> getLayerStyles(Integer layerId) throws ConstellationException;

    Map.Entry<Integer, List<LayerConfig>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) throws ConstellationException;

    /**
     * Return {code true} if the specified alias is available in the service.
     * 
     * @param serviceId The service identifier.
     * @param alias The candidate alias.
     * 
     */
    boolean isAvailableAlias(Integer serviceId, String alias);

}
