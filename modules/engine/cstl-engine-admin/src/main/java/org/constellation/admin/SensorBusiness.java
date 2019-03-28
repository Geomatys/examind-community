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

package org.constellation.admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.List;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.constellation.business.ISensorBusiness;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Sensor;
import org.constellation.repository.DataRepository;
import org.constellation.repository.SensorRepository;
import org.geotoolkit.sml.xml.SensorMLMarshallerPool;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.xml.bind.Marshaller;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.util.logging.Logging;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.repository.ServiceRepository;
import org.constellation.dto.service.config.sos.SensorMLTree;
import org.constellation.provider.DataProvider;
import org.constellation.provider.DataProviders;
import org.constellation.business.IProviderBusiness.SPI_NAMES;
import org.constellation.dto.SensorReference;
import org.constellation.business.IUserBusiness;
import org.constellation.exception.ConstellationStoreException;
import org.constellation.provider.Data;
import org.constellation.provider.SensorData;
import org.constellation.provider.SensorProvider;
import org.geotoolkit.storage.DataStores;
import org.geotoolkit.util.NamesExt;
import org.opengis.parameter.ParameterValueGroup;

@Component
@Primary
public class SensorBusiness implements ISensorBusiness {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.admin");

    @Inject
    private IUserBusiness userBusiness;

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private ServiceRepository serviceRepository;

    @Inject
    private org.constellation.security.SecurityManager securityManager;

    @Inject
    private DataRepository dataRepository;

    @Inject
    private IProviderBusiness providerBusiness;

    @Inject
    private IClusterBusiness clusterBusiness;

    @PostConstruct
    public void contextInitialized() {
        Lock lock = clusterBusiness.acquireLock("setup-default-sensor-internal-provider");
        lock.lock();
        LOGGER.fine("LOCK Acquired on cluster: setup-default-sensor-internal-provider");

        try {
            getDefaultInternalProviderID();
        } catch (ConfigurationException ex) {
            LOGGER.log(Level.WARNING, "Unable to generate default internal sensor provider{0}", ex.getMessage());
        } finally {
            LOGGER.fine("UNLOCK on cluster: setup-default-sensor-internal-provider");
            lock.unlock();
        }
    }

    @Override
    public Sensor getSensor(final String id) {
        return sensorRepository.findByIdentifier(id);
    }

    @Override
    public Sensor getSensor(final Integer id) {
        return sensorRepository.findById(id);
    }

    @Override
    public List<Sensor> getAll() {
        return sensorRepository.findAll();
    }

    @Override
    public List<Integer> getLinkedDataProviderIds(final Integer sensorId){
        return sensorRepository.getLinkedDataProviders(sensorId);
    }

    @Override
    public List<Integer> getLinkedDataIds(final Integer sensorId){
        return sensorRepository.getLinkedDatas(sensorId);
    }

    @Override
    public List<Integer> getLinkedServiceIds(Integer sensorId) {
        return sensorRepository.getLinkedServices(sensorId);
    }

    @Override
    public List<Sensor> getChildren(final String parentIdentifier) {
        return sensorRepository.getChildren(parentIdentifier);
    }

    @Override
    @Transactional
    public boolean delete(final String sensorID) throws ConfigurationException {
        final Integer sensor = sensorRepository.findIdByIdentifier(sensorID);
        if (sensor != null) {
            return delete(sensor);
        }
        return false;
    }

    @Override
    @Transactional
    public boolean delete(final Integer sensorID) throws ConfigurationException {
        final Sensor sensor = sensorRepository.findById(sensorID);
        boolean result = false;
        if (sensor != null) {
            final DataProvider provider = DataProviders.getProvider(sensor.getProviderId());
            result = provider.remove(NamesExt.create(sensor.getIdentifier()));
            if (result) sensorRepository.delete(sensor.getIdentifier());
        }
        return result;
    }

    @Override
    @Transactional
    public void delete(String sensorid, String providerId) throws ConfigurationException {
        delete(sensorid);
    }

    @Override
    @Transactional
    public void deleteAll() throws ConfigurationException {
        List<Sensor> sensors = sensorRepository.findAll();
        for (Sensor sensor : sensors) {
            delete(sensor.getIdentifier());
        }
    }

    @Override
    @Transactional
    public void deleteFromProvider(String providerId) {
        Integer provider = providerBusiness.getIDFromIdentifier(providerId);
        if (provider != null) {
            sensorRepository.deleteFromProvider(provider);
        }
    }

