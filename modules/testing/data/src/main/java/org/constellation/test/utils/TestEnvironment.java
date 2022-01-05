package org.constellation.test.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.Unmarshaller;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ProviderType;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.exception.ConfigurationException;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.ProviderParameters;
import static org.constellation.provider.ProviderParameters.getOrCreate;
import static org.constellation.test.utils.TestResourceUtils.unmarshallSensorResource;
import static org.constellation.test.utils.TestResourceUtils.writeDataFileEPSG;
import org.constellation.util.Util;
import org.geotoolkit.coverage.worldfile.FileCoverageProvider;
import org.geotoolkit.data.shapefile.ShapefileFolderProvider;
import org.geotoolkit.internal.sql.DefaultDataSource;
import org.geotoolkit.internal.sql.DerbySqlScriptRunner;
import org.geotoolkit.nio.IOUtilities;
import org.geotoolkit.nio.ZipUtilities;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValue;
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
public class TestEnvironment {

    /**
     * Current version of EPSG databse.
     * Used to replace "EPSG_VERSION" in some test resource file.
     * Must be updated if current EPSG database version change.
     */
    public static final String EPSG_VERSION = getEPSGVersion();

    public static class DataImport {
        public final int id;
        public final String namespace;
        public final String name;
        public final int pid;

        DataImport(int id, String namespace, String name, int pid) {
            this.id = id;
            this.namespace = namespace;
            this.name = name;
            this.pid = pid;
        }
    }
    
    public static class ProviderImport {
        public final int id;
        public final int datasetId;
        public final List<DataImport> datas;

        ProviderImport(int id, int datasetId, List<DataImport> datas) {
            this.id = id;
            this.datas = Collections.unmodifiableList(datas);
            this.datasetId = datasetId;
        }
    }

     public static class ProvidersImport {

        public final List<ProviderImport> providers;

        ProvidersImport(List<ProviderImport> providers) {
            this.providers = Collections.unmodifiableList(providers);
        }

        public List<DataImport> datas() {
            return providers.stream().flatMap(pi -> pi.datas.stream()).collect(Collectors.toList());
        }

        public List<Integer> pids() {
            return providers.stream().map(pi -> pi.id).collect(Collectors.toList());
        }

        public DataImport findDataByName(String name) {
            for (ProviderImport pi : providers) {
                for (DataImport di : pi.datas) {
                    if (di.name.equals(name)) {
                        return di;
                    }
                }
            }
            return null;
        }
    }

    /*
        List of resources available
     */
    public static class TestResource {

        static List<TestResource> values = new ArrayList<>();

        /**
         * Full directory of images (needed for proper deployement of all the associated image files)
         */
        public static final TestResource IMAGES = new TestResource("org/constellation/data/image");
        
        /**
         * Coverage file datastore with PNG file.
         * 
         *  data :
         *  - SSTMDE200305
         */
        public static final TestResource PNG = new TestResource("org/constellation/data/image/png/SSTMDE200305.png", TestEnvironment::createCoverageFileProvider, TestEnvironment::createFileCoverageStore);

        /**
         * Coverage file datastore with TIF file.
         *
         *  data :
         *  - martinique
         */
        public static final TestResource TIF = new TestResource("org/constellation/data/image/tif/martinique.tif", TestEnvironment::createTifProvider, TestEnvironment::createFileCoverageStore);

        /**
         * Netcdf provider.
         *
         * data :
         * - Nav
         * - Nmodels
         * - Record
         * - sea_water_temperature
         */
        public static final TestResource NETCDF = new TestResource("org/constellation/netcdf/2005092200_sst_21-24.en.nc", TestEnvironment::createNCProvider);

        /**
         * Coverage xml pyramid datastore.
         *
         * data : - haiti_01_pyramid
         */
        public static TestResource XML_PYRAMID = new TestResource("org/constellation/data/tiles/xml-pyramid/haiti_01_pyramid.zip", TestEnvironment::createXMLPyramidProvider, null);

        public static final TestResource SQL_SCRIPTS = new TestResource("org/constellation/sql", null, null);

        /**
         * GML datastore.
         *
         * data :
         *  - http://cite.opengeospatial.org/gmlsf : AggregateGeoFeature
         */
        public static TestResource WFS110_AGGREGATE = new TestResource("org/constellation/ws/embedded/wfs110/aggregate", TestEnvironment::createAggregateProvider);

