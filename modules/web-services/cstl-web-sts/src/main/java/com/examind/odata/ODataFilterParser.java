/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.odata;

import com.examind.odata.ODataParser.ExpressionContext;
import com.examind.odata.ODataParser.ExpressionFctParamContext;
import com.examind.odata.ODataParser.ExpressionNumContext;
import com.examind.odata.ODataParser.ExpressionTermContext;
import com.examind.odata.ODataParser.ExpressionUnaryContext;
import com.examind.odata.ODataParser.FilterContext;
import com.examind.odata.ODataParser.FilterTermContext;
import com.examind.odata.ODataParser.FilterGeometryContext;

import static com.examind.odata.ODataParser.*;
import static com.examind.sts.core.STSUtils.FORMATTERS;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import javax.measure.Unit;
import javax.measure.quantity.Length;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.sis.measure.Units;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.filter.FilterUtilities;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.OMEntity;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.ValueReference;
import org.opengis.geometry.Envelope;
import org.opengis.temporal.TemporalPrimitive;


/**
 *
 * @author Guilhem Legal (Geomatys)
 * @author  Johann Sorel (Geomatys)
 */
public final class ODataFilterParser {

    private static final String RESULT = "result";
    private static final String RESULT_TIME = "resulttime";
    private static final String PHENOMENON_TIME = "phenomenontime";
    private static final String PROPERTIES = "properties";
    private static final String ID = "id";

    private ODataFilterParser() {
    }

    public static Expression<Feature,?> parseExpression(OMEntity entityType, String odata) throws ODataParseException {
        return parseExpression(entityType, odata, null);
    }

    public static Expression<Feature,?> parseExpression(OMEntity entityType, String odata, FilterFactory factory) throws ODataParseException {
        final Object obj = AntlrOData.compileExpression(odata);
        Expression<Feature,?> result = null;
        if (obj instanceof ExpressionContext) {
            ParseTree tree = (ParseTree) obj;
            if (factory == null) {
                factory = FilterUtilities.FF;
            }
            result = convertExpression(entityType, tree, factory);
        }
        return result;
    }

    public static Filter<? super Feature> parseFilter(OMEntity entityType, String odata) throws ODataParseException {
        return parseFilter(entityType, odata, null);
    }

    public static Filter<? super Feature> parseFilter(OMEntity entityType, String odata, FilterFactory factory) throws ODataParseException {
        odata = odata.trim();

        // Bypass parsing for inclusive filter.
        if (odata.isEmpty() || "*".equals(odata)) {
            return Filter.include();
        }
        final Object obj = AntlrOData.compileFilter(odata);
        Filter<? super Feature> result = null;
        if (obj instanceof FilterContext) {
            ParseTree tree = (ParseTree) obj;
            if (factory == null) {
                factory = FilterUtilities.FF;
            }
            result = convertFilter(entityType, tree, factory);
        }
        return result;
    }

