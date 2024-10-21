package com.examind.ogc.api.rest.coverages.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.geotoolkit.atom.xml.Link;

import java.util.List;

/**
 * @author Quentin BIALOTA
 */
public class DomainSet extends CoverageResponse {

    @JsonProperty("type")
    private final CoverageResponseType type = CoverageResponseType.DomainSet;

    @JsonProperty("generalGrid")
    private GeneralGrid generalGrid;

    public DomainSet(GeneralGrid generalGrid) {
        this.generalGrid = generalGrid;
    }

    public CoverageResponseType getType() {
        return type;
    }

    public GeneralGrid getGeneralGrid() {
        return generalGrid;
    }

    public void setGeneralGrid(GeneralGrid generalGrid) {
        this.generalGrid = generalGrid;
    }
}
