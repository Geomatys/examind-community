/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008 - 2009, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.constellation.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import org.geotoolkit.util.NamesExt;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotoolkit.gml.GeometrytoJTS;
import org.geotoolkit.gml.xml.v311.AbstractGeometryType;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.ogc.xml.v110.PropertyNameType;
import org.geotoolkit.ogc.xml.v110.SortByType;
import org.geotoolkit.ogc.xml.v110.SortPropertyType;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.ObjectConverters;

import org.opengis.util.GenericName;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.MatchAction;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.ogc.xml.OGCJAXBStatics;

/**
 * Transform OGC jaxb xml in GT classes.
 *
 * @author Johann Sorel (Geomatys)
 */
public class DtoToOGCFilterTransformer {

    protected final FilterFactory2 filterFactory;

    private final Map<String, String> namespaceMapping;

    public DtoToOGCFilterTransformer(final FilterFactory2 factory) {
        this.filterFactory = factory;
        this.namespaceMapping = null;
    }

    public DtoToOGCFilterTransformer(final FilterFactory2 factory, final Map<String, String> namespaceMapping) {
        this.filterFactory = factory;
        this.namespaceMapping = namespaceMapping;
    }

    public Filter visitFilter(final org.constellation.dto.Filter ft) throws FactoryException {
        if (ft == null) {
            return null;
        }

        if (isComparisonOp(ft)) {
            return visitComparisonOp(ft);
        } else if (isLogicOp(ft)) {
            return visitLogicOp(ft);
        } else if (isSpatialOp(ft)) {
            return visitSpatialOp(ft);
        } else if ("featureId".equalsIgnoreCase(ft.getOperator())) {
            return visitIds(ft);
        } else {
            //this case should not happen but if so, we consider it's an ALL features filter
            return Filter.INCLUDE;
        }

    }

