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
import org.constellation.concurrent.BoundedCompletionService;
import org.constellation.metadata.index.AbstractCSWIndexer;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.ReflectionUtilities;
import org.constellation.util.XpathUtils;
import org.geotoolkit.index.IndexingException;
import org.opengis.metadata.Metadata;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.constellation.api.PathType;
import org.geotoolkit.metadata.MetadataStore;

/**
 * A Lucene Index Handler for a generic Database.
 * @author Guilhem Legal
 */
public class GenericIndexer extends AbstractCSWIndexer<Object> {

    /**
     * The Reader of this lucene index (generic DB mode).
     */
    private final MetadataStore store;

    /**
     * Shared Thread Pool for parallel execution
     */
    private final ExecutorService pool = Executors.newFixedThreadPool(6);

    /**
     * Creates a new Lucene Index into the specified directory with the specified generic database reader.
     *
     * @param store A generic reader to request the metadata dataSource.
     * @param configurationDirectory The directory where the index can write indexation file.
     * @param indexID The identifier, if there is one, of the index.
     * @param additionalQueryable A map of additional queryable element.
     * @param create {@code true} if the index need to be created.
     *
     * @throws org.geotoolkit.index.IndexingException If an erro roccurs during the index creation.
     */
    public GenericIndexer(final MetadataStore store, final Path configurationDirectory, final String indexID,
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
    public GenericIndexer(final List<Object> toIndex, final Map<String, PathType> additionalQueryable, final Path configDirectory,
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
    public GenericIndexer(final List<Object> toIndex, final Map<String, PathType> additionalQueryable, final Path configDirectory,
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
    protected Object getEntry(final String identifier) throws IndexingException {
        try {
            return store.getMetadata(identifier, MetadataType.ISO_19115);
        } catch (MetadataIoException ex) {
            throw new IndexingException("Metadata_IOException while reading entry for:" + identifier, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void indexSpecialField(final Object metadata, final Document doc) throws IndexingException {
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
    protected String getType(final Object metadata) {
        return metadata.getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isISO19139(final Object meta) {
        return meta instanceof Metadata;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isDublinCore(final Object meta) {
        return ReflectionUtilities.instanceOf("org.geotoolkit.csw.xml.v202.RecordType", meta.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEbrim25(final Object meta) {
        return ReflectionUtilities.instanceOf("org.geotoolkit.ebrim.xml.v250.RegistryObjectType", meta.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isEbrim30(final Object meta) {
        return ReflectionUtilities.instanceOf("org.geotoolkit.ebrim.xml.v300.IdentifiableType", meta.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFeatureCatalogue(Object meta) {
        return ReflectionUtilities.instanceOf("org.geotoolkit.feature.catalog.FeatureCatalogueImpl", meta.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void indexQueryableSet(final Document doc, final Object metadata,final  Map<String, PathType> queryableSet, final StringBuilder anyText) throws IndexingException {
        final CompletionService<TermValue> cs = new BoundedCompletionService<>(this.pool, 5);
        for (final String term :queryableSet.keySet()) {
            cs.submit(new Callable<TermValue>() {

                @Override
                public TermValue call() {
                    final List<String> paths = XpathUtils.xpathToMDPath(queryableSet.get(term).paths);
                    return new TermValue(term, Utils.extractValues(metadata, paths));
                }
            });
        }

        for (int i = 0; i < queryableSet.size(); i++) {
            try {
                final TermValue values = formatStringValue(cs.take().get());
                indexFields(values.value, values.term, anyText, doc);

            } catch (InterruptedException ex) {
               LOGGER.log(Level.WARNING, "InterruptedException in parralele create document:\n{0}", ex.getMessage());
            } catch (ExecutionException ex) {
               LOGGER.log(Level.WARNING, "ExecutionException in parralele create document:\n" + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Format the value part in case of a "date" term.
     * @param values
     * @return
     */
    private TermValue formatStringValue(final TermValue values) {
         if ("date".equals(values.term)) {
             final List<Object> newValues = new ArrayList<>();
             for (Object value : values.value) {
                 if (value instanceof String) {
                     String stringValue = (String) value;
                     if (stringValue.endsWith("z") || stringValue.endsWith("Z")) {
                         stringValue = stringValue.substring(0, stringValue.length() - 1);
                     }
                     if (stringValue != null) {
                        stringValue = stringValue.replace("-", "");
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
         }
         return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getIdentifier(final Object obj) {
        return Utils.findIdentifier(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    protected String getValues(final Object metadata, final List<String> paths) {
        final List<String> mdpaths = XpathUtils.xpathToMDPath(paths);
        final List<Object> values =  Utils.extractValues(metadata, mdpaths);
        final StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            sb.append(value).append(',');
        }
        if (!sb.toString().isEmpty()) {
            // we remove the last ','
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    @Override
    public void destroy() {
        LOGGER.info("shutting down generic indexer");
        super.destroy();
        try {
            pool.shutdown();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.FINER, ex.getMessage(), ex);
        } finally {
            // in case there's tasks which didn't finished in specified timeout.
            pool.shutdownNow();
        }
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