        /**
         * GML datastore.
         *
         * data :
         *  - http://cite.opengeospatial.org/gmlsf : EntitéGénérique
         */
        public static final TestResource WFS110_ENTITY = new TestResource("org/constellation/ws/embedded/wfs110/entity", TestEnvironment::createEntityGenericGMLProvider);

        /**
         * GML datastore.
         *
         * data :
         *  - http://cite.opengeospatial.org/gmlsf : PrimitiveGeoFeature
         */
        public static final TestResource WFS110_PRIMITIVE = new TestResource("org/constellation/ws/embedded/wfs110/primitive", TestEnvironment::createPrimitiveGMLProvider);
        public static final TestResource WFS110_CITE_GMLSF0 = new TestResource("org/constellation/ws/embedded/wfs110/cite-gmlsf0.xsd");

        /**
         * Multiple shapefiles.
         * 
         * data :
         * - BuildingCenters
         * - BasicPolygons
         * - Bridges
         * - Streams
         * - Lakes
         * - NamedPlaces
         * - Buildings
         * - RoadSegments
         * - DividedRoutes
         * - Forests
         * - MapNeatline
         * - Ponds
         */
        public static final TestResource WMS111_SHAPEFILES = new TestResource("org/constellation/ws/embedded/wms111/shapefiles", TestEnvironment::createShapefileProvider, TestEnvironment::createShapefileStore, TestEnvironment::createShapefileProviders);
        public static final TestResource WMS111_STYLES = new TestResource("org/constellation/ws/embedded/wms111/styles");

        public static final TestResource SHAPEFILES = new TestResource("org/constellation/data/shapefiles", TestEnvironment::createShapefileProvider, TestEnvironment::createShapefileStore, TestEnvironment::createShapefileProviders);

        /**
         * Observation and mesurement provider.
         * 
         * data :
         * - http://www.opengis.net/sampling/1.0 : SamplingPoint
         */
        public static final TestResource OM2_FEATURE_DB = new TestResource(null, TestEnvironment::createOM2FeatureProvider);
        public static final TestResource OM2_DB = new TestResource(null, TestEnvironment::createOM2DatabaseProvider);
        public static final TestResource OM_XML = new TestResource("org/constellation/xml/sos/single-observations.xml", TestEnvironment::createOMFileProvider);
        public static final TestResource OM_GENERIC_DB = new TestResource("org/constellation/xml/sos/generic-config.xml", TestEnvironment::createOMGenericDBProvider);
        public static final TestResource OM_LUCENE = new TestResource(null,  TestEnvironment::createOMLuceneProvider, null);

        // Sensor Providers
        public static final TestResource SENSOR_FILE = new TestResource("org/constellation/xml/sml", TestEnvironment::createSensorFileProvider);
        public static final TestResource SENSOR_INTERNAL = new TestResource(null, TestEnvironment::createSensorInternalProvider);

        // metadata providers
        public static final TestResource METADATA_FILE = new TestResource(null, TestEnvironment::createMetadataFileProvider);
        public static final TestResource METADATA_NETCDF = new TestResource(null, TestEnvironment::createMetadataNetCDFProvider);
        public static final TestResource METADATA_INTERNAL = new TestResource(null, TestEnvironment::createMetadataInternalProvider);

        /**
         * External Postgis feature database.
         *
         * data :
         * - http://cite.opengeospatial.org/gmlsf2 : AggregateGeoFeature
         * - http://cite.opengeospatial.org/gmlsf2 : PrimitiveGeoFeature
         * - http://cite.opengeospatial.org/gmlsf2 : EntitéGénérique
         * - http://cite.opengeospatial.org/gmlsf2 : CustomSQLQuery
         */
        public static final TestResource FEATURE_DATABASE = new TestResource(null, TestEnvironment::createFeatDBProvider);

        //xml files
        public static final TestResource XML = new TestResource("org/constellation/xml");
        public static final TestResource XML_METADATA = new TestResource("org/constellation/xml/metadata");
        public static final TestResource XML_SML = new TestResource("org/constellation/xml/sml");
        public static final TestResource XML_SOS = new TestResource("org/constellation/xml/sos");

        /**
         * GEOJSON file provider.
         * 
         * data :
         * - feature
         */
        public static final TestResource JSON_FEATURE = new TestResource("org/constellation/ws/embedded/json/feature.json", TestEnvironment::createGeoJsonProvider);

