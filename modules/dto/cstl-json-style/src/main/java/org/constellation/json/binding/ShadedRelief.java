/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;
import static org.constellation.json.util.StyleFactories.SF;

/**
 * @author Alessandro Valeri (Geomatys).
 */
public final class ShadedRelief implements StyleElement<org.opengis.style.ShadedRelief> {

    private String reliefFactor = "0.0";


    public ShadedRelief() {
    }

    public ShadedRelief(final org.opengis.style.ShadedRelief shadedRelief) {
        ensureNonNull("shadedRelief", shadedRelief);
        if (shadedRelief.getReliefFactor() != null) {
            this.reliefFactor = StyleUtilities.toCQL(shadedRelief.getReliefFactor());
        }
    }


    @Override
    public org.opengis.style.ShadedRelief toType() {
        return SF.shadedRelief(StyleUtilities.parseExpression(reliefFactor));
    }

    public String getReliefFactor() {
        return reliefFactor;
    }

    public void setReliefFactor(String reliefFactor) {
        this.reliefFactor = reliefFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShadedRelief that = (ShadedRelief) o;

        return reliefFactor != null ? reliefFactor.equals(that.reliefFactor) : that.reliefFactor == null;
    }

    @Override
    public int hashCode() {
        return reliefFactor != null ? reliefFactor.hashCode() : 0;
    }

}
