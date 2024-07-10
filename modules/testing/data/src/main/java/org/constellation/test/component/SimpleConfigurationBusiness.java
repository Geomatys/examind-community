/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2021 Geomatys.
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
package org.constellation.test.component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationRuntimeException;
import org.constellation.token.TokenUtils;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SimpleConfigurationBusiness implements IConfigurationBusiness {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.test.component");

    @Override
    public Path getConfigurationDirectory() {
        return ConfigDirectory.getConfigDirectory();
    }

    @Override
    public Path getDataDirectory() {
        return ConfigDirectory.getDataDirectory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getDataIntegratedDirectory(String providerId, boolean create) throws IOException {
        if (providerId == null) {
            return ConfigDirectory.getDataIntegratedDirectory();
        }
        return ConfigDirectory.getDataIntegratedDirectory(providerId, create);
    }

    @Override
    public boolean removeDataIntegratedDirectory(String providerId) {
        boolean result = false;
        try {
            final Path provDir = ConfigDirectory.getDataIntegratedDirectory(providerId, false);
            if (Files.exists(provDir)) {
                org.geotoolkit.nio.IOUtilities.deleteRecursively(provDir);
                result = true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during delete data on FS for provider {0}", providerId);
        }
        return result;
    }

    @Override
    public Path getUploadDirectory(String userName) throws IOException {
        return ConfigDirectory.getUploadDirectory(userName);
    }

    @Override
    public List<Path> getInstanceDirectories(String type) throws IOException {
        return ConfigDirectory.getInstanceDirectories(type);
    }

    @Override
    public Path getInstanceDirectory(String type, String id) {
        return ConfigDirectory.getInstanceDirectory(type, id);
    }

    @Override
    public Path getAssetsDirectory() {
        final Path folder = ConfigDirectory.getDataDirectory().resolve("assets");
        try {
            Files.createDirectories(folder);
        } catch (IOException ex) {
            throw new ConfigurationRuntimeException("Could not create: " + folder.toString(), ex);
        }
        return folder;
    }
    
    @Override
    public Path getProcessDirectory() {
        final Path folder = ConfigDirectory.getDataDirectory().resolve("process");
        try {
            Files.createDirectories(folder);
        } catch (IOException ex) {
            throw new ConfigurationRuntimeException("Could not create: " + folder.toString(), ex);
        }
        return folder;
    }
    
    @Override
    public void removeInstanceDirectory(String type, String id) {
        final Path instanceDir = ConfigDirectory.getInstanceDirectory(type, id);
        if (Files.isDirectory(instanceDir)) {
            //FIXME use deleteRecursively instead and handle exception
            IOUtilities.deleteSilently(instanceDir);
        }
    }

    @Override
    //FIXME RESTORE cleaning mechanism @Scheduled(fixedDelay=5*60*1000)
    public void cleanupFileSystem() {
        LOGGER.finer("Start filesystem cleanup");
        java.nio.file.Path uploadDirectory = ConfigDirectory.getUploadDirectory();

        DirectoryStream.Filter<Path> tokenExpiredFilter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return TokenUtils.isExpired(entry.getFileName().toString());
            }
        };

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDirectory, tokenExpiredFilter)) {
            for (Path path : stream) {
                LOGGER.log(Level.INFO, "{0} expired", path.getFileName());
                IOUtilities.deleteSilently(path);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during uploadDirectory browsing.", e);
        }
    }

    @Override
    public Object getProperty(final String key, final Object fallback, boolean allowSecure) {
        return null; // TODO ?
    }

    @Override
    @Transactional
    public void setProperty(final String key, final String value) {
        // TODO ?
    }

    @Override
    public Properties getMetadataTemplateProperties() {
        return ConfigDirectory.getMetadataTemplateProperties();
    }

    @Override
    public Map<String, Object> getProperties(boolean showSecure) {
        return new HashMap<>();
    }

    @Override
    public boolean allowedFilesystemAccess(String path) {
        return true;
    }
}
