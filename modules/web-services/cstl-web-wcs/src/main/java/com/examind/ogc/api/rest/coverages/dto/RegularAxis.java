package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA
 */
public class RegularAxis extends Axis{

    @JsonProperty("uomLabel")
    private String uomLabel;

    @JsonProperty("resolution")
    private double resolution;

    public RegularAxis(String axisLabel, Object lowerBound, Object upperBound, String uomLabel, double resolution) {
        super("RegularAxis", axisLabel, lowerBound, upperBound);
        this.resolution = resolution;
        this.uomLabel = uomLabel;
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }

    public String getUomLabel() {
        return uomLabel;
    }

    public void setUomLabel(String uomLabel) {
        this.uomLabel = uomLabel;
    }
}
