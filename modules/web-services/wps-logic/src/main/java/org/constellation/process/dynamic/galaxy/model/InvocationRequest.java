package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class InvocationRequest {
    @JsonProperty("new_history_name")
    private String newHistoryName = null;
    @JsonProperty("history_id")
    private String historyId = null;
    @JsonProperty("replacement_params")
    private Map<String, Object> replacementParams = new HashMap<>();
    @JsonProperty("use_cached_job")
    private boolean useCachedJob = false;
    @JsonProperty("inputs")
    private Map<String, Map<String, Object>> inputs = new HashMap<>();
    @JsonProperty("parameters")
    private Map<String, Map<String, Object>> parameters = new HashMap<>();
    @JsonProperty("parameters_normalized")
    private boolean parametersNormalized = true;
    @JsonProperty("batch")
    private boolean batch = true;
    @JsonProperty("require_exact_tool_versions")
    private boolean requireExactToolVersions = false;

    public String getNewHistoryName() {
        return newHistoryName;
    }

    public void setNewHistoryName(String newHistoryName) {
        this.newHistoryName = newHistoryName;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public Map<String, Object> getReplacementParams() {
        return replacementParams;
    }

    public void setReplacementParams(Map<String, Object> replacementParams) {
        this.replacementParams = replacementParams;
    }

    public boolean isUseCachedJob() {
        return useCachedJob;
    }

    public void setUseCachedJob(boolean useCachedJob) {
        this.useCachedJob = useCachedJob;
    }

    public Map<String, Map<String, Object>> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Map<String, Object>> inputs) {
        this.inputs = inputs;
    }

    public Map<String, Map<String, Object>> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Map<String, Object>> parameters) {
        this.parameters = parameters;
    }

    public boolean isParametersNormalized() {
        return parametersNormalized;
    }

    public void setParametersNormalized(boolean parametersNormalized) {
        this.parametersNormalized = parametersNormalized;
    }

    public boolean isBatch() {
        return batch;
    }

    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    public boolean isRequireExactToolVersions() {
        return requireExactToolVersions;
    }

    public void setRequireExactToolVersions(boolean requireExactToolVersions) {
        this.requireExactToolVersions = requireExactToolVersions;
    }
}
