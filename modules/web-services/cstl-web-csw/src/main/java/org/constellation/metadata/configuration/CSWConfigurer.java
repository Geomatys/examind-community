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

package org.constellation.metadata.configuration;

import org.constellation.dto.service.config.csw.BriefNode;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.DataSourceType;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.constellation.ogc.configuration.OGCConfigurer;
import org.geotoolkit.lucene.index.AbstractIndexer;
import org.geotoolkit.nio.ZipUtilities;
import org.constellation.metadata.index.Indexer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.constellation.ws.ICSWConfigurer;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.metadata.core.IndexConfigHandler;
import org.constellation.metadata.core.MetadataStoreWrapper;
import org.constellation.metadata.utils.Utils;
import org.constellation.provider.DataProviders;
import org.constellation.store.metadata.filesystem.FileSystemMetadataStore;
import org.constellation.util.NodeUtilities;
import org.geotoolkit.metadata.MetadataStore;
import org.geotoolkit.index.tree.manager.NamedEnvelope;
import org.geotoolkit.index.tree.manager.SQLRtreeManager;
import org.geotoolkit.lucene.index.IndexLucene;
import org.geotoolkit.metadata.RecordInfo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link OGCConfigurer} implementation for CSW service.
 *
 * TODO: implement specific configuration methods
 *
 * @author Fabien Bernard (Geomatys).
 * @author Cédric Briançon (Geomatys)
 * @version 0.9
 * @since 0.9
 */
public class CSWConfigurer extends OGCConfigurer implements ICSWConfigurer {

    /**
     * A flag indicating if an indexation is going on.
     */
    private boolean indexing;

    /**
     * The list of service currently indexing.
     */
    private final List<String> SERVICE_INDEXING = new ArrayList<>();

    @Autowired
    @Qualifier(value = "indexConfigHandler")
    protected IndexConfigHandler indexHandler;

