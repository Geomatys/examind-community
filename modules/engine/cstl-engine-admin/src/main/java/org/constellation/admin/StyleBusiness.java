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

import org.constellation.api.DataType;
import org.constellation.api.StatisticState;
import org.constellation.business.*;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.dto.*;
import org.constellation.dto.service.config.wxs.LayerSummary;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.repository.*;
import org.geotoolkit.display2d.ext.cellular.CellSymbolizer;
import org.geotoolkit.display2d.ext.dynamicrange.DynamicRangeSymbolizer;
import org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.opengis.style.RasterSymbolizer;
import org.opengis.style.Symbolizer;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
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
import org.constellation.dto.service.Service;
import org.geotoolkit.style.StyleUtilities;
import org.opengis.sld.StyledLayerDescriptor;
import org.opengis.style.StyleFactory;

/**
 * @author Bernard Fabien (Geomatys)
 * @version 0.9
 * @since 0.9
 */
@Component("exaStyleBusiness")
public class StyleBusiness implements IStyleBusiness {

    @Autowired
    private IUserBusiness userBusiness;

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    private ILayerBusiness layerBusiness;

    @Autowired
    private IClusterBusiness clusterBusiness;

    @Autowired
    private StyledLayerRepository styledLayerRepository;

    @Autowired
    private org.constellation.security.SecurityManager securityManager;

    private final StyleXmlIO sldParser = new StyleXmlIO();

