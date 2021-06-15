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

import org.apache.sis.util.logging.Logging;
import org.constellation.api.StyleType;
import org.constellation.business.*;
import org.constellation.dto.CstlUser;
import org.constellation.dto.DataBrief;
import org.constellation.dto.Style;
import org.constellation.dto.StyleBrief;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.repository.*;
import org.geotoolkit.display2d.ext.cellular.CellSymbolizer;
import org.geotoolkit.display2d.ext.dynamicrange.DynamicRangeSymbolizer;
import org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer;
import org.geotoolkit.sld.*;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Symbolizer;
import org.opengis.util.FactoryException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isBlank;
import org.constellation.exception.ConstellationException;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.process.StyleProcessReference;
import static org.constellation.business.ClusterMessageConstant.*;
import org.apache.sis.internal.system.DefaultFactories;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import org.constellation.dto.Layer;
import org.constellation.dto.service.Service;
import org.opengis.style.StyleFactory;

/**
 * @author Bernard Fabien (Geomatys)
 * @version 0.9
 * @since 0.9
 */
@Component("exaStyleBusiness")
public class StyleBusiness implements IStyleBusiness {

    @Inject
    IUserBusiness userBusiness;

    @Inject
    StyleRepository styleRepository;

    @Inject
    DataRepository dataRepository;

    @Inject
    LayerRepository layerRepository;

    @Inject
    ServiceRepository serviceRepository;

    @Inject
    ProviderRepository providerRepository;

    @Inject
    private IDataBusiness dataBusiness;

    @Inject
    private ILayerBusiness layerBusiness;

    @Inject
    private IClusterBusiness clusterBusiness;

    @Inject
    private org.constellation.security.SecurityManager securityManager;

    private final StyleXmlIO sldParser = new StyleXmlIO();

