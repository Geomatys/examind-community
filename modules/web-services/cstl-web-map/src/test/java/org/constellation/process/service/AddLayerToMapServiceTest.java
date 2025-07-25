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

import org.constellation.dto.service.config.wxs.GetFeatureInfoCfg;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.map.featureinfo.CSVFeatureInfoFormat;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.process.ExamindProcessFactory;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.process.ProcessDescriptor;
import org.geotoolkit.process.ProcessFinder;
import org.junit.*;
import org.opengis.filter.Filter;
import org.opengis.parameter.ParameterValueGroup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CommonCRS;
import org.constellation.dto.StyleReference;
import org.constellation.dto.process.DataProcessReference;
import org.constellation.dto.process.ServiceProcessReference;
import org.constellation.exception.ConstellationException;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.constellation.test.utils.TestEnvironment.ProvidersImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.geotoolkit.filter.FilterUtilities;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.opengis.filter.FilterFactory;

/**
 *
 * @author Quentin Boileau (Geomatys)
 */
public abstract class AddLayerToMapServiceTest extends AbstractMapServiceTest {

    private static final String PROCESS_NAME = "service.add_layer";
    private static DataProcessReference COUNTRIES_DATA_REF;
    private static final StyleReference STYLE_DATA_REF = new StyleReference(null, "redBlue", 1, "sld");
    private static final FilterFactory FF = FilterUtilities.FF;

    private List<Integer> providerIds;

    @Before
    public void createProvider() throws Exception {
        //setup data
        final ProvidersImport pis = testResources.createProviders(TestResource.SHAPEFILES, providerBusiness, null);
        final DataImport di = pis.findDataByName("Countries");
        providerIds = pis.pids();
        COUNTRIES_DATA_REF = new DataProcessReference(di.id, "Countries", null, null, di.pid);
    }

