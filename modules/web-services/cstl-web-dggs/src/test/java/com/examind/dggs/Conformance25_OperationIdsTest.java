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

import java.net.URI;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.model.LinkRelations;
import org.geotoolkit.ogcapi.model.common.LandingPage;
import org.geotoolkit.ogcapi.model.common.Link;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Requirements class 25: Requirements Class API Definition Operation IDs
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_operation-ids
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_operation-ids
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance25_OperationIdsTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 18.2.  Operation IDs
    // A.25.1.  Abstract Test for Operation IDs
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_operation-ids_operation-ids
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_operation-ids_operation-ids
     */
    @Test
    public void requirement39() throws Exception {
        initLayerList();

        { //check description links found
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default");
            final LandingPage dto = sendRequestAndParse(uri, "application/json", LandingPage.class);

            boolean descFound = false;
            boolean docFound = false;
            for (Link link : dto.getLinks()) {
                if (LinkRelations.SERVICE_DESC.equals(link.getRel())) {
                    descFound = true;
                    assertTrue(link.getHref().endsWith("/WS/dggs/default/api?f=json"));
                }
                if (LinkRelations.SERVICE_DOC.equals(link.getRel())) {
                    docFound = true;
                    assertTrue(link.getHref().endsWith("/WS/dggs/default/api?f=html"));
                }
            }
            assertTrue(descFound);
            assertTrue(docFound);
        }

        { //check description content
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/api?f=json");
            final String dto = sendRequestAndParse(uri, "application/json", String.class);

            assertTrue(dto.contains("\"operationId\": \"getLandingPage\""));
            assertTrue(dto.contains("\"operationId\": \"getConformance\""));
            assertTrue(dto.contains("\"operationId\": \"getAPI\""));
            assertTrue(dto.contains("\"operationId\": \"getCollectionsList\""));
            assertTrue(dto.contains("\"operationId\": \"getCollection\""));
            assertTrue(dto.contains("\"operationId\": \".dataset.getDGGRSList\""));
            assertTrue(dto.contains("\"operationId\": \".dataset.getDGGRS\""));
            assertTrue(dto.contains("\"operationId\": \".dataset.getDGGRSZoneInfo\""));
            assertTrue(dto.contains("\"operationId\": \".collection.getDGGRSList\""));
            assertTrue(dto.contains("\"operationId\": \".collection.getDGGRS\""));
            assertTrue(dto.contains("\"operationId\": \".collection.getDGGRSZoneInfo\""));
            assertTrue(dto.contains("\"operationId\": \".dataset.getDGGRSZoneData\""));
            assertTrue(dto.contains("\"operationId\": \".collection.getDGGRSZoneData\""));
            assertTrue(dto.contains("\"operationId\": \".dataset.getDGGRSZones\""));
            assertTrue(dto.contains("\"operationId\": \".collection.getDGGRSZones\""));
        }
    }

}
