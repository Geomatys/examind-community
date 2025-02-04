/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2025 Geomatys.
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
package com.examind.store.observation.csvsplitted;

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
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import org.apache.sis.io.stream.IOUtilities;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;

/**
 *
 * @author Samuel Andrés (Geomatys)
 */
@StoreMetadata(
        formatName = CsvSplittedObservationStoreFactory.NAME,
        capabilities = Capability.READ)
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class CsvSplittedObservationStoreFactory extends FileParsingObservationStoreFactory {

    /** factory identification **/
    public static final String NAME = "observationCsvSplittedFile";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR
            = PARAM_BUILDER.addName(NAME).addName("ObservationCsvSplittedFileParameters").createGroup(IDENTIFIER, NAMESPACE, CSVProvider.PATH, CSVProvider.SEPARATOR,
                    MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, FOI_COLUMN, OBSERVATION_TYPE,
                    PROCEDURE_ID, PROCEDURE_DESC, PROCEDURE_NAME, PROCEDURE_COLUMN, PROCEDURE_NAME_COLUMN, PROCEDURE_DESC_COLUMN, PROCEDURE_REGEX, PROCEDURE_PROPERTIES_MAP_COLUMN, PROCEDURE_PROPERTIES_COLUMN, Z_COLUMN, UOM_COLUMN, UOM_REGEX, UOM_ID, RESULT_COLUMN, OBS_PROP_COLUMN, OBS_PROP_ID,
                    OBS_PROP_NAME_COLUMN, OBS_PROP_FILTER_COLUMN, OBS_PROP_REGEX, OBS_PROP_NAME, OBS_PROP_DESC, OBS_PROP_DESC_COLUMN, OBS_PROP_PROPERTIES_MAP_COLUMN, OBS_PROP_PROPERTIES_COLUMN, TYPE_COLUMN, CHARQUOTE, FILE_MIME_TYPE, NO_HEADER, DIRECT_COLUMN_INDEX, QUALITY_COLUMN, 
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
    public CsvSplittedObservationStore open(final ParameterValueGroup params) throws DataStoreException {

        try {
            return new CsvSplittedObservationStore(params);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem opening csv file", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Collection<String> getSuffix() {
        return Arrays.asList("csv");
    }
    
    @Override
    public ProbeResult probeContent(StorageConnector connector) throws DataStoreException {
        final Path path = connector.getStorageAs(Path.class);
        if (path != null && path.getFileName().toString().equals("observations.csv")) return new ProbeResult(true, "text/csv; subtype=\"om\"", null);
        return ProbeResult.UNSUPPORTED_STORAGE;
    }
}
