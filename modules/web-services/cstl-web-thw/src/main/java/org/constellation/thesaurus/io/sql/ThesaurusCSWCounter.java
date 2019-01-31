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

import java.net.MalformedURLException;
import java.net.URL;
import org.constellation.thesaurus.api.IThesaurusHandler;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.SpringHelper;
import org.constellation.thesaurus.api.IThesaurusCSWCounter;
import org.constellation.thesaurus.util.HTTPCommunicator;
import org.constellation.ws.CstlServiceException;
import org.geotoolkit.csw.xml.CSWMarshallerPool;
import org.geotoolkit.csw.xml.ResultType;
import static org.geotoolkit.csw.xml.TypeNames.IDENTIFIER_QNAME;
import static org.geotoolkit.csw.xml.TypeNames.RECORD_202_QNAME;
import org.geotoolkit.csw.xml.v202.GetRecordsResponseType;
import org.geotoolkit.csw.xml.v202.GetRecordsType;
import org.geotoolkit.csw.xml.v202.QueryConstraintType;
import org.geotoolkit.csw.xml.v202.QueryType;
import org.geotoolkit.csw.xml.v202.RecordType;
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.Thesaurus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ThesaurusCSWCounter implements IThesaurusCSWCounter {

    protected static final Logger LOGGER = Logging.getLogger(ThesaurusCSWCounter.class.getName());

    @Autowired
    @Qualifier("dataSource")
    private DataSource datasource;

    protected final IThesaurusHandler handler;

    protected final String likeOperator = "ILIKE";

    private static final String CQL_REQUEST = "( dc:title LIKE '\"$term\"'  OR dct:abstract LIKE '\"$term\"'  OR dc:subject LIKE '\"$term\"' )";

    public ThesaurusCSWCounter(final IThesaurusHandler handler) {
        this.handler = handler;
        SpringHelper.injectDependencies(this);
    }

    protected void clearThesaurusCount(final int cswID, final Connection c) {
        try (PreparedStatement clearTCStatement  = c.prepareStatement("DELETE FROM \"th_base\".\"term_count\" WHERE \"service\"=?");
             PreparedStatement clearTIdStatement = c.prepareStatement("DELETE FROM \"th_base\".\"aggregated_identifier\" WHERE \"service\"=?")){
            clearTCStatement.setInt(1, cswID);
            clearTCStatement.executeUpdate();

            clearTIdStatement.setInt(1, cswID);
            clearTIdStatement.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while clearing the term count for:" + cswID, ex);
        }
    }

    @Override
    public void refreshThesaurusCSWCount() {
        final long start = System.currentTimeMillis();
        try (Connection con = datasource.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT * FROM \"th_base\".\"linked_service\"");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                final int id     = rs.getInt(1);
                final String url = rs.getString(2);
                createThesaurusCswCount(url, id, con);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL error while refreshing the CSW mapping", ex);
        }
        LOGGER.log(Level.INFO, "thesaurus CSW mapping executed in :{0} ms", (System.currentTimeMillis() - start));
    }

    protected String getCswID(final String url, final Connection c) throws SQLException {
        String result = null;
        try (PreparedStatement stmt = c.prepareStatement("SELECT\"id\" WHERE url=?")) {
            stmt.setString(1, url);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result = rs.getString(1);
                }
            }
        }
        return result;
    }


    protected void createThesaurusCswCount(final String CSWURL, final int cswID, final Connection con) {
        try {
            clearThesaurusCount(cswID, con);
            final URL cswURL                         = new URL(CSWURL);
            final List<Thesaurus> availableThesaurus = handler.getLoadedThesaurus();

            for (Thesaurus thesaurus : availableThesaurus) {
                if(thesaurus.getState()){
                    for (ISOLanguageCode languageCode : thesaurus.getLanguage()) {
                        final String language = languageCode.getTwoLetterCode();

                        final List<Concept> topConcepts = handler.getTopMostConcept(languageCode, Arrays.asList(thesaurus.getURI()), null);
                        if (topConcepts != null && !topConcepts.isEmpty()) {
                            LOGGER.log(Level.INFO, "Transitive search enabled for:{0} language={1}", new Object[]{thesaurus.getURI(), language});
                            for (Concept topConcept : topConcepts) {
                                if (topConcept == null) {
                                    LOGGER.warning("A top concept is null");
                                    continue;
                                }
                                searchTransitive(cswID, languageCode, cswURL, topConcept, null, thesaurus.getURI(), con);
                            }
                        } else {
                            LOGGER.log(Level.INFO, "Flat search enabled for:{0} language={1}", new Object[]{thesaurus.getURI(), language});

                            List<Concept> allConcept = handler.getAllConcepts(-1, languageCode, Arrays.asList(thesaurus.getURI()));
                            LOGGER.log(Level.INFO, "{0}terms to request", allConcept.size());
                            for (Concept c : allConcept) {
                                final List<String> identifiers = getCountForConcept(cswURL, c.getPrefLabel(language));
                                final int count = identifiers.size();
                                if (count > 0) {
                                    storeTermCount(cswID, c.getAbout(), c.getPrefLabel(language), count, identifiers, language, null, thesaurus.getURI(), con);
                                }
                            }

                        }
                    }
                }
            }
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.WARNING, "Marlformed CSW URL:" + CSWURL, ex);
        }
    }

    protected void updateTermCount(final int serviceID, final String uriConcept, final String label, final List<String> aggregatedIdentifiers, final String language, final Connection c) {
        updateTermCount(serviceID, uriConcept, label, aggregatedIdentifiers, language, "term_count", "aggregated_identifier", c);
    }

    protected void updateTermCount(final int serviceID, final String uriConcept, final String label, final List<String> aggregatedIdentifiers, final String language,
            final String termTableName, final String aggregatedTableName, final Connection c) {
        try (PreparedStatement updateTCStmt = c.prepareStatement("UPDATE \"th_base\".\"" + termTableName + "\" "
                                                               + "SET \"aggregated_count\"=? "
                                                               + "WHERE \"label\"=? "
                                                               + "AND \"service\"=? "
                                                               + "AND \"language\"=? "
                                                               + "AND \"uri_concept\"=?");
            final PreparedStatement updateTIdStmt = c.prepareStatement("UPDATE \"th_base\".\"" + aggregatedTableName + "\" "
                                                                     + "SET \"identifier\"=? "
                                                                     + "WHERE \"label\"=? "
                                                                     + "AND \"service\"=? "
                                                                     + "AND \"uri_concept\"=? ")) {
            updateTCStmt.setInt(1, aggregatedIdentifiers.size());
            updateTCStmt.setString(2, label);
            updateTCStmt.setInt(3, serviceID);
            updateTCStmt.setString(4, language);
            updateTCStmt.setString(5, uriConcept);
            updateTCStmt.executeUpdate();


            final StringBuilder sb = new StringBuilder();
            for (String id : aggregatedIdentifiers) {
                sb.append(id).append(';');
            }
            updateTIdStmt.setString(1, sb.toString());
            updateTIdStmt.setString(2, label);
            updateTIdStmt.setInt(3, serviceID);
            updateTIdStmt.setString(4, uriConcept);
            updateTIdStmt.executeUpdate();

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL Exception while storing term count", ex);
        }
    }

    protected boolean storeTermCount(final int serviceID, final String uriConcept, final String label, final int count,
            final List<String> aggregatedIdentifiers, final String language, final String theme, final String uriThesaurus, final Connection c) {
          return storeTermCount(serviceID, uriConcept, label, count, aggregatedIdentifiers, language, theme, "term_count", "aggregated_identifier", uriThesaurus, c);
    }

    private boolean storeTermCount(final int serviceID, final String uriConcept, final String label, final int count, final List<String> aggregatedIdentifiers,
            final String language, final String theme, final String termTableName, final String aggregatedTableName, final String uriThesaurus, final Connection c) {
        try {
            LOGGER.log(Level.FINER, "storing count for:{0} = {1} Aggregated = {2}", new Object[]{label, count, aggregatedIdentifiers.size()});

            try (PreparedStatement insertTermCountStatement = c.prepareStatement("INSERT INTO \"th_base\".\"" + termTableName + "\" VALUES (?,?,?,?,?,?,?,?)")) {
                insertTermCountStatement.setString(1, label);
                insertTermCountStatement.setInt(2, serviceID);
                insertTermCountStatement.setString(3, language);
                insertTermCountStatement.setInt(4, count);
                insertTermCountStatement.setInt(5, aggregatedIdentifiers.size());
                insertTermCountStatement.setString(6, theme);
                insertTermCountStatement.setString(7, uriConcept);
                insertTermCountStatement.setString(8, uriThesaurus);
                insertTermCountStatement.executeUpdate();
            }

            final StringBuilder sb = new StringBuilder();
            for (String id : aggregatedIdentifiers) {
                sb.append(id).append(';');
            }

            try (PreparedStatement insertTermIdStatement = c.prepareStatement("INSERT INTO \"th_base\".\"" + aggregatedTableName + "\" VALUES (?,?,?,?,?,?)")) {
                insertTermIdStatement.setString(1, label);
                insertTermIdStatement.setInt(2, serviceID);
                insertTermIdStatement.setString(3, sb.toString());
                insertTermIdStatement.setString(4, uriConcept);
                insertTermIdStatement.setString(5, language);
                insertTermIdStatement.setString(6, uriThesaurus);
                insertTermIdStatement.executeUpdate();
            }
            return true;

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL Exception while storing term count: term=" + label + " language=" + language + " maybe the term has two parent.(" +  ex.getMessage() + ")");
            return false;
        }
    }

    @Override
    public void storeLinkedCsw(final List<String> linkedCsws) {
        try (Connection con = datasource.getConnection()) {

            try (PreparedStatement clearLinkedStmt = con.prepareStatement("DELETE FROM \"th_base\".\"linked_service\"")){
                clearLinkedStmt.executeUpdate();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "SQL Exception while clearing linked CSW", ex);
            }

            try (PreparedStatement insertLinkedStmt = con.prepareStatement("INSERT INTO \"th_base\".\"linked_service\" VALUES (?,?)")) {
                int i = 0;
                for (String csw : linkedCsws) {
                    try {
                        insertLinkedStmt.setInt(1, i);
                        insertLinkedStmt.setString(2, csw);
                        insertLinkedStmt.executeUpdate();
                    } catch (SQLException ex) {
                        LOGGER.log(Level.WARNING, "SQL Exception while storing linked CSW:" + csw, ex);
                    }
                    i++;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL Exception while storing linked CSW.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<String> getTermsMatchingKeywords(final String keyword, final List<String> csw, final List<String> ignoreCsw,
            final String language, final boolean aggregated, final List<String> themes, final List<String> thesaurusList, final int SearchMode) {
        final List<String> result = new ArrayList<>();
        final Map<String, ShortConcept> termOccurenceMerge = new HashMap<>();
        final String countColumn;
        if (aggregated) {
            countColumn = "aggregated_count";
        } else {
            countColumn = "count";
        }
        final StringBuilder request = new StringBuilder("SELECT \"uri_concept\", \"label\", \"");
        request.append(countColumn);
        request.append("\" FROM \"th_base\".\"term_count\" tc, \"th_base\".\"linked_service\" ls");
        /*
         * 1) Language selection
         */
        request.append(" WHERE \"language\"='").append(language.toUpperCase(Locale.US)).append("'");

        /*
         * 2) If the specified CSW are null we search for all.
         */
        if (csw != null && !csw.isEmpty()) {
            request.append(" AND (");
            for (String cswUrl : csw) {
                request.append("\"url\"='").append(cswUrl).append("' OR ");
            }
            request.delete(request.length() - 4, request.length());
            request.append(')');
        }

        /*
         * 3) we exclude the unwanted csw.
         */
        if (ignoreCsw != null && !ignoreCsw.isEmpty()) {
            request.append(" AND (");
            for (String cswUrl : ignoreCsw) {
                request.append("\"url\"!='").append(cswUrl).append("' AND ");
            }
            request.delete(request.length() - 5, request.length());
            request.append(')');
        }

        /*
         * 4) we filter on the theme column
         */
        if (themes != null && !themes.isEmpty()) {
            request.append(" AND (");
            for (String theme : themes) {
                request.append("\"theme\"='").append(theme).append("' OR ");
            }
            request.delete(request.length() - 4, request.length());
            request.append(')');
        }

        /*
         * 5) we filter on the keyword.
         * @TODO use searchModel
         */
        request.append(" AND \"label\" ").append(likeOperator).append("'").append(keyword).append("%'");

        /*
         * 6) we exclude the zero occurence count for non aggregated.
         */
        if (!aggregated) {
            request.append(" AND \"count\" > 0 ");
        }

        /*
         * 7) we filter on the thesaurus
         */
        if (thesaurusList.isEmpty()) {
            LOGGER.info("no thesaurus activated returning 0 result");
            return result;
        }

        request.append(" AND (");
        for (String thesaurus : thesaurusList) {
            request.append("\"uri_thesaurus\"='").append(thesaurus).append("' OR ");
        }
        request.delete(request.length() - 4, request.length());
        request.append(')');


        /*
         * 8) we execute the request
         */
        final String requestValue = request.toString();
        try (Connection c = datasource.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(requestValue)){

            while (rs.next()) {
                final String uri_concept = rs.getString(1);
                final String term = rs.getString(2);
                final int count   = rs.getInt(3);
                final ShortConcept oldSC = termOccurenceMerge.get(uri_concept);
                if (oldSC != null) {
                    termOccurenceMerge.put(uri_concept, new ShortConcept(uri_concept, term, oldSC.count + count));
                } else {
                    termOccurenceMerge.put(uri_concept, new ShortConcept(uri_concept, term, count));
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while executing:" + requestValue, ex);
        }

        /*
         * 6) we transform the map in string list.
         */
        for (ShortConcept entry : termOccurenceMerge.values()) {
            result.add(entry.term + " (" + entry.count + ')');
        }
        return result;
    }

    @Override
    public Integer getNumeredCountForTerm(final String uriConcept, final String term, final String language, final List<String> csw, final String theme) {
        Integer result = null;
        try (Connection c = datasource.getConnection()) {
            final StringBuilder sb = new StringBuilder("SELECT \"count\" FROM \"th_base\".\"term_count\" WHERE \"label\"=? AND \"language\"=? AND \"uri_concept\"=? ");
            if (csw != null && !csw.isEmpty()) {
                sb.append(" AND (");
                for (String url : csw) {
                    final String cswId = getCswID(url, c);
                    sb.append("\"service\"='").append(cswId).append("' OR ");
                }
                sb.delete(sb.length() - 4, sb.length());
                sb.append(" )");
            }
            try (PreparedStatement stmt = c.prepareStatement(sb.toString())) {
                stmt.setString(1, term);
                stmt.setString(2, language.toUpperCase());
                stmt.setString(3, uriConcept);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (result == null) {
                            result = 0;
                        }
                        result += rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL excption while looking for numered count" , ex);
        }
        return result;
    }

    @Override
    public Integer getAggregatedCountForTerm(final String uriConcept, final String term, final String language, final List<String> csw, final String theme) {
        Integer result = null;
        try (Connection c = datasource.getConnection()) {
            final StringBuilder sb = new StringBuilder("SELECT \"aggregated_count\" FROM \"th_base\".\"term_count\" WHERE \"label\"=? AND \"language\"=? AND \"uri_concept\"=? ");
            if (csw != null && !csw.isEmpty()) {
                sb.append(" AND (");
                for (String url : csw) {
                    final String cswId = getCswID(url, c);
                    sb.append("\"service\"='").append(cswId).append("' OR ");
                }
                sb.delete(sb.length() - 4, sb.length());
                sb.append(" )");
            }
            try (PreparedStatement stmt = c.prepareStatement(sb.toString())) {
                stmt.setString(1, term);
                stmt.setString(2, language.toUpperCase());
                stmt.setString(3, uriConcept);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        if (result == null) {
                            result = 0;
                        }
                        result += rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while looking for aggregated count" , ex);
        }
        return result;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<String> getLinkedCsw() {
        final List<String> results = new ArrayList<>();
        try (Connection c = datasource.getConnection();
             PreparedStatement getLinkedCswStmt = c.prepareStatement("SELECT \"url\" FROM \"th_base\".\"linked_service\"");
             ResultSet rs = getLinkedCswStmt.executeQuery()) {

            while (rs.next()) {
                results.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL error while getting the linked CSW", ex);
        }
        return results;
    }


    @Override
    public void close() {
        // do nothing
    }

    @Override
    public List<String> getAggregatedIdsForTerm(final String uriConcept, final String prefLabel, final String language, final List<String> csw, final String theme) {
        final List<String> results =  new ArrayList<>();
        try (Connection c = datasource.getConnection()) {
            final StringBuilder sb = new StringBuilder("SELECT \"identifier\" FROM \"th_base\".\"aggregated_identifier\" WHERE \"label\"=? AND \"uri_concept\"=? AND \"language\"=?");
            if (csw != null && !csw.isEmpty()) {
                sb.append(" AND (");
                for (String url : csw) {
                    final String cswId = getCswID(url, c);
                    sb.append("\"service\"='").append(cswId).append("' OR ");
                }
                sb.delete(sb.length() - 4, sb.length());
                sb.append(" )");
            }
            try (PreparedStatement stmt = c.prepareStatement(sb.toString())) {
                stmt.setString(1, prefLabel);
                stmt.setString(2, uriConcept);
                stmt.setString(3, language);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        final String idList = rs.getString(1);
                        results.addAll(Arrays.asList(idList.split(";")));
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while looking for aggregated count" , ex);
        }
        return results;
    }

    protected void searchTransitive(final int cswId, final ISOLanguageCode lang, final URL cswURL,
            final Concept concept, final DefaultMutableTreeNode parent, final String thesaurusUri, final Connection c) {
        if (concept == null) {return;}
        final String language          = lang.getTwoLetterCode();
        final String conceptURI        = concept.getAbout();
        final String currentTerm       = concept.getPrefLabel(language);
        final List<String> identifiers = getCountForConcept(cswURL, currentTerm);
        final int count = identifiers.size();
        if (count > 0) {
            DefaultMutableTreeNode currentNode = parent;
            while (currentNode != null) {
                final ShortConcept sc = (ShortConcept) currentNode.getUserObject();
                final List<String> oldIdentifiers = sc.identifiers;
                final List<String> newIdentifiers = mergeList(oldIdentifiers, identifiers);
                sc.identifiers = newIdentifiers;
                if (!newIdentifiers.isEmpty()) {
                    if (oldIdentifiers.isEmpty()) {
                        final String theme = handler.getConceptTheme(sc.uri, thesaurusUri);
                        storeTermCount(cswId, sc.uri, sc.term, 0, newIdentifiers, language, theme, thesaurusUri, c);
                    } else {
                        LOGGER.log(Level.FINER, "{0}: updating count for:{1} Aggregated = {2}", new Object[]{currentTerm, sc.term, newIdentifiers.size()});
                        updateTermCount(cswId, sc.uri, sc.term, newIdentifiers, language, c);
                    }
                }
                currentNode = (DefaultMutableTreeNode) currentNode.getParent();
            }
            final String theme = handler.getConceptTheme(conceptURI, thesaurusUri);
            storeTermCount(cswId, conceptURI, currentTerm, count, identifiers, language, theme, thesaurusUri, c);
        }

        final DefaultMutableTreeNode currentNode = new DefaultMutableTreeNode(new ShortConcept(conceptURI, concept.getPrefLabel(language), identifiers));
        if (parent != null) {
            parent.add(currentNode);
        }
        final List<Concept> relateds = handler.getRelatedConcept(conceptURI, "http://www.w3.org/2004/02/skos/core#narrower", lang, thesaurusUri);
        for (Concept related : relateds) {
            // debug
            if (related == null) {
                LOGGER.log(Level.WARNING, "a null concept was obtained for the narrower of :{0}", conceptURI);
            }
            searchTransitive(cswId, lang, cswURL, related, currentNode, thesaurusUri, c);
        }
    }



    private static List<String> mergeList(final List<String> list1, final List<String> list2) {
        final List<String> result = new ArrayList<>();
        result.addAll(list1);
        for (String s : list2) {
            if (!result.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }
    /**
     * Send a GetRecords request to the specified csw service and the specified term.
     * Return then the count of results for this term.
     *
     * @param cswUrl The CSW service URL.
     * @param term The term searched.
     *
     * @return The number of result matching for the term in the CSW service.
     */
    private List<String> getCountForConcept(final URL cswUrl, final String term) {
        final List<String> results = new ArrayList<>();
        if (term == null) {
            return results;
        }
        /*
         * 1) Build the GetRecord request.
         */
        final String cleanTerm = term.replaceAll("'", "''");
        final String cql = CQL_REQUEST.replace("$term", cleanTerm);
        final QueryConstraintType constraint = new QueryConstraintType(cql, "1.1.0");
        final QueryType query = new QueryType(Arrays.asList(RECORD_202_QNAME), Arrays.asList(IDENTIFIER_QNAME), null, constraint);
        final GetRecordsType cswRequest = new GetRecordsType("CSW", "2.0.2", ResultType.RESULTS, null, "text/xml", "http://www.opengis.net/cat/csw/2.0.2", 1, 5000, query, null);

        /*
         * 2) send the request.
         */
        try {
            final Object response = HTTPCommunicator.sendRequest(cswUrl, cswRequest, CSWMarshallerPool.getInstance(), false);
            if (response instanceof GetRecordsResponseType) {
                final GetRecordsResponseType gr = (GetRecordsResponseType) response;
                // 3) we extract the identifiers of matching result from the response.
                final List<Object> records = gr.getSearchResults().getAny();
                for (Object abstractRecord : records) {
                    if (abstractRecord instanceof RecordType) {
                        final RecordType record = (RecordType) abstractRecord;
                        if (record.getIdentifier() != null && !record.getIdentifier().getContent().isEmpty()) {
                            results.add(record.getIdentifier().getContent().get(0));
                        } else {
                            LOGGER.warning("No identifier in record");
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Not a DC record:{0}", abstractRecord);
                    }
                }
                return results;
            } else {
                LOGGER.log(Level.WARNING, "Unexpected CSW response type:{0}", response.getClass().getName());
            }
        } catch (CstlServiceException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return results;
    }

    protected static class ShortConcept {
        public final String uri;
        public final String term;
        public List<String> identifiers;
        public Integer count;

        public ShortConcept(final String uri, final String term, final List<String> identifiers) {
            this.term = term;
            this.uri  = uri;
            this.identifiers = identifiers;
        }

        public ShortConcept(final String uri, final String term, final Integer count) {
            this.term  = term;
            this.uri   = uri;
            this.count = count;
        }
    }

}
