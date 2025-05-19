/*
 *     Examind Community - An open source and standard compliant SDI
 *     https://community.examind.com/
 *
 *  Copyright 2023 Geomatys.
 *
 *  Licensed under the Apache License, Version 2.0 (    the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package org.constellation.process.service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.provider.Data;
import org.constellation.provider.DataProviders;
import org.constellation.provider.FeatureData;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author guilhem
 */
public class FeatureTogeoJSONTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.process.service");

    @Autowired
    protected IProviderBusiness providerBusiness;
    @Autowired
    protected IDataBusiness dataBusiness;
    
    private static boolean initialized = false;

    private static Integer DATA_ID = null;

    @PostConstruct
    public void setUpClass() {
        if (!initialized) {
            try {

                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                final List<DataImport> datas = testResources.createProviders(TestEnvironment.TestResource.WMS111_SHAPEFILES, providerBusiness, null).datas();
                for (DataImport data : datas) {
                    if (data.name.equals("Bridges")) {
                        DATA_ID = data.id;
                    }
                }
                
                
                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "error while initializing test", ex);
            }
        }
    }

    @Test
    public void toGeojsonTestTest() throws Exception {
        Assert.assertNotNull("Test initialization does not found 'Bridges' vector data", DATA_ID);
        Data data = DataProviders.getProviderData(DATA_ID);

        Assert.assertNotNull("Unable to found 'Bridges' vector data", data);

        Assert.assertTrue(data instanceof FeatureData);

        final ProcessDescriptor desc = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, FeatureToGeoJSONDescriptor.NAME);

        final ParameterValueGroup in = desc.getInputDescriptor().createValue();
        in.parameter(FeatureToGeoJSONDescriptor.FEATURESET_NAME).setValue(((FeatureData)data).getOrigin());

        org.geotoolkit.process.Process proc = desc.createProcess(in);

        ParameterValueGroup results = proc.call();
        String jsonResult = results.parameter(FeatureToGeoJSONDescriptor.GEOJSON_OUTPUT_NAME).stringValue();
        System.out.println(jsonResult);
        String expected = """
                          {
                            "type" : "FeatureCollection",
                            "features" : [ {
                              "type" : "Feature",
                              "id" : "Bridges.1",
                              "geometry" : {
                                "type" : "Point",
                                "coordinates" : [ 0.0007, 0.0002 ]
                              },
                              "properties" : {
                                "FID" : "110",
                                "NAME" : "Cam Bridge"
                              }
                            } ]
                          }""";
                         
        Assert.assertEquals(expected, jsonResult);
    }
}
