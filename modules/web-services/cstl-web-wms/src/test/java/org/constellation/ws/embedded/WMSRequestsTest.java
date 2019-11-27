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

import org.constellation.dto.service.config.Languages;
import org.constellation.dto.service.config.Language;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.dto.portrayal.WMSPortrayal;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ProviderType;
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.constellation.test.ImageTesting;
import org.constellation.test.utils.Order;
import org.geotoolkit.image.io.plugin.WorldFileImageReader;
import org.geotoolkit.image.jai.Registry;
import org.geotoolkit.inspire.xml.vs.ExtendedCapabilitiesType;
import org.geotoolkit.inspire.xml.vs.LanguageType;
import org.geotoolkit.inspire.xml.vs.LanguagesType;
import org.geotoolkit.ogc.xml.exception.ServiceExceptionReport;
import org.geotoolkit.sld.xml.v110.DescribeLayerResponseType;
import org.geotoolkit.sld.xml.v110.LayerDescriptionType;
import org.geotoolkit.sld.xml.v110.TypeNameType;
import org.geotoolkit.util.NamesExt;
import org.geotoolkit.wms.xml.WMSMarshallerPool;
import org.geotoolkit.wms.xml.v111.LatLonBoundingBox;
import org.geotoolkit.wms.xml.v111.Layer;
import org.geotoolkit.wms.xml.v111.WMT_MS_Capabilities;
import org.geotoolkit.wms.xml.v130.WMSCapabilities;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

import javax.imageio.ImageIO;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.opengis.util.GenericName;
import org.apache.sis.util.logging.Logging;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.SimpleValue;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.ProviderParameters;
import org.constellation.provider.datastore.DataStoreProviderService;
import org.constellation.test.utils.CstlDOMComparator;
import org.constellation.test.utils.TestRunner;
import static org.constellation.ws.embedded.AbstractGrizzlyServer.getImageFromURL;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.test.Commons;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;


/**
 * A set of methods that request a Grizzly server which embeds a WMS service.
 *
 * @version $Id$
 *
 * @author Cédric Briançon (Geomatys)
 * @since 0.3
 */
@RunWith(TestRunner.class)
public class WMSRequestsTest extends AbstractGrizzlyServer {

    /**
     * The layer to test.
     */
    private static final GenericName LAYER_TEST = NamesExt.create("SSTMDE200305");

     /**
     * Checksum value on the returned image expressed in a geographic CRS for the SST_tests layer.
     */
    private Long sstChecksumGeo = null;

    private static final String EPSG_VERSION = CRS.getVersion("EPSG").toString();

    /**
     * URLs which will be tested on the server.
     */
    private static final String WMS_GETCAPABILITIES ="request=GetCapabilities&service=WMS&version=1.1.1";

    private static final String WMS_GETCAPABILITIES_WMS1_111 ="request=GetCapabilities&service=WMS&version=1.1.1";

    private static final String WMS_GETCAPABILITIES_WMS1 ="request=GetCapabilities&service=WMS&version=1.3.0";

    private static final String WMS_GETCAPABILITIES_WMS1_FRE ="request=GetCapabilities&service=WMS&version=1.3.0&language=fre";

    private static final String WMS_GETCAPABILITIES_WMS1_ENG ="request=GetCapabilities&service=WMS&version=1.3.0&language=eng";

    private static final String WMS_FALSE_REQUEST ="request=SomethingElse";

    private static final String WMS_GETMAP ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=";

    private static final String WMS_GETMAP_BAD_HEIGHT ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=0&" +
                                      "srs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=";

    private static final String WMS_GETMAP_BAD_WIDTH ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=-1&height=512&" +
                                      "srs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=";


    private static final String WMS_GETMAP_LAYER_LIMIT ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "styles=&layers=";

    private static final String WMS_GETFEATUREINFO ="request=GetFeatureInfo&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=&" +
                                      "query_layers="+ LAYER_TEST +"&info_format=text/plain&" +
                                      "X=300&Y=200";

    private static final String WMS_GETFEATUREINFO2 ="request=GetFeatureInfo&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=200&height=100&" +
                                      "srs=CRS:84&BbOx=0,-0.0020,0.0040,0&" +
                                      "layers=Lakes&styles=&" +
                                      "query_layers=Lakes&info_format=text/plain&" +
                                      "X=60&Y=60";

    private static final String WMS_GETFEATUREINFO3 = "QuErY_LaYeRs=BasicPolygons&I=50&"
                                                    + "LaYeRs=BasicPolygons&StYlEs=&WiDtH=100&CrS=CRS:84&"
                                                    + "ReQuEsT=GetFeatureInfo&InFo_fOrMaT=text/plain&BbOx=-2,2,2,6"
                                                    + "&HeIgHt=100&J=50&VeRsIoN=1.3.0&FoRmAt=image/gif";

