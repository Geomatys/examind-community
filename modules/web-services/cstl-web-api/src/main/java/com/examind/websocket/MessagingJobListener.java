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
package com.examind.websocket;

import org.constellation.dto.process.TaskStatus;
import org.constellation.dto.service.config.sos.OM2ResultEventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.constellation.business.ClusterMessage;
import static org.constellation.business.ClusterMessageConstant.*;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.MessageException;
import org.constellation.business.MessageListener;
import org.constellation.exception.ConfigurationException;
import org.constellation.ws.CstlServiceException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Quartz job listener that register a geotk process listener each time the job is executed.
 * And send messages on websocket "/topic/taskevents*" topic.
 *
 * @author Quentin Boileau (Geomatys)
 */
@Component
public class MessagingJobListener  extends MessageListener {

    private String uid;
    
    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private IClusterBusiness clusterBusiness;

    @PostConstruct
    private void init() {
         //attach listener to event topic
        uid = clusterBusiness.addMessageListener(this);
    }

    @Override
    protected IClusterBusiness getClusterBusiness() {
        return clusterBusiness;
    }

    @Override
    protected boolean filter(ClusterMessage message) {
        return PRC_TASK.equals(message.getTypeId())
             ||SRV_SOS_EVENT.equals(message.getTypeId());
    }

    @Override
    protected ClusterMessage process(ClusterMessage message) throws Exception, MessageException, CstlServiceException, ConfigurationException {
        if (PRC_TASK.equals(message.getTypeId())) {
            TaskStatus status = message.getComplex(PRC_TASK_STATUS, false, TaskStatus.class);
            template.convertAndSend("/topic/taskevents", status);
            template.convertAndSend("/topic/taskevents/" + status.getTaskId(), status);
        }
        
        if (SRV_SOS_EVENT.equals(message.getTypeId())) {
            OM2ResultEventDTO resultEvent = message.getComplex(SRV_SOS_EVENT_BODY, false, OM2ResultEventDTO.class);
            template.convertAndSend("/topic/sosevents/" + resultEvent.getProcedureID(), resultEvent);
        }
        return null;
    }
    
    @PreDestroy
    public void destroy(){
        clusterBusiness.removeMessageListener(uid);
    }
}
