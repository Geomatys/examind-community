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

import org.geotoolkit.style.MutableRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.constellation.json.util.StyleFactories.SF;
import static org.constellation.json.util.StyleUtilities.filter;
import static org.constellation.json.util.StyleUtilities.listType;

/**
 * @author Fabien Bernard (Geomatys).
 * @version 0.9
 * @since 0.9
 */
public final class Rule implements StyleElement<MutableRule> {

    private String name                  = "Change me!";
    private String title                 = "";
    private String description           = "";
    private double minScale              = 0.0;
    private double maxScale              = Double.MAX_VALUE;
    private List<Symbolizer> symbolizers = new ArrayList<>(0);
    private String filter                = null;

    public Rule() {
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public double getMinScale() {
        return minScale;
    }

    public void setMinScale(final double minScale) {
        this.minScale = minScale;
    }

    public double getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(final double maxScale) {
        this.maxScale = maxScale;
    }

    public List<Symbolizer> getSymbolizers() {
        return symbolizers;
    }

    public void setSymbolizers(final List<Symbolizer> symbolizers) {
        this.symbolizers = symbolizers;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(final String filter) {
        this.filter = filter;
    }

    @Override
    public MutableRule toType() {
        return SF.rule(
            name,
            SF.description(title != null ? title : "", description != null ? description : ""),
            null,
            minScale,
            maxScale,
            listType(symbolizers),
            filter(filter));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Rule that = (Rule) obj;
            return  Objects.equals(this.description, that.description) &&
                    Objects.equals(this.filter, that.filter) &&
                    Objects.equals(this.maxScale, that.maxScale) &&
                    Objects.equals(this.minScale, that.minScale) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.symbolizers, that.symbolizers) &&
                    Objects.equals(this.title, that.title);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.name);
        hash = 71 * hash + Objects.hashCode(this.title);
        hash = 71 * hash + Objects.hashCode(this.description);
        hash = 71 * hash + (int) (Double.doubleToLongBits(this.minScale) ^ (Double.doubleToLongBits(this.minScale) >>> 32));
        hash = 71 * hash + (int) (Double.doubleToLongBits(this.maxScale) ^ (Double.doubleToLongBits(this.maxScale) >>> 32));
        hash = 71 * hash + Objects.hashCode(this.symbolizers);
        hash = 71 * hash + Objects.hashCode(this.filter);
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Rule]\n");
        sb.append("name=").append(name).append('\n');
        if (symbolizers != null) {
            sb.append("symbolizers=\n");
            for (Symbolizer ht : symbolizers) {
                sb.append(ht).append('\n');
            }
        }
        sb.append("description=").append(description).append('\n');
        sb.append("filter=").append(filter).append('\n');
        sb.append("maxScale=").append(maxScale).append('\n');
        sb.append("minScale=").append(minScale).append('\n');
        sb.append("title=").append(title).append('\n');
        return sb.toString();
    }
}
