package com.examind.openeo.api.rest.process.dto;

import com.examind.openeo.api.rest.process.dto.deserializer.DataTypeSchemaTypeDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTypeSchema {

    public enum Type {
        ARRAY("array"),
        BOOLEAN("boolean"),
        INTEGER("integer"),
        NULL("null"),
        NUMBER("number"),
        OBJECT("object"),
        STRING("string");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Type fromValue(String value, boolean isArray) {

            if (isArray) {
                return ARRAY;
            }

            if (value.equalsIgnoreCase("Real")) {
                return NUMBER;
            } else if (value.equalsIgnoreCase("CharacterString")) {
                return STRING;
            }

            for (Type type : Type.values()) {
                if (type.getValue().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return OBJECT;
            //throw new IllegalArgumentException("Invalid type: " + value);
        }

        public Class<?> getClassAssociated(String subtype) {
            switch (this) {
                case ARRAY -> {
                    if (subtype.equalsIgnoreCase("string") || subtype.equalsIgnoreCase("CharacterString")) {
                        return String[].class;
                    } else if (subtype.equalsIgnoreCase("Real") || subtype.equalsIgnoreCase("Double")) {
                        return double[].class;
                    } else if (subtype.equalsIgnoreCase("Integer") || subtype.equalsIgnoreCase("Int")) {
                        return int[].class;
                    } else if (subtype.equalsIgnoreCase("Boolean") || subtype.equalsIgnoreCase("Bool")) {
                        return boolean[].class;
                    } else if (subtype.equalsIgnoreCase("Object")) {
                        return Object[].class;
                    }
                    return ArrayList.class;
                }
                case BOOLEAN -> {
                    return Boolean.class;
                }
                case INTEGER -> {
                    return Integer.class;
                }
                case NULL -> {
                    return null;
                }
                case NUMBER -> {
                    return Double.class;
                }
                case OBJECT -> {
                    return Object.class;
                }
                case STRING -> {
                    return String.class;
                }
                default -> {
                    return null;
                }
            }
        }
    }

    public DataTypeSchema() {}

    public DataTypeSchema(List<Type> type, String subType) {
        this.type = type;
        this.subType = subType;
    }

    public DataTypeSchema(String title, String description, Map<String,Object> properties, Map<String, Object> items,
                          List<String> required, List<Type> type, String subType) {
        this.title = title;
        this.description = description;
        this.properties = properties;
        this.items = items;
        this.required = required;
        this.type = type;
        this.subType = subType;
    }

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("properties")
    private Map<String, Object> properties;

    @JsonProperty("items")
    private Map<String, Object> items;

    @JsonProperty("required")
    private List<String> required;

    @JsonProperty("type")
    @JsonDeserialize(using = DataTypeSchemaTypeDeserializer.class) //Type can be a String or a List of String
    private List<Type> type = new ArrayList<>();

    @JsonProperty("subtype")
    private String subType;

    public List<Type> getType() {
        return type;
    }

    public void setType(List<Type> type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataTypeSchema that = (DataTypeSchema) o;
        return Objects.equals(type, that.type) && Objects.equals(subType, that.subType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subType);
    }

    @Override
    public String toString() {
        return "DataTypeSchema{" +
                "type=" + type +
                ", subType='" + subType + '\'' +
                '}';
    }
}