    @Override
    @Transactional
    public void linkDataToSensor(QName dataName, String providerId, String sensorIdentifier) {
        final Integer data = dataRepository.findIdFromProvider(dataName.getNamespaceURI(), dataName.getLocalPart(), providerId);
        final Integer sensor = sensorRepository.findIdByIdentifier(sensorIdentifier);
        sensorRepository.linkDataToSensor(data,sensor);
    }

    @Override
    @Transactional
    public void linkDataToSensor(int dataId, int sensorId) {
        sensorRepository.linkDataToSensor(dataId, sensorId);
    }

    /**
     * Proceed to remove the link between data and sensor.
     *
     * @param dataName given data name to find the data instance.
     * @param providerId given provider identifier for data.
     * @param sensorIdentifier given sensor identifier that will be unlinked.
     */
    @Override
    @Transactional
    public void unlinkDataToSensor(final QName dataName,
                                   final String providerId,
                                   final String sensorIdentifier) throws TargetNotFoundException {
        final Integer data = dataRepository.findIdFromProvider(
                dataName.getNamespaceURI(),
                dataName.getLocalPart(),
                providerId);
        final Integer sensor = sensorRepository.findIdByIdentifier(sensorIdentifier);
        if(data == null){
            throw new TargetNotFoundException("Cannot unlink data to sensor," +
                    " because target data is not found for" +
                    " name : "+dataName.getLocalPart()+" and provider : "+providerId);
        }
        if(sensor == null){
            throw new TargetNotFoundException("Cannot unlink data to sensor," +
                    " because target sensor is not found for" +
                    " sensorIdentifier : "+sensorIdentifier);
        }
        sensorRepository.unlinkDataToSensor(data,sensor);
    }

    /**
     * Proceed to remove the link between data and sensor.
     *
     * @param dataId given data name to find the data instance.
     * @param sensorId given sensor identifier that will be unlinked.
     */
    @Override
    @Transactional
    public void unlinkDataToSensor(final int dataId, final int sensorId) {
        sensorRepository.unlinkDataToSensor(dataId, sensorId);
    }

    @Override
    @Transactional
    public Sensor create(final String identifier, final String type, final String parent, final Object metadata, final Long date, final Integer providerID) throws ConfigurationException {
        // look for already existing sensor
        if (sensorRepository.existsByIdentifier(identifier)) {
            throw new ConfigurationException("the Sensor is already registered in the system");
        }

        Optional<CstlUser> user = userBusiness.findOne(securityManager.getCurrentUserLogin());
        Sensor sensor = new Sensor();
        sensor.setIdentifier(identifier);
        sensor.setType(type);
        if(user.isPresent()) {
            sensor.setOwner(user.get().getId());
        }
        sensor.setParent(parent);
        if (date != null) {
            sensor.setDate(new Date(date));
        }
        sensor.setProviderId(providerID);
        sensor.setProfile(getTemplateFromType(type));

        sensor = sensorRepository.create(sensor);
        if (metadata != null) {
            updateSensorMetadata(identifier, metadata);
        }
        return sensor;
    }

    @Override
    @Transactional
    public void update(Sensor sensor) {
        sensorRepository.update(sensor);
    }

    @Override
    public List<Sensor> getByProviderId(int providerId) {
        return sensorRepository.findByProviderId(providerId);
    }

    @Override
    public List<Sensor> getByServiceId(String serviceID) {
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        if (service != null) {
            return sensorRepository.findByServiceId(service);
        }
        return new ArrayList<>();
    }

    @Override
    public List<SensorReference> getByDataId(int dataId) {
        return sensorRepository.fetchByDataId(dataId);
    }

    @Override
    public int getCountByServiceId(String serviceID) {
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        if (service != null) {
            return sensorRepository.getLinkedSensorCount(service);
        }
        return 0;
    }

    @Override
    public Object getSensorMetadata(String sensorID) throws ConfigurationException {
        final Integer sensor = sensorRepository.findIdByIdentifier(sensorID);
        if (sensor != null) {
            return getSensorMetadata(sensor);
        }
        return null;
    }

    @Override
    public Object getSensorMetadata(Integer sensorID) throws ConfigurationException {
        final Sensor sensor = sensorRepository.findById(sensorID);
        if (sensor != null) {
            final DataProvider provider = DataProviders.getProvider(sensor.getProviderId());
            Data data = provider.get(NamesExt.create(sensor.getIdentifier()));
            if (data instanceof SensorData) {
                return ((SensorData)data).getSensorMetadata();
            }
        }
        return null;
    }

