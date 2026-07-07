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

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
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
public class ReloadFsConfigDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "reload.fs.config";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Reload the filesystem config.");

    /**Input parameters */
    
    public static final String ASYNC_NAME = "async";
    private static final String ASYNC_REMARKS = "if set to true the service will be created first, and the populated as the datas are installed.";
    public static final ParameterDescriptor<Boolean> ASYNC;
    
    public static final String DIFF_NAME = "diff";
    private static final String DIFF_REMARKS = "if set to true the files will be monitored to detect changes at reload.";
    public static final ParameterDescriptor<Boolean> DIFF;
    
    public static final String CLEANUP_NAME = "cleanup";
    private static final String CLEANUP_REMARKS = "if set to true all the entities will be removed before installing new one.";
    public static final ParameterDescriptor<Boolean> CLEANUP;
    
    static {
        final ParameterBuilder builder = new ParameterBuilder();
        
        // fill the default value from the configuration
        boolean asyncDefValue = Application.getBooleanProperty(AppProperty.EXA_FS_ASYNC, Boolean.FALSE);
        ASYNC = builder
            .addName(ASYNC_NAME)
            .setRemarks(ASYNC_REMARKS)
            .setRequired(false)
            .create(Boolean.class, asyncDefValue);
        
        // fill the default value from the configuration
        boolean diffDefValue = Application.getBooleanProperty(AppProperty.EXA_FS_DIFF, Boolean.FALSE);
        DIFF = builder
            .addName(DIFF_NAME)
            .setRemarks(DIFF_REMARKS)
            .setRequired(false)
            .create(Boolean.class, diffDefValue);
        
        // if diff mode is set to true, the cleanup is so to false by default
        // however we allow the user to force the deletion
        CLEANUP = builder
            .addName(CLEANUP_NAME)
            .setRemarks(CLEANUP_REMARKS)
            .setRequired(false)
            .create(Boolean.class, !diffDefValue);
    }

    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(ASYNC, DIFF, CLEANUP);

     /**Output parameters */
     public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
             .createGroup();

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public ReloadFsConfigDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new ReloadFsConfigDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ReloadFsConfigProcess(this, input);
    }
}
