/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.process.service;

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.service.config.AbstractConfigurationObject;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ExamindProcessFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * ProcessDescriptor for create a new OGC service.
 * 
 * @author Quentin Boileau (Geomatys).
 * @author Benjamin Garcia (Geomatys).
 *
 */
public class CreateServiceDescriptor extends AbstractServiceDescriptor {

    public static final String NAME = "service.create";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Get an existing or create a new ogc service in Examind.");

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(SERVICE_TYPE, IDENTIFIER, CONFIGURATION, SERVICE_METADATA);

    public static final String OUT_CONFIG_NAME = "out_configuration";
    private static final String OUT_CONFIG_REMARKS = "The configuration object for the new instance.";
    public static final ParameterDescriptor<AbstractConfigurationObject> OUT_CONFIGURATION = BUILDER
            .addName(OUT_CONFIG_NAME)
            .setRemarks(OUT_CONFIG_REMARKS)
            .setRequired(false)
            .create(AbstractConfigurationObject.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(OUT_CONFIGURATION);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public CreateServiceDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new CreateService(this, input);
    }

}
