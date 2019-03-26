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

import org.apache.sis.xml.MarshallerPool;
import org.constellation.business.IDataBusiness;
import org.constellation.business.ILayerBusiness;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.IServiceBusiness;
import org.constellation.configuration.ConfigDirectory;
import org.constellation.admin.SpringHelper;
import org.constellation.api.ProviderType;
import org.constellation.dto.service.config.wxs.LayerContext;
import org.constellation.provider.DataProviders;
import org.constellation.provider.DataProviderFactory;
import org.apache.sis.test.xml.DocumentComparator;
import org.constellation.test.utils.Order;
import org.constellation.test.utils.TestEnvironment;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.ogc.xml.v110.FeatureIdType;
import org.geotoolkit.ows.xml.v110.ExceptionReport;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.sampling.xml.v100.SamplingPointType;
import org.geotoolkit.wfs.xml.ResultTypeType;
import org.geotoolkit.wfs.xml.ValueCollection;
import org.geotoolkit.wfs.xml.v110.FeatureCollectionType;
import org.geotoolkit.wfs.xml.v110.GetFeatureType;
import org.geotoolkit.wfs.xml.v110.InsertResultsType;
import org.geotoolkit.wfs.xml.v110.InsertedFeatureType;
import org.geotoolkit.wfs.xml.v110.QueryType;
import org.geotoolkit.wfs.xml.v110.TransactionResponseType;
import org.geotoolkit.wfs.xml.v110.TransactionSummaryType;
import org.geotoolkit.wfs.xml.v110.WFSCapabilitiesType;
import org.geotoolkit.wfs.xml.v200.DescribeStoredQueriesResponseType;
import org.geotoolkit.wfs.xml.v200.DescribeStoredQueriesType;
import org.geotoolkit.wfs.xml.v200.GetPropertyValueType;
import org.geotoolkit.wfs.xml.v200.ListStoredQueriesResponseType;
import org.geotoolkit.wfs.xml.v200.ListStoredQueriesType;
import org.geotoolkit.wfs.xml.v200.MemberPropertyType;
import org.geotoolkit.wfs.xml.v200.ValueCollectionType;
import org.geotoolkit.xsd.xml.v2001.Schema;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.parameter.ParameterValueGroup;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotoolkit.ogc.xml.v200.ResourceIdType;
import org.geotoolkit.wfs.xml.v200.ActionResultsType;
import org.geotoolkit.wfs.xml.v200.CreateStoredQueryResponseType;
import org.geotoolkit.wfs.xml.v200.CreatedOrModifiedFeatureType;
import org.geotoolkit.wfs.xml.v200.DropStoredQueryResponseType;
import org.geotoolkit.wfs.xml.v200.StoredQueryListItemType;
import org.apache.sis.util.logging.Logging;
import org.constellation.exception.ConstellationException;
import static org.constellation.provider.ProviderParameters.SOURCE_ID_DESCRIPTOR;
import static org.constellation.provider.ProviderParameters.getOrCreate;
import static org.constellation.provider.datastore.DataStoreProviderService.SOURCE_CONFIG_DESCRIPTOR;
import org.constellation.test.utils.TestDatabaseHandler;
import org.constellation.test.utils.TestRunner;
import static org.geotoolkit.db.AbstractJDBCFeatureStoreFactory.DATABASE;
import static org.geotoolkit.db.AbstractJDBCFeatureStoreFactory.HOST;
import static org.geotoolkit.db.AbstractJDBCFeatureStoreFactory.PASSWORD;
import static org.geotoolkit.db.AbstractJDBCFeatureStoreFactory.SCHEMA;
import static org.geotoolkit.db.AbstractJDBCFeatureStoreFactory.USER;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author Guilhem Legal (Geomatys)
 */
@RunWith(TestRunner.class)
public class WFSRequestTest extends AbstractGrizzlyServer {


    private static final String WFS_GETCAPABILITIES_URL_NO_SERV = "request=GetCapabilities&version=1.1.0";
    private static final String WFS_GETCAPABILITIES_URL_NO_SERV2 = "request=GetCapabilities&version=2.0.0";

    private static final String WFS_GETCAPABILITIES_URL_NO_VERS = "request=GetCapabilities&service=WFS";

    private static final String WFS_GETCAPABILITIES_URL = "request=GetCapabilities&version=1.1.0&service=WFS";

    private static final String WFS_GETCAPABILITIES_URL_AV = "request=GetCapabilities&acceptversions=10.0.0,2.0.0,1.1.0&service=WFS";

    private static final String WFS_GETCAPABILITIES_ERROR_URL = "request=GetCapabilities&version=1.3.0&service=WFS";

    private static final String WFS_GETFEATURE_URL = "request=getFeature&service=WFS&version=1.1.0&"
            + "typename=sa:SamplingPoint&namespace=xmlns(sa=http://www.opengis.net/sampling/1.0)&"
            + "filter=%3Cogc:Filter%20xmlns:ogc=%22http://www.opengis.net/ogc%22%20xmlns:gml=%22http://www.opengis.net/gml%22%3E"
            + "%3Cogc:PropertyIsEqualTo%3E"
            + "%3Cogc:PropertyName%3Egml:name%3C/ogc:PropertyName%3E"
            + "%3Cogc:Literal%3E10972X0137-PONT%3C/ogc:Literal%3E"
            + "%3C/ogc:PropertyIsEqualTo%3E"
            + "%3C/ogc:Filter%3E";

    private static final String WFS_GETFEATURE_URL_V2 = "request=getFeature&service=WFS&version=2.0.0&"
            + "typenames=sa:SamplingPoint&namespaces=xmlns(sa,http://www.opengis.net/sampling/1.0)&"
            + "filter=%3Cfes:Filter%20xmlns:fes=%22http://www.opengis.net/fes/2.0%22%20xmlns:gml=%22http://www.opengis.net/gml/3.2%22%3E"
            + "%3Cfes:PropertyIsEqualTo%3E"
            + "%3Cfes:ValueReference%3Egml:name%3C/fes:ValueReference%3E"
            + "%3Cfes:Literal%3E10972X0137-PONT%3C/fes:Literal%3E"
            + "%3C/fes:PropertyIsEqualTo%3E"
            + "%3C/fes:Filter%3E";

    private static final String WFS_GETFEATURE_SQ_URL = "typeName=tns:SamplingPoint&startindex=0&count=10&request=GetFeature&service=WFS"
            +                                           "&namespaces=xmlns(xml,http://www.w3.org/XML/1998/namespace),xmlns(tns,http://www.opengis.net/sampling/1.0),xmlns(wfs,http://www.opengis.net/wfs/2.0)"
            +                                           "&storedquery_id=urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType&version=2.0.0";

    private static final String WFS_DESCRIBE_FEATURE_TYPE_URL = "request=DescribeFeatureType&service=WFS&version=1.1.0&outputformat=text%2Fxml%3B+subtype%3D%22gml%2F3.1.1%22";
    private static final String WFS_DESCRIBE_FEATURE_TYPE_URL_V2 = "request=DescribeFeatureType&service=WFS&version=2.0.0&outputformat=text%2Fxml%3B+subtype%3D%22gml%2F3.2%22";

    private static final String WFS_GETFEATURE_JSON = "service=WFS&version=1.1.0&request=GetFeature&typename=SamplingPoint&outputFormat=application/json&srsName=epsg:3857&maxFeatures=2";

