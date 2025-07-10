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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Hex;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.OMUtils;
import org.geotoolkit.observation.model.Field;
import org.geotoolkit.observation.model.FieldDataType;
import static org.geotoolkit.observation.model.FieldDataType.BOOLEAN;
import static org.geotoolkit.observation.model.FieldDataType.JSON;
import static org.geotoolkit.observation.model.FieldDataType.QUANTITY;
import static org.geotoolkit.observation.model.FieldDataType.TEXT;
import static org.geotoolkit.observation.model.FieldDataType.TIME;
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
    
    private static final NumberFormat NF = new DecimalFormat("#");

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
            final String header = removeBom(headers[i]);
            if (header.equals(columnName) || (ignoreCase && header.equalsIgnoreCase(columnName))) {
                if (appendIndex != null) {
                    appendIndex.add(i);
                }
                return computeMaxValue(i, maxIndex);
            }
        }
        return -1;
    }
    
    /**
     * Check if there is a BOM - Byte Order Mark - at the beginning of a String and removes it if it is present
     * @param string the String to remove the BOM from
     * @return the String without the BOM if there was one
     */
    public static String removeBom(String string) {
        if (!string.isEmpty()) {
            //Get the first character
            String bom = string.substring(0, 1);
            //Convert first character to byte to character(Use Apache Commons Codec Hex class)
            String bomByte = new String(Hex.encodeHex(bom.getBytes()));
            if ("efbbbf".equals(bomByte)) {
                //Eliminate BOM
                string = string.substring(1);
            }
        }
        return string;
    }

    private static int computeMaxValue(int i, AtomicInteger max) {
        if (max != null && max.get() < i) {
            max.set(i);
        }
        return i;
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, DataFileReader reader, boolean directColumnIndex, boolean ignoreCase, String qualifier) throws IOException, DataStoreException {
        if (directColumnIndex) {
            List<Integer> results = new ArrayList<>();
            for (String columnName : columnNames) {
                results.add(Integer.valueOf(columnName));
            }
            return results;
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndexes(columnNames, headers, directColumnIndex, ignoreCase, qualifier);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex, boolean ignoreCase, String qualifier) throws DataStoreException {
        return getColumnIndexes(columnNames, headers, null, directColumnIndex, ignoreCase, null, qualifier);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex, String qualifier) throws DataStoreException {
        return getColumnIndexes(columnNames, headers, null, directColumnIndex, ignoreCase, maxIndex, qualifier);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex, boolean ignoreCase, String qualifier) throws DataStoreException {
        return getColumnIndexes(columnNames, headers, appendName, directColumnIndex, ignoreCase, null, qualifier);
    }
    
    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex, String qualifier) throws DataStoreException {
        return getColumnIndexes(columnNames, headers, appendName, directColumnIndex, ignoreCase, maxIndex, null, qualifier);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex, List<String> fixedValues, String qualifier) throws DataStoreException {
        List<Integer> results = new ArrayList<>();
        int cpt = 0;
        if (directColumnIndex) {
            for (String columnName : columnNames) {
                int index = Integer.parseInt(columnName);
                results.add(computeMaxValue(index, maxIndex));
                if (appendName != null) {
                    if (fixedValues != null) {
                        appendName.add(fixedValues.get(cpt));
                    } else if (headers != null) {
                        appendName.add(removeBom(headers[index]));
                    }
                }
                cpt++;
            }
            return results;
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = removeBom(headers[i]);
            if (columnNames.contains(header)) {
                results.add(computeMaxValue(i, maxIndex));
                if (appendName != null) {
                    if (fixedValues != null && !fixedValues.isEmpty()) {
                        appendName.add(fixedValues.get(cpt));
                    } else {
                        appendName.add(header);
                    }
                }
                cpt++;
            }
        }
        if (columnNames.size() != results.size()) {
            throw new DataStoreException("Unable to find " + qualifier + " column(s): " + columnNames.toString());
        }
        return results;
    }
    
    public static Map<Integer, String> getNamedColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex, boolean ignoreCase) {
        return getNamedColumnIndexes(columnNames, headers, null, directColumnIndex, ignoreCase, null, null);
    }
    
    public static Map<Integer, String> getNamedColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex) {
        return getNamedColumnIndexes(columnNames, headers, null, directColumnIndex, ignoreCase, maxIndex, null);
    }
    
    public static Map<Integer, String> getNamedColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex, boolean ignoreCase, AtomicInteger maxIndex, List<String> fixedValues) {
        Map<Integer, String> results = new HashMap<>();
        int cpt = 0;
        if (directColumnIndex) {
            // kind of useless in this case
            for (String columnName : columnNames) {
                int index = Integer.parseInt(columnName);
                results.put(index , Integer.toString(computeMaxValue(index, maxIndex)));
                if (appendName != null) {
                    if (fixedValues != null  && !fixedValues.isEmpty()) {
                        appendName.add(fixedValues.get(cpt));
                    } else if (headers != null) {
                        appendName.add(removeBom(headers[index]));
                    }
                }
                cpt++;
            }
            return results;
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = removeBom(headers[i]);
            if (columnNames.contains(header)) {
                results.put(computeMaxValue(i, maxIndex), header);
                if (appendName != null) {
                    if (fixedValues != null && !fixedValues.isEmpty()) {
                        appendName.add(fixedValues.get(cpt));
                    } else {
                        appendName.add(header);
                    }
                }
                cpt++;
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
    
    public static Map<String, Object> getColumnMapValue(int index, Object[] line) {
        Map<String, Object> result = new HashMap<>();
        if (index != -1) {
            String str = asString(line[index]);
            if (str == null || str.isEmpty()) return result;
            String[] kvs = str.split("\\|", 0);
            for (String kv : kvs) {
                int pos = kv.indexOf(':');
                if (pos == -1) {
                    LOGGER.warning("malformed map value:" + kv);
                    continue;
                }
                String key = kv.substring(0, pos);
                String value = kv.substring(pos + 1, kv.length());
                if (value.startsWith("[") && value.endsWith("]")) {
                    value = value.substring(1, value.length() -1);
                    List<String> values = List.of(value.split(",", 0));
                    result.put(key, values);
                } else {
                    result.put(key, value);
                }
            }
        }
        return result;
    }
    
    /**
     * Extract a value from a CSV line.
     * If a fixed value is specified, it will be returned.
     * Multiple column index can be specified, the result will be a concatenation separated by '-'.
     * example: value1-value2
     * 
     * @param line A csvline.
     * @param fixedValue If not {@code null} will always be returned.
     * @param columnsIndexes One or more column indexes (can be {@code nul} or empty if fixedValue is specified).
     * @return A value.
     */
    public static String getMultiOrFixedValue(Object[] line, String fixedValue, List<Integer> columnsIndexes) {
        return getMultiOrFixedValue(line, fixedValue, columnsIndexes, null);
    }

    /**
    * Extract a value from a CSV line.
     * If a fixed value is specified, it will be returned.
     * Multiple column index can be specified, the result will be a concatenation separated by '-'.
     * example: value1-value2
     * 
     * @param line A csvline.
     * @param fixedValue If not {@code null} will always be returned.
     * @param columnsIndexes One or more column indexes (can be {@code nul} or empty if fixedValue is specified).
     * @param regex if specified, a regex will be applied oon the extract value from column(s).
     * @return A value.
     */
    public static String getMultiOrFixedValue(Object[] line, String fixedValue, List<Integer> columnsIndexes, String regex) {
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
                result += extractWithRegex(regex, asString(line[columnIndex]));
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
        if (doubleFields.isEmpty()) return false;
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
    public static boolean verifyEmptyLine(Object[] line, int lineNumber, List<MeasureField> typedFields, DateFormat sdf) {
        for (MeasureField field : typedFields) {
            int i = field.columnIndex;
            FieldDataType ft = field.dataType;
            try {
                Object value = line[i];
                if (value == null) continue;
                switch(ft) {

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
                    LOGGER.fine(String.format("Problem parsing '%s value at line %d and column %d (value='%s')", field, lineNumber, i, line[i]));
                }
            }
        }
        return true;
    }
    
    public static Object parseFieldValue(Object value, FieldDataType dataType, DateFormat sdf) throws ParseException {
        if (value == null) return null;
        return switch (dataType) {
            case BOOLEAN  -> parseBoolean(value);
            case QUANTITY -> parseDouble(value);
            case TEXT     -> value instanceof String ? value : value.toString();
            case TIME     -> parseObjectDate(value, sdf);
            case JSON     -> parseMap(value);
        };
    }

    /**
     * Return {@code true} if the input line is empty or didn't match the expected minimal length maxIndex
     *
     * @param line CSV line.
     * @param lineNumber Line number (for logging purpose).
     * @param headers CSV headers.
     * @param maxIndex Maximum index where we expected to have data.
     *
     * @return {@code true} if the input line is empty or didn't match the expected minimal length maxIndex
     */
    public static boolean verifyLineCompletion(Object[] line, int lineNumber, String[] headers, AtomicInteger maxIndex) {
        if (line.length == 0) {
            LOGGER.finer("skipping empty line " + lineNumber);
            return true;
        } else if (headers != null && line.length < (maxIndex.get() + 1)) {
            LOGGER.finer("skipping imcomplete line " + lineNumber + " (" +line.length + "/" + headers.length + ")");
            return true;
        }
        return false;
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
    
    // TODO, does DBF need lock?
    private static List<String> LOCKED_MIME_TYPE = List.of("xlsx-flat", "xlsx", "xls-flat", "xls", "application/vnd.ms-excel; subtype=\"om\"", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; subtype=\"om\"");
    
    public static boolean needLock(String mimeType) throws IOException {
        // if we have no informations, we lock by security
        if (mimeType == null) return true;
        return LOCKED_MIME_TYPE.contains(mimeType);
    }

    public static Map parseMap(Object value) throws ParseException {
        if (value instanceof String strValue) {
            return OMUtils.readJsonMap(strValue);
        } else if (value instanceof Map mValue) {
            return mValue;
        } else {
            throw new ParseException("Unable to parse a Map value", 0);
        }
    }
    
    public static long parseObjectDate(Object dateObj, DateFormat sdf) throws ParseException {
        if (dateObj instanceof Double db) {
            // with some date format, xls/x parser return a double. like for an input like: '20240101'
            // more, it will a representation like 2.0240101E-7
            if (sdf != null) {
                String str = NF.format(db);
                synchronized(sdf) {
                    return sdf.parse(str).getTime();
                }
                
            // DBF case.
            } else {
                return dateFromDouble(db).getTime();
            }
        } else if (dateObj instanceof String str) {
            synchronized(sdf) {
                return sdf.parse(str).getTime();
            }
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
    
    /**
     * Return the string representation of a cell content.
     * This method will throw an @{@link IllegalArgumentException} if you supplied a Date object.
     * In this case you must use the asString method with a DateFormat in parameters.
     * 
     * @param value The cell content.
     * 
     * @return A string representation of a cell content.
     */
    public static String asString(Object value) {
        return asString(value, null, false);
    }
    
    /**
     * Return the string representation of a cell content.
     * This method will throw an @{@link IllegalArgumentException} if you supplied a Date object.
     * In this case you must use the asString method with a DateFormat in parameters.
     * 
     * @param value The cell content.
     * @param emptyAsNull If set to true, an empty string value will return null instead of the empty string.
     * 
     * @return A string representation of a cell content.
     */
    public static String asString(Object value, boolean emptyAsNull) {
        return asString(value, null, emptyAsNull);
    }
    
    /**
     * Return the string representation of a cell content.
     * This method will allow Date objects.
     * 
     * @param value The cell content.
     * @param df A dateFormat use to format Date values.
     * 
     * @return A string representation of a cell content.
     */
    public static String asString(Object value, DateFormat df) {
        return asString(value, df, false);
    }

    /**
     * Return the string representation of a cell content.
     * This method will allow Date objects.
     * 
     * @param value The cell content.
     * @param df A dateFormat use to format Date values.
     * @param emptyAsNull If set to true, an empty string value will return null instead of the empty string.
     * 
     * @return A string representation of a cell content. 
     */
    public static String asString(Object value, DateFormat df, boolean emptyAsNull) {
        if (value == null) return null;
        if (value instanceof String s) {
            s = s.trim();
            if (s.isEmpty() && emptyAsNull) return null;
            return s;
        } else if (value instanceof Number n) {
            synchronized (NF) {
                return NF.format(n);
            }
           // return value.toString().replaceFirst("\\.0*$", "");
        } else if (value instanceof Date d) {
            if (df == null) throw new IllegalArgumentException("asString for a date must provide a DateFormat");
            synchronized (df) {
                return df.format(d);
            }
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
    
    public static List<MeasureField> buildExtraMeasureFields(List<String> nameColumns, List<String> columnIds, List<String> columnTypes) {
        List<MeasureField> results = new ArrayList<>();
        for (int i = 0; i < nameColumns.size(); i++) {
            String qName = nameColumns.get(i);
            if (i < columnIds.size()) {
                qName = columnIds.get(i);
            }
            qName = normalizeFieldName(qName);
            FieldDataType qtype = FieldDataType.TEXT;
            if (i < columnTypes.size()) {
                qtype = FieldDataType.valueOf(columnTypes.get(i));
            }
            results.add(new MeasureField(- 1, qName, qtype, List.of(), List.of()));
        }
        return results;
    }
    
    public static List<Field> buildExtraFields(List<String> nameColumns, List<String> idColumns, List<String> typeColumns, FieldType type) {
        List<Field> results = new ArrayList<>();
        for (int i = 0; i < nameColumns.size(); i++) {
            String qName = nameColumns.get(i);
            if (i < idColumns.size()) {
                qName = idColumns.get(i);
            }
            qName = normalizeFieldName(qName);
            FieldDataType qtype = FieldDataType.TEXT;
            if (i < typeColumns.size()) {
                qtype = FieldDataType.valueOf(typeColumns.get(i));
            }
            results.add(new Field(- 1, qtype, qName, qName, null, null, type));
        }
        return results;
    }
}
