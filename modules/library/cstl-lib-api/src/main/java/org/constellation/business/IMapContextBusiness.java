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
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.exception.ConstellationException;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.MapContextDTO;
import org.constellation.exception.TargetNotFoundException;
import org.opengis.geometry.Envelope;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IMapContextBusiness {

    List<MapContextLayersDTO> findAllMapContextLayers(boolean full) throws ConstellationException;

    Integer createFromData(Integer userId, String contextName, String crs, Envelope env, List<DataBrief> briefs) throws ConstellationException;

    Integer create(final MapContextLayersDTO mapContext) throws ConstellationException;

    void setMapItems(final int contextId, final List<AbstractMCLayerDTO> layers);

    /**
     * Return a mapcontext with its layers included for the specified id.
     * If not found a {@link TargetNotFoundException} will be throw.
     *
     * @param id The searched mapcontext identifier.
     * @param full if set to false, a lighter version of the mapcontext will be returned.
     * @return
     *
     * @throws ConstellationException If the the context can't be found.
     */
    MapContextLayersDTO findMapContextLayers(int id, boolean full) throws ConstellationException;

    /**
     * Return a mapcontext with its layers included for the specified name.
     * If not found a {@link TargetNotFoundException} will be throw.
     *
     * @param name The searched mapcontext name.
     * @param full if set to false, a lighter version of the mapcontext will be returned.
     * @return
     *
     * @throws ConstellationException If the the context can't be found.
     */
    MapContextLayersDTO findByName(String name, boolean full) throws ConstellationException;

    /**
     * Get the extent of all included layers in this map context.
     *
     * @param id Context identifier
     * @return
     * @throws ConstellationException
     */
    ParameterValues getExtent(int id) throws ConstellationException;

    /**
     * Get the extent for the given layers.
     *
     * @param layers Layers to consider.
     * @return
     * @throws ConstellationException
     */
    ParameterValues getExtentForLayers(final List<AbstractMCLayerDTO> layers) throws ConstellationException;

    List<MapContextDTO> getAllContexts(boolean full);

    void updateContext(MapContextLayersDTO mapContext) throws ConstellationException;

    /**
     * Remove the specified mapcontext and its metadata if there is one.
     *
     * @param id mapcontext identifier.
     *
     * @return 1 if the context has been removed, 0 else.
     * @throws ConstellationException If an erros occurs during the metadata/mapcontext removal.
     */
    int delete(int id) throws ConstellationException;

    void deleteAll()throws ConstellationException;

    /**
     * Return a mapcontext for the specified id.
     * If not found a {@link TargetNotFoundException} will be throw.
     *
     * @param id The searched mapcontext identifier.
     * @param full if set to false, a lighter version of the mapcontext will be returned.
     * @return
     *
     * @throws ConstellationException If the the context can't be found.
     */
    MapContextDTO getContextById(int id, boolean full) throws ConstellationException;

    Data getMapContextData(int id) throws ConstellationException;

    Map.Entry<Integer, List<MapContextDTO>> filterAndGetBrief(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage);

    Map.Entry<Integer, List<MapContextLayersDTO>> filterAndGetMapContextLayers(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage)throws ConstellationException;

    void initializeDefaultMapContextData() throws ConstellationException;

    /**
     * Return {@code true} if the specified mapcontext exist.
     * 
     * @param id The searched mapcontext identifier.
     * @return  {@code true} if the specified mapcontext exist.
     */
    boolean existById(int id);

}