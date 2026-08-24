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

import com.examind.dto.fs.Collection;
import com.examind.dto.fs.Service;
import com.examind.setup.FileSystemAnalysis;
import static com.examind.setup.ProviderUtilities.createComputedProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.constellation.dto.Data;

public class ComputedProviderHandler extends FSProviderHandler {
    
    public ComputedProviderHandler(FileSystemAnalysis.ProviderWithPath pwp, Map<String, List<Service>> asyncInfos) {
         super(pwp, asyncInfos);
    }

    @Override
    public Integer createProviders(boolean diffMode) {
       try {
            List<Data> datas = new ArrayList<>();
            for (Collection col : pwp.provider.getComputedData()) {
                datas.addAll(fsSetupBusiness.getDataFromCollection(col, asyncInfos != null));
            }
            // Create provider
            final Integer pid = createComputedProvider(pwp.provider, providerBusiness, datas);

            // Generate data.
            generateDatas(pid, pwp.provider, asyncInfos, true);
            
            // no file datasource for computed provider
            return -1;
       } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error while importing provider file: " + pwp.ymlFile.getFileName().toString(), ex);
        }
        return null;
    }

}
