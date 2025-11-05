package com.examind.community.storage.coverage.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.storage.DataStore;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStoreProvider;
import org.apache.sis.storage.GridCoverageResource;
import org.apache.sis.storage.ProbeResult;
import org.apache.sis.storage.Resource;
import org.apache.sis.storage.StorageConnector;
import org.apache.sis.util.iso.Names;
import org.geotoolkit.nio.IOUtilities;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.examind.community.storage.coverage.aggregation.GridAggregation.aggregateTimeSeries;
import static com.examind.community.storage.coverage.aggregation.GridAggregation.resolveSourcesAndSetTime;
import java.net.URI;
import java.nio.file.Paths;

public class LinearGridTimeSeries extends DataStoreProvider {

    public static final String NAME = "LinearGridTimeSeries";
    public static final ParameterDescriptor<URI> CONF_PATH;
    public static final ParameterDescriptorGroup OPEN_PARAMS;
    static {
        var builder = new ParameterBuilder();
        CONF_PATH = builder
                .addName(LOCATION)
                .setDescription("Path to the time-series configuration file.")
                .setRequired(true)
                .create(URI.class, null);
        OPEN_PARAMS = builder.addName("LinearGridTimeSeries").createGroup(CONF_PATH);
    }

    @Override
    public String getShortName() {
        return NAME;
    }

    @Override
    public ParameterDescriptorGroup getOpenParameters() {
        return OPEN_PARAMS;
    }

    @Override
    public ProbeResult probeContent(StorageConnector storageConnector) throws DataStoreException {
        return probeContent(storageConnector, Path.class, this::testConf);
    }

    @Override
    public DataStore open(StorageConnector storageConnector) throws DataStoreException {
        var confFile = Objects.requireNonNull(storageConnector.getStorageAs(URI.class), "No configuration file provided (a java nio URI is expected)");
        return open(confFile);
    }

    @Override
    public DataStore open(ParameterValueGroup parameters) throws DataStoreException {
        var confFile = Parameters.castOrWrap(parameters).getMandatoryValue(CONF_PATH);
        return open(confFile);
    }

    private DataStore open(URI confUri) throws DataStoreException {
        Path confFile = Paths.get(confUri);
        final Configuration conf = readConf(confFile);
        final Pair<List<DataStore>,List<Resource>> sources = resolveSourcesAndSetTime(conf);
        //TODO: use this instead of custom time-series, once (and only once) it's stable and work well for time-series management.
        //GridCoverageResource resultAggregation = aggregateCoverageAggregator(sources.getRight(), conf);
        GridCoverageResource resultAggregation = aggregateTimeSeries(sources.getRight(), conf);

        return new GridAggregation(Names.createLocalName(null, null, conf.name), sources.getLeft(), resultAggregation, toParam(confFile), confFile, conf.watchFiles);
    }

    public static Configuration readConf(Path confFile) throws DataStoreException {
        String extension = IOUtilities.extension(confFile);
        if (extension != null && !extension.equalsIgnoreCase("json")) {
            throw new DataStoreException("Invalid configuration file provided (" + confFile + ")");
        }

        var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        FileSystem fs = confFile.getFileSystem();
        Path confFileDir = confFile.getParent();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(FileConfiguration.class, new FileConfigurationDeserializer(fs, confFileDir));
        mapper.registerModule(module);
        try (var input = Files.newInputStream(confFile)) {
            return mapper.readValue(input, Configuration.class);
        } catch (IOException e) {
            throw new DataStoreException("Cannot open configuration file", e);
        }
    }

    private ParameterValueGroup toParam(Path confFile) {
        var params = Parameters.castOrWrap(OPEN_PARAMS.createValue());
        params.getOrCreate(CONF_PATH).setValue(confFile);
        return params;
    }

    private ProbeResult testConf(Path confFile) {
        try {
            readConf(confFile);
            return ProbeResult.SUPPORTED;
        } catch (DataStoreException e) {
            return ProbeResult.UNSUPPORTED_STORAGE;
        }
    }

    /////////////////////////////////////////////////////////////////////
    // CONFIGURATION RECORDS
    /////////////////////////////////////////////////////////////////////

    /**
     *
     * @param path Path of the file storing the data
     * @date date DateTime value on temporal axis of the data
     */
    public record FileConfiguration(Path path, OffsetDateTime startdate, OffsetDateTime enddate) {}

    /**
     *
     * @param name Name of the data to create
     * @param files List of files (path and datetime of the data) for the timeSeries
     */
    public record Configuration(String name, Boolean watchFiles, List<FileConfiguration> files) {
        public Configuration {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name must be set and not empty/blank.");
            if (files == null || files.isEmpty()) throw new IllegalArgumentException("files must be set and not empty.");
            if (watchFiles == null) watchFiles = false;
        }
    }
}
