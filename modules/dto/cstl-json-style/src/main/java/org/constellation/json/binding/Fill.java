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

import java.util.Objects;
import org.constellation.json.util.StyleUtilities;
import org.opengis.filter.Expression;

import java.awt.*;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.literal;
import static org.constellation.json.util.StyleUtilities.parseExpression;
import static org.constellation.json.util.StyleUtilities.toHex;
import org.opengis.filter.Literal;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class Fill implements StyleElement<org.opengis.style.Fill> {

    private String color   = "#555555";
    private String opacity = "1.0";

    public Fill() {
    }

    public Fill(final org.opengis.style.Fill fill) {
        ensureNonNull("fill", fill);
        if (fill.getColor() instanceof Literal lit) {
           if (lit.getValue() instanceof Color col) {
               color = toHex(col);
           } else if (lit.getValue() instanceof String colStr) {
               color = colStr;
           } else {
               throw new IllegalArgumentException("Expected Color or String value for Fill color literal");
           }
        } else if (fill.getColor() != null) {
            throw new IllegalArgumentException("Expected literal or function interpolate for Fill color");
        }
        final Expression opacityExp = fill.getOpacity();
        if(opacityExp != null){
            opacity = StyleUtilities.toCQL(opacityExp);
        }
    }

    public String getColor() {
        return color;
    }

    public void setColor(final String color) {
        this.color = color;
    }

    public String getOpacity() {
        return opacity;
    }

    public void setOpacity(final String opacity) {
        this.opacity = opacity;
    }

    @Override
    public org.opengis.style.Fill toType() {
        return SF.fill(literal(color), parseExpression(opacity));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fill fill = (Fill) o;

        return Objects.equals(color, fill.color)
                && Objects.equals(opacity, fill.opacity);
    }

    @Override
    public int hashCode() {
        int result = color != null ? color.hashCode() : 0;
        result = 31 * result + (opacity != null ? opacity.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Fill[opacity='"+opacity+"', color='"+color+"']";
    }
}
