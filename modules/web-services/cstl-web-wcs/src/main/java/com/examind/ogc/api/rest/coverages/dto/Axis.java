package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA
 */
public abstract class Axis {

    @JsonProperty("type")
    private String type;

    @JsonProperty("axisLabel")
    private String axisLabel;

    @JsonProperty("lowerBound")
    private Object lowerBound;

    @JsonProperty("upperBound")
    private Object upperBound;

    public Axis(String type, String axisLabel, Object lowerBound, Object upperBound) {
        this.type = type;
        this.axisLabel = axisLabel;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAxisLabel() {
        return axisLabel;
    }

    public void setAxisLabel(String axisLabel) {
        this.axisLabel = axisLabel;
    }

    public Object getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Object lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Object getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Object upperBound) {
        this.upperBound = upperBound;
    }
}
