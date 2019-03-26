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
import java.util.Map;
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.thw.model.*;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IThesaurusHandler {

    /**
     * Analyze a list of keywords and return the result of the analysis.
     *
     * The analysis result contains a list of matching languages
     * (a correct sentence must match only one language) and the list
     * of concept recognized in the keywords.
     *
     * @param terms The keywords to analyze.
     *
     * @return The Result of the analysis.
     */
    Result analyze(final List<String> terms);

    /**
     * Analyze a sentence and return the result of the analysis.
     *
     * The analysis result contains a list of matching languages
     * (a correct sentence must match only one language) and the list
     * of concept recognized in the sentence.
     *
     * @param sentence The string to analyze.
     *
     * @return The Result of the analysis.
     */
    Result analyze(final String sentence);

    List<String> getSupportedLanguageCode();

    String getTranslation(final List<Concept> concept, final ISOLanguageCode languageCode, final List<String> operators);

    /**
     * Return a list of all the words contains in the thesaurus.
     * @return
     */
    List<Word> getWords(final ISOLanguageCode languageCode, final Boolean showDeactivated);
    
    List<Concept> getTopMostConcept(final ISOLanguageCode language, final List<String> thesaurusURI, final List<String> theme);
    
    /**
     * Return a List of String containing all the prefered labels of the different Thesaurus.
     *
     * @param limit limit if its positive, this parameter allows you to limit the number af term you received.
     * @param include
     * @param language
     * @param ignore
     *
     * @return a list of thesaurus terms.
     */
    List<String> getAllPreferedLabels(final int limit, final List<String> include, final ISOLanguageCode language, final List<String> ignore);
    
    List<Concept> getAllConcepts(final int limit, final ISOLanguageCode language, final List<String> thesaurusUri);
    
    List<Concept> getRelatedConcept(final String uriConcept, final String uriRelation, final ISOLanguageCode language);

    List<Concept> getRelatedConcept(final String uriConcept, final String uriRelation, final ISOLanguageCode language, final String thesaurusURI);
    
    List<Mapping> getRelations(final String uriConcept, final String uriThesaurus, final String uriRelation);
    
    Concept getConcept(final String uri, final ISOLanguageCode language, final String thesaurusURI);
    
    List<LanguageString> getAllTranslationForProperty(final String uriConcept, final String property);
    
    List<String> findLabels(final String word, final int searchMode, final List<String> thesaurusURI, final ISOLanguageCode language, final List<String> ignoreThesaurus, final List<String> theme, final Boolean showDeactivated);
    
    Map<String, List<String>> findMatchings(final String word, final int searchMode, final List<String> thesaurusURI, final ISOLanguageCode language, final List<String> ignoreThesaurus, final List<String> theme, final Boolean showDeactivated);
    
    List<Concept> findConcept(final String word, final int searchMode, final List<String> thesaurusURI, final ISOLanguageCode language, final boolean geometric, final List<String> ignoreThesaurus, final List<String> theme, final Boolean showDeactivated);
    
    Concept getGeometricConcept(final String uriConcept);
    
    List<String> getConceptLanguages(final String uri);
    
    List<String> getThesaurusLanguages(final String uri);
    
    Thesaurus getThesaurus(final String uri);
    
    List<Thesaurus> getLoadedThesaurus();
    
    void close();
    
    String getConceptTheme(final String uriConcept);

    String getConceptTheme(final String uriConcept, final String thesaurusURI);
}
