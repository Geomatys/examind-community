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
package org.constellation.process.provider;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ExamindProcessFactory;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class CreateProviderDescriptor extends AbstractProviderDescriptor {

    public static final String NAME = "provider.create";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Create a new provider in Examind.");

    public static final String PROVIDER_TYPE_NAME = "provider_type";
    private static final String PROVIDER_TYPE_REMARKS = "Provider factory name like 'data-store', ... .";
    public static final ParameterDescriptor<String> PROVIDER_TYPE = BUILDER
            .addName(PROVIDER_TYPE_NAME)
            .setRemarks(PROVIDER_TYPE_REMARKS)
            .setRequired(true)
            .create(String.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(PROVIDER_TYPE, SOURCE);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = new ParameterBuilder().addName("OutputParameters").setRequired(true)
            .createGroup(PROVIDER_ID);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public CreateProviderDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final CreateProviderDescriptor INSTANCE = new CreateProviderDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new CreateProvider(this, input);
    }

}
