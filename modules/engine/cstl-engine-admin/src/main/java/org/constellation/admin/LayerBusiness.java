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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.imageio.spi.ServiceRegistry;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.constellation.exception.ConstellationException;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.DataBrief;
import org.constellation.dto.service.config.DataSourceType;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.service.config.wxs.AddLayer;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.Layer;
import org.constellation.dto.Style;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.repository.UserRepository;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.ws.MapFactory;
import org.constellation.ws.LayerSecurityFilter;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.constellation.util.DataReference;
import org.constellation.dto.service.config.wxs.FilterAndDimension;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.service.ServiceComplete;
import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class LayerBusiness implements ILayerBusiness {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StyleRepository styleRepository;
    @Autowired
    private LayerRepository layerRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private ProviderRepository providerRepository;
    @Autowired
    private IServiceBusiness serviceBusiness;
    @Autowired
    private org.constellation.security.SecurityManager securityManager;
    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private IClusterBusiness clusterBusiness;

    @Override
    @Transactional
    public void add(final AddLayer addLayerData) throws ConfigurationException {
        final String name        = addLayerData.getLayerId();
        // Prevents adding empty layer namespace, put null instead
        final String namespace   = (addLayerData.getLayerNamespace() != null && addLayerData.getLayerNamespace().isEmpty()) ? null : addLayerData.getLayerNamespace();
        final String providerId  = addLayerData.getProviderId();
        final String alias       = addLayerData.getLayerAlias();
        final String serviceId   = addLayerData.getServiceId();
        final String serviceType = addLayerData.getServiceType();
        add(name, namespace, providerId, alias, serviceId, serviceType, null);
    }

    @Override
    @Transactional
    public void add(final String name, String namespace, final String providerId, final String alias,
            final String serviceId, final String serviceType, final org.constellation.dto.service.config.wxs.Layer config) throws ConfigurationException {

        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(serviceType.toLowerCase(), serviceId);

        if (service !=null) {

            if (namespace != null && namespace.isEmpty()) {
                // Prevents adding empty layer namespace, put null instead
                namespace = null;
            }

            // look for layer namespace
            if (namespace == null) {
                final Integer pvId = providerRepository.findIdForIdentifier(providerId);
                final DataProvider provider = DataProviders.getProvider(pvId);
                if (provider != null) {
                    namespace = ProviderParameters.getNamespace(provider);
                }
            }

            final Integer data = dataRepository.findIdFromProvider(namespace, name, providerId);
            if(data == null) {
                throw new TargetNotFoundException("Unable to find data for namespace:" + namespace+" name:"+name+" provider:"+providerId);
            }
            add(data, alias, service, config);

        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    @Override
    @Transactional
    public void add(int dataId, String alias,
             int serviceId, org.constellation.dto.service.config.wxs.Layer config) throws ConfigurationException {

        final org.constellation.dto.service.ServiceComplete service = serviceBusiness.getServiceById(serviceId);

        if (service !=null) {

            final Data data = dataRepository.findById(dataId);
            if(data == null) {
                throw new TargetNotFoundException("Unable to find data for id:" + dataId);
            }

            String namespace = data.getNamespace();
            if (namespace.isEmpty()) {
                // Prevents adding empty layer namespace, put null instead
                namespace = null;
            }

            boolean update = true;
            Layer layer = layerRepository.findByServiceIdAndLayerName(service.getId(), data.getName(), namespace);
            if (layer == null) {
                update = false;
                layer = new Layer();
            }
            layer.setName(data.getName());
            layer.setNamespace(namespace);
            layer.setAlias(alias);
            layer.setService(service.getId());
            layer.setDataId(data.getId());
            layer.setDate(new Date(System.currentTimeMillis()));
            Optional<CstlUser> user = userRepository.findOne(securityManager.getCurrentUserLogin());
            if(user.isPresent()) {
                layer.setOwnerId(user.get().getId());
            }
            final String configXml = writeLayerConfiguration(config);
            layer.setConfig(configXml);

            int layerID;
            if (!update) {
                layerID = layerRepository.create(layer);
            } else {
                layerRepository.update(layer);
                layerID = layer.getId();
            }

            for (int styleID : styleRepository.getStyleIdsForData(data.getId())) {
                styleRepository.linkStyleToLayer(styleID, layerID);
            }

            //clear cache event
            final ClusterMessage request = clusterBusiness.createRequest(ServiceMessageConsumer.MESSAGE_TYPE_ID,false);
            request.put(ServiceMessageConsumer.KEY_ACTION, ServiceMessageConsumer.VALUE_ACTION_CLEAR_CACHE);
            request.put(ServiceMessageConsumer.KEY_TYPE, service.getType());
            request.put(ServiceMessageConsumer.KEY_IDENTIFIER, service.getIdentifier());
            clusterBusiness.publish(request);
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    @Override
    @Transactional
    public void updateLayerTitle(int layerID, String newTitle) throws ConfigurationException {
        layerRepository.updateLayerTitle(layerID, newTitle);
    }

    @Override
    @Transactional
    public void remove(final String spec, final String serviceId, final String name, final String namespace) throws ConfigurationException {
        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(spec.toLowerCase(), serviceId);
        if (service != null) {
             Layer layer = (namespace != null && !namespace.isEmpty())?
                    layerRepository.findByServiceIdAndLayerName(service, name, namespace) :
                    layerRepository.findByServiceIdAndLayerName(service, name);
            if (layer != null) {
                removeLayer(layer.getId(), spec.toLowerCase(), serviceId);
            } else {
                throw new TargetNotFoundException("Unable to find a layer: {" + namespace + "}" + name);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    @Override
    @Transactional
    public void removeForService(final String serviceType, final String serviceId) throws ConfigurationException {
        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(serviceType.toLowerCase(), serviceId);
        if (service != null) {
            final List<Layer> layers = layerRepository.findByServiceId(service);
            for (Layer layer : layers) {
                removeLayer(layer.getId(), serviceType.toLowerCase(), serviceId);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    @Override
    @Transactional
    public void removeAll() throws ConfigurationException {
        final List<Layer> layers = layerRepository.findAll();
        for (Layer layer : layers) {
            // NOT OPTIMIZED => multiple event sent
            final ServiceComplete service = serviceBusiness.getServiceById(layer.getService());
            if (service != null) {
                removeLayer(layer.getId(), service.getType(), service.getIdentifier());
            }

        }
    }

    protected void removeLayer(int layerId, String serviceType, String serviceId) throws ConfigurationException {
        layerRepository.delete(layerId);

        //clear cache event
        final ClusterMessage request = clusterBusiness.createRequest(ServiceMessageConsumer.MESSAGE_TYPE_ID,false);
        request.put(ServiceMessageConsumer.KEY_ACTION, ServiceMessageConsumer.VALUE_ACTION_CLEAR_CACHE);
        request.put(ServiceMessageConsumer.KEY_TYPE, serviceType);
        request.put(ServiceMessageConsumer.KEY_IDENTIFIER, serviceId);
        clusterBusiness.publish(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LayerSummary> getLayerRefFromStyleId(final Integer styleId) {
        final List<LayerSummary> sumLayers = new ArrayList<>();
        final List<Layer> layers = layerRepository.getLayersRefsByLinkedStyle(styleId);
        for(final Layer lay : layers) {
            final LayerSummary layerSummary = new LayerSummary();
            layerSummary.setId(lay.getId());
            layerSummary.setName(lay.getName());
            layerSummary.setDataId(lay.getDataId());
            sumLayers.add(layerSummary);
        }
        return sumLayers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LayerSummary> getLayerSummaryFromStyleId(final Integer styleId) throws ConstellationException{
        final List<LayerSummary> sumLayers = new ArrayList<>();
        final List<Layer> layers = layerRepository.getLayersByLinkedStyle(styleId);
        for(final Layer lay : layers){
            final QName fullName = new QName(lay.getNamespace(), lay.getName());
            final Data data = dataRepository.findById(lay.getDataId());
            final DataBrief db = dataBusiness.getDataBrief(fullName, data.getProviderId());
            final LayerSummary layerSummary = new LayerSummary();
            layerSummary.setId(lay.getId());
            layerSummary.setName(data.getName());
            layerSummary.setNamespace(data.getNamespace());
            layerSummary.setAlias(lay.getAlias());
            layerSummary.setTitle(lay.getTitle());
            layerSummary.setType(db.getType());
            layerSummary.setSubtype(db.getSubtype());
            layerSummary.setDate(lay.getDate());
            layerSummary.setOwner(db.getOwner());
            layerSummary.setProvider(db.getProvider());
            layerSummary.setDataId(lay.getDataId());
            sumLayers.add(layerSummary);
        }
        return sumLayers;
    }

    @Override
    public List<org.constellation.dto.service.config.wxs.Layer> getLayers(final Integer serviceId, final String login) throws ConfigurationException {
        final List<org.constellation.dto.service.config.wxs.Layer> response = new ArrayList<>();
        if (serviceId != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
            final List<Layer> layers   = layerRepository.findByServiceId(serviceId);
            for (Layer layer : layers) {
                if (securityFilter.allowed(login, layer.getId())) {
                    org.constellation.dto.service.config.wxs.Layer confLayer = toLayerConfig(layer);
                    if (confLayer != null) {
                        response.add(confLayer);
                    }
                }
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
        return response;
    }

    @Override
    public List<QName> getLayerNames(final String serviceType, final String serviceName, final String login) throws ConfigurationException {
        final List<QName> response = new ArrayList<>();
        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(serviceType.toLowerCase(), serviceName);

        if (service != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(service);

            final List<Layer> layers   = layerRepository.findByServiceId(service);
            for (Layer layer : layers) {
                final QName name         = new QName(layer.getNamespace(), layer.getName());
                if (securityFilter.allowed(login, layer.getId())) {
                    response.add(name);
                }
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceName);
        }
        return response;
    }

    /**
     * Get a single layer from service spec and identifier and layer name and namespace.
     *
     * @param spec service type
     * @param identifier service identifier
     * @param name layer name
     * @param namespace layer namespace
     * @param login login for security check
     * @return org.constellation.dto.Layer
     * @throws ConfigurationException
     */
    @Override
    public org.constellation.dto.service.config.wxs.Layer getLayer(final String spec, final String identifier, final String name,
                                                          final String namespace, final String login) throws ConfigurationException {
        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(spec.toLowerCase(), identifier);

        if (service != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(service);
            Layer layer = (namespace != null && !namespace.isEmpty())?
                    layerRepository.findByServiceIdAndLayerName(service, name, namespace) :
                    layerRepository.findByServiceIdAndLayerName(service, name);
            if (layer != null) {
                if (securityFilter.allowed(login, layer.getId())) {
                    return toLayerConfig(layer);
                } else {
                    throw new ConfigurationException("Not allowed to see this layer.");
                }
            } else {
                throw new TargetNotFoundException("Unable to find a layer:" + name);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + identifier);
        }
    }

    @Override
    public NameInProvider getFullLayerName(final String spec, final String identifier, final String name,
                                                          final String namespace, final String login) throws ConfigurationException {
        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(spec.toLowerCase(), identifier);

        if (service != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(service);

            Layer layer = (namespace != null && !namespace.isEmpty())?
                    layerRepository.findByServiceIdAndLayerName(service, name, namespace) :
                    layerRepository.findByServiceIdAndLayerName(service, name);

            final GenericName layerName = NamesExt.create(namespace, name);
            if (layer != null) {
                if (securityFilter.allowed(login, layer.getId())) {
                    final ProviderBrief provider  = providerRepository.findForData(layer.getDataId());
                    Date version = null;
                    /* TODO how to get version?
                      if (layer.getVersion() != null) {
                        version = new Date(layer.getVersion());
                    }*/
                    return new NameInProvider(layerName, provider.getIdentifier(), version, layer.getAlias());
                } else {
                    throw new ConfigurationException("Not allowed to see this layer.");
                }
            } else {
                throw new TargetNotFoundException("Unable to find a layer:" + name);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + identifier);
        }
    }

    @Override
    public FilterAndDimension getLayerFilterDimension(final String spec, final String identifier, final String name,
                                                          final String namespace, final String login) throws ConfigurationException {
        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(spec.toLowerCase(), identifier);

        if (service != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(service);
            Layer layer = (namespace != null && !namespace.isEmpty())?
                        layerRepository.findByServiceIdAndLayerName(service, name, namespace) :
                        layerRepository.findByServiceIdAndLayerName(service, name);

            if (layer != null) {
                if (securityFilter.allowed(login, layer.getId())) {
                    org.constellation.dto.service.config.wxs.Layer layerConfig = readLayerConfiguration(layer.getConfig());
                    if (layerConfig != null) {
                        return new FilterAndDimension(layerConfig.getFilter(), layerConfig.getDimensions());
                    }
                } else {
                    throw new ConfigurationException("Not allowed to see this layer.");
                }
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + identifier);
        }
        return new FilterAndDimension();
    }

    /**
     *
     * @param login
     * @param securityFilter
     * @param layer
     * @return
     * @throws ConfigurationException
     */
    private org.constellation.dto.service.config.wxs.Layer toLayerConfig(Layer layer) throws ConfigurationException {
        final ProviderBrief provider  = providerRepository.findForData(layer.getDataId());
        final QName name         = new QName(layer.getNamespace(), layer.getName());
        final List<Style> styles = styleRepository.findByLayer(layer.getId());
        org.constellation.dto.service.config.wxs.Layer layerConfig = readLayerConfiguration(layer.getConfig());
        if (layerConfig == null) {
            layerConfig = new org.constellation.dto.service.config.wxs.Layer(name);
        }
        layerConfig.setId(layer.getId());

        // override with table values (TODO remove)
        layerConfig.setAlias(layer.getAlias());
        layerConfig.setTitle(layer.getTitle());
        layerConfig.setDate(layer.getDate());
        layerConfig.setOwner(layer.getOwnerId());
        layerConfig.setProviderID(provider.getIdentifier());
        layerConfig.setProviderType(provider.getType());
        layerConfig.setDataId(layer.getDataId());

        // TODO layerDto.setAbstrac();
        // TODO layerDto.setAttribution(null);
        // TODO layerDto.setAuthorityURL(null);
        // TODO layerDto.setCrs(null);
        // TODO layerDto.setDataURL(null);
        // TODO layerDto.setDimensions(null);
        // TODO layerDto.setFilter(null);
        // TODO layerDto.setGetFeatureInfoCfgs(null);
        // TODO layerDto.setKeywords();
        // TODO layerDto.setMetadataURL(null);
        // TODO layerDto.setOpaque(Boolean.TRUE);


        for (Style style : styles) {
            DataReference styleProviderReference = DataReference.createProviderDataReference(DataReference.PROVIDER_STYLE_TYPE, "sld", style.getName());
            layerConfig.getStyles().add(styleProviderReference);
        }

         // TODO layerDto.setTitle(null);
         // TODO layerDto.setVersion();
        return layerConfig;
    }

    private LayerSecurityFilter getSecurityFilter(int  serviceId) throws ConfigurationException {
        final Object config = serviceBusiness.getConfiguration(serviceId);
        if (config instanceof LayerContext) {
            final LayerContext context = (LayerContext) config;
            final MapFactory mapfactory = getMapFactory(context.getImplementation());
            return mapfactory.getSecurityFilter();
        } else {
            throw new ConfigurationException("Trying to get a layer security filter on a non Layer service");
        }
    }

    private org.constellation.dto.service.config.wxs.Layer readLayerConfiguration(final String xml) throws ConfigurationException {
        try {
            if (xml != null) {
                final Unmarshaller u = GenericDatabaseMarshallerPool.getInstance().acquireUnmarshaller();
                final Object config = u.unmarshal(new StringReader(xml));
                GenericDatabaseMarshallerPool.getInstance().recycle(u);
                return (org.constellation.dto.service.config.wxs.Layer) config;
            }
            return null;
        } catch (JAXBException ex) {
            throw new ConfigurationException(ex);
        }
    }

    private String writeLayerConfiguration(final org.constellation.dto.service.config.wxs.Layer obj) {
        String config = null;
        if (obj != null) {
            try {
                final StringWriter sw = new StringWriter();
                final Marshaller m = GenericDatabaseMarshallerPool.getInstance().acquireMarshaller();
                m.marshal(obj, sw);
                GenericDatabaseMarshallerPool.getInstance().recycle(m);
                config = sw.toString();
            } catch (JAXBException e) {
                throw new ConstellationPersistenceException(e);
            }
        }
        return config;
    }

    /**
     * Select the good Map factory in the available ones in function of the dataSource type.
     *
     * @param type
     * @return
     */
    private MapFactory getMapFactory(final DataSourceType type) throws ConfigurationException {
        final Iterator<MapFactory> ite = ServiceRegistry.lookupProviders(MapFactory.class);
        while (ite.hasNext()) {
            MapFactory currentFactory = ite.next();
            if (currentFactory.factoryMatchType(type)) {
                return currentFactory;
            }
        }
        throw new ConfigurationException("No Map factory has been found for type:" + type);
    }
}
