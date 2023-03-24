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

import org.constellation.dto.thesaurus.ConceptBrief;
import org.constellation.dto.thesaurus.FullConcept;
import org.geotoolkit.internal.sql.ScriptRunner;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.skos.xml.Concept;
import org.geotoolkit.skos.xml.RDF;
import org.geotoolkit.skos.xml.Value;
import org.geotoolkit.thw.model.ISOLanguageCode;
import org.geotoolkit.thw.model.WriteableThesaurus;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.constellation.thesaurus.api.ThesaurusException;
import org.constellation.util.Util;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ThesaurusDatabaseWriter extends ThesaurusDatabase implements WriteableThesaurus {

    public ThesaurusDatabaseWriter(final DataSource datasource, final String schema, final String dialect) throws ThesaurusException {
        super(datasource, schema, dialect);
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
    public ThesaurusDatabaseWriter(final DataSource datasource, final String schema, final String dialect,
            final String uri, final String name, final String description, final List<ISOLanguageCode> languages,
            final ISOLanguageCode defaultLanguage) {
        super(datasource, schema, dialect, uri, name, description, languages, defaultLanguage);
    }

    private void writeProperty(final String uriConcept, final String property, final List<? extends Object> conceptList, final Connection connection) throws SQLException {
        if (conceptList != null) {
            for (Object o : conceptList) {
                if (o instanceof Concept c) {
                    if (c.getAbout() != null) {
                        writeProperty(uriConcept, property, c.getAbout(), connection);
                    } else if (c.getResource() != null) {
                        writeProperty(uriConcept, property, c.getResource(), connection);
                    } else {
                        LOGGER.log(Level.WARNING, "About and resource property cannot be null.");
                    }
                } else if (o instanceof String s) {
                    writeProperty(uriConcept, property, s, connection);
                } else if (o instanceof Boolean) {
                    writeProperty(uriConcept, property, o.toString(), connection);
                } else if (o instanceof ConceptBrief fc) {
                    if (fc.getUri() != null) {
                        writeProperty(uriConcept, property, fc.getUri(), connection);
                    } else {
                        LOGGER.log(Level.WARNING, "Full concept uri property cannot be null.");
                    }
                } else if (o != null) {
                    throw new IllegalArgumentException("Unexpected type for a property:" + o.getClass().getName());
                }
            }
        }
    }

    protected void writeProperty(final String uriconcept, final String property, final String value, final Connection connection) throws SQLException {
        if (value == null) {return;}
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"" + TABLE_NAME + "\" VALUES (?, ?, ?, NULL)")) {//NOSONAR
            writeProperty(uriconcept, property, value, stmt);
            stmt.executeUpdate();
        }
    }

    protected void writeProperty(final String uriconcept, final String property, final String value, final PreparedStatement stmt) throws SQLException {
        if (value == null) {return;}
        stmt.setString(1, uriconcept);
        stmt.setString(2, property);
        stmt.setString(3, value);
    }

    protected void updateProperty(final String uriconcept, final String property, final Object value, final Connection connection) throws SQLException {
        final boolean update;
        final String query = "SELECT \"uri_concept\" FROM  \"" + schema + "\".\"" + TABLE_NAME + "\" "
                           + "WHERE \"uri_concept\"=? AND \"predicat\"=?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {//NOSONAR
            stmt.setString(1, uriconcept);
            stmt.setString(2, property);
            try (ResultSet result = stmt.executeQuery()) {
                update = result.next();
            }
        }

        final String stringValue;
        if (value instanceof String) {
            stringValue = (String) value;
        } else if (value instanceof Boolean) {
            stringValue = value.toString();
        } else if (value instanceof Concept c) {
            stringValue = c.getResource();
        } else if (value instanceof ConceptBrief c) {
            stringValue = c.getUri();
        } else if (value instanceof List ls) {
            deleteProperty(uriconcept, property, connection);
            writeProperty(uriconcept, property, ls, connection);
            return;
        } else if (value != null) {
            throw new IllegalArgumentException("Unexpected type for a property value :" + value.getClass().getName());
        } else {
            stringValue = null;
        }

        if (update) {
            if (stringValue != null) {
                final String upQuery = "UPDATE \"" + schema + "\".\"" + TABLE_NAME + "\" "
                                     + "SET \"objet\"=? "
                                     + "WHERE  \"uri_concept\"=? AND \"predicat\"=?";
                try (PreparedStatement upStmt = connection.prepareStatement(upQuery)) {//NOSONAR
                    upStmt.setString(1, stringValue);
                    upStmt.setString(2, uriconcept);
                    upStmt.setString(3, property);
                    upStmt.executeUpdate();
                }
            } else {
                deleteProperty(uriconcept, property, connection);
            }
        } else {
            writeProperty(uriconcept, property, stringValue, connection);
        }
    }

    private void deleteReference(final String uriconcept, final Connection connection) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE  \"objet\"=?")) {//NOSONAR
            deleteStmt.setString(1, uriconcept);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteProperty(final String uriconcept, final String property, final Connection connection) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE  \"uri_concept\"=? AND \"predicat\"=?")) {//NOSONAR
            deleteStmt.setString(1, uriconcept);
            deleteStmt.setString(2, property);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteAllProperty(final String uriconcept, final Connection connection) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE  \"uri_concept\"=?")) {//NOSONAR
            deleteStmt.setString(1, uriconcept);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteLanguage(final ISOLanguageCode language) throws SQLException {
        try (Connection connection = datasource.getConnection();
             PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"language\" WHERE  \"language_iso\"=?")) {//NOSONAR
            deleteStmt.setString(1, language.getTwoLetterCode().toLowerCase());
            deleteStmt.executeUpdate();
        }
    }

    private void lookForLanguageRegistration(final String language) throws SQLException {
        if (language != null) {
            //we verify that the language is already registred otherwise we record it
            final ISOLanguageCode currentLanguage = ISOLanguageCode.fromCode(language);
            addLanguage(currentLanguage);
        }
    }

    @Override
    public void addLanguage(final ISOLanguageCode currentLanguage) throws SQLException {
        if (!languages.contains(currentLanguage)) {
            try (Connection connection = datasource.getConnection();
                 PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"language\" VALUES (?, 1)")) {//NOSONAR
                stmt.setString(1, currentLanguage.getTwoLetterCode().toLowerCase());
                stmt.executeUpdate();
            }
            languages.add(currentLanguage);
        }
    }

    private void writeTerm(final String uriconcept, final String property, final List<Value> values, final int tableFlag, final Connection connection, final boolean deleteBefore) throws SQLException {
        writeTerm(uriconcept, property, values, tableFlag, connection, deleteBefore, schema);
    }

    private void writeTerm(final String uriconcept, final String property, final List<Value> values, final int tableFlag, final Connection connection, final boolean deleteBefore, String theme) throws SQLException {
        if (values == null) {return;}
        if (theme  == null) {theme = schema;}

        final String table;
        if (tableFlag == COMPLETION) {
            table = "terme_completion";
        } else {
            table = "terme_localisation";
        }

        if (deleteBefore) {
            try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + table + "\" WHERE  \"uri_concept\"=? AND \"type_terme\"=?")) {//NOSONAR
                deleteStmt.setString(1, uriconcept);
                deleteStmt.setString(2, property);
                deleteStmt.executeUpdate();
            }
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"" + table + "\"" + " VALUES (?, ?, ?, ?, ?)")) {//NOSONAR
            for (Value value : values) {
                //we verify that the language is already registred otherwise we record it
                lookForLanguageRegistration(value.getLang());

                stmt.setString(1, uriconcept);
                stmt.setString(2, value.getValue());
                stmt.setString(3, theme);
                if (value.getLang() == null) {
                    LOGGER.warning("You are not supposed to write skos property with no language/ Using default 'en'");
                    stmt.setString(4, "en");
                } else {
                    stmt.setString(4, value.getLang().toLowerCase());
                }
                stmt.setString(5, property);
                stmt.executeUpdate();
            }
        }
    }

    private void deleteAllTerm(final String uriconcept, final int tableFlag, final Connection connection) throws SQLException {
        final String table;
        if (tableFlag == COMPLETION) {
            table = "terme_completion";
        } else {
            table = "terme_localisation";
        }
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + table + "\" WHERE  \"uri_concept\"=?")) {//NOSONAR
            deleteStmt.setString(1, uriconcept);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteAllTermForLanguage(final ISOLanguageCode language) throws SQLException {
        try (Connection connection = datasource.getConnection();
             PreparedStatement deleteStmt  = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"terme_completion\" WHERE  \"langage_iso\"=?");//NOSONAR
             PreparedStatement deleteStmt2 = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"terme_localisation\" WHERE  \"langage_iso\"=?")) {//NOSONAR

            deleteStmt.setString(1, language.getTwoLetterCode().toLowerCase());
            deleteStmt.executeUpdate();
            deleteStmt2.setString(1, language.getTwoLetterCode().toLowerCase());
            deleteStmt2.executeUpdate();
        }
    }

    @Override
    public String writeConcept(final Concept concept) throws SQLException {
        if (getConcept(concept.getAbout()) == null) {
            return insertConcept(concept);
        } else {
            return updateConcept(concept);
        }
    }

    public void writeConcept(final FullConcept concept) throws SQLException {
        if (getConcept(concept.getUri()) == null) {
            insertConcept(concept);
        } else {
            updateConcept(concept);
        }
    }

    protected String insertConcept(final Concept concept) throws SQLException {
        final String uriConcept;
        if (concept.getAbout() == null) {
            uriConcept = UUID.randomUUID().toString();
        } else {
            uriConcept = concept.getAbout();
        }
        try (Connection c = datasource.getConnection()) {
            concept.setDefaultTypeIfNone();
            writeProperty(uriConcept, CREATOR_PREDICATE,            concept.getCreator(), c);
            writeProperty(uriConcept, DATE_PREDICATE,               concept.getDate(), c);
            writeProperty(uriConcept, DESCRIPTION_PREDICATE,        concept.getDescription(), c);
            writeProperty(uriConcept, RIGHTS_PREDICATE,             concept.getRights(), c);
            writeProperty(uriConcept, TITLE_PREDICATE,              concept.getTitle(), c);
            writeProperty(uriConcept, SUBJECT_PREDICATE,            concept.getSubject(), c);
            writeProperty(uriConcept, CONTRIBUTOR_PREDICATE,        concept.getContributor(), c);
            writeProperty(uriConcept, ISSUED_PREDICATE,             concept.getIssued(), c);
            writeProperty(uriConcept, MODIFIED_PREDICATE,           concept.getModified(), c);
            writeProperty(uriConcept, HAS_VERSION_PREDICATE,        concept.getHasVersion(), c);
            writeProperty(uriConcept, VALUE_PREDICATE,              concept.getValue(), c);
            writeProperty(uriConcept, CHANGE_NOTE_PREDICATE,        concept.getChangeNote(), c);
            writeProperty(uriConcept, EXTERNAL_ID_PREDICATE,        concept.getExternalID(), c);
            writeProperty(uriConcept, NAME_PREDICATE,               concept.getName(), c);
            writeProperty(uriConcept, TYPE_PREDICATE,               Arrays.asList(concept.getType()), c);
            writeProperty(uriConcept, BROADER_PREDICATE,            concept.getBroader(), c);
            writeProperty(uriConcept, NARROWER_PREDICATE,           concept.getNarrower(), c);
            writeProperty(uriConcept, HAS_TOP_CONCEPT_PREDICATE,    concept.getHasTopConcept(), c);
            writeProperty(uriConcept, LANGUAGE_PREDICATE,           concept.getLanguage(), c);
            writeProperty(uriConcept, RELATED_PREDICATE,            concept.getRelated(), c);
            writeProperty(uriConcept, NARROWER_TRANS_PREDICATE,     concept.getNarrowerTransitive(), c);
            writeProperty(uriConcept, HIERARCHY_ROOT_TY_PREDICATE,  Arrays.asList(concept.getHierarchyRootType()), c);
            writeProperty(uriConcept, HIERARCHY_ROOT_PREDICATE,     Arrays.asList(concept.getHierarchyRoot()), c);

            final String theme;
            if (concept.getInScheme() != null && !concept.getInScheme().isEmpty()) {
                theme = concept.getInScheme().get(0).getResource();
            } else {
                theme = null;
            }
            writeTerm(uriConcept, LABEL_TYPE,            concept.getLabel(),       COMPLETION,   c, false, theme);
            writeTerm(uriConcept, ALT_LABEL_TYPE,        concept.getAltLabel(),    COMPLETION,   c, false, theme);
            writeTerm(uriConcept, PREF_LABEL_TYPE,       concept.getPrefLabel(),   COMPLETION,   c, false, theme);
            writeTerm(uriConcept, DEFINITION_LABEL_TYPE, concept.getDefinition(),  LOCALISATION, c, false);
            writeTerm(uriConcept, SCOPE_NOTE_TYPE,       concept.getScopeNote(),   LOCALISATION, c, false);
            writeTerm(uriConcept, HISTORY_NOTE_TYPE,     concept.getHistoryNote(), LOCALISATION, c, false);
            writeTerm(uriConcept, EXAMPLE_TYPE,          concept.getExample(),     LOCALISATION, c, false);
        }
        return uriConcept;
    }

    protected String updateConcept(final Concept concept) throws SQLException {
        final String uriConcept = concept.getAbout();
        try (Connection c = datasource.getConnection()) {
            updateProperty(uriConcept, CREATOR_PREDICATE,           concept.getCreator(), c);
            updateProperty(uriConcept, CONTRIBUTOR_PREDICATE,       concept.getContributor(), c);
            updateProperty(uriConcept, DATE_PREDICATE,              concept.getDate(), c);
            updateProperty(uriConcept, DESCRIPTION_PREDICATE,       concept.getDescription(), c);
            updateProperty(uriConcept, RIGHTS_PREDICATE,            concept.getRights(), c);
            updateProperty(uriConcept, TITLE_PREDICATE,             concept.getTitle(), c);
            updateProperty(uriConcept, SUBJECT_PREDICATE,           concept.getSubject(), c);
            updateProperty(uriConcept, ISSUED_PREDICATE,            concept.getIssued(), c);
            updateProperty(uriConcept, MODIFIED_PREDICATE,          concept.getModified(), c);
            updateProperty(uriConcept, HAS_VERSION_PREDICATE,       concept.getIssued(), c);
            updateProperty(uriConcept, CHANGE_NOTE_PREDICATE,       concept.getChangeNote(), c);
            updateProperty(uriConcept, VALUE_PREDICATE,             concept.getValue(), c);
            updateProperty(uriConcept, NAME_PREDICATE,              concept.getName(), c);
            updateProperty(uriConcept, EXTERNAL_ID_PREDICATE,       concept.getExternalID(), c);
            updateProperty(uriConcept, HIERARCHY_ROOT_PREDICATE,    concept.getHierarchyRoot(), c);
            updateProperty(uriConcept, TYPE_PREDICATE,              concept.getType(), c);
            updateProperty(uriConcept, HIERARCHY_ROOT_TY_PREDICATE, concept.getHierarchyRootType(), c);
            updateProperty(uriConcept, RELATED_PREDICATE,           concept.getRelated(), c);
            updateProperty(uriConcept, LANGUAGE_PREDICATE,          concept.getLanguage(), c);
            updateProperty(uriConcept, BROADER_PREDICATE,           concept.getBroader(), c);
            updateProperty(uriConcept, NARROWER_PREDICATE,          concept.getNarrower(), c);
            updateProperty(uriConcept, NARROWER_TRANS_PREDICATE,    concept.getNarrowerTransitive(), c);
            updateProperty(uriConcept, HAS_TOP_CONCEPT_PREDICATE,   concept.getHasTopConcept(), c);

            final String theme;
            if (concept.getInScheme() != null && !concept.getInScheme().isEmpty()) {
                theme = concept.getInScheme().get(0).getResource();
            } else {
                theme = null;
            }
            writeTerm(uriConcept, LABEL_TYPE,            concept.getLabel(),       COMPLETION,   c, true, theme);
            writeTerm(uriConcept, ALT_LABEL_TYPE,        concept.getAltLabel(),    COMPLETION,   c, true, theme);
            writeTerm(uriConcept, PREF_LABEL_TYPE,       concept.getPrefLabel(),   COMPLETION,   c, true, theme);
            writeTerm(uriConcept, DEFINITION_LABEL_TYPE, concept.getDefinition(),  LOCALISATION, c, true);
            writeTerm(uriConcept, SCOPE_NOTE_TYPE,       concept.getScopeNote(),   LOCALISATION, c, true);
            writeTerm(uriConcept, EXAMPLE_TYPE,          concept.getExample(),     LOCALISATION, c, true);
            writeTerm(uriConcept, HISTORY_NOTE_TYPE,     concept.getHistoryNote(), LOCALISATION, c, true);
        }
        return uriConcept;
    }

    protected String updateConcept(final FullConcept concept) throws SQLException {
        final String uriConcept = concept.getUri();
        try (Connection c = datasource.getConnection()) {
            updateProperty(uriConcept, RELATED_PREDICATE,           concept.getRelated(), c);
            updateProperty(uriConcept, BROADER_PREDICATE,           concept.getBroaders(), c);
            updateProperty(uriConcept, NARROWER_PREDICATE,          concept.getNarrowers(), c);

            List<Value> prefLabels = concept.getPrefLabel().entrySet().stream().map(e -> new Value(e.getValue(), e.getKey())).toList();
            writeTerm(uriConcept, PREF_LABEL_TYPE,      prefLabels,   COMPLETION,   c, true);

            List<Value> definitions = concept.getDefinition().entrySet().stream().map(e -> new Value(e.getValue(), e.getKey())).toList();
            writeTerm(uriConcept, DEFINITION_LABEL_TYPE,      definitions,   LOCALISATION,   c, true);

            List<Value> altLabels = concept.getAltLabels().entrySet().stream().flatMap(e -> {
                List<Value> vals = new ArrayList<>();
                for (String v : e.getValue()) {
                    vals.add(new Value(e.getKey(), v));
                }
                return vals.stream();
            }).toList();
            writeTerm(uriConcept, ALT_LABEL_TYPE,        altLabels,    COMPLETION,   c, true);
        }
        return uriConcept;
    }

    @Override
    public void deleteConcept(final Concept concept) throws SQLException {
        final String uriConcept = concept.getAbout();
        try (Connection c = datasource.getConnection()) {
            deleteAllProperty(uriConcept, c);

            deleteAllTerm(uriConcept, COMPLETION, c);
            deleteAllTerm(uriConcept, LOCALISATION, c);

            deleteReference(uriConcept, c);
        }
    }

    @Override
    public void deleteConceptCascad(final Concept concept) throws SQLException {
        deleteConceptCascad(concept, new HashMap<>());
    }

    private void deleteConceptCascad(final Concept concept, final Map<String, Concept> alreadyProcess) throws SQLException {
        final String uriConcept = concept.getAbout();
        if (alreadyProcess.containsKey(uriConcept)) {
            return;
        } else {
            alreadyProcess.put(uriConcept, concept);
        }

        if (concept.getNarrower() != null) {
            for (Concept child : concept.getNarrower()) {

                final Concept fullChild;
                if (child.getAbout() != null) {
                    fullChild = child;
                } else if(child.getResource() != null) {
                    fullChild = getConcept(child.getResource());
                } else {
                    LOGGER.log(Level.WARNING, "About and resource property cannot be null.");
                    continue;
                }

                if (fullChild != null) {
                    boolean delete = true;
                    // id there is another parent (cyclic graphe) we don't delete the child
                    for (Concept childBroader : fullChild.getBroader()) {
                        final String broaderUri = childBroader.getResource();
                        if (!broaderUri.equals(uriConcept)) {
                            delete = false;
                        }
                    }
                    if (delete) {
                        deleteConceptCascad(fullChild, alreadyProcess);
                    }
                }
            }
        }
        deleteConcept(concept);
    }

    /**
     * Deletes the concepts with the specified {@code URI}.
     *
     * @param conceptUri the concept uri
     */
    @Override
    public void deleteConcept(String conceptUri) {
        try {
            deleteConcept(new Concept(conceptUri));
        } catch (SQLException ex) {
            throw new RuntimeException("SQL exception in deleteConcept()", ex);
        }
    }

    /**
     * Deletes the concepts with the specified {@code URI} and all its "narrowers".
     *
     * @param conceptUri the concept uri
     */
    @Override
    public void deleteConceptCascade(String conceptUri) {
        try {
            deleteConceptCascad(new Concept(conceptUri));
        } catch (SQLException ex) {
            throw new RuntimeException("SQL exception in deleteConcept()", ex);
        }
    }

    /**
     * Inserts the specified concept in database.
     *
     * @param fullConcept the concept value
     */
    public void insertConcept(FullConcept fullConcept) {

        String insertRelation = "INSERT INTO \"" + schema + "\".\"" + TABLE_NAME + "\" (uri_concept, predicat, objet) VALUES (?, ?, ?)";

        String insertComplTerm = "INSERT INTO \"" + schema + "\".\"terme_completion\" VALUES (?, ?, ?, ?, ?)";

        String insertLocalTerm = "INSERT INTO \"" + schema + "\".\"terme_localisation\" VALUES (?, ?, ?, ?, ?)";

        try (Connection con = datasource.getConnection();
             PreparedStatement relationStmt = con.prepareStatement(insertRelation);//NOSONAR
             PreparedStatement complTermStmt = con.prepareStatement(insertComplTerm);//NOSONAR
             PreparedStatement lacalTermStmt = con.prepareStatement(insertLocalTerm)) {//NOSONAR

            // Type.
            writeProperty(fullConcept.getUri(), TYPE_PREDICATE, CONCEPT_TYPE, relationStmt);
            relationStmt.addBatch();

            if (fullConcept.isTopConcept()) {
                List<Concept> hierarchyRoots = getHierarchyRoots(null);

                // Create default hierarchy root.
                if (hierarchyRoots.isEmpty()) {
                    String rootUri = UUID.randomUUID().toString();
                    hierarchyRoots.add(new Concept(rootUri));
                    writeProperty(rootUri, HIERARCHY_ROOT_PREDICATE, "true", relationStmt);
                    relationStmt.addBatch();
                    writeProperty(rootUri, TYPE_PREDICATE, CONCEPT_TYPE, relationStmt);
                    relationStmt.addBatch();
                    complTermStmt.setString(1, rootUri);
                    complTermStmt.setString(2, "ROOT");
                    complTermStmt.setString(3, schema);
                    complTermStmt.setString(4, defaultLanguage.getTwoLetterCode().toLowerCase());
                    complTermStmt.setString(5, PREF_LABEL_TYPE);
                    complTermStmt.addBatch();
                }

                // Top concept.
                for (Concept concept : hierarchyRoots) {
                    writeProperty(concept.getAbout(), HAS_TOP_CONCEPT_PREDICATE, fullConcept.getUri(), relationStmt);
                    relationStmt.addBatch();
                }
                relationStmt.executeBatch();
            } else {
                // Broaders.
                for (ConceptBrief conceptBrief : fullConcept.getBroaders()) {
                    writeProperty(fullConcept.getUri(), BROADER_PREDICATE, conceptBrief.getUri(), relationStmt);
                    relationStmt.addBatch();
                    writeProperty(conceptBrief.getUri(), NARROWER_PREDICATE, fullConcept.getUri(), relationStmt);
                    relationStmt.addBatch();
                }
                relationStmt.executeBatch();
            }

            // Narrowers.
            for (ConceptBrief conceptBrief : fullConcept.getNarrowers()) {
                writeProperty(fullConcept.getUri(), NARROWER_PREDICATE, conceptBrief.getUri(), relationStmt);
                relationStmt.addBatch();
                writeProperty(conceptBrief.getUri(), NARROWER_PREDICATE, fullConcept.getUri(), relationStmt);
                relationStmt.addBatch();
            }
            relationStmt.executeBatch();

            // Related.
            for (ConceptBrief conceptBrief : fullConcept.getRelated()) {
                writeProperty(fullConcept.getUri(), RELATED_PREDICATE, conceptBrief.getUri(), relationStmt);
                relationStmt.addBatch();
                writeProperty(conceptBrief.getUri(), RELATED_PREDICATE, fullConcept.getUri(), relationStmt);
                relationStmt.addBatch();
            }
            relationStmt.executeBatch();

            // Preferred labels.
            for (Map.Entry<String, String> entry : fullConcept.getPrefLabel().entrySet()) {
                if (isNotBlank(entry.getValue())) {
                    complTermStmt.setString(1, fullConcept.getUri());
                    complTermStmt.setString(2, entry.getValue());
                    complTermStmt.setString(3, schema);
                    complTermStmt.setString(4, entry.getKey());
                    complTermStmt.setString(5, PREF_LABEL_TYPE);
                    complTermStmt.addBatch();
                }
            }
            complTermStmt.executeBatch();

            // Alternative labels.
            for (Map.Entry<String, String[]> entry : fullConcept.getAltLabels().entrySet()) {
                for (String value : entry.getValue()) {
                    if (isNotBlank(value)) {
                        complTermStmt.setString(1, fullConcept.getUri());
                        complTermStmt.setString(2, value);
                        complTermStmt.setString(3, schema);
                        complTermStmt.setString(4, entry.getKey());
                        complTermStmt.setString(5, ALT_LABEL_TYPE);
                        complTermStmt.addBatch();
                    }
                }
            }
            complTermStmt.executeBatch();

            // Definitions.
            for (Map.Entry<String, String> entry : fullConcept.getDefinition().entrySet()) {
                if (isNotBlank(entry.getValue())) {
                    lacalTermStmt.setString(1, fullConcept.getUri());
                    lacalTermStmt.setString(2, entry.getValue());
                    lacalTermStmt.setString(3, schema);
                    lacalTermStmt.setString(4, entry.getKey());
                    lacalTermStmt.setString(5, DEFINITION_LABEL_TYPE);
                    lacalTermStmt.addBatch();
                }
            }
            lacalTermStmt.executeBatch();

        } catch (SQLException ex) {
            throw new RuntimeException("SQL exception in insertConcept()", ex);
        }
    }

    @Override
    public void writeRdf(final RDF rdf) throws SQLException {
        if (rdf != null) {
            if (rdf.getConcept() != null) {
                for (Concept c : rdf.getConcept()) {
                    insertConcept(c);
                }
            }
            if (rdf.getDescription() != null) {
                for (Concept c : rdf.getDescription()) {
                    insertConcept(c);
                }
            }
        }
    }

    @Override
    public void updateThesaurusProperties() throws SQLException {
        try (Connection connection = datasource.getConnection();
             PreparedStatement upStmt = connection.prepareStatement("UPDATE \"" + schema + "\".\"propriete_thesaurus\" SET \"uri\"=?, \"name\"=?, \"description\"=?, \"enable\"=?, \"defaultLang\"=?")) {//NOSONAR
            final int enable;
            if (state) {
                enable = 1;
            } else {
                enable = 0;
            }
            upStmt.setString(1, uri);
            upStmt.setString(2, name);
            upStmt.setString(3, description);
            upStmt.setInt(4, enable);
            if (defaultLanguage != null) {
                upStmt.setString(5, defaultLanguage.getTwoLetterCode());
            } else {
                upStmt.setNull(5, java.sql.Types.VARCHAR);
            }
            upStmt.executeUpdate();
        }
    }

    @Override
    public void store() throws SQLException, IOException {
        boolean derby        = "derby".equals(dialect);
        String sql           = IOUtilities.toString(Util.getResourceAsStream("org/constellation/thesaurus/io/sql/create-new-thesaurus.sql"));
        sql                  = sql.replace("{schema}", schema);
        if (derby) {
            sql = sql.replace("(100000)", "(1000)");
        }
        try (Connection connection = datasource.getConnection()) {
            final ScriptRunner runner;
            if (derby) {
                runner = new DerbySqlScriptRunner(connection);
            } else {
                runner = new ScriptRunner(connection);
            }
            runner.run(sql);
            if (languages != null) {
                try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"language\" VALUES (?, 1)")) {//NOSONAR
                    for (ISOLanguageCode language : languages) {
                        stmt.setString(1, language.getTwoLetterCode().toLowerCase());
                        stmt.executeUpdate();
                    }
                }
            }
            runner.close(false);
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"propriete_thesaurus\" VALUES (?, ?, ?, ?, 1)")) {//NOSONAR
                stmt.setString(1, uri);
                stmt.setString(2, name);
                stmt.setString(3, description);
                if (defaultLanguage == null) {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                } else {
                    stmt.setString(4, defaultLanguage.getTwoLetterCode());
                }
                stmt.executeUpdate();
            }
        }
    }

    @Override
    public void delete() throws SQLException {
        try (Connection connection = datasource.getConnection();
             Statement stmt = connection.createStatement()) {
            if ("derby".equals(dialect)) {
                stmt.executeUpdate("DROP TABLE \"" + schema +"\".\"language\"");//NOSONAR
                stmt.executeUpdate("DROP TABLE \"" + schema +"\".\"" + TABLE_NAME + "\"");//NOSONAR
                stmt.executeUpdate("DROP TABLE \"" + schema +"\".\"propriete_thesaurus\"");//NOSONAR
                stmt.executeUpdate("DROP TABLE \"" + schema +"\".\"terme_completion\"");//NOSONAR
                stmt.executeUpdate("DROP TABLE \"" + schema +"\".\"terme_localisation\"");//NOSONAR
                stmt.executeUpdate("DROP SCHEMA \"" + schema +"\" RESTRICT");//NOSONAR
            } else {
                stmt.executeUpdate("DROP SCHEMA \"" + schema + "\" CASCADE");//NOSONAR
            }
        } 
    }

    @Override
    public void delete(final ISOLanguageCode language) throws SQLException {
        deleteAllTermForLanguage(language);
        deleteLanguage(language);
        this.languages.remove(language);
    }

    @Override
    public void computeTopMostConcept() throws SQLException {
        final List<Concept> topConcepts = new ArrayList<>();
        /*
         * 1) look for concept with no broader
         */
        final String query = " SELECT distinct \"uri_concept\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\"  WHERE \"uri_concept\" NOT IN ("
                           + " SELECT distinct \"uri_concept\" FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE \"predicat\"='http://www.w3.org/2004/02/skos/core#broader')";
        try (Connection connection = datasource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);//NOSONAR
            ResultSet result = stmt.executeQuery()) {
            while (result.next()) {
                topConcepts.add(readConcept(result.getString(1), false, connection, null));
            }
        }

       /*
        * 2) build a new root concept
        *
        */
        final Concept newRoot = new Concept();
        newRoot.setHierarchyRoot(true);
        final Concept hierarchyRootType = new Concept();
        hierarchyRootType.setResource("http://www.w3.org/2004/02/skos/core#ConceptScheme");
        newRoot.setHierarchyRootType(hierarchyRootType);
        newRoot.setType(hierarchyRootType);
        newRoot.setLabel(name);
        newRoot.setTitle(name);
        newRoot.setHasTopConcept(topConcepts);

        writeConcept(newRoot);
    }
}
