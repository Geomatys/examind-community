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
package org.constellation.admin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.collection.WeakValueHashMap;
import org.constellation.business.ClusterMessage;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.MessageListener;
import org.constellation.business.MessageResponse;
import org.constellation.dto.cluster.Cluster;
import org.constellation.dto.cluster.ClusterMember;
import org.constellation.dto.service.ServiceComplete;
import static org.constellation.business.ClusterMessageConstant.*;

/**
 * Single instanceof messaging business.
 *
 * @author Johann Sorel (Geomatys)
 */
public class LocalClusterBusiness implements IClusterBusiness{

    private static final Logger LOGGER = Logger.getLogger("org.constellation.admin");

    private final String memberUID = UUID.randomUUID().toString();
    private final AtomicLong messageInc = new AtomicLong();
    private final AtomicLong listenerInc = new AtomicLong();

    private final Map<String,MessageListener> listeners = new HashMap<>();
    private final WeakValueHashMap<String,Lock> locks = new WeakValueHashMap<>(String.class);

    public LocalClusterBusiness(){}

    @Override
    public String getMemberUID() {
        return memberUID;
    }

    @Override
    public Lock acquireLock(String lockName) {
        synchronized (locks) {
            Lock lock = locks.get(lockName);
            if (lock==null) {
                lock = new ReentrantLock();
                locks.put(lockName, lock);
            }
            return lock;
        }
    }

    @Override
    public void publish(ClusterMessage message) {

        final List<MessageListener> lst;
        synchronized (listeners) {
            lst = new ArrayList<>(listeners.values());
        }
        for (MessageListener listener : lst) {
            try {
                listener.receive(message);
            } catch(Exception ex) {
                //we catch anything, the message must be send to all listeners
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
    }

    @Override
    public MessageResponse publish(ClusterMessage message, Consumer<ClusterMessage> callback) {
        ArgumentChecks.ensureNonNull("message", message);
        ArgumentChecks.ensureNonNull("callback", callback);
        return new MessageResponse(this, message, callback);
    }

    @Override
    public void publishAndWait(ClusterMessage message, long time, TimeUnit timeUnit, Consumer<ClusterMessage> callback) {
        final MessageResponse response = publish(message, callback);
        try {
            response.await(time, timeUnit);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }
        response.release();
    }

    @Override
    public Cluster clusterStatus() {

        final ClusterMessage request = createRequest(SRV_MESSAGE_TYPE_ID,true);
        request.put(KEY_ACTION, SRV_VALUE_ACTION_STATUS);

        final Cluster cluster = new Cluster();

        publishAndWait(request, 10, TimeUnit.SECONDS, new Consumer<ClusterMessage>() {
            @Override
            public void accept(ClusterMessage message) {
                final String who = message.getMemberUID();
                final ClusterMember member = new ClusterMember();
                cluster.getMembers().add(member);
                member.setSocketAddress(who);
                for(Map.Entry<String,Object> entry : message.entrySet()){
                    final String spec = entry.getKey();
                    final Map<String,Boolean> instances = (Map<String,Boolean>) entry.getValue();
                    for(Map.Entry<String,Boolean> states : instances.entrySet()){
                        final ServiceComplete s = new ServiceComplete();
                        member.getServices().add(s);
                        s.setType(spec);
                        s.setTitle(states.getKey());
                        s.setStatus(states.getValue() ? "STARTED":"STOPPED");
                    }
                }
            }
        });

        return cluster;
    }

    @Override
    public int getMemberSize() {
        return 1;
    }

    @Override
    public boolean removeMessageListener(String uuid) {
        synchronized (listeners) {
            return listeners.remove(uuid) != null;
        }
    }

    @Override
    public String addMessageListener(MessageListener listener) {
        ArgumentChecks.ensureNonNull("listener", listener);
        synchronized (listeners) {
            final String uuid = Long.toString(listenerInc.incrementAndGet());
            listeners.put(uuid, listener);
            return uuid;
        }
    }

    @Override
    public ClusterMessage createRequest(String typeId, boolean expectResponse) {
        return new LocalClusterMessage(typeId, expectResponse);
    }
    
    private class LocalClusterMessage extends ClusterMessage {
        protected LocalClusterMessage(String typeId, boolean expectResponse){
            super(LocalClusterBusiness.this.getMemberUID(), typeId, (expectResponse?ClusterMessage.Type.REQUEST_WITH_RESPONSE:ClusterMessage.Type.REQUEST_NO_RESPONSE));
            messageUID = LocalClusterBusiness.this.messageInc.incrementAndGet();
        }
    }

}
