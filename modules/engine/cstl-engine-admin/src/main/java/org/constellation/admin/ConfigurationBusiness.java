package org.constellation.admin;

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


import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.util.MetadataUtilities;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.Application;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Service;
import org.constellation.repository.PropertyRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.token.TokenUtils;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("configurationBusiness")
@Primary
public class ConfigurationBusiness implements IConfigurationBusiness {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private IMetadataBusiness metadatabusiness;

    public void init() {
        LOGGER.info("=== Configure directory ===");
        ConfigDirectory.init();
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
    public Path getDataIntegratedDirectory(String providerId) throws IOException {
        if (providerId == null) {
            return ConfigDirectory.getDataIntegratedDirectory();
        }
        return ConfigDirectory.getDataIntegratedDirectory(providerId);
    }

    @Override
    public void removeDataIntegratedDirectory(String providerId) {
        if (providerId == null || providerId.isEmpty()) {
            throw new IllegalArgumentException("ProviderId must not be null or empty");
        }
        try {
            final Path provDir = ConfigDirectory.getDataIntegratedDirectory(providerId).normalize();
            final Path baseDir = ConfigDirectory.getDataIntegratedDirectory().normalize();
            // Security: if given "." or ".." or any fragment allowing to resolve directory upstream, launch an error to prevent data corruption
            if (!provDir.startsWith(baseDir) || baseDir.startsWith(provDir)) {
                throw new IllegalArgumentException("Given provider ID is invalid");
            }
            org.geotoolkit.nio.IOUtilities.deleteRecursively(provDir);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during delete data on FS for provider {0}", providerId);
        }
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
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "An error occurred updating service URL", ex);
        }
    }

    @Override
    public Properties getMetadataTemplateProperties() {
        return ConfigDirectory.getMetadataTemplateProperties();
    }

}
