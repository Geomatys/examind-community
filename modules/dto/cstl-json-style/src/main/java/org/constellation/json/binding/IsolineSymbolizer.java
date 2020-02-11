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

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

/**
 *
 * @author Guilhem Legal Geomatys
 */
public class IsolineSymbolizer implements Symbolizer {

    private String name;
    private RasterSymbolizer rasterSymbolizer;
    private LineSymbolizer lineSymbolizer;
    private TextSymbolizer textSymbolizer;
    private Boolean isolineOnly;

    public IsolineSymbolizer() {
    }

    public IsolineSymbolizer(org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer symbolizer) {
        ensureNonNull("symbolizer", symbolizer);
        this.name = symbolizer.getName();
        this.isolineOnly = symbolizer.getIsolineOnly();
        if (symbolizer.getLineSymbolizer() != null) {
            this.lineSymbolizer = new LineSymbolizer(symbolizer.getLineSymbolizer());
        }
        if (symbolizer.getRasterSymbolizer()!= null) {
            this.rasterSymbolizer = new RasterSymbolizer(symbolizer.getRasterSymbolizer());
        }
        if (symbolizer.getTextSymbolizer()!= null) {
            this.textSymbolizer = new TextSymbolizer(symbolizer.getTextSymbolizer());
        }
    }

    /**
     * @return the rasterSymbolizer
     */
    public RasterSymbolizer getRasterSymbolizer() {
        return rasterSymbolizer;
    }

    /**
     * @param rasterSymbolizer the rasterSymbolizer to set
     */
    public void setRasterSymbolizer(RasterSymbolizer rasterSymbolizer) {
        this.rasterSymbolizer = rasterSymbolizer;
    }

    /**
     * @return the lineSymbolizer
     */
    public LineSymbolizer getLineSymbolizer() {
        return lineSymbolizer;
    }

    /**
     * @param lineSymbolizer the lineSymbolizer to set
     */
    public void setLineSymbolizer(LineSymbolizer lineSymbolizer) {
        this.lineSymbolizer = lineSymbolizer;
    }

    /**
     * @return the textSymbolizer
     */
    public TextSymbolizer getTextSymbolizer() {
        return textSymbolizer;
    }

    /**
     * @param textSymbolizer the textSymbolizer to set
     */
    public void setTextSymbolizer(TextSymbolizer textSymbolizer) {
        this.textSymbolizer = textSymbolizer;
    }

    /**
     * @return the isolineOnly
     */
    public Boolean isIsolineOnly() {
        return isolineOnly;
    }

    /**
     * @param isolineOnly the isolineOnly to set
     */
    public void setIsolineOnly(Boolean isolineOnly) {
        this.isolineOnly = isolineOnly;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public org.opengis.style.Symbolizer toType() {
        org.opengis.style.RasterSymbolizer rs = null;
        if (rasterSymbolizer != null) {
            rs = (org.opengis.style.RasterSymbolizer) rasterSymbolizer.toType();
        }
        org.opengis.style.LineSymbolizer ls = null;
        if (lineSymbolizer != null) {
            ls = (org.opengis.style.LineSymbolizer) lineSymbolizer.toType();
        }
        org.opengis.style.TextSymbolizer ts = null;
        if (textSymbolizer != null) {
            ts = (org.opengis.style.TextSymbolizer) textSymbolizer.toType();
        }
        org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer result =
            new org.geotoolkit.display2d.ext.isoline.symbolizer.IsolineSymbolizer(rs, ls, ts, isolineOnly);
        result.setName(name);
        return result;
    }
}
