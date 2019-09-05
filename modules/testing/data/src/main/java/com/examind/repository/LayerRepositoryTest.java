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
package com.examind.repository;

import java.util.List;
import org.constellation.repository.LayerRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.dto.Layer;
import org.constellation.repository.DataRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class LayerRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Test
    public void all() {
        dump(layerRepository.findAll());
    }

    @Test
    @Transactional()
    public void crud() {

        // no removeAll method
        List<Layer> all = layerRepository.findAll();
        for (Layer p : all) {
            layerRepository.delete(p.getId());
        }
        all = layerRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        Data data = dataRepository.create(TestSamples.newData(owner.getId(), pid));
        Assert.assertNotNull(data);
        Assert.assertNotNull(data.getId());

        Integer sid = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid);

        Integer lid = layerRepository.create(TestSamples.newLayer(owner.getId(), pid, data.getId(), sid));
        Assert.assertNotNull(lid);

        layerRepository.delete(lid);

        Layer layer = layerRepository.findById(lid);
        Assert.assertNull(layer);
    }


}
