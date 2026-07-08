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
import static com.examind.setup.FileSystemUtilities.regexFileFilter;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.sis.util.ObjectConverters;
import org.constellation.api.ProviderType;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.Data;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Utility methods for provider configuration.
 * 
 * @author Guilhem Legal (Geomatys)
 */
public class ProviderUtilities {
    
    public static final String COVERAGE_SQL = "coverage-sql";
    public static final String COMPUTED_PROVIDER = "computed-resource";
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    private static class Config {
        public final ParameterValueGroup source;
        public final ParameterValueGroup config;
        
        public Config(ParameterValueGroup source, ParameterValueGroup config) {
            this.config = config;
            this.source = source;
        }
    }
    
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

    private static Config createProviderConfig(String factoryName, String providerIdentifier, String impl, Integer datasourceId, String datasourceParamName) throws ConstellationException {
        return createProviderConfig(factoryName, providerIdentifier, impl, datasourceId, datasourceParamName, null, Set.of());
    }
    
    private static Config createProviderConfig(String factoryName, String providerIdentifier, String impl, 
            Integer datasourceId, String datasourceParamName, 
            Map<String, String> parameters, Set<String> ignoredParameters) throws ConstellationException {
        
        final DataProviderFactory factory = DataProviders.getFactory(factoryName);
        if (factory == null) {
            throw new ConstellationException("Provider service not found: " + factoryName);
        }
        final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
        source.parameter("id").setValue(providerIdentifier);
        source.parameter("providerType").setValue(factoryName);
        
        final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
        final ParameterValueGroup config = choice.addGroup(impl);
        
        if (datasourceId != null) {
            config.parameter(datasourceParamName).setValue(datasourceId);
        }
        
        if (parameters != null) {
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                // skip some reserved or know parameter
                String key = param.getKey();
                if (ignoredParameters.contains(key)) continue;
                try {
                    ParameterValue<?> paramValue = config.parameter(param.getKey());
                    paramValue.setValue(ObjectConverters.convert(param.getValue(), paramValue.getDescriptor().getValueClass()));
                } catch (ParameterNotFoundException ex) {
                    LOGGER.log(Level.WARNING, "Erreur while setting advanced parameter " + param.getKey() + " on provider: " + providerIdentifier, ex);
                }
            }
        }
        return new Config(source, config);
    }
    
    public static Integer createSourceProvider(Provider conf, IProviderBusiness pBusiness, Integer datasourceId) throws ConstellationException {
        return createProvider(conf, pBusiness, datasourceId, null);
    }
    
    public static Integer createFileProvider(Provider conf, IProviderBusiness pBusiness, URI pathUri) throws ConstellationException {
        return createProvider(conf, pBusiness, null, pathUri);
    }
    
    private static Integer createProvider(Provider conf, IProviderBusiness pBusiness, Integer datasourceId, URI pathUri) throws ConstellationException {
        String providerIdentifier = conf.getGeneratedIdentifier();
        
        if (pBusiness.existIdentifier(providerIdentifier)) {
            throw new ConstellationException("Duplicated provider:" + providerIdentifier);
        }
        String factoryName = conf.getDataType();
        String impl = conf.getProviderType();
        Config config = createProviderConfig(factoryName, providerIdentifier, impl, datasourceId, "datasourceId", conf.getAdvancedParameters(), Set.of());
        String pathParamName = getPathParamName(conf);
        
        if (pathParamName != null) {
            config.config.parameter(pathParamName).setValue(pathUri);
        }
        return pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, factoryName, config.source);
    }
    
    public static Integer createComputedProvider(Provider conf, IProviderBusiness pBusiness, List<Data> datas) throws ConstellationException {
        String providerIdentifier = conf.getGeneratedIdentifier();
        String impl = conf.getProviderType();
        Config config = createProviderConfig(COMPUTED_PROVIDER, providerIdentifier, impl, null, null, conf.getAdvancedParameters(), Set.of());
                    
        GeneralParameterDescriptor genParamDesc = config.config.getDescriptor().descriptor("data_ids");
        if (genParamDesc instanceof ParameterDescriptor paramDesc) {
            for (Data brief : datas) {
                ParameterValue value = paramDesc.createValue();
                value.setValue(brief.getId());
                config.config.values().add(value);
            }
        }
        return pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, COMPUTED_PROVIDER, config.source);
    }
    
    public static Integer createCSQLProvider(IProviderBusiness pBusiness, String providerIdentifier, Integer datasourceId) throws ConstellationException {
        Config config = createProviderConfig("data-store", providerIdentifier, "exa-coverage-sql", datasourceId, "datasourceId");

        config.config.parameter("rootDirectory").setValue(Path.of("/"));
        return pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", config.source);
    }
    
    
    public static Integer createMetadataFSProvider(IProviderBusiness pBusiness, String serviceId, String dataDirectory) throws ConstellationException {
        final String providerIdentifier   = "csw-" + serviceId;
        final Config config = createProviderConfig("metadata-store", providerIdentifier, "FilesystemMetadata", null, null);
        
        config.config.parameter("folder").setValue(dataDirectory);
        config.config.parameter("store-id").setValue(providerIdentifier);

        int pid = pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "metadata-store", config.source);
        pBusiness.createOrUpdateData(pid, null, false, false, null);
        return pid;
    }
    
    public static Integer createSensorFSProvider(IProviderBusiness pBusiness, String serviceId, String path) throws ConstellationException {
        final String providerIdentifier   = "sensor-" + serviceId;
        final Config config = createProviderConfig("sensor-store", providerIdentifier, "filesensor", null, null);

        config.config.parameter("data_directory").setValue(path);

        return pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "sensor-store", config.source);
    }
    
    private final static Set<String> SKIPPED_OM = Set.of("om-implementation", "sn-implementation", "direct-provider", "create-data", "generate-from-existing", "sensor-metadata-path");
    
    public static Integer createOM2DatabaseProvider(IProviderBusiness pBusiness, String serviceId, Map<String, String> parameters, Integer datasourceId) {
        try {
            final String providerIdentifier   = "om-" + serviceId;
            String impl = parameters.getOrDefault("om-implementation", "observationSOSDatabase");
            
            final Config config = createProviderConfig("observation-store", providerIdentifier, impl, datasourceId, "datasource-id", parameters, SKIPPED_OM);
            
            // fixed for now TODO remove ? 
            if (impl.equals("observationSOSDatabase")) {
                addOMSpecificProperties(config);
            }
            
            return pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", config.source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    public static Integer createSensorDatabaseProvider(IProviderBusiness pBusiness, String serviceId, Map<String, String> parameters, Integer datasourceId) {
        try {
            final String providerIdentifier   = "sensor-" + serviceId;
            String impl = parameters.getOrDefault("sn-implementation", "om2sensor");
            
            final Config config = createProviderConfig("sensor-store", providerIdentifier, impl, datasourceId, "datasource-id", parameters, SKIPPED_OM);
            
            // fixed for now TODO remove ? 
            if (impl.equals("om2sensor")) {
                addOMSpecificProperties(config);
            }
            
            return pBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "sensor-store", config.source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    private static void addOMSpecificProperties(Config config) {
        config.config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
        config.config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
        config.config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
        config.config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
    }
    
    public static String getPathParamName(Provider providerConf) {
        if (providerConf.getSource() != null) return null;
        if ("coverage-xml-pyramid".equals(providerConf.getProviderType())) {
            return "path";
        // default case for file provider    
        } else if (providerConf.getLocation() != null) {
            return "location";
        }
        return null;
    }
    
    public static Predicate<Path> getProviderFileFilter(Provider providerConf) {
        String dirFilter = providerConf.getDirectoryFilter();
        final Pattern dirPattern = (dirFilter != null) ? Pattern.compile(dirFilter) : null;
        return dirPattern != null ? p -> regexFileFilter(p, dirPattern) : null;
    }
}
