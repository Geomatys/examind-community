/*
 *    Examind community - An open source and standard compliant SDI
 *    https://community.examind.com/
 *
 * Copyright 2022 Geomatys.
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
package org.constellation.admin.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.sis.storage.Resource;
import org.apache.sis.util.ObjectConverters;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.SpringTestRunner;
import org.constellation.test.utils.TestEnvironment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.logging.Logger;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.storage.FeatureSet;
import org.apache.sis.storage.GridCoverageResource;
import org.constellation.converter.DataProcessReferenceConverter;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.constellation.util.ParamUtilities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;


@RunWith(SpringTestRunner.class)
public class DataProcessReferenceToResourceTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.converter");

    /**
     * DatasetBusiness used for provider GUI editors data
     */
    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    private static int nbVectorData = -1;

    private static DataImport covData;
    private static DataImport vectData;

    private static boolean initialized = false;

    @PostConstruct
    public void init() throws Exception {
        if (!initialized) {
            dataBusiness.deleteAll();
            providerBusiness.removeAll();

            //Initialize geotoolkit
            ImageIO.scanForPlugins();
            org.geotoolkit.lang.Setup.initialize(null);

            // coverage-file datastore
            covData = testResources.createProvider(TestEnvironment.TestResource.TIF, providerBusiness, null).datas.get(0);
            testResources.createProvider(TestEnvironment.TestResource.PNG, providerBusiness, null);

            // shapefile datastore
            List<DataImport> datas = testResources.createProviders(TestEnvironment.TestResource.SHAPEFILES, providerBusiness, null).datas();
            nbVectorData = datas.size();
            vectData = datas.get(0);

            initialized = true;
        }
    }

    /**
     * This test uses all datas already existing in examind.
     * The Datas are added to a list as DataProcessReferences.
     * The converter DataProcessReferenceToResourceConverter is called to convert all those datas from DataProcessReference to Resources.
     */
    @Test
    public void convertTest() {
        List<DataProcessReference> dataPRef = dataBusiness.findDataProcessReference("VECTOR");
        assertEquals(nbVectorData, dataPRef.size());
        for (DataProcessReference dpr : dataPRef) {
            final Resource converted = ObjectConverters.convert(dpr, Resource.class);
            Assert.isInstanceOf(FeatureSet.class, converted);
        }

        dataPRef = dataBusiness.findDataProcessReference("COVERAGE");
        assertEquals(2, dataPRef.size());
        for (DataProcessReference dpr : dataPRef) {
            final Resource converted = ObjectConverters.convert(dpr, Resource.class);
            Assert.isInstanceOf(GridCoverageResource.class, converted);
        }
    }

    @Test
    public void jsonConvertGridCoverageTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Resource.class, new StdDelegatingDeserializer<>(new DataProcessReferenceConverter()));

        String dataRef = "{" +
                         "\"id\":" + covData.id + "," +
                         "\"name\":\"" + covData.name + "\"," +
                         "\"namespace\":\"" + covData.namespace + "\"," +
                         "\"provider\":" + covData.pid  +
                         "}";
        mapper.registerModule(module);
        Resource imm = mapper.readValue(dataRef, Resource.class);
        assertTrue(imm instanceof Resource);
        assertTrue(imm instanceof GridCoverageResource);


         /* will fail because of jackson converter inheritance
            https://github.com/FasterXML/jackson-databind/issues/2596
        GridCoverageResource gcr = mapper.readValue(dataRef, GridCoverageResource.class);
        assertTrue(gcr instanceof Resource);*/

        // now try to read in the context of a parameter value
        final ParameterBuilder builder = new ParameterBuilder();

        final ParameterDescriptor<Resource> resParam = builder.addName("resourceParam").setRequired(true).create(Resource.class, null);
        final ParameterDescriptorGroup descriptor    =  builder.addName("group").setRequired(true).createGroup(resParam);

        String json = "{\"resourceParam\":" +  dataRef + "}";

        ParameterValueGroup pvalue = (ParameterValueGroup) ParamUtilities.readParameterJSON(json, descriptor);

        Object value = pvalue.parameter("resourceParam").getValue();
        assertTrue(value instanceof Resource);
        assertTrue(value instanceof GridCoverageResource);

    }

    @Test
    public void jsonConvertFeatureSetTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Resource.class, new StdDelegatingDeserializer<>(new DataProcessReferenceConverter()));

        String dataRef = "{" +
                         "\"id\":" + vectData.id + "," +
                         "\"name\":\"" + vectData.name + "\"," +
                         "\"namespace\":\"" + vectData.namespace + "\"," +
                         "\"provider\":" + vectData.pid  +
                         "}";
        mapper.registerModule(module);
        Resource imm = mapper.readValue(dataRef, Resource.class);
        assertTrue(imm instanceof Resource);
        assertTrue(imm instanceof FeatureSet);

        /* will fail because of jackson converter inheritance
            https://github.com/FasterXML/jackson-databind/issues/2596

        FeatureSet fs = mapper.readValue(dataRef, FeatureSet.class);
        assertTrue(fs instanceof Resource);*/

        // now try to read in the context of a parameter value
        final ParameterBuilder builder = new ParameterBuilder();

        final ParameterDescriptor<Resource> resParam = builder.addName("resourceParam").setRequired(true).create(Resource.class, null);
        final ParameterDescriptorGroup descriptor    =  builder.addName("group").setRequired(true).createGroup(resParam);

        String json = "{\"resourceParam\":" +  dataRef + "}";

        ParameterValueGroup pvalue = (ParameterValueGroup) ParamUtilities.readParameterJSON(json, descriptor);

        Object value = pvalue.parameter("resourceParam").getValue();
        assertTrue(value instanceof Resource);
        assertTrue(value instanceof FeatureSet);

    }

}
