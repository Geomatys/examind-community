package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class Step extends StepInfo {
    @JsonProperty("outputs")
    private Outputs outputs;
    @JsonProperty("output_collections")
    private OutputCollections outputCollections;
    @JsonProperty("jobs")
    private Job[] jobs;

    public Outputs getOutputs() {
        return outputs;
    }

    public void setOutputs(Outputs outputs) {
        this.outputs = outputs;
    }

    public OutputCollections getOutputCollections() {
        return outputCollections;
    }

    public void setOutputCollections(OutputCollections outputCollections) {
        this.outputCollections = outputCollections;
    }

    public Job[] getJobs() {
        return jobs;
    }

    public void setJobs(Job[] jobs) {
        this.jobs = jobs;
    }
}
