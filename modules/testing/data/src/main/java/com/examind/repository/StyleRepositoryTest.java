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
import org.constellation.dto.Layer;
import org.constellation.dto.Style;
import org.constellation.repository.DataRepository;
import org.constellation.repository.DatasetRepository;
import org.constellation.repository.LayerRepository;
import org.constellation.repository.ProviderRepository;
import org.constellation.repository.ServiceRepository;
import org.constellation.repository.StyleRepository;
import org.junit.Assert;
import org.junit.Test;
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

    @Test
    public void crude() {

        // no removeAll method
        List<Style> all = styleRepository.findAll();
        for (Style p : all) {
            styleRepository.delete(p.getId());
        }
        all = styleRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        int sid = styleRepository.create(TestSamples.newStyle(owner.getId(), "ds1"));
        Assert.assertNotNull(sid);

        Style s = styleRepository.findById(sid);
        Assert.assertNotNull(s);

        styleRepository.delete(s.getId());

        s = styleRepository.findById(s.getId());
        Assert.assertNull(s);
    }

    @Test
    public void layersLink() {
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
         * link with style
         */
        Style style1 = TestSamples.newStyle(owner.getId(), "style1");
        int stid = styleRepository.create(style1);
        Assert.assertNotNull(stid);
        style1.setId(stid);

        styleRepository.linkStyleToLayer(stid, lid);

        List<Style> styles = styleRepository.findByLayer(lid);
        Assert.assertEquals(1, styles.size());
        Assert.assertEquals(style1, styles.get(0));


        styleRepository.unlinkStyleToLayer(stid, lid);
        styles = styleRepository.findByLayer(lid);
        Assert.assertEquals(0, styles.size());

        // cleanup
        styleRepository.delete(stid);
        layerRepository.delete(lid);

    }

}