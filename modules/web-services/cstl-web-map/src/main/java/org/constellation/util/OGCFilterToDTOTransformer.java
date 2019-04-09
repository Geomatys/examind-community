/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2017, Geomatys
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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.geometry.isoonjts.JTSUtils;
import org.geotoolkit.geometry.jts.SRIDGenerator;
import org.geotoolkit.gml.GMLUtilities;
import org.geotoolkit.gml.xml.v311.AbstractGeometryType;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.ogc.xml.FilterUtilities;
import org.geotoolkit.ogc.xml.OGCJAXBStatics;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.And;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Id;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.BinarySpatialOperator;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.Crosses;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Disjoint;
import org.opengis.filter.spatial.Equals;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Overlaps;
import org.opengis.filter.spatial.Touches;
import org.opengis.filter.spatial.Within;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class OGCFilterToDTOTransformer {

    private static final FilterFactory FF = DefaultFactories.forBuildin(FilterFactory.class);

    public OGCFilterToDTOTransformer() {
    }

    public String extract(final Expression exp) {

        if (exp instanceof Function) {
            throw new UnsupportedOperationException("Function transformation is not supported yet");
        } else if (exp instanceof Multiply) {
            throw new UnsupportedOperationException("Function transformation is not supported yet");
        } else if (exp instanceof Literal) {

            Object val = ((Literal) exp).getValue();
            if (val instanceof Color) {
                val = FilterUtilities.toString((Color)val);
            }
            return val.toString();
        } else if (exp instanceof Add) {
            throw new UnsupportedOperationException("Function transformation is not supported yet");
        } else if (exp instanceof Divide) {
            throw new UnsupportedOperationException("Function transformation is not supported yet");
        } else if (exp instanceof Subtract) {
            throw new UnsupportedOperationException("Function transformation is not supported yet");
        } else if (exp instanceof PropertyName) {

            return ((PropertyName) exp).getPropertyName();
        } else if (exp instanceof NilExpression) {
            //DO nothing on NILL expression
        } else {
            throw new IllegalArgumentException("Unknowed expression element :" + exp);
        }
        return null;
    }

    public org.constellation.dto.Filter visit(final Filter filter) {
        if (filter.equals(Filter.INCLUDE)) {
            return null;
        }
        if (filter.equals(Filter.EXCLUDE)) {
            return null;
        }

        if (filter instanceof PropertyIsBetween) {
            final PropertyIsBetween pib = (PropertyIsBetween) filter;
            String lower = extract(pib.getLowerBoundary());
            String upper = extract(pib.getUpperBoundary());
            String field = extract(pib.getExpression());
            return new org.constellation.dto.Filter(field, lower + '/' + upper, OGCJAXBStatics.FILTER_COMPARISON_ISBETWEEN);

        } else if (filter instanceof PropertyIsEqualTo) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getExpression1());
            String value = extract(pit.getExpression2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISEQUAL);

        } else if (filter instanceof PropertyIsGreaterThan) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getExpression1());
            String value = extract(pit.getExpression2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISGREATER);

        } else if (filter instanceof PropertyIsGreaterThanOrEqualTo) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getExpression1());
            String value = extract(pit.getExpression2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISGREATEROREQUAL);

        } else if (filter instanceof PropertyIsLessThan) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getExpression1());
            String value = extract(pit.getExpression2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISLESS);

        } else if (filter instanceof PropertyIsLessThanOrEqualTo) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getExpression1());
            String value = extract(pit.getExpression2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISLESSOREQUAL);

        } else if (filter instanceof PropertyIsLike) {
            final PropertyIsLike pis = (PropertyIsLike) filter;
            final String field = extract(pis.getExpression());
            final String value = pis.getLiteral();
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISLIKE);

        } else if (filter instanceof PropertyIsNotEqualTo) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getExpression1());
            String value = extract(pit.getExpression2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL);

        } else if (filter instanceof PropertyIsNull) {
            final PropertyIsNull pis = (PropertyIsNull) filter;
            String field = extract(pis.getExpression());
            return new org.constellation.dto.Filter(field, null, OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL);

        } else if (filter instanceof And) {
            final And and = (And) filter;
            List<org.constellation.dto.Filter> filters = new ArrayList<>();
            for (final Filter f : and.getChildren()) {
                org.constellation.dto.Filter ele = visit(f);
                if (ele != null) {
                    filters.add(ele);
                }
            }
            return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_AND, filters);

        } else if (filter instanceof Or) {
            final Or or = (Or) filter;
            List<org.constellation.dto.Filter> filters = new ArrayList<>();
            for (final Filter f : or.getChildren()) {
                org.constellation.dto.Filter ele = visit(f);
                if (ele != null) {
                    filters.add(ele);
                }
            }
            return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_OR, filters);

        } else if (filter instanceof Not) {
            final Not not = (Not) filter;
            final org.constellation.dto.Filter sf = visit(not.getFilter());
            return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_NOT, Arrays.asList(sf));

        } else if (filter instanceof FeatureId) {
            throw new IllegalArgumentException("Not parsed yet : " + filter);

        } else if (filter instanceof BBOX) {
            final BBOX bbox = (BBOX) filter;

            final Expression left = bbox.getExpression1();
            final Expression right = bbox.getExpression2();

            final String field;
            final double minx;
            final double maxx;
            final double miny;
            final double maxy;
            String srs;

            if (left instanceof PropertyName) {
                field = ((PropertyName) left).getPropertyName();

                final Object objGeom = ((Literal) right).getValue();
                if (objGeom instanceof org.opengis.geometry.Envelope) {
                    final org.opengis.geometry.Envelope env = (org.opengis.geometry.Envelope) objGeom;
                    minx = env.getMinimum(0);
                    maxx = env.getMaximum(0);
                    miny = env.getMinimum(1);
                    maxy = env.getMaximum(1);
                    try {
                        srs = IdentifiedObjects.lookupURN(env.getCoordinateReferenceSystem(), null);
                        if (srs == null) {
                            srs = ReferencingUtilities.lookupIdentifier(env.getCoordinateReferenceSystem(), true);
                        }
                    } catch (FactoryException ex) {
                        throw new IllegalArgumentException("invalid bbox element : " + filter + " " + ex.getMessage(), ex);
                    }
                } else if (objGeom instanceof Geometry) {
                    final Geometry geom = (Geometry) objGeom;
                    final Envelope env = geom.getEnvelopeInternal();
                    minx = env.getMinX();
                    maxx = env.getMaxX();
                    miny = env.getMinY();
                    maxy = env.getMaxY();
                    srs = SRIDGenerator.toSRS(geom.getSRID(), SRIDGenerator.Version.V1);
                } else {
                    throw new IllegalArgumentException("invalid bbox element : " + filter);
                }

            } else if (right instanceof PropertyName) {
                field = ((PropertyName) right).getPropertyName();

                final Object objGeom = ((Literal) left).getValue();
                if (objGeom instanceof org.opengis.geometry.Envelope) {
                    final org.opengis.geometry.Envelope env = (org.opengis.geometry.Envelope) objGeom;
                    minx = env.getMinimum(0);
                    maxx = env.getMaximum(0);
                    miny = env.getMinimum(1);
                    maxy = env.getMaximum(1);
                    try {
                        srs = IdentifiedObjects.lookupURN(env.getCoordinateReferenceSystem(), null);
                    } catch (FactoryException ex) {
                        throw new IllegalArgumentException("invalid bbox element : " + filter + " " + ex.getMessage(), ex);
                    }
                } else if (objGeom instanceof Geometry) {
                    final Geometry geom = (Geometry) objGeom;
                    final Envelope env = geom.getEnvelopeInternal();
                    minx = env.getMinX();
                    maxx = env.getMaxX();
                    miny = env.getMinY();
                    maxy = env.getMaxY();
                    srs = SRIDGenerator.toSRS(geom.getSRID(), SRIDGenerator.Version.V1);
                } else {
                    throw new IllegalArgumentException("invalid bbox element : " + filter);
                }
            } else {
                throw new IllegalArgumentException("invalid bbox element : " + filter);
            }

            StringBuilder sb = new StringBuilder()
                                .append(Double.toString(minx)).append(',')
                                .append(Double.toString(maxx)).append(',')
                                .append(Double.toString(miny)).append(',')
                                .append(Double.toString(maxy)).append(',')
                                .append(srs);

            return new org.constellation.dto.Filter(field, sb.toString(), OGCJAXBStatics.FILTER_SPATIAL_BBOX);

        } else if (filter instanceof Id) {
            //todo OGC filter can not handle ID when we are inside another filter type
            //so here we make a small tric to change an id filter in a serie of propertyequal filter
            //this is not really legal but we dont have the choice here
            //we should propose an evolution of ogc filter do consider id filter as a comparison filter
            final PropertyName n = FF.property(AttributeConvention.IDENTIFIER_PROPERTY.toString());
            final List<String> lst = new ArrayList<>();

            for (Identifier ident : ((Id) filter).getIdentifiers()) {
                lst.add(ident.getID().toString());
            }

            if (lst.isEmpty()) {
                return null;
            } else if (lst.size() == 1) {
                return new org.constellation.dto.Filter(null, lst.get(0), "featureId");

            } else {
                List<org.constellation.dto.Filter> filters = new ArrayList<>();
                for (String l : lst) {
                    filters.add(new org.constellation.dto.Filter(null, l, "featureId"));
                }
                return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_AND, filters);
            }

        } else if (filter instanceof BinarySpatialOperator) {
            final BinarySpatialOperator spatialOp = (BinarySpatialOperator) filter;

            Expression exp1 = spatialOp.getExpression1();
            Expression exp2 = spatialOp.getExpression2();

            if (!(exp1 instanceof PropertyName)) {
                //flip order
                final Expression ex = exp1;
                exp1 = exp2;
                exp2 = ex;
            }

            if (!(exp1 instanceof PropertyName)) {
                throw new IllegalArgumentException("Filter can not be transformed in wml filter, "
                        + "expression are not of the requiered type ");
            }

            final String pnt = extract(exp1);
            final EnvelopeType jaxEnv;
            final AbstractGeometryType jaxGeom;

            final Object geom = ((Literal) exp2).getValue();

            if (geom instanceof Geometry) {
                final Geometry jts = (Geometry) geom;
                final String srid = SRIDGenerator.toSRS(jts.getSRID(), SRIDGenerator.Version.V1);
                CoordinateReferenceSystem crs;
                try {
                    crs = CRS.forCode(srid);
                } catch (Exception ex) {
                    Logging.getLogger("org.geotoolkit.sld.xml").log(Level.WARNING, null, ex);
                    crs = null;
                }

                jaxGeom = GMLUtilities.getGMLFromISO(JTSUtils.toISO(jts, crs));
                jaxEnv = null;

            } else if (geom instanceof org.opengis.geometry.Geometry) {
                jaxGeom = GMLUtilities.getGMLFromISO((org.opengis.geometry.Geometry) geom);
                jaxEnv = null;

            } else if (geom instanceof org.opengis.geometry.Envelope) {
                final org.opengis.geometry.Envelope genv = (org.opengis.geometry.Envelope) geom;
                String srs = null;
                try {
                    srs = IdentifiedObjects.lookupURN(genv.getCoordinateReferenceSystem(), null);
                } catch (FactoryException ex) {
                    Logging.getLogger("org.geotoolkit.sld.xml").log(Level.WARNING, null, ex);
                }
                jaxGeom = null;
                jaxEnv = new EnvelopeType(new DirectPositionType(genv.getLowerCorner()), new DirectPositionType(genv.getUpperCorner()), srs);
            } else {
                throw new IllegalArgumentException("Type is not geometric or envelope.");
            }

            if (filter instanceof Beyond) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_BEYOND);

            } else if (filter instanceof Contains) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_CONTAINS);

            } else if (filter instanceof Crosses) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_CROSSES);

            } else if (filter instanceof DWithin) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_DWITHIN);

            } else if (filter instanceof Disjoint) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_DISJOINT);

            } else if (filter instanceof Equals) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_EQUALS);

            } else if (filter instanceof Intersects) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_INTERSECTS);

            } else if (filter instanceof Overlaps) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_OVERLAPS);

            } else if (filter instanceof Touches) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_TOUCHES);

            } else if (filter instanceof Within) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_WITHIN);
            } else {
                throw new IllegalArgumentException("Unknowed filter element : " + filter + " class :" + filter.getClass());
            }
        } else {
            throw new IllegalArgumentException("Unknowed filter element : " + filter + " class :" + filter.getClass());
        }
    }

    // TODO
    private String writeGeoJSON(final EnvelopeType jaxEnv, final AbstractGeometryType jaxGeom) {
        if (jaxEnv != null) {
            return null; // TODO
        } else if (jaxGeom != null) {
            return null; // TODO
        }
        return null;
    }

}
