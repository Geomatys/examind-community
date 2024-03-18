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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.sis.measure.Range;
import org.geotoolkit.display.PortrayalException;
import org.geotoolkit.display2d.ext.legend.DefaultLegendService;
import org.geotoolkit.display2d.ext.legend.LegendTemplate;
import org.geotoolkit.display2d.service.DefaultGlyphService;
import org.apache.sis.map.MapItem;
import org.apache.sis.map.MapLayer;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.style.Style;
import org.constellation.exception.ConstellationStoreException;
import org.geotoolkit.filter.visitor.ListingPropertyVisitor;
import org.geotoolkit.style.MutableFeatureTypeStyle;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.wms.xml.v111.LatLonBoundingBox;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.metadata.extent.GeographicBoundingBox;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class WMSUtilities {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.map.ws");

    public static BufferedImage getLegendGraphicImg(final MapItem mapItem, Dimension dimension, final LegendTemplate template,
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

    /**
     * Ensure that the data envelope is not empty. It can occurs with vector data, on a single point.
     *
     * @param inputGeoBox the box to verify.
     * @return the input box extendd if needed.
     */
    public static GeographicBoundingBox notEmptyBBOX(GeographicBoundingBox inputGeoBox) {
        final double width  = inputGeoBox.getEastBoundLongitude() - inputGeoBox.getWestBoundLongitude();
        final double height = inputGeoBox.getNorthBoundLatitude() - inputGeoBox.getSouthBoundLatitude();
        if (width == 0 && height == 0) {
            final double diffWidth = Math.nextUp(inputGeoBox.getEastBoundLongitude()) - inputGeoBox.getEastBoundLongitude();
            final double diffHeight = Math.nextUp(inputGeoBox.getNorthBoundLatitude()) - inputGeoBox.getNorthBoundLatitude();
            inputGeoBox = new LatLonBoundingBox(inputGeoBox.getWestBoundLongitude() - diffWidth,
                                                inputGeoBox.getSouthBoundLatitude() - diffHeight,
                                                Math.nextUp(inputGeoBox.getEastBoundLongitude()),
                                                Math.nextUp(inputGeoBox.getNorthBoundLatitude()));
        }
        if (width == 0) {
            final double diffWidth = Math.nextUp(inputGeoBox.getEastBoundLongitude()) - inputGeoBox.getEastBoundLongitude();
            inputGeoBox = new LatLonBoundingBox(inputGeoBox.getWestBoundLongitude() - diffWidth,
                                                inputGeoBox.getSouthBoundLatitude(),
                                                Math.nextUp(inputGeoBox.getEastBoundLongitude()),
                                                inputGeoBox.getNorthBoundLatitude());
        }
        if (height == 0) {
            final double diffHeight = Math.nextUp(inputGeoBox.getNorthBoundLatitude()) - inputGeoBox.getNorthBoundLatitude();
            inputGeoBox = new LatLonBoundingBox(inputGeoBox.getWestBoundLongitude(),
                                                inputGeoBox.getSouthBoundLatitude() - diffHeight,
                                                inputGeoBox.getEastBoundLongitude(),
                                                Math.nextUp(inputGeoBox.getNorthBoundLatitude()));
        }
        // fix for overlapping box
        if (inputGeoBox.getWestBoundLongitude() > inputGeoBox.getEastBoundLongitude()) {
            inputGeoBox = new LatLonBoundingBox(-180,
                                                 inputGeoBox.getSouthBoundLatitude(),
                                                 180,
                                                 inputGeoBox.getNorthBoundLatitude());
        }
        return inputGeoBox;
    }

     /**
     * Get all values of given extra dimension.
     * @return collection never null, can be empty.
     */
    public static List<Range> getDimensionRange(FeatureSet fs, Expression lower, Expression upper) throws ConstellationStoreException {
        try {
            final Set<String> properties = new HashSet<>();
            ListingPropertyVisitor.VISITOR.visit(lower, properties);
            ListingPropertyVisitor.VISITOR.visit(upper, properties);

            final FeatureQuery qb = new FeatureQuery();
            qb.setProjection(properties.toArray(String[]::new));
            final FeatureSet col = fs.subset(qb);

            try (Stream<Feature> stream = col.features(false)) {
                return stream
                        .map(f -> {
                            return new Range(
                                    Comparable.class,
                                    (Comparable) lower.apply(f), true,
                                    (Comparable) upper.apply(f), true
                            );
                        })
                        .toList();
            }
        } catch (DataStoreException ex) {
            throw new ConstellationStoreException(ex);
        }
    }

    public static String printValues(Set<Range> refs) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Range r : refs) {
            if (!first) {
                sb.append(",");
            }
            if (r.getMinValue().compareTo(r.getMaxValue()) != 0) {
                sb.append(r.getMinValue()).append("-").append(r.getMaxValue());
            } else {
                sb.append(r.getMinValue());
            }

            first = false;
        }
        return sb.toString();
    }
}
