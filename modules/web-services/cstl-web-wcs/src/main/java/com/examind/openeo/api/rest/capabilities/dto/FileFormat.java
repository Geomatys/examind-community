package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.geotoolkit.atom.xml.Link;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class FileFormat {

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    /**
     * Gets or Sets gisDataTypes
     */
    public enum GisDataTypesEnum {
        RASTER("raster"),
        VECTOR("vector"),
        TABLE("table"),
        OTHER("other");

        private String value;

        GisDataTypesEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static GisDataTypesEnum fromValue(String value) {
            for (GisDataTypesEnum b : GisDataTypesEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @JsonProperty("gis_data_types")
    @Valid
    private List<GisDataTypesEnum> gisDataTypes = new ArrayList<>();

    @JsonProperty("parameters")
    @Valid
    private Map<String, Argument> parameters = null;

    @JsonProperty("links")
    @Valid
    private List<Link> links = null;

    public FileFormat title(String title) {
        this.title = title;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public FileFormat description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FileFormat gisDataTypes(List<GisDataTypesEnum> gisDataTypes) {
        this.gisDataTypes = gisDataTypes;
        return this;
    }

    public FileFormat addGisDataTypesItem(GisDataTypesEnum gisDataTypesItem) {
        this.gisDataTypes.add(gisDataTypesItem);
        return this;
    }

    public List<GisDataTypesEnum> getGisDataTypes() {
        return gisDataTypes;
    }

    public void setGisDataTypes(List<GisDataTypesEnum> gisDataTypes) {
        this.gisDataTypes = gisDataTypes;
    }

    public FileFormat parameters(Map<String, Argument> parameters) {
        this.parameters = parameters;
        return this;
    }

    public FileFormat putParametersItem(String key, Argument parametersItem) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(key, parametersItem);
        return this;
    }

    public Map<String, Argument> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Argument> parameters) {
        this.parameters = parameters;
    }

    public FileFormat links(List<Link> links) {
        this.links = links;
        return this;
    }

    public FileFormat addLinksItem(Link linksItem) {
        if (this.links == null) {
            this.links = new ArrayList<>();
        }
        this.links.add(linksItem);
        return this;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }


    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileFormat fileFormat = (FileFormat) o;
        return Objects.equals(this.title, fileFormat.title) &&
                Objects.equals(this.description, fileFormat.description) &&
                Objects.equals(this.gisDataTypes, fileFormat.gisDataTypes) &&
                Objects.equals(this.parameters, fileFormat.parameters) &&
                Objects.equals(this.links, fileFormat.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, gisDataTypes, parameters, links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class FileFormat {\n");

        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    gisDataTypes: ").append(toIndentedString(gisDataTypes)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
