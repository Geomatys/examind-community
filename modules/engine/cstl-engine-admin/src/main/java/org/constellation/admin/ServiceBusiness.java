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

import org.constellation.exception.ConfigurationException;
import org.constellation.exception.TargetNotFoundException;
import org.constellation.dto.service.ServiceStatus;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ServiceDef;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.contact.Details;
import org.constellation.exception.ConstellationPersistenceException;
import org.constellation.dto.CstlUser;
import org.constellation.dto.service.Service;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.ThesaurusRepository;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.security.SecurityManager;
import org.constellation.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import org.constellation.business.ClusterMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.IProviderBusiness;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.business.IConfigurationBusiness;
import org.constellation.business.IUserBusiness;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.sos.SOSConfiguration;
import org.constellation.dto.service.config.wps.ProcessContext;
import org.constellation.dto.service.config.wps.Processes;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.ws.MimeType;

@Component
@Primary
public class ServiceBusiness implements IServiceBusiness {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    @Autowired
    private SecurityManager securityManager;

    @Autowired
    private IUserBusiness userBusiness;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private IConfigurationBusiness configBusiness;

    @Autowired
    private IProviderBusiness providerBusiness;

    @Autowired
    private IClusterBusiness clusterBusiness;

    @Autowired
    private ThesaurusRepository thesaurusRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String serviceType, final String identifier, Object configuration, Details details, Integer owner)
            throws ConfigurationException {
        return create(serviceType, identifier, configuration, details, owner, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Integer create(final String serviceType, final String identifier, Object configuration, Details details, Integer owner, String impl)
            throws ConfigurationException {

        if (identifier == null || identifier.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }

        if (configuration == null) {
            configuration = getDefaultConfiguration(serviceType);
        }
        if (owner == null) {
            Optional<CstlUser> user = userBusiness.findOne(securityManager.getCurrentUserLogin());
            if (user.isPresent()) {
                owner = user.get().getId();
            }
        }

        final String config = Util.writeConfigurationObject(configuration);
        final Service service = new Service();
        service.setConfig(config);
        service.setDate(new Date());
        service.setType(ServiceDef.Specification.valueOf(serviceType.toUpperCase()).name().toLowerCase());
        if (owner != null) {
            service.setOwner(owner);
        }
        service.setIdentifier(identifier);
        service.setStatus(ServiceStatus.STOPPED.toString());
        service.setImpl(impl);

        // TODO metadata-Iso

        if (details == null) {
            final InputStream in = Util.getResourceAsStream("org/constellation/xml/" + service.getType().toUpperCase() + "Capabilities.xml");
            if (in != null) {
                try (InputStream in2 = in) {
                    details = Util.readConfigurationObject(in2, Details.class);
                    details.setIdentifier(service.getIdentifier());
                    details.setLang("eng"); // default value
                } catch (IOException ex) {
                    throw new ConfigurationException(ex);
                }
            } else {
                throw new ConfigurationException("Unable to find the capabilities skeleton from resource.");
            }
        } else if (details.getLang() == null) {
            details.setLang("eng");// default value
        }

        final String versions;
        if (details.getVersions() != null) {
            versions = StringUtils.join(details.getVersions(), "µ");
        } else {
            versions = "";
        }
        service.setVersions(versions);

        int serviceId = serviceRepository.create(service);
        setInstanceDetails(serviceType, identifier, details, details.getLang(), true);

        return serviceId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void start(final Integer id) throws ConfigurationException {
        if (id == null) {
            throw new ConfigurationException("Service instance identifier can't be null.");
        }
        final Service service = serviceRepository.findById(id);
        if (service != null) {
            service.setStatus(ServiceStatus.STARTED.toString());
            serviceRepository.update(service);

            final ClusterMessage request = clusterBusiness.createRequest(SRV_MESSAGE_TYPE_ID,false);
            request.put(KEY_ACTION, SRV_VALUE_ACTION_START);
            request.put(SRV_KEY_TYPE, service.getType());
            request.put(KEY_IDENTIFIER, service.getIdentifier());
            clusterBusiness.publish(request);
        } else {
            throw new TargetNotFoundException("Service instance with identifier \"" + id
                    + "\" not found. There is not configuration in the database.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void stop(final Integer id) throws ConfigurationException {
        if (id == null) {
            throw new ConfigurationException("Service instance identifier can't be null.");
        }

        final Service service = serviceRepository.findById(id);
        if (service != null) {
            service.setStatus(ServiceStatus.STOPPED.toString());
            serviceRepository.update(service);

            final ClusterMessage request = clusterBusiness.createRequest(SRV_MESSAGE_TYPE_ID,false);
            request.put(KEY_ACTION, SRV_VALUE_ACTION_STOP);
            request.put(SRV_KEY_TYPE, service.getType());
            request.put(KEY_IDENTIFIER, service.getIdentifier());
            clusterBusiness.publish(request);
        } else {
            throw new TargetNotFoundException("Service instance with identifier \"" + id
                    + "\" not found. There is not configuration in the database.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void restart(final Integer id) throws ConfigurationException {
        if (id == null) {
            throw new ConfigurationException("Service instance identifier can't be null.");
        }
        stop(id);
        start(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void rename(final Integer id, final String newIdentifier) throws ConfigurationException {

        final Service service = serviceRepository.findById(id);
        if(service==null) throw new ConfigurationException("no existing instance:" + id);

        final Service newService = serviceRepository.findByIdentifierAndType(newIdentifier, service.getType());
        if(newService!=null) throw new ConfigurationException("already existing instance:" + newIdentifier);

        //stop service
        stop(id);

        //rename it
        service.setIdentifier(newIdentifier);
        serviceRepository.update(service);

        //start service
        start(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(final Integer id) throws ConstellationException {
        if (id == null) {
            throw new ConfigurationException("Service instance identifier can't be null.");
        }

        final Service service = serviceRepository.findById(id);
        if (service == null) throw new ConfigurationException("There is no instance:" + id + " to delete");

        // stop services
        stop(id);

        // TODO remove special cases
        if (service.getType().equalsIgnoreCase("csw")) {
            serviceRepository.removelinkedMetadataProvider(id);
            serviceRepository.removelinkedMetadatas(id);
        } else if (service.getType().equalsIgnoreCase("sos") || service.getType().equalsIgnoreCase("sts")) {
            List<Integer> linkedProviders = serviceRepository.getLinkedSensorProviders(id, null);
            serviceRepository.removelinkedSensorProviders(id);
            serviceRepository.removelinkedSensors(id);

            // we don't want to remove the default internal sensor provider, because its shared by many services
            Integer dpid = providerBusiness.getIDFromIdentifier("default-internal-sensor");
            for (Integer linkedProviderID : linkedProviders) {
                if (!linkedProviderID.equals(dpid)) {
                    providerBusiness.removeProvider(linkedProviderID);
                }
            }
        }

        // delete from database
        serviceRepository.delete(id);
        // delete folder
        configBusiness.removeInstanceDirectory(service.getType(), service.getIdentifier());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAll() throws ConstellationException {
        final List<Service> services = serviceRepository.findAll();
        for (Service service : services) {
            delete(service.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Transactional
    @Override
    public void configure(final String serviceType, final String identifier, Details details, Object configuration)
            throws ConstellationException {
        if (identifier == null || identifier.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }

        if (configuration == null) {
            configuration = getDefaultConfiguration(serviceType);
        }

        // write configuration file.
        final Service service = serviceRepository.findByIdentifierAndType(identifier, serviceType);
        if (service == null) throw new ConfigurationException("Service " + serviceType + ':' + identifier + " not found.");

        service.setConfig(Util.writeConfigurationObject(configuration));
        if (details != null) {
            setInstanceDetails(serviceType, identifier, details, details.getLang(), true);
        } else {
            details = getInstanceDetails(service.getId(), null);
        }
        if (details != null) {
            service.setVersions(StringUtils.join(details.getVersions(), "µ"));
        }
        serviceRepository.update(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getConfiguration(final String serviceType, final String identifier) throws ConfigurationException {
        if (identifier == null || identifier.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }
        final Service service = serviceRepository.findByIdentifierAndType(identifier, serviceType);
        if (service != null) {
            return Util.readConfigurationObject(service.getConfig(), Object.class);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getConfiguration(final int id) throws ConfigurationException {
        final Service service = serviceRepository.findById(id);
        if (service != null) {
            return Util.readConfigurationObject(service.getConfig(), Object.class);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void setConfiguration(final String serviceType, final String identifier, final Object config) throws ConfigurationException {
        if (identifier == null || identifier.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }
        try {
            final Service service = serviceRepository.findByIdentifierAndType(identifier, serviceType);
            if (service != null) {
                final String confXml = Util.writeConfigurationObject(config);
                service.setConfig(confXml);
                serviceRepository.update(service);
            }
        } catch (ConstellationPersistenceException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void setConfiguration(final Integer id, final Object config) throws ConfigurationException {
        if (id == null) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }
        try {
            final Service service = serviceRepository.findById(id);
            if (service != null) {
                final String confXml = Util.writeConfigurationObject(config);
                service.setConfig(confXml);
                serviceRepository.update(service);
            }
        } catch (ConstellationPersistenceException ex) {
            throw new ConfigurationException(ex.getMessage(), ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getExtraConfiguration(final String serviceType, final String identifier, final String fileName) throws ConfigurationException {
        return getExtraConfiguration(serviceType, identifier, fileName, GenericDatabaseMarshallerPool.getInstance());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getExtraConfiguration(final String serviceType, final String identifier, final String fileName, final MarshallerPool pool)
            throws ConfigurationException {
        final Integer service = serviceRepository.findIdByIdentifierAndType(identifier, serviceType);
        if (service != null) {
            final String content = serviceRepository.getExtraConfig(service, fileName);
            if (content != null) {
                return Util.readConfigurationObject(content, Object.class, pool);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void setExtraConfiguration(final String serviceType, final String identifier, final String fileName, final Object config,
            final MarshallerPool pool) throws ConstellationException {
        final Integer serviceId = serviceRepository.findIdByIdentifierAndType(identifier, serviceType);
        if (serviceId != null) {
            final String content = Util.writeConfigurationObject(config, pool);
            serviceRepository.updateExtraFile(serviceId, fileName, content);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, ServiceStatus> getStatus(final String spec) {
        final List<Service> services = serviceRepository.findByType(spec);
        final Map<Integer, ServiceStatus> status = new HashMap<>();
        for (Service service : services) {
            status.put(service.getId(), ServiceStatus.valueOf(service.getStatus()));
        }
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getServiceIdentifiers(final String spec) {
        return serviceRepository.findIdentifiersByType(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceComplete getServiceById(int id, String lang) {
        final Service service = serviceRepository.findById(id);
        if (service != null) {
            Details details = null;
            try {
                details = getInstanceDetails(id, lang);
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.INFO, "Error while reading service details", ex);
            }
            return new ServiceComplete(service, details);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceComplete getServiceByIdentifierAndType(String serviceType, String identifier) {
        final Service service = serviceRepository.findByIdentifierAndType(identifier, serviceType);
        if (service != null) {
            Details details = null;
            try {
                details = getInstanceDetails(service.getId(), null);
            } catch (ConfigurationException ex) {
                LOGGER.log(Level.INFO, "Error while reading service details", ex);
            }
            return new ServiceComplete(service, details);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getServiceIdByIdentifierAndType(String serviceType, String identifier) {
        return serviceRepository.findIdByIdentifierAndType(identifier, serviceType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Details getInstanceDetails(final String serviceType, final String identifier, final String language)
            throws ConfigurationException {
        final Integer id = serviceRepository.findIdByIdentifierAndType(identifier, serviceType);
        if (id == null) {
            throw new TargetNotFoundException(serviceType + " service instance with identifier \"" + identifier
                    + "\" not found. There is not configuration in the database.");
        }
        return getInstanceDetails(id, language);
    }

    /**
     * Return the service description in the requested language.
     * if the requested language is not available, we return the default one.
     *
     * @param serviceId Service identifier.
     * @param language Request language or {@code null} to get the default one.
     *
     * @return A service description.
     * @throws ConfigurationException if the XML object reading fail.
     */
    private Details getInstanceDetails(final int serviceId, String language) throws ConfigurationException {
        String details;
        if (language == null) {
            details = serviceRepository.getServiceDetailsForDefaultLang(serviceId);
        } else {
            if (serviceRepository.getServiceDefinedLanguage(serviceId).contains(language)) {
                details = serviceRepository.getServiceDetails(serviceId, language);
            } else {
                details = serviceRepository.getServiceDetailsForDefaultLang(serviceId);
            }
        }
        if (details != null) {
            return Util.readConfigurationObject(details, Details.class);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void setInstanceDetails(final String serviceType, final String identifier, final Details details, final String language,
            final boolean default_) throws ConfigurationException {
        final Integer id = serviceRepository.findIdByIdentifierAndType(identifier, serviceType);
        if (id == null) {
            throw new TargetNotFoundException(serviceType + " service instance with identifier \"" + identifier
                    + "\" not found. There is not configuration in the database.");
        }
        final String xml = Util.writeConfigurationObject(details);
        serviceRepository.createOrUpdateServiceDetails(id, language, xml, default_);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureExistingInstance(final String spec, final String identifier) throws ConfigurationException {
        final Integer service = serviceRepository.findIdByIdentifierAndType(identifier, spec);
        if (service == null) {
            throw new TargetNotFoundException(spec + " service instance with identifier \"" + identifier
                    + "\" not found. There is not configuration in the database.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ensureExistingInstance(final Integer id) throws ConfigurationException {
        if (!serviceRepository.existsById(id)) {
            throw new TargetNotFoundException(id + " service instance not found.");
        }
    }

    @Override
    public List<ServiceComplete> getAllServices(String lang) throws ConstellationException {
        List<ServiceComplete> serviceDTOs = new ArrayList<>();
        List<Service> services = serviceRepository.findAll();
        for (Service service : services) {
            final Details details = getInstanceDetails(service.getId(), lang);
            final ServiceComplete serviceDTO = new ServiceComplete(service, details);
            serviceDTOs.add(serviceDTO);
        }
        return serviceDTOs;
    }

    @Override
    public List<ServiceComplete> getAllServicesByType(String lang, String type) throws ConstellationException {
        List<ServiceComplete> serviceDTOs = new ArrayList<>();
        List<Service> services = serviceRepository.findByType(type);
        for (Service service : services) {
            final Details details = getInstanceDetails(service.getId(), lang);
            final ServiceComplete serviceDTO = new ServiceComplete(service, details);
            serviceDTOs.add(serviceDTO);
        }
        return serviceDTOs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getLinkedProviders(Integer serviceID) {
        return serviceRepository.getLinkedSensorProviders(serviceID, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void linkServiceAndSensorProvider(Integer serviceID, Integer providerID, boolean fullLink) {
        if (serviceID != null && providerID != null) {
            serviceRepository.linkSensorProvider(serviceID, providerID, fullLink);
        }
    }

    @Override
    public List<Integer> getCSWLinkedProviders(String identifier) {
        final Integer serviceID = serviceRepository.findIdByIdentifierAndType(identifier, "csw");
        if (serviceID != null) {
            return serviceRepository.getLinkedMetadataProvider(serviceID).stream().map(lp -> lp.getId()).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<Service> getSensorLinkedServices(Integer sensorID) throws ConfigurationException {
        return serviceRepository.getSensorLinkedSensorServices(sensorID);
    }

    @Override
    public List<Service> getDataLinkedSensorServices(int dataId) {
        return serviceRepository.getDataLinkedSensorServices(dataId);
    }

    @Override
    public List<Service> getProviderLinkedSensorServices(Integer providerId) {
        return serviceRepository.getProviderLinkedSensorServices(providerId);
    }

    @Override
    @Transactional
    public void linkCSWAndProvider(Integer serviceID, Integer providerID, boolean allEntry) {
        if (serviceID != null && providerID != null) {
            serviceRepository.linkMetadataProvider(serviceID, providerID, allEntry);
        }
    }

    @Override
    public List<String> getLinkedThesaurusUri(Integer serviceId) throws ConfigurationException {
        return thesaurusRepository.getLinkedThesaurusUri(serviceId);
    }

    @Override
    public String getServiceImplementation(Integer serviceId) {
        return serviceRepository.getImplementation(serviceId);
    }

    @Override
    public Object getDefaultConfiguration(final String serviceType) {
        return switch(serviceType.toLowerCase()) {
            case "csw" ->  new Automatic();
            case "wps" -> new ProcessContext(new Processes(true));
            case "sos", "sts" -> new SOSConfiguration();
            // other case assume WXS
            default -> new LayerContext(createGenericConfiguration());
        };
    }

    /**
     * Create the default {@link GetFeatureInfoCfg} list to configure a LayerContext.
     * This list is build from generic {@link FeatureInfoFormat} and there supported mimetype.
     * HTMLFeatureInfoFormat, CSVFeatureInfoFormat, GMLFeatureInfoFormat
     *
     * @return a list of {@link GetFeatureInfoCfg}
     */
    private static List<GetFeatureInfoCfg> createGenericConfiguration () {
        //Default featureInfo configuration
        final List<GetFeatureInfoCfg> featureInfos = new ArrayList<>();

        //HTML
        featureInfos.add(new GetFeatureInfoCfg(MimeType.TEXT_HTML, "org.constellation.map.featureinfo.HTMLFeatureInfoFormat"));

        //CSV
        featureInfos.add(new GetFeatureInfoCfg(MimeType.TEXT_PLAIN, "org.constellation.map.featureinfo.CSVFeatureInfoFormat"));

        //GML
        featureInfos.add(new GetFeatureInfoCfg(MimeType.APP_GML, "org.constellation.map.featureinfo.GMLFeatureInfoFormat"));//will return map server GML
        featureInfos.add(new GetFeatureInfoCfg(MimeType.APP_GML_XML, "org.constellation.map.featureinfo.GMLFeatureInfoFormat"));//will return GML 3

        //XML
        featureInfos.add(new GetFeatureInfoCfg(MimeType.APP_XML, "org.constellation.map.featureinfo.XMLFeatureInfoFormat"));
        featureInfos.add(new GetFeatureInfoCfg(MimeType.TEXT_XML, "org.constellation.map.featureinfo.XMLFeatureInfoFormat"));

        //JSON
        featureInfos.add(new GetFeatureInfoCfg(MimeType.APP_JSON, "org.constellation.map.featureinfo.JSONFeatureInfoFormat"));
        featureInfos.add(new GetFeatureInfoCfg(MimeType.APP_JSON_UTF8, "org.constellation.map.featureinfo.JSONFeatureInfoFormat"));

        //Examind specific for coverages
        featureInfos.add(new GetFeatureInfoCfg("application/json; subtype=profile", "org.constellation.map.featureinfo.CoverageProfileInfoFormat"));

        return featureInfos;
    }
}
