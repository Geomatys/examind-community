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
package org.constellation.database.impl.repository;

import com.examind.database.api.jooq.Tables;
import org.constellation.dto.service.Service;
import com.examind.database.api.jooq.tables.pojos.ServiceExtraConfig;
import com.examind.database.api.jooq.tables.pojos.ProviderXSos;
import com.examind.database.api.jooq.tables.records.ServiceDetailsRecord;
import com.examind.database.api.jooq.tables.records.ServiceExtraConfigRecord;
import com.examind.database.api.jooq.tables.records.ServiceRecord;
import org.constellation.dto.ServiceReference;
import org.constellation.repository.ServiceRepository;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.examind.database.api.jooq.Tables.DATA;
import static com.examind.database.api.jooq.Tables.DATA_X_DATA;
import static com.examind.database.api.jooq.Tables.LAYER;
import static com.examind.database.api.jooq.Tables.METADATA;
import static com.examind.database.api.jooq.Tables.PROVIDER_X_SOS;
import static com.examind.database.api.jooq.Tables.PROVIDER_X_CSW;
import static com.examind.database.api.jooq.Tables.METADATA_X_CSW;
import static com.examind.database.api.jooq.Tables.PROVIDER;
import static com.examind.database.api.jooq.Tables.SENSOR_X_SOS;
import static com.examind.database.api.jooq.Tables.SENSORED_DATA;
import static com.examind.database.api.jooq.Tables.SERVICE;
import static com.examind.database.api.jooq.Tables.SERVICE_DETAILS;
import static com.examind.database.api.jooq.Tables.SERVICE_EXTRA_CONFIG;
import com.examind.database.api.jooq.tables.pojos.ProviderXCsw;
import org.constellation.dto.LinkedProvider;
import org.springframework.context.annotation.DependsOn;

@Component
@DependsOn("database-initer")
public class JooqServiceRepository extends AbstractJooqRespository<ServiceRecord, com.examind.database.api.jooq.tables.pojos.Service> implements ServiceRepository {

    private static final Field[] REFERENCE_FIELDS = new Field[]{
            SERVICE.ID.as("id"),
            SERVICE.IDENTIFIER.as("identifier"),
            SERVICE.TYPE.as("type")};


    public JooqServiceRepository() {
        super(com.examind.database.api.jooq.tables.pojos.Service.class, SERVICE);
    }

