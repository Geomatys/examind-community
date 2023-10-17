/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.util;

import org.opengis.feature.Feature;
import org.opengis.filter.Expression;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;

import static java.util.Objects.requireNonNull;
import static org.apache.sis.util.ArgumentChecks.ensureDimensionMatches;

/**
 * Define how to extract a given dimension from a specific type of object (the <em>receiver</em>).
 * It is useful to add additional dimensions to resources such as {@link Feature features} that defines a 2D CRS,
 * but provides a data column with its time of validity, its elevation, etc.
 * This can even be used for representation of an engineering dimension,
 * such as an enumeration type, or a scientific measure (frequency, etc.).
 *
 * There are two constructors:
 *
 * <ul>
 *     <li>{@link #DimensionDef(SingleCRS, Expression, Expression)} when the receiver is available on a range.</li>
 *     <li>{@link #DimensionDef(SingleCRS, Expression)} when the receiver is available on a single point.</li>
 * </ul>
 *
 * TODO: in the future, we might make this more type-safe and generalized using sealed hierarchy to represent ranges, points, and point lists.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Alexis Manin (Geomatys)
 * @param crs Mandatory. Dimension coordinate system. Must be a single-dimension CRS.
 * @param lower Mandatory. Function that extracts minimum value from receiver validity range.
 * @param upper Mandatory. Function that extracts maximum value from receiver validity range.
 * @param <CRS> Type of coordinate system, typically a {@link TemporalCRS} or a {@link VerticalCRS}.
 * @param <R> Expression <em>Receiver</em>. The type of object dimension expression can be evaluated upon.
 *            Typically, a {@link Feature}.
 * @param <V> Expression <em>Value type</em>. Type of value returned by the expression.
 *            Generally, these expressions should return a floating-point number.
 *            However, other types are allowed, to fit the case where a datastore evaluate dimension values in a special way.
 */
public record DimensionDef<CRS extends SingleCRS, R, V>(CRS crs, Expression<R, V> lower, Expression<R, V> upper) {

    /**
     * If receiver is valid only in a single point of the dimension, use this constructor instead of {@link #DimensionDef(SingleCRS, Expression, Expression)}.
     */
    public DimensionDef(CRS crs, Expression<R, V> valueEvaluator) { this(crs, valueEvaluator, valueEvaluator); }

    public DimensionDef {
        ensureDimensionMatches("Dimension CRS", 1, requireNonNull(crs, "Dimension CRS"));
        requireNonNull(lower, "Dimension lower edge expression");
        requireNonNull(upper, "Dimension upper edge expression");
    }
}
