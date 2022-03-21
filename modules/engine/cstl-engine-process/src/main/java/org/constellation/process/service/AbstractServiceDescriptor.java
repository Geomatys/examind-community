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
package org.constellation.process.service;

import org.constellation.api.ServiceDef;
import org.constellation.dto.contact.Details;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.opengis.metadata.identification.Identification;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class AbstractServiceDescriptor extends AbstractCstlProcessDescriptor {

    public AbstractServiceDescriptor(final String name, final Identification factoryId, final InternationalString abs,
            final ParameterDescriptorGroup inputDesc, final ParameterDescriptorGroup outputdesc) {
        super(name, factoryId, abs, inputDesc, outputdesc);
    }

    public static final String SERVICE_TYPE_NAME = "service_type";
    private static final String SERVICE_TYPE_REMARKS = "The type of the service.";
    public static final ParameterDescriptor<String> SERVICE_TYPE = BUILDER
            .addName(SERVICE_TYPE_NAME)
            .setRemarks(SERVICE_TYPE_REMARKS)
            .setRequired(true)
            .createEnumerated(String.class, ServiceDef.Specification.availableSpecifications(), null);

    public static final String IDENTIFIER_NAME = "identifier";
    private static final String IDENTIFIER_REMARKS = "Identifier of the new service instance.";
    public static final ParameterDescriptor<String> IDENTIFIER = BUILDER
            .addName(IDENTIFIER_NAME)
            .setRemarks(IDENTIFIER_REMARKS)
            .setRequired(false)
            .create( String.class, null);

    public static final String CONFIG_NAME = "configuration";
    private static final String CONFIG_REMARKS = "Object use to configure the instance. If not specified the instance will be configured  a default value.";
    public static final ParameterDescriptor<Object> CONFIGURATION = BUILDER
            .addName(CONFIG_NAME)
            .setRemarks(CONFIG_REMARKS)
            .setRequired(false)
            .create(Object.class, null);

    public static final String SERVICE_METADATA_NAME = "serviceMetadata";
    private static final String SERVICE_METADATA_REMARKS = "The service metadata to apply.";
    public static final ParameterDescriptor<Details> SERVICE_METADATA = BUILDER
            .addName(SERVICE_METADATA_NAME)
            .setRemarks(SERVICE_METADATA_REMARKS)
            .setRequired(false)
            .create(Details.class, null);
}
