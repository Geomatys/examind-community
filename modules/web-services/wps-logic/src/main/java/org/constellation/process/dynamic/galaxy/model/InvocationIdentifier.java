package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class InvocationIdentifier {
    @JsonProperty("model_class")
    private String modelClass;
    @JsonProperty("id")
    private String id;
    @JsonProperty("workflow_id")
    private String workflowId;
    @JsonProperty("history_id")
    private String historyId;
    @JsonProperty("state")
    private Status state;

    public String getId() {
        return id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public Status getState() {
        return state;
    }

    @Override
    public String toString() {
        return "InvocationIdentifier{" +
                "modelClass='" + modelClass + '\'' +
                ", id='" + id + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", historyId='" + historyId + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
