package com.examind.openeo.api.rest.capabilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Quentin BIALOTA (Geomatys)
 * Based on : <a href="https://api.openeo.org/#tag/Capabilities">OpenEO Doc</a>
 */
public class Conformance {

    @JsonProperty("conformsTo")
    private List<String> conformsTo = new ArrayList<>();
    
    public Conformance() {
        
    }
    
    public Conformance(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }

    public Conformance conformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
        return this;
    }

    public Conformance addConformsTo(String conformsToItem) {
        this.conformsTo.add(conformsToItem);
        return this;
    }

    public List<String> getConformsTo() {
        return conformsTo;
    }

    public void setConformsTo(List<String> conformsTo) {
        this.conformsTo = conformsTo;
    }
}
