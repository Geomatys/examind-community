/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com
 *
 * Copyright 2025 Geomatys.
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
import com.examind.dto.fs.Service;
import static com.examind.setup.ProviderUtilities.COMPUTED_PROVIDER;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.compress.utils.FileNameUtils;
import org.constellation.dto.service.config.wps.ProcessFactory;
import org.constellation.dto.service.config.wps.Processes;

/**
 * File system related utility methods.
 * 
 * @author Guilhem Legal (Geomatys)
 */
final class FileSystemUtilities {
    
    private static final Logger LOGGER = Logger.getLogger("com.examind.setup");
    
    private static final ObjectMapper FS_MAPPER = new ObjectMapper(new YAMLFactory());
    
    /**
     * Service that may generate data instanciated.
     * For example STA / SOS can generate sensor/foi vector data of their published sensor.
     */
    private static final List<String> DATA_CREATING_SERVICE = List.of("sts", "sos");
    
    /**
     * service that allow vector data.
     */
    private static final List<String> VECTOR_ALLOWED        = List.of("wfs", "wms");

    private static final List<String> COVERAGE_PYRAMID_ONLY = List.of("wmts", "tms");

    /**
     * Service that allow coverage data.
     */
    private static final List<String> COVERAGE_ALLOWED      = List.of("wcs", "wms", "tms");
    
    /**
     * Return true if the specified data type/subtype can be published on this service type.
     * 
     * @param serviceType The service type.
     * @param dataType The data main type.
     * @param subDataType The data sub type.
     * @return {@code true} if the specified data type/subtype can be published on this service type.
     */
    static boolean isAllowedDataTypeForService(String serviceType, String dataType, String subDataType) {
        serviceType = serviceType.toLowerCase(Locale.ROOT);
        return switch (dataType.toLowerCase()) {
            case "vector"     -> VECTOR_ALLOWED.contains(serviceType);
            case "coverage"   -> COVERAGE_ALLOWED.contains(serviceType)
                                 && (!COVERAGE_PYRAMID_ONLY.contains(serviceType) || "pyramid".equalsIgnoreCase(subDataType));
            case "observation" -> ("VECTOR".equals(subDataType) && VECTOR_ALLOWED.contains(serviceType));
            
            default -> false;
        };
    }
    
    /**
     * file filter on SLD file (based on extension).
     * 
     * @param path A file.
     */
    static boolean sldFileFilter(Path path) {
        return fileFilter(path, List.of("sld", "xml"));
    }
    
     /**
     * file filter based on extension.
     * 
     * @param path A file.
     */
    static boolean fileFilter(Path path, List<String> allowedExt) {
        return !Files.isDirectory(path) && allowedExt.contains(FileNameUtils.getExtension(path));
    }
    
     /**
     * file filter based on a regex against the file name.
     * 
     * @param path A file.
     */
    static boolean regexFileFilter(Path path, Pattern dirPattern) {
        return !Files.isDirectory(path) && dirPattern.matcher(path.getFileName().toString()).matches();
    }

    /**
     * File filter based on yaml extension and its content for regular provider.
     * 
     * @param path A file.
     */
    static boolean regularProviderFileFilter(Path path) {
        return providerFileFilter(path, Boolean.FALSE);
    }
    
     /**
     * File filter based on yaml extension and its content for computed provider.
     * 
     * @param path A file.
     */
    static boolean computedProviderFileFilter(Path path) {
        return providerFileFilter(path, Boolean.TRUE);
    }
    
     /**
     * File filter based on yaml extension and its content.
     * 
     * @param path A file.
     * @param computedResource a flag to determine if the provider file contains or not a computed provider.
     */
    static boolean providerFileFilter(Path path, boolean computedResource) {
        if (!fileFilter(path, List.of("yaml", "yml"))) return false;
        try {
            Provider providerConf = FS_MAPPER.readValue(path.toFile(), Provider.class);
            return providerConf.getDataType()!= null && COMPUTED_PROVIDER.equals(providerConf.getDataType()) == computedResource;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, path.getFileName() + "Invalid provider file:");
            LOGGER.log(Level.FINE, "Invalid provider file", ex);
            return false;
        }
    }
    
    /**
     * File filter based on yaml extension and its content for sevice generating data.
     * 
     * @param path A file.
     */
    static boolean serviceWithDataFileFilter(Path path) {
        return serviceFileFilter(path, Boolean.TRUE);
    }
    
    /**
     * File filter based on yaml extension and its content for regular service.
     * 
     * @param path A file.
     */
    static boolean serviceNoDataFileFilter(Path path) {
        return serviceFileFilter(path, Boolean.FALSE);
    }
    
    /**
     *  file filter based on yaml extension and its content.
     * 
     * @param path A file.
     * @param creatingData a flag to determine if the service file contains or not a service that may create data (can be {@code null}).
     */
    static boolean serviceFileFilter(Path path, Boolean creatingData) {
        if (!fileFilter(path, List.of("yaml", "yml"))) return false;
        try {
            Service serviceConf = FS_MAPPER.readValue(path.toFile(), Service.class);
            return serviceConf.getType() != null &&  (creatingData == null || DATA_CREATING_SERVICE.contains(serviceConf.getType()) == creatingData);
        } catch (Exception ex) {
            return false;
        }
    }

    static boolean isDataCreatingService(Service svc) {
        return svc != null && svc.getType() != null && DATA_CREATING_SERVICE.contains(svc.getType());
    }
    
    /**
     * Transform a WPS Service configuration into the examind model wps configuration.
     * 
     * @param instance A service configuration.
     */
    static Processes toWPSConfig(Service instance) {
        List<ProcessFactory> factories = new ArrayList<>();
        for (com.examind.dto.fs.ProcessFactory factory : instance.getProcessFactories()) {
            ProcessFactory processFactory;
            if (factory.getProcess().isEmpty()) {
                processFactory = new ProcessFactory(factory.getAuthority(), Boolean.TRUE);
            } else {
                processFactory = new ProcessFactory(factory.getAuthority(), Boolean.FALSE);
                for (String pr : factory.getProcess()) {
                    processFactory.getInclude().add(new org.constellation.dto.service.config.wps.Process(pr));
                }
            }
            factories.add(processFactory);
        }
        return new Processes(false, factories);
    }
    
    /**
     * Parse a Yaml configuration file.
     * Return null if the file can not be read because it is not valid or not accessible.
     * 
     * @param <A> Expected mapping type class.
     * @param path A file path.
     * @param type Expected mapping type.
     * @return An instance of type or {@code null}.
     */
    static <A> A parseYaml(Path path, Class<A> type) {
        try {
            return FS_MAPPER.readValue(path.toFile(), type);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Error while reading yaml file: " + path.toString(), ex);
        }
        return null;
    }
}