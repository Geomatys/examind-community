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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.dggs.DggrsZonesResponse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Requirements class 7: Requirements Class Filtering Zone Queries with CQL2
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_zone-query-cql2-filter
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query-cql2-filter
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance07_ZoneQueryWithCQL2lTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 13.2.  filter query parameter
    // A.7.1.  Abstract Test for Requirement filter query parameter (for zone queries)
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_filter
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query-cql2-filter_filter
     */
    @Test
    public void requirement21() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        { //base result
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones"),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b")));
        }

        /*
        - assert that the Implementation supports a filter query parameter specified using the CQL2-Text encoding of
          the OGC Common Query Language for the zone query operation (resource path ending with …​/dggs/{dggrsId}/zones).
        - assert that the list of returned zones are only those for which the CQL2 expression evaluates to true when
          considering the geometry and the data of the DGGS zones resource being queried.
        - assert that the CQL2 expression evaluator supports the queryables declared in the JSON Schema resource linked
          to from the origin of the DGGRS resources using the [ogc-rel:queryables] link relation type.
        */
        {
            final String filter = URLEncoder.encode("band2 >= 90", StandardCharsets.UTF_8);
            final String parameters = "filter="+ filter;
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("10", "11", "12", "13", "14", "15", "16", "17")));
        }
    }

}
