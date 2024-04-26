package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class Invocation extends InvocationIdentifier{
    @JsonProperty("steps")
    private StepInfo[] steps;

    public StepInfo[] getSteps() {
        return steps;
    }
}
