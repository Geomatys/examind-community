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

import com.google.common.collect.Lists;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.constellation.dto.thesaurus.ConceptBrief;
import org.constellation.dto.thesaurus.ConceptNode;
import org.constellation.dto.thesaurus.FullConcept;

import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.skos.xml.RDF;
import org.geotoolkit.skos.xml.Value;

import org.geotoolkit.thw.model.Word;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.ScoredConcept;
import org.geotoolkit.thw.model.Thesaurus;
import static org.constellation.dto.thesaurus.SearchMode.*;
import org.constellation.thesaurus.api.ThesaurusException;
import org.constellation.util.FilterSQLRequest;
import org.constellation.util.SQLResult;
import org.constellation.util.SingleFilterSQLRequest;
import org.constellation.util.Util;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @author Mehdi Sidhoum (Geomatys)
 * @since 2.0.4
 */
public class ThesaurusDatabase implements Thesaurus, AutoCloseable {

    public static final String CONCEPT_TYPE = "http://www.w3.org/2004/02/skos/core#Concept";

    public static final String LABEL_TYPE               = "label";
    public static final String ALT_LABEL_TYPE           = "altLabel";
    public static final String DEFINITION_LABEL_TYPE    = "definition";
    public static final String PREF_LABEL_TYPE          = "prefLabel";
    public static final String SCOPE_NOTE_TYPE          = "scopeNote";
    public static final String HISTORY_NOTE_TYPE        = "historyNote";
    public static final String EXAMPLE_TYPE             = "example";


    public static final String TYPE_PREDICATE              = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String VALUE_PREDICATE             = "http://www.w3.org/1999/02/22-rdf-syntax-ns#value";
    public static final String CHANGE_NOTE_PREDICATE       = "http://www.w3.org/2004/02/skos/core#changeNote";
    public static final String BROADER_PREDICATE           = "http://www.w3.org/2004/02/skos/core#broader";
    public static final String HAS_TOP_CONCEPT_PREDICATE   = "http://www.w3.org/2004/02/skos/core#hasTopConcept";
    public static final String NARROWER_PREDICATE          = "http://www.w3.org/2004/02/skos/core#narrower";
    public static final String RELATED_PREDICATE           = "http://www.w3.org/2004/02/skos/core#related";
    public static final String NARROWER_TRANS_PREDICATE    = "http://www.w3.org/2004/02/skos/core#narrowerTransitive";
    public static final String EXTERNAL_ID_PREDICATE       = "http://www.w3.org/2004/02/skos/core#externalID";
    public static final String HIERARCHY_ROOT_TY_PREDICATE = "http://semantic-web.at/ontologies/csw.owl#hierarchyRootType";
    public static final String HIERARCHY_ROOT_PREDICATE    = "http://semantic-web.at/ontologies/csw.owl#hierarchyRoot";
    public static final String CREATOR_PREDICATE           = "http://purl.org/dc/elements/1.1/creator";
    public static final String DATE_PREDICATE              = "http://purl.org/dc/elements/1.1/date";
    public static final String LANGUAGE_PREDICATE          = "http://purl.org/dc/elements/1.1/language";
    public static final String DESCRIPTION_PREDICATE       = "http://purl.org/dc/elements/1.1/description";
    public static final String CONTRIBUTOR_PREDICATE       = "http://purl.org/dc/elements/1.1/contributor";
    public static final String RIGHTS_PREDICATE            = "http://purl.org/dc/elements/1.1/rights";
    public static final String TITLE_PREDICATE             = "http://purl.org/dc/elements/1.1/title";
    public static final String SUBJECT_PREDICATE           = "http://purl.org/dc/elements/1.1/subject";
    public static final String ISSUED_PREDICATE            = "http://purl.org/dc/terms/issued";
    public static final String MODIFIED_PREDICATE          = "http://purl.org/dc/terms/modified";
    public static final String HAS_VERSION_PREDICATE       = "http://purl.org/dc/terms/hasVersion";
    public static final String NAME_PREDICATE              = "http://xmlns.com/foaf/0.1/name";



    protected static final int COMPLETION   = 0;
    protected static final int LOCALISATION = 1;

    protected String uri;

    protected String name;

    private String version;

    protected String description;

    protected ISOLanguageCode defaultLanguage;

    protected List<ISOLanguageCode> languages;

    protected final String schema;

    protected static final String TABLE_NAME = "propriete_concept";

    protected final String likeOperator;

    protected boolean state;

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.thesaurus.io");

    protected DataSource datasource;

    protected final String dialect;

    public ThesaurusDatabase(final DataSource datasource, final String schema, final String dialect) throws ThesaurusException {
        this.datasource    = datasource;
        this.dialect         = dialect;
        if (Util.containsForbiddenCharacter(schema)) {
            throw new ThesaurusException("Invalid schema prefix value");
        }
        this.schema        = schema;
        if ("derby".equals(dialect)) {
            likeOperator   = "LIKE";
        } else {
            likeOperator   = "ILIKE";
        }
        try {
            this.languages = readLanguage();
            readProperty();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Unable to read thesaurus properties from thesaurus database.", ex);
        }
    }

    /**
     * This constructor is used when the thesaurus is not yet stored in the database.
     *
     * @param datasource The datasource whiwh will store the thesaurus
     * @param schema The database schema for this thesaurus
     * @param dialect A flag indicating the datasource implementation (derby, postgres, hsql).
     * @param uri The unique identifier of the thesaurus
     * @param name The name of the thesaurus.
     * @param description A brief description of the thesaurus
     * @param languages A list of languages contained in the thesaurus (for label, altLabel, ...)
     * @param defaultLanguage The default language to be used when none is specified.
     */
    public ThesaurusDatabase(final DataSource datasource, final String schema, final String dialect,
            final String uri, final String name, final String description, final List<ISOLanguageCode> languages,
            final ISOLanguageCode defaultLanguage) {
        this.datasource = datasource;
        this.dialect    = dialect;
        if ("derby".equals(dialect)) {
            likeOperator   = "LIKE";
        } else {
            likeOperator   = "ILIKE";
        }
        this.schema      = schema;
        this.description = description;
        this.name        = name;
        this.uri         = uri;
        this.languages   = languages;
        this.defaultLanguage = defaultLanguage;

    }

