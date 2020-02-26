/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2015, Geomatys
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

package org.constellation.sos.io.generic;

import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.storage.DataStoreException;
import org.constellation.dto.service.config.generic.Automatic;
import org.geotoolkit.observation.AbstractObservationStoreFactory;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.createFixedIdentifier;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.StorageConnector;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@StoreMetadata(
        formatName = SOSGenericObservationStoreFactory.NAME,
        capabilities = {Capability.READ, Capability.CREATE, Capability.WRITE},
        resourceTypes = {})
@StoreMetadataExt(resourceTypes = ResourceType.SENSOR)
public class SOSGenericObservationStoreFactory extends AbstractObservationStoreFactory {

    /** factory identification **/
    public static final String NAME = "observationSOSGeneric";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    private static final ParameterBuilder BUILDER = new ParameterBuilder();
    /**
     * Parameter for database port
     */
    public static final ParameterDescriptor<Automatic> CONFIGURATION =
             BUILDER.addName("Configuration").setRemarks("Configuration").setRequired(true).create(Automatic.class,null);


    public static final ParameterDescriptor<String> PHENOMENON_ID_BASE =
             BUILDER.addName("phenomenon-id-base").setRemarks("phenomenon-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_TEMPLATE_ID_BASE =
             BUILDER.addName("observation-template-id-baseobservation-template-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> OBSERVATION_ID_BASE =
             BUILDER.addName("observation-id-base").setRemarks("observation-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptor<String> SENSOR_ID_BASE =
             BUILDER.addName("sensor-id-base").setRemarks("sensor-id-base").setRequired(false).create(String.class, null);

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR = BUILDER.addName(NAME).addName("SOSGenericParameters").setRequired(true)
            .createGroup(IDENTIFIER,CONFIGURATION, PHENOMENON_ID_BASE, OBSERVATION_TEMPLATE_ID_BASE, OBSERVATION_ID_BASE, SENSOR_ID_BASE);

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public SOSGenericObservationStore open(ParameterValueGroup params) throws DataStoreException {
        return new SOSGenericObservationStore(params);
    }

    @Override
    public ProbeResult probeContent(StorageConnector sc) throws DataStoreException {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector sc) throws DataStoreException {
        throw new DataStoreException("StorageConnector not supported.");
    }

}
