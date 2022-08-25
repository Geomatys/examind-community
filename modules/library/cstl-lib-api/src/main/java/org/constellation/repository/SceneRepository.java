/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2022 Geomatys.
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
import org.constellation.dto.Scene;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface SceneRepository {

    /**
     * List all 3D scenes.
     *
     * @return never null, can be empty.
     */
    List<Scene> findAll();

    /**
     * Get scene for given identifier.
     *
     * @param id scene identifier, not null.
     * @return can be null if not found.
     */
    Scene findById(Integer id);

    /**
     * Get scene for given name.
     *
     * @param name scene name, not null.
     * @return can be null if not found.
     */
    Scene findByName(String name);

    /**
     * Delete scene for given identifier.
     *
     * @param id scene identifier, not null.
     * @return number of deleted scenes.
     */
    int delete(Integer id);

    /**
     * Search if given scene name is already used.
     *
     * @param name searched name
     * @return true if name is used
     */
    boolean isUsedName(String name);

    /**
     * Store given scene.
     *
     * @param scene not null
     */
    void create(Scene scene);

    /**
     * Update scene status.
     *
     * @param id scene identifier, not null.
     * @param status scene status, not null
     */
    void updateStatus(int id, String status);

    /**
     * Update scene bounding box, in CRS:84 .
     *
     * @param id scene identifier, not null.
     * @param minx minimum longitude
     * @param maxx maximum longitude
     * @param miny minimum latitude
     * @param maxy maximum latitude
     */
    void updateBBox(int id, Double minx, Double maxx, Double miny, Double maxy);

    /**
     * Update scene parameters.
     *
     * @param scene not null
     */
    void update(Scene scene);

    /**
     * Search all scenes in given theater.
     *
     * @param theaterId theater identifier, not null
     * @return never null, can be empty.
     */
    List<Scene> getTheatherScene(Integer theaterId);
}
