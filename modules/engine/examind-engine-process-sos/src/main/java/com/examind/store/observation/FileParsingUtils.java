/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2021, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package com.examind.store.observation;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotoolkit.gml.xml.AbstractGeometry;
import org.geotoolkit.gml.xml.GMLXmlFactory;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.sampling.xml.SamplingFeature;
import org.geotoolkit.observation.OMUtils;
import static org.geotoolkit.observation.OMUtils.TIME_FIELD;
import org.geotoolkit.sos.xml.SOSXmlFactory;
import org.geotoolkit.swe.xml.AbstractDataRecord;
import org.geotoolkit.swe.xml.AnyScalar;
import org.geotoolkit.swe.xml.Quantity;
import org.geotoolkit.swe.xml.UomProperty;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileParsingUtils {

    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");

    private static final NumberFormat FR_FORMAT = NumberFormat.getInstance(Locale.FRANCE);
    
    /**
     * Return the value int the csv line if the supplied index is different from -1.
     * Else return the default value specified.
     * 
     * @param index column index
     * @param line parsed CSV line.
     * @param defaultValue value to returne if column == -1
     * 
     * @return The value of the column or the default value.
     */
    public static String getColumnValue(int index, String[] line, String defaultValue) {
        String result = defaultValue;
        if (index != -1) {
            result = line[index];
        }
        return result;
    }

    /**
     * Verify if the line is empty, meaning no measure field is filled.
     * 
     * @param line parsed CSV line.
     * @param lineNumber line number in the file (for logging purpose).
     * @param doubleFields List of column index where to look for Double value.
     *
     * @return {@code true} if the line is considered empty.
     */
    public static boolean verifyEmptyLine(String[] line, int lineNumber, List<Integer> doubleFields) {
        boolean empty = true;
        for (int i : doubleFields) {
            try {
                String value = line[i];
                if (value == null || (value = value.trim()).isEmpty()) continue;
                parseDouble(value);
                empty = false;
                break;
            } catch (NumberFormatException | ParseException ex) {
                if (!line[i].isEmpty()) {
                    LOGGER.fine(String.format("Problem parsing double value at line %d and column %d (value='%s')", lineNumber, i, line[i]));
                }
            }
        }
        return empty;
    }

    public static String extractWithRegex(String regex, String value, String defaultValue) {
        String result = defaultValue;
         if (regex != null && value != null) {
            final Pattern pa = Pattern.compile(regex);
            final Matcher m = pa.matcher(value);
            if (m.find() && m.groupCount() >= 1) {
                result = m.group(1).trim();
            }
        }
        return result;
    }

    /**
     * Parse a string double with dot or comma separator.
     * @param s string value of a double.
     * @return : double.
     * @throws ParseException the parse method failed.
     */
    public static double parseDouble(String s) throws ParseException {
        if (s.contains(",")) {
            synchronized(FR_FORMAT) {
                Number number = FR_FORMAT.parse(s);
                return number.doubleValue();
            }
        } else {
            return Double.parseDouble(s);
        }
    }

    public static AbstractGeometry buildGeom(final List<DirectPosition> positions) {
        final AbstractGeometry sp;
        if (positions.isEmpty()) {
            return null;
        } else if (positions.size() > 1) {
            sp = GMLXmlFactory.buildLineString("3.2.1", null, "EPSG:4326", positions);
        } else {
            sp = GMLXmlFactory.buildPoint("3.2.1", null, "EPSG:4326", positions.get(0));
        }
        return sp;
    }

    public static SamplingFeature buildFOIByGeom(String foiID, final List<DirectPosition> positions, final Set<org.opengis.observation.sampling.SamplingFeature> existingFeatures) {
        final SamplingFeature sp;
        if (positions.isEmpty()) {
            sp = SOSXmlFactory.buildSamplingFeature("2.0.0", foiID, null, null, null);
        } else if (positions.size() > 1) {
            sp = OMUtils.buildSamplingCurve(foiID, positions);
        } else {
            sp = OMUtils.buildSamplingPoint(foiID, positions.get(0).getOrdinate(0),  positions.get(0).getOrdinate(1));
        }
        for (org.opengis.observation.sampling.SamplingFeature existingFeature : existingFeatures) {

            if (existingFeature instanceof SamplingFeature) {
                SamplingFeature ef = (SamplingFeature) existingFeature;
                if ((ef.getGeometry() == null && sp.getGeometry() == null) ||
                    (ef.getGeometry() != null && equalsGeom(ef.getGeometry(), positions))
                ) {
                    return ef;
                }
            }
        }
        return sp;
    }

    private static boolean equalsGeom(Geometry current, List<DirectPosition> positions) {
        // the problem here is that the axis will be flipped after save,
        // so we need to flip the axis for comparison...
        Geometry spGeometry;
         if (positions.isEmpty()) {
            return false;
        } else if (positions.size() > 1) {
            List<DirectPosition> flipped = new ArrayList<>();
            positions.forEach(dp -> flipped.add(SOSXmlFactory.buildDirectPosition("2.0.0", "EPSG:4326", 2, Arrays.asList(dp.getCoordinate()[1], dp.getCoordinate()[0]))));
            spGeometry = (Geometry) SOSXmlFactory.buildLineString("2.0.0", null, "EPSG:4326", flipped);
        } else {
            final DirectPosition position = SOSXmlFactory.buildDirectPosition("2.0.0", "EPSG:4326", 2, Arrays.asList(positions.get(0).getOrdinate(1), positions.get(0).getOrdinate(0)));
            spGeometry = (Geometry) SOSXmlFactory.buildPoint("2.0.0", "SamplingPoint", position);
        }
        return current.equals(spGeometry);
    }

    public static SamplingFeature buildFOIById(String foiID, final List<DirectPosition> positions, final Set<org.opengis.observation.sampling.SamplingFeature> existingFeatures) {
        final SamplingFeature sp;
        if (positions.isEmpty()) {
            sp = SOSXmlFactory.buildSamplingFeature("2.0.0", foiID, null, null, null);
        } else if (positions.size() > 1) {
            sp = OMUtils.buildSamplingCurve(foiID, positions);
        } else {
            sp = OMUtils.buildSamplingPoint(foiID, positions.get(0).getOrdinate(0),  positions.get(0).getOrdinate(1));
        }
        for (org.opengis.observation.sampling.SamplingFeature existingFeature : existingFeatures) {
            if (existingFeature instanceof SamplingFeature &&
               ((SamplingFeature)existingFeature).getId().equals(sp.getId())
            ) {
                return (SamplingFeature) existingFeature;
            }
        }
        return sp;
    }

    public static AbstractDataRecord getDataRecordProfile(final String version, final List<Field> phenomenons) {
        final List<AnyScalar> fields = new ArrayList<>();
        for (Field phenomenon : phenomenons) {
            final UomProperty uom = SOSXmlFactory.buildUomProperty(version, phenomenon.uom, null);
            final Quantity cat = SOSXmlFactory.buildQuantity(version, phenomenon.name, uom, null);
            fields.add(SOSXmlFactory.buildAnyScalar(version, null, phenomenon.name, cat));
        }
        return SOSXmlFactory.buildSimpleDatarecord(version, null, null, null, true, fields);
    }

    public static AbstractDataRecord getDataRecordTrajectory(final String version, final List<Field> phenomenons) {
        final List<AnyScalar> fields = new ArrayList<>();
        fields.add(TIME_FIELD.get(version));
        for (Field phenomenon : phenomenons) {
            final UomProperty uom = SOSXmlFactory.buildUomProperty(version, phenomenon.uom, null);
            final Quantity cat = SOSXmlFactory.buildQuantity(version, phenomenon.name, uom, null);
            fields.add(SOSXmlFactory.buildAnyScalar(version, null, phenomenon.name, cat));
        }
        return SOSXmlFactory.buildSimpleDatarecord(version, null, null, null, true, fields);
    }
}
