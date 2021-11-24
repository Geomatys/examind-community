/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.map.core;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.ext.legend.DefaultLegendService;
import org.geotoolkit.display2d.ext.legend.LegendTemplate;
import org.geotoolkit.display2d.service.DefaultGlyphService;
import org.apache.sis.portrayal.MapItem;
import org.apache.sis.portrayal.MapLayer;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.opengis.style.Style;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class WMSUtilities {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.map.ws");

    public static BufferedImage getLegendGraphic(final MapItem mapItem, Dimension dimension, final LegendTemplate template,
                                          final Style style, final String rule, final Double scale)
                                          throws PortrayalException
    {

        if (!(style instanceof MutableStyle) || !(mapItem instanceof MapLayer)) {
            return DefaultLegendService.portray(template, mapItem, dimension);
        }
        final MutableStyle mutableStyle = (MutableStyle) style;
        final MapLayer maplayer         = (MapLayer) mapItem;

        if (template == null) {
            if (dimension == null) {
                dimension = DefaultGlyphService.glyphPreferredSize(mutableStyle, dimension, null);
            }
            // If a rule is given, we try to find the matching one in the style.
            // If none matches, then we can just apply all the style.
            if (rule != null) {
                final MutableRule mr = findRuleByNameInStyle(rule, mutableStyle);
                if (mr != null) {
                    return DefaultGlyphService.create(mr, dimension, maplayer);
                }
            }
            // Otherwise, if there is a scale, we can filter rules.
            if (scale != null) {
                final MutableRule mr = findRuleByScaleInStyle(scale, mutableStyle);
                if (mr != null) {
                    return DefaultGlyphService.create(mr, dimension, maplayer);
                }
            }
            return DefaultGlyphService.create(mutableStyle, dimension, maplayer);
        }
        try {
            return DefaultLegendService.portray(template, mapItem, dimension);
        } catch (PortrayalException ex) {
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }

        if (dimension == null) {
            dimension = DefaultGlyphService.glyphPreferredSize(mutableStyle, dimension, null);
        }
        // If a rule is given, we try to find the matching one in the style.
        // If none matches, then we can just apply all the style.
        if (rule != null) {
            final MutableRule mr = findRuleByNameInStyle(rule, mutableStyle);
            if (mr != null) {
                return DefaultGlyphService.create(mr, dimension, maplayer);
            }
        }
        // Otherwise, if there is a scale, we can filter rules.
        if (scale != null) {
            final MutableRule mr = findRuleByScaleInStyle(scale, mutableStyle);
            if (mr != null) {
                return DefaultGlyphService.create(mr, dimension, maplayer);
            }
        }
        return DefaultGlyphService.create(mutableStyle, dimension, maplayer);
    }

        /**
     * Returns the {@linkplain MutableRule rule} which matches with the given name, or {@code null}
     * if none.
     *
     * @param ruleName The rule name to try finding in the given style.
     * @param ms The style for which we want to extract the rule.
     * @return The rule with the given name, or {@code null} if no one matches.
     */
    private static MutableRule findRuleByNameInStyle(final String ruleName, final MutableStyle ms) {
        if (ruleName == null) {
            return null;
        }
        for (final MutableFeatureTypeStyle mfts : ms.featureTypeStyles()) {
            for (final MutableRule mutableRule : mfts.rules()) {
                if (ruleName.equals(mutableRule.getName())) {
                    return mutableRule;
                }
            }
        }
        return null;
    }

    /**
     * Returns the {@linkplain MutableRule rule} which can be applied for the given scale, or
     * {@code null} if no rules can be used at this scale.
     *
     * @param scale The scale.
     * @param ms The style for which we want to extract a rule for the given scale.
     * @return The first rule for the given scale that can be applied, or {@code null} if no
     *         one matches.
     */
    private static MutableRule findRuleByScaleInStyle(final Double scale, final MutableStyle ms) {
        if (scale == null) {
            return null;
        }
        for (final MutableFeatureTypeStyle mfts : ms.featureTypeStyles()) {
            for (final MutableRule mutableRule : mfts.rules()) {
                if (scale < mutableRule.getMaxScaleDenominator() &&
                    scale > mutableRule.getMinScaleDenominator())
                {
                    return mutableRule;
                }
            }
        }
        return null;
    }

}