    /**
     * Convert the given tree in an Expression.
     */
    private static Expression<Feature,?> convertExpression(OMEntity entityType, ParseTree tree, FilterFactory ff) throws ODataParseException {
        if (tree instanceof ExpressionContext) {
            //: expression MULT expression
            //| expression UNARY expression
            //| expressionTerm
            if (tree.getChildCount() == 3) {
                final String operand = tree.getChild(1).getText();
                // TODO: unsafe cast.
                final Expression<? super Feature, ? extends Number> left  = (Expression<? super Feature, ? extends Number>) convertExpression(entityType, tree.getChild(0), ff);
                final Expression<? super Feature, ? extends Number> right = (Expression<? super Feature, ? extends Number>) convertExpression(entityType, tree.getChild(2), ff);
                if (" mul ".equals(operand)) {
                    return ff.multiply(left, right);
                } else if (" div ".equals(operand)) {
                    return ff.divide(left, right);
                } else if (" add ".equals(operand)) {
                    return ff.add(left, right);
                } else if (" sub ".equals(operand)) {
                    return ff.subtract(left, right);
                }
            } else {
                return convertExpression(entityType, tree.getChild(0), ff);
            }
        } //        else if(tree instanceof ExpressionStringContext){
        //            //strip start and end '
        //            final String text = tree.getText();
        //            return ff.literal(text.substring(1, text.length()-1));
        //        }
        else if (tree instanceof ExpressionTermContext) {
            //: expressionString
            //| expressionUnary
            //| PROPERTY_NAME
            //| DATE
            //| DURATION_P
            //| DURATION_T
            //| NAME (LPAREN expressionFctParam? RPAREN)?
            //| expressionGeometry
            //| LPAREN expression RPAREN

            //: TEXT
            //| expressionUnary
            //| PROPERTY_NAME
            //| DATE
            //| DURATION_P
            //| DURATION_T
            //| expressionGeometry
            final ExpressionTermContext exp = (ExpressionTermContext) tree;
            if (exp.getChildCount() == 1) {
                return convertExpression(entityType, tree.getChild(0), ff);
            }
            // LPAREN expression RPAREN
            if (exp.expression() != null) {
                return convertExpression(entityType, exp.expression(), ff);
            }
            // NAME (LPAREN expressionFctParam? RPAREN)?
            if (exp.NAME() != null) {
                final String name = exp.NAME().getText();
                final ExpressionFctParamContext prm = exp.expressionFctParam();
                if (prm == null) {
                    //handle as property name
                    String property = getSupportedProperties(entityType, name);
                    return ff.property(property);
                }
                // Handle as a function.
                final List<ExpressionContext> params = prm.expression();
                final List<Expression<Feature,?>> exps = new ArrayList<>();
                for (int i = 0, n = params.size(); i < n; i++) {
                    exps.add(convertExpression(entityType, params.get(i), ff));
                }
                return ff.function(name, exps.toArray(new Expression[exps.size()]));
            }
        } else if (tree instanceof ExpressionUnaryContext) {
            //: UNARY? expressionNum ;
            final ExpressionUnaryContext exp = (ExpressionUnaryContext) tree;
            return ff.literal(unaryAsNumber(exp));

        } else if (tree instanceof ExpressionNumContext) {
            //: INT | FLOAT ;
            return convertExpression(entityType, tree.getChild(0), ff);
        } else if (tree instanceof TerminalNode) {
            final TerminalNode exp = (TerminalNode) tree;
            switch (exp.getSymbol().getType()) {
                case PROPERTY_NAME: {
                    // strip start and end "
                    String text = tree.getText();
                    text = text.substring(1, text.length() - 1);
                    String property = getSupportedProperties(entityType, text);
                    return ff.property(property);
                }
                case NAME:  return ff.property(getSupportedProperties(entityType, tree.getText()));
                case INT:   return ff.literal(Integer.valueOf(tree.getText()));
                case FLOAT: return ff.literal(Double.valueOf(tree.getText()));
                case BOOL:  return ff.literal(Boolean.valueOf(tree.getText()));
                case DATE: {

                    TemporalPrimitive ta = parseTemporalObj(tree.getText());
                    return ff.literal(ta);
                }
                case TEXT: {
                    // strip start and end '
                    String text = tree.getText();
                    text = text.replaceAll("\\\\'", "'");
                    return ff.literal(text.substring(1, text.length() - 1));
                }
            }
            return convertExpression(entityType, tree.getChild(0), ff);
        }
        throw new ODataParseException("Unreconized expression : type=" + tree.getText());
    }

    private static Number unaryAsNumber(ExpressionUnaryContext exp) {
        //: UNARY? expressionNum ;
        final boolean negate = (exp.UNARY() != null && exp.UNARY().getSymbol().getText().equals("-"));
        final ExpressionNumContext num = exp.expressionNum();
        if (num.INT() != null) {
            int val = Integer.valueOf(num.INT().getText());
            return negate ? -val : val;
        } else {
            double val = Double.valueOf(num.FLOAT().getText());
            return negate ? -val : val;
        }
    }

    private static Unit<Length> parseLengthUnit(final Expression<Feature,?> unitExp) {
        Object value = unitExp.apply(null);
        if (value != null) {
            return Units.ensureLinear(Units.valueOf(value.toString()));
        }
        if (unitExp instanceof ValueReference<?,?>) {
            value = ((ValueReference<?,?>) unitExp).getXPath();
        }
        throw new IllegalArgumentException("Unit `" + value + "` is not a literal.");
    }

