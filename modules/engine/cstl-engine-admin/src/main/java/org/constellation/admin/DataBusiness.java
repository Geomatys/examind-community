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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.listener.DefaultDataBusinessListener;
import org.constellation.admin.listener.IDataBusinessListener;
import org.constellation.admin.util.DataCoverageUtilities;
import org.constellation.admin.util.MetadataUtilities;
import org.constellation.api.DataType;
import org.constellation.api.ServiceDef;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDataCoverageJob;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.DataDescription;
import org.constellation.dto.DataSet;
import org.constellation.dto.DataSummary;
import org.constellation.dto.Dimension;
import org.constellation.dto.ParameterValues;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.Style;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.importdata.FileBean;
import org.constellation.dto.metadata.MetadataLightBrief;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.service.Service;
import org.constellation.dto.service.ServiceProtocol;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.metadata.utils.CstlMetadatas;
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
import org.constellation.repository.UserRepository;
import org.constellation.security.SecurityManagerHolder;
import org.constellation.token.TokenUtils;
import org.constellation.util.StoreUtilities;
import org.geotoolkit.metadata.ImageStatistics;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.temporal.util.PeriodUtilities;
import org.geotoolkit.util.NamesExt;
import org.opengis.feature.PropertyType;
import org.opengis.feature.catalog.FeatureCatalogue;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

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
    private static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        ISO8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Used for debugging purposes.
     */
    protected static final Logger LOGGER = Logging.getLogger("org.constellation.admin");
    /**
     * Injected user repository.
     */
    @Inject
    private UserRepository userRepository;
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

    /**
     * Injected data coverage job
     */
    @Inject
    private IDataCoverageJob dataCoverageJob;

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
    public Object loadIsoDataMetadata(final String providerId,
                                               final QName name) throws ConfigurationException {
        final Data data = dataRepository.findDataFromProvider(name.getNamespaceURI(), name.getLocalPart(), providerId);
        if (data != null) {
            return loadIsoDataMetadata(data.getId());
        }
        return null;
    }

    @Override
    public Object loadIsoDataMetadata(final int dataID) throws ConfigurationException {
        return metadataBusiness.getIsoMetadataForData(dataID);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataBrief(int dataId) throws ConstellationException {
        final Data data = dataRepository.findById(dataId);
        final List<DataBrief> dataBriefs = getDataBriefFrom(Collections.singletonList(data), null, null);
        if (dataBriefs != null && dataBriefs.size() == 1) {
            return dataBriefs.get(0);
        }
        throw new ConstellationException(new Exception("Problem : DataBrief Construction is null or multiple"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataBrief(QName dataName,Integer providerId) throws ConstellationException {
        final Data data = dataRepository.findByNameAndNamespaceAndProviderId(dataName.getLocalPart(), dataName.getNamespaceURI(), providerId);
        final List<Data> datas = new ArrayList<>();
        datas.add(data);
        final List<DataBrief> dataBriefs = getDataBriefFrom(datas, null, null);
        if (dataBriefs != null && dataBriefs.size() == 1) {
            return dataBriefs.get(0);
        }
        throw new ConstellationException(new Exception("Problem : DataBrief Construction is null or multiple"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataBrief(final QName fullName,
                                  final String providerIdentifier) throws ConstellationException {
        final Data data = dataRepository.findDataFromProvider(fullName.getNamespaceURI(), fullName.getLocalPart(), providerIdentifier);
        final List<Data> datas = new ArrayList<>();
        if (data != null) {
            datas.add(data);
            final List<DataBrief> dataBriefs = getDataBriefFrom(datas, null, null);
            if (dataBriefs != null && dataBriefs.size() == 1) {
                return dataBriefs.get(0);
            }
        }
        throw new ConstellationException(new Exception("Problem : DataBrief Construction is null or multiple"));
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
        return getDataBriefFrom(datas, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataLayer(final String layerAlias,
                                  final String providerId) throws ConstellationException {
        final Data data = layerRepository.findDatasFromLayerAlias(layerAlias, providerId);
        final List<Data> datas = new ArrayList<>();
        datas.add(data);
        final List<DataBrief> dataBriefs = getDataBriefFrom(datas, null, null);
        if (dataBriefs != null && !dataBriefs.isEmpty()){
            return dataBriefs.get(0);
        }
        throw new ConstellationException(new Exception("Problem : DataBrief Construction is null or multiple"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataBrief getDataLayer(final int layerId) throws ConstellationException {
        final Data data = layerRepository.findDataFromLayer(layerId);
        return getDataBrief(data.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataBriefsFromDatasetId(final Integer datasetId) {
        final List<Data> dataList = dataRepository.findByDatasetId(datasetId);
        return getDataBriefFrom(dataList, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataSummary> getDataSummaryFromDatasetId(final Integer datasetId) throws ConfigurationException {
        final List<Data> dataList = dataRepository.findByDatasetId(datasetId);
        return getDataSummaryFrom(dataList, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataBriefsFromStyleId(final Integer styleId) {
        final List<Data> dataList = dataRepository.getRefDataByLinkedStyle(styleId);
        return getDataBriefFrom(dataList, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataBriefsFromDatasetId(Integer datasetId, boolean included, boolean hidden, Boolean sensorable, Boolean published) throws ConstellationException {
        final List<Data> dataList = dataRepository.findByDatasetId(datasetId, included, hidden);
        return getDataBriefFrom(dataList, sensorable, published);
    }

    @Override
    public List<DataBrief> getDataBriefsFromProviderId(Integer providerId, String dataType, boolean included, boolean hidden, Boolean sensorable, Boolean published, Boolean fetchDataDescription) throws ConstellationException {
        final List<Data> dataList = providerRepository.findDatasByProviderId(providerId, dataType, included, hidden);
        return getDataBriefFrom(dataList, sensorable, published, fetchDataDescription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DataBrief> getDataRefsFromStyleId(final Integer styleId) {
        final List<Data> dataList = dataRepository.getRefDataByLinkedStyle(styleId);
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
            dataResult = serviceRepository.findDataByServiceId(service.getId());
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

    protected List<DataBrief> getDataBriefFrom(final List<Data> datas, Boolean sensorable, Boolean published) {
        return getDataBriefFrom(datas, sensorable, published, true);
    }


    /**
     * Convert a list of {@link Data} to list of {@link DataBrief}.
     * @param datas given list of {@link Data}.
     * @param sensorable
     * @param published
     * @return the list of {@link DataBrief}.
     */
    protected List<DataBrief> getDataBriefFrom(final List<Data> datas, Boolean sensorable, Boolean published, Boolean fetchDataDescription) {
        final List<DataBrief> dataBriefs = new ArrayList<>();
        if (datas == null) {
            return dataBriefs;
        }
        for (final Data data : datas) {

            /*
             * apply filter on sensorable if specified
             */
            List<String> targetSensors = sensorRepository.getLinkedSensors(data.getId());
            if (sensorable != null) {
                if ((sensorable && targetSensors.isEmpty()) ||
                   (!sensorable && !targetSensors.isEmpty())) {
                    continue;
                }
            }

            /*
             * apply filter on published if specified
             */
            final List<Data> linkedDataList = dataRepository.getDataLinkedData(data.getId());
            final List<Service> services = serviceRepository.findByDataId(data.getId());
            for(final Data d : linkedDataList){
                final List<Service> servicesLinked = serviceRepository.findByDataId(d.getId());
                services.addAll(servicesLinked);
            }

            //use HashSet to avoid duplicated objects.
            final Set<ServiceProtocol> serviceProtocols = new HashSet<>();
            for (final Service service : services) {
                final List<String> protocol = new ArrayList<>();
                final ServiceDef.Specification spec = ServiceDef.Specification.valueOf(service.getType().toUpperCase());
                protocol.add(spec.name());
                protocol.add(spec.fullName);
                final ServiceProtocol sp = new ServiceProtocol(service.getIdentifier(), protocol);
                serviceProtocols.add(sp);
            }

            if (published != null) {
                if ((published  && serviceProtocols.isEmpty()) ||
                    (!published && !serviceProtocols.isEmpty())) {
                    continue;
                }
            }

            final DataBrief db = new DataBrief();
            db.setId(data.getId());
            final Optional<CstlUser> user = userRepository.findById(data.getOwnerId());
            if (user != null && user.isPresent()) {
                final CstlUser cstlUser = user.get();
                if(cstlUser!=null){
                    db.setOwner(cstlUser.getLogin());
                }
            }

            if (fetchDataDescription) {
                try {
                    final DataProvider provider = DataProviders.getProvider(data.getProviderId());
                    final org.constellation.provider.Data layer = provider.get(NamesExt.create(data.getNamespace(), data.getName()));
                    final ImageStatistics stats = DataCoverageUtilities.getDataStatistics(data);
                    if (layer != null) {
                        final DataDescription dataDescription = layer.getDataDescription(stats);
                        db.setDataDescription(dataDescription);
                    } else {
                        LOGGER.warning("Unable to find a provider data: {" + data.getNamespace() + "} " + data.getName());
                    }
                } catch (Throwable e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }

            final int providerId = data.getProviderId();
            final String providerName = getProviderIdentifier(providerId);
            db.setName(data.getName());
            db.setNamespace(data.getNamespace());
            db.setDate(data.getDate());
            db.setProviderId(providerId);
            db.setProvider(providerName);
            db.setDatasetId(data.getDatasetId());
            db.setType(data.getType());
            db.setSubtype(data.getSubtype());
            db.setSensorable(data.getSensorable());
            db.setTargetSensor(targetSensors);
            db.setStatsResult(data.getStatsResult());
            db.setStatsState(data.getStatsState());
            db.setRendered(data.getRendered());
            db.setHidden(data.getHidden());

            for (final Data d : linkedDataList) {
                if("pyramid".equalsIgnoreCase(d.getSubtype()) &&
                        !d.getRendered()){
                    final String pyramidProvId = getProviderIdentifier(d.getProviderId());
                    db.setPyramidConformProviderId(pyramidProvId);
                    break;
                }
            }
            //if the data is a pyramid itself. we need to fill the property to enable the picto of pyramided data.
            if("pyramid".equalsIgnoreCase(data.getSubtype()) && !data.getRendered()){
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
                sb.setProvider(1 == style.getProviderId()? "sld" : "sld_temp");
                sb.setDate(style.getDate());
                sb.setName(style.getName());

                final Optional<CstlUser> userStyle = userRepository.findById(style.getOwnerId());
                if (userStyle!=null && userStyle.isPresent()) {
                    final CstlUser cstlUser = userStyle.get();
                    if(cstlUser!=null){
                        sb.setOwner(cstlUser.getLogin());
                    }
                }
                styleBriefs.add(sb);
            }
            db.setTargetStyle(styleBriefs);
            db.setTargetService(new ArrayList<>(serviceProtocols));

            /**
             * Add for linked metadatas
             */
            db.setMetadatas(metadataBusiness.getMetadataBriefForData(data.getId()));
            dataBriefs.add(db);
        }
        return dataBriefs;
    }

    /**
     * Convert a list of {@link Data} to list of {@link DataSummary}.
     * @param datas given list of {@link Data}.
     * @param sensorable
     * @param published
     * @return the list of {@link DataBrief}.
     */
    protected List<DataSummary> getDataSummaryFrom(final List<Data> datas, final Boolean sensorable, final Boolean published) throws ConfigurationException {
        final List<DataSummary> dataSummaries = new ArrayList<>();
        if (datas == null) {
            return dataSummaries;
        }

        for (final Data data : datas) {

            /*
             * apply filter on sensorable if specified
             */
            List<String> targetSensors = sensorRepository.getLinkedSensors(data.getId());
            if (sensorable != null) {
                if ((sensorable && targetSensors.isEmpty()) ||
                   (!sensorable && !targetSensors.isEmpty())) {
                    continue;
                }
            }

            /*
             * apply filter on published if specified
             */
            final List<Data> linkedDataList = dataRepository.getDataLinkedData(data.getId());
            final List<Service> services = serviceRepository.findByDataId(data.getId());
            for(final Data d : linkedDataList){
                final List<Service> servicesLinked = serviceRepository.findByDataId(d.getId());
                services.addAll(servicesLinked);
            }

            //use HashSet to avoid duplicated objects.
            final Set<ServiceProtocol> serviceProtocols = new HashSet<>();
            for (final Service service : services) {
                final List<String> protocol = new ArrayList<>();
                final ServiceDef.Specification spec = ServiceDef.Specification.valueOf(service.getType().toUpperCase());
                protocol.add(spec.name());
                protocol.add(spec.fullName);
                final ServiceProtocol sp = new ServiceProtocol(service.getIdentifier(), protocol);
                serviceProtocols.add(sp);
            }

            if (published != null) {
                if ((published  && serviceProtocols.isEmpty()) ||
                    (!published && !serviceProtocols.isEmpty())) {
                    continue;
                }
            }

            final DataSummary db = new DataSummary();
            db.setId(data.getId());
            final Optional<CstlUser> user = userRepository.findById(data.getOwnerId());
            if (user != null && user.isPresent()) {
                final CstlUser cstlUser = user.get();
                if(cstlUser != null){
                    db.setOwner(cstlUser.getLogin());
                }
            }

            final int providerId = data.getProviderId();
            final String providerName = getProviderIdentifier(providerId);
            db.setName(data.getName());
            db.setDate(data.getDate());
            db.setDatasetId(data.getDatasetId());
            db.setType(data.getType());
            db.setSubtype(data.getSubtype());
            db.setSensorable(data.getSensorable());
            db.setTargetSensor(targetSensors);

            for (final Data d : linkedDataList) {
                if("pyramid".equalsIgnoreCase(d.getSubtype()) &&
                        !d.getRendered()){
                    final String pyramidProvId = getProviderIdentifier(d.getProviderId());
                    db.setPyramidConformProviderId(pyramidProvId);
                    break;
                }
            }
            //if the data is a pyramid itself. we need to fill the property to enable the picto of pyramided data.
            if("pyramid".equalsIgnoreCase(data.getSubtype()) && !data.getRendered()){
                db.setPyramidConformProviderId(providerName);
            }

            final DataProvider provider = DataProviders.getProvider(data.getProviderId());
            final org.constellation.provider.Data layer = provider.get(NamesExt.create(data.getNamespace(), data.getName()));

            try {
                if (layer != null) {
                    final ImageStatistics stats = DataCoverageUtilities.getDataStatistics(data);
                    final DataDescription dataDescription = layer.getDataDescription(stats);
                    db.setDataDescription(dataDescription);
                } else {
                    LOGGER.warning("Unable to find a provider data: {" + data.getNamespace() + "} " + data.getName());
                }
            } catch (ConstellationStoreException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }

            // List of elevations, times and dim_range values.
            final List<Dimension> dimensions = new ArrayList<>();

            /*
             * Dimension: the available date
             */
            Dimension dim;
            SortedSet<Date> dates=null;
            try {
                if (layer!= null) {
                    dates = layer.getAvailableTimes();
                }
            } catch (ConstellationStoreException ex) {
                LOGGER.log(Level.WARNING, "Error retrieving dates values for the data: {" + data.getNamespace() + "} " + data.getName(), ex);
            }
            if (dates != null && !(dates.isEmpty())) {
                final PeriodUtilities periodFormatter = new PeriodUtilities(ISO8601_FORMAT);
                final String defaut = ISO8601_FORMAT.format(dates.last());
                dim = new Dimension("time", "ISO8601", defaut, null);
                dim.setValue(periodFormatter.getDatesRespresentation(dates));
                dimensions.add(dim);
            }
            db.setDimensions(dimensions);
            dataSummaries.add(db);
        }
        return dataSummaries;
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

                // Relevant erase dataset when the is no more data in it. fr now we remove it
                deleteDatasetIfEmpty(data.getDatasetId());
            }
        }
    }

    protected void deleteDatasetIfEmpty(Integer datasetID) throws ConfigurationException {
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
    public Integer create(final QName name, final String providerIdentifier,
                       final String type, final boolean sensorable,
                       final boolean included, final String subType, final String metadataXml) {
        return create(name, providerIdentifier, type, sensorable, included, null, subType, metadataXml);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(QName name, String providerIdentifier, String type, boolean sensorable, boolean included, Boolean rendered, String subType, String metadataXml) {
        return create(name, providerIdentifier, type, sensorable, included, null, subType, metadataXml, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(QName name, String providerIdentifier, String type, boolean sensorable, boolean included, Boolean rendered, String subType, String metadataXml, boolean hidden) {
        final Integer provider = providerRepository.findIdForIdentifier(providerIdentifier);
        if (provider != null) {
            Data data = new Data();
            data.setDate(new Date());
            data.setName(name.getLocalPart());
            data.setNamespace(name.getNamespaceURI());
            final Optional<CstlUser> user = userRepository.findOne(securityManager.getCurrentUserLogin());
            if (user.isPresent()) {
                data.setOwnerId(user.get().getId());
            }
            data.setProviderId(provider);
            data.setSensorable(sensorable);
            data.setType(type);
            data.setSubtype(subType);
            data.setIncluded(included);
            data.setMetadata(metadataXml);
            data.setRendered(rendered);
            data.setHidden(hidden);
            data = dataRepository.create(data);
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
            final GenericName dataName = NamesExt.create(data.getNamespace(), data.getName());
            if (!included) {
                // 1. remove layers involving the data
                for (Integer layerID : layerRepository.findByDataId(dataID)) {
                    layerRepository.delete(layerID);
                }

                // 2. remove link with dataset
                data.setDatasetId(null);
                dataRepository.update(data);


                // 3. unlink from csw
                dataRepository.removeDataFromAllCSW(dataID);

                // 4. remove metadata
                metadataBusiness.deleteDataMetadata(dataID);

                // 5. remove data files
                if (removeFiles) {
                    final DataProvider dataProvider = DataProviders.getProvider(providerID);
                    dataProvider.remove(dataName);
                }

                // 6. cleanup provider if empty
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
                    try {
                        final Path provDir = ConfigDirectory.getDataIntegratedDirectory(providerIdentifier);
                        IOUtilities.deleteRecursively(provDir);
                    } catch (IOException e) {
                        LOGGER.warning("Error during delete data on FS for provider: " + providerIdentifier);
                    }
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
    public synchronized void removeDataFromProvider(final Integer providerId) throws ConfigurationException {
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
    public ParameterValues getVectorDataColumns(int id) throws ConfigurationException {
        final Data d = dataRepository.findById(id);
        final DataProvider dataProvider = DataProviders.getProvider(d.getProviderId());

        final List<String> colNames = new ArrayList<>();
        final String dataName = dataRepository.findById(id).getName();
        final DataStore store = dataProvider.getMainStore();
        try {
            final Resource rs = StoreUtilities.findResource(store, dataName);
            if (rs instanceof FeatureSet) {
                final FeatureSet fs = (FeatureSet) rs;
                final org.opengis.feature.FeatureType ft = fs.getType();
                for (final PropertyType prop : ft.getProperties(true)) {
                    colNames.add(prop.getName().toString());
                }

                final ParameterValues values = new ParameterValues();
                final HashMap<String, String> mapVals = new HashMap<>();
                for (final String colName : colNames) {
                    mapVals.put(colName, colName);
                }
                values.setValues(mapVals);
                return values;
            } else {
                throw new ConfigurationException("Not a vector data requested");
            }
        } catch (DataStoreException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
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
    @Scheduled(cron = "1 * * * * *")
    public void updateDataStatistics() {
        String propertyValue = Application.getProperty(AppProperty.DATA_AUTO_ANALYSE);
        boolean doAnalysis = propertyValue == null ? false : Boolean.valueOf(propertyValue);
        if (doAnalysis) {
            computeEmptyDataStatistics(false);
        }
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
     * {@inheritDoc}
     */
    @Override
    public void computeEmptyDataStatistics(boolean isInit) {
        final List<Data> dataList = dataRepository.findStatisticLess();

        List<Integer> dataWithoutStats = new ArrayList<>();
        for (final Data data : dataList) {

            //compute statistics only on coverage data not rendered and without previous statistic computed.
            if (DataType.COVERAGE.name().equals(data.getType()) && !"pyramid".equalsIgnoreCase(data.getSubtype()) &&
                    (data.getRendered() == null || !data.getRendered())) {

                String state = data.getStatsState();
                if (isInit) {
                    //rerun statistic for error and pending states
                    if ("PENDING".equalsIgnoreCase(state) || "ERROR".equalsIgnoreCase(state)) {
                        SpringHelper.executeInTransaction(new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                                dataRepository.updateStatistics(data.getId(), null, null);
                            }
                        });
                        dataWithoutStats.add(data.getId());
                    }
                }

                if (state == null || state.isEmpty()) {
                    dataWithoutStats.add(data.getId());
                }
            }
        }

        for (Integer dataId : dataWithoutStats) {
            dataCoverageJob.asyncUpdateDataStatistics(dataId);
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


    @Override
    //FIXME RESTORE cleaning mechanism @Scheduled(fixedDelay=5*60*1000)
    public void uploadCleaner() {
        LOGGER.finer("Cleaner");
        java.nio.file.Path uploadDirectory = ConfigDirectory.getUploadDirectory();

        DirectoryStream.Filter<Path> tokenExpiredFilter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return TokenUtils.isExpired(entry.getFileName().toString());
            }
        };

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDirectory, tokenExpiredFilter)) {
            for (Path path : stream) {
                LOGGER.info(path.getFileName() + " expired");
                IOUtilities.deleteSilently(path);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during uploadDirectory browsing.", e);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<FileBean> getFilesFromPath(final String pathStr, final boolean filtered, final boolean onlyXML) throws ConstellationException {
        final List<FileBean> listBean = new ArrayList<>();
        final Set<String> extensions = DataCoverageUtilities.getAvailableFileExtension().keySet();

        Path path;
        try {
            path = IOUtilities.toPath(pathStr);
        } catch (IOException e) {
            throw new ConstellationException("Invalid path :" +e.getMessage());
        }

        if (!Files.exists(path)) {
            throw new ConstellationException("path does not exists!");
        }

        final boolean isLocal = path.toUri().toASCIIString().startsWith("file");

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {

                if (Files.isRegularFile(entry)) {
                    String ext = IOUtilities.extension(entry).toLowerCase();

                    if (filtered) {
                        if (onlyXML && !"xml".equals(ext)) {
                            return false;
                        // allow zip files
                        } else if ("zip".equals(ext)) {
                            return true;

                        } else if (!extensions.contains(ext)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
            for (Path child : stream) {
                listBean.add(new FileBean(child, isLocal));
            }
        } catch (IOException e) {
            throw new ConstellationException("Error occurs during directory browsing", e);
        }

        Collections.sort(listBean);
        return listBean;
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
        } catch (ConfigurationException ex) {
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
        final GenericName name  = NamesExt.create(data.getNamespace(), data.getName());
        final DataProvider dataProvider = DataProviders.getProvider(provider.getId());
        DefaultMetadata extractedMetadata;
        String crsName = null;
        switch (dataType.toLowerCase()) {
            case "raster":
            case "coverage":
                try {
                    extractedMetadata = MetadataUtilities.getRasterMetadata(dataProvider, name);
                    crsName = MetadataUtilities.getProviderCRSName(dataProvider);
                } catch (DataStoreException e) {
                    LOGGER.log(Level.WARNING, "Error when trying to get raster metadata", e);
                    extractedMetadata = new DefaultMetadata();
                }
                break;
            case "vector":
                try {
                    extractedMetadata = MetadataUtilities.getVectorMetadata(dataProvider, name);
                    crsName = MetadataUtilities.getProviderCRSName(dataProvider);
                } catch (DataStoreException | TransformException e) {
                    LOGGER.log(Level.WARNING, "Error when trying to get metadata for a shape file", e);
                    extractedMetadata = new DefaultMetadata();
                }
                break;
            default:
                extractedMetadata = new DefaultMetadata();
        }

        //initialize metadata from the template and fill it with properties file
        final String metadataID = CstlMetadatas.getMetadataIdForData(provider.getIdentifier(), name);

        // get current user name and email and store into metadata contact.
        final String login = SecurityManagerHolder.getInstance().getCurrentUserLogin();
        final Optional<CstlUser> optUser = userRepository.findOne(login);

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
        final String xml = MetadataUtilities.fillMetadataFromProperties(dataType, metadataID, title, crsName, optUser, keywords);
        final DefaultMetadata templateMetadata = (DefaultMetadata) metadataBusiness.unmarshallMetadata(xml);

        DefaultMetadata mergedMetadata;
        if (extractedMetadata != null) {
            mergedMetadata = MetadataUtilities.mergeMetadata(templateMetadata, extractedMetadata);
        } else {
            mergedMetadata = templateMetadata;
        }

        //merge with uploaded metadata
        DefaultMetadata uploadedMetadata;
        try {
            uploadedMetadata = (DefaultMetadata) loadIsoDataMetadata(dataId);
        } catch (Exception ex) {
            uploadedMetadata = null;
        }
        if (uploadedMetadata != null) {
            mergedMetadata = MetadataUtilities.mergeMetadata(uploadedMetadata,mergedMetadata);
        }
        mergedMetadata.prune();

        //Save metadata
        return updateMetadata(dataId, mergedMetadata, hidden);
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
        final List<DataBrief> results = getDataBriefFrom(dataList, null, null);
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), results);
    }

    @Override
    public List<DataProcessReference> findAllDataProcessReference() {
        final List<DataProcessReference> dataPRef = new ArrayList<>();
        final List<Data> datas = dataRepository.findAllByVisibility(false);
        if(datas!=null){
            for(final Data ds : datas){
                final DataProcessReference ref = new DataProcessReference();
                ref.setId(ds.getId());
                ref.setName(ds.getName());
                ref.setNamespace(ds.getNamespace());
                ref.setProvider(ds.getProviderId());
                ref.setType(ds.getType());
                dataPRef.add(ref);
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

        // 2. For coverage data, we create a pyramid conform.
        if ("raster".equalsIgnoreCase(dataType)) {
            final IProviderBusiness providerBusiness = SpringHelper.getBean(IProviderBusiness.class);
            DataBrief pyramidData = providerBusiness.createPyramidConform(id, owner);// link original data with the tiled data.
            if (pyramidData != null) {
                // link original data with the tiled data.
                linkDataToData(id,pyramidData.getId());
            }
        }

        // 3. Initialize the default metadata.
        initDataMetadata(data.getId(), hidden);

        // 4. if enable and for vector data, we generate feature catalog metadata
        boolean generateFeatCat = Application.getBooleanProperty(AppProperty.GENERATE_FEATURE_CATALOG, true);
        if ("vector".equalsIgnoreCase(dataType) && generateFeatCat) {
            FeatureCatalogue fc = ISO19110Builder.createCatalogueForData(data.getProviderId(), new QName(data.getNamespace(), data.getName()));
            Integer intProviderID = metadataBusiness.getDefaultInternalProviderID();
            if (intProviderID != null) {
                metadataBusiness.updateMetadata(fc.getId(), fc, data.getId(), null, null, owner, intProviderID, "DOC", null, hidden);
            }
        }
        return getDataBrief(id);
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
            } catch (ConstellationException ex) {
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
}
