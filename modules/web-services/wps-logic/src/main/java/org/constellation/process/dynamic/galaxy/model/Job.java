package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class Job {
    @JsonProperty("model_class")
    private String modelClass;
    @JsonProperty("id")
    private String id;
    @JsonProperty("state")
    private Status state;
    @JsonProperty("exit_code")
    private int exitCode;
    @JsonProperty("history_id")
    private String historyId;
    @JsonProperty("inputs")
    private Map<String, Object> inputs;
    @JsonProperty("params")
    private Map<String, Object> params;
}
