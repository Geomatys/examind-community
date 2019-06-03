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
package org.constellation.business;

import java.util.List;
import java.util.Map;

import org.apache.sis.xml.MarshallerPool;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.contact.Details;
import org.constellation.dto.service.Service;
import org.constellation.dto.service.ServiceComplete;

/**
 * @author Cédric Briançon (Geomatys)
 */
public interface IServiceBusiness {

    /**
     * Stops a service instance.
     *
     * @param serviceType The service type (WMS, WFS, ...)
     * @param identifier The service identifier.
     *
     * @throws ConfigurationException if the operation has failed for any reason
     */
    void stop(String serviceType, String identifier) throws ConfigurationException;

    /**
     * Configures a service instance.
     *
     * @param serviceType The service type (WMS, WFS, ...)
     * @param identifier The service identifier.
     * @param configuration The service configuration (depending on implementation).
     * @param serviceMetadata The service metadata.
     *
     * @throws ConfigurationException if the operation has failed for any reason
     */
    void configure(String serviceType, String identifier, Details serviceMetadata, Object configuration) throws ConstellationException;

    /**
     * Starts a service instance.
     *
     * @param serviceType
     *            The service type (WMS, WFS, ...)
     * @param identifier
     *            the service identifier
     * @throws ConfigurationException
     *             if the operation has failed for any reason
     */
    void start(String serviceType, String identifier) throws ConfigurationException;

    /**
     * Restarts a service instance.
     *
     * @param serviceType
     *            The service type (WMS, WFS, ...)
     * @param identifier
     *            the service identifier
     * @param closeFirst @deprecated not used anymore
     * @throws ConfigurationException
     *             if the operation has failed for any reason
     */
    void restart(String serviceType, String identifier, boolean closeFirst) throws ConfigurationException;

    /**
     * Renames a service instance.
     *
     * @param serviceType
     *            The service type (WMS, WFS, ...)
     * @param identifier
     *            the current service identifier
     * @param newIdentifier
     *            the new service identifier
     * @throws ConfigurationException
     *             if the operation has failed for any reason
     */
    void rename(String serviceType, String identifier, String newIdentifier) throws ConfigurationException;

    /**
     * Deletes a service instance.
     *
     * @param serviceType
     *            The service type (WMS, WFS, ...)
     * @param identifier
     *            the service identifier
     * @throws ConfigurationException
     *             if the operation has failed for any reason
     */
    void delete(String serviceType, String identifier) throws ConfigurationException;

    /**
     * Ensure that a service instance really exists.
     *
     * @param spec The service type.
     * @param identifier The service identifier
     *
     * @throws ConfigurationException If the service with specified identifier does not exist
     */
    void ensureExistingInstance(String spec, String identifier) throws ConfigurationException;

    /**
     * Ensure that a service instance really exists.
     *
     * @param id The service identifier
     *
     * @throws ConfigurationException If the service with specified identifier does not exist
     */
    void ensureExistingInstance(Integer id) throws ConfigurationException;

    /**
     * Returns the configuration object of a service instance.
     *
     * @param serviceType The service type (WMS, WFS, ...)
     * @param identifier the service name.
     *
     * @return a configuration {@link Object} (depending on implementation)
     * @throws ConfigurationException
     *             if the operation has failed for any reason
     */
    Object getConfiguration(String serviceType, String identifier) throws ConfigurationException;

    /**
     * Returns the configuration object of a service instance.
     *
     * @param id The service identifier.
     *
     * @return a configuration {@link Object} (depending on implementation)
     * @throws ConfigurationException if the operation has failed for any reason
     */
    Object getConfiguration(int id) throws ConfigurationException;

    /**
     * Update the configuration object of a service instance.
     *
     * @param serviceType The service type (WMS, WFS, ...)
     * @param identifier the service name.
     * @param configuration The configuration Object of the service.
     *
     * @throws ConfigurationException if the operation has failed for any reason
     */
    void setConfiguration(String serviceType, String identifier, Object configuration) throws ConfigurationException;

    /**
     * Create a new service instance from input information.
     * @param serviceType Type of service to instantiate (CSW, WMS, etc.)
     * @param identifier The name to give to the service.
     * @param configuration An optional configuration specific to the queried type of service (WPS -- ProcessContext, etc.).
     * @param serviceMetadata An ISO 19115-2 metadata file to describe the service. If null a default empty metadata will be created.
     * @param owner the owner id, or {@code null} if you want to use the current logged user.
     *
     * @return
     * @throws ConfigurationException
     */
    Object create(String serviceType, String identifier, Object configuration, Details serviceMetadata, Integer owner) throws ConfigurationException;

