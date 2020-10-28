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
package org.constellation.ws.embedded;

// J2SE dependencies
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.constellation.admin.SpringHelper;
import org.constellation.exception.ConstellationException;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.ImageTesting;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import org.geotoolkit.image.jai.Registry;
import org.geotoolkit.ogc.xml.exception.ServiceExceptionReport;
import org.geotoolkit.ows.xml.v110.ExceptionReport;
import org.geotoolkit.wcs.xml.WCSMarshallerPool;
import org.geotoolkit.wcs.xml.v100.CoverageOfferingBriefType;
import org.geotoolkit.wcs.xml.v100.DCPTypeType.HTTP.Get;
import org.geotoolkit.wcs.xml.v100.LonLatEnvelopeType;
import org.geotoolkit.wcs.xml.v100.WCSCapabilitiesType;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.util.GenericName;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.domCompare;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getCurrentPort;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.unmarshallJsonResponse;
import org.geotoolkit.referencing.CRS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// JUnit dependencies

/**
 * A set of methods that request a Grizzly server which embeds a WCS service.
 *
 * @version $Id$
 *
 * @author Cédric Briançon (Geomatys)
 * @since 0.3
 */
@RunWith(TestRunner.class)
public class WCSRequestsTest extends AbstractGrizzlyServer {

    private static String EPSG_VERSION;

    /**
     * The layer to test.
     */
    private static final GenericName LAYER_TEST = NamesExt.create("SSTMDE200305");
    private static final GenericName LAYER_ALIAS = NamesExt.create("aliased");
    private static final GenericName LAYER_TEST2 = NamesExt.create("martinique");

    /**
     * URLs which will be tested on the server.
     */
    private static final String WCS_FALSE_REQUEST ="request=SomethingElse";

    private static final String WCS_FALSE_REQUEST_100 ="request=GetCoverage&service=WCS&version=1.0.0&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "crs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "coverage=wrongLayer";

    private static final String WCS_FALSE_REQUEST_111 ="request=GetCoverage&service=WCS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "crs=EPSG:4326&boundingbox=-180,-90,180,90,EPSG4326&" +
                                      "identifier=wrongLayer";

    private static final String WCS_GETCOVERAGE ="request=GetCoverage&service=WCS&version=1.0.0&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "crs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "coverage="+ LAYER_TEST;
    
    private static final String WCS_GETCOVERAGE_ALIAS ="request=GetCoverage&service=WCS&version=1.0.0&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "crs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "coverage="+ LAYER_ALIAS;

    private static final String WCS_GETCOVERAGE_MATRIX ="request=GetCoverage&service=WCS&version=1.0.0&" +
                                      "format=matrix&width=1024&height=512&" +
                                      "crs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "coverage="+ LAYER_TEST;

    private static final String WCS_GETCOVERAGE_PNG_TIFF_201 ="request=GetCoverage&service=WCS&version=2.0.1&" +
                                      "format=image/tiff&coverageid="+ LAYER_TEST;
    
    private static final String WCS_GETCOVERAGE_TIFF_TIFF_201 ="request=GetCoverage&service=WCS&version=2.0.1&" +
                                      "format=image/tiff&coverageid="+ LAYER_TEST2;
    
    private static final String WCS_GETCAPABILITIES ="request=GetCapabilities&service=WCS&version=1.0.0";

    private static final String WCS_GETCAPABILITIES2 ="request=GetCapabilities&service=WCS&version=1.0.0";

    private static final String WCS_DESCRIBECOVERAGE ="request=DescribeCoverage&coverage=SSTMDE200305&service=wcs&version=1.0.0";
    private static final String WCS_DESCRIBECOVERAGE_ALIAS ="request=DescribeCoverage&coverage=" + LAYER_ALIAS + "&service=wcs&version=1.0.0";
    private static final String WCS_DESCRIBECOVERAGE_TIFF ="request=DescribeCoverage&coverage=martinique&service=wcs&version=1.0.0";
    
