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
    public static final ParameterDescriptor<String> DATA_FOLDER = PARAM_BUILDER
            .addName("data folder")
            .setRemarks("data folder")
            .setRequired(true)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName("Observation Type")
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, "Timeserie");

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, OBS_TYPE);

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
