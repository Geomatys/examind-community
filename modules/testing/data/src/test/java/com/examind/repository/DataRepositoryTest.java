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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.constellation.repository.DataRepository;
import org.constellation.dto.CstlUser;
import org.constellation.dto.Data;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.ProviderRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

public class DataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private ProviderRepository providerRepository;

    public void findAll() {
        dump(dataRepository.findAll());
    }

    public void crud() {

        dataRepository.deleteAll();
        List<Data> all = dataRepository.findAll();
        Assert.assertTrue(all.isEmpty());


        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        Integer dsid = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "dataset 1"));
        Integer dsid2 = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "dataset 2"));

        Integer pid = providerRepository.create(TestSamples.newProvider(owner.getId()));
        Assert.assertNotNull(pid);
        Integer pid2 = providerRepository.create(TestSamples.newProvider2(owner.getId()));
        Assert.assertNotNull(pid2);

        /**
         * Data insertion
         */
        Integer did1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, dsid));
        Assert.assertNotNull(did1);
        Data data1 = dataRepository.findById(did1);
        Assert.assertNotNull(data1);
        Assert.assertNotNull(data1.getId());

        Integer did2 = dataRepository.create(TestSamples.newData2(owner.getId(), pid2, dsid));
        Assert.assertNotNull(did2);
        Data data2 = dataRepository.findById(did2);
        Assert.assertNotNull(data2);
        Assert.assertNotNull(data2.getId());

        Integer did3 = dataRepository.create(TestSamples.newData3(owner.getId(), pid, dsid));
        Assert.assertNotNull(did3);
        Data data3 = dataRepository.findById(did3);
        Assert.assertNotNull(data3);
        Assert.assertNotNull(data3.getId());

        Integer did4 = dataRepository.create(TestSamples.newDataQuote(owner.getId(), pid, dsid2));
        Assert.assertNotNull(did4);
        Data data4 = dataRepository.findById(did4);
        Assert.assertNotNull(data4);
        Assert.assertNotNull(data4.getId());

        Integer did5 = dataRepository.create(TestSamples.newData5(owner.getId(), pid2, dsid2));
        Assert.assertNotNull(did5);
        Data data5 = dataRepository.findById(did5);
        Assert.assertNotNull(data5);
        Assert.assertNotNull(data5.getId());

        //  set data 5 child of 4 and 3
        dataRepository.linkDataToData(did3, did5);
        dataRepository.linkDataToData(did4, did5);

        List<Integer> parents = dataRepository.getParents(did5);
        Assert.assertTrue(parents.contains(did3));
        Assert.assertTrue(parents.contains(did4));
        
        /**
         * Data search
         */
        Assert.assertTrue(dataRepository.existsById(data1.getId()));
        Assert.assertTrue(dataRepository.existsById(data2.getId()));
        Assert.assertTrue(dataRepository.existsById(data3.getId()));
        Assert.assertTrue(dataRepository.existsById(data4.getId()));
        Assert.assertTrue(dataRepository.existsById(data5.getId()));

        Assert.assertEquals(5, dataRepository.findAll().size());

        Assert.assertEquals(Integer.valueOf(3), dataRepository.countAll(false));
        Assert.assertEquals(Integer.valueOf(5), dataRepository.countAll(true));

        List<Data> datas = dataRepository.findAllByDatasetId(dsid);
        Assert.assertTrue(datas.contains(data1));
        Assert.assertTrue(datas.contains(data2));
        Assert.assertTrue(datas.contains(data3));

        datas = dataRepository.findAllByDatasetId(dsid2);
        Assert.assertTrue(datas.contains(data4));
        Assert.assertTrue(datas.contains(data5));

        datas = dataRepository.findAllByVisibility(false);
        Assert.assertTrue(datas.contains(data1));
        Assert.assertTrue(datas.contains(data2));
        Assert.assertTrue(datas.contains(data4));

        datas = dataRepository.findAllByVisibility(true);
        Assert.assertTrue(datas.contains(data3));
        Assert.assertTrue(datas.contains(data5));

        datas = dataRepository.findByDatasetId(dsid, true, true);
        Assert.assertTrue(datas.contains(data3));

        datas = dataRepository.findByProviderId(pid);
        Assert.assertTrue(datas.contains(data1));
        Assert.assertTrue(datas.contains(data3));
        Assert.assertTrue(datas.contains(data4));

        datas = dataRepository.findByProviderId(pid2);
        Assert.assertTrue(datas.contains(data2));
        Assert.assertTrue(datas.contains(data5));

        datas = dataRepository.findByProviderId(pid, "COVERAGE", true, false);
        Assert.assertTrue(datas.contains(data1));

        List<Integer> dids = dataRepository.findIdsByProviderId(pid);
        Assert.assertTrue(dids.contains(did1));
        Assert.assertTrue(dids.contains(did3));
        Assert.assertTrue(dids.contains(did4));

        dids = dataRepository.findIdsByProviderId(pid2);
        Assert.assertTrue(dids.contains(did2));
        Assert.assertTrue(dids.contains(did5));

        dids = dataRepository.findIdsByProviderId(pid, "COVERAGE", true, false);
        Assert.assertTrue(dids.contains(did1));

        Data d = dataRepository.findByNameAndNamespaceAndProviderId("test data 3", "", pid);
        Assert.assertNotNull(d);
        Assert.assertEquals(data3.getId(), d.getId());

        d = dataRepository.findByNameAndNamespaceAndProviderId("bla bloiu '; quote", "bla '; select * from admin.datas;", pid);
        Assert.assertNotNull(d);
        Assert.assertEquals(data4.getId(), d.getId());

        /**
         * Search
         */
        Map<String, Object> filters = new HashMap<>();
        Entry<String, String> sortEntry = null;
        Entry<Integer, List<Data>> dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(3), dataFound.getKey());

        filters.put("hidden", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data3));

        filters.clear();
        filters.put("dataset", dsid);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(2), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data1));
        Assert.assertTrue(dataFound.getValue().contains(data2));

        filters.clear();
        filters.put("dataset", dsid);
        filters.put("hidden", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data3));

        // not hidden data from provider 1
        filters.clear();
        filters.put("provider_id", pid);
        filters.put("hidden", false);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(2), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data1));
        Assert.assertTrue(dataFound.getValue().contains(data4));

        // hidden data from provider 1
        filters.clear();
        filters.put("provider_id", pid);
        filters.put("hidden", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data3));

        //data from provider 1 and 2
        filters.clear();
        filters.put("OR", Arrays.asList(new AbstractMap.SimpleEntry<>("provider_id", pid), new AbstractMap.SimpleEntry<>("provider_id", pid2)));
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(3), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data1));
        Assert.assertTrue(dataFound.getValue().contains(data2));
        Assert.assertTrue(dataFound.getValue().contains(data4));

        //with a AND it should return 0 results
        filters.clear();
        filters.put("AND", Arrays.asList(new AbstractMap.SimpleEntry<>("provider_id", pid), new AbstractMap.SimpleEntry<>("provider_id", pid2)));
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(0), dataFound.getKey());

        filters.clear();
        filters.put("included", false);
        filters.put("hidden", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data5));

        filters.clear();
        filters.put("rendered", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data1));

        filters.clear();
        filters.put("sub_type", "point");
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data2));

        filters.clear();
        filters.put("sensorable", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data2));

        filters.clear();
        filters.put("term", "test data");
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(2), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data1));
        Assert.assertTrue(dataFound.getValue().contains(data2));

        filters.clear();
        filters.put("term", "test data");
        filters.put("hidden", true);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data3));

        filters.clear();
        filters.put("term", "test data");
        filters.put("hidden", true);
        filters.put("included", false);
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data5));

        filters.clear();
        filters.put("type", "COVERAGE");
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data1));

        filters.clear();
        filters.put("sub_type", "'; drop table admin.datas;'");
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data4));

        filters.clear();
        filters.put("term", "'; quote");
        dataFound = dataRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(Integer.valueOf(1), dataFound.getKey());
        Assert.assertTrue(dataFound.getValue().contains(data4));


        /**
         * Data deletion
         */
        int res = dataRepository.delete(data1.getId());
        Assert.assertEquals(1, res);

        res = dataRepository.delete(data2.getId());
        Assert.assertEquals(1, res);

        res = dataRepository.delete(data3.getId());
        Assert.assertEquals(1, res);

        res = dataRepository.delete(data4.getId());
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
