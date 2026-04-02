/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2019 Geomatys.
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
package com.examind.json.binding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import org.constellation.dto.BandDescription;
import org.constellation.dto.CoverageDataDescription;
import org.constellation.dto.DataBrief;
import org.constellation.dto.FeatureDataDescription;
import org.constellation.dto.PropertyDescription;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPoint;

/**
 *
 * @author guilhem
 */
public class JsonBindingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    public JsonBindingTest() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void unmarshallingTest() throws IOException {
        
        String input = 
                """
                {
                  "targetStyle" : [ ],
                  "targetService" : [ ],
                  "targetSensor" : [ ],
                  "metadatas" : [ ],
                  "linkedDatas" : [ ],
                  "dataDescription" : {
                    "type" : "feature",
                    "boundingBox" : [ 1.1, 1.2, 1.3, 1.4 ],
                    "geometryProperty" : {
                      "namespace" : "http://my-namespace.org",
                      "name" : "geom",
                      "type" : "org.locationtech.jts.geom.MultiPoint"
                    },
                    "properties" : [ {
                      "namespace" : "http://my-namespace.org",
                      "name" : "db",
                      "type" : "java.lang.Double"
                    }, {
                      "namespace" : "http://my-namespace.org",
                      "name" : "st",
                      "type" : "java.lang.String"
                    } ]
                  },
                  "dimensions" : [ ]
                }""";
        
        DataBrief result = objectMapper.readValue(input, DataBrief.class);
        
        DataBrief expectedResult = new DataBrief();
        
        FeatureDataDescription fdd = new FeatureDataDescription();
        fdd.setGeometryProperty(new PropertyDescription("http://my-namespace.org", "geom", MultiPoint.class));
        fdd.setProperties(
                List.of(new PropertyDescription("http://my-namespace.org", "db", Double.class),
                        new PropertyDescription("http://my-namespace.org", "st", String.class)));
        fdd.setBoundingBox(new double[] {1.1, 1.2, 1.3, 1.4});
        
        expectedResult.setDataDescription(fdd);
        
        Assert.assertTrue(result.getDataDescription() instanceof FeatureDataDescription);
        Assert.assertEquals(expectedResult, result);
        
        input = 
                """
                {
                  "targetStyle" : [ ],
                  "targetService" : [ ],
                  "targetSensor" : [ ],
                  "metadatas" : [ ],
                  "linkedDatas" : [ ],
                  "dataDescription" : {
                    "type" : "coverage",
                    "boundingBox" : [ 1.1, 1.2, 1.3, 1.4 ],
                    "bands" : [ {
                      "indice" : "1",
                      "name" : "band 1",
                      "minValue" : 1.0,
                      "maxValue" : 12.0,
                      "noDataValues" : [ 1.1, 1.2 ]
                    }, {
                      "indice" : "2",
                      "name" : "band 2",
                      "minValue" : 2.0,
                      "maxValue" : 23.0,
                      "noDataValues" : [ 2.1, 2.2 ]
                    } ]
                  },
                  "dimensions" : [ ]
                }""";
        
        result = objectMapper.readValue(input, DataBrief.class);
        
        CoverageDataDescription covDesc = new CoverageDataDescription();
        
        covDesc.setBands(List.of(new BandDescription("1", "band 1", 1.0, 12.0, new double[]{1.1, 1.2}),
                                 new BandDescription("2", "band 2", 2.0, 23.0, new double[]{2.1, 2.2})));
        covDesc.setBoundingBox(new double[] {1.1, 1.2, 1.3, 1.4});
        
        expectedResult.setDataDescription(covDesc);
        
        Assert.assertTrue(result.getDataDescription() instanceof CoverageDataDescription);
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void marshallingTest() throws Exception {
        DataBrief db = new DataBrief();
        
        FeatureDataDescription fdd = new FeatureDataDescription();
        fdd.setGeometryProperty(new PropertyDescription("http://my-namespace.org", "geom", MultiPoint.class));
        fdd.setProperties(
                List.of(new PropertyDescription("http://my-namespace.org", "db", Double.class),
                        new PropertyDescription("http://my-namespace.org", "st", String.class)));
        fdd.setBoundingBox(new double[] {1.1, 1.2, 1.3, 1.4});
        
        db.setDataDescription(fdd);
        
        StringWriter jsonWriter = new StringWriter();
        objectMapper.writeValue(jsonWriter, db);
        
        String expectedResult = 
                """
                {
                  "targetStyle" : [ ],
                  "targetService" : [ ],
                  "targetSensor" : [ ],
                  "metadatas" : [ ],
                  "linkedDatas" : [ ],
                  "dataDescription" : {
                    "type" : "feature",
                    "boundingBox" : [ 1.1, 1.2, 1.3, 1.4 ],
                    "geometryProperty" : {
                      "namespace" : "http://my-namespace.org",
                      "name" : "geom",
                      "type" : "org.locationtech.jts.geom.MultiPoint"
                    },
                    "properties" : [ {
                      "namespace" : "http://my-namespace.org",
                      "name" : "db",
                      "type" : "java.lang.Double"
                    }, {
                      "namespace" : "http://my-namespace.org",
                      "name" : "st",
                      "type" : "java.lang.String"
                    } ]
                  },
                  "dimensions" : [ ]
                }""";
        Assert.assertEquals(expectedResult, jsonWriter.toString());
        
        CoverageDataDescription covDesc = new CoverageDataDescription();
        
        covDesc.setBands(List.of(new BandDescription("1", "band 1", 1.0, 12.0, new double[]{1.1, 1.2}),
                                 new BandDescription("2", "band 2", 2.0, 23.0, new double[]{2.1, 2.2})));
        covDesc.setBoundingBox(new double[] {1.1, 1.2, 1.3, 1.4});
        
        db.setDataDescription(covDesc);
        
        jsonWriter = new StringWriter();
        objectMapper.writeValue(jsonWriter, db);
        
        expectedResult = 
                """
                {
                  "targetStyle" : [ ],
                  "targetService" : [ ],
                  "targetSensor" : [ ],
                  "metadatas" : [ ],
                  "linkedDatas" : [ ],
                  "dataDescription" : {
                    "type" : "coverage",
                    "boundingBox" : [ 1.1, 1.2, 1.3, 1.4 ],
                    "bands" : [ {
                      "indice" : "1",
                      "name" : "band 1",
                      "minValue" : 1.0,
                      "maxValue" : 12.0,
                      "noDataValues" : [ 1.1, 1.2 ]
                    }, {
                      "indice" : "2",
                      "name" : "band 2",
                      "minValue" : 2.0,
                      "maxValue" : 23.0,
                      "noDataValues" : [ 2.1, 2.2 ]
                    } ]
                  },
                  "dimensions" : [ ]
                }""";
        Assert.assertEquals(expectedResult, jsonWriter.toString());
    }

}
