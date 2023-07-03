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

import org.geotoolkit.display2d.ext.BackgroundTemplate;
import org.geotoolkit.display2d.ext.DefaultBackgroundTemplate;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.awt.*;
import java.io.Serializable;

/**
 * Background used by decorations and legend template.
 *
 * @author Quentin Boileau (Geomatys).
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Background implements Serializable {

    @XmlElement(name = "Stroke")
    private Stroke stroke;
    
    @XmlElement(name = "FillColor")
    private String fillColor;
    
    @XmlElement(name = "FillOpacity")
    private Float fillOpacity;
    
    @XmlElement(name = "Inset")
    private Inset inset;
    
    @XmlElement(name = "Round")
    private Float round;

    /**
     * Default background.
     */
    public Background() {
        this.stroke = new Stroke();
        this.fillColor = "#FFFFFF";
        this.fillOpacity = 1.0f;
        this.inset = new Inset();
        this.round = 10.0f;
    }

    public Background(final Stroke stroke, final String fillColor, final Float fillOpacity, final Inset inset, final Float round) {
        this.stroke = stroke;
        this.fillColor = fillColor;
        this.fillOpacity = fillOpacity;
        this.inset = inset;
        this.round = round;
    }

    public Background (final BackgroundTemplate displayBackground) {
        
        final String strokePaint = DecorationUtils.colorToHex((Color)displayBackground.getBackgroundStrokePaint());
        final Float strokeOpacity = DecorationUtils.getColorOpacity((Color)displayBackground.getBackgroundStrokePaint());
        this.stroke = new Stroke(displayBackground.getBackgroundStroke(), strokePaint, strokeOpacity);
        this.inset = new Inset(displayBackground.getBackgroundInsets());
        this.fillColor = DecorationUtils.colorToHex((Color)displayBackground.getBackgroundPaint());
        this.fillOpacity = DecorationUtils.getColorOpacity((Color)displayBackground.getBackgroundPaint());
        this.round = (float) displayBackground.getRoundBorder();
    }
            
    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public Float getFillOpacity() {
        return fillOpacity;
    }

    public void setFillOpacity(Float fillOpacity) {
        this.fillOpacity = fillOpacity;
    }

    public Inset getInset() {
        return inset;
    }

    public void setInset(Inset inset) {
        this.inset = inset;
    }

    public Float getRound() {
        return round;
    }

    public void setRound(Float round) {
        this.round = round;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }
    
    /**
     * Convert to displayable background.
     * @return BackgroundTemplate
     */
    public BackgroundTemplate toBackgroundTemplate() {
        
        final java.awt.Stroke awtStroke = stroke.toAwtStroke();
        final Insets awtInset = inset.toAwtInsets();
        final Color strokePaint = DecorationUtils.parseColor(stroke.getStrokeColor(), stroke.getStrokeOpacity(), Color.DARK_GRAY);
        final Color fillPaint = DecorationUtils.parseColor(fillColor, fillOpacity, Color.WHITE);
        
        return new DefaultBackgroundTemplate(awtStroke, strokePaint, fillPaint, awtInset, round.intValue());
    }
    
}
