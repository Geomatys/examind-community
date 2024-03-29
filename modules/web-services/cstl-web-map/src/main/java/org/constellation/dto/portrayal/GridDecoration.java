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
package org.constellation.dto.portrayal;

import org.apache.sis.referencing.CRS;
import org.geotoolkit.display2d.ext.grid.DefaultGridTemplate;
import org.geotoolkit.display2d.ext.grid.GridTemplate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class GridDecoration extends AbstractDecoration {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.dto");
    @XmlElement(name = "MainGrid")
    private Grid mainGrid;
    @XmlElement(name = "SecondGrid")
    private Grid secondGrid;
    @XmlElement(name = "CRS")
    private String crsStr;

    public GridDecoration() {
        this(new Grid(), new Grid(new Stroke(1.0f, null, "#C0C0C0", 1.0f), "serial-BOLD-14"), null);
    }

    public GridDecoration(Grid mainGrid, Grid secondGrid, String crs) {
        this.mainGrid = mainGrid;
        this.secondGrid = secondGrid;
        this.crsStr = crs;
    }

    public Grid getMainGrid() {
        return mainGrid;
    }

    public void setMainGrid(Grid mainGrid) {
        this.mainGrid = mainGrid;
    }

    public Grid getSecondGrid() {
        return secondGrid;
    }

    public void setSecondGrid(Grid secondGrid) {
        this.secondGrid = secondGrid;
    }

    public String getCrsStr() {
        return crsStr;
    }

    public void setCrsStr(String crs) {
        this.crsStr = crs;
    }

    /**
     * Convert to geotoolkit GridTemplate.
     * @return GridTemplate
     */
    public GridTemplate toGridTemplate() {
        CoordinateReferenceSystem crs = null;
        if (crsStr != null) {
            try {
                crs = CRS.forCode(crsStr);
            } catch (FactoryException ex) {
                LOGGER.log(Level.WARNING, null, ex);
            }
        }

        final java.awt.Stroke mainLineStroke = mainGrid.getStroke() != null ? mainGrid.getStroke().toAwtStroke() : new BasicStroke(2);
        final Paint mainLinePaint = DecorationUtils.parseColor(mainGrid.getStroke().getStrokeColor(), mainGrid.getStroke().getStrokeOpacity(), Color.DARK_GRAY);
        final Font mainLineFont = mainGrid.getFont() != null ? Font.decode(mainGrid.getFont()) : new Font("serial", Font.BOLD, 14);

        final java.awt.Stroke secondLineStroke = secondGrid.getStroke() != null ? secondGrid.getStroke().toAwtStroke()
                : new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 5}, 0);
        final Paint secondLinePaint = DecorationUtils.parseColor(secondGrid.getStroke().getStrokeColor(), secondGrid.getStroke().getStrokeOpacity(), Color.GRAY);
        final Font secondLineFont = secondGrid.getFont() != null ? Font.decode(secondGrid.getFont()) : new Font("serial", Font.BOLD, 14);

        return new DefaultGridTemplate(
                crs,
                mainLineStroke,
                mainLinePaint,
                secondLineStroke,
                secondLinePaint,
                mainLineFont,
                mainLinePaint,
                0,
                new Color(0f, 0f, 0f, 0f),
                secondLineFont,
                secondLinePaint,
                0,
                new Color(0f, 0f, 0f, 0f));
    }
}
