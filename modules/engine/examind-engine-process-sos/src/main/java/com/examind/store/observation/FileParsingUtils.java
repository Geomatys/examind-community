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

import static com.examind.store.observation.FileParsingObservationStoreFactory.EMPTY_PARAMS;
import static com.examind.store.observation.FileParsingObservationStoreFactory.TYPE;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.model.SamplingFeature;
import org.geotoolkit.util.NamesExt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureOperationException;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Property;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileParsingUtils {

    private static final Logger LOGGER = Logger.getLogger("com.examind.store.observation");

    private static final NumberFormat FR_FORMAT = NumberFormat.getInstance(Locale.FRANCE);

    private static final GeometryFactory GF = new GeometryFactory();

    public static int getColumnIndex(String columnName, DataFileReader reader, boolean directColumnIndex) throws IOException {
        if (columnName == null) return -1;
        if (directColumnIndex) {
            return Integer.parseInt(columnName);
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndex(columnName, headers, directColumnIndex);
    }

    public static int getColumnIndex(String columnName, String[] headers, boolean directColumnIndex) throws IOException {
        return getColumnIndex(columnName, headers, null, directColumnIndex);
    }

    public static int getColumnIndex(String columnName, String[] headers, List<Integer> appendIndex, boolean directColumnIndex) throws IOException {
        if (columnName == null) return -1;
        if (directColumnIndex) {
            return Integer.parseInt(columnName);
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = headers[i];
            if (header.equals(columnName)) {
                if (appendIndex != null) {
                    appendIndex.add(i);
                }
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, DataFileReader reader, boolean directColumnIndex) throws IOException {
        if (directColumnIndex) {
            List<Integer> results = new ArrayList<>();
            for (String columnName : columnNames) {
                results.add(Integer.valueOf(columnName));
            }
            return results;
        }
        final String[] headers = reader.getHeaders();
        return getColumnIndexes(columnNames, headers, directColumnIndex);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, boolean directColumnIndex) throws IOException {
        return getColumnIndexes(columnNames, headers, null, directColumnIndex);
    }

    public static List<Integer> getColumnIndexes(Collection<String> columnNames, String[] headers, Collection<String> appendName, boolean directColumnIndex) throws IOException {
        List<Integer> results = new ArrayList<>();
        if (directColumnIndex) {
            for (String columnName : columnNames) {
                int index = Integer.parseInt(columnName);
                results.add(index);
                if (headers != null) {
                    appendName.add(headers[index]);
                }
            }
            return results;
        }
        for (int i = 0; i < headers.length; i++) {
            final String header = headers[i];
            if (columnNames.contains(header)) {
                results.add(i);
            }
            if (appendName != null) {
                appendName.add(header);
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
                result += line[columnIndex];
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
        boolean empty = true;
        for (int i : doubleFields) {
            try {
                Object value = line[i];
                if (value instanceof String strValue) {
                    if (strValue == null || (strValue = strValue.trim()).isEmpty()) continue;
                    parseDouble(strValue);
                    empty = false;
                    break;
                } else if (value instanceof Number) {
                    empty = false;
                    break;
                }
            } catch (NumberFormatException | ParseException ex) {
                if (!((String)line[i]).isEmpty()) {
                    LOGGER.fine(String.format("Problem parsing double value at line %d and column %d (value='%s')", lineNumber, i, line[i]));
                }
            }
        }
        return empty;
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
     * Build feature type from csv file headers.
     *
     * @param file csv file URI
     * @param separator csv file separator
     * @param charquote csv file quote character
     * @param dateColumn the header of the expected date column
     * @param longitudeColumn Name of the longitude column.
     * @param latitudeColumn Name of the latitude column.
     * @param measureColumns Names of the measure columns.
     * @return
     * @throws DataStoreException
     * @throws java.io.IOException
     */
    public static FeatureType buildFeatureType(final URI file, final String mimeType, final char separator, final char charquote, final List<String> dateColumn,
            final String longitudeColumn, final String latitudeColumn, final Set<String> measureColumns) throws DataStoreException, IOException {

        // no possibility to open the file with the correct reader.
        if (mimeType == null) {
            return null;
        }
        /*
        1- read file headers
        ======================*/
        try (final DataFileReader reader = FileParsingUtils.getDataFileReader(mimeType, Paths.get(file), separator, charquote)) {

            final String[] fields = reader.getHeaders();
            final FeatureTypeBuilder ftb = new FeatureTypeBuilder();

            /*
            2- build feature type name and id fields
            ======================================*/
            final String path = file.toString();
            final int slash = Math.max(0, path.lastIndexOf('/') + 1);
            int dot = path.indexOf('.', slash);
            if (dot < 0) {
                dot = path.length();
            }

            ftb.setName(path.substring(slash, dot));
            ftb.addAttribute(Integer.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);

            /*
            3- map fields to feature type attributes
            ======================================*/
            for (String field : fields) {
                if (field.isEmpty()) continue;
                if (charquote != 0) {
                    if (field.charAt(0) == charquote) {
                        field = field.substring(1);
                    }
                    if (field.charAt(field.length() -1) == charquote) {
                        field = field.substring(0, field.length() -1);
                    }
                }
                final AttributeTypeBuilder atb = ftb.addAttribute(Object.class);
                atb.setName(NamesExt.create(field));

                if (dateColumn.contains(field)     // i'm not sure about the list of date columns TODO
               || (!measureColumns.contains(field)
                && !field.equals(longitudeColumn)
                && !field.equals(latitudeColumn))) {
                    atb.setValueClass(String.class);
                } else {
                    atb.setValueClass(Double.class);
                }
            }

            /*
            4- build a geometry operation property from longitude/latitude fields
            ===================================================================*/
            if (latitudeColumn != null && longitudeColumn != null) {
                ftb.addProperty(new AbstractOperation(Collections.singletonMap(DefaultAttributeType.NAME_KEY, AttributeConvention.GEOMETRY_PROPERTY)) {

                    @Override
                    public ParameterDescriptorGroup getParameters() {
                        return EMPTY_PARAMS;
                    }

                    @Override
                    public IdentifiedType getResult() {
                        return TYPE;
                    }

                    @Override
                    public Property apply(final Feature ftr, final ParameterValueGroup pvg) throws FeatureOperationException {

                        final Attribute<Point> att = TYPE.newInstance();
                        Point pt = GF.createPoint(
                                new Coordinate((Double) ftr.getPropertyValue(longitudeColumn),
                                        (Double) ftr.getPropertyValue(latitudeColumn)));
                        JTS.setCRS(pt, CommonCRS.defaultGeographic());
                        att.setValue(pt);
                        return att;
                    }
                });
            }
            return ftb.build();
        }
    }
}
