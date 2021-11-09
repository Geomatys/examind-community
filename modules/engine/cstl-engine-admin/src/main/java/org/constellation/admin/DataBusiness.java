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

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.metadata.MetadataCopier;
import org.apache.sis.metadata.MetadataStandard;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.Resource;
import org.constellation.admin.listener.DefaultDataBusinessListener;
import org.constellation.admin.util.MetadataUtilities;
import org.constellation.api.DataType;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.business.listener.IDataBusinessListener;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.DataSet;
import org.constellation.dto.Dimension;
import org.constellation.dto.DimensionRange;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.ServiceReference;
import org.constellation.dto.SimpleDataDescription;
import org.constellation.dto.StatInfo;
import org.constellation.dto.Style;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.metadata.utils.MetadataFeeder;
import org.constellation.metadata.utils.Utils;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ISO19110Builder;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.SensorRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.security.SecurityManagerHolder;
import org.geotoolkit.temporal.util.PeriodUtilities;
import org.opengis.feature.catalog.FeatureCatalogue;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business facade for data.
 *
 * @author Mehdi Sidhoum (Geomatys).
 * @version 0.9
 * @since 0.9
 */

@Component("cstlDataBusiness")
//@DependsOn({"database-initer", "providerBusiness"})
@DependsOn({"providerBusiness"})
@Primary
public class DataBusiness implements IDataBusiness {

    /**
     * Temporal formatting for layer with TemporalCRS.
     * @TODO Duplicate from {@link org.constellation.map.ws.DefaultWMSWorker}
     */
    private final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Used for debugging purposes.
     */
    protected static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    private static List<MetadataFeeding> METADATA_FILL_STRATEGIES = Collections.unmodifiableList(Arrays.asList(
            (datasource, feeder) -> feeder.setExtent(datasource, MetadataFeeder.WriteOption.CREATE_NEW),
            (datasource, feeder) -> feeder.setSpatialRepresentation(datasource, MetadataFeeder.WriteOption.CREATE_NEW)
    ));

    /**
     * Injected user business.
     */
    @Inject
    private IUserBusiness userBusiness;

    @Inject
    private IConfigurationBusiness configBusiness;
    /**
     * Injected data repository.
     */
    @Inject
    protected DataRepository dataRepository;
    /**
     * Injected layer repository.
     */
    @Inject
    protected LayerRepository layerRepository;
    /**
     * Injected security manager.
     */
    @Inject
    protected org.constellation.security.SecurityManager securityManager;
    /**
     * Injected style repository.
     */
    @Inject
    private StyleRepository styleRepository;
    /**
     * Injected provider repository.
     */
    @Inject
    protected ProviderRepository providerRepository;
    /**
     * Injected dataset repository.
     */
    @Inject
    protected DatasetRepository datasetRepository;
    /**
     * Injected sensor repository.
     */
    @Inject
    private SensorRepository sensorRepository;
    /**
     * Injected metadata repository.
     */
    @Inject
    protected IMetadataBusiness metadataBusiness;

    @Autowired(required = false)
    protected IDataBusinessListener dataBusinessListener = new DefaultDataBusinessListener();

