/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2022 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.process.service;

import com.examind.process.test.NbFeatureDescriptor;
import com.examind.process.test.NbFeatureProcess;
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
public class FeatureToGeoJSONDescriptor extends AbstractCstlProcessDescriptor {

     public static final String NAME = "featureset.to.geojson";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Return a GeoJSON view of a featureSet.");

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

    public static final String GEOJSON_OUTPUT_NAME = "geojson.output";
    private static final String GEOJSON_OUTPUT_REMARKS = "The GeoJSON view of the featureSet.";
    public static final ParameterDescriptor<String> GEOJSON_OUTPUT = BUILDER
            .addName(GEOJSON_OUTPUT_NAME)
            .setRemarks(GEOJSON_OUTPUT_REMARKS)
            .setRequired(true)
            .create(String.class, null);

    /**Output parameters */
    public static final ParameterDescriptorGroup OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true)
            .createGroup(GEOJSON_OUTPUT);

    /**
     * Public constructor use by the ServiceRegistry to find and instantiate all ProcessDescriptor.
     */
    public FeatureToGeoJSONDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

     public static final ProcessDescriptor INSTANCE = new FeatureToGeoJSONDescriptor();

    @Override
    public AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new FeatureToGeoJSONProcess(this, input);
    }
}
