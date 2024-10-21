package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

import static com.examind.ogc.api.rest.coverages.dto.GeneralGrid.getAxisLabels;

/**
 * @author Quentin BIALOTA
 */
public class GridLimits {

    @JsonProperty("type")
    private final String type = "GridLimits";

    @JsonProperty("srsName")
    private final String srsName = "http://www.opengis.net/def/crs/OGC/0/Index4D";

    @JsonProperty("axisLabels")
    private List<String> axisLabels;

    @JsonProperty("axis")
    private List<IndexAxis> axis;

    public GridLimits(List<IndexAxis> axis) {
        this.axis = axis;
        this.axisLabels = GeneralGrid.getAxisLabels(axis.stream()
                                                    .map(a -> (Axis) a)
                                                    .collect(Collectors.toList()));
    }

    public List<String> getAxisLabels() {
        return axisLabels;
    }

    public List<IndexAxis> getAxis() {
        return axis;
    }

    public void setAxis(List<IndexAxis> axis) {
        this.axis = axis;
        this.axisLabels = GeneralGrid.getAxisLabels(axis.stream()
                                                    .map(a -> (Axis) a)
                                                    .collect(Collectors.toList()));
    }
}
