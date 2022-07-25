package com.examind.community.storage.interop;

import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.internal.storage.Capability;
import org.apache.sis.internal.storage.ResourceOnFileSystem;
import org.apache.sis.internal.storage.StoreMetadata;
import org.apache.sis.internal.storage.image.WorldFileStoreProvider;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.Aggregate;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.DataStores;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.storage.geotiff.GeoTiffStore;
import org.apache.sis.storage.geotiff.GeoTiffStoreProvider;
import org.apache.sis.util.DefaultInternationalString;
import org.constellation.provider.DataProviders;
import org.geotoolkit.storage.Bundle;
import org.geotoolkit.storage.ResourceType;
import org.geotoolkit.storage.StoreMetadataExt;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.util.GenericName;

import static com.examind.community.storage.interop.FileCoverageProvider.NAME;

/**
 * Bypass deletion of Geotoolkit FileCoverageProvider driver.
 * This class intend to provide a safe way to open datasets registered in database as Geotk file-coverage resources.
 *
 * <em>This should not be used as a primary data provider.</em>
 * Only use it as an interoperability fallback.
 *
 * @author Alexis Manin (Geomatys)
 */
@StoreMetadata(
        yieldPriority = true,
        formatName = NAME,
        capabilities = { Capability.READ },
        resourceTypes = GridCoverageResource.class)
@StoreMetadataExt(resourceTypes = ResourceType.GRID)
public class FileCoverageProvider extends DataStoreProvider {

    /** factory identification **/
    public static final String NAME = "coverage-file";

    /**
     * Format identifier meaning "automatic detection of data format".
     */
    public static final String FORMAT_AUTO = "AUTO";

    /**
     * Mandatory - path to the file/folder to open.
     */
    public static final ParameterDescriptor<URI> PATH;

    /**
     * Mandatory - the image reader type.
     * Use AUTO if type should be detected automatically.
     */
    public static final ParameterDescriptor<String> TYPE;

    /**
     * @deprecated Not supported anymore. Declared only to be able to read back parameter defined with it. Ignored otherwise.
     */
    public static final ParameterDescriptor<String> PATH_SEPARATOR;

    public static final ParameterDescriptorGroup PARAMETERS_DESCRIPTOR;

    static {
        final ParameterBuilder builder = new ParameterBuilder().setRequired(false);

        PATH_SEPARATOR= builder
                .addName("pathSeparator")
                .addName(Bundle.formatInternational(Bundle.Keys.pathSeparator))
                .setRemarks(Bundle.formatInternational(Bundle.Keys.pathSeparator_remarks))
                .create(String.class, null);

        builder.setRequired(true);

        // Note: remove internation version of the name, that was olways defined as 'URL'.
        PATH = builder
                .addName(LOCATION)
                .setDeprecated(true)
                .addName("path")
                .addName("URL")
                .setDeprecated(false)
                .create(URI.class, null);

        var types = createReaderTypeList();

        TYPE = builder
                .addName("type")
                .setDeprecated(true)
                .addName(new DefaultInternationalString(Map.of(Locale.ROOT, "Image format", Locale.FRENCH, "Format d'image")))
                .setDeprecated(false)
                .createEnumerated(String.class, types, FORMAT_AUTO);

        PARAMETERS_DESCRIPTOR = builder.addName(NAME)
                .addName("FileCoverageStoreParameters")
                .createGroup(PATH, TYPE, PATH_SEPARATOR);
    }

    /**
     * ONLY FOR INTERNAL USE.
     *
     * List all available formats.
     */
    private static String[] createReaderTypeList() {
        ImageIO.scanForPlugins();

        final String[] imageIONames = ImageIO.getReaderFormatNames();
        final String[] withAuto = Arrays.copyOf(imageIONames, imageIONames.length + 1);
        withAuto[imageIONames.length] = FORMAT_AUTO;

        return withAuto;
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return PARAMETERS_DESCRIPTOR;
    }

    @Override
    public ProbeResult probeContent(StorageConnector storageConnector) {
        return ProbeResult.UNSUPPORTED_STORAGE;
    }

