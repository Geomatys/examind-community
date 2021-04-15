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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.ClusterMessage;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Layer;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.StyleReference;
import org.constellation.dto.service.Service;
import org.constellation.dto.service.config.wxs.AddLayer;
import org.constellation.dto.service.config.wxs.FilterAndDimension;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.ws.LayerSecurityFilter;
import org.constellation.ws.MapFactory;
import org.geotoolkit.util.NamesExt;
import org.opengis.util.GenericName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component("cstlLayerBusiness")
@Primary
public class LayerBusiness implements ILayerBusiness {

    @Autowired
    protected IUserBusiness userBusiness;
    @Autowired
    protected StyleRepository styleRepository;
    @Autowired
    protected LayerRepository layerRepository;
    @Autowired
    protected DataRepository dataRepository;
    @Autowired
    private ProviderRepository providerRepository;
    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected org.constellation.security.SecurityManager securityManager;
    @Autowired
    protected IDataBusiness dataBusiness;
    @Autowired
    protected IClusterBusiness clusterBusiness;

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    /**
     * Lazy loaded map of {@link MapFactory} found in classLoader.
     */
    private Map<String, MapFactory> mapFactories = null;

    @Override
    @Transactional
    @Deprecated
    public Integer add(final AddLayer addLayerData) throws ConfigurationException {
        final String name        = addLayerData.getLayerId();
        // Prevents adding empty layer namespace, put null instead
        String namespace         = (addLayerData.getLayerNamespace() != null && addLayerData.getLayerNamespace().isEmpty()) ? null : addLayerData.getLayerNamespace();
        final String providerId  = addLayerData.getProviderId();
        final String alias       = addLayerData.getLayerAlias();
        final String serviceId   = addLayerData.getServiceId();
        final String serviceType = addLayerData.getServiceType();

        final Integer service = serviceBusiness.getServiceIdByIdentifierAndType(serviceType.toLowerCase(), serviceId);

        if (service !=null) {

            if (namespace != null && namespace.isEmpty()) {
                // Prevents adding empty layer namespace, put null instead
                namespace = null;
            }

            // look for layer namespace
            if (namespace == null) {
                Integer pvId = providerRepository.findIdForIdentifier(providerId);
                if (pvId == null) {
                    try {
                        pvId = Integer.parseInt(providerId);
                    } catch (NumberFormatException ex) {
                        throw new TargetNotFoundException("Unable to find provider :" + providerId);
                    }
                }
                final DataProvider provider = DataProviders.getProvider(pvId);
                if (provider != null) {
                    namespace = ProviderParameters.getNamespace(provider);
                }
            }

            Integer data = dataRepository.findIdFromProvider(namespace, name, providerId);
            if(data == null) {
                try {
                    data = Integer.parseInt(name);
                } catch (NumberFormatException ex) {
                    throw new TargetNotFoundException("Unable to find data for namespace:" + namespace+" name:"+name+" provider:"+providerId);
                }
            }

            return add(data, alias, namespace, name, service, null);
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    @Override
    @Transactional
    public Integer add(int dataId, String alias, String namespace, String name,
             int serviceId, org.constellation.dto.service.config.wxs.Layer config) throws ConfigurationException {

        final Service service = serviceBusiness.getServiceById(serviceId, null);

        if (service !=null) {

            if (namespace != null && namespace.isEmpty()) {
                // Prevents adding empty layer namespace, put null instead
                namespace = null;
            }

            Layer layer;
            if (alias != null) {
                layer = layerRepository.findByServiceIdAndAlias(serviceId, alias);
            } else {
                layer = layerRepository.findByServiceIdAndDataId(service.getId(), dataId);
            }
            boolean update = true;
            if (layer == null) {
                update = false;
                layer = new Layer();
            }
            layer.setName(name);
            layer.setNamespace(namespace);
            layer.setAlias(alias);
            layer.setService(service.getId());
            layer.setDataId(dataId);
            layer.setDate(new Date(System.currentTimeMillis()));
            Optional<CstlUser> user = userBusiness.findOne(securityManager.getCurrentUserLogin());
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

            for (int styleID : styleRepository.getStyleIdsForData(dataId)) {
                styleRepository.linkStyleToLayer(styleID, layerID);
            }

            //clear cache event
            final ClusterMessage request = clusterBusiness.createRequest(SRV_MESSAGE_TYPE_ID,false);
            request.put(KEY_ACTION, SRV_VALUE_ACTION_CLEAR_CACHE);
            request.put(SRV_KEY_TYPE, service.getType());
            request.put(KEY_IDENTIFIER, service.getIdentifier());
            clusterBusiness.publish(request);

            return layerID;
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void update(int layerID, LayerSummary summary) throws ConfigurationException {
        final Layer layer = layerRepository.findById(layerID);
        if (layer != null) {
            String title = summary.getTitle();
            if (title != null && title.isEmpty()) {
                title = null;
            }
            layer.setTitle(title);
            String alias  = summary.getAlias();
            if (alias != null && alias.isEmpty()) {
                alias = null;
            }
            layer.setAlias(alias);
            layerRepository.update(layer);

            //clear cache event
            Service serv = serviceBusiness.getServiceById(layer.getService(), null); // should never be null
            if (serv != null) {
                final ClusterMessage request = clusterBusiness.createRequest(SRV_MESSAGE_TYPE_ID,false);
                request.put(KEY_ACTION, SRV_VALUE_ACTION_CLEAR_CACHE);
                request.put(SRV_KEY_TYPE, serv.getType());
                request.put(KEY_IDENTIFIER, serv.getIdentifier());
                clusterBusiness.publish(request);
            }
        } else {
            throw new TargetNotFoundException("Unable to find a layer: {" + layerID + "}");
        }
    }

    @Override
    @Transactional
    public void remove(final Integer layerId) throws ConstellationException {
        final Layer layer = layerRepository.findById(layerId);
        if (layer != null) {
            Service serv = serviceBusiness.getServiceById(layer.getService(), null);
            removeLayer(layer.getId(), serv.getType().toLowerCase(), serv.getIdentifier());
        } else {
            throw new TargetNotFoundException("Unable to find a layer: {" + layerId + "}");
        }
    }

    @Override
    @Transactional
    public void removeForService(final Integer serviceId) throws ConstellationException {
        final Service service = serviceBusiness.getServiceById(serviceId, null);
        if (service != null) {
            final List<Layer> layers = layerRepository.findByServiceId(serviceId);
            for (Layer layer : layers) {
                removeLayer(layer.getId(), service.getType(), service.getIdentifier());
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
    }

    @Override
    @Transactional
    public void removeAll() throws ConstellationException {
        final List<Layer> layers = layerRepository.findAll();
        for (Layer layer : layers) {
            // NOT OPTIMIZED => multiple event sent
            final Service service = serviceBusiness.getServiceById(layer.getService(), null);
            if (service != null) {
                removeLayer(layer.getId(), service.getType(), service.getIdentifier());
            }

        }
    }

    /**
     * Remove an existing layer and send event to service hosting ths layer.
     * For (WMTS) pyramid layer generated from a data at publishing time, if no longer used by any service, 
     * the data will be removed (so as the files on disk).
     * 
     * @param layerId
     * @param serviceType
     * @param serviceId
     * @throws ConfigurationException 
     */
    protected void removeLayer(int layerId, String serviceType, String serviceId) throws ConstellationException {
        if ("wmts".equalsIgnoreCase(serviceType)) {
            Layer l = layerRepository.findById(layerId);
            if (l != null && l.getDataId() != null && dataRepository.getParent(l.getDataId()) != null) {
                Data d = dataRepository.findById(l.getDataId());
                if ("pyramid".equals(d.getSubtype())) {
                    boolean stillReferenced = layerRepository.findByDataId(l.getDataId()).size() - 1 > 0;
                    if (!stillReferenced) {
                        dataBusiness.removeData(l.getDataId(), true);
                    }
                }
            }
        }
        layerRepository.delete(layerId);

        //clear cache event
        final ClusterMessage request = clusterBusiness.createRequest(SRV_MESSAGE_TYPE_ID,false);
        request.put(KEY_ACTION, SRV_VALUE_ACTION_CLEAR_CACHE);
        request.put(SRV_KEY_TYPE, serviceType);
        request.put(KEY_IDENTIFIER, serviceId);
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
            final LayerSummary layerSummary = new LayerSummary();
            if (lay.getDataId() != null) {
                final Data data = dataRepository.findById(lay.getDataId());
                if (data != null) {
                    layerSummary.setName(data.getName());
                    layerSummary.setNamespace(data.getNamespace());
                    final QName fullName = new QName(lay.getNamespace(), lay.getName());
                    final DataBrief db = dataBusiness.getDataBrief(fullName, data.getProviderId());
                    if (db != null) {
                        layerSummary.setType(db.getType());
                        layerSummary.setSubtype(db.getSubtype());
                        layerSummary.setOwner(db.getOwner());
                        layerSummary.setProvider(db.getProvider());
                    }
                }
            }
            layerSummary.setId(lay.getId());
            layerSummary.setAlias(lay.getAlias());
            layerSummary.setTitle(lay.getTitle());
            layerSummary.setDate(lay.getDate());
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
    public List<NameInProvider> getLayerNames(final Integer serviceId, final String login) throws ConfigurationException {
        serviceBusiness.ensureExistingInstance(serviceId);

        final List<NameInProvider> response = new ArrayList<>();

        final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
        final List<Layer> layers   = layerRepository.findByServiceId(serviceId);
        for (Layer layer : layers) {
            final GenericName layerName = NamesExt.create(layer.getNamespace(), layer.getName());
            Date version = null;
            /* TODO how to get version?
              if (layer.getVersion() != null) {
                version = new Date(layer.getVersion());
            }*/
            final Data db = dataRepository.findById(layer.getDataId());
            if (db != null) {
                final GenericName dataName = NamesExt.create(db.getNamespace(), db.getName());
                if (securityFilter.allowed(login, layer.getId())) {
                    response.add(new NameInProvider(layer.getId(), layerName, db.getProviderId(), version, layer.getAlias(), dataName));
                }
            } else {
                LOGGER.warning("Unable to find a data (id = " + layer.getDataId() + ") for the layer:" + layerName);
            }
        }
        return response;
    }

    @Override
    public List<Integer> getLayerIds(final Integer serviceId, final String login) throws ConfigurationException {
        final List<Integer> response = new ArrayList<>();
        serviceBusiness.ensureExistingInstance(serviceId);

        final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);

        final List<Integer> layers   = layerRepository.findIdByServiceId(serviceId);
        for (Integer layer : layers) {
            if (securityFilter.allowed(login, layer)) {
                response.add(layer);
            }
        }
        return response;
    }

    /**
     * Get a single layer from its identifier.
     *
     * @param LayerId  layer identifier
     * @param login login for security check
     *
     * @return org.constellation.dto.service.config.wxs.Layer
     *
     * @throws ConfigurationException
     * @throws TargetNotFoundException if the layer does not exist.
     */
    @Override
    public org.constellation.dto.service.config.wxs.Layer getLayer(final Integer LayerId, final String login) throws ConfigurationException {
        Layer layer = layerRepository.findById(LayerId);
        if (layer != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(layer.getService());
            if (securityFilter.allowed(login, layer.getId())) {
                return toLayerConfig(layer);
            } else {
                throw new ConfigurationException("Not allowed to see this layer.");
            }
        } else {
            throw new TargetNotFoundException("Unable to find a layer:" + LayerId);
        }

    }

    @Override
    public NameInProvider getFullLayerName(final Integer serviceId, final String nameOrAlias,
                                                          final String namespace, final String login) throws ConfigurationException {
        Layer layer;
        if (namespace != null && !namespace.isEmpty()) {
            //1. search by name and namespace
            layer = layerRepository.findByServiceIdAndLayerName(serviceId, nameOrAlias, namespace);
        } else {
            //2. search by alias
            layer = layerRepository.findByServiceIdAndAlias(serviceId, nameOrAlias);

            //3. search by single name
            if  (layer == null) {
                layer = layerRepository.findByServiceIdAndLayerName(serviceId, nameOrAlias);
            }
        }

        if (layer != null) {
            final GenericName layerName = NamesExt.create(layer.getNamespace(), layer.getName());
            final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
            if (securityFilter.allowed(login, layer.getId())) {
                Date version = null;
                /* TODO how to get version?
                  if (layer.getVersion() != null) {
                    version = new Date(layer.getVersion());
                }*/
                final Data db = dataRepository.findById(layer.getDataId());
                if (db != null) {
                    final GenericName dataName = NamesExt.create(db.getNamespace(), db.getName());
                    return new NameInProvider(layer.getId(), layerName, db.getProviderId(), version, layer.getAlias(), dataName);
                } else {
                    throw new ConfigurationException("Unable to find a data (id = " + layer.getDataId() + ") for the layer:" + layerName);
                }
            } else {
                throw new ConfigurationException("Not allowed to see this layer.");
            }
        } else {
            // if layer is null, maybe the service does not exist.
            serviceBusiness.ensureExistingInstance(serviceId);
            throw new TargetNotFoundException("Unable to find a layer:" + nameOrAlias);
        }

    }

    @Override
    public NameInProvider getFullLayerName(final Integer serviceId, final Integer layerId, final String login) throws ConfigurationException {

        final Layer layer = layerRepository.findById(layerId);
        if (layer != null) {
            final GenericName layerName = NamesExt.create(layer.getNamespace(), layer.getName());
            final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
            if (securityFilter.allowed(login, layer.getId())) {
                Date version = null;
                /* TODO how to get version?
                  if (layer.getVersion() != null) {
                    version = new Date(layer.getVersion());
                }*/
                final Data db = dataRepository.findById(layer.getDataId());
                if (db != null) {
                    final GenericName dataName = NamesExt.create(db.getNamespace(), db.getName());
                    return new NameInProvider(layerId, layerName, db.getProviderId(), version, layer.getAlias(), dataName);
                } else {
                    throw new ConfigurationException("Unable to find a data (id = " + layer.getDataId() + ") for the layer:" + layerName);
                }
            } else {
                throw new ConfigurationException("Not allowed to see this layer.");
            }
        } else {
            throw new TargetNotFoundException("Unable to find a layer:" + layerId);
        }

    }

    @Override
    public FilterAndDimension getLayerFilterDimension(final Integer layerId) throws ConfigurationException {

        Layer layer = layerRepository.findById(layerId);
        if (layer != null) {
            org.constellation.dto.service.config.wxs.Layer layerConfig = readLayerConfiguration(layer.getConfig());
            if (layerConfig != null) {
                return new FilterAndDimension(layerConfig.getFilter(), layerConfig.getDimensions());
            }
        } else {
             throw new TargetNotFoundException("Unable to find a layer:" + layerId);
        }
        return new FilterAndDimension();
    }

    @Override
    public List<StyleReference> getLayerStyles(Integer layerId) throws ConfigurationException {
        if (layerId != null) {
            return styleRepository.fetchByLayerId(layerId);
        }
        return new ArrayList<>();
    }

    @Override
    public Map.Entry<Integer, List<org.constellation.dto.service.config.wxs.Layer>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) throws ConfigurationException {
        final Map.Entry<Integer, List<Layer>> entry = layerRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), toLayerConfig(entry.getValue()));
    }


    private List<org.constellation.dto.service.config.wxs.Layer> toLayerConfig(List<Layer> layers) throws ConfigurationException {
        List<org.constellation.dto.service.config.wxs.Layer> results = new ArrayList<>();
        for (Layer layer : layers) {
            results.add(toLayerConfig(layer));
        }
        return results;
    }

    /**
     *
     * @param layer
     * @return
     * @throws ConfigurationException
     */
    private org.constellation.dto.service.config.wxs.Layer toLayerConfig(Layer layer) throws ConfigurationException {
        final ProviderBrief provider  = providerRepository.findForData(layer.getDataId());
        final QName name         = new QName(layer.getNamespace(), layer.getName());
        org.constellation.dto.service.config.wxs.Layer layerConfig = readLayerConfiguration(layer.getConfig());
        if (layerConfig == null) {
            layerConfig = new org.constellation.dto.service.config.wxs.Layer(name);
            layerConfig.setTitle(layer.getTitle());
        }
        layerConfig.setId(layer.getId());

        // override with table values (TODO remove)
        layerConfig.setAlias(layer.getAlias());
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

        final List<StyleReference> styles = styleRepository.fetchByLayerId(layer.getId());
        layerConfig.getStyles().addAll(styles);
        
         // TODO layerDto.setTitle(null);
         // TODO layerDto.setVersion();
        return layerConfig;
    }

    private LayerSecurityFilter getSecurityFilter(int  serviceId) throws ConfigurationException {
        final String servImpl = serviceBusiness.getServiceImplementation(serviceId);
        final MapFactory mapfactory = getMapFactory(servImpl != null ? servImpl : "default"); // backward compatibility
        return mapfactory.getSecurityFilter();

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
    private MapFactory getMapFactory(final String impl) throws ConfigurationException {
        if (mapFactories == null) {
            mapFactories = new HashMap<>();
            final Iterator<MapFactory> ite = ServiceLoader.load(MapFactory.class).iterator();
            while (ite.hasNext()) {
                MapFactory currentFactory = ite.next();
                mapFactories.put(impl, currentFactory);
            }
        }
        if (!mapFactories.containsKey(impl)) {
            throw new ConfigurationException("No Map factory has been found for type:" + impl);
        }
        return mapFactories.get(impl);
    }
}
