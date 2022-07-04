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
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import static org.apache.sis.feature.AbstractIdentifiedType.NAME_KEY;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.internal.storage.io.IOUtilities;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import org.geotoolkit.storage.ProviderOnFileSystem;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.util.StringUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.Attribute;
import org.opengis.feature.AttributeType;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureOperationException;
import org.opengis.feature.FeatureType;
import org.opengis.feature.IdentifiedType;
import org.opengis.feature.Property;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class FileParsingObservationStoreFactory extends AbstractObservationStoreFactory implements ProviderOnFileSystem {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    protected static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName("main_column")
            .setRequired(true)
            .create(String.class, "");

    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName("date_column")
            .setRequired(true)
            .create(String.class, "");

    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName("date_format")
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName("longitude_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName("latitude_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName("feature_of_interest_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_TYPE = PARAM_BUILDER
            .addName("observation_type")
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, null);

    public static final ParameterDescriptor<String> PROCEDURE_ID = PARAM_BUILDER
            .addName("procedure_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_COLUMN = PARAM_BUILDER
            .addName("procedure_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_NAME_COLUMN = PARAM_BUILDER
            .addName("procedure_name_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_DESC_COLUMN = PARAM_BUILDER
            .addName("procedure_desc_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> Z_COLUMN = PARAM_BUILDER
            .addName("z_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> UOM_REGEX = PARAM_BUILDER
            .addName("uom_regex")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> RESULT_COLUMN = PARAM_BUILDER
            .addName("result_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_COLUMN = PARAM_BUILDER
            .addName("observed_properties_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_ID = PARAM_BUILDER
            .addName("observed_properties_id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_NAME_COLUMN = PARAM_BUILDER
            .addName("observed_properties_name_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_NAME = PARAM_BUILDER
            .addName("observed_properties_name")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_FILTER_COLUMN = PARAM_BUILDER
            .addName("observed_properties_filter_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_REGEX = PARAM_BUILDER
            .addName("observed_properties_regex")
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptor<String> TYPE_COLUMN = PARAM_BUILDER
            .addName("type_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> UOM_COLUMN = PARAM_BUILDER
            .addName("uom_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> CHARQUOTE = PARAM_BUILDER
            .addName("char_quote")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> FILE_MIME_TYPE = PARAM_BUILDER
            .addName("file_mime_type")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<Boolean> DIRECT_COLUMN_INDEX = PARAM_BUILDER
            .addName("direct_column_index")
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptor<Boolean> NO_HEADER = PARAM_BUILDER
            .addName("no_header")
            .setRequired(false)
            .create(Boolean.class, false);

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        GeneralParameterDescriptor desc;
        try {
            desc = getOpenParameters().descriptor(LOCATION);
        } catch (ParameterNotFoundException e) {
            throw new DataStoreException("Unsupported input");
        }

        if (!(desc instanceof ParameterDescriptor)) {
            throw new DataStoreException("Unsupported input");
        }

        try {
            final Object locationValue = sc.getStorageAs(((ParameterDescriptor)desc).getValueClass());
            final ParameterValueGroup params = getOpenParameters().createValue();
            params.parameter(LOCATION).setValue(locationValue);

            if (canProcess(params)) {
                return open(params);
            }
        } catch(IllegalArgumentException ex) {
            throw new DataStoreException("Unsupported input:" + ex.getMessage());
        }

        throw new DataStoreException("Unsupported input");
    }

    private static ParameterDescriptorGroup parameters(final String name, final int minimumOccurs) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ParameterDescriptorGroup.NAME_KEY, name);
        properties.put(Identifier.AUTHORITY_KEY, Citations.SIS);
        return new DefaultParameterDescriptorGroup(properties, minimumOccurs, 1);
    }

    protected static final AttributeType<Point> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, NamesExt.create("Point")), Point.class, 1, 1, null);

    protected static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("CalculatePoint", 1);

    protected static final GeometryFactory GF = new GeometryFactory();

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
    protected FeatureType readType(final URI file, final String mimeType, final char separator, final char charquote, final List<String> dateColumn,
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

    protected static Set<String> getMultipleValues(final ParameterValueGroup params, final String descCode) {
        final ParameterValue<String> paramValues = (ParameterValue<String>) params.parameter(descCode);
        return paramValues.getValue() == null ?
                new HashSet<>() : new HashSet<>(StringUtilities.toStringList(paramValues.getValue()));
    }

    protected static List<String> getMultipleValuesList(final ParameterValueGroup params, final String descCode) {
        final ParameterValue<String> paramValues = (ParameterValue<String>) params.parameter(descCode);
        return paramValues.getValue() == null ?
                new ArrayList<>() : StringUtilities.toStringList(paramValues.getValue());
    }

    @Override
    public Collection<byte[]> getSignature() {
        return Collections.emptyList();
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        final Collection<String> suffix = getSuffix();
        if (!suffix.isEmpty() && path != null) {
            final String extension = IOUtilities.extension(path).toLowerCase();
            final boolean extValid = suffix.contains(extension);
            if (extValid) {
                switch (extension) {
                    case "csv" :  return new ProbeResult(true, "text/csv; subtype=\"om\"", null);
                    case "xlsx":  return new ProbeResult(true, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; subtype=\"om\"", null);
                    case "xls" :  return new ProbeResult(true, "application/vnd.ms-excel; subtype=\"om\"", null);
                    case "dbf" :  return new ProbeResult(true, "application/dbase; subtype=\"om\"", null);
                }
            }
        }
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

}