    private static final String WFS_GETFEATURE_JSON2 = "service=WFS&version=2.0.0&request=GetFeature&typenames=SamplingPoint&outputFormat=application/json&srsName=epsg:3857&count=2";

    private static final String WFS_GETFEATURE_CITE1 = "service=WFS&version=1.1.0&request=GetFeature&typename=sf:PrimitiveGeoFeature&namespace=xmlns%28sf=http://cite.opengeospatial.org/gmlsf%29&filter=%3Cogc:Filter%20xmlns:gml=%22http://www.opengis.net/gml%22%20xmlns:ogc=%22http://www.opengis.net/ogc%22%3E%3Cogc:PropertyIsEqualTo%3E%3Cogc:PropertyName%3E//gml:description%3C/ogc:PropertyName%3E%3Cogc:Literal%3Edescription-f008%3C/ogc:Literal%3E%3C/ogc:PropertyIsEqualTo%3E%3C/ogc:Filter%3E";

    private static final String WFS_GETFEATURE_CITE2 = "service=WFS&version=1.1.0&request=GetFeature&typename=sf:PrimitiveGeoFeature&namespace=xmlns(sf=http://cite.opengeospatial.org/gmlsf)&filter=%3Cogc:Filter%20xmlns:ogc=%22http://www.opengis.net/ogc%22%3E%3Cogc:PropertyIsEqualTo%3E%3Cogc:PropertyName%3E*%5B1%5D%3C/ogc:PropertyName%3E%3Cogc:Literal%3Edescription-f001%3C/ogc:Literal%3E%3C/ogc:PropertyIsEqualTo%3E%3C/ogc:Filter%3E";

    private static String EPSG_VERSION;

    private static boolean initialized = false;

    private static boolean localdb_active = true;

    private static Path primitive;
    private static Path entity;
    private static Path aggregate;
    private static Path citeGmlsf0;
    private static Path shapefiles;

