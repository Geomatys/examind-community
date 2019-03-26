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

import java.util.List;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IThesaurusCSWCounter {
    
    void refreshThesaurusCSWCount();
    
    void storeLinkedCsw(final List<String> linkedCsws);
    
    /**
     * Return A list of matching keywords for the specified piece of word (matching ones start with the word)
     * int the specified services and the specified language.
     * 
     * @param keyword
     * @param csw
     * @param ignoreCsw
     * @param language
     * @param aggregated 
     * @param themes 
     * @param thesaurusList 
     * @param SearchMode 
     * @return 
     */
    List<String> getTermsMatchingKeywords(final String keyword, final List<String> csw, final List<String> ignoreCsw, 
            final String language, final boolean aggregated, final List<String> themes, final List<String> thesaurusList, final int SearchMode);
    
    Integer getNumeredCountForTerm(final String uriConcept, final String term, final String language, final List<String> csw, final String theme);
    
    Integer getAggregatedCountForTerm(final String uriConcept, final String term, final String language, final List<String> csw, final String theme);
    
    /**
     * Return the current linked CSW service, to the thesaurus service.
     * @return 
     */
    List<String> getLinkedCsw();
    
    List<String> getAggregatedIdsForTerm(final String uriConcept, final String prefLabel, final String language, final List<String> csw, final String theme);
    
    void close();
}
