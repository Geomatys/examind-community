/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2014, Geomatys
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
package com.examind.store.observation.csvflat;

import com.examind.store.observation.FileParsingObservationStoreFactory;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@StoreMetadata(
        formatName = CsvFlatObservationStoreFactory.NAME,
        capabilities = Capability.READ)
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class CsvFlatObservationStoreFactory extends FileParsingObservationStoreFactory {

    /** factory identification **/
    public static final String NAME = "observationCsvFlatFile";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR
            = PARAM_BUILDER.addName(NAME).addName("ObservationCsvFlatFileParameters").createGroup(IDENTIFIER, NAMESPACE, CSVProvider.PATH, CSVProvider.SEPARATOR,
                    MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, FOI_COLUMN, OBSERVATION_TYPE,
                    PROCEDURE_ID, PROCEDURE_DESC, PROCEDURE_NAME, PROCEDURE_COLUMN, PROCEDURE_NAME_COLUMN, PROCEDURE_DESC_COLUMN, PROCEDURE_REGEX, Z_COLUMN, UOM_COLUMN, UOM_REGEX, UOM_ID, RESULT_COLUMN, OBS_PROP_COLUMN, OBS_PROP_ID,
                    OBS_PROP_NAME_COLUMN, OBS_PROP_FILTER_COLUMN, OBS_PROP_REGEX, OBS_PROP_NAME, TYPE_COLUMN, CHARQUOTE, FILE_MIME_TYPE, NO_HEADER, DIRECT_COLUMN_INDEX, QUALITY_COLUMN, 
                    QUALITY_COLUMN_ID, QUALITY_COLUMN_TYPE, LAX_HEADER, COMPUTE_FOI);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public CsvFlatObservationStore open(final ParameterValueGroup params) throws DataStoreException {

        try {
            return new CsvFlatObservationStore(params);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem opening csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Collection<String> getSuffix() {
        return Arrays.asList("csv", "xlsx", "xls", "tsv");
    }
}
