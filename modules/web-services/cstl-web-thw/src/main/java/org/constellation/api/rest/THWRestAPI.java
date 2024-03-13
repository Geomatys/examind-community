/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.api.rest;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.constellation.dto.Sort;
import org.geotoolkit.skos.xml.Value;
import org.springframework.web.bind.annotation.RestController;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.Page;
import org.constellation.dto.PagedSearch;
import org.constellation.dto.thesaurus.ConceptBrief;
import org.constellation.dto.thesaurus.ConceptNode;
import org.constellation.dto.thesaurus.FullConcept;
import org.constellation.dto.thesaurus.SearchMode;
import org.constellation.dto.thesaurus.Thesaurus;
import org.constellation.dto.thesaurus.ThesaurusBrief;
import org.constellation.dto.thesaurus.ThesaurusComparator;
import org.constellation.exception.ConstellationException;
import org.constellation.thesaurus.api.IThesaurusBusiness;
import org.constellation.thesaurus.api.ThesaurusException;
import static org.constellation.thesaurus.io.sql.ThesaurusDatabase.CONCEPT_TYPE;
import org.constellation.thesaurus.io.sql.ThesaurusDatabaseWriter;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.skos.xml.RDF;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.WriteableThesaurus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static org.springframework.http.HttpStatus.*;
import org.springframework.http.MediaType;
import static org.springframework.http.MediaType.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RestController
public class THWRestAPI {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.rest.api");


    @Autowired
    private IThesaurusBusiness thesaurusBusiness;

