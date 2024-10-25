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
package org.constellation.api.rest.websocket;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.constellation.dto.process.TaskStatus;
import org.constellation.dto.service.config.sos.OM2ResultEventDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Quartz job listener that register a geotk process listener each time the job is executed.
 * And send messages on websocket "/topic/taskevents*" topic.
 *
 * @author Quentin Boileau (Geomatys)
 */
@Component
public class MessagingJobListener  {

    @Autowired
    private SimpMessagingTemplate template;

    @Autowired
    private EventBus eventBus;

    @PostConstruct
    private void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onBusEvent(TaskStatus taskStatus) {
        template.convertAndSend("/topic/taskevents", taskStatus);
        template.convertAndSend("/topic/taskevents/"+taskStatus.getTaskId(), taskStatus);
    }


    @Subscribe
    public void onBusEvent(OM2ResultEventDTO sosResult) {
        template.convertAndSend("/topic/sosevents/"+sosResult.getProcedureID(), sosResult);
    }
}
