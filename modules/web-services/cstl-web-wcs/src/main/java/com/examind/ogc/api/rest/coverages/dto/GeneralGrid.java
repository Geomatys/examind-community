package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Quentin BIALOTA
 */
public class GeneralGrid {

    @JsonProperty("type")
    private final String type = "GeneralGridCoverage";

    @JsonProperty("srsName")
    private String srsName;

    @JsonProperty("axisLabels")
    private List<String> axisLabels;

    @JsonProperty("axis")
    private List<Axis> axis;

    @JsonProperty("gridLimits")
    private GridLimits gridLimits;

    public GeneralGrid(String srsName, List<Axis> axis, GridLimits gridLimits) {
        this.srsName = srsName;
        this.axis = axis;
        this.gridLimits = gridLimits;
        this.axisLabels = getAxisLabels(axis);
    }

    public String getSrsName() {
        return srsName;
    }

    public void setSrsName(String srsName) {
        this.srsName = srsName;
    }

    public List<String> getAxisLabels() {
        return axisLabels;
    }


    public List<Axis> getAxis() {
        return axis;
    }

    public void setAxis(List<Axis> axis) {
        this.axis = axis;
        this.axisLabels = getAxisLabels(axis);
    }

    public GridLimits getGridLimits() {
        return gridLimits;
    }

    public void setGridLimits(GridLimits gridLimits) {
        this.gridLimits = gridLimits;
    }

    static List<String> getAxisLabels(List<Axis> axis) {
        return axis.stream()
                .map(Axis::getAxisLabel)
                .collect(Collectors.toList());
    }
}
