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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.constellation.dto.contact.AccessConstraint;
import org.constellation.dto.contact.Contact;
import org.constellation.dto.contact.Details;
import org.constellation.map.featureinfo.FeatureInfoUtilities;
import org.constellation.test.ImageTesting;
import org.constellation.test.utils.Order;
import org.geotoolkit.inspire.xml.vs.ExtendedCapabilitiesType;
import org.geotoolkit.inspire.xml.vs.LanguageType;
import org.geotoolkit.inspire.xml.vs.LanguagesType;
import org.geotoolkit.ogc.xml.exception.ServiceExceptionReport;
import org.geotoolkit.sld.xml.v110.DescribeLayerResponseType;
import org.geotoolkit.sld.xml.v110.LayerDescriptionType;
import org.geotoolkit.sld.xml.v110.TypeNameType;
import org.geotoolkit.wms.xml.WMSMarshallerPool;
import org.geotoolkit.wms.xml.v111.LatLonBoundingBox;
import org.geotoolkit.wms.xml.v111.Layer;
import org.geotoolkit.wms.xml.v111.WMT_MS_Capabilities;
import org.geotoolkit.wms.xml.v130.WMSCapabilities;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import jakarta.xml.bind.JAXBException;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static java.lang.Double.NaN;
import java.net.URI;
import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.GeneralEnvelope;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.constellation.dto.AcknowlegementType;
import org.constellation.dto.Filter;
import org.constellation.dto.service.Instance;
import org.constellation.dto.service.InstanceReport;
import org.constellation.dto.service.ServiceStatus;
import org.constellation.dto.SimpleValue;
import org.constellation.dto.service.ServiceComplete;
import org.constellation.dto.service.config.wxs.DimensionDefinition;
import org.constellation.dto.service.config.wxs.LayerConfig;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.map.featureinfo.CoverageProfileInfoFormat;
import org.constellation.test.utils.CstlDOMComparator;
import org.constellation.test.utils.TestEnvironment;
import org.constellation.test.utils.TestEnvironment.DataImport;
import org.constellation.test.utils.TestEnvironment.TestResource;
import org.constellation.test.utils.TestEnvironment.TestResources;
import org.constellation.test.utils.TestRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.test.Commons;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.constellation.test.utils.TestEnvironment.initDataDirectory;
import static org.constellation.test.utils.TestResourceUtils.getResourceAsString;
import static org.geotoolkit.ogc.xml.OGCJAXBStatics.FILTER_COMPARISON_ISLESS;
import org.geotoolkit.wms.xml.v111.Dimension;
import org.geotoolkit.wms.xml.v111.Extent;

/**
 * A set of methods that request a SpringBoot server which embeds a WMS service.
 *
 * @version $Id$
 *
 * @author Guilhem Legal (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @since 0.3
 */
@RunWith(TestRunner.class)
public class WMSRequestsTest extends AbstractGrizzlyServer {

    /**
     * The layer to test.
     */
    private static final String LAYER_TEST = "SSTMDE200305";
    private static final String COV_ALIAS = "SST";
    private static final String MARTINIQUE = "martinique";
    private static final String JCOL_FILTER = "JCOLF";
    private static final String OM2_LAYER = "SamplingPoint";
    private static final String LAKES = "Lakes";
    private static final String COUNTRIES = "Countries";

    /**
     * Checksum value on the returned image expressed in a geographic CRS for
     * the SST_tests layer.
     */
    private Long sstChecksumGeo = null;

    private static final String EPSG_VERSION = CRS.getVersion("EPSG").toString();

    /**
     * URLs which will be tested on the server.
     */
    private static final String WMS_GETCAPABILITIES_111 = "request=GetCapabilities&service=WMS&version=1.1.1";

    private static final String WMS_GETCAPABILITIES_130 = "request=GetCapabilities&service=WMS&version=1.3.0";

    private static final String WMS_GETCAPABILITIES_130_FRE = "request=GetCapabilities&service=WMS&version=1.3.0&language=fre";

    private static final String WMS_GETCAPABILITIES_130_ENG = "request=GetCapabilities&service=WMS&version=1.3.0&language=eng";

    private static final String WMS_FALSE_REQUEST = "request=SomethingElse";

    private static final String WMS_GETMAP = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:4326&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_COV_JPEG = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/jpeg&width=1024&height=512&"
            + "srs=EPSG:4326&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_ANTI_MERI_CROSS = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:4326&bbox=110,-90,-60,90&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_ANTI_MERI_CROSS_MERC = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:3857&bbox=11368937.8390,-5009377.0857,-12092949.3709,9764371.7413&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_BAD_HEIGHT = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=0&"
            + "srs=EPSG:4326&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_BAD_WIDTH = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=-1&height=512&"
            + "srs=EPSG:4326&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_LAYER_LIMIT = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:4326&bbox=-180,-90,180,90&"
            + "styles=&layers=";

    private static final String WMS_GETFEATUREINFO_PLAIN_COV = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + LAYER_TEST + "&styles=&"
            + "query_layers=" + LAYER_TEST + "&info_format=text/plain&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_PLAIN_COV2 = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png"
            + "&I=50&J=50&CRS=EPSG%3A3857&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-6841646.293883737%2C1624185.0768806678%2C-6786056.35298747%2C1679775.0177769344&"
            + "layers=" + MARTINIQUE + "&styles=&"
            + "query_layers=" + MARTINIQUE + "&info_format=text/plain";

    private static final String WMS_GETFEATUREINFO_PLAIN_COV_ALIAS = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + COV_ALIAS + "&styles=&"
            + "query_layers=" + COV_ALIAS + "&info_format=text/plain&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_GML_COV = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + LAYER_TEST + "&styles=&"
            + "query_layers=" + LAYER_TEST + "&info_format=application/vnd.ogc.gml&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_GML_COV_ALIAS = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + COV_ALIAS + "&styles=&"
            + "query_layers=" + COV_ALIAS + "&info_format=application/vnd.ogc.gml&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_GML_COV2 = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png"
            + "&I=50&J=50&CRS=EPSG%3A3857&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-6841646.293883737%2C1624185.0768806678%2C-6786056.35298747%2C1679775.0177769344&"
            + "layers=" + MARTINIQUE + "&styles=&"
            + "query_layers=" + MARTINIQUE + "&info_format=application/vnd.ogc.gml";

    private static final String WMS_GETFEATUREINFO_PLAIN_FEAT = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=200&height=100&"
            + "srs=CRS:84&BbOx=0,-0.0020,0.0040,0&"
            + "layers=Lakes&styles=&"
            + "query_layers=Lakes&info_format=text/plain&"
            + "X=60&Y=60";

    private static final String WMS_GETFEATUREINFO_PLAIN_FEAT_REPROJECTED = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png&width=200&height=100&"
            + "crs=EPSG:4326&BbOx=-0.0020,0,0,0.0040&"
            + "layers=Lakes&styles=&"
            + "query_layers=Lakes&info_format=text/plain&"
            + "i=60&j=60";

    private static final String WMS_GETFEATUREINFO_PLAIN_FEAT_REPROJECTED2 = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png&width=200&height=100&"
            + "crs=EPSG:3857&BbOx=0,-222.63898163026252,445.2779631730943,0&"
            + "layers=Lakes&styles=&"
            + "query_layers=Lakes&info_format=text/plain&"
            + "i=60&j=60";

    private static final String WMS_GETFEATUREINFO_PLAIN_FEAT2 = "QuErY_LaYeRs=BasicPolygons&I=50&"
            + "LaYeRs=BasicPolygons&StYlEs=&WiDtH=100&CrS=CRS:84&"
            + "ReQuEsT=GetFeatureInfo&InFo_fOrMaT=text/plain&BbOx=-2,2,2,6"
            + "&HeIgHt=100&J=50&VeRsIoN=1.3.0&FoRmAt=image/gif";

    private static final String WMS_GETFEATUREINFO_GML_FEAT = "QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/vnd.ogc.gml&"
            + "X=60&StYlEs=&LaYeRs=Lakes&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    private static final String WMS_GETFEATUREINFO_HTML_FEAT = "QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=text/html&"
            + "X=60&StYlEs=&LaYeRs=Lakes&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

     private static final String WMS_GETFEATUREINFO_HTML_COV = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + LAYER_TEST + "&styles=&"
            + "query_layers=" + LAYER_TEST + "&info_format=text/html&"
            + "X=169&Y=20";

     private static final String WMS_GETFEATUREINFO_HTML_COV2 = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png"
            + "&I=50&J=50&CRS=EPSG%3A3857&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-6841646.293883737%2C1624185.0768806678%2C-6786056.35298747%2C1679775.0177769344&"
            + "layers=" + MARTINIQUE + "&styles=&"
            + "query_layers=" + MARTINIQUE + "&info_format=text/html";

     private static final String WMS_GETFEATUREINFO_HTML_COV_ALIAS = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + COV_ALIAS + "&styles=&"
            + "query_layers=" + COV_ALIAS + "&info_format=text/html&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_JSON_FEAT = "QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
            + "X=60&StYlEs=&LaYeRs=Lakes&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    /**
     * The queried line should cross SSTMDE200305 on following pixel coordinates:
     * Start -> X: 338.11 ; Y: 213.8709 ; value: 200
     * End   -> X: 338.82 ; Y: 214.8083 ; value: 0
     */
    private static final String WMS_GETFEATUREINFO_PROFILE_COV = "QuErY_LaYeRs=" + LAYER_TEST + "&BbOx=0,-0.0020,0.0040,0&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json;%20subtype=profile&"
            + "X=60&StYlEs=&LaYeRs=" + LAYER_TEST + "&"
        //    + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&PROFILE=LINESTRING(-61.132875680921%2014.81104016304,%20-60.973573923109%2014.673711061478,%20-60.946108102796%2014.706670045853,%20-60.915895700453%2014.610539674759,%20-60.882936716078%2014.48145031929)";
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&PROFILE=LINESTRING(-61.132875680921%2016.568852663,%20-60.973573923109%2016.431523561,%20-60.946108102796%2016.464482546,%20-60.915895700453%2016.368352175,%20-60.882936716078%2016.239262819)";

    private static final String WMS_GETFEATUREINFO_PROFILE_COV_ALIAS = "QuErY_LaYeRs=" + COV_ALIAS + "&BbOx=0,-0.0020,0.0040,0&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json;%20subtype=profile&"
            + "X=60&StYlEs=&LaYeRs=" + LAYER_TEST + "&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&PROFILE=LINESTRING(-61.132875680921%2014.81104016304,%20-60.973573923109%2014.673711061478,%20-60.946108102796%2014.706670045853,%20-60.915895700453%2014.610539674759,%20-60.882936716078%2014.48145031929)";

