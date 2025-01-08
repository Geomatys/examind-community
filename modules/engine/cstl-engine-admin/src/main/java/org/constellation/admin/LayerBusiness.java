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

import com.examind.map.factory.DefaultMapFactory;
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
import javax.xml.namespace.QName;
import org.constellation.business.ClusterMessage;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Layer;
import org.constellation.dto.NameInProvider;
import org.constellation.dto.StyleReference;
import org.constellation.dto.StyledLayerBrief;
import org.constellation.dto.service.Service;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.repository.DataRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.StyleRepository;
import org.constellation.repository.StyledLayerRepository;
import org.constellation.util.Util;
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
    private StyledLayerRepository styledLayerRepository;
    @Autowired
    protected IStyleBusiness styleBusiness;
    @Autowired
    protected LayerRepository layerRepository;
    @Autowired
    protected DataRepository dataRepository;
    @Autowired
    protected IServiceBusiness serviceBusiness;
    @Autowired
    protected org.constellation.security.SecurityManager securityManager;
    @Autowired
    protected IDataBusiness dataBusiness;
    @Autowired
    protected IClusterBusiness clusterBusiness;

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    /**
     * Lazy loaded map of {@link MapFactory} found in classLoader.
     */
    private final Map<String, MapFactory> mapFactories = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer add(int dataId, String alias, String namespace, String name, String title,
             int serviceId, LayerConfig config) throws ConfigurationException {

        final Service service = serviceBusiness.getServiceById(serviceId, null);

        if (service !=null) {

            final QName layerName;
            if (namespace != null && !namespace.isEmpty()) {
                // Prevents adding empty layer namespace, put null instead
                layerName = new QName(namespace, name);
            } else {
                layerName = new QName(name);
            }

            Layer layer = new Layer();
            layer.setName(layerName);
            layer.setAlias(alias);
            layer.setService(service.getId());
            layer.setDataId(dataId);
            layer.setDate(new Date(System.currentTimeMillis()));
            if (title != null) {
                layer.setTitle(title);
            }
            Optional<CstlUser> user = userBusiness.findOne(securityManager.getCurrentUserLogin());
            if(user.isPresent()) {
                layer.setOwnerId(user.get().getId());
            }

            final String configXml = Util.writeConfigurationObject(config);
            layer.setConfig(configXml);

            int layerID = layerRepository.create(layer);
            for (int styleID : styleRepository.getStyleIdsForData(dataId)) {
                // StyleBusiness instead of StyleRepository to use the method overriden in other projects.
                styleBusiness.linkToLayer(styleID, layerID);
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
        if (summary == null) throw new ConfigurationException("Layer summary must not be null for layer update");
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
            String config = summary.getConfig();
            if (config != null && config.isEmpty()) {
                config = null;
            }
            layer.setConfig(config);
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
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
     * Remove an existing layer and send event to service hosting this layer.
     * For pyramid layer generated from a data at publishing time, if no longer used by any service, 
     * the data will be removed (so as the files on disk).
     * 
     * @param layerId The layer identifier.
     * @param serviceType The service type (redundant but almost always already calculated)
     * @param serviceId The service identifier (redundant but almost always already calculated)
     *
     * @throws ConstellationException if a problem occurs during pyramid data removal.
     * @throws TargetNotFoundException if the layer does not exist.
     */
    protected void removeLayer(int layerId, String serviceType, String serviceId) throws ConstellationException {
        final Layer l = layerRepository.findById(layerId);
        if (l != null) {
            if (l.getDataId() != null) {
                Data d = dataRepository.findById(l.getDataId());
                if ("pyramid".equals(d.getSubtype()) && Boolean.TRUE.equals(d.getHidden())) {
                    boolean stillReferenced = layerRepository.findByDataId(l.getDataId()).size() - 1 > 0;
                    if (!stillReferenced) {
                        dataBusiness.removeData(l.getDataId(), true);
                    }
                }
            }
        } else {
            throw new TargetNotFoundException("Unable to find a layer: {" + layerId + "}");
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
    public List<LayerSummary> getLayerSummaryFromStyleId(final Integer styleId) throws ConstellationException{
        final List<Layer> layers = layerRepository.getLayersByLinkedStyle(styleId);
        return toLayerSummary(layers);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<LayerSummary> getLayerSummary(final Integer serviceId, final String login) throws ConstellationException{
        final List<LayerSummary> sumLayers = new ArrayList<>();
        if (serviceId != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
            final List<Layer> layers = layerRepository.findByServiceId(serviceId);
            for (final Layer lay : layers) {
                if (!securityFilter.allowed(login, lay.getId())) continue;
                sumLayers.add(toLayerSummary(lay));
            }
        }
        return sumLayers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LayerConfig> getLayers(final Integer serviceId, final String login) throws ConfigurationException {
        final List<LayerConfig> response = new ArrayList<>();
        if (serviceId != null) {
            final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
            final List<Layer> layers   = layerRepository.findByServiceId(serviceId);
            for (Layer layer : layers) {
                if (!securityFilter.allowed(login, layer.getId())) continue;
                LayerConfig confLayer = toLayerConfig(layer);
                if (confLayer != null) {
                    response.add(confLayer);
                }
            }
        } else {
            throw new TargetNotFoundException("Unable to find a service:" + serviceId);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLayerCount(Integer serviceId) {
        return layerRepository.countByServiceId(serviceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NameInProvider> getLayerNames(final Integer serviceId, final String login) throws ConfigurationException {
        serviceBusiness.ensureExistingInstance(serviceId);

        final List<NameInProvider> response = new ArrayList<>();

        final LayerSecurityFilter securityFilter = getSecurityFilter(serviceId);
        final List<Layer> layers   = layerRepository.findByServiceId(serviceId);
        for (Layer layer : layers) {
            final QName layerName = layer.getName();
            Date version = null;
            /* TODO how to get version?
              if (layer.getVersion() != null) {
                version = new Date(layer.getVersion());
            }*/
            final Data db = dataRepository.findById(layer.getDataId());
            if (db != null) {
                final GenericName dataName = NamesExt.create(db.getNamespace(), db.getName());
                if (securityFilter.allowed(login, layer.getId())) {
                    response.add(new NameInProvider(layer.getId(), layerName, db.getProviderId(), version, layer.getAlias(), dataName, layer.getDataId()));
                }
            } else {
                LOGGER.warning("Unable to find a data (id = " + layer.getDataId() + ") for the layer:" + layerName);
            }
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
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
     * {@inheritDoc}
     */
    @Override
    public LayerConfig getLayer(final Integer LayerId, final String login) throws ConfigurationException {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public NameInProvider getFullLayerName(final Integer serviceId, final String nameOrAlias,
                                                          final String namespace, final String login) throws ConfigurationException {
        try {
            Layer layer;
            if (namespace != null && !namespace.isEmpty()) {
                //1. search by name and namespace
                layer = layerRepository.findByServiceIdAndLayerName(serviceId, nameOrAlias, namespace);
            } else {
                //2. search by alias
                layer = layerRepository.findByServiceIdAndAlias(serviceId, nameOrAlias);

                //3. search by single name with no namespace
                if  (layer == null) {
                    layer = layerRepository.findByServiceIdAndLayerName(serviceId, nameOrAlias, true);

                    //4. search by single name (with ommited namespace)
                    if  (layer == null) {
                        layer = layerRepository.findByServiceIdAndLayerName(serviceId, nameOrAlias, false);
                    }
                }
            }

            if (layer != null) {
                final QName layerName = layer.getName();
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
                        return new NameInProvider(layer.getId(), layerName, db.getProviderId(), version, layer.getAlias(), dataName, layer.getDataId());
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
        // jooq send runtime exception when it find multiple layer
        } catch (Exception ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NameInProvider getFullLayerName(final Integer serviceId, final Integer layerId, final String login) throws ConfigurationException {
        final Layer layer = layerRepository.findById(layerId);
        if (layer != null) {
            final QName layerName = layer.getName();
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
                    return new NameInProvider(layerId, layerName, db.getProviderId(), version, layer.getAlias(), dataName, layer.getDataId());
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StyleReference> getLayerStyles(Integer layerId) throws ConfigurationException {
        if (layerId != null) {
            return styleRepository.fetchByLayerId(layerId);
        }
        return new ArrayList<>();
    }

    @Override
    public Map.Entry<Integer, List<LayerConfig>> filterAndGet(Map<String, Object> filterMap, Map.Entry<String, String> sortEntry, int pageNumber, int rowsPerPage) throws ConfigurationException {
        final Map.Entry<Integer, List<Layer>> entry = layerRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), toLayerConfig(entry.getValue()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailableAlias(Integer serviceId, String alias) {
        Integer id = layerRepository.findIdByServiceIdAndAlias(serviceId, alias);
        if (id == null) {
            id = layerRepository.findIdByServiceIdAndLayerName(serviceId, alias, true);
            return id == null;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailableName(Integer serviceId, String name, String namespace) {
        // namespace seems to be always empty instead of null in data, but in layer namespace are never empty.
        String nsmp = (namespace != null && !namespace.isEmpty()) ? namespace : null;
        Integer id = layerRepository.findIdByServiceIdAndLayerName(serviceId, name, nsmp);
        return id == null;
    }
    
    /**
     * Convert a list of {@link Layer} into a list of  {@link toLayerSummary}.
     *
     * @param layers The layers to convert.
     * @return A list of  {@link LayerConfig}
     * @throws ConfigurationException If a layer is misconfigured.
     */
    private List<LayerSummary> toLayerSummary(List<Layer> layers) throws ConstellationException {
        List<LayerSummary> results = new ArrayList<>();
        for (Layer layer : layers) {
            results.add(toLayerSummary(layer));
        }
        return results;
    }

    /**
     * Convert a {@link Layer} into a {@link toLayerSummary}.
     * @param layer The layer to convert.
     *
     * @throws ConfigurationException If the layer is misconfigured.
     */
    private LayerSummary toLayerSummary(Layer layer) throws ConstellationException {
        DataBrief db = null;
        if (layer.getDataId() != null) {
            db = dataBusiness.getDataBrief(layer.getDataId(), false, true);
        }
        String owner = userBusiness.findById(layer.getOwnerId()).map(CstlUser::getLogin).orElse(null);
        List<StyleReference> styles = styleRepository.fetchByLayerId(layer.getId());
        List<StyledLayerBrief> stBriefs = Util.convertRefIntoStyledLayerBrief(styles);
        for (StyledLayerBrief styledLayerBrief : stBriefs) {
            boolean activateStats = styledLayerRepository.getActivateStats(styledLayerBrief.getId(), layer.getId());
            styledLayerBrief.setActivateStats(activateStats);
        }
        return new LayerSummary(layer, db, owner, stBriefs);
    }

    /**
     * Convert a list of {@link Layer} into a list of  {@link LayerConfig}.
     *
     * @param layers The layers to convert.
     * @return A list of  {@link LayerConfig}
     * @throws ConfigurationException If a layer is misconfigured.
     */
    private List<LayerConfig> toLayerConfig(List<Layer> layers) throws ConfigurationException {
        List<LayerConfig> results = new ArrayList<>();
        for (Layer layer : layers) {
            results.add(toLayerConfig(layer));
        }
        return results;
    }
    
    /**
     * Convert a {@link Layer} into a {@link LayerConfig}.
     * @param layer The layer to convert.
     *
     * @throws ConfigurationException If the layer is misconfigured.
     */
    private LayerConfig toLayerConfig(Layer layer) throws ConfigurationException {
        LayerConfig layerConfig = Util.readConfigurationObject(layer.getConfig(), LayerConfig.class);
        if (layerConfig == null) {
            layerConfig = new LayerConfig(layer.getId(), layer.getName());
            layerConfig.setTitle(layer.getTitle());
        }
        layerConfig.setId(layer.getId());

        // override with table values (TODO remove)
        layerConfig.setName(layer.getName());
        layerConfig.setAlias(layer.getAlias());
        layerConfig.setDate(layer.getDate());
        layerConfig.setOwnerId(layer.getOwnerId());
        layerConfig.setDataId(layer.getDataId());
        layerConfig.setTitle(layer.getTitle());

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

    /**
     * Select the good Map factory in the available ones in function of the dataSource type.
     *
     * @param impl implementation name.
     * @return
     */
    private synchronized MapFactory getMapFactory(final String impl) throws ConfigurationException {
        if (mapFactories.isEmpty()) {
            final Iterator<MapFactory> ite = ServiceLoader.load(MapFactory.class).iterator();
            while (ite.hasNext()) {
                MapFactory currentFactory = ite.next();
                mapFactories.put(impl, currentFactory);
            }
        }
        if (!mapFactories.containsKey(impl)) {
            // fallback when META_INF fails on us
            if ("default".equals(impl)) {
                LOGGER.warning("MapFactory META-INF loading fails");
                return new DefaultMapFactory();
            }
            throw new ConfigurationException("No Map factory has been found for type:" + impl);
        }
        return mapFactories.get(impl);
    }
}
