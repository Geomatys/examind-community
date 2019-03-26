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

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.constellation.exception.ConfigurationException;
import org.constellation.ws.CstlServiceException;

/**
 * Listen to response messages for a given message uid.<br>
 * <br>
 * This listener will stop listening to responses when it is not referenced anymore.
 * <br>
 *
 *
 * @author Johann Sorel (Geomatys)
 */
public class MessageResponse {

    private final ClusterMessage message;
    private final Consumer<ClusterMessage> callback;
    private final long uid;
    private final String rgid;
    private final IClusterBusiness clusterBusiness;
    private final CountDownLatch countdown;

    /**
     * Create a new message m
     *
     * @param business
     * @param message
     * @param callback
     */
    public MessageResponse(IClusterBusiness business, ClusterMessage message, Consumer<ClusterMessage> callback) {
        if(!message.expectResponse()){
            throw new IllegalArgumentException("Message reponse are possible only with request message expecting a response.");
        }
        this.message = message;
        this.callback = callback;
        this.clusterBusiness = business;
        this.uid = message.getMessageUID();
        this.rgid = clusterBusiness.addMessageListener(new Weak(this));

        final int nbMember = clusterBusiness.getMemberSize();
        countdown = new CountDownLatch(nbMember);

        //broadcast message
        clusterBusiness.publish(message);
    }


    /**
     * Block current thread waiting for all member answers or given timeout.
     *
     * @param timeout amount of time to wait
     * @param timeUnit time unit
     */
    public void await(long timeout, TimeUnit timeUnit) throws InterruptedException{
       countdown.await(timeout, timeUnit);
    }

    /**
     * Unregister response listener.
     */
    public void release(){
        clusterBusiness.removeMessageListener(rgid);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    private void receive(ClusterMessage message) {
        if (!message.isRequest() && uid == message.getMessageUID()) {
            countdown.countDown();
            callback.accept(message);
        }
    }

    /**
     * Weak message listener, to avoid the topic to keep a hard reference on the listener.
     */
    private static class Weak extends MessageListener {

        private final WeakReference<MessageResponse> ref;

        public Weak(MessageResponse lst) {
            super();
            this.ref = new WeakReference<>(lst);
        }

        /**
         * We don't consume message, just a countdown.
         * @param event
         * @return 
         */
        @Override
        protected boolean filter(ClusterMessage event) {
            final MessageResponse lst = ref.get();
            if(lst!=null) lst.receive(event);
            return false;
        }

        
        @Override
        protected ClusterMessage process(ClusterMessage event) throws Exception, MessageException, CstlServiceException, ConfigurationException {
            throw new CstlServiceException("Should not be called");
        }

        @Override
        protected IClusterBusiness getClusterBusiness() {
            return ref.get().clusterBusiness;
        }
    }

}
