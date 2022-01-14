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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.xml.bind.Marshaller;
import org.apache.sis.storage.DataStoreProvider;
import org.constellation.admin.util.MetadataUtilities;
import static org.constellation.api.ProviderConstants.INTERNAL_SENSOR_PROVIDER;
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
import org.constellation.dto.service.config.sos.ProcedureTree;
import org.constellation.exception.ConstellationException;
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

    protected static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

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
    public List<Sensor> getChildren(final Integer parentId) {
        Sensor s = getSensor(parentId);
        if (s != null) {
            return sensorRepository.getChildren(s.getIdentifier());
        }
        return new ArrayList<>();
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
        if (sensor != null) {
            boolean rmFromDb = true;
            if (sensor.getProviderId() != null) {
                final DataProvider provider = DataProviders.getProvider(sensor.getProviderId());
                rmFromDb = provider.remove(NamesExt.create(sensor.getIdentifier()));
            }
            if (rmFromDb) {
                sensorRepository.unlinkSensorFromAllServices(sensor.getId());
                sensorRepository.delete(sensor.getIdentifier());
                return true;
            }
        }
        return false;
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
    public void deleteFromProvider(Integer providerId) throws ConfigurationException {
        List<Sensor> sensors = sensorRepository.findByProviderId(providerId);
        for (Sensor sensor : sensors) {
            delete(sensor.getIdentifier());
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
        if (!sensorRepository.isLinkedDataToSensor(dataId, sensorId)) {
            sensorRepository.linkDataToSensor(dataId, sensorId);
        }
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
    public Integer create(final String identifier, final String name, final String description, final String type, final String omType, final String parent, final Object metadata, final Long date, Integer providerID) throws ConfigurationException {
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
        if (providerID == null) {
            providerID = getDefaultInternalProviderID();
        }
        sensor.setProviderId(providerID);
        sensor.setProfile(getTemplateFromType(type));
        sensor.setOmType(omType);
        sensor.setName(name);
        sensor.setDescription(description);

        Integer sid= sensorRepository.create(sensor);
        if (metadata != null) {
            updateSensorMetadata(identifier, metadata);
        }
        return sid;
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
    public List<Sensor> getByServiceId(Integer serviceID) {
        return sensorRepository.findByServiceId(serviceID, null);
    }

    @Override
    public List<SensorReference> getByDataId(int dataId) {
        return sensorRepository.fetchByDataId(dataId);
    }

    @Override
    public int getCountByServiceId(Integer serviceID) {
        return sensorRepository.getLinkedSensorCount(serviceID);
    }

    @Override
    public Object getSensorMetadata(String sensorID) throws ConstellationException {
        final Integer sensor = sensorRepository.findIdByIdentifier(sensorID);
        if (sensor != null) {
            return getSensorMetadata(sensor);
        }
        return null;
    }

    @Override
    public Object getSensorMetadata(Integer sensorID) throws ConstellationException {
        final Sensor sensor = sensorRepository.findById(sensorID);
        if (sensor != null) {
            final DataProvider provider = DataProviders.getProvider(sensor.getProviderId());
            Data data = provider.get(null, sensor.getIdentifier());
            if (data instanceof SensorData) {
                return ((SensorData)data).getSensorMetadata();
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
            if (sensor.getProviderId() != null) {
                final DataProvider provider = DataProviders.getProvider(sensor.getProviderId());
                if (provider instanceof SensorProvider) {
                    try {
                        if (sensorMetadata instanceof String) {
                            sensorMetadata =  unmarshallSensor((String) sensorMetadata);
                        }
                        ((SensorProvider)provider).writeSensor(sensor.getIdentifier(), sensorMetadata);
                    } catch (ConstellationException ex) {
                        throw new ConfigurationException(ex);
                    }
                } else {
                    throw new ConfigurationException("the provider" + sensor.getProviderId() + " is not a sensor Provider");
                }
            } else {
                throw new ConfigurationException("the sensor " + sensor.getIdentifier()+ " has no assigned provider");
            }
        }
    }

    @Override
    public Integer getDefaultInternalProviderID() throws ConfigurationException {
        Integer provider = providerBusiness.getIDFromIdentifier(INTERNAL_SENSOR_PROVIDER);
        if (provider == null) {
            // TODO fill missing parameters
            final DataStoreProvider factory = DataStores.getProviderById("cstlsensor");
            if (factory != null) {
                ParameterValueGroup params = factory.getOpenParameters().createValue();
                provider = providerBusiness.create(INTERNAL_SENSOR_PROVIDER, SPI_NAMES.SENSOR_SPI_NAME, params);
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
    public Object unmarshallSensor(final java.nio.file.Path f) throws ConstellationException {
        try (InputStream stream = Files.newInputStream(f)) {
            final Unmarshaller um = SensorMLMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = um.unmarshal(stream);
            SensorMLMarshallerPool.getInstance().recycle(um);
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement) obj).getValue();
            }
            return  obj;
        } catch (JAXBException | IOException ex) {
            throw new ConstellationException(ex);
        }
    }

    @Override
    public Object unmarshallSensor(final String xml) throws ConstellationException {
        try {
            final Unmarshaller um = SensorMLMarshallerPool.getInstance().acquireUnmarshaller();
            Object obj = um.unmarshal(new StringReader(xml));
            SensorMLMarshallerPool.getInstance().recycle(um);
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement) obj).getValue();
            }
            return obj;
        } catch (JAXBException ex) {
            throw new ConstellationException(ex);
        }
    }

    @Override
    public String marshallSensor(Object sensorMetadata) throws ConstellationException {
        try {
            final Marshaller m = SensorMLMarshallerPool.getInstance().acquireMarshaller();
            final StringWriter sw = new StringWriter();
            m.marshal(sensorMetadata, sw);
            SensorMLMarshallerPool.getInstance().recycle(m);
            return sw.toString();
        } catch (JAXBException ex) {
            throw new ConstellationException(ex);
        }
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
     * {@inheritDoc}
     */
    @Override
    public List<String> getLinkedSensorIdentifiers(Integer serviceID, String sensorType) throws ConfigurationException {
        return sensorRepository.findIdentifierByServiceId(serviceID, sensorType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLinkedSensor(Integer serviceID, String sensorID) {
        final Integer sid = sensorRepository.findIdByIdentifier(sensorID);
        if (serviceID != null && sid != null) {
            return sensorRepository.isLinkedSensorToService(sid, serviceID);
        }
        return false;
    }

    @Override
    public Map<String, List<String>> getAcceptedSensorMLFormats(Integer serviceID) throws ConfigurationException {
        final Map<String, List<String>> results = new HashMap<>();
        if (serviceID != null) {
            final List<Integer> providers = serviceRepository.getLinkedSensorProviders(serviceID, null);
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void addSensorToService(Integer serviceID, Integer sensorID) throws ConfigurationException {
        if (serviceID != null && sensorID != null) {
            if (sensorRepository.existsById(sensorID)) {
                if (!sensorRepository.isLinkedSensorToService(sensorID, serviceID)) {
                    sensorRepository.linkSensorToService(sensorID, serviceID);
                }
                List<Sensor> children = getChildren(sensorID);
                for (Sensor child : children) {
                    if (!sensorRepository.isLinkedSensorToService(child.getId(), serviceID)) {
                        sensorRepository.linkSensorToService(child.getId(), serviceID);
                    }
                }
            } else {
                throw new ConfigurationException("Unexisting sensor  :" + sensorID);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void removeSensorFromService(Integer serviceID, Integer sensorID) throws ConfigurationException {
        if (serviceID != null && sensorID != null) {
            Sensor s = sensorRepository.findById(sensorID);
            if (s != null) {
                removeSensorFromService(serviceID, s);
            } else {
                throw new ConfigurationException("Unexisting sensor  :" + sensorID);
            }
        }
    }

    private void removeSensorFromService(Integer serviceID, Sensor s) throws ConfigurationException {
         // if all the sensor provider is linked we must link individually all the other sensors
        if (serviceRepository.isAllLinked(serviceID, s.getProviderId())) {
            serviceRepository.linkSensorProvider(serviceID, s.getProviderId(), false);
            List<Sensor> sensors = sensorRepository.findByProviderId(s.getProviderId());
            for (Sensor ss : sensors) {
                if (!ss.getId().equals(s.getId())) {
                    sensorRepository.linkSensorToService(ss.getId(), serviceID);
                }
            }
        } else {
            sensorRepository.unlinkSensorFromService(s.getId(), serviceID);
        }

        // unlink sensor children
        List<Sensor> children = getChildren(s.getId());
        for (Sensor child : children) {
            removeSensorFromService(serviceID, child);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SensorMLTree getFullSensorMLTree() {
        return getSensorMLTree(sensorRepository.findAll());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SensorMLTree getServiceSensorMLTree(Integer id) {
        final Collection<Sensor> sensors = getByServiceId(id);
        return getSensorMLTree(sensors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SensorMLTree getSensorMLTree(String sensorId) throws TargetNotFoundException {
        Sensor s = sensorRepository.findByIdentifier(sensorId);
        if (s != null) {
            return getSensorTreeFromSensor(s);
        }
        throw new TargetNotFoundException("Unable to find a sensor:" + sensorId);
    }

    /**
     * Build a Tree of sensor from a flat sensor list.
     * Add a root node at the top.
     *
     *
     * @param sensors a flat sensor list.
     * @return
     */
    private SensorMLTree getSensorMLTree(final Collection<Sensor> sensors) {
        final List<SensorMLTree> values = new ArrayList<>();
        for (final Sensor sensor : sensors) {
            final SensorMLTree t = getSensorTreeFromSensor(sensor);
            values.add(t);
        }
        return SensorMLTree.buildTree(values, true);
    }

    private SensorMLTree getSensorTreeFromSensor(Sensor sensor) {
        final Optional<CstlUser> optUser = userBusiness.findById(sensor.getOwner());
        String owner = null;
        if(optUser.isPresent()){
            owner = optUser.get().getLogin();
        }
        final SensorMLTree t = new SensorMLTree(sensor.getId(), sensor.getIdentifier(), sensor.getName(), sensor.getDescription(), sensor.getType(), owner, sensor.getDate(), null);
        final List<SensorMLTree> children = new ArrayList<>();
        final List<Sensor> records = sensorRepository.getChildren(sensor.getIdentifier());
        for (final Sensor record : records) {
            final Optional<CstlUser> optUserChild = userBusiness.findById(record.getOwner());
            String ownerChild = null;
            if(optUserChild.isPresent()){
                ownerChild = optUserChild.get().getLogin();
            }
            children.add(new SensorMLTree(record.getId(), record.getName(), record.getDescription(), record.getIdentifier(), record.getType(), ownerChild, record.getDate(), null));
        }
        t.setChildren(children);
        return t;
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

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer generateSensorForData(final int dataID, final ProcedureTree process, Integer providerID, final String parentID) throws ConfigurationException {
        
        if (providerID == null) {
            providerID = getDefaultInternalProviderID();
        }

        final Properties prop = new Properties();
        prop.put("id",         process.getId());
        if (process.getDateStart() != null) {
            prop.put("beginTime",  process.getDateStart());
        }
        if (process.getDateEnd() != null) {
            prop.put("endTime",    process.getDateEnd());
        }
        if (process.getMinx() != null) {
            prop.put("longitude",  process.getMinx());
        }
        if (process.getMiny() != null) {
            prop.put("latitude",   process.getMiny());
        }
        prop.put("phenomenon", process.getFields());

        Sensor sensor = getSensor(process.getId());
        Integer sid;
        if (sensor == null) {
            sid = create(process.getId(), process.getName(), process.getDescription(), process.getType(), process.getOmType(), parentID, null, System.currentTimeMillis(), providerID);
        } else {
            sid = sensor.getId();
        }

        final List<String> component = new ArrayList<>();
        for (ProcedureTree child : process.getChildren()) {
            component.add(child.getId());
            generateSensorForData(dataID, child, providerID, process.getId());
        }
        prop.put("component", component);
        final String sml = MetadataUtilities.getTemplateSensorMLString(prop, process.getType());

        // update sensor metadata
        updateSensorMetadata(sid, sml);
        // link data to created sensor
        linkDataToSensor(dataID, sid);
        return sid;
    }

}
