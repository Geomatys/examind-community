/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2019, Geomatys
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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import static org.apache.sis.feature.AbstractIdentifiedType.NAME_KEY;
import org.apache.sis.feature.AbstractOperation;
import org.apache.sis.feature.DefaultAttributeType;
import org.apache.sis.feature.builder.AttributeTypeBuilder;
import org.apache.sis.feature.builder.FeatureTypeBuilder;
import org.apache.sis.internal.feature.AttributeConvention;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.parameter.DefaultParameterDescriptorGroup;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
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
public abstract class FileParsingObservationStoreFactory extends AbstractObservationStoreFactory {

    protected static final Logger LOGGER = Logger.getLogger("com.examind.process.sos");

    protected static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName("main_column")
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName("date_column")
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName("date_format")
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName("longitude_column")
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName("latitude_column")
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

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

    public static final ParameterDescriptor<Boolean> EXTRACT_UOM = PARAM_BUILDER
            .addName("extract_uom")
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptor<String> RESULT_COLUMN = PARAM_BUILDER
            .addName("result_column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_COLUMN = PARAM_BUILDER
            .addName("observed_properties_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_NAME_COLUMN = PARAM_BUILDER
            .addName("observed_properties_name_columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_PROP_FILTER_COLUMN = PARAM_BUILDER
            .addName("observed_properties_filter_columns")
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

    private static final AttributeType<Point> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, NamesExt.create("Point")), Point.class, 1, 1, null);

    private static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("CalculatePoint", 1);

    private static final GeometryFactory GF = new GeometryFactory();

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
    protected FeatureType readType(final URI file, final char separator, final char charquote, final String dateColumn,
            final String longitudeColumn, final String latitudeColumn, final Set<String> measureColumns) throws DataStoreException, IOException {

        /*
        1- read csv file headers
        ======================*/
        final String line;
        try (final Scanner scanner = new Scanner(Paths.get(file))) {
            if (scanner.hasNextLine()) {
                line = scanner.nextLine();
            } else {
                return null;
            }
        }

        final String[] fields = line.split("" + separator, -1);

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

            if (dateColumn.equals(field)
           || (!measureColumns.contains(field)
            && !longitudeColumn.equals(field)
            && !latitudeColumn.equals(field))) {
                atb.setValueClass(String.class);
            } else {
                atb.setValueClass(Double.class);
            }
        }

        /*
        4- build a geometry operation property from longitude/latitude fields
        ===================================================================*/
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

        return ftb.build();
    }

    protected static Set<String> getMultipleValues(final ParameterValueGroup params, final String descCode) {
        final ParameterValue<String> paramValues = (ParameterValue<String>) params.parameter(descCode);
        return paramValues.getValue() == null ?
                Collections.emptySet() : new HashSet<>(StringUtilities.toStringList(paramValues.getValue()));
    }
}
