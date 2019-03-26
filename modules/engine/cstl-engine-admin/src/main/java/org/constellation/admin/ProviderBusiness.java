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


import java.awt.Dimension;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.measure.NumberRange;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.util.IOUtilities;
import org.constellation.api.DataType;
import org.constellation.api.ProviderType;
import org.constellation.business.*;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.CstlUser;
import org.constellation.dto.DataBrief;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.ProviderPyramidChoiceList;
import org.constellation.dto.Sensor;
import org.constellation.dto.Style;
import org.constellation.dto.metadata.MetadataBrief;
import org.constellation.dto.process.TaskParameter;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.MetadataData;
import org.constellation.provider.MetadataProvider;
import org.constellation.provider.ProviderParameters;
import org.constellation.provider.SensorData;
import org.constellation.provider.SensorProvider;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.SensorRepository;
import org.constellation.repository.UserRepository;
import org.constellation.util.ParamUtilities;
import org.geotoolkit.coverage.Category;
import org.geotoolkit.coverage.GridSampleDimension;
import org.geotoolkit.coverage.grid.GeneralGridGeometry;
import org.geotoolkit.coverage.grid.GridCoverage2D;
import org.geotoolkit.coverage.grid.ViewType;
import org.geotoolkit.coverage.io.CoverageStoreException;
import org.geotoolkit.coverage.io.GridCoverageReadParam;
import org.geotoolkit.coverage.io.GridCoverageReader;
import org.geotoolkit.coverage.xmlstore.XMLCoverageResource;
import org.geotoolkit.coverage.xmlstore.XMLCoverageStore;
import org.geotoolkit.coverage.xmlstore.XMLCoverageStoreFactory;
import org.geotoolkit.data.multires.DefiningPyramid;
import org.geotoolkit.data.multires.Pyramid;
import org.geotoolkit.data.multires.Pyramids;
import org.geotoolkit.image.interpolation.InterpolationCase;
import org.geotoolkit.map.MapBuilder;
import org.geotoolkit.map.MapContext;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.storage.coverage.CoverageResource;
import org.geotoolkit.storage.coverage.PyramidalCoverageResource;
import org.geotoolkit.util.NamesExt;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;
import org.opengis.util.NoSuchIdentifierException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;


@Component("providerBusiness")
@Primary
public class ProviderBusiness implements IProviderBusiness {
    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    private static final String CONFORM_PREFIX = "conform_";

    @Inject
    private UserRepository userRepository;

    @Inject
    private ProviderRepository providerRepository;

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private org.constellation.security.SecurityManager securityManager;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private IProcessBusiness processBusiness;

    @Inject
    private ISensorBusiness sensorBusiness;

    @Inject
    private IMetadataBusiness metadataBusiness;

    @Inject
    private IClusterBusiness clusterBusiness;

    @Inject
    private IDataCoverageJob dataCoverageJob;

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
        if (!providerRepository.existById(providerId)){
            throw new ConstellationException("Provider " + providerId + " does not exist.");
        }

        try {
            createOrUpdateData(providerId,null,false);
        } catch (IOException ex) {
            throw new ConstellationException(ex.getMessage(),ex);
        }

