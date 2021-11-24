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
import org.constellation.dto.Layer;
import org.constellation.dto.Style;
import org.constellation.dto.StyleReference;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.StyleRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class StyleRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private StyleRepository styleRepository;

    @Autowired
    private LayerRepository layerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private DataRepository dataRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    public void crude() {
        dataRepository.deleteAll();
        datasetRepository.deleteAll();
        providerRepository.deleteAll();
        layerRepository.deleteAll();
        serviceRepository.deleteAll();
        styleRepository.deleteAll();

        List<Style> all = styleRepository.findAll();
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

        /**
         * Layer insertion
         */
         Integer sid = serviceRepository.create(TestSamples.newService(owner.getId()));
        Assert.assertNotNull(sid);

        Integer lid1 = layerRepository.create(TestSamples.newLayer(owner.getId(), did1, sid));
        Assert.assertNotNull(lid1);

        Layer l1 = layerRepository.findById(lid1);
        Assert.assertNotNull(l1);

        Integer lid2 = layerRepository.create(TestSamples.newLayer2(owner.getId(), did1, sid));
        Assert.assertNotNull(lid1);

        Layer l2 = layerRepository.findById(lid2);
        Assert.assertNotNull(l2);

        /**
         * link with style
         */
        Style style1 = TestSamples.newStyle(owner.getId(), "style1");
        int stid1 = styleRepository.create(style1);
        Assert.assertNotNull(stid1);
        style1.setId(stid1);

        Style s = styleRepository.findById(stid1);
        Assert.assertEquals(s, style1);
        Assert.assertEquals(Boolean.TRUE, s.getIsShared());

        styleRepository.linkStyleToData(stid1, did1);
        styleRepository.linkStyleToLayer(stid1, lid1);

        Style style2 = TestSamples.newStyleQuote(owner.getId(), "style'; select *';1");
        int stid2 = styleRepository.create(style2);
        Assert.assertNotNull(stid2);
        style2.setId(stid2);

        s = styleRepository.findById(stid2);
        Assert.assertEquals(s, style2);
        Assert.assertEquals(Boolean.FALSE, s.getIsShared());

        styleRepository.linkStyleToData(stid2, did2);
        styleRepository.linkStyleToLayer(stid2, lid2);

        /**
         * Search
         */
        List<Style> styles = styleRepository.findByLayer(lid1);
        Assert.assertEquals(1, styles.size());
        Assert.assertEquals(style1, styles.get(0));

        styles = styleRepository.findByLayer(lid2);
        Assert.assertEquals(1, styles.size());
        Assert.assertEquals(style2, styles.get(0));

        styles = styleRepository.findByName("style'; select *';1");
        Assert.assertEquals(1, styles.size());
        Assert.assertEquals(style2, styles.get(0));

        styles = styleRepository.findByProvider(1);
        Assert.assertEquals(2, styles.size());
        Assert.assertTrue(styles.contains(style1));
        Assert.assertTrue(styles.contains(style2));

        styles = styleRepository.findByType("VE''CTOR");
        Assert.assertEquals(1, styles.size());
        Assert.assertEquals(style2, styles.get(0));

        styles = styleRepository.findByTypeAndProvider(1, "VE''CTOR");
        Assert.assertEquals(1, styles.size());
        Assert.assertEquals(style2, styles.get(0));


        List<StyleReference> refs = styleRepository.fetchByDataId(did1);
        Assert.assertEquals(1, refs.size());
        Assert.assertEquals(new StyleReference(stid1, "style1", 1, "sld"), refs.get(0));

        refs = styleRepository.fetchByDataId(did2);
        Assert.assertEquals(1, refs.size());
        Assert.assertEquals(new StyleReference(stid2, "style'; select *';1", 1, "sld"), refs.get(0));

        refs = styleRepository.fetchByLayerId(lid2);
        Assert.assertEquals(1, refs.size());
        Assert.assertEquals(new StyleReference(stid2, "style'; select *';1", 1, "sld"), refs.get(0));

        List<Integer> ids = styleRepository.getStyleIdsForData(did1);
        Assert.assertEquals(1, ids.size());
        Assert.assertTrue(ids.contains(stid1));

        Map<String, Object> filters = new HashMap<>();
        Entry<String, String> sortEntry = null;
        Entry<Integer, List<Style>> styleFound = styleRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(new Integer(2), styleFound.getKey());

        filters.clear();
        filters.put("isShared", false);
        styleFound = styleRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(new Integer(1), styleFound.getKey());
        Assert.assertTrue(styleFound.getValue().contains(style2));

        filters.clear();
        filters.put("owner", owner.getId());
        styleFound = styleRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(new Integer(2), styleFound.getKey());
        Assert.assertTrue(styleFound.getValue().contains(style1));
        Assert.assertTrue(styleFound.getValue().contains(style2));

        filters.clear();
        filters.put("type", "VE''CTOR");
        styleFound = styleRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(new Integer(1), styleFound.getKey());
        Assert.assertTrue(styleFound.getValue().contains(style2));

        filters.clear();
        filters.put("provider", 1);
        styleFound = styleRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(new Integer(2), styleFound.getKey());
        Assert.assertTrue(styleFound.getValue().contains(style1));
        Assert.assertTrue(styleFound.getValue().contains(style2));

        filters.clear();
        filters.put("term", "select *';");
        styleFound = styleRepository.filterAndGet(filters, sortEntry, 1, 10);
        Assert.assertEquals(new Integer(1), styleFound.getKey());
        Assert.assertTrue(styleFound.getValue().contains(style2));
        /**
         * update
         */
        styleRepository.unlinkStyleToLayer(stid1, lid1);
        styles = styleRepository.findByLayer(lid1);
        Assert.assertEquals(0, styles.size());

        Assert.assertEquals(Boolean.TRUE, style1.getIsShared());
        styleRepository.changeSharedProperty(stid1, false);
        s = styleRepository.findById(stid1);
        Assert.assertEquals(Boolean.FALSE, s.getIsShared());

        // cleanup
        dataRepository.deleteAll();
        datasetRepository.deleteAll();
        providerRepository.deleteAll();
        layerRepository.deleteAll();
        styleRepository.deleteAll();

    }

}