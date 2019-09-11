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

package org.constellation.metadata.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.util.BytesRef;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.util.NullArgumentException;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.lucene.index.AbstractIndexer;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.api.CommonConstants.NULL_VALUE;
import org.constellation.api.PathType;
import org.geotoolkit.index.tree.StoreIndexException;
import org.geotoolkit.index.tree.manager.SQLRtreeManager;
import org.geotoolkit.lucene.LuceneUtils;

/**
 *
 * @author Guilhem Legal (Geomatys)
 *
 * @param <A> the type of indexed Object
 */
public abstract class AbstractCSWIndexer<A> extends AbstractIndexer<A> implements Indexer<A> {

    protected static final String NOT_SPATIALLY_INDEXABLE = "unable to spatially index metadata: ";

    private final Map<String, PathType> additionalQueryable;

    protected static final FieldType ID_TYPE = new FieldType();
    static {
        ID_TYPE.setTokenized(false);
        ID_TYPE.setStored(true);
        ID_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    }

    protected static final FieldType SORT_TYPE = new FieldType();
    static {
        SORT_TYPE.setTokenized(false);
        SORT_TYPE.setStored(false);
        SORT_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        SORT_TYPE.setDocValuesType(DocValuesType.SORTED);
    }

    protected static final FieldType RAW_TYPE = new FieldType();
    static {
        RAW_TYPE.setTokenized(false);
        RAW_TYPE.setStored(false);
        RAW_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    }

    protected static final FieldType TEXT_TYPE = new FieldType();
    static {
        TEXT_TYPE.setTokenized(true);
        TEXT_TYPE.setStored(true);
        TEXT_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    }

    protected static final FieldType SEARCH_TYPE = new FieldType();
    static {
        SEARCH_TYPE.setTokenized(true);
        SEARCH_TYPE.setStored(false);
        SEARCH_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    }

    /**
     * Build a new CSW metadata indexer.
     *
     * @param indexID The identifier, if there is one, of the index.
     * @param configDirectory The directory where the files of the index will be stored.
     * @param additionalQueryable A map of additional queryable elements.
     */
    public AbstractCSWIndexer(final String indexID, final Path configDirectory, final Map<String, PathType> additionalQueryable) {
        super(indexID, configDirectory);
        if (additionalQueryable != null) {
            this.additionalQueryable = additionalQueryable;
        } else {
            this.additionalQueryable = new HashMap<>();
        }
    }

    /**
     * Build a new CSW metadata indexer, with the specified lucene analyzer.
     *
     * @param indexID The identifier, if there is one, of the index.
     * @param configDirectory The directory where the files of the index will be stored.
     * @param analyzer A lucene analyzer used in text values indexation (default is ClassicAnalyzer).
     * @param additionalQueryable  A map of additional queryable elements.
     */
    public AbstractCSWIndexer(String indexID, Path configDirectory, Analyzer analyzer, Map<String, PathType> additionalQueryable) {
        super(indexID, configDirectory, analyzer);
        if (additionalQueryable != null) {
            this.additionalQueryable = additionalQueryable;
        } else {
            this.additionalQueryable = new HashMap<>();
        }
    }

    /**
    * Makes a document for a A Metadata Object.
    *
    * @param metadata The metadata to index.
    * @param docId the document identifier.
    * @return A Lucene document.
    */
    @Override
    protected Document createDocument(final A metadata, final int docId) throws IndexingException {
        // make a new, empty document
        final Document doc = new Document();
        doc.add(new Field("docid", docId + "", ID_TYPE));

        indexSpecialField(metadata, doc);

        final StringBuilder anyText     = new StringBuilder();
        boolean alreadySpatiallyIndexed = false;

        SpecificQueryablePart spe = getSpecificQueryableByType(metadata);
        if (spe.queryable != null) {
            final Map<String, PathType> speQueryable = removeOverridenField(spe.queryable);
            indexQueryableSet(doc, metadata, speQueryable, anyText);
            if (spe.spatial) {
                //we add the geometry parts
                alreadySpatiallyIndexed = indexSpatialPart(doc, metadata, speQueryable, CommonCRS.WGS84.normalizedGeographic());
            }
        }
        doc.add(new Field("objectType", spe.type, SEARCH_TYPE));

        // All metadata types must be compatible with dublinCore.
        final Map<String, PathType> dcQueryable = removeOverridenField(DUBLIN_CORE_QUERYABLE);
        indexQueryableSet(doc, metadata, dcQueryable, anyText);

        //we add the geometry parts if its nor already indexed
        if (!alreadySpatiallyIndexed) {
            try {
                CoordinateReferenceSystem crs = CRS.forCode("EPSG:4326");
                indexSpatialPart(doc, metadata, dcQueryable, crs);
            } catch (FactoryException e) {
                throw new IndexingException("Unable to decode EPSG:4326 CRS", e);
            }
        }

        // we add to the index the special queryable elements
        indexQueryableSet(doc, metadata, additionalQueryable, anyText);

        // add a default meta field to make searching all documents easy
        doc.add(new Field("metafile", "doc",SEARCH_TYPE));

        //we add the anyText values
        doc.add(new Field("AnyText", anyText.toString(),   SEARCH_TYPE));

        return doc;
    }