    /**
     * Convert the given tree in a Filter.
     */
    private static Filter<? super Feature> convertFilter(OMEntity entityType, ParseTree tree, FilterFactory ff) throws ODataParseException {
        if (tree instanceof FilterContext) {
            //: filter (AND filter)+
            //| filter (OR filter)+
            //| LPAREN filter RPAREN
            //| NOT filterTerm
            //| filterTerm

            final FilterContext exp = (FilterContext) tree;

            //| filterTerm
            if (exp.getChildCount() == 1) {
                return convertFilter(entityType, tree.getChild(0), ff);
            } else if (exp.NOT() != null) {
                //| NOT (filterTerm | ( LPAREN filter RPAREN ))
                if (exp.filterTerm() != null) {
                    return ff.not(convertFilter(entityType, exp.filterTerm(), ff));
                } else {
                    return ff.not(convertFilter(entityType, exp.filter(0), ff));
                }

            } else if (!exp.AND().isEmpty()) {
                //: filter (AND filter)+
                final List<Filter<? super Feature>> subs = new ArrayList<>();
                for (FilterContext f : exp.filter()) {
                    final Filter<? super Feature> sub = convertFilter(entityType, f, ff);
                    if (sub.getOperatorType() == LogicalOperatorName.AND) {
                        subs.addAll(((LogicalOperator<? super Feature>) sub).getOperands());
                    } else {
                        subs.add(sub);
                    }
                }
                return ff.and(subs);
            } else if (!exp.OR().isEmpty()) {
                //| filter (OR filter)+
                final List<Filter<? super Feature>> subs = new ArrayList<>();
                for (FilterContext f : exp.filter()) {
                    final Filter<? super Feature> sub = convertFilter(entityType, f, ff);
                    if (sub.getOperatorType() == LogicalOperatorName.OR) {
                        subs.addAll(((LogicalOperator<? super Feature>) sub).getOperands());
                    } else {
                        subs.add(sub);
                    }
                }
                return ff.or(subs);
            } else if (exp.LPAREN() != null) {
                //| LPAREN filter RPAREN
                return convertFilter(entityType, exp.filter(0), ff);
            }
        } else if (tree instanceof FilterGeometryContext) {
            final FilterGeometryContext exp = (FilterGeometryContext) tree;
            if (exp.CONTAINS() != null) {
                final Expression<Feature,?> exp1 = convertExpression(entityType, exp.expression(), ff);
                String geomStr = exp.TEXT().getText();
                if (geomStr.startsWith("'")) {
                    geomStr = geomStr.substring(1);
                }
                if (geomStr.endsWith("'")) {
                    geomStr = geomStr.substring(0, geomStr.length() -1);
                }
                WKTReader reader = new WKTReader();
                try {
                    Geometry geom = reader.read(geomStr);
                    geom.setUserData(CommonCRS.WGS84.normalizedGeographic());
                    Envelope e = JTS.toEnvelope(geom);
                    return ff.bbox(ff.property("location"), e);
                } catch (ParseException ex) {
                    throw new ODataParseException("malformed spatial filter geometry");
                }
            }

        } else if (tree instanceof FilterTermContext) {
            //: expression
            //    (
            //              COMPARE  expression
            //            | NOT? IN LPAREN (expressionFctParam )?  RPAREN
            //            | BETWEEN expression AND expression
            //            | NOT? LIKE expression
            //            | NOT? ILIKE expression
            //            | IS NOT? NULL
            //            | AFTER expression
            //            | ANYINTERACTS expression
            //            | BEFORE expression
            //            | BEGINS expression
            //            | BEGUNBY expression
            //            | DURING expression
            //            | ENDEDBY expression
            //            | ENDS expression
            //            | MEETS expression
            //            | METBY expression
            //            | OVERLAPPEDBY expression
            //            | TCONTAINS expression
            //            | TEQUALS expression
            //            | TOVERLAPS expression
            //    )
            //| filterGeometry

            final FilterTermContext exp = (FilterTermContext) tree;
            final List<ExpressionContext> exps = exp.expression();
            if (exp.COMPARE() != null) {
                // expression COMPARE expression
                final String text = exp.COMPARE().getText();
                final Expression<Feature,?> left  = convertExpression(entityType, exps.get(0), ff);
                final Expression<Feature,?> right = convertExpression(entityType, exps.get(1), ff);
                if (" eq ".equals(text)) {
                    if (isTemporalProperty(left)) {
                        return ff.tequals(left, right);
                    }
                    return ff.equal(left, right);
                } else if (" ne ".equals(text)) {
                    return ff.notEqual(left, right);
                } else if (" gt ".equals(text)) {
                    if (isTemporalProperty(left)) {
                        return ff.after(left, right);
                    }
                    return ff.greater(left, right);
                } else if (" lt ".equals(text)) {
                    if (isTemporalProperty(left)) {
                        return ff.before(left, right);
                    }
                    return ff.less(left, right);
                } else if (" ge ".equals(text)) {
                    if (isTemporalProperty(left)) {
                        return ff.after(left, right);
                    }
                    return ff.greaterOrEqual(left, right);
                } else if (" le ".equals(text)) {
                    if (isTemporalProperty(left)) {
                        return ff.before(left, right);
                    }
                    return ff.lessOrEqual(left, right);
                }
            } else if (exp.IN() != null) {
                // expression NOT? IN LPAREN (expressionFctParam )?  RPAREN
                final Expression<Feature,?> val = convertExpression(entityType, exps.get(0), ff);
                final ExpressionFctParamContext prm = exp.expressionFctParam();
                final List<ExpressionContext> params = prm.expression();
                final List<Expression<Feature,?>> subexps = new ArrayList<>();
                for (int i = 0, n = params.size(); i < n; i++) {
                    subexps.add(convertExpression(entityType, params.get(i), ff));
                }
                final int size = subexps.size();
                final Filter<Feature> selection;
                switch (size) {
                    case 0: {
                        selection = Filter.exclude();
                        break;
                    }
                    case 1: {
                        selection = ff.equal(val, subexps.get(0));
                        break;
                    }
                    default: {
                        final List<Filter<? super Feature>> filters = new ArrayList<>();
                        for (Expression<Feature,?> e : subexps) {
                            filters.add(ff.equal(val, e));
                        }   selection = ff.or(filters);
                        break;
                    }
                }
                if (exp.NOT() != null) {
                    return ff.not(selection);
                } else {
                    return selection;
                }
            }
        }
        throw new ODataParseException("Unreconized filter : type=" + tree.getText());
    }

