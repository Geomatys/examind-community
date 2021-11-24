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


package org.constellation.thesaurus.io.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;

import org.constellation.thesaurus.api.IThesaurusHandler;

// Geotoolkit dependencies
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.skos.xml.Value;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.LanguageString;
import org.geotoolkit.thw.model.Mapping;
import org.geotoolkit.thw.model.Result;
import org.geotoolkit.thw.model.ScoredConcept;
import org.geotoolkit.thw.model.Thesaurus;
import org.geotoolkit.thw.model.UnknowConcept;
import org.geotoolkit.thw.model.Word;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ThesaurusHandler implements IThesaurusHandler {

    private static final Logger LOGGER = Logging.getLogger("oorg.constellation.thesaurus.io.sql");

    /**
     * A list of thesaurus.
     */
    private final List<Thesaurus> thesaurus;

    /**
     * Initialize the Thesaurus handler by reading the properties file.
     * @param thesaurus A list of managed Thesaurus.
     */
    public ThesaurusHandler(final List<Thesaurus> thesaurus) {
        this.thesaurus            = thesaurus;
    }

    
    /**
     * {@inheritDoc }
     */
    @Override
    public Result analyze(final String sentence) {
        final List<String> terms     = new ArrayList<>();
        final List<String> operators = new ArrayList<>();

        if (sentence != null) {
            String tmp = sentence;
            //we remove the operators
            int pos;
            while ((pos = tmp.indexOf("AND,")) != -1) {
                final String start = tmp.substring(0, pos);
                tmp           = start + tmp.substring(pos + 3);
                operators.add("AND");
            }

            while ((pos = tmp.indexOf("OR,")) != -1) {
                final String start = tmp.substring(0, pos);
                tmp           = start + tmp.substring(pos + 2);
                operators.add("OR");
            }

            while ((pos = tmp.indexOf("NOT,")) != -1) {
                final String start = tmp.substring(0, pos);
                tmp           = start + tmp.substring(pos + 3);
                operators.add("NOT");
            }

            // we split the sentence in words
            final StringTokenizer tokenizer = new StringTokenizer(tmp, ",");
            while (tokenizer.hasMoreTokens()) {
                final String token = tokenizer.nextToken().trim();
                terms.add(token);
            }
        }
        final Result r = analyze(terms);
        r.setOperators(operators);
        return r;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Result analyze(final List<String> terms) {
        final Result result       = new Result();
        int nbUnknowConcept = 0;

        //we search each term one by one
        for (String term : terms) {

            final List<ScoredConcept> termMatchingConcept = new ArrayList<>();

            // we search the term in each Thesaurus
            for (Thesaurus t : thesaurus) {
                final List<ISOLanguageCode> languages = t.getLanguage();

                for (ISOLanguageCode language : languages) {
                    final List<ScoredConcept> matchingConcept = t.search(term, language);
                    if (!matchingConcept.isEmpty()) {

                        //debug
                        String singleScore = "";
                        if (matchingConcept.size() == 1) {
                            singleScore = " score " + matchingConcept.get(0).score;
                        }
                        LOGGER.info("Term: " + term + " language matching:" + language.getName() + " (" + matchingConcept.size() + " concept matching) in " + t.getName() + singleScore + "\n");

                        // (1) normal case only one Concept matching the term
                        if (matchingConcept.size() == 1) {
                            final ScoredConcept sc = matchingConcept.get(0);

                            // if the list of matching concept for this term already contains this concept
                            final ScoredConcept already = alreadyContainsConcept(termMatchingConcept, sc.uriConcept);
                            if (already != null) {

                                if (language.equals(already.language)) {
                                    LOGGER.info("identical Concept found\n");
                                } else {
                                    LOGGER.info("Concept identical in different language\n");
                                    already.language = null;
                                }
                            } else {
                                termMatchingConcept.add(sc);
                            }

                        // (2) there is more than one Concept matching.
                        } else {

                            LOGGER.info("There is more than one concept matching.");
                            //we must decide which is the best concept, so we use the score
                            ScoredConcept bestConcept = matchingConcept.get(0);;
                            double bestScore          = bestConcept.score;
                            for (ScoredConcept sc : matchingConcept) {
                                if (sc.score > bestScore) {
                                    bestConcept = sc;
                                    bestScore   = sc.score;
                                }
                            }

                            //for debug
                            matchingConcept.remove(bestConcept);
                            for (ScoredConcept sc : matchingConcept) {
                                if (sc.score == bestScore) {
                                    LOGGER.severe("The concept " + sc.uriConcept + " was matching as well with the same score:" + bestScore);
                                }
                            }

                            final ScoredConcept already = alreadyContainsConcept(termMatchingConcept, bestConcept.uriConcept);
                            if (already != null) {

                                if (language.equals(already.language)) {
                                    LOGGER.info("identical Concept found\n");
                                } else {
                                    LOGGER.info("Concept identical in different language\n");
                                    if (termMatchingConcept.size() != 1) {
                                        already.language = null;
                                    }
                                }
                            } else {
                                termMatchingConcept.add(bestConcept);
                            }

                        }
                    }
                }
            }

            final Concept choosenConcept;
            final ISOLanguageCode choosenLanguage;

            // if there is no mapping for the term we build an UnknowConcept
            if (termMatchingConcept.isEmpty()) {
                choosenConcept = new UnknowConcept("urn:concept:unknow:" + nbUnknowConcept, term);
                result.addMatchingConcept(choosenConcept);
                nbUnknowConcept++;

            // normal case one mapping for the term
            } else if (termMatchingConcept.size() == 1) {
                choosenConcept  = termMatchingConcept.get(0).getConcept();
                choosenLanguage = termMatchingConcept.get(0).language;
                String langCode;
                if (choosenLanguage == null) {
                    langCode = null;
                } else {
                    langCode = choosenLanguage.getTwoLetterCode();
                }
                result.addMatchingConcept(choosenConcept);
                if (!result.getMatchingLanguage().contains(langCode)) {
                    result.addMatchingLanguage(langCode);
                }


            // if there is more than one mapping for the term we must choose the best score
            } else {
                ScoredConcept bestConcept = termMatchingConcept.get(0);
                double bestScore          = bestConcept.score;
                for (ScoredConcept sc : termMatchingConcept) {
                    if (sc.score > bestScore) {
                        bestConcept = sc;
                        bestScore   = sc.score;
                    }
                }
                choosenConcept  = bestConcept.getConcept();
                choosenLanguage = bestConcept.language;
                result.addMatchingConcept(choosenConcept);
                if (!result.getMatchingLanguage().contains(choosenLanguage.getTwoLetterCode())) {
                    result.addMatchingLanguage(choosenLanguage.getTwoLetterCode());
                }

                // for debug
                termMatchingConcept.remove(bestConcept);
                for (ScoredConcept sc : termMatchingConcept) {
                    if (sc.score == bestScore) {
                        LOGGER.severe("The concept " + sc.uriConcept + '(' + sc.originThesaurus.getName() + ')' + " was matching as well with the same score:" + bestScore);
                    }
                }
            }
        }

        // if all the terms match both language (or none) we add the default language
        if (!result.getMatchingConcept().isEmpty() && result.getMatchingLanguage().isEmpty()) {
            LOGGER.info("No specific language found taking default one.");
            result.addMatchingLanguage(thesaurus.get(0).getLanguage().get(0).getTwoLetterCode());
        }
        return result;
    }


    /**
     * if the list of scoredConcept already contains a concept with the specified URI it will be return.
     *
     * @param list A list of ScoredConcept
     * @param URIConcept The URI of the concept we are looking for.
     *
     * @return A Concept matching the specified URI or null if the list does not contains a match.
     */
    private static ScoredConcept alreadyContainsConcept(final List<ScoredConcept> list, final String uriConcept) {
        for (ScoredConcept c : list) {
            if (c.uriConcept.equals(uriConcept)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Free the resources before destroying the handler.
     */
    @Override
    public void close() {
        for (Thesaurus t : thesaurus) {
            t.close();
        }
    }

    @Override
    public List<LanguageString> getAllTranslationForProperty(final String uriConcept, final String property) {

        final List<LanguageString> response = new ArrayList<>();
        //we find the thesaurus for the specified language
        for (Thesaurus tmp : thesaurus) {
            final Concept c = tmp.getConcept(uriConcept);
            if (c != null) {
                final List<Value> values = c.getPropertyValues(property);
                for (Value v : values) {
                    response.add(new LanguageString(v.getLang(), v.getValue()));
                }
            }
        }
        return response;
    }

    @Override
    public String getTranslation(final List<Concept> concepts, final ISOLanguageCode languageCode, final List<String> operators) {
        final int nbOperators;
        if (operators == null) {
            nbOperators = 0;
        } else {
            nbOperators = operators.size();
        }

        final StringBuilder translation = new StringBuilder();
        int i              = 0;
        for (Concept c : concepts) {
            translation.append(c.getPrefLabel(languageCode.getTwoLetterCode())).append(' ');
            if (i < nbOperators) {
                translation.append(operators.get(i)).append(' ');
            }
            i++;
        }
        return translation.toString();
    }

    @Override
    public List<Concept> findConcept(final String word, final int searchMode, final List<String> thesaurusURI, final ISOLanguageCode language,
    final boolean geometric, final List<String> ignoreThesaurus, final List<String> theme, final Boolean showDeactivated) {
        final List<Concept> response = new ArrayList<>();
        for (Thesaurus thesau : thesaurus) {
            if ((thesaurusURI == null || thesaurusURI.isEmpty() || thesaurusURI.contains(thesau.getURI())) &&
                (language     == null || thesau.getLanguage().contains(language)) &&
                (ignoreThesaurus == null || !ignoreThesaurus.contains(thesau.getURI())) &&
                (showDeactivated || thesau.getState())) {
                response.addAll(thesau.search(word, searchMode, geometric, theme, language));
            }
        }
        return response;
    }

    @Override
    public List<String> findLabels(final String word, final int searchMode, final List<String> thesaurusURI, final ISOLanguageCode language,
    final List<String> ignoreThesaurus, final List<String> theme, final Boolean showDeactivated) {
        final List<String> response = new ArrayList<>();
        for (Thesaurus thesau : thesaurus) {
            if ((thesaurusURI == null || thesaurusURI.isEmpty() || thesaurusURI.contains(thesau.getURI())) &&
                (language     == null || (thesau.getLanguage().contains(language))) &&
                (!ignoreThesaurus.contains(thesau.getURI())) &&
                (showDeactivated || thesau.getState())) {
                response.addAll(thesau.searchLabels(word, searchMode, theme, language));
            }
        }
        return response;
    }

    @Override
    public Map<String, List<String>> findMatchings(final String word, final int searchMode, final List<String> thesaurusURI, final ISOLanguageCode language,
    final List<String> ignoreThesaurus, final List<String> theme, final Boolean showDeactivated) {
        final Map<String, List<String>> response = new HashMap<>();
        for (Thesaurus thesau : thesaurus) {
            if ((thesaurusURI == null || thesaurusURI.isEmpty() || thesaurusURI.contains(thesau.getURI())) &&
                (language     == null || (thesau.getLanguage().contains(language))) &&
                (!ignoreThesaurus.contains(thesau.getURI())) &&
                (showDeactivated || thesau.getState())) {
                final List<Concept> concepts = thesau.search(word, searchMode, false, theme, language);
                if (!concepts.isEmpty()) {
                    final List<String> conceptURIs = new ArrayList<>();
                    for (Concept c : concepts) {
                        conceptURIs.add(c.getAbout());
                    }
                    response.put(thesau.getURI(), conceptURIs);
                }
            }
        }
        return response;
    }

    /**
     * Return a list of languages supported by this handler.
     *
     * @return The list of languages supported by this handler.
     */
    public List<ISOLanguageCode> getSupportedLanguages() {
        final List<ISOLanguageCode> response = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
            final List<ISOLanguageCode> ls = t.getLanguage();
            for (ISOLanguageCode l : ls) {
                if (!response.contains(l)) {
                    response.add(l);
                }
            }
        }
        return response;
    }

    /**
     * Return a List of String containing all the prefered and alternatives labels of the different Thesaurus.
     *
     * @param limit if its positive, this parameter allows you to limit the number af term you received.
     * @param include
     * @param language
     * @param ignore
     *
     * @return a list of thesaurus terms.
     */
    public List<String> getAllLabels(final int limit, final List<String> include, final ISOLanguageCode language, final List<String> ignore) {
        final List<String> response = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
             if ((include  == null || include.isEmpty() || include.contains(t.getURI())) &&
                (language == null || (t.getLanguage().contains(language))) &&
                (ignore   == null || ignore.isEmpty()  || !ignore.contains(t.getURI()))) {

                 if ((limit > 0 && response.size() < limit) || limit < 0) {
                    response.addAll(t.getAllLabels(limit, language));
                } else if (response.size() > limit) {
                    return response;
                }
             }
        }
        return response;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<String> getAllPreferedLabels(final int limit, final List<String> include, final ISOLanguageCode language, final List<String> ignore) {
        final List<String> response = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
            if ((include  == null || include.isEmpty() || include.contains(t.getURI())) &&
                (language == null || (t.getLanguage().contains(language))) &&
                (ignore   == null || ignore.isEmpty()  || !ignore.contains(t.getURI()))) {

                if ((limit > 0 && response.size() < limit) || limit < 0) {
                    response.addAll(t.getAllPreferedLabels(limit, language));

                } else if (response.size() > limit) {
                    return response;
                }
            }
        }
        return response;
    }

    /**
     * Return A list of String expressing the name and language of the loaded Thesaurus.
     * They are expressed as "thesaurus name (thesaurus language).
     *
     * @return a list of thesaurus description.
     */
    public List<String> getLoadedThesaurusName() {
        final List<String> response = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
            response.add(t.getName());
        }
        return response;
    }

    /**
     * Return A list of the loaded Thesaurus.
     *
     * @return a list of thesaurus.
     */
    @Override
    public List<Thesaurus> getLoadedThesaurus() {
        return thesaurus;
    }

    @Override
    public Thesaurus getThesaurus(final String uri) {
        for (Thesaurus t : thesaurus) {
            if (t.getURI().equals(uri)) {return t;}
        }
        return null;
    }

    @Override
    public Concept getConcept(final String uri, final ISOLanguageCode language, final String thesaurusURI) {
        for (Thesaurus t : thesaurus) {
            if ((t.getURI() != null && t.getURI().equals(thesaurusURI)) || thesaurusURI == null) {
                if (language == null || t.getLanguage().contains(language)) {
                    final Concept c = t.getConcept(uri, language);
                    if (c != null) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public List<String> getConceptLanguages(final String uri) {
        final List<String> languages = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
            final Concept c = t.getConcept(uri);
            if (c != null) {
                for (ISOLanguageCode l : t.getLanguage()) {
                    if (!languages.contains(l.getTwoLetterCode())) {
                        languages.add(l.getTwoLetterCode());
                    }
                }
            }
        }
        return languages;
    }

    @Override
    public List<String> getThesaurusLanguages(final String uri) {
        final List<String> languages = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
            if (t.getURI() != null && t.getURI().equals(uri)) {
                final List<ISOLanguageCode> lc = t.getLanguage();
                for (ISOLanguageCode l : lc) {
                    if (!languages.contains(l.getTwoLetterCode())) {
                        languages.add(l.getTwoLetterCode());
                    }
                }
            }
        }
        return languages;
    }

    @Override
    public List<Mapping> getRelations(final String uriConcept, final String uriThesaurus, final String uriRelation) {
       final List<Mapping> response = new ArrayList<>();
        for (Thesaurus t: thesaurus) {
            if (uriThesaurus == null || uriThesaurus.equals(t.getURI())) {
                final Concept c = t.getConcept(uriConcept);
                if (c != null) {
                    if (uriRelation == null) {
                        final Map<String, String> relations = c.getRelations();
                        for (Entry<String,String> entry : relations.entrySet()) {
                            final String relation = entry.getValue();
                            final String target   = entry.getKey();
                            final Mapping m = new Mapping(uriConcept, relation, target);
                            if (!response.contains(m)) {
                                response.add(m);
                            }
                        }
                    } else {
                        final List<String> relations = c.getRelations(uriRelation);
                        for (String value : relations) {
                            final Mapping m = new Mapping(uriConcept, uriRelation, value);
                            if (!response.contains(m)) {
                               response.add(m);
                            }
                        }
                    }
                }
            }
        }
        return response;
    }

    @Override
    public List<Concept> getRelatedConcept(final String uriConcept, final String uriRelation, final ISOLanguageCode language) {
        final List<Concept> response = new ArrayList<>();
        for (Thesaurus t: thesaurus) {
            if (language == null || t.getLanguage().contains(language)) {
                final Concept c = t.getConcept(uriConcept, language);
                if (c != null) {
                    if (uriRelation != null) {
                        final List<String> relations = c.getRelations(uriRelation);
                        if (relations != null) {
                            for (String value : relations) {
                                final Concept rc = t.getConcept(value, language);
                                response.add(rc);
                            }
                        }
                    } else {
                        final Map<String, String> relations = c.getRelations();
                        if (relations != null) {
                            for (String value : relations.keySet()) {
                                final Concept rc = t.getConcept(value, language);
                                response.add(rc);
                            }
                        }
                    }
                }
            }
        }
        return response;
    }

    @Override
    public List<Concept> getRelatedConcept(final String uriConcept, final String uriRelation, final ISOLanguageCode language, final String thesaurusURI) {
        final List<Concept> response = new ArrayList<>();
        for (Thesaurus t: thesaurus) {
            if(thesaurusURI != null && thesaurusURI.equals(t.getURI())){
                if (language == null || t.getLanguage().contains(language)) {
                    final Concept c = t.getConcept(uriConcept, language);
                    if (c != null) {
                        if (uriRelation != null) {
                            final List<String> relations = c.getRelations(uriRelation);
                            if (relations != null) {
                                for (String value : relations) {
                                    final Concept rc = t.getConcept(value, language);
                                    response.add(rc);
                                }
                            }
                        } else {
                            final Map<String, String> relations = c.getRelations();
                            if (relations != null) {
                                for (String value : relations.keySet()) {
                                    final Concept rc = t.getConcept(value, language);
                                    response.add(rc);
                                }
                            }
                        }
                    }
                }
            }
            
        }
        return response;
    }

    @Override
    public List<Concept> getAllConcepts(final int limit, final ISOLanguageCode language, final List<String> thesaurusURI) {
        final List<Concept> response = new ArrayList<>();
        for (Thesaurus t: thesaurus) {
            if (thesaurusURI == null || thesaurusURI.contains(t.getURI()) &&
                t.getLanguage().contains(language) ) {
                response.addAll(t.getAllConcepts(limit));
                if (limit != -1 && response.size() >= limit ) {
                    return response;
                }
            }
        }
        return response;
    }

    @Override
    public Concept getGeometricConcept(final String uriConcept) {
        for (Thesaurus t: thesaurus) {
            final Concept c = t.getGeometricConcept(uriConcept);
            if (c != null) {return c;}
        }
        return null;
    }

    @Override
    public List<String> getSupportedLanguageCode() {
        final List<String> response = new ArrayList<>();
        for (Thesaurus t : thesaurus) {
            final List<ISOLanguageCode> lc = t.getLanguage();
            for (ISOLanguageCode l : lc) {
                if (!response.contains(l.getTwoLetterCode())) {
                    response.add(l.getTwoLetterCode());
                }
            }
        }
        return response;
    }

    @Override
    public List<Concept> getTopMostConcept(final ISOLanguageCode language, final List<String> thesaurusURI, final List<String> theme) {
        final List<Concept> response = new ArrayList<>();
        for (Thesaurus t: thesaurus) {
            if (thesaurusURI == null || thesaurusURI.contains(t.getURI())) {
                if (language == null || t.getLanguage().contains(language)) {
                    response.addAll(t.getTopMostConcepts(theme, language));
                }
            }
        }
        return response;
    }

    @Override
    public synchronized List<Word> getWords(final ISOLanguageCode language, final Boolean showDeactivated) {

        final List<Word> buffer = new ArrayList<>();
        int before              = 0;
        final StringBuilder sb  = new StringBuilder("Words loaded by schema:\n");
        for (final Thesaurus t : thesaurus) {
            if ((language == null || t.getLanguage().contains(language)) &&
                (showDeactivated  || t.getState())){
                t.getWords(buffer, language);
                int nbWord = buffer.size() - before;
                before = buffer.size();
                sb.append("schema:").append(t.getName()).append(" ").append(nbWord).append('\n');
            }
        }
        LOGGER.info(sb.toString());
        return buffer;
    }

    @Override
    public String getConceptTheme(final String uriConcept) {
        for (Thesaurus t: thesaurus) {
            final String s = t.getConceptTheme(uriConcept);
            if (s != null) {return s;}
        }
        return null;
    }

    @Override
    public String getConceptTheme(final String uriConcept, final String thesaurusURI){
        for (Thesaurus t: thesaurus) {
            if (thesaurusURI != null && thesaurusURI.equals(t.getURI())){
                return t.getConceptTheme(uriConcept);
            }
        }
        return null;
    }
}
