/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2019, Geomatys
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

import java.util.logging.Logger;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import static org.apache.sis.storage.DataStoreProvider.LOCATION;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.logging.Logging;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public abstract class FileParsingObservationStoreFactory extends AbstractObservationStoreFactory {

    protected static final Logger LOGGER = Logging.getLogger("com.examind.process.sos");

    protected static final ParameterBuilder PARAM_BUILDER = new ParameterBuilder();

    public static final ParameterDescriptor<String> MAIN_COLUMN = PARAM_BUILDER
            .addName("main column")
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_COLUMN = PARAM_BUILDER
            .addName("date column")
            .setRequired(true)
            .create(String.class, "DATE (yyyy-mm-ddThh:mi:ssZ)");

    public static final ParameterDescriptor<String> DATE_FORMAT = PARAM_BUILDER
            .addName("date format")
            .setRequired(true)
            .create(String.class, "yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static final ParameterDescriptor<String> LONGITUDE_COLUMN = PARAM_BUILDER
            .addName("longitude column")
            .setRequired(true)
            .create(String.class, "LONGITUDE (degree_east)");

    public static final ParameterDescriptor<String> LATITUDE_COLUMN = PARAM_BUILDER
            .addName("latitude column")
            .setRequired(true)
            .create(String.class, "LATITUDE (degree_north)");

    public static final ParameterDescriptor<String> MEASURE_COLUMNS = PARAM_BUILDER
            .addName("measure columns")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> MEASURE_COLUMNS_SEPARATOR = PARAM_BUILDER
            .addName("measure columns separator")
            .setRequired(false)
            .create(String.class, "\\|");

    public static final ParameterDescriptor<String> FOI_COLUMN = PARAM_BUILDER
            .addName("Feature of Interest column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_TYPE = PARAM_BUILDER
            .addName("Observation type")
            .setRequired(false)
            .createEnumerated(String.class, new String[]{"Timeserie", "Trajectory", "Profile"}, "Timeserie");

    public static final ParameterDescriptor<String> PROCEDURE_ID = PARAM_BUILDER
            .addName("Assigned procedure id")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<String> PROCEDURE_COLUMN = PARAM_BUILDER
            .addName("extracted procedure column")
            .setRequired(false)
            .create(String.class, null);

    public static final ParameterDescriptor<Boolean> EXTRACT_UOM = PARAM_BUILDER
            .addName("extract uom")
            .setRequired(false)
            .create(Boolean.class, false);

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        GeneralParameterDescriptor desc;
        try {
            desc = getOpenParameters().descriptor(LOCATION);
        } catch (ParameterNotFoundException e) {
            throw new DataStoreException("Unsupported input");
        }

        if (!(desc instanceof ParameterDescriptor)) {
            throw new DataStoreException("Unsupported input");
        }

        try {
            final Object locationValue = sc.getStorageAs(((ParameterDescriptor)desc).getValueClass());
            final ParameterValueGroup params = getOpenParameters().createValue();
            params.parameter(LOCATION).setValue(locationValue);

            if (canProcess(params)) {
                return open(params);
            }
        } catch(IllegalArgumentException ex) {
            throw new DataStoreException("Unsupported input:" + ex.getMessage());
        }

        throw new DataStoreException("Unsupported input");
    }
}
