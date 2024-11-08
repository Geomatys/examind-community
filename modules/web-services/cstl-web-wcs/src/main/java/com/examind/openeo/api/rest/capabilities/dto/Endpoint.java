package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Endpoint {
    @JsonProperty("path")
    private String path;

    /**
     * Gets or Sets methods
     */
    public enum MethodsEnum {
        GET("GET"),
        POST("POST"),
        PATCH("PATCH"),
        PUT("PUT"),
        DELETE("DELETE");

        private String value;

        MethodsEnum(String value) {
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
        public static MethodsEnum fromValue(String value) {
            for (MethodsEnum b : MethodsEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    @JsonProperty("methods")
    @Valid
    private List<MethodsEnum> methods = new ArrayList<>();

    public Endpoint(String path, List<MethodsEnum> methods) {
        this.path = path;
        this.methods = methods;
    }

    public Endpoint path(String path) {
        this.path = path;
        return this;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Endpoint methods(List<MethodsEnum> methods) {
        this.methods = methods;
        return this;
    }

    public Endpoint addMethodsItem(MethodsEnum methodsItem) {
        this.methods.add(methodsItem);
        return this;
    }

    public List<MethodsEnum> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodsEnum> methods) {
        this.methods = methods;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(this.path, endpoint.path) &&
                Objects.equals(this.methods, endpoint.methods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, methods);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Endpoint {\n");

        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    methods: ").append(toIndentedString(methods)).append("\n");
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
