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
package org.constellation.provider.computed;

import java.util.Arrays;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.util.collection.BackingStoreException;
import org.constellation.provider.DataProviderFactory;
import static org.geotoolkit.observation.AbstractObservationStoreFactory.createFixedIdentifier;

import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource;
import org.geotoolkit.utility.parameter.ExtendedParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageProviderDescriptor extends ComputedResourceProviderDescriptor {

    public static final String NAME = "AggregatedCoverageProvider";

    public static final ParameterDescriptor<String> IDENTIFIER = createFixedIdentifier(NAME);

    public static final ParameterDescriptor<String> RESULT_CRS;

    public static final ParameterDescriptor<String> MODE;

    public static final ParameterDescriptor<Short> BAND_INDEX;
    public static final ParameterDescriptor<GridCoverageResource> SOURCE_DATA;
    public static final ParameterDescriptor<Integer> SOURCE_DATA_ID;
    public static final ParameterDescriptorGroup SOURCE_BAND;
    public static final ParameterDescriptorGroup VIRTUAL_BAND;
    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR;

    private static final short MAX_NUMBER_OF_VIRTUAL_BANDS = (short) 1000;

    public static final ParameterDescriptor<Integer> DATA_IDS =
            new ExtendedParameterDescriptor<>("data_ids", "data identifiers", 0, Integer.MAX_VALUE, Integer.class, null, null, null);

    static {
        final ParameterBuilder builder = new ParameterBuilder();

        builder.setRequired(true);

        BAND_INDEX = builder.addName("bandIndex")
                .setDescription("Index of the band to use or to create")
                .createBounded(Short.class, (short) 0, MAX_NUMBER_OF_VIRTUAL_BANDS, (short) 0);

        SOURCE_DATA = builder.addName("sourceData")
                .setDescription("A source coverage data to aggregate in the result")
                .create(GridCoverageResource.class, null);

        SOURCE_DATA_ID = builder.addName("sourceDataId")
                .setDescription("Identifier of a source coverage data to aggregate in the result")
                .create(Integer.class, null);

        SOURCE_BAND = builder.addName("sourceBand")
                .setDescription("Pick a band in a source data. Define a source band to be aggregated in target virtual band")
                .createGroup(1, Integer.MAX_VALUE, SOURCE_DATA_ID, BAND_INDEX);

        builder.setRequired(false);

        VIRTUAL_BAND = builder.addName("virtualBand")
                .setDescription("Definition of a virtual band. Virtual bands are used as replacement for a list of data. It acts as an advanced configuration way. You must not specify both data list and virtual bands.")
                .createGroup(0, MAX_NUMBER_OF_VIRTUAL_BANDS, BAND_INDEX, SOURCE_BAND);

        RESULT_CRS = builder.addName("ResultCRS")
                .setRemarks("Result CRS: CRS to use for aggregation result. Allow user to force reprojection of source datasets in a user space, instead of trying to guess automatically a good common space")
                .create(String.class,null);

        final String[] modes = Arrays.stream(AggregatedCoverageResource.Mode.values())
                .map(mode -> mode.name())
                .toArray(String[]::new);
        MODE = builder.addName("mode")
                .setRemarks("mode")
                .createEnumerated(String.class, modes, AggregatedCoverageResource.Mode.ORDER.name());

        builder.setRequired(true);

        PARAMETERS_DESCRIPTOR = builder.addName(NAME)
                .createGroup(IDENTIFIER, DATA_NAME, DATA_IDS, VIRTUAL_BAND, RESULT_CRS, MODE);
    }


    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public ComputedResourceProvider buildProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        try {
            return new AggregatedCoverageProvider(providerId, service, param);
        } catch (FactoryException e) {
            throw new BackingStoreException("Cannot initialize aggregated resource for provider "+providerId, e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}