    private static final String WMS_GETFEATUREINFO4 ="QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
                                                    + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
                                                    + "VeRsIoN=1.1.1&InFo_fOrMaT=application/vnd.ogc.gml&"
                                                    + "X=60&StYlEs=&LaYeRs=Lakes&"
                                                    + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    private static final String WMS_GETFEATUREINFO5 ="QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
                                                    + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
                                                    + "VeRsIoN=1.1.1&InFo_fOrMaT=text/html&"
                                                    + "X=60&StYlEs=&LaYeRs=Lakes&"
                                                    + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    private static final String WMS_GETFEATUREINFO6 ="QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
                                                    + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
                                                    + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
                                                    + "X=60&StYlEs=&LaYeRs=Lakes&"
                                                    + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    private static final String WMS_GETFEATUREINFO7 ="QuErY_LaYeRs="+ LAYER_TEST +"&BbOx=0,-0.0020,0.0040,0&"
                                                    + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
                                                    + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json;%20subtype=profile&"
                                                    + "X=60&StYlEs=&LaYeRs="+ LAYER_TEST +"&"
                                                    + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&PROFILE=LINESTRING(-61.132875680921%2014.81104016304,%20-60.973573923109%2014.673711061478,%20-60.946108102796%2014.706670045853,%20-60.915895700453%2014.610539674759,%20-60.882936716078%2014.48145031929)";


    private static final String WMS_GETLEGENDGRAPHIC = "request=GetLegendGraphic&service=wms&" +
            "width=200&height=40&layer="+ LAYER_TEST +"&format=image/png&version=1.1.0";

    private static final String WMS_DESCRIBELAYER ="request=DescribeLayer&service=WMS&" +
            "version=1.1.1&layers="+ LAYER_TEST;

    private static final String WMS_GETMAP2 =
    "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/png&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_BMP =
    "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/bmp&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_JPEG =
    "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/jpeg&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_BMP_111 =
    "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/bmp&ReQuEsT=GetMap&StYlEs=&SrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.1.1&WiDtH=100";

    private static final String WMS_GETMAP_PPM =
    "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/x-portable-pixmap&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_GIF =
    "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/gif&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_GIF_UNVALID_LAYER =
    "TrAnSpArEnT=False&HeIgHt=100&LaYeRs=unknownlayer&FoRmAt=image/gif&ReQuEsT=GetMap&StYlEs=&srS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.1.1&WiDtH=100&EXCEPTIONS=application/vnd.ogc.se_inimage";

    private static final String WMS_GETMAP_GIF_TRANSPARENT =
    "TrAnSpArEnT=TRUE&CrS=CRS:84&FoRmAt=image%2Fgif&VeRsIoN=1.3.0&HeIgHt=100&WiDtH=200&StYlEs=&LaYeRs=Lakes&ReQuEsT=GetMap&BbOx=0,-0.0020,0.0040,0";

    private static final String WMS_GETMAP_111_PROJ ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=EPSG:3395&bbox=-19000000,-19000000,19000000,19000000&" +
                                      "layers="+ LAYER_TEST +"&styles=";
    private static final String WMS_GETMAP_130_PROJ ="request=GetMap&service=WMS&version=1.3.0&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "crs=EPSG:3395&bbox=-19000000,-19000000,19000000,19000000&" +
                                      "layers="+ LAYER_TEST +"&styles=";
    private static final String WMS_GETMAP_111_GEO ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=EPSG:4022&bbox=-90,-180,90,180&" +
                                      "layers="+ LAYER_TEST +"&styles=";
    private static final String WMS_GETMAP_111_EPSG_4326 ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=EPSG:4326&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=";
    private static final String WMS_GETMAP_130_EPSG_4326 ="request=GetMap&service=WMS&version=1.3.0&" +
                                      "format=image/png&width=512&height=1024&" +
                                      "crs=EPSG:4326&bbox=-90,-180,90,180&" +
                                      "layers="+ LAYER_TEST +"&styles=";
    private static final String WMS_GETMAP_111_CRS_84 ="request=GetMap&service=WMS&version=1.1.1&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "srs=CRS:84&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=";
    private static final String WMS_GETMAP_130_CRS_84 ="request=GetMap&service=WMS&version=1.3.0&" +
                                      "format=image/png&width=1024&height=512&" +
                                      "crs=CRS:84&bbox=-180,-90,180,90&" +
                                      "layers="+ LAYER_TEST +"&styles=";

    private static boolean initialized = false;

