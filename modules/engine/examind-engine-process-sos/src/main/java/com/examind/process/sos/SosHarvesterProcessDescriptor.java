/*
 *    Examind - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2011 Geomatys.
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
package com.examind.process.sos;

import com.examind.store.observation.FileParsingObservationStoreFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author Samuel Andrés (Geomatys)
 */
public class SosHarvesterProcessDescriptor extends AbstractProcessDescriptor{

    /**
     * Limit number of properties to a few dozens, to limit system overhead.
     * TODO: better describe this choice, or relax this limitation.
     */
    private static final int INPUT_MAX_CARDINALITY = 92;
    private static final int OUTPUT_MAX_CARDINALITY = Integer.MAX_VALUE;

    /**Process name : addition */
    public static final String NAME = "sosHarvester";

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    /**
     * Input parameters
     */
    public static final String DATA_FOLDER_NAME = "data_folder";
    public static final String DATA_FOLDER_DESC = "Folder containing the files to harvest.";
    public static final ParameterDescriptor<String> DATA_FOLDER = PARAM_BUILDER
            .addName(DATA_FOLDER_NAME)
            .setRemarks(DATA_FOLDER_DESC)
            .setRequired(true)
            .create(String.class, null);

    public static final String USER_NAME = "user_name";
    public static final String USER_DESC = "FTP user name.";
    public static final ParameterDescriptor<String> USER = PARAM_BUILDER
            .addName(USER_NAME)
            .setRemarks(USER_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String PWD_NAME = "user_password";
    public static final String PWD_DESC = "FTP user password";
    public static final ParameterDescriptor<String> PWD = PARAM_BUILDER
            .addName(PWD_NAME)
            .setRemarks(PWD_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String REMOTE_READ_NAME = "remote_reading";
    public static final String REMOTE_READ_DESC = "Do not copy files into examind dedicated folder.";
    public static final ParameterDescriptor<Boolean> REMOTE_READ = PARAM_BUILDER
            .addName(REMOTE_READ_NAME)
            .setRemarks(REMOTE_READ_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String DIRECT_COLUMN_INDEX_NAME = "direct_column_index";
    public static final String DIRECT_COLUMN_INDEX_DESC = "Use directly the column index instead of column name extracted fom headers.";
    public static final ParameterDescriptor<Boolean> DIRECT_COLUMN_INDEX = PARAM_BUILDER
            .addName(DIRECT_COLUMN_INDEX_NAME)
            .setRemarks(DIRECT_COLUMN_INDEX_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String NO_HEADER_NAME = "no_header";
    public static final String NO_HEADER_DESC = "the file has no header. Must be used with direct_column_index";
    public static final ParameterDescriptor<Boolean> NO_HEADER = PARAM_BUILDER
            .addName(NO_HEADER_NAME)
            .setRemarks(NO_HEADER_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String LAX_HEADER_NAME = "lax_header";
    public static final String LAX_HEADER_DESC = "Column research ignore case";
    public static final ParameterDescriptor<Boolean> LAX_HEADER = PARAM_BUILDER
            .addName(LAX_HEADER_NAME)
            .setRemarks(NO_HEADER_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String SERVICE_ID_NAME = "sensor_service";
    public static final String SERVICE_ID_DESC = "Sensor service where to publish the sensors.";
    public static final ParameterDescriptor<ServiceProcessReference> SERVICE_ID =
    new ExtendedParameterDescriptor<>(
                SERVICE_ID_NAME, SERVICE_ID_DESC, 1, INPUT_MAX_CARDINALITY, ServiceProcessReference.class, null, null, Collections.singletonMap("filter", Collections.singletonMap("type", Arrays.asList("sos", "sts"))));

    public static final String DATASET_IDENTIFIER_NAME = "dataset_identifier";
    public static final String DATASET_IDENTIFIER_DESC = "Dataset identifier where to add the data.";
    public static final ParameterDescriptor<String> DATASET_IDENTIFIER = PARAM_BUILDER
            .addName(DATASET_IDENTIFIER_NAME)
            .setRemarks(DATASET_IDENTIFIER_DESC)
            .setRequired(true)
            .create(String.class, null);

    public static final String THING_ID_NAME = "thing_id";
    public static final String THING_ID_DESC = "Assigned thing identifier or template if combinated with thing_column";
    public static final ParameterDescriptor<String> THING_ID = PARAM_BUILDER
            .addName(THING_ID_NAME)
            .setRemarks(THING_ID_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String THING_NAME_NAME = "thing_name";
    public static final String THING_NAME_DESC = "Assigned thing name";
    public static final ParameterDescriptor<String> THING_NAME  = PARAM_BUILDER
            .addName(THING_NAME_NAME)
            .setRemarks(THING_NAME_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String THING_DESC_NAME = "thing_desc";
    public static final String THING_DESC_DESC = "Assigned thing description";
    public static final ParameterDescriptor<String> THING_DESC  = PARAM_BUILDER
            .addName(THING_DESC_NAME)
            .setRemarks(THING_DESC_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String THING_COLUMN_NAME = "thing_column";
    public static final String THING_COLUMN_DESC = "Extracted thing column";
    public static final ParameterDescriptor<String> THING_COLUMN  = PARAM_BUILDER
            .addName(THING_COLUMN_NAME)
            .setRemarks(THING_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String THING_NAME_COLUMN_NAME = "thing_name_column";
    public static final String THING_NAME_COLUMN_DESC = "Extracted thing name column";
    public static final ParameterDescriptor<String> THING_NAME_COLUMN  = PARAM_BUILDER
            .addName(THING_NAME_COLUMN_NAME)
            .setRemarks(THING_NAME_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String THING_DESC_COLUMN_NAME = "thing_desc_column";
    public static final String THING_DESC_COLUMN_DESC = "Extracted thing description column";
    public static final ParameterDescriptor<String> THING_DESC_COLUMN  = PARAM_BUILDER
            .addName(THING_DESC_COLUMN_NAME)
            .setRemarks(THING_DESC_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String THING_REGEX_NAME = "thing_regex";
    public static final String THING_REGEX_DESC = "Extract thing from thing column applying a regular expression";
    public static final ParameterDescriptor<String> THING_REGEX  = PARAM_BUILDER
            .addName(THING_REGEX_NAME)
            .setRemarks(THING_REGEX_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_TYPE_NAME = "observation_type";
    public static final String OBS_TYPE_DESC = "Observation type (\"Timeserie\", \"Trajectory\" or \"Profile\")";
    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName(OBS_TYPE_NAME)
            .setRemarks(OBS_TYPE_DESC)
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, null);

    public static final String SEPARATOR_NAME = CSVProvider.SEPARATOR.getName().getCode();
    public static final String SEPARATOR_DESC = CSVProvider.SEPARATOR.getName().getCode();
    public static final ParameterDescriptor<String> SEPARATOR = PARAM_BUILDER
            .addName(SEPARATOR_NAME)
            .setRemarks(SEPARATOR_DESC)
            .setRequired(false)
            .create(String.class, ",");

    public static final String MAIN_COLUMN_NAME = FileParsingObservationStoreFactory.MAIN_COLUMN.getName().getCode();
    public static final String MAIN_COLUMN_DESC = FileParsingObservationStoreFactory.MAIN_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> MAIN_COLUMN = new ExtendedParameterDescriptor<>(
            MAIN_COLUMN_NAME,
            MAIN_COLUMN_DESC,
            1, INPUT_MAX_CARDINALITY,
            String.class,
            null, null, null
    );

    public static final String Z_COLUMN_NAME = FileParsingObservationStoreFactory.Z_COLUMN.getName().getCode();
    public static final String Z_COLUMN_DESC = FileParsingObservationStoreFactory.Z_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> Z_COLUMN = PARAM_BUILDER
            .addName(Z_COLUMN_NAME)
            .setRemarks(Z_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String DATE_COLUMN_NAME = FileParsingObservationStoreFactory.DATE_COLUMN.getName().getCode();
    public static final String DATE_COLUMN_DESC = FileParsingObservationStoreFactory.DATE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> DATE_COLUMN = new ExtendedParameterDescriptor<>(
            DATE_COLUMN_NAME,
            DATE_COLUMN_DESC,
            1, INPUT_MAX_CARDINALITY,
            String.class,
            null, null, null
    );

    public static final String DATE_FORMAT_NAME = FileParsingObservationStoreFactory.DATE_FORMAT.getName().getCode();
    public static final String DATE_FORMAT_DESC = FileParsingObservationStoreFactory.DATE_FORMAT.getName().getCode();
    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName(DATE_FORMAT_NAME)
            .setRemarks(DATE_FORMAT_DESC)
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final String LONGITUDE_COLUMN_NAME = FileParsingObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode();
    public static final String LONGITUDE_COLUMN_DESC = FileParsingObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName(LONGITUDE_COLUMN_NAME)
            .setRemarks(LONGITUDE_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String LATITUDE_COLUMN_NAME = FileParsingObservationStoreFactory.LATITUDE_COLUMN.getName().getCode();
    public static final String LATITUDE_COLUMN_DESC = FileParsingObservationStoreFactory.LATITUDE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName(LATITUDE_COLUMN_NAME)
            .setRemarks(LATITUDE_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String FOI_COLUMN_NAME = FileParsingObservationStoreFactory.FOI_COLUMN.getName().getCode();
    public static final String FOI_COLUMN_DESC = FileParsingObservationStoreFactory.FOI_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName(FOI_COLUMN_NAME)
            .setRemarks(FOI_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String UOM_COLUMN_NAME = "uom_column";
    public static final String UOM_COLUMN_DESC = "Extracted uom column";
    public static final ParameterDescriptor<String> UOM_COLUMN  = PARAM_BUILDER
            .addName(UOM_COLUMN_NAME)
            .setRemarks(UOM_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String UOM_REGEX_NAME = "uom_regex";
    public static final String UOM_REGEX_DESC = "extract uom from uom column applying a regular expression";
    public static final ParameterDescriptor<String> UOM_REGEX = PARAM_BUILDER
            .addName(UOM_REGEX_NAME)
            .setRemarks(UOM_REGEX_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String UOM_ID_NAME = "uom_id";
    public static final String UOM_ID_DESC = "assigned uom id";
    public static final ParameterDescriptor<String> UOM_ID = PARAM_BUILDER
            .addName(UOM_ID_NAME)
            .setRemarks(UOM_ID_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String REMOVE_PREVIOUS_NAME = "remove_previous_integration";
    public static final String REMOVE_PREVIOUS_DESC = "remove previous integration";
    public static final ParameterDescriptor<Boolean> REMOVE_PREVIOUS = PARAM_BUILDER
            .addName(REMOVE_PREVIOUS_NAME)
            .setRemarks(REMOVE_PREVIOUS_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String STORE_ID_NAME = "store_id";
    public static final String STORE_ID_DESC = "Store Identifier";
    public static final ParameterDescriptor<String> STORE_ID = PARAM_BUILDER
            .addName(STORE_ID_NAME)
            .setRemarks(STORE_ID_DESC)
            .setRequired(true)
            .create(String.class, "observationCsvFile");
            //.createEnumerated(String.class, new String[]{"observationCsvFile", "observationCsvFlatFile", "observationDbfFile"}, "observationCsvFile");

    public static final String FORMAT_NAME = "format";
    public static final String FORMAT_DESC = "Data format (mime type) of the file to insert";
    public static final ParameterDescriptor<String> FORMAT = PARAM_BUILDER
            .addName(FORMAT_NAME)
            .setRemarks(FORMAT_DESC)
            .setRequired(false)
            .create(String.class, null);
            /*.createEnumerated(String.class, new String[]{"text/csv; subtype=\"om\"",
                                                         "application/dbase; subtype=\"om\"",
                                                         "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; subtype=\"om\"",
                                                         "application/vnd.ms-excel; subtype=\"om\""},
                                                         "text/csv; subtype=\"om\"");*/

    public static final String RESULT_COLUMN_NAME = "result_column";
    public static final String RESULT_COLUMN_DESC = "Column containing result values (used with csv-flat)";
    public static final ParameterDescriptor<String> RESULT_COLUMN = PARAM_BUILDER
            .addName(RESULT_COLUMN_NAME)
            .setRemarks(RESULT_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_PROP_ID_NAME = "observed_properties_id";
    public static final String OBS_PROP_ID_DESC = "Fixed observed property identifier (used with csv-flat)";
    public static final ParameterDescriptor<String> OBS_PROP_ID = PARAM_BUILDER
            .addName(OBS_PROP_ID_NAME)
            .setRemarks(OBS_PROP_ID_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_PROP_COLUMN_NAME = "observed_properties_columns";
    public static final String OBS_PROP_COLUMN_DESC = "Columns containing the observed properties";
    public static final ParameterDescriptor<String> OBS_PROP_COLUMN = new ExtendedParameterDescriptor<>(
            OBS_PROP_COLUMN_NAME,
            OBS_PROP_COLUMN_DESC,
            0, INPUT_MAX_CARDINALITY,
            String.class,
            null, null, null
    );

    public static final String OBS_PROP_NAME_NAME = "observed_properties_name";
    public static final String OBS_PROP_NAME_DESC = "Fixed observed property name (used with csv-flat)";
    public static final ParameterDescriptor<String> OBS_PROP_NAME = PARAM_BUILDER
            .addName(OBS_PROP_NAME_NAME)
            .setRemarks(OBS_PROP_NAME_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_PROP_NAME_COLUMN_NAME = "observed_properties_name_columns";
    public static final String OBS_PROP_NAME_COLUMN_DESC = "Columns containing the observed property description (used with csv-flat)";
    public static final ParameterDescriptor<String> OBS_PROP_NAME_COLUMN = new ExtendedParameterDescriptor<>(
            OBS_PROP_NAME_COLUMN_NAME,
            OBS_PROP_NAME_COLUMN_DESC,
            0, INPUT_MAX_CARDINALITY,
            String.class,
            null, null, null
    );

    public static final String OBS_PROP_COLUMNS_FILTER_NAME = "observed_properties_columns_filters";
    public static final String OBS_PROP_COLUMNS_FILTER_DESC =  "A filter on observed properties to extract";
    public static final ParameterDescriptor<String> OBS_PROP_COLUMNS_FILTER  = new ExtendedParameterDescriptor<>(
                OBS_PROP_COLUMNS_FILTER_NAME,
                OBS_PROP_COLUMNS_FILTER_DESC,
                0, INPUT_MAX_CARDINALITY,
                String.class,
                null, null, null
                );

    public static final String OBS_PROP_REGEX_NAME = "observed_properties_regex";
    public static final String OBS_PROP_REGEX_DESC = "extract observed properties label from column applying a regular expression";
    public static final ParameterDescriptor<String> OBS_PROP_REGEX = PARAM_BUILDER
            .addName(OBS_PROP_REGEX_NAME)
            .setRemarks(OBS_PROP_REGEX_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_PROP_COLUMN_TYPE_NAME = "observed_properties_columns_type";
    public static final String OBS_PROP_COLUMN_TYPE_DESC = "Field type of the columns containing the observed property (used with csv)";
    public static final ParameterDescriptor<String> OBS_PROP_COLUMN_TYPE = new ExtendedParameterDescriptor<>(
            OBS_PROP_COLUMN_TYPE_NAME,
            OBS_PROP_COLUMN_TYPE_DESC,
            0, INPUT_MAX_CARDINALITY,
            String.class,
            "QUANTITY",
            new String[]{"QUANTITY", "TEXT", "BOOLEAN", "TIME"},
            null
    );

    public static final String QUALITY_COLUMN_NAME = "quality_columns";
    public static final String QUALITY_COLUMN_DESC = "Columns containing the quality (linked to an observed property)";
    public static final ParameterDescriptor<String> QUALITY_COLUMN = new ExtendedParameterDescriptor<>(
            QUALITY_COLUMN_NAME,
            QUALITY_COLUMN_DESC,
            0, INPUT_MAX_CARDINALITY,
            String.class,
            null, null, null
    );

    public static final String QUALITY_COLUMN_ID_NAME = "quality_columns_id";
    public static final String QUALITY_COLUMN_ID_DESC = "Override id/name of the quality columns";
    public static final ParameterDescriptor<String> QUALITY_COLUMN_ID = new ExtendedParameterDescriptor<>(
            QUALITY_COLUMN_ID_NAME,
            QUALITY_COLUMN_ID_DESC,
            0, INPUT_MAX_CARDINALITY,
            String.class,
            null, null, null
    );

    public static final String QUALITY_COLUMN_TYPE_NAME = "quality_columns_type";
    public static final String QUALITY_COLUMN_TYPE_DESC = "Primitive type of the quality columns (STRING)";
    public static final ParameterDescriptor<String> QUALITY_COLUMN_TYPE = new ExtendedParameterDescriptor<>(
            QUALITY_COLUMN_TYPE_NAME,
            QUALITY_COLUMN_TYPE_DESC,
            0, INPUT_MAX_CARDINALITY,
            String.class,
            "QUANTITY",
            new String[]{"QUANTITY", "TEXT", "BOOLEAN", "TIME"},
            null
    );

    public static final String TYPE_COLUMN_NAME = "type_column";
    public static final String TYPE_COLUMN_DESC = "type column";
    public static final ParameterDescriptor<String> TYPE_COLUMN = PARAM_BUILDER
            .addName(TYPE_COLUMN_NAME)
            .setRemarks(TYPE_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String CHARQUOTE_NAME = "quote_character";
    public static final String CHARQUOTE_DESC = "quote character";
    public static final ParameterDescriptor<String> CHARQUOTE = PARAM_BUILDER
            .addName(CHARQUOTE_NAME)
            .setRemarks(CHARQUOTE_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String EXTRA_STORE_PARAMETERS_NAME = "extra_store_parameter";
    public static final String EXTRA_STORE_PARAMETERS_DESC = "extra store parameter";
    public static final ParameterDescriptor<Map> EXTRA_STORE_PARAMETERS = PARAM_BUILDER
            .addName(EXTRA_STORE_PARAMETERS_NAME)
            .setRemarks(EXTRA_STORE_PARAMETERS_DESC)
            .setRequired(false)
            .create(Map.class, null);

    public static final String GENERATE_METADATA_NAME = "generate_metadata";
    public static final String GENERATE_METADATA_DESC = "Generate a metadata for each imported file.";
    public static final ParameterDescriptor<Boolean> GENERATE_METADATA = PARAM_BUILDER
            .addName(GENERATE_METADATA_NAME)
            .setRemarks(GENERATE_METADATA_DESC)
            .setRequired(false)
            .create(Boolean.class, true);

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, USER, PWD, REMOTE_READ, SERVICE_ID, DATASET_IDENTIFIER, THING_ID, THING_NAME, THING_DESC, THING_COLUMN, THING_NAME_COLUMN, THING_DESC_COLUMN, THING_REGEX, OBS_TYPE,
                    SEPARATOR, CHARQUOTE, MAIN_COLUMN, Z_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, FOI_COLUMN, UOM_COLUMN, UOM_REGEX, UOM_ID, REMOVE_PREVIOUS,
                    STORE_ID, FORMAT, RESULT_COLUMN, OBS_PROP_ID, OBS_PROP_COLUMN, OBS_PROP_COLUMN_TYPE, OBS_PROP_NAME, OBS_PROP_NAME_COLUMN, OBS_PROP_COLUMNS_FILTER, OBS_PROP_REGEX, QUALITY_COLUMN, QUALITY_COLUMN_ID, QUALITY_COLUMN_TYPE, TYPE_COLUMN, EXTRA_STORE_PARAMETERS, DIRECT_COLUMN_INDEX,
                    NO_HEADER, LAX_HEADER, GENERATE_METADATA);

    public static final String FILE_ALREADY_INSERTED_COUNT_NAME = "files_already_inserted_count";
    public static final String FILE_ALREADY_INSERTED_COUNT_DESC = "Number of files already inserted";
    public static final ParameterDescriptor<Integer> FILE_ALREADY_INSERTED_COUNT = PARAM_BUILDER
            .addName(FILE_ALREADY_INSERTED_COUNT_NAME)
            .setRemarks(FILE_ALREADY_INSERTED_COUNT_DESC)
            .setRequired(false)
            .create(Integer.class, 0);

    public static final String FILE_ALREADY_INSERTED_NAME = "files_already_inserted";
    public static final String FILE_ALREADY_INSERTED_DESC = "Name of the files already inserted";
    public static final ParameterDescriptor<String> FILE_ALREADY_INSERTED = 
            new ExtendedParameterDescriptor<>(FILE_ALREADY_INSERTED_NAME, FILE_ALREADY_INSERTED_DESC, 0, Integer.MAX_VALUE, String.class, null, null, null);

    public static final String FILE_REMOVED_COUNT_NAME = "files_removed_count";
    public static final String FILE_REMOVED_COUNT_DESC = "Number of files removed";
    public static final ParameterDescriptor<Integer> FILE_REMOVED_COUNT = PARAM_BUILDER
            .addName(FILE_REMOVED_COUNT_NAME)
            .setRemarks(FILE_REMOVED_COUNT_DESC)
            .setRequired(false)
            .create(Integer.class, 0);

    public static final String FILE_REMOVED_NAME = "files_removed";
    public static final String FILE_REMOVED_DESC = "Name of the files removed";
    public static final ParameterDescriptor<String> FILE_REMOVED =
            new ExtendedParameterDescriptor<>(FILE_REMOVED_NAME, FILE_REMOVED_DESC, 0, Integer.MAX_VALUE, String.class, null, null, null);

    public static final String FILE_INSERTED_COUNT_NAME = "files_inserted_count";
    public static final String FILE_INSERTED_COUNT_DESC = "Number of files inserted";
    public static final ParameterDescriptor<Integer> FILE_INSERTED_COUNT = PARAM_BUILDER
            .addName(FILE_INSERTED_COUNT_NAME)
            .setRemarks(FILE_INSERTED_COUNT_DESC)
            .setRequired(false)
            .create(Integer.class, 0);

    public static final String FILE_INSERTED_NAME = "files_inserted";
    public static final String FILE_INSERTED_DESC = "Name of the files inserted";
    public static final ParameterDescriptor<String> FILE_INSERTED =
            new ExtendedParameterDescriptor<>(FILE_INSERTED_NAME, FILE_INSERTED_DESC, 0, Integer.MAX_VALUE, String.class, null, null, null);

    public static final String FILE_ERROR_COUNT_NAME = "files_error_count";
    public static final String FILE_ERROR_COUNT_DESC = "Number of files in error.";
    public static final ParameterDescriptor<Integer> FILE_ERROR_COUNT = PARAM_BUILDER
            .addName(FILE_ERROR_COUNT_NAME)
            .setRemarks(FILE_ERROR_COUNT_DESC)
            .setRequired(false)
            .create(Integer.class, 0);

    public static final String FILE_ERROR_NAME = "files_error";
    public static final String FILE_ERROR_DESC = "Name of the files in error.";
    public static final ParameterDescriptor<String> FILE_ERROR =
        new ExtendedParameterDescriptor<>(FILE_ERROR_NAME, FILE_ERROR_DESC, 0, Integer.MAX_VALUE, String.class, null, null, null);


    public static final String OBSERVATION_INSERTED_NAME = "observations_inserted_count";
    public static final String OBSERVATION_INSERTED_DESC = "Number of observations inserted";
    public static final ParameterDescriptor<Integer> OBSERVATION_INSERTED = PARAM_BUILDER
            .addName(OBSERVATION_INSERTED_NAME)
            .setRemarks(OBSERVATION_INSERTED_DESC)
            .setRequired(false)
            .create(Integer.class, 0);

    public static final String GENERATE_DATA_IDS_NAME = "generated_data_ids";
    public static final String GENERATE_DATA_IDS_DESC = "Generated data identifiers";
    public static final ParameterDescriptor<Integer> GENERATE_DATA_IDS = new ExtendedParameterDescriptor<>(
            GENERATE_DATA_IDS_NAME,
            GENERATE_DATA_IDS_DESC,
            0, OUTPUT_MAX_CARDINALITY,
            Integer.class,
            null, null, null
    );

    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup(FILE_ALREADY_INSERTED_COUNT, FILE_ALREADY_INSERTED,
                                                                FILE_INSERTED_COUNT, FILE_INSERTED,
                                                                FILE_ERROR_COUNT, FILE_ERROR,
                                                                FILE_REMOVED_COUNT, FILE_REMOVED,
                                                                OBSERVATION_INSERTED, GENERATE_DATA_IDS);


    /** Instance */
    public static final ProcessDescriptor INSTANCE = new SosHarvesterProcessDescriptor();

    public SosHarvesterProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Harvests SOS data."),
                INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new SosHarvesterProcess(this, input);
    }

}
