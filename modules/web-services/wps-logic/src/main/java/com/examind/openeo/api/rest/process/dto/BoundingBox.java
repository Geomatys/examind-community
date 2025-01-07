package com.examind.openeo.api.rest.process.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

/**
 * @author Quentin BIALOTA (Geomatys)
 */
public class BoundingBox {

    @JsonProperty("west")
    private double west;

    @JsonProperty("south")
    private double south;

    @JsonProperty("east")
    private double east;

    @JsonProperty("north")
    private double north;

    @JsonProperty("base")
    private double base;

    @JsonProperty("height")
    private double height;

    @JsonProperty(value = "crs")
    private String crs;

    public double getWest() {
        return west;
    }

    public void setWest(double west) {
        this.west = west;
    }

    public double getSouth() {
        return south;
    }

    public void setSouth(double south) {
        this.south = south;
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
    }

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = north;
    }

    public double getBase() {
        return base;
    }

    public void setBase(double base) {
        this.base = base;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }
}
