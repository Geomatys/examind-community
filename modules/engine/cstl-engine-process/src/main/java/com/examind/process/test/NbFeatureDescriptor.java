/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package com.examind.process.test;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.FeatureSet;
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
public class NbFeatureDescriptor extends AbstractCstlProcessDescriptor {


    public static final String NAME = "test.nb_feature";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Count the number of feature in a featureSet.");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String FEATURESET_NAME = "featureset";
    private static final String FEATURESET_REMARKS = "The input featureSet.";
    public static final ParameterDescriptor<FeatureSet> FEATURESET = BUILDER
            .addName(FEATURESET_NAME)
            .setRemarks(FEATURESET_REMARKS)
            .setRequired(true)
            .create(FeatureSet.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(FEATURESET);

    public static final String NB_FEATURE_NAME = "nb.feature";
    private static final String NB_FEATURE_REMARKS = "The number of feature in the featureSet.";
    public static final ParameterDescriptor<Long> NB_FEATURE = BUILDER
            .addName(NB_FEATURE_NAME)
            .setRemarks(NB_FEATURE_REMARKS)
            .setRequired(true)
            .create(Long.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(NB_FEATURE);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public NbFeatureDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

     public static final ProcessDescriptor INSTANCE = new NbFeatureDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new NbFeatureProcess(this, input);
    }

}
