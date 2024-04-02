/*
 *    Examind - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FieldNameCorrectorDescriptor extends AbstractProcessDescriptor{
    
    public static final String NAME = "sensor.correct.fieldname";

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    /**
     * Input parameters
     */
    public static final String OBSERVATION_PROVIDER_ID_NAME = "observation_provider_id";
    public static final String OBSERVATION_PROVIDER_ID_DESC = "Observation provider identifier.";
    public static final ParameterDescriptor<String> OBSERVATION_PROVIDER_ID = PARAM_BUILDER
            .addName(OBSERVATION_PROVIDER_ID_NAME)
            .setRemarks(OBSERVATION_PROVIDER_ID_DESC)
            .setRequired(true)
            .create(String.class, null);

    public static final String SENSOR_ID_NAME = "sensor_id";
    public static final String SENSOR_ID_DESC = "Sensor identifier (Optional).";
    public static final ParameterDescriptor<String> SENSOR_ID = PARAM_BUILDER
            .addName(SENSOR_ID_NAME)
            .setRemarks(SENSOR_ID_DESC)
            .setRequired(false)
            .create(String.class, null);
    
    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(OBSERVATION_PROVIDER_ID, SENSOR_ID);

    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup();

    /** Instance */
    public static final ProcessDescriptor INSTANCE = new HarvesterPreProcessDescriptor();

    public FieldNameCorrectorDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Fill the sensor fields label."),INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public org.geotoolkit.process.Process createProcess(final ParameterValueGroup input) {
        return new FieldNameCorrectorProcess(this, input);
    }
}