    @Override
    public DataStore open(StorageConnector storageConnector) throws DataStoreException {
        throw new UnsupportedOperationException("This datastore can only open back data registered using Geotk CoverageFile parameters.");
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        final Parameters input = Parameters.castOrWrap(parameters);
        final String sep = input.getValue(PATH_SEPARATOR);
        if (sep != null && !sep.isEmpty()) DataProviders.LOGGER.warning(() -> "Path separator parameter specified. It will be ignored. Input value was ["+sep+"]");
        final URI file = input.getMandatoryValue(PATH);
        final StorageConnector storage = new StorageConnector(file);
        final String imageType = input.getMandatoryValue(TYPE);
        var datastore = switch (imageType.toLowerCase(Locale.ROOT)) {
            case "auto" -> openAuto(storage);
            case "tif", "tiff", "geotiff", "geotif", "tiff-wf" -> openTiff(storage);
            case "png-wf", "jpeg-wf" -> openWorldFile(storage);
            default -> throw new UnsupportedOperationException("Unknown image type: "+imageType);
        };

        if (datastore instanceof GeoTiffStore tiffStore) {
            datastore = validateTiffDataStore(tiffStore, file);
        }

        return new FileCoverageStore(this, storage, datastore);
    }

    private DataStore openTiff(StorageConnector storage) throws DataStoreException {
        return new GeoTiffStoreProvider().open(storage);
    }

    private DataStore openWorldFile(StorageConnector storage) throws DataStoreException {
        return new WorldFileStoreProvider().open(storage);
    }

    private DataStore openAuto(StorageConnector storage) throws DataStoreException {
        return DataStores.open(storage);
    }

    private DataStore validateTiffDataStore(GeoTiffStore tiffStore, URI file) throws DataStoreException {
        try {
            final GridCoverageResource next = tiffStore.components().iterator().next();
            next.getGridGeometry().getCoordinateReferenceSystem();
            return tiffStore;
        } catch (Exception e) {
            tiffStore.close();
            final DataStoreProvider geotkProvider = org.geotoolkit.storage.DataStores.getProviderById("geotk-geotiff");
            try {
                return geotkProvider.open(new StorageConnector(file));
            } catch (Exception bis) {
                e.addSuppressed(bis);
                throw e;
            }
        }
    }

    private static class FileCoverageStore extends DataStore implements GridCoverageResource, ResourceOnFileSystem {

        private final DataStore source;

        private final GridCoverageResource dataset;

        private final Path[] files;

        private FileCoverageStore(FileCoverageProvider provider, StorageConnector connector, DataStore source) throws DataStoreException {
            super(provider, connector);
            this.source = source;
            if (source instanceof GridCoverageResource gcr) dataset = gcr;
            else if (source instanceof Aggregate agg) {
                final Iterator<? extends Resource> components = agg.components().iterator();
                if (!components.hasNext()) throw new IllegalArgumentException("No resource in decorated data store");
                final Resource next = components.next();
                if (components.hasNext()) throw new IllegalArgumentException("Decorated data store should provide only a single dataset");
                if (next instanceof GridCoverageResource gcr) dataset = gcr;
                else throw new IllegalArgumentException("Decorated data store does not provide any grid coverage resource");
            } else throw new IllegalArgumentException("Decorated data store does not provide any grid coverage resource");

            if (source instanceof ResourceOnFileSystem rof) files = rof.getComponentFiles();
            // GeoTiff datastore is not a resource on file-system, but its component is one.
            else if (dataset instanceof ResourceOnFileSystem rof) files = rof.getComponentFiles();
            // At this point, the storage connector is already closed, so we cannot use it anymore to get storage as path.
            else throw new IllegalArgumentException("Cannot determine file-system resources used.");
        }

        @Override
        public Optional<GenericName> getIdentifier() throws DataStoreException {
            return source.getIdentifier();
        }

        @Override
        public Path[] getComponentFiles() {
            return Arrays.copyOf(files, files.length);
        }

        @Override
        public Optional<ParameterValueGroup> getOpenParameters() {
            // Should we return the coverage-file parameters, or the decorated datastore arguments ?
            // As this is a "fallback" strategy, we will leave it empty for now, and check if it causes problems in the future.
            return Optional.empty();
        }

        @Override
        public Metadata getMetadata() throws DataStoreException {
            return source.getMetadata();
        }

        @Override
        public void close() throws DataStoreException {
            source.close();
        }

        @Override
        public GridGeometry getGridGeometry() throws DataStoreException {
            return dataset.getGridGeometry();
        }

        @Override
        public List<SampleDimension> getSampleDimensions() throws DataStoreException {
            return dataset.getSampleDimensions();
        }

        @Override
        public GridCoverage read(GridGeometry domain, int... range) throws DataStoreException {
            return dataset.read(domain, range);
        }

        @Override
        public Optional<Envelope> getEnvelope() throws DataStoreException {
            return dataset.getEnvelope();
        }
    }
}
