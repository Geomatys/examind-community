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
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
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

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ThesaurusDatabaseWriter extends ThesaurusDatabase implements WriteableThesaurus {

    public ThesaurusDatabaseWriter(final DataSource datasource, final String schema, final boolean derby) {
        super(datasource, schema, derby);
    }

    /**
     * This constructor is used when the thesaurus is not yet stored in the database.
     *
     * @param datasource The datasource whiwh will store the thesaurus
     * @param schema The database schema for this thesaurus
     * @param derby A flag indicating if the datasource ids a derby implementation.
     * @param uri The unique identifier of the thesaurus
     * @param name The name of the thesaurus.
     * @param description A brief description of the thesaurus
     * @param languages A list of languages contained in the thesaurus (for label, altLabel, ...)
     * @param defaultLanguage The default language to be used when none is specified.
     */
    public ThesaurusDatabaseWriter(final DataSource datasource, final String schema, final boolean derby,
            final String uri, final String name, final String description, final List<ISOLanguageCode> languages,
            final ISOLanguageCode defaultLanguage) {
        super(datasource, schema, derby, uri, name, description, languages, defaultLanguage);
    }

    private void writeProperty(final String uriConcept, final String property, final List<? extends Object> conceptList, final Connection connection) throws SQLException {
        if (conceptList != null) {
            for (Object o : conceptList) {
                if (o instanceof Concept) {
                    final Concept c = (Concept) o;
                    if (c.getAbout() != null) {
                        writeProperty(uriConcept, property, c.getAbout(), connection);
                    } else if (c.getResource() != null) {
                        writeProperty(uriConcept, property, c.getResource(), connection);
                    } else {
                        LOGGER.log(Level.WARNING, "About and resource property cannot be null.");
                    }
                } else if (o instanceof String) {
                    writeProperty(uriConcept, property, (String)o, connection);
                } else if (o instanceof Boolean) {
                    writeProperty(uriConcept, property, o.toString(), connection);
                } else if (o != null) {
                    throw new IllegalArgumentException("Unexpected type for a property:" + o.getClass().getName());
                }
            }
        }
    }

    private void writeProperty(final String uriconcept, final String property, final String value, final Connection connection) throws SQLException {
        if (value == null) {return;}
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"" + TABLE_NAME + "\" VALUES (?, ?, ?, NULL)")) {
            stmt.setString(1, uriconcept);
            stmt.setString(2, property);
            stmt.setString(3, value);
            stmt.executeUpdate();
        }
    }

    private void updateProperty(final String uriconcept, final String property, final Object value, final Connection connection) throws SQLException {
        final boolean update;
        try (Statement stmt = connection.createStatement();
             ResultSet result = stmt.executeQuery(
                "SELECT \"uri_concept\" FROM  \"" + schema + "\".\"" + TABLE_NAME + "\" " +
                "WHERE \"uri_concept\"='" + uriconcept +"' " +
                "AND \"predicat\"='" + property + "'")) {
            update = result.next();
        }

        final String stringValue;
        if (value instanceof String) {
            stringValue = (String) value;
        } else if (value instanceof Boolean) {
            stringValue = value.toString();
        } else if (value instanceof Concept) {
            stringValue = ((Concept) value).getResource();
        } else if (value instanceof List) {
            deleteProperty(uriconcept, property, connection);
            writeProperty(uriconcept, property, (List)value, connection);
            return;
        } else if (value != null) {
            throw new IllegalArgumentException("Unexpected type for a property value :" + value.getClass().getName());
        } else {
            stringValue = null;
        }

        if (update) {

            if (stringValue != null) {
                try (PreparedStatement upStmt = connection.prepareStatement("UPDATE \"" + schema + "\".\"" + TABLE_NAME + "\" "
                        + "SET \"objet\"=? "
                        + "WHERE  \"uri_concept\"=? AND \"predicat\"=?")) {
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
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + TABLE_NAME + "\" WHERE  \"objet\"=?")) {
            deleteStmt.setString(1, uriconcept);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteProperty(final String uriconcept, final String property, final Connection connection) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + TABLE_NAME + "\" "
                + "WHERE  \"uri_concept\"=? AND \"predicat\"=?")) {
            deleteStmt.setString(1, uriconcept);
            deleteStmt.setString(2, property);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteAllProperty(final String uriconcept, final Connection connection) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + TABLE_NAME + "\" "
                + "WHERE  \"uri_concept\"=?")) {
            deleteStmt.setString(1, uriconcept);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteLanguage(final ISOLanguageCode language) throws SQLException {
        try (Connection connection = datasource.getConnection();
             PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"language\" "
                + "WHERE  \"language_iso\"=?")) {
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
                 Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("INSERT INTO \"" + schema + "\".\"language\" VALUES ('" + currentLanguage.getTwoLetterCode().toLowerCase() + "', 1)");
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
            try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + table + "\" "
                    + "WHERE  \"uri_concept\"=? AND \"type_terme\"=?")) {
                deleteStmt.setString(1, uriconcept);
                deleteStmt.setString(2, property);
                deleteStmt.executeUpdate();
            }
        }
        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"" + table + "\"" + " VALUES (?, ?, ?, ?, ?)")) {
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
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"" + table + "\" "
                + "WHERE  \"uri_concept\"=?")) {
            deleteStmt.setString(1, uriconcept);
            deleteStmt.executeUpdate();
        }
    }

    private void deleteAllTermForLanguage(final ISOLanguageCode language) throws SQLException {
        try (Connection connection = datasource.getConnection();
             PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"terme_completion\" "
                    + "WHERE  \"langage_iso\"=?");
             PreparedStatement deleteStmt2 = connection.prepareStatement("DELETE FROM \"" + schema + "\".\"terme_localisation\" "
                    + "WHERE  \"langage_iso\"=?");) {

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

    private String insertConcept(final Concept concept) throws SQLException {
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

    private String updateConcept(final Concept concept) throws SQLException {
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
        deleteConceptCascad(concept, new HashMap<String, Concept>());
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

        String insertRelation = "INSERT INTO \"" + schema + "\".propriete_concept" +
                " (uri_concept, predicat, objet) VALUES (?, ?, ?)";

        String insertComplTerm = "INSERT INTO \"" + schema + "\".terme_completion VALUES (?, ?, ?, ?, ?)";

        String insertLocalTerm = "INSERT INTO \"" + schema + "\".terme_localisation VALUES (?, ?, ?, ?, ?)";

        try (Connection con = datasource.getConnection();
             PreparedStatement relationStmt = con.prepareStatement(insertRelation);
             PreparedStatement complTermStmt = con.prepareStatement(insertComplTerm);
             PreparedStatement lacalTermStmt = con.prepareStatement(insertLocalTerm)) {

            // Type.
            relationStmt.setString(1, fullConcept.getUri());
            relationStmt.setString(2, TYPE_PREDICATE);
            relationStmt.setString(3, CONCEPT_TYPE);
            relationStmt.addBatch();

            if (fullConcept.isTopConcept()) {
                List<Concept> hierarchyRoots = getHierarchyRoots(null);

                // Create default hierarchy root.
                if (hierarchyRoots.isEmpty()) {
                    String rootUri = UUID.randomUUID().toString();
                    hierarchyRoots.add(new Concept(rootUri));
                    relationStmt.setString(1, rootUri);
                    relationStmt.setString(2, HIERARCHY_ROOT_PREDICATE);
                    relationStmt.setString(3, "true");
                    relationStmt.addBatch();
                    relationStmt.setString(1, rootUri);
                    relationStmt.setString(2, TYPE_PREDICATE);
                    relationStmt.setString(3, CONCEPT_TYPE);
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
                    relationStmt.setString(1, concept.getAbout());
                    relationStmt.setString(2, HAS_TOP_CONCEPT_PREDICATE);
                    relationStmt.setString(3, fullConcept.getUri());
                    relationStmt.addBatch();
                }
                relationStmt.executeBatch();
            } else {
                // Broaders.
                for (ConceptBrief conceptBrief : fullConcept.getBroaders()) {
                    relationStmt.setString(1, fullConcept.getUri());
                    relationStmt.setString(2, BROADER_PREDICATE);
                    relationStmt.setString(3, conceptBrief.getUri());
                    relationStmt.addBatch();
                    relationStmt.setString(1, conceptBrief.getUri());
                    relationStmt.setString(2, NARROWER_PREDICATE);
                    relationStmt.setString(3, fullConcept.getUri());
                    relationStmt.addBatch();
                }
                relationStmt.executeBatch();
            }

            // Narrowers.
            for (ConceptBrief conceptBrief : fullConcept.getNarrowers()) {
                relationStmt.setString(1, fullConcept.getUri());
                relationStmt.setString(2, NARROWER_PREDICATE);
                relationStmt.setString(3, conceptBrief.getUri());
                relationStmt.addBatch();
                relationStmt.setString(1, conceptBrief.getUri());
                relationStmt.setString(2, BROADER_PREDICATE);
                relationStmt.setString(3, fullConcept.getUri());
                relationStmt.addBatch();
            }
            relationStmt.executeBatch();

            // Related.
            for (ConceptBrief conceptBrief : fullConcept.getRelated()) {
                relationStmt.setString(1, fullConcept.getUri());
                relationStmt.setString(2, RELATED_PREDICATE);
                relationStmt.setString(3, conceptBrief.getUri());
                relationStmt.addBatch();
                relationStmt.setString(1, conceptBrief.getUri());
                relationStmt.setString(2, RELATED_PREDICATE);
                relationStmt.setString(3, fullConcept.getUri());
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
             PreparedStatement upStmt = connection.prepareStatement("UPDATE \"" + schema + "\".\"propriete_thesaurus\" "
                + "SET \"uri\"=?, \"name\"=?, \"description\"=?, \"enable\"=?, \"defaultLang\"=?")) {
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
        String sql           = IOUtilities.toString(getResourceAsStream("org/constellation/thesaurus/io/sql/create-new-thesaurus.sql"));
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
                for (ISOLanguageCode language : languages) {
                    runner.run("INSERT INTO \"" + schema + "\".\"language\" VALUES ('" + language.getTwoLetterCode().toLowerCase() + "', 1);");
                }
            }
            runner.close(false);
            try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO \"" + schema + "\".\"propriete_thesaurus\" VALUES (?, ?, ?, ?, 1)")) {
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
        try (Connection connection = datasource.getConnection()) {
            final ScriptRunner runner;
            if (derby) {
                runner = new DerbySqlScriptRunner(connection);
                runner.run("DROP TABLE \"" + schema +"\".\"language\";");
                runner.run("DROP TABLE \"" + schema +"\".\"propriete_concept\";");
                runner.run("DROP TABLE \"" + schema +"\".\"propriete_thesaurus\";");
                runner.run("DROP TABLE \"" + schema +"\".\"terme_completion\";");
                runner.run("DROP TABLE \"" + schema +"\".\"terme_localisation\";");
                runner.run("DROP SCHEMA \"" + schema +"\" RESTRICT;");
            } else {
                runner = new ScriptRunner(connection);
                runner.run("DROP SCHEMA \"" + schema + "\" CASCADE;");
            }
            runner.close(false);
        } catch (IOException ex) {
            // should never happen
            LOGGER.log(Level.WARNING, "IOException while deleting thesaurus", ex);
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
        try (Connection connection = datasource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(" SELECT distinct \"uri_concept\" " +
                    " FROM \"" + schema + "\".\"propriete_concept\" " +
                    " WHERE \"uri_concept\" NOT IN (" +
                    " SELECT distinct \"uri_concept\" FROM \"" + schema + "\".\"propriete_concept\" " +
                    " WHERE \"predicat\"='http://www.w3.org/2004/02/skos/core#broader')");
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

    /**
     * Return an input stream of the specified resource.
     * @param url The urel of the specified resource.
     *
     * @return A stream on the specified resource.
     */
    private static InputStream getResourceAsStream(final String url) {
        final ClassLoader cl = getContextClassLoader();
        return cl.getResourceAsStream(url);
    }

    /**
     * Obtain the Thread Context ClassLoader.
     */
    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {

            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }
}
