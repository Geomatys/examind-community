package com.examind.community.storage.coverage.aggregation;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.sis.coverage.SampleDimension;
import org.apache.sis.coverage.grid.GridCoverage;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.coverage.grid.GridGeometry;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.operation.matrix.Matrices;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.storage.*;
import org.apache.sis.storage.aggregate.CoverageAggregator;
import org.apache.sis.util.iso.Names;
import org.geotoolkit.storage.ResourceProcessor;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.Metadata;
import org.opengis.metadata.spatial.DimensionNameType;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.util.FactoryException;
import org.opengis.util.GenericName;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * GridAggregation represents a data store for aggregated grid coverages.
 *
 * This class extends DataStore and implements GridCoverageResource, providing functionalities
 * for managing and aggregating grid coverages. It allows for the aggregation of multiple
 * grid coverages into a single aggregated coverage.
 *
 * GridAggregation includes functionality for monitoring configuration file
 * changes and updating the aggregation accordingly. It utilizes a watch service to monitor
 * specified configuration files for modifications, allowing for dynamic updates to the
 * aggregation when the underlying data sources change.
 *
 * @author Quentin BIALOTA
 */
public class GridAggregation extends DataStore implements GridCoverageResource {

    /**
     * Logger instance for logging messages related to this class.
     */
    private static final Logger LOGGER = Logger.getLogger("com.examind.community.storage.coverage.aggregation");

    /**
     * The name of the aggregation.
     */
    private final GenericName name;

    /**
     * The list of data stores containing the sources of the aggregation.
     */
    private List<DataStore> sources;

    /**
     * The aggregated grid coverage resource.
     */
    private GridCoverageResource aggregation;

    /**
     * The parameters associated this grid aggregation.
     */
    private final ParameterValueGroup openParams;

    /**
     * Indicates whether the grid aggregation is actively watching files for changes.
     */
    private boolean watchingFiles;

    /**
     * The path to the configuration file associated with this grid aggregation.
     */
    private final Path configFile;

    /**
     * A mapping of configuration file paths to grid aggregations for monitoring file changes.
     */
    private static final Map<Path, GridAggregation> watchingConfigFileMap = new HashMap<>();

    /**
     * A mapping of directory paths to watch keys for monitoring file changes.
     */
    private static final Map<Path, WatchKey> watchingKeysConfigFileMap = new HashMap<>();

    /**
     * A map that associates each file system with its corresponding Thread.
     * This map is used to manage multiple WatchService and Thread instances, where each Thread (and WatchService associated)
     * is responsible for watching directories within its associated file system.
     */
    private static final Map<FileSystem, Thread> fileSystemToThreadMap = new HashMap<>();
    private static final Map<FileSystem, WatchService> fileSystemToWatchServiceMap = new HashMap<>();

    //TODO: delete these constants when CoverageAggregator works with non-regular data
    private final static Instant tropicalYearOrigin = Instant.parse("2000-01-01T00:00:00Z");
    private final static double offset = 31556925445.0;

    /**
     * Constructs a {@link GridAggregation} object with the specified parameters.
     *
     * @param name          The name of the aggregation.
     * @param sources       The list of data stores containing the sources of the aggregation.
     * @param aggregation   The aggregated grid coverage resource.
     * @param openParams    The parameters used to open this grid aggregation.
     * @param confFile      The path to the configuration file associated with this grid aggregation.
     * @param watchingFiles Indicates whether the grid aggregation is actively watching files for changes.
     */
    public GridAggregation(GenericName name, List<DataStore> sources, GridCoverageResource aggregation,
                           ParameterValueGroup openParams, Path confFile, boolean watchingFiles) {
        this.name = Objects.requireNonNull(name, "Aggregation name");
        this.sources = Objects.requireNonNull(sources, "Aggregation sources");
        this.aggregation = Objects.requireNonNull(aggregation, "Aggregation result");
        this.openParams = openParams;
        this.watchingFiles = watchingFiles;

        //TODO: Find why /usr/local/tomcat/file: is added in ServerFile mode in the path (and remove this part when fixed)
        String filePath = confFile.toString();
        int separatorPos = filePath.indexOf(":");
        String partAfterSeparator;
        if (separatorPos != -1) {
            partAfterSeparator = filePath.substring(separatorPos + 1);
            confFile = confFile.getFileSystem().getPath(partAfterSeparator);
        }
        ///////////////

        this.configFile = confFile;

        if(this.openParams.parameter("location").getValue() != null && this.watchingFiles) {
            createWatchThread(confFile.getFileSystem());
            addToWatchThread(configFile, this);
        }
    }

