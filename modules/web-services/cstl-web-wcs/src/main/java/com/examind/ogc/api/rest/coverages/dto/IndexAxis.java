package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Quentin BIALOTA
 */
public class IndexAxis extends Axis {

    public IndexAxis(String axisLabel, Object lowerBound, Object upperBound) {
        super("IndexAxis", axisLabel, lowerBound, upperBound);
    }
}
