/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package com.examind.repository;

import java.util.List;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Sensor;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.SensorRepository;
import org.constellation.repository.ServiceRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class SensorRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private ProviderRepository providerRepository;
    
    public void crude() {

        dataRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();
        sensorRepository.deleteAll();
        
        List<Sensor> all = sensorRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        /**
         * sensor insertion
         */
        Integer pid = providerRepository.create(TestSamples.newProviderQuote(owner.getId()));
        Assert.assertNotNull(pid);

        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, null));
        Assert.assertNotNull(did1);

        Integer sid1 = sensorRepository.create(TestSamples.newSensor(owner.getId(), "sensor1"));
        Assert.assertNotNull(sid1);

        Sensor s1 = sensorRepository.findById(sid1);
        Assert.assertNotNull(s1);

        Integer sid2 = sensorRepository.create(TestSamples.newSensor(owner.getId(), "senso'; r2"));
        Assert.assertNotNull(sid2);

        Sensor s2 = sensorRepository.findById(sid2);
        Assert.assertNotNull(s2);

        Integer srid1 = serviceRepository.create(TestSamples.newService(owner.getId()));

        sensorRepository.linkSensorToService(sid2, srid1);
        sensorRepository.linkDataToSensor(did1, sid1);

        /**
         * sensor search
         */
        Assert.assertEquals(s1, sensorRepository.findByIdentifier("sensor1"));
        Assert.assertEquals(s1.getId(), sensorRepository.findIdByIdentifier("sensor1"));

        Assert.assertEquals(s2, sensorRepository.findByIdentifier("senso'; r2"));
        Assert.assertEquals(s2.getId(), sensorRepository.findIdByIdentifier("senso'; r2"));

        Assert.assertTrue(sensorRepository.existsById(sid2));
        Assert.assertTrue(sensorRepository.existsByIdentifier("senso'; r2"));

        List<Integer> dataIds = sensorRepository.getLinkedDatas(sid1);
        Assert.assertTrue(dataIds.contains(did1));

        Assert.assertTrue(sensorRepository.isLinkedSensorToService(sid2, srid1));
        List<Integer> servIds = sensorRepository.getLinkedServices(sid2);
        Assert.assertTrue(servIds.contains(srid1));
        Assert.assertEquals(1, sensorRepository.getLinkedSensorCount(srid1));

        sensorRepository.unlinkSensorFromService(sid2, srid1);
        sensorRepository.unlinkDataToSensor(did1, sid1);

        dataIds = sensorRepository.getLinkedDatas(sid1);
        Assert.assertTrue(dataIds.isEmpty());

        servIds = sensorRepository.getLinkedServices(sid2);
        Assert.assertTrue(servIds.isEmpty());

        /**
         * sensor delete
         */
        sensorRepository.delete(s1.getIdentifier());

        s1 = sensorRepository.findById(s1.getId());
        Assert.assertNull(s1);
        
        dataRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();
        sensorRepository.deleteAll();
    }

}