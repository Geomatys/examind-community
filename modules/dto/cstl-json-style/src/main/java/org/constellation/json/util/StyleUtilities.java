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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.cql.CQLException;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;
import org.constellation.json.binding.ChannelSelection;
import org.constellation.json.binding.DynamicRangeSymbolizer;
import org.constellation.json.binding.IsolineSymbolizer;
import org.constellation.json.binding.LineSymbolizer;
import org.constellation.json.binding.PointSymbolizer;
import org.constellation.json.binding.PolygonSymbolizer;
import org.constellation.json.binding.RasterSymbolizer;
import org.constellation.json.binding.SelectedChannelType;
import org.constellation.json.binding.StyleElement;
import org.constellation.json.binding.Symbolizer;
import static org.constellation.json.util.StyleFactories.FF;
import org.geotoolkit.cql.CQL;
import org.geotoolkit.filter.visitor.DuplicatingFilterVisitor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.NilExpression;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class StyleUtilities extends Static {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.json.util");

    /**
     * Parse given string as CQL and returns Expression.
     * @param exp
     * @return
     */
    public static Expression parseExpression(final String exp) {
        try{
            return CQL.parseExpression(exp);
        } catch (CQLException ex) {
            return Expression.NIL;
        }
    }

    /**
     * Convert expression to String CQL
     * if exp is NilExpression then returns empty string because NilExpression is not supported by CQL
     * @param exp {Expression exp}
     * @return {String}
     */
    public static String toCQL(final Expression exp) {
        if(exp instanceof NilExpression) {
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
        f = (Filter) f.accept(new NoNilFilter(),null);
        return CQL.write(f);
    }

    public static Expression opacity(final double opacity) {
        return (opacity >= 0 && opacity <= 1.0) ? FF.literal(opacity) : Expression.NIL;
    }

    public static Expression literal(final Object value) {
        return value != null ? FF.literal(value) : Expression.NIL;
    }

    public static <T> T type(final StyleElement<T> elt) {
        return elt != null ? elt.toType() : null;
    }

    public static <T> List<T> singletonType(final StyleElement<T> elt) {
        return elt != null ? Collections.singletonList(elt.toType()) : new ArrayList<T>(0);
    }

    public static <T> List<T> listType(final List<? extends StyleElement<T>> elts) {
        final List<T> list = new ArrayList<T>();
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

    private static class NoNilFilter extends DuplicatingFilterVisitor {
        @Override
        public Object visit(NilExpression expression, Object extraData) {
            return ff.literal(null);
        }
    }
}
