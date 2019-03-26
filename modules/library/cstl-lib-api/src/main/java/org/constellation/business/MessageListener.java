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
package org.constellation.business;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConfigurationException;
import org.constellation.ws.CstlServiceException;


/**
 * Constellation message listener.
 *
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class MessageListener {

    protected static final Logger LOGGER = Logging.getLogger("org.constellation.admin");


    public MessageListener(){
    }

    protected abstract IClusterBusiness getClusterBusiness();

    public void receive(ClusterMessage message) {
        if(filter(message)){
            LOGGER.log(Level.FINE, "RECEIVED {0}", message.toString());
            try {
                final ClusterMessage response = process(message);

                if(message.expectResponse()){
                    if(response==null){
                        throw new IllegalStateException("Message expected a response but listener did not produce any response.");
                    }
                    //send response
                    getClusterBusiness().publish(response);
                }

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                if (message.expectResponse()) {
                    //send back an exception response
                    getClusterBusiness().publish(message.createExceptionResponse(getClusterBusiness(),ex));
                }
            }
        }
    }

    /**
     * The constellation topic will transport a wide range of messages, not all
     * of them may be of interest for this listener.
     * This method should filter message that are interesting for this listener.
     *
     * @param event
     * @return true if message must be processing by this listener.
     */
    protected abstract boolean filter(ClusterMessage event);

    /**
     *
     * @param event
     * @return possible response
     * @throws Exception
     * @throws MessageException
     * @throws CstlServiceException
     * @throws ConfigurationException
     */
    protected abstract ClusterMessage process(ClusterMessage event) throws Exception, MessageException, CstlServiceException, ConfigurationException;

}
