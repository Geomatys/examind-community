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
import java.util.logging.Level;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStoreException;
import org.constellation.exception.ConfigurationException;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DefaultCoverageData;
import static org.constellation.provider.computed.AggregatedCoverageProviderDescriptor.MODE;
import static org.constellation.provider.computed.AggregatedCoverageProviderDescriptor.RESULT_CRS;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.Mode;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.Source;
import org.geotoolkit.storage.coverage.mosaic.AggregatedCoverageResource.VirtualBand;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class AggregatedCoverageProvider extends ComputedResourceProvider {

    private final String resultCRSName;
    private final String mode;

    public AggregatedCoverageProvider(String providerId, DataProviderFactory service, ParameterValueGroup param) {
        super(providerId, service, param);
        resultCRSName = (String) param.parameter(RESULT_CRS.getName().getCode()).getValue();
        mode = (String) param.parameter(MODE.getName().getCode()).getValue();
    }

    @Override
    public String getCRSName() {
        return resultCRSName;
    }

    @Override
    protected Data getComputedData() {
        try {
            List<Data> datas = getResourceList();
            CoordinateReferenceSystem resultCrs = CRS.forCode(resultCRSName);
            List<VirtualBand> bands = new ArrayList<>();
            for (Data d : datas) {
                if (d instanceof DefaultCoverageData)  {
                    DefaultCoverageData cd = (DefaultCoverageData)d;
                    VirtualBand v = new VirtualBand();
                    v.setSources(new Source(cd.getOrigin(), 0));
                    bands.add(v);
                } else {
                    throw new ConfigurationException("An coverage data was expected for aggregated coverage");
                }
            }
            Mode modee = Mode.valueOf(mode);
            AggregatedCoverageResource res = new AggregatedCoverageResource(bands, modee, resultCrs);
            return new DefaultCoverageData(dataName, res, null);
        } catch (ConfigurationException | DataStoreException | TransformException | FactoryException ex){
            LOGGER.log(Level.WARNING, id, ex);
        }
        return null;
    }
}
