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

import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.service.config.csw.BriefNode;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.DataSourceType;
import org.constellation.dto.service.Instance;
import org.constellation.dto.StringList;
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.metadata.MetadataIoException;
import org.geotoolkit.metadata.MetadataType;
import org.constellation.ogc.configuration.OGCConfigurer;
import org.geotoolkit.index.IndexingException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.ICSWConfigurer;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
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

    @Override
    public AcknowlegementType refreshIndex(final String id, final boolean asynchrone, final boolean forced) throws ConfigurationException {
        if (isIndexing(id) && !forced) {
            final AcknowlegementType refused = new AcknowlegementType("Failure",
                    "An indexation is already started for this service:" + id);
            return refused;
        } else if (indexing && forced) {
            AbstractIndexer.stopIndexation(Arrays.asList(id));
        }

        startIndexation(id);
        AcknowlegementType ack;
        try {
            ack = refreshIndex(asynchrone, id);
        } catch (IndexingException ex) {
            throw new ConfigurationException(ex);
        } finally {
            endIndexation(id);
        }
        return ack;
    }

    /**
     * Destroy the CSW index directory in order that it will be recreated.
     *
     * @param asynchrone a flag for indexation mode.
     * @param id The service identifier.
     *
     * @return
     * @throws CstlServiceException
     */
    private AcknowlegementType refreshIndex(final boolean asynchrone, final String id) throws ConfigurationException, IndexingException {
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
                return new AcknowlegementType("Failure", "there is no service " + id);
            }
        }


        final List<Path> cswInstanceDirectories = new ArrayList<>();
        if ("all".equals(id)) {
            try {
                cswInstanceDirectories.addAll(ConfigDirectory.getInstanceDirectories("csw"));
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "unable to find CSW instances directories" + suffix);
            }
        } else {
            final Path instanceDir = ConfigDirectory.getInstanceDirectory("csw", id);
            if (instanceDir != null) {
                cswInstanceDirectories.add(instanceDir);
            }
        }

        if (!asynchrone) {
            synchroneIndexRefresh(cswInstances, true);
        } else {
            asynchroneIndexRefresh(cswInstances);
        }
        return new AcknowlegementType("Success", "CSW index successfully recreated");
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
     * Add some CSW record to the index.
     *
     * @param id identifier of the CSW service.
     * @param identifierList list of metadata identifier to add into the index.
     *
     * @return
     * @throws ConfigurationException
     */
    @Override
    public AcknowlegementType addToIndex(final String id, final List<String> identifierList) throws ConfigurationException {
        if (identifierList.isEmpty()) {
            return new AcknowlegementType("Success", "warning: identifier list empty");
        }
        LOGGER.fine("Add to index requested");

        final MetadataStoreWrapper store = getMetadataStore(id);
        if (store != null) {

            //hack for FS CSW
            final MetadataStore origStore = store.getOriginalStore();
            if (origStore instanceof FileSystemMetadataStore) {
                try {
                    ((FileSystemMetadataStore)origStore).analyzeFileSystem(true);
                } catch (MetadataIoException ex) {
                    throw new ConfigurationException(ex);
                }
            }

            String requestUUID = UUID.randomUUID().toString();
            Indexer indexer = getIndexer(id, store, requestUUID);
            if (indexer != null) {
                try {
                    for (String identifier : identifierList) {
                        final RecordInfo obj = origStore.getMetadata(identifier, MetadataType.NATIVE);
                        if (obj == null) {
                            throw new ConfigurationException("Unable to find the metadata: " + identifier);
                        }
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
        return new AcknowlegementType("Success", "The specified record have been added to the CSW index");
    }

    /**
     * Remove some CSW record to the index.
     *
     * @param id identifier of the CSW service.
     * @param identifierList list of metadata identifier to add into the index.
     *
     * @return
     * @throws ConfigurationException
     */
    @Override
    public AcknowlegementType removeFromIndex(final String id, final List<String> identifierList) throws ConfigurationException {
        if (identifierList.isEmpty()) {
            return new AcknowlegementType("Success", "warning: identifier list empty");
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

        final String msg = "The specified record have been remove from the CSW index";
        return new AcknowlegementType("Success", msg);
    }


    /**
     * Stop all the indexation going on.
     *
     * @param id identifier of the CSW service.
     * @return an Acknowledgment.
     */
    @Override
    public AcknowlegementType stopIndexation(final String id) {
        LOGGER.info("\n stop indexation requested \n");
        if (isIndexing(id)) {
            return new AcknowlegementType("Success", "There is no indexation to stop");
        } else {
            AbstractIndexer.stopIndexation(Arrays.asList(id));
            return new AcknowlegementType("Success", "The indexation have been stopped");
        }
    }

    @Override
    public AcknowlegementType importRecords(final String id, final Path f, final String fileName) throws ConfigurationException {
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
                } else {
                    throw new ConfigurationException("An imported file is null");
                }
            }
            final String msg = "The specified record have been imported in the CSW";
            return new AcknowlegementType("Success", msg);
        } catch (SAXException | ParserConfigurationException | IOException ex) {
            LOGGER.log(Level.WARNING, "Exception while unmarshalling imported file", ex);
        }
        return new AcknowlegementType("Error", "An error occurs during the process");
    }

    @Override
    public AcknowlegementType importRecord(final String id, final Node n) throws ConfigurationException {
        LOGGER.fine("Importing record");
        final int providerID = getProviderID(id);
        String metadataID = Utils.findIdentifierNode(n);
        metadataBusiness.updateMetadata(metadataID, n, null, null, null, null, providerID, "DOC");

        return new AcknowlegementType("Success", "The specified record have been imported in the CSW");
    }

    public boolean canImportInternalData(String id) throws ConfigurationException {
        final MetadataStore store = getMetadataStore(id);
        return store.getWriter().canImportInternalData();
    }

    @Override
    public AcknowlegementType removeRecords(final String identifier) throws ConfigurationException {
        final boolean deleted = metadataBusiness.deleteMetadata(identifier);
        if (deleted) {
            final String msg = "The specified record has been deleted from the CSW";
            return new AcknowlegementType("Success", msg);
        } else {
            final String msg = "The specified record has not been deleted from the CSW";
            return new AcknowlegementType("Failure", msg);
        }
    }

    @Override
    public AcknowlegementType removeAllRecords(final String id) throws ConfigurationException {
        final MetadataStore store = getMetadataStore(id);
        final List<Integer> metaIDS = new ArrayList<>();
        try {
            final List<String> metas = store.getAllIdentifiers();
            for (String meta : metas) {
                metaIDS.add(metadataBusiness.getMetadataPojo(meta).getId());
            }
            metadataBusiness.deleteMetadata(metaIDS);
        } catch (MetadataIoException ex) {
            throw new ConfigurationException(ex);
        }
        final String msg = "All records have been deleted from the CSW";
        return new AcknowlegementType("Success", msg);
    }

    @Override
    public AcknowlegementType metadataExist(final String id, final String metadataID) throws ConfigurationException {
        final boolean exist = metadataBusiness.isLinkedMetadataToCSW(metadataID, id);
        if (exist) {
            final String msg = "The specified record exist in the CSW";
            return new AcknowlegementType("Exist", msg);
        } else {
            final String msg = "The specified record does not exist in the CSW";
            return new AcknowlegementType("Not Exist", msg);
        }
    }

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
                results.add(createBriefNode(rec.node, fieldMap));
            }
        } catch (MetadataIoException ex) {
            throw new ConfigurationException(ex);
        }
        return results;
    }

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
                results.add(rec.node);
            }
            return results;
        } catch (MetadataIoException ex) {
            throw new ConfigurationException(ex);
        }
    }


    @Override
    public Node getMetadata(final String id, final String metadataID) throws ConfigurationException {
        return metadataBusiness.getMetadataNode(metadataID);
    }

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

    @Override
    public StringList getAvailableCSWDataSourceType() {
        final List<DataSourceType> sources = indexHandler.getAvailableDatastourceType();
        final StringList result = new StringList();
        for (DataSourceType source : sources) {
            result.getList().add(source.getName());
        }
        return result;
    }

    @Override
    public AcknowlegementType removeIndex(String id) throws ConfigurationException {
        try {
            String requestUUID = UUID.randomUUID().toString();
            MetadataStore store = getMetadataStore(id);
            IndexLucene indexer = (IndexLucene) getIndexer(id, store, requestUUID);
            destroyIndexer(id, requestUUID);
            SQLRtreeManager.removeTree(indexer.getFileDirectory());
        } catch (ConfigurationException | SQLException | IOException ex) {
           throw new ConfigurationException(ex);
        }
        return new AcknowlegementType("Success", "CSW index successfully destroyed");
    }

    /**
     * Refresh the map of configuration object.
     *
     * @param id identifier of the CSW service.
     * @return
     * @throws ConfigurationException
     */
    protected Automatic getServiceConfiguration(final String id) throws ConfigurationException {
        final Path instanceDirectory = ConfigDirectory.getInstanceDirectory("csw", id);
        try {
            // we get the CSW configuration file
            final Automatic config = (Automatic) serviceBusiness.getConfiguration("csw", id);
            if (config !=  null) {
                config.setConfigurationDirectory(instanceDirectory);
            }
            return config;

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
     * @throws org.constellation.ws.CstlServiceException
     */
    private void synchroneIndexRefresh(final List<String> cswInstances, final boolean reloadFSStore) throws ConfigurationException, IndexingException {
        boolean deleted = false;
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
                if (store.getOriginalStore() instanceof FileSystemMetadataStore) {
                    try {
                        ((FileSystemMetadataStore)store.getOriginalStore()).analyzeFileSystem(true);
                    } catch (MetadataIoException ex) {
                        throw new ConfigurationException(ex);
                    }
                }
            }
        }

        //if we have deleted something we restart the services
        if (deleted) {
            //restart(); TODO
        } else {
            LOGGER.log(Level.INFO, "there is no index to delete");
        }

    }

    /**
     * Build a new Index in a new folder.
     * This index will be used at the next restart of the server.
     *
     * @param id The service identifier.
     * @param configurationDirectory  The CSW configuration directory.
     *
     * @throws org.constellation.ws.CstlServiceException
     */
    private void asynchroneIndexRefresh(final List<String> cswInstances) throws ConfigurationException {
        String requestUUID = UUID.randomUUID().toString();
        for (String cswInstance : cswInstances) {
            final Automatic config = getServiceConfiguration(cswInstance);
            final MetadataStoreWrapper store = getMetadataStore(cswInstance);
            Indexer indexer = getIndexer(cswInstance, store, requestUUID);
            try {
                indexHandler.refreshIndex(config, cswInstance, indexer, true);

            } catch (IllegalArgumentException | IndexingException ex) {
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
     *
     * @return A lucene Indexer.
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
                        if (indexer.needCreation()) {
                            indexer.createIndex();
                        }
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
        final Integer providerID = serviceBusiness.getCSWLinkedProviders(serviceID);
        if (providerID != null) {
            final MetadataStore originalStore = (MetadataStore) DataProviders.getProvider(providerID).getMainStore();
            return new MetadataStoreWrapper(serviceID, originalStore, config.getCustomparameters(), providerID);

        } else {
            throw new ConfigurationException("there is no metadata store correspounding to this ID:" + serviceID);
        }
    }

    protected int getProviderID(final String serviceID) throws ConfigurationException {
        final Integer providerID     = serviceBusiness.getCSWLinkedProviders(serviceID);
        if (providerID != null) {
            return providerID;
        } else {
            throw new ConfigurationException("there is no providere correspounding to this ID:" + serviceID);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instance getInstance(final Integer id) throws ConfigurationException {
        final Instance instance = super.getInstance(id);
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

}
