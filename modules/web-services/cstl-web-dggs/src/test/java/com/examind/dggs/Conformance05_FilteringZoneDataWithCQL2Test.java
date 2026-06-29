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
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.dggs.DggrsData;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Requirements class 5: Requirements Class Filtering Zone Data with CQL2
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_data-cql2-filter
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-cql2-filter
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance05_FilteringZoneDataWithCQL2Test extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 11.2.  filter query parameter
    // A.5.1.  Abstract Test for Requirement filter query parameter (for zone data)
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-cql2-filter_filter
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-cql2-filter_filter
     */
    @Test
    public void requirement10() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";


        { //check getting the data, no filter
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=2"),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 2 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "band1" : {
                         "title" : "band1",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       },
                       "band2" : {
                         "title" : "band2",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 2
                       }
                     }
                   },
                   "values" : {
                     "band1" : [ {
                       "depth" : 2,
                       "shape" : {
                         "count" : 16,
                         "subZones" : 16
                       },
                       "data" : [ 180.0, 191.0, 168.0, 180.0, 202.0, 213.0, 191.0, 202.0, 157.0, 168.0, 146.0, 157.0, 180.0, 191.0, 168.0, 180.0 ]
                     } ],
                     "band2" : [ {
                       "depth" : 2,
                       "shape" : {
                         "count" : 16,
                         "subZones" : 16
                       },
                       "data" : [ 60.0, 70.0, 70.0, 80.0, 80.0, 90.0, 90.0, 99.0, 80.0, 90.0, 90.0, 99.0, 99.0, 109.0, 109.0, 120.0 ]
                     } ]
                   }
                 }""", dto.toString());
        }

        { //check getting the data, filter on attribute band2 >= 90
            final String filter = URLEncoder.encode("band2 >= 90", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=2&filter=" + filter),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 2 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "band1" : {
                         "title" : "band1",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       },
                       "band2" : {
                         "title" : "band2",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 2
                       }
                     }
                   },
                   "values" : {
                     "band1" : [ {
                       "depth" : 2,
                       "shape" : {
                         "count" : 16,
                         "subZones" : 16
                       },
                       "data" : [ null, null, null, null, null, 213.0, 191.0, 202.0, null, 168.0, 146.0, 157.0, 180.0, 191.0, 168.0, 180.0 ]
                     } ],
                     "band2" : [ {
                       "depth" : 2,
                       "shape" : {
                         "count" : 16,
                         "subZones" : 16
                       },
                       "data" : [ null, null, null, null, null, 90.0, 90.0, 99.0, null, 90.0, 90.0, 99.0, 99.0, 109.0, 109.0, 120.0 ]
                     } ]
                   }
                 }""", dto.toString());
        }

    }

}
