/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2015 Geomatys.
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

import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.constellation.api.ServiceDef;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.business.MessageException;
import org.constellation.business.MessageListener;
import org.constellation.exception.ConstellationException;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.IWSEngine;
import org.constellation.ws.Refreshable;
import org.constellation.ws.Worker;
import org.springframework.stereotype.Component;
import static org.constellation.business.ClusterMessageConstant.*;

/**
 * Listen to constellation messages related to service operations.
 *
 * @author Johann Sorel (Geomatys)
 */
@Component
public class ServiceMessageConsumer extends MessageListener{

    private String uid;

    @Inject
    private IWSEngine wsengine;

    @Inject
    private IClusterBusiness clusterBusiness;

    @PostConstruct
    public void init(){
        //attach listener to event topic
        uid = clusterBusiness.addMessageListener(this);
    }

    @PreDestroy
    public void destroy(){
        clusterBusiness.removeMessageListener(uid);
    }

    @Override
    protected IClusterBusiness getClusterBusiness() {
        return clusterBusiness;
    }

    @Override
    protected boolean filter(ClusterMessage message) {
        return SRV_MESSAGE_TYPE_ID.equals(message.getTypeId())
               && message.isRequest();
    }

    @Override
    public ClusterMessage process(ClusterMessage message) throws MessageException, ConfigurationException, CstlServiceException {
        final String action = message.getString(KEY_ACTION,false);

        switch(action){
            case SRV_VALUE_ACTION_START : return start(message);
            case SRV_VALUE_ACTION_STOP : return stop(message);
            case SRV_VALUE_ACTION_REFRESH : return refresh(message);
            case SRV_VALUE_ACTION_STATUS : return status(message);
            case SRV_VALUE_ACTION_CLEAR_CACHE : return clearCache(message);
            default: throw new MessageException("Unknown request action : "+action);
        }
    }

    private ClusterMessage start(ClusterMessage message) throws ConfigurationException, MessageException {
        final String serviceType = message.getString(SRV_KEY_TYPE,false);
        final String serviceId = message.getString(KEY_IDENTIFIER,false);

        if(!wsengine.serviceInstanceExist(serviceType, serviceId)){
            //create service if it doesn't exist
            try {
                final Worker worker = wsengine.buildWorker(serviceType, serviceId);
                if (worker != null) {
                    wsengine.addServiceInstance(serviceType, serviceId, worker);
                    if (!worker.isStarted()) {
                        throw new ConfigurationException("service "+serviceId+" start failed.");
                    }
                } else {
                    throw new ConfigurationException("The instance " + serviceId + " can not be instanciated.");
                }
            } catch (IllegalArgumentException | ConstellationException ex) {
                throw new ConfigurationException(ex.getMessage(), ex);
            }
        }

        return null;
    }

    private ClusterMessage stop(ClusterMessage message) throws ConfigurationException, MessageException {
        final String serviceType = message.getString(SRV_KEY_TYPE,false);
        final String serviceId = message.getString(KEY_IDENTIFIER,false);

        if (serviceId == null || serviceId.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }
        if (wsengine.serviceInstanceExist(serviceType, serviceId)) {
            wsengine.shutdownInstance(serviceType, serviceId);
        }

        return null;
    }

    private ClusterMessage refresh(ClusterMessage message) throws ConfigurationException, CstlServiceException, MessageException {
        final String serviceType = message.getString(SRV_KEY_TYPE,false);
        final String serviceId = message.getString(KEY_IDENTIFIER,false);

        if (serviceId == null || serviceId.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }
        final Worker worker = wsengine.getInstance(serviceType, serviceId);
        if (worker instanceof Refreshable) {
            ((Refreshable)worker).refresh();
        }

        return null;
    }

    /**
     *
     * Respone structure.
     * - wms
     *      - instance1 : true
     *      - instance2 : false
     *      - ...
     * - csw
     *      - instance1 : true
     *      - instance2 : true
     *      - ...
     * - ...
     *
     * @param message
     * @return
     * @throws ConfigurationException
     * @throws CstlServiceException
     */
    private ClusterMessage status(ClusterMessage message) throws ConfigurationException, CstlServiceException {
        final ClusterMessage response = message.createResponse(clusterBusiness);

        for(ServiceDef.Specification spec : ServiceDef.Specification.values()){
            final ClusterMessage specRes = response.createPart();
            for(Map.Entry<String, Boolean> entry : wsengine.getEntriesStatus(spec.name())){
                specRes.put(entry.getKey(), entry.getValue());
            }
            response.put(spec.name(), specRes);
        }

        return response;
    }

    private ClusterMessage clearCache(ClusterMessage message) throws ConfigurationException, CstlServiceException, MessageException {
        final String serviceType = message.getString(SRV_KEY_TYPE,false);
        final String serviceId = message.getString(KEY_IDENTIFIER,false);

        if (serviceId == null || serviceId.isEmpty()) {
            throw new ConfigurationException("Service instance identifier can't be null or empty.");
        }
        final Worker worker = wsengine.getInstance(serviceType, serviceId);
        if (worker != null) {
            worker.refreshUpdateSequence();
            worker.clearCapabilitiesCache();
        }
        return null;
    }

}
