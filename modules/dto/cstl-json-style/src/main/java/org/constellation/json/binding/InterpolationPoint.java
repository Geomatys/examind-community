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
import org.opengis.filter.Literal;

import java.awt.*;
import java.util.logging.Logger;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.literal;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class InterpolationPoint implements StyleElement<org.geotoolkit.style.function.InterpolationPoint> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger("org.constellation.json.binding");

    private Number data = null;
    private String color = "#000000";

    public InterpolationPoint() {
    }

    public InterpolationPoint(Number data, String color) {
        this.color = color;
        this.data = data;
    }

    public InterpolationPoint(final org.geotoolkit.style.function.InterpolationPoint interpolationPoint) {
        ensureNonNull("interpolationPoint", interpolationPoint);
        double value = interpolationPoint.getData().doubleValue();
        if (Double.isNaN(value)) {
            data = null;
        } else {
            data = value;
        }
        if (interpolationPoint.getValue() instanceof Literal) {
            final Object obj = ((Literal) interpolationPoint.getValue()).getValue();
            if (obj instanceof Color col) {
                color = StyleUtilities.toHex(col);
            } else if (obj instanceof String colStr) {
                color = colStr;
            } else if (obj != null) {
                 // don't throw exception for unexpected
                LOGGER.warning("Unable to convert interpolation point literal value:" + obj);
            }
        }
    }

    public Number getData() {
        return data;
    }

    public void setData(final Number data) {
        this.data = data;
    }

    public String getColor() {
        return color;
    }

    public void setColor(final String color) {
        this.color = color;
    }

    @Override
    public org.geotoolkit.style.function.InterpolationPoint toType() {
        return SF.interpolationPoint(data, literal(StyleUtilities.COLOR_CONVERTER.apply(color)));
    }
}
