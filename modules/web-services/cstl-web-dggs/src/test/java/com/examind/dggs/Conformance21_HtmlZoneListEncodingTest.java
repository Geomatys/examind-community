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
import org.geotoolkit.ogcapi.dto.LinkRelations;
import org.geotoolkit.ogcapi.dto.common.Link;
import org.geotoolkit.ogcapi.dto.dggs.DggrsZonesResponse;
import org.geotoolkit.ogcapi.dto.dggs.MediaTypes;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Requirements class 21: Requirements Class HTML Zone List
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-zone_html
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-html
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance21_HtmlZoneListEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 17.2.2.  HTML Zone List
    // A.21.1.  Abstract Test for Requirement HTML zone list encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-html_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-html_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-html_zone-information
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-html_zone-data
     */
    @Test
    public void requirement35() throws Exception {
        initLayerList();

        final String dataId = "coveragetiff";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        {//test zone list from dggrs
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId);
            final DggrsZonesResponse dto = sendRequestAndParse(uri, "application/json", DggrsZonesResponse.class);

            boolean queryLinkFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.OGC_DGGRS_ZONE_QUERY.equals(link.getRel())) {
                    queryLinkFound = true;
                    assertTrue(link.getHref().endsWith("/dggs/"+dggrsId+"/zones"));
                }
            }
            assertTrue(queryLinkFound);
        }

        {//test zone list for dggrs
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId+"/zones");
            final String dto = sendRequestAndParse(uri, MediaTypes.ZONELIST_HTML, String.class);
            assertTrue(dto.startsWith("<!DOCTYPE html>\n<html>"));
            assertTrue(dto.contains(zoneId));
        }

        {//test zone list from collection dggrs
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/" + dataId + "/dggs/"+dggrsId);
            final DggrsZonesResponse dto = sendRequestAndParse(uri, "application/json", DggrsZonesResponse.class);

            boolean queryLinkFound = false;
            for (Link link : dto.getLinks()) {
                assertTrue(link.getHref() != null && !link.getHref().isBlank());
                if (LinkRelations.OGC_DGGRS_ZONE_QUERY.equals(link.getRel())) {
                    queryLinkFound = true;
                    assertTrue(link.getHref().endsWith("collections/" + dataId + "/dggs/"+dggrsId+"/zones"));
                }
            }
            assertTrue(queryLinkFound);
        }

        {//test zone list for collection dggrs
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/" + dataId + "/dggs/"+dggrsId+"/zones");
            final String dto = sendRequestAndParse(uri, MediaTypes.ZONELIST_HTML, String.class);
            assertTrue(dto.startsWith("<!DOCTYPE html>\n<html>"));
            //assertTrue(!dto.contains(zoneId)); //ensure it does not contain all root zones but only more accurate zones
            assertTrue(dto.contains("\\x22zones\\x22:[\\x221322a\\x22,\\x224c8a3\\x22,\\x224c8ac\\x22,\\x224c8ae\\x22,\\x224ca00\\x22,\\x224ca01\\x22]"));
        }
    }

}
