/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package org.constellation.process.data;

import java.util.List;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IDatasourceBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.business.IStyleBusiness;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourceSelectedPath;
import org.constellation.dto.importdata.DatasourceAnalysisV3;
import org.constellation.dto.ProviderConfiguration;
import org.constellation.dto.importdata.ResourceStoreAnalysisV3;
import org.constellation.exception.ConstellationException;
import org.constellation.process.AbstractCstlProcess;
import static org.constellation.process.data.ImportDataSampleDescriptor.DATASOURCE_ID;
import static org.constellation.process.data.ImportDataSampleDescriptor.OUT_CONFIGURATION;
import static org.constellation.process.data.ImportDataSampleDescriptor.PROVIDER_CONFIGURATION;
import static org.constellation.process.data.ImportDataSampleDescriptor.SAMPLE_COUNT;
import static org.geotoolkit.parameter.Parameters.getOrCreate;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ImportDataSample extends AbstractCstlProcess {

    @Autowired
    public IDatasourceBusiness datasourceBusiness;

    @Autowired
    public IDataBusiness dataBusiness;

    @Autowired
    public IMetadataBusiness metadataBusiness;

    @Autowired
    public IStyleBusiness styleBusiness;

    public ImportDataSample(final ProcessDescriptor desc, final ParameterValueGroup parameter) {
        super(desc, parameter);
    }


    @Override
    protected void execute() throws ProcessException {

        final Integer datasourceId   = inputParameters.getValue(DATASOURCE_ID);
        final Integer sampleCount   = inputParameters.getValue(SAMPLE_COUNT);
        final ProviderConfiguration provConfig = inputParameters.getValue(PROVIDER_CONFIGURATION);


        final DataSource ds = datasourceBusiness.getDatasource(datasourceId);
        try {
             datasourceBusiness.recordSelectedPath(ds);
             List<DataSourceSelectedPath> paths = datasourceBusiness.getSelectedPath(ds, sampleCount);
             DatasourceAnalysisV3 outputDatas = new DatasourceAnalysisV3();
             for (DataSourceSelectedPath p : paths) {
                 ResourceStoreAnalysisV3 store = datasourceBusiness.treatDataPath(p, ds, provConfig, true, null);
                 outputDatas.getStores().add(store);
             }
             getOrCreate(OUT_CONFIGURATION, outputParameters).setValue(outputDatas);

         } catch (ConstellationException ex) {
             throw new ProcessException("Error while importing data", this, ex);
         }

    }
}
