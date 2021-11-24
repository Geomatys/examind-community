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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.constellation.dto.CstlUser;
import org.constellation.dto.DataSet;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.ProviderRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DatasetRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private ProviderRepository providerRepository;

    public void crude() {

        dataRepository.deleteAll();
        datasetRepository.deleteAll();
        List<DataSet> all = datasetRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        CstlUser owner2 = getOrCreateUser(TestSamples.newDataUser());
        Assert.assertNotNull(owner2);
        Assert.assertNotNull(owner2.getId());

        int pid = providerRepository.create(TestSamples.newProvider(owner.getId()));

        /**
         * dataset insertion
         */
        Integer did = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "ds1"));
        Assert.assertNotNull(did);

        DataSet ds1 = datasetRepository.findById(did);
        Assert.assertNotNull(ds1);

        Integer did2 = datasetRepository.create(TestSamples.newDataSet(owner2.getId(), "ds'; delete * from admin.dataset'"));
        Assert.assertNotNull(did);

        DataSet ds2 = datasetRepository.findById(did2);
        Assert.assertNotNull(ds2);

        Integer did3 = datasetRepository.create(TestSamples.newDataSet(owner2.getId(), "ds3"));
        Assert.assertNotNull(did3);
        DataSet ds3 = datasetRepository.findById(did3);
        Assert.assertNotNull(ds3);

        /**
         * data insertion
         */
        int dataId1 = dataRepository.create(TestSamples.newData1(owner.getId(), pid, did));
        int dataId2 = dataRepository.create(TestSamples.newData2(owner2.getId(), pid, did2));

        /**
         * dataset search
         */
        DataSet ds = datasetRepository.findByIdentifier("ds1");
        Assert.assertNotNull(ds);
        Assert.assertEquals(did, ds.getId());

        all = datasetRepository.findAll();
        Assert.assertTrue(all.contains(ds1));
        Assert.assertTrue(all.contains(ds2));

        ds = datasetRepository.findByIdentifier("ds'; delete * from admin.dataset'");
        Assert.assertNotNull(ds);
        Assert.assertEquals(did2, ds.getId());

        /**
         * search full
         */
        Map<String, Object> filterMap = new HashMap<>();
        Entry<String, String> sortEntry = null;
        Entry<Integer, List<DataSet>> results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(3), results.getKey());
        Assert.assertEquals(3, results.getValue().size());
        Assert.assertTrue(results.getValue().contains(ds1));
        Assert.assertTrue(results.getValue().contains(ds2));
        Assert.assertTrue(results.getValue().contains(ds3));

        /**
         * search by id
         */
        filterMap.put("id", did);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(1), results.getKey());
        Assert.assertEquals(1, results.getValue().size());
        Assert.assertEquals(ds1, results.getValue().get(0));

        /**
         * search by wrong id
         */
        filterMap.put("id", 999666);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(0), results.getKey());
        Assert.assertEquals(0, results.getValue().size());

        filterMap.clear();
        filterMap.put("owner", owner2.getId());
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(2), results.getKey());
        Assert.assertEquals(2, results.getValue().size());
        Assert.assertTrue(results.getValue().contains(ds2));
        Assert.assertTrue(results.getValue().contains(ds));

        filterMap.clear();
        filterMap.put("term", "ds'; delete * from admin.dataset'");
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(1), results.getKey());
        Assert.assertEquals(1, results.getValue().size());
        Assert.assertTrue(results.getValue().contains(ds2));

        filterMap.clear();
        filterMap.put("hasVectorData", true);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(1), results.getKey());
        Assert.assertEquals(1, results.getValue().size());
        Assert.assertTrue(results.getValue().contains(ds2));

        filterMap.clear();
        filterMap.put("hasCoverageData", true);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(1), results.getKey());
        Assert.assertEquals(1, results.getValue().size());
        Assert.assertTrue(results.getValue().contains(ds1));

        filterMap.clear();
        filterMap.put("excludeEmpty", true);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(2), results.getKey());
        Assert.assertEquals(2, results.getValue().size());
        Assert.assertTrue(results.getValue().contains(ds2));
        Assert.assertTrue(results.getValue().contains(ds1));

        /**
         * dataset deletion
         */
        dataRepository.deleteAll();
        datasetRepository.delete(did);

        ds1 = datasetRepository.findById(did);
        Assert.assertNull(ds1);

        datasetRepository.delete(did);
        datasetRepository.delete(did2);
        datasetRepository.delete(did3);
    }

}
