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
import org.constellation.json.util.StyleUtilities;
import org.opengis.filter.Expression;

import java.util.ArrayList;
import java.util.List;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.literal;
import static org.constellation.json.util.StyleUtilities.parseExpression;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class Font implements StyleElement<org.opengis.style.Font> {

    private String size    = "12";
    private boolean bold   = false;
    private boolean italic = false;
    private List<String> family = new ArrayList<>();

    public Font() {
    }

    public Font(org.opengis.style.Font font) {
        ensureNonNull("font", font);

        final Expression sizeExp = font.getSize();
        if(sizeExp != null){
            size = StyleUtilities.toCQL(sizeExp);
        }
        final Expression weightExp = font.getWeight();
        final String weightStr = StyleUtilities.toCQL(weightExp);
        if (weightExp != null && weightStr != null) {
            bold = weightStr.toLowerCase().contains("bold");
        }
        final Expression styleExp = font.getStyle();
        final String styleStr = StyleUtilities.toCQL(styleExp);
        if (styleExp != null && styleStr != null) {
            italic = styleStr.toLowerCase().contains("italic");
        }
        for (final Expression fam : font.getFamily()) {
            family.add((String) fam.apply(null));
        }
    }

    public String getSize() {
        return size;
    }

    public void setSize(final String size) {
        this.size = size;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(final boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(final boolean italic) {
        this.italic = italic;
    }

    public List<String> getFamily() {
        return family;
    }

    public void setFamily(List<String> family) {
        this.family = family;
    }

    @Override
    public org.opengis.style.Font toType() {
        final List<Expression> famExp = new ArrayList<>();
        for (final String fam : family) {
            famExp.add(literal(fam));
        }
        if (famExp.isEmpty()) {
            famExp.add(literal("Arial"));
        }
        return SF.font(
                famExp,
                literal(this.italic ? "italic" : "normal"),
                literal(this.bold ? "bold" : "normal"),
                parseExpression(this.size));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Font font = (Font) o;

        if (bold != font.bold) return false;
        if (italic != font.italic) return false;
        if (!Objects.equals(size, font.size)) return false;
        return Objects.equals(family, font.family);
    }

    @Override
    public int hashCode() {
        int result = size != null ? size.hashCode() : 0;
        result = 31 * result + (bold ? 1 : 0);
        result = 31 * result + (italic ? 1 : 0);
        result = 31 * result + (family != null ? family.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Font.class.getSimpleName() + "[", "]")
                .add("size='" + size + "'")
                .add("bold=" + bold)
                .add("italic=" + italic)
                .toString();
    }
}