    @BeforeClass
    public static void startup() {
        ConfigDirectory.setupTestEnvironement("WMSRequestTest");
        controllerConfiguration = WMSControllerConfig.class;
    }

    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    public void initLayerList() {
        pool = WMSMarshallerPool.getInstance();
        if (!initialized) {
            try {
                startServer(null);

                try {
                    layerBusiness.removeAll();
                    serviceBusiness.deleteAll();
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {}

                // coverage-file datastore
                final File rootDir                   = AbstractGrizzlyServer.initDataDirectory();
                final DataProviderFactory covFilefactory = DataProviders.getFactory("data-store");
                final ParameterValueGroup sourceCF   = covFilefactory.getProviderDescriptor().createValue();
                sourceCF.parameter("id").setValue("coverageTestSrc");
                final ParameterValueGroup choice3 = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourceCF);

                final ParameterValueGroup srcCFConfig = choice3.addGroup("FileCoverageStoreParameters");

                srcCFConfig.parameter("path").setValue(new URL("file:" + rootDir.getAbsolutePath() + "/org/constellation/data/image/SSTMDE200305.png"));
                srcCFConfig.parameter("type").setValue("AUTO");

                providerBusiness.storeProvider("coverageTestSrc", null, ProviderType.LAYER, "data-store", sourceCF);
                dataBusiness.create(new QName("SSTMDE200305"), "coverageTestSrc", "COVERAGE", false, true, null, null);


                final DataProviderFactory ffactory = DataProviders.getFactory("data-store");
                final File outputDir = initDataDirectory();
                final ParameterValueGroup sourcef = ffactory.getProviderDescriptor().createValue();
                sourcef.parameter("id").setValue("shapeSrc");

                final ParameterValueGroup choice = ProviderParameters.getOrCreate(DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR, sourcef);
                final ParameterValueGroup shpconfig = choice.addGroup("ShapefileParametersFolder");
                String path;
                if (outputDir.getAbsolutePath().endsWith("org/constellation/ws/embedded/wms111/styles")) {
                    path = outputDir.getAbsolutePath().substring(0, outputDir.getAbsolutePath().indexOf("org/constellation/ws/embedded/wms111/styles"));
                } else {
                    path = outputDir.getAbsolutePath();
                }
                shpconfig.parameter("path").setValue(URI.create("file:"+path + "/org/constellation/ws/embedded/wms111/shapefiles"));

                providerBusiness.storeProvider("shapeSrc", null, ProviderType.LAYER, "data-store", sourcef);

                dataBusiness.create(new QName("http://www.opengis.net/gml", "BuildingCenters"), "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "BasicPolygons"),   "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Bridges"),         "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Streams"),         "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Lakes"),           "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "NamedPlaces"),     "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Buildings"),       "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "RoadSegments"),    "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "DividedRoutes"),   "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Forests"),         "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "MapNeatline"),     "shapeSrc", "VECTOR", false, true, null, null);
                dataBusiness.create(new QName("http://www.opengis.net/gml", "Ponds"),           "shapeSrc", "VECTOR", false, true, null, null);


                final LayerContext config = new LayerContext();
                config.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

                Integer defId =serviceBusiness.create("wms", "default", config, null, null);
                final Details details = serviceBusiness.getInstanceDetails("wms", "default", "eng");
                details.getServiceConstraints().setLayerLimit(100);
                serviceBusiness.setInstanceDetails("wms", "default", details, "eng", true);

