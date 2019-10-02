/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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

package org.constellation.metadata.index.elasticsearch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.constellation.api.PathType;
import org.constellation.metadata.utils.Utils;
import org.constellation.util.NodeUtilities;
import org.elasticsearch.ElasticsearchException;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.temporal.object.ISODateParser;
import org.w3c.dom.Node;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ElasticSearchNodeIndexer extends ElasticSearchIndexer<Node> {

    private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public ElasticSearchNodeIndexer(final MetadataStore store, final String host, final String clusterName, final String indexName,
                                    final Map<String, PathType> additionalQueryable, final boolean create, final boolean remoteES) throws IndexingException {
        super(store, host, clusterName, indexName, additionalQueryable, remoteES);
        try {
            if (create && needCreation()) {
                createIndex();
            }
        } catch (ElasticsearchException ex) {
            super.destroy();
            throw new IndexingException("error while connecting ELasticSearch cluster", ex);
        }
    }

    public ElasticSearchNodeIndexer(final List<Node> toIndex, final String host, final String clusterName, final String indexName,
                                    final Map<String, PathType> additionalQueryable, final boolean create, final boolean remoteES) throws IndexingException {
        super(null, host, clusterName, indexName, additionalQueryable, remoteES);
        try {
            if (create && needCreation()) {
                createIndex(toIndex);
            }
        } catch (ElasticsearchException ex) {
            super.destroy();
            throw new IndexingException("error while connecting ELasticSearch cluster", ex);
        }
    }

    @Override
    protected String getIdentifier(Node metadata) {
        return Utils.findIdentifier(metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void indexSpecialField(final Node metadata, final Map map) throws IndexingException {
        final String identifier = getIdentifier(metadata);
        if ("unknow".equals(identifier)) {
            throw new IndexingException("unexpected metadata type.");
        }
        map.put("id", identifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Object> getValues(final Node metadata, final PathType paths) {
        return NodeUtilities.extractValues(metadata, paths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void indexQueryableSet(final Map doc, final Node metadata, final  Map<String, PathType> queryableSet, final StringBuilder anyText) throws IndexingException {
        for (final String term : queryableSet.keySet()) {
            final PathType ptype = queryableSet.get(term);
            final TermValue tm = new TermValue(term, NodeUtilities.extractValues(metadata, ptype, false));

            final ElasticSearchNodeIndexer.TermValue values = formatStringValue(tm, ptype.type);
            indexFields(ptype.type, values.value, values.term, anyText, doc);


        }
    }

    protected void indexFields(final Class type, final List<Object> values, final String fieldName, final StringBuilder anyText, final Map doc) {
        final List<Object> cleanValues = new ArrayList<>();
        for (Object value : values) {
            if (type == String.class || type == Boolean.class) { // TODO look for the boolean case
                String stringValue = (String) value;
                if (!"null".equals(stringValue) && anyText.indexOf(stringValue) == -1) {
                    anyText.append(stringValue).append(" ");
                }
                cleanValues.add(value);
            } else if (type == Date.class){
                if (!"null".equals(value)) {
                    doc.put(fieldName, value);
                    doc.put(fieldName + "_sort", value);
                    cleanValues.add(value);
                }
            } else if (Number.class.isAssignableFrom(type)) {
                if (!"null".equals(value)) {
                    doc.put(fieldName, value);
                    doc.put(fieldName + "_sort", value);
                    cleanValues.add(value);
                }
            } else if (value != null){
                LOGGER.log(Level.WARNING, "unexpected type for field:{0}", type);
            }
        }

        if (cleanValues.size() == 1) {
            doc.put(fieldName, cleanValues.get(0));
            doc.put(fieldName + "_sort", cleanValues.get(0));
        } else if (!cleanValues.isEmpty()){
            doc.put(fieldName, cleanValues);
            doc.put(fieldName + "_sort", cleanValues);
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

    /**
     * Format the value part in case of a "date" term.
     * @param values
     * @return
     */
    private TermValue formatStringValue(final TermValue values, Class type) {
        if (Date.class.equals(type)) {
            final ISODateParser parser = new ISODateParser();
            final List<Object> newValues = new ArrayList<>();
            for (Object value : values.value) {
                if (value instanceof String) {
                    String stringValue = (String) value;
                    Date d = parser.parseToDate(stringValue);
                    synchronized (formatter) {
                        value = formatter.format(d);
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
}