     /**
     * Injected service repository.
     */
    @Inject
    private ServiceRepository serviceRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public Data getData(int dataId) throws ConfigurationException {
        Data d = dataRepository.findById(dataId);
        if (d == null) {
            throw new TargetNotFoundException("No data found for id:" + dataId);
        }
        return d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataBrief(int dataId, boolean fetchDataDescription, boolean fetchAssociations) throws ConstellationException {
        final Data data = dataRepository.findById(dataId);
        if (data != null) {
            final List<DataBrief> dataBriefs = getDataBriefFrom(Collections.singletonList(data), null, null, fetchDataDescription, fetchAssociations);
            if (!dataBriefs.isEmpty()) {
                return dataBriefs.get(0);
            } else {
                throw new ConstellationException("Unable to build a dataBrief from the data with the id:" + dataId);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a data with the id: " + dataId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataBrief(QName dataName,Integer providerId, boolean fetchDataDescription, boolean fetchAssociations) throws ConstellationException {
        final Data data = dataRepository.findByNameAndNamespaceAndProviderId(dataName.getLocalPart(), dataName.getNamespaceURI(), providerId);
        if (data != null) {
            final List<DataBrief> dataBriefs = getDataBriefFrom(Collections.singletonList(data), null, null, fetchDataDescription, fetchAssociations);
           if (!dataBriefs.isEmpty()) {
                return dataBriefs.get(0);
            } else {
                throw new ConstellationException("Unable to build a dataBrief from the data with the id:" + data.getId());
            }
        } else {
            throw new TargetNotFoundException("Unable to find a data with the name: " + dataName + " in the provider id:" + providerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataBrief(final QName dataName,
                                  final String providerIdentifier, boolean fetchDataDescription, boolean fetchAssociations) throws ConstellationException {
        final Data data = dataRepository.findDataFromProvider(dataName.getNamespaceURI(), dataName.getLocalPart(), providerIdentifier);
        if (data != null) {
            final List<DataBrief> dataBriefs = getDataBriefFrom(Collections.singletonList(data), null, null, fetchDataDescription, fetchAssociations);
             if (!dataBriefs.isEmpty()) {
                return dataBriefs.get(0);
            } else {
                throw new ConstellationException("Unable to build a dataBrief from the data with the id:" + data.getId());
            }
        }
         else {
            throw new TargetNotFoundException("Unable to find a data with the name: " + dataName + " in the provider:" + providerIdentifier);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,Object> getDataRawModel(int dataId) throws ConstellationException {
        final Data data = dataRepository.findById(dataId);
        final org.constellation.provider.Data provData  = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
        if (provData != null) {
            try {
                return provData.rawDescription();
            } catch (Exception ex) {
                throw new ConstellationException(ex);
            }
        }
        return new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getDataId(QName fullName, int providerId) throws ConstellationException {

        final Data data = dataRepository.findByNameAndNamespaceAndProviderId(fullName.getLocalPart(), fullName.getNamespaceURI(), providerId);
        if (data != null) {
            return data.getId();
        }
        throw new TargetNotFoundException("Unexisting data :" + fullName + " in provider:" + providerId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataBriefsFromMetadataId(final String metadataId) {
        final List<Data> datas = findByMetadataId(metadataId);
        return getDataBriefFrom(datas, null, null, true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataBriefsFromDatasetId(Integer datasetId, boolean included, boolean hidden, Boolean sensorable, Boolean published, boolean fetchDataDescription, boolean fetchAssociations) throws ConstellationException {
        final List<Data> dataList = dataRepository.findByDatasetId(datasetId, included, hidden);
        //sort by name by defaut
        dataList.sort((Data o1, Data o2) -> String.valueOf(o1.getName()).compareToIgnoreCase(String.valueOf(o2.getName())));
        return getDataBriefFrom(dataList, sensorable, published, fetchDataDescription, fetchAssociations);
    }

    @Override
    public List<DataBrief> getDataBriefsFromProviderId(Integer providerId, String dataType, boolean included, boolean hidden, Boolean sensorable, Boolean published, boolean fetchDataDescription, boolean fetchAssociations) throws ConstellationException {
        final List<Data> dataList = dataRepository.findByProviderId(providerId, dataType, included, hidden);
        return getDataBriefFrom(dataList, sensorable, published, fetchDataDescription, fetchAssociations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataFromStyleId(final Integer styleId) {
        final List<Data> dataList = dataRepository.getDataByLinkedStyle(styleId);
        if (dataList != null) {
            return toDataRef(dataList);
        }
        return new ArrayList<>();
    }


    /**
     * Returns a list of {@link Data} for given metadata identifier.
     * @param metadataId given metadata identifier.
     * @return list of {@link Data}.
     */
    private List<Data> findByMetadataId(final String metadataId) {
        List<Data> dataResult   = new ArrayList<>();
        final DataSet dataset   = datasetRepository.findByMetadataId(metadataId);
        final Data data         = dataRepository.findByMetadataId(metadataId);
        final Service service   = serviceRepository.findByMetadataId(metadataId);
        if (dataset != null){
            dataResult = dataRepository.findByDatasetId(dataset.getId());
        } else if (service!= null) {
            dataResult = dataRepository.findByServiceId(service.getId());
        } else if (data != null) {
            dataResult.add(data);
        }
        return dataResult;
    }

    /**
     * Convert a Data to DataBrief using only fields :
     * <ul>
     *     <li>id</li>
     *     <li>name</li>
     *     <li>namespace</li>
     *     <li>provider</li>
     *     <li>type</li>
     *     <li>subtype</li>
     * </ul>
     * @param dataList data to convert
     * @return
     */
    protected List<DataBrief> toDataRef(List<Data> dataList) {
        final List<DataBrief> dataBriefs = new ArrayList<>();

        for (final Data data : dataList) {
            final int providerId = data.getProviderId();
            final String providerName = getProviderIdentifier(providerId);
            final DataBrief db = new DataBrief();
            db.setId(data.getId());
            db.setName(data.getName());
            db.setNamespace(data.getNamespace());
            db.setProviderId(providerId);
            db.setProvider(providerName);
            db.setType(data.getType());
            db.setSubtype(data.getSubtype());
            dataBriefs.add(db);
        }
        return dataBriefs;
    }

    /**
     * Convert a list of {@link Data} to list of {@link DataBrief}.
     *
     * @param datas given list of {@link Data}.
     * @param sensorable filter en sensorable data. can be {@code null}
     * @param published filter en published data. can be {@code null}
     * @param fetchDataDescription Flag to add or not data description (high cost)
     * @param fetchAssociations Flag to add data associations
     *
     * @return the list of {@link DataBrief} never {@code null}.
     */
    protected List<DataBrief> getDataBriefFrom(final List<Data> datas, Boolean sensorable, Boolean published, Boolean fetchDataDescription, Boolean fetchAssociations) {
        final List<DataBrief> dataBriefs = new ArrayList<>();
        if (datas == null) {
            return dataBriefs;
        }
        final Map<String, CoordinateReferenceSystem> crsMap = new HashMap<>();
        for (final Data data : datas) {


            List<String> targetSensors = null;
            List<Data> linkedDataList = null;
            Set<ServiceReference> serviceRefs = null;

            /**
             * Compute data association
             *
             */
            if (fetchAssociations) {
              
                /*
                * apply filter on sensorable if specified
                 */
                targetSensors = sensorRepository.getDataLinkedSensors(data.getId());
                if (sensorable != null) {
                    if ((sensorable && targetSensors.isEmpty())
                            || (!sensorable && !targetSensors.isEmpty())) {
                        continue;
                    }
                }

               /*
                * Look for linked services.
                */
                linkedDataList = dataRepository.getDataLinkedData(data.getId());
                final List<Service> services = serviceRepository.findByDataId(data.getId());
                for(final Data d : linkedDataList){
                    final List<Service> servicesLinked = serviceRepository.findByDataId(d.getId());
                    services.addAll(servicesLinked);
                }

                //use HashSet to avoid duplicated objects.
                serviceRefs = new HashSet<>();
                for (final Service service : services) {
                    final ServiceReference sp = new ServiceReference(service);
                    serviceRefs.add(sp);
                }

                /*
                 * apply filter on published if specified
                 */
                if (published != null) {
                    if ((published  && serviceRefs.isEmpty()) ||
                        (!published && !serviceRefs.isEmpty())) {
                        continue;
                    }
                }
            }

            final DataBrief db = convertToDataBrief(data, targetSensors, linkedDataList, serviceRefs, crsMap, fetchDataDescription, fetchAssociations);
            dataBriefs.add(db);
        }
        return dataBriefs;
    }

    /**
     * Convert a {@link Data} into a {@link DataBrief}.
     *
     * @param data given list of {@link Data}.

     * @param fetchDataDescription Flag to add or not data dscription (high cost)
     * @return a {@link DataBrief}  never {@code null}.
     */
    private DataBrief convertToDataBrief(Data data, List<String> targetSensors, final List<Data> linkedDataList, final Set<ServiceReference> serviceRefs, Map<String, CoordinateReferenceSystem> crsMap, Boolean fetchDataDescription, Boolean fetchAssociations) {
       final DataBrief db = new DataBrief(data);
       
       final String owner = userBusiness.findById(data.getOwnerId()).map(CstlUser::getLogin).orElse(null);
       db.setOwner(owner);

       if (Boolean.TRUE.equals(fetchDataDescription)) {
            try {
                final org.constellation.provider.Data provData = DataProviders.getProviderData(data.getProviderId(), data.getNamespace(), data.getName());
                if (provData != null) {
                    StatInfo stats = null;
                    if (DataType.COVERAGE.name().equals(data.getType()) && (data.getRendered() == null || !data.getRendered())) {
                        stats = new StatInfo(data.getStatsState(), data.getStatsResult());
                    }
                    Envelope cachedEnv = null;
                    if (data.getCachedInfo()) {
                        cachedEnv = readEnvelope(data.getId(), data.getCrs(), crsMap);
                    }
                     // List of elevations, times and dim_range values.
                    final List<Dimension> dimensions = new ArrayList<>();

                    /*
                     * Dimension: the available date
                     */
                    SortedSet<Date> dates = provData.getAvailableTimes();
                    if (dates != null && !(dates.isEmpty())) {
                        synchronized(ISO8601_FORMAT) {
                            final PeriodUtilities periodFormatter = new PeriodUtilities(ISO8601_FORMAT);
                            final String defaut = ISO8601_FORMAT.format(dates.last());
                            Dimension dim = new Dimension("time", "ISO8601", defaut, null);
                            dim.setValue(periodFormatter.getDatesRespresentation(dates));
                            dimensions.add(dim);
                        }
                    }
                    // TODO elevations and other dimensions
                    db.setDimensions(dimensions);
                    
                    final DataDescription dataDescription = provData.getDataDescription(stats, cachedEnv);
                    db.setDataDescription(dataDescription);
                } else {
                    // because UI can't support data without data description
                    db.setDataDescription(new SimpleDataDescription());
                    LOGGER.warning("Unable to find a provider data: {" + data.getNamespace() + "} " + data.getName());
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                // because UI can't support data without data description
                if (db.getDataDescription() == null) {
                    db.setDataDescription(new SimpleDataDescription());
                }
            }
        }

       String title = data.getName();
       Integer dsid = data.getDatasetId();
       if(dsid != null && dsid >= 0) {
           String datasetId = datasetRepository.findById(dsid).getIdentifier();
            title = datasetId + " / " + data.getName();
       }
       final int providerId = data.getProviderId();
       final String providerName = getProviderIdentifier(providerId);
       db.setTitle(title);
       db.setProvider(providerName);

       if (fetchAssociations) {
            db.setTargetSensor(targetSensors);

            final List<DataBrief> linkedBriefs = new ArrayList<>();
            for (final Data ld : linkedDataList) {
                // do not return a complete brief for linked data.
                DataBrief d = convertToDataBrief(ld, new ArrayList<>(), new ArrayList<>(), new HashSet<>(), crsMap, false, false);
                if ("pyramid".equalsIgnoreCase(d.getSubtype()) && !d.getRendered()) {
                    final String pyramidProvId = getProviderIdentifier(d.getProviderId());
                    db.setPyramidConformProviderId(pyramidProvId);
                }
                linkedBriefs.add(d);
            }
            db.setLinkedDatas(linkedBriefs);

            //if the data is a pyramid itself. we need to fill the property to enable the picto of pyramided data.
                 if ("pyramid".equalsIgnoreCase(data.getSubtype()) && !data.getRendered()){
                db.setPyramidConformProviderId(providerName);
            }

            /**
             * Add for linked styles
             */
            final List<Style> styles = styleRepository.findByData(data.getId());
            final List<StyleBrief> styleBriefs = new ArrayList<>(0);
            for (final Style style : styles) {
                final StyleBrief sb = new StyleBrief();
                sb.setId(style.getId());
                sb.setType(style.getType());
                     sb.setProvider(1 == style.getProviderId() ? "sld" : "sld_temp");
                sb.setDate(style.getDate());
                sb.setName(style.getName());

                final Optional<CstlUser> userStyle = userBusiness.findById(style.getOwnerId());
                if (userStyle.isPresent()) {
                    sb.setOwner(userStyle.get().getLogin());
                }
                styleBriefs.add(sb);
            }
            db.setTargetStyle(styleBriefs);
            db.setTargetService(new ArrayList<>(serviceRefs));

            /**
             * Add for linked metadatas
             */
            db.setMetadatas(metadataBusiness.getMetadataBriefForData(data.getId()));
       }
       return db;
    }

    /**
     * Returns provider identifier for given provider id.
     * @param providerId given provider id.
     * @return provider identifier as string.
     */
    private String getProviderIdentifier(final int providerId) {
        return providerRepository.findOne(providerId).getIdentifier();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void missingData(final QName name, final String providerIdentifier) throws ConstellationException {
        final Integer provider = providerRepository.findIdForIdentifier(providerIdentifier);
        if (provider != null) {
            final Data data = dataRepository.findByNameAndNamespaceAndProviderId(name.getLocalPart(), name.getNamespaceURI(), provider);
            if (data != null) {
                // delete data entry
                metadataBusiness.deleteDataMetadata(data.getId());
                dataBusinessListener.preDataDelete(data);
                dataRepository.delete(data.getId());
                dataBusinessListener.postDataDelete(data);
            }
        }
    }

    protected void deleteDatasetIfEmpty(Integer datasetID) throws ConstellationException {
        if (datasetID != null) {
            List<Data> datas = dataRepository.findAllByDatasetId(datasetID);
            if (datas.isEmpty()) {
                metadataBusiness.deleteDatasetMetadata(datasetID);
                datasetRepository.delete(datasetID);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void removeData(Integer dataId, final boolean removeFiles) throws ConstellationException {
        final List<Data> linkedDataList = dataRepository.getDataLinkedData(dataId);
        for(final Data d : linkedDataList){
            updateDataIncluded(d.getId(), false, removeFiles);
        }
        updateDataIncluded(dataId, false, removeFiles);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAll() throws ConstellationException {
        final List<Data> datas = dataRepository.findAll();
        for (final Data data : datas) {
            metadataBusiness.deleteDataMetadata(data.getId());
            dataBusinessListener.preDataDelete(data);
            dataRepository.delete(data.getId());
            dataBusinessListener.postDataDelete(data);
            // Relevant erase dataset when the is no more data in it. fr now we remove it
            deleteDatasetIfEmpty(data.getDatasetId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(QName name, Integer providerId, String type, boolean sensorable, boolean included, Boolean rendered, String subType, boolean hidden, Integer owner) {
        if (providerId != null) {
            Data data = new Data();
            data.setDate(new Date());
            data.setName(name.getLocalPart());
            data.setNamespace(name.getNamespaceURI());
            if (owner == null) {
                final Optional<CstlUser> user = userBusiness.findOne(securityManager.getCurrentUserLogin());
                if (user.isPresent()) {
                    data.setOwnerId(user.get().getId());
                }
            } else {
                data.setOwnerId(owner);
            }
            data.setProviderId(providerId);
            data.setSensorable(sensorable);
            data.setType(type);
            data.setSubtype(subType);
            data.setIncluded(included);
            data.setRendered(rendered);
            data.setHidden(hidden);
            int id = dataRepository.create(data);
            data.setId(id);
            dataBusinessListener.postDataCreate(data);
            return data.getId();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateDataIncluded(final int dataId, boolean included, final boolean removeFiles) throws ConstellationException {
        final Data data = dataRepository.findById(dataId);
        if (data != null) {
            data.setIncluded(included);
            dataRepository.update(data);

            final int providerID = data.getProviderId();
            final int dataID = data.getId();
            if (!included) {
                // 1. remove layers involving the data
                for (Integer layerID : layerRepository.findByDataId(dataID)) {
                    layerRepository.delete(layerID);
                }

                // 2. remove link with dataset / hide data
                data.setDatasetId(null);
                data.setHidden(true);
                dataRepository.update(data);


                // 3. remove metadata
                metadataBusiness.deleteDataMetadata(dataID);

                // 4. remove data files
                if (removeFiles) {
                    final DataProvider dataProvider = DataProviders.getProvider(providerID);
                    dataProvider.remove(data.getNamespace(), data.getName());
                }

                // 5. cleanup provider if empty
                boolean remove = true;
                List<Data> providerData = dataRepository.findByProviderId(providerID);
                for (Data pdata : providerData) {
                    if (pdata.getIncluded()) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    //notify pre delete
                    for (Data pdata : providerData) {
                        dataBusinessListener.preDataDelete(pdata);
                        // remove metadata (should have been done already)
                        metadataBusiness.deleteDataMetadata(pdata.getId());
                    }

                    final ProviderBrief p = providerRepository.findOne(providerID);
                    final String providerIdentifier = p.getIdentifier();
                    final IProviderBusiness providerBusiness = SpringHelper.getBean(IProviderBusiness.class);
                    providerBusiness.removeProvider(providerID);

                    //notify post delete
                    for (final Data pdata : providerData) {
                        dataBusinessListener.postDataDelete(pdata);
                    }

                    // delete associated files in integrated folder. the file name (p.getIdentifier()) is a folder.
                    configBusiness.removeDataIntegratedDirectory(providerIdentifier);
                }

                // Relevant erase dataset when there is no more data in it. for now we remove it
                deleteDatasetIfEmpty(data.getDatasetId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public synchronized void removeDataFromProvider(final Integer providerId) throws ConstellationException {
        final List<Data> datas = dataRepository.findByProviderId(providerId);
        for (final Data data : datas) {
            dataBusinessListener.preDataDelete(data);
            metadataBusiness.deleteDataMetadata(data.getId());
            dataRepository.delete(data.getId());
            dataBusinessListener.postDataDelete(data);
            // Relevant erase dataset when the is no more data in it. fr now we remove it
            deleteDatasetIfEmpty( data.getDatasetId());
        }
    }

    @Override
    @Transactional
    public MetadataLightBrief updateMetadata(int dataId, Object newMetadata, final boolean hidden) throws ConstellationException {
        final Data data = dataRepository.findById(dataId);
        if (data != null) {
            Integer internalProviderID = metadataBusiness.getDefaultInternalProviderID();
            if (internalProviderID != null) {
                Object oldMetadata = metadataBusiness.getIsoMetadataForData(dataId);
                String metadataID;
                if (oldMetadata != null) {
                    metadataID = Utils.findIdentifier(oldMetadata);
                } else {
                    metadataID = Utils.findIdentifier(newMetadata);
                }
                return metadataBusiness.updateMetadata(metadataID, newMetadata, dataId, null, null, null, internalProviderID, "DOC", null, hidden);
            } else {
                LOGGER.warning("No metadata provider available");
            }
        } else {
            throw new TargetNotFoundException("Data :" + dataId +  " not found");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getCountAll(boolean includeInvisibleData) {
        return dataRepository.countAll(includeInvisibleData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Scheduled(fixedDelay = 3600000L)
    @Transactional
    public void removeOldHiddenData() throws ConstellationException {
        LOGGER.finer("Cleaning data table");
        Long currentTime = System.currentTimeMillis();
        List<Data> datas = dataRepository.findAllByVisibility(true);
        for (Data data : datas) {
            // if the data is older than 3 hours => remove
            if (!data.getIncluded() && currentTime - data.getDate().getTime() > 10800000) {
                removeData(data.getId(), false);
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    @Transactional
    public void updateDataRendered(final QName fullName, final String providerIdentifier, boolean isRendered) {
        final Data data = dataRepository.findDataFromProvider(fullName.getNamespaceURI(),
                fullName.getLocalPart(),
                providerIdentifier);
        data.setRendered(isRendered);
        dataRepository.update(data);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    @Transactional
    public void updateDataRendered(final int dataId, boolean isRendered) {
        final Data data = dataRepository.findById(dataId);
        data.setRendered(isRendered);
        dataRepository.update(data);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    @Transactional
    public void updateDataDataSetId(final Integer dataId, final Integer datasetId) {
        final Data data = dataRepository.findById(dataId);
        data.setDatasetId(datasetId);
        dataRepository.update(data);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    @Transactional
    public void updateDataHidden(final int dataId, boolean value) {
        final Data data = dataRepository.findById(dataId);
        data.setHidden(value);
        dataRepository.update(data);
    }


    @Transactional
    @Override
    public void linkDataToData(final int dataId, final int childId) {
        dataRepository.linkDataToData(dataId, childId);
    }

    @Override
    public boolean existsById(int dataId) {
        return dataRepository.existsById(dataId);
    }

    @Override
    public Map<String, Object> getDataAssociations(int dataId) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("styles", styleRepository.fetchByDataId(dataId));
        entity.put("services", serviceRepository.fetchByDataId(dataId));
        entity.put("sensors", sensorRepository.fetchByDataId(dataId));
        return entity;
    }

    @Override
    public Path[] exportData(int dataId) throws ConstellationException {
        final Data data = dataRepository.findById(dataId);

        final DataProvider provider;
        try {
            provider = DataProviders.getProvider(data.getProviderId());
            return provider.getFiles();
        } catch (Exception ex) {
            throw new ConstellationException("Error while accessing provider for data:" + dataId, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public MetadataLightBrief initDataMetadata(final int dataId, final boolean hidden) throws ConstellationException {
        final Data data         = dataRepository.findById(dataId);
        final ProviderBrief provider = providerRepository.findOne(data.getProviderId());
        final String dataType   = data.getType();
        final org.constellation.provider.Data dataP = DataProviders.getProviderData(provider.getId(), data.getNamespace(), data.getName());

        final DefaultMetadata extractedMetadata = extractMetadata(dataP);
        tryFillMetadata(dataP.getOrigin(), extractedMetadata);

        //initialize metadata from the template and fill it with properties file
        final String metadataID = MetadataUtilities.getMetadataIdForData(provider.getIdentifier(), data.getNamespace(), data.getName());

        // get current user name and email and store into metadata contact.
        final String login = SecurityManagerHolder.getInstance().getCurrentUserLogin();
        final Optional<CstlUser> optUser = userBusiness.findOne(login);

        //fill in keywords the dataset identifier.
        final List<String> keywords = new ArrayList<>();
        if (data.getDatasetId() != null) {
            final DataSet dataset = datasetRepository.findById(data.getDatasetId());
            if (dataset != null) {
                keywords.add(dataset.getIdentifier());
            }
        }

        // find unused title
        String title = data.getName();
        int i = 1;
        while (metadataBusiness.existMetadataTitle(title)) {
            title = data.getName() + '_' + i;
            i++;
        }

        // initialize metadata
        final Properties prop = configBusiness.getMetadataTemplateProperties();
        final String xml = MetadataUtilities.fillMetadataFromProperties(prop, dataType, metadataID, title, dataP.getResourceCRSName(), optUser, keywords);
        final DefaultMetadata templateMetadata = (DefaultMetadata) metadataBusiness.unmarshallMetadata(xml);

        DefaultMetadata mergedMetadata = Utils.mergeMetadata(templateMetadata, extractedMetadata);

        //merge with uploaded metadata
        DefaultMetadata uploadedMetadata;
        try {
            uploadedMetadata = (DefaultMetadata) metadataBusiness.getIsoMetadataForData(dataId);
        } catch (Exception ex) {
            uploadedMetadata = null;
        }
        if (uploadedMetadata != null) {
            mergedMetadata = Utils.mergeMetadata(uploadedMetadata,mergedMetadata);
        }
        mergedMetadata.prune();

        //Save metadata
        return updateMetadata(dataId, mergedMetadata, hidden);
    }

    private void tryFillMetadata(Resource origin, DefaultMetadata target) {
        if (origin instanceof org.apache.sis.storage.DataSet) {
            final org.apache.sis.storage.DataSet dataset = (org.apache.sis.storage.DataSet) origin;
            final MetadataFeeder feeder = new MetadataFeeder(target);

            for (final MetadataFeeding feedStrategy : METADATA_FILL_STRATEGIES) {
                try {
                    // If resource already provide extent information do not add it.
                    feedStrategy.apply(dataset, feeder);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "Cannot update metadata information", e);
                }
            }
        }
    }

    private static DefaultMetadata extractMetadata(org.constellation.provider.Data targetData) {
        try {
            final Metadata sourceMeta = targetData.getOrigin().getMetadata();
            final MetadataCopier copier = new MetadataCopier(MetadataStandard.ISO_19115);
            return new DefaultMetadata(copier.copy(Metadata.class, sourceMeta));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot extract resource metadata. Fallback to an empty one");
            // Display details only in debug mode.
            LOGGER.log(Level.FINE, "Cannot extract resource metadata. Fallback to an empty one", e);
        }
        return new DefaultMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateDataOwner(int dataId, int newOwner) {
        dataRepository.updateOwner(dataId, newOwner);
    }

    @Override
    public Map.Entry<Integer, List<DataBrief>> filterAndGetBrief(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) {
        final Map.Entry<Integer, List<Data>> entry = dataRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
        final List<Data> dataList = entry.getValue();
        final List<DataBrief> results = getDataBriefFrom(dataList, null, null, true, true);
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), results);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataProcessReference> findDataProcessReference(String dataType) {
        final List<Data> datas = dataRepository.findAllByVisibility(false);
        final List<DataProcessReference> dataPRef = new ArrayList<>();
        if (datas != null) {
            for (final Data ds : datas) {
                if (dataType== null || dataType.equals(ds.getType())) {
                    final DataProcessReference ref = new DataProcessReference();
                    ref.setId(ds.getId());
                    ref.setName(ds.getName());
                    ref.setNamespace(ds.getNamespace());
                    ref.setProvider(ds.getProviderId());
                    ref.setType(ds.getType());
                    dataPRef.add(ref);
                }
            }
        }
        return dataPRef;
    }

    @Override
    @Transactional
    public DataBrief acceptData(int id, int owner, boolean hidden) throws ConstellationException {
        Data data = dataRepository.findById(id);
        String dataType = data.getType();

        // 1. change the hidden status of the data
        updateDataHidden(id, hidden);

        // 2. change the ownership of the data
        updateDataOwner(id, owner);

        /* deactivated

           3. For coverage data, we create a pyramid conform.
        if ("raster".equalsIgnoreCase(dataType)) {
            final IPyramidBusiness pyramidBusiness = SpringHelper.getBean(IPyramidBusiness.class);
            DataBrief pyramidData = pyramidBusiness.createPyramidConform(id, owner);
            if (pyramidData != null) {
                // link original data with the tiled data.
                linkDataToData(id,pyramidData.getId());
            }
        }*/

        // 4. Initialize the default metadata.
        initDataMetadata(data.getId(), hidden);

        // 5. if enable and for vector data, we generate feature catalog metadata
        boolean generateFeatCat = Application.getBooleanProperty(AppProperty.GENERATE_FEATURE_CATALOG, true);
        if ("vector".equalsIgnoreCase(dataType) && generateFeatCat) {
            FeatureCatalogue fc = ISO19110Builder.createCatalogueForData(data.getProviderId(), new QName(data.getNamespace(), data.getName()));
            Integer intProviderID = metadataBusiness.getDefaultInternalProviderID();
            if (intProviderID != null) {
                metadataBusiness.updateMetadata(fc.getId(), fc, data.getId(), null, null, owner, intProviderID, "DOC", null, hidden);
            }
        }
        return getDataBrief(id, true, true);
    }

    @Override
    @Transactional
    public Map<String, List> acceptDatas(List<Integer> ids, int owner, boolean hidden) throws ConstellationException {
        Map<String, List> results = new HashMap<>();
        results.put("accepted", new ArrayList<>());
        results.put("refused", new ArrayList<>());

        for (Integer id : ids) {
            try {
                results.get("accepted").add(acceptData(id, owner, hidden));
            } catch (Exception ex) {
                results.get("refused").add(id);
                LOGGER.log(Level.INFO, ex.getMessage(), ex);
            }
        }
        return results;
    }

    @Override
    public Integer getDataProvider(Integer dataId) {
        final Data data = dataRepository.findById(dataId);
        if (data != null) {
            return data.getProviderId();
        }
        return null;
    }

    @Override
    public Integer getDataDataset(Integer dataId) {
        final Data data = dataRepository.findById(dataId);
        if (data != null) {
            return data.getDatasetId();
        }
        return null;
    }

    @Override
    public Envelope getEnvelope(int dataId) {
        if (dataRepository.isCachedDataInfo(dataId)) {
            final Data data = dataRepository.findById(dataId);
            String crsWKT = data.getCrs();
            return readEnvelope(dataId, crsWKT, new HashMap<>());
        }
        return null;
    }
    
    private Envelope readEnvelope(int dataId, String crsWKT, Map<String, CoordinateReferenceSystem> crsMap) {
        if (crsWKT != null) {
            try {
                CoordinateReferenceSystem crs;
                if (crsMap.containsKey(crsWKT)) {
                    crs = crsMap.get(crsWKT);
                } else {
                    crs = CRS.fromWKT(crsWKT);
                    crsMap.put(crsWKT, crs);
                }
                List<Double[]> coordinates = dataRepository.getDataBBox(dataId);
                GeneralEnvelope env = new GeneralEnvelope(crs);
                for (int i = 0; i < crs.getCoordinateSystem().getDimension(); i++) {
                    env.setRange(i, coordinates.get(i)[0], coordinates.get(i)[1]);
                }
                return env;
            } catch (FactoryException ex) {
                LOGGER.log(Level.WARNING, "Unreadable WKT CRS", ex);
            }
        }
        return null;
    }

    @Override
    public SortedSet<Date> getDataTimes(int dataId, boolean range) {
        return dataRepository.getDataTimes(dataId, range);
    }

    @Override
    public SortedSet<Number> getDataElevations(int dataId) {
        return dataRepository.getDataElevations(dataId);
    }

    @Override
    public SortedSet<DimensionRange> getDataDimensionRange(int dataId) {
        return dataRepository.getDataDimensionRange(dataId);
    }

    @Override
    public void cacheDataInformation(int dataId, boolean refresh) throws ConstellationException {
        DataBrief db = getDataBrief(dataId, false, false);
        if (db == null) {
            throw new TargetNotFoundException("Unable to find a data with the id:" + dataId);
        }
        if (db.getCachedInfo() && !refresh) {
            return;
        }
        Envelope env = null;
        Set<Date> dates = null;
        Set<Number> elevations = null;
        Set<DimensionRange> dims = null;
        try {
            org.constellation.provider.Data providerData = DataProviders.getProviderData(db.getProviderId(), db.getNamespace(), db.getName());
            if (providerData != null) {
                env        = providerData.getEnvelope();
                dates      = providerData.getAvailableTimes();
                elevations = providerData.getAvailableElevations();
                dims       = providerData.getSampleValueRanges();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, ex.getMessage(), ex);
        }

        // cache data informations in the database
        if (env != null) {
            String crs;
            try {
                crs = env.getCoordinateReferenceSystem().toWKT();
                if (crs != null) {
                    List<Double[]> coordinates = new ArrayList<>();
                    for (int i = 0; i < env.getDimension(); i++) {
                        coordinates.add(new Double[]{env.getMinimum(i), env.getMaximum(i)});
                    }
                    dataRepository.updateDataBBox(dataId, crs, coordinates);
                    dataRepository.updateDataTimes(dataId, dates);
                    dataRepository.updateDataElevations(dataId, elevations);
                    dataRepository.updateDimensionRange(dataId, dims);
                }
            } catch (UnsupportedOperationException ex) {
                LOGGER.log(Level.WARNING, "Error while serializing data CRS to WKT: {0}", ex.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface MetadataFeeding {
        void apply(final org.apache.sis.storage.DataSet datasource, final MetadataFeeder target) throws Exception;
    }
}