                layerBusiness.add("SSTMDE200305",                     null,           "coverageTestSrc",        null, "default", "wms", null);
                layerBusiness.add("BuildingCenters",     "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("BasicPolygons",       "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("Bridges",             "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("Streams",             "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("Lakes",               "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("NamedPlaces",         "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("Buildings",           "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("RoadSegments",        "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("DividedRoutes",       "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("Forests",             "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("MapNeatline",         "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);
                layerBusiness.add("Ponds",               "http://www.opengis.net/gml",       "shapeSrc",        null, "default", "wms", null);


                final LayerContext config2 = new LayerContext();
                config2.setSupportedLanguages(new Languages(Arrays.asList(new Language("fre"), new Language("eng", true))));
                config2.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

                Integer wm1Id = serviceBusiness.create("wms", "wms1", config2, null, null);
                layerBusiness.add("Lakes",    "http://www.opengis.net/gml", "shapeSrc",        null, "wms1", "wms", null);


                final Details serviceEng = new Details();
                serviceEng.setDescription("Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.");
                serviceEng.setIdentifier("wms1");
                serviceEng.setKeywords(Arrays.asList("WMS"));
                serviceEng.setName("this is the default english capabilities");
                final AccessConstraint cstr = new AccessConstraint("NONE", "NONE", 20, 1024, 1024);
                serviceEng.setServiceConstraints(cstr);
                final Contact ct = new Contact();
                serviceEng.setServiceContact(ct);
                serviceEng.setVersions(Arrays.asList("1.1.1", "1.3.0"));

                serviceBusiness.setInstanceDetails("wms", "wms1", serviceEng, "eng", true);
                //ConfigDirectory.writeServiceMetadata("wms1", "wms", serviceEng, "eng");

                final Details serviceFre = new Details();
                serviceFre.setDescription("Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.");
                serviceFre.setIdentifier("wms1");
                serviceFre.setKeywords(Arrays.asList("WMS"));
                serviceFre.setName("Ceci est le document capabilities français");
                serviceFre.setServiceConstraints(cstr);
                serviceFre.setServiceContact(ct);
                serviceFre.setVersions(Arrays.asList("1.1.1", "1.3.0"));

                serviceBusiness.setInstanceDetails("wms", "wms1", serviceFre, "fre", false);

                final LayerContext config3 = new LayerContext();
                config3.getCustomParameters().put("supported_versions", "1.3.0");
                config3.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());
                final Details details3 = new Details();
                details3.setIdentifier("wms2");
                details3.setName("wms2");
                details3.setVersions(Arrays.asList("1.3.0"));

                Integer wm2Id = serviceBusiness.create("wms", "wms2", config3, details3, null);
                layerBusiness.add("SSTMDE200305",                      null,          "coverageTestSrc",        null, "wms2", "wms", null);
                layerBusiness.add("BuildingCenters",     "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("BasicPolygons",       "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("Bridges",             "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("Streams",             "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("Lakes",               "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("NamedPlaces",         "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("Buildings",           "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("RoadSegments",        "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("DividedRoutes",       "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("Forests",             "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("MapNeatline",         "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);
                layerBusiness.add("Ponds",               "http://www.opengis.net/gml",       "shapeSrc",        null, "wms2", "wms", null);

                WorldFileImageReader.Spi.registerDefaults(null);
                WMSPortrayal.setEmptyExtension(true);

                //reset values, only allow pure java readers
                for(String jn : ImageIO.getReaderFormatNames()){
                    Registry.setNativeCodecAllowed(jn, ImageReaderSpi.class, false);
                }

                //reset values, only allow pure java writers
                for(String jn : ImageIO.getWriterFormatNames()){
                    Registry.setNativeCodecAllowed(jn, ImageWriterSpi.class, false);
                }

                final WMSPortrayal port = new WMSPortrayal();

                serviceBusiness.setExtraConfiguration("wms", "wms2", "WMSPortrayal.xml", port, GenericDatabaseMarshallerPool.getInstance());

                serviceBusiness.start(defId);
                serviceBusiness.start(wm1Id);
                serviceBusiness.start(wm2Id);
                waitForRestStart("wms","default");
                waitForRestStart("wms","wms1");
                waitForRestStart("wms","wms2");

                initialized = true;
            } catch (Exception ex) {
                Logging.getLogger("org.constellation.ws.embedded").log(Level.SEVERE, null, ex);
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
        } catch (Exception ex) {
            Logging.getLogger("org.constellation.ws.embedded").log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement("WMSRequestTest");
        stopServer();
    }

    /**
     * Ensure that a wrong value given in the request parameter for the WMS server
     * returned an error report for the user.
     */
    @Test
    @Order(order=1)
    public void testWMSWrongRequest() throws Exception {

        initLayerList();

        // Creates an intentional wrong url, regarding the WMS version 1.1.1 standard
        final URL wrongUrl;
        try {
            wrongUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_FALSE_REQUEST);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a ServiceExceptionReport.
        final Object obj = unmarshallResponse(wrongUrl);
        assertTrue(obj instanceof ServiceExceptionReport);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=2)
    public void testWMSGetMap() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 8);

        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BAD_HEIGHT);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        String obj = getStringResponse(getMapUrl);
        assertTrue("was " + obj, obj.contains("InvalidDimensionValue"));


        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BAD_WIDTH);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        obj = getStringResponse(getMapUrl);
        assertTrue("was " + obj, obj.contains("InvalidDimensionValue"));
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=3)
    public void testWMSGetMapLakeGif() throws Exception {
        initLayerList();
         // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_GIF);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/gif");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=4)
    public void testWMSGetMapLakeGifransparent() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_GIF_TRANSPARENT);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/gif");

        // Test on the returned image.
        assertEquals(200, image.getWidth());
        assertEquals(100,  image.getHeight());
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=5)
    public void testWMSGetMapLakePng() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=6)
    public void testWMSGetMapLakeBmp() throws Exception {

        // Creates a valid GetMap url.
        URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BMP);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/bmp");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 2);

        // wms do not supported 1.1.1 request
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms2?" + WMS_GETMAP_BMP_111);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        Object obj = unmarshallResponse(getMapUrl);
        assertTrue(obj instanceof ServiceExceptionReport);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=7)
    public void testWMSGetMapLakeJpeg() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_JPEG);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/jpeg");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a {@link BufferedImage}.
     */
    @Test
    @Order(order=8)
    public void testWMSGetMapLakePpm() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_PPM);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/x-portable-pixmap");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that an exception is returned when requesting too many layers.
     */
    @Test
    @Order(order=9)
    public void testWMSGetMapLayerLimit() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            final StringBuilder sb = new StringBuilder();
            sb.append("http://localhost:").append(getCurrentPort()).append("/WS/wms/default?" + WMS_GETMAP_LAYER_LIMIT);
            sb.append(LAYER_TEST);
            for (int i=0;i<120;i++) {
                sb.append(',').append(LAYER_TEST);
            }
            getMapUrl = new URL(sb.toString());
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        try{
            final BufferedImage image = getImageFromURL(getMapUrl, "image/png");
            Assert.fail("Service should have raised an error");
        }catch(Exception ex){
            //ok
        }

    }

