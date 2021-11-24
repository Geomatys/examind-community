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
import org.constellation.dto.CstlUser;
import org.constellation.dto.metadata.Metadata;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.MetadataRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MetadataRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private MetadataRepository metadataRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    public void crude() {

       //cleanup
        dataRepository.deleteAll();
        datasetRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();
        metadataRepository.deleteAll();

        List<Metadata> all = metadataRepository.findAll();
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
         * metadata insertion
         */
        int mid1 = metadataRepository.create(TestSamples.newMetadata(owner.getId(), "meta-1", did1, null, null));
        Assert.assertNotNull(mid1);

        int mid2 = metadataRepository.create(TestSamples.newMetadata(owner.getId(), "meta-2", null, dsid, null));
        Assert.assertNotNull(mid2);

        int mid3 = metadataRepository.create(TestSamples.newMetadata(owner.getId(), "meta-3", null, null, sid));
        Assert.assertNotNull(mid3);

        int mid4 = metadataRepository.create(TestSamples.newMetadataQuote(owner.getId(), "meta'4"));
        Assert.assertNotNull(mid4);

        Metadata m1 = metadataRepository.findById(mid1);
        Assert.assertNotNull(m1);
        Metadata m2 = metadataRepository.findById(mid2);
        Assert.assertNotNull(m2);
        Metadata m3 = metadataRepository.findById(mid3);
        Assert.assertNotNull(m3);
        Metadata m4 = metadataRepository.findById(mid4);
        Assert.assertNotNull(m4);

        /**
         * metadata search
         */
        List<Metadata> metas = metadataRepository.findByDataId(did1);
        Assert.assertTrue(metas.contains(m1));

        Assert.assertEquals(m2, metadataRepository.findByDatasetId(dsid));

        Assert.assertEquals(m3, metadataRepository.findByServiceId(sid));

        Assert.assertEquals(m4, metadataRepository.findByMetadataId("meta'4"));

        Assert.assertTrue(metadataRepository.existMetadataTitle("tt'le"));

        final Map<String,Object> filterMap = new HashMap<>();
        filterMap.put("owner", owner.getId());
        List<Map<String, Object>> results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(4, results.size());
        Assert.assertEquals(mid1, (int) results.get(0).get("id"));

        filterMap.clear();
        filterMap.put("data", did1);
        results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(mid1, (int) results.get(0).get("id"));

        filterMap.clear();
        filterMap.put("dataset", dsid);
        results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(mid2, (int) results.get(0).get("id"));

        filterMap.clear();
        filterMap.put("profile", "profile_import");
        results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(3, results.size());
        Assert.assertEquals(mid1, (int) results.get(0).get("id"));
        Assert.assertEquals(mid2, (int) results.get(1).get("id"));
        Assert.assertEquals(mid3, (int) results.get(2).get("id"));

        filterMap.clear();
        filterMap.put("profile", "profile'; delete * from * 'import");
        results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(mid4, (int) results.get(0).get("id"));

        filterMap.clear();
        filterMap.put("identifier", "meta'4");
        results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(mid4, (int) results.get(0).get("id"));

        filterMap.clear();
        filterMap.put("term", "tt'le");
        results = metadataRepository.filterAndGetWithoutPagination(filterMap);
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(mid4, (int) results.get(0).get("id"));

        /**
         * metadata deletion
         */
        metadataRepository.delete(mid1);
        metadataRepository.delete(mid2);
        metadataRepository.delete(mid3);

        m1 = metadataRepository.findById(mid1);
        Assert.assertNull(m1);

        //cleanup
        dataRepository.deleteAll();
        datasetRepository.deleteAll();
        serviceRepository.deleteAll();
        providerRepository.deleteAll();
        metadataRepository.deleteAll();
    }
}
