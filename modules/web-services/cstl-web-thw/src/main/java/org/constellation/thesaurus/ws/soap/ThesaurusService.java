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

package org.constellation.thesaurus.ws.soap;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.soap.SOAPMessage;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.thesaurus.core.THWworker;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.soap.OGCWebService;
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.thw.model.LanguageString;
import org.geotoolkit.thw.model.Mapping;
import org.geotoolkit.thw.model.Word;
import org.geotoolkit.thw.model.XmlThesaurus;
import org.geotoolkit.thw.xml.*;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@WebService(name = "ThesaurusService")
@SOAPBinding(parameterStyle = ParameterStyle.WRAPPED)
@XmlSeeAlso({org.geotoolkit.ows.xml.v110.ObjectFactory.class,
            org.geotoolkit.skos.xml.ObjectFactory.class,
            org.geotoolkit.gml.xml.v311.ObjectFactory.class,
            org.apache.sis.internal.jaxb.geometry.ObjectFactory.class})
public class ThesaurusService extends OGCWebService<THWworker> {


    public ThesaurusService() {
        super(Specification.THW);
        LOGGER.log(Level.INFO, "Thesaurus SOAP service running ({0} instances)\n", getWorkerMapSize());
    }

    /**
     * Get the top concepts of a thesaurus.
     *
     * @param thesaurus
     * @param language
     * @return
     */
    @WebMethod(action="getTopmostConcepts")
    public List<Concept> getTopmostConcepts(@WebParam(name = "thesaurus") List<String> thesaurus, @WebParam(name = "language") String language, @WebParam(name = "themes") List<String> themes) throws CstlServiceException {
        LOGGER.info("getTopmostConcepts request received.");
        final THWworker worker = getCurrentWorker();
        final GetTopmostConcepts gtc = new GetTopmostConcepts(thesaurus, language, themes);
        final GetTopmostConceptsResponse response = worker.getTopmostConcepts(gtc);
        return response.getReturn();
    }

    /**
     * Get a list of all concept relatives.
     *
     * @param concept
     * @param thesaurus
     * @param relation
     * @return
     */
    @WebMethod(action="getAllConceptRelatives")
    public List<Mapping> getAllConceptRelatives(@WebParam(name = "concept")URI concept, @WebParam(name = "thesaurus")URI thesaurus, @WebParam(name = "relation")URI relation) throws CstlServiceException  {
        LOGGER.info("getAllConceptRelatives request received.");
        final THWworker worker = getCurrentWorker();
        final GetAllConceptRelatives request = new GetAllConceptRelatives(concept, thesaurus, relation);
        final GetAllConceptRelativesResponse response = worker.getAllConceptRelatives(request);
        return response.getReturn();
    }

    /**
     * Get a list of all concept with a given relation to the object.
     *
     * @param concept
     * @param relation
     * @param language
     * @return
     */
    @WebMethod(action="getRelatedConcepts")
    public List<Concept> getRelatedConcepts(@WebParam(name = "concept")String concept, @WebParam(name = "relation")String relation, @WebParam(name = "language") String language, @WebParam(name = "thesaurus_uri") String thesaurus_uri) throws CstlServiceException  {
        LOGGER.info("getRelatedConcepts request received.");
        final THWworker worker = getCurrentWorker();
        final GetRelatedConcepts request = new GetRelatedConcepts(concept,relation, language, thesaurus_uri, null);
        final GetRelatedConceptsResponse response = worker.getRelatedConcepts(request);
        return response.getReturn();
    }

    /**
     * Get a concept by a known URI.
     *
     * @param uri
     * @param language
     * @return
     */
    @WebMethod(action="getConcept")
    public Concept getConcept(@WebParam(name = "uri")URI uri, @WebParam(name = "language")String language) throws CstlServiceException  {
        final THWworker worker = getCurrentWorker();
        LOGGER.info("getConcept request received: uri=" + uri + " language=" + language);
        final GetConcept request = new GetConcept(uri, language);
        final GetConceptResponse response = worker.getConcept(request);
        return response.getReturn();
    }

    /**
     * Returns all translations for a property of a given concept.
     *
     * @param conceptUri
     * @param propertyUri
     * @return
     */
    @WebMethod(action="getAllTranslationsForConcept")
    public List<LanguageString> getAllTranslationsForConcept(@WebParam(name = "concept_uri")URI conceptUri, @WebParam(name = "property_uri")String propertyUri) throws CstlServiceException  {
        LOGGER.info("getAllTranslationsForConcept request received.");
        final THWworker worker = getCurrentWorker();
        final GetAllTranslationsForConcept request = new GetAllTranslationsForConcept(conceptUri, propertyUri);
        final GetAllTranslationsForConceptResponse response = worker.getAllTranslationsForConcept(request);
        return response.getReturn();
    }

