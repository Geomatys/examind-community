/*
 *    Examind community - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2026 Geomatys.
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
package com.examind.process.admin.yamlReader;

import com.examind.process.admin.AdminProcessRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.constellation.test.utils.JSONComparator;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/cstl/spring/test-no-hazelcast.xml")
public class ProcessFromYamlTest {
    
    @Test
    public void harvesterCSVFlatProfileSingleFromYamlNoFIlterTest() throws Exception {

        // Create a temporary yaml file.
        Path tempFile = Files.createTempFile(null, null);
        List<String> listYamlParameter = List.of(
                "process_name: math:add",
                "first: 1.1",
                "second: 2.0"
        );
        Files.write(tempFile, listYamlParameter, StandardOpenOption.APPEND);

        ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(AdminProcessRegistry.NAME, "yamlReader");
        ParameterValueGroup in = desc.getInputDescriptor().createValue();

        in.parameter(ProcessFromYamlProcessDescriptor.DATA_FOLDER_NAME).setValue(tempFile);
        org.geotoolkit.process.Process process = desc.createProcess(in); // Create the process

        ParameterValueGroup results = process.call();// Call the process.

        String jsonResult = results.parameter("out").stringValue();
       
        String expected = """
                          {"result" : [ 3.1 ]}
                          """;
        JSONComparator.compareJSON(expected, jsonResult);
    }

    
}
