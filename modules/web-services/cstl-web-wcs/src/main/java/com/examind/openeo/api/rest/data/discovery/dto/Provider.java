package com.examind.openeo.api.rest.data.discovery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/EO-Data-Discovery">OpenEO Doc</a>
 */
public class Provider {
    @JsonProperty("name")
    private String name;

    @JsonProperty("url")
    private URI url;

    @JsonProperty("roles")
    private List<String> roles = new ArrayList<>();

    @JsonProperty("description")
    private String description;

    public Provider name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Provider url(URI url) {
        this.url = url;
        return this;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Provider description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Provider provider = (Provider) o;
        return Objects.equals(this.name, provider.name) &&
                Objects.equals(this.url, provider.url) &&
                Objects.equals(this.roles, provider.roles) &&
                Objects.equals(this.description, provider.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, roles, description);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Providers {\n");

        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
        sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
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
