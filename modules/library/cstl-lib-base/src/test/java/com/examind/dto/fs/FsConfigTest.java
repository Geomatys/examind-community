/*
 *    Examind community - An open source and standard compliant SDI
 *
 * Copyright 2025 Geomatys.
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
package com.examind.dto.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.constellation.dto.contact.Details;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys).
 */
public class FsConfigTest {
    
    @Test
    public void cannotRemoveBadDirectory() throws IOException {
        Service s = new Service();
        s.setIdentifier("wms1");
        s.setType("WMS");
        Details metadata = new Details();
        metadata.setName("WMS 1");
        metadata.setVersions(List.of("1.1.1", "1.3.0"));
        s.setMetadata(metadata);
        
        Collection c = new Collection();
        c.setDataSet("ds1");
        c.setDatasetStyle("style_ds_1");
        
        CollectionItem item1 = new CollectionItem();
        item1.setAlias("alibi");
        item1.setName("data1_nm");
        item1.setNamespace("data2_nmsp");
        c.setData(List.of(item1));
        s.setCollections(List.of(c));
        
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, s);
        
        String expected = """
                          ---
                          identifier: "wms1"
                          type: "WMS"
                          metadata:
                            name: "WMS 1"
                            identifier: null
                            keywords: null
                            description: null
                            versions:
                            - "1.1.1"
                            - "1.3.0"
                            lang: null
                            serviceContact: null
                            serviceConstraints: null
                            transactional: false
                          collections:
                          - dataSet: "ds1"
                            filter: null
                            datasetStyle: "style_ds_1"
                            includeAll: true
                            data:
                            - provider: null
                              name: "data1_nm"
                              namespace: "data2_nmsp"
                              title: null
                              alias: "alibi"
                              style: null
                              dimensions: []
                          processFactories: []
                          advancedParameters: {}
                          source: null
                          """;
        assertEquals(expected, sw.toString());
        
        
    }
}
