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
import org.opengis.filter.expression.Expression;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.*;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class TextSymbolizer implements Symbolizer {
    private String name;
    private String label = null;
    private Font font    = new Font();
    private Fill fill    = new Fill();
    private Halo halo    = new Halo();

    public TextSymbolizer() {
    }

    public TextSymbolizer(final org.opengis.style.TextSymbolizer symbolizer) {
        ensureNonNull("symbolizer", symbolizer);
        name = symbolizer.getName();
        final Expression labelExp = symbolizer.getLabel();
        if (labelExp != null) {
            this.label = StyleUtilities.toCQL(labelExp);
        }
        if (symbolizer.getFont() != null) {
            this.font = new Font(symbolizer.getFont());
        }
        if (symbolizer.getFill() != null) {
            this.fill = new Fill(symbolizer.getFill());
        }
        if (symbolizer.getHalo() != null) {
            this.halo = new Halo(symbolizer.getHalo());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(final Font font) {
        this.font = font;
    }

    public Fill getFill() {
        return fill;
    }

    public void setFill(final Fill fill) {
        this.fill = fill;
    }

    public Halo getHalo() {
        return halo;
    }

    public void setHalo(Halo halo) {
        this.halo = halo;
    }

    @Override
    public org.opengis.style.Symbolizer toType() {
        return SF.textSymbolizer(name,
                (String)null,
                null,
                null,
                parseExpression(label),
                type(font),
                null,
                type(halo),
                type(fill));
    }
}