         /**
         * GEOJSON file provider.
         *
         * data :
         * - featureCollection
         */
        public static final TestResource JSON_FEATURE_COLLECTION = new TestResource("org/constellation/ws/embedded/json/featureCollection.json", TestEnvironment::createGeoJsonProvider);

        protected final String path;

        private final BiFunction<IProviderBusiness, Path, Integer> createProvider;
        private final BiFunction<IProviderBusiness, Path, List<Integer>> createProviders;
        private final Function<Path, DataStore> createStore;

        public TestResource(String path) {
            this(path, null, null, null);
        }

        public TestResource(String path, BiFunction<IProviderBusiness, Path, Integer> createProvider) {
            this(path, createProvider, null, null);
        }

        public TestResource(String path, BiFunction<IProviderBusiness, Path, Integer> createProvider, Function<Path, DataStore> createStore) {
            this(path, createProvider, createStore, null);
        }

        public TestResource(String path, BiFunction<IProviderBusiness, Path, Integer> createProvider, Function<Path, DataStore> createStore, BiFunction<IProviderBusiness, Path, List<Integer>> createProviders) {
            this.path = path;
            this.createProvider = createProvider;
            this.createStore= createStore;
            this.createProviders = createProviders;
            values.add(this);
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
        List<TestResource> resources = TestResource.values;
        for (TestResource resource : resources) {
            Path deployedPath = null;
            if (resource.path != null) {
                deployedPath = IOUtilities.copyResource(resource.path, classloader, workspace, true);
                if (IOUtilities.extension(deployedPath).equals("zip")) {
                    Path target = deployedPath.resolveSibling(IOUtilities.filenameWithoutExtension(deployedPath));
                    ZipUtilities.unzipNIO(deployedPath, target, false);
                    IOUtilities.deleteSilently(deployedPath);
                    deployedPath = target;
                }
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

        /**
         * Create The provider for the specified test resource, and generate the associated data.
         * If a dataset id is specified, the data will be associated with it.
         * return a report of the created entity.
         *
         * @param tr A test resource.
         * @param providerBusiness Spring bean.
         * @param datasetId Dataset id, ca be {@code null}.
         *
         * @return A report of the created entity containing the provider id, and a view of the created datas.
         * @throws org.constellation.exception.ConstellationException
         */
        public ProviderImport createProvider(TestResource tr, IProviderBusiness providerBusiness, Integer datasetId) throws ConstellationException {
           DeployedTestResource dpr = resources.get(tr);
           if (dpr != null) {
               int pid = dpr.createProvider(providerBusiness, datasetId);
               int dsId = providerBusiness.createOrUpdateData(pid, datasetId, true, false, null);
               List<DataImport> datas = providerBusiness.getDataBriefsFromProviderId(pid, null, true, false, false, false)
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid))
                       .collect(Collectors.toList());
               return new ProviderImport(pid, dsId, datas);
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.path);
        }

        public ProvidersImport createProviders(TestResource tr, IProviderBusiness providerBusiness, Integer datasetId) throws ConstellationException {
            DeployedTestResource dpr = resources.get(tr);
            if (dpr != null) {
                List<ProviderImport> results = new ArrayList<>();
                List<Integer> pids = dpr.createProviders(providerBusiness, datasetId);
                for (Integer pid : pids) {
                    int dsId = providerBusiness.createOrUpdateData(pid, datasetId, true, false, null);
                    List<DataImport> datas = providerBusiness.getDataBriefsFromProviderId(pid, null, true, false, false, false)
                            .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid))
                            .collect(Collectors.toList());
                    results.add(new ProviderImport(pid, dsId, datas));
                }
                return new ProvidersImport(results);
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.path);
        }

