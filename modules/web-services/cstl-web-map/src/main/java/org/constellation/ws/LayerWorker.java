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
package org.constellation.ws;

import org.constellation.dto.service.config.wxs.FilterAndDimension;
import org.constellation.api.ServiceDef.Specification;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.Language;
import org.constellation.dto.service.config.Languages;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.constellation.ws.security.SimplePDP;
import org.geotoolkit.style.MutableStyle;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.MessageException;
import org.constellation.business.MessageListener;
import org.constellation.dto.NameInProvider;
import org.constellation.provider.DataProvider;
import org.geotoolkit.util.NamesExt;

import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import org.opengis.util.GenericName;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.dto.StyleReference;
import org.constellation.exception.ConstellationException;

/**
 * A super class for all the web service worker dealing with layers (WMS, WCS, WMTS, WFS, ...)
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class LayerWorker extends AbstractWorker {

    @Inject
    private ILayerBusiness layerBusiness;

    @Inject
    protected IStyleBusiness styleBusiness;

    @Inject
    protected IClusterBusiness clusterBusiness;

    private LayerContext layerContext;

    protected final List<String> supportedLanguages = new ArrayList<>();

    protected final String defaultLanguage;

    private String listenerUid;

    public LayerWorker(final String id, final Specification specification) {
        super(id, specification);

        String defaultLanguageCandidate = null;

        try {
            final Object obj = serviceBusiness.getConfiguration(specification.name().toLowerCase(), id);
            if (obj instanceof LayerContext) {
                layerContext = (LayerContext) obj;
                final String sec = layerContext.getSecurity();
                // Instantiaties the PDP only if a rule has been discovered.
                if (sec != null && !sec.isEmpty()) {
                    pdp = new SimplePDP(sec);
                }
                final Languages languages = layerContext.getSupportedLanguages();
                if (languages != null) {
                    for (Language language : languages.getLanguages()) {
                        supportedLanguages.add(language.getLanguageCode());
                        if (language.getDefault()) {
                            defaultLanguageCandidate = language.getLanguageCode();
                        }
                    }
                }
                // look for capabilities cache flag
                final String cc = getProperty("cacheCapabilities");
                if (cc != null && !cc.isEmpty()) {
                    cacheCapabilities = Boolean.parseBoolean(cc);
                }

                //Check  FeatureInfo configuration (if exist)
                FeatureInfoUtilities.checkConfiguration(layerContext);

            } else {
                startError("The layer context File does not contain a layerContext object", null);
            }
        } catch (ClassNotFoundException | ConfigurationException ex) {
            startError("Custom FeatureInfo configuration error : " + ex.getMessage(), ex);
        } catch (Exception ex) {
            startError(ex.getMessage(), ex);
        }
        defaultLanguage = defaultLanguageCandidate;

    }

    @PostConstruct
    public void init(){

        //listen to changes on the providers to clear the getcapabilities cache
        listenerUid = clusterBusiness.addMessageListener(new MessageListener() {
            @Override
            protected boolean filter(ClusterMessage message) {
                return message.getMemberUID().equals(clusterBusiness.getMemberUID())
                    && PRV_MESSAGE_TYPE_ID.equals(message.getTypeId())
                    && PRV_VALUE_ACTION_UPDATED.equals(message.get(KEY_ACTION));
                // TODO : also check that impacted provider is the parent of a layer in this service.
            }

            @Override
            protected ClusterMessage process(ClusterMessage event) throws Exception, MessageException, CstlServiceException, ConfigurationException {
                refreshUpdateSequence();
                clearCapabilitiesCache();
                return null;
            }

            @Override
            protected IClusterBusiness getClusterBusiness(){
                return clusterBusiness;
            }
        });
    }

    /**
     * Removes topic listener
     */
    @PreDestroy
    @Override
    public void destroy(){
        if (listenerUid != null) {
            clusterBusiness.removeMessageListener(listenerUid);
        }
    }

    protected List<NameInProvider> getConfigurationLayerNames(final String login) {
        try {
            return layerBusiness.getLayerNames(getServiceId(), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting layers names", ex);
        }
        return new ArrayList<>();
    }

    protected FilterAndDimension getLayerFilterDimensions(final Integer layerId) {
        try {
            return layerBusiness.getLayerFilterDimension(layerId);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting filter and dimension for layer", ex);
        }
        return new FilterAndDimension();
    }

    /**
     * Look for a authorized layer matching the specified name :
     *  - by the layer alias
     *  - by the name and namespace
     *  - by the name only
     *
     * @param login user requesting the layer informations
     * @param name Generic name of the layer or alias.
     *
     * @return a complete Name indentifier of the layer or {@code null}
     */
    private NameInProvider getFullLayerName(final String login, final GenericName name) {
        if (name == null) {
            return null;
        }
        try {
            return layerBusiness.getFullLayerName(getServiceId(), name.tip().toString(), NamesExt.getNamespace(name), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.INFO, "Error while getting layer name:{0}", ex.getMessage());
        }
        return null;
    }

    protected MutableStyle getStyle(final StyleReference styleReference) throws CstlServiceException {
        MutableStyle style;
        if (styleReference != null) {
            try {
                style = (MutableStyle) styleBusiness.getStyle(styleReference.getId());
            } catch (TargetNotFoundException e) {
                throw new CstlServiceException("Style provided: " + styleReference.getName()+ " not found.", STYLE_NOT_DEFINED);
            }
        } else {
            //no defined styles, use the favorite one, let the layer get it himself.
            style = null;
        }
        return style;
    }

    protected Layer getMainLayer() {
        if (layerContext == null) {
            return null;
        }
        return layerContext.getMainLayer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(final String ip, final String referer) {
        return pdp.isAuthorized(ip, referer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecured() {
        return (pdp != null);
    }

    @Override
    protected final String getProperty(final String key) {
        if (layerContext != null && layerContext.getCustomParameters() != null) {
            return layerContext.getCustomParameters().get(key);
        }
        return null;
    }

    @Override
    public LayerContext getConfiguration() {
        return layerContext;
    }

    /**
     * Parse a Name from a string.
     * @param layerName
     * @return
     */
    protected GenericName parseCoverageName(final String layerName) {
        final GenericName namedLayerName;
        if (layerName != null && layerName.lastIndexOf(':') != -1) {
            final String namespace = layerName.substring(0, layerName.lastIndexOf(':'));
            final String localPart = layerName.substring(layerName.lastIndexOf(':') + 1);
            namedLayerName = NamesExt.create(namespace, localPart);
        } else {
            namedLayerName = NamesExt.create(layerName);
        }
        return namedLayerName;
    }

    private Data getData(NameInProvider nip){
        try {
            final DataProvider provider = DataProviders.getProvider(nip.providerID);
            if (nip.dataVersion != null) {
                LOGGER.log(Level.FINE, "Provider with name = {0} and version = {1}", new Object[]{nip.name, nip.dataVersion});
                return provider.get(nip.name, nip.dataVersion);
            }else{
                LOGGER.log(Level.FINE, "Provider with name = {0}", nip.name);
                return provider.get(nip.name);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Exception in getData() : "+ex.getMessage(), ex);
            return null;
        }
    }

    protected List<LayerCache> getLayerCaches(final String login) throws CstlServiceException {
        List<LayerCache> results = new ArrayList<>();
        try {
            List<NameInProvider> nips = layerBusiness.getLayerNames(getServiceId(), login);
            for (NameInProvider nip : nips) {
                results.add(getLayerCache(nip, login));
            }
            return results;
        } catch (ConfigurationException ex) {
            throw new CstlServiceException(ex);
        }
    }

    protected List<LayerCache> getLayerCaches(final String login, final Collection<GenericName> names) throws CstlServiceException {
        List<LayerCache> results = new ArrayList<>();
        for (GenericName name : names) {
            results.add(getLayerCache(login, name));
        }
        return results;
    }

    protected LayerCache getLayerCache(final String login, GenericName name) throws CstlServiceException {
        NameInProvider nip = getFullLayerName(login, name);
        if (nip != null) {
            return getLayerCache(nip, login);
        } else {
            throw new CstlServiceException("Unknown Layer name:" + name, LAYER_NOT_DEFINED);
        }
    }

    private LayerCache getLayerCache(NameInProvider nip, String login) throws CstlServiceException {
        Data data = getData(nip);
        if (data != null) {
            final GenericName layerName;
            if (nip.alias != null) {
                layerName = NamesExt.create(nip.alias);
            } else {
                layerName = nip.name;
            }
            List<StyleReference> styles = new ArrayList<>();
            Layer configuration;
            try {
                configuration = layerBusiness.getLayer(nip.layerId, login);
                styles.addAll(configuration.getStyles());
            } catch (ConstellationException ex) {
               throw new CstlServiceException(ex);
            }
            return new LayerCache(
                    nip.layerId,
                    layerName,
                    data,
                    styles,
                    configuration);
        } else {
            throw new CstlServiceException("Unable to find  the Layer named:{" + NamesExt.getNamespace(nip.name) + '}' + nip.name.tip().toString() + " in the provider proxy", NO_APPLICABLE_CODE);
        }
    }
}
