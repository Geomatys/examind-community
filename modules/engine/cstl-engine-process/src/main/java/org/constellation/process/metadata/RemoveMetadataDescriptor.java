/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package org.constellation.process.metadata;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Quentin Boileau (Geomatys)
 */
public class RemoveMetadataDescriptor extends AbstractProcessDescriptor {
    
    public static final String NAME = "metadata.remove";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Remove a metadata to a CSW service.");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String SERVICE_IDENTIFIER_NAME = "service_identifier";
    private static final String SERVICE_IDENTIFIER_REMARKS = "the identifier of the CSW service.";
    public static final ParameterDescriptor<String> SERVICE_IDENTIFIER = BUILDER
            .addName(SERVICE_IDENTIFIER_NAME)
            .setRemarks(SERVICE_IDENTIFIER_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String METADATA_ID_NAME = "metadata-id";
    private static final String METADATA_ID_REMARKS = "The metadata identifier.";
    public static final ParameterDescriptor<String> METADATA_ID = BUILDER
            .addName(METADATA_ID_NAME)
            .setRemarks(METADATA_ID_REMARKS)
            .setRequired(true)
            .create(String.class, null);
    
    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(SERVICE_IDENTIFIER, METADATA_ID);
    
     /**Output parameters */
     public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
             .createGroup();
    
    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public RemoveMetadataDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new RemoveMetadataDescriptor();
    
    @Override
    public org.geotoolkit.process.Process createProcess(ParameterValueGroup input) {
        return new RemoveMetadaProcess(this, input);
    }
}
