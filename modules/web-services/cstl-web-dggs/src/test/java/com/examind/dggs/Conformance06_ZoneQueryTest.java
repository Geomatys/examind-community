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
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.dggs.DggrsZonesResponse;
import static org.junit.Assert.*;
import org.junit.Test;
import org.opengis.geometry.Envelope;

/**
 * Requirements class 6: Requirements Class Zone Query
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_zone-query
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance06_ZoneQueryTest extends DGGSAbstractTest {

    // ////////////////////////////////////////////////////////////////////////
    // 12.2.  Listing zones (…​/dggs/{dggrsId}/zones)
    // A.6.1.  Abstract Test for Requirement Listing zones
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_zones-list
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_zones-list
     */
    @Test
    public void requirement11() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        final DggrsZonesResponse dto = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones"),
                    "application/json", DggrsZonesResponse.class);
        /*
        - assert that the content of the response is a list of zones fully covering where data is available
          (in the case where the resource is associated with a particular dataset), and matching any additional query
          parameters specified by the client (e.g., a filtering query parameter), without any redundancy.
        - assert that unless the zones are a compact list of zones (see compact-zones query parameter),
          the zones returned all are of the same DGGRS hierarchy level.
        - assert that the selection of an encoding for the returned list of zones is consistent with HTTP content negotiation.
        - assert that the Implementation supports at minimum a JSON encoding (media type application/json).
        */
        assertEquals(
            """
             {
               "zones" : [ "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b" ],
               "returnedAreaMetersSquare" : 5.0949534678428306E14,
               "links" : [ {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=html",
                 "rel" : "alternate",
                 "type" : "text/html",
                 "title" : "Html"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=json",
                 "rel" : "alternate",
                 "type" : "application/json",
                 "title" : "Json"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=yaml",
                 "rel" : "alternate",
                 "type" : "application/yaml",
                 "title" : "Yaml"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=xml",
                 "rel" : "alternate",
                 "type" : "application/xml",
                 "title" : "Xml"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/",
                 "rel" : "https://www.opengis.net/def/rel/ogc/1.0/dataset",
                 "title" : "Root OGC Web API"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix",
                 "rel" : "https://www.opengis.net/def/rel/ogc/1.0/dggrs",
                 "title" : "Healpix DGGRS"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/definition",
                 "rel" : "https://www.opengis.net/def/rel/ogc/1.0/dggrs-definition",
                 "title" : "Healpix DGGRS definition"
               } ],
               "_linkZoneDepth" : 6,
               "_previewZoneDepth" : 8
             }""", dto.toString());
    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.3.  JSON zone list encoding
    // A.6.2.  Abstract Test for Requirement JSON zone list encoding
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_json-response
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_json-response
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-query_zone-total-area
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-json_additional-links
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-json_zone-order
     */
    @Test
    public void requirement12() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        final DggrsZonesResponse dto = sendRequestAndParse(
                    new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones"),
                    "application/json", DggrsZonesResponse.class);
        /*
        - assert that the schema for the JSON document follows the JSON Schema for DGGS Zone Query described in Requirement 12:
          /req/zone-query/json-response, where the zone identifiers are strings within a zones array property within a JSON object.
        - assert that the links property includes an [ogc-rel:dggrs] link to the Discrete Global Grid Reference System description resource.
        - assert that the links property includes an [ogc-rel:dggrs-definition] link to the DGGRS definition,
          using the schema defined in Annex B — DGGRS Definitions or a later version.
        */
        assertEquals(
            """
             {
               "zones" : [ "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b" ],
               "returnedAreaMetersSquare" : 5.0949534678428306E14,
               "links" : [ {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=html",
                 "rel" : "alternate",
                 "type" : "text/html",
                 "title" : "Html"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=json",
                 "rel" : "alternate",
                 "type" : "application/json",
                 "title" : "Json"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=yaml",
                 "rel" : "alternate",
                 "type" : "application/yaml",
                 "title" : "Yaml"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/zones?f=xml",
                 "rel" : "alternate",
                 "type" : "application/xml",
                 "title" : "Xml"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/",
                 "rel" : "https://www.opengis.net/def/rel/ogc/1.0/dataset",
                 "title" : "Root OGC Web API"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix",
                 "rel" : "https://www.opengis.net/def/rel/ogc/1.0/dggrs",
                 "title" : "Healpix DGGRS"
               }, {
                 "href" : "http://localhost:9090/WS/dggs/default/dggs/Healpix/definition",
                 "rel" : "https://www.opengis.net/def/rel/ogc/1.0/dggrs-definition",
                 "title" : "Healpix DGGRS definition"
               } ],
               "_linkZoneDepth" : 6,
               "_previewZoneDepth" : 8
             }""", dto.toString());
    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.4.  zone-level query parameter
    // A.6.3.  Abstract Test for Requirement zone-level query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_zone-level
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_zone-level
     */
    @Test
    public void requirement13() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        { // check with compact-zones = false
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones?zone-level=1&compact-zones=false"),
                        "application/json", DggrsZonesResponse.class);
            /*
            - assert that if a compact zones list is returned (which is the default, unless the compact-zones parameter is set to false),
              the zones returned in the response are of the DGGRS hierarchy level specified by the zone-level query parameter,
              or of a lower hierarchy level standing in for a compact representation of multiple zones at the requested hierarchy level.
            - assert that if a non-compact zones list is returned (if the compact-zones query parameter is set to false),
              the zones returned in the response are of the DGGRS hierarchy level specified by the zone-level query parameter
            */
            assertTrue(dto.getZones().equals(List.of(
                    "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "4a", "4b", "4c", "4d", "4e", "4f", "50",
                    "51", "52", "53", "54", "55", "56", "57", "58", "59", "5a", "5b", "5c", "5d", "5e", "5f", "60", "61",
                    "62", "63", "64", "65", "66", "67", "68", "69", "6a", "6b", "6c", "6d", "6e", "6f")));

        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.5.  compact-zones query parameter
    // A.6.4.  Abstract Test for Requirement compact-zones query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_compact-zones
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_compact-zones
     */
    @Test
    public void requirement14() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        /*
        - assert that the Implementation supports a Boolean compact-zones query parameter for the zone query operation
          (resource path ending with …​/dggs/{dggrsId}/zones), where a value of true corresponds to the default behavior
          when the query parameter is not specified, and a value of false disables the use of compact-zones in the response.
        - assert that when the compact-zones query parameter is set to false, the zones list response is not a compact
          list, and explicitly lists every individual zone at the requested or default DGGRS hierarchy level.
        - assert that when the compact-zones query parameter is set to true (or unspecified), the zones list response
          is a compact list, where children zones completely covering the area of a parent zone are replaced by that
          parent zone, in a recursive manner all the way to the lowest DGGRS hierarchy level.
        */

        { // check with compact-zones = false
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones?zone-level=2&compact-zones=false&bbox=10,10,140,80"),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of(
                    "101", "102", "103", "104", "105", "106", "107", "108", "109", "10a", "10b", "10c", "10d", "10e",
                    "10f", "110", "111", "112", "113", "116", "118", "119", "11a", "11b", "11c", "11d", "11e", "11f",
                    "147", "14c", "14d", "14f", "157", "15d", "15e", "15f")));
        }

        { // check with compact-zones = true
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones?zone-level=2&compact-zones=true&bbox=10,10,140,80"),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of(
                   "41", "42", "43", "44", "46", "47", "101", "102", "103", "116", "147", "14c", "14d", "14f", "157", "15d", "15e", "15f")));
        }


    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.6.  parent-zone query parameter for hierarchical exploration
    // A.6.5.  Abstract Test for Requirement parent-zone query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_parent-zone
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_parent-zone
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-query_zone-order
     */
    @Test
    public void requirement15() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        { // check simple sub-zone
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones?zone-level=1&parent-zone=15&compact-zones=false"),
                        "application/json", DggrsZonesResponse.class);
            /*
            - assert that the Implementation supports a parent-zone query parameter accepting a textual zone identifier.
            - assert that when specified, the response does not contain zones which are not this parent zone itself or a sub-zone of that zone.
            */
            assertTrue(dto.getZones().equals(List.of("54", "55", "56", "57")));
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.7.  limit query parameter for paging (recommendation)
    // ////////////////////////////////////////////////////////////////////////

    /*
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-query_limit
     */
    // no tests

    // ////////////////////////////////////////////////////////////////////////
    // 12.8.  bbox query parameter
    // A.6.6.  Abstract Test for Requirement bbox query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_bbox
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_bbox
     */
    @Test
    public void requirement16() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        { // check simple bbox
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones?zone-level=3&bbox=-45,10,-30,25&compact-zones=false"),
                        "application/json", DggrsZonesResponse.class);
            /*
            - assert that the Implementation supports a bbox query parameter for the zone query operation
              (resource path ending with …​/dggs/{dggrsId}/zones) as defined in Requirement 16: /req/zone-query/bbox
            - assert that the bbox query parameter is interpreted as a comma separated list of four or six floating point numbers,
              that if the bounding box consists of six numbers, the first three numbers are interpreted as the coordinates
              of the lower bound corner of a three-dimensional bounding box and the last three are interpreted as the coordinates of the upper bound corner.
            - assert that the axis order is determined by the bbox-crs query parameter value or longitude and latitude
              if the query parameter is omitted (https://www.opengis.net/def/crs/OGC/1.3/CRS84 axis order for a
              2D bounding box, https://www.opengis.net/def/crs/OGC/1.3/CRS84h for a 3D bounding box).
            - assert that the returned list of zone IDs only contain zones inside or intersecting with the spatial
              extent of the geographical area of the bounding box.
            */
            assertTrue(dto.getZones().equals(List.of("4c1", "4c3", "4c4", "4c5", "4c6", "4c7", "4cc", "4cd", "52e")));
        }

        { // check point query
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/dggs/"+dggrsId+"/zones?zone-level=3&bbox=-45,10,-45,10&compact-zones=false"),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("4c3")));
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.9.  bbox-crs query parameter
    // A.6.7.  Abstract Test for Requirement bbox-crs query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_bbox-crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_bbox-crs
     */
    @Test
    public void requirement17() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";

        { // check reprojected box
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, -45, -30);
            env.setRange(1, 10, 25);
            final Envelope penv = Envelopes.transform(env, CRS.forCode("EPSG:3857"));

            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones?zone-level=3&bbox="
                                + Double.toString(penv.getMinimum(0)) + ","
                                + Double.toString(penv.getMinimum(1)) + ","
                                + Double.toString(penv.getMaximum(0)) + ","
                                + Double.toString(penv.getMaximum(1)) + "&bbox-crs=EPSG:3857&compact-zones=false"),
                        "application/json", DggrsZonesResponse.class);
            /*
            - assert that the list of zones resource supports a bbox-crs query parameter specifying the CRS used for the bbox query parameter.
            - assert that for Earth centric data, the Implementation supports https://www.opengis.net/def/crs/OGC/1.3/CRS84 as a value.
            - assert that if the bbox-crs is not indicated https://www.opengis.net/def/crs/OGC/1.3/CRS84 is assumed.
            - assert that the native CRS (storageCrs) is supported as a value.
            - assert that both CRS expressed as URIs and as safe CURIEs are supported.
            - assert that if the bbox query parameter is not used, the bbox-crs is ignored.
            */
            assertTrue(dto.getZones().equals(List.of("4c1", "4c3", "4c4", "4c5", "4c6", "4c7", "4cc", "4cd", "52e")));
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.10.  subset query parameter
    // A.6.8.  Abstract Test for Requirement subset query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_subset
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_subset
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_rec_zone-query_subset-crs-axis-names
     */
    @Test
    public void requirement18() throws Exception {
        initLayerList();

        final String dataId = "coverage4d";
        final String dggrsId = "Healpix";

        { //sanity check, no parameters
            final String parameters =  URLEncoder.encode("", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b")));
        }

        /*
        - assert that the Implementation supports as axis names Lat and Lon for geographic CRS and E and N for projected
          CRS, which are to be interpreted as the best matching spatial axis in the CRS definition.
        */
        {
            final String parameters = "subset="+ URLEncoder.encode("Lon(0:90)", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of( "10", "11", "14", "15", "18", "19" )));
        }
        {
            final String parameters = "subset="+ URLEncoder.encode("Lon(0:90),Lat(-90:-45)", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of( "60", "61", "62", "64", "66", "6d")));
        }

        /*
        - assert that if a third spatial dimension is supported (if the resource’s spatial extent bounding box is three
          dimensional), the Implementation also supports a h dimension (elevation above the ellipsoid in EPSG:4979 or CRS84h)
          for geographic CRS and z for projected CRS, which are to be interpreted as the vertical axis in the CRS definition.
        */
        {
            final String parameters = "subset="+ URLEncoder.encode("z(100:200)", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b")));
        }
        {
            //Z is not in data range
            final String parameters = "subset="+ URLEncoder.encode("z(-800:-700)", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of()));
        }
        /*
        - assert that the Implementation supports as axis names time for a temporal dataset.
        */
        {
            final String parameters = "subset="+ URLEncoder.encode("time(\"2000-06-10T11:00:00Z\" : \"2030-01-01T01:00:00Z\")", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b")));
        }
        {
            //time is out of range
            final String parameters = "subset="+ URLEncoder.encode("time(\"2060-06-10T11:00:00Z\" : \"2100-01-01T01:00:00Z\")", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of()));
        }
        /*
        - assert that the Implementation supports as axis names any additional dimension (beyond spatial and temporal)
          as described in the extent property of the collection or dataset description.
        */
        {
            //todo, no data with this case
        }

        /*
        - assert that the Implementation returns a 400 error status code if an axis name does not correspond to one of
          the axes of the Coordinate Reference System (CRS) of the data or an axis defined in the relevant extent property.
        */
        {
            //time is out of range
            final String parameters = "subset="+ URLEncoder.encode("abc(10:20)", StandardCharsets.UTF_8);
            final HttpResponse<byte[]> response = sendRequest(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json");
            assertTrue(response.statusCode() == 400);
        }
        /*
        - assert that for a CRS where an axis can wrap around, such as subsetting across the dateline (anti-meridian)
          in a geographic CRS, a low value greater than high is supported to indicate an extent crossing that wrapping point.
        */
        {
            //todo bug : https://projects.geomatys.com/projects/ogc-2025/work_packages/3951
//            final String parameters = "subset="+ URLEncoder.encode("Lon(160:-40)", StandardCharsets.UTF_8);
//            final DggrsZonesResponse dto = sendRequestAndParse(
//                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
//                        "application/json", DggrsZonesResponse.class);
//            assertTrue(dto.getZones().equals(List.of("10", "14", "15", "18")));
        }
        /*
        - assert that the Implementation interprets the coordinates as values for the named axis of the CRS specified
          in the subset-crs query parameter value or in https://www.opengis.net/def/crs/OGC/1.3/CRS84
          (https://www.opengis.net/def/crs/OGC/1.3/CRS84h for vertical dimension) if the subset-crs query parameter is missing.
        */
        {
            //todo, we do not have vertical transformation
        }
        /*
        - assert that if the subset query parameter including any of the dimensions corresponding to those of the map
          bounding box is used with a bbox, the server returns a 400 client error.
        */
        {
            final String parameters = "bbox=10,10,140,80&subset="+ URLEncoder.encode("Lon(0:90)", StandardCharsets.UTF_8);
            final HttpResponse<byte[]> response = sendRequest(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json");
            assertEquals(400, response.statusCode());
        }
        /*
        - assert that the Implementation interprets multiple subset query parameters, as if all dimension subsetting
          values were provided in a single subset query parameter (comma separated).
        */
        {
            final String parameters = "subset="+ URLEncoder.encode("Lon(0:90)", StandardCharsets.UTF_8)
                                    + "&subset="+ URLEncoder.encode("Lat(-90:-45)", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of( "60", "61", "62", "64", "66", "6d")));
        }

    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.11.  subset-crs query parameter
    // A.6.9.  Abstract Test for Requirement subset-crs query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_subset-crs
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_subset-crs
     */
    @Test
    public void requirement19() throws Exception {
        initLayerList();

        final String dataId = "coverage4d";
        final String dggrsId = "Healpix";

        /*
        - assert that the zone listing operation supports a query parameter subset-crs identifying the CRS in which
          the subset query parameter is specified with a URI or safe CURIE.
        - assert that for Earth centric data, https://www.opengis.net/def/crs/OGC/1.3/CRS84 as a value is supported.
        - assert that if the subset-crs is not indicated, https://www.opengis.net/def/crs/OGC/1.3/CRS84 is assumed.
        - assert that the native CRS (storageCrs) are supported as a value. Other requirements classes may allow
          additional values (see crs query parameter definition).
        - assert that CRSs expressed both as URIs or as safe CURIEs are supported.
        - assert that if no subset query parameter referring to an axis of the CRS is used, the subset-crs is ignored.
        */
        {
            final GeneralEnvelope env = new GeneralEnvelope(CommonCRS.WGS84.normalizedGeographic());
            env.setRange(0, 0, 80);
            env.setRange(1, -80, -45);
            final Envelope penv = Envelopes.transform(env, CRS.forCode("EPSG:3857"));
            final double minlon = penv.getMinimum(0);
            final double maxlon = penv.getMaximum(0);
            final double minlat = penv.getMinimum(1);
            final double maxlat = penv.getMaximum(1);

            final String parameters = "subset-crs=EPSG:3857&subset="+ URLEncoder.encode("Lon(" + minlon + ":" +  maxlon + "),Lat(" + minlat + ":" + maxlat + ")", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of( "60", "61", "62", "6c", "6d")));
        }

    }

    // ////////////////////////////////////////////////////////////////////////
    // 12.12.  datetime query parameter
    // A.6.10.  Abstract Test for Requirement datetime query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_zone-query_datetime
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_zone-query_datetime
     */
    @Test
    public void requirement20() throws Exception {
        initLayerList();

        final String dataId = "coverage4d";
        final String dggrsId = "Healpix";

        /*
        - assert that the Implementation supports a datetime query parameter expressed corresponding to either a date-time
          instant or a time interval, conforming to the ABNF in Requirement 20: /req/zone-query/datetime.
        - assert that the implementation supports an instant defined as specified by RFC 3339, 5.6, with the exception
          that the server is only required to support the Z UTC time notation, and not required to support local time offsets.
        - assert that only the zones with data whose geometry intersect with the specified temporal interval are part of the zone list response.
        - assert that time intervals unbounded at the start or at the end are supported using a double-dot (..)
          or an empty string for the start/end.
        - assert that if a datetime query parameter is specified requesting zone data where no temporal dimension applies,
          the Implementation either ignores the query parameter or returns a 4xx client error.
        */
        {
            final String parameters = "datetime="+ URLEncoder.encode("2000-06-10T11:00:00Z / 2030-01-01T01:00:00Z", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of("10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "1a", "1b")));
        }
        {
            //time is out of range
            final String parameters = "datetime="+ URLEncoder.encode("2100-06-10T11:00:00Z / ..", StandardCharsets.UTF_8);
            final DggrsZonesResponse dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones?" + parameters),
                        "application/json", DggrsZonesResponse.class);
            assertTrue(dto.getZones().equals(List.of()));
        }
    }

}
