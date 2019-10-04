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
import org.constellation.dto.MapContextDTO;
import org.constellation.repository.MapContextRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapcontextRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private MapContextRepository mapcontextRepository;

    @Test
    public void crude() {

        CstlUser owner = getOrCreateUser();
        Assert.assertNotNull(owner);
        Assert.assertNotNull(owner.getId());

        // no removeAll method
        List<MapContextDTO> all = mapcontextRepository.findAll();
        for (MapContextDTO p : all) {
            mapcontextRepository.delete(p.getId());
        }
        all = mapcontextRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        int sid = mapcontextRepository.create(TestSamples.newMapcontext(owner, "mp"));
        Assert.assertNotNull(sid);

        MapContextDTO s = mapcontextRepository.findById(sid);
        Assert.assertNotNull(s);

        mapcontextRepository.delete(s.getId());

        s = mapcontextRepository.findById(s.getId());
        Assert.assertNull(s);
    }

}
