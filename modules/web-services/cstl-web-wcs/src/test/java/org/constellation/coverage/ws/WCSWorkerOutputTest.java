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
package org.constellation.coverage.ws;

import org.constellation.coverage.core.WCSWorker;
import org.constellation.coverage.core.DefaultWCSWorker;
import org.constellation.exception.ConfigurationException;
import org.constellation.test.SpringContextTest;
import org.constellation.ws.CstlServiceException;
import org.constellation.ws.MimeType;
import org.geotoolkit.gml.xml.v311.DirectPositionType;
import org.geotoolkit.gml.xml.v311.EnvelopeType;
import org.geotoolkit.gml.xml.v311.GridLimitsType;
import org.geotoolkit.gml.xml.v311.GridType;
import org.geotoolkit.gml.xml.v311.TimePositionType;
import org.geotoolkit.wcs.xml.DescribeCoverage;
import org.geotoolkit.wcs.xml.DescribeCoverageResponse;
import org.geotoolkit.wcs.xml.GetCapabilities;
import org.geotoolkit.wcs.xml.GetCapabilitiesResponse;
import org.geotoolkit.wcs.xml.GetCoverage;
import org.geotoolkit.wcs.xml.v100.CoverageDescription;
import org.geotoolkit.wcs.xml.v100.CoverageOfferingBriefType;
import org.geotoolkit.wcs.xml.v100.CoverageOfferingType;
import org.geotoolkit.wcs.xml.v100.DescribeCoverageType;
import org.geotoolkit.wcs.xml.v100.DomainSubsetType;
import org.geotoolkit.wcs.xml.v100.GetCapabilitiesType;
import org.geotoolkit.wcs.xml.v100.GetCoverageType;
import org.geotoolkit.wcs.xml.v100.OutputType;
import org.geotoolkit.wcs.xml.v100.SpatialDomainType;
import org.geotoolkit.wcs.xml.v100.SpatialSubsetType;
import org.geotoolkit.wcs.xml.v100.TimeSequenceType;
import org.geotoolkit.wcs.xml.v100.WCSCapabilitiesType;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import org.constellation.admin.SpringHelper;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.test.utils.TestEnvironment.ProviderImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.junit.AfterClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Testing class for WCS requests.
 *
 * @version $Id$
 * @author Cédric Briançon (Geomatys)
 *
 * @since 0.5
 */
public class WCSWorkerOutputTest extends SpringContextTest {

    private static final Logger LOGGER = Logger.getLogger("org.constellation.coverage.ws");
    /**
     * The layer to test.
     */
    private static final String LAYER_TEST = "SSTMDE200305";
    private static final String LAYER_ALIAS = "aliased";

    @Inject
    private IServiceBusiness serviceBusiness;
    @Inject
    private ILayerBusiness layerBusiness;
    @Inject
    private IProviderBusiness providerBusiness;
    @Inject
    private IDataBusiness dataBusiness;

    private static WCSWorker WORKER;
    private static boolean initialized = false;

