/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
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
package com.examind.process.sos;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
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
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.data.FileFeatureStoreFactory;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import org.geotoolkit.storage.ProviderOnFileSystem;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.geotoolkit.util.NamesExt;
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
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class CsvObservationStoreFactory extends AbstractObservationStoreFactory implements ProviderOnFileSystem {

    private static final GeometryFactory GF = new GeometryFactory();

    private static final Logger LOGGER = Logging.getLogger("org.geotoolkit.data");

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    /** factory identification **/
    public static final String NAME = "observationCsvFile";

    public static final String MIME_TYPE = "text/csv; subtype=\"om\"";

    private static final AttributeType<Point> TYPE = new DefaultAttributeType<>(
            Collections.singletonMap(NAME_KEY, NamesExt.create("Point")), Point.class, 1, 1, null);

    private static final ParameterDescriptorGroup EMPTY_PARAMS = parameters("CalculatePoint", 1);

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName("main column")
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName("date column")
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName("date format")
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'hh:mm:ss'Z'");

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName("longitude column")
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName("latitude column")
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

    public static final ParameterDescriptor<String> MEASURE_COLUMNS = PARAM_BUILDER
            .addName("measure columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> MEASURE_COLUMNS_SEPARATOR = PARAM_BUILDER
            .addName("measure columns separator")
            .setRequired(false)
            .create(String.class, "\\|");

    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName("Feature  of Interest column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_TYPE = PARAM_BUILDER
            .addName("Observation type")
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, "Timeserie");


    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR
            = PARAM_BUILDER.addName(NAME).addName("ObservationCsvFileParameters").createGroup(IDENTIFIER, NAMESPACE, CSVFeatureStoreFactory.PATH, CSVFeatureStoreFactory.SEPARATOR,
                    MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, MEASURE_COLUMNS, MEASURE_COLUMNS_SEPARATOR, OBSERVATION_TYPE);


    private static ParameterDescriptorGroup parameters(final String name, final int minimumOccurs) {
        final Map<String,Object> properties = new HashMap<>(4);
        properties.put(ParameterDescriptorGroup.NAME_KEY, name);
        properties.put(Identifier.AUTHORITY_KEY, Citations.SIS);
        return new DefaultParameterDescriptorGroup(properties, minimumOccurs, 1);
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public CsvObservationStore open(final ParameterValueGroup params) throws DataStoreException {

        final String measureColumnsSeparator = (String) params.parameter(MEASURE_COLUMNS_SEPARATOR.getName().toString()).getValue();

        final URI uri = (URI) params.parameter(CSVFeatureStoreFactory.PATH.getName().toString()).getValue();
        final char separator = (Character) params.parameter(CSVFeatureStoreFactory.SEPARATOR.getName().toString()).getValue();
        final String mainColumn = (String) params.parameter(MAIN_COLUMN.getName().toString()).getValue();
        final String dateColumn = (String) params.parameter(DATE_COLUMN.getName().toString()).getValue();
        final String dateFormat = (String) params.parameter(DATE_FORMAT.getName().toString()).getValue();
        final String longitudeColumn = (String) params.parameter(LONGITUDE_COLUMN.getName().toString()).getValue();
        final String latitudeColumn = (String) params.parameter(LATITUDE_COLUMN.getName().toString()).getValue();
        final String foiColumn = (String) params.parameter(FOI_COLUMN.getName().toString()).getValue();
        final String observationType = (String) params.parameter(OBSERVATION_TYPE.getName().toString()).getValue();
        final ParameterValue<String> measureCols = (ParameterValue<String>) params.parameter(MEASURE_COLUMNS.getName().toString());
        final Set<String> measureColumns = measureCols.getValue() == null ?
                Collections.emptySet() : new HashSet<>(Arrays.asList(measureCols.getValue().split(measureColumnsSeparator)));
        try {
            return new CsvObservationStore(Paths.get(uri),
                    separator, readType(uri, separator, dateColumn, longitudeColumn, latitudeColumn, measureColumns),
                    mainColumn, dateColumn, dateFormat, longitudeColumn, latitudeColumn, measureColumns, observationType, foiColumn);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem opening csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public CsvObservationStore create(final ParameterValueGroup params) throws DataStoreException {

        final String measureColumnsSeparator = (String) params.parameter(MEASURE_COLUMNS_SEPARATOR.getName().toString()).getValue();

        final URI uri = (URI) params.parameter(CSVFeatureStoreFactory.PATH.getName().toString()).getValue();
        final char separator = (Character) params.parameter(CSVFeatureStoreFactory.SEPARATOR.getName().toString()).getValue();
        final String mainColumn = (String) params.parameter(MAIN_COLUMN.getName().toString()).getValue();
        final String dateColumn = (String) params.parameter(DATE_COLUMN.getName().toString()).getValue();
        final String dateFormat = (String) params.parameter(DATE_FORMAT.getName().toString()).getValue();
        final String longitudeColumn = (String) params.parameter(LONGITUDE_COLUMN.getName().toString()).getValue();
        final String latitudeColumn = (String) params.parameter(LATITUDE_COLUMN.getName().toString()).getValue();
        final String foiColumn = (String) params.parameter(FOI_COLUMN.getName().toString()).getValue();
        final String observationType = (String) params.parameter(OBSERVATION_TYPE.getName().toString()).getValue();
        final ParameterValue<String> measureCols = (ParameterValue<String>) params.parameter(MEASURE_COLUMNS.getName().toString());
        final Set<String> measureColumns = measureCols.getValue() == null ?
                Collections.emptySet() : new HashSet<>(Arrays.asList(measureCols.getValue().split(measureColumnsSeparator)));
        try {
            return new CsvObservationStore(Paths.get(uri),
                    separator, readType(uri, separator, dateColumn, longitudeColumn, latitudeColumn, measureColumns),
                    mainColumn, dateColumn, dateFormat, longitudeColumn, latitudeColumn, measureColumns, observationType, foiColumn);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem opening csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Collection<String> getSuffix() {
        return Arrays.asList("csv");
    }

    @Override
    public Collection<byte[]> getSignature() {
        return Collections.emptyList();
    }

    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        return FileFeatureStoreFactory.probe(this, connector, MIME_TYPE);
    }

    /**
     * Build feature type from csv file headers.
     *
     * @param file csv file URI
     * @param separator csv file separator
     * @param dateColumn the header of the expected date column
     * @return
     * @throws DataStoreException
     */
    private FeatureType readType(final URI file, final char separator, final String dateColumn,
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

        ftb.addAttribute(String.class).setName(AttributeConvention.IDENTIFIER_PROPERTY);

        /*
        3- map fields to feature type attributes
        ======================================*/
        for (final String field : fields) {

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
        ftb.addProperty(new AbstractOperation(Collections.singletonMap(DefaultAttributeType.NAME_KEY, NamesExt.create("geometry"))) {

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

}
