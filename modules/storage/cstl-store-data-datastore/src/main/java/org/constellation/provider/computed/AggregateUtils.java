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

import java.util.ArrayList;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.provider.CoverageData;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.constellation.repository.DataRepository;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.Mode;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregateUtils {

    public static Data<?> getData(DataRepository repo, int dataId) throws ConfigurationException {
        final org.constellation.dto.Data d = repo.findById(dataId);
        if (d == null) throw new TargetNotFoundException("No data found with id:" + dataId);
        final Data<?> dp = DataProviders.getProviderData(d.getProviderId(), d.getNamespace(), d.getName());
        if (dp == null) throw new TargetNotFoundException("No data found in provider named: {" + d.getNamespace() + "} " + d.getName());
        return dp;
    }

    /**
     * Aggregate the input coverage bands sharing the same index in the sample dimensions.
     *
     * @param dataIds Examin data identifiers.
     * @param mode Aggregation mode (ORDER or SCALE)
     * @param resultCrs Output crs of the created resource.
     *
     * @return An aggregated coverage resource.
     */
    public static AggregatedCoverageResource createFromDataIds(int[] dataIds, Mode mode, CoordinateReferenceSystem resultCrs) {
        try {
            if (dataIds == null || dataIds.length < 2) {
                throw new IllegalArgumentException("Not enough data given for aggregation. At least 2 resources required.");
            }
            final DataRepository repo = SpringHelper.getBean(DataRepository.class).orElseThrow(() -> new ConstellationException("No spring context available"));
            List<AggregatedCoverageResource.VirtualBand> bands = new ArrayList<>();
            for (int dataId : dataIds) {
                Data<?> d = getData(repo, dataId);
                if (d instanceof CoverageData cd) {
                    for (int i = 0; i < cd.getSampleDimensions().size(); i++) {
                        if (bands.size() <= i) {
                            bands.add(new AggregatedCoverageResource.VirtualBand());
                        }
                        AggregatedCoverageResource.VirtualBand vb = bands.get(i);
                        List<AggregatedCoverageResource.Source> sources = new ArrayList<>(vb.getSources());
                        sources.add(new AggregatedCoverageResource.Source(cd.getOrigin(), i));
                        vb.setSources(sources);
                    }
                } else {
                    throw new ConfigurationException("An coverage data was expected for aggregated coverage");
                }
            }
            return new AggregatedCoverageResource(bands, mode, resultCrs);
        } catch (ConstellationException | DataStoreException | TransformException e) {
            throw new RuntimeException(e);
        }
    }

    public static GridCoverageResource createFromResourceList(List<GridCoverageResource> coverages, Mode mode, CoordinateReferenceSystem resultCrs) {
        try {
            if (coverages == null || coverages.isEmpty()) {
                throw new IllegalArgumentException("No data given for aggregation. At least 1 resources required");
            } else if (coverages.size() == 1) {
                return coverages.get(0);
            }
            List<AggregatedCoverageResource.VirtualBand> bands = new ArrayList<>();
            for (GridCoverageResource gcr : coverages) {
                for (int i = 0; i < gcr.getSampleDimensions().size(); i++) {
                    if (bands.size() <= i) {
                        bands.add(new AggregatedCoverageResource.VirtualBand());
                    }
                    AggregatedCoverageResource.VirtualBand vb = bands.get(i);
                    List<AggregatedCoverageResource.Source> sources = new ArrayList<>(vb.getSources());
                    sources.add(new AggregatedCoverageResource.Source(gcr, i));
                    vb.setSources(sources);
                }
            }
            return new AggregatedCoverageResource(bands, mode, resultCrs);
        } catch (DataStoreException | TransformException e) {
            throw new RuntimeException(e);
        }
    }
}
