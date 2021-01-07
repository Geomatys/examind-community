/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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
import org.constellation.dto.thesaurus.Thesaurus;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface ThesaurusRepository extends AbstractRepository {

    /**
     * Return all the available thesaurus
     * @return
     */
    List<Thesaurus> getAll();

    /**
     * Store a new thesaurus into the datasource.
     *
     * @param thesaurus
     * @return
     */
    Integer create(Thesaurus thesaurus);

    /**
     * Find a thesaurus identified by its URI.
     *
     * @param uri
     * @return
     */
    Thesaurus getByUri(String uri);

    /**
     * Find a thesaurus identified by its name.
     *
     * @param name
     * @return
     */
    Thesaurus getByName(String name);

    /**
     * Find a thesaurus with the specified id.
     *
     * @param id
     * @return
     */
    Thesaurus get(int id);

    void update(Thesaurus thesaurus);

    List<Thesaurus> getLinkedThesaurus(int serviceId);

    List<String> getLinkedThesaurusUri(int serviceId);

    void linkThesaurusAndService(int thesaurusId, int serviceId);

}
