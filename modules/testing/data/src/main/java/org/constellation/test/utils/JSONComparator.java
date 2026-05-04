/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2020 Geomatys.
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
package org.constellation.test.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import java.util.Comparator;
import java.util.List;
import org.geotoolkit.test.xml.DocumentComparator;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class JSONComparator implements Comparator<JsonNode> {
    
    @Override
    public int compare(JsonNode o1, JsonNode o2) {
        if (o1.equals(o2)) {
            return 0;
        }
        // special case for dynamic value that we don"t want to compare
        if ("<whatever>".equals(o1.asText()) ||
            "<whatever>".equals(o2.asText())) {
            return 0;
        }
        if (o1.asText() != null && o1.asText().startsWith("<?xml") &&
            o2.asText() != null && o2.asText().startsWith("<?xml")) {
            return 0;
        }
        if ((o1 instanceof NumericNode) && (o2 instanceof NumericNode)){
            Double d1 = ((NumericNode) o1).asDouble();
            Double d2 = ((NumericNode) o2).asDouble();
            Assert.assertEquals(d1, d2, 0.0001);
            return 0;
        }
        // TODO
        if (o1.isContainerNode() && o2.isContainerNode()) {
            ContainerNode c1 = (ContainerNode) o1;
            ContainerNode c2 = (ContainerNode) o2;
        }
        throw new AssertionError("expected:" + o1 +" but was " + o2);
    }
    
    public static void compareJSON(String expected, String result) throws Exception {
        JSONComparator comparator = new JSONComparator();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expectedNode = mapper.readTree(expected);
        JsonNode resultNode = mapper.readTree(result);

        boolean eq = expectedNode.equals(comparator, resultNode);

        StringBuilder sb = new StringBuilder("expected:\n");
        sb.append(expected).append("\nbut was:\n");
        sb.append(result);
        assertTrue(sb.toString(), eq);
    }
    
    protected static void xmlCompare(final Object actual, final Object expected, List<String> extraIgnoredAttributes, List<String> extraIgnoredNodes) throws Exception {

        final DocumentComparator comparator = new DocumentComparator(expected, actual);
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("updateSequence");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        if (extraIgnoredAttributes != null) {
            comparator.ignoredAttributes.addAll(extraIgnoredAttributes);
        }
        if (extraIgnoredNodes != null) {
            comparator.ignoredNodes.addAll(extraIgnoredNodes);
        }
        comparator.compare();
    }
}

