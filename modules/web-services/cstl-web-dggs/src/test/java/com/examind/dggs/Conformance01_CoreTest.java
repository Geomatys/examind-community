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
import java.util.ArrayList;
import java.util.List;
import org.constellation.test.utils.Order;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.LinkRelations;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.LandingPage;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.ogcapi.dto.dggs.Dggrs;
import org.geotoolkit.ogcapi.dto.dggs.DggrsItem;
import org.geotoolkit.ogcapi.dto.dggs.DggrsLinkTemplatesInner;
import org.geotoolkit.ogcapi.dto.dggs.DggrsListResponse;
import org.geotoolkit.ogcapi.dto.dggs.DggrsZonesResponse;
import org.geotoolkit.ogcapi.dto.dggs.ZoneInfo;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Requirements class 1: Requirements Class Core
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_core
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_core
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance01_CoreTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 7.2.  Listing available DGGRSs (…​/dggs)
    // A.1.1.  Abstract Test for Requirement Listing available DGGRS
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_core_dggrs-list
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_core_dggrs-list
     */
    @Test
    @Order(order = 1)
    public void requirement1() throws Exception {
        initLayerList();

        { //test dto
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs");

            assertTrue(uri.toString().endsWith("/dggs"));

            final HttpResponse<byte[]> response = sendRequest(uri, "application/json");
            final DggrsListResponse dto = new ObjectMapper().readValue(response.body(), DggrsListResponse.class);

            assertTrue(dto != null);
            assertTrue(!dto.getDggrs().isEmpty());

            for (DggrsItem dggrs : dto.getDggrs()) {
                assertTrue(dggrs.getId() != null && !dggrs.getId().isBlank());
                assertTrue(dggrs.getTitle() != null && !dggrs.getTitle().isBlank());
                assertTrue(dggrs.getUri() != null);
                assertTrue(!dggrs.getLinks().isEmpty());

                boolean selfLinkFound = false;
                boolean descriptionLinkFound = false;
                for (Link link : dggrs.getLinks()) {
                    assertTrue(link.getHref() != null && !link.getHref().isBlank());
                    if (LinkRelations.SELF.equals(link.getRel())) {
                        selfLinkFound = true;
                    }
                    if (LinkRelations.OGC_DGGRS_DEFINITION.equals(link.getRel())) {
                        descriptionLinkFound = true;
                    }
                }
                assertTrue(selfLinkFound);
                assertTrue(descriptionLinkFound);
            }
        }

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

    }

    // ////////////////////////////////////////////////////////////////////////
    // 7.3.  Discrete Global Grid Reference System (DGGRS) description (…​/dggs/{dggrsId})
    // A.1.2.  Abstract Test for Requirement Discrete global grid reference system description
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_core_dggrs-description
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_core_dggrs-description
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_core_max-refinement
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_per_core_beyond-max-refinement
     */
    @Test
    @Order(order = 2)
    public void requirement2() throws Exception {
        initLayerList();

        final List<String> dggrsIds = new ArrayList<>();
        {
            final DggrsListResponse dto = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs"),
                    "application/json", DggrsListResponse.class);
            for (DggrsItem dggrs : dto.getDggrs()) {
                dggrsIds.add(dggrs.getId());
            }
        }
        assertTrue(!dggrsIds.isEmpty());

        for (String dggrsId : dggrsIds) {
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId);
            final Dggrs dto = sendRequestAndParse(uri, "application/json", Dggrs.class);

            assertTrue(dggrsId.equals(dto.getId()));

            boolean selfLinkFound = false;
            boolean definitionLinkFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.SELF.equals(link.getRel())) {
                    selfLinkFound = true;
                    assertTrue(link.getHref().endsWith("/dggs/"+dggrsId));
                }
                if (LinkRelations.OGC_DGGRS_DEFINITION.equals(link.getRel())) {
                    definitionLinkFound = true;
                }
            }
            assertTrue(selfLinkFound);
            assertTrue(definitionLinkFound);

            boolean templateFound = false;
            for (DggrsLinkTemplatesInner template : dto.getLinkTemplates()) {
                if (LinkRelations.OGC_DGGRS_ZONE_INFO.equals(template.getRel())) {
                    templateFound = true;
                    assertTrue(template.getUriTemplate().contains("{zoneId}"));
                    assertTrue(template.getUriTemplate().endsWith("/dggs/"+dggrsId+"/zones/{zoneId}"));
                }
            }
            assertTrue(templateFound);

            //todo how to check this ?
            /*
            assert that if the discrete global grid reference system (the combination of the discrete global grid and indexing system)
            is registered with an authority, the resource includes a uri property corresponding to that registered discrete global grid reference system.
            */

            assertTrue(dto.getCrs() != null);
            assertTrue(dto.getTitle() != null && !dto.getTitle().isBlank());
            assertTrue(dto.getDescription()!= null && !dto.getDescription().isBlank());
            assertTrue(dto.getMaxRefinementLevel() != 0);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 7.4.  Retrieving zone information (…​/dggs/{dggrsId}/zones/{zoneId})
    // A.1.3.  Abstract Test for Requirement Retrieving zone information
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_core_zone-info
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_core_zone-info
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_core_zone-info
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_core_robots-txt
     */
    @Test
    @Order(order = 3)
    public void requirement3() throws Exception {
        initLayerList();

        final List<String> dggrsIds = new ArrayList<>();
        {
            final DggrsListResponse dto = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs"),
                    "application/json", DggrsListResponse.class);
            for (DggrsItem dggrs : dto.getDggrs()) {
                dggrsIds.add(dggrs.getId());
            }
        }
        assertTrue(!dggrsIds.isEmpty());

        for (String dggrsId : dggrsIds) {

            //get a list of root zones
            final DggrsZonesResponse dto = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId+"/zones"),
                    "application/json", DggrsZonesResponse.class);
            final List<String> zones = dto.getZones();
            assertTrue(!zones.isEmpty());

            for (String zoneId : zones) {
                final ZoneInfo zone = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId+"/zones/"+zoneId),
                    "application/json", ZoneInfo.class);

                assertTrue(zone.getId().equals(zoneId));

                boolean dggrsLinkFound = false;
                for (Link link : zone.getLinks()) {
                    assertTrue(link.getHref() != null && !link.getHref().isBlank());
                    if (LinkRelations.OGC_DGGRS.equals(link.getRel())) {
                        dggrsLinkFound = true;
                        assertTrue(link.getHref().endsWith("/dggs/"+dggrsId));
                    }
                }
                assertTrue(dggrsLinkFound);
            }
        }

    }

}
