package org.constellation.admin;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.sis.metadata.iso.DefaultMetadata;
import org.apache.sis.util.logging.Logging;
import org.constellation.admin.util.MetadataUtilities;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IMetadataBusiness;
import org.constellation.configuration.AppProperty;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Service;
import org.constellation.repository.PropertyRepository;
import org.constellation.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class ConfigurationBusiness implements IConfigurationBusiness {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private IMetadataBusiness metadatabusiness;

    @Override
    public Path getConfigurationDirectory() {
        return ConfigDirectory.getConfigDirectory();
    }

    @Override
    public Path getDataDirectory() {
        return ConfigDirectory.getDataDirectory();
    }

    @Override
    public String getProperty(final String key) {
        return propertyRepository.getValue(key, null);
    }

    @Override
    @Transactional
    public void setProperty(final String key, final String value) {
        System.setProperty(key, value);
        // FIXME continue to save in database ?
        // create/update external configuration file to save preferences ?
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

}
