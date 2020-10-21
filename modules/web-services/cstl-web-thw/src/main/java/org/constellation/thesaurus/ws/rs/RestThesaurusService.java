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

package org.constellation.thesaurus.ws.rs;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

// constellation dependencies
import org.constellation.api.ServiceDef;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.Worker;
import org.constellation.ws.rs.OGCWebService;

//geotoolkit dependencies
import org.geotoolkit.ows.xml.v110.ExceptionReport;
import org.geotoolkit.util.StringUtilities;

import org.constellation.thesaurus.core.THWworker;
import org.constellation.ws.rs.ResponseObject;
import static org.geotoolkit.ows.xml.OWSExceptionCode.*;

import org.geotoolkit.skos.xml.RDF;
import org.geotoolkit.thw.xml.FetchGroups;
import org.geotoolkit.thw.xml.FetchThemes;
import org.geotoolkit.thw.xml.GetAggregatedConcept;
import org.geotoolkit.thw.xml.GetAggregatedConceptIds;
import org.geotoolkit.thw.xml.GetAggregatedConceptResponse;
import org.geotoolkit.thw.xml.GetAggregatedConceptsMatchingKeyword;
import org.geotoolkit.thw.xml.GetAllConceptRelatives;
import org.geotoolkit.thw.xml.GetGeometricConcept;
import org.geotoolkit.thw.xml.GetAllPreferedLabel;
import org.geotoolkit.thw.xml.GetAllPreferedLabelResponse;
import org.geotoolkit.thw.xml.GetAllTranslationsForConcept;
import org.geotoolkit.thw.xml.GetAllTranslationsForConceptResponse;
import org.geotoolkit.thw.xml.GetAvailableLanguages;
import org.geotoolkit.thw.xml.GetAvailableLanguagesResponse;
import org.geotoolkit.thw.xml.GetAvailableThesauri;
import org.geotoolkit.thw.xml.GetConcept;
import org.geotoolkit.thw.xml.GetConceptResponse;
import org.geotoolkit.thw.xml.GetConceptsMatchingKeyword;
import org.geotoolkit.thw.xml.GetConceptsMatchingRegexByThesaurus;
import org.geotoolkit.thw.xml.GetLinkedCsw;
import org.geotoolkit.thw.xml.GetLinkedCswResponse;
import org.geotoolkit.thw.xml.GetNumeredConcept;
import org.geotoolkit.thw.xml.GetNumeredConceptResponse;
import org.geotoolkit.thw.xml.GetNumeredConceptsMatchingKeyword;
import org.geotoolkit.thw.xml.GetRelatedConcepts;
import org.geotoolkit.thw.xml.GetSupportedLangs;
import org.geotoolkit.thw.xml.GetSupportedLangsResponse;
import org.geotoolkit.thw.xml.GetTopmostConcepts;
import org.geotoolkit.thw.xml.GetTopmostConceptsResponse;
import org.geotoolkit.thw.xml.THSMarshallerPool;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * The Rest thesaurus service.
 *
 * todo For now the thesaurus does not follow the constellation pattern with workers.
 *       It does not support multi instance either
 *
 * @ignore Ignore from rest api documentation
 * @author Guilhem Legal (Geomatys)
 * @author Mehdi Sidhoum (Geomatys)
 */
@Controller
@RequestMapping("thw/{serviceId:.+}")
public class RestThesaurusService extends OGCWebService<THWworker> {


