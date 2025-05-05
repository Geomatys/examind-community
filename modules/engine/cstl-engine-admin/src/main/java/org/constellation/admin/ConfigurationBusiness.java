package org.constellation.admin;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.sis.metadata.iso.DefaultMetadata;
import org.constellation.admin.util.MetadataUtilities;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConstellationException;
import org.constellation.repository.PropertyRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.token.TokenUtils;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("configurationBusiness")
@Primary
public class ConfigurationBusiness implements IConfigurationBusiness {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private IMetadataBusiness metadatabusiness;
    
    @Autowired
    private Environment env;

    public void init() {
        LOGGER.info("=== Configure directory ===");
        
        // for filesystem configuration mode we don't create all the directories.
        boolean fsMode = false;
        for (String ap : env.getActiveProfiles()) {
            if ("fsconfig".equals(ap)) {
                fsMode = true;
                break;
            }
        }
        ConfigDirectory.init(fsMode);
    }

    @Override
    public Path getConfigurationDirectory() {
        return ConfigDirectory.getConfigDirectory();
    }

    @Override
    public Path getDataDirectory() {
        return ConfigDirectory.getDataDirectory();
    }
    
    @Override
    public Path getProvidersDirectory() {
        return ConfigDirectory.getProvidersDirectory();
    }
    
    @Override
    public Path getStylesDirectory() {
        return ConfigDirectory.getStylesDirectory();
    }

    @Override
    public Path getAssetsDirectory() {
        return ConfigDirectory.getAssetsDirectory();
    }
    
    @Override
    public Path getProcessDirectory() {
        return ConfigDirectory.getProcessDirectory();
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
        if (providerId == null || providerId.isEmpty()) {
            throw new IllegalArgumentException("ProviderId must not be null or empty");
        }
        try {
            Path provDir = ConfigDirectory.getDataIntegratedDirectory(providerId, false);
            Path baseDir = ConfigDirectory.getDataIntegratedDirectory();
            if (provDir == null || baseDir == null) {
               return false; 
            } else {
                provDir = provDir.normalize();
                baseDir = baseDir.normalize();
            }
            // Security: if given "." or ".." or any fragment allowing to resolve directory upstream, launch an error to prevent data corruption
            if (!provDir.startsWith(baseDir) || baseDir.startsWith(provDir)) {
                throw new IllegalArgumentException("Given provider ID is invalid");
            }
            if (Files.exists(provDir)) {
                org.geotoolkit.nio.IOUtilities.deleteRecursively(provDir);
                result = true;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during delete data on FS for provider: {0}", providerId);
        }
        return result;
    }

    @Override
    public Path getUploadDirectory(String userName) throws IOException {
        return ConfigDirectory.getUploadDirectory(userName);
    }

    @Override
    public Path getInstanceDirectory(String type, String id) {
        return ConfigDirectory.getInstanceDirectory(type, id, true);
    }

    @Override
    public Path getServicesDirectory() {
        return ConfigDirectory.getServicesDirectory();
    }
    
    @Override
    public void removeInstanceDirectory(String type, String id) {
        final Path instanceDir = ConfigDirectory.getInstanceDirectory(type, id, false);
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
    public Map<String, Object> getProperties(boolean showSecure) {
        Map<String, Object> results = new HashMap<>();
        for (AppProperty prop : AppProperty.values()) {
            Object value = getProperty(prop.getKey(), null, showSecure);
            if (value != null) results.put(prop.getKey(), value);
        }
        return results;
    }

    @Override
    public Object getProperty(final String key, final Object fallback, boolean showSecure) {
        // to allow the update of properties, we first look into database for override.
        Object value = propertyRepository.getValue(key, null);

        // if not fallback on
        if (value == null) {
            value = Application.getObjectProperty(key, fallback);
        }

        // look for secure properties
        AppProperty prop = AppProperty.fromKey(key);
        if (value != null && prop != null && (!showSecure && prop.isSecure())) {
            return "<protected>";
        }
        return value;
    }

    /**
     * TODO
     * there is probably some properties that can not be update and some that needs reload of some stuff
     * not really ready just yet
     *
     * @param key
     * @param value
     */
    @Override
    @Transactional
    public void setProperty(final String key, final String value) {
        propertyRepository.update(key, value);
        // update metadata when service URL key is updated
        if (AppProperty.CSTL_SERVICE_URL.getKey().equals(key)) {
            updateServiceUrlForMetadata(value);
        }
    }

    private void updateServiceUrlForMetadata(final String url) {
        try {
            final List<Service> records = serviceRepository.findAll();
            for (Service record : records) {
                final Object metadata = metadatabusiness.getIsoMetadataForService(record.getId());
                if (metadata instanceof DefaultMetadata) {
                    final DefaultMetadata servMeta = (DefaultMetadata) metadata;
                    MetadataUtilities.updateServiceMetadataURL(record.getIdentifier(), record.getType(), url, servMeta);
                    metadatabusiness.updateMetadata(servMeta.getFileIdentifier(), servMeta, null, null, null, null, null, "DOC");
                } else {
                    LOGGER.info("Service metadata is not a ISO 19139 object");
                }
            }
        } catch (ConstellationException ex) {
            LOGGER.log(Level.WARNING, "An error occurred updating service URL", ex);
        }
    }

    @Override
    public Properties getMetadataTemplateProperties() {
        return ConfigDirectory.getMetadataTemplateProperties();
    }

    @Override
    public boolean allowedFilesystemAccess(String path) {
        Path p;
        try {
            p = Paths.get(new URI(path)).normalize();
        } catch (URISyntaxException ex) {
            LOGGER.log(Level.WARNING, "Invalid URI syntax for path:" + path, ex);
            return false;
        }
        List<String> allowedPaths = Application.getListProperty(AppProperty.EXA_ALLOWED_FS_PATH);
        // if not set, the entire filesystem is available
        if (allowedPaths.isEmpty()) {
            allowedPaths.add("file:///");
        }
        // CSTL_HOME id always allowed
        allowedPaths.add(ConfigDirectory.getConfigDirectory().toUri().toString());

        for (String allowedPath : allowedPaths) {
            if (!allowedPath.startsWith("file://")) {
                allowedPath = "file://" + allowedPath;
            }
            try {
                Path ap = Paths.get(new URI(allowedPath)).normalize();
                if (p.startsWith(ap)) {
                    return true;
                }
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.WARNING, "Invalid URI syntax for allowed path:" + allowedPath, ex);
            }
        }
        return false;
    }

}
