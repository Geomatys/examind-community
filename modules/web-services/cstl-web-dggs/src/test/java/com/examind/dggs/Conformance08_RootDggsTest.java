/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
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
package com.examind.dggs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpResponse;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.LinkRelations;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.ogcapi.dto.dggs.DggrsListResponse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Requirements class 8: Requirements Class Root DGGS
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_root-dggs
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_root-dggs
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance08_RootDggsTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 14.2.  Root DGGS (/dggs)
    // A.8.1.  Abstract Test for Requirement Root DGGS
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_root-dggs_dggs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_root-dggs_dggs
     */
    @Test
    public void requirement22() throws Exception {
        initLayerList();

        { //test link from landing page
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/");
            final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
            final LandingPage dto = new ObjectMapper().readValue(response.body(), LandingPage.class);
            boolean dggrsListFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.OGC_DGGRS_LIST.equals(link.getRel())) {
                    dggrsListFound = true;
                }
            }
            assertTrue(dggrsListFound);
        }

        { //test link from collection item
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/coveragepng");
            final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
            final CollectionDescription dto = new ObjectMapper().readValue(response.body(), CollectionDescription.class);
            boolean dggrsListFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.OGC_DGGRS_LIST.equals(link.getRel())) {
                    dggrsListFound = true;
                }
            }
            assertTrue(dggrsListFound);
        }

        { //test link to dataset
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/coveragepng/dggs");
            final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
            final DggrsListResponse dto = new ObjectMapper().readValue(response.body(), DggrsListResponse.class);
            boolean datasetLinkFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.OGC_DATASET.equals(link.getRel())) {
                    datasetLinkFound = true;
                }
            }
            assertTrue(datasetLinkFound);
        }

        { //test link to dataset
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/coveragepng/dggs/Healpix");
            final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
            final DggrsListResponse dto = new ObjectMapper().readValue(response.body(), DggrsListResponse.class);
            boolean datasetLinkFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.OGC_DATASET.equals(link.getRel())) {
                    datasetLinkFound = true;
                }
            }
            assertTrue(datasetLinkFound);
        }
    }

}
