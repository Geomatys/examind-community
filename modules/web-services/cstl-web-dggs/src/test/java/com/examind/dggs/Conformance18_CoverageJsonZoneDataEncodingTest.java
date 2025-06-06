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
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Requirements class 18: Requirements Class CoverageJSON Data
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-data_coveragejson
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-coveragejson
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance18_CoverageJsonZoneDataEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 16.10.2.  CoverageJSON Zone Data
    // A.18.1.  Abstract Test for Requirement CoverageJSON Zone data encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-coveragejson_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-coveragejson_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-coveragejson_null-values
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-coveragejson_crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-coveragejson_profile
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-coveragejson_profile-links
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_per_data-coveragejson_supported-profiles
     */
    @Test
    public void requirement32() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data as geosjon
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1"),
                        "application/prs.coverage+json", String.class);

            System.out.println(dto);

            assertTrue(dto.toString().startsWith(
                """
                {
                    "type": "Coverage",
                    "domain": {
                        "type": "Domain",
                        "domainType": "Grid",
                        "axes": {
                            "x": {
                                "start": -44.6484375,
                                "stop": 44.6484375,
                                "num": 128
                            },
                            "y": {
                                "start": 41.48367181065532,
                                "stop": -41.48367181065532,
                                "num": 128
                            }
                        },
                        "referencing": [
                            {
                                "coordinates": [
                                    "x",
                                    "y"
                                ],
                                "system": {
                                    "type": "GeographicCRS",
                                    "id": "urn:ogc:def:crs:OGC:1.3:CRS84"
                                }
                            }
                        ]
                    },
                    "parameters": {
                        "band1": {
                            "type": "Parameter",
                            "id": "band1"
                        },
                        "band2": {
                            "type": "Parameter",
                            "id": "band2"
                        }
                    },
                    "ranges": {
                        "band1": {
                            "type": "NdArray",
                            "dataType": "float",
                            "axisNames": [
                                "y",
                                "x"
                            ],
                            "shape": [
                                128,
                                128
                            ],
                            "values": [
                """));
        }

        //todo profiles
    }

}
