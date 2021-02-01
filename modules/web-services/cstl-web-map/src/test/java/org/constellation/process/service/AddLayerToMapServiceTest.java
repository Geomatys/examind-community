/*
 *    Constellation - An open source and standard compliant SDI
 *    http://www.constellation-sdi.org
 *
 * Copyright 2014 Geomatys.
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
package org.constellation.process.service;

import org.constellation.util.Util;
import org.constellation.exception.ConfigurationException;
import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.Layer;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.map.featureinfo.CSVFeatureInfoFormat;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.process.ExamindProcessFactory;
import org.constellation.util.DataReference;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessException;
import org.geotoolkit.process.ProcessFinder;
import org.junit.*;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.NoSuchIdentifierException;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.sis.internal.system.DefaultFactories;
import org.constellation.dto.ProviderBrief;
import org.constellation.dto.StyleReference;
import org.geotoolkit.util.NamesExt;
import org.constellation.exception.ConstellationException;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Quentin Boileau (Geomatys)
 */
public abstract class AddLayerToMapServiceTest extends AbstractMapServiceTest {

    private static final String PROCESS_NAME = "service.add_layer";
    private static DataReference COUNTRIES_DATA_REF;
    private static final StyleReference STYLE_DATA_REF = new StyleReference(null, "redBlue", 1, "sld");
    private static final FilterFactory FF = DefaultFactories.forBuildin(FilterFactory.class);

    private Integer providerId;

    @Before
    public void createProvider() throws ConfigurationException, IOException, URISyntaxException {

        //setup data
        final TestResources testResource = initDataDirectory();
        providerId = testResource.createProvider(TestResource.SHAPEFILES, providerBusiness);
        try {
            providerBusiness.createOrUpdateData(providerId, null, true);
            ProviderBrief pb = providerBusiness.getProvider(providerId);
            COUNTRIES_DATA_REF = DataReference.createProviderDataReference(DataReference.PROVIDER_LAYER_TYPE, pb.getIdentifier(), "Countries");
        } catch (IOException | ConstellationException ex) {
            throw new ConfigurationException(ex.getMessage(),ex);
        }
    }

    @After
    public void destroyProvider() throws ConstellationException {
        providerBusiness.removeProvider(providerId);
    }

    @AfterClass
    public static void teardown() {
        Path shapes = Paths.get("/target/"+serviceName+"_"+AddLayerToMapServiceDescriptor.NAME);
        if (Files.exists(shapes)) {
            IOUtilities.deleteSilently(shapes);
        }
    }

    public AddLayerToMapServiceTest(final String serviceName, final Class workerClass) {
        super(AddLayerToMapServiceDescriptor.NAME, serviceName, workerClass);
    }

