package org.constellation.process.dynamic.galaxy.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class OutputCollections {
    @JsonProperty("plots")
    private Plots plots;

    public Plots getPlots() {
        return plots;
    }

    public void setPlots(Plots plots) {
        this.plots = plots;
    }
}
