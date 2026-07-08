/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2026 Geomatys.
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
package com.examind.setup.data;

import com.examind.dto.fs.Service;
import static com.examind.setup.DatasourceUtilities.getOrCreateSQLDatasource;
import com.examind.setup.FileSystemAnalysis;
import com.examind.setup.FileSystemSetupBusiness;
import com.examind.setup.ProviderUtilities;
import static com.examind.setup.ProviderUtilities.createSourceProvider;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;

/**
 *
 * @author glegal
 */
public class OtherProviderHandler extends FSProviderHandler {
    
    public OtherProviderHandler(FileSystemAnalysis.ProviderWithPath pwp, Map<String, List<Service>> asyncInfos, FileSystemSetupBusiness parent) {
         super(pwp, asyncInfos, parent);
    }

    @Override
    public Integer createProviders(boolean diffMode) {
         try {
            Integer datasourceId = getOrCreateSQLDatasource(parent.datasourceBusiness, pwp.provider);

            // Create provider
            final Integer pid = createSourceProvider(pwp.provider, parent.providerBusiness, datasourceId);

            // Generate data.
            generateDatas(pid, pwp.provider, asyncInfos, true);
            
            return datasourceId;
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider file: " + pwp.ymlFile.getFileName().toString(), ex);
        }
        return null;
    }
}