    @Test
    public void testAddSFLayerToConfiguration() throws NoSuchIdentifierException, ProcessException, MalformedURLException, ConfigurationException {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        //init
        Integer serviceId = null;
        try{
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            serviceId = createCustomInstance("addLayer1", inputContext);
            startInstance("addLayer1");

            final Filter bbox = FF.bbox("geom", 10, 0, 30, 50, null);

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_TYPE_PARAM_NAME).setValue(serviceName);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue("addLayer1");

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final Layer outputLayer = (Layer) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(outputLayer);
            final List<Layer> layers = layerBusiness.getLayers(serviceId, null);
            assertTrue(layers.size() == 1);

            final Layer outLayer = layers.get(0);
            assertEquals(Util.getLayerId(COUNTRIES_DATA_REF).tip().toString(),outLayer.getName().getLocalPart());
            assertEquals("Europe-costlines" ,outLayer.getAlias());
            assertNotNull(outLayer.getFilter());
            assertEquals(STYLE_DATA_REF ,outLayer.getStyles().get(0));

            assertTrue(checkInstanceExist("addLayer1"));
        }finally{
            deleteInstance( layerBusiness, serviceId);
        }
    }


     /**
     * Source exist
     */
    @Test
    public void testAddSFLayerToConfiguration2() throws NoSuchIdentifierException, ProcessException, MalformedURLException, ConfigurationException {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        Integer serviceId = null;
        try{
            final LayerContext inputContext = new LayerContext();
            serviceId = createCustomInstance("addLayer2", inputContext);

            startInstance("addLayer2");

            final Filter bbox = FF.bbox("geom", 10, 0, 30, 50, null);

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_TYPE_PARAM_NAME).setValue(serviceName);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue("addLayer2");

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final Layer layer = (Layer) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(layer);

            final List<Layer> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(layer.getGetFeatureInfoCfgs().isEmpty()); //default generic GetFeatureInfo


            final Layer outLayer = layers.get(0);
            assertEquals(Util.getLayerId(COUNTRIES_DATA_REF).tip().toString() ,outLayer.getName().getLocalPart());
            assertEquals("Europe-costlines" ,outLayer.getAlias());
            assertNotNull(outLayer.getFilter());
            assertEquals(STYLE_DATA_REF ,outLayer.getStyles().get(0));

            assertTrue(checkInstanceExist("addLayer2"));
        }finally{
            deleteInstance(layerBusiness,  serviceId);
        }
    }

     /**
     * Layer already exist -> replacement
     */
    @Test
    public void testAddSFLayerToConfiguration3() throws NoSuchIdentifierException, ProcessException, MalformedURLException, ConstellationException {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        Integer serviceId = null;
        try{
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            serviceId = createCustomInstance("addLayer3", inputContext);
            startInstance("addLayer3");

            Integer dataId = dataBusiness.getDataId(new QName("Countries"), providerId);

            Layer layer = new Layer(new QName(NamesExt.getNamespace(Util.getLayerId(COUNTRIES_DATA_REF)), Util.getLayerId(COUNTRIES_DATA_REF).tip().toString()));
            layer.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            layerBusiness.add(dataId,
                              "Europe-costlines",
                              null,
                              "Europe-costlines",
                              serviceId,
                              layer);


            final Filter bbox = FF.bbox("geom", 10, 0, 30, 50, null);

            final List<GetFeatureInfoCfg> gfi = FeatureInfoUtilities.createGenericConfiguration();
            final GetFeatureInfoCfg[] customGFI = gfi.toArray(new GetFeatureInfoCfg[gfi.size()]);

            //exec process
            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_TYPE_PARAM_NAME).setValue(serviceName);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue("addLayer3");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_CUSTOM_GFI_PARAM_NAME).setValue(customGFI);

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final Layer outputLayer = (Layer) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(outputLayer);

            final List<Layer> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(outputLayer.getGetFeatureInfoCfgs().size() > 0); //default generic GetFeatureInfo

            final Layer outLayer = layers.get(0);
            assertEquals(Util.getLayerId(COUNTRIES_DATA_REF).tip().toString(),outLayer.getName().getLocalPart());
            assertEquals("Europe-costlines" ,outLayer.getAlias());
            assertNotNull(outLayer.getFilter());
            assertEquals(STYLE_DATA_REF ,outLayer.getStyles().get(0));

            assertTrue(checkInstanceExist("addLayer3"));
        }finally{
            deleteInstance(layerBusiness, serviceId);
        }
    }


    /**
     *  Source in loadAllMode and layer already exist in exclude list
     */
     @Test
    public void testAddSFLayerToConfiguration5() throws NoSuchIdentifierException, ProcessException, MalformedURLException, ConstellationException {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        Integer serviceId = null;
        try{
            //init
            final LayerContext inputContext = new LayerContext();
            final List<GetFeatureInfoCfg> gfi = FeatureInfoUtilities.createGenericConfiguration();
            final GetFeatureInfoCfg[] gfiArray = gfi.toArray(new GetFeatureInfoCfg[gfi.size()]);
            inputContext.setGetFeatureInfoCfgs(gfi);

            serviceId = createCustomInstance("addLayer5", inputContext);
            startInstance("addLayer5");

            Integer countriesDataId = dataBusiness.getDataId(new QName("Countries"), providerId);

            Layer layer1 = new Layer(new QName(NamesExt.getNamespace(Util.getLayerId(COUNTRIES_DATA_REF)), Util.getLayerId(COUNTRIES_DATA_REF).tip().toString()));
            layer1.setGetFeatureInfoCfgs(gfi);
            layerBusiness.add(countriesDataId,
                              "Europe-costlines",
                               null,
                              "Europe-costlines",
                              serviceId,
                              layer1);

            Integer cityDataId = dataBusiness.getDataId(new QName("city"), providerId);

            Layer layer2 = new Layer(new QName(NamesExt.getNamespace(Util.getLayerId(COUNTRIES_DATA_REF)), "city"));
            layer2.setGetFeatureInfoCfgs(gfi);
            layerBusiness.add(cityDataId,
                              null,
                              null,
                              "city",
                              serviceId,
                              layer2);


            final Filter bbox = FF.bbox("geom", 10, 0, 30, 50, null);

            //exec process
            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_TYPE_PARAM_NAME).setValue(serviceName);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue("addLayer5");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_CUSTOM_GFI_PARAM_NAME).setValue(gfiArray);


            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final Layer outputLayer = (Layer) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(outputLayer);

            final List<Layer> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 2);
            assertTrue(outputLayer.getGetFeatureInfoCfgs().size() > 0); //default generic GetFeatureInfo

             for (Layer outLayer : layers) {
                 assertNotNull(outLayer);
                 if (Util.getLayerId(COUNTRIES_DATA_REF).tip().toString().equals(outLayer.getName().getLocalPart())) {
                     assertEquals("Europe-costlines", outLayer.getAlias());
                     assertNotNull(outLayer.getFilter());
                     assertEquals(STYLE_DATA_REF, outLayer.getStyles().get(0));
                 }
             }

            //assertTrue(outSource.isExcludedLayer(new QName("http://custom-namespace/", "city")));


            assertTrue(checkInstanceExist("addLayer5"));

        }finally{
            deleteInstance(layerBusiness, serviceId);
        }

    }

    /**
     * No style, no filter, no alias
     */
     @Test
    public void testAddSFLayerToConfiguration6() throws NoSuchIdentifierException, ProcessException, MalformedURLException, ConfigurationException {

        Integer serviceId = null;
        try{
        //init
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            serviceId = createCustomInstance("addLayer6", inputContext);
            startInstance("addLayer6");

            final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_TYPE_PARAM_NAME).setValue(serviceName);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue("addLayer6");

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final Layer outputLayer = (Layer) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(outputLayer);

            final List<Layer> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(outputLayer.getGetFeatureInfoCfgs().isEmpty()); //default generic GetFeatureInfo


            final Layer outLayer = layers.get(0);
            assertEquals(Util.getLayerId(COUNTRIES_DATA_REF).tip().toString(),outLayer.getName().getLocalPart());
            assertNull(outLayer.getAlias());
            assertNull(outLayer.getFilter());
            assertTrue(outLayer.getStyles().isEmpty());

            assertTrue(checkInstanceExist("addLayer6"));

        }finally{
            deleteInstance(layerBusiness, serviceId);
        }

    }

    /**
     * Test custom GetFeatureInfo
     */
    @Test
    public void testAddSFLayerToConfiguration7() throws NoSuchIdentifierException, ProcessException, MalformedURLException, ConfigurationException {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        Integer serviceId = null;
        try{
            //init
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            serviceId = createCustomInstance("addLayer7", inputContext);
            startInstance("addLayer7");

            final Filter bbox = FF.bbox("geom", 10, 0, 30, 50, null);
            final GetFeatureInfoCfg[] customGFI = new GetFeatureInfoCfg[1];
            customGFI[0] = new GetFeatureInfoCfg("text/plain", CSVFeatureInfoFormat.class.getCanonicalName());

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_TYPE_PARAM_NAME).setValue(serviceName);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue("addLayer7");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_CUSTOM_GFI_PARAM_NAME).setValue(customGFI);

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final Layer output = (Layer) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(output);

            final List<Layer> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(output.getGetFeatureInfoCfgs().size() == 1); //default generic GetFeatureInfo


            final Layer outLayer = layers.get(0);
            assertEquals(Util.getLayerId(COUNTRIES_DATA_REF).tip().toString(),outLayer.getName().getLocalPart());
            assertEquals("Europe-costlines" ,outLayer.getAlias());
            assertNotNull(outLayer.getFilter());
            assertEquals(STYLE_DATA_REF, outLayer.getStyles().get(0));
            assertTrue(outLayer.getGetFeatureInfoCfgs().size() == 1);

            final GetFeatureInfoCfg outGFI = outLayer.getGetFeatureInfoCfgs().get(0);
            assertEquals("text/plain", outGFI.getMimeType());
            assertEquals(CSVFeatureInfoFormat.class.getCanonicalName(), outGFI.getBinding());

            assertTrue(checkInstanceExist("addLayer7"));

        }finally{
            deleteInstance(layerBusiness, serviceId);
        }
    }

}
