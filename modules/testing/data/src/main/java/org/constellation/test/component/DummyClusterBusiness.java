/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2017 Geomatys.
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
package org.constellation.test.component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.constellation.dto.cluster.Cluster;
import java.util.function.Consumer;
import org.constellation.business.IClusterBusiness;
import org.constellation.business.ClusterMessage;
import org.constellation.business.MessageListener;
import org.constellation.business.MessageResponse;
import org.springframework.stereotype.Component;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Component
public class DummyClusterBusiness implements IClusterBusiness {

    @Override
    public String getMemberUID() {
        return "this";
    }

    @Override
    public Lock acquireLock(String lockName) {
        return new ReentrantLock();
    }

    @Override
    public void publish(ClusterMessage message) {
        //do nothing
    }

    @Override
    public Cluster clusterStatus() {
        return new Cluster();
    }

    @Override
    public int getMemberSize() {
        return 1;
    }

    @Override
    public boolean removeMessageListener(String uuid) {
        // do nothing
        return true;
    }

    @Override
    public String addMessageListener(MessageListener listener) {
        return null;
    }

    @Override
    public ClusterMessage createRequest(String typeId, boolean expectResponse) {
        return null;
    }

    @Override
    public MessageResponse publish(ClusterMessage message, Consumer<ClusterMessage> callback) {
        return null;
    }

    @Override
    public void publishAndWait(ClusterMessage message, long time, TimeUnit timeUnit, Consumer<ClusterMessage> callback) {
        // do nothing
    }

}
