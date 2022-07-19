/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.process.sensor;

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DeleteSensorDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "sensor.remove";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Remove one or all the sensors.");

    public static final String SENSOR_IDENTIFIER_NAME = "sensor_identifier";
    private static final String SENSOR_IDENTIFIER_REMARKS = "the identifier of the sensor.";
    public static final ParameterDescriptor<String> SENSOR_IDENTIFIER = BUILDER
            .addName(SENSOR_IDENTIFIER_NAME)
            .setRemarks(SENSOR_IDENTIFIER_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String DELETE_DATA_NAME = "delete_data";
    private static final String DELETE_DATA_REMARKS = "If set this flag the associated data wil be removed.";
    public static final ParameterDescriptor<Boolean> DELETE_DATA = BUILDER
            .addName(DELETE_DATA_NAME)
            .setRemarks(DELETE_DATA_REMARKS)
            .setRequired(true)
            .create(Boolean.class, Boolean.TRUE);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(SENSOR_IDENTIFIER, DELETE_DATA);

     /**Output parameters */
     public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
             .createGroup();

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public DeleteSensorDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new DeleteSensorDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeleteSensorProcess(this, input);
    }
}
