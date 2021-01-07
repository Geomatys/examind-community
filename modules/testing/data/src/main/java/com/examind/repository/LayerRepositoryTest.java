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
        dataRepository.deleteAll();
        providerRepository.deleteAll();
        datasetRepository.deleteAll();
        layerRepository.deleteAll();
        serviceRepository.deleteAll();

        List<Layer> all = layerRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer dsid = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "dataset 1"));

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, dsid));
        Assert.assertNotNull(did1);

        Integer did2 = dataRepository.create(TestSamples.newData2(owner.getId(), pid, dsid));
        Assert.assertNotNull(did2);

        Integer sid1 = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid1);

        Integer sid2 = serviceRepository.create(TestSamples.newService2(owner.getId()));
        Assert.assertNotNull(sid2);

        /**
         * Layer insertion
         */

        Integer lid1 = layerRepository.create(TestSamples.newLayer(owner.getId(), did1, sid1));
        Assert.assertNotNull(lid1);

        Layer l1 = layerRepository.findById(lid1);
        Assert.assertNotNull(l1);

        Integer lid2 = layerRepository.create(TestSamples.newLayer2(owner.getId(), did2, sid2));
        Assert.assertNotNull(lid2);

        Layer l2 = layerRepository.findById(lid2);
        Assert.assertNotNull(l2);

        Integer lid3 = layerRepository.create(TestSamples.newLayer3(owner.getId(), did1, sid2));
        Assert.assertNotNull(lid3);

        Layer l3 = layerRepository.findById(lid3);
        Assert.assertNotNull(l3);

        Integer lid4 = layerRepository.create(TestSamples.newLayer4(owner.getId(), did2, sid1));
        Assert.assertNotNull(lid4);

        Layer l4 = layerRepository.findById(lid4);
        Assert.assertNotNull(l4);

        /**
         * layer search
         */
        List<Layer> layers = layerRepository.findAll();
        Assert.assertTrue(layers.contains(l1));

        List<Integer> lids = layerRepository.findByDataId(did1);
        Assert.assertEquals(2, lids.size());
        Assert.assertTrue(lids.contains(lid1));
        Assert.assertTrue(lids.contains(lid3));

        lids = layerRepository.findByDataId(did2);
        Assert.assertEquals(2, lids.size());
        Assert.assertTrue(lids.contains(lid2));
        Assert.assertTrue(lids.contains(lid4));

        layers = layerRepository.findByServiceId(sid1);
        Assert.assertEquals(2, layers.size());
        Assert.assertTrue(layers.contains(l1));
        Assert.assertTrue(layers.contains(l4));

        layers = layerRepository.findByServiceId(sid2);
        Assert.assertEquals(2, layers.size());
        Assert.assertTrue(layers.contains(l2));
        Assert.assertTrue(layers.contains(l3));

        Layer l = layerRepository.findByServiceIdAndAlias(sid2, "layer'; delete from admin.layer;'Alias");
        Assert.assertNotNull(l);
        Assert.assertEquals(l2, l);

        l = layerRepository.findByServiceIdAndLayerName(sid2, "testlayer3");
        Assert.assertNotNull(l);
        Assert.assertEquals(l3, l);

        l = layerRepository.findByServiceIdAndLayerName(sid1, "test'l'ayer4;", "test' nmsp");
        Assert.assertNotNull(l);
        Assert.assertEquals(l4, l);

        layerRepository.updateLayerTitle(lid4, "some'; delete * from admin.layers;");
        l = layerRepository.findById(lid4);
        Assert.assertNotNull(l);

        Assert.assertEquals("some'; delete * from admin.layers;", l.getTitle());

        l.setAlias("bloup'; '");
        layerRepository.update(l);

        l = layerRepository.findById(lid4);
        Assert.assertNotNull(l);

        Assert.assertEquals("bloup'; '", l.getAlias());

        /**
         * layer deletion
         */
        layerRepository.deleteServiceLayer(sid2);
        layers = layerRepository.findByServiceId(sid2);
        Assert.assertEquals(0, layers.size());

        layerRepository.delete(lid1);

        Layer layer = layerRepository.findById(lid1);
        Assert.assertNull(layer);


        // cleanup after test
        dataRepository.deleteAll();
        providerRepository.deleteAll();
        datasetRepository.deleteAll();
        layerRepository.deleteAll();
        serviceRepository.deleteAll();

    }


}
