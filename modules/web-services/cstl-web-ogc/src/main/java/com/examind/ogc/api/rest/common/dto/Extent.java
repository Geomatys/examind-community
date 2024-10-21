package com.examind.ogc.api.rest.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.List;

/**
 * @author Quentin BIALOTA
 * @author Guilhem LEGAL
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Extent {

    @JsonProperty("crs")
    private String crs;

    @JsonProperty("spatial")
    private SpatialCRS spatial;

    @JsonProperty("trs")
    private String trs;

    @JsonProperty("temporal")
    private TemporalCRS temporal;

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public SpatialCRS getSpatial() {
        return spatial;
    }

    public void setSpatial(SpatialCRS spatial) {
        this.spatial = spatial;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }

    public TemporalCRS getTemporal() {
        return temporal;
    }

    public void setTemporal(TemporalCRS temporal) {
        this.temporal = temporal;
    }

    public void setFromCoordinateReferenceSystem(CoordinateReferenceSystem crs) {

        if (crs instanceof CompoundCRS compoundCRS) {
            for (CoordinateReferenceSystem crsInCompound : compoundCRS.getComponents()) {
                if (crsInCompound instanceof org.opengis.referencing.crs.TemporalCRS) {
                    this.trs = opengisCRS(crsInCompound);
                } else {
                    this.crs = opengisCRS(crsInCompound);
                }
            }
        }

        else {
            this.crs = opengisCRS(crs);
        }
    }

    //TODO : add others CRS and they links to opengis
    private String opengisCRS(CoordinateReferenceSystem crs) {
        String name = crs.getName().getCode();

        // SPATIAL
        if (name.equalsIgnoreCase("WGS 84")) {
            return "http://www.opengis.net/def/crs/OGC/1.3/CRS84";
        }
        else if (name.equalsIgnoreCase("ETRS89-extended / LAEA Europe")) {
            return "https://www.opengis.net/def/crs/EPSG/0/3035";
        }

        // TIME
        else if (name.equalsIgnoreCase("Java time")) {
            return "https://www.opengis.net/def/crs/OGC/0/UnixTime";
        }

        // NOT FOUND
        else {
            return name;
        }
    }

    public String getSrs() {
        if (this.crs != null && this.trs == null) { //Only CRS
            return this.crs;
        }
        if (this.crs == null && this.trs != null) { //Only TRS
            return this.trs;
        }
        if (this.crs == null && this.trs == null) { //No CRS - No TRS
            return null;
        }

        return "http://www.opengis.net/def/crs-compound?" +
                "1=" + this.crs + "&" +
                "2=" + this.trs;
    }
}
