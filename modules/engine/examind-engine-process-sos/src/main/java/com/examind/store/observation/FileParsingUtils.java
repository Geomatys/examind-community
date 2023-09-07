/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.examind.store.observation;

import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.referencing.CommonCRS;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.model.FieldType;
import org.geotoolkit.observation.model.SamplingFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileParsingUtils {

    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");

    private static final NumberFormat FR_FORMAT = DecimalFormat.getNumberInstance(Locale.FRANCE);

    private static final GeometryFactory GF = new GeometryFactory();

    public static int getColumnIndex(String columnName, DataFileReader reader, boolean directColumnIndex, boolean ignoreCase) throws IOException {
        if (columnName == null) return -1;
        if (directColumnIndex) {
            return Integer.parseInt(columnName);
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndex(columnName, headers, directColumnIndex, ignoreCase);
    }

    public static int getColumnIndex(String columnName, String[] headers, boolean directColumnIndex, boolean ignoreCase) {
        return getColumnIndex(columnName, headers, null, directColumnIndex, ignoreCase, null);
    }
    public static int getColumnIndex(String columnName, String[] headers, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex) {
        return getColumnIndex(columnName, headers, null, directColumnIndex, ignoreCase, maxIndex);
    }

    public static int getColumnIndex(String columnName, String[] headers, List<Integer> appendIndex, boolean directColumnIndex, boolean ignoreCase) {
        return getColumnIndex(columnName, headers, appendIndex, directColumnIndex, ignoreCase, null);
    }

    public static int getColumnIndex(String columnName, String[] headers, List<Integer> appendIndex, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex) {
        if (columnName == null) return -1;
        if (directColumnIndex) {
            return computeMaxValue(Integer.parseInt(columnName), maxIndex);
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = headers[i];
            if (header.equals(columnName) || (ignoreCase && header.equalsIgnoreCase(columnName))) {
                if (appendIndex != null) {
                    appendIndex.add(i);
                }
                return computeMaxValue(i, maxIndex);
            }
        }
        return -1;
    }

    private static int computeMaxValue(int i, AtomicInteger max) {
        if (max != null && max.get() < i) {
            max.set(i);
        }
        return i;
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, DataFileReader reader, boolean directColumnIndex, boolean ignoreCase) throws IOException {
        if (directColumnIndex) {
            List<Integer> results = new ArrayList<>();
            for (String columnName : columnNames) {
                results.add(Integer.valueOf(columnName));
            }
            return results;
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndexes(columnNames, headers, directColumnIndex, ignoreCase);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex, boolean ignoreCase) {
        return getColumnIndexes(columnNames, headers, null, directColumnIndex, ignoreCase, null);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex) {
        return getColumnIndexes(columnNames, headers, null, directColumnIndex, ignoreCase, maxIndex);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex, boolean ignoreCase) {
        return getColumnIndexes(columnNames, headers, appendName, directColumnIndex, ignoreCase, null);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex) {
        List<Integer> results = new ArrayList<>();
        if (directColumnIndex) {
            for (String columnName : columnNames) {
                int index = Integer.parseInt(columnName);
                results.add(computeMaxValue(index, maxIndex));
                if (headers != null) {
                    appendName.add(headers[index]);
                }
            }
            return results;
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = headers[i];
            if (columnNames.contains(header)) {
                results.add(computeMaxValue(i, maxIndex));
                if (appendName != null) {
                    appendName.add(header);
                }
            }
            
        }
        return results;
    }

    /**
     * Return the value in the line if the supplied index is different from -1.
     * Else return the default value specified.
     * 
     * @param index column index
     * @param line parsed object line.
     * @param defaultValue value to returne if column == -1
     * 
     * @return The value of the column or the default value.
     */
    public static Object getColumnValue(int index, Object[] line, Object defaultValue) {
        Object result = defaultValue;
        if (index != -1) {
            result = line[index];
        }
        return result;
    }

     public static String getMultiOrFixedValue(Object[] line, String fixedValue, List<Integer> columnsIndexes) {
        String result = "";
        if (fixedValue != null && !fixedValue.isEmpty()) {
            // Use fixed value
            result = fixedValue;
        } else {
            // Concatenate result from input code columns
            boolean first = true;
            for (Integer columnIndex : columnsIndexes) {
                if (!first) {
                    result += "-";
                } else {
                    first = false;
                }
                result += asString(line[columnIndex]);
            }
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
    public static boolean verifyEmptyLine(Object[] line, int lineNumber, List<Integer> doubleFields) {
        for (int i : doubleFields) {
            try {
                Object value = line[i];
                if (value instanceof String strValue) {
                    if ((strValue = strValue.trim()).isEmpty()) continue;
                    parseDouble(strValue);
                    return false;
                } else if (value instanceof Number) {
                    return false;
                }
            } catch (NumberFormatException | ParseException ex) {
                if (!((String)line[i]).isEmpty()) {
                    LOGGER.fine(String.format("Problem parsing double value at line %d and column %d (value='%s')", lineNumber, i, line[i]));
                }
            }
        }
        return true;
    }

    /**
     * Verify if the line is empty, meaning no measure field is filled.
     *
     * @param line parsed CSV line.
     * @param lineNumber line number in the file (for logging purpose).
     * @param strFields List of column index where to look for String value.
     *
     * @return {@code true} if the line is considered empty.
     */
    public static boolean verifyEmptyLineStr(Object[] line, int lineNumber, List<Integer> strFields) {
        for (int i : strFields) {
            Object value = line[i];
            if (value instanceof String strValue && !strValue.isBlank()) {
                return false;
            } else if (value != null && !value.toString().isBlank()) {
               return false;
            }
        }
        return true;
    }

    /**
     * Verify if the line is empty, meaning no measure field is filled.
     *
     * @param line parsed CSV line.
     * @param lineNumber line number in the file (for logging purpose).
     * @param typedFields List of column index where to look for Double value.
     *
     * @return {@code true} if the line is considered empty.
     */
    public static boolean verifyEmptyLine(Object[] line, int lineNumber, Map<Integer, FieldType> typedFields, DateFormat sdf) {
        for (Entry<Integer, FieldType> field : typedFields.entrySet()) {
            int i = field.getKey();
            try {
                Object value = line[i];
                if (value == null) continue;
                switch(field.getValue()) {
                    case QUANTITY -> {
                         if (value instanceof String strValue) {
                            if ((strValue = strValue.trim()).isEmpty()) continue;
                            parseDouble(strValue);
                            return false;
                        } else if (value instanceof Number) {
                            return false;
                        }
                    }
                    case BOOLEAN -> {
                        if (value instanceof String strValue) {
                            if ((strValue = strValue.trim()).isEmpty()) continue;
                            return false;
                        } else if (value instanceof Boolean) {
                            return false;
                        }
                    }
                    case TEXT -> {
                        if (value instanceof String strValue) {
                            if ((strValue = strValue.trim()).isEmpty()) continue;
                            return false;
                        // a toString will be applied
                        } else {
                            return false;
                        }
                    }
                    case TIME -> {
                        parseObjectDate(value, sdf);
                        return false;
                    }
                }
               
            } catch (NumberFormatException | ParseException | ClassCastException ex) {
                if (!((String)line[i]).isEmpty()) {
                    LOGGER.fine(String.format("Problem parsing '%s value at line %d and column %d (value='%s')", field.getValue(), lineNumber, i, line[i]));
                }
            }
        }
        return true;
    }

    public static String extractWithRegex(String regex, String value) {
        return extractWithRegex(regex, value, value);
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
     * @param obj  A double object or a string value of a double.
     * @return : double.
     * @throws ParseException the parse method failed.
     */
    public static double parseDouble(Object obj) throws ParseException, NumberFormatException {
        if (obj instanceof String s) {
            s = s.trim();
            // nbsp
            s = s.replace(" ", "");
            s = s.replace(" ", "");
            if (s.contains(",")) {
                synchronized(FR_FORMAT) {
                    Number number = FR_FORMAT.parse(s);
                    return number.doubleValue();
                }
            } else {
                return Double.parseDouble(s);
            }
        } else if (obj instanceof Double dValue) {
            return dValue;
        } else {
            throw new NumberFormatException("Expecting a double but got: " + obj);
        }
    }

    public static boolean parseBoolean(Object obj) throws ParseException {
        if (obj instanceof String s) {
            return Boolean.parseBoolean(s);
        } else if (obj instanceof Boolean dValue) {
            return dValue;
        } else {
            throw new ParseException("Expecting a boolean but got: " + obj, 0);
        }
    }

    public static Geometry buildGeom(final List<Coordinate> positions) {
        Geometry geom = null;
        if (positions.size() > 1) {
            geom = GF.createLineString(positions.toArray(Coordinate[]::new));
        } else if (!positions.isEmpty()) {
            geom = GF.createPoint(positions.get(0));
        }
        if (geom != null) {
            JTS.setCRS(geom, CommonCRS.WGS84.geographic());
        }
        return geom;
    }

    public static SamplingFeature buildFOIByGeom(String foiID, final List<Coordinate> positions, final Set<SamplingFeature> existingFeatures) {
        final Geometry geom = buildGeom(positions);
        final SamplingFeature sp = new SamplingFeature(foiID, null, null, null, foiID, geom);
        for (SamplingFeature ef : existingFeatures) {
            if ((ef.getGeometry() == null && sp.getGeometry() == null) ||
                (ef.getGeometry() != null && equalsGeom(ef.getGeometry(), positions))
            ) {
                return ef;
            }
        }
        return sp;
    }

    private static boolean equalsGeom(Geometry existing, List<Coordinate> positions) {
        // the problem here is that the axis will be flipped after save,
        // so we need to flip the axis for comparison...
        Geometry spGeometry;
         if (positions.isEmpty()) {
            return false;
        } else if (positions.size() > 1) {
            List<Coordinate> flipped = new ArrayList<>();
            positions.forEach(dp -> flipped.add(new Coordinate(dp.getOrdinate(1), dp.getOrdinate(0))));
            spGeometry = GF.createLineString(flipped.toArray(Coordinate[]::new));
        } else {
            final Coordinate position = positions.get(0);
            final Coordinate flipped  = new Coordinate(position.getOrdinate(1), position.getOrdinate(0));
            spGeometry = GF.createPoint(flipped);
        }
        return existing.equals(spGeometry);
    }

    @Deprecated
    public static boolean equalsGeom(Geometry current, Geometry existing) {
        // the problem here is that the axis will be flipped after save,
        // so we need to flip the axis for comparison... TODO
        if (current instanceof Point && existing instanceof Point exPt) {
            /*Coordinate position = exPt.getCoordinate();
            final Coordinate flipped = new Coordinate(position.getOrdinate(1), position.getOrdinate(0));
            Geometry spGeometry = GF.createPoint(flipped);*/
            return current.equals(exPt);
        }
        return false;
    }

    public static SamplingFeature buildFOIById(String foiID, final List<Coordinate> positions, final Set<org.opengis.observation.sampling.SamplingFeature> existingFeatures) {
        final Geometry geom = buildGeom(positions);
        final SamplingFeature sp = new SamplingFeature(foiID, null, null, null, foiID, geom);
        for (org.opengis.observation.sampling.SamplingFeature existingFeature : existingFeatures) {
            if (existingFeature instanceof SamplingFeature &&
               ((SamplingFeature)existingFeature).getId().equals(sp.getId())
            ) {
                return (SamplingFeature) existingFeature;
            }
        }
        return sp;
    }

    public static DataFileReader getDataFileReader(String mimeType, Path dataFile, Character delimiter, Character quotechar) throws IOException {
        switch (mimeType) {
            case "xlsx-flat":
            case "xlsx":
            case "xls-flat":
            case "xls":
            case "application/vnd.ms-excel; subtype=\"om\"":
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; subtype=\"om\"": return new XLSXDataFileReader(dataFile);
            case "csv-flat":
            case "csv":
            case "tsv":
            case "text/csv; subtype=\"om\"": return new CSVDataFileReader(dataFile, delimiter, quotechar);
            case "dbf":
            case "application/dbase; subtype=\"om\"":
            default: return new DBFDataFileReader(dataFile);
        }
    }

    public static long parseObjectDate(Object dateObj, DateFormat sdf) throws ParseException {
        if (dateObj instanceof Double db) {
            return dateFromDouble(db).getTime();
        } else if (dateObj instanceof String str) {
            return sdf.parse(str).getTime();
        } else if (dateObj instanceof Date d) {
            return d.getTime();
        } else if (dateObj instanceof Long l) {
            return l;
        } else {
            throw new ClassCastException("Unhandled date type");
        }
    }

    private static final long TIME_AT_2000;
    static {
        long candidate = 0L;
        try {
            candidate = new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01").getTime();
        } catch (ParseException ex) {
            LOGGER.log(Level.SEVERE, "Errow while caculationg time at 2000-01-01", ex);
        }
        TIME_AT_2000 = candidate;
    }

    /**
     * Assume that the date is the number of second since the  first january 2000.
     *
     * @param myDouble
     * @return
     * @throws ParseException
     */
    private static Date dateFromDouble(double myDouble) throws ParseException {
        long i = (long) (myDouble*1000);
        long l = TIME_AT_2000 + i;
        return new Date(l);
    }

    public static String asString(Object value) {
        return asString(value, null);
    }

    public static String asString(Object value, DateFormat df) {
        if (value == null) return null;
        if (value instanceof String s) {
            return s;
        } else if (value instanceof Number) {
            return value.toString().replaceFirst("\\.0*$", "");
        } else if (value instanceof Date d) {
            if (df == null) throw new IllegalArgumentException("asString for a date must provide a DateFormat");
            return df.format(d);
        } else {
            return value.toString();
        }
    }

    public static Optional<Long> parseDate(Object[] line, final Long preComputeValue, List<Integer> dateIndexes, final DateFormat sdf, int lineNumber) {
        if (preComputeValue != null) return Optional.of(preComputeValue);

        if (dateIndexes.isEmpty()) {
            return Optional.empty();

        } else if (dateIndexes.size() == 1) {
            Object value = line[dateIndexes.get(0)];
            try {
                return  Optional.of(parseObjectDate(value, sdf));
            } catch (ParseException ex) {
                LOGGER.fine(String.format("Problem parsing date for date field at line %d (value='%s'). skipping line...", lineNumber, value));
                return Optional.empty();
            }

        // composite dates are only supported for string column
        } else {
            String value = "";
            for (Integer dateIndex : dateIndexes) {
                value += asString(line[dateIndex]);
            }
            try {
                return Optional.of(sdf.parse(value).getTime());
            } catch (ParseException ex) {
                LOGGER.fine(String.format("Problem parsing date for date field at line %d (value='%s'). skipping line...", lineNumber, value));
                return Optional.empty();
            }
        }
    }

    public static String normalizeFieldName(String s) {
        return s.toLowerCase().replace(" ", "_");
    }
}
