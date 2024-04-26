package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class StepJobState {
    @JsonProperty("populated_state")
    private Status populatedState;
    @JsonProperty("states")
    private Map<Status, Integer> states;
    @JsonProperty("model")
    private String model;
    @JsonProperty("id")
    private String id;

    public StepJobState() {}

    public StepJobState(Status populatedState, Map<Status, Integer> states, String model, String id) {
        this.populatedState = populatedState;
        this.states = states;
        this.model = model;
        this.id = id;
    }

    public boolean hasOtherStatesThanOk() {
        for (Map.Entry<Status, Integer> entry : states.entrySet()) {
            if (!entry.getKey().equals(Status.OK)) {
                return true;
            }
        }
        return false;
    }

    public Status getPopulatedState() {
        return populatedState;
    }

    public void setPopulatedState(Status populatedState) {
        this.populatedState = populatedState;
    }

    public Map<Status, Integer> getStates() {
        return states;
    }

    public void setStates(Map<Status, Integer> states) {
        this.states = states;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
