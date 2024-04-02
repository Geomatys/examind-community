package com.examind.community.storage.coverage.aggregation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class FileConfigurationDeserializer extends StdDeserializer<LinearGridTimeSeries.FileConfiguration> {
    private final FileSystem fs;
    private final Path configFileDirPath;

    public FileConfigurationDeserializer(FileSystem fs, Path configFileDirPath) {
        this(null, fs, configFileDirPath);
    }

    public FileConfigurationDeserializer(Class<?> vc, FileSystem fs, Path configFileDirPath) {
        super(vc);
        this.fs = fs;
        this.configFileDirPath = configFileDirPath;
    }

    @Override
    public LinearGridTimeSeries.FileConfiguration deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        JsonNode node = jp.getCodec().readTree(jp);
        String relPathStr = node.get("path").asText();
        Path absPath = configFileDirPath.resolve(relPathStr);
        Path path = fs.getPath(absPath.toString());
        OffsetDateTime startdate = OffsetDateTime.parse(node.get("startdate").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        OffsetDateTime enddate = OffsetDateTime.parse(node.get("enddate").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return new LinearGridTimeSeries.FileConfiguration(path, startdate, enddate);
    }
}

