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
import java.net.http.HttpResponse;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.dggs.DggrsData;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Requirements class 10: Requirements Class DGGS-JSON Zone Data
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-data_json
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-json
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance10_ZoneDataEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 16.2.2.  DGGS-JSON Zone Data
    // A.10.1.  Abstract Test for Requirement DGGS-JSON Zone data encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-json_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-json_content
     */
    @Test
    public void requirement24() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data at a single depth
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1"),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 1 ],
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
                       "depth" : 1,
                       "shape" : {
                         "count" : 4,
                         "subZones" : 4
                       },
                       "data" : [ 180.0, 202.0, 157.0, 180.0 ]
                     } ],
                     "band2" : [ {
                       "depth" : 1,
                       "shape" : {
                         "count" : 4,
                         "subZones" : 4
                       },
                       "data" : [ 70.0, 90.0, 90.0, 109.0 ]
                     } ]
                   }
                 }""", dto.toString());
        }


        { //check getting the data at a relative level exiding max subdepth

            // https://docs.ogc.org/DRAFTS/21-038r1.html#_per_core_beyond-max-refinement
            final HttpResponse<byte[]> response = sendRequest(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=9"),
                        "application/json");
            assertEquals(400, response.statusCode());
            final String text = new String(response.body());
            assertEquals("Bad request parameter : Relative depth greater then maximum relative depth : 8 reason/value(s) : []", text);
        }
    }

}
