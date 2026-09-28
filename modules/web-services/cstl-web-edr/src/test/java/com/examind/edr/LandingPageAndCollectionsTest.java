/*
 *    Constellation - An open source and standard compliant SDI
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
package com.examind.edr;

import java.net.URI;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.common.Collections;
import org.geotoolkit.ogcapi.dto.common.ConfClasses;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.edr.EdrCollection;
import org.geotoolkit.ogcapi.dto.edr.EdrInstances;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Tests the non-query EDR endpoints: landing page, conformance, collections list/description and instances.
 *
 * @author Quentin Bialota (Geomatys)
 */
@RunWith(TestRunner.class)
public class LandingPageAndCollectionsTest extends EDRAbstractTest {

    @Test
    @Order(order = 1)
    public void landingPageTest() throws Exception {
        initLayerList();
        final LandingPage result = sendRequestAndParse(
                new URI("http://localhost:" + getCurrentPort() + "/WS/edr/default/"),
                "application/json", LandingPage.class);
        assertNotNull(result);
        assertNotNull(result.getLinks());
        assertFalse(result.getLinks().isEmpty());
    }

    @Test
    @Order(order = 2)
    public void conformanceTest() throws Exception {
        initLayerList();
        final ConfClasses result = sendRequestAndParse(
                new URI("http://localhost:" + getCurrentPort() + "/WS/edr/default/conformance"),
                "application/json", ConfClasses.class);
        assertNotNull(result);
        assertTrue(result.getConformsTo().stream().anyMatch(c -> c.contains("edr/1.1/conf/core")
                || c.toLowerCase().contains("edr")));
        assertTrue(result.getConformsTo().stream().anyMatch(c -> c.toLowerCase().contains("collections")));
    }

    @Test
    @Order(order = 3)
    public void collectionsTest() throws Exception {
        initLayerList();
        final Collections result = sendRequestAndParse(
                new URI("http://localhost:" + getCurrentPort() + "/WS/edr/default/collections"),
                "application/json", Collections.class);
        assertNotNull(result);
        assertNotNull(result.getCollections());
        assertFalse(result.getCollections().isEmpty());
        assertTrue(result.getCollections().stream().anyMatch(c -> "coveragepng".equals(c.getTitle())));
        // vector layers of the service are not EDR collections
        assertFalse(result.getCollections().stream().anyMatch(c -> "jsonfeature".equals(c.getTitle())));
    }

    @Test
    @Order(order = 4)
    public void collectionTest() throws Exception {
        initLayerList();
        final EdrCollection result = sendRequestAndParse(
                new URI("http://localhost:" + getCurrentPort() + "/WS/edr/default/collections/coveragepng"),
                "application/json", EdrCollection.class);
        assertNotNull(result);
        assertEquals("coveragepng", result.getTitle());
        assertNotNull(result.getDataQueries());
        assertNotNull(result.getExtent());
    }

    @Test
    @Order(order = 6)
    public void vectorCollectionNotFoundTest() throws Exception {
        initLayerList();
        final String base = "http://localhost:" + getCurrentPort() + "/WS/edr/default/collections/jsonfeature";
        assertEquals(404, sendRequest(new URI(base), "application/json").statusCode());
        assertEquals(404, sendRequest(new URI(base + "/instances"), "application/json").statusCode());
    }

    @Test
    @Order(order = 5)
    public void instancesTest() throws Exception {
        initLayerList();
        final EdrInstances result = sendRequestAndParse(
                new URI("http://localhost:" + getCurrentPort() + "/WS/edr/default/collections/coveragepng/instances"),
                "application/json", EdrInstances.class);
        assertNotNull(result);
        assertNotNull(result.getInstances());
        assertFalse(result.getInstances().isEmpty());
    }
}
