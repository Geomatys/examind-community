/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2016 Geomatys.
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.constellation.dto.cluster.Cluster;
import java.util.function.Consumer;

/**
 *
 * @author Guilhem Legal (Geomatys)
 * @author Johann Sorel (Geomatys)
 */
public interface IClusterBusiness {
 
    /**
     * Get this application instance unique identifier.
     * 
     * @return current instance uid
     */
    String getMemberUID();

    Lock acquireLock(String lockName);
        

    /**
     * Broadcast message on constellation event topic.
     *
     * @param message not null
     */
    void publish(ClusterMessage message);

    /**
     * Broadcast message on constellation event topic and return immediately.<br>
     * <br>
     * This method return a message response object, a reference to this object
     * must be kept as long as we want to listen for possible response messages.
     *
     * @param message not null
     * @param callback
     * @return MessageResponse
     */
    MessageResponse publish(ClusterMessage message, Consumer<ClusterMessage> callback);

    /**
     *  Broadcast message on constellation event topic and wait for all responses or given timeout.<br>
     * <br>
     * This method return a message response object, a reference to this object
     * must be kept as long as we want to listen for possible response messages.
     *
     * @param message
     * @param time
     * @param timeUnit
     * @param callback
     */
    void publishAndWait(ClusterMessage message, long time, TimeUnit timeUnit, Consumer<ClusterMessage> callback);

    Cluster clusterStatus();
    
    int getMemberSize();

    /**
     * Remove a message listener.
     *
     * @param uuid listener identifier
     * @return true if listener has been found and removed
     */
    boolean removeMessageListener(String uuid);

    /**
     * Register a new message listener.
     *
     * @param listener, not null
     * @return unique identifier given to the listener
     */
    String addMessageListener(MessageListener listener);

    /**
     * Create a new request message.
     *
     * @param typeId not null
     * @param expectResponse set to true if this request expect a response.
     * @return MessageMap never null
     */
    ClusterMessage createRequest(String typeId, boolean expectResponse);

}
