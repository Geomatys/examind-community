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
package com.examind.process.datacombine;

import com.examind.process.admin.AdminProcessDescriptor;
import com.examind.process.admin.AdminProcessRegistry;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageDescriptor extends AbstractDataCombineDescriptor implements AdminProcessDescriptor {

    public static final ParameterDescriptor<String> RESULT_CRS;
    public static final ParameterDescriptor<String> MODE;

    public static final ParameterDescriptorGroup INPUT;

    public static final ParameterDescriptorGroup OUTPUT;

     static {
        final ParameterBuilder builder = new ParameterBuilder();

        builder.setRequired(true);

        RESULT_CRS = builder.addName("result.crs")
                .setDescription("Result CRS.")
                .create(String.class, "EPSG:4326");

        MODE = builder.addName("mode")
                .setDescription("Aggregation ordering mode.")
                .createEnumerated(String.class, new String[]{"ORDER", "SCALE"}, "ORDER");

        INPUT = builder.addName("input")
                .setRequired(true)
                .createGroup(DATA_NAME, TARGET_DATASET, DATASET, DATA, RESULT_CRS, MODE);

        OUTPUT = builder.addName("output")
                .createGroup();
    }

    public AggregatedCoverageDescriptor() {
        super(new DefaultIdentifier(new DefaultCitation(AdminProcessRegistry.NAME), "Aggregated coverage"), new SimpleInternationalString("Combine coverage data"), INPUT, OUTPUT);
    }

    @Override
    public org.geotoolkit.process.Process createProcess(ParameterValueGroup input) {
        return new AggregatedCoverageProcess(this, input);
    }

}
