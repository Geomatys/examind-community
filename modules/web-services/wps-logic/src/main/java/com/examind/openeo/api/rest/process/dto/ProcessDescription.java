package com.examind.openeo.api.rest.process.dto;

import com.examind.openeo.api.rest.process.dto.deserializer.ProcessDescriptionDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Process-Discovery">OpenEO Doc</a>
 */
@JsonDeserialize(using = ProcessDescriptionDeserializer.class)
public class ProcessDescription {

    public ProcessDescription() {}

    public ProcessDescription(String processId, String title, String description, Map<String, ProcessDescriptionArgument> arguments, Object returns, Boolean result) {
        this.processId = processId;
        this.title = title;
        this.description = description;
        this.arguments = arguments;
        this.returns = returns;
        this.result = result;
    }

    @JsonProperty("process_id")
    private String processId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("arguments")
    private Map<String, ProcessDescriptionArgument> arguments = new HashMap<>();

    @JsonProperty("returns")
    private Object returns = null;

    @JsonProperty(value = "result", defaultValue = "false")
    private Boolean result = false;

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, ProcessDescriptionArgument> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, ProcessDescriptionArgument> arguments) {
        this.arguments = arguments;
    }

    public Object getReturns() {
        return returns;
    }

    public void setReturns(Object returns) {
        this.returns = returns;
    }

    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessDescription that = (ProcessDescription) o;
        return Objects.equals(processId, that.processId) && Objects.equals(title, that.title) && Objects.equals(description, that.description) && Objects.equals(arguments, that.arguments) && Objects.equals(returns, that.returns) && Objects.equals(result, that.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, title, description, arguments, returns, result);
    }

    @Override
    public String toString() {
        return "ProcessExample{" +
                "processId='" + processId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", arguments=" + arguments +
                ", returns=" + returns +
                ", result=" + result +
                '}';
    }
}
