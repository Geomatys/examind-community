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

import org.apache.sis.util.ObjectConverters;
import org.apache.sis.util.UnconvertibleObjectException;

import java.awt.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Quentin Boileau (Geomatys)
 */
public final class DecorationUtils {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.dto");

    private DecorationUtils() {
    }

    public static float parseFloat(final String str, final int fallback){
        if(str == null) return fallback;

        try{
            return Float.parseFloat(str);
        }catch(NumberFormatException ex){
            LOGGER.log(Level.WARNING, "Not a valid float : "+str,ex);
            return fallback;
        }
    }

    public static Color parseColor(final String strColor, final Float strOpacity, final Color fallback){
        if(strColor == null) return fallback;

        Color color;
        try {
            color = ObjectConverters.convert(strColor, Color.class);
        } catch (UnconvertibleObjectException e) {
            return fallback;
        }

        float opa = strOpacity != null ? strOpacity : 1.0f;
        if (opa < 0 || opa > 1) {
            opa = 1;
        }
        color = new Color(color.getRed() / 256f, color.getGreen() / 256f, color.getBlue() / 256f, opa);


        return color;
    }

    public static String colorToHex(final Color color) {

        String colorCode = "";
        if (color != null ) {
            String redCode = Integer.toHexString(color.getRed());
            String greenCode = Integer.toHexString(color.getGreen());
            String blueCode = Integer.toHexString(color.getBlue());
            if (redCode.length() == 1)      redCode = "0" + redCode;
            if (greenCode.length() == 1)    greenCode = "0" + greenCode;
            if (blueCode.length() == 1)     blueCode = "0" + blueCode;

            int alpha = color.getAlpha();
            if(alpha != 255){
                String alphaCode = Integer.toHexString(alpha);
                if (alphaCode.length() == 1) alphaCode = "0" + alphaCode;
                colorCode = "#" + alphaCode + redCode + greenCode + blueCode;
            }else{
                colorCode = "#" + redCode + greenCode + blueCode;
            }
        }

        return colorCode.toUpperCase();
    }

    public static Float getColorOpacity (final Color color) {
        Float opacity = 1.0f;
        if (color != null) {
            final int alpha = color.getAlpha();
            opacity = alpha * (1.0f/255);
        }
        return opacity;
    }

    public static URL parseURL(final String url, final URL fallback){
        if(url == null) return fallback;

        try{
            return new URL(url);
        }catch(Exception ex){
            LOGGER.log(Level.WARNING, "Could not parse url",ex);
            return fallback;
        }
    }
}