    /**
     * Creates a new watch thread to monitor the specified directory for file modifications.
     * If the watch thread is not already running, it starts a new thread to monitor file modifications.
     * This method ensures thread safety by synchronizing access to shared resources.
     *
     * @param fs The filesystem used for the service
     */
    private static synchronized void createWatchThread(FileSystem fs) {
        if (!fileSystemToThreadMap.containsKey(fs)) {
            try {
                WatchService service = fs.newWatchService();
                Thread watchThread = new Thread(() -> watchFiles(service));
                watchThread.setDaemon(true);
                watchThread.start();
                fileSystemToThreadMap.put(fs,watchThread);
                fileSystemToWatchServiceMap.put(fs, service);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Stops the watch thread associated with the specified file system if it is running and no files are being watched.
     *
     * This method checks if the watch thread associated with the given file system exists, is alive,
     * and if there are no files being watched on that file system. If these conditions are met,
     * it stops the watch thread.
     *
     * @param fs The file system for which the watch thread should be stopped.
     */
    private static synchronized void stopWatchThread(FileSystem fs) {
        Thread t = fileSystemToThreadMap.get(fs);

        if (t != null && t.isAlive()) {
            boolean noKeysForFileSystem = watchingKeysConfigFileMap.keySet().stream()
                    .noneMatch(entry -> entry.getFileSystem().equals(fs));
            if(noKeysForFileSystem) {
                t.stop();
                fileSystemToThreadMap.remove(fs);
                fileSystemToWatchServiceMap.remove(fs);
            }
        }
    }

    /**
     * Adds the configFile to the existing watch thread for monitoring file modifications
     * (if the directory of the config file is not already watched).
     * This method ensures thread safety by synchronizing access to shared resources.
     *
     * @param configFile The configFile to be added to the watch thread (directory of the file will be added)
     * @param store      The {@link GridAggregation} store of the configFile
     */
    private static synchronized void addToWatchThread(Path configFile, GridAggregation store) {
        FileSystem fileSystem = configFile.getFileSystem();
        Thread watchThread = fileSystemToThreadMap.get(fileSystem);

        if (watchThread == null || !watchThread.isAlive()) {
            // If the thread does not exist or is not alive, create a new watch thread
            createWatchThread(fileSystem);
            watchThread = fileSystemToThreadMap.get(fileSystem);
        }

        WatchService watchService = fileSystemToWatchServiceMap.get(fileSystem);

        if (watchThread != null && watchService != null) {
            watchingConfigFileMap.put(configFile, store);
            Path parentDirectory = configFile.getParent();

            if (!watchingKeysConfigFileMap.containsKey(parentDirectory)) {
                try {
                    WatchKey key = parentDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    watchingKeysConfigFileMap.put(parentDirectory, key);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Removes a file from the watch thread.
     *
     * @param configFile The path of the file to be removed from the watch thread.
     */
    private static synchronized void removeFromWatchThread(Path configFile) {
        if (watchingConfigFileMap.containsKey(configFile)) {
            watchingConfigFileMap.remove(configFile);

            Path parentDirectory = configFile.getParent();
            if (!watchingConfigFileMap.keySet().stream().anyMatch(p -> p.getParent().equals(parentDirectory))) {
                WatchKey key = watchingKeysConfigFileMap.remove(parentDirectory);
                key.cancel();
            }
        }
    }

    /**
     * Monitors specified directories for file modifications using a watch service.
     *
     * This method runs in a separate thread, continuously monitoring the directories
     * for file modification events. When a modification event is detected, it checks
     * if the modified file corresponds to a configuration file being watched by any
     * GridAggregation instance. If so, it triggers the {@link GridAggregation#updateStore(Path)} method of the
     * corresponding GridAggregation object to update the aggregation based on the
     * modified configuration file.
     *
     * If the watch service is interrupted or closed, the method gracefully exits the
     * loop and terminates.
     *
     * @param service The watch service associated with the directory to be monitored.
     */
    private static void watchFiles(WatchService service) {
        final Thread currentThread = Thread.currentThread();

        while (!currentThread.isInterrupted()) {
            final WatchKey key;
            try {
                key = service.take();
            } catch (ClosedWatchServiceException e) {
                LOGGER.log(Level.INFO, "Folder watcher has been closed !");
                break;
            } catch (InterruptedException e) {
                LOGGER.log(Level.INFO, "Folder watcher has been interrupted !");
                currentThread.interrupt();
                break;
            }

            // Do the check here, because we don't know how many time we have waited for an event, nor what happened.
            if (currentThread.isInterrupted()) {
                break;
            }

            // If could happen if we've cancelled if and it was already waiting for new events. We just have to ignore it.
            if (!key.isValid()) {
                continue;
            }

            Path dir = (Path)key.watchable();

            for (WatchEvent event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // Watch service received too many events from the system and cannot handle them.
                if (kind == OVERFLOW) {
                    LOGGER.log(Level.INFO, "Too many changes happened to the watched directory. Unable to catch them all.");
                    continue;
                }

                try {
                    Path target;
                    final Object context = event.context();
                    if (context instanceof Path) {
                        target = (Path) context;
                    } else if (context instanceof File) {
                        target = ((File) context).toPath();
                    } else {
                        // Create an exception to be able to retrieve code fragment from message, which will allow to quickly add new cases if needed.
                        final IllegalArgumentException e = new IllegalArgumentException("Watch event is of unknown type (need Path or File).");
                        LOGGER.log(Level.INFO, "Watch event skipped.", e);
                        continue;
                    }

                        Path fullPath = dir.resolve(target);

                        if (kind == ENTRY_MODIFY) {
                            GridAggregation gridAggregation = watchingConfigFileMap.get(fullPath);
                            if (gridAggregation != null) {
                                gridAggregation.updateStore(fullPath);
                            }
                        }

                } catch (Exception e) {
                    // We don't want the entire mechanism to be destroyed for a simple error on a file.
                    LOGGER.log(Level.WARNING, "An error occurred while processing " + event.context() + " for the event " + kind, e);
                }
                key.reset();
            }
        }
    }

    /**
     * Updates the aggregation based on the modifications in the configuration file.
     *
     * This method reads the updated configuration file, constructs a new aggregation
     * based on the modified data sources and parameters, and updates the internal
     * state of the GridAggregation object accordingly. If watchingFiles is enabled,
     * it adds the updated configuration file to the watch list and starts monitoring
     * it for further modifications. If watchingFiles is disabled, it removes the
     * configuration file from the watch list and stops the watch thread if no other
     * configuration files are being monitored.
     *
     * @param updatedConfigFile The path to the updated configuration file.
     * @throws DataStoreException If an error occurs while updating the data store.
     */
    private void updateStore(Path updatedConfigFile) throws DataStoreException {
        final LinearGridTimeSeries.Configuration newConfig = LinearGridTimeSeries.readConf(updatedConfigFile);

        final Pair<List<DataStore>,List<Resource>> sources = resolveSourcesAndSetTime(newConfig);

        //TODO: use this instead of custom time-series, once (and only once) it's stable and work well for time-series management.
        //GridCoverageResource resultAggregation = aggregateCoverageAggregator(sources.getRight(), conf);
        GridCoverageResource resultAggregation = aggregateTimeSeries(sources.getRight(), newConfig);

        this.sources.clear();
        this.sources = sources.getLeft();
        this.aggregation = resultAggregation;
        this.watchingFiles = newConfig.watchFiles();

        if(this.watchingFiles) {
            createWatchThread(updatedConfigFile.getFileSystem());
            addToWatchThread(updatedConfigFile,this);
        }
        else {
            removeFromWatchThread(updatedConfigFile);
            stopWatchThread(updatedConfigFile.getFileSystem()); //Check and stop the thread if needed
        }
    }

    @Override
    public Optional<GenericName> getIdentifier() throws DataStoreException {
        return Optional.of(name);
    }

    @Override
    public Optional<ParameterValueGroup> getOpenParameters() {
        return Optional.ofNullable(openParams);
    }

    @Override
    public Metadata getMetadata() throws DataStoreException {
        return aggregation.getMetadata();
    }

    @Override
    public void close() throws DataStoreException {
        removeFromWatchThread(configFile);
        stopWatchThread(configFile.getFileSystem()); //Check and stop the thread if needed
        var error = sources.stream().parallel()
                .map(store -> {
                    try {
                        store.close();
                        return null;
                    } catch (Exception e) {
                        return e;
                    }
                })
                .filter(Objects::nonNull)
                .collect(
                        () -> new DataStoreException("Some source datastores failed to be closed. See suppressed errors for details"),
                        DataStoreException::addSuppressed,
                        (err1, err2) -> {
                            for (var e : err2.getSuppressed()) err1.addSuppressed(e);
                        }
                );
        if (error == null || error.getSuppressed().length < 1) return;
        throw error;
    }

    @Override
    public GridGeometry getGridGeometry() throws DataStoreException {
        return aggregation.getGridGeometry();
    }

    @Override
    public List<SampleDimension> getSampleDimensions() throws DataStoreException {
        return aggregation.getSampleDimensions();
    }

    @Override
    public GridCoverageResource subset(Query query) throws UnsupportedQueryException, DataStoreException {
        return aggregation.subset(query);
    }

    @Override
    public GridCoverage read(GridGeometry gridGeometry, int... bands) throws DataStoreException {
        return aggregation.read(gridGeometry, bands);
    }

    @Override
    public Optional<Envelope> getEnvelope() throws DataStoreException {
        return aggregation.getEnvelope();
    }

    /**
     * Aggregate multiple grid coverage resources using {@link CoverageAggregator}.
     *
     * @param sources List of resources to aggregate
     * @param conf    Configuration for the aggregation
     * @return        Aggregated grid coverage resource
     * @throws IllegalArgumentException If input data is not an image/coverage or if there's an issue adding data to the aggregate
     * @throws IllegalStateException    If the temporal aggregate is not working or if there's an issue finding a raster resource in the built aggregate
     */
    public static GridCoverageResource aggregateCoverageAggregator(List<Resource> sources, LinearGridTimeSeries.Configuration conf) {
        final CoverageAggregator agg = new CoverageAggregator();
        final Iterator<Resource> it = sources.iterator();

        while (it.hasNext()) {
            final Resource resource = it.next();
            if (!(resource instanceof GridCoverageResource gcr)) {
                throw new IllegalArgumentException("Input data is not an image/coverage");
            }
            try {
                agg.add(gcr);
            } catch (DataStoreException e) {
                throw new IllegalArgumentException("Cannot add data in aggregate", e);
            }
        }

        try {
            final Resource result = agg.build(Names.createLocalName(null, null, conf.name()));
            final Collection<GridCoverageResource> candidates = org.geotoolkit.storage.DataStores.flatten(result, true, GridCoverageResource.class);
            final Iterator<GridCoverageResource> itCandidates = candidates.iterator();
            if (!itCandidates.hasNext()) throw new IllegalStateException("Temporal aggregate is not working");
            return itCandidates.next();
        } catch (DataStoreException e) {
            throw new IllegalStateException("Cannot find a raster resource in built aggregate");
        }
    }

    /**
     * Aggregate multiple grid coverage resources using {@link TimeSeries}.
     * TODO: Remove this methos when CoverageAggregator works with non-regular data
     *
     * @param sources List of resources to aggregate
     * @param conf    Configuration for the aggregation
     * @return        Aggregated grid coverage resource
     * @throws IllegalArgumentException If input data is not an image/coverage
     */
    public static GridCoverageResource aggregateTimeSeries(List<Resource> sources, LinearGridTimeSeries.Configuration conf) {
        List<GridCoverageResource> sourcesGcr = new ArrayList<>();
        final Iterator<Resource> it = sources.iterator();

        while (it.hasNext()) {
            final Resource resource = it.next();
            if (!(resource instanceof GridCoverageResource gcr)) {
                throw new IllegalArgumentException("Input data is not an image/coverage");
            }
            sourcesGcr.add(gcr);
        }

        return new TimeSeries(sourcesGcr, null);
    }

    /**
     * Adds a temporal dimension to the source grid geometry.
     *
     * @param source    Source grid geometry to which to add the temporal dimension
     * @param date      Reference instant for the temporal dimension to add
     * @param timeSpan  Duration of the temporal dimension to add
     * @return          Grid geometry with the added temporal dimension
     * @throws IllegalArgumentException If the input grid geometry is incomplete
     * @throws IllegalStateException    If the temporal dimension cannot be added to the source data's spatial reference
     */
    private static GridGeometry addTime(GridGeometry source, Instant date, Duration timeSpan) {
        if (!source.isDefined(GridGeometry.CRS + GridGeometry.EXTENT + GridGeometry.GRID_TO_CRS)) {
            throw new IllegalArgumentException("Input grid geometry is incomplete");
        }

        //long timeCell = Period.between(tropicalYearOrigin.atOffset(ZoneOffset.UTC).toLocalDate(), date.toLocalDate()).getYears();

        final GridExtent ext = source.getExtent();
        //var expandedExtent = ext.insertDimension(ext.getDimension(), DimensionNameType.TIME, timeCell, timeCell, true);
        var expandedExtent = ext.insertDimension(ext.getDimension(), DimensionNameType.TIME, 0, 0, true);
        final int targetDim = expandedExtent.getDimension();
        var timeConversionMatrix = Matrices.createIdentity(targetDim + 1);

        // time offset
        //timeConversionMatrix.setElement(targetDim - 1, targetDim, tropicalYearOrigin.toEpochMilli());
        timeConversionMatrix.setElement(targetDim - 1, targetDim, date.toEpochMilli());
        // time scale
        //timeConversionMatrix.setElement(targetDim - 1, targetDim -1, offset);
        timeConversionMatrix.setElement(targetDim - 1, targetDim -1, timeSpan.toMillis());

        var expandedGrid2Crs = MathTransforms.concatenate(
                MathTransforms.linear(timeConversionMatrix),
                MathTransforms.passThrough(0, source.getGridToCRS(PixelInCell.CELL_CORNER), 1)
        );

        CoordinateReferenceSystem expandedCrs;
        try {
            expandedCrs = CRS.compound(source.getCoordinateReferenceSystem(), CommonCRS.Temporal.JAVA.crs());
        } catch (FactoryException e) {
            throw new IllegalStateException("Cannot add time dimension to data CRS", e);
        }
        return new GridGeometry(expandedExtent, PixelInCell.CELL_CORNER, expandedGrid2Crs, expandedCrs);
    }

    /**
     * Resolves sources and sets time for the given configuration
     *
     * This method iterates over the file configurations provided,
     * opens each data store associated with the file path, retrieves the grid coverage resource,
     * adds a temporal dimension to the grid geometry based on the start and end dates provided in the file configuration,
     * and resamples the grid coverage resource.
     *
     * @param conf The linear grid time series configuration containing file configurations
     * @return A pair containing lists of data stores and grid coverage resources resolved from the file configurations
     * @throws DataStoreException If an error occurs while accessing the data store or processing the resources
     * @throws IllegalArgumentException If the input data is not an image/coverage or if there's an issue opening the data store
     */
    public static Pair<List<DataStore>, List<Resource>> resolveSourcesAndSetTime(LinearGridTimeSeries.Configuration conf) throws DataStoreException {
        final Iterator<LinearGridTimeSeries.FileConfiguration> it = conf.files().iterator();
        List<Resource> resources = new ArrayList<>();
        List<DataStore> stores = new ArrayList<>();

        try {
            while (it.hasNext()) {
                final LinearGridTimeSeries.FileConfiguration fileConfiguration = it.next();

                DataStore ds = DataStores.open(fileConfiguration.path());

                GridCoverageResource gcr = firstCoverage(ds);
                if (gcr == null) {
                    it.remove();
                    throw new IllegalArgumentException("Input data is not an image/coverage");
                }

                GridGeometry resultGeometry = addTime(gcr.getGridGeometry(), fileConfiguration.startdate().toInstant(),
                        Duration.between(fileConfiguration.startdate(), fileConfiguration.enddate()));

                gcr = new ResourceProcessor().resample(gcr, resultGeometry, null);
                stores.add(ds);
                resources.add(gcr);
            }
        } catch (DataStoreException e) {
            throw new IllegalArgumentException("Cannot open the datastore for the path specified", e);
        }

        return Pair.of(stores, resources);
    }

    /**
     * Selects a grid coverage resource from the given resource.
     * Current implementation takes the first grid coverage.
     * Future implementation should do something better.
     *
     * @param resource The input resource from which to select a grid coverage resource
     * @return The selected grid coverage resource, or {@code null} if none is found
     * @throws DataStoreException If an error occurs while accessing the data store
     */
    private static GridCoverageResource firstCoverage(final Resource resource) throws DataStoreException {
        if (resource instanceof GridCoverageResource) {
            return (GridCoverageResource) resource;
        }
        if (resource instanceof Aggregate) {
            for (final Resource component : ((Aggregate) resource).components()) {
                if (component instanceof GridCoverageResource) {
                    return (GridCoverageResource) component;
                }
            }
        }
        return null;
    }

}
