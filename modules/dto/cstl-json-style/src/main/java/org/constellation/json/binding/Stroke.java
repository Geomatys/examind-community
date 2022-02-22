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

import java.awt.Color;
import org.constellation.json.util.StyleUtilities;
import org.opengis.filter.Expression;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.literal;
import static org.constellation.json.util.StyleUtilities.parseExpression;
import static org.constellation.json.util.StyleUtilities.toHex;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class Stroke implements StyleElement<org.opengis.style.Stroke> {

    private String color   = "#000000";
    private Function function;
    private String opacity = "1.0";
    private String width   = "1.0";
    private boolean dashed = false;
    private String lineJoin = "round";
    private String lineCap = "round";
    private float[] dashArray;
    private double dashOffset;

    public Stroke() {
    }

    public Stroke(final org.opengis.style.Stroke stroke) {
        ensureNonNull("stroke", stroke);
        if (stroke.getColor() instanceof org.geotoolkit.style.function.Interpolate) {
            function = new Interpolate((org.geotoolkit.style.function.Interpolate)stroke.getColor());
        } else {
            final Color col = (Color) stroke.getColor().apply(null);
            color = toHex(col);
        }
        final Expression opacityExp = stroke.getOpacity();
        if(opacityExp != null){
            opacity = StyleUtilities.toCQL(opacityExp);
        }

        final Expression widthExp = stroke.getWidth();
        if(widthExp != null){
            width = StyleUtilities.toCQL(widthExp);
        }
        dashed  = (stroke.getDashArray() != null);
        lineJoin = stroke.getLineJoin().apply(null).toString();
        lineCap = stroke.getLineCap().apply(null).toString();
        dashArray = stroke.getDashArray();
        try{
            dashOffset = Double.parseDouble(stroke.getDashOffset().apply(null).toString());
        }catch(Exception ex){
            //do nothing
        }
    }

    public String getColor() {
        return color;
    }

    public String getOpacity() {
        return opacity;
    }

    public void setOpacity(final String opacity) {
        this.opacity = opacity;
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(final String width) {
        this.width = width;
    }

    public boolean getDashed() {
        return dashed;
    }

    public void setDashed(final boolean dashed) {
        this.dashed = dashed;
    }

    public float[] getDashArray() {
        return dashArray;
    }

    public void setDashArray(float[] dashArray) {
        this.dashArray = dashArray;
    }

    public double getDashOffset() {
        return dashOffset;
    }

    public void setDashOffset(String dashOffset) {
        try{
            this.dashOffset = Double.parseDouble(dashOffset);
        }catch(Exception ex){
            //do nothing
        }
    }

    public String getLineJoin() {
        return lineJoin;
    }

    public void setLineJoin(String lineJoin) {
        this.lineJoin = lineJoin;
    }

    public String getLineCap() {
        return lineCap;
    }

    public void setLineCap(String lineCap) {
        this.lineCap = lineCap;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    @Override
    public org.opengis.style.Stroke toType() {
        Expression exp;
        if (function != null) {
            exp = function.toType();
        } else {
            exp = literal(this.color);
        }
        return SF.stroke(
                exp,
                parseExpression(opacity),
                parseExpression(this.width),
                literal(this.lineJoin),
                literal(this.lineCap),
                dashArray,
                literal(this.dashOffset));
    }
}
