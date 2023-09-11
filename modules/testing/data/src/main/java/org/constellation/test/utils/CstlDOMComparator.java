/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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

import org.apache.sis.internal.util.DefinitionURI;
import org.geotoolkit.test.xml.DocumentComparator;
import org.apache.sis.util.CharSequences;
import org.junit.Assert;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class CstlDOMComparator extends DocumentComparator {

    public CstlDOMComparator(final Object expected, final Object actual) throws IOException, ParserConfigurationException, SAXException {
        super(handlePathInput(expected), handlePathInput(actual));
    }

    /**
     * Hack to add support of Path object to DocumentComparator.
     * Convert Path object to InputStream
     *
     * @param object
     * @return InputStream behind a Path or input object
     * @throws IOException
     */
    private static Object handlePathInput(Object object) throws IOException {
        if (object instanceof Path) {
            return Files.newInputStream((Path)object);
        }
        return object;
    }

    /**
     * Compares the names and namespaces of the given node.
     *
     * Exclude the prefix from comparison
     *
     * @param expected The node having the expected name and namespace.
     * @param actual The node to compare.
     */
    @Override
    protected void compareNames(final Node expected, final Node actual) {
        assertPropertyEquals("namespace", expected.getNamespaceURI(), actual.getNamespaceURI(), expected, actual);
        String expectedNodeName = expected.getNodeName();
        int i = expectedNodeName.indexOf(':');
        if (i != -1) {
            expectedNodeName = expectedNodeName.substring(i + 1);
        }
        String actualNodeName   = actual.getNodeName();
        i = actualNodeName.indexOf(':');
        if (i != -1) {
            actualNodeName = actualNodeName.substring(i + 1);
        }
        assertPropertyEquals("name", expectedNodeName, actualNodeName, expected, actual);
    }

    /**
     * Override compareNode to add special case on Lower and Upper corners of bbox from capabilities documents.
     * Compare actual double values of bboxes instead of sloppy String comparison.
     *
     * @param expected
     * @param actual
     */
    @Override
    protected void compareNode(Node expected, Node actual) {
        if (expected.getLocalName() != null) {
            switch (expected.getLocalName()) {
                case "posList":     //fall trough
                case "LowerCorner": //fall trough
                case "lowerCorner": //fall trough
                case "upperCorner": //fall trough
                case "UpperCorner":
                    double[] expectedDoubles = CharSequences.parseDoubles(expected.getTextContent(), ' ');
                    double[] actualDoubles = CharSequences.parseDoubles(actual.getTextContent(), ' ');
                    Assert.assertArrayEquals(expectedDoubles, actualDoubles, 0.01);
                    return;
                case "identifier":
                case "srsName":
                    if(expected.getTextContent()!=null && expected.getTextContent().startsWith("urn:")){
                        //only consider authority and code when testing URN
                        String str1 = expected.getTextContent();
                        if (actual != null) {
                            String str2 = actual.getTextContent();
                            DefinitionURI urn1 = DefinitionURI.parse(str1);
                            DefinitionURI urn2 = DefinitionURI.parse(str2);
                            Assert.assertNotNull(urn1);
                            Assert.assertNotNull(urn2);
                            Assert.assertNotNull(urn1.authority);
                            Assert.assertNotNull(urn2.authority);
                            Assert.assertNotNull(urn1.code);
                            Assert.assertNotNull(urn2.code);
                            Assert.assertEquals(urn1.authority.toLowerCase(), urn2.authority.toLowerCase());
                            Assert.assertEquals(urn1.code.toLowerCase(),      urn2.code.toLowerCase());
                            compareAttributes(expected, actual);
                            return;
                        }
                    }
                    break;
                case "codeSpace":
                    if(expected.getTextContent()!=null && expected.getTextContent().startsWith("EPSG")){
                        //ignore version when comparing EPSG codespace
                        Assert.assertTrue(actual.getTextContent().startsWith("EPSG"));
                        return;
                    }

                case "remarks":
                    //bypass tests on remark content
                    return;
            }
        }
        super.compareNode(expected, actual);
    }
}
