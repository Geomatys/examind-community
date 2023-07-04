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

package org.constellation.thesaurus.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// constellation dependencies
import org.constellation.api.ServiceDef.Specification;
import org.constellation.api.WorkerState;
import org.constellation.thesaurus.api.IThesaurusBusiness;
import org.constellation.thesaurus.api.IThesaurusCSWCounter;
import org.constellation.ws.AbstractWorker;
import org.constellation.ws.CstlServiceException;
import org.constellation.thesaurus.io.sql.ThesaurusCSWCounter;
import org.constellation.thesaurus.api.IThesaurusHandler;
import org.constellation.thesaurus.io.sql.ThesaurusHandler;


// geotoolkit dependencies
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.skos.xml.RDF;
import org.geotoolkit.ows.xml.OWSExceptionCode;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.Thesaurus;
import org.geotoolkit.thw.model.Word;
import org.geotoolkit.thw.model.XmlThesaurus;
import org.geotoolkit.thw.xml.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("THWWorker")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class THWworker extends AbstractWorker {

    private IThesaurusHandler handler;

    private IThesaurusCSWCounter cswCounter;

    @Autowired
    private IThesaurusBusiness thesaurusBusiness;

    public THWworker(final String serviceID) {
        super(serviceID, Specification.THW, true);
        if (getState().equals(WorkerState.ERROR)) return;
        int nbThesaurus = 0;
        try {

            final List<String> linkedThesaurusURI = serviceBusiness.getLinkedThesaurusUri(getServiceId());

            final List<Thesaurus> thesaurus = new ArrayList<>();

            for (String thURI : linkedThesaurusURI) {
                thesaurus.add(thesaurusBusiness.createThesaurusWriter(thURI));
            }
            handler = new ThesaurusHandler(thesaurus);

            cswCounter = new ThesaurusCSWCounter(handler);

            if (handler == null) {//NOSONAR
                startError("The thesaurus is not working! \n cause: unable to acquire a thesaurus handler", null);
            } else {
                nbThesaurus = handler.getLoadedThesaurus().size();
            }
        } catch (Exception ex) {
            startError("The Thesaurus worker can not start!\nCause:"+ ex.getMessage(), ex);
        }
        started();
    }

    public GetTopmostConceptsResponse getTopmostConcepts(final GetTopmostConcepts request) throws CstlServiceException {
        isWorking();
        final String language           = request.getLanguage();
        final List<String> thesaurusURI = request.getThesaurus();
        final List<String> themes       = request.getTheme();
        final ISOLanguageCode languageC;
        if (language != null) {
            languageC = ISOLanguageCode.fromCode(language);
        } else {
            languageC = null;
        }
        final List<Concept> topConcepts = handler.getTopMostConcept(languageC, thesaurusURI, themes);
        return new GetTopmostConceptsResponse(topConcepts);
    }

    public GetAllConceptRelativesResponse getAllConceptRelatives(final GetAllConceptRelatives request) throws CstlServiceException {
        isWorking();
        final String thesaurusURI = request.getThesaurus();
        final String relation     = request.getRelation();
        final String conceptURI   = request.getConcept();
        return new GetAllConceptRelativesResponse(handler.getRelations(conceptURI, thesaurusURI, relation));
    }

    public GetRelatedConceptsResponse getRelatedConcepts(final GetRelatedConcepts request) throws CstlServiceException {
        isWorking();
        final String language     = request.getLanguage();
        final String relation     = request.getRelation();
        final String conceptURI   = request.getConcept();
        final String thesaurusURI = request.getThesaurusUri();
        final ISOLanguageCode languageC;
        if (language != null) {
            languageC = ISOLanguageCode.fromCode(language);
        } else {
            languageC = null;
        }
        return new GetRelatedConceptsResponse(handler.getRelatedConcept(conceptURI, relation, languageC, thesaurusURI));
    }

    public GetConceptResponse getConcept(final GetConcept request) throws CstlServiceException {
        isWorking();
        final String language   = request.getLanguage();
        final String conceptURI = request.getUri();
        final String thesaurusURI = request.getThesaurusUri();
        final ISOLanguageCode languageC;
        if (language != null) {
            languageC = ISOLanguageCode.fromCode(language);
        } else {
            languageC = null;
        }
        return  new GetConceptResponse(handler.getConcept(conceptURI, languageC, thesaurusURI));
    }

    public GetNumeredConceptResponse getNumeredConcept(final GetNumeredConcept request) throws CstlServiceException {
        isWorking();
        final String language   = request.getLanguage();
        final String conceptURI = request.getUri();
        final String theme      = request.getTheme();
        final String thesaurusURI = request.getThesaurusUri();
        final ISOLanguageCode languageC = ISOLanguageCode.fromCode(language);
        final Concept concept   = handler.getConcept(conceptURI, languageC, thesaurusURI);
        if (concept != null) {
            final Integer count = cswCounter.getNumeredCountForTerm(conceptURI, concept.getPrefLabel(language), language, null, theme);
            concept.setCount(count);
        }
        return  new GetNumeredConceptResponse(concept);
    }

    public GetAggregatedConceptResponse getAggregatedConcept(final GetAggregatedConcept request) throws CstlServiceException {
        isWorking();
        final String language   = request.getLanguage();
        final String conceptURI = request.getUri();
        final String theme      = request.getTheme();
        final String thesaurusURI = request.getThesaurusUri();
        final ISOLanguageCode languageC = ISOLanguageCode.fromCode(language);
        final Concept concept   = handler.getConcept(conceptURI, languageC, thesaurusURI);
        if (concept != null) {
            final Integer count = cswCounter.getAggregatedCountForTerm(conceptURI, concept.getPrefLabel(language), language, null, theme);
            concept.setCount(count);
        }
        return  new GetAggregatedConceptResponse(concept);
    }

    public GetAllTranslationsForConceptResponse getAllTranslationsForConcept(final GetAllTranslationsForConcept request) throws CstlServiceException {
        isWorking();
        final String property   = request.getPropertyUri();
        final String conceptURI = request.getConceptUri();
        return  new GetAllTranslationsForConceptResponse(handler.getAllTranslationForProperty(conceptURI, property));
    }

    public Object getConceptsMatchingKeyword(final GetConceptsMatchingKeyword request) throws CstlServiceException {
        isWorking();
        final String language         = request.getLanguage();
        final String word             = request.getKeyword();
        final int searchMode          = request.getSearchMode();
        final String responseFormat   = request.getOutputFormat();
        final List<String> tu         = request.getThesaurus();
        boolean geometric             = request.getGeometric();
        final List<String> ignoreTh   = request.getIgnoreThesaurus();
        final List<String> themes     = request.getTheme();
        final Boolean showDeactivated = request.getShowDeactivated();
        if (language == null) {
            throw new CstlServiceException("The parameter languague is mandatory", OWSExceptionCode.MISSING_PARAMETER_VALUE, "language");
        }
        final ISOLanguageCode languageC = ISOLanguageCode.fromCode(language);
        final List<String> labels;
        final List<Concept> concepts;
        final Map<String, List<String>> matching;
        if ("simple".equals(request.getOutputFormat())) {
            concepts = null;
            matching = null;
            labels   = handler.findLabels(word, searchMode, tu, languageC, ignoreTh, themes, showDeactivated);
        } else if ("matching".equals(request.getOutputFormat())) {
            concepts = null;
            matching = handler.findMatchings(word, searchMode, tu, languageC, ignoreTh, themes, showDeactivated);
            labels   = null;
        } else {
            concepts = handler.findConcept(word, searchMode, tu, languageC, geometric, ignoreTh, themes, showDeactivated);
            matching = null;
            labels   = null;
        }

        if ("RDF".equalsIgnoreCase(responseFormat)) {
            return new RDF(null, concepts);
        } else {
            return new GetConceptsMatchingKeywordResponse(concepts, labels, matching);
        }
    }

    public GetAvailableLanguagesResponse getAvailableLanguages(final GetAvailableLanguages request) throws CstlServiceException {
        isWorking();
        final String conceptURI = request.getConcept();
        return new GetAvailableLanguagesResponse(handler.getConceptLanguages(conceptURI));
    }

    public GetSupportedLangsResponse getSupportedLangs(final GetSupportedLangs request) throws CstlServiceException {
        isWorking();
        final String thesaurusURI = request.getThesaurus();
        return new GetSupportedLangsResponse(handler.getThesaurusLanguages(thesaurusURI));
    }

    public Object getAvailableThesauri(final GetAvailableThesauri request) throws CstlServiceException {
        isWorking();
        final List<Thesaurus> thesaurus = handler.getLoadedThesaurus();

        if ("RDF".equalsIgnoreCase(request.getOutputFormat())) {
            final List<Concept> concepts = new ArrayList<>();
            for (Thesaurus t : thesaurus) {
                if (request.getShowDeactivated() || t.getState()) {
                    final Concept c = new Concept(t.getURI());
                    c.setName(t.getName());
                    c.setDescription(t.getDescription());
                    // miss version
                    concepts.add(c);
                }
            }
            return new RDF(concepts);
        } else {
            final List<XmlThesaurus> xmlt = new ArrayList<>();
            for (Thesaurus t : thesaurus) {
                if (request.getShowDeactivated() || t.getState()) {
                    xmlt.add(new XmlThesaurus(t));
                }
            }
            return new GetAvailableThesauriResponse(xmlt);
        }
    }

    public GetAllPreferedLabelResponse getAllPreferedLabel(final GetAllPreferedLabel request) throws CstlServiceException {
        isWorking();
        final String language   = request.getLanguage();
        final ISOLanguageCode languageC = ISOLanguageCode.fromCode(language);
        final List<Word> words = handler.getWords(languageC, request.getShowDeactivated());
        return new GetAllPreferedLabelResponse(words);
    }

    public Object GetGeometricConcept(final GetGeometricConcept request) throws CstlServiceException {
        isWorking();
        final String uriConcept      = request.getUriConcept();
        final String responseFormat  = request.getOutputFormat();
        final Concept concept = handler.getGeometricConcept(uriConcept);
        if ("RDF".equalsIgnoreCase(responseFormat)) {
            final RDF rdf = new RDF(Arrays.asList(concept));
            return rdf;
        } else {
            return new GetGeometricConceptResponse(concept);
        }
    }

    public Object getNumeredConceptsMatchingKeyword(final GetNumeredConceptsMatchingKeyword gcmk) throws CstlServiceException {
        isWorking();
        final List<String> thList = new ArrayList<>();
        for (Thesaurus th : handler.getLoadedThesaurus()) {
            final String thUri = th.getURI();
            if ((gcmk.getThesaurus() == null       || gcmk.getThesaurus().isEmpty() || gcmk.getThesaurus().contains(thUri)) &&
                (gcmk.getIgnoreThesaurus() == null || !gcmk.getIgnoreThesaurus().contains(thUri)) &&
                (gcmk.getShowDeactivated()         || th.getState())) {
                thList.add(thUri);
            }
        }

        final List<String> terms = cswCounter.getTermsMatchingKeywords(gcmk.getKeyword(), gcmk.getCsw(), gcmk.getIgnoreCsw(), gcmk.getLanguage(), false, gcmk.getTheme(), thList, gcmk.getSearchMode());
        return new GetNumeredConceptsMatchingKeywordResponse(null, terms);
    }

    public Object getAggregatedConceptsMatchingKeyword(final GetAggregatedConceptsMatchingKeyword gcmk) throws CstlServiceException {
        isWorking();
        final List<String> thList = new ArrayList<>();
        for (Thesaurus th : handler.getLoadedThesaurus()) {
            final String thUri = th.getURI();
            if ((gcmk.getThesaurus() == null       || gcmk.getThesaurus().isEmpty() || gcmk.getThesaurus().contains(thUri)) &&
                (gcmk.getIgnoreThesaurus() == null || !gcmk.getIgnoreThesaurus().contains(thUri))&&
                (gcmk.getShowDeactivated()         || th.getState())) {
                thList.add(thUri);
            }
        }
        final List<String> terms = cswCounter.getTermsMatchingKeywords(gcmk.getKeyword(), gcmk.getCsw(), gcmk.getIgnoreCsw(), gcmk.getLanguage(), true, gcmk.getTheme(), thList, gcmk.getSearchMode());
        return new GetAggregatedConceptsMatchingKeywordResponse(null, terms);
    }

    public GetLinkedCswResponse getLinkedCsw(final GetLinkedCsw glc) throws CstlServiceException {
        isWorking();
        final List<String> linkedCsw = cswCounter.getLinkedCsw();
        return new GetLinkedCswResponse(linkedCsw);
    }

    public GetAggregatedConceptIdsResponse getAggregatedConceptIds(final GetAggregatedConceptIds request) throws CstlServiceException {
        isWorking();
        final String language   = request.getLanguage();
        final String conceptURI = request.getUri();
        final String theme      = request.getTheme();
        final String thesaurusURI = request.getThesaurusUri();
        final ISOLanguageCode languageC = ISOLanguageCode.fromCode(language);
        final Concept concept   = handler.getConcept(conceptURI, languageC, thesaurusURI);
        final List<String> results;
        if (concept != null) {
            results = cswCounter.getAggregatedIdsForTerm(conceptURI, concept.getPrefLabel(language), language, null, theme);
        } else {
            results = new ArrayList<>();
        }
        return new GetAggregatedConceptIdsResponse(results);

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void destroy() {
        if (handler != null) {
            handler.close();
        }
        if (cswCounter != null) {
            cswCounter.close();
        }
        stopped();
    }
}
