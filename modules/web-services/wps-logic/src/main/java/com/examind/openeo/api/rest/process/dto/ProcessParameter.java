package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
public class ProcessParameter {

    public ProcessParameter() {}

    public ProcessParameter(String name, String description, DataTypeSchema schema) {
        this.name = name;
        this.description = description;
        this.schema = schema;
    }

    public ProcessParameter(String name, String description, DataTypeSchema schema, boolean optional, Object defaultObject) {
        this.name = name;
        this.description = description;
        this.schema = schema;
        this.optional = optional;
        this.defaultObject = defaultObject;
    }

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("schema")
    private DataTypeSchema schema;

    @JsonProperty("optional")
    private boolean optional = false;

    @JsonProperty("default")
    private Object defaultObject = null;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public Object getDefaultObject() {
        return defaultObject;
    }

    public void setDefaultObject(Object defaultObject) {
        this.defaultObject = defaultObject;
    }

    public boolean isValid() {
        return !((optional && defaultObject == null) || (!optional && defaultObject != null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessParameter that = (ProcessParameter) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, schema);
    }

    @Override
    public String toString() {
        return "ProcessParameter{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", schema=" + schema +
                '}';
    }
}