    /**
     * Asks for a profile intersecting regions without data available. The aim is to ensure that no-data values are
     * returned as NaN to the client.
     */
    private static final String WMS_GETFEATUREINFO_PROFILE_NAN_ANY = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo"
            + "&LAYERS=Band1&QUERY_LAYERS=Band1&StYlEs="
            + "&FoRmAt=image/jpeg&INFO_FORMAT=application/json;%20subtype=profile"
            + "&I=0&J=0&WiDtH=256&HeIgHt=256"
            + "&CRS=CRS:84&BBOX=-1,-2,-1,2"
            + "&PROFILE=LINESTRING(-1%20-2%2C-1%202)" // (-1 -2,-1 2)
            + "&nanPropagation=any&outOfBounds=ignore&reducer=nearest";
    /**
     * Asks for a profile intersecting regions without data available. The aim is to ensure that no-data values are
     * returned as NaN to the client.
     */
    private static final String WMS_GETFEATUREINFO_PROFILE_NAN_SEGMENT = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo"
            + "&LAYERS=Band1&QUERY_LAYERS=Band1&StYlEs="
            + "&FoRmAt=image/jpeg&INFO_FORMAT=application/json;%20subtype=profile"
            + "&I=0&J=0&WiDtH=256&HeIgHt=256"
            + "&CRS=CRS:84&BBOX=-2,-2,2,2"
            + "&PROFILE=LINESTRING(-1%200%2C0%200)" // (-1 0,0 0)
            + "&nanPropagation=all";

    /**
     * Ask a transect (profile) around a local data. The queried line does not cross the queried layer, but its bbox
     * does. In such case, the backend should detect disjoint domain, and return empty series.
     */
    private static final String WMS_GETFEATUREINFO_PROFILE_OUTSIDE = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetFeatureInfo"
            + "&LAYERS=" + MARTINIQUE + "&QUERY_LAYERS=" + MARTINIQUE + "&StYlEs="
            + "&FoRmAt=image/jpeg&INFO_FORMAT=application/json;%20subtype=profile"
            + "&I=0&J=0&WiDtH=256&HeIgHt=256"
            + "&CRS=CRS:84&BBOX=-62%2C14%2C-61%2C16"
            + "&PROFILE=LINESTRING(-62%2014%2C-62%2016%2C-61%2016)"
            + "&outofBounds=ignore";

    private static final String WMS_GETFEATUREINFO_JSON_FEAT_ALIAS = "QuErY_LaYeRs=JS1&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
            + "X=60&StYlEs=&LaYeRs=JS1&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    private static final String WMS_GETFEATUREINFO_JSON_FEAT_ALIAS2 = "QuErY_LaYeRs=JS2&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
            + "X=60&StYlEs=&LaYeRs=JS2&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";

    private static final String WMS_GETFEATUREINFO_JSON_FEAT_PROPNAME = "QuErY_LaYeRs=JS2&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
            + "X=60&StYlEs=&LaYeRs=JS2&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&"
            + "PROPERTYNAME=name";

    private static final String WMS_GETFEATUREINFO_JSON_FEAT_INVERTED_PROPNAME = "QuErY_LaYeRs=JS2&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
            + "X=60&StYlEs=&LaYeRs=JS2&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&"
            + "PROPERTYNAME=-name";

    private static final String WMS_GETFEATUREINFO_JSON_MULTI_PROPNAME = "QuErY_LaYeRs=JS1,JS2&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/json&"
            + "X=60&StYlEs=&LaYeRs=JS1,JS2&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60&"
            + "PROPERTYNAME=()(name)";

    private static final String WMS_GETFEATUREINFO_JSON_COV = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + LAYER_TEST + "&styles=&"
            + "query_layers=" + LAYER_TEST + "&info_format=application/json&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_JSON_COV_ALIAS = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + COV_ALIAS + "&styles=&"
            + "query_layers=" + COV_ALIAS + "&info_format=application/json&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_JSON_COV2 = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png"
            + "&I=50&J=50&CRS=EPSG%3A3857&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-6841646.293883737%2C1624185.0768806678%2C-6786056.35298747%2C1679775.0177769344&"
            + "layers=" + MARTINIQUE + "&styles=&"
            + "query_layers=" + MARTINIQUE + "&info_format=application/json";

    private static final String WMS_GETFEATUREINFO_XML_COV = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + LAYER_TEST + "&styles=&"
            + "query_layers=" + LAYER_TEST + "&info_format=application/vnd.ogc.xml&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_XML_COV_ALIAS = "request=GetFeatureInfo&service=WMS&version=1.1.1&"
            + "format=image/png&width=256&height=256&"
            + "srs=EPSG:4326&bbox=-180,-90,-90,0&"
            + "layers=" + COV_ALIAS + "&styles=&"
            + "query_layers=" + COV_ALIAS + "&info_format=application/vnd.ogc.xml&"
            + "X=169&Y=20";

    private static final String WMS_GETFEATUREINFO_XML_COV2 = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png"
            + "&I=50&J=50&CRS=EPSG%3A3857&STYLES=&WIDTH=101&HEIGHT=101&BBOX=-6841646.293883737%2C1624185.0768806678%2C-6786056.35298747%2C1679775.0177769344&"
            + "layers=" + MARTINIQUE + "&styles=&"
            + "query_layers=" + MARTINIQUE + "&info_format=application/vnd.ogc.xml";

    private static final String WMS_GETFEATUREINFO_XML_FEAT = "QuErY_LaYeRs=Lakes&BbOx=0,-0.0020,0.0040,0&"
            + "FoRmAt=image/gif&ReQuEsT=GetFeatureInfo&"
            + "VeRsIoN=1.1.1&InFo_fOrMaT=application/vnd.ogc.xml&"
            + "X=60&StYlEs=&LaYeRs=Lakes&"
            + "SrS=EPSG:4326&WiDtH=200&HeIgHt=100&Y=60";
    
    private static final String WMS_GETFEATUREINFO_JSON_JSON = "request=GetFeatureInfo&service=WMS&version=1.3.0&"
            + "format=image/png&width=256&height=256&"
            + "CRS=EPSG%3A3857&BBOX=410925.4640611075%2C5439870.428999424%2C415817.43387135875%2C5444762.398809675&"
            + "layers=" + COUNTRIES + "&styles=&"
            + "query_layers=" + COUNTRIES + "&info_format=application/json&"
            + "I=253&J=44";

    private static final String WMS_GETLEGENDGRAPHIC = "request=GetLegendGraphic&service=wms&"
            + "width=200&height=40&layer=" + LAYER_TEST + "&format=image/png&version=1.1.0";
    
    private static final String WMS_GETLEGENDGRAPHIC_JSON = "request=GetLegendGraphic&service=wms&"
            + "width=200&height=40&layer=" + LAKES + "&format=application/json&version=1.1.0&style=default-polygon";

    private static final String WMS_GETLEGENDGRAPHIC_ALIAS = "request=GetLegendGraphic&service=wms&"
            + "width=200&height=40&layer=JS1&format=image/png&version=1.1.0";

    private static final String WMS_GETLEGENDGRAPHIC_ALIAS2 = "request=GetLegendGraphic&service=wms&"
            + "width=200&height=40&layer=JS2&format=image/png&version=1.1.0";

    private static final String WMS_DESCRIBELAYER = "request=DescribeLayer&service=WMS&"
            + "version=1.1.1&layers=" + LAYER_TEST;

    private static final String WMS_GETMAP2
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/png&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_ALIAS
            = "HeIgHt=100&LaYeRs=JS1&FoRmAt=image/png&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_ALIAS2
            = "HeIgHt=100&LaYeRs=JS2&FoRmAt=image/png&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-80.72487831115721,35.2553619492954,-80.70324897766113,35.27035945142482&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_BMP
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/bmp&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100&TRANSPARENT=${transparent}";

    private static final String WMS_GETMAP_JPEG
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/jpeg&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_BMP_111
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/bmp&ReQuEsT=GetMap&StYlEs=&SrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.1.1&WiDtH=100";

    private static final String WMS_GETMAP_PPM
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/x-portable-pixmap&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100&TRANSPARENT=${transparent}";

    private static final String WMS_GETMAP_GIF
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/gif&ReQuEsT=GetMap&StYlEs=&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";
    
    private static final String WMS_GETMAP_STYLE_SENSOR
            = "HeIgHt=100&LaYeRs=Lakes&FoRmAt=image/png&ReQuEsT=GetMap&StYlEs=default-point-sensor&CrS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.3.0&WiDtH=100";

    private static final String WMS_GETMAP_GIF_UNVALID_LAYER
            = "TrAnSpArEnT=False&HeIgHt=100&LaYeRs=unknownlayer&FoRmAt=image/gif&ReQuEsT=GetMap&StYlEs=&srS=CRS:84&BbOx=-0.0025,-0.0025,0.0025,0.0025&VeRsIoN=1.1.1&WiDtH=100&EXCEPTIONS=application/vnd.ogc.se_inimage";

    private static final String WMS_GETMAP_GIF_TRANSPARENT
            = "TrAnSpArEnT=TRUE&CrS=CRS:84&FoRmAt=image%2Fgif&VeRsIoN=1.3.0&HeIgHt=100&WiDtH=200&StYlEs=&LaYeRs=Lakes&ReQuEsT=GetMap&BbOx=0,-0.0020,0.0040,0";

    private static final String WMS_GETMAP_111_PROJ = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:3395&bbox=-19000000,-19000000,19000000,19000000&"
            + "layers=" + LAYER_TEST + "&styles=";
    private static final String WMS_GETMAP_130_PROJ = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=EPSG:3395&bbox=-19000000,-19000000,19000000,19000000&"
            + "layers=" + LAYER_TEST + "&styles=";
    private static final String WMS_GETMAP_111_GEO = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:4022&bbox=-90,-180,90,180&"
            + "layers=" + LAYER_TEST + "&styles=";
    private static final String WMS_GETMAP_111_EPSG_4326 = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=EPSG:4326&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";
    private static final String WMS_GETMAP_130_EPSG_4326 = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=512&height=1024&"
            + "crs=EPSG:4326&bbox=-90,-180,90,180&"
            + "layers=" + LAYER_TEST + "&styles=";
    private static final String WMS_GETMAP_111_CRS_84 = "request=GetMap&service=WMS&version=1.1.1&"
            + "format=image/png&width=1024&height=512&"
            + "srs=CRS:84&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";
    private static final String WMS_GETMAP_130_CRS_84 = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-180,-90,180,90&"
            + "layers=" + LAYER_TEST + "&styles=";

