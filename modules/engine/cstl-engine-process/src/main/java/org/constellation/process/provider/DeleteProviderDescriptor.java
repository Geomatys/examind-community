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

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Quentin Boileau (Geomatys).
 */
public class DeleteProviderDescriptor extends AbstractProviderDescriptor {

    public static final String NAME = "provider.delete";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Delete a provider from Examind.");

    public static final String ONLY_HIDDEN_DATA_NAME = "only_hidden_data";
    private static final String ONLY_HIDDEN_DATA_REMARKS = "Flag to remove provider containing only hidden (non-pyramid) data.";
    public static final ParameterDescriptor<Boolean> ONLY_HIDDEN_DATA = BUILDER
            .addName(ONLY_HIDDEN_DATA_NAME)
            .setRemarks(ONLY_HIDDEN_DATA_REMARKS)
            .setRequired(false)
            .create(Boolean.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(PROVIDER_ID, ONLY_HIDDEN_DATA);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public DeleteProviderDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, EMPTY_OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new DeleteProviderDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeleteProvider(this, input);
    }

}
