package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class Outputs {
    @JsonProperty("output_index")
    private OutputIndex outputIndex;

    public OutputIndex getOutputIndex() {
        return outputIndex;
    }

    public void setOutputIndex(OutputIndex outputIndex) {
        this.outputIndex = outputIndex;
    }
}
