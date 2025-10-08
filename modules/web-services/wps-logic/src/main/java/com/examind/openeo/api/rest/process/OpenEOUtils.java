package com.examind.openeo.api.rest.process;

import com.examind.openeo.api.rest.process.dto.DataTypeSchema;
import org.geotoolkit.process.ProcessDescriptor;
import org.opengis.geometry.Envelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenEOUtils {

    public static String examindProcessIdToOpenEOProcessId(String exaProcessId) {
        return switch (exaProcessId) {
            case "examind.coverage.openeo.load", "coverage.openeo.load" -> "load_collection";
            case "examind.coverage.save_result", "coverage.save_result" -> "save_result";
            default -> exaProcessId;
        };
    }

    public static String openEOProcessIdToExamindProcessId(String eoProcessId, boolean fullDescriptor) {
        return switch (eoProcessId) {
            case "load_collection" -> fullDescriptor ? "examind.coverage.openeo.load" : "coverage.openeo.load";
            case "save_result" -> fullDescriptor ? "examind.coverage.save_result" : "coverage.save_result";
            default -> eoProcessId;
        };
    }

    public static DataTypeSchema[] buildDataTypeSchema(ProcessDescriptor descriptor, String descriptorName,
                                                       String type, Class<?> clazz, boolean isArray, boolean mandatory) {
        List<DataTypeSchema> dataTypeSchemas = new ArrayList<>();

        if (Objects.equals(descriptor.getIdentifier().getCode(), "coverage.openeo.load") && descriptorName.equalsIgnoreCase("id")) {
            return new DataTypeSchema[]{new DataTypeSchema(type == null ? List.of() : List.of(DataTypeSchema.Type.fromValue(type, isArray)), "collection-id")};
        }

        if (clazz == Envelope.class) {
            String title = "Bounding Box";
            String description = "A bounding box is a list of 4 numbers (west, south, east, north) and an associated CRS.";
            String subtype = "bounding-box";
            List<String> required = new ArrayList<>(List.of("west", "south", "east", "north"));

            Map<String, Object> properties = new HashMap<>();
            properties.put("north", Map.of("description", "North (upper right corner)", "type", "number"));
            properties.put("south", Map.of("description", "South (lower left corner)", "type", "number"));
            properties.put("west", Map.of("description", "West (lower left corner)", "type", "number"));
            properties.put("east", Map.of("description", "East (upper right corner)", "type", "number"));
            properties.put("crs", Map.of("description", "The coordinate reference system in which the coordinates are given.", "type", "any", "default", "EPSG:4326"));

            dataTypeSchemas.add(new DataTypeSchema(title, description, properties, null, required, List.of(DataTypeSchema.Type.fromValue(type, isArray)), subtype));

            if (!mandatory) {
                dataTypeSchemas.add(new DataTypeSchema("No filter", "Don't filter spatially", null, null, null, List.of(DataTypeSchema.Type.NULL), null));
            }
            return dataTypeSchemas.toArray(new DataTypeSchema[0]);
        }

        if (clazz == String[].class && isArray && descriptorName.equalsIgnoreCase("temporal_extent")) {
            String title = "Temporal extent";
            String description = "Temporal extent to load.";
            String subtype = "temporal-interval";

            Map<String, Object> items = new HashMap<>();
            items.put("anyOf", List.of(
                    Map.of("format", "date-time", "subtype", "date-time", "type", "string"),
                    Map.of("format", "date", "subtype", "date", "type", "string"),
                    Map.of("type", "NULL")
                    )
            );

            dataTypeSchemas.add(new DataTypeSchema(title, description, null, items, null, List.of(DataTypeSchema.Type.fromValue(type, isArray)), subtype));

            if (!mandatory) {
                dataTypeSchemas.add(new DataTypeSchema("No filter", "Don't filter temporally.", null, null, null, List.of(DataTypeSchema.Type.NULL), null));
            }
            return dataTypeSchemas.toArray(new DataTypeSchema[0]);
        }

        if ((clazz == Integer[].class || clazz == String[].class) && isArray && descriptorName.equalsIgnoreCase("bands")) {
            String title = "Bands identifier to select";
            String description = "Bands to select using their index (you can use information in the collection to match this index to the content of the band)";

            Map<String, Object> items = Map.of("subtype", "band", "type", "number");

            dataTypeSchemas.add(new DataTypeSchema(title, description, null, items, null, List.of(DataTypeSchema.Type.fromValue(type, isArray)), null));

            if (!mandatory) {
                dataTypeSchemas.add(new DataTypeSchema("No filter", "Don't filter bands. All bands are included in the data cube.", null, null, null, List.of(DataTypeSchema.Type.NULL), null));
            }
            return dataTypeSchemas.toArray(new DataTypeSchema[0]);
        }

        return new DataTypeSchema[]{new DataTypeSchema(type == null ? List.of() : List.of(DataTypeSchema.Type.fromValue(type, isArray)), type)};
    }
}
