package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Quentin BIALOTA
 */
public class IrregularAxis extends Axis{

    @JsonProperty("uomLabel")
    private String uomLabel;

    @JsonProperty("coordinate")
    private List<Object> coordinate;

    public IrregularAxis(String axisLabel, Object lowerBound, Object upperBound, String uomLabel, List<Object> coordinate) {
        super("IrregularAxis", axisLabel, lowerBound, upperBound);
        this.coordinate = coordinate;
        this.uomLabel = uomLabel;
    }

    public List<Object> getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(List<Object> coordinate) {
        this.coordinate = coordinate;
    }

    public String getUomLabel() {
        return uomLabel;
    }

    public void setUomLabel(String uomLabel) {
        this.uomLabel = uomLabel;
    }
}