    /**
     * Get a list of concepts matching a keyword for a particular thesaurus.
     *
     * @param keyword
     * @param searchMode
     * @param thesaurus
     * @param language
     * @return
     */
    @WebMethod(action="getConceptsMatchingKeyword")
    public List<Concept> getConceptsMatchingKeyword(@WebParam(name = "keyword")String keyword, @WebParam(name = "searchMode")int searchMode, @WebParam(name = "thesaurus_uri")URI thesaurus,
            @WebParam(name = "language")String language,  @WebParam(name = "geometric")boolean geometric, @WebParam(name = "ignore_thesaurus")List<URI> ignoreList, @WebParam(name = "themes")List<String> themes) throws CstlServiceException  {
        LOGGER.info("getConceptsMatchingKeyword request received.");
        final THWworker worker = getCurrentWorker();
        final GetConceptsMatchingKeyword request = new GetConceptsMatchingKeyword(keyword, searchMode, thesaurus, language, null, geometric, ignoreList, themes);
        final GetConceptsMatchingKeywordResponse response = (GetConceptsMatchingKeywordResponse) worker.getConceptsMatchingKeyword(request);
        return response.getReturn();
    }


    /**
     * Get a list of concepts matching a keyword for a particular thesaurus.
     *
     * @param keyword
     * @param searchMode
     * @param thesaurusList
     * @param language
     * @return
     */
    @WebMethod(action="getNumeredConceptsMatchingKeyword")
    public List<Concept> getNumeredConceptsMatchingKeyword(@WebParam(name = "keyword")String keyword, @WebParam(name = "csw")List<String> csw,
            @WebParam(name = "language")String language,  @WebParam(name = "ignore_csw")List<String> ignoreList, @WebParam(name = "themes")List<String> themes,
            @WebParam(name = "thesaurus_uri")List<String> thesaurusList, @WebParam(name = "showDeactivated")boolean showDeactivated, @WebParam(name = "searchMode")int searchMode) throws CstlServiceException  {
        LOGGER.info("getNumeredConceptsMatchingKeyword request received.");
        final THWworker worker = getCurrentWorker();
        final GetNumeredConceptsMatchingKeyword request = new GetNumeredConceptsMatchingKeyword(keyword, csw, language, null, ignoreList, themes, thesaurusList, showDeactivated, searchMode);
        final GetNumeredConceptsMatchingKeywordResponse response = (GetNumeredConceptsMatchingKeywordResponse) worker.getNumeredConceptsMatchingKeyword(request);
        return response.getReturn();
    }

    /**
     * Get a list of concepts matching a keyword for a particular thesaurus.
     *
     * @param keyword
     * @param searchMode
     * @param thesaurusList
     * @param language
     * @return
     */
    @WebMethod(action="getAggregatedConceptsMatchingKeyword")
    public List<Concept> getAggregatedConceptsMatchingKeyword(@WebParam(name = "keyword")String keyword, @WebParam(name = "csw")List<String> csw,
            @WebParam(name = "language")String language,  @WebParam(name = "ignore_csw")List<String> ignoreList, @WebParam(name = "themes")List<String> themes,
            @WebParam(name = "thesaurus_uri")List<String> thesaurusList, @WebParam(name = "showDeactivated")boolean showDeactivated, @WebParam(name = "searchMode")int searchMode) throws CstlServiceException  {
        LOGGER.info("getAggregatedConceptsMatchingKeyword request received.");
        final THWworker worker = getCurrentWorker();
        final GetAggregatedConceptsMatchingKeyword request = new GetAggregatedConceptsMatchingKeyword(keyword, csw, language, null, ignoreList, themes, thesaurusList, showDeactivated,searchMode);
        final GetAggregatedConceptsMatchingKeywordResponse response = (GetAggregatedConceptsMatchingKeywordResponse) worker.getAggregatedConceptsMatchingKeyword(request);
        return response.getReturn();
    }


    /**
     * Get a list of concepts matching a regex for a particular thesaurus.
     * The language argument is used both for specifying what language to search in and for returning the concept in the correct language.
     *
     * @param regex
     * @param thesaurus
     * @param language
     * @return
     */
    @WebMethod(action="getConceptsMatchingRegexByThesaurus")
    public Concept[] getConceptsMatchingRegexByThesaurus(@WebParam(name = "regex")String regex, @WebParam(name = "thesaurus")URI thesaurus, @WebParam(name = "language")String language) throws CstlServiceException  {
        LOGGER.info("getConceptsMatchingRegexByThesaurus request received.");
        final THWworker worker = getCurrentWorker();
        return null;
    }

