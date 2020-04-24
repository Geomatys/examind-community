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

package org.constellation.metadata.index.elasticsearch;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.NullArgumentException;
import org.apache.sis.util.logging.Logging;
import static org.constellation.api.CommonConstants.NULL_VALUE;
import org.constellation.api.PathType;
import static org.constellation.metadata.CSWQueryable.DUBLIN_CORE_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_FC_QUERYABLE;
import static org.constellation.metadata.CSWQueryable.ISO_QUERYABLE;
import org.constellation.metadata.index.Indexer;
import org.elasticsearch.ElasticsearchException;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.geotoolkit.index.IndexingException;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.util.collection.CloseableIterator;
import org.opengis.geometry.Envelope;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class ElasticSearchIndexer<E> implements Indexer<E> {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.metadata.index.elasticsearch.ElasticSearchIndexer");

    protected final ElasticSearchClient client;

    private final String hostName;

    private final String clusterName;

    private final Map<String, PathType> additionalQueryable;

    private final String indexName;

    protected final MetadataStore store;

    private boolean withPlugin = false;

    /**
     * A flag to stop the indexation going on
     */
    protected static boolean stopIndexing = false;

    /**
     * A list of services id
     */
    protected static final List<String> indexationToStop = new ArrayList<>();

    public ElasticSearchIndexer(final MetadataStore store, final String host, final String clusterName, final String indexName,
            final Map<String, PathType> additionalQueryable) throws IndexingException {

        this.clusterName         = clusterName;
        this.hostName            = host;
        this.indexName           = indexName.toLowerCase();
        this.additionalQueryable = additionalQueryable;
        this.store              = store;
        try {
            this.client      = ElasticSearchClient.getClientInstance(host, clusterName);
        } catch (UnknownHostException | ElasticsearchException  ex) {
            throw new IndexingException("error while connecting ELasticSearch cluster", ex);
        }
    }

    @Override
    public boolean needCreation() {
        try {
            return !client.indexExist(indexName);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        return false;
    }


    private void createMapping() throws IOException {
        Map<String, Class> fields = new HashMap<>();

        fields.put("id", String.class);
        fields.put("objectType", String.class);
        fields.put("metafile",  String.class);
        fields.put("AnyText",  String.class);

        for (Entry<String, PathType> entry : ISO_QUERYABLE.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().type);
        }
        for (Entry<String, PathType> entry : ISO_FC_QUERYABLE.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().type);
        }
        for (Entry<String, PathType> entry : DUBLIN_CORE_QUERYABLE.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().type);
        }
        for (Entry<String, PathType> entry : additionalQueryable.entrySet()) {
            fields.put(entry.getKey(), entry.getValue().type);
        }
        // add spatial part
        fields.put("geoextent", Envelope.class);
        client.prepareType(indexName, fields, withPlugin);
    }

    /**
     * Create a new Index with the specified list of object.
     *
     * @param toIndex objects to index.
     * @throws IndexingException
     */
    public void createIndex(final List<E> toIndex) throws IndexingException {
        LOGGER.log(Level.INFO, "Creating ElasticSearch index for please wait...");

        final long time     = System.currentTimeMillis();
        final int nbEntries = toIndex.size();
        try {
            createMapping();

            for (E entry : toIndex) {
                if (!stopIndexing && !indexationToStop.contains(indexName)) {
                    indexDocument(entry);
                } else {
                     LOGGER.info("Index creation stopped after " + (System.currentTimeMillis() - time) + " ms for service:" + indexName);
                     stopIndexation(client, indexName);
                     return;
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE,"error while indexing: ", ex.getMessage());
            throw new IndexingException("IOException while indexing documents:" + ex.getMessage(), ex);
        }
        LOGGER.log(Level.INFO, "Index creation process in " + (System.currentTimeMillis() - time) + " ms\n" +
                " documents indexed: " + nbEntries);
    }

    @Override
    public void createIndex() throws IndexingException {
        LOGGER.log(Level.INFO, "(light memory) Creating ElasticSearch index please wait...");
        final long time  = System.currentTimeMillis();
        int nbEntries      = 0;
        try {
            LOGGER.log(Level.INFO, "starting indexing...");
            createMapping();

            if (store.getReader().useEntryIterator()) {
                final Iterator<E> entries = (Iterator<E>) store.getReader().getEntryIterator();
                while (entries.hasNext()) {
                    if (!stopIndexing && !indexationToStop.contains(indexName)) {

                        final E entry = entries.next();
                        indexDocument(entry);
                        nbEntries++;

                    } else {
                         LOGGER.info("Index creation stopped after " + (System.currentTimeMillis() - time) + " ms for service:" + indexName);
                         stopIndexation(client, indexName);
                         return;
                    }
                }
                if (entries instanceof CloseableIterator) {
                    ((CloseableIterator)entries).close();
                }
            } else {
                final Iterator<String> identifiers = store.getIdentifierIterator();
                while (identifiers.hasNext()) {
                    final String identifier = identifiers.next();
                    if (!stopIndexing && !indexationToStop.contains(indexName)) {
                        try {
                            final E entry = getEntry(identifier);
                            indexDocument(entry);
                            nbEntries++;
                        } catch (IndexingException ex) {
                            LOGGER.warning("Metadata IO exeption while indexing metadata: " + identifier + " " + ex.getMessage() + "\nmove to next metadata...");
                        }
                    } else {
                         LOGGER.info("Index creation stopped after " + (System.currentTimeMillis() - time) + " ms for service:" + indexName);
                         stopIndexation(client, indexName);
                         return;
                    }
                }
                if (identifiers instanceof CloseableIterator) {
                    ((CloseableIterator)identifiers).close();
                }
            }

        } catch (MetadataIoException | IOException ex) {
            LOGGER.log(Level.SEVERE,"error while indexing: ", ex.getMessage());
            throw new IndexingException("IOException while indexing documents:" + ex.getMessage(), ex);
        }
        LOGGER.log(Level.INFO, "Index creation process in " + (System.currentTimeMillis() - time) + " ms\n documents indexed: " + nbEntries + ".");
    }

    @Override
    public boolean destroyIndex() throws IndexingException {
        try {
            return client.deleteIndex(indexName);
        } catch (IOException ex) {
            throw new IndexingException("Error while destroying index", ex);
        }
    }


    @Override
    public void indexDocument(E metadata) {
        try {
            String id = getIdentifier(metadata);
            client.indexDoc(indexName, id, createDocument(metadata));
            LOGGER.log(Level.FINER, "Metadata: {0} indexed", id);

        } catch (IndexingException | IOException ex) {
            LOGGER.log(Level.WARNING, "Error while indexing single document", ex);
        }
    }

    /*
    * TODO move to super class
    */
    @Override
    public void indexDocuments(List<E> documents) {
        for (E doc : documents) {
            indexDocument(doc);
        }
    }

    @Override
    public void removeDocument(String id) {
        try {
            client.removeDoc(indexName, id);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while removing single document", ex);
        }
    }

    protected abstract String getIdentifier(E metadata);

    /**
     * Add the specifics implementation field to the document.
     *
     * @param metadata The metadata to index.
     * @param doc The lucene document currently building.
     * @throws IndexingException
     */
    protected abstract void indexSpecialField(final E metadata, final Map doc) throws IndexingException;

    /**
     * Index a set of properties contained in the queryableSet.
     *
     * @param doc The lucene document currently building.
     * @param metadata The metadata to index.
     * @param queryableSet A set of queryable properties and their relative path in the metadata.
     * @param anyText A {@link StringBuilder} in which are concatened all the text values.
     * @throws IndexingException
     */
    protected abstract void indexQueryableSet(final Map doc, final E metadata, Map<String, PathType> queryableSet, final StringBuilder anyText) throws IndexingException;

    /**
     * Return true if the metadata object is a ISO19139 object.
     *
     * @param meta The object to index
     * @return true if the metadata object is a ISO19139 object.
     */
    protected abstract boolean isISO19139(E meta);

    /**
     * Return true if the metadata object is a DublinCore object.
     *
     * @param meta The object to index
     * @return true if the metadata object is a DublinCore object.
     */
    protected abstract boolean isDublinCore(E meta);

    /**
     * Return true if the metadata object is a Ebrim version 2.5 object.
     *
     * @param meta The object to index
     * @return true if the metadata object is a Ebrim version 2.5 object.
     */
    protected abstract boolean isEbrim25(E meta);

    /**
     * Return true if the metadata object is a Ebrim version 3.0 object.
     *
     * @param meta The object to index
     * @return true if the metadata object is a Ebrim version 3.0 object.
     */
    protected abstract boolean isEbrim30(E meta);

    /**
     * Return true if the metadata object is a FeatureCatalogue object.
     *
     * @param meta The object to index
     * @return true if the metadata object is a FeatureCatalogue object.
     */
    protected abstract boolean isFeatureCatalogue(E meta);

    /**
     * Return a String description of the type of the metadata.
     *
     * @param metadata The metadata currently indexed
     * @return A string description (name of the class, name of the top value type, ...)
     */
    protected abstract String getType(final E metadata);

    /**
     * Extract some values from a metadata object using  the list of paths.
     *
     * @param meta The object to index.
     * @param paths A list of paths where to find the information within the metadata, along with the expected type.
     *
     * @return A List of extracted values found in the metadata.
     * @throws IndexingException
     */
    protected abstract List<Object> getValues(final E meta, final PathType paths) throws IndexingException;

    protected Map createDocument(E metadata) throws IndexingException {
        // make a new, empty document
        final Map doc = new HashMap();

        indexSpecialField(metadata, doc);

        final StringBuilder anyText     = new StringBuilder();
        boolean alreadySpatiallyIndexed = false;

        // For an ISO 19139 object
        final String type;
        if (isISO19139(metadata)) {
            final Map<String, PathType> isoQueryable = removeOverridenField(ISO_QUERYABLE);
            indexQueryableSet(doc, metadata, isoQueryable, anyText);

            //we add the geometry parts
            alreadySpatiallyIndexed = indexSpatialPart(doc, metadata, isoQueryable, "CRS:84");

            type = "MD_Metadata";

        } else if (isEbrim30(metadata)) {
           // TODO
            type = "Ebrim";
        } else if (isEbrim25(metadata)) {
            // TODO
            type = "Ebrim";
        } else if (isFeatureCatalogue(metadata)) {
            final Map<String, PathType> fcQueryable = removeOverridenField(ISO_FC_QUERYABLE);
            indexQueryableSet(doc, metadata, fcQueryable, anyText);

            type = "FC_FeatureCatalogue";
        } else if (isDublinCore(metadata)) {

            type = "Record";
        } else {
            type = "undefined";
            LOGGER.log(Level.WARNING, "unknow Object classe unable to index: {0}", getType(metadata));
        }

        doc.put("objectType", type);
        doc.put("objectType_sort", type);

        // All metadata types must be compatible with dublinCore.
        final Map<String, PathType> dcQueryable = removeOverridenField(DUBLIN_CORE_QUERYABLE);
        indexQueryableSet(doc, metadata, dcQueryable, anyText);

        //we add the geometry parts if its nor already indexed
        if (!alreadySpatiallyIndexed) {

            indexSpatialPart(doc, metadata, dcQueryable, "EPSG:4326");
        }

        // we add to the index the special queryable elements
        indexQueryableSet(doc, metadata, additionalQueryable, anyText);

        // add a default meta field to make searching all documents easy
        doc.put("metafile", "doc");

        //we add the anyText values
        doc.put("AnyText", anyText.toString());

        LOGGER.log(Level.FINER, "indexing doc:\n{0}", doc.toString());
        return doc;
    }

    private E getEntry(final String identifier) throws IndexingException {
        try {
            return (E) store.getMetadata(identifier, MetadataType.NATIVE);
        } catch (MetadataIoException ex) {
            throw new IndexingException("Metadata_IOException while reading entry for:" + identifier, ex);
        }
    }

    /**
     * This method stop all the current indexation running
     */
    public static void stopIndexation() {
        stopIndexing = true;
    }

    private void stopIndexation(final ElasticSearchClient client, final String serviceID) throws IOException {
        client.deleteIndex(indexName);
        if (indexationToStop.contains(serviceID)) {
            indexationToStop.remove(serviceID);
        }
        if (indexationToStop.isEmpty()) {
            stopIndexing = false;
        }
    }

    /**
     * Remove the mapping of the specified Queryable set if it is overridden by one in the additional Queryable set.
     *
     * @param queryableSet
     */
    protected Map<String, PathType> removeOverridenField(Map<String, PathType> queryableSet) {
        Map<String, PathType> result = new HashMap<>();
        for (Map.Entry<String, PathType> entry : queryableSet.entrySet()) {
            if (!additionalQueryable.containsKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Spatially index the form extracting the BBOX values with the specified queryable set.
     *
     * @param doc The current Lucene document.
     * @param form The metadata records to spatially index.
     * @param queryableSet A set of queryable Term.
     * @param srid the coordinate reference system SRID
     *
     * @return true if the indexation succeed
     * @throws MD_IOException
     */
    private boolean indexSpatialPart(Map doc, E form, Map<String, PathType> queryableSet, String CRScode) throws IndexingException {

        final List<Double> minxs = extractPositions(form, queryableSet.get("WestBoundLongitude"));
        final List<Double> maxxs = extractPositions(form, queryableSet.get("EastBoundLongitude"));
        final List<Double> maxys = extractPositions(form, queryableSet.get("NorthBoundLatitude"));
        final List<Double> minys = extractPositions(form, queryableSet.get("SouthBoundLatitude"));
        try {
            if (minxs.size() == minys.size() && minys.size() == maxxs.size() && maxxs.size() == maxys.size()) {
                System.out.println(doc.get("id"));
                doc.putAll(generateMapEnvelopes("geoextent", CRScode, minxs, maxxs, minys, maxys));
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Unable to spatially index metadata: {0}\n cause: missing coordinates.", getIdentifier(form));
            }
        } catch (NullArgumentException ex) {
            throw new IndexingException("error while spatially indexing:" + doc.get("id"), ex);
        }
        return false;
    }

    protected Map generateMapEnvelopes(final String spatialAttribute, final String CRSNameCode, final List<Double> minx, final List<Double> maxx, final List<Double> miny, final List<Double> maxy) {

        final int nbEnv = minx.size();
        final Map map   = new HashMap();
        final List<Map> envelopeList = new ArrayList<>();

        for (int e = 0; e < nbEnv; e++) {
            if (!Double.isNaN(minx.get(e)) && !Double.isNaN(maxx.get(e)) && !Double.isNaN(miny.get(e)) && !Double.isNaN(maxy.get(e))) {
                Map boxMap = new HashMap();
                if (withPlugin) {
                    boxMap.put("minx", ""+ minx.get(e));
                    boxMap.put("maxx", ""+ maxx.get(e));
                    boxMap.put("miny", ""+ miny.get(e));
                    boxMap.put("maxy", ""+ maxy.get(e));
                    boxMap.put("crs",  CRSNameCode);
                } else {
                    // TODO reproj for bas CRS

                    // Elasticsearch supports an envelope type, which consists of coordinates
                    // for upper left and lower right points of the shape to represent a bounding rectangle
                    // in the format [[minLon, maxLat], [maxLon, minLat]]:

                    // Elasticsearch do not like "line/point" bbox
                    double delta = 0.01;
                    double ix = minx.get(e);
                    double ax = maxx.get(e);
                    double iy = miny.get(e);
                    double ay = maxy.get(e);

                    if (ix == ax) {
                        ax = ax + delta;
                    }
                    if (iy == ay) {
                        ay = ay + delta;
                    }

                    boxMap.put("coordinates", Arrays.asList(Arrays.asList(ix, ay), Arrays.asList(ax, iy)));
                    boxMap.put("type", "envelope");
                    System.out.println("PUT: " + "[[" + ix + ',' + ay + "],[" + ax + ',' + iy + "]]");
                }
                envelopeList.add(boxMap);
            } else {
                LOGGER.warning("Nan coordinates in bbox");
            }
        }
        map.put(spatialAttribute, envelopeList);
        return map;
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
    private List<Double> extractPositions(E metadata, PathType paths) throws IndexingException {
        final List<Object> coord     = getValues(metadata, paths);
        final List<Double> coordinate = new ArrayList<>();
        Object current = null;
        try {
            for (Object obj : coord) {
                current = obj;
                if (obj instanceof Double) {
                    coordinate.add((Double)obj);
                } else if (obj instanceof Integer) {
                    coordinate.add(((Integer)obj).doubleValue());
                } else {
                    coordinate.add(Double.parseDouble(String.valueOf(obj)));
                }
            }
        } catch (NumberFormatException e) {
            if (current != null && !NULL_VALUE.equals(String.valueOf(current))) {
                LOGGER.warning("Unable to spatially index metadata: " + getIdentifier(metadata) +
                               "\ncause: unable to parse double: " + current);
            }
        }
        return coordinate;
    }

    @Override
    public String getTreeRepresentation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Map getMapperContent() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setFileDirectory(Path aFileDirectory) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void removeIndex() throws IOException {
        client.deleteIndex(indexName);
    }

    @Override
    public void destroy() {
        ElasticSearchClient.releaseClientInstance(hostName, clusterName);
    }

    public boolean isWithPlugin() {
        return withPlugin;
    }

    public void setWithPlugin(boolean withPlugin) {
        this.withPlugin = withPlugin;
    }
}
