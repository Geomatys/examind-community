package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
public class ProcessDescriptionArgument {

    public enum ArgumentType {
        VALUE(null),
        ARRAY(null),
        FROM_NODE("from_node"),
        FROM_PARAMETER("from_parameter"),
        PROCESS_GRAPH("process_graph");

        private final String value;

        ArgumentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ArgumentType fromValue(String value) {
            for (ArgumentType type : ArgumentType.values()) {
                if (type.getValue().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid argument type: " + value);
        }
    }

    public ProcessDescriptionArgument(Object value, ArgumentType type) {
        this.value = value;
        this.type = type;
    }

    @JsonProperty("value")
    private Object value;

    @JsonProperty("type")
    private ArgumentType type;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ArgumentType getType() {
        return type;
    }

    public void setType(ArgumentType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessDescriptionArgument that = (ProcessDescriptionArgument) o;
        return Objects.equals(value, that.value) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return "ProcessDescriptionArgument{" +
                "value=" + value +
                ", type=" + type +
                '}';
    }
}
