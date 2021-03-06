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
import org.geotoolkit.filter.DefaultLiteral;
import org.geotoolkit.style.StyleConstants;
import org.geotoolkit.style.function.Method;
import org.geotoolkit.style.function.Mode;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.FF;
import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.listType;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class Interpolate implements Function {

    private static final long serialVersionUID = 1L;

    private List<InterpolationPoint> points = new ArrayList<>();

    private Double interval;

    private String nanColor;

    private String propertyName;

    public Interpolate() {
    }

    public Interpolate(final org.geotoolkit.style.function.Interpolate interpolate) {
        ensureNonNull("interpolate", interpolate);
        if (interpolate.getInterpolationPoints() != null) {
            for (final org.geotoolkit.style.function.InterpolationPoint point : interpolate.getInterpolationPoints()) {
                this.points.add(new InterpolationPoint(point));
                if (nanColor == null &&
                        point.getData() instanceof Double &&
                        Double.isNaN((double) point.getData())) {
                    if (point.getValue() instanceof DefaultLiteral) {
                        final Object colorHex = ((DefaultLiteral) point.getValue()).getValue();
                        nanColor = StyleUtilities.toHex(((Color) colorHex));
                    }
                }
            }
            this.interval = (double) interpolate.getInterpolationPoints().size();
        }
        if (interpolate.getLookupValue() instanceof PropertyName) {
            propertyName = ((PropertyName)interpolate.getLookupValue()).getPropertyName();
        }
    }

    public List<InterpolationPoint> getPoints() {
        return points;
    }

    public void setPoints(final List<InterpolationPoint> points) {
        this.points = points;
    }

    /**
     * Calculate points for palette and return the list of interpolation points.
     * @return {@code List<InterpolationPoint>}
     */
    public List<InterpolationPoint> reComputePoints(final Integer nbPoints) {
        //remove nan point if exists because it is added later, and it cause error for max/min values
        final List<InterpolationPoint> nullPoints = new ArrayList<>();
        for (final InterpolationPoint ip : points) {
            if(ip.getData() == null){
                nullPoints.add(ip);
            }
        }
        points.removeAll(nullPoints);
        final org.geotoolkit.style.function.Interpolate inter =  SF.interpolateFunction(StyleConstants.DEFAULT_CATEGORIZE_LOOKUP,
                listType(points),
                Method.COLOR,
                Mode.LINEAR,
                StyleConstants.DEFAULT_FALLBACK);
        Double min = null, max= null;
        // Iteration to find min and max values
        for (final InterpolationPoint ip : points) {
            if(min==null && max==null){
                min = ip.getData().doubleValue();
                max = ip.getData().doubleValue();
            }
            min = Math.min(min,ip.getData().doubleValue());
            max = Math.max(max,ip.getData().doubleValue());
        }
        //init final InterpolationPoint list and coefficient
        final List<InterpolationPoint> recomputePoints = new ArrayList<>();
        if(nanColor !=null){
            final InterpolationPoint nanPoint = new InterpolationPoint();
            nanPoint.setColor(nanColor);
            nanPoint.setData(Double.NaN);
            recomputePoints.add(nanPoint);
        }
        if(max !=null && min != null && nbPoints != null) {
            double coefficient = (max-min) / (nbPoints - 1);
            // Loop to create points with new point evaluation
            for (int i = 0; i < nbPoints; i++) {
                final double val = min + (coefficient * i);
                final Color color = inter.evaluate(val, Color.class);
                final InterpolationPoint point = new InterpolationPoint();
                point.setColor(StyleUtilities.toHex(color));
                point.setData(val);
                recomputePoints.add(point);
            }
        }
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

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public org.opengis.filter.expression.Function toType() {

        //remove nan point if exists because it is added later, and it cause error for max/min values
        final List<InterpolationPoint> nullPoints = new ArrayList<>();
        for (final InterpolationPoint ip : points) {
            if (ip.getData() == null) {
                nullPoints.add(ip);
            }
        }
        points.removeAll(nullPoints);
        if (nanColor != null) {
            final InterpolationPoint nanPoint = new InterpolationPoint();
            nanPoint.setColor(nanColor);
            nanPoint.setData(Double.NaN);
            points.add(nanPoint);
        }
        Expression lookup;
        if (propertyName != null && !propertyName.isEmpty()) {
            lookup = FF.property(propertyName);
        } else {
            lookup = StyleConstants.DEFAULT_CATEGORIZE_LOOKUP;
        }
        return SF.interpolateFunction(lookup,
                listType(points),
                Method.COLOR,
                Mode.LINEAR,
                StyleConstants.DEFAULT_FALLBACK);

    }

}