    private static final String WMS_GETMAP_TIFF = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fpng"
            + "&TRANSPARENT=true&LAYERS=" + MARTINIQUE
            + "&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES="
            + "&BBOX=-6887893.4928338025%2C1565430.3392804079%2C-6731350.458905761%2C1721973.3732084488";

    private static final String WMS_GETMAP_TIFF_TO_JPEG = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fjpeg"
            + "&TRANSPARENT=${transparent}&LAYERS=" + MARTINIQUE
            + "&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES="
            + "&BBOX=-6887893.4928338025%2C1565430.3392804079%2C-6731350.458905761%2C1721973.3732084488";
    
    private static final String WMS_GETMAP_TIFF_TO_TIFF = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Ftiff"
            + "&TRANSPARENT=true&LAYERS=" + MARTINIQUE
            + "&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES="
            + "&BBOX=-6887893.4928338025%2C1565430.3392804079%2C-6731350.458905761%2C1721973.3732084488";

    private static final String WMS_GETMAP_SHAPE_POINT = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fpng&"
            + "TRANSPARENT=true&LAYERS=BuildingCenters&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES="
            + "&BBOX=0%2C0%2C305.748113140705%2C305.748113140705";

    private static final String WMS_GETMAP_SHAPE_POLYGON = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fpng"
            + "&TRANSPARENT=true&LAYERS=BasicPolygons&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES="
            + "&BBOX=0%2C0%2C626172.1357121639%2C626172.1357121639";

    private static final String WMS_GETMAP_NETCDF = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fpng"
            + "&TRANSPARENT=true&LAYERS=sea_water_temperature&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES=&"
            + "BBOX=0%2C0%2C20037508.342789244%2C20037508.342789244";

    private static final String WMS_GETMAP_JSON_FEATURE = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fpng"
            + "&TRANSPARENT=true&LAYERS=JS1&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES=&"
            + "BBOX=-8986548.541431602%2C4197310.097195599%2C-8984102.556526477%2C4199756.082100725";

    private static final String WMS_GETMAP_JSON_COLLECTION = "SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&FORMAT=image%2Fpng"
            + "&TRANSPARENT=true&LAYERS=JS2&SLD_VERSION=1.1.0&WIDTH=256&HEIGHT=256&CRS=EPSG%3A3857&STYLES=&"
            + "BBOX=-9001224.450862356%2C4187526.157575095%2C-8962088.692380344%2C4226661.916057105";

    private static final String WMS_GETMAP_130_JCOLL = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-81,35,-80.5,35.5&"
            + "layers=JCOL&styles=";

    private static final String WMS_GETMAP_130_JCOLL_LAYER_FILTER = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-81,35,-80.5,35.5&"
            + "layers=" + JCOL_FILTER + "&styles=";

    private static final String WMS_GETMAP_130_JCOLL_REQUEST_CQL_FILTER = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-81,35,-80.5,35.5&"
            + "layers=JCOL&styles=&CQL_FILTER=elevation%20%3C%201000";

    private static final String WMS_GETMAP_130_JCOLL_REQUEST_FILTER = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-81,35,-80.5,35.5&"
            + "layers=JCOL&styles=&"
            + "filter=%3Cogc:Filter%20xmlns:ogc=%22http://www.opengis.net/ogc%22%20xmlns:gml=%22http://www.opengis.net/gml%22%3E"
            + "%3Cogc:PropertyIsLessThan%3E"
            + "%3Cogc:PropertyName%3Eelevation%3C/ogc:PropertyName%3E"
            + "%3Cogc:Literal%3E1000%3C/ogc:Literal%3E"
            + "%3C/ogc:PropertyIsLessThan%3E"
            + "%3C/ogc:Filter%3E";

    private static final String WMS_GETMAP_130_JCOLL_ELEVATION = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-81,35,-80.5,35.5&"
            + "layers=" + JCOL_FILTER + "&styles=&elevation=700";
    
    private static final String WMS_GETMAP_OM2 = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&bbox=-180,-90,180,90&"
            + "layers=" + OM2_LAYER + "&styles=";
    
    private static final String WMS_GETMAP_130_NP_REQUEST_CQL_FILTER = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&BBOX=0.001,-0.002,0.005,0.003&"
            + "layers=NamedPlaces&styles=&CQL_FILTER=FID%20%3D%20118";
    
    private static final String WMS_GETMAP_130_NP_REQUEST = "request=GetMap&service=WMS&version=1.3.0&"
            + "format=image/png&width=1024&height=512&"
            + "crs=CRS:84&BBOX=0.001,-0.002,0.005,0.003&"
            + "layers=NamedPlaces&styles=";
    
    private static boolean initialized = false;

    private static Path CONFIG_DIR;

    @BeforeClass
    public static void startup() {
        CONFIG_DIR = ConfigDirectory.setupTestEnvironement("WMSRequestTest");
        controllerConfiguration = WMSControllerConfig.class;
    }

    private static final int DEF_NB_LAYER = 29;

