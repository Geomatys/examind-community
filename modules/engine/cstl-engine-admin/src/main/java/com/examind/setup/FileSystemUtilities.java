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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.compress.utils.FileNameUtils;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class FileSystemUtilities {
    
    public static final ObjectMapper FS_MAPPER = new ObjectMapper(new YAMLFactory());
    
    public static final String COMPUTED_PROVIDER = "computed-resource";
    
    public static final List<String> DATA_CREATING_SERVICE = List.of("sts", "sos");
    
    private static final List<String> VECTOR_ALLOWED = List.of("wfs", "wms");
    private static final List<String> COVERAGE_ALLOWED = List.of("wcs", "wms");
    
    public static boolean isAllowedDataTypeForService(String serviceType, String dataType, String subDataType) {
        return switch (dataType.toLowerCase()) {
            case "vector"     -> VECTOR_ALLOWED.contains(serviceType.toLowerCase());
            case "coverage"   -> ("pyramid".equals(subDataType.toLowerCase()) && "wmts".equals(serviceType.toLowerCase())) ||
                                 (!"pyramid".equals(subDataType.toLowerCase()) && COVERAGE_ALLOWED.contains(serviceType.toLowerCase()));
                
            case "observation" -> ("VECTOR".equals(subDataType) && VECTOR_ALLOWED.contains(serviceType.toLowerCase())); 
            
            default -> false;
        };
    }
    
    public static boolean ymlFileFilter(Path path) {
        return fileFilter(path, List.of("yaml", "yml"));
    }
    
    public static boolean sldFileFilter(Path path) {
        return fileFilter(path, List.of("sld", "xml"));
    }
    
    public static boolean fileFilter(Path path, List<String> allowedExt) {
        return !Files.isDirectory(path) && allowedExt.contains(FileNameUtils.getExtension(path));
    }
    
    public static List<URI> listFiles(Path ymlFile, String location, final Pattern dirPattern) throws IOException {
        List<URI> files = new ArrayList<>();
        URI dataUri = getDataPath(ymlFile.getParent(), location);
        Path dataDir = Paths.get(dataUri);
        if (dirPattern != null && Files.isDirectory(dataDir)) {
            try (Stream<Path> stream = Files.walk(dataDir)) {
                files.addAll(
                    stream.filter(p -> regexFileFilter(p, dirPattern))
                          .map(p -> p.toUri())
                          .toList()
                );
            }
        } else {
            files.add(dataUri);
        }
        return files;
    }
    
    public static boolean regexFileFilter(Path path, Pattern dirPattern) {
        return !Files.isDirectory(path) && dirPattern.matcher(path.getFileName().toString()).matches();
    }
    
    public static URI getDataPath(Path parentDir, String dataStr) {
        URI uri;
        try {
            URI parsed = new URI(dataStr);
            if (parsed.getScheme() != null) {
                uri = parsed;
            } else {
                uri = parentDir.resolve(dataStr).normalize().toUri();
            }
        } catch (URISyntaxException e) {
            uri = parentDir.resolve(dataStr).toUri();
        }
        return uri;
    }
    
    public static boolean providerFileFilter(Path path, boolean computedResource) {
        if (!fileFilter(path, List.of("yaml", "yml"))) return false;
        try {
            Provider providerConf = FS_MAPPER.readValue(path.toFile(), Provider.class);
            return providerConf.getDataType()!= null && COMPUTED_PROVIDER.equals(providerConf.getDataType()) == computedResource;
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean serviceFileFilter(Path path, boolean creatingData) {
        if (!fileFilter(path, List.of("yaml", "yml"))) return false;
        try {
            Service serviceConf = FS_MAPPER.readValue(path.toFile(), Service.class);
            return serviceConf.getType() != null && DATA_CREATING_SERVICE.contains(serviceConf.getType()) == creatingData;
        } catch (Exception ex) {
            return false;
        }
    }
}