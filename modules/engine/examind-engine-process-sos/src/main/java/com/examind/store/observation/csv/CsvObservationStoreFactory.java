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
package com.examind.store.observation.csv;

import com.examind.store.observation.FileParsingObservationStoreFactory;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.csv.CSVProvider;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@StoreMetadata(
        formatName = CsvObservationStoreFactory.NAME,
        capabilities = Capability.READ)
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class CsvObservationStoreFactory extends FileParsingObservationStoreFactory {

    /** factory identification **/
    public static final String NAME = "observationCsvFile";

    public static final String MIME_TYPE = "text/csv; subtype=\"om\"";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR
            = PARAM_BUILDER.addName(NAME).addName("ObservationCsvFileParameters").createGroup(IDENTIFIER, NAMESPACE, CSVProvider.PATH, CSVProvider.SEPARATOR,
                    MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, OBS_PROP_COLUMN, FOI_COLUMN, OBSERVATION_TYPE, PROCEDURE_ID,
                    PROCEDURE_COLUMN, PROCEDURE_NAME_COLUMN, PROCEDURE_DESC_COLUMN, Z_COLUMN, UOM_REGEX, CHARQUOTE, OBS_PROP_REGEX);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public CsvObservationStore open(final ParameterValueGroup params) throws DataStoreException {

        final URI uri = (URI) params.parameter(CSVProvider.PATH.getName().toString()).getValue();
        final char separator = (Character) params.parameter(CSVProvider.SEPARATOR.getName().toString()).getValue();
        final String quotecharString = (String) params.parameter(CHARQUOTE.getName().toString()).getValue();
        char quotechar = 0;
        if (quotecharString != null) {
            quotechar = quotecharString.charAt(0);
        }
        final String mainColumn = (String) params.parameter(MAIN_COLUMN.getName().toString()).getValue();
        final String dateColumn = (String) params.parameter(DATE_COLUMN.getName().toString()).getValue();
        final String dateFormat = (String) params.parameter(DATE_FORMAT.getName().toString()).getValue();
        final String longitudeColumn = (String) params.parameter(LONGITUDE_COLUMN.getName().toString()).getValue();
        final String latitudeColumn = (String) params.parameter(LATITUDE_COLUMN.getName().toString()).getValue();
        final String foiColumn = (String) params.parameter(FOI_COLUMN.getName().toString()).getValue();
        final String procedureId = (String) params.parameter(PROCEDURE_ID.getName().toString()).getValue();
        final String procedureColumn = (String) params.parameter(PROCEDURE_COLUMN.getName().toString()).getValue();
        final String procedureNameColumn = (String) params.parameter(PROCEDURE_NAME_COLUMN.getName().toString()).getValue();
        final String procedureDescColumn = (String) params.parameter(PROCEDURE_DESC_COLUMN.getName().toString()).getValue();
        final String zColumn = (String) params.parameter(Z_COLUMN.getName().toString()).getValue();
        final String observationType = (String) params.parameter(OBSERVATION_TYPE.getName().toString()).getValue();
        final String uomRegex = (String) params.parameter(UOM_REGEX.getName().toString()).getValue();
        final Set<String> obsPropColumns = getMultipleValues(params, OBS_PROP_COLUMN.getName().getCode());
        final String obsPropRegex = (String) params.parameter(OBS_PROP_REGEX.getName().toString()).getValue();
        try {
            return new CsvObservationStore(Paths.get(uri),
                    separator, quotechar, readType(uri, separator, quotechar, dateColumn, longitudeColumn, latitudeColumn, obsPropColumns),
                    mainColumn, dateColumn, dateFormat, longitudeColumn, latitudeColumn, obsPropColumns, observationType,
                    foiColumn, procedureId, procedureColumn, procedureNameColumn, procedureDescColumn, zColumn, uomRegex, obsPropRegex);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem opening csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Collection<String> getSuffix() {
        return Arrays.asList("csv");
    }
}
