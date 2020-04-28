package org.constellation.test.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.referencing.CRS;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ProviderType;
import org.constellation.business.IProviderBusiness;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ProviderParameters;
import static org.constellation.provider.ProviderParameters.getOrCreate;
import org.constellation.util.Util;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;

/**
 * Utility class used for setup test data environment of test classes.
 *
 * @author Quentin Boileau (Geomatys)
 * @author Guilhem Legal (Geomatys)
 */
public final class TestEnvironment {

    /**
     * Current version of EPSG databse.
     * Used to replace "EPSG_VERSION" in some test resource file.
     * Must be updated if current EPSG database version change.
     */
    public static final String EPSG_VERSION = getEPSGVersion();

    /*
        List of resources available
     */
    public enum TestResource {

        //image
        DATA("org/constellation/data/image", null),

        PNG("org/constellation/data/image/SSTMDE200305.png", TestEnvironment::createCoverageFileProvider),
        TIF("org/constellation/data/image/martinique.tif", TestEnvironment::createTifProvider),

        NETCDF("org/constellation/netcdf", null),

        SQL_SCRIPTS("org/constellation/sql", null),

        WFS110_AGGREGATE("org/constellation/ws/embedded/wfs110/aggregate", TestEnvironment::createAggregateProvider),
        WFS110_ENTITY("org/constellation/ws/embedded/wfs110/entity", TestEnvironment::createEntityGenericGMLProvider),
        WFS110_PRIMITIVE("org/constellation/ws/embedded/wfs110/primitive", TestEnvironment::createPrimitiveGMLProvider),
        WFS110_CITE_GMLSF0("org/constellation/ws/embedded/wfs110/cite-gmlsf0.xsd", null),

        WMS111_SHAPEFILES("org/constellation/ws/embedded/wms111/shapefiles", TestEnvironment::createShapefileProvider),
        WMS111_STYLES("org/constellation/ws/embedded/wms111/styles", null),

        SHAPEFILES("org/constellation/data/shapefiles", TestEnvironment::createShapefileProvider),

        // Observation and mesurement providers
        OM2_FEATURE_DB(null, TestEnvironment::createOM2FeatureProvider),
        OM2_DB(null, TestEnvironment::createOM2DatabaseProvider),
        OM_XML("org/constellation/xml/sos/single-observations.xml", TestEnvironment::createOMFileProvider),
        OM_GENERIC_DB("org/constellation/xml/sos/generic-config.xml", TestEnvironment::createOMGenericDBProvider),
        OM_LUCENE(null,  TestEnvironment::createOMLuceneProvider),

        // Sensor Providers
        SENSOR_FILE(null, TestEnvironment::createSensorFileProvider),
        SENSOR_INTERNAL(null, TestEnvironment::createSensorInternalProvider),

        // metadata providers
        METADATA_FILE(null, TestEnvironment::createMetadataFileProvider),
        METADATA_NTCDF(null, TestEnvironment::createMetadataNetCDFProvider),
        METADATA_INTERNAL(null, TestEnvironment::createMetadataInternalProvider),

        // feature database
        FEATURE_DATABASE(null, TestEnvironment::createFeatDBProvider),

        //xml files
        XML("org/constellation/xml", null),
        XML_METADATA("org/constellation/xml/metadata", null),
        XML_SML("org/constellation/xml/sml", null),
        XML_SOS("org/constellation/xml/sos", null),

        //json files
        JSON_FEATURE("org/constellation/ws/embedded/json/feature.json", TestEnvironment::createGeoJsonProvider),
        JSON_FEATURE_COLLECTION("org/constellation/ws/embedded/json/featureCollection.json", TestEnvironment::createGeoJsonProvider);

        private final String path;

        private final BiFunction<IProviderBusiness, Path, Integer> createProvider;

        TestResource(String path, BiFunction<IProviderBusiness, Path, Integer> createProvider) {
            this.path = path;
            this.createProvider = createProvider;
        }
    }

    private TestEnvironment() {
    }

