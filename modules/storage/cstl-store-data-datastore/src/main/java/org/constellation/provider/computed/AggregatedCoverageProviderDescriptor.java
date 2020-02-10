/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.provider.computed;

import org.constellation.provider.DataProviderFactory;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.createFixedIdentifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageProviderDescriptor extends ComputedResourceProviderDescriptor {

    public static final String NAME = "AggregatedCoverageProvider";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptor<String> RESULT_CRS =
             BUILDER.addName("ResultCRS").setRemarks("Result CRS").setRequired(false).create(String.class,null);

    public static final ParameterDescriptor<String> MODE =
             BUILDER.addName("mode").setRemarks("mode").setRequired(false).create(String.class, null);


    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = BUILDER.addName(NAME).addName("AggregatedCoverageProvider").setRequired(true)
            .createGroup(IDENTIFIER, DATA_NAME, DATA_IDS, RESULT_CRS, MODE);

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public ComputedResourceProvider buildProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        return new AggregatedCoverageProvider(providerId, service, param);
    }

    @Override
    public String getName() {
        return NAME;
    }

}
