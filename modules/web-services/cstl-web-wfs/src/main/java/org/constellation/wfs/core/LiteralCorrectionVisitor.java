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

import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.feature.privy.FeatureExpression;
import org.geotoolkit.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.feature.AttributeType;
import org.opengis.feature.FeatureType;
import org.opengis.filter.BinaryComparisonOperator;
import org.opengis.filter.ComparisonOperatorName;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;
import org.opengis.filter.ValueReference;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class LiteralCorrectionVisitor extends DuplicatingFilterVisitor {
    public LiteralCorrectionVisitor(final FeatureType ft) {
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_EQUAL_TO, (f) -> {
            final BinaryComparisonOperator filter = (BinaryComparisonOperator) f;
            final Expression exp1 = filter.getOperand1();
            final Expression exp2 = filter.getOperand2();
            if (exp1 instanceof FeatureExpression property) {
                if (exp2 instanceof Literal literal) {

                    // Add a support for a filter on boolean property using integer 0 or 1
                    if (ft != null) {
                        final Object obj = property.expectedType(ft, new FeatureTypeBuilder()).build();
                        if (obj instanceof AttributeType) {
                            final AttributeType descriptor = (AttributeType) obj;
                            if (descriptor.getValueClass().equals(Boolean.class) && literal.getValue() instanceof Number) {
                                final Literal booleanLit;
                                if (literal.getValue().equals(1.0)) {
                                    booleanLit = ff.literal(true);
                                } else {
                                    booleanLit = ff.literal(false);
                                }
                                return ff.equal(exp1, booleanLit);
                            } else if (descriptor.getValueClass().equals(String.class) && literal.getValue() instanceof Number) {
                                final Literal stringLit = ff.literal(String.valueOf(literal.getValue()));
                                return ff.equal(exp1, stringLit);
                            }
                        }
                    }
                }
            }
            return filter;
        });
        setFilterHandler(ComparisonOperatorName.PROPERTY_IS_NOT_EQUAL_TO, (f) -> {
            final BinaryComparisonOperator filter = (BinaryComparisonOperator) f;
            final Expression exp1 = filter.getOperand1();
            final Expression exp2 = filter.getOperand2();
            if (exp1 instanceof FeatureExpression property) {
                if (exp2 instanceof Literal literal) {

                    // Add a support for a filter on boolean property using integer 0 or 1
                    if (ft != null) {
                        final AttributeType descriptor = (AttributeType) property.expectedType(ft, new FeatureTypeBuilder()).build();
                        if (descriptor != null) {
                            if (descriptor.getValueClass().equals(Boolean.class) && literal.getValue() instanceof Number) {
                                final Literal booleanLit;
                                if (literal.getValue().equals(1.0)) {
                                    booleanLit = ff.literal(true);
                                } else {
                                    booleanLit = ff.literal(false);
                                }
                                return ff.notEqual(exp1, booleanLit);
                            } else if (descriptor.getValueClass().equals(String.class) && literal.getValue() instanceof Number) {
                                final Literal stringLit = ff.literal(String.valueOf(literal.getValue()));
                                return ff.notEqual(exp1, stringLit);
                            }
                        }
                    }
                }
            }
            return filter;
        });
    }
}