    /**
     * Copy all test resources into target directory
     * @param workspace
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private static Map<TestResource, DeployedTestResource> initWorkspaceData(Path workspace) throws URISyntaxException, IOException {
        final Map<TestResource, DeployedTestResource> results = new HashMap<>();
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        TestResource[] resources = TestResource.values();
        for (TestResource resource : resources) {
            Path deployedPath = null;
            if (resource.path != null) {
                deployedPath = IOUtilities.copyResource(resource.path, classloader, workspace, true);
            }
            DeployedTestResource dtr = new DeployedTestResource(resource, deployedPath);
            results.put(resource, dtr);
        }
        return results;
    }

    public static TestResources initDataDirectory() throws IOException {
        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path outputDir = tmpDir.resolve("Constellation");
        Files.createDirectories(outputDir);
        TestResources result = new TestResources();
        result.outputDir = outputDir;
        try {
            result.resources = initWorkspaceData(outputDir);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
        return result;
    }

    private static String getEPSGVersion() {
        final CRSAuthorityFactory epsg;
        try {
            epsg = CRS.getAuthorityFactory("EPSG");
        } catch (FactoryException e) {
            throw new IllegalStateException("No EPSG factory defined", e);
        }
        final InternationalString edition = epsg.getAuthority().getEdition();
        if (edition == null) throw new IllegalStateException("No EPSG version defined !");
        return edition.toString(Locale.ROOT);
    }

    public static class TestResources {
        public Path outputDir;
        public Map<TestResource, DeployedTestResource> resources;

        public Integer createProvider(TestResource tr, IProviderBusiness providerBusiness) {
           DeployedTestResource dpr = resources.get(tr);
           if (dpr != null) {
               return dpr.createProvider(providerBusiness);
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.name());
        }

        public Integer createProviderWithPath(TestResource tr, Path p, IProviderBusiness providerBusiness) {
           DeployedTestResource dpr = resources.get(tr);
           if (dpr != null) {
               dpr.dataDir = p;
               return dpr.createProvider(providerBusiness);
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.name());
        }
    }

    public static class DeployedTestResource {
        public Path dataDir;
        public final TestResource tr;

        public DeployedTestResource(TestResource tr, Path dataDir) {
            this.dataDir = dataDir;
            this.tr = tr;
        }

        public Integer createProvider(IProviderBusiness providerBusiness) {
            return tr.createProvider.apply(providerBusiness, dataDir);
        }
    }

    private static Integer createShapefileProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            String providerIdentifier = "shapeSrc" + UUID.randomUUID().toString();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("ShapefileParametersFolder");
            config.parameter("path").setValue(p.toUri());

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createGeoJsonProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            String providerIdentifier = "geojsonSrc" + UUID.randomUUID().toString();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("geojson");
            config.parameter("path").setValue(p.toUri());

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createOM2FeatureProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "omSrc-" + UUID.randomUUID().toString();
            final String url = buildDerbyOM2Database(providerIdentifier);

            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("SOSDBParameters");
            config.parameter("sgbdtype").setValue("derby");
            config.parameter("derbyurl").setValue(url);

            return  providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createGMLProvider(IProviderBusiness providerBusiness, Path p, String providerIdentifier, String typeName) {
        try {

            Path citeGmlsf0 = p.getParent().resolve("cite-gmlsf0.xsd");
            final DataProviderFactory factory = DataProviders.getFactory("data-store");

            // Defines a GML data provider
            ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);

            ParameterValueGroup choice = getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(),source);
            ParameterValueGroup config = choice.addGroup("gml");
            config.parameter("location").setValue(p.toUri());
            config.parameter("sparse").setValue(Boolean.TRUE);
            config.parameter("xsd").setValue(citeGmlsf0.toUri().toURL());
            config.parameter("xsdtypename").setValue(typeName);
            config.parameter("longitudeFirst").setValue(Boolean.TRUE);

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createPrimitiveGMLProvider(IProviderBusiness providerBusiness, Path p) {
        final String providerIdentifier = "primGMLSrc-" + UUID.randomUUID().toString();
        return createGMLProvider(providerBusiness, p, providerIdentifier, "PrimitiveGeoFeature");
    }

    private static Integer createEntityGenericGMLProvider(IProviderBusiness providerBusiness, Path p) {
        final String providerIdentifier = "entGMLSrc-" + UUID.randomUUID().toString();
        return createGMLProvider(providerBusiness, p, providerIdentifier, "EntitéGénérique");
    }

    private static Integer createAggregateProvider(IProviderBusiness providerBusiness, Path p) {
        final String providerIdentifier = "aggGMLSrc-" + UUID.randomUUID().toString();
        return createGMLProvider(providerBusiness, p, providerIdentifier, "AggregateGeoFeature");
    }

    private static Integer createFeatDBProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
             source.parameter("id").setValue("postgisSrc");

            final ParameterValueGroup choice = getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(),source);
            final ParameterValueGroup config = choice.addGroup("PostgresParameters");
            config.parameter("database").setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_name"));
            config.parameter("host").setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_host"));
            config.parameter("schema").setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_schema"));
            config.parameter("user").setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_user"));
            config.parameter("password").setValue(TestDatabaseHandler.testProperties.getProperty("feature_db_pass"));

            //add a custom sql query layer
            final ParameterValueGroup layer = source.addGroup("Layer");
            layer.parameter("name").setValue("CustomSQLQuery");
            layer.parameter("language").setValue("CUSTOM-SQL");
            layer.parameter("statement").setValue("SELECT name as nom, \"pointProperty\" as geom FROM \"PrimitiveGeoFeature\" ");

            return providerBusiness.storeProvider("postgisSrc", null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createCoverageFileProvider(IProviderBusiness providerBusiness, Path pngFile) {
        final String providerIdentifier = "coverageTestSrc-" + UUID.randomUUID().toString();
        try {
            final DataProviderFactory dataStorefactory = DataProviders.getFactory("data-store");

            final ParameterValueGroup source = dataStorefactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dataStorefactory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("FileCoverageStoreParameters");
            config.parameter("path").setValue(pngFile.toUri().toURL());
            config.parameter("type").setValue("AUTO");

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createTifProvider(IProviderBusiness providerBusiness, Path tifFile) {
        final String providerIdentifier = "coverageTiffSrc-" + UUID.randomUUID().toString();
        try {
            final DataProviderFactory dsFactory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = dsFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dsFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("coverage-file");
            config.parameter("location").setValue(tifFile.toUri().toURL());
            config.parameter("type").setValue("AUTO");

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static String buildDerbyOM2Database(String providerIdentifier) throws Exception {
        final String url = "jdbc:derby:memory:" + providerIdentifier;
        final DefaultDataSource ds = new DefaultDataSource(url + ";create=true");
        try (final Connection con = ds.getConnection()) {
            final DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
            String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations.sql"));
            sql = sql.replace("$SCHEMA", "");
            sr.run(sql);
            sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2.sql"));
        }
        ds.shutdown();
        return url;
    }

    private static Integer createOMFileProvider(IProviderBusiness providerBusiness, Path xmlFile) {
        final String providerIdentifier = "omTestSrc" + UUID.randomUUID().toString();
        try {
            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("observationXmlFile");
            config.parameter("path").setValue(xmlFile.toUri().toURL());

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createOM2DatabaseProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "omSrc-" + UUID.randomUUID().toString();
            final String url = buildDerbyOM2Database(providerIdentifier);

            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("observationSOSDatabase");
            config.parameter("sgbdtype").setValue("derby");
            config.parameter("derbyurl").setValue(url);
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");

            return  providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createOMGenericDBProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "omGenericDBSrc-" + UUID.randomUUID().toString();

            final String url = buildDerbyOM2Database(providerIdentifier);

            MarshallerPool pool = GenericDatabaseMarshallerPool.getInstance();
            Unmarshaller unmarshaller = pool.acquireUnmarshaller();
            Automatic OMConfiguration = (Automatic) unmarshaller.unmarshal(p.toFile());
            OMConfiguration.getBdd().setConnectURL(url);
            pool.recycle(unmarshaller);

            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("observationSOSGeneric");
            config.parameter("Configuration").setValue(OMConfiguration);
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createOMLuceneProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "omLucneSrc-" + UUID.randomUUID().toString();

            Path configDir     = p;
            Path SOSDirectory  = configDir.resolve("SOS");
            Path instDirectory = SOSDirectory.resolve("default");

            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("observationSOSLucene");
            config.parameter("data-directory").setValue(instDirectory);
            config.parameter("config-directory").setValue(configDir);
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createSensorFileProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "sensorFileSrc-" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("sensor-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("filesensor");
            config.parameter("data_directory").setValue(p.toUri());

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.SENSOR, "sensor-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createMetadataFileProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "metadataFileSrc-" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("metadata-store");
            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
            sourcef.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
            final ParameterValueGroup config = choice.addGroup("FilesystemMetadata");
            config.parameter("folder").setValue(p.toUri());
            config.parameter("store-id").setValue(providerIdentifier);

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "metadata-store", sourcef);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createMetadataNetCDFProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "NCmetadataSrc-" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("metadata-store");
            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
            sourcef.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
            final ParameterValueGroup config = choice.addGroup("NetCDFMetadata");
            config.parameter("folder").setValue(p.toUri());

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "metadata-store", sourcef);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createSensorInternalProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "sensorInternalSrc-" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("sensor-store");
            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
            sourcef.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
            final ParameterValueGroup config = choice.addGroup("cstlsensor");

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.SENSOR, "sensor-store", sourcef);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createMetadataInternalProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "metadataInternalSrc-" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("metadata-store");
            final ParameterValueGroup sourcef = factory.getProviderDescriptor().createValue();
            sourcef.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), sourcef);
            final ParameterValueGroup config = choice.addGroup("InternalCstlmetadata");

            return providerBusiness.storeProvider(providerIdentifier, null, ProviderType.LAYER, "metadata-store", sourcef);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
}
