/*
 *    Examind Community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2024 Geomatys.
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

import org.apache.sis.util.SimpleInternationalString;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ParamDependencyDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "test.param.dependency";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Param used to test parameter value dependencies.");

    public static final String COUNTRY_NAME = "country";
    private static final String COUNTRY_REMARKS = "Country.";
    public static final ParameterDescriptor<String> COUNTRY = BUILDER
            .addName(COUNTRY_NAME)
            .setRemarks(COUNTRY_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    public static final String CITY_NAME = "city";
    private static final String CITY_REMARKS = "City.";
    public static final ParameterDescriptor<String> CITY = BUILDER
            .addName(CITY_NAME)
            .setRemarks(CITY_REMARKS)
            .setRequired(false)
            .create(String.class, null);

    public static final String DISTRICT_NAME = "district";
    private static final String DISTRICT_REMARKS = "District.";
    public static final ParameterDescriptor<Integer> DISTRICT = BUILDER
            .addName(DISTRICT_NAME)
            .setRemarks(DISTRICT_REMARKS)
            .setRequired(false)
            .create(Integer.class, null);
    
    public static final String BOUNDARY_NAME = "boundary";
    private static final String BOUNDARY_REMARKS = "Boundary.";
    public static final ParameterDescriptor<Envelope> BOUNDARY = BUILDER
            .addName(BOUNDARY_NAME)
            .setRemarks(BOUNDARY_REMARKS)
            .setRequired(false)
            .create(Envelope.class, null);

    /**Input parameters */
    public static final ParameterDescriptorGroup INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true)
            .createGroup(COUNTRY, CITY, DISTRICT, BOUNDARY);


    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup();

    public static final ProcessDescriptor INSTANCE = new ParamDependencyDescriptor();

    public ParamDependencyDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new ParamDependencyProcess(this, input);
    }

}
