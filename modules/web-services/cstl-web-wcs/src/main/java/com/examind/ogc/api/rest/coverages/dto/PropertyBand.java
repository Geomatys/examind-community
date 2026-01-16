package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyBand {

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private String type = "number"; // Usually 'number' or 'integer'

    @JsonProperty("minimum")
    private Double minimum; // Theoretical min (e.g. 0)

    @JsonProperty("maximum")
    private Double maximum; // Theoretical max (e.g. 65535)

    @JsonProperty("x-ogc-statistics")
    private RangeStatistics statistics;

    @JsonProperty("x-ogc-propertySeq")
    private int propertySeq;

    public PropertyBand(String title, String description, Double minimum, Double maximum, RangeStatistics statistics, int propertySeq) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.statistics = statistics;
        this.title = title;
        this.description = description;
        this.propertySeq = propertySeq;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getMinimum() { return minimum; }
    public void setMinimum(Double minimum) { this.minimum = minimum; }

    public Double getMaximum() { return maximum; }
    public void setMaximum(Double maximum) { this.maximum = maximum; }

    public RangeStatistics getStatistics() { return statistics; }
    public void setStatistics(RangeStatistics statistics) { this.statistics = statistics; }
}