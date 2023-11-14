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
package org.constellation.map.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.feature.internal.AttributeConvention;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.IdentifiedObjects;
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
import org.opengis.filter.BetweenComparisonOperator;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.opengis.filter.ResourceId;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.DistanceOperatorName;
import org.opengis.filter.LikeOperator;
import org.opengis.filter.LogicalOperator;
import org.opengis.filter.LogicalOperatorName;
import org.opengis.filter.NullOperator;
import org.opengis.filter.SpatialOperatorName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.CodeList;
import org.opengis.util.FactoryException;

/**
 *
 * @author Alexis Manin (Geomatys)
 */
public class OGCFilterToDTOTransformer {

    private static final FilterFactory FF = org.geotoolkit.filter.FilterUtilities.FF;

    public OGCFilterToDTOTransformer() {
    }

    public String extract(final Expression exp) {
        if (exp instanceof Literal) {
            Object val = ((Literal) exp).getValue();
            if (val instanceof Color) {
                val = FilterUtilities.toString((Color)val);
                if (val == null) return null;
            }
            return val.toString();
        } else if (exp instanceof ValueReference) {
            return ((ValueReference) exp).getXPath();
        } else {
            throw new UnsupportedOperationException("Function transformation is not supported yet");
        }
    }

    public org.constellation.dto.Filter visit(final Filter filter) {
        if (filter.equals(Filter.include())) {
            return null;
        }
        if (filter.equals(Filter.exclude())) {
            return null;
        }
        final CodeList<?> type = filter.getOperatorType();
        if (filter instanceof BetweenComparisonOperator) {
            final BetweenComparisonOperator pib = (BetweenComparisonOperator) filter;
            String lower = extract(pib.getLowerBoundary());
            String upper = extract(pib.getUpperBoundary());
            String field = extract(pib.getExpression());
            return new org.constellation.dto.Filter(field, lower + '/' + upper, OGCJAXBStatics.FILTER_COMPARISON_ISBETWEEN);

        } else if (type == ComparisonOperatorName.PROPERTY_IS_EQUAL_TO) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getOperand1());
            String value = extract(pit.getOperand2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISEQUAL);

        } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getOperand1());
            String value = extract(pit.getOperand2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISGREATER);

        } else if (type == ComparisonOperatorName.PROPERTY_IS_GREATER_THAN_OR_EQUAL_TO) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getOperand1());
            String value = extract(pit.getOperand2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISGREATEROREQUAL);

        } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getOperand1());
            String value = extract(pit.getOperand2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISLESS);

        } else if (type == ComparisonOperatorName.PROPERTY_IS_LESS_THAN_OR_EQUAL_TO) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getOperand1());
            String value = extract(pit.getOperand2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISLESSOREQUAL);

        } else if (filter instanceof LikeOperator) {
            final LikeOperator<Object> pis = (LikeOperator) filter;
            final String field = extract(pis.getExpressions().get(0));
            final String value = (String) ((Literal) pis.getExpressions().get(1)).getValue();
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISLIKE);

        } else if (type == ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO) {
            final BinaryComparisonOperator pit = (BinaryComparisonOperator) filter;
            String field = extract(pit.getOperand1());
            String value = extract(pit.getOperand2());
            return new org.constellation.dto.Filter(field, value, OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL);

        } else if (filter instanceof NullOperator) {
            final NullOperator<Object> pis = (NullOperator) filter;
            String field = extract(pis.getExpressions().get(0));
            return new org.constellation.dto.Filter(field, null, OGCJAXBStatics.FILTER_COMPARISON_ISNOTEQUAL);

        } else if (type == LogicalOperatorName.AND) {
            final LogicalOperator<Object> and = (LogicalOperator) filter;
            List<org.constellation.dto.Filter> filters = new ArrayList<>();
            for (final Filter f : and.getOperands()) {
                org.constellation.dto.Filter ele = visit(f);
                if (ele != null) {
                    filters.add(ele);
                }
            }
            return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_AND, filters);

        } else if (type == LogicalOperatorName.OR) {
            final LogicalOperator<Object> or = (LogicalOperator) filter;
            List<org.constellation.dto.Filter> filters = new ArrayList<>();
            for (final Filter f : or.getOperands()) {
                org.constellation.dto.Filter ele = visit(f);
                if (ele != null) {
                    filters.add(ele);
                }
            }
            return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_OR, filters);

        } else if (type == LogicalOperatorName.NOT) {
            final LogicalOperator<Object> not = (LogicalOperator) filter;
            final org.constellation.dto.Filter sf = visit(not.getOperands().get(0));
            return new org.constellation.dto.Filter(OGCJAXBStatics.FILTER_LOGIC_NOT, Arrays.asList(sf));

        } else if (type == SpatialOperatorName.BBOX) {
            final BinarySpatialOperator bbox = (BinarySpatialOperator) filter;

            final Expression left = bbox.getOperand1();
            final Expression right = bbox.getOperand2();

            final String field;
            final double minx;
            final double maxx;
            final double miny;
            final double maxy;
            String srs;

            if (left instanceof ValueReference) {
                field = ((ValueReference) left).getXPath();

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

            } else if (right instanceof ValueReference) {
                field = ((ValueReference) right).getXPath();

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

        } else if (filter instanceof ResourceId) {
            //todo OGC filter can not handle ID when we are inside another filter type
            //so here we make a small tric to change an id filter in a serie of propertyequal filter
            //this is not really legal but we dont have the choice here
            //we should propose an evolution of ogc filter do consider id filter as a comparison filter
            final ValueReference n = FF.property(AttributeConvention.IDENTIFIER_PROPERTY.toString());
            final List<String> lst = new ArrayList<>();

            lst.add(((ResourceId) filter).getIdentifier());

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

            Expression exp1 = spatialOp.getOperand1();
            Expression exp2 = spatialOp.getOperand2();

            if (!(exp1 instanceof ValueReference)) {
                //flip order
                final Expression ex = exp1;
                exp1 = exp2;
                exp2 = ex;
            }

            if (!(exp1 instanceof ValueReference)) {
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
                    Logger.getLogger("org.geotoolkit.sld.xml").log(Level.WARNING, null, ex);
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
                    Logger.getLogger("org.geotoolkit.sld.xml").log(Level.WARNING, null, ex);
                }
                jaxGeom = null;
                jaxEnv = new EnvelopeType(new DirectPositionType(genv.getLowerCorner()), new DirectPositionType(genv.getUpperCorner()), srs);
            } else {
                throw new IllegalArgumentException("Type is not geometric or envelope.");
            }

            if (type == DistanceOperatorName.BEYOND) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_BEYOND);

            } else if (type == SpatialOperatorName.CONTAINS) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_CONTAINS);

            } else if (type == SpatialOperatorName.CROSSES) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_CROSSES);

            } else if (type == DistanceOperatorName.WITHIN) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_DWITHIN);

            } else if (type == SpatialOperatorName.DISJOINT) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_DISJOINT);

            } else if (type == SpatialOperatorName.EQUALS) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_EQUALS);

            } else if (type == SpatialOperatorName.INTERSECTS) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_INTERSECTS);

            } else if (type == SpatialOperatorName.OVERLAPS) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_OVERLAPS);

            } else if (type == SpatialOperatorName.TOUCHES) {

                return new org.constellation.dto.Filter(pnt, writeGeoJSON(jaxEnv, jaxGeom), OGCJAXBStatics.FILTER_SPATIAL_TOUCHES);

            } else if (type == SpatialOperatorName.WITHIN) {

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
