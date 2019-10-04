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
import org.constellation.dto.Layer;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class LayerRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Test
    public void all() {
        dump(layerRepository.findAll());
    }

    @Test
    public void crud() {

        // no removeAll method
        List<Layer> all = layerRepository.findAll();
        for (Layer p : all) {
            layerRepository.delete(p.getId());
        }
        all = layerRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer dsid = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "dataset 1"));

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, dsid));
        Assert.assertNotNull(did1);

        Integer sid = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid);

        /**
         * Layer insertion
         */

        Integer lid = layerRepository.create(TestSamples.newLayer(owner.getId(), did1, sid));
        Assert.assertNotNull(lid);

        Layer l1 = layerRepository.findById(lid);
        Assert.assertNotNull(l1);

        /**
         * layer search
         */
        List<Layer> layers = layerRepository.findAll();
        Assert.assertTrue(layers.contains(l1));

        List<Integer> lids = layerRepository.findByDataId(did1);
        Assert.assertTrue(lids.contains(lid));

        layers = layerRepository.findByServiceId(sid);
        Assert.assertTrue(layers.contains(l1));


        /**
         * layer deletion
         */
        layerRepository.delete(lid);

        Layer layer = layerRepository.findById(lid);
        Assert.assertNull(layer);


        // cleanup after test
        serviceRepository.delete(sid);
        dataRepository.delete(did1);
        providerRepository.delete(pid);
        datasetRepository.delete(dsid);

    }


}
