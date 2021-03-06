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

import org.constellation.dto.MapContextDTO;
import org.constellation.dto.MapContextStyledLayerDTO;

public interface MapContextRepository extends AbstractRepository {

    MapContextDTO findById(int id);

    List<MapContextDTO> findAll();

    List<Integer> findAllId();

    List<MapContextStyledLayerDTO> getLinkedLayers(int mapContextId);

    void setLinkedLayers(int mapContextId, List<MapContextStyledLayerDTO> layers);

    Integer create(MapContextDTO mapContext);

    int update(MapContextDTO mapContext);

    Map.Entry<Integer, List<MapContextDTO>> filterAndGet(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage);
}
