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
import org.constellation.dto.AbstractMCLayerDTO;
import org.constellation.dto.MapContextDTO;

public interface MapContextRepository extends AbstractRepository {

    /**
     * Test if the mapcontext name exist.
     *
     * @param name mapcontext candidate name.
     * @return {@code true} if the mapcontext name is already used.
     */
    boolean existsByName(String name);

    MapContextDTO findById(int id);

    MapContextDTO findByName(String name);

    List<MapContextDTO> findAll();

    List<Integer> findAllId();

    List<AbstractMCLayerDTO> getLinkedLayers(int mapContextId);

    void setLinkedLayers(int mapContextId, List<AbstractMCLayerDTO> layers);

    Integer create(MapContextDTO mapContext);

    int update(MapContextDTO mapContext);

    Map.Entry<Integer, List<MapContextDTO>> filterAndGet(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage);
}