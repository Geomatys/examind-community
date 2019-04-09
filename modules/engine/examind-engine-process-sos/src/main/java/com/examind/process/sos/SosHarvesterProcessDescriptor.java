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

import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 * @author Samuel Andrés (Geomatys)
 */
public class SosHarvesterProcessDescriptor extends AbstractProcessDescriptor{

    /**Process name : addition */
    public static final String NAME = "sosHarvester";

    /**
     * Input parameters
     */
    public static final ParameterDescriptor<String> DATA_FOLDER = new ParameterBuilder()
            .addName("data folder")
            .setRemarks("data folder")
            .setRequired(true)
            .create(String.class, null);
    public static final ParameterDescriptor<ServiceProcessReference> SERVICE_ID = new ParameterBuilder()
            .addName("SOS service")
            .setRemarks("SOS service")
            .setRequired(true)
            .create(ServiceProcessReference.class, null);
    public static final ParameterDescriptor<String> DATASET_IDENTIFIER = new ParameterBuilder()
            .addName("dataset identifier")
            .setRemarks("dataset identifier")
            .setRequired(true)
            .create(String.class, null);

    public static final ParameterDescriptor<Character> SEPARATOR = new ParameterBuilder()
            .addName(CSVFeatureStoreFactory.SEPARATOR.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(Character.class, ',');

    public static final ParameterDescriptor<String> DATE_COLUMN = new ParameterBuilder()
            .addName(CsvObservationStoreFactory.DATE_COLUMN.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_FORMAT = new ParameterBuilder()
            .addName(CsvObservationStoreFactory.DATE_FORMAT.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'hh:mm:ss'Z'");

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = new ParameterBuilder()
            .addName(CsvObservationStoreFactory.LONGITUDE_COLUMN.getName().getCode())
//            .setRemarks(Bundle.formatInternational(Bundle.Keys.paramURLRemarks))
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = new ParameterBuilder()
            .addName(CsvObservationStoreFactory.LATITUDE_COLUMN.getName().getCode())
//            .setRemarks(org.geotoolkit.data.csv.Bundle.formatInternational(org.geotoolkit.data.csv.Bundle.Keys.paramSeparatorRemarks))
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

    public static final ParameterDescriptor<String> MEASURE_COLUMNS = new ParameterBuilder()
            .addName("measure columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptorGroup INPUT_DESC =
            new ParameterBuilder().addName("InputParameters").createGroup(DATA_FOLDER, SERVICE_ID, DATASET_IDENTIFIER,
                    SEPARATOR, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, MEASURE_COLUMNS);


    /** Instance */
    public static final ProcessDescriptor INSTANCE = new SosHarvesterProcessDescriptor();

    public SosHarvesterProcessDescriptor() {
        super(NAME, ExamindProcessFactory.IDENTIFICATION,
                new SimpleInternationalString("Harvests SOS data."),
                INPUT_DESC, new ParameterBuilder().addName("OutputParameters").createGroup());
    }

    @Override
    public Process createProcess(final ParameterValueGroup input) {
        return new SosHarvesterProcess(this, input);
    }

}