    /**
     * Return the languages a concept's preferred label is available in.
     *
     * @param concept
     * @return
     */
    @WebMethod(action="getAvailableLanguages")
    public List<String> getAvailableLanguages(@WebParam(name = "concept")URI concept) throws CstlServiceException {
        LOGGER.info("getAvailableLanguages request received.");
        final THWworker worker = getCurrentWorker();
        final GetAvailableLanguages request = new GetAvailableLanguages(concept);
        final GetAvailableLanguagesResponse response = worker.getAvailableLanguages(request);
        return response.getReturn();
    }

    /**
     * Return all the languages the thesaurus has information in.
     *
     * @param thesaurus
     * @return
     */
    @WebMethod(action="getSupportedLangs")
    public List<String> getSupportedLangs(@WebParam(name = "thesaurus")URI thesaurus) throws CstlServiceException {
        LOGGER.info("getSupportedLangs request received.");
        final THWworker worker = getCurrentWorker();
        final GetSupportedLangs request = new GetSupportedLangs(thesaurus);
        final GetSupportedLangsResponse response = worker.getSupportedLangs(request);
        return response.getReturn();
    }

    /**
     * Return all the thesauri uris, the service knows of
     *
     * @return
     */
    @WebMethod(action="getAvailableThesauri")
    public List<XmlThesaurus> getAvailableThesauri(@WebParam(name="outputFormat") String outputFormat, @WebParam(name = "showDeactivated")boolean showDeactivated) throws CstlServiceException {
        LOGGER.info("getAvailableThesauri request received.");
        final THWworker worker = getCurrentWorker();
        final GetAvailableThesauriResponse response =  (GetAvailableThesauriResponse) worker.getAvailableThesauri(new GetAvailableThesauri(outputFormat, showDeactivated));
        return response.getReturn();
    }

    /**
     * Convenience method that calls getTopmostConcepts('http://www.eionet.europa.eu/gemet/theme/', language)
     *
     * @param language
     * @return
     */
    @WebMethod(action="fetchThemes")
    public List<Concept> fetchThemes(@WebParam(name = "language")String language, @WebParam(name = "themes") List<String> themes) throws CstlServiceException {
        LOGGER.info("fetchThemes request received.");
        final List<String> uri = Arrays.asList("http://www.eionet.europa.eu/gemet/theme/");
        return getTopmostConcepts(uri, language, themes);
    }

    /**
     * Convenience method that calls fetchGroups(language)
     *
     * @param language
     * @return
     */
    @WebMethod(action="fetchGroups")
    public List<Concept> fetchGroups(@WebParam(name = "language")String language, @WebParam(name = "themes") List<String> themes) throws CstlServiceException {
        LOGGER.info("fetchGroups request received.");
       final List<String> uri = Arrays.asList("http://www.eionet.europa.eu/gemet/theme/");
        return getTopmostConcepts(uri, language, themes);
    }

    /**
     * @return
     */
    @WebMethod(action="getLinkedCsw")
    public List<String> getLinkedCsw() throws CstlServiceException {
        LOGGER.info("getLinkedCsw request received.");
        final THWworker worker = getCurrentWorker();
        final GetLinkedCswResponse response = worker.getLinkedCsw(null);
        return response.getReturn();
    }

    /**
     * return all the value for the specified property in the specified language
     *
     * @param language
     * @return
     */
    @WebMethod(action="getAllPreferedLabel")
    public List<Word> getAllPreferedLabel(@WebParam(name = "language")String language, @WebParam(name = "showDeactivated")boolean showDeactivated) throws CstlServiceException {
        LOGGER.info("getAllPreferedLabel request received.");
        final THWworker worker = getCurrentWorker();
        final GetAllPreferedLabel request = new GetAllPreferedLabel(language, showDeactivated, null);
        final GetAllPreferedLabelResponse response = worker.getAllPreferedLabel(request);
        return response.getReturn();
    }

    @Override
    protected SOAPMessage processExceptionResponse(String message, String code, String locator) {
        throw new UnsupportedOperationException("TODO.");
    }

    @Override
    protected Object treatIncomingRequest(Object objectRequest, THWworker worker) throws CstlServiceException {
        throw new UnsupportedOperationException("TODO.");
    }

}
