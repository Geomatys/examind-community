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
package org.constellation.ws;

import java.util.Map;
import java.util.Set;
import org.constellation.api.ServiceDef;
import org.constellation.api.WorkerState;
import org.constellation.dto.service.ServiceProtocol;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.NotRunningServiceException;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public interface IWSEngine {

    /**
     * Return a map of the registred OGC services and their endpoint protocols (REST,...).
     *
     * @return
     */
    Map<String, ServiceProtocol> getRegisteredServices();

    /**
     * Add a service type to the list of registered service if it is not already registered.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param protocol A service protocol (REST,...)
     */
    void registerService(final String specification, final ServiceProtocol protocol);

    /**
     * Return true if the correspounding service is already registered.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).

     * @return
     */
    boolean isSetService(final String specification);

    /**
     * Instanciate a new {@link Worker} for the specified OGC service.
     *
     * @param specification The OGC service type (WMS, CSW, WFS, ...).
     * @param identifier The identifier of the new {@link Worker}.
     *
     * @return The new instancied {@link Worker}.
     */
    Worker buildWorker(final String specification, final String identifier) throws ConstellationException;

    /**
     * Add a new Worker instance to the registered worker pool.
     *
     * @param specification The OGC service type (WMS, CSW, WFS, ...).
     * @param serviceID The identifier of the new {@link Worker}.
     * @param instance The new {@link Worker}.
     */
    void addServiceInstance(final String specification, final String serviceID, final Worker instance);

    /**
     * Destroy all the service instances for the specified OGC service.
     *
     * @param specification The OGC service type (WMS, CSW, WFS, ...).
     */
    void destroyInstances(final String specification);

    /**
     * Return the service instance identifier for the specified service type.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).

     * @return
     */
    Set<String> getInstanceNames(final String specification);

    /**
     * Return the number of instances for the specified OGC service.
     *
     * @param specification the OGC service type (WMS, CSW, WFS, ...)
     * @return
     */
    int getInstanceSize(final String specification);

    /**
     * Return true if the correspounding service instance is registered.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param serviceID The identifier of the new {@link Worker}.
     *
     * @return
     */
    boolean serviceInstanceExist(final String specification, final String serviceID);

    /**
     * Return a map of the service instances workers for the specified service type.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).

     * @return
     */
    Map<String, Worker> getWorkersMap(final String specification);

    /**
     * Gets the {@link ServiceConfigurer} implementation from the service {@link ServiceDef.Specification}.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     *
     * @return the {@link ServiceConfigurer} instance
     * @throws NotRunningServiceException if the service is not registered or if the configuration
     * directory is missing
     */
    ServiceConfigurer newInstance(final ServiceDef.Specification specification) throws NotRunningServiceException;

    /**
     * Return a {@link Worker} for the specified OGC service instance.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param serviceID The identifier of the new {@link Worker}.
     * @return
     */
    Worker getInstance(final String specification, final String serviceID);

    /**
     * Return the current status for each instance of OGC service instance.
     *
     * @return
     */
    Map<String, Map<String, WorkerState>> getWorkerStatus();

    /**
     * Return the current status for each instance of the specified OGC service specification.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @return
     */
    Map<String, WorkerState> getWorkerStatus(final String specification);

    /**
     * Return the current status for the specified OGC service instance.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param serviceID The identifier of the new {@link Worker}.
     * @return
     */
    WorkerState getWorkerStatus(final String specification, final String serviceID);

    /**
     * Update the current status for the specified OGC service instance.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param serviceID The identifier of the new {@link Worker}.
     * @param workerState The new worker state
     */
    void updateWorkerStatus(final String specification, final String serviceID, final WorkerState workerState);

    /**
     * Remove the specified OGC service instance from the status list.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param serviceID The identifier of the new {@link Worker}.
     */
    void removeWorkerStatus(final String specification, final String serviceID);

    /**
     * Shutdown the specified service instance an remove it from the pool.
     *
     * @param specification A service type (CSW, SOS, WMS, ...).
     * @param serviceID The identifier of the {@link Worker} to shutdown.
     */
    void shutdownInstance(final String specification, final String serviceID);
}