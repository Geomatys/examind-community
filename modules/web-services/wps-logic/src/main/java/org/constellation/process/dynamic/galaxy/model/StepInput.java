package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class StepInput {
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("tool_id")
    private String toolId;
    @JsonProperty("tool_version")
    private String toolVersion;
    @JsonProperty("annotation")
    private String annotation;
    @JsonProperty("tool_inputs")
    private ToolInputs toolInputs;

    public String getId() {
        return id;
    }

    public ToolInputs getToolInputs() {
        return toolInputs;
    }

    @Override
    public String toString() {
        return "Step{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", annotation='" + annotation + '\'' +
                '}';
    }
}