    /**
     * Ensures that an error is returned in image as gif and
     * is not all black.
     */
    @Test
    @Order(order=10)
    public void testWMSGetMapErrorInImageGif() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_GIF_UNVALID_LAYER);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/gif");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) == 2);
        assertFalse (ImageTesting.hasTransparency(image));

    }

    /**
     * Ensures that a valid GetCapabilities request returns indeed a valid GetCapabilities
     * document representing the server capabilities in the WMS version 1.1.1/ 1.3.0 standard.
     */
    @Test
    @Order(order=11)
    public void testWMSGetCapabilities() throws JAXBException, Exception {

        // Creates a valid GetCapabilities url.
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETCAPABILITIES);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue("was:" + obj, obj instanceof WMT_MS_Capabilities);
        WMT_MS_Capabilities responseCaps = (WMT_MS_Capabilities)obj;

        Layer layer = (Layer) responseCaps.getLayerFromName(LAYER_TEST.tip().toString());

        assertNotNull(layer);
        assertEquals("EPSG:4326", layer.getSRS().get(0));
        final LatLonBoundingBox bboxGeo = (LatLonBoundingBox) layer.getLatLonBoundingBox();
        assertTrue(bboxGeo.getWestBoundLongitude() == -180d);
        assertTrue(bboxGeo.getSouthBoundLatitude() ==  -90d);
        assertTrue(bboxGeo.getEastBoundLongitude() ==  180d);
        assertTrue(bboxGeo.getNorthBoundLatitude() ==   90d);


        String currentUrl = responseCaps.getCapability().getRequest().getGetMap().getDCPType().get(0).getHTTP().getGet().getOnlineResource().getHref();

        assertEquals("http://localhost:" + getCurrentPort() + "/WS/wms/default?", currentUrl);

        // Creates a valid GetCapabilities url.
        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETCAPABILITIES_WMS1_111);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMT_MS_Capabilities);

        responseCaps = (WMT_MS_Capabilities) obj;

        // The layer test must be excluded
        layer = (Layer) responseCaps.getLayerFromName(LAYER_TEST.tip().toString());
        assertNull(layer);


        // The layer lake must be included
        layer = (Layer) responseCaps.getLayerFromName("http://www.opengis.net/gml:Lakes");
        assertNotNull(layer);

        currentUrl = responseCaps.getCapability().getRequest().getGetMap().getDCPType().get(0).getHTTP().getGet().getOnlineResource().getHref();

        assertEquals("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?", currentUrl);

        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETCAPABILITIES);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMT_MS_Capabilities);
        responseCaps = (WMT_MS_Capabilities) obj;

        currentUrl = responseCaps.getCapability().getRequest().getGetMap().getDCPType().get(0).getHTTP().getGet().getOnlineResource().getHref();

        assertEquals("http://localhost:" + getCurrentPort() + "/WS/wms/default?", currentUrl);


        // Creates a valid GetCapabilities url.
        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms2?" + WMS_GETCAPABILITIES_WMS1_111);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        //the service WMS2 does not support 1.1.0 version
        obj = unmarshallResponse(getCapsUrl);
        assertTrue("was :" + obj.getClass().getName(), obj instanceof WMSCapabilities);

         // Creates a valid GetCapabilities url.
        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms2?" + WMS_GETCAPABILITIES_WMS1);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);
    }

    @Test
    @Order(order=12)
    public void testWMSGetCapabilitiesLanguage() throws JAXBException, Exception {
        pool = WMSMarshallerPool.getInstance();

         // Creates a valid GetMap url.
        URL getCapsUrl;
        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" +WMS_GETCAPABILITIES_WMS1);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);

        WMSCapabilities responseCaps130 = (WMSCapabilities)obj;
        ExtendedCapabilitiesType  ext = responseCaps130.getCapability().getInspireExtendedCapabilities();
        assertEquals("eng", ext.getCurrentLanguage());

        LanguageType l1 = new LanguageType("fre", false);
        LanguageType l2 = new LanguageType("eng", true);
        LanguagesType languages = new LanguagesType(Arrays.asList(l1, l2));
        assertEquals(ext.getLanguages(), languages);

        assertEquals("this is the default english capabilities", responseCaps130.getService().getName());

        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" +WMS_GETCAPABILITIES_WMS1_ENG);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);

        responseCaps130 = (WMSCapabilities)obj;
        ext = responseCaps130.getCapability().getInspireExtendedCapabilities();
        assertEquals("eng", ext.getCurrentLanguage());
        assertEquals(ext.getLanguages(), languages);

        assertEquals("this is the default english capabilities", responseCaps130.getService().getName());

        try {
            getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" +WMS_GETCAPABILITIES_WMS1_FRE);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);

        responseCaps130 = (WMSCapabilities)obj;
        ext = responseCaps130.getCapability().getInspireExtendedCapabilities();
        assertEquals("fre", ext.getCurrentLanguage());
        assertEquals(ext.getLanguages(), languages);

        assertEquals("Ceci est le document capabilities français", responseCaps130.getService().getName());

    }

    /**
     * Ensures that the {@code WMS GetFeatureInfo} request on a particular point of the
     * testing layer produces the wanted result.
     * @throws java.io.Exception
     */
    @Test
    @Order(order=13)
    public void testWMSGetFeatureInfoPlainCoveragePng() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult = "SSTMDE200305\n" +
                           "0;\n"+
                           "0.0;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);

        // the result here is probably false
        // now i get 0.0 here ? assertTrue   (value.startsWith("210.0")); // I d'ont know why but before the test was => (value.startsWith("28.5"));
    }

    /**
     * I don't know why this test do not work
     * @throws Exception
     */
    @Test
    @Order(order=14)
    public void testWMSGetFeatureInfoPlainShapePng() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult =
                "Lakes\n" +
                "sis:envelope:Operation;sis:geometry:Operation;sis:identifier:String;the_geom:MultiPolygon;FID:String;NAME:String;\n" +
                "BOX(6.0E-4 -0.0018, 0.0031 -1.0E-4);MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)));Lakes.1;MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)));101;Blue Lake;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=15)
    public void testWMSGetFeatureInfoPlainShapeGif() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO3);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult =
                "BasicPolygons\n" +
                "sis:envelope:Operation;sis:geometry:Operation;sis:identifier:String;the_geom:MultiPolygon;ID:String;\n" +
                "BOX(-2 3, 1 6);MULTIPOLYGON (((-2 6, 1 6, 1 3, -2 3, -2 6)));BasicPolygons.2;MULTIPOLYGON (((-2 6, 1 6, 1 3, -2 3, -2 6)));;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=16)
    public void testWMSGetFeatureInfoGMLShapeGif() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO4);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<msGMLOutput xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "<Lakes_layer>\n" +
                "	<Lakes_feature>\n" +
                "		<ID>Lakes.1</ID>\n" +
                "		<identifier>Lakes.1</identifier>\n" +
                "		<the_geom>\n" +
                "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))		</the_geom>\n" +
                "		<FID>101</FID>\n" +
                "		<NAME>Blue Lake</NAME>\n" +
                "	</Lakes_feature>\n" +
                "</Lakes_layer>\n" +
                "</msGMLOutput>";

        String result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);
    }

    /**
     * Ensures that a valid GetLegendGraphic request returns indeed a {@link BufferedImage}.
     *
     * @throws java.io.Exception
     */
    @Test
    public void testWMSGetLegendGraphic() throws Exception {
        initLayerList();
        // Creates a valid GetLegendGraphic url.
        final URL getLegendUrl;
        try {
            getLegendUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETLEGENDGRAPHIC);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getLegendUrl, "image/png");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(200, image.getWidth());
        assertEquals(40,  image.getHeight());
    }

    /**
     * Ensures that a valid DescribeLayer request produces a valid document.
     */
    @Test
    @Order(order=18)
    public void testWMSDescribeLayer() throws JAXBException, Exception {

        // Creates a valid DescribeLayer url.
        final URL describeUrl;
        try {
            describeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_DESCRIBELAYER);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        final Object obj = unmarshallResponse(describeUrl);
        assertTrue(obj instanceof DescribeLayerResponseType);

        // Tests on the response
        final DescribeLayerResponseType desc = (DescribeLayerResponseType)obj;
        final List<LayerDescriptionType> layerDescs = desc.getLayerDescription();
        assertFalse(layerDescs.isEmpty());
        final List<TypeNameType> typeNames = layerDescs.get(0).getTypeName();
        assertFalse(typeNames.isEmpty());
        final GenericName name = NamesExt.create(typeNames.get(0).getCoverageName());
        assertEquals(LAYER_TEST, name);
    }


    @Test
    @Order(order=19)
    public void testWMSGetMapLakePostKvp() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("HeIgHt","100");
        parameters.put("LaYeRs","Lakes");
        parameters.put("FoRmAt","image/png");
        parameters.put("ReQuEsT","GetMap");
        parameters.put("StYlEs","");
        parameters.put("CrS","CRS:84");
        parameters.put("BbOx","-0.0025,-0.0025,0.0025,0.0025");
        parameters.put("VeRsIoN","1.3.0");
        parameters.put("WiDtH","100");

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromPostKvp(getMapUrl, parameters, "image/png");

        // Test on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100,  image.getHeight());
        assertTrue  (ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a GetMap in version 1.1.1 on a projected
     * {@linkplain CoordinateReferenceSystem CRS} provides the same image that a GetMap
     * in version 1.3.0 on the same CRS.
     */
    @Ignore
    @Order(order = 20)
    public void testGetMap111And130Projected() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        final URL getMap111Url, getMap130Url;
        try {
            getMap111Url = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_PROJ);
            getMap130Url = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_PROJ);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image111 = getImageFromURL(getMap111Url, "image/png");
        final BufferedImage image130 = getImageFromURL(getMap130Url, "image/png");

        // Tests on the returned images.
        assertTrue  (!(ImageTesting.isImageEmpty(image111)));
        assertEquals(1024, image111.getWidth());
        assertEquals(512,  image111.getHeight());
        assertTrue  (!(ImageTesting.isImageEmpty(image130)));
        assertEquals(1024, image130.getWidth());
        assertEquals(512,  image130.getHeight());
        assertEquals(Commons.checksum(image111), Commons.checksum(image130));
    }

    /**
     * Test a GetMap request on a WMS version 1.1.1 for a geographical
     * {@linkplain CoordinateReferenceSystem CRS}.
     *
     * TODO: fix the implementation of the GetMap request concerning the handling of
     *       geographical CRS (not WGS84) and do this test then.
     */
    @Ignore
    @Order(order = 21)
    public void testCRSGeographique111() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_GEO);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
    }

    /**
     * Verify the axis order for a GetMap in version 1.1.1 for the {@code WGS84} CRS.
     */
    @Test
    @Order(order = 22)
    public void testGetMap111Epsg4326() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_EPSG_4326);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    /**
     * Verify the axis order for a GetMap in version 1.3.0 for the {@code WGS84} CRS.
     */
    @Test
    @Order(order = 23)
    public void testGetMap130Epsg4326() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_EPSG_4326);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(512, image.getWidth());
        assertEquals(1024,  image.getHeight());
        if (sstChecksumGeo == null) {
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertTrue(sstChecksumGeo.longValue() != Commons.checksum(image));
        }
    }

    /**
     * Verify the axis order for a GetMap in version 1.1.1 for the {@code WGS84} CRS.
     */
    @Test
    @Order(order = 24)
    public void testGetMap111Crs84() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_CRS_84);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    /**
     * Verify the axis order for a GetMap in version 1.3.0 for the {@code WGS84} CRS.
     *
     * TODO: fix the implementation of the GetMap request concerning the axes order,
     *       and do this test then.
     */
    @Test
    @Order(order = 25)
    public void testGetMap130Crs84() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl;
        try {
            getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_CRS_84);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue  (!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512,  image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    @Test
    @Order(order=26)
    public void testWMSGetFeatureInfoHTMLShape() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO5);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult =
                "<html>\n" +
                "    <head>\n" +
                "        <title>GetFeatureInfo HTML output</title>\n" +
                "    </head>\n" +
                "    <style>\n" +
                "ul{\n" +
                "               margin-top: 0;\n" +
                "               margin-bottom: 0px;\n" +
                "           }\n" +
                "           .left-part{\n" +
                "               display:inline-block;\n" +
                "               width:350px;\n" +
                "               overflow:auto;\n" +
                "               white-space:nowrap;\n" +
                "           }\n" +
                "           .right-part{\n" +
                "               display:inline-block;\n" +
                "               width:600px;\n" +
                "               overflow: hidden;\n" +
                "           }\n" +
                "           .values{\n" +
                "               text-overflow: ellipsis;\n" +
                "               white-space:nowrap;\n" +
                "               display:block;\n" +
                "               overflow: hidden;\n" +
                "           }    </style>\n" +
                "    <body>\n" +
                "<h2>Lakes</h2><br/><h2>Lakes.1</h2></br><div><div class=\"left-part\"><ul>\n" +
                "<li>\n" +
                "envelope</li>\n" +
                "<li>\n" +
                "geometry</li>\n" +
                "<li>\n" +
                "identifier</li>\n" +
                "<li>\n" +
                "the_geom</li>\n" +
                "<li>\n" +
                "FID</li>\n" +
                "<li>\n" +
                "NAME</li>\n" +
                "</ul>\n" +
                "</div><div class=\"right-part\"><a class=\"values\" title=\"BOX(6.0E-4 -0.0018, 0.0031 -1.0E-4)\">BOX(6.0E-4 -0.0018, 0.0031 -1.0E-4)</a><a class=\"values\" title=\"MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))\">MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))</a><a class=\"values\" title=\"Lakes.1\">Lakes.1</a><a class=\"values\" title=\"MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))\">MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))</a><a class=\"values\" title=\"101\">101</a><a class=\"values\" title=\"Blue Lake\">Blue Lake</a></div></div><br/>    </body>\n" +
                "</html>";

        String result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=27)
    public void testWMSGetFeatureInfoJSONShape() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO6);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult =
                "{\"Layer\":\"Lakes\",\"Name\":\"Lakes\",\"ID\":\"Lakes.1\",\"feature\":{\"identifier\":\"Lakes.1\",\"the_geom\":\"MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))\",\"FID\":\"101\",\"NAME\":\"Blue Lake\"}}";

        String result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=28)
    public void testWMSGetFeatureInfoJSONProfileShape() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi;
        try {
            gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO7);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        String expResult =
                "{\"layers\":[{\"name\":\"SSTMDE200305\",\"data\":[{\"unit\":null,\"min\":0.0,\"max\":0.0,\"points\":[{\"x\":3.8516484760372463E-13,\"y\":0.0},{\"x\":22.915557595600948,\"y\":0.0},{\"x\":22.915557595601836,\"y\":0.0},{\"x\":27.611269657740863,\"y\":0.0},{\"x\":27.611269657741225,\"y\":0.0},{\"x\":38.734410245335305,\"y\":0.0},{\"x\":38.73441024533549,\"y\":0.0},{\"x\":51.33753842087088,\"y\":0.0},{\"x\":53.45249659867336,\"y\":0.0}]}],\"message\":null}]}";

        String result = getStringResponse(gfi);
        System.out.println(result);
        assertNotNull(result);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order=29)
    public void testNewInstance() throws Exception {
        initLayerList();

        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we build a new instance
         */
        URL niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms");


        // for a POST request
        URLConnection conec = niUrl.openConnection();

        final Details meta = new Details();
        meta.setIdentifier("wms3");
        meta.setName("OGC:WMS");
        meta.setDescription("Constellation Map Server");
        meta.setVersions(Arrays.asList( "1.3.0", "1.1.1"));
        putJsonRequestObject(conec, meta);
        Object obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "WMS service \"wms3\" successfully created.");
        assertEquals(expResult, obj);

        /*
         * we see the instance with a status NOT_STARTED
         */
        URL liUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/all");


        // for a POST request
        conec = liUrl.openConnection();

        obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList( "1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList( "1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, 13, ServiceStatus.STARTED));
        instances.add(new Instance(2, "wms1",    "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1,  ServiceStatus.STARTED));
        instances.add(new Instance(3, "wms2",    "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED));
        instances.add(new Instance(4, "wms3",    "OGC:WMS", "Constellation Map Server", "wms", versions, 0,  ServiceStatus.STOPPED));
        InstanceReport expResult2 = new InstanceReport(instances);
        expResult2.equals(obj);
        assertEquals(expResult2, obj);

        /*
         * if we want to build the same new instance we receive an error
         */

        // for a POST request
        conec = niUrl.openConnection();
        putJsonRequestObject(conec, meta);
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        expResult = new AcknowlegementType("Failure", "Instance already created");
        assertEquals(expResult, obj);
    }

    @Test
    @Order(order=30)
    public void testStartInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we start the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/wms3/start");


        // for a POST request
        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallJsonResponsePost(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "WMS service \"wms3\" successfully started.");
        assertEquals(expResult, obj);

         /*
         * we verify tat the instance has now a status WORKING
         */
        URL liUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/all");


        // for a POST request
        conec = liUrl.openConnection();

        obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList( "1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList( "1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, 13, ServiceStatus.STARTED));
        instances.add(new Instance(2, "wms1",    "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1,  ServiceStatus.STARTED));
        instances.add(new Instance(3, "wms2",    "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED));
        instances.add(new Instance(4, "wms3",    "OGC:WMS", "Constellation Map Server", "wms", versions, 0,  ServiceStatus.STARTED));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

    }

    @Ignore
    @Order(order=31)
    public void testConfigureInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we configure the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/wms3/config");


        // for a POST request
        URLConnection conec = niUrl.openConnection();
        LayerContext layerContext = new LayerContext();

        postRequestObject(conec, layerContext);
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "Service instance configuration successfully updated.");
        assertEquals(expResult, obj);

        /*
         * we restart the instance to take change in count
         */
        niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/wms3/restart");


        // for a POST request
        conec = niUrl.openConnection();
        postRequestObject(conec, new SimpleValue(false), GenericDatabaseMarshallerPool.getInstance());
        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof AcknowlegementType);

        expResult = new AcknowlegementType("Success", "Service instance successfully restarted.");
        assertEquals(expResult, obj);

        URL gcDefaultURL = new URL("http://localhost:" +  getCurrentPort() + "/WS/wms/default?request=GetCapabilities&service=WMS&version=1.1.1");
        URL gcWms2URL    = new URL("http://localhost:" +  getCurrentPort() + "/WS/wms/wms3?request=GetCapabilities&service=WMS&version=1.1.1");

        String expCapabiliites = getStringResponse(gcDefaultURL.openConnection());
        String resCapabiliites = getStringResponse(gcWms2URL.openConnection());

        resCapabiliites = resCapabiliites.replace("wms3", "default");

        assertEquals(expCapabiliites, resCapabiliites);
    }

    @Test
    @Order(order=32)
    public void testStopInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we stop the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/wms3/stop");


        // for a POST request
        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallJsonResponsePost(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "WMS service \"wms3\" successfully stopped.");
        assertEquals(expResult, obj);

         /*
         * we see the instance has now a status NOT_STARTED
         */
        URL liUrl = new URL("http://localhost:" +  getCurrentPort() + "/API/OGC/wms/all");
        waitForRestStart(liUrl.toString());


        // for a POST request
        conec = liUrl.openConnection();

        obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList( "1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList( "1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, 13, ServiceStatus.STARTED));
        instances.add(new Instance(2, "wms1",    "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1,  ServiceStatus.STARTED));
        instances.add(new Instance(3, "wms2",    "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED));
        instances.add(new Instance(4, "wms3",    "OGC:WMS", "Constellation Map Server", "wms", versions, 0,  ServiceStatus.STOPPED));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);
    }

    @Test
    @Order(order=33)
    public void testDeleteInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we stop the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/wms3");


        // for a POST request
        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallJsonResponseDelete(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "WMS service \"wms3\" successfully deleted.");
        assertEquals(expResult, obj);

         /*
         * we see the instance has now a status NOT_STARTED
         */
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/all");
        waitForRestStart(liUrl.toString());


        // for a POST request
        conec = liUrl.openConnection();

        obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList( "1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList( "1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, 13, ServiceStatus.STARTED));
        instances.add(new Instance(2, "wms1",    "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1,  ServiceStatus.STARTED));
        instances.add(new Instance(3, "wms2",    "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);
    }

    public static void domCompare(final Object expected, String actual) throws Exception {

        String expectedStr;
        if (expected instanceof Path) {
            expectedStr = IOUtilities.toString((Path)expected);
        } else {
            expectedStr = (String) expected;
        }
        expectedStr = expectedStr.replace("EPSG_VERSION", EPSG_VERSION);

        final CstlDOMComparator comparator = new CstlDOMComparator(expectedStr, actual);
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add("http://www.w3.org/2001/XMLSchema-instance:schemaLocation");
        comparator.compare();
    }
}
