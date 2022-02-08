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
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.portrayal.MapLayers;
import org.apache.sis.util.ObjectConverters;
import org.constellation.business.IDataBusiness;
import org.constellation.business.IMapContextBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.converter.MapContextProcessReferenceConverter;
import org.constellation.dto.DataBrief;
import org.constellation.dto.process.MapContextProcessReference;
import org.constellation.test.SpringContextTest;
import org.constellation.test.utils.TestEnvironment;
import org.constellation.util.ParamUtilities;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
public class MapContextReferenceToMapLayersTest extends SpringContextTest {

    @Autowired
    private IDataBusiness dataBusiness;

    @Autowired
    protected IProviderBusiness providerBusiness;

    @Autowired
    protected IMapContextBusiness mpBusiness;

    private static int MC_ID = -1;

    private static String MC_NAME = "mp";

    private static boolean initialized = false;

    @PostConstruct
    public void init() throws Exception {
        if (!initialized) {
            dataBusiness.deleteAll();
            providerBusiness.removeAll();
            mpBusiness.initializeDefaultMapContextData();

            //Initialize geotoolkit
            ImageIO.scanForPlugins();
            org.geotoolkit.lang.Setup.initialize(null);

            // coverage-file datastore
            List<DataBrief> briefs = new ArrayList<>();
            Integer did = testResources.createProvider(TestEnvironment.TestResource.TIF, providerBusiness, null).datas.get(0).id;
            briefs.add(dataBusiness.getDataBrief(did, true, true));

            did = testResources.createProvider(TestEnvironment.TestResource.PNG, providerBusiness, null).datas.get(0).id;
            briefs.add(dataBusiness.getDataBrief(did, true, true));

            MC_ID = mpBusiness.createFromData(null, MC_NAME, "EPSG:4326", new GeneralEnvelope(2), briefs);

            initialized = true;
        }
    }
   
    @Test
    public void convertTest() {
        final MapContextProcessReference ref = new MapContextProcessReference(MC_ID, MC_NAME);
        final MapLayers converted = ObjectConverters.convert(ref, MapLayers.class);
        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.getComponents().size());
    }

    @Test
    public void jsonConvertTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MapLayers.class, new StdDelegatingDeserializer<>(new MapContextProcessReferenceConverter()));

        String mpRef =   "{" +
                         "\"id\":" + MC_ID + "," +
                         "\"name\":\"" + MC_NAME + "\"" +
                         "}";
        mapper.registerModule(module);
        MapLayers converted = mapper.readValue(mpRef, MapLayers.class);
        Assert.assertNotNull(converted);
        Assert.assertEquals(2, converted.getComponents().size());


        // now try to read in the context of a parameter value
        final ParameterBuilder builder = new ParameterBuilder();

        final ParameterDescriptor<MapLayers> resParam = builder.addName("mlParam").setRequired(true).create(MapLayers.class, null);
        final ParameterDescriptorGroup descriptor    =  builder.addName("group").setRequired(true).createGroup(resParam);

        String json = "{\"mlParam\":" +  mpRef + "}";

        ParameterValueGroup pvalue = (ParameterValueGroup) ParamUtilities.readParameterJSON(json, descriptor);

        Object value = pvalue.parameter("mlParam").getValue();
        assertTrue(value instanceof MapLayers);
       Assert.assertEquals(2, ((MapLayers)value).getComponents().size());

    }
}