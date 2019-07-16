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

import org.constellation.dto.Style;
import org.constellation.dto.StyleReference;

public interface StyleRepository {

    int create(Style style);

    List<Style> findAll();

    List<Style> findByType(final String type);

    List<Style> findByTypeAndProvider(final int providerId, final String type);

    List<Style> findByProvider(final int providerId);

    Style findByNameAndProvider(final int providerId, String name);

    Style findById(int id);

    List<Style> findByName(final String name);

    List<Style> findByData(Integer dataId);

    List<Style> findByLayer(Integer layerId);

    void linkStyleToData(int styleId, int dataid);

    void unlinkStyleToData(int styleId, int dataid);

    void unlinkAllStylesFromData(int dataId);

    void linkStyleToLayer(int styleId, int layerid);

    void unlinkStyleToLayer(int styleId, int layerId);

    List<Integer> getStyleIdsForData(int id);

    void delete(int id);

    @Deprecated
    void delete(int providerId, String name);

    void update(Style s);

    public boolean existsById(int styleId);

    public List<StyleReference> fetchByDataId(int dataId);

    public List<StyleReference> fetchByLayerId(int layerId);

    void changeSharedProperty(final int id, final boolean shared);

    Map.Entry<Integer, List<Style>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage);


}
