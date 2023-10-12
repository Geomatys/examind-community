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
package org.constellation.admin;


import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.api.DataType;
import org.constellation.api.ProviderType;
import org.constellation.business.*;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.business.IDatasourceBusiness.PathStatus;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.CstlUser;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataSource;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.Sensor;
import org.constellation.dto.Style;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.MetadataData;
import org.constellation.provider.MetadataProvider;
import org.constellation.provider.SensorData;
import org.constellation.provider.SensorProvider;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasourceRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.SensorRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.util.ParamUtilities;
import org.constellation.util.Util;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;

@Component("providerBusiness")
@Primary
public class ProviderBusiness implements IProviderBusiness {
    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    @Autowired
    private IUserBusiness userBusiness;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private org.constellation.security.SecurityManager securityManager;

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private ISensorBusiness sensorBusiness;

    @Autowired
    private IMetadataBusiness metadataBusiness;

    @Autowired
    private IClusterBusiness clusterBusiness;

    @Autowired
    private IConfigurationBusiness configBusiness;

    @Autowired
    private IDatasourceBusiness datasourceBusiness;

    @Autowired
    private DatasourceRepository datasourceRepository;

    @Override
    public List<ProviderBrief> getProviders() {
        return providerRepository.findAll();
    }

    @Override
    public Integer getIDFromIdentifier(final String identifier) {
        return providerRepository.findIdForIdentifier(identifier);
    }

    @Override
    public ProviderBrief getProvider(Integer id) {
        return providerRepository.findOne(id);
    }

    @Override
    public List<String> getProviderIds() {
        final List<String> ids = new ArrayList<>();
        final List<ProviderBrief> providers = providerRepository.findAll();
        for (ProviderBrief p : providers) {
            ids.add(p.getIdentifier());
        }
        return ids;
    }

    @Override
    public void reload(int providerId) throws ConstellationException {
        if (!providerRepository.existsById(providerId)){
            throw new TargetNotFoundException("Provider " + providerId + " does not exist.");
        }

        // if already instanciated, must be reloaded before updating data.
        final DataProvider provider = DataProviders.getProvider(providerId);
        if (provider != null) {
            provider.reload();
        }
        createOrUpdateData(providerId, null, false, false, null);

        //send message for other nodes to reload (will cause double reload on this node)
        final ClusterMessage message = clusterBusiness.createRequest(PRV_MESSAGE_TYPE_ID,false);
        message.put(KEY_ACTION, PRV_VALUE_ACTION_RELOAD);
        message.put(KEY_IDENTIFIER, providerId);
        clusterBusiness.publish(message);
    }

    @Override
    public void reload(String identifier) throws ConstellationException {
        final Integer providerId = providerRepository.findIdForIdentifier(identifier);
        if (providerId == null) {
            throw new TargetNotFoundException("Provider " + identifier + " does not exist.");
        }
        reload(providerId);
    }

    @Override
    @Deprecated
    @Transactional
    public void removeProvider(final String identifier) throws ConstellationException {
        final Integer provider = getIDFromIdentifier(identifier);
        if(provider!=null) removeProvider(provider);
    }

    @Override
    @Transactional
    public void removeProvider(final int identifier) throws ConstellationException {
        final ProviderBrief provider = providerRepository.findOne(identifier);
        if (provider==null) return;

        // remove data from provider
        dataBusiness.removeDataFromProvider(identifier);

        // remove metadata from provider (if its a metadata provider).
        metadataBusiness.deleteFromProvider(identifier);

        // remove sensor from provider (if its a sensor provider).
        sensorRepository.deleteFromProvider(identifier);

        // remove from database
        providerRepository.delete(identifier);

        //send message to nodes
        final ClusterMessage message = clusterBusiness.createRequest(PRV_MESSAGE_TYPE_ID,false);
        if (message != null) {
            message.put(KEY_ACTION, PRV_VALUE_ACTION_DELETE);
            message.put(KEY_IDENTIFIER, provider.getId());
            clusterBusiness.publish(message);
        }

        //delete provider folder
        //TODO : not hazelcast compatible
        boolean fileRemoved = false;
        try {
            fileRemoved = configBusiness.removeDataIntegratedDirectory(provider.getIdentifier());
        } catch (InvalidPathException e) {
            // Note: the provider try to remove an associated integration directory even when none is available.
            // In case of a provider that does not use any directory, and if its name contains invalid file characters,
            // An invalid path error could happen here. We swallow it, because an invalid path here means that the
            // provider cannot have created the directory previously, so it is of no consequence.
            LOGGER.log(Level.FINE, "Cannot delete provider associated directory: invalid file path", e);
        }

        // change the integrated datasource path status:
        //  - "REMOVED" if the file has been removed from the filesystem
        //  - "PENDING" if the file is still present. meaning it can be re-integratd again
        PathStatus newStatus = fileRemoved  ? PathStatus.REMOVED : PathStatus.PENDING;
        datasourceRepository.updateAfterProviderRemoval(identifier, newStatus.name());
    }

