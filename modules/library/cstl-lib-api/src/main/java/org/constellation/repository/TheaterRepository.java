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
import org.constellation.dto.Theater;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface TheaterRepository {

    /**
     * List all 3D theaters.
     *
     * @return never null, can be empty.
     */
    List<Theater> findAll();

    /**
     * Get theater for given identifier.
     *
     * @param id theater identifier, not null.
     * @return can be null if not found.
     */
    Theater findById(Integer id);

    /**
     * Get theater for given name.
     *
     * @param name theater name, not null.
     * @return can be null if not found.
     */
    Theater findByName(String name);

    /**
     * Delete theater for given identifier.
     *
     * @param id theater identifier, not null.
     * @return number of deleted theaters.
     */
    int delete(Integer id);

    /**
     * Store given theater.
     *
     * @param theater not null
     * @return stored theater identifier
     */
    int create(Theater theater);

    /**
     * Add a scene in theater.
     *
     * @param id theater identifier, not null
     * @param sceneId scene identifier, not null
     */
    void addScene(Integer id, Integer sceneId);

    /**
     * Remove all sscenes from theater.
     *
     * @param id theater identifier, not null
     */
    void removeAllScene(Integer id);

    /**
     * Remove scene from theater.
     *
     * @param id theater identifier, not null
     * @param sceneId scene identifier, not null
     */
    void removeScene(Integer id, Integer sceneId);

    /**
     * Find theaters using given scene.
     *
     * @param sceneId searched scene identifier, not null
     * @return never null, can be empty.
     */
    List<Theater> findForScene(Integer sceneId);
}