    public RestThesaurusService() {
        super(ServiceDef.THW.specification);
        setXMLContext(THSMarshallerPool.getInstance());
        LOGGER.log(Level.INFO, "Thesaurus REST service running ({0} instances)\n", getWorkerMapSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResponseObject processExceptionResponse(CstlServiceException ex, ServiceDef sdef, Worker w) {
        try {
            if (ex != null) {
                String code = null;
                if (ex.getExceptionCode() != null)  {
                    code = ex.getExceptionCode().name();
                }
                final ExceptionReport report = new ExceptionReport(ex.getMessage(), code, ex.getLocator(), null);
                final StringWriter sw        = new StringWriter();
                Marshaller marshaller        = null;
                try {
                    marshaller = getMarshallerPool().acquireMarshaller();
                    marshaller.marshal(report, sw);
                } finally {
                    if (marshaller != null) {
                        getMarshallerPool().recycle(marshaller);
                    }
                }
                return new ResponseObject(sw.toString(), "text/xml");
            }
        } catch (JAXBException e) {
            LOGGER.log(Level.WARNING, null, e);
        }
        return new ResponseObject("Internal error", "text/plain");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseObject treatIncomingRequest(Object objectRequest, final THWworker worker) {
        String outputFormat                 = "text/xml";
        if (worker != null) {
            try {

                 logParameters();
                 String request = "";
                 if (objectRequest == null) {
                    request = (String) getParameter("REQUEST", true);
                 } else if (objectRequest instanceof JAXBElement){
                     objectRequest = ((JAXBElement)objectRequest).getValue();
                 }

                 if (request.equalsIgnoreCase("GetTopmostConcepts") || (objectRequest instanceof GetTopmostConcepts)) {
                     final GetTopmostConcepts gtc;
                     if (objectRequest != null) {
                        gtc = (GetTopmostConcepts) objectRequest;
                     } else {
                         final String thesaurusURI = getParameter("thesaurus", true);
                         final String language     = getParameter("language",  false);
                         final String output       = getParameter("outputFormat",  false);
                         final String themes       = getParameter("themes",  false);
                         final List<String> themeList = StringUtilities.toStringList(themes);
                         gtc = new GetTopmostConcepts(thesaurusURI, language, output, themeList);
                     }
                     Object response = worker.getTopmostConcepts(gtc);
                     if (gtc.getOutputFormat() != null && gtc.getOutputFormat().equals("RDF")) {
                         response = new RDF(((GetTopmostConceptsResponse)response).getReturn());
                     } else if (gtc.getOutputFormat() != null) {
                         outputFormat = gtc.getOutputFormat();
                     }
                     return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetAllConceptRelatives") || (objectRequest instanceof GetAllConceptRelatives)) {
                     final GetAllConceptRelatives gcr;
                     if (objectRequest != null) {
                        gcr = (GetAllConceptRelatives) objectRequest;
                     } else {
                         final String thesau     = getParameter("thesaurus",    false);
                         final String relation   = getParameter("relation",     false);
                         final String conceptURI = getParameter("concept",      true);
                         final String output     = getParameter("outputFormat", false);
                         gcr = new GetAllConceptRelatives(conceptURI, thesau, relation, output);
                     }
                     if (gcr.getOutputFormat() != null) {
                         outputFormat = gcr.getOutputFormat();
                     }
                    return new ResponseObject(worker.getAllConceptRelatives(gcr), outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetRelatedConcepts") || (objectRequest instanceof GetRelatedConcepts)) {
                    final GetRelatedConcepts grc;
                    if (objectRequest != null) {
                       grc = (GetRelatedConcepts) objectRequest;
                    } else {
                       final String language      = getParameter("language",      false);
                       final String relation      = getParameter("relation",      false);
                       final String conceptURI    = getParameter("concept",       true);
                       final String thesaurusURI  = getParameter("thesaurus_uri", false);
                       final String output        = getParameter("outputFormat", false);
                       grc = new GetRelatedConcepts(conceptURI, relation, language, thesaurusURI, output);
                     }
                    if (grc.getOutputFormat() != null) {
                         outputFormat = grc.getOutputFormat();
                    }
                    return new ResponseObject(worker.getRelatedConcepts(grc), outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetConcept") || (objectRequest instanceof GetConcept)) {
                    final GetConcept gc;
                    if (objectRequest != null) {
                       gc = (GetConcept) objectRequest;
                    } else {
                        final String language     = getParameter("language",      false);
                        final String conceptURI   = getParameter("concept",       true);
                        final String output       = getParameter("outputFormat",  false);
                        final String thesaurusURI = getParameter("thesaurus_uri", false);
                        gc = new GetConcept(conceptURI, language, output, thesaurusURI);
                     }
                    Object response = worker.getConcept(gc);
                    if (gc.getOutputFormat() != null && gc.getOutputFormat().equals("RDF")) {
                        response = new RDF(Arrays.asList(((GetConceptResponse)response).getReturn()));
                    } else if (gc.getOutputFormat() != null) {
                        outputFormat = gc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetNumeredConcept") || (objectRequest instanceof GetNumeredConcept)) {
                    final GetNumeredConcept gc;
                    if (objectRequest != null) {
                       gc = (GetNumeredConcept) objectRequest;
                    } else {
                        final String language     = getParameter("language",      true);
                        final String conceptURI   = getParameter("concept",       true);
                        final String output       = getParameter("outputFormat",  false);
                        final String theme        = getParameter("theme",         false);
                        final String thesaurusURI = getParameter("thesaurus_uri", false);
                        gc = new GetNumeredConcept(conceptURI, language, output, theme, thesaurusURI);
                     }
                    Object response = worker.getNumeredConcept(gc);
                    if (gc.getOutputFormat() != null && gc.getOutputFormat().equals("RDF")) {
                        response = new RDF(Arrays.asList(((GetNumeredConceptResponse)response).getReturn()));
                    } else if (gc.getOutputFormat() != null) {
                        outputFormat = gc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetAggregatedConcept") || (objectRequest instanceof GetAggregatedConcept)) {
                    final GetAggregatedConcept gc;
                    if (objectRequest != null) {
                       gc = (GetAggregatedConcept) objectRequest;
                    } else {
                        final String language     = getParameter("language",      true);
                        final String conceptURI   = getParameter("concept",       true);
                        final String output       = getParameter("outputFormat",  false);
                        final String theme        = getParameter("theme",         false);
                        final String thesaurusURI = getParameter("thesaurus_uri", false);
                        gc = new GetAggregatedConcept(conceptURI, language, output, theme, thesaurusURI);
                     }
                    Object response = worker.getAggregatedConcept(gc);
                    if (gc.getOutputFormat() != null && gc.getOutputFormat().equals("RDF")) {
                        response = new RDF(Arrays.asList(((GetAggregatedConceptResponse)response).getReturn()));
                    } else if (gc.getOutputFormat() != null) {
                        outputFormat = gc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetAggregatedConceptIds") || (objectRequest instanceof GetAggregatedConceptIds)) {
                    final GetAggregatedConceptIds gc;
                    if (objectRequest != null) {
                       gc = (GetAggregatedConceptIds) objectRequest;
                    } else {
                        final String language     = getParameter("language",      true);
                        final String conceptURI   = getParameter("concept",       true);
                        final String output       = getParameter("outputFormat",  false);
                        final String theme        = getParameter("theme",         false);
                        final String thesaurusURI = getParameter("thesaurus_uri", false);
                        gc = new GetAggregatedConceptIds(conceptURI, language, output, theme, thesaurusURI);
                     }
                    Object response = worker.getAggregatedConceptIds(gc);
                    if (gc.getOutputFormat() != null && gc.getOutputFormat().equals("RDF")) {
                        // TODO final RDF rdf = new RDF(Arrays.asList(response.getReturn()));
                    } else if (gc.getOutputFormat() != null) {
                        outputFormat = gc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetAllTranslationsForConcept") || (objectRequest instanceof GetAllTranslationsForConcept)) {
                    final GetAllTranslationsForConcept gatfc;
                    if (objectRequest != null) {
                       gatfc = (GetAllTranslationsForConcept) objectRequest;
                    } else {
                        final String property    = getParameter("property_uri",  true);
                        final String conceptURI  = getParameter("concept_uri",   true);
                        final String output      = getParameter("outputFormat",  false);
                        gatfc = new GetAllTranslationsForConcept(conceptURI, property, output);
                     }
                    final GetAllTranslationsForConceptResponse response = worker.getAllTranslationsForConcept(gatfc);
                    if (gatfc.getOutputFormat() != null) {
                         outputFormat = gatfc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetConceptsMatchingKeyword") || (objectRequest instanceof GetConceptsMatchingKeyword)) {
                    final GetConceptsMatchingKeyword gcmk;
                    if (objectRequest != null) {
                       gcmk = (GetConceptsMatchingKeyword) objectRequest;

                    } else {
                        final String language       = getParameter("language", true);
                        final String word           = getParameter("keyword",  false);
                        final String tu             = getParameter("thesaurus_uri",   false);
                        final List<String> tuList   = StringUtilities.toStringList(tu);
                        final String sm             = getParameter("search_mode", true);
                        int searchMode              = Integer.parseInt(sm);
                        final String responseFormat = getParameter("outputFormat", false);
                        final String geom           = getParameter("geometric", false);
                        boolean geometric           = false;
                        if (geom != null) {
                            geometric = Boolean.parseBoolean(geom);
                        }
                        final String sd             = getParameter("showDeactivated", false);
                        boolean showDeactivated     = false;
                        if (sd != null) {
                            showDeactivated = Boolean.parseBoolean(sd);
                        }
                        final String ignoreList            = getParameter("ignore_thesaurus",   false);
                        final List<String> ignoreThesaurus = StringUtilities.toStringList(ignoreList);
                        final String themes          = getParameter("themes",  false);
                        final List<String> themeList = StringUtilities.toStringList(themes);
                        gcmk = new GetConceptsMatchingKeyword(word, searchMode, tuList, language, responseFormat, geometric, ignoreThesaurus, themeList, showDeactivated);

                    }
                    final Object response       = worker.getConceptsMatchingKeyword(gcmk);
                    if (gcmk.getOutputFormat() != null && !"RDF".equalsIgnoreCase(gcmk.getOutputFormat())) {
                        outputFormat = gcmk.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetNumeredConceptsMatchingKeyword") || (objectRequest instanceof GetNumeredConceptsMatchingKeyword)) {
                    final GetNumeredConceptsMatchingKeyword gcmk;
                    if (objectRequest != null) {
                       gcmk = (GetNumeredConceptsMatchingKeyword) objectRequest;

                    } else {
                        final String language         = getParameter("language", true);
                        final String word             = getParameter("keyword",  false);
                        final String cswList          = getParameter("csw",   false);
                        final String tu               = getParameter("thesaurus_uri",   false);
                        final String responseFormat   = getParameter("outputFormat", false);
                        final String ignoreList       = getParameter("ignore_csw",   false);
                        final String themes           = getParameter("themes",  false);
                        final List<String> themeList  = StringUtilities.toStringList(themes);
                        final List<String> ignoreCsw  = StringUtilities.toStringList(ignoreList);
                        final List<String> includeCsw = StringUtilities.toStringList(cswList);
                        final List<String> tuList     = StringUtilities.toStringList(tu);
                        final String sd               = getParameter("showDeactivated", false);
                        boolean showDeactivated       = false;
                        if (sd != null) {
                            showDeactivated = Boolean.parseBoolean(sd);
                        }
                        final String sm             = getParameter("search_mode", false);
                        final int searchMode;
                        if (sm == null) {
                            searchMode = 4;
                        } else {
                            searchMode              = Integer.parseInt(sm);
                        }
                        gcmk = new GetNumeredConceptsMatchingKeyword(word, includeCsw, language, responseFormat, ignoreCsw, themeList, tuList, showDeactivated, searchMode);
                    }
                    final Object response       = worker.getNumeredConceptsMatchingKeyword(gcmk);
                    if (gcmk.getOutputFormat() != null && !"RDF".equalsIgnoreCase(gcmk.getOutputFormat())) {
                        outputFormat = gcmk.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetAggregatedConceptsMatchingKeyword") || (objectRequest instanceof GetAggregatedConceptsMatchingKeyword)) {
                    final GetAggregatedConceptsMatchingKeyword gcmk;
                    if (objectRequest != null) {
                       gcmk = (GetAggregatedConceptsMatchingKeyword) objectRequest;

                    } else {
                        final String language         = getParameter("language", true);
                        final String word             = getParameter("keyword",  false);
                        final String cswList          = getParameter("csw",   false);
                        final String responseFormat   = getParameter("outputFormat", false);
                        final String ignoreList       = getParameter("ignore_csw",   false);
                        final String themes           = getParameter("themes",  false);
                        final String tu               = getParameter("thesaurus_uri",   false);
                        final String sd               = getParameter("showDeactivated", false);
                        final List<String> themeList  = StringUtilities.toStringList(themes);
                        final List<String> ignoreCsw  = StringUtilities.toStringList(ignoreList);
                        final List<String> includeCsw = StringUtilities.toStringList(cswList);
                        final List<String> tuList     = StringUtilities.toStringList(tu);
                        boolean showDeactivated       = false;
                        if (sd != null) {
                            showDeactivated = Boolean.parseBoolean(sd);
                        }
                        final String sm             = getParameter("search_mode", false);
                        final int searchMode;
                        if (sm == null) {
                            searchMode = 4;
                        } else {
                            searchMode              = Integer.parseInt(sm);
                        }
                        gcmk = new GetAggregatedConceptsMatchingKeyword(word, includeCsw, language, responseFormat, ignoreCsw, themeList, tuList, showDeactivated, searchMode);

                    }
                    final Object response       = worker.getAggregatedConceptsMatchingKeyword(gcmk);
                    if (gcmk.getOutputFormat() != null && !"RDF".equalsIgnoreCase(gcmk.getOutputFormat())) {
                        outputFormat = gcmk.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetLinkedCsw") || (objectRequest instanceof GetLinkedCsw)) {
                    final GetLinkedCsw glc;
                    if (objectRequest != null) {
                       glc = (GetLinkedCsw) objectRequest;
                    } else {
                       final String responseFormat   = getParameter("outputFormat", false);
                       glc = new GetLinkedCsw(responseFormat);
                    }
                    final GetLinkedCswResponse response = worker.getLinkedCsw(glc);
                    if (glc.getOutputFormat() != null) {
                        outputFormat = glc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetConceptsMatchingRegexByThesaurus") || (objectRequest instanceof GetConceptsMatchingRegexByThesaurus)) {
                    throw new CstlServiceException("non implemented method");
                 }

                 if (request.equalsIgnoreCase("GetAvailableLanguages") || (objectRequest instanceof GetAvailableLanguages)) {
                    final GetAvailableLanguages gal;
                    if (objectRequest != null) {
                       gal = (GetAvailableLanguages) objectRequest;
                    } else {
                        final String conceptURI     = getParameter("concept_uri",   true);
                        final String responseFormat = getParameter("outputFormat", false);
                        gal = new GetAvailableLanguages(conceptURI, responseFormat);
                     }
                    final GetAvailableLanguagesResponse response = worker.getAvailableLanguages(gal);
                    if (gal.getOutputFormat() != null) {
                        outputFormat = gal.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetSupportedLangs") || (objectRequest instanceof GetSupportedLangs)) {
                    final GetSupportedLangs gsl;
                    if (objectRequest != null) {
                       gsl = (GetSupportedLangs) objectRequest;
                    } else {
                       final String thesaurusURI  = getParameter("thesaurus",   true);
                       final String responseFormat = getParameter("outputFormat", false);
                       gsl = new GetSupportedLangs(thesaurusURI, responseFormat);
                    }
                    final GetSupportedLangsResponse response = worker.getSupportedLangs(gsl);
                    if (gsl.getOutputFormat() != null) {
                        outputFormat = gsl.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                if (request.equalsIgnoreCase("GetAvailableThesauri") || (objectRequest instanceof GetAvailableThesauri)) {
                    final GetAvailableThesauri gat;
                    final String responseFormat;
                    if (objectRequest != null) {
                        gat = (GetAvailableThesauri) objectRequest;
                        responseFormat = gat.getOutputFormat();
                    } else {
                        final String sd               = getParameter("showDeactivated", false);
                        boolean showDeactivated       = false;
                        if (sd != null) {
                            showDeactivated = Boolean.parseBoolean(sd);
                        }
                        responseFormat = getParameter("outputFormat", false);
                        gat = new GetAvailableThesauri(responseFormat, showDeactivated);
                    }
                    final Object response = worker.getAvailableThesauri(gat);
                    if (responseFormat != null && !"RDF".equalsIgnoreCase(responseFormat)) {
                        outputFormat = responseFormat;
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetAllPreferedLabel") || (objectRequest instanceof GetAllPreferedLabel)) {
                    final GetAllPreferedLabel gapl;
                    if (objectRequest != null) {
                       gapl = (GetAllPreferedLabel) objectRequest;
                    } else {
                       final String sd               = getParameter("showDeactivated", false);
                       boolean showDeactivated       = false;
                       if (sd != null) {
                            showDeactivated = Boolean.parseBoolean(sd);
                       }
                       final String language  = getParameter("language",   true);
                       final String responseFormat = getParameter("outputFormat", false);
                       gapl = new GetAllPreferedLabel(language, showDeactivated, responseFormat);
                    }
                    final GetAllPreferedLabelResponse response = worker.getAllPreferedLabel(gapl);
                    if (gapl.getOutputFormat() != null) {
                        outputFormat = gapl.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("GetGeometricConcept") || (objectRequest instanceof GetGeometricConcept)) {
                    final GetGeometricConcept ggc;
                    if (objectRequest != null) {
                       ggc = (GetGeometricConcept) objectRequest;
                    } else {
                       final String uriConcept     = getParameter("uri_concept",   true);
                       final String responseFormat = getParameter("outputFormat", false);
                       ggc = new GetGeometricConcept(uriConcept, responseFormat);
                    }
                    final String responseFormat  = ggc.getOutputFormat();
                    final Object response = worker.GetGeometricConcept(ggc);
                    if (responseFormat != null && !"RDF".equalsIgnoreCase(responseFormat)) {
                        outputFormat = ggc.getOutputFormat();
                    }
                    return new ResponseObject(response, outputFormat);
                 }

                 if (request.equalsIgnoreCase("fetchGroups") || (objectRequest instanceof FetchGroups)) {
                    throw new CstlServiceException("non implemented method", NO_APPLICABLE_CODE);
                 }

                 if (request.equalsIgnoreCase("FetchThemes") || (objectRequest instanceof FetchThemes)) {
                    throw new CstlServiceException("non implemented method", NO_APPLICABLE_CODE);
                 }

                 if (objectRequest != null) {
                    throw new CstlServiceException("The operation type " + objectRequest.getClass().getName() + " is not supported by the service" , NO_APPLICABLE_CODE);
                 } else {
                     throw new CstlServiceException("The operation " + request + " is not supported by the service" , NO_APPLICABLE_CODE);
                 }
            } catch (CstlServiceException ex) {
                return processExceptionResponse(ex, null, worker);
            }
        } else {
            return new ResponseObject("The Thesaurus service is not working", "text/plain");
        }
    }

}
