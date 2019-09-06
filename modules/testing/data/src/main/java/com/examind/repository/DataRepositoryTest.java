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
import org.constellation.repository.DataRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.UserRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class DataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Test
    public void findAll() {
        dump(dataRepository.findAll());
    }

    @Test
    @Transactional()
    public void crud() {

        // no removeAll method
        List<Data> all = dataRepository.findAll();
        for (Data p : all) {
            dataRepository.delete(p.getId());
        }
        all = dataRepository.findAll();
        Assert.assertTrue(all.isEmpty());


        CstlUser owner = userRepository.create(TestSamples.newAdminUser());
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer dsid = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "dataset 1"));

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);

        /**
         * Data insertion
         */
        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, dsid));
        Assert.assertNotNull(did1);
        Data data1 = dataRepository.findById(did1);
        Assert.assertNotNull(data1);
        Assert.assertNotNull(data1.getId());

        Integer did2 = dataRepository.create(TestSamples.newData2(owner.getId(), pid, dsid));
        Assert.assertNotNull(did2);
        Data data2 = dataRepository.findById(did2);
        Assert.assertNotNull(data2);
        Assert.assertNotNull(data2.getId());

        Integer did3 = dataRepository.create(TestSamples.newData3(owner.getId(), pid, dsid));
        Assert.assertNotNull(did3);
        Data data3 = dataRepository.findById(did3);
        Assert.assertNotNull(data3);
        Assert.assertNotNull(data3.getId());


        /**
         * Data search
         */
        Assert.assertTrue(dataRepository.existsById(data1.getId()));
        Assert.assertTrue(dataRepository.existsById(data2.getId()));

        Assert.assertEquals(3, dataRepository.findAll().size());

        Assert.assertEquals(new Integer(2), dataRepository.countAll(false));
        Assert.assertEquals(new Integer(3), dataRepository.countAll(true));

        List<Data> datas = dataRepository.findAllByDatasetId(dsid);
        Assert.assertTrue(datas.contains(data1));
        Assert.assertTrue(datas.contains(data2));
        Assert.assertTrue(datas.contains(data3));

        datas = dataRepository.findAllByVisibility(false);
        Assert.assertTrue(datas.contains(data1));
        Assert.assertTrue(datas.contains(data2));

        datas = dataRepository.findAllByVisibility(true);
        Assert.assertTrue(datas.contains(data3));

        datas = dataRepository.findByDatasetId(dsid, true, true);
        Assert.assertTrue(datas.contains(data3));

        datas = dataRepository.findByProviderId(pid);
        Assert.assertTrue(datas.contains(data1));
        Assert.assertTrue(datas.contains(data2));
        Assert.assertTrue(datas.contains(data3));

        datas = dataRepository.findByProviderId(pid, "raster", true, false);
        Assert.assertTrue(datas.contains(data1));

        List<Integer> dids = dataRepository.findIdsByProviderId(pid);
        Assert.assertTrue(dids.contains(did1));
        Assert.assertTrue(dids.contains(did2));
        Assert.assertTrue(dids.contains(did3));

        dids = dataRepository.findIdsByProviderId(pid, "raster", true, false);
        Assert.assertTrue(dids.contains(did1));

        /**
         * Data deletion
         */
        int res = dataRepository.delete(data1.getId());
        Assert.assertEquals(1, res);

        res = dataRepository.delete(data2.getId());
        Assert.assertEquals(1, res);

        res = dataRepository.delete(data3.getId());
        Assert.assertEquals(1, res);

        res = dataRepository.delete(-1);
        Assert.assertEquals(0, res);

        data1 = dataRepository.findById(data1.getId());
        Assert.assertNull(data1);

        // cleanup after test
        providerRepository.delete(pid);
        datasetRepository.delete(dsid);
    }

}
