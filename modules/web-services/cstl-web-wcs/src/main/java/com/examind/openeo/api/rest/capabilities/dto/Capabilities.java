package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Capabilities {
    @JsonProperty("api_version")
    private String apiVersion;

    @JsonProperty("backend_version")
    private String backendVersion;

    @JsonProperty("stac_version")
    private String stacVersion;

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("conformsTo")
    private List<String> conformsTo = new ArrayList<>();

    @JsonProperty("production")
    private Boolean production = false;

    @JsonProperty("endpoints")
    @Valid
    private List<Endpoint> endpoints = new ArrayList<>();

    @JsonProperty("billing")
    private Billing billing;

    @JsonProperty("links")
    @Valid
    private List<Link> links = new ArrayList<>();

    public Capabilities apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public Capabilities backendVersion(String backendVersion) {
        this.backendVersion = backendVersion;
        return this;
    }

    public String getBackendVersion() {
        return backendVersion;
    }

    public void setBackendVersion(String backendVersion) {
        this.backendVersion = backendVersion;
    }

    public Capabilities stacVersion(String stacVersion) {
        this.stacVersion = stacVersion;
        return this;
    }

    public String getStacVersion() {
        return stacVersion;
    }

    public void setStacVersion(String stacVersion) {
        this.stacVersion = stacVersion;
    }

    public Capabilities id(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Capabilities title(String title) {
        this.title = title;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Capabilities description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Capabilities conformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
        return this;
    }

    public Capabilities addConformsTo(String conformsToItem) {
        this.conformsTo.add(conformsToItem);
        return this;
    }

    public List<String> getConformsTo() {
        return conformsTo;
    }

    public void setConformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }

    public Capabilities production(Boolean production) {
        this.production = production;
        return this;
    }

    public Boolean isProduction() {
        return production;
    }

    public void setProduction(Boolean production) {
        this.production = production;
    }

    public Capabilities endpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
        return this;
    }

    public Capabilities addEndpointsItem(Endpoint endpointsItem) {
        this.endpoints.add(endpointsItem);
        return this;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public Capabilities billing(Billing billing) {
        this.billing = billing;
        return this;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public Capabilities links(List<Link> links) {
        this.links = links;
        return this;
    }

    public Capabilities addLinksItem(Link linksItem) {
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
        Capabilities capabilities = (Capabilities) o;
        return Objects.equals(this.apiVersion, capabilities.apiVersion) &&
                Objects.equals(this.backendVersion, capabilities.backendVersion) &&
                Objects.equals(this.stacVersion, capabilities.stacVersion) &&
                Objects.equals(this.id, capabilities.id) &&
                Objects.equals(this.title, capabilities.title) &&
                Objects.equals(this.description, capabilities.description) &&
                Objects.equals(this.production, capabilities.production) &&
                Objects.equals(this.endpoints, capabilities.endpoints) &&
                Objects.equals(this.billing, capabilities.billing) &&
                Objects.equals(this.links, capabilities.links);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, backendVersion, stacVersion, id, title, description, production, endpoints, billing, links);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Capabilities {\n");

        sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
        sb.append("    backendVersion: ").append(toIndentedString(backendVersion)).append("\n");
        sb.append("    stacVersion: ").append(toIndentedString(stacVersion)).append("\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    production: ").append(toIndentedString(production)).append("\n");
        sb.append("    endpoints: ").append(toIndentedString(endpoints)).append("\n");
        sb.append("    billing: ").append(toIndentedString(billing)).append("\n");
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
