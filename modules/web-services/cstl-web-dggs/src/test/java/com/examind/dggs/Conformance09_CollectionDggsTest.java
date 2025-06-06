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
import java.util.ArrayList;
import java.util.List;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.model.LinkRelations;
import org.geotoolkit.ogcapi.model.common.CollectionDescription;
import org.geotoolkit.ogcapi.model.common.Collections;
import org.geotoolkit.ogcapi.model.common.Link;
import org.geotoolkit.ogcapi.model.dggs.DggrsItem;
import org.geotoolkit.ogcapi.model.dggs.DggrsListResponse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Requirements class 9: Requirements Class Collection DGGS
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_collection-dggs
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_collection-dggs
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance09_CollectionDggsTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 15.2.  Collection DGGS (/collections/{collectionId}/dggs)
    // A.9.1.  Abstract Test for Requirement Collection DGGS
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_collection-dggs_dggs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_collection-dggs_dggs
     */
    @Test
    public void requirement23() throws Exception {
        initLayerList();

        final List<String> collectionIds = new ArrayList<>();
        {
            final Collections dto = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections"),
                    "application/json", Collections.class);
            for (CollectionDescription desc : dto.getCollections()) {
                collectionIds.add(desc.getId());
            }
        }
        assertTrue(!collectionIds.isEmpty());

        for (String collectionId : collectionIds) {

            { //check dggrs link
                final CollectionDescription colDto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+collectionId),
                        "application/json", CollectionDescription.class);

                boolean dggrsLinkFound = false;
                for (Link link : colDto.getLinks()) {
                    assertTrue(link.getHref() != null && !link.getHref().isBlank());
                    if (LinkRelations.OGC_DGGRS_LIST.equals(link.getRel())) {
                        dggrsLinkFound = true;
                        assertTrue(link.getHref().endsWith("/WS/dggs/default/collections/"+collectionId+"/dggs"));
                    }
                }
                assertTrue(dggrsLinkFound);
            }


            final List<String> dggrsIds = new ArrayList<>();
            { //check reverse link from dggrs list to data
                final DggrsListResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+collectionId+"/dggs"),
                        "application/json", DggrsListResponse.class);
                boolean geodataLinkFound = false;
                for (Link link : dto.getLinks()) {
                    assertTrue(link.getHref() != null && !link.getHref().isBlank());
                    if (LinkRelations.OGC_GEODATA.equals(link.getRel())) {
                        geodataLinkFound = true;
                        assertTrue(link.getHref().endsWith("/WS/dggs/default/collections/"+collectionId));
                    }
                }
                assertTrue(geodataLinkFound);

                for (DggrsItem dggrs : dto.getDggrs()) {
                    dggrsIds.add(dggrs.getId());
                }
            }
            assertTrue(!dggrsIds.isEmpty());

            //check reverse link from dggrs to data
            for (String dggrsId : dggrsIds) {
                final DggrsListResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+collectionId+"/dggs/"+ dggrsId),
                        "application/json", DggrsListResponse.class);
                boolean geodataLinkFound = false;
                for (Link link : dto.getLinks()) {
                    assertTrue(link.getHref() != null && !link.getHref().isBlank());
                    if (LinkRelations.OGC_GEODATA.equals(link.getRel())) {
                        geodataLinkFound = true;
                        assertTrue(link.getHref().endsWith("/WS/dggs/default/collections/"+collectionId));
                    }
                }
                assertTrue(geodataLinkFound);
            }


        }

    }

}
