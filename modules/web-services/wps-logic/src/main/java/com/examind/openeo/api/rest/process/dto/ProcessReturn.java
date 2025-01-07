package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
public class ProcessReturn {

    public ProcessReturn() {}

    public ProcessReturn(String description, DataTypeSchema schema) {
        this.description = description;
        this.schema = schema;
    }

    @JsonProperty("description")
    private String description;

    @JsonProperty("schema")
    private DataTypeSchema schema = null;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DataTypeSchema getSchema() {
        return schema;
    }

    public void setSchema(DataTypeSchema schema) {
        this.schema = schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessReturn that = (ProcessReturn) o;
        return Objects.equals(description, that.description) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, schema);
    }

    @Override
    public String toString() {
        return "ProcessReturn{" +
                "description='" + description + '\'' +
                ", schema=" + schema +
                '}';
    }
}