    @BeforeClass
    public static void initTestDir() throws IOException, URISyntaxException {
        controllerConfiguration = WFSControllerConfig.class;
        File workspace = ConfigDirectory.setupTestEnvironement("WFSRequestTest").toFile();
        primitive = TestEnvironment.initWorkspaceData(workspace.toPath(), TestEnvironment.TestResources.WFS110_PRIMITIVE);
        entity = TestEnvironment.initWorkspaceData(workspace.toPath(), TestEnvironment.TestResources.WFS110_ENTITY);
        aggregate = TestEnvironment.initWorkspaceData(workspace.toPath(), TestEnvironment.TestResources.WFS110_AGGREGATE);
        citeGmlsf0 = TestEnvironment.initWorkspaceData(workspace.toPath(), TestEnvironment.TestResources.WFS110_CITE_GMLSF0);
        shapefiles = TestEnvironment.initWorkspaceData(workspace.toPath(), TestEnvironment.TestResources.WMS111_SHAPEFILES);
    }
    /**
     * Initialize the list of layers from the defined providers in Constellation's configuration.
     */
    @Before
    public void initPool() {

        if (!initialized) {
            try {
                startServer(null);

                layerBusiness.removeAll();
                serviceBusiness.deleteAll();
                dataBusiness.deleteAll();
                providerBusiness.removeAll();

                final File outputDir = initDataDirectory();
                String path = outputDir.getAbsolutePath();
                LOGGER.info("DATA PATH:" + path);

                final DataProviderFactory featfactory = DataProviders.getFactory("data-store");

                // Defines a PostGis data provider
                localdb_active = TestDatabaseHandler.hasLocalDatabase();
                if (localdb_active) {
                    final ParameterValueGroup source = featfactory.getProviderDescriptor().createValue();
                    source.parameter(SOURCE_ID_DESCRIPTOR.getName().getCode()).setValue("postgisSrc");

                    final ParameterValueGroup choice = getOrCreate(SOURCE_CONFIG_DESCRIPTOR,source);
                    final ParameterValueGroup pgconfig = choice.addGroup("PostgresParameters");
                    pgconfig.parameter(DATABASE .getName().getCode()).setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_name"));
                    pgconfig.parameter(HOST     .getName().getCode()).setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_host"));
                    pgconfig.parameter(SCHEMA   .getName().getCode()).setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_schema"));
                    pgconfig.parameter(USER     .getName().getCode()).setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_user"));
                    pgconfig.parameter(PASSWORD .getName().getCode()).setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_pass"));

                    //add a custom sql query layer
                    final ParameterValueGroup layer = source.addGroup("Layer");
                    layer.parameter("name").setValue("CustomSQLQuery");
                    layer.parameter("language").setValue("CUSTOM-SQL");
                    layer.parameter("statement").setValue("SELECT name as nom, \"pointProperty\" as geom FROM \"PrimitiveGeoFeature\" ");

                    providerBusiness.storeProvider("postgisSrc", null, ProviderType.LAYER, "data-store", source);

                    dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf2", "AggregateGeoFeature"), "postgisSrc", "VECTOR", false, true, null, null);
                    dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf2", "PrimitiveGeoFeature"), "postgisSrc", "VECTOR", false, true, null, null);
                    dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf2", "EntitéGénérique"),     "postgisSrc", "VECTOR", false, true, null, null);
                    dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf2", "CustomSQLQuery"),      "postgisSrc", "VECTOR", false, true, null, null);
                }

                // Defines a GML data provider
                ParameterValueGroup source = featfactory.getProviderDescriptor().createValue();
                source.parameter(SOURCE_ID_DESCRIPTOR.getName().getCode()).setValue("primGMLSrc");

                ParameterValueGroup choice = getOrCreate(SOURCE_CONFIG_DESCRIPTOR,source);
                ParameterValueGroup pgconfig = choice.addGroup("GMLParameters");
                pgconfig.parameter("identifier").setValue("gml");
                pgconfig.parameter("path").setValue(primitive.toUri());
                pgconfig.parameter("sparse").setValue(Boolean.TRUE);
                pgconfig.parameter("xsd").setValue(citeGmlsf0.toUri().toURL());
                pgconfig.parameter("xsdtypename").setValue("PrimitiveGeoFeature");
                pgconfig.parameter("longitudeFirst").setValue(Boolean.TRUE);

                providerBusiness.storeProvider("primGMLSrc", null, ProviderType.LAYER, "data-store", source);
                dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf", "PrimitiveGeoFeature"), "primGMLSrc", "VECTOR", false, true, null, null);


                source = featfactory.getProviderDescriptor().createValue();
                source.parameter(SOURCE_ID_DESCRIPTOR.getName().getCode()).setValue("entGMLSrc");

                choice = getOrCreate(SOURCE_CONFIG_DESCRIPTOR,source);
                pgconfig = choice.addGroup("GMLParameters");
                pgconfig.parameter("identifier").setValue("gml");
                pgconfig.parameter("path").setValue(entity.toUri());
                pgconfig.parameter("sparse").setValue(Boolean.TRUE);
                pgconfig.parameter("xsd").setValue(citeGmlsf0.toUri().toURL());
                pgconfig.parameter("xsdtypename").setValue("EntitéGénérique");
                pgconfig.parameter("longitudeFirst").setValue(Boolean.TRUE);
                providerBusiness.storeProvider("entGMLSrc", null, ProviderType.LAYER, "data-store", source);
                dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf", "EntitéGénérique"),     "entGMLSrc", "VECTOR", false, true, null, null);


                source = featfactory.getProviderDescriptor().createValue();
                source.parameter(SOURCE_ID_DESCRIPTOR.getName().getCode()).setValue("aggGMLSrc");

                choice = getOrCreate(SOURCE_CONFIG_DESCRIPTOR,source);
                pgconfig = choice.addGroup("GMLParameters");
                pgconfig.parameter("identifier").setValue("gml");
                pgconfig.parameter("path").setValue(aggregate.toUri());
                pgconfig.parameter("sparse").setValue(Boolean.TRUE);
                pgconfig.parameter("xsd").setValue(citeGmlsf0.toUri().toURL());
                pgconfig.parameter("xsdtypename").setValue("AggregateGeoFeature");
                pgconfig.parameter("longitudeFirst").setValue(Boolean.TRUE);
                providerBusiness.storeProvider("aggGMLSrc", null, ProviderType.LAYER, "data-store", source);
                dataBusiness.create(new QName("http://cite.opengeospatial.org/gmlsf", "AggregateGeoFeature"), "aggGMLSrc", "VECTOR", false, true, null, null);


                final ParameterValueGroup sourcef = featfactory.getProviderDescriptor().createValue();
                sourcef.parameter("id").setValue("shapeSrc");

                final ParameterValueGroup choice2 = getOrCreate(SOURCE_CONFIG_DESCRIPTOR, sourcef);
                final ParameterValueGroup shpconfig = choice2.addGroup("ShapefileParametersFolder");

                shpconfig.parameter("path").setValue(shapefiles.toUri());

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

                final String url = "jdbc:derby:memory:TestWFSRequestOM";
                final DefaultDataSource ds = new DefaultDataSource(url + ";create=true");
                Connection con = ds.getConnection();
                DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
                String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
                sql = sql.replace("$SCHEMA", "");
                sr.run(sql);
                sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));
                con.close();
                ds.shutdown();

                final ParameterValueGroup sourceOM = featfactory.getProviderDescriptor().createValue();
                sourceOM.parameter("id").setValue("omSrc");

                final ParameterValueGroup choiceOM = getOrCreate(SOURCE_CONFIG_DESCRIPTOR, sourceOM);
                final ParameterValueGroup omconfig = choiceOM.addGroup("SOSDBParameters");
                omconfig.parameter("sgbdtype").setValue("derby");
                omconfig.parameter("derbyurl").setValue(url);

                providerBusiness.storeProvider("omSrc", null, ProviderType.LAYER, "data-store", sourceOM);
                dataBusiness.create(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint"), "omSrc", "VECTOR", false, true, null, null);

                final LayerContext config = new LayerContext();
                config.getCustomParameters().put("transactionSecurized", "false");
                config.getCustomParameters().put("transactional", "true");

                serviceBusiness.create("wfs", "default", config, null);

                if (localdb_active) {
                   layerBusiness.add("AggregateGeoFeature", "http://cite.opengeospatial.org/gmlsf2", "postgisSrc", null, "default", "WFS", null);
                   layerBusiness.add("PrimitiveGeoFeature", "http://cite.opengeospatial.org/gmlsf2", "postgisSrc", null, "default", "WFS", null);
                   layerBusiness.add("EntitéGénérique",     "http://cite.opengeospatial.org/gmlsf2", "postgisSrc", null, "default", "WFS", null);
                   layerBusiness.add("CustomSQLQuery",      "http://cite.opengeospatial.org/gmlsf2", "postgisSrc", null, "default", "WFS", null);
               }

                layerBusiness.add("AggregateGeoFeature", "http://cite.opengeospatial.org/gmlsf", "aggGMLSrc", null, "default", "wfs", null);
                layerBusiness.add("PrimitiveGeoFeature", "http://cite.opengeospatial.org/gmlsf", "primGMLSrc", null, "default", "wfs", null);
                layerBusiness.add("EntitéGénérique",     "http://cite.opengeospatial.org/gmlsf", "entGMLSrc", null, "default", "wfs", null);

                layerBusiness.add("SamplingPoint",       "http://www.opengis.net/sampling/1.0",  "omSrc",      null, "default", "wfs", null);
                layerBusiness.add("BuildingCenters",     "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("BasicPolygons",       "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("Bridges",             "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("Streams",             "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("Lakes",               "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("NamedPlaces",         "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("Buildings",           "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("RoadSegments",        "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("DividedRoutes",       "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("Forests",             "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("MapNeatline",         "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);
                layerBusiness.add("Ponds",               "http://www.opengis.net/gml",       "shapeSrc",   null, "default", "wfs", null);

                serviceBusiness.create("wfs", "test", config, null);
                layerBusiness.add("AggregateGeoFeature", "http://cite.opengeospatial.org/gmlsf", "aggGMLSrc",  null, "test", "wfs", null);
                layerBusiness.add("PrimitiveGeoFeature", "http://cite.opengeospatial.org/gmlsf", "primGMLSrc", null, "test", "wfs", null);
                layerBusiness.add("EntitéGénérique",     "http://cite.opengeospatial.org/gmlsf", "entGMLSrc",  null, "test", "wfs", null);

                layerBusiness.add("SamplingPoint",       "http://www.opengis.net/sampling/1.0",  "omSrc",      null, "test", "wfs", null);
                layerBusiness.add("BuildingCenters",     "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("BasicPolygons",       "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("Bridges",             "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("Streams",             "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("Lakes",               "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("NamedPlaces",         "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("Buildings",           "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("RoadSegments",        "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("DividedRoutes",       "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("Forests",             "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("MapNeatline",         "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);
                layerBusiness.add("Ponds",               "http://www.opengis.net/gml",       "shapeSrc",   null, "test", "wfs", null);


                final LayerContext config2 = new LayerContext();
                config2.getCustomParameters().put("transactionSecurized", "false");
                config2.getCustomParameters().put("transactional", "true");

                serviceBusiness.create("wfs", "test1", config, null);
                layerBusiness.add("SamplingPoint",       "http://www.opengis.net/sampling/1.0",  "omSrc",      null, "test1", "wfs", null);
                layerBusiness.add("BuildingCenters",     "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("BasicPolygons",       "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("Bridges",             "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("Streams",             "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("Lakes",               "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("NamedPlaces",         "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("Buildings",           "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("RoadSegments",        "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("DividedRoutes",       "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("Forests",             "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("MapNeatline",         "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);
                layerBusiness.add("Ponds",               "http://www.opengis.net/gml",       "shapeSrc",   null, "test1", "wfs", null);


                EPSG_VERSION = CRS.getVersion("EPSG").toString();
                pool = new MarshallerPool(JAXBContext.newInstance("org.geotoolkit.wfs.xml.v110"   +
                        ":org.geotoolkit.ogc.xml.v110"  +
                        ":org.geotoolkit.wfs.xml.v200"  +
                        ":org.geotoolkit.gml.xml.v311"  +
                        ":org.geotoolkit.xsd.xml.v2001" +
                        ":org.geotoolkit.sampling.xml.v100" +
                        ":org.apache.sis.internal.jaxb.geometry"), null);

                serviceBusiness.start("wfs", "default");
                serviceBusiness.start("wfs", "test");
                serviceBusiness.start("wfs", "test1");
                waitForRestStart("wfs","default");
                waitForRestStart("wfs","test");
                waitForRestStart("wfs","test1");

                initialized = true;
            } catch (Exception ex) {
                Logging.getLogger("org.constellation.ws.embedded").log(Level.SEVERE, null, ex);
            }
        }
    }

    @AfterClass
    public static void shutDown() {
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
        ConfigDirectory.shutdownTestEnvironement("WFSRequestTest");
        File f = new File("derby.log");
        if (f.exists()) {
            f.delete();
        }
        stopServer();
    }

    @Test
    @Order(order=1)
    public void testWFSGetCapabilities() throws Exception {

        initPool();

        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_URL);

        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WFSCapabilitiesType);

        WFSCapabilitiesType responseCaps = (WFSCapabilitiesType)obj;
        String currentUrl =  responseCaps.getOperationsMetadata().getOperation("GetCapabilities").getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref();
        assertEquals("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?", currentUrl);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/test?" + WFS_GETCAPABILITIES_URL);
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WFSCapabilitiesType);

        responseCaps = (WFSCapabilitiesType)obj;
        currentUrl =  responseCaps.getOperationsMetadata().getOperation("GetCapabilities").getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref();
        assertEquals("http://localhost:"+ getCurrentPort() + "/WS/wfs/test?", currentUrl);


        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_URL);

        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WFSCapabilitiesType);
        responseCaps = (WFSCapabilitiesType)obj;
        currentUrl =  responseCaps.getOperationsMetadata().getOperation("GetCapabilities").getDCP().get(0).getHTTP().getGetOrPost().get(0).getHref();
        assertEquals("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?", currentUrl);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_ERROR_URL);
        obj = unmarshallResponse(getCapsUrl);
        assertTrue("unexpected type:" + obj.getClass().getName(), obj instanceof ExceptionReport);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_URL_AV);
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.WFSCapabilitiesType);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_URL_NO_SERV);
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof org.geotoolkit.ows.xml.v100.ExceptionReport);
        org.geotoolkit.ows.xml.v100.ExceptionReport report100 = (org.geotoolkit.ows.xml.v100.ExceptionReport) obj;
        assertEquals("1.0.0", report100.getVersion());

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_URL_NO_SERV2);
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof ExceptionReport);
        ExceptionReport report200 = (ExceptionReport) obj;
        assertEquals("2.0.0", report200.getVersion());

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETCAPABILITIES_URL_NO_VERS);
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.WFSCapabilitiesType);
    }

    /**
     */
    @Test
    @Order(order=2)
    public void testWFSGetFeaturePOST() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");

        final List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        final GetFeatureType request = new GetFeatureType("WFS", "1.1.0", null, 2, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.1.1\"");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof FeatureCollectionType);

    }

    @Test
    @Order(order=3)
    public void testWFSGetFeaturePOSTV2() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");

        final List<org.geotoolkit.wfs.xml.v200.QueryType> queries = new ArrayList<>();
        queries.add(new org.geotoolkit.wfs.xml.v200.QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        final org.geotoolkit.wfs.xml.v200.GetFeatureType request = new org.geotoolkit.wfs.xml.v200.GetFeatureType("WFS", "2.0.0", null, null, 2, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.2.1\"");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof org.geotoolkit.wfs.xml.v200.FeatureCollectionType);

    }

