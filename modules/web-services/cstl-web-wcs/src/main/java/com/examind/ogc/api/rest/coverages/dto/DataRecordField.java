package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA
 */
public class DataRecordField {

    @JsonProperty("type")
    private final String type = "Quantity";

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("encodingInfo")
    private EncodingInfo encodingInfo;

    public DataRecordField(String name, String description, EncodingInfo encodingInfo) {
        this.name = name;
        this.description = description;
        this.encodingInfo = encodingInfo;
    }

    public String getType() {
        return type;
    }

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

    public EncodingInfo getEncodingInfo() {
        return encodingInfo;
    }

    public void setEncodingInfo(EncodingInfo encodingInfo) {
        this.encodingInfo = encodingInfo;
    }
}
