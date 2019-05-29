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

import java.util.Collections;
import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
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
    public static final String DATA_FOLDER_NAME = "data folder";
    public static final String DATA_FOLDER_DESC = "data folder";
    public static final ParameterDescriptor<String> DATA_FOLDER = PARAM_BUILDER
            .addName(DATA_FOLDER_NAME)
            .setRemarks(DATA_FOLDER_DESC)
            .setRequired(true)
            .create(String.class, null);

    public static final String SERVICE_ID_NAME = "SOS service";
    public static final String SERVICE_ID_DESC = "SOS service";
    public static final ParameterDescriptor<ServiceProcessReference> SERVICE_ID =
        new ExtendedParameterDescriptor<>(
                SERVICE_ID_NAME, SERVICE_ID_DESC, ServiceProcessReference.class, null, true, Collections.singletonMap("filter", Collections.singletonMap("type", "sos")));

    public static final String DATASET_IDENTIFIER_NAME = "dataset identifier";
    public static final String DATASET_IDENTIFIER_DESC = "dataset identifier";
    public static final ParameterDescriptor<String> DATASET_IDENTIFIER = PARAM_BUILDER
            .addName(DATASET_IDENTIFIER_NAME)
            .setRemarks(DATASET_IDENTIFIER_DESC)
            .setRequired(true)
            .create(String.class, null);

    public static final String PROCEDURE_ID_NAME = "procedure id";
    public static final String PROCEDURE_ID_DESC = "Assigned procedure identifier";
    public static final ParameterDescriptor<String> PROCEDURE_ID = PARAM_BUILDER
            .addName(PROCEDURE_ID_NAME)
            .setRemarks(PROCEDURE_ID_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_TYPE_NAME = "Observation Type";
    public static final String OBS_TYPE_DESC = "Observation Type";
    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName(OBS_TYPE_NAME)
            .setRemarks(OBS_TYPE_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, "Timeserie");

    public static final String SEPARATOR_NAME = CSVFeatureStoreFactory.SEPARATOR.getName().getCode();
    public static final String SEPARATOR_DESC = CSVFeatureStoreFactory.SEPARATOR.getName().getCode();
    public static final ParameterDescriptor<String> SEPARATOR = PARAM_BUILDER
            .addName(SEPARATOR_NAME)
            .setRemarks(SEPARATOR_DESC)
            .setRequired(true)
            .create(String.class, ",");

    public static final String MAIN_COLUMN_NAME = CsvObservationStoreFactory.MAIN_COLUMN.getName().getCode();
    public static final String MAIN_COLUMN_DESC = CsvObservationStoreFactory.MAIN_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName(MAIN_COLUMN_NAME)
            .setRemarks(MAIN_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final String DATE_COLUMN_NAME = CsvObservationStoreFactory.DATE_COLUMN.getName().getCode();
    public static final String DATE_COLUMN_DESC = CsvObservationStoreFactory.DATE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName(DATE_COLUMN_NAME)
            .setRemarks(DATE_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final String DATE_FORMAT_NAME = CsvObservationStoreFactory.DATE_FORMAT.getName().getCode();
    public static final String DATE_FORMAT_DESC = CsvObservationStoreFactory.DATE_FORMAT.getName().getCode();
    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName(DATE_FORMAT_NAME)
            .setRemarks(DATE_FORMAT_DESC)
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'hh:mm:ss'Z'");

    public static final String LONGITUDE_COLUMN_NAME = CsvObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode();
    public static final String LONGITUDE_COLUMN_DESC = CsvObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName(LONGITUDE_COLUMN_NAME)
            .setRemarks(LONGITUDE_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final String LATITUDE_COLUMN_NAME = CsvObservationStoreFactory.LATITUDE_COLUMN.getName().getCode();
    public static final String LATITUDE_COLUMN_DESC = CsvObservationStoreFactory.LATITUDE_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName(LATITUDE_COLUMN_NAME)
            .setRemarks(LATITUDE_COLUMN_DESC)
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

    public static final String FOI_COLUMN_NAME = CsvObservationStoreFactory.FOI_COLUMN.getName().getCode();
    public static final String FOI_COLUMN_DESC = CsvObservationStoreFactory.FOI_COLUMN.getName().getCode();
    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName(FOI_COLUMN_NAME)
            .setRemarks(FOI_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String MEASURE_COLUMNS_NAME = "measure columns";
    public static final String MEASURE_COLUMNS_DESC =  "A set of measure columns to extract";
    public static final ParameterDescriptor<String> MEASURE_COLUMNS  = new ExtendedParameterDescriptor<>(
                MEASURE_COLUMNS_NAME,
                MEASURE_COLUMNS_DESC,
                0, 92,
                String.class,
                null, null, null
                );

    public static final String REMOVE_PREVIOUS_NAME = "remove previous integration";
    public static final String REMOVE_PREVIOUS_DESC = "remove previous integration";
    public static final ParameterDescriptor<Boolean> REMOVE_PREVIOUS = PARAM_BUILDER
            .addName(REMOVE_PREVIOUS_NAME)
            .setRemarks(REMOVE_PREVIOUS_DESC)
            .setRequired(false)
            .create(Boolean.class, false);

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, SERVICE_ID, DATASET_IDENTIFIER, PROCEDURE_ID, OBS_TYPE,
                    SEPARATOR, MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, FOI_COLUMN, MEASURE_COLUMNS, REMOVE_PREVIOUS);


    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup();


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