    private static boolean isTemporalProperty(Expression<Feature,?> exp) throws ODataParseException {
        if (exp instanceof ValueReference) {
            final String p = ((ValueReference)exp).getXPath().toLowerCase();
            return p.equals(RESULT_TIME) || p.equals(PHENOMENON_TIME) || p.equals("time");
        }
        return false;
    }

    private static TemporalPrimitive parseTemporalObj(String to) throws ODataParseException {
        int index = to.indexOf('/');
        if (index != -1) {
            Date begin = parseDate(to.substring(0, index));
            Date end   = parseDate(to.substring(index + 1));
            return OMUtils.buildTime("t", begin, end);
        } else {
            Date d = parseDate(to);
            return OMUtils.buildTime("t", d, null);
        }
    }

    public static Date parseDate(String str) throws ODataParseException {
        for (SimpleDateFormat format : FORMATTERS) {
            try {
                synchronized (format) {
                    return format.parse(str);
                }
            } catch (java.text.ParseException ex) {}
        }
        throw new ODataParseException("Error while parsing date value:" + str);
    }

    private static String getSupportedProperties(OMEntity entityType, String xPath) throws ODataParseException {
        String[] properties = xPath.split("/");
        String property;
        if (properties.length >= 2) {
            String previous = properties[properties.length - 2];
            String last = properties[properties.length - 1];//.toLowerCase();
            if (last.equals("id")) {
                property = previous;
            } else if (last.equalsIgnoreCase(RESULT_TIME) || last.equalsIgnoreCase(PHENOMENON_TIME) || last.startsWith(RESULT)) {
                property = last;
            } else if (previous.equals(PROPERTIES)) {
                // max 3 level
                if (properties.length > 2) {
                    String entity = staToInternalName(properties[properties.length - 3]);
                    return entity + '/' + previous + '/' + last;
                } else {
                    return previous + '/' + last;
                }
            } else {
                throw new ODataParseException("malformed or unknow filter propertyName. was expecting something/id ");
            }
        } else if (properties.length >= 1) {
            property = properties[properties.length - 1];
        } else {
            throw new ODataParseException("malformed filter propertyName. was expecting something/id or something/result");
        }
        if ((property.startsWith(RESULT) || property.startsWith(PROPERTIES)) && !property.equalsIgnoreCase(RESULT_TIME)) {
            return property;
        } else if (property.equals(ID)) {
            property = entityType.getName();
        }
        return staToInternalName(property);
    }

    private static String staToInternalName(String property) throws ODataParseException {
         return switch(property.toLowerCase()) {
            case "thing"             -> "procedure";
            case "sensor"            -> "procedure";
            case "location"          -> "procedure";
            case "observedproperty"  -> "observedProperty";
            case "datastream"        -> "observationId";
            case "multidatastream"   -> "observationId";
            case "observation"       -> "observationId";
            case "featureofinterest" -> "featureOfInterest";
            case RESULT_TIME         -> "time";
            case PHENOMENON_TIME     -> "time";
            case "time"              -> "time";
            default -> throw new ODataParseException("Unexpected property name:" + property);
        };
    }

}
