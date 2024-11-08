package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Service {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("url")
    private URI url;

    @JsonProperty("type")
    private String type;

    @JsonProperty("enabled")
    private Boolean enabled = true;

    @JsonProperty("process")
    private Process process;

    @JsonProperty("configuration")
    private Object _configuration;

    @JsonProperty("attributes")
    private Object attributes;

    @JsonProperty("created")
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime created;

    @JsonProperty("plan")
    private String plan;

    @JsonProperty("costs")
    private BigDecimal costs = null;

    @JsonProperty("budget")
    private BigDecimal budget = null;

    public Service id(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Service title(String title) {
        this.title = title;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Service description(String description) {
        this.description = description;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Service url(URI url) {
        this.url = url;
        return this;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public Service type(String type) {
        this.type = type;
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Service enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Service process(Process process) {
        this.process = process;
        return this;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Service _configuration(Object _configuration) {
        this._configuration = _configuration;
        return this;
    }

    public Object getConfiguration() {
        return _configuration;
    }

    public void setConfiguration(Object _configuration) {
        this._configuration = _configuration;
    }

    public Service attributes(Object attributes) {
        this.attributes = attributes;
        return this;
    }

    public Object getAttributes() {
        return attributes;
    }

    public void setAttributes(Object attributes) {
        this.attributes = attributes;
    }

    public Service created(OffsetDateTime created) {
        this.created = created;
        return this;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public Service plan(String plan) {
        this.plan = plan;
        return this;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Service costs(BigDecimal costs) {
        this.costs = costs;
        return this;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public void setCosts(BigDecimal costs) {
        this.costs = costs;
    }

    public Service budget(BigDecimal budget) {
        this.budget = budget;
        return this;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Service service = (Service) o;
        return Objects.equals(this.id, service.id) &&
                Objects.equals(this.title, service.title) &&
                Objects.equals(this.description, service.description) &&
                Objects.equals(this.url, service.url) &&
                Objects.equals(this.type, service.type) &&
                Objects.equals(this.enabled, service.enabled) &&
                Objects.equals(this.process, service.process) &&
                Objects.equals(this._configuration, service._configuration) &&
                Objects.equals(this.attributes, service.attributes) &&
                Objects.equals(this.created, service.created) &&
                Objects.equals(this.plan, service.plan) &&
                Objects.equals(this.costs, service.costs) &&
                Objects.equals(this.budget, service.budget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, url, type, enabled, process, _configuration, attributes, created, plan, costs, budget);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Service {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    url: ").append(toIndentedString(url)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
        sb.append("    process: ").append(toIndentedString(process)).append("\n");
        sb.append("    _configuration: ").append(toIndentedString(_configuration)).append("\n");
        sb.append("    attributes: ").append(toIndentedString(attributes)).append("\n");
        sb.append("    created: ").append(toIndentedString(created)).append("\n");
        sb.append("    plan: ").append(toIndentedString(plan)).append("\n");
        sb.append("    costs: ").append(toIndentedString(costs)).append("\n");
        sb.append("    budget: ").append(toIndentedString(budget)).append("\n");
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