    private List<ISOLanguageCode> readLanguage() throws SQLException {
        final List<ISOLanguageCode> response = new ArrayList<>();
        try (Connection c = datasource.getConnection();
             PreparedStatement stmt = c.prepareStatement("SELECT \"language_iso\"  FROM \"" + schema + "\".\"language\"");//NOSONAR
             ResultSet result = stmt.executeQuery()) {
            while (result.next()) {
                response.add(ISOLanguageCode.fromCode(result.getString(1)));
            }
        }
        return response;
    }

    private void readProperty() throws SQLException {
        try (Connection c = datasource.getConnection();
             PreparedStatement stmt = c.prepareStatement("SELECT \"uri\", \"name\", \"description\", \"enable\", \"defaultLang\"  FROM \"" + schema + "\".\"propriete_thesaurus\"");//NOSONAR
             ResultSet result = stmt.executeQuery()) {

            if (result.next()) {
                uri         = result.getString(1);
                name        = result.getString(2);
                description = result.getString(3);
                state       = result.getInt(4) == 1;
                final String lang = result.getString(5);
                if (lang != null) {
                    defaultLanguage = ISOLanguageCode.fromCode(lang);
                }
            }
        }
    }

    private List<Tuple> getConceptTuples(final String uriConcept, final boolean strict, final Connection c) throws SQLException {
        final List<Tuple> response = new ArrayList<>();
        final String sql;
        final String uriValue;
        if (strict) {
            sql   = "SELECT \"predicat\", \"objet\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"uri_concept\" = ? order by \"graphid\"";
            uriValue = uriConcept;
        } else {
            sql   = "SELECT \"predicat\", \"objet\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"uri_concept\" LIKE ? order by \"graphid\"";
            uriValue = '%' + uriConcept;
        }
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, uriValue);
            try (ResultSet result1 = stmt.executeQuery()) {
                while (result1.next()) {
                    final String predicat = removePrefix(result1.getString(1));
                    final String objet    = removePrefix(result1.getString(2));
                    response.add(new Tuple(predicat, objet));
                }
            }
        }
        return response;
    }

    protected String readConceptProperty(final String uriConcept, final String predicat, final Connection c) throws SQLException {
        final String sql = "SELECT \"objet\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"uri_concept\" = ? and \"predicat\" = ?";
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, uriConcept);
            stmt.setString(2, predicat);
            try (ResultSet result1 = stmt.executeQuery()) {
                if (result1.next()) {
                    return removePrefix(result1.getString(1));
                }
            }
        }
        return null;
    }

    private String getTheme(final String uriConcept, final boolean strict, final Connection c) throws SQLException {
        final String sql;
        final String uriValue;
        if (strict) {
            sql      = "SELECT \"thesaurus_origine\"  FROM \"" + schema + "\".\"terme_completion\" WHERE \"uri_concept\" = ?";
            uriValue = uriConcept;
        } else {
            sql      = "SELECT \"thesaurus_origine\" FROM \"" + schema + "\".\"terme_completion\" WHERE \"uri_concept\" LIKE ?";
            uriValue = '%' + uriConcept;
        }
        String theme = null;

        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, uriValue);
            try (ResultSet result = stmt.executeQuery()) {
                if (result.next()) {
                    theme = result.getString(1);
                }
            }
        }
        if (schema.equals(theme)) {
            return null;
        }
        return theme;
    }

    protected List<Value> getMultiLingualTerm(final String uriConcept, final String termType, final boolean strict, final int tableFlag, final Connection c, final ISOLanguageCode language) throws SQLException {
        final String table;
        if (tableFlag == COMPLETION) {
            table = "terme_completion";
        } else {
            table = "terme_localisation";
        }
        final String sql;
        final String uriValue;
        if (strict) {
            sql      = "SELECT \"label\", \"langage_iso\"  FROM \"" + schema + "\".\"" + table + "\" WHERE \"uri_concept\" = ? AND \"type_terme\"=?";
            uriValue = uriConcept;
        } else {
            sql      = "SELECT \"label\", \"langage_iso\" FROM \"" + schema + "\".\"" + table + "\" WHERE \"uri_concept\" LIKE ? AND \"type_terme\"=?";
            uriValue = '%' + uriConcept;
        }
        final List<Value> response = new ArrayList<>();

        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, uriValue);
            stmt.setString(2, termType);
            try (ResultSet result = stmt.executeQuery()) {
                while (result.next()) {
                    final String selectLanguage = result.getString(2);
                    if (language != null) {
                        if (language.getTwoLetterCode().toLowerCase().equals(selectLanguage)) {
                            response.add(new Value(result.getString(1), selectLanguage));
                        }
                    } else {
                        response.add(new Value(result.getString(1), selectLanguage));
                    }
                }
            }
        }
        return response;
    }

    protected FilterSQLRequest appendLanguageFilter(final ISOLanguageCode language, boolean where, FilterSQLRequest request) {
        if (where) {
            request.append(" WHERE ");
        } else {
            request.append(" AND ");
        }
        if (language != null) {
            request.append("\"langage_iso\"=").appendValue(language.getTwoLetterCode().toLowerCase()).append(" ");
        }
        return request;
    }

    protected FilterSQLRequest appendThemeFilter(final List<String> themes, FilterSQLRequest request) {
        if (themes != null && !themes.isEmpty()) {
            request.append(" AND \"thesaurus_origine\" IN (");
            for (String theme : themes) {
                request.appendValue(theme).append(",");
            }
            request.deleteLastChar(1);
            request.append(")");
        }
        return request;
    }

    /**
     * Try to find the concept matching the specified term.
     *
     * @param brutTerm The term to search.
     * @param language if not {@code null} add a language filter to the search.
     * @return
     */
    @Override
    public List<ScoredConcept> search(final String brutTerm, final ISOLanguageCode language) {
        final List<ScoredConcept> matchingConcept = new ArrayList<>();

        try (Connection c = datasource.getConnection()) {

            final String term     = brutTerm.replace("'", "''");

            // first we search fo full matching => score 1.0
            FilterSQLRequest query = new SingleFilterSQLRequest("SELECT \"uri_concept\" FROM \"" + schema + "\".\"terme_completion\" WHERE ");
            query.append("\"label\"=").appendValue(term);
            appendLanguageFilter(language, false, query);

            boolean stop = false;
            try (final SQLResult result = query.execute(c)) {
                while (result.next()) {
                    stop        = true;
                    String uric = result.getString(1);
                    uric        = removePrefix(uric);
                    matchingConcept.add(new ScoredConcept(uric, this, 1.0, language));
                }
            }

            if (stop) {
                return matchingConcept;
            }

            // second we search fo full matching case insensitive => score 0.9
            query = new SingleFilterSQLRequest("SELECT \"uri_concept\" FROM \"" + schema + "\".\"terme_completion\" WHERE ");
            query.append("upper(\"label\") = ").appendValue(term.toUpperCase());
            appendLanguageFilter(language, false, query);

            try (final SQLResult result = query.execute(c)) {
                while (result.next()) {
                    stop        = true;
                    String uric = result.getString(1);
                    uric        = removePrefix(uric);
                    matchingConcept.add(new ScoredConcept(uric, this, 0.9, language));
                }
            }

            if (stop) {
                return matchingConcept;
            }

            // then we search for mispelled matching => score 0.8
            String tmp = term;
            for (char[] spe : language.getSpecialCharacter()) {
                for (char s : spe) {
                    if (tmp.indexOf(s) != -1) {
                        tmp = tmp.replace(s, '_');
                    }
                }
            }
            query = new SingleFilterSQLRequest("SELECT \"uri_concept\" FROM \"" + schema + "\".\"terme_completion\" WHERE ");
            query.append("\"label\" " + likeOperator + " ").append(tmp);
            appendLanguageFilter(language, false, query);
            LOGGER.log(Level.FINER, "Mispelled Query:{0}", query);

            try (final SQLResult result = query.execute(c)) {
                while (result.next()) {
                    stop        = true;
                    String uric = result.getString(1);
                    uric        = removePrefix(uric);
                    matchingConcept.add(new ScoredConcept(uric, this ,0.8, language));
                }
            }

            if (stop) {
                return matchingConcept;
            }

            // then we search fo partial matching => score 0.7
            query = new SingleFilterSQLRequest("SELECT \"uri_concept\" FROM \"" + schema + "\".\"terme_completion\" WHERE ");
            query.append("\"label\" " + likeOperator + " ").appendValue('%' + term + '%');
            appendLanguageFilter(language, false, query);

            try (final SQLResult result = query.execute(c)) {
                while (result.next()) {
                    stop        = true;
                    String uric = result.getString(1);
                    uric        = removePrefix(uric);
                    matchingConcept.add(new ScoredConcept(uric, this, 0.7, language));
                }
            }

            if (stop) {
                return matchingConcept;
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "exception in search:" + ex.getMessage(), ex);
        }

        return matchingConcept;
    }

    /**
     * Try to find the concept matching the specified term.
     *
     * @param term The term to search.
     * @param searchMode The mode used to search.
     * @param geometric Special flag for geometric thesaurus.
     * @param themes If not {@code null} add a theme filter to the search.
     * @param language if not {@code null} add a language filter to the search.
     *
     * @return
     */
    @Override
    public List<Concept> search(final String term, final int searchMode, final boolean geometric, final List<String> themes, final ISOLanguageCode language) {
        final List<Concept> matchingConcept = new ArrayList<>();

        final StringBuilder sb = new StringBuilder();
        if (geometric) {
            sb.append(" SELECT t.\"uri_concept\" FROM \"").append(schema).append("\".\"terme_completion\" t, \"").append(schema).append("\".\"concept\" c");
            sb.append(" WHERE c.\"uri_concept\" = t.\"uri_concept\"");
            sb.append(" AND c.\"layer_associe\" != 'NULL' ");
        } else {
            sb.append("SELECT \"uri_concept\" FROM \"").append(schema).append("\".\"terme_completion\" ");
        }
        final String queryPrefix = sb.toString();

        try (Connection c   = datasource.getConnection()) {

            if (searchMode == NO_WILD_CHAR || searchMode == AUTO_SEARCH) {

                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                if (geometric) {
                    query.append(" AND ");
                } else {
                    query.append(" WHERE ");
                }
                query.append("\"label\"=").appendValue(term).append(" ");
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);

                boolean stop = false;

                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop = true;
                        String uric = result.getString(1);
                        uric = removePrefix(uric);
                        final Concept concept = readConcept(uric, false, c, null);
                        if (!matchingConcept.contains(concept)) {
                            matchingConcept.add(concept);
                        }
                    }
                }
                if (stop || searchMode == NO_WILD_CHAR) {
                    return matchingConcept;
                }
            }

            if (searchMode == PREFIX_REGEX || searchMode == AUTO_SEARCH) {
                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                if (geometric) {
                    query.append(" AND ");
                } else {
                    query.append(" WHERE ");
                }
                query.append("\"label\" ").append(likeOperator).appendValue("%" + term).append(" ");
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);

                boolean stop = false;
                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop        = true;
                        String uric = result.getString(1);
                        uric        = removePrefix(uric);
                        final Concept concept = readConcept(uric, false, c, null);
                        if (!matchingConcept.contains(concept)) {
                            matchingConcept.add(concept);
                        }
                    }
                }
                if (stop || searchMode == PREFIX_REGEX) {
                    return matchingConcept;
                }
            }

            if (searchMode == SUFFIX_REGEX || searchMode == AUTO_SEARCH) {

                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                if (geometric) {
                    query.append(" AND ");
                } else {
                    query.append(" WHERE ");
                }
                query.append("\"label\" ").append(likeOperator).appendValue(term + "%").append(" ");
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);

                boolean stop = false;
                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop = true;
                        String uric = result.getString(1);
                        uric = removePrefix(uric);
                        final Concept concept = readConcept(uric, false, c, null);
                        if (!matchingConcept.contains(concept)) {
                            matchingConcept.add(concept);
                        }
                    }
                }
                if (stop || searchMode == SUFFIX_REGEX) {
                    return matchingConcept;
                }
            }

            if (searchMode == PREFIX_SUFFIX_REGEX || searchMode == AUTO_SEARCH) {

                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                if (geometric) {
                    query.append(" AND ");
                } else {
                    query.append(" WHERE ");
                }
                query.append("\"label\" ").append(likeOperator).appendValue("%" + term + "%").append(" ");
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);

                boolean stop = false;
                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop        = true;
                        String uric = result.getString(1);
                        uric        = removePrefix(uric);
                        final Concept concept = readConcept(uric, false, c, null);
                        if (!matchingConcept.contains(concept)) {
                            matchingConcept.add(concept);
                        }
                    }
                }
                if (stop || searchMode == PREFIX_SUFFIX_REGEX) {
                    return matchingConcept;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "exception in search:" + ex.getMessage() + '\n' + " for query: " + sb.toString(), ex);
        }
        return matchingConcept;
    }

    /**
     * Try to find the concept matching the specified term.
     *
     * @param brutTerm The term to search.
     * @param searchMode The mode used to search.
     * @param themes If not {@code null} add a theme filter to the search.
     * @param language if not {@code null} add a language filter to the search.
     *
     * @return
     */
    @Override
    public List<String> searchLabels(final String brutTerm, final int searchMode, final List<String> themes, final ISOLanguageCode language) {
        final List<String> results = new ArrayList<>();

        final String queryPrefix = "SELECT \"label\" FROM \"" + schema + "\".\"terme_completion\" ";

        try (Connection c      = datasource.getConnection();
             Statement stmt    = c.createStatement()) {

            if (searchMode == NO_WILD_CHAR || searchMode == AUTO_SEARCH) {

                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                query.append(" WHERE \"label\"=").appendValue(brutTerm);
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);

                boolean stop = false;

                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop = true;
                        results.add(result.getString(1));
                    }
                }
                if (stop || searchMode == NO_WILD_CHAR) {
                    return results;
                }
            }

            if (searchMode == PREFIX_REGEX || searchMode == AUTO_SEARCH) {
                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                query.append(" WHERE \"label\" " + likeOperator).appendValue("%" + brutTerm);
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);
                boolean stop = false;

                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop        = true;
                        results.add(result.getString(1));
                    }
                }
                if (stop || searchMode == PREFIX_REGEX) {
                    return results;
                }
            }

            if (searchMode == SUFFIX_REGEX || searchMode == AUTO_SEARCH) {

                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                query.append(" WHERE \"label\" " + likeOperator).appendValue(brutTerm + "%");
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);
                boolean stop = false;

                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop = true;
                        results.add(result.getString(1));
                    }
                }
                if (stop || searchMode == SUFFIX_REGEX) {
                    return results;
                }
            }

            if (searchMode == PREFIX_SUFFIX_REGEX || searchMode == AUTO_SEARCH) {

                final FilterSQLRequest query = new SingleFilterSQLRequest(queryPrefix);
                query.append(" WHERE \"label\" " + likeOperator).appendValue("%" + brutTerm + "%");
                appendLanguageFilter(language, false, query);
                appendThemeFilter(themes, query);
                boolean stop = false;

                try (final SQLResult result = query.execute(c)) {
                    while (result.next()) {
                        stop        = true;
                        results.add(result.getString(1));
                    }
                }
                if (stop || searchMode == PREFIX_SUFFIX_REGEX) {
                    return results;
                }
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "exception in search:" + ex.getMessage(), ex);
        }
        return results;
    }

    /**
     * Close the connection to the database and clear the cache.
     */
    @Override
    public void close() {
    }

    @Override
    public Concept getGeometricConcept(final String uriConcept) {
        Concept c = null;
        try (Connection con = datasource.getConnection()) {
            c = readConcept(uriConcept, true, con, null);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return c;
    }
    
    @Override
    public Concept getConcept(final String uriConcept) {
        return getConcept(uriConcept, null);
    }

    @Override
    public Concept getConcept(final String uriConcept, final ISOLanguageCode language) {
        Concept c = null;
        try (Connection con = datasource.getConnection()) {
            c = readConcept(uriConcept, false, con, language);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "SQL exception in getConcept(): {0}", ex.getMessage());
        }
        return c;
    }

    protected Concept buildEmptyConcept(String uriConcept) {
        return new Concept(removePrefix(uriConcept));
    }

    protected Concept readConcept(final String uriConcept, final boolean withGeometry, final Connection con, final ISOLanguageCode language) throws SQLException {
        if (uriConcept == null) return null;

        // hack to avoid the confusion with the LIKE operator when uriconcept is an integer
        boolean strict = true;
        try {
            Integer.parseInt(uriConcept);
        } catch (NumberFormatException ex) {
            strict = false;
        }

        final Concept concept = buildEmptyConcept(uriConcept);

        concept.setPrefLabel(getMultiLingualTerm(uriConcept,   PREF_LABEL_TYPE,       strict, COMPLETION,   con, language));
        concept.setAltLabel(getMultiLingualTerm(uriConcept,    ALT_LABEL_TYPE,        strict, COMPLETION,   con, language));
        concept.setLabel(getMultiLingualTerm(uriConcept,       LABEL_TYPE,            strict, COMPLETION,   con, language));
        concept.setDefinition(getMultiLingualTerm(uriConcept,  DEFINITION_LABEL_TYPE, strict, LOCALISATION, con, language));
        concept.setScopeNote(getMultiLingualTerm(uriConcept,   SCOPE_NOTE_TYPE,       strict, LOCALISATION, con, language));
        concept.setHistoryNote(getMultiLingualTerm(uriConcept, HISTORY_NOTE_TYPE,     strict, LOCALISATION, con, language));
        concept.setExample(getMultiLingualTerm(uriConcept,     EXAMPLE_TYPE,          strict, LOCALISATION, con, language));

        // extended attribute for cnes distrib
        final String theme = getTheme(uriConcept, strict, con);
        if (theme != null && !theme.equals(schema)){
            final Concept tconcept = new Concept();
            tconcept.setResource(theme);
            concept.addInScheme(tconcept);
        }

        final List<Tuple> tuples = getConceptTuples(uriConcept, strict, con);

        if (tuples.isEmpty()) return null;

        fillConceptPropertiesFromTuple(concept, tuples);

        /*
         * Set concept type (always concept)
         */
        if (concept.getType() == null) {
            final Concept c = new Concept();
            c.setResource(CONCEPT_TYPE);
            concept.setType(c);
        }
        return concept;
    }

    protected void fillConceptPropertiesFromTuple(Concept concept, final List<Tuple> tuples) {
        for (Tuple tuple : tuples) {
            final String predicat = tuple.predicat;
            final String objet    = tuple.object;
            if (CREATOR_PREDICATE.equals(predicat)) {
                concept.setCreator(objet);
            } else if (DATE_PREDICATE.equals(predicat)) {
                concept.setDate(objet);
            } else if (EXTERNAL_ID_PREDICATE.equals(predicat)) {
                concept.setExternalID(objet);
            } else if (DESCRIPTION_PREDICATE.equals(predicat)) {
                concept.setDescription(objet);
            } else if (LANGUAGE_PREDICATE.equals(predicat)) {
                if (concept.getLanguage() == null || !concept.getLanguage().contains(objet)) {
                    concept.addLanguage(objet);
                }
            } else if (RIGHTS_PREDICATE.equals(predicat)) {
                concept.setRights(objet);
            } else if (TITLE_PREDICATE.equals(predicat)) {
                concept.setTitle(objet);
            } else if (SUBJECT_PREDICATE.equals(predicat)) {
                concept.setSubject(objet);
            } else if (CONTRIBUTOR_PREDICATE.equals(predicat)) {
                concept.setContributor(objet);
            } else if (HAS_VERSION_PREDICATE.equals(predicat)) {
                concept.setHasVersion(objet);
            } else if (ISSUED_PREDICATE.equals(predicat)) {
                concept.setIssued(objet);
            } else if (MODIFIED_PREDICATE.equals(predicat)) {
                concept.setModified(objet);
            } else if (TYPE_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.setType(c);
            } else if (VALUE_PREDICATE.equals(predicat)) {
                concept.setValue(objet);
            } else if (BROADER_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.addBroader(c);
            } else if (CHANGE_NOTE_PREDICATE.equals(predicat)) {
                concept.setChangeNote(objet);
            } else if (NARROWER_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.addNarrower(c);
            } else if (NARROWER_TRANS_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.addNarrowerTransitive(c);
            } else if (RELATED_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.addRelated(c);
            } else if (NAME_PREDICATE.equals(predicat)) {
                concept.setName(objet);
            } else if (HIERARCHY_ROOT_PREDICATE.equals(predicat)) {
                final boolean value = Boolean.parseBoolean(objet);
                concept.setHierarchyRoot(value);
            } else if (HIERARCHY_ROOT_TY_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.setHierarchyRootType(c);
            } else if (HAS_TOP_CONCEPT_PREDICATE.equals(predicat)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.addHasTopConcept(c);
            }
        }
    }

    protected List<String> readMultipleConceptProperty(final String uriConcept, final String predicat, final Connection c) throws SQLException {
        final String sql = "SELECT \"objet\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"uri_concept\" = ? and \"predicat\" = ? order by \"graphid\"";
        List<String> result;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, uriConcept);
            stmt.setString(2, predicat);
            try (ResultSet result1 = stmt.executeQuery()) {
                result = new ArrayList<>();
                while (result1.next()) {
                    result.add(removePrefix(result1.getString(1)));
                }
            }
        }
        return result;
    }

    protected List<Concept> readMultipleConceptPropertyBrief(final String uriConcept, final String predicat, final Connection c) throws SQLException {
        final String sql = "SELECT \"objet\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"uri_concept\" = ? and \"predicat\" = ? order by \"graphid\"";
        List<Concept> result;
        try (PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setString(1, uriConcept);
            stmt.setString(2, predicat);
            try (ResultSet result1 = stmt.executeQuery()) {
                result = new ArrayList<>();
                while (result1.next()) {
                    final Concept ct = new Concept();
                    ct.setResource(removePrefix(result1.getString(1)));
                    result.add(ct);
                }
            }
        }
        return result;
    }

    protected Concept readPartialConcept(final String uriConcept, final Connection con, List<String> customProperties) throws SQLException {
        if (uriConcept == null) return null;

        // will return a concept only with uri/prefLabel
        if (customProperties == null) {
            customProperties = Collections.EMPTY_LIST;
        }

        // hack to avoid the confusion with the LIKE operator when uriconcept is an integer
        boolean strict = true;
        try {
            Integer.valueOf(uriConcept);
        } catch (NumberFormatException ex) {
            strict = false;
        }

       List<Value> prefLabels = getMultiLingualTerm(uriConcept,   PREF_LABEL_TYPE,  strict, COMPLETION, con, null);

       // empty prefLabels mean that the concept does not exist
       if (prefLabels.isEmpty()) return null;

       final Concept concept = buildEmptyConcept(removePrefix(uriConcept));
       concept.setPrefLabel(prefLabels);

        for (String customProperty : customProperties) {
            // multiple properties case
            if (BROADER_PREDICATE.equals(customProperty)) {
                List<Concept> objet = readMultipleConceptPropertyBrief(uriConcept, customProperty, con);
                concept.setBroader(objet);
                continue;
            } else if (HAS_TOP_CONCEPT_PREDICATE.equals(customProperty)) {
                List<Concept> objet = readMultipleConceptPropertyBrief(uriConcept, customProperty, con);
                concept.setHasTopConcept(objet);
                continue;
            } else if (NARROWER_PREDICATE.equals(customProperty)) {
                List<Concept> objet = readMultipleConceptPropertyBrief(uriConcept, customProperty, con);
                concept.setNarrower(objet);
                continue;
            } else if (RELATED_PREDICATE.equals(customProperty)) {
                List<Concept> objet = readMultipleConceptPropertyBrief(uriConcept, customProperty, con);
                concept.setRelated(objet);
                continue;
            } else if (NARROWER_TRANS_PREDICATE.equals(customProperty)) {
                List<Concept> objet = readMultipleConceptPropertyBrief(uriConcept, customProperty, con);
                concept.setNarrowerTransitive(objet);
                continue;
            } else if (LANGUAGE_PREDICATE.equals(customProperty)) {
                List<String> objet = readMultipleConceptProperty(uriConcept, customProperty, con);
                concept.setLanguage(objet);
                continue;
            }

            String objet = readConceptProperty(uriConcept, customProperty, con);
            if (TYPE_PREDICATE.equals(customProperty)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.setType(c);
            } else if (VALUE_PREDICATE.equals(customProperty)) {
                concept.setValue(objet);
            } else if (CHANGE_NOTE_PREDICATE.equals(customProperty)) {
                concept.setChangeNote(objet);
            } else if (EXTERNAL_ID_PREDICATE.equals(customProperty)) {
                concept.setExternalID(objet);
            } else if (HIERARCHY_ROOT_TY_PREDICATE.equals(customProperty)) {
                final Concept c = new Concept();
                c.setResource(objet);
                concept.setHierarchyRootType(c);
            } else if (HIERARCHY_ROOT_PREDICATE.equals(customProperty)) {
                final boolean value = Boolean.parseBoolean(objet);
                concept.setHierarchyRoot(value);
            } else if (CREATOR_PREDICATE.equals(customProperty)) {
                concept.setCreator(objet);
            } else if (DATE_PREDICATE.equals(customProperty)) {
                concept.setDate(objet);
            } else if (DESCRIPTION_PREDICATE.equals(customProperty)) {
                concept.setDescription(objet);
            } else if (CONTRIBUTOR_PREDICATE.equals(customProperty)) {
                concept.setContributor(objet);
            } else if (RIGHTS_PREDICATE.equals(customProperty)) {
                concept.setRights(objet);
            } else if (TITLE_PREDICATE.equals(customProperty)) {
                concept.setTitle(objet);
            } else if (SUBJECT_PREDICATE.equals(customProperty)) {
                concept.setSubject(objet);
            } else if (ISSUED_PREDICATE.equals(customProperty)) {
                concept.setIssued(objet);
            } else if (MODIFIED_PREDICATE.equals(customProperty)) {
                concept.setModified(objet);
            } else if (HAS_VERSION_PREDICATE.equals(customProperty)) {
                concept.setHasVersion(objet);
            } else if (NAME_PREDICATE.equals(customProperty)) {
                concept.setName(objet);
            }
        }
        return concept;
    }

    @Override
    public List<Concept> getHierarchyRoots(final List<String> themes) {
        final List<Concept> result = new ArrayList<>();
        final String query;
        if (themes == null || themes.isEmpty()) {
            query = "SELECT \"uri_concept\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" "
                  + "WHERE \"predicat\"='http://semantic-web.at/ontologies/csw.owl#hierarchyRoot' "
                  + "AND \"objet\"='true'";
        } else {
            String themeValue = "";
            for (String theme : themes) {
                themeValue = "'" + theme + "',";
            }
            themeValue = themeValue.substring(0, themeValue.length() - 1);

            query = "SELECT \"uri_concept\" "
                  + "FROM \"" + schema + "\".\"" + TABLE_NAME + "\" p,\"" + schema + "\".\"terme_completion\" t "
                  + "WHERE \"predicat\"='http://semantic-web.at/ontologies/csw.owl#hierarchyRoot' "
                  + "AND \"objet\"='true' "
                  + "AND p.\"uri_concept\"=t.\"uri_concept\" "
                  + "AND t.\"thesaurus_origine\" IN (" + themeValue + ")";
        }
        try (Connection c = datasource.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {//NOSONAR

            while (rs.next()) {
                final String conceptURI = rs.getString(1);
                result.add(readConcept(conceptURI, false, c, null));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while retrieving the top concepts. \nQuery:" + query, ex);
        }
        return result;
    }

    @Override
    public List<Concept> getTopMostConcepts(final List<String> themes, final ISOLanguageCode language) {
        final List<Concept> result = new ArrayList<>();
        final FilterSQLRequest query;
        if (themes == null || themes.isEmpty()) {
            query = new SingleFilterSQLRequest("SELECT \"objet\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"predicat\"='http://www.w3.org/2004/02/skos/core#hasTopConcept'");
        } else {
            query = new SingleFilterSQLRequest("SELECT \"objet\" "
                  + "FROM \"" + schema + "\".\"" + TABLE_NAME + "\" p,\"" + schema + "\".\"terme_completion\" t "
                  + "WHERE \"predicat\"='http://www.w3.org/2004/02/skos/core#hasTopConcept' "
                  + "AND p.\"objet\"=t.\"uri_concept\" "
                  + "AND t.\"thesaurus_origine\" IN (");

            for (String theme : themes) {
                query.appendValue(theme).append(",");
            }
            query.deleteLastChar(1);
            query.append(")");
        }

        try (Connection c = datasource.getConnection();
             SQLResult rs = query.execute(c)) {

            while (rs.next()) {
                final String conceptURI = rs.getString(1);
                result.add(readConcept(conceptURI, false, c, language));
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while retrieving the top concepts. \nQuery:" + query, ex);
        }
        return result;
    }

    @Override
    public List<Concept> getAllConcepts(final int limit) {
        final List<Concept> result = new ArrayList<>();
        final String query = "SELECT DISTINCT \"uri_concept\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\"";
        try (Connection c = datasource.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {//NOSONAR

            while (rs.next()) {
                final String conceptURI = rs.getString(1);
                result.add(readConcept(conceptURI, false, c, null));
                if (limit != -1 && result.size() >= limit) {
                    return result;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception while retrieving all the concepts. \nQuery:" + query, ex);
        }
        return result;
    }

    protected static String removePrefix(final String s) {
        final int i = s.indexOf("::");
        if (i != -1) {
            return s.substring(i + 2);
        }
        return s;
    }

    /**
     * Return the language of this Thesaurus.
     * @return
     */
    @Override
    public List<ISOLanguageCode> getLanguage() {
        return languages;
    }

    public void setLanguage(final List<ISOLanguageCode> languages) {
        this.languages = languages;
    }

    @Override
    public List<String> getAllLabels(final int limit, final ISOLanguageCode language) {
        final List<String> response = new ArrayList<>();
        final FilterSQLRequest query = new SingleFilterSQLRequest("SELECT \"label\" FROM \"" + schema + "\".\"terme_completion\" ");
        appendLanguageFilter(language, true, query);

        try (Connection c = datasource.getConnection();
             SQLResult result = query.execute(c)) {

            while (result.next()) {
                if (limit > 0 && response.size() < limit) {
                    response.add(result.getString(1));
                } else if (limit < 0) {
                    response.add(result.getString(1));
                } else if (response.size() > limit) {
                    break;
                }
            }
        } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "SQL exception in getAllLabel:" + ex.getMessage(), ex);
        }
        return response;
    }

    @Override
    public List<String> getAllLabels(final ISOLanguageCode language) {
        return getAllLabels(-1, language);
    }

    @Override
    public List<String> getAllPreferedLabels(final int limit, final ISOLanguageCode language) {
        final List<String> response = new ArrayList<>();
        final FilterSQLRequest query = new SingleFilterSQLRequest("SELECT \"label\" FROM \"" + schema + "\".\"terme_completion\" WHERE \"type_terme\"='prefLabel'");
        appendLanguageFilter(language, false, query);

        try (Connection c = datasource.getConnection();
             SQLResult result = query.execute(c)) {

            while (result.next()) {
                if (limit > 0 && response.size() < limit) {
                    response.add(result.getString(1));
                } else if (limit < 0) {
                    response.add(result.getString(1));
                } else if (response.size() > limit) {
                    break;
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "SQL exception in getAllPreferedLabel:" + ex.getMessage(), ex);
        }
        return response;
    }

    /**
     * Returns a List that contains all words from the thesaurus.
     * @param buffer The buffer to be fill with word.
     * @param language if not {@code null} add a language filter to the search.
     * @return
     */
    @Override
    public List<Word> getWords(final List<Word> buffer, final ISOLanguageCode language) {
        final List<Word> result = (buffer != null) ? buffer : new ArrayList<>();
        final FilterSQLRequest query = new SingleFilterSQLRequest("SELECT \"label\", \"thesaurus_origine\", \"uri_concept\" FROM \"" + schema + "\".\"terme_completion\" ");
        if (language != null) {
            appendLanguageFilter(language, true, query);
        }
        try (Connection c = datasource.getConnection();
            SQLResult res = query.execute(c)) {

            while (res.next()) {
                final String label = res.getString(1).replace("\"", "\\\"");
                final String uriConcept = res.getString(3).replace("Uv::", "");
                result.add(new Word(label, res.getString(2), uriConcept));
            }

        } catch (SQLException evt) {
            LOGGER.log(Level.SEVERE, "error SQL get words", evt);
        }
        return result;
    }

    @Override
    public List<String> getAllPreferedLabels(final ISOLanguageCode language) {
        return getAllPreferedLabels(-1, language);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the uri
     */
    @Override
    public String getURI() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(final String uri) {
        this.uri = uri;
    }

    /**
     * @return the version
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getConceptTheme(final String uriConcept) {
        String theme = null;
        final String query = "SELECT \"thesaurus_origine\" "
                    + "FROM \"" + schema + "\".\"terme_completion\""
                    + "WHERE \"uri_concept\"=?";

        try (Connection c = datasource.getConnection();
             PreparedStatement stmt = c.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, uriConcept);
            try (ResultSet res = stmt.executeQuery()) {
                if (res.next()) {
                    theme = res.getString(1);
                } else {
                    theme = null;
                }
            }
        } catch (SQLException evt) {
            LOGGER.log(Level.SEVERE, "error SQL get concept theme", evt);
        }
        return theme;
    }

    /**
     * @return the state
     */
    @Override
    public boolean getState() {
        return state;
    }

    @Override
    public RDF toRDF() {
        return toRDF(null);
    }

    @Override
    public RDF toRDF(final Concept root) {
        final List<Concept> concepts;
        if (root == null) {
            concepts = getAllConcepts(-1);
        } else {
            final List<Concept> list = new ArrayList<>();
            list.add(root);
            concepts = getChildrenConcept(root, list);
        }
        return new RDF(null, concepts);
    }

    private List<Concept> getChildrenConcept(final Concept root, final List<Concept> result) {
        final List<Concept> chidren = root.getNarrower();
        if (chidren != null) {
            for (Concept child : chidren) {
                final Concept fullChild = getConcept(child.getResource());
                if (!result.contains(fullChild)) {
                    result.add(fullChild);
                    getChildrenConcept(fullChild, result);
                } else {
                    LOGGER.log(Level.WARNING, "cycle detected in thesaurus: {0}", name);
                }
            }
        }
        return result;
    }

    public String getConceptPrefLabel(final String uriConcept, final String language) {
        try (Connection con = datasource.getConnection();
             PreparedStatement stmt = con.prepareStatement("SELECT \"label\" FROM \"" + schema + "\".\"terme_completion\" WHERE \"uri_concept\"=? AND \"type_terme\" = 'prefLabel'");) {//NOSONAR
            stmt.setString(1, uriConcept);
            String label = null;
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    label = rs.getString(1);
                }
            }
            return label;
        } catch (SQLException ex) {
            throw new RuntimeException("SQL exception in getConceptPrefLabel(...)", ex);
        }
    }

    /**
     * Queries and returns the "narrowers" (children) of a concept.
     *
     * @param conceptUri the parent concept URI.
     * @return the children {@link ConceptNode} list.
     */
    public List<ConceptNode> getConceptNarrowers(String conceptUri) {

        String countNarrowers = "SELECT count(*) FROM \"" + schema + "\".\"" + TABLE_NAME + "\"" +
                " WHERE uri_concept = pc.objet AND predicat = '" + NARROWER_PREDICATE + "'";

        String selectNarrowers = "SELECT pc.objet, tc.label, tc.type_terme, tc.langage_iso, (" + countNarrowers + ")" +
                " FROM \"" + schema + "\".\"" + TABLE_NAME + "\" AS pc" +
                " LEFT JOIN \"" + schema + "\".\"terme_completion\" AS tc ON tc.uri_concept = pc.objet" +
                " WHERE pc.uri_concept = ? AND pc.predicat = '" + NARROWER_PREDICATE + "'" +
                " GROUP BY pc.objet, tc.label, tc.type_terme, tc.langage_iso";

        try (Connection con = datasource.getConnection();
             PreparedStatement stmt = con.prepareStatement(selectNarrowers)) {//NOSONAR

            stmt.setString(1, conceptUri);
            try (ResultSet rs = stmt.executeQuery()) {
                Map<String, ConceptNode> conceptMap = new HashMap<>();
                while (rs.next()) {
                    String objectUri = rs.getString(1);
                    int narrowerCount = rs.getInt(5);

                    // Merge results by unique URI.
                    ConceptNode conceptNode = conceptMap.get(objectUri);
                    if (conceptNode == null) {
                        conceptNode = new ConceptNode(objectUri);
                        conceptMap.put(objectUri, conceptNode);
                    }

                    // Read concept completion terms and narrower count.
                    readCompletionTerm(rs, conceptNode);
                    conceptNode.setNarrowerCount(narrowerCount);
                }
                return Lists.newArrayList(conceptMap.values());
            }
        } catch (SQLException ex) {
            throw new RuntimeException("SQL exception in getConceptNarrowers()", ex);
        }
    }

    protected FullConcept buildEmptyFullConcept(String uriConcept) {
        return new FullConcept(removePrefix(uriConcept));
    }

     /**
     * Queries and returns the details of a concept.
     *
     * @param conceptUri the concept URI.
     * @return the {@link FullConcept} instance.
     */
    public FullConcept getFullConcept(String conceptUri) throws SQLException {

        String selectTerms = "SELECT tc.\"label\", tc.\"type_terme\", tc.\"langage_iso\"" +
                " FROM \"" + schema + "\".\"terme_completion\" AS tc" +
                " WHERE tc.\"uri_concept\" = ?" +
                " UNION SELECT tl.\"label\", tl.\"type_terme\", tl.\"langage_iso\"" +
                " FROM \"" + schema + "\".\"terme_localisation\" AS tl" +
                " WHERE tl.\"uri_concept\" = ?";

        String selectHasTop = "SELECT count(*)" +
                " FROM \"" + schema + "\".\"" + TABLE_NAME + "\" AS pc" +
                " WHERE pc.\"objet\" = ? AND pc.\"predicat\" = '" + HAS_TOP_CONCEPT_PREDICATE + "'";

        String selectRelations = "SELECT pc.\"objet\", pc.\"predicat\", tc.\"label\", tc.\"type_terme\", tc.\"langage_iso\"" +
                " FROM \"" + schema + "\".\"" + TABLE_NAME + "\" AS pc" +
                " LEFT JOIN \"" + schema + "\".\"terme_completion\" AS tc ON tc.\"uri_concept\" = pc.\"objet\"" +
                " WHERE pc.\"uri_concept\" = ?";

        try (Connection con = datasource.getConnection();
             PreparedStatement termsStmt = con.prepareStatement(selectTerms);//NOSONAR
             PreparedStatement hasTopStmt = con.prepareStatement(selectHasTop);//NOSONAR
             PreparedStatement relationsStmt = con.prepareStatement(selectRelations)) {//NOSONAR

            boolean found = false;
            FullConcept fullConcept = buildEmptyFullConcept(conceptUri);

            // Read the concept terms.
            termsStmt.setString(1, conceptUri);
            termsStmt.setString(2, conceptUri);
            try (ResultSet rs = termsStmt.executeQuery()) {
                while (rs.next()) {
                    readCompletionTerm(rs, fullConcept);
                    readLocalisationTerm(rs, fullConcept);
                }
            }

            // Determine if the concept is a "top concept".
            hasTopStmt.setString(1, conceptUri);
            try (ResultSet rs = hasTopStmt.executeQuery()) {
                rs.next();
                fullConcept.setTopConcept(rs.getInt(1) > 0);
            }

            // Read the concept broaders, narrowers, related...
            relationsStmt.setString(1, conceptUri);
            try (ResultSet rs = relationsStmt.executeQuery()) {
                Map<String, ConceptBrief> conceptMap = new HashMap<>();
                while (rs.next()) {
                    found = true;
                    String objectUri = rs.getString(1);
                    String predicate = rs.getString(2);

                    // Merge results by unique URI.
                    ConceptBrief conceptBrief = conceptMap.get(objectUri);
                    if (conceptBrief == null) {
                        conceptBrief = new ConceptBrief(objectUri);
                        conceptMap.put(objectUri, conceptBrief);
                    }

                    // Read concept completion terms.
                    readCompletionTerm(rs, conceptBrief);

                    // Proceed concept predicate.
                    if (BROADER_PREDICATE.equals(predicate) && !fullConcept.getBroaders().contains(conceptBrief)) {
                        fullConcept.getBroaders().add(conceptBrief);
                    } else if (NARROWER_PREDICATE.equals(predicate) && !fullConcept.getNarrowers().contains(conceptBrief)) {
                        fullConcept.getNarrowers().add(conceptBrief);
                    } else if (RELATED_PREDICATE.equals(predicate) && !fullConcept.getRelated().contains(conceptBrief)) {
                        fullConcept.getRelated().add(conceptBrief);
                    }
                    // TODO → handle other predicates
                }
            }

            return found ? fullConcept : null;
        }
    }

    /**
     * @param state the state to set
     */
    @Override
    public void setState(boolean state) {
        this.state = state;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public ISOLanguageCode getDefaultLanguage() {
        return defaultLanguage;
    }

    @Override
    public void setDefaultLanguage(final ISOLanguageCode lang) {
        this.defaultLanguage = lang;
    }

    protected static class Tuple {
        public String predicat;
        public String object;

        public Tuple(final String predicat, final String object) {
            this.object   = object;
            this.predicat = predicat;
        }

    }

    // -------------------------------------------------------------------------
    //  Private utility methods
    // -------------------------------------------------------------------------

    private static void readCompletionTerm(ResultSet source, ConceptBrief destination) throws SQLException {
        final String termValue = source.getString("label");
        final String termType  = source.getString("type_terme");
        final String termLang  = source.getString("langage_iso");

        if (ALT_LABEL_TYPE.equals(termType)) {
            destination.addAltLabel(termLang, termValue);
        } else if (PREF_LABEL_TYPE.equals(termType)) {
            destination.getPrefLabel().put(termLang, termValue);
        }
        // TODO → handle other types
    }

    private static void readLocalisationTerm(ResultSet source, FullConcept destination) throws SQLException {
        final String termValue = source.getString("label");
        final String termType = source.getString("type_terme");
        final String termLang = source.getString("langage_iso");

        if (DEFINITION_LABEL_TYPE.equals(termType)) {
            destination.getDefinition().put(termLang, termValue);
        }
        // TODO → handle all label types
    }

}