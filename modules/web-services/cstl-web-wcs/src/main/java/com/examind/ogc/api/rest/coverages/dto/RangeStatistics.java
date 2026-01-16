package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RangeStatistics {

    @JsonProperty("min")
    private Double min;

    @JsonProperty("max")
    private Double max;

    @JsonProperty("mean")
    private Double mean;

    @JsonProperty("stdDev")
    private Double stdDev;

    public RangeStatistics() {}

    public RangeStatistics(Double min, Double max, Double mean, Double stdDev) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stdDev = stdDev;
    }

    // Getters and Setters
    public Double getMin() { return min; }
    public void setMin(Double min) { this.min = min; }

    public Double getMax() { return max; }
    public void setMax(Double max) { this.max = max; }

    public Double getMean() { return mean; }
    public void setMean(Double mean) { this.mean = mean; }

    public Double getStdDev() { return stdDev; }
    public void setStdDev(Double stdDev) { this.stdDev = stdDev; }
}