        public void generateSensors(ISensorBusiness sensorBusiness, int omProviderId, int smlProviderId) throws ConstellationException {
           ObservationProvider omProv = (ObservationProvider) DataProviders.getProvider(omProviderId);
           Collection<String> procs   = omProv.getProcedureNames(null, new HashMap<>());

           // default sensor initialisation from the sensor present in the OM provider
            for (String proc : procs) {
                sensorBusiness.create(proc, proc, null, "system", null, null, null, Long.MIN_VALUE, smlProviderId);
            }

            // complete some sensor with sml
            createOrUpdateSensor("org/constellation/xml/sml/urnµogcµobjectµsensorµGEOMµ1.xml", "urn:ogc:object:sensor:GEOM:1", "GEOM 1", "system", "timeseries", smlProviderId, sensorBusiness);
            createOrUpdateSensor("org/constellation/xml/sml/urnµogcµobjectµsensorµGEOMµ2.xml", "urn:ogc:object:sensor:GEOM:2", "GEOM 2", "component", "profile", smlProviderId, sensorBusiness);
            createOrUpdateSensor("org/constellation/xml/sml/urnµogcµobjectµsensorµGEOMµtest-1.xml", "urn:ogc:object:sensor:GEOM:test-1", "test 1", "system", "timeseries", smlProviderId, sensorBusiness);
            createOrUpdateSensor("org/constellation/xml/sml/urnµogcµobjectµsensorµGEOMµ8.xml", "urn:ogc:object:sensor:GEOM:8", "GEOM 8", "system", "timeseries", smlProviderId, sensorBusiness);
        }

        private void createOrUpdateSensor(String fileName, String sensorId, String name, String smlType, String omType, int smlProviderId, ISensorBusiness sensorBusiness) throws ConstellationException {
            Object sml = unmarshallSensorResource(fileName, sensorBusiness);
            Sensor s = sensorBusiness.getSensor(sensorId);
            if (s != null) {
                s.setName(name);
                s.setDescription(name);
                s.setType(smlType);
                s.setOmType(omType);
                sensorBusiness.update(s);
                sensorBusiness.updateSensorMetadata(sensorId, sml);
            } else {
                sensorBusiness.create(sensorId, sensorId, null, "system", null, null, null, Long.MIN_VALUE, smlProviderId);
            }
        }

