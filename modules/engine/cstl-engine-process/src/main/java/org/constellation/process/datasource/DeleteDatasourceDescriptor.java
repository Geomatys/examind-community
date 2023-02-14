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
package org.constellation.process.datasource;

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
public class DeleteDatasourceDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "datasource.remove";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Remove a or multiple datasource by their id or path.");

    public static final String DATASOURCE_IDENTIFIER_NAME = "datasource.id";
    private static final String DATASOURCE_IDENTIFIER_REMARKS = "the identifier of the datasource to remove.";
    public static final ParameterDescriptor<Integer> DATASOURCE_IDENTIFIER = BUILDER
            .addName(DATASOURCE_IDENTIFIER_NAME)
            .setRemarks(DATASOURCE_IDENTIFIER_REMARKS)
            .setRequired(false)
            .create(Integer.class, null);

    public static final String DATASOURCE_PATH_NAME = "datasource.path";
    private static final String DATASOURCE_PATH_REMARKS = "the path of the datasources to remove.";
    public static final ParameterDescriptor<String> DATASOURCE_PATH = BUILDER
            .addName(DATASOURCE_PATH_NAME)
            .setRemarks(DATASOURCE_PATH_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(DATASOURCE_IDENTIFIER, DATASOURCE_PATH);

     /**Output parameters */
     public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
             .createGroup();

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public DeleteDatasourceDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    public static final ProcessDescriptor INSTANCE = new DeleteDatasourceDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new DeleteDatasourceProcess(this, input);
    }
}
