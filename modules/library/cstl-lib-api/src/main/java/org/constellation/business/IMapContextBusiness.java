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
import org.constellation.dto.DataBrief;

import org.constellation.dto.MapContextLayersDTO;
import org.constellation.exception.ConstellationException;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextStyledLayerDTO;
import org.opengis.geometry.Envelope;
import org.opengis.util.FactoryException;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IMapContextBusiness {

    List<MapContextLayersDTO> findAllMapContextLayers() throws ConstellationException;

    Integer createFromData(Integer userId, String contextName, String crs, Envelope env, List<DataBrief> briefs) throws ConstellationException;

    Integer create(final MapContextLayersDTO mapContext) throws ConstellationException;

    void setMapItems(final int contextId, final List<MapContextStyledLayerDTO> layers);

    MapContextLayersDTO findMapContextLayers(int contextId) throws ConstellationException;

    ParameterValues getExtent(int contextId) throws FactoryException,ConstellationException;

    ParameterValues getExtentForLayers(final List<MapContextStyledLayerDTO> styledLayers) throws FactoryException,ConstellationException;

    List<MapContextDTO> getAllContexts();

    void updateContext(MapContextLayersDTO mapContext);

    void delete(int contextId) throws ConstellationException;

    void deleteAll()throws ConstellationException;

    MapContextDTO getContextById(int id);

    Map.Entry<Integer, List<MapContextDTO>> filterAndGetBrief(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage);

    Map.Entry<Integer, List<MapContextLayersDTO>> filterAndGetMapContextLayers(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage)throws ConstellationException;

}
