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
import java.util.Set;
import org.constellation.dto.DataSource;
import org.constellation.dto.DataSourcePath;
import org.constellation.dto.DataSourcePathComplete;
import org.constellation.repository.DatasourceRepository;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class DatasourceRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private DatasourceRepository datasourceRepository;

    public void crude() {

        datasourceRepository.deleteAll();
        List<DataSource> all = datasourceRepository.findAll();
        Assert.assertTrue(all.isEmpty());

        /**
         * datasource insertion
         */
        int did = datasourceRepository.create(TestSamples.newDataSource());
        DataSourcePath dsPath = new DataSourcePath(did, "/", "fold", true, null, 0l);
        Map<String, String> types = new HashMap<>();
        datasourceRepository.addAnalyzedPath(dsPath, types);

        dsPath = new DataSourcePath(did, "/file1", "file1", false, "/", 123l);
        types.put("store1", "type1");
        datasourceRepository.addAnalyzedPath(dsPath, types);

        dsPath = new DataSourcePath(did, "/file2", "file2", false, "/", 123l);
        types.clear();
        types.put("store1", "type2");
        datasourceRepository.addAnalyzedPath(dsPath, types);
        
        dsPath = new DataSourcePath(did, "/file3", "file3", false, "/", 123l);
        types.clear();
        types.put("store2", "type1");
        datasourceRepository.addAnalyzedPath(dsPath, types);

        datasourceRepository.addSelectedPath(did, "/file1");
        datasourceRepository.addSelectedPath(did, "/file2");

        DataSource ds1 = datasourceRepository.findById(did);
        Assert.assertNotNull(ds1);

        int did2 = datasourceRepository.create(TestSamples.newDataSourceQuote());

        DataSource ds2 = datasourceRepository.findById(did2);
        Assert.assertNotNull(ds2);

        /**
         * datasource verification
         */
        List<DataSource> dss = datasourceRepository.search("file:///home/test", null, null, null);
        Assert.assertEquals(1, dss.size());
        DataSource ds = dss.get(0);
        Assert.assertNotNull(ds);

        Assert.assertEquals("PENDING", datasourceRepository.getAnalysisState(did));

        Map<String,Set<String>> stores = datasourceRepository.getDatasourceStores(did);
        Assert.assertTrue(stores.containsKey("store1"));
        Assert.assertTrue(stores.containsKey("store2"));
        Assert.assertTrue(stores.get("store1").contains("type1"));
        Assert.assertTrue(stores.get("store1").contains("type2"));
        Assert.assertTrue(stores.get("store2").contains("type1"));

        DataSourcePathComplete dpc = datasourceRepository.getAnalyzedPath(did, "/file1");
        Assert.assertNotNull(dpc);
        
        /*
        * selected path
        */
        Assert.assertTrue(datasourceRepository.hasSelectedPath(did));
        Assert.assertTrue(datasourceRepository.existSelectedPath(did, "/file1"));
        datasourceRepository.clearSelectedPath(did);
        Assert.assertFalse(datasourceRepository.hasSelectedPath(did));

        datasourceRepository.deletePath(did, "/file1");
        dpc = datasourceRepository.getAnalyzedPath(did, "/file1");
        Assert.assertNull(dpc);

        dss = datasourceRepository.search("file:///home/tes't", null, null, null);
        Assert.assertEquals(1, dss.size());
        ds = dss.get(0);
        Assert.assertNotNull(ds);


        /**
         * datasource delete
         */
        datasourceRepository.delete(did);

        ds = datasourceRepository.findById(did);
        Assert.assertNull(ds);
    }

}
