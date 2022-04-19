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

    List<Scene> findAll();

    Scene findById(Integer id);

    Scene findByName(String name);

    int delete(Integer id);

    boolean isUsedName(String name);

    void create(Scene scene);

    void updateStatus(int id, String status);

    void updateBBox(int id, Double minx, Double maxx, Double miny, Double maxy);

    void update(Scene scene);

    List<Scene> getTheatherScene(Integer theaterId);
}