    @Autowired
    protected IMetadataBusiness metadataBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    /**
     * Create a new {@link CSWConfigurer} instance.
     */
    public CSWConfigurer() {
        indexing = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean refreshIndex(final String id, final boolean asynchrone, final boolean forced) throws ConstellationException {
        if (isIndexing(id) && !forced) {
            throw new ConfigurationException( "An indexation is already started for this service.");
        } else if (indexing && forced) {
            AbstractIndexer.stopIndexation(Arrays.asList(id));
        }

        startIndexation(id);
        try {
            return refreshIndex(asynchrone, id);
        } finally {
            endIndexation(id);
        }
    }

    /**
     * Rebuild the CSW index in order that it will be recreated.
     *
     * @param asynchrone a flag for indexation mode.
     * @param id The service identifier.
     *
     * @return {@code true} if the indexation succeed.
     * 
     * @throws TargetNotFoundException If the csw service does not exist.
     * @throws ConstellationException If a problem occurs during the indexation.
     */
    private boolean refreshIndex(final boolean asynchrone, final String id) throws ConstellationException {
        String suffix = "";
        if (asynchrone) {
            suffix = " (asynchrone)";
        }
        if (id != null && !id.isEmpty()) {
            suffix = suffix + " id:" + id;
        }
        LOGGER.log(Level.INFO, "refresh index requested{0}", suffix);

        final List<String> cswInstances;
        if ("all".equals(id)) {
            cswInstances = serviceBusiness.getServiceIdentifiers("csw");
        } else {
            if (serviceBusiness.getServiceIdByIdentifierAndType("csw", id) != null) {
                cswInstances = Arrays.asList(id);
            } else {
                throw new TargetNotFoundException("there is no service " + id);
            }
        }
        if (!asynchrone) {
            synchroneIndexRefresh(cswInstances, true);
        } else {
            asynchroneIndexRefresh(cswInstances);
        }
        return true;
    }

    /**
     * Add the specified service to the indexing service list.
     * @param id
     */
    private void startIndexation(final String id) {
        indexing  = true;
        if (id != null) {
            SERVICE_INDEXING.add(id);
        }
    }

    /**
     * remove the selected service from the indexing service list.
     * @param id
     */
    private void endIndexation(final String id) {
        indexing = false;
        if (id != null) {
            SERVICE_INDEXING.remove(id);
        }
    }

    /**
     * Return true if the select service (identified by his ID) is currently indexing (CSW).
     * @param id
     * @return
     */
    private boolean isIndexing(final String id) {
        return indexing && SERVICE_INDEXING.contains(id);
    }

    public boolean isIndexing() {
        return indexing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addToIndex(final String id, final List<String> identifierList) throws ConstellationException {
        if (identifierList.isEmpty()) {
            return false;
        }
        LOGGER.fine("Add to index requested");

        final MetadataStoreWrapper store = getMetadataStore(id);
        if (store != null) {

            //hack for FS CSW
            reloadFSProvider(store);

            String requestUUID = UUID.randomUUID().toString();
            Indexer indexer = getIndexer(id, store, requestUUID);
            if (indexer != null) {
                try {
                    for (String identifier : identifierList) {
                        LOGGER.fine("Adding record:" + identifier + " to index");
                        final RecordInfo obj = store.getMetadataFromOriginalStore(identifier, MetadataType.NATIVE)
                                .orElseThrow(() -> new ConfigurationException("Unable to find the metadata: " + identifier));
                        synchronized(indexer) {
                            indexer.indexDocument(obj.node);
                        }
                    }
                } catch (MetadataIoException ex) {
                    throw new ConfigurationException(ex);
                } finally {
                    destroyIndexer(id, requestUUID);
                }
            } else {
                throw new ConfigurationException("Unable to create an indexer for the id:" + id);
            }
        } else {
            throw new ConfigurationException("Unable to get a store for the id:" + id);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeFromIndex(final String id, final List<String> identifierList) throws ConfigurationException {
        if (identifierList.isEmpty()) {
            return false;
        }
        LOGGER.finer("Remove from index requested");

        final MetadataStore store = getMetadataStore(id);
        String requestUUID = UUID.randomUUID().toString();
        Indexer indexer = getIndexer(id, store, requestUUID);
        if (indexer != null) {
            try {
                for (String metadataID : identifierList) {
                    synchronized(indexer) {
                        indexer.removeDocument(metadataID);
                    }
                }
            } finally {
                destroyIndexer(id, requestUUID);
            }
        } else {
            throw new ConfigurationException("Unable to create an indexer for the id:" + id);
        }
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean stopIndexation(final String id) {
        LOGGER.info("\n stop indexation requested \n");
        if (!isIndexing(id)) {
            return true;
        } 
        AbstractIndexer.stopIndexation(Arrays.asList(id));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importRecords(final String id, final Path f, final String fileName) throws ConstellationException {
        LOGGER.finer("Importing record");
        final List<Path> files;
        if (fileName.endsWith("zip")) {
            try  {
                files = ZipUtilities.unzip(f, null);
            } catch (IOException ex) {
                throw new ConfigurationException(ex);
            }
        } else if (fileName.endsWith("xml")) {
            files = Arrays.asList(f);
        } else {
            throw new ConfigurationException("Unexpected file extension, accepting zip or xml");
        }
        try {
            for (Path importedFile: files) {
                if (importedFile != null) {
                    final Node n = NodeUtilities.getNodeFromPath(importedFile);
                    String metadataID = Utils.findIdentifierNode(n);
                    metadataBusiness.updateMetadata(metadataID, n, null, null, null, null, getProviderID(id), "DOC");
                    metadataBusiness.linkMetadataIDToCSW(metadataID, id);

                } else {
                    throw new ConfigurationException("An imported file is null");
                }
            }
            return true;
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            LOGGER.log(Level.WARNING, "Exception while unmarshalling imported file", ex);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importRecord(String id, String metadataId) throws ConstellationException {
        if (metadataBusiness.existInternalMetadata(metadataId, true, false, null)) {
            metadataBusiness.linkMetadataIDToCSW(metadataId, id);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importRecords(String id, Collection<String> metadataIds) throws ConstellationException {
        metadataBusiness.linkMetadataIDsToCSW(new ArrayList<>(metadataIds), id);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean importRecord(final String id, final Node n) throws ConstellationException {
        final int providerID = getProviderID(id);
        String metadataID = Utils.findIdentifierNode(n);
        metadataBusiness.updateMetadata(metadataID, n, null, null, null, null, providerID, "DOC");
        metadataBusiness.linkMetadataIDToCSW(metadataID, id);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeRecords(final String id, final String metadataId) throws ConstellationException {
        if (metadataBusiness.isLinkedMetadataToCSW(metadataId, id)) {
            metadataBusiness.unlinkMetadataIDToCSW(metadataId, id);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAllRecords(final String id) throws ConstellationException {
        final MetadataStore store = getMetadataStore(id);
        try {
            final List<String> metas = store.getAllIdentifiers();
            for (String meta : metas) {
                metadataBusiness.unlinkMetadataIDToCSW(meta, id);
            }
        } catch (MetadataIoException ex) {
            throw new ConfigurationException(ex);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean metadataExist(final String id, final String metadataID) throws ConfigurationException {
        return metadataBusiness.isLinkedMetadataToCSW(metadataID, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BriefNode> getMetadataList(final String id, final int count, final int startIndex) throws ConfigurationException {
        final Automatic config        = getServiceConfiguration(id);
        final MetadataStore store     = getMetadataStore(id);
        final List<BriefNode> results = new ArrayList<>();
        try {
            final List<String> ids = store.getAllIdentifiers();
            if (startIndex >= ids.size()) {
                return results;
            }
            final Map<String , List<String>> fieldMap = indexHandler.getBriefFieldMap(config);
            for (int i = startIndex; i<ids.size() && i<startIndex + count; i++) {
                RecordInfo rec = store.getMetadata(ids.get(i),  MetadataType.NATIVE);
                if (rec != null) {
                    results.add(createBriefNode(rec.node, fieldMap));
                }
            }
        } catch (MetadataIoException ex) {
            throw new ConfigurationException(ex);
        }
        return results;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Node> getFullMetadataList(final String id, final int count, final int startIndex, String type) throws ConfigurationException {
        final MetadataStore store = getMetadataStore(id);
        MetadataType metaType = MetadataType.valueOf(type);
        try {
            final List<Node> results = new ArrayList<>();
            final List<String> ids = store.getAllIdentifiers();
            if (startIndex >= ids.size()) {
                return results;
            }

            for (int i = startIndex; i<ids.size() && i<startIndex + count; i++) {
                RecordInfo rec = store.getMetadata(ids.get(i), metaType);
                if (rec != null) {
                    results.add(rec.node);
                }
            }
            return results;
        } catch (MetadataIoException ex) {
            throw new ConfigurationException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getMetadata(final String id, final String metadataID) throws ConstellationException {
        return metadataBusiness.getMetadataNode(metadataID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMetadataCount(final String id) throws ConfigurationException {
        final MetadataStore store = getMetadataStore(id);
        if (store != null) {
            try {
                return store.getEntryCount();
            } catch (MetadataIoException ex) {
                throw new ConfigurationException(ex);
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAvailableCSWDataSourceType() {
        final List<DataSourceType> sources = indexHandler.getAvailableDatastourceType();
        final List<String> result = new ArrayList<>();
        for (DataSourceType source : sources) {
            result.add(source.getName());
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeIndex(String id) throws ConfigurationException {
        try {
            String requestUUID = UUID.randomUUID().toString();
            MetadataStore store = getMetadataStore(id);
            IndexLucene indexer = (IndexLucene) getIndexer(id, store, requestUUID);
            destroyIndexer(id, requestUUID);
            SQLRtreeManager.removeTree(indexer.getFileDirectory());
        } catch (ConfigurationException | SQLException | IOException ex) {
           throw new ConfigurationException(ex);
        }
        return true;
    }

    /**
     * Refresh the map of configuration object.
     *
     * @param id identifier of the CSW service.
     * @return
     * @throws ConfigurationException
     */
    protected Automatic getServiceConfiguration(final String id) throws ConfigurationException {
        try {
            // we get the CSW configuration file
            return (Automatic) serviceBusiness.getConfiguration("csw", id);
        } catch (ConfigurationException ex) {
            throw new ConfigurationException("Configuration exception while getting the CSW configuration for:" + id, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ConfigurationException("IllegalArgumentException: " + ex.getMessage());
        }
    }

    /**
     * Delete The index folder and call the restart() method.
     *
     * TODO maybe we can directly recreate the index here (fusion of synchrone/asynchrone)
     *
     * @param configurationDirectory The CSW configuration directory.
     *
     * @throws ConstellationException if something went wrong during the indexation.
     */
    private void synchroneIndexRefresh(final List<String> cswInstances, final boolean reloadFSStore) throws ConstellationException {
        String requestUUID = UUID.randomUUID().toString();
        for (String cswInstance : cswInstances) {
            final Automatic config = getServiceConfiguration(cswInstance);
            final MetadataStoreWrapper store = getMetadataStore(cswInstance);
            final Indexer indexer  = getIndexer(cswInstance, store, requestUUID);
            try {
                indexHandler.refreshIndex(config, cswInstance, indexer, false);
            } finally {
                destroyIndexer(cswInstance, requestUUID);
            }

            //hack for FS CSW
            if (reloadFSStore) {
                reloadFSProvider(store);
            }
        }
    }

    /**
     * Build a new Index in a new folder.
     * This index will be used at the next restart of the server.
     *
     * @param id The service identifier.
     * @param configurationDirectory  The CSW configuration directory.
     *
     * @throws ConfigurationException if something went wrong during the indexation.
     */
    private void asynchroneIndexRefresh(final List<String> cswInstances) throws ConfigurationException {
        String requestUUID = UUID.randomUUID().toString();
        for (String cswInstance : cswInstances) {
            final Automatic config = getServiceConfiguration(cswInstance);
            final MetadataStoreWrapper store = getMetadataStore(cswInstance);
            Indexer indexer = getIndexer(cswInstance, store, requestUUID);
            try {
                indexHandler.refreshIndex(config, cswInstance, indexer, true);

            } catch (ConstellationException ex) {
                throw new ConfigurationException("An exception occurs while creating the index!\ncause:" + ex.getMessage());
            } finally {
                if (indexer != null) {
                    destroyIndexer(cswInstance, requestUUID);
                }
            }
        }
    }

    public Map<Integer, NamedEnvelope> getMapperContent(String serviceID) throws ConfigurationException {
        String requestUUID = UUID.randomUUID().toString();
        final MetadataStoreWrapper store = getMetadataStore(serviceID);
        final Indexer indexer = getIndexer(serviceID, store, requestUUID);
        if (indexer != null) {
            try {
                return indexer.getMapperContent();
            } catch (IOException ex) {
                throw new ConfigurationException(ex);
            } finally {
                destroyIndexer(serviceID, requestUUID);
            }
        }
        return new HashMap<>();
    }

    public String getTreeRepresentation(String serviceID) throws ConfigurationException {
        String requestUUID = UUID.randomUUID().toString();
        final MetadataStoreWrapper store = getMetadataStore(serviceID);
        final Indexer indexer = getIndexer(serviceID, store, requestUUID);
        if (indexer != null) {
            try {
                return indexer.getTreeRepresentation();
            } finally {
                destroyIndexer(serviceID, requestUUID);
            }
        }
        return null;
    }

    private static final Map<String, Indexer> INDEXER_MAP = new HashMap<>();
    private static final Map<String, Set<String>> USED_INDEXER = new HashMap<>();

    /**
     * Build a new Indexer for the specified service ID.
     *
     * @param serviceID the service identifier (form multiple CSW) default: ""
     * @param store the metadata reader of the specified sevrice.
     * @param uuid unique identifier assigned to the indexer, so we can share it.
     *
     * @return A geotk Indexer.
     * @throws ConfigurationException
     */
    protected Indexer getIndexer(final String serviceID, MetadataStore store, final String uuid) throws ConfigurationException {
        synchronized(INDEXER_MAP) {
            Indexer indexer = INDEXER_MAP.get(serviceID);
            if (indexer == null) {
                // we get the CSW configuration file
                final Automatic config = getServiceConfiguration(serviceID);
                if (config != null) {
                    try {
                        if (store == null) {
                            store = getMetadataStore(serviceID);
                        }
                        indexer = indexHandler.getIndexer(config, store, serviceID);
                        /* do not perform indexation as we only want to get the indexer
                        if (indexer.needCreation()) {
                            indexer.createIndex();
                        }*/
                        INDEXER_MAP.put(serviceID, indexer);
                        USED_INDEXER.put(serviceID, new HashSet<>());
                    } catch (Exception ex) {
                        throw new ConfigurationException("An exception occurs while initializing the indexer!\ncause:" + ex.getMessage());
                    }
                } else {
                    throw new ConfigurationException("there is no configuration file correspounding to this ID:" + serviceID);
                }
            }
            USED_INDEXER.get(serviceID).add(uuid);
            return indexer;
        }
    }

    private void destroyIndexer(String serviceId, String uuid) {
        synchronized(INDEXER_MAP) {
             Set<String> used = USED_INDEXER.get(serviceId);
             used.remove(uuid);
             if (used.isEmpty()) {
                Indexer indexer = INDEXER_MAP.get(serviceId);
                indexer.destroy();
                USED_INDEXER.remove(serviceId);
                INDEXER_MAP.remove(serviceId);
             }
        }
    }


    /**
     * Build a new Metadata reader for the specified service ID.
     *
     * @param serviceID the service identifier (form multiple CSW) default: ""
     *
     * @return A metadata reader.
     * @throws ConfigurationException
     */
    protected MetadataStoreWrapper getMetadataStore(final String serviceID) throws ConfigurationException {
        final Automatic config   = getServiceConfiguration(serviceID);
        final List<Integer> providerIDs = serviceBusiness.getCSWLinkedProviders(serviceID);
        final Map<Integer, MetadataStore> wrappeds = new HashMap<>();
        for (Integer providerID : providerIDs) {
            try {
                final MetadataStore originalStore = (MetadataStore) DataProviders.getProvider(providerID).getMainStore();
                wrappeds.put(providerID, originalStore);
            } catch (ConstellationStoreException ex) {
                throw new ConfigurationException(ex);
            }
        }
        return new MetadataStoreWrapper(serviceID, wrappeds, config.getCustomparameters());
    }

    /**
     * Return the first provider id linked to this service.
     *
     * @param serviceID A csw service identifier.
     *
     * @throws ConfigurationException if there is no metadata provider linked to this service.
     */
    protected int getProviderID(final String serviceID) throws ConfigurationException {
        final List<Integer> providerIDs  = serviceBusiness.getCSWLinkedProviders(serviceID);
        if (!providerIDs.isEmpty()) {
            return providerIDs.get(0);
        } else {
            throw new ConfigurationException("there is no metadata provider linked with this sevice: " + serviceID);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instance getInstance(final Integer id, final String lang) throws ConfigurationException {
        final Instance instance = super.getInstance(id, lang);
        try {
            instance.setLayersNumber(getMetadataCount(instance.getIdentifier()));
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting metadata count on CSW instance:" + id, ex);
        }
        return instance;
    }

    public String getTemplateName(final String serviceID, final String metadataID, final String type) throws ConfigurationException {
        return indexHandler.getTemplateName(metadataID, type);
    }

    private BriefNode createBriefNode(final Node node, final Map<String, List<String>> fieldMapping) {
        final BriefNode brief = new BriefNode();
        brief.setNode(node);

        final List<String> identifiers = new ArrayList<>();
        for (String path : fieldMapping.get("identifier")) {
            identifiers.addAll(NodeUtilities.getValuesFromPath(node, path));
        }
        if (!identifiers.isEmpty()) {
            brief.setIdentifier(identifiers.get(0));
        }
        // what to do if we don't find an identifier?

        final List<String> titles = new ArrayList<>();
        for (String path : fieldMapping.get("title")) {
            titles.addAll(NodeUtilities.getValuesFromPath(node, path));
        }
        if (!titles.isEmpty()) {
            brief.setTitle(titles.get(0));
        } else {
            brief.setTitle("unknwown title");
        }

        // optional
        List<String> dates = new ArrayList<>();
        for (String path : fieldMapping.get("date")) {
            dates.addAll(NodeUtilities.getValuesFromPath(node, path));
        }
        if (!dates.isEmpty()) {
            brief.setCreateDate(dates.get(0));
        }
        final List<String> creators = new ArrayList<>();
        for (String path : fieldMapping.get("creator")) {
            creators.addAll(NodeUtilities.getValuesFromPath(node, path));
        }
        if (!creators.isEmpty()) {
            brief.setCreator(creators.get(0));
        }

        return brief;
    }

    private void reloadFSProvider(MetadataStoreWrapper store) throws ConstellationException {
        for (Entry<Integer, MetadataStore> storeEntry : store.getOriginalStores().entrySet()) {
            if (storeEntry.getValue() instanceof FileSystemMetadataStore) {
                try {
                    ((FileSystemMetadataStore) storeEntry.getValue()).analyzeFileSystem(true);
                } catch (MetadataIoException ex) {
                    throw new ConfigurationException(ex);
                }
                providerBusiness.reload(storeEntry.getKey());
            }
        }
    }
}
