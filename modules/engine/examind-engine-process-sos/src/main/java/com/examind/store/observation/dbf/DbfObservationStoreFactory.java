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
package com.examind.store.observation.dbf;

import com.examind.store.observation.FileParsingObservationStoreFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import org.apache.sis.storage.base.Capability;
import org.apache.sis.storage.base.StoreMetadata;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.FeatureSet;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Samuel Andrés (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
@StoreMetadata(
        formatName = DbfObservationStoreFactory.NAME,
        capabilities = Capability.READ,
        resourceTypes = {FeatureSet.class})
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class DbfObservationStoreFactory extends FileParsingObservationStoreFactory {

    /** factory identification **/
    public static final String NAME = "observationDbfFile";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR
            = PARAM_BUILDER.addName(NAME).addName("ObservationDbfFileParameters").createGroup(IDENTIFIER, NAMESPACE, PATH,
                    MAIN_COLUMN, DATE_COLUMN, DATE_FORMAT, LONGITUDE_COLUMN, LATITUDE_COLUMN, OBS_PROP_COLUMN, OBS_PROP_ID, OBS_PROP_NAME, OBS_PROP_COLUMN_TYPE, OBS_PROP_DESC, FOI_COLUMN, OBSERVATION_TYPE,
                    PROCEDURE_ID, PROCEDURE_DESC, PROCEDURE_NAME, PROCEDURE_COLUMN, PROCEDURE_NAME_COLUMN, PROCEDURE_DESC_COLUMN, PROCEDURE_REGEX, PROCEDURE_PROPERTIES_MAP_COLUMN, PROCEDURE_PROPERTIES_COLUMN, Z_COLUMN, UOM_REGEX, UOM_ID, OBS_PROP_REGEX, FILE_MIME_TYPE, NO_HEADER,
                    DIRECT_COLUMN_INDEX, 
                    QUALITY_COLUMN, QUALITY_COLUMN_ID, QUALITY_COLUMN_TYPE, 
                    PARAMETER_COLUMN, PARAMETER_COLUMN_ID, PARAMETER_COLUMN_TYPE,
                    LAX_HEADER, COMPUTE_FOI);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public DbfObservationStore open(final ParameterValueGroup params) throws DataStoreException {
        try {
            return new DbfObservationStore(params);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "problem opening dbf store", ex);
            throw new DataStoreException(ex);
        }
    }

    @Override
    public Collection<String> getSuffix() {
        return Arrays.asList("dbf");
    }
}
