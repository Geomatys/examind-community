/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014-2017 Geomatys.
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

package org.constellation.json.metadata.binding;

import org.constellation.dto.metadata.RootBlock;
import org.constellation.dto.metadata.SuperBlockObj;
import org.constellation.dto.metadata.Block;
import org.constellation.dto.metadata.RootObj;
import org.constellation.dto.metadata.Field;
import org.constellation.dto.metadata.SuperBlock;
import org.constellation.dto.metadata.FieldObj;
import org.constellation.dto.metadata.BlockObj;
import org.constellation.dto.metadata.ComponentObj;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JsonBindingTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    public void mashallingTest() throws IOException {
        final RootObj root = new RootObj();
        final RootBlock rb = new RootBlock();
        final SuperBlockObj sbo = new SuperBlockObj();
        final SuperBlock sb = new SuperBlock();
        final BlockObj bo = new BlockObj();
        final Block b = new Block();
        
        final Field f = new Field();
        f.setRender("text");
        f.setDefaultValue("defautt");
        f.setValue("test");
                
        final Field f2 = new Field();
        f2.setRender("integer");
        f2.setDefaultValue("36");
        f2.setValue("2");
        
        
        final FieldObj fo  = new FieldObj(f);
        final FieldObj fo2 = new FieldObj(f2);
        final Block b2 = new Block();
        final BlockObj bo2 = new BlockObj(b2);
        final List<ComponentObj> compo = new ArrayList<>();
        compo.add(fo);
        compo.add(fo2);
        compo.add(bo2);
        b.setChildren(compo);
        bo.setBlock(b);
        sb.setChildren(Arrays.asList(bo));
        sbo.setSuperblock(sb);
        rb.setChildren(Arrays.asList(sbo));
        root.setRoot(rb);
        
        final StringWriter sw = new StringWriter();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(sw, root);
        //System.out.println(sw.toString());
        
        RootObj ro = objectMapper.readValue(new StringReader(sw.toString()), RootObj.class);
        
        FieldObj f1 = (FieldObj) ro.getRoot().getChildren().get(0).getSuperblock().getBlocks().get(0).getChildren().get(0);
        //System.out.println(f1.getField().value);
    }
}
