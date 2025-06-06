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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.model.LinkRelations;
import org.geotoolkit.ogcapi.model.common.Link;
import org.geotoolkit.ogcapi.model.dggs.DggrsZonesResponse;
import org.geotoolkit.ogcapi.model.dggs.MediaTypes;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Requirements class 22: Requirements Class 64-bit Binary Zone List
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-zone_binary64bit
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-uint64
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance22_Int64ZoneListEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 17.3.2.  64 bit Binary Zone List
    // A.22.1.  Abstract Test for Requirement Binary 64-bit integer zone list encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-uint64_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-uint64_content
     */
    @Test
    public void requirement36() throws Exception {
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
            final byte[] dto = sendRequestAndParse(uri, MediaTypes.ZONELIST_UINT64, byte[].class);
            assertEquals(13*8, dto.length);
            final long[] ids = new long[13];
            ByteBuffer.wrap(dto).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get(ids);
            assertEquals(13, ids.length);
            assertEquals(12l, ids[0]); //nb
            assertEquals(16l, ids[1]);
            assertEquals(27l, ids[12]);
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
            final byte[] dto = sendRequestAndParse(uri, MediaTypes.ZONELIST_UINT64, byte[].class);
            assertEquals(7*8, dto.length);
            final long[] ids = new long[7];
            ByteBuffer.wrap(dto).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().get(ids);
            assertEquals(7, ids.length);
            assertEquals(6l, ids[0]); //nb
            assertEquals(78378l, ids[1]);
            assertEquals(313507l, ids[2]);
            assertEquals(313516l, ids[3]);
            assertEquals(313518l, ids[4]);
            assertEquals(313856l, ids[5]);
            assertEquals(313857l, ids[6]);
        }
    }

}