    @After
    public void destroyProvider() throws ConstellationException {
        for (Integer providerId : providerIds) {
            providerBusiness.removeProvider(providerId);
        }
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
    public void testAddSFLayerToConfiguration() throws Exception {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        //init
        Integer serviceId = null;
        try{
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            ServiceProcessReference servRef = createCustomInstance("addLayer1", inputContext);
            serviceId = servRef.getId();
            startInstance("addLayer1");

            GeneralEnvelope env = new GeneralEnvelope(CommonCRS.defaultGeographic());
            env.setRange(0, 10, 30);
            env.setRange(1, 0, 50);
            final Filter bbox = FF.bbox(FF.property("geom"), env);

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue(servRef);

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final LayerConfig outputLayer = (LayerConfig) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(outputLayer);
            final List<LayerConfig> layers = layerBusiness.getLayers(serviceId, null);
            assertTrue(layers.size() == 1);

            final LayerConfig outLayer = layers.get(0);
            assertEquals(COUNTRIES_DATA_REF.getName(), outLayer.getName().getLocalPart());
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
    public void testAddSFLayerToConfiguration2() throws Exception {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        Integer serviceId = null;
        try {
            final LayerContext inputContext = new LayerContext();
            ServiceProcessReference servRef = createCustomInstance("addLayer2", inputContext);
            serviceId = servRef.getId();
                    
            startInstance("addLayer2");

            GeneralEnvelope env = new GeneralEnvelope(CommonCRS.defaultGeographic());
            env.setRange(0, 10, 30);
            env.setRange(1, 0, 50);
            final Filter bbox = FF.bbox(FF.property("geom"), env);

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue(servRef);

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final LayerConfig layer = (LayerConfig) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(layer);

            final List<LayerConfig> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(layer.getGetFeatureInfoCfgs().isEmpty()); //default generic GetFeatureInfo


            final LayerConfig outLayer = layers.get(0);
            assertEquals(COUNTRIES_DATA_REF.getName() ,outLayer.getName().getLocalPart());
            assertEquals("Europe-costlines" ,outLayer.getAlias());
            assertNotNull(outLayer.getFilter());
            assertEquals(STYLE_DATA_REF ,outLayer.getStyles().get(0));

            assertTrue(checkInstanceExist("addLayer2"));
        } finally {
            deleteInstance(layerBusiness,  serviceId);
        }
    }

    /**
     * No style, no filter, no alias
     */
     @Test
    public void testAddSFLayerToConfiguration6() throws Exception {

        Integer serviceId = null;
        try {
        //init
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            ServiceProcessReference servRef = createCustomInstance("addLayer6", inputContext);
            serviceId = servRef.getId();
            startInstance("addLayer6");

            final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue(servRef);

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final LayerConfig outputLayer = (LayerConfig) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(outputLayer);

            final List<LayerConfig> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(outputLayer.getGetFeatureInfoCfgs().isEmpty()); //default generic GetFeatureInfo


            final LayerConfig outLayer = layers.get(0);
            assertEquals(COUNTRIES_DATA_REF.getName(),outLayer.getName().getLocalPart());
            assertNull(outLayer.getAlias());
            assertNull(outLayer.getFilter());
            assertTrue(outLayer.getStyles().isEmpty());

            assertTrue(checkInstanceExist("addLayer6"));

        } finally {
            deleteInstance(layerBusiness, serviceId);
        }

    }

    /**
     * Test custom GetFeatureInfo
     */
    @Test
    public void testAddSFLayerToConfiguration7() throws Exception {
        final ProcessDescriptor descriptor = ProcessFinder.getProcessDescriptor(ExamindProcessFactory.NAME, PROCESS_NAME);

        Integer serviceId = null;
        try {
            //init
            final LayerContext inputContext = new LayerContext();
            inputContext.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

            ServiceProcessReference servRef = createCustomInstance("addLayer7", inputContext);
            serviceId = servRef.getId();
            
            startInstance("addLayer7");

            GeneralEnvelope env = new GeneralEnvelope(CommonCRS.defaultGeographic());
            env.setRange(0, 10, 30);
            env.setRange(1, 0, 50);
            final Filter bbox = FF.bbox(FF.property("geom"), env);

            final GetFeatureInfoCfg[] customGFI = new GetFeatureInfoCfg[1];
            customGFI[0] = new GetFeatureInfoCfg("text/plain", CSVFeatureInfoFormat.class.getCanonicalName());

            final ParameterValueGroup inputs = descriptor.getInputDescriptor().createValue();
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_REF_PARAM_NAME).setValue(COUNTRIES_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_ALIAS_PARAM_NAME).setValue("Europe-costlines");
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_STYLE_PARAM_NAME).setValue(STYLE_DATA_REF);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_FILTER_PARAM_NAME).setValue(bbox);
            inputs.parameter(AddLayerToMapServiceDescriptor.SERVICE_INSTANCE_PARAM_NAME).setValue(servRef);
            inputs.parameter(AddLayerToMapServiceDescriptor.LAYER_CUSTOM_GFI_PARAM_NAME).setValue(customGFI);

            final org.geotoolkit.process.Process process = descriptor.createProcess(inputs);
            final ParameterValueGroup outputs = process.call();
            final LayerConfig output = (LayerConfig) outputs.parameter(AddLayerToMapServiceDescriptor.OUT_LAYER_PARAM_NAME).getValue();

            assertNotNull(output);

            final List<LayerConfig> layers = layerBusiness.getLayers(serviceId, null);
            assertFalse(layers.isEmpty());
            assertTrue(layers.size() == 1);
            assertTrue(output.getGetFeatureInfoCfgs().size() == 1); //default generic GetFeatureInfo


            final LayerConfig outLayer = layers.get(0);
            assertEquals(COUNTRIES_DATA_REF.getName(),outLayer.getName().getLocalPart());
            assertEquals("Europe-costlines" ,outLayer.getAlias());
            assertNotNull(outLayer.getFilter());
            assertEquals(STYLE_DATA_REF, outLayer.getStyles().get(0));
            assertTrue(outLayer.getGetFeatureInfoCfgs().size() == 1);

            final GetFeatureInfoCfg outGFI = outLayer.getGetFeatureInfoCfgs().get(0);
            assertEquals("text/plain", outGFI.getMimeType());
            assertEquals(CSVFeatureInfoFormat.class.getCanonicalName(), outGFI.getBinding());

            assertTrue(checkInstanceExist("addLayer7"));

        } finally {
            deleteInstance(layerBusiness, serviceId);
        }
    }
}
