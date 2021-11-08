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
package org.constellation.repository;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.constellation.dto.Layer;

public interface LayerRepository extends AbstractRepository {

    /**
     * Return all the layers.
     * 
     * @return Complete list of layers.
     */
    List<Layer> findAll();

    /**
     * Return the layer with the specified id.
     * 
     * @param id layer identifier.
     * @return A Layer or {@code null}.
     */
    Layer findById(Integer id);

    /**
     * Return all the layers for the specified service.
     *
     * @param serviceId The service identifier.
     * @return The service layers.
     */
    List<Layer> findByServiceId(int serviceId);

    /**
     * Return all the layers identifiers for the specified service.
     *
     * @param serviceId The service identifier.
     * @return The service layers identifiers.
     */
    List<Integer> findIdByServiceId(int serviceId);

    /**
     * Return all the service layer names.
     * 
     * @param serviceId The service identifier.
     * @return A list of layer QName
     */
    List<QName> findNameByServiceId(int serviceId);

    /**
     * Return all the layer identifier for the specified data.
     * 
     * @param dataId the data identifier.
     * @return A list of layer Identifiers.
     */
    List<Integer> findByDataId(int dataId);

    /**
     * Delete all layer associated to a service.
     * 
     * @param service service identifier of which layers are removed.
     * @return number of layer removed.
     */
    int deleteServiceLayer(Integer service);

    /**
     * Store a new layer.
     *
     * @param storeLayer Layer to store.
     * @return The assigned identifier.
     */
    Integer create(Layer storeLayer);

    /**
     * Update a layer entry.
     * @param storeLayer layer to update
     */
    void update(Layer storeLayer);

    /**
     * Search for a Layer identifier with the specified name in the specified service.
     * The layer must have no alias.
     *
     * @param serviceId The service identifier.
     * @param layerName The searched layer name.
     * @param noNamespace if set to {@code true} the layer namespace must be {@code null} to be found.
     *
     * @return A layer Identifier if found or {@code null}.
     */
    Integer findIdByServiceIdAndLayerName(int serviceId, String layerName, boolean noNamespace);

    /**
     * Search for a Layer with the specified name in the specified service.
     * The layer must have no alias.
     *
     * @param serviceId The service identifier.
     * @param layerName The searched layer name.
     * @param noNamespace if set to {@code true} the layer namespace must be {@code null} to be found.
     *
     * @return A layer if found or {@code null}.
     */
    Layer findByServiceIdAndLayerName(int serviceId, String layerName, boolean noNamespace);

    /**
     * Search for a Layer identifier with the specified name and namespace in the specified service.
     *
     * @param serviceId The service identifier.
     * @param layerName The searched layer name.
     * @param namespace The searched layer namespace.
     *
     * @return A layer identifier if found or {@code null}.
     */
    Integer findIdByServiceIdAndLayerName(int serviceId, String layerName, String namespace);

    /**
     * Search for a Layer with the specified name and namespace in the specified service.
     *
     * @param serviceId The service identifier.
     * @param layerName The searched layer name.
     * @param namespace The searched layer namespace.
     *
     * @return A layer if found or {@code null}.
     */
    Layer findByServiceIdAndLayerName(int serviceId, String layerName, String namespace);

    /**
     * Search for a Layer identifier with the specified alias in the specified service.
     *
     * @param serviceId The service identifier.
     * @param alias The searched layer alias.
     *
     * @return A layer identifier if found or {@code null}.
     */
    Integer findIdByServiceIdAndAlias(int serviceId, String alias);

    /**
     * Search for a Layer with the specified alias in the specified service.
     *
     * @param serviceId The service identifier.
     * @param alias The searched layer alias.
     *
     * @return A layer if found or {@code null}.
     */
    Layer findByServiceIdAndAlias(int serviceId, String alias);

     /**
     * Search for layers with the specified data in the specified service.
     *
     * @param serviceId The service identifier.
     * @param dataId The data identifiere.
     *
     * @return A layer if found or {@code null}.
     */
    List<Layer> findByServiceIdAndDataId(int serviceId, int dataId);
    
    /**
     * Retrieve all layers linked to a given style id.
     *
     * @param styleId style id candidate
     * @return list of {@link Layer}
     */
    List<Layer> getLayersByLinkedStyle(final int styleId);

    /**
     * Search layers.
     *
     * @param filterMap filters to apply to the search.
     * @param sortEntry Sort to apply to the search.
     * @param pageNumber page number to retrieve.
     * @param rowsPerPage Number of row per page.
     *
     * @return The total count, and the layer records for the specified page.
     */
    Map.Entry<Integer, List<Layer>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage);

}
