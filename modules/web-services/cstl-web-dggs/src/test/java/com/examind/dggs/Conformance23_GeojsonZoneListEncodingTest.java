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
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Requirements class 23: Requirements Class GeoJSON Zone List
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-zone_geojson
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-geojson
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance23_GeojsonZoneListEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 17.4.2.  GeoJSON Zone List
    // A.23.1.  Abstract Test for Requirement GeoJSON zone list encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-geojson_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-geojson_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geojson_jsonfg-profile
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geojson_profile-links
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_per_zone-geojson_supported-rofiles
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geojson_id
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geojson_crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geojson_mid-points
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-geojson_geometry
     */
    @Test
    public void requirement37() throws Exception {
        initLayerList();

        final String dggrsId = "Healpix";

        { //check getting the zones as geojson for dggrs
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId+"/zones"),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"10","geometry":{"type":"Polygon","coordinates":[[[45.0,0.0],[90.0,41.810314895778596],[44.99999999999999,90.0],[0.0,41.810314895778596],[45.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"11","geometry":{"type":"Polygon","coordinates":[[[135.0,0.0],[180.0,41.810314895778596],[135.0,90.0],[90.0,41.810314895778596],[135.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"12","geometry":{"type":"Polygon","coordinates":[[[-135.0,0.0],[-90.0,41.810314895778596],[-135.0,90.0],[-180.0,41.810314895778596],[-135.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"13","geometry":{"type":"Polygon","coordinates":[[[-45.0,0.0],[0.0,41.810314895778596],[-44.99999999999999,90.0],[-90.0,41.810314895778596],[-45.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"14","geometry":{"type":"Polygon","coordinates":[[[0.0,-41.810314895778596],[45.0,0.0],[0.0,41.810314895778596],[-45.0,0.0],[0.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"15","geometry":{"type":"Polygon","coordinates":[[[90.0,-41.810314895778596],[135.0,0.0],[90.0,41.810314895778596],[45.0,0.0],[90.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"16","geometry":{"type":"Polygon","coordinates":[[[180.0,-41.810314895778596],[225.0,0.0],[180.0,41.810314895778596],[135.0,0.0],[180.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"17","geometry":{"type":"Polygon","coordinates":[[[-90.0,-41.810314895778596],[-45.0,0.0],[-90.0,41.810314895778596],[-135.0,0.0],[-90.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"18","geometry":{"type":"Polygon","coordinates":[[[44.99999999999999,-90.0],[90.0,-41.810314895778596],[45.0,0.0],[0.0,-41.810314895778596],[44.99999999999999,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"19","geometry":{"type":"Polygon","coordinates":[[[135.0,-90.0],[180.0,-41.810314895778596],[135.0,0.0],[90.0,-41.810314895778596],[135.0,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"1a","geometry":{"type":"Polygon","coordinates":[[[-135.0,-90.0],[-90.0,-41.810314895778596],[-135.0,0.0],[-180.0,-41.810314895778596],[-135.0,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"1b","geometry":{"type":"Polygon","coordinates":[[[-44.99999999999999,-90.0],[0.0,-41.810314895778596],[-45.0,0.0],[-90.0,-41.810314895778596],[-44.99999999999999,-90.0]]]},"properties":{}}
                ]}""".replace("\n", ""), dto.toString());
        }

        { //check getting the zones as geojson for 2d coverage
            final String dataId = "coverage2d";
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones"),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"10","geometry":{"type":"Polygon","coordinates":[[[45.0,0.0],[90.0,41.810314895778596],[44.99999999999999,90.0],[0.0,41.810314895778596],[45.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"11","geometry":{"type":"Polygon","coordinates":[[[135.0,0.0],[180.0,41.810314895778596],[135.0,90.0],[90.0,41.810314895778596],[135.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"12","geometry":{"type":"Polygon","coordinates":[[[-135.0,0.0],[-90.0,41.810314895778596],[-135.0,90.0],[-180.0,41.810314895778596],[-135.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"13","geometry":{"type":"Polygon","coordinates":[[[-45.0,0.0],[0.0,41.810314895778596],[-44.99999999999999,90.0],[-90.0,41.810314895778596],[-45.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"14","geometry":{"type":"Polygon","coordinates":[[[0.0,-41.810314895778596],[45.0,0.0],[0.0,41.810314895778596],[-45.0,0.0],[0.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"15","geometry":{"type":"Polygon","coordinates":[[[90.0,-41.810314895778596],[135.0,0.0],[90.0,41.810314895778596],[45.0,0.0],[90.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"16","geometry":{"type":"Polygon","coordinates":[[[180.0,-41.810314895778596],[225.0,0.0],[180.0,41.810314895778596],[135.0,0.0],[180.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"17","geometry":{"type":"Polygon","coordinates":[[[-90.0,-41.810314895778596],[-45.0,0.0],[-90.0,41.810314895778596],[-135.0,0.0],[-90.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"18","geometry":{"type":"Polygon","coordinates":[[[44.99999999999999,-90.0],[90.0,-41.810314895778596],[45.0,0.0],[0.0,-41.810314895778596],[44.99999999999999,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"19","geometry":{"type":"Polygon","coordinates":[[[135.0,-90.0],[180.0,-41.810314895778596],[135.0,0.0],[90.0,-41.810314895778596],[135.0,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"1a","geometry":{"type":"Polygon","coordinates":[[[-135.0,-90.0],[-90.0,-41.810314895778596],[-135.0,0.0],[-180.0,-41.810314895778596],[-135.0,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"1b","geometry":{"type":"Polygon","coordinates":[[[-44.99999999999999,-90.0],[0.0,-41.810314895778596],[-45.0,0.0],[-90.0,-41.810314895778596],[-44.99999999999999,-90.0]]]},"properties":{}}
                ]}""".replace("\n", ""), dto.toString());
        }

        { //check getting the zones as geojson for 4D coverage
            final String dataId = "coverage4d";
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones"),
                        "application/geo+json", String.class);

            //result should be the same as with 2D, temporal and elevation are not returned
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"10","geometry":{"type":"Polygon","coordinates":[[[45.0,0.0],[90.0,41.810314895778596],[44.99999999999999,90.0],[0.0,41.810314895778596],[45.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"11","geometry":{"type":"Polygon","coordinates":[[[135.0,0.0],[180.0,41.810314895778596],[135.0,90.0],[90.0,41.810314895778596],[135.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"12","geometry":{"type":"Polygon","coordinates":[[[-135.0,0.0],[-90.0,41.810314895778596],[-135.0,90.0],[-180.0,41.810314895778596],[-135.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"13","geometry":{"type":"Polygon","coordinates":[[[-45.0,0.0],[0.0,41.810314895778596],[-44.99999999999999,90.0],[-90.0,41.810314895778596],[-45.0,0.0]]]},"properties":{}},
                {"type":"Feature","id":"14","geometry":{"type":"Polygon","coordinates":[[[0.0,-41.810314895778596],[45.0,0.0],[0.0,41.810314895778596],[-45.0,0.0],[0.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"15","geometry":{"type":"Polygon","coordinates":[[[90.0,-41.810314895778596],[135.0,0.0],[90.0,41.810314895778596],[45.0,0.0],[90.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"16","geometry":{"type":"Polygon","coordinates":[[[180.0,-41.810314895778596],[225.0,0.0],[180.0,41.810314895778596],[135.0,0.0],[180.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"17","geometry":{"type":"Polygon","coordinates":[[[-90.0,-41.810314895778596],[-45.0,0.0],[-90.0,41.810314895778596],[-135.0,0.0],[-90.0,-41.810314895778596]]]},"properties":{}},
                {"type":"Feature","id":"18","geometry":{"type":"Polygon","coordinates":[[[44.99999999999999,-90.0],[90.0,-41.810314895778596],[45.0,0.0],[0.0,-41.810314895778596],[44.99999999999999,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"19","geometry":{"type":"Polygon","coordinates":[[[135.0,-90.0],[180.0,-41.810314895778596],[135.0,0.0],[90.0,-41.810314895778596],[135.0,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"1a","geometry":{"type":"Polygon","coordinates":[[[-135.0,-90.0],[-90.0,-41.810314895778596],[-135.0,0.0],[-180.0,-41.810314895778596],[-135.0,-90.0]]]},"properties":{}},
                {"type":"Feature","id":"1b","geometry":{"type":"Polygon","coordinates":[[[-44.99999999999999,-90.0],[0.0,-41.810314895778596],[-45.0,0.0],[-90.0,-41.810314895778596],[-44.99999999999999,-90.0]]]},"properties":{}}
                ]}""".replace("\n", ""), dto.toString());
        }

        //todo profiles
    }

}