    private static final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);
    /**
     * Logger used for debugging and event notification.
     */
    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    /**
     * There are only two style groups.
     * sld and sld_temp.
     */
    private static int nameToId(final String name) throws TargetNotFoundException {
        switch(name){
            case "sld" : return 1;
            case "sld-temp" :
            case "sld_temp" :
            case "temp" : return 2;
            default : throw new TargetNotFoundException("Style provider with name \"" + name + "\" does not exist.");
        }
    }

    /**
     * There are only two style groups.
     * sld and sld_temp.
     */
    private static String idToName(final int id) {
        switch(id){
            case 1 : return "sld";
            case 2 : return "sld_temp";
            default : throw new IllegalArgumentException("Style provider with identifier \"" + id + "\" does not exist.");
        }
    }

    @Override
    public List<StyleProcessReference> getAllStyleReferences() {
        final List<StyleProcessReference> servicePRef = new ArrayList<>();
        final List<Style> styles = styleRepository.findAll();
        if (styles != null) {
            for(final Style style : styles){
                final StyleProcessReference ref = new StyleProcessReference();
                ref.setId(style.getId());
                ref.setType(style.getType());
                ref.setName(style.getName());
                ref.setProvider(style.getProviderId());
                servicePRef.add(ref);
            }
        }
        return servicePRef;
    }

    /**
     * Ensures that a style with the specified identifier really exists from the
     * style provider with the specified identifier.
     *
     * @param providerId
     *            the style provider identifier
     * @param styleName
     *            the style identifier
     * @throws TargetNotFoundException
     *             if the style instance can't be found
     */
    private Style ensureExistingStyle(final String providerId, final String styleName) throws TargetNotFoundException {
        final int provider = nameToId(providerId);
        final Style style = styleRepository.findByNameAndProvider(provider, styleName);
        if (style == null) {
            throw new TargetNotFoundException("Style provider with identifier \"" + providerId + "\" does not contain style named \""
                    + styleName + "\".");
        }
        return style;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer createStyle(final String providerId, final org.opengis.style.Style styleI) throws ConfigurationException {
        nameToId(providerId);
        if (styleI instanceof MutableStyle) {
            MutableStyle style = (MutableStyle) styleI;
            String styleName = style.getName();
            // Proceed style name.
            if (isBlank(styleName)) {
                if (isBlank(style.getName())) {
                    throw new ConfigurationException("Unable to create/update the style. No specified style name.");
                } else {
                    styleName = style.getName();
                }
            } else {
                style.setName(styleName);
            }
            // Retrieve or not the provider instance.
            final int provider;
            try{
                provider = nameToId(providerId);
            }catch(IllegalArgumentException ex){
                throw new ConfigurationException("Unable to set the style named \"" + style.getName() + "\". Provider with id \"" + providerId
                        + "\" not found.");
            }
            final StringWriter sw = new StringWriter();
            final StyleXmlIO util = new StyleXmlIO();
            try {
                util.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
            } catch (JAXBException ex) {
                throw new ConfigurationException(ex);
            }
            final Style s = styleRepository.findByNameAndProvider(provider, styleName);
            final Integer id;
            if (s != null) {
                s.setBody(sw.toString());
                s.setName(styleName);
                s.setType(getTypeFromMutableStyle(style));
                styleRepository.update(s);
                id = s.getId();
            } else {
                Integer userId = userBusiness.findOne(securityManager.getCurrentUserLogin()).map((CstlUser input) -> input.getId()).orElse(null);
                final Style newStyle = new Style();
                newStyle.setName(styleName);
                newStyle.setProviderId(provider);
                newStyle.setType(getTypeFromMutableStyle(style));
                newStyle.setDate(new Date());
                newStyle.setBody(sw.toString());
                newStyle.setOwnerId(userId);
                id = styleRepository.create(newStyle);
            }
            return id;
        } else {
            throw new ConfigurationException("Style is not an instanceof Mutable style");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StyleBrief> getAvailableStyles(final String category) throws ConstellationException {
        return getAvailableStyles(null, category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StyleBrief> getAvailableStyles(final String providerId, final String type) throws ConstellationException {
        final Integer provider = providerId!=null ? nameToId(providerId) : null;
        final List<Style> styles;
        if (type == null) {
            if (provider==null) {
                styles = styleRepository.findAll();
            } else {
                styles = styleRepository.findByProvider(provider);
            }
        } else {
            if (provider==null) {
                styles = styleRepository.findByType(type);
            } else {
                styles = styleRepository.findByTypeAndProvider(provider, type);
            }
        }
        final List<StyleBrief> beans = new ArrayList<>();
        for (final Style style : styles) {
            final StyleBrief bean = convertToBrief(style);
            beans.add(bean);
        }
        return beans;
    }

    private StyleBrief convertToBrief(final Style style) {
        final StyleBrief bean = new StyleBrief();
        final Integer styleId = style.getId();
        bean.setId(styleId);
        bean.setName(style.getName());
        bean.setProvider(idToName(style.getProviderId()));
        bean.setType(style.getType());
        final Optional<CstlUser> userStyle = userBusiness.findById(style.getOwnerId());
        if (userStyle.isPresent()) {
            bean.setOwner(userStyle.get().getLogin());
        }
        bean.setDate(style.getDate());
        //get linked data references
        final List<DataBrief> dataList = dataBusiness.getDataRefsFromStyleId(styleId);
        bean.setDataList(dataList);

        // get linked layers references
        try {
            final List<LayerSummary> layersList = layerBusiness.getLayerSummaryFromStyleId(styleId);
            bean.setLayersList(layersList);
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, null, ex);
        }
        bean.setIsShared(style.getIsShared());

        return bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.opengis.style.Style getStyle(final String providerId, final String styleName) throws TargetNotFoundException {
        final Style style = ensureExistingStyle(providerId, styleName);
        return parseStyle(style.getName(), style.getBody());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getStyleId(final String providerId, final String styleName) throws TargetNotFoundException {
        return ensureExistingStyle(providerId, styleName).getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.opengis.style.Style getStyle(int styleId) throws TargetNotFoundException {
        Style style = styleRepository.findById(styleId);
        if (style == null) {
            throw new TargetNotFoundException("Style with id"+styleId+" not found.");
        }
        return parseStyle(style.getName(), style.getBody());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsStyle(final String providerId, final String styleName) throws TargetNotFoundException {
        //may produces TargetNotFoundException if provider does not exists
        final int provider = nameToId(providerId);

        //the provider is never null here, then check if the style exists in this provider
        final Style style = styleRepository.findByNameAndProvider(provider, styleName);
        return style != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsStyle(final int styleId) {
        return styleRepository.existsById(styleId);
    }

    @Override
    @Transactional
    public void updateStyle(final int id, final org.opengis.style.Style style) throws ConfigurationException {
        final String styleName = style.getName();
        // Proceed style name.
        if (isBlank(styleName)) {
            throw new ConfigurationException("Unable to create/update the style. No specified style name.");
        }
        final StringWriter sw = new StringWriter();
        final StyleXmlIO util = new StyleXmlIO();
        try {
            util.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
        } catch (JAXBException ex) {
            throw new ConfigurationException(ex);
        }
        final Style s = styleRepository.findById(id);
        if (s != null) {
            s.setBody(sw.toString());
            s.setType(getTypeFromMutableStyle((MutableStyle) style));
            s.setName(styleName);
            styleRepository.update(s);
        } else {
            throw new TargetNotFoundException("Style with identifier \"" + id + "\" does not exist.");
        }
    }

    @Override
    @Transactional
    public void linkToLayer(int styleId, int layerId) throws ConfigurationException {
        Layer l = layerRepository.findById(layerId);
        if (l != null) {
            final boolean styleFound = styleRepository.existsById(styleId);
            if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
            styleRepository.linkStyleToLayer(styleId, layerId);
            clearServiceCache(l.getService());
        } else {
            throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");
        }
    }

    @Override
    @Transactional
    public void unlinkToLayer(int styleId, int layerId) throws ConfigurationException {
        Layer l = layerRepository.findById(layerId);
        if (l != null) {
            final boolean styleFound = styleRepository.existsById(styleId);
            if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
            styleRepository.unlinkStyleToLayer(styleId, layerId);
            clearServiceCache(l.getService());
        } else {
            throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");
        }
    }

    @Override
    @Transactional
    public void deleteStyle(final int id) throws ConfigurationException {
        styleRepository.delete(id);
    }

    @Override
    @Transactional
    public void deleteAll() throws ConfigurationException {
        styleRepository.deleteAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void linkToData(final int styleId, final int dataId) throws ConfigurationException {
        final Boolean styleFound = styleRepository.existsById(styleId);
        final Boolean dataFound = dataRepository.existsById(dataId);
        if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
        if (!dataFound) throw new TargetNotFoundException("Data " + dataId + " can't be found from database.");
        styleRepository.linkStyleToData(styleId, dataId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void unlinkFromData(final int styleId, final int dataId) throws ConfigurationException {
        final boolean styleFound = styleRepository.existsById(styleId);
        final boolean dataFound = dataRepository.existsById(dataId);
        if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
        if (!dataFound) throw new TargetNotFoundException("Data " + dataId + " can't be found from database.");
        styleRepository.unlinkStyleToData(styleId, dataId);
    }

    @Override
    @Transactional
    public void unlinkAllFromData(int dataId) throws ConfigurationException {
        final boolean dataFound = dataRepository.existsById(dataId);
        if (!dataFound) throw new TargetNotFoundException("Data " + dataId + " can't be found from database.");
        styleRepository.unlinkAllStylesFromData(dataId);
    }

    @Override
    @Transactional
    public void unlinkAllFromLayer(int layerId) throws ConfigurationException {
        Layer l = layerRepository.findById(layerId);
        if (l != null) {
            styleRepository.unlinkAllStylesFromLayer(layerId);
            clearServiceCache(l.getService());
        } else {
            throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");
        }
    }

    /**
     * Send an event to clear the specified service cache.
     *
     * @param serviceID Service identifier.
     */
    protected void clearServiceCache(Integer serviceID) {
        Service service = serviceRepository.findById(serviceID);
        //clear cache event
        final ClusterMessage request = clusterBusiness.createRequest(SRV_MESSAGE_TYPE_ID,false);
        request.put(KEY_ACTION, SRV_VALUE_ACTION_CLEAR_CACHE);
        request.put(SRV_KEY_TYPE, service.getType());
        request.put(KEY_IDENTIFIER, service.getIdentifier());
        clusterBusiness.publish(request);
    }

    protected org.opengis.style.Style parseStyle(final String name, final String xml) {
        MutableStyle value = null;
        StringReader sr = new StringReader(xml);
        final String baseErrorMsg = "SLD Style ";
         // try UserStyle SLD 1.1
        try {
            value = sldParser.readStyle(sr, Specification.SymbologyEncoding.V_1_1_0);
            if (value != null) {
                value.setName(name);
                LOGGER.log(Level.FINE, "{0}{1} is a UserStyle SLD 1.1.0", new Object[] { baseErrorMsg, name });
                return value;
            }
        } catch (JAXBException | FactoryException ex) { /* dont log */
        } finally {
            sr = new StringReader(xml);
        }
        // try UserStyle SLD 1.0
        try {
            value = sldParser.readStyle(sr, Specification.SymbologyEncoding.SLD_1_0_0);
            if (value != null) {
                value.setName(name);
                LOGGER.log(Level.FINE, "{0}{1} is a UserStyle SLD 1.0.0", new Object[] { baseErrorMsg, name });
                return value;
            }
        } catch (JAXBException | FactoryException ex) { /* dont log */
        } finally {
            sr = new StringReader(xml);
        }
        // try SLD 1.1
        try {
            final MutableStyledLayerDescriptor sld = sldParser.readSLD(sr, Specification.StyledLayerDescriptor.V_1_1_0);
            value = getFirstStyle(sld);
            if (value != null) {
                value.setName(name);
                LOGGER.log(Level.FINE, "{0}{1} is an SLD 1.1.0", new Object[] { baseErrorMsg, name });
                return value;
            }
        } catch (JAXBException | FactoryException ex) { /* dont log */
        } finally {
            sr = new StringReader(xml);
        }
        // try SLD 1.0
        try {
            final MutableStyledLayerDescriptor sld = sldParser.readSLD(sr, Specification.StyledLayerDescriptor.V_1_0_0);
            value = getFirstStyle(sld);
            if (value != null) {
                value.setName(name);
                LOGGER.log(Level.FINE, "{0}{1} is an SLD 1.0.0", new Object[] { baseErrorMsg, name });
                return value;
            }
        } catch (JAXBException | FactoryException ex) { /* dont log */
        } finally {
            sr = new StringReader(xml);
        }
        // try FeatureTypeStyle SE 1.1
        try {
            final MutableFeatureTypeStyle fts = sldParser.readFeatureTypeStyle(sr, Specification.SymbologyEncoding.V_1_1_0);
            value = SF.style();
            value.featureTypeStyles().add(fts);
            value.setName(name);
            LOGGER.log(Level.FINE, "{0}{1} is FeatureTypeStyle SE 1.1", new Object[] { baseErrorMsg, name });
            return value;

        } catch (JAXBException | FactoryException ex) { /* dont log */
        } finally {
            sr = new StringReader(xml);
        }
        // try FeatureTypeStyle SLD 1.0
        try {
            final MutableFeatureTypeStyle fts = sldParser.readFeatureTypeStyle(sr, Specification.SymbologyEncoding.SLD_1_0_0);
            value = SF.style();
            value.featureTypeStyles().add(fts);
            value.setName(name);
            LOGGER.log(Level.FINE, "{0}{1} is an FeatureTypeStyle SLD 1.0", new Object[] { baseErrorMsg, name });
            return value;
        } catch (JAXBException | FactoryException ex) { /* dont log */
        } finally {
            sr = new StringReader(xml);
        }
        return value;
    }

    private static MutableStyle getFirstStyle(final MutableStyledLayerDescriptor sld) {
        if (sld == null)
            return null;
        for (final MutableLayer layer : sld.layers()) {
            if (layer instanceof MutableNamedLayer) {
                final MutableNamedLayer mnl = (MutableNamedLayer) layer;
                for (final MutableLayerStyle stl : mnl.styles()) {
                    if (stl instanceof MutableStyle) {
                        return (MutableStyle) stl;
                    }
                }
            } else if (layer instanceof MutableUserLayer) {
                final MutableUserLayer mnl = (MutableUserLayer) layer;
                if (mnl.styles() != null && !mnl.styles().isEmpty()) {
                    return mnl.styles().get(0);
                }
            }
        }
        return null;
    }

    private static String getTypeFromMutableStyle(final MutableStyle style) {
        for (final MutableFeatureTypeStyle fts : style.featureTypeStyles()) {
            for (final MutableRule rule : fts.rules()) {
                for (final Symbolizer symbolizer : rule.symbolizers()) {
                    // TODO: find a better strategy for style classification
                    if (symbolizer instanceof RasterSymbolizer ||
                        symbolizer instanceof CellSymbolizer ||
                        symbolizer instanceof DynamicRangeSymbolizer ||
                        symbolizer instanceof IsolineSymbolizer
                    ) {
                        return "COVERAGE";
                    }
                }
            }
        }
        return "VECTOR";
    }

    @Override
    @Transactional
    public void writeStyle(final String name, final Integer providerId, final StyleType type, final org.opengis.style.Style body) throws IOException {
        final String login = securityManager.getCurrentUserLogin();
        Style style = new Style();
        style.setBody(writeStyle((MutableStyle) body));
        style.setDate(new Date(System.currentTimeMillis()));
        style.setName(name);
        Optional<CstlUser> optionalUser = userBusiness.findOne(login);
        if(optionalUser.isPresent()) style.setOwnerId(optionalUser.get().getId());
        style.setProviderId(providerId);
        style.setType(type.name());
        styleRepository.create(style);
    }

    @Override
    public Map.Entry<Integer, List<StyleBrief>> filterAndGetBrief(final Map<String,Object> filterMap, final Map.Entry<String,String> sortEntry,final int pageNumber,final int rowsPerPage) {
        Map.Entry<Integer, List<Style>> entry = styleRepository.filterAndGet(filterMap, sortEntry, pageNumber, rowsPerPage);
        List<StyleBrief> results = new ArrayList<>();
        final List<Style> styleList = entry.getValue();
        if (styleList != null) {
            for (final Style st : styleList) {
                results.add(convertToBrief(st));
            }
        }
        return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), results);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateSharedProperty(final int id, final boolean shared) throws ConfigurationException {
        final Style style = styleRepository.findById(id);
        if (style != null) {
            styleRepository.changeSharedProperty(id, shared);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void updateSharedProperty(final List<Integer> ids, final boolean shared) throws ConfigurationException {
        for (Integer id : ids) {
            updateSharedProperty(id, shared);
        }
    }

    /**
     * Transform a {@link org.opengis.style.Style} instance into a {@link String} instance.
     *
     * @param style
     *            the style to be written
     * @return a {@link String} instance
     * @throws IOException
     *             on error while writing {@link org.opengis.style.Style} XML
     */
    private static String writeStyle(final org.opengis.style.Style style) throws IOException {
        ensureNonNull("style", style);
        final StyleXmlIO util = new StyleXmlIO();
        try {
            final StringWriter sw = new StringWriter();
            util.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
            return sw.toString();
        } catch (JAXBException ex) {
            throw new IOException("An error occurred while writing MutableStyle XML.", ex);
        }
    }

}