    /**
     * Initialisation of the worker and the PostGRID data provider before launching
     * the different tests.
     */
    @PostConstruct
    public void setUpClass() {
        if (!initialized) {
            try {
                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                //Initialize geotoolkit
                ImageIO.scanForPlugins();
                org.geotoolkit.lang.Setup.initialize(null);

                ProviderImport pi = testResources.createProvider(TestResource.PNG, providerBusiness, null);
                Integer did = pi.datas.get(0).id;

                // second data for alias
                pi = testResources.createProvider(TestResource.PNG, providerBusiness, null);
                Integer did2 = pi.datas.get(0).id;

                final LayerContext config = new LayerContext();

                Integer sid = serviceBusiness.create("wcs", "default", config, null, null);
                layerBusiness.add(did,       null, null, "SSTMDE200305", null, sid, null);
                layerBusiness.add(did2, "aliased", null, "SSTMDE200305", null, sid, null);
                layerBusiness.add(did,  null,      "SST", "SSTMDE200305",  null, sid, null);


                WORKER = new DefaultWCSWorker("default");
                // Default instanciation of the worker' servlet context and uri context.
                WORKER.setServiceUrl("http://localhost:9090");
                initialized = true;

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

    }

    @AfterClass
    public static void tearDownClass() throws Exception {
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
        } catch (ConfigurationException ex) {
            Logger.getAnonymousLogger().log(Level.WARNING, ex.getMessage());
        }
        File derbyLog = new File("derby.log");
        if (derbyLog.exists()) {
            derbyLog.delete();
        }
    }

    /**
     * Ensures that a PostGRID layer preconfigured is found in the GetCapabilities document
     * returned by the {@link WCSWorker}.
     *
     * @throws JAXBException
     * @throws CstlServiceException
     */
    @Test
    public void testGetCapabilities() throws JAXBException, CstlServiceException {

        GetCapabilities request = new GetCapabilitiesType("1.0.0", "WCS", null, null);
        GetCapabilitiesResponse response = WORKER.getCapabilities(request);

        assertNotNull(response);
        assertTrue(response instanceof WCSCapabilitiesType);
        WCSCapabilitiesType getCaps = (WCSCapabilitiesType) response;


        // Verifies that the test layer is present into the GetCapabilities response.
        boolean find = false;
        boolean findA = false;
        final List<CoverageOfferingBriefType> offerings = getCaps.getContentMetadata().getCoverageOfferingBrief();
        assertFalse(offerings.isEmpty());
        for (CoverageOfferingBriefType offering : offerings) {
            for (JAXBElement<String> element : offering.getRest()) {
                if (element.getName().getLocalPart().equalsIgnoreCase("name")) {
                    if (element.getValue().equals(LAYER_TEST)) find = true;
                    else if (element.getValue().equals(LAYER_ALIAS)) findA = true;
                }
            }
        }
        // Not found in the list of coverage offerings, there is a mistake here.
        if (!find)  fail("Unable to find the layer "+ LAYER_TEST +" in the GetCapabilities document.");
        if (!findA) fail("Unable to find the layer "+ LAYER_ALIAS +" in the GetCapabilities document.");

        request = new GetCapabilitiesType("1.0.0", "WCS", "/WCS_Capabilities/Capability", null);
        getCaps = (WCSCapabilitiesType) WORKER.getCapabilities(request);

        assertNotNull(getCaps.getCapability());
        assertNull(getCaps.getContentMetadata());
        assertNull(getCaps.getService());

        request = new GetCapabilitiesType("1.0.0", "WCS", "/WCS_Capabilities/Service", null);
        getCaps = (WCSCapabilitiesType) WORKER.getCapabilities(request);

        assertNull(getCaps.getCapability());
        assertNull(getCaps.getContentMetadata());
        assertNotNull(getCaps.getService());

        request = new GetCapabilitiesType("1.0.0", "WCS", "/WCS_Capabilities/ContentMetadata", null);
        getCaps = (WCSCapabilitiesType) WORKER.getCapabilities(request);

        assertNull(getCaps.getCapability());
        assertNotNull(getCaps.getContentMetadata());
        assertNull(getCaps.getService());
    }

    /**
     * Ensures that a PostGRID layer preconfigured can be requested with a DescribeCoverage request,
     * and that the output document contains all data information.
     *
     * @throws JAXBException
     * @throws CstlServiceException
     */
    @Test
    public void testDescribeCoverage() throws JAXBException, CstlServiceException {

        final DescribeCoverage request = new DescribeCoverageType(LAYER_TEST);
        final DescribeCoverageResponse response = WORKER.describeCoverage(request);
        assertNotNull(response);
        assertTrue(response instanceof CoverageDescription);

        boolean find = false;
        boolean findA = false;

        final CoverageDescription descCov = (CoverageDescription) response;
        // Verifies that the test layer is present into the DescribeCoverage response.
        for (CoverageOfferingType offering : descCov.getCoverageOffering()) {
            for (JAXBElement<String> element : offering.getRest()) {
                if (element.getName().getLocalPart().equalsIgnoreCase("name")) {
                    if (element.getValue().equals(LAYER_TEST)) {

                        final SpatialDomainType spatialDomain = (SpatialDomainType) offering.getDomainSet()
                                .getContent().get(0).getValue();
                        final TimeSequenceType temporalDomain = (TimeSequenceType) offering.getDomainSet()
                                .getContent().get(1).getValue();
                        // Builds expected spatial domain
                        final List<DirectPositionType> pos = new ArrayList<>();
                        pos.add(new DirectPositionType(-180.0, -89.82421875));
                        pos.add(new DirectPositionType(180.0, 90.0));
                        final EnvelopeType expectedEnvelope = new EnvelopeType(pos, "urn:ogc:def:crs:EPSG::4326");
                        // Builds expected temporal domain
                        final List<TimePositionType> expectedTimes =
                                Collections.singletonList(new TimePositionType("2003-05-16T00:00:00Z"));
                        // Do assertions
                        assertEquals(expectedEnvelope, spatialDomain.getEnvelope());
                        // assertEquals(expectedTimes, temporalDomain.getTimePositionOrTimePeriod());
                        find = true;
                    }
                }
            }
        }
        if (!find)  fail("Unable to find the layer "+ LAYER_TEST +" in the DescribeCoverage document.");
    }

    @Test
    public void testDescribeCoverageAlias() throws JAXBException, CstlServiceException {

        final DescribeCoverage request = new DescribeCoverageType(LAYER_ALIAS);
        final DescribeCoverageResponse response = WORKER.describeCoverage(request);
        assertNotNull(response);
        assertTrue(response instanceof CoverageDescription);

        boolean find = false;
        boolean findA = false;

        final CoverageDescription descCov = (CoverageDescription) response;
        // Verifies that the test layer is present into the DescribeCoverage response.
        for (CoverageOfferingType offering : descCov.getCoverageOffering()) {
            for (JAXBElement<String> element : offering.getRest()) {
                if (element.getName().getLocalPart().equalsIgnoreCase("name")) {
                    if (element.getValue().equals(LAYER_ALIAS)) {

                        final SpatialDomainType spatialDomain = (SpatialDomainType) offering.getDomainSet()
                                .getContent().get(0).getValue();
                        final TimeSequenceType temporalDomain = (TimeSequenceType) offering.getDomainSet()
                                .getContent().get(1).getValue();
                        // Builds expected spatial domain
                        final List<DirectPositionType> pos = new ArrayList<>();
                        pos.add(new DirectPositionType(-180.0, -89.82421875));
                        pos.add(new DirectPositionType(180.0, 90.0));
                        final EnvelopeType expectedEnvelope = new EnvelopeType(pos, "urn:ogc:def:crs:EPSG::4326");
                        // Builds expected temporal domain
                        final List<TimePositionType> expectedTimes =
                                Collections.singletonList(new TimePositionType("2003-05-16T00:00:00Z"));
                        // Do assertions
                        assertEquals(expectedEnvelope, spatialDomain.getEnvelope());
                        // assertEquals(expectedTimes, temporalDomain.getTimePositionOrTimePeriod());
                        find = true;
                    }
                }
            }
        }
        if (!find)  fail("Unable to find the layer "+ LAYER_ALIAS +" in the DescribeCoverage document.");
    }

    /**
     * Ensures that a PostGRID layer preconfigured can be requested with a GetCoverage request.
     *
     * TODO: do a checksum on the output image.
     *
     * @throws JAXBException
     * @throws CstlServiceException
     */
    @Test
    public void testGetCoverage() throws JAXBException, CstlServiceException {

        // Builds the GetCoverage request
        final List<String> axis = new ArrayList<>();
        axis.add("width");
        axis.add("height");
        final long[] low  = new long[2];
        low[0] = 0L;
        low[1] = 0L;
        final long[] high = new long[2];
        high[0] = 1024L;
        high[1] = 512L;
        final GridLimitsType limits = new GridLimitsType(low, high);
        final GridType grid = new GridType(limits, axis);
        final List<DirectPositionType> pos = new ArrayList<>();
        pos.add(new DirectPositionType(-180.0, -90.0));
        pos.add(new DirectPositionType(180.0, 90.0));
        final EnvelopeType envelope = new EnvelopeType(pos, "CRS:84");
        final DomainSubsetType domain = new DomainSubsetType(null, new SpatialSubsetType(envelope, grid));
        GetCoverage request = new GetCoverageType(LAYER_TEST, domain, null, null, new OutputType(MimeType.IMAGE_PNG, "CRS:84"));

        // Finally execute the request on the worker.
        final RenderedImage image = (RenderedImage) WORKER.getCoverage(request);
        // Test on the returned image.
        assertEquals(image.getWidth(), 1024);
        assertEquals(image.getHeight(), 512);
        // Test the checksum of the image, if the image is indexed (and its values of type byte).
        // TODO: the image should have indexed colors. Find the origin of the conversion from
        //       indexed color to RGB (int values).
//        assertEquals(Commons.checksum(image), 3183786073L);


        request = new GetCoverageType(LAYER_TEST, domain, null, "WCS_INTERPLATION_METHOD_INVALID", new OutputType(MimeType.IMAGE_PNG, "CRS:84"));
        boolean exLaunched = false;
        try {
            WORKER.getCoverage(request);
        } catch (CstlServiceException ex) {
            exLaunched = true;
        }
        assertTrue(exLaunched);
    }
}
