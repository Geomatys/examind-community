/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2018 Geomatys.
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
package com.examind.repository.filesystem;

import static com.examind.repository.filesystem.FileSystemUtilities.getObjectFromPath;
import static com.examind.repository.filesystem.FileSystemUtilities.writeObjectInPath;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.constellation.api.ServiceDef;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Data;
import org.constellation.dto.ServiceReference;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.ServiceRepository;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemServiceRepository extends AbstractFileSystemRepository implements ServiceRepository {

    private final Map<String, Service> byMetadataService = new HashMap<>();
    private final Map<Integer, Service> loadedService = new HashMap<>();
    private final Map<Integer, Map<String, String>> loadedServiceDetails = new HashMap<>();
    private final Map<String, Map<String, Service>> byTypeNameService = new HashMap<>();
    private final Map<Integer, Map<String, String>> extraConfigs = new HashMap<>();

    public FileSystemServiceRepository() {
        super(Service.class);
        load();
    }

    private void load() {
        try {
            for (ServiceDef.Specification spec : ServiceDef.Specification.values()) {
                Collection<? extends Path> instances = ConfigDirectory.getInstanceDirectories(spec.name().toLowerCase());
                Iterator<? extends Path> it = instances.iterator();
                Map<String, Service> nameService = new HashMap<>();
                while (it.hasNext()) {
                    Path p = it.next();


                    Path servFile = p.resolve("service.xml");
                    Service serObj = (Service) getObjectFromPath(servFile, pool);
                    loadedService.put(serObj.getId(), serObj);
                    nameService.put(serObj.getIdentifier(), serObj);

                    if (serObj.getId() >= currentId) {
                        currentId = serObj.getId() +1;
                    }

                    Path extraFiles = p.resolve("extras");
                    if (!Files.isDirectory(extraFiles)) {
                        Files.createDirectory(extraFiles);
                    }
                    Map<String, String> servExtraConfigs = new HashMap<>();
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(extraFiles)) {
                        for (Path extraFile : directoryStream) {
                            servExtraConfigs.put(extraFile.getFileName().toString(), IOUtilities.toString(extraFile));
                        }
                    }
                    extraConfigs.put(serObj.getId(), servExtraConfigs);

                    Path detailFiles = p.resolve("i18n");
                    if (!Files.isDirectory(detailFiles)) {
                        Files.createDirectory(detailFiles);
                    }
                    Map<String, String> servdetails = new HashMap<>();
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(detailFiles)) {
                        for (Path detailFile : directoryStream) {
                            servdetails.put(detailFile.getFileName().toString(), IOUtilities.toString(detailFile));
                        }
                    }
                    loadedServiceDetails.put(serObj.getId(), servdetails);

                    Path metadataFile = p.resolve("metadata.xml");
                    // TODO


                }
                byTypeNameService.put(spec.name().toLowerCase(), nameService);
            }
        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List<Service> findAll() {
        return new ArrayList<>(loadedService.values());
    }

    @Override
    public Service findById(int id) {
        return loadedService.get(id);
    }

    @Override
    public List<Service> findByType(String type) {
        Map<String, Service>  services = byTypeNameService.get(type.toLowerCase());
        if (services != null) {
            return new ArrayList(services.values());
        }
        return new ArrayList();
    }

    @Override
    public Service findByIdentifierAndType(String id, String type) {
        Map<String, Service>  services = byTypeNameService.get(type.toLowerCase());
        if (services != null) {
            return services.get(id);
        }
        return null;
    }

    @Override
    public Integer findIdByIdentifierAndType(String id, String type) {
        Map<String, Service>  services = byTypeNameService.get(type.toLowerCase());
        if (services != null && services.get(id) != null) {
            return services.get(id).getId();
        }
        return null;
    }

    @Override
    public List<String> findIdentifiersByType(String type) {
        List<String> identifiers = new ArrayList<>();
        Map<String, Service>  services = byTypeNameService.get(type.toLowerCase());
        if (services != null) {
            for (Service service : services.values()) {
                identifiers.add(service.getIdentifier());
            }
        }
        return identifiers;
    }

    @Override
    public String getServiceDetailsForDefaultLang(int serviceId) {
        Map<String, String> details = loadedServiceDetails.get(serviceId);
        if (details != null && !details.values().isEmpty()) {
            return details.values().iterator().next();
        }
        return null;
    }

    @Override
    public String getServiceDetails(int serviceId, String language) {
        Map<String, String> details = loadedServiceDetails.get(serviceId);
        if (details != null) {
            return details.get(language);
        }
        return null;
    }

    @Override
    public Service findByMetadataId(String metadataId) {
        return byMetadataService.get(metadataId);
    }

    @Override
    public Map<String, String> getExtraConfig(int id) {
        return extraConfigs.get(id);
    }

    @Override
    public String getExtraConfig(int id, String filename) {
        Map<String, String> configs = extraConfigs.get(id);
        if (configs != null) {
            return configs.get(filename);
        }
        return null;
    }


    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(Service service) {
        service.setId(currentId);
        currentId++;

        Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
        Path servFile = servDir.resolve("service.xml");

        writeObjectInPath(service, servFile, pool);
        loadedService.put(service.getId(), service);

        Map<String, Service> nameService = byTypeNameService.get(service.getType());
        if (nameService == null) {
            nameService = new HashMap<>();
        }
        nameService.put(service.getIdentifier(), service);
        byTypeNameService.put(service.getType(), nameService);

        return service.getId();
    }

    @Override
    public void delete(Integer id) {
        Service service = loadedService.get(id);
        if (service != null) {
            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            try {
                IOUtilities.deleteRecursively(servDir);
            } catch (IOException ex) {
                Logger.getLogger(FileSystemServiceRepository.class.getName()).log(Level.SEVERE, null, ex);
            }
            loadedService.remove(id);
            loadedServiceDetails.remove(id);
            Map<String, Service> byType = byTypeNameService.get(service.getType());
            if (byType != null) {
                byType.remove(service.getIdentifier());
            }
            extraConfigs.remove(id);
        }
    }

    @Override
    public void createOrUpdateServiceDetails(Integer id, String lang, String content, Boolean defaultLang) {
        if (loadedService.containsKey(id)) {
            Service service = loadedService.get(id);

            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path detailFiles = servDir.resolve("i18n");
            try {
                if (!Files.isDirectory(detailFiles)) {
                    Files.createDirectory(detailFiles);
                }
                Path detailFile  = detailFiles.resolve(lang);

                IOUtilities.writeString(content, detailFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException(ex);
            }

            Map<String, String> details = loadedServiceDetails.get(id);
            if (details == null) {
                details = new HashMap<>();
            }
            details.put(lang, content);
            loadedServiceDetails.put(id, details);
        }
    }

    @Override
    public Service update(Service service) {
        if (loadedService.containsKey(service.getId())) {
            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path servFile = servDir.resolve("service.xml");

            writeObjectInPath(service, servFile, pool);
            loadedService.put(service.getId(), service);

            Map<String, Service> nameService = byTypeNameService.get(service.getType());
            if (nameService == null) {
                nameService = new HashMap<>();
            }
            nameService.put(service.getIdentifier(), service);
            byTypeNameService.put(service.getType(), nameService);
        }
        return service;
    }

    @Override
    public void updateStatus(int id, String status) {
        if (loadedService.containsKey(id)) {
            Service service = loadedService.get(id);
            service.setStatus(status);

            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path servFile = servDir.resolve("service.xml");

            writeObjectInPath(service, servFile, pool);
            loadedService.put(service.getId(), service);

            Map<String, Service> nameService = byTypeNameService.get(service.getType());
            if (nameService == null) {
                nameService = new HashMap<>();
            }
            nameService.put(service.getIdentifier(), service);
            byTypeNameService.put(service.getType(), nameService);
        }
    }

    @Override
    public void updateExtraFile(Integer serviceID, String fileName, String config) {
        if (loadedService.containsKey(serviceID)) {
            Service service = loadedService.get(serviceID);

            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path extraFiles = servDir.resolve("extras");
            Path extraFile  = extraFiles.resolve("fileName");

            try {
                IOUtilities.writeString(config, extraFile);
            } catch (IOException ex) {
                throw new ConstellationPersistenceException("unable to write extra file:" + extraFile.toString());
            }
            Map<String, String> extras = extraConfigs.get(serviceID);
            if (extras == null) {
                extras = new HashMap<>();
            }
            extras.put(fileName, config);
            extraConfigs.put(serviceID, extras);
        }
    }


    ////--------------------------------------------------------------------///
    ////------------------------    DATA        ----------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Service> findByDataId(int dataId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Data> findDataByServiceId(Integer id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ServiceReference> fetchByDataId(int dataId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SOS         ----------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Integer> getLinkedSensorProviders(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Service> getLinkedSOSServices(int providerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkSensorProvider(int serviceId, int providerID, boolean allSensor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removelinkedSensorProviders(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removelinkedSensors(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

     ////--------------------------------------------------------------------///
    ////------------------------    CSW         ----------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer getLinkedMetadataProvider(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void linkMetadataProvider(int serviceId, int providerID, boolean allMetadata) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Service getLinkedMetadataService(int providerId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removelinkedMetadataProvider(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isLinkedMetadataProviderAndService(int serviceId, int providerID) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
