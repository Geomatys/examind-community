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

import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author Samuel Andrés (Geomatys)
 */
public class HarvesterPreProcessDescriptor extends AbstractProcessDescriptor{

    /**Process name : addition */
    public static final String NAME = "sosHarvester.prepare";

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

    public static final String USER_NAME = "user name";
    public static final String USER_DESC = "user name";
    public static final ParameterDescriptor<String> USER = PARAM_BUILDER
            .addName(USER_NAME)
            .setRemarks(USER_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String PWD_NAME = "password";
    public static final String PWD_DESC = "password";
    public static final ParameterDescriptor<String> PWD = PARAM_BUILDER
            .addName(PWD_NAME)
            .setRemarks(PWD_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String OBS_TYPE_NAME = "Observation Type";
    public static final String OBS_TYPE_DESC = "Observation Type";
    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName(OBS_TYPE_NAME)
            .setRemarks(OBS_TYPE_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, "Timeserie");

    public static final String TASK_NAME_NAME = "generated task name";
    public static final String TASK_NAME_DESC = "generated task name";
    public static final ParameterDescriptor<String> TASK_NAME = PARAM_BUILDER
            .addName(TASK_NAME_NAME)
            .setRemarks(TASK_NAME_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String FORMAT_NAME = "Format";
    public static final String FORMAT_DESC = "Format";
    public static final ParameterDescriptor<String> FORMAT = PARAM_BUILDER
            .addName(FORMAT_NAME)
            .setRemarks(FORMAT_DESC)
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"csv", "csv-coriolis", "dbf"}, "csv");

    public static final String MEASURE_VALUE_NAME = "measure value";
    public static final String MEASURE_VALUE_DESC = "measure value";
    public static final ParameterDescriptor<String> MEASURE_VALUE = PARAM_BUILDER
            .addName(MEASURE_VALUE_NAME)
            .setRemarks(MEASURE_VALUE_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final String MEASURE_CODE_NAME = "measure code";
    public static final String MEASURE_CODE_DESC = "measure code";
    public static final ParameterDescriptor<String> MEASURE_CODE = PARAM_BUILDER
            .addName(MEASURE_CODE_NAME)
            .setRemarks(MEASURE_CODE_DESC)
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, USER, PWD, OBS_TYPE, TASK_NAME, FORMAT, MEASURE_VALUE, MEASURE_CODE);

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
