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
package org.constellation.thesaurus.api;


import java.util.Map;
import org.constellation.dto.thesaurus.Thesaurus;
import org.geotoolkit.thw.model.WriteableThesaurus;

/**
 * API to manage constellation Thesaurus
 *
 * @author Quentin Boileau (Geomatys)
 */
public interface IThesaurusBusiness {

    /**
     * List all loaded thesaurus
     * @return Thesaurus map where key is thesaurus URI
     */
    Map<String, Thesaurus> listThesaurus();

    /**
     * Search thesaurus URI from thesaurus name.
     *
     * @param thesaurusName searched thesaurus name
     * @return thesaurus URI String
     * @throws ThesaurusException if thesaurus name is not contained in loaded thesaurus list.
     */
    String getThesaurusURIByName(String thesaurusName) throws ThesaurusException;

    /**
     * Create new {@link WriteableThesaurus} object to manipulate thesaurus.
     * In case of Geosud project, returned {@link WriteableThesaurus} is an
     * instance of {@link WriteableThesaurus}.
     *
     * @param thesaurusURI Thesaurus URI (should be in loaded thesaurus list)
     * @return a new instance of {@link WriteableThesaurus}
     * @throws ThesaurusException if thesaurus URI is invalid
     */
    WriteableThesaurus createThesaurusWriter(String thesaurusURI) throws ThesaurusException;

    /**
     * Create new thesaurus.
     * In case of Geosud project, returned {@link Thesaurus} is an
     * instance of {@link Thesaurus}.
     *
     * @param thesaurus thesaurus model
     * @return a new instance of {@link Thesaurus}
     * @throws ThesaurusException an error occurred
     */
    WriteableThesaurus createNewThesaurus(Thesaurus thesaurus) throws ThesaurusException;

    /**
     * Delete a thesaurus with given URI
     * @param thesaurusURI thesaurus URI to delete
     * @throws ThesaurusException if thesaurus URI is invalid
     */
    void deleteThesaurus(String thesaurusURI) throws ThesaurusException;

}
