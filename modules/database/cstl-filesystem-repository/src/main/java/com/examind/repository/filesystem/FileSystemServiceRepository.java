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

import static com.examind.repository.filesystem.FileSystemUtilities.SERVICE_X_META_PROV_DIR;
import static com.examind.repository.filesystem.FileSystemUtilities.SERVICE_X_SENS_PROV_DIR;
import static com.examind.repository.filesystem.FileSystemUtilities.getDirectory;
import static com.examind.repository.filesystem.FileSystemUtilities.getIntegerList;
import static com.examind.repository.filesystem.FileSystemUtilities.getObjectFromPath;
import static com.examind.repository.filesystem.FileSystemUtilities.writeObjectInPath;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.constellation.api.ServiceDef;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.Layer;
import org.constellation.dto.ServiceReference;
import org.constellation.dto.StringList;
import org.constellation.dto.service.Service;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ServiceRepository;
import org.geotoolkit.nio.IOUtilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class FileSystemServiceRepository extends AbstractFileSystemRepository implements ServiceRepository {

    private final Map<String, Service> byMetadataService = new HashMap<>();
    private final Map<Integer, Service> byId = new HashMap<>();
    private final Map<Integer, Map<String, String>> loadedServiceDetails = new HashMap<>();
    private final Map<String, Map<String, Service>> byTypeNameService = new HashMap<>();
    private final Map<Integer, Map<String, String>> extraConfigs = new HashMap<>();
    private final Map<Integer, Service> byMetaProvider = new HashMap<>();
    private final Map<Integer, List<Service>> bySensorProvider = new HashMap<>();

    @Autowired
    private LayerRepository layerRepository;


    public FileSystemServiceRepository() {
        super(Service.class, StringList.class);
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
                    byId.put(serObj.getId(), serObj);
                    nameService.put(serObj.getIdentifier(), serObj);

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

                    Path servMetaProvDir = getDirectory(SERVICE_X_META_PROV_DIR);
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(servMetaProvDir)) {
                        for (Path sensorProvFile : directoryStream) {
                            StringList sensorList = (StringList) getObjectFromPath(sensorProvFile, pool);
                            String fileName = sensorProvFile.getFileName().toString();
                            Integer providerId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                            // only one
                            Service linked = null;
                            for (Integer servId : getIntegerList(sensorList)) {
                                linked = byId.get(servId);
                            }
                            byMetaProvider.put(providerId, linked);
                        }
                    }
                    Path sensorProvDir = getDirectory(SERVICE_X_SENS_PROV_DIR);
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorProvDir)) {
                        for (Path sensorProvFile : directoryStream) {
                            StringList styleList = (StringList) getObjectFromPath(sensorProvFile, pool);
                            String fileName = sensorProvFile.getFileName().toString();
                            Integer providerId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
                            List<Service> linked = new ArrayList<>();
                            for (Integer serviceId : getIntegerList(styleList)) {
                                linked.add(byId.get(serviceId));
                            }
                            bySensorProvider.put(providerId, linked);
                        }
                    }
                    incCurrentId(serObj);
                    
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
        return new ArrayList<>(byId.values());
    }

    @Override
    public Service findById(int id) {
        return byId.get(id);
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
    public String getImplementation(Integer serviceId) {
        Service s = byId.get(serviceId);
        if (s != null) {
            return s.getImpl();
        }
        return null;
    }

    @Override
    public Service findByMetadataId(String metadataId) {
        return byMetadataService.get(metadataId);
    }

    @Override
    public Map<String, String> getExtraConfig(int id) {
        return new HashMap<>(extraConfigs.get(id));
    }

    @Override
    public String getExtraConfig(int id, String filename) {
        Map<String, String> configs = extraConfigs.get(id);
        if (configs != null) {
            return configs.get(filename);
        }
        return null;
    }

    @Override
    public boolean existsById(Integer id) {
        return byId.containsKey(id);
    }


    ////--------------------------------------------------------------------///
    ////------------------------    TRANSACTIONAL  -------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public int create(Service service) {
        final int id = assignCurrentId(service);

        Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
        Path servFile = servDir.resolve("service.xml");

        writeObjectInPath(service, servFile, pool);
        byId.put(id, service);

        Map<String, Service> nameService = byTypeNameService.get(service.getType());
        if (nameService == null) {
            nameService = new HashMap<>();
        }
        nameService.put(service.getIdentifier(), service);
        byTypeNameService.put(service.getType(), nameService);

        return service.getId();
    }

    @Override
    public int delete(Integer id) {
        Service service = byId.get(id);
        if (service != null) {
            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            try {
                IOUtilities.deleteRecursively(servDir);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            byId.remove(id);
            loadedServiceDetails.remove(id);
            Map<String, Service> byType = byTypeNameService.get(service.getType());
            if (byType != null) {
                byType.remove(service.getIdentifier());
            }
            extraConfigs.remove(id);
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteAll() {
        int cpt = 0;
        for (Integer ds : new HashSet<>(byId.keySet())) {
            cpt = cpt + delete(ds);
        }
        return cpt;
    }

    @Override
    public void createOrUpdateServiceDetails(Integer id, String lang, String content, Boolean defaultLang) {
        if (byId.containsKey(id)) {
            Service service = byId.get(id);

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
        if (byId.containsKey(service.getId())) {
            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path servFile = servDir.resolve("service.xml");

            writeObjectInPath(service, servFile, pool);
            byId.put(service.getId(), service);

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
        if (byId.containsKey(id)) {
            Service service = byId.get(id);
            service.setStatus(status);

            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path servFile = servDir.resolve("service.xml");

            writeObjectInPath(service, servFile, pool);
            byId.put(service.getId(), service);

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
        if (byId.containsKey(serviceID)) {
            Service service = byId.get(serviceID);

            Path servDir = ConfigDirectory.getInstanceDirectory(service.getType(), service.getIdentifier());
            Path extraFiles = servDir.resolve("extras");
            Path extraFile  = extraFiles.resolve("fileName");

            try {
                if (!Files.isDirectory(extraFiles)) {
                    Files.createDirectory(extraFiles);
                }
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
        List<Service> results = new ArrayList<>();
        List<Integer> layerIds = layerRepository.findByDataId(dataId);
        for (Integer layerId : layerIds) {
            Layer l = layerRepository.findById(layerId);
            results.add(byId.get(l.getService()));
        }
        return results;
    }

    @Override
    public List<ServiceReference> fetchByDataId(int dataId) {
        List<ServiceReference> results = new ArrayList<>();
        List<Integer> layerIds = layerRepository.findByDataId(dataId);
        for (Integer layerId : layerIds) {
            Layer l = layerRepository.findById(layerId);
            Service s = byId.get(l.getService());
            results.add(new ServiceReference(s.getId(), s.getIdentifier(), s.getType()));
        }
        return results;
    }

    ////--------------------------------------------------------------------///
    ////------------------------    SOS         ----------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public List<Integer> getLinkedSensorProviders(Integer serviceId) {
        List<Integer> results = new ArrayList<>();
        for (Integer pid : bySensorProvider.keySet()) {
            List<Service> ss = bySensorProvider.get(pid);
            for (Service s : ss) {
                if (s.getId().equals(serviceId)) {
                    results.add(pid);
                }
            }
        }
        return results;
    }

    @Override
    public List<Service> getLinkedSOSServices(Integer providerId) {
        if (bySensorProvider.containsKey(providerId)) {
            return bySensorProvider.get(providerId);
        }
        return new ArrayList<>();
    }

    @Override
    public void linkSensorProvider(int serviceId, int providerID, boolean allSensor) {
        Path sensorProvDir = getDirectory(SERVICE_X_SENS_PROV_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorProvDir)) {
            for (Path sensorProvFile : directoryStream) {
                String fileName = sensorProvFile.getFileName().toString();
                Integer currentProvId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentProvId.equals(providerID)) {
                    found = true;
                    StringList serviceList = (StringList) getObjectFromPath(sensorProvFile, pool);
                    List<Integer> serviceIds = getIntegerList(serviceList);
                    if (!serviceIds.contains(serviceId)) {
                        serviceIds.add(serviceId);

                        // update fs
                        writeObjectInPath(serviceList, sensorProvFile, pool);

                        // update memory
                        List<Service> services = bySensorProvider.get(providerID);
                        services.add(byId.get(serviceId));
                    }
                }
            }

            // create new file
            if (!found) {
                // update fs
                StringList serviceList = new StringList(Arrays.asList(serviceId + ""));
                Path sensorProvFile = sensorProvDir.resolve(providerID + ".xml");
                writeObjectInPath(serviceList, sensorProvFile, pool);

                // update memory
                List<Service> services = new ArrayList<>();
                services.add(byId.get(serviceId));
                bySensorProvider.put(providerID, services);
            }
        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while linking sensor and service", ex);
        }
    }

    @Override
    public void removelinkedSensorProviders(int serviceId) {
        Path sensorProvDir = getDirectory(SERVICE_X_SENS_PROV_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sensorProvDir)) {
            for (Path sensorProvFile : directoryStream) {
                String fileName = sensorProvFile.getFileName().toString();
                Integer currentProvId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                StringList serviceList = (StringList) getObjectFromPath(sensorProvFile, pool);
                List<Integer> serviceIds = getIntegerList(serviceList);
                if (serviceIds.contains(serviceId)) {
                    serviceIds.remove((Integer)serviceId);

                    // update fs
                    writeObjectInPath(serviceList, sensorProvFile, pool);

                    // update memory
                    List<Service> services = bySensorProvider.get(currentProvId);
                    services.remove(byId.get(serviceId));
                }
                
            }
        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking sensor providers", ex);
        }
    }

    @Override
    public void removelinkedSensors(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Service> getSensorLinkedServices(int sensorId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
     ////--------------------------------------------------------------------///
    ////------------------------    CSW         ----------------------------///
    ////--------------------------------------------------------------------///

    @Override
    public Integer getLinkedMetadataProvider(int serviceId) {
        for (Integer pid : byMetaProvider.keySet()) {
            Service s = byMetaProvider.get(pid);
            if (s.getId() == serviceId) {
                return pid;
            }
        }
        return null;
    }

    @Override
    public Service getLinkedMetadataService(int providerId) {
        return byMetaProvider.get(providerId);
    }

    @Override
    public boolean isLinkedMetadataProviderAndService(int serviceId, int providerID) {
        Service s = byMetaProvider.get(providerID);
        return s != null && s.getId().equals(serviceId);
    }

    @Override
    public void linkMetadataProvider(int serviceId, int providerID, boolean allMetadata) {
        Path metaProvDir = getDirectory(SERVICE_X_META_PROV_DIR);
        boolean found = false;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metaProvDir)) {
            for (Path metaProvFile : directoryStream) {
                String fileName = metaProvFile.getFileName().toString();
                Integer currentProvId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                if (currentProvId.equals(providerID)) {
                    found = true;
                    StringList serviceList = (StringList) getObjectFromPath(metaProvFile, pool);
                    List<Integer> serviceIds = getIntegerList(serviceList);
                    if (!serviceIds.contains(serviceId)) {
                        serviceIds.add(serviceId);

                        // update fs
                        writeObjectInPath(serviceList, metaProvFile, pool);

                        // update memory
                        byMetaProvider.put(providerID,  byId.get(serviceId));
                    }
                }
            }

            // create new file
            if (!found) {
                // update fs
                StringList serviceList = new StringList(Arrays.asList(serviceId + ""));
                Path metaProvFile = metaProvDir.resolve(providerID + ".xml");
                writeObjectInPath(serviceList, metaProvFile, pool);

                // update memory
                byMetaProvider.put(providerID, byId.get(serviceId));
            }
        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while linking sensor and service", ex);
        }
    }

    @Override
    public void removelinkedMetadataProvider(int serviceId) {
        Path metaProvDir = getDirectory(SERVICE_X_META_PROV_DIR);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metaProvDir)) {
            for (Path metaProvFile : directoryStream) {
                String fileName = metaProvFile.getFileName().toString();
                Integer currentProvId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));

                // update file
                StringList serviceList = (StringList) getObjectFromPath(metaProvFile, pool);
                List<Integer> serviceIds = getIntegerList(serviceList);
                if (serviceIds.contains(serviceId)) {
                    serviceIds.remove((Integer)serviceId);

                    // update fs
                    writeObjectInPath(serviceList, metaProvFile, pool);

                    // update memory
                    byMetaProvider.remove(currentProvId);
                }
            }
        } catch (IOException | JAXBException ex) {
            LOGGER.log(Level.WARNING, "Error while unlinking metadata providers", ex);
        }
    }

   
    @Override
    public List<String> getServiceDefinedLanguage(int serviceId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
