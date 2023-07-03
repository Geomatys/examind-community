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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.exception.ConfigurationException;
import org.constellation.business.MessageException;
import org.constellation.business.MessageListener;
import org.constellation.provider.DataProviders;
import org.constellation.ws.CstlServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static org.constellation.business.ClusterMessageConstant.*;

/**
 * Listen to constellation messages related to provider operations.
 *
 * @author Johann Sorel (Geomatys)
 */
@Component
public class ProviderMessageConsumer extends MessageListener {

    private String uid;

    @Autowired
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
        return PRV_MESSAGE_TYPE_ID.equals(message.getTypeId())
               && message.isRequest();
    }

    @Override
    public ClusterMessage process(ClusterMessage message) throws MessageException, ConfigurationException, CstlServiceException {
        final String action = message.getString(KEY_ACTION,false);
        final int providerId = message.getInteger(KEY_IDENTIFIER,false);

        switch(action){
            case PRV_VALUE_ACTION_RELOAD :
            case PRV_VALUE_ACTION_DELETE : clearCache(providerId); break;
            case PRV_VALUE_ACTION_UPDATED : /*just an info, nothing to do*/ break;
            default: throw new MessageException("Unknown request action : "+action);
        }

        return null;
    }

    private void clearCache(int providerId) {
        DataProviders.dispose(providerId);

        //send event, used for services indexes,caches,capabilities,...
        final ClusterMessage message = clusterBusiness.createRequest(PRV_MESSAGE_TYPE_ID,false);
        message.put(KEY_ACTION, PRV_VALUE_ACTION_UPDATED);
        message.put(KEY_IDENTIFIER, providerId);
        clusterBusiness.publish(message);
    }

}
