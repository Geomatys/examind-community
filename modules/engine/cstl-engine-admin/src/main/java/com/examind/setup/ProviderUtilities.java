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

package com.examind.setup;

import com.examind.dto.fs.Provider;
import org.constellation.exception.ConfigurationException;

/**
 * Utility methods for provider configuration.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class ProviderUtilities {
    
    public static final String COVERAGE_SQL = "coverage-sql";
    public static final String COMPUTED_PROVIDER = "computed-resource";
    
    /**
     * Category of providers.
     * - Computed is for data that are made of other data.
     * - Coverage SQL data are a special mode where multiple files are grouped together with an SQL schemas that store metadata.
     * - File store data are provider pointing to a single (and eventually other complementary files) like shapefile, tiff
     * - Other store. For now we put in there SQL feature but maybe other will fall into this category.
     */
    public enum ProviderSourceType {
        COMPUTED,
        CSQL,
        OTHER,
        FILE
    }
    
    /**
     * Return a provider category by looking at its confguration parameters.
     * 
     * @param conf A provider configuration.
     * @return 
     */
    public static ProviderSourceType getProviderSourceType(Provider conf) {
        if (COVERAGE_SQL.equals(conf.getProviderType()))  return ProviderSourceType.CSQL;
        if (COMPUTED_PROVIDER.equals(conf.getDataType())) return ProviderSourceType.COMPUTED;
        
        if (conf.getSource() != null && conf.getLocation() == null) return  ProviderSourceType.OTHER;
        
        return ProviderSourceType.FILE; 
    }

    
    /**
     * Look for mandatory parameters in a provider yaml file, dependending on its found category.
     * 
     * @param provider A provider configuration.
     * @throws ConfigurationException If a mandatory parameter is missing
     */
    public static void validateProviderFile(Provider provider) throws ConfigurationException {
        if (provider.getProviderType() == null) throw new ConfigurationException("Provider type is missing.");
        if (provider.getDataset()      == null) throw new ConfigurationException("Dataset is missing.");
        
        ProviderSourceType type = getProviderSourceType(provider);
        switch(type) {
            case CSQL -> {
                if (provider.getSource()   == null) throw new ConfigurationException("Source is missing for coverage sql provider.");
                if (provider.getLocation() == null) throw new ConfigurationException("Location is missing for coverage sql provider.");
            }
            case COMPUTED -> {
                if (provider.getComputedData() == null || provider.getComputedData().isEmpty()) {
                    throw new ConfigurationException("computed data is missing for computed provider.");
                }
            }
            case FILE -> {
                if (provider.getLocation() == null) throw new ConfigurationException("Location is missing for file provider.");
            }
            case OTHER -> {}
        }
    }

}