    /**
     * Transform a SLD spatial Filter v1.1 in GT filter.
     */
    public Filter visitSpatialOp(org.constellation.dto.Filter f) throws NoSuchAuthorityCodeException, FactoryException {

        final String OpName = f.getOperator();

        if (isBinarySpatialOp(f)) {

            final AbstractGeometryType geom = null; // TODO read from GEOJson
            final EnvelopeType env =  null; // TODO read from GEOJson

            final Expression left = visitPropertyName(f.getValue());
            final Expression right;
            if (env != null) {
                try {
                    right = visitEnv(env);
                } catch (FactoryException ex) {
                    throw new IllegalArgumentException("SRS name is unknowned : " + ex.getLocalizedMessage(), ex);
                }
            } else {
                right = visit(geom);
            }

            if (OGCJAXBStatics.FILTER_SPATIAL_CONTAINS.equalsIgnoreCase(OpName)) {
                return filterFactory.contains(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_CROSSES.equalsIgnoreCase(OpName)) {
                return filterFactory.crosses(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_DISJOINT.equalsIgnoreCase(OpName)) {
                return filterFactory.disjoint(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_EQUALS.equalsIgnoreCase(OpName)) {
                return filterFactory.equal(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_INTERSECTS.equalsIgnoreCase(OpName)) {
                return filterFactory.intersects(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_OVERLAPS.equalsIgnoreCase(OpName)) {
                return filterFactory.overlaps(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_TOUCHES.equalsIgnoreCase(OpName)) {
                return filterFactory.touches(left, right);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_WITHIN.equalsIgnoreCase(OpName)) {
                return filterFactory.within(left, right);
            }

            throw new IllegalArgumentException("Illegal filter element" + OpName + ".");

        } else if (isSpatialDistanceOp(f)) {
            final AbstractGeometryType geom = null; // TODO read from geoJSON

            final Expression geom1 = visitPropertyName(f.getField());
            final Expression geom2 = visit(geom);

            final double distance = f.getDistance();
            final String units = f.getUnits();

            if (OGCJAXBStatics.FILTER_SPATIAL_DWITHIN.equalsIgnoreCase(OpName)) {
                return filterFactory.dwithin(geom1, geom2, distance, units);
            } else if (OGCJAXBStatics.FILTER_SPATIAL_BEYOND.equalsIgnoreCase(OpName)) {
                return filterFactory.beyond(geom1, geom2, distance, units);
            }

            throw new IllegalArgumentException("Illegal filter element" + OpName + ".");

        } else if (OGCJAXBStatics.FILTER_SPATIAL_BBOX.equalsIgnoreCase(OpName)) {
            String[] values = f.getValue().split(",");

            final double minx = Double.parseDouble(values[0]);
            final double maxx = Double.parseDouble(values[1]);
            final double miny = Double.parseDouble(values[2]);
            final double maxy = Double.parseDouble(values[3]);

            final String srs = values[4];

            return filterFactory.bbox(f.getField(), minx, miny, maxx, maxy, srs);
        }

        throw new IllegalArgumentException("Unknowed filter element" + OpName);
    }

    /**
     * Transform a SLD logic Filter v1.1 in GT filter.
     */
    public Filter visitLogicOp(org.constellation.dto.Filter f)
            throws NoSuchAuthorityCodeException, FactoryException {

        final String OpName = f.getOperator();

        if (OGCJAXBStatics.FILTER_LOGIC_NOT.equalsIgnoreCase(OpName)) {
            Filter filter = null;
            if (f.getFilters().size() == 1) {
                filter = visitFilter(f.getFilters().get(0));
            }

            if (filter == null) {
                throw new IllegalArgumentException("Invalide filter element" + f);
            }

            return filterFactory.not(filter);

        } else if (isBinaryLogicOp(f)) {

            if (OGCJAXBStatics.FILTER_LOGIC_AND.equalsIgnoreCase(OpName)) {
                final List<Filter> filters = new ArrayList<>();

                for (org.constellation.dto.Filter ele : f.getFilters()) {
                    filters.add(visitFilter(ele));
                }

                if (filters.isEmpty()) {
                    return Filter.INCLUDE;
                } else if (filters.size() == 1) {
                    return filters.get(0);
                } else {
                    return filterFactory.and(filters);
                }

            } else if (OGCJAXBStatics.FILTER_LOGIC_OR.equalsIgnoreCase(OpName)) {
                final List<Filter> filters = new ArrayList<>();

                for (org.constellation.dto.Filter ele : f.getFilters()) {
                    filters.add(visitFilter(ele));
                }

                if (filters.isEmpty()) {
                    return Filter.INCLUDE;
                } else if (filters.size() == 1) {
                    return filters.get(0);
                } else {
                    return filterFactory.or(filters);
                }
            }

        }

        throw new IllegalArgumentException("Unknowed filter element" + OpName);
    }

    /**
     * Transform a SLD comparison Filter v1.1 in GT filter.
     */
    public Filter visitComparisonOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();

        if (isBinaryComparisonOp(f)) {

            final Expression left = visitPropertyName(f.getField());
            final Expression right = visitLiteral(f.getValue());
            Boolean match = f.getMatchCase();
            if (match == null) {
                match = Boolean.TRUE;
            }
            final MatchAction action = f.getMatchAction();

            if (OGCJAXBStatics.FILTER_COMPARISON_ISEQUAL.equalsIgnoreCase(OpName)) {
                return filterFactory.equal(left, right, match, action);
            } else if (OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL.equalsIgnoreCase(OpName)) {
                return filterFactory.notEqual(left, right, match, action);
            } else if (OGCJAXBStatics.FILTER_COMPARISON_ISLESS.equalsIgnoreCase(OpName)) {
                return filterFactory.less(left, right, match, action);
            } else if (OGCJAXBStatics.FILTER_COMPARISON_ISGREATER.equalsIgnoreCase(OpName)) {
                return filterFactory.greater(left, right, match, action);
            } else if (OGCJAXBStatics.FILTER_COMPARISON_ISLESSOREQUAL.equalsIgnoreCase(OpName)) {
                return filterFactory.lessOrEqual(left, right, match, action);
            } else if (OGCJAXBStatics.FILTER_COMPARISON_ISGREATEROREQUAL.equalsIgnoreCase(OpName)) {
                return filterFactory.greaterOrEqual(left, right, match, action);
            }

            throw new IllegalArgumentException("Illegal filter operator:" + OpName);

        } else if (OGCJAXBStatics.FILTER_COMPARISON_ISLIKE.equalsIgnoreCase(OpName)) {

            final Expression expr = visitPropertyName(f.getField());
            final String pattern = visitLiteral(f.getValue()).toString();
            final String wild = "*";
            final String single = "?";
            final String escape = "/";

            return filterFactory.like(expr, pattern, wild, single, escape);

        } else if (OGCJAXBStatics.FILTER_COMPARISON_ISBETWEEN.equalsIgnoreCase(OpName)) {

            String value = f.getValue();
            int separator = value.indexOf('/');
            if (separator != -1) {
                final Expression lower = visitLiteral(value.substring(0, separator - 1));
                final Expression upper = visitLiteral(value.substring(separator + 1, value.length()));
                final Expression expr = visitPropertyName(f.getField());

                return filterFactory.between(expr, lower, upper);
            } else {
                throw new IllegalArgumentException("Malformed between value:" + f.getValue() + "; Expected lower/upper");
            }

        } else if (OGCJAXBStatics.FILTER_COMPARISON_ISNULL.equalsIgnoreCase(OpName)) {

            final Expression expr = visitPropertyName(f.getField());
            return filterFactory.isNull(expr);
        }

        throw new IllegalArgumentException("Unknowed filter operator:" + OpName);
    }

    /**
     * Transform a SLD IDS Filter v1.1 in GT filter.
     */
    public Filter visitIds(org.constellation.dto.Filter f) {
        final Set<Identifier> ids = new HashSet<>();
        ids.add(filterFactory.featureId(f.getValue()));
        return filterFactory.id(ids);
    }

    public List<SortBy> visitSortBy(final SortByType type) {
        final List<SortBy> sorts = new ArrayList<>();

        for (final SortPropertyType spt : type.getSortProperty()) {
            final PropertyName pn = visitPropertyName(spt.getPropertyName());
            sorts.add(filterFactory.sort(pn.getPropertyName(), spt.getSortOrder()));
        }

        return sorts;
    }

    public Expression visit(AbstractGeometryType ele) throws NoSuchAuthorityCodeException, FactoryException {
        return filterFactory.literal(GeometrytoJTS.toJTS(ele));
    }

    public Expression visitEnv(final EnvelopeType entry) throws FactoryException {
        String srs = entry.getSrsName();
        DirectPositionType lower = entry.getLowerCorner();
        DirectPositionType upper = entry.getUpperCorner();

        GeneralEnvelope genv = new GeneralEnvelope(CRS.forCode(srs));
        genv.setRange(0, lower.getOrdinate(0), upper.getOrdinate(0));
        genv.setRange(1, lower.getOrdinate(1), upper.getOrdinate(1));

        return filterFactory.literal(genv);
    }

    public PropertyName visitPropertyName(final PropertyNameType pnt) {
        if (pnt != null) {
            return visitPropertyName(pnt.getContent());
        }
        return null;
    }

    public PropertyName visitPropertyName(final String pnt) {
        String brutPname = pnt;
        if (brutPname.indexOf(':') == -1) {
            return filterFactory.property(brutPname);
        }

        String[] pnames = brutPname.split("/");
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String pname : pnames) {
            if (pnames.length > 1 && i != 0) {
                sb.append("/");
            }
            int pos = pname.indexOf(':');
            if (pos != -1 && namespaceMapping != null) {
                String prefix = pname.substring(0, pos);
                String namespace = namespaceMapping.get(prefix);
                if (namespace == null) {
                    throw new IllegalArgumentException("Prefix " + prefix + " is nor bounded.");
                } else {
                    sb.append('{').append(namespace).append('}').append(pname.substring(pos + 1));
                }
            } else {
                sb.append(pname);
            }
            i++;
        }
        return filterFactory.property(sb.toString());
    }

    /**
     * Transform a literalType in Expression.
     */
    public Expression visitLiteral(final String str) {

        Object obj = str;
        if (!str.isEmpty()) {
            if (str.charAt(0) == '#') {
                //try to convert it to a color
                try {
                    Color c = ObjectConverters.convert(str, Color.class);
                    if (c != null) {
                        obj = c;
                    }
                } catch (UnconvertibleObjectException e) {
                    // TODO - do we really want to ignore?
                    try {
                        obj = Color.decode(str);
                    } catch (Exception bis) {
                        bis.addSuppressed(e);
                        Logging.recoverableException(null, DtoToOGCFilterTransformer.class, "visitLiteral", e);
                    }
                }
            } else {
                //try to convert it to a number
                try {
                    obj = Double.valueOf(str);
                } catch (NumberFormatException ex) {
                }
            }
            return filterFactory.literal(obj);
        }
        return filterFactory.literal("");
    }

    /**
     * Change a QName in Name.
     */
    public GenericName visitQName(final QName qname) {
        if (qname == null) {
            return null;
        }
        return NamesExt.create(qname);
    }

    private boolean isBinaryComparisonOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return OGCJAXBStatics.FILTER_COMPARISON_ISEQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISLESS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISGREATER.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISLESSOREQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISGREATEROREQUAL.equalsIgnoreCase(OpName);
    }

    private boolean isComparisonOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return OGCJAXBStatics.FILTER_COMPARISON_ISEQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISLESS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISGREATER.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISLESSOREQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISGREATEROREQUAL.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISLIKE.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISBETWEEN.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_COMPARISON_ISNULL.equalsIgnoreCase(OpName);
    }

    private boolean isLogicOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return OGCJAXBStatics.FILTER_LOGIC_AND.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_LOGIC_NOT.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_LOGIC_OR.equalsIgnoreCase(OpName);
    }

    private boolean isBinaryLogicOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return OGCJAXBStatics.FILTER_LOGIC_AND.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_LOGIC_OR.equalsIgnoreCase(OpName);
    }

    private boolean isSpatialOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return OGCJAXBStatics.FILTER_SPATIAL_BBOX.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_BEYOND.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_CONTAINS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_CROSSES.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_DISJOINT.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_DWITHIN.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_EQUALS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_INTERSECTS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_OVERLAPS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_TOUCHES.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_WITHIN.equalsIgnoreCase(OpName);
    }

    private boolean isBinarySpatialOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return  OGCJAXBStatics.FILTER_SPATIAL_CONTAINS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_CROSSES.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_DISJOINT.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_EQUALS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_INTERSECTS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_OVERLAPS.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_TOUCHES.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_WITHIN.equalsIgnoreCase(OpName);
    }

    private boolean isSpatialDistanceOp(org.constellation.dto.Filter f) {
        final String OpName = f.getOperator();
        return OGCJAXBStatics.FILTER_SPATIAL_BEYOND.equalsIgnoreCase(OpName)
                || OGCJAXBStatics.FILTER_SPATIAL_DWITHIN.equalsIgnoreCase(OpName);
    }

}
