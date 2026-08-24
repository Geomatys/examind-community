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
package com.examind.process.setup;

import java.nio.file.Path;
import org.apache.sis.parameter.ParameterBuilder;
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
public class ProviderFileHandleDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "provider.file.handle";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Handle files for a provider configurated throught filesystem config.");

    /**Input parameters */
    
    public static final String PROVIDER_PATH_NAME = "provider.path";
    private static final String PROVIDER_PATH_REMARKS = "The provider configuration yaml file.";
    public static final ParameterDescriptor<Path> PROVIDER_PATH;
    
    public static final String FILES_DATASOURCE_NAME = "files.datasource";
    private static final String FILES_DATASOURCE_REMARKS = "The datasource linked to this provider.";
    public static final ParameterDescriptor<Integer> FILES_DATASOURCE;
    
    static {
        final ParameterBuilder builder = new ParameterBuilder();
        
        PROVIDER_PATH = builder
            .addName(PROVIDER_PATH_NAME)
            .setRemarks(PROVIDER_PATH_REMARKS)
            .setRequired(true)
            .create(Path.class, null);
        
        FILES_DATASOURCE = builder
            .addName(FILES_DATASOURCE_NAME)
            .setRemarks(FILES_DATASOURCE_REMARKS)
            .setRequired(false)
            .create(Integer.class, null);
    }

    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(PROVIDER_PATH, FILES_DATASOURCE);

     /**Output parameters */
     public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
             .createGroup();

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public ProviderFileHandleDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new ProviderFileHandleDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ProviderFileHandleProcess(this, input);
    }
}
