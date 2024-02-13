package org.constellation.test.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.sql.DataSource;
import jakarta.xml.bind.Unmarshaller;
import org.apache.sis.internal.storage.image.WorldFileStoreProvider;
import org.apache.sis.referencing.CRS;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.apache.sis.xml.MarshallerPool;
import org.constellation.api.ProviderType;
import org.constellation.business.IProviderBusiness;
import org.constellation.business.ISensorBusiness;
import org.constellation.dto.Sensor;
import org.constellation.dto.service.config.generic.Automatic;
import org.constellation.dto.service.config.sos.ProcedureDataset;
import org.constellation.exception.ConstellationException;
import org.constellation.exception.ConstellationRuntimeException;
import org.constellation.generic.database.GenericDatabaseMarshallerPool;
import org.constellation.provider.DataProviderFactory;
import org.constellation.provider.DataProviders;
import org.constellation.provider.ObservationProvider;
import org.constellation.provider.ProviderParameters;
import static org.constellation.provider.ProviderParameters.getOrCreate;
import org.constellation.provider.SensorProvider;
import static org.constellation.test.utils.TestResourceUtils.unmarshallSensorResource;
import org.constellation.util.SQLUtilities;
import org.constellation.util.Util;
import org.geotoolkit.data.shapefile.ShapefileFolderProvider;
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
        public final String providerName;

        DataImport(int id, String namespace, String name, int pid, String providerName) {
            this.id = id;
            this.namespace = namespace;
            this.name = name;
            this.pid = pid;
            this.providerName = providerName;
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
        public static final TestResource PNG = new TestResource("org/constellation/data/image/png/SSTMDE200305.png", TestEnvironment::createWorldFileProvider, TestEnvironment::createWorldFileStore);

        /**
         * Coverage file datastore with TIF file.
         *
         *  data :
         *  - martinique
         */
        public static final TestResource TIF = new TestResource("org/constellation/data/image/tif/martinique.tif", TestEnvironment::createTifProvider, TestEnvironment::createTifStore);

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
         * NetCDF 3 dataset that contains two rows of fill-value. It is a 16x16 2D matrix centered on OGC:CRS::84 origin.
         * It contains 7 rows of value 1.0, then two rows of no-data values, then 7 rows of value 2.0.
         * Generated using Python library <a href="https://rasterio.readthedocs.io/en/latest/index.html">rasterio</a>.
         */
        public static final TestResource NETCDF_WITH_NAN = new TestResource("org/constellation/netcdf/with_nan.nc", TestEnvironment::createNCProvider);

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

        public static final TestResource STATISTICS_SHAPEFILES = new TestResource("org/constellation/data/shapefiles/statistics", TestEnvironment::createShapefileProvider, TestEnvironment::createShapefileStore, TestEnvironment::createShapefileProviders);

        /**
         * Observation and mesurement provider.
         *
         * data :
         * - http://www.opengis.net/sampling/1.0 : SamplingPoint
         */
        public static final TestResource OM2_DB = new TestResource(null, TestEnvironment::createOM2DatabaseProvider, TestEnvironment::createOM2DatabaseStore);
        public static final TestResource OM2_DB_NO_DATA = new TestResource(null, TestEnvironment::createOM2DatabaseProviderNoData);
        public static final TestResource OM2_DB_DUCK = new TestResource(null,  TestEnvironment::createOM2DatabaseDDBProviderNoData);


        public static final TestResource OM_XML = new TestResource("org/constellation/xml/sos/omfile/single-observations.xml", TestEnvironment::createOMFileProvider);
        public static final TestResource OM_GENERIC_DB = new TestResource("org/constellation/xml/sos/config/generic-config.xml", TestEnvironment::createOMGenericDBProvider);
        public static final TestResource OM_LUCENE = new TestResource("org/constellation/xml/sos",  TestEnvironment::createOMLuceneProvider, null);

        // Sensor Providers
        public static final TestResource SENSOR_FILE = new TestResource("org/constellation/xml/sos/sensors", TestEnvironment::createSensorFileProvider);
        public static final TestResource SENSOR_INTERNAL = new TestResource(null, TestEnvironment::createSensorInternalProvider);
        public static final TestResource SENSOR_OM2_DB = new TestResource(null, TestEnvironment::createSensorOM2Provider, TestEnvironment::createOM2DatabaseSensorStore);

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

        /**
         * GEOJSON file provider.
         *
         * data :
         * - land
         */
        public static final TestResource JSON_LAND = new TestResource("org/constellation/data/geojson/land.geojson", TestEnvironment::createGeoJsonProvider);

        /**
         * GEOJSON file provider.
         *
         * data :
         * - water
         */
        public static final TestResource JSON_WATER = new TestResource("org/constellation/data/geojson/water.geojson", TestEnvironment::createGeoJsonProvider);

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
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid, db.getProvider()))
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
                            .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid, db.getProvider()))
                            .collect(Collectors.toList());
                    results.add(new ProviderImport(pid, dsId, datas));
                }
                return new ProvidersImport(results);
           }
           throw new ConstellationRuntimeException("Missing test resource:" + tr.path);
        }

        public void generateSensors(ISensorBusiness sensorBusiness, int omProviderId, int smlProviderId) throws ConstellationException {
           ObservationProvider omProv = (ObservationProvider) DataProviders.getProvider(omProviderId);
           List<ProcedureDataset> procs   = omProv.getProcedureTrees(null);

           // default sensor initialisation from the sensor present in the OM provider
            for (ProcedureDataset proc : procs) {
                sensorBusiness.generateSensor(proc, smlProviderId, null, null);
            }

            // complete some sensor with sml
            createOrUpdateSensor("org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ1.xml", "urn:ogc:object:sensor:GEOM:1", "GEOM 1", "system", "timeseries", smlProviderId, sensorBusiness);
            createOrUpdateSensor("org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ2.xml", "urn:ogc:object:sensor:GEOM:2", "GEOM 2", "component", "profile", smlProviderId, sensorBusiness);
            createOrUpdateSensor("org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµtest-1.xml", "urn:ogc:object:sensor:GEOM:test-1", "test 1", "system", "timeseries", smlProviderId, sensorBusiness);
            createOrUpdateSensor("org/constellation/xml/sos/sensors/urnµogcµobjectµsensorµGEOMµ8.xml", "urn:ogc:object:sensor:GEOM:8", "GEOM 8", "system", "timeseries", smlProviderId, sensorBusiness);

            // reload sml provider to take the manually added
            SensorProvider smProv = (SensorProvider) DataProviders.getProvider(smlProviderId);
            smProv.reload();
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
                sensorBusiness.create(sensorId, sensorId, null, "system", null, null, sml, Long.MIN_VALUE, smlProviderId);
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
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid, db.getProvider()))
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
            String providerIdentifier = "aggCovSrc" + UUID.randomUUID().toString();
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
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid, db.getProvider()))
                       .collect(Collectors.toList());
            return new ProviderImport(pid, dsId, datas);
        } catch (Exception ex) {
            throw new ConstellationException(ex);
        }
    }

    public static ProviderImport createVectorAggregationProvider(IProviderBusiness providerBusiness, String dataName, List<Integer> dataIds, Integer datasetId) throws ConstellationException {
        try {
            String providerIdentifier = "vectAggSrc" + UUID.randomUUID().toString();
            final DataProviderFactory factory = DataProviders.getFactory("computed-resource");
            final ParameterValueGroup source  = factory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice =  ProviderParameters.getOrCreate((ParameterDescriptorGroup) factory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("vector-aggregation-with-extra-dimensions");

            config.parameter("DataName").setValue(dataName);

            for (Integer dataId : dataIds) {
                ParameterValueGroup configuration = config.addGroup("configuration");
                configuration.parameter("data").setValue(dataId);
                ParameterValueGroup temporal = configuration.addGroup("temporal-dimension");
                temporal.parameter("start-expression").setValue("when");
                ParameterValueGroup vertical = configuration.addGroup("vertical-dimension");
                vertical.parameter("start-expression").setValue("elevation");
            }

            int pid = providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "computed-resource", source);
            int dsId = providerBusiness.createOrUpdateData(pid, datasetId, true, false, null);
            List<DataImport> datas = providerBusiness.getDataBriefsFromProviderId(pid, null, true, false, false, false)
                       .stream().map(db -> new DataImport(db.getId(), db.getNamespace(), db.getName(), pid, db.getProvider()))
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

    private static Integer createWorldFileProvider(IProviderBusiness providerBusiness, Path pngFile) {
        final String providerIdentifier = "coverageTestSrc-" + UUID.randomUUID().toString();
        try {
            final DataProviderFactory dataStorefactory = DataProviders.getFactory("data-store");

            final ParameterValueGroup source = dataStorefactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) dataStorefactory.getStoreDescriptor(), source);
            final ParameterValueGroup config = choice.addGroup("World file");
            config.parameter("location").setValue(pngFile.toUri());

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

            final ParameterValueGroup config = choice.addGroup("GeoTIFF");
            config.parameter("location").setValue(tifFile.toUri().toURL());

            return providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "data-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static DataStore createTifStore(Path p) {
        try {
            GeoTiffStoreProvider provider = new GeoTiffStoreProvider();
            ParameterValueGroup params = provider.getOpenParameters().createValue();
            params.parameter("location").setValue(p);
            return provider.open(params);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static DataStore createWorldFileStore(Path p) {
        try {
            WorldFileStoreProvider provider = new WorldFileStoreProvider();
            ParameterValueGroup params = provider.getOpenParameters().createValue();
            params.parameter("location").setValue(p);
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

    private static String buildEmbeddedOM2Database(String providerIdentifier, boolean withData, boolean ddb) throws Exception {
        final String url;
        String urlSuffix = "";
        String fileSuffix = "";
        if (ddb) {
            url = "jdbc:duckdb:";
            fileSuffix = "_ddb";
        }else {
            /* you can pass in file mode for debugging purpose
               final String url = "jdbc:derby:/tmp/" + providerIdentifier + ".derby";
               System.out.println("DERBY URL:" + url);*/
            url = "jdbc:derby:memory:" + providerIdentifier;
            urlSuffix = ";create=true";
        }
        final DataSource ds = SQLUtilities.getDataSource(url + urlSuffix);
        try (final Connection con = ds.getConnection()) {
            final DerbySqlScriptRunner sr = new DerbySqlScriptRunner(con);
            String sql = IOUtilities.toString(Util.getResourceAsStream("org/constellation/om2/structure_observations" + fileSuffix + ".sql"));
            sql = sql.replace("$SCHEMA", "");
            sr.run(sql);
            if (withData) {
                sr.run(Util.getResourceAsStream("org/constellation/sql/sos-data-om2" + fileSuffix + ".sql"));
            }
        }
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
        return createOM2DatabaseProvider(providerBusiness, p, true, false);
    }

    private static Integer createOM2DatabaseProviderNoData(IProviderBusiness providerBusiness, Path p) {
        return createOM2DatabaseProvider(providerBusiness, p, false, false);
    }

    private static Integer createOM2DatabaseDDBProviderNoData(IProviderBusiness providerBusiness, Path p) {
        return createOM2DatabaseProvider(providerBusiness, p, false, true);
    }

    private static Integer createOM2DatabaseProvider(IProviderBusiness providerBusiness, Path p, boolean withData, boolean ddb) {
        try {
            final String providerIdentifier = "omSrc-" + UUID.randomUUID().toString();
            final String url = buildEmbeddedOM2Database(providerIdentifier, withData, ddb);

            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("observationSOSDatabase");
            config.parameter("sgbdtype").setValue(ddb ? "duckdb" : "derby");
            config.parameter("derbyurl").setValue(url);
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            config.parameter("max-field-by-table").setValue(10);

            return  providerBusiness.storeProvider(providerIdentifier, ProviderType.LAYER, "observation-store", source);
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static DataStore createOM2DatabaseStore(Path p) {
        try {
            final String providerIdentifier = "omSrc-" + UUID.randomUUID().toString();
            final String url = buildEmbeddedOM2Database(providerIdentifier, true, false);

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

            return omFactory.createProvider(providerIdentifier, source).getMainStore();
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }
    
    private static DataStore createOM2DatabaseSensorStore(Path p) {
        try {
            final String providerIdentifier = "omSrc-" + UUID.randomUUID().toString();
            final String url = buildEmbeddedOM2Database(providerIdentifier, true, false);

            final DataProviderFactory sensorFactory = DataProviders.getFactory("sensor-store");
            final ParameterValueGroup source    = sensorFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) sensorFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("om2sensor");
            config.parameter("sgbdtype").setValue("derby");
            config.parameter("derbyurl").setValue(url);
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");

            return sensorFactory.createProvider(providerIdentifier, source).getMainStore();
        } catch (Exception ex) {
            throw new ConstellationRuntimeException(ex);
        }
    }

    private static Integer createOMGenericDBProvider(IProviderBusiness providerBusiness, Path p) {
        try {
            final String providerIdentifier = "omGenericDBSrc-" + UUID.randomUUID().toString();

            final String url = buildEmbeddedOM2Database(providerIdentifier, true, false);

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

            Path configDir     = Files.createTempDirectory(providerIdentifier);
            Path SOSDirectory  = configDir.resolve("SOS");
            Path instDirectory = SOSDirectory.resolve("default");

            TestResourceUtils.copyEPSG(p, instDirectory);
            //createOMLuceneDataFile(instDirectory);

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
    
    private static Integer createSensorOM2Provider(IProviderBusiness providerBusiness, Path p) {
        boolean withData = false; // TODO
        boolean ddb = false; // TODO
        try {
            final String providerIdentifier = "omSensorSrc-" + UUID.randomUUID().toString();
            final String url = buildEmbeddedOM2Database(providerIdentifier, withData, ddb);

            final DataProviderFactory omFactory = DataProviders.getFactory("observation-store");
            final ParameterValueGroup source    = omFactory.getProviderDescriptor().createValue();
            source.parameter("id").setValue(providerIdentifier);
            final ParameterValueGroup choice = ProviderParameters.getOrCreate((ParameterDescriptorGroup) omFactory.getStoreDescriptor(), source);

            final ParameterValueGroup config = choice.addGroup("observationSOSDatabase");
            config.parameter("sgbdtype").setValue(ddb ? "duckdb" : "derby");
            config.parameter("derbyurl").setValue(url);
            config.parameter("phenomenon-id-base").setValue("urn:ogc:def:phenomenon:GEOM:");
            config.parameter("observation-template-id-base").setValue("urn:ogc:object:observation:template:GEOM:");
            config.parameter("observation-id-base").setValue("urn:ogc:object:observation:GEOM:");
            config.parameter("sensor-id-base").setValue("urn:ogc:object:sensor:GEOM:");
            config.parameter("max-field-by-table").setValue(10);

            return  providerBusiness.storeProvider(providerIdentifier, ProviderType.SENSOR, "sensor-store", source);
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
}
