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
import org.constellation.dto.DataSource;
import org.constellation.repository.DatasourceRepository;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@Transactional
public class DatasourceRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DatasourceRepository datasourceRepository;

    @Test
    @Transactional()
    public void crude() {

        // no removeAll method
        List<DataSource> all = datasourceRepository.findAll();
        for (DataSource p : all) {
            datasourceRepository.delete(p.getId());
        }
        all = datasourceRepository.findAll();
        Assert.assertTrue(all.isEmpty());


        /**
         * datasource insertion
         */
        int did = datasourceRepository.create(TestSamples.newDataSource());
        Assert.assertNotNull(did);

        DataSource s = datasourceRepository.findById(did);
        Assert.assertNotNull(s);

        /**
         * datasource search
         */
        Assert.assertNotNull(datasourceRepository.findByUrl(s.getUrl()));

        /**
         * datasource delete
         */
        datasourceRepository.delete(did);

        s = datasourceRepository.findById(did);
        Assert.assertNull(s);
    }

}
