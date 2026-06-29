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
import org.geotoolkit.ogcapi.dto.dggs.DggrsData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Requirements class 4: Requirements Class Data Custom Depths
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_data-custom-depths
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-custom-depths
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance04_DataCustomDepthsTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 10.2.  zone-depth query parameter
    // A.4.1.  Abstract Test for Requirement zone-depth query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-custom-depths_zone-depth
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-custom-depths_zone-depth
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-custom-depths_beyond-max-depth
     */
    @Test
    public void requirement9() throws Exception {
        initLayerList();

        final String dataId = "coveragetiff";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data at a single depth
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1"),
                        "application/json", DggrsData.class);
            final int expecteDepth = 1;
            assertNotNull(dto);

            assertTrue(zoneId.equals(dto.getZoneId()));

            assertEquals(1, dto.getDepths().size());
            assertEquals(expecteDepth, dto.getDepths().get(0).intValue());
            assertEquals(3, dto.getSchema().getProperties().size());
            assertEquals(3, dto.getValues().size());
            assertEquals(expecteDepth, dto.getValues().get("Red").get(0).getDepth().intValue());
            assertEquals(4, dto.getValues().get("Red").get(0).getData().size());
        }

        { //check getting the data at a list of depth
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=4,1,3"),
                        "application/json", DggrsData.class);
            assertNotNull(dto);

            assertTrue(zoneId.equals(dto.getZoneId()));
            assertEquals(3, dto.getSchema().getProperties().size());
            assertEquals(3, dto.getValues().size());
            assertEquals(3, dto.getDepths().size());

            {//depth 4
                assertEquals(4, dto.getDepths().get(0).intValue());
                assertEquals(4, dto.getValues().get("Red").get(0).getDepth().intValue());
                assertEquals(4*4*4*4, dto.getValues().get("Red").get(0).getData().size());
            }

            {//depth 1
                assertEquals(1, dto.getDepths().get(1).intValue());
                assertEquals(1, dto.getValues().get("Red").get(1).getDepth().intValue());
                assertEquals(4, dto.getValues().get("Red").get(1).getData().size());
            }

            {//depth 3
                assertEquals(3, dto.getDepths().get(2).intValue());
                assertEquals(3, dto.getValues().get("Red").get(2).getDepth().intValue());
                assertEquals(4*4*4, dto.getValues().get("Red").get(2).getData().size());
            }
        }

        { //check getting the data from a range of depth
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=2-4"),
                        "application/json", DggrsData.class);
            assertNotNull(dto);

            assertTrue(zoneId.equals(dto.getZoneId()));
            assertEquals(3, dto.getSchema().getProperties().size());
            assertEquals(3, dto.getValues().size());
            assertEquals(3, dto.getDepths().size());


            {//depth 2
                assertEquals(2, dto.getDepths().get(0).intValue());
                assertEquals(2, dto.getValues().get("Red").get(0).getDepth().intValue());
                assertEquals(4*4, dto.getValues().get("Red").get(0).getData().size());
            }

            {//depth 3
                assertEquals(3, dto.getDepths().get(1).intValue());
                assertEquals(3, dto.getValues().get("Red").get(1).getDepth().intValue());
                assertEquals(4*4*4, dto.getValues().get("Red").get(1).getData().size());
            }

            {//depth 4
                assertEquals(4, dto.getDepths().get(2).intValue());
                assertEquals(4, dto.getValues().get("Red").get(2).getDepth().intValue());
                assertEquals(4*4*4*4, dto.getValues().get("Red").get(2).getData().size());
            }
        }
    }

}
