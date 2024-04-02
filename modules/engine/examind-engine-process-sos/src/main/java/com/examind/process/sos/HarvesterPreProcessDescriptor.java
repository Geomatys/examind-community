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

import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author Samuel Andrés (Geomatys)
 */
public class HarvesterPreProcessDescriptor extends AbstractProcessDescriptor{

    public static final String NAME = "sosHarvester.prepare";

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

    public static final String OBS_TYPE_NAME = "observation_type";
    public static final String OBS_TYPE_DESC = "Observation type (\"Timeserie\", \"Trajectory\" or \"Profile\")";
    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName(OBS_TYPE_NAME)
            .setRemarks(OBS_TYPE_DESC)
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, null);

    public static final String TASK_NAME_NAME = "generated_task_name";
    public static final String TASK_NAME_DESC = "generated task name";
    public static final ParameterDescriptor<String> TASK_NAME = PARAM_BUILDER
            .addName(TASK_NAME_NAME)
            .setRemarks(TASK_NAME_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String FORMAT_NAME = "format";
    public static final String FORMAT_DESC = "Data format (mime type) of the file to insert";
    public static final ParameterDescriptor<String> FORMAT = PARAM_BUILDER
            .addName(FORMAT_NAME)
            .setRemarks(FORMAT_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"csv", "csv-flat", "dbf", "xlsx", "xlsx-flat", "xls", "xls-flat"}, "csv");

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
    
    public static final String TYPE_COLUMN_NAME = "type_column";
    public static final String TYPE_COLUMN_DESC = "type column";
    public static final ParameterDescriptor<String> TYPE_COLUMN = PARAM_BUILDER
            .addName(TYPE_COLUMN_NAME)
            .setRemarks(TYPE_COLUMN_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String SEPARATOR_NAME = CSVProvider.SEPARATOR.getName().getCode();
    public static final String SEPARATOR_DESC = CSVProvider.SEPARATOR.getName().getCode();
    public static final ParameterDescriptor<String> SEPARATOR = PARAM_BUILDER
            .addName(SEPARATOR_NAME)
            .setRemarks(SEPARATOR_DESC)
            .setRequired(true)
            .create(String.class, ",");

    public static final String CHARQUOTE_NAME = "quote_character";
    public static final String CHARQUOTE_DESC = "quote character";
    public static final ParameterDescriptor<String> CHARQUOTE = PARAM_BUILDER
            .addName(CHARQUOTE_NAME)
            .setRemarks(CHARQUOTE_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, USER, PWD, OBS_TYPE, TASK_NAME, FORMAT, RESULT_COLUMN, OBS_PROP_COLUMN, TYPE_COLUMN, SEPARATOR, CHARQUOTE);

    public static final String PROCESS_ID_NAME = "process.id";
    private static final String PROCESS_ID_REMARKS = "The assigned identifier of the deployed process.";
    public static final ParameterDescriptor<String> PROCESS_ID = PARAM_BUILDER
            .addName(PROCESS_ID_NAME)
            .setRemarks(PROCESS_ID_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup(PROCESS_ID);

    /** Instance */
    public static final ProcessDescriptor INSTANCE = new HarvesterPreProcessDescriptor();

    public HarvesterPreProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("prepare SOS data Harvesting."),INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new HarvesterPreProcess(this, input);
    }

}