    /**
     * Use to install all ressource available thesaurus.
     * Will be probably remove later.
     *
     * @return
     * @throws Exception
     */
    @RequestMapping(value="/THW/install",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity InstallAvaillableThesaurus() throws Exception {
        final java.nio.file.Path thDir = IOUtilities.getResourceAsPath("org/constellation/thesaurus/xml");
        final MarshallerPool pool = thesaurusBusiness.getSkosMarshallerPool();
        try (DirectoryStream<java.nio.file.Path> dirStream = Files.newDirectoryStream(thDir)) {
            for (java.nio.file.Path path : dirStream) {
                final String name = IOUtilities.filenameWithoutExtension(path);
                if (thesaurusBusiness.getThesaurusURIByName(name).isEmpty()) {
                    try (InputStream stream = Files.newInputStream(path)){
                        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
                        Object object = unmarshaller.unmarshal(stream);
                        pool.recycle(unmarshaller);
                        if (object instanceof JAXBElement) {
                            object = ((JAXBElement) object).getValue();
                        }
                        if (object instanceof RDF) {
                            final RDF rdf = (RDF) object;

                            final Thesaurus model = new Thesaurus();
                            model.setName(name);

                            try (final WriteableThesaurus thesaurus = thesaurusBusiness.createNewThesaurus(model)) {
                                thesaurus.writeRdf(rdf);

                                //update defaultLanguage
                                if (!thesaurus.getLanguage().isEmpty()) {
                                    thesaurus.setDefaultLanguage(thesaurus.getLanguage().get(0));
                                    thesaurus.updateThesaurusProperties();
                                }
                            }
                        } else {
                            throw new Exception("the specified XML file does not contains a RDF object");
                        }
                    } catch (SQLException | JAXBException e) {
                        throw new Exception("Exception while importing new thesaurus", e);
                    }
                }
            }
            return new ResponseEntity(AcknowlegementType.success("Thesaurus datasource created"), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    // -------------------------------------------------------------------------
    //  Instance API
    // -------------------------------------------------------------------------

    @RequestMapping(value="/THW/search", method=POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity search(@RequestBody PagedSearch pagedSearch) {
        try {
            List<Thesaurus> results = new ArrayList<>();

            // Filter thesaurus list.
            String searchTerm = pagedSearch.getText();
            final Map<String, Thesaurus> listThesaurus = thesaurusBusiness.listThesaurus();
            for (Thesaurus instance : listThesaurus.values()) {
                if (isBlank(searchTerm) || containsIgnoreCase(instance.getName(), searchTerm)) {
                    results.add(instance);
                }
            }

            // Sort thesaurus list.
            if (pagedSearch.getSort() != null) {
                Collections.sort(results, new ThesaurusComparator(
                        pagedSearch.getSort().getField(),
                        pagedSearch.getSort().getOrder().equals(Sort.Order.ASC)
                ));
            }

            // Truncate thesaurus list.
            int from = (pagedSearch.getPage() - 1) * pagedSearch.getSize();
            int to = Math.min(from + pagedSearch.getSize(), results.size());
            results = results.subList(from, to);

            // Build and return the page of thesaurus.
            return new ResponseEntity(new Page<ThesaurusBrief>()
                    .setNumber(pagedSearch.getPage())
                    .setSize(pagedSearch.getSize())
                    .setContent(toThesaurusBriefs(results))
                    .setTotal(listThesaurus.size()), OK);

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}",method=GET, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity get(@PathVariable("thesaurusUri") String thesaurusUri) {
        try {
            return new ResponseEntity(getThesaurus(thesaurusUri), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW",method=PUT, consumes = APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity create(@RequestBody Thesaurus thesaurus) throws Exception {
        try {
            thesaurusBusiness.createNewThesaurus(thesaurus);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity list() throws ThesaurusException {
        try {
            return new ResponseEntity(toThesaurusBriefs(thesaurusBusiness.listThesaurus().values()), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity delete(@PathVariable("thesaurusUri") String thesaurusUri) throws Exception {
        try {
            thesaurusBusiness.deleteThesaurus(thesaurusUri);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    // -------------------------------------------------------------------------
    //  Concept API
    // -------------------------------------------------------------------------

    @RequestMapping(value="/THW/{thesaurusUri}/concept",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getTopMostConcepts(@PathVariable("thesaurusUri") String thesaurusUri) {
        try (WriteableThesaurus thesaurus = getThesaurusWriter(thesaurusUri)) {
            List<Concept> concepts = thesaurus.getTopMostConcepts(null, null);
            return new ResponseEntity(toConceptNodes(concepts), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/{lang}/concept/search/{keyword}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity searchConcepts(@PathVariable("thesaurusUri") String thesaurusUri,
                                            @PathVariable("lang") String lang,
                                            @PathVariable("keyword") String keyword,
                                            @RequestParam(name = "mode", required = false, defaultValue = "0") int mode) {
        try (WriteableThesaurus thesaurus = getThesaurusWriter(thesaurusUri)) {
            ISOLanguageCode isoLang = ISOLanguageCode.fromCode(lang);
            List<Concept> concepts = thesaurus.search(keyword, mode, false, null, isoLang);
            return new ResponseEntity(toConceptNodes(concepts), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/concept/{conceptUri}/narrowers",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getConceptNarrowers(@PathVariable("thesaurusUri") String thesaurusUri,
                                              @PathVariable("conceptUri") String conceptUri) {
        try (ThesaurusDatabaseWriter thesaurus = getThesaurusWriter(thesaurusUri)) {
            return new ResponseEntity(thesaurus.getConceptNarrowers(conceptUri), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/concept/{conceptUri}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity getConcept(@PathVariable("thesaurusUri") String thesaurusUri,
                                     @PathVariable("conceptUri") String conceptUri) {
        try (ThesaurusDatabaseWriter thesaurus = getThesaurusWriter(thesaurusUri)) {
            return new ResponseEntity(thesaurus.getFullConcept(conceptUri), OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/concept",method=POST, consumes = APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity createConcept(@PathVariable("thesaurusUri") String thesaurusUri, @RequestBody FullConcept fullConcept) {
        try (ThesaurusDatabaseWriter thesaurus = getThesaurusWriter(thesaurusUri)) {
            fullConcept.setUri(UUID.randomUUID().toString());
            thesaurus.insertConcept(fullConcept);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/concept",method=PUT, consumes = APPLICATION_JSON_VALUE, produces=APPLICATION_JSON_VALUE)
    public ResponseEntity updateConcept(@PathVariable("thesaurusUri") String thesaurusUri, @RequestBody FullConcept fullConcept) {
        try (ThesaurusDatabaseWriter thesaurus = getThesaurusWriter(thesaurusUri)) {
            thesaurus.deleteConcept(fullConcept.getUri());
            thesaurus.insertConcept(fullConcept);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/concept/{conceptUri}",method=DELETE,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity deleteConcept(@PathVariable("thesaurusUri") String thesaurusUri,
                              @PathVariable("conceptUri") String conceptUri) throws SQLException {
        try (WriteableThesaurus thesaurus = getThesaurusWriter(thesaurusUri)) {
            thesaurus.deleteConceptCascade(conceptUri);
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/autocomplete/{language}/{limit}/{term}",method=GET,produces=APPLICATION_JSON_VALUE)
    public ResponseEntity autocomplete(@PathVariable("limit") int limit, @PathVariable("language") String language, @PathVariable("term") String term) {
        try {
            final List<String> totalResults = new ArrayList<>();
            final Map<String, Thesaurus> listThesaurus = thesaurusBusiness.listThesaurus();
            for (Thesaurus instance : listThesaurus.values()) {
                ThesaurusDatabaseWriter thesaurus = getThesaurusWriter(instance.getUri());
                List<String> results = thesaurus.searchLabels(term, SearchMode.SUFFIX_REGEX, null, ISOLanguageCode.fromCode(language));
                totalResults.addAll(results);
                if (totalResults.size() >= limit) {
                    return new ResponseEntity(totalResults.subList(0, limit), OK);
                }
            }
            return new ResponseEntity(totalResults, OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    // -------------------------------------------------------------------------
    //  RDF methods
    // -------------------------------------------------------------------------

    @RequestMapping(value="/THW/{thesaurusUri}/import",method=POST, consumes=MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity importRDF(@PathVariable("thesaurusUri") String thesaurusUri,
                          @RequestParam(name = "rdfFile", required = false) MultipartFile rdfFile) throws Exception {

        try (WriteableThesaurus th = getThesaurusWriter(thesaurusUri)) {

            MarshallerPool pool = thesaurusBusiness.getSkosMarshallerPool();
            Unmarshaller unmarshaller = pool.acquireUnmarshaller();
            Object object = unmarshaller.unmarshal(rdfFile.getInputStream());
            pool.recycle(unmarshaller);
            if (object instanceof JAXBElement) {
                object = ((JAXBElement) object).getValue();
            }
            if (object instanceof RDF) {
                final RDF rdf = (RDF) object;
                th.writeRdf(rdf);

//  TODO → determine if the RDF file contains a top concept
//                if (generateTop) {
//                    th.computeTopMostConcept();
//                }

                //update defaultLanguage
                if (!th.getLanguage().isEmpty()) {
                    th.setDefaultLanguage(th.getLanguage().get(0));
                    th.updateThesaurusProperties();
                }
            } else {
                throw new Exception("the specified XML file does not contains a RDF object");
            }
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/export",method=GET,produces=APPLICATION_XML_VALUE)
    public ResponseEntity getThesaurusRdf(@PathVariable("thesaurusUri") String thesaurusUri, HttpServletResponse response) {
        try (WriteableThesaurus thesaurus = getThesaurusWriter(thesaurusUri)) {
            RDF thesaurusRdf = thesaurus.toRDF();

            MarshallerPool pool = thesaurusBusiness.getSkosMarshallerPool();
            Marshaller m = pool.acquireMarshaller();
            m.marshal(thesaurusRdf, response.getOutputStream());
            pool.recycle(m);

            response.addHeader("Content-Disposition","attachment; filename=" + thesaurus.getName() + ".xml");
            response.setContentType(MediaType.APPLICATION_XML.toString());
            response.flushBuffer();
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    @RequestMapping(value="/THW/{thesaurusUri}/export/{conceptUri}",method=GET,produces=APPLICATION_XML_VALUE)
    public ResponseEntity getPartialThesaurusRdf(@PathVariable("thesaurusUri") String thesaurusUri,
                                           @PathVariable("conceptUri") String conceptUri, HttpServletResponse response) {

        try (WriteableThesaurus thesaurus = getThesaurusWriter(thesaurusUri)) {
            Concept concept = thesaurus.getConcept(conceptUri);
            if (concept == null) {
                throw new ThesaurusException("No concept found in thesaurus (" + thesaurus.getURI() + ") for URI: " + conceptUri);
            }
            RDF thesaurusRdf = thesaurus.toRDF();

            MarshallerPool pool = thesaurusBusiness.getSkosMarshallerPool();
            Marshaller m = pool.acquireMarshaller();
            m.marshal(thesaurusRdf, response.getOutputStream());
            pool.recycle(m);

            response.addHeader("Content-Disposition", "attachment; filename=" + thesaurus.getName() + "_" + conceptUri + ".xml");
            response.setContentType(MediaType.APPLICATION_XML.toString());
            response.flushBuffer();
            return new ResponseEntity(OK);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            return new ErrorMessage(ex).build();
        }
    }

    // -------------------------------------------------------------------------
    //  Private methods
    // -------------------------------------------------------------------------

    private ThesaurusDatabaseWriter getThesaurusWriter(String thesaurusUri) throws ConstellationException {
        try {
            return (ThesaurusDatabaseWriter) thesaurusBusiness.createThesaurusWriter(thesaurusUri);
        } catch (ThesaurusException e) {
            throw new ConstellationException("No thesaurus instance found for URI: " + thesaurusUri, e);
        }
    }
    private Thesaurus getThesaurus(String uri) throws ConstellationException {
        return thesaurusBusiness.listThesaurus().get(uri);
    }


    private static List<ThesaurusBrief> toThesaurusBriefs(Collection<Thesaurus> thesaurus) {
        return Lists.transform(Lists.newArrayList(thesaurus), new Function<Thesaurus, ThesaurusBrief>() {
            @Override
            public ThesaurusBrief apply(Thesaurus instance) {
                return new ThesaurusBrief()
                        .setUri(instance.getUri())
                        .setName(instance.getName())
                        .setDefaultLang(instance.getDefaultLang())
                        .setCreationDate(instance.getCreationDate());
            }
        });
    }

    private static List<ConceptNode> toConceptNodes(List<Concept> concepts) {
        return Lists.transform(concepts, new Function<Concept, ConceptNode>() {
            @Override
            public ConceptNode apply(Concept concept) {
                ConceptNode conceptNode = new ConceptNode(concept.getAbout())
                        .setNarrowerCount(concept.getNarrower().size());
                for (Value prefLabel : concept.getPrefLabel()) {
                    conceptNode.getPrefLabel().put(prefLabel.getLang(), prefLabel.getValue());
                }
                for (Value altLabel : concept.getAltLabel()) {
                    conceptNode.addAltLabel(altLabel.getLang(), altLabel.getValue());
                }
                return conceptNode;
            }
        });
    }

    private static List<Concept> toPartialSkosConcept(List<ConceptBrief> briefs) {
        return Lists.transform(briefs, BRIEF_TO_SKOS);
    }

    private static final Function<ConceptBrief, Concept> BRIEF_TO_SKOS = new Function<ConceptBrief, Concept>() {
        @Override
        public Concept apply(ConceptBrief brief) {
            Concept concept = new Concept();
            concept.setResource(brief.getUri());
            return concept;
        }
    };

    /**
     * TODO handle IsTop concept
     */
    private static final Function<FullConcept, Concept> FULL_TO_SKOS = new Function<FullConcept, Concept>() {

        @Override
        public Concept apply(FullConcept f) {
            final Concept c = new Concept(f.getUri());
            c.setType(new Concept(CONCEPT_TYPE));
            c.setBroader(toPartialSkosConcept(f.getBroaders()));
            c.setNarrower(toPartialSkosConcept(f.getNarrowers()));
            c.setRelated(toPartialSkosConcept(f.getRelated()));

            final List<Value> definitions = new ArrayList<>();
            if (f.getDefinition() != null) {
                for (Map.Entry<String, String> entry : f.getDefinition().entrySet()) {
                    if (isNotBlank(entry.getValue())) {
                        definitions.add(new Value(entry.getKey(), entry.getValue()));
                    }
                }
            }
            c.setDefinition(definitions);

            final List<Value> prefLabels = new ArrayList<>();
            if (f.getPrefLabel()!= null) {
                for (Map.Entry<String, String> entry : f.getPrefLabel().entrySet()) {
                    if (isNotBlank(entry.getValue())) {
                        prefLabels.add(new Value(entry.getKey(), entry.getValue()));
                    }
                }
            }
            c.setPrefLabel(prefLabels);

            return c;
        }
    };
}
