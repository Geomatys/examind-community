/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.constellation.dto.Data;
import org.constellation.dto.service.Service;
import org.constellation.dto.ServiceReference;

public interface ServiceRepository {

    int create(Service service);

    List<Service> findAll();

    Service findById(int id);

    List<Service> findByDataId(int dataId);

    List<Service> findByType(String type);

    Service findByIdentifierAndType(String id, String type);

    Integer findIdByIdentifierAndType(String id, String type);

    boolean exist(Integer id);

    void delete(Integer id);

    List<String> findIdentifiersByType(String type);

    String getServiceDetailsForDefaultLang(int serviceId);

    String getServiceDetails(int serviceId, String language);

    List<String> getServiceDefinedLanguage(int serviceId);

    void createOrUpdateServiceDetails(Integer id, String lang, String content, Boolean defaultLang);

    Map<String, String> getExtraConfig(int id);

    String getExtraConfig(int id, String filename);

    Service update(Service service);

    void updateStatus(int id, String status);

    void updateExtraFile(Integer serviceID, String fileName, String config);

    Service findByMetadataId(String metadataId);

    List<Data> findDataByServiceId(Integer id);

    List<ServiceReference> fetchByDataId(int dataId);

    List<Integer> getLinkedSensorProviders(int serviceId);

    List<Service> getLinkedSOSServices(int providerId);

    List<Service> getSensorLinkedServices(int sensorId);

    void linkSensorProvider(int serviceId, int providerID, boolean allSensor);

    void removelinkedSensorProviders(int serviceId);

    void removelinkedSensors(int serviceId);

    Integer getLinkedMetadataProvider(int serviceId);

    void linkMetadataProvider(int serviceId, int providerID, boolean allMetadata);

    Service getLinkedMetadataService(int providerId);

    void removelinkedMetadataProvider(int serviceId);

    boolean isLinkedMetadataProviderAndService(int serviceId, int providerID);

}
