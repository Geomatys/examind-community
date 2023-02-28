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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.BackingStoreException;
import org.apache.sis.util.iso.Names;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DefaultCoverageData;
import static org.constellation.provider.computed.AggregateUtils.getData;

import org.constellation.repository.DataRepository;
import org.geotoolkit.referencing.ReferencingUtilities;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.Mode;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.Source;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.VirtualBand;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import static org.constellation.provider.computed.AggregatedCoverageProviderDescriptor.*;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageProvider extends ComputedResourceProvider {

    private final CoordinateReferenceSystem resultCrs;
    private final String resultCRSName;
    private final Mode mode;
    private final List<ParameterValueGroup> vBands;
    private final int[] dataIds;

    public AggregatedCoverageProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) throws FactoryException {
        super(providerId, service, param);
        final Parameters input = Parameters.castOrWrap(param);
        mode = Mode.valueOf(input.getValue(MODE));
        vBands = input.groups(VIRTUAL_BAND.getName().toString());
        dataIds = input.values().stream()
                .filter(it -> DATA_IDS.equals(it.getDescriptor()))
                .map(it -> (ParameterValue<Integer>) it)
                .filter(it -> it.getValue() != null)
                .mapToInt(it -> it.getValue())
                .toArray();

        final String crsParam = input.getValue(RESULT_CRS);
        resultCrs = crsParam == null ? null : decodeCrs(crsParam);
        resultCRSName = resultCrs == null ? null : ReferencingUtilities.lookupIdentifier(resultCrs, true);
    }

    @Override
    public String getCRSName() {
        return resultCRSName;
    }

    @Override
    protected synchronized  Data getComputedData() {
        if (cachedData == null) {
            try {
                final AggregatedCoverageResource res = createFromVirtualBands()
                        .orElseGet(this::createFromDataIds);

                final String aggregationName = getDataName().orElse("Aggregation");
                cachedData = new DefaultCoverageData(Names.createLocalName(null, ":", aggregationName), res, null);
            } catch (Exception ex){
                LOGGER.log(Level.WARNING, id, ex);
            }
        }
        return cachedData;
    }

    private AggregatedCoverageResource createFromDataIds() {
        return AggregateUtils.createFromDataIds(dataIds, mode, resultCrs);
    }

    private Optional<AggregatedCoverageResource> createFromVirtualBands() throws DataStoreException, TransformException {
        if (vBands == null || vBands.isEmpty()) return Optional.empty();
        final List<VirtualBand> bands = toVirtualBands(vBands);
        return Optional.of(new AggregatedCoverageResource(bands, mode, resultCrs));
    }

    private List<VirtualBand> toVirtualBands(final List<ParameterValueGroup> vBands) throws DataStoreException {
        final VirtualBand[] bands = new VirtualBand[vBands.size()];
        final DataRepository repo = SpringHelper.getBean(DataRepository.class)
                                                .orElseThrow(() -> new DataStoreException("No spring context available"));
        for (ParameterValueGroup group : vBands) {
            final Parameters band = Parameters.castOrWrap(group);
            final short bandIndex = band.getMandatoryValue(BAND_INDEX);
            ArgumentChecks.ensureBetween("Band index", 0, bands.length - 1, bandIndex);
            if (bands[bandIndex] != null) throw new IllegalArgumentException("Duplicate band configuration for band index: "+bandIndex);
            final VirtualBand vBand = new VirtualBand();
            bands[bandIndex] = vBand;
            final List<Source> sources = group.groups(SOURCE_BAND.getName().toString()).stream()
                    .map(sourceGroup -> toSource(sourceGroup, repo))
                    .collect(Collectors.toList());
            vBand.setSources(sources);
        }

        for (int i = 0 ; i < bands.length ; i++) {
            if (bands[i] == null) throw new IllegalArgumentException("Missing virtual band definition for index: "+i);
        }

        return Arrays.asList(bands);
    }

    private Source toSource(ParameterValueGroup parameterValueGroup, final DataRepository repo) {
        final Parameters sourceParam = Parameters.castOrWrap(parameterValueGroup);
        final int dataId = sourceParam.getMandatoryValue(SOURCE_DATA_ID);
        final int bandId = sourceParam.getMandatoryValue(BAND_INDEX);
        final Data<?> sourceData;
        try {
            sourceData = getData(repo, dataId);
        } catch (ConfigurationException e) {
            throw new BackingStoreException(e);
        }
        final Resource sourceCoverage = sourceData.getOrigin();
        if (!(sourceCoverage instanceof GridCoverageResource)) throw new RuntimeException("Invalid source data: is not a coverage");
        // TODO: allow to add a transfer function
        return new Source((GridCoverageResource) sourceCoverage, bandId);
    }

    private static CoordinateReferenceSystem decodeCrs(final String crsText) throws FactoryException {
        try {
            return CRS.forCode(crsText);
        } catch (RuntimeException | FactoryException e) {
            try {
                return CRS.fromWKT(crsText);
            } catch (Exception bis) {
                e.addSuppressed(bis);
                throw e;
            }
        }
    }
}
