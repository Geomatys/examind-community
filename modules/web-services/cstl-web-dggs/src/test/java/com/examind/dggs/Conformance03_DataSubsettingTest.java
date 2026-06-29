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
import java.time.OffsetDateTime;
import java.util.List;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import org.geotoolkit.ogcapi.dto.common.CollectionDescription;
import org.geotoolkit.ogcapi.dto.common.Extent;
import org.geotoolkit.ogcapi.dto.common.SpatialExtent;
import org.geotoolkit.ogcapi.dto.common.TemporalExtent;
import org.geotoolkit.ogcapi.dto.dggs.DggrsData;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Requirements class 3: Requirements Class Data Subsetting
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#rc-table_data-subsetting
 * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-subsetting
 *
 * @author Johann Sorel (Geomatys)
 */
public class Conformance03_DataSubsettingTest extends DGGSAbstractTest {

    /**
     * Additional test to check data envelope
     * @todo to move in collection conformance tests
     */
    @Test
    public void collectionItemExtent() throws Exception {
        initLayerList();

        final String dataId = "coverage4d";

        final CollectionDescription dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId),
                        "application/json", CollectionDescription.class);
        final Extent extent = dto.getExtent();
        final SpatialExtent spatial = extent.getSpatial();
        final TemporalExtent temporal = extent.getTemporal();
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84h", spatial.getCrs());
        assertEquals(1, spatial.getBbox().length);
        assertArrayEquals(new double[]{-180d, -90d, 100d, 180d, 90d, 400d}, spatial.getBbox()[0], 0);

        assertEquals("http://www.opengis.net/def/uom/ISO-8601/0/Gregorian", temporal.getTrs());
        assertEquals(1, temporal.getInterval().length);
        //data envelope is from 10 to 14 with 3 values at 10:20, 11:00, 11:40.
        assertArrayEquals(new OffsetDateTime[]{OffsetDateTime.parse("2000-06-10T10:20:00Z"),
                             OffsetDateTime.parse("2000-06-10T11:40:00Z")},
                temporal.getInterval()[0]);

    }

    // ////////////////////////////////////////////////////////////////////////
    // 9.2.  subset query parameter
    // A.3.1.  Abstract Test for Requirement subset query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-subsetting_subset
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-subsetting_subset
     */
    @Test
    public void requirement5() throws Exception {
        initLayerList();

        final String dataId = "coverage4d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //sanity check, no parameters

            //service returns only a slice by default, first Z, last date

            final String parameters =  URLEncoder.encode("", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0 ]
                     },
                     "interval" : [ 150.0, 150.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T11:40:00Z",
                       "coordinates" : [ "2000-06-10T11:40:00Z" ]
                     },
                     "interval" : [ "2000-06-10T11:40:00Z", "2000-06-10T11:40:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 256,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 1,
                           "time" : 1
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t2
            final double[] repeat = new double[]{  7 };
            assertEquals(256, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }

        { //check time subset as point
            final String parameters = "subset="+ URLEncoder.encode("time(\"2000-06-10T11:00:00Z\")", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0 ]
                     },
                     "interval" : [ 150.0, 150.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T11:00:00Z",
                       "coordinates" : [ "2000-06-10T11:00:00Z" ]
                     },
                     "interval" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:00:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 256,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 1,
                           "time" : 1
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t1
            final double[] repeat = new double[]{   4 };
            assertEquals(256, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }

        { //check time subset as interval
            final String parameters = "subset="+ URLEncoder.encode("time(\"2000-06-10T11:00:00Z\" : \"2030-01-01T01:00:00Z\")", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0 ]
                     },
                     "interval" : [ 150.0, 150.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 2,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T11:00:00Z",
                       "coordinates" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                     },
                     "interval" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 512,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 1,
                           "time" : 2
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t1 v0t2
            final double[] repeat = new double[]{   4,   7 };
            assertEquals(512, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }

        { //check h/z
            final String[] axisNames = new String[]{"h","z"};
            for (String axisName : axisNames) {
                { //check h/z subset as point
                    final String parameters = "subset="+ URLEncoder.encode(axisName+"(250)", StandardCharsets.UTF_8);
                    final DggrsData dto = sendRequestAndParse(
                                new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                                "application/json", DggrsData.class);
                    assertEquals(
                        """
                         {
                           "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                           "zoneId" : "14",
                           "depths" : [ 4 ],
                           "schema" : {
                             "type" : "object",
                             "properties" : {
                               "test" : {
                                 "title" : "test",
                                 "type" : "number",
                                 "minItems" : 0,
                                 "x-ogc-propertySeq" : 1
                               }
                             }
                           },
                           "dimensions" : [ {
                             "name" : "z",
                             "grid" : {
                               "cellsCount" : 1,
                               "resolution" : 100.0,
                               "firstCoordinate" : 250.0,
                               "coordinates" : [ 250.0 ]
                             },
                             "interval" : [ 250.0, 250.0 ]
                           }, {
                             "name" : "time",
                             "grid" : {
                               "cellsCount" : 1,
                               "resolution" : "PT40M",
                               "firstCoordinate" : "2000-06-10T11:40:00Z",
                               "coordinates" : [ "2000-06-10T11:40:00Z" ]
                             },
                             "interval" : [ "2000-06-10T11:40:00Z", "2000-06-10T11:40:00Z" ]
                           } ],
                           "values" : {
                             "test" : [ {
                               "depth" : 4,
                               "shape" : {
                                 "count" : 256,
                                 "subZones" : 256,
                                 "dimensions" : {
                                   "z" : 1,
                                   "time" : 1
                                 }
                               },
                               "data" : []
                             } ]
                           }
                         }""", noData(dto));

                    final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
                    //                                    v1t2
                    final double[] repeat = new double[]{  8 };
                    assertEquals(256, data.length);
                    for (int i = 0; i < data.length; i++) {
                        assertEquals(repeat[i % repeat.length], data[i], 0.0);
                    }
                }
                { //check h/z subset as interval
                    final String parameters = "subset="+ URLEncoder.encode(axisName+"(250 : 1000)", StandardCharsets.UTF_8);
                    final DggrsData dto = sendRequestAndParse(
                                new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                                "application/json", DggrsData.class);
                    assertEquals(
                        """
                         {
                           "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                           "zoneId" : "14",
                           "depths" : [ 4 ],
                           "schema" : {
                             "type" : "object",
                             "properties" : {
                               "test" : {
                                 "title" : "test",
                                 "type" : "number",
                                 "minItems" : 0,
                                 "x-ogc-propertySeq" : 1
                               }
                             }
                           },
                           "dimensions" : [ {
                             "name" : "z",
                             "grid" : {
                               "cellsCount" : 2,
                               "resolution" : 100.0,
                               "firstCoordinate" : 250.0,
                               "coordinates" : [ 250.0, 350.0 ]
                             },
                             "interval" : [ 250.0, 350.0 ]
                           }, {
                             "name" : "time",
                             "grid" : {
                               "cellsCount" : 1,
                               "resolution" : "PT40M",
                               "firstCoordinate" : "2000-06-10T11:40:00Z",
                               "coordinates" : [ "2000-06-10T11:40:00Z" ]
                             },
                             "interval" : [ "2000-06-10T11:40:00Z", "2000-06-10T11:40:00Z" ]
                           } ],
                           "values" : {
                             "test" : [ {
                               "depth" : 4,
                               "shape" : {
                                 "count" : 512,
                                 "subZones" : 256,
                                 "dimensions" : {
                                   "z" : 2,
                                   "time" : 1
                                 }
                               },
                               "data" : []
                             } ]
                           }
                         }""", noData(dto));

                    final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
                    //                                    v1t2 v2t2
                    final double[] repeat = new double[]{   8,   9};
                    assertEquals(512, data.length);
                    for (int i = 0; i < data.length; i++) {
                        assertEquals(repeat[i % repeat.length], data[i], 0.0);
                    }
                }
            }
        }


        { //check incorrect axis name
            final String parameters = "subset="+ URLEncoder.encode("wrong(\"2000-06-10T11:00:00Z\" : \"2030-01-01T01:00:00Z\")", StandardCharsets.UTF_8);
            final HttpResponse<byte[]> response = sendRequest(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json");
            assertEquals(400, response.statusCode());
        }

        { //check time + h subset as point.
            final String[] params = new String[]{
                //all parameters together
                "subset="+ URLEncoder.encode("time(\"2000-06-10T11:00:00Z\"), h(250.0)", StandardCharsets.UTF_8),
                //in different subset
                "subset="+ URLEncoder.encode("time(\"2000-06-10T11:00:00Z\")", StandardCharsets.UTF_8) + "&subset="+ URLEncoder.encode("h(250.0)", StandardCharsets.UTF_8)
            };
            for(String parameters : params) {
                final DggrsData dto = sendRequestAndParse(
                            new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                            "application/json", DggrsData.class);
                assertEquals(
                    """
                     {
                       "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                       "zoneId" : "14",
                       "depths" : [ 4 ],
                       "schema" : {
                         "type" : "object",
                         "properties" : {
                           "test" : {
                             "title" : "test",
                             "type" : "number",
                             "minItems" : 0,
                             "x-ogc-propertySeq" : 1
                           }
                         }
                       },
                       "dimensions" : [ {
                         "name" : "z",
                         "grid" : {
                           "cellsCount" : 1,
                           "resolution" : 100.0,
                           "firstCoordinate" : 250.0,
                           "coordinates" : [ 250.0 ]
                         },
                         "interval" : [ 250.0, 250.0 ]
                       }, {
                         "name" : "time",
                         "grid" : {
                           "cellsCount" : 1,
                           "resolution" : "PT40M",
                           "firstCoordinate" : "2000-06-10T11:00:00Z",
                           "coordinates" : [ "2000-06-10T11:00:00Z" ]
                         },
                         "interval" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:00:00Z" ]
                       } ],
                       "values" : {
                         "test" : [ {
                           "depth" : 4,
                           "shape" : {
                             "count" : 256,
                             "subZones" : 256,
                             "dimensions" : {
                               "z" : 1,
                               "time" : 1
                             }
                           },
                           "data" : []
                         } ]
                       }
                     }""", noData(dto));

                final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
                //                                    v1t1
                final double[] repeat = new double[]{   5};
                assertEquals(256, data.length);
                for (int i = 0; i < data.length; i++) {
                    assertEquals(repeat[i % repeat.length], data[i], 0.0);
                }
            }
        }

        { //get everything
            final String parameters = "subset="+ URLEncoder.encode("z(100 : 1000)", StandardCharsets.UTF_8)
                                    + "&subset="+ URLEncoder.encode("time(\"1990-06-10T11:00:00Z\" : \"2050-01-01T01:00:00Z\")", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 3,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0, 250.0, 350.0 ]
                     },
                     "interval" : [ 150.0, 350.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 3,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T10:20:00Z",
                       "coordinates" : [ "2000-06-10T10:20:00Z", "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                     },
                     "interval" : [ "2000-06-10T10:20:00Z", "2000-06-10T11:40:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 2304,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 3,
                           "time" : 3
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t0 v0t1 v0t2 v1t0 v1t1 v1t2 v2t0 v2t1 v2t2
            final double[] repeat = new double[]{   1,   4,   7,   2,   5,   8,   3,   6,   9};
            assertEquals(2304, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }


    }

    // ////////////////////////////////////////////////////////////////////////
    // 9.3.  datetime query parameter
    // A.3.2.  Abstract Test for Requirement datetime query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-subsetting_datetime
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-subsetting_datetime
     */
    @Test
    public void requirement6() throws Exception {
        initLayerList();

        final String dataId = "coverage4d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";

        { //check time subset as point
            final String parameters = "datetime="+ URLEncoder.encode("2000-06-10T11:00:00Z", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0 ]
                     },
                     "interval" : [ 150.0, 150.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T11:00:00Z",
                       "coordinates" : [ "2000-06-10T11:00:00Z" ]
                     },
                     "interval" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:00:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 256,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 1,
                           "time" : 1
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t1
            final double[] repeat = new double[]{   4 };
            assertEquals(256, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }

        { //check time subset as interval
            final String parameters = "datetime="+ URLEncoder.encode("2000-06-10T11:00:00Z / 2030-01-01T01:00:00Z", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0 ]
                     },
                     "interval" : [ 150.0, 150.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 2,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T11:00:00Z",
                       "coordinates" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                     },
                     "interval" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 512,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 1,
                           "time" : 2
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t1 v0t2
            final double[] repeat = new double[]{   4,   7 };
            assertEquals(512, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }

        { //check time subset as infinite interval
            final String parameters = "datetime="+ URLEncoder.encode("2000-06-10T11:00:00Z / ..", StandardCharsets.UTF_8);
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?" + parameters),
                        "application/json", DggrsData.class);
            assertEquals(
                """
                 {
                   "dggrs" : "https://en.wikipedia.org/wiki/HEALPix",
                   "zoneId" : "14",
                   "depths" : [ 4 ],
                   "schema" : {
                     "type" : "object",
                     "properties" : {
                       "test" : {
                         "title" : "test",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "dimensions" : [ {
                     "name" : "z",
                     "grid" : {
                       "cellsCount" : 1,
                       "resolution" : 100.0,
                       "firstCoordinate" : 150.0,
                       "coordinates" : [ 150.0 ]
                     },
                     "interval" : [ 150.0, 150.0 ]
                   }, {
                     "name" : "time",
                     "grid" : {
                       "cellsCount" : 2,
                       "resolution" : "PT40M",
                       "firstCoordinate" : "2000-06-10T11:00:00Z",
                       "coordinates" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                     },
                     "interval" : [ "2000-06-10T11:00:00Z", "2000-06-10T11:40:00Z" ]
                   } ],
                   "values" : {
                     "test" : [ {
                       "depth" : 4,
                       "shape" : {
                         "count" : 512,
                         "subZones" : 256,
                         "dimensions" : {
                           "z" : 1,
                           "time" : 2
                         }
                       },
                       "data" : []
                     } ]
                   }
                 }""", noData(dto));

            final double[] data = dto.getValues().get("test").get(0).getData().stream().mapToDouble((o)-> ((Number)o).doubleValue()).toArray();
            //                                    v0t1 v0t2
            final double[] repeat = new double[]{   4,   7};
            assertEquals(512, data.length);
            for (int i = 0; i < data.length; i++) {
                assertEquals(repeat[i % repeat.length], data[i], 0.0);
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 9.4.  properties query parameter
    // A.3.3.  Abstract Test for Requirement properties query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-subsetting_properties
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-subsetting_properties
     */
    @Test
    public void requirement7() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";


        { //check getting only property band2
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=2&properties=band2"),
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
                       "band2" : {
                         "title" : "band2",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "values" : {
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

        { //check incorrect band name
            final HttpResponse<byte[]> response = sendRequest(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?zone-depth=2&properties=band5"),
                        "application/json");
            assertEquals(400, response.statusCode());
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // 9.5.  exclude-properties query parameter
    // A.3.4.  Abstract Test for Requirement exclude-properties query parameter
    // ////////////////////////////////////////////////////////////////////////

    /**
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#conf_data-subsetting_exclude-properties
     * @see https://docs.ogc.org/DRAFTS/21-038r1.html#_req_data-subsetting_exclude-properties
     */
    @Test
    public void requirement8() throws Exception {
        initLayerList();

        final String dataId = "coverage2d";
        final String dggrsId = "Healpix";
        final String zoneId = "14";


        { //check getting only property band2
            final DggrsData dto = sendRequestAndParse(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+dggrsId+"/zones/"+zoneId+"/data?zone-depth=2&exclude-properties=band1"),
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
                       "band2" : {
                         "title" : "band2",
                         "type" : "number",
                         "minItems" : 0,
                         "x-ogc-propertySeq" : 1
                       }
                     }
                   },
                   "values" : {
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

        { //check incorrect band name
            final HttpResponse<byte[]> response = sendRequest(
                        new URI("http://localhost:" + getCurrentPort() + "/WS/dggs/default/collections/"+dataId+"/dggs/"+ dggrsId + "/zones/"+ zoneId+"/data?zone-depth=2&exclude-properties=band5"),
                        "application/json");
            assertEquals(400, response.statusCode());
        }
    }

    private static String noData(DggrsData dto) {
        return dto.toString().replaceAll("\"data\" : \\[.*\\]", "\"data\" : []");
    }
}