   /*
    * TODO move to super class
    */
    @Override
    public void indexDocuments(List<A> documents) {
        final IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (final IndexWriter writer = new IndexWriter(LuceneUtils.getAppropriateDirectory(getFileDirectory()), config)) {

            for (A doc : documents) {
                indexDocument(writer, doc);
            }
            if (rTree != null) {
                rTree.getTreeElementMapper().flush();
                rTree.flush();
            }

        } catch (StoreIndexException | IndexingException ex) {
            LOGGER.log(Level.WARNING, "Error while indexing single document", ex);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, IO_SINGLE_MSG + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean destroyIndex() throws IndexingException {
        final Path indexDirectory = getFileDirectory();
        boolean deleted = false;
        try {
            SQLRtreeManager.removeTree(indexDirectory);
            deleted = true;
        } catch (IOException |SQLException e) {
            throw new IndexingException("The index folder can't be deleted.", e);
        }
        return deleted;
    }


    /**
     * Remove the mapping of the specified Queryable set if it is overridden by one in the additional Queryable set.
     *
     * @param queryableSet
     */
    private Map<String, PathType> removeOverridenField(Map<String, PathType> queryableSet) {
        Map<String, PathType> result = new HashMap<>();
        for (Entry<String, PathType> entry : queryableSet.entrySet()) {
            if (!additionalQueryable.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Index the values for the specified Field
     *
     * @param values
     * @param fieldName
     * @param anyText
     * @param doc
     */
    protected void indexFields(final List<Object> values, final String fieldName, final StringBuilder anyText, final Document doc) {
        for (Object value : values) {
            if (value instanceof String) {
                indexField(fieldName, (String) value, anyText, doc);
            } else if (value instanceof Number) {
                indexNumericField(fieldName, (Number) value, doc);
            } else if (value != null){
                LOGGER.log(Level.WARNING, "unexpected type for field:{0}", value.getClass());
            }
        }
    }

    /**
     * Index a single String field.
     * Add this value to the anyText builder if its not equals to "null".
     *
     * @param fieldName
     * @param stringValue
     * @param anyText
     * @param doc
     */
    protected void indexField(final String fieldName, final String stringValue, final StringBuilder anyText, final Document doc) {
        final Field field        = new Field(fieldName, stringValue, SEARCH_TYPE);
        doc.add(field);

        final Field fieldRaw        = new Field(fieldName+"_raw", stringValue, RAW_TYPE);
        doc.add(fieldRaw);

        String sortName = fieldName + "_sort";
        if (doc.getField(sortName) == null) {
            //only one sorted field by name (multi sorted field with same name not allowed by Lucene 5.x.x)
            final Field fieldSort    = new Field(sortName, new BytesRef(stringValue.getBytes()), SORT_TYPE);
            doc.add(fieldSort);
        }

        if (!stringValue.equals(NULL_VALUE) && anyText.indexOf(stringValue) == -1) {
            anyText.append(stringValue).append(" ");
        }
    }

    /**
     * Inex a numeric field.
     *
     * @param fieldName
     * @param numValue
     * @param doc
     */
    protected void indexNumericField(final String fieldName, final Number numValue, final Document doc) {

        final Field numField;
        final Field numSortField;
        final Character fieldType;

        final FieldType numericType = new FieldType(SORT_TYPE);
        numericType.setDocValuesType(DocValuesType.SORTED_NUMERIC);
        if (numValue instanceof Integer) {
            numericType.setNumericType(FieldType.NumericType.INT);
            numField     = new IntField(fieldName,           (Integer) numValue, Field.Store.NO);
            numSortField = new IntField(fieldName + "_sort", (Integer) numValue, numericType);
            fieldType = 'i';
        } else if (numValue instanceof Double) {
            numericType.setNumericType(FieldType.NumericType.DOUBLE);
            numField     = new DoubleField(fieldName,           (Double) numValue, Field.Store.NO);
            numSortField = new DoubleField(fieldName + "_sort", (Double) numValue, numericType);
            fieldType = 'd';
        } else if (numValue instanceof Float) {
            numericType.setNumericType(FieldType.NumericType.FLOAT);
            numField     = new FloatField(fieldName,           (Float) numValue, Field.Store.NO);
            numSortField = new FloatField(fieldName + "_sort", (Float) numValue, numericType);
            fieldType = 'f';
        } else if (numValue instanceof Long) {
            numericType.setNumericType(FieldType.NumericType.LONG);
            numField     = new LongField(fieldName,           (Long) numValue, Field.Store.NO);
            numSortField = new LongField(fieldName + "_sort", (Long) numValue, numericType);
            fieldType = 'l';
        } else {
            numField     = new StringField(fieldName,           String.valueOf(numValue), Field.Store.NO);
            numSortField = new StringField(fieldName + "_sort", String.valueOf(numValue), Field.Store.NO);
            fieldType = 'u';
            if (numValue != null) {
                LOGGER.log(Level.WARNING, "Unexpected Number type:{0}", numValue.getClass().getName());
            }
        }
        addNumericField(fieldName, fieldType);
        addNumericField(fieldName + "_sort", fieldType);
        doc.add(numField);
        doc.add(numSortField);
    }

    /**
     * Add the specifics implementation field to the document.
     *
     * @param metadata The metadata to index.
     * @param doc The lucene document currently building.
     * @throws IndexingException
     */
    protected abstract void indexSpecialField(final A metadata, final Document doc) throws IndexingException;

    /**
     * Return a String description of the type of the metadata.
     *
     * @param metadata The metadata currently indexed
     * @return A string description (name of the class, name of the top value type, ...)
     */
    protected abstract String getType(final A metadata);

    /**
     * Index a set of properties contained in the queryableSet.
     *
     * @param doc The lucene document currently building.
     * @param metadata The metadata to index.
     * @param queryableSet A set of queryable properties and their relative path in the metadata.
     * @param anyText A {@link StringBuilder} in which are concatened all the text values.
     * @throws IndexingException
     */
    protected abstract void indexQueryableSet(final Document doc, final A metadata, Map<String, PathType> queryableSet, final StringBuilder anyText) throws IndexingException;

    /**
     * Spatially index the form extracting the BBOX values with the specified queryable set.
     *
     * @param doc The current Lucene document.
     * @param form The metadata records to spatially index.
     * @param queryableSet A set of queryable Term.
     * @param crs the coordinate reference system
     *
     * @return true if the indexation succeed
     * @throws IndexingException
     */
    private boolean indexSpatialPart(Document doc, A form, Map<String, PathType> queryableSet, CoordinateReferenceSystem crs) throws IndexingException {

        final List<Double> minxs = extractPositions(form, queryableSet.get("WestBoundLongitude"));
        final List<Double> maxxs = extractPositions(form, queryableSet.get("EastBoundLongitude"));
        final List<Double> maxys = extractPositions(form, queryableSet.get("NorthBoundLatitude"));
        final List<Double> minys = extractPositions(form, queryableSet.get("SouthBoundLatitude"));
        try {
            if (minxs.size() == minys.size() && minys.size() == maxxs.size() && maxxs.size() == maxys.size()) {
                addBoundingBox(doc, minxs, maxxs, minys, maxys, crs);
                return true;
            } else {
                LOGGER.log(Level.WARNING,NOT_SPATIALLY_INDEXABLE + "{0}\n cause: missing coordinates.", getIdentifier(form));
            }
        } catch (NullArgumentException ex) {
            throw new IndexingException("error while spatially indexing:" + doc.get("id"), ex);
        }
        return false;
    }

     /**
      * Extract the double coordinate from a metadata object using a list of paths to find the data.
      *
      * @param metadata The metadata to spatially index.
      * @param paths A list of paths where to find the information within the metadata.
      * @return A list of Double coordinates.
      *
      * @throws IndexingException
      */
    private List<Double> extractPositions(A metadata, PathType paths) throws IndexingException {
        final List<Object> coord = getValues(metadata, paths);
        final List<Double> coordinate = new ArrayList<>();
        try {
            for (Object obj : coord) {
                if (obj instanceof Double) {
                    coordinate.add((Double)obj);
                } else if (obj instanceof Integer) {
                    coordinate.add(((Integer)obj).doubleValue());
                } else {
                    coordinate.add(Double.parseDouble(String.valueOf(obj)));
                }
            }
        } catch (NumberFormatException e) {
            if (!coord.equals(NULL_VALUE)) {
                LOGGER.warning(NOT_SPATIALLY_INDEXABLE + getIdentifier(metadata) +
                        "\ncause: unable to parse double: " + coord);
            }
        }
        return coordinate;
    }

    @Override
    protected Iterator<A> getEntryIterator() throws IndexingException {
        throw new UnsupportedOperationException("Not supported by this implementation");
    }

    @Override
    protected boolean useEntryIterator() {
        return false;
    }

    /**
     * Extract some values from a metadata object using  the list of paths.
     *
     * @param meta The object to index.
     * @param paths A list of paths where to find the information within the metadata, along with the expected type.
     *
     * @return A List of extracted values found in the metadata.
     * @throws IndexingException
     */
    protected abstract List<Object> getValues(final A meta, final PathType paths) throws IndexingException;


    /**
     * Return a specific set of queryable dependeing on the metadata type.
     * Return also the metadata type;
     *
     * @param meta The object to index.
     */
    protected abstract SpecificQueryablePart getSpecificQueryableByType(A meta);

}
