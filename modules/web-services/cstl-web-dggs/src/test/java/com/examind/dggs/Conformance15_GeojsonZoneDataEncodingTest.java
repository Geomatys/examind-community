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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Requirements class 15: Requirements Class GeoJSON
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc_table-data_geojson
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-geojson
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance15_GeojsonZoneDataEncodingTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 16.7.2.  GeoJSON Zone Data
    // A.15.1.  Abstract Test for Requirement GeoJSON Zone data encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-geojson_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-geojson_content
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_jsonfg-profile
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_profile-links
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_per_data-geojson_supported-profiles
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_clipping
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_generalization
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_omission
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_geometry
     */
    @Test
    public void requirement29() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check getting the data as geosjon
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1"),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"50","geometry":{"type":"Polygon","coordinates":[[[0.0,-41.810314895778596],[22.5,-19.47122063449069],[0.0,0.0],[-22.500000000000025,-19.47122063449069],[0.0,-41.810314895778596]]]},"properties":{"band1":180.0,"band2":70.0}},
                {"type":"Feature","id":"51","geometry":{"type":"Polygon","coordinates":[[[22.5,-19.47122063449069],[45.0,0.0],[22.5,19.47122063449069],[0.0,0.0],[22.5,-19.47122063449069]]]},"properties":{"band1":202.0,"band2":90.0}},
                {"type":"Feature","id":"52","geometry":{"type":"Polygon","coordinates":[[[-22.500000000000025,-19.47122063449069],[0.0,0.0],[-22.500000000000025,19.47122063449069],[-45.0,0.0],[-22.500000000000025,-19.47122063449069]]]},"properties":{"band1":157.0,"band2":90.0}},
                {"type":"Feature","id":"53","geometry":{"type":"Polygon","coordinates":[[[0.0,0.0],[22.5,19.47122063449069],[0.0,41.810314895778596],[-22.500000000000025,19.47122063449069],[0.0,0.0]]]},"properties":{"band1":180.0,"band2":109.0}}
                ]}""".replace("\n", ""), dto.toString());
        }

        //todo profiles
    }

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_data-geojson_geometry
     */
    @Ignore //TODO more work to be done before full feature support
    @Test
    public void recommendation22() throws Exception {
        initLayerList();

        final String dataId = "city";
        final String dggrsId = "Healpix";
        final String zoneId = "10aed664f";

        { //getting data as geojson with zone
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1&geometry=zone-region"),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"42bb5993c","geometry":{"type":"Polygon","coordinates":[[[2.7925647331115293,49.69630732879481],[2.792665726375177,49.69782808936244],[2.789511754068718,49.69934884252405],[2.789410871216231,49.69782808936244],[2.7925647331115293,49.69630732879481]]]},"properties":{"osm_id":43780199,"name":"","type":""}},
                {"type":"Feature","id":"42bb5993d","geometry":{"type":"Polygon","coordinates":[[[2.792665726375177,49.69782808936244],[2.79276672694394,49.69934884252405],[2.7896126442185962,49.70086958828001],[2.789511754068718,49.69934884252405],[2.792665726375177,49.69782808936244]]]},"properties":{"osm_id":43779886,"name":"","type":""}},
                {"type":"Feature","id":"42bb5993e","geometry":{"type":"Polygon","coordinates":[[[2.789410871216231,49.69782808936244],[2.789511754068718,49.69934884252405],[2.7863575536185747,49.700869588280014],[2.78625678119349,49.69934884252405],[2.789410871216231,49.69782808936244]]]},"properties":{"osm_id":43783673,"name":"","type":""}},
                {"type":"Feature","id":"42bb5993f","geometry":{"type":"Polygon","coordinates":[[[2.789511754068718,49.69934884252405],[2.7896126442185962,49.70086958828001],[2.786458333333333,49.7023903266307],[2.7863575536185747,49.700869588280014],[2.789511754068718,49.69934884252405]]]},"properties":{"osm_id":43767387,"name":"","type":""}}
                ]}""".replace("\n", ""), dto.toString());
        }

        { //getting data as geojson with centroid
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=1&geometry=zone-centroid"),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"42bb5993c","geometry":{"type":"Point","coordinates":[2.791038281632832,49.697828086893765]},"properties":{"osm_id":43780199,"name":"","type":""}},
                {"type":"Feature","id":"42bb5993d","geometry":{"type":"Point","coordinates":[2.791139223342159,49.6993488400555]},"properties":{"osm_id":43779886,"name":"","type":""}},
                {"type":"Feature","id":"42bb5993e","geometry":{"type":"Point","coordinates":[2.787884250465609,49.6993488400555]},"properties":{"osm_id":43783673,"name":"","type":""}},
                {"type":"Feature","id":"42bb5993f","geometry":{"type":"Point","coordinates":[2.7879850817517875,49.70086958581159]},"properties":{"osm_id":43767387,"name":"","type":""}}]}""".replace("\n", ""), dto.toString());
        }

        { //getting data as geojson with vectorized
            final String dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/42bb59937e/data?zone-depth=6&geometry=vectorized"),
                        "application/geo+json", String.class);
            assertEquals(
                """
                {
                "type":"FeatureCollection"
                ,"features":[
                {"type":"Feature","id":"city.247","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7929955,49.6981608],[2.7929117,49.6982719],[2.793427,49.6984373],[2.7934861,49.698361],[2.7934882,49.6983157],[2.7929955,49.6981608]]]]},"properties":{"osm_id":43626278,"name":"","type":""}}
                ,{"type":"Feature","id":"city.539","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7929675,49.6989361],[2.7929531,49.6988548],[2.7928426,49.6988695],[2.7926962,49.6989024],[2.7926839,49.6988656],[2.792665,49.6988663],[2.7926387,49.6988347],[2.7926215,49.6988063],[2.79259,49.6987469],[2.7923204,49.6988558],[2.7921905,49.6989083],[2.792216,49.6989684],[2.7925375,49.6989564],[2.7927053,49.6989474],[2.7927327,49.6989472],[2.792867,49.6989463],[2.7929675,49.6989361]]]]},"properties":{"osm_id":43766810,"name":"","type":""}}
                ,{"type":"Feature","id":"city.541","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7926387,49.6988347],[2.7927083,49.6988172],[2.7927011,49.6988048],[2.792784,49.6987875],[2.7928101,49.6988503],[2.792928,49.69883],[2.7928909,49.6987498],[2.7926215,49.6988063],[2.7926387,49.6988347]]]]},"properties":{"osm_id":43766812,"name":"","type":""}}
                ,{"type":"Feature","id":"city.542","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7928909,49.6987498],[2.7929342,49.6987397],[2.7928643,49.6986356],[2.7928065,49.698671],[2.7927505,49.6986878],[2.7927339,49.6986912],[2.79259,49.6987469],[2.7926215,49.6988063],[2.7928909,49.6987498]]]]},"properties":{"osm_id":43766813,"name":"","type":""}}
                ,{"type":"Feature","id":"city.543","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7927505,49.6986878],[2.7928065,49.698671],[2.7927414,49.6986227],[2.7927003,49.6986464],[2.7927505,49.6986878]]]]},"properties":{"osm_id":43766814,"name":"","type":""}}
                ,{"type":"Feature","id":"city.544","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7928065,49.698671],[2.7928643,49.6986356],[2.7927973,49.6985884],[2.7927414,49.6986227],[2.7928065,49.698671]]]]},"properties":{"osm_id":43766815,"name":"","type":""}}
                ,{"type":"Feature","id":"city.545","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7927278,49.698582],[2.792736,49.6985876],[2.792766,49.6985733],[2.7927603,49.6985687],[2.7927278,49.698582]]]]},"properties":{"osm_id":43766816,"name":"","type":""}}
                ,{"type":"Feature","id":"city.547","geometry":{"type":"MultiPolygon","coordinates":[[[[2.7930307,49.6987622],[2.7930693,49.6987941],[2.7930963,49.6987816],[2.7930762,49.6987656],[2.7930985,49.6987552],[2.7931475,49.6987974],[2.7931054,49.6988188],[2.793173,49.6988714],[2.7933167,49.6987999],[2.7932428,49.6987463],[2.7932047,49.6987662],[2.7931452,49.6987198],[2.7931229,49.6987292],[2.7931069,49.6987174],[2.7930307,49.6987622]]]]},"properties":{"osm_id":43766818,"name":"","type":""}}
                ]}""", dto.toString());
        }
    }


}
