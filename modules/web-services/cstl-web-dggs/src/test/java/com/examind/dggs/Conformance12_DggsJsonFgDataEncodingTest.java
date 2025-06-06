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
import org.geotoolkit.ogcapi.client.dggs.DggsApi;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Requirements class 12: Requirements Class DGGS-JSON-FG Zone Data
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-data_dggs_fgjson
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-dggs-jsonfg
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance12_DggsJsonFgDataEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 16.4.2.  DGGS-JSON-FG Zone Data
    // A.12.1.  Abstract Test for Requirement DGGS-JSON-FG Zone data encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-dggs-jsonfg_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-dggs-jsonfg_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-dggs-jsonfg_profile-links
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-dggs-jsonfg_geometry
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_per_data-dggs-jsonfg_supported-profiles
     */
    @Test
    public void requirement26() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data as geosjon
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1&profile=" + DggsApi.PROFILE_JSONFG_DGGS),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"50","geometry":{"type":"Polygon","coordinates":[[[0.0,-41.810314895778596],[22.5,-19.47122063449069],[0.0,0.0],[-22.500000000000025,-19.47122063449069],[0.0,-41.810314895778596]]]},"properties":{"band1":180.0,"band2":70.0}}
                ,{"type":"Feature","id":"51","geometry":{"type":"Polygon","coordinates":[[[22.5,-19.47122063449069],[45.0,0.0],[22.5,19.47122063449069],[0.0,0.0],[22.5,-19.47122063449069]]]},"properties":{"band1":202.0,"band2":90.0}}
                ,{"type":"Feature","id":"52","geometry":{"type":"Polygon","coordinates":[[[-22.500000000000025,-19.47122063449069],[0.0,0.0],[-22.500000000000025,19.47122063449069],[-45.0,0.0],[-22.500000000000025,-19.47122063449069]]]},"properties":{"band1":157.0,"band2":90.0}}
                ,{"type":"Feature","id":"53","geometry":{"type":"Polygon","coordinates":[[[0.0,0.0],[22.5,19.47122063449069],[0.0,41.810314895778596],[-22.500000000000025,19.47122063449069],[0.0,0.0]]]},"properties":{"band1":180.0,"band2":109.0}}
                ]}""".replace("\n", ""), dto.toString().replace("\n", ""));
        }
    }

}