    private static final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);
    /**
     * Logger used for debugging and event notification.
     */
    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

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
        return switch (id) {
            case 1  -> "sld";
            case 2  -> "sld_temp";
            default -> throw new IllegalArgumentException("Style provider with identifier \"" + id + "\" does not exist.");
        };
    }

    @Override
    public List<StyleProcessReference> getAllStyleReferences(String providerId) throws TargetNotFoundException {
        final List<StyleProcessReference> results = new ArrayList<>();
        final List<Style> styles;
        if (providerId != null) {
            styles = styleRepository.findByProvider(nameToId(providerId));
        } else {
            styles = styleRepository.findAll();
        }
        for(final Style style : styles){
            final StyleProcessReference ref = new StyleProcessReference();
            ref.setId(style.getId());
            ref.setType(style.getType());
            ref.setName(style.getName());
            ref.setProvider(style.getProviderId());
            results.add(ref);
        }
        return results;
    }

    /**
     * Ensures that a style with the specified identifier really exists from the
     * style provider with the specified identifier.
     *
     * @param providerId The style provider identifier (sld or sld_temp).
     * @param styleName The style name.
     * @throws TargetNotFoundException if the style instance can't be found.
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
        if (styleI instanceof MutableStyle style) {
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
            final int provider = nameToId(providerId);

            final String xmlStyle = writeStyle(sldParser, styleI);

            Integer userId = userBusiness.findOne(securityManager.getCurrentUserLogin()).map((CstlUser input) -> input.getId()).orElse(null);
            final Style newStyle = new Style();
            newStyle.setName(styleName);
            newStyle.setProviderId(provider);
            newStyle.setType(getTypeFromMutableStyle(style));
            newStyle.setDate(new Date());
            newStyle.setBody(xmlStyle);
            newStyle.setOwnerId(userId);
            return styleRepository.create(newStyle);
        } else {
            throw new ConfigurationException("Style is not an instanceof Mutable style");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public  StyleBrief getStyleBrief(final int styleId) throws TargetNotFoundException {
        final Style style = styleRepository.findById(styleId);
        if (style == null) {
            throw new TargetNotFoundException("Style with id" + styleId + " not found.");
        }

        return convertToBrief(style);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StyleBrief> getAvailableStyles(final String type) throws ConstellationException {
        return getAvailableStyles(null, type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StyleBrief> getAvailableStyles(final String providerId, final String type) throws ConstellationException {
        final Integer provider = providerId != null ? nameToId(providerId) : null;
        final List<Style> styles;
        if (type == null) {
            if (provider == null) {
                styles = styleRepository.findAll();
            } else {
                styles = styleRepository.findByProvider(provider);
            }
        } else {
            if (provider == null) {
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
        final List<DataBrief> dataList = dataBusiness.getDataFromStyleId(styleId);
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
        return parseStyle(style.getName(), style.getBody(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getStyleId(final String providerId, final String styleName) throws TargetNotFoundException {
        final int provider = nameToId(providerId);
        final Integer styleId = styleRepository.findIdByNameAndProvider(provider, styleName);
        if (styleId == null) {
            throw new TargetNotFoundException("Style provider with identifier \"" + providerId + "\" does not contain style named \"" + styleName + "\".");
        }
        return styleId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.opengis.style.Style getStyle(int styleId) throws TargetNotFoundException {
        Style style = styleRepository.findById(styleId);
        if (style == null) {
            throw new TargetNotFoundException("Style with id" + styleId + " not found.");
        }
        return parseStyle(style.getName(), style.getBody(), null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsStyle(final String providerId, final String styleName) throws TargetNotFoundException {
        //may produces TargetNotFoundException if provider does not exists
        final int provider = nameToId(providerId);

        //the provider is never null here, then check if the style exists in this provider
        final Integer styleId = styleRepository.findIdByNameAndProvider(provider, styleName);
        return styleId != null;
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
        ensureNonNull("Style", style);
        final String styleName = style.getName();
        // Proceed style name.
        if (isBlank(styleName)) {
            throw new ConfigurationException("Unable to create/update the style. No specified style name.");
        }

        final Style s = styleRepository.findById(id);

        if (s != null) {
            final String xmlStyle = writeStyle(sldParser, style);
            s.setBody(xmlStyle);
            s.setType(getTypeFromMutableStyle((MutableStyle) style));
            s.setName(styleName);
            styleRepository.update(s);

            // Force statistics and state to null for each StyledLayer linked to this style.
            // The cron on @LayerStatisticsJob will recompute the statistics for each layer.
            List<StyledLayer> styledLayers = styledLayerRepository.findByStyleId(id);
            for (StyledLayer styledLayer : styledLayers) {
                if (styledLayer.getActivateStats()) {
                    final Integer layer = styledLayer.getLayer();
                    styledLayerRepository.updateStatistics(id, layer, null, null);
                }
            }
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
            if (Application.getBooleanProperty(AppProperty.LAYER_ACTIVATE_STATISTICS, Boolean.FALSE)) {
                final Data data = dataRepository.findById(l.getDataId());
                if (data == null) {
                    LOGGER.warning("Data " + l.getDataId() + " can't be found from database." +
                            "\n Can't activate statistics computation for layer " + layerId + " with style " + styleId);
                } else {
                    if ((DataType.VECTOR.name().equals(data.getType()) || DataType.COVERAGE.name().equals(data.getType()))) {
                        // check Service type
                        final Service service = serviceRepository.findById(l.getService());
                        if ("wms".equalsIgnoreCase(service.getType())) {
                            styledLayerRepository.updateActivateStats(styleId, layerId, true);
                        }
                    }
                }
            }
            clearServiceCache(l.getService());
        } else {
            throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");
        }
    }

    @Override
    @Transactional
    public void updateActivateStatsForLayerAndStyle(final int styleId, final int layerId, final boolean activateStats) throws TargetNotFoundException {
        Layer l = layerRepository.findById(layerId);
        if (l != null) {
            final boolean styleFound = styleRepository.existsById(styleId);
            if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
            styledLayerRepository.updateActivateStats(styleId, layerId, activateStats);
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
    public void setDefaultStyleToLayer(int styleId, int layerId) throws ConfigurationException {
        Layer l = layerRepository.findById(layerId);
        if (l != null) {
            final boolean styleFound = styleRepository.existsById(styleId);
            if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
            styleRepository.setDefaultStyleToLayer(styleId, layerId);
            clearServiceCache(l.getService());
        } else {
            throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteStyle(final int id) throws ConfigurationException {
        return styleRepository.delete(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int deleteAll() throws ConfigurationException {
        return styleRepository.deleteAll();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public org.opengis.style.Style parseStyle(final String styleName, final Object source, final String fileName) {
        MutableStyle value = null;
        final String baseErrorMsg = "SLD Style ";
         // 1. try UserStyle
        try {
            value = (MutableStyle) readStyle(source, false);
            if (value != null) {
                if (styleName != null) value.setName(styleName);
                LOGGER.log(Level.FINE, "{0}{1} is a UserStyle", new Object[] { baseErrorMsg, styleName });
                return value;
            }
        } catch (ConstellationException ex) { /* no exception should be throw has we set throwEx to false*/ }
        // 2. try SLD
        try {
            final StyledLayerDescriptor sld = readSLD(source, false);
            List<MutableStyle> styles = StyleUtilities.getStylesFromSLD(sld);
            if (!styles.isEmpty()) {
                value = styles.remove(0);
                if (styleName != null && !styleName.isEmpty()) value.setName(styleName);
                LOGGER.log(Level.FINE, "{0}{1} is an SLD", new Object[] { baseErrorMsg, styleName });
                logIgnoredStyles(styles);
                return value;
            }
        } catch (ConstellationException ex) { /* no exception should be throw has we set throwEx to false*/ }
        // 3.1 try FeatureTypeStyle SE 1.1
        try {
            final MutableFeatureTypeStyle fts = sldParser.readFeatureTypeStyle(source, Specification.SymbologyEncoding.V_1_1_0);
            value = SF.style();
            value.featureTypeStyles().add(fts);
            if (styleName != null && !styleName.isEmpty()) value.setName(styleName);
            LOGGER.log(Level.FINE, "{0}{1} is FeatureTypeStyle SE 1.1", new Object[] { baseErrorMsg, styleName });
            return value;

        } catch (JAXBException | FactoryException ex) { /* dont log */ }
        // 3.2 try FeatureTypeStyle SLD 1.0
        try {
            final MutableFeatureTypeStyle fts = sldParser.readFeatureTypeStyle(source, Specification.SymbologyEncoding.SLD_1_0_0);
            value = SF.style();
            value.featureTypeStyles().add(fts);
            if (styleName != null && !styleName.isEmpty()) value.setName(styleName);
            LOGGER.log(Level.FINE, "{0}{1} is an FeatureTypeStyle SLD 1.0", new Object[] { baseErrorMsg, styleName });
            return value;
        } catch (JAXBException | FactoryException ex) { /* dont log */ }
        // 4 try to build a style from palette
        try {
            if (fileName != null) {
                if (source instanceof String str) {
                    value = StyleUtilities.getStyleFromPalette(fileName, str);
                } else if (source instanceof byte[] buffer) {
                    value = StyleUtilities.getStyleFromPalette(fileName, buffer);
                }
                if (value != null) {
                    if (styleName != null && !styleName.isEmpty()) value.setName(styleName);
                    LOGGER.log(Level.FINE, "{0}{1} is an Palette", new Object[] { baseErrorMsg, styleName });
                    return value;
                }
            }
        } catch (IOException ex) { /* dont log */ }
        return value;
    }

    private static void logIgnoredStyles(List<MutableStyle> styles) {
        //log styles which have been ignored
        if(!styles.isEmpty()){
            final StringBuilder sb = new StringBuilder("Ignored styles at import :");
            for(MutableStyle ms : styles){
                sb.append(' ').append(ms.getName());
            }
            LOGGER.log(Level.FINEST, sb.toString());
        }
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
        styleRepository.changeSharedProperty(id, shared);
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
     * @param style The style to be written.
     * @return a {@link String} instance
     * @throws IOException On error while writing {@link org.opengis.style.Style} XML
     */
    private static String writeStyle(final StyleXmlIO sldParser, final org.opengis.style.Style style) throws ConfigurationException {
        ensureNonNull("style", style);
        try {
            final StringWriter sw = new StringWriter();
            sldParser.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
            return sw.toString();
        } catch (JAXBException ex) {
            throw new ConfigurationException("An error occurred while writing MutableStyle XML.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StyledLayerDescriptor readSLD(final Object sldSrc, boolean throwEx) throws ConstellationException {
        StyledLayerDescriptor sld = null;
        try {
            sld = sldParser.readSLD(sldSrc, Specification.StyledLayerDescriptor.V_1_0_0);
        } catch (JAXBException ex) {
            // If a JAXBException occurs it can be because it is not parsed in the
            // good version. Let's just continue with the other version.
            LOGGER.finest(ex.getLocalizedMessage());
        } catch (FactoryException ex) {
            if (throwEx) {
                throw new ConstellationException(ex);
            }
        }
        if (sld == null) {
            try {
                sld = sldParser.readSLD(sldSrc, Specification.StyledLayerDescriptor.V_1_1_0);
            } catch (JAXBException | FactoryException ex) {
                if (throwEx) {
                    throw new ConstellationException(ex);
                }
            }
        }
        return sld;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StyledLayerDescriptor readSLD(Object sldSrc, String sldVersion) throws ConstellationException {
        try {
            Specification.StyledLayerDescriptor version = Specification.StyledLayerDescriptor.version(sldVersion);
            return sldParser.readSLD(sldSrc, version);
        } catch (JAXBException | FactoryException ex) {
            throw new ConstellationException("Error while reading sld.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.opengis.style.Style readStyle(final Object sldSrc, boolean throwEx) throws ConstellationException {
        org.opengis.style.Style style = null;
        try {
            style = sldParser.readStyle(sldSrc, Specification.SymbologyEncoding.V_1_1_0);
        } catch (JAXBException ex) {
            // If a JAXBException occurs it can be because it is not parsed in the
            // good version. Let's just continue with the other version.
            LOGGER.finest(ex.getLocalizedMessage());
        } catch (FactoryException ex) {
            if (throwEx) {
                throw new ConstellationException(ex);
            }
        }
        if (style == null) {
            try {
                style = sldParser.readStyle(sldSrc, Specification.SymbologyEncoding.SLD_1_0_0);
            } catch (JAXBException | FactoryException ex) {
                if (throwEx) {
                    throw new ConstellationException(ex);
                }
            }
        }
        return style;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.opengis.style.Style readStyle(final Object styleSrc, final String seVersion) throws ConstellationException {
        try {
            Specification.SymbologyEncoding version = Specification.SymbologyEncoding.version(seVersion);
            return sldParser.readStyle(styleSrc, version);
        } catch (JAXBException | FactoryException ex) {
            throw new ConstellationException("Error while reading sld.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String getExtraInfoForStyleAndLayer(final Integer styleId, final Integer layerId) throws ConstellationException {
        final Boolean styleFound = styleRepository.existsById(styleId);
        final Boolean layerFound = layerRepository.existsById(layerId);
        if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
        if (!layerFound) throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");

        final StyledLayer styledLayer = styleRepository.getStyledLayer(styleId, layerId);
        if (styledLayer == null) {
            throw new TargetNotFoundException("The layer " + layerId + " is not linked to the style + " + styleId + ".");
        }
        if (!styledLayer.getActivateStats()) {
            throw new ConstellationException("The statistics computation is not activated for layer " + layerId + " with style + " + styleId + ".");
        }
        final String statsState = styledLayer.getStatsState();
        if (statsState == null ) {
            throw new ConstellationException("The statistics have not been computed yet for layer " + layerId + " with style + " + styleId + ".");
        }
        if (!StatisticState.STATE_COMPLETED.equals(statsState)) {
            throw new ConstellationException("The statistics computation has not been performed for layer " + layerId + " with style + " + styleId + "." +
                    "\nThe status is " + statsState);
        }
        return styledLayer.getExtraInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void addExtraInfoForStyleAndLayer(final Integer styleId, final Integer layerId, final String extraInfo) throws TargetNotFoundException {
        final Boolean styleFound = styleRepository.existsById(styleId);
        final Boolean layerFound = layerRepository.existsById(layerId);
        if (!styleFound) throw new TargetNotFoundException("Style " + styleId + " can't be found from database.");
        if (!layerFound) throw new TargetNotFoundException("Layer " + layerId + " can't be found from database.");
        styleRepository.addExtraInfo(styleId, layerId, extraInfo);
    }
}
