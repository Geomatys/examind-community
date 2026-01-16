package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author Quentin BIALOTA
 */
public class Schema {

    @JsonProperty("$schema")
    private final String schemaUrl = "https://json-schema.org/draft/2020-12/schema";

    @JsonProperty("type")
    private final String type = "object";

    @JsonProperty("title")
    private String title;

    @JsonProperty("properties")
    private Map<String, PropertyBand> properties;

    public Schema(String title, Map<String, PropertyBand> properties) {
        this.title = title;
        this.properties = properties;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, PropertyBand> properties() {
        return properties;
    }

    public void setProperties(Map<String, PropertyBand> properties) {
        this.properties = properties;
    }
}