    @Override
    public Object getSensorMetadata(String sensorID, String serviceID) throws ConfigurationException {
        final Sensor sensor   = sensorRepository.findByIdentifier(sensorID);
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        if (sensor != null && service != null) {
            if (sensorRepository.isLinkedSensorToSOS(sensor.getId(), service)) {
                return getSensorMetadata(sensor.getId());
            }
        }
        return null;
    }

    @Override
    public void updateSensorMetadata(String sensorID, Object sensorMetadata) throws ConfigurationException {
        final Integer sensor = sensorRepository.findIdByIdentifier(sensorID);
        if (sensor != null) {
            updateSensorMetadata(sensor, sensorMetadata);
        }
    }

    @Override
    public void updateSensorMetadata(Integer sensorID, Object sensorMetadata) throws ConfigurationException {
        final Sensor sensor = sensorRepository.findById(sensorID);
        if (sensor != null) {
            final DataProvider provider = DataProviders.getProvider(sensor.getProviderId());
            if (provider instanceof SensorProvider) {
                try {
                    if (sensorMetadata instanceof String) {
                        sensorMetadata =  unmarshallSensor((String) sensorMetadata);
                    }
                    ((SensorProvider)provider).writeSensor(sensor.getIdentifier(), sensorMetadata);
                } catch (ConstellationStoreException | JAXBException | IOException ex) {
                    throw new ConfigurationException(ex);
                }
            } else {
                throw new ConfigurationException("the provider" + sensor.getProviderId() + " is not a sensor Provider");
            }
        }
    }

    @Override
    public Integer getDefaultInternalProviderID() throws ConfigurationException {
        Integer provider = providerBusiness.getIDFromIdentifier("default-internal-sensor");
        if (provider == null) {
            // TODO fill missing parameters
            final DataStoreProvider factory = DataStores.getProviderById("cstlsensor");
            if (factory != null) {
                ParameterValueGroup params = factory.getOpenParameters().createValue();
                provider = providerBusiness.create("default-internal-sensor", SPI_NAMES.SENSOR_SPI_NAME, params);
                if (provider == null) {
                    throw new ConfigurationException("Fail to create default internal sensor provider");
                }
            } else {
                throw new ConfigurationException("Fail to create default internal sensor provider: no factory found");
            }
        }
        return provider;
    }