        //send message to nodes
        final ClusterMessage message = clusterBusiness.createRequest(ProviderMessageConsumer.MESSAGE_TYPE_ID,false);
        message.put(ProviderMessageConsumer.KEY_ACTION, ProviderMessageConsumer.VALUE_ACTION_RELOAD);
        message.put(ProviderMessageConsumer.KEY_IDENTIFIER, providerId);
        clusterBusiness.publish(message);
    }

    @Override
    public void reload(String identifier) throws ConstellationException {
        final Integer provider = providerRepository.findIdForIdentifier(identifier);
        if(provider==null){
            throw new ConstellationException("Provider "+identifier+" does not exist.");
        }

        try {
            createOrUpdateData(provider,null,false);
        } catch (IOException ex) {
            throw new ConstellationException(ex.getMessage(),ex);
        }

        //send message to nodes
        final ClusterMessage message = clusterBusiness.createRequest(ProviderMessageConsumer.MESSAGE_TYPE_ID,false);
        message.put(ProviderMessageConsumer.KEY_ACTION, ProviderMessageConsumer.VALUE_ACTION_RELOAD);
        message.put(ProviderMessageConsumer.KEY_IDENTIFIER, provider);
        clusterBusiness.publish(message);
    }

    @Override
    @Deprecated
    @Transactional
    public void removeProvider(final String identifier) throws ConfigurationException {
        final Integer provider = getIDFromIdentifier(identifier);
        if(provider!=null) removeProvider(provider);
    }

    @Override
    @Transactional
    public void removeProvider(final int identifier) throws ConfigurationException {
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
        final ClusterMessage message = clusterBusiness.createRequest(ProviderMessageConsumer.MESSAGE_TYPE_ID,false);
        message.put(ProviderMessageConsumer.KEY_ACTION, ProviderMessageConsumer.VALUE_ACTION_DELETE);
        message.put(ProviderMessageConsumer.KEY_IDENTIFIER, provider.getId());
        clusterBusiness.publish(message);

        //delete provider folder
        //TODO : not hazelcast compatible
        try {
            final Path provDir = ConfigDirectory.getDataIntegratedDirectory(provider.getIdentifier());
            org.geotoolkit.nio.IOUtilities.deleteRecursively(provDir);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during delete data on FS for provider {0}", provider.getIdentifier());
        }
    }

    @Override
    @Transactional
    public void removeAll() throws ConfigurationException {
        final List<ProviderBrief> providers = providerRepository.findAll();
        for (ProviderBrief p : providers) {
            removeProvider(p.getId());
        }
    }

    @Override
    public List<Integer> getDataIdsFromProviderId(Integer id) {
        return providerRepository.findDataIdsByProviderId(id);
    }

    @Override
    public List<Integer> getDataIdsFromProviderId(Integer id, String dataType, boolean included, boolean hidden) {
        if (dataType != null) {
            dataType = dataType.toUpperCase();
        }
        return providerRepository.findDataIdsByProviderId(id, dataType, included, hidden);
    }

    @Override
    public List<DataBrief> getDataBriefsFromProviderId(Integer id, String dataType, boolean included, boolean hidden) throws ConstellationException{
        final List<DataBrief> results = new ArrayList<>();
        final List<Integer> datas = getDataIdsFromProviderId(id, dataType, included, hidden);
        for (final Integer dataId : datas) {
            results.add(dataBusiness.getDataBrief(dataId));
        }
        return results;
    }

    @Override
    @Transactional
    public void updateParent(String providerIdentifier, String newParentIdentifier) {
        final ProviderBrief provider = providerRepository.findByIdentifier(providerIdentifier);
        provider.setParent(newParentIdentifier);
        providerRepository.update(provider);
    }

    @Override
    public Set<GenericName> test(final String providerIdentifier, final ProviderConfiguration configuration) throws ConfigurationException {
        final String type = configuration.getType();
        final String subType = configuration.getSubType();
        final Map<String, String> inParams = configuration.getParameters();

        final DataProviderFactory providerService = DataProviders.getFactory(type);
        final ParameterDescriptorGroup sourceDesc = providerService.getProviderDescriptor();
        ParameterValueGroup sources = sourceDesc.createValue();
        sources.parameter("id").setValue(providerIdentifier);
        sources.parameter("providerType").setValue(type);
        sources = fillProviderParameter(type, subType, inParams, sources);
        try {
            return DataProviders.testProvider(providerIdentifier, providerService, sources);
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String id, ParameterValueGroup spiConfiguration) throws ConfigurationException {
        return create(id, SPI_NAMES.DATA_SPI_NAME, spiConfiguration);
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

        return create(id, pFactory.getName(), providerConfig);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String id, final ProviderConfiguration config) throws ConfigurationException {
        final String type = config.getType();
        final String subType = config.getSubType();
        final Map<String,String> inParams = config.getParameters();

        final DataProviderFactory providerService = DataProviders.getFactory(type);
        final ParameterDescriptorGroup sourceDesc = providerService.getProviderDescriptor();
        ParameterValueGroup sources = sourceDesc.createValue();
        sources.parameter("id").setValue(id);
        sources.parameter("providerType").setValue(type);
        sources = fillProviderParameter(type, subType, inParams, sources);
        return create(id, providerService.getName(), sources);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String id, final String providerFactoryId, final ParameterValueGroup providerConfig) throws ConfigurationException {
        return storeProvider(id, null, ProviderType.LAYER, providerFactoryId, providerConfig);
    }

    @Override
    @Transactional
    public Integer storeProvider(final String identifier, final String parent, final ProviderType type, final String providerFactoryId,
                                  final GeneralParameterValue config) throws ConfigurationException {
        final ProviderBrief provider = new ProviderBrief();
        final Optional<CstlUser> user = userRepository.findOne(securityManager.getCurrentUserLogin());
        if (user.isPresent()) {
            provider.setOwner(user.get().getId());
        }
        provider.setParent(parent);
        provider.setType(type.name());
        try {
            provider.setConfig(IOUtilities.writeParameter(config));
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
            throw new ConfigurationException("Unexting provider for name "+id);
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
            config = IOUtilities.writeParameter(providerConfig);
        } catch (IOException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }

        update(id, config);
    }

    @Override
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
            newConfig = IOUtilities.writeParameter(sources);
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
            throw new ConfigurationException("Provider " + id + " does not exist.");
        }
        provider.setConfig(config);

        providerRepository.update(provider);

        //send message
        final ClusterMessage message = clusterBusiness.createRequest(ProviderMessageConsumer.MESSAGE_TYPE_ID,false);
        message.put(ProviderMessageConsumer.KEY_ACTION, ProviderMessageConsumer.VALUE_ACTION_RELOAD);
        message.put(ProviderMessageConsumer.KEY_IDENTIFIER, provider.getId());
        clusterBusiness.publish(message);
    }

    @Override
    @Transactional
    public void update(final Integer id, String config) throws ConfigurationException {

        final ProviderBrief provider = providerRepository.findOne(id);
        if (provider == null) {
            throw new ConfigurationException("Provider " + id + " does not exist.");
        }
        provider.setConfig(config);

        providerRepository.update(provider);

        //send message
        final ClusterMessage message = clusterBusiness.createRequest(ProviderMessageConsumer.MESSAGE_TYPE_ID,false);
        message.put(ProviderMessageConsumer.KEY_ACTION, ProviderMessageConsumer.VALUE_ACTION_RELOAD);
        message.put(ProviderMessageConsumer.KEY_IDENTIFIER, provider.getId());
        clusterBusiness.publish(message);
    }


    protected ParameterValueGroup fillProviderParameter(String type, String subType,
                                                        Map<String, String> inParams,
                                                        ParameterValueGroup sources)throws ConfigurationException {

        fixPath(inParams, "path");

        if("data-store".equals(type)){
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
                    dbObsParams.parameter("port").setValue(Integer.parseInt(inParams.get("port")));
                    dbObsParams.parameter("database").setValue(inParams.get("database"));
                    dbObsParams.parameter("user").setValue(inParams.get("user"));
                    dbObsParams.parameter("password").setValue(inParams.get("password"));
                    break;
            }
        }else if("filesensor".equals(subType)){

            final ParameterValueGroup sensParams = sources.addGroup("FilesystemSensor");
            sensParams.parameter("data_directory").setValue(URI.create(inParams.get("data_directory")));
        }

        if("data-store".equals(type)){
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
     * {@inheritDoc}
     */
    @Override
    public ProviderPyramidChoiceList listPyramids(final String id, final String layerName) throws ConfigurationException {
        final ProviderPyramidChoiceList choices = new ProviderPyramidChoiceList();

        final List<ProviderBrief> childrenRecs = providerRepository.findChildren(id);

        for (ProviderBrief childRec : childrenRecs) {
            final DataProvider provider = DataProviders.getProvider(childRec.getId());
            final GenericName gname = NamesExt.create(ProviderParameters.getNamespace(provider), layerName);
            final Data cacheData = provider.get(gname);
            if (cacheData != null) {
                final PyramidalCoverageResource cacheRef = (PyramidalCoverageResource) cacheData.getOrigin();
                final Collection<Pyramid> pyramids;
                try {
                    pyramids = Pyramids.getPyramids(cacheRef);
                } catch (DataStoreException ex) {
                    throw new ConfigurationException(ex.getMessage(), ex);
                }
                if (pyramids.isEmpty()) continue;
                //TODO what do we do if there are more then one pyramid ?
                //it the current state of constellation there is only one pyramid
                final Pyramid pyramid = pyramids.iterator().next();
                final Identifier crsid = pyramid.getCoordinateReferenceSystem().getIdentifiers().iterator().next();

                final ProviderPyramidChoiceList.CachePyramid cache = new ProviderPyramidChoiceList.CachePyramid();
                cache.setCrs(crsid.getCode());
                cache.setScales(pyramid.getScales());
                cache.setProviderId(provider.getId());
                cache.setDataId(layerName);
                cache.setConform(childRec.getIdentifier().startsWith("conform_"));

                choices.getPyramids().add(cache);
            }
        }
        return choices;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createAllPyramidConformForProvider(final int providerId) throws ConstellationException {
        final List<org.constellation.dto.Data> dataList = providerRepository.findDatasByProviderId(providerId);
        for(final org.constellation.dto.Data d : dataList) {
            try {
                final DataBrief db = createPyramidConform(d.getId(), d.getOwnerId());
                // link original data with the tiled data.
                dataBusiness.linkDataToData(d.getId(),db.getId());
            }catch(ConstellationException ex) {
                LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            }
        }
    }

    @Override
    public int createZXYPyramidProvider(String providerId, String pyramidProviderId) throws ConstellationException {
        try {
            //create the output folder for pyramid
            final Path providerDirectory = ConfigDirectory.getDataIntegratedDirectory(providerId);
            final Path pyramidDirectory = providerDirectory.resolve(pyramidProviderId);
            if (!Files.exists(pyramidDirectory)) {
                Files.createDirectories(pyramidDirectory);
            }

            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final Parameters pparams = Parameters.castOrWrap(factory.getProviderDescriptor().createValue());
            pparams.getOrCreate(ProviderParameters.SOURCE_ID_DESCRIPTOR).setValue(pyramidProviderId);
            pparams.getOrCreate(ProviderParameters.SOURCE_TYPE_DESCRIPTOR).setValue("data-store");
            final String storeChoiceName = factory.getStoreDescriptor().getName().getCode();
            final ParameterValueGroup choiceParams = pparams.groups(storeChoiceName).stream()
                    .findFirst()
                    .orElseGet(() -> pparams.addGroup(storeChoiceName));

            final Parameters xmlParams = choiceParams.groups("zxy").stream()
                    .findFirst()
                    .map(Parameters::castOrWrap)
                    .orElseGet(() -> Parameters.castOrWrap(choiceParams.addGroup("zxy")));

            xmlParams.parameter(DataStoreProvider.LOCATION).setValue(pyramidDirectory.toUri());

            return create(pyramidProviderId, factory.getName(), pparams);
        } catch (IOException ex) {
            throw new ConstellationException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief createPyramidConform(final String providerId, final String dataName, final String namespace,
                                          final int userOwnerId) throws ConstellationException {
        final QName qName = new QName(namespace, dataName);
        final DataBrief inData = dataBusiness.getDataBrief(qName, providerId);
        return createPyramidConform(inData.getId(), userOwnerId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief createPyramidConform(final int dataId, final int userOwnerId) throws ConstellationException {
        final DataBrief inData = dataBusiness.getDataBrief(dataId);
        if (inData != null){
            // Execute in transaction to ensure that taskParameter is in Database before run
            // process in Quartz scheduler.
            final Map<String, Object> result;
            try {
                result = SpringHelper.executeInTransaction((TransactionStatus transactionStatus) -> {
                    try {
                        return preparePyramidConform(inData, userOwnerId);
                    } catch (ConstellationException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            } catch (Exception e) {
                // Before sending back initial error, we try to find a constellation exception in it.
                // It's likely to be the real error.
                Throwable cause = e.getCause();
                while (cause != null) {
                    if (cause instanceof ConstellationException)
                        throw (ConstellationException)cause;
                    cause = cause.getCause();
                }
                throw new ConstellationException("Cannot initialize confirm pyramid for data " + dataId, e);
            }

            final DataBrief outData = (DataBrief) result.get("outData");
            final Process process = (Process) result.get("process");
            final Integer taskParameterId = (Integer) result.get("taskParameterId");
            final String taskName = (String) result.get("taskName");

            //run pyramid process in Quartz
            processBusiness.runProcess(taskName, process, taskParameterId, userOwnerId);

            // run statistics on integrated data if data.auto.analyse is false
            // TODO try to run statistics AFTER pyramid process ending
            String propertyValue = Application.getProperty(AppProperty.DATA_AUTO_ANALYSE);
            boolean doAnalysis = propertyValue == null ? false : Boolean.valueOf(propertyValue);
            if (!doAnalysis) {
                dataCoverageJob.asyncUpdateDataStatistics(inData.getId());
            }
            return outData;
        }
        throw new ConstellationException("Data "+ dataId +" not found.");
    }

    /**
     * Prepare pyramid conform for a data.
     * This method should not be called alone, but before
     * {@link org.constellation.business.IProcessBusiness#runProcess(String, org.geotoolkit.process.Process, Integer, Integer)}.
     *
     * This method need to be called in a Transaction
     *
     * @param inData Data to pyramid
     * @param userOwnerId owner of the pyramid
     * @return a Map with :
     *      <ul>
     *          <li>"ouData" : new pyramid DataBrief</li>
     *          <li>"process" : pyramid process to execute</li>
     *          <li>"taskParameterId" : task parameter id linked to process</li>
     *          <li>"taskName" : name of the task</li>
     *      </ul>
     * @throws ConstellationException
     */
    private Map<String, Object> preparePyramidConform(final DataBrief inData, final int userOwnerId) throws ConstellationException {
        final String dataName = inData.getName();
        final String namespace = inData.getNamespace();
        final String providerId = inData.getProvider();
        final Integer datasetID = inData.getDatasetId();
        GenericName name = NamesExt.create(namespace,dataName);

        //get data
        final DataProvider inProvider;
        try {
            inProvider = DataProviders.getProvider(inData.getProviderId());
        } catch (ConfigurationException ex) {
            throw new ConstellationException(ex.getMessage(),ex);
        }
        if (inProvider == null) {
            throw new ConstellationException("Provider " + providerId + " does not exist");
        }
        final org.constellation.provider.Data providerData = inProvider.get(name);
        if (providerData == null) {
            throw new ConstellationException("Data " + dataName + " does not exist in provider " + providerId);
        }
        Envelope dataEnv;
        try {
            //use data crs
            dataEnv = providerData.getEnvelope();
        } catch (ConstellationStoreException ex) {
            throw new ConstellationException("Failed to extract envelope for data " + dataName + ". " + ex.getMessage(),ex);
        }

        final Object origin = providerData.getOrigin();

        if(!(origin instanceof CoverageResource)) {
            throw new ConstellationException("Cannot create pyramid conform for no raster data, it is not supported yet!");
        }

        //init coverage reference and grid geometry
        CoverageResource inRef = (CoverageResource) origin;
        final GeneralGridGeometry gg;
        try {
            final GridCoverageReader reader = (GridCoverageReader) inRef.acquireReader();
            gg = reader.getGridGeometry(inRef.getImageIndex());
            inRef.recycle(reader);
        } catch (CoverageStoreException ex) {
            throw new ConstellationException("Failed to extract grid geometry for data " + dataName + ". " + ex.getMessage(),ex);
        }


        //find the type of data we are dealing with, geophysic or photographic
        try {
            final GridCoverageReader reader = (GridCoverageReader) inRef.acquireReader();
            final List<GridSampleDimension> sampleDimensions = reader.getSampleDimensions(inRef.getImageIndex());
            inRef.recycle(reader);
            if (sampleDimensions != null) {
                final int nbBand = sampleDimensions.size();
                boolean hasCategories = false;
                for (int i = 0; i < nbBand; i++) {
                    hasCategories = hasCategories || sampleDimensions.get(i).getCategories() != null;
                }

                if(!hasCategories){
                    //no sample dimension categories, we force some categories
                    //this is a bypass solution to avoid black border images in pyramids
                    //note : we need a pyramid storage model that doesn't produce any pixels
                    //outside the original coverage area

                    RenderedImage img = readSmallImage(reader, gg);

                    final List<GridSampleDimension> newDims = new ArrayList<>();
                    for (int i = 0; i < nbBand; i++) {
                        final GridSampleDimension sd = sampleDimensions.get(i);
                        final int dataType = img.getSampleModel().getDataType();
                        NumberRange range;
                        switch(dataType){
                            case DataBuffer.TYPE_BYTE : range = NumberRange.create(0, true, 255, true); break;
                            case DataBuffer.TYPE_SHORT : range = NumberRange.create(Short.MIN_VALUE, true, Short.MAX_VALUE, true); break;
                            case DataBuffer.TYPE_USHORT : range = NumberRange.create(0, true, 0xFFFF, true); break;
                            case DataBuffer.TYPE_INT : range = NumberRange.create(Integer.MIN_VALUE, true, Integer.MAX_VALUE, true); break;
                            default : range = NumberRange.create(-Double.MAX_VALUE, true, +Double.MAX_VALUE, true); break;
                        }

                        final Category cat = new Category("data", null, range, range);
                        final GridSampleDimension nsd = new GridSampleDimension(sd.getDescription(), new Category[]{cat}, sd.getUnits());
                        newDims.add(nsd);
                    }
                    inRef = new ForcedSampleDimensionsCoverageResource(inRef, newDims);
                }
            }
        } catch (DataStoreException ex) {
            throw new ConstellationException("Failed to extract no-data values for resampling " + ex.getMessage(),ex);
        }


        //create the output folder for pyramid
        PyramidalCoverageResource outRef;
        final String pyramidProviderId = CONFORM_PREFIX + UUID.randomUUID().toString();
        //create the output provider
        final DataProvider outProvider;
        final DataBrief pyramidDataBrief;
        try {
            //create the output folder for pyramid
            final Path pyramidDirectory = ConfigDirectory.getPyramidDirectory(providerId, pyramidProviderId);

            //create output store
            final Parameters storeParams = Parameters.castOrWrap(XMLCoverageStoreFactory.PARAMETERS_DESCRIPTOR.createValue());
            storeParams.getOrCreate(XMLCoverageStoreFactory.PATH).setValue(pyramidDirectory.toUri());
            storeParams.getOrCreate(XMLCoverageStoreFactory.CACHE_TILE_STATE).setValue(true);

            XMLCoverageStore outStore = (XMLCoverageStore) DataStores.open(storeParams);
            if (outStore == null) {
                throw new ConstellationException("Failed to create pyramid layer ");
            }
            XMLCoverageResource covRef = (XMLCoverageResource)outStore.create(name, ViewType.GEOPHYSICS, "TIFF");

            // create provider
            final int outConfigProvider = createPyramidProvider(providerId, pyramidProviderId);
            outProvider = DataProviders.getProvider(outConfigProvider);
            createOrUpdateData(outConfigProvider, datasetID, false);

            name = covRef.getIdentifier();
            outStore = (XMLCoverageStore) outProvider.getMainStore();
            outRef = (XMLCoverageResource) outStore.findResource(name.toString());

            // Update the parent attribute of the created provider
            updateParent(outProvider.getId(), providerId);

            final QName qName = new QName(namespace, name.tip().toString());
            //set rendered attribute to false to indicates that this pyramid can have stats.
            dataBusiness.updateDataRendered(qName, outProvider.getId(), false);

            //set hidden value to true for the pyramid conform data
            pyramidDataBrief = dataBusiness.getDataBrief(qName, pyramidProviderId);
            dataBusiness.updateDataHidden(pyramidDataBrief.getId(),true);

        } catch (Exception ex) {
            throw new ConstellationException("Failed to create pyramid provider " + ex.getMessage(),ex);
        }


        //calculate scales

        try {
            final DefiningPyramid template = Pyramids.createTemplate(gg, gg.getCoordinateReferenceSystem(), new Dimension(256, 256));
            outRef.createModel(template);
        } catch (Exception ex) {
            throw new ConstellationException("Failed to create pyramid and mosaics in store " + ex.getMessage(),ex);
        }

        //prepare process

        Map<String, Object> result = new HashMap<>();
        result.put("outData", pyramidDataBrief);

        //add task in scheduler
        try {
            final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor("administration", "gen-pyramid");

            final MapContext context = MapBuilder.createContext();
            context.layers().add(MapBuilder.createCoverageLayer(inRef));
            final ParameterValueGroup input = desc.getInputDescriptor().createValue();
            input.parameter("mapcontext").setValue(context);
            input.parameter("resource").setValue(outRef);
            input.parameter("mode").setValue("data");
            input.parameter("interpolation").setValue(InterpolationCase.NEIGHBOR);
            final org.geotoolkit.process.Process p = desc.createProcess(input);
            result.put("process", p);

            String taskName = "conform_pyramid_" + providerId + ":" + dataName + "_" + System.currentTimeMillis();
            TaskParameter taskParameter = new TaskParameter();
            taskParameter.setProcessAuthority(desc.getIdentifier().getAuthority().toString());
            taskParameter.setProcessCode(desc.getIdentifier().getCode());
            taskParameter.setDate(System.currentTimeMillis());
            taskParameter.setInputs(ParamUtilities.writeParameterJSON(input));
            taskParameter.setOwner(userOwnerId);
            taskParameter.setName(taskName);
            taskParameter.setType("INTERNAL");
            Integer taskId = processBusiness.addTaskParameter(taskParameter);
            result.put("taskParameterId", taskId);
            result.put("taskName", taskName);

        } catch (IOException | NoSuchIdentifierException ex) {
            throw new ConstellationException("Unable to run pyramid process on scheduler",ex);
        }
        return result;
    }

    private RenderedImage readSmallImage(GridCoverageReader reader, GeneralGridGeometry gg) throws CoverageStoreException{
        //read a single pixel value
        double[] resolution = gg.getResolution();
        if(resolution!=null){
            final GeneralEnvelope envelope = new GeneralEnvelope(gg.getEnvelope());
            for(int i=0;i<resolution.length;i++){
                resolution[i] = envelope.getSpan(i)/ 5.0;
            }

            GridCoverageReadParam params = new GridCoverageReadParam();
            params.setEnvelope(envelope);
            params.setResolution(resolution);
            GridCoverage cov = reader.read(0, params);
            if(cov instanceof GridCoverage2D){
                return ((GridCoverage2D) cov).getRenderedImage();
            }
        }
        return null;
    }

    @Override
    public List<Integer> getProviderIdsAsInt() {
        return providerRepository.getAllIds();
    }

    @Override
    public List<Integer> getProviderIdsAsInt(boolean noParent) {
        if (noParent) {
            return providerRepository.getAllIdsWithNoParent();
        }
        return getProviderIdsAsInt();

    }

    @Transactional
    @Override
    public int createPyramidProvider(String providerId, String pyramidProviderId) throws ConstellationException {
        return createPyramidProvider(providerId, pyramidProviderId, true);
    }

    @Override
    public int createPyramidProvider(String providerId, String pyramidProviderId, boolean cacheTileState) throws ConstellationException {
        try {
            //create the output folder for pyramid
            final Path providerDirectory = ConfigDirectory.getDataIntegratedDirectory(providerId);
            final Path pyramidDirectory = providerDirectory.resolve(pyramidProviderId);
            if (!Files.exists(pyramidDirectory)) {
                Files.createDirectories(pyramidDirectory);
            }

            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final Parameters pparams = Parameters.castOrWrap(factory.getProviderDescriptor().createValue());
            pparams.getOrCreate(ProviderParameters.SOURCE_ID_DESCRIPTOR).setValue(pyramidProviderId);
            pparams.getOrCreate(ProviderParameters.SOURCE_TYPE_DESCRIPTOR).setValue("data-store");
            final String storeChoiceName = factory.getStoreDescriptor().getName().getCode();
            final ParameterValueGroup choiceParams = pparams.groups(storeChoiceName).stream()
                    .findFirst()
                    .orElseGet(() -> pparams.addGroup(storeChoiceName));

            final String xmlParamName = XMLCoverageStoreFactory.PARAMETERS_DESCRIPTOR.getName().getCode();
            final Parameters xmlParams = choiceParams.groups(xmlParamName).stream()
                    .findFirst()
                    .map(Parameters::castOrWrap)
                    .orElseGet(() -> Parameters.castOrWrap(choiceParams.addGroup(xmlParamName)));

            xmlParams.getOrCreate(XMLCoverageStoreFactory.PATH).setValue(pyramidDirectory.toUri());
            xmlParams.getOrCreate(XMLCoverageStoreFactory.CACHE_TILE_STATE).setValue(cacheTileState);

            return create(pyramidProviderId, factory.getName(), pparams);
        } catch (IOException ex) {
            throw new ConstellationException(ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Integer createOrUpdateData(final int providerId, Integer datasetId,final boolean createDatasetIfNull)
            throws IOException, ConstellationException{
        return createOrUpdateData(providerId, datasetId, createDatasetIfNull, false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Integer createOrUpdateData(final int providerId, Integer datasetId,final boolean createDatasetIfNull, final boolean hideNewData)
            throws IOException, ConstellationException{
        final ProviderBrief pr = providerRepository.findOne(providerId);
        if (pr == null) {
            throw new ConstellationException("Provider " + providerId + " does not exist.");
        }
        final List<org.constellation.dto.Data> previousData = providerRepository.findDatasByProviderId(pr.getId());

        final DataProvider provider = DataProviders.getProvider(providerId);

        if (provider == null) {
            throw new ConstellationException("Unable to instanciate the provider: " + pr.getIdentifier());
        }
       /*
        * WARNING : do not auto inject dataset business as a class member of this class. it will cause this bean to be instanciated BEFORE the database init.
        */
        final IDatasetBusiness datasetBusiness = SpringHelper.getBean(IDatasetBusiness.class);
        if (datasetId == null) {
            datasetId = datasetBusiness.getDatasetId(pr.getIdentifier());
            if (datasetId == null && createDatasetIfNull)  {
                datasetId = datasetBusiness.createDataset(pr.getIdentifier(), pr.getOwner(), null);
            }
        }

        if (provider instanceof SensorProvider) {
            final SensorProvider sensorProvider = (SensorProvider) provider;
            try {
                final List<Sensor> sensors = sensorBusiness.getByProviderId(pr.getId());
                // Remove no longer existing sensors.
                for (final Sensor sensor : sensors) {
                    if (!sensorProvider.getKeys().contains(NamesExt.create(sensor.getIdentifier()))) {
                        sensorBusiness.delete(sensor.getId());
                    }
                }
                // Add not registered new sensor.
                Set<GenericName> copyKeys = new HashSet<>(sensorProvider.getKeys());
                for (final GenericName key : copyKeys) {
                    boolean found = false;
                    for (final Sensor sensor : sensors) {
                        if (key.equals(NamesExt.create(sensor.getIdentifier()))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        SensorData sData = (SensorData) sensorProvider.get(key);
                        Object sml = sData.getSensorMetadata();
                        if (sml != null) {
                            final String type = sData.getSensorMLType();
                            final String parentIdentifier = null; // TODO
                            sensorBusiness.create(key.toString(), type, parentIdentifier, sml, System.currentTimeMillis(), pr.getId());
                        }
                    }
                }
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.WARNING, "Error while retrieving sensor from sensor store", ex);
            }
        }

        if (provider instanceof MetadataProvider) {
            final MetadataProvider metadataProvider = (MetadataProvider) provider;
            try {
                final List<MetadataBrief> metadatas = metadataBusiness.getByProviderId(pr.getId(), "DOC");
                // Remove no longer existing metadatas.
                for (final MetadataBrief metadata : metadatas) {
                    if (!metadataProvider.getKeys().contains(NamesExt.create(metadata.getFileIdentifier()))) {
                        metadataBusiness.deleteMetadata(metadata.getId());
                    }
                }
                // Add not registered new metadata.
                Set<GenericName> copyKeys = new HashSet<>(metadataProvider.getKeys());
                for (final GenericName key : copyKeys) {
                    boolean found = false;
                    for (final MetadataBrief metadata : metadatas) {
                        if (key.equals(NamesExt.create(metadata.getFileIdentifier()))) {
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
        }

        // Remove no longer existing data.
        for (final org.constellation.dto.Data data : previousData) {
            boolean found = false;
            for (final GenericName key : provider.getKeys()) {
                String nmsp = NamesExt.getNamespace(key);

                if (nmsp != null && !nmsp.isEmpty()) {
                    if (data.getName().equals(key.tip().toString()) &&
                            data.getNamespace().equals(nmsp)) {
                        found = true;
                        break;
                    }
                } else {
                    if (data.getName().equals(key.tip().toString())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                dataBusiness.missingData(new QName(data.getNamespace(), data.getName()), provider.getId());
            }
        }

        // Add new data.
        for (final GenericName key : provider.getKeys()) {
            final QName name = new QName(NamesExt.getNamespace(key), key.tip().toString());

            boolean found = false;
            for (final org.constellation.dto.Data data : previousData) {
                if (key.equals(NamesExt.create(data.getNamespace(),data.getName()))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String subType  = null;
                boolean included = true;
                Boolean rendered = null;
                if (DataType.COVERAGE.equals(provider.getDataType()) ||
                    DataType.VECTOR.equals(provider.getDataType())) {
                    Data providerData = provider.get(key);
                    if (providerData != null) {
                        // find if data is rendered
                        rendered = providerData.isRendered();
                        subType = providerData.getSubType();
                    }
                }

                Integer dataId = dataBusiness.create(name,
                        pr.getIdentifier(), provider.getDataType().name(), provider.isSensorAffectable(),
                        included, rendered, subType, null, hideNewData);

                if (datasetId != null) {
                    dataBusiness.updateDataDataSetId(dataId, datasetId);
                }
            }
        }
        return datasetId;
    }

    @Override
    public List<Style> getStylesFromProviderId(Integer providerId) {
        return providerRepository.findStylesByProviderId(providerId);
    }
}
