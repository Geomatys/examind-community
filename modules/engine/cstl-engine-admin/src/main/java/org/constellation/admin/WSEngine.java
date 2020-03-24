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

import org.apache.sis.util.logging.Logging;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.constellation.api.ServiceDef;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.NotRunningServiceException;
import org.constellation.ws.IWSEngine;
import org.constellation.ws.ServiceConfigurer;
import org.constellation.ws.Worker;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class WSEngine implements IWSEngine {

    private static final Logger LOGGER = Logging.getLogger("org.constellation.ws");

    @Inject
    private ApplicationContext applicationContext;

    /**
     * A map of service worker.
     */
    private final Map<String, Map<String, Worker>> WORKERS_MAP = new HashMap<>();

    /**
     * A map of the registred OGC services and their endpoint protocols (REST).
     */
    private final Map<String, List<String>> REGISTERED_SERVICE = new HashMap<>();


    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Map<String, Worker> getWorkersMap(final String specification) {
        final Map<String, Worker> result = WORKERS_MAP.get(specification.toLowerCase());
        if (result == null) {
            return new HashMap<>();
        }
        return new HashMap<>(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getInstanceSize(final String specification) {
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap != null) {
            return workersMap.size();
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean serviceInstanceExist(final String specification, final String serviceID) {
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap != null) {
            return workersMap.containsKey(serviceID);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<String> getInstanceNames(final String specification) {
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap != null) {
            return workersMap.keySet();
        }
        return Collections.EMPTY_SET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Worker getInstance(final String specification, final String serviceID) {
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap != null) {
            return workersMap.get(serviceID);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void destroyInstances(final String specification) {
        final Map<String, Worker> workersMap = WORKERS_MAP.remove(specification.toLowerCase());
        if (workersMap != null) {
            for (final Worker worker : workersMap.values()) {
                worker.destroy();
            }
            workersMap.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean isSetService(final String specification) {
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        return workersMap != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void addServiceInstance(final String specification, final String serviceID, final Worker instance) {
        Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap == null) {
            workersMap = new HashMap<>();
            WORKERS_MAP.put(specification.toLowerCase(), workersMap);
        }
        final Worker oldWorker = workersMap.put(serviceID, instance);
        if (oldWorker != null) {
            LOGGER.log(Level.INFO, "Destroying old worker: {0}({1})", new Object[]{specification.toLowerCase(), serviceID});
            oldWorker.destroy();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<Entry<String, Boolean>> getEntriesStatus(final String specification) {
        final Set<Map.Entry<String, Boolean>> response = new HashSet<>();
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap != null) {
            for (Entry<String, Worker> entry : workersMap.entrySet()) {
                response.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().isStarted()));
            }
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void shutdownInstance(final String specification, final String serviceID) {
        final Map<String, Worker> workersMap = WORKERS_MAP.get(specification.toLowerCase());
        if (workersMap != null) {
            final Worker worker = workersMap.get(serviceID);
            if (worker != null) {
                worker.destroy();
                workersMap.remove(serviceID);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerService(final String specification, final String protocol) {
        if (REGISTERED_SERVICE.containsKey(specification.toLowerCase())) {
            final List<String> protocols = REGISTERED_SERVICE.get(specification.toLowerCase());
            if (!protocols.contains(protocol)) {
                protocols.add(protocol);
            }
            REGISTERED_SERVICE.put(specification.toLowerCase(), protocols);
        } else {
            final List<String> protocols = new ArrayList<>();
            protocols.add(protocol);
            REGISTERED_SERVICE.put(specification.toLowerCase(), protocols);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getRegisteredServices() {
        return new HashMap<>(REGISTERED_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Worker buildWorker(final String specification, final String identifier) throws ConstellationException {
        try {
            final String workerBeanName = specification.toUpperCase()+"Worker";
            return (Worker) applicationContext.getBean(workerBeanName, identifier);
        } catch (BeansException ex) {
            throw new ConstellationException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceConfigurer newInstance(final ServiceDef.Specification spec) throws NotRunningServiceException {
        final String serviceType = spec.name().toLowerCase();
        if (getRegisteredServices().get(serviceType) == null) {
            throw new NotRunningServiceException(spec);
        }
        final String configurerBeanName = serviceType.toUpperCase()+"Configurer";
        final ServiceConfigurer sc = (ServiceConfigurer) applicationContext.getBean(configurerBeanName);
        applicationContext.getAutowireCapableBeanFactory().autowireBean(sc);
        return sc;
    }

}
