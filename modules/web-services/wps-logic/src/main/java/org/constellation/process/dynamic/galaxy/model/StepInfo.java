package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class StepInfo {
    @JsonProperty("model_class")
    private String modelClass;
    @JsonProperty("id")
    private String id;
    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("workflow_step_id")
    private String workflowStepId;
    @JsonProperty("subworkflow_invocation_id")
    private String subworkflowInvocationId;
    @JsonProperty("state")
    private Status state;
    @JsonProperty("action")
    private String action;
    @JsonProperty("order_index")
    private int orderIndex;
    @JsonProperty("workflow_step_label")
    private String workflowStepLabel;
    @JsonProperty("workflow_step_uuid")
    private String workflowStepUuid;

    public String getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public String getWorkflowStepId() {
        return workflowStepId;
    }

    public String getSubworkflowInvocationId() {
        return subworkflowInvocationId;
    }

    public Status getState() {
        return state;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    @Override
    public String toString() {
        return "StepInfo{" +
                "modelClass='" + modelClass + '\'' +
                ", id='" + id + '\'' +
                ", jobId='" + jobId + '\'' +
                ", workflowStepId='" + workflowStepId + '\'' +
                ", subworkflowInvocationId='" + subworkflowInvocationId + '\'' +
                ", state='" + state + '\'' +
                ", action='" + action + '\'' +
                ", orderIndex=" + orderIndex +
                ", workflowStepLabel='" + workflowStepLabel + '\'' +
                '}';
    }
}
