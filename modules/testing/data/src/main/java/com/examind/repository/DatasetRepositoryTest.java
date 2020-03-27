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
import org.constellation.dto.Data;
import org.constellation.dto.DataSet;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.junit.Assert;
import org.junit.Test;
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

    @Test
    public void crude() {

        List<Data> alldata = dataRepository.findAll();
        for (Data p : alldata) {
            dataRepository.delete(p.getId());
        }

        // no removeAll method
        List<DataSet> all = datasetRepository.findAll();
        for (DataSet p : all) {
            datasetRepository.delete(p.getId());
        }
        all = datasetRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        /**
         * dataset insertion
         */
        int did = datasetRepository.create(TestSamples.newDataSet(owner.getId(), "ds1"));
        Assert.assertNotNull(did);

        DataSet s = datasetRepository.findById(did);
        Assert.assertNotNull(s);

        /**
         * dataset search
         */
        Assert.assertNotNull(datasetRepository.findByIdentifier("ds1"));

        Assert.assertEquals(s.getId(), datasetRepository.findIdForIdentifier("ds1"));

        Assert.assertTrue(datasetRepository.findAll().contains(s));


        /**
         * search full
         */
        Map<String, Object> filterMap = new HashMap<>();
        Entry<String, String> sortEntry = null;
        Entry<Integer, List<DataSet>> results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(1), results.getKey());
        Assert.assertEquals(1, results.getValue().size());
        Assert.assertEquals(s, results.getValue().get(0));

        /**
         * search by id
         */
        filterMap.put("id", did);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(1), results.getKey());
        Assert.assertEquals(1, results.getValue().size());
        Assert.assertEquals(s, results.getValue().get(0));

        /**
         * search by wrong id
         */
        filterMap.put("id", 999666);
        results = datasetRepository.filterAndGet(filterMap, sortEntry, 1, 10);

        Assert.assertEquals(new Integer(0), results.getKey());
        Assert.assertEquals(0, results.getValue().size());

        /**
         * dataset deletion
         */
        datasetRepository.delete(did);

        s = datasetRepository.findById(did);
        Assert.assertNull(s);
    }

}
