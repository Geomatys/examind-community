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
import org.apache.sis.storage.GridCoverageResource;
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
 *  @author Guilhem Legal (Geomatys)
 */
public class NbBandDescriptor extends AbstractCstlProcessDescriptor {


    public static final String NAME = "test.nb_band";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Count the number of band in a gridCoverageResource.");

    private static final ParameterBuilder BUILDER = new ParameterBuilder();

    public static final String COVERAGE_NAME = "coverage";
    private static final String COVERAGE_REMARKS = "The input coverage.";
    public static final ParameterDescriptor<GridCoverageResource> COVERAGE = BUILDER
            .addName(COVERAGE_NAME)
            .setRemarks(COVERAGE_REMARKS)
            .setRequired(true)
            .create(GridCoverageResource.class, null);


    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(COVERAGE);

    public static final String NB_BAND_NAME = "nb.band";
    private static final String NB_BAND_REMARKS = "The number of band in the coverage.";
    public static final ParameterDescriptor<Integer> NB_BAND = BUILDER
            .addName(NB_BAND_NAME)
            .setRemarks(NB_BAND_REMARKS)
            .setRequired(true)
            .create(Integer.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(NB_BAND);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public NbBandDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

     public static final ProcessDescriptor INSTANCE = new NbFeatureDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new NbBandProcess(this, input);
    }

}