    /**
     * Initialize the list of layers from the defined providers in
     * Constellation's configuration.
     */
    public void initLayerList() {
        pool = WMSMarshallerPool.getInstance();
        if (!initialized) {
            try {
                startServer();

                try {
                    layerBusiness.removeAll();
                    serviceBusiness.deleteAll();
                    dataBusiness.deleteAll();
                    providerBusiness.removeAll();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error whle cleaning database before test", ex);
                }

                WMSPortrayal.setEmptyExtension(true);

                final TestResources testResource = initDataDirectory();
                final List<DataImport> datas = new ArrayList<>();

                // coverage-file datastore
                DataImport did  = testResource.createProvider(TestResource.PNG, providerBusiness, null).datas.get(0);
                DataImport did2 = testResource.createProvider(TestResource.TIF, providerBusiness, null).datas.get(0);

                // alias on coverage data
                DataImport did3 = testResource.createProvider(TestResource.PNG, providerBusiness, null).datas.get(0);

                // aggregated datastore
                datas.addAll(TestEnvironment.createAggregateProvider(providerBusiness, "aggData", Arrays.asList(did.id, did2.id), null).datas);

                // shapefile datastore
                final List<DataImport> wms111ShapeDatas = new ArrayList<>();
                wms111ShapeDatas.addAll(testResource.createProviders(TestResource.WMS111_SHAPEFILES, providerBusiness, null).datas());
                datas.addAll(wms111ShapeDatas);

                // we add two times a new geojson provider in order to create 2 layer with same name but different alias
                DataImport d13 = testResource.createProvider(TestResource.JSON_FEATURE, providerBusiness, null).datas.get(0);
               // DataImport d14 = testResource.createProvider(TestResource.JSON_FEATURE, providerBusiness, null).datas.get(0);

                // netcdf datastore
                datas.addAll(testResource.createProvider(TestResource.NETCDF, providerBusiness, null).datas);
                datas.addAll(testResource.createProvider(TestResource.NETCDF_WITH_NAN, providerBusiness, null).datas);

                DataImport d15 = testResource.createProvider(TestResource.JSON_FEATURE_COLLECTION, providerBusiness, null).datas.get(0);
                
                datas.addAll(testResource.createProviderWithDatasource(TestResource.OM2_DB, providerBusiness, datasourceBusiness, null).datas);
                
                final LayerContext config = new LayerContext();
                config.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

                Integer defId = serviceBusiness.create("wms", "default", config, null, null);
                final Details details = serviceBusiness.getInstanceDetails("wms", "default", "eng");
                details.getServiceConstraints().setLayerLimit(100);
                serviceBusiness.setInstanceDetails("wms", "default", details, "eng", true);

                layerBusiness.add(did.id,       null,            null, LAYER_TEST,   null, defId, null);
                layerBusiness.add(did2.id, MARTINIQUE,  did2.namespace,  did2.name,   null, defId, null);
                layerBusiness.add(did3.id, COV_ALIAS,  did3.namespace,  did3.name,   null, defId, null);

                for (DataImport d : datas) {
                    layerBusiness.add(d.id, null, d.namespace, d.name, null, defId, null);
                }

                layerBusiness.add(d13.id,  "JS1", d13.namespace,  d13.name,    null, defId, null);
                layerBusiness.add(d13.id,  "JS2", d13.namespace,  d13.name,    null, defId, null);
                
                List<DataImport> shapesDatas = testResource.createProviders(TestResource.SHAPEFILES, providerBusiness, null).datas();
                 for (DataImport d : shapesDatas) {
                     LayerConfig lconfig = null;
                     if (COUNTRIES.equals(d.name)) {
                         lconfig = new LayerConfig();
                         DimensionDefinition dd = new DimensionDefinition("time", "date_creat", "date_creat");
                         lconfig.setDimensions(Arrays.asList(dd));
                     }
                     layerBusiness.add(d.id, null, d.namespace, d.name, null, defId, lconfig);
                 }

                // add a filter on "elevation" property
                LayerConfig lconfig = new LayerConfig();
                lconfig.setFilter(new Filter("elevation", "1000", FILTER_COMPARISON_ISLESS));
                DimensionDefinition dd = new DimensionDefinition("elevation", "elevation", "elevation");
                lconfig.setDimensions(Arrays.asList(dd));
                layerBusiness.add(d15.id, JCOL_FILTER, d15.namespace,  d15.name,    null, defId, lconfig);

                // add basic layer for comparison
                layerBusiness.add(d15.id, "JCOL", d15.namespace,  d15.name,    null, defId, null);
                
                final LayerContext config2 = new LayerContext();
                config2.setSupportedLanguages(new Languages(Arrays.asList(new Language("fre"), new Language("eng", true))));
                config2.setGetFeatureInfoCfgs(FeatureInfoUtilities.createGenericConfiguration());

                int styleId1 = styleBusiness.getStyleId("sld", "default-polygon");
                int styleId2 = styleBusiness.getStyleId("sld", "default-point-sensor");
                Integer wm1Id = serviceBusiness.create("wms", "wms1", config2, null, null);
                // only add the 'lakes' data and add a namespace to the layer
                for (DataImport d : wms111ShapeDatas) {
                    if (LAKES.equals(d.name)) {
                        int layerId = layerBusiness.add(d.id, null, "http://www.opengis.net/gml", d.name, null, wm1Id, null);
                        styleBusiness.linkToLayer(styleId2, layerId);
                        styleBusiness.linkToLayer(styleId1, layerId);
                    }
                }

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
                layerBusiness.add(did.id,   null,  did.namespace,  did.name,   null, wm2Id, null);
                for (DataImport d : wms111ShapeDatas) {
                    layerBusiness.add(d.id, null, "http://www.opengis.net/gml", d.name, null, wm2Id, null);
                }

                final WMSPortrayal port = new WMSPortrayal();

                serviceBusiness.setExtraConfiguration("wms", "wms2", "WMSPortrayal.xml", port, GenericDatabaseMarshallerPool.getInstance());

                serviceBusiness.start(defId);
                serviceBusiness.start(wm1Id);
                serviceBusiness.start(wm2Id);
                waitForRestStart("wms", "default");
                waitForRestStart("wms", "wms1");
                waitForRestStart("wms", "wms2");

                initialized = true;

                GeneralEnvelope env = new GeneralEnvelope(org.apache.sis.referencing.CRS.forCode("EPSG:4326"));
                env.setRange(0, -0.0020, 0);
                env.setRange(1, 0, 0.0040);
                System.out.println("ENV:" + env);
                env = (GeneralEnvelope) Envelopes.transform(env, org.apache.sis.referencing.CRS.forCode("EPSG:3857"));
                System.out.println("ENV R:" + env);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() throws JAXBException {
        try {
            final ILayerBusiness layerBean = SpringHelper.getBean(ILayerBusiness.class).orElse(null);;
            if (layerBean != null) {
                layerBean.removeAll();
            }
            final IServiceBusiness service = SpringHelper.getBean(IServiceBusiness.class).orElse(null);;
            if (service != null) {
                service.deleteAll();
            }
            final IDataBusiness dataBean = SpringHelper.getBean(IDataBusiness.class).orElse(null);;
            if (dataBean != null) {
                dataBean.deleteAll();
            }
            final IProviderBusiness provider = SpringHelper.getBean(IProviderBusiness.class).orElse(null);;
            if (provider != null) {
                provider.removeAll();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage());
        }
        ConfigDirectory.shutdownTestEnvironement();
        stopServer();
    }

    /**
     * Ensure that a wrong value given in the request parameter for the WMS
     * server returned an error report for the user.
     */
    @Test
    @Order(order = 1)
    public void testWMSWrongRequest() throws Exception {

        initLayerList();

        // Creates an intentional wrong url, regarding the WMS version 1.1.1 standard
        final URL wrongUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_FALSE_REQUEST);

        // Try to marshall something from the response returned by the server.
        // The response should be a ServiceExceptionReport.
        final Object obj = unmarshallResponse(wrongUrl);
        assertTrue(obj instanceof ServiceExceptionReport);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 2)
    public void testWMSGetMap() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BAD_HEIGHT);
        String obj = getStringResponse(getMapUrl);
        assertTrue("was " + obj, obj.contains("InvalidDimensionValue"));

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BAD_WIDTH);
        obj = getStringResponse(getMapUrl);
        assertTrue("was " + obj, obj.contains("InvalidDimensionValue"));

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_COV_JPEG);
        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/jpeg");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
    }

    @Test
    @Order(order = 2)
    public void testWMSGetMapAntiMeridianCross() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_ANTI_MERI_CROSS);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
        writeImageInFile(image, "image/png", CONFIG_DIR.resolve("ANTI-MERI.png"));

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_ANTI_MERI_CROSS_MERC);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
        writeImageInFile(image, "image/png", CONFIG_DIR.resolve("ANTI-MERI-MERC.png"));

    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 3)
    public void testWMSGetMapLakeGif() throws Exception {
        initLayerList();
        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_GIF);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/gif");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 4)
    public void testWMSGetMapLakeGifransparent() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_GIF_TRANSPARENT);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/gif");

        // Test on the returned image.
        assertEquals(200, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 5)
    public void testWMSGetMapLakePng() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP2);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 6)
    public void testWMSGetMapLakeBmp() throws Exception {
        initLayerList();

        // Creates a valid GetMap url. with transparent = FALSE
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BMP.replace("${transparent}", "FALSE"));

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/bmp");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);

         // Creates a valid GetMap url. with transparent = TRUE
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_BMP.replace("${transparent}", "TRUE"));

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/bmp");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);

        // wms do not supported 1.1.1 request
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms2?" + WMS_GETMAP_BMP_111);
        Object obj = unmarshallResponse(getMapUrl);
        assertTrue(obj instanceof ServiceExceptionReport);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 7)
    public void testWMSGetMapLakeJpeg() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_JPEG);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/jpeg");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a valid GetMap request returns indeed a
     * {@link BufferedImage}.
     */
    @Test
    @Order(order = 8)
    @Ignore("Pixmap support has been removed. Re-activate if we support it again in the future.")
    public void testWMSGetMapLakePpm() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.  with transparent = FALSE
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_PPM.replace("${transparent}", "FALSE"));

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/x-portable-pixmap");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);

        // Creates a valid GetMap url.  with transparent = TRUE
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_PPM.replace("${transparent}", "TRUE"));

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/x-portable-pixmap");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that an exception is returned when requesting too many layers.
     */
    @Test
    @Order(order = 9)
    public void testWMSGetMapLayerLimit() throws Exception {

        // Creates a valid GetMap url.
            final StringBuilder sb = new StringBuilder();
            sb.append("http://localhost:").append(getCurrentPort()).append("/WS/wms/default?" + WMS_GETMAP_LAYER_LIMIT);
            sb.append(LAYER_TEST);
            for (int i = 0; i < 120; i++) {
                sb.append(',').append(LAYER_TEST);
            }
        final URL getMapUrl = new URL(sb.toString());
        // Try to get a map from the url. The test is skipped in this method if it fails.
        try {
            final BufferedImage image = getImageFromURL(getMapUrl, "image/png");
            Assert.fail("Service should have raised an error");
        } catch (Exception ex) {
            //ok
        }

    }

    /**
     * Ensures that an error is returned in image as gif and is not all black.
     */
    @Test
    @Order(order = 10)
    public void testWMSGetMapErrorInImageGif() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_GIF_UNVALID_LAYER);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/gif");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) == 2);
        assertFalse(ImageTesting.hasTransparency(image));

    }

    /**
     * Ensures that a valid GetCapabilities request returns indeed a valid
     * GetCapabilities document representing the server capabilities in the WMS
     * version 1.1.1/ 1.3.0 standard.
     */
    @Test
    @Order(order = 11)
    public void testWMSGetCapabilities() throws JAXBException, Exception {
        initLayerList();

        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETCAPABILITIES_111);

        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue("was:" + obj, obj instanceof WMT_MS_Capabilities);
        WMT_MS_Capabilities responseCaps = (WMT_MS_Capabilities) obj;

        Layer layer = (Layer) responseCaps.getLayerFromName(LAYER_TEST);

        assertNotNull(layer);
        assertEquals("EPSG:4326", layer.getSRS().get(0));
        final LatLonBoundingBox bboxGeo = (LatLonBoundingBox) layer.getLatLonBoundingBox();
        assertEquals(bboxGeo.getWestBoundLongitude(),-180.0, 0.2);
        assertEquals(bboxGeo.getSouthBoundLatitude(), -90.0, 0.2);
        assertEquals(bboxGeo.getEastBoundLongitude(), 180.0, 0.2);
        assertEquals(bboxGeo.getNorthBoundLatitude(),  90.0, 0.2);

        layer = (Layer) responseCaps.getLayerFromName(JCOL_FILTER);
        assertEquals(1, layer.getDimension().size());
        Dimension elevation = layer.getDimension().get(0);
        assertEquals("Ellipsoidal height", elevation.getName());
        assertEquals("", elevation.getValue());

        assertEquals(1, layer.getExtent().size());
        Extent extent = layer.getExtent().get(0);
        assertEquals("Ellipsoidal height", extent.getName());
        var extentValueStr = extent.getvalue();
        Assertions.assertNotNull(extentValueStr);
        var extentValues = Pattern.compile("\s*,\s*")
                .splitAsStream(extentValueStr)
                .map(Integer::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of(700, 800, 900, 1000, 1100, 1200, 1300), extentValues);
        
        layer = (Layer) responseCaps.getLayerFromName(COUNTRIES);
        assertEquals(1, layer.getDimension().size());
        Dimension time = layer.getDimension().get(0);
        assertEquals("time", time.getName());
        assertEquals("", time.getValue());

        assertEquals(1, layer.getExtent().size());
        Extent extentTime = layer.getExtent().get(0);
        assertEquals("time", extentTime.getName());
        var extentTimeValueStr = extentTime.getvalue();
        Assertions.assertNotNull(extentTimeValueStr);
        var extentTimeValues = Pattern.compile("\s*,\s*")
                .splitAsStream(extentTimeValueStr)
                .map(String::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of("2026-03-12T23:00:00Z", 
                            "2027-03-12T23:00:00Z", 
                            "2023-03-12T23:00:00Z", 
                            "2024-03-12T23:00:00Z", 
                            "2021-03-12T23:00:00Z", 
                            "2025-03-12T23:00:00Z", 
                            "2022-03-12T23:00:00Z"), extentTimeValues);


        String currentUrl = responseCaps.getCapability().getRequest().getGetMap().getDCPType().get(0).getHTTP().getGet().getOnlineResource().getHref();

        assertEquals("http://localhost:" + getCurrentPort() + "/WS/wms/default?", currentUrl);

        // Creates a valid GetCapabilities url.
        getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETCAPABILITIES_111);
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMT_MS_Capabilities);

        responseCaps = (WMT_MS_Capabilities) obj;

        // The layer test must be excluded
        layer = (Layer) responseCaps.getLayerFromName(LAYER_TEST);
        assertNull(layer);

        // The layer lake must be included
        layer = (Layer) responseCaps.getLayerFromName("http://www.opengis.net/gml:Lakes");
        assertNotNull(layer);

        currentUrl = responseCaps.getCapability().getRequest().getGetMap().getDCPType().get(0).getHTTP().getGet().getOnlineResource().getHref();

        assertEquals("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?", currentUrl);

        getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETCAPABILITIES_130);

        // Try to marshall something from the response returned by the server.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);
        WMSCapabilities responseCaps130 = (WMSCapabilities) obj;

        org.geotoolkit.wms.xml.v130.Layer layer130 = (org.geotoolkit.wms.xml.v130.Layer) responseCaps130.getLayerFromName(JCOL_FILTER);
        org.geotoolkit.wms.xml.v130.Dimension elevation130 = layer130.getDimension().get(0);

        assertEquals("Ellipsoidal height", elevation130.getName());
        extentValueStr = elevation130.getValue();
        Assertions.assertNotNull(extentValueStr);
        extentValues = Pattern.compile("\s*,\s*")
                .splitAsStream(extentValueStr)
                .map(Integer::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of(700, 800, 900, 1000, 1100, 1200, 1300), extentValues);

        currentUrl = responseCaps130.getCapability().getRequest().getGetMap().getDCPType().get(0).getHTTP().getGet().getOnlineResource().getHref();

        assertEquals("http://localhost:" + getCurrentPort() + "/WS/wms/default?", currentUrl);

        // Creates a valid GetCapabilities url.
        getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms2?" + WMS_GETCAPABILITIES_111);
        //the service WMS2 does not support 1.1.0 version so it respond in 1.3.0
        obj = unmarshallResponse(getCapsUrl);
        assertTrue("was :" + obj.getClass().getName(), obj instanceof WMSCapabilities);

        // Creates a valid GetCapabilities url.
        getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms2?" + WMS_GETCAPABILITIES_130);
        // Try to marshall something from the response returned by the server.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);
    }

    @Test
    @Order(order = 12)
    public void testWMSGetCapabilitiesLanguage() throws JAXBException, Exception {
        pool = WMSMarshallerPool.getInstance();

        // Creates a valid GetMap url.
        URL getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETCAPABILITIES_130);
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);

        WMSCapabilities responseCaps130 = (WMSCapabilities) obj;
        ExtendedCapabilitiesType ext = responseCaps130.getCapability().getInspireExtendedCapabilities();
        assertEquals("eng", ext.getCurrentLanguage());

        LanguageType l1 = new LanguageType("fre", false);
        LanguageType l2 = new LanguageType("eng", true);
        LanguagesType languages = new LanguagesType(Arrays.asList(l1, l2));
        assertEquals(ext.getLanguages(), languages);

        assertEquals("this is the default english capabilities", responseCaps130.getService().getName());

        getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETCAPABILITIES_130_ENG);
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);

        responseCaps130 = (WMSCapabilities) obj;
        ext = responseCaps130.getCapability().getInspireExtendedCapabilities();
        assertEquals("eng", ext.getCurrentLanguage());
        assertEquals(ext.getLanguages(), languages);

        assertEquals("this is the default english capabilities", responseCaps130.getService().getName());

        getCapsUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETCAPABILITIES_130_FRE);
        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WMSCapabilities);

        responseCaps130 = (WMSCapabilities) obj;
        ext = responseCaps130.getCapability().getInspireExtendedCapabilities();
        assertEquals("fre", ext.getCurrentLanguage());
        assertEquals(ext.getLanguages(), languages);

        assertEquals("Ceci est le document capabilities français", responseCaps130.getService().getName());

    }

    /**
     * Ensures that the {@code WMS GetFeatureInfo} request on a particular point
     * of the testing layer produces the wanted result.
     *
     * @throws Exception
     */
    @Test
    @Order(order = 13)
    public void testWMSGetFeatureInfoPlainCoveragePng() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_COV);

        String expResult = "SSTMDE200305\n"
                + "Color index;\n"
                + "201.0;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_COV_ALIAS);

        expResult = "SST\n"
                + "Color index;\n"
                + "201.0;\n\n";

        result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_COV2);

        expResult =  MARTINIQUE + "\n" +
                    "Red;Green;Blue;\n" +
                    "63.0;92.0;132.0;\n\n";

        result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);
    }

    /**
     * I don't know why this test do not work
     *
     * @throws Exception
     */
    @Test
    @Order(order = 14)
    public void testWMSGetFeatureInfoPlainShapePng() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_FEAT);

        String expResult
                = "Lakes\n"
                + "sis:identifier:String;the_geom:MultiPolygon;FID:String;NAME:String;\n"
                + "Lakes.1;MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)));101;Blue Lake;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);
    }

    /**
     * Ensure that returned features are reprojected according to specified CRS.
     * This test queries coordinates in lat/lon order. The check consist in ensuring that coordinates have been flipped
     * correctly.
     * Opportunist check: verify that WMS 1.3.0 axis order of CRS definitions.
     */
    @Test
    @Order(order = 14)
    public void testWMSGetFeatureInfoPlainShapeReprojected() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_FEAT_REPROJECTED);

        String expResult
                = "Lakes\n"
                + "sis:identifier:String;the_geom:MultiPolygon;FID:String;NAME:String;\n"
                + "Lakes.1;MULTIPOLYGON (((-0.0018 0.0006, -0.0006 0.001, -0.0001 0.0024, -0.0015 0.0031, -0.0018 0.0006), (-0.0011 0.0017, -0.0011 0.0025, -0.0006 0.0025, -0.0006 0.0017, -0.0011 0.0017)));101;Blue Lake;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_FEAT_REPROJECTED2);

        expResult
                = "Lakes\n"
                + "sis:identifier:String;the_geom:MultiPolygon;FID:String;NAME:String;\n"
                + "Lakes.1;MULTIPOLYGON (((66.79169447596414 -200.37508346162957, 111.31949079327357 -66.7916944775693, 267.16677790385654 -11.131949078903947, 345.09042145914805 -166.97923620897757, 66.79169447596414 -200.37508346162957), (189.24313434856506 -122.4514398800355, 278.29872698318394 -122.4514398800355, 278.29872698318394 -66.7916944775693, 189.24313434856506 -66.7916944775693, 189.24313434856506 -122.4514398800355)));101;Blue Lake;\n\n";

        result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order = 15)
    public void testWMSGetFeatureInfoPlainShapeGif() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PLAIN_FEAT2);

        String expResult
                = "BasicPolygons\n"
                + "sis:identifier:String;the_geom:MultiPolygon;ID:String;\n"
                + "BasicPolygons.2;MULTIPOLYGON (((-2 6, 1 6, 1 3, -2 3, -2 6)));;\n\n";

        String result = getStringResponse(gfi);

        assertNotNull(expResult);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order = 16)
    public void testWMSGetFeatureInfoGMLGif() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_GML_FEAT);

        String expResult
                = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<msGMLOutput xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                + "<Lakes_layer>\n"
                + "	<Lakes_feature>\n"
                + "		<ID>Lakes.1</ID>\n"
                + "		<identifier>Lakes.1</identifier>\n"
                + "		<the_geom>\n"
                + "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))\n"
                + "		</the_geom>\n"
                + "		<FID>101</FID>\n"
                + "		<NAME>Blue Lake</NAME>\n"
                + "	</Lakes_feature>\n"
                + "</Lakes_layer>\n"
                + "</msGMLOutput>";

        String result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_GML_COV);

        expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<msGMLOutput xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "<SSTMDE200305_layer>\n" +
                    "	<SSTMDE200305_feature>\n" +
                    "		<gml:boundedBy>\n" +
                    "			<gml:Box srsName=\"CRS:84\">\n" +
                    "				<gml:coordinates>-120.41015625,-7.20703125 -120.41015625,-7.20703125</gml:coordinates>\n" +
                    "			</gml:Box>\n" +
                    "		</gml:boundedBy>\n" +
                    "		<x>-120.41015625</x>\n" +
                    "		<y>-7.20703125</y>\n" +
                    "		<variable>Color index</variable>\n" +
                    "		<value>201.0</value>\n" +
                    "	</SSTMDE200305_feature>\n" +
                    "</SSTMDE200305_layer>\n" +
                    "</msGMLOutput>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_GML_COV2);

        expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<msGMLOutput xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "<" + MARTINIQUE + "_layer>\n" +
                    "	<" + MARTINIQUE + "_feature>\n" +
                    "		<gml:boundedBy>\n" +
                    "			<gml:Box srsName=\"EPSG:3857\">\n" +
                    "				<gml:coordinates>-6813851.323435604,1651980.0473288011 -6813851.323435604,1651980.0473288011</gml:coordinates>\n" +
                    "			</gml:Box>\n" +
                    "		</gml:boundedBy>\n" +
                    "		<x>-6813851.323435604</x>\n" +
                    "		<y>1651980.0473288011</y>\n" +
                    "		<variable>Red,Green,Blue</variable>\n" +
                    "		<value>63.0,92.0,132.0</value>\n" +
                    "	</" + MARTINIQUE + "_feature>\n" +
                    "</" + MARTINIQUE + "_layer>\n" +
                    "</msGMLOutput>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_GML_COV_ALIAS);

        expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<msGMLOutput xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                    "<SST_layer>\n" +
                    "	<SST_feature>\n" +
                    "		<gml:boundedBy>\n" +
                    "			<gml:Box srsName=\"CRS:84\">\n" +
                    "				<gml:coordinates>-120.41015625,-7.20703125 -120.41015625,-7.20703125</gml:coordinates>\n" +
                    "			</gml:Box>\n" +
                    "		</gml:boundedBy>\n" +
                    "		<x>-120.41015625</x>\n" +
                    "		<y>-7.20703125</y>\n" +
                    "		<variable>Color index</variable>\n" +
                    "		<value>201.0</value>\n" +
                    "	</SST_feature>\n" +
                    "</SST_layer>\n" +
                    "</msGMLOutput>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);
    }

    @Test
    public void testWMSGetFeatureInfoXMLGif() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_XML_FEAT);

        String expResult
                =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<FeatureInfo>\n" +
                    "	<Feature>\n" +
                    "		<Layer>Lakes</Layer>\n" +
                    "		<Name>Lakes</Name>\n" +
                    "		<ID>Lakes.1</ID>\n" +
                    "		<identifier>Lakes.1</identifier>\n" +
                    "		<the_geom>\n" +
                    "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))\n" +
                    "		</the_geom>\n" +
                    "		<FID>101</FID>\n" +
                    "		<NAME>Blue Lake</NAME>\n" +
                    "	</Feature>\n" +
                    "</FeatureInfo>";

        String result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_XML_COV);

        expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<FeatureInfo>\n" +
                    "	<Coverage>\n" +
                    "		<Layer>SSTMDE200305</Layer>\n" +
                    "		<values>\n" +
                    "			<Color_index>201.0</Color_index>\n" +
                    "		</values>\n" +
                    "	</Coverage>\n" +
                    "</FeatureInfo>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_XML_COV_ALIAS);

        expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<FeatureInfo>\n" +
                    "	<Coverage>\n" +
                    "		<Layer>SST</Layer>\n" +
                    "		<values>\n" +
                    "			<Color_index>201.0</Color_index>\n" +
                    "		</values>\n" +
                    "	</Coverage>\n" +
                    "</FeatureInfo>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_XML_COV2);

        expResult = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<FeatureInfo>\n" +
                    "	<Coverage>\n" +
                    "		<Layer>" + MARTINIQUE + "</Layer>\n" +
                    "		<values>\n" +
                    "			<Red>63.0</Red>\n" +
                    "			<Green>92.0</Green>\n" +
                    "			<Blue>132.0</Blue>\n" +
                    "		</values>\n" +
                    "	</Coverage>\n" +
                    "</FeatureInfo>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);
    }

    /**
     * Ensures that a valid GetLegendGraphic request returns indeed a
     * {@link BufferedImage}.
     *
     */
    @Test
    public void testWMSGetLegendGraphic() throws Exception {
        initLayerList();
        // Creates a valid GetLegendGraphic url.
        final URL getLegendUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETLEGENDGRAPHIC);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getLegendUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(200, image.getWidth());
        assertEquals(40, image.getHeight());
    }
    
    @Test
    public void testWMSGetLegendGraphicJson() throws Exception {
        initLayerList();
        // Creates a valid GetLegendGraphic url.
        final URL getLegendUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETLEGENDGRAPHIC_JSON);

        String expResult = getResourceAsString("org/constellation/ws/embedded/style1.json");
        String result = getStringResponse(getLegendUrl);
        assertNotNull(result);
        compareJSON(expResult, result);
    }

    /**
     * Ensures that a valid DescribeLayer request produces a valid document.
     */
    @Test
    @Order(order = 18)
    public void testWMSDescribeLayer() throws JAXBException, Exception {

        // Creates a valid DescribeLayer url.
        final URL describeUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_DESCRIBELAYER);

        // Try to marshall something from the response returned by the server.
        // The response should be a WMT_MS_Capabilities.
        final Object obj = unmarshallResponse(describeUrl);
        assertTrue(obj instanceof DescribeLayerResponseType);

        // Tests on the response
        final DescribeLayerResponseType desc = (DescribeLayerResponseType) obj;
        final List<LayerDescriptionType> layerDescs = desc.getLayerDescription();
        assertFalse(layerDescs.isEmpty());
        final List<TypeNameType> typeNames = layerDescs.get(0).getTypeName();
        assertFalse(typeNames.isEmpty());
        assertEquals(LAYER_TEST, typeNames.get(0).getCoverageName());
    }

    @Test
    @Order(order = 19)
    public void testWMSGetMapLakePostKvp() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?");

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("HeIgHt", "100");
        parameters.put("LaYeRs", LAKES);
        parameters.put("FoRmAt", "image/png");
        parameters.put("ReQuEsT", "GetMap");
        parameters.put("StYlEs", "");
        parameters.put("CrS", "CRS:84");
        parameters.put("BbOx", "-0.0025,-0.0025,0.0025,0.0025");
        parameters.put("VeRsIoN", "1.3.0");
        parameters.put("WiDtH", "100");

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromPostKvp(getMapUrl, parameters, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);
    }

    /**
     * Ensures that a GetMap in version 1.1.1 on a projected
     * {@linkplain CoordinateReferenceSystem CRS} provides the same image that a
     * GetMap in version 1.3.0 on the same CRS.
     */
    @Ignore
    @Order(order = 20)
    public void testGetMap111And130Projected() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        final URL getMap111Url = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_PROJ);
        final URL getMap130Url = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_PROJ);;

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image111 = getImageFromURL(getMap111Url, "image/png");
        final BufferedImage image130 = getImageFromURL(getMap130Url, "image/png");

        // Tests on the returned images.
        assertTrue(!(ImageTesting.isImageEmpty(image111)));
        assertEquals(1024, image111.getWidth());
        assertEquals(512, image111.getHeight());
        assertTrue(!(ImageTesting.isImageEmpty(image130)));
        assertEquals(1024, image130.getWidth());
        assertEquals(512, image130.getHeight());
        assertEquals(Commons.checksum(image111), Commons.checksum(image130));
    }

    /**
     * Test a GetMap request on a WMS version 1.1.1 for a geographical
     * {@linkplain CoordinateReferenceSystem CRS}.
     *
     * TODO: fix the implementation of the GetMap request concerning the
     * handling of geographical CRS (not WGS84) and do this test then.
     */
    @Ignore
    @Order(order = 21)
    public void testCRSGeographique111() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_GEO);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
    }

    /**
     * Verify the axis order for a GetMap in version 1.1.1 for the {@code WGS84}
     * CRS.
     */
    @Test
    @Order(order = 22)
    public void testGetMap111Epsg4326() throws Exception {

        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_EPSG_4326);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    /**
     * Verify the axis order for a GetMap in version 1.3.0 for the {@code WGS84}
     * CRS.
     */
    @Test
    @Order(order = 23)
    public void testGetMap130Epsg4326() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_EPSG_4326);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(512, image.getWidth());
        assertEquals(1024, image.getHeight());
        if (sstChecksumGeo == null) {
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertTrue(sstChecksumGeo.longValue() != Commons.checksum(image));
        }
    }

    /**
     * Verify the axis order for a GetMap in version 1.1.1 for the {@code WGS84}
     * CRS.
     */
    @Test
    @Order(order = 24)
    public void testGetMap111Crs84() throws Exception {

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_111_CRS_84);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    /**
     * Verify the axis order for a GetMap in version 1.3.0 for the {@code WGS84}
     * CRS.
     *
     * TODO: fix the implementation of the GetMap request concerning the axes
     * order, and do this test then.
     */
    @Test
    @Order(order = 25)
    public void testGetMap130Crs84() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_CRS_84);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    @Test
    @Order(order = 25)
    public void testGetMap130Tiff() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_TIFF);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }

        // try JPEG with transparent = FALSE
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_TIFF_TO_JPEG.replace("${transparent}", "FALSE"));

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/jpeg");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);

        // try JPEG with transparent = TRUE
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_TIFF_TO_JPEG.replace("${transparent}", "TRUE"));

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/jpeg");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
        
        // try TIFF
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_TIFF_TO_TIFF);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/tiff");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
    }

    @Test
    @Order(order = 25)
    public void testGetMap130ShapePoint() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_SHAPE_POINT);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 2);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    @Test
    @Order(order = 25)
    public void testGetMap130ShapePolygon() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_SHAPE_POLYGON);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    @Test
    @Order(order = 25)
    public void testGetMap130NetCDF() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_NETCDF);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        if (sstChecksumGeo == null) {
            sstChecksumGeo = Commons.checksum(image);
            assertTrue(ImageTesting.getNumColors(image) > 8);
        } else {
            assertEquals(sstChecksumGeo.longValue(), Commons.checksum(image));
        }
    }

    @Test
    @Order(order = 25)
    public void testGetMap130GeoJson() throws Exception {
        initLayerList();

        // Creates a valid GetMap url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_JSON_FEATURE);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_JSON_COLLECTION);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Tests on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 8);
    }

    @Test
    @Order(order = 26)
    public void testWMSGetFeatureInfoHTMLShape() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_HTML_FEAT);

        String expResult
                = "<html>\n"
                + "    <head>\n"
                + "        <title>GetFeatureInfo HTML output</title>\n"
                + "    </head>\n"
                + "    <style>\n"
                + "ul{\n"
                + "               margin-top: 0;\n"
                + "               margin-bottom: 0px;\n"
                + "           }\n"
                + "           .left-part{\n"
                + "               display:inline-block;\n"
                + "               width:350px;\n"
                + "               overflow:auto;\n"
                + "               white-space:nowrap;\n"
                + "           }\n"
                + "           .right-part{\n"
                + "               display:inline-block;\n"
                + "               width:600px;\n"
                + "               overflow: hidden;\n"
                + "           }\n"
                + "           .values{\n"
                + "               text-overflow: ellipsis;\n"
                + "               white-space:nowrap;\n"
                + "               display:block;\n"
                + "               overflow: hidden;\n"
                + "           }    </style>\n"
                + "    <body>\n"
                + "<h2>Lakes</h2><br/><h2>Lakes.1</h2></br><div><div class=\"left-part\"><ul>\n"
                + "<li>\n"
                + "identifier</li>\n"
                + "<li>\n"
                + "the_geom</li>\n"
                + "<li>\n"
                + "FID</li>\n"
                + "<li>\n"
                + "NAME</li>\n"
                + "</ul>\n"
                + "</div><div class=\"right-part\"><a class=\"values\" title=\"Lakes.1\">Lakes.1</a><a class=\"values\" title=\"MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))\">MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))</a><a class=\"values\" title=\"101\">101</a><a class=\"values\" title=\"Blue Lake\">Blue Lake</a></div></div><br/>    </body>\n"
                + "</html>";

        String result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_HTML_COV);

        expResult = "<html>\n" +
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
                    "<h2>SSTMDE200305</h2><br/><div><div class=\"left-part\"><ul>\n" +
                    "<li>\n" +
                    "Color index</li>\n" +
                    "</ul>\n" +
                    "</div><div class=\"right-part\">201.0<br/>\n" +
                    "</div></div><br/>    </body>\n" +
                    "</html>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_HTML_COV2);

        expResult = "<html>\n" +
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
                    "<h2>" + MARTINIQUE + "</h2><br/><div><div class=\"left-part\"><ul>\n" +
                    "<li>\n" +
                    "Red</li>\n" +
                    "<li>\n" +
                    "Green</li>\n" +
                    "<li>\n" +
                    "Blue</li>\n" +
                    "</ul>\n" +
                    "</div><div class=\"right-part\">63.0<br/>\n" +
                    "92.0<br/>\n" +
                    "132.0<br/>\n" +
                    "</div></div><br/>    </body>\n" +
                    "</html>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_HTML_COV_ALIAS);

        expResult = "<html>\n" +
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
                    "<h2>SST</h2><br/><div><div class=\"left-part\"><ul>\n" +
                    "<li>\n" +
                    "Color index</li>\n" +
                    "</ul>\n" +
                    "</div><div class=\"right-part\">201.0<br/>\n" +
                    "</div></div><br/>    </body>\n" +
                    "</html>";

        result = getStringResponse(gfi);

        assertNotNull(result);
        assertEquals(expResult, result);
    }

    @Test
    @Order(order = 27)
    public void testWMSGetFeatureInfoJSONShape() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        final URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT);

        String result = getStringResponse(gfi);
        assertNotNull(result);

        // Note: do not check directly text representation, as field ordering could arbitrarily change on serialization.
        final Map[] binding = new ObjectMapper().readValue(result, Map[].class);
        assertEquals("A single result should be returned", 1, binding.length);
        Map record = binding[0];
        assertEquals("layer property", "Lakes", record.get("layer"));
        assertEquals("type property", "feature", record.get("type"));
        record = (Map) record.get("feature");
        assertEquals("feature type property", "Lakes", record.get("type"));
        assertEquals("feature identifier property", "Lakes.1", record.get("identifier"));
        assertEquals("feature NAME property", "Blue Lake", record.get("NAME"));
        // TODO: proper geometric equality
        assertEquals("feature geometric property",
                "MULTIPOLYGON (((0.0006 -0.0018, 0.001 -0.0006, 0.0024 -0.0001, 0.0031 -0.0015, 0.0006 -0.0018), (0.0017 -0.0011, 0.0025 -0.0011, 0.0025 -0.0006, 0.0017 -0.0006, 0.0017 -0.0011)))",
                record.get("the_geom")
        );
    }

    @Test
    @Order(order = 28)
    public void testWMSGetFeatureInfoJSONProfileCoverage() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PROFILE_COV);

        String result = getStringResponse(gfi);
        assertNotNull(result);

        LOGGER.fine(result);

        // Note: do not check directly text representation, as field ordering could arbitrarily change on serialization/ and precison change with JDK version.
        Map binding = new ObjectMapper().readValue(result, Map.class);
        assertNotNull("result is empty", binding);
        List records = (List) binding.get("layers");
        assertEquals("A single layer result should be returned", 1, records.size());
        Map record = (Map) records.get(0);

        assertEquals("layer name property", "SSTMDE200305", record.get("name"));

        List datas = (List) record.get("data");

        assertEquals("A single data result should be returned", 1, datas.size());
        Map data = (Map) datas.get(0);

        assertEquals("min property", 199.0, data.get("min"));
        assertEquals("max property", 201.0, data.get("max"));

        List<Map> points = (List) data.get("points");

        assertEquals("9 points should be returned", 5, points.size());

        assertEquals("pt0 X property", 0,     (double)points.get(0).get("x"), 1e-1);
        assertEquals("pt0 Y property", 199.0, (double)points.get(0).get("y"), 1e-1);

        assertEquals("pt1 X property", 10.3,  (double)points.get(1).get("x"), 1e-1);
        assertEquals("pt1 Y property", 199.6, (double)points.get(1).get("y"), 1e-1);

        assertEquals("pt2 X property", 25.2,  (double)points.get(2).get("x"), 1e-1);
        assertEquals("pt2 Y property", 201.0,  (double)points.get(2).get("y"), 1e-1);

        assertEquals("pt3 X property", 37.1,  (double)points.get(3).get("x"), 1e-1);
        assertEquals("pt3 Y property", 201.0, (double)points.get(3).get("y"), 1e-1);

        assertEquals("pt4 X property", 53.4,  (double)points.get(4).get("x"), 1e-1);
        assertEquals("pt4 Y property", 200.0, (double)points.get(4).get("y"), 1e-1);

        gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PROFILE_COV_ALIAS);

        result = getStringResponse(gfi);
        assertNotNull(result);

        LOGGER.fine(result);

        // Note: do not check directly text representation, as field ordering could arbitrarily change on serialization/ and precison change with JDK version.
        binding = new ObjectMapper().readValue(result, Map.class);
        assertNotNull("result is empty", binding);
        records = (List) binding.get("layers");
        assertEquals("A single layer result should be returned", 1, records.size());
        record = (Map) records.get(0);

        assertEquals("layer name property", "SST", record.get("name"));
        assertEquals("layer alias property", "SST", record.get("alias"));
    }

    @Test
    public void testWMSGetFeatureInfoProfileNaNAny() throws Exception {
        final double[] expectedDistinctValues = { 2, NaN, 1 };
        testProfile(WMS_GETFEATUREINFO_PROFILE_NAN_ANY, values -> {
            // The assertion is quite complex, but it is difficult to know precisely how many values will be returned,
            // we opt for testing that distinct values arrive in a specific order (distinct until changed algorithm).
            final double[] distinctUntilChangedValues = new double[expectedDistinctValues.length];
            distinctUntilChangedValues[0] = values[0];
            for (int i = 1, j = 0 ; i < values.length ; i++) {
                final double valueDiff = Math.abs(values[i] - distinctUntilChangedValues[j]);
                if (Double.isNaN(values[i]) && Double.isFinite(expectedDistinctValues[j])
                        || Double.isFinite(values[i]) && Double.isNaN(expectedDistinctValues[j])
                        || valueDiff > 1e-4) {
                    if (++j >= distinctUntilChangedValues.length) throw new AssertionError(String.format(
                            "Profile values are invalid.%n" +
                                    "Distinct values expected in order: %s%n" +
                                    "But distinct consolidated values are: %s%n" +
                                    "Raw value array is: %s%n" +
                                    "Shift detected at raw value index: %d",
                            Arrays.toString(expectedDistinctValues), Arrays.toString(distinctUntilChangedValues), Arrays.toString(values), i));
                    distinctUntilChangedValues[j] = values[i];
                }
            }
            assertArrayEquals("Profile values", expectedDistinctValues, distinctUntilChangedValues, 1e-2);
        });
    }

    @Test
    public void testWMSGetFeatureInfoProfileNaNSegment() throws Exception {
        final String request = WMS_GETFEATUREINFO_PROFILE_NAN_SEGMENT;

        testProfile(request, values -> {
            assertTrue("Result should not be empty", values.length > 1);
            for (int i = 0 ; i < values.length ; i++) {
                assertTrue("All values should be NaN. Offending index: "+i, Double.isNaN(values[i]));
            }
        });
    }

    private void testProfile(String request, Consumer<double[]> assertYValues) throws Exception {
        // TODO: should be in a @BeforeEah method
        initLayerList();

        URL gfi = URI.create("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + request).toURL();
        URLConnection c = gfi.openConnection();
        String result = getStringResponse(c, 200);

        assertNotNull(result);
        LOGGER.fine(result);

        final CoverageProfileInfoFormat.Profile profile = new ObjectMapper().readValue(result, CoverageProfileInfoFormat.Profile.class);
        final List<CoverageProfileInfoFormat.XY> points = profile.layers.get(0).getData().get(0).points;
        final double[] values = points.stream().mapToDouble(CoverageProfileInfoFormat.XY::getY)
                .toArray();
        assertYValues.accept(values);
    }

    /**
     * Checks that no error happens when querying a profile outside validity domain of a data. Instead, an empty profile
     * should be returned.
     *
     * @see #WMS_GETFEATUREINFO_PROFILE_OUTSIDE  related query string
     */
    @Test
    public void testWMSGetFeatureInfoOutside() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_PROFILE_OUTSIDE);
        String result = getStringResponse(gfi);

        LOGGER.fine(result);

        final ObjectMapper mapper = new ObjectMapper();
        CoverageProfileInfoFormat.Profile profile = mapper.readValue(result, CoverageProfileInfoFormat.Profile.class);
        List<CoverageProfileInfoFormat.XY> points = profile.layers.get(0).getData().get(0).points;
        assertTrue(points.isEmpty());

        result = getStringResponse(new URL(gfi.toString().replace("ignore", "nan")));
        LOGGER.fine(result);
        profile = mapper.readValue(result, CoverageProfileInfoFormat.Profile.class);
        points = profile.layers.get(0).getData().get(0).points;
        assertEquals(2, points.size());
        assertTrue(Double.isNaN(points.get(0).y));
        assertTrue(Double.isNaN(points.get(1).y));
    }

    @Test
    public void testWMSGetFeatureInfoJSONAlias() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT_ALIAS).toURL();

        String expResult = getResourceAsString("org/constellation/ws/embedded/gfi1.json");
        String result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT_ALIAS2).toURL();

        expResult= getResourceAsString("org/constellation/ws/embedded/gfi2.json");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
    }
    
    @Test
    public void testWMSGetFeatureInfoGeoJSONAlias() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT_ALIAS.replaceAll("application/json", "application/geo%2Bjson")).toURL();

        String expResult = getResourceAsString("org/constellation/ws/embedded/gfi1.geojson");
        String result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT_ALIAS2.replaceAll("application/json", "application/geo%2Bjson")).toURL();

        expResult= getResourceAsString("org/constellation/ws/embedded/gfi2.geojson");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
    }

    @Test
    @Order(order = 28)
    public void testWMSGetFeatureInfoJSONPropertyName() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT_PROPNAME).toURL();

        String expResult = getResourceAsString("org/constellation/ws/embedded/gfi3.json");
        String result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

        // Creates a valid GetFeatureInfo url.
        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_MULTI_PROPNAME).toURL();

        expResult = getResourceAsString("org/constellation/ws/embedded/gfi4.json");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

        // Creates a valid GetFeatureInfo url.
        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_FEAT_INVERTED_PROPNAME).toURL();

        expResult = getResourceAsString("org/constellation/ws/embedded/gfi5.json");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

    }

    @Test
    public void testWMSGetFeatureInfoJSONCoverage() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_COV).toURL();

        String expResult = getResourceAsString("org/constellation/ws/embedded/gfi7.json");
        String result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
        
        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_COV_ALIAS).toURL();

        expResult = getResourceAsString("org/constellation/ws/embedded/gfi8.json");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_COV2).toURL();

        expResult = getResourceAsString("org/constellation/ws/embedded/gfi9.json");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
    }
    
    @Test
    public void testWMSGetFeatureInfoGeoJSONCoverage() throws Exception {
        initLayerList();
        // Creates a valid GetFeatureInfo url.
        URL gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_COV.replaceAll("application/json", "application/geo%2Bjson")).toURL();

        String expResult = getResourceAsString("org/constellation/ws/embedded/gfi7.geojson");
        String result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
        
        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_COV_ALIAS.replaceAll("application/json", "application/geo%2Bjson")).toURL();

        expResult = getResourceAsString("org/constellation/ws/embedded/gfi8.geojson");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);

        gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_COV2.replaceAll("application/json", "application/geo%2Bjson")).toURL();

        expResult = getResourceAsString("org/constellation/ws/embedded/gfi9.geojson");
        result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
    }
        
        
    @Test
    public void testWMSGetFeatureInfoJSONTime() throws Exception {
        initLayerList();
        URL gfi = new URI("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETFEATUREINFO_JSON_JSON).toURL();

        String expResult = getResourceAsString("org/constellation/ws/embedded/gfi6.json");
        String result = getStringResponse(gfi);
        assertNotNull(result);
        compareJSON(expResult, result);
    }

    @Test
    @Order(order = 29)
    public void testWMSGetMapALias() throws Exception {
        initLayerList();
        // Creates a valid GetMap url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_ALIAS);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_ALIAS2);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
    }

    /**
     * Ensures that a valid GetLegendGraphic request returns indeed a
     * {@link BufferedImage}.
     *
     * @throws java.io.Exception
     */
    @Test
    @Order(order = 29)
    public void testWMSGetLegendGraphicAlias() throws Exception {
        initLayerList();
        // Creates a valid GetLegendGraphic url.
        URL getLegendUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETLEGENDGRAPHIC_ALIAS);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getLegendUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(200, image.getWidth());
        assertEquals(40, image.getHeight());

        getLegendUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETLEGENDGRAPHIC_ALIAS2);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getLegendUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(200, image.getWidth());
        assertEquals(40, image.getHeight());
    }

    @Test
    @Order(order = 29)
    public void testWMSGetMapFilter() throws Exception {
        initLayerList();
        // Creates a valid GetLegendGraphic url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_JCOLL);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        Path p = CONFIG_DIR.resolve("JCOLL-FULL.png");
        writeInFile(getMapUrl, p);

        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_JCOLL_LAYER_FILTER);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        p = CONFIG_DIR.resolve("JCOLL-LAYER-FILTERED.png");
        writeInFile(getMapUrl, p);

        // this request must be equivalent to the last one, but the filter is passed throught the request in CQL instead of the configuration
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_JCOLL_REQUEST_CQL_FILTER);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        p = CONFIG_DIR.resolve("JCOLL-REQUEST-CQL-FILTERED.png");
        writeInFile(getMapUrl, p);

        // this request must be equivalent to the last one, but the filter is passed throught the request in XML instead of the configuration
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_JCOLL_REQUEST_FILTER);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        p = CONFIG_DIR.resolve("JCOLL-REQUEST-FILTERED.png");
        writeInFile(getMapUrl, p);

        System.out.println("");
    }
    
    @Test
    @Order(order = 29)
    public void testWMSGetMapFeatureFilter() throws Exception {
        initLayerList();
        // Creates a valid GetLegendGraphic url.
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_NP_REQUEST);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        Path p = CONFIG_DIR.resolve("NP-FULL.png");
        writeInFile(getMapUrl, p);

        // Creates a valid GetLegendGraphic url.
        getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_NP_REQUEST_CQL_FILTER);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        p = CONFIG_DIR.resolve("NP-FILTER.png");
        writeInFile(getMapUrl, p);
    }

    @Test
    @Order(order = 29)
    public void testWMSGetMapCustomDimension() throws Exception {
        initLayerList();
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_130_JCOLL_ELEVATION);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        Path p = CONFIG_DIR.resolve("JCOLL-ELEVATION.png");
        writeInFile(getMapUrl, p);

        System.out.println("");
    }
    
    @Test
    @Order(order = 29)
    public void testWMSGetMapLakeSensorStyle() throws Exception {
        initLayerList();
        // Creates a valid GetMap url.
        final URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms1?" + WMS_GETMAP_STYLE_SENSOR);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        final BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(100, image.getWidth());
        assertEquals(100, image.getHeight());
        assertTrue(ImageTesting.getNumColors(image) > 2);
    }
    
    @Test
    @Order(order = 29)
    public void testWMSGetMapOM2() throws Exception {
        initLayerList();
        URL getMapUrl = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?" + WMS_GETMAP_OM2);

        // Try to get a map from the url. The test is skipped in this method if it fails.
        BufferedImage image = getImageFromURL(getMapUrl, "image/png");

        // Test on the returned image.
        assertTrue(!(ImageTesting.isImageEmpty(image)));
        assertEquals(1024, image.getWidth());
        assertEquals(512, image.getHeight());

        Path p = CONFIG_DIR.resolve("OM2.png");
        writeInFile(getMapUrl, p);

        System.out.println("");
    }
    
    @Test
    @Order(order = 30)
    public void testNewInstance() throws Exception {
        initLayerList();

        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we build a new instance
         */
        URL niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms");

        // for a POST request
        URLConnection conec = niUrl.openConnection();

        final Details meta = new Details();
        meta.setIdentifier("wms3");
        meta.setName("OGC:WMS");
        meta.setDescription("Constellation Map Server");
        meta.setVersions(Arrays.asList("1.3.0", "1.1.1"));
        putJsonRequestObject(conec, meta);
        Object obj = unmarshallJsonResponse(conec, ServiceComplete.class);

        assertTrue(obj instanceof ServiceComplete);

        ServiceComplete result = (ServiceComplete) obj;
        assertEquals("wms3", result.getIdentifier());

        /*
         * we see the instance with a status NOT_STARTED
         */
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/all");

        // for a POST request
        conec = liUrl.openConnection();

        obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);
        InstanceReport result2 = (InstanceReport) obj;

        final Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList("1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, DEF_NB_LAYER, ServiceStatus.STARTED, "null/wms/default"));
        instances.add(new Instance(2, "wms1", "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1, ServiceStatus.STARTED, "null/wms/wms1"));
        instances.add(new Instance(3, "wms2", "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED, "null/wms/wms2"));
        instances.add(new Instance(4, "wms3", "OGC:WMS", "Constellation Map Server", "wms", versions, 0, ServiceStatus.STOPPED, "null/wms/wms3"));
        InstanceReport expResult2 = new InstanceReport(instances);
        expResult2.equals(obj);

        assertEquals(expResult2.getInstance("default").getLayersNumber(), result2.getInstance("default").getLayersNumber());
        assertEquals(expResult2.getInstance("default"), result2.getInstance("default"));
        assertEquals(expResult2, obj);

        /*
         * if we want to build the same new instance we receive an error
         */
        // for a POST request
        conec = niUrl.openConnection();
        putJsonRequestObject(conec, meta);
        obj = unmarshallJsonResponse(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Failure", "Instance already created");
        assertEquals(expResult, obj);
    }

    @Test
    @Order(order = 31)
    public void testStartInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we start the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/wms3/start");

        // for a POST request
        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallJsonResponsePost(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "WMS service \"wms3\" successfully started.");
        assertEquals(expResult, obj);

        /*
         * we verify tat the instance has now a status WORKING
         */
        URL liUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/all");

        // for a POST request
        conec = liUrl.openConnection();

        obj = unmarshallJsonResponse(conec, InstanceReport.class);

        assertTrue(obj instanceof InstanceReport);

        Set<Instance> instances = new HashSet<>();
        final List<String> versions = Arrays.asList("1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList("1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, DEF_NB_LAYER, ServiceStatus.STARTED, "null/wms/default"));
        instances.add(new Instance(2, "wms1", "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1, ServiceStatus.STARTED, "null/wms/wms1"));
        instances.add(new Instance(3, "wms2", "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED, "null/wms/wms2"));
        instances.add(new Instance(4, "wms3", "OGC:WMS", "Constellation Map Server", "wms", versions, 0, ServiceStatus.STARTED, "null/wms/wms3"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);

    }

    @Ignore
    @Order(order = 32)
    public void testConfigureInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we configure the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/wms3/config");

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
        niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/wms3/restart");

        // for a POST request
        conec = niUrl.openConnection();
        postRequestObject(conec, new SimpleValue(false), GenericDatabaseMarshallerPool.getInstance());
        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof AcknowlegementType);

        expResult = new AcknowlegementType("Success", "Service instance successfully restarted.");
        assertEquals(expResult, obj);

        URL gcDefaultURL = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/default?request=GetCapabilities&service=WMS&version=1.1.1");
        URL gcWms2URL = new URL("http://localhost:" + getCurrentPort() + "/WS/wms/wms3?request=GetCapabilities&service=WMS&version=1.1.1");

        String expCapabiliites = getStringResponse(gcDefaultURL.openConnection());
        String resCapabiliites = getStringResponse(gcWms2URL.openConnection());

        resCapabiliites = resCapabiliites.replace("wms3", "default");

        assertEquals(expCapabiliites, resCapabiliites);
    }

    @Test
    @Order(order = 33)
    public void testStopInstance() throws Exception {
        pool = GenericDatabaseMarshallerPool.getInstance();
        /*
         * we stop the instance created at the previous test
         */
        URL niUrl = new URL("http://localhost:" + getCurrentPort() + "/API/OGC/wms/wms3/stop");

        // for a POST request
        URLConnection conec = niUrl.openConnection();

        Object obj = unmarshallJsonResponsePost(conec, AcknowlegementType.class);

        assertTrue(obj instanceof AcknowlegementType);

        AcknowlegementType expResult = new AcknowlegementType("Success", "WMS service \"wms3\" successfully stopped.");
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
        final List<String> versions = Arrays.asList("1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList("1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, DEF_NB_LAYER, ServiceStatus.STARTED, "null/wms/default"));
        instances.add(new Instance(2, "wms1", "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1, ServiceStatus.STARTED, "null/wms/wms1"));
        instances.add(new Instance(3, "wms2", "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED, "null/wms/wms2"));
        instances.add(new Instance(4, "wms3", "OGC:WMS", "Constellation Map Server", "wms", versions, 0, ServiceStatus.STOPPED, "null/wms/wms3"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);
    }

    @Test
    @Order(order = 34)
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
        final List<String> versions = Arrays.asList("1.3.0", "1.1.1");
        final List<String> versions2 = Arrays.asList("1.3.0");
        instances.add(new Instance(1, "default", "OGC:WMS", "Constellation Map Server", "wms", versions, DEF_NB_LAYER, ServiceStatus.STARTED, "null/wms/default"));
        instances.add(new Instance(2, "wms1", "this is the default english capabilities", "Serveur Cartographique.  Contact: someone@geomatys.fr.  Carte haute qualité.", "wms", versions, 1, ServiceStatus.STARTED, "null/wms/wms1"));
        instances.add(new Instance(3, "wms2", "wms2", null, "wms", versions2, 13, ServiceStatus.STARTED, "null/wms/wms2"));
        InstanceReport expResult2 = new InstanceReport(instances);
        assertEquals(expResult2, obj);
    }

    public static void domCompare(final Object expected, String actual) throws Exception {

        String expectedStr;
        if (expected instanceof Path) {
            expectedStr = IOUtilities.toString((Path) expected);
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
