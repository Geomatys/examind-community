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


package org.constellation.wfs.core;

import java.util.function.Function;
import java.util.logging.Level;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.feature.FeatureExt;
import org.geotoolkit.filter.binaryspatial.LooseBBox;
import org.geotoolkit.filter.binaryspatial.UnreprojectedLooseBBox;
import org.geotoolkit.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.FeatureType;
import org.opengis.feature.PropertyNotFoundException;
import org.opengis.feature.PropertyType;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;
import org.geotoolkit.ogc.xml.BBOX;
import org.geotoolkit.geometry.BoundingBox;
import org.opengis.filter.BinarySpatialOperator;
import org.opengis.filter.SpatialOperatorName;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DefaultGeomPropertyVisitor extends DuplicatingFilterVisitor{
    public DefaultGeomPropertyVisitor(final FeatureType ft) {
        Function previous = getFilterHandler(SpatialOperatorName.BBOX);
        setFilterHandler(SpatialOperatorName.BBOX, (f) -> {
            final BBOX filter = BBOX.wrap((BinarySpatialOperator) f);
            final PropertyType geomAtt;
            try {
                geomAtt = FeatureExt.getDefaultGeometry(ft);
            } catch (PropertyNotFoundException e) {
                Logging.getLogger("com.examind.ws.wfs").log(Level.FINE, "No geometry found", e);
                return previous.apply(filter);
            }
            Expression exp1 = (Expression) visit(filter.getOperand1());
            if (exp1 instanceof ValueReference) {
                ValueReference pname = (ValueReference) exp1;
                if (pname.getXPath().trim().isEmpty()) {
                    exp1 = ff.property(geomAtt.getName().toString());
                }
            }
            final Expression exp2 = filter.getOperand2();
            if (!(exp2 instanceof Literal)) {
                //this value is supposed to hold a BoundingBox
                throw new IllegalArgumentException("Illegal BBOX filter, "
                        + "second expression should have been a literal with a boundingBox value: \n" + filter);
            } else {
                final Literal l = (Literal) visit(exp2);
                final Object obj = l.getValue();
                if (obj instanceof BoundingBox) {
                    if (filter instanceof UnreprojectedLooseBBox) {
                        return new UnreprojectedLooseBBox((ValueReference) exp1, ff.literal((BoundingBox) obj));
                    } else if (filter instanceof LooseBBox) {
                        return new LooseBBox((ValueReference)exp1, ff.literal((BoundingBox) obj));
                    } else {
                        return ff.bbox(exp1, (BoundingBox) obj);
                    }
                } else {
                    throw new IllegalArgumentException("Illegal BBOX filter, "
                        + "second expression should have been a literal with a boundingBox value but value was a : \n" + obj.getClass());
                }
            }
        });
    }
}
