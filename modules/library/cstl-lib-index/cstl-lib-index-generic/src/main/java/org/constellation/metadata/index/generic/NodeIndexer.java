/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

package org.constellation.metadata.index.generic;

// J2SE dependencies

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.constellation.metadata.index.AbstractCSWIndexer;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.index.IndexingException;
import org.constellation.api.PathType;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.metadata.RecordInfo;


/**
 * A Lucene Index Handler for a generic Database.
 * @author Guilhem Legal
 */
public class NodeIndexer extends AbstractCSWIndexer<Node> {

    /**
     * The Reader of this lucene index (generic DB mode).
     */
    protected final MetadataStore store;

    /**
     * Creates a new Lucene Index into the specified directory with the specified generic database reader.
     *
     * @param store A node reader to request the metadata dataSource.
     * @param configurationDirectory The directory where the index can write indexation file.
     * @param indexID The identifier, if there is one, of the index/service.
     * @param additionalQueryable A map of additional queryable element.
     * @param create {@code true} if the index need to be created.
     *
     * @throws org.geotoolkit.index.IndexingException If an erro roccurs during the index creation.
     */
    public NodeIndexer(final MetadataStore store, final Path configurationDirectory, final String indexID,
            final Map<String, PathType> additionalQueryable, final boolean create) throws IndexingException {
        super(indexID, configurationDirectory, additionalQueryable);
        this.store = store;
        if (create && needCreation()) {
            createIndex();
        }
    }

    /**
     * Creates a new Lucene Index into the specified directory with the specified list of object to index.
     *
     * @param toIndex A list of Object
     * @param additionalQueryable A Map of additionable queryable to add to the index (name - List of Xpath)
     * @param configDirectory A directory where the index can write indexation file.
     * @param indexID The identifier, if there is one, of the index.
     * @param analyzer The lucene analyzer used.
     * @param logLevel A log level for info information.
     * @param create {@code true} if the index need to be created.
     *
     * @throws org.geotoolkit.index.IndexingException If an erro roccurs during the index creation.
     */
    public NodeIndexer(final List<Node> toIndex, final Map<String, PathType> additionalQueryable, final Path configDirectory,
            final String indexID, final Analyzer analyzer, final Level logLevel, final boolean create) throws IndexingException {
        super(indexID, configDirectory, analyzer, additionalQueryable);
        this.logLevel            = logLevel;
        this.store               = null;
        if (create && needCreation()) {
            createIndex(toIndex);
        }
    }

    /**
     * Creates a new Lucene Index into the specified directory with the specified list of object to index.
     *
     * @param toIndex A list of Object
     * @param additionalQueryable A Map of additionable queryable to add to the index (name - List of Xpath)
     * @param configDirectory A directory where the index can write indexation file.
     * @param indexID The identifier, if there is one, of the index.
     * @param create {@code true} if the index need to be created.
     *
     * @throws org.geotoolkit.index.IndexingException If an erro roccurs during the index creation.
     */
    public NodeIndexer(final List<Node> toIndex, final Map<String, PathType> additionalQueryable, final Path configDirectory,
            final String indexID, final boolean create) throws IndexingException {
        super(indexID, configDirectory, additionalQueryable);
        this.store = null;
        if (create && needCreation()) {
            createIndex(toIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<String> getAllIdentifiers() throws IndexingException {
        try {
            return store.getAllIdentifiers();
        } catch (MetadataIoException ex) {
            throw new IndexingException("Metadata_IOException while reading all identifiers", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<String> getIdentifierIterator() throws IndexingException {
        try {
            return store.getIdentifierIterator();
        } catch (MetadataIoException ex) {
            throw new IndexingException("Metadata_IOException while reading identifier iterator", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Node getEntry(final String identifier) throws IndexingException {
        try {
            RecordInfo record = store.getMetadata(identifier, MetadataType.NATIVE);
            if (record != null) {
                return record.node;
            }
        } catch (MetadataIoException ex) {
            throw new IndexingException("Metadata_IOException while reading entry for:" + identifier, ex);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void indexSpecialField(final Node metadata, final Document doc) throws IndexingException {
        final String identifier = getIdentifier(metadata);
        if ("unknow".equals(identifier)) {
            throw new IndexingException("unexpected metadata type.");
        }
        doc.add(new Field("id", identifier,  ID_TYPE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getType(final Node metadata) {
        return metadata.getLocalName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isISO19139(final Node meta) {
        return "MD_Metadata".equals(meta.getLocalName()) ||
               "MI_Metadata".equals(meta.getLocalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isDublinCore(final Node meta) {
        return "Record".equals(meta.getLocalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEbrim25(final Node meta) {
        // TODO list rootElement
        return "RegistryObject".equals(meta.getLocalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEbrim30(final Node meta) {
        // TODO list rootElement
        return "Identifiable".equals(meta.getLocalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFeatureCatalogue(Node meta) {
        return "FC_FeatureCatalogue".equals(meta.getLocalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isDIF(Node meta) {
        return "DIF".equals(meta.getLocalName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void indexQueryableSet(final Document doc, final Node metadata, final  Map<String, PathType> queryableSet, final StringBuilder anyText) throws IndexingException {
        for (final String term : queryableSet.keySet()) {
            final TermValue tm = new TermValue(term, NodeUtilities.extractValues(metadata, queryableSet.get(term)));
            final NodeIndexer.TermValue values;
            if (queryableSet.get(term).type.equals(Date.class)) {
                values = formatDateValue(tm);
            } else {
                values = tm;
            }
            indexFields(values.value, values.term, anyText, doc);
        }
    }

    /**
     * Format the value part in case of a "date" term.
     * @param values
     * @return
     */
    private TermValue formatDateValue(final TermValue values) {

        final List<Object> newValues = new ArrayList<>();
        for (Object value : values.value) {
            if (value instanceof String) {
                String stringValue = (String) value;
                if (stringValue.endsWith("z") || stringValue.endsWith("Z")) {
                    stringValue = stringValue.substring(0, stringValue.length() - 1);
                }
                if (stringValue != null) {
                   stringValue = stringValue.replace("-", "");
                   stringValue = stringValue.replace(":", "");
                   stringValue = stringValue.replace("T", "");
                   //add time if there is no
                   if (stringValue.length() == 8) {
                       stringValue = stringValue + "000000";
                   }
                   value = stringValue;
                }
            }
           newValues.add(value);
        }
        values.value = newValues;
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(final Node metadata) {
        return Utils.findIdentifier(metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Object> getValues(final Node metadata, final PathType paths) {
        return NodeUtilities.extractValues(metadata, paths);
    }

    @Override
    protected Iterator<Node> getEntryIterator() throws IndexingException {
        try {
            final Iterator<RecordInfo> it = store.getEntryIterator();
            return new Iterator<Node>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Node next() {
                    return it.next().node;
                }
            };
        } catch (MetadataIoException ex) {
            throw new IndexingException("Error while getting entry iterator", ex);
        }
    }

    @Override
    protected boolean useEntryIterator() {
        return store.getReader().useEntryIterator();
    }

    @Override
    public void destroy() {
        LOGGER.log(logLevel, "shutting down Node indexer");
        super.destroy();
    }

    private static class TermValue {
        public String term;

        public List<Object> value;

        public TermValue(String term, List<Object> value) {
            this.term  = term;
            this.value = value;
        }
    }
}
