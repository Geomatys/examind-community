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
import org.constellation.util.DataReference;
import org.constellation.ws.security.SimplePDP;
import org.geotoolkit.factory.FactoryNotFoundException;
import org.geotoolkit.style.MutableStyle;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.PreDestroy;
import org.constellation.admin.ProviderMessageConsumer;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.MessageException;
import org.constellation.business.MessageListener;
import org.constellation.dto.NameInProvider;
import org.constellation.provider.DataProvider;
import org.constellation.util.Util;
import org.geotoolkit.util.NamesExt;

import static org.geotoolkit.ows.xml.OWSExceptionCode.LAYER_NOT_DEFINED;
import static org.geotoolkit.ows.xml.OWSExceptionCode.NO_APPLICABLE_CODE;
import static org.geotoolkit.ows.xml.OWSExceptionCode.STYLE_NOT_DEFINED;
import org.opengis.util.GenericName;

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
        isStarted = true;

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

                applySupportedVersion();
            } else {
                startError = "The layer context File does not contain a layerContext object";
                isStarted  = false;
                LOGGER.log(Level.WARNING, startError);
            }
        } catch (FactoryNotFoundException ex) {
            startError = ex.getMessage();
            isStarted  = false;
            LOGGER.log(Level.WARNING, startError, ex);
        } catch (ClassNotFoundException | ConfigurationException ex) {
            startError = "Custom FeatureInfo configuration error : " + ex.getMessage();
            isStarted  = false;
            LOGGER.log(Level.WARNING, startError, ex);
        } catch (CstlServiceException ex) {
            startError = "Error applying supported versions : " + ex.getMessage();
            isStarted  = false;
            LOGGER.log(Level.WARNING, startError, ex);
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
                    && ProviderMessageConsumer.MESSAGE_TYPE_ID.equals(message.getTypeId())
                    && ProviderMessageConsumer.VALUE_ACTION_UPDATED.equals(message.get(ProviderMessageConsumer.KEY_ACTION));
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

    protected List<Layer> getConfigurationLayers(final String login, final List<GenericName> layerNames) {
        final List<Layer> layerConfigs = new ArrayList<>();
        for (GenericName layerName : layerNames) {
            Layer l = getConfigurationLayer(layerName, login);
            layerConfigs.add(l);
        }
        return layerConfigs;
    }

    protected Layer getConfigurationLayer(final GenericName layerName, final String login) {
        if (layerName != null && layerName.tip().toString()!= null) {
            final QName qname = new QName(NamesExt.getNamespace(layerName), layerName.tip().toString());
            return getConfigurationLayer(qname, login);
        }
        return null;
    }

    protected Layer getConfigurationLayer(final QName layerName, final String login) {

        try {
            return layerBusiness.getLayer(this.specification.name(), getId(), layerName.getLocalPart(), layerName.getNamespaceURI(), login);
        } catch (ConfigurationException e) {
            LOGGER.log(Level.FINE, "No layer is exactly named as queried. Search using alias will start now", e);
        }

        if (layerName != null) {
            final List<Layer> layers = getConfigurationLayers(login);
            for (Layer layer : layers) {
                if (layer.getName().equals(layerName) || (layer.getAlias() != null && layer.getAlias().equals(layerName.getLocalPart()))) {
                    return layer;
                }
            }
            // we do a second round with missing namespace search
            for (Layer layer : layers) {
                if (layer.getName().getLocalPart().equals(layerName.getLocalPart())) {
                    return layer;
                }
            }
        }
        return null;
    }

    protected List<QName> getConfigurationLayerNames(final String login) {
        try {
            return layerBusiness.getLayerNames(this.specification.name().toLowerCase(), getId(), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting layers names", ex);
        }
        return new ArrayList<>();
    }

    protected FilterAndDimension getLayerFilterDimensions(final GenericName layerName, final String login) {
        try {
            return layerBusiness.getLayerFilterDimension(this.specification.name().toLowerCase(), getId(), layerName.tip().toString(), NamesExt.getNamespace(layerName), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting filter and dimension for layer", ex);
        }
        return new FilterAndDimension();
    }

    /**
     * @param login
     *
     * @return map of additional informations for each layer declared in the
     * layer context.
     */
    public List<Layer> getConfigurationLayers(final String login) {
        try {
            return layerBusiness.getLayers(getServiceId(), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Error while getting layers", ex);
        }
        return new ArrayList<>();
    }


    /**
     * Return all layers details in LayerProviders from there names.
     * @param login
     * @param layerNames
     * @return a list of LayerDetails
     * @throws CstlServiceException
     */
    protected List<Data> getLayerReferences(final String login, final Collection<GenericName> layerNames) throws CstlServiceException {
        final List<Data> layerRefs = new ArrayList<>();
        for (GenericName layerName : layerNames) {
            layerRefs.add(getLayerReference(login, layerName));
        }
        return layerRefs;
    }

    protected Data getLayerReference(final Layer layer) throws CstlServiceException {
        return getData(new NameInProvider(NamesExt.create(layer.getName()), layer.getProviderID(), null, layer.getAlias()));
    }

    protected Data getLayerReference(final String login, final QName layerName) throws CstlServiceException {
        return getLayerReference(login, NamesExt.create(layerName));
    }

    /**
     * Search layer real name and return the LayerDetails from LayerProvider.
     * @param login
     * @param layerName
     * @return a LayerDetails
     * @throws CstlServiceException
     */
    protected Data getLayerReference(final String login, final GenericName layerName) throws CstlServiceException {
        LOGGER.log(Level.FINE, "Login = {0} ; layerName = {1}", new Object[]{login, layerName});
        final Data layerRef;
        final NameInProvider fullName = layersContainsKey(login, layerName);
        if (fullName != null) {
            layerRef = getData(fullName);
            if (layerRef == null) {
                throw new CstlServiceException("Unable to find  the Layer named:{"+NamesExt.getNamespace(layerName) + '}' + layerName.tip().toString()+ " in the provider proxy", NO_APPLICABLE_CODE);
            }
        } else {
            throw new CstlServiceException("Unknown Layer name:" + layerName, LAYER_NOT_DEFINED);
        }
        return layerRef;
    }

    private NameInProvider getFullLayerName(final GenericName layerName, final String login) {
        try {
            return layerBusiness.getFullLayerName(this.specification.name().toLowerCase(), getId(), layerName.tip().toString(), NamesExt.getNamespace(layerName), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.INFO, "Error while getting layer name:{0}", ex.getMessage());
        }
        return null;
    }

    private NameInProvider getFullLayerName(final QName layerName, final String login) {
        try {
            return layerBusiness.getFullLayerName(this.specification.name().toLowerCase(), getId(), layerName.getLocalPart(), layerName.getNamespaceURI(), login);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.INFO, "Error while getting layer name:{0}", ex.getMessage());
        }
        return null;
    }
    /**
     * We can't use directly layers.containsKey because it may miss the namespace or the alias.
     * @param login
     * @param name
     */
    protected NameInProvider layersContainsKey(final String login, final GenericName name) {
        if (name == null) {
            return null;
        }
        NameInProvider directLayer = getFullLayerName(name, login);
        if (directLayer == null) {

            final List<QName> layerNames = getConfigurationLayerNames(login);
            if (layerNames == null) {
                return null;
            }

            //search with only localpart
            for (QName layerName : layerNames) {
                if (layerName.getLocalPart().equals(name.tip().toString())) {
                    return getFullLayerName(layerName, login);
                }
            }

            //search in alias if any
            for (QName l : layerNames) {
                final NameInProvider layer = getFullLayerName(l, login);
                if (layer.alias != null && !layer.alias.isEmpty()) {
                    final String alias = layer.alias;
                    if (alias.equals(name.tip().toString())) {
                        return layer;
                    }
                }
            }

            return null;
        }
        return directLayer;
    }

    protected MutableStyle getStyle(final DataReference styleReference) throws CstlServiceException {
        MutableStyle style;
        if (styleReference != null) {
            try {
                style = (MutableStyle) styleBusiness.getStyle(styleReference.getProviderId(), Util.getLayerId(styleReference).tip().toString());
            } catch (TargetNotFoundException e) {
                throw new CstlServiceException("Style provided: " + styleReference.getReference() + " not found.", STYLE_NOT_DEFINED);
            }
        } else {
            //no defined styles, use the favorite one, let the layer get it himself.
            style = null;
        }
//        final MutableStyle style;
//        if (styleName != null) {
//            //try to grab the style if provided
//            //a style has been given for this layer, try to use it
//            style = StyleProviders.getInstance().get(styleName.getLayerId().getLocalPart(), styleName.getProviderId());
//            if (style == null) {
//                throw new CstlServiceException("Style provided: " + styleName.getReference() + " not found.", STYLE_NOT_DEFINED);
//            }
//        } else {
//            //no defined styles, use the favorite one, let the layer get it himself.
//            style = null;
//        }
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
        final DataProvider provider;
        try {
            provider = DataProviders.getProvider(nip.providerID);
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.INFO, "Exception in getData() : "+ex.getMessage(), ex);
            return null;
        }
        if (nip.dataVersion != null) {
            LOGGER.log(Level.FINE, "Provider with name = {0} and version = {1}", new Object[]{nip.name, nip.dataVersion});
            return provider.get(nip.name, nip.dataVersion);
        }else{
            LOGGER.log(Level.FINE, "Provider with name = {0}", nip.name);
            return provider.get(nip.name);
        }
    }
}
