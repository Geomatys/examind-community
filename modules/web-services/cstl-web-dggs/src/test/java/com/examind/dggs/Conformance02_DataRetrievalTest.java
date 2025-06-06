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
import org.geotoolkit.ogcapi.model.common.Link;
import org.geotoolkit.ogcapi.model.dggs.Dggrs;
import org.geotoolkit.ogcapi.model.dggs.DggrsData;
import org.geotoolkit.ogcapi.model.dggs.DggrsLinkTemplatesInner;
import org.geotoolkit.ogcapi.model.dggs.ZoneInfo;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Requirements class 2: Requirements Class Data Retrieval
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_data-retrieval
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-retrieval
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance02_DataRetrievalTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 8.2.  Retrieving data from a zone (../dggs/{dggrsId}/zones/{zoneId}/data)
    // A.2.1.  Abstract Test for Requirement Retrieving data from a zone
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-retrieval_zone-data
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#req_data-retrieval_zone-data
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rec_data-retrieval_crs
     */
    @Test
    public void requirement4() throws Exception {
        initLayerList();

        final String dataId = "coveragetiff";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        final int defaultDepth;
        { //check template link is in the dggs
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId);
            final Dggrs dto = sendRequestAndParse(uri, "application/json", Dggrs.class);

            boolean templateFound = false;
            for (DggrsLinkTemplatesInner template : dto.getLinkTemplates()) {
                if (LinkRelations.OGC_DGGRS_ZONE_DATA.equals(template.getRel())) {
                    templateFound = true;
                    assertTrue(template.getUriTemplate().contains("{zoneId}"));
                    assertTrue(template.getUriTemplate().endsWith("/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/{zoneId}/data"));
                }
            }
            assertTrue(templateFound);

            //check default depth
            defaultDepth = (int) dto.getDefaultDepth();
            assertEquals(4, defaultDepth);
        }

        { //check link is in the zone
            final URI uri = new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId);
            final ZoneInfo dto = sendRequestAndParse(uri, "application/json", ZoneInfo.class);

            boolean datalinkFound = false;
            for (Link link : dto.getLinks()) {
                if (LinkRelations.OGC_DGGRS_ZONE_DATA.equals(link.getRel())) {
                    datalinkFound = true;
                    assertTrue(link.getHref().endsWith("/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data"));
                }
            }
            assertTrue(datalinkFound);
        }

        { //check getting the data
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data"),
                        "application/json", DggrsData.class);
            assertNotNull(dto);

            assertTrue(zoneId.equals(dto.getZoneId()));

            assertEquals(1, dto.getDepths().size());
            assertEquals(defaultDepth, dto.getDepths().get(0).intValue());
            assertEquals(3, dto.getSchema().getProperties().size());
            assertEquals(3, dto.getValues().size());
            assertEquals(defaultDepth, dto.getValues().get("Red").get(0).getDepth().intValue());
            assertEquals(4*4*4*4, dto.getValues().get("Red").get(0).getData().size());

        }

    }

}
