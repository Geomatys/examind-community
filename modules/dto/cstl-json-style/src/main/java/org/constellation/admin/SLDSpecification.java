/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2024 Geomatys.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.constellation.business.IStyleConverterBusiness;
import org.constellation.business.StyleSpecification;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.json.binding.Style;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.sld.StyledLayerDescriptor;
import org.geotoolkit.sld.xml.Specification;
import org.geotoolkit.sld.xml.StyleXmlIO;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import org.geotoolkit.style.StyleUtilities;
import org.opengis.util.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
@Component("sld")
public final class SLDSpecification implements StyleSpecification<MutableStyle> {

    private final StyleXmlIO sldParser = new StyleXmlIO();
    private static final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;

    @Autowired
    private IStyleConverterBusiness styleConverterBusiness;

    @Override
    public String getName() {
        return "sld";
    }

    @Override
    public Class<MutableStyle> getStyleClass() {
        return MutableStyle.class;
    }

    @Override
    public Set<String> getTemplates() {
        return Set.of("empty");
    }

    @Override
    public MutableStyle create(String template) {
        return SF.style(SF.pointSymbolizer());
    }

    @Override
    public String encode(MutableStyle style) throws ConfigurationException {
        try {
            final StringWriter sw = new StringWriter();
            sldParser.writeStyle(sw, style, Specification.StyledLayerDescriptor.V_1_1_0);
            return sw.toString();
        } catch (JAXBException ex) {
            throw new ConfigurationException("An error occurred while writing MutableStyle XML.", ex);
        }
    }

    @Override
    public MutableStyle decode(String source) throws ConfigurationException {
        MutableStyle value = null;
         // 1. try UserStyle
        try {
            value = (MutableStyle) readStyle(source, false);
            if (value != null) {
                return value;
            }
        } catch (ConstellationException ex) { /* no exception should be throw has we set throwEx to false*/ }
        // 2. try SLD
        try {
            final StyledLayerDescriptor sld = readSLD(source, false);
            List<MutableStyle> styles = StyleUtilities.getStylesFromSLD(sld);
            if (!styles.isEmpty()) {
                value = styles.remove(0);
                return value;
            }
        } catch (ConstellationException ex) { /* no exception should be throw has we set throwEx to false*/ }
        // 3.1 try FeatureTypeStyle SE 1.1
        try {
            final MutableFeatureTypeStyle fts = sldParser.readFeatureTypeStyle(source, Specification.SymbologyEncoding.V_1_1_0);
            value = GO2Utilities.STYLE_FACTORY.style();
            value.featureTypeStyles().add(fts);
            return value;

        } catch (JAXBException | FactoryException ex) { /* dont log */ }
        // 3.2 try FeatureTypeStyle SLD 1.0
        try {
            final MutableFeatureTypeStyle fts = sldParser.readFeatureTypeStyle(source, Specification.SymbologyEncoding.SLD_1_0_0);
            value = GO2Utilities.STYLE_FACTORY.style();
            value.featureTypeStyles().add(fts);
            return value;
        } catch (JAXBException | FactoryException ex) { /* dont log */ }
        // 4 try to build a style from palette
        try {
            value = StyleUtilities.getStyleFromPalette(source, source);
            if (value != null) {
                return value;
            }
        } catch (IOException ex) { /* dont log */ }
        return value;
    }

    private MutableStyle readStyle(final String sldSrc, boolean throwEx) throws ConfigurationException {
       MutableStyle style = null;
        try {
            style = sldParser.readStyle(sldSrc, Specification.SymbologyEncoding.V_1_1_0);
        } catch (JAXBException ex) {
            // If a JAXBException occurs it can be because it is not parsed in the
            // good version. Let's just continue with the other version.
        } catch (FactoryException ex) {
            if (throwEx) {
                throw new ConfigurationException(ex);
            }
        }
        if (style == null) {
            try {
                style = sldParser.readStyle(sldSrc, Specification.SymbologyEncoding.SLD_1_0_0);
            } catch (JAXBException | FactoryException ex) {
                if (throwEx) {
                    throw new ConfigurationException(ex);
                }
            }
        }
        return style;
    }

    private StyledLayerDescriptor readSLD(final Object sldSrc, boolean throwEx) throws ConstellationException {
        StyledLayerDescriptor sld = null;
        try {
            sld = sldParser.readSLD(sldSrc, Specification.StyledLayerDescriptor.V_1_0_0);
        } catch (JAXBException ex) {
            // If a JAXBException occurs it can be because it is not parsed in the
            // good version. Let's just continue with the other version.
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

    @Override
    public void deleteResources(MutableStyle style) throws ConfigurationException {
        //nothing to delete
    }

    @Override
    public Map.Entry<String,Object> exportToEdition(MutableStyle style, String subPath) throws ConfigurationException {
        final Style json = styleConverterBusiness.getJsonStyle(style);
        try {
            return new AbstractMap.SimpleImmutableEntry<>("application/json", new ObjectMapper().writeValueAsString(json));
        } catch (JsonProcessingException ex) {
            throw new ConfigurationException("Failed to encode style in JSON",ex);
        }
    }

    @Override
    public MutableStyle importFromEdition(MutableStyle style, String subPath, Object json) throws ConfigurationException {
        try {
            final Style jsonStyle = new ObjectMapper().readValue(String.valueOf(json), Style.class);
            return org.constellation.json.util.StyleUtilities.type(jsonStyle);
        } catch (JsonProcessingException ex) {
            throw new ConfigurationException("Failed to parse json style", ex);
        }
    }
}
