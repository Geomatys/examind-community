/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2011, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package com.examind.process.sos;

import org.apache.sis.parameter.ParameterBuilder;
import org.geotoolkit.processing.AbstractProcessDescriptor;
import org.geotoolkit.process.Process;
import org.geotoolkit.process.ProcessDescriptor;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author Samuel Andrés (Geomatys)
 */
public class SosHarvesterProcessDescriptor extends AbstractProcessDescriptor{

    /**Process name : addition */
    public static final String NAME = "sosHarvester";

    private static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    /**
     * Input parameters
     */
    public static final ParameterDescriptor<String> DATA_FOLDER = PARAM_BUILDER
            .addName("data folder")
            .setRemarks("data folder")
            .setRequired(true)
            .create(String.class, null);
    public static final ParameterDescriptor<ServiceProcessReference> SERVICE_ID = PARAM_BUILDER
            .addName("SOS service")
            .setRemarks("SOS service")
            .setRequired(true)
            .create(ServiceProcessReference.class, null);
    public static final ParameterDescriptor<String> DATASET_IDENTIFIER = PARAM_BUILDER
            .addName("dataset identifier")
            .setRemarks("dataset identifier")
            .setRequired(true)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBS_TYPE = PARAM_BUILDER
            .addName("Observation Type")
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, "Timeserie");

    public static final ParameterDescriptor<Character> SEPARATOR = PARAM_BUILDER
            .addName(CSVFeatureStoreFactory.SEPARATOR.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(Character.class, ',');

    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName(CsvObservationStoreFactory.MAIN_COLUMN.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName(CsvObservationStoreFactory.DATE_COLUMN.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName(CsvObservationStoreFactory.DATE_FORMAT.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'hh:mm:ss'Z'");

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName(CsvObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName(CsvObservationStoreFactory.LATITUDE_COLUMN.getName().getCode())
//            .setRemarks(org.geotoolkit.data.csv.Bundle.formatInternational(org.geotoolkit.data.csv.Bundle.Keys.paramSeparatorRemarks))
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName(CsvObservationStoreFactory.FOI_COLUMN.getName().getCode())
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> MEASURE_COLUMNS  = new ExtendedParameterDescriptor<>(
                "measure columns",
                 "A set of measure columns to extract",
                 0, 92,
                String.class,
                null, null, null
                );

    public static final ParameterDescriptorGroup INPUT_DESC =
            PARAM_BUILDER.addName("InputParameters").createGroup(DATA_FOLDER, SERVICE_ID, DATASET_IDENTIFIER, OBS_TYPE,
                    SEPARATOR, MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, FOI_COLUMN, MEASURE_COLUMNS);


    public static final ParameterDescriptorGroup OUTPUT_DESC =
            PARAM_BUILDER.addName("OutputParameters").createGroup();


    /** Instance */
    public static final ProcessDescriptor INSTANCE = new SosHarvesterProcessDescriptor();

    public SosHarvesterProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Harvests SOS data."),
                INPUT_DESC, OUTPUT_DESC);
    }

    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new SosHarvesterProcess(this, input);
    }

}