    @Override
    public List<Service> findByDataId(int dataId) {
        final List<com.examind.database.api.jooq.tables.pojos.Service> results = new ArrayList<>();

        results.addAll(dsl.select(SERVICE.fields()).from(SERVICE).join(Tables.LAYER).onKey()
                .where(Tables.LAYER.DATA.eq(dataId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));

        results.addAll(dsl.select(SERVICE.fields()).from(Arrays.asList(SERVICE,METADATA_X_CSW,METADATA))
                .where(METADATA_X_CSW.CSW_ID.eq(SERVICE.ID))
                .and(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID))
                .and(METADATA.DATA_ID.eq(dataId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));

        results.addAll(dsl.select(SERVICE.fields()).from(Arrays.asList(SERVICE,SENSOR_X_SOS,SENSORED_DATA))
                .where(SENSOR_X_SOS.SOS_ID.eq(SERVICE.ID))
                .and(SENSOR_X_SOS.SENSOR_ID.eq(SENSORED_DATA.SENSOR))
                .and(SENSORED_DATA.DATA.eq(dataId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));

        return convertListToDto(results);
    }

    @Override
    public Service findByIdentifierAndType(String identifier, String type) {
        return  convertIntoServiceDto(dsl
                .select()
                .from(SERVICE)
                .where(SERVICE.IDENTIFIER.eq(identifier)
                .and(SERVICE.TYPE.equalIgnoreCase(type)))
                .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Service.class));

    }

    @Override
    public Integer findIdByIdentifierAndType(String identifier, String type) {
        return dsl.select(SERVICE.ID).from(SERVICE)
                .where(SERVICE.IDENTIFIER.eq(identifier).and(SERVICE.TYPE.equalIgnoreCase(type))).fetchOneInto(Integer.class);

    }

    @Override
    public boolean existsById(Integer id) {
        return dsl.fetchExists(dsl.selectOne().from(SERVICE).where(SERVICE.ID.eq(id)));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int delete(Integer id) {
        dsl.delete(SENSOR_X_SOS).where(SENSOR_X_SOS.SOS_ID.eq(id)).execute();
        dsl.delete(PROVIDER_X_SOS).where(PROVIDER_X_SOS.SOS_ID.eq(id)).execute();
        dsl.delete(PROVIDER_X_CSW).where(PROVIDER_X_CSW.CSW_ID.eq(id)).execute();
        dsl.delete(SERVICE_DETAILS).where(SERVICE_DETAILS.ID.eq(id)).execute();
        dsl.delete(SERVICE_EXTRA_CONFIG).where(SERVICE_EXTRA_CONFIG.ID.eq(id)).execute();
        return dsl.delete(SERVICE).where(SERVICE.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int deleteAll() {
        dsl.delete(SENSOR_X_SOS).execute();
        dsl.delete(PROVIDER_X_SOS).execute();
        dsl.delete(PROVIDER_X_CSW).execute();
        dsl.delete(SERVICE_DETAILS).execute();
        dsl.delete(SERVICE_EXTRA_CONFIG).execute();
        return dsl.delete(SERVICE).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Service update(Service service) {
        dsl.update(SERVICE)
                .set(SERVICE.DATE, service.getDate() != null ? service.getDate().getTime() : null)
                .set(SERVICE.CONFIG, service.getConfig())
                .set(SERVICE.IDENTIFIER, service.getIdentifier())
                .set(SERVICE.OWNER, service.getOwner())
                .set(SERVICE.STATUS, service.getStatus())
                .set(SERVICE.TYPE, service.getType())
                .set(SERVICE.VERSIONS, service.getVersions())
                .where(SERVICE.ID.eq(service.getId())).execute();
        return service;
    }

    @Override
    public List<String> findIdentifiersByType(String type) {
        return dsl.select(SERVICE.IDENTIFIER).from(SERVICE).where(SERVICE.TYPE.eq(type)).fetch(SERVICE.IDENTIFIER);
    }

    @Override
    public List<Service> findByType(String type) {
        SelectConditionStep<Record> from = dsl.select().from(SERVICE).where(SERVICE.TYPE.eq(type.toLowerCase()));
        return convertListToDto(from.fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));
    }

    @Override
    public String getServiceDetailsForDefaultLang(int serviceId) {
        return dsl.select(SERVICE_DETAILS.CONTENT).from(SERVICE_DETAILS).where(SERVICE_DETAILS.ID.eq(serviceId)).and(SERVICE_DETAILS.DEFAULT_LANG.eq(true)).fetchOneInto(String.class);
    }

    @Override
    public String getServiceDetails(int serviceId, String language) {
        if (language != null) {
            return dsl.select(SERVICE_DETAILS.CONTENT).from(SERVICE_DETAILS).where(SERVICE_DETAILS.ID.eq(serviceId)).and(SERVICE_DETAILS.LANG.eq(language)).fetchOneInto(String.class);
        } else {
            return dsl.select(SERVICE_DETAILS.CONTENT).from(SERVICE_DETAILS).where(SERVICE_DETAILS.ID.eq(serviceId)).and(SERVICE_DETAILS.DEFAULT_LANG.eq(true)).fetchOneInto(String.class);
        }
    }

    @Override
    public List<String> getServiceDefinedLanguage(int serviceId) {
        return dsl.select(SERVICE_DETAILS.LANG).from(SERVICE_DETAILS).where(SERVICE_DETAILS.ID.eq(serviceId)).fetchInto(String.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void createOrUpdateServiceDetails(Integer id, String lang, String content, Boolean defaultLang) {
        final String old = getServiceDetails(id, lang);
        if (old!=null){
            dsl.update(SERVICE_DETAILS).set(SERVICE_DETAILS.CONTENT, content)
                    .set(SERVICE_DETAILS.DEFAULT_LANG, defaultLang)
                    .where(SERVICE_DETAILS.ID.eq(id))
                    .and(SERVICE_DETAILS.LANG.eq(lang))

                    .execute();
        } else {
            ServiceDetailsRecord newRecord = dsl.newRecord(SERVICE_DETAILS);
            newRecord.setContent(content);
            newRecord.setLang(lang);
            newRecord.setId(id);
            newRecord.setDefaultLang(defaultLang);
            newRecord.store();
        }
    }

    @Override
    public Map<String, String> getExtraConfig(int id) {
        List<ServiceExtraConfig> configs = dsl.select().from(SERVICE_EXTRA_CONFIG).where(SERVICE_EXTRA_CONFIG.ID.eq(id))
                .fetchInto(ServiceExtraConfig.class);
        Map<String, String> results = new HashMap<>();
        for (ServiceExtraConfig config : configs) {
            results.put(config.getFilename(), config.getContent());
        }
        return results;
    }

    @Override
    public String getExtraConfig(int id, String filename) {
        return dsl.select(SERVICE_EXTRA_CONFIG.CONTENT).from(SERVICE_EXTRA_CONFIG)
                .where(SERVICE_EXTRA_CONFIG.ID.eq(id).and(SERVICE_EXTRA_CONFIG.FILENAME.eq(filename)))
                .fetchOneInto(String.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateStatus(int id, String status) {
        dsl.update(SERVICE).set(SERVICE.STATUS, status).where(SERVICE.ID.eq(id)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateExtraFile(Integer serviceID, String fileName, String config) {
        int updateCount = dsl.update(SERVICE_EXTRA_CONFIG)
                .set(SERVICE_EXTRA_CONFIG.CONTENT, config)
                .where(SERVICE_EXTRA_CONFIG.ID.eq(serviceID))
                .and(SERVICE_EXTRA_CONFIG.FILENAME.eq(fileName))
                .execute();
        if (updateCount == 0) {
            ServiceExtraConfigRecord newRecord = dsl.newRecord(SERVICE_EXTRA_CONFIG);
            newRecord.setContent(config);
            newRecord.setFilename(fileName);
            newRecord.setId(serviceID);
            newRecord.store();
        }
    }

    @Override
    public Service findById(int id) {
        Record one = dsl.select().from(SERVICE).where(SERVICE.ID.eq(id)).fetchOne();
        if (one == null)
            return null;
        return convertIntoServiceDto(one.into(com.examind.database.api.jooq.tables.pojos.Service.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public int create(Service service) {
        ServiceRecord serviceRecord = dsl.newRecord(SERVICE);
        serviceRecord.setConfig(service.getConfig());
        serviceRecord.setDate(service.getDate() != null ? service.getDate().getTime() : null);
        serviceRecord.setIdentifier(service.getIdentifier());
        serviceRecord.setType(service.getType());
        serviceRecord.setOwner(service.getOwner());
        serviceRecord.setStatus(service.getStatus());
        serviceRecord.setVersions(service.getVersions());
        serviceRecord.setImpl(service.getImpl());
        serviceRecord.store();
        return serviceRecord.getId();
    }


    @Override
    public Service findByMetadataId(String metadataId) {
        return convertIntoServiceDto(dsl
                .select()
                .from(SERVICE)
                .join(METADATA)
                .onKey(METADATA.SERVICE_ID)
                .where(METADATA.METADATA_ID.eq(metadataId))
                .fetchOneInto(com.examind.database.api.jooq.tables.pojos.Service.class));
    }

    @Override
    public List<ServiceReference> fetchByDataId(int dataId) {
        List<ServiceReference> services = new ArrayList<>();

        // "Layer" services.
        services.addAll(dsl.select(REFERENCE_FIELDS).from(SERVICE)
                .join(LAYER).on(LAYER.SERVICE.eq(SERVICE.ID)) // service -> layer
                .leftOuterJoin(DATA_X_DATA).on(DATA_X_DATA.CHILD_ID.eq(LAYER.DATA)) // layer -> data_x_data (child_id)
                .where(LAYER.DATA.eq(dataId).or(DATA_X_DATA.DATA_ID.eq(dataId)))
                .fetchInto(ServiceReference.class));

        // "Metadata" services.
        services.addAll(dsl.select(REFERENCE_FIELDS).from(DATA)
                .join(METADATA).on(METADATA.DATASET_ID.eq(DATA.DATASET_ID)) // data -> metadata
                .join(METADATA_X_CSW).on(METADATA_X_CSW.METADATA_ID.eq(METADATA.ID)) // metadata -> metadata_x_csw
                .join(SERVICE).on(SERVICE.ID.eq(METADATA_X_CSW.CSW_ID)) // metadata_x_csw -> service
                .where(DATA.ID.eq(dataId))
                .fetchInto(ServiceReference.class));

        // "Sensor" services.
        services.addAll(dsl.select(SERVICE.fields()).from(Arrays.asList(SERVICE,SENSOR_X_SOS,SENSORED_DATA))
                .where(SENSOR_X_SOS.SOS_ID.eq(SERVICE.ID))
                .and(SENSOR_X_SOS.SENSOR_ID.eq(SENSORED_DATA.SENSOR))
                .and(SENSORED_DATA.DATA.eq(dataId))
                .fetchInto(ServiceReference.class));

        return services;
    }

    @Override
    public String getImplementation(Integer serviceId) {
        return dsl.select(SERVICE.IMPL).from(SERVICE)
                .where(SERVICE.ID.eq(serviceId)).fetchOneInto(String.class);
    }

    @Override
    public List<Integer> getLinkedSensorProviders(Integer serviceId, String type) {
        if (type != null) {
            return dsl.select(PROVIDER_X_SOS.PROVIDER_ID).from(Arrays.asList(PROVIDER_X_SOS, PROVIDER))
                      .where(PROVIDER_X_SOS.SOS_ID.eq(serviceId))
                      .and(PROVIDER.ID.eq(PROVIDER_X_SOS.PROVIDER_ID))
                      .and(PROVIDER.TYPE.eq(type))
                      .fetchInto(Integer.class);
        } else {
            return dsl.select(PROVIDER_X_SOS.PROVIDER_ID).from(Arrays.asList(PROVIDER_X_SOS))
                      .where(PROVIDER_X_SOS.SOS_ID.eq(serviceId)).fetchInto(Integer.class);
        }
    }

    @Override
    public boolean isAllLinked(int serviceId, int providerId) {
        ProviderXCsw pxc = dsl.select(PROVIDER_X_CSW.ALL_METADATA).from(PROVIDER_X_CSW)
           .where(PROVIDER_X_CSW.PROVIDER_ID.eq(providerId))
           .and(PROVIDER_X_CSW.CSW_ID.eq(serviceId)).fetchOneInto(ProviderXCsw.class);
        if (pxc != null) {
            return pxc.getAllMetadata();
        }
        ProviderXSos pxs = dsl.select(PROVIDER_X_SOS.ALL_SENSOR).from(PROVIDER_X_SOS)
           .where(PROVIDER_X_SOS.PROVIDER_ID.eq(providerId))
           .and(PROVIDER_X_SOS.SOS_ID.eq(serviceId)).fetchOneInto(ProviderXSos.class);
        if (pxs != null) {
            return pxs.getAllSensor();
        }
        return false;
    }

    @Override
    public List<Service> getProviderLinkedSensorServices(Integer providerId) {
        return convertListToDto(dsl.select(SERVICE.fields()).from(Arrays.asList(PROVIDER_X_SOS,SERVICE))
                .where(PROVIDER_X_SOS.SOS_ID.eq(SERVICE.ID))
                .and(PROVIDER_X_SOS.PROVIDER_ID.eq(providerId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));
    }

    @Override
    public List<Service> getSensorLinkedSensorServices(int sensorId) {
        return convertListToDto(dsl.select(SERVICE.fields()).from(Arrays.asList(SENSOR_X_SOS,SERVICE))
                .where(SENSOR_X_SOS.SOS_ID.eq(SERVICE.ID))
                .and(SENSOR_X_SOS.SENSOR_ID.eq(sensorId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));
    }

    @Override
    public List<Service> getDataLinkedSensorServices(int dataId) {
        return convertListToDto(dsl.select(SERVICE.fields()).from(Arrays.asList(SENSOR_X_SOS,SERVICE, SENSORED_DATA))
                .where(SENSOR_X_SOS.SOS_ID.eq(SERVICE.ID))
                .and(SENSOR_X_SOS.SENSOR_ID.eq(SENSORED_DATA.SENSOR))
                .and(SENSORED_DATA.DATA.eq(dataId))
                .fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));
    }
    
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkSensorProvider(int serviceId, int providerID, boolean allSensor) {
        boolean exist = dsl.selectCount()
                           .from(PROVIDER_X_SOS)
                           .where(PROVIDER_X_SOS.PROVIDER_ID.eq(providerID))
                           .and(PROVIDER_X_SOS.SOS_ID.eq(serviceId))
                           .fetchOneInto(Integer.class) > 0;
        if (exist) {
            dsl.update(PROVIDER_X_SOS).set(PROVIDER_X_SOS.ALL_SENSOR, allSensor)
                                      .where(PROVIDER_X_SOS.PROVIDER_ID.eq(providerID))
                                      .and(PROVIDER_X_SOS.SOS_ID.eq(serviceId)).execute();
        } else {
            dsl.insertInto(PROVIDER_X_SOS).set(PROVIDER_X_SOS.SOS_ID, serviceId)
                                          .set(PROVIDER_X_SOS.PROVIDER_ID, providerID)
                                          .set(PROVIDER_X_SOS.ALL_SENSOR, allSensor)
                                          .execute();
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removelinkedSensorProviders(int serviceId) {
        dsl.delete(PROVIDER_X_SOS).where(PROVIDER_X_SOS.SOS_ID.eq(serviceId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removelinkedSensors(int serviceId) {
        dsl.delete(SENSOR_X_SOS).where(SENSOR_X_SOS.SOS_ID.eq(serviceId)).execute();
    }

    @Override
    public List<LinkedProvider> getLinkedMetadataProvider(int serviceId) {
        return convertPXCListToDto(dsl.select().from(Arrays.asList(PROVIDER_X_CSW))
                .where(PROVIDER_X_CSW.CSW_ID.eq(serviceId)).fetchInto(ProviderXCsw.class));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkMetadataProvider(int serviceId, int providerID, boolean allMetadata) {
        if (isLinkedMetadataProviderAndService(serviceId, providerID) == null) {
            dsl.insertInto(PROVIDER_X_CSW).set(PROVIDER_X_CSW.CSW_ID, serviceId)
                                          .set(PROVIDER_X_CSW.PROVIDER_ID, providerID)
                                          .set(PROVIDER_X_CSW.ALL_METADATA, allMetadata)
                                          .execute();
        } else {
            dsl.update(PROVIDER_X_CSW).set(PROVIDER_X_CSW.ALL_METADATA, allMetadata)
                                      .where(PROVIDER_X_CSW.CSW_ID.eq(serviceId))
                                      .and(PROVIDER_X_CSW.PROVIDER_ID.eq(providerID))
                                      .execute();
        }
    }

    @Override
    public Service getLinkedMetadataService(int providerId) {
        return dsl.select(SERVICE.fields()).from(Arrays.asList(PROVIDER_X_CSW,SERVICE))
                .where(PROVIDER_X_CSW.PROVIDER_ID.eq(providerId))
                .and(PROVIDER_X_CSW.CSW_ID.eq(SERVICE.ID)).fetchOneInto(Service.class);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removelinkedMetadataProvider(int serviceId) {
        dsl.delete(PROVIDER_X_CSW).where(PROVIDER_X_CSW.CSW_ID.eq(serviceId)).execute();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removelinkedMetadatas(int serviceId) {
        dsl.delete(METADATA_X_CSW).where(METADATA_X_CSW.CSW_ID.eq(serviceId)).execute();
    }

    @Override
    public LinkedProvider isLinkedMetadataProviderAndService(int serviceId, int providerID) {
        return convertIntoPXCDto(dsl
                .select().from(PROVIDER_X_CSW)
                .where(PROVIDER_X_CSW.PROVIDER_ID.eq(providerID))
                .and(PROVIDER_X_CSW.CSW_ID.eq(serviceId))
                .fetchOneInto( ProviderXCsw.class));
    }

    @Override
    public List<Service> findAll() {
        return convertListToDto(dsl.select().from(SERVICE).fetchInto(com.examind.database.api.jooq.tables.pojos.Service.class));
    }

    private List<Service> convertListToDto(Collection<com.examind.database.api.jooq.tables.pojos.Service> daos) {
        Map<Integer, Service> results = new LinkedHashMap<>();
        // eliminates doublons if any
        for (com.examind.database.api.jooq.tables.pojos.Service dao : daos) {
            if (!results.containsKey(dao.getId())) {
                results.put(dao.getId(), convertIntoServiceDto(dao));
            }
            
        }
        return new ArrayList<>(results.values());
    }

    private Service convertIntoServiceDto(final com.examind.database.api.jooq.tables.pojos.Service service) {
        if (service != null) {
            final org.constellation.dto.service.Service serviceDTO = new org.constellation.dto.service.Service();
            serviceDTO.setOwner(service.getOwner());
            serviceDTO.setConfig(service.getConfig());
            serviceDTO.setDate(new Date(service.getDate()));
            serviceDTO.setId(service.getId());
            serviceDTO.setIdentifier(service.getIdentifier());
            serviceDTO.setStatus(service.getStatus());
            serviceDTO.setType(service.getType());
            serviceDTO.setVersions(service.getVersions());
            serviceDTO.setImpl(service.getImpl());
            return serviceDTO;
        }
        return null;
    }

    private List<LinkedProvider> convertPXCListToDto(List<com.examind.database.api.jooq.tables.pojos.ProviderXCsw> daos) {
        List<LinkedProvider> results = new ArrayList<>();
        for (com.examind.database.api.jooq.tables.pojos.ProviderXCsw dao : daos) {
            results.add(convertIntoPXCDto(dao));
        }
        return results;
    }

    private LinkedProvider convertIntoPXCDto(final com.examind.database.api.jooq.tables.pojos.ProviderXCsw pxc) {
        if (pxc != null) {
            return new LinkedProvider(pxc.getProviderId(), pxc.getAllMetadata());
        }
        return null;
    }
}
