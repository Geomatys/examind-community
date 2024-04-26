package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Map;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class Workflow {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("published")
    private boolean published;
    @JsonProperty("importable")
    private boolean importable;
    @JsonProperty("deleted")
    private boolean deleted;
    @JsonProperty("hidden")
    private boolean hidden;
    @JsonProperty("tags")
    private String[] tags;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("slug")
    private String slug;
    @JsonProperty("inputs")
    private String[] inputs;
    @JsonProperty("annotation")
    private String annotation;
    @JsonProperty("license")
    private String license;
    @JsonProperty("steps")
    private Map<String, StepInput> steps;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String[] getInputs() {
        return inputs;
    }

    public Map<String, StepInput> getSteps() {
        return steps;
    }

    @Override
    public String toString() {
        return "Workflow{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", published=" + published +
                ", importable=" + importable +
                ", deleted=" + deleted +
                ", hidden=" + hidden +
                ", tags=" + Arrays.toString(tags) +
                ", owner='" + owner + '\'' +
                ", slug='" + slug + '\'' +
                ", inputs=" + Arrays.toString(inputs) +
                ", annotation='" + annotation + '\'' +
                ", license='" + license + '\'' +
                ", steps=" + steps +
                '}';
    }
}
