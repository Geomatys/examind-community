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

import org.constellation.api.ServiceDef.Specification;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.Language;
import org.constellation.dto.service.config.Languages;
import org.constellation.dto.service.config.wxs.LayerConfig;
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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import org.constellation.api.WorkerState;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.MessageException;
import org.constellation.business.MessageListener;
import org.constellation.dto.NameInProvider;
import org.constellation.provider.DataProvider;
import org.geotoolkit.util.NamesExt;

import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import org.opengis.util.GenericName;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.dto.StyleReference;
import org.constellation.exception.ConstellationException;
import org.constellation.map.featureinfo.FeatureInfoFormat;
import static org.geotoolkit.ows.xml.OWSExceptionCode.INVALID_FORMAT;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import org.springframework.lang.NonNull;

/**
 * A super class for all the web service worker dealing with layers (WMS, WCS, WMTS, WFS, ...)
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class LayerWorker extends AbstractWorker<LayerContext> {

    @Inject
    private ILayerBusiness layerBusiness;

    @Inject
    protected IStyleBusiness styleBusiness;

    @Inject
    protected IClusterBusiness clusterBusiness;

    protected final List<String> supportedLanguages = new ArrayList<>();

    private String defaultLanguage = null;

    private String listenerUid;

    public LayerWorker(final String id, final Specification specification) {
        super(id, specification);
        if (getState().equals(WorkerState.ERROR)) return;
        try {
            final String sec = configuration.getSecurity();
            // Instantiaties the PDP only if a rule has been discovered.
            if (sec != null && !sec.isEmpty()) {
                pdp = new SimplePDP(sec);
            }
            final Languages languages = configuration.getSupportedLanguages();
            if (languages != null) {
                for (Language language : languages.getLanguages()) {
                    supportedLanguages.add(language.getLanguageCode());
                    if (language.getDefault()) {
                        defaultLanguage = language.getLanguageCode();
                    }
                }
            }
            // look for capabilities cache flag
            final String cc = getProperty("cacheCapabilities");
            if (cc != null && !cc.isEmpty()) {
                cacheCapabilities = Boolean.parseBoolean(cc);
            }

            //Check  FeatureInfo configuration (if exist)
            FeatureInfoUtilities.checkConfiguration(configuration);

        } catch (ClassNotFoundException | ConfigurationException ex) {
            startError("Custom FeatureInfo configuration error : " + ex.getMessage(), ex);
        } catch (Exception ex) {
            startError(ex.getMessage(), ex);
        }
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
        super.destroy();
        if (listenerUid != null) {
            clusterBusiness.removeMessageListener(listenerUid);
        }
        stopped();
    }

    protected List<NameInProvider> getLayerNames(final String login) {
        try {
            return layerBusiness.getLayerNames(getServiceId(), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting layers names", ex);
        }
        return new ArrayList<>();
    }

    protected List<String> getStrLayerNames(final String login) {
       return getLayerNames(login).stream().map(nip -> getStrNameFromNIP(nip)).toList();
    }

    protected List<GenericName> getTypeNames(String login) {
        return getLayerNames(login).stream().map(nip -> getNameFromNIP(nip)).toList();
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

    protected LayerConfig getMainLayer() {
        if (configuration == null) {
            return null;
        }
        return configuration.getMainLayer();
    }

    protected String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Return A {@link FeatureInfoFormat} for the specified layer configuration.
     *
     * @param config A layer configuration.
     * @param infoFormat name of the format.
     *
     * @return A {@link FeatureInfoFormat} {@code never null}.
     * @throws CstlServiceException if an error occurs during featureInfo format retrieval or inf the infor format does not exist.
     */
    protected FeatureInfoFormat getFeatureInfo(LayerConfig config, String infoFormat) throws CstlServiceException {
        FeatureInfoFormat featureInfo = null;
        try {
            featureInfo = FeatureInfoUtilities.getFeatureInfoFormat(getConfiguration(), config, infoFormat);
        } catch (ClassNotFoundException | ConfigurationException ex) {
            throw new CstlServiceException(ex, NO_APPLICABLE_CODE);
        }

        if (featureInfo == null) {
            throw new CstlServiceException("INFO_FORMAT=" + infoFormat + " not supported.", INVALID_FORMAT);
        }
        return featureInfo;
    }

    private Data getData(NameInProvider nip){
        try {
            final DataProvider provider = DataProviders.getProvider(nip.providerID);
            if (nip.dataVersion != null) {
                LOGGER.log(Level.FINE, "Data with name = {0} and version = {1}", new Object[]{nip.dataName, nip.dataVersion});
                return provider.get(nip.dataName, nip.dataVersion);
            }else{
                LOGGER.log(Level.FINE, "Provider with name = {0}", nip.dataName);
                return provider.get(nip.dataName);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.INFO, "Exception in getData() : "+ex.getMessage(), ex);
            return null;
        }
    }

    protected List<LayerCache> getLayerCaches(final String login) throws CstlServiceException {
        return getLayerCaches(login, false);
    }
    
    protected List<LayerCache> getLayerCaches(final String login, boolean sort) throws CstlServiceException {
        List<LayerCache> results = new ArrayList<>();
        try {
            List<NameInProvider> nips = layerBusiness.getLayerNames(getServiceId(), login);
            for (NameInProvider nip : nips) {
                try {
                    results.add(getLayerCache(nip, login));
                } catch (CstlServiceException ex) {
                    LOGGER.log(Level.WARNING, ex.getMessage());
                }
            }
            if (sort) {
                Collections.sort(results, new LayerCacheComparator());
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

    private GenericName getNameFromNIP(NameInProvider nip) {
        if (nip.alias != null) {
            return NamesExt.create(nip.alias);
        } else {
            return nip.layerName;
        }
    }

    private String getStrNameFromNIP(NameInProvider nip) {
        if (nip.alias != null) {
            return nip.alias;
        } else {
            final String namespace = NamesExt.getNamespace(nip.layerName);
            final String localName = nip.layerName.tip().toString();
            if (namespace == null) {
                return localName;
            } else {
                return namespace + ':' + localName;
            }
        }
    }

    private LayerCache getLayerCache(NameInProvider nip, String login) throws CstlServiceException {
        Data data = getData(nip);
        if (data != null) {
            final GenericName layerName = getNameFromNIP(nip);
            List<StyleReference> styles = new ArrayList<>();
            LayerConfig configuration;
            try {
                configuration = layerBusiness.getLayer(nip.layerId, login);
                styles.addAll(configuration.getStyles());
            } catch (ConstellationException ex) {
               throw new CstlServiceException(ex);
            }
            return new LayerCache(
                    nip,
                    layerName,
                    data,
                    styles,
                    configuration);
        } else {
            throw new CstlServiceException("Unable to find  the Layer named:{" + NamesExt.getNamespace(nip.layerName) + '}' + nip.layerName.tip().toString() + " in the provider proxy", NO_APPLICABLE_CODE);
        }
    }

    /**
     * return a namespace prefixed identifier got a Layer.
     *
     * @param layer A layer cache.
     * @return A identifier including a namespace.
     */
    protected static @NonNull String identifier(@NonNull LayerCache layer) {
        final GenericName layerName = layer.getName();
        final String namespace = NamesExt.getNamespace(layerName);
        final String localName = layerName.tip().toString();
        if (namespace == null) {
            return localName;
        } else {
            return namespace + ':' + localName;
        }
    }
}
