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
import java.util.StringJoiner;
import org.apache.sis.measure.Units;
import org.constellation.json.util.StyleUtilities;
import org.opengis.feature.Feature;
import org.opengis.filter.Expression;

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
    private LabelPlacement labelPlacement;
    private String unit;
    private String geometry;

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
        if (symbolizer.getLabelPlacement() != null) {
            this.labelPlacement = LabelPlacement.toJsonBinding(symbolizer.getLabelPlacement()).orElse(null);
        }
        this.unit = stringify(symbolizer.getUnitOfMeasure()).orElse(null);
        final Expression<Feature, ?> geom = symbolizer.getGeometry();
        if (geom != null) this.geometry = toCQL(geom);
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

    /**
     *
     * @return Rules that specify text position relative to source geometry, may be null.
     */
    public LabelPlacement getLabelPlacement() {
        return labelPlacement;
    }

    public void setLabelPlacement(LabelPlacement labelPlacement) {
        this.labelPlacement = labelPlacement;
    }

    /**
     * Unit used when expressing gaps and displacement. By default, when null, in pixels (target/UI space).
     * If a unit symbol/name is specified (m, meter, degrees, etc.), then distances will be expressed as map values
     * (source/objective space).
     */
    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * An expression that indicates what geoemetry this label applies upon. If null, default geometry of rendered entity
     * will be used. Otherwise, it must be an expression capable of extracting/generating a geometry from the entity.
     */
    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    @Override
    public org.opengis.style.Symbolizer toType() {
        return SF.textSymbolizer(name,
                geometry == null || geometry.isEmpty() ? null : parseExpression(geometry),
                null,
                unit == null || unit.isEmpty() ? null : Units.valueOf(unit),
                parseExpression(label),
                type(font),
                type(labelPlacement),
                type(halo),
                type(fill));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextSymbolizer that = (TextSymbolizer) o;

        return Objects.equals(name, that.name)
                && Objects.equals(label, that.label)
                && Objects.equals(font, that.font)
                && Objects.equals(fill, that.fill)
                && Objects.equals(halo, that.halo)
                && Objects.equals(labelPlacement, that.labelPlacement)
                && Objects.equals(unit, that.unit)
                && Objects.equals(geometry, that.geometry);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (font != null ? font.hashCode() : 0);
        result = 31 * result + (fill != null ? fill.hashCode() : 0);
        result = 31 * result + (halo != null ? halo.hashCode() : 0);
        result = 31 * result + (labelPlacement != null ? labelPlacement.hashCode() : 0);
        result = 31 * result + (unit != null ? unit.hashCode() : 0);
        result = 31 * result + (geometry != null ? geometry.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "TextSymbolizer[", "]")
                .add("name='" + name + "'")
                .add("label='" + label + "'")
                .add("font=" + font)
                .add("fill=" + fill)
                .add("halo=" + halo)
                .add("labelPlacement=" + labelPlacement)
                .add("unit='" + unit + "'")
                .add("geometry='" + geometry + "'")
                .toString();
    }
}
