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
package org.constellation.process.provider;

import org.constellation.process.AbstractCstlProcessDescriptor;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractProviderDescriptor extends AbstractCstlProcessDescriptor {

    public AbstractProviderDescriptor(final String name, final Identification factoryId, final InternationalString abs,
            final ParameterDescriptorGroup inputDesc, final ParameterDescriptorGroup outputdesc) {
        super(name, factoryId, abs, inputDesc, outputdesc);
    }

    public static final String PROVIDER_ID_NAME = "provider_id";
    private static final String PROVIDER_ID_REMARKS = "Identifier of the provider.";
    public static final ParameterDescriptor<Integer> PROVIDER_ID = BUILDER
            .addName(PROVIDER_ID_NAME)
            .setRemarks(PROVIDER_ID_REMARKS)
            .setRequired(false)
            .create(Integer.class, null);

    public static final String SOURCE_NAME = "source";
    private static final String SOURCE_REMARKS = "provider source configuration.";
    public static final ParameterDescriptor<ParameterValueGroup> SOURCE = BUILDER
            .addName(SOURCE_NAME)
            .setRemarks(SOURCE_REMARKS)
            .setRequired(true)
            .create(ParameterValueGroup.class, null);


}