    private static final String WCS_DESCRIBECOVERAGE_201_PNG ="request=DescribeCoverage&coverageid=SSTMDE200305&service=wcs&version=2.0.1";
    
    private static final String WCS_DESCRIBECOVERAGE_201_TIFF ="request=DescribeCoverage&coverageid=martinique&service=wcs&version=2.0.1";

    private static boolean initialized = false;

    @BeforeClass
    public static void initTestDir() {
        ConfigDirectory.setupTestEnvironement("WCSRequestsTest");
        controllerConfiguration = WCSControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void initLayerList() {

        if (!initialized) {
            try {
                startServer();

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();
                
                EPSG_VERSION = CRS.getVersion("EPSG").toString();

                final TestResources testResource = initDataDirectory();

                Integer pid = testResource.createProvider(TestResource.PNG, providerBusiness);
                Integer did = dataBusiness.create(new QName("SSTMDE200305"), pid, "COVERAGE", false, true, true, null, null);

                // second data for alias
                pid = testResource.createProvider(TestResource.PNG, providerBusiness);
                Integer did2 = dataBusiness.create(new QName("SSTMDE200305"), pid, "COVERAGE", false, true, true, null, null);
                
                pid = testResource.createProvider(TestResource.TIF, providerBusiness);
                Integer did3 = dataBusiness.create(new QName("martinique"), pid, "COVERAGE", false, true, null, null);

                final LayerContext config = new LayerContext();

                Integer defId = serviceBusiness.create("wcs", "default", config, null, null);
                layerBusiness.add(did, null, defId, null);
                layerBusiness.add(did2, "aliased", defId, null);
                layerBusiness.add(did3, null, defId, null);

                Integer testId = serviceBusiness.create("wcs", "test", config, null, null);
                layerBusiness.add(did, null, testId, null);


                pool = WCSMarshallerPool.getInstance();

                WorldFileImageReader.Spi.registerDefaults(null);

                //reset values, only allow pure java readers
                for(String jn : ImageIO.getReaderFormatNames()){
                    Registry.setNativeCodecAllowed(jn, ImageReaderSpi.class, false);
                }

                //reset values, only allow pure java writers
                for(String jn : ImageIO.getWriterFormatNames()){
                    Registry.setNativeCodecAllowed(jn, ImageWriterSpi.class, false);
                }

                serviceBusiness.start(defId);
                serviceBusiness.start(testId);
                waitForRestStart("wcs","default");
                waitForRestStart("wcs","test");

                initialized = true;
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class);
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class);
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class);
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class);
            if (provider != null) {
                provider.removeAll();
            }
        } catch (ConstellationException ex) {
            Logger.getAnonymousLogger().log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement("WCSRequestsTest");
        stopServer();
    }

    /**
     * Ensure that a wrong value given in the request parameter for the WCS server
     * returned an error report for the user.
     */
    @Test
    @Order(order=1)
    public void testWCSWrongRequest() throws Exception {
        initLayerList();

        // Creates an intentional wrong url, regarding the WCS version 1.0.0 standard
        URL wrongUrl;
        try {
            wrongUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_FALSE_REQUEST);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a ServiceExceptionReport.
        Object obj = unmarshallResponse(wrongUrl);
        assertTrue(obj instanceof ServiceExceptionReport);

        try {
            wrongUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_FALSE_REQUEST_100);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a ServiceExceptionReport.
        obj = unmarshallResponse(wrongUrl);
        assertTrue(obj instanceof ServiceExceptionReport);

        try {
            wrongUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_FALSE_REQUEST_111);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a OWS ExceptionReport.
        obj = unmarshallResponse(wrongUrl);
        assertTrue("exception type:" + obj.getClass().getName(), obj instanceof ExceptionReport);
    }

    /**
     * Ensures that a valid GetCoverage request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=2)
    public void testWCSGetCoverage() throws Exception {
        initLayerList();
        // Creates a valid GetCoverage url.
        final URL getCoverageUrl;
        try {
            getCoverageUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCOVERAGE);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get the coverage from the url.
        final BufferedImage image = getImageFromURL(getCoverageUrl, "image/png");

        // Test on the returned image.
        assertFalse (ImageTesting.isImageEmpty(image));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 8);
    }

    @Test
    @Order(order=2)
    public void testWCSGetCoverageAlias() throws Exception {
        initLayerList();
        // Creates a valid GetCoverage url.
        final URL getCoverageUrl;
        try {
            getCoverageUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCOVERAGE_ALIAS);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get the coverage from the url.
        final BufferedImage image = getImageFromURL(getCoverageUrl, "image/png");

        // Test on the returned image.
        assertFalse (ImageTesting.isImageEmpty(image));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 8);
    }

    @Test
    @Order(order=2)
    public void testWCSGetCoverage201() throws Exception {
        initLayerList();
        // Creates a valid GetCoverage url.
        URL getCoverageUrl;
        try {
            getCoverageUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCOVERAGE_PNG_TIFF_201);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get the coverage from the url.
        BufferedImage image = getImageFromURLByFormat(getCoverageUrl, "geotiff");
        // TODO verification on result
        
      /*  
        
        Issue here to read the tiff
        
        
        try {
            getCoverageUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCOVERAGE_TIFF_TIFF_201);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get the coverage from the url.
        image = getImageFromURLByFormat(getCoverageUrl, "geotiff");
        // TODO verification on result
        
        */
    }

    
    /**
     * Ensures a GetCoverage request with the output format matrix works fine.
     *
     * For now, this format is not well handled by the current Geotools. There are some
     * errors in the reading of this format, and they will be corrected in the next version
     * of Geotools.
     *
     * @TODO: do this test when moving of Geotools' version
     */
    @Ignore
    @Order(order=3)
    public void testWCSGetCoverageMatrixFormat() throws Exception {
        initLayerList();
        // Creates a valid GetCoverage url.
        final URL getCovMatrixUrl;
        try {
            getCovMatrixUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCOVERAGE_MATRIX);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        final BufferedImage image = getImageFromURL(getCovMatrixUrl, "application/matrix");
        //assertEquals(Commons.checksum(image), ...);
    }

    /**
     * Ensures that a valid GetCapabilities request returns indeed a valid GetCapabilities
     * document representing the server capabilities in the WCS version 1.0.0 standard.
     */
    @Test
    @Order(order=4)
    public void testWCSGetCapabilities() throws Exception {
        initLayerList();
        // Creates a valid GetCapabilities url.
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCAPABILITIES);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WCSCapabilitiesType.
        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj.toString(), obj instanceof WCSCapabilitiesType);

        WCSCapabilitiesType responseCaps = (WCSCapabilitiesType)obj;
        final List<CoverageOfferingBriefType> coverages = responseCaps.getContentMetadata().getCoverageOfferingBrief();

        assertNotNull(coverages);
        assertFalse(coverages.isEmpty());
        boolean layerTestFound = false;
        boolean layerAliasFound = false;
        for (CoverageOfferingBriefType coverage : coverages) {
            for (JAXBElement<String> elem : coverage.getRest()) {
                if (elem.getValue().equals(LAYER_TEST.tip().toString())) {
                    layerTestFound = true;
                    final LonLatEnvelopeType env = coverage.getLonLatEnvelope();
                    assertTrue(env.getPos().get(0).getValue().get(0) == -180d);
                    assertTrue(env.getPos().get(0).getValue().get(1) ==  -90d);
                    assertTrue(env.getPos().get(1).getValue().get(0) ==  180d);
                    assertTrue(env.getPos().get(1).getValue().get(1) ==   90d);
                }
                if (elem.getValue().equals(LAYER_ALIAS.tip().toString())) {
                    layerAliasFound = true;
                    final LonLatEnvelopeType env = coverage.getLonLatEnvelope();
                    assertTrue(env.getPos().get(0).getValue().get(0) == -180d);
                    assertTrue(env.getPos().get(0).getValue().get(1) ==  -90d);
                    assertTrue(env.getPos().get(1).getValue().get(0) ==  180d);
                    assertTrue(env.getPos().get(1).getValue().get(1) ==   90d);
                }
            }
        }
        if (layerTestFound == false) {
            throw new AssertionError("The layer \""+ LAYER_TEST +"\" was not found in the returned GetCapabilities.");
        }
        if (layerAliasFound == false) {
            throw new AssertionError("The layer \""+ LAYER_ALIAS +"\" was not found in the returned GetCapabilities.");
        }

        Get get = (Get) responseCaps.getCapability().getRequest().getGetCapabilities().getDCP().get(0).getHTTP().getRealGetOrPost().get(0);
        assertEquals("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?", get.getOnlineResource().getHref());

        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/test?SERVICE=WCS&" + WCS_GETCAPABILITIES2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WCSCapabilitiesType.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WCSCapabilitiesType);

        responseCaps = (WCSCapabilitiesType)obj;

        get = (Get) responseCaps.getCapability().getRequest().getGetCapabilities().getDCP().get(0).getHTTP().getRealGetOrPost().get(0);
        assertEquals("http://localhost:"+ getCurrentPort() + "/WS/wcs/test?", get.getOnlineResource().getHref());


        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_GETCAPABILITIES);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WCSCapabilitiesType.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WCSCapabilitiesType);

        responseCaps = (WCSCapabilitiesType)obj;

        get = (Get) responseCaps.getCapability().getRequest().getGetCapabilities().getDCP().get(0).getHTTP().getRealGetOrPost().get(0);
        assertEquals("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?", get.getOnlineResource().getHref());
    }

    /**
     * Ensures that a valid DescribeCoverage request returns indeed a valid document.
     */
    @Test
    @Order(order=5)
    public void testWCSDescribeCoverage() throws Exception {
        initLayerList();
        // Creates a valid DescribeCoverage url.
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_DESCRIBECOVERAGE);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String result = getStringResponse(getCapsUrl);
        String expResult = getStringFromFile("org/constellation/ws/embedded/v100/describeCoveragePNG.xml");
        
        domCompare(result, expResult);

        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_DESCRIBECOVERAGE_ALIAS);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        result = getStringResponse(getCapsUrl);
        expResult = getStringFromFile("org/constellation/ws/embedded/v100/describeCoveragePNG_ALIAS.xml");
        
        domCompare(result, expResult);
        
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_DESCRIBECOVERAGE_TIFF);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        result = getStringResponse(getCapsUrl);
        expResult = getStringFromFile("org/constellation/ws/embedded/v100/describeCoverageTIFF.xml");
        
        domCompare(result, expResult);
        
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_DESCRIBECOVERAGE_201_PNG);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        result = getStringResponse(getCapsUrl);
        expResult = getStringFromFile("org/constellation/ws/embedded/v201/describeCoveragePNG.xml");
       
        domCompare(result, expResult);
        
        try {
            getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wcs/default?SERVICE=WCS&" + WCS_DESCRIBECOVERAGE_201_TIFF);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        result = getStringResponse(getCapsUrl);
        expResult = getStringFromFile("org/constellation/ws/embedded/v201/describeCoverageTIFF.xml");
        
        domCompare(result, expResult);

    }
    
    @Test
    @Order(order=6)
    public void listInstanceTest() throws Exception {
        initLayerList();
        
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wcs/all");

        URLConnection conec = liUrl.openConnection();

        Object obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.0.0", "1.1.1", "2.0.1");
        instances.add(new Instance(1, "default", "Web Coverage Server", "Constellation Web Coverage Server maintained by geomatys.", "wcs", versions, 3, ServiceStatus.STARTED, "null/wcs/default"));
        instances.add(new Instance(2, "test",    "Web Coverage Server", "Constellation Web Coverage Server maintained by geomatys.", "wcs", versions, 1, ServiceStatus.STARTED, "null/wcs/test"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

    }

    protected static void domCompare(final Object actual, String expected) throws Exception {
        expected = expected.replace("EPSG_VERSION", EPSG_VERSION);
        domCompare(actual, expected, Arrays.asList("http://www.opengis.net/gml/3.2:id"));
    }
}
