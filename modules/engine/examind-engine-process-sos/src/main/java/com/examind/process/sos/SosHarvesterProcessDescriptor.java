/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
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

import com.examind.store.observation.FileParsingObservationStoreFactory;
import java.util.Arrays;
import java.util.Collections;
import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.apache.sis.util.iso.SimpleInternationalString;
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

    public static final String SERVICE_ID_NAME = "sensor_service";
    public static final String SERVICE_ID_DESC = "Sensor service where to publish the sensors.";
    public static final ParameterDescriptor<ServiceProcessReference> SERVICE_ID =
    new ExtendedParameterDescriptor<>(
                SERVICE_ID_NAME, SERVICE_ID_DESC, 1, 92, ServiceProcessReference.class, null, null, Collections.singletonMap("filter", Collections.singletonMap("type", Arrays.asList("sos", "sts"))));

    public static final String DATASET_IDENTIFIER_NAME = "dataset_identifier";
    public static final String DATASET_IDENTIFIER_DESC = "Dataset identifier where to add the data.";
    public static final ParameterDescriptor<String> DATASET_IDENTIFIER = PARAM_BUILDER
            .addName(DATASET_IDENTIFIER_NAME)
            .setRemarks(DATASET_IDENTIFIER_DESC)
            .setRequired(true)
            .create(String.class, null);

    public static final String PROCEDURE_ID_NAME = "procedure_id";
    public static final String PROCEDURE_ID_DESC = "Assigned procedure identifier or template if combinated with procedure.colmun";
    public static final ParameterDescriptor<String> PROCEDURE_ID = PARAM_BUILDER
            .addName(PROCEDURE_ID_NAME)
            .setRemarks(PROCEDURE_ID_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String PROCEDURE_COLUMN_NAME = "procedure_column";
    public static final String PROCEDURE_COLUMN_DESC = "Extracted procedure column";
    public static final ParameterDescriptor<String> PROCEDURE_COLUMN  = PARAM_BUILDER
            .addName(PROCEDURE_COLUMN_NAME)
            .setRemarks(PROCEDURE_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String PROCEDURE_NAME_COLUMN_NAME = "procedure_name_column";
    public static final String PROCEDURE_NAME_COLUMN_DESC = "Extracted procedure name column";
    public static final ParameterDescriptor<String> PROCEDURE_NAME_COLUMN  = PARAM_BUILDER
            .addName(PROCEDURE_NAME_COLUMN_NAME)
            .setRemarks(PROCEDURE_NAME_COLUMN_DESC)
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
            .setRequired(true)
            .create(String.class, ",");

    public static final String MAIN_COLUMN_NAME = FileParsingObservationStoreFactory.MAIN_COLUMN.getName().getCode();
    public static final String MAIN_COLUMN_DESC = FileParsingObservationStoreFactory.MAIN_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName(MAIN_COLUMN_NAME)
            .setRemarks(MAIN_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");
    
    public static final String Z_COLUMN_NAME = FileParsingObservationStoreFactory.Z_COLUMN.getName().getCode();
    public static final String Z_COLUMN_DESC = FileParsingObservationStoreFactory.Z_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> Z_COLUMN = PARAM_BUILDER
            .addName(Z_COLUMN_NAME)
            .setRemarks(Z_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String DATE_COLUMN_NAME = FileParsingObservationStoreFactory.DATE_COLUMN.getName().getCode();
    public static final String DATE_COLUMN_DESC = FileParsingObservationStoreFactory.DATE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName(DATE_COLUMN_NAME)
            .setRemarks(DATE_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

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
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final String LATITUDE_COLUMN_NAME = FileParsingObservationStoreFactory.LATITUDE_COLUMN.getName().getCode();
    public static final String LATITUDE_COLUMN_DESC = FileParsingObservationStoreFactory.LATITUDE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName(LATITUDE_COLUMN_NAME)
            .setRemarks(LATITUDE_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

    public static final String FOI_COLUMN_NAME = FileParsingObservationStoreFactory.FOI_COLUMN.getName().getCode();
    public static final String FOI_COLUMN_DESC = FileParsingObservationStoreFactory.FOI_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName(FOI_COLUMN_NAME)
            .setRemarks(FOI_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String REMOVE_PREVIOUS_NAME = "remove_previous_integration";
    public static final String REMOVE_PREVIOUS_DESC = "remove previous integration";
    public static final ParameterDescriptor<Boolean> REMOVE_PREVIOUS = PARAM_BUILDER
            .addName(REMOVE_PREVIOUS_NAME)
            .setRemarks(REMOVE_PREVIOUS_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String EXTRACT_UOM_NAME = "extract_uom";
    public static final String EXTRACT_UOM_DESC = "extract uom from headers";
    public static final ParameterDescriptor<Boolean> EXTRACT_UOM = PARAM_BUILDER
            .addName(EXTRACT_UOM_NAME)
            .setRemarks(EXTRACT_UOM_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final String STORE_ID_NAME = "store_id";
    public static final String STORE_ID_DESC = "Store Identifier";
    public static final ParameterDescriptor<String> STORE_ID = PARAM_BUILDER
            .addName(STORE_ID_NAME)
            .setRemarks(STORE_ID_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"observationCsvFile", "observationCsvFlatFile", "observationDbfFile"}, "observationCsvFile");

    public static final String FORMAT_NAME = "format";
    public static final String FORMAT_DESC = "Data format (mime type) of the file to insert";
    public static final ParameterDescriptor<String> FORMAT = PARAM_BUILDER
            .addName(FORMAT_NAME)
            .setRemarks(FORMAT_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"text/csv; subtype=\"om\"", "application/dbase; subtype=\"om\""}, "text/csv; subtype=\"om\"");

    public static final String RESULT_COLUMN_NAME = "result_column";
    public static final String RESULT_COLUMN_DESC = "Column containing result values (used with csv-flat)";
    public static final ParameterDescriptor<String> RESULT_COLUMN = PARAM_BUILDER
            .addName(RESULT_COLUMN_NAME)
            .setRemarks(RESULT_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_PROP_COLUMN_NAME = "observed_properties_columns";
    public static final String OBS_PROP_COLUMN_DESC = "Columns containing the observed property (used with csv-flat)";
    public static final ParameterDescriptor<String> OBS_PROP_COLUMN = new ExtendedParameterDescriptor<>(
            OBS_PROP_COLUMN_NAME,
            OBS_PROP_COLUMN_DESC,
            0, 92,
            String.class,
            null, null, null
    );

    public static final String OBS_PROP_NAME_COLUMN_NAME = "observed_properties_name_columns";
    public static final String OBS_PROP_NAME_COLUMN_DESC = "Columns containing the observed property description (used with csv-flat)";
    public static final ParameterDescriptor<String> OBS_PROP_NAME_COLUMN = new ExtendedParameterDescriptor<>(
            OBS_PROP_NAME_COLUMN_NAME,
            OBS_PROP_NAME_COLUMN_DESC,
            0, 92,
            String.class,
            null, null, null
    );

    public static final String OBS_PROP_COLUMNS_FILTER_NAME = "observed_properties_columns_filters";
    public static final String OBS_PROP_COLUMNS_FILTER_DESC =  "A filter on observed properties to extract";
    public static final ParameterDescriptor<String> OBS_PROP_COLUMNS_FILTER  = new ExtendedParameterDescriptor<>(
                OBS_PROP_COLUMNS_FILTER_NAME,
                OBS_PROP_COLUMNS_FILTER_DESC,
                0, 92,
                String.class,
                null, null, null
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

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, USER, PWD, REMOTE_READ, SERVICE_ID, DATASET_IDENTIFIER, PROCEDURE_ID, PROCEDURE_COLUMN, PROCEDURE_NAME_COLUMN, OBS_TYPE,
                    SEPARATOR, CHARQUOTE, MAIN_COLUMN, Z_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, FOI_COLUMN, REMOVE_PREVIOUS, EXTRACT_UOM,
                    STORE_ID, FORMAT, RESULT_COLUMN, OBS_PROP_COLUMN, OBS_PROP_NAME_COLUMN, OBS_PROP_COLUMNS_FILTER, TYPE_COLUMN);

    public static final String FILE_INSERTED_NAME = "files_inserted_count";
    public static final String FILE_INSERTED_DESC = "Number of files inserted ";
    public static final ParameterDescriptor<Integer> FILE_INSERTED = PARAM_BUILDER
            .addName(FILE_INSERTED_NAME)
            .setRemarks(FILE_INSERTED_DESC)
            .setRequired(false)
            .create(Integer.class, 0);


    public static final String OBSERVATION_INSERTED_NAME = "observations_inserted_count";
    public static final String OBSERVATION_INSERTED_DESC = "Number of observations inserted";
    public static final ParameterDescriptor<Integer> OBSERVATION_INSERTED = PARAM_BUILDER
            .addName(OBSERVATION_INSERTED_NAME)
            .setRemarks(OBSERVATION_INSERTED_DESC)
            .setRequired(false)
            .create(Integer.class, 0);

    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup(FILE_INSERTED, OBSERVATION_INSERTED);


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