    @Override
    public Object unmarshallSensor(final java.nio.file.Path f) throws JAXBException, IOException {
        try (InputStream stream = Files.newInputStream(f)) {
            final Unmarshaller um = SensorMLMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = um.unmarshal(stream);
            SensorMLMarshallerPool.getInstance().recycle(um);
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement) obj).getValue();
            }
            return  obj;
        }
    }

    @Override
    public Object unmarshallSensor(final String xml) throws JAXBException, IOException {
        final Unmarshaller um = SensorMLMarshallerPool.getInstance().acquireUnmarshaller();
        Object obj = um.unmarshal(new StringReader(xml));
        SensorMLMarshallerPool.getInstance().recycle(um);
        if (obj instanceof JAXBElement) {
            obj = ((JAXBElement) obj).getValue();
        }
        return obj;
    }

    @Override
    public String marshallSensor(Object sensorMetadata) throws JAXBException, IOException {
        final Marshaller m = SensorMLMarshallerPool.getInstance().acquireMarshaller();
        final StringWriter sw = new StringWriter();
        m.marshal(sensorMetadata, sw);
        SensorMLMarshallerPool.getInstance().recycle(m);
        return sw.toString();
    }

    /**
     * Return a free sensor identifier for the specified sensor provider.
     *
     * @param providerID Identifier of a sensor provider.
     *
     * @return A free sensor identifier.
     * @throws ConfigurationException
     */
    @Override
    public String getNewSensorId(Integer providerID) throws ConfigurationException {
        final DataProvider provider = DataProviders.getProvider(providerID);
        if (provider instanceof SensorProvider) {
            try {
                return ((SensorProvider) provider).getNewSensorId();
            } catch (ConstellationStoreException ex) {
                throw new ConfigurationException(ex);
            }
        } else {
            throw new ConfigurationException("the provider" + providerID + " is not a sensor Provider");
        }
    }

    /**
     * Return all the sensor identifiers for the specified SOS service.
     *
     * @param serviceID identifier of the SOS service.
     *
     * @return All the sensor identifiers for the specified SOS service.
     * @throws ConfigurationException
     */
    @Override
    public List<String> getLinkedSensorIdentifiers(String serviceID) throws ConfigurationException {
        List<String> results = new ArrayList<>();
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        if (service != null) {
            results.addAll(sensorRepository.getLinkedSensorIdentifiers(service, null));
        }
        return results;
    }

    /**
     * Return all the sensor identifiers for the specified SOS service.
     *
     * @param serviceID identifier of the SOS service.
     * @param sensorType filter on the type of sensor.
     *
     * @return All the sensor identifiers for the specified SOS service.
     * @throws ConfigurationException
     */
    @Override
    public List<String> getLinkedSensorIdentifiers(String serviceID, String sensorType) throws ConfigurationException {
        List<String> results = new ArrayList<>();
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        if (service != null) {
            results.addAll(sensorRepository.getLinkedSensorIdentifiers(service, sensorType));
        }
        return results;
    }

    @Override
    public Map<String, List<String>> getAcceptedSensorMLFormats(String serviceID) throws ConfigurationException {
        final Map<String, List<String>> results = new HashMap<>();
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        if (service != null) {
            final List<Integer> providers = serviceRepository.getLinkedSensorProviders(service);
            for (Integer providerID : providers) {
                final DataProvider provider = DataProviders.getProvider(providerID);
                if (provider instanceof SensorProvider) {
                    final Map<String, List<String>> formats = ((SensorProvider) provider).getAcceptedSensorMLFormats();

                    // merge the results
                    for (Entry<String, List<String>> entry : formats.entrySet()) {
                        if (results.containsKey(entry.getKey())) {
                            List<String> resFormats = results.get(entry.getKey());
                            for (String newFormat : entry.getValue()) {
                                if (!resFormats.contains(newFormat)) {
                                    resFormats.add(newFormat);
                                }
                            }
                        } else {
                            results.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                        }
                    }

                }
            }
        }
        return results;
    }

    @Override
    @Transactional
    public void addSensorToSOS(String serviceID, String sensorID) {
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        final Integer sensor   = sensorRepository.findIdByIdentifier(sensorID);
        if (service != null && sensor != null) {
            sensorRepository.linkSensorToSOS(sensor, service);
        } else if (service == null) {
            LOGGER.log(Level.WARNING, "Unexisting service:{0}", serviceID);
        } else if (sensor == null) {
            LOGGER.log(Level.WARNING, "Unexisting sensor:{0}", sensorID);
        }
    }

    @Override
    @Transactional
    public void removeSensorFromSOS(String serviceID, String sensorID) {
        final Integer service = serviceRepository.findIdByIdentifierAndType(serviceID, "sos");
        final Integer sensor   = sensorRepository.findIdByIdentifier(sensorID);
        if (service != null && sensor != null) {
            sensorRepository.unlinkSensorFromSOS(sensor, service);
        } else if (service == null) {
            LOGGER.log(Level.WARNING, "Unexisting service:{0}", serviceID);
        } else if (sensor == null) {
            LOGGER.log(Level.WARNING, "Unexisting sensor:{0}", sensorID);
        }
    }

    @Override
    public SensorMLTree getFullSensorMLTree() {
        final List<SensorMLTree> values = new ArrayList<>();
        final List<Sensor> sensors = sensorRepository.findAll();
        for (final Sensor sensor : sensors) {
            final Optional<CstlUser> optUser = userBusiness.findById(sensor.getOwner());
            String owner = null;
            if(optUser!=null && optUser.isPresent()){
                final CstlUser user = optUser.get();
                if(user != null){
                    owner = user.getLogin();
                }
            }
            final SensorMLTree t = new SensorMLTree(sensor.getId(), sensor.getIdentifier(), sensor.getType(), owner, sensor.getDate());
            final List<SensorMLTree> children = new ArrayList<>();
            final List<Sensor> records = sensorRepository.getChildren(sensor.getIdentifier());
            for (final Sensor record : records) {
                final Optional<CstlUser> optUserChild = userBusiness.findById(sensor.getOwner());
                String ownerChild = null;
                if(optUserChild!=null && optUserChild.isPresent()){
                    final CstlUser user = optUserChild.get();
                    if(user != null){
                        ownerChild = user.getLogin();
                    }
                }
                children.add(new SensorMLTree(record.getId(), record.getIdentifier(), record.getType(), ownerChild, record.getDate()));
            }
            t.setChildren(children);
            values.add(t);
        }
        return SensorMLTree.buildTree(values);
    }

    private String getTemplateFromType(String type) {
        final String templateName;
        if ("system".equalsIgnoreCase(type)) {
            templateName = "profile_sensorml_system";
        } else if ("component".equalsIgnoreCase(type)) {
            templateName = "profile_sensorml_component";
        } else {
            templateName = "profile_sensorml_system";
        }
        return templateName;
    }

}