    List<String> getServiceIdentifiers(String type);

    /**
     * Try to retrieve a service bu its id.
     *
     * @param id The identifier of the service to return.
     * @return A service of the queried id, or null if we cannot find any.
     */
    ServiceComplete getServiceById(int id);

    /**
     * Try to retrieve a service of the given type, using its name.
     *
     * @param type Type of the service (WMTS, WPS, etc.) we search.
     * @param id The name of the service to return.
     * @return A service of the queried type matching input name, or null if we cannot find any.
     */
    ServiceComplete getServiceByIdentifierAndType(String type, String id);

    /**
     * Try to retrieve a service Id using its name and type.
     *
     * @param type Type of the service (WMTS, WPS, etc.) we search.
     * @param id The name of the service to return.
     * @return A service Id, or null if we cannot find any.
     */
    Integer getServiceIdByIdentifierAndType(String type, String id);

    /**
     * Returns a service instance metadata in the specified language.
     *
     * @param serviceType The type of the service.
     * @param identifier The service identifier
     * @param language The language of the metadata object.
     * @return The service metadatas
     *
     * @throws ConfigurationException if the operation has failed for any reason
     */
    Details getInstanceDetails(String serviceType, String identifier, String language) throws ConfigurationException;

    /**
     * Return an extra configuration file for the service.
     *
     * @param serviceType Type of the service (WMTS, WPS, etc.).
     * @param identifier The name of the service.
     * @param fileName The file name of extra configuration object.
     *
     * @return The unmarshalled Object content of the extra configuration file.
     * @throws ConfigurationException
     */
    Object getExtraConfiguration(String serviceType, String identifier, String fileName) throws ConfigurationException;

    /**
     * Return an extra configuration file for the service.
     *
     * @param serviceType Type of the service (WMTS, WPS, etc.).
     * @param identifier The name of the service.
     * @param pool MarshallerPool used to read the content of the extra configuration file.
     * @param fileName The file name of extra configuration object.
     *
     * @return The unmarshalled Object content of the extra configuration file.
     * @throws ConstellationException
     */
    Object getExtraConfiguration(final String serviceType, final String identifier, final String fileName, final MarshallerPool pool) throws ConstellationException;

    /**
     * Save an extra configuration file for the service.
     *
     * @param serviceType Type of the service (WMTS, WPS, etc.).
     * @param identifier The name of the service.
     * @param pool MarshallerPool used to write the content of the extra configuration file.
     * @param config The configuration object to store
     * @param fileName The file name of extra configuration object.
     *
     */
    void setExtraConfiguration(String serviceType, String identifier, String fileName, Object config, MarshallerPool pool);

    /**
     * Delete all the services.
     *
     * @throws ConstellationException
     */
    void deleteAll() throws ConstellationException;

    /**
     * Returns all service instances (for current specification) status.
     *
     * @param spec The service type (WMS, CSW, ...)
     * @return a {@link Map} of {@link ServiceStatus} status
     */
    Map<Integer,ServiceStatus> getStatus(String spec);

    Instance getI18nInstance(String serviceType, String identifier, String lang) throws ConstellationException;

    List<org.constellation.dto.service.ServiceComplete> getAllServices(String lang) throws ConstellationException;

    List<org.constellation.dto.service.ServiceComplete> getAllServicesByType(String lang, String type) throws ConstellationException;

    /**
     * Updates a service instance metadata.
     *
     * @param serviceType The type of the service.
     * @param identifier The service identifier
     * @param details The service metadata
     * @param language The language of the metadata object
     * @param default_ True if this is the default language.
     *
     * @throws ConfigurationException If the operation has failed for any reason
     */
    void setInstanceDetails(String serviceType, String identifier, Details details, String language,
                            boolean default_) throws ConfigurationException;

    List<Integer> getSOSLinkedProviders(final String serviceID);

    List<Service> getSensorLinkedServices(final Integer sensorID) throws ConfigurationException;

    void linkSOSAndProvider(final String serviceID, final String providerID);

    Integer getCSWLinkedProviders(final String serviceID);

    void linkCSWAndProvider(String serviceID, String providerID);

    List<String> getLinkedThesaurusUri(Integer id) throws ConfigurationException;
}