    @Override
    @Transactional
    public void removeAll() throws ConstellationException {
        final List<ProviderBrief> providers = providerRepository.findAll();
        for (ProviderBrief p : providers) {
            removeProvider(p.getId());
        }
    }

    @Override
    public List<Integer> getDataIdsFromProviderId(Integer id) {
        return dataRepository.findIdsByProviderId(id);
    }

    @Override
    public List<Integer> getDataIdsFromProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        if (dataType != null) {
            dataType = dataType.toUpperCase();
        }
        return dataRepository.findIdsByProviderId(id, dataType, included, hidden);
    }

    @Override
    public List<DataBrief> getDataBriefsFromProviderId(Integer id, String dataType, boolean included, boolean hidden, boolean fetchDataDescription, boolean fetchDataAssociations) throws ConstellationException{
        final List<DataBrief> results = new ArrayList<>();
        final List<Integer> datas = getDataIdsFromProviderId(id, dataType, included, hidden);
        for (final Integer dataId : datas) {
            results.add(dataBusiness.getDataBrief(dataId, fetchDataDescription, fetchDataAssociations));
        }
        return results;
    }

    @Override
    public boolean test(final String providerIdentifier, final ProviderConfiguration configuration) throws ConstellationException {
        final String type = configuration.getType();
        final String subType = configuration.getSubType();
        final Map<String, String> inParams = configuration.getParameters();

        final DataProviderFactory providerService = DataProviders.getFactory(type);
        final ParameterDescriptorGroup sourceDesc = providerService.getProviderDescriptor();
        ParameterValueGroup sources = sourceDesc.createValue();
        sources.parameter("id").setValue(providerIdentifier);
        sources.parameter("providerType").setValue(type);
        sources = fillProviderParameter(type, subType, inParams, sources);
        return !DataProviders.testProvider(providerIdentifier, providerService, sources).isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String id, SPI_NAMES spiName, ParameterValueGroup spiConfiguration) throws ConfigurationException {
        if (getIDFromIdentifier(id) != null) {
            throw new ConfigurationException("A provider already exists for name "+id);
        }

        final String providerType = spiName.name;
        final DataProviderFactory pFactory = DataProviders.getFactory(providerType);
        final ParameterValueGroup providerConfig = pFactory.getProviderDescriptor().createValue();

        providerConfig.parameter("id").setValue(id);
        providerConfig.parameter("providerType").setValue(providerType);
        final ParameterValueGroup choice =
                providerConfig.groups("choice").get(0).addGroup(spiConfiguration.getDescriptor().getName().getCode());
        org.apache.sis.parameter.Parameters.copy(spiConfiguration, choice);

        return storeProvider(id, ProviderType.LAYER, pFactory.getName(), providerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String id, final ProviderConfiguration config) throws ConstellationException {
        final String type = config.getType();
        final String subType = config.getSubType();
        final Map<String,String> inParams = config.getParameters();

        // we record the sql datasource if we found one in the parameters (not really generic for now)
        recordSQLDataSource(config);

        final DataProviderFactory providerService = DataProviders.getFactory(type);
        if (providerService != null) {
            final ParameterDescriptorGroup sourceDesc = providerService.getProviderDescriptor();
            ParameterValueGroup sources = sourceDesc.createValue();
            sources.parameter("id").setValue(id);
            sources.parameter("providerType").setValue(type);
            sources = fillProviderParameter(type, subType, inParams, sources);
            return storeProvider(id, ProviderType.LAYER, providerService.getName(), sources);
        }
        throw new ConfigurationException("Unable to find a provider factory for type:" + type);
    }


    private void recordSQLDataSource(ProviderConfiguration config) throws ConstellationException {
        String sgbdtype = config.getParameters().get("sgbdtype");
        String host = config.getParameters().get("host");
        String port = config.getParameters().get("port");
        String database = config.getParameters().get("database");
        String user = config.getParameters().get("user");
        String pwd = config.getParameters().get("password");

        if (sgbdtype != null && host != null && port != null && database != null && user != null && pwd != null) {
            String dbUrl = sgbdtype + "://" + host + ':' + port + '/' + database;
            List<DataSource> dss = datasourceBusiness.search(dbUrl, null, null, user, pwd);
            if (dss.isEmpty()) {
                DataSource ds = new DataSource(null, "database", dbUrl, user, pwd, null, false, System.currentTimeMillis(), "COMPLETED", null, true);
                datasourceBusiness.create(ds);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer storeProvider(final String identifier, final ProviderType type, final String providerFactoryId,
                                  final GeneralParameterValue config) throws ConfigurationException {
        final ProviderBrief provider = new ProviderBrief();
        final Optional<CstlUser> user = userBusiness.findOne(securityManager.getCurrentUserLogin());
        if (user.isPresent()) {
            provider.setOwner(user.get().getId());
        }
        provider.setType(type.name());
        try {
            provider.setConfig(ParamUtilities.writeParameter(config));
        } catch (IOException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
        provider.setIdentifier(identifier);
        provider.setImpl(providerFactoryId);
        return providerRepository.create(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void update(final String id, SPI_NAMES spiName, ParameterValueGroup spiConfiguration) throws ConfigurationException {
        if (getIDFromIdentifier(id) == null) {
            throw new TargetNotFoundException("Unexting provider for name " + id);
        }

        final String providerType = spiName.name;
        final DataProviderFactory pFactory = DataProviders.getFactory(providerType);
        final ParameterValueGroup providerConfig = pFactory.getProviderDescriptor().createValue();

        providerConfig.parameter("id").setValue(id);
        providerConfig.parameter("providerType").setValue(providerType);
        final ParameterValueGroup choice =
                providerConfig.groups("choice").get(0).addGroup(spiConfiguration.getDescriptor().getName().getCode());
        org.apache.sis.parameter.Parameters.copy(spiConfiguration, choice);

        String config;
        try {
            config = ParamUtilities.writeParameter(providerConfig);
        } catch (IOException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }

        update(id, config);
    }

    @Override
    @Transactional
    public void update(final String id, final ProviderConfiguration config) throws ConfigurationException {
        final String type = config.getType();
        final String subType = config.getSubType();
        final Map<String, String> inParams = config.getParameters();

        final DataProviderFactory providerService = DataProviders.getFactory(type);
        final ParameterDescriptorGroup sourceDesc = providerService.getProviderDescriptor();
        ParameterValueGroup sources = sourceDesc.createValue();
        sources.parameter("id").setValue(id);
        sources.parameter("providerType").setValue(type);
        sources = fillProviderParameter(type, subType, inParams, sources);

        final String newConfig;
        try {
            newConfig = ParamUtilities.writeParameter(sources);
        } catch (IOException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }

        update(id, newConfig);
    }

    @Override
    @Transactional
    public void update(final String id, String config) throws ConfigurationException {

        final ProviderBrief provider = providerRepository.findByIdentifier(id);
        if (provider == null) {
            throw new TargetNotFoundException("Provider " + id + " does not exist.");
        }
        provider.setConfig(config);

        providerRepository.update(provider);

        //send message
        final ClusterMessage message = clusterBusiness.createRequest(PRV_MESSAGE_TYPE_ID,false);
        message.put(KEY_ACTION, PRV_VALUE_ACTION_RELOAD);
        message.put(KEY_IDENTIFIER, provider.getId());
        clusterBusiness.publish(message);
    }

    @Override
    @Transactional
    public void update(final Integer id, String config) throws ConfigurationException {

        final ProviderBrief provider = providerRepository.findOne(id);
        if (provider == null) {
            throw new TargetNotFoundException("Provider " + id + " does not exist.");
        }
        provider.setConfig(config);

        providerRepository.update(provider);

        //send message
        final ClusterMessage message = clusterBusiness.createRequest(PRV_MESSAGE_TYPE_ID,false);
        message.put(KEY_ACTION, PRV_VALUE_ACTION_RELOAD);
        message.put(KEY_IDENTIFIER, provider.getId());
        clusterBusiness.publish(message);
    }


    /**
     * TODO It is now urgent to remove all this hacks
     */
    protected ParameterValueGroup fillProviderParameter(String type, String subType,
                                                        Map<String, String> inParams,
                                                        ParameterValueGroup sources)throws ConfigurationException {

        fixPath(inParams, "path");

        if("observation-store".equals(type)){
            switch (subType) {
                // TODO : Remove this hacky switch / case when input map will have the right identifier for url parameter.
                case "observationFile":
                    final ParameterValueGroup ncObsParams = sources.groups("choice").get(0).addGroup("ObservationFileParameters");
                    ncObsParams.parameter("identifier").setValue("observationFile");
                    ncObsParams.parameter("namespace").setValue("no namespace");
                    ncObsParams.parameter("path").setValue(URI.create(inParams.get("path")));
                    break;
                case "observationXmlFile":
                    final ParameterValueGroup xmlObsParams = sources.groups("choice").get(0).addGroup("ObservationXmlFileParameters");
                    xmlObsParams.parameter("identifier").setValue("observationXmlFile");
                    xmlObsParams.parameter("namespace").setValue("no namespace");
                    xmlObsParams.parameter("path").setValue(URI.create(inParams.get("path")));
                    break;
                case "observationSOSDatabase":
                    final ParameterValueGroup dbObsParams = sources.groups("choice").get(0).addGroup("SOSDBParameters");
                    dbObsParams.parameter("sgbdtype").setValue(inParams.get("sgbdtype"));
                    dbObsParams.parameter("host").setValue(inParams.get("host"));
                    if (inParams.get("port") != null) {
                        dbObsParams.parameter("port").setValue(Integer.parseInt(inParams.get("port")));
                    }
                    dbObsParams.parameter("database").setValue(inParams.get("database"));
                    dbObsParams.parameter("user").setValue(inParams.get("user"));
                    dbObsParams.parameter("password").setValue(inParams.get("password"));
                    dbObsParams.parameter("schema-prefix").setValue(inParams.get("schema-prefix"));
                    if (inParams.get("timescaledb") != null) {
                        dbObsParams.parameter("timescaledb").setValue(Boolean.parseBoolean(inParams.get("timescaledb")));
                    }
                    dbObsParams.parameter("phenomenon-id-base").setValue(inParams.get("phenomenon-id-base"));
                    dbObsParams.parameter("observation-template-id-base").setValue(inParams.get("observation-template-id-base"));
                    dbObsParams.parameter("observation-id-base").setValue(inParams.get("observation-id-base"));
                    dbObsParams.parameter("sensor-id-base").setValue(inParams.get("sensor-id-base"));
                    dbObsParams.parameter("derbyurl").setValue(inParams.get("derbyurl"));
                    break;
            }
        }else if("filesensor".equals(subType)){

            final ParameterValueGroup sensParams = sources.groups("choice").get(0).addGroup("FileSensorParameters");
            sensParams.parameter("data_directory").setValue(URI.create(inParams.get("data_directory")));
        }

        if("data-store".equals(type) || "sensor-store".equals(type) || "observationCsvFile".equals(subType) || "observationDbfFile".equals(subType) || "observationCsvFlatFile".equals(subType)){
            if (subType!=null && !subType.isEmpty()) {
                final DataStoreProvider featureFactory = DataStores.getProviderById(subType);
                final ParameterValueGroup cvgConfig = org.geotoolkit.parameter.Parameters.toParameter(inParams, featureFactory.getOpenParameters(), true);
                List<ParameterValueGroup> sourceChoices = sources.groups("choice");
                if (sourceChoices.isEmpty()) {
                    throw new ConfigurationException("No valid choice (data store definition) found");
                }
                final ParameterValueGroup sourceChoice = sourceChoices.get(0);
                final String wantedGroupName = cvgConfig.getDescriptor().getName().getCode();
                final List<ParameterValueGroup> groups = sourceChoice.groups(wantedGroupName);
                final ParameterValueGroup groupToFill;
                if (groups.isEmpty()) {
                    groupToFill = sourceChoice.addGroup(wantedGroupName);
                } else {
                    groupToFill = groups.get(0);
                }
                Parameters.copy(cvgConfig, groupToFill);
            } else {
                throw new ConfigurationException("No provider found to resolve the data : ");
            }

        }

        return sources;
    }

    /**
     * Fix path parameter to add file: if not defined.
     *
     * @param inParams
     * @param param
     * @throws ConfigurationException
     */
    private void fixPath(Map<String, String> inParams, String param) throws ConfigurationException {
        String pathStr = inParams.get(param);
        if (pathStr != null) {
            try {
                //try conversion to path
                Path path = org.geotoolkit.nio.IOUtilities.toPath(pathStr);
                inParams.put(param, path.toUri().toString());
            } catch (IOException | InvalidPathException | FileSystemNotFoundException ex) {
                throw new ConfigurationException("Invalid path "+pathStr, ex);
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Integer> getProviderIdsAsInt() {
        return providerRepository.getAllIds();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Integer createOrUpdateData(final int providerId, Integer datasetId,final boolean createDatasetIfNull, final boolean hideNewData, Integer owner)
            throws ConstellationException{
        final ProviderBrief pr = providerRepository.findOne(providerId);
        if (pr == null) {
            throw new TargetNotFoundException("Provider " + providerId + " does not exist.");
        }
        final List<org.constellation.dto.Data> previousData = dataRepository.findByProviderId(pr.getId());

        final DataProvider provider = DataProviders.getProvider(providerId);

        if (provider == null) {
            throw new ConstellationException("Unable to instanciate the provider: " + pr.getIdentifier());
        }
       /*
        * WARNING : do not auto inject dataset business as a class member of this class. it will cause this bean to be instanciated BEFORE the database init.
        */
        final IDatasetBusiness datasetBusiness = SpringHelper.getBean(IDatasetBusiness.class)
                                                             .orElseThrow(() -> new ConstellationException("No spring context available"));
        if (datasetId == null) {
            datasetId = datasetBusiness.getDatasetId(pr.getIdentifier());
            if (datasetId == null && createDatasetIfNull)  {
                datasetId = datasetBusiness.createDataset(pr.getIdentifier(), pr.getOwner(), null);
            }
        }

       /* ----------------------------------------------------------------------
        * ----------------------- Sensor Provider  -----------------------------
        * ----------------------------------------------------------------------
        */
        if (provider instanceof SensorProvider) {
            final SensorProvider sensorProvider = (SensorProvider) provider;
            try {
                final Set<GenericName> keys = provider.getKeys();

                final List<Sensor> sensors = sensorBusiness.getByProviderId(pr.getId());
                // Remove no longer existing sensors.
                for (final Sensor sensor : sensors) {
                    if (!keys.contains(NamesExt.create(sensor.getIdentifier()))) {
                        sensorBusiness.delete(sensor.getId());
                    }
                }
                // Add not registered new sensor.
                Set<GenericName> copyKeys = new HashSet<>(keys);
                for (final GenericName key : copyKeys) {
                    boolean found = false;
                    for (final Sensor sensor : sensors) {
                        if (NamesExt.match(key, NamesExt.create(sensor.getIdentifier()))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        SensorData sData = (SensorData) sensorProvider.get(key);
                        Object sml = sData.getSensorMetadata();
                        if (sml != null) {
                            final String name = sData.getSensorName();
                            final String description = sData.getDescription();
                            final String type = sData.getSensorMLType();
                            final String omType = sData.getOMType();
                            final String parentIdentifier = null; // TODO
                            sensorBusiness.create(key.toString(), name, description, type, omType, parentIdentifier, sml, System.currentTimeMillis(), pr.getId());
                        }
                    }
                }
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "Error while retrieving sensor from sensor store", ex);
            }

       /* ----------------------------------------------------------------------
        * ----------------------- Metadata Provider  ---------------------------
        * ----------------------------------------------------------------------
        */
        } else if (provider instanceof MetadataProvider) {

            final MetadataProvider metadataProvider = (MetadataProvider) provider;
            try {
                final Set<GenericName> keys = provider.getKeys();

                final List<MetadataBrief> metadatas = metadataBusiness.getByProviderId(pr.getId(), "DOC");
                // Remove no longer existing metadatas.
                List<Integer> toRemove = new ArrayList<>();
                for (final MetadataBrief metadata : metadatas) {
                    if (!keys.contains(NamesExt.create(metadata.getFileIdentifier()))) {
                        toRemove.add(metadata.getId());
                    }
                }
                metadataBusiness.deleteMetadata(toRemove);

                // Add not registered new metadata.
                Set<GenericName> copyKeys = new HashSet<>(keys);
                for (final GenericName key : copyKeys) {
                    boolean found = false;
                    for (final MetadataBrief metadata : metadatas) {
                        if (NamesExt.match(key, NamesExt.create(metadata.getFileIdentifier()))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        MetadataData mData = (MetadataData) metadataProvider.get(key);
                        Node n = mData == null? null : mData.getMetadata();
                        if (n != null) {
                            try {
                                metadataBusiness.updateMetadata(key.tip().toString(), n, null, null, null, null, pr.getId(), "DOC", null, false);
                            } catch (ConfigurationException ex) {
                                LOGGER.log(Level.WARNING, "Error while inserting metadata: " + key + " into database:" + ex.getMessage(), ex);
                            }
                        }
                    }
                }
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "Error while retrieving metadata from metadata store", ex);
            }


       /* ----------------------------------------------------------------------
        * ----------------------- Data Provider  -------------------------------
        * ----------------------------------------------------------------------
        */
        } else {

            final Set<GenericName> keys = new TreeSet<>(provider.getKeys());

            // Remove no longer existing data.
            for (final org.constellation.dto.Data data : previousData) {
                final Data indexedDataName = provider.get(data.getNamespace(), data.getName());
                if (indexedDataName == null) {
                    dataBusiness.missingData(new QName(data.getNamespace(), data.getName()), provider.getId());
                } else {
                    keys.remove(indexedDataName.getName()); // Data already exists. We do not want to re-integrate it.
                }
            }
            
            boolean cacheDataInfo = Application.getBooleanProperty(AppProperty.EXA_CACHE_DATA_INFO, false);

            // Add new data.
            for (final GenericName key : keys) {
                final QName name = Util.getQnameFromName(key);

                DataType type    = DataType.OTHER;
                String subType   = null;
                boolean included = true;
                Boolean rendered = null;
                try {
                    Data providerData = provider.get(key);
                    if (providerData != null) {
                        type     = providerData.getDataType();
                        subType  = providerData.getSubType();
                        rendered = providerData.isRendered();
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.FINER, ex.getMessage(), ex);
                }

                Integer dataId = dataBusiness.create(name,
                        pr.getId(), type.name(), provider.isSensorAffectable(),
                        included, rendered, subType, hideNewData, owner);
                
                // cache data informations in the database
                if (cacheDataInfo) {
                    dataBusiness.cacheDataInformation(dataId, false);
                }

                if (datasetId != null) {
                    dataBusiness.updateDataDataSetId(dataId, datasetId);
                }
            }
        }
        return datasetId;
    }

    @Override
    public List<Style> getStylesFromProviderId(Integer providerId) {
        return styleRepository.findByProvider(providerId);
    }
}
