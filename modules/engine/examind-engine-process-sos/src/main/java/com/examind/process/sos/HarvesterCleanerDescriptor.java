/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package com.examind.process.sos;

import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author guilhem
 */
public class HarvesterCleanerDescriptor extends AbstractProcessDescriptor {
    
    /**Process name : addition */
    public static final String NAME = "sosHarvester.clean";

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    /**
     * Input parameters
     */
    public static final String DATA_FOLDER_NAME = "data folder";
    public static final String DATA_FOLDER_DESC = "data folder";
    public static final ParameterDescriptor<String> DATA_FOLDER = PARAM_BUILDER
            .addName(DATA_FOLDER_NAME)
            .setRemarks(DATA_FOLDER_DESC)
            .setRequired(true)
            .create(String.class, null);
    
    public static final String OBS_TYPE_NAME = "Observation Type";
    public static final String OBS_TYPE_DESC = "Observation Type";
    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName(OBS_TYPE_NAME)
            .setRemarks(OBS_TYPE_DESC)
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, null);
    
    public static final String STORE_ID_NAME = "Store Id";
    public static final String STORE_ID_DESC = "Store Id";
    public static final ParameterDescriptor<String> STORE_ID = PARAM_BUILDER
            .addName(STORE_ID_NAME)
            .setRemarks(STORE_ID_DESC)
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"observationCsvFile", "observationCsvFlatFile", "observationDbfFile"}, "observationCsvFile");
    
    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, OBS_TYPE, STORE_ID);
    
    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup();
    
    /** Instance */
    public static final ProcessDescriptor INSTANCE = new HarvesterCleanerDescriptor();

    public HarvesterCleanerDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Harvests SOS data."),
                INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new HarvesterCleanerProcess(this, input);
    }
}
