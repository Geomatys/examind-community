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

package org.constellation.json.binding;


import org.constellation.json.util.StyleUtilities;
import org.geotoolkit.style.StyleConstants;
import org.geotoolkit.style.function.ThreshholdsBelongTo;
import org.opengis.filter.Expression;
import org.opengis.filter.Literal;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;
import static org.geotoolkit.filter.FilterUtilities.FF;

/**
 * @author Benjamin Garcia (Geomatys)
 */
public class Categorize implements Function {

    private static final long serialVersionUID = 1L;

    private List<InterpolationPoint> points = new ArrayList<InterpolationPoint>();

    private Double interval;

    private String nanColor;

    public Categorize() {
    }

    @SuppressWarnings("rawtypes")
    public Categorize(final org.geotoolkit.style.function.Categorize categorize) {
        ensureNonNull("categorize", categorize);
        final Map<Expression, Expression> thresholdsMap = categorize.getThresholds();
        if (thresholdsMap != null) {
            for (final Map.Entry<Expression, Expression> entry : thresholdsMap.entrySet()) {
                final InterpolationPoint ip = new InterpolationPoint();
                final Expression expression = entry.getKey();
                final Expression colorHexExp = entry.getValue();

                if (colorHexExp instanceof Literal) {
                    final Object colorHex = ((Literal) colorHexExp).getValue();
                    ip.setColor(StyleUtilities.toHex((Color) colorHex));
                }

                if (expression instanceof Literal) {
                    final Object obj = ((Literal) expression).getValue();
                    if (obj instanceof Double) {
                        if (Double.isNaN((double) obj)) {
                            ip.setData(null);
                            nanColor = ip.getColor();
                        } else {
                            ip.setData((Number) obj);
                        }
                    } else if (StyleConstants.CATEGORIZE_LESS_INFINITY.equals(expression)) {
                        continue; //skip for infinity first key it will be restored later.
                    }
                }
                points.add(ip);
            }
            this.interval = (double) categorize.getThresholds().size();
        }
    }


    public List<InterpolationPoint> getPoints() {
        return points;
    }

    public void setPoints(List<InterpolationPoint> points) {
        this.points = points;
    }

    public List<InterpolationPoint> reComputePoints(final Integer nbPoints) {
        //remove nan point if exists because it is added later, and it cause error for max/min values
        final List<InterpolationPoint> nullPoints = new ArrayList<>();
        for (final InterpolationPoint ip : points) {
            if (ip.getData() == null) {
                nullPoints.add(ip);
            }
        }
        points.removeAll(nullPoints);
        final Map<Expression, Expression> values = new HashMap<>();
        values.put(StyleConstants.CATEGORIZE_LESS_INFINITY, FF.literal(Color.GRAY));
        for (final InterpolationPoint ip : points) {
            values.put(FF.literal(ip.getData().doubleValue()),
                       FF.literal(ip.getColor()));
        }
        final org.geotoolkit.style.function.Categorize categorize = SF.categorizeFunction(StyleConstants.DEFAULT_CATEGORIZE_LOOKUP,
                values,
                ThreshholdsBelongTo.PRECEDING,
                StyleConstants.DEFAULT_FALLBACK);

        // Iteration to find min and max values
        Double min = null, max = null;
        for (final InterpolationPoint ip : points) {
            if (min == null && max == null) {
                min = ip.getData().doubleValue();
                max = ip.getData().doubleValue();
            }
            min = Math.min(min, ip.getData().doubleValue());
            max = Math.max(max, ip.getData().doubleValue());
        }

        //init final threshold map and coefficient
        final Map<Expression, Expression> valuesRecompute = new HashMap<>();
        if (nanColor != null) {
            valuesRecompute.put(FF.literal(Double.NaN),
                                FF.literal(Color.decode(nanColor)));
        }

        if (min != null && max != null && nbPoints != null) {
            double coefficient = (max-min) / (nbPoints - 1);
            // Loop to create values with new point evaluation
            for (int i = 0; i < nbPoints; i++) {
                double val = min + (coefficient * i);
                Color color = (Color) categorize.apply(val);
                valuesRecompute.put(FF.literal(val), FF.literal(color));
            }
        }

        final List<InterpolationPoint> recomputePoints = new ArrayList<>();
        for(final Map.Entry<Expression,Expression> entry : valuesRecompute.entrySet()) {
            final Literal value = (Literal)entry.getKey();
            final Literal color = (Literal)entry.getValue();

            final Color colorObj = (Color) color.getValue();
            final Double valueObj = (Double) value.getValue();
            final InterpolationPoint point = new InterpolationPoint();
            point.setColor(StyleUtilities.toHex(colorObj));
            point.setData(valueObj);
            recomputePoints.add(point);
        }
        //sort recomputePoints
        Collections.sort(recomputePoints,new InterpolationPointComparator());
        return recomputePoints;
    }

    @Override
    public Double getInterval() {
        return interval;
    }

    @Override
    public void setInterval(Double interval) {
        this.interval = interval;
    }

    @Override
    public String getNanColor() {
        return nanColor;
    }

    @Override
    public void setNanColor(String nanColor) {
        this.nanColor = nanColor;
    }

    @Override
    public Expression toType() {

        //remove nan point if exists because it is added later, and it cause error for max/min values
        final List<InterpolationPoint> nullPoints = new ArrayList<>();
        for (final InterpolationPoint ip : points) {
            if (ip.getData() == null) {
                nullPoints.add(ip);
            }
        }
        points.removeAll(nullPoints);

        // create first threshold map to create first categorize function.
        Map<Expression, Expression> values = new HashMap<>(0);
        if (nanColor != null) {
            values.put(FF.literal(Double.NaN),
                    FF.literal(nanColor));
        }
        values.put(StyleConstants.CATEGORIZE_LESS_INFINITY, FF.literal("#00ffffff"));
        for (final InterpolationPoint ip : points) {
            values.put(FF.literal(ip.getData().doubleValue()),
                    FF.literal(ip.getColor()));
        }
        return SF.categorizeFunction(StyleConstants.DEFAULT_CATEGORIZE_LOOKUP,
                values,
                ThreshholdsBelongTo.PRECEDING,
                StyleConstants.DEFAULT_FALLBACK);
    }

    private static class InterpolationPointComparator implements Comparator<InterpolationPoint> {
        @Override
        public int compare(InterpolationPoint o1, InterpolationPoint o2) {
            return Double.compare((Double)o1.getData(), (Double)o2.getData());
        }
    }
}