    /**
     */
    @Test
    @Order(order=4)
    public void testWFSGetFeatureGET() throws Exception {
        final URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_URL);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        Object obj = unmarshallResponse(getfeatsUrl);

        assertTrue(obj instanceof FeatureCollectionType);

        FeatureCollectionType feat = (FeatureCollectionType) obj;
        assertEquals(1, feat.getFeatureMember().size());

        assertTrue("expected samplingPoint but was:" +  feat.getFeatureMember().get(0),
                feat.getFeatureMember().get(0).getAbstractFeature() instanceof SamplingPointType);
        SamplingPointType sp = (SamplingPointType) feat.getFeatureMember().get(0).getAbstractFeature();

        assertEquals("10972X0137-PONT", sp.getName().getCode());
    }

    @Test
    @Order(order=5)
    public void testWFSGetFeatureGET2() throws Exception {
        final URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_URL_V2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        Object obj = unmarshallResponse(getfeatsUrl);

        assertTrue("was:" + obj, obj instanceof org.geotoolkit.wfs.xml.v200.FeatureCollectionType);

        org.geotoolkit.wfs.xml.v200.FeatureCollectionType feat = (org.geotoolkit.wfs.xml.v200.FeatureCollectionType) obj;
        assertEquals(1, feat.getMember().size());

        MemberPropertyType member = feat.getMember().get(0);

        final JAXBElement element = (JAXBElement) member.getContent().get(0);

        assertTrue("expected samplingPoint but was:" +  element.getValue(), element.getValue() instanceof SamplingPointType);
        SamplingPointType sp = (SamplingPointType) element.getValue();

        // assertEquals("10972X0137-PONT", sp.getName()); TODO name attribute is moved to namespace GML 3.2 so the java binding does not match
    }

    @Test
    @Order(order=6)
    public void testWFSGetFeatureGETStoredQuery() throws Exception {
        final URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_SQ_URL);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        final URLConnection conec = getfeatsUrl.openConnection();

        String xmlResult    = getStringResponse(conec);
        String xmlExpResult = getStringFromFile("org/constellation/wfs/xml/samplingPointCollection-3v2.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");
        domCompare(xmlResult, xmlExpResult);
    }


    /**
     */
    @Test
    @Order(order=7)
    public void testWFSDescribeFeatureGET() throws Exception {
        initPool();

        URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_DESCRIBE_FEATURE_TYPE_URL);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        Object obj = unmarshallResponse(getfeatsUrl);

        assertTrue(obj instanceof Schema);

        Schema schema = (Schema) obj;

        assertEquals(3, schema.getIncludeOrImportOrRedefine().size());

        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/test?" + WFS_DESCRIBE_FEATURE_TYPE_URL_V2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        obj = unmarshallResponse(getfeatsUrl);

        assertTrue("was:" + obj, obj instanceof Schema);

        schema = (Schema) obj;

        assertEquals(3, schema.getIncludeOrImportOrRedefine().size());


    }

    /**
     */
    @Test
    @Order(order=8)
    public void testWFSTransactionInsert() throws Exception {
        initPool();

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/Insert-SamplingPoint-1.xml");
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        TransactionResponseType result = (TransactionResponseType) obj;

        TransactionSummaryType sum        = new TransactionSummaryType(2, 0, 0);
        List<InsertedFeatureType> insertedFeatures = new ArrayList<>();
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-007"), null));
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-008"), null));
        InsertResultsType insertResult    = new InsertResultsType(insertedFeatures);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, insertResult, "1.1.0");

        assertEquals(ExpResult, result);


        /**
         * We verify that the 2 new samplingPoint are inserted
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        GetFeatureType request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.1.1\"");

        // for a POST request
        conec = getCapsUrl.openConnection();

        postRequestObject(conec, request);
        String xmlResult    = getStringResponse(conec);
        String xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-1.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);

        // for a POST request
        conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/Insert-SamplingPoint-2.xml");

        // Try to unmarshall something from the response returned by the server.
        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        result = (TransactionResponseType) obj;

        sum              = new TransactionSummaryType(2, 0, 0);
        insertedFeatures = new ArrayList<>();
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-009"), null));
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-010"), null));
        insertResult    = new InsertResultsType(insertedFeatures);
        ExpResult = new TransactionResponseType(sum, null, insertResult, "1.1.0");

        assertEquals(ExpResult, result);

        /**
         * We verify that the 2 new samplingPoint are inserted
         */
        queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.1.1\"");

        // for a POST request
        conec = getCapsUrl.openConnection();

        postRequestObject(conec, request);

        // Try to unmarshall something from the response returned by the server.
        xmlResult    = getStringResponse(conec);

        xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-2.xml");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);


        // for a POST request
        conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/Insert-SamplingPoint-3.xml");

        // Try to unmarshall something from the response returned by the server.
        obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        result = (TransactionResponseType) obj;

        sum              = new TransactionSummaryType(2, 0, 0);
        insertedFeatures = new ArrayList<>();
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-011"), null));
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-012"), null));
        insertResult    = new InsertResultsType(insertedFeatures);
        ExpResult = new TransactionResponseType(sum, null, insertResult, "1.1.0");

        assertEquals(ExpResult, result);

        /**
         * We verify that the 2 new samplingPoint are inserted
         */
        queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null));
        request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.1.1\"");

        // for a POST request
        conec = getCapsUrl.openConnection();

        postRequestObject(conec, request);

        // Try to unmarshall something from the response returned by the server.
        xmlResult    = getStringResponse(conec);
        xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-3.xml");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);

    }

    @Test
    @Order(order=9)
    public void testWFSTransactionUpdate() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/Update-NamedPlaces-1.xml");

        // Try to unmarshall something from the response returned by the server.
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        TransactionResponseType result = (TransactionResponseType) obj;

        TransactionSummaryType sum              = new TransactionSummaryType(0, 1, 0);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, null, "1.1.0");

        assertEquals(ExpResult, result);


        /**
         * We verify that the namedPlaces have been changed
         */
        List<QueryType> queries = new ArrayList<>();
        queries.add(new QueryType(null, Arrays.asList(new QName("http://www.opengis.net/gml", "NamedPlaces")), null));
        GetFeatureType request = new GetFeatureType("WFS", "1.1.0", null, Integer.MAX_VALUE, queries, ResultTypeType.RESULTS, "text/xml; subtype=\"gml/3.1.1\"");

        // for a POST request
        conec = getCapsUrl.openConnection();

        postRequestObject(conec, request);

        // Try to unmarshall something from the response returned by the server.
        String xmlResult    = getStringResponse(conec);
        String xmlExpResult = getStringFromFile("org/constellation/xml/namedPlacesCollection-1.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=10)
    public void testWFSListStoredQueries() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");

        final ListStoredQueriesType request = new ListStoredQueriesType("WFS", "2.0.0", null);

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof ListStoredQueriesResponseType);

    }

    @Test
    @Order(order=11)
    public void testWFSDescribeStoredQueries() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");

        final DescribeStoredQueriesType request = new DescribeStoredQueriesType("WFS", "2.0.0", null, Arrays.asList("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureById"));

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        Object obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof DescribeStoredQueriesResponseType);

    }

    @Test
    @Order(order=12)
    public void testWFSGetPropertyValue() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?");

         /**
         * Test 1 : query on typeName samplingPoint with HITS
         */
        org.geotoolkit.wfs.xml.v200.QueryType query = new org.geotoolkit.wfs.xml.v200.QueryType(null, Arrays.asList(new QName("http://www.opengis.net/sampling/1.0", "SamplingPoint")), null);
        String valueReference = "sampledFeature";
        GetPropertyValueType request = new GetPropertyValueType("WFS", "2.0.0", null, null, Integer.MAX_VALUE, query, ResultTypeType.HITS, "text/xml; subtype=\"gml/3.2.1\"",valueReference);
        request.setValueReference(valueReference);

         // for a POST request
        URLConnection conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        Object result = unmarshallResponse(conec);

        assertTrue("unexpected type: " + result.getClass().getName() + "\n" + result, result instanceof ValueCollectionType);

        assertTrue(result instanceof ValueCollection);
        assertEquals(12, ((ValueCollection)result).getNumberReturned());

        /**
         * Test 2 : query on typeName samplingPoint with RESULTS
         */
        request.setResultType(ResultTypeType.RESULTS);
        conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        String sresult = getStringResponse(conec);

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.embedded.ValueCollectionOM1.xml"));
        domCompare(sresult, expectedResult);

        /**
         * Test 3 : query on typeName samplingPoint with RESULTS
         */
        valueReference = "position";
        request.setValueReference(valueReference);
        conec = getCapsUrl.openConnection();
        postRequestObject(conec, request);
        sresult = getStringResponse(conec);

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.embedded.ValueCollectionOM2.xml"));
        domCompare(sresult, expectedResult);

    }

    @Test
    @Order(order=13)
    public void testWFSGetCapabilitiesREST() throws Exception {
        initPool();

        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0");

        Object obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof WFSCapabilitiesType);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0");
        obj = unmarshallResponse(getCapsUrl);
        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.WFSCapabilitiesType);
    }

    @Test
    @Order(order=14)
    public void testWFSDescribeFeatureREST() throws Exception {
        initPool();

        URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/schema");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        System.out.println(">>>>>>>>>>>>>>>>>>>>>>><" + getStringResponse(getfeatsUrl.openConnection()));

        Object obj = unmarshallResponse(getfeatsUrl);

        assertTrue(obj instanceof Schema);

        Schema schema = (Schema) obj;
        assertEquals(3, schema.getIncludeOrImportOrRedefine().size());

        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/test/2.0.0/schema");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        obj = unmarshallResponse(getfeatsUrl);

        assertTrue("was:" + obj, obj instanceof Schema);

        schema = (Schema) obj;

        assertEquals(3, schema.getIncludeOrImportOrRedefine().size());

        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/test/2.0.0/BasicPolygons.xsd");
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

         obj = unmarshallResponse(getfeatsUrl);

        assertTrue("was:" + obj, obj instanceof Schema);

        schema = (Schema) obj;
        assertEquals(1, schema.getElements().size());
        Assert.assertNotNull(schema.getElementByName("BasicPolygons"));

    }

    private static final String WFS_GETFEATURE_FILTER =
              "filter=%3Cogc:Filter%20xmlns:ogc=%22http://www.opengis.net/ogc%22%20xmlns:gml=%22http://www.opengis.net/gml%22%3E"
            + "%3Cogc:PropertyIsEqualTo%3E"
            + "%3Cogc:PropertyName%3Egml:name%3C/ogc:PropertyName%3E"
            + "%3Cogc:Literal%3E10972X0137-PONT%3C/ogc:Literal%3E"
            + "%3C/ogc:PropertyIsEqualTo%3E"
            + "%3C/ogc:Filter%3E";

    private static final String WFS_GETFEATURE_FILTER_V2 =
            "filter=%3Cfes:Filter%20xmlns:fes=%22http://www.opengis.net/fes/2.0%22%20xmlns:gml=%22http://www.opengis.net/gml/3.2%22%3E"
            + "%3Cfes:PropertyIsEqualTo%3E"
            + "%3Cfes:ValueReference%3Egml:name%3C/fes:ValueReference%3E"
            + "%3Cfes:Literal%3E10972X0137-PONT%3C/fes:Literal%3E"
            + "%3C/fes:PropertyIsEqualTo%3E"
            + "%3C/fes:Filter%3E";
    @Test
    @Order(order=15)
    public void testWFSGetFeatureREST() throws Exception {
        URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint?" + WFS_GETFEATURE_FILTER);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        Object obj = unmarshallResponse(getfeatsUrl);

        assertTrue(obj instanceof FeatureCollectionType);

        FeatureCollectionType feat = (FeatureCollectionType) obj;
        assertEquals(1, feat.getFeatureMember().size());

        assertTrue("expected samplingPoint but was:" +  feat.getFeatureMember().get(0),
                feat.getFeatureMember().get(0).getAbstractFeature() instanceof SamplingPointType);
        SamplingPointType sp = (SamplingPointType) feat.getFeatureMember().get(0).getAbstractFeature();

        assertEquals("10972X0137-PONT", sp.getName().getCode());


        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint?" + WFS_GETFEATURE_FILTER_V2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        obj = unmarshallResponse(getfeatsUrl);

        assertTrue("was:" + obj, obj instanceof org.geotoolkit.wfs.xml.v200.FeatureCollectionType);

        org.geotoolkit.wfs.xml.v200.FeatureCollectionType feat2 = (org.geotoolkit.wfs.xml.v200.FeatureCollectionType) obj;
        assertEquals(1, feat2.getMember().size());

        MemberPropertyType member = feat2.getMember().get(0);

        final JAXBElement element = (JAXBElement) member.getContent().get(0);

        assertTrue("expected samplingPoint but was:" +  element.getValue(), element.getValue() instanceof SamplingPointType);
        sp = (SamplingPointType) element.getValue();

        //assertEquals("10972X0137-PONT", sp.getName()); //TODO name attribute is moved to namespace GML 3.2 so the java binding does not match
    }

    @Test
    @Order(order=16)
    public void testWFSTransactionInsertREST() throws Exception {
        initPool();

        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint");


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/xml/Insert-SamplingPoint-4.xml");
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        TransactionResponseType result = (TransactionResponseType) obj;

        TransactionSummaryType sum        = new TransactionSummaryType(2, 0, 0);
        List<InsertedFeatureType> insertedFeatures = new ArrayList<>();
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-013"), null));
        insertedFeatures.add(new InsertedFeatureType(new FeatureIdType("station-014"), null));
        InsertResultsType insertResult    = new InsertResultsType(insertedFeatures);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, insertResult, "1.1.0");

        assertEquals(ExpResult, result);


        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-4.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    private static final String WFS_REPLACE_FILTER =
            "filter=%3Cfes:Filter%20xmlns:fes=%22http://www.opengis.net/fes/2.0%22%20xmlns:gml=%22http://www.opengis.net/gml/3.2%22%3E"
            + "%3Cfes:PropertyIsEqualTo%3E"
            + "%3Cfes:ValueReference%3Ename%3C/fes:ValueReference%3E"
            + "%3Cfes:Literal%3E10972X0137-SOUPAS%3C/fes:Literal%3E"
            + "%3C/fes:PropertyIsEqualTo%3E"
            + "%3C/fes:Filter%3E";

    @Test
    @Order(order=17)
    public void testWFSTransactionReplaceREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint?" + WFS_REPLACE_FILTER);


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        putRequestFile(conec, "org/constellation/xml/Replace-SamplingPoint-1.xml");
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.TransactionResponseType);

        org.geotoolkit.wfs.xml.v200.TransactionResponseType result = (org.geotoolkit.wfs.xml.v200.TransactionResponseType) obj;

        org.geotoolkit.wfs.xml.v200.TransactionSummaryType sum        = new org.geotoolkit.wfs.xml.v200.TransactionSummaryType(0, 0, 0, 1);
        List<CreatedOrModifiedFeatureType> insertedFeatures = new ArrayList<>();
        insertedFeatures.add(new CreatedOrModifiedFeatureType(new ResourceIdType("station-014"), null));

         ActionResultsType act = new ActionResultsType(insertedFeatures);
        org.geotoolkit.wfs.xml.v200.TransactionResponseType ExpResult = new org.geotoolkit.wfs.xml.v200.TransactionResponseType(sum, null, null, act, "2.0.0");

        assertEquals(ExpResult, result);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint");

        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-5.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    private static final String WFS_DELETE_FILTER =
            "filter=%3Cfes:Filter%20xmlns:fes=%22http://www.opengis.net/fes/2.0%22%20xmlns:gml=%22http://www.opengis.net/gml/3.2%22%3E"
            + "%3Cfes:PropertyIsEqualTo%3E"
            + "%3Cfes:ValueReference%3Ename%3C/fes:ValueReference%3E"
            + "%3Cfes:Literal%3E10972X0137-CALOS%3C/fes:Literal%3E"
            + "%3C/fes:PropertyIsEqualTo%3E"
            + "%3C/fes:Filter%3E";


    @Test
    @Order(order=18)
    public void testWFSTransactionDeleteREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint?" + WFS_DELETE_FILTER);


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        Object obj = unmarshallResponseDelete(conec);

        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.TransactionResponseType);

        org.geotoolkit.wfs.xml.v200.TransactionResponseType result = (org.geotoolkit.wfs.xml.v200.TransactionResponseType) obj;

        org.geotoolkit.wfs.xml.v200.TransactionSummaryType sum        = new org.geotoolkit.wfs.xml.v200.TransactionSummaryType(0, 0, 1, 0);

        org.geotoolkit.wfs.xml.v200.TransactionResponseType ExpResult = new org.geotoolkit.wfs.xml.v200.TransactionResponseType(sum, null, null, null, "2.0.0");

        assertEquals(ExpResult, result);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint");

        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-6.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=19)
    public void testWFSGetFeatureByIDREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint/station-014");


        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/xml/Replace-SamplingPoint-1.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=20)
    public void testWFSTransactionReplaceByIdREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/station-014");


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        putRequestFile(conec, "org/constellation/xml/Replace-SamplingPoint-2.xml");
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.TransactionResponseType);

        org.geotoolkit.wfs.xml.v200.TransactionResponseType result = (org.geotoolkit.wfs.xml.v200.TransactionResponseType) obj;

        org.geotoolkit.wfs.xml.v200.TransactionSummaryType sum        = new org.geotoolkit.wfs.xml.v200.TransactionSummaryType(0, 0, 0, 1);
        List<CreatedOrModifiedFeatureType> insertedFeatures = new ArrayList<>();
        insertedFeatures.add(new CreatedOrModifiedFeatureType(new ResourceIdType("station-014"), null));

         ActionResultsType act = new ActionResultsType(insertedFeatures);
        org.geotoolkit.wfs.xml.v200.TransactionResponseType ExpResult = new org.geotoolkit.wfs.xml.v200.TransactionResponseType(sum, null, null, act, "2.0.0");

        assertEquals(ExpResult, result);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint");

        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-7.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=21)
    public void testWFSTransactionDeleteByIdREST() throws Exception {


        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/station-014");


        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        Object obj = unmarshallResponseDelete(conec);

        assertTrue(obj instanceof org.geotoolkit.wfs.xml.v200.TransactionResponseType);

        org.geotoolkit.wfs.xml.v200.TransactionResponseType result = (org.geotoolkit.wfs.xml.v200.TransactionResponseType) obj;

        org.geotoolkit.wfs.xml.v200.TransactionSummaryType sum        = new org.geotoolkit.wfs.xml.v200.TransactionSummaryType(0, 0, 1, 0);

        org.geotoolkit.wfs.xml.v200.TransactionResponseType ExpResult = new org.geotoolkit.wfs.xml.v200.TransactionResponseType(sum, null, null, null, "2.0.0");

        assertEquals(ExpResult, result);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/SamplingPoint");

        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/xml/samplingPointCollection-3.xml");

        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=22)
    public void testWFSGetPropertyValueREST() throws Exception {

         /**
         * Test 1 : query on typeName samplingPoint with HITS
         */
        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/property/sampledFeature?resultType=hits");


        Object result = unmarshallResponse(getCapsUrl.openConnection());

        assertTrue("unexpected type: " + result.getClass().getName() + "\n" + result, result instanceof ValueCollectionType);

        assertTrue(result instanceof ValueCollection);
        assertEquals(12, ((ValueCollection)result).getNumberReturned());

        /**
         * Test 2 : query on typeName samplingPoint with RESULTS
         */
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/property/sampledFeature");

        String sresult = getStringResponse(getCapsUrl.openConnection());

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.embedded.ValueCollectionOM1.xml"));
        domCompare(sresult, expectedResult);

        /**
         * Test 3 : query on typeName samplingPoint with RESULTS
         */
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/property/position");
        sresult = getStringResponse(getCapsUrl.openConnection());

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.embedded.ValueCollectionOM2.xml"));
        domCompare(sresult, expectedResult);

    }

    private static final String WFS_UPDATE_FILTER =
              "filter=%3Cogc:Filter%20xmlns:ogc=%22http://www.opengis.net/ogc%22%20xmlns:gml=%22http://www.opengis.net/gml%22%3E"
            + "%3Cogc:PropertyIsEqualTo%3E"
            + "%3Cogc:PropertyName%3ENAME%3C/ogc:PropertyName%3E"
            + "%3Cogc:Literal%3EAshton%3C/ogc:Literal%3E"
            + "%3C/ogc:PropertyIsEqualTo%3E"
            + "%3C/ogc:Filter%3E";


    @Test
    @Order(order=23)
    public void testWFSTransactionUpdateREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/NamedPlaces/property/the_geom?" + WFS_UPDATE_FILTER);

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        putRequestFile(conec, "org/constellation/xml/Update-NamedPlaces-2.xml");

        // Try to unmarshall something from the response returned by the server.
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        TransactionResponseType result = (TransactionResponseType) obj;

        TransactionSummaryType sum              = new TransactionSummaryType(0, 1, 0);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, null, "1.1.0");

        assertEquals(ExpResult, result);


        /**
         * We verify that the namedPlaces have been changed
         */
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/NamedPlaces");

        // Try to unmarshall something from the response returned by the server.
        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/wfs/xml/namedPlacesCollection-6.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=23)
    public void testWFSGetPropertyValueByIdREST() throws Exception {

         /**
         * Test 1 : query on typeName samplingPoint with HITS
         */
        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/station-004/sampledFeature");

        String sresult = getStringResponse(getCapsUrl.openConnection());

        String expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.embedded.ValueCollectionOM1_single.xml"));
        domCompare(sresult, expectedResult);

        /**
         * Test 3 : query on typeName samplingPoint with RESULTS
         */
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/SamplingPoint/station-004/position");
        sresult = getStringResponse(getCapsUrl.openConnection());

        expectedResult = IOUtilities.toString(IOUtilities.getResourceAsPath("org.constellation.wfs.xml.embedded.ValueCollectionOM2_single.xml"));
        domCompare(sresult, expectedResult);

    }

    @Test
    @Order(order=24)
    public void testWFSTransactionUpdateByIdREST() throws Exception {

         URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/NamedPlaces/NamedPlaces.1/the_geom");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        putRequestFile(conec, "org/constellation/xml/Update-NamedPlaces-3.xml");

        // Try to unmarshall something from the response returned by the server.
        Object obj = unmarshallResponse(conec);

        assertTrue(obj instanceof TransactionResponseType);

        TransactionResponseType result = (TransactionResponseType) obj;

        TransactionSummaryType sum              = new TransactionSummaryType(0, 1, 0);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, null, "1.1.0");

        assertEquals(ExpResult, result);


        /**
         * We verify that the namedPlaces have been changed
         */
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/NamedPlaces");

        // Try to unmarshall something from the response returned by the server.
        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/wfs/xml/namedPlacesCollection-1.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=25)
    public void testWFSTransactionUpdateNullByIdREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/NamedPlaces/NamedPlaces.1/NAME");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        // Try to unmarshall something from the response returned by the server.
        Object obj = unmarshallResponseDelete(conec);

        assertTrue(obj instanceof TransactionResponseType);

        TransactionResponseType result = (TransactionResponseType) obj;

        TransactionSummaryType sum              = new TransactionSummaryType(0, 1, 0);
        TransactionResponseType ExpResult = new TransactionResponseType(sum, null, null, "1.1.0");

        assertEquals(ExpResult, result);


        /**
         * We verify that the namedPlaces have been changed
         */
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/1.1.0/NamedPlaces");

        // Try to unmarshall something from the response returned by the server.
        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/wfs/xml/namedPlacesCollection-7.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);
    }

    @Test
    @Order(order=26)
    public void testWFSListStoredQueriesREST() throws Exception {

        // Creates a valid GetCapabilities url.
        final URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query");


        Object obj = unmarshallResponse(getCapsUrl.openConnection());

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof ListStoredQueriesResponseType);

    }

    @Test
    @Order(order=27)
    public void testWFSAdhocStoredQueriesREST() throws Exception {

        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/wfs/xml/embedded/AdhocQuery1.xml");

        Object obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof CreateStoredQueryResponseType);

        // TODO at this point i don"t know how to retrieve the identifier
        // SO i use the list operation
        ListStoredQueriesResponseType listQuery = (ListStoredQueriesResponseType) unmarshallResponse(getCapsUrl.openConnection());
        assertEquals(3, listQuery.getStoredQuery().size());

        String id = null;
        for (StoredQueryListItemType item : listQuery.getStoredQuery()) {
            if (!item.getId().equals("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureById") &&
                !item.getId().equals("urn:ogc:def:storedQuery:OGC-WFS::GetFeatureByType")) {
                id = item.getId();
            }
        }
        Assert.assertNotNull(id);

        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query/" + id);

        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/wfs/xml/embedded/singleNamedPlaces.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);

        Object response = unmarshallResponseDelete(getCapsUrl.openConnection());

        assertTrue(response instanceof DropStoredQueryResponseType);

        // verify that the query is removed
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query");

        listQuery = (ListStoredQueriesResponseType) unmarshallResponse(getCapsUrl.openConnection());
        assertEquals(2, listQuery.getStoredQuery().size());
    }

    @Test
    @Order(order=28)
    public void testWFSStoredQueriesREST() throws Exception {

        // Creates a valid GetCapabilities url.
        URL getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query/STquery1");

        // for a POST request
        URLConnection conec = getCapsUrl.openConnection();

        postRequestFile(conec, "org/constellation/wfs/xml/embedded/StoredQuery1.xml");

        Object obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof CreateStoredQueryResponseType);

        // verify that the query is added
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query");
        ListStoredQueriesResponseType listQuery = (ListStoredQueriesResponseType) unmarshallResponse(getCapsUrl.openConnection());
        assertEquals(3, listQuery.getStoredQuery().size());

        // execute it
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query/STquery1?param1=Goose%20Island");

        String xmlResult    = getStringResponse(getCapsUrl.openConnection());
        String xmlExpResult = getStringFromFile("org/constellation/wfs/xml/embedded/singleNamedPlaces.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);

        // replace
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query/STquery1");
        conec = getCapsUrl.openConnection();

        putRequestFile(conec, "org/constellation/wfs/xml/embedded/StoredQuery2.xml");

        obj = unmarshallResponse(conec);

        assertTrue("unexpected type: " + obj.getClass().getName() + "\n" + obj, obj instanceof CreateStoredQueryResponseType);

        // execute it
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query/STquery1?param1=Goose%20Island");

        xmlResult    = getStringResponse(getCapsUrl.openConnection());
        xmlExpResult = getStringFromFile("org/constellation/wfs/xml/embedded/singleNamedPlaces2.xml");
        xmlExpResult = xmlExpResult.replace("9090", getCurrentPort() + "");
        xmlResult    = xmlResult.replaceAll("timeStamp=\"[^\"]*\" ", "timeStamp=\"\" ");

        domCompare(xmlResult, xmlExpResult);

        // remove
        Object response = unmarshallResponseDelete(getCapsUrl.openConnection());

        assertTrue(response instanceof DropStoredQueryResponseType);

        // verify that the query is removed
        getCapsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default/2.0.0/query");

        listQuery = (ListStoredQueriesResponseType) unmarshallResponse(getCapsUrl.openConnection());
        assertEquals(2, listQuery.getStoredQuery().size());
    }

    @Test
    @Order(order=29)
    public void testWFSGetFeatureCITET() throws Exception {

        URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_CITE1);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        Object obj = unmarshallResponse(getfeatsUrl);

        assertTrue(obj instanceof FeatureCollectionType);

        FeatureCollectionType feat = (FeatureCollectionType) obj;
        assertEquals(1, feat.getFeatureMember().size());

        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_CITE2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        obj = unmarshallResponse(getfeatsUrl);

        assertTrue(obj instanceof FeatureCollectionType);

        feat = (FeatureCollectionType) obj;
        assertEquals(1, feat.getFeatureMember().size());
    }

    @Test
    @Order(order=30)
    public void testWFSDescribeFeatureGETCUstom() throws Exception {

        initPool();

        assumeTrue(localdb_active);

        final URL getfeatsUrl;
        try {
            getfeatsUrl = new URL("http://localhost:"+ getCurrentPort() +"/WS/wfs/default?" +WFS_DESCRIBE_FEATURE_TYPE_URL);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }

        Object obj = unmarshallResponse(getfeatsUrl);
        assertTrue(obj instanceof Schema);
        final Schema schema = (Schema) obj;
        final List elements = schema.getElements();
        assertEquals(1, elements.size());

        final DocumentComparator comparator = new DocumentComparator(WFSRequestTest.class.getResource("/expected/customsqlquery.xsd"), getfeatsUrl);
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.compare();

    }

    /**
     * Test GetFeature response in json for both versions 1.1.0 and 2.0.0
     * check if json string is valid
     */
    @Test
    @Order(order=31)
    public void testWFSGetFeatureGETJSON() throws Exception {
        initPool();
        final URL getfeatsUrl1;
        final URL getfeatsUrl2;
        try {
            getfeatsUrl1 = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_JSON);
            getfeatsUrl2 = new URL("http://localhost:"+ getCurrentPort() + "/WS/wfs/default?" + WFS_GETFEATURE_JSON2);
        } catch (MalformedURLException ex) {
            assumeNoException(ex);
            return;
        }
        //for WFS 1.1.0
        String result = getStringResponse(getfeatsUrl1.openConnection());
        result = result.replaceAll("\\s+", "");
        assertTrue(isJSONValid(result));
        assertEquals("{\"type\":\"FeatureCollection\",\"crs\":{\"type\":\"name\",\"properties\":" +
                        "{\"name\":\"urn:ogc:def:crs:EPSG:"+EPSG_VERSION+":3857\"}},\"features\":[{\"type\":\"Feature\",\"id\":\"station-001\",\"geometry\":" +
                        "{\"type\":\"Point\",\"coordinates\":[-461417.5781,5219276.6054]},\"properties\":" +
                        "{\"@id\":\"station-001\",\"description\":\"Pointd'eauBSSS\",\"name\":[\"[10972X0137-PONT]\"],\"sampledFeature\":[\"[urn:-sandre:object:bdrhf:123X]\"]}}," +
                        "{\"type\":\"Feature\",\"id\":\"station-002\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-461417.5781,5219276.6054]},\"properties\":" +
                        "{\"@id\":\"station-002\",\"description\":\"Pointd'eauBSSS\",\"name\":[\"[10972X0137-PLOUF]\"],\"sampledFeature\":[\"[urn:-sandre:object:bdrhf:123X]\"]}}]}"
                , result);

        //for WFS 2.0.0
        String result2 = getStringResponse(getfeatsUrl2.openConnection());
        result2 = result2.replaceAll("\\s+", "");
        assertTrue(isJSONValid(result2));
        assertEquals("{\"type\":\"FeatureCollection\",\"crs\":{\"type\":\"name\",\"properties\":" +
                        "{\"name\":\"urn:ogc:def:crs:EPSG:"+EPSG_VERSION+":3857\"}},\"features\":[{\"type\":\"Feature\",\"id\":\"station-001\",\"geometry\":" +
                        "{\"type\":\"Point\",\"coordinates\":[-461417.5781,5219276.6054]},\"properties\":" +
                        "{\"@id\":\"station-001\",\"description\":\"Pointd'eauBSSS\",\"name\":[\"[10972X0137-PONT]\"],\"sampledFeature\":[\"[urn:-sandre:object:bdrhf:123X]\"]}}," +
                        "{\"type\":\"Feature\",\"id\":\"station-002\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-461417.5781,5219276.6054]},\"properties\":" +
                        "{\"@id\":\"station-002\",\"description\":\"Pointd'eauBSSS\",\"name\":[\"[10972X0137-PLOUF]\"],\"sampledFeature\":[\"[urn:-sandre:object:bdrhf:123X]\"]}}]}"
                , result2);
    }

    private boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            // e.g. in case JSONArray is valid as well...
            LOGGER.log(Level.WARNING,ex.getLocalizedMessage(),ex);
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                LOGGER.log(Level.WARNING,ex1.getLocalizedMessage(),ex1);
                return false;
            }
        }
        return true;
    }

    protected static void domCompare(final Object actual, String expected) throws Exception {
        expected = expected.replace("EPSG_VERSION", EPSG_VERSION);
        domCompare(actual, expected, new ArrayList<>());
    }
}
