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

package org.constellation.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.measure.Unit;
import org.apache.sis.cql.CQL;
import org.apache.sis.cql.CQLException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureQuery;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.DefaultInternationalString;
import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.Static;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.json.binding.AutoIntervalValues;
import org.constellation.json.binding.AutoUniqueValues;
import org.constellation.json.binding.ChannelSelection;
import org.constellation.json.binding.ChartDataModel;
import org.constellation.json.binding.DynamicRangeSymbolizer;
import org.constellation.json.binding.IsolineSymbolizer;
import org.constellation.json.binding.LineSymbolizer;
import org.constellation.json.binding.PointSymbolizer;
import org.constellation.json.binding.PolygonSymbolizer;
import org.constellation.json.binding.RasterSymbolizer;
import org.constellation.json.binding.SelectedChannelType;
import org.constellation.json.binding.Style;
import org.constellation.json.binding.StyleElement;
import org.constellation.json.binding.Symbolizer;
import static org.constellation.json.util.StyleFactories.FF;
import org.geotoolkit.display2d.GO2Utilities;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.internal.InternalUtilities;
import org.geotoolkit.style.DefaultDescription;
import org.geotoolkit.style.DefaultLineSymbolizer;
import org.geotoolkit.style.DefaultPointSymbolizer;
import org.geotoolkit.style.DefaultPolygonSymbolizer;
import org.geotoolkit.style.MutableRule;
import org.geotoolkit.style.MutableStyle;
import org.geotoolkit.style.MutableStyleFactory;
import static org.geotoolkit.style.StyleConstants.DEFAULT_DESCRIPTION;
import static org.geotoolkit.style.StyleConstants.DEFAULT_DISPLACEMENT;
import static org.geotoolkit.style.StyleConstants.DEFAULT_UOM;
import static org.geotoolkit.style.StyleConstants.MARK_CIRCLE;
import org.geotoolkit.style.interval.DefaultIntervalPalette;
import org.geotoolkit.style.interval.IntervalPalette;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Filter;
import org.opengis.filter.Expression;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.NilOperator;
import org.opengis.filter.ValueReference;
import org.opengis.style.Fill;
import org.opengis.style.Graphic;
import org.opengis.style.GraphicalSymbol;
import org.opengis.style.Mark;
import org.opengis.style.Stroke;
import org.opengis.style.StyleFactory;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class StyleUtilities extends Static {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.json.util");

    public static final Function<String, Color> COLOR_CONVERTER;
    static {
        Function<String, Color> tmp;
        try {
            tmp = (Function<String, Color>) ObjectConverters.find(String.class, Color.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot find a converter from text to AWT color. Fallback on opaque color interpretation", e);
            tmp = Color::decode;
        }

        COLOR_CONVERTER = tmp;
    }
    private static Expression nil() {
        return FF.literal(null);
    }

    /**
     * Parse given string as CQL and returns Expression.
     */
    public static Expression parseExpression(final String exp) {
        try{
            return CQL.parseExpression(exp);
        } catch (CQLException ex) {
            return nil();
        }
    }

    /**
     * Convert expression to String CQL
     * if exp is NilExpression then returns empty string because NilExpression is not supported by CQL
     * @param exp {Expression exp}
     * @return {String}
     */
    public static String toCQL(final Expression exp) {
        if(exp instanceof NilOperator) {
            return "";
        } else {
            return CQL.write(exp);
        }
    }

    /**
     * Convert filter to String CQL.
     * if the filter contains NilExpression then return null literal because Nil is not supported by CQL
     * @param f {Filter}
     * @return {String}
     */
    public static String toCQL(Filter f) {
        return CQL.write(f);
    }

    public static Expression opacity(final double opacity) {
        return (opacity >= 0 && opacity <= 1.0) ? FF.literal(opacity) : nil();
    }

    public static Expression literal(final Object value) {
        return value != null ? FF.literal(value) : nil();
    }

    public static <T> T type(final StyleElement<T> elt) {
        return elt != null ? elt.toType() : null;
    }

    public static <T> List<T> singletonType(final StyleElement<T> elt) {
        return elt != null ? Collections.singletonList(elt.toType()) : new ArrayList<>(0);
    }

    public static <T> List<T> listType(final List<? extends StyleElement<T>> elts) {
        final List<T> list = new ArrayList<>();
        if (elts == null) {
            return list;
        }
        for (final StyleElement<T> elt : elts) {
            list.add(elt.toType());
        }
        return list;
    }

    public static Filter filter(final String filter) {
        if (filter == null) {
            return null;
        }
        try {
            return CQL.parseFilter(filter);
        } catch (CQLException ex) {
            LOGGER.log(Level.WARNING, "An error occurred during filter parsing.", ex);
        }
        return null;
    }

    public static String writeJson(final Object object) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    public static <T> T readJson(final String json, final Class<T> clazz) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, clazz);
    }

    public static String toHex(final Color color) {
        String redCode = Integer.toHexString(color.getRed());
        String greenCode = Integer.toHexString(color.getGreen());
        String blueCode = Integer.toHexString(color.getBlue());
        if (redCode.length() == 1)      redCode = "0" + redCode;
        if (greenCode.length() == 1)    greenCode = "0" + greenCode;
        if (blueCode.length() == 1)     blueCode = "0" + blueCode;

        int alpha = color.getAlpha();
        if(alpha != 255){
            String alphaCode = Integer.toHexString(alpha);
            if (alphaCode.length() == 1) alphaCode = "0" + alphaCode;
            return "#" + alphaCode + redCode + greenCode + blueCode;
        }else{
            return "#" + redCode + greenCode + blueCode;
        }
    }

    public static final Symbolizer DEFAULT_POINT_SYMBOLIZER        = new PointSymbolizer();
    public static final Symbolizer DEFAULT_LINE_SYMBOLIZER         = new LineSymbolizer();
    public static final Symbolizer DEFAULT_POLYGON_SYMBOLIZER      = new PolygonSymbolizer();
    public static final RasterSymbolizer DEFAULT_GREY_RASTER_SYMBOLIZER  = new RasterSymbolizer();
    public static final RasterSymbolizer DEFAULT_RGB_RASTER_SYMBOLIZER   = new RasterSymbolizer();
    public static final DynamicRangeSymbolizer DEFAULT_RGB_DYNAMICRANGE_SYMBOLIZER   = new DynamicRangeSymbolizer();
    static {
        final ChannelSelection rgbSelection = new ChannelSelection();
        final SelectedChannelType red   = new SelectedChannelType();
        final SelectedChannelType green = new SelectedChannelType();
        final SelectedChannelType blue  = new SelectedChannelType();
        red.setName("0");
        green.setName("1");
        blue.setName("2");
        rgbSelection.setRgbChannels(new SelectedChannelType[]{red, green, blue});
        DEFAULT_RGB_RASTER_SYMBOLIZER.setChannelSelection(rgbSelection);

        final ChannelSelection greySelection = new ChannelSelection();
        final SelectedChannelType grey = new SelectedChannelType();
        grey.setName("0");
        greySelection.setGreyChannel(grey);
        DEFAULT_GREY_RASTER_SYMBOLIZER.setChannelSelection(greySelection);
    }
    public static final IsolineSymbolizer DEFAULT_ISOLIGNE_SYMBOLIZER   = new IsolineSymbolizer();

    /**
     * Determines if the {@link Class} passed in arguments is assignable
     * to a JTS {@link Geometry}.
     *
     * @param clazz the {@link Class}
     * @return {@code true} if the class is assignable to the expected type
     */
    public static boolean isAssignableToGeometry(final Class<?> clazz) {
        return Geometry.class.isAssignableFrom(clazz);
    }

    /**
     * Determines if the {@link Class} passed in arguments is assignable
     * to a JTS polygon {@link Geometry}.
     *
     * @param clazz the {@link Class} to test
     * @return {@code true} if the class is assignable to the expected type
     */
    public static boolean isAssignableToPolygon(final Class<?> clazz) {
        return Polygon.class.isAssignableFrom(clazz) || MultiPolygon.class.isAssignableFrom(clazz);
    }

    /**
     * Determines if the {@link Class} passed in arguments is assignable
     * to a JTS line {@link Geometry}.
     *
     * @param clazz the {@link Class}
     * @return {@code true} if the class is assignable to the expected type
     */
    public static boolean isAssignableToLine(final Class<?> clazz) {
        return LineString.class.isAssignableFrom(clazz) || MultiLineString.class.isAssignableFrom(clazz)
                || LinearRing.class.isAssignableFrom(clazz);
    }

    /**
     * Determines if the {@link Class} passed in arguments is assignable
     * to a JTS point {@link Geometry}.
     *
     * @param clazz the {@link Class}
     * @return {@code true} if the class is assignable to the expected type
     */
    public static boolean isAssignableToPoint(final Class<?> clazz) {
        return Point.class.isAssignableFrom(clazz) || MultiPoint.class.isAssignableFrom(clazz);
    }

    public static Optional<String> stringify(Unit<?> unit) {
        if (unit == null) return Optional.empty();
        final String symbol = unit.getSymbol();
        if (symbol == null) {
            LOGGER.warning("Unit has no symbol and cannot be expressed as a String");
            return Optional.empty();
        }
        return Optional.of(symbol);
    }

    public static org.opengis.style.Symbolizer createSymbolizer(final String symbolizerType) {
        final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
        final FilterFactory FF = GO2Utilities.FILTER_FACTORY;
        final org.opengis.style.Symbolizer symbolizer;
        if ("polygon".equals(symbolizerType)) {
            final Stroke stroke = SF.stroke(Color.BLACK, 1);
            final Fill fill = SF.fill(Color.BLUE);
            symbolizer = new DefaultPolygonSymbolizer(
                    stroke,
                    fill,
                    DEFAULT_DISPLACEMENT,
                    FF.literal(0),
                    DEFAULT_UOM,
                    null,
                    "polygon",
                    DEFAULT_DESCRIPTION);
        } else if ("line".equals(symbolizerType)) {
            final Stroke stroke = SF.stroke(Color.BLUE, 2);
            symbolizer = new DefaultLineSymbolizer(
                    stroke,
                    FF.literal(0),
                    DEFAULT_UOM,
                    null,
                    "line",
                    DEFAULT_DESCRIPTION);
        } else {
            final Stroke stroke = SF.stroke(Color.BLACK, 1);
            final Fill fill = SF.fill(Color.BLUE);
            final List<GraphicalSymbol> symbols = new ArrayList<>();
            symbols.add(SF.mark(MARK_CIRCLE, fill, stroke));
            final Graphic gra = SF.graphic(symbols, FF.literal(1), FF.literal(12), FF.literal(0), SF.anchorPoint(), SF.displacement());
            symbolizer = new DefaultPointSymbolizer(
                    gra,
                    DEFAULT_UOM,
                    null,
                    "point",
                    DEFAULT_DESCRIPTION);
        }
        return symbolizer;
    }

    public static org.opengis.style.Symbolizer derivateSymbolizer(final org.opengis.style.Symbolizer symbol, final Color color) {
        final MutableStyleFactory SF = GO2Utilities.STYLE_FACTORY;
        if (symbol instanceof org.opengis.style.PolygonSymbolizer ps) {
            final Fill fill = SF.fill(SF.literal(color), ps.getFill().getOpacity());
            return SF.polygonSymbolizer(ps.getName(), ps.getGeometryPropertyName(),
                    ps.getDescription(), ps.getUnitOfMeasure(), ps.getStroke(),
                    fill, ps.getDisplacement(), ps.getPerpendicularOffset());
        } else if (symbol instanceof org.opengis.style.LineSymbolizer ls) {
            final Stroke oldStroke = ls.getStroke();
            final Stroke stroke = SF.stroke(SF.literal(color), oldStroke.getOpacity(), oldStroke.getWidth(),
                    oldStroke.getLineJoin(), oldStroke.getLineCap(), oldStroke.getDashArray(), oldStroke.getDashOffset());
            return SF.lineSymbolizer(ls.getName(), ls.getGeometryPropertyName(),
                    ls.getDescription(), ls.getUnitOfMeasure(), stroke, ls.getPerpendicularOffset());
        } else if (symbol instanceof org.opengis.style.PointSymbolizer ps) {
            final Graphic oldGraphic = ps.getGraphic();
            final Mark oldMark = (Mark) oldGraphic.graphicalSymbols().get(0);
            final Fill fill = SF.fill(SF.literal(color), oldMark.getFill().getOpacity());
            final List<GraphicalSymbol> symbols = new ArrayList<>();
            symbols.add(SF.mark(oldMark.getWellKnownName(), fill, oldMark.getStroke()));
            final Graphic graphic = SF.graphic(symbols, oldGraphic.getOpacity(), oldGraphic.getSize(),
                    oldGraphic.getRotation(), oldGraphic.getAnchorPoint(), oldGraphic.getDisplacement());
            return SF.pointSymbolizer(graphic, ps.getGeometryPropertyName());
        } else {
            throw new IllegalArgumentException("Unexpected symbolizer type: " + symbol);
        }
    }

    public static Expression getPaletteFunction(MutableStyle style, String ruleName) throws TargetNotFoundException{
        final List<MutableRule> mutableRules = new ArrayList<>();
        if (!style.featureTypeStyles().isEmpty()) {
            mutableRules.addAll(style.featureTypeStyles().get(0).rules());
        }
        // search related rule
        Expression function = null;
        boolean ruleFound = false;
        search:
        for (final MutableRule mutableRule : mutableRules) {
            if (mutableRule.getName().equalsIgnoreCase(ruleName)) {
                ruleFound = true;
                for (final org.opengis.style.Symbolizer symbolizer : mutableRule.symbolizers()) {
                    // search raster symbolizer and return function
                    if (symbolizer instanceof org.opengis.style.RasterSymbolizer rasterSymbolizer) {
                        if (rasterSymbolizer.getColorMap() != null){
                            function = rasterSymbolizer.getColorMap().getFunction();
                            break search;
                        }
                    } else if (symbolizer instanceof org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer isolineSymbolizer) {
                        if (isolineSymbolizer.getLineSymbolizer() != null &&
                            isolineSymbolizer.getLineSymbolizer().getStroke() != null &&
                            isolineSymbolizer.getLineSymbolizer().getStroke().getColor() instanceof Expression) {
                            function = (Expression) isolineSymbolizer.getLineSymbolizer().getStroke().getColor();
                            break search;
                        }
                    }
                }
                break search;
            }
        }
        if (!ruleFound) {
            throw new TargetNotFoundException("Rule :" + ruleName + " not found.");
        }
        return function;
    }

    public static org.opengis.style.Style generateAutoIntervalStyle(AutoIntervalValues intervalValues, Style originalStyle, Resource dataRes) throws ConstellationException {

        final String attribute = intervalValues.getAttr();
        if (attribute ==null || attribute.trim().isEmpty()){
            throw new ConstellationException("Attribute field should not be empty!");
        }

        final String method = intervalValues.getMethod();
        final int intervals = intervalValues.getNbIntervals();

        final String symbolizerType = intervalValues.getSymbol();
        final List<String> colorsList = intervalValues.getColors();

        //rules that will be added to the style
        final List<MutableRule> newRules = new ArrayList<>();

        /*
         * I - Get feature type and feature data.
         */
        if (dataRes instanceof FeatureSet fs) {

           /*
            * II - Search extreme values.
            */
            final Set<Double> values = new HashSet<>();
            double minimum = Double.POSITIVE_INFINITY;
            double maximum = Double.NEGATIVE_INFINITY;

            final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);
            final FilterFactory FF = FilterUtilities.FF;

            final ValueReference property = FF.property(attribute);

            final FeatureQuery query = new FeatureQuery();
            query.setProjection(attribute);

            try (final Stream<Feature> featureSet = fs.subset(query).features(false)) {
                Iterator<Feature> it = featureSet.iterator();
                while(it.hasNext()){
                    final Feature feature = it.next();
                    final Number number = (Number) property.apply(feature);
                    final Double value = number.doubleValue();
                    values.add(value);
                    if (value < minimum) {
                        minimum = value;
                    }
                    if (value > maximum) {
                        maximum = value;
                    }
                }
            } catch (DataStoreException ex) {
                throw new ConstellationStoreException("Error while iterating on featureset", ex);
            }

            /*
            * III - Analyze values.
            */
            final Double[] allValues = values.toArray(Double[]::new);
            double[] interValues = new double[0];
            if ("equidistant".equals(method)) {
                interValues = new double[intervals + 1];
                for (int i = 0; i < interValues.length; i++) {
                    interValues[i] = minimum + (float) i / (interValues.length - 1) * (maximum - minimum);
                }
            } else if ("mediane".equals(method)) {
                interValues = new double[intervals + 1];
                for (int i = 0; i < interValues.length; i++) {
                    interValues[i] = allValues[i * (allValues.length - 1) / (interValues.length - 1)];
                }
            } else {
                if (interValues.length != intervals + 1) {
                    interValues = Arrays.copyOf(interValues, intervals + 1);
                }
            }

            /*
            * IV - Generate rules deriving symbolizer with given colors.
            */
            final org.opengis.style.Symbolizer symbolizer = createSymbolizer(symbolizerType);
            final Color[] colors = new Color[colorsList.size()];
            int loop = 0;
            for(final String c : colorsList){
                colors[loop] = new Color(InternalUtilities.parseColor(c));
                loop++;
            }
            final IntervalPalette palette = new DefaultIntervalPalette(colors);
            int count = 0;

            /*
             * Create one rule for each interval.
             */
            for (int i = 1; i < interValues.length; i++) {
                final double step = (double) (i - 1) / (interValues.length - 2); // derivation step
                double start = interValues[i - 1];
                double end = interValues[i];
                /*
                * Create the interval filter.
                */
                final Filter above = FF.greaterOrEqual(property, FF.literal(start));
                final Filter under;
                if (i == interValues.length - 1) {
                    under = FF.lessOrEqual(property, FF.literal(end));
                } else {
                    under = FF.less(property, FF.literal(end));
                }
                final Filter interval = FF.and(above, under);
                /*
                * Create new rule deriving the base symbolizer.
                */
                final MutableRule rule = SF.rule();
                rule.setName((count++)+" - AutoInterval - " + property.getXPath());
                rule.setDescription(new DefaultDescription(new DefaultInternationalString(property.getXPath()+" "+start+" - "+end),null));
                rule.setFilter(interval);
                rule.symbolizers().add(derivateSymbolizer(symbolizer, palette.interpolate(step)));
                newRules.add(rule);
            }
        }

        //add rules to the style
        final MutableStyle mutableStyle = StyleUtilities.type(originalStyle);
        //remove all auto intervals rules if exists before adding the new list.
        final List<MutableRule> backupRules = new ArrayList<>(mutableStyle.featureTypeStyles().get(0).rules());
        final List<MutableRule> rulesToRemove = new ArrayList<>();
        for(final MutableRule r : backupRules){
            if(r.getName().contains("AutoInterval")){
                rulesToRemove.add(r);
            }
        }
        backupRules.removeAll(rulesToRemove);
        mutableStyle.featureTypeStyles().get(0).rules().clear();
        mutableStyle.featureTypeStyles().get(0).rules().addAll(backupRules);
        mutableStyle.featureTypeStyles().get(0).rules().addAll(newRules);

        return mutableStyle;
    }

    public static org.opengis.style.Style generateAutoUniqueStyle(AutoUniqueValues autoUniqueValues, Style originalStyle, Resource dataRes) throws ConstellationException {

        final String attribute = autoUniqueValues.getAttr();
        if (attribute == null || attribute.trim().isEmpty()){
            throw new ConstellationException("Attribute field should not be empty!");
        }

        final String symbolizerType = autoUniqueValues.getSymbol();
        final List<String> colorsList = autoUniqueValues.getColors();

        //rules that will be added to the style
        final List<MutableRule> newRules = new ArrayList<>();

        /*
         * I - Get feature type and feature data.
         */
        if (dataRes instanceof FeatureSet fs) {

            /*
            * II - Extract all different values.
            */
            final MutableStyleFactory SF = (MutableStyleFactory) DefaultFactories.forBuildin(StyleFactory.class);
            final FilterFactory FF = FilterUtilities.FF;
            final ValueReference property = FF.property(attribute);
            final List<Object> differentValues = new ArrayList<>();

            final FeatureQuery query = new FeatureQuery();
            query.setProjection(attribute);

            try (final Stream<Feature> featureSet = fs.subset(query).features(false)) {
                Iterator<Feature> it = featureSet.iterator();
                while(it.hasNext()){
                    final Feature feature = it.next();
                    final Object value = property.apply(feature);
                    if (!differentValues.contains(value)) {
                        differentValues.add(value);
                    }
                }
            } catch (DataStoreException ex) {
                throw new ConstellationStoreException("Error while iterating on featureset", ex);
            }

            /*
            * III - Generate rules deriving symbolizer with colors array.
            */
            final org.opengis.style.Symbolizer symbolizer = createSymbolizer(symbolizerType);
            final Color[] colors = new Color[colorsList.size()];
            int loop = 0;
            for(final String c : colorsList){
                colors[loop] = new Color(InternalUtilities.parseColor(c));
                loop++;
            }
            final IntervalPalette palette = new DefaultIntervalPalette(colors);
            int count = 0;
            /*
            * Create one rule for each different value.
            */
            for (int i = 0; i < differentValues.size(); i++) {
                final double step = ((double) i) / (differentValues.size() - 1); // derivation step
                final Object value = differentValues.get(i);
                /*
                 * Create the unique value filter.
                 */
                final Filter filter;
                if(value instanceof String && !value.toString().isEmpty() && value.toString().contains("'")){
                    final String val = ((String) value).replaceAll("'","\\"+"'");
                    filter = FF.like(property, FF.literal(val).toString(), '*', '?', '\\', true);
                }else {
                    filter = FF.equal(property, FF.literal(value));
                }

                /*
                 * Create new rule derivating the base symbolizer.
                 */
                final MutableRule rule = SF.rule(derivateSymbolizer(symbolizer, palette.interpolate(step)));
                rule.setName((count++)+" - AutoUnique - " + property.getXPath());
                final Object valStr = value instanceof String && ((String) value).isEmpty() ? "''":value;
                rule.setDescription(new DefaultDescription(new DefaultInternationalString(property.getXPath()+" = "+valStr),null));
                rule.setFilter(filter);
                newRules.add(rule);
            }
        }

        //add rules to the style
        final MutableStyle mutableStyle = StyleUtilities.type(originalStyle);
        //remove all auto unique values rules if exists before adding the new list.
        final List<MutableRule> backupRules = new ArrayList<>(mutableStyle.featureTypeStyles().get(0).rules());
        final List<MutableRule> rulesToRemove = new ArrayList<>();
        for(final MutableRule r : backupRules){
            if(r.getName().contains("AutoUnique")){
                rulesToRemove.add(r);
            }
        }
        backupRules.removeAll(rulesToRemove);
        mutableStyle.featureTypeStyles().get(0).rules().clear();
        mutableStyle.featureTypeStyles().get(0).rules().addAll(backupRules);
        mutableStyle.featureTypeStyles().get(0).rules().addAll(newRules);

        return mutableStyle;
    }

    public static ChartDataModel generateChartData(final String attribute, final int intervals, Resource dataRes) throws ConstellationException {
        final ChartDataModel result = new ChartDataModel();

        if (dataRes instanceof FeatureSet fs) {

            final Map<Object,Long> mapping = new LinkedHashMap<>();
            final FilterFactory FF = FilterUtilities.FF;
            final ValueReference property = FF.property(attribute);

            final FeatureQuery query = new FeatureQuery();
            query.setProjection(attribute);
            try {
                fs = fs.subset(query);

                //check if property is numeric
                // if it is numeric then proceed to create intervals as the keys and get for each interval the feature count.
                // otherwise put each (string value, count) into the map

                final PropertyType p = fs.getType().getProperty(attribute);
                if (p instanceof AttributeType at) {
                    final Class cl = at.getValueClass();
                    result.setNumberField(Number.class.isAssignableFrom(cl));
                }

                try (final Stream<Feature> featureSet = fs.features(false)) {

                    if (result.isNumberField()) {
                        final Set<Double> values = new HashSet<>();
                        Iterator<Feature> it = featureSet.iterator();
                        while (it.hasNext()) {
                            final Feature feature = it.next();
                            final Number number = (Number) property.apply(feature);
                            if (number != null) {
                                final Double value = number.doubleValue();
                                values.add(value);
                            }
                        }

                        final Double[] allValues = values.toArray(Double[]::new);
                        double maximum = 0, minimum = 0;
                        if (allValues.length > 0) {
                            Arrays.sort(allValues);
                            minimum = allValues[0];
                            maximum = allValues[allValues.length-1];
                        }

                        result.setMinimum(minimum);
                        result.setMaximum(maximum);

                        double[] interValues = new double[intervals + 1];
                        for (int i = 0; i < interValues.length; i++) {
                            interValues[i] = minimum + ((maximum - minimum) * i / (interValues.length - 1))  ;
                        }

                        for (int i = 1; i < interValues.length; i++) {
                            double start = interValues[i - 1];
                            double end = interValues[i];
                            FeatureQuery qb = new FeatureQuery();
                            final Filter above = FF.greaterOrEqual(property, FF.literal(start));
                            final Filter under;
                            if (i == interValues.length - 1) {
                                under = FF.lessOrEqual(property, FF.literal(end));
                            } else {
                                under = FF.less(property, FF.literal(end));
                            }
                            final Filter interval = FF.and(above, under);
                            qb.setSelection(interval);
                            try (final Stream<Feature> subCol = fs.subset(qb).features(false)) {
                                mapping.put((long) start + " - " + (long) end, subCol.count());
                            }
                        }
                    } else {
                        Iterator<Feature> it = featureSet.iterator();
                        while(it.hasNext()){
                            final Feature feature = it.next();
                            Object value = property.apply(feature);
                            if(value == null){
                                value = "null";
                            }
                            Long count = mapping.get(value);
                            if(mapping.get(value)!=null){
                                count++;
                                mapping.put(value,count);
                            }else {
                                mapping.put(value,1L);
                            }
                        }

                        //adjust mapping size for performance in client side issue.
                        final Set<Object> keys = mapping.keySet();
                        final Map<Object,Long> newmap = new LinkedHashMap<>();
                        int limit = 100;
                        if(keys.size()>limit){
                            int gap = keys.size()/limit;
                            int i=1;
                            for(final Object key : keys){
                                if(i== gap){
                                    newmap.put(key,mapping.get(key));
                                    i=1;//reset i
                                }else {
                                    i++;//skip the key and increase i
                                }
                            }
                            mapping.clear();
                            mapping.putAll(newmap);
                        }
                    }

                }
                result.setMapping(mapping);
            } catch (DataStoreException ex) {
                throw new ConstellationException(ex);
            }
        }
        return result;
    }
}
