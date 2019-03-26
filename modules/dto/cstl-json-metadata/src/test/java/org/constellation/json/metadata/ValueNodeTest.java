/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2017 Geomatys.
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
package org.constellation.json.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.constellation.dto.metadata.RootObj;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class ValueNodeTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void testTreeRepresentation() throws IOException {
        final InputStream stream = TemplateWriterTest.class.getResourceAsStream("profile_special_type.json");
        final RootObj root       =  objectMapper.readValue(stream, RootObj.class);
        final TemplateTree tree  = TemplateTree.getTreeFromRootObj(root);
        ValueNode node = tree.getRoot();

        System.out.println(node.treeRepresentation());
    }

}
