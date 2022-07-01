/*
 *    Examind Comunity - An open source and standard compliant SDI
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
package com.examind.process.test;

import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.util.SimpleInternationalString;
import org.constellation.dto.MapContextLayersDTO;
import org.constellation.dto.process.MapContextProcessReference;
import org.constellation.process.AbstractCstlProcess;
import org.constellation.process.AbstractCstlProcessDescriptor;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.InternationalString;

/**
 * This is a test process to demonstrate the mapLayers parameter input.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextInputDescriptor extends AbstractCstlProcessDescriptor {

    public static final String NAME = "test.mapcontext.input";
    public static final InternationalString ABSTRACT = new SimpleInternationalString("Test MapContext input.");


    public static final String SIS_INPUT_NAME = "sis.input";
    private static final String SIS_INPUT_REMARKS = "SIS Maplayers input.";
    public static final ParameterDescriptor<MapLayers> SIS_INPUT;

    public static final String EXA_REF_INPUT_NAME = "exa.ref.input";
    private static final String EXA_REF_INPUT_REMARKS = "Examind mapcontext reference input.";
    public static final ParameterDescriptor<MapContextProcessReference> EXA_REF_INPUT;

    public static final String EXA_INPUT_NAME = "exa.input";
    private static final String EXA_INPUT_REMARKS = "Examind mapcontext input.";
    public static final ParameterDescriptor<MapContextLayersDTO> EXA_INPUT;

    public static final ParameterDescriptorGroup INPUT_DESC;

    public static final String NB_ITEMS_OUTPUT_NAME = "nb.items.output";
    private static final String NB_ITEMS_OUTPUT_REMARKS = "Number of items int the map layers.";
    public static final ParameterDescriptor<Integer> NB_ITEMS_OUTPUT;

    public static final ParameterDescriptorGroup OUTPUT_DESC;

    static {

        SIS_INPUT = BUILDER
                .addName(SIS_INPUT_NAME)
                .setRemarks(SIS_INPUT_REMARKS)
                .setRequired(false)
                .create(MapLayers.class, null);

        EXA_REF_INPUT = BUILDER
                .addName(EXA_REF_INPUT_NAME)
                .setRemarks(EXA_REF_INPUT_REMARKS)
                .setRequired(false)
                .create(MapContextProcessReference.class, null);

        EXA_INPUT = BUILDER
                .addName(EXA_INPUT_NAME)
                .setRemarks(EXA_INPUT_REMARKS)
                .setRequired(false)
                .create(MapContextLayersDTO.class, null);

        INPUT_DESC = BUILDER.addName("InputParameters").setRequired(true).createGroup(SIS_INPUT, EXA_REF_INPUT, EXA_INPUT);

        NB_ITEMS_OUTPUT = BUILDER
                .addName(NB_ITEMS_OUTPUT_NAME)
                .setRemarks(NB_ITEMS_OUTPUT_REMARKS)
                .setRequired(false)
                .create(Integer.class, null);

        OUTPUT_DESC = BUILDER.addName("OutputParameters").setRequired(true).createGroup(NB_ITEMS_OUTPUT);
    }

    public static final ProcessDescriptor INSTANCE = new MapContextInputDescriptor();
    
    public MapContextInputDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION, ABSTRACT, INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    protected AbstractCstlProcess buildProcess(ParameterValueGroup input) {
        return new MapContextInputProcess(this, input);
    }

}