        /**
         * Create The provider for the specified test resource using the specified files location, and generate the associated data.
         * If a dataset id is specified, the data will be associated with it.
         * return a report of the created entity.
         *
         * @param tr A test resource.
         * @param location The location of the file to use in the datasore
         * @param providerBusiness Spring bean.
         * @param datasetId Dataset id, ca be {@code null}.
         *
         * @return A report of the created entity containing the provider id, and a view of the created datas.
         * @throws org.constellation.exception.ConstellationException
         */
        public ProviderImport createProviderWithPath(TestResource tr, Path location, IProviderBusiness providerBusiness, Integer datasetId) throws ConstellationException {
           DeployedTestResource dpr = resources.get(tr);
           if (dpr != null) {
               dpr.dataDir = location;
               int pid = dpr.createProvider(providerBusiness, datasetId);
               int dsId = providerBusiness.createOrUpdateData(pid, datasetId, true, false, null);
               List<DataImport> datas = providerBusiness.getDataBriefsFromProviderId(pid, null, true, false, false, false)
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid))
                       .collect(Collectors.toList());
               return new ProviderImport(pid, dsId, datas);
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.path);
        }

        public DataStore createStore(TestResource tr) {
           DeployedTestResource dpr = resources.get(tr);
           if (dpr != null) {
               return dpr.createStore();
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.path);
        }
    }

    public static class DeployedTestResource {
        public Path dataDir;
        public final TestResource tr;

        public DeployedTestResource(TestResource tr, Path dataDir) {
            this.dataDir = dataDir;
            this.tr = tr;
        }

        public Integer createProvider(IProviderBusiness providerBusiness, Integer datasetId) {
            return tr.createProvider.apply(providerBusiness, dataDir);
        }

        public List<Integer> createProviders(IProviderBusiness providerBusiness, Integer datasetId) {
            return tr.createProviders.apply(providerBusiness, dataDir);
        }

        public DataStore createStore() {
            return tr.createStore.apply(dataDir);
        }
    }

    public static ProviderImport createAggregateProvider(IProviderBusiness providerBusiness, String dataName, List<Integer> dataIds, Integer datasetId) throws ConstellationException {
        try {
            String providerIdentifier = "aggSrc" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
            final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("AggregatedCoverageProvider");

            final GeneralParameterDescriptor dataIdsDesc = config.getDescriptor().descriptor("data_ids");
            for (Integer dataId : dataIds) {
                ParameterValue p = (ParameterValue) dataIdsDesc.createValue();
                p.setValue(dataId);
                config.values().add(p);
            }
            config.parameter("DataName").setValue(dataName);
            config.parameter("ResultCRS").setValue("EPSG:4326");
            config.parameter("mode").setValue("ORDER");

            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "computed-resource", source);
            int dsId = providerBusiness.createOrUpdateData(pid, datasetId, true, false, null);
            List<DataImport> datas = providerBusiness.getDataBriefsFromProviderId(pid, null, true, false, false, false)
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid))
                       .collect(Collectors.toList());
            return new ProviderImport(pid, dsId, datas);
        } catch (Exception ex) {
            throw new ConstellationException(ex);
        }
    }

    private static List<Integer> createShapefileProviders(IProviderBusiness providerBusiness, Path p) {
        try {
            if (Files.isDirectory(p)) {
                try (Stream<Path> stream = Files.list(p)) {
                    return stream
                      .filter(file -> !Files.isDirectory(file))
                      .filter(file -> IOUtilities.extension(file).equalsIgnoreCase("shp"))
                      .map(file -> createShapefileProvider(providerBusiness, file))
                      .collect(Collectors.toList());
                }
            } else {
                return Arrays.asList(createShapefileProvider(providerBusiness, p));
            }

        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createShapefileProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final DataProviderFactory factory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            String providerIdentifier = "shapeSrc" + UUID.randomUUID().toString();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("shapefile");
            config.parameter("path").setValue(p.toUri());

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static DataStore createShapefileStore(Path p) {
        try {
            ShapefileFolderProvider provider = new ShapefileFolderProvider();
            ParameterValueGroup params = provider.getOpenParameters().createValue();
            params.parameter("path").setValue(p);
            return provider.open(params);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
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

            return  providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
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

            return providerBusiness.storeProvider("postgisSrc", ProviderType.LAYER, "data-store", source);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static DataStore createFileCoverageStore(Path p) {
        try {
            FileCoverageProvider provider = new FileCoverageProvider();
            ParameterValueGroup params = provider.getOpenParameters().createValue();
            params.parameter("path").setValue(p);
            return provider.open(params);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createXMLPyramidProvider(IProviderBusiness providerBusiness, Path pyramidDir) {
        final String providerIdentifier = "xmlPyramidSrc-" + UUID.randomUUID().toString();
        try {
            final DataProviderFactory dsFactory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = dsFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dsFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("coverage-xml-pyramid");
            config.parameter("path").setValue(pyramidDir.toUri().toURL());
            config.parameter("cacheTileState").setValue(true);

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    private static Integer createNCProvider(IProviderBusiness providerBusiness, Path ncFile) {
        final String providerIdentifier = "netcdfSrc-" + UUID.randomUUID().toString();
        try {
            final DataProviderFactory dsFactory = DataProviders.getFactory("data-store");
            final ParameterValueGroup source = dsFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dsFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("NetCDF");
            config.parameter("location").setValue(ncFile.toUri().toURL());

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
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

            return  providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
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
            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
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

            createOMLuceneDataFile(instDirectory);

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
            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createSensorFileProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "sensorFileSrc-" + UUID.randomUUID().toString();

            Path tmpDir = Files.createTempDirectory(providerIdentifier);
            IOUtilities.copy(p, tmpDir);
            final DataProviderFactory factory = DataProviders.getFactory("sensor-store");
            final ParameterValueGroup source = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);

            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("filesensor");
            config.parameter("data_directory").setValue(tmpDir.toUri());

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.SENSOR, "sensor-store", source);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "metadata-store", sourcef);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "metadata-store", sourcef);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.SENSOR, "sensor-store", sourcef);
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

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "metadata-store", sourcef);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Path createOMLuceneDataFile(Path instDirectory) throws IOException {

        //we write the data files
        Path offeringDirectory = instDirectory.resolve("offerings");
        Files.createDirectories(offeringDirectory);

        Path offeringV100Directory = offeringDirectory.resolve("1.0.0");
        Files.createDirectories(offeringV100Directory);
        //writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-1.xml", "offering-allSensor.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-1.xml", "offering-1.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-2.xml", "offering-2.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-3.xml", "offering-3.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-4.xml", "offering-4.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-5.xml", "offering-5.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-6.xml", "offering-6.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-7.xml", "offering-7.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-8.xml", "offering-8.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-9.xml", "offering-9.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-10.xml", "offering-10.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-11.xml", "offering-11.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-12.xml", "offering-12.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-13.xml", "offering-13.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV100Directory, "org/constellation/sos/v100/offering-14.xml", "offering-14.xml", EPSG_VERSION);

        Path offeringV200Directory = offeringDirectory.resolve("2.0.0");
        Files.createDirectories(offeringV200Directory);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-1.xml", "offering-1.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-2.xml", "offering-2.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-3.xml", "offering-3.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-4.xml", "offering-4.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-5.xml", "offering-5.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-6.xml", "offering-6.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-7.xml", "offering-7.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-8.xml", "offering-8.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-9.xml", "offering-9.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-10.xml", "offering-10.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-11.xml", "offering-11.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-12.xml", "offering-12.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-13.xml", "offering-13.xml", EPSG_VERSION);
        writeDataFileEPSG(offeringV200Directory, "org/constellation/sos/v200/offering-14.xml", "offering-14.xml", EPSG_VERSION);

        Path phenomenonDirectory = instDirectory.resolve("phenomenons");
        Files.createDirectories(phenomenonDirectory);
        writeDataFileEPSG(phenomenonDirectory, "org/constellation/sos/phenomenon-depth.xml", "depth.xml", EPSG_VERSION);
        writeDataFileEPSG(phenomenonDirectory, "org/constellation/sos/phenomenon-temp.xml",  "temperature.xml", EPSG_VERSION);
        writeDataFileEPSG(phenomenonDirectory, "org/constellation/sos/phenomenon-sal.xml",  "salinity.xml", EPSG_VERSION);
        writeDataFileEPSG(phenomenonDirectory, "org/constellation/sos/phenomenon-depth-temp.xml",  "aggregatePhenomenon.xml", EPSG_VERSION);
        writeDataFileEPSG(phenomenonDirectory, "org/constellation/sos/phenomenon-depth-temp-sal.xml",  "aggregatePhenomenon-2.xml", EPSG_VERSION);

        Path featureDirectory = instDirectory.resolve("features");
        Files.createDirectories(featureDirectory);
        Path featureV200Directory = featureDirectory.resolve("2.0.0");
        Files.createDirectories(featureV200Directory);
        writeDataFileEPSG(featureV200Directory, "org/constellation/sos/v200/feature1.xml", "station-001.xml", EPSG_VERSION);
        writeDataFileEPSG(featureV200Directory, "org/constellation/sos/v200/feature2.xml", "station-002.xml", EPSG_VERSION);
        writeDataFileEPSG(featureV200Directory, "org/constellation/sos/v200/feature3.xml", "station-006.xml", EPSG_VERSION);

        Path featureV100Directory = featureDirectory.resolve("1.0.0");
        Files.createDirectories(featureV100Directory);
        writeDataFileEPSG(featureV100Directory, "org/constellation/sos/v100/feature1.xml", "station-001.xml", EPSG_VERSION);
        writeDataFileEPSG(featureV100Directory, "org/constellation/sos/v100/feature2.xml", "station-002.xml", EPSG_VERSION);
        writeDataFileEPSG(featureV100Directory, "org/constellation/sos/v100/feature3.xml", "station-006.xml", EPSG_VERSION);

        Path observationsDirectory = instDirectory.resolve("observations");
        Files.createDirectories(observationsDirectory);
        Path obsV200Directory = observationsDirectory.resolve("2.0.0");
        Files.createDirectories(obsV200Directory);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation1.xml", "urn:ogc:object:observation:GEOM:304.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation2.xml", "urn:ogc:object:observation:GEOM:305.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation3.xml", "urn:ogc:object:observation:GEOM:406.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation4.xml", "urn:ogc:object:observation:GEOM:307.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation5.xml", "urn:ogc:object:observation:GEOM:507.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation6.xml", "urn:ogc:object:observation:GEOM:801.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV200Directory, "org/constellation/sos/v200/observation7.xml", "urn:ogc:object:observation:GEOM:901.xml", EPSG_VERSION);

        Path obsV100Directory = observationsDirectory.resolve("1.0.0");
        Files.createDirectories(obsV100Directory);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/observation1.xml", "urn:ogc:object:observation:GEOM:304.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/observation2.xml", "urn:ogc:object:observation:GEOM:305.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/observation3.xml", "urn:ogc:object:observation:GEOM:406.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/observation4.xml", "urn:ogc:object:observation:GEOM:307.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/observation5.xml", "urn:ogc:object:observation:GEOM:507.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/observation6.xml", "urn:ogc:object:observation:GEOM:801.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure1.xml",     "urn:ogc:object:observation:GEOM:901-1-1.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure2.xml",     "urn:ogc:object:observation:GEOM:901-1-2.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure3.xml",     "urn:ogc:object:observation:GEOM:901-1-3.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure4.xml",     "urn:ogc:object:observation:GEOM:901-1-4.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure5.xml",     "urn:ogc:object:observation:GEOM:901-1-5.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure6.xml",     "urn:ogc:object:observation:GEOM:901-1-6.xml", EPSG_VERSION);
        writeDataFileEPSG(obsV100Directory, "org/constellation/sos/v100/measure7.xml",     "urn:ogc:object:observation:GEOM:901-1-7.xml", EPSG_VERSION);

        Path observationTemplatesDirectory = instDirectory.resolve("observationTemplates");
        Files.createDirectories(observationTemplatesDirectory);
        Path obsTV200Directory = observationTemplatesDirectory.resolve("2.0.0");
        Files.createDirectories(obsTV200Directory);
        writeDataFileEPSG(obsTV200Directory, "org/constellation/sos/v200/observationTemplate-3.xml", "urn:ogc:object:observation:template:GEOM:3.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV200Directory, "org/constellation/sos/v200/observationTemplate-4.xml", "urn:ogc:object:observation:template:GEOM:4.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV200Directory, "org/constellation/sos/v200/observationTemplate-5.xml", "urn:ogc:object:observation:template:GEOM:test-1.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV200Directory, "org/constellation/sos/v200/observationTemplate-6.xml", "urn:ogc:object:observation:template:GEOM:6.xml", EPSG_VERSION);
        //writeDataFileEPSG(obsTV200Directory, "org/constellation/sos/v200/observationTemplate-7.xml", "urn:ogc:object:observation:template:GEOM:7.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV200Directory, "org/constellation/sos/v200/observationTemplate-8.xml", "urn:ogc:object:observation:template:GEOM:8.xml", EPSG_VERSION);

        Path obsTV100Directory = observationTemplatesDirectory.resolve("1.0.0");
        Files.createDirectories(obsTV100Directory);
        writeDataFileEPSG(obsTV100Directory, "org/constellation/sos/v100/observationTemplate-3.xml", "urn:ogc:object:observation:template:GEOM:3.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV100Directory, "org/constellation/sos/v100/observationTemplate-4.xml", "urn:ogc:object:observation:template:GEOM:4.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV100Directory, "org/constellation/sos/v100/observationTemplate-5.xml", "urn:ogc:object:observation:template:GEOM:test-1.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV100Directory, "org/constellation/sos/v100/observationTemplate-6.xml", "urn:ogc:object:observation:template:GEOM:6.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV100Directory, "org/constellation/sos/v100/observationTemplate-7.xml", "urn:ogc:object:observation:template:GEOM:7-2.xml", EPSG_VERSION);
        writeDataFileEPSG(obsTV100Directory, "org/constellation/sos/v100/observationTemplate-8.xml", "urn:ogc:object:observation:template:GEOM:8.xml", EPSG_VERSION);


        Path sensorDirectory = instDirectory.resolve("sensors");
        Files.createDirectories(sensorDirectory);
        Path sensor1         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ1.xml");
        Files.createFile(sensor1);
        Path sensor2         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ2.xml");
        Files.createFile(sensor2);
        Path sensor3         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ3.xml");
        Files.createFile(sensor3);
        Path sensor4         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ4.xml");
        Files.createFile(sensor4);
        Path sensor5         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµtest-1.xml");
        Files.createFile(sensor5);
        Path sensor6         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ6.xml");
        Files.createFile(sensor6);
        Path sensor7         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ7.xml");
        Files.createFile(sensor7);
        Path sensor8         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ8.xml");
        Files.createFile(sensor8);
        Path sensor9         = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ9.xml");
        Files.createFile(sensor9);
        Path sensor10        = sensorDirectory.resolve("urnµogcµobjectµsensorµGEOMµ10.xml");
        Files.createFile(sensor10);

        return instDirectory;
    }
}
